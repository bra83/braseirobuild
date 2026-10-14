"""Source contract guards for canonical Kingmaker PDF p. 522 / 530 / 531.
No claim of whole-book certification: these guards detect regression of verified fixes.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")

def method_body(signature):
    pos=SOURCE.index(signature)
    boundary=SOURCE.find("\n    public ",pos+len(signature))
    return SOURCE[pos:boundary if boundary>=0 else pos+6000]

def test_collect_taxes_grants_economy_bonus_not_invented_rp():
    s=method_body('public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)')
    assert '"economy_circumstance_bonus"' in s
    assert '"resource_points", 2' not in s and '"resource_points", 1' not in s
    assert '"SUCCESS".equals(degree)?(repeated?1:0)' in s
    assert '"CRITICAL_FAILURE".equals(degree)?2:0' in s
    assert 'change.put(ruin,1)' in s
    assert 'Só é permitido Coletar Impostos uma vez' in s

def test_tax_circumstance_on_future_economy_checks_from_active_turn_log():
    s=method_body('private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject structureContext)')
    assert 'taxesCircumstanceBonus(turn.optLong("id"))' in s
    assert '"economy".equals(ability)' in s
    assert '"collect_taxes".equals(activity)' in s
    assert '.put("taxes_circumstance",taxes)' in s

def test_quell_unrest_is_d6_with_explicit_ruin_alternative():
    s=method_body('public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,int successD6,int failureD4,')
    assert 'successD6<1||successD6>6' in s
    assert '"CRITICAL_SUCCESS".equals(d)?-successD6' in s
    assert '"CRITICAL_FAILURE".equals(d)&&"unrest".equals(choice)?failureD4:0' in s
    assert 'change.put(a,1).put(b,1)' in s
    assert 'limite de uma tentativa por turno' in s

def test_failed_claim_hex_consumes_one_rp_and_critical_stability_penalty():
    s=method_body('public JSONObject resolveClaimHex(String hexId,int naturalRoll,String skillKey,String notes)')
    assert 'state().optInt("resource_points")<1' in s
    assert 'new JSONObject().put("resource_points",-1)' in s
    assert 'stability_circumstance_penalty' in s
    assert 'Math' not in s[:140]  # No helper-generated invented bonus.
    assert '.put("unrest",1)' not in s

def test_claim_penalty_persists_to_next_kingdom_turn_and_expires():
    s=SOURCE
    assert "a.degree='CRITICAL_FAILURE' AND t.turn_number>=? AND t.turn_number<=?" in s
    assert '"turn_number-1"' not in s
    assert '"stability".equals(ability)?claimHexStabilityPenalty(s.optInt("turn_number")):0' in s

def test_version_and_non_intentional_scope():
    assert "VERSION_NAME=9.0.37" in (ROOT/"version.properties").read_text()

def test_collect_taxes_rejects_other_skills_before_check():
    s=method_body('public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)')
    guard='if(!"trade".equals(skill))throw new IllegalArgumentException'
    assert guard in s
    assert s.index(guard) < s.index('recordCheck("collect_taxes"')

def test_expansion_expert_bonus_is_limited_to_claim_hex_and_kingdom_level_four():
    check=method_body('private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject structureContext)')
    assert '"claim_hex".equals(activity)&&s.optInt("level",1)>=4?2:0' in check
    assert 'int circumstanceBonus=Math.max(taxes,expansionExpert);' in check
    assert 'vacancy-ruin+circumstanceBonus-claimPenalty' in check
    assert '.put("circumstance_bonus_applied",circumstanceBonus)' in check
    assert '.put("expansion_expert_circumstance",expansionExpert)' in check

def test_unfinished_build_keeps_prepaid_materials_and_options():
    body=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    assert 'prepaidMaterials=resume?jsonObject(pending.optString("commodity_costs_json","{}"))' in body
    assert 'if(!paidChoice.equals(commodityChoice))throw' in body
    assert 'previous.optJSONObject("build_options")' in body
    assert 'new String[]{"trade_focus","ruin","urban_grid","border"}' in body
    assert body.index('if(!paidChoice.equals(commodityChoice))throw') < body.index('db.runInTransactionChecked')
    assert 'stone=resume?prepaidMaterials.optInt("stone"):def.optInt("stone",0)' in body
    assert 'lumber=resume?prepaidMaterials.optInt("lumber"):def.optInt("lumber",0)' in body
    assert '.put("commodity_choice",commodityChoice).put("build_options",new JSONObject(buildOptions.toString()))' in body

def test_public_check_delegates_to_contextual_calculation():
    body=method_body('public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)')
    assert 'return recordCheckAtSettlement("",activityKey,title,role,skillKey,natural,dc,sourceRef,notes,awardReputationOnCrit)' in body
    assert 'return recordCheckAtSettlement(settlementKey,activityKey,title,role,skillKey,natural,dc,sourceRef,notes,awardReputationOnCrit,new JSONObject())' in SOURCE

def test_grid_infrastructure_is_placed_persisted_and_repaired_in_place():
    build=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    assert 'requireGridInfrastructure(town,new JSONArray(structures(townKey)),chosenKey,buildOptions)' in build
    assert build.index('requireGridInfrastructure(') < build.index('db.runInTransactionChecked')
    assert 'effects.put("infrastructure_instances",KingmakerStructureSemantics.appendedInfrastructure(existing,k,buildOptions))' in SOURCE
    assert 'trimInstanceEffects(existing,count-1)' in SOURCE
    assert 'requires_special_placement",false)&&!KingmakerStructureSemantics.isInfrastructure(structure)' in SOURCE

def test_twins_preparation_requires_rest_and_persists_before_return():
    body=method_body('public JSONObject prepareTwinsPersona(final String requested,final String campKey)')
    assert 'db.runInTransactionChecked' in body
    assert '!"complete".equals(camp.optString("status"))' in body
    assert 'optInt("total_rest_hours")<8' in body
    assert 'db.getCombatState().optInt("running")==1' in body
    assert 'upsertFeature("companion:twins_persona"' in body
    assert 'chosen.optInt("prepared_day",-1)!=actual.optInt("prepared_day",-2)' in body
    assert 'automaticInvestment(companion,role,twinsCurrentPersona())' in SOURCE

def test_border_placement_is_checked_before_spending_and_island_penalty_is_real():
    build=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    assert 'requireBorderInfrastructure(town,new JSONArray(structures(townKey)),chosenKey,buildOptions)' in build
    assert build.index('requireBorderInfrastructure(') < build.index('db.runInTransactionChecked')
    assert 'originalOptions.has(field)' in build
    check=method_body('private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject structureContext)')
    assert 'islandTradePenalty="trade".equals(key(skillKey))?officialIslandTradePenalty():0' in check
    assert 'civicItemBonus-=islandTradePenalty' in check
    assert '.put("island_trade_item_penalty",-islandTradePenalty)' in check
    assert 'preserveWaterBorders(jsonObject(old.optString("grid_json","{}")),jsonObject(gridJson))' in SOURCE

def test_build_reserves_and_rechecks_actual_lots_before_persisting():
    build=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    assert 'chooseLotPlacement(town,new JSONArray(structures(townKey)),chosenKey,lots,requested,resume?pendingKey:"")' in build
    assert build.index('chooseLotPlacement(') < build.index('db.runInTransactionChecked')
    assert 'sameLotPlacement(saved,requested)' in build
    assert 'effects.put("lot_instances",KingmakerStructureSemantics.appendedLotPlacement(existing,position))' in SOURCE
    assert 'requireLotPlacement(town,new JSONArray(structures(settlementKey)),k,def.optInt("lots"),position,"")' in SOURCE
    assert 'v.put("influence",officialSettlementInfluence(town))' in SOURCE
    assert 'public void declareExistingLotPlacement(' in SOURCE

def test_registered_districts_and_water_mills_use_real_placement():
    assert 'int housingBlocks=builtBlocks<0?blocks:builtBlocks' in SOURCE
    assert 'KingmakerStructureSemantics.builtBlockCount(new JSONArray(structures(settlementKey)))' in SOURCE
    assert 'boolean waterMill=KingmakerStructureSemantics.hasWaterMill(town,rows)' in SOURCE


def test_companion_supplement_has_only_published_adventures_and_no_fake_rewards():
    import json
    data=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    assert len(data["quests"])==7 and len(data["npcs"])==12
    assert data["metadata"]["official_campaign_chapter"] is False
    assert len(data["metadata"]["retirement"]["quests"])==12
    assert data["metadata"]["coverage"]["automatic_rewards_compiled"] is False
    assert all(q["rewards"]=={"gold":0,"xp":0,"items":[]} and q["owner_character_id"]==0 for q in data["quests"])
    importer=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCampaignImporter.java").read_text(encoding="utf-8")
    assert 'return "companions-r36-primary".equals' in importer
    assert 'hasCombatants && !npc.stats.optBoolean("profile_only")' in importer
    assert 'hasNpcCognitive && !npc.stats.optBoolean("profile_only")' in importer
    assert 'canonicalCompanionKey(npc.npcKey.replaceFirst("^npc_",""))' in importer


def test_harrim_protection_changes_saved_encounter_dc_before_actual_roll():
    body=method_body('public JSONObject recordCampHour(String campKey,String activityJson,int encounterRoll)')
    assert 'KingmakerCompanionSemantics.harrimCampingProtection(activities)' in body
    assert 'before.optInt("current_encounter_dc")<=0' in body
    assert 'companion.optInt("available",1)!=1' in body
    assert 'harrimEncounterDcBonus(activities,db.getSceneState().optString("time_of_day"))' in body
    assert 'int dc=baseDc>0?baseDc+nightBonus:0' in body
    assert body.index('harrimCampingProtection(')<body.index('boolean triggered=')
    assert 'v.put("current_encounter_dc",Math.max(0,baseDc-1))' in body
    assert 'v.put("activities_json",activities.toString())' in body


def test_old_companion_milestones_are_retired_without_reversing_history():
    import json
    data=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    assert set(data["metadata"]["retirement"]["kingmaker_features"])=={"milestone:milestone_lealdade_vanguarda","milestone:milestone_resgate_octavia_regongar","milestone:milestone_redencao_tristian_valerie","milestone:milestone_lenda_doze_companheiros"}
    importer=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCampaignImporter.java").read_text(encoding="utf-8")
    assert '"kingmaker_features".equals(table)?"feature_key":"name"' in importer
    assert 'if("kingmaker_features".equals(table)&&cols.contains("status"))v.put("status","inactive")' in importer


def test_registered_settlement_level_tracks_built_blocks_in_saved_state():
    update=method_body('public void updateSettlement(long id,String type,int level,int population,int consumption,int developedBlocks,int occupiedLots,int residentialLots,String gridJson,String storageJson,String notes)')
    assert 'v.put("level",builtBlocks<0?' in update
    assert ':Math.max(1,Math.min(20,builtBlocks)))' in update
    assert 'v.put("level",builtBlocks<0?town.optInt("level",1):Math.max(1,Math.min(20,builtBlocks)))' in SOURCE


def test_demolition_logs_real_roll_and_stores_every_remaining_rubble_lot_atomically():
    body=method_body('public JSONObject resolveDemolish(final String settlementKey,final String structureKey,final int naturalRoll,final int chosenLot,final boolean clearEntire,final String commodity,final int recoveryD6,final String notes)')
    assert 'db.runInTransactionChecked' in body
    assert 'recordCheckAtSettlement(settlementKey,"demolish"' in body
    assert 'KingmakerStructureSemantics.demolitionOutcome(physical,selected,check.optString("degree")' in body
    assert 'rubble.put("status","rubble")' in body and 'rubble.put("lots",1)' in body
    assert 'rubble.put("count",remaining.length())' in body
    assert 'effect=new JSONObject().put("demolition_group",true)' in body
    assert 'KingmakerStructureSemantics.trimInstanceEffects(before,before.optInt("count")-1)' in body
    assert body.index('refreshSettlementOccupancy(')<body.index('officialCommodityCapacity(kind)')
    assert 'p.optInt("lot",0),p.optBoolean("clear_entire",true)' in SOURCE


def test_daily_preparations_use_verified_sleep_date_and_advance_clock_once():
    body=method_body('public JSONObject recordCompletedCampSleep(final String campKey)')
    assert 'db.runInTransactionChecked' in body and 'loggedSleepingHours(' in body
    assert 'logged<required||camp.optInt("elapsed_hours")<required' in body
    assert '.put("completed_day",Math.max(1,db.getSceneState().optInt("day_number",1)))' in body
    assert 'if("logged_camp_hours".equals(rest.optString("sleep_authority")))' in body
    preparation=method_body('public JSONObject prepareTwinsPersona(final String requested,final String campKey)')
    assert 'optInt("completed_day"' in preparation
    assert 'if(previous.optInt("prepared_day")==day){out[0]=previous;return;}' in preparation
    assert 'total=start+0.5d' in preparation and 'db.saveSceneStateChecked(clock)' in preparation
    assert preparation.index('db.saveSceneStateChecked(clock)')<preparation.index('upsertFeature("companion:twins_persona"')
    rules=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java").read_text(encoding="utf-8")
    assert 'restResult.optBoolean("spells_prepared_restored")&&restResult.optBoolean("focus_points_restored")' in rules



def test_growth_is_spatial_and_persisted_inside_construction_transaction():
    build=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    assert 'town.put("kingdom_level",s.optInt("level",1))' in build
    assert build.index('db.runInTransactionChecked') < build.index('applyConstructionGrowth(townKey,') < build.index('recordCheckAtSettlement(townKey,"build_structure"')
    assert 'KingmakerStructureSemantics.constructionTown(before,new JSONArray(structures(settlementKey)),position)' in SOURCE
    assert 'if(!before.optString("type").equals(type))grantSettlementMilestone(type)' in SOURCE
    assert 'O porte muda ao construir nos novos blocos' in SOURCE


def test_completed_construction_records_net_actual_payment_before_insertion():
    build=method_body('public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)')
    receipt=build.index('buildOptions.put("construction_payment"')
    complete=build.index('JSONObject completed=buildStructure(')
    assert receipt<complete
    for field in ('lumber','stone','ore','luxuries'):
        assert f'.put("{field}",{field}-(critical?{field}/2:0))' in build
    assert '.put("resource_points",rp)' in build
    assert 'KingmakerStructureSemantics.appendedConstructionPayment(existing,payment)' in SOURCE
    assert '.put("authority",prepaid?"gift":"paid")' in SOURCE


def test_structure_upgrade_keeps_original_until_real_completion():
    body=method_body('public JSONObject resolveStructureUpgrade(')
    assert 'db.runInTransactionChecked' in body
    assert body.index('recordCheckAtSettlement')<body.index('if(complete)')<body.index('trimInstanceEffects(old')
    assert 'old_structure_still_active' in body and 'reserved_lots' in body
    assert 'additionalUpgradeLots(original,position)' in body
    assert 'Math.max(0,-delta)+crit' in body
    assert 'if(!resume){adjustState(charge' in body
    assert 'recordRpSpent(Math.max(0,differential' not in body
    assert 'Reinicie o upgrade somente no próximo turno' in body


def test_companion_arc_uses_published_quest_not_unverified_legacy_flag():
    body=method_body('public JSONObject ensureCompanionPersonalEvent(')
    assert 'officialCompanionArcStatus(companionKey)' in body
    assert 'questAvailable(companionKey,partyLevel,true,companion.optString("personal_quest_status")' not in body
    assert 'legacy:companion_alias:' in SOURCE
    assert 'mergeLegacyAlias(current,alias)' in SOURCE
    assert "entity_kind='companion' AND entity_id=?" in SOURCE


def test_campaign_objectives_and_completion_share_checked_transaction():
    body=method_body('public JSONObject recordCampaignObjective(')
    assert 'db.runInTransactionChecked' in body and 'requireJournalEligibility(quest)' in body
    assert 'KingmakerCampaignGate.resolveObjective(' in body
    body=method_body('public JSONObject finishCampaignQuest(')
    assert 'db.runInTransactionChecked' in body and 'objectivesResolved(before)' in body
    assert 'if(k.startsWith("q_cg_"))throw new IllegalStateException' in body
    branch=method_body('public JSONObject resolveCompanionAdventureBranch(')
    assert 'db.runInTransactionChecked' in branch
    assert 'personal_quest_status","complete"' in branch
    assert 'companion_branch_receipt:q_cg_' in branch


def test_narrator_uses_the_same_upgrade_backend():
    body=method_body('private JSONObject resolveKingdomActivityWithContext(')
    assert 'if(p.optString("upgrade_from").length()>0)return resolveStructureUpgrade(' in body


def test_narrator_objective_updates_validate_preserved_definition():
    app=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java").read_text(encoding="utf-8")
    assert 'requirePublishedCompanionQuest(previous)' in app
    assert 'KingmakerCampaignGate.transitionState(previous,q)' in app
    assert 'previous.length()==0&&KingmakerCampaignGate.official(q)' in app
    assert 'CompanionSemantics.questAvailable(companionKey,level' in app


def test_mansion_combat_uses_primary_book_stats_and_initial_hp():
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    start=main.index('private String applyCombatPlan(')
    end=main.index('private String buildHeroAiContext(',start)
    body=main[start:end]
    assert 'KingmakerCampaignGate.mansionEncounter(room)' in body
    assert 'KingmakerPreludePolicy.isMansionPhase' in body
    assert 'combat.put("enemies",encounter.optJSONArray("enemies"))' in body
    assert 'Math.max(hp,Math.min(999,e.optInt("max_hp",hp)))' in body
    assert 'enemyResources.put("official_stats"' in body


def test_official_manor_maps_are_copied_and_match_provenance():
    import hashlib,json
    folder=ROOT/"android/app/src/main/assets/pathfinder/kingmaker/maps"
    records=json.loads((folder/"provenance.json").read_text(encoding="utf-8"))
    assert {r['printed_page'] for r in records}=={26,34}
    assert len(records)==2
    for record in records:
        data=(folder/record['asset']).read_bytes()
        assert data[:2]==b'\xff\xd8'
        assert hashlib.sha256(data).hexdigest()==record['image_sha256']
        assert record['feet_per_square']==5
        assert len(record['source_sha256'])==64
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'showKingmakerManorMaps()' in main
    assert 'file:///android_asset/pathfinder/kingmaker/maps/' in main
    assert 'setJavaScriptEnabled(false)' in main


def test_completed_camp_sleep_updates_actual_character_sheets_once():
    body=method_body('public JSONObject recordCompletedCampSleep(')
    assert 'receipt.put("character_recovery",applyVerifiedCampCharacterRecovery(campKey))' in body
    assert 'private JSONArray applyVerifiedCampCharacterRecovery(' in SOURCE
    assert 'db.setCharacterHpChecked(id,recovery.optInt("after_hp"))' in SOURCE
    assert 'sheet.optJSONObject("kingmaker_rest_recovery")' in SOURCE
    assert 'db.updateCharacterSheetChecked(id,sheet.toString())' in SOURCE


def test_manor_fire_actions_persist_actual_damage_and_earned_character_xp():
    assert 'public JSONObject resolveManorFireAction(' in SOURCE
    assert 'db.runInTransactionChecked(new AppDb.CheckedTransaction()' in SOURCE
    assert '"manor_action:"+actionId' in SOURCE
    assert 'KingmakerCampaignGate.manorFireAction(' in SOURCE
    assert 'db.applyCombatantDamagePf2(' in SOURCE
    assert 'sheet.put("experience_points",xp+30)' in SOURCE
    assert 'after.put("xp_awarded",true)' in SOURCE
    assert 'skillKey+"_item_bonus"' in SOURCE
    app=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java").read_text(encoding="utf-8")
    assert 'sheet.put("dead",dead).put("dying",dying).put("wounded",wounded)' in app
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    start=main.index('private String applyCombatPlan(')
    end=main.index('private String buildHeroAiContext(',start)
    body=main[start:end]
    assert body.index('combat.optJSONArray("manor_actions")')<body.index('if("none".equals(action)')
    assert 'showKingmakerManorFire()' in main


def test_manor_smoke_delay_and_air_count_have_actual_combat_and_sheet_effects():
    assert 'public JSONObject resolveManorBreathingAction(' in SOURCE
    assert 'KingmakerCampaignGate.manorSuffocation(' in SOURCE
    assert 'Math.max(0,5+core.attributeModifier(hero,"Constituição"))' in SOURCE
    assert '!jsonObject(combat.optString("resources_json","{}")).has("manor_suffocation_starts_round")' in SOURCE
    assert 'manor_air_last_round' in SOURCE
    assert 'manor_suffocation_checks' in SOURCE
    assert 'death.put("hp",0)' in SOURCE
    assert 'db.updateCharacterSheetChecked(hero.optLong("id"),sheet.toString())' in SOURCE


def test_manor_treasury_reaches_actual_party_inventory_and_gold():
    assert 'public JSONObject resolveManorTreasuryAction(' in SOURCE
    assert 'KingmakerCampaignGate.manorTreasuryOutcome(' in SOURCE
    assert 'adjustCompanyGold(outcome.optInt("gold_delta"))' in SOURCE
    assert 'resources.put("shared_inventory",inventory).put("treasure_receipts",receipts)' in SOURCE
    assert 'db.savePartyStateChecked(party)' in SOURCE
    assert 'sheet.put("thieves_toolkit_broken",true)' in SOURCE
    assert 'int cost="pick_lock".equals(action)?2:' in SOURCE
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'combat.optJSONArray("manor_treasury_actions")' in main
    assert 'showKingmakerManorTreasury()' in main
    app=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java").read_text(encoding="utf-8")
    assert 'syncPlayerCombatantHp(id,ch.optString("name"),safe)' in app
    assert "source_kind='character' AND source_id=?" in app


def test_new_leadership_is_real_persisted_kingdom_activity_not_free_assignment():
    assert 'public JSONObject resolveNewLeadership(final String role,' in SOURCE
    assert 'KingmakerLeadershipSemantics.skillCircumstance(r,sk)' in SOURCE
    assert 'KingmakerLeadershipSemantics.improved(original,preferred)' in SOURCE
    assert 'KingmakerLeadershipSemantics.appointment(degree,r,currentTurn,jaethal,famous,die)' in SOURCE
    assert 'KingmakerLeadershipSemantics.validateTiming(' in SOURCE
    assert 'KingmakerLeadershipSemantics.validateSingleRolePerLeader(' in SOURCE
    assert 'KingmakerLeadershipSemantics.validateInvestedReselection(' in SOURCE
    assert 'public JSONArray reselectInvestedRoles(' in SOURCE
    assert 'public JSONObject setRoleInvested(' in SOURCE
    assert 'db.runInTransactionChecked(new AppDb.CheckedTransaction()' in SOURCE
    assert 'db.getWritableDatabase().update("kingmaker_turn_activities",log' in SOURCE
    assert 'upsertFeature("leadership:"+r,"leadership_assignment"' in SOURCE
    assert 'Depois da fundação, use a atividade Nova Liderança' in SOURCE
    assert 'return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), nat, skill, notes);' in SOURCE


def test_leadership_checks_and_approval_effects_reach_real_runtime():
    assert 'newLeadershipShift+incumbentShift' in SOURCE
    assert 'new JSONObject().put("unrest",unrest)' in SOURCE
    assert 'public JSONObject resolveNewLeadershipApproval(' in SOURCE
    assert 'status.put("approval_pending",false)' in SOURCE
    assert '!"EVENT".equals(turn.optString("phase"))' in SOURCE
    assert 'Mudança oficial de cargo' in SOURCE
    assert 'if(!p.has("natural_roll"))throw new IllegalArgumentException' in SOURCE
    assert 'public JSONObject moveInSettlementGrid(' in SOURCE
    assert 'public JSONObject manageMuseumExhibit(' in SOURCE
    assert 'public JSONObject addCreatureToMenagerie(' in SOURCE
    assert 'public JSONObject resolveCharacterSavingThrow(' in SOURCE
    assert 'public JSONObject resolveCharacterSkillCheck(' in SOURCE
    pf2=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/Pathfinder2eRulesEngine.java").read_text(encoding="utf-8")
    assert 'KingmakerCampaignGate.helixSaveStatusBonus(' in pf2
    assert 'KingmakerStructureSemantics.pcItemBonus(' in pf2
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'kingmakerRepository.resolveKingdomActivity("new_leadership",args)' in main
    assert 'kingmakerRepository.resolveKingdomActivity("new_leadership_approval",args)' in main
    assert (ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerLeadershipSemantics.java").exists()


def test_companion_adventures_candlemere_and_40_official_parts_completeness():
    import json
    assert 'public JSONObject resolveCompanionAdventureCheck(' in SOURCE
    assert 'public JSONObject resolveCompanionAdventureBranch(' in SOURCE
    assert 'KingmakerCompanionSemantics.resolveCompanionAdventureCheck(' in SOURCE
    assert 'KingmakerCompanionSemantics.resolveCompanionAdventureBranch(' in SOURCE
    comp_data = json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    for q in comp_data["quests"]:
        assert len(q["objectives"]) >= 3
        prog = q["progress_json"]
        scenes = prog.get("scenes", [])
        assert len(scenes) >= 1
        assert sum(len(s.get("enemies", [])) for s in scenes) >= 1
        assert sum(len(s.get("skill_checks", [])) for s in scenes) >= 1
        assert len(prog.get("branches", [])) >= 2
    candlemere = json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_08_they_lurk_below.json").read_text(encoding="utf-8"))
    assert len(candlemere["pois"]) == 2
    assert len(candlemere["scenes"]) == 2
    assert len(candlemere["hazards"]) == 3
    assert len(candlemere["npcs"]) == 13
    npc_keys = {n["npc_key"] for n in candlemere["npcs"]}
    for required_npc in ("npc_foras", "npc_korog", "npc_kyetriel", "npc_niodrhast", "npc_leng_envoy", "npc_thresholder_mystic", "npc_candlemere_shoggoth"):
        assert required_npc in npc_keys


def test_companion_branch_ids_are_executable_and_ekundayo_matches_primary_guide():
    """Catalog-to-Java/UI interface and original Companion Guide pp.27-31 regression."""
    import json
    data=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    expected={"amiri","ekundayo","jubilost","linzi","nok_nok","tristian","valerie"}
    assert {q["progress_json"]["companion_key"] for q in data["quests"]}==expected
    for quest in data["quests"]:
        branches=quest["progress_json"]["branches"]
        ids=[b["branch_key"] for b in branches]
        assert len(ids)==len(set(ids))>=2, quest["quest_key"]
        assert all(b.get("description") and b.get("feature_unlocked") for b in branches)
    engine=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    assert 'currentProgress.optJSONArray("branches")' in engine
    assert 'candidate.optString("branch_key")' in engine
    assert 'if(selected==null)throw new IllegalArgumentException' in engine
    assert 'boolean rewardsCertified=currentProgress.optBoolean("automatic_rewards_compiled",false)' in engine
    assert 'if(!declared)throw new IllegalArgumentException' in engine
    assert 'if(!matchedDc)throw new IllegalArgumentException' in engine
    ui=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'showKingmakerCompanionBranchChoices' in ui
    assert 'kingmakerRepository.resolveCompanionAdventureBranch(companion,' in ui
    journal=method_body('public JSONObject finishCampaignQuest(final long questId)')
    assert 'if(k.startsWith("q_cg_"))throw new IllegalStateException' in journal

    ek=next(q for q in data["quests"] if q["quest_key"]=="q_cg_ekundayo")["progress_json"]
    assert ek["feast_points_target"]==5 and ek["feast_contribution_dc"]==22
    assert ek["extra_xp_if_ntavi_does_not_recruit_ekundayo"]==10
    bridge=next(s for s in ek["scenes"] if s["code"]=="STANDOFF")
    foes={e["npc_key"]:(e["level"],e["count"]) for e in bridge["enemies"]}
    assert foes=={"cg_gragglebeard":(7,1),"cg_reaver_ogre_warrior":(3,4),"cg_reaver_wyvern":(6,1)}
    assert any(a["npc_key"]=="cg_ntavi" and a["level"]==6 for a in bridge["allies"])
    hazard=next(h for h in bridge["hazards"] if h["hazard_key"]=="cg_hazard_collapsing_bridge")
    assert hazard["level"]==6 and hazard["saving_throw_dc"]==24
    assert hazard["support"]["hp_current"]==10 and hazard["support"]["hardness"]==10
    assert bridge["treasure"]["silver_pieces"]==25 and bridge["treasure"]["gold_pieces"]==12


def test_ekundayo_book_outcomes_persist_in_actual_journal_and_sheets():
    """Companion Guide pp.27–31: checks -> saved quest -> source outcomes -> published XP."""
    import json
    catalog=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    q=next(x for x in catalog["quests"] if x["quest_key"]=="q_cg_ekundayo")
    progress=q["progress_json"]
    assert progress["feast_points_target"]==5
    assert progress["feast_xp_if_5_victory_points"]==30
    assert progress["feast_xp_otherwise"]==10
    assert progress["extra_xp_if_ntavi_does_not_recruit_ekundayo"]==10
    assert set(progress["ntavi_conversation_results"])=={"CRITICAL_SUCCESS","SUCCESS","FAILURE","CRITICAL_FAILURE"}
    assert progress["ntavi_conversation_results"]["CRITICAL_FAILURE"]["false_silverstep_clue"]
    outcomes=progress["branches"]
    assert len(outcomes)==2
    assert outcomes[0]["available_after"] is True
    assert outcomes[1]["available_after"] is False
    assert all(not b["items_awarded"] and b["xp_awarded"]==0 for b in outcomes)
    scene=next(x for x in progress["scenes"] if x["code"]=="STANDOFF")
    assert {e["npc_key"]:(e["count"],e["level"]) for e in scene["enemies"]}=={
        "cg_gragglebeard":(1,7),"cg_reaver_ogre_warrior":(4,3),"cg_reaver_wyvern":(1,6)}
    assert scene["treasure"]["items"]==["+1 falchion","fear gem talisman","mentalist's staff","spellguard shield"]
    semantic=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    for literal in ['"ntavi_interruption_degree"','"ntavi_recruitment_prevented"',
                    '"ntavi_false_silverstep_clue"','"feast_participated"',
                    'currentProgress.optBoolean("feast_success",false)?30:10',
                    'nextInfluence=Math.max(nextInfluence,8)',
                    '"ekundayo_romance_with_pc"']:
        assert literal in semantic
    assert 'db.runInTransactionChecked' in method_body('public JSONObject resolveCompanionAdventureCheck(')
    assert 'db.runInTransactionChecked' in method_body('public JSONObject resolveCompanionAdventureBranch(')
    journal=method_body('public JSONObject resolveCompanionAdventureCheck(')
    assert 'progress.put("ekundayo_helpful"' in journal
    main=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'showKingmakerCompanionSkillChecks' in main
    assert 'showKingmakerCompanionSkillRoll' in main
    assert 'availableDcs.get(chosenDc.getSelectedItemPosition())' in main
    assert 'kingmakerRepository.resolveCompanionAdventureCheck(companion,' in main


def test_companion_branch_receipt_rejects_conflicting_replays():
    """Android receipts are idempotent only for the same published choice."""
    branch=method_body('public JSONObject resolveCompanionAdventureBranch(')
    assert 'String requestedBranch=clean(branchKey)' in branch
    assert 'requestedBranch.equals(previous.optString("branch_key",""))' in branch
    assert 'previous.put("idempotent_replay",true)' in branch
    assert 'if(KingmakerCampaignGate.completed(quest.optString("status")))' in branch
    assert 'resolveCompanionAdventureBranch(canonical,requestedBranch,comp,progress)' in branch
    engine=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    assert 'currentProgress.optString("branch_resolved","")' in engine


def test_jubilost_other_host_question_causes_official_departure():
    """Companion Guide pp.42-43: group question makes Jubilost leave."""
    data=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    quest=next(q["progress_json"] for q in data["quests"] if q["progress_json"]["companion_key"]=="jubilost")
    choice=next(b for b in quest["branches"] if b["branch_key"]=="ask_stolen_lands_planar_secret")
    assert choice["available_after"] is False
    assert "attitude_after" not in choice
    source=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    assert 'nextProgress.put("jubilost_departure_tone"' in source
    assert 'nextAvailable=0' in source


def test_ekundayo_guide_request_bridge_and_gm_dc_contract():
    """Companion Guide p.31: result of party aid and Request is persisted."""
    import json
    data=json.loads((ROOT/"android/app/src/main/assets/pathfinder/kingmaker/chapters/chapter_12_companion_stories.json").read_text(encoding="utf-8"))
    ek=next(q["progress_json"] for q in data["quests"] if q["progress_json"]["companion_key"]=="ekundayo")
    bridge=next(s for s in ek["scenes"] if s["code"]=="STANDOFF")
    request=next(c for c in bridge["skill_checks"] if c["check_id"]=="ekundayo_request_stay")
    assert request["dc_mode"]=="GM_SELECTED" and "dc" not in request
    semantic=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    assert 'currentProgress.optBoolean("pc_aided_bridge",false)' in semantic
    assert 'currentProgress.optBoolean("bridge_battle_concluded",false)' in semantic
    assert '"ekundayo_request_stay_accepted",deg>=2' in semantic
    assert '"GM_SELECTED".equals(c.optString("dc_mode"))' in semantic
    journal=method_body('public JSONObject recordEkundayoBridgeOutcome(')
    assert 'db.runInTransactionChecked' in journal
    assert '"cg_ekundayo_step_3".equals(obj.optString("id"))' in journal
    assert 'progress.put("bridge_battle_concluded",true).put("pc_aided_bridge",partyHelped)' in journal
    ui=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'recordEkundayoBridgeOutcome(true)' in ui
    assert 'recordEkundayoBridgeOutcome(false)' in ui


def test_ekundayo_party_identity_ntavi_no_intervention_and_romance():
    """Companion Guide pp.27-31: party identity and durable choices."""
    semantic=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerCompanionSemantics.java").read_text(encoding="utf-8")
    assert '"feast_contributor_ids"' in semantic
    assert 'participants.toString()' in semantic and 'java.util.regex.Pattern.compile' in semantic
    assert '"ntavi_conversation_resolved",true' in semantic
    assert '"NO_INTERVENTION"' in semantic
    assert '"ekundayo_romance_pc_id",characterId' in semantic
    assert 'currentProgress.optBoolean("ntavi_conversation_resolved",false)' in semantic
    journal=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")
    assert 'public JSONObject recordEkundayoNtaviNoIntervention()' in journal
    assert 'public JSONObject recordEkundayoRomance(final long characterId)' in journal
    assert 'for(JSONObject hero:db.listActiveCharacters())if(hero.optLong("id")==feastActorId)' in journal
    assert 'feastActorId' in journal and 'progress_json' in journal
    ui=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    assert 'feastActors.get(feastActorSpinner.getSelectedItemPosition()).optLong("id")' in ui
    assert 'recordEkundayoNtaviNoIntervention()' in ui
    assert 'recordEkundayoRomance(heroes.get(chosen.getSelectedItemPosition()).optLong("id"))' in ui
