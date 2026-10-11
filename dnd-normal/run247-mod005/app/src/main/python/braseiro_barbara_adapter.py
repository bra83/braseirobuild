"""Adaptação D&D/Mystara do Motor Barbara 1.1.3.

O Braseiro permanece a autoridade persistente e mecânica: AppDb, motores de regras
D&D/Mystara, RuleRetrievalEngine, Atlas, dados físicos, combate, áudio e Mundo Vivo.
Barbara supervisiona intenção, agência, política narrativa e consistência da projeção
operacional, sem segunda base de dados e sem duplicar chamada Gemini.
"""
from __future__ import annotations

import json
import re
from copy import deepcopy

from barbara import __version__
from barbara.adapters import AdapterRegistry
from barbara.intent import ActionClassifier
from barbara.mechanics import MechanicsAuthority
from barbara.narrative import NarrativePolicy
from barbara.state import CampaignState
from barbara.system_module import SystemModuleRegistry

_policy = NarrativePolicy()
_mechanics = MechanicsAuthority()
_modules = SystemModuleRegistry()
_adapters = AdapterRegistry(_modules)
_classifier = ActionClassifier()
_dnd = _modules.get("dnd")
_mystara = _modules.get("mystara")

# O núcleo Barbara 1.1.3 permanece genérico. Estas obrigações de apresentação
# pertencem ao host D&D/Mystara e preservam o contrato narrativo maduro 6.3.x.
DND_STORY_TARGETS = {
    "campaign_opening": (8500, 16000),
    "first_arrival": (6000, 12000),
    "changed_return": (4200, 9000),
    "poi_discovery": (4200, 9000),
    "geography_discovery": (3500, 7600),
    "weather_transition": (2400, 6000),
}
DND_STORY_WORD_TARGETS = {
    "campaign_opening": (1200, 2200),
    "first_arrival": (850, 1600),
    "changed_return": (600, 1200),
    "poi_discovery": (600, 1200),
    "geography_discovery": (500, 1000),
    "weather_transition": (350, 800),
}
DND_STORY_PARAGRAPH_TARGETS = {
    "campaign_opening": (10, 18),
    "first_arrival": (8, 14),
    "changed_return": (6, 12),
    "poi_discovery": (6, 12),
    "geography_discovery": (5, 10),
    "weather_transition": (4, 8),
}
DND_STORY_OCCASIONS = {"continuation", *DND_STORY_TARGETS.keys()}

HOST_CONTRACT_REQUIRED = {
    "contract_id": "braseiro.barbara.host",
    "contract_version": 1,
    "state_authority": "sqlite_host",
    "mutation_protocol": "proposal_validate_commit",
    "scene_commit_id_required": True,
    "idempotent_scene_commits": True,
    "grounding_envelope_version": 1,
    "roll_result_authority": "host_physical_faces",
    "direct_database_write": False,
    "world_event_ledger": True,
    "failure_policy": "fail_closed",
}


