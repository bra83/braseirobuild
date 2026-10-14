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

/** Size-capped XMM save riders preserve save damage and fail closed on unknown size. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmSizedSaveForcedMovementRobolectricTest {
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void bind(AppDb db,long id,String block)throws Exception{
        JSONObject r=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));r.put("block_id",block);
        ContentValues v=new ContentValues();v.put("resources_json",r.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
            new String[]{String.valueOf(id),"dnd"}));
    }
    private static void runCase(String actorBlock,String targetBlock,int damage,int destinationX,
        boolean expectMovement,boolean unknownSize)throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_save_size_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,9)+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Source "+actorBlock,20,150,150,16,"",false);
            target=db.addCombatant("XMM Save Target "+actorBlock,10,100,100,12,"",unknownSize);
            bind(db,actor,actorBlock);if(!targetBlock.isEmpty())bind(db,target,targetBlock);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,4,2);
            int targetHp=db.getCombatant(target).optInt("hp");
            JSONObject result=db.applyCompiledStatblockSaveAction(actor,target,actorBlock,
                "Air Elemental".equals(actorBlock)?"Whirlwind":"Water Jet",1,damage,
                destinationX,destinationX>=0?2:-1,"ui-monster-save-xmm-size-"+actorBlock.replace(" ","-"));
            if(unknownSize){
                assertEquals("target_size_not_verified",result.optString("statblock_save_blocked"));
                assertEquals(targetHp,db.getCombatant(target).optInt("hp"));
                assertEquals(4,db.tacticalGridPosition(target).optInt("cell_x"));
                return;
            }
            assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
            assertEquals(targetHp-damage,db.getCombatant(target).optInt("hp"));
            JSONObject resolved=result.getJSONObject("resolved");
            if(expectMovement){
                assertEquals(destinationX,db.tacticalGridPosition(target).optInt("cell_x"));
                assertEquals(10,resolved.getJSONObject("forced_movement_transition").optInt("feet"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
            }else{
                assertEquals(4,db.tacticalGridPosition(target).optInt("cell_x"));
                assertEquals("target_size_exceeds_movement_limit",resolved.optString("forced_movement_skipped"));
                assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
                assertFalse(resolved.has("forced_movement_transition"));
            }
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void airElementalAndMaridRespectSourceSizeCaps()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        runCase("Air Elemental","Satyr",6,6,true,false);
        runCase("Air Elemental","Colossus",6,-1,false,false);
        runCase("Air Elemental","",6,-1,false,true);
        runCase("Marid","Merrow",9,6,true,false);
        runCase("Marid","Colossus",9,-1,false,false);
        runCase("Marid","",9,-1,false,true);
    }
}
