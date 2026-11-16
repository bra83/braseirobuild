package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** A fixture with explicit hidden facts MUST be redacted from perceptual prompt. */
public final class FictionObserverProjectionTest {
    @Test public void knownAndHiddenNpcSecretsNeverReachObservationBus()throws Exception{
        String secret="NOME_DO_VILAO_INTERNO_SECRETO";
        JSONObject internal=new JSONObject()
          .put("npcs",new JSONArray().put(new JSONObject().put("name",secret).put("goals","destruir os heróis")))
          .put("npc_cognition",new JSONArray().put(new JSONObject().put("beliefs_json","arma escondida")))
          .put("world_events",new JSONArray().put(new JSONObject().put("description","ambush tonight")))
          .put("story",new JSONObject().put("last_narration","Você vê duas pessoas ao longe."))
          .put("campaign",new JSONObject().put("name","Strixhaven"))
          .put("scene",new JSONObject().put("present_npcs_json","[{\\\"name\\\":\\\""+secret+"\\\"}]"));
        JSONObject scene=new JSONObject().put("day_number",1).put("clock_hour",10.25)
          .put("location","Jardim").put("weather","chuva")
          .put("clues_json",new JSONArray().put("segredo de linhagem"))
          .put("present_npcs_json",new JSONArray().put(
              new JSONObject().put("name",secret)).toString());
        JSONArray visible=new JSONArray().put(new JSONObject()
            .put("stable_ref","npc_aaeeccee").put("name_known_to_observer",""));
        JSONObject out=FictionObserverProjection.bus(internal,scene,visible);
        assertFalse(out.toString().contains(secret));
        assertFalse(out.toString().contains("arma escondida"));
        assertFalse(out.toString().contains("ambush tonight"));
        assertFalse(out.toString().contains("segredo de linhagem"));
        assertEquals(10.25,out.getJSONObject("scene").optDouble("clock_hour"),.01);
        assertTrue(out.optBoolean("observer_only"));
    }
}
