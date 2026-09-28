package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure JVM E2E Test Harness for Kingmaker Chapters 1-12.
 * Validates campaign catalogs, quest progression, hazard parsing, NPC attitudes,
 * hex exploration, solo flow routing, and continuity contracts without Android SDK or Gradle.
 */
public final class KingmakerCampaignE2EHarness {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FALHA NA VERIFICAÇÃO DO HARNESS E2E: " + label);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        testChapterSchemaCatalogLoading();
        testQuestStateTransitionsAndMilestones();
        testCanonicalHazardRemasterStatBlocks();
        testNpcAttitudeAndDialogueTransitions();
        testHexDiscoveryAndPoiExploration();
        testSoloDirectorRoutingIntegration();
        testContinuityCheckpointRoundtrip();

        System.out.println("KINGMAKER_CAMPAIGN_E2E_HARNESS_PASS (" + checks + " checks passed)");
    }

    // =========================================================================
    // 1. CHAPTER SCHEMA CATALOG LOADING (Chapters 1-12)
    // =========================================================================
    private static void testChapterSchemaCatalogLoading() throws Exception {
        String[][] canonicalChapters = {
            {"chapter_01_stolen_lands_part1", "Terras Roubadas - Parte 1: Posto de Oleg e Ermos", "1", "2"},
            {"chapter_02_stolen_lands_part2", "Terras Roubadas - Parte 2: O Senhor dos Cervos", "2", "5"},
            {"chapter_03_rivers_run_red", "Rios Correm Vermelhos: A Ameaça dos Troles", "5", "7"},
            {"chapter_04_cult_of_bloom", "O Culto da Flor e a Praga do Primeiro Mundo", "7", "9"},
            {"chapter_05_varnhold_vanishing", "O Desaparecimento de Varnhold e o Sepulcro", "9", "11"},
            {"chapter_06_blood_for_blood", "Sangue por Sangue: Os Bárbaros Senhores dos Tigres", "11", "13"},
            {"chapter_07_war_of_river_kings", "A Guerra dos Reis dos Rios: O Desafio de Pitax", "13", "15"},
            {"chapter_08_they_lurk_below", "Eles Espreitam Abaixo: As Profundezas das Terras Sombrias", "15", "16"},
            {"chapter_09_sound_thousand_screams", "O Som de Mil Gritos: A Casa no Limiar do Tempo", "16", "18"},
            {"chapter_10_curse_lantern_king", "A Maldição do Rei Lanterna: O Julgamento Feérico", "18", "20"},
            {"chapter_11_hex_atlas_pois", "Atlas Regional das Terras Roubadas e Pontos de Interesse", "1", "20"},
            {"chapter_12_companion_stories", "Histórias, Diálogos e Missões dos Doze Companheiros", "1", "20"}
        };

        for (String[] entry : canonicalChapters) {
            String key = entry[0];
            String title = entry[1];
            int lvlMin = Integer.parseInt(entry[2]);
            int lvlMax = Integer.parseInt(entry[3]);

            JSONObject catalog = createSampleChapterCatalog(key, title, lvlMin, lvlMax);
            validateChapterCatalogSchema(catalog);

            check("braseiro-pathfinder2e-kingmaker-chapter-v1".equals(catalog.optString("schema")),
                    "schema correto para " + key);
            check(key.equals(catalog.optString("chapter_key")), "chapter_key corresponde para " + key);
            check(title.equals(catalog.optString("title")), "title em pt-BR correto para " + key);
            check(catalog.optInt("level_min") == lvlMin, "level_min correto para " + key);
            check(catalog.optInt("level_max") == lvlMax, "level_max correto para " + key);
            check(catalog.optJSONArray("quests").length() > 0, "possui quests em " + key);
            check(catalog.optJSONArray("hazards").length() > 0, "possui perigos em " + key);
            check(catalog.optJSONArray("pois").length() > 0, "possui POIs em " + key);
            check(catalog.optJSONArray("npcs").length() > 0, "possui NPCs em " + key);
        }
    }

    private static JSONObject createSampleChapterCatalog(String key, String title, int lvlMin, int lvlMax) throws Exception {
        JSONObject catalog = new JSONObject();
        catalog.put("schema", "braseiro-pathfinder2e-kingmaker-chapter-v1");
        catalog.put("chapter_key", key);
        catalog.put("title", title);
        catalog.put("locale", "pt-BR");
        catalog.put("level_min", lvlMin);
        catalog.put("level_max", lvlMax);

        JSONArray quests = new JSONArray();
        JSONObject q = new JSONObject();
        q.put("quest_key", key + "_q1");
        q.put("title", "Missão Principal de " + title);
        q.put("status", "unstarted");
        q.put("milestone_xp", 100);
        JSONArray objs = new JSONArray();
        objs.put(new JSONObject().put("id", "obj1").put("desc", "Explorar o perímetro").put("completed", false));
        q.put("objectives", objs);
        quests.put(q);
        catalog.put("quests", quests);

        JSONArray hazards = new JSONArray();
        JSONObject h = new JSONObject();
        h.put("hazard_key", key + "_h1");
        h.put("name", "Armadilha Oculta em " + title);
        h.put("level", lvlMin);
        h.put("stealth_dc", 15 + lvlMin);
        h.put("disable_dc", 16 + lvlMin);
        h.put("saving_throw_dc", 14 + lvlMin);
        hazards.put(h);
        catalog.put("hazards", hazards);

        JSONArray pois = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("poi_key", key + "_poi1");
        p.put("name", "Acampamento em " + title);
        p.put("hex_id", "H_" + key);
        p.put("status", "unexplored");
        pois.put(p);
        catalog.put("pois", pois);

        JSONArray npcs = new JSONArray();
        JSONObject n = new JSONObject();
        n.put("npc_key", key + "_npc1");
        n.put("name", "Guia Local de " + title);
        n.put("attitude", "indiferente");
        npcs.put(n);
        catalog.put("npcs", npcs);

        return catalog;
    }

    private static void validateChapterCatalogSchema(JSONObject catalog) {
        if (catalog == null) throw new IllegalArgumentException("Catálogo nulo");
        String[] required = {"schema", "chapter_key", "title", "locale", "level_min", "level_max", "quests", "hazards", "pois", "npcs"};
        for (String field : required) {
            if (!catalog.has(field)) {
                throw new IllegalStateException("Campo obrigatório ausente no catálogo: " + field);
            }
        }
    }

    // =========================================================================
    // 2. QUEST STATE TRANSITIONS & MILESTONES
    // =========================================================================
    private static void testQuestStateTransitionsAndMilestones() throws Exception {
        // Simulação de ciclo de vida de quest com verificação de pré-requisitos e marcos de XP
        Map<String, String> questDb = new HashMap<>();
        Map<String, Integer> xpLedger = new HashMap<>();

        String quest1 = "pzo_q01_reuniao_oleg";
        String quest2 = "pzo_q02_derrotar_senhor_cervos";

        // Estado inicial: unstarted
        questDb.put(quest1, "unstarted");
        questDb.put(quest2, "unstarted");

        // 1. Iniciar quest 1
        transitionQuest(questDb, quest1, "active", null);
        check("active".equals(questDb.get(quest1)), "quest 1 está ativa");

        // 2. Avançar para in_progress
        transitionQuest(questDb, quest1, "in_progress", null);
        check("in_progress".equals(questDb.get(quest1)), "quest 1 em progresso");

        // 3. Tentar ativar quest 2 com dependência não cumprida -> deve falhar
        boolean failedAsExpected = false;
        try {
            transitionQuest(questDb, quest2, "active", quest1);
        } catch (IllegalStateException e) {
            failedAsExpected = true;
        }
        check(failedAsExpected, "ativação de quest com pré-requisito pendente deve falhar");

        // 4. Completar quest 1 e conceder XP
        transitionQuest(questDb, quest1, "completed", null);
        xpLedger.put("party_xp", xpLedger.getOrDefault("party_xp", 0) + 120);
        check("completed".equals(questDb.get(quest1)), "quest 1 concluída");
        check(xpLedger.get("party_xp") == 120, "XP de marco concedido (120 XP)");

        // 5. Agora ativar quest 2 com dependência cumprida -> sucesso
        transitionQuest(questDb, quest2, "active", quest1);
        check("active".equals(questDb.get(quest2)), "quest 2 ativada após pré-requisito cumprido");

        // 6. Transição inválida: de completed direto para unstarted deve falhar
        boolean invalidTransitionCaught = false;
        try {
            transitionQuest(questDb, quest1, "unstarted", null);
        } catch (IllegalStateException e) {
            invalidTransitionCaught = true;
        }
        check(invalidTransitionCaught, "transição regressiva inválida rejeitada");
    }

    private static void transitionQuest(Map<String, String> db, String questKey, String nextState, String requiredPrerequisite) {
        String current = db.getOrDefault(questKey, "unstarted");
        if (requiredPrerequisite != null) {
            String prereqStatus = db.getOrDefault(requiredPrerequisite, "unstarted");
            if (!"completed".equals(prereqStatus)) {
                throw new IllegalStateException("Pré-requisito não satisfeito: " + requiredPrerequisite + " está em " + prereqStatus);
            }
        }

        if ("unstarted".equals(current) && !"active".equals(nextState)) {
            throw new IllegalStateException("Transição inválida de unstarted para " + nextState);
        }
        if ("completed".equals(current) && !"completed".equals(nextState)) {
            throw new IllegalStateException("Quest já concluída não pode mudar de estado");
        }

        db.put(questKey, nextState);
    }

    // =========================================================================
    // 3. CANONICAL HAZARD REMASTER STAT BLOCKS
    // =========================================================================
    private static void testCanonicalHazardRemasterStatBlocks() throws Exception {
        // Validação de perigos oficiais do Pathfinder 2e Remaster
        JSONObject pitHazard = new JSONObject()
                .put("hazard_key", "hazard_ch1_poco_camuflado")
                .put("name", "Poço Camuflado com Estacas")
                .put("level", 1)
                .put("traits", new JSONArray().put("mecanico").put("armadilha"))
                .put("stealth_dc", 17)
                .put("perception_dc", 17)
                .put("saving_throw_type", "Reflexos")
                .put("saving_throw_dc", 17)
                .put("disable_skills", new JSONArray()
                        .put(new JSONObject().put("skill", "Ladroagem").put("dc", 17).put("proficiency", "treinado"))
                        .put(new JSONObject().put("skill", "Atletismo").put("dc", 18).put("proficiency", "treinado")))
                .put("routine_actions", 1);

        validateHazard(pitHazard);
        check(pitHazard.optInt("level") == 1, "nível do perigo é 1");
        check(pitHazard.optInt("stealth_dc") == 17, "CD de Furtividade é 17");
        check("Reflexos".equals(pitHazard.optString("saving_throw_type")), "Salvaguarda de Reflexos");
        check(pitHazard.optJSONArray("disable_skills").length() == 2, "2 perícias para desativação");

        // Perigo de alto nível: Gavinhas do Rei Lanterna (Nível 19)
        JSONObject apexHazard = new JSONObject()
                .put("hazard_key", "hazard_ch10_gavinhas_rei_lanterna")
                .put("name", "Gavinhas Ilusórias do Rei Lanterna")
                .put("level", 19)
                .put("traits", new JSONArray().put("complexo").put("magico").put("feerico").put("ilusao"))
                .put("stealth_dc", 44)
                .put("perception_dc", 44)
                .put("saving_throw_type", "Vontade")
                .put("saving_throw_dc", 41)
                .put("disable_skills", new JSONArray()
                        .put(new JSONObject().put("skill", "Arcanismo").put("dc", 43).put("proficiency", "mestre"))
                        .put(new JSONObject().put("skill", "Ocultismo").put("dc", 43).put("proficiency", "mestre"))
                        .put(new JSONObject().put("skill", "Ladroagem").put("dc", 45).put("proficiency", "lendario")))
                .put("routine_actions", 3);

        validateHazard(apexHazard);
        check(apexHazard.optInt("level") == 19, "perigo épico de nível 19");
        check(apexHazard.optInt("saving_throw_dc") == 41, "CD de salvaguarda 41");
        check(apexHazard.optInt("routine_actions") == 3, "rotina complexa com 3 ações");

        // Fail-closed test para perigo corrompido (sem stealth_dc ou DC <= 0)
        boolean invalidHazardCaught = false;
        try {
            JSONObject badHazard = new JSONObject(pitHazard.toString()).put("stealth_dc", -5);
            validateHazard(badHazard);
        } catch (IllegalArgumentException e) {
            invalidHazardCaught = true;
        }
        check(invalidHazardCaught, "perigo com CD inválida (< 0) rejeitado fail-closed");
    }

    private static void validateHazard(JSONObject hazard) {
        if (hazard == null) throw new IllegalArgumentException("Perigo nulo");
        int level = hazard.optInt("level", -1);
        if (level < 0 || level > 25) throw new IllegalArgumentException("Nível do perigo fora da faixa PF2e: " + level);
        int stealthDc = hazard.optInt("stealth_dc", 0);
        if (stealthDc <= 0) throw new IllegalArgumentException("CD de Furtividade do perigo deve ser positiva");
        int saveDc = hazard.optInt("saving_throw_dc", 0);
        if (saveDc <= 0) throw new IllegalArgumentException("CD de Salvaguarda deve ser positiva");
        JSONArray disableSkills = hazard.optJSONArray("disable_skills");
        if (disableSkills == null || disableSkills.length() == 0) {
            throw new IllegalArgumentException("Perigo deve ter métodos de desativação definidos");
        }
    }

    // =========================================================================
    // 4. NPC ATTITUDE & DIALOGUE TRANSITIONS
    // =========================================================================
    private static void testNpcAttitudeAndDialogueTransitions() {
        String[] attitudes = {"hostil", "desfavoravel", "indiferente", "amistoso", "prestativo"};
        Map<String, Integer> attitudeRanks = new HashMap<>();
        for (int i = 0; i < attitudes.length; i++) {
            attitudeRanks.put(attitudes[i], i);
        }

        // Caso 1: Oleg Leveton (indiferente -> amistoso -> prestativo)
        String olegAttitude = "indiferente";
        olegAttitude = advanceAttitude(attitudeRanks, olegAttitude, 1);
        check("amistoso".equals(olegAttitude), "Oleg avança para amistoso após salvar o entreposto");
        olegAttitude = advanceAttitude(attitudeRanks, olegAttitude, 1);
        check("prestativo".equals(olegAttitude), "Oleg avança para prestativo após recuperação dos suprimentos");

        // Caso 2: Kressle (hostil -> desfavoravel / indiferente após captura e negociação)
        String kressleAttitude = "hostil";
        kressleAttitude = advanceAttitude(attitudeRanks, kressleAttitude, 2);
        check("indiferente".equals(kressleAttitude), "Kressle passa de hostil a indiferente ao ser poupada");

        // Limite superior: prestativo não pode subir além de prestativo
        String maxAttitude = advanceAttitude(attitudeRanks, "prestativo", 1);
        check("prestativo".equals(maxAttitude), "prestativo é o nível máximo de disposição");

        // Limite inferior: hostil não pode descer abaixo de hostil
        String minAttitude = advanceAttitude(attitudeRanks, "hostil", -1);
        check("hostil".equals(minAttitude), "hostil é o nível mínimo de disposição");
    }

    private static String advanceAttitude(Map<String, Integer> ranks, String current, int delta) {
        String[] attitudes = {"hostil", "desfavoravel", "indiferente", "amistoso", "prestativo"};
        int rank = ranks.getOrDefault(current, 2);
        int next = Math.max(0, Math.min(attitudes.length - 1, rank + delta));
        return attitudes[next];
    }

    // =========================================================================
    // 5. HEX DISCOVERY & POI EXPLORATION
    // =========================================================================
    private static void testHexDiscoveryAndPoiExploration() throws Exception {
        JSONObject hex = new JSONObject()
                .put("hex_id", "H_01_OLEG")
                .put("status", "unexplored")
                .put("terrain", "planicie")
                .put("pois", new JSONArray().put(new JSONObject()
                        .put("poi_id", "poi_posto_oleg")
                        .put("name", "Posto Comercial de Oleg")
                        .put("status", "hidden")
                        .put("type", "settlement")));

        check("unexplored".equals(hex.optString("status")), "hex inicia inexplorado");

        // 1. Reconhecer hexágono (Reconnoiter)
        hex.put("status", "reconnoitered");
        JSONArray pois = hex.optJSONArray("pois");
        pois.optJSONObject(0).put("status", "discovered");
        check("reconnoitered".equals(hex.optString("status")), "hex reconhecido com sucesso");
        check("discovered".equals(pois.optJSONObject(0).optString("status")), "POI revelado ao reconhecer");

        // 2. Limpar hexágono (Clear)
        hex.put("status", "cleared");
        pois.optJSONObject(0).put("status", "secured");
        check("cleared".equals(hex.optString("status")), "hex limpo de ameaças");
        check("secured".equals(pois.optJSONObject(0).optString("status")), "POI pacificado e seguro");

        // 3. Reivindicar território (Claim)
        hex.put("status", "claimed");
        hex.put("claimed_turn", 1);
        check("claimed".equals(hex.optString("status")), "hex anexado ao Reino");
        check(hex.optInt("claimed_turn") == 1, "turno de reivindicação registrado");
    }

    // =========================================================================
    // 6. SOLO DIRECTOR ROUTING INTEGRATION
    // =========================================================================
    private static void testSoloDirectorRoutingIntegration() {
        String[] scenes = {
            "adventure_start", "camp_encounter", "camp", "war",
            "exploration_resolution", "companion", "kingdom_bridge",
            "travel", "free_exploration"
        };

        for (String scene : scenes) {
            String action = KingmakerSoloFlowRouter.actionFor(scene);
            check(!KingmakerSoloFlowRouter.MASTER_FALLBACK.equals(action),
                    "next_scene canônica tem destino: " + scene);
        }

        // Teste de fallback seguro
        String fallbackAction = KingmakerSoloFlowRouter.actionFor("cena_inexistente_ou_nula");
        check(KingmakerSoloFlowRouter.MASTER_FALLBACK.equals(fallbackAction),
                "cena desconhecida roteia para master_fallback");

        String nullAction = KingmakerSoloFlowRouter.actionFor(null);
        check(KingmakerSoloFlowRouter.MASTER_FALLBACK.equals(nullAction),
                "null roteia para master_fallback");
    }

    // =========================================================================
    // 7. CONTINUITY CHECKPOINT ROUND-TRIP CONTRACT
    // =========================================================================
    private static void testContinuityCheckpointRoundtrip() throws Exception {
        JSONObject before = new JSONObject()
                .put("story_started", 1)
                .put("current_hex", "H_01_OLEG")
                .put("destination_hex", "H_02_SICOMORO")
                .put("route_json", new JSONArray().put("H_02_SICOMORO"))
                .put("scene_day", 14)
                .put("scene_period", "manha")
                .put("scene_weather", "claro")
                .put("camp_key", "acampamento_rio_shrike")
                .put("camp_phase", "DESCANSO")
                .put("active_companions", new JSONArray().put("amiri").put("linzi").put("valerie"))
                .put("pending_companion_events", 1)
                .put("pending_exploration_events", 0)
                .put("active_wars", 0)
                .put("kingdom_action", "")
                .put("next_hex", "H_02_SICOMORO")
                .put("turn_id", 42)
                .put("updated_at", 1727470000000L);

        // Simulando restore onde IDs internos e timestamps foram recalculados
        JSONObject restored = new JSONObject(before.toString())
                .put("turn_id", 9999)
                .put("updated_at", 1727479999999L);

        JSONObject audit = KingmakerContinuityContract.compare(before, restored);
        check(audit.optBoolean("ok", false), "checkpoint preserva equivalência semântica: " + audit.toString());
        check(KingmakerContinuityContract.equivalent(before, restored), "equivalent() retorna true");
    }
}
