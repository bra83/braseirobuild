#!/usr/bin/env python3
"""R36 Kingmaker: enforce official Chapter 10 -> 11 story gating.

The official Chapter 11 starts only after the Nyrissa resolution (slain or
forgiven). This patch fixes a genuine missing cross-chapter quest prerequisite
without rewriting player choices, side quests, saves or companion arcs.

Source: Kingmaker 2e pp. 480-484, "Twin Conclusions".
Applies to the verified extracted R36 source, not the immutable R35 archive.
This is a single confirmed correction, NOT full chapter certification.
"""
from pathlib import Path
import json
import sys

ASSETS=Path("android/app/src/main/assets/pathfinder/kingmaker/chapters")
ENDING="q_redencao_espada_briar"
OPENING="q_revelacao_rei_lanterna"

def transform(previous,final):
    if previous.get("chapter_number")!=9 or final.get("chapter_number")!=10:
        raise RuntimeError("Expected canonical app asset chapters 09 and 10")
    previous_quests=[q for q in previous.get("quests",[]) if q.get("quest_key")==ENDING]
    final_quests=[q for q in final.get("quests",[]) if q.get("quest_key")==OPENING]
    if len(previous_quests)!=1 or len(final_quests)!=1:
        raise RuntimeError("Missing/duplicate Nyrissa resolution or Lantern King opening")
    required=previous_quests[0]
    if not isinstance(required.get("objectives"),list) or not required["objectives"]:
        raise RuntimeError("Nyrissa resolution has no objectives")
    next_quest=final_quests[0]
    current=next_quest.get("prerequisites")
    if not isinstance(current,list) or any(not isinstance(x,str) for x in current):
        raise RuntimeError("Invalid Chapter 11 prerequisite model")
    if current.count(ENDING)>1:
        raise RuntimeError("Duplicate predecessor")
    if ENDING not in current:
        next_quest["prerequisites"]=current+[ENDING]
    next_quest["book_progression_source"]="Kingmaker 2e pp. 480-484: Chapter 11 follows the resolution of Nyrissa"
    return final

def apply(root):
    root=Path(root).resolve()
    version=root/"version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8"):
        raise RuntimeError("Canonical R36 generated project required")
    a=root/ASSETS/"chapter_09_sound_thousand_screams.json"
    z=root/ASSETS/"chapter_10_curse_lantern_king.json"
    previous=json.loads(a.read_text(encoding="utf-8"))
    final=json.loads(z.read_text(encoding="utf-8"))
    corrected=transform(previous,final)
    z.write_text(json.dumps(corrected,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    return z

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_chapter11_gate.py <R36_android_studio_project>")
    print("KINGMAKER_CHAPTER_11_OFFICIAL_PROGRESS_GATE_APPLIED",apply(sys.argv[1]))
