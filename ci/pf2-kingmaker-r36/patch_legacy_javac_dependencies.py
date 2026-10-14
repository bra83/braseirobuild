#!/usr/bin/env python3
"""Extend four historical R35 JVM harness compile lists for the R36 engine.

R36's Pathfinder2eRulesEngine and KingmakerRulesEngine now reference four
additional production classes. Historical harnesses use explicit javac source
lists. Do not weaken, skip, or rewrite their assertions: add real Java sources
in the disposable R36 extracted tree only. Keep the canonical R35 immutable.
"""
from pathlib import Path
import ast
import sys

DEPENDENCIES = (
    "KingmakerStructureSemantics.java",
    "KingmakerCampaignGate.java",
    "KingmakerCompanionSemantics.java",
    "KingmakerLeadershipSemantics.java",
)
TESTS = {
    "tests_pf2/test_kingmaker_activities_and_structures.py":
        '            str(SRC / "Pathfinder2eRulesEngine.java"),',
    "tests_pf2/test_kingmaker_mechanics_complete.py":
        '        str(SRC / "Pathfinder2eRulesEngine.java"),',
    "tests_pf2/test_kingmaker_warfare.py":
        '        str(SRC / "Pathfinder2eRulesEngine.java"),',
    "tests_pf2/test_r35_action_overlap.py":
        '        SRC / "Pathfinder2eRulesEngine.java",',
}
JAVA_SOURCE = Path("android/app/src/main/java/com/braseiro/pathfinder2e")


def prepare(root):
    root = Path(root).resolve()
    marker = root / "version.properties"
    if not marker.is_file() or "VERSION_NAME=9.0.37" not in marker.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("R36 disposable project only; never patch canonical R35.")
    for dep in DEPENDENCIES:
        if not (root / JAVA_SOURCE / dep).is_file():
            raise RuntimeError("Production Java dependency not found: " + dep)
    changes = {}
    for relative, anchor in TESTS.items():
        target = root / relative
        original = target.read_text(encoding="utf-8")
        if original.count(anchor) != 1:
            raise RuntimeError("Historic JVM harness source anchor changed: " + relative)
        indent = anchor[:len(anchor) - len(anchor.lstrip())]
        quoted = "str(SRC /" in anchor
        insertion = "".join(
            indent + ('str(SRC / "' if quoted else 'SRC / "') + name + '"),\n'
            for name in DEPENDENCIES
        )
        revised = original.replace(anchor, anchor + "\n" + insertion.rstrip("\n"), 1)
        ast.parse(revised, filename=str(target))
        if any(revised.count(name) != 1 for name in DEPENDENCIES):
            raise RuntimeError("Incorrect dependency injection: " + relative)
        if revised.replace(anchor + "\n" + insertion.rstrip("\n"), anchor, 1) != original:
            raise RuntimeError("Harness assertions unexpectedly changed: " + relative)
        changes[target] = revised
    return changes


def patch(root):
    changes = prepare(root)
    for target, content in changes.items():
        target.write_text(content, encoding="utf-8")
        print("R36_LEGACY_JAVAC_REAL_DEPENDENCIES_PATCHED", target)
    print("R36_OLD_JVM_HARNESSES=4; ORIGINAL_ASSERTIONS_PRESERVED=true")
    return changes


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_legacy_javac_dependencies.py <disposable_R36_project>")
    patch(sys.argv[1])
