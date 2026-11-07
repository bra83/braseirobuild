package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Mechanical audit: no invented success, no dropped intellectual check. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class DndRulesNarrativeAdjudication6654Test {
    private static final String RIDDLE="Tento decifrar a charada de Silverquill para descobrir os selos.";

    @Test public void helpOnRulesTriggersResearchWithoutPlayerAction() {
        for(String q:new String[]{
                "Qual teste posso fazer para descobrir a resposta?",
                "Que perícia devo usar para entender a inscrição?",
                "Quais testes estão disponíveis?",
                "Preciso rolar para decifrar a charada?"}){
            RuleRetrievalPolicy.Decision d=RuleRetrievalPolicy.decide(
                "Narrador","GM_HELP",q,false,false);
            assertTrue(q,d.searchRequired);
            assertFalse(DndActionCheckPolicy.consequentialChallenge(q,null));
        }
    }

    @Test public void ordinaryTalkObservationAndTravelDoNotForceDice() throws Exception {
        for(String action:new String[]{
                "Pergunto à aluna se ela participa do salão de poesia.",
                "Observo os estudantes à distância.",
                "Caminho até o balcão de atendimento."})
            assertFalse(DndActionCheckPolicy.consequentialChallenge(action,null));
        assertFalse(DndActionCheckPolicy.missingRequiredResolution(
            "Observo os estudantes",
            new JSONObject().put("narration","Uma estudante acena para Kvothe."),false));
    }

    @Test public void unrolledImplicitCheckCannotNarrateResolvedOutcome() throws Exception{
        for(String text:new String[]{
                "Kvothe decifra a charada e encontra a resposta.",
                "Kvothe não consegue decifrar a charada; a tentativa fracassa.",
                "Você consegue descobrir a resposta da charada."}){
            assertTrue(text,MasterTestRequestGuard.wouldResolveWithoutRoll(RIDDLE,
                new JSONObject().put("narration",text),false));
        }
        JSONObject pending=new JSONObject().put("required",true).put("dc",14)
            .put("ability","Inteligência").put("skill","Investigação");
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(RIDDLE,
            new JSONObject().put("narration","Kvothe decifra a charada.")
               .put("pending_test",pending),false));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(RIDDLE,
            new JSONObject().put("narration","Kvothe decifra a charada."),true));
        assertFalse(MasterTestRequestGuard.wouldResolveWithoutRoll(RIDDLE,
            new JSONObject().put("narration","A aluna aponta para as fitas e aguarda a tentativa."),false));
    }

    @Test public void explicitClueTestNeedsValidPendingRollBeforeNarration() throws Exception {
        String action="Quero fazer um teste pra desvendar a pista.";
        JSONObject missing=new JSONObject().put("protocol_version",1)
            .put("narration","O Mestre conta o que acontece.");
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,missing));
        JSONObject noDc=new JSONObject().put("protocol_version",1)
            .put("narration","O Mestre pede um teste.")
            .put("pending_test",new JSONObject().put("required",true)
                .put("type","ability").put("ability","Inteligência")
                .put("skill","Investigação"));
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,noDc));
        JSONObject valid=new JSONObject().put("protocol_version",1)
            .put("narration","Kvothe observa os sulcos e se prepara para rolar.")
            .put("pending_test",new JSONObject().put("required",true)
                .put("type","ability").put("ability","Inteligência")
                .put("skill","Investigação").put("dc",14)
                .put("reason","Padrão geométrico complexo."));
        assertFalse(MasterTestRequestGuard.requiresRemoteCorrection(action,valid));
        valid.put("narration","Kvothe decifra a pista e descobre a resposta.");
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,valid));
        valid.put("narration","Kvothe aguarda a rolagem.");
        valid.remove("protocol_version");
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,valid));
    }

    @Test public void preserveIntellectualCheckButNeverInventDcTen() throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String file="dnd6654-rules-"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,file);
        try{
            db.setActiveMapProfile("dnd");
            db.saveCampaign("Strixhaven",DndRulesEngine.PROFILE_2024,
                "Fantasia acadêmica",true,true,false,"");
            MasterRulesArbiter arbiter=new MasterRulesArbiter(db);
            JSONObject signal=arbiter.rulesFrame(RIDDLE).optJSONObject("action_adjudication");
            assertNotNull(signal);
            assertTrue(signal.optBoolean("uncertain_skill_challenge"));
            JSONObject success=arbiter.validate(new JSONObject()
                .put("narration","O Mestre pede uma prova.")
                .put("pending_test",new JSONObject().put("required",true).put("dc",14)
                    .put("ability","Inteligência").put("skill","Investigação"))
                .put("state_updates",new JSONArray()),RIDDLE);
            assertNotNull(success.optJSONObject("pending_test"));
            assertEquals(14,success.getJSONObject("pending_test").optInt("dc"));
            JSONObject incomplete=arbiter.validate(new JSONObject()
                .put("narration","O Mestre pede uma prova.")
                .put("pending_test",new JSONObject().put("required",true)
                    .put("ability","Inteligência").put("skill","Investigação")),RIDDLE);
            assertTrue(incomplete.optBoolean("_mechanics_blocked"));
            assertNull(incomplete.optJSONObject("pending_test"));
        }finally{db.close();context.deleteDatabase(file);}
    }
}
