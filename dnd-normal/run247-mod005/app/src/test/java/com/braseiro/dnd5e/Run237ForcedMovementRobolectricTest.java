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

/** Android AppDb integration specifications; NOT claimed executed until CI. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run237ForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void manualPullCommitsAndBlockedPathRollsBack()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run237_force_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            String id="run237-fixture-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN237 test fixture")
                .put("source_locator","run237-test-only");
            JSONArray actions=new JSONArray().put(new JSONObject().put("name","Magnetic Pull")
                .put("category","bonus_action")
                .put("save_ability","dex").put("save_dc",13).put("damage","1d6")
                .put("damage_on_save","").put("damage_type","force")
                .put("effect_on_fail","pull:10ft"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN237 test fixture").put("creature_type","construct")
                .put("armor_class",15).put("hit_points",35).put("actions",actions)
                .put("mechanical_basis",basis));
            long source=db.addCombatant("RUN237 Source",20,35,35,15,"",false);
            long target=db.addCombatant("RUN237 Target",10,30,30,13,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(source).optString("resources_json","{}"));
            resources.put("block_id",id);
            ContentValues actor=new ContentValues();actor.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",actor,
                "id=? AND world_profile=?",new String[]{String.valueOf(source),"dnd"}));
            db.setCombatRunning(true);
            ContentValues active=new ContentValues();active.put("active_turn",1);
            assertEquals(1,db.getWritableDatabase().update("combatants",active,
                "id=? AND world_profile=?",new String[]{String.valueOf(source),"dnd"}));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            position(db,source,2,2);position(db,target,5,2);
            ContentValues obstacle=new ContentValues();obstacle.put("obstacles_json",
                new JSONArray().put(new JSONObject().put("x",4).put("y",2)
                    .put("blocks_movement",true)).toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacle,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            int hp=db.getCombatant(target).optInt("hp");
            JSONObject blocked=db.applyCompiledStatblockSaveAction(source,target,id,
                "Magnetic Pull",5,3,4,2,"ui-monster-save-run237-blocked");
            assertEquals("forced_movement_path_blocked",blocked.optString("statblock_save_blocked"));
            assertEquals(hp,db.getCombatant(target).optInt("hp"));
            assertEquals(5,db.tacticalGridPosition(target).optInt("cell_x"));
            obstacle.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacle,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject result=db.applyCompiledStatblockSaveAction(source,target,id,
                "Magnetic Pull",5,3,4,2,"ui-monster-save-run237-valid");
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            assertEquals(4,db.tacticalGridPosition(target).optInt("cell_x"));
            assertEquals(hp-3,db.getCombatant(target).optInt("hp"));
            JSONObject afterResources=new JSONObject(db.getCombatant(source).optString("resources_json","{}"));
            assertFalse(afterResources.optBoolean("bonus_action_available",true));
            assertTrue(afterResources.optBoolean("action_available",true));
            assertEquals(MonsterForcedMovementRules.SCHEMA,
                result.getJSONObject("resolved").getJSONObject("forced_movement_transition")
                    .getString("schema"));
            JSONObject replay=db.applyCompiledStatblockSaveAction(source,target,id,
                "Magnetic Pull",5,3,4,2,"ui-monster-save-run237-valid");
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_save_blocked"));
            JSONObject backup=db.exportAll();db.importAll(backup);
            JSONObject restoredTarget=db.findCombatantByName("RUN237 Target");
            assertEquals(4,db.tacticalGridPosition(restoredTarget.optLong("id")).optInt("cell_x"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
