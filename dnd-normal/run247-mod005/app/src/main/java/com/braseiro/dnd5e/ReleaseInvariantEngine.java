package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Final invariant cross-check: independent release surfaces must agree before packaging. */
public final class ReleaseInvariantEngine{
 private ReleaseInvariantEngine(){}
 public static JSONObject audit(){JSONObject o=new JSONObject();JSONArray p=new JSONArray();try{
  JSONObject g=ReleaseGateEngine.evaluate(),m=ReleaseEvidenceManifestEngine.report(),h=ReleaseAuthorityHealthEngine.report(),q=ExactMissingContentQueueEngine.report();
  int qc=q.optInt("count",-1),mc=m.optInt("exact_missing_content_count",-2),gc=g.optInt("exact_missing_content_count",-3),hc=h.optInt("exact_missing_content_count",-4);
  if(qc<0||qc!=mc||qc!=gc||qc!=hc)p.put("exact_missing_count_disagreement");
  if(g.optBoolean("release_allowed",false)!=m.optBoolean("release_allowed",false))p.put("release_decision_disagreement");
  if(qc>0&&g.optBoolean("release_allowed",false))p.put("release_allowed_with_missing_content");
  if(!h.optBoolean("healthy",false)&&g.optBoolean("release_allowed",false))p.put("release_allowed_with_unhealthy_authority");
  return o.put("schema","release_invariants_v1").put("valid",p.length()==0).put("problem_count",p.length()).put("problems",p).put("exact_missing_content_count",qc).put("release_allowed",g.optBoolean("release_allowed",false)).put("repair_performed",false).put("auto_promotion",false);
 }catch(Exception e){try{return o.put("schema","release_invariants_v1").put("valid",false).put("problem_count",1).put("authority","invariant_audit_error");}catch(Exception ignored){return o;}}}
}
