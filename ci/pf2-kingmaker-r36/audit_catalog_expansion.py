#!/usr/bin/env python3
"""Fail-closed structural numeric crosswalk for all 73 Kingmaker structures.

Inspects extracted production KingmakerRulesEngine.java after all R36 patches.
Numeric match is distinct from special effects, spatial rules and Android proof.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location("catalog_generator",HERE/"patch_catalog_expansion.py")
gen=importlib.util.module_from_spec(spec);spec.loader.exec_module(gen)

def audit(source):
    rows=gen.official_manifest()
    legacy=source.split("R36_OFFICIAL_CATALOG_73;",1)[-1]
    results=[]
    for r in rows:
        key=gen.key_for(r["official_name"])
        # 55 new types return an expression, while 18 old types retain their
        # original branch and are separately audited by audit_structure_catalog.
        # Metadata-only blocks do not return a numeric catalog definition.
        # Parse each brace-delimited block independently, never scan through
        # intervening structures looking for a later "return".
        blocks=re.findall(r'if\\("'+re.escape(key)+r'"\\.equals\\(r36OfficialKey\\)\\)\\s*\\{([^{}]*)\\}',source,re.DOTALL)
        numeric=[re.search(r'\\breturn\\s+(.*?);',block,re.DOTALL) for block in blocks]
        numeric=[m for m in numeric if m]
        if not numeric:
            legacy_anchor='if("'+key+'".equals(k))'
            results.append({"name":r["official_name"],"key":key,
                            "status":"existing_branch" if legacy_anchor in legacy else "MISSING"})
            continue
        if len(numeric)!=1:raise ValueError("Duplicate numeric overlay: "+key)
        expr=numeric[0].group(1)
        values={"level":r["official_level"],"lots":r["lots"] or 0,**r["costs"],
                "construction_dc":r["construction_dc"]}
        differences=[]
        for field,val in values.items():
            token='.put("'+field+'",'+str(val)+')'
            if token not in expr:
                differences.append(field)
        rank=gen.skill_data(r["construction"])[1]
        if '.put("construction_rank","'+rank+'")' not in expr:
            differences.append("construction_rank")
        if " or " in r["cost_text"] and '.put("requires_cost_choice",true)' not in expr:
            differences.append("cost_choice_guard")
        if r["lots"] is None and '.put("requires_special_placement",true)' not in expr:
            differences.append("special_placement_guard")
        results.append({"name":r["official_name"],"key":key,
                        "status":"matched_numeric_and_skill_metadata" if not differences else "MISMATCH",
                        "differences":differences})
    bad=[r for r in results if r["status"] in ("MISSING","MISMATCH")]
    return {"official_entries":len(rows),
            "present_existing":sum(r["status"]=="existing_branch" for r in results),
            "generated_numeric_match":sum(r["status"]=="matched_numeric_and_skill_metadata" for r in results),
            "mismatch_count":len(bad),"issues":bad,
            "semantic_effects_certified":False,
            "android_persistence_certified":False}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--project",required=True)
    p.add_argument("--out",required=True)
    p.add_argument("--require-73",action="store_true")
    a=p.parse_args()
    code=(Path(a.project)/gen.RULES).read_text(encoding="utf-8")
    result=audit(code)
    target=Path(a.out);target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("R36_OFFICIAL_CATALOG_73_CROSSWALK="+json.dumps(result,ensure_ascii=False),flush=True)
    if a.require_73 and (result["mismatch_count"] or result["official_entries"]!=73
         or result["present_existing"]+result["generated_numeric_match"]!=73):
        raise SystemExit("FAIL_CLOSED: 73 official structure numeric/skill definitions not present")
if __name__=="__main__":
    main()
