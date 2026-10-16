package com.braseiro.starwarsedge;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists the current external Gemini image target across activity/process recreation. */
final class ExternalImageShareState {
    private static final String PREFS = "braseiro_external_image_share_v1";
    private static final String KEY_KIND = "kind";
    private static final String KEY_ID = "target_id";
    private static final String KEY_AT = "started_at";
    static final String KIND_SCENE = "scene";
    static final String KIND_NPC = "npc";

    static final class Pending {
        final String kind;
        final String targetId;
        final long startedAt;
        Pending(String kind, String targetId, long startedAt) {
            this.kind = kind;
            this.targetId = targetId;
            this.startedAt = startedAt;
        }
    }

    private ExternalImageShareState() {}

    static void setScene(Context context, String sceneKey) { set(context, KIND_SCENE, sceneKey); }
    static void setNpc(Context context, String npcId) { set(context, KIND_NPC, npcId); }

    private static void set(Context context, String kind, String targetId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_KIND, kind)
                .putString(KEY_ID, targetId)
                .putLong(KEY_AT, System.currentTimeMillis())
                .apply();
    }

    static Pending get(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String kind = p.getString(KEY_KIND, "");
        String id = p.getString(KEY_ID, "");
        long at = p.getLong(KEY_AT, 0L);
        if ((!KIND_SCENE.equals(kind) && !KIND_NPC.equals(kind)) || id == null || id.isBlank()) return null;
        return new Pending(kind, id, at);
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
