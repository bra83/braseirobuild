package com.braseiro.dnd5e;

import android.content.Context;
import android.content.ContentValues;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** RUN231: real AppDb SQLite via Robolectric, not a Python DDL simulation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class Run231HostilityRobolectricTest {
    private static AppDb open(String name){
        Context c=RuntimeEnvironment.getApplication();
        AppDb db=new AppDb(c,"run231_"+name+".db");db.setActiveMapProfile("dnd");
        db.getWritableDatabase();return db;
    }
    private static long id(AppDb db,String name){
        for(JSONObject row:db.listCombatants())if(name.equals(row.optString("name")))return row.optLong("id");
        return 0;
    }
    @Test public void ledgerPersistsRejectsReplayAndRemapsAcrossBackup21() throws Exception {
        String name="ledger_"+System.nanoTime();AppDb db=open(name);
        try {
            long monster=db.addCombatant("Atirador",20,22,22,13,"",false);
            long target=db.addCombatant("Alvo",10,18,18,12,"",true);
            long threat=db.addCombatant("Sentinela",8,18,18,12,"",false);
            assertTrue(monster>0&&target>0&&threat>0);
            JSONObject first=db.replaceCompiledMonsterHostilityLedger(monster,
                new JSONArray().put(threat),"ui-hostility-run231-001");
            assertTrue(first.toString(),first.optBoolean("hostility_ledger_committed"));
            assertEquals(1,first.optInt("revision"));
            JSONObject replay=db.replaceCompiledMonsterHostilityLedger(monster,
                new JSONArray().put(target),"ui-hostility-run231-001");
            assertFalse(replay.toString(),replay.optBoolean("hostility_ledger_committed"));
            assertEquals(threat,db.getCompiledMonsterHostilityLedger(monster)
                .getJSONArray("hostile_ids").getLong(0));
            JSONObject invalid=db.replaceCompiledMonsterHostilityLedger(monster,
                new JSONArray().put(99999999L),"ui-hostility-run231-bad");
            assertFalse(invalid.toString(),invalid.optBoolean("hostility_ledger_committed"));
            assertEquals(1,db.getCompiledMonsterHostilityLedger(monster).optInt("revision"));
            db.close();db=open(name);
            assertEquals(threat,db.getCompiledMonsterHostilityLedger(monster)
                .getJSONArray("hostile_ids").getLong(0));
            JSONObject backup=db.exportAll();assertEquals(21,backup.optInt("version"));
            assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.importAll(backup);
            long restoredMonster=id(db,"Atirador"),restoredThreat=id(db,"Sentinela");
            assertTrue(restoredMonster>0&&restoredThreat>0);
            JSONObject restored=db.getCompiledMonsterHostilityLedger(restoredMonster);
            assertTrue(restored.toString(),restored.optBoolean("hostility_ledger_valid"));
            assertEquals(restoredThreat,restored.getJSONArray("hostile_ids").getLong(0));
            assertEquals(restoredMonster,new JSONObject(db.getCombatant(restoredMonster)
                .optString("resources_json")).getJSONObject("combat_hostility_v1").getLong("actor_id"));
            db.close();db=open(name);
            assertEquals(restoredThreat,db.getCompiledMonsterHostilityLedger(restoredMonster)
                .getJSONArray("hostile_ids").getLong(0));
        } finally {db.close();}
    }
    @Test public void automaticRangedThreatUsesDeclaredLiveSqliteTokens() throws Exception {
        String name="auto_"+System.nanoTime();AppDb db=open(name);
        try {
            String block="run231-ranged-"+name;
            JSONObject source=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",block)
                .put("source_book","RUN231 test fixture").put("source_locator","local-test-only");
            JSONObject attack=new JSONObject().put("name","Arco").put("category","action")
                .put("to_hit",4).put("damage","1d6 + 2").put("damage_type","perfurante")
                .put("range_normal_ft",80).put("range_long_ft",320);
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",block)
                .put("source_book","RUN231 test fixture").put("creature_type","humanoid")
                .put("armor_class",13).put("hit_points",20)
                .put("actions",new JSONArray().put(attack)).put("mechanical_basis",source));
            long monster=db.addCombatant("Arqueiro",20,20,20,13,"",false);
            long target=db.addCombatant("Alvo",10,20,20,12,"",true);
            long threat=db.addCombatant("Sentinela",8,20,20,12,"",false);
            JSONObject r=new JSONObject(db.getCombatant(monster).optString("resources_json","{}"));
            r.put("block_id",block);ContentValues cv=new ContentValues();cv.put("resources_json",r.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",cv,
                "id=? AND world_profile=?",new String[]{String.valueOf(monster),"dnd"}));
            assertTrue(db.replaceCompiledMonsterHostilityLedger(monster,
                new JSONArray().put(threat),"ui-hostility-run231-auto").optBoolean("hostility_ledger_committed"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            int w=scene.optInt("width_cells"),h=scene.optInt("height_cells");
            assertTrue(w>=7&&h>=7);
            db.ensureTacticalTokensForCombat();
            db.setTacticalTokenPosition(monster,2.5f/w,2.5f/h);
            db.setTacticalTokenPosition(target,6.5f/w,2.5f/h);
            db.setTacticalTokenPosition(threat,2.5f/w,3.5f/h);
            JSONObject preview=db.previewCompiledMonsterAttackRollMode(monster,target,block,"Arco");
            assertTrue(preview.toString(),preview.optBoolean("monster_roll_preview_valid"));
            assertEquals(threat,preview.optLong("additional_hostile_id"));
            assertEquals("typed_auto",preview.optString("hostility_selection_source"));
            assertEquals("Desvantagem",preview.optString("roll_mode"));
            db.updateConditions(threat,"Inconsciente");
            JSONObject safe=db.previewCompiledMonsterAttackRollMode(monster,target,block,"Arco");
            assertTrue(safe.toString(),safe.optBoolean("monster_roll_preview_valid"));
            assertEquals(0,safe.optLong("additional_hostile_id"));
            assertEquals("Normal",safe.optString("roll_mode"));
        } finally {db.close();}
    }
}
