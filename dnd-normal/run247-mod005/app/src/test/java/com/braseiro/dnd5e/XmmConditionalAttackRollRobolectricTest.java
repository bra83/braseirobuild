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

    @Test public void grappledTargetAdvantageNeedsTheAttackerOwnedPersistedLink(){
        JSONObject action=new JSONObject();
        try{action.put("attack_advantage_condition","target_grappled_by_attacker");}
        catch(Exception failure){throw new AssertionError(failure);}
        XmmConditionalAttackRollRules.Result owner=XmmConditionalAttackRollRules.advantage(action,20,20,true);
        assertTrue(owner.legal());assertTrue(owner.advantage);
        XmmConditionalAttackRollRules.Result other=XmmConditionalAttackRollRules.advantage(action,20,20,false);
        assertTrue(other.legal());assertFalse(other.advantage);
        assertEquals("source_advantage_grapple_link_unverified",
            XmmConditionalAttackRollRules.advantage(action,20,20).blocked);
    }

    @Test public void bugbearWarriorLightHammerUsesItsPersistedGrappleLink()throws Exception{
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_bugbear_grapple_adv_"+Long.toHexString(System.nanoTime())+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long bugbear=db.addCombatant("Bugbear Warrior",10,40,40,14,"",false);
            long target=db.addCombatant("Grapple Target",10,40,40,10,"",true);
            bind(db,bugbear,"Bugbear Warrior",null);bind(db,target,null,"medium");
            db.setCombatRunning(true);JSONObject opening=db.nextTurn();
            if(opening.optLong("id")!=bugbear)opening=db.nextTurn();
            assertEquals(bugbear,opening.optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,bugbear,2,2);position(db,target,3,2);
            JSONObject grab=db.applyCompiledStatblockAttack(bugbear,target,"Bugbear Warrior","Grab",
                15,0,8,0,"ui-monster-bugbear-grab-source-bound");
            assertTrue(grab.toString(),grab.optBoolean("statblock_attack_committed",false));
            JSONObject grappled=db.getCombatant(target);
            assertTrue(ConditionTokenGuard.has(grappled.optString("conditions",""),"grappled"));
            assertEquals(bugbear,new JSONObject(grappled.optString("resources_json","{}"))
                .getJSONObject("xmm_grapple_state").optLong("source_actor_id"));

            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(bugbear,db.nextTurn().optLong("id"));
            JSONObject preview=db.previewCompiledMonsterAttackRollMode(bugbear,target,
                "Bugbear Warrior","Light Hammer");
            assertTrue(preview.toString(),preview.optBoolean("monster_roll_preview_valid",false));
            assertEquals("Vantagem",preview.optString("roll_mode"));
            JSONObject hammer=db.applyCompiledStatblockAttack(bugbear,target,"Bugbear Warrior","Light Hammer",
                2,15,8,0,"ui-monster-bugbear-light-hammer-grapple-advantage");
            assertTrue(hammer.toString(),hammer.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=hammer.getJSONObject("resolved");
            assertEquals("Vantagem",resolved.optString("roll_mode"));
            assertTrue(resolved.optBoolean("attack_had_advantage",false));
            assertEquals(24,db.getCombatant(target).optInt("hp"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    @Test public void ankhegBiteAppliesItsTypedGrappleAndNextBiteGetsAdvantage()throws Exception{
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONObject bite=findAction("Ankheg","Bite");assertNotNull(bite);
        assertEquals("target_grappled_by_attacker",bite.optString("attack_advantage_condition"));
        assertFalse(bite.optBoolean("manual_rider_resolution_required",true));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_ankheg_grapple_adv_"+Long.toHexString(System.nanoTime())+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long ankheg=db.addCombatant("Ankheg",10,50,50,14,"",false);
            long target=db.addCombatant("Grapple Target",10,60,60,10,"",true);
            bind(db,ankheg,"Ankheg",null);bind(db,target,null,"medium");
            db.setCombatRunning(true);JSONObject opening=db.nextTurn();
            if(opening.optLong("id")!=ankheg)opening=db.nextTurn();
            assertEquals(ankheg,opening.optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,ankheg,2,2);position(db,target,3,2);
            JSONArray damage=new JSONArray()
                .put(new JSONObject().put("expression","2d6 + 3").put("damage_type","slashing").put("damage_total",8))
                .put(new JSONObject().put("expression","1d6").put("damage_type","acid").put("damage_total",3));
            JSONObject first=db.applyCompiledStatblockAttack(ankheg,target,"Ankheg","Bite",
                15,0,damage,0,0L,-1,-1,"ui-monster-ankheg-bite-source-grapple");
            assertTrue(first.toString(),first.optBoolean("statblock_attack_committed",false));
            JSONObject grappled=db.getCombatant(target);
            assertTrue(ConditionTokenGuard.has(grappled.optString("conditions",""),"grappled"));
            assertEquals(ankheg,new JSONObject(grappled.optString("resources_json","{}"))
                .getJSONObject("xmm_grapple_state").optLong("source_actor_id"));
            JSONObject escape=db.getXmmGrappleEscapeInfo(target);
            assertTrue(escape.toString(),escape.optBoolean("grapple_escape_available",false));
            assertEquals(13,escape.optInt("escape_dc"));

            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(ankheg,db.nextTurn().optLong("id"));
            JSONObject preview=db.previewCompiledMonsterAttackRollMode(ankheg,target,"Ankheg","Bite");
            assertTrue(preview.toString(),preview.optBoolean("monster_roll_preview_valid",false));
            assertEquals("Vantagem",preview.optString("roll_mode"));
            JSONObject second=db.applyCompiledStatblockAttack(ankheg,target,"Ankheg","Bite",
                2,15,damage,0,0L,-1,-1,"ui-monster-ankheg-bite-grapple-advantage");
            assertTrue(second.toString(),second.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=second.getJSONObject("resolved");
            assertEquals("Vantagem",resolved.optString("roll_mode"));
            assertTrue(resolved.optBoolean("attack_had_advantage",false));
            assertEquals(38,db.getCombatant(target).optInt("hp"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    private static void bind(AppDb db,long id,String block,String size)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(block!=null)resources.put("block_id",block);
        if(size!=null)resources.put("size_category",size);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
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
