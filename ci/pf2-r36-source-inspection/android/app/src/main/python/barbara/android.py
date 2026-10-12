"""Minimal Android-facing entrypoint for embedding Barbara through a Python runtime.

The Kotlin/Java host only exchanges UTF-8 JSON strings with this module. It does not
need to know Barbara's internal classes, which lets the motor evolve independently.
"""
from __future__ import annotations

import json
import os
from threading import RLock
from .bridge import HostBridge
from .engine import BarbaraEngine
from .gemini import GeminiProvider
from .platform import BarbaraPlatform
from .state import CampaignState
from .audio import AudioDirector
from .host_contract import HostIntegrationLoader, external_rpg_contract
from .supervisor import SupervisorBridge

_lock = RLock()
_bridge: HostBridge | None = None
_config: tuple | None = None
_platform: BarbaraPlatform | None = None
_platform_root: str | None = None
_supervisor: SupervisorBridge | None = None
_supervisor_contract = None


def configure(*, api_key=None, model='gemini-3.5-flash-lite', rag_db_path=None, use_gemini=True):
    """Configure one process-local Barbara bridge and return a small status dict."""
    global _bridge, _config
    if not isinstance(use_gemini, bool):
        raise ValueError('invalid_use_gemini')
    if model is not None and (not isinstance(model, str) or not model.strip()):
        raise ValueError('invalid_model')
    if rag_db_path is not None and (not isinstance(rag_db_path, str) or not rag_db_path.strip()):
        raise ValueError('invalid_rag_db_path')
    cfg = (api_key, model, rag_db_path, use_gemini)
    with _lock:
        if _bridge is None or _config != cfg:
            previous=_bridge
            resolved_key = api_key or os.getenv('GEMINI_API_KEY')
            provider = GeminiProvider(api_key=resolved_key, model=model) if (use_gemini and resolved_key) else None
            _bridge = HostBridge(BarbaraEngine(provider=provider, rag_db_path=rag_db_path, strict_world_introductions=True))
            _config = cfg
            if previous is not None:previous.close()
    active_model = model if (use_gemini and (api_key or os.getenv('GEMINI_API_KEY'))) else None
    return {'configured': True, 'model': active_model, 'rag_persistent': rag_db_path is not None}


def configure_json(api_key=None, model='gemini-3.5-flash-lite', rag_db_path=None, use_gemini=True):
    """Positional, JSON-safe adapter intended for Kotlin/Java bridges.

    Chaquopy-style hosts can call Python functions positionally much more simply than
    keyword-only functions. Returning JSON also avoids relying on Python dict string
    formatting, which is not valid JSON.
    """
    status = configure(api_key=api_key, model=model, rag_db_path=rag_db_path, use_gemini=use_gemini)
    return json.dumps(status, ensure_ascii=False, sort_keys=True, separators=(',', ':'))



def _provider_from_config():
    with _lock:
        cfg = _config
    if not cfg:
        return None
    api_key, model, _rag_db_path, use_gemini = cfg
    resolved_key = api_key or os.getenv('GEMINI_API_KEY')
    return GeminiProvider(api_key=resolved_key, model=model) if (use_gemini and resolved_key) else None


