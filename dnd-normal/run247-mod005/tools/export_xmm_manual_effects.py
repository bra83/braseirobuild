#!/usr/bin/env python3
"""Export a source-pointer inventory of effects not yet executable end-to-end."""
import argparse
import hashlib
import json
import sys
from pathlib import Path


def _category(source_field):
    return {"action":"action", "bonus":"bonus_action", "reaction":"reaction",
            "legendary":"legendary_action"}[source_field]


def _family(action):
    if action.get("condition_choice_on_hit"):
        return "condition_choice_on_hit"
    if action.get("condition_recurring_damage"):
        return "condition_recurring_damage"
    if action.get("condition_on_target_reduced_to_zero_hp"):
        return "condition_at_zero_hp"
    if action.get("swallow_conditions_manual") or action.get("swallow_exit_condition"):
        return "swallow_and_escape"
    if action.get("effect_on_fail") or action.get("effect_on_hit"):
        return "forced_movement_or_compound_rider"
    if action.get("condition_on_fail"):
        return "save_failure_condition"
    if action.get("condition_on_hit"):
        return "attack_hit_condition"
    if action.get("spells"):
        return "monster_spell_action"
    if action.get("name") == "Multiattack":
        return "multiattack_sequence"
    if action.get("manual_effect_choice_required"):
        return "explicit_effect_choice"
    if action.get("damage_on_save") or action.get("damage"):
        return "damage_or_save_branch"
    return "other_source_effect"


def _owner(family):
    return {
        "condition_choice_on_hit":"Xmm2025ConditionChoiceEngine / HybridXmmBatchResolutionEngine",
        "save_failure_condition":"StatblockCombatFlowEngine / MonsterConditionRecoveryLedger",
        "attack_hit_condition":"Xmm2025AttackConditionGateway / StatblockCombatFlowEngine",
        "multiattack_sequence":"Xmm2025MultiattackPlanIndex / StatblockCombatFlowEngine",
        "monster_spell_action":"Xmm2025SpellUsageIndex / CompiledSupplementExecutionEngine",
        "condition_recurring_damage":"XMM recurring-condition executor (not implemented)",
        "condition_at_zero_hp":"XMM zero-HP transition executor (not implemented)",
        "swallow_and_escape":"Xmm2025GrappleEscapeRules / StatblockCombatFlowEngine",
        "forced_movement_or_compound_rider":"MonsterForcedMovementRules / StatblockCombatFlowEngine",
    }.get(family,"XMM action-specific executor (not implemented)")


