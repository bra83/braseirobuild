#!/usr/bin/env python3
"""Evidence-driven Kingmaker Companion Guide + Warfare audit on canonical R35 assets.
Inspects real assets and production Java, never treats keyword hits as certification.
"""
from pathlib import Path
import json, re, unicodedata, argparse
p=argparse.ArgumentParser()
p.add_argument("--project",required=True)
p.add_argument("--out",required=True)
a=p.parse_args()
root=Path(a.project);out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
cross=json.loads((Path(__file__).parent/"KINGMAKER_CANONICAL_BOOK_CROSSWALK.json").read_text(encoding="utf-8"))
chapterdir=root/"android/app/src/main/assets/pathfinder/kingmaker/chapters"
sources=[]
for file in sorted(chapterdir.glob("chapter_*.json")):
    d=json.loads(file.read_text(encoding="utf-8"))
    sources.append((file.name,d))
def norm(s):
    return "".join(c for c in unicodedata.normalize("NFKD",str(s).casefold()) if not unicodedata.combining(c))
npcs=[(filename,r) for filename,d in sources for r in d.get("npcs",[]) if isinstance(r,dict)]
quests=[(filename,r) for filename,d in sources for r in d.get("quests",[]) if isinstance(r,dict)]
events=[(filename,r) for filename,d in sources for r in d.get("events",[]) if isinstance(r,dict)]
companions=[]
for category,key in (("primary","primary_companions"),("secondary","secondary_companions")):
    for person in cross[key]:
        parts=[norm(p.strip()) for p in person.split(" and ")]
        matched=[(file,r) for file,r in npcs if all(p in norm(json.dumps(r,ensure_ascii=False)) for p in parts)]
        candidateIds={v for _,r in matched for v in (r.get("npc_key"),r.get("id"),r.get("key")) if isinstance(v,str)}
        related=[]
        for file,r in quests:
            blob=norm(json.dumps(r,ensure_ascii=False))
            if any(p in blob for p in parts) or any(norm(uid) in blob for uid in candidateIds):
                related.append({"asset":file,"quest_key":r.get("quest_key"),"objective_count":len(r.get("objectives",[]))})
        relatedEvents=[]
        for file,r in events:
            blob=norm(json.dumps(r,ensure_ascii=False))
            if any(p in blob for p in parts) or any(norm(uid) in blob for uid in candidateIds):
                relatedEvents.append(file)
        companions.append({
            "name":person,"category":category,
            "npc_records":len(matched),
            "quest_literal_links":related,
            "related_event_assets":sorted(set(relatedEvents)),
            "manual_review_required":True,
            "note":"JSON text matching is not a certification of companion loyalty, branching, quests or dialogue"
        })

repo=root/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
war=root/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerWarfareEngine.java"
src=repo.read_text(encoding="utf-8")
warf=war.read_text(encoding="utf-8") if war.exists() else ""
checks={
 "army_attack_uses_active_encounter":'Não há batalha ativa' in src and 'resolveWarAttack(' in src,
 "attacker_target_distinct":'atacante' in norm(src) and 'alvo' in norm(src),
 "army_eligible_roster":'roster.contains(attackerClean)' in src and 'roster.contains(targetClean)' in src,
 "army_damage_mutation":'damageArmy(targetClean' in src,
 "hp_defeat_resolution":'targetAfter.optInt("hp")<=0' in src,
 "war_turn_advance_persisted":'advanceWarEncounter(' in src,
 "warfare_engine_available":bool(warf),
 "army_condition_persistence":'kingmaker_armies' in src and 'updateArmyConditions(' in src,
 "army_destruction_resolution":'checkArmyDestruction(' in src,
}
findings=[]
for row in companions:
    if not row["quest_literal_links"]:
        findings.append({"severity":"review","companion":row["name"],"issue":"No quest references found by literal name/ID; possible alternate PTBR alias"})
for name,present in checks.items():
    if not present:
        findings.append({"severity":"critical","war_mechanic":name,"issue":"Production Java source contract absent"})
result={
 "scope":"Official Companion Guide names; warfare production Java source",
 "sources":{"companion_pdf":"18-yv9f-2Fz6fnnoZDFHKEfKSRu9KUViq","kingmaker_pdf":"1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3"},
 "counts":{"npcs":len(npcs),"quests":len(quests),"companions":len(companions)},
 "companions":companions,"warfare_source_checks":checks,"findings":findings,
 "certification":{"companion_quests_and_loyalty":"NOT_CERTIFIED",
                 "warfare_rules_and_consequences":"NOT_CERTIFIED",
                 "android_integration":"NOT_CERTIFIED"},
 "explanation":"Presence and literal links do not prove fidelity or functioning branches; no physical or emulated live war battle is executed in this audit"
}
(out/"COMPANION_WAR_DEPTH.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
md=["# Companion Guide and Warfare — source depth crosswalk",""]
for row in companions:
    md.append(f"- {row['name']} ({row['category']}): {row['npc_records']} NPC records, {len(row['quest_literal_links'])} literal quest references, {len(row['related_event_assets'])} event sources")
md+=["","## Warfare source contracts"]
for key,ok in checks.items():md.append(f"- {key}: {'present' if ok else 'MISSING'}")
md+=["","Not certified: companion quest outcomes, loyalty, all battle rules and persistence in a real full game."]
(out/"COMPANION_WAR_DEPTH.md").write_text("\n".join(md)+"\n",encoding="utf-8")
print("COMPANION_WAR_DEPTH="+json.dumps({"companions":len(companions),"no_literal_quest_links":sum(not x["quest_literal_links"] for x in companions),"war_checks_passed":sum(checks.values()),"war_checks_total":len(checks),"critical":sum(f["severity"]=="critical" for f in findings)},ensure_ascii=False))
for f in findings:print("COMPANION_WAR_REVIEW="+json.dumps(f,ensure_ascii=False))
raise SystemExit(2 if any(f["severity"]=="critical" for f in findings) else 0)
