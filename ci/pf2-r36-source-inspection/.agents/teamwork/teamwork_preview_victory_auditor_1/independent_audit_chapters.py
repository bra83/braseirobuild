import json
import glob
import os
import re

chapters = sorted(glob.glob('android/app/src/main/assets/pathfinder/kingmaker/chapters/*.json'))
print(f"Total chapters found: {len(chapters)}")
assert len(chapters) == 12, f"Expected 12 chapters, found {len(chapters)}"

total_quests = 0
total_hazards = 0
total_pois = 0
total_npcs = 0
total_scenes = 0
total_xp = 0

hazard_levels = []
hazard_saving_throws = set()
hazard_disable_proficiencies = set()

suspicious_terms = ['TODO', 'TBD', 'FIXME', 'PLACEHOLDER', 'Lorem ipsum', 'mock', 'fake', 'dummy']

for idx, f in enumerate(chapters, 1):
    with open(f, 'r', encoding='utf-8') as fp:
        raw_text = fp.read()
    
    for term in suspicious_terms:
        if term == 'TODO':
            # Check for uppercase standalone TODO
            m = re.search(r'\bTODO\b', raw_text)
            if m:
                print(f"SUSPICIOUS uppercase TODO found in {f}")
        else:
            if term.lower() in raw_text.lower():
                # Allow Portuguese words
                if term in ['fake', 'dummy', 'mock', 'placeholder']:
                    print(f"SUSPICIOUS {term} found in {f}")

    data = json.loads(raw_text)
    
    chapter_num = data.get('chapter_number') or data.get('chapterNumber')
    chapter_key = data.get('chapter_key') or data.get('chapterKey')
    title = data.get('title') or data.get('title_pt_br')
    schema = data.get('schema') or data.get('schema_version') or data.get('schemaVersion')
    
    assert schema in ['braseiro-pathfinder2e-chapter-v1', 'braseiro-pathfinder2e-kingmaker-chapter-v1'], f"Invalid schema in {f}: {schema}"
    
    quests = data.get('quests', [])
    hazards = data.get('hazards', [])
    pois = data.get('hex_pois', []) or data.get('pois', [])
    npcs = data.get('npcs', [])
    scenes = data.get('tactical_scenes', []) or data.get('scenes', [])
    
    total_quests += len(quests)
    total_hazards += len(hazards)
    total_pois += len(pois)
    total_npcs += len(npcs)
    total_scenes += len(scenes)
    
    milestones = data.get('milestone_xp', []) or data.get('milestones', [])
    for m in milestones:
        total_xp += m.get('xp', 0)
    for q in quests:
        total_xp += q.get('reward_xp', 0)
        
    for h in hazards:
        lvl = h.get('level')
        hazard_levels.append(lvl)
        st_type = h.get('saving_throw_type')
        st_dc = h.get('saving_throw_dc')
        stealth_dc = h.get('stealth_dc')
        disable_skills = h.get('disable_skills', [])
        
        assert lvl is not None and 0 <= lvl <= 25, f"Hazard invalid level in {h}"
        assert stealth_dc is not None and stealth_dc > 0, f"Hazard stealth DC invalid in {h}"
        assert st_dc is not None and st_dc > 0, f"Hazard saving throw DC invalid in {h}"
        assert st_type in ['Reflexos', 'Fortitude', 'Vontade'], f"Invalid saving throw type: {st_type} in {h}"
        hazard_saving_throws.add(st_type)
        
        assert len(disable_skills) > 0, f"Hazard missing disable skills in {h}"
        for ds in disable_skills:
            prof = ds.get('proficiency')
            hazard_disable_proficiencies.add(prof)
            assert prof in ['treinado', 'especialista', 'mestre', 'lendario'], f"Invalid prof {prof} in {h}"
            assert ds.get('dc', 0) > 0, f"Disable skill DC <= 0 in {h}"
            assert len(ds.get('skill', '')) > 0, f"Empty disable skill name in {h}"

    print(f"Ch {idx:02d} ({chapter_key}): title=\"{title}\", quests={len(quests)}, hazards={len(hazards)}, pois={len(pois)}, npcs={len(npcs)}, scenes={len(scenes)}")

print("\n=== TOTAL METRICS ===")
print(f"Total Quests: {total_quests}")
print(f"Total Canonical Hazards: {total_hazards}")
print(f"Total Hex POIs: {total_pois}")
print(f"Total NPCs: {total_npcs}")
print(f"Total Tactical Scenes: {total_scenes}")
print(f"Total Accumulated XP: {total_xp}")
print(f"Hazard saving throw types: {sorted(list(hazard_saving_throws))}")
print(f"Hazard disable proficiencies: {sorted(list(hazard_disable_proficiencies))}")
print(f"Hazard levels range: min={min(hazard_levels)}, max={max(hazard_levels)}")
