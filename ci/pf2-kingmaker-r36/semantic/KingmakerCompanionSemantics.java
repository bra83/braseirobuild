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
 public static JSONObject naturalRestRecovery(JSONObject hero,int constitution,JSONObject prior,double absoluteHours)throws Exception{
  if(Double.isNaN(absoluteHours)||Double.isInfinite(absoluteHours)||absoluteHours<24)throw new IllegalArgumentException("Horário de descanso inválido.");
  int before=hero.optInt("hp"),maximum=hero.optInt("max_hp"),level=hero.optInt("level",1);
  if(before<0||maximum<1||before>maximum||level<1||level>20)throw new IllegalArgumentException("PV ou nível inválidos para o descanso.");
  JSONObject sheet=hero.optJSONObject("sheet_json");if(sheet==null)sheet=new JSONObject(hero.optString("sheet_json","{}"));
  JSONObject out=new JSONObject().put("before_hp",before).put("after_hp",before).put("healed",0).put("eligible",false);
  if(sheet.optBoolean("dead")||sheet.optInt("dying")>0)return out.put("reason","Não aplicar recuperação a morto ou personagem ainda morrendo.");
  if(prior.has("rest_end_hours")&&(Double.isNaN(prior.optDouble("rest_end_hours"))||Double.isInfinite(prior.optDouble("rest_end_hours"))))throw new IllegalArgumentException("Histórico de descanso inválido.");
  if(prior.has("rest_end_hours")&&absoluteHours-prior.optDouble("rest_end_hours")<24d)return out.put("reason","O benefício do descanso já foi recebido nas últimas 24 horas.");
  int healing=(int)Math.min((long)maximum-before,Math.max(1L,(long)constitution)*level);
  return out.put("eligible",true).put("after_hp",before+healing).put("healed",healing).put("rest_end_hours",absoluteHours);
 }
 public static JSONObject mergeLegacyAlias(JSONObject current,JSONObject alias)throws Exception{
  JSONObject out=new JSONObject(current.toString());
  boolean untouched=current.optInt("influence")==0&&current.optInt("npc_id")==0&&current.optInt("available",1)==1&&"reserve".equals(current.optString("assignment"))&&"Companheiro oficial das Terras Roubadas.".equals(current.optString("notes"))&&history(current).length()==0;
  if(untouched)for(String field:new String[]{"influence","npc_id","available","attitude","assignment","leadership_role","personal_quest_status","personal_quest_notes","unlocked_rewards_json","notes"})if(alias.has(field))out.put(field,alias.opt(field));
  return out;
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

 public static String attitudeForInfluence(String k,int influence){
  if(influence<0)return "hostile";
  if("amiri".equals(k)||"harrim".equals(k)||"octavia".equals(k))return influence>=4?"helpful":influence>=2?"friendly":"indifferent";
  if("linzi".equals(k))return influence>=2?"helpful":influence>=1?"friendly":"indifferent";
  if("jaethal".equals(k)||"valerie".equals(k))return influence>=6?"helpful":influence>=3?"friendly":"indifferent";
  if("ekundayo".equals(k))return influence>=8?"helpful":influence>=4?"friendly":influence>=2?"indifferent":"unfriendly";
  if("regongar".equals(k))return influence>=6?"helpful":influence>=4?"friendly":influence>=2?"indifferent":"unfriendly";
  if("kalikke_kanerah".equals(k))return influence>=6?"helpful":influence>=4?"friendly":"indifferent";
  if("nok_nok".equals(k))return influence>=8?"helpful":influence>=4?"friendly":"indifferent";
  if("jubilost".equals(k)||"tristian".equals(k))return influence>=8?"helpful":influence>=2?"friendly":"indifferent";
  return "indifferent";
 }

 public static JSONObject resolveCompanionAdventureCheck(String companionKey,String checkId,int naturalRoll,int totalModifier,int dc,JSONObject currentProgress)throws Exception{
  if(companionKey==null||questLevel(companionKey)==0)throw new IllegalArgumentException("Companheiro sem aventura publicada: "+companionKey);
  if(checkId==null||checkId.trim().length()==0)throw new IllegalArgumentException("Identificador do teste da aventura é obrigatório.");
  if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("O d20 natural deve estar entre 1 e 20.");
  if(dc<10||dc>60)throw new IllegalArgumentException("CD inválida para aventura de companheiro: "+dc);
  // For a real imported quest, a caller cannot invent a check ID or its DC.
  JSONArray officialScenes=currentProgress==null?null:currentProgress.optJSONArray("scenes");
  if(officialScenes!=null){
   boolean declared=false,matchedDc=false;
   for(int i=0;i<officialScenes.length();i++){
    JSONObject scene=officialScenes.optJSONObject(i);
    if(scene==null)continue;
    JSONArray checks=scene.optJSONArray("skill_checks");
    if(checks==null)continue;
    for(int j=0;j<checks.length();j++){
     JSONObject c=checks.optJSONObject(j);
     if(c!=null&&checkId.trim().equals(c.optString("check_id"))){
      declared=true;
      matchedDc=dc==c.optInt("dc",-1)||dc==c.optInt("alternative_dc_when_helpful",-1)||dc==c.optInt("alternative_dc_for_crafting",-1);
      JSONArray alternateSkillDcs=c.optJSONArray("alternate_skill_dcs");
      if(alternateSkillDcs!=null)for(int n=0;n<alternateSkillDcs.length();n++){
       JSONObject alt=alternateSkillDcs.optJSONObject(n);
       if(alt!=null&&dc==alt.optInt("dc",-1))matchedDc=true;
      }
     }
    }
   }
   if(!declared)throw new IllegalArgumentException("Teste não definido no Companion Guide para "+companionKey+": "+checkId);
   if(!matchedDc)throw new IllegalArgumentException("CD alterada indevidamente no teste "+checkId+": "+dc);
  }
  int total=naturalRoll+totalModifier;
  int deg=total>=dc+10?3:total>=dc?2:total<=dc-10?0:1;
  if(naturalRoll==20)deg=Math.min(3,deg+1);else if(naturalRoll==1)deg=Math.max(0,deg-1);
  if("ekundayo_interrupt_ntavi".equals(checkId)&&currentProgress!=null&&currentProgress.optBoolean("ekundayo_helpful",false))
   deg=Math.min(3,deg+1);
  String degree=deg==3?"CRITICAL_SUCCESS":deg==2?"SUCCESS":deg==1?"FAILURE":"CRITICAL_FAILURE";
  JSONObject next=currentProgress!=null?new JSONObject(currentProgress.toString()):new JSONObject();
  JSONArray checks=next.optJSONArray("adventure_checks");if(checks==null)checks=new JSONArray();
  JSONObject entry=new JSONObject().put("check_id",checkId.trim()).put("natural_roll",naturalRoll).put("modifier",totalModifier).put("total",total).put("dc",dc).put("degree",degree);
  checks.put(entry);
  next.put("adventure_checks",checks).put("journal_started",true);
  if("ekundayo".equals(companionKey)&&(officialScenes==null||"ekundayo_feast_contribution".equals(checkId))){
   int delta=deg==3?2:deg==2?1:deg==1?0:-1;
   int pts=Math.max(0,next.optInt("feast_points",0)+delta);
   int goal=next.optInt("feast_points_target",officialScenes==null?8:5);
   next.put("feast_points",pts).put("feast_success",pts>=goal)
    .put("feast_participated",true);
   entry.put("feast_points_delta",delta).put("feast_points_total",pts);
  }else if("ekundayo".equals(companionKey)&&"ekundayo_interrupt_ntavi".equals(checkId)){
   boolean prevented=deg>=2;
   next.put("ntavi_interruption_degree",degree)
    .put("ntavi_at_bridge",deg!=2)
    .put("ekundayo_departs_early",!prevented)
    .put("ntavi_recruitment_prevented",prevented)
    .put("ntavi_location_revealed",deg>=2)
    .put("ntavi_false_silverstep_clue",deg==0);
   entry.put("ntavi_conclusion",prevented?"PC_LOYALTY_RETAINED":"EKUNDAYO_RECRUITED");
  }else if("jubilost".equals(companionKey)&&(officialScenes==null||checkId.startsWith("jubilost_debate_round_"))){
   int wins=next.optInt("debate_wins",0)+(deg>=2?1:0);
   int losses=next.optInt("debate_losses",0)+(deg<2?1:0);
   next.put("debate_wins",wins).put("debate_losses",losses);
   if(officialScenes!=null){
    // Companion Guide p.42: three fixed questions at DC 24, degrees yield
    // +2, +1, 0 or -1 Victory Points. Three mere "wins" are not the metric.
    int answered=currentProgress.optInt("debate_rounds_answered",0);
    String expected="jubilost_debate_round_"+(answered+1);
    if(answered>=3||!expected.equals(checkId))
     throw new IllegalStateException("Os três debates devem ocorrer na ordem do livro e sem repetições. Próximo: "+expected);
    int gained=deg==3?2:deg==2?1:deg==1?0:-1;
    int vp=currentProgress.optInt("debate_victory_points",0)+gained;
    next.put("debate_rounds_answered",answered+1).put("debate_victory_points",vp)
     .put("debate_won",answered+1==3&&vp>=3)
     .put("host_question_available",answered+1==3&&vp>=2)
     .put("host_each_pc_question",answered+1==3&&vp>=4);
    if(answered+1==3)next.put("debate_outcome",vp<=0?"LOST":vp==1?"NO_WINNER":vp==2?"TIE":vp==3?"WIN":"EXTRAORDINARY_WIN");
    entry.put("victory_points_delta",gained).put("victory_points_total",vp);
   }else next.put("debate_won",wins>=3);
   entry.put("debate_wins",wins).put("debate_losses",losses);
  }else if("tristian".equals(companionKey)){
   int rpDelta=deg==3?2:deg==2?1:0;
   int rp=next.optInt("research_points",0)+rpDelta;
   int curse=next.optInt("curse_countdown",0)+(deg==0?2:1);
   next.put("research_points",rp).put("curse_countdown",curse).put("sanctum_located",rp>=12).put("maray_afflicted",curse>=6);
   entry.put("research_points_total",rp).put("curse_countdown",curse);
  }else if("valerie".equals(companionKey)){
   int rounds=next.optInt("duel_rounds",0)+1;
   boolean subdued=next.optBoolean("fredero_subdued")||deg>=2;
   next.put("duel_rounds",rounds).put("fredero_subdued",subdued);
   entry.put("fredero_subdued",subdued);
  }
  return new JSONObject().put("companion_key",companionKey).put("check",entry).put("progress",next);
 }

 public static JSONObject resolveCompanionAdventureBranch(String companionKey,String branchKey,JSONObject companionBefore,JSONObject currentProgress)throws Exception{
  if(companionKey==null||questLevel(companionKey)==0)throw new IllegalArgumentException("Companheiro sem aventura publicada: "+companionKey);
  if(branchKey==null||branchKey.trim().length()==0)throw new IllegalArgumentException("Ramificação da aventura é obrigatória.");
  String b=branchKey.trim();
  // Production companion quests carry their published branches in progress_json.
  // Never fall through to historical hard-coded outcomes for a live quest:
  // the legacy IDs differ from the actual seven adventure definitions.
  JSONArray canonicalBranches=currentProgress==null?null:currentProgress.optJSONArray("branches");
  if(canonicalBranches!=null){
   JSONObject selected=null;
   for(int i=0;i<canonicalBranches.length();i++){
    JSONObject candidate=canonicalBranches.optJSONObject(i);
    if(candidate!=null&&b.equals(candidate.optString("branch_key"))){
     if(selected!=null)throw new IllegalStateException("Desfecho duplicado no catálogo: "+b);
     selected=candidate;
    }
   }
   if(selected==null)throw new IllegalArgumentException("Desfecho não publicado para "+companionKey+": "+b);
   if("jubilost".equals(companionKey)&&currentProgress.has("debate_rule")){
    if(currentProgress.optInt("debate_rounds_answered",0)!=3)
     throw new IllegalStateException("Responda as três perguntas do Anfitrião antes de encerrar os Debates.");
    boolean questionEarned=currentProgress.optInt("debate_victory_points",0)>=2;
    if(questionEarned=="jubilost_debates_no_answer".equals(b))
     throw new IllegalStateException("Este resultado não corresponde à pontuação do debate.");
   }
   int priorInfluence=companionBefore.optInt("influence",0);
   int delta=selected.optInt("influence_delta",0);
   int nextInfluence=Math.max(0,priorInfluence+delta);
   int nextAvailable=selected.optBoolean("available_after",true)?1:0;
   if("ekundayo".equals(companionKey)){
    // Guide pp.27-31: 5 feast VP advances one influence tier; helping
    // defeat the Reavers and keeping Ekundayo raises influence to at least 8.
    if(currentProgress.optBoolean("feast_success",false))
     nextInfluence=Math.max(nextInfluence,priorInfluence<2?2:priorInfluence<4?4:8);
    if(nextAvailable==1)nextInfluence=Math.max(nextInfluence,8);
    if(nextAvailable==0){
     if(currentProgress.has("ntavi_at_bridge")&&!currentProgress.optBoolean("ntavi_at_bridge"))
      throw new IllegalStateException("Ntavi saiu após a conversa e não está presente para recrutar Ekundayo.");
     if(currentProgress.optBoolean("ekundayo_romance_with_pc")||currentProgress.optBoolean("ekundayo_request_stay_accepted"))
      throw new IllegalStateException("Ekundayo permanece com os PJs por romance ou Request aceito.");
    }
   }
   String nextAttitude="ekundayo".equals(companionKey)
    ?(nextAvailable==0?"unfriendly":attitudeForInfluence(companionKey,nextInfluence))
    :selected.optString("attitude_after",attitudeForInfluence(companionKey,nextInfluence));
   String unlocked=selected.optString("feature_unlocked","");
   JSONArray historyAfter=rewardHistory(companionBefore,nextInfluence,nextAttitude,history(companionBefore));
   if(unlocked.length()>0&&!has(historyAfter,unlocked))historyAfter.put(unlocked);
   String alignment=selected.optString("alignment_after","");
   if("nok_nok".equals(companionKey)&&alignment.length()>0){
    JSONArray cleanRewards=new JSONArray();
    for(int i=0;i<historyAfter.length();i++)if(!historyAfter.optString(i).startsWith("nok_nok_alignment:"))cleanRewards.put(historyAfter.optString(i));
    cleanRewards.put("nok_nok_alignment:"+alignment);historyAfter=cleanRewards;
   }
   // Source-defined automatic rewards are quarantined until the exact
   // book loot/XP has been certified; never mint invented inventory/XP.
   boolean rewardsCertified=currentProgress.optBoolean("automatic_rewards_compiled",false);
   int xpCertified=rewardsCertified?selected.optInt("xp_awarded",0):0;
   if("ekundayo".equals(companionKey)){
    // The Guide explicitly awards 30/10 XP for feast participation and
    // 10 XP for preventing Ntavi's reckless private recruitment.
    // Do not grant unverified treasure or made-up branch XP.
    if(currentProgress.optBoolean("feast_participated",false))
     xpCertified+=currentProgress.optBoolean("feast_success",false)?30:10;
    if(currentProgress.optBoolean("ntavi_recruitment_prevented",false))xpCertified+=10;
   }
   JSONArray itemsCertified=new JSONArray();
   if(rewardsCertified){
    JSONArray sourceItems=selected.optJSONArray("items_awarded");
    if(sourceItems!=null)for(int i=0;i<sourceItems.length();i++)itemsCertified.put(sourceItems.optString(i));
   }
   JSONObject nextProgress=new JSONObject(currentProgress.toString());
   String summary=selected.optString("description",selected.optString("title"));
   nextProgress.put("branch_resolved",b).put("branch_summary",summary)
    .put("xp_awarded",xpCertified).put("items_awarded",itemsCertified).put("journal_started",true);
   return new JSONObject().put("companion_key",companionKey).put("branch_key",b)
    .put("summary",summary).put("influence_before",priorInfluence)
    .put("influence_delta",delta).put("influence_after",nextInfluence)
    .put("attitude_after",nextAttitude).put("available_after",nextAvailable)
    .put("xp_awarded",xpCertified).put("items_awarded",itemsCertified)
    .put("feature_unlocked",unlocked).put("unlocked_rewards_json",historyAfter)
    .put("progress",nextProgress);
  }
  int infDelta=0,xp=80,availableAfter=companionBefore.optInt("available",1);
  String alignAfter="",feature="",summary="";
  JSONArray items=new JSONArray();
  if("amiri".equals(companionKey)){
   if("reconcile_six_bears".equals(b)||"defeat_tigers_spare_bears".equals(b)){
    infDelta=2;xp=120;items.put("Espada Grande de Kean Reforjada (+2 Greater Striking Frost Giant-Bane)");
    feature="companion_boon:amiri_six_bears_alliance";summary="Amiri honra Nilak, reconcilia-se com a tribo dos Seis Ursos e desperta o poder ancestral da espada de Kean.";
   }else if("slay_tigers_and_avenge_nilak".equals(b)){
    infDelta=1;xp=80;items.put("Espada Grande de Kean Desperta (+2 Striking Giant-Bane)");
    feature="companion_boon:amiri_blood_calling_vengeance";summary="Amiri abate Fionn e os Senhores Tigres em vingança direta por Nilak.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Amiri: "+b);
  }else if("ekundayo".equals(companionKey)){
   if("accept_new_life_and_ntavi".equals(b)||"feast_triumph_let_go_vengeance".equals(b)){
    infDelta=2;xp=80;items.put("Arco Composto de Ntavi (+1 Striking)");
    feature="companion_boon:ekundayo_feast_of_feasts";summary="Ekundayo supera o luto no Banquete dos Banquetes, derrota Karga e aceita uma nova vida nas Terras Roubadas.";
   }else if("cling_to_vengeance".equals(b)){
    infDelta=0;xp=40;feature="companion_boon:ekundayo_sworn_avenger";summary="Ekundayo derrota Karga, mas mantém seu juramento implacável contra os gigantes.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Ekundayo: "+b);
  }else if("jubilost".equals(companionKey)){
   if("win_debate_free_host".equals(b)||"answer_xae_save_gnomes".equals(b)){
    infDelta=2;xp=80;items.put("Notas Secretas de Xae sobre o Branqueamento");
    feature="jubilost:official_structure_offer";summary="Jubilost vence os Debates Inconsequentes, liberta seu duplo e recebe a resposta de Xae, oferecendo um Laboratório de Alquimia gratuito ao Reino.";
   }else if("forfeit_or_lose_debate".equals(b)){
    infDelta=-2;xp=30;feature="companion_outcome:jubilost_debate_ setback";summary="Jubilost deixa os Debates Inconsequentes sem o favor completo de Xae.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Jubilost: "+b);
  }else if("linzi".equals(companionKey)){
   if("expose_eike_and_pardon_linzi".equals(b)||"recover_press_establish_printing".equals(b)){
    infDelta=2;xp=80;items.put("Prensa Tipográfica Completa da Academia");
    feature="companion_boon:linzi_printing_press";summary="O grupo derrota o coven Jaggedbriar, desmascara Eike, recupera a prensa tipográfica e perdoa o empréstimo de Linzi.";
   }else if("punish_linzi_strict_audit".equals(b)){
    infDelta=-1;xp=80;items.put("Prensa Tipográfica Confiscada pelo Tesouro");
    feature="companion_boon:linzi_printing_press";summary="A prensa é recuperada do coven Jaggedbriar, mas Linzi sofre repreensão formal pelo uso do tesouro.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Linzi: "+b);
  }else if("nok_nok".equals(companionKey)){
   if("hero_of_stolen_lands".equals(b)||"reject_lamashtu_choose_heroism".equals(b)){
    infDelta=2;xp=80;alignAfter="chaotic_neutral";items.put("Corta-Cães do Quinto Deus-Herói (+1 Striking Returning)");
    feature="companion_boon:nok_nok_fifth_hero";summary="Nok-Nok derrota Nargloom e o Barghest Maior, rejeita a servidão cega a Lamashtu e assume seu papel como herói das Terras Roubadas.";
   }else if("worship_lamashtu_shrine".equals(b)){
    infDelta=0;xp=40;alignAfter="chaotic_evil";feature="companion_boon:nok_nok_lamashtu_zealot";summary="Nok-Nok derrota Nargloom mas mantém os santuários de Lamashtu.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Nok-Nok: "+b);
  }else if("tristian".equals(companionKey)){
   if("redeem_cultists_awaken_deva".equals(b)||"cleanse_curse_incarnate_ancestry".equals(b)){
    infDelta=2;xp=120;items.put("Asas Celestiais Despertas de Sarenrae").put("Ritual Ancestralidade Encarnada (Incarnate Ancestry)");
    feature="tristian:official_structure_offer";summary="Tristian derrota Maray, purifica o Reino dos Purificados, desperta sua herança de Deva Astral e oferece um Orfanato ou Santuário gratuito ao Reino.";
   }else if("purge_cult_without_mercy".equals(b)){
    infDelta=0;xp=80;items.put("Asas Celestiais Despertas de Sarenrae");
    feature="companion_boon:tristian_celestial_wings";summary="Tristian erradica o Reino dos Purificados pela força e desperta suas asas celestiais.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Tristian: "+b);
  }else if("valerie".equals(companionKey)){
   if("spare_fredero_and_slay_tulvak".equals(b)||"redeem_fredero_break_curse".equals(b)){
    infDelta=2;xp=80;availableAfter=1;items.put("Escudo de Aço Polido de Sir Fredero (+2 Sturdy Shield)");
    feature="companion_boon:valerie_scar_healed";summary="Valerie vence o duelo com honra, poupa Sir Fredero, derrota a bruxa Tulvak e tem sua cicatriz curada por Shelyn.";
   }else if("deny_mercy_slay_fredero".equals(b)){
    infDelta=-6;xp=0;availableAfter=0;feature="companion_departure:valerie_denied_mercy";summary="Sir Fredero tem a misericórdia negada; indignada com a violação do código de honra, Valerie abandona permanentemente a companhia.";
   }else throw new IllegalArgumentException("Ramificação desconhecida para Valerie: "+b);
  }
  int beforeInf=companionBefore.optInt("influence",0);
  int afterInf=Math.max(0,beforeInf+infDelta);
  String attitudeAfter=availableAfter==0?"hostile":attitudeForInfluence(companionKey,afterInf);
  JSONArray existingRewards=history(companionBefore);
  JSONArray updatedRewards=rewardHistory(companionBefore,afterInf,attitudeAfter,existingRewards);
  if(feature.length()>0&&!has(updatedRewards,feature))updatedRewards.put(feature);
  if("nok_nok".equals(companionKey)&&alignAfter.length()>0){
   JSONArray cleaned=new JSONArray();
   for(int i=0;i<updatedRewards.length();i++)if(!updatedRewards.optString(i).startsWith("nok_nok_alignment:"))cleaned.put(updatedRewards.optString(i));
   cleaned.put("nok_nok_alignment:"+alignAfter);
   updatedRewards=cleaned;
  }
  JSONObject nextProgress=currentProgress!=null?new JSONObject(currentProgress.toString()):new JSONObject();
  nextProgress.put("branch_resolved",b).put("branch_summary",summary).put("xp_awarded",xp).put("items_awarded",items).put("journal_started",true);
  return new JSONObject()
   .put("companion_key",companionKey)
   .put("branch_key",b)
   .put("summary",summary)
   .put("influence_before",beforeInf)
   .put("influence_delta",infDelta)
   .put("influence_after",afterInf)
   .put("attitude_after",attitudeAfter)
   .put("available_after",availableAfter)
   .put("xp_awarded",xp)
   .put("items_awarded",items)
   .put("feature_unlocked",feature)
   .put("unlocked_rewards_json",updatedRewards)
   .put("progress",nextProgress);
 }
}


