package com.braseiro.dnd5e;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/** Focused G06 preflight tests: supported 2024 catalog only, source class,
 * material component, and deterministic area target selection. */
public class SpellTargetComponentExecutorTest {
    private static SpellTargetComponentExecutor.Request request(
            String edition, String sourceClass, List<String> inventory) {
        return new SpellTargetComponentExecutor.Request(
                edition, "Fireball", sourceClass, "int", 5,
                12, 8, "scene-1", "[]", 1L,
                2, 2, -1, 0L,
                Collections.singletonList(new Core2024CastingEligibility.ClassLevel("Wizard", 5)),
                inventory,
                Arrays.asList(
                        new Core2024TacticalGeometry.Token(1L, .20, .25),
                        new Core2024TacticalGeometry.Token(2L, .25, .25),
                        new Core2024TacticalGeometry.Token(3L, .90, .75)));
    }

    @Test public void rejectsLegacyEditionInsteadOfRunning2024Catalog() {
        SpellTargetComponentExecutor.Result result = SpellTargetComponentExecutor.execute(
                request("2014", "Wizard", Collections.singletonList("Arcane Focus")));

        assertFalse(result.allowed());
        assertEquals("requires_2024_rules_profile", result.error);
        assertTrue(result.targetIds.isEmpty());
    }

    @Test public void verifiesWizardMaterialAndRecomputesFireballTargets() {
        SpellTargetComponentExecutor.Result result = SpellTargetComponentExecutor.execute(
                request("2024", "Wizard", Collections.singletonList("Arcane Focus")));

        assertTrue(result.error, result.allowed());
        assertEquals("wizard", result.sourceClass);
        assertEquals(Arrays.asList(1L, 2L), result.targetIds);
        assertEquals(64, result.signature.length());
    }

    @Test public void missingMaterialOrWrongSourceClassFailsClosed() {
        SpellTargetComponentExecutor.Result missingMaterial = SpellTargetComponentExecutor.execute(
                request("2024", "Wizard", Collections.<String>emptyList()));
        assertFalse(missingMaterial.allowed());
        assertEquals("material_component_missing", missingMaterial.error);

        SpellTargetComponentExecutor.Result wrongClass = SpellTargetComponentExecutor.execute(
                request("2024", "Sorcerer", Collections.singletonList("Arcane Focus")));
        assertFalse(wrongClass.allowed());
        assertEquals("source_class_mismatch", wrongClass.error);
    }
}
