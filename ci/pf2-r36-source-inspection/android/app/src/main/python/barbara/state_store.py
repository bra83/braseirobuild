from __future__ import annotations

import hashlib
import json
import sqlite3
import threading
import time
import zlib
import base64
from copy import deepcopy

from .host_contract import HostIntegrationContract, HostIntegrationLoader
from .state import CampaignState


class StateVersionConflict(ValueError):
    pass


class DurableStateStore:
    """SQLite/WAL canonical store with CAS and durable command idempotency.

    This store is intentionally platform-neutral. Android may implement the same
    protocol with Room, while desktop/server hosts can use this implementation.
    State and command receipt are committed in one SQLite transaction.
    """

    def __init__(self, db_path: str):
        if not isinstance(db_path, str) or not db_path:
            raise ValueError('invalid_state_store_path')
        self._lock = threading.RLock()
        self._db = sqlite3.connect(db_path, timeout=30, check_same_thread=False, isolation_level=None)
        self._db.execute('PRAGMA journal_mode=WAL')
        self._db.execute('PRAGMA synchronous=FULL')
        self._db.execute('PRAGMA foreign_keys=ON')
        self._db.execute('PRAGMA busy_timeout=30000')
        self._db.executescript('''
            CREATE TABLE IF NOT EXISTS campaigns(
                campaign_id TEXT PRIMARY KEY,
                system_id TEXT NOT NULL,
                state_version INTEGER NOT NULL,
                state_json TEXT NOT NULL,
                contract_json TEXT,
                updated_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS command_receipts(
                campaign_id TEXT NOT NULL,
                command_id TEXT NOT NULL,
                fingerprint TEXT NOT NULL,
                state_version INTEGER NOT NULL,
                response_json TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(campaign_id, command_id),
                FOREIGN KEY(campaign_id) REFERENCES campaigns(campaign_id) ON DELETE CASCADE
            );
            CREATE INDEX IF NOT EXISTS idx_command_receipts_campaign_version
                ON command_receipts(campaign_id, state_version);
            CREATE TABLE IF NOT EXISTS event_archives(
                campaign_id TEXT NOT NULL,
                seq INTEGER NOT NULL,
                event_count INTEGER NOT NULL,
                sha256 TEXT NOT NULL,
                payload BLOB NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(campaign_id, seq),
                FOREIGN KEY(campaign_id) REFERENCES campaigns(campaign_id) ON DELETE CASCADE
            );
        ''')

    @staticmethod
    def fingerprint(payload) -> str:
        try:
            raw = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)
        except (TypeError, ValueError) as exc:
            raise ValueError('invalid_command_fingerprint_payload') from exc
        return hashlib.sha256(raw.encode('utf-8')).hexdigest()

    def close(self):
        with self._lock:
            if self._db is not None:
                self._db.close()
                self._db = None

    def _ensure_open(self):
        if self._db is None:
            raise RuntimeError('state_store_closed')

    def create_campaign(self, state: CampaignState, contract: HostIntegrationContract | None = None, *, replace=False):
        state.validate()
        if contract is not None and contract.system_id != state.system_id:
            raise ValueError('host_contract_system_mismatch')
        contract_json = None if contract is None else json.dumps(contract.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))
        with self._lock:
            self._ensure_open()
            sql = 'INSERT OR REPLACE' if replace else 'INSERT'
            try:
                self._db.execute('BEGIN IMMEDIATE')
                self._db.execute(
                    f'{sql} INTO campaigns(campaign_id,system_id,state_version,state_json,contract_json,updated_at) VALUES(?,?,?,?,?,?)',
                    (state.campaign_id, state.system_id, state.state_version, state.to_json(), contract_json, int(time.time() * 1000)),
                )
                if replace:
                    self._db.execute('DELETE FROM command_receipts WHERE campaign_id=?', (state.campaign_id,))
                self._db.execute('COMMIT')
            except Exception:
                self._db.execute('ROLLBACK')
                raise
        return state.snapshot()

    def load(self, campaign_id: str) -> CampaignState:
        if not isinstance(campaign_id, str) or not campaign_id:
            raise ValueError('invalid_campaign_id')
        with self._lock:
            self._ensure_open()
            row = self._db.execute('SELECT state_json FROM campaigns WHERE campaign_id=?', (campaign_id,)).fetchone()
        if row is None:
            raise KeyError(campaign_id)
        return CampaignState.from_json(row[0])

    def load_contract(self, campaign_id: str) -> HostIntegrationContract | None:
        with self._lock:
            self._ensure_open()
            row = self._db.execute('SELECT contract_json FROM campaigns WHERE campaign_id=?', (campaign_id,)).fetchone()
        if row is None:
            raise KeyError(campaign_id)
        if row[0] is None:
            return None
        return HostIntegrationLoader().load(json.loads(row[0]))

    def lookup_command(self, campaign_id: str, command_id: str, fingerprint: str | None = None):
        with self._lock:
            self._ensure_open()
            row = self._db.execute(
                'SELECT fingerprint,response_json,state_version FROM command_receipts WHERE campaign_id=? AND command_id=?',
                (campaign_id, command_id),
            ).fetchone()
        if row is None:
            return None
        if fingerprint is not None and row[0] != fingerprint:
            raise ValueError('request_id_collision')
        return {'fingerprint': row[0], 'response': json.loads(row[1]), 'state_version': row[2]}

    def compare_and_swap(self, *, expected_state_version: int, new_state: CampaignState, command_id: str, fingerprint: str, response: dict):
        if not isinstance(expected_state_version, int) or isinstance(expected_state_version, bool) or expected_state_version < 0:
            raise ValueError('invalid_expected_state_version')
        if not isinstance(command_id, str) or not command_id or len(command_id) > 160:
            raise ValueError('invalid_request_id')
        if not isinstance(fingerprint, str) or len(fingerprint) != 64:
            raise ValueError('invalid_command_fingerprint')
        if not isinstance(response, dict):
            raise ValueError('invalid_command_response')
        new_state.validate()
        response_json = json.dumps(response, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)
        with self._lock:
            self._ensure_open()
            try:
                self._db.execute('BEGIN IMMEDIATE')
                receipt = self._db.execute(
                    'SELECT fingerprint,response_json,state_version FROM command_receipts WHERE campaign_id=? AND command_id=?',
                    (new_state.campaign_id, command_id),
                ).fetchone()
                if receipt is not None:
                    if receipt[0] != fingerprint:
                        raise ValueError('request_id_collision')
                    self._db.execute('COMMIT')
                    return {'idempotent': True, 'response': json.loads(receipt[1]), 'state_version': receipt[2]}
                row = self._db.execute(
                    'SELECT system_id,state_version FROM campaigns WHERE campaign_id=?', (new_state.campaign_id,)
                ).fetchone()
                if row is None:
                    raise KeyError(new_state.campaign_id)
                if row[0] != new_state.system_id:
                    raise ValueError('state_store_system_mismatch')
                if row[1] != expected_state_version:
                    raise StateVersionConflict('state_version_conflict')
                if new_state.state_version < expected_state_version or new_state.state_version > expected_state_version + 1:
                    raise ValueError('invalid_state_version_transition')
                cur = self._db.execute(
                    'UPDATE campaigns SET state_version=?,state_json=?,updated_at=? WHERE campaign_id=? AND state_version=?',
                    (new_state.state_version, new_state.to_json(), int(time.time() * 1000), new_state.campaign_id, expected_state_version),
                )
                if cur.rowcount != 1:
                    raise StateVersionConflict('state_version_conflict')
                self._db.execute(
                    'INSERT INTO command_receipts(campaign_id,command_id,fingerprint,state_version,response_json,created_at) VALUES(?,?,?,?,?,?)',
                    (new_state.campaign_id, command_id, fingerprint, new_state.state_version, response_json, int(time.time() * 1000)),
                )
                self._db.execute('COMMIT')
                return {'idempotent': False, 'response': deepcopy(response), 'state_version': new_state.state_version}
            except Exception:
                if self._db.in_transaction:
                    self._db.execute('ROLLBACK')
                raise


    def has_campaign(self, campaign_id: str) -> bool:
        if not isinstance(campaign_id, str) or not campaign_id:
            raise ValueError('invalid_campaign_id')
        with self._lock:
            self._ensure_open()
            row = self._db.execute('SELECT 1 FROM campaigns WHERE campaign_id=?', (campaign_id,)).fetchone()
        return row is not None

    def delete_campaign(self, campaign_id: str) -> bool:
        if not isinstance(campaign_id, str) or not campaign_id:
            raise ValueError('invalid_campaign_id')
        with self._lock:
            self._ensure_open()
            cur = self._db.execute('DELETE FROM campaigns WHERE campaign_id=?', (campaign_id,))
        return cur.rowcount == 1

    def export_campaign(self, campaign_id: str) -> dict:
        """Lossless canonical export including durable idempotency receipts."""
        with self._lock:
            self._ensure_open()
            row = self._db.execute(
                'SELECT system_id,state_version,state_json,contract_json,updated_at FROM campaigns WHERE campaign_id=?',
                (campaign_id,),
            ).fetchone()
            if row is None:
                raise KeyError(campaign_id)
            receipts = self._db.execute(
                'SELECT command_id,fingerprint,state_version,response_json,created_at FROM command_receipts WHERE campaign_id=? ORDER BY created_at,command_id',
                (campaign_id,),
            ).fetchall()
            archives = self._db.execute(
                'SELECT seq,event_count,sha256,payload,created_at FROM event_archives WHERE campaign_id=? ORDER BY seq',
                (campaign_id,),
            ).fetchall()
        state = CampaignState.from_json(row[2])
        return {
            'schema': 1,
            'campaign_id': campaign_id,
            'system_id': row[0],
            'state_version': row[1],
            'state': state.to_dict(),
            'host_contract': None if row[3] is None else json.loads(row[3]),
            'updated_at': row[4],
            'event_archives': [
                {'seq':item[0],'event_count':item[1],'sha256':item[2],'payload_b64':base64.b64encode(item[3]).decode('ascii'),'created_at':item[4]} for item in archives
            ],
            'receipts': [
                {
                    'command_id': item[0], 'fingerprint': item[1], 'state_version': item[2],
                    'response': json.loads(item[3]), 'created_at': item[4],
                } for item in receipts
            ],
        }

    def import_campaign(self, document: dict, *, campaign_id: str | None = None, replace: bool = False) -> CampaignState:
        if not isinstance(document, dict) or document.get('schema') != 1:
            raise ValueError('invalid_campaign_backup')
        allowed = {'schema','campaign_id','system_id','state_version','state','host_contract','updated_at','receipts','event_archives'}
        if not set(document) <= allowed or not (allowed-{'event_archives'}) <= set(document):
            raise ValueError('invalid_campaign_backup_fields')
        source_id = document.get('campaign_id')
        target_id = campaign_id or source_id
        if not isinstance(target_id, str) or not target_id or len(target_id) > 160:
            raise ValueError('invalid_campaign_id')
        raw_state = deepcopy(document.get('state'))
        if not isinstance(raw_state, dict):
            raise ValueError('invalid_campaign_backup_state')
        raw_state['campaign_id'] = target_id
        state = CampaignState.from_dict(raw_state)
        if state.system_id != document.get('system_id') or state.state_version != document.get('state_version'):
            raise ValueError('campaign_backup_state_mismatch')
        contract = document.get('host_contract')
        contract_obj = None if contract is None else HostIntegrationLoader().load(contract)
        if contract_obj is not None and contract_obj.system_id != state.system_id:
            raise ValueError('host_contract_system_mismatch')
        receipts = document.get('receipts')
        if not isinstance(receipts, list):
            raise ValueError('invalid_campaign_backup_receipts')
        normalized = []
        seen = set()
        for item in receipts:
            if not isinstance(item, dict) or set(item) != {'command_id','fingerprint','state_version','response','created_at'}:
                raise ValueError('invalid_campaign_backup_receipt')
            cid=item['command_id']; fp=item['fingerprint']; ver=item['state_version']; response=item['response']; created=item['created_at']
            if not isinstance(cid,str) or not cid or len(cid)>160 or cid in seen:
                raise ValueError('invalid_campaign_backup_receipt')
            seen.add(cid)
            if not isinstance(fp,str) or len(fp)!=64 or any(c not in '0123456789abcdef' for c in fp):
                raise ValueError('invalid_campaign_backup_receipt')
            if not isinstance(ver,int) or isinstance(ver,bool) or not 0<=ver<=state.state_version:
                raise ValueError('invalid_campaign_backup_receipt')
            if not isinstance(response,dict) or not isinstance(created,int) or isinstance(created,bool) or created<0:
                raise ValueError('invalid_campaign_backup_receipt')
            response = deepcopy(response)
            if target_id != source_id:
                # Receipts remain exactly-once after cloning a World, but any embedded
                # canonical state must identify the cloned campaign rather than the source.
                raw_response_state = response.get('state')
                if isinstance(raw_response_state, str):
                    try:
                        receipt_state = CampaignState.from_json(raw_response_state)
                    except ValueError:
                        receipt_state = None
                    if receipt_state is not None and receipt_state.campaign_id == source_id:
                        receipt_state.campaign_id = target_id
                        response['state'] = receipt_state.to_json()
                def remap_ids(value):
                    if isinstance(value, dict):
                        return {k: (target_id if k == 'campaign_id' and v == source_id else remap_ids(v)) for k, v in value.items()}
                    if isinstance(value, list):
                        return [remap_ids(v) for v in value]
                    return value
                response = remap_ids(response)
            response_json=json.dumps(response,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
            normalized.append((cid,fp,ver,response_json,created))
        archive_rows=[]
        archive_seen=set()
        for item in document.get('event_archives',[]):
            if not isinstance(item,dict) or set(item)!={'seq','event_count','sha256','payload_b64','created_at'}:
                raise ValueError('invalid_campaign_backup_archive')
            seq=item['seq']; count=item['event_count']; sha=item['sha256']; created=item['created_at']
            if not isinstance(seq,int) or isinstance(seq,bool) or seq<0 or seq in archive_seen: raise ValueError('invalid_campaign_backup_archive')
            archive_seen.add(seq)
            if not isinstance(count,int) or isinstance(count,bool) or count<1: raise ValueError('invalid_campaign_backup_archive')
            if not isinstance(sha,str) or len(sha)!=64: raise ValueError('invalid_campaign_backup_archive')
            try: payload=base64.b64decode(item['payload_b64'],validate=True)
            except Exception as exc: raise ValueError('invalid_campaign_backup_archive') from exc
            if hashlib.sha256(payload).hexdigest()!=sha: raise ValueError('campaign_backup_archive_integrity')
            try: events=json.loads(zlib.decompress(payload).decode('utf-8'))
            except Exception as exc: raise ValueError('invalid_campaign_backup_archive') from exc
            if not isinstance(events,list) or len(events)!=count: raise ValueError('invalid_campaign_backup_archive')
            for event in events:
                if not isinstance(event,dict) or not isinstance(event.get('event_id'),str): raise ValueError('invalid_campaign_backup_archive')
            if not isinstance(created,int) or isinstance(created,bool) or created<0: raise ValueError('invalid_campaign_backup_archive')
            archive_rows.append((seq,count,sha,payload,created))
        contract_json=None if contract_obj is None else json.dumps(contract_obj.snapshot(),ensure_ascii=False,sort_keys=True,separators=(',',':'))
        with self._lock:
            self._ensure_open()
            try:
                self._db.execute('BEGIN IMMEDIATE')
                exists=self._db.execute('SELECT 1 FROM campaigns WHERE campaign_id=?',(target_id,)).fetchone() is not None
                if exists and not replace:
                    raise ValueError('campaign_already_exists')
                if exists:
                    self._db.execute('DELETE FROM campaigns WHERE campaign_id=?',(target_id,))
                self._db.execute(
                    'INSERT INTO campaigns(campaign_id,system_id,state_version,state_json,contract_json,updated_at) VALUES(?,?,?,?,?,?)',
                    (target_id,state.system_id,state.state_version,state.to_json(),contract_json,int(document.get('updated_at') or time.time()*1000)),
                )
                for seq,count,sha,payload,created in archive_rows:
                    self._db.execute(
                        'INSERT INTO event_archives(campaign_id,seq,event_count,sha256,payload,created_at) VALUES(?,?,?,?,?,?)',
                        (target_id,seq,count,sha,payload,created),
                    )
                for cid,fp,ver,response_json,created in normalized:
                    self._db.execute(
                        'INSERT INTO command_receipts(campaign_id,command_id,fingerprint,state_version,response_json,created_at) VALUES(?,?,?,?,?,?)',
                        (target_id,cid,fp,ver,response_json,created),
                    )
                self._db.execute('COMMIT')
            except Exception:
                if self._db.in_transaction:
                    self._db.execute('ROLLBACK')
                raise
        return state

    def compact_event_log(self, campaign_id: str, *, keep_last=1000, chunk_size=500):
        """Move old canonical events into compressed, hash-verified durable chunks."""
        if not isinstance(keep_last,int) or isinstance(keep_last,bool) or keep_last<0: raise ValueError('invalid_keep_last')
        if not isinstance(chunk_size,int) or isinstance(chunk_size,bool) or not 1<=chunk_size<=5000: raise ValueError('invalid_chunk_size')
        with self._lock:
            self._ensure_open()
            row=self._db.execute('SELECT state_json,state_version FROM campaigns WHERE campaign_id=?',(campaign_id,)).fetchone()
            if row is None: raise KeyError(campaign_id)
            state=CampaignState.from_json(row[0])
            cut=max(0,len(state.event_log)-keep_last)
            if cut==0: return {'archived':0,'chunks':0,'state_version':state.state_version}
            events=deepcopy(state.event_log[:cut]); tail=deepcopy(state.event_log[cut:])
            next_seq=(self._db.execute('SELECT COALESCE(MAX(seq),-1)+1 FROM event_archives WHERE campaign_id=?',(campaign_id,)).fetchone()[0])
            chunks=[]
            for start in range(0,len(events),chunk_size):
                block=events[start:start+chunk_size]
                raw=json.dumps(block,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False).encode('utf-8')
                payload=zlib.compress(raw,9); sha=hashlib.sha256(payload).hexdigest()
                chunks.append((next_seq+len(chunks),len(block),sha,payload,int(time.time()*1000)))
            state.event_log=tail
            state.state_version += 1
            state.validate()
            try:
                self._db.execute('BEGIN IMMEDIATE')
                current=self._db.execute('SELECT state_version FROM campaigns WHERE campaign_id=?',(campaign_id,)).fetchone()
                if current is None or current[0]!=row[1]: raise StateVersionConflict('state_version_conflict')
                for seq,count,sha,payload,created in chunks:
                    self._db.execute('INSERT INTO event_archives(campaign_id,seq,event_count,sha256,payload,created_at) VALUES(?,?,?,?,?,?)',(campaign_id,seq,count,sha,payload,created))
                self._db.execute('UPDATE campaigns SET state_version=?,state_json=?,updated_at=? WHERE campaign_id=?',(state.state_version,state.to_json(),int(time.time()*1000),campaign_id))
                self._db.execute('COMMIT')
            except Exception:
                if self._db.in_transaction:self._db.execute('ROLLBACK')
                raise
            return {'archived':len(events),'chunks':len(chunks),'state_version':state.state_version}

    def load_event_history(self, campaign_id: str):
        with self._lock:
            self._ensure_open()
            rows=self._db.execute('SELECT seq,event_count,sha256,payload FROM event_archives WHERE campaign_id=? ORDER BY seq',(campaign_id,)).fetchall()
            state=self.load(campaign_id)
        events=[]
        for seq,count,sha,payload in rows:
            if hashlib.sha256(payload).hexdigest()!=sha: raise ValueError('event_archive_integrity_mismatch')
            try:block=json.loads(zlib.decompress(payload).decode('utf-8'))
            except Exception as exc: raise ValueError('invalid_event_archive') from exc
            if not isinstance(block,list) or len(block)!=count: raise ValueError('invalid_event_archive')
            events.extend(block)
        events.extend(deepcopy(state.event_log))
        ids=[e.get('event_id') for e in events if isinstance(e,dict)]
        if len(ids)!=len(set(ids)): raise ValueError('duplicate_event_id_across_archive')
        return events

    def command_count(self, campaign_id: str) -> int:
        with self._lock:
            self._ensure_open()
            row = self._db.execute('SELECT COUNT(*) FROM command_receipts WHERE campaign_id=?', (campaign_id,)).fetchone()
        return int(row[0])
