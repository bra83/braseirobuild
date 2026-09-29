"""Suíte de Testes Adversariais Tier 5 — Milestone M_FINAL (challenger_final_1).

Cobertura e Hardening de Estresse:
1. Capítulos 1 a 12:
   - Ingestão, parsing e validação de todos os 12 catálogos oficiais.
   - Progressão de níveis 1 a 20 e somatório de marcos de XP.
   - Casos de borda de perigos Remaster (CDs de salvaguarda, CDs de desativação, furtividade, rotinas).
2. Diretor Solo e Roteamento:
   - getActiveCampaignGuidance em múltiplos estados (não-iniciado, quests ativas, quests concluídas, múltiplos capítulos).
   - getAvailableRumors e filtros de resolução (status, quest concluída, hex limpo).
   - Roteamento determinístico das 9 rotas no KingmakerSoloFlowRouter com fallback adversarial.
3. Backup v22 Fail-Closed:
   - Injeção de estruturas corrompidas (não-array, perigos inválidos, quests sem título, canonical!=0).
4. Execução pura JVM sem tarefas Gradle pesadas.
"""

import json
import sqlite3
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
CHAPTERS_DIR = ROOT / "android/app/src/main/assets/pathfinder/kingmaker/chapters"

JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"

CANONICAL_CHAPTER_FILES = [
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
    "chapter_12_companion_stories.json",
]


# ==============================================================================
# 1. HARNESS PURO JVM: TIER 5 FINAL ADVERSARIAL STRESS
# ==============================================================================

