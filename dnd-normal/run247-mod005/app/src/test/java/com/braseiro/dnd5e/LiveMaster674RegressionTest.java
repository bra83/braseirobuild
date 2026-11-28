package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;
public final class LiveMaster674RegressionTest{
    private JSONObject scene()throws Exception{return new JSONObject().put("location","Pousada Marco do Percurso").put("present_npcs",new JSONArray().put(new JSONObject().put("name","Kote").put("role","estalajadeiro")));}
    @Test public void dialogueInMainComposerNeverBecomesObservation()throws Exception{
        JSONObject s=scene();
        for(String q:new String[]{"Você é o dono?","Kote, é você o dono?","O senhor tem quartos?","Quanto custa a cama?","É o dono?","Tem quartos?","Perguntar ao estalajadeiro o preço"}){
            assertEquals(q,FictionTurnIntent.Kind.CHARACTER_SPEECH,FictionTurnIntent.classify(q,s,"PLAYER_ACTION"));
            assertFalse(FictionTurnIntent.freezesWorld(FictionTurnIntent.classify(q,s,"PLAYER_ACTION"),q,s));
        }
        assertEquals(FictionTurnIntent.Kind.FICTION_OBSERVATION,FictionTurnIntent.classify("O que vejo?",s,"PLAYER_ACTION"));
        assertEquals(FictionTurnIntent.Kind.RULES_HELP,FictionTurnIntent.classify("Você é o dono?",s,"GM_HELP"));
    }
    @Test public void openingCannotPassWithTinyTextOrWrongKeeper()throws Exception{
        String a="ABERTURA DA AVENTURA PRINCIPAL IMPORTADA: Pousada Marco do Percurso";
        assertTrue(LiveMasterAcceptance.problem("Barl limpa a caneca.",new JSONObject(),scene(),a).contains("Kote"));
        assertTrue(LiveMasterAcceptance.problem("Vocês veem Kote.",new JSONObject(),scene(),a).contains("insuficiente"));
        assertFalse(LiveMasterProseGate.problem("O bafo morno da estalagem recebe o grupo.",new JSONObject(),scene(),"Entro na estalagem").isEmpty());
        JSONObject bad=new JSONObject().put("npc_updates",new JSONArray().put(new JSONObject().put("name","Outro").put("role","estalajadeiro")));
        assertTrue(LiveMasterAcceptance.problem("Vocês veem um homem.",bad,scene(),a).contains("identidade"));
        assertEquals("",LiveMasterAcceptance.problem("Kote: — Tenho quartos. Vocês querem jantar?",new JSONObject(),scene(),"Você tem quartos?"));
    }
    @Test public void principalSourceKeepsOriginalIntroductionAndCast()throws Exception{
        String introduction="Introdução integral da aventura, não um resumo.";
        JSONObject pkg=new JSONObject().put("id","principal").put("story",new JSONObject().put("playerIntroduction",introduction))
            .put("start",new JSONObject().put("hook","Gancho obrigatório"))
            .put("npcs",new JSONArray().put(new JSONObject().put("name","Kote").put("role","estalajadeiro")))
            .put("sideQuestLibrary",new JSONArray().put("não inserir"));
        JSONObject source=BraseiroAdventureStore.principalNarrativeSource(pkg);
        assertEquals(introduction,source.getJSONObject("story").getString("playerIntroduction"));
        assertEquals("Gancho obrigatório",source.getJSONObject("start").getString("hook"));
        assertEquals("Kote",source.getJSONArray("npcs").getJSONObject(0).getString("name"));
        assertFalse(source.has("sideQuestLibrary"));
    }
    @Test public void dedicatedOpeningIncludesCanonicalSourceAndAvoidsTurnMechanics()throws Exception{
        JSONObject source=new JSONObject().put("story",new JSONObject().put("playerIntroduction","Introdução integral"))
            .put("start",new JSONObject().put("hook","Gancho exclusivo"));
        String prompt=LiveMasterOpeningPolicy.prompt(source,scene(),java.util.Arrays.asList(new JSONObject().put("name","Kvothe"),new JSONObject().put("name","Auri")),"Regra local","ABERTURA DA AVENTURA PRINCIPAL IMPORTADA");
        assertTrue(prompt.contains("Introdução integral"));assertTrue(prompt.contains("Gancho exclusivo"));
        assertTrue(prompt.contains("Kvothe"));assertTrue(prompt.contains("Auri"));
        assertTrue(prompt.contains("Kote"));assertTrue(prompt.contains("350 palavras"));
        assertTrue(prompt.contains("BRAS_BOUNDED_OPENING"));assertTrue(prompt.contains("pending_test=null"));
        assertFalse(prompt.contains("BRAS_MASTER_COGNITIVE_340"));
    }
}
