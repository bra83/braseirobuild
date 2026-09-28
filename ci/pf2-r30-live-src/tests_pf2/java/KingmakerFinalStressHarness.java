package com.braseiro.pathfinder2e;

import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tier 5 Final Adversarial Stress Challenger Harness for Milestone M_FINAL.
 * Validates:
 * 1. Chapters 1-12 Stress:
 *    - Real disk loading, parsing via KingmakerCampaignModels.ChapterCatalog.fromJson, and clean validate().
 *    - Extreme level progression (Level 1 to 20) and milestone XP summation.
 *    - Remaster hazard edge cases across all chapters (saving throw DCs, disable DCs, stealth DCs, routine actions).
 * 2. Transactional Ingestion & Isolation:
 *    - Batch ingestion of all 12 chapters, multi-profile isolation, idempotency, and rollback atomicity.
 * 3. Solo Director & Routing Stress:
 *    - All 9 routes in KingmakerSoloFlowRouter resolve deterministically with adversarial fallback handling.
 * 4. Backup v22 Fail-Closed Stress:
 *    - Exhaustive fuzzing with corrupted structures confirming validateCampaignRestore strictly returns healthy: false.
 */
public final class KingmakerFinalStressHarness {
    private static int checksPassed = 0;

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED [TIER 5]: " + description);
        }
        checksPassed++;
    }

    private static String readFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            char[] buf = new char[8192];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== TIER 5 FINAL ADVERSARIAL STRESS HARNESS INICIANDO ===");

        File chaptersDir = null;
        if (args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            chaptersDir = new File(args[0]);
        }
        if (chaptersDir == null || !chaptersDir.exists()) {
            chaptersDir = new File("android/app/src/main/assets/pathfinder/kingmaker/chapters");
        }
        if (!chaptersDir.exists()) {
            chaptersDir = new File("../android/app/src/main/assets/pathfinder/kingmaker/chapters");
        }
        if (!chaptersDir.exists()) {
            throw new IllegalStateException("Diretório de capítulos não encontrado: " + chaptersDir.getAbsolutePath());
        }

        testChapters1To12Stress(chaptersDir);
        testTransactionalIngestionAndProfileIsolation(chaptersDir);
        testSoloFlowRouterNineRoutesAndAdversarialFallback();
        testBackupV22FailClosedCorruptedPayloadFuzzing();

        System.out.println("=== TIER 5 HARNESS CONCLUÍDO COM SUCESSO ABSOLUTO ===");
        System.out.println("TOTAL DE VERIFICAÇÕES PASSADAS COM ZERO FALHAS: " + checksPassed);
        System.out.println("FINAL_STRESS_HARNESS_PASS");
    }

    // =========================================================================
    // 1. CHAPTERS 1-12 ADVERSARIAL STRESS & LEVEL PROGRESSION
    // =========================================================================
    private static void testChapters1To12Stress(File chaptersDir) throws Exception {
        System.out.println("-> [1/4] Testando integridade canônica, parsing e progressão de níveis nos Capítulos 1-12...");

        String[] canonicalFilenames = {
            "chapter_01_stolen_lands_part1.json",
            "chapter_02_stolen_lands_part2.json",
            "chapter_03_rivers_run_red.json",
            "chapter_04_cult_of_bloom.json",
            "chapter_05_varnhold_vanishing.json",
            "chapter_06_blood_for_blood.json",
            "chapter_07_war_of_river_kings.json",
            "chapter_08_they_lurk_below.json",
            "chapter_09_sound_thousand_screams.json",
            "chapter_10_curse_lantern_king.json",
            "chapter_11_hex_atlas_pois.json",
            "chapter_12_companion_stories.json"
        };

        Set<String> validSavingThrows = new HashSet<>(Arrays.asList("Reflexos", "Fortitude", "Vontade"));
        Set<String> validProficiencies = new HashSet<>(Arrays.asList("treinado", "especialista", "mestre", "lendario"));

        long totalMilestoneXp = 0;
        int totalQuestsCount = 0;
        int totalHazardsCount = 0;
        int totalPoisCount = 0;

        List<KingmakerCampaignModels.ChapterCatalog> catalogs = new ArrayList<>();

        for (int i = 0; i < canonicalFilenames.length; i++) {
            String filename = canonicalFilenames[i];
            File file = new File(chaptersDir, filename);
            check(file.exists(), "Arquivo de capítulo deve existir no disco: " + filename);

            String content = readFile(file);
            check(content.length() > 500, "Arquivo de capítulo não pode ser vazio ou truncado: " + filename);

            JSONObject json = new JSONObject(content);
            KingmakerCampaignModels.ChapterCatalog cat = KingmakerCampaignModels.ChapterCatalog.fromJson(json);

            // 1. Validação estrita do catálogo via método interno
            cat.validate();

            check("braseiro-pathfinder2e-chapter-v1".equals(cat.schema) || "braseiro-pathfinder2e-kingmaker-chapter-v1".equals(cat.schema),
                    "Esquema canônico deve corresponder para " + filename);
            check("pt-BR".equals(cat.locale), "Idioma deve ser pt-BR para " + filename);
            check(cat.title != null && !cat.title.trim().isEmpty(), "Título em pt-BR obrigatório para " + filename);
            check(cat.chapterKey != null && !cat.chapterKey.trim().isEmpty(), "chapter_key obrigatório para " + filename);
            check(cat.levelMin >= 1 && cat.levelMax <= 20 && cat.levelMin <= cat.levelMax,
                    "Faixa de níveis válida para " + filename + " (" + cat.levelMin + "-" + cat.levelMax + ")");

            // Roundtrip de serialização
            JSONObject serialized = cat.toJson();
            KingmakerCampaignModels.ChapterCatalog reloaded = KingmakerCampaignModels.ChapterCatalog.fromJson(serialized);
            reloaded.validate();
            check(cat.chapterKey.equals(reloaded.chapterKey), "Roundtrip preserva chapterKey em " + filename);
            check(cat.quests.size() == reloaded.quests.size(), "Roundtrip preserva contagem de quests em " + filename);
            check(cat.hazards.size() == reloaded.hazards.size(), "Roundtrip preserva contagem de perigos em " + filename);
            check(cat.pois.size() == reloaded.pois.size(), "Roundtrip preserva contagem de POIs em " + filename);

            // Quests e marcos de XP
            check(!cat.quests.isEmpty(), "Capítulo deve possuir quests estruturadas: " + filename);
            totalQuestsCount += cat.quests.size();

            for (KingmakerCampaignModels.QuestModel q : cat.quests) {
                check(q.title != null && !q.title.trim().isEmpty(), "Quest sem título em " + filename + ": " + q.questKey);
                check(q.milestoneXp >= 0, "Milestone XP não pode ser negativo na quest " + q.questKey);
                totalMilestoneXp += q.milestoneXp;
            }

            for (KingmakerCampaignModels.CampaignMilestone m : cat.milestones) {
                check(m.xp >= 0, "Milestone XP não pode ser negativo no marco " + m.milestoneKey);
                totalMilestoneXp += m.xp;
            }

            // Perigos canônicos (catálogo raiz)
            check(!cat.hazards.isEmpty(), "Capítulo deve possuir perigos canônicos: " + filename);
            for (KingmakerCampaignModels.CanonicalHazardModel h : cat.hazards) {
                totalHazardsCount++;
                check(h.hazardKey != null && !h.hazardKey.trim().isEmpty(), "hazardKey não pode ser vazio");
                check(h.name != null && !h.name.trim().isEmpty(), "Nome do perigo não pode ser vazio");
                check(h.level >= 0 && h.level <= 25, "Nível do perigo fora da faixa PF2e: " + h.level + " em " + h.hazardKey);
                check(h.stealthDc > 0, "CD de Furtividade deve ser positiva: " + h.stealthDc + " em " + h.hazardKey);
                check(h.perceptionDc > 0, "CD de Percepção deve ser positiva: " + h.perceptionDc + " em " + h.hazardKey);
                check(h.savingThrowDc > 0, "CD de Salvaguarda deve ser positiva: " + h.savingThrowDc + " em " + h.hazardKey);
                check(validSavingThrows.contains(h.savingThrowType), "Tipo de salvaguarda Remaster inválido: " + h.savingThrowType);
                check(!h.disableSkills.isEmpty(), "Perigo deve ter métodos de desativação: " + h.hazardKey);

                for (KingmakerCampaignModels.HazardDisableOption opt : h.disableSkills) {
                    check(opt.skill != null && !opt.skill.trim().isEmpty(), "Perícia de desativação não pode ser vazia");
                    check(opt.dc > 0, "CD de desativação deve ser positiva: " + opt.dc);
                    check(validProficiencies.contains(opt.proficiency), "Proficiência inválida: " + opt.proficiency);
                }

                check(h.routineActions >= 1, "Perigo deve ter routineActions >= 1");
                check("simples".equals(h.complexity) || "complexo".equals(h.complexity), "Complexidade inválida: " + h.complexity);
                check(h.ac >= 0 && h.hardness >= 0 && h.hp >= 0 && h.bt >= 0, "Estatísticas defensivas devem ser não-negativas");
            }

            // POIs hexagonais
            check(!cat.pois.isEmpty(), "Capítulo deve possuir POIs: " + filename);
            for (KingmakerCampaignModels.HexPoiModel p : cat.pois) {
                totalPoisCount++;
                check(p.canonical == 0, "POIs mutáveis devem possuir canonical=0");
                check("campaign".equals(p.source) || "procedural".equals(p.source), "source inválido em POI: " + p.source);
                check(p.hexId != null && !p.hexId.trim().isEmpty(), "hexId obrigatório em POI: " + p.poiId);
                check(p.name != null && !p.name.trim().isEmpty(), "name obrigatório em POI: " + p.poiId);
            }

            catalogs.add(cat);
        }

        // Validação da progressão contínua de níveis (Capítulos 1 a 10)
        System.out.println("-> Validando progressão contínua de níveis 1 a 20 nos capítulos principais 1-10...");
        check(catalogs.get(0).levelMin == 1, "Capítulo 1 deve iniciar no Nível 1");
        check(catalogs.get(9).levelMax == 20, "Capítulo 10 deve atingir o Nível 20");

        for (int i = 0; i < 9; i++) {
            KingmakerCampaignModels.ChapterCatalog cur = catalogs.get(i);
            KingmakerCampaignModels.ChapterCatalog next = catalogs.get(i + 1);
            check(next.levelMin <= cur.levelMax,
                    "Progressão de nível sem lacunas entre " + cur.chapterKey + " (" + cur.levelMin + "-" + cur.levelMax +
                    ") e " + next.chapterKey + " (" + next.levelMin + "-" + next.levelMax + ")");
        }

        // Capítulos 11 (Atlas) e 12 (Companheiros) cobrem Nível 1 a 20
        check(catalogs.get(10).levelMin == 1 && catalogs.get(10).levelMax == 20, "Capítulo 11 deve cobrir 1-20");
        check(catalogs.get(11).levelMin == 1 && catalogs.get(11).levelMax == 20, "Capítulo 12 deve cobrir 1-20");

        // Sumatório de XP de marcos
        check(totalMilestoneXp >= 19000, "Milestone XP total da campanha deve ser suficiente para progressão nível 1 a 20: " + totalMilestoneXp);
        System.out.println("-> Estatísticas canônicas agregadas: " + catalogs.size() + " capítulos, " +
                totalQuestsCount + " quests, " + totalHazardsCount + " perigos, " + totalPoisCount + " POIs, " +
                totalMilestoneXp + " XP de marcos acumulados.");
    }

    // =========================================================================
    // 2. TRANSACTIONAL INGESTION, ISOLATION & ROLLBACK STRESS
    // =========================================================================
    private static void testTransactionalIngestionAndProfileIsolation(File chaptersDir) throws Exception {
        System.out.println("-> [2/4] Testando ingestão transacional, isolamento multi-perfil e rollback atômico...");

        SQLiteDatabase db = new SQLiteDatabase();
        String primaryProfile = "profile_campaign_alpha";
        String secondaryProfile = "profile_campaign_beta";

        String[] canonicalFilenames = {
            "chapter_01_stolen_lands_part1.json",
            "chapter_02_stolen_lands_part2.json",
            "chapter_03_rivers_run_red.json",
            "chapter_04_cult_of_bloom.json",
            "chapter_05_varnhold_vanishing.json",
            "chapter_06_blood_for_blood.json",
            "chapter_07_war_of_river_kings.json",
            "chapter_08_they_lurk_below.json",
            "chapter_09_sound_thousand_screams.json",
            "chapter_10_curse_lantern_king.json",
            "chapter_11_hex_atlas_pois.json",
            "chapter_12_companion_stories.json"
        };

        // 1. Ingestão de todos os 12 capítulos no perfil primário
        for (String filename : canonicalFilenames) {
            File f = new File(chaptersDir, filename);
            JSONObject json = new JSONObject(readFile(f));
            boolean ok = KingmakerCampaignImporter.importChapter(db, primaryProfile, json);
            check(ok, "Importação do capítulo deve ter sucesso: " + filename);
            String chKey = json.optString("chapter_key");
            check(KingmakerCampaignImporter.isChapterImported(db, primaryProfile, chKey),
                    "isChapterImported deve retornar true para " + chKey);
        }

        // Exportar e validar contagens
        JSONObject stateAlpha = KingmakerCampaignImporter.exportCampaignState(db, primaryProfile);
        JSONArray questsAlpha = stateAlpha.optJSONArray("quests");
        JSONArray poisAlpha = stateAlpha.optJSONArray("campaignPois");
        JSONArray chaptersAlpha = stateAlpha.optJSONArray("chapters");
        check(chaptersAlpha != null && chaptersAlpha.length() == 12, "Perfil Alpha deve ter exatamente 12 capítulos importados");
        check(questsAlpha != null && questsAlpha.length() > 0, "Perfil Alpha deve conter quests exportadas");
        check(poisAlpha != null && poisAlpha.length() > 0, "Perfil Alpha deve conter POIs exportados");

        int initialPoisCount = poisAlpha.length();
        int initialQuestsCount = questsAlpha.length();

        // 2. Teste de Idempotência: re-importar capítulos 1 e 2 no perfil primário não deve duplicar
        for (int i = 0; i < 2; i++) {
            File f = new File(chaptersDir, canonicalFilenames[i]);
            JSONObject json = new JSONObject(readFile(f));
            boolean ok = KingmakerCampaignImporter.importChapter(db, primaryProfile, json);
            check(ok, "Re-importação idempotente com sucesso: " + canonicalFilenames[i]);
        }

        JSONObject stateAlphaReimport = KingmakerCampaignImporter.exportCampaignState(db, primaryProfile);
        check(stateAlphaReimport.optJSONArray("campaignPois").length() == initialPoisCount,
                "Idempotência: contagem de POIs não pode alterar após reimportação");
        check(stateAlphaReimport.optJSONArray("quests").length() == initialQuestsCount,
                "Idempotência: contagem de Quests não pode alterar após reimportação");

        // 3. Teste de Isolamento Cross-Profile
        File f1 = new File(chaptersDir, canonicalFilenames[0]);
        JSONObject jsonCh1 = new JSONObject(readFile(f1));
        boolean okBeta = KingmakerCampaignImporter.importChapter(db, secondaryProfile, jsonCh1);
        check(okBeta, "Importação de Ch1 no perfil Beta com sucesso");

        JSONObject stateBeta = KingmakerCampaignImporter.exportCampaignState(db, secondaryProfile);
        check(stateBeta.optJSONArray("chapters").length() == 1, "Perfil Beta deve conter apenas 1 capítulo");
        check(stateBeta.optJSONArray("campaignPois").length() < initialPoisCount,
                "Perfil Beta não deve conter POIs dos capítulos 2-12");

        // Confirma que perfil Alpha permanece com 12 capítulos sem contaminação
        JSONObject stateAlphaUntouched = KingmakerCampaignImporter.exportCampaignState(db, primaryProfile);
        check(stateAlphaUntouched.optJSONArray("chapters").length() == 12,
                "Perfil Alpha permanece intacto com 12 capítulos após atividade no Beta");

        // 4. Teste de Rollback Atômico sob Falha Injetada
        String failProfile = "profile_failing_atomic";
        db.failOnTableInsert = "tactical_scenes"; // Simula falha catastrófica de escrita na tabela de cenas

        boolean threwExpected = false;
        try {
            KingmakerCampaignImporter.importChapter(db, failProfile, jsonCh1);
        } catch (Exception expected) {
            threwExpected = true;
        }
        check(threwExpected, "Importação com injeção de falha deve lançar exceção");
        db.failOnTableInsert = null; // Remove a falha

        // Verifica que nenhuma tabela ficou com dados parciais no perfil com falha
        check(!KingmakerCampaignImporter.isChapterImported(db, failProfile, jsonCh1.optString("chapter_key")),
                "Capítulo não deve constar como importado após rollback");
        JSONObject stateFailed = KingmakerCampaignImporter.exportCampaignState(db, failProfile);
        check(stateFailed.optJSONArray("chapters").length() == 0, "Rollback: 0 capítulos persistidos");
        check(stateFailed.optJSONArray("quests").length() == 0, "Rollback: 0 quests persistidas");
        check(stateFailed.optJSONArray("campaignPois").length() == 0, "Rollback: 0 POIs persistidos");
    }

    // =========================================================================
    // 3. SOLO FLOW ROUTER 9 ROUTES & ADVERSARIAL FALLBACK
    // =========================================================================
    private static void testSoloFlowRouterNineRoutesAndAdversarialFallback() {
        System.out.println("-> [3/4] Testando roteamento determinístico das 9 rotas e fallbacks adversariais...");

        // As 9 rotas canônicas
        check("start_adventure".equals(KingmakerSoloFlowRouter.actionFor("adventure_start")),
                "adventure_start -> start_adventure");
        check("camp_encounter".equals(KingmakerSoloFlowRouter.actionFor("camp_encounter")),
                "camp_encounter -> camp_encounter");
        check("camp".equals(KingmakerSoloFlowRouter.actionFor("camp")),
                "camp -> camp");
        check("war".equals(KingmakerSoloFlowRouter.actionFor("war")),
                "war -> war");
        check("exploration_resolution".equals(KingmakerSoloFlowRouter.actionFor("exploration_resolution")),
                "exploration_resolution -> exploration_resolution");
        check("companion".equals(KingmakerSoloFlowRouter.actionFor("companion")),
                "companion -> companion");
        check("kingdom_bridge".equals(KingmakerSoloFlowRouter.actionFor("kingdom_bridge")),
                "kingdom_bridge -> kingdom_bridge");
        check("travel".equals(KingmakerSoloFlowRouter.actionFor("travel")),
                "travel -> travel");
        check("free_exploration".equals(KingmakerSoloFlowRouter.actionFor("free_exploration")),
                "free_exploration -> free_exploration");

        // Fuzzing adversarial e entradas corrompidas / desconhecidas
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor(null)),
                "null -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("")),
                "vazio -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("   ")),
                "espaços em branco -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("unknown_scene_random")),
                "cena desconhecida -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("COMBAT_ENCOUNTER")),
                "caixa alta não-mapeada -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("<script>alert(1)</script>")),
                "XSS payload -> master_fallback");
        check("master_fallback".equals(KingmakerSoloFlowRouter.actionFor("'; DROP TABLE quests; --")),
                "SQL injection payload -> master_fallback");

        // Espaços laterais são normalizados por trim()
        check("travel".equals(KingmakerSoloFlowRouter.actionFor("  travel  ")),
                "espaços laterais normalizados para travel");
        check("free_exploration".equals(KingmakerSoloFlowRouter.actionFor("\tfree_exploration\n")),
                "tabs e newlines normalizados para free_exploration");
    }

    // =========================================================================
    // 4. BACKUP V22 FAIL-CLOSED CORRUPTED PAYLOAD FUZZING
    // =========================================================================
    private static void testBackupV22FailClosedCorruptedPayloadFuzzing() throws Exception {
        System.out.println("-> [4/4] Testando fail-closed estrito contra estruturas corrompidas no backup v22...");

        // 1. Baseline: Backup saudável e válido
        JSONObject validBackup = new JSONObject();
        JSONArray validPois = new JSONArray();
        JSONObject vp = new JSONObject();
        vp.put("poi_id", "p_valid_1");
        vp.put("hex_id", "H_01");
        vp.put("canonical", 0);
        vp.put("source", "campaign");
        validPois.put(vp);
        validBackup.put("campaignPois", validPois);

        JSONArray validScenes = new JSONArray();
        JSONObject vs = new JSONObject();
        vs.put("scene_key", "scene_valid_1");
        JSONArray vHazards = new JSONArray();
        JSONObject vh = new JSONObject();
        vh.put("level", 2);
        vh.put("stealth_dc", 16);
        vh.put("saving_throw_dc", 18);
        vHazards.put(vh);
        vs.put("hazards_json", vHazards.toString());
        validScenes.put(vs);
        validBackup.put("tacticalScenes", validScenes);

        JSONArray validQuests = new JSONArray();
        JSONObject vq = new JSONObject();
        vq.put("title", "Missão Válida");
        validQuests.put(vq);
        validBackup.put("quests", validQuests);

        JSONObject reportValid = KingmakerCampaignImporter.validateCampaignRestore(validBackup);
        check(reportValid.optBoolean("healthy", false), "Backup válido deve retornar healthy: true");
        check(reportValid.optBoolean("valid", false), "Backup válido deve retornar valid: true");

        // 2. Fuzzing Adversarial: null backupRoot
        JSONObject rNull = KingmakerCampaignImporter.validateCampaignRestore(null);
        check(!rNull.optBoolean("healthy", true), "null backupRoot deve retornar healthy: false");

        // 3. Fuzzing Adversarial: tipos não-array para coleções obrigatórias
        String[] rootCollections = {
            "campaignPois", "tacticalScenes", "quests", "chapters", "factions",
            "combatants", "worldClocks", "worldEvents", "companions", "npcMemories", "npcCognitiveStates"
        };

        for (String coll : rootCollections) {
            // String em vez de array
            JSONObject badStr = new JSONObject().put(coll, "corrupted_string_not_an_array");
            JSONObject r1 = KingmakerCampaignImporter.validateCampaignRestore(badStr);
            check(!r1.optBoolean("healthy", true), "Coleção " + coll + " como String deve retornar healthy: false");

            // Integer em vez de array
            JSONObject badInt = new JSONObject().put(coll, 123456);
            JSONObject r2 = KingmakerCampaignImporter.validateCampaignRestore(badInt);
            check(!r2.optBoolean("healthy", true), "Coleção " + coll + " como Integer deve retornar healthy: false");

            // Boolean em vez de array
            JSONObject badBool = new JSONObject().put(coll, true);
            JSONObject r3 = KingmakerCampaignImporter.validateCampaignRestore(badBool);
            check(!r3.optBoolean("healthy", true), "Coleção " + coll + " como Boolean deve retornar healthy: false");

            // Object em vez de array
            JSONObject badObj = new JSONObject().put(coll, new JSONObject().put("fake", 1));
            JSONObject r4 = KingmakerCampaignImporter.validateCampaignRestore(badObj);
            check(!r4.optBoolean("healthy", true), "Coleção " + coll + " como Object deve retornar healthy: false");
        }

        // 4. Injeção de anomalias em campaignPois
        // Caso A: canonical != 0 (e.g. canonical: 1)
        JSONObject badPoi1 = new JSONObject();
        JSONArray pois1 = new JSONArray();
        JSONObject bp1 = new JSONObject().put("poi_id", "p_bad1").put("hex_id", "H_01").put("canonical", 1);
        pois1.put(bp1);
        badPoi1.put("campaignPois", pois1);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badPoi1).optBoolean("healthy", true),
                "POI com canonical=1 deve falhar fail-closed");

        // Caso B: source inválido
        JSONObject badPoi2 = new JSONObject();
        JSONArray pois2 = new JSONArray();
        JSONObject bp2 = new JSONObject().put("poi_id", "p_bad2").put("hex_id", "H_01").put("canonical", 0).put("source", "procedural_alien");
        pois2.put(bp2);
        badPoi2.put("campaignPois", pois2);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badPoi2).optBoolean("healthy", true),
                "POI com source inválido deve falhar fail-closed");

        // Caso C: hex_id vazio
        JSONObject badPoi3 = new JSONObject();
        JSONArray pois3 = new JSONArray();
        JSONObject bp3 = new JSONObject().put("poi_id", "p_bad3").put("hex_id", "  ").put("canonical", 0);
        pois3.put(bp3);
        badPoi3.put("campaignPois", pois3);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badPoi3).optBoolean("healthy", true),
                "POI com hex_id vazio deve falhar fail-closed");

        // Caso D: POIs duplicados
        JSONObject badPoi4 = new JSONObject();
        JSONArray pois4 = new JSONArray();
        JSONObject bp4a = new JSONObject().put("poi_id", "poi_dup").put("hex_id", "H_01").put("canonical", 0);
        JSONObject bp4b = new JSONObject().put("poi_id", "poi_dup").put("hex_id", "H_02").put("canonical", 0);
        pois4.put(bp4a); pois4.put(bp4b);
        badPoi4.put("campaignPois", pois4);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badPoi4).optBoolean("healthy", true),
                "POIs com IDs duplicados devem falhar fail-closed");

        // 5. Injeção de anomalias em tacticalScenes e hazards
        // Caso E: hazards_json malformado
        JSONObject badSc1 = new JSONObject();
        JSONArray scArr1 = new JSONArray();
        JSONObject bsc1 = new JSONObject().put("scene_key", "sc1").put("hazards_json", "{malformed_not_array}");
        scArr1.put(bsc1);
        badSc1.put("tacticalScenes", scArr1);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc1).optBoolean("healthy", true),
                "hazards_json malformado deve falhar fail-closed");

        // Caso F: hazard level < 0
        JSONObject badSc2 = new JSONObject();
        JSONArray scArr2 = new JSONArray();
        JSONArray hz2 = new JSONArray().put(new JSONObject().put("level", -5).put("stealth_dc", 15).put("saving_throw_dc", 15));
        JSONObject bsc2 = new JSONObject().put("scene_key", "sc2").put("hazards_json", hz2.toString());
        scArr2.put(bsc2);
        badSc2.put("tacticalScenes", scArr2);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc2).optBoolean("healthy", true),
                "Perigo com nível negativo deve falhar fail-closed");

        // Caso G: hazard level > 25
        JSONObject badSc3 = new JSONObject();
        JSONArray scArr3 = new JSONArray();
        JSONArray hz3 = new JSONArray().put(new JSONObject().put("level", 26).put("stealth_dc", 15).put("saving_throw_dc", 15));
        JSONObject bsc3 = new JSONObject().put("scene_key", "sc3").put("hazards_json", hz3.toString());
        scArr3.put(bsc3);
        badSc3.put("tacticalScenes", scArr3);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc3).optBoolean("healthy", true),
                "Perigo com nível > 25 deve falhar fail-closed");

        // Caso H: stealth_dc <= 0
        JSONObject badSc4 = new JSONObject();
        JSONArray scArr4 = new JSONArray();
        JSONArray hz4 = new JSONArray().put(new JSONObject().put("level", 2).put("stealth_dc", 0).put("saving_throw_dc", 15));
        JSONObject bsc4 = new JSONObject().put("scene_key", "sc4").put("hazards_json", hz4.toString());
        scArr4.put(bsc4);
        badSc4.put("tacticalScenes", scArr4);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc4).optBoolean("healthy", true),
                "Perigo com stealth_dc <= 0 deve falhar fail-closed");

        // Caso I: saving_throw_dc <= 0
        JSONObject badSc5 = new JSONObject();
        JSONArray scArr5 = new JSONArray();
        JSONArray hz5 = new JSONArray().put(new JSONObject().put("level", 2).put("stealth_dc", 15).put("saving_throw_dc", -2));
        JSONObject bsc5 = new JSONObject().put("scene_key", "sc5").put("hazards_json", hz5.toString());
        scArr5.put(bsc5);
        badSc5.put("tacticalScenes", scArr5);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc5).optBoolean("healthy", true),
                "Perigo com saving_throw_dc <= 0 deve falhar fail-closed");

        // Caso J: scene_key duplicada
        JSONObject badSc6 = new JSONObject();
        JSONArray scArr6 = new JSONArray();
        scArr6.put(new JSONObject().put("scene_key", "same_scene_key"));
        scArr6.put(new JSONObject().put("scene_key", "same_scene_key"));
        badSc6.put("tacticalScenes", scArr6);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badSc6).optBoolean("healthy", true),
                "tacticalScenes com chaves duplicadas deve falhar fail-closed");

        // 6. Injeção de anomalias em quests
        JSONObject badQ1 = new JSONObject();
        JSONArray qArr1 = new JSONArray();
        qArr1.put(new JSONObject().put("title", "")); // Sem título
        badQ1.put("quests", qArr1);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badQ1).optBoolean("healthy", true),
                "Quest sem título deve falhar fail-closed");

        // 7. Injeção de anomalias em chapters
        JSONObject badCh1 = new JSONObject();
        JSONArray chArr1 = new JSONArray();
        chArr1.put(new JSONObject().put("chapter_key", "")); // Sem chapter_key
        badCh1.put("chapters", chArr1);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badCh1).optBoolean("healthy", true),
                "Chapter sem chapter_key deve falhar fail-closed");

        // 8. Injeção de anomalias em factions
        JSONObject badFact1 = new JSONObject();
        JSONArray factArr1 = new JSONArray();
        factArr1.put(new JSONObject().put("name", "")); // Sem nome
        badFact1.put("factions", factArr1);
        check(!KingmakerCampaignImporter.validateCampaignRestore(badFact1).optBoolean("healthy", true),
                "Faction sem nome deve falhar fail-closed");
    }
}
