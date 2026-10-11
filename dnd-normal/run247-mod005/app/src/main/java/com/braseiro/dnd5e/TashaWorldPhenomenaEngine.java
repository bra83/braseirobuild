package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Post-release Tasha GM-world systems: supernatural regions and magical phenomena.
 * Source-bound and persistence-friendly. Deterministic effects are executable;
 * table entries that inherently require GM interpretation are persisted as Barbara-review work.
 */
public final class TashaWorldPhenomenaEngine {
    public static final String BOOK=TashaCompiledSourceRulesEngine.BOOK;
    public static final String SOURCE_FILE=TashaCompiledSourceRulesEngine.SOURCE_FILE;
    public static final String SOURCE_SHA256=TashaCompiledSourceRulesEngine.SOURCE_SHA256;

    private static final String[] REGION_TYPES={"blessed_radiance","far_realm","haunted","infested","mirror_zone","psychic_resonance","unraveling_magic"};
    private static final String[] REGION_LABELS={"Esplendor Abençoado","Reino Distante","Assombrado","Infestado","Zona Espelhada","Ressonância Psíquica","Magia Estragada"};
    private static final String[] STORM_TYPES={"scouring_sandstorm","flame_storm","necrotic_storm","thrym_howl"};
    private static final String[] ECHO_TYPES={"boldness","doubt","fear","hate","inspiration","joy","love","sadness"};

    private TashaWorldPhenomenaEngine(){}

