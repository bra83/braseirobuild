#!/usr/bin/env python3
"""Independent structural crosswalk against REAL R35 chapter JSON and warfare Java.
Official chapter and companion names come from King's official PDF contents.
This does not claim page-level, Android, or in-session semantic certification.
"""
import argparse
import json
import re
import hashlib
from pathlib import Path

ap=argparse.ArgumentParser()
ap.add_argument("--project",type=Path,required=True)
ap.add_argument("--out",type=Path,required=True)
args=ap.parse_args()
out=args.out.resolve();out.mkdir(parents=True,exist_ok=True)
source=args.project.resolve()
config=json.loads((Path(__file__).parent/"KINGMAKER_CANONICAL_BOOK_CROSSWALK.json").read_text(encoding="utf-8"))
chdir=source/"android/app/src/main/assets/pathfinder/kingmaker/chapters"
issues=[];report=[]
assets={}
for p in sorted(chdir.glob("chapter_*.json")):
    try: assets[p.name]=json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:issues.append({"severity":"critical","code":"INVALID_CHAPTER_JSON","asset":p.name,"reason":str(e)})
if not assets:issues.append({"severity":"critical","code":"MISSING_CHAPTER_ASSETS"})

def locate(candidate):
    stem=candidate[:-5] if candidate.endswith(".json") else candidate
    return [(n,v) for n,v in assets.items() if n==stem+".json" or n.startswith(stem+"_") or n==candidate]

for chapter in config["official_adventure_chapters"]:
    found={}
    for label in chapter["candidate_assets"]:
        for filename,doc in locate(label):
            found[filename]=doc
    accum={field:sum(len(d.get(field,[])) if isinstance(d.get(field),list) else 0 for d in found.values())
           for field in ("quests","hazards","pois","npcs","scenes","events","milestones")}
    row={"official_chapter":chapter["number"],"official_name":chapter["name"],
         "official_page":chapter["page"],"assets":sorted(found),
         "record_counts":accum,
         "real_android_playthrough":False,"semantic_page_comparison_certified":False,
         "source":"Kingmaker.pdf, official contents"}
    if not found:
        issues.append({"severity":"critical","code":"CHAPTER_WITHOUT_ANY_CANDIDATE_ASSET",
                       "chapter":chapter["number"],"title":chapter["name"]})
    if not (accum["quests"]+accum["scenes"]+accum["pois"]):
        issues.append({"severity":"critical","code":"CHAPTER_WITHOUT_INTERACTIVE_RECORDS",
                       "chapter":chapter["number"],"assets":sorted(found)})
    report.append(row)

npc_blob="\n".join(json.dumps(d,ensure_ascii=False) for d in assets.values()).casefold()
companions=[]
for kind,key in (("primary","primary_companions"),("secondary","secondary_companions")):
    for name in config[key]:
        components=[x.strip().casefold() for x in name.split(" and ")]
        matches=[word in npc_blob for word in components]
        ok=all(matches)
        companions.append({"name":name,"role":kind,"literal_name_found":ok,
                           "source":"Kingmaker Companion Guide, contents",
                           "quest_influence_verified":False})
        if not ok:
            issues.append({"severity":"review","code":"COMPANION_NAME_NOT_FOUND_LITERAL",
                           "companion":name,"caveat":"PT-BR aliases or source assets may encode another spelling"})

war=source/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerWarfareEngine.java"
repository=source/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
war_text=war.read_text(encoding="utf-8") if war.exists() else ""
repo_text=repository.read_text(encoding="utf-8") if repository.exists() else ""
war_checks={
 "combat_engine_source":bool(war_text),
 "army_attack_resolver":"resolveWarAttack(" in repo_text,
 "army_conditions_persisted":"updateArmyConditions(" in repo_text,
 "war_encounter_persistence":"advanceWarEncounter(" in repo_text,
 "damage_calculation":"applyArmyDamage(" in repo_text,
 "army_destruction_check":"checkArmyDestruction(" in repo_text,
}
for name,good in war_checks.items():
    if not good:issues.append({"severity":"critical","code":"WAR_SOURCE_SURFACE_MISSING","mechanic":name})

data={
 "schema":"braseiro-kingmaker-r36-real-source-book-crosswalk-audit-v1",
 "official_pdf_ids":{"kingmaker":"1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3",
   "companion":"18-yv9f-2Fz6fnnoZDFHKEfKSRu9KUViq"},
 "asset_count":len(assets),
 "official_chapters":report,
 "companions":companions,
 "war_source_surfaces":war_checks,
 "gates":{"chapter_structural_presence":not any(x["severity"]=="critical" and x["code"].startswith("CHAPTER_") for x in issues),
  "war_source_presence":all(war_checks.values()),
  "chapter_1_to_11_semantic_certification":False,
  "companions_quests_and_influence_certification":False,
  "war_rules_page_level_certification":False,
  "whole_campaign_end_to_end_android":False},
 "issues":issues,
 "source_assets_sha256":{name:hashlib.sha256((chdir/name).read_bytes()).hexdigest() for name in assets},
 "methodology":"Real published R35 chapter files + official PDF contents crosswalk. Name/structure presence alone is not semantic equivalence, and no live Android playthrough is claimed."
}
(out/"CROSSWALK_REPORT.json").write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding="utf-8")
lines=["# Kingmaker R36 — official contents crosswalk, real published source","","## Official chapters 1–11"]
for row in report:
    counts=row["record_counts"]
    lines.append(f"- **{row['official_chapter']}. {row['official_name']}** (book p. {row['official_page']}): "+", ".join(row["assets"])+f"; quests {counts['quests']}, scenes {counts['scenes']}, POIs {counts['pois']}")
lines+=["","## Companion Guide"]
for x in companions:lines.append(f"- {x['name']}: literal name {'present' if x['literal_name_found'] else 'not located'}; influence/quest certification pending")
lines+=["","## Warfare source surfaces"]
for k,v in war_checks.items():lines.append(f"- {k}: {'present' if v else 'MISSING'}")
lines+=["","## Explicitly not certified","- Full rulebook-to-implementation semantic coverage","- All quest branches and outcomes through Android","- Companion influence progression and specific quest consequences","- Warfare end-to-end, saves and restoration","",f"## Findings ({len(issues)})"]
for x in issues:lines.append("- "+json.dumps(x,ensure_ascii=False))
(out/"CROSSWALK_REPORT.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
print("BOOK_CROSSWALK_RESULT="+json.dumps({"chapters":len(report),"companions":len(companions),"assets":len(assets),"issues":len(issues),"critical":sum(x["severity"]=="critical" for x in issues),"war":war_checks},ensure_ascii=False))
for finding in issues:print("BOOK_CROSSWALK_FINDING="+json.dumps(finding,ensure_ascii=False))
raise SystemExit(2 if any(x["severity"]=="critical" for x in issues) else 0)
