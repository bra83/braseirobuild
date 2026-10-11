package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.Iterator;
import java.util.Locale;

/**
 * Fail-closed acknowledgement of a character transaction.  A denied effect,
 * failed cast, stale action or replay may be reported to the UI but must not
 * commit incidental writes made by its resolver.
 */
public final class CharacterAtomicCommitGuard {
    private CharacterAtomicCommitGuard() {}

    public static String refusal(JSONObject result) {
        if (result == null || result.length() == 0) return "empty_character_result";
        if (result.has("operation_replayed") && result.optBoolean("operation_replayed", false))
            return "character_operation_replayed";
        if (result.has("accepted") && !result.optBoolean("accepted", false))
            return "character_action_not_accepted";
        Iterator<String> keys = result.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String name = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (!(name.endsWith("_blocked") || name.endsWith("_error") ||
                  "operation_blocked".equals(name) || "operation_error".equals(name))) continue;
            Object payload = result.opt(key);
            if (payload == null || payload == JSONObject.NULL) continue;
            if (payload instanceof Boolean && !((Boolean) payload)) continue;
            if (payload instanceof String && ((String) payload).trim().length() == 0) continue;
            return key;
        }
        return "";
    }
}
