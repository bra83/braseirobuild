#!/usr/bin/env python3
"""Lightweight Java 8 regression + optional exact XMM source verification.

No Codex, Gradle, Android, or project-wide checkout required. Successful
JVM checks are NOT certification of live combat, timed effects, or SQLite.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/braseiro/dnd5e"
TEST = ROOT / "app/src/test/java/com/braseiro/dnd5e"
PINNED_BLOB = "9a4137b508ef37562ed948894a70b1594b57e85c"
ACTIONS = (
    (372, "Revenant", "bonus", 0, "Vow of Revenge", 0),
    (386, "Satyr Revelmaster", "action", 2, "Fey Melody", 2),
    (404, "Spectator", "action", 2, "Eye Rays", 4),
    (408, "Sphinx of Valor", "action", 2, "Roar", 3),
    (409, "Sphinx of Wonder", "reaction", 0, "Burst of Ingenuity", 0),
    (448, "Umber Hulk", "bonus", 0, "Confusing Gaze", 3),
    (449, "Unicorn", "legendary", 1, "Shimmering Shield", 0),
    (490, "Young Copper Dragon", "action", 3, "Slowing Breath", 0),
    (491, "Young Gold Dragon", "action", 3, "Weakening Breath", 0),
)
def verify_source(path: Path):
    raw = path.read_bytes()
    actual = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    if actual != PINNED_BLOB:
        raise ValueError("XMM_SOURCE_BLOB_NOT_PINNED " + actual)
    db = json.loads(raw)
    for i, monster, category, idx, action_name, min_branches in ACTIONS:
        obj = db["monster"][i]
        assert obj["name"] == monster, ("MONSTER", i)
        action = obj[category][idx]
        assert action["name"].startswith(action_name), ("ACTION", i, action_name)
        assert action["entries"], ("EMPTY_ACTION", i)
        if min_branches:
            choices = action["entries"][1]
            assert choices["type"] == "list"
            assert len(choices["items"]) >= min_branches
    print("XMM_OTHER03_PINNED_SOURCE_IDENTITIES=PASS; 9 actions, 4 Spectator rays, 3 Sphinx roars")

def verify_java():
    with tempfile.TemporaryDirectory(prefix="xmm-other03-jvm-") as out:
        subprocess.run([
            "javac", "--release", "8", "-d", out,
            str(SRC / "DamageMitigationRules.java"),
            str(SRC / "XmmOtherSourceEffect03.java"),
            str(TEST / "XmmOtherSourceEffect03Scenarios.java")
        ], check=True)
        subprocess.run([
            "java", "-cp", out,
            "com.braseiro.dnd5e.XmmOtherSourceEffect03Scenarios"
        ], check=True)
    print("OTHER03_ANDROID_COMBAT=NOT_INTEGRATED")
    print("OTHER03_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")
    print("OTHER03_TIMED_EFFECT_LIFECYCLE=NOT_INTEGRATED")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Exact pinned upstream bestiary-xmm.json")
    args = parser.parse_args()
    if args.source:
        verify_source(args.source)
    else:
        print("XMM_OTHER03_PINNED_SOURCE=NOT_TESTED_LOCALLY")
    verify_java()
