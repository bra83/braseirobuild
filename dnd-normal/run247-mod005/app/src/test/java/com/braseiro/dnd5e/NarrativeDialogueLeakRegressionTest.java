package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class NarrativeDialogueLeakRegressionTest {
    @Test public void preservesQuotedNpcDialogueAndLinebreaksWithoutLeakingMetadata() {
        String raw="Auri pousa a caneca.\n— Você ouviu o sino? — pergunta Auri.\n"
                +"— Ouvi — responde Wilem.\n\n"
                +"O mundo não pausa enquanto essa decisão é tomada.\n\n"
                +"A chuva bate na janela.";
        assertTrue(NarrativeGuardService.containsAnyLeak(raw));
        String cleaned=GeneratedTextSanitizer.narrative(raw);
        assertTrue(cleaned.contains("— Você ouviu o sino? — pergunta Auri."));
        assertTrue(cleaned.contains("\n— Ouvi — responde Wilem."));
        assertTrue(cleaned.contains("A chuva bate na janela."));
        assertFalse(cleaned.contains("O mundo não pausa"));
        assertFalse(NarrativeGuardService.containsAnyLeak(cleaned));
    }

    @Test public void removesOneLeakedSentenceWithoutDestroyingAdjacentDialogue() {
        String raw="— A ponte está fechada — diz o guarda. O mundo não pausa enquanto essa decisão é tomada.";
        String cleaned=GeneratedTextSanitizer.narrative(raw);
        assertTrue(cleaned.contains("— A ponte está fechada — diz o guarda."));
        assertFalse(cleaned.contains("O mundo não pausa"));
    }

    @Test public void doesNotMistakeOrdinaryInWorldSpeechForEngineDirective() {
        String raw="— O mestre ferreiro chega amanhã — explica Auri.\n— Isso resolve o problema — responde Wilem.";
        assertEquals("Auri chega amanhã.",PortugueseNarrationSanitizer.sanitize("Auri chega amanhã."));
        assertEquals(raw,GeneratedTextSanitizer.narrative(raw));
        assertFalse(NarrativeGuardService.containsAnyLeak(raw));
    }

    @Test public void offlineDialogueOnlyUsesNpcVerifiedInTheScene() throws Exception {
        JSONObject scene=new JSONObject().put("present_npcs_json",
                new JSONArray().put(new JSONObject().put("name","Auri").put("role","estudante")).toString());
        assertEquals("Auri",OfflineMasterEngine.presentNpcName(scene));
        assertTrue(OfflineMasterEngine.situatedSocialReply("Falo com Auri","Auri","").contains("— Estou ouvindo"));
        assertEquals("",OfflineMasterEngine.situatedSocialReply("Olho o céu","Auri",""));
        assertEquals("",OfflineMasterEngine.situatedSocialReply("Falo com Auri","",""));
        assertEquals("",OfflineMasterEngine.situatedSocialReply("Falo com Auri","Auri","ameaça imediata"));
        assertEquals("",OfflineMasterEngine.presentNpcName(new JSONObject()));
    }

    @Test public void rejectsExactRealWorldLeakPatternsButRetainsFiction() {
        assertTrue(NarrativeGuardService.containsAnyLeak("host_world_authority"));
        assertTrue(NarrativeGuardService.containsAnyLeak("O mestre deve revelar a pista para o jogador"));
        assertTrue(NarrativeGuardService.containsAnyLeak("scene_state.present_npcs"));
        String cleaned=GeneratedTextSanitizer.narrative("O porteiro sorri.\n\nscene_state.present_npcs\n\n— Bem-vindo — diz o porteiro.");
        assertTrue(cleaned.contains("O porteiro sorri."));
        assertTrue(cleaned.contains("— Bem-vindo — diz o porteiro."));
        assertFalse(cleaned.contains("scene_state"));
    }
}
