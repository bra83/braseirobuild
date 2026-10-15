#!/usr/bin/env python3
"""JDK 8 independent verification for 10 source-pinned XMM other effects.

--source expects exact 5etools-src bestiary-xmm.json Git blob SHA.
It is never copied into the repo. Android/SQLite or long-term condition
scheduler are NOT certified by this standalone verification.
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
CASES=[
    (9,"Adult Copper Dragon","action",3,"Slowing Breath",0),
    (10,"Adult Gold Dragon","action",3,"Weakening Breath",0),
    (21,"Ancient Copper Dragon","action",3,"Slowing Breath",0),
    (22,"Ancient Gold Dragon","action",3,"Weakening Breath",0),
    (60,"Beholder","action",2,"Eye Rays",10),
    (61,"Beholder Zombie","action",2,"Eye Rays",4),
    (63,"Berserker Commander","bonus",0,"Frenzied Rush",0),
    (103,"Copper Dragon Wyrmling","action",2,"Slowing Breath",0),
    (112,"Cyclops Sentry","reaction",0,"Limited Foresight",0),
    (120,"Death Tyrant","action",2,"Eye Rays",10),
]
def check_source(path):
    raw=path.read_bytes()
    actual=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if actual!=SOURCE_BLOB:
        raise ValueError("PINNED_BESTIARY_BLOB_MISMATCH "+actual)
    doc=json.loads(raw)
    for idx,monster_name,field,index,action_name,ray_count in CASES:
        monster=doc["monster"][idx]
        assert monster["name"]==monster_name,("MONSTER",idx)
        action=monster[field][index]
        assert action["name"].startswith(action_name),("ACTION",idx,index)
        assert len(action["entries"])>=1,("ENTRIES",idx)
        if ray_count:
            rays=action["entries"][1]
            assert rays["type"]=="list" and len(rays["items"])==ray_count,("RAYS",idx)
    print("XMM_OTHER01_PINNED_SOURCE_IDENTITIES=PASS; 10 entries, eye rays 10+4+10")

def compile_and_run():
    with tempfile.TemporaryDirectory(prefix="xmm-other01-") as build:
        subprocess.run([
            "javac","--release","8","-d",build,
            str(MAIN/"DamageMitigationRules.java"),
            str(MAIN/"XmmOtherSourceEffect01.java"),
            str(TEST/"XmmOtherSourceEffect01Scenarios.java")
        ],check=True)
        subprocess.run([
            "java","-cp",build,
            "com.braseiro.dnd5e.XmmOtherSourceEffect01Scenarios"
        ],check=True)
    print("OTHER01_COMBAT_ANDROID=NOT_INTEGRATED")
    print("OTHER01_SQLITE_BACKUP21=NOT_INTEGRATED")
    print("OTHER01_TIMED_EFFECT_RUNNER=NOT_INTEGRATED")

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",type=Path,help="Exact pinned upstream bestiary-xmm.json")
    args=parser.parse_args()
    if args.source:check_source(args.source)
    else:print("XMM_OTHER01_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    compile_and_run()
