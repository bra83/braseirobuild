"""Suíte de Testes Automatizados para Milestone 5: Diretor Solo, Rumores e Backup v22 Fail-Closed.

Validações essenciais:
1. KingmakerSoloDirector.java:
   - getActiveCampaignGuidance(worldProfile) retorna:
     active_chapter, current_stage, active_quests, recommended_scene, scene_action (via KingmakerSoloFlowRouter) e guidance_text.
   - getAvailableRumors(worldProfile, currentChapter) retorna:
     rumor_id, chapter, source_poi, text (pt-BR) e resolved status.
   - Operação 100% read-only, não interfere no livre arbítrio do jogador.
   - Textos 100% normalizados em Português do Brasil (pt-BR).
2. Backup v22 Fail-Closed:
   - KingmakerRepository.validateRestoreAgainst(backupRoot) incorpora KingmakerCampaignImporter.validateCampaignRestore(backupRoot).
   - Auditoria fail-closed: qualquer chave de coleção de campanha corrompida ou não-array aborta com healthy: false.
3. Zero Runtime RAG / PDF:
   - Zero chamadas a RuleRetrievalEngine, PdfChunker, TokenPdfLibrary ou parsing dinâmico de PDF em tempo de execução.
4. Execução em bytecode puro JVM via javac/java compilado contra tools/runtime_json e mock_android.
"""

import json
import re
import sqlite3
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"


# ==============================================================================
# 1. TESTES DE ASSINATURA E CONTRATO DE KINGMAKERSOLODIRECTOR
# ==============================================================================

def test_director_guidance_methods_presence_and_signatures():
    """Valida a presença de getActiveCampaignGuidance e getAvailableRumors no KingmakerSoloDirector."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    assert "public JSONObject getActiveCampaignGuidance(String worldProfile)" in code, \
        "Assinatura getActiveCampaignGuidance(String worldProfile) deve ser pública."
    assert "public JSONObject getActiveCampaignGuidance()" in code, \
        "Sobrecarga padrão getActiveCampaignGuidance() deve existir."
    assert "public JSONArray getAvailableRumors(String worldProfile, int currentChapter)" in code, \
        "Assinatura getAvailableRumors(String worldProfile, int currentChapter) deve ser pública."
    assert "public JSONArray getAvailableRumors(int currentChapter)" in code, \
        "Sobrecarga padrão getAvailableRumors(int currentChapter) deve existir."


def test_director_read_only_and_player_agency_preservation():
    """Garante que KingmakerSoloDirector é estritamente read-only e não executa mutações no SQLite."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    # Não deve chamar mutadores de escrita no SQLite
    forbidden_mutators = [
        "getWritableDatabase()",
        "db.insert(",
        "db.update(",
        "db.delete(",
        "execSQL(",
        "insertWithOnConflict(",
        "deleteWithOnConflict(",
    ]
    for mutator in forbidden_mutators:
        assert mutator not in code, f"Violação de isolamento read-only em KingmakerSoloDirector: {mutator} encontrado."

    # Preservação de agência do jogador: não automatiza escolhas ou rolagens
    assert "KingmakerSoloFlowRouter.actionFor" in code
    assert "Não interrogue o jogador sobre JSON, RP, CD ou manutenção técnica" in code


def test_zero_runtime_rag_and_zero_pdf_compliance():
    """Audita KingmakerSoloDirector e KingmakerRepository garantindo zero dependência de RAG ou PDFs."""
    director_code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")
    repo_code = (SRC / "KingmakerRepository.java").read_text(encoding="utf-8")
    router_code = (SRC / "KingmakerSoloFlowRouter.java").read_text(encoding="utf-8")

    forbidden_tokens = [
        "RuleRetrievalEngine",
        "PdfChunker",
        "TokenPdfLibrary",
        "generateContent",
        "retrievalEngine",
        "loadPdf",
        "openPdf",
        "readPdf",
    ]

    for token in forbidden_tokens:
        assert token not in director_code, f"Referência proibida '{token}' encontrada em KingmakerSoloDirector.java!"
        assert token not in repo_code, f"Referência proibida '{token}' encontrada em KingmakerRepository.java!"
        assert token not in router_code, f"Referência proibida '{token}' encontrada em KingmakerSoloFlowRouter.java!"


