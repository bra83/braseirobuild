package com.braseiro.dnd5e;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Adapter for Barbara/Gemini suggestions: multiple official XMM rules can be
 * resolved using existing deterministic executors in a single read-only call.
 * Neither RAG content nor model-proposed mutations ever reach AppDb here.
 */
public final class HybridXmmBatchResolutionEngine {
    public static final int MAX_REQUESTS = 24;
    private static final Set<String> OPS = new HashSet<String>(Arrays.asList(
            "source_reference", "save_damage_rule", "save_damage_resolve",
            "trait_profile", "trait_resolve", "recharge",
            "spell_options", "spell_usage_check", "condition_choice_profile",
            "condition_choice_resolve", "multiattack_profile", "multiattack_resolve"));
    private static final Set<String> ENVELOPE_KEYS = new HashSet<String>(Arrays.asList(
            "origin", "source_blob_sha1", "requests"));
    private static final Set<String> REQUEST_KEYS = new HashSet<String>(Arrays.asList(
            "request_id", "book_id", "edition", "monster", "source_pointer",
            "operation", "inputs"));

    private HybridXmmBatchResolutionEngine() {}

    public static JSONObject preview(OfficialBookRuntimeGateway host, JSONObject envelope) {
        JSONObject out = new JSONObject();
        try {
            out.put("source", "XMM").put("edition", "2025")
                    .put("state_authority", "sqlite_host")
                    .put("read_only", true).put("auto_rolls", false)
                    .put("auto_mutation_allowed", false).put("persisted", false)
                    .put("requires_host_commit", true);
            if (host == null || envelope == null)
                return block(out, "missing_host_or_envelope");
            if (!onlyKeys(envelope, ENVELOPE_KEYS))
                return block(out, "unrecognized_or_mutating_envelope_fields");
            String origin = envelope.optString("origin");
            if (!"gemini".equals(origin) && !"barbara".equals(origin)
                    && !"player_preview".equals(origin))
                return block(out, "unrecognized_proposal_origin");
            if (!Xmm2025SourceIndex.SOURCE_BLOB.equals(
                    envelope.optString("source_blob_sha1")))
                return block(out, "source_blob_pin_mismatch");
            JSONArray requests = envelope.optJSONArray("requests");
            if (requests == null || requests.length() == 0
                    || requests.length() > MAX_REQUESTS)
                return block(out, "invalid_batch_size");
            Set<String> seen = new HashSet<String>();
            JSONArray results = new JSONArray();
            int resolved = 0;
            int blocked = 0;
            for (int i = 0; i < requests.length(); i++) {
                JSONObject request = requests.optJSONObject(i);
                String id = request == null ? "" : request.optString("request_id").trim();
                JSONObject decision;
                if (id.length() < 1 || id.length() > 80 || !seen.add(id)) {
                    decision = new JSONObject().put("request_id", id)
                            .put("resolved", false)
                            .put("reason", "missing_or_duplicate_request_id");
                } else {
                    decision = one(host, request);
                }
                if (decision.optBoolean("resolved", false)) resolved++;
                else blocked++;
                results.put(decision);
            }
            return out.put("evaluated", true).put("requested", requests.length())
                    .put("resolved_count", resolved).put("blocked_count", blocked)
                    .put("results", results).put("complete", blocked == 0);
        } catch (Exception failure) {
            try {return block(out, "batch_resolution_error");}
            catch (Exception ignored) {return new JSONObject();}
        }
    }

