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

/** Android SQLite/backup21 round-trip; written for CI, not a JVM-host substitute. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run236ConditionBackupRobolectricTest {
    private static void setTimers(AppDb db,long target,long source)throws Exception{
        JSONObject timer=new JSONObject().put("schema",MonsterConditionRecoveryLedger.SCHEMA)
            .put("name","envenenado").put("source_actor_id",source)
            .put("source_statblock","run236-fixture").put("source_action","Venom Cloud")
            .put("source_receipt","ui-monster-save-run236").put("save_dc",13)
            .put("save_ability","con").put("trigger","target_turn_end")
            .put("last_attempt_round",-1).put("last_attempt_encounter","");
        ContentValues v=new ContentValues();v.put("condition_timers_json",new JSONArray().put(timer).toString());
        v.put("conditions","envenenado");
        assertEquals(1,db.getWritableDatabase().update("combatants",v,
            "id=? AND world_profile=?",new String[]{String.valueOf(target),"dnd"}));
    }
    @Test public void backupRemapsSourceActorAndPreservesCondition()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run236_backup_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long source=db.addCombatant("RUN236 Source",20,30,30,12,"",false);
            long target=db.addCombatant("RUN236 Target",10,30,30,12,"",false);
            setTimers(db,target,source);
            JSONObject backup=db.exportAll();
            JSONArray exported=backup.getJSONArray("combatants");
            assertEquals(2,exported.length());
            for(int i=0;i<exported.length();i++){
                JSONObject row=exported.getJSONObject(i);
                assertFalse(row.toString(),row.has("cover"));
                assertFalse(row.toString(),row.has("effective_ac"));
                assertFalse(row.toString(),row.has("total_cover"));
                assertFalse(row.toString(),row.has("effective_dex_save_bonus"));
            }
            db.addCombatant("RUN236 Intervening",5,20,20,10,"",false);
            db.importAll(backup);
            JSONObject newSource=db.findCombatantByName("RUN236 Source");
            JSONObject newTarget=db.findCombatantByName("RUN236 Target");
            assertTrue(newSource.toString(),newSource.optLong("id")>0);
            assertTrue(newTarget.toString(),newTarget.optLong("id")>0);
            assertNotEquals(source,newSource.optLong("id"));
            JSONArray timers=new JSONArray(newTarget.optString("condition_timers_json","[]"));
            assertEquals(1,timers.length());
            assertEquals(newSource.optLong("id"),timers.getJSONObject(0).getLong("source_actor_id"));
            assertTrue(ConditionTokenGuard.has(newTarget.optString("conditions"),"envenenado"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            assertEquals(newSource.optLong("id"),new JSONArray(db.findCombatantByName("RUN236 Target")
                .optString("condition_timers_json","[]")).getJSONObject(0).getLong("source_actor_id"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void legacyEncounterCannotBypassPendingRecovery()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run236_legacy_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long source=db.addCombatant("RUN236 Source",20,30,30,12,"",false);
            long target=db.addCombatant("RUN236 Target",10,30,30,12,"",false);
            setTimers(db,target,source);
            db.setCombatRunning(true);
            JSONObject first=db.nextTurn();
            assertEquals("first turn: "+first.toString(),source,first.optLong("id"));
            JSONObject second=db.nextTurn();
            assertEquals("second turn: "+second.toString(),target,second.optLong("id"));
            ContentValues legacy=new ContentValues();legacy.put("encounter_id","");
            assertEquals(1,db.getWritableDatabase().update("combat_state_profiles",legacy,
                "world_profile=?",new String[]{"dnd"}));
            JSONObject blocked=db.nextTurn();
            assertEquals("condition_recovery_save_pending",blocked.optString("turn_advance_blocked"));
            assertEquals(target,db.findCombatantByName("RUN236 Target").optLong("id"));
            assertTrue(db.getCombatState().optString("encounter_id","").startsWith("enc-"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void orphanTimerAbortsEntireImport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String dbName="run236_orphan_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long source=db.addCombatant("RUN236 Source",20,30,30,12,"",false);
            long target=db.addCombatant("RUN236 Target",10,30,30,12,"",false);
            setTimers(db,target,source);
            JSONObject backup=db.exportAll();
            JSONArray rows=backup.getJSONArray("combatants"),partial=new JSONArray();
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.getJSONObject(i);
                if(row.optLong("id")!=source)partial.put(row);
            }
            backup.put("combatants",partial);
            Backup21IntegrityEnvelope.seal(backup);
            int before=db.listCombatants().size();
            try{db.importAll(backup);fail("Orphan source should rollback backup21");}
            catch(Exception expected){assertTrue(expected.toString().length()>0);}
            assertEquals(before,db.listCombatants().size());
            assertEquals(source,db.findCombatantByName("RUN236 Source").optLong("id"));
            assertEquals(target,db.findCombatantByName("RUN236 Target").optLong("id"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
