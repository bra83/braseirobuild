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

/** G05 RUN245: last attack in authorized multiattack can have forced rider.
 * Integration evidence MUST come from a real Gradle Robolectric run. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Run245G05MultiattackFinalForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y){
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static JSONObject step(String action,long target,int natural,int damage,int save,int dx,int dy)throws Exception {
        return new JSONObject().put("action",action).put("target_id",target)
            .put("natural",natural).put("damage_total",damage)
            .put("rider_save_roll",save).put("long_range_second_d20",0)
            .put("additional_hostile_id",0L)
            .put("forced_destination_x",dx).put("forced_destination_y",dy);
    }
    @Test public void lastComponentPushIsAtomicAndBlocksAnObstructedDestination()throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        String name="r5_"+java.util.UUID.randomUUID().toString().substring(0,8)+".db";
        AppDb db=new AppDb(context,name);
        try{
            db.setActiveMapProfile("dnd");
            String id="run245-multi-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN245 test-only fixture")
                .put("source_locator","test-only");
            JSONArray actions=new JSONArray()
                .put(new JSONObject().put("name","Hammer").put("category","action")
                    .put("to_hit",10).put("damage","1d6").put("damage_type","bludgeoning")
                    .put("reach_ft",5))
                .put(new JSONObject().put("name","Gust").put("category","action")
                    .put("to_hit",10).put("damage","1d6").put("damage_type","force")
                    .put("reach_ft",5).put("save_dc",13).put("save_ability","str")
                    .put("effect_on_fail","push:5ft;prone").put("condition_on_fail","prone"));
            JSONObject multi=new JSONObject().put("count",2)
                .put("sequence",new JSONArray().put("Hammer").put("Gust"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN245 test-only fixture").put("creature_type","construct")
                .put("armor_class",12).put("hit_points",35)
                .put("actions",actions).put("multiattack",multi)
                .put("mechanical_basis",basis));
            long actor=db.addCombatant("RUN245 Multi Actor",20,35,35,15,"",false);
            long target1=db.addCombatant("RUN245 Multi Target 1",10,35,35,12,"",true);
            long target2=db.addCombatant("RUN245 Multi Target 2",9,35,35,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",id);
            ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target1,2,3);position(db,target2,3,2);
            JSONArray steps=new JSONArray().put(step("Hammer",target1,10,2,0,-1,-1))
                .put(step("Gust",target2,10,3,5,4,2));
            ContentValues obstacle=new ContentValues();obstacle.put("obstacles_json",
                new JSONArray().put(new JSONObject().put("x",4).put("y",2)
                    .put("blocks_movement",true)).toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacle,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject denied=db.applyCompiledStatblockMultiattack(actor,id,steps,"ui-monster-multi-run245-blocked");
            assertEquals(denied.toString(),"forced_movement_path_blocked",denied.optString("statblock_multiattack_blocked"));
            assertEquals(35,db.getCombatant(target1).optInt("hp"));
            assertEquals(35,db.getCombatant(target2).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            obstacle.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacle,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            String token="ui-monster-multi-run245-commit";
            JSONObject good=db.applyCompiledStatblockMultiattack(actor,id,steps,token);
            assertTrue(good.toString(),good.optBoolean("statblock_multiattack_committed"));
            assertTrue(good.optBoolean("action_spent_once"));
            assertEquals(33,db.getCombatant(target1).optInt("hp"));
            assertEquals(32,db.getCombatant(target2).optInt("hp"));
            assertEquals(4,db.tacticalGridPosition(target2).optInt("cell_x"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target2).optString("conditions",""),"prone"));
            assertEquals(MonsterForcedMovementRules.SCHEMA,good.getJSONArray("components")
                .getJSONObject(1).getJSONObject("resolved")
                .getJSONObject("forced_movement_transition").optString("schema"));
            assertFalse(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",true));
            JSONObject replay=db.applyCompiledStatblockMultiattack(actor,id,steps,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_multiattack_blocked"));
            JSONObject backup=db.exportAll();db.importAll(backup);
            JSONObject restored=db.findCombatantByName("RUN245 Multi Target 2");
            assertEquals(32,restored.optInt("hp"));
            assertEquals(4,db.tacticalGridPosition(restored.optLong("id")).optInt("cell_x"));
        }finally{db.close();context.deleteDatabase(name);}
    }
}
