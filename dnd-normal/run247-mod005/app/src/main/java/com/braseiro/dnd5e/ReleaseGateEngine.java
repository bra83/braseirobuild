package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Final fail-closed release gate. This is intentionally stricter than runtime readiness. */
public final class ReleaseGateEngine{
 private ReleaseGateEngine(){}
 public static JSONObject evaluate(){JSONObject o=new JSONObject();try{JSONObject h=ReleaseAuthorityHealthEngine.report(),p=ProjectClosureEngine.report(),a=ContentExecutionAuditEngine.audit();boolean content=a.optBoolean("safe_to_claim_content_complete",false),project=p.optBoolean("ready_for_final_release",false),health=h.optBoolean("healthy",false),ready=content&&project&&health&&h.optInt("exact_missing_content_count",1)==0;return o.put("release_allowed",ready).put("content_complete",content).put("project_closure_ready",project).put("authority_healthy",health).put("exact_missing_content_count",h.optInt("exact_missing_content_count",-1)).put("false_100_percent_forbidden",!ready).put("authority",ready?"authorized_release_gate_pass":"blocked_release_gate").put("health",h);}catch(Exception e){try{return o.put("release_allowed",false).put("false_100_percent_forbidden",true).put("authority","blocked_release_gate_error");}catch(Exception ignored){return o;}}}
}
