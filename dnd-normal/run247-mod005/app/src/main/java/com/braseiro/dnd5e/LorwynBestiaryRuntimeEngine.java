package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** LFL bestiary compiled from bestiary-lfl.json; deterministic and no embedded source prose. */
public final class LorwynBestiaryRuntimeEngine {
    private LorwynBestiaryRuntimeEngine(){}
    public static final String BOOK="Lorwyn: First Light";
    public static final String SOURCE_CODE="LFL";
    public static final String SOURCE_BLOB="8f13baea417997bd64c0d2838c178bedc1ab0777";

    private static final String[] IDS={
        "Incarnation of Transience","Incarnation of Vibrance","Lorwyn Giant","Merrow Doublespeaker",
        "Merrow Haranguer","Noggle Ransacker","Noggle Wild Mage","Shadowmoor Giant"
    };

    private static JSONObject action(String name,String category){JSONObject o=new JSONObject();try{return o.put("name",name).put("category",category);}catch(Exception e){return o;}}
    private static JSONObject move(int walk){JSONObject o=new JSONObject();try{return o.put("walk",walk);}catch(Exception e){return o;}}
    private static JSONObject moveSwim(int walk,int swim){JSONObject o=move(walk);try{o.put("swim",swim);}catch(Exception ignored){}return o;}
    private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){JSONObject o=new JSONObject();try{return o.put("str",str).put("dex",dex).put("con",con).put("int",intel).put("wis",wis).put("cha",cha);}catch(Exception e){return o;}}
    private static JSONObject senses(int passive,String raw){JSONObject o=new JSONObject();try{return o.put("passive_perception",passive).put("raw",raw);}catch(Exception e){return o;}}
    private static JSONObject base(String id,int ac,int hp,String formula,String type,double cr,JSONObject movement,JSONObject stats,JSONObject sensory){JSONObject o=new JSONObject();try{return o
        .put("id",id).put("source_book",BOOK).put("revision",1).put("armor_class",ac).put("hit_points",hp).put("hit_points_formula",formula)
        .put("creature_type",type).put("challenge_rating",cr).put("movement",movement).put("abilities",stats).put("senses",sensory)
        .put("traits",new JSONArray()).put("actions",new JSONArray()).put("special_actions",new JSONArray())
        .put("resistances","").put("vulnerabilities","").put("immunities","").put("condition_immunities","")
        .put("saving_throws",new JSONObject()).put("skills",new JSONObject()).put("languages","")
        .put("statblock_completeness","tactical_runtime").put("full_statblock_claim",false)
        .put("mechanical_basis",new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock").put("content_name",id).put("source_book",BOOK)
            .put("source_locator","5e.tools data/bestiary/bestiary-lfl.json blob "+SOURCE_BLOB))
        .put("source_fidelity",new JSONObject().put("source_code",SOURCE_CODE).put("source_blob",SOURCE_BLOB).put("auto_rolls",false).put("raw_prose_embedded",false));
    }catch(Exception e){return o;}}

    private static JSONObject transience(){JSONObject o=base("Incarnation of Transience",16,142,"15d12 + 45","fey",7,moveSwim(40,40),abilities(21,20,17,10,14,16),senses(12,"Darkvision 60 ft."));try{
        o.put("saving_throws",new JSONObject().put("dex","+8").put("con","+6")).put("languages","Sylvan; telepathy 120 ft.")
         .put("condition_immunities","grappled, paralyzed, petrified, prone, restrained");
        o.optJSONArray("traits").put(new JSONObject().put("name","Ephemeral Movement").put("mechanic","lfl_ephemeral_movement").put("object_end_damage","1d10 force explicit"))
         .put(new JSONObject().put("name","Magic Resistance").put("mechanic","save_advantage_vs_magic"));
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Bite","action").put("to_hit",8).put("damage","2d8 + 5").put("damage_type","piercing").put("reach_ft",5))
         .put(action("Miasmic Globule","action").put("to_hit",8).put("damage","6d4").put("damage_type","acid").put("reach_ft",120).put("range_normal_ft",60).put("range_long_ft",120).put("effect_on_hit","speed -10 ft until end of incarnation next turn"))
         .put(action("Spectral Stampede","action").put("save_ability","dex").put("save_dc",16).put("damage","4d8").put("damage_on_save","half of 4d8").put("damage_type","force").put("effect_on_fail","Prone if Huge or smaller").put("movement_ft",80));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",3).put("options","Bite|Miasmic Globule"));
        o.optJSONArray("special_actions").put(action("Shape-Shift","bonus").put("size_choice","Medium|Large|Huge"));
    }catch(Exception ignored){}return o;}

    private static JSONObject vibrance(){JSONObject o=base("Incarnation of Vibrance",14,230,"20d12 + 100","fey",10,move(40),abilities(20,17,20,10,16,21),senses(13,"Darkvision 60 ft."));try{
        o.put("saving_throws",new JSONObject().put("con","+9").put("cha","+9")).put("languages","Sylvan; telepathy 120 ft.").put("resistances","radiant")
         .put("condition_immunities","blinded, charmed, deafened, frightened");
        o.optJSONArray("traits").put(new JSONObject().put("name","Magic Resistance").put("mechanic","save_advantage_vs_magic"));
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Vine","action").put("to_hit",9).put("damage","3d8 + 5").put("damage_type","bludgeoning").put("reach_ft",20).put("condition_on_fail","grappled").put("effect_on_hit","Grappled escape DC 15 if target no larger than incarnation; max two vines"))
         .put(action("Radiant Blast","action").put("save_ability","con").put("save_dc",17).put("damage","2d8 + 5").put("damage_on_save","half of 2d8 + 5").put("damage_type","radiant").put("emanation_ft",20))
         .put(action("Spellcasting","action").put("spells",new JSONArray().put("Daylight").put("Druidcraft").put("Plant Growth").put("Blindness/Deafness 1/day")).put("spellcasting_ability","CHA").put("components_required",false));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("sequence","2 Vine + Radiant Blast; may replace one Vine with Blindness/Deafness if available"));
        o.optJSONArray("special_actions").put(action("Shape-Shift","bonus").put("size_choice","Medium|Large|Huge"))
         .put(action("Teleport","bonus").put("teleport_ft",60).put("blocked_while_grappling",true));
    }catch(Exception ignored){}return o;}

    private static JSONObject lorwynGiant(){JSONObject o=base("Lorwyn Giant",13,115,"11d12 + 44","giant",6,move(40),abilities(21,13,18,9,16,8),senses(13,""));try{
        o.put("saving_throws",new JSONObject().put("con","+7").put("wis","+6")).put("languages","Common, Giant");
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Stone Sword","action").put("to_hit",8).put("damage","3d10 + 5").put("damage_type","slashing").put("reach_ft",15))
         .put(action("Boulder","action").put("to_hit",8).put("damage","2d8 + 5").put("damage_type","bludgeoning").put("reach_ft",240).put("range_normal_ft",60).put("range_long_ft",240).put("condition_on_hit","Prone if Large or smaller"));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",2).put("options","Stone Sword|Boulder"));
        o.optJSONArray("special_actions").put(action("Name Sleep","bonus").put("recharge","5-6").put("temp_hp",20).put("condition","Unconscious until damaged or end next turn").put("advantage_duration","1 minute on ability checks and saves"));
    }catch(Exception ignored){}return o;}

    private static JSONObject doublespeaker(){JSONObject o=base("Merrow Doublespeaker",14,67,"9d8 + 27","monstrosity",4,moveSwim(20,40),abilities(18,17,16,12,13,16),senses(11,"Darkvision 120 ft."));try{
        o.put("saving_throws",new JSONObject().put("dex","+5").put("cha","+5")).put("skills",new JSONObject().put("deception","+5").put("stealth","+5")).put("languages","Common, Primordial (Aquan)");
        o.optJSONArray("traits").put(new JSONObject().put("name","Amphibious").put("mechanic","breathe_air_and_water"))
         .put(new JSONObject().put("name","Mimicry").put("mechanic","lfl_mimicry").put("insight_dc",15));
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Spined Tail","action").put("to_hit",6).put("damage","3d6 + 4").put("damage_type","piercing").put("reach_ft",60).put("range_normal_ft",20).put("range_long_ft",60))
         .put(action("Spellcasting","action").put("spells",new JSONArray().put("Command at will").put("Mage Hand at will").put("Minor Illusion at will")).put("save_dc",13).put("spellcasting_ability","CHA").put("material_components_required",false));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",3).put("action","Spined Tail").put("may_replace_one_with","Command"));
    }catch(Exception ignored){}return o;}

    private static JSONObject haranguer(){JSONObject o=base("Merrow Haranguer",14,90,"12d8 + 36","monstrosity",5,moveSwim(5,40),abilities(18,18,16,14,15,19),senses(12,""));try{
        o.put("saving_throws",new JSONObject().put("dex","+7").put("cha","+7")).put("skills",new JSONObject().put("performance","+7").put("persuasion","+7")).put("languages","Common, Primordial (Aquan)");
        o.optJSONArray("traits").put(new JSONObject().put("name","Amphibious").put("mechanic","breathe_air_and_water"));
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Coral Scepter","action").put("to_hit",7).put("damage","2d10 + 4").put("damage_type","bludgeoning").put("reach_ft",5))
         .put(action("Poisoned Longbow","action").put("to_hit",7).put("damage","1d8 + 4 + 2d6").put("damage_type","piercing + poison").put("reach_ft",600).put("range_normal_ft",150).put("range_long_ft",600))
         .put(action("Invective","action").put("save_ability","wis").put("save_dc",15).put("damage","4d6").put("damage_on_save","half of 4d6").put("damage_type","psychic").put("max_targets",3).put("range_ft",60).put("effect_on_fail","Disadvantage on next attack roll before end of merrow next turn"));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",3).put("options","Coral Scepter|Poisoned Longbow"));
    }catch(Exception ignored){}return o;}

    private static JSONObject ransacker(){JSONObject o=base("Noggle Ransacker",12,13,"3d6 + 3","fey",0.25,move(30),abilities(14,15,13,8,10,12),senses(10,"Darkvision 60 ft."));try{
        o.put("skills",new JSONObject().put("stealth","+4")).put("languages","Common, Sylvan");
        o.optJSONArray("traits").put(new JSONObject().put("name","Siege Monster").put("mechanic","lfl_siege_monster").put("damage_multiplier_vs_objects_structures",2));
        o.optJSONArray("actions").put(action("Sabotaging Swipe","action").put("to_hit",4).put("damage","1d8 + 2").put("damage_type","slashing").put("reach_ft",5).put("save_ability","dex").put("save_dc",12).put("effect_on_fail","armor AC bonus -1 until target uses an action to remove penalty"));
        o.optJSONArray("special_actions").put(action("Nimble Escape","bonus").put("options","Disengage|Hide"));
    }catch(Exception ignored){}return o;}

    private static JSONObject wildMage(){JSONObject o=base("Noggle Wild Mage",13,22,"4d6 + 8","fey",1,move(30),abilities(11,16,14,10,12,17),senses(11,"Darkvision 60 ft."));try{
        o.put("saving_throws",new JSONObject().put("con","+4").put("cha","+5")).put("languages","Common, Sylvan");
        o.optJSONArray("traits").put(new JSONObject().put("name","Bloodied Wild Magic").put("mechanic","lfl_bloodied_wild_magic").put("bloodied_fly_ft",30).put("may_change_damage_to_force",true));
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Noggling Stick","action").put("to_hit",5).put("damage","1d4 + 3 + 1d4").put("damage_type","bludgeoning + psychic").put("reach_ft",5))
         .put(action("Befuddling Ray","action").put("to_hit",5).put("damage","1d6 + 3").put("damage_type","psychic").put("reach_ft",60).put("range_normal_ft",60).put("effect_on_hit","subtract explicit 1d4 from next saving throw before end of noggle next turn").put("rider_die","1d4 explicit"));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",2).put("options","Noggling Stick|Befuddling Ray"));
    }catch(Exception ignored){}return o;}

    private static JSONObject shadowmoorGiant(){JSONObject o=base("Shadowmoor Giant",14,172,"15d12 + 75","giant",8,move(40),abilities(22,14,20,7,19,8),senses(14,"Darkvision 120 ft."));try{
        o.put("saving_throws",new JSONObject().put("con","+8").put("wis","+7")).put("languages","Giant");
        o.optJSONArray("actions").put(action("Multiattack","action"))
         .put(action("Tree Club","action").put("to_hit",9).put("damage","2d10 + 6").put("damage_type","bludgeoning").put("reach_ft",15))
         .put(action("Boulder","action").put("to_hit",9).put("damage","2d6 + 6").put("damage_type","bludgeoning").put("reach_ft",240).put("range_normal_ft",60).put("range_long_ft",240).put("condition_on_hit","Prone if Large or smaller"))
         .put(action("Wild Magic Cataclysm","action").put("recharge","5-6").put("save_ability","dex").put("save_dc",15).put("damage","8d6").put("damage_on_save","half of 8d6").put("damage_type","explicit d4: 1 acid; 2 fire; 3 lightning; 4 thunder").put("damage_type_die","1d4 explicit").put("sphere_radius_ft",20).put("range_ft",120));
        o.put("multiattack",new JSONObject().put("manual_sequence_required",true).put("count",3).put("options","Tree Club|Boulder"));
    }catch(Exception ignored){}return o;}

    private static JSONObject[] records(){return new JSONObject[]{transience(),vibrance(),lorwynGiant(),doublespeaker(),haranguer(),ransacker(),wildMage(),shadowmoorGiant()};}

    public static synchronized JSONObject install(){int declared=0,installed=0,blocked=0;JSONArray failures=new JSONArray();try{
        for(JSONObject r:records()){String id=r.optString("id");try{JSONObject d=AuthorizedBestiaryScopeEngine.declare(new JSONObject().put("id",id).put("source_book",BOOK).put("source_locator",r.optJSONObject("mechanical_basis").optString("source_locator")));if(d.optBoolean("accepted"))declared++;CompiledStatblockEngine.registerAuthorized(r);installed++;}catch(Exception e){blocked++;failures.put(id);}}
        return new JSONObject().put("source",SOURCE_CODE).put("scope_declared",declared).put("statblocks_installed",installed).put("blocked",blocked).put("failures",failures).put("auto_rolls",false).put("raw_prose_embedded",false).put("runtime_tier","tactical_runtime");
    }catch(Exception e){return new JSONObject();}}

    public static JSONObject coverage(){JSONObject o=new JSONObject();try{int ready=0;JSONArray missing=new JSONArray();for(String id:IDS){if(CompiledStatblockEngine.has(id))ready++;else missing.put(id);}return o.put("source",SOURCE_CODE).put("expected",IDS.length).put("tactical_runtime",ready).put("pending",IDS.length-ready).put("complete_tactical",ready==IDS.length).put("full_statblock_claim",false).put("missing",missing).put("trait_runtime_complete",ready==IDS.length);}catch(Exception e){return o;}}

    /** Unique LFL trait semantics not already covered by the generic statblock runtime. */
    public static JSONObject resolveTrait(String statblock,String trait,JSONObject in){JSONObject o=new JSONObject();try{
        String t=trait==null?"":trait.trim();JSONObject p=CompiledStatblockEngine.runtimeProfile(statblock);if(!p.optBoolean("found",false))return o.put("resolved",false).put("reason","statblock_not_found").put("auto_rolls",false);
        if("Ephemeral Movement".equalsIgnoreCase(t)){boolean ends=in!=null&&in.optBoolean("ends_turn_inside_object",false);o.put("resolved",true).put("difficult_terrain_through_creatures_objects",true).put("opportunity_attacks_provoked",false).put("force_damage_required",ends).put("force_damage_expression",ends?"1d10":"").put("manual_damage_roll",ends);}
        else if("Mimicry".equalsIgnoreCase(t)){if(in==null||!in.has("insight_total"))return o.put("resolved",false).put("status","pending_input").put("reason","insight_total_required").put("auto_rolls",false);int total=in.optInt("insight_total");o.put("resolved",true).put("recognized_as_imitation",total>=15).put("insight_dc",15).put("insight_total",total);}
        else if("Siege Monster".equalsIgnoreCase(t))o.put("resolved",true).put("damage_multiplier_vs_objects_structures",2);
        else if("Bloodied Wild Magic".equalsIgnoreCase(t)){boolean bloodied=in!=null&&in.optBoolean("bloodied",false);o.put("resolved",true).put("applies",bloodied).put("fly_speed_ft",bloodied?30:0).put("may_change_dealt_damage_to_force",bloodied);}
        else return StatblockCombatRuntimeEngine.evaluateTrait(statblock,trait,in==null?new JSONObject():in);
        return o.put("statblock_id",statblock).put("trait",trait).put("source",SOURCE_CODE).put("auto_rolls",false).put("auto_state_mutation",false);
    }catch(Exception e){try{return o.put("resolved",false).put("reason","runtime_error").put("auto_rolls",false);}catch(Exception ignored){return o;}}}
}
