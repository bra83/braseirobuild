package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Evidence-based release gate. Missing copyrighted mechanics are blockers, never guessed or promoted from an index. */
public final class FinalReleaseEvidenceEngine{
 private FinalReleaseEvidenceEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{
  JSONObject closure=ProjectClosureEngine.report(), content=ContentExecutionAuditEngine.audit();JSONArray missing=new JSONArray();
  JSONObject spells=content.optJSONObject("spells"),items=content.optJSONObject("items"),feats=content.optJSONObject("feats"),stats=content.optJSONObject("statblocks"),chars=content.optJSONObject("character_options"),bookScope=content.optJSONObject("book_scope");
  if(chars==null||!chars.optBoolean("feature_effects_compiled",false))missing.put("authorized_subclass_feature_procedures");
  if(spells==null||spells.optInt("pending",0)>0)missing.put("authorized_spell_effect_procedures");
  if(items==null||items.optInt("pending",0)>0)missing.put("authorized_item_effect_procedures");
  if(feats==null||feats.optInt("pending",0)>0)missing.put("authorized_feat_effect_procedures");
  if(stats==null||!stats.optBoolean("complete",false))missing.put("authorized_statblock_records");
  if(bookScope==null||!bookScope.optBoolean("complete",false))missing.put("authorized_book_mechanics_scope");
  o.put("runtime_core_ready",true).put("blocker_diagnostic",ReleaseBlockerDiagnosticEngine.report()).put("content_complete",missing.length()==0).put("missing_authority",missing);
  o.put("fallback_policy","fail_closed_no_rule_invention").put("rag_may_explain_but_not_mutate",true);
  o.put("release_100_allowed",missing.length()==0&&closure.optBoolean("claim_100_percent_allowed",false));
 }catch(Exception ignored){}return o;}
}
