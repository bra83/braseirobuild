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
 static JSONObject growthRow(String key,int grid,int block,int... lots)throws Exception{
  JSONArray selected=new JSONArray();for(int lot:lots)selected.put(lot);
  JSONObject position=new JSONObject().put("urban_grid",grid).put("block",block).put("lots",selected);
  return structure(key,1).put("lots",lots.length).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(position)));
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
  JSONObject jubilost=new JSONObject().put("companion_key","jubilost").put("influence",8).put("available",1).put("unlocked_rewards_json","[]");
  check(KingmakerCompanionSemantics.structureRewards(jubilost).length()==1,"Jubilost offers one alchemy laboratory");
  jubilost.put("unlocked_rewards_json",KingmakerCompanionSemantics.claimStructureReward(jubilost,"alchemist"));
  check(KingmakerCompanionSemantics.structureRewards(jubilost).length()==0,"Jubilost free structure claim deduplicated");
  boolean wrongGift=false;try{KingmakerCompanionSemantics.claimStructureReward(jubilost,"palace");}catch(IllegalStateException expected){wrongGift=true;}
  check(wrongGift,"companion cannot grant arbitrary free structure");
  tristian.put("unlocked_rewards_json","[]");
  check(KingmakerCompanionSemantics.structureRewards(tristian).length()==2,"Tristian offers orphanage and shrine");
  tristian.put("unlocked_rewards_json",KingmakerCompanionSemantics.claimStructureReward(tristian,"orphanage"));
  check(KingmakerCompanionSemantics.structureRewards(tristian).length()==1,"Tristian shrine can be placed separately");
  JSONObject offered=new JSONObject().put("companion_key","jubilost").put("influence",7).put("available",1).put("unlocked_rewards_json","[]");
  offered.put("unlocked_rewards_json",KingmakerCompanionSemantics.rewardHistory(offered,8,"helpful",new JSONArray())).put("influence",3);
  check(KingmakerCompanionSemantics.structureRewards(offered).length()==1,"unclaimed official gift remains after influence loss");
  JSONObject hostileNok=new JSONObject().put("companion_key","nok_nok").put("influence",12).put("attitude","hostile").put("unlocked_rewards_json",new JSONArray().put("nok_nok_alignment:chaotic_good"));
  JSONArray resetNok=KingmakerCompanionSemantics.alignmentHistory(hostileNok,12,"helpful",new JSONArray());
  hostileNok.put("attitude","helpful").put("unlocked_rewards_json",resetNok);
  check("chaotic_evil".equals(KingmakerCompanionSemantics.nokAlignment(hostileNok)),"hostility reset cannot be undone by zero influence adjustment");
  check(KingmakerStructureSemantics.investmentGp(2)==18,"official investment level two is eighteen gp");
  check(KingmakerStructureSemantics.investmentGp(20)==35000,"official investment level twenty");
  JSONObject funded=KingmakerStructureSemantics.investmentOutcome(1,10,"SUCCESS",4,new JSONArray().put(3).put(4),0,"none");
  check(funded.optInt("rp_gained")==7&&funded.optInt("gold_spent")==10,"successful investment rolls two resource dice");
  check(KingmakerStructureSemantics.investmentOutcome(1,10,"CRITICAL_SUCCESS",4,new JSONArray().put(1).put(2).put(3).put(4),0,"none").optInt("rp_gained")==10,"critical investment four resource dice");
  JSONObject low=KingmakerStructureSemantics.investmentOutcome(2,17,"CRITICAL_SUCCESS",4,new JSONArray(),0,"none");
  check(low.optBoolean("automatic_critical_failure")&&low.optInt("rp_gained")==0&&low.optInt("crime")==1,"underfunded investment overrides critical success");
  check(KingmakerStructureSemantics.investmentOutcome(1,25,"SUCCESS",4,new JSONArray().put(3).put(4),0,"none").optInt("gold_spent")==25,"investment excess is spent not converted linearly");
  JSONObject loss=KingmakerStructureSemantics.investmentOutcome(1,10,"CRITICAL_FAILURE",6,new JSONArray().put(5),0,"resource_die");
  check(loss.optInt("rp_gained")==5&&loss.optInt("crime")==5,"critical investment optional die adds equal crime");
  check(KingmakerStructureSemantics.investmentOutcome(1,10,"FAILURE",12,new JSONArray(),4,"none").optInt("rp_gained")==4,"failed investment uses d4 not resource die");
  JSONObject tapped=KingmakerStructureSemantics.tapTreasuryOutcome(2,"SUCCESS",false,"crime");
  check(tapped.optInt("gold_gained")==18&&tapped.optInt("economy_circumstance_penalty")==-1,"treasury grants gp and overdraw penalty");
  JSONObject repeated=KingmakerStructureSemantics.tapTreasuryOutcome(2,"CRITICAL_SUCCESS",true,"crime");
  check("FAILURE".equals(repeated.optString("degree"))&&repeated.optInt("gold_gained")==0,"unpaid treasury worsens result two degrees");
  check(repeated.optInt("loyalty_circumstance_penalty")==-1,"failed treasury also penalizes loyalty");
  JSONObject worse=KingmakerStructureSemantics.tapTreasuryOutcome(2,"SUCCESS",true,"strife");
  check(worse.optInt("unrest")==1&&worse.optInt("strife")==1,"critical treasury choice affects chosen ruin");
  JSONObject guild=structure("guildhall",3).put("effects_json",new JSONObject().put("trade_focus_instances",new JSONArray().put("Padaria").put("Ferraria").put("padaria")));
  JSONArray guilds=new JSONArray().put(guild);JSONObject bread=new JSONObject().put("ability","economy").put("trade_focus","PADARIA");
  check(KingmakerStructureSemantics.activityBonus(guilds,"trade_commodities","industry",3,bread)==2,"matching guild focuses stack within settlement cap");
  bread.put("ability","stability");check(KingmakerStructureSemantics.activityBonus(guilds,"build_structure","engineering",3,bread)==0,"guild focus bonus only economy skills");
  bread.put("ability","economy").put("trade_focus","tecelagem");check(KingmakerStructureSemantics.activityBonus(guilds,"trade_commodities","industry",3,bread)==0,"unrelated trade gains no guild bonus");
  check(KingmakerStructureSemantics.pcItemBonus(guilds,"earn_income","crafting","padaria")==1,"PC guild bonus stays one item bonus");
  JSONObject shop=structure("trade_shop",1).put("effects_json",new JSONObject().put("trade_focus_instances",new JSONArray().put("ferraria")));
  check(KingmakerStructureSemantics.pcItemBonus(new JSONArray().put(shop),"craft","crafting","Ferraria")==1,"trade shop related crafting item bonus");
  check(KingmakerStructureSemantics.pcItemBonus(new JSONArray().put(shop),"craft","crafting","padaria")==0,"trade shop bonus requires associated craft");
  JSONArray craftingShops=new JSONArray().put(structure("smithy",1)).put(structure("specialized_artisan",1));
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","crafting","metal")==1,"Smithy bonuses apply to metal Crafting");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","crafting","METALURGIA")==1,"Smithy PT-BR focus normalized");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","crafting","joalheria")==1,"Artisan bonuses apply to jewelry Crafting");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","crafting","clockwork")==1,"Artisan bonuses apply to clockwork Crafting");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","crafting","wood")==0,"unrelated crafting material gains no item bonus");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"repair","crafting","metal")==0,"Crafting-only building bonus cannot spill into Repair");
  check(KingmakerStructureSemantics.pcItemBonus(craftingShops,"craft","society","metal")==0,"Crafting-only building bonus cannot spill into Society");
  check(KingmakerStructureSemantics.pcItemBonus(new JSONArray().put(structure("smithy",3)),"craft","crafting","metal")==1,"additional smithies never stack PC item bonuses");
  JSONObject oldGuild=structure("guildhall",2).put("effects_json",new JSONObject());
  JSONArray declared=KingmakerStructureSemantics.declareGuildFocus(oldGuild,2,"ferraria");
  check(declared.length()==2&&declared.optString(0).isEmpty()&&"ferraria".equals(declared.optString(1)),"legacy focus declared per establishment");
  oldGuild.put("effects_json",new JSONObject().put("trade_focus_instances",declared));
  boolean changedFocus=false;try{KingmakerStructureSemantics.declareGuildFocus(oldGuild,2,"padaria");}catch(IllegalStateException expected){changedFocus=true;}
  check(changedFocus,"declared focus cannot change freely for next check");
  check(build("houses",0,true,true,true,"",4,4).optJSONArray("dice").length()==0,"only actual rule dice recorded");
  check(build("castle",0,true,true,true,"",4,4).optJSONArray("dice").length()==1,"castle uses one d4");

  JSONObject metropolis=new JSONObject().put("type","metropolis").put("developed_blocks",10);
  JSONObject firstGrid=new JSONObject().put("urban_grid",1),secondGrid=new JSONObject().put("urban_grid",2);
  KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray(),"paved_streets",secondGrid);
  check(true,"existing second metropolis grid accepts paving");
  boolean badGrid=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray(),"paved_streets",new JSONObject().put("urban_grid",3));}catch(IllegalArgumentException expected){badGrid=true;}check(badGrid,"unallocated grid rejected");
  boolean nonMetro=false;try{KingmakerStructureSemantics.requireGridInfrastructure(new JSONObject().put("type","city").put("developed_blocks",10),new JSONArray(),"paved_streets",secondGrid);}catch(IllegalStateException expected){nonMetro=true;}check(nonMetro,"multiple grids require metropolis");
  JSONArray installations=KingmakerStructureSemantics.appendedInfrastructure(new JSONObject(),"paved_streets",firstGrid);
  JSONObject paving=structure("paved_streets",1).put("effects_json",new JSONObject().put("infrastructure_instances",installations));
  boolean gridDuplicate=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray().put(paving),"paved_streets",firstGrid);}catch(IllegalStateException expected){gridDuplicate=true;}check(gridDuplicate,"paving cannot be paid for twice in same grid");
  KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray().put(paving),"paved_streets",secondGrid);
  installations=KingmakerStructureSemantics.appendedInfrastructure(paving,"paved_streets",secondGrid);
  check(installations.length()==2&&installations.optJSONObject(1).optInt("urban_grid")==2,"second paving location persisted");
  paving.put("count",2).put("effects_json",new JSONObject().put("infrastructure_instances",installations));
  JSONObject trimmed=KingmakerStructureSemantics.trimInstanceEffects(paving,1);
  check(trimmed.optJSONArray("infrastructure_instances").length()==1&&trimmed.optJSONArray("infrastructure_instances").optJSONObject(0).optInt("urban_grid")==1,"demolition removes last installation only");
  JSONObject sewer=structure("sewer_system",1).put("effects_json",new JSONObject().put("infrastructure_instances",KingmakerStructureSemantics.appendedInfrastructure(new JSONObject(),"sewer_system",secondGrid)));
  check(new JSONObject(sewer.opt("effects_json").toString()).optJSONArray("infrastructure_instances").optJSONObject(0).optBoolean("settlement_wide"),"sewer applies across all metropolis grids");
  boolean doubleSewer=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray().put(sewer),"sewer_system",firstGrid);}catch(IllegalStateException expected){doubleSewer=true;}check(doubleSewer,"sewer cannot stack across grids");
  check(KingmakerStructureSemantics.consumptionReduction(new JSONArray().put(sewer),false)==1,"metropolis sewer consumption reduction once");
  JSONObject restarted=new JSONObject(paving.toString());
  boolean restartDuplicate=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray().put(restarted),"paved_streets",secondGrid);}catch(IllegalStateException expected){restartDuplicate=true;}check(restartDuplicate,"persisted grid location survives serialization");
  boolean missingLocation=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray(),"magical_streetlamps",new JSONObject());}catch(IllegalArgumentException expected){missingLocation=true;}check(missingLocation,"infrastructure requires explicit grid");
  boolean legacyLocation=false;try{KingmakerStructureSemantics.requireGridInfrastructure(metropolis,new JSONArray().put(structure("magical_streetlamps",1)),"magical_streetlamps",firstGrid);}catch(IllegalStateException expected){legacyLocation=true;}check(legacyLocation,"legacy unknown locations never guessed");
  check(!KingmakerStructureSemantics.isGridInfrastructure("bridge")&&!KingmakerStructureSemantics.isGridInfrastructure("wall_stone"),"border infrastructure needs separate placement");
  check(KingmakerStructureSemantics.trimInstanceEffects(oldGuild,1).optJSONArray("trade_focus_instances").length()==1,"demolition also preserves remaining trade focus indices");

  JSONObject twinC=new JSONObject().put("companion_key","kalikke_kanerah").put("available",1).put("influence",8);
  JSONObject twinPrepared=KingmakerCompanionSemantics.selectTwinsPersona(twinC,new JSONObject(),"kalikke",2,"kalikke");
  check("kalikke".equals(twinPrepared.optString("persona"))&&twinPrepared.optInt("prepared_day")==2,"twin persona stored for daily preparations");
  boolean twinRepeat=false;try{KingmakerCompanionSemantics.selectTwinsPersona(twinC,twinPrepared,"kanerah",2,"kalikke");}catch(IllegalStateException expected){twinRepeat=true;}check(twinRepeat,"cannot switch twin again in same preparation day");
  JSONObject twinSame=KingmakerCompanionSemantics.selectTwinsPersona(twinC,twinPrepared,"kalikke",2,"kalikke");check(twinSame.optInt("prepared_day")==2&&"kalikke".equals(twinSame.optString("persona"))&&"kalikke_kanerah".equals(twinSame.optString("shared_influence_key")),"same twin receipt idempotent");
  JSONObject twinNext=KingmakerCompanionSemantics.selectTwinsPersona(twinC,twinPrepared,"kanerah",3,"kalikke");
  check("kanerah".equals(twinNext.optString("persona")),"new daily preparations permit persona change");
  check(twinC.optInt("influence")==8&&"kalikke_kanerah".equals(twinNext.optString("shared_influence_key")),"persona choice never splits or alters shared influence");
  check(KingmakerCompanionSemantics.automaticInvestment(twinC,"counselor","kalikke"),"active Kalikke automatically invests counselor");
  check(!KingmakerCompanionSemantics.automaticInvestment(twinC,"emissary","kalikke"),"Kalikke does not grant Kanerah investment");
  check(KingmakerCompanionSemantics.automaticInvestment(twinC,"emissary","kanerah"),"active Kanerah automatically invests emissary");
  check(!KingmakerCompanionSemantics.automaticInvestment(twinC,"counselor","kanerah"),"Kanerah does not grant Kalikke investment");
  twinC.put("influence",6);boolean twinEarly=false;try{KingmakerCompanionSemantics.selectTwinsPersona(twinC,new JSONObject(),"kanerah",2,"kalikke");}catch(IllegalStateException expected){twinEarly=true;}check(twinEarly,"PC directed persona selection requires influence eight");
  check("kanerah".equals(KingmakerCompanionSemantics.selectTwinsPersona(twinC,twinNext,"kanerah",4,"kalikke").optString("persona")),"lower influence keeps already present twin");
  boolean twinOld=false;try{KingmakerCompanionSemantics.selectTwinsPersona(twinC,twinNext,"kanerah",2,"kalikke");}catch(IllegalStateException expected){twinOld=true;}check(twinOld,"old rest receipt cannot overwrite present persona");
  twinC.put("influence",8);check(KingmakerCompanionSemantics.ongoing(twinC,10).optBoolean("separation_hook_available"),"separation request available at influence eight");
  check(KingmakerCompanionSemantics.ongoing(twinC,10).optBoolean("powerful_magic_or_freedom_ritual_option"),"separation magic option has no invented level thirteen gate");
  twinC.put("available",0);check(!KingmakerCompanionSemantics.automaticInvestment(twinC,"emissary","kanerah"),"absent twin grants no investment");

  check(!KingmakerCompanionSemantics.questAvailable("amiri",11,true,"not_started",false,true),"Amiri rumors follow Vordakai");
  check(KingmakerCompanionSemantics.questAvailable("amiri",11,true,"not_started",true,true),"Amiri source trigger available");
  check(KingmakerCompanionSemantics.questAvailable("jubilost",8,true,"not_started",false,false),"Jubilost preferred Bloom timing is not mandatory prerequisite");
  for(String primaryKey:new String[]{"amiri","ekundayo","jubilost","linzi","nok_nok","tristian","valerie"}){
   JSONObject officialQuest=KingmakerCompanionSemantics.personalQuestDefinition(primaryKey);
   check(officialQuest.optString("source_ref").startsWith("Kingmaker Companion Guide pp.")&&officialQuest.optInt("minimum_level")==KingmakerCompanionSemantics.questLevel(primaryKey),"published personal quest source "+primaryKey);
  }
  check(KingmakerCompanionSemantics.personalQuestDefinition("harrim").length()==0,"no invented published secondary personal adventure");
  check(KingmakerCompanionSemantics.profile("tristian",new JSONObject().put("personal_quest","Deva caído inventado")).optString("personal_quest").equals("O Reino dos Purificados"),"Tristian narrative supersedes invented fallen deva quest");

  JSONObject borderTown=new JSONObject().put("type","town").put("developed_blocks",4).put("grid_json",new JSONObject());
  JSONArray borderAllWater=new JSONArray().put("north").put("east").put("south").put("west");
  JSONObject borderTopology=KingmakerStructureSemantics.declareWaterBorders(borderTown,1,borderAllWater,true);
  borderTown.put("grid_json",borderTopology);
  check(KingmakerStructureSemantics.isIsland(borderTown,new JSONArray()),"four water borders without bridge form island");
  boolean borderImmutable=false;try{KingmakerStructureSemantics.declareWaterBorders(borderTown,1,new JSONArray().put("north"),true);}catch(IllegalStateException expected){borderImmutable=true;}check(borderImmutable,"water borders permanent after declaration");
  boolean borderDry=false;try{KingmakerStructureSemantics.declareWaterBorders(new JSONObject().put("type","village").put("developed_blocks",1),1,new JSONArray().put("north"),false);}catch(IllegalArgumentException expected){borderDry=true;}check(borderDry,"water border requires eligible hex");
  boolean borderRepeat=false;try{KingmakerStructureSemantics.declareWaterBorders(new JSONObject().put("type","village"),1,new JSONArray().put("north").put("north"),true);}catch(IllegalArgumentException expected){borderRepeat=true;}check(borderRepeat,"repeated border rejected");
  boolean borderEdit=false;try{KingmakerStructureSemantics.preserveWaterBorders(borderTopology,new JSONObject());}catch(IllegalStateException expected){borderEdit=true;}check(borderEdit,"generic settlement editor cannot erase topology");
  JSONObject borderNewNotes=new JSONObject(borderTopology.toString()).put("other_note","preserved");
  check(KingmakerStructureSemantics.preserveWaterBorders(borderTopology,borderNewNotes).optString("other_note").equals("preserved"),"other civic grid fields editable");
  JSONObject borderNorth=new JSONObject().put("urban_grid",1).put("border","north");
  KingmakerStructureSemantics.requireBorderInfrastructure(borderTown,new JSONArray(),"bridge",borderNorth);check(true,"bridge occupies declared water border");
  JSONObject borderBridge=structure("bridge",1).put("effects_json",new JSONObject().put("infrastructure_instances",KingmakerStructureSemantics.appendedInfrastructure(new JSONObject(),"bridge",borderNorth)));
  check(!KingmakerStructureSemantics.isIsland(borderTown,new JSONArray().put(borderBridge)),"completed bridge restores island connection");
  boolean borderDoubleBridge=false;try{KingmakerStructureSemantics.requireBorderInfrastructure(borderTown,new JSONArray().put(borderBridge),"bridge",borderNorth);}catch(IllegalStateException expected){borderDoubleBridge=true;}check(borderDoubleBridge,"same border cannot contain duplicate bridge");
  borderBridge.put("status","rubble");check(KingmakerStructureSemantics.isIsland(borderTown,new JSONArray().put(borderBridge)),"rubble bridge grants no island connection");
  boolean borderLandBridge=false;try{KingmakerStructureSemantics.requireBorderInfrastructure(new JSONObject().put("type","village"),new JSONArray(),"bridge",borderNorth);}catch(IllegalStateException expected){borderLandBridge=true;}check(borderLandBridge,"bridge rejected on default land border");
  KingmakerStructureSemantics.requireBorderInfrastructure(borderTown,new JSONArray(),"wall_wooden",borderNorth);check(true,"wall has chosen border");
  JSONObject borderWall=structure("wall_wooden",1).put("effects_json",new JSONObject().put("infrastructure_instances",KingmakerStructureSemantics.appendedInfrastructure(new JSONObject(),"wall_wooden",borderNorth)));
  boolean borderOverlap=false;try{KingmakerStructureSemantics.requireBorderInfrastructure(borderTown,new JSONArray().put(borderWall),"wall_stone",borderNorth);}catch(IllegalStateException expected){borderOverlap=true;}check(borderOverlap,"new stone wall cannot overlap wooden wall without conversion");
  KingmakerStructureSemantics.requireBorderInfrastructure(borderTown,new JSONArray().put(borderWall),"wall_stone",new JSONObject().put("urban_grid",1).put("border","east"));check(true,"different wall borders independent");
  check(KingmakerStructureSemantics.isInfrastructure("bridge")&&KingmakerStructureSemantics.isInfrastructure("wall_stone")&&KingmakerStructureSemantics.isInfrastructure("wall_wooden"),"all border infrastructures supported");
  check(KingmakerStructureSemantics.isIsland(new JSONObject(borderTown.toString()),new JSONArray()),"water topology survives serialization");
  JSONObject borderMetro=new JSONObject().put("type","metropolis").put("developed_blocks",10);
  borderMetro.put("grid_json",KingmakerStructureSemantics.declareWaterBorders(borderMetro,1,borderAllWater,true));
  check(!KingmakerStructureSemantics.isIsland(borderMetro,new JSONArray()),"land borders of second grid preserve land connection");
  borderMetro.put("grid_json",KingmakerStructureSemantics.declareWaterBorders(borderMetro,2,borderAllWater,true));
  check(KingmakerStructureSemantics.isIsland(borderMetro,new JSONArray()),"all metropolis grids with water only are island");

  JSONObject lotVillage=new JSONObject().put("type","village").put("developed_blocks",1).put("grid_json",new JSONObject());
  JSONObject lotOne=new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(1));
  JSONObject lotHouse=structure("houses",1).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(lotOne)));
  boolean lotDiagonal=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"academy",2,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(1).put(4)),"");}catch(IllegalArgumentException expected){lotDiagonal=true;}check(lotDiagonal,"two diagonally touching lots are not contiguous");
  JSONObject lotTwo=new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(3).put(4));
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotHouse),"academy",2,lotTwo,"");check(true,"two adjacent free lots in same block accepted");
  boolean lotOverlap=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotHouse),"library",1,lotOne,"");}catch(IllegalStateException expected){lotOverlap=true;}check(lotOverlap,"occupied lot cannot be built on twice");
  boolean lotFoundry=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotHouse),"foundry",2,lotTwo,"");}catch(IllegalStateException expected){lotFoundry=true;}check(lotFoundry,"foundry cannot share block with housing");
  boolean lotDump=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotHouse),"dump",1,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(3)),"");}catch(IllegalStateException expected){lotDump=true;}check(lotDump,"dump cannot share residential block");
  JSONObject lotTannery=structure("tannery",1).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(lotOne)));
  boolean lotLaterResidence=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotTannery),"inn",1,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(2)),"");}catch(IllegalStateException expected){lotLaterResidence=true;}check(lotLaterResidence,"later residence cannot bypass tannery restriction");
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotTannery),"tenement",1,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(2)),"");check(true,"tenement exception beside tannery allowed");
  boolean lotNoWater=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"pier",1,lotOne,"");}catch(IllegalStateException expected){lotNoWater=true;}check(lotNoWater,"pier requires actual adjacency to water border");
  lotVillage.put("grid_json",KingmakerStructureSemantics.declareWaterBorders(lotVillage,1,new JSONArray().put("north"),true));
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"lumberyard",2,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(1).put(2)),"");check(true,"lumberyard next to declared water border accepted");
  boolean lotInteriorWater=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"pier",1,new JSONObject().put("urban_grid",1).put("block",5).put("lots",new JSONArray().put(1)),"");}catch(IllegalStateException expected){lotInteriorWater=true;}check(lotInteriorWater,"interior lot not adjacent merely because settlement has water");
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"waterfront",4,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(1).put(2).put(3).put(4)),"");check(true,"four-lot waterfront occupies water-facing block");
  boolean lotLuxuryMissing=false;try{KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray(),"luxury_store",1,lotOne,"");}catch(IllegalStateException expected){lotLuxuryMissing=true;}check(lotLuxuryMissing,"luxury store requires mansion or noble villa in same block");
  JSONObject lotMansion=structure("mansion",1).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(lotOne)));
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotMansion),"luxury_store",1,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(2)),"");check(true,"luxury store beside mansion allowed");
  JSONObject lotTown=new JSONObject(lotVillage.toString()).put("type","town").put("developed_blocks",2);
  boolean lotTownDisconnected=false;try{KingmakerStructureSemantics.requireLotPlacement(lotTown,new JSONArray().put(lotHouse),"library",1,new JSONObject().put("urban_grid",1).put("block",9).put("lots",new JSONArray().put(1)),"");}catch(IllegalStateException expected){lotTownDisconnected=true;}check(lotTownDisconnected,"town blocks must be contiguous");
  KingmakerStructureSemantics.requireLotPlacement(lotTown,new JSONArray().put(lotHouse),"library",1,new JSONObject().put("urban_grid",1).put("block",2).put("lots",new JSONArray().put(1)),"");check(true,"adjacent town block accepted");
  boolean lotLegacyUnknown=false;try{KingmakerStructureSemantics.chooseLotPlacement(lotTown,new JSONArray().put(structure("houses",1).put("lots",1)),"library",1,null,"");}catch(IllegalStateException expected){lotLegacyUnknown=true;}check(lotLegacyUnknown,"legacy positions must be registered instead of guessed");
  JSONObject lotAuto=KingmakerStructureSemantics.chooseLotPlacement(lotVillage,new JSONArray().put(lotHouse),"library",1,null,"");
  check(lotAuto.optInt("block")==1&&lotAuto.optJSONArray("lots").optInt(0)==2,"automatic placement selects an actually free lot");
  JSONObject lotPending=structure("under_construction_library",1).put("lots",1).put("effects_json",new JSONObject().put("original_structure_key","library").put("build_options",new JSONObject().put("lot_placement",lotAuto)));
  check(KingmakerStructureSemantics.lotPlacements(lotPending).length()==1,"unfinished building reserves actual lots");
  KingmakerStructureSemantics.requireLotPlacement(lotVillage,new JSONArray().put(lotHouse).put(lotPending),"library",1,lotAuto,"under_construction_library");check(true,"resume ignores own reservation only");
  JSONObject lotRecorded=structure("library",2).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(lotOne).put(lotAuto)));
  check(KingmakerStructureSemantics.trimInstanceEffects(lotRecorded,1).optJSONArray("lot_instances").length()==1,"demolition releases last registered structure footprint");
  check(KingmakerStructureSemantics.sameLotPlacement(lotTwo,new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(4).put(3))),"same footprint independent of lot array ordering");

  check(KingmakerStructureSemantics.builtBlockCount(new JSONArray().put(lotHouse))==1,"one developed structure district needs one residential lot");
  check(KingmakerStructureSemantics.builtBlockCount(new JSONArray().put(lotPending))==0,"unfinished work alone is not a built residential district");
  check(KingmakerStructureSemantics.builtBlockCount(new JSONArray().put(structure("houses",1).put("lots",1)))==-1,"unknown legacy blocks remain explicitly unverified");
  JSONObject registeredMill=structure("mill",1).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(lotOne)));
  check(KingmakerStructureSemantics.hasWaterMill(lotVillage,new JSONArray().put(registeredMill)),"water-facing mill reduces consumption from recorded position");
  JSONObject inlandMill=structure("mill",1).put("lots",1).put("effects_json",new JSONObject().put("lot_instances",new JSONArray().put(new JSONObject(lotOne.toString()).put("block",5))));
  JSONObject oldMillFlagTown=new JSONObject(lotVillage.toString());JSONObject oldMillGrid=new JSONObject(oldMillFlagTown.opt("grid_json").toString()).put("water_mill",true);oldMillFlagTown.put("grid_json",oldMillGrid);
  check(!KingmakerStructureSemantics.hasWaterMill(oldMillFlagTown,new JSONArray().put(inlandMill)),"registered inland mill cannot claim old manual water flag");
  check(KingmakerStructureSemantics.hasWaterMill(oldMillFlagTown,new JSONArray().put(structure("mill",1).put("lots",1))),"legacy declared water mill preserved until placement registered");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_amiri","unstarted")),"retire fabricated q_companheiro_amiri");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_valerie","unstarted")),"retire fabricated q_companheiro_valerie");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_linzi","unstarted")),"retire fabricated q_companheiro_linzi");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_harrim","unstarted")),"retire fabricated q_companheiro_harrim");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_tristian","unstarted")),"retire fabricated q_companheiro_tristian");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_jaethal","unstarted")),"retire fabricated q_companheiro_jaethal");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_regongar","unstarted")),"retire fabricated q_companheiro_regongar");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_octavia","unstarted")),"retire fabricated q_companheiro_octavia");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_ekundayo","unstarted")),"retire fabricated q_companheiro_ekundayo");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_jubilost","unstarted")),"retire fabricated q_companheiro_jubilost");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_nok_nok","unstarted")),"retire fabricated q_companheiro_nok_nok");
  check(KingmakerCampaignGate.retiredQuest(quest("q_companheiro_irmas_tiefling","unstarted")),"retire fabricated q_companheiro_irmas_tiefling");
  JSONObject retiredCompleted=quest("q_companheiro_amiri","completed");
  KingmakerCampaignGate.requireTransition(retiredCompleted,"archived",new ArrayList<JSONObject>());
  check(true,"archive completed draft without erasing history");
  boolean retiredRejected=false;
  try{KingmakerCampaignGate.requireTransition(quest("q_companheiro_tristian","unstarted"),"completed",new ArrayList<JSONObject>());}catch(IllegalStateException expected){retiredRejected=true;}
  check(retiredRejected,"retired companion quest cannot grant its invented reward");
  check("O Chamado do Sangue".equals(KingmakerCompanionSemantics.personalQuestDefinition("amiri").optString("title")),"primary Amiri adventure is Blood Calling, not only foreshadowing");
  check(KingmakerCompanionSemantics.harrimCampingProtection(new JSONArray())==2,"Harrim protection raises actual encounter DC by two");
  boolean repeatedHarrim=false;
  try{KingmakerCompanionSemantics.harrimCampingProtection(new JSONArray().put(new JSONObject().put("companion_activity","harrim_blend_into_night")));}catch(IllegalStateException expected){repeatedHarrim=true;}
  check(repeatedHarrim,"same camp cannot stack repeated Harrim prayers");
  check(KingmakerCompanionSemantics.harrimCampingProtection(new JSONArray().put(new JSONObject().put("activity","Vigiar")))==2,"ordinary watch does not consume Harrim protection");
  JSONArray harrimAtCamp=new JSONArray().put(new JSONObject().put("companion_activity","harrim_blend_into_night").put("actor_companion_key","harrim").put("encounter_dc_bonus",2));
  check(KingmakerCompanionSemantics.harrimEncounterDcBonus(harrimAtCamp,"Noite")==2,"Harrim protection applies to night encounter check");
  check(KingmakerCompanionSemantics.harrimEncounterDcBonus(harrimAtCamp,"Madrugada")==2,"Harrim protects the pre-dawn night");
  check(KingmakerCompanionSemantics.harrimEncounterDcBonus(harrimAtCamp,"Tarde")==0,"Harrim protection does not reduce daytime encounters");
  check(KingmakerCompanionSemantics.harrimEncounterDcBonus(new JSONArray(),"Noite")==0,"Harrim not invoked cannot grant protection");
  JSONArray demoFour=new JSONArray().put(1).put(2).put(3).put(4);
  JSONObject demoSingle=KingmakerStructureSemantics.demolitionOutcome(demoFour,2,"SUCCESS",false,"",1);
  check(demoSingle.optJSONArray("remaining_rubble_lots").length()==3&&demoSingle.optJSONArray("cleared_lots").length()==1,"demolition success clears one physical lot, not the whole building");
  check(demoSingle.optBoolean("original_structure_disabled"),"demolition immediately disables every original building effect");
  JSONObject demoCritical=KingmakerStructureSemantics.demolitionOutcome(demoFour,2,"CRITICAL_SUCCESS",true,"",1);
  check(demoCritical.optJSONArray("remaining_rubble_lots").length()==0&&demoCritical.optJSONArray("cleared_lots").length()==4,"critical demolition may clear the entire footprint");
  check(demoCritical.optInt("recovered")==0,"whole structure critical choice does not also salvage commodities");
  JSONObject demoSalvage=KingmakerStructureSemantics.demolitionOutcome(demoFour,3,"CRITICAL_SUCCESS",false,"stone",6);
  check(demoSalvage.optInt("recovered")==6&&demoSalvage.optJSONArray("remaining_rubble_lots").length()==3,"single lot critical choice recovers chosen real d6 commodities");
  JSONObject demoFailed=KingmakerStructureSemantics.demolitionOutcome(demoFour,1,"FAILURE",true,"",1);
  check(demoFailed.optJSONArray("remaining_rubble_lots").length()==4&&demoFailed.optInt("unrest")==0,"failure leaves every lot in rubble");
  JSONObject demoCriticalFailed=KingmakerStructureSemantics.demolitionOutcome(demoFour,1,"CRITICAL_FAILURE",false,"",1);
  check(demoCriticalFailed.optInt("unrest")==1&&demoCriticalFailed.optJSONArray("remaining_rubble_lots").length()==4,"critical failure adds unrest and retains rubble");
  boolean invalidDemoLot=false;
  try{KingmakerStructureSemantics.demolitionOutcome(new JSONArray().put(1),2,"SUCCESS",false,"",1);}catch(IllegalArgumentException expected){invalidDemoLot=true;}
  check(invalidDemoLot,"cannot demolish a lot outside this building");
  boolean invalidRecovery=false;
  try{KingmakerStructureSemantics.demolitionOutcome(demoFour,1,"CRITICAL_SUCCESS",false,"food",2);}catch(IllegalArgumentException expected){invalidRecovery=true;}
  check(invalidRecovery,"salvage never fabricates food or luxuries");
  check(KingmakerCompanionSemantics.loggedSleepingHours(new JSONArray())==0,"empty camp does not prove completed sleep");
  JSONArray sleepJournal=new JSONArray().put(new JSONObject().put("activity","Descansar").put("hour",1)).put(new JSONObject().put("activity","Descansar").put("hour",1)).put(new JSONObject().put("activity","Vigiar").put("hour",2)).put(new JSONObject().put("activity","Descansar").put("hour",3));
  check(KingmakerCompanionSemantics.loggedSleepingHours(sleepJournal)==2,"sleep journal counts distinct recorded rest hours");
  check(KingmakerCompanionSemantics.loggedSleepingHours(new JSONArray().put(new JSONObject().put("activity","Descansar")))==0,"narrative without a recorded hour cannot certify rest");
  JSONObject growthVillage=new JSONObject().put("type","village").put("developed_blocks",1).put("kingdom_level",3);
  JSONArray growthFull=new JSONArray().put(growthRow("houses",1,1,1,2,3,4));
  JSONObject growthNext=new JSONObject().put("urban_grid",1).put("block",2).put("lots",new JSONArray().put(1));
  check("town".equals(KingmakerStructureSemantics.constructionTown(growthVillage,growthFull,growthNext).optString("type")),"full village grows by construction in adjacent block");
  KingmakerStructureSemantics.requireLotPlacement(growthVillage,growthFull,"houses",1,growthNext,"");check(true,"new adjacent town lot passes placement without administrative promotion");
  check("village".equals(KingmakerStructureSemantics.constructionTown(new JSONObject(growthVillage.toString()).put("kingdom_level",2),growthFull,growthNext).optString("type")),"kingdom level two cannot grow a town");
  check("village".equals(KingmakerStructureSemantics.constructionTown(growthVillage,new JSONArray().put(growthRow("houses",1,1,1,2,3)),growthNext).optString("type")),"village needs all four actual completed lots");
  check("village".equals(KingmakerStructureSemantics.constructionTown(growthVillage,new JSONArray().put(growthRow("library",1,1,1,2,3,4)),growthNext).optString("type")),"overcrowded village cannot expand");
  check("village".equals(KingmakerStructureSemantics.constructionTown(growthVillage,growthFull,new JSONObject(growthNext.toString()).put("block",9)).optString("type")),"village expansion must touch its actual block");
  JSONObject growthTown=new JSONObject().put("type","town").put("developed_blocks",4).put("kingdom_level",9);
  JSONArray growthFour=new JSONArray();for(int block:new int[]{1,2,4,5})growthFour.put(growthRow("houses",1,block,1,2));
  JSONObject growthFifth=new JSONObject(growthNext.toString()).put("block",9);
  check("city".equals(KingmakerStructureSemantics.constructionTown(growthTown,growthFour,growthFifth).optString("type")),"four connected town blocks each containing two completed lots grow city on new block");
  check(KingmakerStructureSemantics.constructionTown(growthTown,growthFour,growthFifth).optInt("developed_blocks")==9,"city allocates the full urban grid");
  JSONArray growthUneven=new JSONArray().put(growthRow("houses",1,1,1,2,3,4)).put(growthRow("houses",1,2,1,2)).put(growthRow("houses",1,4,1)).put(growthRow("houses",1,5,1));
  check("town".equals(KingmakerStructureSemantics.constructionTown(growthTown,growthUneven,growthFifth).optString("type")),"eight total lots cannot replace two in every block");
  JSONArray growthDisconnected=new JSONArray();for(int block:new int[]{1,3,7,9})growthDisconnected.put(growthRow("houses",1,block,1,2));
  check("town".equals(KingmakerStructureSemantics.constructionTown(growthTown,growthDisconnected,new JSONObject(growthNext.toString()).put("block",5)).optString("type")),"disconnected old town blocks cannot certify city promotion");
  JSONObject growthCity=new JSONObject().put("type","city").put("developed_blocks",9).put("kingdom_level",15);
  JSONArray growthNine=new JSONArray();for(int block=1;block<=9;block++)growthNine.put(growthRow("houses",1,block,1,2));
  JSONObject growthGridTwo=new JSONObject(growthNext.toString()).put("urban_grid",2).put("block",1);
  check("metropolis".equals(KingmakerStructureSemantics.constructionTown(growthCity,growthNine,growthGridTwo).optString("type")),"nine blocks each with two completed lots allow second grid");
  check(KingmakerStructureSemantics.constructionTown(growthCity,growthNine,growthGridTwo).optInt("developed_blocks")==18,"second grid has nine allocated blocks");
  check("city".equals(KingmakerStructureSemantics.constructionTown(new JSONObject(growthCity.toString()).put("kingdom_level",14),growthNine,growthGridTwo).optString("type")),"metropolis requires kingdom level fifteen");
  JSONArray growthPending=new JSONArray();for(int block=1;block<=9;block++)growthPending.put(growthRow(block==9?"under_construction_houses":"houses",1,block,1,2));
  check("city".equals(KingmakerStructureSemantics.constructionTown(growthCity,growthPending,growthGridTwo).optString("type")),"unfinished lots cannot qualify for a new grid");
  JSONObject growthMetro=new JSONObject().put("type","metropolis").put("developed_blocks",18).put("kingdom_level",15);
  JSONArray growthEighteen=new JSONArray();for(int grid=1;grid<=2;grid++)for(int block=1;block<=9;block++)growthEighteen.put(growthRow("houses",grid,block,1,2));
  check(KingmakerStructureSemantics.constructionTown(growthMetro,growthEighteen,new JSONObject(growthGridTwo.toString()).put("urban_grid",3)).optInt("developed_blocks")==27,"every available block must qualify before adding third grid");
  check(KingmakerStructureSemantics.constructionTown(growthMetro,growthNine,new JSONObject(growthGridTwo.toString()).put("urban_grid",3)).optInt("developed_blocks")==18,"half-empty existing metropolis cannot allocate another grid");
  JSONObject paid=new JSONObject().put("resource_points",10).put("lumber",3).put("stone",0).put("ore",0).put("luxuries",0).put("authority","paid");
  JSONArray receipts=KingmakerStructureSemantics.appendedConstructionPayment(new JSONObject().put("count",2),paid);
  check(receipts.length()==3,"legacy payment placeholders preserve physical instance indexing");
  check("unknown".equals(receipts.optJSONObject(0).optString("authority")),"legacy catalog costs are not payment evidence");
  check(receipts.optJSONObject(2).optInt("lumber")==3,"net paid commodity amount survives recording");
  JSONObject paidRow=new JSONObject().put("count",3).put("effects_json",new JSONObject().put("construction_payments",receipts).toString());
  check(KingmakerStructureSemantics.trimInstanceEffects(paidRow,2).optJSONArray("construction_payments").length()==2,"demolition trims the matching last receipt");
  boolean badReceipt=false;try{KingmakerStructureSemantics.appendedConstructionPayment(new JSONObject(),new JSONObject(paid.toString()).put("ore",-1));}catch(IllegalArgumentException expected){badReceipt=true;}
  check(badReceipt,"negative actual payment cannot become upgrade credit");
  check(KingmakerStructureSemantics.canUpgrade("luxury_store","magic_shop"),"printed luxury store to magic shop upgrade retained");
  check(!KingmakerStructureSemantics.canUpgrade("watchtower","keep"),"unprinted watchtower to keep upgrade rejected");
  JSONObject academyPaid=new JSONObject(paid.toString()).put("resource_points",52).put("lumber",12).put("stone",12).put("luxuries",6);
  JSONObject academyRow=new JSONObject().put("structure_key","academy").put("count",1).put("effects_json",new JSONObject().put("construction_payments",new JSONArray().put(academyPaid)).toString());
  JSONObject military=new JSONObject().put("key","military_academy").put("rp",36).put("lumber",12).put("stone",6);
  check(KingmakerStructureSemantics.upgradeCost(academyRow,0,military).optInt("resource_points")==-16,"printed upgrade subtraction preserves negative RP difference");
  boolean unknownUpgrade=false;try{KingmakerStructureSemantics.upgradeCost(new JSONObject().put("structure_key","academy"),0,military);}catch(IllegalStateException expected){unknownUpgrade=true;}
  check(unknownUpgrade,"unknown old payment cannot be replaced by catalog price");
  JSONObject originalFootprint=new JSONObject().put("urban_grid",1).put("block",1).put("lots",new JSONArray().put(1));
  JSONObject upgradeTown=new JSONObject().put("type","village").put("developed_blocks",1);
  JSONObject selectedUpgrade=KingmakerStructureSemantics.upgradePlacement(upgradeTown,new JSONArray(),"academy",2,originalFootprint,null);
  check(selectedUpgrade.optInt("block")==1,"upgrade stays in the original block");
  check(KingmakerStructureSemantics.additionalUpgradeLots(originalFootprint,selectedUpgrade).length()==1,"upgrade reserves only additional lots");
  boolean movedUpgrade=false;try{KingmakerStructureSemantics.upgradePlacement(upgradeTown,new JSONArray(),"academy",2,originalFootprint,new JSONObject().put("urban_grid",1).put("block",2));}catch(IllegalArgumentException expected){movedUpgrade=true;}check(movedUpgrade,"upgrade cannot relocate to another block");
  boolean replacedOriginal=false;try{KingmakerStructureSemantics.upgradePlacement(upgradeTown,new JSONArray(),"academy",2,originalFootprint,new JSONObject().put("lots",new JSONArray().put(3).put(4)));}catch(IllegalArgumentException expected){replacedOriginal=true;}check(replacedOriginal,"upgrade must retain every original lot");
  JSONArray blockedUpgrade=new JSONArray().put(growthRow("houses",1,1,2,4));
  JSONObject remainingUpgrade=KingmakerStructureSemantics.upgradePlacement(upgradeTown,blockedUpgrade,"academy",2,originalFootprint,null);
  check(remainingUpgrade.optJSONArray("lots").optInt(1)==3,"upgrade chooses actual free adjacent lot");
  boolean noRoom=false;try{KingmakerStructureSemantics.upgradePlacement(upgradeTown,blockedUpgrade,"cathedral",4,originalFootprint,null);}catch(IllegalStateException expected){noRoom=true;}check(noRoom,"four lot upgrade fails when its block has another building");
  JSONArray filteredUpgrade=KingmakerStructureSemantics.withoutLastInstance(new JSONArray().put(academyRow),"academy","pending_upgrade");check(filteredUpgrade.length()==0,"placement removes only selected old instance from simulated occupancy");
  JSONObject journalQuest=new JSONObject().put("chapter_key","chapter_00_call_for_heroes").put("quest_key","q_journal_test").put("status","active").put("progress_json",new JSONObject().put("objectives",new JSONArray().put(new JSONObject().put("id","first").put("description","Resolver a cena").put("completed",false)).put(new JSONObject().put("id","second").put("description","Escolher o desfecho").put("completed",false))).toString());
  check(!KingmakerCampaignGate.objectivesResolved(journalQuest),"unresolved objectives cannot certify quest completion");
  JSONObject journalProgress=KingmakerCampaignGate.resolveObjective(journalQuest,"first",false,"Cena resolvida com os personagens");journalQuest.put("progress_json",journalProgress.toString());
  check(!KingmakerCampaignGate.objectivesResolved(journalQuest),"one resolved objective does not complete the entire quest");
  journalProgress=KingmakerCampaignGate.resolveObjective(journalQuest,"second",true,"A decisão do grupo dispensou esta alternativa");journalQuest.put("progress_json",journalProgress.toString());
  check(KingmakerCampaignGate.objectivesResolved(journalQuest),"documented branch resolution counts without inventing an encounter");
  check(journalProgress.optJSONArray("objectives").optJSONObject(1).optString("resolution_notes").contains("decisão"),"branch evidence remains in saved quest progress");
  boolean inventedObjective=false;try{KingmakerCampaignGate.resolveObjective(journalQuest,"fake",false,"Texto suficientemente longo");}catch(IllegalArgumentException expected){inventedObjective=true;}check(inventedObjective,"unknown objective IDs are rejected");
  boolean missingEvidence=false;try{KingmakerCampaignGate.resolveObjective(journalQuest,"first",true,"");}catch(IllegalArgumentException expected){missingEvidence=true;}check(missingEvidence,"branch waivers require evidence");
  JSONObject protectedJournal=KingmakerCampaignGate.preserveDefinition(journalQuest,new JSONObject().put("progress_json",new JSONObject().put("objectives",new JSONArray()).toString()));
  check(KingmakerCampaignGate.objectives(protectedJournal).length()==2,"model updates cannot erase the authoritative objective list");
  JSONObject untouchedCompanion=new JSONObject().put("id",10).put("companion_key","amiri").put("influence",0).put("npc_id",0).put("available",1).put("assignment","reserve").put("notes","Companheiro oficial das Terras Roubadas.").put("unlocked_rewards_json","[]");
  JSONObject oldAlias=new JSONObject().put("id",20).put("companion_key","npc_amiri").put("influence",8).put("npc_id",50).put("available",1).put("assignment","active").put("notes","Histórico real da campanha").put("unlocked_rewards_json","[\"amiri:old_reward\"]");
  JSONObject mergedAlias=KingmakerCompanionSemantics.mergeLegacyAlias(untouchedCompanion,oldAlias);
  check(mergedAlias.optInt("influence")==8&&mergedAlias.optInt("npc_id")==50,"legacy companion progress replaces only an untouched default");
  check(mergedAlias.optLong("id")==10&&"amiri".equals(mergedAlias.optString("companion_key")),"alias merge preserves canonical identity and leader references");
  JSONObject newerCompanion=new JSONObject(untouchedCompanion.toString()).put("influence",2).put("notes","Decisão mais recente reduziu a influência");
  check(KingmakerCompanionSemantics.mergeLegacyAlias(newerCompanion,oldAlias).optInt("influence")==2,"older high influence cannot overwrite a newer canonical decision");
  JSONObject narratorQuest=new JSONObject(journalQuest.toString());JSONObject resetProgress=new JSONObject(narratorQuest.optString("progress_json"));JSONArray resetObjectives=resetProgress.optJSONArray("objectives");for(int n=0;n<resetObjectives.length();n++)resetObjectives.optJSONObject(n).put("completed",false).put("status","pending");narratorQuest.put("progress_json",resetProgress.toString());
  JSONArray narratorResolutions=new JSONArray().put(new JSONObject().put("id","first").put("resolution_notes","A cena acabou com o resgate confirmado")).put(new JSONObject().put("id","second").put("waived",true).put("resolution_notes","O acordo evitou o combate desta alternativa"));
  JSONObject narratorChange=KingmakerCampaignGate.preserveDefinition(narratorQuest,new JSONObject().put("objective_resolutions",narratorResolutions).put("status","completed"));
  check(KingmakerCampaignGate.objectivesResolved(KingmakerCampaignGate.transitionState(narratorQuest,narratorChange)),"narrator can resolve known objectives and finish in one validated update");
  check(KingmakerCampaignGate.objectives(narratorQuest).optJSONObject(0).optBoolean("completed")==false,"narrator resolution cannot mutate the original saved snapshot");
  boolean narratorInvented=false;try{KingmakerCampaignGate.preserveDefinition(narratorQuest,new JSONObject().put("objective_resolutions",new JSONArray().put(new JSONObject().put("id","invented").put("resolution_notes","Sem fonte real no livro"))));}catch(IllegalArgumentException expected){narratorInvented=true;}check(narratorInvented,"narrator cannot append invented campaign objectives");
  JSONObject publishedCompanionCandidate=new JSONObject().put("quest_key","q_cg_amiri").put("chapter_key","chapter_12_companion_stories").put("status","unstarted");
  check(KingmakerCampaignGate.playable(java.util.Arrays.asList(publishedCompanionCandidate),java.util.Arrays.asList(publishedCompanionCandidate)).size()==1,"published companion adventures enter the solo director candidate list");
  JSONObject automaticActivation=KingmakerCampaignGate.preserveDefinition(narratorQuest,new JSONObject().put("objective_resolutions",new JSONArray().put(new JSONObject().put("id","first").put("resolution_notes","O grupo resolveu esta cena"))));
  check("active".equals(automaticActivation.optString("status")),"first narrated objective activates the quest automatically");
  JSONObject openingEncounter=KingmakerCampaignGate.mansionEncounter("A1");JSONObject initialCutthroats=openingEncounter.optJSONArray("enemies").optJSONObject(0);
  check(initialCutthroats.optInt("count")==3,"opening room has three printed cutthroats");
  check(initialCutthroats.optInt("hp")==8&&initialCutthroats.optInt("ac")==15,"opening cutthroats use printed HP and AC");
  check(initialCutthroats.optJSONObject("official_stats").optInt("level")==-1,"negative creature level is preserved");
  check(initialCutthroats.optInt("initiative_bonus")==5,"ambush initiative uses Stealth");
  check(KingmakerCampaignGate.mansionEncounter("A4").optJSONArray("enemies").optJSONObject(0).optInt("count")==4,"water garden has four cutthroats");
  JSONObject gromogCreature=KingmakerCampaignGate.mansionCreature("gromog");check("blinded".equals(gromogCreature.optString("conditions")),"Gromog starts blinded");
  check(!gromogCreature.optJSONObject("official_stats").optBoolean("deadly_trait"),"Gromog's itchy ogre hook has no deadly trait");
  JSONObject jurgrindorCreature=KingmakerCampaignGate.mansionCreature("jurgrindor");check(jurgrindorCreature.optInt("hp")==25&&jurgrindorCreature.optInt("max_hp")==50,"Jurgrindor starts at twenty five of fifty HP");
  check(jurgrindorCreature.optJSONObject("official_stats").optInt("chill_breath_reflex_dc")==18,"Jurgrindor breath uses printed Reflex DC");
  JSONObject nishkivCreature=KingmakerCampaignGate.mansionCreature("nishkiv");check("1d6".equals(nishkivCreature.optJSONObject("official_stats").optString("sneak_attack_damage")),"Nishkiv has printed precision damage");
  check(KingmakerCampaignGate.mansionEncounter("A21").optJSONArray("enemies").length()==2,"parlor preserves both printed enemy types");
  JSONObject finalMansion=KingmakerCampaignGate.mansionEncounter("A23");check(finalMansion.optBoolean("frost_giants_fought_by_jamandi"),"Jamandi's frost giants are not added to the PC encounter");
  check(finalMansion.optInt("statblock_cutthroats")==2&&finalMansion.optInt("narrative_cutthroats")==4,"contradictory printed counts remain explicit rather than silently blended");
  boolean inventedRoom=false;try{KingmakerCampaignGate.mansionEncounter("A99");}catch(IllegalArgumentException expected){inventedRoom=true;}check(inventedRoom,"unknown mansion rooms cannot create invented battles");
  JSONObject restingHero=new JSONObject().put("hp",10).put("max_hp",30).put("level",5).put("sheet_json","{}");
  JSONObject naturalRecovery=KingmakerCompanionSemantics.naturalRestRecovery(restingHero,3,new JSONObject(),50);
  check(naturalRecovery.optInt("after_hp")==25&&naturalRecovery.optInt("healed")==15,"verified rest heals Constitution modifier times actual hero level");
  check(KingmakerCompanionSemantics.naturalRestRecovery(restingHero,-2,new JSONObject(),50).optInt("healed")==5,"negative Constitution still uses minimum one per level");
  check(KingmakerCompanionSemantics.naturalRestRecovery(new JSONObject(restingHero.toString()).put("hp",29),3,new JSONObject(),50).optInt("after_hp")==30,"natural rest never exceeds maximum HP");
  check(!KingmakerCompanionSemantics.naturalRestRecovery(restingHero,3,new JSONObject().put("rest_end_hours",45),50).optBoolean("eligible"),"a second camp cannot grant rest benefits within twenty four hours");
  check(KingmakerCompanionSemantics.naturalRestRecovery(restingHero,3,new JSONObject().put("rest_end_hours",26),50).optBoolean("eligible"),"exactly twenty four hours permits the next recovery");
  check(!KingmakerCompanionSemantics.naturalRestRecovery(new JSONObject(restingHero.toString()).put("sheet_json",new JSONObject().put("dead",true).toString()),3,new JSONObject(),50).optBoolean("eligible"),"rest cannot resurrect a dead hero");
  JSONArray fireDice=new JSONArray().put(5).put(4);
  JSONObject flames=new JSONObject();
  flames=KingmakerCampaignGate.manorFireAction(flames,1,"fight_fire","CRITICAL_SUCCESS",30,1,false,fireDice,2);
  check(flames.optJSONArray("rooms").optJSONObject(0).optInt("points")==2,"critical success grants two extinguish points");
  flames=KingmakerCampaignGate.manorFireAction(flames,1,"fight_fire","CRITICAL_FAILURE",30,1,false,fireDice,2);
  check(flames.optJSONArray("rooms").optJSONObject(0).optInt("points")==1,"critical failure removes one extinguish point");
  flames=KingmakerCampaignGate.manorFireAction(flames,2,"fight_fire","CRITICAL_FAILURE",30,1,false,fireDice,2);
  check(flames.optJSONArray("rooms").optJSONObject(1).optInt("points")==0,"extinguish points cannot become negative");
  for(int room=1;room<=3;room++)flames=KingmakerCampaignGate.manorFireAction(flames,room,"break_window","",30,1,false,fireDice,2);
  check(KingmakerCampaignGate.manorSmokeDc(flames)==11,"three different windows reduce smoke DC to eleven");
  JSONObject flare=KingmakerCampaignGate.manorFireAction(flames,1,"fire_damage","FAILURE",30,1,false,fireDice,2);
  check(flare.optJSONObject("last_outcome").optInt("damage")==9,"broken window adds second fire die during same round");
  flare=KingmakerCampaignGate.manorFireAction(flames,1,"fire_damage","FAILURE",30,2,false,fireDice,2);
  check(flare.optJSONObject("last_outcome").optInt("damage")==5,"window flare does not persist to later rounds");
  flare=KingmakerCampaignGate.manorFireAction(flames,1,"fire_damage","SUCCESS",30,2,false,fireDice,2);
  check(flare.optJSONObject("last_outcome").optInt("damage")==2,"basic Reflex halves and rounds damage down");
  JSONObject smoke=KingmakerCampaignGate.manorFireAction(flames,1,"smoke","CRITICAL_FAILURE",30,2,false,fireDice,2);
  check(smoke.optJSONObject("last_outcome").optInt("sickened")==2&&smoke.optJSONObject("last_outcome").optBoolean("persistent_nonlethal"),"critical smoke failure applies sickened two and persistent nonlethal");
  smoke=KingmakerCampaignGate.manorFireAction(flames,1,"smoke","FAILURE",30,2,true,fireDice,2);
  check(smoke.optJSONObject("last_outcome").optInt("damage")==0,"holding breath suppresses smoke trigger");
  for(int room=1;room<=5;room++)while(flames.optJSONArray("rooms").optJSONObject(room-1).optInt("points")<4)flames=KingmakerCampaignGate.manorFireAction(flames,room,"fight_fire","CRITICAL_SUCCESS",30,2,false,fireDice,2);
  check(flames.optBoolean("all_extinguished")&&Math.abs(flames.optDouble("smoke_clears_hours")-(30+1d/6d))<0.00001,"five fires extinguished with three windows clears smoke after ten minutes");
  smoke=KingmakerCampaignGate.manorFireAction(flames,1,"smoke","FAILURE",30.1,3,false,fireDice,2);
  check(smoke.optJSONObject("last_outcome").optInt("damage")==5,"extinguishing fires does not clear smoke immediately");
  smoke=KingmakerCampaignGate.manorFireAction(flames,1,"smoke","FAILURE",30.2,3,false,fireDice,2);
  check(smoke.optJSONObject("last_outcome").optInt("damage")==0,"smoke stops after ventilation timer");
  JSONObject burnout=KingmakerCampaignGate.manorFireAction(new JSONObject().put("started_hours",30),1,"fire_damage","FAILURE",30.5,3,false,fireDice,2);
  check(burnout.optBoolean("naturally_burned_out")&&!burnout.optBoolean("all_extinguished")&&burnout.optJSONObject("last_outcome").optInt("damage")==0,"natural burnout is separate from earned extinguish award");
  JSONObject breathing=new JSONObject().put("manor_suffocation_starts_round",5).put("unconscious",true);
  boolean tooEarly=false;try{KingmakerCampaignGate.manorSuffocation(breathing,4,"FAILURE",new JSONArray().put(7),false,0);}catch(IllegalArgumentException e){tooEarly=true;}check(tooEarly,"smoke unconsciousness delay must expire before suffocation");
  breathing=KingmakerCampaignGate.manorSuffocation(breathing,5,"FAILURE",new JSONArray().put(7),false,0);
  check(breathing.optJSONObject("last_breath_outcome").optInt("damage")==7&&breathing.optJSONObject("last_breath_outcome").optInt("dc")==20,"first suffocation check uses DC twenty and one d10");
  boolean duplicateBreath=false;try{KingmakerCampaignGate.manorSuffocation(breathing,5,"FAILURE",new JSONArray().put(3).put(4),false,0);}catch(IllegalArgumentException e){duplicateBreath=true;}check(duplicateBreath,"suffocation cannot be resolved twice in the same round");
  breathing=KingmakerCampaignGate.manorSuffocation(breathing,6,"SUCCESS",null,false,0);
  check(breathing.optJSONObject("last_breath_outcome").optInt("dc")==25&&breathing.optJSONObject("last_breath_outcome").optInt("damage")==0,"successful check still increases next suffocation DC");
  breathing=KingmakerCampaignGate.manorSuffocation(breathing,7,"FAILURE",new JSONArray().put(2).put(3).put(4),false,0);
  check(breathing.optJSONObject("last_breath_outcome").optInt("damage")==9&&breathing.optJSONObject("last_breath_outcome").optInt("dc")==30,"suffocation damage dice and DC accumulate across successful checks");
  JSONObject breathingDeath=KingmakerCampaignGate.manorSuffocation(breathing,8,"CRITICAL_FAILURE",null,false,0);
  check(breathingDeath.optBoolean("dead")&&breathingDeath.optInt("dying")==0,"critical suffocation failure kills without inventing dying damage");
  JSONObject rescued=KingmakerCampaignGate.manorSuffocation(breathing,8,"",null,true,4);
  check(!rescued.has("manor_suffocation_starts_round")&&!rescued.optBoolean("unconscious"),"fresh air ends suffocation and consciousness returns above zero HP");
  rescued=KingmakerCampaignGate.manorSuffocation(breathing,8,"",null,true,0);
  check(rescued.optBoolean("unconscious"),"fresh air does not wake a character with zero HP");
  JSONObject treasury=KingmakerCampaignGate.manorTreasuryOutcome(new JSONObject(),"search_a4","FAILURE");
  check(!treasury.optBoolean("a4_discovered"),"failed search cannot invent garden compartment");
  treasury=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"search_a4","SUCCESS");treasury=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"loot_a4","");
  check(treasury.optBoolean("has_a7_key")&&KingmakerCampaignGate.manorTreasureDefinition("A4").optJSONArray("items").optJSONObject(0).optInt("quantity")==4,"garden stash preserves four minor potions and actual bronze key");
  boolean repeatedLoot=false;try{KingmakerCampaignGate.manorTreasuryOutcome(treasury,"loot_a4","");}catch(IllegalArgumentException e){repeatedLoot=true;}check(repeatedLoot,"garden loot cannot be duplicated");
  JSONObject lock=KingmakerCampaignGate.manorTreasuryOutcome(new JSONObject(),"pick_lock","CRITICAL_SUCCESS");
  check(lock.optInt("lock_successes")==2&&!lock.optBoolean("a7_unlocked"),"critical success alone cannot open three success lock");
  lock=KingmakerCampaignGate.manorTreasuryOutcome(lock,"pick_lock","CRITICAL_FAILURE");
  check(lock.optJSONObject("last_treasure_outcome").optBoolean("breaks_toolkit")&&lock.optInt("lock_successes")==2,"critical lock failure breaks tools without erasing prior successes");
  lock=KingmakerCampaignGate.manorTreasuryOutcome(lock,"pick_lock","SUCCESS");
  check(lock.optBoolean("a7_unlocked"),"third lock success opens chest");
  treasury=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"unlock_key","");treasury=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"loot_a7","");
  check(treasury.optInt("gold_held")==800&&treasury.optJSONObject("last_treasure_outcome").optInt("gold_delta")==800,"secondary treasury grants eight bags of one hundred gold exactly once");
  JSONObject treasure=KingmakerCampaignGate.manorTreasureDefinition("A7");check(treasure.optJSONArray("items").optJSONObject(0).optInt("quantity")==2&&treasure.optJSONArray("items").optJSONObject(0).optString("name").contains("lesser"),"secondary treasury keeps lesser healing potions separate from garden minor potions");
  repeatedLoot=false;try{KingmakerCampaignGate.manorTreasuryOutcome(treasury,"loot_a7","");}catch(IllegalArgumentException e){repeatedLoot=true;}check(repeatedLoot,"secondary treasury cannot create gold twice");
  JSONObject dishonest=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"lie","SUCCESS");
  check(!dishonest.optBoolean("gold_demanded"),"successful lie can convince Jamandi of escaped bandit");
  dishonest=KingmakerCampaignGate.manorTreasuryOutcome(dishonest,"weapons_seen","");check(dishonest.optBoolean("gold_demanded"),"recognized weapons invalidate otherwise successful lie");
  dishonest=KingmakerCampaignGate.manorTreasuryOutcome(dishonest,"refuse","");check(dishonest.optInt("gold_held")==800&&dishonest.optBoolean("future_tariff_discretionary"),"refusal keeps treasure and records only discretionary future tariff");
  JSONObject honest=KingmakerCampaignGate.manorTreasuryOutcome(treasury,"admit_return","");check(honest.optJSONObject("last_treasure_outcome").optInt("gold_delta")==-800&&honest.optString("jamandi_resolution").equals("honesty_reward"),"honesty returns actual gold while preserving permitted weapons and remaining potions");
  boolean repeatedReturn=false;try{KingmakerCampaignGate.manorTreasuryOutcome(honest,"return_gold","");}catch(IllegalArgumentException e){repeatedReturn=true;}check(repeatedReturn,"gold cannot be debited twice by treasury return");
  check(KingmakerLeadershipSemantics.skillCircumstance("emissary","intrigue")==2,"Intrigue grants +2 to Emissary");
  check(KingmakerLeadershipSemantics.skillCircumstance("treasurer","intrigue")==2,"Intrigue grants +2 to Treasurer");
  check(KingmakerLeadershipSemantics.skillCircumstance("counselor","politics")==2,"Politics grants +2 to Counselor");
  check(KingmakerLeadershipSemantics.skillCircumstance("ruler","politics")==-2,"Ruler has net -2 with Politics (+2 minus 4)");
  check(KingmakerLeadershipSemantics.skillCircumstance("ruler","warfare")==-4,"Ruler has -4 with nonpreferred skill");
  check(KingmakerLeadershipSemantics.skillCircumstance("magister","statecraft")==2,"Statecraft grants +2 to Magister");
  check(KingmakerLeadershipSemantics.skillCircumstance("viceroy","statecraft")==2,"Statecraft grants +2 to Viceroy");
  check(KingmakerLeadershipSemantics.skillCircumstance("general","warfare")==2,"Warfare grants +2 to General");
  check(KingmakerLeadershipSemantics.skillCircumstance("warden","warfare")==2,"Warfare grants +2 to Warden");
  check(KingmakerLeadershipSemantics.improved("CRITICAL_FAILURE",true).equals("FAILURE"),"Preferred companion improves critical failure one degree");
  check(KingmakerLeadershipSemantics.improved("FAILURE",true).equals("SUCCESS"),"Preferred companion improves failure one degree");
  check(KingmakerLeadershipSemantics.improved("SUCCESS",true).equals("CRITICAL_SUCCESS"),"Preferred companion improves success one degree");
  check(KingmakerLeadershipSemantics.improved("CRITICAL_SUCCESS",true).equals("CRITICAL_SUCCESS"),"Preferred companion cannot exceed critical success");
  check(KingmakerLeadershipSemantics.appointment("SUCCESS","emissary",6,true,true,3).optInt("new_leadership_unrest")==3,"Famous Jaethal adds 1d3 Unrest");
  check(KingmakerLeadershipSemantics.appointment("SUCCESS","emissary",6,true,false,0).optInt("new_leadership_unrest")==0,"Infamous Jaethal causes no Unrest");
  check(KingmakerLeadershipSemantics.appointment("CRITICAL_FAILURE","emissary",6,true,true,2).optInt("new_leadership_unrest")==1,"Rejected Jaethal causes only base critical failure Unrest");
  check(KingmakerLeadershipSemantics.appointment("FAILURE","ruler",6,false,false,0).optInt("new_leadership_unrest")==1,"Noncritical Ruler assignment adds one Unrest");
  check(KingmakerLeadershipSemantics.appointment("CRITICAL_FAILURE","ruler",6,false,false,0).optInt("new_leadership_unrest")==2,"Critical failed Ruler appointment causes ruler and rejection Unrest");
  JSONObject poor=KingmakerLeadershipSemantics.appointment("FAILURE","magister",4,false,false,0);
  check(KingmakerLeadershipSemantics.leaderActivityModifier(poor,100)==-1,"Failure penalty persists until successful acceptance");
  check(KingmakerLeadershipSemantics.leaderActivityModifier(poor,100,"ACTIVITY")==-1,"Failure penalty applies in Activity phase");
  check(KingmakerLeadershipSemantics.leaderActivityModifier(poor,100,"UPKEEP")==0&&KingmakerLeadershipSemantics.leaderActivityModifier(poor,100,"EVENT")==0&&KingmakerLeadershipSemantics.leaderActivityModifier(poor,100,"COMMERCE")==0,"No New Leadership failure penalty outside Activity phase");
  JSONObject welcomed=KingmakerLeadershipSemantics.appointment("CRITICAL_SUCCESS","magister",4,false,false,0);
  check(KingmakerLeadershipSemantics.leaderActivityModifier(welcomed,5)==1&&KingmakerLeadershipSemantics.leaderActivityModifier(welcomed,6)==0,"Critical bonus lasts through next Kingdom turn");
  // Priority 1: New Leadership Timing, Single Role, Invested Reselection, Paved Streets, Museum & Menagerie
  boolean foundingCheckBlocked=false;try{KingmakerLeadershipSemantics.validateTiming(0,"UPKEEP",true,false,false);}catch(IllegalStateException e){foundingCheckBlocked=true;}
  check(foundingCheckBlocked,"Founding turn 0 uses free assignment, not New Leadership check");
  check("UPKEEP_STEP_1".equals(KingmakerLeadershipSemantics.validateTiming(1,"UPKEEP",false,false,false).optString("mode")),"Normal in-turn New Leadership allowed in Upkeep Step 1");
  check("EXTRAORDINARY_OUT_OF_TURN".equals(KingmakerLeadershipSemantics.validateTiming(2,"",true,false,false).optString("mode")),"Out-of-turn New Leadership allowed when role is vacant");
  check("EXTRAORDINARY_OUT_OF_TURN".equals(KingmakerLeadershipSemantics.validateTiming(2,"",false,true,false).optString("mode")),"Out-of-turn New Leadership allowed when incumbent dies or is unavailable");
  boolean outOfTurnNonVacantBlocked=false;try{KingmakerLeadershipSemantics.validateTiming(2,"",false,false,false);}catch(IllegalStateException e){outOfTurnNonVacantBlocked=true;}
  check(outOfTurnNonVacantBlocked,"Out-of-turn replacement of available non-vacant leader is blocked");
  JSONArray sampleLeaders=new JSONArray()
   .put(new JSONObject().put("role_key","ruler").put("vacant",0).put("entity_kind","character").put("entity_id",10L).put("leader_name","Aldori Hero"))
   .put(new JSONObject().put("role_key","counselor").put("vacant",0).put("entity_kind","companion").put("entity_id",2L).put("leader_name","Tristian"))
   .put(new JSONObject().put("role_key","general").put("vacant",0).put("entity_kind","companion").put("entity_id",1L).put("leader_name","Amiri"))
   .put(new JSONObject().put("role_key","treasurer").put("vacant",0).put("entity_kind","companion").put("entity_id",3L).put("leader_name","Jubilost"))
   .put(new JSONObject().put("role_key","emissary").put("vacant",0).put("entity_kind","npc").put("entity_id",0L).put("leader_name","Kesten Garess"))
   .put(new JSONObject().put("role_key","magister").put("vacant",0).put("entity_kind","npc").put("entity_id",0L).put("leader_name","Storyteller"))
   .put(new JSONObject().put("role_key","viceroy").put("vacant",1).put("entity_kind","").put("entity_id",0L).put("leader_name",""));
  boolean duplicateNameBlocked=false;try{KingmakerLeadershipSemantics.validateSingleRolePerLeader(sampleLeaders,"warden","npc",0L,"kesten garess");}catch(IllegalStateException e){duplicateNameBlocked=true;}
  check(duplicateNameBlocked,"Same leader name cannot occupy two leadership roles simultaneously");
  Set<String> autoComp=new HashSet<String>(Arrays.asList("counselor","general"));
  Set<String> reselected=KingmakerLeadershipSemantics.validateInvestedReselection(sampleLeaders,new JSONArray().put("ruler").put("treasurer").put("emissary").put("magister"),autoComp);
  check(reselected.size()==6&&reselected.contains("counselor")&&reselected.contains("general"),"Four manual invested roles combine with automatic companion investments");
  boolean fiveManualBlocked=false;try{KingmakerLeadershipSemantics.validateInvestedReselection(sampleLeaders,new JSONArray().put("ruler").put("treasurer").put("emissary").put("magister").put("counselor"),Collections.<String>emptySet());}catch(IllegalStateException e){fiveManualBlocked=true;}
  check(fiveManualBlocked,"More than four manual invested roles is rejected");
  JSONArray pavedRows=new JSONArray().put(new JSONObject().put("structure_key","paved_streets").put("status","built").put("count",1).put("effects_json",new JSONObject().put("infrastructure_instances",new JSONArray().put(new JSONObject().put("urban_grid",1)))));
  check(KingmakerStructureSemantics.lotTravelMinutes(pavedRows,1,3)==15&&KingmakerStructureSemantics.lotTravelMinutes(pavedRows,2,3)==45,"Paved streets reduce adjacent lot travel from 15 to 5 minutes per lot in that Urban Grid");
  JSONObject donated=KingmakerStructureSemantics.museumExhibitOutcome(new JSONArray(),"donate","briar_replica","Relic of Stag",6,true);
  check(donated.optInt("unrest_delta")==-1&&donated.optJSONArray("exhibits").length()==1,"Donating level 6+ historical magic item to Museum reduces Unrest by 1");
  JSONObject removedExhibit=KingmakerStructureSemantics.museumExhibitOutcome(donated.optJSONArray("exhibits"),"remove","briar_replica","Relic of Stag",6,true);
  check(removedExhibit.optInt("unrest_delta")==1&&removedExhibit.optJSONArray("exhibits").length()==0,"Removing item from Museum display increases Unrest by 1");
  JSONObject menagerieAdd=KingmakerStructureSemantics.menagerieAdditionOutcome(new JSONArray(),10,"owlbear_alpha","Alpha Owlbear",6,-4,"ruin","strife","fame");
  check(menagerieAdd.optInt("ruin_delta")==-1&&"strife".equals(menagerieAdd.optString("ruin_key"))&&!menagerieAdd.optBoolean("sapient"),"Level 6 animal added to Menagerie at Kingdom level 10 reduces chosen Ruin by 1");
  JSONObject sapientAdd=KingmakerStructureSemantics.menagerieAdditionOutcome(menagerieAdd.optJSONArray("creatures"),12,"wyvern_captive","Captive Wyvern",6,-2,"reputation","","fame");
  check(sapientAdd.optBoolean("sapient")&&sapientAdd.optInt("fame_delta")==1,"Sapient creature in Menagerie grants Fame but is flagged sapient");
  JSONArray menagerieStruct=new JSONArray().put(new JSONObject().put("structure_key","menagerie").put("status","built").put("count",1).put("effects_json",new JSONObject().put("menagerie_creatures",sapientAdd.optJSONArray("creatures"))));
  check(KingmakerStructureSemantics.menagerieSapientTurnUnrest(menagerieStruct)==1,"Each sapient creature in a Menagerie adds 1 Unrest at start of Kingdom turn");
  // Priority 2: Companion Guide adventure checks and branching consequences
  JSONObject ekProg=new JSONObject();
  for(int i=0;i<4;i++)ekProg=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("ekundayo","feast_round_"+(i+1),18,12,20,ekProg).optJSONObject("progress");
  check(ekProg.optInt("feast_points")==8&&ekProg.optBoolean("feast_success"),"Four critical successes in Feast of Feasts reach 8 Feast Points");
  JSONObject ekBranch=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("ekundayo","accept_new_life_and_ntavi",new JSONObject().put("companion_key","ekundayo").put("influence",6),ekProg);
  check(ekBranch.optInt("influence_after")==8&&"helpful".equals(ekBranch.optString("attitude_after"))&&ekBranch.optInt("xp_awarded")==80,"Ekundayo positive branch reaches Helpful (8) and awards 80 XP");
  JSONObject jubProg=new JSONObject();
  for(int i=0;i<3;i++)jubProg=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","debate_q"+(i+1),15,14,24,jubProg).optJSONObject("progress");
  check(jubProg.optInt("debate_wins")==3&&jubProg.optBoolean("debate_won"),"Three successful rounds win the Inconsequent Debates");
  JSONObject jubBranch=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("jubilost","win_debate_free_host",new JSONObject().put("companion_key","jubilost").put("influence",6),jubProg);
  check(jubBranch.optJSONArray("unlocked_rewards_json").toString().contains("jubilost:official_structure_offer"),"Jubilost debate victory unlocks official Alchemist structure offer");
  JSONObject valMercy=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("valerie","spare_fredero_and_slay_tulvak",new JSONObject().put("companion_key","valerie").put("influence",4).put("available",1),new JSONObject());
  check(valMercy.optInt("available_after")==1&&"helpful".equals(valMercy.optString("attitude_after")),"Valerie remains available and Helpful when Sir Fredero is spared");
  JSONObject valDepart=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("valerie","deny_mercy_slay_fredero",new JSONObject().put("companion_key","valerie").put("influence",4).put("available",1),new JSONObject());
  check(valDepart.optInt("available_after")==0&&"hostile".equals(valDepart.optString("attitude_after")),"Valerie permanently leaves the party when Sir Fredero is denied mercy");
  JSONObject trisProg=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian","tristian_research_cult",19,18,25,new JSONObject()).optJSONObject("progress");
  check(trisProg.optInt("research_points")==2&&trisProg.optInt("curse_countdown")==20,"Tristian critical research grants 2 points without shortening the official countdown of 20");
  JSONObject trisBranch=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("tristian","redeem_cultists_awaken_deva",new JSONObject().put("companion_key","tristian").put("influence",6),trisProg);
  check(trisBranch.optInt("xp_awarded")==120&&trisBranch.optJSONArray("items_awarded").length()==2,"Tristian redemption branch awards 120 XP, Celestial Wings, and Incarnate Ancestry ritual");
  JSONObject nokBranch=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("nok_nok","hero_of_stolen_lands",new JSONObject().put("companion_key","nok_nok").put("influence",4),new JSONObject());
  check(nokBranch.optJSONArray("unlocked_rewards_json").toString().contains("nok_nok_alignment:chaotic_neutral"),"Nok-Nok heroism branch shifts alignment to chaotic neutral");
  // Regression: every actual Companion Guide branch ID must resolve through
  // the catalog, not through contradictory legacy branch names.
  String[] auditedCompanionKeys={"amiri","ekundayo","jubilost","linzi","nok_nok","tristian","valerie"};
  String[] publishedBranches={"free_kean_reforge_pure_blade","temper_vengeance_embrace_fellowship",
    "ask_gnome_first_world_truth","forgive_linzi_establish_press",
    "guide_nok_nok_to_true_heroism","cleanse_sanctuary_awaken_aasimar_wings","deny_mercy_to_dying_fredero"};
  for(int i=0;i<auditedCompanionKeys.length;i++){
   JSONObject publishedBranch=new JSONObject().put("branch_key",publishedBranches[i])
    .put("influence_delta",i==6?-4:2).put("available_after",i!=6)
    .put("attitude_after",i==6?"indifferent":"helpful")
    .put("xp_awarded",500).put("items_awarded",new JSONArray().put("UNVERIFIED REWARD"))
    .put("feature_unlocked","proof:"+auditedCompanionKeys[i]).put("description","Escolha oficial");
   JSONObject progress=new JSONObject().put("branches",new JSONArray().put(publishedBranch))
    .put("automatic_rewards_compiled",false);
   JSONObject before=new JSONObject().put("companion_key",auditedCompanionKeys[i]).put("influence",4).put("available",1);
   JSONObject outcome=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(auditedCompanionKeys[i],publishedBranches[i],before,progress);
   check(publishedBranches[i].equals(outcome.optString("branch_key")),"Companion published branch ID: "+auditedCompanionKeys[i]);
   check(outcome.optInt("xp_awarded")==0&&outcome.optJSONArray("items_awarded").length()==0,
    "Unverified reward never minted: "+auditedCompanionKeys[i]);
   check(outcome.optInt("available_after")==(i==6?0:1),"Official companion availability: "+auditedCompanionKeys[i]);
   boolean refused=false;
   try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(auditedCompanionKeys[i],"obsolete_unpublished_branch",before,progress);}
   catch(IllegalArgumentException ex){refused=true;}
   check(refused,"Obsolete/invented branch rejected: "+auditedCompanionKeys[i]);
  }
  // Genuine Ekundayo feast: exactly 5 Victory Points, not 8.
  JSONObject publishedFeast=new JSONObject()
   .put("scenes",new JSONArray().put(new JSONObject()
    .put("skill_checks",new JSONArray().put(new JSONObject().put("check_id","ekundayo_feast_contribution").put("dc",22)))))
   .put("feast_points_target",5).put("feast_points",4);
  JSONObject feastOutcome=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_feast_contribution",10,12,22,publishedFeast,101L).optJSONObject("progress");
  check(feastOutcome.optInt("feast_points")==5&&feastOutcome.optBoolean("feast_success"),
   "Official 5-VP feast threshold is reached at fifth point");
  boolean wrongDcBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("ekundayo","ekundayo_feast_contribution",10,12,20,publishedFeast);}
  catch(IllegalArgumentException ex){wrongDcBlocked=true;}
  check(wrongDcBlocked,"Narrator cannot lower published DC");
  boolean inventedCheckBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("ekundayo","invented_check",10,12,22,publishedFeast);}
  catch(IllegalArgumentException ex){inventedCheckBlocked=true;}
  check(inventedCheckBlocked,"Narrator cannot inject unlisted check");
  // Official asset integration: all seven canonical JSON definitions as installed
  // (not synthetic branch IDs invented by the test).
  String projectRoot=System.getenv("PF2_R36_PROJECT");
  check(projectRoot!=null&&projectRoot.length()>0,"R36 project path supplied to integration harness");
  String companionAsset=new String(java.nio.file.Files.readAllBytes(
   java.nio.file.Paths.get(projectRoot,"android","app","src","main","assets","pathfinder","kingmaker","chapters","chapter_12_companion_stories.json")),
   java.nio.charset.StandardCharsets.UTF_8);
  JSONObject actualChapter=new JSONObject(companionAsset);
  JSONArray seven=actualChapter.optJSONArray("quests");
  check(seven.length()==7,"Exactly seven actual personal adventures imported");
  Set<String> checkedCompanions=new HashSet<String>();
  for(int n=0;n<seven.length();n++){
   JSONObject q=seven.optJSONObject(n);
   JSONObject progress=q.optJSONObject("progress_json");
   String companionKey=progress.optString("companion_key");
   check(checkedCompanions.add(companionKey),"Personal quest identity unique: "+companionKey);
   check(("q_cg_"+companionKey).equals(q.optString("quest_key")),"Personal quest identity linked: "+companionKey);
   JSONArray options=progress.optJSONArray("branches");
   check(options.length()>=2,"Actual personal quest has alternate outcomes: "+companionKey);
   for(int k=0;k<options.length();k++){
    JSONObject sourceBranch=options.optJSONObject(k);
    String branchId=sourceBranch.optString("branch_key");
    JSONObject companionBefore=new JSONObject().put("companion_key",companionKey).put("influence",4).put("available",1);
    // Use real published checks to reach each Jubilost outcome. An unanswered
    // fixture may not bypass the Host's three ordered questions (Guide p.42).
    JSONObject branchProgress=progress;
    if("ekundayo".equals(companionKey)){
     boolean staying="temper_vengeance_embrace_fellowship".equals(branchId);
     branchProgress=new JSONObject(progress.toString())
      .put("bridge_battle_concluded",true).put("pc_aided_bridge",true);
     if(staying)branchProgress=KingmakerCompanionSemantics.recordEkundayoRomance(branchProgress,101L);
     branchProgress=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
      "ekundayo","ekundayo_interrupt_ntavi",staying?10:3,staying?11:7,21,branchProgress)
      .optJSONObject("progress");
    }
    if("jubilost".equals(companionKey)){
     branchProgress=new JSONObject(progress.toString());
     int debateDc=branchProgress.optJSONObject("debate_rule").optInt("dc");
     check(debateDc==24,"Actual Jubilost debate DC from installed Companion Guide data");
     boolean noAnswer="jubilost_debates_no_answer".equals(branchId);
     for(int round=1;round<=3;round++){
      // At 1 VP Shyka names no winner; at 3 VP the Host answers.
      int modifier=debateDc-10-((noAnswer&&round>1)?1:0);
      branchProgress=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
       companionKey,"jubilost_debate_round_"+round,10,modifier,debateDc,branchProgress)
       .optJSONObject("progress");
     }
     check(branchProgress.optInt("debate_rounds_answered")==3,
      "Jubilost published outcome requires all three real debate checks: "+branchId);
     check(branchProgress.optInt("debate_victory_points")==(noAnswer?1:3),
      "Jubilost published outcome has consistent Victory Points: "+branchId);
    }
    JSONObject actualResult=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
     companionKey,branchId,companionBefore,branchProgress);
    check(branchId.equals(actualResult.optString("branch_key")),"Canonical JSON branch accepted: "+branchId);
    check(actualResult.optInt("influence_delta")==sourceBranch.optInt("influence_delta"),"Influence matches canonical branch: "+branchId);
    check(actualResult.optInt("available_after")==(sourceBranch.optBoolean("available_after",true)?1:0),
     "Companion departure matches canonical branch: "+branchId);
    check(actualResult.optString("feature_unlocked").equals(sourceBranch.optString("feature_unlocked")),
     "Feature matches canonical branch: "+branchId);
    if("jubilost".equals(companionKey)&&"ask_stolen_lands_planar_secret".equals(branchId)){
     JSONObject helpful=new JSONObject(companionBefore.toString()).put("attitude","helpful");
     JSONObject unhappy=new JSONObject(companionBefore.toString()).put("attitude","indifferent");
     JSONObject quiet=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(companionKey,branchId,helpful,branchProgress);
     JSONObject angry=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(companionKey,branchId,unhappy,branchProgress);
     check(quiet.optInt("available_after")==0&&
      "quiet".equals(quiet.optJSONObject("progress").optString("jubilost_departure_tone"))&&
      "helpful".equals(quiet.optString("attitude_after")),
      "Companion Guide pp.42-43: Helpful Jubilost leaves quietly");
     check(angry.optInt("available_after")==0&&
      "angry".equals(angry.optJSONObject("progress").optString("jubilost_departure_tone"))&&
      "indifferent".equals(angry.optString("attitude_after")),
      "Companion Guide pp.42-43: non-Helpful Jubilost leaves angrily");
    }
    int bookXp="ekundayo".equals(companionKey)&&branchProgress.optBoolean("ntavi_recruitment_prevented",false)?10:0;
    check(actualResult.optInt("xp_awarded")==bookXp&&actualResult.optJSONArray("items_awarded").length()==0,
     "Only book-verified XP granted; unverified branch loot blocked: "+branchId);
    JSONObject restoredProgress=new JSONObject(actualResult.optJSONObject("progress").toString());
    boolean replayBlocked=false;
    try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
      companionKey,branchId,companionBefore,restoredProgress);}
    catch(IllegalStateException expected){replayBlocked=true;}
    check(replayBlocked,"Saved outcome cannot be granted again: "+branchId);
    String alternate=options.optJSONObject((k+1)%options.length()).optString("branch_key");
    boolean changedOutcomeBlocked=false;
    try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
      companionKey,alternate,companionBefore,restoredProgress);}
    catch(IllegalStateException expected){changedOutcomeBlocked=true;}
    check(changedOutcomeBlocked,"Saved outcome cannot change branch: "+branchId);
   }
   JSONArray scenes=progress.optJSONArray("scenes");
   int officialChecks=0;
   JSONObject checkedProgress=progress;
   for(int k=0;k<scenes.length();k++){
    JSONArray sourceChecks=scenes.optJSONObject(k).optJSONArray("skill_checks");
    if(sourceChecks==null)continue;
    for(int c=0;c<sourceChecks.length();c++){
     JSONObject sourceCheck=sourceChecks.optJSONObject(c);
     String id=sourceCheck.optString("check_id");
     int dc=sourceCheck.optInt("dc");
     long feastActor="ekundayo_feast_contribution".equals(id)?1101L:0L;
     boolean gmRequest="ekundayo_request_stay".equals(id)&&"GM_SELECTED".equals(sourceCheck.optString("dc_mode"));
     if(gmRequest){
      dc=21; // fixture GM choice, not a book-defined DC
      checkedProgress=new JSONObject(progress.toString()).put("bridge_battle_concluded",true)
       .put("ntavi_at_bridge",true).put("ekundayo_attitude_at_request","friendly");
     }
     // Tristian must investigate to 8 RP before Talbot becomes accessible.
     // Earn these points via the published research check, not a manual flag.
     if("tristian".equals(companionKey)&&"tristian_confront_talbot".equals(id)){
      while(checkedProgress.optInt("research_points")<8)
       checkedProgress=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
        companionKey,"tristian_research_cult",10,15,25,checkedProgress).optJSONObject("progress");
     }
     JSONObject result=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
      companionKey,id,10,dc-10,dc,checkedProgress,feastActor);
     if("jubilost".equals(companionKey)||"tristian".equals(companionKey)||gmRequest)
      checkedProgress=result.optJSONObject("progress");
     check(id.equals(result.optJSONObject("check").optString("check_id")),
      "Canonical JSON skill check accepted: "+id);
     officialChecks++;
     if(gmRequest){
      check(result.optJSONObject("check").optInt("dc")==21
       &&result.optJSONObject("progress").optBoolean("ekundayo_request_stay_accepted"),
       "GM Request DC and success are saved, without inventing fixed book DC");
      boolean ekRequestRepeated=false;
      try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck(companionKey,id,10,dc-10,dc,checkedProgress);}
      catch(IllegalStateException expected){ekRequestRepeated=true;}
      check(ekRequestRepeated,"Repeat Request cannot silently override prior result");
     }else{
      boolean forbidden=false;
      try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck(companionKey,id,10,dc-10,dc+1,progress);}
      catch(IllegalArgumentException expected){forbidden=true;}
      check(forbidden,"Modified canonical skill DC rejected: "+id);
     }
    }
   }
   check(officialChecks>0,"Real companion story checks executable: "+companionKey);
  }
  check(checkedCompanions.containsAll(Arrays.asList("amiri","ekundayo","jubilost","linzi","nok_nok","tristian","valerie")),
   "All seven official companions checked against installed JSON");
  // Companion Guide pp.27-31: real installed Feast of Feasts quest,
  // Ntavi's four conversation outcomes, XP and departure conditions.
  JSONObject ekActual=null;
  for(int n=0;n<seven.length();n++){
   JSONObject quest=seven.optJSONObject(n);
   if("q_cg_ekundayo".equals(quest.optString("quest_key")))
    ekActual=quest.optJSONObject("progress_json");
  }
  check(ekActual!=null,"Ekundayo canonical quest from installed source");
  JSONObject feastReady=new JSONObject(ekActual.toString());
  for(int i=0;i<5;i++)
   feastReady=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
    "ekundayo","ekundayo_feast_contribution",10,12,22,feastReady,100L+i).optJSONObject("progress");
  check(feastReady.optInt("feast_points")==5&&feastReady.optBoolean("feast_success")
   &&feastReady.optJSONArray("feast_contributor_ids").length()==5,
   "Five distinct PCs earned the five official banquet VPs");
  boolean repeatFeastBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_feast_contribution",10,12,22,feastReady,100L);}
  catch(IllegalStateException expected){repeatFeastBlocked=true;}
  check(repeatFeastBlocked,"One feast contribution per PC; no repeat VP farming");
  feastReady.put("ekundayo_helpful",true);
  JSONObject ntaviCritical=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_interrupt_ntavi",10,11,21,feastReady).optJSONObject("progress");
  check("CRITICAL_SUCCESS".equals(ntaviCritical.optString("ntavi_interruption_degree"))
   &&ntaviCritical.optBoolean("ntavi_at_bridge")&&ntaviCritical.optBoolean("ntavi_recruitment_prevented"),
   "Helpful Ekundayo upgrades social success; Ntavi remains and early attack prevented");
  ntaviCritical.put("bridge_battle_concluded",true).put("pc_aided_bridge",true)
   .put("ekundayo_attitude_at_request","friendly");
  boolean stayWithoutRequestBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","temper_vengeance_embrace_fellowship",
   new JSONObject().put("influence",4).put("attitude","friendly"),ntaviCritical);}
  catch(IllegalStateException expected){stayWithoutRequestBlocked=true;}
  check(stayWithoutRequestBlocked,"Ntavi present: require a completed successful Request or romance");
  JSONObject requestSuccess=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_request_stay",10,12,21,ntaviCritical).optJSONObject("progress");
  check(requestSuccess.optBoolean("ekundayo_request_stay_accepted"),"Successful GM-selected Request recorded");
  JSONObject ekStay=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","temper_vengeance_embrace_fellowship",
   new JSONObject().put("influence",4).put("attitude","friendly").put("available",1),requestSuccess);
  check(ekStay.optInt("influence_after")>=8&&ekStay.optInt("xp_awarded")==40
   &&ekStay.optJSONArray("items_awarded").length()==0,
   "Book-verified 30+10 XP and at-least-8 influence, no invented branch treasure");
  JSONObject ntaviSuccess=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_interrupt_ntavi",10,11,21,ekActual).optJSONObject("progress");
  check("SUCCESS".equals(ntaviSuccess.optString("ntavi_interruption_degree"))
   &&!ntaviSuccess.optBoolean("ntavi_at_bridge")&&ntaviSuccess.optBoolean("ntavi_recruitment_prevented"),
   "Normal social success sends Ntavi away but prevents early attack");
  ntaviSuccess.put("bridge_battle_concluded",true).put("pc_aided_bridge",false);
  boolean cannotDepartWithAbsentNtavi=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","consume_in_ruthless_vengeance",new JSONObject().put("influence",0),ntaviSuccess);}
  catch(IllegalStateException expected){cannotDepartWithAbsentNtavi=true;}
  check(cannotDepartWithAbsentNtavi,"Ekundayo cannot depart with Ntavi after she has left festival");
  JSONObject ntaviFailure=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_interrupt_ntavi",3,7,21,ekActual).optJSONObject("progress");
  check("CRITICAL_FAILURE".equals(ntaviFailure.optString("ntavi_interruption_degree"))
   &&ntaviFailure.optBoolean("ntavi_false_silverstep_clue")
   &&ntaviFailure.optBoolean("ekundayo_departs_early"),
   "Critical failure causes false Silverstep clue and early departure");
  ntaviFailure.put("bridge_battle_concluded",true).put("pc_aided_bridge",false);
  JSONObject ekDeparture=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","consume_in_ruthless_vengeance",new JSONObject().put("influence",0),ntaviFailure);
  check(ekDeparture.optInt("available_after")==0&&ekDeparture.optInt("xp_awarded")==0,
   "Ekundayo may leave with present Ntavi; no arbitrary XP");
  JSONObject romance=new JSONObject(ntaviFailure.toString()).put("ekundayo_romance_with_pc",true);
  boolean romanceProtected=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","consume_in_ruthless_vengeance",new JSONObject().put("influence",0),romance);}
  catch(IllegalStateException expected){romanceProtected=true;}
  check(romanceProtected,"Ekundayo romance with a PC blocks leaving with Ntavi");
  JSONObject noAid=new JSONObject(ekActual.toString())
   .put("bridge_battle_concluded",true).put("pc_aided_bridge",false)
   .put("ntavi_conversation_resolved",true).put("ntavi_at_bridge",false);
  JSONObject notHelped=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","temper_vengeance_embrace_fellowship",
   new JSONObject().put("influence",4).put("attitude","friendly"),noAid);
  check(notHelped.optInt("influence_after")==4,"Cannot mint 8 Influence without banquet threshold or bridge help");
  noAid.put("pc_aided_bridge",true);
  JSONObject helped=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","temper_vengeance_embrace_fellowship",
   new JSONObject().put("influence",4).put("attitude","friendly"),noAid);
  check(helped.optInt("influence_after")==8,"Bridge aid plus Ekundayo staying earns 8 Influence");
  JSONObject feastPenalty=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_feast_contribution",1,0,22,new JSONObject(ekActual.toString()),901L).optJSONObject("progress");
  check(feastPenalty.optInt("feast_points")==-1,"Critical failure deducts one banquet VP");
  JSONObject ntaviUninterrupted=KingmakerCompanionSemantics.recordEkundayoNoIntervention(ekActual);
  check(ntaviUninterrupted.optBoolean("ntavi_conversation_resolved")
   &&"NO_INTERVENTION".equals(ntaviUninterrupted.optString("ntavi_interruption_degree"))
   &&ntaviUninterrupted.optBoolean("ntavi_at_bridge")
   &&ntaviUninterrupted.optBoolean("ekundayo_departs_early"),
   "No intervention: Ntavi recruits Ekundayo, both depart early");
  boolean lateInterruptionBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
   "ekundayo","ekundayo_interrupt_ntavi",10,11,21,ntaviUninterrupted);}
  catch(IllegalStateException expected){lateInterruptionBlocked=true;}
  check(lateInterruptionBlocked,"No late interruption after explicitly ignoring Ntavi");
  boolean unknownNtaviBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch(
   "ekundayo","temper_vengeance_embrace_fellowship",
   new JSONObject().put("influence",4).put("attitude","friendly"),
   new JSONObject(ekActual.toString()).put("bridge_battle_concluded",true));}
  catch(IllegalStateException expected){unknownNtaviBlocked=true;}
  check(unknownNtaviBlocked,"Cannot assume Ntavi present or absent without resolved conversation");
  JSONObject establishedRomance=KingmakerCompanionSemantics.recordEkundayoRomance(ekActual,101L);
  check(establishedRomance.optBoolean("ekundayo_romance_with_pc")
   &&establishedRomance.optLong("ekundayo_romance_pc_id")==101L,
   "Existing romance with actual PC ID is persisted in quest progress");
  boolean changedRomanceBlocked=false;
  try{KingmakerCompanionSemantics.recordEkundayoRomance(establishedRomance,102L);}
  catch(IllegalStateException expected){changedRomanceBlocked=true;}
  check(changedRomanceBlocked,"Recorded romantic bond cannot be reassigned");
  boolean lateRomanceBlocked=false;
  try{KingmakerCompanionSemantics.recordEkundayoRomance(ntaviUninterrupted,101L);}
  catch(IllegalStateException expected){lateRomanceBlocked=true;}
  check(lateRomanceBlocked,"Romance cannot be invented after Ntavi's conversation");
  // Jubilost official VP accounting and outcomes — Companion Guide pp.41-43.
  JSONArray jvSourceChecks=new JSONArray();
  for(int jvIndex=1;jvIndex<=3;jvIndex++)jvSourceChecks.put(new JSONObject().put("check_id","jubilost_debate_round_"+jvIndex).put("dc",24));
  JSONObject jvSource=new JSONObject().put("scenes",new JSONArray().put(new JSONObject().put("skill_checks",jvSourceChecks))).put("debate_rule",new JSONObject());
  JSONObject jvProgress=new JSONObject(jvSource.toString());
  for(int jvIndex=1;jvIndex<=3;jvIndex++)jvProgress=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(
    "jubilost","jubilost_debate_round_"+jvIndex,14,10,24,jvProgress).optJSONObject("progress");
  check(jvProgress.optInt("debate_rounds_answered")==3&&jvProgress.optInt("debate_victory_points")==3,
    "Three ordinary successful debate answers award 3 Victory Points, not generic wins");
  check("WIN".equals(jvProgress.optString("debate_outcome"))&&jvProgress.optBoolean("host_question_available"),
    "Exactly 3 Victory Points earns one Host answer");
  boolean jvRepeatedBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_3",14,10,24,jvProgress);}
  catch(IllegalStateException ex){jvRepeatedBlocked=true;}
  check(jvRepeatedBlocked,"Repeat debate round must not mint extra Victory Points");
  JSONObject jvOnePoint=new JSONObject(jvSource.toString());
  jvOnePoint=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_1",14,10,24,jvOnePoint).optJSONObject("progress");
  jvOnePoint=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_2",15,0,24,jvOnePoint).optJSONObject("progress");
  jvOnePoint=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_3",15,0,24,jvOnePoint).optJSONObject("progress");
  check(jvOnePoint.optInt("debate_victory_points")==1&&"NO_WINNER".equals(jvOnePoint.optString("debate_outcome")),
    "One Victory Point makes Shyka declare no winner, not grant an answer");
  boolean jvUnwonQuestionBlocked=false;
  jvOnePoint.put("branches",new JSONArray().put(new JSONObject().put("branch_key","ask_gnome_first_world_truth").put("available_after",true)));
  try{KingmakerCompanionSemantics.resolveCompanionAdventureBranch("jubilost","ask_gnome_first_world_truth",
    new JSONObject().put("influence",4),jvOnePoint);}
  catch(IllegalStateException ex){jvUnwonQuestionBlocked=true;}
  check(jvUnwonQuestionBlocked,"Cannot ask the Host a question after scoring only 1 Victory Point");
  JSONObject jvFailBranch=new JSONObject().put("branch_key","jubilost_debates_no_answer")
    .put("influence_delta",0).put("available_after",true).put("attitude_after","indifferent")
    .put("description","Debate encerrado sem resposta");
  jvOnePoint.optJSONArray("branches").put(jvFailBranch);
  JSONObject jvNoAnswer=KingmakerCompanionSemantics.resolveCompanionAdventureBranch("jubilost",
    "jubilost_debates_no_answer",new JSONObject().put("influence",4),jvOnePoint);
  check(jvNoAnswer.optInt("xp_awarded")==0&&jvNoAnswer.optJSONArray("items_awarded").length()==0,
    "Unwon debate does not produce invented XP/loot");
  JSONObject jvCritical=new JSONObject(jvSource.toString());
  jvCritical=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_1",20,14,24,jvCritical).optJSONObject("progress");
  check(jvCritical.optInt("debate_victory_points")==2,"A critical success awards 2 VP");
  boolean jvOutOfOrderBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("jubilost","jubilost_debate_round_3",20,14,24,jvCritical);}
  catch(IllegalStateException ex){jvOutOfOrderBlocked=true;}
  check(jvOutOfOrderBlocked,"Three official questions must be answered in order");
  // Tristian Guide pp.73-75: 20 descending clock, 8 RP reveals recruiter,
  // non-research room checks must never change Research Points.
  JSONObject trOfficial=new JSONObject()
   .put("scenes",new JSONArray().put(new JSONObject().put("skill_checks",new JSONArray()
    .put(new JSONObject().put("check_id","tristian_research_cult").put("dc",25)
     .put("alternate_skill_dcs",new JSONArray().put(new JSONObject().put("skill","Diplomacia").put("dc",27))
      .put(new JSONObject().put("skill","Sociedade").put("dc",29))))
    .put(new JSONObject().put("check_id","tristian_confront_talbot").put("dc",25)))))
   .put("curse_rule",new JSONObject().put("starting_countdown",20));
  JSONObject trResearch=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian",
    "tristian_research_cult",20,20,25,trOfficial).optJSONObject("progress");
  check(trResearch.optInt("research_points")==2&&trResearch.optInt("curse_countdown")==20,
   "Tristian critical research: +2 RP, countdown stays 20");
  check("guide_20_down_v1".equals(trResearch.optString("curse_semantics_revision")),
   "Tristian countdown revision is persisted to protect legacy saves");
  JSONObject trAlt=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian",
    "tristian_research_cult",10,17,27,trResearch).optJSONObject("progress");
  check(trAlt.optInt("research_points")==3&&trAlt.optInt("curse_countdown")==20,
   "Tristian DC 27 Diplomacy alternate research succeeds");
  JSONObject trCriticalFail=KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian",
    "tristian_research_cult",1,0,25,trAlt).optJSONObject("progress");
  check(trCriticalFail.optInt("curse_countdown")==19&&trCriticalFail.optInt("research_points")==3,
   "Tristian critical research failure reduces countdown by 1, not raises it");
  JSONObject trUnknownCheck=new JSONObject(trCriticalFail.toString());
  boolean trUnknownBlocked=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian",
    "invented_research_action",12,15,25,trUnknownCheck);}
  catch(IllegalArgumentException ex){trUnknownBlocked=true;}
  check(trUnknownBlocked,"Tristian invented research action rejected");
  boolean trRecruiterTooSoon=false;
  try{KingmakerCompanionSemantics.resolveCompanionAdventureCheck("tristian",
    "tristian_confront_talbot",12,16,25,trCriticalFail);}
  catch(IllegalStateException ex){trRecruiterTooSoon=true;}
  check(trRecruiterTooSoon,"Tristian cannot confront hidden recruiter before 8 Research Points");
  System.out.println("R36_BOOK_SEMANTICS_ASSERTIONS_PASS="+assertions);
 }
}



