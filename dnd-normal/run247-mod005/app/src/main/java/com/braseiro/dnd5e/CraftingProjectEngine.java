package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Estado persistível de crafting; progresso temporal é separado da concessão do item. */
public final class CraftingProjectEngine {
    private CraftingProjectEngine(){}
    public static JSONObject create(String id,String crafter,String recipe,double workUnits,int deadlineDay){JSONObject o=new JSONObject();try{o.put("id",text(id)).put("crafter",text(crafter)).put("recipe",text(recipe)).put("required_work_units",Math.max(1,workUnits)).put("work_units",0d).put("deadline_day",deadlineDay).put("status","planned").put("materials",new JSONArray()).put("output_granted",false).put("version",1);}catch(Exception ignored){}return o;}
    public static JSONObject addWork(JSONObject project,double units,int currentDay){JSONObject o=copy(project);try{if("cancelled".equals(o.optString("status"))||"completed".equals(o.optString("status")))return o;double next=Math.min(o.optDouble("required_work_units",1),o.optDouble("work_units",0)+Math.max(0,units));o.put("work_units",next).put("last_work_day",currentDay).put("status",next>=o.optDouble("required_work_units",1)?"awaiting_authorized_resolution":"active");if(o.optInt("deadline_day",0)>0&&currentDay>o.optInt("deadline_day"))o.put("overdue",true);}catch(Exception ignored){}return o;}
    public static JSONObject resolveOutput(JSONObject project,String itemGrant,JSONObject basis){JSONObject o=copy(project);try{if(!"awaiting_authorized_resolution".equals(o.optString("status"))){o.put("resolution_error","work_incomplete");return o;}JSONObject mutation=new JSONObject().put("item_grant",text(itemGrant));JSONObject gate=DowntimeEconomyRulesEngine.validateMutation(mutation,basis);if(!gate.optBoolean("authorized")){o.put("resolution_error","mechanical_basis_required");return o;}o.put("status","completed").put("output_granted",true).put("output",text(itemGrant)).remove("resolution_error");}catch(Exception ignored){}return o;}
    public static JSONObject advanceDay(JSONObject project,int currentDay){JSONObject o=copy(project);try{int deadline=o.optInt("deadline_day",0);String status=o.optString("status");o.put("current_day",Math.max(1,currentDay));if(deadline>0&&currentDay>deadline&&!"completed".equals(status)&&!"cancelled".equals(status))o.put("overdue",true).put("overdue_days",currentDay-deadline);else if(deadline>0&&currentDay<=deadline)o.put("overdue",false).put("overdue_days",0);}catch(Exception ignored){}return o;}
    public static JSONObject pause(JSONObject project){JSONObject o=copy(project);try{if("active".equals(o.optString("status"))||"planned".equals(o.optString("status")))o.put("status","paused");}catch(Exception ignored){}return o;}
    public static JSONObject resume(JSONObject project){JSONObject o=copy(project);try{if("paused".equals(o.optString("status")))o.put("status","active");}catch(Exception ignored){}return o;}
    private static String text(String s){return s==null?"":s.trim();}private static JSONObject copy(JSONObject src){try{return src==null?new JSONObject():new JSONObject(src.toString());}catch(Exception e){return new JSONObject();}}
}
