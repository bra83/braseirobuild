package com.braseiro.dnd5e.adventure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.braseiro.dnd5e.AppDb;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * D&D Normal-only BACKUP21 bridge for adventure modules and durable state.
 * Opt-in on import so backups created before this bridge remain readable.
 * Never reads, deletes or writes any Mystara-specific records.
 */
public final class AdventureBackupBridge {
    private static final String MODULES = "adventureModules";
    private static final String STATES = "adventureStates";
    private AdventureBackupBridge() {}

    public static void attach(JSONObject backup, AppDb source) throws Exception {
        if (backup == null || source == null) throw new IllegalArgumentException("Backup ausente.");
        if (!"dnd".equals(source.getActiveMapProfile())) return;
        SQLiteDatabase sql = source.getWritableDatabase();
        ensureSchema(sql);
        backup.put(MODULES, readRows(sql,
                "SELECT module_id,version,module_json,updated_at FROM adventure_modules ORDER BY module_id"));
        backup.put(STATES, readRows(sql,
                "SELECT module_id,party_id,current_node_id,state_json,updated_at " +
                "FROM adventure_state_journal ORDER BY module_id,party_id"));
    }

    /**
     * Must execute inside AppDb.importAll's existing SQLite transaction.
     * A failure throws before commit, preserving prior campaign and saves.
     */
    public static void restoreIfPresent(JSONObject backup, SQLiteDatabase sql, String profile)
            throws Exception {
        if (backup == null || sql == null) throw new IllegalArgumentException("Backup ausente.");
        if (!"dnd".equals(profile)) return;
        boolean hasModules = backup.has(MODULES);
        boolean hasStates = backup.has(STATES);
        if (!hasModules && !hasStates) return; // legacy BACKUP21
        if (!hasModules || !hasStates)
            throw new IllegalArgumentException("Backup de aventuras incompleto.");
        JSONArray modules = backup.optJSONArray(MODULES);
        JSONArray states = backup.optJSONArray(STATES);
        if (modules == null || states == null)
            throw new IllegalArgumentException("Seções de aventuras devem ser listas.");
        if (!sql.inTransaction())
            throw new IllegalStateException("Importação de aventuras exige transação SQLite.");

        // Validate everything BEFORE deleting any existing adventure row.
        Map<String, AdventureModule> validatedModules = new HashMap<>();
        for (int i = 0; i < modules.length(); i++) {
            JSONObject row = modules.getJSONObject(i);
            String id = row.getString("module_id");
            String version = row.getString("version");
            String raw = row.getString("module_json");
            if (id.trim().isEmpty() || version.trim().isEmpty() || raw.trim().isEmpty())
                throw new IllegalArgumentException("Módulo inválido no backup.");
            AdventureModule parsed = AdventureModule.fromJson(new JSONObject(raw));
            if (!id.equals(parsed.getId()) || !version.equals(parsed.getVersion())
                    || parsed.getNode(parsed.getRootNodeId()) == null
                    || validatedModules.put(id, parsed) != null)
                throw new IllegalArgumentException("Módulo de aventura divergente ou duplicado.");
        }
        Set<String> distinctStates = new HashSet<>();
        for (int i = 0; i < states.length(); i++) {
            JSONObject row = states.getJSONObject(i);
            String moduleId = row.getString("module_id");
            long partyId = row.getLong("party_id");
            String node = row.getString("current_node_id");
            String raw = row.getString("state_json");
            AdventureModule module = validatedModules.get(moduleId);
            if (module == null || partyId < 0 || node.isEmpty()
                    || !distinctStates.add(moduleId + "|" + partyId))
                throw new IllegalArgumentException("Estado de aventura inválido no backup.");
            JSONObject stored = new JSONObject(raw);
            AdventureState state = AdventureState.fromJson(stored);
            if (!node.equals(state.getCurrentNodeId()) || 
                    (module.getNode(node) == null &&
                    !(("completed".equals(node) || "failed".equals(node))
                            && node.equals(state.getStatus()))))
                throw new IllegalArgumentException("Estado de aventura órfão no backup.");
        }

        ensureSchema(sql);
        sql.delete("adventure_state_journal", null, null);
        sql.delete("adventure_modules", null, null);
        for (int i = 0; i < modules.length(); i++) {
            JSONObject row = modules.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("module_id", row.getString("module_id"));
            v.put("version", row.getString("version"));
            v.put("module_json", row.getString("module_json"));
            v.put("updated_at", row.optLong("updated_at", System.currentTimeMillis()));
            if (sql.insertWithOnConflict("adventure_modules", null, v,
                    SQLiteDatabase.CONFLICT_REPLACE) < 0)
                throw new IllegalStateException("Falha ao restaurar módulo.");
        }
        for (int i = 0; i < states.length(); i++) {
            JSONObject row = states.getJSONObject(i);
            ContentValues v = new ContentValues();
            v.put("module_id", row.getString("module_id"));
            v.put("party_id", row.getLong("party_id"));
            v.put("current_node_id", row.getString("current_node_id"));
            v.put("state_json", row.getString("state_json"));
            v.put("updated_at", row.optLong("updated_at", System.currentTimeMillis()));
            if (sql.insertWithOnConflict("adventure_state_journal", null, v,
                    SQLiteDatabase.CONFLICT_REPLACE) < 0)
                throw new IllegalStateException("Falha ao restaurar estado de aventura.");
        }
    }

    private static JSONArray readRows(SQLiteDatabase sql, String query) throws Exception {
        JSONArray rows = new JSONArray();
        Cursor cursor = sql.rawQuery(query, null);
        try {
            String[] names = cursor.getColumnNames();
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < names.length; i++) {
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: row.put(names[i], JSONObject.NULL); break;
                        case Cursor.FIELD_TYPE_INTEGER: row.put(names[i], cursor.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: row.put(names[i], cursor.getDouble(i)); break;
                        default: row.put(names[i], cursor.getString(i));
                    }
                }
                rows.put(row);
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    private static void ensureSchema(SQLiteDatabase sql) {
        sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_modules (" +
                "module_id TEXT PRIMARY KEY,version TEXT NOT NULL,module_json TEXT NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        sql.execSQL("CREATE TABLE IF NOT EXISTS adventure_state_journal (" +
                "module_id TEXT NOT NULL,party_id INTEGER NOT NULL,current_node_id TEXT NOT NULL," +
                "state_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(module_id,party_id))");
    }
}