def build_inventory(source, compiled, proof_sha="", save_condition_profiles=None,
                    recurring_damage_profiles=None):
    import verify_xmm_503_source as verifier
    compiled_by_id={row.get("id"):row for row in compiled}
    save_profiles={(row.get("id"),row.get("action"),row.get("source_pointer")):row
        for row in (save_condition_profiles or [])}
    recurring_profiles={(row.get("id"),row.get("action"),row.get("ptr")):row
        for row in (recurring_damage_profiles or [])}
    entries=[]
    categories=(("action","actions",None),("bonus","special_actions","bonus_action"),
                ("reaction","special_actions","reaction"),
                ("legendary","special_actions","legendary_action"))
    for monster_index,monster in enumerate(source):
        profile=compiled_by_id.get(monster.get("name"),{})
        for source_field,dest_field,category in categories:
            destination=profile.get(dest_field,[])
            if category:
                destination=[item for item in destination if item.get("category")==category]
            for slot,action in enumerate(monster.get(source_field,[])):
                if not verifier.source_action_requires_manual_resolution(action,monster.get("name","")):
                    continue
                compiled_action=destination[slot] if slot<len(destination) else {}
                if verifier.source_action_has_typed_condition_contract(monster["name"],action,compiled_action):
                    continue
                family=_family(compiled_action)
                is_choice_preview=(family=="condition_choice_on_hit" and
                    verifier.condition_choice_contract_matches(monster["name"],action,compiled_action))
                entry_pointer=f"/monster/{monster_index}/{source_field}/{slot}/entries/0"
                normalized_action=verifier.RCH.sub("",action.get("name","")).strip()
                save_profile=save_profiles.get((monster["name"],normalized_action,entry_pointer))
                is_save_condition_preview=(save_profile is not None)
                recurring_profile=recurring_profiles.get((monster["name"],normalized_action,entry_pointer))
                is_recurring_preview=(recurring_profile is not None)
                entry_pointer=f"/monster/{monster_index}/{source_field}/{slot}/entries/0"
                contract_fields=("attack_modes","to_hit","reach_ft","range_normal_ft","range_long_ft",
                    "damage","damage_type","damage_on_save","save_ability","save_dc",
                    "condition_on_hit","condition_on_fail","condition_choice_on_hit",
                    "condition_choice_max_target_size","condition_duration_rounds",
                    "condition_duration_tick","condition_recovery_save_ability",
                    "condition_recovery_save_dc","condition_recovery_trigger",
                    "condition_recovery_condition","condition_recovery_linked_conditions",
                    "effect_on_hit","effect_on_fail","condition_recurring_damage",
                    "condition_recurring_damage_type","condition_recurring_damage_tick",
                    "escape_dc","condition_max_target_size","recharge","spells",
                    "manual_effect_choice_required","swallow_conditions_manual",
                    "condition_on_target_reduced_to_zero_hp","speed_reduction_ft",
                    "speed_reduction_duration_tick")
                contract={key:compiled_action[key] for key in contract_fields if key in compiled_action}
                blocker=("Preview tipado; execução em combate depende da integração protegida de UI/SQLite após o PR #39."
                    if is_choice_preview else
                    "Proposta de condições validada; duração/recuperação, demais efeitos da ação e aplicação dependem de integração de combate."
                    if is_save_condition_preview else
                    "Preview do tick fonte-vinculado; ciclo de vida, defesas e aplicação dependem de integração de combate."
                    if is_recurring_preview else
                    "A ação permanece manual: requer executor específico, contexto adicional ou integração ainda ausente.")
                test_id=("Xmm2025ConditionChoiceEngineTest#allSixSourceContractsRemainManualAndSourceBound"
                    if is_choice_preview else
                    "Xmm2025SaveConditionPreviewEngineTest#allFiftyEightProfilesRemainSourceBoundAndManual"
                    if is_save_condition_preview else
                    "Xmm2025RecurringDamagePreviewEngineTest#allThreeSourceProfilesRetainTheirDistinctTicks"
                    if is_recurring_preview else "")
                status=("BLOCKED_MISSING_CONTEXT" if is_choice_preview or is_save_condition_preview or is_recurring_preview else
                    "MANUAL_UNSUPPORTED")
                if is_save_condition_preview and (not save_profile.get("save_ability") or
                        save_profile.get("save_dc",0)<1):
                    status="SOURCE_REVIEW_REQUIRED"
                if not entry_pointer or not action.get("name"):
                    status="SOURCE_REVIEW_REQUIRED"
                entries.append({
                    "source_pointer":entry_pointer,
                    "monster_id":monster["name"],
                    "action_id":action.get("name",""),
                    "family":family,
                    "mechanical_contract":contract,
                    "engine_owner":_owner(family),
                    "coverage_status":status,
                    "test_id":test_id,
                    "proof_sha":proof_sha if is_choice_preview or is_save_condition_preview or is_recurring_preview else "",
                    "blocker_reason":blocker,
                })
    pointers=[row["source_pointer"] for row in entries]
    if len(set(pointers))!=len(pointers):
        raise ValueError("duplicate_source_pointer_in_manual_inventory")
    return {
        "schema":"XMM_SOURCE_EFFECT_INVENTORY_V1",
        "source_commit":verifier.PIN,
        "source_bestiary_blob_sha1":verifier.BESTIARY_SHA,
        "source_monsters":len(source),
        "manual_unmodeled_effect_count":len(entries),
        "tipado_preview_pendente_integracao_count":sum(
            row["coverage_status"]=="BLOCKED_MISSING_CONTEXT" for row in entries),
        "source_review_required_count":sum(
            row["coverage_status"]=="SOURCE_REVIEW_REQUIRED" for row in entries),
        "full_monster_combat_automation_certified":False,
        "entries":entries,
    }


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--source-dir",type=Path,required=True)
    parser.add_argument("--project-root",type=Path,required=True)
    parser.add_argument("--out",type=Path,required=True)
    parser.add_argument("--proof-json",type=Path,
        help="Verifier JSON whose bytes will be SHA-256 pinned in preview entries")
    args=parser.parse_args()
    sys.path.insert(0,str(args.project_root.resolve()/"tools"))
    import verify_xmm_503_source as verifier
    source=json.loads((args.source_dir/"bestiary-xmm.json").read_text(encoding="utf-8"))["monster"]
    runtime=verifier.java_rows((args.project_root/verifier.JAVA_RUNTIME).read_text(encoding="utf-8"))
    save_profiles=verifier.java_rows(
        (args.project_root/verifier.JAVA_SAVE_CONDITIONS).read_text(encoding="utf-8"))
    recurring_profiles=verifier.java_rows(
        (args.project_root/verifier.JAVA_RECURRING_DAMAGE).read_text(encoding="utf-8"))
    proof_sha=(hashlib.sha256(args.proof_json.read_bytes()).hexdigest()
        if args.proof_json else "")
    inventory=build_inventory(source,runtime,proof_sha,save_profiles,recurring_profiles)
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in inventory.items() if k!="entries"},ensure_ascii=False,indent=2))


if __name__=="__main__":
    main()
