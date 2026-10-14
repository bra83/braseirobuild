#!/usr/bin/env python3
"""Fix Windows cp1252 decoding of UTF-8 JVM stress harness stdout.

The R36 BRELGA Java environment emits UTF-8. Two historical pytest suites
use subprocess.run(text=True) without explicit encoding; Python on Windows
decodes using cp1252 and can return stdout=None after a reader-thread
UnicodeDecodeError. Add explicit decoding ONLY to the ephemeral R36 copy.
No assertion, Java game code, R35 release, or test is removed.
"""
from pathlib import Path
import ast
import sys

TESTS = (
    "tests_pf2/test_kingmaker_challenger_final.py",
    "tests_pf2/test_kingmaker_m1_transactional_stress.py",
)
OLD = (
    "subprocess.run(compile_cmd, capture_output=True, text=True)",
    "subprocess.run(run_cmd, capture_output=True, text=True)",
)
NEW = (
    'subprocess.run(compile_cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")',
    'subprocess.run(run_cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")',
)


def prepare(root):
    root = Path(root).resolve()
    version = root / "version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("R36 disposable project only; immutable R35 must remain unchanged.")
    changes = {}
    for rel in TESTS:
        path = root / rel
        original = path.read_text(encoding="utf-8")
        revised = original
        for before, after in zip(OLD, NEW):
            if revised.count(before) != 1 or after in revised:
                raise RuntimeError("Historical JVM subprocess anchor changed: " + rel)
            revised = revised.replace(before, after, 1)
        ast.parse(revised, filename=str(path))
        roundtrip = revised
        for before, after in zip(OLD, NEW):
            roundtrip = roundtrip.replace(after, before, 1)
        if roundtrip != original:
            raise RuntimeError("A JVM assertion or unrelated source changed: " + rel)
        changes[path] = revised
    return changes


def patch(root):
    changes = prepare(root)
    for path, revised in changes.items():
        path.write_text(revised, encoding="utf-8")
        print("R36_JVM_STRESS_SUBPROCESS_UTF8_FIXED", path)
    print("R36_JVM_STRESS_ORIGINAL_ASSERTIONS_PRESERVED=true")
    return changes


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_windows_harness_utf8.py <disposable_R36_project>")
    patch(sys.argv[1])
