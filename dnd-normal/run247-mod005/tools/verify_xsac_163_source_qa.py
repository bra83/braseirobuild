#!/usr/bin/env python3
"""XSAC stage-02 gate: pin and pair 163 actual Q/A bodies with 163 runtime cases.

This is a source-index/structural gate, NOT certification of behavioral fidelity.
DM final authority is a separate policy endpoint, never counted as a Q/A.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
CSV = Path("docs/XSAC_SOURCE_QA_TRACE_163.csv")
JAVA = Path("app/src/test/java/com/braseiro/dnd5e/SageAdvice163RuntimeContractTest.java")
ENGINE = Path("app/src/main/java/com/braseiro/dnd5e/SageAdvice2025RulesEngine.java")
BLOB = "b99ffecf35c09ad8efc5f3b1ab6b3df2b6562a06"
REV = "8c026b807fac21862a309379a0c3228a0683198b"
SOURCE = "https://raw.githubusercontent.com/5etools-mirror-3/5etools-src/" + REV + "/data/book/book-xsac.json"
CATS = {
    3: "ability_checks", 4: "adventuring", 5: "combat", 6: "multiclassing",
    7: "class_features", 8: "backgrounds", 9: "species_traits",
    10: "feats", 11: "equipment", 12: "spellcasting",
    13: "monsters", 14: "magic_items",
}
PATTERN = re.compile(r'\{"([a-z0-9_]+)",\s*"([a-z_]+)",\s*"(DETERMINISTIC|MANUAL_DM)"\}')

def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\\0" + data).hexdigest()

def pointer(data, path):
    if not path.startswith("/bookData/"):
        raise ValueError("NOT_A_BOOK_POINTER")
    cur = data
    for part in path[1:].split("/"):
        part = part.replace("~1", "/").replace("~0", "~")
        if isinstance(cur, list):
            if not part.isdecimal() or int(part) >= len(cur):
                raise ValueError("BAD_INDEX:" + path)
            cur = cur[int(part)]
        elif isinstance(cur, dict):
            if part not in cur:
                raise ValueError("BAD_KEY:" + path)
            cur = cur[part]
        else:
            raise ValueError("SCALAR_POINTER:" + path)
    return cur

def nested_name(entries):
    return any(isinstance(x, dict) and (
        bool(x.get("name")) or nested_name(x.get("entries", [])))
        for x in entries)

def collect(node, at, category, section_id):
    if isinstance(node, list):
        out = []
        for i, sub in enumerate(node):
            out.extend(collect(sub, at + "/" + str(i), category, section_id))
        return out
    if not isinstance(node, dict):
        return []
    entries = node.get("entries", [])
    if node.get("name") and not nested_name(entries):
        return [(at, category, section_id, str(node.get("id", "")), len(entries))]
    out = []
    for i, sub in enumerate(entries):
        out.extend(collect(sub, at + "/entries/" + str(i), category, section_id))
    return out

def verified_rows(project: Path, source_bytes: bytes):
    if git_blob_sha(source_bytes) != BLOB:
        raise ValueError("PINNED_XSAC_GIT_BLOB_MISMATCH")
    doc = json.loads(source_bytes.decode("utf-8-sig"))
    root = doc["bookData"][0]["data"][0]["entries"]
    source_entries = []
    for section_idx, category in CATS.items():
        source_entries.extend(collect(root[section_idx],
            f"/bookData/0/data/0/entries/{section_idx}",
            category, "sec_0_" + str(section_idx - 1)))
    with (project / CSV).open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))
    java_source = (project / JAVA).read_text(encoding="utf-8")
    engine = (project / ENGINE).read_text(encoding="utf-8")
    cases = PATTERN.findall(java_source)
    if len(rows) != 163 or len(source_entries) != 163 or len(cases) != 163:
        raise ValueError(f"XSAC_NOT_163_ROWS:{len(rows)}:{len(source_entries)}:{len(cases)}")
    if len({r["rule_id"] for r in rows}) != 163 or len({r["source_json_pointer"] for r in rows}) != 163:
        raise ValueError("XSAC_DUPLICATE_ID_OR_QA_POINTER")
    for i, row in enumerate(rows):
        expected_pointer, category, section, source_id, segments = source_entries[i]
        case_id, case_category, case_mode = cases[i]
        if row["source_json_pointer"] != expected_pointer or row["category"] != category or row["section_id"] != section:
            raise ValueError("XSAC_SOURCE_QA_MAPPING_INVALID:" + str(i + 1))
        if row["rule_id"] != case_id or row["category"] != case_category or row["resolution_mode"] != case_mode:
            raise ValueError("XSAC_RUNTIME_QA_MAPPING_INVALID:" + str(i + 1))
        if row["source_qa_id"] != source_id or int(row["answer_segments"]) != segments:
            raise ValueError("XSAC_QA_CONTENT_METADATA_MISMATCH:" + str(i + 1))
        if row["pinned_source_commit"] != REV or row["pinned_source_blob_sha"] != BLOB:
            raise ValueError("XSAC_UNPINNED_QA:" + str(i + 1))
        if row["semantic_certification"] != "PENDING_INDIVIDUAL_BEHAVIOR_PROOF":
            raise ValueError("XSAC_UNPROVEN_SEMANTIC_CERTIFICATION:" + str(i + 1))
        obj = pointer(doc, expected_pointer)
        if not isinstance(obj, dict) or not isinstance(obj.get("name"), str) or not obj.get("entries"):
            raise ValueError("XSAC_EMPTY_QA_BODY:" + str(i + 1))
        if not re.search(r'case "' + re.escape(case_id) + r'":\s*return\s+(ok|manual)\(', engine):
            raise ValueError("XSAC_RUNTIME_CASE_MISSING:" + case_id)
    if '"dm_final_say", "rules_role_meta"' in java_source:
        raise ValueError("XSAC_POLICY_MISTAKEN_FOR_QA")
    if 'case "dm_final_say"' not in engine:
        raise ValueError("XSAC_DM_POLICY_ENDPOINT_MISSING")
    from collections import Counter
    return {
        "format": "XSAC_PINNED_163_QA_TRACE_V1",
        "source_commit": REV, "source_git_blob_sha1": BLOB,
        "official_qa_count": len(rows),
        "unique_source_qa_pointers": len({r["source_json_pointer"] for r in rows}),
        "runtime_case_count": len(cases),
        "sections": 14,
        "category_counts": dict(sorted(Counter(r["category"] for r in rows).items())),
        "source_index_gate_pass": True,
        "gameplay_semantics_certified": False,
        "note": "Full source-body pin and original Q/A order; behavioral fidelity still requires independently witnessed tests.",
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", type=Path, default=ROOT)
    parser.add_argument("--source-dir", type=Path)
    parser.add_argument("--json-out", type=Path)
    opt = parser.parse_args()
    if opt.source_dir:
        source_data = (opt.source_dir / "book-xsac.json").read_bytes()
    else:
        req = Request(SOURCE, headers={"User-Agent": "Braseiro-XSAC-163-Source-Check/1.0"})
        with urlopen(req, timeout=60) as stream:
            source_data = stream.read(8 * 1024 * 1024 + 1)
        if len(source_data) > 8 * 1024 * 1024:
            raise ValueError("XSAC_SOURCE_TOO_LARGE")
    result = verified_rows(opt.project_root.resolve(), source_data)
    payload = json.dumps(result, ensure_ascii=False, indent=2)
    print(payload)
    if opt.json_out:
        opt.json_out.parent.mkdir(parents=True, exist_ok=True)
        opt.json_out.write_text(payload + "\\n", encoding="utf-8")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
