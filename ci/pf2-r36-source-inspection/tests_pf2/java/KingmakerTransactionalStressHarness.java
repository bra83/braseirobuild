package com.braseiro.pathfinder2e;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Empirical Transactional Stress Harness for Milestone 1:
 * Challenges KingmakerCampaignImporter on:
 * 1. Partial ingestion failures: mid-chapter injection, 100% rollback verification.
 * 2. Concurrency / world_profile isolation: multiple profiles importing same chapter.
 * 3. Corrupt backup restoration payloads: fail-closed validation & healthy: false contract.
 */
public final class KingmakerTransactionalStressHarness {
    private static int checks = 0;
    private static int findingsCount = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FALHA NA VERIFICAÇÃO DO HARNESS DE STRESS: " + label);
        }
        checks++;
    }

    private static void recordVulnerability(String title, String detail) {
        findingsCount++;
        System.out.println("VULNERABILIDADE ENCONTRADA #" + findingsCount + " [" + title + "]: " + detail);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("INICIANDO STRESS TESTING EMPÍRICO DE TRANSAÇÕES E FAIL-CLOSED...");

        testPartialIngestionPreValidationRollback();
        testPartialIngestionMidStreamFailureRollback();
        testWorldProfileIsolationAndPoiKeyConflict();
        testCorruptBackupValidationPayloads();

        System.out.println("KINGMAKER_TRANSACTIONAL_STRESS_HARNESS_COMPLETE ("
                + checks + " checks passed, " + findingsCount + " vulnerabilities documented)");
    }

    private static JSONObject createValidChapterJson(String chapterKey) throws JSONException {
        JSONObject cat = new JSONObject();
        cat.put("schema", "braseiro-pathfinder2e-kingmaker-chapter-v1");
        cat.put("chapter_key", chapterKey);
        cat.put("chapter_number", 1);
        cat.put("title", "Capítulo 1 - Terras Roubadas");
        cat.put("level_min", 1);
        cat.put("level_max", 2);
        cat.put("summary", "Prólogo canônico");

        // Facção
        JSONArray fArr = new JSONArray();
        JSONObject f = new JSONObject();
        f.put("name", "Restov Aldori");
        f.put("agenda", "Colonizar as Terras Roubadas");
        fArr.put(f);
        cat.put("factions", fArr);

        // Quest
        JSONArray qArr = new JSONArray();
        JSONObject q = new JSONObject();
        q.put("quest_key", chapterKey + "_q1");
        q.put("title", "Proteger o Posto de Oleg");
        q.put("status", "unstarted");
        q.put("milestone_xp", 100);
        qArr.put(q);
        cat.put("quests", qArr);

        // POI
        JSONArray pArr = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("poi_id", "poi_posto_oleg");
        p.put("poi_key", "poi_posto_oleg");
        p.put("name", "Posto Avançado de Oleg");
        p.put("hex_id", "H_01");
        p.put("source", "campaign");
        p.put("canonical", 0);
        p.put("status", "active");
        pArr.put(p);
        cat.put("pois", pArr);

        // Hazard
        JSONArray hArr = new JSONArray();
        JSONObject h = new JSONObject();
        h.put("hazard_key", "poco_com_estacas");
        h.put("name", "Poço Camuflado");
        h.put("level", 2);
        h.put("stealth_dc", 18);
        h.put("saving_throw_dc", 17);
        JSONArray dis = new JSONArray();
        dis.put(new JSONObject().put("skill", "Ladroagem").put("dc", 18).put("proficiency", "treinado"));
        h.put("disable_skills", dis);
        hArr.put(h);
        cat.put("hazards", hArr);

        return cat;
    }

    // =========================================================================
    // 1. PARTIAL INGESTION: PRE-VALIDATION FAILURE & 100% ROLLBACK
    // =========================================================================
    private static void testPartialIngestionPreValidationRollback() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        JSONObject invalidChapter = createValidChapterJson("ch1_corrupt_hazard");

        // Injeta perigo com level negativo (-1) e CD de salvaguarda 0
        JSONArray hazards = invalidChapter.optJSONArray("hazards");
        JSONObject badHazard = hazards.optJSONObject(0);
        badHazard.put("level", -1);
        badHazard.put("saving_throw_dc", 0);

        boolean exceptionCaught = false;
        try {
            KingmakerCampaignImporter.importChapter(db, "pf2", invalidChapter);
        } catch (IllegalArgumentException e) {
            exceptionCaught = true;
        }

        check(exceptionCaught, "Exceção disparada na pré-validação de perigo malformado");
        check(db.getRowCount("kingmaker_campaign_chapters") == 0, "Zero capítulos persistidos");
        check(db.getRowCount("factions") == 0, "Zero facções persistidas");
        check(db.getRowCount("quests") == 0, "Zero quests persistidas");
        check(db.getRowCount("campaign_pois") == 0, "Zero POIs persistidos");
        check(db.getRowCount("tactical_scenes") == 0, "Zero cenas persistidas");
        System.out.println("  [PASS] Pré-validação fail-closed barrou 100% dos dados.");
    }

    // =========================================================================
    // 2. PARTIAL INGESTION: MID-STREAM I/O FAILURE & 100% ROLLBACK
    // =========================================================================
    private static void testPartialIngestionMidStreamFailureRollback() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        JSONObject validChapter = createValidChapterJson("ch1_midstream_fail");

        // Simula falha de I/O SQLite no meio da inserção (na tabela 'quests')
        // Metadados do capítulo e facções já foram inseridos antes de 'quests'
        db.failOnTableInsert = "quests";

        boolean exceptionCaught = false;
        try {
            KingmakerCampaignImporter.importChapter(db, "pf2", validChapter);
        } catch (SQLiteException e) {
            exceptionCaught = true;
        }

        check(exceptionCaught, "Exceção SQLite disparada no meio da transação");
        // Verifica se o rollback desfez as inserções anteriores (capítulo e facções)
        check(db.getRowCount("kingmaker_campaign_chapters") == 0, "100% rollback: kingmaker_campaign_chapters vazio");
        check(db.getRowCount("factions") == 0, "100% rollback: factions vazio");
        check(db.getRowCount("quests") == 0, "100% rollback: quests vazio");
        check(db.getRowCount("campaign_pois") == 0, "100% rollback: campaign_pois vazio");
        System.out.println("  [PASS] Atomicidade de transação SQLite: 100% de rollback após falha no meio da importação.");
    }

    // =========================================================================
    // 3. CONCURRENCY & WORLD PROFILE ISOLATION: POI KEY COLLISION
    // =========================================================================
    private static void testWorldProfileIsolationAndPoiKeyConflict() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        JSONObject chProfileA = createValidChapterJson("chapter_01_stolen_lands");
        JSONObject chProfileB = createValidChapterJson("chapter_01_stolen_lands");

        // Perfil A importa Capítulo 1
        boolean okA = KingmakerCampaignImporter.importChapter(db, "profile_alpha", chProfileA);
        check(okA, "Perfil Alpha importou com sucesso");

        List<Map<String, Object>> poisAlphaBefore = db.queryTable("campaign_pois", "profile_alpha");
        check(poisAlphaBefore.size() == 1, "Perfil Alpha tem 1 POI antes do Perfil Beta importar");

        // Perfil B importa simultaneamente / posteriormente o mesmo Capítulo 1
        boolean okB = KingmakerCampaignImporter.importChapter(db, "profile_beta", chProfileB);
        check(okB, "Perfil Beta importou com sucesso");

        List<Map<String, Object>> poisBeta = db.queryTable("campaign_pois", "profile_beta");
        check(poisBeta.size() == 1, "Perfil Beta tem 1 POI");

        // AUDITORIA CRÍTICA: O que aconteceu com o POI do Perfil Alpha?
        List<Map<String, Object>> poisAlphaAfter = db.queryTable("campaign_pois", "profile_alpha");

        if (poisAlphaAfter.isEmpty()) {
            recordVulnerability("QUEBRA_DE_ISOLAMENTO_WORLD_PROFILE_CAMPAIGN_POIS",
                    "Em AppDb.java linha 821, 'campaign_pois' define 'poi_id TEXT PRIMARY KEY' sem incluir world_profile na PK. "
                    + "Quando 'profile_beta' importou o mesmo poi_id ('poi_posto_oleg'), o CONFLICT_REPLACE em KingmakerCampaignImporter:413 "
                    + "sobrescreveu e deletou o POI de 'profile_alpha', reduzindo seus POIs de 1 para 0!");
        } else {
            check(poisAlphaAfter.size() == 1, "Perfil Alpha manteve seu POI isolado");
        }

        // Verifica exportCampaignState de profile_alpha
        JSONObject exportAlpha = KingmakerCampaignImporter.exportCampaignState(db, "profile_alpha");
        JSONArray exportAlphaPois = exportAlpha.optJSONArray("campaignPois");
        if (exportAlphaPois == null || exportAlphaPois.length() == 0) {
            recordVulnerability("EXPORT_CAMPAIGN_STATE_DATA_LOSS_CROSS_PROFILE",
                    "exportCampaignState('profile_alpha') retornou 0 campaignPois após importação do profile_beta.");
        }
    }

    // =========================================================================
    // 4. CORRUPT BACKUP RESTORATION PAYLOADS: FAIL-CLOSED & HEALTHY: FALSE
    // =========================================================================
    private static void testCorruptBackupValidationPayloads() throws Exception {
        // 4A. Teste do contrato 'healthy' na resposta de validateCampaignRestore
        JSONObject sampleBackup = new JSONObject();
        sampleBackup.put("campaignPois", new JSONArray());
        sampleBackup.put("tacticalScenes", new JSONArray());
        sampleBackup.put("quests", new JSONArray());

        JSONObject reportValid = KingmakerCampaignImporter.validateCampaignRestore(sampleBackup);
        if (!reportValid.has("healthy")) {
            recordVulnerability("CONTRATO_BACKUP_V22_HEALTHY_AUSENTE",
                    "validateCampaignRestore retornou 'valid' mas OMITIU a chave mandatória 'healthy'. "
                    + "O contrato de backup v22 e AppDb.java:2360 exigem 'healthy: boolean'. Chamadas a optBoolean(\"healthy\", false) "
                    + "avaliarão incorretamente para false mesmo em backups saudáveis!");
        }

        // 4B. Teste de payload corrompido com perigos inválidos
        JSONObject corruptHazardBackup = new JSONObject();
        JSONArray scenes = new JSONArray();
        JSONObject scene = new JSONObject();
        scene.put("scene_key", "cena_corrompida");
        JSONArray hazards = new JSONArray();
        JSONObject badHaz = new JSONObject();
        badHaz.put("level", 99); // Nível inválido (>25)
        badHaz.put("stealth_dc", -5); // Furtividade negativa
        badHaz.put("saving_throw_dc", 0); // Salvaguarda zero
        hazards.put(badHaz);
        scene.put("hazards_json", hazards.toString());
        scenes.put(scene);
        corruptHazardBackup.put("tacticalScenes", scenes);

        JSONObject reportCorrupt = KingmakerCampaignImporter.validateCampaignRestore(corruptHazardBackup);
        check(!reportCorrupt.optBoolean("valid", true), "validateCampaignRestore rejeitou perigo com nível 99 (valid: false)");
        if (!reportCorrupt.has("healthy") || reportCorrupt.optBoolean("healthy", true)) {
            recordVulnerability("VALIDADOR_NAO_RETORNA_HEALTHY_FALSE_EM_CORRUPCAO",
                    "Em caso de estrutura corrompida de perigo, validateCampaignRestore não definiu 'healthy: false'.");
        }

        // 4C. Teste de fail-closed com tipos não-array corrompidos
        JSONObject corruptTypesBackup = new JSONObject();
        corruptTypesBackup.put("campaignPois", "STRING_CORROMPIDA_NAO_ARRAY");
        corruptTypesBackup.put("tacticalScenes", 99999);
        corruptTypesBackup.put("quests", true);

        JSONObject reportTypes = KingmakerCampaignImporter.validateCampaignRestore(corruptTypesBackup);
        if (reportTypes.optBoolean("valid", false)) {
            recordVulnerability("FAIL_OPEN_EM_TIPOS_DE_DADOS_CORROMPIDOS",
                    "validateCampaignRestore retornou valid: true quando campaignPois, tacticalScenes e quests "
                    + "continham strings/números/booleanos corrompidos em vez de JSONArrays! "
                    + "Como optJSONArray retornou null, a validação foi silenciosamente pulada sem registrar erros.");
        }

        // 4D. Teste com backupRoot nulo
        JSONObject reportNull = KingmakerCampaignImporter.validateCampaignRestore(null);
        check(!reportNull.optBoolean("valid", true), "Backup nulo rejeitado com valid: false");
    }
}