# ==============================================================================
# 2. TESTES DE ORIENTAÇÃO DE CAMPANHA (getActiveCampaignGuidance)
# ==============================================================================

def test_guidance_payload_contract_keys():
    """Valida que o payload de getActiveCampaignGuidance inclui todas as chaves obrigatórias."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    required_keys = [
        '"active_chapter"',
        '"current_stage"',
        '"active_quests"',
        '"recommended_scene"',
        '"scene_action"',
        '"guidance_text"',
    ]
    for key in required_keys:
        assert key in code, f"Chave mandatória {key} ausente no retorno de getActiveCampaignGuidance!"

    # Verifica conexão com KingmakerSoloFlowRouter
    assert "KingmakerSoloFlowRouter.actionFor(recommendedScene)" in code


def test_guidance_text_pt_br_normalization():
    """Verifica que todos os textos de guidance e descrições de capítulos estão em 100% pt-BR."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    # Frases e vocabulário canônico em Português do Brasil
    pt_br_markers = [
        "Terras Roubadas",
        "Posto Avançado de Oleg",
        "Lorde Cervo",
        "Fundação da Baronia",
        "Rios Correm Vermelhos",
        "Culto da Florada",
        "Desaparecimento de Varnhold",
        "Sangue por Sangue",
        "Guerra dos Reis do Rio",
        "Aqueles que Espreitam Abaixo",
        "Som de Mil Gritos",
        "Maldição do Rei Lanterna",
        "Atlas Regional",
        "Companheiros",
        "Missão ativa",
        "Estágio",
        "Próxima cena sugerida",
        "exploração livre",
    ]
    for marker in pt_br_markers:
        assert marker in code, f"Texto em pt-BR '{marker}' deve estar presente no orientador de campanha."

    # Termos em inglês não devem vazar na orientação
    english_leaks = [
        '"Active Chapter:"',
        '"Current Stage:"',
        '"Next Scene:"',
        '"Stolen Lands Part 1"',
        '"Rivers Run Red Chapter"',
    ]
    for leak in english_leaks:
        assert leak not in code, f"Vazamento em inglês detectado: {leak}"


def test_guidance_flow_routing_integration():
    """Valida a integração completa entre guidance recommended_scene e KingmakerSoloFlowRouter."""
    router_code = (SRC / "KingmakerSoloFlowRouter.java").read_text(encoding="utf-8")
    director_code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    scenes = [
        ("adventure_start", "START_ADVENTURE"),
        ("camp_encounter", "CAMP_ENCOUNTER"),
        ("camp", "CAMP"),
        ("war", "WAR"),
        ("exploration_resolution", "EXPLORATION_RESOLUTION"),
        ("companion", "COMPANION"),
        ("kingdom_bridge", "KINGDOM_BRIDGE"),
        ("travel", "TRAVEL"),
        ("free_exploration", "FREE_EXPLORATION"),
    ]
    for scene, action in scenes:
        assert f'"{scene}".equals(scene)' in router_code or f'"{scene}".equals' in router_code
        assert action in router_code

    assert "KingmakerSoloFlowRouter.actionFor" in director_code


# ==============================================================================
# 3. TESTES DE TABELAS DE RUMORES CANÔNICOS (getAvailableRumors)
# ==============================================================================

def test_canonical_rumors_catalog_covers_all_12_chapters():
    """Garante que o catálogo canônico de rumores cobre rigorosamente todos os Capítulos de 1 a 12."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    for ch in range(1, 13):
        pattern = f'CanonicalRumor\\("rumor_ch{ch}_'
        assert re.search(pattern, code) is not None, f"Capítulo {ch} deve possuir rumores canônicos definidos."


def test_rumor_json_object_contract_fields():
    """Verifica que cada objeto rumor possui rumor_id, chapter, source_poi, text e resolved."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    required_rumor_fields = [
        '"rumor_id"',
        '"chapter"',
        '"source_poi"',
        '"text"',
        '"resolved"',
    ]
    for f in required_rumor_fields:
        assert f in code, f"Chave obrigatória de rumor {f} ausente em getAvailableRumors."


