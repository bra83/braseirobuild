"""Adaptação Pathfinder 2e Remaster do Motor Barbara 1.1.2.

O Braseiro permanece autoridade persistente e mecânica: AppDb,
Pathfinder2eRulesEngine, RuleRetrievalEngine, MasterRulesArbiter, Atlas, combate,
áudio e Mundo Vivo. Barbara 1.1.2 supervisiona intenção, agência, continuidade,
descoberta do mundo e validação narrativa. Não existe segunda chamada Gemini nem
segundo banco de campanha.

O Barbara 1.1.2 ainda não possui um SystemModule mecânico nativo Pathfinder 2e;
portanto esta ponte deliberadamente NÃO converte PF2e para D&D. Toda mecânica PF2e
continua fail-closed no host e só é aplicada com regra recuperada e verificada.
"""
from __future__ import annotations

import json

from barbara import __version__
from barbara import android as _barbara_android
from barbara.intent import ActionClassifier
from barbara.mechanics import MechanicsAuthority
from barbara.narrative import NarrativePolicy

_policy = NarrativePolicy()
_mechanics = MechanicsAuthority()
_classifier = ActionClassifier()


_supervisor_snapshot_cache = None

def _supervisor_snapshot():
    """Configure Barbara 1.1.2 in external-host supervisor mode, never as a second game engine."""
    global _supervisor_snapshot_cache
    if _supervisor_snapshot_cache is not None:
        return _supervisor_snapshot_cache
    _barbara_android.configure(use_gemini=False)
    datasets = {
        "atlas": {"kind": "atlas", "namespace": "pathfinder2e.inner_sea", "revision": "v6-grid-native-hires", "owner": "host"},
        "rules": {"kind": "rules", "namespace": "pathfinder2e.remaster", "revision": "host", "owner": "host"},
        "campaign_pois": {"kind": "world", "namespace": "pathfinder2e.campaign_pois", "revision": "db-schema-58", "owner": "host"},
    }
    raw = _barbara_android.configure_external_supervisor_json(
        "pathfinder_2e",
        "8.5.0",
        _dump(["PLAYER_ACTION", "GM_HELP", "atlas", "campaign_pois", "living_world", "vtt", "audio"]),
        _dump(datasets),
        _dump({"mode": "external_host_supervisor", "mechanics": "Pathfinder2eRulesEngine", "database": "AppDb", "duplicate_gemini_call": False, "second_database": False}),
    )
    _supervisor_snapshot_cache = json.loads(raw)
    return _supervisor_snapshot_cache


