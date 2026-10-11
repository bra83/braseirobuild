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

/** SQLite/Android integration specification. Requires Robolectric SDK 35. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class Run235RecoveryRobolectricTest {
    private String register(boolean valid,String effect)throws Exception{
        String id="run235-recovery-"+System.nanoTime();
        JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
            .put("content_kind","statblock").put("content_name",id)
            .put("source_book","RUN235 test fixture").put("source_locator","run235-jvm-test");
        JSONObject action=new JSONObject().put("name","Venom Cloud")
            .put("save_dc",13).put("save_ability","con").put("damage","")
            .put("damage_on_save","").put("condition_on_fail","poisoned")
            .put("effect_on_fail",effect).put("condition_recovery_save_dc",13)
            .put("condition_recovery_save_ability","con")
            .put("condition_recovery_trigger",valid?"target_turn_end":"source_turn_end");
        CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
            .put("source_book","RUN235 test fixture").put("creature_type","beast")
            .put("armor_class",13).put("hit_points",30)
            .put("actions",new JSONArray().put(action)).put("mechanical_basis",basis));
        return id;
    }
    private void bind(AppDb db,long actor,String id)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id",id);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,"id=? AND world_profile=?",
            new String[]{String.valueOf(actor),"dnd"}));
    }
    @Test public void recoveryRequiresRealRollPersistsFailureAndExpiresAfterSuccess()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run235_success_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");String block=register(true,"poisoned");
            long actor=db.addCombatant("Poisoner",20,30,30,13,"",false);
            long target=db.addCombatant("Target",10,30,30,13,"",false);
            bind(db,actor,block);db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject applied=db.applyCompiledStatblockSaveAction(actor,target,block,"Venom Cloud",4,0,
                "ui-monster-save-run235-apply");
            assertTrue(applied.toString(),applied.optBoolean("statblock_save_committed"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(1,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject blocked=db.nextTurn();
            assertEquals("condition_recovery_save_pending",blocked.optString("turn_advance_blocked"));
            JSONObject failed=db.resolveCompiledMonsterConditionRecovery(target,"poisoned",12,
                "ui-monster-recovery-run235-fail");
            assertTrue(failed.toString(),failed.optBoolean("recovery_committed"));
            assertFalse(failed.getJSONObject("recovery_result").getBoolean("success"));
            assertEquals("recovery_already_attempted_this_turn",db.resolveCompiledMonsterConditionRecovery(
                target,"poisoned",20,"ui-monster-recovery-run235-reroll").optString("recovery_blocked"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals("condition_recovery_save_pending",db.nextTurn().optString("turn_advance_blocked"));
            JSONObject success=db.resolveCompiledMonsterConditionRecovery(target,"poisoned",13,
                "ui-monster-recovery-run235-pass");
            assertTrue(success.toString(),success.optBoolean("recovery_committed"));
            assertTrue(success.getJSONObject("recovery_result").getBoolean("success"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
            assertEquals(actor,db.nextTurn().optLong("id"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void invalidRecoverySourceDoesNotSpendAction()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run235_rollback_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");String block=register(false,"poisoned");
            long actor=db.addCombatant("Poisoner",20,30,30,13,"",false);
            long target=db.addCombatant("Target",10,30,30,13,"",false);
            bind(db,actor,block);db.setCombatRunning(true);db.nextTurn();
            String before=db.getCombatant(actor).optString("resources_json","");
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,block,"Venom Cloud",4,0,
                "ui-monster-save-run235-bad");
            assertEquals("transaction_rolled_back",result.optString("operation_error"));
            assertEquals(before,db.getCombatant(actor).optString("resources_json",""));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"poisoned"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
