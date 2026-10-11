package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Persistent, deterministic NPC memory. Stores events, promises, debts and relationships without AI invention. */
public final class NpcMemoryGraphEngine {
    private NpcMemoryGraphEngine(){}
    public static JSONObject remember(JSONObject state, JSONObject event, int day){
        JSONObject out=cloneObj(state); try{
            JSONArray memories=arr(out,"memories"); JSONObject m=new JSONObject();
            String key=event==null?"":event.optString("key").trim(); if(key.length()==0) key="day:"+Math.max(1,day)+":"+normalize(event==null?"":event.optString("summary"));
            for(int i=0;i<memories.length();i++) if(key.equals(memories.optJSONObject(i)==null?"":memories.optJSONObject(i).optString("key"))) return out;
            m.put("key",key).put("day",Math.max(1,day)).put("summary",event==null?"":event.optString("summary"))
             .put("actor",event==null?"":event.optString("actor")).put("kind",event==null?"event":event.optString("kind","event"))
             .put("importance",clamp(event==null?50:event.optInt("importance",50))).put("resolved",event!=null&&event.optBoolean("resolved",false));
            memories.put(m); out.put("memories",memories).put("last_memory_day",Math.max(1,day));
            applyRelationship(out,event); applyCommitment(out,event,day);
        }catch(Exception ignored){} return out;
    }
    public static JSONObject resolveCommitment(JSONObject state,String key,int day){JSONObject out=cloneObj(state);try{JSONArray a=arr(out,"commitments");for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null&&key.equals(c.optString("key"))){c.put("status","resolved").put("resolved_day",Math.max(1,day));}}out.put("commitments",a);}catch(Exception ignored){}return out;}
    public static JSONObject advanceDay(JSONObject state,int day){JSONObject out=cloneObj(state);try{JSONArray a=arr(out,"commitments");for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null||!"open".equals(c.optString("status","open")))continue;int due=c.optInt("due_day",Integer.MAX_VALUE);if(due<=day)c.put("overdue",day>due).put("due_today",day==due).put("overdue_days",Math.max(0,day-due)).put("consequence_pending",true);}out.put("commitments",a).put("current_day",Math.max(1,day));}catch(Exception ignored){}return out;}
    public static JSONObject acknowledgeCommitment(JSONObject state,String key,int day){JSONObject out=cloneObj(state);try{JSONArray a=arr(out,"commitments");for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null&&key.equals(c.optString("key")))c.put("consequence_pending",false).put("reviewed_day",Math.max(1,day));}out.put("commitments",a);}catch(Exception ignored){}return out;}
    public static JSONArray dueCommitments(JSONObject state,int day){JSONArray due=new JSONArray();JSONArray a=arr(state,"commitments");for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null&&"open".equals(c.optString("status","open"))&&c.optInt("due_day",Integer.MAX_VALUE)<=day)due.put(c);}return due;}
    private static void applyRelationship(JSONObject out,JSONObject e)throws Exception{if(e==null)return;String actor=e.optString("actor").trim();int delta=e.optInt("relationship_delta",0);if(actor.length()==0||delta==0)return;JSONObject r=obj(out,"relationships");r.put(actor,Math.max(-100,Math.min(100,r.optInt(actor,0)+delta)));out.put("relationships",r);}
    private static void applyCommitment(JSONObject out,JSONObject e,int day)throws Exception{if(e==null)return;String text=e.optString("commitment").trim();if(text.length()==0)return;JSONArray a=arr(out,"commitments");String key=e.optString("commitment_key",normalize(text));for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null&&key.equals(c.optString("key")))return;}a.put(new JSONObject().put("key",key).put("text",text).put("created_day",Math.max(1,day)).put("due_day",e.optInt("due_day",Integer.MAX_VALUE)).put("status","open"));out.put("commitments",a);}
    private static JSONObject cloneObj(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONArray arr(JSONObject o,String k){JSONArray a=o==null?null:o.optJSONArray(k);return a==null?new JSONArray():a;}
    private static JSONObject obj(JSONObject o,String k){JSONObject a=o==null?null:o.optJSONObject(k);return a==null?new JSONObject():a;}
    private static int clamp(int v){return Math.max(0,Math.min(100,v));}
    private static String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");}
}
