"""73 official PDF structure traits must be incorporated in REAL R36 Java source.
This tests metadata/residential housing plumbing, NOT effects semantic fulfillment.
"""
import importlib.util
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location("r36_effects_catalog",HERE/"patch_catalog_expansion.py")
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
DATA=json.loads((ROOT/"pf2-kingmaker-certification/kingmaker_official_structure_effects_p544_552.json").read_text(encoding="utf-8"))

def test_pdf_structure_effects_crosswalk_all_73():
    assert DATA["certification"]=="NOT_CERTIFIED"
    rows=mod.official_semantics()
    assert len(rows)==73
    assert sum("INFRASTRUCTURE" in x["traits"] for x in rows.values())==6
    assert sum("RESIDENTIAL" in x["traits"] for x in rows.values())==9
    for key,rec in rows.items():
        assert rec["official_level"]>=0
        assert rec["traits"],key
        assert rec["semantic_certification"]=="PENDING",key

def test_residential_lots_propagate_to_built_structure_effects():
    # Reproduces metadata overlay transformation with actual production Java.
    src=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java"
    if not src.exists():
        return
    data=src.read_text(encoding="utf-8")
    assert "R36_OFFICIAL_CATALOG_73" in data
    for key,rec in mod.official_semantics().items():
        fragment=data[data.index('if("'+key+'".equals(r36OfficialKey))'):][:850]
        residence=1 if "RESIDENTIAL" in rec["traits"] else 0
        assert 'officialBase.put("residential_lots",'+str(residence)+')' in fragment,key
    for key in ("houses","inn","garrison","noble_villa","marketplace"):
        assert mod.official_semantics()[key]["traits"]
