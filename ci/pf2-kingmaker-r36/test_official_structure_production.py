"""Production source contracts: actual patched KingmakerRulesEngine/Repository.
NOT real Android/SQLite execution; those require separate runtime checks.
"""
import json, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java").read_text(encoding="utf-8")
REPO=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")
BOOK=json.loads((Path(__file__).parent/"kingmaker_official_structures_p544_552.json").read_text(encoding="utf-8"))
MAPPING={
    "academy":"ACADEMY","alchemist":"ALCHEMY LABORATORY","bank":"BANK",
    "barracks":"BARRACKS","brewery":"BREWERY","cathedral":"CATHEDRAL",
    "embassy":"EMBASSY","garrison":"GARRISON","granary":"GRANARY",
    "library":"LIBRARY","monument":"MONUMENT","pier":"PIER",
    "smithy":"SMITHY","stables":"STABLE","temple":"TEMPLE","theater":"THEATER",
}

def test_official_16_catalog_values_are_in_shipped_rules_engine():
    official={x["official_name"]:x for x in BOOK["structures"]}
    assert len(official)==73
    for key,name in MAPPING.items():
        rec=official[name]
        marker='if("'+key+'".equals(k)){'
        assert JAVA.count(marker)>=1,key
        start=JAVA.index(marker,JAVA.index('public JSONObject structure(String key)'))
        tail=JAVA[start:start+1500]
        b=re.search(r'}\s*else\s+if\s*\(',tail)
        assert b is not None,key
        part=tail[:b.start()]
        expected={"level":rec["official_level"],"lots":rec["lots"],**rec["costs"]}
        for field,value in expected.items():
            assert '.put("'+field+'",'+str(value)+')' in part,(key,field,value)
        assert '.put("construction_dc",'+str(rec["construction_dc"])+')' in part,key
        assert '.put("construction_skill",' in part
        assert '.put("construction_rank",' in part

def test_build_and_repair_pass_structure_specific_dc_to_recordcheck():
    a=REPO.index("public JSONObject resolveBuildStructure(")
    b=REPO.index("public JSONObject resolveDemolish(",a)
    build=REPO[a:b]
    assert 'naturalRoll,def.optInt("construction_dc",0)' in build
    a=REPO.index('public JSONObject resolveRepairStructure(String settlementKey,String structureKey,int naturalRoll')
    b=REPO.index("public JSONObject resolveUpgradeSettlement(",a)
    repair=REPO[a:b]
    assert 'naturalRoll,def.optInt("construction_dc",0)' in repair

def test_manifest_stays_explicitly_uncertified_and_references_correct_book():
    assert BOOK["source_drive_file_id"]=="1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3"
    assert BOOK["certification"]=="NOT_CERTIFIED"
