package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Regression reproduced from the Nov 6 scene: 'guarda o papel' is not a guard NPC. */
public final class GroundedSceneActionSlots6654Test {
    private static final String SILVERQUILL =
        "Kvothe aperta o pergaminho ainda úmido de cera estelar, sentindo o peso do lacre intacto "
        +"enquanto a agitação dos calouros ferve ao redor. A poucos passos, um grupo de estudantes "
        +"de Silverquill examina uma charada inscrita em fitas de tinta voadora. "
        +"Uma das estudantes ergue o queixo ao notar a aproximação de Kvothe e guarda o papel contra o peito. "
        +"Aluna de Silverquill: — Se você procura o salão de poesia ou os testes de caligrafia rúnica, "
        +"vai precisar de mais do que pressa para acompanhar nossa cadência. "
        +"Quer juntar os selos ou veio apenas gastar nosso tempo com prosa vazia?";

    private static JSONArray invalidGuardActions() throws Exception {
        JSONArray invalid=new JSONArray();
        for(String verb:new String[]{"Observo","Pergunto ao","Disfarço o interesse pelo","Aproximo-me do"}){
            String action=verb+" guarda enquanto acompanho seus gestos.";
            invalid.put(new JSONObject().put("label",verb+" guarda").put("action",action));
        }
        return invalid;
    }

    @Test public void onlyFourActionsAndNoInventedGuardFromGuardingVerb() throws Exception {
        JSONObject stale=new JSONObject().put("location","Strixhaven")
            .put("present_npcs_json","[{\"name\":\"o guarda\"}]");
        assertFalse(SceneOptionVisibleEvidence.guardNounPresent(SILVERQUILL));
        JSONArray out=SceneOptionDiversifier.ensureAlways(invalidGuardActions(),new JSONArray(),
            SILVERQUILL,"Escuto a aluna",stale,35);
        assertEquals(out.toString(),4,out.length());
        assertTrue(StoryMomentumAction.isRequest(out.getJSONObject(0).optString("action")));
        assertTrue(out.getJSONObject(0).optString("label").contains("SELOS"));
        Set<String> types=new HashSet<>();
        for(int i=1;i<out.length();i++){
            JSONObject c=out.getJSONObject(i);
            types.add(c.optString("action_type"));
            String shown=(c.optString("label")+" "+c.optString("action")).toLowerCase();
            assertFalse(shown,shown.matches("(?s).*\\bguarda\\b.*"));
            assertTrue(SceneOptionVisibleEvidence.supported(c,SILVERQUILL));
            assertTrue(SceneOptionDiversifier.isGroundedInScene(c.optString("action"),SILVERQUILL,stale));
        }
        assertTrue(types.contains("OBSERVAR"));
        assertTrue(types.contains("AGIR"));
        assertTrue(types.contains("DESPISTAR"));
        assertTrue(out.toString().contains("CHARADA"));
    }

    @Test public void legacySavedGuardMenuIsRevalidatedBeforeRendering() throws Exception {
        JSONArray legacy=invalidGuardActions();
        legacy.put(StoryMomentumAction.option());
        JSONArray safe=StoryMomentumAction.ensureVisible(legacy,SILVERQUILL);
        assertEquals(4,safe.length());
        for(int i=1;i<safe.length();i++)
            assertFalse("A guard NPC must not appear in a saved menu just because a student guarda o papel",
                SceneOptionVisibleEvidence.guardNounPresent(safe.getJSONObject(i).optString("action")));
        assertEquals(safe.toString(),StoryMomentumAction.ensureVisible(safe,SILVERQUILL).toString());
    }

    @Test public void anActuallyPresentGuardIsNotGloballyBanned() throws Exception {
        String scene="Dois guardas observam a ponte e interrogam viajantes. Um dos guardas acena para Kvothe.";
        JSONArray out=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),scene,"",new JSONObject(),4);
        assertEquals(4,out.length());
        assertTrue(SceneOptionVisibleEvidence.guardNounPresent(scene));
        assertTrue(SceneOptionVisibleEvidence.allSupported(out,scene));
    }
}
