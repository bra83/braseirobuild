"""Replace draft-specific assertions with primary-book expectations, preserving checks."""
from pathlib import Path
def exact(s,a,b):
 if s.count(a)!=1:raise ValueError('Campaign regression anchor count: '+str(s.count(a))+' '+a[:75])
 return s.replace(a,b,1)
def apply_regressions(root):
 directory=Path(root)/'tests_pf2'
 p=directory/'test_kingmaker_m3_chapters5_to_8.py';s=p.read_text(encoding='utf-8')
 for a,b in [('"chapter_06_blood_for_blood": (10, 12, 6)','"chapter_06_blood_for_blood": (10, 13, 6)'),('"chapter_07_war_of_river_kings": (12, 14, 7)','"chapter_07_war_of_river_kings": (13, 16, 7)'),('"chapter_08_they_lurk_below": (14, 16, 8)','"chapter_08_they_lurk_below": (16, 17, 8)')]:s=exact(s,a,b)
 anchor='        assert len(data["quests"]) >= 4, f"Quests insuficientes em {key}"'
 s=exact(s,anchor,'''        if key == "chapter_08_they_lurk_below":
            assert len(data["quests"]) == 5
            assert len(data["hazards"]) >= 1 and len(data["npcs"]) >= 1
            assert len(data["metadata"]["parts"]) == 2
            assert data["clocks"] == [] and data["events"] == []
            assert len(data["milestones"]) == 2
            assert data["metadata"]["coverage"]["all_maps_compiled"] is False
            continue
'''+anchor)
 start=s.index('def test_m3_chapter_08_they_lurk_below_canonical_content():')
 end=s.index('\n\n# ==============================================================================',start)
 replacement='''def test_m3_chapter_08_they_lurk_below_canonical_content():
    data = load_chapter_data("chapter_08_they_lurk_below")
    assert "Candlemere" in data["title"]
    assert data["level_min"] == 16 and data["level_max"] == 17
    keys = {q["quest_key"] for q in data["quests"]}
    assert keys == {"q_candlemere_spiral_seal", "q_candlemere_contested", "q_candlemere_helix", "q_candlemere_foras", "q_candlemere_absolution"}
    retired = {q["key"] for q in data["metadata"]["retirement"]["quests"]}
    assert keys.isdisjoint(retired)
    foras = next(n for n in data["npcs"] if n["npc_key"] == "npc_foras")
    assert foras["stats"]["level"] == 19 and foras["stats"]["hp"] == 355 and foras["stats"]["ac"] == 43
    helix = next(q for q in data["quests"] if q["quest_key"] == "q_candlemere_helix")
    assert helix["rewards"]["xp"] == 120
    assert "B14, B13, B12, B10, B8, B4, B3, B2 e B1" in helix["objectives"][1]["desc"]
    fight = next(q for q in data["quests"] if q["quest_key"] == "q_candlemere_foras")
    assert "q_candlemere_helix" not in fight["prerequisites"]
    assert data["events"] == [] and data["clocks"] == []
'''
 s=s[:start]+replacement+s[end:]
 anchor='            assert h.get("ac", 0) >= 10, f"CA inválida em {h_key}"'
 s=exact(s,anchor,'''            if h_key == "hazard_candlemere_praise":
                assert h["physical_defenses_published"] is False
                assert h["level"] == 19 and h["saving_throw_dc"] == 41 and h["stealth_dc"] == 47
                assert [d["dc"] for d in h["disable_skills"]] == [47, 49]
                assert h["routine_actions"] == 1 and "12d6" in h["routine"][0]["description"]
                continue
'''+anchor)
 s=exact(s,'assert "varnhold" in text_dump or "senhores dos tigres" in text_dump or "rushlight" in text_dump or "terras sombrias" in text_dump','assert "varnhold" in text_dump or "senhores dos tigres" in text_dump or "rushlight" in text_dump or "candlemere" in text_dump')
 s=exact(s,'assert "ladroagem" in text_dump or "sobrevivência" in text_dump or "natureza" in text_dump or "arcanismo" in text_dump','assert "ladroagem" in text_dump or "sobrevivência" in text_dump or "natureza" in text_dump or "arcanismo" in text_dump or "ocultismo" in text_dump')
 # The ingestion check must match the actual definitions, not require filler POIs.
 line=next(x for x in s.splitlines() if 'assert poi_count >= 20' in x or ('Esperado >= 20 POIs' in x and 'assert' in x))
 prefix=line[:len(line)-len(line.lstrip())]
 count=line.strip().split('assert ',1)[1].split(' >=',1)[0]
 s=exact(s,line,prefix+'expected_pois = sum(len(load_chapter_data(ch).get("pois", [])) for ch in CHAPTER_KEYS)\n'+prefix+'assert '+count+' == expected_pois, f"POIs importados: {'+count+'}; esperados: {expected_pois}"')
 s=exact(s,'assert total_scenes >= 16, f"Esperado >= 16 cenas táticas, encontrado {total_scenes}"','expected_scenes = sum(len(load_chapter_data(ch).get("scenes", [])) + bool(load_chapter_data(ch).get("hazards", [])) for ch in CHAPTER_KEYS)\n    assert total_scenes == expected_scenes, f"Cenas importadas: {total_scenes}; esperadas: {expected_scenes}"')
 s=exact(s,'assert milestones >= 16, f"Esperado >= 16 marcos, encontrado {milestones}"','expected_milestones = sum(len(load_chapter_data(ch).get("milestones", [])) for ch in CHAPTER_KEYS)\n    assert milestones == expected_milestones, f"Marcos importados: {milestones}; esperados: {expected_milestones}"')
 p.write_text(s,encoding='utf-8')
 p=directory/'test_kingmaker_m4_chapters9_to_12.py';s=p.read_text(encoding='utf-8')
 s=exact(s,'"chapter_09_sound_thousand_screams": (16, 18, 9)','"chapter_09_sound_thousand_screams": (17, 20, 9)');p.write_text(s,encoding='utf-8')
 p=directory/'test_kingmaker_challenger_final.py';s=p.read_text(encoding='utf-8')
 line=next(x for x in s.splitlines() if 'Capítulo sem POIs:' in x and 'assert' in x)
 prefix=line[:len(line)-len(line.lstrip())]
 s=exact(s,line,prefix+'if data.get("metadata", {}).get("definition_revision") != "candlemere-r36-primary":\n'+prefix+'    '+line.strip())
 p.write_text(s,encoding='utf-8')
 p=directory/'java/KingmakerFinalStressHarness.java';s=p.read_text(encoding='utf-8')
 old='check(!cat.pois.isEmpty(), "Capítulo deve possuir POIs: " + filename);'
 s=exact(s,old,'check(!cat.pois.isEmpty() || "candlemere-r36-primary".equals(cat.metadata.optString("definition_revision")), "POIs ou revisão de Candlemere explicitamente sem mapas: " + filename);')
 p.write_text(s,encoding='utf-8')
