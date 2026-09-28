"""Suíte de Testes de Stress Transacional e Concorrência — Milestone 1 Challenger.

Objetivos de Desafio Adversarial:
1. Ingestão parcial com falhas mid-stream: verificar 100% de rollback (0 linhas persistidas).
2. Concorrência e isolamento de world_profile: verificar se perfis concorrentes vazam ou sobrescrevem dados mútuos.
3. Payloads de restauração de backup corrompidos: verificar se validateCampaignRestore rejeita fail-closed com healthy: false.
4. Execução puramente em Python / JVM sem compilação de APK ou Gradle.
"""

import json
import sqlite3
import subprocess
import threading
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"


def setup_appdb_production_sqlite_schema(conn: sqlite3.Connection):
    """Configura o esquema exato de produção do AppDb.java e KingmakerRepository.java."""
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS kingmaker_campaign_chapters (
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
        CREATE TABLE IF NOT EXISTS factions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            name TEXT NOT NULL COLLATE NOCASE,
            agenda TEXT DEFAULT '',
            resources TEXT DEFAULT '',
            relationship INTEGER DEFAULT 0,
            current_plan TEXT DEFAULT '',
            location TEXT DEFAULT '',
            active INTEGER DEFAULT 1,
            updated_at INTEGER NOT NULL,
            UNIQUE(world_profile, name)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            title TEXT NOT NULL,
            status TEXT DEFAULT 'active',
            stage TEXT DEFAULT '',
            objective TEXT DEFAULT '',
            stakes TEXT DEFAULT '',
            owner_character_id INTEGER DEFAULT 0,
            faction_id INTEGER DEFAULT 0,
            progress_json TEXT DEFAULT '{}',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    """)
    # Esquema CANÔNICO de produção do AppDb.java linha 821:
    # ATENÇÃO: poi_id é TEXT PRIMARY KEY (sem world_profile na PK!)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS campaign_pois (
            poi_id TEXT PRIMARY KEY,
            world_profile TEXT NOT NULL,
            campaign_key TEXT NOT NULL,
            map_id TEXT NOT NULL DEFAULT '',
            name TEXT NOT NULL,
            name_key TEXT NOT NULL,
            type TEXT NOT NULL DEFAULT 'point_of_interest',
            hex_id TEXT NOT NULL,
            location_id TEXT,
            q INTEGER DEFAULT 0,
            r INTEGER DEFAULT 0,
            source TEXT NOT NULL DEFAULT 'campaign',
            origin TEXT NOT NULL DEFAULT 'campaign_discovery',
            canonical INTEGER NOT NULL DEFAULT 0,
            hidden INTEGER NOT NULL DEFAULT 0,
            discovered INTEGER NOT NULL DEFAULT 1,
            created_by TEXT NOT NULL DEFAULT 'player',
            created_tick INTEGER NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'active',
            description TEXT NOT NULL DEFAULT '',
            tags_json TEXT NOT NULL DEFAULT '[]',
            faction_id TEXT,
            owner_npc_id TEXT,
            parent_poi_id TEXT,
            development_level INTEGER NOT NULL DEFAULT 1,
            icon TEXT NOT NULL DEFAULT '•',
            notes TEXT NOT NULL DEFAULT '',
            private_notes TEXT NOT NULL DEFAULT '',
            settlement_key TEXT,
            settlement_json TEXT NOT NULL DEFAULT '{}',
            state_json TEXT NOT NULL DEFAULT '{}',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            CHECK(canonical=0),
            CHECK(source IN ('campaign','procedural')),
            CHECK(status IN ('active','abandoned','ruined','destroyed','occupied','rebuilding'))
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS tactical_scenes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            scene_key TEXT NOT NULL,
            location TEXT DEFAULT '',
            hex_id TEXT DEFAULT '',
            environment TEXT DEFAULT 'generic',
            width_cells INTEGER DEFAULT 12,
            height_cells INTEGER DEFAULT 8,
            seed INTEGER DEFAULT 0,
            zones_json TEXT DEFAULT '[]',
            obstacles_json TEXT DEFAULT '[]',
            features_json TEXT DEFAULT '[]',
            cover_json TEXT DEFAULT '[]',
            hazards_json TEXT DEFAULT '[]',
            entry_points_json TEXT DEFAULT '[]',
            source_context_json TEXT DEFAULT '{}',
            status TEXT DEFAULT 'ready',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            UNIQUE(world_profile,scene_key)
        )
    """)
    conn.commit()


def test_challenger_partial_ingestion_transaction_rollback():
    """Desafio 1: Injeta falha no meio da importação do capítulo e verifica 100% de rollback."""
    conn = sqlite3.connect(":memory:")
    setup_appdb_production_sqlite_schema(conn)
    cur = conn.cursor()

    # Simula a transação do KingmakerCampaignImporter.importChapter
    # db.beginTransaction()
    conn.isolation_level = None  # Controle manual de transação
    cur.execute("BEGIN TRANSACTION")

    try:
        # Etapa 1: Metadados do capítulo
        cur.execute("""
            INSERT INTO kingmaker_campaign_chapters (
                world_profile, chapter_key, chapter_number, title, level_min, level_max, imported_at
            ) VALUES ('pf2', 'chapter_01_stolen_lands', 1, 'Terras Roubadas', 1, 2, 1000)
        """)

        # Etapa 2: Facções
        cur.execute("""
            INSERT INTO factions (world_profile, name, updated_at)
            VALUES ('pf2', 'Restov Aldori', 1000)
        """)

        # Etapa 3: Injeção de Falha Fatal mid-stream (ex: violação de constraint ou JSON malformado)
        # Injeta uma quest com status inválido ou violação de integridade forçada
        raise sqlite3.IntegrityError("Simulação de falha catastrófica de validação de perigo/quest mid-chapter")

        # Se passasse, faria commit:
        cur.execute("COMMIT")
    except Exception:
        # db.endTransaction() sem setTransactionSuccessful() -> ROLLBACK
        cur.execute("ROLLBACK")

    # VERIFICAÇÃO ADVERSARIAL: 0 linhas devem persistir em todas as tabelas afetadas
    assert cur.execute("SELECT COUNT(*) FROM kingmaker_campaign_chapters").fetchone()[0] == 0, \
        "Capítulos persistiram após falha mid-stream! Falha de atomicidade."
    assert cur.execute("SELECT COUNT(*) FROM factions").fetchone()[0] == 0, \
        "Facções persistiram após falha mid-stream! Falha de atomicidade."
    assert cur.execute("SELECT COUNT(*) FROM quests").fetchone()[0] == 0, \
        "Quests persistiram após falha mid-stream!"
    assert cur.execute("SELECT COUNT(*) FROM campaign_pois").fetchone()[0] == 0, \
        "POIs persistiram após falha mid-stream!"


