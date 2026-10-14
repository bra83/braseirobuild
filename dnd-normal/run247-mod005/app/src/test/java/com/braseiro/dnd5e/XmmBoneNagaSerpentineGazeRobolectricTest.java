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

/** Serpentine Gaze's failed-save Charm expires at the source's next turn start. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmBoneNagaSerpentineGazeRobolectricTest {
    @Test public void failedSaveDamagesAndCharmEndsAtNagasNextTurnStart()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=null;org.json.JSONArray actions=CompiledStatblockEngine
            .runtimeProfile("Bone Naga").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Serpentine Gaze".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);assertEquals("charmed",action.optString("condition_on_fail"));
        assertEquals("source_turn_start",action.optString("condition_duration_tick"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_bone_naga_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,10)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Bone Naga",20,100,100,15,"",false);
            target=db.addCombatant("Serpentine Target",10,50,50,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id","Bone Naga");ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,"Bone Naga",
                "Serpentine Gaze",1,6,"ui-monster-save-xmm-bone-naga-gaze");
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            assertEquals(44,db.getCombatant(target).optInt("hp"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"charmed"));
            assertEquals(target,db.nextTurn().optLong("id"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"charmed"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"charmed"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
