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

    public synchronized AdventureState loadState(String moduleId,long partyId){if(db==null||moduleId==null)return null;try{SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);JSONObject row=db.queryOne("SELECT state_json FROM adventure_state_journal WHERE module_id=? AND party_id=? LIMIT 1",new String[]{moduleId,String.valueOf(partyId)});String raw=row==null?"":row.optString("state_json","");return raw.length()>0?AdventureState.fromJson(new JSONObject(raw)):null;}catch(Exception e){AppLog.e("AdventurePersistenceAdapter","Falha ao carregar estado da aventura.",e);return null;}}

    private void ensureSchema(SQLiteDatabase sql){if(sql==null)return;sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_modules (module_id TEXT PRIMARY KEY,version TEXT NOT NULL,module_json TEXT NOT NULL,updated_at INTEGER NOT NULL)");sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_state_journal (module_id TEXT NOT NULL,party_id INTEGER NOT NULL,current_node_id TEXT NOT NULL,state_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(module_id, party_id))");}
}