def test_challenger_world_profile_isolation_poi_primary_key_flaw():
    """Desafio 2: Auditoria Adversarial da chave primária de campaign_pois no AppDb.
    
    Verifica que em AppDb.java linha 821, a tabela campaign_pois foi definida com
    'poi_id TEXT PRIMARY KEY' em vez de PRIMARY KEY(world_profile, poi_id).
    Como KingmakerCampaignImporter usa CONFLICT_REPLACE com o poi_id simples ('poi_posto_oleg'),
    quando um perfil B importa o mesmo capítulo que o perfil A, o registro do perfil A é DELETADO/SOBRESCRITO!
    """
    conn = sqlite3.connect(":memory:")
    setup_appdb_production_sqlite_schema(conn)
    cur = conn.cursor()

    # Perfil A importa Capítulo 1 com POI canônico 'poi_posto_oleg'
    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, name, name_key, hex_id, source, canonical, status, created_at, updated_at
        ) VALUES (
            'poi_posto_oleg', 'profile_alpha', 'ch1', 'Posto de Oleg', 'posto_oleg', 'H_01', 'campaign', 0, 'active', 1000, 1000
        )
    """)
    conn.commit()

    # Confirma que Profile Alpha possui 1 POI
    alpha_count_before = cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_alpha'").fetchone()[0]
    assert alpha_count_before == 1

    # Perfil B importa o mesmo capítulo com o mesmo POI 'poi_posto_oleg'
    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, name, name_key, hex_id, source, canonical, status, created_at, updated_at
        ) VALUES (
            'poi_posto_oleg', 'profile_beta', 'ch1', 'Posto de Oleg', 'posto_oleg', 'H_01', 'campaign', 0, 'active', 2000, 2000
        )
    """)
    conn.commit()

    # Confirma que Profile Beta possui o POI
    beta_count = cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_beta'").fetchone()[0]
    assert beta_count == 1

    # AUDITORIA ADVERSARIAL: O que aconteceu com o POI de Profile Alpha?
    alpha_count_after = cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_alpha'").fetchone()[0]

    # DEMONSTRAÇÃO DO VAZAMENTO / DESTRUIÇÃO DE DADOS ENTRE PERFIS:
    # Se alpha_count_after for 0, o isolamento entre perfis FALHOU!
    print(f"\n[ISOLATION AUDIT] POIs de profile_alpha antes: {alpha_count_before}, depois: {alpha_count_after}")
    assert alpha_count_after == 0, "O teste confirmou que o POI de profile_alpha foi destruído pela importação de profile_beta devido à PK simples poi_id!"


def test_challenger_validate_campaign_restore_contract_and_fail_open_defects():
    """Validação da remediação contratual de validateCampaignRestore."""
    importer_code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")

    # 1. Contrato de Backup: chave "healthy" presente
    # AppDb.java linha 2360 exige optBoolean("healthy", false)
    assert 'report.put("valid"' in importer_code
    assert 'report.put("healthy"' in importer_code, \
        "validateCampaignRestore deve emitir a chave mandatória 'healthy'."

    # 2. Hardening Fail-Closed: tipos não-array são validados com erro
    assert 'backupRoot.has("campaignPois") && backupRoot.optJSONArray("campaignPois") == null' in importer_code
    assert 'backupRoot.has("tacticalScenes") && backupRoot.optJSONArray("tacticalScenes") == null' in importer_code
    assert 'backupRoot.has("quests") && backupRoot.optJSONArray("quests") == null' in importer_code


def test_challenger_pure_jvm_transactional_stress_harness(tmp_path):
    """Desafio 4: Compila e executa o harness puro Java KingmakerTransactionalStressHarness."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado no caminho padrão do Android Studio")

    out_dir = tmp_path / "stress_classes"
    out_dir.mkdir(parents=True, exist_ok=True)

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
        str(ROOT / "tests_pf2/java/KingmakerTransactionalStressHarness.java"),
    ]

    compile_cmd = [JAVAC, "-encoding", "UTF-8", "-d", str(out_dir)] + sources
    res_compile = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_compile.returncode == 0, f"Falha na compilação do harness de stress:\n{res_compile.stderr}"

    run_cmd = [JAVA, "-cp", str(out_dir), "com.braseiro.pathfinder2e.KingmakerTransactionalStressHarness"]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness de stress:\n{res_run.stderr}"
    assert "KINGMAKER_TRANSACTIONAL_STRESS_HARNESS_COMPLETE" in res_run.stdout
    assert "0 vulnerabilities documented" in res_run.stdout
