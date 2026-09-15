from __future__ import annotations
import hashlib, json, sqlite3, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = (ROOT / 'tools' / 'm5_schema_v1.sql').read_text()

def sha(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()

def create(conn, cid='c1'):
    state='{"revision":0}'
    checksum=sha(state)
    conn.execute("INSERT INTO campaign_meta VALUES (?,?,?,?,?,?,?,?,?)", (cid,'Teste',0,1,1,checksum,'splitmix64-counter-v1','seed',1))
    conn.execute("INSERT INTO campaign_state VALUES (?,?,?,?)", (cid,0,state,checksum))
    conn.execute("INSERT INTO campaign_snapshot VALUES (?,?,?,?,?,?,?,?,?,?)", ('s0',cid,0,state,checksum,1,'splitmix64-counter-v1','{}',None,1))
    conn.commit()

def load(conn,cid):
    meta=conn.execute('SELECT revision,state_checksum FROM campaign_meta WHERE campaign_id=?',(cid,)).fetchone()
    if not meta: return ('NOT_FOUND',None)
    state=conn.execute('SELECT revision,state_json,checksum FROM campaign_state WHERE campaign_id=?',(cid,)).fetchone()
    if not state: return ('CORRUPT','missing-state')
    if meta[0]!=state[0]: return ('CORRUPT','revision')
    actual=sha(state[1])
    if actual!=state[2] or actual!=meta[1]: return ('CORRUPT','checksum')
    return ('OK',state[0])

def atomic_commit(conn, expected, fail=False):
    try:
        conn.execute('BEGIN IMMEDIATE')
        meta=conn.execute("SELECT revision,state_checksum FROM campaign_meta WHERE campaign_id='c1'").fetchone()
        state=conn.execute("SELECT revision,checksum FROM campaign_state WHERE campaign_id='c1'").fetchone()
        if meta[0] != expected or state[0] != expected:
            raise RuntimeError('REVISION_CONFLICT')
        newrev=expected+1; newstate=json.dumps({'revision':newrev},separators=(',',':')); newsha=sha(newstate)
        conn.execute("INSERT INTO campaign_event VALUES (?,?,?,?,?,?,?,?)", ('e'+str(newrev),'c1',newrev,'test','{}',state[1],newsha,2))
        if fail: raise RuntimeError('INJECTED_FAILURE')
        conn.execute("UPDATE campaign_state SET revision=?,state_json=?,checksum=? WHERE campaign_id='c1'",(newrev,newstate,newsha))
        conn.execute("UPDATE campaign_meta SET revision=?,state_checksum=?,updated_at_audit=? WHERE campaign_id='c1'",(newrev,newsha,2))
        conn.commit(); return 'OK'
    except:
        conn.rollback(); raise

def main():
    with tempfile.TemporaryDirectory() as td:
        path=Path(td)/'wave1.db'
        conn=sqlite3.connect(path)
        conn.executescript(SCHEMA)
        assert conn.execute('PRAGMA user_version').fetchone()[0] == 1
        create(conn)
        assert load(conn,'c1') == ('OK',0)
        assert load(conn,'missing') == ('NOT_FOUND',None)
        try: atomic_commit(conn,9)
        except RuntimeError as e: assert str(e)=='REVISION_CONFLICT'
        assert load(conn,'c1') == ('OK',0)
        try: atomic_commit(conn,0,True)
        except RuntimeError as e: assert str(e)=='INJECTED_FAILURE'
        assert load(conn,'c1') == ('OK',0)
        assert conn.execute("SELECT COUNT(*) FROM campaign_event").fetchone()[0] == 0
        assert atomic_commit(conn,0) == 'OK'
        assert load(conn,'c1') == ('OK',1)
        conn.execute("UPDATE campaign_state SET state_json='corrupt' WHERE campaign_id='c1'"); conn.commit()
        status,_=load(conn,'c1'); assert status == 'CORRUPT'
        print('SQLITE_CONTRACT_PASS schema=1 create/load=PASS revision_conflict=PASS rollback=PASS corrupt_read=PASS')

if __name__ == '__main__': main()
