package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Deterministic procedures from the 2024 Player's Handbook. Never rolls dice automatically. */
public final class Core2024RulesEngine {
    public static final String BOOK="Player's Handbook (2024)";
    private Core2024RulesEngine(){}

    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{o.put("id","core2024_runtime").put("book",BOOK).put("status","compiled_phase_1")
            .put("auto_rolls",false).put("procedures",new JSONArray()
                    .put("core2024.heroic_inspiration.reroll").put("core2024.heroic_inspiration.grant")
                    .put("core2024.weapon_mastery.resolve").put("core2024.action.validate")
                    .put("core2024.tool_skill_synergy").put("core2024.craft.nonmagical")
                    .put("core2024.craft.healing_potion").put("core2024.craft.spell_scroll"));}catch(Exception ignored){}return o;}

    public static JSONObject execute(String id,JSONObject in){JSONObject x=in==null?new JSONObject():in;
        if("core2024.heroic_inspiration.reroll".equals(id))return heroicReroll(x);
        if("core2024.heroic_inspiration.grant".equals(id))return heroicGrant(x);
        if("core2024.weapon_mastery.resolve".equals(id))return weaponMastery(x);
        if("core2024.action.validate".equals(id))return actionValidate(x);
        if("core2024.tool_skill_synergy".equals(id))return toolSkillSynergy(x);
        if("core2024.craft.nonmagical".equals(id))return craftNonmagical(x);
        if("core2024.craft.healing_potion".equals(id))return craftHealingPotion(x);
        if("core2024.craft.spell_scroll".equals(id))return craftSpellScroll(x);
        return blocked("unknown_core2024_rule");
    }

    private static JSONObject heroicReroll(JSONObject in){JSONObject o=base(13);try{
        boolean has=in.optBoolean("has_heroic_inspiration",false),immediate=in.optBoolean("immediately_after_roll",false),spend=in.optBoolean("spend",false);
        if(!has)return blocked(o,"heroic_inspiration_not_available");if(!spend)return blocked(o,"spend_not_confirmed");if(!immediate)return blocked(o,"must_be_immediately_after_roll");
        o.put("allowed",true).put("consume_heroic_inspiration",true).put("reroll_any_die",true).put("must_use_new_result",true);
        if(in.has("new_roll_result"))o.put("resolved_result",in.opt("new_roll_result"));
        return o;
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject heroicGrant(JSONObject in){JSONObject o=base(13);try{
        boolean already=in.optBoolean("recipient_already_has",false);boolean target=in.optBoolean("transfer_target_available",false);
        o.put("recipient_gains",!already).put("recipient_keeps_existing",already).put("can_transfer_instead",already&&target)
         .put("max_heroic_inspiration_per_character",1).put("human_daily_start_supported",true);
        return o;
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject weaponMastery(JSONObject in){JSONObject o=base(214);try{
        String p=norm(in.optString("property"));boolean unlocked=in.optBoolean("mastery_unlocked",false);if(!unlocked)return blocked(o,"weapon_mastery_not_unlocked");
        boolean hit=in.optBoolean("attack_hit",false),damage=in.optBoolean("damage_dealt",hit),once=in.optBoolean("once_per_turn_available",true);
        int ability=in.optInt("attack_ability_modifier",0),pb=Math.max(0,in.optInt("proficiency_bonus",0));
        if(any(p,"afligir","vex")){o.put("property","Afligir").put("applies",hit&&damage).put("next_attack_advantage",hit&&damage).put("expires","end_of_your_next_turn");}
        else if(any(p,"agil","nick")){o.put("property","Ágil").put("applies",in.optBoolean("light_extra_attack_eligible",false)&&once).put("extra_light_attack_as_part_of_attack_action",in.optBoolean("light_extra_attack_eligible",false)&&once).put("once_per_turn",true);}
        else if(any(p,"derrubar","topple")){int dc=8+ability+pb;o.put("property","Derrubar").put("applies",hit).put("constitution_save_dc",dc).put("condition_on_failed_save","Caído");if(in.has("save_success"))o.put("target_falls_prone",hit&&!in.optBoolean("save_success",false));}
        else if(any(p,"drenar","sap")){o.put("property","Drenar").put("applies",hit).put("target_next_attack_disadvantage",hit).put("expires","start_of_your_next_turn");}
        else if(any(p,"empurrar","push")){String size=norm(in.optString("target_size"));boolean sizeOk=sizeRank(size)<=sizeRank("large")&&sizeRank(size)>0;o.put("property","Empurrar").put("applies",hit&&sizeOk).put("push_meters",hit&&sizeOk?3:0).put("requires_target_large_or_smaller",true);}
        else if(any(p,"garantido","graze")){boolean miss=!hit;o.put("property","Garantido").put("applies",miss).put("damage_on_miss",miss?ability:0).put("damage_floor_rule","exact attack ability modifier; only increased if modifier increases");}
        else if(any(p,"lentidao","slow")){o.put("property","Lentidão").put("applies",hit&&damage).put("speed_reduction_meters",hit&&damage?3:0).put("same_property_reduction_cap_meters",3).put("expires","start_of_your_next_turn");}
        else if(any(p,"trespassar","cleave")){boolean melee=in.optBoolean("melee_attack",false),second=in.optBoolean("second_target_within_1_5m_of_first_and_in_reach",false);boolean ok=hit&&melee&&second&&once;o.put("property","Trespassar").put("applies",ok).put("extra_attack_allowed",ok).put("once_per_turn",true).put("second_hit_damage_adds_ability_modifier",false).put("negative_modifier_still_applies",true);}
        else return blocked(o,"unknown_weapon_mastery_property");
        return o;
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject actionValidate(JSONObject in){JSONObject o=base(14);try{String a=norm(in.optString("action")),skill=norm(in.optString("skill")),ability=norm(in.optString("ability"));JSONArray allowed=new JSONArray();String expected="";
        if(any(a,"analisar","study")){expected="Inteligência";for(String s:new String[]{"Arcanismo","História","Investigação","Natureza","Religião"})allowed.put(s);}
        else if(any(a,"influenciar","influence")){expected="Carisma ou Sabedoria (Lidar com Animais)";for(String s:new String[]{"Atuação","Enganação","Intimidação","Persuasão","Lidar com Animais"})allowed.put(s);}
        else if(any(a,"procurar","search")){expected="Sabedoria";for(String s:new String[]{"Intuição","Medicina","Percepção","Sobrevivência"})allowed.put(s);}
        else if(any(a,"usar objeto","utilize","utilizar")){return o.put("action","Usar Objeto").put("valid",true).put("summary","Utilizar um objeto não mágico.");}
        else return blocked(o,"unsupported_action");
        boolean skillOk=false;for(int i=0;i<allowed.length();i++)if(norm(allowed.optString(i)).equals(skill)){skillOk=true;break;}
        boolean abilityOk=true;if(any(a,"analisar","study"))abilityOk=any(ability,"inteligencia","intelligence");else if(any(a,"procurar","search"))abilityOk=any(ability,"sabedoria","wisdom");else if(any(a,"influenciar","influence")){abilityOk=skill.equals(norm("Lidar com Animais"))?any(ability,"sabedoria","wisdom"):any(ability,"carisma","charisma");}
        return o.put("action",a).put("allowed_skills",allowed).put("expected_ability",expected).put("skill_valid",skillOk).put("ability_valid",abilityOk).put("valid",skillOk&&abilityOk);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject toolSkillSynergy(JSONObject in){JSONObject o=base(14);try{boolean tool=in.optBoolean("tool_proficient",false),skill=in.optBoolean("skill_proficient",false),toolApplies=in.optBoolean("tool_applies",false),skillApplies=in.optBoolean("skill_applies",false);boolean both=tool&&skill&&toolApplies&&skillApplies;return o.put("add_proficiency_bonus",tool&&toolApplies).put("advantage",both).put("both_proficiencies_apply_without_double_proficiency",both);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject craftNonmagical(JSONObject in){JSONObject o=base(233);try{double price=Math.max(0,in.optDouble("purchase_price_gp",0));int workers=Math.max(1,in.optInt("qualified_workers",1));if(price<=0)return blocked(o,"purchase_price_required");if(!in.optBoolean("tool_proficient",false)||!in.optBoolean("required_tool_available",false))return blocked(o,"required_tool_and_proficiency");double raw=Math.floor(price/2d);int soloDays=(int)Math.ceil(price/10d);int days=(int)Math.ceil(soloDays/(double)workers);return o.put("raw_material_cost_gp",raw).put("solo_days",soloDays).put("days_required",Math.max(1,days)).put("work_hours_per_day",8).put("qualified_workers",workers).put("sale_price_gp",price);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject craftHealingPotion(JSONObject in){JSONObject o=base(233);try{boolean prof=in.optBoolean("herbalism_kit_proficient",false),kit=in.optBoolean("herbalism_kit_available",false);if(!prof||!kit)return blocked(o,"herbalism_kit_and_proficiency_required");return o.put("potion","Poção de Cura").put("raw_material_cost_gp",25).put("days_required",1).put("work_hours",8);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject craftSpellScroll(JSONObject in){JSONObject o=base(233);try{int level=in.optInt("spell_level",-1);if(level<0||level>9)return blocked(o,"spell_level_0_to_9_required");boolean prof=in.optBoolean("arcana_proficient",false)||in.optBoolean("calligrapher_supplies_proficient",false);if(!prof)return blocked(o,"arcana_or_calligrapher_proficiency_required");if(!in.optBoolean("spell_prepared_each_day",false))return blocked(o,"spell_must_be_prepared_each_day");if(!in.optBoolean("material_components_available",false))return blocked(o,"material_components_required");int[] days={1,1,3,5,10,25,40,50,60,120};int[] gp={15,25,100,150,1000,1500,10000,12500,15000,50000};return o.put("days_required",days[level]).put("gold_cost",gp[level]).put("work_hours_per_day",8).put("uses_caster_spell_save_dc",true).put("uses_caster_spell_attack_bonus",true).put("cantrip_uses_scriber_level",level==0);}catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject base(int page){JSONObject o=new JSONObject();try{o.put("book",BOOK).put("page",page).put("status","ok").put("auto_rolls",false);}catch(Exception ignored){}return o;}
    private static JSONObject blocked(String reason){return blocked(base(0),reason);}private static JSONObject blocked(JSONObject o,String reason){try{o.put("status","blocked").put("requires_resolution",true).put("reason",reason);}catch(Exception ignored){}return o;}
    private static String norm(String s){return s==null?"":java.text.Normalizer.normalize(s.toLowerCase(new Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").trim();}
    private static boolean any(String s,String... xs){for(String x:xs)if(s.equals(norm(x)))return true;return false;}
    private static int sizeRank(String s){String x=norm(s);if(any(x,"tiny","minusculo"))return 1;if(any(x,"small","pequeno"))return 2;if(any(x,"medium","medio"))return 3;if(any(x,"large","grande"))return 4;if(any(x,"huge","enorme"))return 5;if(any(x,"gargantuan","colossal"))return 6;return 0;}
}