def configure_platform(root_path):
    global _platform, _platform_root
    if not isinstance(root_path, str) or not root_path.strip():
        raise ValueError('invalid_barbara_platform_root')
    resolved = os.path.abspath(os.path.expanduser(root_path))
    with _lock:
        if _platform is None or _platform_root != resolved:
            previous = _platform
            _platform = BarbaraPlatform(resolved)
            _platform_root = resolved
            if previous is not None:
                previous.close()
    return json.dumps({'configured': True, 'root': resolved, 'protocol': 2}, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def _require_platform() -> BarbaraPlatform:
    with _lock:
        if _platform is None:
            raise RuntimeError('barbara_platform_not_configured')
        return _platform


def platform_install_package(archive_path, expected_sha256=None):
    package = _require_platform().install_package(archive_path, expected_sha256=expected_sha256)
    return json.dumps(package.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def platform_packages():
    platform = _require_platform()
    items=[]
    for package_type in ('system','module','world'):
        for item in platform.packages.all(package_type):
            items.append(item.snapshot())
    return json.dumps(items, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def platform_worlds():
    return json.dumps([w.snapshot() for w in _require_platform().worlds.all()], ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def platform_create_world(world_id, title, system_id, system_version=None, campaign_id=None):
    world = _require_platform().create_world(world_id, title, system_id, system_version=system_version, campaign_id=campaign_id)
    return json.dumps(world.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def platform_enable_module(world_id, module_id, version=None):
    world = _require_platform().worlds.enable_module(world_id, module_id, version)
    return json.dumps(world.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def platform_disable_module(world_id, module_id):
    world = _require_platform().worlds.disable_module(world_id, module_id)
    return json.dumps(world.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def _world_session(world_id):
    platform = _require_platform()
    session = platform._sessions.get(world_id)
    if session is None:
        session = platform.launch_world(world_id, provider=_provider_from_config())['session']
    return session


def platform_turn(world_id, request_json):
    request = json.loads(request_json) if isinstance(request_json, str) else request_json
    out = _world_session(world_id).turn(request)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def platform_vtt_snapshot(world_id, actor_id=None, role='gm'):
    out = _world_session(world_id).vtt_snapshot(actor_id=actor_id,role=role)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def platform_vtt_command(world_id, actor_id, role, command_json):
    command = json.loads(command_json) if isinstance(command_json, str) else command_json
    out = _world_session(world_id).vtt_command(command, actor_id=actor_id, role=role)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def platform_backup_world(world_id, archive_path):
    out = _require_platform().backup_world(world_id, archive_path)
    # launch_plan contains install paths and is operational metadata, not needed by Android UI.
    out.pop('launch_plan', None)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def platform_restore_world(archive_path, target_world_id=None, target_campaign_id=None):
    out = _require_platform().restore_world(archive_path, target_world_id=target_world_id, target_campaign_id=target_campaign_id)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)



def platform_stop_world(world_id):
    return json.dumps({'stopped':bool(_require_platform().stop_world(world_id))},ensure_ascii=False,sort_keys=True,separators=(',',':'))


def platform_compact_world(world_id, keep_last=1000, chunk_size=500):
    out=_require_platform().compact_world(world_id,keep_last=int(keep_last),chunk_size=int(chunk_size))
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def reset_for_tests():
    global _bridge, _config, _platform, _platform_root, _supervisor, _supervisor_contract
    with _lock:
        if _bridge is not None:_bridge.close()
        _bridge = None
        _config = None
        if _platform is not None: _platform.close()
        _platform = None
        _platform_root = None
        _supervisor = None
        _supervisor_contract = None


def _require_bridge() -> HostBridge:
    with _lock:
        if _bridge is None:
            raise RuntimeError('barbara_android_not_configured')
        return _bridge


def new_campaign(campaign_id, system_id, **initial):
    return _require_bridge().new_campaign(campaign_id, system_id, **initial)


def turn(state_json, request_json):
    """Return a JSON string containing both updated state and turn result."""
    return _require_bridge().turn_json(state_json, request_json)


def accept_narrative(state_json, request_json):
    """Validate/accept presentation-only prose generated by an on-device Android model."""
    return _require_bridge().accept_narrative_json(state_json, request_json)


def _json_value(raw, *, default=None, expected=None, error='invalid_json_payload'):
    if raw is None:
        value=default
    elif isinstance(raw,str):
        try:value=json.loads(raw)
        except (TypeError,json.JSONDecodeError) as exc:raise ValueError(error) from exc
    else:
        value=raw
    if expected is not None and not isinstance(value,expected):
        raise ValueError(error)
    return value



def configure_supervisor(contract_json):
    """Configure presentation-only SupervisorBridge for a complete external RPG host.

    The host keeps canonical authority. Barbara may inspect/gate presentation but cannot
    mutate the supplied CampaignState projection.
    """
    global _supervisor, _supervisor_contract
    manifest = _json_value(contract_json, expected=dict, error='invalid_host_contract_manifest')
    contract = HostIntegrationLoader().load(manifest)
    with _lock:
        engine = _require_bridge().engine
        _supervisor = SupervisorBridge(engine, contract)
        _supervisor_contract = contract
    return json.dumps(contract.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def configure_external_supervisor_json(system_id, system_version, capabilities_json='[]', datasets_json='{}', metadata_json='{}'):
    """Safe Android helper: all canonical domains remain host-owned."""
    global _supervisor, _supervisor_contract
    if not isinstance(system_id, str) or not system_id.strip():
        raise ValueError('invalid_host_system_id')
    if not isinstance(system_version, str) or not system_version.strip():
        raise ValueError('invalid_host_system_version')
    capabilities = _json_value(capabilities_json, default=[], expected=list, error='invalid_host_capabilities')
    datasets = _json_value(datasets_json, default={}, expected=dict, error='invalid_host_datasets')
    metadata = _json_value(metadata_json, default={}, expected=dict, error='invalid_host_metadata')
    contract = external_rpg_contract(system_id.strip(), system_version.strip(), capabilities=capabilities, datasets=datasets, metadata=metadata)
    with _lock:
        engine = _require_bridge().engine
        _supervisor = SupervisorBridge(engine, contract)
        _supervisor_contract = contract
    return json.dumps(contract.snapshot(), ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def _require_supervisor() -> SupervisorBridge:
    with _lock:
        if _supervisor is None:
            raise RuntimeError('barbara_supervisor_not_configured')
        return _supervisor


def supervisor_inspect(state_json, text, importance='normal', input_channel=None):
    out = _require_supervisor().inspect(state_json, text, importance=importance, input_channel=input_channel)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def supervisor_validate_resolution(state_json, resolution_json=None):
    resolution = _json_value(resolution_json, default=None, error='invalid_supervisor_resolution')
    if resolution is not None and not isinstance(resolution, dict):
        raise ValueError('invalid_supervisor_resolution')
    out = _require_supervisor().validate_resolution(state_json, resolution)
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def supervisor_finalize_narrative(state_json, text, request_id, resolution_json=None, candidate_json=None, importance='normal', input_channel=None):
    resolution = _json_value(resolution_json, default=None, error='invalid_supervisor_resolution')
    if resolution is not None and not isinstance(resolution, dict):
        raise ValueError('invalid_supervisor_resolution')
    candidate = _json_value(candidate_json, default=None, error='invalid_narrative_candidate')
    if candidate is not None and not isinstance(candidate, (dict, str)):
        raise ValueError('invalid_narrative_candidate')
    out = _require_supervisor().finalize_narrative(
        state_json, text, request_id, resolution=resolution, candidate=candidate, importance=importance, input_channel=input_channel,
    )
    return json.dumps(out, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)

def world_introduction_contract(state_json, occasion=None, context_json=None, evidence_json=None):
    """Return the cross-system presentation obligation without mutating host truth."""
    state=CampaignState.from_json(state_json)
    engine=_require_bridge().engine
    if occasion is None or not str(occasion).strip():
        occasion=engine._story_occasion(state,{'mode':'fiction'})
    context=_json_value(context_json,default=None,error='invalid_world_intro_context')
    if context is None:
        context=engine.context_engine.build(state)
    elif not isinstance(context,dict):
        raise ValueError('invalid_world_intro_context')
    evidence=_json_value(evidence_json,default=[],expected=list,error='invalid_world_intro_evidence')
    out=engine.presentation_policy.build(state,str(occasion),context=context,evidence=evidence)
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def poi_create_command(request_json):
    request=_json_value(request_json,expected=dict,error='invalid_poi_request')
    out=_require_bridge().engine.poi_commands.create(**request)
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def poi_update_command(poi_json, patch_json, tick, reason, actor=None):
    poi=_json_value(poi_json,expected=dict,error='invalid_poi')
    patch=_json_value(patch_json,expected=dict,error='invalid_poi_patch')
    out=_require_bridge().engine.poi_commands.update(poi,patch=patch,tick=int(tick),reason=reason,actor=actor)
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def poi_discover_command(poi_json, tick, method='exploration'):
    poi=_json_value(poi_json,expected=dict,error='invalid_poi')
    out=_require_bridge().engine.poi_commands.discover(poi,tick=int(tick),method=method)
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def poi_visible(canonical_json='[]', campaign_json='[]', procedural_json='[]', gm=False):
    canonical=_json_value(canonical_json,default=[],expected=list,error='invalid_canonical_pois')
    campaign=_json_value(campaign_json,default=[],expected=list,error='invalid_campaign_pois')
    procedural=_json_value(procedural_json,default=[],expected=list,error='invalid_procedural_pois')
    out=_require_bridge().engine.poi_projection.visible(canonical,campaign,procedural,gm=bool(gm))
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def poi_generation_proposals(context_json, templates_json, trigger, seed_hint=None):
    context=_json_value(context_json,expected=dict,error='invalid_poi_generation_context')
    templates=_json_value(templates_json,expected=list,error='invalid_poi_templates')
    out=_require_bridge().engine.poi_generation.propose(context=context,templates=templates,trigger=trigger,seed_hint=seed_hint)
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)


def audio_scene_plan(events_json, context_json=None, catalog_json=None, mix_json=None, narration_active=False):
    """Derive presentation-only audio commands from canonical events and host assets."""
    events=_json_value(events_json,expected=list,error='invalid_game_events')
    context=_json_value(context_json,default={},expected=dict,error='invalid_audio_context')
    catalog=_json_value(catalog_json,default={},expected=dict,error='invalid_audio_catalog')
    mix=_json_value(mix_json,default={},expected=dict,error='invalid_audio_mix')
    director=AudioDirector(catalog=catalog,mix=mix)
    out=director.scene_plan(events,context=context,narration_active=bool(narration_active))
    return json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