def _dump(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def health_json():
    return _dump({
        "ok": True,
        "barbara_version": __version__,
        "integration": "pathfinder_2e_embedded_1_1_2",
        "provider_mode": "host_gemini",
        "duplicate_gemini_call": False,
        "second_database": False,
        "system_id": "pathfinder_2e",
        "system_name": "Pathfinder 2e Remaster",
        "mechanics_authority": "Pathfinder2eRulesEngine",
        "native_barbara_system_module": False,
        "native_barbara_system_module_reason": "Barbara 1.1.2 supervisor mode keeps Pathfinder2eRulesEngine and AppDb host-authoritative",
        "input_channels": [ActionClassifier.CHANNEL_PLAYER_ACTION, ActionClassifier.CHANNEL_GM_HELP],
        "supervisor_mode": "external_host",
        "supervisor_contract": _supervisor_snapshot(),
    })


def plan_json(action, importance="normal", narrative=True, non_advancing=False,
              story_occasion="continuation", input_channel=None):
    action = "" if action is None else str(action)
    importance = importance if importance in {"routine", "normal", "meaningful", "climax"} else "normal"
    if input_channel not in {None, ActionClassifier.CHANNEL_PLAYER_ACTION, ActionClassifier.CHANNEL_GM_HELP}:
        raise ValueError("invalid_input_channel")
    if not action.strip():
        # Openings may be synthesized by the host, but Barbara's classifier requires
        # non-empty text. Preserve deterministic behavior with a neutral fiction cue.
        action_for_classification = "continuação da cena"
    else:
        action_for_classification = action
    classification = _classifier.classify(action_for_classification, input_channel=input_channel)

    # The UI channel is binding. PLAYER_ACTION never freezes because it contains a
    # question or addressed dialogue. GM_HELP never advances time, position, NPCs,
    # clocks, world simulation or campaign memory.
    if not narrative or non_advancing or input_channel == ActionClassifier.CHANNEL_GM_HELP:
        plan = _policy.turn_plan(action_for_classification, False, importance, input_channel=input_channel)
    else:
        first = _policy.turn_plan(action_for_classification, False, importance, input_channel=input_channel)
        # Barbara may decide that the prose contains uncertainty, but it never chooses
        # PF2e DC/modifier/degree. Those remain exclusively host-owned.
        inferred = _mechanics.requires_rule(action_for_classification, False, first)
        plan = _policy.turn_plan(action_for_classification, inferred, importance, input_channel=input_channel)
    plan = _policy.apply_story_obligation(plan, story_occasion)
    directives = _policy.narrator_directives(importance, _policy.question_count(action_for_classification), plan)
    return _dump({
        "barbara_version": __version__,
        "system_id": "pathfinder_2e",
        "input_channel": input_channel,
        "input_classification": classification,
        "turn_plan": plan,
        "narrative_policy": directives,
        "world_discovery_contract": directives["world_discovery_contract"],
        "authority": {
            "host_world": True,
            "host_rules_engine": "Pathfinder2eRulesEngine+RuleRetrievalEngine+MasterRulesArbiter",
            "host_atlas": True,
            "host_audio": True,
            "narrator_proposal_only": True,
            "barbara_mechanics_authority": False,
            "pf2e_mechanics_fail_closed": True,
            "rag_fail_closed": True,
            "input_channel_authoritative": True,
            "canonical_position_owner": "host_pathfinder_2e",
            "fast_scene_contract": "host_commit_before_narrative",
        },
        "host_world_authority": True,
        "duplicate_gemini_call": False,
        "second_database": False,
    })


def validate_envelope_json(envelope_json, action="", importance="normal",
                           story_occasion="continuation", input_channel=None):
    try:
        envelope = json.loads(envelope_json)
        if not isinstance(envelope, dict):
            raise ValueError("invalid_envelope")
        if input_channel not in {None, ActionClassifier.CHANNEL_PLAYER_ACTION, ActionClassifier.CHANNEL_GM_HELP}:
            raise ValueError("invalid_input_channel")
        narration = str(envelope.get("narration", "")).strip()
        # GM help is rendered in a separate non-fiction panel and may legitimately
        # return no narration field. Fictional turns retain the narrative checks.
        if input_channel != ActionClassifier.CHANNEL_GM_HELP and not narration:
            raise ValueError("invalid_narration")
        importance = importance if importance in {"routine", "normal", "meaningful", "climax"} else "normal"
        if narration and input_channel != ActionClassifier.CHANNEL_GM_HELP:
            _policy.validate_player_agency(action, narration)
            _policy.validate_response_coverage(action, narration)
            _policy.validate_scene_ending(narration, importance)
            _policy.validate_story_obligation(narration, story_occasion)
        return _dump({
            "valid": True,
            "code": "ok",
            "barbara_version": __version__,
            "system_id": "pathfinder_2e",
            "input_channel": input_channel,
        })
    except Exception as exc:
        code = str(exc).split(":", 1)[0] or exc.__class__.__name__
        soft_quality = {
            "historia_de_abertura_resumida_demais",
            "historia_substituida_por_relatorio",
            "historia_com_poucos_paragrafos",
            "historia_sem_cena_suficiente",
            "cena_sem_abertura_para_decisao",
        }
        if code in soft_quality:
            return _dump({
                "valid": True,
                "needs_repair": True,
                "quality_warning": True,
                "code": code,
                "barbara_version": __version__,
                "system_id": "pathfinder_2e",
                "input_channel": input_channel,
            })
        return _dump({
            "valid": False,
            "code": code,
            "barbara_version": __version__,
            "system_id": "pathfinder_2e",
            "input_channel": input_channel,
        })
