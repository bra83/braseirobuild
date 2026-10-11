package com.braseiro.dnd5e.adventure;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
/** In-memory SQLite-like adapter for host JVM contract tests; not real Android SQLite. */
public final class AdventureEngine {
 private static final Map<String,JSONObject> SAVED=new HashMap<>();
 private AdventureState active;
 private String key;
 public JSONObject loadOrStartAdventure(AdventureModule module,long party){
  key=module.getId()+"/"+party;
  JSONObject saved=SAVED.get(key);
  active=saved==null?new AdventureState("start"):AdventureState.fromJson(new JSONObject(saved.toString()));
  if(saved==null)SAVED.put(key,new JSONObject(active.toJson().toString()));
  return new JSONObject().put("status",saved==null?"started":"resumed");
 }
 public AdventureState getActiveState(){return active;}
 public JSONObject processUniversal(JSONObject cmd){
  AdventureState p=AdventureState.fromJson(new JSONObject(active.toJson().toString()));
  JSONObject result=UniversalRpgEngine.process(p,cmd);
  if(result.optBoolean("committed")){SAVED.put(key,new JSONObject(p.toJson().toString()));active=p;}
  return result;
 }
}
