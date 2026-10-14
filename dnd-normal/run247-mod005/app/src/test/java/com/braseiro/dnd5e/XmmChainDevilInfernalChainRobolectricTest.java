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

/** XMM source-bound failed-save pull, restrained duration and size limit. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmChainDevilInfernalChainRobolectricTest {
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void configure(AppDb db,long id,String size,boolean source)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(size==null)resources.remove("size_category");else resources.put("size_category",size);
        if(source)resources.put("block_id","Chain Devil");
        ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
            new String[]{String.valueOf(id),"dnd"}));
    }
    private static JSONObject runCase(String targetSize,boolean expectMove,boolean blockSight)throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_chain_dev_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,9)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Chain Devil",20,40,40,12,"",false);
            target=db.addCombatant("Infernal Chain Target",10,100,100,12,"",true);
            configure(db,actor,"Medium",true);configure(db,target,targetSize,false);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,5,2);
            assertEquals(2,db.tacticalGridPosition(actor).optInt("cell_x"));
            assertEquals(5,db.tacticalGridPosition(target).optInt("cell_x"));
            if(blockSight){
                JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
                JSONArray obstacles=new JSONArray(scene.optString("obstacles_json","[]"));
                for(int y=0;y<scene.optInt("height_cells");y++)obstacles.put(
                    new JSONObject().put("x",3).put("y",y).put("type","wall")
                        .put("blocks_movement",true).put("blocks_sight",true));
                ContentValues wall=new ContentValues();wall.put("obstacles_json",obstacles.toString());
                assertEquals(1,db.getWritableDatabase().update("tactical_scenes",wall,
                    "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            }
            int targetHp=db.getCombatant(target).optInt("hp");
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,"Chain Devil",
                "Conjure Infernal Chain",1,6,expectMove?4:-1,expectMove?2:-1,
                "ui-monster-save-xmm-chain-devil-"+(targetSize==null?"unknown":targetSize.toLowerCase()));
            if(blockSight){
                assertEquals("line_of_sight_blocked",result.optString("statblock_save_blocked"));
                assertEquals(targetHp,db.getCombatant(target).optInt("hp"));
                assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"restrained"));
                return result;
            }
            if(targetSize==null){
                assertEquals("target_size_not_verified",result.optString("statblock_save_blocked"));
                assertEquals(targetHp,db.getCombatant(target).optInt("hp"));
                assertEquals(5,db.tacticalGridPosition(target).optInt("cell_x"));
                assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"restrained"));
                return result;
            }
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            JSONObject liveTarget=db.getCombatant(target);
            assertEquals(targetHp-6,liveTarget.optInt("hp"));
            assertTrue(ConditionTokenGuard.has(liveTarget.optString("conditions"),"restrained"));
            if(expectMove){
                assertEquals(4,db.tacticalGridPosition(target).optInt("cell_x"));
                assertEquals(5,result.getJSONObject("resolved").getJSONObject("forced_movement_transition").optInt("feet"));
            }else{
                assertEquals(5,db.tacticalGridPosition(target).optInt("cell_x"));
                assertEquals("target_size_exceeds_movement_limit",
                    result.getJSONObject("resolved").optString("forced_movement_skipped"));
                assertFalse(result.getJSONObject("resolved").has("forced_movement_transition"));
            }
            JSONArray timers=new JSONArray(liveTarget.optString("condition_timers_json","[]"));
            assertTrue(timers.toString(),timers.length()>0);
            JSONObject restrained=null;
            for(int i=0;i<timers.length();i++)if("impedido".equalsIgnoreCase(
                timers.optJSONObject(i).optString("name","")))restrained=timers.optJSONObject(i);
            assertNotNull(timers.toString(),restrained);
            assertEquals(1,restrained.optInt("rounds"));
            assertEquals("source_turn_end",restrained.optString("trigger"));
            return result;
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void failedSaveAppliesSourceRiderAndLargeOrSmallerPullLimit()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=null;JSONArray actions=CompiledStatblockEngine.runtimeProfile("Chain Devil").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Conjure Infernal Chain".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);assertEquals("pull:30ft;restrained",action.optString("effect_on_fail"));
        assertEquals("Large",action.optString("effect_on_fail_movement_max_target_size"));
        runCase("Large",true,false);runCase("Huge",false,false);runCase(null,false,false);
        runCase("Large",false,true);
    }
}
