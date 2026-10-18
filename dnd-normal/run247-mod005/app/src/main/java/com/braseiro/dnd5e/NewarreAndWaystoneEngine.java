package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motor oficial de Newarre e da Pousada Marco do Percurso (Waystone Inn).
 *
 * Integra a pequena aldeia de Newarre ao Vale do Dessarin (hexágono DO40 de Faerûn),
 * com a hospedaria de Kote e Bast, estabelecimentos navegáveis, moradores com cognição
 * persistente, economia rural 5e e integração com o Mestre IA.
 */
public final class NewarreAndWaystoneEngine {

    public static final String HEX_ID = "DO40";
    public static final String ROAD_REFERENCE_HEX_ID = "DQ36";
    public static final String REGION_NAME = "Vale do Dessarin";
    public static final String SETTLEMENT_NAME = "Newarre";
    public static final String INN_NAME = "Pousada Marco do Percurso";

    public static final String POI_NEWARRE_ID = "poi-newarre-settlement-do40";
    public static final String POI_WAYSTONE_INN_ID = "poi-waystone-inn-do40";

    // Sublocais internos da Pousada e da Aldeia
    public static final String POI_HALL_ID = "poi-waystone-main-hall-do40";
    public static final String POI_BAR_ID = "poi-waystone-bar-service-do40";
    public static final String POI_KITCHEN_ID = "poi-waystone-kitchen-pantry-do40";
    public static final String POI_ROOMS_ID = "poi-waystone-guest-rooms-do40";
    public static final String POI_KOTE_ROOMS_ID = "poi-waystone-kote-rooms-do40";
    public static final String POI_BAST_ROOMS_ID = "poi-waystone-bast-rooms-do40";
    public static final String POI_STABLES_ID = "poi-waystone-stables-exterior-do40";
    public static final String POI_BLACKSMITH_ID = "poi-newarre-blacksmith-do40";
    public static final String POI_MILL_ID = "poi-newarre-mill-warehouse-do40";
    public static final String POI_SQUARE_ID = "poi-newarre-town-square-do40";
    public static final String POI_FARMS_ID = "poi-newarre-farms-do40";
    public static final String POI_WOODS_ID = "poi-newarre-woods-trails-do40";

