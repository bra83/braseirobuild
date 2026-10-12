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
  JSONObject nok=new JSONObject().put("companion_key","nok_nok").put("influence",12).put("attitude","helpful").put("available",1);
  JSONArray history=KingmakerCompanionSemantics.alignmentHistory(nok,3,"indifferent",new JSONArray());
  nok.put("influence",3).put("attitude","indifferent").put("unlocked_rewards_json",history);
  check("chaotic_good".equals(KingmakerCompanionSemantics.nokAlignment(nok)),"Nok retains good alignment after influence loss");
  check(KingmakerCompanionSemantics.ongoing(nok,10).optInt("camping_circumstance_penalty")==0,"reformed Nok has no camping penalty");
  history=KingmakerCompanionSemantics.alignmentHistory(nok,0,"indifferent",history);
  nok.put("influence",0).put("unlocked_rewards_json",history);
  check("chaotic_evil".equals(KingmakerCompanionSemantics.nokAlignment(nok)),"Nok reverts at zero");
  nok.put("influence",4).put("unlocked_rewards_json",history);
  check("chaotic_evil".equals(KingmakerCompanionSemantics.nokAlignment(nok)),"zero reset persists when influence rises below six");
  JSONObject octavia=new JSONObject().put("companion_key","octavia").put("influence",6).put("available",1);
  check(KingmakerCompanionSemantics.automaticInvestment(octavia,"magister"),"Octavia Magister auto investment");
  check(!KingmakerCompanionSemantics.automaticInvestment(octavia,"general"),"automatic investment requires designated role");
  check(KingmakerCompanionSemantics.leadershipInfluence("regongar")==8,"Regongar leadership at eight");
  check(KingmakerCampaignGate.officialChapterForQuest(quest("q_oleg","active"))==3,"Oleg official chapter three");
  check(KingmakerCampaignGate.officialChapterForQuest(quest("q_fundacao_baronia","active"))==4,"kingdom foundation chapter four");
  check(KingmakerStructureSemantics.itemAvailabilityLevel(5,new JSONArray().put(structure("general_store",1)).put(structure("magic_shop",3)),"mundane")==5,"magic shops do not raise mundane item availability");
  JSONArray works=new JSONArray().put(structure("foundry",2)).put(structure("lumberyard",1));
  check(KingmakerStructureSemantics.activityBonus(works,"work_sites","industry",3,new JSONObject().put("site_type","mine"))==2,"foundry bonus only mine context");
  check(KingmakerStructureSemantics.activityBonus(works,"work_sites","industry",3,new JSONObject().put("site_type","lumber_camp"))==1,"lumberyard bonus only lumber context");
  check(KingmakerStructureSemantics.activityBonus(works,"work_sites","industry",3,new JSONObject().put("site_type","quarry"))==0,"wrong work site grants no bonus");
  JSONObject ritual=new JSONObject();JSONArray attendees=new JSONArray().put(7).put(9);
  for(int i=0;i<9;i++){
   ritual=KingmakerCampaignGate.advanceHelix(ritual,KingmakerCampaignGate.HELIX_ROOMS[i],10,26,true,false,attendees);
   check(ritual.optInt("step")==i+1,"ritual correct order step "+i);
  }
  check(!ritual.optBoolean("completed"),"nine checks require final closed icon placement");
  ritual=KingmakerCampaignGate.advanceHelix(ritual,"B1",10,26,true,true,attendees);
  check(ritual.optBoolean("completed")&&ritual.optInt("xp_award")==120,"ritual complete and award once");
  check(KingmakerCampaignGate.helixSaveStatusBonus(ritual,7,true,"teleportation")==4,"present hero teleportation protection in House");
  check(KingmakerCampaignGate.helixSaveStatusBonus(ritual,10,true,"teleportation")==0,"absent hero receives no protection");
  check(KingmakerCampaignGate.helixSaveStatusBonus(ritual,7,false,"teleportation")==0,"protection confined to House");
  check(KingmakerCampaignGate.helixSaveStatusBonus(ritual,7,true,"fire")==0,"no universal save bonus");
  boolean duplicate=false;try{KingmakerCampaignGate.advanceHelix(ritual,"B1",10,26,true,true,attendees);}catch(IllegalStateException expected){duplicate=true;}
  check(duplicate,"completed ritual cannot award twice");
  JSONObject wrong=KingmakerCampaignGate.advanceHelix(new JSONObject(),"B13",20,40,true,false,attendees);
  check(wrong.optInt("step")==0&&wrong.optBoolean("reset"),"wrong ritual chamber resets despite critical success");
  JSONObject failed=KingmakerCampaignGate.advanceHelix(new JSONObject().put("step",4),"B8",2,26,true,false,attendees);
  check(failed.optInt("step")==0,"failed occultism resets ritual");
  JSONObject twenty=KingmakerCampaignGate.advanceHelix(new JSONObject(),"B14",20,0,true,false,attendees);
  check(twenty.optInt("step")==0,"natural twenty raises critical failure only to failure");
  check(KingmakerCompanionSemantics.automaticInvestment(new JSONObject().put("companion_key","amiri").put("available",1),"warden"),"Amiri Warden automatic investment");
  check(KingmakerCompanionSemantics.automaticInvestment(new JSONObject().put("companion_key","linzi").put("available",1),"counselor"),"Linzi Counselor automatic investment");
  JSONObject tristian=new JSONObject().put("companion_key","tristian").put("available",1).put("influence",0);
  check(KingmakerCompanionSemantics.canLead(tristian,"fame"),"Tristian accepts famous kingdom without influence gate");
  check(!KingmakerCompanionSemantics.canLead(tristian,"infamy"),"Tristian rejects infamous kingdom below eight");
  tristian.put("influence",8);check(KingmakerCompanionSemantics.canLead(tristian,"infamy"),"Tristian joins infamous kingdom at eight");
  check(!KingmakerCompanionSemantics.canLead(ekun,"fame"),"Ekundayo must be helpful to accept leadership");
  ekun.put("attitude","helpful");check(KingmakerCompanionSemantics.canLead(ekun,"fame"),"helpful Ekundayo accepts leadership");
  check(KingmakerCampaignGate.playable(Arrays.asList(quest("q_terras_sombrias","active")),graph).isEmpty(),"retired invented quest excluded from guidance");
  JSONObject academy=KingmakerStructureSemantics.normalizeDefinition(new JSONObject().put("official_catalog_key","academy").put("prerequisites","library").put("min_settlement_type","city"));
  check(academy.optString("prerequisites").isEmpty(),"academy can be built without upgrading library");
  check("village".equals(academy.optString("min_settlement_type")),"no invented settlement rank for building");
  check(KingmakerStructureSemantics.normalizeDefinition(new JSONObject().put("official_catalog_key","bridge").put("lots",1)).optInt("lots")==0,"infrastructure occupies no lot");
  boolean wrongCapital=false;try{KingmakerStructureSemantics.requireCapital(new JSONObject().put("official_catalog_key","palace"),new JSONObject().put("name","Oleg"),"Restov");}catch(IllegalStateException expected){wrongCapital=true;}
  check(wrongCapital,"palace rejects other settlement");
  KingmakerStructureSemantics.requireCapital(new JSONObject().put("official_catalog_key","palace"),new JSONObject().put("name","Capital"),"Capital");
  System.out.println("R36_BOOK_SEMANTICS_ASSERTIONS_PASS="+assertions);
 }
}

