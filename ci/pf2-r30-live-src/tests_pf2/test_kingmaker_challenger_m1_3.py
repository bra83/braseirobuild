"""Suíte de Testes Adversariais do Challenger 3 — Milestone 1.

Validações Específicas do Challenger 3:
1. Re-test cross-profile POI scoping: dois perfis distintos ('profile_alpha' e 'profile_beta')
   importam o mesmo capítulo sem colisão, overwrite ou deleção de POIs.
2. Re-test backup v22 restore contract: validateCampaignRestore retorna healthy: true em backups válidos
   e healthy: false em backups corrompidos.
3. Re-test fail-closed hardening: tipos não-array (campaignPois: 'string', quests: 123, tacticalScenes: true)
   são estritamente rejeitados com healthy: false.
4. Execução do harness puro JVM KingmakerChallenger3StressHarness sem tarefas Gradle.
"""

import json
import sqlite3
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"


def setup_production_campaign_pois_table(conn: sqlite3.Connection):
    """Cria tabela campaign_pois com o esquema canônico estrito do AppDb.java linha 821.
    Nota: A chave primária é estritamente poi_id TEXT PRIMARY KEY."""
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS campaign_pois (
            poi_id TEXT PRIMARY KEY,
            world_profile TEXT NOT NULL,
            campaign_key TEXT NOT NULL,
            poi_key TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            name TEXT NOT NULL,
            hex_id TEXT NOT NULL,
            source TEXT NOT NULL DEFAULT 'campaign',
            canonical INTEGER NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'active',
            parent_poi_id TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            CHECK(canonical=0),
            CHECK(source IN ('campaign','procedural'))
        )
    """)
    conn.commit()


def test_challenger3_cross_profile_poi_scoping_no_collision_or_overwrite():
    """Valida que o escopo <world_profile>:<poi_id> impede qualquer colisão ou deleção entre perfis distintos."""
    conn = sqlite3.connect(":memory:")
    setup_production_campaign_pois_table(conn)
    cur = conn.cursor()

    raw_poi_id = "poi_posto_oleg"
    raw_child_id = "poi_oleg_armazem"

    # Perfil Alpha importa Capítulo 1
    profile_alpha = "profile_alpha"
    scoped_alpha_parent = f"{profile_alpha}:{raw_poi_id}"
    scoped_alpha_child = f"{profile_alpha}:{raw_child_id}"

    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, poi_key, chapter_key, name, hex_id, parent_poi_id, created_at, updated_at
        ) VALUES (?, ?, 'ch1', ?, 'ch1', 'Posto de Oleg', 'H_01', NULL, 1000, 1000)
    """, (scoped_alpha_parent, profile_alpha, raw_poi_id))

    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, poi_key, chapter_key, name, hex_id, parent_poi_id, created_at, updated_at
        ) VALUES (?, ?, 'ch1', ?, 'ch1', 'Armazém de Oleg', 'H_01', ?, 1000, 1000)
    """, (scoped_alpha_child, profile_alpha, raw_child_id, scoped_alpha_parent))
    conn.commit()

    # Confirma que Profile Alpha possui 2 POIs
    alpha_pois = cur.execute("SELECT poi_id, parent_poi_id FROM campaign_pois WHERE world_profile=?", (profile_alpha,)).fetchall()
    assert len(alpha_pois) == 2

    # Perfil Beta importa exatamente o mesmo Capítulo 1 com os mesmos POIs canônicos
    profile_beta = "profile_beta"
    scoped_beta_parent = f"{profile_beta}:{raw_poi_id}"
    scoped_beta_child = f"{profile_beta}:{raw_child_id}"

    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, poi_key, chapter_key, name, hex_id, parent_poi_id, created_at, updated_at
        ) VALUES (?, ?, 'ch1', ?, 'ch1', 'Posto de Oleg', 'H_01', NULL, 2000, 2000)
    """, (scoped_beta_parent, profile_beta, raw_poi_id))

    cur.execute("""
        INSERT OR REPLACE INTO campaign_pois (
            poi_id, world_profile, campaign_key, poi_key, chapter_key, name, hex_id, parent_poi_id, created_at, updated_at
        ) VALUES (?, ?, 'ch1', ?, 'ch1', 'Armazém de Oleg', 'H_01', ?, 2000, 2000)
    """, (scoped_beta_child, profile_beta, raw_child_id, scoped_beta_parent))
    conn.commit()

    # Confirma que Profile Beta possui 2 POIs
    beta_pois = cur.execute("SELECT poi_id, parent_poi_id FROM campaign_pois WHERE world_profile=?", (profile_beta,)).fetchall()
    assert len(beta_pois) == 2

    # AUDITORIA ADVERSARIAL: Profile Alpha deve permanecer com exatamente 2 POIs
    alpha_pois_after = cur.execute("SELECT poi_id, parent_poi_id FROM campaign_pois WHERE world_profile=?", (profile_alpha,)).fetchall()
    assert len(alpha_pois_after) == 2, "Profile Alpha teve POIs deletados ou sobrescritos!"

    # Verifica isolamento de chave primária e referências parent
    alpha_dict = dict(alpha_pois_after)
    assert scoped_alpha_parent in alpha_dict
    assert scoped_alpha_child in alpha_dict
    assert alpha_dict[scoped_alpha_child] == scoped_alpha_parent

    beta_dict = dict(beta_pois)
    assert scoped_beta_parent in beta_dict
    assert scoped_beta_child in beta_dict
    assert beta_dict[scoped_beta_child] == scoped_beta_parent


def test_challenger3_backup_v22_restore_contract_code_audit():
    """Audita a implementação Java de validateCampaignRestore para conformidade estrita com o contrato v22."""
    code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")

    # Contrato healthy presente em todos os ramos
    assert 'report.put("healthy", isHealthy);' in code
    assert 'report.put("healthy", false);' in code

    # Fail-closed em nós ausentes ou nulos
    assert 'if (backupRoot == null)' in code

    # Fail-closed em tipos não-array
    assert 'backupRoot.has("campaignPois") && backupRoot.optJSONArray("campaignPois") == null' in code
    assert 'backupRoot.has("tacticalScenes") && backupRoot.optJSONArray("tacticalScenes") == null' in code
    assert 'backupRoot.has("quests") && backupRoot.optJSONArray("quests") == null' in code


def test_challenger3_pure_jvm_stress_harness_execution(tmp_path):
    """Executa o harness puro JVM KingmakerChallenger3StressHarness com 70 asserções adversariais."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado")

    out_dir = tmp_path / "challenger3_classes"
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
        str(ROOT / "tests_pf2/java/KingmakerChallenger3StressHarness.java"),
    ]

    compile_cmd = [JAVAC, "-encoding", "UTF-8", "-d", str(out_dir)] + sources
    res_compile = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_compile.returncode == 0, f"Falha na compilação do harness Challenger 3:\n{res_compile.stderr}"

    run_cmd = [JAVA, "-cp", str(out_dir), "com.braseiro.pathfinder2e.KingmakerChallenger3StressHarness"]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness Challenger 3:\n{res_run.stderr}"
    assert "CHALLENGER 3: SUCESSO TOTAL" in res_run.stdout
    assert "70 CHECKS PASSADOS COM ZERO FALHAS" in res_run.stdout
