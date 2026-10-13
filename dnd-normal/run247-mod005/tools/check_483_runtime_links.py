#!/usr/bin/env python3
"""Check every one of the 483 *structural* editorial links.

This tests whether the declared implementation source and declared JUnit class
exist, and whether the ledger remains 1:1. It is deliberately NOT proof of
book accuracy: only real source-specific behavioral evidence can certify that.
"""
import argparse
import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATALOG = "docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv"
LEDGER = "docs/EDITORIAL_SECTION_ACCEPTANCE.csv"
BOOKS = ("XMM", "XSAC", "FRHOF", "FRAIF", "EFA", "AU", "RHW", "ABH", "LFL", "NF")


def check(root=ROOT):
    root = Path(root).resolve()
    with (root / CATALOG).open(encoding="utf-8-sig", newline="") as f:
        inventory = list(csv.DictReader(f))
    with (root / LEDGER).open(encoding="utf-8-sig", newline="") as f:
        ledger = list(csv.DictReader(f))
    errors = []
    out = []
    key = lambda r: (r["book_id"], r["chapter_id"], r["section_id"])
    keys = [key(r) for r in inventory]
    ledger_keys = [key(r) for r in ledger]
    if len(keys) != 483 or len(ledger_keys) != 483:
        errors.append(f"EXPECTED_483_ROWS: inventory={len(keys)}, ledger={len(ledger_keys)}")
    if len(set(keys)) != len(keys) or set(keys) != set(ledger_keys):
        errors.append("DUPLICATE_OR_MISSING_SECTION_KEY")
    for row in inventory:
        book, chapter, section = key(row)
        runtime = row.get("implementation_path", "").strip()
        test_class = row.get("test_id", "").strip().split("#")[0]
        detail = {
            "book_id": book, "chapter_id": chapter, "section_id": section,
            "runtime_source": runtime, "declared_test_class": test_class,
        }
        problems = []
        if book not in BOOKS:
            problems.append("UNKNOWN_BOOK")
        # Never allow absolute paths or traversal outside this project.
        runtime_path = (root / runtime).resolve()
        if root not in runtime_path.parents or not runtime.endswith(".java") or not runtime_path.is_file():
            problems.append("IMPLEMENTATION_SOURCE_MISSING")
        test_root = root / "app/src/test/java"
        if not test_class or "/" in test_class or "\\" in test_class or ".." in test_class:
            problems.append("INVALID_TEST_CLASS")
        else:
            matching = list(test_root.rglob(test_class + ".java"))
            if len(matching) != 1:
                problems.append("EXPECTED_SINGLE_TEST_CLASS_FOUND_" + str(len(matching)))
        detail["issues"] = problems
        out.append(detail)
    counts = Counter(r["book_id"] for r in out)
    defects = [r for r in out if r["issues"]]
    return {
        "format": "DND_483_RUNTIME_LINKS_V1",
        "inventory_rows": len(inventory),
        "ledger_rows": len(ledger),
        "by_book": dict(counts),
        "runtime_link_issues": len(defects),
        "broken_links": defects,
        "matrix_issues": errors,
        "all_483_runtime_links_valid": not defects and not errors,
        "editorial_accuracy_certified": False,
        "reason": "Runtime class existence is not equivalent to section-specific source and behavior proof.",
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--json", action="store_true")
    p.add_argument("--out", type=Path)
    args = p.parse_args()
    result = check()
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"SECTIONS={result['inventory_rows']} LINKS_BROKEN={result['runtime_link_issues']}")
        for row in result["broken_links"][:30]:
            print(f"{row['book_id']}:{row['chapter_id']}:{row['section_id']} => {','.join(row['issues'])}")
        for e in result["matrix_issues"]:
            print("ERROR:", e)
        print("SEMANTIC_CERTIFICATION=NOT_CLAIMED")
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0 if result["all_483_runtime_links_valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
