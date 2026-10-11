package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Executable deterministic portions of Tasha's 15 feats. No autonomous rolls or character edits. */
public final class TashaFeatRuntimeEngine {
    public static final String BOOK="Tasha's Cauldron of Everything";
    private TashaFeatRuntimeEngine(){}

    private static final String[] IDS={
            "tasha.feat.artificer_initiate","tasha.feat.chef","tasha.feat.crusher","tasha.feat.eldritch_adept","tasha.feat.fey_touched",
            "tasha.feat.fighting_initiate","tasha.feat.gunner","tasha.feat.metamagic_adept","tasha.feat.piercer","tasha.feat.poisoner",
            "tasha.feat.shadow_touched","tasha.feat.skill_expert","tasha.feat.slasher","tasha.feat.telekinetic","tasha.feat.telepathic"};

    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{JSONArray a=new JSONArray();for(String id:IDS)a.put(id);o.put("id","tasha_feat_runtime").put("book",BOOK).put("status","compiled_phase_1").put("auto_rolls",false).put("procedures",a);}catch(Exception ignored){}return o;}

    public static JSONObject execute(String id,JSONObject in){JSONObject x=in==null?new JSONObject():in;
        if("tasha.feat.artificer_initiate".equals(id))return artificerInitiate(x);
        if("tasha.feat.chef".equals(id))return chef(x);
        if("tasha.feat.crusher".equals(id))return crusher(x);
        if("tasha.feat.eldritch_adept".equals(id))return eldritchAdept(x);
        if("tasha.feat.fey_touched".equals(id))return touched(x,true);
        if("tasha.feat.fighting_initiate".equals(id))return fightingInitiate(x);
        if("tasha.feat.gunner".equals(id))return gunner(x);
        if("tasha.feat.metamagic_adept".equals(id))return metamagicAdept(x);
        if("tasha.feat.piercer".equals(id))return piercer(x);
        if("tasha.feat.poisoner".equals(id))return poisoner(x);
        if("tasha.feat.shadow_touched".equals(id))return touched(x,false);
        if("tasha.feat.skill_expert".equals(id))return skillExpert(x);
        if("tasha.feat.slasher".equals(id))return slasher(x);
        if("tasha.feat.telekinetic".equals(id))return telekinetic(x);
        if("tasha.feat.telepathic".equals(id))return telepathic(x);
        return blocked("unknown_tasha_feat_rule");
    }

    private static JSONObject artificerInitiate(JSONObject in){JSONObject o=base("Artificer Initiate");try{
        if(!in.optBoolean("cantrip_is_artificer",false)||!in.optBoolean("level1_spell_is_artificer",false))return blocked(o,"artificer_cantrip_and_level1_spell_required");
        if(!in.optBoolean("artisan_tool_selected",false))return blocked(o,"artisan_tool_required");
        return o.put("valid",true).put("artificer_cantrips_known",1).put("artificer_level1_spells_known",1).put("spellcasting_ability","INT")
                .put("free_level1_casts_per_long_rest",1).put("can_cast_with_spell_slots",true).put("artisan_tool_proficiency_gain",1)
                .put("chosen_artisan_tool_can_focus_intelligence_spells",true);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject chef(JSONObject in){JSONObject o=base("Chef");try{String mode=norm(in.optString("mode"));int pb=Math.max(0,in.optInt("proficiency_bonus",0));
        o.put("ability_increase_choices",new JSONArray().put("CON").put("WIS")).put("ability_increase",1).put("cook_utensils_proficiency",true);
        if(any(mode,"short rest meal","refeicao descanso curto","meal")){int targets=Math.max(0,in.optInt("targets_count",0));int max=4+pb;if(targets>max)return blocked(o,"targets_exceed_4_plus_proficiency_bonus");boolean spent=in.optBoolean("target_spent_hit_die",false);o.put("max_targets",max).put("targets_valid",true).put("requires_target_spend_at_least_one_hit_die",true).put("extra_healing_die",spent?"1d8":"0");if(spent&&in.has("extra_healing_roll"))o.put("extra_healing",Math.max(1,Math.min(8,in.optInt("extra_healing_roll",1))));return o;}
        if(any(mode,"prepare snacks","preparar petiscos","snacks"))return o.put("snacks_created",pb).put("preparation_time","1 hour of work or a long rest").put("lifetime_hours",8).put("eat_action","bonus_action").put("temporary_hp_per_snack",pb);
        return o.put("max_short_rest_targets",4+pb).put("short_rest_extra_healing","1d8 if target spends at least one Hit Die").put("snacks_created",pb).put("snack_temp_hp",pb).put("requires_mode_for_resolution",true);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject crusher(JSONObject in){JSONObject o=base("Crusher");try{boolean hit=in.optBoolean("bludgeoning_hit",false),once=in.optBoolean("once_per_turn_available",true),crit=in.optBoolean("critical_hit",false);int attacker=sizeRank(in.optString("attacker_size")),target=sizeRank(in.optString("target_size"));boolean sizeOk=attacker>0&&target>0&&target<=attacker+1;return o.put("ability_increase_choices",new JSONArray().put("STR").put("CON")).put("ability_increase",1).put("move_target_5ft_allowed",hit&&once&&sizeOk).put("movement_requires_target_no_more_than_one_size_larger",true).put("critical_advantage_against_target",crit).put("critical_advantage_expires","start_of_your_next_turn");}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject eldritchAdept(JSONObject in){JSONObject o=base("Eldritch Adept");try{if(!in.optBoolean("has_spellcasting_or_pact_magic",false))return blocked(o,"spellcasting_or_pact_magic_prerequisite_required");boolean prereq=in.optBoolean("invocation_has_prerequisite",false);if(prereq&&(!in.optBoolean("is_warlock",false)||!in.optBoolean("invocation_prerequisite_met",false)))return blocked(o,"invocation_prerequisite_requires_eligible_warlock");return o.put("eldritch_invocations_known",1).put("selection_valid",true).put("can_swap_on_character_level_gain",true);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject touched(JSONObject in,boolean fey){JSONObject o=base(fey?"Fey Touched":"Shadow Touched");try{String ability=norm(in.optString("ability"));if(!any(ability,"int","intelligence","inteligencia","wis","wisdom","sabedoria","cha","charisma","carisma"))return blocked(o,"ability_must_be_int_wis_or_cha");String school=norm(in.optString("additional_spell_school"));boolean schoolOk=fey?any(school,"divination","adivinhacao","enchantment","encantamento"):any(school,"illusion","ilusao","necromancy","necromancia");if(!schoolOk)return blocked(o,"invalid_additional_level1_spell_school");if(!in.optBoolean("additional_spell_is_level1",false))return blocked(o,"additional_spell_must_be_level1");return o.put("ability_increase",1).put("ability",ability).put("granted_spell",fey?"Misty Step":"Invisibility").put("additional_level1_spell_valid",true).put("each_spell_free_casts_per_long_rest",1).put("can_cast_with_spell_slots",true).put("spellcasting_ability",ability);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject fightingInitiate(JSONObject in){JSONObject o=base("Fighting Initiate");try{if(!in.optBoolean("martial_weapon_proficient",false))return blocked(o,"martial_weapon_proficiency_prerequisite_required");if(!in.optBoolean("style_is_fighter_option",false))return blocked(o,"fighter_fighting_style_required");if(in.optBoolean("style_already_known",false))return blocked(o,"must_choose_different_fighting_style");return o.put("fighting_styles_known",1).put("selection_valid",true).put("can_swap_style_on_asi_level",true);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject gunner(JSONObject in){JSONObject o=base("Gunner");try{return o.put("dexterity_increase",1).put("firearm_proficiency",true).put("ignore_firearm_loading_property",true).put("ranged_attack_no_disadvantage_from_hostile_within_5ft",true);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject metamagicAdept(JSONObject in){JSONObject o=base("Metamagic Adept");try{if(!in.optBoolean("has_spellcasting_or_pact_magic",false))return blocked(o,"spellcasting_or_pact_magic_prerequisite_required");int selected=Math.max(0,in.optInt("distinct_metamagic_options_selected",0));if(selected!=2)return blocked(o,"exactly_two_distinct_metamagic_options_required");return o.put("metamagic_options_known",2).put("sorcery_points",2).put("sorcery_points_use","metamagic_only").put("recharge","long_rest").put("can_swap_one_option_on_asi_level",true);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject piercer(JSONObject in){JSONObject o=base("Piercer");try{boolean dmg=in.optBoolean("piercing_damage_roll",false),once=in.optBoolean("once_per_turn_available",true),crit=in.optBoolean("critical_hit",false);boolean reroll=in.optBoolean("choose_reroll",false)&&dmg&&once;return o.put("ability_increase_choices",new JSONArray().put("STR").put("DEX")).put("ability_increase",1).put("reroll_one_piercing_damage_die",reroll).put("must_use_new_roll",reroll).put("critical_extra_piercing_damage_die",crit?1:0);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject poisoner(JSONObject in){JSONObject o=base("Poisoner");try{String mode=norm(in.optString("mode"));int pb=Math.max(0,in.optInt("proficiency_bonus",0));o.put("ignore_poison_resistance",true).put("poisoners_kit_proficiency",true).put("apply_poison_action","bonus_action");if(any(mode,"craft","fabricar")){if(in.optDouble("gold_available",0)<50d||in.optDouble("hours_available",0)<1d)return blocked(o,"craft_requires_50_gp_and_1_hour");return o.put("gold_cost",50).put("hours_required",1).put("doses_created",pb);}if(any(mode,"apply","aplicar")){if(!in.optBoolean("bonus_action_available",false))return blocked(o,"bonus_action_required");return o.put("potency_minutes",1).put("constitution_save_dc",14).put("failed_save_damage","2d8 poison").put("failed_save_condition","Poisoned until end of target next turn");}return o.put("craft_doses_formula","proficiency_bonus for 50 GP and 1 hour").put("save_dc",14).put("failed_save_damage","2d8 poison").put("requires_mode_for_resolution",true);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject skillExpert(JSONObject in){JSONObject o=base("Skill Expert");try{String ability=norm(in.optString("ability"));if(!abilityAny(ability))return blocked(o,"valid_ability_required");if(!in.optBoolean("new_skill_selected",false)||!in.optBoolean("expertise_skill_already_proficient",false))return blocked(o,"skill_proficiency_and_expertise_selection_required");return o.put("ability_increase",1).put("ability",ability).put("skill_proficiency_gain",1).put("expertise_gain",1);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject slasher(JSONObject in){JSONObject o=base("Slasher");try{boolean hit=in.optBoolean("slashing_hit",false),once=in.optBoolean("once_per_turn_available",true),crit=in.optBoolean("critical_hit",false);return o.put("ability_increase_choices",new JSONArray().put("STR").put("DEX")).put("ability_increase",1).put("speed_reduction_ft",hit&&once?10:0).put("speed_reduction_expires","start_of_your_next_turn").put("critical_target_attack_disadvantage",crit).put("critical_disadvantage_expires","start_of_your_next_turn");}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject telekinetic(JSONObject in){JSONObject o=base("Telekinetic");try{String ability=norm(in.optString("ability"));if(!any(ability,"int","intelligence","inteligencia","wis","wisdom","sabedoria","cha","charisma","carisma"))return blocked(o,"ability_must_be_int_wis_or_cha");int pb=Math.max(0,in.optInt("proficiency_bonus",0)),mod=in.optInt("ability_modifier",0);boolean shove=in.optBoolean("choose_shove",false);o.put("ability_increase",1).put("ability",ability).put("mage_hand_known",true).put("mage_hand_no_verbal_or_somatic_components",true).put("mage_hand_can_be_invisible",true).put("mage_hand_extra_range_ft_if_already_known",30).put("telekinetic_shove_range_ft",30).put("telekinetic_shove_save_dc",8+pb+mod).put("telekinetic_shove_distance_ft",5);if(shove){if(!in.optBoolean("bonus_action_available",false)||!in.optBoolean("target_visible_within_30ft",false))return blocked(o,"shove_requires_bonus_action_and_visible_target_within_30ft");o.put("shove_allowed",true).put("save","Strength").put("willing_target_can_fail_save",true);}return o;}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject telepathic(JSONObject in){JSONObject o=base("Telepathic");try{String ability=norm(in.optString("ability"));if(!any(ability,"int","intelligence","inteligencia","wis","wisdom","sabedoria","cha","charisma","carisma"))return blocked(o,"ability_must_be_int_wis_or_cha");return o.put("ability_increase",1).put("ability",ability).put("telepathy_range_ft",60).put("telepathy_one_way",true).put("target_must_understand_a_language",true).put("detect_thoughts_free_casts_per_long_rest",1).put("detect_thoughts_no_components",true).put("detect_thoughts_can_use_spell_slots",true).put("detect_thoughts_spellcasting_ability",ability);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject base(String feat){JSONObject o=new JSONObject();try{o.put("book",BOOK).put("pages","80-83").put("feat",feat).put("status","ok").put("auto_rolls",false).put("auto_character_mutation",false);}catch(Exception ignored){}return o;}
    private static JSONObject blocked(String reason){JSONObject o=base("");return blocked(o,reason);}private static JSONObject blocked(JSONObject o,String reason){try{o.put("status","blocked").put("requires_resolution",true).put("reason",reason);}catch(Exception ignored){}return o;}
    private static String norm(String s){return s==null?"":Normalizer.normalize(s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","").trim();}
    private static boolean any(String s,String... xs){for(String x:xs)if(s.equals(norm(x)))return true;return false;}
    private static boolean abilityAny(String a){return any(a,"str","strength","forca","dex","dexterity","destreza","con","constitution","constituicao","int","intelligence","inteligencia","wis","wisdom","sabedoria","cha","charisma","carisma");}
    private static int sizeRank(String s){String x=norm(s);if(any(x,"tiny","minusculo"))return 1;if(any(x,"small","pequeno"))return 2;if(any(x,"medium","medio"))return 3;if(any(x,"large","grande"))return 4;if(any(x,"huge","enorme"))return 5;if(any(x,"gargantuan","colossal"))return 6;return 0;}
}
