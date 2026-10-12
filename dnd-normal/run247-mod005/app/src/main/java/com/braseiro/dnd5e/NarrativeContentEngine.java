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
                        // PRESERVAÇÃO DE ESTADO VIVO: descoberta do jogador NUNCA é revertida por seed!
                        if (existing.discovered) {
                            discovered = true;
                            visibility = "grupo";
                        } else if (!"secreto".equals(existing.visibility) && !"mestre".equals(existing.visibility)) {
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
        int count = 0;

        // 1. Forgotten Realms: Heroes of Faerun (FRHoF) & Adventures in Faerun (FRAiF)
        Map<String, String> harperRel = new HashMap<>();
        harperRel.put("rival", "faerun_zhentarim");
        harperRel.put("ally", "faerun_elminster");
        if (seedCanonicalEntity(new Entity(
                "faerun_harpers", "FRHoF", "forgotten_realms", "present", "faction",
                "Os Harpistas", "The Harpers",
                "Orquestra de agentes secretos que lutam pelo equilíbrio e contra a tirania em Faerûn.",
                java.util.Arrays.asList(
                        "Fundados por Elminster e líderes das raças livres para defender os fracos.",
                        "Operam através de células independentes e usam o símbolo da harpa e da lua de prata."
                ),
                java.util.Arrays.asList(
                        "Possuem esconderijos mágicos camuflados sob tabernas ao longo da Costa da Espada.",
                        "Monitoram em segredo o renascimento de cultos antigos nos picos de Netheril."
                ),
                java.util.Arrays.asList("faerun", "harpers", "good", "secret_agents"),
                harperRel, "grupo", true
        ))) count++;

        Map<String, String> zhentRel = new HashMap<>();
        zhentRel.put("rival", "faerun_harpers");
        if (seedCanonicalEntity(new Entity(
                "faerun_zhentarim", "FRHoF", "forgotten_realms", "present", "faction",
                "A Rede Negra (Zhentarim)", "The Zhentarim",
                "Sindicato mercantil e militar que busca monopólio econômico e controle político.",
                java.util.Arrays.asList(
                        "Oferecem caravanas fortificadas, empréstimos e proteção profissional.",
                        "Sua ala pública atua como guilda de mercenários respeitada nas cidades da Costa da Espada."
                ),
                java.util.Arrays.asList(
                        "O Círculo Interno conspira para subornar magistrados em Baldur's Gate e Waterdeep.",
                        "Mantêm armazéns subterrâneos com pólvora de fumaça e relíquias de Netheril."
                ),
                java.util.Arrays.asList("faerun", "zhentarim", "mercenaries", "shadow_trade"),
                zhentRel, "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "faerun_elminster", "FRAiF", "forgotten_realms", "present", "npc",
                "Elminster de Shadowdale", "Elminster of Shadowdale",
                "O lendário Escolhido de Mystra e conselheiro dos povos livres de Toril.",
                java.util.Arrays.asList(
                        "Vive em uma torre pacífica no Vale das Sombras e fuma seu cachimbo místico.",
                        "Auxilia heróis quando a estabilidade da Trama (Weave) é ameaçada."
                ),
                java.util.Arrays.asList(
                        "Guarda o feitiço primordial de reparação do Weave em caso de catástrofe planar.",
                        "Conhece as localizações secretas dos últimos nós de Mythallar em Anauroch."
                ),
                java.util.Arrays.asList("faerun", "chosen", "archmage", "shadowdale"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "faerun_undermountain_depths", "FRAiF", "forgotten_realms", "present", "location",
                "Profundezas de Undermountain", "Undermountain Depths",
                "O colossal calabouço subterrâneo sob a montanha de Waterdeep.",
                java.util.Arrays.asList(
                        "Criado pelo mago louco Halaster Blackcloak através de milênios de escavações.",
                        "Conecta-se a Skullport e ao Subterrâneo através de portais dimensionais instáveis."
                ),
                java.util.Arrays.asList(
                        "Halaster plantou runas de drenagem que absorvem magias de alto nível nas câmaras baixas.",
                        "Um portal direto para as ruínas de Netheril repousa selado no vigésimo terceiro nível."
                ),
                java.util.Arrays.asList("faerun", "waterdeep", "dungeon", "halaster"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // 2. Netheril's Fall (NF)
        if (seedCanonicalEntity(new Entity(
                "netheril_xinok_enclave", "NF", "forgotten_realms", "ancient_netheril", "location",
                "Enclave de Xinok", "Xinok Enclave",
                "Cidade flutuante do apogeu do Império de Netheril erguida por um Mythallar primordial.",
                java.util.Arrays.asList(
                        "Xinok era habitada por arquimagos e seus servos arcanos antes da Queda.",
                        "Seus destroços repousam sob as dunas áridas do Grande Deserto de Anauroch."
                ),
                java.util.Arrays.asList(
                        "O Mythallar de Xinok permanece parcialmente energizado e gera campos de gravidade anômala.",
                        "Contém o manuscrito proibido com a fórmula de Karsus para ascensão divina."
                ),
                java.util.Arrays.asList("netheril", "mythallar", "ancient_magic", "ruins"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // 3. Eberron: Forge of the Artificer (EFA)
        Map<String, String> cannithRel = new HashMap<>();
        cannithRel.put("enemy", "eberron_lord_of_blades");
        if (seedCanonicalEntity(new Entity(
                "eberron_house_cannith", "EFA", "eberron", "998 YK", "faction",
                "Casa Cannith", "House Cannith",
                "A Casa Marcada portadora da Marca da Criação e artífices supremos de Khorvaire.",
                java.util.Arrays.asList(
                        "Criadores dos Warforged e mestres de forjas elementais e itens mágicos.",
                        "Fraturada em três facções rivais após a destruição da sede em Cyre no Dia do Luto."
                ),
                java.util.Arrays.asList(
                        "Mantêm um protótipo operacional de Forja de Criação oculto nas bordas de Breland.",
                        "Baron Merrix pesquisa almas artificiais além dos limites do Tratado do Trono."
                ),
                java.util.Arrays.asList("eberron", "dragonmarked", "cannith", "artificer"),
                cannithRel, "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "eberron_lord_of_blades", "EFA", "eberron", "998 YK", "npc",
                "O Senhor das Lâminas", "The Lord of Blades",
                "Líder messiânico dos forjados de guerra rebeldes entrincheirado na Terra Lamentada.",
                java.util.Arrays.asList(
                        "Prega a supremacia dos Warforged e a rejeição das raças de carne.",
                        "Comanda patrulhas nas brumas cinzentas de Cyre recuperando relíquias de guerra."
                ),
                java.util.Arrays.asList(
                        "Possui esquemas para transformar o colosso de guerra em uma fortaleza ambulante.",
                        "Foi forjado como guarda-costas de um dos maiores patriarcas da Casa Cannith."
                ),
                java.util.Arrays.asList("eberron", "warforged", "mournland", "leader"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "eberron_eston_creation_forge", "EFA", "eberron", "998 YK", "location",
                "Forjas da Criação de Eston", "Eston Creation Forge",
                "Complexo soterrado na Terra Lamentada onde foram forjados os primeiros Warforged colossais.",
                java.util.Arrays.asList(
                        "Eston era o coração industrial de Cyre antes da catástrofe que arrasou a nação.",
                        "As linhas de trem elemental convergem para seus terminais despedaçados."
                ),
                java.util.Arrays.asList(
                        "Uma matriz mestra pulsante continua viva nos níveis inferiores protegida por sentinelas de ferro.",
                        "Patrulhas do Lord of Blades vigiam as fendas para impedir saqueadores de Sharn."
                ),
                java.util.Arrays.asList("eberron", "mournland", "creation_forge", "cannith"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // 4. Ravenloft: The Horrors Within (RHW)
        Map<String, String> strahdRel = new HashMap<>();
        strahdRel.put("enemy", "ravenloft_van_richten");
        strahdRel.put("enemy", "ravenloft_keepers_feather");
        if (seedCanonicalEntity(new Entity(
                "ravenloft_strahd", "RHW", "ravenloft", "735 BC", "npc",
                "Conde Strahd von Zarovich", "Count Strahd von Zarovich",
                "O primeiro Darklord e mestre tirânico do domínio de Baróvia.",
                java.util.Arrays.asList(
                        "Um vampiro antigo, guerreiro brilhante e arquimago que governa das torres de Ravenloft.",
                        "As Brumas obedecem ao seu chamado para fechar as fronteiras do domínio."
                ),
                java.util.Arrays.asList(
                        "O Coração do Pavor nas torres do castelo absorve ferimentos graves antes de atingi-lo.",
                        "O sangue de Tatyana renasce ciclicamente em Baróvia e é a obsessão eterna do Darklord."
                ),
                java.util.Arrays.asList("ravenloft", "vampire", "darklord", "barovia"),
                strahdRel, "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "ravenloft_castle_ravenloft", "RHW", "ravenloft", "735 BC", "location",
                "Castelo Ravenloft", "Castle Ravenloft",
                "Fortaleza monumental cravada sobre o precipício de mil pés acima da vila de Baróvia.",
                java.util.Arrays.asList(
                        "Construído pelo arquiteto Khazan em honra à mãe de Strahd, Rainha Ravenovia.",
                        "Suas masmorras e criptas abrigam túmulos dos generais e antepassados da família von Zarovich."
                ),
                java.util.Arrays.asList(
                        "A cripta de Sergei von Zarovich guarda a luz que pode quebrar a névoa do castelo.",
                        "Um elevador mecânico e armadilhas de teletransporte conduzem intrusos ao calabouço das almas."
                ),
                java.util.Arrays.asList("ravenloft", "castle", "barovia", "dungeon"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "ravenloft_keepers_feather", "RHW", "ravenloft", "735 BC", "faction",
                "Os Guardiões da Pena", "Keepers of the Feather",
                "Sociedade secreta de wereravens leais que desafiam o governo de Strahd nas sombras.",
                java.util.Arrays.asList(
                        "Operam a partir da Vinícola Magia do Mago e de tabernas em Vallaki e Baróvia.",
                        "Avisam forasteiros bem-intencionados sobre emboscadas e perigos letais das Brumas."
                ),
                java.util.Arrays.asList(
                        "Guardam uma das três gemas mágicas que fertilizam a terra de Baróvia.",
                        "Conhecem passagens secretas nas colinas que contornam as patrulhas dos lobos de Strahd."
                ),
                java.util.Arrays.asList("ravenloft", "wereraven", "resistance", "barovia"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        // 5. Astarion's Book of Hungers (ABH)
        if (seedCanonicalEntity(new Entity(
                "astarion_cazador_palace", "ABH", "ravenloft", "present", "location",
                "Palácio Szarr", "Szarr Palace",
                "Mansão aristocrática nas sombras com vastos complexos rituais subterrâneos.",
                java.util.Arrays.asList(
                        "Residência de nobres vampiros onde festas pródigas ocultam rituais de sangue.",
                        "Câmaras revestidas de runas ancestrais protegem os caixões dos mestres."
                ),
                java.util.Arrays.asList(
                        "Sete mil almas de proles vampíricas foram marcadas para o Ritual de Ascensão Profana.",
                        "O sarcófago oculto sob o poço contém o sangue ancestral que sacia a fome por eras."
                ),
                java.util.Arrays.asList("astarion", "vampire", "szarr", "hunger"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // 6. Lorwyn: First Light (LFL)
        if (seedCanonicalEntity(new Entity(
                "lorwyn_aeghith_complex", "LFL", "lorwyn", "dawn_era", "location",
                "Complexo Comunal de Aeghith", "Aeghith Community Complex",
                "O grande centro cívico e de encontros das espécies feéricas de Lorwyn.",
                java.util.Arrays.asList(
                        "Sede de celebrações artísticas, exposições de inventores e banquetes diplomáticos.",
                        "Possui salas de acolhimento, oficinas de artífices e corredores abertos para os bosques."
                ),
                java.util.Arrays.asList(
                        "Os porões do Aeghith contêm relíquias do ciclo anterior que reagem à aproximação da Grande Aurora.",
                        "A sabotagem das máquinas voadoras foi financiada por infiltrados com Relicários do Crepúsculo."
                ),
                java.util.Arrays.asList("lorwyn", "aeghith", "fey", "exhibition"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "lorwyn_aurora_nexus", "LFL", "lorwyn", "dawn_era", "location",
                "Nexus da Grande Aurora", "Great Aurora Nexus",
                "O vórtice dimensional onde o dia perpétuo de Lorwyn colide com a noite de Shadowmoor.",
                java.util.Arrays.asList(
                        "A Grande Aurora banha o plano em luz mágica eterna onde a decadência não se manifesta.",
                        "Os Relicários da Aurora concedem imunidade às perturbações do Crepúsculo."
                ),
                java.util.Arrays.asList(
                        "Quando a Aurora vacila, as criaturas sofrem transmutação forçada de memória e personalidade.",
                        "A Rainha Oona manipula os fios dos sonhos nas bordas do vórtice planar."
                ),
                java.util.Arrays.asList("lorwyn", "shadowmoor", "aurora", "nexus"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // === EXPANSÃO APROFUNDADA DOS 10 LIVROS (HISTÓRIA, GEOGRAFIA, POVOS, FACÇÕES, NPCS, SEGREDOS) ===

        // FRHoF & FRAiF (Faerûn Avançado)
        if (seedCanonicalEntity(new Entity(
                "faerun_baldurs_gate", "FRHoF", "forgotten_realms", "present", "location",
                "Portal de Baldur (Baldur's Gate)", "Baldur's Gate",
                "A colossal metrópole costeira dividida entre a Cidade Alta fortificada, a Cidade Baixa mercantil e os Bairros Exteriores.",
                java.util.Arrays.asList(
                        "Governada pelos Quatro Duques e patrulhada com punho de ferro pelo Punho Flamejante (Flaming Fist).",
                        "Porto comercial vital no Rio Chionthar que atrai refugiados, mercenários e espiões de todo o continente."
                ),
                java.util.Arrays.asList(
                        "Células do Culto dos Três Mortos (Bhaal, Bane, Myrkul) operam em catacumbas secretas sob os esgotos da Cidade Baixa.",
                        "O Patriarcado financia expedições secretas para desenterrar segredos netherenses nas colinas ao norte."
                ),
                java.util.Arrays.asList("faerun", "city", "baldurs_gate", "sword_coast"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "faerun_flaming_fist", "FRHoF", "forgotten_realms", "present", "faction",
                "O Punho Flamejante", "The Flaming Fist",
                "A mais poderosa e disciplinada companhia mercenária de Faerûn, atuando como polícia de Baldur's Gate.",
                java.util.Arrays.asList(
                        "Fundada pelo Grão-Duque Eltan, opera com postos avançados em Chult e ao longo da Costa da Espada.",
                        "Mantêm o Forte da Rocha (Seatower of Balduran) como quartel-general impenetrável."
                ),
                java.util.Arrays.asList(
                        "Oficiais corruptos recebem subornos da Guilda dos Ladrões para desviar patrulhas noturnas.",
                        "Possuem acordos secretos com os Harpistas para caçar cultistas infernais em troca de inteligência mágica."
                ),
                java.util.Arrays.asList("faerun", "mercenaries", "flaming_fist", "baldurs_gate"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "faerun_silver_hand", "FRAiF", "forgotten_realms", "present", "npc",
                "Laeral Silverhand", "Laeral Silverhand",
                "Lorde Aberta de Waterdeep, uma das Sete Irmãs e Escolhida de Mystra.",
                java.util.Arrays.asList(
                        "Lidera a Aliança dos Lordes com sabedoria diplomática e prodigioso poder de arquimaga.",
                        "Dedica sua vida à preservação da paz entre as cidades-estado da Costa da Espada."
                ),
                java.util.Arrays.asList(
                        "Possui um medalhão de proteção que a liga diretamente às defesas mágicas primordiais do Palácio de Waterdeep.",
                        "Mantém vigilância secreta sobre portais dimensionais adormecidos no Monte Waterdeep."
                ),
                java.util.Arrays.asList("faerun", "waterdeep", "chosen", "open_lord"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        // NF (Netheril's Fall - História, Arquimagos e Locais)
        if (seedCanonicalEntity(new Entity(
                "netheril_karsus_sanctum", "NF", "forgotten_realms", "ancient_netheril", "location",
                "Santuário da Loucura de Karsus", "Karsus's Sanctum of Folly",
                "A câmara arcana primordial onde o arquimago Karsus teceu o feitiço de nível 12 que rompeu o Weave.",
                java.util.Arrays.asList(
                        "A ruína está suspensa em gravidade nula no coração de Anauroch, banhada em petrificação arcana permanente.",
                        "Karsus tentou roubar a divindade de Mystryl, causando a queda simultânea de todos os enclaves flutuantes."
                ),
                java.util.Arrays.asList(
                        "Ecos do avatar petrificado de Karsus ainda emitem sussurros que corrompem a mente de conjuradores desavisados.",
                        "Um fragmento intacto do Mythallar de Karsus continua girando no epicentro da ruptura temporal."
                ),
                java.util.Arrays.asList("netheril", "karsus", "fall", "anauroch", "mythallar"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "netheril_larloch", "NF", "forgotten_realms", "ancient_netheril", "npc",
                "Larloch, o Rei das Sombras", "Larloch the Shadow King",
                "O último arquimago sobrevivente de Netheril, um lich imensamente poderoso que reina na Cripta dos Feiticeiros.",
                java.util.Arrays.asList(
                        "Governava uma cidade flutuante netherense antes do colapso e acumulou milênios de feitiços arcanos proibidos.",
                        "Seu covil em Warlock's Crypt é cercado por defesas mortais e dezenas de liches servos."
                ),
                java.util.Arrays.asList(
                        "Larloch absorve a energia vital de deuses mortos para preparar a reconstrução de seu enclave nos céus de Toril.",
                        "Conhece as chaves de comando dos Mythallars remanescentes espalhados pelo deserto."
                ),
                java.util.Arrays.asList("netheril", "lich", "archmage", "larloch"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // EFA (Eberron: Forge of the Artificer - Facções e Cidades)
        if (seedCanonicalEntity(new Entity(
                "eberron_sharn_city", "EFA", "eberron", "998 YK", "location",
                "Sharn, a Cidade das Torres", "Sharn, City of Towers",
                "A mais populosa e vertical metrópole de Khorvaire, construída sobre uma zona de manifestação de Syrania.",
                java.util.Arrays.asList(
                        "Torres com milhares de metros de altura ligadas por pontes suspensas onde dirigíveis e mensageiros circulam.",
                        "Dividida em platôs sociais: a Cidade Alta aristocrática, Cidade Média comercial e Cidade Baixa perigosa."
                ),
                java.util.Arrays.asList(
                        "Nas profundezas (The Cogs), forjas industriais ilegais operam sob proteção de cultistas do Dragão das Profundezas.",
                        "O sindicato criminoso Boromar Clan controla a rota de contrabando de sonhogrão e relíquias de Xen'drik."
                ),
                java.util.Arrays.asList("eberron", "sharn", "towers", "khorvaire"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "eberron_mournland_fog", "EFA", "eberron", "998 YK", "hazard",
                "Névoa Cinzenta da Terra Lamentada", "Mournland Dead-Gray Fog",
                "A misteriosa e letal barreira de brumas que envolve os restos despedaçados da nação destruída de Cyre.",
                java.util.Arrays.asList(
                        "Em 20 de Olarune de 994 YK, um cataclismo de origem desconhecida aniquilou Cyre em um único dia.",
                        "Dentro das brumas, ferimentos não cicatrizam naturalmente e magias sofrem mutações imprevisíveis."
                ),
                java.util.Arrays.asList(
                        "Cadáveres de soldados que tombaram no Dia do Luto permanecem intactos sem se decompor há quatro anos.",
                        "Uma tempestade de magia viva percorre as planícies gerando feitiços sencientes com vontade própria."
                ),
                java.util.Arrays.asList("eberron", "mournland", "cyre", "mourning"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        // RHW & ABH (Ravenloft & Astarion - Domínios, Darklords e Fome)
        if (seedCanonicalEntity(new Entity(
                "ravenloft_barovia_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Baróvia", "Domain of Barovia",
                "Um vale lúgubre cercado por montanhas escarpadas e envolto em névoas eternas sob o jugo de Strahd.",
                java.util.Arrays.asList(
                        "Seus habitantes vivem sob medo perpétuo; muitos nascem desprovidos de alma, meras cascas melancólicas.",
                        "Pontos notáveis incluem a Vila de Baróvia, a cidade murada de Vallaki e a colina do Templo Âmbar."
                ),
                java.util.Arrays.asList(
                        "As Brumas devoram qualquer um que tente cruzar a fronteira sem a permissão explícita de Strahd.",
                        "O Templo Âmbar nas montanhas congeladas guarda sarcófagos com os Poderes Sombrios aprisionados."
                ),
                java.util.Arrays.asList("ravenloft", "barovia", "domain", "darklord"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "ravenloft_van_richten", "RHW", "ravenloft", "735 BC", "npc",
                "Dr. Rudolph van Richten", "Dr. Rudolph van Richten",
                "O lendário caçador de monstros e estudioso dos horrores sobrenaturais do Semiprimordial do Pavor.",
                java.util.Arrays.asList(
                        "Autor de guias famosos sobre vampiros, lobisomens e mortos-vivos.",
                        "Viaja disfarçado reunindo aliados e preparando o plano final para destruir Strahd von Zarovich."
                ),
                java.util.Arrays.asList(
                        "Carrega uma maldição trágica: todos os companheiros que se afeiçoam a ele encontram fins terríveis nas mãos de monstros.",
                        "Possui anotações criptografadas com fraquezas biológicas e místicas específicas de cada Darklord."
                ),
                java.util.Arrays.asList("ravenloft", "hunter", "van_richten", "scholar"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "astarion_vampiric_hungers", "ABH", "ravenloft", "present", "misc",
                "As Sete Fomes Vampíricas", "The Seven Vampiric Hungers",
                "A doutrina ancestral sobre os apetites sombrios que dominam as linhagens de vampiros puros e dhampirs.",
                java.util.Arrays.asList(
                        "A fome não é meramente física por sangue, mas um impulso místico por vitalidade, memórias e emoções da presa.",
                        "Dhampirs resistem ao chamado absoluto, mas manifestam compulsões por drenagem psíquica ou medular."
                ),
                java.util.Arrays.asList(
                        "O frenesi de sangue suprime toda razão moral quando o hospedeiro cai abaixo de metade de seus pontos de vida.",
                        "Astarion descobriu que sangue de criaturas feéricas de Lorwyn confere imunidade temporária ao frenesi faminto."
                ),
                java.util.Arrays.asList("astarion", "vampire", "hunger", "dhampir"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // LFL (Lorwyn: First Light - Sociedade Feérica, Clãs e Aurora)
        if (seedCanonicalEntity(new Entity(
                "lorwyn_boggart_warrens", "LFL", "lorwyn", "dawn_era", "faction",
                "Goblins Boggarts e suas Tias", "Boggarts and Aunties",
                "O povo goblin de Lorwyn, motivado pela busca voraz por novas sensações, fofocas e prazeres sensoriais.",
                java.util.Arrays.asList(
                        "Vivem em tocas labirínticas sob as colinas e são liderados por matriarcas respeitadas chamadas Tias (Aunties).",
                        "Não conhecem o ódio nem a malícia premeditada; tudo é uma brincadeira feérica ou coleta de lembranças."
                ),
                java.util.Arrays.asList(
                        "Possuem esconderijos secretos com tortas de sonhos roubadas das cozinhas de Aeghith.",
                        "Quando o Crepúsculo se aproxima, transformam-se em boggarts cruéis e vorazes de Shadowmoor."
                ),
                java.util.Arrays.asList("lorwyn", "boggart", "fey", "clans"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "lorwyn_flamekin_hearth", "LFL", "lorwyn", "dawn_era", "faction",
                "Os Flamekin e a Chama Interior", "The Flamekin and the Inner Flame",
                "Seres elementais de fogo vivo e pedra que buscam iluminação espiritual e comunhão ardente.",
                java.util.Arrays.asList(
                        "Possuem corpos incandescentes envoltos em armaduras de obsidiana e viajam em peregrinações contemplativas.",
                        "Sua magia conecta a mente ao plano elemental térmico sem cinzas nem fumaça tóxica."
                ),
                java.util.Arrays.asList(
                        "A Chama Interior pode se extinguir se um Flamekin for exposto a desespero extremo gerado por relíquias de Shadowmoor.",
                        "Guardiões da tocha que acende as forjas do Complexo de Aeghith durante as exposições de inventores."
                ),
                java.util.Arrays.asList("lorwyn", "flamekin", "elemental", "fey"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        // AU (Arcana Unleashed - Feitiços Arcanos e Itens Épicos)
        if (seedCanonicalEntity(new Entity(
                "arcana_unleashed_weavers", "AU", "forgotten_realms", "present", "faction",
                "Círculo dos Tecelões Arcanos", "Arcane Weavers Circle",
                "Conselho independente de pesquisadores arcanos dedicados à maestria sobre os fios da magia.",
                java.util.Arrays.asList(
                        "Desenvolvem técnicas avançadas para infiltradores arcanos e conjuradores de linha de frente.",
                        "Catalogam itens que evoluem conforme o portador realiza feitos épicos."
                ),
                java.util.Arrays.asList(
                        "Descobriram como ancorar magias de concentração em pequenos constructos cristalinos.",
                        "Guardam o tratado sobre a fusão de magia elemental com feywild arcana."
                ),
                java.util.Arrays.asList("arcana_unleashed", "magic", "wizards", "circle"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "arcana_unleashed_vault", "AU", "forgotten_realms", "present", "location",
                "Abóbada dos Itens Evolutivos", "Vault of Evolving Relics",
                "Câmara protegida por runas primordiais onde armas e vestimentas arcanas se aprimoram com o destino do herói.",
                java.util.Arrays.asList(
                        "Itens evolutivos possuem tiers dormentes, despertos e exaltados, ativados por marcos heróicos e rituais.",
                        "Construídos segundo os tratados arcanos compilados pelos mestres do Círculo dos Tecelões."
                ),
                java.util.Arrays.asList(
                        "As armas forjadas com lágrimas do Weave quebram defesas mágicas de criaturas abissais com golpes críticos.",
                        "O tomo de catalisação elemental repousa selado sob custódia de um golem sentinela de mithral."
                ),
                java.util.Arrays.asList("arcana_unleashed", "magic_items", "evolving", "vault"),
                Collections.<String, String>emptyMap(), "secreto", false
        ))) count++;

        // Monster Manual 2025 (XMM) & Sage Advice 2025 (XSAC)
        if (seedCanonicalEntity(new Entity(
                "xmm_dragon_ecology_2025", "XMM", "multiverse", "present", "misc",
                "Ecologia Dracônica dos 503 Monstros", "2025 Draconic Ecology",
                "Padrões de comportamento, covis e ecologia dos predadores lendários do Multiverso.",
                java.util.Arrays.asList(
                        "Os dragões cromáticos e metálicos possuem ações lendárias reequilibradas e covis dinâmicos.",
                        "O Bestiário de 2025 define imunidades e resistências canônicas sem aproximações arbitrárias."
                ),
                java.util.Arrays.asList(
                        "Dragões anciões sentem a ruptura de nós de Weave ou Mythallars em um raio de 50 milhas.",
                        "O sopro de um dragão vermelho de 2025 destrói barreiras mágicas de nível inferior."
                ),
                java.util.Arrays.asList("xmm", "dragons", "bestiary", "legendary"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        if (seedCanonicalEntity(new Entity(
                "xsac_adjudication_rules", "XSAC", "multiverse", "present", "misc",
                "Compêndio de Adjudicação Canônica 2025", "Sage Advice Compendium 2025",
                "Compilação autoritativa das decisões e interpretações oficiais dos designers do D&D.",
                java.util.Arrays.asList(
                        "163 decisões canônicas sobre ataques desarmados, descansos, sintonização e concentração.",
                        "Proíbe rolagens automáticas e preserva o consentimento explícito do jogador."
                ),
                java.util.Arrays.asList(
                        "A adjudicação do Mestre nunca substitui regras primárias quando a regra do livro for expressa.",
                        "Efeitos de medo e charme exigem salvamentos explícitos em cada rodada."
                ),
                java.util.Arrays.asList("xsac", "rulings", "sage_advice", "mechanics"),
                Collections.<String, String>emptyMap(), "grupo", true
        ))) count++;

        return count;
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
