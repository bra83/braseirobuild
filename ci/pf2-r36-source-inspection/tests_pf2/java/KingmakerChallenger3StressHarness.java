package com.braseiro.pathfinder2e;

import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Challenger 3 Pure JVM Stress and Edge-Case Harness for Milestone 1.
 * Adversarially challenges:
 * 1. Cross-profile POI scoping and parent-child hierarchy across distinct world_profiles.
 * 2. Backup v22 restore contract: healthy: true on valid, healthy: false on corrupt.
 * 3. Fail-closed type hardening: strings, numbers, booleans, objects rejected with healthy: false.
 * 4. Idempotency: repeated imports in same profile do not duplicate or corrupt POIs.
 */
public final class KingmakerChallenger3StressHarness {
    private static int checksPassed = 0;

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED: " + description);
        }
        checksPassed++;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== CHALLENGER 3: INICIANDO BATERIA ADVERSARIAL COMPLETA ===");

        testCrossProfilePoiScopingAndHierarchy();
        testIdempotencyAndNoDoublePrefixing();
        testBackupV22ContractValidBackups();
        testBackupV22ContractCorruptedPayloads();
        testFailClosedNonArrayTypeHardening();

        System.out.println("=== CHALLENGER 3: SUCESSO TOTAL (" + checksPassed + " CHECKS PASSADOS COM ZERO FALHAS) ===");
    }

    private static JSONObject createSampleChapter(String chapterKey) throws JSONException {
        JSONObject cat = new JSONObject();
        cat.put("schema", "braseiro-pathfinder2e-kingmaker-chapter-v1");
        cat.put("chapter_key", chapterKey);
        cat.put("chapter_number", 1);
        cat.put("title", "Capítulo 1 - Terras Roubadas");
        cat.put("level_min", 1);
        cat.put("level_max", 2);
        cat.put("summary", "Prólogo canônico");

        JSONArray fArr = new JSONArray();
        JSONObject f = new JSONObject();
        f.put("name", "Restov Aldori");
        f.put("agenda", "Colonização");
        fArr.put(f);
        cat.put("factions", fArr);

        JSONArray qArr = new JSONArray();
        JSONObject q = new JSONObject();
        q.put("quest_key", chapterKey + "_q1");
        q.put("title", "Proteger Oleg");
        q.put("status", "unstarted");
        q.put("milestone_xp", 100);
        qArr.put(q);
        cat.put("quests", qArr);

        JSONArray pArr = new JSONArray();
        JSONObject parentPoi = new JSONObject();
        parentPoi.put("poi_id", "poi_posto_oleg");
        parentPoi.put("poi_key", "poi_posto_oleg");
        parentPoi.put("name", "Posto de Oleg");
        parentPoi.put("hex_id", "H_01");
        parentPoi.put("source", "campaign");
        parentPoi.put("canonical", 0);
        parentPoi.put("status", "active");
        pArr.put(parentPoi);

        JSONObject childPoi = new JSONObject();
        childPoi.put("poi_id", "poi_oleg_armazem");
        childPoi.put("poi_key", "poi_oleg_armazem");
        childPoi.put("name", "Armazém de Oleg");
        childPoi.put("parent_poi_id", "poi_posto_oleg");
        childPoi.put("hex_id", "H_01");
        childPoi.put("source", "campaign");
        childPoi.put("canonical", 0);
        childPoi.put("status", "active");
        pArr.put(childPoi);

        cat.put("pois", pArr);

        JSONArray hArr = new JSONArray();
        JSONObject h = new JSONObject();
        h.put("hazard_key", "armadilha_urso");
        h.put("name", "Armadilha de Urso");
        h.put("level", 1);
        h.put("stealth_dc", 15);
        h.put("saving_throw_dc", 16);
        JSONArray dis = new JSONArray();
        dis.put(new JSONObject().put("skill", "Ladroagem").put("dc", 18).put("proficiency", "treinado"));
        h.put("disable_skills", dis);
        hArr.put(h);
        cat.put("hazards", hArr);

        return cat;
    }

    private static void testCrossProfilePoiScopingAndHierarchy() throws Exception {
        System.out.println("-> Testando isolamento e hierarquia cross-profile de POIs...");
        SQLiteDatabase db = new SQLiteDatabase();

        String profileAlpha = "profile_alpha";
        String profileBeta = "profile_beta";
        String profileGamma = "profile_gamma";

        // Alpha importa Capítulo 1
        boolean okA = KingmakerCampaignImporter.importChapter(db, profileAlpha, createSampleChapter("ch1"));
        check(okA, "Alpha importou ch1 com sucesso");

        List<Map<String, Object>> alphaPois = db.queryTable("campaign_pois", profileAlpha);
        check(alphaPois.size() == 2, "Alpha possui exatamente 2 POIs");

        // Beta importa mesmo Capítulo 1
        boolean okB = KingmakerCampaignImporter.importChapter(db, profileBeta, createSampleChapter("ch1"));
        check(okB, "Beta importou ch1 com sucesso");

        // Gamma importa mesmo Capítulo 1
        boolean okG = KingmakerCampaignImporter.importChapter(db, profileGamma, createSampleChapter("ch1"));
        check(okG, "Gamma importou ch1 com sucesso");

        // Verifica que Alpha ainda possui 2 POIs intactos
        List<Map<String, Object>> alphaPoisAfter = db.queryTable("campaign_pois", profileAlpha);
        check(alphaPoisAfter.size() == 2, "Alpha mantém 2 POIs após importações de Beta e Gamma");

        // Verifica que Beta possui 2 POIs intactos
        List<Map<String, Object>> betaPois = db.queryTable("campaign_pois", profileBeta);
        check(betaPois.size() == 2, "Beta possui 2 POIs intactos");

        // Verifica que Gamma possui 2 POIs intactos
        List<Map<String, Object>> gammaPois = db.queryTable("campaign_pois", profileGamma);
        check(gammaPois.size() == 2, "Gamma possui 2 POIs intactos");

        // Verifica o escopo exato do poi_id e parent_poi_id em cada perfil
        for (Map<String, Object> row : alphaPoisAfter) {
            String poiId = (String) row.get("poi_id");
            String parentId = (String) row.get("parent_poi_id");
            check(poiId.startsWith("profile_alpha:"), "POI de Alpha deve iniciar com profile_alpha:");
            if (parentId != null && parentId.length() > 0) {
                check(parentId.startsWith("profile_alpha:"), "Parent POI de Alpha deve iniciar com profile_alpha:");
                check(parentId.equals("profile_alpha:poi_posto_oleg"), "Parent de armazém deve ser profile_alpha:poi_posto_oleg");
            }
        }

        for (Map<String, Object> row : betaPois) {
            String poiId = (String) row.get("poi_id");
            String parentId = (String) row.get("parent_poi_id");
            check(poiId.startsWith("profile_beta:"), "POI de Beta deve iniciar com profile_beta:");
            if (parentId != null && parentId.length() > 0) {
                check(parentId.startsWith("profile_beta:"), "Parent POI de Beta deve iniciar com profile_beta:");
                check(parentId.equals("profile_beta:poi_posto_oleg"), "Parent de armazém deve ser profile_beta:poi_posto_oleg");
            }
        }

        // Verifica isolamento em exportCampaignState
        JSONObject exportAlpha = KingmakerCampaignImporter.exportCampaignState(db, profileAlpha);
        JSONArray exportAlphaPois = exportAlpha.optJSONArray("campaignPois");
        check(exportAlphaPois != null && exportAlphaPois.length() == 2, "exportCampaignState(Alpha) contém exatamente 2 POIs");

        JSONObject exportBeta = KingmakerCampaignImporter.exportCampaignState(db, profileBeta);
        JSONArray exportBetaPois = exportBeta.optJSONArray("campaignPois");
        check(exportBetaPois != null && exportBetaPois.length() == 2, "exportCampaignState(Beta) contém exatamente 2 POIs");
    }

    private static void testIdempotencyAndNoDoublePrefixing() throws Exception {
        System.out.println("-> Testando idempotência de reimportação e prevenção de double-prefixing...");
        SQLiteDatabase db = new SQLiteDatabase();
        String profile = "idempotent_profile";

        // Importação 1
        boolean ok1 = KingmakerCampaignImporter.importChapter(db, profile, createSampleChapter("ch1"));
        check(ok1, "Primeira importação bem sucedida");
        check(db.queryTable("campaign_pois", profile).size() == 2, "2 POIs após primeira importação");

        // Importação 2 (idêntica)
        boolean ok2 = KingmakerCampaignImporter.importChapter(db, profile, createSampleChapter("ch1"));
        check(ok2, "Segunda importação bem sucedida");
        List<Map<String, Object>> poisAfter2 = db.queryTable("campaign_pois", profile);
        check(poisAfter2.size() == 2, "Ainda 2 POIs após reimportação (sem duplicatas)");

        // Garante que poi_id não foi prefixado duas vezes
        for (Map<String, Object> row : poisAfter2) {
            String poiId = (String) row.get("poi_id");
            check(!poiId.startsWith("idempotent_profile:idempotent_profile:"), "Não deve haver double-prefix");
            check(poiId.startsWith("idempotent_profile:"), "Deve ter prefixo simples");
        }
    }

    private static void testBackupV22ContractValidBackups() throws Exception {
        System.out.println("-> Testando contrato backup v22 para backups válidos...");

        // Caso 1: Backup vazio saudável
        JSONObject emptyBackup = new JSONObject();
        emptyBackup.put("campaignPois", new JSONArray());
        emptyBackup.put("tacticalScenes", new JSONArray());
        emptyBackup.put("quests", new JSONArray());

        JSONObject reportEmpty = KingmakerCampaignImporter.validateCampaignRestore(emptyBackup);
        check(reportEmpty.has("healthy"), "Relatório de backup possui chave 'healthy'");
        check(reportEmpty.has("valid"), "Relatório de backup possui chave 'valid'");
        check(reportEmpty.optBoolean("healthy", false), "Backup vazio deve ser healthy: true");
        check(reportEmpty.optBoolean("valid", false), "Backup vazio deve ser valid: true");
        check(reportEmpty.optJSONArray("errors") != null && reportEmpty.optJSONArray("errors").length() == 0, "Zero erros em backup vazio");

        // Caso 2: Backup populado saudável
        JSONObject validBackup = new JSONObject();

        JSONArray pois = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("poi_id", "p1");
        p.put("hex_id", "H_01");
        p.put("canonical", 0);
        p.put("source", "campaign");
        pois.put(p);
        validBackup.put("campaignPois", pois);

        JSONArray scenes = new JSONArray();
        JSONObject sc = new JSONObject();
        sc.put("scene_key", "sc1");
        JSONArray hazArr = new JSONArray();
        JSONObject haz = new JSONObject();
        haz.put("level", 3);
        haz.put("stealth_dc", 18);
        haz.put("saving_throw_dc", 20);
        hazArr.put(haz);
        sc.put("hazards_json", hazArr.toString());
        scenes.put(sc);
        validBackup.put("tacticalScenes", scenes);

        JSONArray quests = new JSONArray();
        JSONObject q = new JSONObject();
        q.put("title", "Quest Principal");
        quests.put(q);
        validBackup.put("quests", quests);

        JSONObject reportValid = KingmakerCampaignImporter.validateCampaignRestore(validBackup);
        check(reportValid.optBoolean("healthy", false), "Backup populado válido deve ser healthy: true");
        check(reportValid.optBoolean("valid", false), "Backup populado válido deve ser valid: true");
        check(reportValid.optJSONArray("errors") != null && reportValid.optJSONArray("errors").length() == 0, "Zero erros em backup populado válido");
        JSONObject stats = reportValid.optJSONObject("stats");
        check(stats != null, "stats não pode ser nulo");
        check(stats.optInt("pois_count", 0) == 1, "Stats contêm 1 POI");
        check(stats.optInt("scenes_count", 0) == 1, "Stats contêm 1 cena");
        check(stats.optInt("hazards_count", 0) == 1, "Stats contêm 1 perigo");
        check(stats.optInt("quests_count", 0) == 1, "Stats contêm 1 quest");
    }

    private static void testBackupV22ContractCorruptedPayloads() throws Exception {
        System.out.println("-> Testando contrato backup v22 para payloads corrompidos...");

        // Caso 1: Backup nulo
        JSONObject reportNull = KingmakerCampaignImporter.validateCampaignRestore(null);
        check(reportNull.has("healthy"), "Report null deve conter 'healthy'");
        check(!reportNull.optBoolean("healthy", true), "Report null deve ter healthy: false");
        check(!reportNull.optBoolean("valid", true), "Report null deve ter valid: false");
        check(reportNull.optJSONArray("errors") != null && reportNull.optJSONArray("errors").length() > 0, "Report null deve registrar erro");

        // Caso 2: Perigo com nível inválido (-1 e 30)
        JSONObject badHazardBackup = new JSONObject();
        JSONArray scenes = new JSONArray();
        JSONObject sc = new JSONObject();
        sc.put("scene_key", "sc_bad_haz");
        JSONArray hazArr = new JSONArray();
        JSONObject hBad1 = new JSONObject();
        hBad1.put("level", -1);
        hBad1.put("stealth_dc", 15);
        hBad1.put("saving_throw_dc", 15);
        hazArr.put(hBad1);
        JSONObject hBad2 = new JSONObject();
        hBad2.put("level", 30);
        hBad2.put("stealth_dc", 15);
        hBad2.put("saving_throw_dc", 15);
        hazArr.put(hBad2);
        sc.put("hazards_json", hazArr.toString());
        scenes.put(sc);
        badHazardBackup.put("tacticalScenes", scenes);

        JSONObject reportBadHaz = KingmakerCampaignImporter.validateCampaignRestore(badHazardBackup);
        check(!reportBadHaz.optBoolean("healthy", true), "Nível de perigo inválido deve gerar healthy: false");
        check(!reportBadHaz.optBoolean("valid", true), "Nível de perigo inválido deve gerar valid: false");
        check(reportBadHaz.optJSONArray("errors") != null && reportBadHaz.optJSONArray("errors").length() >= 2, "Erros registrados para perigos com level -1 e 30");

        // Caso 3: Perigo com stealth_dc <= 0 ou saving_throw_dc <= 0
        JSONObject badDcBackup = new JSONObject();
        JSONArray scenes2 = new JSONArray();
        JSONObject sc2 = new JSONObject();
        sc2.put("scene_key", "sc_bad_dc");
        JSONArray hazArr2 = new JSONArray();
        JSONObject hBadDc = new JSONObject();
        hBadDc.put("level", 2);
        hBadDc.put("stealth_dc", 0);
        hBadDc.put("saving_throw_dc", -3);
        hazArr2.put(hBadDc);
        sc2.put("hazards_json", hazArr2.toString());
        scenes2.put(sc2);
        badDcBackup.put("tacticalScenes", scenes2);

        JSONObject reportBadDc = KingmakerCampaignImporter.validateCampaignRestore(badDcBackup);
        check(!reportBadDc.optBoolean("healthy", true), "CDs não-positivas devem gerar healthy: false");
        check(!reportBadDc.optBoolean("valid", true), "CDs não-positivas devem gerar valid: false");

        // Caso 4: POI duplicado ou canonical != 0
        JSONObject badPoiBackup = new JSONObject();
        JSONArray badPois = new JSONArray();
        JSONObject p1 = new JSONObject();
        p1.put("poi_id", "dup1");
        p1.put("hex_id", "H_01");
        p1.put("canonical", 1); // violação!
        badPois.put(p1);
        JSONObject p2 = new JSONObject();
        p2.put("poi_id", "dup1"); // duplicado!
        p2.put("hex_id", "H_02");
        p2.put("canonical", 0);
        badPois.put(p2);
        badPoiBackup.put("campaignPois", badPois);

        JSONObject reportBadPoi = KingmakerCampaignImporter.validateCampaignRestore(badPoiBackup);
        check(!reportBadPoi.optBoolean("healthy", true), "POI duplicado/canonical!=0 deve gerar healthy: false");
        check(!reportBadPoi.optBoolean("valid", true), "POI duplicado/canonical!=0 deve gerar valid: false");
        check(reportBadPoi.optJSONArray("errors") != null && reportBadPoi.optJSONArray("errors").length() >= 2, "Erros registrados para canonical e duplicidade");

        // Caso 5: hazards_json com sintaxe inválida
        JSONObject badHazJsonBackup = new JSONObject();
        JSONArray scenes3 = new JSONArray();
        JSONObject sc3 = new JSONObject();
        sc3.put("scene_key", "sc_syntax_err");
        sc3.put("hazards_json", "{not_valid_json_array");
        scenes3.put(sc3);
        badHazJsonBackup.put("tacticalScenes", scenes3);

        JSONObject reportBadSyntax = KingmakerCampaignImporter.validateCampaignRestore(badHazJsonBackup);
        check(!reportBadSyntax.optBoolean("healthy", true), "hazards_json malformado deve gerar healthy: false");
        check(!reportBadSyntax.optBoolean("valid", true), "hazards_json malformado deve gerar valid: false");
    }

    private static void testFailClosedNonArrayTypeHardening() throws Exception {
        System.out.println("-> Testando fail-closed hardening para tipos não-array...");

        // Caso 1: String em campaignPois
        JSONObject corruptStringPois = new JSONObject();
        corruptStringPois.put("campaignPois", "string_invalida_nao_array");
        JSONObject r1 = KingmakerCampaignImporter.validateCampaignRestore(corruptStringPois);
        check(!r1.optBoolean("healthy", true), "campaignPois como String deve retornar healthy: false");
        check(!r1.optBoolean("valid", true), "campaignPois como String deve retornar valid: false");
        check(hasError(r1, "campaignPois deve ser um array"), "Deve conter mensagem de erro de array para campaignPois");

        // Caso 2: Inteiro em tacticalScenes
        JSONObject corruptIntScenes = new JSONObject();
        corruptIntScenes.put("tacticalScenes", 123456);
        JSONObject r2 = KingmakerCampaignImporter.validateCampaignRestore(corruptIntScenes);
        check(!r2.optBoolean("healthy", true), "tacticalScenes como Integer deve retornar healthy: false");
        check(!r2.optBoolean("valid", true), "tacticalScenes como Integer deve retornar valid: false");
        check(hasError(r2, "tacticalScenes deve ser um array"), "Deve conter mensagem de erro de array para tacticalScenes");

        // Caso 3: Booleano em quests
        JSONObject corruptBoolQuests = new JSONObject();
        corruptBoolQuests.put("quests", true);
        JSONObject r3 = KingmakerCampaignImporter.validateCampaignRestore(corruptBoolQuests);
        check(!r3.optBoolean("healthy", true), "quests como Boolean deve retornar healthy: false");
        check(!r3.optBoolean("valid", true), "quests como Boolean deve retornar valid: false");
        check(hasError(r3, "quests deve ser um array"), "Deve conter mensagem de erro de array para quests");

        // Caso 4: Objeto JSONObject em campaignPois
        JSONObject corruptObjPois = new JSONObject();
        corruptObjPois.put("campaignPois", new JSONObject().put("raw", "object"));
        JSONObject r4 = KingmakerCampaignImporter.validateCampaignRestore(corruptObjPois);
        check(!r4.optBoolean("healthy", true), "campaignPois como JSONObject deve retornar healthy: false");
        check(!r4.optBoolean("valid", true), "campaignPois como JSONObject deve retornar valid: false");
        check(hasError(r4, "campaignPois deve ser um array"), "Deve conter mensagem de erro de array para campaignPois com JSONObject");

        // Caso 5: Todos os 3 corrompidos simultaneamente
        JSONObject corruptAll = new JSONObject();
        corruptAll.put("campaignPois", "corrupt_pois");
        corruptAll.put("tacticalScenes", 99999);
        corruptAll.put("quests", false);
        JSONObject r5 = KingmakerCampaignImporter.validateCampaignRestore(corruptAll);
        check(!r5.optBoolean("healthy", true), "Todos corrompidos deve retornar healthy: false");
        check(!r5.optBoolean("valid", true), "Todos corrompidos deve retornar valid: false");
        check(r5.optJSONArray("errors") != null && r5.optJSONArray("errors").length() == 3, "Exatamente 3 erros de tipagem registrados");
        check(hasError(r5, "campaignPois deve ser um array"), "Erro de campaignPois registrado");
        check(hasError(r5, "tacticalScenes deve ser um array"), "Erro de tacticalScenes registrado");
        check(hasError(r5, "quests deve ser um array"), "Erro de quests registrado");
    }

    private static boolean hasError(JSONObject report, String expectedSubstring) {
        JSONArray errs = report.optJSONArray("errors");
        if (errs == null) return false;
        for (int i = 0; i < errs.length(); i++) {
            String err = errs.optString(i, "");
            if (err.contains(expectedSubstring)) {
                return true;
            }
        }
        return false;
    }
}
