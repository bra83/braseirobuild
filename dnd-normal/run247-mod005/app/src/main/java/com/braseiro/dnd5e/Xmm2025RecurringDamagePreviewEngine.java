package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Source-pinned proposals for recurring damage ticks; never mutates encounter state. */
public final class Xmm2025RecurringDamagePreviewEngine {
    public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
    private static final String[] RAW={
        "{\"id\":\"Pit Fiend\",\"action\":\"Bite\",\"ptr\":\"/monster/346/action/1/entries/0\",\"damage\":\"6d6\",\"damage_type\":\"poison\",\"tick\":\"target_turn_start\",\"active_condition\":\"poisoned\",\"recovery_save_ability\":\"con\",\"recovery_save_dc\":21,\"auto_success_rounds\":10}",
        "{\"id\":\"Stirge\",\"action\":\"Proboscis\",\"ptr\":\"/monster/417/action/0/entries/0\",\"damage\":\"2d4\",\"damage_type\":\"necrotic\",\"tick\":\"source_turn_start\",\"active_condition\":\"attached\"}",
        "{\"id\":\"Vine Blight\",\"action\":\"Constricting Vine\",\"ptr\":\"/monster/456/action/0/entries/0\",\"damage\":\"1d8\",\"damage_type\":\"bludgeoning\",\"tick\":\"target_turn_start\",\"active_condition\":\"grappled\",\"escape_dc\":12,\"target_max_size\":\"large\"}"
    };
    private static final Map<String,JSONObject> INDEX=new LinkedHashMap<String,JSONObject>();
    private Xmm2025RecurringDamagePreviewEngine(){}
    static{try{for(String raw:RAW){JSONObject row=new JSONObject(raw);
        String key=key(row.optString("id"),row.optString("action"));
        if(INDEX.put(key,row)!=null)throw new IllegalStateException("duplicate_recurring_damage_profile");
    }if(INDEX.size()!=3)throw new IllegalStateException("recurring_damage_profile_count");
    }catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static String key(String monster,String action){return monster+"\u0000"+action;}
    public static JSONArray catalog()throws Exception{JSONArray out=new JSONArray();for(JSONObject row:INDEX.values())out.put(new JSONObject(row.toString()));return out;}
    public static JSONObject profile(String monster,String action)throws Exception{
        JSONObject spec=INDEX.get(key(monster,action));if(spec==null)return new JSONObject();
        if(!compiledMatch(spec))return blocked("compiled_recurring_damage_contract_mismatch");
        return new JSONObject(spec.toString()).put("resolved",true).put("source_blob_sha1",SOURCE_BLOB)
            .put("coverage_status","BLOCKED_MISSING_CONTEXT").put("damage_applied",false)
            .put("condition_applied",false).put("persisted",false).put("auto_mutation_allowed",false);
    }
    public static JSONObject resolve(JSONObject input){
        if(input==null)return blocked("request_required");
        try{
            java.util.Set<String> allowed=new java.util.HashSet<String>(java.util.Arrays.asList(
                "monster","name","action","source_pointer","source_blob_sha1","turn_boundary",
                "lifecycle_active","damage_total"));
            java.util.Iterator<String> keys=input.keys();while(keys.hasNext())
                if(!allowed.contains(keys.next()))return blocked("unrecognized_recurring_damage_input");
            String monster=input.optString("monster",""),action=input.optString("action","");
            if(input.has("name")&&!monster.equals(input.optString("name","")))
                return blocked("cross_monster_input_rejected");
            JSONObject spec=INDEX.get(key(monster,action));if(spec==null)return blocked("unverified_recurring_damage_profile");
            if(!SOURCE_BLOB.equals(input.optString("source_blob_sha1","")))return blocked("source_blob_pin_mismatch");
            if(!spec.optString("ptr").equals(input.optString("source_pointer","")))return blocked("source_pointer_mismatch");
            if(!compiledMatch(spec))return blocked("compiled_recurring_damage_contract_mismatch");
            if(!spec.optString("tick").equals(input.optString("turn_boundary","")))
                return blocked("wrong_turn_boundary");
            if(!(input.opt("lifecycle_active") instanceof Boolean))return blocked("explicit_lifecycle_state_required");
            if(!input.optBoolean("lifecycle_active"))return noTick(spec,"lifecycle_not_active");
            Object raw=input.opt("damage_total");if(!(raw instanceof Number))return blocked("explicit_damage_total_required");
            Number value=(Number)raw;double n=value.doubleValue();
            int[] bounds=bounds(spec.optString("damage"));
            if(bounds==null||!Double.isFinite(n)||n!=Math.floor(n)||n<bounds[0]||n>bounds[1])
                return blocked("damage_total_out_of_source_bounds");
            return new JSONObject().put("resolved",true).put("status","recurring_damage_tick_pending_host_application")
                .put("monster",monster).put("action",action).put("source_pointer",spec.optString("ptr"))
                .put("source_blob_sha1",SOURCE_BLOB).put("turn_boundary",spec.optString("tick"))
                .put("lifecycle_condition",spec.optString("active_condition")).put("damage_expression",spec.optString("damage"))
                .put("damage_total",value.intValue()).put("damage_type",spec.optString("damage_type"))
                .put("defenses_pending",true).put("lifecycle_management_manual",true)
                .put("additional_source_effects_manual",true).put("damage_applied",false)
                .put("condition_applied",false).put("persisted",false).put("auto_rolls",false)
                .put("auto_mutation_allowed",false).put("requires_host_commit",true);
        }catch(Exception failure){return blocked("recurring_damage_resolution_error");}
    }
    private static JSONObject noTick(JSONObject spec,String reason)throws Exception{
        return new JSONObject().put("resolved",true).put("status",reason).put("source_pointer",spec.optString("ptr"))
            .put("proposed_damage",0).put("damage_applied",false).put("condition_applied",false)
            .put("persisted",false).put("auto_rolls",false).put("auto_mutation_allowed",false);
    }
    private static boolean compiledMatch(JSONObject spec)throws Exception{
        if(!CompiledStatblockEngine.has(spec.optString("id")))Xmm2025CompiledBestiaryEngine.install();
        JSONObject block=CompiledStatblockEngine.get(spec.optString("id"));JSONArray actions=block.optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){JSONObject row=actions.optJSONObject(i);
            if(row!=null&&spec.optString("action").equals(row.optString("name")))
                return row.optBoolean("source_bound",false)&&
                    row.optBoolean("manual_unmodeled_source_effect_required",false)&&
                    spec.optString("damage").equals(row.optString("condition_recurring_damage",""))&&
                    spec.optString("damage_type").equals(row.optString("condition_recurring_damage_type",""))&&
                    spec.optString("tick").equals(row.optString("condition_recurring_damage_tick",""));
        }return false;
    }
    private static int[] bounds(String expression){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)d(\\d+)").matcher(expression);
        if(!m.matches())return null;int count=Integer.parseInt(m.group(1)),sides=Integer.parseInt(m.group(2));
        return count<1||sides<2?null:new int[]{count,count*sides};
    }
    private static JSONObject blocked(String reason){try{return new JSONObject().put("resolved",false).put("reason",reason)
        .put("damage_applied",false).put("condition_applied",false).put("persisted",false)
        .put("auto_rolls",false).put("auto_mutation_allowed",false);}catch(Exception ignored){return new JSONObject();}}
}
