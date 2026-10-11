package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Epic Destinies de Adventures in Faerûn usam a progressão normal, não níveis 21–30. */
public final class EpicDestinyRuntimeEngine {
    public JSONObject evaluateProgression(int characterLevel) {
        JSONObject res = new JSONObject();
        try {
            res.put("level", Math.max(1, characterLevel));
            res.put("uses_normal_character_progression", true);
            res.put("epic_tier_21_30", false);
            res.put("authority", "Epic Destinies são arcos com marcos acordados entre Mestre e jogador dentro da progressão normal.");
        } catch (Exception ignored) {}
        return res;
    }
}
