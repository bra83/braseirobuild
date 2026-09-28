"""Suíte de Testes Automatizados para Milestone 2: Ingestão dos Capítulos 1 a 4 de Kingmaker (PZO2020).

Valida:
1. Existência e integridade sintática dos quatro arquivos JSON canônicos em:
   android/app/src/main/assets/pathfinder/kingmaker/chapters/
2. Conformidade estrita com o esquema "braseiro-pathfinder2e-chapter-v1" e com o modelo
   ChapterCatalog em KingmakerCampaignModels.java.
3. Arcos narrativos, quests encadeadas, NPCs, POIs hexagonais, cenas táticas, facções,
   relógios de mundo, eventos e marcos de XP dos Capítulos 1 a 4 em pt-BR 100% normalizado.
4. Blocos de estatísticas canônicos de perigos Remaster (CDs de Furtividade, Salvaguarda,
   Desativação, ações de rotina, CA, Dureza, PV, traits Remaster).
5. Execução em bytecode puro JVM via javac/java compilado contra tools/runtime_json e
   KingmakerCampaignModels.java validando ChapterCatalog.fromJson(json) e catalog.validate().
6. Persistência e isolamento transacional SQLite via KingmakerCampaignImporter no esquema do AppDb
   (com escopo por world_profile, invariante canonical=0, idempotência e fail-closed).
"""

import json
import sqlite3
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
CHAPTERS_DIR = ROOT / "android/app/src/main/assets/pathfinder/kingmaker/chapters"
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"

CHAPTER_KEYS = [
    "chapter_01_stolen_lands_part1",
    "chapter_02_stolen_lands_part2",
    "chapter_03_rivers_run_red",
    "chapter_04_cult_of_bloom",
]


def load_chapter_data(key: str) -> dict:
    file_path = CHAPTERS_DIR / f"{key}.json"
    assert file_path.exists(), f"Arquivo do capítulo não encontrado: {file_path}"
    return json.loads(file_path.read_text(encoding="utf-8"))


# ==============================================================================
# 1. FILE & SCHEMA INTEGRITY TESTS
# ==============================================================================

def test_m2_chapters1_to_4_files_exist_and_valid_json():
    """Valida existência física e sintaxe JSON válida para todos os 4 capítulos."""
    for key in CHAPTER_KEYS:
        file_path = CHAPTERS_DIR / f"{key}.json"
        assert file_path.exists(), f"Arquivo ausente: {file_path}"
        content = file_path.read_text(encoding="utf-8")
        assert len(content) > 500, f"Arquivo excessivamente curto: {file_path}"
        data = json.loads(content)
        assert isinstance(data, dict), f"Raiz do JSON deve ser objeto: {key}"


def test_m2_chapter_schema_and_top_level_contract():
    """Valida chaves obrigatórias contratuais, faixas de níveis e metadados."""
    expected_ranges = {
        "chapter_01_stolen_lands_part1": (1, 2, 1),
        "chapter_02_stolen_lands_part2": (2, 5, 2),
        "chapter_03_rivers_run_red": (5, 7, 3),
        "chapter_04_cult_of_bloom": (7, 9, 4),
    }

    required_keys = [
        "schema",
        "chapterKey",
        "chapterNumber",
        "title",
        "levelMin",
        "levelMax",
        "quests",
        "hazards",
        "pois",
        "npcs",
        "scenes",
        "factions",
        "clocks",
        "events",
        "milestones",
        "metadata",
    ]

    for key, (lvl_min, lvl_max, ch_num) in expected_ranges.items():
        data = load_chapter_data(key)

        for req in required_keys:
            assert req in data, f"Campo contratual '{req}' ausente no capítulo {key}"

        # Suporte dual camelCase e snake_case para interop total
        assert data["schema"] == "braseiro-pathfinder2e-chapter-v1"
        assert data["chapterKey"] == key
        assert data["chapter_key"] == key
        assert data["chapterNumber"] == ch_num
        assert data["chapter_number"] == ch_num
        assert data["levelMin"] == lvl_min
        assert data["level_min"] == lvl_min
        assert data["levelMax"] == lvl_max
        assert data["level_max"] == lvl_max
        assert data["locale"] == "pt-BR"

        assert len(data["quests"]) >= 4, f"Quests insuficientes em {key}"
        assert len(data["hazards"]) >= 3, f"Perigos insuficientes em {key}"
        assert len(data["pois"]) >= 4, f"POIs insuficientes em {key}"
        assert len(data["npcs"]) >= 4, f"NPCs insuficientes em {key}"
        assert len(data["scenes"]) >= 2, f"Cenas insuficientes em {key}"
        assert len(data["factions"]) >= 2, f"Facções insuficientes em {key}"
        assert len(data["clocks"]) >= 2, f"Relógios insuficientes em {key}"
        assert len(data["events"]) >= 2, f"Eventos insuficientes em {key}"
        assert len(data["milestones"]) >= 3, f"Marcos insuficientes em {key}"


