#!/usr/bin/env python3
"""Audita um export real 4.4.2 sem incorporar dados privados ao projeto."""

from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unicodedata
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
DEFAULT_FIXTURE = os.environ.get("BRASEIRO_LEGACY_EXPORT", "").strip()
FIXTURE = Path(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_FIXTURE).expanduser()

checks: list[tuple[str, bool]] = []


def check(name: str, condition: object) -> None:
    ok = bool(condition)
    checks.append((name, ok))
    print(("OK  " if ok else "FALHA  ") + name)


def normalized(value: object) -> str:
    text = unicodedata.normalize("NFD", str(value or "").lower())
    return re.sub(r"[^a-z0-9]+", " ", "".join(c for c in text if unicodedata.category(c) != "Mn")).strip()


def parse_embedded(value: object, fallback: object) -> object:
    if isinstance(value, (dict, list)):
        return value
    try:
        return json.loads(str(value or ""))
    except (TypeError, ValueError, json.JSONDecodeError):
        return fallback


def legacy_effect_key(row: dict) -> str:
    values = (
        row.get("world_profile", ""),
        row.get("title", ""),
        row.get("summary", ""),
        row.get("payload_json", ""),
    )
    created_at = max(0, int(row.get("created_at", 0) or 0))
    material = "\n".join(str(value or "").strip() for value in values) + "\n" + str(created_at)
    return "legacy-transaction:" + hashlib.sha256(material.encode("utf-8")).hexdigest()[:32]


def generated_identity_conflicts(character: dict) -> set[str]:
    authoritative = normalized(character.get("species"))
    known = {
        normalized(x): x
        for x in (
            "Humano", "Anão", "Anã", "Elfo", "Elfa", "Halfling", "Golias",
            "Gnomo", "Gnoma", "Draconato", "Draconata", "Tiefling", "Aasimar",
            "Orc", "Meio-orc", "Meio-elfo",
        )
    }
    sheet = parse_embedded(character.get("sheet_json"), {})
    values = [character.get("visual_description"), character.get("spells")]
    if isinstance(sheet, dict):
        values.extend(sheet.get(k) for k in ("appearance", "visual_description", "concept", "features", "spells"))
    haystack = " " + normalized(" ".join(str(v or "") for v in values)) + " "
    return {label for key, label in known.items() if key != authoritative and f" {key} " in haystack}


def execute_identity_repair(character: dict, foreign_species: str) -> bool:
    """Executa a classe Java de produção com os campos reais, sem salvar a fixture."""
    try:
        sheet = parse_embedded(character.get("sheet_json"), {})
        if not isinstance(sheet, dict):
            return False
        fields = ("concept", "appearance", "visual_description", "history", "life_path", "features", "spellcasting", "spells", "class_entry")
        row_fields = ("species", "class_name", "background", "visual_description", "spells", "notes")
        species = str(character.get("species", ""))
        cls = str(character.get("class_name", ""))
        background = str(character.get("background", ""))
        subclass = re.split(r"[—-]", cls, maxsplit=1)[1].strip() if re.search(r"[—-]", cls) else ""

        def literal(value: object) -> str:
            return json.dumps(str(value or ""), ensure_ascii=False)

        row_chain = "".join(f".put({literal(key)},{literal(character.get(key, ''))})" for key in row_fields)
        sheet_chain = "".join(f".put({literal(key)},{literal(sheet.get(key, ''))})" for key in fields if key in sheet)
        java_source = f'''package com.braseiro.dnd;
import org.json.JSONObject;
public final class LegacyFixtureRuntimeTest {{
  private static void require(boolean ok,String message){{if(!ok)throw new AssertionError(message);}}
  public static void main(String[] args)throws Exception{{
    JSONObject row=new JSONObject(){row_chain};
    require(CharacterIdentityCoherence.repairLegacyRow(row),"linha real não foi reparada");
    require(row.optString("visual_description").startsWith({literal(species + ',')}),"aparência real manteve espécie estrangeira");
    require(row.optString("visual_description").contains({literal(cls)}),"aparência real perdeu a classe autoritativa");
    JSONObject sheet=new JSONObject(){sheet_chain};
    require(CharacterIdentityCoherence.repairSheet(sheet,{literal(species)},{literal(cls)},{literal(background)},{literal(foreign_species)}),"sheet real não foi reparado");
    require(sheet.optString("concept").startsWith({literal(species + ' • ' + cls)}),"conceito real permaneceu contaminado");
    require(sheet.optString("appearance").startsWith({literal(species + ',')}),"aparência do sheet permaneceu contaminada");
    require(sheet.optString("features").contains({literal('Traços de espécie: ' + species)}),"traço de espécie real permaneceu contaminado");
    require({str(not subclass).lower()} || sheet.optString("features").contains({literal(subclass)}),"subclasse real permaneceu contaminada");
    System.out.println("OK: fixture real reparada pela classe Java de produção.");
  }}
}}
'''
        with tempfile.TemporaryDirectory(prefix="braseiro-legacy-fixture-") as temp:
            temp_path = Path(temp)
            source_path = temp_path / "LegacyFixtureRuntimeTest.java"
            source_path.write_text(java_source, "utf-8")
            runtime_json = sorted((ROOT / "tools/runtime_json/org/json").glob("*.java"))
            javac = shutil.which("javac") or str(ROOT / "tools/javac")
            java = shutil.which("java") or "/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
            command = [javac, "-encoding", "UTF-8", "-d", str(temp_path / "out")]
            command.extend(str(path) for path in runtime_json)
            command.extend((str(JAVA / "CharacterIdentityCoherence.java"), str(source_path)))
            compiled = subprocess.run(command, text=True, capture_output=True, timeout=60)
            if compiled.returncode != 0:
                print("DIAGNÓSTICO JAVAC FIXTURE: " + " ".join(compiled.stderr.splitlines()[:2])[:800])
                return False
            executed = subprocess.run(
                [java, "-cp", str(temp_path / "out"), "com.braseiro.dnd.LegacyFixtureRuntimeTest"],
                text=True, capture_output=True, timeout=30,
            )
            if executed.returncode != 0:
                print("DIAGNÓSTICO JVM FIXTURE: " + " ".join(executed.stderr.splitlines()[:3])[:800])
            return executed.returncode == 0 and "fixture real reparada" in executed.stdout
    except Exception as error:
        print("DIAGNÓSTICO FIXTURE: " + str(error).replace("\n", " ")[:800])
        return False


