package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
/** Book-specific influence thresholds and access gates; no generic 30-point track. */
public final class KingmakerCompanionSemantics {
 private KingmakerCompanionSemantics(){}
 public static JSONObject profile(String raw,JSONObject base)throws Exception{
  String k=raw;
  if(base.length()==0)return base;
  int[] points;String[] attitudes;
  if("amiri".equals(k)||"harrim".equals(k)){points=new int[]{2,4};attitudes=new String[]{"friendly","helpful"};}
  else if("linzi".equals(k)){points=new int[]{1,2};attitudes=new String[]{"friendly","helpful"};}
  else if("jaethal".equals(k)){points=new int[]{3,6};attitudes=new String[]{"friendly","helpful"};}
  else if("ekundayo".equals(k)||"regongar".equals(k)){points=new int[]{2,4,8};attitudes=new String[]{"indifferent","friendly","helpful"};
    if("regongar".equals(k)){points=new int[]{2,4,6};attitudes=new String[]{"indifferent","friendly","helpful"};}}
  else if("kalikke_kanerah".equals(k)){points=new int[]{4,6};attitudes=new String[]{"friendly","helpful"};}
  else if("octavia".equals(k)){points=new int[]{2,4};attitudes=new String[]{"friendly","helpful"};}
  else if("nok_nok".equals(k)){points=new int[]{4,8};attitudes=new String[]{"friendly","helpful"};}
  else if("jubilost".equals(k)||"tristian".equals(k)){points=new int[]{2,8};attitudes=new String[]{"friendly","helpful"};}
  else if("valerie".equals(k)){points=new int[]{3,6};attitudes=new String[]{"friendly","helpful"};}
  else throw new IllegalArgumentException("Companheiro sem fonte oficial: "+k);
  JSONArray thresholds=new JSONArray();
  for(int i=0;i<points.length;i++)thresholds.put(new JSONObject().put("points",points[i]).put("attitude",attitudes[i]).put("attitude_label",label(attitudes[i])));
  base.put("initial_attitude",("ekundayo".equals(k)||"regongar".equals(k))?"unfriendly":"indifferent");
  base.put("thresholds",thresholds).put("influence_rule_source",source(k))
      .put("quest_minimum_level",questLevel(k)).put("official_personal_quest",questLevel(k)>0);
  // Secondary entries do not have the primary companions' seven published quests.
  if(questLevel(k)==0)base.put("personal_quest","").put("personal_quest_summary","");
  return base;
 }
 private static String label(String k){return "friendly".equals(k)?"Amistoso":"helpful".equals(k)?"Prestativo":"indifferent".equals(k)?"Indiferente":"Inamistoso";}
 public static String source(String k){
  if("amiri".equals(k))return "Kingmaker p.19; Companion Guide pp.6-21";
  if("harrim".equals(k))return "Kingmaker p.20; Companion Guide pp.94-95";
  if("jaethal".equals(k))return "Kingmaker pp.20-21; Companion Guide pp.96-97";
  if("linzi".equals(k))return "Kingmaker p.21; Companion Guide pp.44-57";
  if("valerie".equals(k))return "Kingmaker p.23; Companion Guide pp.82-93";
  return "Kingmaker Companion Guide: "+k;
 }
 public static int questLevel(String k){
  if("amiri".equals(k))return 11;if("ekundayo".equals(k))return 6;
  if("jubilost".equals(k))return 8;if("linzi".equals(k))return 7;
  if("nok_nok".equals(k))return 5;if("tristian".equals(k))return 10;
  if("valerie".equals(k))return 9;return 0;
 }
 public static boolean questAvailable(String k,int partyLevel,boolean available,String status,boolean vordakaiResolved,boolean bloomResolved){
  int level=questLevel(k);if(level==0||partyLevel<level||!available||KingmakerCampaignGate.completed(status))return false;
  if("tristian".equals(k))return vordakaiResolved;
  if("jubilost".equals(k))return bloomResolved;
  return true;
 }
 public static int constructionDcAdjustment(JSONObject companion,int lumberCost){
  return "ekundayo".equals(companion.optString("companion_key"))&&companion.optInt("available",1)==1&&
    "helpful".equals(companion.optString("attitude"))&&lumberCost>0?-2:0;
 }
 public static int leadershipInfluence(String k){
  if("harrim".equals(k)||"nok_nok".equals(k)||"regongar".equals(k))return 8;
  if("jaethal".equals(k)||"octavia".equals(k))return 6;
  if("kalikke_kanerah".equals(k))return 4;return 0;
 }
 public static boolean automaticInvestment(JSONObject c,String role){
  if(c.optInt("available",1)!=1)return false;String k=c.optString("companion_key");
  return ("harrim".equals(k)&&"magister".equals(role)&&c.optInt("influence")>=8)||
         ("jaethal".equals(k)&&"emissary".equals(role)&&c.optInt("influence")>=6)||
         ("nok_nok".equals(k)&&"emissary".equals(role)&&c.optInt("influence")>=8)||
         ("octavia".equals(k)&&"magister".equals(role)&&c.optInt("influence")>=6)||
         ("regongar".equals(k)&&"general".equals(role)&&c.optInt("influence")>=8);
 }
 public static String nokAlignment(JSONObject c){
  int p=c.optInt("influence");
  if(p<=0||"hostile".equals(c.optString("attitude")))return "chaotic_evil";
  JSONArray history=c.optJSONArray("unlocked_rewards_json");
  if(history==null)try{history=new JSONArray(c.optString("unlocked_rewards_json","[]"));}catch(Exception ignored){history=new JSONArray();}
  String saved="chaotic_evil";
  for(int i=0;i<history.length();i++){
   if("nok_nok_alignment:chaotic_good".equals(history.optString(i)))saved="chaotic_good";
   else if("nok_nok_alignment:chaotic_neutral".equals(history.optString(i))&&!"chaotic_good".equals(saved))saved="chaotic_neutral";
  }
  if(p>=12)return "chaotic_good";
  if(p>=6&&"chaotic_evil".equals(saved))return "chaotic_neutral";
  return saved;
 }
 public static JSONArray alignmentHistory(JSONObject before,int next,String attitude,JSONArray rewards)throws Exception{
  JSONArray out=new JSONArray();
  for(int i=0;i<rewards.length();i++)if(!rewards.optString(i).startsWith("nok_nok_alignment:"))out.put(rewards.optString(i));
  if(!"nok_nok".equals(before.optString("companion_key")))return out;
  JSONObject current=new JSONObject(before.toString());
  String prior=nokAlignment(before);
  out.put("nok_nok_alignment:"+prior);
  current.put("influence",next).put("attitude",attitude).put("unlocked_rewards_json",out);
  String alignment=nokAlignment(current);
  JSONArray result=new JSONArray();
  for(int i=0;i<out.length();i++)if(!out.optString(i).startsWith("nok_nok_alignment:"))result.put(out.optString(i));
  return result.put("nok_nok_alignment:"+alignment);
 }
 public static JSONObject ongoing(JSONObject c,int partyLevel)throws Exception{
  String k=c.optString("companion_key");int p=c.optInt("influence");boolean present=c.optInt("available",1)==1;
  JSONObject out=new JSONObject().put("companion_key",k).put("active",present);
  if("nok_nok".equals(k))out.put("alignment",nokAlignment(c)).put("camping_circumstance_penalty",present&&"chaotic_evil".equals(nokAlignment(c))?-1:0).put("primary_hero_influence_bonus",present&&p>=2?1:0);
  if("ekundayo".equals(k))out.put("lumber_construction_dc_adjustment",constructionDcAdjustment(c,1));
  if("harrim".equals(k))out.put("infectious_ennui_access",present&&p>=6&&partyLevel>=5).put("magister_automatic_investment",automaticInvestment(c,"magister"));
  if("jaethal".equals(k))out.put("emissary_automatic_investment",automaticInvestment(c,"emissary")).put("weapon_rune_access",present&&p>=8&&partyLevel>=7);
  if("octavia".equals(k))out.put("vision_of_beauty_access",present&&p>=4&&partyLevel>=3);
  if("regongar".equals(k))out.put("energy_absorbing_rune_access",present&&p>=6&&partyLevel>=11);
  if("kalikke_kanerah".equals(k))out.put("daily_persona_choice",present&&p>=8).put("separation_ritual_available",present&&p>=8&&partyLevel>=13);
  return out;
 }
}

