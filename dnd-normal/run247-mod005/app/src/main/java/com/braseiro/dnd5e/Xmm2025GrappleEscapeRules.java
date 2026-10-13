package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.Locale;

/** Source-bound, manual XMM grapple escape checks; never rolls or mutates state. */
public final class Xmm2025GrappleEscapeRules {
    private Xmm2025GrappleEscapeRules() { }

    public static boolean validSource(String monster, JSONObject action) {
        if(monster==null||action==null||!action.optBoolean("source_bound",false)||
            !action.has("to_hit")||!"grappled".equals(action.optString("condition_on_hit","").trim().toLowerCase(Locale.ROOT))||
            !action.has("escape_dc")||!MonsterOnHitProneRules.validSize(action.optString("condition_max_target_size",""))||
            action.has("condition_on_fail")||action.has("effect_on_fail")||action.has("effect_on_hit")||
            action.has("condition_duration_rounds")||action.has("condition_duration_tick")||
            action.has("save_dc")||action.has("save_ability"))return false;
        try {
            int escapeDc=action.getInt("escape_dc");
            if(escapeDc<1||escapeDc>30)return false;
            JSONObject pinned=Xmm2025AttackConditionEngine.profile(monster,action.optString("name",""));
            return pinned.length()>0&&"grappled".equals(pinned.optString("condition",""))&&
                !pinned.optBoolean("other",true)&&pinned.optInt("dc",0)==escapeDc&&
                pinned.optString("size","").equalsIgnoreCase(action.optString("condition_max_target_size",""));
        } catch(Exception invalid){return false;}
    }

    public static boolean targetEligible(String monster,JSONObject action,String targetSize) {
        return MonsterOnHitProneRules.validSize(targetSize)&&validSource(monster,action)&&
            MonsterOnHitProneRules.sizeRank(targetSize)<=
                MonsterOnHitProneRules.sizeRank(action.optString("condition_max_target_size",""));
    }

    public static String targetBlock(String monster,JSONObject action,String targetSize) {
        if(!MonsterOnHitProneRules.validSize(targetSize))return "target_size_not_verified";
        return validSource(monster,action)?"":"unsupported_grapple_escape_rider";
    }

    public static JSONObject binding(String monster,JSONObject action,long sourceActorId,String receipt)
        throws Exception {
        JSONObject pinned=Xmm2025AttackConditionEngine.profile(monster,
            action==null?"":action.optString("name",""));
        if(!validSource(monster,action)||sourceActorId<=0||receipt==null||receipt.trim().isEmpty())
            throw new IllegalArgumentException("invalid_xmm_grapple_binding");
        return new JSONObject().put("schema","xmm_grapple_escape_v1")
            .put("source_actor_id",sourceActorId).put("source_statblock",monster)
            .put("source_action",action.optString("name","")).put("escape_dc",action.optInt("escape_dc"))
            .put("max_target_size",action.optString("condition_max_target_size"))
            .put("source_pointer",pinned.optString("ptr"))
            .put("source_blob_sha1",Xmm2025AttackConditionEngine.SOURCE_BLOB)
            .put("source_receipt",receipt);
    }

    /** The player supplies the check result; escape uses the XPHB action's two skills. */
    public static JSONObject resolveEscape(String skill,int checkTotal,int escapeDc) {
        JSONObject out=new JSONObject();
        try {
            String selected=skill==null?"":skill.trim().toLowerCase(Locale.ROOT);
            if(!"athletics".equals(selected)&&!"acrobatics".equals(selected))
                return out.put("resolved",false).put("reason","invalid_grapple_escape_skill")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false);
            if(checkTotal< -100||checkTotal>100||escapeDc<1||escapeDc>30)
                return out.put("resolved",false).put("reason","grapple_escape_total_or_dc_out_of_range")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false);
            return out.put("resolved",true).put("check","Strength (Athletics) or Dexterity (Acrobatics)")
                .put("selected_skill",selected).put("check_total",checkTotal).put("escape_dc",escapeDc)
                .put("action_cost","action").put("success",checkTotal>=escapeDc)
                .put("auto_rolls",false).put("auto_mutation_allowed",false);
        } catch(Exception failure){return new JSONObject();}
    }
}
