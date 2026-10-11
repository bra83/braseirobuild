package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Final compiled-first readiness matrix. Selection/index coverage is never confused with executable mechanics. */
public final class CompiledContentReadinessEngine{
 private CompiledContentReadinessEngine(){}
 public static JSONObject audit(){JSONObject o=new JSONObject();try{
  JSONObject ca=ContentCompletionTargetEngine.coverage("subclass",CompiledContentMechanicsEngine.count("subclass")); o.put("character_options",section("selection_compiled",true,ca.optBoolean("complete",false),ca.optBoolean("complete",false)?"":"subclass_feature_effects"));
  JSONObject sc=ContentCompletionTargetEngine.coverage("spell",CompiledContentMechanicsEngine.count("spell")),ic=ContentCompletionTargetEngine.coverage("item",CompiledContentMechanicsEngine.count("item")); o.put("spells_items",section("selection_compiled",true,sc.optBoolean("complete",false)&&ic.optBoolean("complete",false),sc.optBoolean("complete",false)&&ic.optBoolean("complete",false)?"":"spell_item_effect_procedures"));
  JSONObject bc=AuthorizedBestiaryScopeEngine.coverage();boolean scopeKnown=bc.optBoolean("target_known",false),bestiaryComplete=bc.optBoolean("complete",false);String bestiaryPending=!scopeKnown?"authorized_bestiary_scope":(bestiaryComplete?"":"authorized_statblock_records");o.put("bestiary_npcs",section("registry_compiled",scopeKnown,bestiaryComplete,bestiaryPending).put("coverage",bc).put("scope_authority",bc.optString("authority")));
  o.put("downtime_economy",section("state_and_ledger_compiled",true,true,"authorized_outcome_rules_remain_gated_per procedure"));
  o.put("world_persistence",section("compiled",true,true,"AppDb aggregate + Barbara living-world systems"));
  o.put("content_execution",ContentExecutionAuditEngine.audit());
  o.put("safe_to_autoresolve",false).put("reason","only individually compiled mechanics may mutate protected outcomes");
 }catch(Exception ignored){}return o;}
 public static JSONObject requireExecutable(String domain,String mechanic,boolean executable){JSONObject r=new JSONObject();try{JSONObject gate=CompiledMechanicsCoverageEngine.gate(domain,mechanic);boolean registered=gate.optBoolean("compiled",false);r.put("registered",registered).put("executable",registered&&executable).put("authority",registered&&executable?"compiled_executable":"blocked_non_executable_or_unknown");}catch(Exception ignored){}return r;}
 private static JSONObject section(String status,boolean indexed,boolean executable,String pending){JSONObject x=new JSONObject();try{x.put("status",status).put("indexed",indexed).put("executable",executable).put("pending",pending);}catch(Exception ignored){}return x;}
}
