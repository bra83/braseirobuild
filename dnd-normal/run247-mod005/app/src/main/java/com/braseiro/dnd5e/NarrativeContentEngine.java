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

    public synchronized boolean saveEntity(final Entity entity) {
        if (entity == null || entity.id.length() == 0 || appDb == null) return false;
        try {
            appDb.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override
                public void run() throws Exception {
                    SQLiteDatabase db = appDb.getWritableDatabase();
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
                    cv.put("visibility", entity.visibility);
                    cv.put("discovered", entity.discovered ? 1 : 0);
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
        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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
        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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

        // 7. Arcana Unleashed (AU)
        if (saveEntity(new Entity(
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

        // 8. Monster Manual 2025 (XMM) & Sage Advice 2025 (XSAC)
        if (saveEntity(new Entity(
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

        if (saveEntity(new Entity(
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
