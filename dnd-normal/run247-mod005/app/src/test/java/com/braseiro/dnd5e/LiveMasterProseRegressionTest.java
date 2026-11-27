package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;
public final class LiveMasterProseRegressionTest {
    @Test public void englishSentenceIsRejectedWithoutRejectingProperNames(){
        assertTrue(LiveMasterProseGate.hasEnglishSentence("Vocês entram na pousada. The innkeeper looks at you and asks what you want."));
        assertFalse(LiveMasterProseGate.hasEnglishSentence("Kvothe, você vê Kote atrás do balcão. Bast fala com vocês na Waystone Inn."));
    }
    @Test public void waystoneKeeperHasOneIdentity()throws Exception{
        JSONObject prior=new JSONObject().put("location","Pousada Marco do Percurso");
        JSONArray cast=new JSONArray().put(new JSONObject().put("name","Kote").put("role","estalajadeiro")).put(new JSONObject().put("name","Estalajadeiro").put("anonymous",true)).put(new JSONObject().put("name","Bast"));
        JSONObject env=new JSONObject().put("scene_state",new JSONObject().put("present_npcs",cast));
        SceneNpcRoleAliases.reconcile(env,prior);
        assertEquals(2,env.getJSONObject("scene_state").getJSONArray("present_npcs").length());
    }
    @Test public void realTableProfileAllowsHandingTurnBack(){
        String s=HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(s.contains("O que vocês fazem?"));assertTrue(s.contains("vocativos"));
        assertFalse(s.contains("NUNCA termine com a pergunta genérica"));
    }
    @Test public void importedOpeningIsNotPlayerAction(){
        assertEquals("",SceneActionClock.cleanPlayerAction("ABERTURA DA AVENTURA PRINCIPAL IMPORTADA: local inicial obrigatório Pousada Marco do Percurso no hex DO40. INÍCIO AUTOMÁTICO — ABERTURA DE HISTÓRIA."));
    }
    @Test public void shortGroundedOpeningIsNotPadded() throws Exception {
        String s="Vocês estão na pousada no Vale do Dessarin. Kote: — Bom dia! O que vocês fazem?";
        assertEquals(s,OfflineMasterEngine.completePresentationWithBarbara(s,"campaign_opening",null,new JSONObject()));
        assertEquals("",WorldOnboardingNarrativeGuard.problem(s,"campaign_opening"));
    }
    @Test public void knownNpcInteractionRequiresSpeechAndRuralLocationStaysRural() throws Exception {
        JSONObject scene=new JSONObject().put("location","Pousada Marco do Percurso").put("present_npcs",new JSONArray().put(new JSONObject().put("name","Kote")));
        assertFalse(LiveMasterProseGate.problem("A companhia avança pelas praças centrais.",new JSONObject(),scene,"ABERTURA DA AVENTURA PRINCIPAL IMPORTADA:").isEmpty());
        assertFalse(LiveMasterProseGate.problem("Kote limpa o balcão. Vocês esperam.",new JSONObject(),scene,"Pergunto a Kote").isEmpty());
        assertEquals("",LiveMasterProseGate.problem("Kvothe, você vê Kote atrás do balcão.\nKote: — Bom dia!\nO que vocês fazem?",new JSONObject(),scene,"Pergunto a Kote"));
    }
}
