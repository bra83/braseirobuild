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

/** Source-bound poisoned condition also prevents hit point regain until source turn. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmBeardedDevilHealingSuppressionRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void beardPoisonBlocksHealingUntilDevilsNextTurnAndThenExpires()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=CompiledStatblockEngine.runtimeProfile("Bearded Devil")
            .getJSONArray("actions").getJSONObject(1);
        assertEquals("Beard",action.optString("name"));
        assertEquals("poisoned",action.optString("condition_on_hit"));
        assertEquals("source_turn_start",action.optString("condition_duration_tick"));
        assertEquals("poisoned",action.optString("prevents_hit_point_regain_while_condition"));
        assertFalse(action.optBoolean("manual_rider_resolution_required",true));
        assertFalse(action.optBoolean("manual_unmodeled_source_effect_required",true));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_beard_heal_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            long devil=db.addCombatant("Bearded Devil",20,58,58,13,"",false);
            long target=db.addCombatant("Poisoned Hero",10,30,50,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(devil).optString("resources_json","{}"));
            resources.put("block_id","Bearded Devil");
            ContentValues bind=new ContentValues();bind.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",bind,
                "id=? AND world_profile=?",new String[]{String.valueOf(devil),"dnd"}));
            db.setCombatRunning(true);assertEquals(devil,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();position(db,devil,2,2);position(db,target,3,2);

            JSONObject hit=db.applyCompiledStatblockAttack(devil,target,"Bearded Devil","Beard",
                18,0,4,0,"ui-monster-beard-poison-hit");
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed",false));
            JSONObject poisoned=db.getCombatant(target);
            int targetHpAfterHit=poisoned.optInt("hp");
            assertTrue(ConditionTokenGuard.has(poisoned.optString("conditions"),"poisoned"));
            org.json.JSONArray timers=new org.json.JSONArray(poisoned.optString("condition_timers_json","[]"));
            assertEquals(1,timers.length());assertTrue(timers.getJSONObject(0)
                .optBoolean("prevents_hit_point_regain",false));

            JSONObject blocked=db.healCombatant(target,10,"xmm-beard-heal-blocked");
            assertEquals("source_condition_prevents_hit_point_regain",
                blocked.optString("healing_blocked"));
            assertEquals(targetHpAfterHit,db.getCombatant(target).optInt("hp"));
            JSONObject batch=db.applyCombatEffectsBatch(new org.json.JSONArray().put(
                new JSONObject().put("target_id",target).put("kind","healing").put("amount",10)),
                "xmm-beard-heal-batch-blocked");
            assertEquals("healing_blocked_source_condition_prevents_hit_point_regain_0",
                batch.optString("effects_batch_blocked"));
            assertEquals(targetHpAfterHit,db.getCombatant(target).optInt("hp"));

            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(devil,db.nextTurn().optLong("id"));
            JSONObject expired=db.getCombatant(target);
            assertFalse(ConditionTokenGuard.has(expired.optString("conditions"),"poisoned"));
            assertEquals(0,new org.json.JSONArray(expired.optString("condition_timers_json","[]")).length());
            JSONObject healed=db.healCombatant(target,10,"xmm-beard-heal-after-expiry");
            assertEquals(10,healed.optInt("applied_healing"));
            assertEquals(Math.min(50,targetHpAfterHit+10),db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
}
