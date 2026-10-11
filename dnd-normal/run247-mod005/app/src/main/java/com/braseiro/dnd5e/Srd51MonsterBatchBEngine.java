package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Source-bound executable SRD 5.1 monster batch (B-section).
 * Tactical runtime only: core combat stats, movement, abilities, senses,
 * selected traits, attack rolls and deterministic save actions are compiled.
 * Every record keeps full_statblock_claim=false until every rider/reaction/
 * spellcasting detail is represented by runtime-capable mechanics.
 */
public final class Srd51MonsterBatchBEngine {
    private Srd51MonsterBatchBEngine(){}
    public static final String BOOK="System Reference Document 5.1 (CC-BY-4.0)";
    public static final String LOCATOR="official SRD 5.1 monster stat blocks B";

    public static JSONObject install(){/*BR_JSON_WRAP_106*/try{
        JSONObject[] batch=new JSONObject[]{
            multi(monster("Balor",19,262,"fiend",19,movement(40,80,0,0),abilities(26,15,22,20,16,22),13,
                actions(attack("Longsword",14,"3d8+8 + 3d8","slashing + lightning",10),attack("Whip",14,"2d6+8 + 3d6","slashing + fire",30)),
                traits(trait("Death Throes","death_trigger_dex_dc20_20d6_fire_half"),trait("Fire Aura","adjacent_fire_aura_3d6"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Magic Weapons","weapon_attacks_magical"),trait("Teleport","teleport_120ft"))),"Longsword","Whip"),
            multi(monster("Barbed Devil",15,110,"fiend",5,movement(30,0,0,0),abilities(16,17,18,12,14,14),18,
                actions(attack("Claw",6,"1d6+3","piercing",5),attack("Tail",6,"2d6+3","piercing",5),attack("Hurl Flame",5,"3d6","fire",150)),
                traits(trait("Barbed Hide","grapple_start_damage_1d10_piercing"),trait("Devil's Sight","magical_darkness_no_impediment"),trait("Magic Resistance","save_advantage_vs_magic"))),"Claw","Claw","Tail"),
            monster("Banshee",12,58,"undead",4,movement(0,40,0,0),abilities(1,14,10,12,11,17),10,
                actions(attack("Corrupting Touch",4,"3d6+2","necrotic",5),save("Horrifying Visage","wis",13,"","","frightened","frightened_1_minute_repeat_save",""),save("Wail","con",13,"","3d6","","drop_to_0_hp_on_fail","1/day")),
                traits(trait("Detect Life","sense_living_5_miles"),trait("Incorporeal Movement","move_through_creatures_objects_difficult_terrain"))),
            monster("Basilisk",15,52,"monstrosity",3,movement(20,0,0,0),abilities(16,8,15,2,8,7),9,
                actions(attack("Bite",5,"2d6+3 + 2d6","piercing + poison",5)),
                traits(trait("Petrifying Gaze","start_turn_con_dc12_restrained_then_petrified"))),
            multi(monster("Bearded Devil",13,52,"fiend",3,movement(30,0,0,0),abilities(16,15,15,9,11,11),10,
                actions(attackSaveRider("Beard",5,"1d8+2","piercing",5,"con",12,"poisoned","poisoned_no_healing_repeat_save"),attackSaveRider("Glaive",5,"1d10+3","slashing",10,"con",12,"infernal_wound","ongoing_1d10_until_stanched_or_magical_healing")),
                traits(trait("Devil's Sight","magical_darkness_no_impediment"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Steadfast","not_frightened_near_ally"))),"Beard","Glaive"),
            multi(monster("Behir",17,168,"monstrosity",11,movement(50,0,40,0),abilities(23,16,18,7,14,12),16,
                actions(attack("Bite",10,"3d10+6","piercing",10),attack("Constrict",10,"2d10+6 + 2d10+6","bludgeoning + slashing",5),save("Lightning Breath","dex",16,"12d10","6d10","","line_20x5_lightning","5-6")),
                traits(trait("Swallow","grappled_medium_or_smaller_bite_then_swallow_6d6_acid_per_turn"))),"Bite","Constrict"),
            monster("Black Dragon Wyrmling",17,33,"dragon",2,movement(30,60,0,30),abilities(15,14,13,10,11,13),14,
                actions(attack("Bite",4,"1d10+2 + 1d4","piercing + acid",5),save("Acid Breath","dex",11,"5d8","half","","line_15x5_acid","5-6")),
                traits(trait("Amphibious","breathe_air_and_water"))),
            monster("Black Pudding",7,85,"ooze",4,movement(20,0,20,0),abilities(16,5,16,1,6,1),8,
                actions(attack("Pseudopod",5,"1d6+3 + 4d8","bludgeoning + acid",5)),
                traits(trait("Amorphous","move_through_1_inch_space"),trait("Corrosive Form","adjacent_touch_acid_and_nonmagical_gear_corrosion"),trait("Spider Climb","climb_difficult_surfaces_ceiling"),trait("Split","reaction_split_on_lightning_or_slashing_if_hp_10_plus"))),
            monster("Blink Dog",13,22,"fey",0.25,movement(40,0,0,0),abilities(12,17,12,10,13,11),13,
                actions(attack("Bite",3,"1d6+1","piercing",5)),
                traits(trait("Keen Hearing and Smell","perception_advantage_hearing_smell"),trait("Teleport","recharge_4_6_teleport_40ft_plus_bite"))),
            monster("Blood Hawk",12,7,"beast",0.125,movement(10,60,0,0),abilities(6,14,10,3,14,5),14,
                actions(attack("Beak",4,"1d4+2","piercing",5)),
                traits(trait("Keen Sight","perception_advantage_sight"),trait("Pack Tactics","attack_advantage_adjacent_ally"))),
            monster("Blue Dragon Wyrmling",17,52,"dragon",3,movement(30,60,0,0).put("burrow_ft",15),abilities(17,10,15,12,11,15),14,
                actions(attack("Bite",5,"1d10+3 + 1d6","piercing + lightning",5),save("Lightning Breath","dex",12,"4d10","half","","line_30x5_lightning","5-6")),traits()),
            multi(monster("Bone Devil",19,142,"fiend",9,movement(40,40,0,0),abilities(18,16,18,13,14,16),12,
                actions(attack("Claw",8,"1d8+4","slashing",10),attackSaveRider("Sting",8,"2d8+4 + 5d6","piercing + poison",10,"con",14,"poisoned","poisoned_1_minute_repeat_save")),
                traits(trait("Devil's Sight","magical_darkness_no_impediment"),trait("Magic Resistance","save_advantage_vs_magic"))),"Claw","Claw","Sting"),
            monster("Brass Dragon Wyrmling",16,16,"dragon",1,movement(30,60,0,0).put("burrow_ft",15),abilities(15,10,13,10,11,13),14,
                actions(attack("Bite",4,"1d10+2","piercing",5),save("Fire Breath","dex",11,"4d6","half","","line_20x5_fire","5-6"),save("Sleep Breath","con",11,"","","unconscious","unconscious_1_minute_breaks_on_damage_or_wake","5-6")),traits()),
            monster("Bronze Dragon Wyrmling",17,32,"dragon",2,movement(30,60,0,30),abilities(17,10,15,12,11,15),14,
                actions(attack("Bite",5,"1d10+3","piercing",5),save("Lightning Breath","dex",12,"3d10","half","","line_40x5_lightning","5-6"),save("Repulsion Breath","str",12,"","","","push_30ft_on_fail","5-6")),
                traits(trait("Amphibious","breathe_air_and_water"))),
            monster("Bugbear",16,27,"humanoid",1,movement(30,0,0,0),abilities(15,14,13,8,11,9),10,
                actions(attack("Morningstar",4,"2d8+2","piercing",5),attack("Javelin (melee)",4,"2d6+2","piercing",5),attack("Javelin (ranged)",4,"1d6+2","piercing",120)),
                traits(trait("Brute","extra_melee_weapon_damage_die"),trait("Surprise Attack","first_round_surprise_extra_2d6"))),
            monster("Bulette",17,94,"monstrosity",5,movement(40,0,0,0).put("burrow_ft",40),abilities(19,11,21,2,10,5),16,
                actions(attack("Bite",7,"4d12+4","piercing",5),save("Deadly Leap","str_or_dex",16,"3d6+4 + 3d6+4","half","prone","jump_15ft_land_area_push_5ft_on_success","")),
                traits(trait("Standing Leap","long_jump_30ft_high_jump_15ft_no_runup")))
        };
        int scoped=0,installed=0; JSONArray ids=new JSONArray();
        for(JSONObject s:batch){String id=s.optString("id");ids.put(id);JSONObject scope=AuthorizedBestiaryScopeEngine.declare(new JSONObject().put("id",id).put("source_book",BOOK).put("source_locator",LOCATOR));if(scope.optBoolean("accepted"))scoped++;try{CompiledStatblockEngine.registerAuthorized(s);installed++;}catch(Exception ignored){}}
        return new JSONObject().put("monster_b_scope_declared",scoped).put("monster_b_statblocks_installed",installed).put("monster_b_ids",ids).put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source",LOCATOR);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    private static JSONObject monster(String id,int ac,int hp,String type,double cr,JSONObject movement,JSONObject abilities,int passive,JSONArray actions,JSONArray traits){/*BR_JSON_WRAP_106*/try{
        return new JSONObject().put("id",id).put("source_book",BOOK).put("revision",1).put("armor_class",ac).put("hit_points",hp).put("creature_type",type).put("challenge_rating",cr)
            .put("movement",movement).put("abilities",abilities).put("senses",new JSONObject().put("passive_perception",passive)).put("traits",traits).put("actions",actions)
            .put("statblock_completeness","tactical_runtime").put("full_statblock_claim",false)
            .put("mechanical_basis",new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock").put("content_name",id).put("source_book",BOOK).put("source_locator",LOCATOR));
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject multi(JSONObject s,String... sequence){/*BR_JSON_WRAP_106*/try{JSONArray a=new JSONArray();for(String x:sequence)a.put(x);return s.put("multiattack",new JSONObject().put("sequence",a).put("count",sequence.length));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject movement(int walk,int fly,int climb,int swim){/*BR_JSON_WRAP_106*/try{JSONObject m=new JSONObject();if(walk>=0)m.put("walk_ft",walk);if(fly>0)m.put("fly_ft",fly);if(climb>0)m.put("climb_ft",climb);if(swim>0)m.put("swim_ft",swim);return m;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("str",str).put("dex",dex).put("con",con).put("int",intel).put("wis",wis).put("cha",cha);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONArray actions(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONArray traits(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONObject trait(String name,String mechanic){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("mechanic",mechanic);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject attack(String name,int toHit,String damage,String type,int reach){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("to_hit",toHit).put("damage",damage).put("damage_type",type).put("reach_ft",reach);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject attackSaveRider(String name,int toHit,String damage,String type,int reach,String saveAbility,int dc,String condition,String effect){/*BR_JSON_WRAP_106*/try{return attack(name,toHit,damage,type,reach).put("save_ability",saveAbility).put("save_dc",dc).put("condition_on_fail",condition).put("effect_on_fail",effect);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject save(String name,String ability,int dc,String damage,String onSave,String condition,String effect,String recharge){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("save_ability",ability).put("save_dc",dc).put("damage",damage).put("damage_on_save",onSave).put("damage_type",damageTypeFor(name)).put("condition_on_fail",condition).put("effect_on_fail",effect).put("recharge",recharge);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static String damageTypeFor(String name){String n=name==null?"":name.toLowerCase();if(n.contains("acid"))return "acid";if(n.contains("lightning"))return "lightning";if(n.contains("fire"))return "fire";if(n.contains("wail"))return "psychic";if(n.contains("leap"))return "bludgeoning + slashing";return "";}
}
