package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.Locale;

/** Unified fail-closed runtime access for authorized compiled mechanics. */
public final class CompiledContentRuntimeGateway {
 private CompiledContentRuntimeGateway() {}

 public static JSONObject readiness(String kind, String name) {
  String k=norm(kind);
  if("procedure".equals(k)) {
   TashaSupplementalRuntimeEngine.install();
   JSONObject gate=AuthorizedRuleProcedureEngine.gate(name,new JSONObject());
   JSONObject record=AuthorizedRuleProcedureEngine.get(name);
   boolean supported="TashaSupplementalRuntimeEngine.resolve".equals(record.optString("runtime_resolver"));
   boolean ready=supported&&gate.optBoolean("executable",false);
   return out(k,name,record.length()>0,ready,ready?"authorized_compiled_procedure":
     supported?gate.optString("authority","blocked_missing_required_inputs"):"blocked_missing_runtime_resolver",ready?record:null);
  }
  if("statblock".equals(k)) {
   boolean found=CompiledStatblockEngine.has(name);
   return out(k,name,found,found,found?"authorized_compiled_statblock":"blocked_missing_authorized_compiled_statblock",found?CompiledStatblockEngine.get(name):null);
  }
  JSONObject record=CompiledContentMechanicsEngine.readiness(k,name);
  JSONObject mechanics=record.optJSONObject("record")==null?null:record.optJSONObject("record").optJSONObject("mechanics");
  String resolver=mechanics==null?"":mechanics.optString("runtime_resolver");
  boolean ready=record.optBoolean("executable",false)&&supported(resolver);
  String authority=ready?record.optString("authority","compiled_content_effect_pipeline"):
    record.optBoolean("executable",false)?"blocked_missing_runtime_resolver":record.optString("authority","blocked_missing_authorized_mechanics");
  return out(k,name,record.optBoolean("known",false),ready,authority,ready?record.optJSONObject("record"):null);
 }

 public static JSONObject execute(String kind, String name, JSONObject context) {
  JSONObject ready=readiness(kind,name),out=new JSONObject();
  String k=ready.optString("kind");
  try {
   out.put("kind",k).put("name",name==null?"":name).put("auto_mutation_allowed",false);
   if(!ready.optBoolean("executable",false))return blocked(out,ready.optString("authority"));
   if("statblock".equals(k))return out.put("executed",true).put("status","compiled_mechanics_ready")
     .put("requires_gameplay_resolution",true).put("mechanics",ready.optJSONObject("record"))
     .put("authority",ready.optString("authority"));
   // A resolver may update its input JSON. Keep that candidate state separate from the caller's live state.
   JSONObject input=context==null?new JSONObject():new JSONObject(context.toString());
   JSONObject result;
   if("procedure".equals(k))result=TashaSupplementalRuntimeEngine.resolve(name,input);
   else {
    JSONObject record=ready.optJSONObject("record");
    String resolver=record.optJSONObject("mechanics").optString("runtime_resolver");
    out.put("mechanics",record);
    result=resolve(resolver,name,input);
   }
   boolean resolved=result!=null&&result.optBoolean("resolved",false);
   out.put("runtime_result",result==null?new JSONObject():result).put("context",input)
     .put("executed",resolved).put("status",resolved?"compiled_mechanics_ready":"blocked")
     .put("requires_gameplay_resolution",resolved)
     .put("authority",resolved?ready.optString("authority"):"blocked_runtime_unresolved");
   return out;
  }catch(Exception error){
   try {return blocked(out,"blocked_runtime_error").put("error",error.getClass().getSimpleName());}
   catch(Exception ignored){return new JSONObject();}
  }
 }

 private static JSONObject resolve(String resolver,String name,JSONObject context) {
  if("TashaSpellRuntimeEngine.resolve".equals(resolver))return TashaSpellRuntimeEngine.resolve(name,context);
  if("TashaArtificerRuntimeEngine.resolveSubclass".equals(resolver))return TashaArtificerRuntimeEngine.resolveSubclass(name,context);
  if("TashaItemRuntimeEngine.resolve".equals(resolver))return TashaItemRuntimeEngine.resolve(name,context);
  if("TashaArtifactRuntimeEngine.resolve".equals(resolver))return TashaArtifactRuntimeEngine.resolve(name,context);
  if("TashaSubclassRuntimeEngine.resolveSubclass".equals(resolver))return TashaSubclassRuntimeEngine.resolveSubclass(name,context);
  if("CrossBookSubclassRuntimeEngine.resolve".equals(resolver))return CrossBookSubclassRuntimeEngine.resolve(name,context);
  return new JSONObject();
 }

 private static boolean supported(String resolver) {
  return "TashaSpellRuntimeEngine.resolve".equals(resolver)
    ||"TashaArtificerRuntimeEngine.resolveSubclass".equals(resolver)
    ||"TashaItemRuntimeEngine.resolve".equals(resolver)
    ||"TashaArtifactRuntimeEngine.resolve".equals(resolver)
    ||"TashaSubclassRuntimeEngine.resolveSubclass".equals(resolver)
    ||"CrossBookSubclassRuntimeEngine.resolve".equals(resolver);
 }

 private static JSONObject out(String kind,String name,boolean known,boolean executable,String authority,JSONObject record) {
  JSONObject out=new JSONObject();
  try {out.put("kind",kind).put("name",name==null?"":name).put("known",known)
    .put("executable",executable).put("auto_mutation_allowed",false).put("authority",authority);
   if(executable&&record!=null)out.put("record",record);
  }catch(Exception ignored){}
  return out;
 }
 private static JSONObject blocked(JSONObject out,String authority) throws Exception {
  return out.put("executed",false).put("status","blocked").put("auto_mutation_allowed",false).put("authority",authority);
 }
 private static String norm(String kind) {
  String k=kind==null?"":kind.trim().toLowerCase(Locale.ROOT);
  return "character_option".equals(k)?"subclass":k;
 }
}
