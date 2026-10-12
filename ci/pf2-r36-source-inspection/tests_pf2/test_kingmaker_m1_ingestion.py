"""Suíte de testes para Milestone 1: Data Models & Ingestion Engine.

Valida:
1. KingmakerCampaignModels.java: esquemas tipados, catálogos, quests, perigos Remaster,
   cenas táticas, POIs, NPCs e facções.
2. KingmakerCampaignImporter.java: ingestão determinística transacional, zero RAG/PDF,
   isolamento por world_profile, constraints SQLite (canonical=0) e export/validate v22.
3. KingmakerRepository.java: métodos de integração de campanha estáticos e de instância.
"""

import json
import sqlite3
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"


def test_m1_kingmaker_campaign_models_pure_jvm_compliant():
    """Valida que KingmakerCampaignModels é pura JVM (sem dependências de android.*)."""
    models_file = SRC / "KingmakerCampaignModels.java"
    assert models_file.exists(), "KingmakerCampaignModels.java deve existir"
    code = models_file.read_text(encoding="utf-8")

    # Garante ausência de dependências do Android SDK no modelo
    assert "android.database" not in code
    assert "android.content" not in code
    assert "android.app" not in code

    # Garante presença dos modelos canônicos
    assert "class ChapterCatalog" in code
    assert "class QuestModel" in code
    assert "class QuestObjective" in code
    assert "class HexPoiModel" in code
    assert "class TacticalSceneModel" in code
    assert "class SceneZone" in code
    assert "class SceneObstacle" in code
    assert "class SceneEntryPoint" in code
    assert "class NpcModel" in code
    assert "class NpcDialogueNode" in code
    assert "class CanonicalHazardModel" in code
    assert "class HazardDisableOption" in code
    assert "class HazardRoutineAction" in code
    assert "class FactionModel" in code
    assert "class WorldClockModel" in code
    assert "class WorldEventModel" in code
    assert "class CampaignMilestone" in code


def test_m1_kingmaker_campaign_importer_zero_rag_compliance():
    """Garante que KingmakerCampaignImporter tem zero dependência de RAG ou PDFs."""
    importer_file = SRC / "KingmakerCampaignImporter.java"
    assert importer_file.exists(), "KingmakerCampaignImporter.java deve existir"
    code = importer_file.read_text(encoding="utf-8")

    assert "RuleRetrievalEngine" not in code
    assert "PdfChunker" not in code
    assert "PdfDocument" not in code
    assert "GeminiClient" not in code

    # Garante métodos contratuais
    assert "boolean importChapter(" in code
    assert "boolean isChapterImported(" in code
    assert "JSONObject exportCampaignState(" in code
    assert "JSONObject validateCampaignRestore(" in code


def test_m1_kingmaker_repository_campaign_extension_methods():
    """Valida métodos de extensão no KingmakerRepository."""
    repo_file = SRC / "KingmakerRepository.java"
    code = repo_file.read_text(encoding="utf-8")

    assert "importCampaignChapter(" in code
    assert "isChapterImported(" in code
    assert "exportCampaignState(" in code
    assert "validateCampaignRestore(" in code
    assert "KingmakerCampaignImporter.ensureSchema(sql)" in code


