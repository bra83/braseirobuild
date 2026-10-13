package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * XMM 2025 single-condition save riders backed by compiled source-bound actions.
 * Returns explicit, non-mutating proposals. Durations, secondary effects, targeting
 * and resource expenditure remain manual and are never inferred from action names.
 */
public final class Xmm2025SaveConditionEngine {
    private static final Set<String> SIMPLE = new HashSet<>(Arrays.asList(
        "poisoned","charmed","paralyzed","prone","incapacitated","stunned",
        "blinded","frightened","restrained","grappled","unconscious","deafened"));
    private static final Set<String> ABILITIES = new HashSet<>(Arrays.asList(
        "str","dex","con","int","wis","cha"));
    private static final String BOOK = "Monster Manual (2025)";
    private Xmm2025SaveConditionEngine() {}

    private static JSONObject blocked(String reason) {
        JSONObject out = new JSONObject();
        try {
            return out.put("resolved",false).put("reason",reason)
                .put("auto_rolls",false).put("auto_mutation_allowed",false)
                .put("condition_applied",false).put("damage_applied",false);
        } catch (Exception e) { return out; }
    }

    private static boolean pinned(JSONObject monster) {
        JSONObject fidelity = monster.optJSONObject("source_fidelity");
        return BOOK.equals(monster.optString("source_book"))
            && fidelity != null && "XMM".equals(fidelity.optString("source_code"));
    }

    private static JSONObject compiled(String monster) throws Exception {
        JSONObject row = CompiledStatblockEngine.get(monster);
        if (!pinned(row)) {
            if (!Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"))
                return new JSONObject();
            row = CompiledStatblockEngine.get(monster);
        }
        return pinned(row) ? row : new JSONObject();
    }

    private static boolean supported(JSONObject action) {
        String condition = action.optString("condition_on_fail");
        return action.optBoolean("source_bound")
            && action.optBoolean("manual_rider_resolution_required")
            && "action".equals(action.optString("category"))
            && SIMPLE.contains(condition)
            && ABILITIES.contains(action.optString("save_ability"))
            && action.optInt("save_dc",0) > 0
            && action.optInt("save_dc",0) <= 40;
    }

    private static JSONObject find(JSONObject monster,String name) throws Exception {
        JSONArray actions = monster.optJSONArray("actions");
        if (actions == null) return new JSONObject();
        JSONObject found = null;
        for (int i=0; i<actions.length(); i++) {
            JSONObject action = actions.getJSONObject(i);
            if (name.equals(action.optString("name")) && supported(action)) {
                if (found != null) return new JSONObject();
                found = new JSONObject(action.toString()).put("source_action_slot",i);
            }
        }
        return found == null ? new JSONObject() : found;
    }

    public static JSONObject profile(String monster,String action) {
        try {
            JSONObject row = compiled(monster);
            JSONObject a = find(row,action);
            if (a.length()==0) return blocked("unverified_or_complex_save_rider");
            JSONObject basis = row.optJSONObject("mechanical_basis");
            return new JSONObject().put("resolved",true).put("monster",monster)
                .put("action",action).put("save_ability",a.getString("save_ability"))
                .put("save_dc",a.getInt("save_dc"))
                .put("condition_on_failed_save",a.getString("condition_on_fail"))
                .put("attack_hit_required",a.optJSONArray("attack_modes")!=null)
                .put("source_action_slot",a.getInt("source_action_slot"))
                .put("source_locator",basis==null?"":basis.optString("source_locator"))
                .put("source_blob_sha1",Xmm2025AttackConditionEngine.SOURCE_BLOB)
                .put("manual_secondary_effects_required",true)
                .put("auto_rolls",false).put("auto_mutation_allowed",false)
                .put("condition_applied",false);
        } catch (Exception e) { return blocked("source_bound_profile_unavailable"); }
    }

    private static boolean requiredBoolean(JSONObject in,String field) throws Exception {
        if (!(in.opt(field) instanceof Boolean))
            throw new IllegalArgumentException("explicit_boolean_"+field+"_required");
        return in.getBoolean(field);
    }

    public static JSONObject resolve(JSONObject in) {
        try {
            if (in==null) return blocked("request_required");
            Object m=in.opt("monster"), a=in.opt("action");
            if (!(m instanceof String)||!(a instanceof String))
                return blocked("monster_and_action_strings_required");
            JSONObject p=profile((String)m,(String)a);
            if (!p.optBoolean("resolved")) return p;
            boolean creature=requiredBoolean(in,"target_is_creature");
            boolean attack=p.getBoolean("attack_hit_required");
            boolean trigger=requiredBoolean(in,attack?"attack_hit":"target_affected");
            boolean eligible=creature&&trigger;
            JSONArray proposals=new JSONArray();
            JSONObject out=new JSONObject(p.toString())
                .put("target_trigger_satisfied",eligible)
                .put("save_result_supplied",false)
                .put("proposed_conditions",proposals)
                .put("damage_applied",false).put("hp_applied",false)
                .put("turn_advanced",false).put("spell_effects_applied",false);
            if (!eligible)
                return out.put("status","target_not_eligible");
            if (!in.has("save_success"))
                return out.put("status","manual_save_result_required");
            boolean saved=requiredBoolean(in,"save_success");
            out.put("save_result_supplied",true).put("save_success",saved);
            if (!saved) proposals.put(new JSONObject()
                .put("condition",p.getString("condition_on_failed_save"))
                .put("condition_applied",false)
                .put("manual_duration_and_secondary_effect_review_required",true));
            return out.put("status",saved?"save_succeeded_no_condition":
                "failed_save_condition_proposal_pending_manual_application");
        } catch (IllegalArgumentException e) {
            return blocked(e.getMessage()==null?"invalid_explicit_input":e.getMessage());
        } catch (Exception e) { return blocked("save_condition_resolution_error"); }
    }

    public static JSONObject coverage() {
        try {
            if (!Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"))
                return blocked("compiled_bestiary_not_installed");
            JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().getJSONArray("names");
            int actions=0,monsters=0;
            for (int i=0;i<names.length();i++) {
                JSONObject monster=compiled(names.getString(i));
                if (!pinned(monster)) return blocked("source_fidelity_mismatch");
                JSONArray list=monster.optJSONArray("actions");
                if (list==null) continue;
                boolean has=false;
                for (int j=0;j<list.length();j++)
                    if (supported(list.getJSONObject(j))) {actions++;has=true;}
                if (has) monsters++;
            }
            return new JSONObject().put("resolved",true).put("source","XMM")
                .put("source_blob_sha1",Xmm2025AttackConditionEngine.SOURCE_BLOB)
                .put("single_condition_save_actions",actions)
                .put("monsters_with_single_condition_save_actions",monsters)
                .put("full_automation_certified",false).put("auto_rolls",false)
                .put("auto_mutation_allowed",false);
        } catch (Exception e) { return blocked("save_condition_coverage_error"); }
    }
}
