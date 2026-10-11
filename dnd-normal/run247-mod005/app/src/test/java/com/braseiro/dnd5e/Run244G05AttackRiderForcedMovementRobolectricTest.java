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

/** G05: on-hit save riders with source-compiled forced movement must be atomic.
 * The test requires an Android/Robolectric runner; its presence is not a PASS. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Run244G05AttackRiderForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();
        values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void attackHitRiderPushProneMustPreflightAndCommitOrRollback()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String name="run244_g05_push_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,name);
        try {
            db.setActiveMapProfile("dnd");
            String id="run244-g05-test-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN244 test fixture")
                .put("source_locator","test-only");
            JSONArray actions=new JSONArray().put(new JSONObject()
                .put("name","Seismic Strike").put("category","action")
                .put("to_hit",10).put("damage","1d6")
                .put("damage_type","bludgeoning").put("reach_ft",5)
                .put("save_dc",13).put("save_ability","str")
                .put("condition_on_fail","prone")
                .put("effect_on_fail","push:5ft;prone"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN244 test fixture").put("creature_type","construct")
                .put("armor_class",15).put("hit_points",35).put("actions",actions)
                .put("mechanical_basis",basis));
            long actor=db.addCombatant("RUN244 Actor",20,35,35,15,"",false);
            long target=db.addCombatant("RUN244 Target",10,50,50,13,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",id);
            ContentValues record=new ContentValues();record.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",record,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);
            int initialHp=db.getCombatant(target).optInt("hp");

            // The old API cannot omit mandatory displacement silently.
            JSONObject omitted=db.applyCompiledStatblockAttack(actor,target,id,
                "Seismic Strike",10,0,3,5,0L,"ui-monster-run244-omitted");
            assertEquals("forced_movement_outside_board",omitted.optString("statblock_attack_blocked"));
            assertEquals(initialHp,db.getCombatant(target).optInt("hp"));
            assertEquals(3,db.tacticalGridPosition(target).optInt("cell_x"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            // An obstacle invalidates the entire combat commit including HP and action economy.
            ContentValues blocked=new ContentValues();blocked.put("obstacles_json",
                new JSONArray().put(new JSONObject().put("x",4).put("y",2)
                    .put("blocks_movement",true)).toString());
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",blocked,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            JSONObject refused=db.applyCompiledStatblockAttack(actor,target,id,
                "Seismic Strike",10,0,3,5,0L,4,2,"ui-monster-run244-obstacle");
            assertEquals("forced_movement_path_blocked",refused.optString("statblock_attack_blocked"));
            assertEquals(initialHp,db.getCombatant(target).optInt("hp"));
            assertEquals(3,db.tacticalGridPosition(target).optInt("cell_x"));

            blocked.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",blocked,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            String token="ui-monster-run244-valid";
            JSONObject applied=db.applyCompiledStatblockAttack(actor,target,id,
                "Seismic Strike",10,0,3,5,0L,4,2,token);
            assertTrue(applied.toString(),applied.optBoolean("statblock_attack_committed"));
            assertEquals(initialHp-3,db.getCombatant(target).optInt("hp"));
            assertEquals(4,db.tacticalGridPosition(target).optInt("cell_x"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
            assertEquals(MonsterForcedMovementRules.SCHEMA,
                applied.getJSONObject("resolved").getJSONObject("forced_movement_transition")
                    .optString("schema"));
            assertFalse(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",true));
            JSONObject replay=db.applyCompiledStatblockAttack(actor,target,id,
                "Seismic Strike",10,0,3,5,0L,4,2,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_attack_blocked"));
            assertEquals(initialHp-3,db.getCombatant(target).optInt("hp"));

            JSONObject backup=db.exportAll();db.importAll(backup);
            JSONObject restored=db.findCombatantByName("RUN244 Target");
            assertEquals(initialHp-3,restored.optInt("hp"));
            assertEquals(4,db.tacticalGridPosition(restored.optLong("id")).optInt("cell_x"));
            assertTrue(ConditionTokenGuard.has(restored.optString("conditions"),"prone"));
        }finally{db.close();context.deleteDatabase(name);}
    }
}
