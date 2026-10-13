package com.braseiro.dnd5e.adventure;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.AppLog;
import org.json.JSONObject;

/** Persistência transacional de módulos pré-compilados e estados de aventura. */
public final class AdventurePersistenceAdapter {
    private final AppDb db;
    public AdventurePersistenceAdapter(AppDb database){this.db=database;}

    public synchronized boolean saveModule(final AdventureModule module){if(db==null||module==null)return false;try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);ContentValues cv=new ContentValues();cv.put("module_id",module.getId());cv.put("version",module.getVersion());cv.put("module_json",module.toJson().toString());cv.put("updated_at",System.currentTimeMillis());sql.insertWithOnConflict("adventure_modules",null,cv,SQLiteDatabase.CONFLICT_REPLACE);}});return true;}catch(Exception e){AppLog.e("AdventurePersistenceAdapter","Falha ao instalar módulo compilado.",e);return false;}}

    public synchronized AdventureModule loadModule(String moduleId){if(db==null||moduleId==null||moduleId.trim().length()==0)return null;try{SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);JSONObject row=db.queryOne("SELECT module_json FROM adventure_modules WHERE module_id=? LIMIT 1",new String[]{moduleId});String raw=row==null?"":row.optString("module_json","");return raw.length()>0?AdventureModule.fromJson(new JSONObject(raw)):null;}catch(Exception e){AppLog.e("AdventurePersistenceAdapter","Falha ao carregar módulo compilado.",e);return null;}}

    public synchronized boolean saveState(final String moduleId,final long partyId,final AdventureState state){if(db==null||moduleId==null||state==null)return false;try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);ContentValues cv=new ContentValues();cv.put("module_id",moduleId);cv.put("party_id",partyId);cv.put("current_node_id",state.getCurrentNodeId());cv.put("state_json",state.toJson().toString());cv.put("updated_at",System.currentTimeMillis());long rowId=sql.insertWithOnConflict("adventure_state_journal",null,cv,SQLiteDatabase.CONFLICT_REPLACE);if(rowId<0)throw new android.database.SQLException("Não foi possível inserir o estado da campanha.");}});return true;}catch(Exception e){AppLog.e("AdventurePersistenceAdapter","Falha ao persistir estado da aventura.",e);return false;}}


    /**
     * Persist both the adventure branch and its active D&D scene location in
     * ONE SQLite transaction. A rejected location write must roll back the
     * branch, flags and rewards as well; Mystara is never a target.
     */
    public synchronized boolean saveTransitionWithLocation(final String moduleId,
            final long partyId, final AdventureState state, final String locationId) {
        if (db == null || moduleId == null || state == null
                || !"dnd".equals(db.getActiveMapProfile())) return false;
        try {
            // Ensure the active world row exists BEFORE opening our transaction.
            db.getSceneState();
            db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override public void run() throws Exception {
                    SQLiteDatabase sql = db.getWritableDatabase();
                    ensureSchema(sql);
                    ContentValues journal = new ContentValues();
                    journal.put("module_id", moduleId);
                    journal.put("party_id", partyId);
                    journal.put("current_node_id", state.getCurrentNodeId());
                    journal.put("state_json", state.toJson().toString());
                    journal.put("updated_at", System.currentTimeMillis());
                    long inserted = sql.insertWithOnConflict("adventure_state_journal",
                            null, journal, SQLiteDatabase.CONFLICT_REPLACE);
                    if (inserted < 0) throw new android.database.SQLException(
                            "Estado da aventura não foi persistido.");
                    if (locationId != null && !locationId.isEmpty()) {
                        ContentValues scene = new ContentValues();
                        scene.put("location", locationId);
                        scene.put("updated_at", System.currentTimeMillis());
                        int changed = sql.update("scene_state_profiles", scene,
                                "world_profile=?", new String[]{"dnd"});
                        if (changed != 1) throw new android.database.SQLException(
                                "Local da cena não pôde ser persistido atomicamente.");
                    }
                }
            });
            return true;
        } catch (Exception e) {
            AppLog.e("AdventurePersistenceAdapter",
                    "Falha atômica ao persistir escolha e localização da aventura.", e);
            return false;
        }
    }

    public synchronized AdventureState loadState(String moduleId,long partyId){if(db==null||moduleId==null)return null;try{SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);JSONObject row=db.queryOne("SELECT state_json FROM adventure_state_journal WHERE module_id=? AND party_id=? LIMIT 1",new String[]{moduleId,String.valueOf(partyId)});String raw=row==null?"":row.optString("state_json","");return raw.length()>0?AdventureState.fromJson(new JSONObject(raw)):null;}catch(Exception e){AppLog.e("AdventurePersistenceAdapter","Falha ao carregar estado da aventura.",e);return null;}}

    /**
     * Fail closed when a previously persisted campaign cannot be read.
     * Absence is the ONLY situation in which a new campaign may be created.
     * Parsing/query errors must never overwrite a real player's save.
     */
    public synchronized AdventureState loadStateForStart(String moduleId, long partyId)
            throws Exception {
        if (db == null || moduleId == null || moduleId.trim().isEmpty())
            throw new IllegalArgumentException("Identificador de campanha inválido.");
        SQLiteDatabase sql = db.getWritableDatabase();
        ensureSchema(sql);
        android.database.Cursor cursor = sql.rawQuery(
                "SELECT state_json,current_node_id FROM adventure_state_journal " +
                "WHERE module_id=? AND party_id=? LIMIT 1",
                new String[]{moduleId, String.valueOf(partyId)});
        try {
            if (!cursor.moveToFirst()) return null; // confirmed absent, safe to start
            String raw = cursor.getString(0);
            String node = cursor.getString(1);
            if (raw == null || raw.trim().isEmpty())
                throw new IllegalStateException("Save da aventura está vazio; não sobrescrever.");
            JSONObject stored = new JSONObject(raw);
            String jsonNode = stored.optString("current_node_id", "");
            if (jsonNode.isEmpty() || !jsonNode.equals(node))
                throw new IllegalStateException("Save da aventura inconsistente; não sobrescrever.");
            return AdventureState.fromJson(stored);
        } finally {
            cursor.close();
        }
    }

    private void ensureSchema(SQLiteDatabase sql){if(sql==null)return;sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_modules (module_id TEXT PRIMARY KEY,version TEXT NOT NULL,module_json TEXT NOT NULL,updated_at INTEGER NOT NULL)");sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_state_journal (module_id TEXT NOT NULL,party_id INTEGER NOT NULL,current_node_id TEXT NOT NULL,state_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(module_id, party_id))");}
}