if not str(FIXTURE) or not FIXTURE.is_file():
    print("Uso: testar_export_legado_real.py CAMINHO_DO_EXPORT.json", file=sys.stderr)
    print("Ou defina BRASEIRO_LEGACY_EXPORT com o caminho da fixture privada.", file=sys.stderr)
    raise SystemExit(2)

try:
    raw = FIXTURE.read_bytes()
    backup = json.loads(raw.decode("utf-8"))
except Exception as exc:
    print(f"FALHA: export ilegível: {exc}", file=sys.stderr)
    raise SystemExit(1)

app_db = (JAVA / "AppDb.java").read_text("utf-8")
main = (JAVA / "MainActivity.java").read_text("utf-8")
coherence = (JAVA / "CharacterIdentityCoherence.java").read_text("utf-8")
transaction_identity = (JAVA / "LegacyTransactionIdentity.java").read_text("utf-8")
remapper = (JAVA / "BackupReferenceRemapper.java").read_text("utf-8")

profile = str(backup.get("worldProfile", "")).strip()
characters = backup.get("characters") if isinstance(backup.get("characters"), list) else []
character_ids = {int(row.get("id", 0) or 0) for row in characters if isinstance(row, dict)}

check("formato de backup reconhecido", backup.get("format") == "braseiro-dnd-backup")
check("fixture pertence à geração 4.4.2", backup.get("version") == 14 and str(backup.get("appVersion", "")).startswith("4.4.2"))
check("escopo portátil de campanha", backup.get("backupScope") == "campaign")
check("perfil D&D declarado", profile == "dnd" and backup.get("campaign", {}).get("world_profile") == profile)
check("personagens reais presentes", len(characters) >= 1 and all(isinstance(row, dict) for row in characters))

profile_problems: list[str] = []
for key, value in backup.items():
    rows = value if isinstance(value, list) else [value] if isinstance(value, dict) else []
    for index, row in enumerate(rows):
        if isinstance(row, dict) and "world_profile" in row and str(row.get("world_profile", "")).strip() != profile:
            profile_problems.append(f"{key}[{index}]")
check("nenhuma linha mistura D&D e Mystara", not profile_problems)

linked_rows = []
for key in ("characterControls", "heroLifeProfiles", "lifeRelationships", "lifeActivities", "partyActions"):
    for row in backup.get(key, []) if isinstance(backup.get(key), list) else []:
        if isinstance(row, dict) and int(row.get("character_id", 0) or 0) > 0:
            linked_rows.append((key, int(row["character_id"])))
check("referências de personagem do export existem", all(character_id in character_ids for _, character_id in linked_rows))
check("restauração remapeia referências profundas", all(token in remapper for token in ("character_id", "activity_id", "troupe_id", "quest_id", "transaction_id", "effect_key")))

