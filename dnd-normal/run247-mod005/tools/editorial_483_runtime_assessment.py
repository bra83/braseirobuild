#!/usr/bin/env python3
"""Executable, section-by-section inventory gate for 483 D&D Normal sections.

No row is marked source-certified simply because a Java class exists or because
a broad integration test passed. Use --require-complete to enforce individual
licensed source, original behavior, and per-section XML evidence later.
"""
from __future__ import annotations

import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path
import re
import sys

import validate_editorial_coverage as existing

ROOT = Path(__file__).resolve().parent.parent
EXPECTED = {"XMM": 36, "XSAC": 14, "FRHOF": 53, "FRAIF": 151,
            "EFA": 48, "AU": 45, "RHW": 54, "ABH": 16, "LFL": 20, "NF": 46}
SECTION_FILE = "docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv"
LEDGER_FILE = "docs/EDITORIAL_SECTION_ACCEPTANCE.csv"


def rows(path: Path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def section_key(row):
    return ":".join((row.get(x) or "").strip()
                    for x in ("book_id", "chapter_id", "section_id"))


def gameplay_tier(kind, title):
    t = (title or "").strip().lower()
    if kind == "editorial_metadata" or t in {"credits", "front cover", "d&d studio team"}:
        return "EDITORIAL_ONLY"
    if kind in {"rules_mechanics", "bestiary_mechanics", "bestiary_index",
                "rules_ruling", "character_options", "magic_and_equipment",
                "regional_bestiary", "magic_items", "spells_mechanics",
                "bastions_mechanics", "airships_mechanics",
                "horror_rules_and_bestiary", "tarokka_deck_mechanics",
                "vampiric_hungers_mechanics", "mythallar_hazards",
                "species_and_rules"}:
        return "GAMEPLAY_RULE_OR_CATALOG"
    if kind in {"adventure_narrative", "official_adventure_published",
                "dm_support_tools"}:
        return "NARRATIVE_AND_CONSEQUENCES"
    return "CANON_LORE_OR_REFERENCE"


def expected_treatment(row):
    book = row["book_id"]
    ch = row["chapter_id"]
    section = row["section_id"]
    if book == "FRAIF" and ch == "ch_9":
        n = int(section.rsplit("_", 1)[-1])
        if n == 0: return "INDEX_ILLUSTRATION_CAPTION_NOT_CREATURE"
        if n == 1: return "INDEX_HEADING_NOT_CREATURE"
        if n <= 24: return "SOURCE_CREATURE_FAMILY_HEADING"
        return "EXACT_FRAIF_CREATURE_STATBLOCK_REQUIRED"
    if book == "FRAIF" and ch == "ch_8":
        return ("INDEX_ILLUSTRATION_CAPTION_NOT_ITEM" if section == "sec_8_0"
                else "EXACT_FRAIF_MAGIC_ITEM_EFFECTS_REQUIRED")
    if book == "AU" and ch == "ch_7":
        return "AU_STANDALONE_ADVENTURE_GRAPH_NOT_YET_SOURCE_CERTIFIED"
    if book == "AU" and ch == "ch_8":
        return "EDITORIAL_ONLY_NO_RULE_IMPLEMENTATION_REQUIRED"
    return "PROVE_SPECIFIC_BEHAVIOR_AGAINST_AUTHORIZED_SOURCE"


def run(root: Path):
    matrix = rows(root / SECTION_FILE)
    ledger = rows(root / LEDGER_FILE)
    by_matrix = {section_key(x): x for x in matrix}
    by_ledger = {section_key(x): x for x in ledger}
    problems = []
    if len(matrix) != 483 or len(ledger) != 483:
        problems.append(f"EXPECTED_483_INVENTORY_ROWS:{len(matrix)}:{len(ledger)}")
    if len(by_matrix) != len(matrix) or len(by_ledger) != len(ledger):
        problems.append("DUPLICATE_SECTION_KEYS")
    if set(by_matrix) != set(by_ledger):
        problems.append("MATRIX_LEDGER_KEYS_DIFFER")

    entries = []
    by_book = Counter()
    by_tier = Counter()
    assessment = Counter()
    shared_test = Counter()
    java_cache = {}
    test_cache = {}
    for key in sorted(set(by_matrix) | set(by_ledger)):
        mat, led = by_matrix.get(key), by_ledger.get(key)
        if mat is None or led is None:
            problems.append("UNPAIRED_SECTION:" + key)
            continue
        book, kind, title = mat["book_id"], mat["content_type"], mat["section_title"]
        by_book[book] += 1
        tier = gameplay_tier(kind, title)
        by_tier[tier] += 1
        relative = mat.get("implementation_path", "")
        runtime_path = existing.within_project(root, relative)
        symbol = led.get("runtime_symbol", "")
        fqcn = (
            "app/src/main/java/" + symbol.replace(".", "/") + ".java"
            if re.fullmatch(r"(?:[A-Za-z_]\w*\.)+[A-Za-z_]\w*", symbol or "")
            else ""
        )
        symbol_path = existing.within_project(root, fqcn)
        source_found = bool(runtime_path and runtime_path.is_file()
                            and symbol_path and symbol_path.is_file()
                            and runtime_path == symbol_path)
        if not source_found:
            problems.append("MISSING_OR_MISROUTED_RUNTIME:" + key)

        assertion = led.get("assertion_id", "")
        cls, sep, method = assertion.partition("#")
        test_dir = root / "app/src/test/java"
        if cls not in test_cache:
            matches = list(test_dir.rglob(cls + ".java")) if sep and cls else []
            test_cache[cls] = matches[0] if len(matches) == 1 else None
        tp = test_cache[cls]
        declared = bool(tp and existing.test_method_present(tp, method))
        if not declared:
            problems.append("MISSING_DECLARED_TEST_METHOD:" + key)
        shared_test[assertion] += 1

        specific = False
        if declared:
            if tp not in java_cache:
                java_cache[tp] = tp.read_text(encoding="utf-8", errors="replace")
            # Source string in a method may mention a section by ID or full key.
            source = java_cache[tp]
            specific = ('"' + mat["section_id"] + '"' in source
                        or '"' + key + '"' in source)

        ver = (led.get("source_version") or "").strip()
        rights = (led.get("rights_basis") or "").strip()
        status = (led.get("status") or "").strip()
        certified = status == "IMPLEMENTED_AND_TESTED"
        source_ready = bool(ver and rights
                            and ver.upper() not in existing.PLACEHOLDERS
                            and rights.upper() not in existing.PLACEHOLDERS)
        assessment["SEMANTIC_CERTIFIED" if certified else "SEMANTIC_NOT_CERTIFIED"] += 1
        if not source_ready:
            assessment["SOURCE_OR_RIGHTS_UNVERIFIED"] += 1
        if not specific:
            assessment["NO_UNIQUE_TEST_ASSERTION_FOR_SECTION"] += 1
        if source_found:
            assessment["RUNTIME_CLASS_PATH_LINKED"] += 1
        if declared:
            assessment["TEST_METHOD_DECLARED"] += 1

        item = {
            "key": key,
            "book_id": book,
            "title": title,
            "kind": kind,
            "tier": tier,
            "runtime_class": symbol,
            "source_path_present": source_found,
            "assertion_id": assertion,
            "test_method_declared": declared,
            "individual_assertion_linked": specific,
            "source_version": ver,
            "rights_basis": rights,
            "semantic_certification": ("CERTIFIED" if certified else "PENDING"),
            "source_rights_verified": source_ready,
            "next_gate": expected_treatment(mat)
        }
        entries.append(item)

    for book, count in EXPECTED.items():
        if by_book[book] != count:
            problems.append(f"BOOK_SECTION_COUNT:{book}:{by_book[book]}:{count}")
    semantic_gate = existing.inspect(root)
    problems.extend("INVENTORY_GATE:" + e for e in semantic_gate["errors"])

    # The root includes 483 distinct sections, but they are NOT all game rules.
    return {
        "format": "BRASEIRO_DND_EDITORIAL_483_SECTION_ASSESSMENT_V1",
        "matrix_sha256": hashlib.sha256((root / SECTION_FILE).read_bytes()).hexdigest(),
        "ledger_sha256": hashlib.sha256((root / LEDGER_FILE).read_bytes()).hexdigest(),
        "sections_evaluated": len(entries),
        "by_book": dict(sorted(by_book.items())),
        "by_tier": dict(sorted(by_tier.items())),
        "structural_evidence": dict(sorted(assessment.items())),
        "individual_test_methods_shared": dict(sorted(shared_test.items())),
        "source_verified_and_semantic_passed": semantic_gate["certified"],
        "pending_semantic_certification": semantic_gate["pending"],
        "structural_gate_pass": not problems,
        "problems": problems,
        "sections": entries
    }


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args(argv)
    report = run(args.root.resolve())
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                               encoding="utf-8")
    if args.json:
        # Full 483-row records go to --output, keep CI logs bounded.
        print(json.dumps({k: v for k, v in report.items()
                          if k not in {"sections", "individual_test_methods_shared"}},
                         ensure_ascii=False, sort_keys=True))
    else:
        print(f"DND_483_STRUCTURAL={'PASS' if report['structural_gate_pass'] else 'FAIL'}"
              f"; ROWS={report['sections_evaluated']}"
              f"; CERTIFIED={report['source_verified_and_semantic_passed']}"
              f"; PENDING={report['pending_semantic_certification']}")
    if not report["structural_gate_pass"]:
        return 1
    if args.require_complete and report["pending_semantic_certification"]:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
