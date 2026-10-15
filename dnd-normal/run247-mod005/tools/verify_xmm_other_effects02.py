#!/usr/bin/env python3
"""JDK-only, isolated XMM other_source_effect-02 (batch 08) regression.

Optional --source verifies the exact Git blob SHA and ten official action
identities from the source-pinned XMM bestiary. No Gradle or Codex required.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/com/braseiro/dnd5e"
TEST=ROOT/"app/src/test/java/com/braseiro/dnd5e"
SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"
ACTIONS=(
    (120,"Death Tyrant","bonus",0,"Negative Energy Cone"),
    (122,"Demilich","legendary",0,"Energy Drain"),
    (140,"Elemental Cataclysm","action",2,"Cataclysmic Event"),
    (141,"Elemental Cultist","reaction",0,"Elemental Absorption"),
    (144,"Empyrean","legendary",0,"Bolster"),
    (161,"Frost Giant","bonus",0,"War Cry"),
    (211,"Gnoll Pack Lord","action",3,"Incite Rampage"),
    (218,"Gold Dragon Wyrmling","action",3,"Weakening Breath"),
    (317,"Nalfeshnee","reaction",0,"Pursuit"),
    (344,"Pirate Admiral","bonus",0,"Rally"),
)
def verify_source(path: Path):
    raw=path.read_bytes()
    actual=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if actual!=SOURCE_BLOB:raise ValueError("PINNED_XMM_BLOB_MISMATCH "+actual)
    data=json.loads(raw)
    for index,monster,section,num,action in ACTIONS:
        m=data["monster"][index]
        assert m["name"]==monster,("monster",index,monster)
        a=m[section][num]
        assert a["name"].startswith(action),("action",index,action)
        assert a["entries"],("empty source action",index)
    cat=data["monster"][140]["action"][2]["entries"][1]
    assert cat["type"]=="list" and len(cat["items"])==4
    print("XMM_OTHER02_PINNED_SOURCE_IDENTITIES=PASS; 10 official actions and 4 cataclysm variants")

def run_java():
    with tempfile.TemporaryDirectory(prefix="xmm-other02-") as tmp:
        subprocess.run([
            "javac","--release","8","-d",tmp,
            str(SRC/"DamageMitigationRules.java"),
            str(SRC/"XmmOtherSourceEffect02.java"),
            str(TEST/"XmmOtherSourceEffect02Scenarios.java")
        ],check=True)
        subprocess.run([
            "java","-cp",tmp,
            "com.braseiro.dnd5e.XmmOtherSourceEffect02Scenarios"
        ],check=True)
    print("XMM_OTHER02_ANDROID_COMBAT=NOT_INTEGRATED")
    print("XMM_OTHER02_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("XMM_OTHER02_TIMED_EFFECT_LIFECYCLE=NOT_INTEGRATED")

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source",type=Path,help="Exact pinned upstream bestiary-xmm.json")
    args=parser.parse_args()
    if args.source:verify_source(args.source)
    else:print("XMM_OTHER02_SOURCE_PIN=NOT_TESTED_LOCALLY")
    run_java()

if __name__=="__main__":
    main()
