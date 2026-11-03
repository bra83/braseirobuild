package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Comparação semântica do checkpoint Solo usado no backup/restore.
 *
 * IDs físicos/timestamps podem mudar numa restauração; fatos que determinam a
 * próxima cena, porém, são autoridade e devem sobreviver exatamente.
 */
public final class KingmakerContinuityContract {
    private static final String[] STABLE_FIELDS = {
            "story_started", "current_hex", "destination_hex", "route_json",
            "scene_day", "scene_period", "scene_weather",
            "kingdom_turn_number", "kingdom_turn_phase",
            "turn_number", "turn_phase", "turn_status", "turn_resource_roll_json",
            "camp_key", "camp_status", "camp_phase", "camp_hex", "camp_day", "camp_hours", "camp_encounter_json",
            "active_companions", "companion_states",
            "pending_companion_events", "pending_companion_event_states",
            "pending_exploration_events", "pending_exploration_event_states",
            "active_wars", "active_war_states",
            "kingdom_action", "next_hex"
    };

    private KingmakerContinuityContract() {}

    public static JSONObject stable(JSONObject checkpoint) throws Exception {
        JSONObject out = new JSONObject();
        JSONObject source = checkpoint == null ? new JSONObject() : checkpoint;
        for (String field : STABLE_FIELDS) {
            Object value = source.opt(field);
            if (value == null || value == JSONObject.NULL) out.put(field, defaultValue(field));
            else out.put(field, normalize(field, value));
        }
        return out;
    }

    public static JSONObject compare(JSONObject before, JSONObject after) throws Exception {
        JSONObject expected = stable(before);
        JSONObject actual = stable(after);
        JSONArray differences = new JSONArray();
        for (String field : STABLE_FIELDS) if (!same(expected.opt(field), actual.opt(field))) differences.put(field);
        return new JSONObject()
                .put("ok", differences.length() == 0)
                .put("differences", differences)
                .put("before", expected)
                .put("after", actual)
                .put("ignored_fields", new JSONArray().put("turn_id").put("timestamps"));
    }

    public static boolean equivalent(JSONObject before, JSONObject after) throws Exception {
        return compare(before, after).optBoolean("ok", false);
    }

    private static Object defaultValue(String field) {
        if (field.endsWith("_states") || "route_json".equals(field) || "active_companions".equals(field)) return new JSONArray();
        if (field.endsWith("_json")) return new JSONObject();
        if (field.endsWith("_events") || "active_wars".equals(field) || field.endsWith("_number") || field.endsWith("_day") || field.endsWith("_hours") || "story_started".equals(field)) return 0;
        return "";
    }

    private static Object normalize(String field, Object value) {
        try {
            if ("route_json".equals(field) && value instanceof String) return new JSONArray(String.valueOf(value));
            if (field.endsWith("_json") && value instanceof String) return new JSONObject(String.valueOf(value));
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private static boolean same(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null || left == JSONObject.NULL || right == JSONObject.NULL) return false;
        if (left instanceof Number && right instanceof Number) return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue()) == 0;
        if (left instanceof JSONObject && right instanceof JSONObject) {
            JSONObject a=(JSONObject)left,b=(JSONObject)right;
            if(a.length()!=b.length())return false;
            Iterator<String> keys=a.keys();
            while(keys.hasNext()){String k=keys.next();if(!b.has(k)||!same(a.opt(k),b.opt(k)))return false;}
            return true;
        }
        if (left instanceof JSONArray && right instanceof JSONArray) {
            JSONArray a=(JSONArray)left,b=(JSONArray)right;if(a.length()!=b.length())return false;
            for(int i=0;i<a.length();i++)if(!same(a.opt(i),b.opt(i)))return false;
            return true;
        }
        return left.toString().equals(right.toString());
    }
}
