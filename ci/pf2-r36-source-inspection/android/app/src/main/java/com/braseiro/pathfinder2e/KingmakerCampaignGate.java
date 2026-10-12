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
  for(JSONObject q:candidates)if((!official(q)||officialChapter(chapterKey(q))>0)&&ready(q,all))out.add(q);
  return out;
 }
 public static JSONObject preserveDefinition(JSONObject before,JSONObject change){
  if(!official(before))return change;
  try{
   JSONObject result=new JSONObject(change.toString());
   if(!result.has("progress_json"))return result;
   JSONObject prior=progress(before),next=progress(result);
   for(String field:new String[]{"quest_key","chapter_key","milestone_xp","prerequisites","rewards"})if(prior.has(field))next.put(field,prior.opt(field));
   result.put("progress_json",next.toString());return result;
  }catch(Exception e){throw new IllegalArgumentException("Não foi possível preservar a definição da missão.",e);}
 }
 public static void requireTransition(JSONObject before,String next,List<JSONObject> all){
  if(!official(before))return;
  if(completed(before.optString("status"))&&!completed(next))throw new IllegalStateException("Uma missão concluída não pode ser reaberta por uma atualização antiga.");
  if(("active".equals(next)||"in_progress".equals(next)||completed(next))&&!ready(before,all))
    throw new IllegalStateException("Conclua os pré-requisitos desta missão antes de avançar.");
 }
 public static int officialChapter(String key){
  if(key==null||key.startsWith("chapter_11_")||key.startsWith("chapter_12_"))return 0;
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("^chapter_(\\d+)_").matcher(key);
  if(!m.find())return 0;
  int legacy=Integer.parseInt(m.group(1));return legacy>=0&&legacy<=10?legacy+1:0;
 }
 public static String officialTitle(int chapter){
  String[] titles={"","Convocação de Heróis","Rumo aos Ermos","Terras Roubadas","Rios de Sangue","O Culto do Florescimento","O Desaparecimento de Varnhold","Sangue por Sangue","A Guerra dos Reis dos Rios","Eles Espreitam Abaixo","O Som de Mil Gritos","A Maldição do Rei Lanterna"};
  return chapter>=1&&chapter<=11?titles[chapter]:"Conteúdo complementar";
 }
}

