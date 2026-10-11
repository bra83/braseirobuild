package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Single authoritative runtime entrypoint for compiled supplement procedures. */
public final class SupplementRuntimeGateway{
 private SupplementRuntimeGateway(){}
 private static boolean deterministic(String ruleId){
  CompiledSupplementProcedureBootstrap.install();TashaSupplementalRuntimeEngine.install();JSONObject p=AuthorizedRuleProcedureEngine.get(ruleId);
  return p.length()>0&&!"state_rule_gate".equals(p.optString("procedure_type"));
 }
 public static JSONObject execute(String ruleId,JSONObject inputs){
  if(!deterministic(ruleId))return blocked(ruleId,"registered_requires_manual_resolution");
  return AuthorizedMechanicalMutationEngine.execute(ruleId,inputs==null?new JSONObject():inputs);
 }
 public static JSONObject mutationEnvelope(String ruleId,JSONObject inputs,JSONObject mutation){
  if(!deterministic(ruleId))return blocked(ruleId,"registered_requires_manual_resolution");
  return AuthorizedMechanicalMutationEngine.mutationEnvelope(ruleId,inputs==null?new JSONObject():inputs,mutation==null?new JSONObject():mutation);
 }
 public static JSONObject readiness(String ruleId,JSONObject inputs){
  CompiledSupplementProcedureBootstrap.install();TashaSupplementalRuntimeEngine.install();JSONObject gate=AuthorizedRuleProcedureEngine.gate(ruleId,inputs==null?new JSONObject():inputs);JSONObject p=AuthorizedRuleProcedureEngine.get(ruleId);JSONObject o=new JSONObject();try{
   boolean registered=gate.optBoolean("registered",false),det=registered&&!"state_rule_gate".equals(p.optString("procedure_type"));boolean ready=det&&gate.optBoolean("inputs_complete",false);
   o.put("rule_id",ruleId==null?"":ruleId).put("registered",registered).put("procedure_type",p.optString("procedure_type")).put("inputs_complete",gate.optBoolean("inputs_complete",false)).put("executable",ready).put("authority",ready?"authorized_compiled_procedure":(registered?(det?"blocked_missing_required_inputs":"registered_requires_manual_resolution"):"blocked_missing_compiled_rule"));
  }catch(Exception ignored){}return o;
 }
 private static JSONObject blocked(String ruleId,String authority){JSONObject o=new JSONObject();try{o.put("rule_id",ruleId==null?"":ruleId).put("executed",false).put("status","blocked").put("requires_resolution",true).put("auto_mutation_allowed",false).put("authority",authority);}catch(Exception ignored){}return o;}
}
