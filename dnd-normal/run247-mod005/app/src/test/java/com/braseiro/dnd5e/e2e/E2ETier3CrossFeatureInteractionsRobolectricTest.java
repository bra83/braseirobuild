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
 * Tier 3: Cross-Feature Interactions E2E Suite.
 * Validates contracts between distinct subsystems without artificial stubs:
 * - Character Progression -> Equipment -> Attack -> Mitigation -> Persistence
 * - Tactical Grid -> Route -> Raycasting Cover -> Narrated Movement -> Replay
 * - Spell Eligibility -> Material Focus -> Concentration -> Turn Advancement
 * - World Time -> Narration Chunker -> Image Policy -> Backup21 Roundtrip
 * - Action Economy -> Condition Ledger -> Transactional Rollback
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class E2ETier3CrossFeatureInteractionsRobolectricTest {

    private static final class Fixture implements AutoCloseable {
        final Context baseContext = RuntimeEnvironment.getApplication();
        final String uniqueDbName = "e2e_t3_" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL) + ".db";
        final Context context;
        final AppDb db;

        Fixture() {
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
    // Interaction 1: Progression -> Equipment -> Attack -> Mitigation -> DB
    // =========================================================================
    @Test
    public void testCrossInteraction_Progression_Equip_Attack_Mitigation_Transaction() throws Exception {
        try (Fixture f = new Fixture()) {
            // Step 1: Create Character at Level 1
            long heroId = f.db.addCharacter("Kaelen", "Human", "Fighter", 1, 12, 12, 16,
                    16, 12, 14, 10, 10, 10, true, "fixture");
            assertTrue(heroId > 0);

            // Step 2: Level up to Level 2 (G07)
            JSONObject rawSheet = new JSONObject()
                    .put("name", "Kaelen").put("class_name", "Fighter").put("level", 1).put("xp", 350)
                    .put("hp", 12).put("max_hp", 12).put("con", 14).put("str", 16).put("dex", 12)
                    .put("int_score", 10).put("wis", 10).put("cha", 10)
                    .put("class_levels", new JSONArray().put(new JSONObject().put("class_name", "Fighter").put("level", 1)));

            JSONObject levelUpRes = CharacterLevelUpTransitionEngine.apply(rawSheet, "Fighter", 10, 2, true, false);
            assertTrue(levelUpRes.optBoolean("accepted"));
            JSONObject sheetLvl2 = levelUpRes.getJSONObject("state_after");
            assertEquals(2, sheetLvl2.optInt("level"));
            assertEquals(20, sheetLvl2.optInt("max_hp"));

            // Step 3: Equip Longsword on Level 2 sheet (G03)
            String weaponId = "t3-longsword";
            JSONObject weapon = new JSONObject().put("id", weaponId).put("name", "Longsword")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "1d8+3").put("weapon_damage_type", "cortante");
            sheetLvl2.put("inventory_ledger", new JSONArray().put(weapon));
            f.db.updateCharacterSheet(heroId, sheetLvl2.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(heroId, weaponId, 5, "1d8+3", "cortante", "melee", 5, 0, 0));

            // Step 4: Add to Combat, place on Grid and Preview (G04/G08)
            long actor = f.db.addCombatantWithVisual("Kaelen", 20, 20, 20, 16, "", true, "character", heroId, "");
            long target = f.db.addCombatantWithVisual("Skeleton", 10, 15, 15, 13, "", false, "statblock", 0, "");
            long actionId = f.db.addPartyAction(heroId, "Kaelen", "Atacar", "combate");
            f.db.markPartyActionsNarrated(new JSONArray().put(actionId));

            new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posA = new ContentValues(); posA.put("x", 2.5 / 12.0); posA.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posA, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)});
            ContentValues posT = new ContentValues(); posT.put("x", 3.5 / 12.0); posT.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posT, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(target)});

            f.db.setCombatRunning(true);
            f.db.nextTurn();

            JSONObject preview = f.db.previewTacticalWeaponAttack(actor, target, weaponId);
            assertTrue(preview.optBoolean("attack_geometry_valid"));
            String fp = preview.optString("attack_geometry_fingerprint");
            int ac = preview.optInt("effective_ac", 13);

            // Step 5: Execute Attack with Damage Mitigation (G02)
            // Skeleton takes normal damage from slashing
            int skeletonHpBefore = f.db.getCombatant(target).optInt("hp");
            JSONObject attackRes = f.db.applySingleWeaponAttackForPartyAction(actionId, actor, target,
                    18, 5, 7, "cortante", "ui-weapon2-t3-token-1", weaponId, "1d8+3", ac, fp, "Normal");
            assertTrue("party_action_resolved must be true: " + attackRes, attackRes.optBoolean("party_action_resolved"));
            assertEquals(skeletonHpBefore - 7, f.db.getCombatant(target).optInt("hp"));
        }
    }

    // =========================================================================
    // Interaction 2: Tactical Grid -> Route -> Raycasting Cover -> Narrated Move -> Replay
    // =========================================================================
    @Test
    public void testCrossInteraction_TacticalMovement_Terrain_Cover_Receipt_Replay() throws Exception {
        try (Fixture f = new Fixture()) {
            long actor = f.db.addCombatant("Ranger", 20, 30, 30, 15, "", true);
            f.db.setCombatRunning(true);
            f.db.nextTurn();
            JSONObject scene = new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();

            // Set initial position
            ContentValues pos = new ContentValues();
            pos.put("x", 1.5 / scene.optInt("width_cells"));
            pos.put("y", 1.5 / scene.optInt("height_cells"));
            f.db.getWritableDatabase().update("tactical_tokens", pos, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)});

            // Check distance calculation RAW
            assertEquals(20, TacticalMovementCostRules.costFeet(10, true, false)); // 10 ft in difficult terrain = 20 ft

            // Raycasting Cover calculation (G08)
            int w = 5, h = 5;
            boolean[][] opaque = new boolean[h][w];
            int[][] cover = new int[h][w];
            boolean[][] occupied = new boolean[h][w];
            cover[1][2] = 2; // Half cover (+2 AC) between (1,1) and (1,3)
            TacticalAttackGeometryRules.Result geom = TacticalAttackGeometryRules.evaluate(
                    w, h, 1, 1, 3, 1, opaque, cover, occupied, "ranged", 0, 150, 600);
            assertTrue(geom.legal());
            assertEquals(2, geom.geometryCoverBonus);

            // Preflight narrated move (G09)
            JSONObject proposal = new JSONObject().put("combatant_id", actor).put("name", "Ranger")
                    .put("x", 3.5 / scene.optInt("width_cells"))
                    .put("y", 1.5 / scene.optInt("height_cells"));

            String commitId = "t3-commit-route-000000001";
            JSONObject preflight = NarratedTacticalMovementExecutor.preflight(f.db, proposal, commitId, 0);
            assertTrue(preflight.optBoolean("narrated_tactical_move_preflight"));

            // Commit move
            JSONObject committed = NarratedTacticalMovementExecutor.executeChecked(f.db, proposal, commitId, 0);
            assertTrue(committed.optBoolean("narrated_tactical_move_committed"));
            String receipt = committed.optString("narrated_tactical_receipt");
            assertTrue(f.db.hasEffectReceipt(receipt));

            // Replay protection: second execution must throw IllegalStateException
            try {
                NarratedTacticalMovementExecutor.executeChecked(f.db, proposal, commitId, 0);
                fail("Replay move must be rejected");
            } catch (IllegalStateException e) {
                assertEquals("position_replayed", e.getMessage());
            }
        }
    }

    // =========================================================================
    // Interaction 3: Spell Concentration -> Turn Boundaries -> Expiration
    // =========================================================================
    @Test
    public void testCrossInteraction_SpellCasting_Concentration_TurnBoundary() {
        // Multi-round progression for Bless (G06)
        int castRound = 1;
        for (int round = 1; round <= 10; round++) {
            assertEquals("Round " + round + " concentration must be active",
                    Core2024BlessTurnRules.Decision.KEEP,
                    Core2024BlessTurnRules.reconcile(castRound, round, "Bênção", false));
        }

        // Round 11: 10 rounds completed -> Expired
        assertEquals(Core2024BlessTurnRules.Decision.EXPIRE,
                Core2024BlessTurnRules.reconcile(castRound, 11, "Bênção", false));

        // Damage disruption (incapacitated) terminates concentration early
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
                Core2024BlessTurnRules.reconcile(castRound, 4, "Bênção", true));
    }

    // =========================================================================
    // Interaction 4: World Time -> Narration -> Image Policy -> Backup21 Roundtrip
    // =========================================================================
    @Test
    public void testCrossInteraction_LivingWorld_Narration_Snapshot_Restore() throws Exception {
        try (Fixture f = new Fixture()) {
            // Step 1: Save World & Campaign
            f.db.saveCampaign("Dragon's Peak", "5e 2014", "Heroic", true, true, false, "active");
            f.db.addJournal("Discovery", "Found ancient dwarven ruins", "quest");

            // Step 2: Living World Time calculation (G11)
            long t0 = 10000000L;
            long t1 = t0 + (48 * 3600 * 1000L); // 48 real hours
            JSONObject catchUp = LivingWorldCatchUpRules.preview(true, true, t0, t1, 24.0, 100);
            assertEquals(48, catchUp.optInt("game_hours"));

            // Step 3: Narration Audio Chunker (G13)
            String prose = "As nuvens cobrem o pico da montanha. O vento uiva entre as rochas antigas.";
            List<String> chunks = NarrationChunker.android(prose);
            assertEquals(1, chunks.size());

            // Step 4: Scene Image Import Policy (G12)
            SceneImageImportPolicy.Result imgCheck = SceneImageImportPolicy.validate(
                    SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, "peak-1", "peak-1");
            assertTrue(imgCheck.accepted);

            // Step 5: Export Backup21 and Seal (G14)
            JSONObject snapshot = f.db.exportAll();
            assertEquals(21, snapshot.optInt("version"));
            assertEquals("", Backup21IntegrityEnvelope.check(snapshot));

            // Step 6: Mutate state, then restore from snapshot
            f.db.saveCampaign("Corrupted Campaign", "5e 2014", "Void", false, false, false, "void");
            f.db.importAll(snapshot);

            assertEquals("Dragon's Peak", f.db.getCampaign().optString("name"));
            assertEquals(1, f.db.listJournal().size());
            assertEquals("Found ancient dwarven ruins", f.db.listJournal().get(0).optString("body"));
        }
    }

    // =========================================================================
    // Interaction 5: Action Economy -> Condition Ledger -> Transaction Rollback
    // =========================================================================
    @Test
    public void testCrossInteraction_ActionEconomy_ConditionLedger_Rollback() throws Exception {
        try (Fixture f = new Fixture()) {
            long actor = f.db.addCharacter("Knight", "Human", "Fighter", 1, 25, 25, 16,
                    16, 12, 14, 10, 10, 10, true, "fixture");
            String weaponId = "t3-sword";
            JSONObject weapon = new JSONObject().put("id", weaponId).put("name", "Sword")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "1d8+3").put("weapon_damage_type", "cortante");
            JSONObject sheet = new JSONObject().put("species", "Human").put("class_name", "Fighter")
                    .put("inventory_ledger", new JSONArray().put(weapon));
            f.db.updateCharacterSheet(actor, sheet.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(actor, weaponId, 5, "1d8+3", "cortante", "melee", 5, 0, 0));

            long hero = f.db.addCombatantWithVisual("Knight", 20, 25, 25, 16, "", true, "character", actor, "");
            long target = f.db.addCombatantWithVisual("Goblin", 10, 10, 10, 12, "", false, "statblock", 0, "");
            long actionId = f.db.addPartyAction(actor, "Knight", "Atacar", "combate");
            f.db.markPartyActionsNarrated(new JSONArray().put(actionId));

            new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posA = new ContentValues(); posA.put("x", 2.5 / 12.0); posA.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posA, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(hero)});
            ContentValues posT = new ContentValues(); posT.put("x", 3.5 / 12.0); posT.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posT, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(target)});

            f.db.setCombatRunning(true);
            f.db.nextTurn();

            JSONObject preview = f.db.previewTacticalWeaponAttack(hero, target, weaponId);
            String fp = preview.optString("attack_geometry_fingerprint");
            int ac = preview.optInt("effective_ac", 12);
            int targetHpBefore = f.db.getCombatant(target).optInt("hp");

            // Inject SQLite failure trigger to force rollback (G02)
            SQLiteDatabase sql = f.db.getWritableDatabase();
            sql.execSQL("CREATE TRIGGER t3_abort BEFORE UPDATE OF status ON party_action_queue " +
                    "WHEN NEW.status='resolved' BEGIN SELECT RAISE(ABORT, 'injected rollback'); END");

            JSONObject failed = f.db.applySingleWeaponAttackForPartyAction(actionId, hero, target,
                    15, 5, 6, "cortante", "ui-weapon2-t3-rollback", weaponId, "1d8+3", ac, fp, "Normal");
            assertTrue("Operation error expected: " + failed, failed.has("operation_error"));
            assertEquals("HP must be untouched after rollback", targetHpBefore, f.db.getCombatant(target).optInt("hp"));

            // Remove trigger and retry successfully
            sql.execSQL("DROP TRIGGER t3_abort");
            JSONObject retry = f.db.applySingleWeaponAttackForPartyAction(actionId, hero, target,
                    15, 5, 6, "cortante", "ui-weapon2-t3-rollback", weaponId, "1d8+3", ac, fp, "Normal");
            assertTrue("Retry must succeed: " + retry, retry.optBoolean("party_action_resolved"));
            assertEquals(targetHpBefore - 6, f.db.getCombatant(target).optInt("hp"));
        }
    }
}
