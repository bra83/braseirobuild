package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** User screenshot: four action choices at a guarded city gate, with no hallucinated NPC or success. */
public final class OpenWorldFourIconActionsRegressionTest {
    private static final String GLOR=
        "A fila avança devagar diante dos portões. Carroças deixam a estrada principal em direção aos "
        +"estábulos, enquanto funcionários controlam o movimento de mercadorias. "
        +"Um oficial observa sua montaria e ergue a mão, indicando a área destinada aos animais.";

    @Test public void fourDistinctPlayableActionsAtGlorGate() throws Exception {
        JSONArray actions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
                GLOR,"",new JSONObject().put("location","Portões de Glór"),12);
        assertEquals("Should offer one initiative plus THREE grounded decisions: "+actions,4,actions.length());
        assertTrue(StoryMomentumAction.isRequest(actions.getJSONObject(0).getString("action")));
        Set<String> types=new HashSet<>();
        for(int i=1;i<actions.length();i++){
            JSONObject item=actions.getJSONObject(i);
            assertTrue("Not scene grounded: "+item,SceneActionEvidence.valid(item,GLOR));
            assertTrue("Not accepted for real click: "+item,SceneActionEvidence.ticketMatches(item,GLOR));
            assertTrue("Not part of the beat: "+item,SceneOptionDiversifier.isRelevantToCurrentBeat(item.optString("action"),GLOR));
            assertTrue("Duplicate: "+item,types.add(item.optString("action_type")));
            String entire=(item.optString("label")+" "+item.optString("action")).toLowerCase();
            assertFalse("No imaginary guard: "+entire,entire.contains("guarda real"));
            assertFalse("No fictional fixed toll: "+entire,entire.contains("taxa de 5"));
        }
        assertTrue("Missing observation: "+actions,types.contains("OBSERVAR"));
        assertTrue("Missing social decision: "+actions,types.contains("AGIR"));
        assertTrue("Missing second place opportunity: "+actions,types.contains("EXPLORAR"));
    }

    @Test public void sparseSceneDoesNotFabricatePeopleOrProps() throws Exception {
        String scene="Uma breve pausa.";
        JSONArray actions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
                scene,"",new JSONObject().put("present_npcs_json","[{\\\"name\\\":\\\"um oficial\\\"}]"),12);
        assertEquals(1,actions.length());
        assertTrue(StoryMomentumAction.isRequest(actions.optJSONObject(0).optString("action")));
    }

    @Test public void avoidOffstageRumorAndAbsentGuard() {
        String scene="Não há oficiais por perto. Os viajantes contam que em outra cidade há portões e estábulos, mas aqui é apenas uma estrada vazia.";
        for(JSONObject item:SceneOpenWorldActionPlanner.candidates(scene))
            assertFalse(item.optString("action"),item.optString("action").contains("oficial"));
    }

    @Test public void deterministicChoicesRemainCorrectAfterRevalidation() throws Exception {
        JSONArray once=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
                GLOR,"",new JSONObject(),13);
        JSONArray twice=StoryMomentumAction.ensureVisible(once,GLOR);
        assertEquals(once.toString(),twice.toString());
    }
}
