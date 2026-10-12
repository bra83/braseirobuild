package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Adversarial edge-case harness testing KingmakerCampaignModels in pure JVM desktop mode.
 *
 * Covers:
 * 1. Extreme level boundaries (level 0 to level 25) for hazards and invalid levels.
 * 2. Complex hazards with multiple routine actions, reactions, traits-based detection,
 *    and multi-skill disable options.
 * 3. Deep dialogue branching, missing/null/corrupt NPC attitude values, and attitude clamping.
 * 4. Comprehensive roundtrip and null/empty safety for all 17 model classes.
 * 5. HexPoi constraints and Quest lifecycle state machines.
 */
public final class KingmakerCampaignModelsEdgeHarness {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FALHA NO EDGE HARNESS: " + label);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== INICIANDO KINGMAKER CAMPAIGN MODELS EDGE HARNESS ===");

        testHazardLevelBoundaries();
        testHazardComplexRoutineAndReactions();
        testHazardMultiSkillDisableOptions();
        testHazardEdgeFallbacksAndSlugs();
        testNpcMissingAndCorruptAttitudes();
        testNpcDeepDialogueTreeBranching();
        testAllSeventeenModelClassesPureJvm();
        testQuestStateTransitionsAndPrerequisites();
        testHexPoiConstraintsAndNormalizations();
        testChapterCatalogExtremeLevelRanges();

        System.out.println("KINGMAKER_CAMPAIGN_MODELS_EDGE_HARNESS_PASS (" + checks + " checks passed)");
    }

    // =========================================================================
    // 1. HAZARD EXTREME LEVEL BOUNDARIES (0 to 25, invalid negative and >25)
    // =========================================================================
    private static void testHazardLevelBoundaries() throws Exception {
        System.out.println("-> Testando fronteiras extremas de níveis de perigo (0 a 25)...");

        // Perigo Nível 0 (ex.: Poço Oculto Simples)
        JSONObject lvl0Json = createBaseHazardJson("poco_oculto_lv0", "Poço Oculto Nível Zero", 0);
        KingmakerCampaignModels.CanonicalHazardModel h0 = KingmakerCampaignModels.CanonicalHazardModel.fromJson(lvl0Json);
        h0.validate(); // Deve passar sem lançar exceção
        check(h0.level == 0, "Perigo de nível 0 aceito e validado");
        JSONObject roundtrip0 = h0.toJson();
        check(roundtrip0.optInt("level", -1) == 0, "Roundtrip preserva nível 0");

        // Perigo Nível 25 (ex.: Fenda Planar Apocalíptica do Rei Lanterna)
        JSONObject lvl25Json = createBaseHazardJson("fenda_lantern_king_lv25", "Fenda Planar Apocalíptica", 25);
        KingmakerCampaignModels.CanonicalHazardModel h25 = KingmakerCampaignModels.CanonicalHazardModel.fromJson(lvl25Json);
        h25.validate(); // Deve passar sem lançar exceção
        check(h25.level == 25, "Perigo de nível 25 aceito e validado");
        JSONObject roundtrip25 = h25.toJson();
        check(roundtrip25.optInt("level", -1) == 25, "Roundtrip preserva nível 25");

        // Níveis intermediários
        int[] validLevels = {1, 5, 10, 15, 20, 24};
        for (int lvl : validLevels) {
            JSONObject json = createBaseHazardJson("hazard_lv" + lvl, "Perigo Nível " + lvl, lvl);
            KingmakerCampaignModels.CanonicalHazardModel h = KingmakerCampaignModels.CanonicalHazardModel.fromJson(json);
            h.validate();
            check(h.level == lvl, "Perigo nível intermediário " + lvl + " validado");
        }

        // Teste de rejeição: Nível -1
        try {
            JSONObject negJson = createBaseHazardJson("hazard_neg", "Perigo Negativo", -1);
            KingmakerCampaignModels.CanonicalHazardModel hNeg = KingmakerCampaignModels.CanonicalHazardModel.fromJson(negJson);
            hNeg.validate();
            check(false, "Nível -1 deveria ter sido rejeitado");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("fora da faixa PF2e"), "Nível -1 rejeitado com sucesso");
        }

        // Teste de rejeição: Nível 26
        try {
            JSONObject highJson = createBaseHazardJson("hazard_high", "Perigo Alto Demais", 26);
            KingmakerCampaignModels.CanonicalHazardModel hHigh = KingmakerCampaignModels.CanonicalHazardModel.fromJson(highJson);
            hHigh.validate();
            check(false, "Nível 26 deveria ter sido rejeitado");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("fora da faixa PF2e"), "Nível 26 rejeitado com sucesso");
        }

        // Teste de rejeição: Níveis extremos (Integer.MIN_VALUE / Integer.MAX_VALUE)
        try {
            JSONObject minJson = createBaseHazardJson("hazard_min", "Perigo Min", Integer.MIN_VALUE);
            KingmakerCampaignModels.CanonicalHazardModel.fromJson(minJson).validate();
            check(false, "Integer.MIN_VALUE deveria falhar");
        } catch (IllegalArgumentException e) {
            check(true, "Integer.MIN_VALUE rejeitado");
        }

        try {
            JSONObject maxJson = createBaseHazardJson("hazard_max", "Perigo Max", Integer.MAX_VALUE);
            KingmakerCampaignModels.CanonicalHazardModel.fromJson(maxJson).validate();
            check(false, "Integer.MAX_VALUE deveria falhar");
        } catch (IllegalArgumentException e) {
            check(true, "Integer.MAX_VALUE rejeitado");
        }
    }

    // =========================================================================
    // 2. COMPLEX HAZARDS WITH ROUTINES, REACTIONS & MULTI-SKILL DISABLE
    // =========================================================================
    private static void testHazardComplexRoutineAndReactions() throws Exception {
        System.out.println("-> Testando perigos complexos com múltiplas rotinas e reações...");

        JSONObject raw = new JSONObject();
        raw.put("hazard_key", "engenho_ceifador_primeiro_mundo");
        raw.put("name", "Engenho Ceifador do Primeiro Mundo");
        raw.put("level", 18);
        raw.put("complexity", "complexo");
        raw.put("hazard_type", "armadilha");
        raw.put("stealth_dc", 38);
        raw.put("perception_dc", 36);
        raw.put("saving_throw_type", "Reflexos");
        raw.put("saving_throw_dc", 40);
        raw.put("routine_actions", 3);
        raw.put("ac", 42);
        raw.put("hardness", 28);
        raw.put("hp", 160);
        raw.put("bt", 80);
        raw.put("reaction", "Gatilho Cinético: Dispara lâmina de corte quando uma criatura cruza o feixe de luz fada.");
        raw.put("reset", "Automático após 1 hora através de fluxo de energia primal.");
        raw.put("xp", 120);
        raw.put("description", "Uma torre mecânica fada com lâminas giratórias e emissores de esporos mágicos.");

        // Traits
        JSONArray traits = new JSONArray();
        traits.put("complexo");
        traits.put("magico");
        traits.put("primal");
        raw.put("traits", traits);

        // 3 Routine Actions
        JSONArray routineArr = new JSONArray();

        JSONObject act1 = new JSONObject();
        act1.put("action_cost", 1);
        act1.put("name", "Vórtice de Lâminas");
        act1.put("description", "Ataque corpo a corpo +34 contra todas as criaturas em um raio de 4,5 metros.");
        routineArr.put(act1);

        JSONObject act2 = new JSONObject();
        act2.put("action_cost", 1);
        act2.put("name", "Emissão de Esporos Alucinógenos");
        act2.put("description", "Emana esporos em cone de 9 metros; salvaguarda de Fortitude CD 38 ou confuso por 1 rodada.");
        routineArr.put(act2);

        JSONObject act3 = new JSONObject();
        act3.put("action_cost", 1);
        act3.put("name", "Pulso de Energia Primal");
        act3.put("description", "Restaura 15 PV do perigo e eletrifica o piso do aposento.");
        routineArr.put(act3);

        raw.put("routine", routineArr);

        // Disable skills
        JSONArray disArr = new JSONArray();
        disArr.put(new JSONObject().put("skill", "Ladroagem").put("dc", 42).put("proficiency", "lendario").put("description", "Desengatar o pistão principal"));
        disArr.put(new JSONObject().put("skill", "Natureza").put("dc", 44).put("proficiency", "mestre").put("description", "Apaziguar as vinhas condutoras"));
        raw.put("disable_skills", disArr);

        KingmakerCampaignModels.CanonicalHazardModel h = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw);
        h.validate();

        check("complexo".equals(h.complexity), "Complexidade complexo mantida");
        check(h.routineActions == 3, "routineActions == 3");
        check(h.routine.size() == 3, "Rotina contém 3 ações");
        check(h.routine.get(0).actionCost == 1, "Ação 1 custo 1");
        check("Vórtice de Lâminas".equals(h.routine.get(0).name), "Ação 1 nome correto");
        check("Emissão de Esporos Alucinógenos".equals(h.routine.get(1).name), "Ação 2 nome correto");
        check("Pulso de Energia Primal".equals(h.routine.get(2).name), "Ação 3 nome correto");
        check(h.reaction.contains("Gatilho Cinético"), "Reação preservada");
        check(h.reset.contains("Automático após 1 hora"), "Reset preservado");
        check(h.ac == 42, "CA 42 preservada");
        check(h.hardness == 28, "Dureza 28 preservada");
        check(h.hp == 160, "PV 160 preservado");
        check(h.bt == 80, "Liar de Quebra (BT) 80 preservado");

        // Roundtrip check
        JSONObject roundtrip = h.toJson();
        KingmakerCampaignModels.CanonicalHazardModel hRound = KingmakerCampaignModels.CanonicalHazardModel.fromJson(roundtrip);
        hRound.validate();
        check(hRound.routine.size() == 3, "Roundtrip preservou 3 ações na rotina");
        check(hRound.routine.get(1).description.contains("cone de 9 metros"), "Roundtrip preservou descrição da ação 2");
        check(hRound.disableSkills.size() == 2, "Roundtrip preservou 2 opções de desativação");
    }

    private static void testHazardMultiSkillDisableOptions() throws Exception {
        System.out.println("-> Testando perigo com desativação multi-perícia (4 perícias alternativas)...");

        JSONObject raw = createBaseHazardJson("armadilha_relicario_vordakai", "Glifo Guardião de Vordakai", 10);
        JSONArray disArr = new JSONArray();

        disArr.put(new JSONObject().put("skill", "Ladroagem").put("dc", 30).put("proficiency", "mestre")
                .put("description", "Raspar as incrustações de pedra de sangue sem romper o condutor."));
        disArr.put(new JSONObject().put("skill", "Arcanismo").put("dc", 32).put("proficiency", "mestre")
                .put("description", "Reverter o fluxo necromântico nos caracteres ciclopianos."));
        disArr.put(new JSONObject().put("skill", "Religião").put("dc", 31).put("proficiency", "especialista")
                .put("description", "Recitar preces de purificação a Pharasma para anular a maldição."));
        disArr.put(new JSONObject().put("skill", "Atletismo").put("dc", 34).put("proficiency", "especialista")
                .put("description", "Deslocar o bloco de sustentação de 500kg para sufocar as runas."));

        raw.put("disable_skills", disArr);

        KingmakerCampaignModels.CanonicalHazardModel h = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw);
        h.validate();

        check(h.disableSkills.size() == 4, "4 opções de desativação carregadas");
        check("Ladroagem".equals(h.disableSkills.get(0).skill) && h.disableSkills.get(0).dc == 30, "Opção 1 Ladroagem CD 30");
        check("Arcanismo".equals(h.disableSkills.get(1).skill) && h.disableSkills.get(1).dc == 32, "Opção 2 Arcanismo CD 32");
        check("Religião".equals(h.disableSkills.get(2).skill) && h.disableSkills.get(2).dc == 31, "Opção 3 Religião CD 31");
        check("Atletismo".equals(h.disableSkills.get(3).skill) && h.disableSkills.get(3).dc == 34, "Opção 4 Atletismo CD 34");

        JSONObject serialized = h.toJson();
        JSONArray outDis = serialized.optJSONArray("disable_skills");
        check(outDis != null && outDis.length() == 4, "Serialização preservou 4 opções de desativação");
    }

    private static void testHazardEdgeFallbacksAndSlugs() throws Exception {
        System.out.println("-> Testando fallbacks de compatibilidade e auto-slugging de perigos...");

        // 1. Auto-slugging de hazard_key a partir do nome se key não for fornecido
        JSONObject raw1 = new JSONObject();
        raw1.put("name", "Armadilha de Espinhos Mágica de Gorum");
        raw1.put("level", 4);
        raw1.put("stealth_dc", 21);
        raw1.put("saving_throw_dc", 20);
        raw1.put("disable_dc", 21); // legada
        KingmakerCampaignModels.CanonicalHazardModel h1 = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw1);
        h1.validate();

        check(h1.hazardKey.startsWith("hazard_armadilha_de_espinhos_magica_de_gorum"), "Auto-slugging gerou chave correta");
        check(h1.disableSkills.size() == 1, "Fallback de disable_dc legada gerou 1 opção");
        check("Ladroagem".equals(h1.disableSkills.get(0).skill), "Fallback usou perícia Ladroagem");
        check(h1.disableSkills.get(0).dc == 21, "Fallback preservou CD 21");

        // 2. Auto-detecção de complexidade a partir dos traits quando complexity não está explícito
        JSONObject raw2 = createBaseHazardJson("hazard_traits_complex", "Armadilha Trait Complexa", 6);
        JSONArray traits = new JSONArray();
        traits.put("magico");
        traits.put("complexo");
        raw2.put("traits", traits);

        KingmakerCampaignModels.CanonicalHazardModel h2 = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw2);
        check("complexo".equals(h2.complexity), "Detectou complexo via traits");

        // 3. Fallback de BT = HP / 2
        JSONObject raw3 = createBaseHazardJson("hazard_fallback_bt", "Armadilha Fallback BT", 6);
        raw3.put("hp", 50);
        KingmakerCampaignModels.CanonicalHazardModel h3 = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw3);
        check(h3.bt == 25, "BT calculado automaticamente como metade do HP (50 / 2 = 25)");
    }

    // =========================================================================
    // 3. DEEP DIALOGUE BRANCHING & MISSING NPC ATTITUDE VALUES
    // =========================================================================
    private static void testNpcMissingAndCorruptAttitudes() throws Exception {
        System.out.println("-> Testando valores ausentes, nulos e corrompidos de atitude de NPC...");

        // 1. Atitude ausente no JSON: padrão é "indiferente"
        JSONObject noAtt = new JSONObject();
        noAtt.put("name", "Kressle");
        KingmakerCampaignModels.NpcModel n1 = KingmakerCampaignModels.NpcModel.fromJson(noAtt);
        check("indiferente".equals(n1.attitude), "Atitude ausente assume 'indiferente'");

        // 2. Atitude vazia: avanço seguro sem travar
        n1.attitude = "";
        n1.advanceAttitude(1); // deve usar indiferente (rank 2) + 1 = amistoso (rank 3)
        check("amistoso".equals(n1.attitude), "Atitude vazia recupera para patamar válido");

        // 3. Atitude inválida/desconhecida (ex.: 'furioso', 'desconhecido')
        n1.attitude = "furioso_com_o_bando";
        n1.advanceAttitude(1);
        check("amistoso".equals(n1.attitude), "Atitude inválida cai para indiferente e avança com segurança");

        // 4. Clamping nos extremos:
        // Limite inferior (hostil, rank 0)
        n1.attitude = "hostil";
        n1.advanceAttitude(-10);
        check("hostil".equals(n1.attitude), "Clamping inferior impede sair de 'hostil'");

        // Limite superior (prestativo, rank 4)
        n1.attitude = "prestativo";
        n1.advanceAttitude(10);
        check("prestativo".equals(n1.attitude), "Clamping superior impede sair de 'prestativo'");

        // 5. Escala completa passo a passo (+1 e -1)
        n1.attitude = "hostil";
        n1.advanceAttitude(1);
        check("desfavoravel".equals(n1.attitude), "hostil + 1 = desfavoravel");
        n1.advanceAttitude(1);
        check("indiferente".equals(n1.attitude), "desfavoravel + 1 = indiferente");
        n1.advanceAttitude(1);
        check("amistoso".equals(n1.attitude), "indiferente + 1 = amistoso");
        n1.advanceAttitude(1);
        check("prestativo".equals(n1.attitude), "amistoso + 1 = prestativo");
        n1.advanceAttitude(-2);
        check("indiferente".equals(n1.attitude), "prestativo - 2 = indiferente");
    }

    private static void testNpcDeepDialogueTreeBranching() throws Exception {
        System.out.println("-> Testando árvore profunda de diálogos ramificados com múltiplos tópicos e requisitos...");

        JSONObject rawNpc = new JSONObject();
        rawNpc.put("npc_key", "npc_bokken");
        rawNpc.put("name", "Bokken, o Eremita");
        rawNpc.put("attitude", "indiferente");
        rawNpc.put("summary", "Alquimista eremita vivendo nas Terras Roubadas.");

        JSONArray tree = new JSONArray();

        // Camada 1: Saudações por atitude
        tree.put(createDialogueNode("hostil", "Sumam daqui antes que eu jogue um frasco de fogo alquímico na cara de vocês!", "saudacao", ""));
        tree.put(createDialogueNode("desfavoravel", "O que vocês querem? Não tenho paciência para aventureiros idiotas.", "saudacao", ""));
        tree.put(createDialogueNode("indiferente", "Se vieram comprar poções, o dinheiro fala primeiro.", "saudacao", ""));
        tree.put(createDialogueNode("amistoso", "Ah, os novos governantes. Tenho alguns ingredientes interessantes hoje.", "saudacao", ""));
        tree.put(createDialogueNode("prestativo", "Meus amigos! Pensei em uma fórmula lendária que só vocês podem me ajudar a destilar.", "saudacao", "quest_pocao_perfeita"));

        // Camada 2: Tópicos de rumores e lore
        tree.put(createDialogueNode("indiferente", "O Lorde Cervo? Um bêbado violento. Não chegue perto da fortaleza dele desarmado.", "rumor_cervo", "quest_derrotar_lorde_cervo"));
        tree.put(createDialogueNode("amistoso", "O Velho Sicômoro ao sul é o lar de ácaros e kobolds. Eles vivem em guerra eterna por uma relíquia imbecil.", "rumor_sicomoro", "quest_velho_sicomoro"));
        tree.put(createDialogueNode("amistoso", "As bagas de presa de presas só crescem onde os rastejadores de carniça fazem ninhos. Preciso delas intactas.", "ervas", "quest_ervas_bokken"));

        // Camada 3: Tópicos profundos desbloqueáveis apenas com atitude prestativa
        tree.put(createDialogueNode("prestativo", "A verdade sobre meu irmão... ele me odiava. Tentou me queimar vivo antes de fugir para os pântanos.", "passado_irmão", "quest_tragedia_familiar"));
        tree.put(createDialogueNode("prestativo", "Se encontrarem o Templo do Alce, purifiquem a água. Uma maldição antiga corrompe a terra ao redor.", "templo_alce", "quest_purificar_templo"));

        rawNpc.put("dialogue_tree", tree);

        KingmakerCampaignModels.NpcModel npc = KingmakerCampaignModels.NpcModel.fromJson(rawNpc);

        check(npc.dialogueTree.size() == 10, "Árvore profunda contém 10 nós de diálogo");

        // Simulação de filtragem de diálogo pelo patamar de atitude do jogador
        // Se atitude for indiferente, nós com req 'amistoso' e 'prestativo' devem ficar bloqueados
        List<KingmakerCampaignModels.NpcDialogueNode> accessibleIndiferente = filterDialogueByAttitude(npc.dialogueTree, "indiferente");
        check(accessibleIndiferente.size() == 4, "Apenas 4 nós acessíveis no patamar indiferente (hostil, desfavorável, indiferente)");

        // Se atitude for amistoso, nós com req 'amistoso' desbloqueiam
        List<KingmakerCampaignModels.NpcDialogueNode> accessibleAmistoso = filterDialogueByAttitude(npc.dialogueTree, "amistoso");
        check(accessibleAmistoso.size() == 7, "7 nós acessíveis no patamar amistoso");

        // Se atitude for prestativo, todos os 10 nós desbloqueiam
        List<KingmakerCampaignModels.NpcDialogueNode> accessiblePrestativo = filterDialogueByAttitude(npc.dialogueTree, "prestativo");
        check(accessiblePrestativo.size() == 10, "Todos os 10 nós acessíveis no patamar prestativo");

        // Roundtrip da árvore inteira
        JSONObject roundtrip = npc.toJson();
        KingmakerCampaignModels.NpcModel npcRound = KingmakerCampaignModels.NpcModel.fromJson(roundtrip);
        check(npcRound.dialogueTree.size() == 10, "Roundtrip preservou todos os 10 nós de diálogo");
        check("quest_pocao_perfeita".equals(npcRound.dialogueTree.get(4).questKey), "Roundtrip preservou questKey");
    }

    private static List<KingmakerCampaignModels.NpcDialogueNode> filterDialogueByAttitude(List<KingmakerCampaignModels.NpcDialogueNode> nodes, String playerAttitude) {
        int playerRank = getAttitudeRank(playerAttitude);
        List<KingmakerCampaignModels.NpcDialogueNode> result = new ArrayList<KingmakerCampaignModels.NpcDialogueNode>();
        for (KingmakerCampaignModels.NpcDialogueNode node : nodes) {
            int nodeRank = getAttitudeRank(node.attitudeReq);
            if (nodeRank <= playerRank) {
                result.add(node);
            }
        }
        return result;
    }

    private static int getAttitudeRank(String att) {
        String[] ranks = KingmakerCampaignModels.NpcModel.ATTITUDE_RANKS;
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i].equalsIgnoreCase(att)) return i;
        }
        return 2; // indiferente default
    }

    // =========================================================================
    // 4. VERIFY ALL 17 MODEL CLASSES PARSE CLEANLY IN PURE JVM DESKTOP MODE
    // =========================================================================
    private static void testAllSeventeenModelClassesPureJvm() throws Exception {
        System.out.println("-> Verificando parsing limpo e roundtrip para todas as 17 classes de modelo...");

        // 1. ChapterCatalog
        KingmakerCampaignModels.ChapterCatalog cat = new KingmakerCampaignModels.ChapterCatalog();
        cat.chapterKey = "ch_test";
        cat.title = "Capítulo Teste";
        check(cat.toJson() != null, "ChapterCatalog toJson ok");
        check(KingmakerCampaignModels.ChapterCatalog.fromJson(new JSONObject()).schema.contains("chapter"), "ChapterCatalog fromJson vazio ok");

        // 2. QuestModel
        KingmakerCampaignModels.QuestModel q = new KingmakerCampaignModels.QuestModel();
        q.questKey = "q_test";
        check(KingmakerCampaignModels.QuestModel.fromJson(null) != null, "QuestModel null safe");
        check(KingmakerCampaignModels.QuestModel.fromJson(new JSONObject()).status.equals("unstarted"), "QuestModel default unstarted");

        // 3. QuestObjective
        KingmakerCampaignModels.QuestObjective obj = new KingmakerCampaignModels.QuestObjective("obj_1", "Derrotar o monstro");
        check("obj_1".equals(obj.id) && "Derrotar o monstro".equals(obj.desc), "QuestObjective construtor ok");
        check(KingmakerCampaignModels.QuestObjective.fromJson(null) != null, "QuestObjective null safe");

        // 4. HexPoiModel
        KingmakerCampaignModels.HexPoiModel poi = new KingmakerCampaignModels.HexPoiModel();
        poi.poiId = "poi_1";
        poi.hexId = "H_01";
        poi.name = "Torre";
        check(KingmakerCampaignModels.HexPoiModel.fromJson(null) != null, "HexPoiModel null safe");

        // 5. TacticalSceneModel
        KingmakerCampaignModels.TacticalSceneModel scene = new KingmakerCampaignModels.TacticalSceneModel();
        scene.sceneKey = "sc_1";
        check(KingmakerCampaignModels.TacticalSceneModel.fromJson(null) != null, "TacticalSceneModel null safe");

        // 6. SceneZone
        KingmakerCampaignModels.SceneZone zone = new KingmakerCampaignModels.SceneZone();
        zone.zoneKey = "z_1";
        check(KingmakerCampaignModels.SceneZone.fromJson(null) != null, "SceneZone null safe");

        // 7. SceneObstacle
        KingmakerCampaignModels.SceneObstacle obs = new KingmakerCampaignModels.SceneObstacle();
        obs.obstacleKey = "o_1";
        check(KingmakerCampaignModels.SceneObstacle.fromJson(null) != null, "SceneObstacle null safe");

        // 8. SceneEntryPoint
        KingmakerCampaignModels.SceneEntryPoint ep = new KingmakerCampaignModels.SceneEntryPoint();
        ep.pointKey = "ep_1";
        check(KingmakerCampaignModels.SceneEntryPoint.fromJson(null) != null, "SceneEntryPoint null safe");

        // 9. NpcModel
        KingmakerCampaignModels.NpcModel npc = new KingmakerCampaignModels.NpcModel();
        check(KingmakerCampaignModels.NpcModel.fromJson(null) != null, "NpcModel null safe");

        // 10. NpcDialogueNode
        KingmakerCampaignModels.NpcDialogueNode dNode = new KingmakerCampaignModels.NpcDialogueNode();
        check(KingmakerCampaignModels.NpcDialogueNode.fromJson(null) != null, "NpcDialogueNode null safe");

        // 11. CanonicalHazardModel
        KingmakerCampaignModels.CanonicalHazardModel haz = new KingmakerCampaignModels.CanonicalHazardModel();
        check(KingmakerCampaignModels.CanonicalHazardModel.fromJson(null) != null, "CanonicalHazardModel null safe");

        // 12. HazardDisableOption
        KingmakerCampaignModels.HazardDisableOption dis = new KingmakerCampaignModels.HazardDisableOption("Ladroagem", 20, "treinado");
        check("Ladroagem".equals(dis.skill) && dis.dc == 20, "HazardDisableOption ok");
        check(KingmakerCampaignModels.HazardDisableOption.fromJson(null) != null, "HazardDisableOption null safe");

        // 13. HazardRoutineAction
        KingmakerCampaignModels.HazardRoutineAction rAct = new KingmakerCampaignModels.HazardRoutineAction();
        check(KingmakerCampaignModels.HazardRoutineAction.fromJson(null) != null, "HazardRoutineAction null safe");

        // 14. FactionModel
        KingmakerCampaignModels.FactionModel fac = new KingmakerCampaignModels.FactionModel();
        fac.name = "Senhores das Espadas Aldori";
        check(KingmakerCampaignModels.FactionModel.fromJson(null) != null, "FactionModel null safe");
        JSONObject fJson = fac.toJson();
        check("Senhores das Espadas Aldori".equals(KingmakerCampaignModels.FactionModel.fromJson(fJson).name), "FactionModel roundtrip");

        // 15. WorldClockModel
        KingmakerCampaignModels.WorldClockModel clk = new KingmakerCampaignModels.WorldClockModel();
        clk.name = "Relógio de Invasão dos Trolls";
        clk.maxValue = 6;
        clk.currentValue = 2;
        check(KingmakerCampaignModels.WorldClockModel.fromJson(null) != null, "WorldClockModel null safe");
        JSONObject cJson = clk.toJson();
        KingmakerCampaignModels.WorldClockModel clkRound = KingmakerCampaignModels.WorldClockModel.fromJson(cJson);
        check(clkRound.maxValue == 6 && clkRound.currentValue == 2, "WorldClockModel roundtrip");

        // 16. WorldEventModel
        KingmakerCampaignModels.WorldEventModel evt = new KingmakerCampaignModels.WorldEventModel();
        evt.title = "Festival de Rushlight";
        evt.dueDay = 45;
        check(KingmakerCampaignModels.WorldEventModel.fromJson(null) != null, "WorldEventModel null safe");
        JSONObject eJson = evt.toJson();
        KingmakerCampaignModels.WorldEventModel evtRound = KingmakerCampaignModels.WorldEventModel.fromJson(eJson);
        check(evtRound.dueDay == 45 && "Festival de Rushlight".equals(evtRound.title), "WorldEventModel roundtrip");

        // 17. CampaignMilestone
        KingmakerCampaignModels.CampaignMilestone mls = new KingmakerCampaignModels.CampaignMilestone();
        mls.milestoneKey = "m_coroacao";
        mls.name = "Coroação do Barão";
        mls.xp = 1200;
        check(KingmakerCampaignModels.CampaignMilestone.fromJson(null) != null, "CampaignMilestone null safe");
        JSONObject mJson = mls.toJson();
        KingmakerCampaignModels.CampaignMilestone mlsRound = KingmakerCampaignModels.CampaignMilestone.fromJson(mJson);
        check(mlsRound.xp == 1200 && "Coroação do Barão".equals(mlsRound.name), "CampaignMilestone roundtrip");
    }

    // =========================================================================
    // 5. QUEST STATE TRANSITIONS & PREREQUISITES
    // =========================================================================
    private static void testQuestStateTransitionsAndPrerequisites() throws Exception {
        System.out.println("-> Testando transições de estado de missões e pré-requisitos...");

        KingmakerCampaignModels.QuestModel q = new KingmakerCampaignModels.QuestModel();
        q.questKey = "q_derrotar_lorde_cervo";
        q.status = "unstarted";
        q.prerequisites.add("q_proteger_oleg");

        // 1. Ativar sem completar pré-requisito deve falhar
        try {
            q.transitionTo("active", "q_outra_coisa");
            check(false, "Deveria ter lançado IllegalStateException por pré-requisito ausente");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("Pré-requisito não satisfeito"), "Pré-requisito não satisfeito bloqueou ativação");
        }

        // 2. Ativar com pré-requisito satisfeito
        q.transitionTo("active", "q_proteger_oleg");
        check("active".equals(q.status), "Transição para active com pré-requisito bem-sucedida");

        // 3. Completar a quest
        q.transitionTo("completed", null);
        check("completed".equals(q.status), "Transição para completed bem-sucedida");

        // 4. Tentar mudar estado de quest já concluída deve falhar
        try {
            q.transitionTo("active", null);
            check(false, "Deveria ter lançado IllegalStateException para quest concluída");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("já concluída"), "Quest concluída não pode mudar de estado");
        }
    }

    // =========================================================================
    // 6. HEX POI CONSTRAINTS & NORMALIZATIONS
    // =========================================================================
    private static void testHexPoiConstraintsAndNormalizations() throws Exception {
        System.out.println("-> Testando invariantes rigorosas de HexPoiModel (canonical=0, status, source)...");

        // Invariante canonical=0 obrigatória
        KingmakerCampaignModels.HexPoiModel p = new KingmakerCampaignModels.HexPoiModel();
        p.poiId = "poi_sycamore";
        p.name = "O Velho Sicômoro";
        p.hexId = "H_04";
        p.canonical = 0;
        p.source = "campaign";
        p.validate();
        check(true, "HexPoi com canonical=0 validado com sucesso");

        try {
            p.canonical = 1;
            p.validate();
            check(false, "canonical=1 deveria ser rejeitado pelo modelo");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("canonical deve ser 0"), "canonical=1 rejeitado com sucesso");
        }
        p.canonical = 0;

        // Source deve ser 'campaign' ou 'procedural'
        try {
            p.source = "homebrew_invalido";
            p.validate();
            check(false, "source inválido deveria ser rejeitado");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("source inválido"), "source inválido rejeitado");
        }

        // Normalização de status legado: 'unexplored' vira 'active' com discovered = false
        JSONObject rawUnexp = new JSONObject();
        rawUnexp.put("poi_id", "p_unexp");
        rawUnexp.put("name", "Toca Secreta");
        rawUnexp.put("status", "unexplored");
        KingmakerCampaignModels.HexPoiModel pUnexp = KingmakerCampaignModels.HexPoiModel.fromJson(rawUnexp);
        check("active".equals(pUnexp.status) && !pUnexp.discovered, "unexplored mapeado para status 'active' com discovered=false");
    }

    // =========================================================================
    // 7. CHAPTER CATALOG EXTREME LEVEL RANGES
    // =========================================================================
    private static void testChapterCatalogExtremeLevelRanges() throws Exception {
        System.out.println("-> Testando faixas de níveis em catálogos de capítulos...");

        KingmakerCampaignModels.ChapterCatalog cat = new KingmakerCampaignModels.ChapterCatalog();
        cat.chapterKey = "chapter_01";
        cat.title = "Terras Roubadas";
        cat.levelMin = 1;
        cat.levelMax = 20;
        cat.validate();
        check(true, "Capítulo com nível 1 a 20 validado com sucesso");

        // levelMin < 1 deve falhar
        try {
            cat.levelMin = 0;
            cat.validate();
            check(false, "levelMin 0 deveria falhar");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("Faixa de níveis inválida"), "levelMin 0 rejeitado");
        }
        cat.levelMin = 1;

        // levelMax > 20 deve falhar
        try {
            cat.levelMax = 21;
            cat.validate();
            check(false, "levelMax 21 deveria falhar");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("Faixa de níveis inválida"), "levelMax 21 rejeitado");
        }
        cat.levelMax = 20;

        // levelMin > levelMax deve falhar
        try {
            cat.levelMin = 5;
            cat.levelMax = 4;
            cat.validate();
            check(false, "levelMin > levelMax deveria falhar");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("Faixa de níveis inválida"), "levelMin > levelMax rejeitado");
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private static JSONObject createBaseHazardJson(String key, String name, int level) throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("hazard_key", key);
        raw.put("name", name);
        raw.put("level", level);
        raw.put("stealth_dc", 15 + level);
        raw.put("saving_throw_dc", 14 + level);
        raw.put("saving_throw_type", "Reflexos");
        JSONArray dis = new JSONArray();
        dis.put(new JSONObject().put("skill", "Ladroagem").put("dc", 15 + level).put("proficiency", "treinado"));
        raw.put("disable_skills", dis);
        return raw;
    }

    private static JSONObject createDialogueNode(String attitude, String text, String topic, String questKey) throws Exception {
        JSONObject d = new JSONObject();
        d.put("attitude_req", attitude);
        d.put("text", text);
        d.put("topic", topic);
        d.put("quest_key", questKey);
        return d;
    }
}
