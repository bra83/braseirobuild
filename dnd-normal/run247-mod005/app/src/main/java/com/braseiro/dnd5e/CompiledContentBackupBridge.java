package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Coordinates the in-memory compiled registries with a BACKUP21 database import. */
public final class CompiledContentBackupBridge {
 public static final String BACKUP_KEY="compiledContentBundle";
 private CompiledContentBackupBridge() {}

 /** Adds a detached snapshot to the campaign backup. Call after SQL export succeeds. */
 public static JSONObject attach(JSONObject backup) throws Exception {
  if(backup==null)throw new IllegalArgumentException("backup_missing");
  JSONObject bundle=CompiledContentPersistenceBundle.snapshot();
  requireBundle(bundle);
  return backup.put(BACKUP_KEY,new JSONObject(bundle.toString()));
 }

 /** Read-only validation. Older BACKUP21 files without this optional field stay importable. */
 public static JSONObject preflight(JSONObject backup) throws Exception {
  if(backup==null)throw new IllegalArgumentException("backup_missing");
  JSONObject result=new JSONObject();
  if(!backup.has(BACKUP_KEY))return result.put("present",false).put("valid",true).put("legacy_without_compiled_bundle",true);
  JSONObject bundle=backup.optJSONObject(BACKUP_KEY);
  requireBundle(bundle);
  return result.put("present",true).put("valid",true).put("schema",bundle.optString("schema"));
 }

 /** Call inside the SQLite import transaction, before setTransactionSuccessful(). */
 public static JSONObject restoreOrThrow(JSONObject backup) throws Exception {
  JSONObject check=preflight(backup);
  if(!check.optBoolean("present"))return check;
  JSONObject result=CompiledContentPersistenceBundle.restore(backup.optJSONObject(BACKUP_KEY));
  if(!result.optBoolean("restored",false)||!result.optBoolean("rollback_verified",false))
   throw new IllegalStateException("compiled_bundle_restore_failed:"+result.optString("authority"));
  return result;
 }

 /** Call in the import's failure path if restoreOrThrow already changed the registries. */
 public static void rollbackOrThrow(JSONObject prior) throws Exception {
  requireBundle(prior);
  JSONObject result=CompiledContentPersistenceBundle.restore(prior);
  if(!result.optBoolean("restored",false)||!result.optBoolean("rollback_verified",false))
   throw new IllegalStateException("compiled_bundle_rollback_failed:"+result.optString("authority"));
 }

 private static void requireBundle(JSONObject bundle) {
  if(bundle==null||!"compiled_content_bundle_v2".equals(bundle.optString("schema"))
    ||!schema(bundle,"evidence","authorized_mechanical_evidence_v1")
    ||!schema(bundle,"content","compiled_content_mechanics_v1")
    ||!schema(bundle,"statblocks","compiled_statblocks_v1")
    ||!schema(bundle,"bestiary_scope","authorized_bestiary_scope_v1"))
   throw new IllegalArgumentException("invalid_compiled_content_bundle");
 }
 private static boolean schema(JSONObject parent,String key,String expected) {
  JSONObject part=parent.optJSONObject(key);
  return part!=null&&expected.equals(part.optString("schema"));
 }
}
