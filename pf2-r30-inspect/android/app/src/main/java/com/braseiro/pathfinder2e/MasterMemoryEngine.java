package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Memória episódica e recuperação por relevância inteiramente locais. */
public final class MasterMemoryEngine {
    private final AppDb db;
    public MasterMemoryEngine(AppDb database){db=database;}

    public JSONArray relevant(String focus,int max){
        List<Scored> scored=new ArrayList<Scored>();String nf=normalize(focus);Set<String> terms=terms(nf);long now=System.currentTimeMillis();
        for(JSONObject e:db.listMasterEpisodes(220)){String hay=normalize(e.optString("action_text")+" "+e.optString("summary")+" "+e.optString("location")+" "+e.optString("tags_json"));int score=e.optInt("significance",1)*3;for(String t:terms)if(hay.contains(t))score+=4;long age=Math.max(0,now-e.optLong("created_at",now));if(age<24L*60L*60L*1000L)score+=4;else if(age<7L*24L*60L*60L*1000L)score+=2;if(score>3)scored.add(new Scored(e,score));}
        Collections.sort(scored,new Comparator<Scored>(){public int compare(Scored a,Scored b){return Integer.compare(b.score,a.score);}});JSONArray out=new JSONArray();List<JSONObject> sessions=db.listSessionMemories(3);for(int i=0;i<sessions.size()&&out.length()<Math.min(2,max);i++){JSONObject e=sessions.get(i);JSONObject x=new JSONObject();try{x.put("memory_type","session");x.put("session_no",e.optInt("session_no"));x.put("title",trim(e.optString("title"),120));x.put("summary",trim(e.optString("summary"),620));x.put("open_threads_json",e.optString("open_threads_json","[]"));x.put("character_beats_json",e.optString("character_beats_json","{}"));}catch(Exception ignored){AppLog.ignored("MasterMemoryEngine","Falha tolerada; a execução continuou.",ignored);}out.put(x);}for(int i=0;i<scored.size()&&out.length()<max;i++){JSONObject e=scored.get(i).value;JSONObject x=new JSONObject();try{x.put("memory_type","episode");x.put("summary",trim(e.optString("summary"),520));x.put("action",trim(e.optString("action_text"),260));x.put("actors_json",e.optString("actors_json","[]"));x.put("scene_mode",e.optString("scene_mode"));x.put("location",e.optString("location"));x.put("hex_id",e.optString("hex_id"));x.put("significance",e.optInt("significance"));}catch(Exception ignored){AppLog.ignored("MasterMemoryEngine","Falha tolerada; a execução continuou.",ignored);}out.put(x);}return out;
    }

    public void record(String action,String narration,JSONObject cognitivePlan,JSONObject envelope){
        if(narration==null||narration.trim().length()<40)return;try{JSONObject scene=db.getSceneState();JSONObject proposed=envelope==null?null:envelope.optJSONObject("scene_state");String location=proposed!=null&&proposed.optString("location").trim().length()>0?proposed.optString("location"):scene.optString("location");String hexId=proposed!=null&&proposed.optString("hex_id").trim().length()>0?proposed.optString("hex_id"):scene.optString("hex_id");JSONArray actors=cognitivePlan==null?new JSONArray():cognitivePlan.optJSONArray("actors");if(actors==null)actors=new JSONArray();JSONArray tags=extractTags(action+" "+narration);int significance=significance(action,narration,envelope);String summary=summarize(action,narration);db.addMasterEpisode(trim(action,900),summary,actors.toString(),cognitivePlan==null?"":cognitivePlan.optString("scene_mode"),location,hexId,tags.toString(),significance);
            JSONObject runtime=db.getMasterRuntimeState();runtime.put("beat_no",runtime.optInt("beat_no",0)+1);runtime.put("scene_mode",cognitivePlan==null?runtime.optString("scene_mode","exploracao"):cognitivePlan.optString("scene_mode","exploracao"));runtime.put("tension",cognitivePlan==null?runtime.optInt("tension",0):cognitivePlan.optInt("tension",0));runtime.put("last_summary",summary);runtime.put("last_cognitive_plan_json",cognitivePlan==null?"{}":cognitivePlan.toString());db.saveMasterRuntimeState(runtime);
        }catch(Exception ignored){AppLog.ignored("MasterMemoryEngine","Falha tolerada; a execução continuou.",ignored);}
    }

    public JSONObject diagnostic(){JSONObject out=new JSONObject();try{out.put("episodes",db.listMasterEpisodes(1000).size());out.put("session_memories",db.listSessionMemories(1000).size());out.put("runtime",db.getMasterRuntimeState());out.put("active",true);}catch(Exception ignored){AppLog.ignored("MasterMemoryEngine","Falha tolerada; a execução continuou.",ignored);}return out;}

    private static String summarize(String action,String narration){String clean=narration.replaceAll("\\s+"," ").trim();String[] sentences=clean.split("(?<=[.!?…])\\s+");StringBuilder b=new StringBuilder();if(action!=null&&action.trim().length()>0)b.append("Ação: ").append(trim(action,240)).append(". ");for(int i=0;i<sentences.length&&i<3;i++){if(b.length()>700)break;b.append(sentences[i].trim()).append(' ');}return trim(b.toString().trim(),850);}
    private static int significance(String action,String narration,JSONObject env){String n=normalize(action+" "+narration);int s=1;if(contains(n,"morre","morte","segredo","revel","missao","aliado","traicao","combate","vitória","derrota"))s+=2;if(env!=null&&((env.optJSONArray("knowledge_updates")!=null&&env.optJSONArray("knowledge_updates").length()>0)||(env.optJSONArray("quest_updates")!=null&&env.optJSONArray("quest_updates").length()>0)))s++;return Math.min(5,s);}
    private static JSONArray extractTags(String text){JSONArray out=new JSONArray();Set<String> seen=new HashSet<String>();for(String t:terms(normalize(text)))if(t.length()>=6&&seen.add(t)){out.put(t);if(out.length()>=12)break;}return out;}
    private static Set<String> terms(String text){Set<String> out=new HashSet<String>();for(String t:text.split(" "))if(t.length()>=4&&!"para como com uma esse essa onde quando ainda depois antes grupo mestre personagem".contains(t))out.add(t);return out;}
    private static boolean contains(String n,String... vs){for(String v:vs)if(n.contains(normalize(v)))return true;return false;}
    private static String trim(String v,int max){String s=v==null?"":v.trim();return s.length()<=max?s:s.substring(0,max)+"…";}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static final class Scored{final JSONObject value;final int score;Scored(JSONObject v,int s){value=v;score=s;}}
}