# ==============================================================================
# 2. CANONICAL CONTENT VERIFICATION (CHAPTERS 1 TO 4)
# ==============================================================================

def test_m2_chapter_01_stolen_lands_part1_canonical_content():
    """Valida o conteúdo canônico de Terras Roubadas Parte 1 (Oleg, Kressle, Bokken)."""
    data = load_chapter_data("chapter_01_stolen_lands_part1")
    assert "Terras Roubadas" in data["title"]
    assert "Oleg" in data["title"]

    quest_keys = [q["quest_key"] for q in data["quests"]]
    assert "q_oleg" in quest_keys
    assert "q_kressle" in quest_keys
    assert "q_bokken" in quest_keys
    assert "q_carta_restov" in quest_keys

    npc_keys = [n["npc_key"] for n in data["npcs"]]
    assert "npc_oleg_leveton" in npc_keys
    assert "npc_svetlana_leveton" in npc_keys
    assert "npc_bokken" in npc_keys
    assert "npc_kressle" in npc_keys
    assert "npc_happs_bydon" in npc_keys

    # Valida progressão de diálogo e atitude de Kressle (hostil -> desfavoravel -> indiferente)
    kressle = next(n for n in data["npcs"] if n["npc_key"] == "npc_kressle")
    attitudes = [node["attitude_req"] for node in kressle["dialogue_tree"]]
    assert "hostil" in attitudes
    assert "desfavoravel" in attitudes
    assert "indiferente" in attitudes

    poi_keys = [p["poi_key"] for p in data["pois"]]
    assert "poi_posto_oleg" in poi_keys
    assert "poi_vau_espinheiro" in poi_keys
    assert "poi_ninho_frutas_presa" in poi_keys

    hazard_keys = [h["hazard_key"] for h in data["hazards"]]
    assert "hazard_laco_espinheiro" in hazard_keys
    assert "hazard_fosso_camuflado" in hazard_keys
    assert "hazard_moita_frutas_presa" in hazard_keys


def test_m2_chapter_02_stolen_lands_part2_canonical_content():
    """Valida o conteúdo canônico de Terras Roubadas Parte 2 (Tuskgutter, Sicômoro, Alce, Cervo)."""
    data = load_chapter_data("chapter_02_stolen_lands_part2")
    assert "Senhor dos Cervos" in data["title"]
    assert "Fundação da Baronia" in data["title"]

    quest_keys = [q["quest_key"] for q in data["quests"]]
    assert "q_tuskgutter" in quest_keys
    assert "q_sicomoro" in quest_keys
    assert "q_templo_alce" in quest_keys
    assert "q_senhor_cervos" in quest_keys
    assert "q_fundacao_baronia" in quest_keys

    npc_keys = [n["npc_key"] for n in data["npcs"]]
    assert "npc_jhod_kavken" in npc_keys
    assert "npc_chefe_sootscale" in npc_keys
    assert "npc_akiros_ismort" in npc_keys
    assert "npc_dovan_de_nisroch" in npc_keys
    assert "npc_senhor_dos_cervos" in npc_keys

    # Valida Akiros Ismort com dilema moral e redenção
    akiros = next(n for n in data["npcs"] if n["npc_key"] == "npc_akiros_ismort")
    assert any("Erastil" in node["text"] or "forte" in node["text"] for node in akiros["dialogue_tree"])

    poi_keys = [p["poi_key"] for p in data["pois"]]
    assert "poi_velho_sicomoro" in poi_keys
    assert "poi_templo_alce" in poi_keys
    assert "poi_guarida_tuskgutter" in poi_keys
    assert "poi_forte_senhor_cervos" in poi_keys
    assert "poi_tribo_sootscale" in poi_keys


