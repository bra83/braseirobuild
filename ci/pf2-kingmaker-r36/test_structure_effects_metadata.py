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


def test_official_pdf_structure_effects_do_not_contain_page_bleed():
    """PDF p.545–552: preserve full clauses, without sidebars/headers/next section."""
    assert DATA["count"]==73
    by_name={r["official_name"]:r["effects_rule"] for r in DATA["structures"]}
    anchors={
        "CEMETERY":("Unrest gained from dangerous events.",
                    "maximum of 4 for four cemeteries"),
        "LUMBERYARD":("increases maximum Lumber Commodity capacity by 1.",
                      "facilitate the shipment of trees to the yard."),
        "ORPHANAGE":("The first time you build an orphanage each Kingdom turn",
                     "reduce Unrest by 1."),
        "TAVERN, LUXURY":("Kingdom turn, reduce Unrest by 1d4+1.",
                         "Gather Information in a settlement with at least one luxury tavern"),
        "WATERFRONT":("next to a Water Border.",
                      "this bonus stacks with similar bonuses in the settlement."),
    }
    for name,phrases in anchors.items():
        effects=by_name[name]
        assert all(phrase in effects for phrase in phrases),name
        assert effects.endswith("."),name
    for name,effects in by_name.items():
        for leakage in ("APPENDICES", "ADDITIONAL RESOURCES", "LORIS SHADWEST",
                        "A FLYING OWLBEAR", "KINGDOM EVENTS"):
            assert leakage not in effects,(name,leakage)
    # This is text provenance cleanup; implementation of the special effects
    # still requires separate semantic/Android verification.
    assert DATA["certification"]=="NOT_CERTIFIED"


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

def test_transform_materializes_all_73_traits_and_residential_occupancy():
    catalog=mod.official_manifest()
    official_18={"academy","alchemist","bank","barracks","brewery",
       "castle","cathedral","embassy","garrison","granary","library",
       "monument","pier","smithy","stables","temple","theater","town_hall"}
    cases="\n".join('if("'+key+'".equals(k)){return new JSONObject();}' for key in sorted(official_18))
    before=("public JSONObject structure(String key){\nString k=key(key);\n"
          +cases+"\n}\n    public JSONObject nextCatalog(){return null;}")
    transformed,_=mod.transform_rules(before,catalog)
    assert transformed.count('officialBase.put("residential_lots",')==73
    assert transformed.count('officialBase.put("official_traits",')==73
    assert transformed.count('officialBase.put("official_effects_rule",')==73
    for key,lots in {"houses":1,"inn":1,"garrison":2,"noble_villa":2,"marketplace":2}.items():
        idx=transformed.index('if("'+key+'".equals(r36OfficialKey))')
        assert 'officialBase.put("residential_lots",'+str(lots)+')' in transformed[idx:idx+250],key
    assert 'special_effects_semantic_certification' in transformed

def test_residential_lots_propagate_to_built_structure_effects():
    # Reproduces metadata overlay transformation with actual production Java.
    src=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRulesEngine.java"
    if not src.exists():
        return
    data=src.read_text(encoding="utf-8")
    assert "R36_OFFICIAL_CATALOG_73" in data
    catalog={mod.key_for(row["official_name"]):row for row in mod.official_manifest()}
    for key,rec in mod.official_semantics().items():
        fragment=data[data.index('if("'+key+'".equals(r36OfficialKey))'):][:850]
        residence=catalog[key]["lots"] if "RESIDENTIAL" in rec["traits"] else 0
        assert 'officialBase.put("residential_lots",'+str(residence)+')' in fragment,key
    for key in ("houses","inn","garrison","noble_villa","marketplace"):
        assert mod.official_semantics()[key]["traits"]

