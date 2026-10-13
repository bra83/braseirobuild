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

import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * P1: both Eberron modules must commit mechanically different routes and
 * endings, with manually provided rolls and SQLite restoration.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class EberronRouteDivergenceRobolectricTest {
    @Test public void bothModulesPersistTheirMainAndAlternateRoutes() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        List<AdventureModule> modules = EberronAdventureModuleCatalog.all();
        assertEquals(2, modules.size());
        int runs = 0;
        for (AdventureModule module : modules) {
            String mainFlag = module.getId() + "_path_main";
            String alternateFlag = module.getId() + "_path_alternative";
            for (boolean alternate : new boolean[]{false, true}) {
                String database = "eberron_route_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
                AppDb db = AppDbTestHelper.createSandbox(context, database);
                try {
                    db.setActiveMapProfile("dnd");
                    AdventureEngine engine = new AdventureEngine(db, null, null);
                    assertTrue(engine.loadOrStartAdventure(module, 39L).optBoolean("module_installed"));
                    assertTrue(engine.processAction("begin", null).optBoolean("committed"));
                    JSONObject initialRoll = new JSONObject().put("roll", 20).put("modifier", 20);
                    assertTrue(engine.processAction("advance_1", initialRoll).toString(),
                            engine.getActiveState().getCurrentNodeId().equals("scene_2"));
                    assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());

                    if (alternate) {
                        JSONObject withoutRoll = engine.processAction("alternative_clues", null);
                        assertEquals("requires_input", withoutRoll.optString("status"));
                        assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());
                        // A failed explicit roll has no irreversible mechanical effect.
                        JSONObject failed = engine.processAction("alternative_clues",
                                new JSONObject().put("roll", 1).put("modifier", -20));
                        assertFalse(failed.toString(), failed.optBoolean("committed"));
                        assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());
                    }
                    JSONObject route = engine.processAction(alternate ? "alternative_clues" : "advance_2",
                            new JSONObject().put("roll", 20).put("modifier", 20));
                    assertTrue(module.getId() + "/" + alternate + ": " + route,
                            route.optBoolean("committed"));
                    assertFalse(route.optBoolean("auto_rolls", false));
                    assertTrue(engine.getActiveState().hasFlag(alternate ? alternateFlag : mainFlag));
                    assertFalse(engine.getActiveState().hasFlag(alternate ? mainFlag : alternateFlag));
                    if (!alternate) {
                        JSONObject confront = engine.processAction("advance_3",
                                new JSONObject().put("roll", 20).put("modifier", 20));
                        assertTrue(confront.toString(), confront.optBoolean("committed"));
                    }
                    assertEquals("scene_4", engine.getActiveState().getCurrentNodeId());
                    String ending = alternate ? module.getNode("scene_4")
                            .getTransitions().get(1).getId()
                            : module.getNode("scene_4").getTransitions().get(0).getId();
                    assertTrue(engine.processAction(ending, null).optBoolean("committed"));
                    assertEquals("completed", engine.getActiveState().getStatus());
                    int branchCount = engine.getActiveState().getBranchHistory().length();

                    for (int reopen = 0; reopen < 3; reopen++) {
                        db.close();
                        db = AppDbTestHelper.createSandbox(context, database);
                        db.setActiveMapProfile("dnd");
                        AdventureEngine restored = new AdventureEngine(db, null, null);
                        assertEquals("completed", restored.loadOrStartAdventure(module, 39L)
                                .optString("current_node_id"));
                        assertEquals("completed", restored.getActiveState().getStatus());
                        assertEquals(branchCount, restored.getActiveState().getBranchHistory().length());
                        assertTrue(restored.getActiveState().hasFlag(alternate ? alternateFlag : mainFlag));
                        assertFalse(restored.getActiveState().hasFlag(alternate ? mainFlag : alternateFlag));
                        assertFalse(restored.processAction(ending, null).optBoolean("committed"));
                    }
                    runs++;
                } finally {
                    db.close();
                    context.deleteDatabase(database);
                }
            }
        }
        assertEquals(4, runs);
    }
}