    public static JSONObject descriptor(){/*BR_JSON_WRAP_106*/try{return new JSONObject()
            .put("id","tasha_world_phenomena").put("book",BOOK).put("source_file",SOURCE_FILE).put("source_sha256",SOURCE_SHA256)
            .put("supernatural_region_types",REGION_TYPES.length).put("mystic_storm_types",STORM_TYPES.length)
            .put("emotional_echo_types",ECHO_TYPES.length).put("enchanted_fountain",true).put("magical_mushrooms",true).put("primal_fruit",true).put("supernatural_roads",true).put("mimic_colonies",true)
            .put("persistence_ready",true).put("deterministic_region_resolvers",20).put("runtime_policy_evaluation",true)
            .put("barbara_review_for_interpretive_tables",true).put("auto_pc_mutation",false);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject emptyRegions(){return new JSONObject();}
    public static JSONObject emptyPhenomena(){return new JSONObject();}

    public static JSONArray regionTypes(){/*BR_JSON_WRAP_106*/try{JSONArray a=new JSONArray();for(int i=0;i<REGION_TYPES.length;i++)a.put(new JSONObject().put("id",REGION_TYPES[i]).put("label_pt",REGION_LABELS[i]));return a;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createRegion(JSONObject regions,String id,String type,String location,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject all=copy(regions);String key=clean(id,"");String t=canonicalRegion(type);if(key.isEmpty())throw new IllegalArgumentException("region id required");if(t.isEmpty())throw new IllegalArgumentException("unknown supernatural region type");
        JSONObject r=new JSONObject().put("id",key).put("type",t).put("label_pt",regionLabel(t)).put("location",clean(location,key)).put("active",true)
                .put("created_day",Math.max(1,day)).put("trigger_count",0).put("effect_history",new JSONArray()).put("pending_barbara_review",false)
                .put("source_book",BOOK).put("source_pages","148-156").put("source_sha256",SOURCE_SHA256);
        all.put(key,r);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /**
     * Records a source-bound d100 regional effect. Twenty entries whose mechanics are
     * unambiguous in the source are compiled as read-only runtime policies. Entries that
     * need a GM choice, random target/duration not supplied by the caller, or narrative
     * interpretation remain Barbara-review gated. No resolver directly mutates a PC.
     */
    public static JSONObject triggerRegion(JSONObject regions,String id,String event,int roll,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject all=copy(regions);JSONObject r=all.optJSONObject(id);if(r==null||!r.optBoolean("active",false))throw new IllegalArgumentException("active supernatural region required");
        String e=clean(event,"").toLowerCase(Locale.ROOT);if(e.isEmpty())throw new IllegalArgumentException("trigger event required");int d100=Math.max(1,Math.min(100,roll));
        JSONObject hit=compiledRegionEffect(r.optString("type"),d100)
                .put("day",Math.max(1,day)).put("trigger",e).put("d100",d100).put("table_entry","effect_"+String.format(Locale.ROOT,"%03d",d100))
                .put("region_type",r.optString("type")).put("source_sha256",SOURCE_SHA256).put("auto_pc_mutation_allowed",false);
        boolean review=hit.optBoolean("requires_barbara_review",true);
        JSONArray h=r.optJSONArray("effect_history");if(h==null)h=new JSONArray();h.put(hit);r.put("effect_history",h).put("last_effect",hit).put("trigger_count",r.optInt("trigger_count",0)+1).put("pending_barbara_review",review).put("last_trigger_day",Math.max(1,day));
        all.put(id,r);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /** Returns the compiled source rule for a d100 result without rolling or mutating state. */
    public static JSONObject compiledRegionEffect(String type,int roll){/*BR_JSON_WRAP_106*/try{
        String t=canonicalRegion(type);if(t.isEmpty())throw new IllegalArgumentException("unknown supernatural region type");int r=Math.max(1,Math.min(100,roll));
        if("blessed_radiance".equals(t)){
            if(between(r,13,18))return safeRegionRule("blessed_13_18_holy_enemy_disadvantage","148-149").put("duration_hours",24).put("affected_creature_types",types("aberration","fiend","undead")).put("attack_roll_disadvantage",true).put("ability_check_disadvantage",true);
            if(between(r,19,24))return safeRegionRule("blessed_19_24_holy_symbol_save_advantage","148-149").put("duration_hours",24).put("requires_holy_symbol",true).put("requires_non_evil_planar_deity",true).put("saving_throw_advantage",true);
            if(between(r,49,54))return safeRegionRule("blessed_49_54_crimson_glow","148-149").put("duration_minutes",1).put("affected_creature_types",types("aberration","fiend","undead")).put("dim_light_ft",10).put("attacks_against_advantage_if_visible",true).put("cannot_benefit_from_invisibility",true);
        }
        if("far_realm".equals(t)&&between(r,28,36))return safeRegionRule("far_realm_28_36_ranged_distortion","149-150").put("ranged_attack_disadvantage",true).put("normal_and_long_range_multiplier",0.5d);
        if("haunted".equals(t)&&between(r,16,20))return safeRegionRule("haunted_16_20_light_suppression","150-151").put("duration_hours",24).put("bright_light_becomes_dim",true).put("dim_light_sources_emit_no_light",true);
        if("infested".equals(t)){
            if(between(r,6,10))return safeRegionRule("infested_06_10_rest_blocked","151-152").put("duration_hours",24).put("short_rest_blocked",true).put("long_rest_blocked",true);
            if(between(r,36,45))return safeRegionRule("infested_36_45_web_difficult_terrain","151-152").put("difficult_terrain",true);
            if(between(r,46,50))return safeRegionRule("infested_46_50_noisy_shells","151-152").put("duration_hours",1).put("stealth_disadvantage_while_moving",true);
            if(between(r,86,90))return safeRegionRule("infested_86_90_armor_interference","151-152").put("duration_hours",24).put("affected_armor_categories",types("medium","heavy")).put("attack_roll_disadvantage",true).put("ability_check_disadvantage",true).put("saving_throw_disadvantage",true);
        }
        if("mirror_zone".equals(t)){
            if(between(r,19,24))return safeRegionRule("mirror_19_24_absent_reflections","153-154").put("duration_until_exit_region",true).put("casts_no_reflection",true).put("insight_against_disadvantage",true).put("persuasion_disadvantage_when_perceived",true);
            if(between(r,47,52))return safeRegionRule("mirror_47_52_reflective_misty_step","153-154").put("reflective_surface_touch_allows_misty_step",true).put("spell_components_required",false);
        }
        if("psychic_resonance".equals(t)){
            if(between(r,19,24))return safeRegionRule("psychic_19_24_sharp_memory","154-155").put("duration_hours",1).put("double_proficiency_intelligence_recall_checks",true);
            if(between(r,25,34))return safeRegionRule("psychic_25_34_humanoid_perception_disadvantage","154-155").put("duration_hours",1).put("affected_creature_types",types("humanoid")).put("perception_check_disadvantage",true);
            if(between(r,47,52))return safeRegionRule("psychic_47_52_telepathy","154-155").put("duration_hours",1).put("telepathy_range_ft",60).put("target_must_know_a_language",true);
            if(between(r,59,64))return safeRegionRule("psychic_59_64_cognitive_interference","154-155").put("duration_hours",1).put("intelligence_check_disadvantage",true).put("concentration_constitution_save_disadvantage",true);
            if(between(r,77,82))return safeRegionRule("psychic_77_82_psychic_resistance","154-155").put("duration_hours",1).put("psychic_damage_resistance",true);
        }
        if("unraveling_magic".equals(t)){
            if(between(r,1,5))return safeRegionRule("unraveling_01_05_magic_item_suppression","155-156").put("duration_hours",1).put("magic_items_nonmagical",true).put("artifacts_exempt",true).put("attunement_restored_after_effect",true);
            if(between(r,6,10))return safeRegionRule("unraveling_06_10_antimagic_region","155-156").put("duration_hours",1).put("antimagic_field_region",true);
            if(between(r,36,40))return safeRegionRule("unraveling_36_40_frozen_fire_extreme_cold","155-156").put("duration_days",1).put("fire_freezes_to_ice",true).put("frozen_fire_emits_blue_light",true).put("extreme_cold_region",true);
            if(between(r,51,55))return safeRegionRule("unraveling_51_55_plus_one_weapons","155-156").put("duration_hours",1).put("eligible_weapon_categories",types("simple","martial")).put("requires_nonmagical_weapon",true).put("weapon_becomes_magical",true).put("attack_bonus",1).put("damage_bonus",1);
        }
        return reviewRegionRule(t,r);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /**
     * Evaluates the most recent compiled deterministic regional policy against caller-
     * supplied context. This is a pure query: it never rolls, spends resources or mutates
     * a character/world object.
     */
    public static JSONObject evaluateRegionEffect(JSONObject regions,String id,JSONObject context){/*BR_JSON_WRAP_106*/try{
        JSONObject r=regions==null?null:regions.optJSONObject(id);if(r==null||!r.optBoolean("active",false))throw new IllegalArgumentException("active supernatural region required");
        JSONObject e=r.optJSONObject("last_effect");if(e==null)throw new IllegalArgumentException("region has no triggered effect");
        JSONObject out=new JSONObject().put("rule_id",e.optString("rule_id")).put("region_type",r.optString("type")).put("source_pages",e.optString("source_pages"))
                .put("source_sha256",SOURCE_SHA256).put("mutates_state",false).put("auto_pc_mutation_allowed",false);
        if(e.optBoolean("requires_barbara_review",true))return out.put("applies",false).put("blocked_by_barbara_review",true);
        JSONObject c=context==null?new JSONObject():context,m=new JSONObject();String rule=e.optString("rule_id"),ct=norm(c.optString("creature_type")),armor=norm(c.optString("armor_category")),category=norm(c.optString("weapon_category"));
        boolean applies=true;
        if("blessed_13_18_holy_enemy_disadvantage".equals(rule)){applies=isOne(ct,"aberration","fiend","undead");if(applies)m.put("attack_roll_disadvantage",true).put("ability_check_disadvantage",true);}
        else if("blessed_19_24_holy_symbol_save_advantage".equals(rule)){applies=c.optBoolean("has_holy_symbol",false)&&c.optBoolean("deity_planar",false)&&!c.optBoolean("deity_evil",false);if(applies)m.put("saving_throw_advantage",true);}
        else if("blessed_49_54_crimson_glow".equals(rule)){applies=isOne(ct,"aberration","fiend","undead");if(applies)m.put("dim_light_ft",10).put("attacks_against_advantage_if_visible",true).put("cannot_benefit_from_invisibility",true);}
        else if("far_realm_28_36_ranged_distortion".equals(rule)){applies="ranged".equals(norm(c.optString("attack_kind")));if(applies)m.put("attack_roll_disadvantage",true).put("range_multiplier",0.5d);}
        else if("haunted_16_20_light_suppression".equals(rule)){m.put("bright_light_becomes_dim",true).put("dim_light_sources_emit_no_light",true);}
        else if("infested_06_10_rest_blocked".equals(rule)){String a=norm(c.optString("activity"));applies="short rest".equals(a)||"long rest".equals(a);if(applies)m.put("rest_blocked",true);}
        else if("infested_36_45_web_difficult_terrain".equals(rule)){m.put("difficult_terrain",true);}
        else if("infested_46_50_noisy_shells".equals(rule)){applies=c.optBoolean("moving",false)&&("stealth".equals(norm(c.optString("skill")))||c.optString("skill").isEmpty());if(applies)m.put("stealth_check_disadvantage",true);}
        else if("infested_86_90_armor_interference".equals(rule)){applies=isOne(armor,"medium","heavy");if(applies)m.put("attack_roll_disadvantage",true).put("ability_check_disadvantage",true).put("saving_throw_disadvantage",true);}
        else if("mirror_19_24_absent_reflections".equals(rule)){m.put("casts_no_reflection",true).put("insight_against_disadvantage",true).put("persuasion_disadvantage_when_perceived",true);}
        else if("mirror_47_52_reflective_misty_step".equals(rule)){applies=c.optBoolean("touching_reflective_surface",false);if(applies)m.put("can_cast_misty_step",true).put("spell_components_required",false);}
        else if("psychic_19_24_sharp_memory".equals(rule)){applies="intelligence".equals(norm(c.optString("check_ability")))&&c.optBoolean("recall_information",false);if(applies)m.put("double_proficiency_bonus",true);}
        else if("psychic_25_34_humanoid_perception_disadvantage".equals(rule)){applies="humanoid".equals(ct);if(applies)m.put("perception_check_disadvantage",true);}
        else if("psychic_47_52_telepathy".equals(rule)){applies=c.optBoolean("target_knows_language",false);if(applies)m.put("telepathy_range_ft",60);}
        else if("psychic_59_64_cognitive_interference".equals(rule)){m.put("intelligence_check_disadvantage",true).put("concentration_constitution_save_disadvantage",true);}
        else if("psychic_77_82_psychic_resistance".equals(rule)){m.put("psychic_damage_resistance",true);}
        else if("unraveling_01_05_magic_item_suppression".equals(rule)){applies=c.optBoolean("is_magic_item",false)&&!c.optBoolean("is_artifact",false);if(applies)m.put("magic_item_properties_suppressed",true).put("attunement_restored_after_effect",true);}
        else if("unraveling_06_10_antimagic_region".equals(rule)){m.put("antimagic_field_region",true);}
        else if("unraveling_36_40_frozen_fire_extreme_cold".equals(rule)){m.put("fire_freezes_to_ice",true).put("frozen_fire_emits_blue_light",true).put("extreme_cold_region",true);}
        else if("unraveling_51_55_plus_one_weapons".equals(rule)){applies=!c.optBoolean("weapon_is_magical",false)&&isOne(category,"simple","martial");if(applies)m.put("weapon_becomes_magical",true).put("attack_bonus",1).put("damage_bonus",1);}
        else return out.put("applies",false).put("blocked_by_barbara_review",true);
        return out.put("applies",applies).put("blocked_by_barbara_review",false).put("modifiers",m).put("duration_hours",e.optInt("duration_hours",0)).put("duration_days",e.optInt("duration_days",0)).put("duration_minutes",e.optInt("duration_minutes",0)).put("duration_until_exit_region",e.optBoolean("duration_until_exit_region",false));
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject acknowledgeRegionEffect(JSONObject regions,String id,String resolution,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(regions);JSONObject r=all.optJSONObject(id);if(r==null)throw new IllegalArgumentException("unknown supernatural region");r.put("pending_barbara_review",false).put("last_resolution",clean(resolution,"reviewed")).put("last_resolution_day",Math.max(1,day));all.put(id,r);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    private static JSONObject safeRegionRule(String id,String pages){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("rule_id",id).put("resolution_mode","deterministic_region_rule").put("deterministic_runtime_ready",true).put("requires_barbara_review",false).put("auto_pc_mutation_allowed",false).put("source_pages",pages).put("source_sha256",SOURCE_SHA256);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject reviewRegionRule(String type,int roll){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("rule_id","review_"+type+"_"+String.format(Locale.ROOT,"%03d",roll)).put("resolution_mode","barbara_review").put("deterministic_runtime_ready",false).put("requires_barbara_review",true).put("auto_pc_mutation_allowed",false).put("source_pages",regionSourcePages(type)).put("source_sha256",SOURCE_SHA256);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static String regionSourcePages(String type){if("blessed_radiance".equals(type))return "148-149";if("far_realm".equals(type))return "149-150";if("haunted".equals(type))return "150-151";if("infested".equals(type))return "151-152";if("mirror_zone".equals(type))return "153-154";if("psychic_resonance".equals(type))return "154-155";if("unraveling_magic".equals(type))return "155-156";return "148-156";}
    private static JSONArray types(String...v){JSONArray a=new JSONArray();for(String q:v)a.put(q);return a;}
    private static boolean between(int n,int a,int b){return n>=a&&n<=b;}
    private static boolean isOne(String value,String... options){for(String q:options)if(q.equals(value))return true;return false;}

    public static JSONObject startMysticStorm(JSONObject phenomena,String id,String type,int durationUnits,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject all=copy(phenomena);String key=clean(id,"");String t=canonicalStorm(type);if(key.isEmpty()||t.isEmpty())throw new IllegalArgumentException("storm id/type required");
        JSONObject s=new JSONObject().put("id",key).put("kind","mystic_storm").put("storm_type",t).put("active",true).put("started_day",Math.max(1,day)).put("victims",new JSONObject()).put("source_pages","156-157").put("source_sha256",SOURCE_SHA256);
        if("scouring_sandstorm".equals(t))s.put("duration_hours",Math.max(1,durationUnits)).put("heavily_obscured",true).put("turn_damage","1d4").put("identify_dc",15).put("identify_skills",new JSONArray().put("Arcana").put("Nature").put("Survival")).put("warning_minutes",1);
        if("flame_storm".equals(t))s.put("duration_minutes",Math.max(1,durationUnits)).put("turn_damage","2d6 fire").put("ignites_unattended_flammables",true).put("perception_disadvantage",true).put("ranged_attack_disadvantage",true);
        if("necrotic_storm".equals(t))s.put("duration_hours",Math.max(1,durationUnits)).put("save_interval_minutes",1).put("constitution_save_dc",13).put("failed_save_damage","3d6 necrotic").put("raises_dead_after_minutes","1d10").put("aftermath_days","1d4");
        if("thrym_howl".equals(t))s.put("duration_hours",Math.max(1,durationUnits)).put("extreme_cold",true).put("heavily_obscured",true).put("ranged_attack_disadvantage",true).put("perception_disadvantage",true).put("turn_damage","2d6 cold").put("healing_blocked_until_warm_hours",1);
        all.put(key,s);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /** Caller supplies the rolled damage from the source dice. No random generator is hidden here. */
    public static JSONObject stormExposure(JSONObject phenomena,String id,String victim,int saveTotal,int rolledDamage,boolean immune,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject all=copy(phenomena);JSONObject s=all.optJSONObject(id);if(s==null||!s.optBoolean("active",false)||!"mystic_storm".equals(s.optString("kind")))throw new IllegalArgumentException("active mystic storm required");String v=clean(victim,"");if(v.isEmpty())throw new IllegalArgumentException("victim required");String t=s.optString("storm_type");int damage=Math.max(0,rolledDamage);boolean save=false;
        if(immune)damage=0;else if("necrotic_storm".equals(t)){save=saveTotal>=13;if(save)damage=0;}
        JSONObject victims=s.optJSONObject("victims");if(victims==null)victims=new JSONObject();JSONObject x=victims.optJSONObject(v);if(x==null)x=new JSONObject();x.put("last_day",Math.max(1,day)).put("last_save_total",saveTotal).put("save_success",save).put("last_damage",damage).put("immune",immune);
        if("thrym_howl".equals(t)&&!immune)x.put("healing_blocked",true).put("requires_warm_environment_hours",1);if("necrotic_storm".equals(t)&&damage>0)x.put("necrotic_exposure",true);
        victims.put(v,x);s.put("victims",victims).put("last_exposure_day",Math.max(1,day));all.put(id,s);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject stopPhenomenon(JSONObject phenomena,String id,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null)throw new IllegalArgumentException("unknown phenomenon");p.put("active",false).put("stopped_day",Math.max(1,day));all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createEmotionalEcho(JSONObject phenomena,String id,String emotion,String location,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);String key=clean(id,"");String e=canonicalEcho(emotion);if(key.isEmpty()||e.isEmpty())throw new IllegalArgumentException("echo id/emotion required");all.put(key,new JSONObject().put("id",key).put("kind","emotional_echo").put("emotion",e).put("location",clean(location,key)).put("active",true).put("suggestion_dc",16).put("suggestion_duration_hours",24).put("last_trigger_day",0).put("source_page",157).put("source_sha256",SOURCE_SHA256));return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /** Once per day, an emotional echo may force a DC16 Suggestion-like effect when the emotion is expressed. */
    public static JSONObject triggerEmotionalEcho(JSONObject phenomena,String id,String creature,boolean emotionExpressed,int wisdomSaveTotal,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"emotional_echo".equals(p.optString("kind")))throw new IllegalArgumentException("active emotional echo required");int d=Math.max(1,day);if(!emotionExpressed)return all;if(p.optInt("last_trigger_day",0)==d)throw new IllegalArgumentException("emotional echo already triggered today");boolean saved=wisdomSaveTotal>=16;p.put("last_trigger_day",d).put("last_target",clean(creature,"unknown")).put("last_save_total",wisdomSaveTotal).put("suggestion_applies",!saved).put("suggestion_expires_day",d+1).put("pending_barbara_review",!saved);all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createEnchantedFountain(JSONObject phenomena,String id,String location,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);String key=clean(id,"");if(key.isEmpty())throw new IllegalArgumentException("fountain id required");all.put(key,new JSONObject().put("id",key).put("kind","enchanted_fountain").put("location",clean(location,key)).put("active",true).put("source_pages","157-158").put("source_sha256",SOURCE_SHA256).put("effect_history",new JSONArray()));return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject useEnchantedFountain(JSONObject phenomena,String id,String creature,int d12,int durationRoll,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"enchanted_fountain".equals(p.optString("kind")))throw new IllegalArgumentException("active enchanted fountain required");int r=Math.max(1,Math.min(12,d12));JSONObject effect=new JSONObject().put("roll",r).put("target",clean(creature,"unknown")).put("day",Math.max(1,day)).put("source_pages","157-158").put("requires_barbara_review",false);
        switch(r){case 1:effect.put("effect","bless").put("duration_hours",1);break;case 2:effect.put("effect","golden_feathers").put("armor_class_bonus",1).put("requires_no_armor",true).put("duration_days",Math.max(1,durationRoll));break;case 10:effect.put("effect","sinister_voice").put("intimidation_advantage",true).put("deception_disadvantage",true).put("persuasion_disadvantage",true).put("duration_hours",24);break;case 11:effect.put("effect","donkey_ears").put("hearing_perception_advantage",true).put("duration_days",Math.max(1,durationRoll));break;case 12:effect.put("effect","third_eye").put("truesight_m",18).put("duration_hours",24);break;default:effect.put("effect","fountain_table_entry_"+r).put("requires_barbara_review",true).put("auto_pc_mutation_allowed",false);}
        JSONArray h=p.optJSONArray("effect_history");if(h==null)h=new JSONArray();h.put(effect);p.put("effect_history",h).put("last_effect",effect).put("pending_barbara_review",effect.optBoolean("requires_barbara_review",false));all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}


    public static JSONObject createMagicalMushroomPatch(JSONObject phenomena,String id,String location,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);String key=clean(id,"");if(key.isEmpty())throw new IllegalArgumentException("mushroom patch id required");all.put(key,new JSONObject().put("id",key).put("kind","magical_mushrooms").put("location",clean(location,key)).put("active",true).put("consumptions",0).put("source_pages","158-159").put("source_sha256",SOURCE_SHA256).put("effect_history",new JSONArray()));return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    /** d10 magical mushroom table, with rolled durations supplied by caller when the source calls for them. */
    public static JSONObject consumeMagicalMushroom(JSONObject phenomena,String id,String creature,int d10,int durationRoll,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"magical_mushrooms".equals(p.optString("kind")))throw new IllegalArgumentException("active magical mushroom patch required");int r=Math.max(1,Math.min(10,d10));JSONObject e=new JSONObject().put("roll",r).put("target",clean(creature,"unknown")).put("day",Math.max(1,day)).put("source_pages","158-159").put("requires_barbara_review",false);
        switch(r){case 1:e.put("effect","permanent_unusual_skin_color").put("removable_by_greater_restoration",true).put("color_roll_required",true);break;case 2:e.put("effect","enlarge_or_reduce").put("duration_hours",1).put("fifty_fifty_variant",true);break;case 3:e.put("effect","healing").put("healing","5d8+20");break;case 4:e.put("effect","chicken_speech_curse").put("duration_hours",1).put("remove_curse_ends",true).put("can_speak_with_chickens",true);break;case 5:e.put("effect","all_languages").put("duration_days",Math.max(1,durationRoll));break;case 6:e.put("effect","telepathy_spell").put("duration_hours",24);break;case 7:e.put("effect","speak_with_plants").put("duration_hours",8);break;case 8:e.put("effect","immediate_time_stop").put("casting_ability","CON");break;case 9:e.put("effect","immediate_detect_thoughts").put("casting_ability","CON");break;case 10:e.put("effect","moving_fog_cloud").put("duration_hours",1);break;}
        JSONArray h=p.optJSONArray("effect_history");if(h==null)h=new JSONArray();h.put(e);p.put("effect_history",h).put("last_effect",e).put("consumptions",p.optInt("consumptions",0)+1);all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createPrimalFruitPlant(JSONObject phenomena,String id,String location,int weeklyYield,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);String key=clean(id,"");if(key.isEmpty())throw new IllegalArgumentException("primal fruit plant id required");int yield=Math.max(1,Math.min(6,weeklyYield));all.put(key,new JSONObject().put("id",key).put("kind","primal_fruit_plant").put("location",clean(location,key)).put("active",true).put("stock",yield).put("weekly_yield",yield).put("potency_days",7).put("last_harvest_day",Math.max(1,day)).put("source_pages","159-160").put("source_sha256",SOURCE_SHA256).put("effect_history",new JSONArray()));return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject harvestPrimalFruit(JSONObject phenomena,String id,int weeklyYield,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"primal_fruit_plant".equals(p.optString("kind")))throw new IllegalArgumentException("active primal fruit plant required");int d=Math.max(1,day);if(d-p.optInt("last_harvest_day",d)<7)throw new IllegalArgumentException("primal fruit has not regenerated for a week");int yield=Math.max(1,Math.min(6,weeklyYield));p.put("stock",p.optInt("stock",0)+yield).put("weekly_yield",yield).put("last_harvest_day",d).put("stock_expires_day",d+7);all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject consumePrimalFruit(JSONObject phenomena,String id,String creature,int d8,int durationRoll,int charismaSaveTotal,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"primal_fruit_plant".equals(p.optString("kind")))throw new IllegalArgumentException("active primal fruit plant required");if(p.optInt("stock",0)<=0)throw new IllegalArgumentException("no primal fruit stock");int r=Math.max(1,Math.min(8,d8));JSONObject e=new JSONObject().put("roll",r).put("target",clean(creature,"unknown")).put("day",Math.max(1,day)).put("source_pages","159-160").put("requires_barbara_review",false);
        switch(r){case 1:e.put("effect","healing_and_light").put("healing","3d8+4").put("light_duration_hours",1);break;case 2:e.put("effect","strength_surge").put("strength_attack_advantage",true).put("strength_check_advantage",true).put("strength_save_advantage",true).put("duration_hours",1).put("exhaustion_when_ends",1);break;case 3:e.put("effect","vitality_surge").put("max_hp_increase","2d10").put("until_long_rest",true).put("charisma_save_dc",15).put("charisma_save_total",charismaSaveTotal).put("lycanthropy_if_failed_after_rest",charismaSaveTotal<15).put("lycanthropy_variant_requires_barbara_review",charismaSaveTotal<15);break;case 4:e.put("effect","damage_resistance").put("duration_hours",1).put("damage_type_requires_barbara_choice",true).put("requires_barbara_review",true);break;case 5:e.put("effect","death_ward_plus_poison_risk").put("death_ward_hours",8).put("constitution_save_dc",13).put("poison_duration_hours",8).put("requires_constitution_save_resolution",true).put("requires_barbara_review",true);break;case 6:e.put("effect","spell_save_advantage").put("duration_hours",1);break;case 7:e.put("effect","no_food_drink_sleep").put("duration_days",Math.max(1,durationRoll)).put("immune_magical_sleep",true).put("perception_disadvantage",true);break;case 8:e.put("effect","telepathy_36m").put("duration_hours",24);break;}
        p.put("stock",p.optInt("stock",0)-1);JSONArray h=p.optJSONArray("effect_history");if(h==null)h=new JSONArray();h.put(e);p.put("effect_history",h).put("last_effect",e).put("pending_barbara_review",e.optBoolean("requires_barbara_review",false));all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createSupernaturalRoad(JSONObject phenomena,String id,String from,String to,int keyRoll,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);String key=clean(id,"");if(key.isEmpty())throw new IllegalArgumentException("road id required");int k=Math.max(1,Math.min(6,keyRoll));all.put(key,new JSONObject().put("id",key).put("kind","supernatural_road").put("from",clean(from,"unknown")).put("to",clean(to,"unknown")).put("active",true).put("key_roll",k).put("key_code","road_key_"+k).put("normal_distance_km_per_road_km",33d/1.6d).put("source_pages","160-161").put("source_sha256",SOURCE_SHA256));return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject supernaturalRoadTravel(JSONObject phenomena,String id,double roadDistanceKm,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);JSONObject p=all.optJSONObject(id);if(p==null||!p.optBoolean("active",false)||!"supernatural_road".equals(p.optString("kind")))throw new IllegalArgumentException("active supernatural road required");double road=Math.max(0d,roadDistanceKm);double equivalent=road*(33d/1.6d);p.put("last_travel_day",Math.max(1,day)).put("last_road_distance_km",road).put("last_normal_distance_equivalent_km",equivalent).put("travel_events",p.optInt("travel_events",0)+1);all.put(id,p);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject advanceDay(JSONObject phenomena,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(phenomena);java.util.Iterator<String>it=all.keys();while(it.hasNext()){String id=it.next();JSONObject p=all.optJSONObject(id);if(p==null)continue;if("emotional_echo".equals(p.optString("kind"))&&p.optInt("suggestion_expires_day",Integer.MAX_VALUE)<=day)p.put("suggestion_applies",false).put("pending_barbara_review",false);}return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    private static String canonicalRegion(String v){String n=norm(v);for(int i=0;i<REGION_TYPES.length;i++)if(n.equals(norm(REGION_TYPES[i]))||n.equals(norm(REGION_LABELS[i])))return REGION_TYPES[i];return "";}
    private static String regionLabel(String id){for(int i=0;i<REGION_TYPES.length;i++)if(REGION_TYPES[i].equals(id))return REGION_LABELS[i];return id;}
    private static String canonicalStorm(String v){String n=norm(v);String[][] a={{"scouring_sandstorm","vento de areia","sandstorm"},{"flame_storm","tempestade de chamas"},{"necrotic_storm","tempestade necrotica"},{"thrym_howl","uivo de thrym"}};for(String[] x:a)for(String q:x)if(n.equals(norm(q)))return x[0];return "";}
    private static String canonicalEcho(String v){String n=norm(v);String[][] a={{"boldness","ousadia"},{"doubt","duvida"},{"fear","medo"},{"hate","odio"},{"inspiration","inspiracao"},{"joy","alegria"},{"love","amor"},{"sadness","tristeza"}};for(String[] x:a)for(String q:x)if(n.equals(norm(q)))return x[0];return "";}
    private static String clean(String s,String d){String x=s==null?"":s.trim();return x.isEmpty()?d:x;}
    private static String norm(String s){String n=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static JSONObject copy(JSONObject o){JSONObject n=new JSONObject();if(o==null)return n;try{java.util.Iterator<String>it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.opt(k);if(v instanceof JSONObject)v=copy((JSONObject)v);else if(v instanceof JSONArray)v=copy((JSONArray)v);n.put(k,v);}}catch(Exception ignored){}return n;}
    private static JSONArray copy(JSONArray a){JSONArray n=new JSONArray();if(a==null)return n;try{for(int i=0;i<a.length();i++){Object v=a.opt(i);if(v instanceof JSONObject)v=copy((JSONObject)v);else if(v instanceof JSONArray)v=copy((JSONArray)v);n.put(v);}}catch(Exception ignored){}return n;}
}
