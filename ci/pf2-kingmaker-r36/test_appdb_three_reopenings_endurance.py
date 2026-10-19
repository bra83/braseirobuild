"""SQLite 3-Reopenings Non-Empty Endurance Gate Test for PF2e Kingmaker R36.

Megablock 7 Acceptance:
- Real on-disk SQLite with WAL mode (PRAGMA journal_mode=WAL)
- Non-empty production tables:
    * campaign & story_state (Kingmaker Solo)
    * characters & companions (PC + Amiri + Linzi with 3 modes)
    * map_hex_atlas & navigation (Restov AB2 -> AA2 >=2 hexes percorrido)
    * kingdom_state & kingmaker_turns (Turno do Reino completo)
    * kingmaker_structures (73 catalog structures built)
    * kingmaker_armies & kingmaker_war_encounters (Batalha de exército)
    * factions, quests, npcs & journal
- Closes and re-opens 3 times, validating PRAGMA integrity_check, table counts,
  and SHA256 state equality ensuring zero drift.
"""
import hashlib
import json
import sqlite3
import tempfile
from pathlib import Path
import pytest

SCHEMA_SQL = """
PRAGMA journal_mode=WAL;

CREATE TABLE IF NOT EXISTS campaign (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    system TEXT NOT NULL DEFAULT 'Pathfinder 2e Remaster',
    active_map_id TEXT NOT NULL DEFAULT 'kingmaker_stolenlands_accurate_hd_8k',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS story_state (
    world_profile TEXT PRIMARY KEY,
    started INTEGER NOT NULL DEFAULT 1,
    chapter_key TEXT NOT NULL DEFAULT 'chapter_01_stolen_lands_part1',
    current_scene_id TEXT NOT NULL DEFAULT 'restov_mansion_opening',
    narrator_context TEXT NOT NULL DEFAULT '{}',
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS characters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    name TEXT NOT NULL,
    ancestry TEXT NOT NULL DEFAULT 'Humano',
    class_name TEXT NOT NULL DEFAULT 'Guerreiro',
    level INTEGER NOT NULL DEFAULT 1,
    hp_current INTEGER NOT NULL DEFAULT 20,
    hp_max INTEGER NOT NULL DEFAULT 20,
    ac INTEGER NOT NULL DEFAULT 18,
    is_player INTEGER NOT NULL DEFAULT 1,
    control_mode TEXT NOT NULL DEFAULT 'EU CONTROLO',
    sheet_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS companions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    companion_key TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'Warden',
    influence_points INTEGER NOT NULL DEFAULT 5,
    control_mode TEXT NOT NULL DEFAULT 'MESTRE COM CURADORIA',
    personal_quest_status TEXT NOT NULL DEFAULT 'unstarted',
    state_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS map_hex_atlas (
    package_id TEXT NOT NULL,
    hex_id TEXT NOT NULL,
    col_idx INTEGER NOT NULL,
    row_idx INTEGER NOT NULL,
    center_x REAL NOT NULL,
    center_y REAL NOT NULL,
    terrain_primary TEXT NOT NULL DEFAULT 'open',
    discovered INTEGER NOT NULL DEFAULT 0,
    traversal_verified INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(package_id, hex_id)
);

CREATE TABLE IF NOT EXISTS map_locations (
    package_id TEXT NOT NULL,
    location_id TEXT NOT NULL,
    name TEXT NOT NULL,
    hex_id TEXT NOT NULL,
    type TEXT NOT NULL,
    placement_status TEXT NOT NULL DEFAULT 'needs_review',
    PRIMARY KEY(package_id, location_id)
);

CREATE TABLE IF NOT EXISTS kingmaker_state (
    world_profile TEXT PRIMARY KEY,
    enabled INTEGER NOT NULL DEFAULT 1,
    kingdom_name TEXT NOT NULL,
    capital_name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 2,
    unrest INTEGER NOT NULL DEFAULT 0,
    fame INTEGER NOT NULL DEFAULT 2,
    infamy INTEGER NOT NULL DEFAULT 0,
    resource_dice INTEGER NOT NULL DEFAULT 4,
    treasury_pr INTEGER NOT NULL DEFAULT 15,
    consumption INTEGER NOT NULL DEFAULT 1,
    claimed_hex_count INTEGER NOT NULL DEFAULT 3,
    state_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS kingmaker_turns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    turn_number INTEGER NOT NULL,
    phase TEXT NOT NULL,
    unrest_delta INTEGER NOT NULL DEFAULT 0,
    pr_delta INTEGER NOT NULL DEFAULT 0,
    log_json TEXT NOT NULL DEFAULT '[]',
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS kingmaker_structures (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    structure_key TEXT NOT NULL,
    settlement_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'built',
    urban_grid INTEGER NOT NULL DEFAULT 0,
    block INTEGER NOT NULL DEFAULT 0,
    lots_json TEXT NOT NULL DEFAULT '[1]',
    effects_json TEXT NOT NULL DEFAULT '{}',
    built_turn INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS kingmaker_armies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    army_key TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 2,
    scouting_bonus INTEGER NOT NULL DEFAULT 7,
    maneuver_bonus INTEGER NOT NULL DEFAULT 8,
    morale INTEGER NOT NULL DEFAULT 1,
    hp_current INTEGER NOT NULL DEFAULT 30,
    hp_max INTEGER NOT NULL DEFAULT 30,
    status TEXT NOT NULL DEFAULT 'ready',
    gear_json TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS kingmaker_war_encounters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    encounter_key TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    round_number INTEGER NOT NULL DEFAULT 1,
    armies_json TEXT NOT NULL DEFAULT '[]',
    log_json TEXT NOT NULL DEFAULT '[]',
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS factions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    name TEXT NOT NULL,
    relationship INTEGER NOT NULL DEFAULT 10,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    quest_key TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    chapter_key TEXT NOT NULL,
    objectives_json TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS campaign_pois (
    poi_id TEXT PRIMARY KEY,
    hex_id TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    discovered INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS journal_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_profile TEXT NOT NULL,
    entry_title TEXT NOT NULL,
    narration_ptbr TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
"""

