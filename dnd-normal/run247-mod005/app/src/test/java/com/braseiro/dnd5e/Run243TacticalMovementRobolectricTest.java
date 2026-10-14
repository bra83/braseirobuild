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

/** G08: real Android SQLite preview -> commit -> backup21 restore specification.
 * MUST be run by Android/Robolectric CI before it is reported as PASS. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run243TacticalMovementRobolectricTest {
    @Test public void explicitTerrainAndRouteReceiptSurviveBackup21()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="g08_weighted_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("G08 Terrain Actor",20,35,35,14,"",false);
            db.setCombatRunning(true);
            JSONObject turn=db.nextTurn();
            assertEquals("active turn: "+turn.toString(),actor,turn.optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            ContentValues pos=new ContentValues();pos.put("x",2.5/12d);pos.put("y",2.5/8d);
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",pos,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(actor)}));
            JSONArray terrain=new JSONArray()
                .put(new JSONObject().put("x",3).put("y",2).put("difficult_terrain",true))
                .put(new JSONObject().put("x",3).put("y",1).put("blocks_movement",true))
                .put(new JSONObject().put("x",3).put("y",3).put("blocks_movement",true));
            ContentValues obstacles=new ContentValues();obstacles.put("obstacles_json",terrain.toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject preview=db.previewTacticalGridMovement(actor,4,2,false,false);
            assertTrue(preview.toString(),preview.optBoolean("grid_valid"));
            // Same geometry in another encounter is not the same approved action.
            String encounter=db.getCombatState().optString("encounter_id");
            ContentValues identity=new ContentValues();identity.put("encounter_id","enc-run243-rekey");
            assertEquals(1,db.getWritableDatabase().update("combat_state_profiles",identity,
                "world_profile=?",new String[]{"dnd"}));
            JSONObject newEncounter=db.previewTacticalGridMovement(actor,4,2,false,false);
            assertNotEquals(preview.optString("grid_fingerprint"),
                newEncounter.optString("grid_fingerprint"));
            identity.put("encounter_id",encounter);
            assertEquals(1,db.getWritableDatabase().update("combat_state_profiles",identity,
                "world_profile=?",new String[]{"dnd"}));
            assertEquals(15,preview.optInt("movement_cost_ft"));
            assertEquals(1,preview.optInt("difficult_steps"));
            assertEquals(3,preview.getJSONArray("route_cells").length());
            // An edited scene invalidates an old preview before spending movement.
            obstacles.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject stale=db.moveCombatantToGridCell(actor,4,2,false,false,
                preview.optString("grid_fingerprint"),"ui-grid-run243-stale");
            assertEquals("route_changed_review_again",stale.optString("grid_blocked"));
            assertEquals(2,db.tacticalGridPosition(actor).optInt("cell_x"));
            obstacles.put("obstacles_json",terrain.toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject moved=db.moveCombatantToGridCell(actor,4,2,false,false,
                preview.optString("grid_fingerprint"),"ui-grid-run243-move");
            assertTrue(moved.toString(),moved.optBoolean("grid_movement_committed"));
            JSONObject committedPath=moved.optJSONObject("committed_path");
            assertTrue(moved.toString(),TacticalGridMovementProofRules.valid(committedPath,actor));
            assertEquals(preview.getJSONArray("route_cells").toString(),
                committedPath.getJSONArray("route_cells").toString());
            assertEquals(4,db.tacticalGridPosition(actor).optInt("cell_x"));
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            assertEquals(15,resources.optInt("movement_remaining_ft"));
            JSONObject backup=db.exportAll();
            String oldPathKey=CombatPersistentReceiptKey.forCombatant(actor,
                "ui-grid-run243-move-movement-path");
            db.addCombatant("G08 Intervening Actor",5,20,20,10,"",false);
            db.importAll(backup);
            JSONObject restored=db.findCombatantByName("G08 Terrain Actor");
            assertTrue(restored.toString(),restored.optLong("id")>0);
            assertNotEquals(actor,restored.optLong("id"));
            assertEquals(4,db.tacticalGridPosition(restored.optLong("id")).optInt("cell_x"));
            assertEquals(15,new JSONObject(restored.optString("resources_json","{}"))
                .optInt("movement_remaining_ft"));
            JSONObject pathRow=db.queryOne("SELECT source FROM effect_receipts WHERE world_profile=? AND effect_key=?",
                new String[]{"dnd",CombatPersistentReceiptKey.forCombatant(restored.optLong("id"),
                    "ui-grid-run243-move-movement-path")});
            assertFalse(pathRow.toString(),pathRow.optString("source","").isEmpty());
            JSONObject restoredPath=new JSONObject(pathRow.optString("source"));
            assertEquals(restored.optLong("id"),restoredPath.optLong("actor_id"));
            assertTrue(TacticalGridMovementProofRules.valid(restoredPath,restored.optLong("id")));
            assertTrue(db.latestTacticalGridMovementProof(restored.optLong("id")).toString(),
                db.latestTacticalGridMovementProof(restored.optLong("id")).optBoolean("grid_path_valid"));
            assertEquals(0,db.queryOne("SELECT effect_key FROM effect_receipts WHERE world_profile=? AND effect_key=?",
                new String[]{"dnd",oldPathKey}).length());
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
