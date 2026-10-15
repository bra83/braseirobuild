#!/usr/bin/env python3
"""JDK-only 2025 XMM lot-12 Animated Rug / Smother independent gate.

Source: bestiary-xmm.json exact Git blob SHA. --source is optional because
the pinned file is not redistributed. Android, SQLite and condition lifecycle
are NOT certified by the standalone Java regression.
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
BLOB="9a4137b508ef37562ed948894a70b1594b57e85c"

def source_gate(path):
    b=path.read_bytes()
    sha=hashlib.sha1(b"blob "+str(len(b)).encode()+b"\0"+b).hexdigest()
    if sha!=BLOB:raise ValueError("XMM_SOURCE_BLOB_MISMATCH "+sha)
    m=json.loads(b)["monster"][31]
    assert m["name"]=="Animated Rug of Smothering"
    action=m["action"][0]
    assert action["name"]=="Smother"
    text=" ".join(x for x in action["entries"] if isinstance(x,str))
    assert "{@hit 5}" in text
    assert "{@damage 2d6 + 3}" in text
    assert "instead of dealing damage" in text
    assert "escape {@dc 13}" in text
    assert "halves the damage it takes" in text
    assert "start of each of its turns" in text
    print("XMM_CHOICE01_PINNED_SOURCE=PASS; Animated Rug of Smothering")

def jvm():
    with tempfile.TemporaryDirectory(prefix="xmm-choice12-") as out:
        subprocess.run(["javac","--release","8","-d",out,
            str(SRC/"DamageMitigationRules.java"),
            str(SRC/"XmmExplicitEffectChoice01.java"),
            str(TEST/"XmmExplicitEffectChoice01Scenarios.java"),
        ],check=True)
        subprocess.run(["java","-cp",out,
            "com.braseiro.dnd5e.XmmExplicitEffectChoice01Scenarios"],check=True)
    print("CHOICE01_ANDROID_COMBAT=NOT_INTEGRATED")
    print("CHOICE01_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("CHOICE01_TURN_AND_CONDITION_ADAPTER=NOT_INTEGRATED")

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source",type=Path,help="Exact upstream bestiary-xmm.json blob")
    a=p.parse_args()
    if a.source:source_gate(a.source)
    else:print("XMM_CHOICE01_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    jvm()
