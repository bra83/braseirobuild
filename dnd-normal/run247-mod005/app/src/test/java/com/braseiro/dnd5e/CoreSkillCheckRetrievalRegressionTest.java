package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** A plain player skill check must not depend on an unrelated manual search hit. */
public final class CoreSkillCheckRetrievalRegressionTest {
    @Test public void clueInvestigationCanUseLocalCore() {
        RuleRetrievalPolicy.Decision d=RuleRetrievalPolicy.decide(
            "Narrador","PLAYER_ACTION","quero fazer um teste pra desvendar a pista",false,false);
        assertFalse(d.reason,d.searchRequired);
        assertTrue(d.mechanicalCandidate);
    }
    @Test public void namedSkillCheckStillUsesCore() {
        RuleRetrievalPolicy.Decision d=RuleRetrievalPolicy.decide(
            "Narrador","PLAYER_ACTION","Faço um teste de Investigação para examinar as runas",false,false);
        assertFalse(d.reason,d.searchRequired);
    }
    @Test public void specialMagicProcedureStillRequiresEvidence() {
        RuleRetrievalPolicy.Decision d=RuleRetrievalPolicy.decide(
            "Narrador","PLAYER_ACTION","Quero fazer um teste para conjurar a magia Escudo",false,false);
        assertTrue(d.reason,d.searchRequired);
    }
    @Test public void explicitRulesQuestionStillRequiresEvidence() {
        RuleRetrievalPolicy.Decision d=RuleRetrievalPolicy.decide(
            "Narrador","GM_HELP","qual a regra da dificuldade para um teste?",false,false);
        assertTrue(d.reason,d.searchRequired);
    }
    @Test public void narrativeSuccessWithoutPendingRollIsStillBlocked() throws Exception {
        String action="quero fazer um teste pra desvendar a pista";
        assertTrue(MasterTestRequestGuard.isExplicitTestDeclaration(action));
        assertTrue(MasterTestRequestGuard.wouldResolveWithoutRoll(action,
            new JSONObject().put("narration","Você descobre o segredo!"),false));
        JSONObject awaiting=new JSONObject().put("pending_test",
            new JSONObject().put("required",true).put("type","ability")
            .put("ability","Inteligência").put("skill","Investigação").put("dc",15));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(action,awaiting,false));
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,
            new JSONObject().put("narration","Você descobre o segredo!")));
        assertFalse(MasterTestRequestGuard.requiresRemoteCorrection(action,awaiting));
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,
            new JSONObject().put("pending_test",
                new JSONObject().put("required",true).put("type","ability")
                    .put("ability","Inteligência"))));
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,
            new JSONObject().put("pending_test",awaiting.optJSONObject("pending_test"))
                .put("narration","Você conseguiu decifrar o glifo.")));
    }
}
