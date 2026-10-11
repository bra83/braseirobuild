from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import sqlite3
import stat
import tempfile
import threading
import time
import zipfile
from copy import deepcopy
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from types import MappingProxyType

from .host_contract import BARBARA_PROTOCOL_VERSION, HostIntegrationLoader
from .system_module import SystemModule, SystemModuleConformance, SystemModuleLoader
from .module_extension import ModuleExtensionLoader


PACKAGE_SCHEMA_VERSION = 1
PACKAGE_TYPES = frozenset({'system', 'module', 'world'})
PACKAGE_ID_RE = re.compile(r'^[a-z0-9][a-z0-9._-]{1,79}$')
SHA256_RE = re.compile(r'^[0-9a-f]{64}$')
MAX_ARCHIVE_FILES = 20000
MAX_ARCHIVE_UNCOMPRESSED = 4 * 1024 * 1024 * 1024
MAX_FILE_UNCOMPRESSED = 512 * 1024 * 1024
MAX_MANIFEST_BYTES = 2 * 1024 * 1024


def _freeze(value, code='invalid_package_mapping'):
    if not isinstance(value, dict):
        raise ValueError(code)
    return MappingProxyType(deepcopy(value))


def _safe_relpath(value: str, code='invalid_package_path') -> str:
    if not isinstance(value, str) or not value or '\\' in value or '\x00' in value:
        raise ValueError(code)
    path = PurePosixPath(value)
    if path.is_absolute() or any(part in {'', '.', '..'} for part in path.parts):
        raise ValueError(code)
    return path.as_posix()


def _version_key(value: str):
    """Deterministic semver-ish comparator without a third-party dependency."""
    if not isinstance(value, str) or not value or len(value) > 80:
        raise ValueError('invalid_package_version')
    main, sep, suffix = value.partition('-')
    parts = main.split('.')
    if not all(part.isdigit() for part in parts):
        raise ValueError('invalid_package_version')
    nums = tuple(int(part) for part in parts)
    # Release > prerelease for the same numeric tuple.
    prerelease_rank = 1 if not sep else 0
    suffix_key = tuple(re.findall(r'\d+|[A-Za-z]+', suffix.lower())) if sep else ()
    normalized = tuple((0, int(x)) if x.isdigit() else (1, x) for x in suffix_key)
    return nums, prerelease_rank, normalized


def _version_in_range(version: str, compatibility: dict | MappingProxyType | None) -> bool:
    if not compatibility:
        return True
    if not isinstance(compatibility, (dict, MappingProxyType)):
        raise ValueError('invalid_package_relationship_compatibility')
    unknown = set(compatibility) - {'minimum', 'verified', 'maximum'}
    if unknown:
        raise ValueError('unknown_package_relationship_compatibility')
    key = _version_key(version)
    minimum = compatibility.get('minimum')
    maximum = compatibility.get('maximum')
    if minimum is not None and key < _version_key(str(minimum)):
        return False
    if maximum is not None and key > _version_key(str(maximum)):
        return False
    return True


@dataclass(frozen=True)
class PackageCompatibility:
    minimum_protocol: int
    verified_protocol: int
    maximum_protocol: int | None = None

    def __post_init__(self):
        for value in (self.minimum_protocol, self.verified_protocol):
            if not isinstance(value, int) or isinstance(value, bool) or value < 1:
                raise ValueError('invalid_package_protocol_compatibility')
        if self.maximum_protocol is not None:
            if not isinstance(self.maximum_protocol, int) or isinstance(self.maximum_protocol, bool) or self.maximum_protocol < 1:
                raise ValueError('invalid_package_protocol_compatibility')
            if self.maximum_protocol < self.minimum_protocol:
                raise ValueError('invalid_package_protocol_compatibility')
        if self.verified_protocol < self.minimum_protocol:
            raise ValueError('invalid_package_protocol_compatibility')

    def supports(self, protocol_version: int) -> bool:
        if not isinstance(protocol_version, int) or isinstance(protocol_version, bool):
            return False
        if protocol_version < self.minimum_protocol:
            return False
        if self.maximum_protocol is not None and protocol_version > self.maximum_protocol:
            return False
        return True

    def snapshot(self):
        return {
            'minimum': self.minimum_protocol,
            'verified': self.verified_protocol,
            'maximum': self.maximum_protocol,
        }


@dataclass(frozen=True)
class BarbaraPackageManifest:
    package_id: str
    title: str
    package_type: str
    version: str
    compatibility: PackageCompatibility
    entrypoints: MappingProxyType
    relationships: MappingProxyType
    datasets: tuple
    integrity: MappingProxyType
    metadata: MappingProxyType
    schema: int = PACKAGE_SCHEMA_VERSION

    def __post_init__(self):
        if self.schema != PACKAGE_SCHEMA_VERSION:
            raise ValueError('unsupported_package_schema')
        if not isinstance(self.package_id, str) or not PACKAGE_ID_RE.fullmatch(self.package_id):
            raise ValueError('invalid_package_id')
        if not isinstance(self.title, str) or not self.title.strip() or len(self.title) > 200:
            raise ValueError('invalid_package_title')
        if self.package_type not in PACKAGE_TYPES:
            raise ValueError('invalid_package_type')
        _version_key(self.version)
        if not isinstance(self.entrypoints, MappingProxyType):
            raise ValueError('mutable_package_entrypoints')
        if not isinstance(self.relationships, MappingProxyType):
            raise ValueError('mutable_package_relationships')
        if not isinstance(self.integrity, MappingProxyType):
            raise ValueError('mutable_package_integrity')
        if not isinstance(self.metadata, MappingProxyType):
            raise ValueError('mutable_package_metadata')
        if not isinstance(self.datasets, tuple):
            raise ValueError('mutable_package_datasets')

    def snapshot(self):
        return {
            'schema': self.schema,
            'id': self.package_id,
            'title': self.title,
            'type': self.package_type,
            'version': self.version,
            'compatibility': {'protocol': self.compatibility.snapshot()},
            'entrypoints': deepcopy(dict(self.entrypoints)),
            'relationships': deepcopy(dict(self.relationships)),
            'datasets': deepcopy(list(self.datasets)),
            'integrity': deepcopy(dict(self.integrity)),
            'metadata': deepcopy(dict(self.metadata)),
        }


