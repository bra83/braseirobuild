package com.braseiro.dnd5e.adventure;

import android.content.Context;
import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.AppDbTestHelper;
import com.braseiro.dnd5e.Backup21IntegrityEnvelope;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

/** Validates that portable BACKUP21 now includes actual D&D adventure saves. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class AdventureBackupRoundTripRobolectricTest {
    @Test public void backupRestoresModuleFlagsAndBranchesAcrossPhysicalSqliteReopen()
            throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_adventure_backup_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule module = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
            AdventureEngine engine = new AdventureEngine(db, null, null);
            assertTrue(engine.loadOrStartAdventure(module, 71L).optBoolean("module_installed"));
            assertTrue(engine.processAction("begin", null).optBoolean("committed"));
            assertTrue(engine.processAction("advance_1", null).optBoolean("committed"));
            assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());
            int originalHistory = engine.getActiveState().getBranchHistory().length();
            assertTrue(engine.getActiveState().hasFlag("heir_claimed_openly"));

            JSONObject backup = db.exportAll();
            assertEquals("BACKUP21 signature mismatch", "",
                    Backup21IntegrityEnvelope.check(backup));
            assertTrue(backup.getJSONArray("adventureModules").length() >= 1);
            assertEquals(1, backup.getJSONArray("adventureStates").length());

            // Mutate after the snapshot and then restore the previous branch.
            assertTrue(engine.processAction("advance_2", null).optBoolean("committed"));
            assertEquals("scene_3", engine.getActiveState().getCurrentNodeId());
            db.importAll(backup);
            AdventurePersistenceAdapter store = new AdventurePersistenceAdapter(db);
            AdventureState restored = store.loadState(module.getId(), 71L);
            assertNotNull(restored);
            assertEquals("scene_2", restored.getCurrentNodeId());
            assertEquals(originalHistory, restored.getBranchHistory().length());
            assertTrue(restored.hasFlag("heir_claimed_openly"));
            assertFalse(restored.hasFlag("zhents_routed"));

            db.close();
            db = AppDbTestHelper.createSandbox(context, name);
            db.setActiveMapProfile("dnd");
            AdventureEngine reopened = new AdventureEngine(db, null, null);
            JSONObject result = reopened.loadOrStartAdventure(module, 71L);
            assertEquals("scene_2", result.optString("current_node_id"));
            assertEquals(originalHistory, reopened.getActiveState().getBranchHistory().length());
            assertTrue(reopened.getActiveState().hasFlag("heir_claimed_openly"));
            assertFalse(reopened.getActiveState().hasFlag("zhents_routed"));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test public void tamperedBackupMustNotDeleteExistingAdventureRows() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_bad_backup_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule module = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
            AdventureEngine engine = new AdventureEngine(db, null, null);
            assertTrue(engine.loadOrStartAdventure(module, 17L).optBoolean("module_installed"));
            assertTrue(engine.processAction("begin", null).optBoolean("committed"));
            JSONObject original = engine.getActiveState().toJson();
            JSONObject bad = db.exportAll();
            JSONArray states = bad.getJSONArray("adventureStates");
            states.getJSONObject(0).put("state_json", "{invalid_json");
            Backup21IntegrityEnvelope.seal(bad);
            boolean rejected = false;
            try { db.importAll(bad); }
            catch (Exception expected) { rejected = true; }
            assertTrue("A structurally invalid adventure backup must fail closed", rejected);
            AdventureState persisted = new AdventurePersistenceAdapter(db)
                    .loadState(module.getId(), 17L);
            assertNotNull(persisted);
            assertEquals(original.getString("current_node_id"), persisted.getCurrentNodeId());
            assertEquals(original.getJSONArray("branch_history").length(),
                    persisted.getBranchHistory().length());
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }
}
