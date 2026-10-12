#!/usr/bin/env python3
"""R36 Kingmaker: apply checked canonical plot anchors to exact quest records.

Narrative metadata only, not certification of encounters, branching, or saves.
Preflight all six files so a missing quest cannot leave a half-patched campaign.
"""
import json
import os
import sys
import tempfile
from pathlib import Path

ASSETS = Path("android/app/src/main/assets/pathfinder/kingmaker/chapters")
# Assets are zero-indexed; official adventure chapter numbers are not.
ANCHORS = (
    ("chapter_08_they_lurk_below.json", "q_aberracoes_subterraneas", {
        "title": "O Flagelo das Profundezas de Candlemere: Foras e os Thresholders de Yog-Sothoth",
        "objective": "Investigar o complexo subterraneo sob a Ilha de Candlemere, confrontar o culto dos Thresholders liderado por Foras e impedir a manifestacao aberrante de Yog-Sothoth.",
        "stakes": "Se o portal do vazio primordial nao for selado, horrores inimaginaveis de Yog-Sothoth consumirao a realidade das Terras Roubadas.",
    }),
    ("chapter_01_stolen_lands_part1.json", "q_carta_restov", {
        "objective_append": "Investigar e repelir os assassinos da guilda Lágrimas Negras (Black Tears) infiltrados em Restov.",
    }),
    ("chapter_04_cult_of_bloom.json", "q_utero_lamashtu", {
        "objective_append": "Investigar os acontecimentos no vilarejo de Darivan e purificar o útero de Lamashtu.",
    }),
    ("chapter_06_blood_for_blood.json", "q_incursoes_tigres", {
        "objective_append": "Defender o assentamento de Tatzlford das investidas dos Senhores Tigres e investigar as alianças secretas de Fort Drelev.",
    }),
    ("chapter_09_sound_thousand_screams.json", "q_incursao_primeiro_mundo", {
        "objective_append": "Cruzar o portal feérico para a floresta primordial de Thousandbreaths (Mil Respiros) e confrontar as hordas de Nyrissa.",
    }),
    ("chapter_10_curse_lantern_king.json", "q_revelacao_rei_lanterna", {
        "objective_append": "Desvendar o enigma de The Apology (A Apologia), navegar pelo pesadelo feérico de Hunting Dream (Sonho de Caça) e confrontar a soberania do Lantern Kingdom (Reino da Lanterna).",
    }),
)

def plan(project_dir):
    """Return every intended file change; reject incomplete or ambiguous inputs."""
    root = Path(project_dir)
    version = root / "version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("Expected generated R36 9.0.37 project; immutable R35 is prohibited")
    ch_dir = root / ASSETS
    if not ch_dir.is_dir():
        raise FileNotFoundError(f"Campaign directory absent: {ch_dir}")
    changes = []
    for filename, key, fields in ANCHORS:
        path = ch_dir / filename
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict) or not isinstance(data.get("quests"), list):
            raise ValueError(f"Malformed chapter quests: {filename}")
        matches = [q for q in data["quests"] if isinstance(q, dict) and q.get("quest_key") == key]
        if len(matches) != 1:
            raise ValueError(f"Expected exactly one quest {key!r} in {filename}; found {len(matches)}")
        quest = matches[0]
        for field, value in fields.items():
            if field == "objective_append":
                original = quest.get("objective", "")
                if not isinstance(original, str):
                    raise ValueError(f"Nontext objective in {filename}:{key}")
                if value not in original:
                    quest["objective"] = (original.rstrip() + " " + value).strip()
            else:
                quest[field] = value
        changes.append((path, (json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8")))
    return changes

def patch(project_dir):
    """Validate every chapter before writing; restore replaced files on failure."""
    changes = plan(project_dir)
    staged, originals, replaced = [], {}, []
    try:
        for path, content in changes:
            originals[path] = path.read_bytes()
            fd, tmp = tempfile.mkstemp(prefix=".r36-", suffix=".json", dir=path.parent)
            staged.append(Path(tmp))
            with os.fdopen(fd, "wb") as stream:
                stream.write(content)
                stream.flush()
                os.fsync(stream.fileno())
        for (path, _), tmp in zip(changes, staged):
            os.replace(tmp, path)
            replaced.append(path)
    except Exception:
        for path in reversed(replaced):
            path.write_bytes(originals[path])
        raise
    finally:
        for tmp in staged:
            tmp.unlink(missing_ok=True)
    print("KINGMAKER_R36_CANONICAL_ANCHORS_BATCH_APPLIED", len(changes))
    return tuple(path for path, _ in changes)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_chapter9_canonical_anchors.py <R36_project_dir>")
    patch(sys.argv[1])
