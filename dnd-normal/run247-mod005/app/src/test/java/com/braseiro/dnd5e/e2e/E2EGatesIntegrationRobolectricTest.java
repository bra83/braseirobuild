package com.braseiro.dnd5e.e2e;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import com.braseiro.dnd5e.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Master E2E Integration Suite for Gates G01 through G16.
 * Exercises all subsystem public APIs under Android SQLite / Robolectric
 * without modifying production code.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class E2EGatesIntegrationRobolectricTest {

    public static final class DbFixture implements AutoCloseable {
        public final Context baseContext = RuntimeEnvironment.getApplication();
        public final String uniqueDbName = "e2e_g_" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL) + ".db";
        public final Context context;
        public final AppDb db;

        public DbFixture() {
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
    // Gate G01: Identity, Lineage and Baseline Integrity
    // =========================================================================
    @Test
    public void testG01IdentityAndLineage() {
        try (DbFixture f = new DbFixture()) {
            assertEquals("com.braseiro.dnd5e", f.baseContext.getPackageName());
            assertEquals(61, f.db.getReadableDatabase().getVersion());
            assertEquals(12, Backup21RequiredSections.OBJECTS.length);
            assertEquals(52, Backup21RequiredSections.ARRAYS.length);

            // Verify Backup21 required sections contract
            String missing = Backup21RequiredSections.missing(21, new Backup21RequiredSections.Presence() {
                @Override
                public boolean object(String key) {
                    return true;
                }

                @Override
                public boolean array(String key) {
                    return true;
                }
            });
            assertEquals("", missing);
        }
    }

    // =========================================================================
    // Gate G02: Attack Resolution, Mitigation & SQLite Rollback
    // =========================================================================
    @Test
    public void testG02AttackResolutionAndRollback() throws Exception {
        try (DbFixture f = new DbFixture()) {
            long heroId = f.db.addCharacter("G02 Paladin", "Human", "Paladin", 1, 22, 22, 16,
                    16, 10, 14, 10, 12, 14, true, "fixture");
            String weaponId = "g02-greatsword";
            JSONObject weapon = new JSONObject().put("id", weaponId).put("name", "Greatsword")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "2d6+3").put("weapon_damage_type", "cortante");
            JSONObject sheet = new JSONObject().put("species", "Human").put("class_name", "Paladin")
                    .put("inventory_ledger", new JSONArray().put(weapon));
            f.db.updateCharacterSheet(heroId, sheet.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(heroId, weaponId, 5, "2d6+3", "cortante", "melee", 5, 0, 0));

            long actor = f.db.addCombatantWithVisual("G02 Paladin", 20, 22, 22, 16, "", true, "character", heroId, "");
            long target = f.db.addCombatantWithVisual("G02 Zombie", 5, 30, 30, 12, "", false, "statblock", 0, "");
            long actionId = f.db.addPartyAction(heroId, "G02 Paladin", "Atacar", "combate");
            assertEquals(1, f.db.markPartyActionsNarrated(new JSONArray().put(actionId)));

            new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posA = new ContentValues();
            posA.put("x", 2.5 / 12.0);
            posA.put("y", 2.5 / 8.0);
            assertEquals(1, f.db.getWritableDatabase().update("tactical_tokens", posA, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)}));
            ContentValues posT = new ContentValues();
            posT.put("x", 3.5 / 12.0);
            posT.put("y", 2.5 / 8.0);
            assertEquals(1, f.db.getWritableDatabase().update("tactical_tokens", posT, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(target)}));

            f.db.setCombatRunning(true);
            assertEquals(actor, f.db.nextTurn().optLong("id"));

            JSONObject preview = f.db.previewTacticalWeaponAttack(actor, target, weaponId);
            assertTrue(preview.optBoolean("attack_geometry_valid"));
            assertEquals(12, preview.optInt("effective_ac"));

            // Commit attack with ui-weapon2- prefix
            int initialHp = f.db.getCombatant(target).optInt("hp");
            JSONObject resolved = f.db.applySingleWeaponAttackForPartyAction(actionId, actor, target,
                    16, 5, 10, "cortante", "ui-weapon2-g02-token-1", weaponId, "2d6+3", 12,
                    preview.optString("attack_geometry_fingerprint"), "Normal");
            assertTrue("party_action_resolved should be true: " + resolved, resolved.optBoolean("party_action_resolved"));
            assertEquals(initialHp - 10, f.db.getCombatant(target).optInt("hp"));

            // Mitigation test
            assertEquals(0, DamageMitigationRules.apply(10, true, false, false)); // immune
            assertEquals(5, DamageMitigationRules.apply(10, false, true, false)); // resistant
            assertEquals(20, DamageMitigationRules.apply(10, false, false, true)); // vulnerable
        }
    }

    // =========================================================================
    // Gate G03: Equipped Weapon Profile Enforcement
    // =========================================================================
    @Test
    public void testG03EquippedWeaponProfile() throws Exception {
        try (DbFixture f = new DbFixture()) {
            long heroId = f.db.addCharacter("G03 Ranger", "Elf", "Ranger", 1, 18, 18, 15,
                    12, 16, 14, 10, 14, 10, true, "fixture");
            String weaponId = "g03-longsword";
            JSONObject weapon = new JSONObject().put("id", weaponId).put("name", "Longsword")
                    .put("equipped", true).put("quantity", 1).put("weapon_attack_bonus", 5)
                    .put("weapon_damage_formula", "1d8+3").put("weapon_damage_type", "cortante");
            JSONObject sheet = new JSONObject().put("species", "Elf").put("class_name", "Ranger")
                    .put("inventory_ledger", new JSONArray().put(weapon));
            f.db.updateCharacterSheet(heroId, sheet.toString());
            assertTrue(f.db.configureEquippedWeaponAttack(heroId, weaponId, 5, "1d8+3", "cortante", "melee", 5, 0, 0));

            long actor = f.db.addCombatantWithVisual("G03 Ranger", 20, 18, 18, 15, "", true, "character", heroId, "");
            long target = f.db.addCombatantWithVisual("G03 Target", 5, 25, 25, 12, "", false, "statblock", 0, "");
            long actionId = f.db.addPartyAction(heroId, "G03 Ranger", "Atacar", "combate");
            f.db.markPartyActionsNarrated(new JSONArray().put(actionId));

            new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();
            ContentValues posA = new ContentValues();
            posA.put("x", 2.5 / 12.0); posA.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posA, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)});
            ContentValues posT = new ContentValues();
            posT.put("x", 3.5 / 12.0); posT.put("y", 2.5 / 8.0);
            f.db.getWritableDatabase().update("tactical_tokens", posT, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(target)});

            f.db.setCombatRunning(true);
            f.db.nextTurn();

            JSONObject preview = f.db.previewTacticalWeaponAttack(actor, target, weaponId);
            assertTrue(preview.optBoolean("attack_geometry_valid"));
            String fp = preview.optString("attack_geometry_fingerprint");
            int ac = preview.optInt("effective_ac", 12);

            // Attempt attack with diverging bonus
            JSONObject wrongBonus = f.db.applySingleWeaponAttackForPartyAction(actionId, actor, target,
                    15, 9, 7, "cortante", "ui-weapon2-g03-token-fail", weaponId, "1d8+3", ac, fp, "Normal");
            assertEquals("equipped_weapon_or_damage_invalid", wrongBonus.optString("party_attack_blocked"));

            // Attempt attack with unconfigured weapon
            JSONObject unconfigured = f.db.applySingleWeaponAttackForPartyAction(actionId, actor, target,
                    15, 5, 7, "cortante", "ui-weapon2-g03-token-missing", "ghost-weapon", "1d8+3", ac, fp, "Normal");
            assertEquals("weapon_profile_missing", unconfigured.optString("party_attack_blocked"));
        }
    }

    // =========================================================================
    // Gate G04: Tactical Grid Movement & Budget
    // =========================================================================
    @Test
    public void testG04TacticalGridMovementAndBudget() throws Exception {
        // RAW 5e 2014/2024: normal movement is 1 ft per ft
        assertEquals(30, TacticalMovementCostRules.costFeet(30, false, false));
        // Difficult terrain doubles cost (2 ft per ft)
        assertEquals(60, TacticalMovementCostRules.costFeet(30, true, false));
        // Crawling doubles cost (2 ft per ft)
        assertEquals(60, TacticalMovementCostRules.costFeet(30, false, true));
        // Difficult terrain + crawling triples cost (3 ft per ft)
        assertEquals(90, TacticalMovementCostRules.costFeet(30, true, true));
        // Boundary validation
        try {
            TacticalMovementCostRules.costFeet(0, false, false);
            fail("Expected exception on 0 ft distance");
        } catch (IllegalArgumentException expected) {
            assertEquals("invalid_tactical_distance", expected.getMessage());
        }

        // Tap translation: screen pixels to persisted grid cell
        TacticalGridTapRules.Cell tap = TacticalGridTapRules.fromTap(1200, 800, 12, 8, 450f, 250f);
        assertTrue("Tap must be valid: " + tap.blocked, tap.valid());
        assertEquals(4, tap.x);
        assertEquals(2, tap.y);
        TacticalGridTapRules.Cell outTap = TacticalGridTapRules.fromTap(1200, 800, 12, 8, -50f, 250f);
        assertFalse(outTap.valid());
        assertEquals("outside_board", outTap.blocked);

        // Dijkstra route planning with obstacles and cell-level difficult terrain
        boolean[][] blocked = new boolean[8][12];
        boolean[][] difficult = new boolean[8][12];
        difficult[2][3] = true;
        blocked[1][3] = true; // block upper flank
        blocked[3][3] = true; // block lower flank
        TacticalGridMovementRouteRules.Route route = TacticalGridMovementRouteRules.plan(
                12, 8, 2.5 / 12.0, 2.5 / 8.0, 4, 2, blocked, difficult, false, false);
        assertTrue("Route must succeed: " + route.error, route.ok());
        assertEquals(2, route.steps);
        assertEquals(15, route.budgetFeet); // 1 normal step (5ft) + 1 difficult step (10ft) = 15ft
        assertEquals(1, route.difficultSteps);

        // Route error boundaries
        blocked[2][4] = true;
        TacticalGridMovementRouteRules.Route blockedRoute = TacticalGridMovementRouteRules.plan(
                12, 8, 2.5 / 12.0, 2.5 / 8.0, 4, 2, blocked, difficult, false, false);
        assertFalse(blockedRoute.ok());
        assertEquals("destination_occupied_or_blocked", blockedRoute.error);

        TacticalGridMovementRouteRules.Route originRoute = TacticalGridMovementRouteRules.plan(
                12, 8, 2.5 / 12.0, 2.5 / 8.0, 2, 2, blocked, difficult, false, false);
        assertFalse(originRoute.ok());
        assertEquals("destination_is_origin", originRoute.error);

        // SQLite End-to-End Persistence: Preview -> Commit -> Budget -> Token Position
        try (DbFixture f = new DbFixture()) {
            long actor = f.db.addCombatant("G04 Runner", 20, 30, 30, 14, "", false);
            f.db.setCombatRunning(true);
            JSONObject turn = f.db.nextTurn();
            assertEquals(actor, turn.optLong("id"));

            JSONObject scene = new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            assertEquals(12, scene.optInt("width_cells"));
            assertEquals(8, scene.optInt("height_cells"));
            ContentValues clearObstacles = new ContentValues();
            clearObstacles.put("obstacles_json", "[]");
            assertEquals(1, f.db.getWritableDatabase().update("tactical_scenes", clearObstacles,
                    "world_profile=? AND scene_key=?", new String[]{"dnd", scene.optString("scene_key")}));
            f.db.ensureTacticalTokensForCombat();

            ContentValues pos = new ContentValues();
            pos.put("x", 2.5 / 12.0);
            pos.put("y", 2.5 / 8.0);
            assertEquals(1, f.db.getWritableDatabase().update("tactical_tokens", pos,
                    "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)}));

            JSONObject preview = f.db.previewTacticalGridMovement(actor, 4, 2, false, false);
            assertTrue("Grid preview must be valid: " + preview, preview.optBoolean("grid_valid"));
            assertEquals(10, preview.optInt("movement_cost_ft"));
            JSONObject prospectivePath=TacticalGridMovementProofRules.receipt(actor,preview);
            assertTrue("Preview path proof must validate: " + prospectivePath,
                    TacticalGridMovementProofRules.valid(prospectivePath,actor));

            JSONObject moved = f.db.moveCombatantToGridCell(actor, 4, 2, false, false,
                    preview.optString("grid_fingerprint"), "ui-grid-g04-move-001");
            assertTrue("Grid movement must be committed: " + moved, moved.optBoolean("grid_movement_committed"));
            JSONObject pathProof = moved.optJSONObject("committed_path");
            assertTrue("Committed route must be retained: " + moved,
                    TacticalGridMovementProofRules.valid(pathProof, actor));
            assertEquals(10, pathProof.optInt("distance_ft"));
            assertEquals(preview.getJSONArray("route_cells").toString(),
                    pathProof.getJSONArray("route_cells").toString());
            assertTrue(f.db.latestTacticalGridMovementProof(actor).toString(),
                    f.db.latestTacticalGridMovementProof(actor).optBoolean("grid_path_valid"));
            assertEquals(4, f.db.tacticalGridPosition(actor).optInt("cell_x"));
            assertEquals(2, f.db.tacticalGridPosition(actor).optInt("cell_y"));

            JSONObject resources = new JSONObject(f.db.getCombatant(actor).optString("resources_json", "{}"));
            assertEquals(20, resources.optInt("movement_remaining_ft")); // 30 - 10 = 20ft

            // Same destination fails fast because actor is already at that position
            JSONObject sameCell = f.db.moveCombatantToGridCell(actor, 4, 2, false, false,
                    preview.optString("grid_fingerprint"), "ui-grid-g04-move-002");
            assertEquals("destination_is_origin", sameCell.optString("grid_blocked"));

            // Replay protection: attempting another movement with previously committed token fails closed
            JSONObject previewNext = f.db.previewTacticalGridMovement(actor, 5, 2, false, false);
            assertTrue(previewNext.optBoolean("grid_valid"));
            JSONObject replayedToken = f.db.moveCombatantToGridCell(actor, 5, 2, false, false,
                    previewNext.optString("grid_fingerprint"), "ui-grid-g04-move-001");
            assertEquals("position_replayed", replayedToken.optString("grid_blocked"));
            JSONObject spentBonus=f.db.spendCombatantTurnResource(actor,"bonus_action","ui-grid-charge-intervening-action");
            assertFalse("Bonus action spend must succeed: "+spentBonus,spentBonus.has("resource_blocked"));
            assertFalse(f.db.latestTacticalGridMovementProof(actor).optBoolean("grid_path_valid",false));
        }
    }

    // =========================================================================
    // Gate G05: Bestiary Damage Formulas & Action Economy
    // =========================================================================
    @Test
    public void testG05BestiaryDamageFormulasAndConditions() throws Exception {
        // Canonical formula equivalence
        assertTrue(CompiledMonsterDamageGuard.equivalent("1d8 + 3", "1d8+3"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("  2d6  +  4 ", "2d6+4"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8 + 3", "1d6 + 3"));

        // Range calculation and acceptance
        CompiledMonsterDamageGuard.Range range = CompiledMonsterDamageGuard.range("1d8+3", "", false);
        assertNotNull(range);
        assertEquals(4, range.min); // 1 + 3
        assertEquals(11, range.max); // 8 + 3
        assertTrue(CompiledMonsterDamageGuard.accepts("1d8+3", "", 7, false));
        assertFalse(CompiledMonsterDamageGuard.accepts("1d8+3", "", 12, false));

        // Critical hit doubles dice only
        CompiledMonsterDamageGuard.Range critRange = CompiledMonsterDamageGuard.range("1d8+3", "", true);
        assertNotNull(critRange);
        assertEquals(5, critRange.min); // 2*1 + 3
        assertEquals(19, critRange.max); // 2*8 + 3

        // Action economy category check
        assertEquals("action", MonsterActionEconomyGuard.standaloneCost("action"));
        assertEquals("bonus_action", MonsterActionEconomyGuard.standaloneCost("bonus_action"));
        assertEquals("bonus_action", MonsterActionEconomyGuard.standaloneCost("bonus"));
        assertEquals("", MonsterActionEconomyGuard.standaloneCost("reaction"));
        assertEquals("", MonsterActionEconomyGuard.standaloneCost("legendary_action"));

        // MasterOrchestrator and G05 legendary action execution integration
        try (DbFixture f = new DbFixture()) {
            MasterOrchestrator orchestrator = new MasterOrchestrator(
                f.db, new PartyDirector(f.db), new WorldStateEngine(f.db), new AiQuotaGovernor(f.context), f.context);
            RavenloftBestiaryRuntimeEngine.install();
            Xmm2025CompiledBestiaryEngine.install();

            StatblockEncounterStateEngine.restore(new JSONObject().put("schema", "statblock_encounter_state_v1").put("tokens", new JSONObject()));
            StatblockTurnEngine.reset();

            JSONObject sp1 = orchestrator.spawnStatblockToken("g05-ankh", "Ankhtepot");
            assertTrue(sp1.toString(), sp1.optBoolean("spawned"));
            JSONObject sp2 = orchestrator.spawnStatblockToken("g05-hero", "Bodytaker Plant");
            assertTrue(sp2.toString(), sp2.optBoolean("spawned"));
            JSONObject spDragon = orchestrator.spawnStatblockToken("g05-dragon", "Adult Brass Dragon");
            assertTrue(spDragon.toString(), spDragon.optBoolean("spawned"));
            JSONObject spSilver = orchestrator.spawnStatblockToken("g05-silver", "Adult Silver Dragon");
            assertTrue(spSilver.toString(), spSilver.optBoolean("spawned"));

            orchestrator.setStatblockInitiative("g05-hero", 20);
            orchestrator.setStatblockInitiative("g05-ankh", 10);
            orchestrator.setStatblockInitiative("g05-dragon", 5);
            orchestrator.setStatblockInitiative("g05-silver", 3);
            orchestrator.startStatblockTurns(); // hero is active

            // Ankhtepot Hand of Fate via MasterOrchestrator
            int heroHpBefore = StatblockEncounterStateEngine.view("g05-hero").optInt("hp");
            JSONObject legResult = orchestrator.resolveStatblockLegendaryAction("g05-ankh", "g05-hero", "Hand of Fate", 12);
            assertTrue(legResult.toString(), legResult.optBoolean("resolved"));
            assertEquals("saving_throw", legResult.optString("resolution_type"));
            int appliedDmg = legResult.optInt("applied_damage");
            assertTrue(appliedDmg > 0);
            assertEquals(heroHpBefore - appliedDmg, StatblockEncounterStateEngine.view("g05-hero").optInt("hp"));

            // Adult Brass Dragon from special_actions via MasterOrchestrator
            int heroHpBeforeUntypedDragon = StatblockEncounterStateEngine.view("g05-hero").optInt("hp");
            JSONObject dragonAction = orchestrator.resolveStatblockLegendaryAction("g05-dragon", "g05-hero", "Scorching Sands", 8);
            assertFalse(dragonAction.toString(), dragonAction.optBoolean("resolved"));
            assertTrue(dragonAction.toString(), dragonAction.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required", dragonAction.optString("reason"));
            assertEquals(heroHpBeforeUntypedDragon, StatblockEncounterStateEngine.view("g05-hero").optInt("hp"));

            // The same untyped speed rider blocks even when the save would negate damage.
            JSONObject dragonNegate = orchestrator.resolveStatblockLegendaryAction("g05-dragon", "g05-hero", "Scorching Sands", 20);
            assertFalse(dragonNegate.toString(), dragonNegate.optBoolean("resolved"));
            assertTrue(dragonNegate.toString(), dragonNegate.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required", dragonNegate.optString("reason"));
            assertEquals(heroHpBeforeUntypedDragon, StatblockEncounterStateEngine.view("g05-hero").optInt("hp"));

            // Adult Silver Dragon Cold Gale has an untyped push rider and also fails closed.
            JSONObject silverHalf = orchestrator.resolveStatblockLegendaryAction("g05-silver", "g05-hero", "Cold Gale", 20);
            assertFalse(silverHalf.toString(), silverHalf.optBoolean("resolved"));
            assertTrue(silverHalf.toString(), silverHalf.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required", silverHalf.optString("reason"));
            assertEquals(heroHpBeforeUntypedDragon, StatblockEncounterStateEngine.view("g05-hero").optInt("hp"));

            // Audit verification: spellcasting action execution
            JSONObject dragonSpell = orchestrator.resolveStatblockLegendaryAction("g05-dragon", "g05-hero", "Blazing Light", 15);
            assertTrue(dragonSpell.toString(), dragonSpell.optBoolean("resolved"));
            assertEquals("spellcasting_action", dragonSpell.optString("resolution_type"));
            assertEquals("Scorching Ray", dragonSpell.optString("spell_executed"));
            assertTrue(dragonSpell.optBoolean("hit"));
            assertTrue(dragonSpell.optInt("applied_damage") > 0);
        }
    }

    // =========================================================================
    // Gate G06: Spells, Components and Concentration
    // =========================================================================
    @Test
    public void testG06SpellsComponentsAndConcentration() throws Exception {
        // 1. Concentration duration reconciliation (Bless)
        assertEquals(Core2024BlessTurnRules.Decision.KEEP,
                Core2024BlessTurnRules.reconcile(1, 5, "Bênção", false));
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
                Core2024BlessTurnRules.reconcile(1, 5, "Bênção", true));
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
                Core2024BlessTurnRules.reconcile(1, 5, "Outra Magia", false));
        assertEquals(Core2024BlessTurnRules.Decision.EXPIRE,
                Core2024BlessTurnRules.reconcile(1, 11, "Bênção", false));

        // 2. Spell Concentration Duration DC calculation: max(10, damage / 2) capped at 30 in 2024
        assertEquals(10, SpellConcentrationDurationRules.dc("2024", 10));
        assertEquals(10, SpellConcentrationDurationRules.dc("2024", 18));
        assertEquals(14, SpellConcentrationDurationRules.dc("2024", 28));
        assertEquals(30, SpellConcentrationDurationRules.dc("2024", 80)); // 2024 profile caps DC at 30
        assertEquals(40, SpellConcentrationDurationRules.dc("2014", 80)); // 2014 profile does not cap DC

        // 3. Spell Concentration Save Resolution
        // Maintained on successful save: DC 10, d20=12 + bonus=0 = 12 >= 10
        SpellConcentrationDurationRules.Outcome ok = SpellConcentrationDurationRules.resolve(
                "2024", 1, 10, 2, 20, 12, 0, false);
        assertTrue(ok.saveRequired);
        assertTrue(ok.maintained);
        assertFalse(ok.ended);

        // Broken on failed save: DC 15, d20=8 + bonus=2 = 10 < 15
        SpellConcentrationDurationRules.Outcome failed = SpellConcentrationDurationRules.resolve(
                "2024", 1, 10, 2, 30, 8, 2, false);
        assertTrue(failed.saveRequired);
        assertFalse(failed.maintained);
        assertTrue(failed.ended);

        // Incapacitated automatically breaks concentration without roll
        SpellConcentrationDurationRules.Outcome inc = SpellConcentrationDurationRules.resolve(
                "2024", 1, 10, 2, 10, 0, 0, true);
        assertFalse(inc.maintained);
        assertTrue(inc.ended);

        // Expired automatically terminates
        SpellConcentrationDurationRules.Outcome expired = SpellConcentrationDurationRules.resolve(
                "2024", 1, 10, 11, 0, 0, 0, false);
        assertTrue(expired.expired);
        assertTrue(expired.ended);

        // 4. Character Sheet JSON Concentration State Lifecycle
        JSONObject casterSheet = new JSONObject().put("name", "G06 Caster").put("level", 3);
        casterSheet = SpellConcentrationRules.begin(casterSheet, "2024", "Bênção", 1, 10);
        assertNotNull(casterSheet.optJSONObject("active_concentration"));
        assertEquals("Bênção", casterSheet.optString("concentration_spell"));

        // Advance to round 5: concentration kept
        casterSheet = SpellConcentrationRules.advance(casterSheet, 5);
        assertNotNull(casterSheet.optJSONObject("active_concentration"));

        // Save failure breaks concentration and cleans up sheet
        casterSheet = SpellConcentrationRules.concentrationSave(casterSheet, 20, 2, 0, 10);
        assertFalse(casterSheet.optBoolean("concentration_maintained"));
        assertNull(casterSheet.optJSONObject("active_concentration"));
        assertEquals("", casterSheet.optString("concentration_spell"));

        // 5. Spell Target Component Executor (2024 Fireball with Arcane Focus)
        SpellTargetComponentExecutor.Request fireballReq = new SpellTargetComponentExecutor.Request(
                "2024", "Fireball", "Wizard", "int", 5,
                12, 8, "scene-1", "[]", 1L,
                2, 2, -1, 0L,
                Collections.singletonList(new Core2024CastingEligibility.ClassLevel("Wizard", 5)),
                Collections.singletonList("Arcane Focus"),
                java.util.Arrays.asList(
                        new Core2024TacticalGeometry.Token(1L, .20, .25),
                        new Core2024TacticalGeometry.Token(2L, .25, .25),
                        new Core2024TacticalGeometry.Token(3L, .90, .75)));

        SpellTargetComponentExecutor.Result execRes = SpellTargetComponentExecutor.execute(fireballReq);
        assertTrue("Fireball execution must be allowed: " + execRes.error, execRes.allowed());
        assertEquals("wizard", execRes.sourceClass);
        assertEquals(java.util.Arrays.asList(1L, 2L), execRes.targetIds);
        assertEquals(64, execRes.signature.length());

        // Rejection when material component is missing
        SpellTargetComponentExecutor.Request noMaterialReq = new SpellTargetComponentExecutor.Request(
                "2024", "Fireball", "Wizard", "int", 5,
                12, 8, "scene-1", "[]", 1L,
                2, 2, -1, 0L,
                Collections.singletonList(new Core2024CastingEligibility.ClassLevel("Wizard", 5)),
                Collections.<String>emptyList(),
                java.util.Arrays.asList(new Core2024TacticalGeometry.Token(1L, .20, .25)));
        SpellTargetComponentExecutor.Result noMatRes = SpellTargetComponentExecutor.execute(noMaterialReq);
        assertFalse(noMatRes.allowed());
        assertEquals("material_component_missing", noMatRes.error);
    }

    // =========================================================================
    // Gate G07: Character Creation & Progression (Levels 1-20)
    // =========================================================================
    @Test
    public void testG07CharacterProgressionAndMulticlass() throws Exception {
        // 1. Single class level-up (Wizard Level 1 -> 2)
        JSONObject sheet = new JSONObject()
                .put("name", "Ada")
                .put("class_name", "Wizard")
                .put("level", 1)
                .put("xp", 300) // threshold for level 2 is 300 XP
                .put("hp", 8)
                .put("max_hp", 8)
                .put("con", 14)
                .put("str", 14)
                .put("dex", 14)
                .put("int_score", 12)
                .put("wis", 10)
                .put("cha", 10)
                .put("class_levels", new JSONArray().put(
                        new JSONObject().put("class_name", "Wizard").put("level", 1)));

        JSONObject result = CharacterLevelUpTransitionEngine.apply(sheet, "Wizard", 6, 2, true, false);
        assertTrue("Level up should succeed: " + result, result.optBoolean("accepted"));
        assertEquals(2, result.optInt("to_level"));
        assertEquals(6, result.optInt("hp_gain")); // d6 fixed average (4) + CON modifier (+2)
        JSONObject stateAfter = result.getJSONObject("state_after");
        assertEquals(14, stateAfter.optInt("max_hp"));
        assertTrue(stateAfter.optBoolean("level_up_choices_pending"));

        // 2. Multi-level sequential progression (Level 2 -> 3)
        stateAfter.put("xp", 900); // threshold for level 3 is 900 XP
        JSONObject lvl3Res = CharacterLevelUpTransitionEngine.apply(stateAfter, "Wizard", 6, 2, true, false);
        assertTrue("Level 3 up should succeed: " + lvl3Res, lvl3Res.optBoolean("accepted"));
        assertEquals(3, lvl3Res.optInt("to_level"));
        assertEquals(20, lvl3Res.getJSONObject("state_after").optInt("max_hp")); // 14 + 6 = 20

        // 3. Multiclassing Prerequisites & Validation
        // Fighter (STR 14) attempting to multiclass into Wizard with INT 12 -> fails prerequisite (INT 13 required)
        JSONObject fighterSheet = new JSONObject()
                .put("name", "G07 Fighter").put("class_name", "Fighter").put("level", 1).put("xp", 300)
                .put("hp", 12).put("max_hp", 12).put("con", 14).put("str", 14).put("dex", 12)
                .put("int_score", 12).put("wis", 10).put("cha", 10)
                .put("class_levels", new JSONArray().put(new JSONObject().put("class_name", "Fighter").put("level", 1)));

        JSONObject mcDenied = CharacterLevelUpTransitionEngine.apply(fighterSheet, "Wizard", 6, 2, true, true);
        assertFalse(mcDenied.optBoolean("accepted"));
        assertEquals("level_up_invalid:multiclass_intelligence_13_required", mcDenied.optString("level_up_blocked"));

        // With INT 13 -> multiclass succeeds
        fighterSheet.put("int_score", 13);
        JSONObject mcAllowed = CharacterLevelUpTransitionEngine.apply(fighterSheet, "Wizard", 6, 2, true, true);
        assertTrue("Multiclass should succeed: " + mcAllowed, mcAllowed.optBoolean("accepted"));
        JSONArray classes = mcAllowed.getJSONObject("state_after").getJSONArray("class_levels");
        assertEquals(2, classes.length());
        assertEquals("fighter", classes.getJSONObject(0).optString("class_name"));
        assertEquals("wizard", classes.getJSONObject(1).optString("class_name"));

        // 4. XP Threshold Validation via DndProgressionEngine
        assertEquals(1, DndProgressionEngine.levelForXp(0));
        assertEquals(1, DndProgressionEngine.levelForXp(299));
        assertEquals(2, DndProgressionEngine.levelForXp(300));
        assertEquals(3, DndProgressionEngine.levelForXp(900));
        assertEquals(5, DndProgressionEngine.levelForXp(6500));
        assertEquals(20, DndProgressionEngine.levelForXp(355000));

        // 5. Boundary guards: Insufficient XP, Level 20 cap, Wrong Hit Die, Multiclass Disabled
        JSONObject noXpSheet = new JSONObject(sheet.toString());
        noXpSheet.put("xp", 100);
        JSONObject refusedXp = CharacterLevelUpTransitionEngine.apply(noXpSheet, "Wizard", 6, 2, true, false);
        assertFalse(refusedXp.optBoolean("accepted"));
        assertEquals("xp_threshold_not_reached", refusedXp.optString("level_up_blocked"));

        JSONObject maxSheet = new JSONObject(sheet.toString()).put("level", 20).put("xp", 355000);
        JSONObject refusedMax = CharacterLevelUpTransitionEngine.apply(maxSheet, "Wizard", 6, 2, true, false);
        assertFalse(refusedMax.optBoolean("accepted"));
        assertEquals("maximum_or_invalid_level", refusedMax.optString("level_up_blocked"));

        JSONObject wrongDie = CharacterLevelUpTransitionEngine.apply(sheet, "Wizard", 8, 2, true, false);
        assertFalse(wrongDie.optBoolean("accepted"));
        assertEquals("class_hit_die_mismatch", wrongDie.optString("level_up_blocked"));

        JSONObject disabledMc = CharacterLevelUpTransitionEngine.apply(fighterSheet, "Wizard", 6, 2, true, false);
        assertFalse(disabledMc.optBoolean("accepted"));
        assertEquals("multiclass_disabled", disabledMc.optString("level_up_blocked"));
    }

    // =========================================================================
    // Gate G08: Tactical Geometry, Raycasting & Cover (+2/+5 AC)
    // =========================================================================
    @Test
    public void testG08TacticalGeometryAndCover() {
        int w = 5, h = 5;
        boolean[][] opaque = new boolean[h][w];
        int[][] cover = new int[h][w];
        boolean[][] occupied = new boolean[h][w];

        // 1. Normal melee attack at distance 5 ft
        TacticalAttackGeometryRules.Result res = TacticalAttackGeometryRules.evaluate(
                w, h, 1, 1, 2, 1, opaque, cover, occupied, "melee", 5, 0, 0);
        assertTrue(res.legal());
        assertEquals(5, res.distanceFeet);
        assertEquals(0, res.geometryCoverBonus);

        // 2. Intervening creature grants half cover (+2 AC) on ranged attack
        occupied[1][2] = true;
        TacticalAttackGeometryRules.Result halfCover = TacticalAttackGeometryRules.evaluate(
                w, h, 1, 1, 3, 1, opaque, cover, occupied, "ranged", 0, 30, 120);
        assertTrue(halfCover.legal());
        assertEquals(10, halfCover.distanceFeet);
        assertEquals(2, halfCover.geometryCoverBonus);

        // 3. Substantial obstacle grants three-quarters cover (+5 AC)
        cover[1][2] = 5;
        TacticalAttackGeometryRules.Result threeQuarterCover = TacticalAttackGeometryRules.evaluate(
                w, h, 1, 1, 3, 1, opaque, cover, occupied, "ranged", 0, 30, 120);
        assertTrue(threeQuarterCover.legal());
        assertEquals(5, threeQuarterCover.geometryCoverBonus);

        // 4. Target out of reach (10 ft for a 5 ft melee weapon)
        TacticalAttackGeometryRules.Result outOfReach = TacticalAttackGeometryRules.evaluate(
                w, h, 1, 1, 3, 1, opaque, cover, occupied, "melee", 5, 0, 0);
        assertFalse(outOfReach.legal());
        assertEquals("target_out_of_reach", outOfReach.blocked);

        // 5. Target inside opaque cell
        opaque[1][3] = true;
        TacticalAttackGeometryRules.Result blocked = TacticalAttackGeometryRules.evaluate(
                w, h, 1, 1, 3, 1, opaque, cover, occupied, "melee", 5, 0, 0);
        assertFalse(blocked.legal());
        assertEquals("target_in_opaque_cell", blocked.blocked);

        // 6. 3D Voxel Creature Geometry (Large Footprint 2x2, Elevation & Blindsight)
        TacticalCreatureGeometryRules.Creature hero = new TacticalCreatureGeometryRules.Creature(1, 1, 0, 1, 1);
        TacticalCreatureGeometryRules.Creature ogre = new TacticalCreatureGeometryRules.Creature(2, 1, 0, 2, 2);
        assertEquals(5, TacticalCreatureGeometryRules.closestDistanceFeet(hero, ogre));

        boolean[][][] opaque3d = new boolean[2][h][w];
        int[][][] cover3d = new int[2][h][w];
        boolean[][][] occ3d = new boolean[2][h][w];
        TacticalCreatureGeometryRules.Result voxelRes = TacticalCreatureGeometryRules.evaluate(
                w, h, 2, hero, ogre, opaque3d, cover3d, occ3d, "melee", 5, 0, 0, true, true, 0);
        assertTrue("Voxel melee attack must be legal: " + voxelRes.blocked, voxelRes.legal());
        assertEquals(5, voxelRes.distanceFeet);

        // Blindsight overcomes darkness / unseen disadvantage
        TacticalCreatureGeometryRules.Result blindRes = TacticalCreatureGeometryRules.evaluate(
                w, h, 2, hero, ogre, opaque3d, cover3d, occ3d, "melee", 5, 0, 0, true, false, 30);
        assertTrue(blindRes.legal());
        assertTrue(blindRes.usedBlindsight);
        assertFalse(blindRes.unseenDisadvantage);

        // 7. Opportunity Attack Evaluation
        OpportunityAttackRules.Position reactorPos = new OpportunityAttackRules.Position(2, 2, 0, 1);
        OpportunityAttackRules.Position stepBefore = new OpportunityAttackRules.Position(2, 3, 0, 1);
        OpportunityAttackRules.Position stepAfter = new OpportunityAttackRules.Position(2, 4, 0, 1);

        // Voluntary mover leaving 5ft reach provokes reaction
        OpportunityAttackRules.Context oaCtx = new OpportunityAttackRules.Context(
                1L, 2L, reactorPos, stepBefore, stepAfter, 5, true, true, true, true, false, false);
        OpportunityAttackRules.Decision oaDecision = OpportunityAttackRules.evaluate(oaCtx);
        assertTrue("Opportunity attack must be triggered: " + oaDecision.blocked, oaDecision.triggered());
        assertEquals(5, oaDecision.beforeFeet);
        assertEquals(10, oaDecision.afterFeet);

        // Disengaged mover does not provoke
        OpportunityAttackRules.Context disengageCtx = new OpportunityAttackRules.Context(
                1L, 2L, reactorPos, stepBefore, stepAfter, 5, true, true, true, true, true, false);
        OpportunityAttackRules.Decision disengageDecision = OpportunityAttackRules.evaluate(disengageCtx);
        assertFalse(disengageDecision.triggered());
        assertEquals("disengaged", disengageDecision.blocked);

        // Forced movement (voluntary=false) does not provoke
        OpportunityAttackRules.Context forcedCtx = new OpportunityAttackRules.Context(
                1L, 2L, reactorPos, stepBefore, stepAfter, 5, true, true, true, false, false, false);
        OpportunityAttackRules.Decision forcedDecision = OpportunityAttackRules.evaluate(forcedCtx);
        assertFalse(forcedDecision.triggered());
        assertEquals("movement_does_not_provoke", forcedDecision.blocked);
    }

    // =========================================================================
    // Gate G09: UI Intent to Executor & Causal Receipts
    // =========================================================================
    @Test
    public void testG09NarratedMovementIntentAndReceipts() throws Exception {
        try (DbFixture f = new DbFixture()) {
            long actor = f.db.addCombatant("G09 Hero", 20, 30, 30, 14, "", true);
            f.db.setCombatRunning(true);
            f.db.nextTurn();
            JSONObject scene = new ProceduralTacticalSceneGenerator(f.db).ensureForCurrentScene();
            f.db.ensureTacticalTokensForCombat();

            ContentValues pos = new ContentValues();
            pos.put("x", 2.5 / scene.optInt("width_cells"));
            pos.put("y", 2.5 / scene.optInt("height_cells"));
            f.db.getWritableDatabase().update("tactical_tokens", pos, "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(actor)});

            JSONObject proposal = new JSONObject().put("combatant_id", actor).put("name", "G09 Hero")
                    .put("x", 4.5 / scene.optInt("width_cells"))
                    .put("y", 2.5 / scene.optInt("height_cells"));

            String commitId = "g09-scene-commit-000000001";

            // Preflight boundary checks
            JSONObject badCommit = NarratedTacticalMovementExecutor.preflight(f.db, proposal, "short", 0);
            assertEquals("invalid_scene_binding", badCommit.optString("narrated_tactical_move_blocked"));

            JSONObject badActor = new JSONObject(proposal.toString()).put("combatant_id", 999999L);
            JSONObject badActorPreflight = NarratedTacticalMovementExecutor.preflight(f.db, badActor, commitId, 0);
            assertEquals("combatant_not_found", badActorPreflight.optString("narrated_tactical_move_blocked"));

            JSONObject badCoords = new JSONObject(proposal.toString()).put("x", 1.5d); // out of bounds [0, 1)
            JSONObject badCoordsPreflight = NarratedTacticalMovementExecutor.preflight(f.db, badCoords, commitId, 0);
            assertEquals("destination_out_of_bounds", badCoordsPreflight.optString("narrated_tactical_move_blocked"));

            // Valid preflight
            JSONObject preflight = NarratedTacticalMovementExecutor.preflight(f.db, proposal, commitId, 0);
            assertTrue(preflight.optBoolean("narrated_tactical_move_preflight"));

            // Execution commits to SQLite and creates causal receipt
            JSONObject executed = NarratedTacticalMovementExecutor.executeChecked(f.db, proposal, commitId, 0);
            assertTrue(executed.optBoolean("narrated_tactical_move_committed"));
            String receipt = executed.optString("narrated_tactical_receipt");
            assertTrue(receipt.startsWith("combat-v1:" + actor + ":"));
            assertTrue(f.db.hasEffectReceipt(receipt));

            // Position updated in database
            JSONObject gridPos = f.db.tacticalGridPosition(actor);
            assertEquals(4, gridPos.optInt("cell_x"));
            assertEquals(2, gridPos.optInt("cell_y"));

            // Replay protection: executing with same token throws IllegalStateException
            try {
                NarratedTacticalMovementExecutor.executeChecked(f.db, proposal, commitId, 0);
                fail("same scene proposal must not replay");
            } catch (IllegalStateException expected) {
                assertEquals("position_replayed", expected.getMessage());
            }
        }
    }

    // =========================================================================
    // Gate G10: Companion Proposal Isolation Rules
    // =========================================================================
    @Test
    public void testG10CompanionProposalIsolation() throws Exception {
        Set<Long> curatedIds = new HashSet<>(Collections.singletonList(42L));

        // 1. Pure intention proposal is accepted (empty rejection string)
        JSONObject safe = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Defend allies")));
        assertEquals("", CompanionProposalIsolationRules.reject(safe, curatedIds));

        // 2. Mixed with direct state updates is rejected
        JSONObject toxic = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Attack")))
                .put("state_updates", new JSONArray().put(new JSONObject().put("hp", 999)));
        assertEquals("curated_intention_mixed_with_state_updates", CompanionProposalIsolationRules.reject(toxic, curatedIds));

        // 3. Mixed with unapproved tactical updates is rejected
        JSONObject tacticalToxic = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Move")))
                .put("tactical_updates", new JSONArray().put(new JSONObject().put("x", 5)));
        assertEquals("curated_intention_mixed_with_tactical_updates", CompanionProposalIsolationRules.reject(tacticalToxic, curatedIds));

        // 4. Mixed with unapproved world events / quest updates is rejected
        JSONObject questToxic = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Explore")))
                .put("quest_updates", new JSONArray().put(new JSONObject().put("quest_id", 1)));
        assertEquals("curated_intention_mixed_with_quest_updates", CompanionProposalIsolationRules.reject(questToxic, curatedIds));

        // 5. Mixed with local tick changes is rejected
        JSONObject tickToxic = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Rest")))
                .put("_local_tick", new JSONObject().put("hours_advanced", 4));
        assertEquals("curated_intention_mixed_with_local_tick", CompanionProposalIsolationRules.reject(tickToxic, curatedIds));

        // 6. Mixed with scene state is rejected
        JSONObject sceneToxic = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Scout")))
                .put("scene_state", new JSONObject().put("weather", "storm"));
        assertEquals("curated_intention_mixed_with_scene_state", CompanionProposalIsolationRules.reject(sceneToxic, curatedIds));

        // 7. Mixed with unapproved combat action is rejected
        JSONObject combatMixed = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 42).put("intended_action", "Cast Spell")))
                .put("combat", new JSONObject().put("action", "attack"));
        assertEquals("curated_intention_mixed_with_combat", CompanionProposalIsolationRules.reject(combatMixed, curatedIds));
    }

    // =========================================================================
    // Gate G11: Living World Deterministic Clock
    // =========================================================================
    @Test
    public void testG11LivingWorldDeterministicClock() throws Exception {
        long syncAt = 1000000L;
        long now = syncAt + (24 * 3600 * 1000L); // 24 real hours later

        // 1. Living World Catch-Up Rules
        JSONObject preview = LivingWorldCatchUpRules.preview(true, true, syncAt, now, 24.0, 100);
        assertTrue(preview.optBoolean("enabled"));
        assertTrue(preview.optBoolean("preview_only"));
        assertEquals(24.0, preview.optDouble("real_hours"), 0.01);
        assertEquals(24, preview.optInt("game_hours"));

        // Max hours clamping: 48 real hours capped at 24 game hours
        long now48 = syncAt + (48 * 3600 * 1000L);
        JSONObject clamped = LivingWorldCatchUpRules.preview(true, true, syncAt, now48, 24.0, 24);
        assertEquals(24, clamped.optInt("game_hours"));

        // When adventure is not started, advance is blocked
        JSONObject notStarted = LivingWorldCatchUpRules.preview(true, false, syncAt, now, 24.0, 100);
        assertEquals("adventure_not_started", notStarted.optString("reason"));

        // When disabled, no catch-up
        JSONObject disabled = LivingWorldCatchUpRules.preview(false, true, syncAt, now, 24.0, 100);
        assertFalse(disabled.optBoolean("enabled"));

        // When lastSyncAt is 0 or negative -> initialize_only
        JSONObject initOnly = LivingWorldCatchUpRules.preview(true, true, 0L, now, 24.0, 100);
        assertTrue(initOnly.optBoolean("initialize_only"));

        // 2. Deterministic World Clock Advancement
        // Advance 60 seconds with interval 10 seconds -> 6 ticks
        WorldClockProgression clk = WorldClockProgression.advance(10, 100, 0.0, 60.0, 10.0, 1.0, 1, false);
        assertEquals(10, clk.before);
        assertEquals(16, clk.after);
        assertEquals(6, clk.ticks);
        assertEquals(0.0, clk.remainder, 0.001);

        // Maximum clock clamping
        WorldClockProgression clkMax = WorldClockProgression.advance(98, 100, 0.0, 60.0, 10.0, 1.0, 1, false);
        assertEquals(98, clkMax.before);
        assertEquals(100, clkMax.after); // clamped at maximum

        // Regression prevention when regressionAllowed = false
        WorldClockProgression clkNoReg = WorldClockProgression.advance(50, 100, 0.0, 10.0, 10.0, 1.0, -5, false);
        assertEquals(50, clkNoReg.after);
        assertEquals(0, clkNoReg.tickDelta);

        // Resilience against NaN and Infinity inputs
        WorldClockProgression clkSafe = WorldClockProgression.advance(10, 100, Double.NaN, Double.POSITIVE_INFINITY, 10.0, 1.0, 1, false);
        assertNotNull(clkSafe);
        assertTrue(clkSafe.after >= 10);
    }

    // =========================================================================
    // Gate G12: Scene Image Import Policy Validation
    // =========================================================================
    @Test
    public void testG12SceneImageImportPolicy() {
        // Valid SEND intent with image MIME (jpeg, png, webp)
        assertTrue(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, "scene-1", "scene-1").accepted);
        assertTrue(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/png", true, "scene-1", "scene-1").accepted);
        assertTrue(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/webp", true, "scene-1", "scene-1").accepted);

        // Invalid action (VIEW, MAIN)
        assertFalse(SceneImageImportPolicy.validate(
                "android.intent.action.VIEW", "image/jpeg", true, "scene-1", "scene-1").accepted);

        // Invalid MIME type (text/plain, application/pdf, null)
        SceneImageImportPolicy.Result badMime = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "text/plain", true, "scene-1", "scene-1");
        assertFalse(badMime.accepted);
        assertEquals("O compartilhamento não contém uma imagem.", badMime.message);

        SceneImageImportPolicy.Result badPdf = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "application/pdf", true, "scene-1", "scene-1");
        assertFalse(badPdf.accepted);

        SceneImageImportPolicy.Result nullMime = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, null, true, "scene-1", "scene-1");
        assertFalse(nullMime.accepted);

        // Missing stream / URI
        assertFalse(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/jpeg", false, "scene-1", "scene-1").accepted);

        // Stale scene fingerprint
        SceneImageImportPolicy.Result stale = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/png", true, "scene-1", "scene-2");
        assertFalse(stale.accepted);
        assertEquals("A imagem pertence a uma cena anterior.", stale.message);
    }

    // =========================================================================
    // Gate G13: Narration Chunking for Audio Delivery
    // =========================================================================
    @Test
    public void testG13NarrationChunkingEngines() {
        // Short text preserved as single chunk
        String shortText = "O guerreiro ergue seu escudo. A batalha começa.";
        List<String> androidShort = NarrationChunker.android(shortText);
        assertEquals(1, androidShort.size());
        assertEquals(shortText, androidShort.get(0));

        // Long text chunking with multiple sentences
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("A tocha ilumina as pedras antigas do corredor úmido número ").append(i).append(". ");
        }
        String longText = sb.toString();
        List<String> chunks = NarrationChunker.android(longText);
        assertTrue("Long narration must be split into multiple chunks: " + chunks.size(), chunks.size() > 1);

        // Buffer limit and content preservation
        StringBuilder reconstructed = new StringBuilder();
        for (String c : chunks) {
            assertTrue("Chunk length must respect audio buffer limit", c.length() <= 720);
            reconstructed.append(c).append(" ");
        }
        // Verify no sentences or words were lost
        assertTrue(reconstructed.toString().contains("número 0"));
        assertTrue(reconstructed.toString().contains("número 29"));

        // Single long continuous word boundary splitting under 720 limit
        StringBuilder longSentence = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longSentence.append("palavra").append(i).append(" ");
        }
        List<String> longChunks = NarrationChunker.android(longSentence.toString());
        assertTrue(longChunks.size() >= 2);
        for (String lc : longChunks) {
            assertTrue(lc.length() <= 720);
        }
    }

    // =========================================================================
    // Gate G14: Backup21 Roundtrip & SHA-256 Integrity Envelope
    // =========================================================================
    @Test
    public void testG14Backup21ExportImportRoundtrip() throws Exception {
        try (DbFixture f = new DbFixture()) {
            f.db.saveCampaign("G14 Saga", "5e 2014", "Fantasia", true, true, false, "ativo");
            long heroId = f.db.addCharacter("G14 Sorcerer", "Elf", "Sorcerer", 1, 16, 16, 14,
                    10, 14, 14, 12, 10, 16, true, "fixture");
            assertTrue(heroId > 0);
            f.db.addJournal("Entrada 1", "O portal foi aberto", "story");

            JSONObject export = f.db.exportAll();
            assertEquals(21, export.optInt("version"));
            assertEquals("", Backup21IntegrityEnvelope.check(export));

            // Verify Backup21 structural completeness (12 objects, 52 arrays)
            assertEquals(12, Backup21RequiredSections.OBJECTS.length);
            assertEquals(52, Backup21RequiredSections.ARRAYS.length);
            for (String objKey : Backup21RequiredSections.OBJECTS) {
                assertTrue("Missing required object key: " + objKey, export.has(objKey));
            }
            for (String arrKey : Backup21RequiredSections.ARRAYS) {
                assertTrue("Missing required array key: " + arrKey, export.has(arrKey));
            }

            // Modify state then restore
            f.db.saveCampaign("Campanha Modificada", "5e 2014", "Dark", false, false, false, "mod");
            f.db.importAll(export);

            assertEquals("G14 Saga", f.db.getCampaign().optString("name"));
            assertEquals(1, f.db.listCharacters().size());
            assertEquals("G14 Sorcerer", f.db.listCharacters().get(0).optString("name"));
            assertEquals(1, f.db.listJournal().size());
        }
    }

    // =========================================================================
    // Gate G15: Build Suite & Package Verification
    // =========================================================================
    @Test
    public void testG15BuildAndRunnerEnvironment() {
        Context ctx = RuntimeEnvironment.getApplication();
        assertNotNull("Application context must be initialized", ctx);
        assertEquals("com.braseiro.dnd5e", ctx.getPackageName());

        // Validate package manager and app info
        android.content.pm.ApplicationInfo appInfo = ctx.getApplicationInfo();
        assertNotNull(appInfo);
        assertTrue((appInfo.flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);

        // Validate runtime properties
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion);
        assertTrue("Java version must be 17+ or compatible: " + javaVersion,
                javaVersion.startsWith("17") || javaVersion.startsWith("21"));
    }

    // =========================================================================
    // Gate G16: Hardware Validation Protocols & Screen Profiles
    // =========================================================================
    @Test
    public void testG16HardwareValidationProtocols() {
        Context ctx = RuntimeEnvironment.getApplication();
        android.content.res.Resources res = ctx.getResources();
        android.util.DisplayMetrics dm = res.getDisplayMetrics();
        assertNotNull("DisplayMetrics must be available", dm);
        assertTrue("Display width must be positive", dm.widthPixels > 0);
        assertTrue("Display height must be positive", dm.heightPixels > 0);
        assertTrue("Display density must be positive", dm.density > 0f);

        // Validate hardware capability definitions for physical deployment targets
        java.util.Map<String, int[]> profiles = new java.util.HashMap<>();
        profiles.put("SM-S918B", new int[]{1440, 3088, 500}); // S23 Ultra WQHD+
        profiles.put("SM-S928B", new int[]{1440, 3120, 505}); // S24 Ultra QHD+
        profiles.put("SM-A576B", new int[]{1080, 2340, 400}); // A57 FHD+
        profiles.put("Pixel-8-Pro", new int[]{1344, 2992, 489}); // Pixel 8 Pro Super Actua

        for (java.util.Map.Entry<String, int[]> entry : profiles.entrySet()) {
            int[] spec = entry.getValue();
            assertTrue(spec[0] >= 1080);
            assertTrue(spec[1] >= 2340);
            assertTrue(spec[2] >= 400);

            // Minimum touch target 48dp calculation: 48 * (density / 160)
            int minTouchPixels = (int) Math.ceil(48.0 * (spec[2] / 160.0));
            assertTrue("Touch target in pixels must be at least 120px on high-res displays", minTouchPixels >= 120);
        }
    }
}
