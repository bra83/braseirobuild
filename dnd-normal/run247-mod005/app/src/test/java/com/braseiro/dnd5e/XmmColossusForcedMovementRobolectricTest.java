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

/** Source-bound hit push resolves with damage and a legal grid path atomically. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmColossusForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues values=new ContentValues();
        values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void bind(AppDb db,long id)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id","Colossus");
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }

    @Test public void slamPushesOnHitAndNeverMovesOnMiss()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject slam=null;org.json.JSONArray actions=CompiledStatblockEngine
            .runtimeProfile("Colossus").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Slam".equals(candidate.optString("name")))slam=candidate;
        }
        assertNotNull(slam);assertEquals("push:20ft",slam.optString("effect_on_hit"));
        assertFalse(slam.optBoolean("manual_unmodeled_source_effect_required",true));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_colossus_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,10)+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Colossus",20,200,200,18,"",false);
            target=db.addCombatant("XMM Slam Target",10,100,100,15,"",true);
            bind(db,actor);db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();position(db,actor,2,2);position(db,target,4,2);
            JSONObject hit=db.applyCompiledStatblockAttack(actor,target,"Colossus","Slam",
                18,0,14,0,0L,6,2,"ui-monster-colossus-slam-hit");
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed"));
            assertEquals(86,db.getCombatant(target).optInt("hp"));
            assertEquals(6,db.tacticalGridPosition(target).optInt("cell_x"));
            JSONObject resolved=hit.getJSONObject("resolved");
            assertEquals("push:20ft",resolved.optString("rider_on_hit_effect"));
            assertEquals(10,resolved.getJSONObject("on_hit_forced_movement_transition")
                .optInt("feet"));

            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            int hpBeforeMiss=db.getCombatant(target).optInt("hp");
            JSONObject miss=db.applyCompiledStatblockAttack(actor,target,"Colossus","Slam",
                1,0,0,0,0L,-1,-1,"ui-monster-colossus-slam-miss");
            assertTrue(miss.toString(),miss.optBoolean("statblock_attack_committed"));
            assertEquals(hpBeforeMiss,db.getCombatant(target).optInt("hp"));
            assertEquals(6,db.tacticalGridPosition(target).optInt("cell_x"));
            assertFalse(miss.getJSONObject("resolved").has("on_hit_forced_movement_transition"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
