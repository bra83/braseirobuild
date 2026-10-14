#!/usr/bin/env python3
"""Fix a test-only JVM JSON parser whose numeric ternary widens Long to Double.

Java numeric conditional promotion makes decimal ? Double.valueOf(raw) :
Long.valueOf(raw) return a boxed Double in BOTH branches. Numeric IDs then
round, break duplicate checks, and diverge from Android org.json behavior.
Patch only the temporary extracted R36 project (VERSION_NAME=9.0.37).
"""
from pathlib import Path
import sys

REL = Path("tools/runtime_json/org/json/JSONObject.java")
OLD = "return decimal?Double.valueOf(raw):Long.valueOf(raw);"
NEW = "if(decimal)return Double.valueOf(raw);return Long.valueOf(raw);"


def patch(root):
    root = Path(root).resolve()
    version = root / "version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("R36 temporary project required; immutable R35 cannot be patched")
    path = root / REL
    content = path.read_text(encoding="utf-8")
    if content.count(OLD) != 1 or NEW in content:
        raise RuntimeError("JVM runtime JSON integer parser changed; inspect before modifying")
    path.write_text(content.replace(OLD, NEW), encoding="utf-8")
    print("R36_JVM_JSON_INTEGRAL_LONG_SEMANTICS=PATCHED", path)
    return path


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_runtime_json_integer.py <temporary_r36_project>")
    patch(sys.argv[1])
