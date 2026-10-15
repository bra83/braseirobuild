#!/usr/bin/env python3
"""XMM batch 10 / attack_hit_condition-01: JVM-only deterministic regressions.

Optional --source checks the exact source Git blob and four published XMM
action texts. This is NOT the integrated Android/SQLite combat gate.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/"app/src/main/java/com/braseiro/dnd5e"
TEST=ROOT/"app/src/test/java/com/braseiro/dnd5e"
PINNED_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"
ACTIONS=(
    (300,"Mind Flayer","action",0,"Tentacles","{@hit 7}"),
    (376,"Roper","action",2,"Tentacle","escape {@dc 14}"),
    (431,"Swarm of Stirges","action",0,"Swarm of Proboscises","{@damage 2d6}"),
    (433,"Tarrasque","action",1,"Bite","escape {@dc 20}"),
)
def check_source(path):
    raw=path.read_bytes()
    digest=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if digest!=PINNED_BLOB:
        raise ValueError("XMM_SOURCE_BLOB_NOT_PINNED "+digest)
    db=json.loads(raw)
    for idx,name,field,pos,action,anchor in ACTIONS:
        monster=db["monster"][idx]
        assert monster["name"]==name,("MONSTER",idx)
        record=monster[field][pos]
        assert record["name"]==action,("ACTION",idx)
        whole=" ".join(e for e in record["entries"] if isinstance(e,str))
        assert anchor in whole,("SOURCE_CONTRACT",idx,anchor)
    print("XMM_HIT01_PINNED_SOURCE=PASS; 4 official action pointers")
def check_java():
    with tempfile.TemporaryDirectory(prefix="xmm-hit01-") as out:
        subprocess.run(["javac","--release","8","-d",out,
            str(MAIN/"DamageMitigationRules.java"),
            str(MAIN/"XmmAttackHitCondition01.java"),
            str(TEST/"XmmAttackHitCondition01Scenarios.java")
        ],check=True)
        subprocess.run(["java","-cp",out,
            "com.braseiro.dnd5e.XmmAttackHitCondition01Scenarios"
        ],check=True)
    print("HIT01_ANDROID_COMBAT=NOT_INTEGRATED")
    print("HIT01_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("HIT01_TURN_GRAPPLE_ADAPTER=NOT_INTEGRATED")
if __name__=="__main__":
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--source",type=Path,help="Exact XMM source-bestary JSON with pinned blob")
    args=ap.parse_args()
    if args.source:check_source(args.source)
    else:print("XMM_HIT01_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    check_java()
