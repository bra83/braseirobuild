package com.braseiro.dnd5e.adventure;

import android.content.Context;
import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.AppDbTestHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * End-to-end regression for both scene-one branches in every installed Faerun
 * adventure. A green catalog/ID test is not enough: different player choices
 * must persist different flags, survive a physical SQLite reopen, and reject
 * replay from the next scene.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class FaerunBranchPersistenceRobolectricTest {

    @Test
    public void allFiveModulesPersistDistinctBranchOutcomesAcrossSqliteReopen() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        List<AdventureModule> modules = FaerunAdventureModuleCatalog.all();
        assertEquals("Baseline: five installed Faerun adventure modules", 5, modules.size());
        int branchRuns = 0;

        for (AdventureModule module : modules) {
            AdventureNode firstScene = module.getNode("scene_1");
            assertNotNull(module.getId() + ": missing first playable scene", firstScene);
            List<AdventureTransition> transitions = firstScene.getTransitions();
            assertTrue(module.getId() + ": first scene must offer real alternatives",
                    transitions.size() >= 2);

            AdventureTransition first = transitions.get(0);
            AdventureTransition alternative = transitions.get(1);
            assertEquals(module.getId() + ": both routes should demonstrate distinct outcomes",
                    first.getTargetNodeId(), alternative.getTargetNodeId());
            assertFalse(module.getId() + ": branch action IDs must differ",
                    first.getId().equals(alternative.getId()));

            String firstOnlyFlag = exclusiveFlag(first, alternative);
            String alternativeOnlyFlag = exclusiveFlag(alternative, first);
            assertNotNull(module.getId() + ": first branch needs its own durable flag", firstOnlyFlag);
            assertNotNull(module.getId() + ": alternative needs its own durable flag",
                    alternativeOnlyFlag);

            for (AdventureTransition chosen : new AdventureTransition[]{first, alternative}) {
                String databaseName = "faerun_branch_" +
                        UUID.randomUUID().toString().substring(0, 8) + ".db";
                AppDb db = AppDbTestHelper.createSandbox(context, databaseName);
                AppDb reopened = null;
                try {
                    db.setActiveMapProfile("dnd");
                    AdventureEngine engine = new AdventureEngine(db, null, null);
                    JSONObject setup = engine.loadOrStartAdventure(module, 23L);
                    assertTrue(module.getId() + ": module must load",
                            setup.optBoolean("module_installed"));
                    JSONObject begin = engine.processAction("begin", null);
                    assertTrue(module.getId() + ": setup -> scene_1 must commit: " + begin,
                            begin.optBoolean("committed"));
                    assertEquals("scene_1", engine.getActiveState().getCurrentNodeId());

                    JSONObject action = new JSONObject()
                            .put("target_node_id", chosen.getTargetNodeId());
                    if (chosen.getCheckOverride().optBoolean("requires_check")) {
                        JSONObject missingRoll = engine.processAction(chosen.getId(), action);
                        assertEquals(module.getId() + ": roll must be provided manually",
                                "requires_input", missingRoll.optString("status"));
                        assertEquals("scene_1", engine.getActiveState().getCurrentNodeId());
                        assertFalse(engine.getActiveState().hasFlag(
                                chosen == first ? firstOnlyFlag : alternativeOnlyFlag));
                    }
                    action.put("roll", 20).put("modifier", 20);
                    JSONObject result = engine.processAction(chosen.getId(), action);
                    assertTrue(module.getId() + "/" + chosen.getId() + ": " + result,
                            result.optBoolean("committed"));
                    assertEquals(chosen.getId(), result.optString("transition_id"));

                    String expectedFlag = chosen == first ? firstOnlyFlag : alternativeOnlyFlag;
                    String forbiddenFlag = chosen == first ? alternativeOnlyFlag : firstOnlyFlag;
                    assertTrue(module.getId() + ": chosen consequence missing",
                            engine.getActiveState().hasFlag(expectedFlag));
                    assertFalse(module.getId() + ": rejected route consequence leaked",
                            engine.getActiveState().hasFlag(forbiddenFlag));
                    assertEquals(chosen.getTargetNodeId(),
                            engine.getActiveState().getCurrentNodeId());
                    int recordedBranches = engine.getActiveState().getBranchHistory().length();
                    assertTrue(recordedBranches >= 2);

                    // Reopen the actual SQLite database, not just a second engine on
                    // the same SQLiteOpenHelper instance.
                    db.close();
                    db = null;
                    reopened = new AppDb(context, databaseName);
                    AdventureEngine resumed = new AdventureEngine(reopened, null, null);
                    JSONObject restored = resumed.loadOrStartAdventure(module, 23L);
                    assertEquals(module.getId() + ": restored node",
                            chosen.getTargetNodeId(),
                            restored.optString("current_node_id"));
                    assertTrue(resumed.getActiveState().hasFlag(expectedFlag));
                    assertFalse(resumed.getActiveState().hasFlag(forbiddenFlag));
                    assertEquals(recordedBranches,
                            resumed.getActiveState().getBranchHistory().length());

                    JSONObject replay = resumed.processAction(chosen.getId(), action);
                    assertFalse(module.getId() + ": old choice must not be replayed",
                            replay.optBoolean("committed"));
                    assertEquals(recordedBranches,
                            resumed.getActiveState().getBranchHistory().length());
                    branchRuns++;
                } finally {
                    if (reopened != null) reopened.close();
                    if (db != null) db.close();
                    context.deleteDatabase(databaseName);
                }
            }
        }
        assertEquals("Five modules with two independent SQLite-reopened branches each",
                10, branchRuns);
    }

    private static String exclusiveFlag(AdventureTransition wanted, AdventureTransition other) {
        JSONArray own = wanted.getEffects().optJSONArray("add_flags");
        JSONArray excluded = other.getEffects().optJSONArray("add_flags");
        if (own == null) return null;
        for (int i = 0; i < own.length(); i++) {
            String flag = own.optString(i);
            if (flag.isEmpty()) continue;
            boolean alsoInOther = false;
            if (excluded != null) {
                for (int j = 0; j < excluded.length(); j++) {
                    if (flag.equals(excluded.optString(j))) {
                        alsoInOther = true;
                        break;
                    }
                }
            }
            if (!alsoInOther) return flag;
        }
        return null;
    }
}
