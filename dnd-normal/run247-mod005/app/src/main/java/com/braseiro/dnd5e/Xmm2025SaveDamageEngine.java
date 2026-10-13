package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Source-pinned conservative subset of XMM 2025 save-for-half actions.
 * Every indexed source entry is exactly one unqualified save/fail-damage/
 * success-half sentence, with no additional effects or triggers.
 * Source prose is not embedded; targeting, saves and dice come from the DM.
 */
public final class Xmm2025SaveDamageEngine {
 public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
 public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
 public static final int VERIFIED_RULES=59;
 private static final String[] RAW={
  "{\"id\":\"Abominable Yeti\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":4,\"source_pointer\":\"/monster/4/action/4/entries/0\",\"ability\":\"con\",\"dc\":18,\"average\":45,\"dice\":\"10d8\",\"damage_type\":\"cold\",\"min_damage\":10,\"max_damage\":80,\"recharge\":true}",
  "{\"id\":\"Adult Black Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/5/action/2/entries/0\",\"ability\":\"dex\",\"dc\":18,\"average\":54,\"dice\":\"12d8\",\"damage_type\":\"acid\",\"min_damage\":12,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Adult Blue Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/6/action/2/entries/0\",\"ability\":\"dex\",\"dc\":19,\"average\":60,\"dice\":\"11d10\",\"damage_type\":\"lightning\",\"min_damage\":11,\"max_damage\":110,\"recharge\":true}",
  "{\"id\":\"Adult Brass Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/7/action/2/entries/0\",\"ability\":\"dex\",\"dc\":18,\"average\":45,\"dice\":\"10d8\",\"damage_type\":\"fire\",\"min_damage\":10,\"max_damage\":80,\"recharge\":true}",
  "{\"id\":\"Adult Bronze Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/8/action/2/entries/0\",\"ability\":\"dex\",\"dc\":19,\"average\":55,\"dice\":\"10d10\",\"damage_type\":\"lightning\",\"min_damage\":10,\"max_damage\":100,\"recharge\":true}",
  "{\"id\":\"Adult Copper Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/9/action/2/entries/0\",\"ability\":\"dex\",\"dc\":18,\"average\":54,\"dice\":\"12d8\",\"damage_type\":\"acid\",\"min_damage\":12,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Adult Gold Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/10/action/2/entries/0\",\"ability\":\"dex\",\"dc\":21,\"average\":66,\"dice\":\"12d10\",\"damage_type\":\"fire\",\"min_damage\":12,\"max_damage\":120,\"recharge\":true}",
  "{\"id\":\"Adult Green Dragon\",\"name\":\"Poison Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/11/action/2/entries/0\",\"ability\":\"con\",\"dc\":18,\"average\":56,\"dice\":\"16d6\",\"damage_type\":\"poison\",\"min_damage\":16,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Adult Red Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/12/action/2/entries/0\",\"ability\":\"dex\",\"dc\":21,\"average\":59,\"dice\":\"17d6\",\"damage_type\":\"fire\",\"min_damage\":17,\"max_damage\":102,\"recharge\":true}",
  "{\"id\":\"Adult Silver Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/13/action/2/entries/0\",\"ability\":\"con\",\"dc\":20,\"average\":54,\"dice\":\"12d8\",\"damage_type\":\"cold\",\"min_damage\":12,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Adult White Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/14/action/2/entries/0\",\"ability\":\"con\",\"dc\":19,\"average\":54,\"dice\":\"12d8\",\"damage_type\":\"cold\",\"min_damage\":12,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Ancient Black Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/17/action/2/entries/0\",\"ability\":\"dex\",\"dc\":22,\"average\":67,\"dice\":\"15d8\",\"damage_type\":\"acid\",\"min_damage\":15,\"max_damage\":120,\"recharge\":true}",
  "{\"id\":\"Ancient Blue Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/18/action/2/entries/0\",\"ability\":\"dex\",\"dc\":23,\"average\":88,\"dice\":\"16d10\",\"damage_type\":\"lightning\",\"min_damage\":16,\"max_damage\":160,\"recharge\":true}",
  "{\"id\":\"Ancient Brass Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/19/action/2/entries/0\",\"ability\":\"dex\",\"dc\":21,\"average\":58,\"dice\":\"13d8\",\"damage_type\":\"fire\",\"min_damage\":13,\"max_damage\":104,\"recharge\":true}",
  "{\"id\":\"Ancient Bronze Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/20/action/2/entries/0\",\"ability\":\"dex\",\"dc\":23,\"average\":82,\"dice\":\"15d10\",\"damage_type\":\"lightning\",\"min_damage\":15,\"max_damage\":150,\"recharge\":true}",
  "{\"id\":\"Ancient Copper Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/21/action/2/entries/0\",\"ability\":\"dex\",\"dc\":22,\"average\":63,\"dice\":\"14d8\",\"damage_type\":\"acid\",\"min_damage\":14,\"max_damage\":112,\"recharge\":true}",
  "{\"id\":\"Ancient Gold Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/22/action/2/entries/0\",\"ability\":\"dex\",\"dc\":24,\"average\":71,\"dice\":\"13d10\",\"damage_type\":\"fire\",\"min_damage\":13,\"max_damage\":130,\"recharge\":true}",
  "{\"id\":\"Ancient Green Dragon\",\"name\":\"Poison Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/23/action/2/entries/0\",\"ability\":\"con\",\"dc\":22,\"average\":77,\"dice\":\"22d6\",\"damage_type\":\"poison\",\"min_damage\":22,\"max_damage\":132,\"recharge\":true}",
  "{\"id\":\"Ancient Red Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/24/action/2/entries/0\",\"ability\":\"dex\",\"dc\":24,\"average\":91,\"dice\":\"26d6\",\"damage_type\":\"fire\",\"min_damage\":26,\"max_damage\":156,\"recharge\":true}",
  "{\"id\":\"Ancient Silver Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/25/action/2/entries/0\",\"ability\":\"con\",\"dc\":24,\"average\":67,\"dice\":\"15d8\",\"damage_type\":\"cold\",\"min_damage\":15,\"max_damage\":120,\"recharge\":true}",
  "{\"id\":\"Ancient White Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/26/action/2/entries/0\",\"ability\":\"con\",\"dc\":22,\"average\":63,\"dice\":\"14d8\",\"damage_type\":\"cold\",\"min_damage\":14,\"max_damage\":112,\"recharge\":true}",
  "{\"id\":\"Ankheg\",\"name\":\"Acid Spray\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/32/action/1/entries/0\",\"ability\":\"dex\",\"dc\":12,\"average\":14,\"dice\":\"4d6\",\"damage_type\":\"acid\",\"min_damage\":4,\"max_damage\":24,\"recharge\":true}",
  "{\"id\":\"Behir\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":3,\"source_pointer\":\"/monster/59/action/3/entries/0\",\"ability\":\"dex\",\"dc\":16,\"average\":66,\"dice\":\"12d10\",\"damage_type\":\"lightning\",\"min_damage\":12,\"max_damage\":120,\"recharge\":true}",
  "{\"id\":\"Black Dragon Wyrmling\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/65/action/2/entries/0\",\"ability\":\"dex\",\"dc\":11,\"average\":22,\"dice\":\"5d8\",\"damage_type\":\"acid\",\"min_damage\":5,\"max_damage\":40,\"recharge\":true}",
  "{\"id\":\"Blue Dragon Wyrmling\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/70/action/2/entries/0\",\"ability\":\"dex\",\"dc\":12,\"average\":21,\"dice\":\"6d6\",\"damage_type\":\"lightning\",\"min_damage\":6,\"max_damage\":36,\"recharge\":true}",
  "{\"id\":\"Brass Dragon Wyrmling\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/75/action/1/entries/0\",\"ability\":\"dex\",\"dc\":11,\"average\":14,\"dice\":\"4d6\",\"damage_type\":\"fire\",\"min_damage\":4,\"max_damage\":24,\"recharge\":true}",
  "{\"id\":\"Bronze Dragon Wyrmling\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/77/action/2/entries/0\",\"ability\":\"dex\",\"dc\":12,\"average\":16,\"dice\":\"3d10\",\"damage_type\":\"lightning\",\"min_damage\":3,\"max_damage\":30,\"recharge\":true}",
  "{\"id\":\"Chimera\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":4,\"source_pointer\":\"/monster/93/action/4/entries/0\",\"ability\":\"dex\",\"dc\":15,\"average\":31,\"dice\":\"7d8\",\"damage_type\":\"fire\",\"min_damage\":7,\"max_damage\":56,\"recharge\":true}",
  "{\"id\":\"Copper Dragon Wyrmling\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/103/action/1/entries/0\",\"ability\":\"dex\",\"dc\":11,\"average\":18,\"dice\":\"4d8\",\"damage_type\":\"acid\",\"min_damage\":4,\"max_damage\":32,\"recharge\":true}",
  "{\"id\":\"Dracolich\",\"name\":\"Necrotic Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/129/action/2/entries/0\",\"ability\":\"con\",\"dc\":20,\"average\":52,\"dice\":\"8d12\",\"damage_type\":\"necrotic\",\"min_damage\":8,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Elephant\",\"name\":\"Trample\",\"category\":\"bonus\",\"slot\":0,\"source_pointer\":\"/monster/142/bonus/0/entries/0\",\"ability\":\"dex\",\"dc\":16,\"average\":17,\"dice\":\"2d10 + 6\",\"damage_type\":\"bludgeoning\",\"min_damage\":8,\"max_damage\":26,\"recharge\":false}",
  "{\"id\":\"Glabrezu\",\"name\":\"Pummel\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/207/action/2/entries/0\",\"ability\":\"dex\",\"dc\":17,\"average\":15,\"dice\":\"3d6 + 5\",\"damage_type\":\"bludgeoning\",\"min_damage\":8,\"max_damage\":23,\"recharge\":false}",
  "{\"id\":\"Gold Dragon Wyrmling\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/218/action/2/entries/0\",\"ability\":\"dex\",\"dc\":13,\"average\":22,\"dice\":\"4d10\",\"damage_type\":\"fire\",\"min_damage\":4,\"max_damage\":40,\"recharge\":true}",
  "{\"id\":\"Gorgon\",\"name\":\"Trample\",\"category\":\"bonus\",\"slot\":0,\"source_pointer\":\"/monster/219/bonus/0/entries/0\",\"ability\":\"dex\",\"dc\":16,\"average\":16,\"dice\":\"2d10 + 5\",\"damage_type\":\"bludgeoning\",\"min_damage\":7,\"max_damage\":25,\"recharge\":false}",
  "{\"id\":\"Green Dragon Wyrmling\",\"name\":\"Poison Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/224/action/2/entries/0\",\"ability\":\"con\",\"dc\":11,\"average\":21,\"dice\":\"6d6\",\"damage_type\":\"poison\",\"min_damage\":6,\"max_damage\":36,\"recharge\":true}",
  "{\"id\":\"Hell Hound\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/240/action/2/entries/0\",\"ability\":\"dex\",\"dc\":12,\"average\":17,\"dice\":\"5d6\",\"damage_type\":\"fire\",\"min_damage\":5,\"max_damage\":30,\"recharge\":true}",
  "{\"id\":\"Ice Mephit\",\"name\":\"Frost Breath\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/256/action/1/entries/0\",\"ability\":\"con\",\"dc\":10,\"average\":7,\"dice\":\"3d4\",\"damage_type\":\"cold\",\"min_damage\":3,\"max_damage\":12,\"recharge\":true}",
  "{\"id\":\"Iron Golem\",\"name\":\"Poison Breath\",\"category\":\"action\",\"slot\":3,\"source_pointer\":\"/monster/261/action/3/entries/0\",\"ability\":\"con\",\"dc\":18,\"average\":55,\"dice\":\"10d10\",\"damage_type\":\"poison\",\"min_damage\":10,\"max_damage\":100,\"recharge\":true}",
  "{\"id\":\"Kraken\",\"name\":\"Lightning Strike\",\"category\":\"action\",\"slot\":3,\"source_pointer\":\"/monster/269/action/3/entries/0\",\"ability\":\"dex\",\"dc\":23,\"average\":33,\"dice\":\"6d10\",\"damage_type\":\"lightning\",\"min_damage\":6,\"max_damage\":60,\"recharge\":false}",
  "{\"id\":\"Magma Mephit\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/285/action/1/entries/0\",\"ability\":\"dex\",\"dc\":11,\"average\":7,\"dice\":\"2d6\",\"damage_type\":\"fire\",\"min_damage\":2,\"max_damage\":12,\"recharge\":true}",
  "{\"id\":\"Mammoth\",\"name\":\"Trample\",\"category\":\"bonus\",\"slot\":0,\"source_pointer\":\"/monster/287/bonus/0/entries/0\",\"ability\":\"dex\",\"dc\":18,\"average\":29,\"dice\":\"4d10 + 7\",\"damage_type\":\"bludgeoning\",\"min_damage\":11,\"max_damage\":47,\"recharge\":false}",
  "{\"id\":\"Nothic\",\"name\":\"Rotting Gaze\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/323/action/2/entries/0\",\"ability\":\"con\",\"dc\":13,\"average\":17,\"dice\":\"5d6\",\"damage_type\":\"necrotic\",\"min_damage\":5,\"max_damage\":30,\"recharge\":false}",
  "{\"id\":\"Planetar\",\"name\":\"Holy Burst\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/349/action/2/entries/0\",\"ability\":\"dex\",\"dc\":20,\"average\":24,\"dice\":\"7d6\",\"damage_type\":\"radiant\",\"min_damage\":7,\"max_damage\":42,\"recharge\":false}",
  "{\"id\":\"Red Dragon Wyrmling\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/368/action/2/entries/0\",\"ability\":\"dex\",\"dc\":13,\"average\":24,\"dice\":\"7d6\",\"damage_type\":\"fire\",\"min_damage\":7,\"max_damage\":42,\"recharge\":true}",
  "{\"id\":\"Silver Dragon Wyrmling\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/399/action/2/entries/0\",\"ability\":\"con\",\"dc\":13,\"average\":18,\"dice\":\"4d8\",\"damage_type\":\"cold\",\"min_damage\":4,\"max_damage\":32,\"recharge\":true}",
  "{\"id\":\"Storm Giant\",\"name\":\"Lightning Storm\",\"category\":\"action\",\"slot\":3,\"source_pointer\":\"/monster/420/action/3/entries/0\",\"ability\":\"dex\",\"dc\":18,\"average\":55,\"dice\":\"10d10\",\"damage_type\":\"lightning\",\"min_damage\":10,\"max_damage\":100,\"recharge\":true}",
  "{\"id\":\"Tree Blight\",\"name\":\"Gnash\",\"category\":\"bonus\",\"slot\":0,\"source_pointer\":\"/monster/440/bonus/0/entries/0\",\"ability\":\"dex\",\"dc\":17,\"average\":19,\"dice\":\"3d8 + 6\",\"damage_type\":\"piercing\",\"min_damage\":9,\"max_damage\":30,\"recharge\":false}",
  "{\"id\":\"White Dragon Wyrmling\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/474/action/2/entries/0\",\"ability\":\"con\",\"dc\":12,\"average\":22,\"dice\":\"5d8\",\"damage_type\":\"cold\",\"min_damage\":5,\"max_damage\":40,\"recharge\":true}",
  "{\"id\":\"Winter Wolf\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":1,\"source_pointer\":\"/monster/478/action/1/entries/0\",\"ability\":\"con\",\"dc\":12,\"average\":18,\"dice\":\"4d8\",\"damage_type\":\"cold\",\"min_damage\":4,\"max_damage\":32,\"recharge\":true}",
  "{\"id\":\"Young Black Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/486/action/2/entries/0\",\"ability\":\"dex\",\"dc\":14,\"average\":49,\"dice\":\"14d6\",\"damage_type\":\"acid\",\"min_damage\":14,\"max_damage\":84,\"recharge\":true}",
  "{\"id\":\"Young Blue Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/487/action/2/entries/0\",\"ability\":\"dex\",\"dc\":16,\"average\":55,\"dice\":\"10d10\",\"damage_type\":\"lightning\",\"min_damage\":10,\"max_damage\":100,\"recharge\":true}",
  "{\"id\":\"Young Brass Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/488/action/2/entries/0\",\"ability\":\"dex\",\"dc\":14,\"average\":38,\"dice\":\"11d6\",\"damage_type\":\"fire\",\"min_damage\":11,\"max_damage\":66,\"recharge\":true}",
  "{\"id\":\"Young Bronze Dragon\",\"name\":\"Lightning Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/489/action/2/entries/0\",\"ability\":\"dex\",\"dc\":15,\"average\":49,\"dice\":\"9d10\",\"damage_type\":\"lightning\",\"min_damage\":9,\"max_damage\":90,\"recharge\":true}",
  "{\"id\":\"Young Copper Dragon\",\"name\":\"Acid Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/490/action/2/entries/0\",\"ability\":\"dex\",\"dc\":14,\"average\":40,\"dice\":\"9d8\",\"damage_type\":\"acid\",\"min_damage\":9,\"max_damage\":72,\"recharge\":true}",
  "{\"id\":\"Young Gold Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/491/action/2/entries/0\",\"ability\":\"dex\",\"dc\":17,\"average\":55,\"dice\":\"10d10\",\"damage_type\":\"fire\",\"min_damage\":10,\"max_damage\":100,\"recharge\":true}",
  "{\"id\":\"Young Green Dragon\",\"name\":\"Poison Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/492/action/2/entries/0\",\"ability\":\"con\",\"dc\":14,\"average\":42,\"dice\":\"12d6\",\"damage_type\":\"poison\",\"min_damage\":12,\"max_damage\":72,\"recharge\":true}",
  "{\"id\":\"Young Red Dragon\",\"name\":\"Fire Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/493/action/2/entries/0\",\"ability\":\"dex\",\"dc\":17,\"average\":56,\"dice\":\"16d6\",\"damage_type\":\"fire\",\"min_damage\":16,\"max_damage\":96,\"recharge\":true}",
  "{\"id\":\"Young Silver Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/495/action/2/entries/0\",\"ability\":\"con\",\"dc\":17,\"average\":49,\"dice\":\"11d8\",\"damage_type\":\"cold\",\"min_damage\":11,\"max_damage\":88,\"recharge\":true}",
  "{\"id\":\"Young White Dragon\",\"name\":\"Cold Breath\",\"category\":\"action\",\"slot\":2,\"source_pointer\":\"/monster/496/action/2/entries/0\",\"ability\":\"con\",\"dc\":15,\"average\":40,\"dice\":\"9d8\",\"damage_type\":\"cold\",\"min_damage\":9,\"max_damage\":72,\"recharge\":true}"
 };
 private static final Map<String,JSONObject> INDEX=new LinkedHashMap<>();
 private Xmm2025SaveDamageEngine(){}
 private static String key(String monster,String category,String action){
  return monster+"\u0000"+category+"\u0000"+action;
 }
 static {try {
  for(String raw:RAW){JSONObject row=new JSONObject(raw);
   String key=key(row.getString("id"),row.getString("category"),row.getString("name"));
   if(INDEX.put(key,row)!=null)throw new IllegalStateException("duplicate XMM save action");
  }
  if(INDEX.size()!=VERIFIED_RULES)throw new IllegalStateException("XMM save damage index incomplete");
 }catch(Exception e){throw new ExceptionInInitializerError(e);}}
 private static JSONObject blocked(String reason)throws Exception{
  return new JSONObject().put("resolved",false).put("status",reason).put("auto_rolls",false)
   .put("auto_mutation_allowed",false).put("damage_applied",false).put("turn_advanced",false);
 }
 public static JSONArray index()throws Exception{
  JSONArray out=new JSONArray();
  for(JSONObject row:INDEX.values())out.put(new JSONObject(row.toString()));
  return out;
 }
 public static JSONObject rule(String monster,String category,String action)throws Exception{
  JSONObject r=INDEX.get(key(monster,category,action));
  if(r==null)return blocked("source_action_not_in_verified_subset");
  return new JSONObject(r.toString()).put("resolved",true).put("source","XMM")
   .put("source_blob_sha1",SOURCE_BLOB).put("auto_rolls",false)
   .put("auto_mutation_allowed",false).put("target_validation","MANUAL_DM");
 }
 public static JSONObject resolve(JSONObject in)throws Exception{
  if(in==null)return blocked("missing_request");
  String monster=in.optString("monster"),category=in.optString("category"),action=in.optString("action");
  JSONObject r=INDEX.get(key(monster,category,action));
  if(r==null)return blocked("source_action_not_in_verified_subset");
  if(!CompiledStatblockEngine.has(monster))Xmm2025CompiledBestiaryEngine.install();
  JSONObject stat=CompiledStatblockEngine.get(monster);
  if(stat.length()==0)return blocked("compiled_statblock_missing");
  JSONArray actions="action".equals(category)?stat.getJSONArray("actions"):stat.getJSONArray("special_actions");
  int slot=r.getInt("slot");
  if("bonus".equals(category)){
   int found=-1,pos=0;
   for(int i=0;i<actions.length();i++)if("bonus_action".equals(actions.getJSONObject(i).optString("category"))&&pos++==slot){found=i;break;}
   slot=found;
  }
  if(slot<0||slot>=actions.length()||!action.equals(actions.getJSONObject(slot).optString("name")))
   return blocked("compiled_action_does_not_match_pinned_source");
  if(!in.optBoolean("target_confirmed",false))return blocked("manual_target_eligibility_confirmation_required");
  if(r.getBoolean("recharge")&&!in.optBoolean("recharge_ready_confirmed",false))
   return blocked("manual_recharge_readiness_confirmation_required");
  if(!in.has("save_total")||in.isNull("save_total")||!(in.opt("save_total") instanceof Number))
   return blocked("explicit_save_total_required");
  Number saveNumber=(Number)in.get("save_total");
  double saveDouble=saveNumber.doubleValue();
  if(!Double.isFinite(saveDouble)||saveDouble!=Math.floor(saveDouble)||saveDouble< -100||saveDouble>200)
   return blocked("invalid_save_total");
  int save=saveNumber.intValue();
  String mode=in.optString("damage_mode");
  int damage;
  if("average".equals(mode)){
   damage=r.getInt("average");
  }else if("rolled".equals(mode)){
   if(!in.has("damage_total")||in.isNull("damage_total")||!(in.opt("damage_total") instanceof Number))
    return blocked("explicit_damage_total_required");
   Number number=(Number)in.get("damage_total");
   double value=number.doubleValue();
   if(!Double.isFinite(value)||value!=Math.floor(value)||value<r.getInt("min_damage")||value>r.getInt("max_damage"))
    return blocked("damage_roll_out_of_source_bounds");
   damage=number.intValue();
  }else return blocked("explicit_damage_mode_required");
  boolean saved=save>=r.getInt("dc");
  int result=saved?damage/2:damage;
  return new JSONObject().put("resolved",true)
   .put("status","source_save_damage_calculated_pending_manual_application")
   .put("monster",monster).put("category",category).put("action",action)
   .put("source","XMM").put("source_blob_sha1",SOURCE_BLOB)
   .put("source_pointer",r.getString("source_pointer"))
   .put("save_ability",r.getString("ability")).put("save_dc",r.getInt("dc"))
   .put("save_total",save).put("save_success",saved)
   .put("damage_mode",mode).put("damage_dice",r.getString("dice"))
   .put("damage_type",r.getString("damage_type"))
   .put("damage_before_defenses",result)
   .put("resistance_vulnerability_immunity_pending",true)
   .put("target_confirmation_manual",true)
   .put("manual_damage_application_required",true)
   .put("damage_applied",false).put("turn_advanced",false)
   .put("auto_rolls",false).put("auto_mutation_allowed",false);
 }
 public static JSONObject coverage()throws Exception{
  return new JSONObject().put("source","XMM").put("verified_simple_save_damage_rules",VERIFIED_RULES)
   .put("source_blob_sha1",SOURCE_BLOB).put("auto_rolls",false)
   .put("full_combat_automation_certified",false);
 }
}