    private NewarreAndWaystoneEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "newarre_waystone_inn");
            o.put("name", "Newarre e Pousada Marco do Percurso");
            o.put("hex_id", HEX_ID);
            o.put("road_reference", ROAD_REFERENCE_HEX_ID);
            o.put("region", REGION_NAME);
            o.put("type", "aldeia_rural");
            o.put("innkeeper", "Kote");
            o.put("assistant", "Bast");
            o.put("status", "ativo_persistente");
        } catch (Exception ignored) {}
        return o;
    }

    /**
     * Instala e sincroniza idempotentemente Newarre, a Pousada, seus sublocais e os NPCs.
     * Preserva dados existentes no banco sem sobrescrever nem duplicar.
     */
    public static synchronized void install(AppDb db) {
        if (db == null) return;
        try {
            ensureNpcs(db);
            ensurePoisAndSettlement(db);
        } catch (Exception e) {
            AppLog.e("NewarreEngine", "Falha na sincronização de Newarre: " + e.getMessage(), e);
        }
    }

    /**
     * Garante a persistência de Kote, Bast e os moradores de Newarre.
     */
    public static void ensureNpcs(AppDb db) throws Exception {
        // 1. KOTE
        JSONObject existingKote = db.findNpcMemoryByName("Kote");
        if (existingKote.length() == 0) {
            JSONObject koteStats = new JSONObject()
                    .put("name", "Kote")
                    .put("system", "D&D 5e")
                    .put("race", "Humano")
                    .put("type", "Humanoide (humano)")
                    .put("role", "Hospedeiro e NPC social")
                    .put("challenge_rating", "0 (aparente)")
                    .put("ac", 11)
                    .put("armor_class", 11)
                    .put("hp", 14)
                    .put("max_hp", 14)
                    .put("hit_points", 14)
                    .put("speed_ft", 30)
                    .put("status", "vivo")
                    .put("abilities", new JSONObject().put("STR", 10).put("DEX", 12).put("CON", 12).put("INT", 18).put("WIS", 15).put("CHA", 17))
                    .put("skills", new JSONObject().put("Arcana", 6).put("History", 6).put("Insight", 4).put("Perception", 4).put("Performance", 7).put("Persuasion", 5))
                    .put("languages", new JSONArray().put("Comum").put("Dialetos regionais de Faerûn"))
                    .put("actions", new JSONArray().put(new JSONObject().put("name", "Objeto improvisado").put("attack_bonus", 0).put("damage", "1d4 contundente")))
                    .put("traits", new JSONArray()
                            .put("Hospitaleiro atento: observa mais do que revela.")
                            .put("Erudição incomum: reconhece histórias, símbolos e princípios arcanos.")
                            .put("Identidade oculta: não confirma ser Kvothe.")
                            .put("Poderes incertos: não usar magia ou feitos lendários automaticamente."))
                    .put("notes", "Adaptação para D&D 5e. O passado lendário não concede poderes atuais sem decisão explícita do Mestre.");

            JSONObject kote = new JSONObject()
                    .put("name", "Kote")
                    .put("summary", "Proprietário da Pousada Marco do Percurso em Newarre. Jovem, ruivo de olhos verdes, cortês, espirituoso e reservado.")
                    .put("visual_description", "Cabelos ruivos como chama apagando, olhos verdes atentos, avental de linho limpo sobre túnica simples, panos de prato no ombro. Busto de hospedeiro rural tranquilo.")
                    .put("goals", "Manter a Pousada Marco do Percurso em funcionamento tranquilo, preservando seu anonimato e protegendo Newarre de atenções perigosas.")
                    .put("secrets", "SEGREDO EXCLUSIVO DO MESTRE: Kote é na verdade Kvothe, o lendário arcanista, bardo e matador do rei. Seus poderes estão adormecidos ou intencionalmente trancados. Guarda no andar superior a espada Folly e um baú pesado de três fechos.")
                    .put("attitude", "hospitaleira, calma e perspicaz")
                    .put("relationship", "Cordial com viajantes respeitosos; atencioso com moradores locais.")
                    .put("knowledge", "Conhece profundamente baladas antigas, história arcana, nomes verdadeiros, folclore, culinária e destilação de sidras e cervejas.")
                    .put("debts", "Nenhuma pública; dívidas espirituais do passado seladas.")
                    .put("last_seen", "Pousada Marco do Percurso, Newarre")
                    .put("hex_id", HEX_ID)
                    .put("voice_profile", "Voz serena, cadenciada e suave, de tom melodioso e ligeiramente melancólico.")
                    .put("stats_json", koteStats.toString())
                    .put("active", 1);
            db.upsertNpcMemory(kote);

            JSONObject koteCognitive = new JSONObject()
                    .put("name", "Kote")
                    .put("emotion", "sereno")
                    .put("trust", 45)
                    .put("fear", 10)
                    .put("suspicion", 60)
                    .put("urgency", 10)
                    .put("next_action", "Polir o balcão da pousada e conferir os barris de cerveja na despensa.")
                    .put("schedule_json", new JSONArray()
                            .put("Alvorada: acender a lareira e preparar o pão matinal")
                            .put("Manhã: atender moradores locais e viajantes matutinos")
                            .put("Tarde: supervisionar mantimentos e orientar Bast")
                            .put("Noite: servir cerveja e ensopado à beira da lareira")
                            .put("Tarde da noite: silêncio de três partes e vigília solitária"))
                    .put("beliefs_json", new JSONArray()
                            .put("Um homem que espera morrer deve fazê-lo sem alarde.")
                            .put("A paciência e um bom ensopado acalmam qualquer tempestade."))
                    .put("faction", "Pousada Marco do Percurso")
                    .put("current_location", "Pousada Marco do Percurso, Newarre");
            db.upsertNpcCognitiveState(koteCognitive);
        } else {
            // Garante hex_id e localização corretos sem apagar dados do usuário
            ContentValues cv = new ContentValues();
            cv.put("hex_id", HEX_ID);
            cv.put("last_seen", "Pousada Marco do Percurso, Newarre");
            db.getWritableDatabase().update("npc_memory_profiles", cv, "id=? AND world_profile=?",
                    new String[]{String.valueOf(existingKote.optLong("id")), db.getActiveMapProfile()});
        }

        // 2. BAST
        JSONObject existingBast = db.findNpcMemoryByName("Bast");
        if (existingBast.length() == 0) {
            JSONObject bastStats = new JSONObject()
                    .put("name", "Bast")
                    .put("system", "D&D 5e")
                    .put("race", "Feérico")
                    .put("type", "Feérico (fauno / fey nobre disfarçado)")
                    .put("role", "Assistente de hospedaria e pupilo")
                    .put("challenge_rating", "2 (aparente plebeu)")
                    .put("ac", 13)
                    .put("hp", 27)
                    .put("speed_ft", 35)
                    .put("status", "vivo")
                    .put("abilities", new JSONObject().put("STR", 12).put("DEX", 16).put("CON", 14).put("INT", 13).put("WIS", 12).put("CHA", 18))
                    .put("skills", new JSONObject().put("Acrobatics", 5).put("Deception", 6).put("Persuasion", 6).put("Stealth", 5))
                    .put("traits", new JSONArray()
                            .put("Graça feérica: passos leves e charme natural.")
                            .put("Disfarce cuidadoso: oculta pequenos traços não-humanos dos moradores."))
                    .put("actions", new JSONArray().put(new JSONObject().put("name", "Adaga rápida").put("attack_bonus", 5).put("damage", "1d4+3 perfurante")));

            JSONObject bast = new JSONObject()
                    .put("name", "Bast")
                    .put("summary", "Assistente, aprendiz e companheiro constante de Kote na Marco do Percurso. Jovem esguio, gracioso e alegre.")
                    .put("visual_description", "Jovem de cabelos escuros e olhar travesso com centelha feérica, botas pesadas para disfarçar o caminhar, sorriso fácil e movimentos incrivelmente fluidos.")
                    .put("goals", "Ver seu mestre Reshi (Kote) despertar de sua melancolia e voltar a ser a lenda que sempre foi.")
                    .put("secrets", "SEGREDO EXCLUSIVO DO MESTRE: Bast é um ser nobre do Crepúsculo Feérico (Feywild), profundamente apegado a Kote. Não revela sua herança feérica a nenhum aldeão.")
                    .put("attitude", "espirituosa, zombeteira e ferozmente protetora de Kote")
                    .put("relationship", "Encantador com hóspedes gentis, porém impiedoso com qualquer um que ameace a paz de seu mestre.")
                    .put("knowledge", "Tradições do mundo feérico, encantos de ilusão, ervas silvestres e boatos que circulam pelas estradas.")
                    .put("debts", "Lealdade absoluta a Kote.")
                    .put("last_seen", "Pousada Marco do Percurso, Newarre")
                    .put("hex_id", HEX_ID)
                    .put("voice_profile", "Voz jovem, melodiosa, com um tom irônico e vivaz.")
                    .put("stats_json", bastStats.toString())
                    .put("active", 1);
            db.upsertNpcMemory(bast);

            JSONObject bastCognitive = new JSONObject()
                    .put("name", "Bast")
                    .put("emotion", "inquieto e esperançoso")
                    .put("trust", 40)
                    .put("fear", 5)
                    .put("suspicion", 70)
                    .put("urgency", 30)
                    .put("next_action", "Observar os viajantes que chegam à pousada em busca de histórias que animem Kote.")
                    .put("schedule_json", new JSONArray()
                            .put("Manhã: colher ervas e cuidar dos cavalos na cocheira")
                            .put("Tarde: estudos e tarefas com Kote")
                            .put("Noite: circular entre as mesas servindo canecas e ouvindo fofocas")
                            .put("Madrugada: passeios solitários pelos bosques de Newarre"))
                    .put("faction", "Pousada Marco do Percurso")
                    .put("current_location", "Pousada Marco do Percurso, Newarre");
            db.upsertNpcCognitiveState(bastCognitive);
        }

        // 3. OSWIN TALM (Ferreiro)
        if (db.findNpcMemoryByName("Oswin Talm").length() == 0) {
            JSONObject oswin = new JSONObject()
                    .put("name", "Oswin Talm")
                    .put("summary", "Ferreiro veterano de Newarre. Homem robusto, de mãos calejadas, mente prática e bom humor seco.")
                    .put("visual_description", "Braços fortes manchados de fuligem, avental de couro grosso, barba grisalha e olhar direto e acolhedor.")
                    .put("goals", "Manter os arados e ferraduras da aldeia em dia e garantir a segurança das estradas locais.")
                    .put("secrets", "Preocupa-se com o sumiço esporádico de pequenas ferramentas e boatos de feras nas colinas a leste.")
                    .put("attitude", "pragmática, cordial e trabalhadora")
                    .put("relationship", "Frequenta a Marco do Percurso todas as noites após a labuta da forja.")
                    .put("knowledge", "Metalurgia básica, conserto de armas e ferramentas, estado das estradas e pontes.")
                    .put("last_seen", "Ferraria de Newarre")
                    .put("hex_id", HEX_ID)
                    .put("stats_json", new JSONObject().put("ac", 12).put("hp", 18).put("role", "Ferreiro").toString())
                    .put("active", 1);
            db.upsertNpcMemory(oswin);
        }

        // 4. MIRA VENN (Moleira)
        if (db.findNpcMemoryByName("Mira Venn").length() == 0) {
            JSONObject mira = new JSONObject()
                    .put("name", "Mira Venn")
                    .put("summary", "Moleira e guardiã do armazém comunitário de Newarre. Mulher decidida, atenta a cada saca de trigo e milho.")
                    .put("visual_description", "Mulher de meia-idade, avental enfarinhado, cabelos castanhos presos firmemente, olhar perspicaz.")
                    .put("goals", "Garantir que a produção agrícola de Newarre seja moída e estocada a tempo para o inverno.")
                    .put("secrets", "Nota pequenas discrepâncias nas rotinas da aldeia e sabe quais famílias estão passando aperto financeiro.")
                    .put("attitude", "firme, honesta e justa")
                    .put("relationship", "Negocia grãos e farinha regularmente com Kote para os pães da pousada.")
                    .put("knowledge", "Preços de grãos, previsões do tempo baseadas no vento e história das famílias locais.")
                    .put("last_seen", "Moinho de Vento de Newarre")
                    .put("hex_id", HEX_ID)
                    .put("stats_json", new JSONObject().put("ac", 10).put("hp", 11).put("role", "Moleira").toString())
                    .put("active", 1);
            db.upsertNpcMemory(mira);
        }

        // 5. EDDA FEN (Parteira e Herborista)
        if (db.findNpcMemoryByName("Edda Fen").length() == 0) {
            JSONObject edda = new JSONObject()
                    .put("name", "Edda Fen")
                    .put("summary", "Conhecedora de ervas, parteira e conselheira respeitada da aldeia. Conhece cada nascimento ocorrido em Newarre.")
                    .put("visual_description", "Senhora idosa de xale de lã verde-escuro, olhos castanhos carinhosos, bolsa de couro com raízes e unguentos.")
                    .put("goals", "Cuidar da saúde dos moradores e manter tradições pacíficas de convivência comunitária.")
                    .put("secrets", "Sabe que Bast não é um rapaz comum, mas mantém silêncio porque vê o bem que ele faz a Kote.")
                    .put("attitude", "maternal, discreta e paciente")
                    .put("relationship", "Muito querida por todos; Kote reserva para ela as melhores infusões de maçã desidratada.")
                    .put("knowledge", "Remédios caseiros, cuidados com febre e ferimentos, lendas antigas do Vale do Dessarin.")
                    .put("last_seen", "Casa de Ervas, Newarre")
                    .put("hex_id", HEX_ID)
                    .put("stats_json", new JSONObject().put("ac", 10).put("hp", 9).put("role", "Herborista e Parteira").toString())
                    .put("active", 1);
            db.upsertNpcMemory(edda);
        }

        // 6. TEREN VALE (Mercador Ambulante)
        if (db.findNpcMemoryByName("Teren Vale").length() == 0) {
            JSONObject teren = new JSONObject()
                    .put("name", "Teren Vale")
                    .put("summary", "Pequeno mercador de carroça que conecta Newarre aos entroncamentos da estrada em DQ36 e assentamentos vizinhos.")
                    .put("visual_description", "Homem magro, capote de viagem com remendos limpos, chapéu de abas largas, botas enlameadas da estrada.")
                    .put("goals", "Fazer viagens comerciais seguras, vender tecidos, sal, pregos e ferramentas, trazendo notícias frescas.")
                    .put("secrets", "Tem medo crescente de bandidos perto do entroncamento norte e procura escolta discreta.")
                    .put("attitude", "faladora, curiosa e negociadora")
                    .put("relationship", "Traz sal e especiarias para Kote em troca de pernoite e refeições quentes.")
                    .put("knowledge", "Rumores de estradas, preços regionais de suprimentos e notícias do noroeste de Faerûn.")
                    .put("last_seen", "Praça Comunitária / Estrada de Newarre")
                    .put("hex_id", HEX_ID)
                    .put("stats_json", new JSONObject().put("ac", 11).put("hp", 12).put("role", "Mercador Itinerante").toString())
                    .put("active", 1);
            db.upsertNpcMemory(teren);
        }

        // 7. LINNA ARVEL (Jovem Trabalhadora Rural)
        if (db.findNpcMemoryByName("Linna Arvel").length() == 0) {
            JSONObject linna = new JSONObject()
                    .put("name", "Linna Arvel")
                    .put("summary", "Jovem filha de lavradores locais. Curiosa, sonhadora e dividida entre a rotina do campo e o fascínio pelas terras distantes.")
                    .put("visual_description", "Jovem de cabelos trançados, camisa de linho rústica, botas de trabalho, olhar ávido por qualquer história de viajantes.")
                    .put("goals", "Aprender sobre o mundo além de Newarre sem desamparar sua família durante a colheita.")
                    .put("secrets", "Sonha secretamente em aprender esgrima ou arquearia para se defender nas estradas.")
                    .put("attitude", "tímida no início, mas cheia de perguntas vivazes")
                    .put("relationship", "Adora ouvir as histórias de Teren e as charadas de Bast quando visita a pousada.")
                    .put("knowledge", "Topografia dos bosques locais, trilhas de animais e ciclo das lavouras de Newarre.")
                    .put("last_seen", "Fazendas de Newarre")
                    .put("hex_id", HEX_ID)
                    .put("stats_json", new JSONObject().put("ac", 11).put("hp", 10).put("role", "Trabalhadora Rural").toString())
                    .put("active", 1);
            db.upsertNpcMemory(linna);
        }
    }

    /**
     * Garante o cadastro dos POIs e do assentamento no banco de dados.
     */
    public static void ensurePoisAndSettlement(AppDb db) throws Exception {
        String profile = db.getActiveMapProfile();
        JSONObject state = db.getMapStateForProfile(profile);
        String logicalMapId = state.optString("logical_map_id", "").trim();
        if (logicalMapId.isEmpty()) {
            logicalMapId = "faerun-atlas-2025-ptbr-21600x15390-v2-max";
        }
        String mapPackageId = state.optString("map_package_id", "");
        String migration = (mapPackageId.length() > 0 && db.isValidMapHex(mapPackageId, HEX_ID)) ? "ready" : "migration_pending";

        // 1. POI Principal da Aldeia (Newarre)
        JSONObject existingVillage = db.getCampaignPoi(POI_NEWARRE_ID);
        if (existingVillage.length() == 0) {
            JSONObject spec = new JSONObject()
                    .put("id", POI_NEWARRE_ID)
                    .put("world_profile", profile)
                    .put("logical_map_id", logicalMapId)
                    .put("map_package_id", mapPackageId)
                    .put("hex_id", HEX_ID)
                    .put("name", SETTLEMENT_NAME)
                    .put("type", "povoado")
                    .put("description", "Pequena aldeia rural esquecida no Vale do Dessarin, com 20 a 30 propriedades rurais e aproximadamente 80 a 120 habitantes. Conhecida pela tranquilidade, bosques suaves e pela acolhedora Pousada Marco do Percurso.")
                    .put("origin", "campaign")
                    .put("creator", "campaign")
                    .put("hidden", false)
                    .put("discovered", true)
                    .put("status", "active")
                    .put("migration_status", migration)
                    .put("entity_kind", "settlement")
                    .put("entity_id", POI_NEWARRE_ID);
            db.createCampaignPoi(spec);
        }

        // 2. Registro do Assentamento (campaign_settlements)
        JSONObject existingSettlement = db.getSettlementForPoi(POI_NEWARRE_ID);
        if (existingSettlement.length() == 0) {
            JSONObject market = new JSONObject()
                    .put("inn_rooms", new JSONObject().put("modest_per_night", "5 pp").put("comfortable_per_night", "5 po"))
                    .put("food_drinks", new JSONObject().put("hearty_stew", "3 pc").put("fresh_bread_cheese", "2 pc").put("house_ale_mug", "4 pc").put("blackberry_wine_bottle", "2 pp"))
                    .put("supplies", new JSONObject().put("rations_1day", "5 pp").put("torch", "1 pc").put("rope_50ft", "1 po").put("smith_repair_simple", "2 pp"))
                    .put("stable_care", new JSONObject().put("horse_per_night", "5 pp"));

            JSONObject defense = new JSONObject()
                    .put("walls", "Nenhuma; cercas de madeira e valas rurais")
                    .put("militia", "Aproximadamente 12 aldeões com ferramentas e arcos de caça em emergência extrema")
                    .put("watch", "Vigia noturno informal revezado entre as propriedades");

            JSONObject settlementData = new JSONObject()
                    .put("population", 95)
                    .put("population_band", "aldeia_pequena")
                    .put("government", "Conselho informal de anciãos e líderes de ofício")
                    .put("economy", "Agricultura familiar, criação de ovelhas, pequenos ofícios e pousada de estrada")
                    .put("security", 55)
                    .put("prosperity", 50)
                    .put("buildings", new JSONArray()
                            .put("Pousada Marco do Percurso")
                            .put("Ferraria de Oswin Talm")
                            .put("Moinho e Armazém de Grãos de Mira Venn")
                            .put("Casa de Ervas e Partos de Edda Fen")
                            .put("Praça Comunitária")
                            .put("Sítios e Apriscos Rurais"))
                    .put("npcs", new JSONArray()
                            .put("Kote").put("Bast").put("Oswin Talm").put("Mira Venn")
                            .put("Edda Fen").put("Teren Vale").put("Linna Arvel"))
                    .put("market", market)
                    .put("defense", defense);

            db.createOrUpdateSettlementForPoi(POI_NEWARRE_ID, settlementData);
        }

        // 3. Pousada Marco do Percurso (filha de Newarre)
        ensureSubPoi(db, POI_WAYSTONE_INN_ID, POI_NEWARRE_ID, INN_NAME, "estalagem",
                "Construção sólida de pedra polida e madeira escura de primeira qualidade, mantida impecavelmente limpa. Salão com lareira de pedra aberta, balcão de freixo polido e ambiente que respira acolhimento e discreta melancolia.", logicalMapId, mapPackageId, profile);

        // Sublocais da Pousada
        ensureSubPoi(db, POI_HALL_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Salão Principal", "edificio",
                "Salão comum com mesas de madeira pesada, lareira crepitante, iluminação quente por velas de sebo e lampiões, e um silêncio característico em três partes.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_BAR_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Balcão de Serviço", "edificio",
                "Balcão longo de madeira de freixo impecavelmente encerada, garrafas alinhadas em prateleiras, canecas de estanho reluzentes e Kote sempre presente.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_KITCHEN_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Cozinha e Despensa", "edificio",
                "Cozinha ampla com forno a lenha, panelas de ferro fundido, despensa com queijos curados, embutidos defumados e barris de sidra e cerveja.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_ROOMS_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Quartos de Hóspedes", "edificio",
                "Quartos modestos no piso superior, com camas confortáveis, lençóis lavados com sabão de lavanda e janelas voltadas para as colinas de Newarre.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_KOTE_ROOMS_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Aposentos de Kote", "edificio",
                "Quarto simples e austero nos fundos do andar superior. [SEGREDO DO MESTRE]: Sob o assoalho e num canto discreto repousa um baú de ferro e cobre com três fechaduras e a lendária espada Folly protegida em tecido cinzento.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_BAST_ROOMS_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Dependências de Bast", "edificio",
                "Pequeno quarto arejado com livros curiosos, ramos secos de árvores silvestres e objetos que revelam refinamento discreto e exótico.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_STABLES_ID, POI_WAYSTONE_INN_ID, "Marco do Percurso — Estábulo e Área Externa", "edificio",
                "Cocheira arejada com feno fresco e água limpa para cavalos e mulas de carga, além de pátio traseiro para carroças de viajantes.", logicalMapId, mapPackageId, profile);

        // Estabelecimentos da Aldeia
        ensureSubPoi(db, POI_BLACKSMITH_ID, POI_NEWARRE_ID, "Ferraria de Oswin", "oficina",
                "Oficina aberta com bigorna, fole e lareira acesa onde Oswin conserta enxadas, arados e ferraduras. Pequenas ferragens e reparos disponíveis.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_MILL_ID, POI_NEWARRE_ID, "Moinho e Armazém de Mira", "edificio",
                "Moinho de madeira com pás movidas pelo vento suave do vale, onde se processam sacos de trigo, cevada e milho colhidos nos campos circunvizinhos.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_SQUARE_ID, POI_NEWARRE_ID, "Praça Comunitária de Newarre", "rua",
                "Espaço aberto de grama batida e pedras arredondadas onde ocorrem encontros comunitários, anúncios da aldeia e estacionamento da carroça de Teren Vale.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_FARMS_ID, POI_NEWARRE_ID, "Fazendas e Apriscos do Entorno", "natureza",
                "Casas de campo de taipa e pedra, cercas de madeira cinzenta, ovelhas pastando e lavouras que desenham o contorno rural do hexágono DO40.", logicalMapId, mapPackageId, profile);

        ensureSubPoi(db, POI_WOODS_ID, POI_NEWARRE_ID, "Trilhas e Bosques de Newarre", "natureza",
                "Bosques mistos de faias e carvalhos que circundam o povoado, ricos em cogumelos, lenha seca e trilhas calmas para caminhadas locais.", logicalMapId, mapPackageId, profile);
    }

    private static void ensureSubPoi(AppDb db, String id, String parentId, String name, String type,
                                     String desc, String logicalMapId, String mapPackageId, String profile) {
        if (db.getCampaignPoi(id).length() > 0) return;
        try {
            String migration = (mapPackageId.length() > 0 && db.isValidMapHex(mapPackageId, HEX_ID)) ? "ready" : "migration_pending";
            JSONObject spec = new JSONObject()
                    .put("id", id)
                    .put("world_profile", profile)
                    .put("logical_map_id", logicalMapId)
                    .put("map_package_id", mapPackageId)
                    .put("hex_id", HEX_ID)
                    .put("name", name)
                    .put("type", type)
                    .put("description", desc)
                    .put("origin", "campaign")
                    .put("creator", "campaign")
                    .put("hidden", false)
                    .put("discovered", true)
                    .put("parent_poi_id", parentId)
                    .put("status", "active")
                    .put("migration_status", migration);
            db.createCampaignPoi(spec);
        } catch (Exception ignored) {}
    }

    /**
     * Gera a narração imersiva e sensorial de primeira chegada a Newarre,
     * rigorosamente dimensionada entre 5.000 e 6.000 caracteres conforme a diretriz.
     */
    public static String buildFirstArrivalNarration(String season, String timeOfDay, String weather) {
        String estacao = (season == null || season.trim().isEmpty()) ? "outono" : season.toLowerCase(Locale.ROOT);
        String horario = (timeOfDay == null || timeOfDay.trim().isEmpty()) ? "crepúsculo" : timeOfDay.toLowerCase(Locale.ROOT);
        String clima = (weather == null || weather.trim().isEmpty()) ? "névoa suave e brisa fria" : weather.toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();

        sb.append("A poeira da estrada batida que corta o Vale do Dessarin começa a ceder espaço a um solo mais macio, salpicado de trevos rasteiros e pedras arredondadas que parecem ter sido esquecidas pelos rios há eras. O viajante que avança pelo hexágono DO40 percebe, antes de qualquer placa ou marco oficial, uma desaceleração quase palpável no pulso do mundo. Não há muralhas imponentes erguidas contra o horizonte, nem sentinelas de armadura polida empunhando alabardas sobre pontes levadiças. Newarre não foi construída para resistir a exércitos nem para chamar a atenção de reis; ela simplesmente descansou ali, acomodada entre colinas onduladas cobertas de bosques de faias e pequenas propriedades familiares de cerca baixa.\n\n");

        sb.append("O ar do ").append(horario).append(" carrega a assinatura indelével de uma comunidade rural que vive em harmonia com as estações. Em pleno ").append(estacao).append(", sob uma atmosfera de ").append(clima).append(", o cheiro característico de lenha de macieira e carvalho queimando nas chaminés sobe em colunas finas de fumaça azulada, misturando-se ao odor adocicado da terra revirada, do feno recém-ensacado e do gado recolhido aos estábulos. Ao longe, o rangido compassado das pás do moinho de Mira Venn quebra a quietude com um ritmo reconfortante, como o tique-taque de um relógio antigo que nunca teve pressa de marcar as horas.\n\n");

        sb.append("As casas de Newarre, não mais de trinta distribuídas com generosa distância entre si, erguem-se em taipa caiada e alicerces de pedra de rio. Suas janelas de vidraça pequena e venezianas de madeira escura brilham com o tom alaranjado das lamparinas a óleo e das velas de cera de abelha. Alguns moradores realizam as últimas tarefas da labuta cotidiana: um homem de braços fortes e avental de couro esfrega o carvão das mãos junto a uma tina de água diante da ferraria, observando com curiosidade respeitosa quem chega da estrada; uma jovem com uma cesta de couve e raízes sob o braço caminha pela grama da praça comunitária, trocando risadas tímidas com uma senhora idosa que ajeita um xale de lã verde sobre os ombros.\n\n");

        sb.append("No coração desse povoado discreto, ergue-se o edifício mais cuidado de todo o vale: a Pousada Marco do Percurso. É uma construção ampla de dois pavimentos, sólida, erguida com vigas pesadas de freixo e pedras cinzentas perfeitamente assentadas, cujas telhas de ardósia reluzem com o sereno que começa a descer. Ao contrário de tantas tavernas barulhentas e enlameadas que pontilham as grandes rotas comerciais de Faerûn, a Marco do Percurso transmite uma sensação desconcertante de ordem e asseio. Os vidros de suas janelas amplas estão transparentes e sem fuligem; as tábuas da soleira foram lavadas com cuidado minucioso e o batente de ferro da porta não possui uma única lasca de ferrugem.\n\n");

        sb.append("Ao empurrar a porta de carvalho pesado, um calor acolhedor envolve o corpo como um manto seco. O salão principal é banhado pela claridade viva de uma lareira imensa, onde toras bem acomodadas estalam em chamas douradas sem lançar fumaça para dentro do recinto. O piso de tábuas largas de pinho está limpo ao ponto de refletir os reflexos do fogo. Algumas mesas de bordas gastas mas enceradas ocupam os cantos, onde dois ou três agricultores locais conversam em sussurros tranquilos, saboreando canecas de cerveja escura e tigelas de louça fumegantes.\n\n");

        sb.append("Contudo, o que mais chama a atenção de qualquer forasteiro experiente é o silêncio peculiar que habita a estalagem. É um silêncio em três partes bem distintas. A primeira parte repousa nas coisas materiais que não fazem barulho: na ausência de vento batendo nas frestas, na solidez das vigas de teto que não rangem e na compostura dos poucos homens sentados, que bebem sem algazarra. A segunda parte repousa no silêncio que falta aos próprios acontecimentos: não há bardo tocando alaúde na esquina, não há risadas estridentes de mercadores embriagados nem brigas de dados sendo disputadas. E a terceira parte — a mais profunda, paciente e pesada — pertence ao homem que se encontra imóvel atrás do balcão de freixo polido.\n\n");

        sb.append("O hospedeiro é um homem jovem, de porte comedido e avental de linho cândido, que move uma flanela seca com precisão ritmada sobre o balcão espelhado. Seus cabelos têm o tom vermelho inconfundível das chamas de outono, vibrantes mesmo na meia-luz, e seus olhos verdes examinam a entrada com uma perspicácia tranquila, que avalia a carga, o cansaço e os passos de quem chega sem demonstrar inquietação. Ele pousa o pano com calma natural, ergue o olhar e oferece um meio-sorriso hospitaleiro, como se estivesse exatamente onde deveria estar.\n\n");

        sb.append("Ao lado, entre a porta da cozinha e os barris de cerveja da despensa, um rapaz esguio e gracioso de cabelos pretos como asa de corvo, vestindo túnica folgada e botas de couro macio, coloca uma travessa de pães quentes sobre uma mesa e lança um olhar travesso em direção aos recém-chegados. O ambiente cheira a pão fresco, alecrim, sidra fermentada e manteiga derretida. Newarre abriu suas portas silenciosas; a Pousada Marco do Percurso oferece abrigo contra o frio e a incerteza dos caminhos de Faerûn.");

        return sb.toString();
    }

    /**
     * Gera narração contextual para visitas subsequentes, sensível ao horário, clima e estação.
     */
    public static String buildContextualNarration(String season, String timeOfDay, String weather, int visitCount) {
        String estacao = (season == null || season.trim().isEmpty()) ? "estação" : season.toLowerCase(Locale.ROOT);
        String horario = (timeOfDay == null || timeOfDay.trim().isEmpty()) ? "dia" : timeOfDay.toLowerCase(Locale.ROOT);

        return "As colinas suaves de Newarre recebem novamente a companhia sob a luz do " + horario +
                " em pleno " + estacao + ". Na Pousada Marco do Percurso, a fumaça da chaminé sinaliza que a lareira " +
                "está acesa e Kote se encontra a postos com o avental ajeitado, enquanto Bast circula entre o estábulo e as mesas. " +
                "A rotina pacífica da aldeia continua seu curso habitual: o martelo de Oswin soa rítmico na forja e o moinho de Mira " +
                "gira suavemente ao sopro do vento do Dessarin.";
    }

    /**
     * Cardápio e serviços econômicos da Pousada e de Newarre.
     */
    public static JSONObject getEconomicServices() {
        JSONObject o = new JSONObject();
        try {
            JSONArray lodging = new JSONArray()
                    .put(new JSONObject().put("service", "Pernoite no quarto comum (com cobertor lavado)").put("price", "5 pp"))
                    .put(new JSONObject().put("service", "Quarto privativo com banho quente").put("price", "5 po"))
                    .put(new JSONObject().put("service", "Estábulo coberto com feno, aveia e água fresca").put("price", "5 pp"));

            JSONArray meals = new JSONArray()
                    .put(new JSONObject().put("item", "Guisado de carneiro com raízes e pão de centeio quente").put("price", "3 pc"))
                    .put(new JSONObject().put("item", "Fatia generosa de queijo curado com maçãs assadas").put("price", "2 pc"))
                    .put(new JSONObject().put("item", "Caneca de cerveja escura da Marco do Percurso").put("price", "4 pc"))
                    .put(new JSONObject().put("item", "Garrafa de vinho de amora silvestre (safra local)").put("price", "2 pp"))
                    .put(new JSONObject().put("item", "Infusão de ervas revigorante preparada por Edda").put("price", "1 pc"));

            JSONArray supplies = new JSONArray()
                    .put(new JSONObject().put("item", "Ração de viagem para 5 dias (pão seco, carne defumada e nozes)").put("price", "2 po e 5 pp"))
                    .put(new JSONObject().put("item", "Conserto de cota de malha ou ferradura na ferraria de Oswin").put("price", "2 pp a 1 po"))
                    .put(new JSONObject().put("item", "Corda de cânhamo de 15 metros com gancho simples").put("price", "1 po"))
                    .put(new JSONObject().put("item", "Tochas resinadas (conjunto com 5)").put("price", "5 pc"));

            o.put("lodging", lodging);
            o.put("meals", meals);
            o.put("supplies", supplies);
        } catch (Exception ignored) {}
        return o;
    }

    public static boolean isNewarreHexOrLocation(String hexId, String location) {
        if (HEX_ID.equalsIgnoreCase(hexId)) return true;
        if (location == null) return false;
        String l = location.toLowerCase(Locale.ROOT);
        return l.contains("newarre") || l.contains("marco do percurso") || l.contains("waystone");
    }

    public static String getSceneNarrationForNewarre(AppDb db, String season, String timeOfDay, String weather) {
        if (db == null) return buildFirstArrivalNarration(season, timeOfDay, weather);
        String profile = db.getActiveMapProfile();
        List<JSONObject> history = db.getCampaignPoiHistory(POI_NEWARRE_ID);
        int visitCount = 0;
        for (JSONObject event : history) {
            if ("visited".equalsIgnoreCase(event.optString("event_type"))) {
                visitCount++;
            }
        }
        if (visitCount == 0) {
            try {
                ContentValues cv = new ContentValues();
                cv.put("poi_id", POI_NEWARRE_ID);
                cv.put("world_profile", profile);
                cv.put("event_type", "visited");
                cv.put("payload_json", new JSONObject().put("first_arrival", true).toString());
                cv.put("world_tick", db.getSceneState().optInt("day_number", 1));
                cv.put("created_at", System.currentTimeMillis());
                db.getWritableDatabase().insert("campaign_poi_events", null, cv);
            } catch (Exception ignored) {}
            return buildFirstArrivalNarration(season, timeOfDay, weather);
        } else {
            return buildContextualNarration(season, timeOfDay, weather, visitCount);
        }
    }
}
