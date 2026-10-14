package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Routes player requests to compiled resolvers without committing state. */
public final class CompiledPlayerActionDispatcher {
 private CompiledPlayerActionDispatcher() {}

 public static JSONObject dispatch(MasterRulesArbiter rules,JSONObject action) {
  JSONObject out=new JSONObject();
  try {
   if(rules==null)return reject(out,"blocked_missing_rules_arbiter");
   JSONObject request=action==null?new JSONObject():action;
   if(!"PLAYER_REACTION".equals(request.optString("channel","").trim()))return reject(out,"blocked_invalid_action_channel");
   String family=request.optString("mechanic_family","").trim();
   String actionId=request.optString("action_id","").trim();
   if(actionId.isEmpty())return reject(out,"blocked_missing_action_id");
   out.put("action_id",actionId);
   if("xmm_batch_preview".equals(family)) {
    // Player-originated preview: no direct write, no hidden roll.
    JSONObject batch=request.optJSONObject("batch");
    JSONObject wrapper=new OfficialBookRuntimeGateway(null).execute(
       "xmm","hybrid_batch_preview",batch);
    JSONObject preview=wrapper==null?null:wrapper.optJSONObject("result");
    if(preview==null||!preview.optBoolean("evaluated",false))
      return unresolved(out,family,preview,"blocked_hybrid_batch_preflight");
    return out.put("accepted",true).put("mechanic_family",family)
       .put("result",preview).put("auto_commit",false)
       .put("effect_applied",false).put("persisted",false)
       .put("requires_normal_arbiter_transaction",false)
       .put("host_resolution_required_for_mutation",true)
       .put("authority","xmm_pinned_read_only_preview");
   }
   if("supplement_procedure".equals(family)) {
    String id=request.optString("rule_id","");
    JSONObject inputs=request.optJSONObject("inputs");
    if(inputs==null)inputs=new JSONObject();
    JSONObject mutation=request.optJSONObject("requested_mutation");
    JSONObject ready=rules.compiledSupplementReadiness(id,inputs);
    if(ready==null||!ready.optBoolean("executable",false))return blocked(out,family,ready);
    JSONObject result=rules.executeCompiledSupplement(id,inputs,mutation);
    if(result==null||!result.optBoolean("executed",false))return unresolved(out,family,result,"blocked_runtime_unresolved");
    if(mutation!=null&&mutation.length()>0&&!result.optBoolean("mutation_authorized",false))
     return unresolved(out,family,result,"blocked_mutation_unauthorized");
    return accepted(out,family,result);
   }
   if("compiled_content".equals(family)) {
    String kind=request.optString("content_kind","").trim();
    String name=request.optString("content_name","").trim();
    if(kind.isEmpty()||name.isEmpty())return reject(out,"blocked_missing_content_identity");
    JSONObject ready=rules.compiledContentReadiness(kind,name);
    if(ready==null||!ready.optBoolean("executable",false))return blocked(out,family,ready);
    JSONObject result=rules.executeCompiledContent(kind,name,request.optJSONObject("context"));
    if(result==null||!result.optBoolean("executed",false))return unresolved(out,family,result,"blocked_runtime_unresolved");
    return accepted(out,family,result);
   }
   return reject(out,"blocked_unknown_mechanic_family");
  }catch(Exception error){
   try{return reject(out,"blocked_dispatch_error").put("error",error.getClass().getSimpleName());}
   catch(Exception ignored){return new JSONObject();}
  }
 }

 private static JSONObject reject(JSONObject out,String authority)throws Exception {
  return out.put("accepted",false).put("authority",authority);
 }
 private static JSONObject blocked(JSONObject out,String family,JSONObject readiness)throws Exception {
  JSONObject ready=readiness==null?new JSONObject():readiness;
  return reject(out,ready.optString("authority","blocked_missing_readiness"))
    .put("mechanic_family",family).put("readiness",ready);
 }
 private static JSONObject unresolved(JSONObject out,String family,JSONObject result,String authority)throws Exception {
  return reject(out,authority).put("mechanic_family",family)
    .put("result",result==null?new JSONObject():result);
 }
 private static JSONObject accepted(JSONObject out,String family,JSONObject result)throws Exception {
  CompiledActionExecutionDecision decision=CompiledActionExecutionDecision.from(result);
  if(decision==null)return unresolved(out,family,result,"blocked_runtime_unresolved");
  JSONObject contract=decision.toJson();
  return out.put("accepted",true).put("mechanic_family",family).put("result",result)
    .put("auto_commit",false).put("requires_normal_arbiter_transaction",decision.commitRequired)
    .put("effect_applied",decision.effectApplied)
    .put("execution_stage",contract.optString("execution_stage"))
    .put("authority",decision.authority);
 }
}
