package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Representações estruturadas canônicas do Pathfinder 2e Remaster para a campanha
 * Kingmaker (Capítulos 1 a 12 - PZO2020/PZO2021).
 *
 * Contém catálogos de capítulos, missões encadeadas com objetivos e apostas,
 * pontos de interesse hexagonais (POIs), cenas táticas com zonas e obstáculos,
 * perfis de NPCs com disposições e facções, e estatísticas completas de perigos
 * canônicos (CDs de salvaguarda, CDs de desativação, furtividade e rotinas).
 *
 * Esta classe é pura e não depende do Android SDK, permitindo compilação
 * e execução rápida em testes JVM e no motor de regras em tempo de execução.
 */
public final class KingmakerCampaignModels {

    private KingmakerCampaignModels() {
        // Utilitário não instanciável
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

    // =========================================================================
    // 1. CHAPTER CATALOG
    // =========================================================================
    public static final class ChapterCatalog {
        public static final String CURRENT_SCHEMA = "braseiro-pathfinder2e-kingmaker-chapter-v1";

        public String schema = CURRENT_SCHEMA;
        public String chapterKey = "";
        public int chapterNumber = 1;
        public String title = "";
        public String locale = "pt-BR";
        public int levelMin = 1;
        public int levelMax = 20;
        public String summary = "";

        public final List<QuestModel> quests = new ArrayList<QuestModel>();
        public final List<CanonicalHazardModel> hazards = new ArrayList<CanonicalHazardModel>();
        public final List<HexPoiModel> pois = new ArrayList<HexPoiModel>();
        public final List<NpcModel> npcs = new ArrayList<NpcModel>();
        public final List<TacticalSceneModel> scenes = new ArrayList<TacticalSceneModel>();
        public final List<FactionModel> factions = new ArrayList<FactionModel>();
        public final List<WorldClockModel> clocks = new ArrayList<WorldClockModel>();
        public final List<WorldEventModel> events = new ArrayList<WorldEventModel>();
        public final List<CampaignMilestone> milestones = new ArrayList<CampaignMilestone>();
        public JSONObject metadata = new JSONObject();

        public ChapterCatalog() {
        }

        public static ChapterCatalog fromJson(JSONObject json) throws JSONException {
            if (json == null) {
                throw new IllegalArgumentException("JSON do catálogo não pode ser nulo");
            }
            ChapterCatalog cat = new ChapterCatalog();
            cat.schema = json.optString("schema", CURRENT_SCHEMA);
            cat.chapterKey = json.optString("chapter_key", json.optString("key", ""));
            cat.chapterNumber = json.optInt("chapter_number", json.optInt("number", 1));
            cat.title = json.optString("title", "");
            cat.locale = json.optString("locale", "pt-BR");
            cat.levelMin = json.optInt("level_min", 1);
            cat.levelMax = json.optInt("level_max", 20);
            cat.summary = json.optString("summary", "");
            cat.metadata = json.optJSONObject("metadata");
            if (cat.metadata == null) {
                cat.metadata = new JSONObject();
            }

            JSONArray questsArr = json.optJSONArray("quests");
            if (questsArr != null) {
                for (int i = 0; i < questsArr.length(); i++) {
                    JSONObject qObj = questsArr.optJSONObject(i);
                    if (qObj != null) {
                        cat.quests.add(QuestModel.fromJson(qObj));
                    }
                }
            }

            JSONArray hazardsArr = json.optJSONArray("hazards");
            if (hazardsArr != null) {
                for (int i = 0; i < hazardsArr.length(); i++) {
                    JSONObject hObj = hazardsArr.optJSONObject(i);
                    if (hObj != null) {
                        cat.hazards.add(CanonicalHazardModel.fromJson(hObj));
                    }
                }
            }

            JSONArray poisArr = json.optJSONArray("pois");
            if (poisArr != null) {
                for (int i = 0; i < poisArr.length(); i++) {
                    JSONObject pObj = poisArr.optJSONObject(i);
                    if (pObj != null) {
                        cat.pois.add(HexPoiModel.fromJson(pObj));
                    }
                }
            }

            JSONArray npcsArr = json.optJSONArray("npcs");
            if (npcsArr != null) {
                for (int i = 0; i < npcsArr.length(); i++) {
                    JSONObject nObj = npcsArr.optJSONObject(i);
                    if (nObj != null) {
                        cat.npcs.add(NpcModel.fromJson(nObj));
                    }
                }
            }

            JSONArray scenesArr = json.optJSONArray("scenes");
            if (scenesArr != null) {
                for (int i = 0; i < scenesArr.length(); i++) {
                    JSONObject sObj = scenesArr.optJSONObject(i);
                    if (sObj != null) {
                        cat.scenes.add(TacticalSceneModel.fromJson(sObj));
                    }
                }
            }

            JSONArray factionsArr = json.optJSONArray("factions");
            if (factionsArr != null) {
                for (int i = 0; i < factionsArr.length(); i++) {
                    JSONObject fObj = factionsArr.optJSONObject(i);
                    if (fObj != null) {
                        cat.factions.add(FactionModel.fromJson(fObj));
                    }
                }
            }

            JSONArray clocksArr = json.optJSONArray("clocks");
            if (clocksArr != null) {
                for (int i = 0; i < clocksArr.length(); i++) {
                    JSONObject cObj = clocksArr.optJSONObject(i);
                    if (cObj != null) {
                        cat.clocks.add(WorldClockModel.fromJson(cObj));
                    }
                }
            }

            JSONArray eventsArr = json.optJSONArray("events");
            if (eventsArr != null) {
                for (int i = 0; i < eventsArr.length(); i++) {
                    JSONObject eObj = eventsArr.optJSONObject(i);
                    if (eObj != null) {
                        cat.events.add(WorldEventModel.fromJson(eObj));
                    }
                }
            }

            JSONArray milestonesArr = json.optJSONArray("milestones");
            if (milestonesArr != null) {
                for (int i = 0; i < milestonesArr.length(); i++) {
                    JSONObject mObj = milestonesArr.optJSONObject(i);
                    if (mObj != null) {
                        cat.milestones.add(CampaignMilestone.fromJson(mObj));
                    }
                }
            }

            return cat;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("schema", schema);
            obj.put("chapter_key", chapterKey);
            obj.put("chapter_number", chapterNumber);
            obj.put("title", title);
            obj.put("locale", locale);
            obj.put("level_min", levelMin);
            obj.put("level_max", levelMax);
            obj.put("summary", summary);
            obj.put("metadata", metadata != null ? metadata : new JSONObject());

            JSONArray qArr = new JSONArray();
            for (QuestModel q : quests) {
                qArr.put(q.toJson());
            }
            obj.put("quests", qArr);

            JSONArray hArr = new JSONArray();
            for (CanonicalHazardModel h : hazards) {
                hArr.put(h.toJson());
            }
            obj.put("hazards", hArr);

            JSONArray pArr = new JSONArray();
            for (HexPoiModel p : pois) {
                pArr.put(p.toJson());
            }
            obj.put("pois", pArr);

            JSONArray nArr = new JSONArray();
            for (NpcModel n : npcs) {
                nArr.put(n.toJson());
            }
            obj.put("npcs", nArr);

            JSONArray sArr = new JSONArray();
            for (TacticalSceneModel s : scenes) {
                sArr.put(s.toJson());
            }
            obj.put("scenes", sArr);

            JSONArray fArr = new JSONArray();
            for (FactionModel f : factions) {
                fArr.put(f.toJson());
            }
            obj.put("factions", fArr);

            JSONArray cArr = new JSONArray();
            for (WorldClockModel c : clocks) {
                cArr.put(c.toJson());
            }
            obj.put("clocks", cArr);

            JSONArray eArr = new JSONArray();
            for (WorldEventModel e : events) {
                eArr.put(e.toJson());
            }
            obj.put("events", eArr);

            JSONArray mArr = new JSONArray();
            for (CampaignMilestone m : milestones) {
                mArr.put(m.toJson());
            }
            obj.put("milestones", mArr);

            return obj;
        }

        public void validate() throws IllegalArgumentException {
            if (chapterKey == null || chapterKey.trim().length() == 0) {
                throw new IllegalArgumentException("chapter_key ausente no catálogo");
            }
            if (title == null || title.trim().length() == 0) {
                throw new IllegalArgumentException("title ausente no catálogo");
            }
            if (chapterNumber < 0) throw new IllegalArgumentException("chapter_number inválido: " + chapterNumber);
            if (levelMin < 1 || levelMax > 20 || levelMin > levelMax) {
                throw new IllegalArgumentException("Faixa de níveis inválida: " + levelMin + " a " + levelMax);
            }
            for (CanonicalHazardModel h : hazards) {
                h.validate();
            }
            for (HexPoiModel p : pois) {
                p.validate();
            }
        }
    }

    // =========================================================================
    // 2. QUEST MODEL & OBJECTIVES
    // =========================================================================
    public static final class QuestModel {
        public String questKey = "";
        public String title = "";
        public String status = "unstarted"; // unstarted, active, in_progress, completed, failed
        public String stage = "";
        public String objective = "";
        public String stakes = "";
        public long ownerCharacterId = 0;
        public long factionId = 0;
        public int milestoneXp = 0;
        public final List<String> prerequisites = new ArrayList<String>();
        public final List<QuestObjective> objectives = new ArrayList<QuestObjective>();
        public JSONObject rewards = new JSONObject();
        public JSONObject progressJson = new JSONObject();

        public QuestModel() {
        }

        public static QuestModel fromJson(JSONObject json) throws JSONException {
            if (json == null) {
                return new QuestModel();
            }
            QuestModel q = new QuestModel();
            q.questKey = json.optString("quest_key", json.optString("key", ""));
            q.title = json.optString("title", "");
            if (q.questKey.length() == 0 && q.title.length() > 0) {
                q.questKey = "quest_" + slugKey(q.title);
            }
            q.status = json.optString("status", "unstarted");
            q.stage = json.optString("stage", "");
            q.objective = json.optString("objective", "");
            q.stakes = json.optString("stakes", "");
            q.ownerCharacterId = json.optLong("owner_character_id", json.optLong("ownerId", 0));
            q.factionId = json.optLong("faction_id", json.optLong("factionId", 0));
            q.milestoneXp = json.optInt("milestone_xp", json.optInt("xp", 0));

            JSONArray prereqArr = json.optJSONArray("prerequisites");
            if (prereqArr != null) {
                for (int i = 0; i < prereqArr.length(); i++) {
                    String p = prereqArr.optString(i, "").trim();
                    if (p.length() > 0) {
                        q.prerequisites.add(p);
                    }
                }
            }

            JSONArray objArr = json.optJSONArray("objectives");
            if (objArr != null) {
                for (int i = 0; i < objArr.length(); i++) {
                    JSONObject o = objArr.optJSONObject(i);
                    if (o != null) {
                        q.objectives.add(QuestObjective.fromJson(o));
                    }
                }
            }

            q.rewards = json.optJSONObject("rewards");
            if (q.rewards == null) {
                q.rewards = new JSONObject();
            }
            q.progressJson = json.optJSONObject("progress_json");
            if (q.progressJson == null) {
                q.progressJson = new JSONObject();
            }

            return q;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("quest_key", questKey);
            obj.put("title", title);
            obj.put("status", status);
            obj.put("stage", stage);
            obj.put("objective", objective);
            obj.put("stakes", stakes);
            obj.put("owner_character_id", ownerCharacterId);
            obj.put("faction_id", factionId);
            obj.put("milestone_xp", milestoneXp);

            JSONArray prereqs = new JSONArray();
            for (String p : prerequisites) {
                prereqs.put(p);
            }
            obj.put("prerequisites", prereqs);

            JSONArray objs = new JSONArray();
            for (QuestObjective o : objectives) {
                objs.put(o.toJson());
            }
            obj.put("objectives", objs);

            obj.put("rewards", rewards != null ? rewards : new JSONObject());
            obj.put("progress_json", progressJson != null ? progressJson : new JSONObject());
            return obj;
        }

        public void transitionTo(String nextState, String completedPrerequisite) {
            if ("completed".equals(this.status) && !"completed".equals(nextState)) {
                throw new IllegalStateException("Quest já concluída não pode mudar de estado");
            }
            if ("unstarted".equals(this.status) && !"active".equals(nextState)) {
                throw new IllegalStateException("Transição inválida de unstarted para " + nextState);
            }
            if (prerequisites.size() > 0 && "active".equals(nextState)) {
                if (completedPrerequisite == null || !prerequisites.contains(completedPrerequisite)) {
                    throw new IllegalStateException("Pré-requisito não satisfeito para ativar a quest: " + questKey);
                }
            }
            this.status = nextState;
        }
    }

    public static final class QuestObjective {
        public String id = "";
        public String desc = "";
        public String status = "pending"; // pending, completed, failed
        public boolean completed = false;

        public QuestObjective() {
        }

        public QuestObjective(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        public static QuestObjective fromJson(JSONObject json) throws JSONException {
            if (json == null) {
                return new QuestObjective();
            }
            QuestObjective o = new QuestObjective();
            o.id = json.optString("id", json.optString("key", ""));
            o.desc = json.optString("desc", json.optString("description", ""));
            o.status = json.optString("status", json.optBoolean("completed", false) ? "completed" : "pending");
            o.completed = "completed".equalsIgnoreCase(o.status) || json.optBoolean("completed", false);
            return o;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("desc", desc);
            obj.put("status", status);
            obj.put("completed", completed);
            return obj;
        }
    }

    // =========================================================================
    // 3. HEX POI MODEL
    // =========================================================================
    public static final class HexPoiModel {
        public String poiId = "";
        public String poiKey = "";
        public String campaignKey = "";
        public String mapId = "stolen_lands";
        public String name = "";
        public String nameKey = "";
        public String type = "point_of_interest";
        public String hexId = "";
        public String locationId = "";
        public int q = 0;
        public int r = 0;
        public String source = "campaign"; // CHECK(source IN ('campaign','procedural'))
        public String origin = "campaign_discovery";
        public int canonical = 0; // CHECK(canonical=0)
        public boolean hidden = false;
        public boolean discovered = true;
        public String createdBy = "campaign";
        public long createdTick = 0;
        public String status = "active"; // CHECK(status IN ('active','abandoned','ruined','destroyed','occupied','rebuilding'))
        public String description = "";
        public final List<String> tags = new ArrayList<String>();
        public String factionId = "";
        public String ownerNpcId = "";
        public String parentPoiId = "";
        public int developmentLevel = 1;
        public String icon = "📍";
        public String notes = "";
        public String privateNotes = "";
        public String settlementKey = "";
        public JSONObject settlementJson = new JSONObject();
        public JSONObject stateJson = new JSONObject();

        public HexPoiModel() {
        }

        public static HexPoiModel fromJson(JSONObject json) throws JSONException {
            if (json == null) {
                return new HexPoiModel();
            }
            HexPoiModel p = new HexPoiModel();
            p.poiKey = json.optString("poi_key", json.optString("key", ""));
            p.poiId = json.optString("poi_id", p.poiKey);
            p.campaignKey = json.optString("campaign_key", json.optString("chapter_key", ""));
            p.mapId = json.optString("map_id", "stolen_lands");
            p.name = json.optString("name", "");
            p.nameKey = json.optString("name_key", slugKey(p.name));
            p.type = json.optString("type", json.optString("poi_type", "point_of_interest"));
            p.hexId = json.optString("hex_id", "");
            p.locationId = json.optString("location_id", "");
            p.q = json.optInt("q", 0);
            p.r = json.optInt("r", 0);

            String src = json.optString("source", "campaign");
            p.source = "procedural".equalsIgnoreCase(src) ? "procedural" : "campaign";
            p.origin = json.optString("origin", "campaign_discovery");
            p.canonical = 0; // Invariante de integridade estrita do AppDb

            p.hidden = json.optBoolean("hidden", false);
            p.discovered = json.has("discovered") ? json.optBoolean("discovered") : true;
            p.createdBy = json.optString("created_by", "campaign");
            p.createdTick = json.optLong("created_tick", 0);

            String st = json.optString("status", "active");
            if ("unexplored".equalsIgnoreCase(st) || "hidden".equalsIgnoreCase(st)) {
                p.status = "active";
                p.discovered = false;
            } else if ("explored".equalsIgnoreCase(st) || "secured".equalsIgnoreCase(st)) {
                p.status = "active";
                p.discovered = true;
            } else {
                p.status = st;
            }

            p.description = json.optString("description", json.optString("notes", ""));
            JSONArray tagsArr = json.optJSONArray("tags");
            if (tagsArr != null) {
                for (int i = 0; i < tagsArr.length(); i++) {
                    p.tags.add(tagsArr.optString(i));
                }
            }

            p.factionId = json.optString("faction_id", "");
            p.ownerNpcId = json.optString("owner_npc_id", "");
            p.parentPoiId = json.optString("parent_poi_id", "");
            p.developmentLevel = json.optInt("development_level", 1);
            p.icon = json.optString("icon", "📍");
            p.notes = json.optString("notes", "");
            p.privateNotes = json.optString("private_notes", "");
            p.settlementKey = json.optString("settlement_key", "");
            p.settlementJson = json.optJSONObject("settlement_json");
            if (p.settlementJson == null) {
                p.settlementJson = new JSONObject();
            }
            p.stateJson = json.optJSONObject("state_json");
            if (p.stateJson == null) {
                p.stateJson = new JSONObject();
            }

            return p;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("poi_id", poiId);
            obj.put("poi_key", poiKey);
            obj.put("campaign_key", campaignKey);
            obj.put("map_id", mapId);
            obj.put("name", name);
            obj.put("name_key", nameKey);
            obj.put("type", type);
            obj.put("poi_type", type);
            obj.put("hex_id", hexId);
            obj.put("location_id", locationId);
            obj.put("q", q);
            obj.put("r", r);
            obj.put("source", source);
            obj.put("origin", origin);
            obj.put("canonical", canonical);
            obj.put("hidden", hidden);
            obj.put("discovered", discovered);
            obj.put("created_by", createdBy);
            obj.put("created_tick", createdTick);
            obj.put("status", status);
            obj.put("description", description);

            JSONArray tArr = new JSONArray();
            for (String t : tags) {
                tArr.put(t);
            }
            obj.put("tags", tArr);

            obj.put("faction_id", factionId);
            obj.put("owner_npc_id", ownerNpcId);
            obj.put("parent_poi_id", parentPoiId);
            obj.put("development_level", developmentLevel);
            obj.put("icon", icon);
            obj.put("notes", notes);
            obj.put("private_notes", privateNotes);
            obj.put("settlement_key", settlementKey);
            obj.put("settlement_json", settlementJson != null ? settlementJson : new JSONObject());
            obj.put("state_json", stateJson != null ? stateJson : new JSONObject());
            return obj;
        }

        public void validate() throws IllegalArgumentException {
            if (canonical != 0) {
                throw new IllegalArgumentException("canonical deve ser 0 para POIs mutáveis");
            }
            if (!"campaign".equals(source) && !"procedural".equals(source)) {
                throw new IllegalArgumentException("source inválido: " + source);
            }
            if (hexId == null || hexId.trim().length() == 0) {
                throw new IllegalArgumentException("hex_id não pode ser vazio para POI");
            }
            if (name == null || name.trim().length() == 0) {
                throw new IllegalArgumentException("name do POI não pode ser vazio");
            }
        }
    }

    // =========================================================================
    // 4. TACTICAL SCENE MODEL & SUB-ELEMENTS
    // =========================================================================
    public static final class TacticalSceneModel {
        public String sceneKey = "";
        public String title = "";
        public String location = "";
        public String hexId = "";
        public String environment = "generic";
        public int widthCells = 12;
        public int heightCells = 8;
        public long seed = 0;
        public final List<SceneZone> zones = new ArrayList<SceneZone>();
        public final List<SceneObstacle> obstacles = new ArrayList<SceneObstacle>();
        public JSONArray features = new JSONArray();
        public JSONArray cover = new JSONArray();
        public final List<CanonicalHazardModel> hazards = new ArrayList<CanonicalHazardModel>();
        public final List<SceneEntryPoint> entryPoints = new ArrayList<SceneEntryPoint>();
        public JSONArray combatants = new JSONArray();
        public JSONObject sourceContext = new JSONObject();
        public String status = "ready";

        public TacticalSceneModel() {
        }

        public static TacticalSceneModel fromJson(JSONObject json) throws JSONException {
            if (json == null) {
                return new TacticalSceneModel();
            }
            TacticalSceneModel s = new TacticalSceneModel();
            s.sceneKey = json.optString("scene_key", json.optString("key", ""));
            s.title = json.optString("title", json.optString("name", ""));
            s.location = json.optString("location", s.title);
            s.hexId = json.optString("hex_id", "");
            s.environment = json.optString("environment", "generic");
            s.widthCells = json.optInt("width_cells", json.optInt("width", 12));
            s.heightCells = json.optInt("height_cells", json.optInt("height", 8));
            s.seed = json.optLong("seed", 0);
            s.status = json.optString("status", "ready");

            JSONArray zArr = json.optJSONArray("zones");
            if (zArr != null) {
                for (int i = 0; i < zArr.length(); i++) {
                    JSONObject z = zArr.optJSONObject(i);
                    if (z != null) s.zones.add(SceneZone.fromJson(z));
                }
            }

            JSONArray oArr = json.optJSONArray("obstacles");
            if (oArr != null) {
                for (int i = 0; i < oArr.length(); i++) {
                    JSONObject o = oArr.optJSONObject(i);
                    if (o != null) s.obstacles.add(SceneObstacle.fromJson(o));
                }
            }

            s.features = json.optJSONArray("features");
            if (s.features == null) s.features = new JSONArray();

            s.cover = json.optJSONArray("cover");
            if (s.cover == null) s.cover = new JSONArray();

            JSONArray hArr = json.optJSONArray("hazards");
            if (hArr != null) {
                for (int i = 0; i < hArr.length(); i++) {
                    JSONObject h = hArr.optJSONObject(i);
                    if (h != null) s.hazards.add(CanonicalHazardModel.fromJson(h));
                }
            }

            JSONArray epArr = json.optJSONArray("entry_points");
            if (epArr != null) {
                for (int i = 0; i < epArr.length(); i++) {
                    JSONObject ep = epArr.optJSONObject(i);
                    if (ep != null) s.entryPoints.add(SceneEntryPoint.fromJson(ep));
                }
            }

            s.combatants = json.optJSONArray("combatants");
            if (s.combatants == null) s.combatants = json.optJSONArray("combatants_json");
            if (s.combatants == null) s.combatants = new JSONArray();

            s.sourceContext = json.optJSONObject("source_context");
            if (s.sourceContext == null) s.sourceContext = json.optJSONObject("source_context_json");
            if (s.sourceContext == null) s.sourceContext = new JSONObject();

            return s;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("scene_key", sceneKey);
            obj.put("title", title);
            obj.put("location", location);
            obj.put("hex_id", hexId);
            obj.put("environment", environment);
            obj.put("width_cells", widthCells);
            obj.put("height_cells", heightCells);
            obj.put("seed", seed);
            obj.put("status", status);

            JSONArray zArr = new JSONArray();
            for (SceneZone z : zones) zArr.put(z.toJson());
            obj.put("zones", zArr);

            JSONArray oArr = new JSONArray();
            for (SceneObstacle o : obstacles) oArr.put(o.toJson());
            obj.put("obstacles", oArr);

            obj.put("features", features != null ? features : new JSONArray());
            obj.put("cover", cover != null ? cover : new JSONArray());

            JSONArray hArr = new JSONArray();
            for (CanonicalHazardModel h : hazards) hArr.put(h.toJson());
            obj.put("hazards", hArr);

            JSONArray epArr = new JSONArray();
            for (SceneEntryPoint ep : entryPoints) epArr.put(ep.toJson());
            obj.put("entry_points", epArr);

            obj.put("combatants", combatants != null ? combatants : new JSONArray());
            obj.put("source_context", sourceContext != null ? sourceContext : new JSONObject());
            return obj;
        }
    }

    public static final class SceneZone {
        public String zoneKey = "";
        public String name = "";
        public int x = 0;
        public int y = 0;
        public int width = 1;
        public int height = 1;
        public String terrain = "normal";
        public String description = "";

        public SceneZone() {
        }

        public static SceneZone fromJson(JSONObject json) throws JSONException {
            if (json == null) return new SceneZone();
            SceneZone z = new SceneZone();
            z.zoneKey = json.optString("zone_key", json.optString("key", ""));
            z.name = json.optString("name", "");
            z.x = json.optInt("x", 0);
            z.y = json.optInt("y", 0);
            z.width = json.optInt("width", 1);
            z.height = json.optInt("height", 1);
            z.terrain = json.optString("terrain", "normal");
            z.description = json.optString("description", "");
            return z;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("zone_key", zoneKey);
            obj.put("name", name);
            obj.put("x", x);
            obj.put("y", y);
            obj.put("width", width);
            obj.put("height", height);
            obj.put("terrain", terrain);
            obj.put("description", description);
            return obj;
        }
    }

    public static final class SceneObstacle {
        public String obstacleKey = "";
        public int x = 0;
        public int y = 0;
        public int width = 1;
        public int height = 1;
        public String type = "rock";
        public boolean blocksMovement = true;
        public boolean blocksVision = false;
        public String cover = "standard";

        public SceneObstacle() {
        }

        public static SceneObstacle fromJson(JSONObject json) throws JSONException {
            if (json == null) return new SceneObstacle();
            SceneObstacle o = new SceneObstacle();
            o.obstacleKey = json.optString("obstacle_key", json.optString("key", ""));
            o.x = json.optInt("x", 0);
            o.y = json.optInt("y", 0);
            o.width = json.optInt("width", 1);
            o.height = json.optInt("height", 1);
            o.type = json.optString("type", "rock");
            o.blocksMovement = json.optBoolean("blocks_movement", true);
            o.blocksVision = json.optBoolean("blocks_vision", false);
            o.cover = json.optString("cover", "standard");
            return o;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("obstacle_key", obstacleKey);
            obj.put("x", x);
            obj.put("y", y);
            obj.put("width", width);
            obj.put("height", height);
            obj.put("type", type);
            obj.put("blocks_movement", blocksMovement);
            obj.put("blocks_vision", blocksVision);
            obj.put("cover", cover);
            return obj;
        }
    }

    public static final class SceneEntryPoint {
        public String pointKey = "";
        public int x = 0;
        public int y = 0;
        public String team = "player"; // player, enemy, neutral
        public String description = "";

        public SceneEntryPoint() {
        }

        public static SceneEntryPoint fromJson(JSONObject json) throws JSONException {
            if (json == null) return new SceneEntryPoint();
            SceneEntryPoint ep = new SceneEntryPoint();
            ep.pointKey = json.optString("point_key", json.optString("key", ""));
            ep.x = json.optInt("x", 0);
            ep.y = json.optInt("y", 0);
            ep.team = json.optString("team", "player");
            ep.description = json.optString("description", "");
            return ep;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("point_key", pointKey);
            obj.put("x", x);
            obj.put("y", y);
            obj.put("team", team);
            obj.put("description", description);
            return obj;
        }
    }

    // =========================================================================
    // 5. NPC MODEL & DIALOGUE
    // =========================================================================
    public static final class NpcModel {
        public static final String[] ATTITUDE_RANKS = {"hostil", "desfavoravel", "indiferente", "amistoso", "prestativo"};

        public String npcKey = "";
        public String stableId = "";
        public String name = "";
        public String summary = "";
        public String goals = "";
        public String secrets = "";
        public String attitude = "indiferente";
        public String relationship = "";
        public String knowledge = "";
        public String debts = "";
        public String lastSeen = "";
        public String hexId = "";
        public String faction = "";
        public String tokenUri = "";
        public String visualDescription = "";
        public boolean isCompanion = false;
        public final List<NpcDialogueNode> dialogueTree = new ArrayList<NpcDialogueNode>();
        public JSONObject stats = new JSONObject();
        public JSONObject cognitiveState = new JSONObject();
        public JSONObject voiceProfile = new JSONObject();

        public NpcModel() {
        }

        public static NpcModel fromJson(JSONObject json) throws JSONException {
            if (json == null) return new NpcModel();
            NpcModel n = new NpcModel();
            n.npcKey = json.optString("npc_key", json.optString("key", ""));
            n.name = json.optString("name", "");
            if (n.npcKey.length() == 0 && n.name.length() > 0) {
                n.npcKey = "npc_" + slugKey(n.name);
            }
            n.stableId = json.optString("stable_id", n.npcKey);
            n.summary = json.optString("summary", "");
            n.goals = json.optString("goals", "");
            n.secrets = json.optString("secrets", "");
            n.attitude = json.optString("attitude", "indiferente");
            n.relationship = json.optString("relationship", "");
            n.knowledge = json.optString("knowledge", "");
            n.debts = json.optString("debts", "");
            n.lastSeen = json.optString("last_seen", "");
            n.hexId = json.optString("hex_id", "");
            n.faction = json.optString("faction", "");
            n.tokenUri = json.optString("token_uri", "");
            n.visualDescription = json.optString("visual_description", "");
            n.isCompanion = json.optBoolean("is_companion", false);

            JSONArray diagArr = json.optJSONArray("dialogue_tree");
            if (diagArr != null) {
                for (int i = 0; i < diagArr.length(); i++) {
                    JSONObject d = diagArr.optJSONObject(i);
                    if (d != null) n.dialogueTree.add(NpcDialogueNode.fromJson(d));
                }
            }

            n.stats = json.optJSONObject("stats");
            if (n.stats == null) n.stats = json.optJSONObject("stats_json");
            if (n.stats == null) n.stats = new JSONObject();

            n.cognitiveState = json.optJSONObject("cognitive_state");
            if (n.cognitiveState == null) n.cognitiveState = new JSONObject();

            n.voiceProfile = json.optJSONObject("voice_profile");
            if (n.voiceProfile == null) n.voiceProfile = new JSONObject();

            return n;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("npc_key", npcKey);
            obj.put("stable_id", stableId);
            obj.put("name", name);
            obj.put("summary", summary);
            obj.put("goals", goals);
            obj.put("secrets", secrets);
            obj.put("attitude", attitude);
            obj.put("relationship", relationship);
            obj.put("knowledge", knowledge);
            obj.put("debts", debts);
            obj.put("last_seen", lastSeen);
            obj.put("hex_id", hexId);
            obj.put("faction", faction);
            obj.put("token_uri", tokenUri);
            obj.put("visual_description", visualDescription);
            obj.put("is_companion", isCompanion);

            JSONArray dArr = new JSONArray();
            for (NpcDialogueNode d : dialogueTree) dArr.put(d.toJson());
            obj.put("dialogue_tree", dArr);

            obj.put("stats", stats != null ? stats : new JSONObject());
            obj.put("cognitive_state", cognitiveState != null ? cognitiveState : new JSONObject());
            obj.put("voice_profile", voiceProfile != null ? voiceProfile : new JSONObject());
            return obj;
        }

        public void advanceAttitude(int delta) {
            int currentRank = 2; // indiferente
            for (int i = 0; i < ATTITUDE_RANKS.length; i++) {
                if (ATTITUDE_RANKS[i].equalsIgnoreCase(attitude)) {
                    currentRank = i;
                    break;
                }
            }
            int next = Math.max(0, Math.min(ATTITUDE_RANKS.length - 1, currentRank + delta));
            this.attitude = ATTITUDE_RANKS[next];
        }
    }

    public static final class NpcDialogueNode {
        public String attitudeReq = "indiferente";
        public String text = "";
        public String topic = "";
        public String questKey = "";

        public NpcDialogueNode() {
        }

        public static NpcDialogueNode fromJson(JSONObject json) throws JSONException {
            if (json == null) return new NpcDialogueNode();
            NpcDialogueNode d = new NpcDialogueNode();
            d.attitudeReq = json.optString("attitude_req", json.optString("attitude", "indiferente"));
            d.text = json.optString("text", "");
            d.topic = json.optString("topic", "");
            d.questKey = json.optString("quest_key", "");
            return d;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("attitude_req", attitudeReq);
            obj.put("text", text);
            obj.put("topic", topic);
            obj.put("quest_key", questKey);
            return obj;
        }
    }

    // =========================================================================
    // 6. CANONICAL HAZARD REMASTER STAT BLOCK
    // =========================================================================
    public static final class CanonicalHazardModel {
        public String hazardKey = "";
        public String name = "";
        public int level = 1;
        public final List<String> traits = new ArrayList<String>();
        public int stealthDc = 15;
        public int perceptionDc = 15;
        public String savingThrowType = "Reflexos"; // Reflexos, Fortitude, Vontade
        public int savingThrowDc = 15;
        public final List<HazardDisableOption> disableSkills = new ArrayList<HazardDisableOption>();
        public int routineActions = 1;
        public String hazardType = "armadilha"; // armadilha, ambiental, assombracao
        public String complexity = "simples"; // simples, complexo
        public int ac = 15;
        public int hardness = 0;
        public int hp = 10;
        public int bt = 5;
        public String reaction = "";
        public final List<HazardRoutineAction> routine = new ArrayList<HazardRoutineAction>();
        public String reset = "";
        public int xp = 30;
        public String description = "";

        public CanonicalHazardModel() {
        }

        public static CanonicalHazardModel fromJson(JSONObject json) throws JSONException {
            if (json == null) return new CanonicalHazardModel();
            CanonicalHazardModel h = new CanonicalHazardModel();
            h.hazardKey = json.optString("hazard_key", json.optString("key", ""));
            h.name = json.optString("name", "");
            if (h.hazardKey.length() == 0 && h.name.length() > 0) {
                h.hazardKey = "hazard_" + slugKey(h.name);
            }
            h.level = json.optInt("level", 1);

            JSONArray tArr = json.optJSONArray("traits");
            if (tArr != null) {
                for (int i = 0; i < tArr.length(); i++) {
                    h.traits.add(tArr.optString(i));
                }
            }

            h.stealthDc = json.optInt("stealth_dc", json.optInt("stealthDc", 0));
            h.perceptionDc = json.optInt("perception_dc", json.optInt("perceptionDc", h.stealthDc));
            h.savingThrowType = json.optString("saving_throw_type", json.optString("savingThrowType", "Reflexos"));
            h.savingThrowDc = json.optInt("saving_throw_dc", json.optInt("savingThrowDc", 0));

            JSONArray dArr = json.optJSONArray("disable_skills");
            if (dArr == null) dArr = json.optJSONArray("disableSkills");
            if (dArr != null) {
                for (int i = 0; i < dArr.length(); i++) {
                    JSONObject dObj = dArr.optJSONObject(i);
                    if (dObj != null) h.disableSkills.add(HazardDisableOption.fromJson(dObj));
                }
            } else if (json.has("disable_dc") || json.has("disableDc")) {
                int dc = json.optInt("disable_dc", json.optInt("disableDc", 15));
                h.disableSkills.add(new HazardDisableOption("Ladroagem", dc, "treinado"));
            }

            h.routineActions = json.optInt("routine_actions", json.optInt("routineActions", 1));
            h.hazardType = json.optString("hazard_type", "armadilha");
            h.complexity = json.optString("complexity", h.traits.contains("complexo") ? "complexo" : "simples");
            h.ac = json.optInt("ac", 15);
            h.hardness = json.optInt("hardness", 0);
            h.hp = json.optInt("hp", 10);
            h.bt = json.optInt("bt", h.hp / 2);
            h.reaction = json.optString("reaction", "");
            h.reset = json.optString("reset", "");
            h.xp = json.optInt("xp", 30);
            h.description = json.optString("description", "");

            JSONArray rArr = json.optJSONArray("routine");
            if (rArr != null) {
                for (int i = 0; i < rArr.length(); i++) {
                    JSONObject rObj = rArr.optJSONObject(i);
                    if (rObj != null) h.routine.add(HazardRoutineAction.fromJson(rObj));
                }
            }

            return h;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("hazard_key", hazardKey);
            obj.put("name", name);
            obj.put("level", level);

            JSONArray tArr = new JSONArray();
            for (String t : traits) tArr.put(t);
            obj.put("traits", tArr);

            obj.put("stealth_dc", stealthDc);
            obj.put("perception_dc", perceptionDc);
            obj.put("saving_throw_type", savingThrowType);
            obj.put("saving_throw_dc", savingThrowDc);

            JSONArray dArr = new JSONArray();
            for (HazardDisableOption d : disableSkills) dArr.put(d.toJson());
            obj.put("disable_skills", dArr);

            obj.put("routine_actions", routineActions);
            obj.put("hazard_type", hazardType);
            obj.put("complexity", complexity);
            obj.put("ac", ac);
            obj.put("hardness", hardness);
            obj.put("hp", hp);
            obj.put("bt", bt);
            obj.put("reaction", reaction);
            obj.put("reset", reset);
            obj.put("xp", xp);
            obj.put("description", description);

            JSONArray rArr = new JSONArray();
            for (HazardRoutineAction r : routine) rArr.put(r.toJson());
            obj.put("routine", rArr);

            return obj;
        }

        public void validate() throws IllegalArgumentException {
            if (level < 0 || level > 25) {
                throw new IllegalArgumentException("Nível do perigo fora da faixa PF2e: " + level);
            }
            if (stealthDc <= 0) {
                throw new IllegalArgumentException("CD de Furtividade do perigo deve ser positiva: " + stealthDc);
            }
            if (savingThrowDc <= 0) {
                throw new IllegalArgumentException("CD de Salvaguarda deve ser positiva: " + savingThrowDc);
            }
            if (disableSkills.isEmpty()) {
                throw new IllegalArgumentException("Perigo deve ter métodos de desativação definidos: " + hazardKey);
            }
        }
    }

    public static final class HazardDisableOption {
        public String skill = "Ladroagem";
        public int dc = 15;
        public String proficiency = "treinado"; // treinado, especialista, mestre, lendario
        public String description = "";

        public HazardDisableOption() {
        }

        public HazardDisableOption(String skill, int dc, String proficiency) {
            this.skill = skill;
            this.dc = dc;
            this.proficiency = proficiency;
        }

        public static HazardDisableOption fromJson(JSONObject json) throws JSONException {
            if (json == null) return new HazardDisableOption();
            HazardDisableOption d = new HazardDisableOption();
            d.skill = json.optString("skill", "Ladroagem");
            d.dc = json.optInt("dc", 15);
            d.proficiency = json.optString("proficiency", "treinado");
            d.description = json.optString("description", "");
            return d;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("skill", skill);
            obj.put("dc", dc);
            obj.put("proficiency", proficiency);
            obj.put("description", description);
            return obj;
        }
    }

    public static final class HazardRoutineAction {
        public int actionCost = 1;
        public String name = "";
        public String description = "";

        public HazardRoutineAction() {
        }

        public static HazardRoutineAction fromJson(JSONObject json) throws JSONException {
            if (json == null) return new HazardRoutineAction();
            HazardRoutineAction r = new HazardRoutineAction();
            r.actionCost = json.optInt("action_cost", json.optInt("actionCost", 1));
            r.name = json.optString("name", "");
            r.description = json.optString("description", "");
            return r;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("action_cost", actionCost);
            obj.put("name", name);
            obj.put("description", description);
            return obj;
        }
    }

    // =========================================================================
    // 7. FACTION, CLOCKS & EVENTS
    // =========================================================================
    public static final class FactionModel {
        public String name = "";
        public String agenda = "";
        public String resources = "";
        public int relationship = 0;
        public String currentPlan = "";
        public String location = "";
        public boolean active = true;

        public FactionModel() {
        }

        public static FactionModel fromJson(JSONObject json) throws JSONException {
            if (json == null) return new FactionModel();
            FactionModel f = new FactionModel();
            f.name = json.optString("name", "");
            f.agenda = json.optString("agenda", "");
            f.resources = json.optString("resources", "");
            f.relationship = json.optInt("relationship", 0);
            f.currentPlan = json.optString("current_plan", "");
            f.location = json.optString("location", "");
            f.active = json.optBoolean("active", true);
            return f;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("agenda", agenda);
            obj.put("resources", resources);
            obj.put("relationship", relationship);
            obj.put("current_plan", currentPlan);
            obj.put("location", location);
            obj.put("active", active);
            return obj;
        }
    }

    public static final class WorldClockModel {
        public String name = "";
        public int currentValue = 0;
        public int maxValue = 4;
        public String consequence = "";
        public boolean automatic = false;
        public boolean active = true;

        public WorldClockModel() {
        }

        public static WorldClockModel fromJson(JSONObject json) throws JSONException {
            if (json == null) return new WorldClockModel();
            WorldClockModel c = new WorldClockModel();
            c.name = json.optString("name", "");
            c.currentValue = json.optInt("current_value", 0);
            c.maxValue = json.optInt("max_value", 4);
            c.consequence = json.optString("consequence", "");
            c.automatic = json.optBoolean("automatic", false);
            c.active = json.optBoolean("active", true);
            return c;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("current_value", currentValue);
            obj.put("max_value", maxValue);
            obj.put("consequence", consequence);
            obj.put("automatic", automatic);
            obj.put("active", active);
            return obj;
        }
    }

    public static final class WorldEventModel {
        public String title = "";
        public String description = "";
        public long dueAt = 0;
        public int dueDay = 0;
        public String status = "scheduled"; // scheduled, resolved, cancelled
        public JSONObject consequenceJson = new JSONObject();

        public WorldEventModel() {
        }

        public static WorldEventModel fromJson(JSONObject json) throws JSONException {
            if (json == null) return new WorldEventModel();
            WorldEventModel e = new WorldEventModel();
            e.title = json.optString("title", "");
            e.description = json.optString("description", "");
            e.dueAt = json.optLong("due_at", 0);
            e.dueDay = json.optInt("due_day", 0);
            e.status = json.optString("status", "scheduled");
            e.consequenceJson = json.optJSONObject("consequence_json");
            if (e.consequenceJson == null) e.consequenceJson = new JSONObject();
            return e;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("description", description);
            obj.put("due_at", dueAt);
            obj.put("due_day", dueDay);
            obj.put("status", status);
            obj.put("consequence_json", consequenceJson != null ? consequenceJson : new JSONObject());
            return obj;
        }
    }

    public static final class CampaignMilestone {
        public String milestoneKey = "";
        public String name = "";
        public int xp = 0;
        public String sourceRef = "";

        public CampaignMilestone() {
        }

        public static CampaignMilestone fromJson(JSONObject json) throws JSONException {
            if (json == null) return new CampaignMilestone();
            CampaignMilestone m = new CampaignMilestone();
            m.milestoneKey = json.optString("milestone_key", json.optString("key", ""));
            m.name = json.optString("name", "");
            m.xp = json.optInt("xp", 0);
            m.sourceRef = json.optString("source_ref", "");
            return m;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("milestone_key", milestoneKey);
            obj.put("name", name);
            obj.put("xp", xp);
            obj.put("source_ref", sourceRef);
            return obj;
        }
    }
}
