package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Multi-scene regression of the exact failure where Aisla calls an already
 * progressed scavenger hunt "the first clue" and a meta-question creates
 * new, unverified NPC dialogue.
 */
public final class CampaignContinuityGateRegressionTest {
    private static JSONObject scene(int id,String narration) throws Exception {
        return new JSONObject().put("id",id).put("kind","Cena")
            .put("title","Dia 1 • 09:45 • Biblioplex")
            .put("body","AÇÃO DECLARADA:\nKvothe examina a pista.\n\n"
                +"NARRAÇÃO DO MESTRE:\n"+narration);
    }
    private static List<JSONObject> accepted(){
        List<JSONObject> journal=new ArrayList<JSONObject>();
        try{
            journal.add(scene(5,"A primeira pista da caça de boas-vindas é apresentada."));
            journal.add(scene(7,"Kvothe encontrou outra pista e decifrou a pista da caça de boas-vindas."));
            journal.add(scene(9,"A segunda pista da caça de boas-vindas está decifrada."));
            journal.add(scene(12,"O enigma seguinte é resolvido. A terceira pista da caça de boas-vindas foi revelada."));
            journal.add(scene(16,"A quarta pista da caça de boas-vindas foi encontrada no pergaminho."));
            journal.add(scene(17,"Aisla Fitzbottom aponta o selo e comenta o alimento servido na cantina."));
        }catch(Exception e){throw new AssertionError(e);}
        return journal;
    }

    @Test public void laterClueBlocksContradictoryFirstClueBeforeCommit(){
        String next="Aisla Fitzbottom: — Esse pergaminho aí? É a primeira pista da caça de boas-vindas.";
        assertFalse(CampaignContinuityGate.contradiction(accepted(),next).isEmpty());
        assertEquals("",CampaignContinuityGate.contradiction(accepted(),
            "Aisla Fitzbottom pergunta sobre os ingredientes encontrados na cantina."));
        assertEquals("",CampaignContinuityGate.contradiction(new ArrayList<JSONObject>(),next));
    }

    @Test public void canonicalChronologyTracksSourcesAcrossThirtyFiveScenes() throws Exception{
        List<JSONObject> log=accepted();
        for(int i=18;i<=55;i++)log.add(scene(i,
            "Cena "+i+": o relógio segue e o grupo observa o ambiente sem concluir outra pista."));
        JSONObject context=CampaignContinuityGate.canonicalContext(log);
        assertEquals(4,context.getInt("highest_explicit_clue_ordinal"));
        assertEquals(44,context.getInt("scenes_scanned"));
        JSONArray timeline=context.getJSONArray("source_linked_chronology_newest_first");
        JSONArray sources=context.getJSONArray("clue_milestone_sources");
        assertTrue(timeline.length()>=12);
        assertTrue(sources.length()>=4);
        assertTrue(context.toString().contains("\"journal_id\":16"));
        assertTrue(context.toString().contains("quarta pista"));
        JSONObject prompt=CampaignContinuityGate.promptContext(log);
        assertEquals(4,prompt.getInt("highest_explicit_clue_ordinal"));
        assertTrue(prompt.toString().length()<9000);
        assertTrue("A fourth clue must survive context truncation",
            prompt.toString().contains("journal_id")&&prompt.toString().contains("16"));
        assertFalse(CampaignContinuityGate.contradiction(log,
            "Aisla diz que é a primeira pista da caça ao tesouro.").isEmpty());
    }

    @Test public void historyDisputeNeverBecomesAnInFictionNpcReply(){
        String q="Nessa cena o NPC disse ser a primeira pista da caça ao tesouro,"
            +" mas já estou na terceira ou quarta. Aisla se esqueceu da penúltima cena?";
        assertTrue(CampaignContinuityGate.historicalQuestion(q));
        assertTrue(CampaignContinuityGate.historicalQuestion(
            "Ela se contradiz: na cena anterior me deu instruções e depois ficou surpresa."));
        assertFalse(CampaignContinuityGate.historicalQuestion(
            "Kvothe pergunta a Aisla onde fica a cantina."));
        String answer=CampaignContinuityGate.auditAnswer(accepted(),q,
            "É a primeira pista da caça de boas-vindas.");
        assertTrue(CampaignContinuityGate.isSafeCanonicalSummary(answer));
        assertTrue(answer.contains("Nenhum NPC falou"));
        assertTrue(answer.contains("Registro 16"));
        assertTrue(answer.contains("pistas")||answer.contains("pista"));
        assertFalse(answer.contains("Aisla Fitzbottom: — Calma lá"));
        assertFalse(answer.contains("vários caminhos diferentes"));
    }

    @Test public void ambiguousEvidenceNeverManufacturesACounterOrADifferentHunt() throws Exception{
        List<JSONObject> log=new ArrayList<JSONObject>();
        log.add(scene(1,"A jovem segura um pergaminho com uma pista."));
        JSONObject context=CampaignContinuityGate.canonicalContext(log);
        assertEquals(0,context.getInt("highest_explicit_clue_ordinal"));
        assertEquals(0,context.getInt("distinct_explicit_hunt_clue_resolutions"));
        assertEquals("",CampaignContinuityGate.contradiction(log,
            "A primeira pista da caça de boas-vindas está escrita no papel."));
        String answer=CampaignContinuityGate.auditAnswer(log,"Essa é a quarta pista?",
            "Primeira pista da caça de boas-vindas.");
        assertTrue(answer.contains("evidência suficiente"));
        assertTrue(answer.contains("não autoriza"));
    }

    @Test public void ignoresNonSceneNotesAndPreservesJournalIdentity() throws Exception{
        List<JSONObject> log=accepted();
        log.add(new JSONObject().put("id",200).put("kind","Mestre")
            .put("body","A quarta pista da caça de boas-vindas foi encontrada."));
        JSONObject context=CampaignContinuityGate.canonicalContext(log);
        assertEquals(6,context.getInt("scenes_scanned"));
        assertFalse(context.toString().contains("\"journal_id\":200"));
    }
}
