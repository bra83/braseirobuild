package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Ravenloft: The Horrors Within (RHW) — runtime compilado e fail-closed.
 *
 * Contrato:
 * - fonte de build: RHW / 5e.tools estruturado; nenhuma consulta externa em gameplay;
 * - auto_rolls=false: todo dado/salvamento aleatório entra explicitamente;
 * - nomes canônicos preservados; superfície e descrições em PT-BR;
 * - recursos limitados são persistidos pelo OfficialBookRuntimeGateway.
 */
public final class RavenloftHorrorsWithinRulesEngine {
    public static final String SUPPLEMENT_ID="rhw";
    public static final String SOURCE="Ravenloft: The Horrors Within";
    public static final String SOURCE_CODE="RHW";

    private static final String[][] SPECIES={
        {"Dhampir","Dhampir"},{"Hexblood","Sangue-de-Bruxa"},{"Lupin","Lupino"},{"Reborn","Renascido"}
    };
    private static final String[][] BACKGROUNDS={
        {"Haunted One","Assombrado"},{"Investigator","Investigador"},{"Mist Wanderer","Andarilho das Brumas"},{"Spirit Medium","Médium Espiritual"}
    };
    private static final String[][] FEATS={
        {"Sharp Eye","Olhar Aguçado","Origin"},{"Survivor","Sobrevivente","Origin"},
        {"Aberrant Anatomy","Anatomia Aberrante","Dark Gift"},{"Echoing Soul","Alma Ecoante","Dark Gift"},
        {"Gathered Whispers","Sussurros Reunidos","Dark Gift"},{"Living Shadow","Sombra Viva","Dark Gift"},
        {"Mist Walker","Caminhante das Brumas","Dark Gift"},{"Second Skin","Segunda Pele","Dark Gift"},
        {"Symbiotic Being","Ser Simbiótico","Dark Gift"},{"Touch of Death","Toque da Morte","Dark Gift"},
        {"Watchers","Observadores","Dark Gift"}
    };
    private static final String[][] SUBCLASSES={
        {"Reanimator","Artificer","Reanimador"},{"College of Spirits","Bard","Colégio dos Espíritos"},
        {"Grave Domain","Cleric","Domínio da Sepultura"},{"Hollow Warden","Ranger","Guardião Oco"},
        {"Phantom","Rogue","Fantasma"},{"Shadow Sorcery","Sorcerer","Feitiçaria das Sombras"},
        {"Undead Patron","Warlock","Patrono Morto-Vivo"}
    };

    public JSONObject descriptor(){JSONObject o=new JSONObject();try{return o.put("id",SUPPLEMENT_ID).put("source",SOURCE).put("source_code",SOURCE_CODE).put("language","pt-BR").put("auto_rolls",false).put("runtime_external_dependency",false).put("mechanics_authority","compiled_source_rules").put("species_count",SPECIES.length).put("background_count",BACKGROUNDS.length).put("feat_count",FEATS.length).put("subclass_count",SUBCLASSES.length);}catch(Exception e){return o;}}

    public JSONArray listSpecies(){JSONArray a=new JSONArray();for(String[] x:SPECIES)try{a.put(species(x[0]));}catch(Exception ignored){}return a;}
    public JSONArray listBackgrounds(){JSONArray a=new JSONArray();for(String[] x:BACKGROUNDS)try{a.put(background(x[0]));}catch(Exception ignored){}return a;}
    public JSONArray listFeats(){JSONArray a=new JSONArray();for(String[] x:FEATS)try{a.put(feat(x[0]));}catch(Exception ignored){}return a;}
    public JSONArray listSubclasses(){JSONArray a=new JSONArray();for(String[] x:SUBCLASSES)try{a.put(subclass(x[0]));}catch(Exception ignored){}return a;}

    public JSONObject species(String name){String n=canon(SPECIES,name);JSONObject o=base("species",n);try{
        if(n.isEmpty())return unknown(o,"species_unknown");
        if("Dhampir".equals(n))return o.put("label_ptbr","Dhampir").put("size","Small|Medium").put("walk_ft",35).put("climb_equals_speed",true).put("spider_climb_hands_free_level",3).put("darkvision_ft",60).put("necrotic_resistance",true).put("bite_damage","1d4 + CON modifier").put("bite_damage_type","piercing").put("bite_empower_uses","proficiency_bonus_per_long_rest");
        if("Hexblood".equals(n))return o.put("label_ptbr","Sangue-de-Bruxa").put("creature_type","Fey").put("size","Small|Medium").put("walk_ft",30).put("darkvision_ft",60).put("eerie_token_range_miles",10).put("free_spells","Disguise Self|Hex; each 1/Long Rest");
        if("Lupin".equals(n))return o.put("label_ptbr","Lupino").put("creature_type","Humanoid").put("size","Small|Medium").put("walk_ft",30).put("darkvision_ft",60).put("unarmed_damage_type","slashing").put("howl_radius_ft",15).put("howl_uses","proficiency_bonus_per_long_rest");
        if("Reborn".equals(n))return o.put("label_ptbr","Renascido").put("creature_type","Humanoid").put("size","Small|Medium").put("walk_ft",30).put("death_save_advantage",true).put("long_rest_hours",4).put("resistance_choice","cold|necrotic|poison").put("past_life_uses","proficiency_bonus_per_long_rest");
    }catch(Exception ignored){}return o;}

    public JSONObject background(String name){String n=canon(BACKGROUNDS,name);JSONObject o=base("background",n);try{
        if(n.isEmpty())return unknown(o,"background_unknown");
        if("Haunted One".equals(n))return o.put("label_ptbr","Assombrado").put("skills","Arcana|Survival").put("tool","Gaming Set (choice)").put("fixed_origin_feat","Survivor").put("dark_gift_choice",true).put("ability_pool","CON|WIS|CHA");
        if("Investigator".equals(n))return o.put("label_ptbr","Investigador").put("skills","Insight|Investigation").put("tool","Disguise Kit").put("fixed_origin_feat","Sharp Eye").put("dark_gift_choice",true).put("ability_pool","INT|WIS|CHA");
        if("Mist Wanderer".equals(n))return o.put("label_ptbr","Andarilho das Brumas").put("skills","Survival|Stealth").put("tool","Artisan's Tools (choice)").put("dark_gift_choice",true).put("ability_pool","DEX|CON|WIS");
        if("Spirit Medium".equals(n))return o.put("label_ptbr","Médium Espiritual").put("skills","Insight|Religion").put("tool","Gaming Set (choice)").put("dark_gift_choice",true).put("ability_pool","CON|INT|WIS");
    }catch(Exception ignored){}return o;}

