package com.braseiro.dnd5e;

import android.database.Cursor;
import org.json.JSONObject;

/** SQLite adapter for the typed Tasha sidekick level commit contract. */
public final class AppDbSidekickLevelStore implements CompiledSidekickLevelCommitContract.Store {
 private final AppDb db;
 public AppDbSidekickLevelStore(AppDb db) {
  if(db==null)throw new IllegalArgumentException("db_missing");
  this.db=db;
 }
 public JSONObject atomic(final CompiledSidekickLevelCommitContract.Work work) throws Exception {
  final JSONObject[] result={null};
  db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception {
   result[0]=work.run();
  }});
  return result[0]==null?new JSONObject():result[0];
 }
 public JSONObject loadLivingWorld(){return db.getLivingWorldAggregate();}
 public boolean editionAllowed(String sidekickId) {
  JSONObject sidekick=sidekick(sidekickId);
  String campaign=db.getCampaign().optString("rules_profile","").trim();
  return !campaign.isEmpty()&&campaign.equals(sidekick.optString("rules_profile",""))&&
    sidekick.optBoolean("tasha_rules_approved",false);
 }
 public boolean levelGrantAuthorized(String id,int current,int next,String grantKey) {
  if(!grantKey.equals("sidekick-level-grant:"+id+":"+next))return false;
  String expected="sidekick_level_grant_v1|id="+id+"|from="+current+"|to="+next;
  return expected.equals(receiptSource(grantKey));
 }
 public int hitDieSides(String id,String statblockId) {
  JSONObject statblock=sourceStatblock(id,statblockId);
  int sides=statblock.optInt("hit_die_sides",0);
  return sides==4||sides==6||sides==8||sides==10||sides==12||sides==20?sides:0;
 }
 public int constitutionModifier(String id,String statblockId) {
  JSONObject abilities=sourceStatblock(id,statblockId).optJSONObject("abilities");
  if(abilities==null)return Integer.MAX_VALUE;
  int score=abilities.optInt("con",abilities.optInt("CON",abilities.optInt("constitution",0)));
  return score>=1&&score<=30?Math.floorDiv(score-10,2):Integer.MAX_VALUE;
 }
 /** Resolve the compiled rule inside this Store's existing transaction; do not open
  * the generic dispatch transaction recursively or leave a dispatch-only receipt. */
 public JSONObject dispatch(JSONObject request) throws Exception {
  return CompiledPlayerActionDispatcher.dispatch(new MasterRulesArbiter(db),request);
 }
 public boolean hasEffectReceipt(String key){return db.hasEffectReceipt(key);}
 public void saveLivingWorld(JSONObject aggregate) {
  if(db.saveLivingWorldAggregate(aggregate)<=0)throw new IllegalStateException("living_world_save_failed");
 }
 public void insertEffectReceipt(String key,String source) {
  db.recordEffectReceipt(key,source);
  if(!source.equals(receiptSource(key)))throw new IllegalStateException("sidekick_receipt_conflict");
 }
 private JSONObject sidekick(String id) {
  JSONObject all=db.getLivingWorldAggregate().optJSONObject("sidekicks");
  return all==null?new JSONObject():all.optJSONObject(id)==null?new JSONObject():all.optJSONObject(id);
 }
 private JSONObject sourceStatblock(String id,String statblockId) {
  if(statblockId==null||statblockId.trim().isEmpty()||
    !statblockId.equals(sidekick(id).optString("statblock_id")))return new JSONObject();
  return CompiledStatblockEngine.get(statblockId);
 }
 private String receiptSource(String key) {
  Cursor cursor=db.getReadableDatabase().rawQuery(
    "SELECT source FROM effect_receipts WHERE world_profile=? AND effect_key=? LIMIT 1",
    new String[]{db.getActiveMapProfile(),key});
  try{return cursor.moveToFirst()?cursor.getString(0):"";}
  finally{cursor.close();}
 }
}
