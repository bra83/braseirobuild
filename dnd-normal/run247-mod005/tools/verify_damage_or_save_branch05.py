#!/usr/bin/env python3
"""Standalone Java 8 gate for XMM batch-05, optional pinned source check.

No Android/SQLite or generated dice. The exact upstream bestiary file is
optional and is never copied into GitHub. Do not equate a green JVM gate with
a completed integrated combat engine.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT / "app/src/main/java/com/braseiro/dnd5e"
TESTS=ROOT / "app/src/test/java/com/braseiro/dnd5e"
PINNED_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"
EXPECTED=(
    (416,"Steam Mephit", "action",1,"Steam Breath"),
    (447,"Ultroloth","action",1,"Mercurial Whip"),
    (452,"Vampire Nightbringer","action",1,"Bite"),
    (454,"Vampire Umbral Lord","bonus",0,"Sanguine Drain"),
    (463,"Warrior Commander","action",1,"Greatsword"),
    (469,"Werebear","action",1,"Bite (Bear or Hybrid Form Only)"),
    (470,"Wereboar","action",1,"Gore (Boar or Hybrid Form Only)"),
    (471,"Wererat","action",1,"Bite (Rat or Hybrid Form Only)"),
    (472,"Weretiger","action",1,"Bite (Tiger or Hybrid Form Only)"),
    (473,"Werewolf","action",1,"Bite (Wolf or Hybrid Form Only)"),
    (475,"Wight","action",3,"Life Drain"),
    (480,"Worg","action",0,"Bite"),
)
def source(path):
    raw=path.read_bytes()
    blob=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if blob!=PINNED_BLOB:raise ValueError("PINNED_BLOB_MISMATCH "+blob)
    doc=json.loads(raw)
    for idx,name,field,pos,title in EXPECTED:
        monster=doc["monster"][idx]
        if monster["name"]!=name:raise AssertionError(("MONSTER",idx,name))
        action=monster[field][pos]
        if action["name"]!=title:raise AssertionError(("ACTION",idx,title))
        if not action.get("entries"):raise AssertionError(("ENTRIES",idx,title))
    print("XMM_BRANCH05_PINNED_12_SOURCE_IDENTITIES=PASS")

def check():
    with tempfile.TemporaryDirectory(prefix="xmm-b05-") as d:
        subprocess.run(["javac","--release","8","-d",d,
            str(SRC/"DamageMitigationRules.java"),
            str(SRC/"XmmDamageOrSaveBranch05.java"),
            str(TESTS/"XmmDamageOrSaveBranch05Scenarios.java")],check=True)
        subprocess.run(["java","-cp",d,
            "com.braseiro.dnd5e.XmmDamageOrSaveBranch05Scenarios"],check=True)
    print("BRANCH05_ANDROID_COMBAT=NOT_INTEGRATED")
    print("BRANCH05_SQLITE=NOT_INTEGRATED")

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",type=Path,help="Official source snapshot with exact pinned blob SHA")
    args=parser.parse_args()
    if args.source:source(args.source)
    else:print("XMM_BRANCH05_SOURCE_PIN=NOT_TESTED_LOCALLY")
    check()
