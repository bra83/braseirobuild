"""Fail-closed contracts for official Kingmaker 2e structure metadata patches.
Synthetic Java only; host/Android runtime is validated by separate jobs.
"""
from pathlib import Path
import importlib.util, json
import pytest

P=Path(__file__).with_name("patch_structure_catalog_values.py")
spec=importlib.util.spec_from_file_location("official_structures",P)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
manifest=json.loads(m.MANIFEST.read_text(encoding="utf-8"))

def fixture():
    branches=[]
    for i,(k,name) in enumerate(m.UNAMBIGUOUS.items()):
        values=''.join('.put("'+field+'",1)' for field in m.FIELDS)
        branches.append(('if' if i==0 else 'else if')+'("'+k+'".equals(k)){\n'
                        +'s.put("name","fixture")'+values+'.put("residential_lots",0);\n}')
    branches.append('else if("sentinel".equals(k)){s.put("name","sentinel");}')
    return 'public JSONObject structure(String key){\n'+'\n'.join(branches)+'\n}\n    public JSONObject nextMethod() {return new JSONObject();}\n'

def test_all_16_single_skill_entries_use_pdf_numbers():
    transformed=m.transform(fixture(),manifest)
    for key,name in m.UNAMBIGUOUS.items():
        rec=next(x for x in manifest["structures"] if x["official_name"]==name)
        start=transformed.index('if("'+key+'".equals(k)){')
        stop=transformed.index('}',start)
        fragment=transformed[start:stop]
        assert '.put("level",'+str(rec["official_level"])+')' in fragment
        for field in ("rp","lumber","stone","ore","luxuries"):
            assert '.put("'+field+'",'+str(rec["costs"][field])+')' in fragment
        assert '.put("construction_dc",'+str(rec["construction_dc"])+')' in fragment
        assert '.put("construction_skill",' in fragment
        assert '.put("construction_rank",' in fragment

def test_official_source_identity_and_alternative_are_fail_closed():
    sample=fixture()
    corrupted=dict(manifest,source_drive_file_id="not-official")
    with pytest.raises(RuntimeError):m.transform(sample,corrupted)
    with pytest.raises(RuntimeError):m.transform(m.transform(sample,manifest),manifest)
    with pytest.raises(RuntimeError):m.transform(sample.replace('if("academy".equals(k))','if("academy-renamed".equals(k))'),manifest)

def test_wrong_version_rejected_without_writing(tmp_path,monkeypatch):
    base=tmp_path/m.REL;base.parent.mkdir(parents=True)
    sample=fixture()
    base.write_text(sample,encoding="utf-8")
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36",encoding="utf-8")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
    assert base.read_text(encoding="utf-8")==sample
