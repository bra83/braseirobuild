package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Source-verified RHW Brain-Rending Bite, including its source-turn expiry. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run245G05RhwOnHitConditionRobolectricTest {
    private static final String BLOCK="Aberrant Death's Head";
    private static final String ACTION="Brain-Rending Bite";

    private static void installSource() throws Exception {
        JSONObject installed=RavenloftBestiaryRuntimeEngine.install();
        assertEquals(installed.toString(),0,installed.optInt("blocked",0));
        JSONObject profile=CompiledStatblockEngine.runtimeProfile(BLOCK);
        assertTrue(profile.toString(),profile.optBoolean("found",false));
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static long bindActor(AppDb db,String name,int initiative)throws Exception {
        long actor=db.addCombatant(name,initiative,24,24,16,"",false);
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id",BLOCK);ContentValues bind=new ContentValues();
        bind.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",bind,
            "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
        return actor;
    }
    private static long addTarget(AppDb db,String name,int initiative,String conditions) {
        return db.addCombatant(name,initiative,50,50,13,conditions,true);
    }
    private static void readyGeometry(AppDb db,long actor,long target)throws Exception {
        JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        assertEquals(12,scene.optInt("width_cells"));
        db.ensureTacticalTokensForCombat();position(db,actor,2,2);position(db,target,3,2);
    }
    private static JSONObject sourceAction()throws Exception {
        return CompiledStatblockEngine.runtimeProfile(BLOCK).getJSONArray("actions")
            .getJSONObject(0);
    }
    private static long timerReceiptCount(AppDb db) {
        return DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=? AND effect_key LIKE 'monster-condition-turn-v1:%'",
            new String[]{"dnd"});
    }
    private static JSONObject typedSourceTimer(long source,String receipt)throws Exception {
        return new JSONObject().put("schema",MonsterConditionTimerLedger.SCHEMA)
            .put("name","amedrontado").put("rounds",1).put("source_actor_id",source)
            .put("trigger","source_turn_start").put("source_statblock",BLOCK)
            .put("source_action",ACTION).put("source_receipt",receipt);
    }
    private static JSONArray damageComponents(int piercing,int psychic)throws Exception {
        return new JSONArray().put(new JSONObject().put("expression","1d4 + 1")
            .put("damage_type","piercing").put("damage_total",piercing))
            .put(new JSONObject().put("expression","1d6")
                .put("damage_type","psychic").put("damage_total",psychic));
    }
    private static void putTimer(AppDb db,long target,JSONObject timer)throws Exception {
        ContentValues values=new ContentValues();values.put("conditions","amedrontado");
        values.put("condition_timers_json",new JSONArray().put(timer).toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(target),"dnd"}));
    }
    private static void resetLegacyFlow()throws Exception {
        StatblockTurnEngine.reset();
        StatblockEncounterStateEngine.restore(new JSONObject().put("schema","statblock_encounter_state_v1")
            .put("tokens",new JSONObject()));
    }

    @Test public void brainRendingBiteDamagePreservesTypedModifierComponents()throws Exception {
        installSource();
        JSONArray parts=StatblockCombatFlowEngine.damageSpecs(
            "1d4 + 1 + 1d6","piercing + psychic","");
        assertEquals(2,parts.length());
        assertEquals("1d4 + 1",parts.getJSONObject(0).optString("expression"));
        assertEquals("piercing",parts.getJSONObject(0).optString("type"));
        assertEquals(3,StatblockCombatFlowEngine.rollAverage(
            parts.getJSONObject(0).optString("expression"),false));
        assertEquals("1d6",parts.getJSONObject(1).optString("expression"));
        assertEquals("psychic",parts.getJSONObject(1).optString("type"));
        assertEquals(3,StatblockCombatFlowEngine.rollAverage(
            parts.getJSONObject(1).optString("expression"),false));
        assertEquals(6,StatblockCombatFlowEngine.rollAverage(
            parts.getJSONObject(0).optString("expression"),true));
        assertEquals(7,StatblockCombatFlowEngine.rollAverage(
            parts.getJSONObject(1).optString("expression"),true));
    }

    @Test public void biteConditionIsTypedSourceTurnBoundAndSurvivesBackup21()throws Exception {
        installSource();JSONObject action=sourceAction();
        JSONObject compiled=CompiledStatblockEngine.get(BLOCK);
        JSONObject provenance=compiled.getJSONObject("mechanical_basis");
        assertEquals("RHW (2026)",provenance.optString("source_edition"));
        assertEquals(243,provenance.optInt("source_page"));
        assertEquals(BLOCK,provenance.optString("source_entry_id"));
        assertEquals(ACTION,provenance.optString("source_action_id"));
        assertEquals("https://5e.tools/bestiary.html#aberrant%20death's%20head_rhw",
            provenance.optString("source_url"));
        assertEquals("Ravenloft: The Horrors Within",compiled.optString("source_book"));
        assertEquals(3,action.optInt("to_hit"));
        assertEquals("1d4 + 1 + 1d6",action.optString("damage"));
        assertEquals("piercing + psychic",action.optString("damage_type"));
        assertEquals(5,action.optInt("reach_ft"));
        assertFalse(action.has("save_dc"));
        assertFalse(action.has("save_ability"));
        assertEquals("necrotic, psychic",compiled.optString("resistances"));
        assertEquals("poison",compiled.optString("immunities"));
        assertEquals("exhaustion, poisoned",compiled.optString("condition_immunities"));
        JSONObject savingThrows=compiled.getJSONObject("saving_throws");
        assertEquals("+1",savingThrows.optString("int"));
        assertEquals("+4",savingThrows.optString("wis"));
        assertEquals("frightened",action.optString("condition_on_hit"));
        assertEquals(1,action.optInt("condition_duration_rounds"));
        assertEquals("source_turn_start",action.optString("condition_duration_tick"));
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(action));
        assertEquals("unsupported_multiattack_on_hit_rider",
            MonsterActionEconomyGuard.attackRiderBlock(action,false));
        assertEquals("unresolved_attack_hit_rider_requires_typed_handler",
            MonsterActionEconomyGuard.attackRiderBlock(new JSONObject().put("to_hit",3)
                .put("condition_on_hit","frightened")));

        Context context=RuntimeEnvironment.getApplication();String dbName="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;String token="ui-monster-run245-rhw-hit";
        try {
            db.setActiveMapProfile("dnd");actor=bindActor(db,"RUN245 RHW Head",20);
            target=addTarget(db,"RUN245 RHW Target",10,"");readyGeometry(db,actor,target);
            ContentValues resistance=new ContentValues();resistance.put("resistances","psychic");
            assertEquals(1,db.getWritableDatabase().update("combatants",resistance,
                "id=? AND world_profile=?",new String[]{String.valueOf(target),"dnd"}));
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject preview=db.previewCompiledMonsterAttackRollMode(actor,target,BLOCK,ACTION);
            assertTrue(preview.toString(),preview.optBoolean("monster_roll_preview_valid",false));
            JSONObject applied=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,damageComponents(2,3),0,0L,-1,-1,token);
            assertTrue(applied.toString(),applied.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=applied.getJSONObject("resolved");
            assertEquals("frightened",resolved.optString("rider_on_hit_condition"));
            assertTrue(resolved.has("on_hit_condition_transition"));
            assertEquals(5,applied.optInt("damage_total"));
            assertEquals(3,resolved.optInt("damage_after_type_modifiers"));
            JSONArray damageProof=resolved.getJSONArray("damage_components");
            assertEquals(2,damageProof.length());
            assertEquals("piercing",damageProof.getJSONObject(0).optString("damage_type"));
            assertEquals(2,damageProof.getJSONObject(0).optInt("applied_damage"));
            assertEquals("psychic",damageProof.getJSONObject(1).optString("damage_type"));
            assertEquals(1,damageProof.getJSONObject(1).optInt("applied_damage"));
            assertEquals(3,applied.getJSONObject("damage").optInt("applied_damage"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"frightened"));
            JSONArray timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
            assertEquals(1,timers.length());
            assertEquals(actor,timers.getJSONObject(0).getLong("source_actor_id"));
            assertEquals("source_turn_start",timers.getJSONObject(0).getString("trigger"));
            assertEquals(1,timers.getJSONObject(0).getInt("rounds"));

            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.importAll(backup);
            JSONObject restoredActor=db.findCombatantByName("RUN245 RHW Head");
            JSONObject restoredTarget=db.findCombatantByName("RUN245 RHW Target");
            assertNotEquals(actor,restoredActor.optLong("id"));
            assertNotEquals(target,restoredTarget.optLong("id"));
            actor=restoredActor.optLong("id");target=restoredTarget.optLong("id");
            JSONArray restoredTimers=new JSONArray(restoredTarget.optString("condition_timers_json","[]"));
            assertEquals(actor,restoredTimers.getJSONObject(0).getLong("source_actor_id"));
            assertEquals("source_turn_start",restoredTimers.getJSONObject(0).getString("trigger"));
            JSONObject replay=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,damageComponents(2,3),0,0L,-1,-1,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_attack_blocked"));
            assertEquals(47,db.getCombatant(target).optInt("hp"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            JSONObject targetTurn=db.nextTurn();assertEquals(target,targetTurn.optLong("id"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"frightened"));
            JSONObject sourceTurn=db.nextTurn();assertEquals(actor,sourceTurn.optLong("id"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"frightened"));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
            JSONArray transitions=sourceTurn.getJSONArray("condition_timer_transitions");
            JSONObject receipt=null;
            for(int i=0;i<transitions.length();i++)if(transitions.getJSONObject(i).optLong("target_id")==target)
                receipt=transitions.getJSONObject(i).getJSONObject("transition");
            assertNotNull(receipt);assertEquals(actor,receipt.getLong("active_actor_id"));
            assertEquals(1,receipt.getJSONArray("expired").length());
            assertEquals("amedrontado",receipt.getJSONArray("expired").getString(0));

            JSONObject secondBackup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(secondBackup));
            db.importAll(secondBackup);
            JSONObject remappedActor=db.findCombatantByName("RUN245 RHW Head");
            JSONObject remappedTarget=db.findCombatantByName("RUN245 RHW Target");
            assertNotEquals(actor,remappedActor.optLong("id"));
            assertNotEquals(target,remappedTarget.optLong("id"));
            // Locate by schema instead of assuming row order or the old target ID.
            android.database.Cursor rows=db.getReadableDatabase().rawQuery(
                "SELECT effect_key,source FROM effect_receipts WHERE world_profile=? AND effect_key LIKE 'monster-condition-turn-v1:%'",
                new String[]{"dnd"});
            try {
                assertTrue(rows.moveToFirst());boolean remapped=false;
                do {JSONObject proof=new JSONObject(rows.getString(1));
                    if(proof.optLong("active_actor_id")==remappedActor.optLong("id")&&
                        proof.optLong("target_id")==remappedTarget.optLong("id"))remapped=true;
                } while(rows.moveToNext());
                assertTrue("backup remaps source actor inside timer receipt",remapped);
            } finally {rows.close();}
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void sourceTurnExpiryBatchRollsBackAllRowsAndRetriesOnce()throws Exception {
        installSource();Context context=RuntimeEnvironment.getApplication();
        String dbName="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");long actor=bindActor(db,"RUN245 Batch Head",30);
            long first=addTarget(db,"RUN245 Batch Target A",20,"");
            long second=addTarget(db,"RUN245 Batch Target B",10,"");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            putTimer(db,first,typedSourceTimer(actor,"batch-a"));
            putTimer(db,second,typedSourceTimer(actor,"batch-b"));
            assertEquals(first,db.nextTurn().optLong("id"));assertEquals(second,db.nextTurn().optLong("id"));
            db.getWritableDatabase().execSQL("CREATE TRIGGER run245_abort_second_timer BEFORE UPDATE OF condition_timers_json ON combatants WHEN NEW.id="+second+
                " BEGIN SELECT RAISE(ABORT, 'run245 injected timer rollback'); END");
            JSONObject beforeState=db.getCombatState();
            JSONObject failed=db.nextTurn();
            assertEquals("transaction_rolled_back",failed.optString("operation_error"));
            JSONObject afterState=db.getCombatState();
            assertEquals(beforeState.optInt("turn_index"),afterState.optInt("turn_index"));
            assertEquals(beforeState.optInt("round_no"),afterState.optInt("round_no"));
            assertEquals(1,db.getCombatant(second).optInt("active_turn"));
            assertEquals(1,new JSONArray(db.getCombatant(first).optString("condition_timers_json","[]"))
                .getJSONObject(0).getInt("rounds"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(first).optString("conditions"),"frightened"));
            assertEquals(0,timerReceiptCount(db));
            db.getWritableDatabase().execSQL("DROP TRIGGER run245_abort_second_timer");
            JSONObject retried=db.nextTurn();assertEquals(actor,retried.optLong("id"));
            assertEquals(0,new JSONArray(db.getCombatant(first).optString("condition_timers_json","[]")).length());
            assertEquals(0,new JSONArray(db.getCombatant(second).optString("condition_timers_json","[]")).length());
            assertFalse(ConditionTokenGuard.has(db.getCombatant(first).optString("conditions"),"frightened"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(second).optString("conditions"),"frightened"));
            assertEquals(2,timerReceiptCount(db));
            db.nextTurn();db.nextTurn();db.nextTurn();
            assertEquals(2,timerReceiptCount(db));
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void preexistingConditionOwnershipConflictFailsClosedBeforeMutation()throws Exception {
        installSource();Context context=RuntimeEnvironment.getApplication();
        String dbName="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");long actor=bindActor(db,"RUN245 Conflict Head",20);
            long target=addTarget(db,"RUN245 Already Frightened",10,"amedrontado");readyGeometry(db,actor,target);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            int before=db.getCombatant(target).optInt("hp");
            JSONObject result=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,damageComponents(2,3),0,0L,-1,-1,"ui-monster-run245-rhw-conflict");
            assertEquals("on_hit_condition_ownership_ambiguous",
                result.optString("statblock_attack_blocked"));
            assertFalse(result.optBoolean("statblock_attack_committed",false));
            assertFalse(result.has("resolved"));
            assertEquals(before,db.getCombatant(target).optInt("hp"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"frightened"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void naturalOneDoesNotApplyTheOnHitRider()throws Exception {
        installSource();Context context=RuntimeEnvironment.getApplication();
        String dbName="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");long actor=bindActor(db,"RUN245 Miss Head",20);
            long target=addTarget(db,"RUN245 Miss Target",10,"");readyGeometry(db,actor,target);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject result=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                1,0,new JSONArray(),0,0L,-1,-1,"ui-monster-run245-rhw-miss");
            assertTrue(result.toString(),result.optBoolean("statblock_attack_committed",false));
            assertFalse(result.getJSONObject("resolved").optBoolean("hit",true));
            assertFalse(result.getJSONObject("resolved").has("on_hit_condition_transition"));
            assertEquals(50,db.getCombatant(target).optInt("hp"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"frightened"));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void aggregateAndMismatchedDamageProofsFailBeforeMutation()throws Exception {
        installSource();Context context=RuntimeEnvironment.getApplication();
        String dbName="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");long actor=bindActor(db,"RUN245 Typed Head",20);
            long target=addTarget(db,"RUN245 Typed Target",10,"");readyGeometry(db,actor,target);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject aggregate=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,5,0,0L,"ui-monster-run245-aggregate-damage");
            assertEquals("typed_damage_components_required",
                aggregate.optString("statblock_attack_blocked"));
            JSONArray swapped=new JSONArray()
                .put(new JSONObject().put("expression","1d4 + 1").put("damage_type","psychic")
                    .put("damage_total",2))
                .put(new JSONObject().put("expression","1d6").put("damage_type","piercing")
                    .put("damage_total",3));
            JSONObject mismatch=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,swapped,0,0L,-1,-1,"ui-monster-run245-swapped-damage");
            assertEquals("typed_damage_component_source_mismatch",
                mismatch.optString("statblock_attack_blocked"));
            assertEquals(50,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            JSONObject accepted=db.applyCompiledStatblockAttack(actor,target,BLOCK,ACTION,
                15,0,damageComponents(2,3),0,0L,-1,-1,
                "ui-monster-run245-typed-damage");
            assertTrue(accepted.toString(),accepted.optBoolean("statblock_attack_committed",false));
            assertEquals(45,db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void legacyFlowBlocksUnsupportedOnHitButKeepsPlainAttack()throws Exception {
        installSource();resetLegacyFlow();
        try {
            assertTrue(StatblockEncounterStateEngine.spawn("flow-head",BLOCK).optBoolean("spawned"));
            assertTrue(StatblockEncounterStateEngine.spawn("flow-target",BLOCK).optBoolean("spawned"));
            StatblockTurnEngine.setInitiative("flow-head",20);
            StatblockTurnEngine.setInitiative("flow-target",10);StatblockTurnEngine.start();
            JSONObject blocked=StatblockCombatFlowEngine.attack("flow-head","flow-target",ACTION,15);
            assertTrue(blocked.toString(),blocked.optBoolean("blocked",false));
            assertEquals("unsupported_on_hit_rider_in_legacy_flow",blocked.optString("reason"));
            assertEquals(24,StatblockEncounterStateEngine.view("flow-head").optInt("hp"));
            assertEquals(24,StatblockEncounterStateEngine.view("flow-target").optInt("hp"));
            assertTrue(StatblockTurnEngine.state().getJSONArray("order").getJSONObject(0)
                .getJSONObject("economy").optBoolean("action",false));

            resetLegacyFlow();
            assertTrue(StatblockEncounterStateEngine.spawn("flow-ankhtepot","Ankhtepot").optBoolean("spawned"));
            assertTrue(StatblockEncounterStateEngine.spawn("flow-plant","Bodytaker Plant").optBoolean("spawned"));
            StatblockTurnEngine.setInitiative("flow-ankhtepot",20);
            StatblockTurnEngine.setInitiative("flow-plant",10);StatblockTurnEngine.start();
            JSONObject wrongBudget=StatblockCombatFlowEngine.attack("flow-ankhtepot","flow-plant",
                "Death Strike",15);
            assertTrue(wrongBudget.toString(),wrongBudget.optBoolean("blocked",false));
            assertEquals("unsupported_non_action_economy_in_legacy_flow",
                wrongBudget.optString("reason"));
            assertEquals(138,StatblockEncounterStateEngine.view("flow-plant").optInt("hp"));
            assertTrue(StatblockTurnEngine.state().getJSONArray("order").getJSONObject(0)
                .getJSONObject("economy").optBoolean("action",false));
            JSONObject plain=StatblockCombatFlowEngine.attack("flow-ankhtepot","flow-plant",
                "Negative Energy Burst",15);
            assertTrue(plain.toString(),plain.optBoolean("resolved",false));
            assertTrue(plain.optBoolean("hit",false));
            assertEquals("legacy attack result: "+plain,26,plain.optInt("applied_damage"));
            assertEquals(112,StatblockEncounterStateEngine.view("flow-plant").optInt("hp"));
        } finally {resetLegacyFlow();}
    }
    @Test public void typedDamageMustMapEveryDiceGroupToItsSourceDamageType() {
        JSONArray groups=StatblockCombatFlowEngine.damageSpecs(
            "2d6 + 1d8 + 4","fire + psychic","");
        assertEquals(2,groups.length());
        assertEquals("2d6",groups.optJSONObject(0).optString("expression"));
        assertEquals("fire",groups.optJSONObject(0).optString("type"));
        assertEquals("1d8+4",groups.optJSONObject(1).optString("expression"));
        assertEquals("psychic",groups.optJSONObject(1).optString("type"));
        JSONArray flat=StatblockCombatFlowEngine.damageSpecs("12","force","");
        assertEquals(1,flat.length());
        assertEquals("12",flat.optJSONObject(0).optString("expression"));
        try {
            StatblockCombatFlowEngine.damageSpecs("1d8","fire + psychic","");
            fail("Dois tipos não podem ser mapeados para um único grupo de dados.");
        } catch (IllegalArgumentException expected) { }
        try {
            StatblockCombatFlowEngine.damageSpecs("2d6 + x + 1d8","fire + psychic","");
            fail("Trecho de dano malformado não pode ser ignorado.");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void damageParserConsumesEveryTermAndRejectsOverflowBeforeCommitting() {
        assertEquals(5, StatblockCombatFlowEngine.rollAverage("1d4 + 1d6",false));
        assertEquals(12, StatblockCombatFlowEngine.rollAverage("1d4 + 1d6",true));
        assertEquals(6, StatblockCombatFlowEngine.rollAverage("1d4 + 1 + 1d6",false));
        assertEquals(0, StatblockCombatFlowEngine.rollAverage("5 - 9",false));
        for(String invalid:new String[]{"1d6 + desconhecido","1d6+2d","2147483647d2147483647","3d0","2d6 + 5 acid"}) {
            try{
                StatblockCombatFlowEngine.rollAverage(invalid,false);
                fail("A expressão inválida não pode provocar dano parcial: "+invalid);
            }catch(IllegalArgumentException expected){
                // Fonte inválida deve impedir mutação e não ser parcialmente interpretada.
            }
        }
    }

    @Test public void legacyAttackRollbackWhenTypedDamageParsingFailsAfterActionSpend()throws Exception {
        resetLegacyFlow();
        try {
            String id="g05-overflow-test-"+System.nanoTime();
            String book="G05 isolated test fixture";
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book",book).put("source_locator","g05-test-overflow-only");
            JSONArray attacks=new JSONArray().put(new JSONObject()
                .put("name","Overflow Strike").put("category","action")
                .put("to_hit",10).put("reach_ft",5)
                .put("damage","999999999999999999999d4")
                .put("damage_type","bludgeoning"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject()
                .put("id",id).put("source_book",book).put("creature_type","construct")
                .put("armor_class",15).put("hit_points",35).put("actions",attacks)
                .put("mechanical_basis",basis));
            assertTrue(StatblockEncounterStateEngine.spawn("overflow-attacker",id).optBoolean("spawned"));
            assertTrue(StatblockEncounterStateEngine.spawn("overflow-target",id).optBoolean("spawned"));
            StatblockTurnEngine.setInitiative("overflow-attacker",20);
            StatblockTurnEngine.setInitiative("overflow-target",10);
            StatblockTurnEngine.start();
            try {
                StatblockCombatFlowEngine.attack("overflow-attacker","overflow-target",
                    "Overflow Strike",15);
                fail("Uma expressão de dados irrecuperável deve abortar a transação");
            } catch (NumberFormatException expected) {
                // Falha de material-fonte injetada somente neste teste.
            }
            assertEquals(35,StatblockEncounterStateEngine.view("overflow-target").optInt("hp"));
            assertTrue(StatblockTurnEngine.state().getJSONArray("order").getJSONObject(0)
                .getJSONObject("economy").optBoolean("action",false));
        } finally {resetLegacyFlow();}
    }

}
