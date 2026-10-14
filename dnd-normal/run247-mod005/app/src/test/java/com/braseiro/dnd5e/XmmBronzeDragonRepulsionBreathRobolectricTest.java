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

/** Bronze age variants push and knock targets Prone only on a failed save. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmBronzeDragonRepulsionBreathRobolectricTest {
    private static void runCase(String block,int dc,int expectedFeet,int destinationX)throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_bronze_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,10)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("Bronze Source "+dc,20,150,150,16,"",false);
            target=db.addCombatant("Bronze Target "+dc,10,100,100,12,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",block);ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,4,2);
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,block,"Repulsion Breath",
                1,0,destinationX,2,"ui-monster-save-xmm-bronze-"+dc);
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            assertEquals(destinationX,db.tacticalGridPosition(target).optInt("cell_x"));
            assertEquals(expectedFeet,result.getJSONObject("resolved").getJSONObject("forced_movement_transition")
                .optInt("feet"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
            assertEquals(100,db.getCombatant(target).optInt("hp"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void fourBronzeAgesPushAndKnockDownOnFailedSave()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        runCase("Adult Bronze Dragon",19,10,6);
        runCase("Ancient Bronze Dragon",23,10,6);
        runCase("Bronze Dragon Wyrmling",12,10,6);
        runCase("Young Bronze Dragon",15,10,6);
    }
}
