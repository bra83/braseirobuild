package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests verifying CompiledMonsterDamageGuard contract harmonization:
 * - Implicit single die syntax (e.g. "d8" -> "1d8", "-d6" -> "-1d6")
 * - Term commutativity / reordering (e.g. "3 + 1d8" == "1d8 + 3")
 * - Whitespace and case insensitivity
 * - Malformed input safety and range calculations
 */
public class CompiledMonsterDamageGuardTest {

    @Test
    public void canonicalNormalizesSingleDieImplicitCount() {
        assertEquals("1d8", CompiledMonsterDamageGuard.canonical("d8"));
        assertEquals("1d8", CompiledMonsterDamageGuard.canonical("1d8"));
        assertEquals("1d12", CompiledMonsterDamageGuard.canonical("d12"));
        assertEquals("1d20", CompiledMonsterDamageGuard.canonical("d20"));
        assertEquals("-1d6", CompiledMonsterDamageGuard.canonical("-d6"));
        assertEquals("-1d6", CompiledMonsterDamageGuard.canonical("-1d6"));
    }

    @Test
    public void equivalentMatchesSingleDieImplicitCount() {
        assertTrue(CompiledMonsterDamageGuard.equivalent("d8", "1d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("1d8", "d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("-d6", "-1d6"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("-1d6", "-d6"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("d10", "1d10"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("d8 + 3", "1d8 + 3"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("3 + d8", "1d8 + 3"));
    }

    @Test
    public void canonicalReordersTermsCommutatively() {
        assertEquals("1d8 + 3", CompiledMonsterDamageGuard.canonical("3 + 1d8"));
        assertEquals("1d8 + 3", CompiledMonsterDamageGuard.canonical("1d8 + 3"));
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical("4 + 1d8"));
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical("1d8 + 4"));
        assertEquals("1d8 + 2d6 + 4", CompiledMonsterDamageGuard.canonical("2d6 + 1d8 + 4"));
        assertEquals("1d8 + 2d6 + 4", CompiledMonsterDamageGuard.canonical("4 + 2d6 + 1d8"));
        assertEquals("1d8 + 2d6 + 4", CompiledMonsterDamageGuard.canonical("1d8 + 4 + 2d6"));
    }

    @Test
    public void equivalentMatchesCommutativeTerms() {
        assertTrue(CompiledMonsterDamageGuard.equivalent("3 + 1d8", "1d8 + 3"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("1d8 + 4", "4 + 1d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("4 + 1d8", "1d8 + 4"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("2d6 + 1d8 + 4", "4 + 2d6 + 1d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("4 + 1d8 + 2d6", "1d8 + 2d6 + 4"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("4 - 1d8", "-1d8 + 4"));
    }

    @Test
    public void canonicalAndEquivalentHandleWhitespaceVariations() {
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical(" 1d8   +   4  "));
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical("1d8+4"));
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical("\t1d8 \n + \t 4 "));
        assertTrue(CompiledMonsterDamageGuard.equivalent("1d8+4", "1d8 + 4"));
        assertTrue(CompiledMonsterDamageGuard.equivalent(" 1d8 + 4 ", "1d8+4"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("  d8  +  4  ", "1d8 + 4"));
    }

    @Test
    public void canonicalAndEquivalentAreCaseInsensitive() {
        assertEquals("1d8 + 4", CompiledMonsterDamageGuard.canonical("1D8 + 4"));
        assertEquals("1d8", CompiledMonsterDamageGuard.canonical("D8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("1D8 + 4", "1d8 + 4"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("D8", "1d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent("2D6 + 3", "2d6 + 3"));
    }

    @Test
    public void equivalentRejectsDivergentFormulas() {
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8 + 4", "1d8 + 5"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8 + 4", "1d6 + 4"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8", "2d8"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8", "1d10"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8 + 3", "1d8 - 3"));
    }

    @Test
    public void nullAndMalformedInputsFailClosedSafely() {
        assertEquals("", CompiledMonsterDamageGuard.canonical(null));
        assertEquals("", CompiledMonsterDamageGuard.canonical(""));
        assertEquals("", CompiledMonsterDamageGuard.canonical("   "));
        assertEquals("", CompiledMonsterDamageGuard.canonical("invalid"));
        assertEquals("", CompiledMonsterDamageGuard.canonical("1d"));
        assertEquals("", CompiledMonsterDamageGuard.canonical("d"));
        assertEquals("", CompiledMonsterDamageGuard.canonical("1d8 +"));
        assertEquals("", CompiledMonsterDamageGuard.canonical("1d8 & 4"));
        assertEquals("", CompiledMonsterDamageGuard.canonical("1d8 + + 4"));

        assertFalse(CompiledMonsterDamageGuard.equivalent(null, "1d8"));
        assertFalse(CompiledMonsterDamageGuard.equivalent("1d8", null));
        assertFalse(CompiledMonsterDamageGuard.equivalent(null, null));
        assertFalse(CompiledMonsterDamageGuard.equivalent("", ""));
        assertFalse(CompiledMonsterDamageGuard.equivalent("invalid", "invalid"));
    }

    @Test
    public void acceptsAndRangeEvaluateImplicitSingleDie() {
        CompiledMonsterDamageGuard.Range range = CompiledMonsterDamageGuard.range("d8", "4", false);
        assertNotNull(range);
        assertEquals(5, range.min);
        assertEquals(12, range.max);

        assertTrue(CompiledMonsterDamageGuard.accepts("d8", "", 5, false));
        assertTrue(CompiledMonsterDamageGuard.accepts("d8", "", 1, false));
        assertTrue(CompiledMonsterDamageGuard.accepts("d8", "", 8, false));
        assertFalse(CompiledMonsterDamageGuard.accepts("d8", "", 0, false));
        assertFalse(CompiledMonsterDamageGuard.accepts("d8", "", 9, false));

        // Critical doubles dice: 2d8 -> min 2, max 16
        CompiledMonsterDamageGuard.Range critRange = CompiledMonsterDamageGuard.range("d8", "0", true);
        assertNotNull(critRange);
        assertEquals(2, critRange.min);
        assertEquals(16, critRange.max);
        assertTrue(CompiledMonsterDamageGuard.accepts("d8", "", 16, true));
        assertFalse(CompiledMonsterDamageGuard.accepts("d8", "", 17, true));
    }

    @Test
    public void equippedPlayerWeaponFormulaToleratesSpacingAndSingleDie() {
        // Player equipped weapon format from EquippedWeaponAttackRules produces "1d8+3"
        String rawWeaponFormula = "1d8+3";
        assertTrue(CompiledMonsterDamageGuard.equivalent(rawWeaponFormula, "1d8 + 3"));
        assertTrue(CompiledMonsterDamageGuard.equivalent(rawWeaponFormula, "3 + 1d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent(rawWeaponFormula, "d8 + 3"));
        assertTrue(CompiledMonsterDamageGuard.equivalent(rawWeaponFormula, "3 + d8"));
        assertTrue(CompiledMonsterDamageGuard.equivalent(rawWeaponFormula, " 1D8  + 3 "));
    }
}
