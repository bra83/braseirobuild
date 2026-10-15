#!/usr/bin/env python3
"""JDK-only Wraith Life Drain / damage_or_save_branch-06 regression gate.

This deliberately does not run Android combat or SQLite. --source validates
the exact Git blob SHA and the 5etools source pointer for this one action.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT / "app/src/main/java/com/braseiro/dnd5e"
TEST=ROOT / "app/src/test/java/com/braseiro/dnd5e"
SHA="9a4137b508ef37562ed948894a70b1594b57e85c"

def check_source(path):
    raw=path.read_bytes()
    actual=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    if actual!=SHA:raise ValueError("PINNED_XMM_BLOB_CHANGED: "+actual)
    doc=json.loads(raw)
    monster=doc["monster"][481]
    assert monster["name"]=="Wraith"
    action=monster["action"][0]
    assert action["name"]=="Life Drain"
    entry=action["entries"][0]
    assert "{@hit 6}" in entry and "{@damage 4d8 + 3}" in entry
    assert "maximum decreases" in entry
    print("XMM_BRANCH06_PINNED_SOURCE=PASS; /monster/481/action/0/entries/0")

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--source",type=Path,help="The exact upstream pinned bestiary-xmm.json")
    args=ap.parse_args()
    if args.source:check_source(args.source)
    else:print("XMM_BRANCH06_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    with tempfile.TemporaryDirectory(prefix="xmm-branch06-") as d:
        subprocess.run([
            "javac","--release","8","-d",d,
            str(PKG/"DamageMitigationRules.java"),
            str(PKG/"XmmDamageOrSaveBranch06.java"),
            str(TEST/"XmmDamageOrSaveBranch06Scenarios.java")
        ],check=True)
        subprocess.run(["java","-cp",d,
            "com.braseiro.dnd5e.XmmDamageOrSaveBranch06Scenarios"],check=True)
    print("XMM_BRANCH06_ANDROID_COMBAT=NOT_INTEGRATED")
    print("XMM_BRANCH06_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")

if __name__=="__main__":
    main()
