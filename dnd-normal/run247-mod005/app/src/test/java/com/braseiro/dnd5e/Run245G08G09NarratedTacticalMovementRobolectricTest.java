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

/** Focused G08/G09 proof for narrated destination -> route preflight -> receipt -> SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run245G08G09NarratedTacticalMovementRobolectricTest {
    private static final class Fixture implements AutoCloseable {
        final Context context=RuntimeEnvironment.getApplication();
        final String dbName="r245g8_"+Long.toHexString(System.nanoTime() & 0xFFFFFFL)+".db";
        final AppDb db=new AppDb(context,dbName);
        final long actor;
        final JSONObject scene;
        Fixture() throws Exception {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("Narrated G08 Actor",20,35,35,14,"",true);
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            ContentValues position=new ContentValues();
            position.put("x",2.5d/scene.optInt("width_cells"));
            position.put("y",2.5d/scene.optInt("height_cells"));
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",position,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(actor)}));
        }
        JSONObject moveTo(int x,int y) throws Exception {
            return new JSONObject().put("combatant_id",actor).put("name","Narrated G08 Actor")
                .put("x",(x+.5d)/scene.optInt("width_cells"))
                .put("y",(y+.5d)/scene.optInt("height_cells"));
        }
        public void close(){db.close();context.deleteDatabase(dbName);}
    }

    @Test public void narratedMovePreflightsThenCommitsPositionBudgetAndReplayReceipt() throws Exception {
        Fixture f=new Fixture();
        try {
            String sceneCommit="g08-g09-commit-000000001";
            JSONObject proposal=f.moveTo(4,2);
            JSONObject plan=NarratedTacticalMovementExecutor.preflight(f.db,proposal,sceneCommit,0);
            assertTrue(plan.toString(),plan.optBoolean("narrated_tactical_move_preflight"));
            assertEquals(20,plan.getJSONObject("route_preview").optInt("movement_cost_ft"));
            JSONObject result=NarratedTacticalMovementExecutor.executeChecked(f.db,proposal,sceneCommit,0);
            assertTrue(result.toString(),result.optBoolean("narrated_tactical_move_committed"));
            assertEquals(4,f.db.tacticalGridPosition(f.actor).optInt("cell_x"));
            assertEquals(2,f.db.tacticalGridPosition(f.actor).optInt("cell_y"));
            JSONObject after=new JSONObject(f.db.getCombatant(f.actor).optString("resources_json","{}"));
            assertEquals(10,after.optInt("movement_remaining_ft"));
            String receipt=result.optString("narrated_tactical_receipt");
            assertTrue(receipt,receipt.startsWith("combat-v1:"+f.actor+":"));
            assertTrue("position receipt persisted",f.db.hasEffectReceipt(receipt));
            try {
                NarratedTacticalMovementExecutor.executeChecked(f.db,proposal,sceneCommit,0);
                fail("same scene proposal must not replay");
            } catch(IllegalStateException expected) {
                assertEquals("position_replayed",expected.getMessage());
            }
            assertEquals(10,new JSONObject(f.db.getCombatant(f.actor).optString("resources_json","{}"))
                .optInt("movement_remaining_ft"));
        } finally { f.close(); }
    }

    @Test public void narratedMoveWithBlockedDestinationFailsClosed() throws Exception {
        Fixture f=new Fixture();
        try {
            JSONObject terrain=new JSONObject().put("x",3).put("y",2).put("blocks_movement",true);
            ContentValues obstacles=new ContentValues();obstacles.put("obstacles_json",new JSONArray().put(terrain).toString());
            assertEquals(1,f.db.getWritableDatabase().update("tactical_scenes",obstacles,
                "world_profile=? AND scene_key=?",new String[]{"dnd",f.scene.optString("scene_key")}));
            JSONObject blocked=NarratedTacticalMovementExecutor.preflight(f.db,f.moveTo(3,2),
                "g08-g09-blocked-00000001",0);
            assertEquals("destination_occupied_or_blocked",blocked.optString("narrated_tactical_move_blocked"));
            assertEquals(2,f.db.tacticalGridPosition(f.actor).optInt("cell_x"));
            assertEquals(30,new JSONObject(f.db.getCombatant(f.actor).optString("resources_json","{}"))
                .optInt("movement_remaining_ft"));
            JSONObject invalid=f.moveTo(4,2).put("x",1d);
            assertEquals("destination_out_of_bounds",NarratedTacticalMovementExecutor.preflight(f.db,invalid,
                "g08-g09-invalid-00000001",1).optString("narrated_tactical_move_blocked"));
        } finally { f.close(); }
    }

    @Test public void narratedMoveWithUnresolvedOpportunityAttackDoesNotSpendOrMove() throws Exception {
        Fixture f=new Fixture();
        try {
            long guard=f.db.addCombatant("Narrated G08 Guard",10,20,20,13,"",false);
            f.db.ensureTacticalTokensForCombat();
            JSONObject scene=f.scene;
            ContentValues guardPosition=new ContentValues();
            guardPosition.put("x",2.5d/scene.optInt("width_cells"));
            guardPosition.put("y",3.5d/scene.optInt("height_cells"));
            assertEquals(1,f.db.getWritableDatabase().update("tactical_tokens",guardPosition,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(guard)}));
            JSONObject weapon=new JSONObject().put("id","guard_claw").put("weapon_attack_kind","melee")
                .put("weapon_reach_ft",5).put("weapon_attack_bonus",5)
                .put("weapon_damage_formula","1d6+2").put("weapon_damage_type","cortante");
            JSONObject resources=new JSONObject().put("speed_ft",30).put("opportunity_weapon",weapon);
            ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
            assertEquals(1,f.db.getWritableDatabase().update("combatants",values,"id=?",new String[]{String.valueOf(guard)}));
            JSONObject proposal=f.moveTo(4,2);
            JSONObject blocked=NarratedTacticalMovementExecutor.preflight(f.db,proposal,
                "g08-g09-opportunity-0001",0);
            assertEquals("opportunity_resolution_required",blocked.optString("narrated_tactical_move_blocked"));
            assertTrue(blocked.optJSONObject("route_preview").optJSONArray("opportunity_attacks").length()>0);
            assertEquals(2,f.db.tacticalGridPosition(f.actor).optInt("cell_x"));
            assertEquals(30,new JSONObject(f.db.getCombatant(f.actor).optString("resources_json","{}"))
                .optInt("movement_remaining_ft"));
        } finally { f.close(); }
    }
}