class PackageManifestLoader:
    REQUIRED = {'schema', 'id', 'title', 'type', 'version', 'compatibility', 'entrypoints'}
    OPTIONAL = {'relationships', 'datasets', 'integrity', 'metadata'}
    ENTRYPOINTS = {
        'system': {'system'},
        'module': set(),
        'world': {'world'},
    }

    def load(self, raw: dict) -> BarbaraPackageManifest:
        if not isinstance(raw, dict):
            raise ValueError('invalid_package_manifest')
        missing = self.REQUIRED - set(raw)
        unknown = set(raw) - self.REQUIRED - self.OPTIONAL
        if missing:
            raise ValueError('missing_package_fields:' + ','.join(sorted(missing)))
        if unknown:
            raise ValueError('unknown_package_fields:' + ','.join(sorted(unknown)))
        if raw['schema'] != PACKAGE_SCHEMA_VERSION:
            raise ValueError('unsupported_package_schema')
        compatibility = raw['compatibility']
        if not isinstance(compatibility, dict) or set(compatibility) != {'protocol'}:
            raise ValueError('invalid_package_compatibility')
        protocol = compatibility['protocol']
        if not isinstance(protocol, dict):
            raise ValueError('invalid_package_compatibility')
        if set(protocol) - {'minimum', 'verified', 'maximum'} or not {'minimum', 'verified'} <= set(protocol):
            raise ValueError('invalid_package_compatibility')
        comp = PackageCompatibility(
            protocol['minimum'], protocol['verified'], protocol.get('maximum')
        )
        entrypoints = raw['entrypoints']
        if not isinstance(entrypoints, dict):
            raise ValueError('invalid_package_entrypoints')
        for key, value in entrypoints.items():
            if key not in {'system', 'module', 'world', 'host_contract'}:
                raise ValueError('unknown_package_entrypoint:' + str(key))
            _safe_relpath(value, 'invalid_package_entrypoint')
        ptype = raw['type']
        if ptype not in PACKAGE_TYPES:
            raise ValueError('invalid_package_type')
        required_entrypoints = self.ENTRYPOINTS[ptype]
        if not required_entrypoints <= set(entrypoints):
            raise ValueError('missing_package_entrypoint:' + ','.join(sorted(required_entrypoints - set(entrypoints))))
        relationships = raw.get('relationships', {})
        self._validate_relationships(relationships)
        datasets = raw.get('datasets', [])
        self._validate_datasets(datasets)
        integrity = raw.get('integrity', {})
        self._validate_integrity(integrity)
        metadata = raw.get('metadata', {})
        if not isinstance(metadata, dict):
            raise ValueError('invalid_package_metadata')
        return BarbaraPackageManifest(
            package_id=raw['id'], title=raw['title'], package_type=ptype, version=str(raw['version']),
            compatibility=comp, entrypoints=_freeze(entrypoints), relationships=_freeze(relationships),
            datasets=tuple(deepcopy(datasets)), integrity=_freeze(integrity), metadata=_freeze(metadata),
            schema=raw['schema'],
        )

    def _validate_relationships(self, value):
        if not isinstance(value, dict):
            raise ValueError('invalid_package_relationships')
        if set(value) - {'requires', 'optional'}:
            raise ValueError('unknown_package_relationships')
        for kind in ('requires', 'optional'):
            entries = value.get(kind, [])
            if not isinstance(entries, list):
                raise ValueError('invalid_package_relationships')
            seen = set()
            for entry in entries:
                if not isinstance(entry, dict) or not {'id', 'type'} <= set(entry):
                    raise ValueError('invalid_package_relationship')
                if set(entry) - {'id', 'type', 'compatibility'}:
                    raise ValueError('invalid_package_relationship')
                if not PACKAGE_ID_RE.fullmatch(str(entry['id'])) or entry['type'] not in PACKAGE_TYPES:
                    raise ValueError('invalid_package_relationship')
                if entry['id'] in seen:
                    raise ValueError('duplicate_package_relationship')
                seen.add(entry['id'])
                if 'compatibility' in entry:
                    _version_in_range('0.0.0', entry['compatibility'])  # validates shape only

    def _validate_datasets(self, value):
        if not isinstance(value, list):
            raise ValueError('invalid_package_datasets')
        seen = set()
        for item in value:
            if not isinstance(item, dict) or not {'id', 'kind', 'namespace'} <= set(item):
                raise ValueError('invalid_package_dataset')
            if set(item) - {'id', 'kind', 'namespace', 'path', 'revision', 'mutable', 'metadata', 'format'}:
                raise ValueError('invalid_package_dataset')
            dataset_id = str(item['id'])
            if not PACKAGE_ID_RE.fullmatch(dataset_id):
                raise ValueError('invalid_package_dataset')
            namespace = str(item['namespace'])
            if not namespace or len(namespace) > 200 or namespace.startswith('/') or '..' in namespace.split('/'):
                raise ValueError('invalid_package_dataset_namespace')
            if dataset_id in seen or namespace in seen:
                raise ValueError('duplicate_package_dataset')
            seen.add(dataset_id); seen.add(namespace)
            if 'path' in item:
                _safe_relpath(item['path'], 'invalid_package_dataset_path')
            if 'format' in item and (not isinstance(item['format'], str) or not item['format'] or len(item['format']) > 80):
                raise ValueError('invalid_package_dataset_format')
            if 'mutable' in item and not isinstance(item['mutable'], bool):
                raise ValueError('invalid_package_dataset')
            if 'metadata' in item and not isinstance(item['metadata'], dict):
                raise ValueError('invalid_package_dataset')

    def _validate_integrity(self, value):
        if not isinstance(value, dict):
            raise ValueError('invalid_package_integrity')
        for path, digest in value.items():
            _safe_relpath(path, 'invalid_package_integrity_path')
            if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest):
                raise ValueError('invalid_package_integrity_digest')


