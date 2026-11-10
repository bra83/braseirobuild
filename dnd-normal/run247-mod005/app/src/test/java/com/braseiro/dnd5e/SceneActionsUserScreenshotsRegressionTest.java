package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Regression fixtures transcribed from Nov 10 screenshots at 10:06 and 10:08. */
public final class SceneActionsUserScreenshotsRegressionTest {
    private static final String AISLA=
        "Aisla solta uma risada curta e vibrante, afastando uma mecha de cabelo da testa "+
        "enquanto alcança uma caneca esmaltada sob o balcão. Com um gesto rápido ela "+
        "despeja um líquido escuro e denso, empurrando a louça fumegante sobre a madeira "+
        "limpa na direção de Kvothe. — Para um calouro que já começou decifrando versos "+
        "de jardim, você vai precisar de mais do que cafeína pura — diz Aisla. "+
        "— O Jardim dos Livros costuma esconder mais do que raízes e arbustos falantes "+
        "quando os veteranos decidem caprichar na caça. Se a \"folha desperta\" apontada "+
        "pelo enigma for o que estou pensando, melhor terminar isso rápido antes que "+
        "os grupos de Witherbloom ocupem os canteiros centrais.";

    private static final String VELINO=
        "A cera verde-oliva cede à pressão dos polegares de Kvothe, espalhando "+
        "lascas secas sobre o balcão de madeira limpa. Desenrolada, a folha de velino "+
        "exibe versos traçados com tinta escura e espessa, libertando um aroma "+
        "discreto de terra úmida e musgo vegetal.";

    @Test public void aislaDialogueOffersThreeDifferentContextualDecisions() {
        JSONArray result=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            AISLA,"Ouço Aisla",new JSONObject(),35);
        assertEquals(result.toString(),4,result.length());
        assertTrue(StoryMomentumAction.isRequest(result.optJSONObject(0).optString("action")));
        assertTrue(result.optJSONObject(0).optString("label").contains("AISLA"));
        assertEquals("OBSERVAR",result.optJSONObject(1).optString("action_type"));
        assertEquals("AGIR",result.optJSONObject(2).optString("action_type"));
        assertEquals("EXPLORAR",result.optJSONObject(3).optString("action_type"));
        assertTrue(result.optJSONObject(1).optString("action").contains("Aisla"));
        assertTrue(result.optJSONObject(2).optString("action").contains("folha desperta"));
        assertTrue(result.optJSONObject(3).optString("action").contains("Jardim dos Livros"));
        Set<String> actions=new HashSet<>();
        for(int i=1;i<result.length();i++){
            JSONObject o=result.optJSONObject(i);
            assertTrue(actions.add(o.optString("action")));
            assertTrue(SceneOptionVisibleEvidence.supported(o,AISLA));
            String label=o.optString("label").toLowerCase(java.util.Locale.ROOT);
            assertFalse(label,label.contains("observar os arbustos"));
            assertFalse(label,label.contains("interagir com os arbustos"));
            assertFalse(label,label.contains("despistar") && label.contains("arbustos"));
        }
        assertEquals(result.toString(),StoryMomentumAction.ensureVisible(result,AISLA).toString());
    }

    @Test public void physicallyUnrolledVellumNeverBecomesObserveTheSilence() {
        JSONArray result=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            VELINO,"Abro o lacre",new JSONObject(),34);
        assertEquals(result.toString(),4,result.length());
        assertEquals("OBSERVAR",result.optJSONObject(1).optString("action_type"));
        assertEquals("AGIR",result.optJSONObject(2).optString("action_type"));
        assertEquals("INVESTIGAR",result.optJSONObject(3).optString("action_type"));
        assertTrue(result.toString().contains("VELINO"));
        assertFalse(result.toString().toLowerCase(java.util.Locale.ROOT).contains("observar o silêncio"));
        assertFalse(result.toString().toLowerCase(java.util.Locale.ROOT).contains("disfarçar interesse pelo silêncio"));
    }

    @Test public void noRemoteShrubInteractionWhenShrubOnlyMentionedInDialogue() {
        JSONArray proposed=new JSONArray();
        for(String kind:new String[]{"OBSERVAR","AGIR","DESPISTAR"})
            proposed.put(new JSONObject().put("action_type",kind)
                .put("label",kind+" • EXAMINAR OS ARBUSTOS")
                .put("action","Aproximo-me dos arbustos e procuro algo."));
        JSONArray rendered=SceneOptionDiversifier.ensureAlways(proposed,new JSONArray(),AISLA,
            "Converso com Aisla",new JSONObject(),36);
        assertEquals(4,rendered.length());
        for(int i=1;i<4;i++){
            String action=rendered.optJSONObject(i).optString("action").toLowerCase(java.util.Locale.ROOT);
            assertFalse(action,action.contains("aproximo-me dos arbustos"));
        }
    }

    @Test public void noInventedObjectForBriefPause() {
        JSONArray choices=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            "Uma breve pausa.","",new JSONObject(),0);
        assertEquals("There is no physical lead to invent",1,choices.length());
        assertTrue(StoryMomentumAction.isRequest(choices.optJSONObject(0).optString("action")));
    }
}