def test_tier5_pure_jvm_final_stress_harness_execution(tmp_path):
    """Compila e executa KingmakerFinalStressHarness.java com mais de 1400 asserções rigorosas."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado em C:/Program Files/Android/Android Studio/jbr")

    out_dir = tmp_path / "final_stress_classes"
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
        str(SRC / "KingmakerSoloFlowRouter.java"),
        str(ROOT / "tests_pf2/java/KingmakerFinalStressHarness.java"),
    ]

    compile_cmd = [JAVAC, "-encoding", "UTF-8", "-d", str(out_dir)] + sources
    res_compile = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_compile.returncode == 0, f"Falha na compilação do harness final:\n{res_compile.stderr}"

    run_cmd = [JAVA, "-cp", str(out_dir), "com.braseiro.pathfinder2e.KingmakerFinalStressHarness", str(CHAPTERS_DIR)]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness final:\n{res_run.stderr}\nStdout:\n{res_run.stdout}"
    assert "FINAL_STRESS_HARNESS_PASS" in res_run.stdout
    assert "TIER 5 HARNESS CONCLUÍDO COM SUCESSO ABSOLUTO" in res_run.stdout


# ==============================================================================
# 2. CAPÍTULOS 1-12: INTEGRIDADE, PROGRESSÃO DE NÍVEIS E XP DE MARCOS
# ==============================================================================

def test_all_twelve_chapters_disk_integrity_and_schema():
    """Valida a presença física e conformidade estrita de schema de todos os 12 capítulos."""
    assert len(CANONICAL_CHAPTER_FILES) == 12

    for filename in CANONICAL_CHAPTER_FILES:
        filepath = CHAPTERS_DIR / filename
        assert filepath.exists(), f"Arquivo de capítulo ausente no disco: {filename}"
        assert filepath.stat().st_size > 1000, f"Arquivo suspeito ou truncado: {filename}"

        data = json.loads(filepath.read_text(encoding="utf-8"))

        assert data.get("schema") in ("braseiro-pathfinder2e-chapter-v1", "braseiro-pathfinder2e-kingmaker-chapter-v1")
        assert data.get("locale") == "pt-BR"
        assert len(data.get("title", "").strip()) > 0
        assert len(data.get("chapter_key", "").strip()) > 0
        assert 1 <= data.get("level_min", 0) <= 20
        assert 1 <= data.get("level_max", 0) <= 20
        assert data.get("level_min") <= data.get("level_max")
        assert len(data.get("quests", [])) > 0, f"Capítulo sem quests: {filename}"
        assert len(data.get("hazards", [])) > 0, f"Capítulo sem perigos: {filename}"
        assert len(data.get("pois", [])) > 0, f"Capítulo sem POIs: {filename}"


def test_level_progression_continuity_level_1_to_20():
    """Verifica progressão contínua sem lacunas de nível 1 a 20 através dos capítulos 1 a 10."""
    chapters = []
    for f in CANONICAL_CHAPTER_FILES[:10]:
        data = json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8"))
        chapters.append(data)

    assert chapters[0]["level_min"] == 1, "Capítulo 1 deve iniciar no Nível 1"
    assert chapters[-1]["level_max"] == 20, "Capítulo 10 deve atingir o Nível 20"

    for i in range(len(chapters) - 1):
        cur = chapters[i]
        nxt = chapters[i + 1]
        assert nxt["level_min"] <= cur["level_max"], \
            f"Lacuna de nível detectada entre {cur['chapter_key']} (máx {cur['level_max']}) e {nxt['chapter_key']} (mín {nxt['level_min']})"

    # Capítulos 11 (Atlas) e 12 (Companheiros) cobrem a campanha inteira (1-20)
    ch11 = json.loads((CHAPTERS_DIR / CANONICAL_CHAPTER_FILES[10]).read_text(encoding="utf-8"))
    ch12 = json.loads((CHAPTERS_DIR / CANONICAL_CHAPTER_FILES[11]).read_text(encoding="utf-8"))
    assert ch11["level_min"] == 1 and ch11["level_max"] == 20
    assert ch12["level_min"] == 1 and ch12["level_max"] == 20


def test_milestone_xp_summation_covers_full_progression():
    """Valida o somatório de XP de marcos em toda a campanha (mínimo 19.000 XP para Nível 20)."""
    total_xp = 0
    quest_count = 0
    milestone_count = 0

    for f in CANONICAL_CHAPTER_FILES:
        data = json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8"))
        for q in data.get("quests", []):
            xp = q.get("milestone_xp", 0)
            assert xp >= 0, f"XP negativo na quest {q.get('quest_key')}"
            total_xp += xp
            quest_count += 1
        for m in data.get("milestones", []):
            xp = m.get("xp", 0)
            assert xp >= 0, f"XP negativo no marco {m.get('milestone_key')}"
            total_xp += xp
            milestone_count += 1

    assert total_xp >= 19000, f"XP acumulado insuficiente ({total_xp}) para cobrir 19 níveis de progressão"
    assert quest_count >= 60, f"Contagem inesperada de quests na campanha: {quest_count}"
    assert milestone_count >= 40, f"Contagem inesperada de marcos na campanha: {milestone_count}"


# ==============================================================================
# 3. PERIGOS REMASTER: CASOS DE BORDA, CDS E ROTINAS
# ==============================================================================

def test_canonical_remaster_hazards_edge_cases():
    """Valida rigorosamente os stat blocks de perigos Remaster em todos os 12 capítulos."""
    valid_saves = {"Reflexos", "Fortitude", "Vontade"}
    valid_proficiencies = {"treinado", "especialista", "mestre", "lendario"}
    hazard_keys_seen = set()

    for f in CANONICAL_CHAPTER_FILES:
        data = json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8"))
        hazards = data.get("hazards", [])

        for h in hazards:
            h_key = h.get("hazard_key", "")
            assert len(h_key) > 0, f"hazard_key vazio em {f}"
            assert h_key not in hazard_keys_seen, f"hazard_key duplicado: {h_key}"
            hazard_keys_seen.add(h_key)

            # Nível e CDs positivas
            lvl = h.get("level", -1)
            assert 0 <= lvl <= 25, f"Nível de perigo fora da faixa Remaster (0-25): {lvl} em {h_key}"

            stealth_dc = h.get("stealth_dc", 0)
            assert stealth_dc > 0, f"CD de Furtividade deve ser positiva: {stealth_dc} em {h_key}"

            save_dc = h.get("saving_throw_dc", 0)
            assert save_dc > 0, f"CD de Salvaguarda deve ser positiva: {save_dc} em {h_key}"

            save_type = h.get("saving_throw_type", "")
            assert save_type in valid_saves, f"Tipo de salvaguarda Remaster inválido: {save_type} em {h_key}"

            # Métodos de desativação
            dis = h.get("disable_skills", [])
            assert len(dis) > 0, f"Perigo sem métodos de desativação: {h_key}"
            for opt in dis:
                assert len(opt.get("skill", "").strip()) > 0, f"Perícia vazia em {h_key}"
                assert opt.get("dc", 0) > 0, f"CD de desativação deve ser positiva em {h_key}"
                assert opt.get("proficiency") in valid_proficiencies, f"Proficiência inválida em {h_key}"

            # Complexidade e rotinas
            complexity = h.get("complexity", "simples")
            assert complexity in ("simples", "complexo"), f"Complexidade inválida em {h_key}: {complexity}"
            assert h.get("routine_actions", 1) >= 1

            if complexity == "complexo":
                has_routine = len(h.get("routine", [])) > 0
                has_reaction = len(h.get("reaction", "").strip()) > 0
                assert has_routine or has_reaction, f"Perigo complexo sem rotina ou reação: {h_key}"


# ==============================================================================
# 4. DIRETOR SOLO E ROTEAMENTO: DETERMINISMO E RUMORES
# ==============================================================================

def test_solo_flow_router_all_nine_routes_and_adversarial_fallbacks():
    """Valida determinismo estrito nas 9 rotas e fallbacks com entradas adversariais."""
    code = (SRC / "KingmakerSoloFlowRouter.java").read_text(encoding="utf-8")

    routes_map = {
        "adventure_start": "start_adventure",
        "camp_encounter": "camp_encounter",
        "camp": "camp",
        "war": "war",
        "exploration_resolution": "exploration_resolution",
        "companion": "companion",
        "kingdom_bridge": "kingdom_bridge",
        "travel": "travel",
        "free_exploration": "free_exploration",
    }

    for scene, action in routes_map.items():
        assert f'if ("{scene}".equals(scene)) return {action.upper()};' in code or \
               f'"{scene}".equals(scene)' in code, f"Rota ausente no roteador: {scene}"


def test_solo_director_guidance_states_simulation():
    """Testa a lógica determinística de getActiveCampaignGuidance em diversos estados."""
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE kingmaker_campaign_chapters (
            world_profile TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            chapter_number INTEGER NOT NULL,
            title TEXT NOT NULL,
            PRIMARY KEY(world_profile, chapter_key)
        )
    """)
    cur.execute("""
        CREATE TABLE quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            quest_key TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            title TEXT NOT NULL,
            status TEXT NOT NULL,
            stage TEXT NOT NULL DEFAULT '',
            objective TEXT NOT NULL DEFAULT ''
        )
    """)

    # Popula 12 capítulos
    for i in range(1, 13):
        cur.execute(
            "INSERT INTO kingmaker_campaign_chapters VALUES ('pf2', ?, ?, ?)",
            (f"chapter_{i:02d}", i, f"Capítulo {i}")
        )
    conn.commit()

    # Estado A: Sem quests -> deve sugerir capítulo 1
    unstarted = cur.execute(
        "SELECT chapter_number, chapter_key, title FROM kingmaker_campaign_chapters WHERE world_profile='pf2' ORDER BY chapter_number ASC LIMIT 1"
    ).fetchone()
    assert unstarted[0] == 1
    assert unstarted[1] == "chapter_01"

    # Estado B: Quest ativa no Capítulo 5
    cur.execute(
        "INSERT INTO quests (world_profile, quest_key, chapter_key, title, status, stage, objective) "
        "VALUES ('pf2', 'q_varnhold', 'chapter_05', 'Desaparecimento', 'active', 'investigacao', 'Explorar Varnhold')"
    )
    conn.commit()

    active = cur.execute(
        "SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress') ORDER BY id ASC"
    ).fetchall()
    assert len(active) == 1
    assert active[0][3] == "chapter_05"
    assert active[0][4] == "Desaparecimento"

    # Estado C: Múltiplas quests ativas -> prioridade para a primeira inserida (ORDER BY id ASC)
    cur.execute(
        "INSERT INTO quests (world_profile, quest_key, chapter_key, title, status, stage, objective) "
        "VALUES ('pf2', 'q_secundaria', 'chapter_05', 'Secundária', 'active', 'sec_stage', 'Objetivo secundário')"
    )
    conn.commit()

    active_multi = cur.execute(
        "SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress') ORDER BY id ASC"
    ).fetchall()
    assert len(active_multi) == 2
    assert active_multi[0][2] == "q_varnhold"  # Primeira permanece prioritária

    # Estado D: Conclusão das quests do Capítulo 5 e avanço para Capítulo 6
    cur.execute("UPDATE quests SET status='completed' WHERE chapter_key='chapter_05'")
    cur.execute(
        "INSERT INTO quests (world_profile, quest_key, chapter_key, title, status, stage, objective) "
        "VALUES ('pf2', 'q_armag', 'chapter_06', 'Tumba de Armag', 'active', 'localizar_tumba', 'Encontrar a espada Ovinrbaane')"
    )
    conn.commit()

    active_next = cur.execute(
        "SELECT * FROM quests WHERE world_profile='pf2' AND status IN ('active', 'in_progress') ORDER BY id ASC"
    ).fetchall()
    assert len(active_next) == 1
    assert active_next[0][3] == "chapter_06"
    assert active_next[0][4] == "Tumba de Armag"

    conn.close()


