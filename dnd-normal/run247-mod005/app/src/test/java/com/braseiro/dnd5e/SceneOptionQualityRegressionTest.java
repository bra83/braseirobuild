package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regressions for fake or ungrounded action cards in the Strixhaven narrator. */
public final class SceneOptionQualityRegressionTest {
    private static final String BIBLIOPLEX =
        "A luz da manhã escorre pelas frestas dos arcos de mármore do Biblioplex. "
        + "Calouros e veteranos cruzam o saguão. Perto da entrada, balcões de atendimento "
        + "e mesas cobertas de folhetos coloridos disputam a atenção de quem chega "
        + "para o primeiro dia de aula.";

    @Test public void biblioplexOpeningOffersConcreteDifferentActions() {
        JSONArray options = SceneOptionDiversifier.ensureAlways(new JSONArray(),
            new JSONArray(), BIBLIOPLEX, "Acordo e começo o dia",
            new JSONObject(), 1);
        assertTrue("Choices cannot be padded: "+options,options.length()>=2&&options.length()<=4);
        assertTrue(StoryMomentumAction.isRequest(options.optJSONObject(0).optString("action")));
        String first = options.optJSONObject(1).optString("label");
        String second = options.optJSONObject(2).optString("label");
        assertTrue(first, first.contains("FOLHETOS"));
        assertEquals("OBSERVAR",options.optJSONObject(1).optString("action_type"));
        assertEquals("AGIR",options.optJSONObject(2).optString("action_type"));
        // Deception is not a mandatory third option.
        for(int i=1;i<options.length();i++)assertTrue(
            SceneOptionVisibleEvidence.supported(options.optJSONObject(i),BIBLIOPLEX));
        assertFalse(options.toString().contains("TESTAR A CONSEQUÊNCIA"));
        for (int i = 1; i < options.length(); i++) {
            JSONObject choice = options.optJSONObject(i);
            assertTrue(SceneOptionDiversifier.isGroundedInScene(
                choice.optString("action"), BIBLIOPLEX, new JSONObject()));
        }
    }

    @Test public void unknownLocationNeverRecyclesOldActionIntoFakeChoice() {
        JSONArray options = SceneOptionDiversifier.ensureAlways(new JSONArray(),
            new JSONArray(), "Uma breve pausa.", "Investigo a sala anterior",
            new JSONObject(), 2);
        assertEquals("A bare pause provides initiative but no fabricated choices", 1, options.length());
        assertTrue(StoryMomentumAction.isRequest(options.optJSONObject(0).optString("action")));
    }

    @Test public void rejectsWeakGeminiSuggestionInsteadOfDisplayingIt() throws Exception {
        JSONArray source = new JSONArray().put(new JSONObject()
            .put("label", "TESTAR A CONSEQUÊNCIA DA AÇÃO")
            .put("description", "Verificar o que mudou.")
            .put("action", "Observar a consequência imediata do que fiz no Biblioplex."));
        JSONArray options = SceneOptionDiversifier.diversify(source, new JSONArray(),
            "Estou no Biblioplex.", "Entrei no Biblioplex", new JSONObject(), 1);
        assertEquals(0, options.length());
    }
}
