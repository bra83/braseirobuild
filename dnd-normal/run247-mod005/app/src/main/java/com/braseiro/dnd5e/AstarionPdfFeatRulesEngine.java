package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pure, fail-closed chapter 1 feat adjudication previews.
 *
 * Source: user-provided Astarion's Book of Hungers PDF, Chapter 1, feat rules.
 * Explicit player input is mandatory; no RNG, automatic condition application,
 * resource spending or writes. Caller must implement transactional persistence.
 * Source anchor: https://drive.google.com/file/d/1rDyGkKe_btRHAR1P4xULh5enYEUM5Qgs/view
 */
public final class AstarionPdfFeatRulesEngine {
    public static final String SOURCE_DRIVE_ID="1rDyGkKe_btRHAR1P4xULh5enYEUM5Qgs";

    private static JSONObject preview(String id) throws Exception {
        return new JSONObject()
            .put("source_book","ABH").put("source_section","ch_1")
            .put("source_drive_id",SOURCE_DRIVE_ID).put("feature",id)
            .put("resolved",false).put("auto_rolls",false)
            .put("auto_mutation_allowed",false).put("persisted",false)
            .put("requires_manual_application",true);
    }
    private static JSONObject blocked(JSONObject r,String reason) throws Exception {
        return r.put("reason",reason);
    }
    private static JSONObject accepted(JSONObject r) throws Exception {
        return r.put("resolved",true).put("status","manual_application_required");
    }
    private static boolean available(JSONObject input,String spentKey,int max) {
        int spent=input.optInt(spentKey,-1);
        return max>0 && spent>=0 && spent<max;
    }
    private static boolean ready(JSONObject in) {
        return !in.optBoolean("used_since_rest",true);
    }
    private static boolean triggered(JSONObject in) {
        return in.optBoolean("trigger_confirmed",false);
    }

