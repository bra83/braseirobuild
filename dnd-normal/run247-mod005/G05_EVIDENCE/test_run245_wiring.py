from pathlib import Path
import json
import re
p=Path(__file__).resolve().parents[1]
source=p/'app/src/main/java/com/braseiro/dnd5e'
db=(source/'AppDb.java').read_text()
ui=(source/'MainActivity.java').read_text()
forced=(source/'MonsterForcedMovementRules.java').read_text()
typed_guard=(source/'MonsterActionEconomyGuard.java').read_text()
condition_timer=(source/'MonsterConditionTimerLedger.java').read_text()
condition_backup=(source/'MonsterConditionBackupCodec.java').read_text()
rhw=(source/'RavenloftBestiaryRuntimeEngine.java').read_text()
legacy_flow=(source/'StatblockCombatFlowEngine.java').read_text()
on_hit_test=(p/'app/src/test/java/com/braseiro/dnd5e/Run245G05RhwOnHitConditionRobolectricTest.java').read_text()
first_record_encoded=re.search(r'^\s+"((?:\\.|[^"\\])*)"\s*,?$',rhw,re.M)
assert first_record_encoded, 'RHW compiled source record missing'
first_record=json.loads(json.loads('"'+first_record_encoded.group(1)+'"'))
first_action=first_record['actions'][0]
first_basis=first_record['mechanical_basis']
start=db.index('    public JSONObject applyCompiledStatblockMultiattack(')
end=db.index('    /** RUN237: source-compiled push/pull;',start)
multi=db[start:end]
start_ui=ui.index('    private void compiledMonsterMultiattackDialog(')
end_ui=ui.index('    /** Guided compiled monster action with a target saving throw;',start_ui)
form=ui[start_ui:end_ui]
checks={
 'multiattack preflight includes forced-source spec': 'MonsterForcedMovementRules.compiled(' in multi,
 'multiattack verifies immutable declared source': 'compiled_effect_source_changed_' in multi,
 'multiattack rejects prior displacement before action spend': multi.index('forced_movement_requires_final_component_')<multi.index('spendCombatantTurnResource('),
 'multiattack checks occupied/wall destination before action spend': multi.index('compiledMonsterForcedMovementPlan(')<multi.index('spendCombatantTurnResource('),
 'multiattack rejects invalid destination type': 'invalid_forced_destination_' in multi,
 'multiattack rejects unnecessary destination': 'forced_movement_not_applicable_' in multi,
 'multiattack commits forced movement after action spend': multi.index('commitCompiledMonsterForcedMovement(')>multi.index('spendCombatantTurnResource('),
 'multiattack remains one atomic batch': 'combatAtomic("compiled_statblock_multiattack"' in multi,
 'multiattack UI only offers last component coordinate selection': 'if(i==count-1){' in form and 'forcedXFields[i]=labeledField' in form,
 'multiattack UI records explicit destination': '.put("forced_destination_x",destX).put("forced_destination_y",destY)' in form,
 'forced movement closes diagonal corners': 'forced_movement_corner_blocked' in forced and 'blockedCells[sideAY][sideAX]' in forced,
 'source guard blocks unmodeled actions': 'MonsterActionEconomyGuard.attackRiderBlock(' in multi,
 'no schema migration': 'DB_SCHEMA=61' in (p/'version.properties').read_text(),
 'backup untouched': 'BACKUP_FORMAT=21' in (p/'version.properties').read_text(),
 'original appId': 'applicationId = "com.braseiro.dnd5e"' in (p/'app/build.gradle.kts').read_text() or 'com.braseiro.dnd5e' in (p/'app/build.gradle.kts').read_text(),
 'RHW bite has typed on-hit frightened rider': first_action.get('condition_on_hit')=='frightened' and 'condition_on_fail' not in first_action,
 'RHW bite source identity is audible': first_basis.get('source_edition')=='RHW (2026)' and first_basis.get('source_action_id')=='Brain-Rending Bite' and first_basis.get('source_page')==243 and first_basis.get('source_url','').startswith('https://5e.tools/bestiary.html#'),
 'RHW source-turn timer only accepts explicit supported triggers': 'source_turn_start' in condition_timer and 'condition_duration_trigger_not_compiled' in condition_timer,
 'source timers advance on owning actor start': 'clockActor==activeActorId' in condition_timer and 'source_actor_id' in condition_timer,
 'on-hit condition uses typed action guard in combat and UI': 'attackRiderBlock(sourceAction)' in db and 'attackRiderBlock(candidate)' in ui,
 'unsupported opaque on-hit text is fail-closed': 'effect_on_hit' in typed_guard and 'unresolved_attack_hit_rider_requires_typed_handler' in typed_guard,
 'on-hit ownership conflict is rejected before action spend': 'compiledMonsterOnHitConditionBlock(actorId,targetId' in db and db.index('compiledMonsterOnHitConditionBlock(actorId,targetId')<db.index('spendCombatantTurnResource(actorId,resourceCost'),
 'multiattack blocks on-hit riders lacking component commit': 'attackRiderBlock(componentSource,false)' in multi and 'unsupported_multiattack_on_hit_rider' in typed_guard,
 'legacy flow refuses unsupported on-hit rider before damage': 'attackRiderBlock(declaredAction,false)' in legacy_flow and 'unsupported_on_hit_rider_in_legacy_flow' in legacy_flow,
 'legacy flow retains ordinary attack path': 'legacyFlowBlocksUnsupportedOnHitButKeepsPlainAttack' in on_hit_test and 'Negative Energy Burst' in on_hit_test,
 'Backup21 remaps source timer and active turn actor': 'timer.put("source_actor_id",mapped' in condition_backup and 'active_actor_id' in condition_backup,
 'focused Robolectric test covers Backup21 and atomic retry': 'Backup21IntegrityEnvelope.check' in on_hit_test and 'run245_abort_second_timer' in on_hit_test and 'preexistingConditionOwnershipConflictFailsClosedBeforeMutation' in on_hit_test,
}
for k,v in checks.items():
 if not v: print('FAIL',k)
assert all(checks.values())
print(f'RUN245_G05_STATIC_WIRING_PASS checks={len(checks)} (NOT Android/Robolectric)')
