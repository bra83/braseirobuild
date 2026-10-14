package com.braseiro.dnd5e;
import java.util.*;
import org.json.*;
/** Source-pinned XMM hit riders. Pure proposals, never mutates combat state. */
public final class Xmm2025AttackConditionEngine {
 public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
 private static final String[] SIZES={"Tiny","Small","Medium","Large","Huge","Gargantuan"};
 private static final String[] RAW={
  "{\"id\":\"Aberrant Cultist\",\"name\":\"Tentacle Lash\",\"slot\":1,\"ptr\":\"/monster/2/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":true}",
  "{\"id\":\"Aboleth\",\"name\":\"Tentacle\",\"slot\":1,\"ptr\":\"/monster/3/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":false}",
  "{\"id\":\"Animated Rug of Smothering\",\"name\":\"Smother\",\"slot\":0,\"ptr\":\"/monster/31/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"choice_required\":true,\"other\":true}",
  "{\"id\":\"Ankheg\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/32/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":13,\"other\":true}",
  "{\"id\":\"Ankylosaurus\",\"name\":\"Tail\",\"slot\":1,\"ptr\":\"/monster/33/action/1/entries/0\",\"size\":\"Huge\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Arch-hag\",\"name\":\"Spectral Claw\",\"slot\":1,\"ptr\":\"/monster/36/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Barbed Devil\",\"name\":\"Claws\",\"slot\":1,\"ptr\":\"/monster/54/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":13,\"other\":false}",
  "{\"id\":\"Barlgura\",\"name\":\"Thrash\",\"slot\":2,\"ptr\":\"/monster/55/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Brown Bear\",\"name\":\"Claw\",\"slot\":2,\"ptr\":\"/monster/78/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Bugbear Warrior\",\"name\":\"Grab\",\"slot\":0,\"ptr\":\"/monster/80/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":false}",
  "{\"id\":\"Chain Devil\",\"name\":\"Chain\",\"slot\":1,\"ptr\":\"/monster/91/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":true}",
  "{\"id\":\"Chimera\",\"name\":\"Ram\",\"slot\":3,\"ptr\":\"/monster/93/action/3/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Chuul\",\"name\":\"Pincer\",\"slot\":1,\"ptr\":\"/monster/94/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":false}",
  "{\"id\":\"Colossus\",\"name\":\"Radiant Ray\",\"slot\":2,\"ptr\":\"/monster/100/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Crocodile\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/107/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":true}",
  "{\"id\":\"Cyclops Sentry\",\"name\":\"Stone Club\",\"slot\":1,\"ptr\":\"/monster/112/action/1/entries/0\",\"size\":\"Huge\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Dao\",\"name\":\"Earthen Maul\",\"slot\":1,\"ptr\":\"/monster/113/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Dire Wolf\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/124/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Djinni\",\"name\":\"Storm Bolt\",\"slot\":2,\"ptr\":\"/monster/127/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Dragon Turtle\",\"name\":\"Tail\",\"slot\":2,\"ptr\":\"/monster/131/action/2/entries/0\",\"size\":\"Huge\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Earth Elemental\",\"name\":\"Rock Launch\",\"slot\":2,\"ptr\":\"/monster/138/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Ettin\",\"name\":\"Battleaxe\",\"slot\":1,\"ptr\":\"/monster/148/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Giant Axe Beak\",\"name\":\"Talons\",\"slot\":2,\"ptr\":\"/monster/171/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Giant Crab\",\"name\":\"Claw\",\"slot\":0,\"ptr\":\"/monster/177/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":11,\"other\":false}",
  "{\"id\":\"Giant Crocodile\",\"name\":\"Bite\",\"slot\":1,\"ptr\":\"/monster/178/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":15,\"other\":true}",
  "{\"id\":\"Giant Crocodile\",\"name\":\"Tail\",\"slot\":2,\"ptr\":\"/monster/178/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Giant Frog\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/182/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":11,\"other\":false}",
  "{\"id\":\"Giant Octopus\",\"name\":\"Tentacles\",\"slot\":0,\"ptr\":\"/monster/186/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"other\":true}",
  "{\"id\":\"Giant Scorpion\",\"name\":\"Claw\",\"slot\":1,\"ptr\":\"/monster/189/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":13,\"other\":false}",
  "{\"id\":\"Giant Squid\",\"name\":\"Tentacle\",\"slot\":2,\"ptr\":\"/monster/193/action/2/entries/0\",\"size\":\"Huge\",\"condition\":\"grappled\",\"dc\":16,\"other\":true}",
  "{\"id\":\"Giant Toad\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/194/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":false}",
  "{\"id\":\"Gibbering Mouther\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/200/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":true}",
  "{\"id\":\"Glabrezu\",\"name\":\"Pincer\",\"slot\":1,\"ptr\":\"/monster/207/action/1/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":15,\"other\":false}",
  "{\"id\":\"Graveyard Revenant\",\"name\":\"Suffocate\",\"slot\":1,\"ptr\":\"/monster/221/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":15,\"other\":true}",
  "{\"id\":\"Grell\",\"name\":\"Paralyzing Tentacles\",\"slot\":2,\"ptr\":\"/monster/227/action/2/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":true,\"save_ability\":\"con\",\"save_dc\":11}",
  "{\"id\":\"Grick\",\"name\":\"Tentacles\",\"slot\":2,\"ptr\":\"/monster/228/action/2/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":false}",
  "{\"id\":\"Grick Ancient\",\"name\":\"Slam\",\"slot\":2,\"ptr\":\"/monster/229/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Grick Ancient\",\"name\":\"Tentacles\",\"slot\":3,\"ptr\":\"/monster/229/action/3/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":false}",
  "{\"id\":\"Griffon\",\"name\":\"Rend\",\"slot\":1,\"ptr\":\"/monster/230/action/1/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":14,\"other\":false}",
  "{\"id\":\"Hill Giant\",\"name\":\"Tree Club\",\"slot\":1,\"ptr\":\"/monster/243/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Kuo-toa Whip\",\"name\":\"Pincer Staff\",\"slot\":0,\"ptr\":\"/monster/273/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":12,\"other\":true}",
  "{\"id\":\"Lizardfolk Sovereign\",\"name\":\"Earthen Maul\",\"slot\":2,\"ptr\":\"/monster/282/action/2/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Mastiff\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/293/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Merfolk Wavebender\",\"name\":\"Aquatic Burst\",\"slot\":1,\"ptr\":\"/monster/296/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Mezzoloth\",\"name\":\"Claws\",\"slot\":1,\"ptr\":\"/monster/298/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":true}",
  "{\"id\":\"Mimic\",\"name\":\"Pseudopod\",\"slot\":1,\"ptr\":\"/monster/299/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":13,\"other\":true}",
  "{\"id\":\"Mind Flayer\",\"name\":\"Tentacles\",\"slot\":0,\"ptr\":\"/monster/300/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":14,\"other\":true}",
  "{\"id\":\"Otyugh\",\"name\":\"Tentacle\",\"slot\":2,\"ptr\":\"/monster/331/action/2/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"other\":false}",
  "{\"id\":\"Purple Worm\",\"name\":\"Bite\",\"slot\":1,\"ptr\":\"/monster/360/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":19,\"other\":true}",
  "{\"id\":\"Remorhaz\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/371/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":17,\"other\":true}",
  "{\"id\":\"Roc\",\"name\":\"Talons\",\"slot\":2,\"ptr\":\"/monster/375/action/2/entries/0\",\"size\":\"Huge\",\"condition\":\"grappled\",\"dc\":19,\"other\":true}",
  "{\"id\":\"Stone Giant\",\"name\":\"Boulder\",\"slot\":2,\"ptr\":\"/monster/418/action/2/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Swarm of Crawling Claws\",\"name\":\"Swarm of Grasping Hands\",\"slot\":0,\"ptr\":\"/monster/423/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Swarm of Stirges\",\"name\":\"Swarm of Proboscises\",\"slot\":0,\"ptr\":\"/monster/431/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"other\":true}",
  "{\"id\":\"Tarrasque\",\"name\":\"Tail\",\"slot\":3,\"ptr\":\"/monster/433/action/3/entries/0\",\"size\":\"Huge\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Tiger\",\"name\":\"Rend\",\"slot\":0,\"ptr\":\"/monster/436/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Tyrannosaurus Rex\",\"name\":\"Bite\",\"slot\":1,\"ptr\":\"/monster/446/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":17,\"other\":true}",
  "{\"id\":\"Tyrannosaurus Rex\",\"name\":\"Tail\",\"slot\":2,\"ptr\":\"/monster/446/action/2/entries/0\",\"size\":\"Huge\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Vampire\",\"name\":\"Grave Strike (Vampire Form Only)\",\"slot\":1,\"ptr\":\"/monster/450/action/1/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":14,\"other\":false}",
  "{\"id\":\"Vampire Spawn\",\"name\":\"Claw\",\"slot\":1,\"ptr\":\"/monster/453/action/1/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"other\":false}",
  "{\"id\":\"Vine Blight\",\"name\":\"Constricting Vine\",\"slot\":0,\"ptr\":\"/monster/456/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"grappled\",\"dc\":12,\"other\":true}",
  "{\"id\":\"Water Elemental\",\"name\":\"Slam\",\"slot\":1,\"ptr\":\"/monster/466/action/1/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Water Weird\",\"name\":\"Surge\",\"slot\":0,\"ptr\":\"/monster/467/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"grappled\",\"dc\":13,\"other\":true}",
  "{\"id\":\"Winter Wolf\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/478/action/0/entries/0\",\"size\":\"Large\",\"condition\":\"prone\",\"dc\":0,\"other\":false}",
  "{\"id\":\"Wolf\",\"name\":\"Bite\",\"slot\":0,\"ptr\":\"/monster/479/action/0/entries/0\",\"size\":\"Medium\",\"condition\":\"prone\",\"dc\":0,\"other\":false}"
 };
 private static final Map<String,JSONObject> INDEX=new LinkedHashMap<>();
 static {try{for(String s:RAW){JSONObject r=new JSONObject(s);
  if(INDEX.put(r.getString("id")+"::"+r.getString("name"),r)!=null)throw new IllegalStateException("duplicate_rider");
 }if(INDEX.size()!=65)throw new IllegalStateException("rider_count");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 private Xmm2025AttackConditionEngine(){}
 private static JSONObject blocked(String why){try{return new JSONObject().put("resolved",false).put("reason",why).put("auto_rolls",false).put("auto_mutation_allowed",false).put("condition_applied",false);}catch(Exception e){return new JSONObject();}}
 public static JSONArray catalog()throws Exception{JSONArray a=new JSONArray();for(JSONObject r:INDEX.values())a.put(new JSONObject(r.toString()));return a;}
 public static JSONObject profile(String monster,String action)throws Exception{JSONObject r=INDEX.get(monster+"::"+action);return r==null?new JSONObject():new JSONObject(r.toString());}
 public static JSONObject coverage()throws Exception{return new JSONObject().put("source","XMM").put("verified",65).put("prone",29).put("grappled",36).put("other_riders_manual",21).put("source_blob_sha1",SOURCE_BLOB).put("full_automation_certified",false).put("auto_rolls",false);}
 private static int size(String s){for(int i=0;i<SIZES.length;i++)if(SIZES[i].equals(s))return i;return -1;}
 private static boolean flag(JSONObject in,String key)throws Exception{if(!(in.opt(key) instanceof Boolean))throw new IllegalArgumentException("explicit_boolean_"+key+"_required");return in.getBoolean(key);}
 public static JSONObject resolve(JSONObject in){try{
  if(in==null)return blocked("request_required");
  String monster=in.optString("monster"),action=in.optString("action");
  JSONObject r=profile(monster,action);if(r.length()==0)return blocked("unverified_attack_rider");
  boolean hit=flag(in,"attack_hit"),creature=flag(in,"target_is_creature");
  Object rawSize=in.opt("target_size");if(!(rawSize instanceof String)||size((String)rawSize)<0)return blocked("explicit_size_required");
  if(!CompiledStatblockEngine.has(monster)&&!Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"))return blocked("compiled_source_not_installed");
  JSONObject compiled=CompiledStatblockEngine.get(monster);JSONArray acts=compiled.optJSONArray("actions");
  int slot=r.getInt("slot");if(acts==null||slot>=acts.length()||!action.equals(acts.getJSONObject(slot).optString("name"))||!acts.getJSONObject(slot).optBoolean("source_bound")||acts.getJSONObject(slot).optJSONArray("attack_modes")==null)return blocked("compiled_action_mismatch");
  boolean eligible=hit&&creature&&size((String)rawSize)<=size(r.getString("size"));
  JSONArray proposals=new JSONArray();if(eligible){JSONObject c=new JSONObject().put("condition",r.getString("condition")).put("condition_applied",false);
   if("grappled".equals(r.getString("condition")))c.put("escape_dc",r.getInt("dc"));proposals.put(c);}
  boolean choiceRequired=r.optBoolean("choice_required",false);
  String requestedChoice=in.optString("chosen_effect_option","");
  boolean choiceSatisfied=!choiceRequired||"grapple".equals(requestedChoice);
  if(choiceRequired&&!choiceSatisfied)proposals=new JSONArray();
  return new JSONObject().put("resolved",true).put("status",choiceRequired&&!choiceSatisfied?"pending_effect_choice":"pending_manual_application").put("monster",monster).put("action",action).put("source_pointer",r.getString("ptr")).put("source_blob_sha1",SOURCE_BLOB).put("trigger_satisfied",eligible).put("choice_required",choiceRequired).put("chosen_effect_option",choiceRequired&&choiceSatisfied?requestedChoice:"").put("proposed_conditions",proposals).put("additional_source_effects_manual",r.getBoolean("other")).put("full_action_effects_certified",false).put("auto_rolls",false).put("auto_mutation_allowed",false).put("condition_applied",false).put("damage_applied",false).put("hp_applied",false).put("turn_advanced",false);
 }catch(Exception e){return blocked(e.getMessage()==null?"runtime_error":e.getMessage());}}
}
