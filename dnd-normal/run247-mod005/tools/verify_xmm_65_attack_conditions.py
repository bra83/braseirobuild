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
                    "other":bool(entry[m.end():].strip())})
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
def check(project,raw):
    if base.blob_sha(raw)!=base.BESTIARY_SHA:raise ValueError("XMM_PINNED_BESTIARY_BLOB_MISMATCH")
    src=json.loads(raw)["monster"]
    expected=expected_rows(src)
    actual=base.java_rows((project/JAVA).read_text(encoding="utf-8"))
    if len(expected)!=65 or len(actual)!=65 or expected!=actual:
        raise ValueError("XMM_65_ATTACK_RIDERS_INDEPENDENT_SOURCE_MISMATCH")
    if sum(x["condition"]=="prone" for x in actual)!=29:raise ValueError("XMM_PRONE_COUNT")
    if sum(x["condition"]=="grappled" for x in actual)!=36:raise ValueError("XMM_GRAPPLE_COUNT")
    if sum(x["other"] for x in actual)!=21:raise ValueError("XMM_OTHER_RIDERS_COUNT")
    return {"source":"XMM","verified_riders":65,"prone":29,"grappled":36,
        "additional_manual":21,"source_blob_sha1":base.BESTIARY_SHA,
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