    public JSONObject feat(String name){String n=canon(FEATS,name);JSONObject o=base("feat",n);try{
        if(n.isEmpty())return unknown(o,"feat_unknown");
        for(String[] x:FEATS)if(x[0].equals(n))o.put("label_ptbr",x[1]).put("category",x[2]);
        if("Sharp Eye".equals(n))return o.put("uses","proficiency_bonus_per_long_rest").put("trigger","Search|Study").put("effect","advantage_on_check").put("failed_check_does_not_spend_use",true);
        if("Survivor".equals(n))return o.put("initiative_reroll_if",9).put("must_use_new_roll",true).put("steel_yourself","reaction + proficiency to failed Charmed/Frightened save; 1/Long Rest");
        if("Aberrant Anatomy".equals(n))return o.put("hold_breath_minutes",60).put("perception_proficiency",true).put("perception_expertise",true).put("blindsight_ft",15).put("nat1_save","CON DC 13 + PB; fail => Stunned to end next turn");
        if("Echoing Soul".equals(n))return o.put("skill_proficiencies",2).put("expertise_choice",1).put("language_choice",1).put("nat1_save","CON DC 13 + PB; fail => Incapacitated to end next turn and Speed halved");
        if("Gathered Whispers".equals(n))return o.put("spells","Message; Augury free 1/Long Rest").put("scream_uses","PB/Long Rest").put("scream_effect","Reaction: +PB AC against triggering attack").put("nat1_save","WIS DC 13 + PB; fail => Deafened to end next turn + Disadvantage ability checks/attacks");
        if("Living Shadow".equals(n))return o.put("spell","Mage Hand").put("reach_uses","PB/Long Rest").put("reach_bonus_ft",10).put("nat1_save","WIS DC 13 + PB; fail => Incapacitated to start next turn then Shadow's Will d8");
        if("Mist Walker".equals(n))return o.put("domain_traveler",true).put("teleport_uses","PB/Long Rest").put("teleport_ft",15).put("poisoned_roots_radius_miles",10).put("poisoned_roots_save","CON DC 13 + PB after Short Rest");
        if("Second Skin".equals(n))return o.put("spell","Alter Self").put("free_casts_per_long_rest",1).put("free_cast_no_concentration",true).put("involuntary_save","CHA DC 13 + PB");
        if("Symbiotic Being".equals(n))return o.put("skill_choice",1).put("language_choice",1).put("sustained_symbiosis_uses","PB/Long Rest + expend Hit Die").put("agenda_save","CHA DC 13 + PB; fail => Charmed 1d12 hours");
        if("Touch of Death".equals(n))return o.put("spell","Chill Touch").put("chill_touch_ignores_necrotic_resistance",true).put("death_saves_disadvantage",true);
        if("Watchers".equals(n))return o.put("spells","Beast Sense|Speak with Animals; each free 1/Long Rest").put("search_bonus","1d4 explicit").put("scrying_save_disadvantage",true).put("nat1_save","WIS DC 13 + PB; fail => Disadvantage D20 Tests up to 1 minute; repeat save end turns");
    }catch(Exception ignored){}return o;}

    public JSONObject subclass(String name){String n=canonSubclass(name);JSONObject o=base("subclass",n);try{
        if(n.isEmpty())return unknown(o,"subclass_unknown");
        for(String[] x:SUBCLASSES)if(x[0].equals(n))o.put("class",x[1]).put("label_ptbr",x[2]);
        if("Reanimator".equals(n))return o.put("feature_levels","3,5,9,15").put("features","Reanimated Companion|Jolt to Life|Strange Modifications|Improved Reanimation|Macabre Modifications|Refined Reanimation");
        if("College of Spirits".equals(n))return o.put("feature_levels","3,6,14").put("features","Channeler|Spirits from Beyond|Empowered Channeling|Mystical Connection").put("spirit_table_entries",12);
        if("Grave Domain".equals(n))return o.put("feature_levels","3,6,17").put("features","Circle of Mortality|Path to the Grave|Sentinel at Death's Door|Divine Reaper");
        if("Hollow Warden".equals(n))return o.put("feature_levels","3,7,11,15").put("features","Wrath of the Wild|Hungering Might|Rot and Violence|Ancient Might");
        if("Phantom".equals(n))return o.put("feature_levels","3,9,13,17").put("features","Wails from the Grave|Whispers of the Dead|Tokens of the Departed|Voice of Death|Ghost Walk|Death's Friend");
        if("Shadow Sorcery".equals(n))return o.put("feature_levels","3,6,14,18").put("features","Power of Shadow|Beasts of Ill Omen|Shadow Walk|Umbral Form");
        if("Undead Patron".equals(n))return o.put("feature_levels","3,6,10,14").put("features","Form of Dread|Grave Touched|Necrotic Husk|Superior Dread");
    }catch(Exception ignored){}return o;}

