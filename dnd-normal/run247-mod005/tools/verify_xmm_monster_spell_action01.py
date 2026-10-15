#!/usr/bin/env python3
"""XMM lot-13 Blue Slaad Mutating Claw JVM + optional pinned source gate.

Source text mentions Wish as transformation REVERSAL, NOT a spell cast by this
monster. The core is not integrated into Android combat, SQLite, or a clock.
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
BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"

def source_gate(path):
    raw=path.read_bytes()
    got=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if got!=BLOB:raise ValueError("XMM_PINNED_SOURCE_MISMATCH "+got)
    m=json.loads(raw)["monster"][71]
    assert m["name"]=="Blue Slaad"
    a=m["action"][1]
    assert a["name"]=="Mutating Claw"
    text=" ".join(x for x in a["entries"] if isinstance(x,str))
    for anchor in (
        "{@hit 8}","{@damage 2d6 + 5}","{@damage 1d6}","{@dc 15}",
        "{@dice 3d6}","after every 24 hours","Red Slaad","Green Slaad",
        "Only a {@spell Wish|XPHB} spell can reverse"
    ):
        assert anchor in text,("MISSING_OFFICIAL_SOURCE_CONTRACT",anchor)
    print("XMM_MONSTER13_PINNED_SOURCE=PASS; Blue Slaad action and Wish reversal clause")

def jdk():
    with tempfile.TemporaryDirectory(prefix="xmm-slaad13-") as d:
        subprocess.run(["javac","--release","8","-d",d,
            str(MAIN/"DamageMitigationRules.java"),
            str(MAIN/"XmmMonsterSpellAction01.java"),
            str(TEST/"XmmMonsterSpellAction01Scenarios.java"),
        ],check=True)
        subprocess.run(["java","-cp",d,
            "com.braseiro.dnd5e.XmmMonsterSpellAction01Scenarios"
        ],check=True)
    print("MONSTER13_ANDROID_COMBAT=NOT_INTEGRATED")
    print("MONSTER13_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("MONSTER13_24H_CLOCK_AND_WISH_ADAPTER=NOT_INTEGRATED")

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source",type=Path,help="Exact upstream pinned XMM bestiary JSON file")
    args=p.parse_args()
    if args.source:source_gate(args.source)
    else:print("XMM_MONSTER13_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    jdk()
