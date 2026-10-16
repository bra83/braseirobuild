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

/** Release guard: undo must never erase later combat events. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class V1ManualRevertSafetyRobolectricTest {
    @Test public void undoOfOldManualActionMustNotEraseNewerDamage() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        Context ctx=RuntimeEnvironment.getApplication();
        String name="v1rv_"+Long.toHexString(System.nanoTime() & 0xFFFFFL)+".db";
        AppDb db=new AppDb(ctx,name);
        try {
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("Mind Flayer",20,80,80,15,"",false);
            long target=db.addCombatant("Rosa",10,40,40,16,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id","Mind Flayer").put("action_available",true);
            ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=?",
                new String[]{String.valueOf(actor)}));
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject applied=db.applyXmmManualArbitration(actor,target,"Mind Flayer",
                "Multiattack","action",10,"psychic","stunned","",true,
                "Confirmado pelo jogador","ui-monster-safe-rv-001");
            assertTrue(applied.toString(),applied.optBoolean("manual_arbitration_applied"));
            assertEquals(30,db.getCombatant(target).optInt("hp"));
            assertTrue(db.getCombatant(target).optString("conditions","").contains("stunned"));
            JSONObject newer=db.applyCombatantDamage(target,3,"slashing",false,false,
                "ui-monster-safe-rv-002");
            assertEquals("Later action should apply damage: "+newer,
                27,db.getCombatant(target).optInt("hp"));
            JSONObject undo=db.revertXmmManualArbitration(actor,
                applied.optString("receipt_key"),"ui-monster-safe-rv-003");
            assertFalse("Stale undo must be blocked: "+undo,
                undo.optBoolean("manual_arbitration_reverted",false));
            assertEquals("target_state_changed_since_arbitration",
                undo.optString("manual_arbitration_revert_blocked"));
            assertEquals("Later damage must survive stale undo",27,
                db.getCombatant(target).optInt("hp"));
            assertTrue("Original effect also remains until a proper reversal",
                db.getCombatant(target).optString("conditions","").contains("stunned"));
            assertFalse("Action remains spent",new JSONObject(db.getCombatant(actor)
                .optString("resources_json","{}")).optBoolean("action_available",true));
        } finally { db.close();ctx.deleteDatabase(name); }
    }
}
