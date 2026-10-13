package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Source-pinned XMM save-for-half damage reaches and persists in combat. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmSaveDamageEffectRobolectricTest {
    private static void bind(AppDb db,long id,String block)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id",block);ContentValues values=new ContentValues();
        values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }

    @Test public void saveSuccessRejectsFullDamageAndCommitsSourceHalfDamage()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONObject rule=Xmm2025SaveDamageEngine.rule("Glabrezu","action","Pummel");
        assertTrue(rule.toString(),rule.optBoolean("resolved"));
        assertEquals(17,rule.optInt("dc"));assertEquals(15,rule.optInt("average"));
        JSONObject result=Xmm2025SaveDamageEngine.resolve(new JSONObject()
            .put("monster","Glabrezu").put("category","action").put("action","Pummel")
            .put("target_confirmed",true).put("recharge_ready_confirmed",true)
            .put("save_total",17).put("damage_mode","average"));
        assertTrue(result.toString(),result.optBoolean("resolved"));
        assertTrue(result.optBoolean("save_success"));
        assertEquals(7,result.optInt("damage_before_defenses"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_save_half_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Glabrezu",20,100,100,17,"",false);
            target=db.addCombatant("XMM Save Target",10,40,40,14,"",true);
            bind(db,actor,"Glabrezu");db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            int initialHp=db.getCombatant(target).optInt("hp");

            JSONObject forgedFull=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,15,"ui-monster-save-xmm-full-damage");
            assertEquals("damage_total_outside_compiled_save_dice",
                forgedFull.optString("statblock_save_blocked"));
            assertEquals(initialHp,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            JSONObject committed=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,result.optInt("damage_before_defenses"),
                "ui-monster-save-xmm-half-damage");
            assertTrue(committed.toString(),committed.optBoolean("statblock_save_committed"));
            assertTrue(committed.getJSONObject("resolved").optBoolean("save_success"));
            assertEquals(7,committed.getInt("damage_total"));
            assertEquals(initialHp-7,db.getCombatant(target).optInt("hp"));
            JSONObject replay=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,7,"ui-monster-save-xmm-half-damage");
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_save_blocked"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            JSONObject restored=db.findCombatantByName("XMM Save Target");
            assertEquals(initialHp-7,restored.optInt("hp"));
            assertEquals("",Backup21IntegrityEnvelope.check(backup));db.importAll(backup);
            restored=db.findCombatantByName("XMM Save Target");
            assertEquals(initialHp-7,restored.optInt("hp"));
        } finally {
            db.close();context.deleteDatabase(dbName);
        }
    }
}