    private static JSONObject one(OfficialBookRuntimeGateway host,
                                  JSONObject request) throws Exception {
        String requestId = request.optString("request_id");
        JSONObject out = new JSONObject().put("request_id", requestId)
                .put("resolved", false).put("effect_applied", false)
                .put("persisted", false).put("auto_mutation_allowed", false);
        if (!onlyKeys(request, REQUEST_KEYS))
            return block(out, "unrecognized_or_mutating_request_fields");
        if (!"xmm".equals(request.optString("book_id"))
                || !"2025".equals(request.optString("edition")))
            return block(out, "wrong_book_or_rules_edition");
        String monster = request.optString("monster").trim();
        String operation = request.optString("operation").trim();
        String pointer = request.optString("source_pointer").trim();
        if (!OPS.contains(operation)) return block(out, "unapproved_runtime_operation");
        JSONObject source = Xmm2025SourceIndex.get(monster);
        String root = source.optString("ptr");
        if (root.isEmpty() || pointer.length() > 128
                || !(pointer.equals(root) || pointer.startsWith(root + "/")))
            return block(out, "source_pointer_not_bound_to_monster");
        JSONObject supplied = request.optJSONObject("inputs");
        if (supplied == null || mutationKeys(supplied))
            return block(out, "invalid_or_mutating_executor_inputs");
        JSONObject inputs = new JSONObject(supplied.toString());
        if ((inputs.has("monster") && !monster.equals(inputs.optString("monster")))
                || (inputs.has("name") && !monster.equals(inputs.optString("name"))))
            return block(out, "cross_monster_input_rejected");
        inputs.put("monster", monster).put("name", monster);
        if (operation.startsWith("condition_choice_")) {
            JSONObject choice = Xmm2025ConditionChoiceEngine.profile(monster,
                    inputs.optString("action"));
            if (choice.length() == 0 || !pointer.equals(choice.optString("source_pointer")))
                return block(out, "condition_choice_pointer_mismatch");
            if ("condition_choice_profile".equals(operation)) {
                return out.put("resolved", true).put("reason", "deterministic_preview_only")
                    .put("operation", operation).put("monster", monster)
                    .put("source_pointer", pointer).put("runtime_result", choice);
            }
            inputs.put("source_blob_sha1", Xmm2025ConditionChoiceEngine.SOURCE_BLOB)
                .put("source_pointer", pointer);
            JSONObject resolution = Xmm2025ConditionChoiceEngine.resolve(inputs);
            if (!resolution.optBoolean("resolved", false))
                return block(out, resolution.optString("reason", "condition_choice_unresolved"));
            return out.put("resolved", true).put("reason", "deterministic_preview_only")
                .put("operation", operation).put("monster", monster)
                .put("source_pointer", pointer).put("runtime_result", resolution);
        } else if (operation.startsWith("multiattack_")) {
            JSONObject plan = Xmm2025MultiattackPlanIndex.plan(monster);
            if (!plan.optBoolean("resolved", false)
                    || !pointer.equals(plan.optString("ptr")))
                return block(out, "multiattack_pointer_mismatch");
            Set<String> accepted = new HashSet<String>(Arrays.asList(
                    "monster", "name", "variant_index", "selected_actions", "step_outcomes"));
            if (!onlyKeys(inputs, accepted))
                return block(out, "unrecognized_multiattack_inputs");
            JSONObject result;
            if ("multiattack_profile".equals(operation)) {
                result = plan;
            } else {
                result = Xmm2025MultiattackPlanIndex.resolve(inputs);
                if (!result.optBoolean("resolved", false))
                    return block(out, result.optString("status", "multiattack_unresolved"));
            }
            return out.put("resolved", true).put("reason", "deterministic_preview_only")
                .put("operation", operation).put("monster", monster)
                .put("source_pointer", pointer).put("runtime_result", result);
        } else if (operation.startsWith("save_damage_")) {
            String category = inputs.optString("category");
            String action = inputs.optString("action");
            JSONObject rule = Xmm2025SaveDamageEngine.rule(monster, category, action);
            if (!rule.optBoolean("resolved", false)
                    || !pointer.equals(rule.optString("source_pointer")))
                return block(out, "save_damage_rule_pointer_mismatch");
        } else if (operation.startsWith("trait_")) {
            JSONObject trait = Xmm2025TraitMechanicsEngine.profile(
                    monster, inputs.optString("trait"));
            if (trait.length() == 0 || !pointer.equals(trait.optString("ptr")))
                return block(out, "trait_rule_pointer_mismatch");
        } else if (!pointer.equals(root)) {
            return block(out, "operation_requires_exact_monster_pointer");
        }

        JSONObject result = host.execute("xmm", operation, inputs);
        if (result == null || !result.optBoolean("executed", false)
                || result.optBoolean("auto_mutation_allowed", true)
                || result.optBoolean("persisted", false))
            return block(out, "deterministic_executor_rejected_input");

        return out.put("resolved", true).put("reason", "deterministic_preview_only")
                .put("operation", operation).put("monster", monster)
                .put("source_pointer", pointer)
                .put("runtime_result", result.optJSONObject("result") == null
                        ? new JSONObject() : result.optJSONObject("result"));
    }

    private static boolean onlyKeys(JSONObject obj, Set<String> accepted) {
        if (obj == null) return false;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) if (!accepted.contains(keys.next())) return false;
        return true;
    }
    private static boolean mutationKeys(JSONObject obj) {
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equals("state_updates") || key.equals("combat")
                    || key.equals("scene_state") || key.equals("requested_mutation")
                    || key.equals("auto_commit") || key.equals("persist")
                    || key.equals("direct_database_write")
                    || key.equals("set_hp") || key.equals("resources_update")
                    || key.startsWith("delete_")) return true;
        }
        return false;
    }
    private static JSONObject block(JSONObject out, String reason) throws Exception {
        return out.put("evaluated", false).put("resolved", false)
                .put("reason", reason).put("auto_mutation_allowed", false)
                .put("persisted", false);
    }
}
