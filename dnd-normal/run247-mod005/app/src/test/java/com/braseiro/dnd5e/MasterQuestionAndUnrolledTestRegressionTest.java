package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression for the false-success and duplicate consultation report. */
public final class MasterQuestionAndUnrolledTestRegressionTest {
    @Test public void accidentalQuestionInMainChatIsNeverAnAction() {
        assertTrue(MasterQuestionIntentGuard.isConsultation(
            "Existe algum teste que eu possa fazer para descobrir onde fica esse lugar?"));
        assertTrue(MasterQuestionIntentGuard.isConsultation(
            "Qual perícia posso usar para localizar o prédio?"));
        assertTrue(MasterQuestionIntentGuard.isConsultation(
            "Onde fica o local da orientação?"));
        assertTrue(MasterQuestionIntentGuard.isConsultation(
            "Mestre, que teste eu posso realizar aqui?"));
        assertFalse(MasterQuestionIntentGuard.isConsultation(
            "Pergunto à bibliotecária onde fica a sala."));
        assertFalse(MasterQuestionIntentGuard.isConsultation(
            "Eu faço um teste de Investigação para encontrar o local."));
        assertFalse(MasterQuestionIntentGuard.isConsultation(
            "O jogador confirma que quer TENTAR na ficção uma ação."));
        assertFalse(MasterQuestionIntentGuard.isConsultation(
            StoryMomentumAction.ACTION));
    }
    @Test public void cannotTreatDeclaredTestAsSuccessfulWithoutRolling() throws Exception {
        String action="Eu faço um teste de Investigação para localizar o edifício.";
        assertTrue(MasterTestRequestGuard.wouldResolveWithoutRoll(action,
            new JSONObject().put("narration","Você encontrou o edifício."),false));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(action,
            new JSONObject().put("pending_test",new JSONObject().put("required",true)
                .put("ability","Inteligência").put("skill","Investigação")),false));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(action,
            new JSONObject(),true));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(
            "Pergunto à bibliotecária onde é a sala.",new JSONObject(),false));
    }
    @Test public void doNotAutoApproveARequestedSkillCheck() {
        assertTrue(MasterTestRequestGuard.isExplicitTestDeclaration(
            "Tento um teste de Percepção para procurar rastros."));
        assertTrue(MasterTestRequestGuard.isExplicitTestDeclaration(
            "Vou rolar uma perícia para descobrir o paradeiro."));
        assertFalse(MasterTestRequestGuard.isExplicitTestDeclaration(
            "Onde posso fazer um teste?"));
    }
}
