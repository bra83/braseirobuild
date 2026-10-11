#!/usr/bin/env python3
"""Read-only crosswalk: verified R35 KingmakerRulesEngine.structure versus official Kingmaker 2e structures.
This is an audit, NOT full rules certification. No source files are modified.
"""
from pathlib import Path
import argparse, json, re, os

MAP = {
    "academy": "ACADEMY",
    "alchemist": "ALCHEMY LABORATORY",
    "bank": "BANK",
    "barracks": "BARRACKS",
    "brewery": "BREWERY",
    "castle": "CASTLE",
    "cathedral": "CATHEDRAL",
    "embassy": "EMBASSY",
    "garrison": "GARRISON",
    "granary": "GRANARY",
    "library": "LIBRARY",
    "monument": "MONUMENT",
    "pier": "PIER",
    "smithy": "SMITHY",
    "stables": "STABLE",
    "temple": "TEMPLE",
    "theater": "THEATER",
    "town_hall": "TOWN HALL",
}
COSTS = ("rp", "lumber", "stone", "ore", "luxuries")

def audit(source, manifest):
    official={row["official_name"]:row for row in manifest["structures"]}
    report=[]
    for java_key,name in MAP.items():
        expected=official.get(name)
        if not expected:
            report.append({"key":java_key,"official_name":name,"status":"official_manifest_entry_missing"})
            continue
        anchor='if("'+java_key+'".equals(k))'
        start=source.find(anchor)
        if start<0:
            report.append({"key":java_key,"official_name":name,"status":"runtime_catalog_key_missing"})
            continue
        section=source[start:start+1600]
        next_entry=re.search(r"}\s*else\s+if\s*\(",section)
        if next_entry: section=section[:next_entry.start()]
        found=dict((k,int(v)) for k,v in re.findall(r'\.put\("(level|lots|rp|lumber|stone|ore|luxuries)",\s*(\d+)\)',section))
        expected_values={"level":expected["official_level"],"lots":expected["lots"],**expected["costs"]}
        comparisons=[]
        for field,value in expected_values.items():
            if value is None:
                continue
            observed=found.get(field)
            if observed!=value:
                comparisons.append({"field":field,"official":value,"app":observed})
        # The legacy Java catalogue lacks official Construction skill/DC metadata.
        skills_present=bool(re.search(r'\.put\("construction_(?:skill|dc)"',section))
        if not skills_present:
            comparisons.append({"field":"construction_skill_and_dc","official":expected["construction"]+" DC "+str(expected["construction_dc"]),"app":None})
        report.append({"key":java_key,"official_name":name,"status":"mismatch" if comparisons else "numeric_match","differences":comparisons})
    return {
       "scope":"read_only_java_catalog_vs_official_pdf",
       "source":"Kingmaker.pdf, Appendix 2, pp. 544-552",
       "canonical_pdf_id":manifest["source_drive_file_id"],
       "official_structures_extracted":len(official),
       "runtime_keys_compared":len(report),
       "mismatched_keys":sum(x["status"]=="mismatch" for x in report),
       "semantic_certification":"OPEN",
       "limitations":["PDF extraction requires manual QA","Comparison uses mapped Java catalogue keys only","This does not validate combat, chapter flow, SQLite state or Android UX"],
       "results":report,
    }

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--project",required=True)
    ap.add_argument("--out",required=True)
    ap.add_argument("--manifest",default=str(Path(__file__).with_name("kingmaker_official_structures_p544_552.json")))
    args=ap.parse_args()
    root=Path(args.project)
    matches=list(root.rglob("KingmakerRulesEngine.java"))
    if len(matches)!=1:
        raise SystemExit("Expected exactly one KingmakerRulesEngine.java, got "+str(len(matches)))
    data=json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    if len(data["structures"])!=73 or data.get("certification")!="NOT_CERTIFIED":
        raise SystemExit("Unverified official manifest identity")
    result=audit(matches[0].read_text(encoding="utf-8"),data)
    path=Path(args.out)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    summary=("# Kingmaker official structure audit\n"
             "Official entries extracted: "+str(result["official_structures_extracted"])+"\n\n"
             "Runtime mapped keys inspected: "+str(result["runtime_keys_compared"])+"\n\n"
             "Keys with differences: "+str(result["mismatched_keys"])+"\n\n"
             "Semantic certification: OPEN. This is a structural comparison only.\n")
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"],"a",encoding="utf-8") as f: f.write(summary)

if __name__=="__main__":
    main()
