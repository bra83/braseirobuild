#!/usr/bin/env python3
"""R35: correct PF2e slowed/stunned action overlap on the exact R34 source.
No changes to maps, TTS, imagery, campaigns or DB schema. Fail closed on drift.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch.py <extracted_R34_project>")
root = Path(sys.argv[1]).resolve()
rules = root / "android/app/src/main/java/com/braseiro/pathfinder2e/Pathfinder2eRulesEngine.java"
version = root / "version.properties"
if not rules.is_file() or not version.is_file():
    raise RuntimeError("R34 project not found")
if "VERSION_NAME=9.0.35" not in version.read_text(encoding="utf-8"):
    raise RuntimeError("Refusing to patch anything other than canonical R34 9.0.35")
text = rules.read_text(encoding="utf-8")
changes = [
    (
        "int max=3+(quickened?1:0)-(slowed?1:0)-Math.max(0,stunned);",
        "int max=3+(quickened?1:0)-Math.max(slowed?1:0,Math.max(0,stunned));"
    ),
    (
        "int actionsBudget=canAct?Math.max(0,Math.min(4,3+(quickened?1:0)-slowed-stunned)):0;",
        "int actionsBudget=canAct?Math.max(0,Math.min(4,3+(quickened?1:0)-Math.max(slowed,stunned))):0;"
    ),
    (
        '.put("actions_lost",slowed+stunned)',
        '.put("actions_lost",Math.max(slowed,stunned))'
    )
]
for old, new in changes:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one R34 action-budget anchor, found {count}: {old}")
    text = text.replace(old, new, 1)
rules.write_text(text, encoding="utf-8")
# Bump only after behavioral patch; amend version assertions for the new release
# without removing R34 scenario tests from the full regression suite.
vtext = version.read_text(encoding="utf-8")
version.write_text(vtext.replace("VERSION_NAME=9.0.35", "VERSION_NAME=9.0.36").replace("VERSION_CODE=90035", "VERSION_CODE=90036"), encoding="utf-8")
tests = root / "tests_pf2"
changed_tests = []
for path in tests.glob("test_*.py"):
    before = path.read_text(encoding="utf-8")
    after = before.replace("VERSION_NAME=9.0.35", "VERSION_NAME=9.0.36").replace("VERSION_CODE=90035", "VERSION_CODE=90036")
    if after != before:
        path.write_text(after, encoding="utf-8")
        changed_tests.append(path.name)
print("R35_ACTION_OVERLAP_PATCH_PASS")
print("PATCHED_FILE=", rules)
print("VERSION=9.0.36")
print("VERSION_TESTS_UPDATED=", ",".join(changed_tests))