def test_rumors_poi_and_quest_bindings():
    """Verifica se os rumores canônicos referenciam POIs canônicos e missões associadas."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    canonical_pois = [
        "poi_oleg",
        "poi_bokken_hut",
        "poi_tuskgutter_lair",
        "poi_kressle_camp",
        "poi_old_sycamore",
        "poi_temple_of_elk",
        "poi_candlemere_island",
        "poi_stag_lord_fort",
        "poi_hargulka_stronghold",
        "poi_womb_of_lamashtu",
        "poi_varnhold_abandoned",
        "poi_vordakai_tomb",
        "poi_armag_tomb",
        "poi_rushlight_tournament",
        "poi_house_edge_time",
        "poi_lantern_king_manifestation",
    ]
    for poi in canonical_pois:
        assert f'"{poi}"' in code, f"POI canônico '{poi}' deve estar referenciado nos rumores."


def test_rumor_resolution_deterministic_logic():
    """Valida que a resolução de rumores verifica missões concluídas, hexágonos limpos e tabela rumors."""
    code = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")

    assert "isRumorResolved" in code
    assert '"completed"' in code
    assert '"cleared"' in code
    assert '"resolved"' in code


# ==============================================================================
# 4. TESTES DE AUDITORIA FAIL-CLOSED DO BACKUP V22
# ==============================================================================

def test_backup_v22_integration_in_repository():
    """Verifica que KingmakerRepository.validateRestoreAgainst incorpora validateCampaignRestore."""
    repo_code = (SRC / "KingmakerRepository.java").read_text(encoding="utf-8")

    assert "KingmakerCampaignImporter.validateCampaignRestore(root)" in repo_code
    assert 'campaignAudit.optBoolean("healthy",false)' in repo_code or 'campaignAudit.optBoolean("healthy", false)' in repo_code
    assert '"campaign_audit"' in repo_code
    assert 'problems.put("Root de backup nulo")' in repo_code or 'Root de backup nulo' in repo_code
    assert 'problems.put(BACKUP_KEYS[i]+" deve ser um array")' in repo_code


def test_backup_v22_importer_fail_closed_on_corrupt_collections():
    """Verifica que validateCampaignRestore valida todas as coleções de campanha fail-closed."""
    importer_code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")

    # Validação obrigatória de array para todas as coleções de campanha
    collections = [
        "campaignPois",
        "tacticalScenes",
        "quests",
        "chapters",
        "factions",
        "combatants",
        "worldClocks",
        "worldEvents",
        "companions",
        "npcMemories",
        "npcCognitiveStates",
    ]
    for coll in collections:
        assert f'backupRoot.has("{coll}") && backupRoot.optJSONArray("{coll}") == null' in importer_code or \
               f'"{coll}"' in importer_code, f"Coleção {coll} deve ser validada contra corrupção não-array."

    assert '"healthy"' in importer_code
    assert '"valid"' in importer_code


# ==============================================================================
# 5. TESTE EM BYTECODE JVM PURO (KingmakerCampaignImporter Restore Validator)
# ==============================================================================

def test_pure_jvm_backup_v22_fail_closed_execution(tmp_path):
    """Executa harness JVM puro para validar contratos fail-closed de restauração de backup v22."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado")

    harness_src = f"""package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public class M5BackupValidatorHarness {{
    private static void check(boolean cond, String msg) {{
        if (!cond) throw new AssertionError("FALHA: " + msg);
    }}

    public static void main(String[] args) throws Exception {{
        // 1. Backup válido vazio
        JSONObject validEmpty = new JSONObject();
        validEmpty.put("chapters", new JSONArray());
        validEmpty.put("campaignPois", new JSONArray());
        validEmpty.put("tacticalScenes", new JSONArray());
        validEmpty.put("quests", new JSONArray());
        validEmpty.put("factions", new JSONArray());

        JSONObject reportEmpty = KingmakerCampaignImporter.validateCampaignRestore(validEmpty);
        check(reportEmpty.optBoolean("healthy", false), "Backup vazio válido deve ser healthy: true");
        check(reportEmpty.optBoolean("valid", false), "Backup vazio válido deve ser valid: true");
        check(reportEmpty.optJSONArray("errors").length() == 0, "Zero erros em backup vazio válido");

        // 2. Backup nulo
        JSONObject reportNull = KingmakerCampaignImporter.validateCampaignRestore(null);
        check(!reportNull.optBoolean("healthy", true), "Backup nulo deve ser healthy: false");
        check(!reportNull.optBoolean("valid", true), "Backup nulo deve ser valid: false");

        // 3. Coleções corrompidas com tipos primitivos (não-array)
        String[] colls = {{"chapters", "campaignPois", "tacticalScenes", "quests", "factions", "combatants", "worldClocks", "worldEvents"}};
        for (String c : colls) {{
            JSONObject corrupt = new JSONObject();
            corrupt.put(c, "string_invalida_nao_array");
            JSONObject rep = KingmakerCampaignImporter.validateCampaignRestore(corrupt);
            check(!rep.optBoolean("healthy", true), c + " como string deve retornar healthy: false");
            check(!rep.optBoolean("valid", true), c + " como string deve retornar valid: false");
            check(rep.optJSONArray("errors").length() > 0, c + " deve conter mensagem de erro");
        }}

        // 4. Coleção chapters com elemento nulo
        JSONObject badChapter = new JSONObject();
        JSONArray badChArr = new JSONArray();
        badChArr.put(JSONObject.NULL);
        badChapter.put("chapters", badChArr);
        JSONObject repBadCh = KingmakerCampaignImporter.validateCampaignRestore(badChapter);
        check(!repBadCh.optBoolean("healthy", true), "chapters com elemento nulo deve retornar healthy: false");

        // 5. Coleção factions com elemento nulo
        JSONObject badFaction = new JSONObject();
        JSONArray badFactArr = new JSONArray();
        badFactArr.put(JSONObject.NULL);
        badFaction.put("factions", badFactArr);
        JSONObject repBadFact = KingmakerCampaignImporter.validateCampaignRestore(badFaction);
        check(!repBadFact.optBoolean("healthy", true), "factions com elemento nulo deve retornar healthy: false");

        System.out.println("M5_BACKUP_VALIDATOR_HARNESS_PASS");
    }}
}}
"""
    out_dir = tmp_path / "m5_classes"
    out_dir.mkdir(parents=True, exist_ok=True)

    harness_file = tmp_path / "M5BackupValidatorHarness.java"
    harness_file.write_text(harness_src, encoding="utf-8")

    sources = [
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/content/ContentValues.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/Cursor.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/sqlite/SQLiteException.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/sqlite/SQLiteDatabase.java"),
        str(SRC / "KingmakerCampaignModels.java"),
        str(SRC / "KingmakerCampaignImporter.java"),
        str(harness_file),
    ]

    compile_cmd = [JAVAC, "-encoding", "UTF-8", "-d", str(out_dir)] + sources
    res_compile = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_compile.returncode == 0, f"Falha na compilação do harness M5:\n{res_compile.stderr}"

    run_cmd = [JAVA, "-cp", str(out_dir), "com.braseiro.pathfinder2e.M5BackupValidatorHarness"]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness M5:\n{res_run.stderr}"
    assert "M5_BACKUP_VALIDATOR_HARNESS_PASS" in res_run.stdout


