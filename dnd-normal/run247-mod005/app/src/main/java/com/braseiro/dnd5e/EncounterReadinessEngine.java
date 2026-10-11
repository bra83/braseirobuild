package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Builds encounter candidates from persistent population without fabricating combat mechanics. */
public final class EncounterReadinessEngine{
 private EncounterReadinessEngine(){}
 public static JSONObject at(JSONObject livingWorld,String location){JSONObject out=new JSONObject();JSONArray ready=new JSONArray(),blocked=new JSONArray();try{
  JSONObject population=livingWorld==null?null:livingWorld.optJSONObject("population");JSONArray actors=WorldPopulationEngine.at(population==null?WorldPopulationEngine.empty():population,location==null?"":location);
  for(int i=0;i<actors.length();i++){JSONObject a=actors.optJSONObject(i);if(a==null)continue;String sid=a.optString("statblock_id");boolean ok=CompiledStatblockEngine.has(sid);JSONObject e=new JSONObject(a.toString());e.put("combat_ready",ok).put("encounter_authority",ok?"authorized_compiled_statblock":"blocked_missing_authorized_compiled_statblock");if(ok)e.put("statblock",CompiledStatblockEngine.get(sid));if(ok)ready.put(e);else blocked.put(e);}
  out.put("location",location==null?"":location).put("actors_seen",actors.length()).put("combat_ready",ready).put("blocked",blocked).put("auto_start_combat",false).put("requires_barbara_review",true);
 }catch(Exception ignored){}return out;}
}
