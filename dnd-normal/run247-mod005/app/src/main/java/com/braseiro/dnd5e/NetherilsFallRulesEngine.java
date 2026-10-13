package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Deterministic NF runtime: anomalies, hazards and item rules. No hidden rolls. */
public final class NetherilsFallRulesEngine {
 private static JSONObject o(Object...kv){JSONObject x=new JSONObject();try{for(int i=0;i<kv.length;i+=2)x.put(String.valueOf(kv[i]),kv[i+1]);}catch(Exception ignored){}return x;}
 private static JSONArray a(String...v){JSONArray x=new JSONArray();for(String s:v)x.put(s);return x;}
 public JSONObject coverage(){return o("source","NF","environmental_effects",6,"hazards",5,"magic_items",8,"auto_rolls",false,"runtime_complete",true);}
 public JSONArray listEnvironmentalEffects(){return a("Communal Telepathy Zone","Dampened Magic Zone","Eerie Chanting Zone","Empowered Force Zone","Low Gravity Zone","Unstable Gravity Zone");}
 public JSONArray listHazards(){return a("Arcane Effluence","Glittermoss","Shadowbark","Wildstorm","Witchbriars");}
 public JSONArray listItems(){return a("Arcane Battery","Brooch of the Elements","Fork of Eddy Summoning","Hat of Vortexes","Magen Handbell","Mask of Changed Appearance","Mythallar Bracelet","Mythallar Cloak");}
 public JSONObject environmental(String name){String n=norm(name);if(n.equals("communal telepathy zone"))return o("found",true,"telepathy_min_int",8,"telepathy_range_ft",30,"typical_diameter_ft",300);if(n.equals("dampened magic zone"))return o("found",true,"spell_duration_multiplier",0.5,"spell_duration_minimum_base_seconds",60,"spell_attacks_disadvantage",true,"saves_vs_magic_advantage",true,"typical_diameter_ft",300);if(n.equals("eerie chanting zone"))return o("found",true,"wisdom_perception_disadvantage",true,"concentration_con_save_disadvantage",true,"typical_diameter_ft",300);if(n.equals("empowered force zone"))return o("found",true,"force_damage_die_minimum",3,"typical_diameter_ft",300);if(n.equals("low gravity zone"))return o("found",true,"jump_distance_multiplier",4,"fall_damage_multiplier",0.5,"round_fall_damage_down",true,"typical_diameter_ft",300);if(n.equals("unstable gravity zone"))return o("found",true,"detect_check","Intelligence (Investigation)","detect_dc",15,"save_dc",16,"effect","reverse_gravity","diameter_ft",60,"height_ft",60,"suppression_interval_roll_required","1d4 rounds","suppression_duration_roll_required","1d4 rounds","requires_manual_roll",true);return o("found",false);}
 /**
  * Resolve explicit environment inputs using Netheril's Fall, chapter 2.
  * The caller supplies rolled dice, base spell duration or damage; no hidden
  * roll and no automatic state mutation. Unknown input fails closed.
  */
 public JSONObject applyEnvironmentalEffect(String name,JSONObject input){
  JSONObject in=input==null?new JSONObject():input;
  JSONObject out=o("source","NF","source_section","Chapter 2: Environmental Effects",
                   "auto_rolls",false,"auto_mutation_allowed",false,
                   "requires_manual_application",true);
  String n=norm(name),action=norm(in.optString("operation"));
  try{
   if(n.equals("dampened magic zone")&&action.equals("spell_duration")){
    if(!in.has("base_duration_seconds"))return out.put("resolved",false).put("reason","base_duration_required");
    double seconds=in.optDouble("base_duration_seconds",-1d);
    if(!Double.isFinite(seconds)||seconds<0d)return out.put("resolved",false).put("reason","invalid_duration");
    return out.put("resolved",true).put("base_duration_seconds",seconds)
              .put("result_duration_seconds",seconds>=60d?seconds/2d:seconds)
              .put("halving_applies",seconds>=60d);
   }
   if(n.equals("empowered force zone")&&action.equals("force_damage_die")){
    int sides=in.optInt("die_sides",0),roll=in.optInt("rolled_damage_die",0);
    if(!in.has("die_sides")||!in.has("rolled_damage_die")||sides<2||
       roll<1||roll>sides)return out.put("resolved",false).put("reason","valid_explicit_die_required");
    if(!"force".equals(norm(in.optString("damage_type"))))
      return out.put("resolved",false).put("reason","force_damage_only");
    return out.put("resolved",true).put("rolled_damage_die",roll)
              .put("adjusted_damage_die",Math.max(3,roll));
   }
   if(n.equals("low gravity zone")&&action.equals("jump_distance")){
    if(!in.has("base_jump_distance_ft"))return out.put("resolved",false).put("reason","jump_distance_required");
    double distance=in.optDouble("base_jump_distance_ft",-1d);
    if(!Double.isFinite(distance)||distance<0d||distance>1000000d)
      return out.put("resolved",false).put("reason","invalid_jump_distance");
    return out.put("resolved",true).put("jump_distance_ft",distance*4d);
   }
   if(n.equals("low gravity zone")&&action.equals("fall_damage")){
    if(!in.has("rolled_fall_damage"))return out.put("resolved",false).put("reason","fall_damage_required");
    int damage=in.optInt("rolled_fall_damage",-1);
    if(damage<0)return out.put("resolved",false).put("reason","invalid_fall_damage");
    return out.put("resolved",true).put("fall_damage_after_gravity",damage/2)
              .put("round_down",true);
   }
   if(n.equals("communal telepathy zone")&&action.equals("telepathy")){
    if(!in.has("intelligence_score"))return out.put("resolved",false).put("reason","intelligence_required");
    int intelligence=in.optInt("intelligence_score",-1);
    if(intelligence<1||intelligence>30)return out.put("resolved",false).put("reason","invalid_intelligence");
    return out.put("resolved",true).put("telepathy_range_ft",intelligence>=8?30:0);
   }
   return out.put("resolved",false).put("reason","unsupported_environment_operation");
  }catch(Exception e){return o("resolved",false,"auto_rolls",false,
                              "auto_mutation_allowed",false,"reason","environment_resolution_error");}
 }
 public JSONObject hazard(String name,int level){String n=norm(name);int tier=level>=17?4:level>=11?3:level>=5?2:1;if(n.equals("arcane effluence"))return o("found",true,"contact_damage","1d6 Acid","ongoing_start_turn",true,"removal","action to scrape or Dispel Magic","manual_damage_roll",true);if(n.equals("glittermoss"))return o("found",true,"save","Constitution","dc",12,"damage","2d10 Radiant","failed_condition","Blinded","repeat_save","end of each turn","spore_radius_ft",10,"destroyed_by",a("Fire","Necrotic"),"manual_rolls",true);if(n.equals("shadowbark")){int dc=tier==1?12:tier==2?14:tier==3?16:18;String dmg=tier==1?"1d10":tier==2?"2d10":tier==3?"4d10":"10d10";return o("found",true,"save","Constitution","dc",dc,"damage",dmg+" Necrotic","success","half damage","failed_extra","cannot regain HP until start of next turn","destroyed_by","Radiant","manual_rolls",true);}if(n.equals("wildstorm"))return o("found",true,"size_ft",10,"speed_ft",50,"duration_roll_required","1d4 minutes","effect_roll_required","1d20","manual_rolls",true,"table",o("1-2","1d4 Fire + burning","3-6","DC12 Dex; 1d10 Force, half on save","7-10","DC12 Con; Poisoned 1 minute; if already Poisoned 2d4 Poison","11-13","DC12 Wis; 1d10 Psychic half + telepathy 10 ft 1 minute","14-17","vulnerability Fire/Force/Poison/Psychic 1 minute","18-19","+2 AC 1 minute","20","Invisible 1 minute"));if(n.equals("witchbriars"))return o("found",true,"ac",11,"hp",30,"trigger","spell slot cast within 10 ft","save","Strength","dc",14,"druids_auto_succeed",true,"failed","Grappled and Restrained","escape_dc",14,"start_turn_damage","2d10 Piercing","max_grappled",4,"destroyed_by","any Fire damage","manual_rolls",true);return o("found",false);}
 public JSONObject item(String name){String n=norm(name);if(n.equals("arcane battery"))return o("found",true,"rarity","uncommon","action","Magic","target","charged magic item","recharge_roll_required","1d4+1","consumed",true,"manual_rolls",true);if(n.equals("brooch of the elements"))return o("found",true,"rarity","rare","attunement",true,"charges",3,"recharge","all at dawn","reaction_range_ft",60,"cost",1,"damage_types",a("Acid","Cold","Fire","Lightning","Poison","Thunder"));if(n.equals("fork of eddy summoning"))return o("found",true,"rarity","rare","summon","Eldritch Eddy","duration","1 hour","recharge","next dawn","initiative","immediately after summoner");if(n.equals("hat of vortexes"))return o("found",true,"rarity","uncommon","attunement",true,"charges",3,"recharge","all at dawn","cost",1,"area","10-foot cube","duration","1 hour","effect","Difficult Terrain");if(n.equals("magen handbell"))return o("found",true,"rarity","rare","summon","Terran Magen","range_ft",30,"duration","1 hour","recharge_roll_required","1d6 days","bloodied_berserk_roll_required","1d6; berserk on 6","regain_control","DC15 Charisma (Persuasion) Influence action","manual_rolls",true);if(n.equals("mask of changed appearance"))return o("found",true,"rarity","common","charges",3,"cost",1,"duration","1 hour","recharge_roll_required","1d3 at dawn","last_charge_break_roll_required","1d20; destroyed on 1","manual_rolls",true);if(n.equals("mythallar bracelet"))return o("found",true,"rarity","common","beads",3,"action","Magic","effect","Advantage on Strength (Athletics)","duration","1 minute","bead_consumed",true);if(n.equals("mythallar cloak"))return o("found",true,"rarity","rare","attunement",true,"charges",10,"recharge_roll_required","1d10 at dawn","activation","Bonus Action; 1 charge","duration","1 minute","fly_speed_ft",30,"hover",true,"extra_damage","1d4 Radiant once each turn on hit","manual_rolls",true);return o("found",false);}
 private static String norm(String s){return s==null?"":s.trim().toLowerCase();}
}
