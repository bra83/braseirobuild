package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Large source-bound SRD 5.1 Faerun-core encounter batch for the existing tactical runtime. */
public final class Srd51FaerunCoreMonsterEngine {
    private Srd51FaerunCoreMonsterEngine(){}
    public static final String BOOK="System Reference Document 5.1 (CC-BY-4.0)";
    public static final String LOCATOR="official SRD 5.1 monster stat blocks - Faerun core closure batch";

    public static JSONObject install(){/*BR_JSON_WRAP_106*/try{
        JSONObject[] batch=new JSONObject[]{
            monster("Darkmantle",11,22,"monstrosity",0.5,movement(10,30,0,0),abilities(16,12,13,2,10,5),10,
                actions(attack("Crush",5,"1d6+3","bludgeoning",5),save("Darkness Aura","con",0,"","","","15ft_magical_darkness_once_per_day","1/day")),
                traits(trait("Echolocation","blind_without_hearing"),trait("False Appearance","motionless_indistinguishable_from_cave_feature"))),
            multi(monster("Doppelganger",14,52,"monstrosity",3,movement(30,0,0,0),abilities(11,18,14,11,12,14),11,
                actions(attack("Slam",6,"1d6+4","bludgeoning",5)),
                traits(trait("Shapechanger","polymorph_humanoid_form"),trait("Ambusher","advantage_vs_surprised"),trait("Surprise Attack","extra_3d6_first_round_vs_surprised"),trait("Read Thoughts","surface_thoughts_action"))),"Slam","Slam"),
            monster("Dryad",11,22,"fey",1,movement(30,0,0,0),abilities(10,12,11,14,15,18),14,
                actions(attack("Club",2,"1d4","bludgeoning",5),save("Fey Charm","wis",14,"","","charmed","charmed_24h_or_until_harmed","")),
                traits(trait("Innate Spellcasting","wisdom_innate_spellcasting_profile"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Speak with Beasts and Plants","communicate_with_beasts_and_plants"),trait("Tree Stride","move_between_trees_within_60ft"))),
            multi(monster("Earth Elemental",17,126,"elemental",5,movement(30,0,30,0),abilities(20,8,20,5,10,5),10,
                actions(attack("Slam",8,"2d8+5","bludgeoning",10)),traits(trait("Earth Glide","burrow_nonmagical_unworked_earth_stone"),trait("Siege Monster","double_damage_objects_structures"))),"Slam","Slam"),
            monster("Ettercap",13,44,"monstrosity",2,movement(30,0,30,0),abilities(14,15,13,7,12,8),13,
                actions(attackSaveRider("Bite",4,"1d8+2","piercing",5,"con",11,"poisoned_damage","extra_1d8_poison_half_on_save"),attack("Claws",4,"2d4+2","slashing",5),save("Web","str",11,"","","restrained","restrained_by_web_escape_dc11","5-6")),
                traits(trait("Spider Climb","climb_difficult_surfaces_including_ceiling"),trait("Web Sense","know_location_of_creatures_on_same_web"),trait("Web Walker","ignore_web_movement_restrictions"))),
            multi(monster("Ettin",12,85,"giant",4,movement(40,0,0,0),abilities(21,8,17,6,10,8),14,
                actions(attack("Battleaxe",7,"2d8+5","slashing",5),attack("Morningstar",7,"2d8+5","piercing",5)),
                traits(trait("Two Heads","advantage_perception_and_saves_vs_blinded_charmed_deafened_frightened_stunned_unconscious"),trait("Wakeful","one_head_awake_while_other_sleeps"))),"Battleaxe","Morningstar"),
            multi(monster("Fire Giant",18,162,"giant",9,movement(30,0,0,0),abilities(25,9,23,10,14,13),16,
                actions(attack("Greatsword",11,"6d6+7","slashing",10),attack("Rock",11,"4d10+7","bludgeoning",240)),traits()),"Greatsword","Greatsword"),
            multi(monster("Flesh Golem",9,93,"construct",5,movement(30,0,0,0),abilities(19,9,18,6,10,5),10,
                actions(attack("Slam",7,"2d8+4","bludgeoning",5)),
                traits(trait("Berserk","low_hp_berserk_check"),trait("Aversion of Fire","fire_damage_disadvantage_until_next_turn"),trait("Immutable Form","immune_form_alteration"),trait("Lightning Absorption","lightning_damage_heals_instead"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Magic Weapons","weapon_attacks_magical"))),"Slam","Slam"),
            multi(monster("Gargoyle",15,52,"elemental",2,movement(30,60,0,0),abilities(15,11,16,6,11,7),10,
                actions(attack("Bite",4,"1d6+2","piercing",5),attack("Claws",4,"1d6+2","slashing",5)),
                traits(trait("False Appearance","motionless_indistinguishable_from_statue"),trait("Stone Body","resists_nonmagical_weapon_damage_except_adamantine"))),"Bite","Claws"),
            monster("Gelatinous Cube",6,84,"ooze",2,movement(15,0,0,0),abilities(14,3,20,1,6,1),8,
                actions(attack("Pseudopod",4,"3d6","acid",5),save("Engulf","dex",12,"3d6","","restrained","engulfed_restrained_and_acid_damage","")),
                traits(trait("Ooze Cube","fills_space_and_can_be_entered"),trait("Transparent","hard_to_spot_while_still"))),
            monster("Ghost",11,45,"undead",4,movement(0,40,0,0),abilities(7,13,10,10,12,17),11,
                actions(attack("Withering Touch",5,"4d6+3","necrotic",5),save("Horrifying Visage","wis",13,"","","frightened","frightened_1_minute_age_if_failed_by_5",""),save("Possession","cha",13,"","","possessed","possess_humanoid_until_ended","6")),
                traits(trait("Ethereal Sight","see_ethereal_60ft"),trait("Incorporeal Movement","move_through_creatures_objects"),trait("Etherealness","shift_ethereal_material_action"))),
            monster("Gibbering Mouther",9,67,"aberration",2,movement(10,0,0,10),abilities(10,8,16,3,10,6),10,
                actions(attack("Bites",2,"5d6","piercing",5),save("Blinding Spittle","dex",13,"","","blinded","blinded_until_end_next_turn","5-6")),
                traits(trait("Aberrant Ground","nearby_ground_difficult_terrain"),trait("Gibbering","wis_save_or_no_reaction_random_action"))),
            monster("Gnoll",15,22,"humanoid",0.5,movement(30,0,0,0),abilities(14,12,11,6,10,7),10,
                actions(attack("Bite",4,"1d4+2","piercing",5),attack("Spear",4,"1d6+2","piercing",20),attack("Longbow",3,"1d8+1","piercing",320)),traits(trait("Rampage","bonus_move_and_bite_after_reducing_enemy_to_0"))),
            monster("Goblin",15,7,"humanoid",0.25,movement(30,0,0,0),abilities(8,14,10,10,8,8),9,
                actions(attack("Scimitar",4,"1d6+2","slashing",5),attack("Shortbow",4,"1d6+2","piercing",320)),traits(trait("Nimble Escape","bonus_action_disengage_or_hide"))),
            monster("Gorgon",19,114,"monstrosity",5,movement(40,0,0,0),abilities(20,11,18,2,12,7),11,
                actions(attack("Gore",8,"2d12+5","piercing",5),save("Petrifying Breath","con",13,"","","restrained_then_petrified","repeat_save_end_next_turn_failure_petrified","5-6")),traits(trait("Trampling Charge","charge_gore_then_bonus_hooves_on_failed_str_save"))),
            monster("Gray Ooze",8,22,"ooze",0.5,movement(10,0,10,0),abilities(12,6,16,1,6,2),8,
                actions(attack("Pseudopod",3,"1d6+1","bludgeoning",5).put("extra_damage","2d6 acid")),traits(trait("Amorphous","move_through_1inch_space"),trait("Corrode Metal","damages_nonmagical_metal_armor_weapons"),trait("False Appearance","motionless_indistinguishable_from_oily_pool"))),
            monster("Green Hag",17,82,"fey",3,movement(30,0,0,30),abilities(18,12,16,13,14,14),14,
                actions(attack("Claws",6,"2d8+4","slashing",5)),traits(trait("Amphibious","breathe_air_and_water"),trait("Innate Spellcasting","charisma_innate_spellcasting_profile"),trait("Mimicry","mimic_animal_humanoid_sounds"),trait("Illusory Appearance","at_will_visual_disguise"),trait("Invisible Passage","turn_invisible_until_attack_spell_or_concentration_ends"))),
            monster("Grick",14,27,"monstrosity",2,movement(30,0,30,0),abilities(14,14,11,3,14,5),12,
                actions(attack("Tentacles",4,"2d6+2","bludgeoning",5),attack("Beak",4,"1d6+2","piercing",5)),traits(trait("Stone Camouflage","stealth_advantage_rocky_terrain"))),
            multi(monster("Griffon",12,59,"monstrosity",2,movement(30,80,0,0),abilities(18,15,16,2,13,8),15,
                actions(attack("Beak",6,"1d8+4","piercing",5),attack("Claws",6,"2d6+4","slashing",5)),traits(trait("Keen Sight","perception_advantage_sight"))),"Beak","Claws"),
            monster("Harpy",11,38,"monstrosity",1,movement(20,40,0,0),abilities(12,13,12,7,10,13),10,
                actions(attack("Claws",3,"2d4+1","slashing",5),attack("Club",3,"1d4+1","bludgeoning",5),save("Luring Song","wis",11,"","","charmed","charmed_and_moves_toward_harpy","")),traits()),
            monster("Hell Hound",15,45,"fiend",3,movement(50,0,0,0),abilities(17,12,14,6,13,6),15,
                actions(attack("Bite",5,"1d8+3","piercing",5).put("extra_damage","2d6 fire"),save("Fire Breath","dex",12,"6d6","half","","cone_15_fire","5-6")),traits(trait("Keen Hearing and Smell","perception_advantage_hearing_smell"),trait("Pack Tactics","attack_advantage_with_adjacent_ally"))),
            multi(monster("Hill Giant",13,105,"giant",5,movement(40,0,0,0),abilities(21,8,19,5,9,6),12,
                actions(attack("Greatclub",8,"3d8+5","bludgeoning",10),attack("Rock",8,"3d10+5","bludgeoning",240)),traits()),"Greatclub","Greatclub"),
            multi(monster("Hippogriff",11,19,"monstrosity",1,movement(40,60,0,0),abilities(17,13,13,2,12,8),15,
                actions(attack("Beak",5,"1d10+3","piercing",5),attack("Claws",5,"2d6+3","slashing",5)),traits(trait("Keen Sight","perception_advantage_sight"))),"Beak","Claws"),
            monster("Hobgoblin",18,11,"humanoid",0.5,movement(30,0,0,0),abilities(13,12,12,10,10,9),10,
                actions(attack("Longsword",3,"1d8+1","slashing",5),attack("Longbow",3,"1d8+1","piercing",600)),traits(trait("Martial Advantage","once_per_turn_extra_2d6_with_ally_adjacent_to_target"))),
            monster("Hydra",15,172,"monstrosity",8,movement(30,0,0,30),abilities(20,12,20,2,10,7),16,
                actions(attack("Bite",8,"1d10+5","piercing",10)),traits(trait("Hold Breath","hold_breath_1_hour"),trait("Multiple Heads","starts_five_heads_loses_head_on_25_damage_single_turn_regrows_two_without_fire"),trait("Reactive Heads","extra_reactions_per_extra_head"),trait("Wakeful","one_head_awake"))),
            monster("Imp",13,10,"fiend",1,movement(20,40,0,0),abilities(6,17,13,11,12,14),11,
                actions(attackSaveRider("Sting",5,"1d4+3","piercing",5,"con",11,"poison_damage","extra_3d6_poison_half_on_save")),traits(trait("Shapechanger","raven_rat_spider_forms"),trait("Devil's Sight","magical_darkness_no_impediment"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Invisibility","invisible_until_attack_or_concentration_ends"))),
            multi(monster("Invisible Stalker",14,104,"elemental",6,movement(50,50,0,0),abilities(16,19,14,10,15,11),18,
                actions(attack("Slam",6,"2d6+3","bludgeoning",5)),traits(trait("Invisibility","permanently_invisible"),trait("Faultless Tracker","knows_direction_distance_to_quarry_same_plane"))),"Slam","Slam"),
            multi(monster("Iron Golem",20,210,"construct",16,movement(30,0,0,0),abilities(24,9,20,3,11,1),10,
                actions(attack("Slam",13,"3d8+7","bludgeoning",5),attack("Sword",13,"3d10+7","slashing",10),save("Poison Breath","con",19,"10d8","half","","cone_15_poison","6")),traits(trait("Fire Absorption","fire_damage_heals_instead"),trait("Immutable Form","immune_form_alteration"),trait("Magic Resistance","save_advantage_vs_magic"),trait("Magic Weapons","weapon_attacks_magical"))),"Slam","Slam"),
            monster("Kobold",12,5,"humanoid",0.125,movement(30,0,0,0),abilities(7,15,9,8,7,8),8,
                actions(attack("Dagger",4,"1d4+2","piercing",5),attack("Sling",4,"1d4+2","bludgeoning",120)),traits(trait("Pack Tactics","attack_advantage_with_adjacent_ally"),trait("Sunlight Sensitivity","sunlight_attack_and_sight_perception_disadvantage"))),
            monster("Ogre",11,59,"giant",2,movement(40,0,0,0),abilities(19,8,16,5,7,7),8,
                actions(attack("Greatclub",6,"2d8+4","bludgeoning",5),attack("Javelin",6,"2d6+4","piercing",120)),traits()),
            multi(monster("Owlbear",13,59,"monstrosity",3,movement(40,0,0,0),abilities(20,12,17,3,12,7),13,
                actions(attack("Beak",7,"1d10+5","piercing",5),attack("Claws",7,"2d8+5","slashing",5)),traits(trait("Keen Sight and Smell","perception_advantage_sight_smell"))),"Beak","Claws"),
            monster("Pegasus",12,59,"celestial",2,movement(60,90,0,0),abilities(18,15,16,10,15,13),16,
                actions(attack("Hooves",6,"2d6+4","bludgeoning",5)),traits()),
            monster("Phase Spider",13,32,"monstrosity",3,movement(30,0,30,0),abilities(15,15,12,6,10,6),10,
                actions(attackSaveRider("Bite",4,"1d10+2","piercing",5,"con",11,"poison_damage","extra_4d8_poison_half_on_save")),traits(trait("Ethereal Jaunt","bonus_action_material_ethereal_shift"),trait("Spider Climb","climb_difficult_surfaces_including_ceiling"),trait("Web Walker","ignore_web_movement_restrictions"))),
            monster("Rust Monster",14,27,"monstrosity",0.5,movement(40,0,0,0),abilities(13,12,13,2,13,6),11,
                actions(attack("Bite",3,"1d8+1","piercing",5),save("Antennae","dex",0,"","","","corrode_nonmagical_ferrous_metal_object","")),traits(trait("Iron Scent","pinpoint_ferrous_metal_30ft"),trait("Rust Metal","corrode_metal_weapon_on_hit"))),
            monster("Skeleton",13,13,"undead",0.25,movement(30,0,0,0),abilities(10,14,15,6,8,5),9,
                actions(attack("Shortsword",4,"1d6+2","piercing",5),attack("Shortbow",4,"1d6+2","piercing",320)),traits(trait("Brittle Bones","vulnerable_bludgeoning"),trait("Undead Nature","immune_poison_and_exhaustion"))),
            monster("Stirge",14,2,"beast",0.125,movement(10,40,0,0),abilities(4,16,11,2,8,6),9,
                actions(attack("Blood Drain",5,"1d4+3","piercing",5)),traits(trait("Attach","on_hit_attach_and_auto_drain_until_detached"))),
            multi(monster("Troll",15,84,"giant",5,movement(30,0,0,0),abilities(18,13,20,7,9,7),12,
                actions(attack("Bite",7,"1d6+4","piercing",5),attack("Claw",7,"2d6+4","slashing",5)),traits(trait("Keen Smell","perception_advantage_smell"),trait("Regeneration","regain_10_hp_start_turn_unless_acid_or_fire_since_previous_turn"))),"Bite","Claw","Claw"),
            multi(monster("Werewolf",11,58,"humanoid",3,movement(30,0,0,0),abilities(15,13,14,10,11,10),14,
                actions(attackSaveRider("Bite",4,"1d8+2","piercing",5,"con",12,"lycanthropy","humanoid_target_cursed_with_werewolf_lycanthropy"),attack("Claws",4,"2d4+2","slashing",5)),traits(trait("Shapechanger","humanoid_hybrid_wolf_forms"),trait("Keen Hearing and Smell","perception_advantage_hearing_smell"),trait("Damage Immunity","nonmagical_nonsilvered_bludgeoning_piercing_slashing"))),"Bite","Claws"),
            multi(monster("Wyvern",13,110,"dragon",6,movement(20,80,0,0),abilities(19,10,16,5,12,6),14,
                actions(attack("Bite",7,"2d6+4","piercing",10),attackSaveRider("Stinger",7,"2d6+4","piercing",10,"con",15,"poison_damage","extra_7d6_poison_half_on_save"),attack("Claws",7,"2d8+4","slashing",5)),traits()),"Bite","Stinger"),
            monster("Orc",13,15,"humanoid",0.5,movement(30,0,0,0),abilities(16,12,16,7,11,10),10,
                actions(attack("Greataxe",5,"1d12+3","slashing",5),attack("Javelin",5,"1d6+3","piercing",120)),traits(trait("Aggressive","bonus_action_move_up_to_speed_toward_visible_or_heard_hostile"))),
            monster("Zombie",8,22,"undead",0.25,movement(20,0,0,0),abilities(13,6,16,3,6,5),8,
                actions(attack("Slam",3,"1d6+1","bludgeoning",5)),traits(trait("Undead Fortitude","on_lethal_damage_con_save_dc_5_plus_damage_except_radiant_or_crit"),trait("Undead Nature","does_not_require_air_food_drink_sleep")))
        };
        int scoped=0,installed=0;JSONArray ids=new JSONArray();for(JSONObject s:batch){String id=s.optString("id");ids.put(id);JSONObject scope=AuthorizedBestiaryScopeEngine.declare(new JSONObject().put("id",id).put("source_book",BOOK).put("source_locator",LOCATOR));if(scope.optBoolean("accepted"))scoped++;try{CompiledStatblockEngine.registerAuthorized(s);installed++;}catch(Exception ignored){}}
        return new JSONObject().put("faerun_core_scope_declared",scoped).put("faerun_core_statblocks_installed",installed).put("faerun_core_ids",ids).put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source",LOCATOR);
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
    private static String damageTypeFor(String name){String n=name==null?"":name.toLowerCase();if(n.contains("acid"))return "acid";if(n.contains("fire"))return "fire";if(n.contains("poison"))return "poison";if(n.contains("lightning"))return "lightning";return "";}
}