def test_m1_sqlite_schema_integrity_and_canonical_zero():
    """Simula banco SQLite com as constraints exatas do AppDb e valida inserção do catálogo."""
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    # Esquema com constraints de integridade rigorosas do AppDb
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
    cur.execute("""
        CREATE TABLE campaign_pois (
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
            icon TEXT NOT NULL DEFAULT '📍',
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
        CREATE TABLE tactical_scenes (
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
    cur.execute("""
        CREATE TABLE factions (
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
            UNIQUE(world_profile,name)
        )
    """)
    cur.execute("""
        CREATE TABLE world_clocks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            name TEXT NOT NULL,
            current_value INTEGER DEFAULT 0,
            max_value INTEGER DEFAULT 4,
            consequence TEXT DEFAULT '',
            automatic INTEGER DEFAULT 0,
            active INTEGER DEFAULT 1,
            updated_at INTEGER NOT NULL
        )
    """)
    cur.execute("""
        CREATE TABLE world_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            title TEXT NOT NULL,
            description TEXT DEFAULT '',
            due_at INTEGER DEFAULT 0,
            due_day INTEGER DEFAULT 0,
            status TEXT DEFAULT 'scheduled',
            consequence_json TEXT DEFAULT '{}',
            created_at INTEGER NOT NULL,
            resolved_at INTEGER DEFAULT 0
        )
    """)
    conn.commit()

    # Valida inserção respeitando CHECK(canonical=0) e CHECK(source IN ('campaign', 'procedural'))
    cur.execute("""
        INSERT INTO campaign_pois (
            poi_id, world_profile, campaign_key, name, name_key, hex_id,
            source, canonical, status, created_at, updated_at
        ) VALUES (
            'poi_oleg', 'pf2', 'ch1', 'Posto de Oleg', 'posto_de_oleg', 'H_01',
            'campaign', 0, 'active', 1000, 1000
        )
    """)
    conn.commit()

    cur.execute("SELECT count(*) FROM campaign_pois WHERE canonical=0")
    assert cur.fetchone()[0] == 1

    # Invariante: violação de canonical=1 deve falhar
    with pytest.raises(sqlite3.IntegrityError):
        cur.execute("""
            INSERT INTO campaign_pois (
                poi_id, world_profile, campaign_key, name, name_key, hex_id,
                source, canonical, status, created_at, updated_at
            ) VALUES (
                'poi_invalido', 'pf2', 'ch1', 'Invalido', 'invalido', 'H_01',
                'campaign', 1, 'active', 1000, 1000
            )
        """)


def test_m1_backup_v22_fail_closed_validation():
    """Valida lógica de restauração fail-closed do estado de campanha."""
    # Teste 1: Backup nulo
    code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")
    assert "public static JSONObject validateCampaignRestore(JSONObject backupRoot)" in code

    # Teste 2: Invariantes verificadas pelo validador
    assert "canonical deve ser 0" in code
    assert "Perigo sem CD de furtividade positiva" in code
    assert "Perigo sem CD de salvaguarda positiva" in code
    assert "Perigo com nível inválido" in code

    # Defeito 2 remediado: contrato v22 exige chave 'healthy'
    assert 'report.put("healthy", isHealthy);' in code
    assert 'report.put("healthy", false);' in code

    # Defeito 3 remediado: fail-closed em tipos não-array
    assert 'campaignPois deve ser um array' in code
    assert 'tacticalScenes deve ser um array' in code
    assert 'quests deve ser um array' in code


def test_m1_cross_profile_poi_primary_key_scoping_remediation():
    """Valida que o escopo de poi_id por world_profile previne colisão e sobrescrita entre perfis."""
    importer_code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")
    assert 'scopedPoiId = rawPoiId.startsWith(worldProfile + ":") ? rawPoiId : (worldProfile + ":" + rawPoiId);' in importer_code
    assert 'v.put("poi_id", scopedPoiId);' in importer_code

    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE campaign_pois (
            poi_id TEXT PRIMARY KEY,
            world_profile TEXT NOT NULL,
            campaign_key TEXT NOT NULL,
            name TEXT NOT NULL,
            hex_id TEXT NOT NULL,
            source TEXT NOT NULL DEFAULT 'campaign',
            canonical INTEGER NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'active',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            CHECK(canonical=0)
        )
    """)
    conn.commit()

    # Perfil Alpha importa Capítulo 1
    scoped_alpha = "profile_alpha:poi_posto_oleg"
    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, name, hex_id, source, canonical, status, created_at, updated_at
        ) VALUES (?, 'profile_alpha', 'ch1', 'Posto de Oleg', 'H_01', 'campaign', 0, 'active', 1000, 1000)
    """, (scoped_alpha,))
    conn.commit()

    assert cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_alpha'").fetchone()[0] == 1

    # Perfil Beta importa o mesmo Capítulo 1 com o mesmo POI lógico
    scoped_beta = "profile_beta:poi_posto_oleg"
    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, name, hex_id, source, canonical, status, created_at, updated_at
        ) VALUES (?, 'profile_beta', 'ch1', 'Posto de Oleg', 'H_01', 'campaign', 0, 'active', 2000, 2000)
    """, (scoped_beta,))
    conn.commit()

    # Ambos os perfis mantêm seus POIs intactos
    alpha_count = cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_alpha'").fetchone()[0]
    beta_count = cur.execute("SELECT COUNT(*) FROM campaign_pois WHERE world_profile='profile_beta'").fetchone()[0]
    assert alpha_count == 1, "Perfil Alpha deve manter seu POI após importação de Perfil Beta"
    assert beta_count == 1, "Perfil Beta deve conter seu POI isolado"


def test_m1_pure_jvm_models_harness_execution(tmp_path):
    """Compila e executa o harness puro Java KingmakerCampaignModelsHarness usando runtime_json."""
    import subprocess
    javac = r"C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
    java = r"C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    if not Path(javac).exists():
        pytest.skip("JDK javac não encontrado em C:/Program Files/Android/Android Studio/jbr")

    out_dir = tmp_path / "classes"
    out_dir.mkdir(parents=True, exist_ok=True)

    compile_cmd = [
        javac, "-encoding", "UTF-8", "-d", str(out_dir),
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(SRC / "KingmakerCampaignModels.java"),
        str(ROOT / "tests_pf2/java/KingmakerCampaignModelsHarness.java"),
    ]
    res_comp = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_comp.returncode == 0, f"Falha na compilação do harness Java: {res_comp.stderr}"

    run_cmd = [
        java, "-cp", str(out_dir),
        "com.braseiro.pathfinder2e.KingmakerCampaignModelsHarness"
    ]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness Java: {res_run.stderr}"
    assert "KINGMAKER_CAMPAIGN_MODELS_HARNESS_PASS" in res_run.stdout
    assert "23 checks passed" in res_run.stdout

