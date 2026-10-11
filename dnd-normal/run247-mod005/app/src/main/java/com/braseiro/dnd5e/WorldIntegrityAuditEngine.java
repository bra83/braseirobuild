package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;import java.util.*;
/** Runtime integrity audit for persistent Barbara/Living World aggregates. */
public final class WorldIntegrityAuditEngine{
 private WorldIntegrityAuditEngine(){}
 public static JSONObject audit(JSONObject aggregate){JSONObject out=new JSONObject();JSONArray issues=new JSONArray();int n=0;try{if(aggregate==null){issues.put("aggregate_missing");n++;aggregate=new JSONObject();}String[] required={"npc_graphs","factions","economy","politics","missions","population","downtime_portfolio","cooking_state","crafting_projects","economy_ledger","group_patron","sidekicks","environmental_hazards","supernatural_regions","magical_phenomena"};for(String k:required)if(!aggregate.has(k)){issues.put("missing_"+k);n++;}int day=aggregate.optInt("day",0);if(day<0){issues.put("negative_day");n++;}out.put("ok",n==0).put("issue_count",n).put("issues",issues).put("day",Math.max(0,day)).put("repair_policy","report_only_no_mechanical_fabrication");}catch(Exception e){try{out.put("ok",false).put("issue_count",1).put("issues",new JSONArray().put("audit_exception"));}catch(Exception ignored){}}return out;}
}
