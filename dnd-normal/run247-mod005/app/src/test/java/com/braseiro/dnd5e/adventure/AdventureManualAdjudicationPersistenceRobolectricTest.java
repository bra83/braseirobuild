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

import java.util.UUID;
import static org.junit.Assert.*;

/** P3: manual roll proposals and confirmations must survive physical SQLite reopen. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class AdventureManualAdjudicationPersistenceRobolectricTest {
    private static JSONObject spec() throws Exception {
        return new JSONObject()
                .put("situation", "Inspecionar passagem escondida")
                .put("on_success", "Descobre a passagem")
                .put("on_failure", "Perde a pista")
                .put("check", "Percepção")
                .put("rule_source", "D&D 5e");
    }

    @Test public void proposalAndPlayerConfirmationSurviveThreePhysicalReopens() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_manual_reopen_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule module = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
            AdventureEngine engine = new AdventureEngine(db, null, null);
            assertTrue(engine.loadOrStartAdventure(module, 55L).optBoolean("module_installed"));
            JSONObject proposal = engine.proposeManualAdjudication(spec());
            assertEquals("requires_manual_result", proposal.optString("status"));
            assertFalse(proposal.optBoolean("auto_rolls"));
            String request = proposal.getJSONObject("pending").getString("request_id");
            assertFalse(request.isEmpty());

            for (int round = 0; round < 3; round++) {
                db.close();
                db = AppDbTestHelper.createSandbox(context, name);
                db.setActiveMapProfile("dnd");
                engine = new AdventureEngine(db, null, null);
                engine.loadOrStartAdventure(module, 55L);
                assertEquals(request, engine.getActiveState()
                        .getPendingManualAdjudication().optString("request_id"));
                assertEquals(0, engine.getActiveState().getManualAdjudicationHistory().length());
            }

            JSONObject mistaken = engine.resolveManualAdjudication(new JSONObject()
                    .put("request_id", "wrong-id").put("outcome", "success"));
            assertFalse(mistaken.optBoolean("committed"));
            JSONObject choice = new JSONObject().put("request_id", request)
                    .put("outcome", "success")
                    .put("observed_facts", new JSONArray().put("hidden_passage_discovered"));
            JSONObject accepted = engine.resolveManualAdjudication(choice);
            assertEquals("resolved_manual", accepted.optString("status"));
            assertTrue(accepted.optBoolean("committed"));
            assertEquals(0, engine.getActiveState().getPendingManualAdjudication().length());
            assertTrue(engine.getActiveState().getDiscoveredFacts()
                    .contains("hidden_passage_discovered"));

            db.close();
            db = AppDbTestHelper.createSandbox(context, name);
            db.setActiveMapProfile("dnd");
            AdventureEngine reopened = new AdventureEngine(db, null, null);
            reopened.loadOrStartAdventure(module, 55L);
            assertTrue(reopened.getActiveState().getDiscoveredFacts()
                    .contains("hidden_passage_discovered"));
            assertEquals(1, reopened.getActiveState().getManualAdjudicationHistory().length());
            assertEquals("setup", reopened.getActiveState().getCurrentNodeId());

            JSONObject duplicate = reopened.resolveManualAdjudication(choice);
            assertEquals("already_resolved", duplicate.optString("status"));
            assertEquals(1, reopened.getActiveState().getManualAdjudicationHistory().length());
            assertEquals("setup", reopened.getActiveState().getCurrentNodeId());
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test public void rejectedSqliteCommitKeepsManualChoicePendingForRetry() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "dnd_manual_failure_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = AppDbTestHelper.createSandbox(context, name);
        try {
            db.setActiveMapProfile("dnd");
            AdventureModule module = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
            AdventureEngine engine = new AdventureEngine(db, null, null);
            engine.loadOrStartAdventure(module, 56L);
            String request = engine.proposeManualAdjudication(spec())
                    .getJSONObject("pending").getString("request_id");
            db.getWritableDatabase().execSQL(
                    "CREATE TRIGGER deny_manual_save BEFORE INSERT ON adventure_state_journal " +
                    "BEGIN SELECT RAISE(ABORT, 'test injection'); END");
            JSONObject decision = new JSONObject().put("request_id", request)
                    .put("outcome", "failure");
            JSONObject rejected = engine.resolveManualAdjudication(decision);
            assertFalse(rejected.toString(), rejected.optBoolean("committed"));
            assertEquals(request, engine.getActiveState()
                    .getPendingManualAdjudication().optString("request_id"));
            assertEquals(0, engine.getActiveState().getManualAdjudicationHistory().length());
            db.getWritableDatabase().execSQL("DROP TRIGGER deny_manual_save");

            JSONObject accepted = engine.resolveManualAdjudication(decision);
            assertEquals("resolved_manual", accepted.optString("status"));
            assertEquals(1, engine.getActiveState().getManualAdjudicationHistory().length());
            assertEquals(0, engine.getActiveState().getPendingManualAdjudication().length());
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }
}
