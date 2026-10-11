from __future__ import annotations

from pathlib import Path
from copy import deepcopy
import hashlib
import json
import os
import tempfile
import zipfile

from .adapters import AdapterRegistry
from .engine import BarbaraEngine
from .bridge import HostBridge
from .packages import PackageRepository, WorldRegistry
from .state import CampaignState
from .state_store import DurableStateStore
from .system_module import SystemModuleRegistry
from .context import ContextEngine
from .resources import WorldResourceRuntime
from .vtt import VttAuthority, VttCommandError
from .module_extension import compose_system_module


class BarbaraWorldSession:
    """Durable, world-scoped execution boundary.

    A session is the Foundry-like running World: it owns an engine instance, while
    canonical campaign state stays in the platform DurableStateStore. Every command
    is fingerprinted, executed against one state version and committed with CAS, so
    UI reconnects/retries cannot duplicate a turn and concurrent stale writers fail
    instead of overwriting campaign truth.
    """

    def __init__(self, world, engine, store, resources=None):
        self.world = world
        self.engine = engine
        self.store = store
        self.resources = resources
        self.bridge = HostBridge(engine)
        self._closed = False
        self.vtt = VttAuthority(atlas_provider=(resources.atlas if resources is not None else None))

    def _ensure_open(self):
        if self._closed:
            raise RuntimeError('world_session_closed')

    def state(self):
        self._ensure_open()
        return self.store.load(self.world.campaign_id)

    def turn(self, request: dict):
        self._ensure_open()
        if not isinstance(request, dict) or not isinstance(request.get('request_id'), str):
            raise ValueError('invalid_host_request')
        request_id = request['request_id']
        fingerprint = self.store.fingerprint(request)
        prior = self.store.lookup_command(self.world.campaign_id, request_id, fingerprint)
        if prior is not None:
            return deepcopy(prior['response'])
        state = self.store.load(self.world.campaign_id)
        expected = state.state_version
        supplied = request.get('expected_state_version')
        if supplied is not None and supplied != expected:
            from .state_store import StateVersionConflict
            raise StateVersionConflict('state_version_conflict')
        bound_request = dict(request)
        bound_request['expected_state_version'] = expected
        out = self.bridge.turn(state.to_json(), bound_request)
        new_state = CampaignState.from_json(out['state'])
        committed = self.store.compare_and_swap(
            expected_state_version=expected,
            new_state=new_state,
            command_id=request_id,
            fingerprint=fingerprint,
            response=out,
        )
        return deepcopy(committed['response'])

    def accept_narrative(self, request: dict):
        self._ensure_open()
        if not isinstance(request, dict) or not isinstance(request.get('request_id'), str):
            raise ValueError('invalid_host_request')
        fingerprint = self.store.fingerprint({'kind':'accept_narrative', **request})
        request_id = request['request_id']
        prior = self.store.lookup_command(self.world.campaign_id, request_id, fingerprint)
        if prior is not None:
            return deepcopy(prior['response'])
        state = self.store.load(self.world.campaign_id)
        expected = state.state_version
        out = self.bridge.accept_narrative(state.to_json(), request)
        new_state = CampaignState.from_json(out['state'])
        committed = self.store.compare_and_swap(
            expected_state_version=expected, new_state=new_state, command_id=request_id,
            fingerprint=fingerprint, response=out,
        )
        return deepcopy(committed['response'])

    def vtt_snapshot(self, *, actor_id=None, role='gm'):
        self._ensure_open()
        state = self.store.load(self.world.campaign_id)
        return {
            'type': 'snapshot',
            'state_version': state.state_version,
            'state': self.vtt.snapshot(state,actor_id=actor_id,role=role),
        }

    def vtt_command(self, command: dict, *, actor_id: str, role: str = 'player'):
        self._ensure_open()
        if not isinstance(command, dict):
            raise ValueError('invalid_vtt_command')
        command_id = command.get('command_id', command.get('commandId'))
        if not isinstance(command_id, str) or not command_id:
            raise ValueError('invalid_command_id')
        normalized = deepcopy(command)
        normalized['command_id'] = command_id
        if 'base_version' not in normalized and 'baseVersion' in normalized:
            normalized['base_version'] = normalized['baseVersion']
        fingerprint = self.store.fingerprint({'kind':'vtt','actor_id':actor_id,'role':role,'command':normalized})
        prior = self.store.lookup_command(self.world.campaign_id, command_id, fingerprint)
        if prior is not None:
            return deepcopy(prior['response'])
        state = self.store.load(self.world.campaign_id)
        draft, response = self.vtt.apply(state, normalized, actor_id=actor_id, role=role)
        committed = self.store.compare_and_swap(
            expected_state_version=state.state_version, new_state=draft, command_id=command_id,
            fingerprint=fingerprint, response=response,
        )
        return deepcopy(committed['response'])

    def close(self):
        if self._closed:
            return
        self._closed = True
        self.bridge.close()
        if self.resources is not None:
            self.resources.close()


