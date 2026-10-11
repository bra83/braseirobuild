package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;import java.util.*;
/** Central compiled-first coverage/gate registry. It never fabricates missing mechanics. */
public final class CompiledMechanicsCoverageEngine{
 private CompiledMechanicsCoverageEngine(){} private static final Map<String,Set<String>> COVERAGE=new LinkedHashMap<>();
 static{register("world",new String[]{"npc_memory","faction_diplomacy","politics","mission_deadlines","population","economy_pressure","consequence_planning"});register("downtime",new String[]{"job_state","craft_project_state","trade_state","research_state","training_state","administration_state","social_state","economy_ledger","authorized_supplement_procedures","persistent_cooking_runtime","downtime_deadlines"});register("content",new String[]{"character_option_selection","spell_item_selection","statblock_gate"});}
 private static void register(String domain,String[] ids){COVERAGE.put(domain,new LinkedHashSet<String>(Arrays.asList(ids)));}
 public static boolean has(String domain,String id){Set<String>s=COVERAGE.get(domain);return s!=null&&s.contains(id);}
 public static JSONObject gate(String domain,String id){JSONObject o=new JSONObject();try{boolean ok=has(domain,id);o.put("domain",domain).put("mechanic",id).put("compiled",ok).put("authority",ok?"compiled_registry":"blocked_missing_compiled_rule");}catch(Exception ignored){}return o;}
 public static JSONObject descriptor(){JSONObject o=new JSONObject();try{for(Map.Entry<String,Set<String>>e:COVERAGE.entrySet()){JSONArray a=new JSONArray();for(String x:e.getValue())a.put(x);o.put(e.getKey(),a);}o.put("principle","unknown mechanics remain blocked; narrative/RAG cannot grant mechanical authority");}catch(Exception ignored){}return o;}
}
