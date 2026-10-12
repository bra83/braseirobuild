package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulos de aventura não lineares para o cenário de Forgotten Realms.
 *
 * Distinção editorial estrita:
 * 1. Adaptações de Aventuras Oficiais Publicadas:
 *    - "The Tomb of Damara" (FRAiF Ch 2, p. 86) — Nível 3. Lady Khara Sulwood, Bonegnasher, resgate da família Silverstar e Orbe de Damara.
 *    - "The Lost Library of Lethchauntos" (FRAiF Ch 7 / FRAiF-TLLoL) — Nível 1. Old Skull Inn, Myrna Nor vs Powell "the Owl" Niccam, disputa pelo tomo Nathlum.
 *
 * 2. Módulos de Campanha Originais Inspirados no Cenário:
 *    - "The Heir's Trial" (Waterdeep, Conselho dos Lordes Mascarados).
 *    - "The Archmage's Legacy" (Anauroch e ruínas netherenses).
 *    - "Conspiracy in the Under-Cellar" (Baldur's Gate, esgotos e culto dos Três Mortos).
 */
public final class FaerunAdventureModuleCatalog {
    private FaerunAdventureModuleCatalog() {}

    public static final String SOURCE_BOOK = "Forgotten Realms: Adventures in Faerûn";
    public static final String SOURCE_CODE = "FRAiF";

    public static List<AdventureModule> all() {
        List<AdventureModule> out = new ArrayList<>();
        out.add(buildTombOfDamara());
        out.add(buildLostLibrary());
        out.add(buildHeirTrial());
        out.add(buildArchmageLegacy());
        out.add(buildBaldursUndercellar());
        return Collections.unmodifiableList(out);
    }

    public static AdventureModule byId(String id) {
        if (id == null) return null;
        for (AdventureModule m : all()) {
            if (m.getId().equalsIgnoreCase(id.trim())) return m;
        }
        return null;
    }

    public static JSONObject coverage() {
        JSONObject o = new JSONObject();
        try {
            int nodes = 0;
            for (AdventureModule m : all()) nodes += m.getNodes().size();
            return o.put("source", SOURCE_CODE).put("modules", 5)
                    .put("official_published_adventures", 2)
                    .put("original_campaign_hook_modules", 3)
                    .put("nodes", nodes)
                    .put("non_linear_branches", true).put("auto_rolls", false);
        } catch (Exception e) {
            return o;
        }
    }

    // =========================================================================
    // 1. THE TOMB OF DAMARA (Oficial Publicada - FRAiF Ch 2, p. 86, Nível 3)
    // =========================================================================
    private static AdventureModule buildTombOfDamara() {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();

        // Setup
        List<AdventureTransition> setupTr = new ArrayList<>();
        setupTr.add(new AdventureTransition("begin", "scene_1", "Aceitar o contrato de Lady Sulwood e marchar até o castelo em ruínas.",
                "", "", false, null, fx("add_flags", arr("damara_contract_accepted"), "add_facts", arr("Khara Sulwood contratou o grupo em Shadowdale"))));
        nodes.put("setup", new AdventureNode("setup", "The Tomb of Damara — Missão de Shadowdale",
                "Lady Khara Sulwood convoca os aventureiros em Shadowdale Town para entrar na Tumba de Damara e destruir o warlord ogro espectral Bonegnasher, que aprisionou a família Silverstar e busca o Orbe de Damara.",
                directives(), "shadowdale_town", Arrays.asList("faerun_khara_sulwood"), setupTr, null, null));

        // Cena 1: Barrack and Great Hall
        List<AdventureTransition> sc1Tr = new ArrayList<>();
        sc1Tr.add(new AdventureTransition("direct_assault", "scene_2", "Combater os ghouls de frente no quartel e abrir caminho até as celas.",
                "", "", false, null, fx("add_flags", arr("damara_ghouls_slain"), "add_clues", arr("chaves_do_carcereiro_ghoul"), "trigger_encounter", "enc_ghouls_barrack")));
        JSONObject stealthCheck = check("Stealth", 12);
        sc1Tr.add(new AdventureTransition("stealth_bypass", "scene_2", "Esgueirar-se pelas sombras evitando o confronto ruidoso.",
                "", "", false, stealthCheck, fx("add_flags", arr("damara_ghouls_bypassed"), "add_clues", arr("patrulhas_hobgoblin_desatentas"))));
        nodes.put("scene_1", new AdventureNode("scene_1", "The Tomb of Damara — Quartel e Grande Salão",
                "Nas ruínas do castelo que encobrem a tumba, dois ghouls devoradores espreitam no antigo quartel prontos para saltar ao menor ruído no grande salão.",
                directives(), "tomb_barracks", Arrays.asList("ghoul_sentinels"), sc1Tr, null, null));

        // Cena 2: Prison & Silverstar Rescue
        List<AdventureTransition> sc2Tr = new ArrayList<>();
        JSONObject rescueCheck = check("Stealth", 13);
        sc2Tr.add(new AdventureTransition("rescue_direct", "scene_3", "Infiltrar-se e resgatar os Silverstar; Sara entrega a chave mágica da tumba de Damara.",
                "", "", false, rescueCheck, fx("add_flags", arr("silverstars_rescued", "tomb_key_obtained"),
                "complete_objectives", arr("rescue_silverstars"), "npc_states", npcMap("sara_silverstar", "rescued", "hobgoblin_captain", "defeated"))));
        JSONObject parleyCheck = check("Persuasion", 14);
        sc2Tr.add(new AdventureTransition("parley_hobgoblins", "scene_3", "Enganar ou subornar o Capitão Hobgoblin com ouro para libertar a família.",
                "", "", false, parleyCheck, fx("add_flags", arr("silverstars_ransomed", "tomb_key_obtained"),
                "complete_objectives", arr("rescue_silverstars"), "npc_states", npcMap("sara_silverstar", "rescued", "hobgoblin_captain", "bribed"))));
        nodes.put("scene_2", new AdventureNode("scene_2", "The Tomb of Damara — Prisão e Resgate dos Silverstar",
                "Quatro membros da família Silverstar são interrogados por um Hobgoblin Captain e dois guardas. A caçula Sara Silverstar esconde a chave mágica da tumba de Damara sob os trapos.",
                directives(), "tomb_prison", Arrays.asList("sara_silverstar", "hobgoblin_captain"), sc2Tr, null, null));

        // Cena 3: Bonegnasher's Den
        List<AdventureTransition> sc3Tr = new ArrayList<>();
        JSONObject banishCheck = check("History", 13);
        sc3Tr.add(new AdventureTransition("banish_specter", "scene_4", "Invocar as fraquezas ancestrais de Bonegnasher e dissipar sua presença espectral.",
                "", "", false, banishCheck, fx("add_flags", arr("bonegnasher_banished"), "npc_states", npcMap("bonegnasher", "banished"))));
        sc3Tr.add(new AdventureTransition("slay_specter", "scene_4", "Destruir a manifestação espectral colossal de Bonegnasher pela força das armas consagradas.",
                "", "", false, null, fx("add_flags", arr("bonegnasher_destroyed"), "npc_states", npcMap("bonegnasher", "destroyed"))));
        nodes.put("scene_3", new AdventureNode("scene_3", "The Tomb of Damara — Covil de Bonegnasher",
                "O espírito colossal do senhor ogro Bonegnasher canaliza energias necróticas sobre a porta selada, rugindo por vingança e pela posse do Orbe de Damara.",
                directives(), "tomb_bonegnasher_lair", Arrays.asList("bonegnasher_warlord"), sc3Tr, null, null));

        // Cena 4: Damara's Tomb & Conclusion
        List<AdventureTransition> sc4Tr = new ArrayList<>();
        JSONObject cleanseReward = fx("add_flags", arr("damara_crypt_cleansed", "orb_of_damara_recovered"),
                "consequence", consequence("Dalelands", 2, 400), "consume_reward", "rew_damara_orb");
        sc4Tr.add(new AdventureTransition("cleanse_crypt", "completed", "Abrir a porta mágica com a chave de Sara, pacificar a guardiã Damara e entregar o Orbe a Lady Sulwood.",
                "tomb_key_obtained", "", false, null, cleanseReward));
        JSONObject claimReward = fx("add_flags", arr("damara_orb_claimed_by_party"),
                "consequence", consequence("Dalelands", 1, 100), "consume_reward", "rew_damara_orb_private");
        sc4Tr.add(new AdventureTransition("claim_orb_for_party", "completed", "Reclamar o Orbe de Damara para o próprio grupo arcano, aceitando apenas a recompensa de caçada.",
                "tomb_key_obtained", "", false, null, claimReward));
        nodes.put("scene_4", new AdventureNode("scene_4", "The Tomb of Damara — Cripta e Conclusão",
                "A porta mágica se abre com a chave de Sara. No interior repousa o sarcófago de Damara e o fabled Orbe de Damara. Lady Sulwood aguarda o retorno dos heróis em Shadowdale.",
                directives(), "tomb_crypt_chamber", Arrays.asList("faerun_khara_sulwood", "damara_guardian"), sc4Tr, null, reward(400, "Orb of Damara")));

        JSONObject meta = meta("tomb_damara", 3, true, "official_published_adventure_adaptation", "official_published_adventure_adaptation");
        return new AdventureModule("fraif_tomb_damara", "The Tomb of Damara (FRAiF Oficial)", "1.0.0", "setup", nodes, meta);
    }

    // =========================================================================
    // 2. THE LOST LIBRARY OF LETHCHAUNTOS (Oficial Publicada - FRAiF Ch 7 / TLLoL, Nível 1)
    // =========================================================================
    private static AdventureModule buildLostLibrary() {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();

        // Setup
        List<AdventureTransition> setupTr = new ArrayList<>();
        setupTr.add(new AdventureTransition("begin", "scene_1", "Aceitar o pacto com Myrna Nor e partir para a floresta de Cormanthor.",
                "", "", false, null, fx("add_flags", arr("lethchauntos_contract_signed"), "add_facts", arr("Myrna Nor quer salvar o tomo Nathlum do Zhentarim"))));
        nodes.put("setup", new AdventureNode("setup", "The Lost Library of Lethchauntos — Premissa",
                "No Old Skull Inn em Shadowdale, a herdeira Myrna Nor contrata o grupo para encontrar a Biblioteca Perdida de Lethchauntos e resgatar o lendário tomo Nathlum antes de Powell 'the Owl' Niccam.",
                directives(), "shadowdale_old_skull_inn", Arrays.asList("myrna_nor"), setupTr, null, null));

        // Cena 1: The Old Skull Inn & Journey
        List<AdventureTransition> sc1Tr = new ArrayList<>();
        JSONObject trackCheck = check("Survival", 12);
        sc1Tr.add(new AdventureTransition("track_powell_trail", "scene_2", "Rastrear os rastros de Powell através das 100 milhas de Cormanthor com perícia florestal.",
                "", "", false, trackCheck, fx("add_flags", arr("powell_trail_tracked"), "add_clues", arr("rastros_frescos_de_carrocas"))));
        sc1Tr.add(new AdventureTransition("hasty_march", "scene_2", "Marcha forçada acelerada sem testes, aceitando risco de fadiga.",
                "", "", false, null, fx("add_flags", arr("hasty_march_taken"), "add_clues", arr("acampamento_zhentilar_avistado"))));
        nodes.put("scene_1", new AdventureNode("scene_1", "The Lost Library of Lethchauntos — Jornada por Cormanthor",
                "Marcha de quatro dias sob as copas ancestrais de Cormanthor rumo à colina oculta onde fica a entrada da biblioteca de Lethchauntos.",
                directives(), "cormanthor_trails", Arrays.asList("myrna_nor"), sc1Tr, null, null));

        // Cena 2: Powell's Vanguard
        List<AdventureTransition> sc2Tr = new ArrayList<>();
        JSONObject pickCheck = check("Thieves' Tools", 15);
        sc2Tr.add(new AdventureTransition("pick_lock_quietly", "scene_3", "Destrancar as portas reforçadas com ferramentas de ladinagem sem alertar as tochas do interior.",
                "", "", false, pickCheck, fx("add_flags", arr("library_door_picked", "breach_library"), "add_clues", arr("caminho_livre_para_o_scriptorium"), "complete_objectives", arr("breach_library"))));
        JSONObject forceCheck = check("Athletics", 14);
        sc2Tr.add(new AdventureTransition("force_door", "scene_3", "Arrombar as portas pesadas à força, chamando a atenção dos guardas de Powell.",
                "", "", false, forceCheck, fx("add_flags", arr("library_door_broken", "breach_library"), "add_clues", arr("sentinelas_em_alerta_maximo"), "complete_objectives", arr("breach_library"))));
        nodes.put("scene_2", new AdventureNode("scene_2", "The Lost Library of Lethchauntos — Vanguarda de Powell",
                "A entrada na colina foi desobstruída. Portas maciças de boa qualidade protegem os corredores iluminados por tochas deixadas pelos capangas de Powell Niccam.",
                directives(), "lethchauntos_outer_halls", Arrays.asList("powell_sentinels"), sc2Tr, null, null));

        // Cena 3: The Athenaeum Confrontation
        List<AdventureTransition> sc3Tr = new ArrayList<>();
        JSONObject parleyPowellCheck = check("Persuasion", 15);
        sc3Tr.add(new AdventureTransition("negotiate_powell", "scene_4", "Convencer Powell 'the Owl' de que a traição com o Zhentarim trará sua ruína.",
                "", "", false, parleyPowellCheck, fx("add_flags", arr("powell_surrendered"), "npc_states", npcMap("powell_niccam", "pacified"))));
        sc3Tr.add(new AdventureTransition("combat_powell", "scene_4", "Derrotar Powell e seus guardas em confronto nos salões de pergaminhos.",
                "", "", false, null, fx("add_flags", arr("powell_defeated_in_battle"), "npc_states", npcMap("powell_niccam", "captured"))));
        nodes.put("scene_3", new AdventureNode("scene_3", "The Lost Library of Lethchauntos — O Ateneu",
                "No coração da biblioteca de Lethchauntos, Powell Niccam e seus mercenários tentam remover o tomo Nathlum de seu pedestal protegido por glifos.",
                directives(), "lethchauntos_athenaeum", Arrays.asList("powell_the_owl_niccam"), sc3Tr, null, null));

        // Cena 4: The Nathlum Preserved
        List<AdventureTransition> sc4Tr = new ArrayList<>();
        JSONObject myrnaReward = fx("add_flags", arr("nathlum_given_to_myrna", "harpers_favor"),
                "consequence", consequence("The Harpers", 2, 250), "consume_reward", "rew_nathlum_reward");
        sc4Tr.add(new AdventureTransition("deliver_to_myrna", "completed", "Entregar o Nathlum a Myrna Nor para estudo legítimo e preservação histórica.",
                "breach_library", "", false, null, myrnaReward));
        JSONObject blackMarketReward = fx("add_flags", arr("nathlum_kept_secret"),
                "consequence", consequence("The Zhentarim", 1, 500), "consume_reward", "rew_nathlum_black_market");
        sc4Tr.add(new AdventureTransition("keep_poison_secrets", "completed", "Vender as fórmulas de venenos raros aos contrabandistas da Rede Negra.",
                "breach_library", "", false, null, blackMarketReward));
        nodes.put("scene_4", new AdventureNode("scene_4", "The Lost Library of Lethchauntos — Destino do Nathlum",
                "Com Powell neutralizado, o tomo Nathlum está acessível com seus segredos de venenos e curas lendárias. O destino do livro define a reputação do grupo.",
                directives(), "lethchauntos_sanctum", Arrays.asList("myrna_nor"), sc4Tr, null, reward(250, "Nathlum Grimoire")));

        JSONObject meta = meta("lost_library", 1, true, "official_published_adventure_adaptation", "official_published_adventure_adaptation");
        return new AdventureModule("fraif_lost_library", "The Lost Library of Lethchauntos (FRAiF Oficial)", "1.0.0", "setup", nodes, meta);
    }

    // =========================================================================
    // 3. THE HEIR'S TRIAL (Original - Gancho de Campanha Waterdeep, Nível 1)
    // =========================================================================
    private static AdventureModule buildHeirTrial() {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();

        // Setup
        List<AdventureTransition> setupTr = new ArrayList<>();
        setupTr.add(new AdventureTransition("begin", "scene_1", "Entrar no Palácio de Waterdeep sob convocação dos Lordes.",
                "", "", false, null, fx("add_flags", arr("heir_trial_started"))));
        nodes.put("setup", new AdventureNode("setup", "The Heir's Trial — Premissa",
                "[Aventura Original] Em Waterdeep, um herdeiro legítimo do trono é ameaçado por conspiradores ligados à Rede Negra; o grupo deve protegê-lo e descobrir as provas de sua linhagem real.",
                directives(), "waterdeep_palace", Arrays.asList("masked_lord_rep"), setupTr, null, null));

        // Cena 1: Council of Masks
        List<AdventureTransition> sc1Tr = new ArrayList<>();
        sc1Tr.add(new AdventureTransition("advance_1", "scene_2", "Defender abertamente a reivindicação do herdeiro perante os Lordes.",
                "", "", false, null, fx("add_flags", arr("heir_claimed_openly"), "add_clues", arr("ameaca_zhentilar_confirmada"))));
        sc1Tr.add(new AdventureTransition("covert_investigation", "scene_2", "Investigar em segredo sem expor o herdeiro à corte pública.",
                "", "", false, null, fx("add_flags", arr("heir_hidden_identity"), "add_clues", arr("pista_das_docas"))));
        nodes.put("scene_1", new AdventureNode("scene_1", "The Heir's Trial — Conselho de Máscaras",
                "O conselho dos Lordes Mascarados de Waterdeep recebe a reivindicação do trono sob protestos do emissário dos Zhentarim.",
                directives(), "waterdeep_palace", Arrays.asList("masked_lord_rep"), sc1Tr, null, null));

        // Cena 2: Streets of Shadows
        List<AdventureTransition> sc2Tr = new ArrayList<>();
        sc2Tr.add(new AdventureTransition("advance_2", "scene_3", "Confrontar os mercenários nas vielas das Docas.",
                "", "", false, null, fx("add_flags", arr("zhents_routed"), "complete_objectives", arr("protect_heir"))));
        sc2Tr.add(new AdventureTransition("diplomatic_branch", "scene_4", "Resolver por via diplomática ou evasão rápida pelos telhados, pulando o confronto direto.",
                "", "", false, null, fx("add_flags", arr("dock_ambush_evaded"), "complete_objectives", arr("protect_heir"))));
        nodes.put("scene_2", new AdventureNode("scene_2", "The Heir's Trial — Ruas de Sombras",
                "Emboscada nos becos do Distrito das Docas; agentes da Rede Negra tentam sequestrar o herdeiro.",
                directives(), "waterdeep_dock_ward", Arrays.asList("zhentarim_ambushers"), sc2Tr, null, null));

        // Cena 3: Vault of Proof
        List<AdventureTransition> sc3Tr = new ArrayList<>();
        JSONObject investCheck = check("Investigation", 14);
        sc3Tr.add(new AdventureTransition("seal_verified", "scene_4", "Decifrar o selo da cripta ancestral na Cidade dos Mortos.",
                "", "", false, investCheck, fx("add_flags", arr("royal_seal_recovered"), "add_clues", arr("linhagem_comprovada"))));
        sc3Tr.add(new AdventureTransition("cleric_witness", "scene_4", "Obter o testemunho de sacerdotes de Kelemvor.",
                "", "", false, null, fx("add_flags", arr("cleric_testimony_obtained"))));
        nodes.put("scene_3", new AdventureNode("scene_3", "The Heir's Trial — Cofre das Provas",
                "Investigação da cripta ancestral na Cidade dos Mortos onde jaz o selo heráldico da linhagem.",
                directives(), "waterdeep_city_of_dead", Arrays.asList("city_guard_captain"), sc3Tr, null, null));

        // Cena 4: Coronation or Exile
        List<AdventureTransition> sc4Tr = new ArrayList<>();
        sc4Tr.add(new AdventureTransition("coronate_heir", "completed", "Coroar o herdeiro e fortalecer a Aliança dos Lordes.",
                "", "", false, null, fx("add_flags", arr("heir_crowned"),
                "consequence", consequence("Lords Alliance", 3, 300), "consume_reward", "rew_waterdeep_patron")));
        sc4Tr.add(new AdventureTransition("exile_and_protect", "completed", "Exilar o herdeiro sob proteção secreta dos Harpistas.",
                "", "", false, null, fx("add_flags", arr("heir_secretly_exiled"),
                "consequence", consequence("The Harpers", 2, 200), "consume_reward", "rew_harper_safehouse")));
        nodes.put("scene_4", new AdventureNode("scene_4", "The Heir's Trial — Coroação ou Exílio",
                "Confronto político final no palácio sob o olhar de Laeral Silverhand.",
                directives(), "waterdeep_palace", Arrays.asList("faerun_silver_hand"), sc4Tr, null, reward(300, "Royal Sigil Ring")));

        JSONObject meta = meta("heir_trial", 1, false, "original_scenario_campaign_hook", "original_scenario_compatible");
        return new AdventureModule("fraif_heir_trial", "The Heir's Trial (FRAiF Original)", "1.0.0", "setup", nodes, meta);
    }

    // =========================================================================
    // 4. THE ARCHMAGE'S LEGACY (Original - Gancho de Campanha Anauroch, Nível 5)
    // =========================================================================
    private static AdventureModule buildArchmageLegacy() {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();

        // Setup
        List<AdventureTransition> setupTr = new ArrayList<>();
        setupTr.add(new AdventureTransition("begin", "scene_1", "Partir para a fronteira de Anauroch.",
                "", "", false, null, fx("add_flags", arr("archmage_legacy_started"))));
        nodes.put("setup", new AdventureNode("setup", "The Archmage's Legacy — Premissa",
                "[Aventura Original] Com o sumiço do Arquimago mestre nas fronteiras de Anauroch, os aprendizes devem recuperar o grimório legado antes que cultistas netherenses o corrompam.",
                directives(), "anauroch_edge", Arrays.asList("bedine_guide"), setupTr, null, null));

        // Cena 1: The Desert Outpost
        List<AdventureTransition> sc1Tr = new ArrayList<>();
        sc1Tr.add(new AdventureTransition("hire_bedine_guide", "scene_2", "Contratar batedores nômades Bedine para cruzar as areias.",
                "", "", false, null, fx("add_flags", arr("bedine_guide_allied"), "add_clues", arr("trilhas_seguras_nas_dunas"))));
        JSONObject survCheck = check("Survival", 14);
        sc1Tr.add(new AdventureTransition("navigate_by_stars", "scene_2", "Navegar pelas estrelas do deserto de forma independente.",
                "", "", false, survCheck, fx("add_flags", arr("solo_navigation_achieved"), "add_clues", arr("anomalia_magica_rastreada"))));
        nodes.put("scene_1", new AdventureNode("scene_1", "The Archmage's Legacy — Posto Avançado",
                "Chegada ao posto avançado no limiar das dunas onde rastros de tempestade mágica foram avistados.",
                directives(), "anauroch_edge", Arrays.asList("bedine_guide"), sc1Tr, null, null));

        // Cena 2: The Shattered Tower
        List<AdventureTransition> sc2Tr = new ArrayList<>();
        JSONObject arcCheck = check("Arcana", 15);
        sc2Tr.add(new AdventureTransition("disarm_glyphs", "scene_3", "Neutralizar os glifos arcanos da torre netherense.",
                "", "", false, arcCheck, fx("add_flags", arr("tower_glyphs_neutralized"), "add_clues", arr("codigo_da_camara_arcana"))));
        sc2Tr.add(new AdventureTransition("force_ward", "scene_3", "Forçar a barreira arcana aceitando descarga de choque.",
                "", "", false, null, fx("add_flags", arr("ward_forced_shock"), "add_clues", arr("camara_arrombada"))));
        nodes.put("scene_2", new AdventureNode("scene_2", "The Archmage's Legacy — A Torre Fraturada",
                "Exploração da torre semidestruída com armadilhas de glifos arcanos de Netheril.",
                directives(), "netheril_tower", Arrays.asList("tower_automaton"), sc2Tr, null, null));

        // Cena 3: The Weave Fracture
        List<AdventureTransition> sc3Tr = new ArrayList<>();
        sc3Tr.add(new AdventureTransition("restore_weave", "scene_4", "Reparar a fenda da Trama com energia purificada.",
                "", "", false, null, fx("add_flags", arr("weave_restored", "secure_grimoire"), "complete_objectives", arr("secure_grimoire"))));
        sc3Tr.add(new AdventureTransition("absorb_anomaly", "scene_4", "Drenar a anomalia arcana para fortalecer o foco mágico do grupo.",
                "", "", false, null, fx("add_flags", arr("weave_energy_drained", "secure_grimoire"), "complete_objectives", arr("secure_grimoire"))));
        nodes.put("scene_3", new AdventureNode("scene_3", "The Archmage's Legacy — Fratura da Trama",
                "Confronto com espectros do Weave que tentam rasgar o grimório deixado pelo arquimago.",
                directives(), "anauroch_ruins", Arrays.asList("weave_specters"), sc3Tr, null, null));

        // Cena 4: Sanctum Claimed
        List<AdventureTransition> sc4Tr = new ArrayList<>();
        sc4Tr.add(new AdventureTransition("donate_blackstaff", "completed", "Doar a torre e o grimório à academia de Blackstaff.",
                "secure_grimoire", "", false, null, fx("add_flags", arr("tower_donated_to_blackstaff"),
                "consequence", consequence("Lords Alliance", 2, 400), "consume_reward", "rew_archmage_grimoire")));
        sc4Tr.add(new AdventureTransition("claim_private_sanctum", "completed", "Reclamar a posse privada da torre demiplanar de pesquisa.",
                "secure_grimoire", "", false, null, fx("add_flags", arr("tower_claimed_privately"),
                "consequence", consequence("The Harpers", 1, 600), "consume_reward", "rew_netherese_battery")));
        nodes.put("scene_4", new AdventureNode("scene_4", "The Archmage's Legacy — Santuário Reclamado",
                "Abertura do portal demiplanar e estabelecimento da posse legítima da torre de pesquisa.",
                directives(), "demiplane_tower", Arrays.asList("archmage_familiar"), sc4Tr, null, reward(600, "Netherese Spell Battery")));

        JSONObject meta = meta("archmage_legacy", 5, false, "original_scenario_campaign_hook", "original_scenario_compatible");
        return new AdventureModule("fraif_archmage_legacy", "The Archmage's Legacy (FRAiF Original)", "1.0.0", "setup", nodes, meta);
    }

    // =========================================================================
    // 5. CONSPIRACY IN THE UNDER-CELLAR (Original - Gancho de Campanha Baldur's Gate, Nível 2)
    // =========================================================================
    private static AdventureModule buildBaldursUndercellar() {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();

        // Setup
        List<AdventureTransition> setupTr = new ArrayList<>();
        setupTr.add(new AdventureTransition("begin", "scene_1", "Descer aos becos da Cidade Baixa em busca de pistas.",
                "", "", false, null, fx("add_flags", arr("undercellar_investigation_started"))));
        nodes.put("setup", new AdventureNode("setup", "Conspiracy in the Under-Cellar — Premissa",
                "[Aventura Original] Nos corredores sombrios do Under-Cellar em Baldur's Gate, agentes dos Três Mortos conspiram com mercadores corruptos para envenenar os reservatórios da cidade.",
                directives(), "baldurs_lower_city", Arrays.asList("guild_informant"), setupTr, null, null));

        // Cena 1: Shadows of the Lower City
        List<AdventureTransition> sc1Tr = new ArrayList<>();
        sc1Tr.add(new AdventureTransition("bribe_informant", "scene_2", "Subornar o informante portuário com 20 GP por mapas dos esgotos.",
                "", "", false, null, fx("add_flags", arr("cistern_entrance_mapped"), "add_clues", arr("simbolo_dos_tres_mortos"))));
        JSONObject intimCheck = check("Intimidation", 13);
        sc1Tr.add(new AdventureTransition("interrogate_smuggler", "scene_2", "Intimidar o contrabandista nos fundos da taverna.",
                "", "", false, intimCheck, fx("add_flags", arr("smuggler_confessed"), "add_clues", arr("lote_de_veneno_letal"))));
        nodes.put("scene_1", new AdventureNode("scene_1", "Conspiracy in the Under-Cellar — Cidade Baixa",
                "Investigação nas tavernas portuárias para identificar o contato dos contrabandistas.",
                directives(), "baldurs_lower_city", Arrays.asList("guild_informant"), sc1Tr, null, null));

        // Cena 2: The Under-Cellar Cisterns
        List<AdventureTransition> sc2Tr = new ArrayList<>();
        JSONObject percCheck = check("Perception", 14);
        sc2Tr.add(new AdventureTransition("disarm_gas_traps", "scene_3", "Detectar os frascos de veneno e desarmar as armadilhas de gás.",
                "", "", false, percCheck, fx("add_flags", arr("gas_traps_disarmed"), "add_clues", arr("passagem_secreta_do_santuario"))));
        sc2Tr.add(new AdventureTransition("endure_poison_gas", "scene_3", "Avançar sob a névoa tóxica chamando a atenção dos fanáticos.",
                "", "", false, null, fx("add_flags", arr("gas_trap_triggered"), "add_clues", arr("cultistas_alertados"))));
        nodes.put("scene_2", new AdventureNode("scene_2", "Conspiracy in the Under-Cellar — Cisternas",
                "Descida pelos esgotos labirínticos repletos de reservatórios e armadilhas de gás.",
                directives(), "baldurs_undercellar_cistern", Arrays.asList("cistern_scavenger"), sc2Tr, null, null));

        // Cena 3: Altar of the Three
        List<AdventureTransition> sc3Tr = new ArrayList<>();
        sc3Tr.add(new AdventureTransition("capture_cult_leader", "scene_4", "Capturar o mestre fanático vivo para interrogatório oficial.",
                "", "", false, null, fx("add_flags", arr("cult_leader_captured"), "npc_states", npcMap("dead_three_leader", "arrested"))));
        sc3Tr.add(new AdventureTransition("destroy_toxic_vats", "scene_4", "Focar na destruição das cubas tóxicas antes que o reservatório seja contaminado.",
                "", "", false, null, fx("add_flags", arr("cistern_poison_neutralized"), "npc_states", npcMap("dead_three_leader", "escaped"))));
        nodes.put("scene_3", new AdventureNode("scene_3", "Conspiracy in the Under-Cellar — Altar dos Três",
                "Interrupção do ritual macabro de contaminação perante o altar dedicado a Bane, Bhaal e Myrkul.",
                directives(), "baldurs_shrine_dead_three", Arrays.asList("dead_three_cult_leader"), sc3Tr, null, null));

        // Cena 4: Gates of Dawn
        List<AdventureTransition> sc4Tr = new ArrayList<>();
        sc4Tr.add(new AdventureTransition("report_flaming_fist", "completed", "Entregar as provas e o prisioneiro ao comandante dos Punhos Flamejantes.",
                "", "", false, null, fx("add_flags", arr("flaming_fist_commended"),
                "consequence", consequence("Flaming Fist", 2, 150), "consume_reward", "rew_flaming_fist_badge")));
        sc4Tr.add(new AdventureTransition("report_harper_network", "completed", "Entregar os frascos de veneno à rede secreta dos Harpistas em Baldur's Gate.",
                "", "", false, null, fx("add_flags", arr("harpers_notified_undercellar"),
                "consequence", consequence("The Harpers", 2, 100), "consume_reward", "rew_harper_token")));
        nodes.put("scene_4", new AdventureNode("scene_4", "Conspiracy in the Under-Cellar — Seatower de Baldur's Gate",
                "Relatório final às autoridades de Baldur's Gate e definição de reputação na Cidade Alta.",
                directives(), "baldurs_seatower", Arrays.asList("flaming_fist_commander"), sc4Tr, null, reward(150, "Flaming Fist Badge of Merit")));

        JSONObject meta = meta("baldurs_undercellar", 2, false, "original_scenario_campaign_hook", "original_scenario_compatible");
        return new AdventureModule("fraif_baldurs_undercellar", "Conspiracy in the Under-Cellar (FRAiF Original)", "1.0.0", "setup", nodes, meta);
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS
    // =========================================================================
    private static String directives() {
        return "Manter consistência com a história oficial de Faerûn. Exigir rolagens explícitas do jogador para perícias sem auto-rolls.";
    }

    private static JSONObject check(String skill, int dc) {
        JSONObject ck = new JSONObject();
        try {
            ck.put("skill", skill).put("dc", dc).put("requires_check", true);
        } catch (Exception ignored) {}
        return ck;
    }

    private static JSONObject reward(int gold, String item) {
        JSONObject rew = new JSONObject();
        try {
            rew.put("gold", gold).put("item", item);
        } catch (Exception ignored) {}
        return rew;
    }

    private static JSONObject consequence(String faction, int delta, int gold) {
        JSONObject cons = new JSONObject();
        try {
            cons.put("reputation_faction", faction).put("reputation_delta", delta).put("gold", gold);
        } catch (Exception ignored) {}
        return cons;
    }

    private static JSONArray arr(String... items) {
        JSONArray a = new JSONArray();
        for (String i : items) a.put(i);
        return a;
    }

    private static JSONObject npcMap(String... pairs) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                o.put(pairs[i], pairs[i + 1]);
            }
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject fx(Object... pairs) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                o.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject meta(String slug, int level, boolean official, String tier, String fidelity) {
        JSONObject meta = new JSONObject();
        try {
            meta.put("source_book", SOURCE_BOOK).put("source_code", SOURCE_CODE).put("slug", slug)
                .put("recommended_level", level)
                .put("is_official_published", official)
                .put("runtime_tier", tier)
                .put("source_fidelity", fidelity)
                .put("non_linear", true).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return meta;
    }
}
