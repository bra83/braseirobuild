package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Motor central de entidades narrativas, lore e proteção de segredos para os 10 livros oficiais.
 * - Suporta isolamento estrito por cenário (Faerûn, Eberron, Ravenloft, Lorwyn, Netheril).
 * - Distingue fatos verificados do cânone, rumores conhecidos e segredos exclusivos do Mestre.
 * - Garante que segredos nunca vazem para a visão do jogador antes da descoberta in-game.
 * - Persistência transacional integrada ao AppDb e exportável no Backup21.
 */
public final class NarrativeContentEngine {

    public static final class Entity {
        public final String id;
        public final String sourceId;
        public final String setting;
        public final String era;
        public final String type;
        public final String name;
        public final String originalName;
        public final String summary;
        public final List<String> verifiedFacts;
        public final List<String> gmSecrets;
        public final List<String> tags;
        public final Map<String, String> relations;
        public final String visibility;
        public final boolean discovered;

        public Entity(String id, String sourceId, String setting, String era, String type,
                      String name, String originalName, String summary,
                      List<String> verifiedFacts, List<String> gmSecrets, List<String> tags,
                      Map<String, String> relations, String visibility, boolean discovered) {
            this.id = id != null ? id.trim() : "";
            this.sourceId = sourceId != null ? sourceId.trim().toUpperCase(Locale.US) : "";
            this.setting = setting != null ? setting.trim().toLowerCase(Locale.US) : "forgotten_realms";
            this.era = era != null ? era.trim() : "present";
            this.type = type != null ? type.trim().toLowerCase(Locale.US) : "misc";
            this.name = name != null ? name.trim() : "";
            this.originalName = originalName != null ? originalName.trim() : "";
            this.summary = summary != null ? summary.trim() : "";
            this.verifiedFacts = verifiedFacts != null ? Collections.unmodifiableList(new ArrayList<>(verifiedFacts)) : Collections.<String>emptyList();
            this.gmSecrets = gmSecrets != null ? Collections.unmodifiableList(new ArrayList<>(gmSecrets)) : Collections.<String>emptyList();
            this.tags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : Collections.<String>emptyList();
            this.relations = relations != null ? Collections.unmodifiableMap(new HashMap<>(relations)) : Collections.<String, String>emptyMap();
            this.visibility = visibility != null ? visibility.trim().toLowerCase(Locale.US) : "grupo";
            this.discovered = discovered;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("source_id", sourceId);
                json.put("setting", setting);
                json.put("era", era);
                json.put("type", type);
                json.put("name", name);
                json.put("original_name", originalName);
                json.put("summary", summary);
                json.put("verified_facts", new JSONArray(verifiedFacts));
                json.put("gm_secrets", new JSONArray(gmSecrets));
                json.put("tags", new JSONArray(tags));
                json.put("relations", new JSONObject(relations));
                json.put("visibility", visibility);
                json.put("discovered", discovered);
            } catch (Exception ignored) {}
            return json;
        }

        public static Entity fromJson(JSONObject json) {
            if (json == null) return null;
            String id = json.optString("id", "");
            String sourceId = json.optString("source_id", "");
            String setting = json.optString("setting", "forgotten_realms");
            String era = json.optString("era", "present");
            String type = json.optString("type", "misc");
            String name = json.optString("name", "");
            String originalName = json.optString("original_name", "");
            String summary = json.optString("summary", "");
            String visibility = json.optString("visibility", "grupo");
            boolean discovered = json.optBoolean("discovered", false);

            List<String> facts = new ArrayList<>();
            JSONArray factsArr = json.optJSONArray("verified_facts");
            if (factsArr != null) for (int i = 0; i < factsArr.length(); i++) facts.add(factsArr.optString(i));

            List<String> secrets = new ArrayList<>();
            JSONArray secretsArr = json.optJSONArray("gm_secrets");
            if (secretsArr != null) for (int i = 0; i < secretsArr.length(); i++) secrets.add(secretsArr.optString(i));

            List<String> tags = new ArrayList<>();
            JSONArray tagsArr = json.optJSONArray("tags");
            if (tagsArr != null) for (int i = 0; i < tagsArr.length(); i++) tags.add(tagsArr.optString(i));

            Map<String, String> relations = new HashMap<>();
            JSONObject relObj = json.optJSONObject("relations");
            if (relObj != null) {
                java.util.Iterator<String> keys = relObj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    relations.put(k, relObj.optString(k));
                }
            }

