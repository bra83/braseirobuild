package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.util.List;

/**
 * Política pura para eventos pessoais do modo Solo.
 *
 * Não altera estado. Centraliza a mesma regra usada pelo diretor e pela UI:
 * somente eventos de companions disponíveis podem entrar no fluxo jogável.
 */
public final class KingmakerSoloEventPolicy {
    private KingmakerSoloEventPolicy() {}

    public static String companionKey(JSONObject brief) {
        if (brief == null) return "";
        String key = brief.optString("companion_key");
        return key.length() > 0 ? key : brief.optString("key");
    }

    public static boolean companionAvailable(String key, List<JSONObject> companions) {
        String wanted = key == null ? "" : key.trim();
        if (wanted.length() == 0 || companions == null) return false;
        for (JSONObject companion : companions) {
            if (wanted.equals(companion.optString("companion_key"))) return companion.optInt("available", 1) == 1;
        }
        return false;
    }

    public static boolean playable(JSONObject event, List<JSONObject> companions) {
        if (event == null || !"companion".equalsIgnoreCase(event.optString("event_type"))) return false;
        String source = event.optString("source_ref");
        if (!source.startsWith("companion:")) return false;
        return companionAvailable(source.substring("companion:".length()), companions);
    }

    public static JSONObject choose(List<JSONObject> pendingEvents, List<JSONObject> companions, long preferredEventId) {
        if (pendingEvents == null) return new JSONObject();
        if (preferredEventId > 0) {
            for (JSONObject event : pendingEvents) {
                if (event.optLong("id") == preferredEventId && playable(event, companions)) return event;
            }
        }
        for (JSONObject event : pendingEvents) if (playable(event, companions)) return event;
        return new JSONObject();
    }
}
