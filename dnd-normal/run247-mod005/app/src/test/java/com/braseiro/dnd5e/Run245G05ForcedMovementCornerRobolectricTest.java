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

/** G05 RUN245 specification; execute through the Android/Robolectric runner. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Run245G05ForcedMovementCornerRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues c=new ContentValues();
        c.put("x",(x+0.5)/12d);c.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",c,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void diagonalCornerBlocksEntireAttackThenAcceptsClearPath()throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        String dbName="run245_g05_corner_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            String id="run245-corner-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN245 test-only fixture")
                .put("source_locator","test-only");
            JSONArray actions=new JSONArray().put(new JSONObject().put("name","Diagonal Gust")
                .put("category","action").put("to_hit",10).put("damage","1d6")
                .put("damage_type","force").put("reach_ft",5)
                .put("save_dc",13).put("save_ability","str")
                .put("effect_on_fail","push:5ft"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN245 test-only fixture")
                .put("creature_type","elemental").put("armor_class",12)
                .put("hit_points",35).put("actions",actions)
                .put("mechanical_basis",basis));
            long actor=db.addCombatant("RUN245 Push Actor",20,35,35,15,"",false);
            long target=db.addCombatant("RUN245 Push Target",10,40,40,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",id);
            ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",values,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            position(db,actor,1,1);position(db,target,2,2);
            int beforeHp=db.getCombatant(target).optInt("hp");
            ContentValues obstacles=new ContentValues();
            obstacles.put("obstacles_json",new JSONArray().put(new JSONObject()
                .put("x",3).put("y",2).put("blocks_movement",true)).toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject rejected=db.applyCompiledStatblockAttack(actor,target,id,"Diagonal Gust",
                10,0,3,5,0L,3,3,"ui-monster-run245-corner-rejected");
            assertEquals(rejected.toString(),"forced_movement_corner_blocked",
                rejected.optString("statblock_attack_blocked"));
            assertEquals(beforeHp,db.getCombatant(target).optInt("hp"));
            assertEquals(2,db.tacticalGridPosition(target).optInt("cell_x"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            obstacles.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            String token="ui-monster-run245-corner-success";
            JSONObject result=db.applyCompiledStatblockAttack(actor,target,id,"Diagonal Gust",
                10,0,3,5,0L,3,3,token);
            assertTrue(result.toString(),result.optBoolean("statblock_attack_committed"));
            assertEquals(beforeHp-3,db.getCombatant(target).optInt("hp"));
            assertEquals(3,db.tacticalGridPosition(target).optInt("cell_x"));
            assertEquals(3,db.tacticalGridPosition(target).optInt("cell_y"));
            assertFalse(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",true));
            JSONObject replay=db.applyCompiledStatblockAttack(actor,target,id,"Diagonal Gust",
                10,0,3,5,0L,3,3,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_attack_blocked"));
            JSONObject backup=db.exportAll();db.importAll(backup);
            JSONObject restored=db.findCombatantByName("RUN245 Push Target");
            assertEquals(beforeHp-3,restored.optInt("hp"));
            assertEquals(3,db.tacticalGridPosition(restored.optLong("id")).optInt("cell_x"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