def _dump(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def _system_id(value):
    return "mystara" if str(value or "").strip().lower() == "mystara" else "dnd"


def _module(system_id):
    return _mystara if _system_id(system_id) == "mystara" else _dnd


def _apply_story_obligation(plan, occasion):
    occasion = str(occasion or "continuation").strip() or "continuation"
    if occasion not in DND_STORY_OCCASIONS:
        raise ValueError("invalid_story_obligation")
    out = deepcopy(plan)
    out["story_obligation"] = occasion
    if occasion in DND_STORY_TARGETS:
        out["target_chars"] = list(DND_STORY_TARGETS[occasion])
        out["target_words"] = list(DND_STORY_WORD_TARGETS[occasion])
        out["target_paragraphs"] = list(DND_STORY_PARAGRAPH_TARGETS[occasion])
    return out


def _dnd_world_discovery_directives(directives):
    out = deepcopy(directives)
    flags = out.setdefault("directives", {})
    flags.update({
        "poi_discovery_must_be_story": True,
        "geography_discovery_must_be_story": True,
        "weather_transition_must_be_story": True,
        "assume_setting_literacy_zero_unless_memory_proves_it": True,
        "introduce_proper_nouns_contextually": True,
        "explain_what_where_why_now_on_first_relevant_mention": True,
        "never_use_unanchored_proper_noun_chains": True,
        "opening_requires_world_to_scene_camera_move": True,
    })
    contract = out.setdefault("world_discovery_contract", {})
    contract.update({
        "assume_setting_literacy_zero_unless_campaign_memory_proves_it": True,
        "introduce_every_new_relevant_proper_noun_with_what_it_is_where_it_sits_and_why_it_matters_now": True,
        "never_write_as_if_the_player_has_read_a_setting_book": True,
        "world_to_region_to_place_to_people_to_immediate_problem_camera_move": True,
    })
    return out


def _validate_dnd_story_obligation(narration, occasion):
    if occasion not in DND_STORY_TARGETS:
        return True
    text = str(narration or "").strip()
    low = text.lower()
    min_words = DND_STORY_WORD_TARGETS[occasion][0]
    min_paragraphs = DND_STORY_PARAGRAPH_TARGETS[occasion][0]
    words = len(re.findall(r"\b[\wÀ-ÿ'’-]+\b", text, flags=re.UNICODE))
    paragraphs = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
    if words < min_words:
        raise ValueError("historia_de_abertura_resumida_demais")
    if len(paragraphs) < min_paragraphs:
        raise ValueError("historia_sem_paragrafos_suficientes")
    if re.match(r"^(resumo|contexto|situação atual|estado atual|summary|current situation)\s*:", low):
        raise ValueError("historia_substituida_por_relatorio")
    min_sentences = {"campaign_opening":10,"first_arrival":8,"changed_return":6,"poi_discovery":6,"geography_discovery":5,"weather_transition":4}[occasion]
    if len([x for x in re.split(r"(?<=[.!?])\s+|\n+", text) if x.strip()]) < min_sentences:
        raise ValueError("historia_sem_cena_suficiente")
    forbidden = ("como você já sabe","como voce ja sabe","como vocês já sabem","como voces ja sabem","vocês sabem que","voces sabem que","como todos sabem","é claro que vocês conhecem","e claro que voces conhecem")
    if any(marker in low for marker in forbidden):
        raise ValueError("historia_pressupoe_conhecimento_do_cenario")
    head = low[:4200 if occasion == "campaign_opening" else 3000]
    if occasion == "campaign_opening":
        scale=("mundo","continente","região","regiao","reino","território","territorio","fronteira","província","provincia","costa","vale","planície","planicie","montanha","floresta","deserto","cidade","vila","terras de","parte de")
        approach=("estrada","caminho","rota","viagem","jornada","chegada","aproxima","ao norte","ao sul","a leste","a oeste","entre ","dentro de","fica ","situa","distância","distancia","dias de","horas de","através","atraves")
        if not any(x in head for x in scale) or not any(x in head for x in approach):
            raise ValueError("historia_sem_orientacao_do_mundo")
    elif occasion == "first_arrival":
        place=("cidade","vila","bairro","porto","fortaleza","templo","ruína","ruina","aldeia","acampamento","região","regiao","entrada","muralha","estrada","trilha","ponte","praça","praca")
        threshold=("chega","chegada","aproxima","entrada","portão","portao","atravessa","avista","surge","caminho","estrada","trilha","limiar","primeiros","ao se aproximar")
        if not any(x in head for x in place) or not any(x in head for x in threshold):
            raise ValueError("historia_sem_limiar_de_chegada")
    elif occasion == "poi_discovery":
        markers=("entrada","porta","torre","ruína","ruina","templo","ponte","caverna","fortaleza","monumento","edifício","edificio","santuário","santuario","masmorra","marco")
        if not any(x in head for x in markers):
            raise ValueError("ponto_de_interesse_sem_apresentacao_concreta")
    elif occasion == "geography_discovery":
        markers=("terreno","vale","floresta","montanha","planície","planicie","deserto","pântano","pantano","costa","colina","rio","estrada","trilha","rocha","vegetação","vegetacao","relevo")
        if not any(x in head for x in markers):
            raise ValueError("geografia_nova_sem_orientacao")
    elif occasion == "weather_transition":
        sensory=("vento","chuva","nuvem","céu","ceu","frio","calor","neve","névoa","nevoa","trovo","umidade","poeira","visibilidade","lama","som","cheiro")
        consequence=("estrada","trilha","rua","mercado","patrulha","viagem","abrigo","passo","movimento","visibilidade","rastro","entrega","caravana","barco","ponte")
        if not any(x in head for x in sensory) or not any(x in head for x in consequence):
            raise ValueError("clima_mudou_sem_consequencia_perceptivel")
    return True


def health_json():
    snap = _dnd.snapshot()
    return _dump({
        "ok": True,
        "barbara_version": __version__,
        "integration": "dnd_embedded_1_1_3",
        "device_profile_contract": "barbara-device-profile-v1",
        "provider_mode": "host_gemini",
        "duplicate_gemini_call": False,
        "system_id": "dnd",
        "system_module_version": _dnd.version,
        "capabilities": sorted(snap["capabilities"]),
        "systems": [a.system_id for a in _adapters.all()],
        "host_contract_required": HOST_CONTRACT_REQUIRED,
    })


def system_contract_json(system_id="dnd"):
    """Contrato declarativo do sistema; nunca substitui o motor mecânico do host."""
    sid = _system_id(system_id)
    snap = _module(sid).snapshot()
    return _dump({
        "system_id": snap["system_id"],
        "family": snap["family"],
        "version": snap["version"],
        "capabilities": snap["capabilities"],
        "entity_schemas": snap["entity_schemas"],
        "action_definitions": snap["action_definitions"],
        "roll_definitions": snap["roll_definitions"],
        "rule_definitions": snap["rule_definitions"],
        "effect_definitions": snap["effect_definitions"],
        "inventory_model": snap["inventory_model"],
        "combat_model": snap["combat_model"],
        "time_model": snap["time_model"],
        "ui_schema": snap["ui_schema"],
        "narrative_vocabulary": snap["narrative_vocabulary"],
        "host_contract_required": HOST_CONTRACT_REQUIRED,
    })


def state_adapter_json(host_json, system_id="dnd"):
    """Valida uma projeção mínima do AppDb em CampaignState.

    Esta é apenas uma barreira de contrato e round-trip. O objeto não é persistido
    pelo Barbara; o SQLite do host continua sendo a única fonte canônica.
    """
    data = json.loads(host_json or "{}")
    if not isinstance(data, dict):
        raise ValueError("invalid_host_state")
    sid = _system_id(system_id)
    module = _module(sid)
    campaign_id = str(data.get("campaign_id") or ("braseiro-mystara" if sid == "mystara" else "braseiro-dnd"))
    state = CampaignState(
        campaign_id=campaign_id,
        system_id=sid,
        tick=max(0, int(data.get("tick") or 0)),
        location=str(data.get("location") or ""),
        facts=deepcopy(data.get("facts") or {}),
        world_flags=deepcopy(data.get("world_flags") or {}),
        npcs=deepcopy(data.get("npcs") or {}),
        factions=deepcopy(data.get("factions") or {}),
        rumors=deepcopy(data.get("rumors") or []),
        events=deepcopy(data.get("events") or []),
        memory=deepcopy(data.get("memory") or []),
        clocks=deepcopy(data.get("clocks") or {}),
        economy=deepcopy(data.get("economy") or {}),
        weather=deepcopy(data.get("weather") or {}),
        player_state=deepcopy(data.get("player_state") or {}),
        scene=deepcopy(data.get("scene") or {}),
        notes=deepcopy(data.get("notes") or {}),
        sites=deepcopy(data.get("sites") or {}),
        public_ledger=deepcopy(data.get("public_ledger") or []),
        secret_ledger=deepcopy(data.get("secret_ledger") or []),
        discovery=deepcopy(data.get("discovery") or {}),
        state_version=max(0, int(data.get("state_version") or 0)),
        game_time=deepcopy(data.get("game_time") or {}),
        system_module_version=module.version,
    )
    restored = CampaignState.from_json(state.to_json())
    return _dump({
        "valid": True,
        "system_id": restored.system_id,
        "campaign_id": restored.campaign_id,
        "location": restored.location,
        "state_version": restored.state_version,
        "system_module_version": restored.system_module_version,
        "roundtrip_equal": restored.to_dict() == state.to_dict(),
        "secret_ledger_count": len(restored.secret_ledger),
        "public_ledger_count": len(restored.public_ledger),
        "host_state_authority": True,
        "host_contract_required": HOST_CONTRACT_REQUIRED,
    })


def plan_json(action, importance="normal", narrative=True, non_advancing=False,
              story_occasion="continuation", input_channel=None, system_id="dnd"):
    action = "" if action is None else str(action)
    importance = importance if importance in {"routine", "normal", "meaningful", "climax"} else "normal"
    sid = _system_id(system_id)
    if input_channel not in {None, ActionClassifier.CHANNEL_PLAYER_ACTION, ActionClassifier.CHANNEL_GM_HELP}:
        raise ValueError("invalid_input_channel")
    classification = _classifier.classify(action, input_channel=input_channel)
    # A origem da caixa é soberana. PLAYER_ACTION nunca vira consulta do Mestre só
    # porque contém '?' ou fala; GM_HELP nunca avança tempo, posição ou Mundo Vivo.
    if not narrative or non_advancing or input_channel == ActionClassifier.CHANNEL_GM_HELP:
        plan = _policy.turn_plan(action, False, importance, input_channel=input_channel)
    else:
        first = _policy.turn_plan(action, False, importance, input_channel=input_channel)
        inferred = _mechanics.requires_rule(action, False, first)
        plan = _policy.turn_plan(action, inferred, importance, input_channel=input_channel)
    plan = _apply_story_obligation(plan, story_occasion)
    directives = _dnd_world_discovery_directives(_policy.narrator_directives(importance, _policy.question_count(action), plan))
    module = _module(sid).snapshot()
    return _dump({
        "barbara_version": __version__,
        "system_id": sid,
        "input_channel": input_channel,
        "input_classification": classification,
        "turn_plan": plan,
        "narrative_policy": directives,
        "world_discovery_contract": directives["world_discovery_contract"],
        "system_module": {
            "system_id": sid,
            "version": module["version"],
            "capabilities": module["capabilities"],
            "time_model": module["time_model"],
            "roll_definitions": module["roll_definitions"],
            "rule_definitions": module["rule_definitions"],
            "combat_model": module["combat_model"],
            "inventory_model": module["inventory_model"],
        },
        "authority": {
            "host_world": True,
            "host_rules_engine": "DndRulesEngine+MystaraRulesEngine+RuleRetrievalEngine+MasterRulesArbiter",
            "host_atlas": True,
            "host_audio": True,
            "host_physical_dice": True,
            "narrator_proposal_only": True,
            "rag_fail_closed": True,
            "input_channel_authoritative": True,
            "canonical_position_owner": "host_dnd",
            "fast_scene_contract": "host_commit_before_narrative",
        },
        "host_world_authority": True,
        "duplicate_gemini_call": False,
        "host_contract_required": HOST_CONTRACT_REQUIRED,
    })


def validate_envelope_json(envelope_json, action="", importance="normal",
                           story_occasion="continuation", input_channel=None,
                           system_id="dnd"):
    try:
        sid = _system_id(system_id)
        envelope = json.loads(envelope_json)
        if not isinstance(envelope, dict):
            raise ValueError("invalid_envelope")
        narration = str(envelope.get("narration", "")).strip()
        if not narration:
            raise ValueError("invalid_narration")
        importance = importance if importance in {"routine", "normal", "meaningful", "climax"} else "normal"
        if input_channel not in {None, ActionClassifier.CHANNEL_PLAYER_ACTION, ActionClassifier.CHANNEL_GM_HELP}:
            raise ValueError("invalid_input_channel")
        _policy.validate_player_agency(action, narration)
        _policy.validate_response_coverage(action, narration)
        _policy.validate_scene_ending(narration, importance)
        _validate_dnd_story_obligation(narration, story_occasion)
        return _dump({
            "valid": True,
            "code": "ok",
            "barbara_version": __version__,
            "system_id": sid,
            "input_channel": input_channel,
            "host_contract_required": HOST_CONTRACT_REQUIRED,
        })
    except Exception as exc:
        code = str(exc).split(":", 1)[0] or exc.__class__.__name__
        # Problemas de profundidade/apresentação pedem reparo, mas não invalidam uma
        # cena estruturalmente segura depois que os guardas Java também a verificam.
        soft_quality = {
            "historia_de_abertura_resumida_demais",
            "historia_substituida_por_relatorio",
            "historia_com_poucos_paragrafos",
            "historia_sem_paragrafos_suficientes",
            "historia_sem_cena_suficiente",
            "cena_sem_abertura_para_decisao",
        }
        payload = {
            "valid": code in soft_quality,
            "code": code,
            "barbara_version": __version__,
            "system_id": _system_id(system_id),
            "input_channel": input_channel,
            "host_contract_required": HOST_CONTRACT_REQUIRED,
        }
        if code in soft_quality:
            payload.update({"needs_repair": True, "quality_warning": True})
        return _dump(payload)


def configure_device_profile_json(metrics_json="{}"):
    """Encaminha somente métricas de apresentação ao contrato 1.1.3."""
    from barbara.android import configure_device_profile_json as configure_profile
    return configure_profile(metrics_json)


def current_device_profile_json():
    """Retorna o perfil de apresentação atual sem tocar em estado canônico."""
    from barbara.android import device_profile_json
    return device_profile_json()