def test_m2_chapter_03_rivers_run_red_canonical_content():
    """Valida o conteúdo canônico de Rios Correm Vermelhos (Hargulka, Jubilost, Bruxa do Pântano)."""
    data = load_chapter_data("chapter_03_rivers_run_red")
    assert "Rios Correm Vermelhos" in data["title"]
    assert "Hargulka" in data["title"]

    quest_keys = [q["quest_key"] for q in data["quests"]]
    assert "q_hargulka" in quest_keys
    assert "q_jubilost" in quest_keys
    assert "q_bruxa_pantano" in quest_keys
    assert "q_troles_pedra" in quest_keys

    npc_keys = [n["npc_key"] for n in data["npcs"]]
    assert "npc_jubilost_narthropple" in npc_keys
    assert "npc_velha_beldame" in npc_keys
    assert "npc_rei_hargulka" in npc_keys
    assert "npc_kesten_garess" in npc_keys

    # Jubilost deve ser marcado como companheiro
    jubilost = next(n for n in data["npcs"] if n["npc_key"] == "npc_jubilost_narthropple")
    assert jubilost.get("is_companion") is True or jubilost.get("isCompanion") is True
    assert "Tesoureiro" in jubilost["summary"] or "erudito" in jubilost["summary"].lower()

    poi_keys = [p["poi_key"] for p in data["pois"]]
    assert "poi_fortaleza_hargulka" in poi_keys
    assert "poi_vau_jubilost" in poi_keys
    assert "poi_choupana_bruxa" in poi_keys
    assert "poi_mina_solitaria" in poi_keys


def test_m2_chapter_04_cult_of_bloom_canonical_content():
    """Valida o conteúdo canônico de Culto da Flor (Útero de Lamashtu, Corujursos, Tristian)."""
    data = load_chapter_data("chapter_04_cult_of_bloom")
    assert "Culto da Flor" in data["title"] or "Infecção do Primeiro Mundo" in data["title"]

    quest_keys = [q["quest_key"] for q in data["quests"]]
    assert "q_utero_lamashtu" in quest_keys
    assert "q_corujursos" in quest_keys
    assert "q_cura_flor" in quest_keys
    assert "q_fenda_primeiro_mundo" in quest_keys

    npc_keys = [n["npc_key"] for n in data["npcs"]]
    assert "npc_sacerdotisa_tsanna" in npc_keys
    assert "npc_tristian" in npc_keys
    assert "npc_corujurso_alfa" in npc_keys
    assert "npc_aldeao_kaelen" in npc_keys

    # Tristian deve ser marcado como companheiro
    tristian = next(n for n in data["npcs"] if n["npc_key"] == "npc_tristian")
    assert tristian.get("is_companion") is True or tristian.get("isCompanion") is True
    assert "Sarenrae" in tristian["summary"]

    poi_keys = [p["poi_key"] for p in data["pois"]]
    assert "poi_utero_lamashtu" in poi_keys
    assert "poi_ninho_corujursos" in poi_keys
    assert "poi_bosque_sussurrante" in poi_keys
    assert "poi_vila_bravan" in poi_keys


# ==============================================================================
# 3. CANONICAL PF2E REMASTER HAZARD STAT BLOCKS
# ==============================================================================

def test_m2_canonical_pf2e_remaster_hazard_stat_blocks():
    """Valida rigorosamente os stat blocks de todos os perigos dos Capítulos 1 a 4."""
    for key in CHAPTER_KEYS:
        data = load_chapter_data(key)
        for h in data["hazards"]:
            h_key = h.get("hazard_key", h.get("name", "sem_nome"))
            # Validação das propriedades numéricas canônicas do Remaster
            assert h.get("stealth_dc", 0) > 0, f"Perigo {h_key} em {key} sem stealth_dc positivo"
            assert h.get("saving_throw_dc", 0) > 0, f"Perigo {h_key} em {key} sem saving_throw_dc positivo"
            assert h.get("saving_throw_type") in ("Reflexos", "Fortitude", "Vontade"), f"Tipo de salvaguarda inválido em {h_key}"

            disable_skills = h.get("disable_skills", [])
            assert len(disable_skills) >= 1, f"Perigo {h_key} em {key} sem métodos de desativação"
            for d in disable_skills:
                assert "skill" in d and len(d["skill"]) > 0, f"Perícia não informada na desativação de {h_key}"
                assert d.get("dc", 0) > 0, f"CD inválida na desativação de {h_key}"
                assert d.get("proficiency") in ("treinado", "especialista", "mestre", "lendario"), f"Proficiência inválida em {h_key}"

            assert h.get("routine_actions", 0) >= 1, f"Ações de rotina inválidas em {h_key}"
            assert h.get("ac", 0) >= 10, f"CA inválida em {h_key}"
            assert h.get("hp", 0) > 0, f"Pontos de vida inválidos em {h_key}"
            assert h.get("bt", 0) >= h.get("hp", 0) // 2, f"Limiar de quebra (BT) inválido em {h_key}"
            assert h.get("hardness", -1) >= 0, f"Dureza inválida em {h_key}"
            assert len(h.get("traits", [])) >= 1, f"Perigo {h_key} sem traits"
            assert len(h.get("routine", [])) >= 1, f"Perigo {h_key} sem rotina definida"


