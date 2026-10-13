#!/usr/bin/env python3
"""Reproduce source provenance of 483 mapped D&D entries against 10 pinned JSON blobs.

Only proves origin/locator/type, never that implementation matches source rules.
No source prose is written to the repository, logs, or QA output.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
import re
import sys
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
MATRIX = Path("docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv")
ACCEPTANCE = Path("docs/EDITORIAL_SECTION_ACCEPTANCE.csv")
PROVENANCE = Path("docs/EDITORIAL_SOURCE_PROVENANCE_483.csv")
SOURCE_REPOSITORY = "5etools-mirror-3/5etools-src"
SOURCE_COMMIT = "8c026b807fac21862a309379a0c3228a0683198b"
BOOKS = {"XMM", "XSAC", "FRHOF", "FRAIF", "EFA", "AU", "RHW", "ABH", "LFL", "NF"}


def normalized(value: str) -> str:
    # Same conservative matching used for the verified title-only source map:
    # accent folding + punctuation/whitespace normalization, NOT semantic paraphrase.
    import unicodedata
    value = unicodedata.normalize("NFKD", value.lower())
    value = "".join(c for c in value if not unicodedata.combining(c))
    return " ".join(re.findall(r"[a-z0-9]+", value))


def git_blob_sha1(blob: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(blob)).encode("ascii") + b"\0" + blob).hexdigest()


def json_pointer(root, pointer: str):
    if not pointer.startswith("/"):
        raise ValueError("JSON_POINTER_INVALID")
    node = root
    for part in pointer[1:].split("/"):
        part = part.replace("~1", "/").replace("~0", "~")
        if isinstance(node, list):
            if not part.isdecimal() or int(part) >= len(node):
                raise ValueError("JSON_POINTER_ARRAY_INDEX_INVALID")
            node = node[int(part)]
        elif isinstance(node, dict):
            if part not in node:
                raise ValueError("JSON_POINTER_KEY_MISSING")
            node = node[part]
        else:
            raise ValueError("JSON_POINTER_CROSSES_SCALAR")
    return node


def load_csv(path: Path):
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def verify(project: Path, source_dir: Path | None = None) -> dict:
    matrix = load_csv(project / MATRIX)
    ledger = load_csv(project / ACCEPTANCE)
    entries = load_csv(project / PROVENANCE)
    if len(matrix) != 483 or len(ledger) != 483 or len(entries) != 483:
        raise ValueError("SOURCE_MATRIX_MUST_HAVE_EXACTLY_483_ROWS")
    key = lambda row: (row["book_id"], row["chapter_id"], row["section_id"])
    mapped = {key(row): row for row in matrix}
    accepted = {key(row): row for row in ledger}
    if len(mapped) != 483 or len(accepted) != 483 or len({key(x) for x in entries}) != 483:
        raise ValueError("DUPLICATE_SECTION_IDS")
    if set(mapped) != set(accepted) or set(mapped) != {key(x) for x in entries}:
        raise ValueError("MATRIX_LEDGER_SOURCE_KEYS_DIVERGE")

    files = {}
    sha256 = {}
    for book in sorted(BOOKS):
        filename = f"book-{book.lower()}.json"
        provenance = [r for r in entries if r["book_id"] == book]
        if not provenance:
            raise ValueError("SOURCE_BOOK_MISSING:" + book)
        expected_sha_set = {r["source_git_blob_sha1"] for r in provenance}
        if len(expected_sha_set) != 1:
            raise ValueError("MULTIPLE_BLOB_SHAS:" + book)
        if source_dir is None:
            url = f"https://raw.githubusercontent.com/{SOURCE_REPOSITORY}/{SOURCE_COMMIT}/data/book/{filename}"
            request = Request(url, headers={"User-Agent": "Braseiro-Source-Provenance-Verifier/1.0"})
            with urlopen(request, timeout=55) as stream:
                data = stream.read(8 * 1024 * 1024 + 1)
            if len(data) > 8 * 1024 * 1024:
                raise ValueError("SOURCE_EXCEEDS_SAFETY_SIZE:" + book)
        else:
            data = (source_dir / filename).read_bytes()
        actual = git_blob_sha1(data)
        expected = next(iter(expected_sha_set))
        if actual != expected:
            raise ValueError(f"SOURCE_BLOB_HASH_MISMATCH:{book}:{actual}!={expected}")
        files[book] = json.loads(data.decode("utf-8-sig"))
        sha256[book] = hashlib.sha256(data).hexdigest()

    types = Counter()
    counts = Counter()
    problems = []
    for row in entries:
        k = key(row)
        book = row["book_id"]
        if book not in BOOKS:
            problems.append((k, "INVALID_BOOK"))
            continue
        if row["source_repository"] != SOURCE_REPOSITORY or row["source_commit"] != SOURCE_COMMIT:
            problems.append((k, "UNPINNED_SOURCE"))
            continue
        if row["source_file"] != f"data/book/book-{book.lower()}.json":
            problems.append((k, "WRONG_SOURCE_FILE"))
            continue
        pointer = row["json_pointer_to_exact_title"]
        field = pointer.rsplit("/", 1)[-1]
        if field not in ("name", "title") or field != row["source_field"]:
            problems.append((k, "INVALID_NAME_OR_TITLE_FIELD"))
            continue
        try:
            title = json_pointer(files[book], pointer)
            parent = json_pointer(files[book], pointer.rsplit("/", 1)[0])
        except (ValueError, KeyError, IndexError) as error:
            problems.append((k, str(error)))
            continue
        if not isinstance(title, str) or not isinstance(parent, dict):
            problems.append((k, "NOT_A_STRING_ENTRY"))
            continue
        if normalized(title) != normalized(mapped[k]["section_title"]):
            problems.append((k, "SOURCE_TITLE_MISMATCH"))
            continue
        source_type = parent.get("type", "unknown")
        if source_type != row["source_entry_type"]:
            problems.append((k, "SOURCE_ENTRY_TYPE_MISMATCH"))
            continue
        chapter = k[1].replace("ch_", "")
        if not chapter.isdecimal() or f"/data/{int(chapter)}/" not in pointer:
            problems.append((k, "SOURCE_CHAPTER_MISMATCH"))
            continue
        if source_type == "image":
            if mapped[k]["content_type"] != "illustration_caption" or accepted[k]["status"] != "NOT_APPLICABLE":
                problems.append((k, "IMAGE_NOT_EXCLUDED_FROM_RULES"))
                continue
        else:
            if mapped[k]["content_type"] == "illustration_caption" or accepted[k]["status"] == "NOT_APPLICABLE":
                problems.append((k, "NONIMAGE_WRONGLY_EXCLUDED"))
                continue
        counts[book] += 1
        types[source_type] += 1

    if problems:
        raise ValueError("SOURCE_POINTERS_FAILED: " + json.dumps(problems[:20], ensure_ascii=False))
    if sum(counts.values()) != 483 or types["image"] != 46:
        raise ValueError("SOURCE_PROVENANCE_INCOMPLETE_OR_IMAGE_COUNT_CHANGED")
    return {
        "format": "DND_PINNED_SOURCE_VERIFICATION_V1",
        "source_repository": SOURCE_REPOSITORY,
        "source_commit": SOURCE_COMMIT,
        "books_verified": len(counts),
        "book_json_sha256": sha256,
        "exact_chapter_title_pointers_verified": sum(counts.values()),
        "counts_by_book": dict(counts),
        "counts_by_type": dict(types),
        "non_rule_illustrations": types["image"],
        "source_title_provenance_passed": True,
        "semantics_against_original_publisher_verified": False,
        "game_runtime_semantics_certified": False,
        "note": "Unofficial community mirror, not WotC ownership, license review, or mechanical-equivalence certification.",
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--project-root", type=Path, default=ROOT)
    p.add_argument("--source-dir", type=Path, default=None)
    p.add_argument("--json-out", type=Path, default=None)
    args = p.parse_args()
    try:
        report = verify(args.project_root, args.source_dir)
    except (OSError, ValueError, UnicodeError, json.JSONDecodeError) as error:
        print(f"DND_483_SOURCE_VERIFICATION_FAILED: {error}", file=sys.stderr)
        return 1
    payload = json.dumps(report, indent=2, ensure_ascii=False)
    print(payload)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(payload + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
