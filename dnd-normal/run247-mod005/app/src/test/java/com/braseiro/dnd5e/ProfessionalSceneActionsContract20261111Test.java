package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** New contract: grounded, stable and nonmechanical player-action proposals. */
public final class ProfessionalSceneActionsContract20261111Test {
    @Test public void validExistingChoiceIsPreservedOnReload() throws Exception {
        String narration="Uma funcionária anunciou a orientação.";
        JSONArray saved=new JSONArray().put(new JSONObject().put("action_type","AGIR")
            .put("label","Perguntar à funcionária")
            .put("action","Pergunto à funcionária quais orientações foram anunciadas."));
        assertTrue("Valid named entity",SceneActionEvidence.namedReferencesExist(narration,saved.getJSONObject(0).getString("action")));
        assertTrue("Visible entity",SceneOptionVisibleEvidence.supported(saved.getJSONObject(0),narration));
        JSONArray first=StoryMomentumAction.ensureVisible(saved,narration);
        assertEquals(first.toString(),2,first.length());
        assertEquals(first.toString(),StoryMomentumAction.ensureVisible(first,narration).toString());
    }

    @Test public void oldAiTestCannotCreateAnExecutableCard() throws Exception {
        String narration="Uma assistente aguarda junto à prancheta na recepção.";
        JSONObject old=new JSONObject().put("action_type","AGIR")
            .put("label","Perguntar à assistente")
            .put("action","Pergunto à assistente como funciona a recepção.")
            .put("test",new JSONObject().put("required",true).put("dc",99));
        JSONArray safe=StoryMomentumAction.ensureVisible(new JSONArray().put(old),narration);
        assertEquals(2,safe.length());
        assertNull("No mechanically executable LLM test",safe.getJSONObject(1).optJSONObject("test"));
    }

    @Test public void maliciousModelMechanicsNeverReachActionCard() throws Exception {
        String narration="Uma assistente segura uma prancheta na recepção.";
        JSONObject supplied=new JSONObject().put("action_type","AGIR")
            .put("label","Perguntar à assistente")
            .put("action","Pergunto à assistente sobre a recepção.")
            .put("test",new JSONObject().put("dc",99))
            .put("check",new JSONObject().put("skill","deception"))
            .put("roll","natural 20").put("dc",99)
            .put("result","success").put("effects",new JSONObject().put("gold",9999))
            .put("state_patch",new JSONObject().put("clock",999))
            .put("time_delta",999);
        JSONObject safe=SceneActionEvidence.bind(supplied,narration);
        for(String key:new String[]{"test","check","roll","dc","result",
                "effects","state_patch","time_delta"}){
            assertFalse("Model must not control mechanical field: "+key,safe.has(key));
        }
        assertTrue(SceneActionEvidence.ticketMatches(safe,narration));
        assertEquals("Pergunto à assistente sobre a recepção.",
            safe.getString("action"));
    }

    @Test public void firejoltCounterIsIndependentGroundedChoice() throws Exception {
        String narration="O aroma de especiarias e café recém-passado domina o Firejolt Cafe. "+
            "No balcão de cobre, o gerente Ellina serve canecas fumegantes enquanto "+
            "dois estudantes de Prismari discutem sobre um mapa rabiscado na mesa do canto.";
        JSONObject counter=null;
        for(JSONObject item:SceneAffordancePlanner.candidates(narration)){
            if(item.optString("action","").contains("balcão que está à vista"))counter=item;
        }
        assertNotNull("Visible physical counter must generate EXPLORAR option",counter);
        String action=counter.getString("action");
        assertTrue("evidence: "+counter,SceneActionEvidence.valid(counter,narration));
        assertTrue("current beat: "+counter,SceneOptionDiversifier.isRelevantToCurrentBeat(action,narration));
        assertTrue("grounding: "+counter,SceneOptionDiversifier.isGroundedInScene(action,narration,new JSONObject()));
        JSONArray actual=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            narration,"Entro no Firejolt Cafe para observar os estudantes",new JSONObject(),5);
        assertEquals("3 grounded actions plus voluntary momentum: "+actual,4,actual.length());
    }

    @Test public void sceneTicketExpiresOnNarrativeChange() throws Exception {
        String original="Uma assistente consulta uma prancheta na recepção.";
        JSONObject raw=new JSONObject().put("action_type","OBSERVAR")
            .put("label","Examinar a prancheta")
            .put("action","Observo a prancheta da assistente.");
        JSONObject bound=SceneActionEvidence.bind(raw,original);
        assertTrue(SceneActionEvidence.ticketMatches(bound,original));
        assertFalse(SceneActionEvidence.ticketMatches(bound,
            "A assistente já saiu com a prancheta e a recepção está vazia."));
    }

    @Test public void dialogueAboutFarawayShrubsIsNotPhysicalPresence() throws Exception {
        String narration="Aisla: — Ontem vi os arbustos despertados em outro jardim. Aqui temos café.";
        JSONObject physical=new JSONObject().put("action_type","OBSERVAR")
            .put("label","Examinar os arbustos")
            .put("action","Observo os arbustos despertados perto de Aisla.");
        assertFalse(SceneActionEvidence.valid(physical,narration));
        JSONObject conversational=new JSONObject().put("action_type","CONVERSAR")
            .put("label","Perguntar sobre o relato")
            .put("action","Pergunto a Aisla sobre os arbustos despertados que ela viu ontem.");
        assertTrue("Talking about distant shrubs is legitimate",
            SceneActionEvidence.valid(conversational,narration));
    }

    @Test public void titleOnlyMenusDoNotInventNpc() throws Exception {
        String narration="Uma pausa curta na sala vazia, sem ninguém por perto.";
        JSONArray choices=SceneOptionDiversifier.ensureAlways(
            new JSONArray(),new JSONArray(),narration,"",new JSONObject(),10);
        assertEquals(1,choices.length());
        assertTrue(StoryMomentumAction.isRequest(choices.getJSONObject(0).getString("action")));
    }
}
