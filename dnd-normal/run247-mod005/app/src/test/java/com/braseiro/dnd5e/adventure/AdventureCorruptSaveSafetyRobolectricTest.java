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