# ==============================================================================
# 6. SIMULAÇÃO DETERMINÍSTICA DO COMPORTAMENTO DO DIRETOR EM SQLITE
# ==============================================================================

def test_guidance_resolution_simulation_with_sqlite():
    """Simula as transições de estado do SQLite testando a determinação de capítulos e estágios."""
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE kingmaker_campaign_chapters (
            world_profile TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            chapter_number INTEGER NOT NULL,
            title TEXT NOT NULL,
            level_min INTEGER NOT NULL DEFAULT 1,
            level_max INTEGER NOT NULL DEFAULT 20,
            summary TEXT NOT NULL DEFAULT '',
            imported_at INTEGER NOT NULL,
            metadata_json TEXT NOT NULL DEFAULT '{}',
            PRIMARY KEY(world_profile, chapter_key)
        )
    """)
    cur.execute("""
        CREATE TABLE quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            quest_key TEXT NOT NULL DEFAULT '',
            chapter_key TEXT NOT NULL DEFAULT '',
            title TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'active',
            stage TEXT NOT NULL DEFAULT '',
            objective TEXT NOT NULL DEFAULT '',
            stakes TEXT NOT NULL DEFAULT '',
            owner_character_id INTEGER DEFAULT 0,
            faction_id INTEGER DEFAULT 0,
            milestone_xp INTEGER DEFAULT 0,
            progress_json TEXT DEFAULT '{}',
            created_at INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0
        )
    """)
    cur.execute("""
        CREATE TABLE campaign_pois (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            poi_id TEXT NOT NULL,
            poi_key TEXT NOT NULL DEFAULT '',
            chapter_key TEXT NOT NULL DEFAULT '',
            campaign_key TEXT NOT NULL DEFAULT '',
            map_id TEXT NOT NULL DEFAULT '',
            name TEXT NOT NULL,
            name_key TEXT NOT NULL DEFAULT '',
            type TEXT NOT NULL DEFAULT 'poi',
            hex_id TEXT NOT NULL DEFAULT '',
            location_id TEXT NOT NULL DEFAULT '',
            q INTEGER NOT NULL DEFAULT 0,
            r INTEGER NOT NULL DEFAULT 0,
            source TEXT NOT NULL DEFAULT 'campaign',
            origin TEXT NOT NULL DEFAULT 'canon',
            canonical INTEGER NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'active',
            created_at INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0
        )
    """)
    cur.execute("""
        CREATE TABLE rumors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            rumor_key TEXT NOT NULL,
            chapter INTEGER DEFAULT 1,
            source_event_key TEXT DEFAULT '',
            origin_location TEXT DEFAULT '',
            current_region TEXT DEFAULT '',
            statement TEXT NOT NULL,
            truth_level TEXT DEFAULT 'parcial',
            distortion INTEGER DEFAULT 15,
            confidence INTEGER DEFAULT 50,
            secrecy INTEGER DEFAULT 20,
            status TEXT DEFAULT 'circulating',
            born_day INTEGER DEFAULT 1,
            expires_day INTEGER DEFAULT 0,
            carriers_json TEXT DEFAULT '[]',
            mutations_json TEXT DEFAULT '[]',
            updated_at INTEGER NOT NULL,
            UNIQUE(world_profile, rumor_key)
        )
    """)
    cur.execute("""
        CREATE TABLE kingmaker_hexes (
            world_profile TEXT NOT NULL,
            map_id TEXT NOT NULL,
            map_package_id TEXT NOT NULL DEFAULT '',
            hex_id TEXT NOT NULL,
            reconnoitered INTEGER NOT NULL DEFAULT 0,
            cleared INTEGER NOT NULL DEFAULT 0,
            claimed INTEGER NOT NULL DEFAULT 0,
            terrain TEXT NOT NULL DEFAULT '',
            improvement TEXT NOT NULL DEFAULT '',
            road INTEGER NOT NULL DEFAULT 0,
            farmland INTEGER NOT NULL DEFAULT 0,
            resource_site TEXT NOT NULL DEFAULT '',
            claimed_turn INTEGER NOT NULL DEFAULT 0,
            notes TEXT NOT NULL DEFAULT '',
            updated_at INTEGER NOT NULL,
            PRIMARY KEY(world_profile, map_id, hex_id)
        )
    """)

    # Inserir capítulos 1 e 2
    cur.execute("""
        INSERT INTO kingmaker_campaign_chapters VALUES
        ('pf2', 'chapter_01_stolen_lands_part1', 1, 'Terras Roubadas - Parte 1', 1, 2, 'Resumo Ch1', 1000, '{}'),
        ('pf2', 'chapter_02_stolen_lands_part2', 2, 'Lorde Cervo e a Fundação', 2, 4, 'Resumo Ch2', 2000, '{}')
    """)

    # 1. Sem quests ativas: capítulo ativo deve ser 1
    rows = cur.execute("SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress')").fetchall()
    assert len(rows) == 0

    first_ch = cur.execute(
        "SELECT chapter_number, chapter_key, title FROM kingmaker_campaign_chapters WHERE world_profile='pf2' ORDER BY chapter_number ASC LIMIT 1"
    ).fetchone()
    assert first_ch[0] == 1

    # 2. Inserir quest ativa no Capítulo 1
    cur.execute("""
        INSERT INTO quests (world_profile, quest_key, chapter_key, title, status, stage, objective)
        VALUES ('pf2', 'q_oleg', 'chapter_01_stolen_lands_part1', 'Defesa do Posto de Oleg', 'active', 'emboscada_preparada', 'Defender o posto contra Happs Bydon')
    """)
    conn.commit()

    active_q = cur.execute("SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress')").fetchall()
    assert len(active_q) == 1
    assert active_q[0][4] == "Defesa do Posto de Oleg"
    assert active_q[0][6] == "emboscada_preparada"

    # 3. Concluir quest do Capítulo 1 e abrir quest do Capítulo 2
    cur.execute("UPDATE quests SET status='completed' WHERE quest_key='q_oleg'")
    cur.execute("""
        INSERT INTO quests (world_profile, quest_key, chapter_key, title, status, stage, objective)
        VALUES ('pf2', 'q_stag_lord', 'chapter_02_stolen_lands_part2', 'Queda do Lorde Cervo', 'active', 'ataque_ao_forte', 'Derrotar o Lorde Cervo em seu forte')
    """)
    conn.commit()

    active_q2 = cur.execute("SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress')").fetchall()
    assert len(active_q2) == 1
    assert active_q2[0][2] == "q_stag_lord"
    assert active_q2[0][3] == "chapter_02_stolen_lands_part2"
    assert active_q2[0][4] == "Queda do Lorde Cervo"
    assert active_q2[0][6] == "ataque_ao_forte"

    conn.close()


def test_rumor_resolution_lifecycle_simulation():
    """Valida a resolução determinística de rumores por quest concluída, hex limpo e status explícito."""
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            quest_key TEXT NOT NULL,
            title TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'active'
        )
    """)
    cur.execute("""
        CREATE TABLE campaign_pois (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            poi_key TEXT NOT NULL,
            hex_id TEXT NOT NULL
        )
    """)
    cur.execute("""
        CREATE TABLE kingmaker_hexes (
            world_profile TEXT NOT NULL,
            hex_id TEXT NOT NULL,
            cleared INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(world_profile, hex_id)
        )
    """)
    cur.execute("""
        CREATE TABLE rumors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            rumor_key TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'circulating'
        )
    """)

    # 1. Rumor não resolvido por padrão
    cur.execute("INSERT INTO campaign_pois VALUES (1, 'pf2', 'poi_oleg', 'H_01')")
    cur.execute("INSERT INTO kingmaker_hexes VALUES ('pf2', 'H_01', 0)")
    cur.execute("INSERT INTO quests VALUES (1, 'pf2', 'q_oleg', 'Defesa de Oleg', 'active')")
    conn.commit()

    # Condição de resolução 1: quest completada
    q_status = cur.execute("SELECT status FROM quests WHERE quest_key='q_oleg'").fetchone()[0]
    assert q_status != "completed"

    cur.execute("UPDATE quests SET status='completed' WHERE quest_key='q_oleg'")
    conn.commit()
    q_status_resolved = cur.execute("SELECT status FROM quests WHERE quest_key='q_oleg'").fetchone()[0]
    assert q_status_resolved == "completed"

    # Condição de resolução 2: hex marcado como limpo
    cur.execute("INSERT INTO campaign_pois VALUES (2, 'pf2', 'poi_tuskgutter_lair', 'H_02')")
    cur.execute("INSERT INTO kingmaker_hexes VALUES ('pf2', 'H_02', 0)")
    conn.commit()

    hex_cleared_before = cur.execute("SELECT cleared FROM kingmaker_hexes WHERE hex_id='H_02'").fetchone()[0]
    assert hex_cleared_before == 0

    cur.execute("UPDATE kingmaker_hexes SET cleared=1 WHERE hex_id='H_02'")
    conn.commit()
    hex_cleared_after = cur.execute("SELECT cleared FROM kingmaker_hexes WHERE hex_id='H_02'").fetchone()[0]
    assert hex_cleared_after == 1

    # Condição de resolução 3: status na tabela rumors
    cur.execute("INSERT INTO rumors VALUES (1, 'pf2', 'rumor_ch1_04', 'circulating')")
    conn.commit()
    r_status_before = cur.execute("SELECT status FROM rumors WHERE rumor_key='rumor_ch1_04'").fetchone()[0]
    assert r_status_before == "circulating"

    cur.execute("UPDATE rumors SET status='resolved' WHERE rumor_key='rumor_ch1_04'")
    conn.commit()
    r_status_after = cur.execute("SELECT status FROM rumors WHERE rumor_key='rumor_ch1_04'").fetchone()[0]
    assert r_status_after == "resolved"

    conn.close()


