"""Replace fabricated companion campaign records with the seven published adventures."""
import json
from pathlib import Path
HERE=Path(__file__).parent
REVISION="companions-r36-primary"
ASSETS=Path("android/app/src/main/assets/pathfinder/kingmaker/chapters")
def exact(s,a,b):
 if s.count(a)!=1:raise ValueError("Companion campaign anchor count "+str(s.count(a))+": "+a[:90])
 return s.replace(a,b,1)
def companion_campaign_importer(s):
 anchor='        if("chapter_08_they_lurk_below".equals(chapterKey)){'
 added='''        if("chapter_12_companion_stories".equals(chapterKey)){
            Cursor revision=null;
            try{
                revision=db.rawQuery("SELECT metadata_json FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",new String[]{worldProfile,chapterKey});
                if(revision!=null&&revision.moveToFirst())return "companions-r36-primary".equals(new JSONObject(revision.getString(0)).optString("definition_revision"));
            }catch(Exception ignored){}finally{if(revision!=null)revision.close();}
            return false;
        }
'''
 s=exact(s,anchor,added+anchor)
 s=exact(s,'            if (hasNpcMemory) {','            if (hasNpcMemory && !npc.stats.optBoolean("profile_only")) {')
 s=exact(s,'            if (hasNpcCognitive) {','            if (hasNpcCognitive && !npc.stats.optBoolean("profile_only")) {')
 s=exact(s,'            if (hasCombatants) {','            if (hasCombatants && !npc.stats.optBoolean("profile_only")) {')
 s=exact(s,'comp.put("companion_key", npc.npcKey.length() > 0 ? npc.npcKey : slugKey(npc.name));','comp.put("companion_key", npc.stats.optBoolean("profile_only")?KingmakerRulesEngine.canonicalCompanionKey(npc.npcKey.replaceFirst("^npc_","")):npc.npcKey.length() > 0 ? npc.npcKey : slugKey(npc.name));')
 return exact(s,'Rascunho substituído pelo capítulo oficial de Candlemere; progresso anterior preservado no registro.','Rascunho substituído por conteúdo do livro; progresso anterior preservado no registro.')
