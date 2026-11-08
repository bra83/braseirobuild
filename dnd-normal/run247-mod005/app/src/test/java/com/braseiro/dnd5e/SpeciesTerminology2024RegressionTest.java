package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Prevents unstable AI-generated species vocabulary in D&D 2024 text. */
public final class SpeciesTerminology2024RegressionTest {
    @Test public void narratorNamesSpeciesWithoutInventedGenderWords(){
        String prompt=HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(prompt.contains("Orc e Orcs"));
        assertTrue(prompt.contains("gnoma, não gnomia"));
        assertTrue(prompt.contains("preserve todos esses dados"));
        assertTrue(prompt.contains("gnômico"));
    }
}
