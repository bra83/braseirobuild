package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Typed, atomic commit for Tasha's sidekick level rule (page 140). */
public final class CompiledSidekickLevelCommitContract {
 public static final String RULE_ID="tasha.sidekick.level";
 private CompiledSidekickLevelCommitContract() {}

 /** The Store adapter must use one SQLite transaction for every method in Work. */
 public interface Store {
  JSONObject atomic(Work work) throws Exception;
  JSONObject loadLivingWorld() throws Exception;
  boolean editionAllowed(String sidekickId) throws Exception;
  boolean levelGrantAuthorized(String sidekickId,int currentLevel,int nextLevel,String grantKey) throws Exception;
  int hitDieSides(String sidekickId,String statblockId) throws Exception;
  int constitutionModifier(String sidekickId,String statblockId) throws Exception;
  JSONObject dispatch(JSONObject request) throws Exception;
  boolean hasEffectReceipt(String key) throws Exception;
  void saveLivingWorld(JSONObject aggregate) throws Exception;
  void insertEffectReceipt(String key,String source) throws Exception;
 }
 public interface Work {JSONObject run() throws Exception;}

 public static final class Input {
  public final String sidekickId,operationToken,grantKey;
  public final int expectedLevel,hpDieRoll,day;
  public Input(String sidekickId,int expectedLevel,int hpDieRoll,int day,String operationToken,String grantKey) {
   this.sidekickId=sidekickId==null?"":sidekickId.trim();
   this.expectedLevel=expectedLevel;this.hpDieRoll=hpDieRoll;this.day=day;
   this.operationToken=operationToken==null?"":operationToken.trim();
   this.grantKey=grantKey==null?"":grantKey.trim();
  }
 }

 public static JSONObject commit(final Store store,final Input input) {
  if(store==null||input==null)return blocked("missing_store_or_input");
  try{return store.atomic(new Work(){public JSONObject run() throws Exception {return apply(store,input);}});}
  catch(Exception error){return errorResult(error);}
 }

 private static JSONObject apply(Store store,Input input) throws Exception {
  if(!input.sidekickId.matches("[A-Za-z0-9_-]{1,80}")||
    !input.operationToken.matches("[A-Za-z0-9_-]{8,128}")||
    !input.grantKey.matches("[A-Za-z0-9:_-]{8,180}")||input.day<1)
   return blocked("invalid_typed_input");
  String receipt="compiled-sidekick-level:"+input.sidekickId+":"+input.operationToken;
  if(store.hasEffectReceipt(receipt))return blocked("replayed");
  JSONObject aggregate=store.loadLivingWorld();
  if(aggregate==null)return blocked("missing_living_world");
  JSONObject sidekicks=aggregate.optJSONObject("sidekicks");
  JSONObject sidekick=sidekicks==null?null:sidekicks.optJSONObject(input.sidekickId);
  if(sidekick==null||!input.sidekickId.equals(sidekick.optString("id"))||
    !sidekick.optBoolean("friendship_established",false)||
    !"active".equals(sidekick.optString("status")))return blocked("ineligible_sidekick");
  int level=sidekick.optInt("level",0);
  if(level!=input.expectedLevel||level<1||level>=20)return blocked("stale_or_invalid_level");
  if(!store.editionAllowed(input.sidekickId))return blocked("edition_not_approved");
  if(!store.levelGrantAuthorized(input.sidekickId,level,level+1,input.grantKey))
   return blocked("missing_level_grant");
  String statblockId=sidekick.optString("statblock_id","");
  int sides=store.hitDieSides(input.sidekickId,statblockId);
  if(sides<1||input.hpDieRoll<1||input.hpDieRoll>sides)return blocked("invalid_hit_die_roll");
  int constitution=store.constitutionModifier(input.sidekickId,statblockId);
  if(constitution<-5||constitution>10)return blocked("invalid_constitution_modifier");
  JSONObject inputs=new JSONObject().put("current_level",level)
    .put("hp_die_roll",input.hpDieRoll).put("constitution_modifier",constitution);
  JSONObject request=new JSONObject().put("channel","PLAYER_REACTION")
    .put("action_id",input.operationToken).put("mechanic_family","supplement_procedure")
    .put("rule_id",RULE_ID).put("inputs",inputs);
  JSONObject dispatch=store.dispatch(request);
  JSONObject execution=dispatch==null?null:dispatch.optJSONObject("result");
  JSONObject rule=execution==null?null:execution.optJSONObject("result");
  JSONObject basis=execution==null?null:execution.optJSONObject("mechanical_basis");
  if(dispatch==null||!dispatch.optBoolean("accepted",false)||execution==null||
    !execution.optBoolean("executed",false)||!RULE_ID.equals(execution.optString("rule_id"))||
    !SupplementContentRegistry.verifyMechanicalBasis(basis)||rule==null)
   throw new IllegalStateException("compiled_dispatch_unresolved");
  JSONObject nextSidekicks=TashaCampaignSystemsEngine.levelSidekick(sidekicks,input.sidekickId,
    input.hpDieRoll,constitution,input.day);
  JSONObject next=nextSidekicks.optJSONObject(input.sidekickId);
  int hpGain=Math.max(1,input.hpDieRoll+constitution);
  if(rule.optInt("next_level",-1)!=level+1||rule.optInt("hp_gain",-1)!=hpGain||
    rule.optInt("next_proficiency_bonus",-1)!=next.optInt("proficiency_bonus",-2)||
    next.optInt("level",-1)!=level+1||
    next.optInt("hit_points_max",-1)!=sidekick.optInt("hit_points_max",0)+hpGain)
   throw new IllegalStateException("compiled_result_mismatch");
  JSONObject after=new JSONObject(aggregate.toString()).put("sidekicks",nextSidekicks);
  String source=RULE_ID+"|id="+input.sidekickId+"|from="+level+"|to="+(level+1)+
    "|hp_gain="+hpGain+"|grant="+input.grantKey;
  store.saveLivingWorld(after);
  store.insertEffectReceipt(receipt,source);
  return new JSONObject().put("applied",true).put("committed",true)
    .put("authority","compiled_tasha_sidekick_level")
    .put("receipt_key",receipt).put("receipt_source",source)
    .put("sidekick_id",input.sidekickId).put("level_after",level+1)
    .put("hit_points_max_after",next.optInt("hit_points_max"));
 }

 private static JSONObject errorResult(Exception error) {
  try{return blocked("transaction_rolled_back").put("error",error.getClass().getSimpleName())
    .put("detail",error.getMessage()==null?"":error.getMessage());}
  catch(Exception ignored){return new JSONObject();}
 }
 private static JSONObject blocked(String reason) {
  try{return new JSONObject().put("applied",false).put("reason",reason).put("auto_commit",false);}
  catch(Exception ignored){return new JSONObject();}
 }
}
