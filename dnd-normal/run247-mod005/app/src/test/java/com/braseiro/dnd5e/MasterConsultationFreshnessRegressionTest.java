package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MasterConsultationFreshnessRegressionTest {
    @Test public void rejectsRecycledParagraphForNewQuestion() throws Exception {
        String oldAnswer="A arquitetura do campus pode conter pistas relativas à charada, e você poderia usar Investigação ou Arcanismo para examinar os termos do poema sem concluir automaticamente nada.";
        JSONObject previous=new JSONObject().put("question","Faço algum teste para decifrar o enigma?")
            .put("answer",oldAnswer);
        assertTrue(MasterConsultationFreshnessGuard.repeatsPreviousForDifferentQuestion(
            "O que está escrito no selo da minha carta?",oldAnswer,previous));
    }
    @Test public void acceptsOriginalQuestionAndGenuineNewReply() throws Exception {
        String oldAnswer="As inscrições estão abertas no saguão principal e os alunos podem perguntar à bibliotecária pelo calendário de aulas.";
        JSONObject previous=new JSONObject().put("question","Onde ficam as inscrições?").put("answer",oldAnswer);
        assertFalse(MasterConsultationFreshnessGuard.repeatsPreviousForDifferentQuestion(
            "Onde ficam as inscrições?",oldAnswer,previous));
        assertFalse(MasterConsultationFreshnessGuard.repeatsPreviousForDifferentQuestion(
            "Qual a localização da sala?","A sala está no segundo corredor.",previous));
        assertFalse(MasterConsultationFreshnessGuard.repeatsPreviousForDifferentQuestion(
            "Qual a localização da sala?","Uma resposta curta.",previous));
    }
}
