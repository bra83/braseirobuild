package com.braseiro.dnd5e;
import org.json.*;
/** XMM-only Android gateway adapter; fail closed and never mutate state. */
public final class Xmm2025AttackConditionGateway {
 private Xmm2025AttackConditionGateway(){}
 public static JSONObject execute(String action,JSONObject in){
  try{
   JSONObject result;
   if("attack_condition_coverage".equals(action))result=Xmm2025AttackConditionEngine.coverage();
   else if("attack_condition_catalog".equals(action))return new JSONObject()
    .put("executed",true).put("auto_mutation_allowed",false)
    .put("result",Xmm2025AttackConditionEngine.catalog());
   else if("attack_condition_profile".equals(action))
    result=Xmm2025AttackConditionEngine.profile(in.optString("monster"),in.optString("action"));
   else if("attack_condition_resolve".equals(action))result=Xmm2025AttackConditionEngine.resolve(in);
   else return new JSONObject().put("executed",false).put("auto_mutation_allowed",false)
    .put("reason","unknown_attack_condition_action");
   boolean resolved="attack_condition_coverage".equals(action)||result.optBoolean("resolved",false)
    ||("attack_condition_profile".equals(action)&&result.length()>0);
   return new JSONObject().put("executed",resolved).put("auto_mutation_allowed",false).put("result",result);
  }catch(Exception e){
   try{return new JSONObject().put("executed",false).put("auto_mutation_allowed",false)
    .put("reason","attack_condition_gateway_error");}
   catch(Exception ignored){return new JSONObject();}
  }
 }
}
