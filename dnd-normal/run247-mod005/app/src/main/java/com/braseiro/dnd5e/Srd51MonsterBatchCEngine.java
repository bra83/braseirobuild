package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Source-bound SRD 5.1 C-section tactical statblocks. */
public final class Srd51MonsterBatchCEngine {
    private Srd51MonsterBatchCEngine(){}
    public static final String BOOK="System Reference Document 5.1 (CC-BY-4.0)";
    public static final String LOCATOR="official SRD 5.1 monster stat blocks C";

    public static JSONObject install(){/*BR_JSON_WRAP_106*/try{
        JSONObject[] batch=new JSONObject[]{
            multi(monster("Centaur",12,45,"monstrosity",2,movement(50,0,0,0),abilities(18,14,14,9,13,11),13,
                actions(attack("Pike",6,"1d10+4","piercing",10),attack("Hooves",6,"2d6+4","bludgeoning",5),attack("Longbow",4,"1d8+2","piercing",600)),
                traits(trait("Charge","move_30ft_then_pike_extra_3d6"),trait("Multiattack Alternative","two_longbow_attacks_allowed"))),"Pike","Hooves"),
            multi(monster("Chain Devil",16,85,"fiend",8,movement(30,0,0,0),abilities(18,15,18,11,12,14),11,
                actions(attack("Chain",8,"2d6+4","slashing",10),save("Unnerving Mask","wis",14,"","","frightened","frightened_until_end_of_turn","reaction")),
                traits(trait("Devil's Sight","magical_darkness_no_impediment"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Animate Chains","rest_recharge_animate_up_to_four_chains"))),"Chain","Chain"),
            multi(monster("Chimera",14,114,"monstrosity",6,movement(30,60,0,0),abilities(19,11,19,3,14,10),18,
                actions(attack("Bite",7,"2d6+4","piercing",5),attack("Horns",7,"1d12+4","bludgeoning",5),attack("Claws",7,"2d6+4","slashing",5),save("Fire Breath","dex",15,"7d8","half","","cone_15_fire","5-6")),traits()),"Bite","Horns","Claws"),
            multi(monster("Chuul",16,93,"aberration",4,movement(30,0,0,30),abilities(19,10,16,5,11,5),14,
                actions(attack("Pincer",6,"2d6+4","bludgeoning",10),save("Tentacles","con",13,"","","poisoned","poisoned_and_paralyzed_1_minute_repeat_save","")),
                traits(trait("Amphibious","breathe_air_and_water"),trait("Sense Magic","detect_magic_120ft_at_will"))),"Pincer","Pincer"),
            multi(monster("Clay Golem",14,133,"construct",9,movement(20,0,0,0),abilities(20,9,18,3,8,1),9,
                actions(attackSaveRider("Slam",8,"2d10+5","bludgeoning",5,"con",15,"hp_max_reduction","reduce_hp_max_by_damage_until_restoration")),
                traits(trait("Acid Absorption","acid_damage_heals_instead"),trait("Berserk","start_turn_hp_60_or_less_roll_d6_6_berserk"),trait("Immutable Form","immune_form_alteration"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Magic Weapons","weapon_attacks_magical"),trait("Haste","recharge_5_6_ac_plus_2_dex_save_advantage_bonus_slam"))),"Slam","Slam"),
            multi(monster("Cloaker",14,78,"aberration",8,movement(10,40,0,0),abilities(17,15,12,13,12,14),11,
                actions(attack("Bite",6,"2d6+3","piercing",5),attack("Tail",6,"1d8+3","slashing",10),save("Moan","wis",13,"","","frightened","frightened_until_end_of_cloaker_next_turn","")),
                traits(trait("Damage Transfer","attached_target_takes_half_damage"),trait("False Appearance","motionless_indistinguishable_from_cloak"),trait("Light Sensitivity","bright_light_attack_and_sight_perception_disadvantage"),trait("Phantasms","rest_recharge_three_illusory_duplicates"))),"Bite","Tail"),
            multi(monster("Cloud Giant",14,200,"giant",9,movement(40,0,0,0),abilities(27,10,22,12,16,16),17,
                actions(attack("Morningstar",12,"3d8+8","piercing",10),attack("Rock",12,"4d10+8","bludgeoning",240)),
                traits(trait("Keen Smell","perception_advantage_smell"),trait("Innate Spellcasting","charisma_innate_spellcasting_profile"))),"Morningstar","Morningstar"),
            monster("Cockatrice",11,27,"monstrosity",0.5,movement(20,40,0,0),abilities(6,12,12,2,13,5),11,
                actions(attackSaveRider("Bite",3,"1d4+1","piercing",5,"con",11,"restrained_then_petrified","repeat_next_turn_failure_petrified_24h")),traits()),
            monster("Copper Dragon Wyrmling",16,22,"dragon",1,movement(30,60,30,0),abilities(15,12,13,14,11,13),14,
                actions(attack("Bite",4,"1d10+2","piercing",5),save("Acid Breath","dex",11,"4d8","half","","line_20x5_acid","5-6"),save("Slowing Breath","con",11,"","","slowed","no_reactions_half_speed_one_attack_action_or_bonus_1_minute_repeat_save","5-6")),traits()),
            monster("Couatl",19,97,"celestial",4,movement(30,90,0,0),abilities(16,20,17,18,20,18),15,
                actions(attackSaveRider("Bite",8,"1d6+5","piercing",5,"con",13,"poisoned_unconscious","poisoned_24h_unconscious_until_shaken_awake"),attack("Constrict",6,"2d6+3","bludgeoning",10)),
                traits(trait("Innate Spellcasting","charisma_innate_spellcasting_profile"),trait("Magic Weapons","weapon_attacks_magical"),trait("Shielded Mind","immune_scrying_emotion_thought_location_detection"),trait("Change Shape","polymorph_humanoid_or_beast_cr_at_most_4"))),
            multi(monster("Cyclops",14,138,"giant",6,movement(30,0,0,0),abilities(22,11,20,8,6,10),8,
                actions(attack("Greatclub",9,"3d8+6","bludgeoning",10),attack("Rock",9,"4d10+6","bludgeoning",120)),
                traits(trait("Poor Depth Perception","disadvantage_attack_targets_beyond_30ft"))),"Greatclub","Greatclub")
        };
        int scoped=0,installed=0;JSONArray ids=new JSONArray();for(JSONObject s:batch){String id=s.optString("id");ids.put(id);JSONObject scope=AuthorizedBestiaryScopeEngine.declare(new JSONObject().put("id",id).put("source_book",BOOK).put("source_locator",LOCATOR));if(scope.optBoolean("accepted"))scoped++;try{CompiledStatblockEngine.registerAuthorized(s);installed++;}catch(Exception ignored){}}
        return new JSONObject().put("monster_c_scope_declared",scoped).put("monster_c_statblocks_installed",installed).put("monster_c_ids",ids).put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source",LOCATOR);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    private static JSONObject monster(String id,int ac,int hp,String type,double cr,JSONObject movement,JSONObject abilities,int passive,JSONArray actions,JSONArray traits){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("id",id).put("source_book",BOOK).put("revision",1).put("armor_class",ac).put("hit_points",hp).put("creature_type",type).put("challenge_rating",cr).put("movement",movement).put("abilities",abilities).put("senses",new JSONObject().put("passive_perception",passive)).put("traits",traits).put("actions",actions).put("statblock_completeness","tactical_runtime").put("full_statblock_claim",false).put("mechanical_basis",new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock").put("content_name",id).put("source_book",BOOK).put("source_locator",LOCATOR));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject multi(JSONObject s,String... sequence){/*BR_JSON_WRAP_106*/try{JSONArray a=new JSONArray();for(String x:sequence)a.put(x);return s.put("multiattack",new JSONObject().put("sequence",a).put("count",sequence.length));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject movement(int walk,int fly,int climb,int swim){/*BR_JSON_WRAP_106*/try{JSONObject m=new JSONObject().put("walk_ft",walk);if(fly>0)m.put("fly_ft",fly);if(climb>0)m.put("climb_ft",climb);if(swim>0)m.put("swim_ft",swim);return m;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("str",str).put("dex",dex).put("con",con).put("int",intel).put("wis",wis).put("cha",cha);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONArray actions(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONArray traits(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONObject trait(String name,String mechanic){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("mechanic",mechanic);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject attack(String name,int toHit,String damage,String type,int reach){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("to_hit",toHit).put("damage",damage).put("damage_type",type).put("reach_ft",reach);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject attackSaveRider(String name,int toHit,String damage,String type,int reach,String saveAbility,int dc,String condition,String effect){/*BR_JSON_WRAP_106*/try{return attack(name,toHit,damage,type,reach).put("save_ability",saveAbility).put("save_dc",dc).put("condition_on_fail",condition).put("effect_on_fail",effect);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject save(String name,String ability,int dc,String damage,String onSave,String condition,String effect,String recharge){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("save_ability",ability).put("save_dc",dc).put("damage",damage).put("damage_on_save",onSave).put("damage_type",damageTypeFor(name)).put("condition_on_fail",condition).put("effect_on_fail",effect).put("recharge",recharge);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static String damageTypeFor(String name){String n=name==null?"":name.toLowerCase();if(n.contains("acid"))return "acid";if(n.contains("fire"))return "fire";return "";}
}
