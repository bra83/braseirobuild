package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
/** Permanent quest dependency evaluation. Never reads PDFs during play. */
public final class KingmakerCampaignGate {
 private KingmakerCampaignGate(){}
 public static JSONObject progress(JSONObject q) {
  JSONObject p=q.optJSONObject("progress_json");if(p!=null)return p;
  try{return new JSONObject(q.optString("progress_json","{}"));}catch(Exception e){throw new IllegalArgumentException("Progresso de missão inválido.",e);}
 }
 public static String questKey(JSONObject q){String k=q.optString("quest_key");return k.length()>0?k:progress(q).optString("quest_key");}
 public static String chapterKey(JSONObject q){String k=q.optString("chapter_key");return k.length()>0?k:progress(q).optString("chapter_key");}
 public static boolean completed(String s){return "completed".equals(s)||"complete".equals(s)||"concluida".equals(s);}
 public static boolean retiredQuest(JSONObject q){return Arrays.asList("q_terras_sombrias","q_brecha_sekamina","q_ancoras_abissais","q_aberracoes_subterraneas","q_terremoto_da_capital","q_companheiro_amiri","q_companheiro_valerie","q_companheiro_linzi","q_companheiro_harrim","q_companheiro_tristian","q_companheiro_jaethal","q_companheiro_regongar","q_companheiro_octavia","q_companheiro_ekundayo","q_companheiro_jubilost","q_companheiro_nok_nok","q_companheiro_irmas_tiefling").contains(questKey(q));}
 public static JSONArray objectives(JSONObject quest){
  JSONArray data=quest.optJSONArray("objectives");if(data!=null)return data;
  data=progress(quest).optJSONArray("objectives");if(data!=null)return data;
  String raw=quest.optString("objectives_json");if(raw.length()>0)try{return new JSONArray(raw);}catch(Exception invalid){throw new IllegalArgumentException("Objetivos inválidos.",invalid);}
  return new JSONArray();
 }
 public static boolean objectivesResolved(JSONObject quest){
  JSONArray data=objectives(quest);for(int n=0;n<data.length();n++){
   JSONObject item=data.optJSONObject(n);if(item==null)return false;
   if(!item.optBoolean("completed")&&!completed(item.optString("status"))&&!("waived".equals(item.optString("status"))&&item.optString("resolution_notes").trim().length()>=5))return false;
  }return true;
 }
 public static JSONObject resolveObjective(JSONObject quest,String id,boolean waived,String evidence)throws Exception{
  if(id==null||id.length()==0||evidence==null||evidence.trim().length()<5)throw new IllegalArgumentException("Objetivo e evidência são obrigatórios.");
  JSONObject next=new JSONObject(progress(quest).toString());JSONArray result=new JSONArray(),source=objectives(quest);boolean found=false;
  for(int n=0;n<source.length();n++){JSONObject copy=new JSONObject(source.optJSONObject(n).toString());if(id.equals(copy.optString("id"))){if(found)throw new IllegalStateException("Identidade de objetivo duplicada.");found=true;copy.put("completed",!waived).put("status",waived?"waived":"completed").put("resolution_notes",evidence.trim());}result.put(copy);}
  if(!found)throw new IllegalArgumentException("O objetivo não pertence à definição desta missão.");next.put("objectives",result);return next;
 }
 public static boolean official(JSONObject q){return chapterKey(q).startsWith("chapter_")&&questKey(q).length()>0;}
 public static JSONArray prerequisites(JSONObject q){
  JSONArray a=q.optJSONArray("prerequisites");
  if(a!=null)return a;
  String raw=q.optString("prerequisites_json");if(raw.length()>0)try{return new JSONArray(raw);}catch(Exception e){throw new IllegalArgumentException("Pré-requisitos inválidos.",e);}
  a=progress(q).optJSONArray("prerequisites");return a==null?new JSONArray():a;
 }
 public static boolean ready(JSONObject q,List<JSONObject> all) {
  if(!official(q))return true;
  Map<String,JSONObject> index=new HashMap<String,JSONObject>();
  for(JSONObject row:all){String k=questKey(row);if(k.length()>0)index.put(k,row);}
  JSONArray required=prerequisites(q);
  for(int i=0;i<required.length();i++){String k=required.optString(i);JSONObject prev=index.get(k);
   if(k.length()==0||prev==null||!completed(prev.optString("status")))return false;
  }
  return true;
 }
 public static List<JSONObject> playable(List<JSONObject> candidates,List<JSONObject> all){
  List<JSONObject> out=new ArrayList<JSONObject>();
  for(JSONObject q:candidates)if(!retiredQuest(q)&&(!official(q)||officialChapter(chapterKey(q))>0||questKey(q).startsWith("q_cg_"))&&ready(q,all))out.add(q);
  return out;
 }
 public static JSONObject transitionState(JSONObject before,JSONObject change){
  if(!official(before))return before;
  try{JSONObject next=new JSONObject(before.toString());if(change.has("progress_json"))next.put("progress_json",change.opt("progress_json"));return next;}
  catch(Exception error){throw new IllegalArgumentException("Estado de missão inválido.",error);}
 }
 public static JSONObject preserveDefinition(JSONObject before,JSONObject change){
  if(!official(before))return change;
  try{
   JSONObject result=new JSONObject(change.toString()),prior=progress(before);
   if(!result.has("progress_json")&&!result.has("objective_resolutions"))return result;
   JSONObject next=result.has("progress_json")?progress(result):new JSONObject(prior.toString());
   for(String field:new String[]{"quest_key","chapter_key","milestone_xp","prerequisites","rewards","objectives"})if(prior.has(field))next.put(field,prior.opt(field));
   JSONArray resolutions=result.optJSONArray("objective_resolutions");
   if(resolutions!=null){
    JSONObject working=new JSONObject(before.toString()).put("progress_json",next.toString());
    for(int n=0;n<resolutions.length();n++){JSONObject resolution=resolutions.optJSONObject(n);if(resolution==null)throw new IllegalArgumentException("Resultado de objetivo inválido.");next=resolveObjective(working,resolution.optString("id"),resolution.optBoolean("waived"),resolution.optString("resolution_notes"));working.put("progress_json",next.toString());}
    if(resolutions.length()>0){next.put("journal_started",true);if(!result.has("status"))result.put("status","active");}
   }
   result.put("progress_json",next.toString());return result;
  }catch(Exception e){throw new IllegalArgumentException("Não foi possível preservar a definição da missão.",e);}
 }
 public static void requireTransition(JSONObject before,String next,List<JSONObject> all){
  if(!official(before))return;
  if(retiredQuest(before)&&"archived".equals(next))return;
  if(retiredQuest(before)&&!"archived".equals(next))throw new IllegalStateException("Esta missão pertence a um rascunho substituído por conteúdo do livro.");
  if(completed(before.optString("status"))&&!completed(next))throw new IllegalStateException("Uma missão concluída não pode ser reaberta por uma atualização antiga.");
  if(completed(next)&&!completed(before.optString("status"))&&!objectivesResolved(before))throw new IllegalStateException("Resolva os objetivos da missão antes de concluí-la.");
  if(("active".equals(next)||"in_progress".equals(next)||completed(next))&&!ready(before,all))
    throw new IllegalStateException("Conclua os pré-requisitos desta missão antes de avançar.");
 }
 public static int officialChapter(String key){
  if(key==null||key.startsWith("chapter_11_")||key.startsWith("chapter_12_"))return 0;
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("^chapter_(\\d+)_").matcher(key);
  if(!m.find())return 0;
  int legacy=Integer.parseInt(m.group(1));return legacy>=0&&legacy<=10?legacy+1:0;
 }
 public static int officialChapterForQuest(JSONObject q){
  String k=questKey(q);
  if("q_call_for_heroes".equals(k)||"q_carta_restov".equals(k))return 1;
  if(Arrays.asList("q_oleg","q_kressle","q_senhor_cervos").contains(k))return 3;
  if(Arrays.asList("q_bokken","q_tuskgutter","q_sicomoro","q_templo_alce").contains(k))return 2;
  if("q_fundacao_baronia".equals(k))return 4;
  return officialChapter(chapterKey(q));
 }
 public static String officialTitle(int chapter){
  String[] titles={"","Convocação de Heróis","Rumo aos Ermos","Terras Roubadas","Rios de Sangue","O Culto do Florescimento","O Desaparecimento de Varnhold","Sangue por Sangue","A Guerra dos Reis dos Rios","Eles Espreitam Abaixo","O Som de Mil Gritos","A Maldição do Rei Lanterna"};
  return chapter>=1&&chapter<=11?titles[chapter]:"Conteúdo complementar";
 }
 public static JSONObject mansionCreature(String key)throws Exception{
  int level,hp,maxHp,ac,fort,ref,will,perception,stealth,speed,attack;String name,weapon,damage,conditions="",pages;
  if("cutthroat".equals(key)){name="Lacaio das Lágrimas Negras";level=-1;hp=8;maxHp=8;ac=15;fort=5;ref=7;will=2;perception=3;stealth=5;speed=25;attack=6;weapon="Adaga";damage="1d4+2";pages="25";}
  else if("gromog".equals(key)){name="Gromog";level=2;hp=28;maxHp=28;ac=16;fort=11;ref=4;will=5;perception=1;stealth=0;speed=25;attack=9;weapon="Gancho de ogro";damage="1d10+3";conditions="blinded";pages="28";}
  else if("jurgrindor".equals(key)){name="Jurgrindor";level=3;hp=25;maxHp=50;ac=17;fort=12;ref=6;will=9;perception=9;stealth=8;speed=30;attack=10;weapon="Punho";damage="1d8+8";pages="31";}
  else if("nishkiv".equals(key)){name="Nishkiv, a Faca";level=1;hp=20;maxHp=20;ac=16;fort=6;ref=9;will=5;perception=5;stealth=7;speed=25;attack=9;weapon="Adaga";damage="1d4+1";pages="36";}
  else if("volodmyra".equals(key)){name="Volodmyra";level=3;hp=48;maxHp=48;ac=18;fort=11;ref=9;will=7;perception=7;stealth=5;speed=25;attack=11;weapon="Machado grande +1";damage="1d12+4";pages="38";}
  else throw new IllegalArgumentException("Adversário não registrado no livro.");
  JSONObject stats=new JSONObject().put("level",level).put("fortitude",fort).put("reflex",ref).put("will",will).put("perception",perception).put("stealth",stealth).put("speed_feet",speed).put("melee_attack",attack).put("melee_damage",damage).put("melee_weapon",weapon);
  if("cutthroat".equals(key))stats.put("ranged_attack",6).put("ranged_damage","1d6").put("ranged_weapon","Besta de mão").put("range_increment_feet",60).put("bolts",10).put("silver_pieces",5);
  if("gromog".equals(key))stats.put("blinded",true).put("itchy_flat_dc",10).put("scratch_actions_success",1).put("scratch_actions_failure",2).put("reach_feet",10).put("deadly_trait",false);
  if("jurgrindor".equals(key))stats.put("wounded_arm_penalties_already_included",true).put("end_turn_bleeding_flat_dc",12).put("persistent_bleed_on_failure","1d6").put("chill_breath_actions",1).put("chill_breath_cone_feet",15).put("chill_breath_damage","2d6").put("chill_breath_reflex_dc",18).put("chill_breath_critical_failure","immobilized").put("escape_dc",18).put("chill_breath_cooldown","1d4").put("rock_attacks_available",false);
  if("nishkiv".equals(key))stats.put("attack_of_opportunity",true).put("double_strike_actions",2).put("second_strike_off_guard_if_first_hits",true).put("normal_multiple_attack_penalty",true).put("sneak_attack_damage","1d6").put("lesser_healing_potions",3);
  if("volodmyra".equals(key))stats.put("attack_of_opportunity",true).put("overhand_smash_actions",2).put("overhand_smash_extra_damage",8).put("overhand_smash_fortitude_dc",17).put("overhand_smash_failure","prone").put("spinning_sweep_actions",1).put("spinning_sweep_third_attack_penalty",-8).put("spinning_sweep_extra_damage","2d6").put("lesser_healing_potions",4).put("necklace_gp",20).put("ring_gp",30);
  return new JSONObject().put("name",name).put("count",1).put("hp",hp).put("max_hp",maxHp).put("ac",ac).put("initiative_bonus",perception).put("conditions",conditions).put("speed",String.valueOf(speed)+" ft").put("attacks",weapon+" +"+attack+"; "+damage).put("official_stats",stats).put("source_ref","Kingmaker p."+pages);
 }
 public static JSONObject mansionEncounter(String room)throws Exception{
  JSONArray enemies=new JSONArray();String pages;
  if("A1".equals(room)){enemies.put(mansionCreature("cutthroat").put("count",3).put("initiative_bonus",5));pages="25";}
  else if("A4".equals(room)){enemies.put(mansionCreature("cutthroat").put("count",4));pages="27";}
  else if("A6".equals(room)){enemies.put(mansionCreature("gromog"));pages="28";}
  else if("A11".equals(room)){enemies.put(mansionCreature("jurgrindor"));pages="30–31";}
  else if("A21".equals(room)){enemies.put(mansionCreature("cutthroat").put("count",2)).put(mansionCreature("nishkiv"));pages="36";}
  else if("A23".equals(room)){enemies.put(mansionCreature("cutthroat").put("count",2)).put(mansionCreature("volodmyra"));pages="37–38";}
  else throw new IllegalArgumentException("Área da mansão sem encontro compilado.");
  JSONObject out=new JSONObject().put("room",room).put("enemies",enemies).put("source_ref","Kingmaker pp."+pages);
  if("A1".equals(room))out.put("initiative_alternative","Perception if the PCs meet the cutthroats in A2 rather than wait in A1");
  if("A23".equals(room))out.put("statblock_cutthroats",2).put("narrative_cutthroats",4).put("count_authority","Printed encounter stat block; narrative discrepancy retained").put("frost_giants_fought_by_jamandi",true);
  return out;
 }
 public static final String[] HELIX_ROOMS={"B14","B13","B12","B10","B8","B4","B3","B2","B1"};
 public static JSONObject advanceHelix(JSONObject before,String room,int natural,int modifier,boolean iconPrepared,boolean placedClosed,JSONArray present)throws Exception{
  if(before.optBoolean("completed"))throw new IllegalStateException("O Teorema da Hélice já foi concluído.");
  if(!iconPrepared)throw new IllegalStateException("Obtenha o ícone de B14 ou prepare-o com Manufatura CD 38.");
  if(!Arrays.asList(HELIX_ROOMS).contains(room))throw new IllegalArgumentException("Câmara inválida para o ritual.");
  int step=before.optInt("step");if(step<0||step>9)throw new IllegalArgumentException("Progresso ritual inválido.");
  JSONObject out=new JSONObject(before.toString());boolean accepted=false;
  if(step==9){
   if(!"B1".equals(room))step=0;
   else accepted=true;
  }else{
   if(natural<1||natural>20)throw new IllegalArgumentException("Informe um d20 entre 1 e 20.");
   int total=natural+modifier,degree=total>=46?3:total>=36?2:total<=26?0:1;
   if(natural==20)degree=Math.min(3,degree+1);else if(natural==1)degree=Math.max(0,degree-1);
   accepted=HELIX_ROOMS[step].equals(room)&&degree>=2;
   step=accepted?step+1:0;
   out.put("natural_roll",natural).put("modifier",modifier).put("total",total).put("dc",36).put("degree",degree);
  }
  out.put("step",step).put("last_room",room).put("reset",!accepted).put("completed",false).put("xp_award",0);
  if(step==9&&placedClosed){
   JSONArray ids=new JSONArray();Set<Long> unique=new HashSet<Long>();
   for(int i=0;i<present.length();i++){long id=Long.parseLong(present.optString(i));if(id<=0)throw new IllegalArgumentException("Personagem presente inválido.");if(unique.add(id))ids.put(id);}
   if(ids.length()==0)throw new IllegalArgumentException("Registre ao menos um personagem presente no ritual.");
   out.put("completed",true).put("present_character_ids",ids).put("xp_award",120);
  }
  return out.put("next_room",step<9?HELIX_ROOMS[step]:"B1: depositar o ícone fechado").put("source_ref","Kingmaker p.413");
 }
 public static int helixSaveStatusBonus(JSONObject ritual,long heroId,boolean inHouse,String effect){
  if(!ritual.optBoolean("completed")||!inHouse||!("teleportation".equals(effect)||"banishment".equals(effect)))return 0;
  JSONArray present=ritual.optJSONArray("present_character_ids");if(present==null)return 0;
  for(int i=0;i<present.length();i++)if(String.valueOf(heroId).equals(present.optString(i)))return 4;
  return 0;
 }
}


