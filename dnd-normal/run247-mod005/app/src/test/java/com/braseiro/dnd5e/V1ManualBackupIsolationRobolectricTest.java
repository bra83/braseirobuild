package com.braseiro.dnd5e;

import android.content.Context;
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
 * Independent V1 release checks. Never treats a pending manual adjudication
 * as an authorized world mutation, and never accepts an unsealed Backup21 edit.
 * Does not depend on Gemini, RAG, Mystara, adventures or supplements.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class V1ManualBackupIsolationRobolectricTest {
    private Context context;
    private String dbName;
    private AppDb db;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "v1_manual_backup_" + System.nanoTime() + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        db.saveCampaign("V1 Core Isolation", DndRulesEngine.PROFILE_2024,
            "Sandbox", true, true, false, "");
    }

    @After public void tearDown() {
        if (db != null) db.close();
        if (context != null && dbName != null) context.deleteDatabase(dbName);
    }

    @Test public void pendingManualAdjudicationNeverCommitsAcrossBackupAndReopen() throws Exception {
        WorldStateEngine world = new WorldStateEngine(db);
        String before = world.gameplaySandboxState().toString();
        JSONObject pending = world.commitGameplayInput(
            "Examino uma runa que exige arbitragem", "PLAYER_ACTION",
            new JSONObject().put("confirm", true).put("requires_test", true));
        assertEquals("pending_manual_result", pending.optString("status"));
        assertFalse(pending.optBoolean("sandbox_mutated"));
        assertEquals("pending action must not change sandbox", before,
            world.gameplaySandboxState().toString());

        JSONObject backup = db.exportAll();
        assertEquals(21, backup.getInt("version"));
        assertEquals("", Backup21IntegrityEnvelope.check(backup));
        db.importAll(backup);
        db.close();
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        assertEquals("V1 Core Isolation", db.getCampaign().optString("name"));
        assertEquals("restoring pending arbitration must not apply effects", before,
            new WorldStateEngine(db).gameplaySandboxState().toString());
        assertEquals("", Backup21IntegrityEnvelope.check(db.exportAll()));
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
