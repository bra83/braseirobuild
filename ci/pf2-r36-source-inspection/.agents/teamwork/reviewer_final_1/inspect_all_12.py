import json
import glob
import os

chapters_dir = os.path.abspath(r'android/app/src/main/assets/pathfinder/kingmaker/chapters')
chapter_files = sorted(glob.glob(os.path.join(chapters_dir, 'chapter_*.json')))

for cpath in chapter_files:
    fname = os.path.basename(cpath)
    with open(cpath, 'r', encoding='utf-8') as f:
        data = json.load(f)
    print(f"==================================================")
    print(f"FILE: {fname}")
    print(f"Chapter: {data.get('chapter_number')} | Key: {data.get('chapter_key')}")
    print(f"Title: {data.get('title')}")
    print(f"Level Range: {data.get('level_min')} - {data.get('level_max')}")
    print(f"Summary: {data.get('summary')[:100]}...")
    
    quests = data.get('quests', [])
    print(f"Quests ({len(quests)}):")
    for q in quests:
        print(f"  - [{q.get('quest_key')}] {q.get('title')} (XP: {q.get('milestone_xp')}, Obj: {len(q.get('objectives', []))})")
    
    hazards = data.get('hazards', [])
    print(f"Hazards ({len(hazards)}):")
    for h in hazards:
        disables = ", ".join(f"{d.get('skill')} CD {d.get('dc')} ({d.get('proficiency')})" for d in h.get('disable_skills', []))
        print(f"  - [{h.get('hazard_key')}] {h.get('name')} (Lvl {h.get('level')}, Furt {h.get('stealth_dc')}, Salv CD {h.get('saving_throw_dc')} {h.get('saving_throw_type')}, Desat: {disables})")
    
    pois = data.get('pois', [])
    print(f"POIs ({len(pois)}):")
    for p in pois[:4]:
        print(f"  - [{p.get('poi_key')}] {p.get('name')} @ {p.get('hex_id')} (tipo: {p.get('type')}, status: {p.get('status')})")
    if len(pois) > 4:
        print(f"  ... and {len(pois)-4} more POIs")
        
    npcs = data.get('npcs', [])
    print(f"NPCs ({len(npcs)}):")
    for n in npcs[:4]:
        print(f"  - [{n.get('npc_key')}] {n.get('name')} (atitude: {n.get('attitude')}, facção: {n.get('faction')})")
    if len(npcs) > 4:
        print(f"  ... and {len(npcs)-4} more NPCs")
        
    scenes = data.get('scenes', [])
    print(f"Scenes ({len(scenes)}):")
    for s in scenes:
        print(f"  - [{s.get('scene_key')}] {s.get('title')} ({s.get('width_cells')}x{s.get('height_cells')}, perigos: {len(s.get('hazards', []))})")
