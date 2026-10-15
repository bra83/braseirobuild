#!/usr/bin/env python3
"""JDK-only XMM branch-04 core checks with optional pinned source identity.

This is not Android/SQLite combat certification. Source blob must be the exact
upstream pinned JSON, not reconstructed or app-compiled approximations.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PKG = ROOT / "app/src/main/java/com/braseiro/dnd5e"
TEST = ROOT / "app/src/test/java/com/braseiro/dnd5e"
PINNED_BLOB = "9a4137b508ef37562ed948894a70b1594b57e85c"
EXPECTED = [
    (311, "Mummy", "Rotting Fist", 1),
    (312, "Mummy Lord", "Rotting Fist", 1),
    (339, "Peryton", "Talons", 2),
    (344, "Pirate Admiral", "Scimitar", 1),
    (345, "Pirate Captain", "Rapier", 1),
    (358, "Psychic Gray Ooze", "Pseudopod", 0),
    (365, "Rakshasa", "Cursed Touch", 1),
    (369, "Red Slaad", "Injecting Claw", 1),
    (393, "Shadow", "Draining Swipe", 0),
    (403, "Solar", "Flying Sword", 1),
    (405, "Specter", "Life Drain", 0),
    (407, "Sphinx of Secrets", "Curse of the Riddle", 2),
]


def validate_source(path: Path) -> None:
    raw = path.read_bytes()
    blob = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    if blob != PINNED_BLOB:
        raise ValueError("PINNED_XMM_SOURCE_BLOB_MISMATCH: " + blob)
    doc = json.loads(raw)
    for num, name, action, slot in EXPECTED:
        monster = doc["monster"][num]
        assert monster["name"] == name, ("MONSTER_CHANGED", num, name)
        entry = monster["action"][slot]
        assert entry["name"] == action, ("ACTION_CHANGED", num, name, action)
        assert isinstance(entry["entries"][0], str), ("SOURCE_ENTRY_NOT_TEXT", name)
        assert "{@atkr" in entry["entries"][0] or "{@actSave" in entry["entries"][0], name
        if name in ("Pirate Admiral", "Red Slaad"):
            assert len(entry["entries"]) > 1, ("SOURCE_RIDER_ENTRY_MISSING", name)
    print("BRANCH04_SOURCE_PIN=PASS; EXACT_ACTION_IDENTITIES=12; SEMANTIC_RUNTIME_CERTIFICATION=NOT_TESTED")


def validate_code() -> None:
    with tempfile.TemporaryDirectory(prefix="xmm-damage04-") as out:
        cmd = [
            "javac", "--release", "8", "-d", out,
            str(PKG / "DamageMitigationRules.java"),
            str(PKG / "XmmDamageOrSaveBranch04.java"),
            str(TEST / "XmmDamageOrSaveBranch04Scenarios.java"),
        ]
        subprocess.run(cmd, check=True)
        subprocess.run([
            "java", "-cp", out,
            "com.braseiro.dnd5e.XmmDamageOrSaveBranch04Scenarios",
        ], check=True)
    print("BRANCH04_COMBAT_INTEGRATION=NOT_TESTED")
    print("BRANCH04_SQLITE_PERSISTENCE=NOT_IMPLEMENTED")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Pinned bestiary-xmm.json obtained independently")
    args = parser.parse_args()
    if args.source is not None:
        validate_source(args.source)
    else:
        print("BRANCH04_SOURCE_PIN=NOT_TESTED_LOCALLY; pass --source /path/to/pinned-bestiary.json")
    validate_code()


if __name__ == "__main__":
    main()
