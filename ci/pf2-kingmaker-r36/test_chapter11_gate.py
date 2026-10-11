"""Chapter 11 cannot start before the Nyrissa resolution; source-specific synthetic contracts."""
import importlib.util
import copy
from pathlib import Path
import pytest

P=Path(__file__).with_name("patch_chapter11_gate.py")
spec=importlib.util.spec_from_file_location("chapter11",P)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def fixtures():
    a={"chapter_number":9,"quests":[{"quest_key":m.ENDING,"objectives":[{"id":"final"}]}]}
    b={"chapter_number":10,"quests":[{"quest_key":m.OPENING,"prerequisites":[]},
        {"quest_key":"q_unrelated_sidequest","prerequisites":[]}]}
    return a,b

def test_nyrissa_completion_is_required_to_unlock_final_chapter():
    a,b=fixtures();out=m.transform(a,b)
    assert out["quests"][0]["prerequisites"]==[m.ENDING]
    assert out["quests"][1]["prerequisites"]==[]
    assert "Kingmaker 2e pp. 480-484" in out["quests"][0]["book_progression_source"]

def test_idempotent_and_fail_closed():
    a,b=fixtures()
    first=m.transform(a,copy.deepcopy(b))
    assert m.transform(a,first)["quests"][0]["prerequisites"]==[m.ENDING]
    a["chapter_number"]=8
    with pytest.raises(RuntimeError):m.transform(a,b)
    a["chapter_number"]=9
    a["quests"][0]["objectives"]=[]
    with pytest.raises(RuntimeError):m.transform(a,b)

def test_wrong_android_version_never_mutates_assets(tmp_path):
    d=tmp_path/m.ASSETS;d.mkdir(parents=True)
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36",encoding="utf-8")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