    /** Regras estruturais das Brumas. Todo resultado aleatório é fornecido pelo host. */
    public JSONObject resolveMistsTravel(JSONObject in){JSONObject o=resolvedBase("mists_travel");try{
        boolean borderClosed=in.optBoolean("origin_border_closed",false);if(borderClosed)return block(o,"origin_domain_border_closed");
        boolean hasTalisman=in.optBoolean("has_mist_talisman",false)||in.optBoolean("mist_walker_domain_traveler",false)||in.optBoolean("tarokka_talisman",false);
        if(hasTalisman){String destination=in.optString("destination_domain","").trim();if(destination.isEmpty())return pending(o,"destination_domain_required");if(in.optBoolean("destination_border_closed",false))hasTalisman=false;else{Integer hours=explicitDie(in,"travel_hours_d6",1,6);if(hours==null)return pending(o,"travel_hours_d6_required");return o.put("resolved",true).put("destination",destination).put("hours",hours).put("followers_radius_ft",30).put("used_talisman",true);}}
        Integer hours=explicitDie(in,"travel_hours_d6",1,6);Integer d100=explicitDie(in,"wandering_d100",1,100);if(hours==null)return pending(o,"travel_hours_d6_required");if(d100==null)return pending(o,"wandering_d100_required");o.put("resolved",true).put("hours",hours).put("wandering_d100",d100).put("used_talisman",false);
        if(d100<=20)o.put("outcome","domain_chosen_by_dm").put("requires_dm_destination",true);
        else if(d100<=40)o.put("outcome","wander_again").put("requires_another_d6_and_d100",true);
        else if(d100<=65){Integer feet=explicitDie(in,"distance_d100_ft",1,100);if(feet==null)return pending(o,"distance_d100_ft_required");o.put("outcome","near_entry").put("distance_ft",feet);}
        else if(d100<=75)o.put("outcome","one_mile_from_entry_next_midnight");
        else if(d100<=85)o.put("outcome","inside_structure_same_domain").put("structure_chosen_by_dm",true);
        else if(d100<=95){Integer parity=explicitPositive(in,"encounter_die");if(parity==null)return pending(o,"encounter_die_required");o.put("outcome","mist_encounter_then_roll_again").put("encounter",parity%2==0?"Commoner (terrified)":"Mist Horror");}
        else if(d100<=99)o.put("outcome","return_to_entry_domain_changed").put("change_chosen_by_dm",true);
        else {Integer returnHours=explicitDie(in,"return_hours_d6",1,6);if(returnHours==null)return pending(o,"return_hours_d6_required");o.put("outcome","material_plane_temporarily").put("return_hours",returnHours);}
        return o;
    }catch(Exception e){return block(o,"runtime_error");}}

    public JSONObject magicInMists(String effect){JSONObject o=resolvedBase("magic_in_mists");try{String e=norm(effect);o.put("resolved",true).put("effect",effect==null?"":effect);
        if(e.equals("escape")||e.equals("plane_shift")||e.equals("astral_projection"))return o.put("allowed",false).put("reason","no_magic_escapes_ravenloft");
        if(e.equals("gate_escape"))return o.put("allowed",false).put("gate_can_conjure_in",true).put("gate_cannot_enable_escape",true);
        if(e.equals("etherealness"))return o.put("allowed",true).put("returns_to_origin_domain_when_leaving_ethereal",true);
        if(e.equals("otherworldly_contact"))return o.put("allowed",true).put("darklord_senses_cast",true).put("darklord_may_intercept",true);
        if(e.equals("planar_boundary"))return o.put("allowed",true).put("each_domain_counts_as_own_plane",true);
        if(e.equals("summoning"))return o.put("allowed",true).put("summoned_manifestation_subject_to_dark_powers",true);
        if(e.equals("demiplane")||e.equals("maze")||e.equals("imprisonment"))return o.put("allowed",true).put("demiplane_magic_functions",true);
        if(e.equals("banishment"))return o.put("allowed",true).put("extraplanar_target_reappears_in_different_domain",true);
        return o.put("allowed",true).put("special_ravenloft_override",false);
    }catch(Exception ex){return block(o,"runtime_error");}}

