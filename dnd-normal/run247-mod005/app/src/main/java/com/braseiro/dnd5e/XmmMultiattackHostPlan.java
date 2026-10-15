package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only source-plan adapter. Execution stays in AppDb's existing transaction. */
public final class XmmMultiattackHostPlan {
    private XmmMultiattackHostPlan() {}
    public static JSONObject declaration(String id, JSONObject selection,
            JSONObject persistedActorResources) throws Exception {
        if (selection == null) return blocked("explicit_source_plan_required");
        // The UI may select a source variant, but may not assert a transient
        // monster ability (such as Hasten) that the persisted host has not set.
        JSONObject request = new JSONObject(selection.toString()).put("monster", id);
        JSONObject sourcePlan = Xmm2025MultiattackPlanIndex.plan(id);
        if (!sourcePlan.optBoolean("resolved", false))
            return blocked("source_multiattack_not_registered");
        JSONObject context = sourcePlan.optJSONObject("context");
        if (context != null) {
            String field = context.optString("field", "");
            if (field.isEmpty()) return blocked("multiattack_context_field_invalid");
            Object stored = persistedActorResources == null ? null : persistedActorResources.opt(field);
            if (stored != null && stored != JSONObject.NULL && !(stored instanceof Boolean))
                return blocked("multiattack_context_persisted_invalid");
            boolean hostValue = Boolean.TRUE.equals(stored);
            if (selection.has(field)) {
                Object supplied = selection.opt(field);
                if (!(supplied instanceof Boolean) || ((Boolean) supplied).booleanValue() != hostValue)
                    return blocked("multiattack_context_not_authorized_by_host");
            }
            request.put(field, hostValue);
        }
        JSONObject resolved = Xmm2025MultiattackPlanIndex.resolve(request);
        if (!resolved.optBoolean("resolved")) return blocked(resolved.optString("status"));
        JSONArray steps = resolved.getJSONArray("steps"), sequence = new JSONArray();
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);
            // Spell/use effects cannot be charged separately without breaking atomicity.
            if (!"attack".equals(step.optString("kind")))
                return blocked("multiattack_component_executor_not_installed_" + step.optString("kind") + "_" + i);
            sequence.put(step.getString("action"));
        }
        String[] names = new String[sequence.length()];
        for (int i = 0; i < names.length; i++) names[i] = sequence.getString(i);
        String block = MultiattackActionRules.declarationBlock(names, names.length, "", "");
        if (!block.isEmpty()) return blocked(block);
        return new JSONObject().put("count", names.length).put("sequence", sequence)
            .put("source_pointer", resolved.getString("source_pointer"))
            .put("source_blob_sha1", resolved.getString("source_blob_sha1"));
    }
    private static JSONObject blocked(String code) throws Exception {
        return new JSONObject().put("statblock_multiattack_blocked", code);
    }
}
