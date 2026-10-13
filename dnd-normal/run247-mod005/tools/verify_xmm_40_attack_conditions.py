#!/usr/bin/env python3
"""Independent source-to-Java comparator for 40 XMM on-hit condition riders."""
import argparse,json,re,urllib.request
from pathlib import Path
import verify_xmm_503_source as base
ROOT=Path(__file__).resolve().parent.parent
JAVA="app/src/main/java/com/braseiro/dnd5e/Xmm2025AttackConditionEngine.java"
PAT=re.compile(r'If the target is a (Tiny|Small|Medium|Large|Huge|Gargantuan) or smaller creature, it has the \{@condition (Prone|Grappled)\|XPHB\} condition(?: \(escape \{@dc (\d+)\}\))?\.')
def expected_rows(monsters):
    out=[]
    for i,monster in enumerate(monsters):
        for j,action in enumerate(monster.get("action",[])):
            entry=action.get("entries",[None])[0]
            if not isinstance(entry,str) or "{@atkr" not in entry or "{@h}" not in entry:continue
            for m in PAT.finditer(entry):
                out.append({"id":monster["name"],"name":base.RCH.sub("",action["name"]).strip(),
                    "slot":j,"ptr":f"/monster/{i}/action/{j}/entries/0",
                    "size":m.group(1),"condition":m.group(2).lower(),
                    "dc":int(m.group(3) or 0),"other":bool(entry[m.end():].strip())})
    return out
def check(project,raw):
    if base.blob_sha(raw)!=base.BESTIARY_SHA:raise ValueError("XMM_PINNED_BESTIARY_BLOB_MISMATCH")
    src=json.loads(raw)["monster"]
    expected=expected_rows(src)
    actual=base.java_rows((project/JAVA).read_text(encoding="utf-8"))
    if len(expected)!=40 or len(actual)!=40 or expected!=actual:
        raise ValueError("XMM_40_ATTACK_RIDERS_INDEPENDENT_SOURCE_MISMATCH")
    if sum(x["condition"]=="prone" for x in actual)!=29:raise ValueError("XMM_PRONE_COUNT")
    if sum(x["condition"]=="grappled" for x in actual)!=11:raise ValueError("XMM_GRAPPLE_COUNT")
    if sum(x["other"] for x in actual)!=8:raise ValueError("XMM_OTHER_RIDERS_COUNT")
    return {"source":"XMM","verified_riders":40,"prone":29,"grappled":11,
        "additional_manual":8,"source_blob_sha1":base.BESTIARY_SHA,
        "full_action_automation_certified":False}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source-file",type=Path)
    p.add_argument("--project-root",type=Path,default=ROOT)
    args=p.parse_args()
    if args.source_file:raw=args.source_file.read_bytes()
    else:
        req=urllib.request.Request(base.SOURCE+base.FILES[0],headers={"User-Agent":"Braseiro-XMM-SourceVerifier"})
        with urllib.request.urlopen(req,timeout=90) as f:raw=f.read(5*1024*1024)
    print(json.dumps(check(args.project_root,raw),indent=2))
if __name__=="__main__":main()