@dataclass(frozen=True)
class InstalledPackage:
    manifest: BarbaraPackageManifest
    install_path: str
    archive_sha256: str
    installed_at: int

    def snapshot(self):
        return {
            'manifest': self.manifest.snapshot(),
            'install_path': self.install_path,
            'archive_sha256': self.archive_sha256,
            'installed_at': self.installed_at,
        }


class PackageRepository:
    """Atomic installer and versioned local repository for Barbara packages.

    Packages are immutable per (type,id,version). Worlds pin exact system versions.
    The archive is validated before any installed package becomes visible.
    """

    def __init__(self, root: str, *, protocol_version: int = BARBARA_PROTOCOL_VERSION):
        if not isinstance(root, str) or not root:
            raise ValueError('invalid_package_repository_root')
        if not isinstance(protocol_version, int) or isinstance(protocol_version, bool) or protocol_version < 1:
            raise ValueError('invalid_protocol_version')
        self.root = Path(root).resolve()
        self.protocol_version = protocol_version
        self.root.mkdir(parents=True, exist_ok=True)
        self._lock = threading.RLock()
        self._db = sqlite3.connect(str(self.root / 'packages.db'), timeout=30, check_same_thread=False, isolation_level=None)
        self._db.execute('PRAGMA journal_mode=WAL')
        self._db.execute('PRAGMA synchronous=FULL')
        self._db.execute('PRAGMA foreign_keys=ON')
        self._db.executescript('''
            CREATE TABLE IF NOT EXISTS packages(
                package_type TEXT NOT NULL,
                package_id TEXT NOT NULL,
                version TEXT NOT NULL,
                manifest_json TEXT NOT NULL,
                install_path TEXT NOT NULL,
                archive_sha256 TEXT NOT NULL,
                installed_at INTEGER NOT NULL,
                PRIMARY KEY(package_type, package_id, version)
            );
            CREATE INDEX IF NOT EXISTS idx_packages_id ON packages(package_id, package_type);
        ''')

    def close(self):
        with self._lock:
            if self._db is not None:
                self._db.close(); self._db = None

    def _ensure_open(self):
        if self._db is None:
            raise RuntimeError('package_repository_closed')

    @staticmethod
    def _archive_sha256(path: Path) -> str:
        h = hashlib.sha256()
        with path.open('rb') as handle:
            for block in iter(lambda: handle.read(1024 * 1024), b''):
                h.update(block)
        return h.hexdigest()

    def _read_manifest(self, archive: zipfile.ZipFile) -> BarbaraPackageManifest:
        try:
            info = archive.getinfo('barbara-package.json')
        except KeyError as exc:
            raise ValueError('package_manifest_missing') from exc
        if info.file_size > MAX_MANIFEST_BYTES:
            raise ValueError('package_manifest_too_large')
        try:
            raw = json.loads(archive.read(info).decode('utf-8'))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ValueError('invalid_package_manifest_json') from exc
        return PackageManifestLoader().load(raw)

    def _validate_archive_members(self, archive: zipfile.ZipFile):
        infos = archive.infolist()
        if not infos or len(infos) > MAX_ARCHIVE_FILES:
            raise ValueError('invalid_package_archive_size')
        total = 0
        names = set()
        for info in infos:
            name = info.filename
            if name.endswith('/'):
                clean = name[:-1]
                if clean:
                    _safe_relpath(clean, 'unsafe_package_archive_path')
                continue
            safe = _safe_relpath(name, 'unsafe_package_archive_path')
            if safe in names:
                raise ValueError('duplicate_package_archive_path')
            names.add(safe)
            mode = (info.external_attr >> 16) & 0xFFFF
            if stat.S_ISLNK(mode):
                raise ValueError('package_symlink_forbidden')
            if info.file_size < 0 or info.file_size > MAX_FILE_UNCOMPRESSED:
                raise ValueError('package_file_too_large')
            total += info.file_size
            if total > MAX_ARCHIVE_UNCOMPRESSED:
                raise ValueError('package_archive_too_large')
        if 'barbara-package.json' not in names:
            raise ValueError('package_manifest_missing')

    def _verify_integrity(self, staging: Path, manifest: BarbaraPackageManifest):
        for relpath, expected in manifest.integrity.items():
            target = (staging / relpath).resolve()
            if staging not in target.parents and target != staging:
                raise ValueError('unsafe_package_integrity_path')
            if not target.is_file():
                raise ValueError('package_integrity_file_missing:' + relpath)
            actual = hashlib.sha256(target.read_bytes()).hexdigest()
            if actual != expected:
                raise ValueError('package_integrity_mismatch:' + relpath)
        # Entrypoints are security-sensitive and must be covered when an integrity map exists.
        if manifest.integrity:
            for relpath in manifest.entrypoints.values():
                if relpath not in manifest.integrity:
                    raise ValueError('package_entrypoint_not_integrity_pinned:' + relpath)

    def _validate_entrypoints(self, staging: Path, manifest: BarbaraPackageManifest):
        for name, relpath in manifest.entrypoints.items():
            target = staging / relpath
            if not target.is_file():
                raise ValueError('package_entrypoint_missing:' + name)
        if manifest.package_type == 'system':
            raw = json.loads((staging / manifest.entrypoints['system']).read_text('utf-8'))
            module = SystemModuleLoader().load(raw)
            if module.system_id != manifest.package_id:
                raise ValueError('system_package_identity_mismatch')
            if module.version != manifest.version:
                raise ValueError('system_package_version_mismatch')
            SystemModuleConformance().validate(module)
        if manifest.package_type == 'module' and 'module' in manifest.entrypoints:
            try:
                raw = json.loads((staging / manifest.entrypoints['module']).read_text('utf-8'))
            except json.JSONDecodeError as exc:
                raise ValueError('invalid_module_extension_json') from exc
            extension = ModuleExtensionLoader().load(raw)
            if extension.module_id != manifest.package_id:
                raise ValueError('module_package_identity_mismatch')
            if extension.version != manifest.version:
                raise ValueError('module_package_version_mismatch')
            required_systems = [r for r in manifest.relationships.get('requires', []) if r.get('type') == 'system']
            if required_systems and not any(r.get('id') == extension.system_id for r in required_systems):
                raise ValueError('module_extension_relationship_mismatch')
        if manifest.package_type == 'world':
            try:
                world = json.loads((staging / manifest.entrypoints['world']).read_text('utf-8'))
            except json.JSONDecodeError as exc:
                raise ValueError('invalid_world_package_json') from exc
            self._validate_world_entrypoint(world)
        for dataset in manifest.datasets:
            relpath = dataset.get('path')
            if relpath:
                target = (staging / relpath).resolve()
                if staging not in target.parents and target != staging:
                    raise ValueError('unsafe_package_dataset_path')
                if not target.exists():
                    raise ValueError('package_dataset_path_missing:' + dataset['id'])
        if 'host_contract' in manifest.entrypoints:
            contract_raw = json.loads((staging / manifest.entrypoints['host_contract']).read_text('utf-8'))
            contract = HostIntegrationLoader().load(contract_raw)
            if contract.system_id != manifest.package_id:
                raise ValueError('package_host_contract_system_mismatch')

    def _validate_world_entrypoint(self, world):
        if not isinstance(world, dict):
            raise ValueError('invalid_world_package')
        required = {'title', 'system'}
        allowed = {'title', 'system', 'campaign_id', 'modules', 'metadata'}
        if not required <= set(world) or set(world) - allowed:
            raise ValueError('invalid_world_package')
        if not isinstance(world['title'], str) or not world['title'].strip():
            raise ValueError('invalid_world_package')
        system = world['system']
        if not isinstance(system, dict) or set(system) != {'id', 'version'}:
            raise ValueError('invalid_world_package_system')
        if not PACKAGE_ID_RE.fullmatch(str(system['id'])):
            raise ValueError('invalid_world_package_system')
        _version_key(str(system['version']))
        modules = world.get('modules', [])
        if not isinstance(modules, list):
            raise ValueError('invalid_world_package_modules')
        seen = set()
        for ref in modules:
            if not isinstance(ref, dict) or set(ref) != {'id', 'version'}:
                raise ValueError('invalid_world_package_modules')
            if not PACKAGE_ID_RE.fullmatch(str(ref['id'])):
                raise ValueError('invalid_world_package_modules')
            _version_key(str(ref['version']))
            if ref['id'] in seen:
                raise ValueError('duplicate_world_package_module')
            seen.add(ref['id'])
        if 'campaign_id' in world and (not isinstance(world['campaign_id'], str) or not world['campaign_id'] or len(world['campaign_id']) > 160):
            raise ValueError('invalid_world_package_campaign')
        if 'metadata' in world and not isinstance(world['metadata'], dict):
            raise ValueError('invalid_world_package_metadata')
        return deepcopy(world)

    def _check_required_relationships(self, manifest: BarbaraPackageManifest):
        for rel in manifest.relationships.get('requires', []):
            versions = self.versions(rel['id'], rel['type'])
            compatible = [v for v in versions if _version_in_range(v, rel.get('compatibility'))]
            if not compatible:
                raise ValueError('package_dependency_missing:' + rel['type'] + ':' + rel['id'])

    def install(self, archive_path: str, *, expected_sha256: str | None = None) -> InstalledPackage:
        source = Path(archive_path).resolve()
        if not source.is_file():
            raise FileNotFoundError(str(source))
        digest = self._archive_sha256(source)
        if expected_sha256 is not None:
            if not isinstance(expected_sha256, str) or not SHA256_RE.fullmatch(expected_sha256):
                raise ValueError('invalid_expected_package_sha256')
            if digest != expected_sha256:
                raise ValueError('package_archive_sha256_mismatch')
        with self._lock:
            self._ensure_open()
            try:
                archive = zipfile.ZipFile(source, 'r')
            except zipfile.BadZipFile as exc:
                raise ValueError('invalid_package_archive') from exc
            with archive:
                self._validate_archive_members(archive)
                manifest = self._read_manifest(archive)
                if not manifest.compatibility.supports(self.protocol_version):
                    raise ValueError('package_protocol_incompatible')
                self._check_required_relationships(manifest)
                existing = self.find(manifest.package_type, manifest.package_id, manifest.version)
                if existing is not None:
                    if existing.archive_sha256 != digest:
                        raise ValueError('package_version_immutable')
                    return existing
                base = self.root / (manifest.package_type + 's') / manifest.package_id
                base.mkdir(parents=True, exist_ok=True)
                final = base / manifest.version
                if final.exists():
                    raise ValueError('package_install_path_conflict')
                staging = Path(tempfile.mkdtemp(prefix='.install-', dir=str(base)))
                try:
                    for info in archive.infolist():
                        if info.is_dir():
                            continue
                        rel = _safe_relpath(info.filename, 'unsafe_package_archive_path')
                        target = staging / rel
                        target.parent.mkdir(parents=True, exist_ok=True)
                        with archive.open(info, 'r') as src, target.open('wb') as dst:
                            shutil.copyfileobj(src, dst, 1024 * 1024)
                    self._verify_integrity(staging, manifest)
                    self._validate_entrypoints(staging, manifest)
                    os.replace(staging, final)
                    staging = None
                    now = int(time.time() * 1000)
                    manifest_json = json.dumps(manifest.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))
                    try:
                        self._db.execute('BEGIN IMMEDIATE')
                        self._db.execute(
                            'INSERT INTO packages(package_type,package_id,version,manifest_json,install_path,archive_sha256,installed_at) VALUES(?,?,?,?,?,?,?)',
                            (manifest.package_type, manifest.package_id, manifest.version, manifest_json, str(final), digest, now),
                        )
                        self._db.execute('COMMIT')
                    except Exception:
                        if self._db.in_transaction:
                            self._db.execute('ROLLBACK')
                        shutil.rmtree(final, ignore_errors=True)
                        raise
                    return InstalledPackage(manifest, str(final), digest, now)
                finally:
                    if staging is not None:
                        shutil.rmtree(staging, ignore_errors=True)

    def find(self, package_type: str, package_id: str, version: str) -> InstalledPackage | None:
        if package_type not in PACKAGE_TYPES:
            raise ValueError('invalid_package_type')
        with self._lock:
            self._ensure_open()
            row = self._db.execute(
                'SELECT manifest_json,install_path,archive_sha256,installed_at FROM packages WHERE package_type=? AND package_id=? AND version=?',
                (package_type, package_id, str(version)),
            ).fetchone()
        if row is None:
            return None
        manifest = PackageManifestLoader().load(json.loads(row[0]))
        return InstalledPackage(manifest, row[1], row[2], row[3])

    def get(self, package_type: str, package_id: str, version: str | None = None) -> InstalledPackage:
        if version is not None:
            result = self.find(package_type, package_id, version)
            if result is None:
                raise KeyError((package_type, package_id, version))
            return result
        versions = self.versions(package_id, package_type)
        if not versions:
            raise KeyError((package_type, package_id))
        latest = max(versions, key=_version_key)
        return self.get(package_type, package_id, latest)

    def versions(self, package_id: str, package_type: str) -> tuple[str, ...]:
        if package_type not in PACKAGE_TYPES:
            raise ValueError('invalid_package_type')
        with self._lock:
            self._ensure_open()
            rows = self._db.execute(
                'SELECT version FROM packages WHERE package_type=? AND package_id=?',
                (package_type, package_id),
            ).fetchall()
        return tuple(sorted((row[0] for row in rows), key=_version_key))

    def all(self, package_type: str | None = None) -> tuple[InstalledPackage, ...]:
        if package_type is not None and package_type not in PACKAGE_TYPES:
            raise ValueError('invalid_package_type')
        with self._lock:
            self._ensure_open()
            if package_type is None:
                rows = self._db.execute(
                    'SELECT package_type,package_id,version FROM packages ORDER BY package_type,package_id'
                ).fetchall()
            else:
                rows = self._db.execute(
                    'SELECT package_type,package_id,version FROM packages WHERE package_type=? ORDER BY package_id',
                    (package_type,),
                ).fetchall()
        return tuple(self.get(*row) for row in rows)

    def load_system_module(self, package_id: str, version: str | None = None) -> SystemModule:
        installed = self.get('system', package_id, version)
        path = Path(installed.install_path) / installed.manifest.entrypoints['system']
        raw = json.loads(path.read_text('utf-8'))
        module = SystemModuleLoader().load(raw)
        SystemModuleConformance().validate(module)
        return module

    def load_module_extension(self, package_id: str, version: str | None = None):
        package = self.get('module', package_id, version)
        rel = package.manifest.entrypoints.get('module')
        if rel is None:
            return None
        try:
            raw = json.loads((Path(package.install_path) / rel).read_text('utf-8'))
        except json.JSONDecodeError as exc:
            raise ValueError('invalid_module_extension_json') from exc
        extension = ModuleExtensionLoader().load(raw)
        if extension.module_id != package.manifest.package_id or extension.version != package.manifest.version:
            raise ValueError('module_package_identity_mismatch')
        return extension

    def uninstall(self, package_type: str, package_id: str, version: str, *, reference_checker=None):
        installed = self.get(package_type, package_id, version)
        if reference_checker is not None and reference_checker(package_type, package_id, version):
            raise ValueError('package_in_use')
        with self._lock:
            self._ensure_open()
            try:
                self._db.execute('BEGIN IMMEDIATE')
                self._db.execute(
                    'DELETE FROM packages WHERE package_type=? AND package_id=? AND version=?',
                    (package_type, package_id, version),
                )
                self._db.execute('COMMIT')
            except Exception:
                if self._db.in_transaction:
                    self._db.execute('ROLLBACK')
                raise
        shutil.rmtree(installed.install_path, ignore_errors=True)
        return installed


