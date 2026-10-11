package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;import java.util.*;
/** Runtime registry for authorized spell/item/subclass mechanics. Registration is fail-closed on mechanical evidence. */
public final class CompiledContentMechanicsEngine{
 private static final Map<String,JSONObject> REG=new LinkedHashMap<String,JSONObject>(); private CompiledContentMechanicsEngine(){}
 public static synchronized JSONObject register(String kind,String name,JSONObject mechanics,JSONObject basis){JSONObject r=new JSONObject();try{
  String k=normKind(kind),n=name==null?"":name.trim(); boolean catalogKnown=known(k,n); boolean authority=contentAuthority(k,n,basis);
  if(!catalogKnown)return r.put("registered",false).put("authority","blocked_unknown_catalog_content");
  if(!authority)return r.put("registered",false).put("authority","blocked_missing_authorized_source");
  if(mechanics==null||mechanics.length()==0)return r.put("registered",false).put("authority","blocked_empty_mechanics");
  String ky=key(k,n);int revision=basis.optInt("revision",1);if(revision<1)revision=1;JSONObject old=REG.get(ky);int oldRevision=old==null?0:old.optInt("revision",1);
  if(old!=null&&revision<oldRevision)return r.put("registered",false).put("authority","blocked_stale_revision").put("current_revision",oldRevision).put("requested_revision",revision);
  if(old!=null&&revision==oldRevision){String oldMechanics=old.optJSONObject("mechanics")==null?"":old.optJSONObject("mechanics").toString();if(!oldMechanics.equals(mechanics.toString()))return r.put("registered",false).put("authority","blocked_revision_conflict").put("current_revision",oldRevision);}
  JSONObject rec=new JSONObject().put("kind",k).put("name",n).put("mechanics",mechanics).put("mechanical_basis",basis).put("revision",revision).put("authority","compiled_content_mechanics");REG.put(ky,rec);
  return r.put("registered",true).put("authority","compiled_content_mechanics").put("kind",k).put("name",n).put("revision",revision).put("updated",old!=null&&revision>oldRevision);
 }catch(Exception e){try{return r.put("registered",false);}catch(Exception ignored){return r;}}}
 public static synchronized boolean has(String kind,String name){return REG.containsKey(key(normKind(kind),name));}
 public static synchronized JSONObject get(String kind,String name){JSONObject x=REG.get(key(normKind(kind),name));return x==null?new JSONObject():x;}
 public static synchronized int count(String kind){String k=normKind(kind);int c=0;for(JSONObject x:REG.values())if(k.equals(x.optString("kind")))c++;return c;}
 public static synchronized boolean hasEffectPipeline(String kind,String name){JSONObject x=REG.get(key(normKind(kind),name));return x!=null&&effectPipelineComplete(x.optJSONObject("mechanics"));}
 public static synchronized int countEffectPipelineComplete(String kind){String k=normKind(kind);int c=0;for(JSONObject x:REG.values())if(k.equals(x.optString("kind"))&&effectPipelineComplete(x.optJSONObject("mechanics")))c++;return c;}
 public static synchronized JSONArray ids(String kind){JSONArray a=new JSONArray();String k=normKind(kind);for(JSONObject x:REG.values())if(k.equals(x.optString("kind")))a.put(x.optString("name"));return a;}
 public static synchronized JSONObject restoreReplacing(JSONObject snapshot){REG.clear();return restore(snapshot);}
 public static synchronized JSONObject snapshot(){JSONObject o=new JSONObject();JSONArray a=new JSONArray();try{for(JSONObject x:REG.values())a.put(new JSONObject(x.toString()));o.put("schema","compiled_content_mechanics_v1").put("records",a).put("count",a.length());}catch(Exception ignored){}return o;}
 public static synchronized JSONObject restore(JSONObject snapshot){JSONObject r=new JSONObject();int restored=0,blocked=0;try{if(snapshot==null||!"compiled_content_mechanics_v1".equals(snapshot.optString("schema")))return r.put("restored",0).put("blocked",true).put("authority","blocked_invalid_snapshot_schema");JSONArray a=snapshot.optJSONArray("records");if(a==null)a=new JSONArray();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null){blocked++;continue;}JSONObject rr=register(x.optString("kind"),x.optString("name"),x.optJSONObject("mechanics"),x.optJSONObject("mechanical_basis"));if(rr.optBoolean("registered",false))restored++;else blocked++;}return r.put("restored",restored).put("blocked_records",blocked).put("authority",blocked==0?"authorized_snapshot_restore":"partial_snapshot_restore");}catch(Exception e){try{return r.put("restored",restored).put("blocked_records",blocked+1).put("authority","blocked_snapshot_restore_error");}catch(Exception ignored){return r;}}}
 public static JSONObject readiness(String kind,String name){String k=normKind(kind);boolean known=known(k,name),compiled=has(k,name),effect=hasEffectPipeline(k,name);JSONObject o=new JSONObject();try{o.put("known",known).put("kind",k).put("name",name).put("mechanics_compiled",compiled).put("source_bound_compiled",compiled).put("effect_pipeline_complete",effect).put("executable",effect).put("authority",effect?"compiled_content_effect_pipeline":(compiled?"source_bound_mechanics_requires_effect_pipeline":(known?"blocked_missing_authorized_mechanics":"blocked_unknown_catalog_content")));if(compiled)o.put("record",get(k,name));}catch(Exception ignored){}return o;}
 public static JSONObject authorityCheck(String kind,String name,JSONObject basis){/*BR_JSON_WRAP_106*/try{String k=normKind(kind),n=name==null?"":name.trim();boolean known=known(k,n),authorized=known&&contentAuthority(k,n,basis);return new JSONObject().put("kind",k).put("name",n).put("known",known).put("authorized",authorized).put("authority",!known?"blocked_unknown_catalog_content":authorized?"authorized_source_bound_content":"blocked_missing_authorized_source");}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 private static boolean contentAuthority(String kind,String name,JSONObject basis){
  if(basis==null)return false; boolean legacy=SupplementContentRegistry.verifyMechanicalBasis(basis),reviewed=AuthorizedMechanicalEvidenceRegistry.verify(kind,name,basis); if(!legacy&&!reviewed)return false;
  String bk=basis.optString("content_kind","").trim().toLowerCase(Locale.ROOT);
  String bn=basis.optString("content_name","").trim();
  if(!kind.equals(normKind(bk))||!normName(name).equals(normName(bn)))return false;
  return true;
 }
 private static boolean effectPipelineComplete(JSONObject mechanics){return mechanics!=null&&mechanics.optBoolean("effect_pipeline_complete",false);}
 private static boolean known(String k,String n){if("spell".equals(k)||"item".equals(k))return SpellItemCatalogEngine.lookup(n).optBoolean("known",false);if("subclass".equals(k)){JSONObject d=CharacterOptionCatalogEngine.lookupSubclass(n);return d.optBoolean("known",false);}if("feat".equals(k))return CharacterOptionCatalogEngine.lookupFeat(n).optBoolean("known",false);return false;}
 private static String normKind(String s){String x=s==null?"":s.trim().toLowerCase(Locale.ROOT);return "character_option".equals(x)?"subclass":x;}
 private static String normName(String n){return n==null?"":n.trim().toLowerCase(Locale.ROOT);}
 private static String key(String k,String n){return k+"|"+normName(n);}
}