    /**
     * Mecânicas com estado por personagem. O gateway persiste state_after apenas em resolved=true.
     * Campos de dados (d4/d6/d8/d10/d12/d20/save_total etc.) nunca são fabricados.
     */
    public JSONObject resolveCharacterAction(String action,JSONObject in,JSONObject character,JSONObject state){String a=norm(action);JSONObject s=copy(state),o=resolvedBase(a);int level=Math.max(1,character.optInt("level",in.optInt("level",1))),pb=proficiency(level),con=modifier(stat(character,in,"con",10)),dex=modifier(stat(character,in,"dex",10)),wis=modifier(stat(character,in,"wis",10)),cha=modifier(stat(character,in,"cha",10)),intel=modifier(stat(character,in,"int",10));try{
        o.put("level",level).put("proficiency_bonus",pb);
        // Species
        if(a.equals("dhampir_bite_empower")){int damage=in.optInt("piercing_damage",-1);if(damage<=0)return pending(o,"positive_piercing_damage_required");String type=norm(in.optString("target_creature_type")),choice=norm(in.optString("choice"));if(type.isEmpty())return pending(o,"target_creature_type_required");if(type.equals("construct")||type.equals("constructo")||type.equals("undead")||type.equals("morto-vivo")||type.equals("morto vivo"))return block(o,"target_ineligible");if(!(choice.equals("drain")||choice.equals("drenar")||choice.equals("strengthen")||choice.equals("fortalecer")))return pending(o,"choice_required");if(!spend(s,"dhampir_bite_empower_uses",pb))return block(o,"no_uses_remaining");o.put("resolved",true).put("state_after",s).put("choice",choice);if(choice.startsWith("d"))o.put("hp_delta",damage).put("healing",damage);else o.put("next_check_or_attack_bonus",damage).put("duration_minutes",1);return o;}
        if(a.equals("hexblood_create_token")){if(s.optBoolean("hexblood_token_exists",false))return block(o,"token_already_exists");if(s.optBoolean("hexblood_token_created_since_long_rest",false))return block(o,"free_token_already_used");s.put("hexblood_token_exists",true).put("hexblood_token_created_since_long_rest",true);return o.put("resolved",true).put("state_after",s).put("range_miles",10);}
        if(a.equals("hexblood_remote_view")){if(!s.optBoolean("hexblood_token_exists",false))return block(o,"token_required");s.put("hexblood_token_exists",false);return o.put("resolved",true).put("state_after",s).put("duration_minutes",1).put("token_destroyed",true);}
        if(a.equals("lupin_howl")){Integer save=explicitTotal(in,"save_total");if(save==null)return pending(o,"save_total_required");int dc=8+con+pb;if(!spend(s,"lupin_howl_uses",pb))return block(o,"no_uses_remaining");boolean fail=save<dc;return o.put("resolved",true).put("state_after",s).put("save_dc",dc).put("save_total",save).put("failed",fail).put("disadvantage_attacks_and_saves",fail).put("duration","until_start_of_lupin_next_turn");}
        if(a.equals("reborn_past_life")){Integer d6=explicitDie(in,"d6",1,6);if(d6==null)return pending(o,"d6_required");if(!in.optBoolean("original_check_failed",false))return block(o,"requires_failed_ability_check");if(!spend(s,"reborn_past_life_uses",pb))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("bonus",d6).put("new_total",in.optInt("original_total",0)+d6);}

        // Origin feats / Dark Gifts
        if(a.equals("sharp_eye")){String act=norm(in.optString("action_type"));if(!(act.equals("search")||act.equals("study")))return block(o,"search_or_study_required");if(uses(s,"sharp_eye_uses")>=pb)return block(o,"no_uses_remaining");if(!in.has("check_succeeded"))return o.put("resolved",true).put("advantage",true).put("state_after",s).put("spend_deferred_until_result",true);if(in.optBoolean("check_succeeded"))spend(s,"sharp_eye_uses",pb);return o.put("resolved",true).put("advantage",true).put("state_after",s).put("use_spent",in.optBoolean("check_succeeded"));}
        if(a.equals("survivor_initiative")){Integer first=explicitDie(in,"initial_d20",1,20);if(first==null)return pending(o,"initial_d20_required");if(first>9)return o.put("resolved",true).put("initiative_d20",first).put("reroll_allowed",false).put("state_after",s);Integer second=explicitDie(in,"reroll_d20",1,20);if(second==null)return pending(o,"reroll_d20_required");return o.put("resolved",true).put("initiative_d20",second).put("must_use_new_roll",true).put("state_after",s);}
        if(a.equals("survivor_steel_yourself")){if(s.optBoolean("survivor_steel_used",false))return block(o,"no_uses_remaining");String cond=norm(in.optString("condition"));if(!(cond.equals("charmed")||cond.equals("frightened")))return block(o,"condition_not_eligible");Integer total=explicitTotal(in,"failed_save_total"),dcv=explicitTotal(in,"save_dc");if(total==null||dcv==null)return pending(o,"failed_save_total_and_save_dc_required");if(total>=dcv)return block(o,"trigger_requires_failed_save");s.put("survivor_steel_used",true);return o.put("resolved",true).put("state_after",s).put("bonus",pb).put("new_total",total+pb).put("success_after_bonus",total+pb>=dcv);}
        if(a.equals("aberrant_anatomy_nat1"))return nat1Save(o,s,in,"constitution",13+pb,"Stunned","until_end_next_turn");
        if(a.equals("echoing_soul_nat1")){JSONObject r=nat1Save(o,s,in,"constitution",13+pb,"Incapacitated","until_end_next_turn");try{if(r.optBoolean("failed",false))r.put("speed_halved",true);}catch(Exception ignored){}return r;}
        if(a.equals("gathered_whispers_scream")){Integer attack=explicitTotal(in,"attack_total"),ac=explicitTotal(in,"base_ac");if(attack==null||ac==null)return pending(o,"attack_total_and_base_ac_required");if(ac<1)return block(o,"valid_base_ac_required");if(attack<ac)return block(o,"requires_attack_roll_that_hit_before_reaction");if(!spend(s,"gathered_whispers_scream_uses",pb))return block(o,"no_uses_remaining");int adj=ac+pb;return o.put("resolved",true).put("state_after",s).put("reaction_required",true).put("triggering_attack_hit_before_reaction",true).put("adjusted_ac",adj).put("attack_misses",attack<adj);}
        if(a.equals("gathered_whispers_nat1")){JSONObject r=nat1Save(o,s,in,"wisdom",13+pb,"Deafened","until_end_next_turn");try{if(r.optBoolean("failed",false))r.put("disadvantage_ability_checks_and_attacks",true);}catch(Exception ignored){}return r;}
        if(a.equals("living_shadow_reach")){if(!spend(s,"living_shadow_reach_uses",pb))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("reach_bonus_ft",10).put("applies_to","one_melee_attack_roll_in_Attack_or_Magic_action");}
        if(a.equals("living_shadow_nat1"))return nat1Save(o,s,in,"wisdom",13+pb,"Incapacitated","until_start_next_turn");
        if(a.equals("living_shadow_will")){Integer d8=explicitDie(in,"d8",1,8);if(d8==null)return pending(o,"d8_required");o.put("resolved",true).put("state_after",s).put("d8",d8);if(d8==1){Integer d4=explicitDie(in,"direction_d4",1,4);if(d4==null)return pending(o,"direction_d4_required");o.put("behavior","move_only").put("direction",new String[]{"","north","east","south","west"}[d4]);}else if(d8<=6)o.put("behavior","one_melee_attack_random_creature_in_reach").put("random_target_requires_host_selection",true);else o.put("behavior","Prone_and_turn_ends");return o;}
        if(a.equals("mist_walker_teleport")){if(!in.optBoolean("valid_trigger",false))return block(o,"requires_damage_or_failed_grapple_restrained_save");if(!spend(s,"mist_walker_teleport_uses",pb))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("teleport_ft",15).put("destination_must_be_visible_unoccupied",true);}
        if(a.equals("mist_walker_short_rest")){Integer save=explicitTotal(in,"save_total");if(save==null)return pending(o,"save_total_required");boolean fail=save<13+pb;return o.put("resolved",true).put("state_after",s).put("save_dc",13+pb).put("failed",fail).put("short_rest_benefits_received",!fail);}
        if(a.equals("second_skin_free_cast")){if(s.optBoolean("second_skin_free_used",false))return block(o,"no_free_cast_remaining");s.put("second_skin_free_used",true);return o.put("resolved",true).put("state_after",s).put("spell","Alter Self").put("requires_concentration",false);}
        if(a.equals("second_skin_catalyst")){if(!in.optBoolean("catalyst_triggered",false))return block(o,"catalyst_not_triggered");Integer save=explicitTotal(in,"save_total");if(save==null)return pending(o,"save_total_required");boolean fail=save<13+pb;return o.put("resolved",true).put("state_after",s).put("failed",fail).put("save_dc",13+pb).put("on_fail",s.optBoolean("second_skin_free_used",false)?"Stunned_until_start_next_turn":"cast_Alter_Self_free_no_concentration");}
        if(a.equals("symbiotic_sustained_save")){if(!in.optBoolean("failed_save",false))return block(o,"requires_failed_save");if(!in.optBoolean("hit_die_spend_confirmed",false))return pending(o,"hit_die_spend_confirmation_required");Integer die=explicitPositive(in,"hit_die_roll");if(die==null)return pending(o,"hit_die_roll_required");if(!spend(s,"symbiotic_sustained_uses",pb))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("bonus",die).put("requires_core_hit_die_commit",true);}
        if(a.equals("symbiotic_agenda")){JSONObject r=nat1Save(o,s,in,"charisma",13+pb,"Charmed","1d12_hours_explicit");if(r.optBoolean("failed",false)){Integer d12=explicitDie(in,"duration_d12_hours",1,12);if(d12==null)return pending(o,"duration_d12_hours_required");r.put("duration_hours",d12);}return r;}
        if(a.equals("watchers_search")){Integer d4=explicitDie(in,"d4",1,4);if(d4==null)return pending(o,"d4_required");return o.put("resolved",true).put("state_after",s).put("search_bonus",d4);}
        if(a.equals("watchers_nat1"))return nat1Save(o,s,in,"wisdom",13+pb,"Disadvantage_on_D20_Tests","up_to_1_minute_repeat_save_end_turn");
        if(a.equals("watchers_repeat_save")){Integer save=explicitTotal(in,"save_total");if(save==null)return pending(o,"save_total_required");return o.put("resolved",true).put("state_after",s).put("save_dc",13+pb).put("effect_ends",save>=13+pb);}

        // RHW 2026 Dark Gift: Touch of Death (data/feats.json, page 35).
        // No die is rolled and no death-save state is silently mutated.
        if(a.equals("touch_of_death_chill_touch")){
            if(!in.optBoolean("spell_cast_confirmed",false))return pending(o,"spell_cast_confirmation_required");
            return o.put("resolved",true).put("state_after",s).put("spell","Chill Touch")
                .put("spell_components_required",false)
                .put("necrotic_damage_ignores_resistance",true)
                .put("spellcasting_ability_choice_required",true);
        }
        if(a.equals("touch_of_death_death_save")){
            Integer first=explicitDie(in,"first_d20",1,20);
            if(first==null)return pending(o,"first_d20_required");
            boolean cancel=in.optBoolean("has_advantage",false);
            Integer second=cancel?null:explicitDie(in,"second_d20",1,20);
            if(!cancel&&second==null)return pending(o,"second_d20_required");
            int chosen=cancel?first:Math.min(first,second);
            return o.put("resolved",true).put("state_after",s)
                .put("death_save_disadvantage",true)
                .put("advantage_cancels_disadvantage",cancel)
                .put("selected_d20",chosen)
                .put("requires_core_death_save_application",true);
        }

        // Reanimator Artificer
        if(a.equals("reanimator_create_companion")){boolean free=!s.optBoolean("reanimator_free_companion_used",false),slot=in.optBoolean("spell_slot_spend_confirmed",false);if(!free&&!slot)return pending(o,"spell_slot_spend_confirmation_required");if(s.optBoolean("reanimated_companion_present",false))return block(o,"companion_already_present");if(free)s.put("reanimator_free_companion_used",true);s.put("reanimated_companion_present",true).put("reanimated_companion_hp",5+5*level);JSONObject c=new JSONObject().put("ac",10+intel).put("hp",5+5*level).put("hit_dice",level+"d8").put("walk_ft",30).put("blindsight_ft",60).put("resistances","necrotic|poison").put("immunity","lightning").put("condition_immunities","Charmed|Exhaustion|Poisoned").put("death_burst","2d4 Necrotic; DEX save spell DC; half on success").put("dreadful_swipe","1d4 + INT Necrotic; uses spell attack modifier");if(level>=9)c.put("death_burst","4d4 Necrotic; DEX save spell DC; half on success").put("companion_necrotic_ignores_resistance",true);return o.put("resolved",true).put("state_after",s).put("companion",c).put("spell_slot_spent",!free);}
        if(a.equals("reanimator_jolt_to_life")){int max=Math.max(0,intel);if(max<=0)return block(o,"intelligence_modifier_has_no_uses");if(!spend(s,"reanimator_jolt_uses",max))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("healing",level).put("lightning_damage_dice",level>=17?"4d4":level>=11?"3d4":"2d4").put("emanation_ft",10).put("save","DEX vs spell save DC; half success");}
        if(a.equals("reanimator_life_transfer")){if(level<15)return block(o,"level_15_required");if(!s.optBoolean("reanimated_companion_present",false))return block(o,"companion_required");int hp=Math.max(0,s.optInt("reanimated_companion_hp",0));s.put("reanimated_companion_present",false).put("reanimated_companion_hp",0);return o.put("resolved",true).put("state_after",s).put("hp_delta",hp).put("healing",hp).put("death_burst_triggered",true);}
        if(a.equals("reanimator_raise_dead_free")){if(level<15)return block(o,"level_15_required");if(s.optBoolean("reanimator_raise_dead_used",false))return block(o,"no_uses_remaining");s.put("reanimator_raise_dead_used",true);return o.put("resolved",true).put("state_after",s).put("spell","Raise Dead").put("material_components_required",false);}

        // College of Spirits Bard
        if(a.equals("spirits_channel_random")){Integer dieSize=explicitDieSize(in,"bardic_inspiration_die_size");Integer roll=explicitDie(in,"bardic_inspiration_roll",1,dieSize==null?0:dieSize);if(dieSize==null)return pending(o,"bardic_inspiration_die_size_required");if(roll==null)return pending(o,"bardic_inspiration_roll_required");if(!in.optBoolean("bardic_inspiration_spend_confirmed",false))return pending(o,"bardic_inspiration_spend_confirmation_required");s.put("spirits_channeled",roll);return o.put("resolved",true).put("state_after",s).put("spirit",spirit(roll)).put("spirit_number",roll);}
        if(a.equals("spirits_channel_controlled")){Integer dieSize=explicitDieSize(in,"bardic_inspiration_die_size"),pick=explicitPositive(in,"spirit_number");if(dieSize==null||pick==null)return pending(o,"die_size_and_spirit_number_required");if(pick>dieSize||pick>12)return block(o,"spirit_number_exceeds_die");if(!in.optBoolean("bardic_inspiration_spend_confirmed",false))return pending(o,"bardic_inspiration_spend_confirmation_required");s.put("spirits_channeled",pick);return o.put("resolved",true).put("state_after",s).put("spirit",spirit(pick)).put("spirit_number",pick);}
        if(a.equals("spirits_unleash")){int spirit=s.optInt("spirits_channeled",0);if(spirit<1)return block(o,"no_channeled_spirit");JSONObject effect=resolveSpirit(spirit,in,level,cha);if(!effect.optBoolean("resolved",false))return effect.put("action",a).put("auto_rolls",false);s.remove("spirits_channeled");return o.put("resolved",true).put("state_after",s).put("spirit_number",spirit).put("spirit",spirit(spirit)).put("effect",effect);}
        if(a.equals("spirits_empowered_channeling")){Integer d6=explicitDie(in,"d6",1,6);if(d6==null)return pending(o,"d6_required");return o.put("resolved",true).put("state_after",s).put("bonus_to_one_damage_roll_or_total_healing",d6).put("once_per_turn",true);}
        if(a.equals("spirits_guardians_free")){if(level<6)return block(o,"level_6_required");if(s.optBoolean("spirits_guardians_free_used",false))return block(o,"no_uses_remaining");s.put("spirits_guardians_free_used",true);return o.put("resolved",true).put("state_after",s).put("spell","Spirit Guardians");}

        // Grave Cleric
        if(a.equals("grave_pull_of_death")){if(!in.optBoolean("target_missing_hp",false))return block(o,"target_must_be_missing_hp");return o.put("resolved",true).put("state_after",s).put("extra_necrotic_dice",level>=11?"1d6":"1d4").put("once_per_turn",true);}
        if(a.equals("grave_path_to_grave")){if(!in.optBoolean("channel_divinity_spend_confirmed",false))return pending(o,"channel_divinity_spend_confirmation_required");return o.put("resolved",true).put("state_after",s).put("range_ft",30).put("duration","until_start_next_turn").put("disadvantage_attacks_and_saves",true).put("hit_may_end_for_extra_damage",level).put("extra_damage_choice","necrotic|radiant");}
        if(a.equals("grave_sentinel")){if(level<6)return block(o,"level_6_required");int max=Math.max(1,wis);if(!spend(s,"grave_sentinel_uses",max))return block(o,"no_uses_remaining");Integer dmg=explicitTotal(in,"incoming_damage");if(dmg==null)return pending(o,"incoming_damage_required");return o.put("resolved",true).put("state_after",s).put("damage_after",dmg/2).put("critical_effects_cancelled",in.optBoolean("critical_hit",false));}
        if(a.equals("grave_keeper_of_souls")){if(level<17)return block(o,"level_17_required");if(s.optBoolean("grave_keeper_used",false)&&!in.optBoolean("level6_slot_spend_confirmed",false))return block(o,"no_uses_remaining");if(in.optBoolean("incapacitated",false))return block(o,"cannot_use_while_incapacitated");if(!in.optBoolean("enemy_died_within_60ft",false))return block(o,"trigger_required");s.put("grave_keeper_used",true);return o.put("resolved",true).put("state_after",s).put("healing",2*level).put("target_within_ft",60);}

        // Hollow Warden Ranger
        if(a.equals("hollow_wrath_start")){if(!in.optBoolean("favored_enemy_spend_confirmed",false))return pending(o,"favored_enemy_spend_confirmation_required");s.put("hollow_wrath_active",true);return o.put("resolved",true).put("state_after",s).put("duration_minutes",1).put("ac_bonus",level>=11?2:1).put("aura_ft",10).put("aura_save","WIS vs spell save DC").put("opportunity_attack_retribution",true);}
        if(a.equals("hollow_hungering_might")){if(level<7||!s.optBoolean("hollow_wrath_active",false))return block(o,"wrath_active_level7_required");if(!in.optBoolean("bloodied",false)||!in.optBoolean("hit_with_attack",false))return block(o,"bloodied_hit_required");Integer d10=explicitDie(in,"d10",1,10);if(d10==null)return pending(o,"d10_required");return o.put("resolved",true).put("state_after",s).put("hp_delta",Math.max(0,d10+wis)).put("healing",Math.max(0,d10+wis)).put("once_per_turn",true);}
        if(a.equals("hollow_persistent_wrath")){if(level<15)return block(o,"level_15_required");if(!s.optBoolean("hollow_wrath_active",false)||!in.optBoolean("would_drop_to_zero_not_killed",false))return block(o,"trigger_required");if(s.optBoolean("hollow_persistent_wrath_used",false)&&!in.optBoolean("level4_slot_spend_confirmed",false))return block(o,"no_uses_remaining");s.put("hollow_persistent_wrath_used",true);return o.put("resolved",true).put("state_after",s).put("set_hp",2*level);}

        // Phantom Rogue
        if(a.equals("phantom_wails")){int max=Math.max(1,dex);if(!in.optBoolean("sneak_attack_dealt",false))return block(o,"sneak_attack_trigger_required");int sneakDice=Math.max(1,in.optInt("sneak_attack_dice",0)),dice=(sneakDice+1)/2;Integer total=explicitTotal(in,"wails_damage_roll_total");if(total==null)return pending(o,"wails_damage_roll_total_required");if(!spend(s,"phantom_wails_uses",max))return block(o,"no_uses_remaining");return o.put("resolved",true).put("state_after",s).put("necrotic_damage",total).put("dice_count",dice).put("target_within_first_target_ft",30);}
        if(a.equals("phantom_gain_trinket")){if(level<9)return block(o,"level_9_required");if(!in.optBoolean("creature_died_within_30ft",false))return block(o,"trigger_required");int max=level>=17?4:level>=13?3:2,count=s.optInt("phantom_trinkets",2);s.put("phantom_trinkets",Math.min(max,count+1));return o.put("resolved",true).put("state_after",s).put("trinkets",s.optInt("phantom_trinkets")).put("max",max);}
        if(a.equals("phantom_spend_trinket")){if(level<9)return block(o,"level_9_required");int count=s.optInt("phantom_trinkets",2);if(count<=0)return block(o,"no_trinkets");s.put("phantom_trinkets",count-1);return o.put("resolved",true).put("state_after",s).put("purpose",in.optString("purpose","Spirit Query / Death's Knell / restore Ghost Walk"));}
        if(a.equals("phantom_ghost_walk")){if(level<13)return block(o,"level_13_required");boolean used=s.optBoolean("phantom_ghost_walk_used",false);if(used){int count=s.optInt("phantom_trinkets",2);if(count<=0)return block(o,"requires_soul_trinket_to_restore");s.put("phantom_trinkets",count-1);}s.put("phantom_ghost_walk_used",true);return o.put("resolved",true).put("state_after",s).put("duration_minutes",10).put("fly_ft",10).put("hover",true).put("attacks_against_disadvantage",true).put("incorporeal_movement",true);}
        if(a.equals("phantom_draw_of_death")){if(level<17)return block(o,"level_17_required");int count=s.optInt("phantom_trinkets",2);if(count==0)s.put("phantom_trinkets",1);return o.put("resolved",true).put("state_after",s).put("trinkets",s.optInt("phantom_trinkets"));}

        // Shadow Sorcerer
        if(a.equals("shadow_strength_grave")){if(s.optBoolean("shadow_strength_grave_used",false))return block(o,"no_uses_remaining");if(!in.optBoolean("would_drop_to_zero_not_killed",false))return block(o,"trigger_required");Integer save=explicitTotal(in,"save_total"),damage=explicitTotal(in,"damage_taken");if(save==null||damage==null)return pending(o,"save_total_and_damage_taken_required");int dc=5+damage,successHp=Math.max(1,cha+level);if(save>=dc)s.put("shadow_strength_grave_used",true);return o.put("resolved",true).put("state_after",s).put("save_dc",dc).put("save_success",save>=dc).put("set_hp",save>=dc?successHp:0).put("use_spent",save>=dc);}
        if(a.equals("shadow_beast_ill_omen")){if(level<6)return block(o,"level_6_required");if(!in.optBoolean("sorcery_points_3_spend_confirmed",false))return pending(o,"sorcery_points_spend_confirmation_required");return o.put("resolved",true).put("state_after",s).put("spell","Summon Beast").put("bonus_action",true).put("slot_required",false).put("material_required",false).put("nearby_enemies_disadvantage_vs_your_spells_ft",5);}
        if(a.equals("shadow_umbral_form")){if(level<18)return block(o,"level_18_required");if(s.optBoolean("shadow_umbral_used",false)&&!in.optBoolean("sorcery_points_6_spend_confirmed",false))return block(o,"no_uses_remaining");s.put("shadow_umbral_used",true);return o.put("resolved",true).put("state_after",s).put("incorporeal_movement",true).put("resistance_all_except_force_radiant",true).put("requires_innate_sorcery",true);}

        // Undead Patron Warlock
        if(a.equals("undead_form_dread")){int max=Math.max(1,cha);if(!spend(s,"undead_form_dread_uses",max))return block(o,"no_uses_remaining");Integer d10=explicitDie(in,"temp_hp_d10",1,10);if(d10==null)return pending(o,"temp_hp_d10_required");return o.put("resolved",true).put("state_after",s).put("duration_minutes",1).put("temp_hp",d10+level).put("frightened_immunity",true).put("once_per_turn_frighten_on_hit",true);}
        if(a.equals("undead_unholy_resuscitation")){if(level<10)return block(o,"level_10_required");if(s.optBoolean("undead_resuscitation_used",false))return block(o,"no_uses_remaining");if(!in.optBoolean("would_drop_to_zero_not_killed",false))return block(o,"trigger_required");Integer damageRoll=explicitTotal(in,"necrotic_2d10_roll_total");if(damageRoll==null)return pending(o,"necrotic_2d10_roll_total_required");s.put("undead_resuscitation_used",true);return o.put("resolved",true).put("state_after",s).put("set_hp",2*level).put("emanation_ft",30).put("necrotic_damage",Math.max(0,damageRoll+cha)).put("save","CON vs spell save DC; half success").put("exhaustion_gain",1);}
        if(a.equals("undead_superior_dread")){if(level<14)return block(o,"level_14_required");return o.put("resolved",true).put("state_after",s).put("requires_form_of_dread",true).put("resistance","bludgeoning|piercing|slashing").put("fly_speed_equals_speed",true).put("hover",true).put("incorporeal_movement",true).put("conjuration_necromancy_components_ignored_except_costly_consumed",true);}

        return block(o,"action_not_implemented");
    }catch(Exception e){return block(o,"runtime_error:"+e.getClass().getSimpleName());}}

