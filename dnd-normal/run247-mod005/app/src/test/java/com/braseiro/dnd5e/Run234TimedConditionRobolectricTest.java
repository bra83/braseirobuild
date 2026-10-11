package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Android SQLite integration contract. NOT executed in this source-only run. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class Run234TimedConditionRobolectricTest {
    private static String fixture(String suffix, int duration, boolean anchored) throws Exception {
        String id="run234-timer-"+suffix+"-"+System.nanoTime();
        JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
            .put("content_kind","statblock").put("content_name",id)
            .put("source_book","RUN234 test-only").put("source_locator","robolectric-only");
        JSONObject action=new JSONObject().put("name","Poison Pulse").put("save_dc",13)
            .put("save_ability","CON").put("condition_on_fail","poisoned")
            .put("effect_on_fail","").put("damage","").put("damage_on_save","")
            .put("condition_duration_rounds",duration);
        if(anchored)action.put("condition_duration_tick","target_turn_start");
        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
            .put("source_book","RUN234 test-only").put("creature_type","beast")
            .put("armor_class",14).put("hit_points",35)
            .put("actions",new JSONArray().put(action)).put("mechanical_basis",basis));
        return id;
    }
    private static void bindActor(AppDb db,long actor,String id) throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id",id);
        ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
            new String[]{String.valueOf(actor),"dnd"}));
    }
    @Test public void durationPersistsAcrossRestartAndExpiresOnTargetTurn() throws Exception {
        String dbName="run234_duration_"+System.nanoTime()+".db";
        Context context=RuntimeEnvironment.getApplication();AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");String block=fixture("valid",2,true);
            long actor=db.addCombatant("Venom beast",20,35,35,14,"",false);
            long target=db.addCombatant("Target",10,30,30,12,"",false);
            assertTrue(actor>0&&target>0);bindActor(db,actor,block);
            db.setCombatRunning(true);
            ContentValues active=new ContentValues();active.put("active_turn",1);
            assertEquals(1,db.getWritableDatabase().update("combatants",active,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            JSONObject committed=db.applyCompiledStatblockSaveAction(actor,target,block,"Poison Pulse",4,0,
                "ui-monster-save-run234-01");
            assertTrue(committed.toString(),committed.optBoolean("statblock_save_committed"));
            JSONObject proof=committed.getJSONObject("condition_transition");
            assertEquals("monster_condition_rider_v2",proof.getString("schema"));
            assertEquals(2,proof.getInt("duration_rounds"));
            assertEquals(1,proof.getJSONArray("condition_timers_after").length());
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(2,new JSONArray(db.getCombatant(target).getString("condition_timers_json"))
                .getJSONObject(0).getInt("rounds"));
            assertEquals("already_resolved_or_replayed",db.applyCompiledStatblockSaveAction(
                actor,target,block,"Poison Pulse",4,0,"ui-monster-save-run234-01")
                .optString("statblock_save_blocked"));
            JSONObject first=db.nextTurn();assertEquals(actor,first.optLong("id"));
            JSONObject second=db.nextTurn();assertEquals(target,second.optLong("id"));
            assertEquals(1,new JSONArray(db.getCombatant(target).getString("condition_timers_json"))
                .getJSONObject(0).getInt("rounds"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject expired=db.nextTurn();assertEquals(target,expired.optLong("id"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(0,new JSONArray(db.getCombatant(target).getString("condition_timers_json")).length());
            JSONObject transition=expired.getJSONObject("condition_timer_transition");
            assertEquals(1,transition.getJSONArray("expired").length());
            assertEquals("envenenado",transition.getJSONArray("expired").getString(0));
            String receipt=expired.getString("condition_timer_receipt");
            assertTrue(receipt.startsWith("monster-condition-turn-v1:enc-"));
            assertTrue(db.hasEffectReceipt(receipt));
            assertEquals(transition.getString("encounter_id"),db.getCombatState().getString("encounter_id"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
    @Test public void malformedTimingRollsBackSpentActionAndCondition() throws Exception {
        String dbName="run234_rollback_"+System.nanoTime()+".db";
        Context context=RuntimeEnvironment.getApplication();AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");String block=fixture("missing-anchor",2,false);
            long actor=db.addCombatant("Unanchored beast",20,35,35,14,"",false);
            long target=db.addCombatant("Target",10,30,30,12,"",false);
            bindActor(db,actor,block);db.setCombatRunning(true);
            ContentValues active=new ContentValues();active.put("active_turn",1);
            db.getWritableDatabase().update("combatants",active,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"});
            String before=db.getCombatant(actor).optString("resources_json");
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,block,"Poison Pulse",4,0,
                "ui-monster-save-run234-bad-anchor");
            assertEquals("transaction_rolled_back",result.optString("operation_error"));
            assertEquals(before,db.getCombatant(actor).optString("resources_json"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(0,new JSONArray(db.getCombatant(target).getString("condition_timers_json")).length());
        } finally {db.close();context.deleteDatabase(dbName);}
    }
}
