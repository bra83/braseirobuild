package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CanonicalSceneAudioRegressionTest {
    @Test public void sunnyOrUnknownWeatherNeverSelectsRain() {
        assertEquals("",SmartAudioLibrary.canonicalClimateKind("Sol"));
        assertEquals("",SmartAudioLibrary.canonicalClimateKind("Céu limpo"));
        assertEquals("",SmartAudioLibrary.canonicalClimateKind("Ensolarado, sem chuva"));
        assertEquals("",SmartAudioLibrary.canonicalClimateKind("clima neutro"));
        assertEquals("",SmartAudioLibrary.canonicalClimateKind(null));
    }
    @Test public void distinctWeatherTypesRemainDistinct() {
        assertEquals("rain",SmartAudioLibrary.canonicalClimateKind("Garoa"));
        assertEquals("storm",SmartAudioLibrary.canonicalClimateKind("Tempestade"));
        assertEquals("snow",SmartAudioLibrary.canonicalClimateKind("Neve"));
        assertEquals("wind",SmartAudioLibrary.canonicalClimateKind("Vento forte"));
    }
    @Test public void tableVoiceAddressesPlayersDirectly() {
        String prompt=HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(prompt.contains("segunda pessoa"));
        assertTrue(prompt.contains("vocês"));
        assertTrue(prompt.contains("não chame os heróis de a companhia"));
    }
}
