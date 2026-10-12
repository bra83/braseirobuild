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
  for(JSONObject q:candidates)if(!retiredQuest(q)&&(!official(q)||officialChapter(chapterKey(q))>0)&&ready(q,all))out.add(q);
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
    if(resolutions.length()>0)next.put("journal_started",true);
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


