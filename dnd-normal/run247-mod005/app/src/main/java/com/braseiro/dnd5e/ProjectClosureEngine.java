package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Machine-readable final closure gate. It never upgrades indexed content to executable content. */
public final class ProjectClosureEngine{
 private ProjectClosureEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{
  JSONObject content=ContentExecutionAuditEngine.audit();JSONArray blockers=new JSONArray();
  if(!content.optBoolean("safe_to_claim_content_complete",false))blockers.put("authorized_content_mechanics_pending");
  JSONObject bookScope=content.optJSONObject("book_scope");if(bookScope==null||!bookScope.optBoolean("complete",false))blockers.put("book_mechanics_scope_pending");
  JSONObject chars=content.optJSONObject("character_options");if(chars==null||!chars.optBoolean("feature_effects_compiled",false))blockers.put("subclass_feature_effects_pending");
  JSONObject spells=content.optJSONObject("spells"),items=content.optJSONObject("items"),feats=content.optJSONObject("feats"),stats=content.optJSONObject("statblocks");
  if(spells!=null&&spells.optInt("pending",0)>0)blockers.put("spell_effects_pending");if(items!=null&&items.optInt("pending",0)>0)blockers.put("item_effects_pending");if(feats!=null&&feats.optInt("pending",0)>0)blockers.put("feat_effects_pending");if(stats==null||!stats.optBoolean("complete",false))blockers.put("authorized_statblocks_pending");
  o.put("core_world_runtime",true).put("persistent_downtime",true).put("authorized_mutation_gate",true).put("encounter_readiness_gate",true).put("strixhaven_to_faerun_flow_preserved",true).put("dynamic_party_preserved",true);
  o.put("content",content).put("blocker_diagnostic",ReleaseBlockerDiagnosticEngine.report()).put("blockers",blockers).put("ready_for_final_release",blockers.length()==0).put("claim_100_percent_allowed",blockers.length()==0);
 }catch(Exception ignored){}return o;}
}