    public JSONObject evaluate(String id,JSONObject input) {
        JSONObject in=input==null?new JSONObject():input;
        String key=id==null?"":id.trim().toLowerCase();
        try {
            JSONObject r=preview(key);
            switch(key) {
                case "tireless_reveler": {
                    // Ally visible, within 60 ft, spends Heroic Inspiration;
                    // self lacks inspiration; limited PB times per Short/Long Rest.
                    int pb=in.optInt("proficiency_bonus",0);
                    if(!in.optBoolean("ally_visible",false) ||
                            !in.optBoolean("ally_spent_heroic_inspiration",false) ||
                            in.optBoolean("self_has_heroic_inspiration",true) ||
                            in.optInt("distance_ft",Integer.MAX_VALUE)>60 ||
                            in.optInt("distance_ft",-1)<0 ||
                            !available(in,"uses_spent",pb))
                        return blocked(r,"trigger_range_or_uses_invalid");
                    return accepted(r).put("grant_heroic_inspiration",true)
                            .put("uses_spent_after",in.getInt("uses_spent")+1)
                            .put("uses_max",pb).put("reset","short_or_long_rest");
                }
                case "vampire_hunter_vitality_ward": {
                    // Reaction: mitigate incoming Necrotic damage by PB d6s.
                    int pb=in.optInt("proficiency_bonus",0);
                    JSONArray dice=in.optJSONArray("d6_rolls");
                    if(!in.optBoolean("reaction_available",false) || !ready(in) ||
                            in.optInt("incoming_necrotic_damage",0)<=0 ||
                            pb<1 || pb>6 || dice==null || dice.length()!=pb)
                        return blocked(r,"reaction_rest_damage_or_explicit_dice_required");
                    int sum=0;
                    for(int i=0;i<dice.length();i++) {
                        int v=dice.optInt(i,0);
                        if(v<1||v>6) return blocked(r,"invalid_d6_roll");
                        sum+=v;
                    }
                    int dmg=in.getInt("incoming_necrotic_damage");
                    return accepted(r).put("mitigation_roll_total",sum)
                            .put("damage_prevented",Math.min(dmg,sum))
                            .put("damage_after",Math.max(0,dmg-sum))
                            .put("reaction_consumed",true).put("reset","short_or_long_rest");
                }
                case "vampires_plaything_timely_retreat": {
                    int pb=in.optInt("proficiency_bonus",0);
                    String choice=in.optString("choice","");
                    if(!in.optBoolean("bonus_action_available",false) ||
                            !("dash".equals(choice)||"disengage".equals(choice)) ||
                            !available(in,"uses_spent",pb))
                        return blocked(r,"bonus_action_choice_or_uses_invalid");
                    return accepted(r).put("bonus_action_consumed",true).put("granted_action",choice)
                            .put("uses_spent_after",in.getInt("uses_spent")+1)
                            .put("uses_max",pb).put("reset","long_rest");
                }
                case "vampires_plaything_decanting": {
                    String item=in.optString("choice","");
                    if(!in.optBoolean("just_finished_long_rest",false) ||
                            !in.optBoolean("empty_vial_or_flask",false) ||
                            !("potion_of_healing".equals(item)||"antitoxin".equals(item)))
                        return blocked(r,"rest_vial_and_choice_required");
                    return accepted(r).put("item_to_create",item)
                            .put("expiry","end_of_next_long_rest")
                            .put("consumes_empty_vial_or_flask",true);
                }
                case "bomber_far_lobber": {
                    String obj=in.optString("projectile","");
                    int distance=in.optInt("distance_ft",-1);
                    if(!in.optBoolean("taking_attack_action",false) ||
                            !in.optBoolean("target_visible",false) ||
                            !("vial".equals(obj)||"flask".equals(obj)) ||
                            distance<0||distance>40)
                        return blocked(r,"attack_action_visible_target_vial_flask_range_required");
                    return accepted(r).put("allowed_throw",true).put("range_ft",40);
                }
                case "cloying_mists_grasping_mist": {
                    if(!in.optBoolean("fog_cloud_cast",false))
                        return blocked(r,"fog_cloud_cast_required");
                    return accepted(r).put("extinguish_nonmagical_flames_in_sphere",true)
                            .put("non_allied_creature_speed_penalty_ft",5)
                            .put("affects_self_or_allies",false);
                }
                case "delicious_pain_toughened_flesh": {
                    String type=in.optString("incoming_damage_type","");
                    if(!in.optBoolean("reaction_available",false) || !triggered(in) ||
                            !ready(in) ||
                            !("bludgeoning".equals(type)||"piercing".equals(type)||
                                    "slashing".equals(type)))
                        return blocked(r,"reaction_damage_type_or_rest_required");
                    return accepted(r).put("resistance_types",
                                    new JSONArray().put("bludgeoning").put("piercing").put("slashing"))
                            .put("duration","until_start_of_next_turn")
                            .put("reaction_consumed",true).put("reset","short_or_long_rest");
                }
                case "light_bringer_suns_healing": {
                    int sides=in.optInt("hit_die_sides",0),roll=in.optInt("rolled_hit_die",0);
                    if(!in.optBoolean("bonus_action_available",false) ||
                            !in.optBoolean("within_sunlight",false) ||
                            !in.optBoolean("hit_die_available",false) || !ready(in) ||
                            !(sides==6||sides==8||sides==10||sides==12) ||
                            roll<1||roll>sides)
                        return blocked(r,"sunlight_bonus_action_hit_die_or_rest_required");
                    return accepted(r).put("healing",roll).put("hit_die_consumed",1)
                            .put("bonus_action_consumed",true)
                            .put("reset","short_or_long_rest");
                }
                case "love_bites_endearing_pain": {
                    String attack=in.optString("attack_type","");
                    if(!in.optBoolean("bonus_action_available",false) ||
                            !in.optBoolean("damaged_target_immediately_prior",false) ||
                            !ready(in) ||
                            !("melee_weapon".equals(attack)||"unarmed_strike".equals(attack)))
                        return blocked(r,"damage_trigger_bonus_action_or_rest_required");
                    return accepted(r).put("condition","charmed")
                            .put("ends_on","start_of_next_turn_or_damage_from_you_or_allies")
                            .put("bonus_action_consumed",true)
                            .put("reset","short_or_long_rest");
                }
                case "putrefy_necrosis": {
                    if(!in.optBoolean("necrotic_damage_roll",false) ||
                            !in.optBoolean("target_took_damage",false)||!ready(in))
                        return blocked(r,"necrotic_damage_target_or_rest_required");
                    return accepted(r).put("condition","poisoned")
                            .put("target_count",1).put("ends_on","start_of_next_turn")
                            .put("reset","short_or_long_rest");
                }
                case "rebuke_radiant_strike": {
                    String size=in.optString("target_size","").toLowerCase();
                    if(!in.optBoolean("radiant_damage_roll",false) ||
                            !in.optBoolean("target_took_damage",false)||!ready(in) ||
                            !("tiny".equals(size)||"small".equals(size)||
                                    "medium".equals(size)||"large".equals(size)||
                                    "huge".equals(size)))
                        return blocked(r,"radiant_damage_target_size_or_rest_required");
                    return accepted(r).put("condition","prone")
                            .put("target_count",1).put("reset","short_or_long_rest");
                }
                case "treacherous_allure_inevitable_betrayal": {
                    if(!in.optBoolean("target_charmed",false) ||
                            !in.optBoolean("making_attack_roll",false))
                        return blocked(r,"target_must_be_charmed_and_attack_required");
                    return accepted(r).put("attack_roll_advantage",true);
                }
                default:
                    return blocked(r,"feat_feature_not_registered");
            }
        } catch(Exception ex) {
            try { return blocked(preview(key),"runtime_error"); }
            catch(Exception ignored) { return new JSONObject(); }
        }
    }
}
