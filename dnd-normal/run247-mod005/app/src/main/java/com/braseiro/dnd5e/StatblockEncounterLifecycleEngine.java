package com.braseiro.dnd5e;
import org.json.*;
/** Defeat-aware encounter lifecycle layered on the persistent statblock turn/combat engines. */
public final class StatblockEncounterLifecycleEngine{
 private StatblockEncounterLifecycleEngine(){}
 public static synchronized JSONObject nextLivingTurn(){/*BR_JSON_WRAP_106*/try{
  JSONObject s=StatblockTurnEngine.state();if(s.optInt("round",0)<1)return block("encounter_not_started");
  JSONArray order=s.optJSONArray("order");if(order==null||order.length()==0)return block("initiative_required");
  for(int i=0;i<order.length();i++){
   JSONObject n=StatblockTurnEngine.next();String token=n.optString("current_token");JSONObject v=StatblockEncounterStateEngine.view(token);
   if(v.optBoolean("found")&&v.optInt("hp",0)>0){JSONObject tick=StatblockConditionRuntimeEngine.tick(token);return n.put("skipped_defeated",i).put("condition_tick",tick);}
  }
  return finish("no_living_combatants");
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 public static synchronized JSONObject status(){/*BR_JSON_WRAP_106*/try{
  JSONObject t=StatblockTurnEngine.state();JSONArray a=t.optJSONArray("order");JSONArray living=new JSONArray(),defeated=new JSONArray();
  if(a!=null)for(int i=0;i<a.length();i++){String token=a.optJSONObject(i).optString("token");JSONObject v=StatblockEncounterStateEngine.view(token);if(v.optBoolean("found")&&v.optInt("hp",0)>0)living.put(token);else defeated.put(token);}
  JSONObject teams=new JSONObject();for(int i=0;i<living.length();i++){JSONObject v=StatblockEncounterStateEngine.view(living.optString(i));String team=v.optString("team","hostile");teams.put(team,teams.optInt(team,0)+1);}
  int activeTeams=teams.length();
  return new JSONObject().put("active",t.optInt("round",0)>0).put("round",t.optInt("round",0)).put("current_token",t.optString("current_token")).put("living",living).put("defeated",defeated).put("living_count",living.length()).put("defeated_count",defeated.length()).put("teams",teams).put("active_team_count",activeTeams).put("resolved",a!=null&&a.length()>0&&activeTeams<=1);
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 public static synchronized JSONObject snapshot(){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("schema","statblock_encounter_bundle_v1").put("encounter",StatblockEncounterStateEngine.snapshot()).put("turns",StatblockTurnEngine.snapshot());}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 public static synchronized JSONObject restore(JSONObject s){/*BR_JSON_WRAP_106*/try{
  if(s==null||!"statblock_encounter_bundle_v1".equals(s.optString("schema")))return block("invalid_snapshot");
  JSONObject before=snapshot(),er=StatblockEncounterStateEngine.restore(s.optJSONObject("encounter")),tr=StatblockTurnEngine.restore(s.optJSONObject("turns"));
  if(er.optBoolean("blocked")||tr.optBoolean("blocked")){StatblockEncounterStateEngine.restore(before.optJSONObject("encounter"));StatblockTurnEngine.restore(before.optJSONObject("turns"));return block("atomic_restore_failed").put("encounter_restore",er).put("turn_restore",tr);}
  return status().put("restored",true);
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 private static JSONObject finish(String reason){/*BR_JSON_WRAP_106*/try{return status().put("resolved",true).put("reason",reason);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 private static JSONObject block(String r){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("blocked",true).put("reason",r);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
