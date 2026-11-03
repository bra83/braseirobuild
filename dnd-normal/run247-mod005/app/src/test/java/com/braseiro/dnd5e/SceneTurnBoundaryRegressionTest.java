package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Reproduces user-reported filler options, repeated image framing and journal leaks. */
public final class SceneTurnBoundaryRegressionTest {
    @Test public void noForcedFillerAndNoOldCartazWhenOrientationStarts() throws Exception {
        String narration=
            "Kvothe atravessa o saguão, deixando para trás veteranos que espalham adereços e cartazes. "+
            "O movimento da universidade continua ao redor enquanto ele alcança o Hall dos Oráculos. "+
            "Calouros se acomodam diante das estátuas. Um estudante ergue uma prancheta e confere nomes. "+
            "Ao notar a aproximação, um assistente de triagem sinaliza que a assembleia começa.";
        JSONArray candidates=new JSONArray()
            .put(new JSONObject().put("label","EXAMINAR OS CARTAZES")
                .put("description","Inspecionar os cartazes de perto em busca de detalhes concretos.")
                .put("action","Examino os cartazes procurando marcas, inscrições ou sinais de uso recente."));
        JSONObject scene=new JSONObject().put("location","Strixhaven • Hall dos Oráculos");
        JSONArray actual=SceneOptionDiversifier.ensureAlways(candidates,new JSONArray(),narration,"Entro no Hall dos Oráculos",scene,1);
        assertTrue(actual.length()<=2);
        for(int i=0;i<actual.length();i++){
            JSONObject x=actual.getJSONObject(i);
            String combined=x.optString("label")+" "+x.optString("description")+" "+x.optString("action");
            assertFalse(combined.contains("EXAMINAR OS CARTAZES"));
            assertFalse(combined.contains("detalhes concretos"));
            assertFalse(combined.contains("verifico de perto os elementos e acessos"));
        }
    }

    @Test public void noQuotaWhenNoGroundedChoiceExists() throws Exception {
        JSONArray options=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            "O brilho do teto diminui um pouco.", "", new JSONObject(),2);
        assertEquals(0,options.length());
    }

    @Test public void repeatedModelVisualPromptUsesLatestBeat() {
        String old="Grande salão com colunas, centenas de calouros e portas majestosas da biblioteca.";
        String newNarration="Kvothe caminha pelo saguão. Um assistente ergue a prancheta, "+
            "confere o nome de Kvothe e aponta para os bancos da orientação.";
        String selected=SceneVisualPromptGuard.choose(old,old,newNarration);
        assertTrue(selected.contains("prancheta"));
        assertFalse(selected.equals(old));
        assertTrue(SceneVisualPromptGuard.sameFrame(old,old));
    }

    @Test public void legacyContractNeverBecomesPlayerJournalAction() {
        String contract="CONTRATO NARRATIVO STRIXHAVEN — obrigatório enquanto active. "+
            "O jogador escolheu: Avanço até o balcão e examino o atendente. "+
            "Continue a partir dessa escolha, sem repetir a cena anterior.\n\n"+
            "=== STRIXHAVEN OFICIAL — RUNTIME COMPILADO ===\nBESTIÁRIO COMPILADO RELEVANTE:";
        assertEquals("Avanço até o balcão e examino o atendente.",
            SceneActionClock.cleanPlayerAction(contract));
        assertEquals("",SceneActionClock.cleanPlayerAction(
            "CONTRATO NARRATIVO STRIXHAVEN — teste.\n\nINÍCIO AUTOMÁTICO — PRIMEIRA CENA DO SEMESTRE."));
        String dirty="AÇÃO DECLARADA:\n"+contract+
            "\n\nNARRAÇÃO DO MESTRE:\nO bibliotecário indica o corredor.";
        String clean=JournalEntryPresentation.safeBody(dirty);
        assertTrue(clean.contains("Avanço até o balcão"));
        assertFalse(clean.contains("BESTIÁRIO COMPILADO"));
        assertFalse(clean.contains("CONTRATO NARRATIVO STRIXHAVEN"));
        assertTrue(clean.contains("O bibliotecário indica o corredor."));
    }

    @Test public void mapMovementAlreadyClockedDoesNotAdvanceAgain() {
        String travel="DESLOCAMENTO CONFIRMADO PELO MAPA: do pátio ao Biblioplex, "+
            "5 minutos já aplicados. Narre a chegada.";
        assertEquals(0d,SceneActionClock.minutesFor(travel),0.0001d);
        assertEquals(0d,SceneActionClock.minutesFor("INÍCIO AUTOMÁTICO — PRIMEIRA CENA DO SEMESTRE."),0.0001d);
    }
}
