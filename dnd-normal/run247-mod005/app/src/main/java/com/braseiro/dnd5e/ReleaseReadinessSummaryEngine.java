package com.braseiro.dnd5e;
import org.json.JSONObject;
/** One release truth surface for UI, handoff and QA. */
public final class ReleaseReadinessSummaryEngine{private ReleaseReadinessSummaryEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{JSONObject c=ProjectClosureEngine.report(),q=ContentClosureQueueEngine.report(),a=ContentExecutionAuditEngine.audit();return o.put("runtime_ready",true).put("content_complete",a.optBoolean("safe_to_claim_content_complete",false)).put("release_ready",c.optBoolean("ready_for_final_release",false)&&q.optInt("count",1)==0).put("exact_missing_content_count",q.optInt("count",0)).put("closure_queue",q).put("blocker_diagnostic",ReleaseBlockerDiagnosticEngine.report()).put("false_100_percent_forbidden",true).put("next_action","ingest_authorized_source_bound_content_records");}catch(Exception e){try{return o.put("release_ready",false);}catch(Exception ignored){return o;}}}
}
