package com.braseiro.dnd5e;
import org.json.*;
/** Deterministic encounter condition durations layered over persistent statblock state. */
public final class StatblockConditionRuntimeEngine{
 private StatblockConditionRuntimeEngine(){}
 public static synchronized JSONObject apply(String token,String condition,int rounds){
  if(rounds<1)return block("invalid_condition_duration");
  JSONObject v=StatblockEncounterStateEngine.view(token);if(!v.optBoolean("found"))return block("token_not_found");
  JSONObject r=StatblockEncounterStateEngine.condition(token,condition,true);if(r.optBoolean("blocked"))return r;
  try{return StatblockEncounterStateEngine.setConditionRounds(token,condition,rounds).put("applied",true);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
 }
 public static synchronized JSONObject tick(String token){/*BR_JSON_WRAP_106*/try{
  JSONObject v=StatblockEncounterStateEngine.view(token);if(!v.optBoolean("found"))return block("token_not_found");
  JSONObject timers=v.optJSONObject("condition_rounds");JSONArray expired=new JSONArray();if(timers==null)return v.put("expired_conditions",expired);
  java.util.Iterator<String> it=timers.keys();java.util.ArrayList<String> keys=new java.util.ArrayList<>();while(it.hasNext())keys.add(it.next());
  for(String c:keys){int n=timers.optInt(c,0)-1;if(n<=0){StatblockEncounterStateEngine.condition(token,c,false);StatblockEncounterStateEngine.clearConditionRounds(token,c);expired.put(c);}else StatblockEncounterStateEngine.setConditionRounds(token,c,n);}
  return StatblockEncounterStateEngine.view(token).put("expired_conditions",expired).put("ticked",true);
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 private static JSONObject block(String r){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("blocked",true).put("reason",r);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
