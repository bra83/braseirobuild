package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only source-plan adapter. Execution stays in AppDb's existing transaction. */
public final class XmmMultiattackHostPlan {
    private XmmMultiattackHostPlan() {}
    public static JSONObject declaration(String id, JSONObject selection) throws Exception {
        if (selection == null) return blocked("explicit_source_plan_required");
        // Actor identity is supplied by persisted host state, never by the request.
        JSONObject request = new JSONObject(selection.toString()).put("monster", id);
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
