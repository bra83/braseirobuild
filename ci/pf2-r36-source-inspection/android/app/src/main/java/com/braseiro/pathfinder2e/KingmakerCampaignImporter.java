package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Motor determinístico de ingestão transacional de capítulos de campanha Kingmaker
 * (Capítulos 1 a 12 do PZO2020/PZO2021) no SQLite nativo (AppDb).
 *
 * Características essenciais:
 * 1. Zero runtime RAG ou leitura de PDF em tempo de execução.
 * 2. Transações SQLite estritamente atômicas (db.beginTransaction() / setTransactionSuccessful() / endTransaction()).
 * 3. Ingestão completa de missões, POIs de hexágono, cenas táticas, combatentes/NPCs, facções,
 *    relógios de mundo e eventos.
 * 4. Validação e exportação de estado de campanha compatível com restauração fail-closed v22.
 */
public final class KingmakerCampaignImporter {

    private KingmakerCampaignImporter() {
        // Utilitário estático não instanciável
    }

    private static String safeString(String val) {
        return val == null ? "" : val.trim();
    }

    private static String slugKey(String value) {
        String s = Normalizer.normalize(safeString(value).toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return s;
    }

    /**
     * Garante a criação idempotente da tabela de capítulos no SQLite.
     */
    public static void ensureSchema(SQLiteDatabase db) {
        if (db == null) return;
        db.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_campaign_chapters ("
                + "world_profile TEXT NOT NULL, "
                + "chapter_key TEXT NOT NULL, "
                + "chapter_number INTEGER NOT NULL, "
                + "title TEXT NOT NULL, "
                + "level_min INTEGER NOT NULL DEFAULT 1, "
                + "level_max INTEGER NOT NULL DEFAULT 20, "
                + "summary TEXT NOT NULL DEFAULT '', "
                + "imported_at INTEGER NOT NULL, "
                + "metadata_json TEXT NOT NULL DEFAULT '{}', "
                + "PRIMARY KEY(world_profile, chapter_key))");
    }

    /**
     * Inspeciona as colunas disponíveis em uma tabela SQLite para adaptação dinâmica.
     */
    private static Set<String> getTableColumns(SQLiteDatabase db, String tableName) {
        Set<String> cols = new HashSet<String>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(nameIdx));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return cols;
    }

