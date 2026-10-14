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

/** Dread Scythe's source-timed no-healing effect is independent of conditions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmDeathCultistHealingSuppressionRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void dreadScytheBlocksHealingUntilEndOfTargetNextTurn()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=CompiledStatblockEngine.runtimeProfile("Death Cultist")
            .getJSONArray("actions").getJSONObject(1);
        assertEquals("Dread Scythe",action.optString("name"));
        assertEquals(1,action.optInt("healing_suppression_duration_rounds"));
        assertEquals("target_turn_end",action.optString("healing_suppression_tick"));
        assertEquals("unsupported_multiattack_timed_healing_suppression",
            MonsterActionEconomyGuard.attackRiderBlock(action,false));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_dread_heal_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            long cultist=db.addCombatant("Death Cultist",20,80,80,13,"",false);
            long target=db.addCombatant("Dread Scythe Target",10,30,100,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(cultist).optString("resources_json","{}"));
            resources.put("block_id","Death Cultist");
            ContentValues bind=new ContentValues();bind.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",bind,
                "id=? AND world_profile=?",new String[]{String.valueOf(cultist),"dnd"}));
            db.setCombatRunning(true);assertEquals(cultist,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();position(db,cultist,2,2);position(db,target,3,2);

            JSONArray specs=StatblockCombatFlowEngine.damageSpecs(action.optString("damage"),
                action.optString("damage_type"),"");JSONArray rolls=new JSONArray();
            for(int i=0;i<specs.length();i++){
                JSONObject spec=specs.getJSONObject(i);
                CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                    spec.optString("expression"),"",false);
                assertNotNull(range);
                rolls.put(new JSONObject().put("expression",spec.optString("expression"))
                    .put("damage_type",spec.optString("type")).put("damage_total",range.min));
            }
            JSONObject hit=db.applyCompiledStatblockAttack(cultist,target,"Death Cultist","Dread Scythe",
                18,0,rolls,0,0L,-1,-1,"ui-monster-dread-scythe-hit");
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=hit.optJSONObject("resolved");
            JSONObject transition=resolved==null?null:resolved.optJSONObject("healing_suppression_transition");
            assertNotNull(transition);assertTrue(transition.optBoolean("applied",false));
            assertEquals(30,db.getCombatant(target).optInt("hp")+hit.optInt("damage_total"));

            JSONObject blocked=db.healCombatant(target,10,"xmm-dread-heal-blocked");
            assertEquals("source_condition_prevents_hit_point_regain",
                blocked.optString("healing_blocked"));
            assertEquals(30-hit.optInt("damage_total"),db.getCombatant(target).optInt("hp"));
            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals("source_condition_prevents_hit_point_regain",
                db.healCombatant(target,10,"xmm-dread-heal-still-blocked").optString("healing_blocked"));
            assertEquals(cultist,db.nextTurn().optLong("id"));
            assertEquals(0,new JSONArray(db.getCombatant(target)
                .optString("condition_timers_json","[]")).length());
            JSONObject healed=db.healCombatant(target,10,"xmm-dread-heal-after-expiry");
            assertEquals(10,healed.optInt("applied_healing"));
            assertEquals(40-hit.optInt("damage_total"),db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
}
