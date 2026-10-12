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
 public static JSONObject normalizeDefinition(JSONObject definition)throws Exception{
  if(definition==null||!BONUSES.containsKey(definition.optString("official_catalog_key")))return definition;
  // Upgrade From describes an optional conversion, never a prerequisite for a new build.
  definition.put("prerequisites","").put("min_settlement_type","village");
  if(Arrays.asList("bridge","magical_streetlamps","paved_streets","sewer_system","wall_stone","wall_wooden").contains(definition.optString("official_catalog_key")))definition.put("lots",0);
  return definition;
 }
 public static void requireCapital(JSONObject definition,JSONObject settlement,String capitalName){
  if("palace".equals(definition.optString("official_catalog_key"))&&
    (capitalName==null||capitalName.length()==0||!capitalName.equals(settlement.optString("name"))))
    throw new IllegalStateException("Um palácio só pode ser construído na capital do Reino.");
 }
 // Core Rulebook table 10-9 / GM Core p.59: Currency per Additional PC.
 private static final int[] INVESTMENT_GP={0,10,18,30,50,80,125,180,250,350,500,700,1000,1500,2250,3250,5000,7500,12000,20000,35000};
 public static int investmentGp(int level){if(level<1||level>20)throw new IllegalArgumentException("Nível de Reino inválido.");return INVESTMENT_GP[level];}
 public static int investmentDice(String degree,int gold,int level,String criticalChoice){
  String actual=gold<investmentGp(level)?"CRITICAL_FAILURE":degree;
  if("CRITICAL_SUCCESS".equals(actual))return 4;if("SUCCESS".equals(actual))return 2;
  if("FAILURE".equals(actual))return 0;
  if(!"CRITICAL_FAILURE".equals(actual))throw new IllegalArgumentException("Grau de sucesso inválido.");
  if("resource_die".equals(criticalChoice))return 1;if("none".equals(criticalChoice))return 0;
  throw new IllegalArgumentException("Escolha a consequência da falha crítica do investimento.");
 }
 public static JSONObject investmentOutcome(int level,int gold,String degree,int die,JSONArray rolls,int failureD4,String criticalChoice)throws Exception{
  if(gold<=0)throw new IllegalArgumentException("O investimento precisa conter fundos pessoais.");
  if(!Arrays.asList(4,6,8,10,12).contains(die))throw new IllegalArgumentException("Dado de recurso inválido.");
  String actual=gold<investmentGp(level)?"CRITICAL_FAILURE":degree;
  int count=investmentDice(actual,gold,level,criticalChoice);if(rolls.length()!=count)throw new IllegalArgumentException("Quantidade incorreta de dados de recurso.");
  int rp=0,crime=0;for(int i=0;i<count;i++){int value=rolls.optInt(i,0);if(value<1||value>die)throw new IllegalArgumentException("Resultado de recurso inválido.");rp+=value;}
  if("FAILURE".equals(actual)){if(failureD4<1||failureD4>4)throw new IllegalArgumentException("Informe 1d4 RP para a falha.");rp=failureD4;}
  else if("CRITICAL_FAILURE".equals(actual))crime="resource_die".equals(criticalChoice)?rp:1;
  return new JSONObject().put("degree",actual).put("required_gp",investmentGp(level)).put("gold_spent",gold).put("rp_gained",rp).put("crime",crime).put("resource_rolls",rolls).put("failure_d4","FAILURE".equals(actual)?failureD4:0).put("automatic_critical_failure",gold<investmentGp(level));
 }
 public static JSONObject tapTreasuryOutcome(int level,String degree,boolean unpaid,String ruin)throws Exception{
  List<String> scale=Arrays.asList("CRITICAL_FAILURE","FAILURE","SUCCESS","CRITICAL_SUCCESS");int rank=scale.indexOf(degree);if(rank<0)throw new IllegalArgumentException("Grau inválido.");
  if(unpaid)rank=Math.max(0,rank-2);String actual=scale.get(rank);JSONObject out=new JSONObject().put("degree",actual).put("gold_gained",rank>=2?investmentGp(level):0).put("economy_circumstance_penalty",rank<3?-1:0).put("loyalty_circumstance_penalty",rank<2?-1:0).put("unrest",rank==0?1:0);
  if(rank==0){if(!Arrays.asList("corruption","crime","decay","strife").contains(ruin))throw new IllegalArgumentException("Escolha a Ruína afetada.");out.put(ruin,1);}
  return out;
 }
 public static int count(JSONArray rows,String key){
  int total=0;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;
   if(key.equals(row.optString("structure_key"))&&"built".equals(row.optString("status","built")))total+=Math.max(0,row.optInt("count",1));}
  return total;
 }
 public static int activityBonus(JSONArray rows,String activity,String skill,int cap,JSONObject context){
  int best=0;
  for(String k:BONUSES.keySet()){
   int n=count(rows,k);if(n==0)continue;if(EDIFICES.contains(k))n=1;
   if("guildhall".equals(k)){
    if("economy".equals(context.optString("ability")))best=Math.max(best,Math.min(Math.max(0,cap),guildFocusCount(rows,context.optString("trade_focus"))));
    continue;
   }
   for(String token:BONUSES.get(k).split(";")){
    if(token.length()==0)continue;String[] pair=token.split("=");String[] target=pair[0].split(":");
    if(!target[0].equals(activity))continue;
    if(target.length>1&&!target[1].equals(skill)&&!target[1].equals(context.optString("site_type"))&&!target[1].equals(context.optString("ruin")))continue;
    long stacked=(long)Integer.parseInt(pair[1])*n;best=Math.max(best,(int)Math.min(Math.max(0,cap),stacked));
   }
  }
  return best;
 }
 private static String focus(String value){return value==null?"":java.text.Normalizer.normalize(value.trim().toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");}
 private static JSONObject effects(JSONObject row){JSONObject value=row.optJSONObject("effects_json");if(value!=null)return value;try{return new JSONObject(row.optString("effects_json","{}"));}catch(Exception error){throw new IllegalArgumentException("Efeitos de estrutura inválidos.",error);}}
 public static int guildFocusCount(JSONArray rows,String requested){return tradeFocusCount(rows,"guildhall",requested);}
 public static int tradeFocusCount(JSONArray rows,String structure,String requested){
  String target=focus(requested);if(target.length()==0)return 0;int total=0;
  for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null||!structure.equals(row.optString("structure_key"))||!"built".equals(row.optString("status","built")))continue;
   JSONArray names=effects(row).optJSONArray("trade_focus_instances");if(names==null)continue;
   for(int n=0;n<Math.min(Math.max(0,row.optInt("count",1)),names.length());n++)if(target.equals(focus(names.optString(n))))total++;
  }return total;
 }
 public static JSONArray appendedGuildFocus(JSONObject existing,String trade)throws Exception{
  if(focus(trade).length()==0)throw new IllegalArgumentException("Indique o foco comercial da guilda.");
  JSONArray previous=effects(existing).optJSONArray("trade_focus_instances"),out=new JSONArray();int count=existing.length()==0?0:Math.max(0,existing.optInt("count",1));
  for(int i=0;i<count;i++)out.put(previous==null?"":previous.optString(i));return out.put(trade.trim());
 }
 public static JSONArray declareGuildFocus(JSONObject existing,int instance,String trade)throws Exception{
  int count=Math.max(0,existing.optInt("count"));if(instance<1||instance>count||focus(trade).length()==0)throw new IllegalArgumentException("Guilda ou foco inválido.");
  JSONArray previous=effects(existing).optJSONArray("trade_focus_instances"),out=new JSONArray();
  for(int i=0;i<count;i++){String old=previous==null?"":previous.optString(i);if(i==instance-1){if(old.length()>0&&!focus(old).equals(focus(trade)))throw new IllegalStateException("O foco desta guilda já foi definido.");old=trade.trim();}out.put(old);}return out;
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
  else if(Arrays.asList("magic","arcane","primal","occult","luxury_magic").contains(category)) {
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
  if(("earn_income".equals(activity)||"repair".equals(activity))&&guildFocusCount(rows,focus)>0)best=Math.max(best,1);
  if("crafting".equals(skill)&&tradeFocusCount(rows,"trade_shop",focus)>0)best=Math.max(best,1);
  return best;
 }
 public static JSONObject buildEffects(String key,int priorSettlementCount,boolean firstSettlement,boolean firstTurn,boolean firstKingdom,boolean famous,boolean infamous,String mode,String ruin,int die1,int die2)throws Exception{
  if(!BONUSES.containsKey(key))throw new IllegalArgumentException("Estrutura fora do catálogo oficial.");
  if(EDIFICES.contains(key)&&priorSettlementCount>0)return new JSONObject().put("cosmetic_only",true);
  int unrest=0,crime=0,selectedRuin=0;JSONArray usedDice=new JSONArray();
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
    usedDice.put(die1);if("tavern_world_class".equals(key))usedDice.put(die2);
   }
   if("jail".equals(key)||"magical_streetlamps".equals(key))crime=-1;
   if("tavern_dive".equals(key))crime=1;
   if("monument".equals(key))selectedRuin=-1;
  }
  if("palace".equals(key)&&firstKingdom)unrest=-10;
  if("illicit_market".equals(key)||"thieves_guild".equals(key))crime++;
  if("tenement".equals(key))selectedRuin++;
  if(selectedRuin!=0&&!Arrays.asList("corruption","crime","decay","strife").contains(ruin))throw new IllegalArgumentException("Escolha a Ruína da estrutura.");
  JSONObject out=new JSONObject().put("unrest",unrest).put("crime",crime).put("dice",usedDice);
  if(selectedRuin!=0)out.put(ruin,out.optInt(ruin)+selectedRuin);
  if("infamy".equals(mode)){if(infamous)out.put("infamy",1);else if(famous)out.put("infamy",-1);}
  else {if(famous)out.put("fame",1);else if(infamous)out.put("fame",-1);}
  return out;
 }
 public static boolean isGridInfrastructure(String key){return Arrays.asList("magical_streetlamps","paved_streets","sewer_system").contains(key);}
 private static JSONObject instanceEffects(JSONObject row)throws Exception{
  Object raw=row.opt("effects_json");return raw instanceof JSONObject?(JSONObject)raw:new JSONObject(row.optString("effects_json","{}"));
 }
 public static void requireGridInfrastructure(JSONObject town,JSONArray rows,String key,JSONObject options)throws Exception{
  if(!isGridInfrastructure(key))throw new IllegalArgumentException("Esta estrutura não é uma instalação de malha urbana.");
  int grid=options.optInt("urban_grid",0),blocks=town.optInt("developed_blocks",1);
  int grids=(Math.max(1,blocks)-1)/9+1;
  if(grid<1||grid>grids)throw new IllegalArgumentException("Selecione uma malha urbana existente: 1 a "+grids+".");
  if(grids>1&&!"metropolis".equals(town.optString("type")))throw new IllegalStateException("Somente metrópoles têm mais de uma malha urbana.");
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.optJSONObject(i);if(row==null||!key.equals(row.optString("structure_key"))||!"built".equals(row.optString("status","built"))||row.optInt("count",1)<=0)continue;
   if("sewer_system".equals(key))throw new IllegalStateException("O assentamento já tem esgoto; ele atende todas as suas malhas urbanas.");
   JSONArray installed=instanceEffects(row).optJSONArray("infrastructure_instances");
   if(installed==null||installed.length()!=row.optInt("count",1))throw new IllegalStateException("As instalações antigas precisam ter seus locais registrados antes de ampliar esta infraestrutura.");
   for(int n=0;n<installed.length();n++)if(installed.optJSONObject(n)!=null&&installed.optJSONObject(n).optInt("urban_grid")==grid)
    throw new IllegalStateException("Esta infraestrutura já existe na malha urbana indicada.");
  }
 }
 public static JSONArray appendedInfrastructure(JSONObject existing,String key,JSONObject options)throws Exception{
  if(!isInfrastructure(key)||options.optInt("urban_grid",0)<1)throw new IllegalArgumentException("Local da infraestrutura inválido.");
  JSONArray old=existing.length()==0?null:instanceEffects(existing).optJSONArray("infrastructure_instances");
  if(existing.length()>0&&(old==null||old.length()!=existing.optInt("count",1)))throw new IllegalStateException("Infraestrutura existente sem locais íntegros.");
  JSONArray result=old==null?new JSONArray():new JSONArray(old.toString());
  JSONObject location=new JSONObject().put("urban_grid",options.optInt("urban_grid")).put("settlement_wide","sewer_system".equals(key));
  if(isBorderInfrastructure(key))location.put("border",options.optString("border"));
  result.put(location);
  return result;
 }
 public static JSONObject trimInstanceEffects(JSONObject row,int remaining)throws Exception{
  if(remaining<0)throw new IllegalArgumentException("Quantidade de estruturas inválida.");
  JSONObject out=new JSONObject(instanceEffects(row).toString());
  for(String field:new String[]{"trade_focus_instances","infrastructure_instances"}){
   JSONArray old=out.optJSONArray(field);if(old==null)continue;
   JSONArray kept=new JSONArray();for(int i=0;i<Math.min(remaining,old.length());i++)kept.put(old.opt(i));out.put(field,kept);
  }
  return out;
 }

 public static boolean isBorderInfrastructure(String key){return Arrays.asList("bridge","wall_stone","wall_wooden").contains(key);}
 public static boolean isInfrastructure(String key){return isGridInfrastructure(key)||isBorderInfrastructure(key);}
 private static Set<String> normalizedBorders(JSONArray borders){
  if(borders==null)throw new IllegalArgumentException("Lista de bordas inválida.");
  Set<String> result=new HashSet<String>();
  for(int i=0;i<borders.length();i++){String value=borders.optString(i);if(!Arrays.asList("north","east","south","west").contains(value)||!result.add(value))throw new IllegalArgumentException("Borda inválida ou repetida.");}
  return result;
 }
 private static int allocatedGrids(JSONObject town){return (Math.max(1,town.optInt("developed_blocks",1))-1)/9+1;}
 private static void requireAllocatedGrid(JSONObject town,int grid){
  if(grid<1||grid>allocatedGrids(town))throw new IllegalArgumentException("Malha urbana inexistente.");
  if(grid>1&&!"metropolis".equals(town.optString("type")))throw new IllegalStateException("Mais de uma malha exige uma metrópole.");
 }
 private static JSONObject settlementGrid(JSONObject town)throws Exception{
  Object raw=town.opt("grid_json");return raw instanceof JSONObject?new JSONObject(raw.toString()):new JSONObject(town.optString("grid_json","{}"));
 }
 public static JSONObject declareWaterBorders(JSONObject town,int grid,JSONArray water,boolean eligible)throws Exception{
  requireAllocatedGrid(town,grid);Set<String> chosen=normalizedBorders(water);
  if(!eligible&&!chosen.isEmpty())throw new IllegalArgumentException("Bordas de água exigem lago, rio ou pântano no hex.");
  JSONObject out=settlementGrid(town),all=out.optJSONObject("water_borders");
  if(out.has("water_borders")&&all==null)throw new IllegalArgumentException("Registro de bordas inválido.");
  if(all==null)all=new JSONObject();String field=String.valueOf(grid);
  if(all.has(field)&&!normalizedBorders(all.optJSONArray(field)).equals(chosen))throw new IllegalStateException("A escolha das bordas de água é permanente.");
  JSONArray canonical=new JSONArray();for(String side:new String[]{"north","east","south","west"})if(chosen.contains(side))canonical.put(side);
  all.put(field,canonical);out.put("water_borders",all);return out;
 }
 public static JSONObject preserveWaterBorders(JSONObject previous,JSONObject next)throws Exception{
  JSONObject old=previous.optJSONObject("water_borders"),now=next.optJSONObject("water_borders");
  if((previous.has("water_borders")&&old==null)||(next.has("water_borders")&&now==null))throw new IllegalArgumentException("Registro de bordas inválido.");
  int before=old==null?0:old.length(),after=now==null?0:now.length();
  if(before!=after)throw new IllegalStateException("Registre novas bordas pelo formulário de bordas; escolhas existentes são permanentes.");
  if(old!=null){Iterator<String> fields=old.keys();while(fields.hasNext()){String field=fields.next();if(now==null||!normalizedBorders(old.optJSONArray(field)).equals(normalizedBorders(now.optJSONArray(field))))throw new IllegalStateException("As bordas de água não podem ser alteradas.");}}
  return next;
 }
 public static void requireBorderInfrastructure(JSONObject town,JSONArray rows,String key,JSONObject options)throws Exception{
  if(!isBorderInfrastructure(key))throw new IllegalArgumentException("Estrutura não é de borda.");
  int grid=options.optInt("urban_grid",0);requireAllocatedGrid(town,grid);String border=options.optString("border");
  if(!Arrays.asList("north","east","south","west").contains(border))throw new IllegalArgumentException("Escolha a borda da infraestrutura.");
  JSONObject top=settlementGrid(town).optJSONObject("water_borders");JSONArray water=top==null?new JSONArray():top.optJSONArray(String.valueOf(grid));
  if("bridge".equals(key)&&(water==null||!normalizedBorders(water).contains(border)))throw new IllegalStateException("Uma ponte só pode ocupar uma borda de água previamente registrada.");
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.optJSONObject(i);if(row==null||row.optInt("count",1)<=0||!"built".equals(row.optString("status","built")))continue;
   String other=row.optString("structure_key");boolean wall=key.startsWith("wall_")&&other.startsWith("wall_");if(!key.equals(other)&&!wall)continue;
   JSONArray installed=instanceEffects(row).optJSONArray("infrastructure_instances");
   if(installed==null||installed.length()!=row.optInt("count",1))throw new IllegalStateException("Infraestrutura antiga sem posições registradas.");
   for(int n=0;n<installed.length();n++){JSONObject location=installed.optJSONObject(n);if(location!=null&&location.optInt("urban_grid")==grid&&border.equals(location.optString("border")))throw new IllegalStateException("Esta borda já tem "+(wall?"uma muralha.":"uma ponte."));}
  }
 }
 public static boolean isIsland(JSONObject town,JSONArray rows){
  try{
   JSONObject water=settlementGrid(town).optJSONObject("water_borders");if(water==null)return false;
   for(int grid=1;grid<=allocatedGrids(town);grid++){JSONArray sides=water.optJSONArray(String.valueOf(grid));if(sides==null||normalizedBorders(sides).size()!=4)return false;}
   for(int i=0;i<rows.length();i++){
    JSONObject row=rows.optJSONObject(i);if(row==null||!"bridge".equals(row.optString("structure_key"))||row.optInt("count",1)<=0||!"built".equals(row.optString("status","built")))continue;
    JSONArray bridges=instanceEffects(row).optJSONArray("infrastructure_instances");if(bridges==null)continue;
    for(int n=0;n<bridges.length();n++){JSONObject location=bridges.optJSONObject(n);if(location==null)continue;int grid=location.optInt("urban_grid");if(grid>=1&&grid<=allocatedGrids(town)&&normalizedBorders(water.optJSONArray(String.valueOf(grid))).contains(location.optString("border")))return false;}
   }
   return true;
  }catch(Exception error){throw new IllegalArgumentException("Registro espacial do assentamento inválido.",error);}
 }

}

