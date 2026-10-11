"""Synthetic unit guards for official 11-chapter/40-part fidelity audit.

Auditor findings do NOT certify runtime gameplay. This test only verifies
that obvious mismatches cannot be silently marked complete.
"""
import json,importlib.util
from pathlib import Path

AUDIT=Path(__file__).with_name("audit_chapters_1_11.py")
spec=importlib.util.spec_from_file_location("chapters_audit",AUDIT)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def make_root(tmp_path):
    source=tmp_path/"android/app/src/main/assets/pathfinder/kingmaker/chapters"
    source.mkdir(parents=True)
    book=json.loads((AUDIT.with_name("KINGMAKER_CANONICAL_BOOK_CROSSWALK.json")).read_text(encoding="utf-8"))
    names=set()
    for chapter in book["official_adventure_chapters"]:
        names.add(chapter["candidate_assets"][0].removesuffix(".json"))
    names.update("chapter_%02d_fixture"%i for i in range(13))
    for name in names:
        (source/(name+".json")).write_text(json.dumps({
            "chapter_number":0,"quests":[],"npcs":[],"scenes":[],"pois":[],
            "events":[],"milestones":[],"hazards":[]}),encoding="utf-8")
    return source

def test_every_chapter_is_present_but_not_certified_by_empty_assets(tmp_path):
    folder=make_root(tmp_path)
    report=m.audit(tmp_path)
    assert report["numbered_official_chapters"]==11
    assert report["official_story_parts"]==40
    assert report["whole_campaign_certified"] is False
    assert len(report["chapters"])==11
    assert sum(len(c["parts"]) for c in report["chapters"])==40
    assert any(x["code"]=="CHAPTER_10_11_UNLOCKS_WITHOUT_NYRISSA_RESOLUTION"
               for x in report["findings"])
    assert any(x["code"]=="OFFICIAL_PLOT_ANCHOR_MISSING"
               for x in report["findings"])

def test_nyrissa_guard_and_noncanonical_candlemere_alert(tmp_path):
    folder=make_root(tmp_path)
    def populate(filename,quests):
        name=folder/filename
        assert name.is_file(),filename
        d=json.loads(name.read_text(encoding="utf-8"))
        d["quests"]=quests
        name.write_text(json.dumps(d),encoding="utf-8")
    populate("chapter_09_sound_thousand_screams.json",[
        {"quest_key":"q_redencao_espada_briar","prerequisites":[],"objectives":[{"id":"1"}]}
    ])
    populate("chapter_10_curse_lantern_king.json",[
        {"quest_key":"q_revelacao_rei_lanterna",
         "prerequisites":["q_redencao_espada_briar"],"objectives":[{"id":"1"}]}
    ])
    populate("chapter_08_they_lurk_below.json",[
        {"quest_key":"fake_neogi","title":"Devoradores de Mentes e Neogis",
         "prerequisites":[],"objectives":[{"id":"1"}]}
    ])
    report=m.audit(tmp_path)
    assert report["chapter_10_to_11_prerequisite_verified"] is True
    assert any(x["code"]=="NONCANONICAL_CHAPTER_9_QUEST" for x in report["findings"])
    assert not any(x["code"]=="CHAPTER_10_11_UNLOCKS_WITHOUT_NYRISSA_RESOLUTION"
                   for x in report["findings"])
