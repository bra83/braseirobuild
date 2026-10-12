package com.braseiro.dnd5e.adventure;

import android.content.Context;
import com.braseiro.dnd5e.AppDb;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

/** Regressões de seleção de escolhas, pré-requisitos e atomicidade SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class AdventureTransactionSafetyRobolectricTest {
    private Context context;
    private AppDb db;
    private String databaseName;
    private AdventureEngine engine;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        databaseName = "adventure_tx_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, databaseName);
        db.setActiveMapProfile("dnd");
        engine = new AdventureEngine(db, null, null);
    }

    @After public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(databaseName);
    }

    private static JSONObject effects(String flag) throws Exception {
        return new JSONObject().put("add_flags", new JSONArray().put(flag));
    }

    private static AdventureModule module(String id, String target, boolean hasTerminalNode) throws Exception {
        AdventureTransition locked = new AdventureTransition("locked", target, "Rota bloqueada",
                "special_key", "", false, null, effects("locked_applied"));
        AdventureTransition chosen = new AdventureTransition("open", target, "Rota aberta",
                "", "", false, null, effects("open_applied"));
        AdventureTransition needsClue = new AdventureTransition("clue", target, "Rota de pista",
                "", "secret_note", false, null, effects("clue_applied"));
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();
        nodes.put("start", new AdventureNode("start", "Começo", "", "", "",
                Collections.<String>emptyList(), Arrays.asList(locked, chosen, needsClue), null, null));
        if (hasTerminalNode) {
            nodes.put(target, new AdventureNode(target, "Destino", "", "", "",
                    Collections.<String>emptyList(), new ArrayList<AdventureTransition>(), null, null));
        }
        return new AdventureModule(id, "Teste de segurança", "1.0", "start", nodes, new JSONObject());
    }

    @Test public void sameTargetRequiresActionIdAndAppliesOnlyChosenEffects() throws Exception {
        AdventureModule sample = module("tx_same_target", "end", true);
        assertTrue(engine.loadOrStartAdventure(sample, 7).optBoolean("module_installed"));

        JSONObject destination = new JSONObject().put("target_node_id", "end");
        assertFalse(engine.processAction("locked", destination).optBoolean("committed"));
        assertFalse(engine.processAction("clue", destination).optBoolean("committed"));
        assertFalse(engine.processAction("wrong_action", destination).optBoolean("committed"));

        // API textual legada sem ID: não adivinhar qual das três rotas aplicar.
        assertFalse(engine.processPlayerAction("Fazer alguma coisa", "end",
                0, 0, false, false).optBoolean("committed"));

        JSONObject actual = engine.processAction("open", destination);
        assertTrue(actual.toString(), actual.optBoolean("committed"));
        assertEquals("open", actual.optString("transition_id"));
        assertEquals("end", engine.getActiveState().getCurrentNodeId());
        assertTrue(engine.getActiveState().hasFlag("open_applied"));
        assertFalse(engine.getActiveState().hasFlag("locked_applied"));
        assertFalse(engine.getActiveState().hasFlag("clue_applied"));
    }

    @Test public void actionIdCannotBePairedWithDifferentDestination() throws Exception {
        engine.loadOrStartAdventure(module("tx_mismatch", "end", true), 7);
        JSONObject forged = engine.processAction("open",
                new JSONObject().put("target_node_id", "unknown"));
        assertEquals("blocked", forged.optString("status"));
        assertEquals("start", engine.getActiveState().getCurrentNodeId());
    }

    @Test public void failedSqliteWriteMustNotPublishGraphStateOrFlags() throws Exception {
        AdventureModule m = module("tx_rollback", "end", true);
        engine.loadOrStartAdventure(m, 7);
        db.getWritableDatabase().execSQL(
                "CREATE TRIGGER deny_adventure_save BEFORE INSERT ON adventure_state_journal " +
                "BEGIN SELECT RAISE(ABORT, 'test injection'); END");
        JSONObject failure = engine.processAction("open", new JSONObject().put("target_node_id", "end"));
        assertFalse(failure.toString(), failure.optBoolean("committed"));
        assertEquals("start", engine.getActiveState().getCurrentNodeId());
        assertFalse(engine.getActiveState().hasFlag("open_applied"));
        AdventureState stillPersisted = new AdventurePersistenceAdapter(db).loadState(m.getId(), 7);
        assertNotNull(stillPersisted);
        assertEquals("start", stillPersisted.getCurrentNodeId());
        db.getWritableDatabase().execSQL("DROP TRIGGER deny_adventure_save");

        JSONObject success = engine.processAction("open", new JSONObject().put("target_node_id", "end"));
        assertTrue(success.toString(), success.optBoolean("committed"));
        assertTrue(engine.getActiveState().hasFlag("open_applied"));
        assertEquals("end", new AdventurePersistenceAdapter(db).loadState(m.getId(), 7).getCurrentNodeId());
        JSONObject replay = engine.processAction("open", new JSONObject().put("target_node_id", "end"));
        assertFalse("A mesma recompensa não pode ser repetida", replay.optBoolean("committed"));
    }

    @Test public void uncheckedCommitApisCannotWriteState() throws Exception {
        AdventureModule m = module("tx_bypass", "end", true);
        engine.loadOrStartAdventure(m, 7);
        JSONObject untrusted = new JSONObject().put("target_node_id", "end")
                .put("add_flags", new JSONArray().put("forged_reward"));
        assertFalse(engine.commitResolution(untrusted).optBoolean("committed"));
        assertFalse(engine.commitTransaction());
        assertEquals("start", engine.getActiveState().getCurrentNodeId());
        assertFalse(engine.getActiveState().hasFlag("forged_reward"));

        AdventureGraphEngine graph = new AdventureGraphEngine();
        graph.startAdventure(m, 7);
        assertFalse(graph.commitResolution(untrusted).optBoolean("committed"));
        assertFalse(graph.commitTransaction());
    }

    @Test public void terminalAdventureStatusSurvivesReloadWithoutTerminalNode() throws Exception {
        AdventureModule m = module("tx_terminal", "completed", false);
        engine.loadOrStartAdventure(m, 7);
        JSONObject completed = engine.processAction("open",
                new JSONObject().put("target_node_id", "completed"));
        assertTrue(completed.toString(), completed.optBoolean("committed"));
        assertEquals("completed", engine.getActiveState().getStatus());
        assertFalse(engine.processAction("open", null).optBoolean("committed"));

        AdventureEngine reopened = new AdventureEngine(db, null, null);
        JSONObject resume = reopened.loadOrStartAdventure(m, 7);
        assertEquals("completed", resume.optString("current_node_id"));
        assertEquals("completed", reopened.getActiveState().getStatus());
        assertTrue(reopened.getActiveState().hasFlag("open_applied"));
    }

    @Test public void faerunChoiceWithSameTargetPreservesSpecificConsequences() throws Exception {
        AdventureModule damara = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
        assertNotNull(damara);
        engine.loadOrStartAdventure(damara, 2);
        assertTrue(engine.processAction("begin", null).optBoolean("committed"));
        JSONObject params = new JSONObject().put("target_node_id", "scene_2")
                .put("roll", 17).put("modifier", 0);
        JSONObject result = engine.processAction("stealth_bypass", params);
        assertTrue(result.toString(), result.optBoolean("committed"));
        assertTrue(engine.getActiveState().hasFlag("damara_ghouls_bypassed"));
        assertFalse(engine.getActiveState().hasFlag("damara_ghouls_slain"));
    }
}
