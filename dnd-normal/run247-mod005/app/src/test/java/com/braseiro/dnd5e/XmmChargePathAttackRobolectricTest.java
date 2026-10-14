package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** XMM source movement proof must reach committed attack damage and prone state. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmChargePathAttackRobolectricTest {
    private static void bind(AppDb db,long id,String block,String size)throws Exception {
        JSONObject resource=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(block!=null)resource.put("block_id",block);
        resource.put("size_category",size);
        ContentValues values=new ContentValues();values.put("resources_json",resource.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y){
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    @Test public void qualifyingBoarChargeAddsDamageAndPersistentProne()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_charge_hit_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("Charge Boar",20,13,13,11,"",false);
            long target=db.addCombatant("Charge Target",10,20,20,10,"",false);
            bind(db,actor,"Boar","medium");bind(db,target,null,"medium");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues clear=new ContentValues();clear.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",clear,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,1,2);position(db,target,6,2);
            JSONObject movement=db.previewTacticalGridMovement(actor,5,2,false,false);
            assertTrue(movement.toString(),movement.optBoolean("grid_valid"));
            assertEquals(20,movement.optInt("movement_cost_ft"));
            JSONObject moved=db.moveCombatantToGridCell(actor,5,2,false,false,
                movement.optString("grid_fingerprint"),"ui-grid-charge-boar-001");
            assertTrue(moved.toString(),moved.optBoolean("grid_movement_committed"));
            JSONObject preview=db.previewXmmChargeAttack(actor,target,"Boar","Gore");
            assertTrue(preview.toString(),preview.optBoolean("charge_preview_valid"));
            assertTrue(preview.toString(),preview.getJSONObject("charge").optBoolean("charge_qualified"));
            assertTrue(preview.toString(),preview.getJSONObject("charge").optBoolean("charge_prone_eligible"));
            JSONObject roll=db.previewCompiledMonsterAttackRollMode(actor,target,"Boar","Gore");
            assertTrue(roll.toString(),roll.optBoolean("monster_roll_preview_valid"));
            int second=roll.optBoolean("second_d20_required")?19:0;
            JSONObject attack=db.applyCompiledStatblockAttack(actor,target,"Boar","Gore",19,second,3,
                0,0,-1,-1,"ui-monster-charge-boar-001");
            assertTrue(attack.toString(),attack.optBoolean("statblock_attack_committed"));
            JSONObject resolved=attack.getJSONObject("resolved");
            assertTrue(resolved.toString(),resolved.optBoolean("charge_qualified"));
            assertEquals("1d6",resolved.optString("extra_damage_expression"));
            assertEquals("prone",resolved.optString("rider_on_hit_condition"));
            assertEquals(3,attack.optInt("damage_total"));
            assertTrue(db.getCombatant(target).optString("conditions").contains("prone")||
                ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
            assertFalse("Attack consumes the route marker",
                db.latestTacticalGridMovementProof(actor).optBoolean("grid_path_valid",false));
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    @Test public void ordinaryBoarAttackDoesNotApplyConditionalDamageOrProne()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_charge_base_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("Ordinary Boar",20,13,13,11,"",false);
            long target=db.addCombatant("Ordinary Target",10,20,20,10,"",false);
            bind(db,actor,"Boar","medium");bind(db,target,null,"medium");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues clear=new ContentValues();clear.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",clear,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,4,2);position(db,target,5,2);
            JSONObject roll=db.previewCompiledMonsterAttackRollMode(actor,target,"Boar","Gore");
            assertTrue(roll.toString(),roll.optBoolean("monster_roll_preview_valid"));
            int second=roll.optBoolean("second_d20_required")?19:0;
            JSONObject attack=db.applyCompiledStatblockAttack(actor,target,"Boar","Gore",19,second,2,
                0,0,-1,-1,"ui-monster-charge-base-001");
            assertTrue(attack.toString(),attack.optBoolean("statblock_attack_committed"));
            JSONObject resolved=attack.getJSONObject("resolved");
            assertFalse(resolved.toString(),resolved.optBoolean("charge_qualified"));
            assertEquals("",resolved.optString("extra_damage_expression"));
            assertEquals("",resolved.optString("rider_on_hit_condition"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
