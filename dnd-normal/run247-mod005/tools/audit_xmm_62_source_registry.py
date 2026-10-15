#!/usr/bin/env python3
"""Fail-closed XMM inventory-to-Java source-pointer identity audit (lots 04-13).

This checks EXACT pointer membership, count, duplicate entry policy and that
the inventory still calls these effects MANUAL_UNSUPPORTED. A green status
never implies source-to-code mechanical semantics, Android, timers, or SQLite.

Sources:
  docs/editorial-evidence/xmm-condition-choice-preview-local-20261015/
  manual-effect-inventory-236.json
  app/src/main/java/com/braseiro/dnd5e/Xmm*.java
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / (
    "docs/editorial-evidence/"
    "xmm-condition-choice-preview-local-20261015/"
    "manual-effect-inventory-236.json"
)
JAVA = ROOT / "app/src/main/java/com/braseiro/dnd5e"
PATTERN = re.compile(r'"/monster/\d+/(?:action|bonus|reaction|legendary)/\d+/entries/\d+"')
LOTS = (
    ("04", "damage_or_save_branch", 36, 48, "XmmDamageOrSaveBranch04.java"),
    ("05", "damage_or_save_branch", 48, 60, "XmmDamageOrSaveBranch05.java"),
    ("06", "damage_or_save_branch", 60, 61, "XmmDamageOrSaveBranch06.java"),
    ("07", "other_source_effect", 0, 10, "XmmOtherSourceEffect01.java"),
    ("08", "other_source_effect", 10, 20, "XmmOtherSourceEffect02.java"),
    ("09", "other_source_effect", 20, 29, "XmmOtherSourceEffect03.java"),
    ("10", "attack_hit_condition", 0, 4, "XmmAttackHitCondition01.java"),
    ("11", "swallow_and_escape", 0, 2, "XmmSwallowAndEscape01.java"),
    ("12", "explicit_effect_choice", 0, 1, "XmmExplicitEffectChoice01.java"),
    ("13", "monster_spell_action", 0, 1, "XmmMonsterSpellAction01.java"),
)
COUNTS = {
    "damage_or_save_branch": 61,
    "other_source_effect": 29,
    "attack_hit_condition": 4,
    "swallow_and_escape": 2,
    "explicit_effect_choice": 1,
    "monster_spell_action": 1,
}
def fail(code, context=None):
    raise SystemExit("XMM_SOURCE_POINTER_REGISTRY=FAIL "
                     + code + (" " + str(context) if context else ""))
def main():
    if not INVENTORY.is_file():
        fail("INVENTORY_ABSENT", INVENTORY)
    doc = json.loads(INVENTORY.read_text(encoding="utf-8"))
    records = doc["entries"]
    by_family = {}
    for family, expected in COUNTS.items():
        rows = [
            row for row in records if row.get("family") == family
            and row.get("coverage_status") == "MANUAL_UNSUPPORTED"
        ]
        rows.sort(key=lambda row: tuple(
            str(row.get(k, "")) for k in
            ("monster_id", "action_id", "source_pointer")))
        if len(rows) != expected:
            fail("INVENTORY_FAMILY_COUNT_DRIFT", (family, len(rows), expected))
        pointers = [row["source_pointer"] for row in rows]
        if len(set(pointers)) != len(pointers):
            fail("INVENTORY_DUPLICATE_POINTER", family)
        by_family[family] = pointers
    report = []
    union = set()
    for num, family, start, stop, filename in LOTS:
        if stop > len(by_family[family]):
            fail("INVENTORY_LOT_SLICE_OUT_OF_RANGE", num)
        expected = set(by_family[family][start:stop])
        source = JAVA / filename
        if not source.is_file():
            fail("MISSING_JAVA_SOURCE", filename)
        contents = source.read_text(encoding="utf-8")
        found_raw = [m[1:-1] for m in PATTERN.findall(contents)]
        found = set(found_raw)
        if len(found) != len(found_raw):
            fail("DUPLICATE_JAVA_POINTER", num)
        if found != expected:
            fail("POINTER_SET_MISMATCH", json.dumps({
                "lot": num,
                "missing": sorted(expected - found),
                "unexpected": sorted(found - expected),
            }, sort_keys=True))
        if union.intersection(found):
            fail("CROSS_LOT_DUPLICATE_POINTER", num)
        union.update(found)
        report.append({"lot": num, "family": family,
                       "pointer_count": len(found), "source_file": filename,
                       "identity_check": "PASS"})
        print("XMM_LOT_" + num + "_POINTER_IDENTITIES=PASS count=" +
              str(len(found)), flush=True)
    if len(union) != 62:
        fail("TOTAL_POINTER_COUNT_MISMATCH", len(union))
    print("XMM_62_SOURCE_POINTER_IDENTITIES=PASS")
    print("XMM_REGISTRY_REPORT=" + json.dumps({
        "scope": "source_pointer_identity_only",
        "actions": 62,
        "lots": report,
        "inventory_other_36_damage_branch": "UNVERIFIED_BY_THIS_GATE",
        "manual_unsupported_98_actions": "NOT_CERTIFIED_BY_THIS_GATE",
        "semantics_verified": False,
        "official_pdf_parity_verified": False,
        "android_combat_integrated": False,
        "sqlite_persistence_integrated": False,
    }, ensure_ascii=False, sort_keys=True))
if __name__ == "__main__":
    main()
