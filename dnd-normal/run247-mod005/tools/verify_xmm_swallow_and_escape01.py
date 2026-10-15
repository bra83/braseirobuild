#!/usr/bin/env python3
"""XMM lot 11 / swallow_and_escape-01: standalone JDK 8 contract gate.

--source optionally checks the *exact pinned* upstream bestiary blob, and
requires source strings for both swallow effects. This gate proves only a
pure JVM core, not live Android/SQLite movement or combat integration.
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
SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"

def verify_source(path):
    raw=path.read_bytes()
    blob=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if blob!=SOURCE_BLOB:
        raise ValueError("XMM_SOURCE_BLOB_MISMATCH "+blob)
    monsters=json.loads(raw)["monster"]
    expected=[
        (182,"Giant Frog","{@damage 2d4}","Small or smaller"),
        (194,"Giant Toad","{@damage 3d6}","Medium or smaller"),
    ]
    for idx,name,dice,size in expected:
        m=monsters[idx]
        assert m["name"]==name,("MONSTER_NAME",idx,name)
        assert m["action"][1]["name"]=="Swallow",("SWALLOW_ACTION",idx)
        all_text=" ".join(x for x in m["action"][1]["entries"] if isinstance(x,str))
        assert dice in all_text and size in all_text,("MECHANICAL_CONTRACT",idx)
        assert "Prone" in all_text and "Restrained" in all_text,("ESCAPE_RIDER",idx)
    print("XMM_SWALLOW01_PINNED_SOURCE=PASS; Giant Frog and Giant Toad")

def gate():
    with tempfile.TemporaryDirectory(prefix="xmm-swallow11-") as d:
        subprocess.run(["javac","--release","8","-d",d,
            str(MAIN/"DamageMitigationRules.java"),
            str(MAIN/"XmmSwallowAndEscape01.java"),
            str(TEST/"XmmSwallowAndEscape01Scenarios.java")
        ],check=True)
        subprocess.run(["java","-cp",d,
            "com.braseiro.dnd5e.XmmSwallowAndEscape01Scenarios"
        ],check=True)
    print("SWALLOW01_ANDROID_COMBAT=NOT_INTEGRATED")
    print("SWALLOW01_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("SWALLOW01_TURN_AND_MOVEMENT_ADAPTER=NOT_INTEGRATED")

if __name__=="__main__":
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--source",type=Path,
        help="Exact upstream source JSON matching the pinned Git blob SHA")
    args=ap.parse_args()
    if args.source:verify_source(args.source)
    else:print("XMM_SWALLOW01_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    gate()