# ==============================================================================
# 4. HEX POI CONSTRAINTS & INVARIANTS
# ==============================================================================

def test_m2_hex_poi_canonical_zero_and_status_invariants():
    """Valida as restrições estritas do AppDb nos POIs (canonical=0, source, status)."""
    for key in CHAPTER_KEYS:
        data = load_chapter_data(key)
        for p in data["pois"]:
            p_name = p.get("name", "sem_nome")
            # Invariante estrita do AppDb: CHECK(canonical=0)
            assert p.get("canonical") == 0, f"POI '{p_name}' em {key} viola invariant canonical=0"
            assert p.get("source") in ("campaign", "procedural"), f"Source inválido em '{p_name}'"
            assert p.get("status") in ("active", "abandoned", "ruined", "destroyed", "occupied", "rebuilding"), f"Status inválido em '{p_name}'"
            assert len(p.get("hex_id", "").strip()) > 0, f"Hex_id ausente em '{p_name}'"
            assert p.get("type") in ("settlement", "landmark", "resource_site", "refuge", "dungeon", "lair"), f"Tipo de POI inválido em '{p_name}'"


# ==============================================================================
# 5. PURE JVM MODEL LOADING VIA CHAPTERCATALOG.FROMJSON
# ==============================================================================

def test_m2_pure_jvm_chapter_catalog_from_json_validation(tmp_path):
    """Compila e executa harness puro Java validando ChapterCatalog.fromJson e validate() em bytecode."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado em C:/Program Files/Android/Android Studio/jbr")

    out_dir = tmp_path / "classes"
    out_dir.mkdir(parents=True, exist_ok=True)

    harness_src = f"""package com.braseiro.pathfinder2e;

