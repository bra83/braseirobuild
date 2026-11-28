package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;

/** Canonical NPC portrait identity and presentation. Database is the source of truth. */
final class NpcPortraitBinding {
 private NpcPortraitBinding(){}
 static String displayUri(JSONObject sceneNpc,JSONObject memory){
  if(memory!=null&&memory.length()>0)return memory.optString("token_uri","");
  return sceneNpc==null?"":sceneNpc.optString("token_uri","");
 }
 static JSONObject ensureIdentity(AppDb db,JSONObject npc)throws Exception{
  if(npc==null||npc.optLong("id")<=0)throw new Exception("NPC não encontrado.");
  if(npc.optString("stable_id","").trim().isEmpty()){
   db.upsertNpcMemory(npc);
   npc=db.getNpcMemory(npc.optLong("id"));
  }
  if(npc.optString("stable_id","").trim().isEmpty())throw new Exception("Identidade do NPC não pôde ser normalizada.");
  return npc;
 }
 static JSONObject resolveTarget(AppDb db,long id,String expected,String requestedName)throws Exception{
  JSONObject npc=db.getNpcMemory(id);
  String canonical=SceneNpcRoleAliases.canonicalName(requestedName,db.getSceneState());
  String stable=expected==null?"":expected.trim();
  String expectedCanonical=NpcIdentityNormalizer.stableId(db.getActiveMapProfile(),canonical);
  // Only recover legacy empty IDs or a row renumbered with the same stable identity.
  if(npc.length()==0||(!stable.isEmpty()&&!stable.equals(npc.optString("stable_id")))){
   JSONObject same=db.findNpcMemoryByName(canonical);
   if(same.length()>0&&(stable.equals(same.optString("stable_id"))||
       (stable.equals(expectedCanonical)&&same.optString("stable_id").isEmpty())))npc=same;
  }
  if(npc.length()==0||!NpcIdentityNormalizer.key(canonical).equals(NpcIdentityNormalizer.key(npc.optString("name"))))
   throw new Exception("A identidade do retrato não corresponde ao NPC escolhido.");
  npc=ensureIdentity(db,npc);
  if(stable.isEmpty())stable=expectedCanonical;
  JSONObject stats;try{stats=new JSONObject(npc.optString("stats_json","{}"));}catch(Exception e){stats=new JSONObject();}
  boolean dead=stats.optBoolean("dead",false)||"dead".equalsIgnoreCase(stats.optString("status"))||"morto".equalsIgnoreCase(stats.optString("status"));
  PersistentPortraitImportPolicy.Result admission=PersistentPortraitImportPolicy.validate("NPC",npc.optLong("id"),stable,npc.optString("stable_id"),npc.optInt("active",1)!=0,dead);
  if(!admission.accepted)throw new Exception(admission.message);
  return npc;
 }
 static void bindVerified(AppDb db,long id,String uri)throws Exception{
  db.setNpcToken(id,uri);
  JSONObject actual=db.getNpcMemory(id);
  if(actual.length()==0||!uri.equals(actual.optString("token_uri")))throw new Exception("O banco não confirmou o retrato do NPC.");
 }
 static JSONArray refreshCast(JSONArray cast,JSONObject memory,JSONObject scene)throws Exception{
  JSONArray out=new JSONArray();
  if(cast==null)return out;
  for(int i=0;i<cast.length();i++){
   JSONObject n=cast.optJSONObject(i);
   if(n==null){out.put(cast.opt(i));continue;}
   n=new JSONObject(n.toString());
   String name=SceneNpcRoleAliases.canonicalName(n.optString("name"),scene);
   if(!n.optBoolean("group")&&NpcIdentityNormalizer.key(name).equals(NpcIdentityNormalizer.key(memory.optString("name")))){
    n.put("name",memory.optString("name")).put("token_uri",memory.optString("token_uri"));
    n.remove("anonymous");n.remove("transient");
   }
   out.put(n);
  }
  return SceneNpcRoleAliases.reconcileCast(out,scene);
 }
}