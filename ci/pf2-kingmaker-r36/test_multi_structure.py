"""Synthetic crosswalk contracts for official four-skill Castle and Town Hall construction."""
from pathlib import Path
import importlib.util,json,pytest
P=Path(__file__).with_name("patch_multi_structure.py")
spec=importlib.util.spec_from_file_location("multi",P)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
book=json.loads(m.MANIFEST.read_text(encoding="utf-8"))

def sample():
    pieces=[]
    for i,k in enumerate(m.MULTI):
        prefix="if" if i==0 else "else if"
        values="".join('.put("'+key+'",1)' for key in m.FIELDS)
        pieces.append(prefix+'("'+k+'".equals(k)){s.put("name","fixture")'+values+'.put("residential_lots",0);}')
    pieces.append('else if("sentinel".equals(k)){return null;}')
    return "public JSONObject structure(String key){"+ "".join(pieces)+"}\n    public JSONObject nextMethod(){return null;}"

def test_official_four_allowed_skills_and_per_structure_dc():
    src=m.transform(sample(),book)
    records={x["official_name"]:x for x in book["structures"]}
    for key,name in m.MULTI.items():
        rec=records[name]
        part=src[src.index('if("'+key+'".equals(k)){'):]
        assert '.put("construction_skills","defense,industry,magic,statecraft")' in part
        assert '.put("construction_dc",'+str(rec["construction_dc"])+')' in part
        expected_rank="expert" if key=="castle" else "trained"
        assert '.put("construction_rank","'+expected_rank+'")' in part
        for field,value in {"level":rec["official_level"],"lots":rec["lots"],**rec["costs"]}.items():
            assert '.put("'+field+'",'+str(value)+')' in part

def test_multi_skill_transform_rejects_drift_and_wrong_book():
    with pytest.raises(RuntimeError):m.transform(sample().replace('"castle".equals(k)','"castle_v2".equals(k)'),book)
    with pytest.raises(RuntimeError):m.transform(m.transform(sample(),book),book)
    with pytest.raises(RuntimeError):m.transform(sample(),dict(book,source_drive_file_id="fake"))