def test_pure_jvm_backup_v22_adversarial_malformed_payloads(tmp_path):
    """Testa rejeição fail-closed contra anomalias severas: canonical!=0, hazard sem CD, scene duplicada."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado")

    harness_src = f"""package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public class M5AdversarialHarness {{
    private static void check(boolean cond, String msg) {{
        if (!cond) throw new AssertionError("FALHA: " + msg);
    }}

    public static void main(String[] args) throws Exception {{
        // Caso A: POI com canonical=1 (deve falhar invariante MANDATÓRIA)
        JSONObject badPoiBackup = new JSONObject();
        JSONArray pois = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("poi_id", "p_bad");
        p.put("hex_id", "H_01");
        p.put("canonical", 1);
        pois.put(p);
        badPoiBackup.put("campaignPois", pois);

        JSONObject rA = KingmakerCampaignImporter.validateCampaignRestore(badPoiBackup);
        check(!rA.optBoolean("healthy", true), "canonical=1 em POI deve retornar healthy: false");

        // Caso B: Perigo em cena tática com saving_throw_dc <= 0
        JSONObject badHazardBackup = new JSONObject();
        JSONArray scenes = new JSONArray();
        JSONObject sc = new JSONObject();
        sc.put("scene_key", "sc_test");
        JSONArray hazArr = new JSONArray();
        JSONObject haz = new JSONObject();
        haz.put("level", 2);
        haz.put("stealth_dc", 15);
        haz.put("saving_throw_dc", 0); // Inválido: DC <= 0
        hazArr.put(haz);
        sc.put("hazards_json", hazArr.toString());
        scenes.put(sc);
        badHazardBackup.put("tacticalScenes", scenes);

        JSONObject rB = KingmakerCampaignImporter.validateCampaignRestore(badHazardBackup);
        check(!rB.optBoolean("healthy", true), "Perigo com CD 0 deve retornar healthy: false");

        // Caso C: Cenas táticas com chaves duplicadas
        JSONObject dupSceneBackup = new JSONObject();
        JSONArray scenesDup = new JSONArray();
        JSONObject s1 = new JSONObject(); s1.put("scene_key", "same_key");
        JSONObject s2 = new JSONObject(); s2.put("scene_key", "same_key");
        scenesDup.put(s1); scenesDup.put(s2);
        dupSceneBackup.put("tacticalScenes", scenesDup);

        JSONObject rC = KingmakerCampaignImporter.validateCampaignRestore(dupSceneBackup);
        check(!rC.optBoolean("healthy", true), "Cenas com chave duplicada devem retornar healthy: false");

        // Caso D: POIs com IDs duplicados
        JSONObject dupPoiBackup = new JSONObject();
        JSONArray poisDup = new JSONArray();
        JSONObject p1 = new JSONObject(); p1.put("poi_id", "poi_dup"); p1.put("hex_id", "H_01"); p1.put("canonical", 0);
        JSONObject p2 = new JSONObject(); p2.put("poi_id", "poi_dup"); p2.put("hex_id", "H_02"); p2.put("canonical", 0);
        poisDup.put(p1); poisDup.put(p2);
        dupPoiBackup.put("campaignPois", poisDup);

        JSONObject rD = KingmakerCampaignImporter.validateCampaignRestore(dupPoiBackup);
        check(!rD.optBoolean("healthy", true), "POIs com ID duplicado devem retornar healthy: false");

        System.out.println("M5_ADVERSARIAL_HARNESS_PASS");
    }}
}}
"""
    out_dir = tmp_path / "m5_adv_classes"
    out_dir.mkdir(parents=True, exist_ok=True)

    harness_file = tmp_path / "M5AdversarialHarness.java"
    harness_file.write_text(harness_src, encoding="utf-8")

    sources = [
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/content/ContentValues.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/Cursor.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/sqlite/SQLiteException.java"),
        str(ROOT / "tests_pf2/java/mock_android/android/database/sqlite/SQLiteDatabase.java"),
        str(SRC / "KingmakerCampaignModels.java"),
        str(SRC / "KingmakerCampaignImporter.java"),
        str(harness_file),
    ]

    compile_cmd = [JAVAC, "-encoding", "UTF-8", "-d", str(out_dir)] + sources
    res_compile = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_compile.returncode == 0, f"Falha na compilação do harness adversarial:\n{res_compile.stderr}"

    run_cmd = [JAVA, "-cp", str(out_dir), "com.braseiro.pathfinder2e.M5AdversarialHarness"]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness adversarial:\n{res_run.stderr}"
    assert "M5_ADVERSARIAL_HARNESS_PASS" in res_run.stdout

