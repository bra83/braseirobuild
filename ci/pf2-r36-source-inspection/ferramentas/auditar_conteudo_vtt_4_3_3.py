#!/usr/bin/env python3
from pathlib import Path
import json
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
maps = root / "android/app/src/main/assets/maps"
checks = []

def check(label, condition, detail=""):
    checks.append((label, bool(condition), detail))

with zipfile.ZipFile(maps / "faerun_maxres_integrated.zip") as archive:
    atlas = json.loads(archive.read("faerun_5e_hex_atlas_v2.json"))["hexes"]
    locations = json.loads(archive.read("faerun_5e_locations_v2.json"))["locations"]
    index = json.loads(archive.read("98_indice_geral.json"))
    eh92 = atlas.get("EH92", {})
    linked = sum(
        1 for location in locations
        if location.get("hexId") or location.get("hex_id") or location.get("placement")
    )
    check("Faerûn possui 59.334 células cartográficas", len(atlas) == 59334, str(len(atlas)))
    check("Faerûn possui 351 locais consolidados", len(locations) == 351, str(len(locations)))
    check("índice-base de Faerûn possui 397 registros", index.get("total_registros_base_pronta") == 397,
          str(index.get("total_registros_base_pronta")))
    check("posicionamento de locais não é inventado", linked == 0, str(linked))
    check(
        "EH92 permanece neutro e marcado para revisão",
        eh92.get("terrain", {}).get("primary") is None
        and eh92.get("terrain", {}).get("confidence") == 0.0
        and eh92.get("review", {}).get("status") == "needs_review"
        and not eh92.get("locationIds"),
        json.dumps(eh92, ensure_ascii=False),
    )

with zipfile.ZipFile(maps / "mystara_known_world_integrated.zip") as archive:
    index = json.loads(archive.read("hex_index_mystara.json"))
    terrain = json.loads(archive.read("mystara_hex_terrain_atlas.json"))["hexes"]
    locations = json.loads(archive.read("mystara_locations_starter.json"))["locations"]
    check("Mystara possui 71.726 células indexadas", len(index["hexes"]) == 71726, str(len(index["hexes"])))
    check("atlas de terreno de Mystara cobre o índice", len(terrain) == len(index["hexes"]), str(len(terrain)))
    check("catálogo inicial de Mystara preserva 21 locais", len(locations) == 21, str(len(locations)))

failed = [label for label, ok, _ in checks if not ok]
for label, ok, detail in checks:
    print(("OK  " if ok else "FALHA  ") + label + ("" if ok else ": " + detail))
print(f"RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações cartográficas aprovadas.")
sys.exit(1 if failed else 0)
