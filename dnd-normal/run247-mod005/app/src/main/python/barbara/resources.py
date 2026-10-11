from __future__ import annotations

import hashlib
import json
import sqlite3
import time
import zipfile
import re
import unicodedata
from copy import deepcopy
from pathlib import Path, PurePosixPath


_TEXT_EXTENSIONS = {'.txt', '.md', '.markdown', '.json', '.jsonl'}
_RAG_EXTENSIONS = _TEXT_EXTENSIONS | {'.pdf'}
_ATLAS_EXTENSIONS = {'.json', '.jsonl'}
_ATLAS_BUNDLE_FORMAT = 'barbara-atlas-bundle-v1'
_ATLAS_DIRECTORY_FORMAT = 'directory-json-v1'
_MAX_ATLAS_BUNDLE_FILES = 50000
_MAX_ATLAS_BUNDLE_UNCOMPRESSED = 4 * 1024 * 1024 * 1024
_MAX_ATLAS_MEMBER_BYTES = 256 * 1024 * 1024


def _sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def _canonical_source_id(namespace: str, relative: str) -> str:
    raw = f'{namespace}/{relative}'.replace('\\', '/')
    if len(raw) <= 160:
        return raw
    digest = hashlib.sha256(raw.encode('utf-8')).hexdigest()[:24]
    return raw[:130] + ':' + digest


def _iter_files(root: Path, extensions: set[str]):
    if root.is_file():
        if root.suffix.lower() in extensions:
            yield root, root.name
        return
    if not root.is_dir():
        raise FileNotFoundError(str(root))
    for path in sorted(root.rglob('*')):
        if path.is_file() and path.suffix.lower() in extensions:
            yield path, path.relative_to(root).as_posix()


def _site_id(record, fallback=None):
    if not isinstance(record, dict):
        raise ValueError('invalid_atlas_site')
    value = record.get('id', record.get('site_id', record.get('hex_id', fallback)))
    if value is None:
        raise ValueError('atlas_site_id_missing')
    value = str(value)
    if not value or len(value) > 300:
        raise ValueError('invalid_atlas_site_id')
    return value


def _iter_sites_from_json(value, fallback_prefix=''):
    if isinstance(value, dict) and 'sites' in value:
        value = value['sites']
    if isinstance(value, list):
        for item in value:
            sid = _site_id(item)
            yield sid, deepcopy(item)
        return
    if isinstance(value, dict):
        if any(key in value for key in ('id', 'site_id', 'hex_id')):
            sid = _site_id(value, fallback_prefix or None)
            yield sid, deepcopy(value)
            return
        for key, item in value.items():
            if not isinstance(item, dict):
                raise ValueError('invalid_atlas_site')
            row = deepcopy(item)
            row.setdefault('id', str(key))
            sid = _site_id(row, str(key))
            yield sid, row
        return
    raise ValueError('invalid_atlas_document')



def _atlas_key(value):
    if value is None:
        return ''
    text = unicodedata.normalize('NFKD', str(value)).encode('ascii','ignore').decode('ascii').lower().strip()
    text = re.sub(r'[^a-z0-9_.:-]+', ' ', text)
    return ' '.join(text.split())


def _atlas_lookup_keys(site_id, site):
    keys=[]
    def add(kind,value,rank):
        key=_atlas_key(value)
        item=(kind,key,rank)
        if key and item not in keys:
            keys.append(item)
    add('id',site_id,0)
    for field,kind,rank in (('name','name',1),('title','name',1),('hex_id','hex',2),('hex_number','hex',2),('hex','hex',2),('number','hex',3)):
        if field in site:
            add(kind,site.get(field),rank)
    aliases=site.get('aliases',site.get('alias',[]))
    if isinstance(aliases,str):
        aliases=[aliases]
    if isinstance(aliases,(list,tuple)):
        for alias in aliases:
            add('alias',alias,2)
    coords=site.get('coordinates',site.get('coord'))
    if not isinstance(coords,dict) and ('q' in site or 'r' in site):
        coords={'q':site.get('q'),'r':site.get('r')}
    if isinstance(coords,dict) and coords.get('q') is not None and coords.get('r') is not None:
        add('qr','qr:%s,%s' % (coords.get('q'),coords.get('r')),1)
        add('qr','%s,%s' % (coords.get('q'),coords.get('r')),2)
    original=list(keys)
    for kind,key,rank in original:
        if kind=='hex':
            if key.isdigit():
                add('hex','hex '+key,rank)
            match=re.fullmatch(r'hex[ _:-]*(\d+)',key)
            if match:
                add('hex',match.group(1),rank+1)
    return tuple(keys)

