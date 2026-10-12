package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.*;

public class SQLiteDatabase {
    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_ROLLBACK = 1;
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_REPLACE = 5;

    private final Map<String, List<Map<String, Object>>> tables = new HashMap<String, List<Map<String, Object>>>();
    private final Map<String, Set<String>> tableColumns = new HashMap<String, Set<String>>();

    private boolean inTransaction = false;
    private boolean transactionSuccessful = false;
    private Map<String, List<Map<String, Object>>> transactionSnapshot = null;

    // Hook para injeção de falhas de teste
    public String failOnTableInsert = null;

    public SQLiteDatabase() {
        // Inicializa esquemas canônicos de teste com base no AppDb.java
        initDefaultSchemas();
    }

    private void initDefaultSchemas() {
        registerTable("kingmaker_campaign_chapters",
                "world_profile", "chapter_key", "chapter_number", "title", "level_min",
                "level_max", "summary", "imported_at", "metadata_json");

        registerTable("kingmaker_features",
                "id", "world_profile", "feature_key", "feature_kind", "name", "level",
                "status", "effects_json", "source_ref", "notes", "created_at", "updated_at");

        registerTable("factions",
                "id", "world_profile", "name", "agenda", "resources", "relationship",
                "current_plan", "location", "active", "updated_at");

        registerTable("quests",
                "id", "world_profile", "quest_key", "chapter_key", "title", "status",
                "stage", "objective", "stakes", "owner_character_id", "faction_id",
                "milestone_xp", "prerequisites_json", "objectives_json", "rewards_json",
                "progress_json", "created_at", "updated_at");

        // Schema estrito de produção do AppDb.java linha 821: PRIMARY KEY (poi_id)
        registerTable("campaign_pois",
                "poi_id", "world_profile", "campaign_key", "poi_key", "chapter_key",
                "map_id", "name", "name_key", "type", "poi_type", "hex_id", "location_id",
                "q", "r", "source", "origin", "canonical", "hidden", "discovered",
                "created_by", "created_tick", "status", "description", "tags_json",
                "faction_id", "owner_npc_id", "parent_poi_id", "development_level",
                "icon", "notes", "private_notes", "settlement_key", "settlement_json",
                "state_json", "created_at", "updated_at");

        registerTable("tactical_scenes",
                "id", "world_profile", "scene_key", "title", "location", "hex_id",
                "environment", "width_cells", "height_cells", "seed", "zones_json",
                "obstacles_json", "features_json", "cover_json", "hazards_json",
                "entry_points_json", "combatants_json", "source_context_json", "status",
                "created_at", "updated_at");

        registerTable("npc_memory_profiles",
                "id", "world_profile", "stable_id", "name", "summary", "goals",
                "secrets", "attitude", "relationship", "knowledge", "debts", "last_seen",
                "token_uri", "visual_description", "stats_json", "hex_id", "active",
                "voice_profile", "updated_at");

        registerTable("npc_cognitive_state",
                "world_profile", "npc_name", "emotion", "trust", "fear", "suspicion",
                "urgency", "next_action", "schedule_json", "beliefs_json", "faction",
                "current_location", "last_interaction_summary", "last_action_at", "updated_at");

        registerTable("combatants",
                "id", "world_profile", "npc_key", "name", "attitude", "faction",
                "dialogue_json", "hp", "max_hp", "ac", "initiative", "source_kind",
                "is_player", "created_at", "updated_at");

        registerTable("kingmaker_companions",
                "id", "world_profile", "companion_key", "name", "influence",
                "attitude", "assignment", "available", "notes", "created_at", "updated_at");

        registerTable("world_clocks",
                "id", "world_profile", "name", "current_value", "max_value",
                "consequence", "automatic", "active", "updated_at");

        registerTable("world_events",
                "id", "world_profile", "title", "description", "due_at", "due_day",
                "status", "consequence_json", "created_at", "resolved_at");

        registerTable("kingmaker_hexes",
                "world_profile", "map_id", "hex_id", "updated_at");
    }

    public void registerTable(String tableName, String... columns) {
        if (!tables.containsKey(tableName)) {
            tables.put(tableName, new ArrayList<Map<String, Object>>());
        }
        Set<String> cols = new HashSet<String>(Arrays.asList(columns));
        tableColumns.put(tableName, cols);
    }

