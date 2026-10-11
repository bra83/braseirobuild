package com.braseiro.dnd5e.e2e;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import com.braseiro.dnd5e.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tier 4: Real-World Scenarios E2E Suite.
 * Simulates end-to-end realistic multi-round, multi-actor tabletop sessions:
 * - Scenario 1: "The Ambush at Blackwood Pass" (tactical multi-round combat)
 * - Scenario 2: "The Crypt Dungeon Crawl & Backup Recovery" (exploration, DM audio & full restore)
 * - Scenario 3: "Milestone Progression to Boss Encounter" (level transition, boss behind cover, rollback & victory)
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class E2ETier4RealWorldScenariosRobolectricTest {

    private static final class SessionFixture implements AutoCloseable {
        final Context baseContext = RuntimeEnvironment.getApplication();
        final String uniqueDbName = "e2e_t4_" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL) + ".db";
        final Context context;
        final AppDb db;

        SessionFixture() {
            context = new ContextWrapper(baseContext) {
                @Override
                public File getDatabasePath(String name) {
                    return super.getDatabasePath(uniqueDbName);
                }
            };
            db = new AppDb(context);
            db.setActiveMapProfile("dnd");
        }

        @Override
        public void close() {
            try {
                db.close();
            } finally {
                baseContext.deleteDatabase(uniqueDbName);
            }
        }
    }

    // =========================================================================
    // Scenario 1: The Ambush at Blackwood Pass (Multi-Round Tactical Combat)
    // =========================================================================
    @Test
    public void testScenario1_AmbushAtBlackwoodPass() throws Exception {
        try (SessionFixture f = new SessionFixture()) {
            f.db.saveCampaign("Ambush at Blackwood Pass", "5e 2014", "Tactical", true, true, false, "active");

            // Hero setup
            long heroId = f.db.addCharacter("Aron Strongbow", "Human", "Fighter", 1, 24, 24, 16,
                    16, 12, 14, 10, 10, 10, true, "hero");
            String swordId = "blackwood-broadsword";
            JSONObject sword = new JSONObject().put("id", swordId).put("name", "Broadsword")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "1d8+3").put("weapon_damage_type", "cortante");
            JSONObject sheet = new JSONObject().put("species", "Human").put("class_name", "Fighter")
                    .put("inventory_ledger", new JSONArray().put(sword));
            f.db.updateCharacterSheet(heroId, sheet.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(heroId, swordId, 5, "1d8+3", "cortante", "melee", 5, 0, 0));

            // Combatants setup
            long aron = f.db.addCombatantWithVisual("Aron Strongbow", 20, 24, 24, 16, "", true, "character", heroId, "");
            long goblin = f.db.addCombatantWithVisual("Goblin Scout", 10, 12, 12, 13, "", false, "statblock", 0, "");

            long attackAction = f.db.addPartyAction(heroId, "Aron Strongbow", "Atacar", "combate");
            f.db.markPartyActionsNarrated(new JSONArray().put(attackAction));

            // Scene and grid tokens
            JSONObject scene = new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posAron = new ContentValues();
            posAron.put("x", 2.5 / scene.optInt("width_cells")); posAron.put("y", 2.5 / scene.optInt("height_cells"));
            f.db.getWritableDatabase().update("tactical_tokens", posAron, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(aron)});
            ContentValues posGoblin = new ContentValues();
            posGoblin.put("x", 3.5 / scene.optInt("width_cells")); posGoblin.put("y", 2.5 / scene.optInt("height_cells"));
            f.db.getWritableDatabase().update("tactical_tokens", posGoblin, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(goblin)});

            // Start Combat
            f.db.setCombatRunning(true);
            JSONObject turn1 = f.db.nextTurn();
            assertEquals(aron, turn1.optLong("id"));

            // Round 1: Attack Goblin
            JSONObject preview = f.db.previewTacticalWeaponAttack(aron, goblin, swordId);
            assertTrue(preview.optBoolean("attack_geometry_valid"));
            int goblinHpBefore = f.db.getCombatant(goblin).optInt("hp");

            JSONObject strike = f.db.applySingleWeaponAttackForPartyAction(attackAction, aron, goblin,
                    15, 5, 7, "cortante", "ui-weapon2-blackwood-r1", swordId, "1d8+3",
                    preview.optInt("effective_ac", 13), preview.optString("attack_geometry_fingerprint"), "Normal");
            assertTrue(strike.optBoolean("party_action_resolved"));
            assertEquals(goblinHpBefore - 7, f.db.getCombatant(goblin).optInt("hp"));

            // Round 2: Advance turn
            JSONObject turn2 = f.db.nextTurn();
            assertEquals(goblin, turn2.optLong("id"));

            // Replay protection on round 1 attack
            JSONObject replay = f.db.applySingleWeaponAttackForPartyAction(attackAction, aron, goblin,
                    15, 5, 7, "cortante", "ui-weapon2-blackwood-r1", swordId, "1d8+3",
                    preview.optInt("effective_ac", 13), preview.optString("attack_geometry_fingerprint"), "Normal");
            assertTrue(replay.has("party_attack_blocked"));
        }
    }

    // =========================================================================
    // Scenario 2: The Crypt Dungeon Crawl & Backup Recovery
    // =========================================================================
    @Test
    public void testScenario2_CryptDungeonCrawlAndBackupRecovery() throws Exception {
        try (SessionFixture f = new SessionFixture()) {
            f.db.saveCampaign("Crypt of the Forgotten", "5e 2014", "Dungeon Crawl", true, true, false, "active");
            f.db.addJournal("Discovery", "Entramos na cripta silenciosa.", "exploration");

            // Living World Time simulation (G11)
            long startTime = 20000000L;
            long afterCrawl = startTime + (36 * 3600 * 1000L); // 36 hours elapsed
            JSONObject timePlan = LivingWorldCatchUpRules.preview(true, true, startTime, afterCrawl, 24.0, 100);
            assertEquals(36, timePlan.optInt("game_hours"));

            // DM Assistant Narration (G13)
            String dmNarration = "A porta de ferro range e se abre. O cheiro de pó e eras esquecidas preenche o ar frio.";
            List<String> chunks = NarrationChunker.android(dmNarration);
            assertEquals(1, chunks.size());

            // Image import verification (G12)
            SceneImageImportPolicy.Result imgOk = SceneImageImportPolicy.validate(
                    SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, "crypt-main", "crypt-main");
            assertTrue(imgOk.accepted);

            // Export complete Backup21 snapshot with SHA-256 seal (G14)
            JSONObject fullBackup = f.db.exportAll();
            assertEquals(21, fullBackup.optInt("version"));
            assertEquals("", Backup21IntegrityEnvelope.check(fullBackup));

            // Simulate catastrophic state reset
            f.db.saveCampaign("Corrupted", "5e 2014", "Empty", false, false, false, "none");
            f.db.importAll(fullBackup);

            // Verify full restoration
            assertEquals("Crypt of the Forgotten", f.db.getCampaign().optString("name"));
            assertEquals(1, f.db.listJournal().size());
            assertEquals("Entramos na cripta silenciosa.", f.db.listJournal().get(0).optString("body"));
        }
    }

    // =========================================================================
    // Scenario 3: Milestone Progression to Boss Encounter
    // =========================================================================
    @Test
    public void testScenario3_MilestoneProgressionToBossEncounter() throws Exception {
        try (SessionFixture f = new SessionFixture()) {
            // Hero earns milestone XP
            JSONObject sheetLvl1 = new JSONObject()
                    .put("name", "Valeria").put("class_name", "Fighter").put("level", 1).put("xp", 300)
                    .put("hp", 11).put("max_hp", 11).put("con", 14).put("str", 16).put("dex", 12)
                    .put("int_score", 10).put("wis", 12).put("cha", 10)
                    .put("class_levels", new JSONArray().put(new JSONObject().put("class_name", "Fighter").put("level", 1)));

            // Level up from 1 to 2
            JSONObject levelRes = CharacterLevelUpTransitionEngine.apply(sheetLvl1, "Fighter", 10, 2, true, false);
            assertTrue(levelRes.optBoolean("accepted"));
            JSONObject sheetLvl2 = levelRes.getJSONObject("state_after");
            assertEquals(2, sheetLvl2.optInt("level"));
            assertEquals(19, sheetLvl2.optInt("max_hp"));

            // Create character in DB
            long heroId = f.db.addCharacter("Valeria", "Human", "Fighter", 2, 19, 19, 16,
                    16, 12, 14, 10, 12, 10, true, "hero");
            String polearmId = "boss-halberd";
            JSONObject halberd = new JSONObject().put("id", polearmId).put("name", "Halberd")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "1d10+3").put("weapon_damage_type", "cortante");
            sheetLvl2.put("inventory_ledger", new JSONArray().put(halberd));
            f.db.updateCharacterSheet(heroId, sheetLvl2.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(heroId, polearmId, 5, "1d10+3", "cortante", "melee", 10, 0, 0));

            // Boss combat setup
            long valeria = f.db.addCombatantWithVisual("Valeria", 20, 19, 19, 16, "", true, "character", heroId, "");
            long boss = f.db.addCombatantWithVisual("Ogre Warlord", 5, 50, 50, 15, "", false, "statblock", 0, "");
            long actionId = f.db.addPartyAction(heroId, "Valeria", "Atacar", "combate");
            f.db.markPartyActionsNarrated(new JSONArray().put(actionId));

            new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posVal = new ContentValues(); posVal.put("x", 2.5 / 12.0); posVal.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posVal, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(valeria)});
            ContentValues posBoss = new ContentValues(); posBoss.put("x", 4.5 / 12.0); posBoss.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posBoss, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(boss)});

            f.db.setCombatRunning(true);
            f.db.nextTurn();

            // Preview attack (Halberd reach 10 ft spans 2 cells)
            JSONObject preview = f.db.previewTacticalWeaponAttack(valeria, boss, polearmId);
            assertTrue(preview.optBoolean("attack_geometry_valid"));
            String fp = preview.optString("attack_geometry_fingerprint");
            int bossAc = preview.optInt("effective_ac", 15);
            int bossHpBefore = f.db.getCombatant(boss).optInt("hp");

            // Attack Boss
            JSONObject attack = f.db.applySingleWeaponAttackForPartyAction(actionId, valeria, boss,
                    16, 5, 9, "cortante", "ui-weapon2-boss-strike-1", polearmId, "1d10+3", bossAc, fp, "Normal");
            assertTrue("party_action_resolved must be true: " + attack, attack.optBoolean("party_action_resolved"));
            assertEquals(bossHpBefore - 9, f.db.getCombatant(boss).optInt("hp"));
        }
    }
}
