package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Compact factual closure diagnostic; never converts missing source authority into progress. */
public final class ReleaseClosureDiagnosticEngine{
 private ReleaseClosureDiagnosticEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{JSONObject a=ContentExecutionAuditEngine.audit(),t=ContentCompletionTargetEngine.targets();JSONObject s=a.optJSONObject("spells"),i=a.optJSONObject("items"),c=a.optJSONObject("character_options"),b=a.optJSONObject("statblocks");o.put("spell",line(s)).put("item",line(i)).put("subclass",line(c)).put("statblock",line(b));o.put("authorized_source_records_still_required",!FinalReleaseEvidenceEngine.report().optBoolean("content_complete",false)).put("release_100_allowed",FinalReleaseEvidenceEngine.report().optBoolean("release_100_allowed",false)).put("policy","no invented mechanics; infrastructure may expand only to make authorized content executable");}catch(Exception ignored){}return o;}
 private static JSONObject line(JSONObject x){/*BR_JSON_WRAP_106*/try{if(x==null)x=new JSONObject();return new JSONObject().put("known",x.optInt("known",0)).put("executable",x.optInt("executable",0)).put("expected",x.opt("expected")).put("pending",x.opt("pending")).put("complete",x.optBoolean("complete",x.optBoolean("feature_effects_compiled",false)));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
