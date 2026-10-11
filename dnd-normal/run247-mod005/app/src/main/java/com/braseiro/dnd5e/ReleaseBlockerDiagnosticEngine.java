package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Exact release blocker diagnostic. It separates missing mechanics from unknown scope. */
public final class ReleaseBlockerDiagnosticEngine{
 private ReleaseBlockerDiagnosticEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{
  JSONObject audit=ContentExecutionAuditEngine.audit(),targets=ContentCompletionTargetEngine.targets();JSONArray blockers=new JSONArray();
  add(blockers,"subclass",audit.optJSONObject("character_options"),targets.optJSONObject("subclass"));
  add(blockers,"feat",audit.optJSONObject("feats"),targets.optJSONObject("feat"));
  add(blockers,"spell",audit.optJSONObject("spells"),targets.optJSONObject("spell"));
  add(blockers,"item",audit.optJSONObject("items"),targets.optJSONObject("item"));
  add(blockers,"statblock",audit.optJSONObject("statblocks"),targets.optJSONObject("statblock"));
  JSONObject books=audit.optJSONObject("book_scope");if(books==null||!books.optBoolean("complete",false))blockers.put(new JSONObject().put("kind","book_scope").put("target_known",books!=null&&books.optBoolean("target_known",false)).put("expected",JSONObject.NULL).put("pending",JSONObject.NULL).put("reason",books==null?"missing_book_scope_audit":books.optString("reason","book_mechanics_scope_pending")));
  o.put("blockers",blockers).put("blocker_count",blockers.length()).put("release_ready",blockers.length()==0).put("no_invented_scope",true);
 }catch(Exception ignored){}return o;}
 private static void add(JSONArray a,String kind,JSONObject coverage,JSONObject target)throws Exception{
  boolean targetKnown=target!=null&&target.optBoolean("target_known",false);boolean complete=coverage!=null&&coverage.optBoolean("complete",false);if(complete)return;
  JSONObject b=new JSONObject().put("kind",kind).put("target_known",targetKnown).put("executable",coverage==null?0:coverage.optInt("executable",0));
  if(targetKnown)b.put("expected",target.optInt("expected",0)).put("pending",coverage==null?target.optInt("expected",0):coverage.opt("pending")).put("reason","authorized_mechanics_missing");
  else b.put("expected",JSONObject.NULL).put("pending",JSONObject.NULL).put("reason",target==null?"missing_completion_target":target.optString("reason","missing_completion_target"));
  a.put(b);
 }
}
