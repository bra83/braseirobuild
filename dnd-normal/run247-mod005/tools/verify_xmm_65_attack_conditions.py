#!/usr/bin/env python3
"""Independent source-to-Java comparator for 65 XMM source-pinned on-hit condition riders."""
import argparse,json,re,urllib.request
from pathlib import Path
import verify_xmm_503_source as base
ROOT=Path(__file__).resolve().parent.parent
JAVA="app/src/main/java/com/braseiro/dnd5e/Xmm2025AttackConditionEngine.java"
PRONE=re.compile(r'If the target is a (Tiny|Small|Medium|Large|Huge|Gargantuan) or smaller creature, it has the \{@condition Prone\|XPHB\} condition\.')
GRAPPLED=re.compile(r'\{@condition Grappled\|XPHB\} condition \(escape \{@dc (\d+)\}\)')
SIZE=re.compile(r'\b(Tiny|Small|Medium|Large|Huge|Gargantuan) or smaller\b',re.I)
SAVE=re.compile(r"\{@actSave\s+(str|dex|con|int|wis|cha)\}\s*\{@dc\s*(\d+)",re.I)
OTHER=re.compile(r'\{@condition (?!Grappled\|XPHB)[^}]+\}|\{@damage |\b(?:until|while|takes|can pull|move|ability checks|disadvantage|advantage)\b',re.I)
RANGE=re.compile(r'\brange\s+(\d+)(?:/(\d+))?\s+(?:ft\.?\b|feet\b)',re.I)
ALTERNATE_DAMAGE=re.compile(r'\bor\s+\d+\s*\(\{@damage\b',re.I)
def expected_rows(monsters):
    out=[]
    for i,monster in enumerate(monsters):
        for j,action in enumerate(monster.get("action",[])):
            entries=action.get("entries",[])
            entry=entries[0] if entries and isinstance(entries[0],str) else ""
            if "{@atkr" not in entry or "{@hit" not in entry:continue
            name=base.RCH.sub("",action["name"]).strip()
            ptr=f"/monster/{i}/action/{j}/entries/0"
            for m in PRONE.finditer(entry):
                out.append({"id":monster["name"],"name":name,"slot":j,"ptr":ptr,
                    "size":m.group(1),"condition":"prone","dc":0,
                    "other":bool(entry[m.end():].strip()) or bool(ALTERNATE_DAMAGE.search(entry))})
            if "saving throw" in entry.lower():continue
            for m in GRAPPLED.finditer(entry):
                sizes=list(SIZE.finditer(entry[:m.start()]))
                # Do not infer a grapple size cap when the source does not state one.
                if not sizes:continue
                row={"id":monster["name"],"name":name,"slot":j,"ptr":ptr,
                    "size":sizes[-1].group(1).title(),"condition":"grappled",
                    "dc":int(m.group(1)),"other":bool(OTHER.search(entry[m.end():]))}
                save=SAVE.search(entry)
                if save:
                    row["save_ability"]=save.group(1).lower()
                    row["save_dc"]=int(save.group(2))
                out.append(row)
    return out
def check_ranged_prone_attack_ranges(source,compiled,contracts):
    source_by_id={m["name"]:m for m in source}
    compiled_by_id={m["id"]:m for m in compiled}
    checked=0
    for row in contracts:
        if row["condition"]!="prone":continue
        profile=compiled_by_id[row["id"]]
        action=profile["actions"][row["slot"]]
        modes=action.get("attack_modes",[])
        if "r" not in modes or "m" in modes:continue
        source_action=source_by_id[row["id"]]["action"][row["slot"]]
        entries=source_action.get("entries",[])
        entry=entries[0] if entries and isinstance(entries[0],str) else ""
        match=RANGE.search(entry)
        if not match:raise ValueError("XMM_RANGED_PRONE_SOURCE_RANGE_MISSING:"+row["id"]+"::"+row["name"])
        normal=int(match.group(1));far=int(match.group(2) or normal)
        if action.get("range_normal_ft")!=normal or action.get("range_long_ft")!=far:
            raise ValueError("XMM_RANGED_PRONE_SOURCE_RANGE_MISMATCH:"+row["id"]+"::"+row["name"])
        checked+=1
    return checked
def check(project,raw):
    if base.blob_sha(raw)!=base.BESTIARY_SHA:raise ValueError("XMM_PINNED_BESTIARY_BLOB_MISMATCH")
    src=json.loads(raw)["monster"]
    expected=expected_rows(src)
    actual=base.java_rows((project/JAVA).read_text(encoding="utf-8"))
    if len(expected)!=65 or len(actual)!=65 or expected!=actual:
        raise ValueError("XMM_65_ATTACK_RIDERS_INDEPENDENT_SOURCE_MISMATCH")
    compiled=base.java_rows((project/base.JAVA_RUNTIME).read_text(encoding="utf-8"))
    ranged=check_ranged_prone_attack_ranges(src,compiled,actual)
    if sum(x["condition"]=="prone" for x in actual)!=29:raise ValueError("XMM_PRONE_COUNT")
    if sum(x["condition"]=="grappled" for x in actual)!=36:raise ValueError("XMM_GRAPPLE_COUNT")
    if sum(x["other"] for x in actual)!=22:raise ValueError("XMM_OTHER_RIDERS_COUNT")
    return {"source":"XMM","verified_riders":65,"prone":29,"grappled":36,
        "additional_manual":22,"source_blob_sha1":base.BESTIARY_SHA,
        "ranged_prone_ranges_compared":ranged,
        "full_action_automation_certified":False}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source-file",type=Path)
    p.add_argument("--project-root",type=Path,default=ROOT)
    args=p.parse_args()
    if args.source_file:raw=args.source_file.read_bytes()
    else:
        raw=base.fetch_source_bytes(base.SOURCE+base.FILES[0],
            user_agent="Braseiro-XMM-SourceVerifier")
    print(json.dumps(check(args.project_root,raw),indent=2))
if __name__=="__main__":main()
