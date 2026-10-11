package com.braseiro.dnd5e;
import org.json.JSONArray;import org.json.JSONObject;
/** Converts the last release blockers into an executable, evidence-bound closure queue. */
public final class FinalFivePercentPlanEngine{
 private FinalFivePercentPlanEngine(){}
 public static JSONObject report(){JSONObject o=new JSONObject();try{
  JSONObject evidence=FinalReleaseEvidenceEngine.report();JSONArray missing=evidence.optJSONArray("missing_authority");if(missing==null)missing=new JSONArray();JSONArray queue=new JSONArray();
  for(int i=0;i<missing.length();i++){String id=missing.optString(i);JSONObject q=new JSONObject().put("blocker",id).put("status","WAITING_AUTHORIZED_SOURCE").put("auto_promote",false);
   if("authorized_subclass_feature_procedures".equals(id))q.put("target","CharacterOptionCatalogEngine").put("accepts","exact feature procedure + source locator");
   else if("authorized_spell_effect_procedures".equals(id))q.put("target","SpellItemCatalogEngine/spell").put("accepts","exact spell effect procedure + source locator");
   else if("authorized_item_effect_procedures".equals(id))q.put("target","SpellItemCatalogEngine/item").put("accepts","exact item effect procedure + source locator");
   else if("authorized_statblock_records".equals(id))q.put("target","CompiledStatblockEngine").put("accepts","authorized complete statblock record");
   queue.put(q);
  }
  o.put("progress_percent",missing.length()==0?100:95).put("remaining_blockers",missing.length()).put("closure_queue",queue);
  o.put("architecture_expansion_allowed",true).put("architecture_expansion_policy","allowed_when_required_for_game_completeness").put("mechanical_authority_required",true).put("only_close_existing_blockers",false).put("release_100_allowed",evidence.optBoolean("release_100_allowed",false));
 }catch(Exception ignored){}return o;}
 public static JSONObject ingestGate(String kind,String id,JSONObject recordOrBasis){JSONObject r=new JSONObject();try{
  String k=kind==null?"":kind.trim().toLowerCase(),n=id==null?"":id.trim();boolean supported="subclass".equals(k)||"spell".equals(k)||"item".equals(k)||"statblock".equals(k)||"statblock_scope".equals(k);boolean basis=false;String authority="blocked_unverified_authority";
  if("subclass".equals(k)||"spell".equals(k)||"item".equals(k)){JSONObject g=CompiledContentMechanicsEngine.authorityCheck(k,n,recordOrBasis);basis=g.optBoolean("authorized",false);authority=g.optString("authority",authority);}
  else if("statblock".equals(k)){JSONObject raw=recordOrBasis==null?new JSONObject():new JSONObject(recordOrBasis.toString());if(raw.optString("id").trim().isEmpty())raw.put("id",n);JSONObject g=CompiledStatblockEngine.authorityCheck(raw);basis=g.optBoolean("authorized",false);authority=g.optString("authority",authority);}
  else if("statblock_scope".equals(k)){JSONObject raw=recordOrBasis==null?new JSONObject():recordOrBasis;String rid=raw.optString("id",n).trim(),book=raw.optString("source_book").trim(),loc=raw.optString("source_locator").trim();basis=rid.length()>=3&&!book.isEmpty()&&!loc.isEmpty()&&!"AUTHORIZED_SOURCE_REQUIRED".equals(book)&&!"AUTHORIZED_SOURCE_REQUIRED".equals(loc);authority=basis?"authorized_identity_scope_ready":"blocked_incomplete_scope_authority";}
  r.put("kind",k).put("id",n).put("supported_blocker_kind",supported).put("mechanical_basis_verified",basis).put("may_compile",supported&&basis).put("authority",supported&&basis?authority:(supported?authority:"blocked_unsupported_blocker_kind"));
 }catch(Exception ignored){}return r;}
}
