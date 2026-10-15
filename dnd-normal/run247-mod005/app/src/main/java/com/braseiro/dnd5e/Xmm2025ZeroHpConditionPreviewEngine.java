package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Source-pinned condition proposals triggered only when an attack reduces a target to 0 HP. */
public final class Xmm2025ZeroHpConditionPreviewEngine {
    public static final String SOURCE_BLOB = "9a4137b508ef37562ed948894a70b1594b57e85c";
    private static final String[] RAW = {
        "{\"id\":\"Phase Spider\",\"action\":\"Bite\",\"ptr\":\"/monster/340/action/1/entries/0\",\"conditions\":\"poisoned, paralyzed\",\"duration_minutes\":60,\"stable\":true,\"linked\":\"paralyzed while poisoned\"}",
        "{\"id\":\"Vampire Familiar\",\"action\":\"Umbral Dagger\",\"ptr\":\"/monster/451/action/1/entries/0\",\"conditions\":\"poisoned, paralyzed\",\"duration_minutes\":60,\"stable\":true,\"linked\":\"paralyzed while poisoned\"}"
    };
    private static final Map<String, JSONObject> INDEX = new LinkedHashMap<>();
    private Xmm2025ZeroHpConditionPreviewEngine() {}
    static {
        try {
            for (String raw : RAW) {
                JSONObject row = new JSONObject(raw);
                if (INDEX.put(key(row.optString("id"), row.optString("action")), row) != null)
                    throw new IllegalStateException("duplicate_zero_hp_profile");
            }
            if (INDEX.size() != 2) throw new IllegalStateException("zero_hp_profile_count");
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static String key(String monster, String action) { return monster + "\u0000" + action; }

    public static JSONArray catalog() throws Exception {
        JSONArray out = new JSONArray();
        for (JSONObject row : INDEX.values()) out.put(new JSONObject(row.toString()));
        return out;
    }

    public static JSONObject profile(String monster, String action) throws Exception {
        JSONObject spec = INDEX.get(key(monster, action));
        if (spec == null) return new JSONObject();
        if (!compiledMatch(spec)) return blocked("compiled_zero_hp_contract_mismatch");
        return new JSONObject(spec.toString()).put("resolved", true)
                .put("source_blob_sha1", SOURCE_BLOB).put("coverage_status", "BLOCKED_MISSING_CONTEXT")
                .put("condition_applied", false).put("persisted", false)
                .put("auto_mutation_allowed", false);
    }

    public static JSONObject resolve(JSONObject input) {
        try {
            if (input == null) return blocked("request_required");
            java.util.Set<String> accepted = new java.util.HashSet<>(java.util.Arrays.asList(
                    "monster", "name", "action", "source_pointer", "source_blob_sha1",
                    "attack_hit", "target_reduced_to_zero_hp"));
            java.util.Iterator<String> keys = input.keys();
            while (keys.hasNext()) if (!accepted.contains(keys.next()))
                return blocked("unrecognized_zero_hp_input");
            String monster = input.optString("monster", ""), action = input.optString("action", "");
            if (input.has("name") && !monster.equals(input.optString("name", "")))
                return blocked("cross_monster_input_rejected");
            JSONObject spec = INDEX.get(key(monster, action));
            if (spec == null) return blocked("unverified_zero_hp_profile");
            if (!SOURCE_BLOB.equals(input.optString("source_blob_sha1", "")))
                return blocked("source_blob_pin_mismatch");
            if (!spec.optString("ptr").equals(input.optString("source_pointer", "")))
                return blocked("source_pointer_mismatch");
            if (!compiledMatch(spec)) return blocked("compiled_zero_hp_contract_mismatch");
            Object rawHit = input.opt("attack_hit"), rawZero = input.opt("target_reduced_to_zero_hp");
            if (!(rawHit instanceof Boolean)) return blocked("explicit_attack_result_required");
            if (!(rawZero instanceof Boolean)) return blocked("explicit_zero_hp_result_required");
            if (Boolean.TRUE.equals(rawZero) && !Boolean.TRUE.equals(rawHit))
                return blocked("zero_hp_without_hit_rejected");
            boolean trigger = Boolean.TRUE.equals(rawHit) && Boolean.TRUE.equals(rawZero);
            JSONArray conditions = new JSONArray();
            if (trigger) for (String condition : spec.optString("conditions").split(",\\s*"))
                conditions.put(condition);
            return new JSONObject().put("resolved", true)
                    .put("status", trigger ? "condition_proposal_pending_host_integration" : "zero_hp_trigger_not_met")
                    .put("monster", monster).put("action", action).put("source_pointer", spec.optString("ptr"))
                    .put("source_blob_sha1", SOURCE_BLOB).put("attack_hit", rawHit)
                    .put("target_reduced_to_zero_hp", rawZero).put("target_stable", trigger && spec.optBoolean("stable"))
                    .put("proposed_conditions", conditions).put("duration_minutes", trigger ? spec.optInt("duration_minutes") : 0)
                    .put("condition_link", trigger ? spec.optString("linked") : "")
                    .put("condition_lifecycle_manual", true).put("full_action_resolution", false)
                    .put("condition_applied", false).put("persisted", false).put("auto_rolls", false)
                    .put("auto_mutation_allowed", false).put("requires_host_commit", trigger);
        } catch (Exception failure) { return blocked("zero_hp_resolution_error"); }
    }

    private static boolean compiledMatch(JSONObject spec) throws Exception {
        if (!CompiledStatblockEngine.has(spec.optString("id"))) Xmm2025CompiledBestiaryEngine.install();
        JSONObject block = CompiledStatblockEngine.get(spec.optString("id"));
        JSONArray actions = block.optJSONArray("actions");
        for (int i = 0; actions != null && i < actions.length(); i++) {
            JSONObject action = actions.optJSONObject(i);
            if (action != null && spec.optString("action").equals(action.optString("name")))
                return action.optBoolean("source_bound", false)
                        && action.optBoolean("manual_unmodeled_source_effect_required", false)
                        && spec.optString("conditions").equals(action.optString("condition_on_target_reduced_to_zero_hp", ""))
                        && spec.optBoolean("stable") == action.optBoolean("target_stable_when_reduced_to_zero_hp", false);
        }
        return false;
    }

    private static JSONObject blocked(String reason) {
        try { return new JSONObject().put("resolved", false).put("reason", reason)
                .put("condition_applied", false).put("persisted", false).put("auto_rolls", false)
                .put("auto_mutation_allowed", false); }
        catch (Exception ignored) { return new JSONObject(); }
    }
}
