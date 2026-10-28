package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Separates the temporary Strixhaven scene from the persisted Faerun scene.
 * Presence is local to a scene; world NPC memory is intentionally untouched.
 * Does not advance time, perform a roll, create an NPC or call any AI service.
 */
public final class StrixhavenSceneBoundary {
    private StrixhavenSceneBoundary() { }

    public static final String SCENE_SNAPSHOT_PREF = "dnd_faerun_scene_before_strixhaven_v1";
    public static final String STORY_SNAPSHOT_PREF = "dnd_faerun_story_before_strixhaven_v1";
    public static final String CAST_SNAPSHOT_PREF = "dnd_faerun_cast_before_strixhaven_v1";

    public static boolean strixhavenLocation(JSONObject scene) {
        if (scene == null) return false;
        String name = scene.optString("location", "").toLowerCase(Locale.ROOT);
        return name.contains("strixhaven") || name.contains("biblioplex")
                || scene.has("strixhaven_map_id") || scene.has("strixhaven_poi_id");
    }

    /** A new physical location cannot inherit visible NPCs of the previous location. */
    public static JSONObject enterNewLocation(JSONObject source) throws JSONException {
        JSONObject next = source == null ? new JSONObject() : new JSONObject(source.toString());
        next.put("present_npcs", new JSONArray());
        next.put("present_npcs_json", "[]");
        return next;
    }

    /**
     * Restore the saved Faerun scene; older installations without a snapshot
     * must not keep Strixhaven's location, NPCs or campus-only fields.
     */
    public static JSONObject returnToFaerun(
            JSONObject previousFaerun, JSONObject activeScene, String atlasLabel,
            String currentAtlasHex) throws JSONException {
        boolean validSnapshot = previousFaerun != null && previousFaerun.length() > 0
                && !strixhavenLocation(previousFaerun);
        JSONObject next = validSnapshot
                ? new JSONObject(previousFaerun.toString())
                : enterNewLocation(activeScene);
        for (String key : new String[] {
                "strixhaven_map_id", "strixhaven_poi_id",
                "strixhaven_exploration_x", "strixhaven_exploration_y",
                "adventure_profile", "world_region"}) {
            next.remove(key);
        }
        // Do not preserve Strixhaven-specific travel labels after returning.
        if (!validSnapshot) {
            next.remove("last_travel_from");
            next.remove("last_travel_to");
            next.remove("last_travel_kind");
            next.remove("last_travel_minutes");
        }
        String location = next.optString("location", "").trim();
        if (location.isEmpty() || strixhavenLocation(next)) {
            String fallback = atlasLabel == null ? "" : atlasLabel.trim();
            if (fallback.isEmpty()) fallback = "Faerûn";
            next.put("location", fallback);
        }
        if (currentAtlasHex != null && !currentAtlasHex.trim().isEmpty()) {
            next.put("hex_id", currentAtlasHex.trim());
        } else if (!validSnapshot) {
            next.remove("hex_id");
        }
        return next;
    }

    public static String waitingForArrival(String location) {
        String where = location == null ? "" : location.trim();
        if (where.isEmpty()) where = "o novo local";
        return "A companhia chegou a " + where
                + ". O Mestre está preparando a descrição deste lugar; "
                + "as pessoas presentes na cena anterior não foram transportadas para cá.";
    }
}