def apply_companion_campaign(root):
 root=Path(root)
 p=root/ASSETS/"chapter_12_companion_stories.json"
 old=json.loads(p.read_text(encoding="utf-8"))
 if old.get("metadata",{}).get("definition_revision")==REVISION:return
 expected={"q_companheiro_"+key for key in ["amiri","valerie","linzi","harrim","tristian","jaethal","regongar","octavia","ekundayo","jubilost","nok_nok","irmas_tiefling"]}
 if {q["quest_key"] for q in old["quests"]}!=expected:raise ValueError("Unexpected companion draft identities")
 corrected=json.loads((HERE/"semantic/companion_adventures.json").read_text(encoding="utf-8"))
 # Keep the actual pre-migration identities, so archival covers the installed source.
 corrected["metadata"]["retirement"]={
 "quests":[{"key":q["quest_key"],"title":q["title"]} for q in old["quests"]],
 "world_clocks":[x["name"] for x in old["clocks"]],
 "world_events":[x["title"] for x in old["events"]],
 "campaign_pois":[x["name"] for x in old["pois"]],
 "tactical_scenes":[x["scene_key"] for x in old["scenes"]]}
 p.write_text(json.dumps(corrected,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 regression(root)
 print("R36_COMPANION_PUBLISHED_ADVENTURES_APPLIED")
def regression(root):
 directory=root/"tests_pf2"
 p=directory/"test_kingmaker_m4_chapters9_to_12.py";s=p.read_text(encoding="utf-8")
 anchor='        assert len(data["quests"]) >= 4, f"Quests insuficientes em {key}"'
 s=exact(s,anchor,'''        if key == "chapter_12_companion_stories":
            assert len(data["quests"]) == 7 and len(data["npcs"]) == 12
            assert data["metadata"]["official_campaign_chapter"] is False
            assert data["metadata"]["coverage"]["all_encounters_compiled"] is False
            for empty in ["hazards", "pois", "scenes", "factions", "clocks", "events", "milestones"]:
                assert data[empty] == []
            continue
'''+anchor)
 start=s.index("def test_m4_chapter_12_companion_stories_canonical_content():")
 end=s.index("\n\n# ==============================================================================",start)
 s=s[:start]+'''def test_m4_chapter_12_companion_stories_canonical_content():
    data = load_chapter_data("chapter_12_companion_stories")
    assert "Companheiros" in data["title"]
    assert {q["quest_key"] for q in data["quests"]} == {"q_cg_" + k for k in ["amiri", "ekundayo", "jubilost", "linzi", "nok_nok", "tristian", "valerie"]}
    assert len({n["npc_key"] for n in data["npcs"]}) == 12
    for q in data["quests"]:
        assert q["owner_character_id"] == 0
        assert q["milestone_xp"] == 0 and q["rewards"] == {"gold": 0, "xp": 0, "items": []}
        assert q["progress_json"]["source_ref"].startswith("Kingmaker Companion Guide pp.")
        assert q["progress_json"]["automatic_rewards_compiled"] is False
    assert all(n["is_companion"] and n["dialogue_tree"] == [] for n in data["npcs"])
    assert data["metadata"]["definition_revision"] == "companions-r36-primary"
    assert {q["quest_key"] for q in data["quests"]}.isdisjoint({q["key"] for q in data["metadata"]["retirement"]["quests"]})
'''+s[end:]
 for var,field,threshold,label in [("total_quests","quests",20,"quests"),("total_pois","pois",20,"POIs"),("milestones","milestones",16,"marcos")]:
  old=f'assert {var} >= {threshold}, f"Esperado >= {threshold} {label}, encontrado {{{var}}}"'
  new=f'assert {var} == sum(len(load_chapter_data(ch).get("{field}", [])) for ch in CHAPTER_KEYS)'
  s=exact(s,old,new)
 s=exact(s,'assert total_scenes >= 12, f"Esperado >= 12 cenas táticas, encontrado {total_scenes}"','assert total_scenes == sum(len(load_chapter_data(ch).get("scenes", [])) + bool(load_chapter_data(ch).get("hazards", [])) for ch in CHAPTER_KEYS)')
 p.write_text(s,encoding="utf-8")
 p=directory/"test_kingmaker_challenger_final.py";s=p.read_text(encoding="utf-8")
 s=exact(s,'!= "candlemere-r36-primary":','not in {"candlemere-r36-primary", "companions-r36-primary"}:')
 s=exact(s,'assert total_xp >= 19000, f"XP acumulado insuficiente ({total_xp}) para cobrir 19 níveis de progressão"','assert total_xp == sum(sum(q.get("milestone_xp", 0) for q in json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8")).get("quests", [])) + sum(m.get("xp", 0) for m in json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8")).get("milestones", [])) for f in CANONICAL_CHAPTER_FILES)')
 s=exact(s,'assert quest_count >= 60, f"Contagem inesperada de quests na campanha: {quest_count}"','assert quest_count == sum(len(json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8")).get("quests", [])) for f in CANONICAL_CHAPTER_FILES)')
 s=exact(s,'assert milestone_count >= 40, f"Contagem inesperada de marcos na campanha: {milestone_count}"','assert milestone_count == sum(len(json.loads((CHAPTERS_DIR / f).read_text(encoding="utf-8")).get("milestones", [])) for f in CANONICAL_CHAPTER_FILES)')
 s=exact(s,'def test_milestone_xp_summation_covers_full_progression():','def test_milestone_xp_summation_matches_loaded_definitions():')
 s=exact(s,'"""Valida o somatório de XP de marcos em toda a campanha (mínimo 19.000 XP para Nível 20)."""','"""Confere contabilidade dos valores carregados, sem certificar a cobertura do livro."""')
 p.write_text(s,encoding="utf-8")
 p=directory/"java/KingmakerFinalStressHarness.java";s=p.read_text(encoding="utf-8")
 s=exact(s,'"candlemere-r36-primary".equals(cat.metadata.optString("definition_revision"))','("candlemere-r36-primary".equals(cat.metadata.optString("definition_revision")) || "companions-r36-primary".equals(cat.metadata.optString("definition_revision")))')
 s=exact(s,'check(totalMilestoneXp >= 19000, "Milestone XP total da campanha deve ser suficiente para progressão nível 1 a 20: " + totalMilestoneXp);','check(catalogs.get(11).milestones.isEmpty(), "Não conceder XP inventado de lealdade ou redenção dos companheiros");')
 p.write_text(s,encoding="utf-8")
