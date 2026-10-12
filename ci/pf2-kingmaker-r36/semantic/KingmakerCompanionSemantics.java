package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Set;
import java.util.HashSet;
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
  else {
   JSONObject published=personalQuestDefinition(k);
   base.put("personal_quest",published.optString("title")).put("personal_quest_summary",published.optString("summary")).put("personal_quest_source",published.optString("source_ref")).put("quest_story_timing",published.optString("story_timing"));
  }
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
  if("tristian".equals(k)||"amiri".equals(k))return vordakaiResolved;
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
 public static boolean canLead(JSONObject c,String reputationMode){
  if(c.optInt("available",1)!=1)return false;
  String k=c.optString("companion_key");
  if("ekundayo".equals(k))return "helpful".equals(c.optString("attitude"));
  if("tristian".equals(k))return !"infamy".equals(reputationMode)||c.optInt("influence")>=8;
  return c.optInt("influence")>=leadershipInfluence(k);
 }
 public static boolean automaticInvestment(JSONObject c,String role){
  return automaticInvestment(c,role,c.optString("active_persona"));
 }
 public static boolean automaticInvestment(JSONObject c,String role,String persona){
  if(c.optInt("available",1)!=1)return false;String k=c.optString("companion_key");
  if("kalikke_kanerah".equals(k))return ("kalikke".equals(persona)&&"counselor".equals(role))||("kanerah".equals(persona)&&"emissary".equals(role));
  return ("amiri".equals(k)&&"warden".equals(role))||
         ("ekundayo".equals(k)&&("general".equals(role)||"warden".equals(role)))||
         ("jubilost".equals(k)&&"treasurer".equals(role))||
         ("linzi".equals(k)&&"counselor".equals(role))||
         ("tristian".equals(k)&&"magister".equals(role))||
         ("valerie".equals(k)&&"general".equals(role))||
         ("harrim".equals(k)&&"magister".equals(role))||
         ("jaethal".equals(k)&&"emissary".equals(role))||
         ("nok_nok".equals(k)&&"emissary".equals(role)&&c.optInt("influence")>=8)||
         ("octavia".equals(k)&&"magister".equals(role))||
         ("regongar".equals(k)&&"general".equals(role));
 }
 public static String nokAlignment(JSONObject c){
  int p=c.optInt("influence");
  if(p<=0||"hostile".equals(c.optString("attitude")))return "chaotic_evil";
  JSONArray history=c.optJSONArray("unlocked_rewards_json");
  if(history==null)try{history=new JSONArray(c.optString("unlocked_rewards_json","[]"));}catch(Exception ignored){history=new JSONArray();}
  String saved="chaotic_evil";boolean recorded=false;
  for(int i=0;i<history.length();i++){
   if(history.optString(i).startsWith("nok_nok_alignment:")){saved=history.optString(i).substring("nok_nok_alignment:".length());recorded=true;}
  }
  if(recorded)return saved;
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
  String alignment=prior;
  if(next<=0||"hostile".equals(attitude))alignment="chaotic_evil";
  else if(before.optInt("influence")<12&&next>=12)alignment="chaotic_good";
  else if(before.optInt("influence")<6&&next>=6&&"chaotic_evil".equals(alignment))alignment="chaotic_neutral";
  JSONArray result=new JSONArray();
  for(int i=0;i<out.length();i++)if(!out.optString(i).startsWith("nok_nok_alignment:"))result.put(out.optString(i));
  return result.put("nok_nok_alignment:"+alignment);
 }
 private static JSONArray history(JSONObject c){
  JSONArray a=c.optJSONArray("unlocked_rewards_json");if(a!=null)return a;
  try{return new JSONArray(c.optString("unlocked_rewards_json","[]"));}catch(Exception e){throw new IllegalArgumentException("Histórico de recompensas inválido.",e);}
 }
 private static boolean has(JSONArray a,String key){for(int i=0;i<a.length();i++)if(key.equals(a.optString(i)))return true;return false;}
 public static JSONArray rewardHistory(JSONObject before,int next,String attitude,JSONArray rewards)throws Exception{
  JSONArray out=alignmentHistory(before,next,attitude,rewards);String k=before.optString("companion_key");
  if(next>=8&&("jubilost".equals(k)||"tristian".equals(k))){String marker=k+":official_structure_offer";if(!has(out,marker))out.put(marker);}
  return out;
 }
 public static JSONArray structureRewards(JSONObject c){
  JSONArray out=new JSONArray();if(c.optInt("available",1)!=1)return out;
  String k=c.optString("companion_key");JSONArray rewards=history(c);
  if(c.optInt("influence")<8&&!has(rewards,k+":official_structure_offer"))return out;
  if("jubilost".equals(k)&&!has(rewards,"jubilost:structure_claimed:alchemist"))out.put("alchemist");
  if("tristian".equals(k))for(String structure:new String[]{"orphanage","shrine"})if(!has(rewards,"tristian:structure_claimed:"+structure))out.put(structure);
  return out;
 }
 public static JSONArray claimStructureReward(JSONObject c,String structure)throws Exception{
  JSONArray available=structureRewards(c);if(!has(available,structure))throw new IllegalStateException("Esta estrutura não está disponível como presente deste companheiro.");
  JSONArray out=new JSONArray(history(c).toString());out.put(c.optString("companion_key")+":structure_claimed:"+structure);return out;
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
  if("kalikke_kanerah".equals(k))out.put("daily_persona_choice",present&&p>=8).put("separation_hook_available",present&&p>=8).put("separation_adventure_recommended_level",13).put("powerful_magic_or_freedom_ritual_option",true);
  return out;
 }
 public static JSONObject selectTwinsPersona(JSONObject companion,JSONObject previous,String requested,int preparedDay,String initialPersona)throws Exception{
  if(!"kalikke_kanerah".equals(companion.optString("companion_key"))||companion.optInt("available",1)!=1)throw new IllegalStateException("As gêmeas não estão disponíveis.");
  if(!"kalikke".equals(requested)&&!"kanerah".equals(requested))throw new IllegalArgumentException("Escolha Kalikke ou Kanerah.");
  if(preparedDay<1)throw new IllegalArgumentException("Dia das preparações inválido.");
  String active=previous.optString("persona",initialPersona);
  if(companion.optInt("influence")<8&&!requested.equals(active))throw new IllegalStateException("A escolha da irmã pelo grupo exige Influência 8.");
  if(previous.has("prepared_day")&&preparedDay<=previous.optInt("prepared_day")){
   if(preparedDay==previous.optInt("prepared_day")&&requested.equals(active))return new JSONObject(previous.toString());
   throw new IllegalStateException("A persona fica fixa até as próximas preparações diárias após um novo descanso.");
  }
  return new JSONObject().put("persona",requested).put("prepared_day",preparedDay).put("shared_influence_key","kalikke_kanerah");
 }
 public static JSONObject personalQuestDefinition(String key)throws Exception{
  String title,summary,pages,timing;
  if("amiri".equals(key)){title="O Chamado do Sangue";summary="Amiri busca os Seis Ursos e Nilak para compreender sua espada, a alma de Kean e a perseguição de Fionn.";pages="12–21";timing="Após derrotar Vordakai, ao alcançar nível 11; os rumores após o ataque a Tatzlford são a ocasião indicada.";}
  else if("ekundayo".equals(key)){title="Um Banquete dos Banquetes";summary="Uma festa para Ekundayo aproxima o grupo de seu passado e da influência de Ntavi sobre sua busca de vingança contra os Reavers de Gragglebeard.";pages="27–31";timing="Após fundar o Reino, quando o grupo alcança nível 6.";}
  else if("jubilost".equals(key)){title="Os Debates Inconsequentes";summary="Jubilost procura os Debates Inconsequentes no Primeiro Mundo, buscando uma resposta sobre a perda de conexão dos gnomos com sua terra de origem.";pages="36–43";timing="Nível 8; de preferência após o Florescimento e antes do Desaparecimento de Varnhold. Essa preferência não é um pré-requisito obrigatório.";}
  else if("linzi".equals(key)){title="Problemas no Tesouro";summary="Linzi retirou recursos para montar uma prensa; as peças finais foram interceptadas pelo coven Jaggedbriar. O grupo investiga o desfalque e o carregamento desaparecido.";pages="48–57";timing="Primeiro dia passado em um assentamento do Reino após alcançar nível 7.";}
  else if("nok_nok".equals(key)){title="Heróis ou Deuses?";summary="O ataque dos goblins Tanglethorn ao entreposto de Oleg leva Nok-Nok a enfrentar Nargloom e reconsiderar sua ambição de se tornar o quinto herói-deus goblin.";pages="61–67";timing="Nível 5; preferencialmente durante o capítulo 4, entre a ameaça dos trolls e o ataque da Fera.";}
  else if("tristian".equals(key)){title="O Reino dos Purificados";summary="Após Vordakai, Tristian investiga a sociedade secreta Reino dos Purificados na capital e descobre a verdade sobre sua ancestralidade.";pages="72–81";timing="Após derrotar Vordakai e chegar ao nível 10, antes dos acontecimentos do capítulo 7.";}
  else if("valerie".equals(key)){title="O Desafio de Sir Fredero";summary="Sir Fredero desafia Valerie no Santuário do Bosque Dourado; a manipulação da bruxa Tulvak está por trás da queda do antigo campeão de Shelyn.";pages="86–93";timing="Mensagem no nível 9, durante a investigação de Varnhold; pode começar no nível 10 com ajustes de dificuldade.";}
  else return new JSONObject();
  return new JSONObject().put("title",title).put("summary",summary).put("source_ref","Kingmaker Companion Guide pp."+pages).put("story_timing",timing).put("minimum_level",questLevel(key));
 }

 public static int harrimCampingProtection(JSONArray activities){
  for(int i=0;i<activities.length();i++){
   JSONObject activity=activities.optJSONObject(i);
   if(activity!=null&&"harrim_blend_into_night".equals(activity.optString("companion_activity")))throw new IllegalStateException("A proteção de Harrim já foi invocada neste acampamento.");
  }
  return 2;
 }

 public static int harrimEncounterDcBonus(JSONArray activities,String daypart){
  String phase=daypart==null?"":daypart.toLowerCase(java.util.Locale.ROOT);
  if(!"noite".equals(phase)&&!"madrugada".equals(phase)&&!"night".equals(phase))return 0;
  for(int i=0;i<activities.length();i++){
   JSONObject activity=activities.optJSONObject(i);
   if(activity!=null&&"harrim_blend_into_night".equals(activity.optString("companion_activity"))&&"harrim".equals(activity.optString("actor_companion_key"))&&activity.optInt("encounter_dc_bonus")==2)return 2;
  }
  return 0;
 }

 public static int loggedSleepingHours(JSONArray activities){
  Set<Integer> hours=new HashSet<Integer>();
  for(int i=0;i<activities.length();i++){
   JSONObject row=activities.optJSONObject(i);
   if(row!=null&&"Descansar".equals(row.optString("activity"))&&row.optInt("hour")>0)hours.add(row.optInt("hour"));
  }
  return hours.size();
 }
}
