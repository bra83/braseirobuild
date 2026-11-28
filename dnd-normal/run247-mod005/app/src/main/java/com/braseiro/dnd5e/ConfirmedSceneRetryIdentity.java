package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * Only a CONFIRMED narration replaces the scene identity for retry.
 * Map label repairs, optional adventure imports and title changes are not
 * accepted scenes and must never strand an unexecuted player action.
 */
public final class ConfirmedSceneRetryIdentity {
    private ConfirmedSceneRetryIdentity(){}

    public static String of(String worldProfile,JSONObject story){
        JSONObject state=story==null?new JSONObject():story;
        return "confirmed-scene-v2|"+(worldProfile==null?"":worldProfile)+"|"+
            state.optInt("started",0)+"|"+state.optString("last_narration","").hashCode();
    }
}