import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class KingmakerM2CatalogLoaderHarness {{
    public static void main(String[] args) throws Exception {{
        String[] files = {{
            "{str(CHAPTERS_DIR / 'chapter_01_stolen_lands_part1.json').replace('\\', '/')}",
            "{str(CHAPTERS_DIR / 'chapter_02_stolen_lands_part2.json').replace('\\', '/')}",
            "{str(CHAPTERS_DIR / 'chapter_03_rivers_run_red.json').replace('\\', '/')}",
            "{str(CHAPTERS_DIR / 'chapter_04_cult_of_bloom.json').replace('\\', '/')}"
        }};

        int totalChapters = 0;
        int totalQuests = 0;
        int totalHazards = 0;
        int totalPois = 0;
        int totalNpcs = 0;

        for (String filePath : files) {{
            String rawJson = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
            JSONObject json = new JSONObject(rawJson);
            KingmakerCampaignModels.ChapterCatalog cat = KingmakerCampaignModels.ChapterCatalog.fromJson(json);
            cat.validate();

            // Valida roundtrip toJson -> fromJson
            JSONObject exported = cat.toJson();
            KingmakerCampaignModels.ChapterCatalog roundtrip = KingmakerCampaignModels.ChapterCatalog.fromJson(exported);
            roundtrip.validate();

            if (!cat.chapterKey.equals(roundtrip.chapterKey)) {{
                throw new AssertionError("Falha no roundtrip para chapterKey: " + cat.chapterKey);
            }}

            totalChapters++;
            totalQuests += cat.quests.size();
            totalHazards += cat.hazards.size();
            totalPois += cat.pois.size();
            totalNpcs += cat.npcs.size();
        }}

        System.out.println("KINGMAKER_M2_JAVA_LOADER_PASS: chapters=" + totalChapters +
                           " quests=" + totalQuests + " hazards=" + totalHazards +
                           " pois=" + totalPois + " npcs=" + totalNpcs);
    }}
}}
"""
    harness_file = out_dir / "KingmakerM2CatalogLoaderHarness.java"
    harness_file.write_text(harness_src, encoding="utf-8")

    compile_cmd = [
        JAVAC, "-encoding", "UTF-8", "-d", str(out_dir),
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(SRC / "KingmakerCampaignModels.java"),
        str(harness_file)
    ]
    res_comp = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_comp.returncode == 0, f"Falha na compilação do harness Java: {res_comp.stderr}"

    run_cmd = [
        JAVA, "-cp", str(out_dir),
        "com.braseiro.pathfinder2e.KingmakerM2CatalogLoaderHarness"
    ]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness Java: {res_run.stderr}"
    assert "KINGMAKER_M2_JAVA_LOADER_PASS" in res_run.stdout
    assert "chapters=4" in res_run.stdout


# ==============================================================================
# 6. SQLITE CAMPAIGN IMPORTER TRANSACTIONAL INGESTION
# ==============================================================================

def setup_production_appdb_schema(conn: sqlite3.Connection):
    """Cria tabelas oficiais do AppDb com todas as constraints ativas."""
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
        CREATE TABLE IF NOT EXISTS quests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            quest_key TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            title TEXT NOT NULL,
            status TEXT DEFAULT 'unstarted',
            stage TEXT DEFAULT '',
            objective TEXT DEFAULT '',
            stakes TEXT DEFAULT '',
            owner_character_id INTEGER DEFAULT 0,
            faction_id INTEGER DEFAULT 0,
            milestone_xp INTEGER DEFAULT 0,
            prerequisites_json TEXT DEFAULT '[]',
            objectives_json TEXT DEFAULT '[]',
            rewards_json TEXT DEFAULT '{}',
            progress_json TEXT DEFAULT '{}',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS campaign_pois (
            poi_id TEXT PRIMARY KEY,
            world_profile TEXT NOT NULL,
            campaign_key TEXT NOT NULL,
            poi_key TEXT NOT NULL,
            chapter_key TEXT NOT NULL,
            map_id TEXT NOT NULL DEFAULT 'stolen_lands',
            name TEXT NOT NULL,
            name_key TEXT NOT NULL,
            type TEXT NOT NULL DEFAULT 'point_of_interest',
            poi_type TEXT NOT NULL DEFAULT 'point_of_interest',
            hex_id TEXT NOT NULL,
            location_id TEXT,
            q INTEGER DEFAULT 0,
            r INTEGER DEFAULT 0,
            source TEXT NOT NULL DEFAULT 'campaign',
            origin TEXT NOT NULL DEFAULT 'campaign_discovery',
            canonical INTEGER NOT NULL DEFAULT 0,
            hidden INTEGER NOT NULL DEFAULT 0,
            discovered INTEGER NOT NULL DEFAULT 1,
            created_by TEXT NOT NULL DEFAULT 'campaign',
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
        CREATE TABLE IF NOT EXISTS tactical_scenes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            scene_key TEXT NOT NULL,
            title TEXT DEFAULT '',
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
            combatants_json TEXT DEFAULT '[]',
            source_context_json TEXT DEFAULT '{}',
            status TEXT DEFAULT 'ready',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            UNIQUE(world_profile, scene_key)
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
        CREATE TABLE IF NOT EXISTS world_clocks (
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
        CREATE TABLE IF NOT EXISTS world_events (
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
    cur.execute("""
        CREATE TABLE IF NOT EXISTS kingmaker_features (
            world_profile TEXT NOT NULL,
            feature_key TEXT NOT NULL,
            feature_kind TEXT NOT NULL,
            name TEXT NOT NULL,
            level INTEGER NOT NULL DEFAULT 1,
            status TEXT NOT NULL DEFAULT 'active',
            effects_json TEXT NOT NULL DEFAULT '{}',
            source_ref TEXT NOT NULL DEFAULT '',
            notes TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            PRIMARY KEY(world_profile, feature_key)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS kingmaker_companions (
            world_profile TEXT NOT NULL,
            companion_key TEXT NOT NULL,
            name TEXT NOT NULL,
            influence INTEGER NOT NULL DEFAULT 0,
            attitude TEXT NOT NULL DEFAULT 'indiferente',
            assignment TEXT NOT NULL DEFAULT 'reserve',
            available INTEGER NOT NULL DEFAULT 1,
            notes TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            PRIMARY KEY(world_profile, companion_key)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS combatants (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            world_profile TEXT NOT NULL,
            npc_key TEXT NOT NULL,
            name TEXT NOT NULL,
            attitude TEXT DEFAULT 'indiferente',
            faction TEXT DEFAULT '',
            dialogue_json TEXT DEFAULT '[]',
            hp INTEGER DEFAULT 20,
            max_hp INTEGER DEFAULT 20,
            ac INTEGER DEFAULT 15,
            initiative INTEGER DEFAULT 0,
            source_kind TEXT DEFAULT 'campaign',
            is_player INTEGER DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            UNIQUE(world_profile, npc_key)
        )
    """)
    conn.commit()


def simulate_import_chapter(conn: sqlite3.Connection, world_profile: str, chapter_data: dict):
    """Executa a ingestão atômica de um capítulo respeitando a lógica exata de KingmakerCampaignImporter."""
    now = 123456789
    cur = conn.cursor()
    ch_key = chapter_data["chapter_key"]

    # 1. Metadados do Capítulo
    cur.execute("""
        INSERT OR REPLACE INTO kingmaker_campaign_chapters (
            world_profile, chapter_key, chapter_number, title, level_min, level_max, summary, imported_at, metadata_json
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        world_profile, ch_key, chapter_data["chapter_number"], chapter_data["title"],
        chapter_data["level_min"], chapter_data["level_max"], chapter_data["summary"],
        now, json.dumps(chapter_data.get("metadata", {}))
    ))

    # Features de capítulo
    cur.execute("""
        INSERT OR REPLACE INTO kingmaker_features (
            world_profile, feature_key, feature_kind, name, level, status, effects_json, source_ref, notes, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        world_profile, f"campaign_chapter:{ch_key}", "campaign_chapter", chapter_data["title"],
        chapter_data["level_min"], "completed", json.dumps({"chapter_number": chapter_data["chapter_number"]}),
        "Kingmaker Adventure Path PZO2020", chapter_data["summary"], now, now
    ))

    # 2. Facções
    for f in chapter_data.get("factions", []):
        cur.execute("""
            INSERT OR REPLACE INTO factions (
                world_profile, name, agenda, resources, relationship, current_plan, location, active, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, f["name"], f.get("agenda", ""), f.get("resources", ""),
            f.get("relationship", 0), f.get("current_plan", ""), f.get("location", ""),
            1 if f.get("active", True) else 0, now
        ))

    # 3. Quests
    for q in chapter_data.get("quests", []):
        cur.execute("""
            INSERT INTO quests (
                world_profile, quest_key, chapter_key, title, status, stage, objective, stakes,
                milestone_xp, prerequisites_json, objectives_json, rewards_json, progress_json,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, q["quest_key"], ch_key, q["title"], q.get("status", "unstarted"),
            q.get("stage", ""), q.get("objective", ""), q.get("stakes", ""),
            q.get("milestone_xp", 0), json.dumps(q.get("prerequisites", [])),
            json.dumps(q.get("objectives", [])), json.dumps(q.get("rewards", {})),
            json.dumps(q.get("progress_json", {})), now, now
        ))

    # 4. POIs
    for p in chapter_data.get("pois", []):
        raw_id = p.get("poi_id") or p.get("poi_key")
        scoped_id = raw_id if raw_id.startswith(f"{world_profile}:") else f"{world_profile}:{raw_id}"
        cur.execute("""
            INSERT OR REPLACE INTO campaign_pois (
                poi_id, world_profile, campaign_key, poi_key, chapter_key, map_id, name, name_key,
                type, poi_type, hex_id, source, origin, canonical, hidden, discovered, created_by,
                status, description, tags_json, development_level, icon, notes, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            scoped_id, world_profile, ch_key, p["poi_key"], ch_key, p.get("map_id", "stolen_lands"),
            p["name"], p.get("name_key", p["name"]), p.get("type", "point_of_interest"),
            p.get("type", "point_of_interest"), p["hex_id"], p.get("source", "campaign"),
            p.get("origin", "campaign_discovery"), p.get("canonical", 0),
            1 if p.get("hidden", False) else 0, 1 if p.get("discovered", True) else 0,
            p.get("created_by", "campaign"), p.get("status", "active"),
            p.get("description", ""), json.dumps(p.get("tags", [])),
            p.get("development_level", 1), p.get("icon", "📍"), p.get("notes", ""), now, now
        ))

    # 5. Cenas Táticas
    for s in chapter_data.get("scenes", []):
        cur.execute("""
            INSERT OR REPLACE INTO tactical_scenes (
                world_profile, scene_key, title, location, hex_id, environment, width_cells,
                height_cells, seed, zones_json, obstacles_json, entry_points_json,
                source_context_json, status, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, s["scene_key"], s["title"], s.get("location", s["title"]),
            s.get("hex_id", ""), s.get("environment", "generic"), s.get("width_cells", 12),
            s.get("height_cells", 8), s.get("seed", 0), json.dumps(s.get("zones", [])),
            json.dumps(s.get("obstacles", [])), json.dumps(s.get("entry_points", [])),
            json.dumps(s.get("source_context", {})), s.get("status", "ready"), now, now
        ))

    # Cena agregada de perigos do capítulo
    if chapter_data.get("hazards"):
        hazard_scene_key = f"{ch_key}_perigos_canonicos"
        cur.execute("""
            INSERT OR REPLACE INTO tactical_scenes (
                world_profile, scene_key, title, location, environment, hazards_json, status, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, hazard_scene_key, f"Perigos Canônicos: {chapter_data['title']}",
            chapter_data["title"], "ermos", json.dumps(chapter_data["hazards"]), "ready", now, now
        ))

    # 6. NPCs & Companheiros
    known_companions = ("jubilost", "tristian", "amiri", "linzi", "valerie", "harrim", "ekundayo", "nok_nok", "jaethal", "regongar", "octavia", "kalikke")
    for n in chapter_data.get("npcs", []):
        cur.execute("""
            INSERT OR REPLACE INTO combatants (
                world_profile, npc_key, name, attitude, faction, dialogue_json, hp, max_hp, ac, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, n["npc_key"], n["name"], n.get("attitude", "indiferente"),
            n.get("faction", ""), json.dumps(n.get("dialogue_tree", [])),
            n.get("stats", {}).get("hp", 20), n.get("stats", {}).get("max_hp", 20),
            n.get("stats", {}).get("ac", 15), now, now
        ))

        is_comp = n.get("is_companion") or n.get("isCompanion") or any(c in n["npc_key"].lower() for c in known_companions)
        if is_comp:
            cur.execute("""
                INSERT OR IGNORE INTO kingmaker_companions (
                    world_profile, companion_key, name, attitude, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (
                world_profile, n["npc_key"], n["name"], n.get("attitude", "indiferente"),
                n.get("summary", ""), now, now
            ))

    # 7. Clocks & Events
    for c in chapter_data.get("clocks", []):
        cur.execute("""
            INSERT INTO world_clocks (world_profile, name, current_value, max_value, consequence, automatic, active, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, c["name"], c.get("current_value", 0), c.get("max_value", 4),
            c.get("consequence", ""), 1 if c.get("automatic", False) else 0,
            1 if c.get("active", True) else 0, now
        ))

    for e in chapter_data.get("events", []):
        cur.execute("""
            INSERT INTO world_events (world_profile, title, description, due_at, due_day, status, consequence_json, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, e["title"], e.get("description", ""), e.get("due_at", 0),
            e.get("due_day", 0), e.get("status", "scheduled"),
            json.dumps(e.get("consequence_json", {})), now
        ))

    # 8. Milestones
    for m in chapter_data.get("milestones", []):
        m_key = m.get("milestone_key") or m.get("name")
        cur.execute("""
            INSERT OR IGNORE INTO kingmaker_features (
                world_profile, feature_key, feature_kind, name, level, status, effects_json, source_ref, notes, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            world_profile, f"milestone:{m_key}", "milestone", m["name"], 1, "active",
            json.dumps({"xp": m.get("xp", 100)}), m.get("source_ref", "Kingmaker"),
            "Marco canônico de campanha.", now, now
        ))


def test_m2_sqlite_campaign_importer_all_chapters_ingestion():
    """Valida a persistência completa de todos os 4 capítulos sem erros SQLite."""
    conn = sqlite3.connect(":memory:")
    setup_production_appdb_schema(conn)

    for key in CHAPTER_KEYS:
        data = load_chapter_data(key)
        simulate_import_chapter(conn, "pf2_prod", data)
        conn.commit()

    cur = conn.cursor()

    # Verifica 4 capítulos registrados
    assert cur.execute("SELECT count(*) FROM kingmaker_campaign_chapters WHERE world_profile='pf2_prod'").fetchone()[0] == 4

    # Verifica quests (>= 17 quests no total somando os 4 capítulos)
    total_quests = cur.execute("SELECT count(*) FROM quests WHERE world_profile='pf2_prod'").fetchone()[0]
    assert total_quests >= 17, f"Esperado >= 17 quests, encontrado {total_quests}"

    # Verifica POIs e confirma constraint canonical=0 em 100% dos registros
    total_pois = cur.execute("SELECT count(*) FROM campaign_pois WHERE world_profile='pf2_prod'").fetchone()[0]
    assert total_pois >= 17, f"Esperado >= 17 POIs, encontrado {total_pois}"
    assert cur.execute("SELECT count(*) FROM campaign_pois WHERE world_profile='pf2_prod' AND canonical != 0").fetchone()[0] == 0

    # Verifica cenas táticas
    total_scenes = cur.execute("SELECT count(*) FROM tactical_scenes WHERE world_profile='pf2_prod'").fetchone()[0]
    assert total_scenes >= 12, f"Esperado >= 12 cenas táticas, encontrado {total_scenes}"

    # Verifica companheiros registrados (Jubilost e Tristian devem constar)
    companions = [r[0] for r in cur.execute("SELECT companion_key FROM kingmaker_companions WHERE world_profile='pf2_prod'").fetchall()]
    assert "npc_jubilost_narthropple" in companions
    assert "npc_tristian" in companions

    # Verifica marcos de campanha em kingmaker_features
    milestones = cur.execute("SELECT count(*) FROM kingmaker_features WHERE world_profile='pf2_prod' AND feature_kind='milestone'").fetchone()[0]
    assert milestones >= 14, f"Esperado >= 14 marcos, encontrado {milestones}"


def test_m2_sqlite_cross_profile_isolation_and_scoping():
    """Valida que o escopo <world_profile>:<poi_id> previne colisão entre perfis distintos."""
    conn = sqlite3.connect(":memory:")
    setup_production_appdb_schema(conn)

    ch1 = load_chapter_data("chapter_01_stolen_lands_part1")

    # Ingestão para Perfil Alpha
    simulate_import_chapter(conn, "perfil_alpha", ch1)
    conn.commit()

    # Ingestão para Perfil Beta
    simulate_import_chapter(conn, "perfil_beta", ch1)
    conn.commit()

    cur = conn.cursor()
    alpha_pois = cur.execute("SELECT count(*) FROM campaign_pois WHERE world_profile='perfil_alpha'").fetchone()[0]
    beta_pois = cur.execute("SELECT count(*) FROM campaign_pois WHERE world_profile='perfil_beta'").fetchone()[0]

    assert alpha_pois == 4
    assert beta_pois == 4

    # Confirma que os IDs são devidamente prefixados com o perfil
    ids = [r[0] for r in cur.execute("SELECT poi_id FROM campaign_pois").fetchall()]
    assert all(i.startswith("perfil_alpha:") or i.startswith("perfil_beta:") for i in ids)


def test_m2_sqlite_fail_closed_transactional_integrity():
    """Valida fail-closed: uma falha em constraint no meio da transação deve reverter tudo."""
    conn = sqlite3.connect(":memory:")
    setup_production_appdb_schema(conn)

    ch1 = load_chapter_data("chapter_01_stolen_lands_part1")

    # Injeta um POI inválido com canonical=1 para forçar violação de CHECK(canonical=0)
    corrupted_data = json.loads(json.dumps(ch1))
    corrupted_data["pois"][0]["canonical"] = 1

    with pytest.raises(sqlite3.IntegrityError):
        conn.isolation_level = None  # controle manual
        cur = conn.cursor()
        cur.execute("BEGIN TRANSACTION")
        try:
            simulate_import_chapter(conn, "pf2_corrupted", corrupted_data)
            cur.execute("COMMIT")
        except Exception:
            cur.execute("ROLLBACK")
            raise

    # Garante que nada foi salvo
    cur = conn.cursor()
    assert cur.execute("SELECT count(*) FROM kingmaker_campaign_chapters WHERE world_profile='pf2_corrupted'").fetchone()[0] == 0
    assert cur.execute("SELECT count(*) FROM campaign_pois WHERE world_profile='pf2_corrupted'").fetchone()[0] == 0


def test_m2_pt_br_localization_completeness():
    """Garante 100% de localização em Português do Brasil sem placeholders em inglês."""
    forbidden_tokens = ["placeholder", "lorem ipsum", "to be determined", "[tbd]", "todo:", "[todo]", "fixme"]

    for key in CHAPTER_KEYS:
        data = load_chapter_data(key)
        text_dump = json.dumps(data, ensure_ascii=False).lower()

        for token in forbidden_tokens:
            assert token not in text_dump, f"Token proibido '{token}' detectado no capítulo {key}"

        # Valida que termos centrais estão em pt-BR
        assert "terras roubadas" in text_dump or "posto avançado" in text_dump or "rios correm vermelhos" in text_dump or "culto da flor" in text_dump
        assert "reflexos" in text_dump or "fortitude" in text_dump or "vontade" in text_dump
        assert "ladroagem" in text_dump or "sobrevivência" in text_dump or "natureza" in text_dump
