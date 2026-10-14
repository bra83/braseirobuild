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

/** Source-bound Telekinetic Thrust commits failed-save damage and push atomically. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmPoltergeistForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void failedStrengthSavePushesAndDamagesAtomically()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=null;org.json.JSONArray actions=CompiledStatblockEngine
            .runtimeProfile("Poltergeist").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Telekinetic Thrust".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);assertEquals("push:30ft",action.optString("effect_on_fail"));
        assertFalse(action.optBoolean("manual_unmodeled_source_effect_required",true));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_poltergeist_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,10)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Poltergeist",20,40,40,12,"",false);
            target=db.addCombatant("Telekinetic Target",10,50,50,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id","Poltergeist");ContentValues v=new ContentValues();
            v.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();position(db,actor,2,2);position(db,target,4,2);
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,"Poltergeist",
                "Telekinetic Thrust",1,4,6,2,"ui-monster-save-xmm-poltergeist-thrust");
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            assertEquals(46,db.getCombatant(target).optInt("hp"));
            assertEquals(6,db.tacticalGridPosition(target).optInt("cell_x"));
            assertEquals(10,result.getJSONObject("resolved").getJSONObject("forced_movement_transition")
                .optInt("feet"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
