import json
import glob
import os
import re
import sys

def audit_chapters():
    chapters_dir = os.path.abspath(r'android/app/src/main/assets/pathfinder/kingmaker/chapters')
    chapter_files = sorted(glob.glob(os.path.join(chapters_dir, 'chapter_*.json')))
    print(f"Total chapter files found: {len(chapter_files)}")
    assert len(chapter_files) == 12, f"Expected 12 chapters, found {len(chapter_files)}"

    valid_skills = {
        "ladroagem", "atletismo", "acrobacia", "religiao", "ocultismo", "arcanismo",
        "natureza", "percepcao", "sobrevivencia", "furtividade", "manufatura",
        "diplomacia", "intimidacao", "enganacao", "sociedade", "medicina", "desempenho"
    }
    valid_proficiencies = {"destreinado", "treinado", "especialista", "mestre", "lendario"}
    valid_saving_throws = {"reflexos", "fortitude", "vontade"}
    valid_poi_statuses = {"active", "abandoned", "ruined", "destroyed", "occupied", "rebuilding"}
    valid_npc_attitudes = {"hostil", "desfavoravel", "indiferente", "amistoso", "prestativo"}

    english_placeholders = [
        r'\bTBD\b', r'\bTODO\b', r'\bWIP\b', r'\bFIXME\b', r'\bPLACEHOLDER\b',
        r'\blorem ipsum\b', r'\[placeholder\]', r'\[TBD\]', r'\buntranslated\b'
    ]
    english_keywords = [
        r'\bChapter\s+\d+\b', r'\bPart\s+\d+\b', r'\bTrap\b', r'\bHaunt\b',
        r'\bReflex\b', r'\bFortitude\b', r'\bWill\b', r'\bTrained\b', r'\bExpert\b',
        r'\bMaster\b', r'\bLegendary\b'
    ]

    total_quests = 0
    total_hazards = 0
    total_pois = 0
    total_npcs = 0
    total_scenes = 0
    total_milestones = 0
    total_clocks = 0
    total_events = 0

    schema_errors = []
    hazard_errors = []
    localization_errors = []
    suspicious_items = []

    for cpath in chapter_files:
        cname = os.path.basename(cpath)
        with open(cpath, 'r', encoding='utf-8') as f:
            try:
                data = json.load(f)
            except Exception as e:
                schema_errors.append(f"{cname}: Invalid JSON: {e}")
                continue

        # 1. Chapter Root Schema
        schema = data.get("schema", "")
        if schema != "braseiro-pathfinder2e-kingmaker-chapter-v1":
            schema_errors.append(f"{cname}: Unexpected schema: {schema}")

        ch_num = data.get("chapter_number")
        if not isinstance(ch_num, int) or ch_num < 1 or ch_num > 12:
            schema_errors.append(f"{cname}: Invalid chapter_number: {ch_num}")

        ch_key = data.get("chapter_key", "")
        if not ch_key:
            schema_errors.append(f"{cname}: Missing chapter_key")

        title = data.get("title", "")
        if not title:
            schema_errors.append(f"{cname}: Missing title")

        locale = data.get("locale", "")
        if locale != "pt-BR":
            schema_errors.append(f"{cname}: locale is not pt-BR ({locale})")

        level_min = data.get("level_min", 0)
        level_max = data.get("level_max", 0)
        if not (1 <= level_min <= level_max <= 20):
            schema_errors.append(f"{cname}: Invalid levels: min={level_min}, max={level_max}")

        summary = data.get("summary", "")
        if not summary or len(summary) < 10:
            schema_errors.append(f"{cname}: Missing or too short summary")

        # Quests
        quests = data.get("quests", [])
        total_quests += len(quests)
        for q in quests:
            qkey = q.get("quest_key", "")
            qtitle = q.get("title", "")
            qobj = q.get("objective", "")
            qstakes = q.get("stakes", "")
            if not qkey:
                schema_errors.append(f"{cname} quest: missing quest_key ({qtitle})")
            if not qtitle:
                schema_errors.append(f"{cname} quest {qkey}: missing title")
            if not qobj:
                schema_errors.append(f"{cname} quest {qkey}: missing objective")
            objectives = q.get("objectives", [])
            for ob in objectives:
                if not ob.get("id") or not ob.get("desc"):
                    schema_errors.append(f"{cname} quest {qkey} objective: missing id or desc")

        # Hazards
        hazards = data.get("hazards", [])
        total_hazards += len(hazards)
        for h in hazards:
            hkey = h.get("hazard_key", "")
            hname = h.get("name", "")
            hlevel = h.get("level")
            stealth_dc = h.get("stealth_dc", 0)
            perception_dc = h.get("perception_dc", 0)
            saving_throw_dc = h.get("saving_throw_dc", 0)
            saving_throw_type = h.get("saving_throw_type", "")
            disable_skills = h.get("disable_skills", [])
            complexity = h.get("complexity", "")

            if not hkey or not hname:
                hazard_errors.append(f"{cname} hazard: missing key or name ({hkey}, {hname})")
            if not isinstance(hlevel, int) or not (0 <= hlevel <= 25):
                hazard_errors.append(f"{cname} hazard {hkey}: level {hlevel} out of range [0, 25]")
            if stealth_dc <= 0:
                hazard_errors.append(f"{cname} hazard {hkey}: invalid stealth_dc {stealth_dc}")
            if saving_throw_dc <= 0:
                hazard_errors.append(f"{cname} hazard {hkey}: invalid saving_throw_dc {saving_throw_dc}")
            if saving_throw_type.lower() not in valid_saving_throws:
                hazard_errors.append(f"{cname} hazard {hkey}: invalid saving_throw_type '{saving_throw_type}'")
            if not disable_skills:
                hazard_errors.append(f"{cname} hazard {hkey}: disable_skills is empty")
            for ds in disable_skills:
                skill = ds.get("skill", "").strip().lower()
                dc = ds.get("dc", 0)
                prof = ds.get("proficiency", "").strip().lower()
                if dc <= 0:
                    hazard_errors.append(f"{cname} hazard {hkey}: invalid disable dc {dc}")
                if prof not in valid_proficiencies:
                    hazard_errors.append(f"{cname} hazard {hkey}: invalid proficiency '{prof}'")
                # check skill
                if skill not in valid_skills and not any(k in skill for k in valid_skills):
                    hazard_errors.append(f"{cname} hazard {hkey}: unrecognized skill '{skill}'")

            if complexity == "complexo":
                routine = h.get("routine", [])
                if not routine:
                    hazard_errors.append(f"{cname} hazard {hkey}: complex hazard has empty routine")
                for r in routine:
                    if r.get("action_cost", 0) <= 0:
                        hazard_errors.append(f"{cname} hazard {hkey}: routine action cost <= 0")

        # POIs
        pois = data.get("pois", [])
        total_pois += len(pois)
        for p in pois:
            pkey = p.get("poi_key", "")
            pname = p.get("name", "")
            hex_id = p.get("hex_id", "")
            source = p.get("source", "")
            status = p.get("status", "")
            canonical = p.get("canonical", 0)
            if not pkey or not pname:
                schema_errors.append(f"{cname} poi: missing key or name")
            if not hex_id:
                schema_errors.append(f"{cname} poi {pkey}: missing hex_id")
            if source not in ("campaign", "procedural"):
                schema_errors.append(f"{cname} poi {pkey}: invalid source {source}")
            if canonical != 0:
                schema_errors.append(f"{cname} poi {pkey}: canonical must be 0 for mutable POIs, got {canonical}")
            if status not in valid_poi_statuses:
                schema_errors.append(f"{cname} poi {pkey}: invalid status {status}")

        # NPCs
        npcs = data.get("npcs", [])
        total_npcs += len(npcs)
        for n in npcs:
            nkey = n.get("npc_key", "")
            nname = n.get("name", "")
            att = n.get("attitude", "")
            if not nkey or not nname:
                schema_errors.append(f"{cname} npc: missing key or name")
            if att and att.lower() not in valid_npc_attitudes:
                schema_errors.append(f"{cname} npc {nkey}: invalid attitude '{att}'")

        # Scenes
        scenes = data.get("scenes", [])
        total_scenes += len(scenes)
        for s in scenes:
            skey = s.get("scene_key", "")
            stitle = s.get("title", "")
            if not skey or not stitle:
                schema_errors.append(f"{cname} scene: missing key or title")
            # Scene hazards
            for sh in s.get("hazards", []):
                sh_key = sh.get("hazard_key", "")
                if not sh_key:
                    schema_errors.append(f"{cname} scene {skey}: hazard without key")

        # Clocks, Events, Milestones
        clocks = data.get("clocks", [])
        total_clocks += len(clocks)
        events = data.get("events", [])
        total_events += len(events)
        milestones = data.get("milestones", [])
        total_milestones += len(milestones)

        # Check localization in text strings
        text_str = json.dumps(data, ensure_ascii=False)
        for ph in english_placeholders:
            matches = re.findall(ph, text_str, re.IGNORECASE)
            if matches:
                localization_errors.append(f"{cname}: Matched placeholder pattern '{ph}': {matches[:3]}")

        # Search for untranslated english words in user-facing texts
        # Inspect titles, summaries, descriptions, dialogues
        for q in quests:
            for field in ("title", "objective", "stakes"):
                val = q.get(field, "")
                for kw in [r'\bthe\b', r'\bquest\b', r'\bdefeat\b', r'\bfind\b', r'\breward\b']:
                    if re.search(kw, val, re.IGNORECASE):
                        localization_errors.append(f"{cname} quest {q.get('quest_key')} {field} has english '{kw}': {val[:60]}")

        for h in hazards:
            for field in ("name", "description"):
                val = h.get(field, "")
                for kw in [r'\btrap\b', r'\bhaunt\b', r'\bdisable\b']:
                    if re.search(kw, val, re.IGNORECASE):
                        # Some names might have legitimate words, let's flag for review
                        suspicious_items.append(f"{cname} hazard {h.get('hazard_key')} {field} has '{kw}': {val[:60]}")

    print("\n--- AUDIT SUMMARY ---")
    print(f"Total Quests: {total_quests}")
    print(f"Total Hazards: {total_hazards}")
    print(f"Total POIs: {total_pois}")
    print(f"Total NPCs: {total_npcs}")
    print(f"Total Scenes: {total_scenes}")
    print(f"Total Clocks: {total_clocks}")
    print(f"Total Events: {total_events}")
    print(f"Total Milestones: {total_milestones}")

    print(f"\nSchema Errors: {len(schema_errors)}")
    for e in schema_errors[:20]:
        print(f"  [SCHEMA ERROR] {e}")

    print(f"\nHazard Errors: {len(hazard_errors)}")
    for e in hazard_errors[:20]:
        print(f"  [HAZARD ERROR] {e}")

    print(f"\nLocalization Errors: {len(localization_errors)}")
    for e in localization_errors[:20]:
        print(f"  [LOCALIZATION ERROR] {e}")

    print(f"\nSuspicious Items (for manual check): {len(suspicious_items)}")
    for s in suspicious_items[:10]:
        print(f"  [SUSPICIOUS] {s}")

if __name__ == '__main__':
    audit_chapters()