            return new Entity(id, sourceId, setting, era, type, name, originalName, summary,
                    facts, secrets, tags, relations, visibility, discovered);
        }
    }

    private final AppDb appDb;

    public NarrativeContentEngine(AppDb appDb) {
        this.appDb = appDb;
        ensureSchema();
    }

    private void ensureSchema() {
        if (appDb == null) return;
        try {
            SQLiteDatabase db = appDb.getWritableDatabase();
            db.execSQL("CREATE TABLE IF NOT EXISTS narrative_entities (" +
                    "id TEXT PRIMARY KEY, " +
                    "source_id TEXT NOT NULL, " +
                    "setting TEXT NOT NULL, " +
                    "era TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "original_name TEXT, " +
                    "summary TEXT NOT NULL, " +
                    "facts_json TEXT NOT NULL, " +
                    "gm_secrets_json TEXT NOT NULL, " +
                    "tags_json TEXT NOT NULL, " +
                    "relations_json TEXT NOT NULL, " +
                    "visibility TEXT NOT NULL, " +
                    "discovered INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_narrative_setting_type ON narrative_entities(setting, type)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_narrative_source ON narrative_entities(source_id)");
        } catch (Exception e) {
            AppLog.e("NarrativeContentEngine", "Falha ao garantir schema de entidades narrativas.", e);
        }
    }

    public synchronized boolean seedCanonicalEntity(final Entity canon) {
        if (canon == null || canon.id.length() == 0 || appDb == null) return false;
        try {
            appDb.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override
                public void run() throws Exception {
                    SQLiteDatabase db = appDb.getWritableDatabase();
                    Entity existing = getEntity(canon.id);
                    ContentValues cv = new ContentValues();
                    cv.put("id", canon.id);
                    cv.put("source_id", canon.sourceId);
                    cv.put("setting", canon.setting);
                    cv.put("era", canon.era);
                    cv.put("type", canon.type);
                    cv.put("name", canon.name);
                    cv.put("original_name", canon.originalName);
                    cv.put("summary", canon.summary);

                    // Fusão inteligente de fatos e segredos preservando novidades
                    List<String> mergedFacts = new ArrayList<>(canon.verifiedFacts);
                    List<String> mergedSecrets = new ArrayList<>(canon.gmSecrets);
                    List<String> mergedTags = new ArrayList<>(canon.tags);
                    Map<String, String> mergedRelations = new HashMap<>(canon.relations);

                    boolean discovered = canon.discovered;
                    String visibility = canon.visibility;

                    if (existing != null) {
                        for (String f : existing.verifiedFacts) {
                            if (!mergedFacts.contains(f)) mergedFacts.add(f);
                        }
                        for (String s : existing.gmSecrets) {
                            if (!mergedSecrets.contains(s)) mergedSecrets.add(s);
                        }
                        for (String t : existing.tags) {
                            if (!mergedTags.contains(t)) mergedTags.add(t);
                        }
                        for (Map.Entry<String, String> r : existing.relations.entrySet()) {
                            mergedRelations.put(r.getKey(), r.getValue());
                        }
                        // Descobertas do jogador jamais são revertidas durante a atualização do catálogo.
                        if (existing.discovered) {
                            discovered = true;
                            visibility = "grupo";
                        } else if ("secreto".equals(canon.visibility) || "mestre".equals(canon.visibility)) {
                            // Sigilo editorial recém-adicionado prevalece sobre uma visibilidade pública antiga.
                            visibility = canon.visibility;
                        } else if ("secreto".equals(existing.visibility) || "mestre".equals(existing.visibility)) {
                            // Nunca expor entidades antigas ainda não descobertas por simples re-seed.
                            visibility = existing.visibility;
                        } else {
                            visibility = existing.visibility;
                        }
                    }

                    // Se a entidade foi descoberta e salva em narrative_discovery_state (restaurado de Backup21), preserva
                    if (!discovered && appDb != null) {
                        JSONObject discovery = appDb.getNarrativeDiscovery("entity", canon.id);
                        if (discovery != null && discovery.length() > 0) {
                            discovered = true;
                            visibility = "grupo";
                        }
                    }

                    cv.put("facts_json", new JSONArray(mergedFacts).toString());
                    cv.put("gm_secrets_json", new JSONArray(mergedSecrets).toString());
                    cv.put("tags_json", new JSONArray(mergedTags).toString());
                    cv.put("relations_json", new JSONObject(mergedRelations).toString());
                    cv.put("visibility", visibility);
                    cv.put("discovered", discovered ? 1 : 0);
                    cv.put("updated_at", System.currentTimeMillis());
                    db.insertWithOnConflict("narrative_entities", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            });
            return true;
        } catch (Exception e) {
            AppLog.e("NarrativeContentEngine", "Falha ao semear entidade canônica: " + canon.id, e);
            return false;
        }
    }

    public synchronized boolean saveEntity(final Entity entity) {
        if (entity == null || entity.id.length() == 0 || appDb == null) return false;
        try {
            appDb.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override
                public void run() throws Exception {
                    SQLiteDatabase db = appDb.getWritableDatabase();
                    Entity existing = getEntity(entity.id);
                    ContentValues cv = new ContentValues();
                    cv.put("id", entity.id);
                    cv.put("source_id", entity.sourceId);
                    cv.put("setting", entity.setting);
                    cv.put("era", entity.era);
                    cv.put("type", entity.type);
                    cv.put("name", entity.name);
                    cv.put("original_name", entity.originalName);
                    cv.put("summary", entity.summary);
                    cv.put("facts_json", new JSONArray(entity.verifiedFacts).toString());
                    cv.put("gm_secrets_json", new JSONArray(entity.gmSecrets).toString());
                    cv.put("tags_json", new JSONArray(entity.tags).toString());
                    cv.put("relations_json", new JSONObject(entity.relations).toString());

                    boolean discovered = entity.discovered;
                    String visibility = entity.visibility;
                    if (existing != null && existing.discovered && !entity.discovered) {
                        // Preserva descoberta se já estava descoberta
                        discovered = true;
                        visibility = "grupo";
                    }

                    cv.put("visibility", visibility);
                    cv.put("discovered", discovered ? 1 : 0);
                    cv.put("updated_at", System.currentTimeMillis());
                    db.insertWithOnConflict("narrative_entities", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            });
            return true;
        } catch (Exception e) {
            AppLog.e("NarrativeContentEngine", "Falha ao salvar entidade: " + entity.id, e);
            return false;
        }
    }

    public synchronized Entity getEntity(String entityId) {
        if (entityId == null || entityId.trim().length() == 0 || appDb == null) return null;
        try {
            JSONObject row = appDb.queryOne("SELECT * FROM narrative_entities WHERE id=? LIMIT 1",
                    new String[]{entityId.trim()});
            if (row == null || row.length() == 0) return null;
            return entityFromRow(row);
        } catch (Exception e) {
            AppLog.w("NarrativeContentEngine", "Falha ao buscar entidade: " + entityId, e);
            return null;
        }
    }

    public synchronized List<Entity> listEntitiesBySetting(String setting, String type) {
        ArrayList<Entity> out = new ArrayList<>();
        if (appDb == null) return out;
        try {
            String normSetting = setting != null ? setting.trim().toLowerCase(Locale.US) : "";
            String normType = type != null ? type.trim().toLowerCase(Locale.US) : "";
            String sql;
            String[] args;
            if (normSetting.length() > 0 && normType.length() > 0) {
                sql = "SELECT * FROM narrative_entities WHERE setting=? AND type=? ORDER BY name";
                args = new String[]{normSetting, normType};
            } else if (normSetting.length() > 0) {
                sql = "SELECT * FROM narrative_entities WHERE setting=? ORDER BY name";
                args = new String[]{normSetting};
            } else if (normType.length() > 0) {
                sql = "SELECT * FROM narrative_entities WHERE type=? ORDER BY name";
                args = new String[]{normType};
            } else {
                sql = "SELECT * FROM narrative_entities ORDER BY name";
                args = new String[0];
            }
            List<JSONObject> rows = appDb.queryList(sql, args);
            for (JSONObject row : rows) {
                Entity e = entityFromRow(row);
                if (e != null) out.add(e);
            }
        } catch (Exception e) {
            AppLog.w("NarrativeContentEngine", "Falha ao listar entidades por cenário.", e);
        }
        return out;
    }

    public synchronized boolean discoverEntity(String entityId) {
        return discoverEntity(entityId, "");
    }

    public synchronized boolean discoverEntity(final String entityId, final String discoveredByNote) {
        if (entityId == null || entityId.trim().length() == 0 || appDb == null) return false;
        try {
            final Entity current = getEntity(entityId);
            if (current == null) return false;
            appDb.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override
                public void run() throws Exception {
                    SQLiteDatabase db = appDb.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("discovered", 1);
                    cv.put("visibility", "grupo");
                    cv.put("updated_at", System.currentTimeMillis());
                    db.update("narrative_entities", cv, "id=?", new String[]{entityId.trim()});

                    // Sincroniza com knowledge_facts para que o KnowledgeFirewall reflita a descoberta
                    String content = current.summary + (discoveredByNote != null && discoveredByNote.length() > 0 ? " (" + discoveredByNote + ")" : "");
                    appDb.addKnowledgeFact(current.name, content, "verdade", "grupo", "[]", "", current.sourceId);

                    // Sincroniza com narrative_discovery_state do AppDb para preservação garantida no Backup21
                    appDb.recordNarrativeDiscovery("entity", entityId.trim(), current.name, discoveredByNote != null ? discoveredByNote : "", 1);
                }
            });
            return true;
        } catch (Exception e) {
            AppLog.e("NarrativeContentEngine", "Falha ao descobrir entidade: " + entityId, e);
            return false;
        }
    }

    /**
     * Constrói visão contextual filtrada para o jogador.
     * Os segredos de Mestre e entidades não descobertas são ESTRITAMENTE OMITIDOS.
     */
    public synchronized JSONObject buildPlayerView(String currentSetting, List<String> requestedEntityIds) {
        JSONObject view = new JSONObject();
        JSONArray entitiesArray = new JSONArray();
        int omittedSecretsCount = 0;
        try {
            String normSetting = currentSetting != null ? currentSetting.trim().toLowerCase(Locale.US) : "forgotten_realms";
            if (requestedEntityIds != null) {
                for (String id : requestedEntityIds) {
                    Entity e = getEntity(id);
                    if (e == null) continue;
                    // Isolamento de cenário estrito
                    if (!e.setting.equals(normSetting) && !e.setting.equals("multiverse")) continue;
                    // Se for segredo ou visibilidade de mestre e ainda não foi descoberta, omite
                    if (!e.discovered && ("secreto".equals(e.visibility) || "mestre".equals(e.visibility))) {
                        omittedSecretsCount += e.gmSecrets.size() + 1;
                        continue;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("id", e.id);
                    obj.put("name", e.name);
                    obj.put("type", e.type);
                    obj.put("summary", e.summary);
                    obj.put("verified_facts", new JSONArray(e.verifiedFacts));
                    // gm_secrets NUNCA são enviados ao jogador
                    entitiesArray.put(obj);
                    omittedSecretsCount += e.gmSecrets.size();
                }
            }
            view.put("setting", normSetting);
            view.put("role", "player");
            view.put("entities", entitiesArray);
            view.put("omitted_secrets_protected", omittedSecretsCount);
            view.put("security_status", "gm_secrets_firewalled");
        } catch (Exception ignored) {}
        return view;
    }

    /**
     * Constrói visão contextual completa para o Mestre.
     * Inclui fatos verificados, rumores, segredos e diretrizes narrativas.
     */
    public synchronized JSONObject buildGmView(String currentSetting, List<String> requestedEntityIds) {
        JSONObject view = new JSONObject();
        JSONArray entitiesArray = new JSONArray();
        try {
            String normSetting = currentSetting != null ? currentSetting.trim().toLowerCase(Locale.US) : "forgotten_realms";
            if (requestedEntityIds != null) {
                for (String id : requestedEntityIds) {
                    Entity e = getEntity(id);
                    if (e == null) continue;
                    // Isolamento de cenário estrito
                    if (!e.setting.equals(normSetting) && !e.setting.equals("multiverse")) continue;
                    JSONObject obj = e.toJson();
                    entitiesArray.put(obj);
                }
            }
            view.put("setting", normSetting);
            view.put("role", "gm");
            view.put("entities", entitiesArray);
            view.put("security_status", "full_gm_access");
        } catch (Exception ignored) {}
        return view;
    }

    /**
     * Semeia o cânone oficial dos 10 livros no SQLite de forma transacional e determinística.
     * Retorna o número de entidades instaladas.
     */
    public synchronized int seedOfficialCanon() {
        if (appDb == null) return 0;
        // The catalog is refreshed on every app version without overwriting the
        // player's discoveries. Keep all canonical merges in ONE SQLite transaction
        // instead of opening and committing a transaction for every entity on startup.
        final int[] count = {0};
        try {
            appDb.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override public void run() throws Exception {
                    for (Entity e : com.braseiro.dnd5e.canon.FaerunCanonCatalog.all())
                        if (seedCanonicalEntity(e)) count[0]++;
                    for (Entity e : com.braseiro.dnd5e.canon.EberronCanonCatalog.all())
                        if (seedCanonicalEntity(e)) count[0]++;
                    for (Entity e : com.braseiro.dnd5e.canon.RavenloftCanonCatalog.all())
                        if (seedCanonicalEntity(e)) count[0]++;
                    for (Entity e : com.braseiro.dnd5e.canon.LorwynNetherilArcanaCanonCatalog.all())
                        if (seedCanonicalEntity(e)) count[0]++;
                }
            });
        } catch (Exception e) {
            AppLog.e("NarrativeContentEngine", "Lote canônico não confirmado; a transação foi revertida.", e);
            return 0;
        }
        return count[0];
    }

    /**
     * Retorna a visão contextual adequada para o Gemini Master e KnowledgeFirewall.
     * Se isGm for falso, oculta estritamente todos os gmSecrets.
     */
    public synchronized JSONObject promptKnowledgeContext(String setting, boolean isGm) {
        String normSetting = setting != null ? setting.trim().toLowerCase(Locale.US) : "forgotten_realms";
        List<Entity> list = listEntitiesBySetting(normSetting, "");
        List<String> ids = new ArrayList<>();
        for (Entity e : list) ids.add(e.id);
        // Também inclui multiverse se houver
        if (!"multiverse".equals(normSetting)) {
            List<Entity> multi = listEntitiesBySetting("multiverse", "");
            for (Entity e : multi) ids.add(e.id);
        }
        return isGm ? buildGmView(normSetting, ids) : buildPlayerView(normSetting, ids);
    }

    private Entity entityFromRow(JSONObject row) {
        try {
            String id = row.optString("id");
            String sourceId = row.optString("source_id");
            String setting = row.optString("setting");
            String era = row.optString("era");
            String type = row.optString("type");
            String name = row.optString("name");
            String originalName = row.optString("original_name");
            String summary = row.optString("summary");
            String visibility = row.optString("visibility");
            boolean discovered = row.optInt("discovered", 0) == 1;

            List<String> facts = new ArrayList<>();
            JSONArray factsArr = new JSONArray(row.optString("facts_json", "[]"));
            for (int i = 0; i < factsArr.length(); i++) facts.add(factsArr.optString(i));

            List<String> secrets = new ArrayList<>();
            JSONArray secretsArr = new JSONArray(row.optString("gm_secrets_json", "[]"));
            for (int i = 0; i < secretsArr.length(); i++) secrets.add(secretsArr.optString(i));

            List<String> tags = new ArrayList<>();
            JSONArray tagsArr = new JSONArray(row.optString("tags_json", "[]"));
            for (int i = 0; i < tagsArr.length(); i++) tags.add(tagsArr.optString(i));

            Map<String, String> relations = new HashMap<>();
            JSONObject relObj = new JSONObject(row.optString("relations_json", "{}"));
            java.util.Iterator<String> keys = relObj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                relations.put(k, relObj.optString(k));
            }

            return new Entity(id, sourceId, setting, era, type, name, originalName, summary,
                    facts, secrets, tags, relations, visibility, discovered);
        } catch (Exception e) {
            return null;
        }
    }
}
