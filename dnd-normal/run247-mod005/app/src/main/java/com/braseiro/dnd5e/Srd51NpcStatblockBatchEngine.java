package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Executable NPC archetypes from the official SRD 5.1 NPC appendix.
 * These records intentionally claim tactical_runtime, not full_runtime:
 * combat statistics, movement, abilities, senses, traits and weapon actions
 * are compiled; full spell lists and every narrative rider remain outside the claim.
 */
public final class Srd51NpcStatblockBatchEngine {
    private Srd51NpcStatblockBatchEngine(){}
    public static final String BOOK = "System Reference Document 5.1 (CC-BY-4.0)";
    public static final String LOCATOR = "official SRD 5.1 NPC appendix pp. 394-402";

    public static JSONObject install(){/*BR_JSON_WRAP_106*/try{
        JSONObject[] batch = new JSONObject[]{
            npc("Acolyte",10,9,0.25,abilities(10,10,10,10,14,11),12,
                actions(action("Club",2,"1d4","bludgeoning",5)),
                traits(trait("Spellcasting","wisdom_spellcasting_profile"))),
            npc("Archmage",12,99,12,abilities(10,14,12,20,15,16),12,
                actions(action("Dagger",6,"1d4+2","piercing",5)),
                traits(trait("Magic Resistance","save_advantage_vs_magic"),trait("Spellcasting","intelligence_spellcasting_profile"))),
            npc("Assassin",15,78,8,abilities(11,16,14,13,11,10),13,
                actions(action("Shortsword",6,"1d6+3 + 7d6","piercing + poison",5),action("Light Crossbow",6,"1d8+3 + 7d6","piercing + poison",320)),
                traits(trait("Assassinate","first_turn_advantage_surprise_critical"),trait("Evasion","dex_save_evasion"))),
            npc("Bandit",12,11,0.125,abilities(11,12,12,10,10,10),10,
                actions(action("Scimitar",3,"1d6+1","slashing",5),action("Light Crossbow",3,"1d8+1","piercing",320)),traits()),
            multi(npc("Bandit Captain",15,65,2,abilities(15,16,14,14,11,14),10,
                actions(action("Scimitar",5,"1d6+3","slashing",5),action("Dagger",5,"1d4+3","piercing",60)),
                traits(trait("Parry","reaction_ac_bonus_2_melee"))), "Scimitar","Scimitar","Dagger"),
            npc("Berserker",13,67,2,abilities(16,12,17,9,11,9),10,
                actions(action("Greataxe",5,"1d12+3","slashing",5)),traits(trait("Reckless","melee_advantage_tradeoff"))),
            npc("Commoner",10,4,0,abilities(10,10,10,10,10,10),10,
                actions(action("Club",2,"1d4","bludgeoning",5)),traits()),
            npc("Cultist",12,9,0.125,abilities(11,12,10,10,11,10),10,
                actions(action("Scimitar",3,"1d6+1","slashing",5)),traits(trait("Dark Devotion","save_advantage_charmed_frightened"))),
            multi(npc("Cult Fanatic",13,33,2,abilities(11,14,12,10,13,14),11,
                actions(action("Dagger",4,"1d4+2","piercing",60)),
                traits(trait("Dark Devotion","save_advantage_charmed_frightened"),trait("Spellcasting","wisdom_spellcasting_profile"))), "Dagger","Dagger"),
            npc("Druid",11,27,2,abilities(10,12,13,12,15,11),14,
                actions(action("Quarterstaff",2,"1d6","bludgeoning",5)),traits(trait("Spellcasting","wisdom_spellcasting_profile"))),
            multi(npc("Gladiator",16,112,5,abilities(18,15,16,10,12,15),11,
                actions(action("Spear",7,"2d6+4","piercing",60),action("Shield Bash",7,"2d4+4","bludgeoning",5)),
                traits(trait("Brave","save_advantage_frightened"),trait("Brute","extra_melee_weapon_damage_die"),trait("Parry","reaction_ac_bonus_3_melee"))), "Spear","Spear","Spear"),
            npc("Guard",16,11,0.125,abilities(13,12,12,10,11,10),12,
                actions(action("Spear",3,"1d6+1","piercing",60)),traits()),
            multi(npc("Knight",18,52,3,abilities(16,11,14,11,11,15),10,
                actions(action("Greatsword",5,"2d6+3","slashing",5),action("Heavy Crossbow",2,"1d10","piercing",400)),
                traits(trait("Brave","save_advantage_frightened"),trait("Parry","reaction_ac_bonus_2_melee"))), "Greatsword","Greatsword"),
            npc("Mage",12,40,6,abilities(9,14,11,17,12,11),11,
                actions(action("Dagger",5,"1d4+2","piercing",60)),traits(trait("Spellcasting","intelligence_spellcasting_profile"))),
            npc("Noble",15,9,0.125,abilities(11,12,11,12,14,16),12,
                actions(action("Rapier",3,"1d8+1","piercing",5)),traits(trait("Parry","reaction_ac_bonus_2_melee"))),
            npc("Priest",13,27,2,abilities(10,10,12,13,16,13),13,
                actions(action("Mace",2,"1d6","bludgeoning",5)),
                traits(trait("Divine Eminence","bonus_action_spell_slot_radiant_rider"),trait("Spellcasting","wisdom_spellcasting_profile"))),
            multi(npc("Scout",13,16,0.5,abilities(11,14,12,11,13,11),15,
                actions(action("Shortsword",4,"1d6+2","piercing",5),action("Longbow",4,"1d8+2","piercing",600)),
                traits(trait("Keen Hearing and Sight","perception_advantage_hearing_sight"))), "Shortsword","Shortsword"),
            multi(npc("Spy",12,27,1,abilities(10,15,10,12,14,16),16,
                actions(action("Shortsword",4,"1d6+2","piercing",5),action("Hand Crossbow",4,"1d6+2","piercing",120)),
                traits(trait("Cunning Action","bonus_dash_disengage_hide"),trait("Sneak Attack","conditional_extra_2d6_once_per_turn"))), "Shortsword","Shortsword"),
            multi(npc("Thug",11,32,0.5,abilities(15,11,14,10,10,11),10,
                actions(action("Mace",4,"1d6+2","bludgeoning",5),action("Heavy Crossbow",2,"1d10","piercing",400)),
                traits(trait("Pack Tactics","attack_advantage_adjacent_ally"))), "Mace","Mace"),
            npc("Tribal Warrior",12,11,0.125,abilities(13,11,12,8,11,8),10,
                actions(action("Spear",3,"1d6+1","piercing",60)),traits(trait("Pack Tactics","attack_advantage_adjacent_ally"))),
            multi(npc("Veteran",17,58,3,abilities(16,13,14,10,11,10),12,
                actions(action("Longsword",5,"1d8+3","slashing",5),action("Shortsword",5,"1d6+3","piercing",5),action("Heavy Crossbow",3,"1d10+1","piercing",400)),traits()),
                "Longsword","Longsword","Shortsword")
        };
        int scoped=0,installed=0;
        JSONArray ids=new JSONArray();
        for(JSONObject s:batch){
            String id=s.optString("id"); ids.put(id);
            JSONObject scope=AuthorizedBestiaryScopeEngine.declare(new JSONObject().put("id",id).put("source_book",BOOK).put("source_locator",LOCATOR));
            if(scope.optBoolean("accepted"))scoped++;
            try{CompiledStatblockEngine.registerAuthorized(s);installed++;}catch(Exception ignored){}
        }
        return new JSONObject().put("npc_scope_declared",scoped).put("npc_statblocks_installed",installed).put("npc_ids",ids)
            .put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source","official D&D SRD 5.1 NPC appendix");
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    private static JSONObject npc(String id,int ac,int hp,double cr,JSONObject abilities,int passive,JSONArray actions,JSONArray traits){/*BR_JSON_WRAP_106*/try{
        return new JSONObject().put("id",id).put("source_book",BOOK).put("revision",1).put("armor_class",ac).put("hit_points",hp)
            .put("creature_type","humanoid").put("challenge_rating",cr).put("movement",new JSONObject().put("walk_ft",30))
            .put("abilities",abilities).put("senses",new JSONObject().put("passive_perception",passive)).put("traits",traits).put("actions",actions)
            .put("statblock_completeness","tactical_runtime").put("full_statblock_claim",false)
            .put("mechanical_basis",new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock")
                .put("content_name",id).put("source_book",BOOK).put("source_locator",LOCATOR));
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject multi(JSONObject s,String... sequence){/*BR_JSON_WRAP_106*/try{JSONArray a=new JSONArray();for(String x:sequence)a.put(x);return s.put("multiattack",new JSONObject().put("sequence",a).put("count",sequence.length));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("str",str).put("dex",dex).put("con",con).put("int",intel).put("wis",wis).put("cha",cha);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONArray actions(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONArray traits(JSONObject... x){JSONArray a=new JSONArray();for(JSONObject o:x)a.put(o);return a;}
    private static JSONObject trait(String name,String mechanic){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("mechanic",mechanic);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject action(String name,int toHit,String damage,String type,int rangeOrReach){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("name",name).put("to_hit",toHit).put("damage",damage).put("damage_type",type).put("reach_ft",rangeOrReach);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