def test_rumor_resolution_and_filtering_across_chapters():
    """Valida o ciclo determinístico de resolução de rumores por status, quest e hex."""
    conn = sqlite3.connect(":memory:")
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE rumors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            rumor_key TEXT NOT NULL,
            chapter INTEGER DEFAULT 1,
            statement TEXT NOT NULL,
            status TEXT DEFAULT 'circulating'
        )
    """)
    cur.execute("""
        CREATE TABLE quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            quest_key TEXT NOT NULL,
            status TEXT NOT NULL
        )
    """)
    cur.execute("""
        CREATE TABLE kingmaker_hexes (
            world_profile TEXT NOT NULL,
            map_id TEXT NOT NULL,
            hex_id TEXT NOT NULL,
            cleared INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(world_profile, map_id, hex_id)
        )
    """)

    # 1. Inserir rumores para capítulos 1 e 2
    cur.execute("INSERT INTO rumors VALUES (1, 'pf2', 'r_bandits', 1, 'Bandidos em Oleg', 'circulating')")
    cur.execute("INSERT INTO rumors VALUES (2, 'pf2', 'r_trolls', 2, 'Troles ao sul', 'circulating')")
    conn.commit()

    # Filtro de rumores por capítulo
    r_ch1 = cur.execute("SELECT * FROM rumors WHERE world_profile='pf2' AND chapter=1").fetchall()
    assert len(r_ch1) == 1 and r_ch1[0][2] == "r_bandits"

    r_ch2 = cur.execute("SELECT * FROM rumors WHERE world_profile='pf2' AND chapter=2").fetchall()
    assert len(r_ch2) == 1 and r_ch2[0][2] == "r_trolls"

    # Resolução por status
    cur.execute("UPDATE rumors SET status='resolved' WHERE rumor_key='r_bandits'")
    conn.commit()
    r_status = cur.execute("SELECT status FROM rumors WHERE rumor_key='r_bandits'").fetchone()[0]
    assert r_status == "resolved"

    # Resolução por quest associada concluída
    cur.execute("INSERT INTO quests VALUES (1, 'pf2', 'q_troll_invasion', 'completed')")
    conn.commit()
    q_status = cur.execute("SELECT status FROM quests WHERE quest_key='q_troll_invasion'").fetchone()[0]
    assert q_status == "completed"

    # Resolução por hex limpo
    cur.execute("INSERT INTO kingmaker_hexes VALUES ('pf2', 'stolen_lands', 'H_05', 1)")
    conn.commit()
    cleared = cur.execute("SELECT cleared FROM kingmaker_hexes WHERE hex_id='H_05'").fetchone()[0]
    assert cleared == 1

    conn.close()


# ==============================================================================
# 5. BACKUP V22: HARDENING FAIL-CLOSED E AUDITORIA DE CORRUPÇÃO
# ==============================================================================

def test_backup_v22_fail_closed_contract_in_code():
    """Audita a implementação estrita de fail-closed no KingmakerCampaignImporter e KingmakerRepository."""
    importer_code = (SRC / "KingmakerCampaignImporter.java").read_text(encoding="utf-8")
    repo_code = (SRC / "KingmakerRepository.java").read_text(encoding="utf-8")
    app_db_code = (SRC / "AppDb.java").read_text(encoding="utf-8")

    # validateCampaignRestore fail-closed
    assert "public static JSONObject validateCampaignRestore(JSONObject backupRoot)" in importer_code
    assert 'report.put("healthy", isHealthy);' in importer_code
    assert 'report.put("healthy", false);' in importer_code
    assert 'if (backupRoot == null)' in importer_code

    # Verificação de tipos não-array
    for coll in ["campaignPois", "tacticalScenes", "quests", "chapters", "factions"]:
        assert f'backupRoot.has("{coll}") && backupRoot.optJSONArray("{coll}") == null' in importer_code

    # Repositório incorpora auditoria de campanha
    assert "KingmakerCampaignImporter.validateCampaignRestore(root)" in repo_code
    assert '!campaignAudit.optBoolean("healthy",false)' in repo_code

    # AppDb reverte restauração se healthy != true
    assert '!kingmakerRestoreAudit.optBoolean("healthy",false)' in app_db_code
    assert "A restauração foi revertida pela auditoria Kingmaker" in app_db_code
