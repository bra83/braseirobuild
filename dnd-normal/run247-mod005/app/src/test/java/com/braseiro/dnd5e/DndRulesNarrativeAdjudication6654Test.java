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

    @Test public void delayedRollWordingNeverMasksUnrolledSuccess() throws Exception {
        String action="Tento decifrar a charada de Silverquill para descobrir os selos.";
        for(String narration:new String[]{
                "O Mestre pede um teste. Logo depois, Kvothe decifra a charada.",
                "Ainda não se sabe o resultado. Mas você consegue descobrir a resposta.",
                "Kvothe aguarda a rolagem. Em seguida você encontra a solução.",
                "Antes de rolar, Kvothe fracassa na tentativa."}){
            assertTrue(narration,DndActionCheckPolicy.unresolvedOutcomeClaimed(narration));
            assertTrue(narration,DndActionCheckPolicy.missingRequiredResolution(action,
                new JSONObject().put("narration",narration),false));
        }
        assertFalse(DndActionCheckPolicy.unresolvedOutcomeClaimed(
            "O Mestre pede um teste. Kvothe aguarda a rolagem, sem saber a resposta."));
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
        assertTrue("A pending roll must not license a narrated result",
            MasterTestRequestGuard.wouldResolveWithoutRoll(RIDDLE,
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
        assertTrue(MasterTestRequestGuard.isPreparedPendingCheck(valid));
        assertFalse(MasterTestRequestGuard.isPreparedPendingCheck(noDc));
        valid.put("narration","Kvothe decifra a pista e descobre a resposta.");
        assertTrue("A resposta do Mestre não pode resolver a pista sem o dado",
            DndActionCheckPolicy.missingRequiredResolution(action,valid,false));
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,valid));
        assertFalse(MasterTestRequestGuard.isPreparedPendingCheck(valid));
        valid.put("narration","Kvothe aguarda a rolagem.");
        valid.remove("protocol_version");
        assertTrue(MasterTestRequestGuard.requiresRemoteCorrection(action,valid));
    }

    @Test public void neverCoerceMalformedOrOutOfRangeDcToTenOrBoundary() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        String file="dnd6654-bad-dc-"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,file);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("Strixhaven",DndRulesEngine.PROFILE_2024,
                "Fantasia acadêmica",true,true,false,"");
            MasterRulesArbiter arbiter=new MasterRulesArbiter(db);
            Object[] invalid={JSONObject.NULL,"abc","14",0,4,31,999,14.25d};
            for(Object dc:invalid){
                JSONObject test=new JSONObject().put("required",true)
                    .put("type","ability").put("ability","Inteligência")
                    .put("skill","Investigação").put("dc",dc);
                JSONObject candidate=new JSONObject()
                    .put("narration","A cena aguarda uma tentativa.")
                    .put("pending_test",test)
                    .put("state_updates",new JSONArray());
                JSONObject validated=arbiter.validate(candidate,RIDDLE);
                assertTrue("Invalid DC must fail closed: "+dc,validated.optBoolean("_mechanics_blocked"));
                assertNull("Invalid DC may not become a roll: "+dc,validated.optJSONObject("pending_test"));
            }
            for(String ability:new String[]{"","Inexistente","Perícia","Charisma42"}){
                JSONObject invalidAbility=new JSONObject().put("narration","Aguarde a rolagem.")
                    .put("pending_test",new JSONObject().put("required",true).put("dc",14)
                        .put("type","ability").put("ability",ability)
                        .put("skill","Investigação"));
                JSONObject checked=arbiter.validate(invalidAbility,RIDDLE);
                assertTrue("Unknown ability must not be treated as Wisdom: "+ability,
                    checked.optBoolean("_mechanics_blocked"));
                assertNull(checked.optJSONObject("pending_test"));
            }
            JSONObject correct=new JSONObject().put("narration","Aguarde a rolagem.")
                .put("pending_test",new JSONObject().put("required",true).put("dc",14)
                    .put("type","ability").put("ability","Inteligência")
                    .put("skill","Investigação"));
            JSONObject accepted=arbiter.validate(correct,RIDDLE);
            assertFalse("Valid numeric DC should not be mechanically blocked",
                accepted.optBoolean("_mechanics_blocked"));
            assertEquals(14,accepted.getJSONObject("pending_test").getInt("dc"));
        } finally { db.close(); context.deleteDatabase(file); }
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
