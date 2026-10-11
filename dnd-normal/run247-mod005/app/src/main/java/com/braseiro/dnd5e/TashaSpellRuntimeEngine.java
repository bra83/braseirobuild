package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;import java.util.*;
/** Executable, no-auto-roll runtime for the 21 spells published in Tasha's Cauldron of Everything. */
public final class TashaSpellRuntimeEngine{
 private TashaSpellRuntimeEngine(){}
 private static final Set<String> NAMES=new LinkedHashSet<String>(Arrays.asList(
  "Blade of Disaster","Booming Blade","Dream of the Blue Veil","Green-Flame Blade","Intellect Fortress","Lightning Lure","Mind Sliver","Spirit Shroud","Summon Aberration","Summon Beast","Summon Celestial","Summon Construct","Summon Elemental","Summon Fey","Summon Fiend","Summon Shadowspawn","Summon Undead","Sword Burst","Tasha's Caustic Brew","Tasha's Mind Whip","Tasha's Otherworldly Guise"));
 public static JSONObject install(){if(CompiledContentMechanicsEngine.get("spell","Blade of Disaster").length()==0)TashaCompiledSourceRulesEngine.install();JSONObject o=new JSONObject();int ok=0;JSONArray bad=new JSONArray();try{for(String n:NAMES){JSONObject old=CompiledContentMechanicsEngine.get("spell",n);JSONObject basis=old.optJSONObject("mechanical_basis");JSONObject mech=old.optJSONObject("mechanics");if(basis==null||mech==null){bad.put(n);continue;}JSONObject b=new JSONObject(basis.toString()).put("revision",2);JSONObject m=new JSONObject(mech.toString()).put("effect_pipeline_complete",true).put("execution_status","compiled_effect_pipeline").put("runtime_resolver","TashaSpellRuntimeEngine.resolve").put("revision",2).put("auto_rolls",false).put("auto_mutation_allowed",false);JSONObject r=CompiledContentMechanicsEngine.register("spell",n,m,b);if(r.optBoolean("registered"))ok++;else bad.put(n+":"+r.optString("authority"));}return o.put("installed",ok).put("expected",NAMES.size()).put("failed",bad).put("complete",ok==NAMES.size());}catch(Exception e){try{return o.put("installed",ok).put("expected",NAMES.size()).put("failed",bad).put("complete",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return o;}}}
 public static JSONObject resolve(String name,JSONObject in){JSONObject o=new JSONObject();try{if(!NAMES.contains(name))return o.put("resolved",false).put("reason","unknown_tasha_spell");if(in==null)in=new JSONObject();String slotError=validateSlotLevel(name,in);if(!slotError.isEmpty())return o.put("resolved",false).put("reason",slotError).put("spell",name);o.put("resolved",true).put("spell",name).put("auto_rolls",false).put("auto_mutation_allowed",false);switch(name){
  case "Blade of Disaster":return blade(o,in);
  case "Booming Blade":return booming(o,in);
  case "Dream of the Blue Veil":return blueVeil(o,in);
  case "Green-Flame Blade":return greenFlame(o,in);
  case "Intellect Fortress":return intellect(o,in);
  case "Lightning Lure":return lightning(o,in);
  case "Mind Sliver":return mindSliver(o,in);
  case "Spirit Shroud":return spiritShroud(o,in);
  case "Sword Burst":return swordBurst(o,in);
  case "Tasha's Caustic Brew":return caustic(o,in);
  case "Tasha's Mind Whip":return mindWhip(o,in);
  case "Tasha's Otherworldly Guise":return guise(o,in);
  default:return summon(o,name,in);
 }}catch(Exception e){try{return o.put("resolved",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return o;}}}
 /** Reject an explicitly selected slot outside the spell's legal range. Missing
  * slot_level retains the engine's existing default-to-base behavior. Cantrips
  * have no slot scaling in this engine and ignore this field. */
 private static String validateSlotLevel(String name,JSONObject in){
  int minimum=minimumSlot(name);if(minimum==0||!in.has("slot_level"))return "";
  Object raw=in.opt("slot_level");if(!(raw instanceof Number))return "invalid_spell_slot_level";
  double value=((Number)raw).doubleValue();
  if(Double.isNaN(value)||Double.isInfinite(value)||value!=Math.floor(value)||value<minimum||value>9)
   return "invalid_spell_slot_level";
  return "";
 }
 private static int minimumSlot(String name){
  if(name.equals("Blade of Disaster"))return 9;
  if(name.equals("Dream of the Blue Veil"))return 7;
  if(name.equals("Tasha's Otherworldly Guise"))return 6;
  if(name.equals("Summon Fiend"))return 6;
  if(name.equals("Summon Celestial"))return 5;
  if(name.equals("Summon Aberration")||name.equals("Summon Construct")||name.equals("Summon Elemental"))return 4;
  if(name.equals("Summon Fey")||name.equals("Summon Shadowspawn")||name.equals("Summon Undead")||name.equals("Spirit Shroud")||name.equals("Intellect Fortress"))return 3;
  if(name.equals("Tasha's Mind Whip"))return 2;
  if(name.equals("Summon Beast"))return 2;
  if(name.equals("Tasha's Caustic Brew"))return 1;
  return 0;
 }
 private static JSONObject blade(JSONObject o,JSONObject in)throws Exception{int roll=in.optInt("attack_d20",0);if(roll<1)return pending(o,"attack_d20 (manual d20 result)");int mod=in.optInt("spell_attack_modifier",0),ac=in.optInt("target_ac",Integer.MIN_VALUE);if(ac==Integer.MIN_VALUE)return pending(o,"target_ac");boolean crit=roll>=18;boolean hit=crit||(roll+mod>=ac);o.put("attack_total",roll+mod).put("critical",crit).put("hit",hit).put("attacks_per_cast_or_bonus_action",2).put("damage_on_hit","4d12 force").put("damage_on_critical","12d12 force").put("concentration",true).put("duration_rounds",10).put("move_blade_ft",30);return o;}
 private static JSONObject booming(JSONObject o,JSONObject in)throws Exception{int lvl=Math.max(1,in.optInt("caster_level",1));boolean hit=in.has("weapon_attack_hit")&&in.optBoolean("weapon_attack_hit");if(!in.has("weapon_attack_hit"))return pending(o,"weapon_attack_hit (resolve the required melee weapon attack)");int d=cantripDice(lvl);o.put("weapon_attack_hit",hit).put("initial_thunder_dice",hit&&lvl>=5?(d-1)+"d8":"0d8").put("movement_trigger_thunder_dice",hit?d+"d8":"0d8").put("trigger","target willingly moves 5 ft or more before start of caster next turn").put("duration_rounds",hit?1:0);return o;}
 private static JSONObject greenFlame(JSONObject o,JSONObject in)throws Exception{int lvl=Math.max(1,in.optInt("caster_level",1)),mod=in.optInt("spellcasting_modifier",0);if(!in.has("weapon_attack_hit"))return pending(o,"weapon_attack_hit (resolve the required melee weapon attack)");boolean hit=in.optBoolean("weapon_attack_hit");int dice=lvl>=5?cantripDice(lvl)-1:0;int secondaryModifier=hit?Math.max(0,mod):0;o.put("weapon_attack_hit",hit).put("primary_extra_fire_dice",hit?dice+"d8":"0d8").put("secondary_fire_dice",(hit?dice:0)+"d8 + "+secondaryModifier).put("secondary_target_range_ft",5).put("secondary_target_must_differ",true);return o;}
 private static JSONObject blueVeil(JSONObject o,JSONObject in)throws Exception{JSONArray travelers=in.optJSONArray("travelers");if(travelers==null)return pending(o,"travelers[] (willing creatures within 10 ft for full casting)");if(!in.optBoolean("material_from_destination_world",false))return pending(o,"material_from_destination_world=true");o.put("casting_time_minutes",10).put("max_creatures",8).put("travelers_count",travelers.length()).put("destination_world",in.optString("destination_world")).put("arrives_near_destination",true);return o;}
 private static JSONObject intellect(JSONObject o,JSONObject in)throws Exception{int slot=Math.max(3,in.optInt("slot_level",3));int targets=1+(slot-3);o.put("concentration",true).put("duration_hours",1).put("targets",targets).put("resistance",new JSONArray().put("psychic")).put("advantage_saves",new JSONArray().put("Intelligence").put("Wisdom").put("Charisma"));return o;}
 private static JSONObject lightning(JSONObject o,JSONObject in)throws Exception{int lvl=Math.max(1,in.optInt("caster_level",1));if(!in.has("strength_save_success"))return pending(o,"strength_save_success (manual target save)");boolean save=in.optBoolean("strength_save_success");o.put("save","Strength").put("range_ft",15).put("on_failed_save_pull_ft",10).put("pulled",!save).put("damage_dice_if_ends_within_5ft",!save?cantripDice(lvl)+"d8 lightning":"0");return o;}
 private static JSONObject mindSliver(JSONObject o,JSONObject in)throws Exception{int lvl=Math.max(1,in.optInt("caster_level",1));if(!in.has("intelligence_save_success"))return pending(o,"intelligence_save_success (manual target save)");boolean save=in.optBoolean("intelligence_save_success");o.put("save","Intelligence").put("damage_dice",save?"0":cantripDice(lvl)+"d6 psychic").put("next_save_penalty",save?"none":"subtract 1d4 from next saving throw before end of caster next turn");return o;}
 private static JSONObject spiritShroud(JSONObject o,JSONObject in)throws Exception{int slot=Math.max(3,in.optInt("slot_level",3));String type=in.optString("damage_type","");if(!(type.equals("cold")||type.equals("necrotic")||type.equals("radiant")))return pending(o,"damage_type = cold|necrotic|radiant");int dice=1+(slot>=5?1:0)+(slot>=7?1:0)+(slot>=9?1:0);o.put("concentration",true).put("duration_minutes",1).put("extra_damage",dice+"d8 "+type+" per attack against target within 10 ft").put("healing_blocked_until_start_of_next_turn",true).put("enemy_speed_reduction_ft",10).put("speed_reduction_radius_ft",10);return o;}
 private static boolean strictSaves(JSONArray saves){
  if(saves==null||saves.length()>128)return false;
  for(int i=0;i<saves.length();i++)if(!(saves.opt(i) instanceof Boolean))return false;
  return true;
 }
 private static JSONObject swordBurst(JSONObject o,JSONObject in)throws Exception{int lvl=Math.max(1,in.optInt("caster_level",1));JSONArray saves=in.optJSONArray("dexterity_save_successes");if(saves==null)return pending(o,"dexterity_save_successes[] (one manual save result per creature within 5 ft)");if(!strictSaves(saves))return pending(o,"strict_boolean_saving_throws_required");JSONArray res=new JSONArray();for(int i=0;i<saves.length();i++)res.put(new JSONObject().put("target_index",i).put("damage",saves.optBoolean(i)?"0":cantripDice(lvl)+"d6 force"));return o.put("save","Dexterity").put("radius_ft",5).put("targets",res);}
 private static JSONObject caustic(JSONObject o,JSONObject in)throws Exception{int slot=Math.max(1,in.optInt("slot_level",1));JSONArray saves=in.optJSONArray("dexterity_save_successes");if(saves==null)return pending(o,"dexterity_save_successes[] (one manual save result per creature in 30-ft line)");if(!strictSaves(saves))return pending(o,"strict_boolean_saving_throws_required");JSONArray res=new JSONArray();for(int i=0;i<saves.length();i++)res.put(new JSONObject().put("target_index",i).put("coated",!saves.optBoolean(i)).put("acid_damage_each_turn",saves.optBoolean(i)?"0":(2*slot)+"d4").put("ends_with_action_to_scrape_or_wash",true));return o.put("concentration",true).put("duration_minutes",1).put("line_ft",30).put("targets",res);}
 private static JSONObject mindWhip(JSONObject o,JSONObject in)throws Exception{int slot=Math.max(2,in.optInt("slot_level",2));JSONArray saves=in.optJSONArray("intelligence_save_successes");if(saves==null)return pending(o,"intelligence_save_successes[] (manual saves; one target + one per slot above 2)");int max=1+(slot-2);if(!strictSaves(saves)||saves.length()<1||saves.length()>max)return pending(o,"invalid_mind_whip_target_saves");JSONArray res=new JSONArray();for(int i=0;i<saves.length();i++){boolean save=saves.optBoolean(i);res.put(new JSONObject().put("target_index",i).put("damage",save?"half of 3d6 psychic":"3d6 psychic").put("no_reactions",!save).put("next_turn_choice",save?"none":"move OR action OR bonus action; only one"));}return o.put("targets_max",max).put("targets",res);}
 private static JSONObject guise(JSONObject o,JSONObject in)throws Exception{String form=in.optString("form","").toLowerCase(Locale.ROOT);boolean upper=form.contains("upper")||form.contains("celestial")||form.contains("superior");boolean lower=form.contains("lower")||form.contains("fiend")||form.contains("inferior");if(!upper&&!lower)return pending(o,"form = upper_planes|lower_planes");JSONArray dmgImm=new JSONArray();JSONArray condImm=new JSONArray();if(upper){dmgImm.put("necrotic").put("radiant");condImm.put("charmed");}else{dmgImm.put("fire").put("poison");condImm.put("poisoned");}o.put("concentration",true).put("duration_minutes",1).put("damage_immunities",dmgImm).put("condition_immunities",condImm).put("flight_speed_ft",40).put("ac_bonus","+2").put("weapon_attacks_use_spellcasting_ability",true).put("extra_attack",true);return o;}
 private static JSONObject summon(JSONObject o,String name,JSONObject in)throws Exception{int base=baseSummonLevel(name),slot=Math.max(base,in.optInt("slot_level",base));String form=in.optString("form","");if(form.trim().isEmpty())return pending(o,"form (choose one statblock form from the spell)");JSONObject s=new JSONObject().put("spell_level",slot).put("form",form).put("proficiency_bonus",in.optInt("proficiency_bonus",0)).put("uses_spell_attack_modifier",true).put("obeys_verbal_commands",true).put("dodges_without_command",true).put("vanishes_at_0_hp_or_spell_end",true).put("concentration",true).put("duration_hours",1);
  if(name.equals("Summon Beast")){boolean air=form.toLowerCase(Locale.ROOT).contains("air");s.put("ac",11+slot).put("hp",(air?20:30)+5*(slot-2)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","1d8 + 4 + spell level piercing").put("air_flyby",air).put("land_water_pack_tactics",!air);}
  else if(name.equals("Summon Fey")){s.put("ac",12+slot).put("hp",30+10*(slot-3)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","1d6 + 3 + spell level piercing + 1d6 force");}
  else if(name.equals("Summon Shadowspawn")){s.put("ac",11+slot).put("hp",35+15*(slot-3)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","1d12 + 3 + spell level cold");}
  else if(name.equals("Summon Undead")){s.put("ac",11+slot).put("hp",30+10*(slot-3)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","form-dependent; uses spell attack modifier and spell-level scaling");}
  else if(name.equals("Summon Aberration")){s.put("ac",11+slot).put("hp",40+10*(slot-4)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","form-dependent; uses spell attack modifier and spell-level scaling");}
  else if(name.equals("Summon Construct")){s.put("ac",13+slot).put("hp",40+15*(slot-4)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","1d8 + 4 + spell level bludgeoning");}
  else if(name.equals("Summon Elemental")){s.put("ac",11+slot).put("hp",50+10*(slot-4)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","1d10 + 4 + spell level, type by form");}
  else if(name.equals("Summon Celestial")){s.put("ac",11+slot+(form.toLowerCase(Locale.ROOT).contains("defender")?2:0)).put("hp",40+10*(slot-5)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","form-dependent radiant attack; scales with spell level");}
  else if(name.equals("Summon Fiend")){s.put("ac",12+slot).put("hp",50+15*(slot-6)).put("multiattack",Math.max(1,slot/2)).put("attack_damage","form-dependent; scales with spell level");}
  return o.put("summon",s);}
 private static int baseSummonLevel(String n){if(n.equals("Summon Beast"))return 2;if(n.equals("Summon Fey")||n.equals("Summon Shadowspawn")||n.equals("Summon Undead"))return 3;if(n.equals("Summon Aberration")||n.equals("Summon Construct")||n.equals("Summon Elemental"))return 4;if(n.equals("Summon Celestial"))return 5;return 6;}
 private static int cantripDice(int lvl){return lvl>=17?4:lvl>=11?3:lvl>=5?2:1;}
 private static JSONObject pending(JSONObject o,String need)throws Exception{return o.put("resolved",false).put("status","requires_player_or_gm_resolution").put("required_input",need).put("auto_rolls",false);}
}