class BarbaraPlatform:
    """Foundry-like Barbara installation: core runtime + installable packages + worlds."""

    def __init__(self, root: str):
        if not isinstance(root, str) or not root:
            raise ValueError('invalid_barbara_platform_root')
        self.root = Path(root).resolve(); self.root.mkdir(parents=True, exist_ok=True)
        self.packages = PackageRepository(str(self.root / 'packages'))
        self.worlds = WorldRegistry(str(self.root / 'worlds'), self.packages)
        self.state = DurableStateStore(str(self.root / 'barbara-state.db'))
        self._sessions = {}

    def close(self):
        for engine in tuple(self._sessions.values()):
            try: engine.close()
            except Exception: pass
        self._sessions.clear()
        self.state.close(); self.worlds.close(); self.packages.close()

    def install_package(self, archive_path: str, *, expected_sha256: str | None = None):
        return self.packages.install(archive_path, expected_sha256=expected_sha256)

    def uninstall_package(self, package_type: str, package_id: str, version: str):
        return self.packages.uninstall(
            package_type, package_id, version,
            reference_checker=self.worlds.references_package,
        )

    def create_world(self, world_id: str, title: str, system_id: str, *, system_version: str | None = None, campaign_id: str | None = None, metadata=None):
        world = self.worlds.create(
            world_id, title, system_id, system_version=system_version,
            campaign_id=campaign_id, metadata=metadata,
        )
        state = CampaignState(world.campaign_id, world.system_id)
        self.state.create_campaign(state)
        return world

    def launch_world(self, world_id: str, *, provider=None, rag_db_path: str | None = None) -> dict:
        world = self.worlds.get(world_id)
        plan = self.worlds.launch_plan(world_id)
        base_module = self.packages.load_system_module(world.system_id, world.system_version)
        extensions = []
        for ref in world.enabled_modules:
            extension = self.packages.load_module_extension(ref['id'], ref['version'])
            if extension is not None:
                extensions.append(extension)
        module = compose_system_module(base_module, extensions) if extensions else base_module
        registry = SystemModuleRegistry(modules=[module])
        adapters = AdapterRegistry(modules=registry)
        if world_id in self._sessions:
            self._sessions[world_id].close()
        data_path = Path(plan['data_path'])
        effective_rag_db = rag_db_path or str(data_path / 'knowledge.db')
        resources = WorldResourceRuntime(
            world=world, datasets=plan['datasets'], data_path=str(data_path), system_module=module,
        )
        context_engine = ContextEngine(atlas_provider=resources.atlas)
        try:
            engine = BarbaraEngine(provider=provider, adapters=adapters, rag_db_path=effective_rag_db, context_engine=context_engine, strict_world_introductions=True)
            resources.bind_engine(engine)
            state = self.state.load(world.campaign_id)
            if state.system_id != module.system_id:
                raise ValueError('world_campaign_system_mismatch')
            state.system_module_version = state.system_module_version or module.version
            session = BarbaraWorldSession(world, engine, self.state, resources=resources)
        except Exception:
            resources.close()
            try: engine.close()
            except Exception: pass
            raise
        self._sessions[world_id] = session
        return {
            'world': world.snapshot(),
            'launch_plan': plan,
            'system_module': module.snapshot(),
            'state_version': state.state_version,
            'engine': engine,
            'session': session,
            'resources': resources,
            'resource_snapshot': resources.snapshot(),
            'rag_db_path': effective_rag_db,
        }

    @staticmethod
    def _backup_bytes(value) -> bytes:
        return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False).encode('utf-8')

    def backup_world(self, world_id: str, archive_path: str) -> dict:
        """Create a portable World backup without duplicating immutable System assets.

        The backup pins package versions+hashes, preserves canonical state and durable
        idempotency receipts, and records external dataset mounts as references. Atlas/RAG
        indexes remain rebuildable projections and are deliberately excluded.
        """
        world = self.worlds.get(world_id)
        plan = self.worlds.launch_plan(world_id)
        state_doc = self.state.export_campaign(world.campaign_id)
        package_refs = []
        system = self.packages.get('system', world.system_id, world.system_version)
        package_refs.append({'type':'system','id':world.system_id,'version':world.system_version,'sha256':system.archive_sha256})
        for ref in world.enabled_modules:
            pkg = self.packages.get('module', ref['id'], ref['version'])
            package_refs.append({'type':'module','id':ref['id'],'version':ref['version'],'sha256':pkg.archive_sha256})
        external = list(self.worlds.external_datasets(world_id))
        # Paths are references only; content remains owned by the game's sync/source layer.
        external = [
            {k:v for k,v in item.items() if k in {'namespace','kind','resolved_path','revision','mutable','metadata'}}
            for item in external
        ]
        files = {
            'world.json': self._backup_bytes(world.snapshot()),
            'campaign.json': self._backup_bytes(state_doc),
            'external-datasets.json': self._backup_bytes(external),
        }
        digests = {name: hashlib.sha256(data).hexdigest() for name, data in files.items()}
        manifest = {
            'schema': 1, 'format': 'barbara-world-backup-v1', 'world_id': world.world_id,
            'campaign_id': world.campaign_id, 'system_id': world.system_id,
            'packages': package_refs, 'files': digests,
        }
        files['backup-manifest.json'] = self._backup_bytes(manifest)
        target = Path(archive_path).resolve()
        target.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(prefix=target.name+'.', suffix='.tmp', dir=str(target.parent)); os.close(fd)
        try:
            with zipfile.ZipFile(tmp, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
                for name in sorted(files):
                    archive.writestr(name, files[name])
            os.replace(tmp, target)
        finally:
            if os.path.exists(tmp): os.unlink(tmp)
        return {'path':str(target),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'manifest':manifest,'launch_plan':plan}

    def restore_world(self, archive_path: str, *, target_world_id: str | None = None, target_campaign_id: str | None = None) -> dict:
        source = Path(archive_path).resolve()
        if not source.is_file(): raise FileNotFoundError(str(source))
        with zipfile.ZipFile(source, 'r') as archive:
            names = set(archive.namelist())
            required = {'backup-manifest.json','world.json','campaign.json','external-datasets.json'}
            if names != required:
                raise ValueError('invalid_world_backup_members')
            try:
                manifest=json.loads(archive.read('backup-manifest.json').decode('utf-8'))
            except (UnicodeDecodeError,json.JSONDecodeError) as exc:
                raise ValueError('invalid_world_backup_manifest') from exc
            if not isinstance(manifest,dict) or manifest.get('schema')!=1 or manifest.get('format')!='barbara-world-backup-v1':
                raise ValueError('unsupported_world_backup')
            file_hashes=manifest.get('files')
            if not isinstance(file_hashes,dict) or set(file_hashes)!={'world.json','campaign.json','external-datasets.json'}:
                raise ValueError('invalid_world_backup_manifest')
            payloads={}
            for name,expected in file_hashes.items():
                if not isinstance(expected,str) or len(expected)!=64: raise ValueError('invalid_world_backup_digest')
                raw=archive.read(name)
                if hashlib.sha256(raw).hexdigest()!=expected: raise ValueError('world_backup_integrity_mismatch:'+name)
                try: payloads[name]=json.loads(raw.decode('utf-8'))
                except (UnicodeDecodeError,json.JSONDecodeError) as exc: raise ValueError('invalid_world_backup_json:'+name) from exc
        world_doc=payloads['world.json']; campaign_doc=payloads['campaign.json']; external=payloads['external-datasets.json']
        if not isinstance(world_doc,dict) or not isinstance(external,list): raise ValueError('invalid_world_backup')
        packages=manifest.get('packages')
        if not isinstance(packages,list) or not packages: raise ValueError('invalid_world_backup_packages')
        for ref in packages:
            if not isinstance(ref,dict) or set(ref)!={'type','id','version','sha256'}: raise ValueError('invalid_world_backup_package')
            pkg=self.packages.get(ref['type'],ref['id'],ref['version'])
            if pkg.archive_sha256!=ref['sha256']: raise ValueError('world_backup_package_hash_mismatch:'+ref['id'])
        world_id = target_world_id or world_doc.get('world_id')
        campaign_id = target_campaign_id or world_doc.get('campaign_id')
        if not isinstance(world_id,str) or not world_id: raise ValueError('invalid_world_id')
        if not isinstance(campaign_id,str) or not campaign_id: raise ValueError('invalid_campaign_id')
        # Fail before mutating if identifiers already exist.
        try: self.worlds.get(world_id)
        except KeyError: pass
        else: raise ValueError('world_already_exists')
        if self.state.has_campaign(campaign_id): raise ValueError('campaign_already_exists')
        system_ref=next((r for r in packages if r['type']=='system'),None)
        if system_ref is None or system_ref['id']!=world_doc.get('system_id') or system_ref['version']!=world_doc.get('system_version'):
            raise ValueError('world_backup_system_mismatch')
        created=False
        try:
            world=self.worlds.create(
                world_id, world_doc.get('title') or world_id, system_ref['id'], system_version=system_ref['version'],
                campaign_id=campaign_id, metadata=deepcopy(world_doc.get('metadata') or {}),
            ); created=True
            for ref in world_doc.get('enabled_modules',[]):
                world=self.worlds.enable_module(world_id,ref['id'],ref['version'])
            self.state.import_campaign(campaign_doc,campaign_id=campaign_id,replace=False)
            unresolved=[]
            for mount in external:
                path=mount.get('resolved_path') if isinstance(mount,dict) else None
                if not isinstance(path,str) or not Path(path).exists():
                    unresolved.append(deepcopy(mount)); continue
                self.worlds.mount_external_dataset(
                    world_id,kind=mount['kind'],namespace=mount['namespace'],local_path=path,
                    revision=mount.get('revision',''),mutable=bool(mount.get('mutable',True)),metadata=mount.get('metadata') or {},
                )
            return {'world':self.worlds.get(world_id).snapshot(),'campaign_id':campaign_id,'unresolved_external_datasets':unresolved}
        except Exception:
            if self.state.has_campaign(campaign_id): self.state.delete_campaign(campaign_id)
            if created:
                try: self.worlds.delete(world_id)
                except Exception: pass
            raise

    def compact_world(self, world_id: str, *, keep_last=1000, chunk_size=500):
        # Maintenance is only allowed with the World stopped. This avoids a live
        # session committing against a state snapshot which no longer contains the
        # same hot event tail.
        if world_id in self._sessions:
            raise RuntimeError('world_must_be_stopped_for_compaction')
        world=self.worlds.get(world_id)
        return self.state.compact_event_log(world.campaign_id,keep_last=keep_last,chunk_size=chunk_size)

    def stop_world(self, world_id: str):
        engine = self._sessions.pop(world_id, None)
        if engine is not None:
            engine.close()
            return True
        return False
