package com.braseiro.dnd5e.adventure;

import android.content.Context;
import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.AppDbTestHelper;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

/** Never silently restart and overwrite an existing but unreadable campaign. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class AdventureCorruptSaveSafetyRobolectricTest {
    private static final String MODULE_ID = "fraif_heir_trial";

    @Test public void brokenJsonMustNotRestartOrReplaceOriginalRow() throws Exception {
        assertReadFailurePreservesExistingRow(false);
    }

    @Test public void orphanedNodeMustNotRestartOrReplaceOriginalRow() throws Exception {
        assertReadFailurePreservesExistingRow(true);
    }

    @Test public void corruptedSecondCampaignCannotReusePreviousActiveSession() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_cross_campaign_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureEngine active = new AdventureEngine(db, null, null);
            AdventureModule first = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
            AdventureModule second = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
            assertTrue(active.loadOrStartAdventure(first, 5L).optBoolean("module_installed"));
            assertTrue(active.processAction("begin", null).optBoolean("committed"));
            AdventureEngine seed = new AdventureEngine(db, null, null);
            assertTrue(seed.loadOrStartAdventure(second, 6L).optBoolean("module_installed"));
            db.getWritableDatabase().execSQL(
                    "UPDATE adventure_state_journal SET state_json=? WHERE module_id=? AND party_id=?",
                    new Object[]{"{damaged", second.getId(), 6});
            JSONObject denied = active.loadOrStartAdventure(second, 6L);
            assertEquals("error", denied.optString("status"));
            assertNull("Cannot retain previous campaign's active state", active.getActiveState());
            assertEquals("Old GM context cannot survive a failed load", 0,
                    active.getNarrationContext().length());
            assertFalse(active.processAction("begin", null).optBoolean("committed"));
            assertEquals("scene_1", new AdventurePersistenceAdapter(db)
                    .loadState(first.getId(), 5L).getCurrentNodeId());
            assertEquals("{damaged", db.queryOne(
                    "SELECT state_json FROM adventure_state_journal WHERE module_id=? AND party_id=?",
                    new String[]{second.getId(), "6"}).optString("state_json"));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test public void invalidSaveMustNotReplaceInstalledModuleVersion() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_module_rollback_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule original = FaerunAdventureModuleCatalog.byId(MODULE_ID);
            AdventureEngine engine = new AdventureEngine(db, null, null);
            assertTrue(engine.loadOrStartAdventure(original, 8L).optBoolean("module_installed"));
            assertTrue(engine.processAction("begin", null).optBoolean("committed"));
            String installedBefore = db.queryOne(
                    "SELECT module_json FROM adventure_modules WHERE module_id=?",
                    new String[]{MODULE_ID}).getString("module_json");

            JSONObject save = new AdventurePersistenceAdapter(db).loadState(MODULE_ID, 8L).toJson();
            save.put("current_node_id", "orphaned_old_scene");
            db.getWritableDatabase().execSQL(
                    "UPDATE adventure_state_journal SET current_node_id=?,state_json=? " +
                    "WHERE module_id=? AND party_id=?",
                    new Object[]{"orphaned_old_scene", save.toString(), MODULE_ID, 8});
            AdventureModule newer = new AdventureModule(original.getId(),
                    "Upgrade must not overwrite a damaged save", "2.0.0",
                    original.getRootNodeId(), original.getNodes(), original.getMetadata());
            AdventureEngine reopened = new AdventureEngine(db, null, null);
            JSONObject rejected = reopened.loadOrStartAdventure(newer, 8L);
            assertEquals("error", rejected.optString("status"));
            assertEquals(installedBefore, db.queryOne(
                    "SELECT module_json FROM adventure_modules WHERE module_id=?",
                    new String[]{MODULE_ID}).getString("module_json"));
            assertEquals("orphaned_old_scene", db.queryOne(
                    "SELECT current_node_id FROM adventure_state_journal " +
                    "WHERE module_id=? AND party_id=?",
                    new String[]{MODULE_ID, "8"}).optString("current_node_id"));
            assertNull(reopened.getActiveState());
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    private static void assertReadFailurePreservesExistingRow(boolean orphanNode) throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_corrupt_save_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule module = FaerunAdventureModuleCatalog.byId(MODULE_ID);
            AdventureEngine original = new AdventureEngine(db, null, null);
            assertTrue(original.loadOrStartAdventure(module, 44L).optBoolean("module_installed"));
            assertTrue(original.processAction("begin", null).optBoolean("committed"));
            assertEquals("scene_1", original.getActiveState().getCurrentNodeId());

            final String poisoned;
            if (orphanNode) {
                JSONObject saved = new JSONObject(new AdventurePersistenceAdapter(db)
                        .loadState(MODULE_ID, 44L).toJson().toString());
                saved.put("current_node_id", "removed_scene_does_not_exist");
                poisoned = saved.toString();
                db.getWritableDatabase().execSQL(
                        "UPDATE adventure_state_journal SET state_json=?,current_node_id=? " +
                        "WHERE module_id=? AND party_id=?",
                        new Object[]{poisoned, "removed_scene_does_not_exist", MODULE_ID, 44});
            } else {
                poisoned = "{invalid_json";
                db.getWritableDatabase().execSQL(
                        "UPDATE adventure_state_journal SET state_json=? " +
                        "WHERE module_id=? AND party_id=?",
                        new Object[]{poisoned, MODULE_ID, 44});
            }

            // Must read from the database again, not from a cached AdventureState.
            db.close();
            db = AppDbTestHelper.createSandbox(context, name);
            AdventureEngine resumed = new AdventureEngine(db, null, null);
            JSONObject result = resumed.loadOrStartAdventure(module, 44L);
            assertEquals("Existing campaign cannot be silently reseeded: " + result,
                    "error", result.optString("status"));
            JSONObject stored = db.queryOne(
                    "SELECT state_json,current_node_id FROM adventure_state_journal " +
                    "WHERE module_id=? AND party_id=?",
                    new String[]{MODULE_ID, "44"});
            assertEquals("Original row must be preserved for repair or backup",
                    poisoned, stored.optString("state_json"));
            assertFalse("Rejected campaign cannot become active", resumed.processAction("begin", null)
                    .optBoolean("committed"));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }
}
