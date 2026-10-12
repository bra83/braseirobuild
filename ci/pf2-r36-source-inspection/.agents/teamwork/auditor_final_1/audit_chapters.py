import json
import os
import glob

chapters_dir = r"android/app/src/main/assets/pathfinder/kingmaker/chapters"
files = sorted(glob.glob(os.path.join(chapters_dir, "*.json")))
print(f"Total chapter files found: {len(files)}")

total_quests = 0
total_encounters = 0
total_hazards = 0
total_npcs = 0
total_pois = 0

hazard_audit_failures = []
placeholder_matches = []

placeholders = ["TODO", "FIXME", "PLACEHOLDER", "LOREM IPSUM", "DUMMY", "MOCK_RETURN"]

for fpath in files:
    fname = os.path.basename(fpath)
    with open(fpath, "r", encoding="utf-8") as f:
        content = f.read()

    for p in placeholders:
        if p in content.upper():
            idx = content.upper().find(p)
            snippet = content[max(0, idx - 30):min(len(content), idx + 50)].replace("\n", " ")
            placeholder_matches.append((fname, p, snippet))

    try:
        data = json.loads(content)
    except Exception as e:
        print(f"ERROR: {fname} failed JSON parse: {e}")
        continue

    ch_num = data.get("chapter_number") or data.get("chapter")
    ch_title = data.get("chapter_title") or data.get("title")
    quests = data.get("quests", [])
    encounters = data.get("encounters", [])
    hazards = data.get("hazards", [])
    npcs = data.get("npcs", [])
    pois = data.get("hex_pois", []) or data.get("pois", [])

    total_quests += len(quests)
    total_encounters += len(encounters)
    total_hazards += len(hazards)
    total_npcs += len(npcs)
    total_pois += len(pois)

    print(f"{fname}: ch={ch_num}, title='{ch_title}', quests={len(quests)}, encounters={len(encounters)}, hazards={len(hazards)}, npcs={len(npcs)}, pois={len(pois)}")

    for h in hazards:
        hname = h.get("name") or h.get("id")
        lvl = h.get("level")
        traits = h.get("traits", [])
        disable = h.get("disable") or h.get("disable_dcs")
        ac = h.get("ac")
        hardness = h.get("hardness")
        hp = h.get("hp")
        bt = h.get("broken_threshold") or h.get("bt")
        routine = h.get("routine")
        saves = h.get("saving_throws") or h.get("saves") or h.get("saving_throw_dc") or h.get("save_dc")
        stealth = h.get("stealth") or h.get("stealth_dc")

        missing = []
        if lvl is None:
            missing.append("level")
        if not traits:
            missing.append("traits")
        if not disable:
            missing.append("disable")
        if routine is None and not h.get("reaction") and not h.get("description"):
            missing.append("routine/reaction/description")
        if missing:
            hazard_audit_failures.append((fname, hname, missing))

print(f"\nTotals: quests={total_quests}, encounters={total_encounters}, hazards={total_hazards}, npcs={total_npcs}, pois={total_pois}")
print(f"Placeholder matches: {len(placeholder_matches)}")
if placeholder_matches:
    for m in placeholder_matches:
        print("  Placeholder match:", m)
print(f"Hazard audit missing core fields: {len(hazard_audit_failures)}")
if hazard_audit_failures:
    for fail in hazard_audit_failures[:10]:
        print("  Hazard audit failure:", fail)