    public void beginTransaction() {
        if (inTransaction) {
            throw new IllegalStateException("Transação aninhada não suportada.");
        }
        inTransaction = true;
        transactionSuccessful = false;
        transactionSnapshot = new HashMap<String, List<Map<String, Object>>>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : tables.entrySet()) {
            List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> row : entry.getValue()) {
                copy.add(new HashMap<String, Object>(row));
            }
            transactionSnapshot.put(entry.getKey(), copy);
        }
    }

    public void setTransactionSuccessful() {
        if (!inTransaction) {
            throw new IllegalStateException("Nenhuma transação ativa.");
        }
        transactionSuccessful = true;
    }

    public void endTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("Nenhuma transação ativa.");
        }
        if (!transactionSuccessful) {
            // ROLLBACK
            tables.clear();
            for (Map.Entry<String, List<Map<String, Object>>> entry : transactionSnapshot.entrySet()) {
                tables.put(entry.getKey(), new ArrayList<Map<String, Object>>(entry.getValue()));
            }
        }
        inTransaction = false;
        transactionSuccessful = false;
        transactionSnapshot = null;
    }

    public boolean inTransaction() {
        return inTransaction;
    }

    public void execSQL(String sql) {
        // Suporte mínimo para CREATE TABLE IF NOT EXISTS
        if (sql.startsWith("CREATE TABLE IF NOT EXISTS ")) {
            String sub = sql.substring("CREATE TABLE IF NOT EXISTS ".length()).trim();
            int p = sub.indexOf('(');
            if (p > 0) {
                String tableName = sub.substring(0, p).trim();
                if (!tables.containsKey(tableName)) {
                    registerTable(tableName);
                }
            }
        }
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm) {
        if (failOnTableInsert != null && failOnTableInsert.equalsIgnoreCase(table)) {
            throw new SQLiteException("Falha simulada de I/O SQLite ao inserir na tabela " + table);
        }

        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null) {
            rows = new ArrayList<Map<String, Object>>();
            tables.put(table, rows);
        }

        Map<String, Object> newRow = values != null ? values.valueMap() : new HashMap<String, Object>();

        // Resolução de conflito de chave primária/única
        int conflictIndex = findConflictRow(table, rows, newRow);
        if (conflictIndex >= 0) {
            if (conflictAlgorithm == CONFLICT_IGNORE) {
                return -1;
            } else if (conflictAlgorithm == CONFLICT_REPLACE) {
                rows.set(conflictIndex, newRow);
                return conflictIndex + 1;
            } else {
                throw new SQLiteException("Violação de constraint única/primária na tabela " + table);
            }
        }

        rows.add(newRow);
        return rows.size();
    }

    private int findConflictRow(String table, List<Map<String, Object>> rows, Map<String, Object> newRow) {
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> existing = rows.get(i);
            if (isConflict(table, existing, newRow)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isConflict(String table, Map<String, Object> a, Map<String, Object> b) {
        if ("campaign_pois".equals(table)) {
            // Em AppDb.java linha 821: poi_id TEXT PRIMARY KEY (sem world_profile na PK!)
            Object idA = a.get("poi_id");
            Object idB = b.get("poi_id");
            return idA != null && idB != null && idA.equals(idB);
        }
        if ("kingmaker_campaign_chapters".equals(table)) {
            return matchesKeys(a, b, "world_profile", "chapter_key");
        }
        if ("kingmaker_features".equals(table)) {
            return matchesKeys(a, b, "world_profile", "feature_key");
        }
        if ("factions".equals(table)) {
            return matchesKeys(a, b, "world_profile", "name");
        }
        if ("tactical_scenes".equals(table)) {
            return matchesKeys(a, b, "world_profile", "scene_key");
        }
        if ("npc_memory_profiles".equals(table)) {
            return matchesKeys(a, b, "world_profile", "name");
        }
        if ("npc_cognitive_state".equals(table)) {
            return matchesKeys(a, b, "world_profile", "npc_name");
        }
        if ("kingmaker_companions".equals(table)) {
            return matchesKeys(a, b, "world_profile", "companion_key");
        }
        if ("kingmaker_hexes".equals(table)) {
            return matchesKeys(a, b, "world_profile", "map_id", "hex_id");
        }
        if ("quests".equals(table)) {
            return matchesKeys(a, b, "world_profile", "quest_key");
        }
        return false;
    }

    private boolean matchesKeys(Map<String, Object> a, Map<String, Object> b, String... keys) {
        for (String k : keys) {
            Object va = a.get(k);
            Object vb = b.get(k);
            if (va == null || vb == null || !va.toString().equalsIgnoreCase(vb.toString())) {
                return false;
            }
        }
        return true;
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null || rows.isEmpty() || values == null) return 0;
        int count = 0;
        for (Map<String, Object> row : rows) {
            if (matchesWhere(table, row, whereClause, whereArgs)) {
                row.putAll(values.valueMap());
                count++;
            }
        }
        return count;
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null || rows.isEmpty()) return 0;
        int count = 0;
        Iterator<Map<String, Object>> it = rows.iterator();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            if (matchesWhere(table, row, whereClause, whereArgs)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    private boolean matchesWhere(String table, Map<String, Object> row, String whereClause, String[] whereArgs) {
        if (whereClause == null || whereClause.trim().length() == 0) return true;
        if (whereClause.contains("world_profile=?")) {
            String targetProfile = whereArgs != null && whereArgs.length > 0 ? whereArgs[0] : "";
            Object wp = row.get("world_profile");
            if (wp == null || !targetProfile.equals(wp.toString())) return false;
        }
        if (whereClause.contains("chapter_key=?")) {
            String chKey = whereArgs != null && whereArgs.length > 1 ? whereArgs[1] : "";
            Object ck = row.get("chapter_key");
            if (ck == null || !chKey.equals(ck.toString())) return false;
        }
        if (whereClause.contains("quest_key=?")) {
            String qKey = whereArgs != null && whereArgs.length > 1 ? whereArgs[1] : "";
            Object qk = row.get("quest_key");
            if (qk == null || !qKey.equals(qk.toString())) return false;
        }
        if (whereClause.contains("title=?")) {
            String targetTitle = whereArgs != null && whereArgs.length > 1 ? whereArgs[1] : "";
            Object t = row.get("title");
            if (t == null || !targetTitle.toString().equalsIgnoreCase(t.toString())) return false;
        }
        return true;
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        if (sql.startsWith("PRAGMA table_info(")) {
            String tableName = sql.substring("PRAGMA table_info(".length()).replace(")", "").trim();
            Set<String> cols = tableColumns.get(tableName);
            if (cols == null) cols = Collections.emptySet();
            return new MockPragmaCursor(new ArrayList<String>(cols));
        }

        if (sql.startsWith("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            String tableName = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : "";
            boolean exists = tables.containsKey(tableName);
            return new MockSingleValueCursor(exists);
        }

        if (sql.contains("FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?")) {
            String wp = selectionArgs[0];
            String ck = selectionArgs[1];
            List<Map<String, Object>> rows = tables.get("kingmaker_campaign_chapters");
            boolean found = false;
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    if (wp.equals(r.get("world_profile")) && ck.equals(r.get("chapter_key"))) {
                        found = true;
                        break;
                    }
                }
            }
            return new MockSingleValueCursor(found);
        }

        if (sql.contains("FROM quests WHERE world_profile=? AND quest_key=?")) {
            String wp = selectionArgs[0];
            String qk = selectionArgs[1];
            List<Map<String, Object>> rows = tables.get("quests");
            boolean found = false;
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    if (wp.equals(r.get("world_profile")) && qk.equals(r.get("quest_key"))) {
                        found = true;
                        break;
                    }
                }
            }
            return new MockSingleValueCursor(found);
        }

        if (sql.startsWith("SELECT * FROM ")) {
            String[] parts = sql.split(" ");
            String tableName = parts[3];
            List<Map<String, Object>> allRows = tables.get(tableName);
            if (allRows == null) allRows = Collections.emptyList();

            List<Map<String, Object>> matching = new ArrayList<Map<String, Object>>();
            String profile = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : null;

            for (Map<String, Object> r : allRows) {
                if (profile == null || profile.equals(r.get("world_profile"))) {
                    matching.add(r);
                }
            }
            Set<String> cols = tableColumns.get(tableName);
            if (cols == null && !matching.isEmpty()) cols = matching.get(0).keySet();
            return new MockTableCursor(new ArrayList<String>(cols != null ? cols : Collections.<String>emptyList()), matching);
        }

        return new MockSingleValueCursor(false);
    }

    public int getRowCount(String tableName) {
        List<Map<String, Object>> rows = tables.get(tableName);
        return rows != null ? rows.size() : 0;
    }

    public List<Map<String, Object>> queryTable(String tableName, String worldProfile) {
        List<Map<String, Object>> rows = tables.get(tableName);
        if (rows == null) return Collections.emptyList();
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> r : rows) {
            if (worldProfile == null || worldProfile.equals(r.get("world_profile"))) {
                out.add(r);
            }
        }
        return out;
    }

    // Cursores mock internos
    private static class MockPragmaCursor implements Cursor {
        private final List<String> colNames;
        private int pos = -1;

        public MockPragmaCursor(List<String> colNames) {
            this.colNames = colNames;
        }

        public boolean moveToFirst() { pos = 0; return pos < colNames.size(); }
        public boolean moveToNext() { pos++; return pos < colNames.size(); }
        public int getColumnIndex(String n) { return "name".equals(n) ? 0 : -1; }
        public int getColumnIndexOrThrow(String n) { return getColumnIndex(n); }
        public String getString(int i) { return (pos >= 0 && pos < colNames.size()) ? colNames.get(pos) : null; }
        public long getLong(int i) { return 0; }
        public int getInt(int i) { return 0; }
        public double getDouble(int i) { return 0; }
        public float getFloat(int i) { return 0; }
        public boolean isNull(int i) { return false; }
        public int getColumnCount() { return 1; }
        public String getColumnName(int i) { return "name"; }
        public String[] getColumnNames() { return new String[]{"name"}; }
        public int getType(int i) { return FIELD_TYPE_STRING; }
        public void close() {}
    }

    private static class MockSingleValueCursor implements Cursor {
        private final boolean hasRow;
        private int pos = -1;

        public MockSingleValueCursor(boolean hasRow) { this.hasRow = hasRow; }
        public boolean moveToFirst() { pos = 0; return hasRow; }
        public boolean moveToNext() { pos++; return false; }
        public int getColumnIndex(String n) { return 0; }
        public int getColumnIndexOrThrow(String n) { return 0; }
        public String getString(int i) { return hasRow ? "1" : null; }
        public long getLong(int i) { return hasRow ? 1L : 0L; }
        public int getInt(int i) { return hasRow ? 1 : 0; }
        public double getDouble(int i) { return 1.0; }
        public float getFloat(int i) { return 1.0f; }
        public boolean isNull(int i) { return !hasRow; }
        public int getColumnCount() { return 1; }
        public String getColumnName(int i) { return "val"; }
        public String[] getColumnNames() { return new String[]{"val"}; }
        public int getType(int i) { return FIELD_TYPE_INTEGER; }
        public void close() {}
    }

    private static class MockTableCursor implements Cursor {
        private final List<String> columnNames;
        private final List<Map<String, Object>> rows;
        private int pos = -1;

        public MockTableCursor(List<String> columnNames, List<Map<String, Object>> rows) {
            this.columnNames = columnNames;
            this.rows = rows;
        }

        public boolean moveToFirst() { pos = 0; return pos < rows.size(); }
        public boolean moveToNext() { pos++; return pos < rows.size(); }
        public int getColumnIndex(String n) { return columnNames.indexOf(n); }
        public int getColumnIndexOrThrow(String n) { int i = columnNames.indexOf(n); if (i < 0) throw new IllegalArgumentException(n); return i; }
        public String getString(int i) {
            Object v = getValue(i);
            return v != null ? v.toString() : null;
        }
        public long getLong(int i) {
            Object v = getValue(i);
            if (v instanceof Number) return ((Number) v).longValue();
            return 0L;
        }
        public int getInt(int i) {
            Object v = getValue(i);
            if (v instanceof Number) return ((Number) v).intValue();
            return 0;
        }
        public double getDouble(int i) {
            Object v = getValue(i);
            if (v instanceof Number) return ((Number) v).doubleValue();
            return 0.0;
        }
        public float getFloat(int i) {
            Object v = getValue(i);
            if (v instanceof Number) return ((Number) v).floatValue();
            return 0.0f;
        }
        public boolean isNull(int i) { return getValue(i) == null; }
        public int getColumnCount() { return columnNames.size(); }
        public String getColumnName(int i) { return columnNames.get(i); }
        public String[] getColumnNames() { return columnNames.toArray(new String[0]); }
        public int getType(int i) {
            Object v = getValue(i);
            if (v == null) return FIELD_TYPE_NULL;
            if (v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte) return FIELD_TYPE_INTEGER;
            if (v instanceof Double || v instanceof Float) return FIELD_TYPE_FLOAT;
            return FIELD_TYPE_STRING;
        }
        private Object getValue(int colIdx) {
            if (pos < 0 || pos >= rows.size() || colIdx < 0 || colIdx >= columnNames.size()) return null;
            String col = columnNames.get(colIdx);
            return rows.get(pos).get(col);
        }
        public void close() {}
    }
}
