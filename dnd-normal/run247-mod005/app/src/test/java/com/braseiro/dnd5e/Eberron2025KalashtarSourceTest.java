package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Eberron: Forge of the Artificer (2025), Kalashtar / Mind Link.
 * Source: 5etools-mirror-3/5etools-src data/races.json,
 * pinned Git blob 853a6e0f5b25075d826fd005e45276d1e91fe328.
 * Telepathy radius = 10 ft per character level; granting a target the
 * ability to reply telepathically requires a Magic action and lasts 1h.
 */
public final class Eberron2025KalashtarSourceTest {

    @Test public void kalashtarTelepathyMustCheckDistanceAgainstTenTimesLevel() throws Exception {
        JSONObject sheet=new JSONObject();
        JSONObject missing=EberronForgeCompleteEngine.resolveSpeciesAction(
                sheet,"Kalashtar",5,"telepathy",new JSONObject().put("target_id","npc"));
        assertFalse(missing.optBoolean("eberron_resolved"));
        assertEquals("distance_ft_required",missing.optString("eberron_pending"));

        JSONObject tooFar=EberronForgeCompleteEngine.resolveSpeciesAction(
                sheet,"Kalashtar",5,"telepathy",
                new JSONObject().put("target_id","npc").put("distance_ft",51));
        assertFalse(tooFar.optBoolean("eberron_resolved"));
        assertEquals("target_outside_mind_link_range",tooFar.optString("eberron_reason"));

        JSONObject allowed=EberronForgeCompleteEngine.resolveSpeciesAction(
                sheet,"Kalashtar",5,"telepathy",
                new JSONObject().put("target_id","npc").put("distance_ft",50));
        assertTrue(allowed.optBoolean("eberron_resolved"));
        JSONObject effect=allowed.getJSONObject("eberron_effect");
        assertEquals(50,effect.optInt("range_ft"));
        assertEquals(50,effect.optInt("distance_ft"));
        assertFalse(effect.optBoolean("line_of_sight_required",true));
        assertFalse(allowed.optBoolean("eberron_auto_rolls",true));
    }

    @Test public void mindLinkReplyRequiresMagicActionAndOneHourDuration() throws Exception {
        JSONObject source=new JSONObject();
        JSONObject noAction=EberronForgeCompleteEngine.resolveSpeciesAction(
                source,"Kalashtar",4,"mind_link_reply",
                new JSONObject().put("target_id","friend").put("distance_ft",20));
        assertFalse(noAction.optBoolean("eberron_resolved"));
        assertEquals("magic_action_confirmed=true",noAction.optString("eberron_pending"));

        JSONObject outOfRange=EberronForgeCompleteEngine.resolveSpeciesAction(
                source,"Kalashtar",4,"mind_link_reply",
                new JSONObject().put("target_id","friend")
                        .put("distance_ft",41).put("magic_action_confirmed",true));
        assertFalse(outOfRange.optBoolean("eberron_resolved"));

        JSONObject accepted=EberronForgeCompleteEngine.resolveSpeciesAction(
                source,"Kalashtar",4,"mind_link_reply",
                new JSONObject().put("target_id","friend")
                        .put("distance_ft",25).put("magic_action_confirmed",true));
        assertTrue(accepted.optBoolean("eberron_resolved"));
        JSONObject effect=accepted.getJSONObject("eberron_effect");
        assertEquals(40,effect.optInt("range_ft"));
        assertEquals(60,effect.optInt("duration_minutes"));
        assertTrue(effect.optBoolean("magic_action_required"));
        assertEquals("friend",accepted.optString("kalashtar_mind_link_reply_target_id"));
        assertFalse(accepted.optBoolean("eberron_auto_rolls",true));
    }
}
