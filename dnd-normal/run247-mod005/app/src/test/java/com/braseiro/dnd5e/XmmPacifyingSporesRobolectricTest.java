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

/** XMM Pacifying Spores recovery, including its source-declared one-minute auto-success. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmPacifyingSporesRobolectricTest {
    private static void bind(AppDb db,long id,String block,String size)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(block!=null)resources.put("block_id",block);
        if(size!=null)resources.put("size_category",size);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    @Test public void pacifyingSporesRepeatsSaveAndAutomaticallyEndsAfterTenFailedTurnSaves()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject action=null;JSONArray actions=CompiledStatblockEngine.runtimeProfile(
            "Myconid Sovereign").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Pacifying Spores".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);assertFalse(action.optBoolean("manual_rider_resolution_required"));
        assertEquals(10,action.optInt("condition_recovery_auto_success_after_failed_saves"));
        assertEquals("target_turn_end",action.optString("condition_recovery_trigger"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_myconid_recovery_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Myconid Sovereign",20,45,45,13,"",false);
            target=db.addCombatant("Pacifying Spores Target",10,50,50,12,"",true);
            bind(db,actor,"Myconid Sovereign",null);bind(db,target,null,"Medium");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues openGrid=new ContentValues();openGrid.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",openGrid,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,4,2);
            JSONObject applied=db.applyCompiledStatblockSaveAction(actor,target,"Myconid Sovereign",
                "Pacifying Spores",1,0,"ui-monster-save-xmm-pacifying-spores");
            assertTrue(applied.toString(),applied.optBoolean("statblock_save_committed"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"stunned"));
            JSONArray timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
            assertEquals(1,timers.length());assertEquals(0,timers.getJSONObject(0).optInt("failed_attempts"));
            assertEquals(10,timers.getJSONObject(0).optInt("auto_success_after_failed_saves"));

            assertEquals(target,db.nextTurn().optLong("id"));
            for(int attempt=1;attempt<=9;attempt++){
                JSONObject failed=db.resolveCompiledMonsterConditionRecovery(target,"stunned",1,
                    "ui-monster-recovery-xmm-pacifying-"+attempt);
                assertTrue(failed.toString(),failed.optBoolean("recovery_committed"));
                assertFalse(failed.getJSONObject("recovery_result").optBoolean("success"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"stunned"));
                timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
                assertEquals(attempt,timers.getJSONObject(0).optInt("failed_attempts"));
                if(attempt<9){
                    assertEquals(actor,db.nextTurn().optLong("id"));
                    assertEquals(target,db.nextTurn().optLong("id"));
                }
            }
            assertEquals(actor,db.nextTurn().optLong("id"));
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject automatic=db.resolveCompiledMonsterConditionRecovery(target,"stunned",-1,
                "ui-monster-recovery-xmm-pacifying-auto");
            assertTrue(automatic.toString(),automatic.optBoolean("recovery_committed"));
            JSONObject result=automatic.getJSONObject("recovery_result");
            assertTrue(result.toString(),result.optBoolean("automatic_success"));
            assertTrue(result.optBoolean("success"));
            JSONObject recovered=db.getCombatant(target);
            assertFalse(ConditionTokenGuard.has(recovered.optString("conditions"),"stunned"));
            assertEquals(0,new JSONArray(recovered.optString("condition_timers_json","[]")).length());
            assertEquals("",Backup21IntegrityEnvelope.check(db.exportAll()));
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    @Test public void myconidAdultSporesAndQuasitScareUseSourceBoundRecovery()throws Exception {
        exerciseSingleTargetRecovery("Myconid Adult","Pacifying Spores (1/Day)",
            "stunned","con",11,10);
        exerciseSingleTargetRecovery("Quasit","Scare (1/Day)",
            "frightened","wis",10,20);
        exerciseSingleTargetRecovery("Carrion Crawler","Paralyzing Tentacles",
            "poisoned","con",12,10,"paralyzed");
    }

    @Test public void doppelgangerVisageAndPentadroneGasPersistTheirAreaConditions()throws Exception {
        exerciseSingleTargetRecovery("Doppelganger","Unsettling Visage",
            "frightened","wis",12,15);
        exerciseSingleTargetRecovery("Modron Pentadrone","Paralysis Gas",
            "paralyzed","con",11,30);
        exerciseSingleTargetRecovery("Graveyard Revenant","Haunting Glare",
            "paralyzed","wis",15,30);
    }

    private static void exerciseSingleTargetRecovery(String block,String action,String condition,
        String ability,int dc,int range)throws Exception {
        exerciseSingleTargetRecovery(block,action,condition,ability,dc,range,"");
    }
    private static void exerciseSingleTargetRecovery(String block,String action,String condition,
        String ability,int dc,int range,String linkedCondition)throws Exception {
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertTrue(installed.toString(),installed.optBoolean("complete"));
        JSONArray actions=CompiledStatblockEngine.runtimeProfile(block).optJSONArray("actions");
        JSONObject compiled=null;
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&action.equals(candidate.optString("name")))compiled=candidate;
        }
        assertNotNull(compiled);assertFalse(compiled.optBoolean("manual_rider_resolution_required"));
        assertEquals(dc,compiled.optInt("condition_recovery_save_dc"));
        assertEquals(range,compiled.optInt("target_range_ft"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_recovery_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM "+block,20,80,80,13,"",false);
            db.addCharacter("Recovery Target","Human","Fighter",1,50,50,12,
                10,10,10,10,10,10,true,"{\"size_category\":\"Medium\"}");
            target=db.addCombatant("Recovery Target",10,50,50,12,"",false);
            bind(db,actor,block,"Quasit".equals(block)?"Tiny":"Medium");
            bind(db,target,null,"Medium");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues openGrid=new ContentValues();openGrid.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",openGrid,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,2,2);position(db,target,4,2);
            JSONObject applied=db.applyCompiledStatblockSaveAction(actor,target,block,action,
                dc-1,0,"ui-monster-save-xmm-"+block.toLowerCase().replace(' ','-'));
            assertTrue(applied.toString(),applied.optBoolean("statblock_save_committed"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
            if(!linkedCondition.isEmpty()){
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),linkedCondition));
                JSONArray activeTimers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
                assertEquals(CompiledMonsterActionGuard.canonicalCondition(linkedCondition),activeTimers.getJSONObject(0)
                    .getJSONArray("linked_conditions").getString(0));
            }
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject recovered=db.resolveCompiledMonsterConditionRecovery(target,condition,dc,
                "ui-monster-recovery-xmm-"+block.toLowerCase().replace(' ','-'));
            assertTrue(recovered.toString(),recovered.optBoolean("recovery_committed"));
            assertTrue(recovered.getJSONObject("recovery_result").optBoolean("success"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
            if(!linkedCondition.isEmpty())assertFalse(db.getCombatant(target).optString("conditions")+
                " :: "+recovered.toString(),ConditionTokenGuard.has(
                db.getCombatant(target).optString("conditions"),linkedCondition));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
            assertEquals("",Backup21IntegrityEnvelope.check(db.exportAll()));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
