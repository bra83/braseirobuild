package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
/** Deterministic structure effects from Kingmaker pp.543-553. */
public final class KingmakerStructureSemantics {
 private KingmakerStructureSemantics(){}
 private static final Map<String,String> BONUSES=new LinkedHashMap<String,String>();
 private static final Set<String> EDIFICES=new HashSet<String>();
 static {
  add("academy","creative_solution=2");
  add("alchemist","demolish=1");
  add("arcanists_tower","quell_unrest:magic=1");
  add("arena","celebrate_holiday=2;quell_unrest:warfare=2");
  add("bank","tap_treasury=1");
  add("barracks","garrison_army=1;recover_army=1;recruit_army=1");
  add("brewery","establish_trade_agreement=1");
  add("bridge","");
  add("castle","new_leadership=2;pledge_of_fealty=2;send_diplomatic_envoy=2;garrison_army=2;recover_army=2;recruit_army=2");
  add("cathedral","celebrate_holiday=3;provide_care=3;repair_reputation:corruption=3");
  add("cemetery","");
  add("construction_yard","build_structure=1;repair_reputation:decay=1");
  add("dump","demolish=1");
  add("embassy","send_diplomatic_envoy=1;request_foreign_aid=1");
  add("festival_hall","celebrate_holiday=1");
  add("foundry","work_sites:mine=1");
  add("garrison","outfit_army=1;train_army=1");
  add("general_store","");
  add("granary","");
  add("guildhall","economy_focus=1");
  add("herbalist","provide_care=1");
  add("hospital","provide_care=1;quell_unrest=1");
  add("houses","");
  add("illicit_market","clandestine_business=1");
  add("inn","hire_adventurers=1");
  add("jail","quell_unrest:intrigue=1");
  add("keep","deploy_army=1;garrison_army=1;train_army=1");
  add("library","rest_and_relax:scholarship=1");
  add("lumberyard","work_sites:lumber_camp=1");
  add("luxury_store","establish_trade_agreement=1");
  add("magic_shop","supernatural_solution=1");
  add("magical_streetlamps","");
  add("mansion","improve_lifestyle=1");
  add("marketplace","establish_trade_agreement=1");
  add("menagerie","rest_and_relax:wilderness=2");
  add("military_academy","pledge_of_fealty:warfare=2;train_army=2");
  add("mill","harvest_crops=1");
  add("mint","capital_investment=3;collect_taxes=3;repair_reputation:crime=3");
  add("monument","");
  add("museum","rest_and_relax:arts=1");
  add("noble_villa","improve_lifestyle=1;quell_unrest:politics=1");
  add("occult_shop","prognostication=2");
  add("opera_house","celebrate_holiday=3;create_a_masterpiece=3");
  add("orphanage","");
  add("palace","new_leadership=3;pledge_of_fealty=3;send_diplomatic_envoy=3;garrison_army=3;recover_army=3;recruit_army=3");
  add("park","rest_and_relax:wilderness=1");
  add("paved_streets","");
  add("pier","go_fishing=1");
  add("sacred_grove","quell_unrest:folklore=1");
  add("secure_warehouse","craft_luxuries=1");
  add("sewer_system","clandestine_business=1");
  add("shrine","celebrate_holiday=1");
  add("smithy","trade_commodities=1;outfit_army=1");
  add("specialized_artisan","craft_luxuries=1");
  add("stables","establish_trade_agreement=1");
  add("stockyard","gather_livestock=1");
  add("stonemason","work_sites:quarry=1");
  add("tannery","trade_commodities=1");
  add("tavern_dive","");
  add("tavern_popular","hire_adventurers=1;rest_and_relax:trade=1");
  add("tavern_luxury","hire_adventurers=2;rest_and_relax:trade=2");
  add("tavern_world_class","hire_adventurers=3;rest_and_relax:trade=3;repair_reputation:strife=3");
  add("temple","celebrate_holiday=1;provide_care=1");
  add("tenement","");
  add("theater","celebrate_holiday=2");
  add("thieves_guild","infiltration=1");
  add("town_hall","");
  add("trade_shop","purchase_commodities=1");
  add("university","creative_solution=3");
  add("wall_stone","");
  add("wall_wooden","");
  add("watchtower","resolve_event=1");
  add("waterfront","go_fishing=1;establish_trade_agreement=1;rest_and_relax:boating=1");
  Collections.addAll(EDIFICES,"academy","arena","castle","cathedral","keep","menagerie","military_academy","mint","monument","opera_house","palace","tavern_world_class","town_hall","university");
 }
 private static void add(String k,String b){if(BONUSES.put(k,b)!=null)throw new IllegalStateException("Estrutura duplicada.");}
 public static Set<String> keys(){return Collections.unmodifiableSet(BONUSES.keySet());}
 public static int count(JSONArray rows,String key){
  int total=0;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;
   if(key.equals(row.optString("structure_key"))&&"built".equals(row.optString("status","built")))total+=Math.max(0,row.optInt("count",1));}
  return total;
 }
 public static int activityBonus(JSONArray rows,String activity,String skill,int cap,JSONObject context){
  int best=0;
  for(String k:BONUSES.keySet()){
   int n=count(rows,k);if(n==0)continue;if(EDIFICES.contains(k))n=1;
   for(String token:BONUSES.get(k).split(";")){
    if(token.length()==0)continue;String[] pair=token.split("=");String[] target=pair[0].split(":");
    if(!target[0].equals(activity))continue;
    if(target.length>1&&!target[1].equals(skill)&&!target[1].equals(context.optString("site_type"))&&!target[1].equals(context.optString("ruin")))continue;
    long stacked=(long)Integer.parseInt(pair[1])*n;best=Math.max(best,(int)Math.min(Math.max(0,cap),stacked));
   }
  }
  return best;
 }
 public static int storageIncrease(JSONArray rows,String commodity){
  String building="food".equals(commodity)?"granary":"lumber".equals(commodity)?"lumberyard":"ore".equals(commodity)?"foundry":"stone".equals(commodity)?"stonemason":"luxuries".equals(commodity)?"secure_warehouse":"";
  return building.length()==0?0:count(rows,building);
 }
 public static int consumptionReduction(JSONArray rows,boolean waterMill){
  return (count(rows,"stockyard")>0?1:0)+(count(rows,"mill")>0&&waterMill?1:0)+(count(rows,"sewer_system")>0?1:0);
 }
 public static int dangerousEventUnrestReduction(JSONArray rows){return Math.min(4,count(rows,"cemetery"));}
 public static int itemAvailabilityLevel(int actual,JSONArray rows,String category){
  int general=count(rows,"general_store")+count(rows,"marketplace")>0?0:-2;
  int illicit=Math.min(3,count(rows,"illicit_market"));
  int waterfront=count(rows,"waterfront")>0?1:0;
  int specific=0;
  if("alchemical".equals(category))specific=Math.min(3,count(rows,"alchemist"));
  else if("divine".equals(category))specific=Math.max(Math.max(Math.min(3,count(rows,"magic_shop")),Math.min(3,count(rows,"occult_shop"))),Math.max(count(rows,"cathedral")>0?3:0,Math.max(Math.min(3,count(rows,"shrine")),Math.min(3,count(rows,"temple")))));
  else {
   int shop=Math.max(Math.min(3,count(rows,"magic_shop")),Math.min(3,count(rows,"occult_shop")));
   if("arcane".equals(category))shop=Math.max(shop,Math.min(3,count(rows,"arcanists_tower")));
   if("primal".equals(category))shop=Math.max(shop,Math.min(3,count(rows,"sacred_grove")));
   if("occult".equals(category))shop=Math.max(shop,Math.min(3,count(rows,"occult_shop")));
   if("luxury_magic".equals(category))shop=Math.max(shop,Math.min(3,count(rows,"luxury_store")));
   specific=shop;
  }
  return Math.max(0,actual+general+waterfront+Math.max(illicit,specific));
 }
 public static int pcItemBonus(JSONArray rows,String activity,String skill,String focus){
  int best=0;
  if(("recall_knowledge_investigating".equals(activity)&&"lore".equals(skill))||"research".equals(activity)||"decipher_writing".equals(activity)){
   if(count(rows,"library")>0)best=1;if(count(rows,"academy")>0)best=2;if(count(rows,"university")>0)best=3;
  }
  if(count(rows,"cathedral")>0&&(("recall_knowledge_investigating".equals(activity)&&("lore".equals(skill)||"religion".equals(skill)))||("research".equals(activity)&&"faith".equals(focus))))best=Math.max(best,3);
  if(count(rows,"occult_shop")>0&&"esoteric".equals(focus)&&("recall_knowledge".equals(activity)||"research".equals(activity)))best=Math.max(best,2);
  if(count(rows,"alchemist")>0&&"identify_alchemy".equals(activity))best=Math.max(best,1);
  if(count(rows,"arcanists_tower")>0&&("borrow_arcane_spell".equals(activity)||"learn_a_spell".equals(activity)))best=Math.max(best,1);
  if(count(rows,"hospital")>0&&"medicine".equals(skill)&&("treat_wounds".equals(activity)||"treat_disease".equals(activity)))best=Math.max(best,2);
  if("earn_income".equals(activity)&&"performance".equals(skill)){
   if(count(rows,"tavern_popular")>0)best=Math.max(best,1);
   if(count(rows,"tavern_luxury")+count(rows,"theater")>0)best=Math.max(best,2);
   if(count(rows,"tavern_world_class")+count(rows,"opera_house")>0)best=Math.max(best,3);
  }
  if("gather_information".equals(activity)){
   if(count(rows,"tavern_popular")>0)best=Math.max(best,1);
   if(count(rows,"tavern_luxury")>0)best=Math.max(best,2);
   if(count(rows,"tavern_world_class")>0)best=Math.max(best,3);
  }
  if(count(rows,"thieves_guild")>0&&"create_forgeries".equals(activity))best=Math.max(best,1);
  return best;
 }
 public static JSONObject buildEffects(String key,int priorSettlementCount,boolean firstSettlement,boolean firstTurn,boolean firstKingdom,boolean famous,boolean infamous,String mode,String ruin,int die1,int die2)throws Exception{
  if(!BONUSES.containsKey(key))throw new IllegalArgumentException("Estrutura fora do catálogo oficial.");
  if(EDIFICES.contains(key)&&priorSettlementCount>0)return new JSONObject().put("cosmetic_only",true);
  int unrest=0,crime=0,selectedRuin=0;
  if("barracks".equals(key)&&firstSettlement)unrest=-1;
  if("brewery".equals(key)&&priorSettlementCount<3)unrest=-1;
  if("garrison".equals(key))unrest=-1;
  if(("wall_stone".equals(key)||"wall_wooden".equals(key))&&firstSettlement)unrest=-1;
  if(firstTurn){
   if(Arrays.asList("houses","keep","monument","orphanage","park","tenement","theater","town_hall","watchtower","tavern_dive").contains(key))unrest=-1;
   if(Arrays.asList("noble_villa","tavern_popular","temple").contains(key))unrest=-2;
   if("cathedral".equals(key)||"opera_house".equals(key))unrest=-4;
   if("castle".equals(key)||"tavern_luxury".equals(key)||"tavern_world_class".equals(key)){
    if(die1<1||die1>4||("tavern_world_class".equals(key)&&(die2<1||die2>4)))throw new IllegalArgumentException("Resultado de d4 inválido.");
    unrest="castle".equals(key)?-die1:"tavern_luxury".equals(key)?-(die1+1):-(die1+die2);
   }
   if("jail".equals(key)||"magical_streetlamps".equals(key))crime=-1;
   if("tavern_dive".equals(key))crime=1;
   if("monument".equals(key))selectedRuin=-1;
  }
  if("palace".equals(key)&&firstKingdom)unrest=-10;
  if("illicit_market".equals(key)||"thieves_guild".equals(key))crime++;
  if("tenement".equals(key))selectedRuin++;
  if(selectedRuin!=0&&!Arrays.asList("corruption","crime","decay","strife").contains(ruin))throw new IllegalArgumentException("Escolha a Ruína da estrutura.");
  JSONObject out=new JSONObject().put("unrest",unrest).put("crime",crime).put("dice",new JSONArray().put(die1).put(die2));
  if(selectedRuin!=0)out.put(ruin,out.optInt(ruin)+selectedRuin);
  if("infamy".equals(mode)){if(infamous)out.put("infamy",1);else if(famous)out.put("infamy",-1);}
  else {if(famous)out.put("fame",1);else if(infamous)out.put("fame",-1);}
  return out;
 }
}