    /**
     * Verifica se uma tabela existe no banco de dados.
     */
    private static boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
            return cursor != null && cursor.moveToFirst();
        } catch (Exception ignored) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Ingestão transacional atômica de um catálogo de capítulo no banco SQLite.
     *
     * @param db Conexão SQLite gravável.
     * @param worldProfile Perfil de isolamento de mundo (ex: 'pf2').
     * @param chapterJson JSON oficial do capítulo em pt-BR.
     * @return true se ingerido com sucesso.
     * @throws JSONException se o formato JSON for inválido.
     * @throws SQLiteException se houver falha de escrita no banco.
     */
    public static boolean importChapter(SQLiteDatabase db, String worldProfile, JSONObject chapterJson)
            throws JSONException, SQLiteException {
        if (db == null) {
            throw new IllegalArgumentException("SQLiteDatabase nulo fornecido para importChapter.");
        }
        if (worldProfile == null || worldProfile.trim().length() == 0) {
            worldProfile = "pf2";
        }
        if (chapterJson == null) {
            throw new JSONException("JSONObject do capítulo é nulo.");
        }

        // Parse e validação do modelo com fail-closed
        KingmakerCampaignModels.ChapterCatalog catalog = KingmakerCampaignModels.ChapterCatalog.fromJson(chapterJson);
        catalog.validate();

        ensureSchema(db);

        boolean isTransactionOwner = !db.inTransaction();
        if (isTransactionOwner) {
            db.beginTransaction();
        }

        try {
            long now = System.currentTimeMillis();

            // 1. Ingestão dos Metadados do Capítulo
            persistChapterMetadata(db, worldProfile, catalog, now);

            // 2. Ingestão de Facções
            persistFactions(db, worldProfile, catalog.factions, now);

            // 3. Ingestão de Missões (Quests)
            persistQuests(db, worldProfile, catalog.chapterKey, catalog.quests, now);

            // 4. Ingestão de POIs de Hexágono
            persistPois(db, worldProfile, catalog.chapterKey, catalog.pois, now);

            // 5. Ingestão de Cenas Táticas e Perigos
            persistTacticalScenes(db, worldProfile, catalog, now);

            // 6. Ingestão de NPCs e Companheiros
            persistNpcs(db, worldProfile, catalog.npcs, now);

            // 7. Ingestão de Relógios e Eventos de Mundo
            persistWorldClocks(db, worldProfile, catalog.clocks, now);
            persistWorldEvents(db, worldProfile, catalog.events, now);

            // 8. Ingestão de Marcos de Campanha
            persistMilestones(db, worldProfile, catalog.milestones, now);

            if (isTransactionOwner) {
                db.setTransactionSuccessful();
            }
            return true;
        } finally {
            if (isTransactionOwner) {
                db.endTransaction();
            }
        }
    }

    /**
     * Persiste metadados do capítulo nas tabelas kingmaker_campaign_chapters e kingmaker_features.
     */
    private static void persistChapterMetadata(SQLiteDatabase db, String worldProfile,
                                                KingmakerCampaignModels.ChapterCatalog catalog, long now)
            throws JSONException {
        ContentValues v = new ContentValues();
        v.put("world_profile", worldProfile);
        v.put("chapter_key", catalog.chapterKey);
        v.put("chapter_number", catalog.chapterNumber);
        v.put("title", catalog.title);
        v.put("level_min", catalog.levelMin);
        v.put("level_max", catalog.levelMax);
        v.put("summary", catalog.summary);
        v.put("imported_at", now);
        v.put("metadata_json", catalog.metadata != null ? catalog.metadata.toString() : "{}");
        db.insertWithOnConflict("kingmaker_campaign_chapters", null, v, SQLiteDatabase.CONFLICT_REPLACE);

        if (tableExists(db, "kingmaker_features")) {
            ContentValues f = new ContentValues();
            f.put("world_profile", worldProfile);
            f.put("feature_key", "campaign_chapter:" + catalog.chapterKey);
            f.put("feature_kind", "campaign_chapter");
            f.put("name", catalog.title);
            f.put("level", catalog.levelMin);
            f.put("status", "completed");

            JSONObject eff = new JSONObject();
            eff.put("chapter_number", catalog.chapterNumber);
            eff.put("level_min", catalog.levelMin);
            eff.put("level_max", catalog.levelMax);
            f.put("effects_json", eff.toString());

            f.put("source_ref", "Kingmaker Adventure Path PZO2020");
            f.put("notes", catalog.summary);
            f.put("created_at", now);
            f.put("updated_at", now);
            db.insertWithOnConflict("kingmaker_features", null, f, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Persiste facções declaradas no capítulo.
     */
    private static void persistFactions(SQLiteDatabase db, String worldProfile,
                                        List<KingmakerCampaignModels.FactionModel> factions, long now) {
        if (!tableExists(db, "factions") || factions == null || factions.isEmpty()) {
            return;
        }
        Set<String> cols = getTableColumns(db, "factions");
        for (KingmakerCampaignModels.FactionModel f : factions) {
            ContentValues v = new ContentValues();
            if (cols.contains("world_profile")) v.put("world_profile", worldProfile);
            if (cols.contains("name")) v.put("name", f.name);
            if (cols.contains("agenda")) v.put("agenda", f.agenda);
            if (cols.contains("resources")) v.put("resources", f.resources);
            if (cols.contains("relationship")) v.put("relationship", f.relationship);
            if (cols.contains("current_plan")) v.put("current_plan", f.currentPlan);
            if (cols.contains("location")) v.put("location", f.location);
            if (cols.contains("active")) v.put("active", f.active ? 1 : 0);
            if (cols.contains("updated_at")) v.put("updated_at", now);

            db.insertWithOnConflict("factions", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Persiste missões (quests) com suporte adaptativo aos esquemas do AppDb e de testes unitários.
     */
    private static void persistQuests(SQLiteDatabase db, String worldProfile, String chapterKey,
                                      List<KingmakerCampaignModels.QuestModel> quests, long now)
            throws JSONException {
        if (!tableExists(db, "quests") || quests == null || quests.isEmpty()) {
            return;
        }
        Set<String> cols = getTableColumns(db, "quests");

        for (KingmakerCampaignModels.QuestModel q : quests) {
            ContentValues v = new ContentValues();
            if (cols.contains("world_profile")) v.put("world_profile", worldProfile);
            if (cols.contains("quest_key")) v.put("quest_key", q.questKey);
            if (cols.contains("chapter_key")) v.put("chapter_key", chapterKey);
            if (cols.contains("title")) v.put("title", q.title);
            if (cols.contains("status")) v.put("status", q.status);
            if (cols.contains("stage")) v.put("stage", q.stage);
            if (cols.contains("objective")) v.put("objective", q.objective);
            if (cols.contains("stakes")) v.put("stakes", q.stakes);
            if (cols.contains("owner_character_id")) v.put("owner_character_id", q.ownerCharacterId);
            if (cols.contains("faction_id")) v.put("faction_id", q.factionId);
            if (cols.contains("milestone_xp")) v.put("milestone_xp", q.milestoneXp);

            // Se colunas específicas de json existirem (como no harness E2E)
            if (cols.contains("prerequisites_json")) {
                JSONArray pr = new JSONArray();
                for (String p : q.prerequisites) pr.put(p);
                v.put("prerequisites_json", pr.toString());
            }
            if (cols.contains("objectives_json")) {
                JSONArray ob = new JSONArray();
                for (KingmakerCampaignModels.QuestObjective o : q.objectives) ob.put(o.toJson());
                v.put("objectives_json", ob.toString());
            }
            if (cols.contains("rewards_json")) {
                v.put("rewards_json", q.rewards != null ? q.rewards.toString() : "{}");
            }

            // Empacotar dados estendidos no progress_json caso colunas separadas não existam
            if (cols.contains("progress_json")) {
                JSONObject prog = q.progressJson != null ? new JSONObject(q.progressJson.toString()) : new JSONObject();
                prog.put("quest_key", q.questKey);
                prog.put("chapter_key", chapterKey);
                prog.put("milestone_xp", q.milestoneXp);
                JSONArray prArr = new JSONArray();
                for (String p : q.prerequisites) prArr.put(p);
                prog.put("prerequisites", prArr);

                JSONArray objArr = new JSONArray();
                for (KingmakerCampaignModels.QuestObjective o : q.objectives) objArr.put(o.toJson());
                prog.put("objectives", objArr);

                prog.put("rewards", q.rewards != null ? q.rewards : new JSONObject());
                v.put("progress_json", prog.toString());
            }

            if (cols.contains("created_at")) v.put("created_at", now);
            if (cols.contains("updated_at")) v.put("updated_at", now);

            // Verifica se a quest já existe para evitar duplicações
            boolean exists = false;
            if (cols.contains("quest_key")) {
                Cursor c = db.rawQuery("SELECT id FROM quests WHERE world_profile=? AND quest_key=?",
                        new String[]{worldProfile, q.questKey});
                if (c != null) {
                    exists = c.moveToFirst();
                    c.close();
                }
            } else {
                Cursor c = db.rawQuery("SELECT id FROM quests WHERE world_profile=? AND title=? COLLATE NOCASE",
                        new String[]{worldProfile, q.title});
                if (c != null) {
                    exists = c.moveToFirst();
                    c.close();
                }
            }

            if (exists) {
                Cursor saved=null;
                try{
                    String identity=cols.contains("quest_key")?"quest_key":"title";
                    String match=cols.contains("quest_key")?q.questKey:q.title;
                    saved=db.rawQuery("SELECT * FROM quests WHERE world_profile=? AND "+identity+"=?",new String[]{worldProfile,match});
                    if(saved!=null&&saved.moveToFirst()){
                        for(String field:new String[]{"status","stage","created_at","progress_json"}){
                            int index=saved.getColumnIndex(field);if(index<0)continue;
                            if("created_at".equals(field))v.put(field,saved.getLong(index));
                            else if("progress_json".equals(field)){
                                JSONObject old=new JSONObject(saved.getString(index)==null?"{}":saved.getString(index));
                                JSONObject canonical=new JSONObject(v.get("progress_json")==null?"{}":String.valueOf(v.get("progress_json")));
                                for(String fixed:new String[]{"quest_key","chapter_key","milestone_xp","prerequisites","rewards"})if(canonical.has(fixed))old.put(fixed,canonical.opt(fixed));
                                v.put(field,old.toString());
                            }else v.put(field,saved.getString(index));
                        }
                    }
                }finally{if(saved!=null)saved.close();}
                if (cols.contains("quest_key")) {
                    db.update("quests", v, "world_profile=? AND quest_key=?", new String[]{worldProfile, q.questKey});
                } else {
                    db.update("quests", v, "world_profile=? AND title=? COLLATE NOCASE", new String[]{worldProfile, q.title});
                }
            } else {
                db.insert("quests", null, v);
            }
        }
    }

    /**
     * Persiste POIs de hexágono garantindo estrita integridade com constraints do AppDb.
     */
    private static void persistPois(SQLiteDatabase db, String worldProfile, String chapterKey,
                                   List<KingmakerCampaignModels.HexPoiModel> pois, long now)
            throws JSONException {
        if (!tableExists(db, "campaign_pois") || pois == null || pois.isEmpty()) {
            return;
        }
        Set<String> cols = getTableColumns(db, "campaign_pois");

        for (KingmakerCampaignModels.HexPoiModel p : pois) {
            ContentValues v = new ContentValues();
            if (cols.contains("world_profile")) v.put("world_profile", worldProfile);
            String rawPoiId = (p.poiId != null && p.poiId.trim().length() > 0) ? p.poiId.trim() : (p.poiKey != null && p.poiKey.trim().length() > 0 ? p.poiKey.trim() : slugKey(p.name));
            String scopedPoiId = rawPoiId.startsWith(worldProfile + ":") ? rawPoiId : (worldProfile + ":" + rawPoiId);
            if (cols.contains("poi_id")) v.put("poi_id", scopedPoiId);
            if (cols.contains("poi_key")) v.put("poi_key", p.poiKey.length() > 0 ? p.poiKey : rawPoiId);
            if (cols.contains("chapter_key")) v.put("chapter_key", chapterKey);
            if (cols.contains("campaign_key")) v.put("campaign_key", p.campaignKey.length() > 0 ? p.campaignKey : chapterKey);
            if (cols.contains("map_id")) v.put("map_id", p.mapId);
            if (cols.contains("name")) v.put("name", p.name);
            if (cols.contains("name_key")) v.put("name_key", p.nameKey.length() > 0 ? p.nameKey : slugKey(p.name));
            if (cols.contains("type")) v.put("type", p.type);
            if (cols.contains("poi_type")) v.put("poi_type", p.type);
            if (cols.contains("hex_id")) v.put("hex_id", p.hexId);
            if (cols.contains("location_id")) v.put("location_id", p.locationId);
            if (cols.contains("q")) v.put("q", p.q);
            if (cols.contains("r")) v.put("r", p.r);

            // CHECK(source IN ('campaign','procedural'))
            if (cols.contains("source")) v.put("source", "campaign");
            if (cols.contains("origin")) v.put("origin", p.origin);

            // Invariante MANDATÓRIA: CHECK(canonical=0)
            if (cols.contains("canonical")) v.put("canonical", 0);

            if (cols.contains("hidden")) v.put("hidden", p.hidden ? 1 : 0);
            if (cols.contains("discovered")) v.put("discovered", p.discovered ? 1 : 0);
            if (cols.contains("created_by")) v.put("created_by", p.createdBy);
            if (cols.contains("created_tick")) v.put("created_tick", p.createdTick);

            // Se for a tabela com CHECK(status IN ('active','abandoned','ruined','destroyed','occupied','rebuilding'))
            // Mapeamos para 'active' se o status for 'unexplored'/'hidden'/'secured'
            if (cols.contains("details_json")) {
                // Tabela estilo teste E2E
                v.put("status", p.status);
                JSONObject det = new JSONObject();
                det.put("notes", p.notes);
                det.put("description", p.description);
                v.put("details_json", det.toString());
            } else {
                // Tabela canônica de produção AppDb
                String st = p.status;
                if (!st.matches("active|abandoned|ruined|destroyed|occupied|rebuilding")) {
                    st = "active";
                }
                if (cols.contains("status")) v.put("status", st);
            }

            if (cols.contains("description")) v.put("description", p.description);
            if (cols.contains("tags_json")) {
                JSONArray tg = new JSONArray();
                for (String t : p.tags) tg.put(t);
                v.put("tags_json", tg.toString());
            }
            if (cols.contains("faction_id")) v.put("faction_id", p.factionId);
            if (cols.contains("owner_npc_id")) v.put("owner_npc_id", p.ownerNpcId);
            if (cols.contains("parent_poi_id")) {
                if (p.parentPoiId != null && p.parentPoiId.trim().length() > 0) {
                    String scopedParent = p.parentPoiId.trim().startsWith(worldProfile + ":") ? p.parentPoiId.trim() : (worldProfile + ":" + p.parentPoiId.trim());
                    v.put("parent_poi_id", scopedParent);
                } else {
                    v.put("parent_poi_id", p.parentPoiId);
                }
            }
            if (cols.contains("development_level")) v.put("development_level", p.developmentLevel);
            if (cols.contains("icon")) v.put("icon", p.icon);
            if (cols.contains("notes")) v.put("notes", p.notes);
            if (cols.contains("private_notes")) v.put("private_notes", p.privateNotes);
            if (cols.contains("settlement_key")) v.put("settlement_key", p.settlementKey);
            if (cols.contains("settlement_json")) v.put("settlement_json", p.settlementJson != null ? p.settlementJson.toString() : "{}");
            if (cols.contains("state_json")) v.put("state_json", p.stateJson != null ? p.stateJson.toString() : "{}");
            if (cols.contains("created_at")) v.put("created_at", now);
            if (cols.contains("updated_at")) v.put("updated_at", now);

            db.insertWithOnConflict("campaign_pois", null, v, SQLiteDatabase.CONFLICT_REPLACE);

            // Garantir que o hexágono exista em kingmaker_hexes se a tabela existir
            if (tableExists(db, "kingmaker_hexes") && p.hexId.length() > 0) {
                ContentValues h = new ContentValues();
                h.put("world_profile", worldProfile);
                h.put("map_id", p.mapId);
                h.put("hex_id", p.hexId);
                h.put("updated_at", now);
                db.insertWithOnConflict("kingmaker_hexes", null, h, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    /**
     * Persiste cenas táticas com suas zonas, obstáculos e perigos canônicos.
     */
    private static void persistTacticalScenes(SQLiteDatabase db, String worldProfile,
                                              KingmakerCampaignModels.ChapterCatalog catalog, long now)
            throws JSONException {
        if (!tableExists(db, "tactical_scenes")) {
            return;
        }
        Set<String> cols = getTableColumns(db, "tactical_scenes");

        // 1. Persistir cenas declaradas explicitamente
        for (KingmakerCampaignModels.TacticalSceneModel scene : catalog.scenes) {
            saveSingleTacticalScene(db, worldProfile, scene, cols, now);
        }

        // 2. Se o capítulo possuir perigos canônicos de nível de capítulo e nenhuma cena dedicada,
        // cria uma cena tática canônica agregando os perigos do capítulo
        if (!catalog.hazards.isEmpty()) {
            String hazardsSceneKey = catalog.chapterKey + "_perigos_canonicos";
            KingmakerCampaignModels.TacticalSceneModel hazardScene = new KingmakerCampaignModels.TacticalSceneModel();
            hazardScene.sceneKey = hazardsSceneKey;
            hazardScene.title = "Perigos Canônicos: " + catalog.title;
            hazardScene.location = catalog.title;
            hazardScene.hexId = catalog.pois.isEmpty() ? "" : catalog.pois.get(0).hexId;
            hazardScene.environment = "ermos";
            hazardScene.hazards.addAll(catalog.hazards);
            saveSingleTacticalScene(db, worldProfile, hazardScene, cols, now);
        }
    }

    private static void saveSingleTacticalScene(SQLiteDatabase db, String worldProfile,
                                                KingmakerCampaignModels.TacticalSceneModel scene,
                                                Set<String> cols, long now) throws JSONException {
        ContentValues v = new ContentValues();
        if (cols.contains("world_profile")) v.put("world_profile", worldProfile);
        if (cols.contains("scene_key")) v.put("scene_key", scene.sceneKey);
        if (cols.contains("title")) v.put("title", scene.title);
        if (cols.contains("location")) v.put("location", scene.location);
        if (cols.contains("hex_id")) v.put("hex_id", scene.hexId);
        if (cols.contains("environment")) v.put("environment", scene.environment);
        if (cols.contains("width_cells")) v.put("width_cells", scene.widthCells);
        if (cols.contains("height_cells")) v.put("height_cells", scene.heightCells);
        if (cols.contains("seed")) v.put("seed", scene.seed);

        if (cols.contains("zones_json")) {
            JSONArray zArr = new JSONArray();
            for (KingmakerCampaignModels.SceneZone z : scene.zones) zArr.put(z.toJson());
            v.put("zones_json", zArr.toString());
        }

        if (cols.contains("obstacles_json")) {
            JSONArray oArr = new JSONArray();
            for (KingmakerCampaignModels.SceneObstacle o : scene.obstacles) oArr.put(o.toJson());
            v.put("obstacles_json", oArr.toString());
        }

        if (cols.contains("features_json")) {
            v.put("features_json", scene.features != null ? scene.features.toString() : "[]");
        }
        if (cols.contains("cover_json")) {
            v.put("cover_json", scene.cover != null ? scene.cover.toString() : "[]");
        }

        if (cols.contains("hazards_json")) {
            JSONArray hArr = new JSONArray();
            for (KingmakerCampaignModels.CanonicalHazardModel h : scene.hazards) hArr.put(h.toJson());
            v.put("hazards_json", hArr.toString());
        }

        if (cols.contains("entry_points_json")) {
            JSONArray epArr = new JSONArray();
            for (KingmakerCampaignModels.SceneEntryPoint ep : scene.entryPoints) epArr.put(ep.toJson());
            v.put("entry_points_json", epArr.toString());
        }

        if (cols.contains("combatants_json")) {
            v.put("combatants_json", scene.combatants != null ? scene.combatants.toString() : "[]");
        }

        if (cols.contains("source_context_json")) {
            v.put("source_context_json", scene.sourceContext != null ? scene.sourceContext.toString() : "{}");
        }

        if (cols.contains("status")) v.put("status", scene.status);
        if (cols.contains("created_at")) v.put("created_at", now);
        if (cols.contains("updated_at")) v.put("updated_at", now);

        db.insertWithOnConflict("tactical_scenes", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Persiste NPCs em npc_memory_profiles, npc_cognitive_state, combatants e kingmaker_companions.
     */
    private static void persistNpcs(SQLiteDatabase db, String worldProfile,
                                   List<KingmakerCampaignModels.NpcModel> npcs, long now)
            throws JSONException {
        if (npcs == null || npcs.isEmpty()) {
            return;
        }

        boolean hasNpcMemory = tableExists(db, "npc_memory_profiles");
        boolean hasNpcCognitive = tableExists(db, "npc_cognitive_state");
        boolean hasCombatants = tableExists(db, "combatants");
        boolean hasCompanions = tableExists(db, "kingmaker_companions");

        Set<String> combatantCols = hasCombatants ? getTableColumns(db, "combatants") : new HashSet<String>();

        for (KingmakerCampaignModels.NpcModel npc : npcs) {
            // 1. npc_memory_profiles
            if (hasNpcMemory) {
                ContentValues v = new ContentValues();
                v.put("world_profile", worldProfile);
                v.put("stable_id", npc.stableId);
                v.put("name", npc.name);
                v.put("summary", npc.summary);
                v.put("goals", npc.goals);
                v.put("secrets", npc.secrets);
                v.put("attitude", npc.attitude);
                v.put("relationship", npc.relationship);
                v.put("knowledge", npc.knowledge);
                v.put("debts", npc.debts);
                v.put("last_seen", npc.lastSeen);
                v.put("token_uri", npc.tokenUri);
                v.put("visual_description", npc.visualDescription);
                v.put("stats_json", npc.stats != null ? npc.stats.toString() : "{}");
                v.put("hex_id", npc.hexId);
                v.put("active", 1);
                v.put("voice_profile", npc.voiceProfile != null ? npc.voiceProfile.toString() : "");
                v.put("updated_at", now);
                db.insertWithOnConflict("npc_memory_profiles", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // 2. npc_cognitive_state
            if (hasNpcCognitive) {
                ContentValues c = new ContentValues();
                c.put("world_profile", worldProfile);
                c.put("npc_name", npc.name);
                c.put("emotion", "neutro");
                c.put("trust", "prestativo".equalsIgnoreCase(npc.attitude) ? 3 : "amistoso".equalsIgnoreCase(npc.attitude) ? 2 : 0);
                c.put("fear", "hostil".equalsIgnoreCase(npc.attitude) ? 2 : 0);
                c.put("suspicion", "desfavoravel".equalsIgnoreCase(npc.attitude) ? 2 : 0);
                c.put("urgency", 0);
                c.put("next_action", "");
                c.put("schedule_json", "[]");
                c.put("beliefs_json", "[]");
                c.put("faction", npc.faction);
                c.put("current_location", npc.hexId);
                c.put("last_interaction_summary", npc.summary);
                c.put("last_action_at", now);
                c.put("updated_at", now);
                db.insertWithOnConflict("npc_cognitive_state", null, c, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // 3. combatants
            if (hasCombatants) {
                ContentValues cb = new ContentValues();
                if (combatantCols.contains("world_profile")) cb.put("world_profile", worldProfile);
                if (combatantCols.contains("npc_key")) cb.put("npc_key", npc.npcKey);
                if (combatantCols.contains("name")) cb.put("name", npc.name);
                if (combatantCols.contains("attitude")) cb.put("attitude", npc.attitude);
                if (combatantCols.contains("faction")) cb.put("faction", npc.faction);

                if (combatantCols.contains("dialogue_json")) {
                    JSONArray dArr = new JSONArray();
                    for (KingmakerCampaignModels.NpcDialogueNode d : npc.dialogueTree) dArr.put(d.toJson());
                    cb.put("dialogue_json", dArr.toString());
                }

                if (combatantCols.contains("hp")) cb.put("hp", npc.stats.optInt("hp", 20));
                if (combatantCols.contains("max_hp")) cb.put("max_hp", npc.stats.optInt("max_hp", 20));
                if (combatantCols.contains("ac")) cb.put("ac", npc.stats.optInt("ac", 15));
                if (combatantCols.contains("initiative")) cb.put("initiative", 0);
                if (combatantCols.contains("source_kind")) cb.put("source_kind", "campaign");
                if (combatantCols.contains("is_player")) cb.put("is_player", 0);
                if (combatantCols.contains("created_at")) cb.put("created_at", now);
                if (combatantCols.contains("updated_at")) cb.put("updated_at", now);

                db.insertWithOnConflict("combatants", null, cb, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // 4. kingmaker_companions (se o NPC for um dos companheiros de Kingmaker)
            if (hasCompanions && (npc.isCompanion || isKnownCompanion(npc.npcKey, npc.name))) {
                ContentValues comp = new ContentValues();
                comp.put("world_profile", worldProfile);
                comp.put("companion_key", npc.npcKey.length() > 0 ? npc.npcKey : slugKey(npc.name));
                comp.put("name", npc.name);
                comp.put("influence", 0);
                comp.put("attitude", npc.attitude);
                comp.put("assignment", "reserve");
                comp.put("available", 1);
                comp.put("notes", npc.summary);
                comp.put("created_at", now);
                comp.put("updated_at", now);
                db.insertWithOnConflict("kingmaker_companions", null, comp, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    private static boolean isKnownCompanion(String key, String name) {
        String k = slugKey(key + " " + name);
        String[] companions = {"amiri", "linzi", "valerie", "harrim", "ekundayo", "jubilost",
                "tristian", "nok_nok", "noknok", "jaethal", "regongar", "octavia", "kalikke", "kanerah"};
        for (String c : companions) {
            if (k.contains(c)) return true;
        }
        return false;
    }

    /**
     * Persiste relógios de mundo.
     */
    private static void persistWorldClocks(SQLiteDatabase db, String worldProfile,
                                          List<KingmakerCampaignModels.WorldClockModel> clocks, long now) {
        if (!tableExists(db, "world_clocks") || clocks == null || clocks.isEmpty()) {
            return;
        }
        for (KingmakerCampaignModels.WorldClockModel c : clocks) {
            ContentValues v = new ContentValues();
            v.put("world_profile", worldProfile);
            v.put("name", c.name);
            v.put("current_value", c.currentValue);
            v.put("max_value", c.maxValue);
            v.put("consequence", c.consequence);
            v.put("automatic", c.automatic ? 1 : 0);
            v.put("active", c.active ? 1 : 0);
            v.put("updated_at", now);
            db.insert("world_clocks", null, v);
        }
    }

    /**
     * Persiste eventos de mundo agendados.
     */
    private static void persistWorldEvents(SQLiteDatabase db, String worldProfile,
                                          List<KingmakerCampaignModels.WorldEventModel> events, long now) {
        if (!tableExists(db, "world_events") || events == null || events.isEmpty()) {
            return;
        }
        for (KingmakerCampaignModels.WorldEventModel e : events) {
            ContentValues v = new ContentValues();
            v.put("world_profile", worldProfile);
            v.put("title", e.title);
            v.put("description", e.description);
            v.put("due_at", e.dueAt);
            v.put("due_day", e.dueDay);
            v.put("status", e.status);
            v.put("consequence_json", e.consequenceJson != null ? e.consequenceJson.toString() : "{}");
            v.put("created_at", now);
            v.put("resolved_at", 0);
            db.insert("world_events", null, v);
        }
    }

    /**
     * Persiste marcos de campanha em kingmaker_features.
     */
    private static void persistMilestones(SQLiteDatabase db, String worldProfile,
                                         List<KingmakerCampaignModels.CampaignMilestone> milestones, long now)
            throws JSONException {
        if (!tableExists(db, "kingmaker_features") || milestones == null || milestones.isEmpty()) {
            return;
        }
        for (KingmakerCampaignModels.CampaignMilestone m : milestones) {
            ContentValues v = new ContentValues();
            v.put("world_profile", worldProfile);
            v.put("feature_key", "milestone:" + (m.milestoneKey.length() > 0 ? m.milestoneKey : slugKey(m.name)));
            v.put("feature_kind", "milestone");
            v.put("name", m.name);
            v.put("level", 1);
            v.put("status", "active");

            JSONObject eff = new JSONObject();
            eff.put("xp", m.xp);
            v.put("effects_json", eff.toString());

            v.put("source_ref", m.sourceRef.length() > 0 ? m.sourceRef : "Kingmaker Campaign Milestones");
            v.put("notes", "Marco canônico de campanha.");
            v.put("created_at", now);
            v.put("updated_at", now);

            db.insertWithOnConflict("kingmaker_features", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    /**
     * Verificação determinística se um capítulo já foi importado no perfil de mundo.
     */
    public static boolean isChapterImported(SQLiteDatabase db, String worldProfile, String chapterKey) {
        if (db == null || chapterKey == null || chapterKey.trim().length() == 0) {
            return false;
        }
        if (worldProfile == null || worldProfile.trim().length() == 0) {
            worldProfile = "pf2";
        }

        // 1. Checa kingmaker_campaign_chapters
        if (tableExists(db, "kingmaker_campaign_chapters")) {
            Cursor c = null;
            try {
                c = db.rawQuery("SELECT 1 FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",
                        new String[]{worldProfile, chapterKey});
                if (c != null && c.moveToFirst()) {
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.close();
            }
        }

        // 2. Checa kingmaker_features
        if (tableExists(db, "kingmaker_features")) {
            Cursor c = null;
            try {
                c = db.rawQuery("SELECT 1 FROM kingmaker_features WHERE world_profile=? AND (feature_key=? OR feature_key=?)",
                        new String[]{worldProfile, "campaign_chapter:" + chapterKey, "milestone:" + chapterKey});
                if (c != null && c.moveToFirst()) {
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.close();
            }
        }

        // 3. Checa quests por chapter_key
        if (tableExists(db, "quests")) {
            Set<String> cols = getTableColumns(db, "quests");
            if (cols.contains("chapter_key")) {
                Cursor c = null;
                try {
                    c = db.rawQuery("SELECT 1 FROM quests WHERE world_profile=? AND chapter_key=? LIMIT 1",
                            new String[]{worldProfile, chapterKey});
                    if (c != null && c.moveToFirst()) {
                        return true;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.close();
                }
            }
        }

        return false;
    }

    /**
     * Exporta o agregado do estado de campanha para restauração e validação de backup v22.
     */
    public static JSONObject exportCampaignState(SQLiteDatabase db, String worldProfile) throws JSONException {
        JSONObject out = new JSONObject();
        if (db == null) return out;
        if (worldProfile == null || worldProfile.trim().length() == 0) worldProfile = "pf2";

        out.put("worldProfile", worldProfile);
        out.put("exportedAt", System.currentTimeMillis());

        out.put("chapters", queryTableRows(db, "kingmaker_campaign_chapters", worldProfile));
        out.put("quests", queryTableRows(db, "quests", worldProfile));
        out.put("campaignPois", queryTableRows(db, "campaign_pois", worldProfile));
        out.put("tacticalScenes", queryTableRows(db, "tactical_scenes", worldProfile));
        out.put("combatants", queryTableRows(db, "combatants", worldProfile));
        out.put("factions", queryTableRows(db, "factions", worldProfile));
        out.put("worldClocks", queryTableRows(db, "world_clocks", worldProfile));
        out.put("worldEvents", queryTableRows(db, "world_events", worldProfile));
        out.put("companions", queryTableRows(db, "kingmaker_companions", worldProfile));
        out.put("npcMemories", queryTableRows(db, "npc_memory_profiles", worldProfile));
        out.put("npcCognitiveStates", queryTableRows(db, "npc_cognitive_state", worldProfile));

        return out;
    }

    private static JSONArray queryTableRows(SQLiteDatabase db, String tableName, String worldProfile) {
        JSONArray arr = new JSONArray();
        if (!tableExists(db, tableName)) return arr;
        Set<String> cols = getTableColumns(db, tableName);
        String sql = cols.contains("world_profile") ? "SELECT * FROM " + tableName + " WHERE world_profile=?" : "SELECT * FROM " + tableName;
        String[] args = cols.contains("world_profile") ? new String[]{worldProfile} : null;

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, args);
            if (cursor != null) {
                String[] columnNames = cursor.getColumnNames();
                while (cursor.moveToNext()) {
                    JSONObject row = new JSONObject();
                    for (int i = 0; i < columnNames.length; i++) {
                        String col = columnNames[i];
                        int type = cursor.getType(i);
                        if (type == Cursor.FIELD_TYPE_NULL) {
                            row.put(col, JSONObject.NULL);
                        } else if (type == Cursor.FIELD_TYPE_INTEGER) {
                            row.put(col, cursor.getLong(i));
                        } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                            row.put(col, cursor.getDouble(i));
                        } else {
                            row.put(col, cursor.getString(i));
                        }
                    }
                    arr.put(row);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr;
    }

    /**
     * Auditoria fail-closed do estado de campanha restaurado contra o contrato de backup v22.
     */
    public static JSONObject validateCampaignRestore(JSONObject backupRoot) {
        JSONObject report = new JSONObject();
        JSONArray errors = new JSONArray();
        JSONArray warnings = new JSONArray();
        JSONObject stats = new JSONObject();

        try {
            if (backupRoot == null) {
                errors.put("Backup nulo fornecido para validação.");
                report.put("valid", false);
                report.put("healthy", false);
                report.put("errors", errors);
                report.put("warnings", warnings);
                report.put("stats", stats);
                return report;
            }

            // Validação de POIs
            if (backupRoot.has("campaignPois") && backupRoot.optJSONArray("campaignPois") == null) {
                errors.put("campaignPois deve ser um array");
            } else {
                JSONArray pois = backupRoot.optJSONArray("campaignPois");
                if (pois != null) {
                    Set<String> seenIds = new HashSet<String>();
                    int validPois = 0;
                    for (int i = 0; i < pois.length(); i++) {
                        JSONObject p = pois.optJSONObject(i);
                        if (p == null) {
                            errors.put("campaignPois[" + i + "] linha nula");
                            continue;
                        }
                        String id = p.optString("poi_id", p.optString("poi_key", ""));
                        if (id.length() == 0 || !seenIds.add(id)) {
                            errors.put("campaignPois[" + i + "] ID ausente ou duplicado: " + id);
                        }
                        if (p.optInt("canonical", 0) != 0 || p.optBoolean("canonical", false)) {
                            errors.put("campaignPois[" + i + "] canonical deve ser 0");
                        }
                        String src = p.optString("source", "");
                        if (src.length() > 0 && !"campaign".equals(src) && !"procedural".equals(src)) {
                            errors.put("campaignPois[" + i + "] source inválido: " + src);
                        }
                        if (p.optString("hex_id", "").trim().length() == 0) {
                            errors.put("campaignPois[" + i + "] sem hex_id");
                        }
                        validPois++;
                    }
                    stats.put("pois_count", validPois);
                }
            }

            // Validação de Cenas Táticas e Perigos Canônicos
            if (backupRoot.has("tacticalScenes") && backupRoot.optJSONArray("tacticalScenes") == null) {
                errors.put("tacticalScenes deve ser um array");
            } else {
                JSONArray scenes = backupRoot.optJSONArray("tacticalScenes");
                if (scenes != null) {
                    Set<String> sceneKeys = new HashSet<String>();
                    int validScenes = 0;
                    int totalHazards = 0;

                    for (int i = 0; i < scenes.length(); i++) {
                        JSONObject s = scenes.optJSONObject(i);
                        if (s == null) continue;
                        String sk = s.optString("scene_key", "");
                        if (sk.length() == 0 || !sceneKeys.add(sk)) {
                            errors.put("tacticalScenes[" + i + "] chave de cena inválida ou duplicada: " + sk);
                        }

                        // Validação de perigos inclusos
                        String rawHazards = s.optString("hazards_json", "[]");
                        try {
                            JSONArray hazardsArr = new JSONArray(rawHazards);
                            for (int h = 0; h < hazardsArr.length(); h++) {
                                JSONObject haz = hazardsArr.optJSONObject(h);
                                if (haz != null) {
                                    int lvl = haz.optInt("level", -1);
                                    if (lvl < 0 || lvl > 25) {
                                        errors.put("Perigo com nível inválido (" + lvl + ") em cena " + sk);
                                    }
                                    int stealthDc = haz.optInt("stealth_dc", 0);
                                    if (stealthDc <= 0) {
                                        errors.put("Perigo sem CD de furtividade positiva em cena " + sk);
                                    }
                                    int saveDc = haz.optInt("saving_throw_dc", 0);
                                    if (saveDc <= 0) {
                                        errors.put("Perigo sem CD de salvaguarda positiva em cena " + sk);
                                    }
                                    totalHazards++;
                                }
                            }
                        } catch (Exception ex) {
                            errors.put("hazards_json corrompido em cena " + sk + ": " + ex.getMessage());
                        }
                        validScenes++;
                    }
                    stats.put("scenes_count", validScenes);
                    stats.put("hazards_count", totalHazards);
                }
            }

            // Validação de Quests
            if (backupRoot.has("quests") && backupRoot.optJSONArray("quests") == null) {
                errors.put("quests deve ser um array");
            } else {
                JSONArray quests = backupRoot.optJSONArray("quests");
                if (quests != null) {
                    int validQuests = 0;
                    for (int i = 0; i < quests.length(); i++) {
                        JSONObject q = quests.optJSONObject(i);
                        if (q == null) continue;
                        String title = q.optString("title", "");
                        if (title.length() == 0) {
                            errors.put("quests[" + i + "] sem título");
                        }
                        validQuests++;
                    }
                    stats.put("quests_count", validQuests);
                }
            }

            // Validação de Chapters
            if (backupRoot.has("chapters") && backupRoot.optJSONArray("chapters") == null) {
                errors.put("chapters deve ser um array");
            } else if (backupRoot.has("chapters")) {
                JSONArray chs = backupRoot.optJSONArray("chapters");
                if (chs != null) {
                    int validChapters = 0;
                    for (int i = 0; i < chs.length(); i++) {
                        JSONObject c = chs.optJSONObject(i);
                        if (c == null) {
                            errors.put("chapters[" + i + "] linha nula");
                            continue;
                        }
                        if (c.optString("chapter_key", "").trim().length() == 0) {
                            errors.put("chapters[" + i + "] sem chapter_key");
                        }
                        validChapters++;
                    }
                    stats.put("chapters_count", validChapters);
                }
            }

            // Validação de Factions
            if (backupRoot.has("factions") && backupRoot.optJSONArray("factions") == null) {
                errors.put("factions deve ser um array");
            } else if (backupRoot.has("factions")) {
                JSONArray facts = backupRoot.optJSONArray("factions");
                if (facts != null) {
                    int validFactions = 0;
                    for (int i = 0; i < facts.length(); i++) {
                        JSONObject f = facts.optJSONObject(i);
                        if (f == null) {
                            errors.put("factions[" + i + "] linha nula");
                            continue;
                        }
                        if (f.optString("name", "").trim().length() == 0) {
                            errors.put("factions[" + i + "] sem nome");
                        }
                        validFactions++;
                    }
                    stats.put("factions_count", validFactions);
                }
            }

            // Validação de coleções adicionais de campanha
            String[] additionalCollections = new String[]{
                "combatants", "worldClocks", "worldEvents", "companions", "npcMemories", "npcCognitiveStates"
            };
            for (String coll : additionalCollections) {
                if (backupRoot.has(coll) && backupRoot.optJSONArray(coll) == null) {
                    errors.put(coll + " deve ser um array");
                } else if (backupRoot.has(coll)) {
                    JSONArray arr = backupRoot.optJSONArray(coll);
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            if (arr.optJSONObject(i) == null) {
                                errors.put(coll + "[" + i + "] linha nula ou corrompida");
                            }
                        }
                    }
                }
            }

            boolean isHealthy = errors.length() == 0;
            report.put("valid", isHealthy);
            report.put("healthy", isHealthy);
            report.put("errors", errors);
            report.put("warnings", warnings);
            report.put("stats", stats);
        } catch (Exception e) {
            try {
                errors.put("Falha inesperada durante validação: " + e.getMessage());
                report.put("valid", false);
                report.put("healthy", false);
                report.put("errors", errors);
                report.put("warnings", warnings);
                report.put("stats", stats);
            } catch (Exception ignored) {
            }
        }

        return report;
    }
}