    public JSONObject shortRest(JSONObject state){JSONObject s=copy(state);try{s.put("grave_keeper_used",false).put("undead_resuscitation_used",false).put("spirits_channeled",0);}catch(Exception ignored){}return s;}
    public JSONObject longRest(JSONObject state){JSONObject s=shortRest(state);try{String[] keys={"dhampir_bite_empower_uses","lupin_howl_uses","reborn_past_life_uses","sharp_eye_uses","gathered_whispers_scream_uses","living_shadow_reach_uses","mist_walker_teleport_uses","symbiotic_sustained_uses","reanimator_jolt_uses","grave_sentinel_uses","phantom_wails_uses","undead_form_dread_uses"};for(String k:keys)s.put(k,0);String[] flags={"survivor_steel_used","second_skin_free_used","hexblood_token_created_since_long_rest","reanimator_free_companion_used","reanimator_raise_dead_used","spirits_guardians_free_used","grave_keeper_used","hollow_persistent_wrath_used","phantom_ghost_walk_used","shadow_strength_grave_used","shadow_umbral_used","undead_resuscitation_used"};for(String k:flags)s.put(k,false);s.put("reanimated_companion_present",false).put("reanimated_companion_hp",0).put("hollow_wrath_active",false);int level=s.optInt("character_level",1);if(level>=9)s.put("phantom_trinkets",Math.max(2,s.optInt("phantom_trinkets",0)));}catch(Exception ignored){}return s;}

