package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** A voluntary progress option must move fiction, never take agency or skip rules. */
public final class StoryMomentumActionRegressionTest {
    @Test public void alwaysPresentEvenWhenModelReturnsNoChoices() throws Exception {
        JSONArray choices=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            "Uma assistente chama calouros no Hall dos Oráculos.", "Entro no salão.",
            new JSONObject().put("location","Biblioplex • Hall dos Oráculos"),4);
        assertTrue(choices.length()>=1);
        assertTrue(StoryMomentumAction.isRequest(choices.getJSONObject(0).getString("action")));
        assertEquals("AGUARDAR A PRÓXIMA INSTRUÇÃO",choices.getJSONObject(0).getString("label"));
    }

    @Test public void twoGroundedOptionsRemainAvailableWithoutFiller() throws Exception {
        JSONArray choices=new JSONArray()
            .put(new JSONObject().put("label","Perguntar sobre o horário")
                .put("action","Pergunto à assistente sobre o horário da orientação."))
            .put(new JSONObject().put("label","Ler os avisos")
                .put("action","Leio os avisos que estão no painel do Hall."));
        JSONArray rendered=StoryMomentumAction.ensureVisible(choices,
            "A assistente aponta o painel de avisos, onde aparece o horário da orientação.");
        assertEquals(3,rendered.length());
        assertTrue(StoryMomentumAction.isRequest(rendered.getJSONObject(0).getString("action")));
        assertEquals("Perguntar sobre o horário",rendered.getJSONObject(1).getString("label"));
        assertEquals("Ler os avisos",rendered.getJSONObject(2).getString("label"));
    }

    @Test public void reloadDoesNotDuplicateOrBuryStoryButton() throws Exception {
        JSONArray once=StoryMomentumAction.ensureVisible(
            new JSONArray().put(new JSONObject().put("label","Ouvir o aviso")
                .put("action","Escuto a mensagem da funcionária.")),
            "Uma funcionária anunciou a orientação.");
        JSONArray twice=StoryMomentumAction.ensureVisible(once,"Uma funcionária anunciou a orientação.");
        assertEquals(2,once.length());
        assertEquals(2,twice.length());
        assertTrue(StoryMomentumAction.isRequest(twice.getJSONObject(0).getString("action")));
        JSONArray history=SceneOptionDiversifier.appendHistory(new JSONArray(),twice);
        assertEquals(1,history.length());
        assertEquals("Escuto a mensagem da funcionária.",history.getString(0));
    }

    @Test public void noStoryBeforeAdventureExists() {
        JSONArray empty=StoryMomentumAction.ensureVisible(new JSONArray(),"");
        assertEquals(0,empty.length());
    }

    @Test public void progressionIsExplicitPlayerActionButNeverAutoplay() {
        assertTrue(StoryMomentumAction.isRequest(StoryMomentumAction.ACTION));
        assertFalse(StoryMomentumAction.isRequest("Pergunto a Rosie sobre o teatro."));
        assertEquals(0d,SceneActionClock.minutesFor(StoryMomentumAction.ACTION),0.0001);
        String strix=StoryMomentumAction.instruction(true);
        assertTrue(strix.contains("UMA batida causal NOVA"));
        assertTrue(strix.contains("não tome")||strix.contains("não fale"));
        assertTrue(strix.contains("não conclua 'Orientation Challenge'"));
        assertTrue(strix.contains("Hall dos Oráculos"));
        assertTrue(strix.contains("sem teletransportá-lo"));
        String other=StoryMomentumAction.instruction(false);
        assertFalse(other.contains("Hall dos Oráculos"));
        assertTrue(other.contains("Não force combate"));
    }

    @Test public void storyButtonIsNeverGeneratedAsModelFiller() throws Exception {
        JSONArray a=new JSONArray().put(StoryMomentumAction.option());
        JSONArray filtered=SceneOptionDiversifier.diversify(a,new JSONArray(),
            "Uma auxiliar anuncia o começo da orientação.","",new JSONObject(),1);
        assertEquals(0,filtered.length()); // dedicated option is injected exactly once elsewhere
        JSONArray rendered=SceneOptionDiversifier.ensureAlways(a,new JSONArray(),
            "Uma auxiliar anuncia o começo da orientação.","",new JSONObject(),1);
        assertEquals(5,rendered.length());
    }
}
