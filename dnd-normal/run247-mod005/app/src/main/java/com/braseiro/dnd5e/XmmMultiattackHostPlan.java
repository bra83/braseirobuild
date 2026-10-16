package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only source-plan adapter. Execution stays in AppDb's existing transaction. */
public final class XmmMultiattackHostPlan {
    private XmmMultiattackHostPlan() {}
    /** Read-only UI preview; selected context is never an execution authorization.
     * The three-argument host overload below validates real persisted resources. */
    public static JSONObject declaration(String id, JSONObject selection) throws Exception {
        JSONObject previewResources = new JSONObject();
        JSONObject plan = Xmm2025MultiattackPlanIndex.plan(id);
        JSONObject context = plan.optJSONObject("context");
        if (context != null && selection != null) {
            String field = context.optString("field", "");
            Object selected = selection.opt(field);
            if (selected instanceof Boolean) previewResources.put(field, selected);
        }
        JSONObject preview = declaration(id, selection, previewResources);
        if (!preview.has("statblock_multiattack_blocked")) preview.put("preview_only", true);
        return preview;
    }
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
            // Missing legacy state means false; an explicitly persisted JSON null
            // is corrupt state and must never become an authorization decision.
            if (stored != null && !(stored instanceof Boolean))
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
            String kind = step.optString("kind", "attack");
            String actionName = step.getString("action");
            if (!"attack".equals(kind)) {
                if (("spell".equals(kind) || "use".equals(kind)) && actionExistsInStatblock(id, actionName)) {
                    // Permitted hybrid action
                } else {
                    return blocked("multiattack_component_executor_not_installed_" + kind + "_" + i);
                }
            }
            sequence.put(actionName);
        }
        String[] names = new String[sequence.length()];
        for (int i = 0; i < names.length; i++) names[i] = sequence.getString(i);
        String block = MultiattackActionRules.declarationBlock(names, names.length, "", "");
        if (!block.isEmpty()) return blocked(block);
        return new JSONObject().put("count", names.length).put("sequence", sequence)
            .put("source_pointer", resolved.getString("source_pointer"))
            .put("source_blob_sha1", resolved.getString("source_blob_sha1"));
    }

    public static boolean actionExistsInStatblock(String blockId, String actionName) {
        if (blockId == null || actionName == null) return false;
        JSONObject profile = CompiledStatblockEngine.runtimeProfile(blockId);
        if (profile != null) {
            for (String field : new String[]{"actions", "special_actions"}) {
                JSONArray list = profile.optJSONArray(field);
                if (list != null) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject entry = list.optJSONObject(i);
                        if (entry == null) continue;
                        if (actionName.trim().equalsIgnoreCase(entry.optString("name", ""))) return true;
                        JSONArray spells = entry.optJSONArray("spells");
                        if (spells != null) {
                            for (int s = 0; s < spells.length(); s++) {
                                if (actionName.trim().equalsIgnoreCase(spells.optString(s, ""))) return true;
                            }
                        }
                    }
                }
            }
        }
        try {
            JSONObject spellOpts = Xmm2025SpellUsageIndex.options(blockId);
            if (spellOpts.optBoolean("resolved", false)) {
                JSONArray entries = spellOpts.optJSONArray("entries");
                if (entries != null) {
                    for (int e = 0; e < entries.length(); e++) {
                        JSONObject entry = entries.optJSONObject(e);
                        if (entry == null) continue;
                        JSONArray will = entry.optJSONArray("will");
                        if (will != null) {
                            for (int w = 0; w < will.length(); w++) {
                                if (actionName.trim().equalsIgnoreCase(will.optString(w, ""))) return true;
                            }
                        }
                        JSONObject daily = entry.optJSONObject("daily");
                        if (daily != null) {
                            java.util.Iterator<String> it = daily.keys();
                            while (it.hasNext()) {
                                JSONArray arr = daily.optJSONArray(it.next());
                                if (arr != null) {
                                    for (int d = 0; d < arr.length(); d++) {
                                        if (actionName.trim().equalsIgnoreCase(arr.optString(d, ""))) return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    private static JSONObject blocked(String code) throws Exception {
        return new JSONObject().put("statblock_multiattack_blocked", code);
    }
}