def _safe_bundle_path(value: str) -> str:
    if not isinstance(value, str) or not value or '\\' in value or '\x00' in value:
        raise ValueError('invalid_atlas_bundle_path')
    path = PurePosixPath(value)
    if path.is_absolute() or any(part in {'', '.', '..'} for part in path.parts):
        raise ValueError('invalid_atlas_bundle_path')
    return path.as_posix()


class AtlasBundleReader:
    """Reads one immutable system-owned Atlas ZIP without extracting it.

    The game/system package owns the Atlas archive and its manifest. Barbara only
    validates and indexes the normalized site records for fast World queries.
    """

    def __init__(self, bundle_path: Path, *, manifest_name='atlas-manifest.json'):
        self.bundle_path = Path(bundle_path)
        self.manifest_name = _safe_bundle_path(manifest_name)

    def _manifest(self, archive: zipfile.ZipFile):
        infos = archive.infolist()
        if not infos or len(infos) > _MAX_ATLAS_BUNDLE_FILES:
            raise ValueError('invalid_atlas_bundle_size')
        total = 0
        names = set()
        for info in infos:
            if info.filename.endswith('/'):
                continue
            name = _safe_bundle_path(info.filename)
            if name in names:
                raise ValueError('duplicate_atlas_bundle_path')
            names.add(name)
            if info.file_size < 0 or info.file_size > _MAX_ATLAS_MEMBER_BYTES:
                raise ValueError('atlas_bundle_member_too_large')
            total += info.file_size
            if total > _MAX_ATLAS_BUNDLE_UNCOMPRESSED:
                raise ValueError('atlas_bundle_too_large')
        if self.manifest_name not in names:
            raise ValueError('atlas_bundle_manifest_missing')
        try:
            raw = json.loads(archive.read(self.manifest_name).decode('utf-8'))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ValueError('invalid_atlas_bundle_manifest') from exc
        if not isinstance(raw, dict) or raw.get('schema') != 1 or raw.get('format') != _ATLAS_BUNDLE_FORMAT:
            raise ValueError('unsupported_atlas_bundle_manifest')
        files = raw.get('files')
        if not isinstance(files, list) or not files:
            raise ValueError('invalid_atlas_bundle_files')
        normalized = []
        seen = set()
        for item in files:
            if isinstance(item, str):
                path, expected = item, None
            elif isinstance(item, dict) and set(item) <= {'path', 'sha256'} and 'path' in item:
                path, expected = item['path'], item.get('sha256')
            else:
                raise ValueError('invalid_atlas_bundle_file')
            path = _safe_bundle_path(path)
            if Path(path).suffix.lower() not in _ATLAS_EXTENSIONS or path == self.manifest_name:
                raise ValueError('invalid_atlas_bundle_file')
            if path in seen or path not in names:
                raise ValueError('invalid_atlas_bundle_file')
            seen.add(path)
            if expected is not None:
                if not isinstance(expected, str) or len(expected) != 64 or any(c not in '0123456789abcdef' for c in expected):
                    raise ValueError('invalid_atlas_bundle_digest')
            normalized.append((path, expected))
        return raw, tuple(normalized)

    def iter_sites(self):
        seen_sites = set()
        with zipfile.ZipFile(self.bundle_path, 'r') as archive:
            _, files = self._manifest(archive)
            for rel, expected in files:
                raw = archive.read(rel)
                if expected is not None and hashlib.sha256(raw).hexdigest() != expected:
                    raise ValueError('atlas_bundle_integrity_mismatch:' + rel)
                if rel.lower().endswith('.jsonl'):
                    try:
                        text = raw.decode('utf-8')
                    except UnicodeDecodeError as exc:
                        raise ValueError('invalid_atlas_bundle_utf8:' + rel) from exc
                    for line_no, line in enumerate(text.splitlines(), 1):
                        if not line.strip():
                            continue
                        try:
                            row = json.loads(line)
                        except json.JSONDecodeError as exc:
                            raise ValueError(f'invalid_atlas_jsonl:{rel}:{line_no}') from exc
                        sid = _site_id(row)
                        if sid in seen_sites:
                            raise ValueError('duplicate_atlas_site:' + sid)
                        seen_sites.add(sid)
                        yield sid, row
                    continue
                try:
                    value = json.loads(raw.decode('utf-8'))
                except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                    raise ValueError('invalid_atlas_json:' + rel) from exc
                for sid, row in _iter_sites_from_json(value, Path(rel).stem):
                    if sid in seen_sites:
                        raise ValueError('duplicate_atlas_site:' + sid)
                    seen_sites.add(sid)
                    yield sid, row