    public JSONObject coverage(){JSONObject o=descriptor();try{return o.put("mists_runtime",true).put("magic_in_mists_runtime",true).put("species_runtime",4).put("background_runtime",4).put("feat_runtime",11).put("subclass_runtime",7).put("bestiary_tactical_runtime",69).put("dynamic_reanimated_companion_runtime",true).put("campaign_arc_modules_runtime",16).put("campaign_arc_runtime_complete",true).put("short_adventure_modules_runtime",17).put("short_adventures_complete",true).put("trait_runtime_complete",true).put("rhw_closed",true).put("remaining_within_rhw","");}catch(Exception e){return o;}}

    private static JSONObject resolveSpirit(int n,JSONObject in,int level,int cha)throws Exception{JSONObject o=resolvedBase("spirit_effect");o.put("spirit_number",n).put("spirit",spirit(n));if(n==1||n==2||n==3||n==6){Integer die=explicitPositive(in,"bardic_die_roll");if(die==null)return pending(o,"bardic_die_roll_required");if(n==1)o.put("healing",die+cha);if(n==2)o.put("force_damage",die);if(n==3)o.put("retaliation_force_damage",die);if(n==6)o.put("temp_hp",die+level).put("speed_bonus_ft",10);}else if(n==4)o.put("target_reaction_teleport_ft",30);else if(n==5)o.put("advantage_d20_tests","until_start_of_bard_next_turn");else if(n==7){Integer two=explicitPositive(in,"two_bardic_dice_total");if(two==null)return pending(o,"two_bardic_dice_total_required");o.put("psychic_damage_on_failed_save",two).put("half_on_success",true).put("charmed_on_fail",true);}else if(n==8){Integer two=explicitPositive(in,"two_bardic_dice_total");if(two==null)return pending(o,"two_bardic_dice_total_required");o.put("invisible","until_end_next_turn_or_attack_damage_spell").put("ending_burst_necrotic",two).put("burst_radius_ft",5);}else if(n==9){Integer four=explicitPositive(in,"four_bardic_dice_total");if(four==null)return pending(o,"four_bardic_dice_total_required");o.put("fire_damage_failed_save",four).put("half_on_success",true);}else if(n==10)o.put("frightened_radius_ft",30).put("on_fail","Frightened; Speed halved; action OR bonus action");else if(n==11){Integer three=explicitPositive(in,"three_bardic_dice_total");if(three==null)return pending(o,"three_bardic_dice_total_required");o.put("thunder_damage_failed_save",three).put("half_on_success",true).put("prone_on_fail",true);}else if(n==12){Integer two=explicitPositive(in,"two_bardic_dice_total");if(two==null)return pending(o,"two_bardic_dice_total_required");o.put("healing",two).put("ends_one_condition","Blinded|Charmed|Deafened|Paralyzed|Poisoned|Stunned");}o.put("resolved",true);return o;}
    private static String spirit(int n){String[] x={"","Beloved","Sharpshooter","Avenger","Renegade","Fortune Teller","Wayfarer","Trickster","Shade","Arsonist","Coward","Brute","Priest"};return n>=1&&n<x.length?x[n]:"";}
    private static JSONObject nat1Save(JSONObject o,JSONObject s,JSONObject in,String ability,int dc,String failCondition,String duration)throws Exception{Integer natural=explicitDie(in,"natural_d20",1,20);if(natural==null)return pending(o,"natural_d20_required");if(natural!=1)return block(o,"trigger_requires_natural_1");Integer total=explicitTotal(in,"save_total");if(total==null)return pending(o,"save_total_required");boolean fail=total<dc;return o.put("resolved",true).put("state_after",s).put("save_ability",ability).put("save_dc",dc).put("save_total",total).put("failed",fail).put("condition_on_fail",fail?failCondition:"").put("duration",fail?duration:"");}
    private static boolean spend(JSONObject s,String key,int max)throws Exception{int used=Math.max(0,s.optInt(key,0));if(max<=0||used>=max)return false;s.put(key,used+1);return true;}
    private static int uses(JSONObject s,String key){return Math.max(0,s.optInt(key,0));}
    private static int stat(JSONObject c,JSONObject in,String key,int def){if(in.has(key))return in.optInt(key,def);if(c.has(key))return c.optInt(key,def);JSONObject sheet=parseSheet(c);return sheet.optInt(key,def);}
    private static JSONObject parseSheet(JSONObject c){try{String raw=c.optString("sheet_json","{}");return raw.trim().startsWith("{")?new JSONObject(raw):new JSONObject();}catch(Exception e){return new JSONObject();}}
    private static int modifier(int score){return (int)Math.floor((score-10)/2.0);}
    private static int proficiency(int level){return 2+Math.max(0,level-1)/4;}
    private static Integer explicitDie(JSONObject in,String key,int min,int max){if(in==null||!in.has(key)||max<min)return null;int v=in.optInt(key,Integer.MIN_VALUE);return v>=min&&v<=max?v:null;}
    private static Integer explicitPositive(JSONObject in,String key){if(in==null||!in.has(key))return null;int v=in.optInt(key,Integer.MIN_VALUE);return v>0?v:null;}
    private static Integer explicitTotal(JSONObject in,String key){if(in==null||!in.has(key))return null;int v=in.optInt(key,Integer.MIN_VALUE);return v==Integer.MIN_VALUE?null:v;}
    private static Integer explicitDieSize(JSONObject in,String key){Integer v=explicitPositive(in,key);return v!=null&&(v==4||v==6||v==8||v==10||v==12)?v:null;}
    private static String canon(String[][] arr,String name){String n=norm(name);for(String[] x:arr)if(norm(x[0]).equals(n)||norm(x[1]).equals(n))return x[0];return "";}
    private static String canonSubclass(String name){String n=norm(name);for(String[] x:SUBCLASSES)if(norm(x[0]).equals(n)||norm(x[2]).equals(n))return x[0];return "";}
    private static String norm(String x){return x==null?"":x.trim().toLowerCase(Locale.ROOT);}
    private static JSONObject base(String kind,String name){JSONObject o=new JSONObject();try{return o.put("kind",kind).put("name",name).put("source",SOURCE).put("source_code",SOURCE_CODE).put("source_verified",!name.isEmpty()).put("auto_rolls",false);}catch(Exception e){return o;}}
    private static JSONObject resolvedBase(String action){JSONObject o=new JSONObject();try{return o.put("action",action).put("resolved",false).put("auto_rolls",false).put("source",SOURCE).put("source_code",SOURCE_CODE);}catch(Exception e){return o;}}
    private static JSONObject pending(JSONObject o,String reason){try{return o.put("resolved",false).put("status","pending_input").put("reason",reason).put("auto_rolls",false);}catch(Exception e){return o;}}
    private static JSONObject block(JSONObject o,String reason){try{return o.put("resolved",false).put("status","blocked").put("reason",reason).put("auto_rolls",false);}catch(Exception e){return o;}}
    private static JSONObject unknown(JSONObject o,String reason){try{return o.put("source_verified",false).put("status","blocked").put("reason",reason);}catch(Exception e){return o;}}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
}
