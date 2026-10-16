package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Independent XPHB/XDMG/XMM V1 release checks. A source-bound XMM composition
 * without an installed effect executor must not spend combat resources or
 * create effect receipts, even across Backup21 restore and SQLite reopens.
 * Does not load adventures, Mystara, supplements, Gemini or RAG.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class V1ManualBackupIsolationRobolectricTest {
    private Context context;
    private String dbName;
    private AppDb db;
    private JSONObject previousStatblocks;

    @Before public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        dbName = "v1_xmm_manual_backup_" + System.nanoTime() + ".db";
        previousStatblocks = CompiledStatblockEngine.snapshot();
        Xmm2025CompiledBestiaryEngine.install();
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        db.saveCampaign("V1 Core Isolation", DndRulesEngine.PROFILE_2024,
            "Sandbox", true, true, false, "");
    }

    @After public void tearDown() {
        if (db != null) db.close();
        if (context != null && dbName != null) context.deleteDatabase(dbName);
        if (previousStatblocks != null) CompiledStatblockEngine.restoreReplacing(previousStatblocks);
    }

    private JSONObject spellComposition() throws Exception {
        return new JSONObject().put("variant_index", 1)
            .put("selected_actions", new JSONArray()
                .put("Scorching Ray").put("Rend").put("Rend"));
    }

    private long setupDragonCombat() throws Exception {
        long actor = db.addCombatant("Adult Red Dragon", 20, 120, 120, 19, "", false);
        db.addCombatant("V1 target", 10, 40, 40, 15, "", true);
        JSONObject resources = new JSONObject(db.getCombatant(actor).getString("resources_json"));
        resources.put("block_id", "Adult Red Dragon");
        ContentValues values = new ContentValues();
        values.put("resources_json", resources.toString());
        assertEquals(1, db.getWritableDatabase().update("combatants", values,
            "id=? AND world_profile=?", new String[]{String.valueOf(actor), "dnd"}));
        db.setCombatRunning(true);
        assertEquals(actor, db.nextTurn().optLong("id"));
        return actor;
    }

    private void assertNoCombatEffects(long actor) throws Exception {
        assertEquals(0, DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=?",
            new String[]{"dnd"}));
        assertEquals(120, db.getCombatant(actor).optInt("hp"));
        JSONObject resources = new JSONObject(db.getCombatant(actor).getString("resources_json"));
        assertEquals("Adult Red Dragon", resources.optString("block_id"));
        assertTrue("blocked composition must not spend action", resources.optBoolean("action_available"));
    }

    @Test public void unimplementedXmmSpellCompositionStaysManualAcrossBackupAndThreeReopens() throws Exception {
        long actor = setupDragonCombat();
        JSONObject selection = spellComposition();
        JSONObject denied = db.applyCompiledStatblockMultiattack(actor, "Adult Red Dragon",
            new JSONArray(), "ui-monster-multi-v1-manual-block", selection);
        assertEquals("multiattack_component_executor_not_installed_spell_0",
            denied.optString("statblock_multiattack_blocked"));
        assertNoCombatEffects(actor);

        JSONObject backup = db.exportAll();
        assertEquals(21, backup.getInt("version"));
        assertEquals("dnd", backup.getString("worldProfile"));
        assertEquals("", Backup21IntegrityEnvelope.check(backup));
        db.importAll(backup);

        for (int reopen = 0; reopen < 3; reopen++) {
            db.close();
            db = new AppDb(context, dbName);
            db.setActiveMapProfile("dnd");
            long restoredActor = DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT id FROM combatants WHERE world_profile=? AND name=? ORDER BY id DESC LIMIT 1",
                new String[]{"dnd", "Adult Red Dragon"});
            assertTrue("dragon must survive Backup21 restore", restoredActor > 0);
            assertNoCombatEffects(restoredActor);
            JSONObject persisted = new JSONObject(db.getCombatant(restoredActor).getString("resources_json"));
            JSONObject stillManual = XmmMultiattackHostPlan.declaration(
                "Adult Red Dragon", selection, persisted);
            assertEquals("multiattack_component_executor_not_installed_spell_0",
                stillManual.optString("statblock_multiattack_blocked"));
            assertEquals("V1 Core Isolation", db.getCampaign().optString("name"));
            assertEquals("", Backup21IntegrityEnvelope.check(db.exportAll()));
        }
    }

    @Test public void tamperedBackup21IsRejectedWithoutOverwritingCampaign() throws Exception {
        JSONObject backup = db.exportAll();
        assertEquals("", Backup21IntegrityEnvelope.check(backup));
        backup.getJSONObject("campaign").put("name", "INJECTED WITHOUT SEAL");
        assertFalse("tampering must invalidate integrity envelope",
            Backup21IntegrityEnvelope.check(backup).isEmpty());
        boolean rejected = false;
        try {
            db.importAll(backup);
        } catch (Exception expected) {
            rejected = true;
        }
        assertTrue("import must fail closed for an invalid Backup21 envelope", rejected);
        assertEquals("V1 Core Isolation", db.getCampaign().optString("name"));

        db.close();
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        assertEquals("V1 Core Isolation", db.getCampaign().optString("name"));
        assertEquals("", Backup21IntegrityEnvelope.check(db.exportAll()));
    }
}