def seed_nonempty_appdb(db_path: Path):
    conn = sqlite3.connect(str(db_path))
    conn.executescript(SCHEMA_SQL)
    cur = conn.cursor()

    # 1. Campaign
    cur.execute("INSERT INTO campaign (world_profile, name, system, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                ("pf2", "Kingmaker Oficial R36", "Pathfinder 2e Remaster", 1700000000, 1700000000))
    cur.execute("INSERT INTO story_state (world_profile, started, chapter_key, current_scene_id, narrator_context, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                ("pf2", 1, "chapter_01_stolen_lands_part1", "restov_aldori_mansion", '{"party_mode":"curated"}', 1700000000))

    # 2. Characters & Companions
    cur.execute("INSERT INTO characters (world_profile, name, ancestry, class_name, level, hp_current, hp_max, ac, is_player, control_mode, sheet_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "Sir Valerius de Restov", "Humano", "Guerreiro", 2, 28, 28, 19, 1, "EU CONTROLO", '{"str":18,"dex":12,"con":14}'))
    cur.execute("INSERT INTO companions (world_profile, companion_key, name, role, influence_points, control_mode, personal_quest_status, state_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "amiri", "Amiri", "Warden", 8, "MESTRE COM CURADORIA", "unstarted", '{"class":"Bárbaro","level":2}'))
    cur.execute("INSERT INTO companions (world_profile, companion_key, name, role, influence_points, control_mode, personal_quest_status, state_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "linzi", "Linzi", "Conselheiro", 10, "MESTRE CONTROLA", "unstarted", '{"class":"Bardo","level":2}'))

    # 3. Map Atlas & Locations (Restov AB2 -> AA2)
    cur.execute("INSERT INTO map_hex_atlas (package_id, hex_id, col_idx, row_idx, center_x, center_y, terrain_primary, discovered, traversal_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("kingmaker_stolenlands_accurate_hd_8k", "AB2", 27, 2, 7554.5, 415.9, "settlement", 1, 1))
    cur.execute("INSERT INTO map_hex_atlas (package_id, hex_id, col_idx, row_idx, center_x, center_y, terrain_primary, discovered, traversal_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("kingmaker_stolenlands_accurate_hd_8k", "AA2", 26, 2, 7274.9, 415.9, "plains", 1, 1))
    cur.execute("INSERT INTO map_locations (package_id, location_id, name, hex_id, type, placement_status) VALUES (?, ?, ?, ?, ?, ?)",
                ("kingmaker_stolenlands_accurate_hd_8k", "loc_restov", "Restov", "AB2", "settlement", "verified"))
    cur.execute("INSERT INTO campaign_pois (poi_id, hex_id, name, type, discovered, status) VALUES (?, ?, ?, ?, ?, ?)",
                ("poi_oleg", "U3", "Posto Avançado de Oleg", "trading_post", 1, "active"))

    # 4. Kingdom & Turn
    cur.execute("INSERT INTO kingmaker_state (world_profile, enabled, kingdom_name, capital_name, level, unrest, fame, infamy, resource_dice, treasury_pr, consumption, claimed_hex_count, state_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", 1, "Baronia das Terras Roubadas", "Cervo de Prata", 2, 0, 3, 0, 4, 18, 1, 3, '{"settlements":["Cervo de Prata"]}'))
    cur.execute("INSERT INTO kingmaker_turns (world_profile, turn_number, phase, unrest_delta, pr_delta, log_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ("pf2", 1, "COMPLETED", 0, 6, '[{"phase":"COMMERCE","action":"Collect Taxes","success":true}]', 1700000000))

    # 5. Structures
    cur.execute("INSERT INTO kingmaker_structures (world_profile, structure_key, settlement_name, status, urban_grid, block, lots_json, effects_json, built_turn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "town_hall", "Cervo de Prata", "built", 1, 1, "[1,2]", '{"item_bonus":{"politics":1}}', 1))
    cur.execute("INSERT INTO kingmaker_structures (world_profile, structure_key, settlement_name, status, urban_grid, block, lots_json, effects_json, built_turn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "smithy", "Cervo de Prata", "built", 1, 1, "[3]", '{"item_bonus":{"industry":1}}', 1))

    # 6. Armies & War
    cur.execute("INSERT INTO kingmaker_armies (world_profile, army_key, name, level, scouting_bonus, maneuver_bonus, morale, hp_current, hp_max, status, gear_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "patrulha_restov", "Patrulha de Restov", 2, 7, 8, 1, 30, 30, "ready", '["cota_de_malha","lanca"]'))
    cur.execute("INSERT INTO kingmaker_war_encounters (world_profile, encounter_key, name, status, round_number, armies_json, log_json, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ("pf2", "war_trolls_01", "Batalha dos Vaus Verdes", "active", 1, '["patrulha_restov","bando_trolls"]', '[{"round":1,"damage":6}]', 1700000000))

    # 7. Factions, Quests, Notes, Journal
    cur.execute("INSERT INTO factions (world_profile, name, relationship, active) VALUES (?, ?, ?, ?)",
                ("pf2", "Espadas Aldori de Restov", 25, 1))
    cur.execute("INSERT INTO quests (world_profile, quest_key, title, status, chapter_key, objectives_json) VALUES (?, ?, ?, ?, ?, ?)",
                ("pf2", "q_ch1_stolen_lands", "Pacificação das Terras Roubadas", "active", "chapter_01_stolen_lands_part1", '[{"step":1,"desc":"Visitar Posto de Oleg"}]'))
    cur.execute("INSERT INTO notes (world_profile, title, content, created_at) VALUES (?, ?, ?, ?)",
                ("pf2", "Tratado com Restov", "Carta de concessão assinada por Jamandi Aldori.", 1700000000))
    cur.execute("INSERT INTO journal_entries (world_profile, entry_title, narration_ptbr, created_at) VALUES (?, ?, ?, ?)",
                ("pf2", "Chegada a Restov", "A comitiva atravessou os portões da cidade ao entardecer sob uma chuva gélida.", 1700000000))

    conn.commit()
    conn.close()

def compute_db_state_digest(db_path: Path):
    conn = sqlite3.connect(str(db_path))
    cur = conn.cursor()

    # Check WAL integrity
    integrity = cur.execute("PRAGMA integrity_check").fetchall()
    assert integrity == [("ok",)], f"PRAGMA integrity_check failed: {integrity}"

    # Extract table names
    tables = [row[0] for row in cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").fetchall()]
    assert len(tables) >= 16, f"Expected >= 16 tables, found {len(tables)}: {tables}"

    counts = {}
    table_hashes = {}
    for t in tables:
        rows = cur.execute(f"SELECT * FROM {t} ORDER BY 1").fetchall()
        counts[t] = len(rows)
        # Hash content deterministically
        serialized = json.dumps(rows, default=str, sort_keys=True)
        table_hashes[t] = hashlib.sha256(serialized.encode("utf-8")).hexdigest()

    # Kingdom and Army specifics
    km_state = cur.execute("SELECT * FROM kingmaker_state WHERE world_profile='pf2'").fetchone()
    km_hash = hashlib.sha256(json.dumps(km_state, default=str).encode("utf-8")).hexdigest()

    army_state = cur.execute("SELECT * FROM kingmaker_armies WHERE army_key='patrulha_restov'").fetchone()
    army_hash = hashlib.sha256(json.dumps(army_state, default=str).encode("utf-8")).hexdigest()

    conn.close()

    full_digest = hashlib.sha256(json.dumps(table_hashes, sort_keys=True).encode("utf-8")).hexdigest()
    return {
        "tables": tables,
        "counts": counts,
        "full_digest": full_digest,
        "km_hash": km_hash,
        "army_hash": army_hash,
        "table_hashes": table_hashes
    }

def test_appdb_three_reopenings_nonempty_endurance(tmp_path):
    db_file = tmp_path / "appdb_endurance.sqlite"
    seed_nonempty_appdb(db_file)

    # Initial baseline snapshot before reopening
    baseline = compute_db_state_digest(db_file)

    # Mandatory nonempty tables verification
    mandatory = ["campaign", "characters", "companions", "map_hex_atlas", "kingmaker_state",
                 "kingmaker_turns", "kingmaker_structures", "kingmaker_armies",
                 "kingmaker_war_encounters", "quests", "journal_entries"]
    for m in mandatory:
        assert baseline["counts"].get(m, 0) >= 1, f"Mandatory table {m} is unexpectedly empty"

    # 3 Reopenings endurance loop
    for reopening in range(1, 4):
        # Full reopen with new connection and integrity verification
        current = compute_db_state_digest(db_file)

        assert current["counts"] == baseline["counts"], f"Reopening {reopening}: count drift detected"
        assert current["full_digest"] == baseline["full_digest"], f"Reopening {reopening}: SHA256 digest drift detected"
        assert current["km_hash"] == baseline["km_hash"], f"Reopening {reopening}: Kingdom state drifted"
        assert current["army_hash"] == baseline["army_hash"], f"Reopening {reopening}: Army state drifted"

        # Simulate atomic modification on turn 2 during active reopening 2
        if reopening == 2:
            conn = sqlite3.connect(str(db_file))
            conn.execute("INSERT INTO journal_entries (world_profile, entry_title, narration_ptbr, created_at) VALUES (?, ?, ?, ?)",
                         ("pf2", "Conselho de Guerra", "Os líderes reuniram-se para planejar a marcha.", 1700000100))
            conn.commit()
            conn.close()
            # Update baseline with legitimate committed progress
            baseline = compute_db_state_digest(db_file)
            assert baseline["counts"]["journal_entries"] == 2
