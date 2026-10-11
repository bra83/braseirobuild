package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Economic world pressure. Produces indices/signals only; never invents item prices or character gold. */
public final class WorldEconomyPressureEngine {
 private WorldEconomyPressureEngine(){}
 public static JSONObject step(JSONObject state,JSONObject event,int day){JSONObject o=copy(state);try{int supply=o.optInt("supply",50),demand=o.optInt("demand",50),security=o.optInt("security",50),access=o.optInt("route_access",50);if(event!=null){supply+=event.optInt("supply_delta",0);demand+=event.optInt("demand_delta",0);security+=event.optInt("security_delta",0);access+=event.optInt("route_access_delta",0);}supply=c(supply);demand=c(demand);security=c(security);access=c(access);int pressure=c(50+(demand-supply)/2+(50-security)/4+(50-access)/4);o.put("supply",supply).put("demand",demand).put("security",security).put("route_access",access).put("market_pressure",pressure).put("last_day",Math.max(1,day)).put("price_mutation_allowed",false).put("price_rule","requires authorized compiled pricing rule");}catch(Exception ignored){}return o;}
 private static int c(int v){return Math.max(0,Math.min(100,v));}private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
}
