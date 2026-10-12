package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pure JVM test harness verifying KingmakerCampaignModels:
 * ChapterCatalog, QuestModel, HexPoiModel, TacticalSceneModel, NpcModel, CanonicalHazardModel.
 */
public final class KingmakerCampaignModelsHarness {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FALHA NO HARNESS DE MODELOS: " + label);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        testHazardModel();
        testQuestModel();
        testPoiModel();
        testTacticalSceneModel();
        testNpcModel();
        testChapterCatalogRoundtrip();

        System.out.println("KINGMAKER_CAMPAIGN_MODELS_HARNESS_PASS (" + checks + " checks passed)");
    }

    private static void testHazardModel() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("hazard_key", "poco_com_estacas");
        raw.put("name", "Poço Camuflado");
        raw.put("level", 2);
        raw.put("stealth_dc", 18);
        raw.put("saving_throw_dc", 17);
        raw.put("saving_throw_type", "Reflexos");
        JSONArray dis = new JSONArray();
        dis.put(new JSONObject().put("skill", "Ladroagem").put("dc", 18).put("proficiency", "treinado"));
        raw.put("disable_skills", dis);

        KingmakerCampaignModels.CanonicalHazardModel h = KingmakerCampaignModels.CanonicalHazardModel.fromJson(raw);
        h.validate();

        check("poco_com_estacas".equals(h.hazardKey), "hazardKey correto");
        check(h.level == 2, "level correto");
        check(h.stealthDc == 18, "stealthDc correto");
        check(h.savingThrowDc == 17, "savingThrowDc correto");
        check(h.disableSkills.size() == 1, "disableSkills tamanho 1");

        JSONObject roundtrip = h.toJson();
        check(roundtrip.optInt("level") == 2, "roundtrip preservou level");
    }

    private static void testQuestModel() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("quest_key", "missao_oleg");
        raw.put("title", "Proteger o Posto de Oleg");
        raw.put("status", "unstarted");
        raw.put("milestone_xp", 100);

        KingmakerCampaignModels.QuestModel q = KingmakerCampaignModels.QuestModel.fromJson(raw);
        check("unstarted".equals(q.status), "status inicial unstarted");

        q.transitionTo("active", null);
        check("active".equals(q.status), "status transicionado para active");
    }

    private static void testPoiModel() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("poi_id", "poi_posto_oleg");
        raw.put("name", "Posto de Oleg");
        raw.put("hex_id", "H_01");
        raw.put("source", "campaign");
        raw.put("canonical", 0);
        raw.put("status", "active");

        KingmakerCampaignModels.HexPoiModel p = KingmakerCampaignModels.HexPoiModel.fromJson(raw);
        p.validate();

        check("poi_posto_oleg".equals(p.poiId), "poiId correto");
        check(p.canonical == 0, "canonical é 0");
        check("campaign".equals(p.source), "source é campaign");
    }

    private static void testTacticalSceneModel() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("scene_key", "cena_posto_oleg");
        raw.put("title", "Pátio do Posto de Oleg");
        raw.put("hex_id", "H_01");
        raw.put("width_cells", 16);
        raw.put("height_cells", 12);

        KingmakerCampaignModels.TacticalSceneModel s = KingmakerCampaignModels.TacticalSceneModel.fromJson(raw);
        check("cena_posto_oleg".equals(s.sceneKey), "sceneKey correto");
        check(s.widthCells == 16, "widthCells correto");
        check(s.heightCells == 12, "heightCells correto");
    }

    private static void testNpcModel() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("npc_key", "oleg_leveton");
        raw.put("name", "Oleg Leveton");
        raw.put("attitude", "indiferente");

        KingmakerCampaignModels.NpcModel n = KingmakerCampaignModels.NpcModel.fromJson(raw);
        check("indiferente".equals(n.attitude), "attitude inicial");

        n.advanceAttitude(1);
        check("amistoso".equals(n.attitude), "attitude apos avanço (+1)");

        n.advanceAttitude(1);
        check("prestativo".equals(n.attitude), "attitude apos avanço (+1)");

        n.advanceAttitude(5);
        check("prestativo".equals(n.attitude), "attitude no limite maximo");
    }

    private static void testChapterCatalogRoundtrip() throws Exception {
        JSONObject raw = new JSONObject();
        raw.put("schema", "braseiro-pathfinder2e-kingmaker-chapter-v1");
        raw.put("chapter_key", "chapter_01_stolen_lands_part1");
        raw.put("chapter_number", 1);
        raw.put("title", "Terras Roubadas - Parte 1");
        raw.put("level_min", 1);
        raw.put("level_max", 2);
        raw.put("summary", "Prólogo da campanha Kingmaker");

        JSONArray quests = new JSONArray();
        JSONObject q = new JSONObject();
        q.put("quest_key", "q1");
        q.put("title", "Proteger Oleg");
        quests.put(q);
        raw.put("quests", quests);

        JSONArray hazards = new JSONArray();
        JSONObject h = new JSONObject();
        h.put("hazard_key", "h1");
        h.put("name", "Armadilha");
        h.put("level", 1);
        h.put("stealth_dc", 15);
        h.put("saving_throw_dc", 15);
        JSONArray dis = new JSONArray();
        dis.put(new JSONObject().put("skill", "Ladroagem").put("dc", 15).put("proficiency", "treinado"));
        h.put("disable_skills", dis);
        hazards.put(h);
        raw.put("hazards", hazards);

        JSONArray pois = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("poi_id", "p1");
        p.put("name", "Oleg");
        p.put("hex_id", "H01");
        p.put("source", "campaign");
        p.put("canonical", 0);
        pois.put(p);
        raw.put("pois", pois);

        KingmakerCampaignModels.ChapterCatalog cat = KingmakerCampaignModels.ChapterCatalog.fromJson(raw);
        cat.validate();

        check("chapter_01_stolen_lands_part1".equals(cat.chapterKey), "chapterKey correto");
        check(cat.quests.size() == 1, "quests carregadas");
        check(cat.hazards.size() == 1, "hazards carregados");
        check(cat.pois.size() == 1, "pois carregados");

        JSONObject roundtrip = cat.toJson();
        check(roundtrip.optInt("chapter_number") == 1, "roundtrip preservou chapter_number");
    }
}
