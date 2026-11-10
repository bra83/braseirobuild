package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * AUDIT-ONLY negative acceptance tests. Intentional RED gates reproduce
 * remaining quality defects in scene action generation. Do not merge this
 * test-only branch into production while these checks are failing.
 */
public final class AuditSceneSuggestedActions20261110Test {
    private static final String AISLA =
        "Aisla solta uma risada curta enquanto alcança a caneca no balcão. "+
        "— Se a folha desperta apontada pelo enigma for o que estou pensando, "+
        "melhor terminar isso antes que os grupos de Witherbloom ocupem os canteiros. "+
        "O Jardim dos Livros costuma esconder mais do que raízes, arbustos falantes e "+
        "pistas dos veteranos — diz Aisla, olhando para Kvothe.";

    @Test public void A01_recentHistoryMustPreventRecyclingSameThreeActions() throws Exception {
        JSONArray first=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            AISLA,"Escuto Aisla",new JSONObject(),35);
        assertEquals(4,first.length());
        JSONArray recent=SceneOptionDiversifier.appendHistory(new JSONArray(),first);
        JSONArray second=SceneOptionDiversifier.ensureAlways(new JSONArray(),recent,
            AISLA,"Escuto Aisla",new JSONObject(),36);
        for(int i=1;i<second.length();i++){
            String candidate=second.getJSONObject(i).getString("action");
            for(int j=1;j<first.length();j++)
                assertFalse("Replayed previous suggestion: "+candidate,
                    SceneOptionDiversifier.similar(candidate,first.getJSONObject(j).getString("action")));
        }
    }

    @Test public void A02_unknownNamedNpcMustNotBeAcceptedBecauseAPropIsVisible() throws Exception {
        String narration="Sobre a mesa repousa um pergaminho. Aisla comenta que "+
            "a charada no pergaminho é muito difícil, mas não revela a solução.";
        JSONArray proposed=new JSONArray().put(new JSONObject()
            .put("action_type","OBSERVAR")
            .put("label","OBSERVAR ZANDOR E O PERGAMINHO")
            .put("action","Observo Zandor junto ao pergaminho, procurando detalhes da charada."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(proposed,new JSONArray(),
            narration,"",new JSONObject(),20);
        for(int i=0;i<result.length();i++)
            assertFalse("NPC nonexistent in narration was accepted: "+result,
                result.getJSONObject(i).optString("action").contains("Zandor"));
    }

    @Test public void A03_mentionedDistantShrubsMustNotBecomeReachableProps() throws Exception {
        String narration="Aisla: — Ontem vi os arbustos despertados no Jardim. "+
            "Mas hoje estou aqui no balcão do café com você, Kvothe.";
        JSONArray proposed=new JSONArray().put(new JSONObject()
            .put("action_type","OBSERVAR")
            .put("label","OBSERVAR OS ARBUSTOS COM AISLA")
            .put("action","Observo os arbustos despertados ao lado de Aisla, procurando seus movimentos."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(proposed,new JSONArray(),
            narration,"",new JSONObject(),20);
        for(int i=1;i<result.length();i++)
            assertFalse("Distant shrubs were treated as present: "+result,
                result.getJSONObject(i).optString("action").contains("Observo os arbustos despertados"));
    }

    @Test public void A04_revealedDestinationMustBeLinkedToTheActualClue() {
        String narration="O pergaminho mostra uma velha ponte no mapa da região. "+
            "A pista está decifrada: o texto completo determina que Kvothe "+
            "siga para a torre do observatório e não para a ponte; "+
            "o obstáculo real será localizar a entrada da torre.";
        assertTrue(SceneDiscoveredLead.available(narration));
        assertFalse("Destination used unrelated bridge instead of tower: "+
                SceneDiscoveredLead.destination(narration),
                "a ponte".equals(SceneDiscoveredLead.destination(narration)));
    }

    @Test public void A05_semanticallyDuplicatedChoicesMustNotPassLastMile() throws Exception {
        String narration="A charada do pergaminho está diante de Kvothe no balcão.";
        JSONArray proposed=new JSONArray()
            .put(new JSONObject().put("action_type","OBSERVAR")
                .put("label","OBSERVAR A CHARADA DO PERGAMINHO")
                .put("action","Examino a charada do pergaminho em busca de detalhes."))
            .put(new JSONObject().put("action_type","AGIR")
                .put("label","AGIR SOBRE A CHARADA DO PERGAMINHO")
                .put("action","Inspeciono a charada no pergaminho para buscar detalhes."))
            .put(new JSONObject().put("action_type","DESPISTAR")
                .put("label","DESPISTAR SOBRE A CHARADA DO PERGAMINHO")
                .put("action","Analiso discretamente a charada do pergaminho buscando detalhes."));
        JSONArray result=GroundedSceneActionSlots.contextual(proposed,narration,new JSONObject(),25);
        assertEquals(3,result.length());
        for(int i=0;i<result.length();i++)
            for(int j=i+1;j<result.length();j++)
                assertFalse("Two paraphrases accepted as distinct decisions: "+result,
                    SceneOptionDiversifier.similar(
                        result.getJSONObject(i).optString("action"),
                        result.getJSONObject(j).optString("action")));
    }

    @Test public void A06_waitingGroupWithoutDecisionMustNotTriggerFillerActions() {
        JSONArray result=SceneOptionDiversifier.ensureAlways(
            new JSONArray(),new JSONArray(),"O grupo aguarda.",
            "",new JSONObject(),3);
        assertEquals("No concrete decision is present; only voluntary story-initiative allowed",
            1,result.length());
    }
}
