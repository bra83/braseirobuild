package com.braseiro.dnd5e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class XmmManualArbitrationBridgeRobolectricTest {

    private static String databaseName() {
        return "xmm_arb_bridge_" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL) + ".db";
    }

    private static void bindMonsterResources(AppDb db, long actorId, String monsterName) throws Exception {
        JSONObject res = new JSONObject(db.getCombatant(actorId).optString("resources_json", "{}"));
        res.put("block_id", monsterName)
            .put("action_available", true)
            .put("bonus_action_available", true)
            .put("reaction_available", true);
        ContentValues values = new ContentValues();
        values.put("resources_json", res.toString());
        assertEquals(1, db.getWritableDatabase().update("combatants", values, "id=?",
            new String[]{String.valueOf(actorId)}));
    }

    @Test
    public void allSixtyTwoRegistryPointersKeepAutoExecutionBlockedAndSupportManualArbitrationAndRevert() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long heroId = db.addCharacter(
                "Valen", "Humano", "Guerreiro", 5, 44, 44, 18,
                16, 14, 14, 10, 12, 10, true, "Soldado");
            long monsterId = db.addCombatant("Monstro XMM", 20, 120, 120, 15, "", false);
            long heroCombatantId = db.addCombatant("Valen", 10, 44, 44, 18, "", true);
            db.setCombatRunning(true);
            assertEquals(monsterId, db.nextTurn().optLong("id"));

            assertEquals(62, XmmCombatSourceRegistry.entries().size());
            int index = 0;
            for (Map.Entry<String, XmmCombatSourceRegistry.Entry> mapEntry : XmmCombatSourceRegistry.entries().entrySet()) {
                index++;
                XmmCombatSourceRegistry.Entry entry = mapEntry.getValue();
                bindMonsterResources(db, monsterId, entry.monster);

                XmmCombatAdmission.Request autoReq = new XmmCombatAdmission.Request(
                    entry.pointer,
                    XmmCombatSourceRegistry.SOURCE_COMMIT,
                    XmmCombatSourceRegistry.SOURCE_BLOB,
                    "ui-monster-auto-" + index,
                    monsterId,
                    heroCombatantId,
                    15, 0, 12,
                    XmmCombatAdmission.Choice.NONE,
                    new int[]{4, 4}
                );
                JSONObject autoRes = db.applyXmmSourceAction(autoReq);
                assertEquals("NOT_INTEGRATED", autoRes.optString("xmm_action_blocked"));
                assertFalse(autoRes.optBoolean("persisted_to_combat", true));
                assertTrue(autoRes.optBoolean("manual_arbitration_available", false));
                assertEquals("applyXmmManualArbitration", autoRes.optString("manual_arbitration_entrypoint"));

                JSONObject preview = db.previewXmmManualArbitration(
                    monsterId, heroCombatantId, entry.monster, entry.pointer,
                    "action", 5, "necrotic", "poisoned", "");
                assertTrue("Preview failed for " + entry.pointer + ": " + preview,
                    preview.optBoolean("preview_only", false));
                assertFalse(preview.optBoolean("persisted_to_combat", true));
                assertEquals(entry.pointer, preview.optString("source_pointer"));
                assertEquals(entry.blocker, preview.optString("blocker_reason"));

                JSONObject unconfirmed = db.applyXmmManualArbitration(
                    monsterId, heroCombatantId, entry.monster, entry.pointer,
                    "action", 5, "necrotic", "poisoned", "", false, "Sem confirmacao",
                    "ui-monster-unconf-" + index);
                assertEquals("explicit_manual_confirmation_required",
                    unconfirmed.optString("manual_arbitration_blocked"));
                assertEquals(44, db.getCombatant(heroCombatantId).optInt("hp"));

                String applyToken = "ui-monster-arb-" + index;
                JSONObject applied = db.applyXmmManualArbitration(
                    monsterId, heroCombatantId, entry.monster, entry.pointer,
                    "action", 5, "necrotic", "poisoned", "", true,
                    "Arbitragem manual " + entry.action, applyToken);
                assertTrue("Apply failed for " + entry.pointer + ": " + applied,
                    applied.optBoolean("manual_arbitration_applied", false));
                assertEquals(39, db.getCombatant(heroCombatantId).optInt("hp"));
                assertEquals(39, db.getCharacter(heroId).optInt("hp"));
                assertTrue(db.getCombatant(heroCombatantId).optString("conditions", "").contains("poisoned"));
                JSONObject monsterResAfter = new JSONObject(db.getCombatant(monsterId).optString("resources_json", "{}"));
                assertFalse(monsterResAfter.optBoolean("action_available", true));

                // Replay with same token must fail-closed
                JSONObject replay = db.applyXmmManualArbitration(
                    monsterId, heroCombatantId, entry.monster, entry.pointer,
                    "action", 5, "necrotic", "poisoned", "", true,
                    "Replay", applyToken);
                assertEquals("already_resolved_or_replayed", replay.optString("manual_arbitration_blocked"));
                assertEquals(39, db.getCombatant(heroCombatantId).optInt("hp"));

                // Revert restores HP, conditions, and action economy
                JSONObject reverted = db.revertXmmManualArbitration(
                    monsterId, applied.optString("receipt_key"), "ui-monster-rev-" + index);
                assertTrue("Revert failed for " + entry.pointer + ": " + reverted,
                    reverted.optBoolean("manual_arbitration_reverted", false));
                assertEquals(44, db.getCombatant(heroCombatantId).optInt("hp"));
                assertEquals(44, db.getCharacter(heroId).optInt("hp"));
                assertEquals("", db.getCombatant(heroCombatantId).optString("conditions", ""));
                JSONObject monsterResRestored = new JSONObject(db.getCombatant(monsterId).optString("resources_json", "{}"));
                assertTrue(monsterResRestored.optBoolean("action_available", false));
            }
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void manualArbitrationCoversMultiattackAndCompiledManualFallbackAndSurvivesReopenAndBackup21() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        long heroCombatantId;
        long monsterId;
        String committedReceiptKey;
        try {
            db.setActiveMapProfile("dnd");
            db.addCharacter(
                "Lyra", "Elfa", "Mago", 5, 32, 32, 15,
                10, 14, 14, 18, 12, 10, true, "Sábio");
            monsterId = db.addCombatant("Mind Flayer", 18, 71, 71, 15, "", false);
            heroCombatantId = db.addCombatant("Lyra", 12, 32, 32, 15, "blessed", true);
            bindMonsterResources(db, monsterId, "Mind Flayer");
            db.setCombatRunning(true);
            assertEquals(monsterId, db.nextTurn().optLong("id"));

            // Multiattack preview & manual arbitration
            JSONObject multiPreview = db.previewXmmManualArbitration(
                monsterId, heroCombatantId, "Mind Flayer", "Multiattack",
                "action", 11, "psychic", "stunned", "blessed");
            assertTrue(multiPreview.optBoolean("preview_only", false));
            assertEquals("xmm_multiattack_manual", multiPreview.optString("source_kind"));

            JSONObject applied = db.applyXmmManualArbitration(
                monsterId, heroCombatantId, "Mind Flayer", "/monster/300/action/0/entries/0",
                "action", 15, "psychic", "grappled, stunned", "blessed",
                true, "Tentacles manual arbitration", "ui-monster-arb-persist-1");
            assertTrue(applied.optBoolean("manual_arbitration_applied", false));
            committedReceiptKey = applied.optString("receipt_key");
            assertNotNull(committedReceiptKey);
            assertEquals(17, db.getCombatant(heroCombatantId).optInt("hp"));

            // Second action attempt in same turn is blocked because action is spent
            JSONObject exhausted = db.applyXmmManualArbitration(
                monsterId, heroCombatantId, "Mind Flayer", "Multiattack",
                "action", 5, "psychic", "", "", true, "Extra", "ui-monster-arb-exhausted");
            assertEquals("action_available_spent", exhausted.optString("manual_arbitration_blocked"));
        } finally {
            db.close();
        }

        try {
            // Survive 3 SQLite reopens
            for (int i = 0; i < 3; i++) {
                AppDb reopened = new AppDb(context, dbName);
                try {
                    reopened.setActiveMapProfile("dnd");
                    assertEquals(17, reopened.getCombatant(heroCombatantId).optInt("hp"));
                    String cond = reopened.getCombatant(heroCombatantId).optString("conditions", "");
                    assertTrue(cond.contains("grappled"));
                    assertTrue(cond.contains("stunned"));
                    assertFalse(cond.contains("blessed"));
                    JSONObject replay = reopened.applyXmmManualArbitration(
                        monsterId, heroCombatantId, "Mind Flayer", "/monster/300/action/0/entries/0",
                        "action", 15, "psychic", "grappled, stunned", "blessed",
                        true, "Replay after reopen", "ui-monster-arb-persist-1");
                    assertEquals("already_resolved_or_replayed", replay.optString("manual_arbitration_blocked"));
                } finally {
                    reopened.close();
                }
            }

            // Survive 2 Backup21 export/import cycles with ID shift and still support revert
            AppDb backupDb = new AppDb(context, dbName);
            try {
                backupDb.setActiveMapProfile("dnd");
                for (int cycle = 1; cycle <= 2; cycle++) {
                    JSONObject snapshot = backupDb.exportAll();
                    assertEquals(21, snapshot.optInt("version"));
                    // Insert dummy combatant before import so IDs shift
                    backupDb.addCombatant("Dummy_" + cycle, 1, 10, 10, 10, "", false);
                    backupDb.importAll(snapshot);
                }
                long newMonsterId = -1;
                long newHeroId = -1;
                for (JSONObject c : backupDb.listCombatants()) {
                    if ("Mind Flayer".equals(c.optString("name"))) newMonsterId = c.optLong("id");
                    if ("Lyra".equals(c.optString("name"))) newHeroId = c.optLong("id");
                }
                assertTrue(newMonsterId > 0);
                assertTrue(newHeroId > 0);
                assertEquals(17, backupDb.getCombatant(newHeroId).optInt("hp"));

                // Replay with same token after Backup21 restore must still be blocked
                JSONObject replayAfterBackup = backupDb.applyXmmManualArbitration(
                    newMonsterId, newHeroId, "Mind Flayer", "/monster/300/action/0/entries/0",
                    "action", 15, "psychic", "grappled, stunned", "blessed",
                    true, "Replay after backup", "ui-monster-arb-persist-1");
                assertEquals("already_resolved_or_replayed", replayAfterBackup.optString("manual_arbitration_blocked"));

                // Revert using remapped receipt after Backup21 restore
                JSONObject revertAfterBackup = backupDb.revertXmmManualArbitration(
                    newMonsterId, "", "ui-monster-arb-rev-after-backup");
                assertTrue("Revert after Backup21 failed: " + revertAfterBackup,
                    revertAfterBackup.optBoolean("manual_arbitration_reverted", false));
                assertEquals(32, backupDb.getCombatant(newHeroId).optInt("hp"));
                assertEquals("blessed", backupDb.getCombatant(newHeroId).optString("conditions", ""));
                JSONObject monsterRes = new JSONObject(backupDb.getCombatant(newMonsterId).optString("resources_json", "{}"));
                assertTrue(monsterRes.optBoolean("action_available", false));
            } finally {
                backupDb.close();
            }
        } finally {
            context.deleteDatabase(dbName);
        }
    }
}
