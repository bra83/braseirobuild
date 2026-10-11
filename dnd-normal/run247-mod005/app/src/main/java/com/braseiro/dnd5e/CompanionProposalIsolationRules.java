package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Set;

/** Pure part of G10 curated-intention isolation, testable without SQLite. */
public final class CompanionProposalIsolationRules {
    private CompanionProposalIsolationRules() {}

    /** Rejects an envelope that turns a curated intention into an unapproved effect. */
    public static String reject(JSONObject envelope, Set<Long> curatedCharacterIds) {
        if (envelope == null || curatedCharacterIds == null || curatedCharacterIds.isEmpty()) return "";
        JSONArray proposals = envelope.optJSONArray("companion_proposals");
        if (proposals == null || proposals.length() == 0) return "";

        boolean curatedIntent = false;
        for (int i = 0; i < proposals.length(); i++) {
            JSONObject proposal = proposals.optJSONObject(i);
            if (proposal != null && curatedCharacterIds.contains(proposal.optLong("character_id", 0))) {
                curatedIntent = true;
                break;
            }
        }
        if (!curatedIntent) return "";

        String[] unsafeArrays = {"state_updates", "tactical_updates", "map_updates",
            "clock_updates", "event_resolutions", "pending_resolutions",
            "world_events", "party_actions", "npc_updates", "npc_cognition_updates",
            "knowledge_updates", "quest_updates", "faction_updates", "guild_updates",
            "journal_updates"};
        for (String key : unsafeArrays) {
            JSONArray values = envelope.optJSONArray(key);
            if (values != null && values.length() > 0) return "curated_intention_mixed_with_" + key;
        }

        if (localTickChanges(envelope.optJSONObject("_local_tick")))
            return "curated_intention_mixed_with_local_tick";
        JSONObject scene = envelope.optJSONObject("scene_state");
        if (scene != null && scene.length() > 0) return "curated_intention_mixed_with_scene_state";
        JSONObject combat = envelope.optJSONObject("combat");
        if (combat != null) {
            String action = combat.optString("action", "none").trim().toLowerCase(Locale.ROOT);
            if (!action.isEmpty() && !"none".equals(action)) return "curated_intention_mixed_with_combat";
            JSONArray updates = combat.optJSONArray("updates");
            JSONArray enemies = combat.optJSONArray("enemies");
            if ((updates != null && updates.length() > 0) ||
                    (enemies != null && enemies.length() > 0))
                return "curated_intention_mixed_with_combat";
        }
        return "";
    }

    private static boolean localTickChanges(JSONObject tick) {
        if (tick == null) return false;
        JSONObject resources = tick.optJSONObject("company_resources_consumed");
        return tick.optInt("hours_advanced", 0) > 0
                || nonEmpty(tick.optJSONArray("events_triggered"))
                || nonEmpty(tick.optJSONArray("clocks_advanced"))
                || (resources != null && resources.optInt("days", 0) > 0);
    }

    private static boolean nonEmpty(JSONArray values) {
        return values != null && values.length() > 0;
    }
}