contaminated = [(row.get("id"), generated_identity_conflicts(row)) for row in characters if generated_identity_conflicts(row)]
check("fixture reproduz contradição gerada de identidade", len(contaminated) >= 1)
contaminated_rows = [row for row in characters if generated_identity_conflicts(row)]
runtime_repair_ok = bool(contaminated_rows) and execute_identity_repair(contaminated_rows[0], sorted(generated_identity_conflicts(contaminated_rows[0]))[0])
check("reparo Java executa sobre campos da fixture real", runtime_repair_ok)
check("importação repara identidade antes de inserir personagem", "CharacterIdentityCoherence.repairLegacyRow(c);long oldId" in app_db)
check("reparo preserva ficha como autoridade", all(token in coherence for token in ("repairLegacyRow", "repairSheet", "detectForeignSpecies", "Traços de espécie:", "Subclasse de nível")))
check("migração também corrige campanhas já instaladas", "repairLegacyCharacterIdentities(db);" in app_db)

transactions = backup.get("pendingTransactions") if isinstance(backup.get("pendingTransactions"), list) else []
blank_effect_keys = [row for row in transactions if isinstance(row, dict) and not str(row.get("effect_key", "")).strip()]
derived_keys = [legacy_effect_key(row) for row in blank_effect_keys]
applied_without_receipt = sum(1 for row in blank_effect_keys if row.get("status") == "applied")
check("fixture reproduz effect_key legado ausente", len(blank_effect_keys) >= 1)
check("chaves derivadas são estáveis e distintas", len(derived_keys) == len(set(derived_keys)) and all(re.fullmatch(r"legacy-transaction:[0-9a-f]{32}", key) for key in derived_keys))
check("fixture cobre recibo ausente de transação aplicada", applied_without_receipt >= 1 and not backup.get("effectReceipts"))
check("migração preenche chave e recibo na mesma restauração", all(token in app_db for token in ("prepareLegacyTransactionRows", "LegacyTransactionIdentity.keyForBackupRow(source)", "backfillLegacyTransactionIdentities(db,backupProfile)", "migração de transação legada", "CONFLICT_IGNORE")))
check("auditoria rejeita transação órfã ou sem recibo", all(token in app_db for token in ("permaneceu sem effect_key", "não possui recibo", "effect_key pendente colidiu")))
check("algoritmo Java usa identidade SHA-256 estável", all(token in transaction_identity for token in ("SHA-256", "legacy-transaction:", "createdAt")))

story = backup.get("storyState") if isinstance(backup.get("storyState"), dict) else {}
options = parse_embedded(story.get("options_json"), [])
pending_test = parse_embedded(story.get("pending_test_json"), {})
lost_options_case = int(story.get("started", 0) or 0) == 1 and len(str(story.get("last_narration", "")).strip()) > 100 and options == [] and pending_test in ({}, "", None)
check("fixture reproduz sugestões perdidas após cena válida", lost_options_case)
check("retomada recria e persiste sugestões ausentes", all(token in main for token in ("currentOptions.length()==0", "SceneOptionDiversifier.diversify", "db.saveStoryState")))

map_state = backup.get("mapState") if isinstance(backup.get("mapState"), dict) else {}
portable_text = json.dumps(map_state, ensure_ascii=False)
check("estado de mapa não depende de caminho privado", not re.search(r"(?i)(?:file:/{2,3}|content://|/data/user/|[A-Z]:\\\\)", portable_text))
check("restauração religa mapa por identidade lógica", all(token in app_db for token in ("restorePortableMapState", "logical_map_id", "getLatestMapPackageByMapId")))

on_create = app_db[app_db.find("public void onCreate"):app_db.find("public void onUpgrade")]
check("instalação limpa recebe o esquema integral", "ensureMasterIntegrationColumns(db);" in on_create)
check("banco v47 migra a geração 4.4.2", bool(re.search(r"DB_VERSION\s*=\s*47", app_db)) and "oldVersion < 46" in app_db and "oldVersion < 47" in app_db)
check("importação v14 permanece aceita", ("backupVersion>18" in app_db or "backupVersion>20" in app_db) and "backupVersion<=0" in app_db)

failed = [name for name, ok in checks if not ok]
print(
    "FIXTURE REAL: "
    f"sha256={hashlib.sha256(raw).hexdigest()} • personagens={len(characters)} • "
    f"transações_legadas={len(blank_effect_keys)} • identidades_contaminadas={len(contaminated)}"
)
print(f"RESULTADO EXPORT 4.4.2: {len(checks) - len(failed)}/{len(checks)} verificações aprovadas.")
if failed:
    print("FALHAS:")
    for name in failed:
        print(" - " + name)
    raise SystemExit(1)
