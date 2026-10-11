package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Machine-readable manifest of the exact evidence required for a release decision. Report-only. */
public final class ReleaseEvidenceManifestEngine{
 private ReleaseEvidenceManifestEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();JSONArray blockers=new JSONArray();try{
  JSONObject gate=ReleaseGateEngine.evaluate(),health=ReleaseAuthorityHealthEngine.report(),queue=ExactMissingContentQueueEngine.report(),targets=ContentCompletionTargetEngine.targets(),bundle=CompiledContentPersistenceBundle.snapshot();
  JSONArray q=queue.optJSONArray("queue");for(int i=0;q!=null&&i<q.length();i++)blockers.put(q.optJSONObject(i));
  boolean scopeKnown=targets.optJSONObject("statblock")!=null&&targets.optJSONObject("statblock").optBoolean("target_known",false);
  return o.put("schema","release_evidence_manifest_v1").put("release_allowed",gate.optBoolean("release_allowed",false)).put("authority_healthy",health.optBoolean("healthy",false)).put("exact_missing_content_count",queue.optInt("count",0)).put("bestiary_scope_known",scopeKnown).put("blockers",blockers).put("targets",targets).put("persistence_schema",bundle.optString("schema")).put("false_100_percent_forbidden",!gate.optBoolean("release_allowed",false)).put("repair_performed",false).put("auto_promotion",false);
 }catch(Exception e){try{return o.put("schema","release_evidence_manifest_v1").put("release_allowed",false).put("false_100_percent_forbidden",true).put("authority","manifest_error");}catch(Exception ignored){return o;}}}
}
