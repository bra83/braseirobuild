package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Settings and story-to-TTS boundary must be deterministic and non-mutating. */
public final class GeminiVoiceAndNarrativeQualityTest {
    @Test public void androidRatePersistsInSafeLimits() {
        assertEquals(60,AndroidTtsBackend.clampSpeedPercent(-10));
        assertEquals(100,AndroidTtsBackend.clampSpeedPercent(100));
        assertEquals(160,AndroidTtsBackend.clampSpeedPercent(800));
        assertEquals(1.25f,AndroidTtsBackend.adjustedRate(1.0f,125),.0001f);
    }
    @Test public void characterContrastIsStableAndDoesNotAlterNarrator() {
        String narrator="Charon";
        assertEquals(narrator,GeminiTtsPerformance.voice(narrator,"Kore","Narrador",100));
        assertEquals(narrator,GeminiTtsPerformance.voice(narrator,"Kore","Estilhaço",0));
        assertEquals(narrator,GeminiTtsPerformance.voice(narrator,"Kore","Estilhaço",25));
        String first=GeminiTtsPerformance.voice(narrator,"Kore","Estilhaço",75);
        assertNotEquals(narrator,first);
        assertEquals(first,GeminiTtsPerformance.voice(narrator,"Kore","Estilhaço",75));
        assertTrue(GeminiTtsPerformance.direction("Estilhaço",75).contains("distinct"));
        assertEquals("",GeminiTtsPerformance.direction("Narrador",100));
    }
    @Test public void tagsOnlyInSynthesizedCopyAndOnlyForExpressedPause() {
        String narration="Um estudante hesita... e responde.";
        assertEquals("Um estudante hesita... <short pause> e responde.",
                GeminiTtsPerformance.taggedTranscript(narration));
        assertEquals("Um estudante hesita... e responde.",narration);
        assertEquals("Uma manhã clara.",GeminiTtsPerformance.taggedTranscript("Uma manhã clara."));
    }
    @Test public void staleDebtDoesNotGenerateUnrelatedGardenChoices() throws Exception {
        String narration="No Jardim dos Livros, três arbustos despertados ajustam as raízes "
            +"e observam estudantes passando ao longo dos canteiros.";
        JSONObject stale=new JSONObject()
            .put("present_npcs",new JSONArray().put(new JSONObject().put("name","Estilhaço")))
            .put("objective","cobrar pagamento a Estilhaço");
        JSONArray previous=new JSONArray()
            .put(new JSONObject().put("label","EXIGIR PROVA DA COBRANÇA")
                .put("action","Exigir prova da cobrança de Estilhaço."));
        JSONArray actual=SceneOptionDiversifier.diversify(previous,new JSONArray(),
                narration,"Caminho pelo jardim",stale,1);
        assertEquals(2,actual.length());
        assertTrue(actual.toString().contains("ARBUSTOS"));
        assertFalse(actual.toString().contains("COBRANÇA"));
        assertFalse(actual.toString().contains("ESTILHAÇO"));
    }
    @Test public void socialNarratorRequiresDialogueWithoutWritingHeroSpeech() {
        String instruction=HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(instruction.contains("2 a 5 falas"));
        assertTrue(instruction.contains("não faça o protagonista dizer"));
    }
}