@dataclass(frozen=True)
class WorldRecord:
    world_id: str
    title: str
    system_id: str
    system_version: str
    campaign_id: str
    enabled_modules: tuple
    metadata: MappingProxyType
    created_at: int
    updated_at: int

    def snapshot(self):
        return {
            'world_id': self.world_id,
            'title': self.title,
            'system_id': self.system_id,
            'system_version': self.system_version,
            'campaign_id': self.campaign_id,
            'enabled_modules': deepcopy(list(self.enabled_modules)),
            'metadata': deepcopy(dict(self.metadata)),
            'created_at': self.created_at,
            'updated_at': self.updated_at,
        }


class WorldRegistry:
    """Foundry-like worlds: one pinned game system plus optional installed modules."""

    def __init__(self, root: str, packages: PackageRepository):
        if not isinstance(packages, PackageRepository):
            raise ValueError('invalid_package_repository')
        self.root = Path(root).resolve(); self.root.mkdir(parents=True, exist_ok=True)
        self.packages = packages
        self._lock = threading.RLock()
        self._db = sqlite3.connect(str(self.root / 'worlds.db'), timeout=30, check_same_thread=False, isolation_level=None)
        self._db.execute('PRAGMA journal_mode=WAL'); self._db.execute('PRAGMA synchronous=FULL')
        self._db.executescript('''
            CREATE TABLE IF NOT EXISTS worlds(
                world_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                system_id TEXT NOT NULL,
                system_version TEXT NOT NULL,
                campaign_id TEXT NOT NULL UNIQUE,
                enabled_modules_json TEXT NOT NULL,
                metadata_json TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS world_dataset_mounts(
                world_id TEXT NOT NULL,
                namespace TEXT NOT NULL,
                kind TEXT NOT NULL,
                local_path TEXT NOT NULL,
                revision TEXT NOT NULL,
                mutable INTEGER NOT NULL,
                metadata_json TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(world_id, namespace),
                FOREIGN KEY(world_id) REFERENCES worlds(world_id) ON DELETE CASCADE
            );
            CREATE INDEX IF NOT EXISTS idx_world_dataset_mounts_world_kind
                ON world_dataset_mounts(world_id, kind);
        ''')
        self._db.execute('PRAGMA foreign_keys=ON')

    def close(self):
        with self._lock:
            if self._db is not None:
                self._db.close(); self._db = None

    def _ensure_open(self):
        if self._db is None:
            raise RuntimeError('world_registry_closed')

    def _decode(self, row) -> WorldRecord:
        return WorldRecord(
            row[0], row[1], row[2], row[3], row[4], tuple(json.loads(row[5])),
            _freeze(json.loads(row[6]), 'invalid_world_metadata'), row[7], row[8]
        )

    def create(self, world_id: str, title: str, system_id: str, *, system_version: str | None = None, campaign_id: str | None = None, metadata=None) -> WorldRecord:
        if not isinstance(world_id, str) or not PACKAGE_ID_RE.fullmatch(world_id):
            raise ValueError('invalid_world_id')
        if not isinstance(title, str) or not title.strip() or len(title) > 200:
            raise ValueError('invalid_world_title')
        system = self.packages.get('system', system_id, system_version)
        campaign_id = campaign_id or world_id
        if not isinstance(campaign_id, str) or not campaign_id or len(campaign_id) > 160:
            raise ValueError('invalid_campaign_id')
        metadata = metadata or {}
        if not isinstance(metadata, dict):
            raise ValueError('invalid_world_metadata')
        now = int(time.time() * 1000)
        with self._lock:
            self._ensure_open()
            self._db.execute(
                'INSERT INTO worlds(world_id,title,system_id,system_version,campaign_id,enabled_modules_json,metadata_json,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)',
                (world_id, title.strip(), system.manifest.package_id, system.manifest.version, campaign_id, '[]', json.dumps(metadata, sort_keys=True, separators=(',', ':')), now, now),
            )
        (self.root / 'data' / world_id).mkdir(parents=True, exist_ok=True)
        return self.get(world_id)

    def delete(self, world_id: str) -> bool:
        world = self.get(world_id)
        with self._lock:
            self._ensure_open()
            cur = self._db.execute('DELETE FROM worlds WHERE world_id=?', (world_id,))
            self._db.execute('DELETE FROM world_dataset_mounts WHERE world_id=?', (world_id,))
        data = self.root / 'data' / world.world_id
        if data.exists():
            shutil.rmtree(data, ignore_errors=True)
        return cur.rowcount == 1

    def get(self, world_id: str) -> WorldRecord:
        with self._lock:
            self._ensure_open()
            row = self._db.execute(
                'SELECT world_id,title,system_id,system_version,campaign_id,enabled_modules_json,metadata_json,created_at,updated_at FROM worlds WHERE world_id=?',
                (world_id,),
            ).fetchone()
        if row is None:
            raise KeyError(world_id)
        return self._decode(row)

    def all(self) -> tuple[WorldRecord, ...]:
        with self._lock:
            self._ensure_open()
            rows = self._db.execute(
                'SELECT world_id,title,system_id,system_version,campaign_id,enabled_modules_json,metadata_json,created_at,updated_at FROM worlds ORDER BY updated_at DESC, world_id'
            ).fetchall()
        return tuple(self._decode(row) for row in rows)

    def _module_compatible(self, module: InstalledPackage, world: WorldRecord) -> bool:
        for rel in module.manifest.relationships.get('requires', []):
            if rel['type'] == 'system' and rel['id'] == world.system_id:
                return _version_in_range(world.system_version, rel.get('compatibility'))
        # Modules with no system relationship are system-agnostic.
        systems = [rel for rel in module.manifest.relationships.get('requires', []) if rel['type'] == 'system']
        return not systems

    def enable_module(self, world_id: str, module_id: str, version: str | None = None) -> WorldRecord:
        world = self.get(world_id)
        module = self.packages.get('module', module_id, version)
        if not self._module_compatible(module, world):
            raise ValueError('module_system_incompatible')
        ref = {'id': module.manifest.package_id, 'version': module.manifest.version}
        enabled = list(world.enabled_modules)
        enabled = [entry for entry in enabled if entry.get('id') != module_id]
        enabled.append(ref); enabled.sort(key=lambda x: x['id'])
        now = int(time.time() * 1000)
        with self._lock:
            self._ensure_open()
            self._db.execute(
                'UPDATE worlds SET enabled_modules_json=?,updated_at=? WHERE world_id=?',
                (json.dumps(enabled, sort_keys=True, separators=(',', ':')), now, world_id),
            )
        return self.get(world_id)

    def disable_module(self, world_id: str, module_id: str) -> WorldRecord:
        world = self.get(world_id)
        enabled = [entry for entry in world.enabled_modules if entry.get('id') != module_id]
        now = int(time.time() * 1000)
        with self._lock:
            self._ensure_open()
            self._db.execute(
                'UPDATE worlds SET enabled_modules_json=?,updated_at=? WHERE world_id=?',
                (json.dumps(enabled, sort_keys=True, separators=(',', ':')), now, world_id),
            )
        return self.get(world_id)

    def launch_plan(self, world_id: str) -> dict:
        world = self.get(world_id)
        system = self.packages.get('system', world.system_id, world.system_version)
        modules = []
        for ref in world.enabled_modules:
            module = self.packages.get('module', ref['id'], ref['version'])
            if not self._module_compatible(module, world):
                raise ValueError('enabled_module_became_incompatible:' + ref['id'])
            modules.append(module)
        return {
            'world': world.snapshot(),
            'system': system.snapshot(),
            'modules': [module.snapshot() for module in modules],
            'datasets': [deepcopy(item) for item in self.resolve_datasets(world_id)],
            'data_path': str(self.root / 'data' / world.world_id),
        }

    def update_metadata(self, world_id: str, metadata: dict) -> WorldRecord:
        if not isinstance(metadata, dict):
            raise ValueError('invalid_world_metadata')
        self.get(world_id)
        now = int(time.time() * 1000)
        payload = json.dumps(metadata, ensure_ascii=False, sort_keys=True, separators=(',', ':'))
        with self._lock:
            self._ensure_open()
            self._db.execute('UPDATE worlds SET metadata_json=?,updated_at=? WHERE world_id=?', (payload, now, world_id))
        return self.get(world_id)

    def select_dataset(self, world_id: str, kind: str, namespace: str | None) -> WorldRecord:
        if not isinstance(kind, str) or not kind or len(kind) > 80:
            raise ValueError('invalid_dataset_kind')
        if namespace is not None and (not isinstance(namespace, str) or not namespace or len(namespace) > 200):
            raise ValueError('invalid_dataset_namespace')
        world = self.get(world_id)
        metadata = deepcopy(dict(world.metadata))
        selection = metadata.setdefault('dataset_selection', {})
        if not isinstance(selection, dict):
            raise ValueError('invalid_world_dataset_selection')
        if namespace is None:
            selection.pop(kind, None)
        else:
            available = [d for d in self.resolve_datasets(world_id) if d['kind'] == kind and d['namespace'] == namespace]
            if not available:
                raise ValueError('dataset_not_available')
            selection[kind] = namespace
        return self.update_metadata(world_id, metadata)

    def mount_external_dataset(self, world_id: str, *, kind: str, namespace: str, local_path: str, revision: str = '', mutable: bool = True, metadata=None) -> dict:
        """Attach a host-synchronized local dataset to a World without repackaging it.

        This is the intended bridge for large Google Drive mirrors and existing app
        libraries: the platform-specific sync layer owns download/auth, while Barbara
        indexes/queries the resulting local path under a stable namespace.
        """
        self.get(world_id)
        if not isinstance(kind,str) or not kind or len(kind)>80:
            raise ValueError('invalid_dataset_kind')
        if not isinstance(namespace,str) or not namespace or len(namespace)>200 or namespace.startswith('/') or '..' in namespace.split('/'):
            raise ValueError('invalid_package_dataset_namespace')
        if not isinstance(local_path,str) or not local_path:
            raise ValueError('invalid_external_dataset_path')
        path=Path(local_path).expanduser().resolve()
        if not path.exists():
            raise FileNotFoundError(str(path))
        if not isinstance(revision,str) or len(revision)>200:
            raise ValueError('invalid_dataset_revision')
        if not isinstance(mutable,bool):
            raise ValueError('invalid_package_dataset')
        metadata={} if metadata is None else deepcopy(metadata)
        if not isinstance(metadata,dict):
            raise ValueError('invalid_package_dataset')
        # A namespace already supplied by an installed System/Module is not shadowable.
        package_namespaces={d['namespace'] for d in self._package_datasets(world_id)}
        if namespace in package_namespaces:
            raise ValueError('dataset_namespace_collision:'+namespace)
        now=int(time.time()*1000)
        payload=json.dumps(metadata,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
        with self._lock:
            self._ensure_open()
            self._db.execute(
                'INSERT INTO world_dataset_mounts(world_id,namespace,kind,local_path,revision,mutable,metadata_json,created_at,updated_at) '
                'VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(world_id,namespace) DO UPDATE SET kind=excluded.kind,local_path=excluded.local_path,revision=excluded.revision,mutable=excluded.mutable,metadata_json=excluded.metadata_json,updated_at=excluded.updated_at',
                (world_id,namespace,kind,str(path),revision,int(mutable),payload,now,now),
            )
        return self.get_external_dataset(world_id,namespace)

    def get_external_dataset(self, world_id: str, namespace: str) -> dict:
        self.get(world_id)
        with self._lock:
            self._ensure_open()
            row=self._db.execute(
                'SELECT kind,local_path,revision,mutable,metadata_json,created_at,updated_at FROM world_dataset_mounts WHERE world_id=? AND namespace=?',
                (world_id,namespace),
            ).fetchone()
        if row is None:
            raise KeyError((world_id,namespace))
        return {
            'id':'external-'+hashlib.sha256(namespace.encode()).hexdigest()[:16],
            'kind':row[0],'namespace':namespace,'path':None,'revision':row[2],
            'mutable':bool(row[3]),'metadata':json.loads(row[4]),
            'package_id':None,'package_version':None,'package_type':'external',
            'resolved_path':row[1],'created_at':row[5],'updated_at':row[6],
        }

    def external_datasets(self, world_id: str) -> tuple[dict, ...]:
        self.get(world_id)
        with self._lock:
            self._ensure_open()
            rows=self._db.execute('SELECT namespace FROM world_dataset_mounts WHERE world_id=? ORDER BY namespace',(world_id,)).fetchall()
        return tuple(self.get_external_dataset(world_id,row[0]) for row in rows)

    def unmount_external_dataset(self, world_id: str, namespace: str) -> bool:
        self.get(world_id)
        with self._lock:
            self._ensure_open()
            cur=self._db.execute('DELETE FROM world_dataset_mounts WHERE world_id=? AND namespace=?',(world_id,namespace))
        return cur.rowcount==1

    def _package_datasets(self, world_id: str) -> tuple[dict, ...]:
        world=self.get(world_id)
        packages=[self.packages.get('system',world.system_id,world.system_version)]
        for ref in world.enabled_modules:
            packages.append(self.packages.get('module',ref['id'],ref['version']))
        resolved=[]; namespaces=set()
        for package in packages:
            base=Path(package.install_path).resolve()
            for dataset in package.manifest.datasets:
                namespace=dataset['namespace']
                if namespace in namespaces:
                    raise ValueError('dataset_namespace_collision:'+namespace)
                namespaces.add(namespace)
                item=deepcopy(dataset)
                item['package_id']=package.manifest.package_id
                item['package_version']=package.manifest.version
                item['package_type']=package.manifest.package_type
                item['owner_system_id']=world.system_id
                item['provider_package_id']=package.manifest.package_id
                item['provider_package_type']=package.manifest.package_type
                if item.get('path'):
                    target=(base/item['path']).resolve()
                    if base not in target.parents and target!=base:
                        raise ValueError('unsafe_installed_dataset_path')
                    if not target.exists():
                        raise ValueError('installed_dataset_missing:'+item['id'])
                    item['resolved_path']=str(target)
                resolved.append(item)
        return tuple(resolved)

    def resolve_datasets(self, world_id: str) -> tuple[dict, ...]:
        world=self.get(world_id)
        selected=dict(world.metadata.get('dataset_selection',{})) if isinstance(world.metadata.get('dataset_selection',{}),dict) else {}
        resolved=[]; namespaces=set()
        for item in (*self._package_datasets(world_id), *self.external_datasets(world_id)):
            item=deepcopy(item); namespace=item['namespace']
            if namespace in namespaces:
                raise ValueError('dataset_namespace_collision:'+namespace)
            namespaces.add(namespace)
            item['active']=selected.get(item['kind']) in (None,namespace)
            if selected.get(item['kind']) is not None:
                item['active']=selected[item['kind']]==namespace
            resolved.append(item)
        return tuple(resolved)

    def instantiate_world_package(self, package_id: str, version: str | None = None, *, world_id: str | None = None) -> WorldRecord:
        package = self.packages.get('world', package_id, version)
        path = Path(package.install_path) / package.manifest.entrypoints['world']
        try:
            spec = json.loads(path.read_text('utf-8'))
        except json.JSONDecodeError as exc:
            raise ValueError('invalid_world_package_json') from exc
        self.packages._validate_world_entrypoint(spec)
        target_id = world_id or package.manifest.package_id
        system = spec['system']
        # Fail before creating any world if exact dependencies are unavailable.
        self.packages.get('system', system['id'], system['version'])
        for ref in spec.get('modules', []):
            self.packages.get('module', ref['id'], ref['version'])
        world = self.create(
            target_id, spec['title'], system['id'], system_version=system['version'],
            campaign_id=spec.get('campaign_id') or target_id, metadata=deepcopy(spec.get('metadata') or {}),
        )
        try:
            for ref in spec.get('modules', []):
                world = self.enable_module(target_id, ref['id'], ref['version'])
            self.resolve_datasets(target_id)
            return world
        except Exception:
            # World package creation is all-or-nothing from the registry's perspective.
            with self._lock:
                if self._db is not None:
                    self._db.execute('DELETE FROM worlds WHERE world_id=?', (target_id,))
            shutil.rmtree(self.root / 'data' / target_id, ignore_errors=True)
            raise

    def references_package(self, package_type: str, package_id: str, version: str) -> bool:
        for world in self.all():
            if package_type == 'system' and world.system_id == package_id and world.system_version == version:
                return True
            if package_type == 'module' and any(ref.get('id') == package_id and ref.get('version') == version for ref in world.enabled_modules):
                return True
        return False

    def upgrade_system(self, world_id: str, target_version: str, *, migrate_document: dict | None = None):
        world = self.get(world_id)
        target = self.packages.get('system', world.system_id, target_version)
        old_module = self.packages.load_system_module(world.system_id, world.system_version)
        new_module = self.packages.load_system_module(world.system_id, target.manifest.version)
        migrated = deepcopy(migrate_document)
        if migrate_document is not None and world.system_version != target.manifest.version:
            # Migrations are declared by the target system package.
            from .system_module import SystemModuleRegistry
            registry = SystemModuleRegistry(modules=[new_module])
            migrated = registry.migrate_document(migrate_document, new_module.system_id, world.system_version, target.manifest.version)
        # Verify existing enabled modules before changing the binding.
        candidate = WorldRecord(world.world_id, world.title, world.system_id, target.manifest.version, world.campaign_id, world.enabled_modules, world.metadata, world.created_at, world.updated_at)
        for ref in world.enabled_modules:
            package = self.packages.get('module', ref['id'], ref['version'])
            if not self._module_compatible(package, candidate):
                raise ValueError('world_upgrade_module_incompatible:' + ref['id'])
        now = int(time.time() * 1000)
        with self._lock:
            self._ensure_open()
            self._db.execute(
                'UPDATE worlds SET system_version=?,updated_at=? WHERE world_id=? AND system_version=?',
                (target.manifest.version, now, world_id, old_module.version),
            )
        return {'world': self.get(world_id), 'migrated_document': migrated}