class WorldAtlasIndex:
    """World-scoped, queryable index for very large immutable Atlas datasets.

    Package Atlas data remains outside CampaignState. Only dynamic overrides live in
    canonical state. This prevents thousands of hexes from inflating every save while
    still making geography authoritative to ContextEngine.
    """

    def __init__(self, db_path: str, datasets):
        self.db_path = str(db_path)
        self.datasets = tuple(deepcopy(list(datasets)))
        Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)
        self._db = sqlite3.connect(self.db_path, timeout=30, check_same_thread=False, isolation_level=None)
        self._db.execute('PRAGMA journal_mode=WAL')
        self._db.execute('PRAGMA synchronous=FULL')
        self._db.execute('PRAGMA busy_timeout=30000')
        self._db.executescript('''
            CREATE TABLE IF NOT EXISTS atlas_datasets(
                namespace TEXT PRIMARY KEY,
                fingerprint TEXT NOT NULL,
                site_count INTEGER NOT NULL,
                indexed_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS atlas_sites(
                namespace TEXT NOT NULL,
                site_id TEXT NOT NULL,
                site_json TEXT NOT NULL,
                PRIMARY KEY(namespace, site_id),
                FOREIGN KEY(namespace) REFERENCES atlas_datasets(namespace) ON DELETE CASCADE
            );
            CREATE INDEX IF NOT EXISTS idx_atlas_sites_site ON atlas_sites(site_id);
            CREATE TABLE IF NOT EXISTS atlas_lookup(
                namespace TEXT NOT NULL,
                lookup_key TEXT NOT NULL,
                kind TEXT NOT NULL,
                rank INTEGER NOT NULL,
                site_id TEXT NOT NULL,
                PRIMARY KEY(namespace, lookup_key, kind, site_id),
                FOREIGN KEY(namespace, site_id) REFERENCES atlas_sites(namespace, site_id) ON DELETE CASCADE
            );
            CREATE INDEX IF NOT EXISTS idx_atlas_lookup_key ON atlas_lookup(lookup_key, rank);
        ''')
        self._active = tuple(d['namespace'] for d in self.datasets if d.get('kind') == 'atlas' and d.get('active') and d.get('resolved_path'))

    def close(self):
        if self._db is not None:
            self._db.close(); self._db = None

    def _ensure_open(self):
        if self._db is None:
            raise RuntimeError('atlas_index_closed')

    def _fingerprint(self, dataset):
        root = Path(dataset['resolved_path'])
        if not root.exists():
            raise FileNotFoundError(str(root))
        if not dataset.get('mutable'):
            payload = {
                'namespace': dataset['namespace'],
                'package_id': dataset.get('package_id'),
                'package_version': dataset.get('package_version'),
                'revision': dataset.get('revision'),
                'path': dataset.get('path'),
            }
            return hashlib.sha256(json.dumps(payload, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        if root.is_file():
            return _sha256_file(root)
        h = hashlib.sha256()
        for path, rel in _iter_files(root, _ATLAS_EXTENSIONS):
            h.update(rel.encode('utf-8')); h.update(b'\0'); h.update(_sha256_file(path).encode('ascii')); h.update(b'\n')
        return h.hexdigest()

    def _read_sites(self, root: Path, dataset):
        dataset_format = dataset.get('format') or _ATLAS_DIRECTORY_FORMAT
        if dataset_format == _ATLAS_BUNDLE_FORMAT:
            if not root.is_file() or root.suffix.lower() != '.zip':
                raise ValueError('atlas_bundle_path_required')
            manifest_name = (dataset.get('metadata') or {}).get('bundle_manifest', 'atlas-manifest.json')
            yield from AtlasBundleReader(root, manifest_name=manifest_name).iter_sites()
            return
        if dataset_format != _ATLAS_DIRECTORY_FORMAT:
            raise ValueError('unsupported_atlas_dataset_format:' + str(dataset_format))
        seen = set()
        for path, rel in _iter_files(root, _ATLAS_EXTENSIONS):
            if path.suffix.lower() == '.jsonl':
                for line_no, line in enumerate(path.read_text('utf-8').splitlines(), 1):
                    if not line.strip():
                        continue
                    try:
                        row = json.loads(line)
                    except json.JSONDecodeError as exc:
                        raise ValueError(f'invalid_atlas_jsonl:{rel}:{line_no}') from exc
                    sid = _site_id(row)
                    if sid in seen:
                        raise ValueError('duplicate_atlas_site:' + sid)
                    seen.add(sid); yield sid, row
                continue
            try:
                value = json.loads(path.read_text('utf-8'))
            except json.JSONDecodeError as exc:
                raise ValueError('invalid_atlas_json:' + rel) from exc
            for sid, row in _iter_sites_from_json(value, path.stem):
                if sid in seen:
                    raise ValueError('duplicate_atlas_site:' + sid)
                seen.add(sid); yield sid, row

    def sync(self):
        self._ensure_open()
        results = []
        namespaces = set()
        for dataset in self.datasets:
            if dataset.get('kind') != 'atlas' or not dataset.get('resolved_path'):
                continue
            namespace = dataset['namespace']
            if namespace in namespaces:
                raise ValueError('dataset_namespace_collision:' + namespace)
            namespaces.add(namespace)
            fingerprint = self._fingerprint(dataset)
            row = self._db.execute('SELECT fingerprint,site_count FROM atlas_datasets WHERE namespace=?', (namespace,)).fetchone()
            lookup_count=self._db.execute('SELECT COUNT(*) FROM atlas_lookup WHERE namespace=?',(namespace,)).fetchone()[0]
            if row is not None and row[0] == fingerprint and (row[1]==0 or lookup_count>0):
                results.append({'namespace': namespace, 'status': 'current', 'site_count': row[1]})
                continue
            root = Path(dataset['resolved_path'])
            try:
                self._db.execute('BEGIN IMMEDIATE')
                self._db.execute('DELETE FROM atlas_lookup WHERE namespace=?', (namespace,))
                self._db.execute('DELETE FROM atlas_sites WHERE namespace=?', (namespace,))
                self._db.execute('DELETE FROM atlas_datasets WHERE namespace=?', (namespace,))
                count = 0
                for sid, site in self._read_sites(root, dataset):
                    payload = json.dumps(site, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)
                    self._db.execute('INSERT INTO atlas_sites(namespace,site_id,site_json) VALUES(?,?,?)', (namespace, sid, payload))
                    for kind,key,rank in _atlas_lookup_keys(sid,site):
                        self._db.execute('INSERT OR IGNORE INTO atlas_lookup(namespace,lookup_key,kind,rank,site_id) VALUES(?,?,?,?,?)',(namespace,key,kind,rank,sid))
                    count += 1
                self._db.execute(
                    'INSERT INTO atlas_datasets(namespace,fingerprint,site_count,indexed_at) VALUES(?,?,?,?)',
                    (namespace, fingerprint, count, int(time.time() * 1000)),
                )
                self._db.execute('COMMIT')
            except Exception:
                if self._db.in_transaction:
                    self._db.execute('ROLLBACK')
                raise
            results.append({'namespace': namespace, 'status': 'indexed', 'site_count': count})
        stale = [row[0] for row in self._db.execute('SELECT namespace FROM atlas_datasets').fetchall() if row[0] not in namespaces]
        if stale:
            with self._db:
                for namespace in stale:
                    self._db.execute('DELETE FROM atlas_sites WHERE namespace=?', (namespace,))
                    self._db.execute('DELETE FROM atlas_datasets WHERE namespace=?', (namespace,))
        return tuple(results)

    def set_active_namespaces(self, namespaces):
        namespaces = tuple(str(x) for x in namespaces)
        known = {row[0] for row in self._db.execute('SELECT namespace FROM atlas_datasets').fetchall()}
        if any(x not in known for x in namespaces):
            raise ValueError('unknown_atlas_namespace')
        self._active = namespaces

    @property
    def active_namespaces(self):
        return self._active

    def get_site(self, site_id: str):
        self._ensure_open()
        if not isinstance(site_id, str) or not site_id:
            return None
        if not self._active:
            return None
        placeholders = ','.join('?' for _ in self._active)
        rows = self._db.execute(
            f'SELECT namespace,site_json FROM atlas_sites WHERE site_id=? AND namespace IN ({placeholders}) ORDER BY namespace',
            (site_id, *self._active),
        ).fetchall()
        if not rows:
            return None
        values = [(ns, json.loads(raw)) for ns, raw in rows]
        if len(values) > 1:
            normalized = {json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')) for _, value in values}
            if len(normalized) > 1:
                raise ValueError('atlas_site_ambiguous:' + site_id)
        result = deepcopy(values[0][1])
        result.setdefault('_atlas_namespace', values[0][0])
        return result

    def resolve_site(self, reference, *, namespace=None):
        """Resolve names, aliases, hex numbers and q/r coordinates across active atlases."""
        self._ensure_open()
        if isinstance(reference,dict):
            if reference.get('site_id') or reference.get('id'):
                raw=reference.get('site_id',reference.get('id'))
            elif reference.get('q') is not None and reference.get('r') is not None:
                raw='qr:%s,%s' % (reference.get('q'),reference.get('r'))
            elif reference.get('hex') is not None:
                raw='hex %s' % reference.get('hex')
            else:
                return None
        else:
            raw=reference
        key=_atlas_key(raw)
        if not key or not self._active:
            return None
        namespaces=(namespace,) if namespace is not None else self._active
        if any(ns not in self._active for ns in namespaces):
            return None
        placeholders=','.join('?' for _ in namespaces)
        query=(
            'SELECT l.namespace,l.site_id,l.kind,l.rank,s.site_json '
            'FROM atlas_lookup l JOIN atlas_sites s ON s.namespace=l.namespace AND s.site_id=l.site_id '
            'WHERE l.lookup_key=? AND l.namespace IN ('+placeholders+')'
        )
        rows=self._db.execute(query,(key,*namespaces)).fetchall()
        if not rows and key.startswith('hex '):
            rows=self._db.execute(query,(key[4:].strip(),*namespaces)).fetchall()
        if not rows:
            return None
        priority={ns:i for i,ns in enumerate(namespaces)}
        rows.sort(key=lambda row:(priority.get(row[0],10**6),int(row[3]),row[1]))
        chosen=rows[0]
        result=deepcopy(json.loads(chosen[4]))
        result.setdefault('_atlas_namespace',chosen[0])
        result.setdefault('id',chosen[1])
        result['_atlas_resolution']={
            'reference':str(raw),'normalized':key,'site_id':chosen[1],'namespace':chosen[0],
            'match_kind':chosen[2],
            'alternatives':[{'namespace':row[0],'site_id':row[1],'kind':row[2]} for row in rows[1:16]],
        }
        return result

    def snapshot(self):
        self._ensure_open()
        rows = self._db.execute('SELECT namespace,site_count,indexed_at FROM atlas_datasets ORDER BY namespace').fetchall()
        return {
            'active_namespaces': list(self._active),
            'datasets': [{'namespace': ns, 'site_count': count, 'indexed_at': indexed} for ns, count, indexed in rows],
        }


class WorldResourceRuntime:
    """Mounts system/module-owned resources into one running Barbara World.

    Atlas archives remain owned by the installed game package. Barbara does not
    become the source of those maps: it validates the package-declared contract and
    builds a World-local query index used by ContextEngine/VTT.
    """

    def __init__(self, *, world, datasets, data_path: str, system_module=None):
        self.world = world
        self.system_module = system_module
        self.datasets = tuple(deepcopy(list(datasets)))
        if system_module is not None:
            contract = dict(system_module.atlas_contract)
            allowed = set(contract.get('formats') or ())
            for dataset in self.datasets:
                if dataset.get('kind') != 'atlas':
                    continue
                if dataset.get('owner_system_id') not in (None, system_module.system_id):
                    raise ValueError('atlas_dataset_wrong_system_owner')
                fmt = dataset.get('format') or _ATLAS_DIRECTORY_FORMAT
                if fmt not in allowed:
                    raise ValueError('atlas_format_not_supported_by_system:' + str(fmt))
                if fmt == _ATLAS_BUNDLE_FORMAT:
                    metadata = dataset.setdefault('metadata', {})
                    metadata.setdefault('bundle_manifest', contract.get('bundle_manifest', 'atlas-manifest.json'))
        self.data_path = Path(data_path).resolve(); self.data_path.mkdir(parents=True, exist_ok=True)
        self._db = sqlite3.connect(str(self.data_path / 'resources.db'), timeout=30, check_same_thread=False, isolation_level=None)
        self._db.execute('PRAGMA journal_mode=WAL'); self._db.execute('PRAGMA synchronous=FULL')
        self._db.executescript('''
            CREATE TABLE IF NOT EXISTS rag_sources(
                namespace TEXT NOT NULL,
                relative_path TEXT NOT NULL,
                source_id TEXT NOT NULL,
                source_hash TEXT NOT NULL,
                revision TEXT,
                status TEXT NOT NULL,
                result_json TEXT,
                error TEXT,
                indexed_at INTEGER NOT NULL,
                PRIMARY KEY(namespace, relative_path)
            );
        ''')
        self.atlas = WorldAtlasIndex(str(self.data_path / 'atlas-index.db'), self.datasets)
        self.atlas.sync()
        self._engine = None

    def bind_engine(self, engine):
        self._engine = engine
        return self

    def close(self):
        self.atlas.close()
        if self._db is not None:
            self._db.close(); self._db = None
        self._engine = None

    def _metadata(self, dataset):
        metadata = deepcopy(dataset.get('metadata') or {})
        kind = str(metadata.pop('kind', 'RULE')).upper()
        authority = metadata.pop('authority', 1.0)
        secret = metadata.pop('secret', False)
        language = str(metadata.pop('language', 'pt'))
        source_type = str(metadata.pop('source_type', 'official_rule'))
        canonicality = str(metadata.pop('canonicality', 'official'))
        priority = metadata.pop('priority', 50)
        if not isinstance(authority, (int, float)) or isinstance(authority, bool) or not 0 <= authority <= 1:
            raise ValueError('invalid_dataset_authority')
        if not isinstance(secret, bool):
            raise ValueError('invalid_dataset_secret')
        if not isinstance(priority, int) or isinstance(priority, bool) or not 0 <= priority <= 100:
            raise ValueError('invalid_dataset_priority')
        return kind, float(authority), secret, {
            'language': language, 'source_type': source_type, 'canonicality': canonicality, 'priority': priority,
            **metadata,
        }

    def _text_pages(self, path: Path):
        raw = path.read_text('utf-8')
        if path.suffix.lower() == '.json':
            try:
                value = json.loads(raw)
            except json.JSONDecodeError:
                value = None
            if isinstance(value, dict) and isinstance(value.get('pages'), list):
                pages = value['pages']
                if all(isinstance(p, dict) and isinstance(p.get('page'), int) and isinstance(p.get('text'), str) for p in pages):
                    return deepcopy(pages)
        return [{'page': 1, 'text': raw}]

    def index_rag(self, *, limit_sources: int | None = None, fail_fast: bool = False):
        if self._engine is None:
            raise RuntimeError('world_resources_engine_not_bound')
        if limit_sources is not None and (not isinstance(limit_sources, int) or isinstance(limit_sources, bool) or limit_sources < 1):
            raise ValueError('invalid_source_limit')
        candidates = []
        for dataset in self.datasets:
            if dataset.get('kind') != 'rag_corpus' or not dataset.get('active') or not dataset.get('resolved_path'):
                continue
            root = Path(dataset['resolved_path'])
            for path, rel in _iter_files(root, _RAG_EXTENSIONS):
                candidates.append((dataset, path, rel))
        candidates.sort(key=lambda row: (row[0]['namespace'], row[2]))
        if limit_sources is not None:
            candidates = candidates[:limit_sources]
        active_keys = {(d['namespace'], rel) for d, _, rel in candidates}
        results = []
        for dataset, path, rel in candidates:
            namespace = dataset['namespace']; digest = _sha256_file(path); revision = str(dataset.get('revision') or '')
            prior = self._db.execute(
                'SELECT source_id,source_hash,revision,status,result_json FROM rag_sources WHERE namespace=? AND relative_path=?',
                (namespace, rel),
            ).fetchone()
            if prior and prior[1] == digest and prior[2] == revision and prior[3] == 'complete':
                results.append({'namespace': namespace, 'path': rel, 'source_id': prior[0], 'status': 'current', 'result': json.loads(prior[4]) if prior[4] else None})
                continue
            source_id = _canonical_source_id(namespace, rel)
            try:
                kind, authority, secret, metadata = self._metadata(dataset)
                if path.suffix.lower() == '.pdf':
                    result = self._engine.ingest_pdf(
                        str(path), source_id=source_id, system_id=self.world.system_id, campaign_id=self.world.campaign_id,
                        kind=kind, authority=authority, secret=secret, **metadata,
                    )
                else:
                    result = self._engine.ingest_pages(
                        self._text_pages(path), source_id=source_id, system_id=self.world.system_id, campaign_id=self.world.campaign_id,
                        kind=kind, authority=authority, secret=secret, **metadata,
                    )
                serializable = deepcopy(result)
                if isinstance(serializable.get('checksums'), tuple):
                    serializable['checksums'] = list(serializable['checksums'])
                payload = json.dumps(serializable, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)
                with self._db:
                    self._db.execute(
                        'INSERT OR REPLACE INTO rag_sources(namespace,relative_path,source_id,source_hash,revision,status,result_json,error,indexed_at) VALUES(?,?,?,?,?,?,?,?,?)',
                        (namespace, rel, source_id, digest, revision, 'complete', payload, None, int(time.time() * 1000)),
                    )
                results.append({'namespace': namespace, 'path': rel, 'source_id': source_id, 'status': 'indexed', 'result': serializable})
            except Exception as exc:
                with self._db:
                    self._db.execute(
                        'INSERT OR REPLACE INTO rag_sources(namespace,relative_path,source_id,source_hash,revision,status,result_json,error,indexed_at) VALUES(?,?,?,?,?,?,?,?,?)',
                        (namespace, rel, source_id, digest, revision, 'failed', None, str(exc)[:1000], int(time.time() * 1000)),
                    )
                results.append({'namespace': namespace, 'path': rel, 'source_id': source_id, 'status': 'failed', 'error': str(exc)})
                if fail_fast:
                    raise
        # Sources removed from immutable packages must not remain retrievable forever.
        if limit_sources is None:
            stale = self._db.execute('SELECT namespace,relative_path,source_id FROM rag_sources').fetchall()
            known_rag_namespaces = {d['namespace'] for d in self.datasets if d.get('kind') == 'rag_corpus' and d.get('active')}
            for namespace, rel, source_id in stale:
                if namespace in known_rag_namespaces and (namespace, rel) not in active_keys:
                    self._engine.rag.delete_source(self.world.campaign_id, self.world.system_id, source_id)
                    with self._db:
                        self._db.execute('DELETE FROM rag_sources WHERE namespace=? AND relative_path=?', (namespace, rel))
        return tuple(results)

    def rag_status(self):
        rows = self._db.execute(
            'SELECT namespace,relative_path,source_id,source_hash,revision,status,error,indexed_at FROM rag_sources ORDER BY namespace,relative_path'
        ).fetchall()
        return tuple({
            'namespace': row[0], 'path': row[1], 'source_id': row[2], 'source_hash': row[3], 'revision': row[4],
            'status': row[5], 'error': row[6], 'indexed_at': row[7],
        } for row in rows)

    def snapshot(self):
        return {
            'data_path': str(self.data_path),
            'atlas': self.atlas.snapshot(),
            'rag': list(self.rag_status()),
            'datasets': deepcopy(list(self.datasets)),
        }
