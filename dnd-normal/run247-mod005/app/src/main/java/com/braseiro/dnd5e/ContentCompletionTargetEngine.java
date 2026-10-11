package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Declares auditable completion targets. Unknown totals remain open instead of being guessed. */
public final class ContentCompletionTargetEngine{
 private ContentCompletionTargetEngine(){}
 public static JSONObject targets(){JSONObject o=new JSONObject();try{
  JSONObject si=SpellItemCatalogEngine.descriptor(),co=CharacterOptionCatalogEngine.descriptor();
  o.put("spell",new JSONObject().put("expected",si.optInt("spell_count",0)).put("target_known",true));
  o.put("item",new JSONObject().put("expected",si.optInt("item_count",0)).put("target_known",true));
  o.put("subclass",new JSONObject().put("expected",co.optInt("subclasses",0)).put("target_known",true));
  o.put("feat",new JSONObject().put("expected",co.optInt("feats",0)).put("target_known",true));
  JSONObject bc=AuthorizedBestiaryScopeEngine.coverage();o.put("statblock",new JSONObject().put("expected",bc.opt("expected")).put("target_known",bc.optBoolean("target_known",false)).put("reason",bc.optBoolean("target_known",false)?"authorized_bestiary_scope_declared":"authorized_bestiary_scope_not_declared"));
 }catch(Exception ignored){}return o;}
 public static JSONObject coverage(String kind,int executable){JSONObject r=new JSONObject();try{JSONObject t=targets().optJSONObject(kind);boolean known=t!=null&&t.optBoolean("target_known",false);int expected=known?t.optInt("expected",0):-1;r.put("kind",kind).put("target_known",known).put("expected",known?expected:JSONObject.NULL).put("executable",Math.max(0,executable)).put("pending",known?Math.max(0,expected-executable):JSONObject.NULL).put("complete",known&&expected>0&&executable>=expected);if(!known)r.put("reason",t==null?"missing_completion_target":t.optString("reason","missing_completion_target"));}catch(Exception ignored){}return r;}
}
