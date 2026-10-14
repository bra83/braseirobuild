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

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmConditionalAttackRollRobolectricTest {
    @Test public void fourSourceAttacksCheckTargetCurrentAndMaximumHp() {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        for(String[] row:new String[][]{
            {"Giant Shark","Bite"},{"Hunter Shark","Bite"},
            {"Piranha","Bite"},{"Swarm of Piranhas","Bites"}}){
            JSONObject action=findAction(row[0],row[1]);
            assertNotNull(row[0]+"/"+row[1],action);
            assertTrue(row[0]+"/"+row[1],action.optBoolean(
                "attack_advantage_if_target_missing_hit_points",false));
            XmmConditionalAttackRollRules.Result wounded=
                XmmConditionalAttackRollRules.advantage(action,9,10);
            assertTrue(wounded.legal());assertTrue(wounded.advantage);
            XmmConditionalAttackRollRules.Result full=
                XmmConditionalAttackRollRules.advantage(action,10,10);
            assertTrue(full.legal());assertFalse(full.advantage);
        }
        JSONObject action=findAction("Giant Shark","Bite");
        assertEquals("source_advantage_target_hit_points_unverified",
            XmmConditionalAttackRollRules.advantage(action,10,0).blocked);
        MonsterAttackRollModeRules.Result canceled=MonsterAttackRollModeRules.evaluate(
            "melee",5,false,"envenenado","",false,true);
        assertTrue(canceled.legal());
        assertEquals(MonsterAttackRollModeRules.NORMAL,canceled.mode);
        assertTrue(canceled.advantage);assertTrue(canceled.disadvantage);
        assertEquals("",canceled.blockDice(10,0));
    }

    @Test public void databaseAttackUsesWoundedTargetAdvantageBeforeDamage()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_wounded_adv_"+Long.toHexString(System.nanoTime())+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            long shark=db.addCombatant("Giant Shark",20,92,92,13,"",false);
            long target=db.addCombatant("Wounded Target",10,39,50,10,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(shark).optString("resources_json","{}"));
            resources.put("block_id","Giant Shark");
            ContentValues binding=new ContentValues();binding.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",binding,
                "id=? AND world_profile=?",new String[]{String.valueOf(shark),"dnd"}));
            db.setCombatRunning(true);assertEquals(shark,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();position(db,shark,2,2);position(db,target,3,2);

            JSONObject result=db.applyCompiledStatblockAttack(shark,target,"Giant Shark","Bite",
                7,19,9,0,0L,-1,-1,"ui-monster-shark-wounded-target-advantage");
            assertTrue(result.toString(),result.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=result.getJSONObject("resolved");
            assertEquals("Vantagem",resolved.optString("roll_mode"));
            assertTrue(resolved.optBoolean("attack_had_advantage",false));
            assertTrue(resolved.optBoolean("roll_advantage",false));
            assertEquals(30,db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    private static JSONObject findAction(String id,String name){
        JSONArray actions=CompiledStatblockEngine.runtimeProfile(id).optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject action=actions.optJSONObject(i);
            if(action!=null&&name.equals(action.optString("name")))return action;
        }
        return null;
    }
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
}
