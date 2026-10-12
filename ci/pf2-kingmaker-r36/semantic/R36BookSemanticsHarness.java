package com.braseiro.pathfinder2e;
import java.util.*;
import org.json.*;
public final class R36BookSemanticsHarness {
 static int assertions=0;
 static void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
 static JSONObject structure(String key,int count)throws Exception{return new JSONObject().put("structure_key",key).put("count",count).put("status","built");}
 static JSONObject quest(String key,String status,String... dependencies)throws Exception{
  JSONArray p=new JSONArray();for(String k:dependencies)p.put(k);
  return new JSONObject().put("status",status).put("progress_json",new JSONObject().put("quest_key",key).put("chapter_key","chapter_01_stolen_lands_part1").put("prerequisites",p));
 }
 static JSONObject build(String key,int prior,boolean firstSettlement,boolean firstTurn,boolean firstKingdom,String ruin,int d1,int d2)throws Exception{
  return KingmakerStructureSemantics.buildEffects(key,prior,firstSettlement,firstTurn,firstKingdom,false,false,"fame",ruin,d1,d2);
 }
 public static void main(String[] args)throws Exception{
  check(KingmakerStructureSemantics.keys().size()==73,"all canonical keys");
  JSONArray lab=new JSONArray().put(structure("alchemist",2)).put(structure("dump",2));
  check(KingmakerStructureSemantics.activityBonus(lab,"demolish","industry",3,new JSONObject())==2,"different structures do not add");
  check(KingmakerStructureSemantics.activityBonus(lab,"claim_hex","industry",3,new JSONObject())==0,"bonus not applied to every skill check");
  check(KingmakerStructureSemantics.activityBonus(new JSONArray().put(structure("alchemist",5)),"demolish","industry",3,new JSONObject())==3,"identical structures cap");
  check(KingmakerStructureSemantics.activityBonus(new JSONArray().put(structure("academy",2)),"creative_solution","scholarship",3,new JSONObject())==2,"edifice not doubled");
  check(KingmakerStructureSemantics.activityBonus(new JSONArray().put(structure("arcanists_tower",2)),"quell_unrest","politics",3,new JSONObject())==0,"skill restriction");
  check(KingmakerStructureSemantics.activityBonus(new JSONArray().put(structure("arcanists_tower",2)),"quell_unrest","magic",3,new JSONObject())==2,"matching skill");
  JSONArray stores=new JSONArray().put(structure("general_store",1)).put(structure("magic_shop",2)).put(structure("arcanists_tower",3));
  check(KingmakerStructureSemantics.itemAvailabilityLevel(5,stores,"arcane")==8,"overlapping magic shops");
  stores.put(structure("waterfront",1));
  check(KingmakerStructureSemantics.itemAvailabilityLevel(5,stores,"arcane")==9,"waterfront explicitly stacks");
  JSONArray divine=new JSONArray().put(structure("general_store",1)).put(structure("cathedral",1)).put(structure("shrine",3)).put(structure("temple",3));
  check(KingmakerStructureSemantics.itemAvailabilityLevel(5,divine,"divine")==8,"divine shops overlap");
  check(KingmakerStructureSemantics.itemAvailabilityLevel(5,new JSONArray(),"mundane")==3,"missing basic market penalty");
  check(KingmakerStructureSemantics.storageIncrease(new JSONArray().put(structure("granary",3)).put(structure("foundry",2)),"food")==3,"food storage");
  check(KingmakerStructureSemantics.storageIncrease(new JSONArray().put(structure("granary",3)).put(structure("foundry",2)),"ore")==2,"ore storage");
  JSONArray utilities=new JSONArray().put(structure("stockyard",3)).put(structure("mill",3)).put(structure("sewer_system",1));
  check(KingmakerStructureSemantics.consumptionReduction(utilities,false)==2,"mill needs water; multiple stockyards once");
  check(KingmakerStructureSemantics.consumptionReduction(utilities,true)==3,"water mill consumption");
  check(KingmakerStructureSemantics.dangerousEventUnrestReduction(new JSONArray().put(structure("cemetery",7)))==4,"cemetery cap");
  check(KingmakerStructureSemantics.pcItemBonus(new JSONArray().put(structure("library",4)).put(structure("academy",2)),"decipher_writing","society","")==2,"PC item bonuses never sum");
  check(build("barracks",0,false,false,false,"",0,0).optInt("unrest")==0,"demolish/rebuild does not repeat first-ever benefit");
  check(build("barracks",0,true,false,false,"",0,0).optInt("unrest")==-1,"first barracks benefit");
  check(build("houses",0,true,true,true,"",0,0).optInt("unrest")==-1,"first houses of turn");
  check(build("houses",1,false,false,false,"",0,0).optInt("unrest")==0,"repeat houses same turn");
  check(build("castle",0,true,true,true,"",4,0).optInt("unrest")==-4,"recorded castle d4");
  check(build("tavern_world_class",0,true,true,true,"",3,4).optInt("unrest")==-7,"recorded two d4");
  check(build("monument",0,true,true,true,"decay",0,0).optInt("decay")==-1,"chosen ruin");
  check(build("tenement",2,false,false,false,"crime",0,0).optInt("crime")==1,"tenement ruin every build");
  check(KingmakerStructureSemantics.buildEffects("illicit_market",0,true,true,true,false,true,"fame","",0,0).optInt("fame")==-1,"opposed reputation loses point");
  JSONObject a=quest("q_oleg","active"),b=quest("q_kressle","unstarted","q_oleg");
  List<JSONObject> graph=Arrays.asList(a,b);
  check(!KingmakerCampaignGate.ready(b,graph),"locked prerequisite");
  a.put("status","completed");
  check(KingmakerCampaignGate.ready(b,graph),"completed prerequisite unlocks");
  check(!KingmakerCampaignGate.ready(quest("missing","unstarted","absent"),graph),"missing prerequisite fails closed");
  check(KingmakerCampaignGate.questKey(b).equals("q_kressle"),"production progress_json fallback");
  for(int chapter=0;chapter<=10;chapter++)check(KingmakerCampaignGate.officialChapter(String.format("chapter_%02d_asset",chapter))==chapter+1,"official chapter "+chapter);
  check(KingmakerCampaignGate.officialChapter("chapter_11_hex_atlas_pois")==0,"atlas is not chapter 12");
  check(KingmakerCampaignGate.officialChapter("chapter_12_companion_stories")==0,"companions supplemental");
  String[] companions={"amiri","ekundayo","jubilost","linzi","nok_nok","tristian","valerie","harrim","jaethal","kalikke_kanerah","octavia","regongar"};
  for(String k:companions)check(KingmakerCompanionSemantics.profile(k,new JSONObject().put("name",k)).optJSONArray("thresholds").length()>0,"published thresholds "+k);
  check(KingmakerCompanionSemantics.profile("linzi",new JSONObject().put("name","Linzi")).optJSONArray("thresholds").optJSONObject(0).optInt("points")==1,"Linzi friendly at 1");
  check(KingmakerCompanionSemantics.profile("valerie",new JSONObject().put("name","Valerie")).optJSONArray("thresholds").optJSONObject(1).optInt("points")==6,"Valerie helpful at 6");
  check(!KingmakerCompanionSemantics.questAvailable("harrim",20,true,"not_started",true,true),"secondary entry has no invented primary quest");
  check(!KingmakerCompanionSemantics.questAvailable("tristian",10,true,"not_started",false,true),"Tristian after Vordakai");
  check(KingmakerCompanionSemantics.questAvailable("tristian",10,true,"not_started",true,true),"Tristian correct trigger");
  check(!KingmakerCompanionSemantics.questAvailable("amiri",10,true,"not_started",true,true),"Amiri needs 11");
  JSONObject ekun=new JSONObject().put("companion_key","ekundayo").put("available",1).put("attitude","helpful");
  check(KingmakerCompanionSemantics.constructionDcAdjustment(ekun,1)==-2,"Ekun lumber benefit");
  check(KingmakerCompanionSemantics.constructionDcAdjustment(ekun,0)==0,"Ekun no benefit without lumber");
  ekun.put("attitude","friendly");check(KingmakerCompanionSemantics.constructionDcAdjustment(ekun,1)==0,"Ekun ongoing benefit conditional");
  System.out.println("R36_BOOK_SEMANTICS_ASSERTIONS_PASS="+assertions);
 }
}

