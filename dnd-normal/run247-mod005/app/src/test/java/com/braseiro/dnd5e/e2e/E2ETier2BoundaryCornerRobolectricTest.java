package com.braseiro.dnd5e.e2e;

import com.braseiro.dnd5e.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tier 2: Boundary & Corner Cases E2E Suite.
 * Validates boundary limits, extreme inputs, null handling,
 * and algorithmic corner cases across the core engine subsystems.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class E2ETier2BoundaryCornerRobolectricTest {

    // =========================================================================
    // 1. Damage Formula Boundaries
    // =========================================================================
    @Test
    public void testDamageFormulaBoundaries() {
        // Flat constant only (0 dice)
        CompiledMonsterDamageGuard.Range flat = CompiledMonsterDamageGuard.range("5", "", false);
        assertNotNull(flat);
        assertEquals(5, flat.min);
        assertEquals(5, flat.max);

        // Max allowed dice count (200 dice)
        CompiledMonsterDamageGuard.Range maxDice = CompiledMonsterDamageGuard.range("200d6", "", false);
        assertNotNull(maxDice);
        assertEquals(200, maxDice.min);
        assertEquals(1200, maxDice.max);

        // Max allowed die faces (1000 faces)
        CompiledMonsterDamageGuard.Range maxFaces = CompiledMonsterDamageGuard.range("1d1000", "", false);
        assertNotNull(maxFaces);
        assertEquals(1, maxFaces.min);
        assertEquals(1000, maxFaces.max);

        // Excess dice count (> 200) rejected
        assertNull(CompiledMonsterDamageGuard.range("201d6", "", false));

        // Excess die faces (> 1000) rejected
        assertNull(CompiledMonsterDamageGuard.range("1d1001", "", false));

        // Formula exceeding 100,000 threshold rejected
        assertNull(CompiledMonsterDamageGuard.range("100001", "", false));

        // Null formula handles gracefully
        assertEquals("", CompiledMonsterDamageGuard.canonical(null));
        assertFalse(CompiledMonsterDamageGuard.equivalent(null, "1d8"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8", null));
    }

    // =========================================================================
    // 2. Damage Mitigation Boundaries
    // =========================================================================
    @Test
    public void testDamageMitigationBoundaries() {
        // Negative raw damage clamped to 0
        assertEquals(0, DamageMitigationRules.apply(-10, false, false, false));
        assertEquals(0, DamageMitigationRules.apply(-10, false, false, true));

        // Zero damage
        assertEquals(0, DamageMitigationRules.apply(0, false, false, false));
        assertEquals(0, DamageMitigationRules.apply(0, true, true, true));

        // Odd number resistance floor division
        assertEquals(0, DamageMitigationRules.apply(1, false, true, false)); // 1 / 2 = 0
        assertEquals(1, DamageMitigationRules.apply(3, false, true, false)); // 3 / 2 = 1
        assertEquals(2, DamageMitigationRules.apply(5, false, true, false)); // 5 / 2 = 2

        // Resistance + Vulnerability on odd numbers: (n / 2) * 2
        assertEquals(0, DamageMitigationRules.apply(1, false, true, true));
        assertEquals(2, DamageMitigationRules.apply(3, false, true, true));
        assertEquals(4, DamageMitigationRules.apply(5, false, true, true));

        // Extreme value integer overflow protection
        int nearMax = Integer.MAX_VALUE - 10;
        int vulnerable = DamageMitigationRules.apply(nearMax, false, false, true);
        assertEquals(Integer.MAX_VALUE, vulnerable);
    }

    // =========================================================================
    // 3. Tactical Movement Cost Boundaries
    // =========================================================================
    @Test
    public void testTacticalMovementDistanceBoundaries() {
        // Minimum valid distance (1 ft)
        assertEquals(1, TacticalMovementCostRules.costFeet(1, false, false));
        assertEquals(2, TacticalMovementCostRules.costFeet(1, true, false));
        assertEquals(3, TacticalMovementCostRules.costFeet(1, true, true));

        // Maximum valid distance (600 ft)
        assertEquals(600, TacticalMovementCostRules.costFeet(600, false, false));
        assertEquals(1200, TacticalMovementCostRules.costFeet(600, true, false));
        assertEquals(1800, TacticalMovementCostRules.costFeet(600, true, true));

        // Distance 0 rejected
        try {
            TacticalMovementCostRules.costFeet(0, false, false);
            fail("Expected exception for 0 ft");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid_tactical_distance", e.getMessage());
        }

        // Negative distance rejected
        try {
            TacticalMovementCostRules.costFeet(-1, false, false);
            fail("Expected exception for negative distance");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid_tactical_distance", e.getMessage());
        }

        // Distance > 600 rejected
        try {
            TacticalMovementCostRules.costFeet(601, false, false);
            fail("Expected exception for 601 ft");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid_tactical_distance", e.getMessage());
        }
    }

    // =========================================================================
    // 4. Attack Geometry & Grid Boundaries
    // =========================================================================
    @Test
    public void testAttackGeometryBoundaries() {
        int w = 5, h = 5;
        boolean[][] opaque = new boolean[h][w];
        int[][] cover = new int[h][w];
        boolean[][] occupied = new boolean[h][w];

        // Self-attack (ax == tx && ay == ty) rejected
        TacticalAttackGeometryRules.Result self = TacticalAttackGeometryRules.evaluate(
                w, h, 2, 2, 2, 2, opaque, cover, occupied, "melee", 5, 0, 0);
        assertFalse(self.legal());
        assertEquals("invalid_attack_grid_or_weapon", self.blocked);

        // Out of bounds coordinates (< 0 or >= w)
        TacticalAttackGeometryRules.Result outBounds = TacticalAttackGeometryRules.evaluate(
                w, h, -1, 0, 2, 2, opaque, cover, occupied, "melee", 5, 0, 0);
        assertFalse(outBounds.legal());
        assertEquals("invalid_attack_grid_or_weapon", outBounds.blocked);

        // Grid dimension boundary (100 is max, 101 is invalid)
        TacticalAttackGeometryRules.Result excessGrid = TacticalAttackGeometryRules.evaluate(
                101, 101, 0, 0, 1, 1, opaque, cover, occupied, "melee", 5, 0, 0);
        assertFalse(excessGrid.legal());
        assertEquals("invalid_attack_grid_or_weapon", excessGrid.blocked);

        // Ranged weapon at normal range boundary vs long range boundary
        int rw = 35, rh = 5;
        boolean[][] ropaque = new boolean[rh][rw];
        int[][] rcover = new int[rh][rw];
        boolean[][] roccupied = new boolean[rh][rw];

        // Normal range (150 ft = 30 cells away)
        TacticalAttackGeometryRules.Result normalRanged = TacticalAttackGeometryRules.evaluate(
                rw, rh, 0, 2, 30, 2, ropaque, rcover, roccupied, "ranged", 0, 150, 600);
        assertTrue(normalRanged.legal());
        assertEquals(150, normalRanged.distanceFeet);
        assertFalse(normalRanged.longRangeDisadvantage);

        // Long range (31 cells away = 155 ft -> disadvantage)
        TacticalAttackGeometryRules.Result longRanged = TacticalAttackGeometryRules.evaluate(
                rw, rh, 0, 2, 31, 2, ropaque, rcover, roccupied, "ranged", 0, 150, 600);
        assertTrue(longRanged.legal());
        assertEquals(155, longRanged.distanceFeet);
        assertTrue(longRanged.longRangeDisadvantage);
    }

    // =========================================================================
    // 5. Character Progression Boundaries
    // =========================================================================
    @Test
    public void testCharacterProgressionBoundaries() throws Exception {
        // Null sheet
        JSONObject nullRes = CharacterLevelUpTransitionEngine.apply(null, "Fighter", 10, 2, true, false);
        assertFalse(nullRes.optBoolean("accepted"));
        assertEquals("character_sheet_required", nullRes.optString("level_up_blocked"));

        // Level < 1
        JSONObject invalidLevel = new JSONObject().put("level", 0);
        JSONObject res0 = CharacterLevelUpTransitionEngine.apply(invalidLevel, "Fighter", 10, 2, true, false);
        assertFalse(res0.optBoolean("accepted"));
        assertEquals("maximum_or_invalid_level", res0.optString("level_up_blocked"));

        // Level >= 20 max level boundary
        JSONObject maxLevel = new JSONObject().put("level", 20);
        JSONObject res20 = CharacterLevelUpTransitionEngine.apply(maxLevel, "Fighter", 10, 2, true, false);
        assertFalse(res20.optBoolean("accepted"));
        assertEquals("maximum_or_invalid_level", res20.optString("level_up_blocked"));

        // XP Boundary: 299 XP (1 XP short of 300)
        JSONObject sheet299 = new JSONObject()
                .put("name", "BoundaryHero")
                .put("class_name", "Fighter")
                .put("level", 1)
                .put("xp", 299)
                .put("hp", 10).put("max_hp", 10)
                .put("con", 14).put("str", 15).put("dex", 10).put("int_score", 10).put("wis", 10).put("cha", 10)
                .put("class_levels", new JSONArray().put(new JSONObject().put("class_name", "Fighter").put("level", 1)));

        JSONObject denied = CharacterLevelUpTransitionEngine.apply(sheet299, "Fighter", 10, 2, true, false);
        assertFalse(denied.optBoolean("accepted"));
        assertEquals("xp_threshold_not_reached", denied.optString("level_up_blocked"));

        // Multiclass prerequisite boundary: STR 12 is insufficient for Fighter multiclass (requires STR 13)
        JSONObject wizard = new JSONObject()
                .put("name", "Scholar")
                .put("class_name", "Wizard")
                .put("level", 1)
                .put("xp", 300)
                .put("hp", 8).put("max_hp", 8)
                .put("con", 12).put("str", 12).put("dex", 10).put("int_score", 14).put("wis", 10).put("cha", 10)
                .put("class_levels", new JSONArray().put(new JSONObject().put("class_name", "Wizard").put("level", 1)));

        JSONObject multiclassDenied = CharacterLevelUpTransitionEngine.apply(wizard, "Fighter", 10, 1, true, true);
        assertFalse(multiclassDenied.optBoolean("accepted"));
        assertTrue(multiclassDenied.optString("level_up_blocked").contains("multiclass_strength_or_dexterity_13_required"));
    }

    // =========================================================================
    // 6. Backup21 Integrity Envelope Boundaries
    // =========================================================================
    @Test
    public void testBackup21IntegrityEnvelopeBoundaries() throws Exception {
        // Null backup
        assertEquals("blocked_missing_backup", Backup21IntegrityEnvelope.check(null));

        // Version != 21 without KEY is accepted for legacy compatibility
        JSONObject legacy = new JSONObject().put("version", 11);
        assertEquals("", Backup21IntegrityEnvelope.check(legacy));

        // Version != 21 with KEY is rejected
        JSONObject wrongVer = new JSONObject().put("version", 11).put(Backup21IntegrityEnvelope.KEY, "abc");
        assertEquals("unexpected_integrity_envelope_version", Backup21IntegrityEnvelope.check(wrongVer));

        // Invalid digest length (63 characters instead of 64)
        JSONObject shortHash = new JSONObject().put("version", 21)
                .put(Backup21IntegrityEnvelope.KEY, "123456789012345678901234567890123456789012345678901234567890123");
        assertEquals("invalid_integrity_digest", Backup21IntegrityEnvelope.check(shortHash));

        // Invalid digest characters (uppercase/non-hex)
        JSONObject upperHash = new JSONObject().put("version", 21)
                .put(Backup21IntegrityEnvelope.KEY, "A234567890123456789012345678901234567890123456789012345678901234");
        assertEquals("invalid_integrity_digest", Backup21IntegrityEnvelope.check(upperHash));
    }

    // =========================================================================
    // 7. Narration Chunking Boundaries
    // =========================================================================
    @Test
    public void testNarrationChunkingBoundaries() {
        // Empty text
        assertEquals(0, NarrationChunker.android("").size());
        assertEquals(0, NarrationChunker.android("   ").size());
        assertEquals(0, NarrationChunker.charon("").size());

        // Single word
        List<String> single = NarrationChunker.android("Fogo.");
        assertEquals(1, single.size());
        assertEquals("Fogo.", single.get(0));

        // Exactly 480 characters without sentence break
        StringBuilder b480 = new StringBuilder();
        for (int i = 0; i < 48; i++) b480.append("123456789 ");
        String text480 = b480.toString().trim();
        List<String> c480 = NarrationChunker.android(text480);
        assertTrue(c480.size() >= 1);
        for (String c : c480) assertTrue(c.length() <= 720);
    }

    // =========================================================================
    // 8. Companion Proposal Isolation Boundaries
    // =========================================================================
    @Test
    public void testCompanionProposalIsolationBoundaries() throws Exception {
        Set<Long> curated = new HashSet<>(Collections.singletonList(99L));

        // Null envelope
        assertEquals("", CompanionProposalIsolationRules.reject(null, curated));

        // Empty envelope
        assertEquals("", CompanionProposalIsolationRules.reject(new JSONObject(), curated));

        // Empty curated set
        JSONObject proposal = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 99).put("action", "Wait")));
        assertEquals("", CompanionProposalIsolationRules.reject(proposal, Collections.<Long>emptySet()));

        // Curated ID not present in proposals
        JSONObject otherProposal = new JSONObject().put("companion_proposals", new JSONArray()
                .put(new JSONObject().put("character_id", 101).put("action", "Wait")))
                .put("state_updates", new JSONArray().put("mutation"));
        assertEquals("", CompanionProposalIsolationRules.reject(otherProposal, curated));

        // Curated ID present with multiple hostile array keys
        String[] hostileKeys = {"tactical_updates", "map_updates", "clock_updates", "npc_updates", "journal_updates"};
        for (String key : hostileKeys) {
            JSONObject hostile = new JSONObject(proposal.toString()).put(key, new JSONArray().put("exploit"));
            assertEquals("curated_intention_mixed_with_" + key, CompanionProposalIsolationRules.reject(hostile, curated));
        }
    }

    // =========================================================================
    // 9. Living World Catchup Boundaries
    // =========================================================================
    @Test
    public void testLivingWorldCatchupBoundaries() throws Exception {
        // Disabled
        assertFalse(LivingWorldCatchUpRules.preview(false, true, 1000, 2000, 24, 10).optBoolean("enabled"));

        // Adventure not started
        assertEquals("adventure_not_started",
                LivingWorldCatchUpRules.preview(true, false, 1000, 2000, 24, 10).optString("reason"));

        // now <= lastSyncAt (clock reversed or identical)
        assertTrue(LivingWorldCatchUpRules.preview(true, true, 2000, 1000, 24, 10).optBoolean("initialize_only"));
        assertTrue(LivingWorldCatchUpRules.preview(true, true, 2000, 2000, 24, 10).optBoolean("initialize_only"));

        // lastSyncAt <= 0
        assertTrue(LivingWorldCatchUpRules.preview(true, true, 0, 2000, 24, 10).optBoolean("initialize_only"));

        // Zero rate clamped to minimum 1.0
        JSONObject rate0 = LivingWorldCatchUpRules.preview(true, true, 1000, 1000 + (24 * 3600 * 1000L), 0.0, 10);
        assertEquals(1.0, rate0.optDouble("rate_game_hours_per_real_day"), 0.01);
    }

    // =========================================================================
    // 10. Scene Image Import Policy Boundaries
    // =========================================================================
    @Test
    public void testSceneImageImportPolicyBoundaries() {
        // Null action
        assertFalse(SceneImageImportPolicy.validate(null, "image/jpeg", true, "s1", "s1").accepted);

        // Unknown action
        assertFalse(SceneImageImportPolicy.validate("android.intent.action.VIEW", "image/jpeg", true, "s1", "s1").accepted);

        // Null MIME
        assertFalse(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_SEND, null, true, "s1", "s1").accepted);

        // Non-image MIME
        assertFalse(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_SEND, "audio/mp3", true, "s1", "s1").accepted);

        // Missing URI
        assertFalse(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_SEND, "image/jpeg", false, "s1", "s1").accepted);
        assertFalse(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_CONSUME, "image/jpeg", false, "s1", "s1").accepted);

        // Empty current fingerprint matching empty pending
        assertTrue(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, "", "").accepted);

        // Null pending fingerprint accepted
        assertTrue(SceneImageImportPolicy.validate(SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, null, "s1").accepted);
    }
}
