package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
/** Role aliases are scoped to the scene, never global. */
final class SceneNpcRoleAliases {
 private SceneNpcRoleAliases(){}
 private static String fold(String s){return NpcIdentityNormalizer.key(s);}
 private static String role(String s){return fold(s).replaceFirst("^(o|a) ","");}
 private static boolean keeper(String s){return role(s).matches("estalajadeiro|dono da pousada|proprietario da pousada");}
 static void reconcile(JSONObject envelope,JSONObject prior)throws Exception{
  JSONObject scene=envelope.optJSONObject("scene_state");
  JSONArray old=prior==null?null:prior.optJSONArray("present_npcs");
  if(old==null&&prior!=null)try{old=new JSONArray(prior.optString("present_npcs_json","[]"));}catch(Exception ignored){}
  JSONArray cast=scene==null?null:scene.optJSONArray("present_npcs"), updates=envelope.optJSONArray("npc_updates");
  Map<String,String> names=new LinkedHashMap<String,String>();
  collect(names,old);collect(names,cast);collect(names,updates);
  if(LiveMasterAcceptance.waystone(prior,"")||LiveMasterAcceptance.waystone(scene,"")){
   names.put("estalajadeiro","Kote");names.put("dono da pousada","Kote");names.put("proprietario da pousada","Kote");
  }
  if(cast!=null)scene.put("present_npcs",merge(cast,names));
  if(updates!=null)envelope.put("npc_updates",merge(updates,names));
 }
 static String canonicalName(String name,JSONObject scene){
  return keeper(name)&&LiveMasterAcceptance.waystone(scene,"")?"Kote":name;
 }
 static boolean isKnownRoleAlias(JSONObject npc,JSONArray cast,JSONObject scene){
  if(npc==null||npc.optBoolean("group")||npc.optInt("count",1)>1)return false;
  String name=npc.optString("name"),canonical=canonicalName(name,scene);
  Map<String,String> names=new LinkedHashMap<String,String>();collect(names,cast);
  String known=names.get(role(name));
  if(!canonical.equals(name))known=canonical;
  if(known==null||known.isEmpty())return false;
  for(int i=0;i<cast.length();i++){JSONObject n=cast.optJSONObject(i);if(n!=null&&fold(known).equals(fold(n.optString("name"))))return true;}
  return false;
 }
 static JSONArray reconcileCast(JSONArray cast,JSONObject prior)throws Exception{
  JSONObject env=new JSONObject().put("scene_state",new JSONObject().put("present_npcs",cast));
  reconcile(env,prior);
  return env.getJSONObject("scene_state").getJSONArray("present_npcs");
 }
 private static void collect(Map<String,String> names,JSONArray list){
  if(list==null)return;
  for(int i=0;i<list.length();i++){
   JSONObject n=list.optJSONObject(i);if(n==null||n.optBoolean("anonymous")||n.optBoolean("group"))continue;
   String name=n.optString("name"),r=role(n.optString("role"));
   if(r.isEmpty()||name.isEmpty()||role(name).equals(r)||keeper(name)||r.matches("npc|npc presente|grupo presente"))continue;
   if(!names.containsKey(r))names.put(r,name);
   else if(!fold(names.get(r)).equals(fold(name)))names.put(r,"");
  }
 }
 private static JSONArray merge(JSONArray list,Map<String,String> aliases)throws Exception{
  JSONArray out=new JSONArray();Map<String,JSONObject> named=new LinkedHashMap<String,JSONObject>();
  for(int i=0;i<list.length();i++){
   Object value=list.opt(i);if(!(value instanceof JSONObject)){out.put(value);continue;}
   JSONObject n=new JSONObject(value.toString());String name=n.optString("name"),alias=aliases.get(role(name));
   if(alias!=null&&!alias.isEmpty()&&!n.optBoolean("group")&&n.optInt("count",1)<=1){
    n.put("name",alias);n.remove("stable_id");n.remove("anonymous");n.remove("transient");name=alias;
   }
   if(name.isEmpty()||n.optBoolean("group")||n.optBoolean("anonymous")){out.put(n);continue;}
   String k=fold(name);JSONObject existing=named.get(k);
   if(existing==null){named.put(k,n);out.put(n);}
   else{java.util.Iterator<String> keys=n.keys();while(keys.hasNext()){String key=keys.next();if(!existing.has(key)||existing.optString(key).isEmpty())existing.put(key,n.opt(key));}}
  }
  return out;
 }
}
