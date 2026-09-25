package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo persistente do áudio V3 — núcleo 4.2.0.
 *
 * Este banco é separado do banco da campanha. A indexação e o histórico de
 * reprodução nunca podem apagar personagens, mapas ou aventuras.
 */
public final class AudioCatalogDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "audio_catalog_v3.db";
    private static final int DB_VERSION = 4;


    /**
     * Verifica o banco de áudio antes de o SQLiteOpenHelper abri-lo. O catálogo
     * pode ser reconstruído a partir do índice JSON; portanto, em caso de
     * corrupção, somente este banco auxiliar é colocado em quarentena. O banco
     * da campanha nunca é tocado.
     */
    public static boolean recoverIfNeeded(Context context) {
        if (context == null) return false;
        File file = context.getApplicationContext().getDatabasePath(DB_NAME);
        if (!file.exists()) return false;
        SQLiteDatabase probe = null;
        try {
            probe = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            Cursor c = probe.rawQuery("PRAGMA quick_check(1)", null);
            try {
                if (c.moveToFirst() && "ok".equalsIgnoreCase(c.getString(0))) return false;
            } finally { c.close(); }
        } catch (Throwable error) {
            AppLog.w("AudioCatalogDb", "Catálogo de áudio inválido; será reconstruído com segurança.", error);
        } finally {
            try { if (probe != null) probe.close(); } catch(Throwable ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
        }
        quarantine(context.getApplicationContext(), "integridade");
        return true;
    }

    /**
     * Um diagnóstico interrompido não é prova de corrupção. Desde a versão
     * 4.2.0 o teste é somente em memória; portanto, o catálogo só é colocado
     * em quarentena quando o quick_check do SQLite realmente falha.
     */
    public static void recoverAfterInterruptedDiagnostic(Context context) {
        recoverIfNeeded(context);
    }

    private static void quarantine(Context context, String reason) {
        File file = context.getDatabasePath(DB_NAME);
        long stamp = System.currentTimeMillis();
        try {
            if (file.exists()) {
                File target = new File(file.getParentFile(), DB_NAME + "." + reason + "." + stamp + ".corrupt");
                if (!file.renameTo(target)) file.delete();
            }
        } catch (Throwable ignored) { try { file.delete(); } catch(Throwable ignoredAgain){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignoredAgain);} }
        deleteSidecar(new File(file.getAbsolutePath() + "-wal"));
        deleteSidecar(new File(file.getAbsolutePath() + "-shm"));
        deleteSidecar(new File(file.getAbsolutePath() + "-journal"));
    }

    private static void deleteSidecar(File file) {
        try { if (file.exists()) file.delete(); } catch(Throwable ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
    }

    public static final class Stats {
        public final long lastPlayedAt;
        public final int playCount;
        public final int failureCount;
        public final long durationMs;
        Stats(long last, int plays, int failures, long duration) {
            lastPlayedAt = last;
            playCount = plays;
            failureCount = failures;
            durationMs = duration;
        }
    }

    public AudioCatalogDb(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        try { db.execSQL("PRAGMA busy_timeout=8000"); }
        catch (Exception e) { AppLog.w("AudioCatalogDb","Não foi possível ampliar a espera do catálogo de áudio.",e); }
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE audio_entries(" +
                "sound_id TEXT PRIMARY KEY," +
                "uri TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "path TEXT NOT NULL," +
                "mime TEXT," +
                "size_bytes INTEGER NOT NULL DEFAULT 0," +
                "modified_at INTEGER NOT NULL DEFAULT 0," +
                "keywords TEXT," +
                "channel TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "variant_group TEXT NOT NULL," +
                "environment TEXT," +
                "weather TEXT," +
                "intensity TEXT," +
                "daypart TEXT," +
                "loopable INTEGER NOT NULL DEFAULT 0," +
                "weight REAL NOT NULL DEFAULT 1.0," +
                "duration_ms INTEGER NOT NULL DEFAULT 0," +
                "last_played_at INTEGER NOT NULL DEFAULT 0," +
                "play_count INTEGER NOT NULL DEFAULT 0," +
                "failure_count INTEGER NOT NULL DEFAULT 0," +
                "enabled INTEGER NOT NULL DEFAULT 1," +
                "catalog_match INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_audio_channel_role ON audio_entries(channel,role)");
        db.execSQL("CREATE INDEX idx_audio_variant_group ON audio_entries(variant_group)");
        db.execSQL("CREATE INDEX idx_audio_last_played ON audio_entries(last_played_at)");
        db.execSQL("CREATE TABLE audio_history(" +
                "history_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sound_id TEXT NOT NULL," +
                "channel TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "variant_group TEXT NOT NULL," +
                "scene_key TEXT," +
                "played_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_audio_history_lookup ON audio_history(channel,role,variant_group,played_at DESC)");
        db.execSQL("CREATE TABLE audio_meta(meta_key TEXT PRIMARY KEY,meta_value TEXT)");
        db.execSQL("CREATE TABLE audio_sessions(" +
                "session_id TEXT PRIMARY KEY," +
                "scene_id TEXT," +
                "started_at INTEGER NOT NULL," +
                "ended_at INTEGER NOT NULL DEFAULT 0," +
                "status TEXT NOT NULL," +
                "backend TEXT)");
        db.execSQL("CREATE TABLE audio_import_jobs(" +
                "job_id TEXT PRIMARY KEY," +
                "tree_uri TEXT," +
                "status TEXT NOT NULL," +
                "imported INTEGER NOT NULL DEFAULT 0," +
                "skipped INTEGER NOT NULL DEFAULT 0," +
                "failed INTEGER NOT NULL DEFAULT 0," +
                "current_item TEXT," +
                "started_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "finished_at INTEGER NOT NULL DEFAULT 0," +
                "error_message TEXT)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE audio_entries ADD COLUMN role TEXT NOT NULL DEFAULT 'base'"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("CREATE INDEX idx_audio_channel_role ON audio_entries(channel,role)"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE audio_history ADD COLUMN role TEXT NOT NULL DEFAULT 'base'"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 3) {
            try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_history_lookup ON audio_history(channel,role,variant_group,played_at DESC)"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("DELETE FROM audio_history WHERE history_id NOT IN (SELECT history_id FROM audio_history ORDER BY played_at DESC LIMIT 2000)"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 4) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS audio_sessions(session_id TEXT PRIMARY KEY,scene_id TEXT,started_at INTEGER NOT NULL,ended_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,backend TEXT)"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS audio_import_jobs(job_id TEXT PRIMARY KEY,tree_uri TEXT,status TEXT NOT NULL,imported INTEGER NOT NULL DEFAULT 0,skipped INTEGER NOT NULL DEFAULT 0,failed INTEGER NOT NULL DEFAULT 0,current_item TEXT,started_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,finished_at INTEGER NOT NULL DEFAULT 0,error_message TEXT)"); } catch(Exception ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);}
        }
    }

    public synchronized void synchronize(JSONArray entries) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Set<String> present = new HashSet<String>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject sound = entries.optJSONObject(i);
                if (sound == null) continue;
                String id = soundId(sound);
                if (id.length() == 0) continue;
                present.add(id);
                Stats old = statsInternal(db, id);
                ContentValues v = new ContentValues();
                v.put("sound_id", id);
                v.put("uri", sound.optString("uri", ""));
                v.put("name", sound.optString("name", ""));
                v.put("path", sound.optString("path", sound.optString("name", "")));
                v.put("mime", sound.optString("mime", ""));
                v.put("size_bytes", sound.optLong("size", 0L));
                v.put("modified_at", sound.optLong("modified", 0L));
                v.put("keywords", sound.optString("keywords", ""));
                v.put("channel", sound.optString("catalog_channel", sound.optString("type", "unknown")));
                v.put("role", sound.optString("catalog_role", "base"));
                v.put("variant_group", sound.optString("variant_group", "generic"));
                v.put("environment", sound.optString("environment", ""));
                v.put("weather", sound.optString("weather", ""));
                v.put("intensity", sound.optString("intensity", "medium"));
                v.put("daypart", sound.optString("daypart", "any"));
                v.put("loopable", sound.optBoolean("loopable", false) ? 1 : 0);
                v.put("weight", sound.optDouble("catalog_weight", 1.0));
                long duration = sound.optLong("duration_ms", 0L);
                if (duration <= 0L) duration = old.durationMs;
                v.put("duration_ms", duration);
                v.put("last_played_at", old.lastPlayedAt);
                v.put("play_count", old.playCount);
                v.put("failure_count", old.failureCount);
                v.put("enabled", 1);
                v.put("catalog_match", sound.optBoolean("catalog", false) ? 1 : 0);
                db.insertWithOnConflict("audio_entries", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            Cursor cursor = db.query("audio_entries", new String[]{"sound_id"}, null, null, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    if (!present.contains(id)) {
                        ContentValues disabled = new ContentValues();
                        disabled.put("enabled", 0);
                        db.update("audio_entries", disabled, "sound_id=?", new String[]{id});
                    }
                }
            } finally { cursor.close(); }
            setMetaInternal(db, "entry_count", String.valueOf(present.size()));
            setMetaInternal(db, "last_sync", String.valueOf(System.currentTimeMillis()));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public synchronized Stats stats(String soundId) {
        return statsInternal(getReadableDatabase(), soundId == null ? "" : soundId);
    }

    private Stats statsInternal(SQLiteDatabase db, String id) {
        Cursor c = db.query("audio_entries",
                new String[]{"last_played_at","play_count","failure_count","duration_ms"},
                "sound_id=?", new String[]{id}, null, null, null, "1");
        try {
            if (c.moveToFirst()) return new Stats(c.getLong(0), c.getInt(1), c.getInt(2), c.getLong(3));
        } finally { c.close(); }
        return new Stats(0L, 0, 0, 0L);
    }

    public synchronized List<String> recentIds(String channel, String role, String group, int limit) {
        ArrayList<String> out = new ArrayList<String>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder where = new StringBuilder("channel=? AND role=?");
        ArrayList<String> args = new ArrayList<String>();
        args.add(safe(channel));
        args.add(safe(role));
        if (group != null && group.length() > 0) {
            where.append(" AND variant_group=?");
            args.add(group);
        }
        Cursor c = db.query("audio_history", new String[]{"sound_id"}, where.toString(),
                args.toArray(new String[args.size()]), null, null, "played_at DESC", String.valueOf(Math.max(1, limit * 3)));
        HashSet<String> unique = new HashSet<String>();
        try {
            while (c.moveToNext() && out.size() < limit) {
                String id = c.getString(0);
                if (id != null && unique.add(id)) out.add(id);
            }
        } finally { c.close(); }
        return out;
    }

    public synchronized boolean markPlayed(JSONObject sound, String channel, String role, String sceneKey) {
        if (sound == null) return false;
        String id = soundId(sound);
        if (id.length() == 0) return false;
        long now = System.currentTimeMillis();
        SQLiteDatabase db = null;
        boolean transaction = false;
        try {
            db = getWritableDatabase();
            db.beginTransaction(); transaction = true;
            db.execSQL("UPDATE audio_entries SET last_played_at=?, play_count=play_count+1 WHERE sound_id=?",
                    new Object[]{now, id});
            ContentValues history = new ContentValues();
            history.put("sound_id", id);
            history.put("channel", safe(channel));
            history.put("role", safe(role));
            history.put("variant_group", sound.optString("variant_group", "generic"));
            history.put("scene_key", safe(sceneKey));
            history.put("played_at", now);
            db.insertOrThrow("audio_history", null, history);
            db.execSQL("DELETE FROM audio_history WHERE history_id NOT IN (SELECT history_id FROM audio_history ORDER BY played_at DESC LIMIT 2000)");
            db.setTransactionSuccessful();
            return true;
        } catch (Throwable error) {
            AppLog.w("AudioCatalogDb", "Histórico de áudio não pôde ser salvo; a reprodução continuará sem persistência.", error);
            return false;
        } finally {
            if (transaction && db != null) { try { db.endTransaction(); } catch(Throwable ignored){AppLog.ignored("AudioCatalogDb","Falha tolerada; a execução continuou.",ignored);} }
        }
    }

    public synchronized void clearDiagnosticHistory() {
        try { getWritableDatabase().delete("audio_history", "scene_key LIKE ?", new String[]{"diagnostico-%"}); }
        catch (Throwable error) { AppLog.w("AudioCatalogDb", "Não foi possível limpar o histórico de diagnóstico.", error); }
    }

    public synchronized void markFailure(JSONObject sound) {
        if (sound == null) return;
        String id = soundId(sound);
        if (id.length() == 0) return;
        getWritableDatabase().execSQL("UPDATE audio_entries SET failure_count=failure_count+1, enabled=CASE WHEN failure_count+1>=3 THEN 0 ELSE enabled END WHERE sound_id=?", new Object[]{id});
    }

    public synchronized void updateDuration(JSONObject sound, long durationMs) {
        if (sound == null || durationMs <= 0L) return;
        String id = soundId(sound);
        if (id.length() == 0) return;
        ContentValues v = new ContentValues();
        v.put("duration_ms", durationMs);
        getWritableDatabase().update("audio_entries", v, "sound_id=?", new String[]{id});
    }

    public synchronized void updateImportJob(String jobId,String treeUri,String status,int imported,int skipped,int failed,String current,String error,boolean finished) {
        if(jobId==null||jobId.length()==0)return;
        long now=System.currentTimeMillis();
        SQLiteDatabase db=getWritableDatabase();
        Cursor existing=db.query("audio_import_jobs",new String[]{"started_at"},"job_id=?",new String[]{jobId},null,null,null,"1");
        long started=now;try{if(existing.moveToFirst())started=existing.getLong(0);}finally{existing.close();}
        ContentValues v=new ContentValues();
        v.put("job_id",jobId);v.put("tree_uri",treeUri==null?"":treeUri);v.put("status",status==null?"running":status);
        v.put("imported",imported);v.put("skipped",skipped);v.put("failed",failed);v.put("current_item",current==null?"":current);
        v.put("started_at",started);v.put("updated_at",now);v.put("finished_at",finished?now:0L);v.put("error_message",error==null?"":error);
        db.insertWithOnConflict("audio_import_jobs",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void startSession(String sessionId,String sceneId,String backend) {
        if(sessionId==null||sessionId.length()==0)return;
        ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("scene_id",sceneId==null?"":sceneId);v.put("started_at",System.currentTimeMillis());v.put("ended_at",0);v.put("status","active");v.put("backend",backend==null?"":backend);
        getWritableDatabase().insertWithOnConflict("audio_sessions",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void finishSession(String sessionId,String status) {
        if(sessionId==null||sessionId.length()==0)return;
        ContentValues v=new ContentValues();v.put("ended_at",System.currentTimeMillis());v.put("status",status==null?"completed":status);
        getWritableDatabase().update("audio_sessions",v,"session_id=?",new String[]{sessionId});
    }

    public synchronized void clearAll() {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("audio_history",null,null);
            db.delete("audio_sessions",null,null);
            db.delete("audio_import_jobs",null,null);
            db.delete("audio_entries",null,null);
            db.delete("audio_meta",null,null);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public synchronized void reactivateAll() {
        getWritableDatabase().execSQL("UPDATE audio_entries SET enabled=1, failure_count=0");
    }

    public synchronized int countEnabled() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM audio_entries WHERE enabled=1", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    public synchronized String report() {
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder b = new StringBuilder();
        b.append("Catálogo de Áudio V3 (SQLite)\n");
        b.append("Entradas ativas: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1")).append('\n');
        b.append("Ambiências-base: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1 AND channel='ambience' AND role='base'")).append('\n');
        b.append("Camadas climáticas: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1 AND channel='ambience' AND role='climate'")).append('\n');
        b.append("Músicas: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1 AND channel='music'")).append('\n');
        b.append("SFX: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1 AND channel='sfx'")).append('\n');
        b.append("Com duração medida: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_entries WHERE enabled=1 AND duration_ms>0")).append('\n');
        b.append("Histórico preservado: ").append(singleInt(db, "SELECT COUNT(*) FROM audio_history")).append(" seleções\n");
        b.append("Falhas registradas: ").append(singleInt(db, "SELECT COALESCE(SUM(failure_count),0) FROM audio_entries"));
        return b.toString();
    }

    private int singleInt(SQLiteDatabase db, String sql) {
        Cursor c = db.rawQuery(sql, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    private void setMetaInternal(SQLiteDatabase db, String key, String value) {
        ContentValues v = new ContentValues();
        v.put("meta_key", key);
        v.put("meta_value", value);
        db.insertWithOnConflict("audio_meta", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String soundId(JSONObject sound) {
        if (sound == null) return "";
        String id = sound.optString("id", "");
        if (id.length() == 0) id = sound.optString("uri", "");
        if (id.length() == 0) id = sound.optString("path", sound.optString("name", ""));
        return id;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
