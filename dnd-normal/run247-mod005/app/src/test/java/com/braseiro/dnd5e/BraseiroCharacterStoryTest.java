package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class BraseiroCharacterStoryTest {

    private JSONObject story() throws Exception {
        return new JSONObject()
            .put("schema", "braseiro.character-story/v1")
            .put("version", 1)
            .put("timeline", new JSONArray()
                .put(new JSONObject().put("id", "troupe")
                    .put("certainty", "confirmed")
                    .put("event", "Cresceu entre artistas itinerantes."))
                .put(new JSONObject().put("id", "parents")
                    .put("certainty", "confirmed")
                    .put("event", "Os dois pais morreram na tragedia."))
                .put(new JSONObject().put("id", "streets")
                    .put("certainty", "confirmed")
                    .put("event", "Viveu anos nas ruas, com fome, medo, maus-tratos e fugas."))
                .put(new JSONObject().put("id", "nickname")
                    .put("certainty", "confirmed_rumor")
                    .put("event", "O Matador de Rei e um rumor de origem desconhecida.")))
            .put("unresolved", new JSONArray()
                .put(new JSONObject().put("id", "actual_regicide")
                    .put("status", "not_established"))
                .put(new JSONObject().put("id", "cause_of_tragedy")
                    .put("status", "unknown")));
    }

    @Test public void storyReachesNarratorWithoutPersonality() throws Exception {
        JSONObject sheet = new JSONObject().put("name", "Kvothe")
            .put("story_profile", story());
        String context = BraseiroPersonalityProfile.forNarrator(sheet, "O professor escuta um rumor.");
        assertTrue(context.contains("Viveu anos nas ruas"));
        assertTrue(context.contains("Os dois pais morreram"));
        assertTrue(context.contains("confirmed_rumor"));
        assertTrue(context.contains("actual_regicide=not_established"));
        assertTrue(context.contains("NPC so pode conhecer"));
        assertTrue(context.contains("nunca prova de crime"));
    }

    @Test public void legacyHistoryWorksWhenStoryExtensionIsStripped() throws Exception {
        JSONObject sheet = new JSONObject().put("name", "Kvothe")
            .put("history", "Anos nas ruas com fome, frio e medo. A alcunha e um rumor.");
        String context = BraseiroPersonalityProfile.forNarrator(sheet, "A primeira aula.");
        assertTrue(context.contains("Anos nas ruas"));
        assertTrue(context.contains("Rumores nao sao fatos comprovados"));
        assertFalse(context.contains("actual_regicide=confirmed"));
    }

    @Test public void legacySheetJsonAndNoStoryRemainSafe() throws Exception {
        JSONObject saved = new JSONObject().put("history", "Foi artista itinerante.");
        JSONObject wrapper = new JSONObject().put("sheet_json", saved.toString());
        assertTrue(BraseiroPersonalityProfile.forNarrator(wrapper, "cena").contains("Foi artista itinerante."));
        assertEquals("", BraseiroPersonalityProfile.forNarrator(new JSONObject(), "cena"));
    }

    @Test public void personalityAndStoryAreBothUsedWithoutChangingMechanics() throws Exception {
        JSONObject personality = new JSONObject().put("schema", "braseiro.personality/v1")
            .put("traits", new JSONArray().put(new JSONObject()
                .put("id", "lealdade").put("label", "Lealdade")
                .put("value", "Valoriza os poucos amigos.")
                .put("priority", 5).put("enabled", true)));
        JSONObject sheet = new JSONObject().put("name", "Kvothe")
            .put("story_profile", story()).put("personality_profile", personality);
        String context = BraseiroPersonalityProfile.forNarrator(sheet, "Um amigo desapareceu.");
        assertTrue(context.contains("PERSONALIDADE_DO_PERSONAGEM"));
        assertTrue(context.contains("HISTORICO_PERSONAGEM"));
        assertTrue(context.contains("Lealdade"));
        assertTrue(context.contains("Nao decidir sentimentos"));
    }
}
