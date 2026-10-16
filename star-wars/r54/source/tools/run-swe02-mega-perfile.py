"""Run every Node test file individually without overwriting historical evidence.

Usage: python tools/run-swe02-mega-perfile.py [output-directory]
"""
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
import json
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
TESTS = sorted((ROOT / "tests").glob("*.test.mjs"))
DEFAULT_OUTPUT = ROOT / "evidence" / f"FRESH_REGRESSION_{datetime.now(timezone.utc):%Y%m%dT%H%M%SZ}"
DEST = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_OUTPUT
DEST.mkdir(parents=True, exist_ok=False)
STRESS = {
    "stress-persistent-adversary-r17.test.mjs",
    "stress-factions-r18.test.mjs",
    "stress-33-faction-plan-r19.test.mjs",
    "stress-faction-plans-r19.test.mjs",
    "stress-33-aftermath-r20.test.mjs",
    "stress-33-sessions-r15.test.mjs",
}
SUMMARY_KEYS = {"tests", "pass", "fail", "skipped"}


def parse_counts(output):
    counts = {}
    for line in output.splitlines():
        fields = line.strip().split()
        if len(fields) < 3:
            continue
        key, value = fields[-2:]
        if key in SUMMARY_KEYS and value.isdecimal():
            counts[key] = int(value)
    return {
        "tests": counts.get("tests"),
        "pass": counts.get("pass"),
        "fail": counts.get("fail"),
        "skip": counts.get("skipped"),
    }


def run(test_file):
    try:
        result = subprocess.run(
            ["node", "--test", str(test_file)],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=180,
        )
        output = (result.stdout or "") + "\n" + (result.stderr or "")
        (DEST / f"{test_file.stem}.tap").write_text(output, encoding="utf-8")
        return {
            "file": test_file.name,
            "stress": test_file.name in STRESS,
            "exit": result.returncode,
            **parse_counts(output),
        }
    except Exception as error:
        message = f"runner exception: {type(error).__name__}: {error}\n"
        (DEST / f"{test_file.stem}.tap").write_text(message, encoding="utf-8")
        return {
            "file": test_file.name,
            "stress": test_file.name in STRESS,
            "exit": -1,
            "error": message.strip(),
            "tests": None,
            "pass": None,
            "fail": None,
            "skip": None,
        }


def is_bad(result):
    return (
        result["exit"] != 0
        or result.get("fail") is None
        or result.get("fail", 1) > 0
        or result.get("pass") is None
    )


def main():
    started = time.monotonic()
    results = []
    with ThreadPoolExecutor(max_workers=8) as executor:
        futures = [executor.submit(run, test_file) for test_file in TESTS]
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            if is_bad(result):
                print("FAIL", json.dumps(result, ensure_ascii=True), flush=True)
            if len(results) % 40 == 0:
                print(f"PROGRESS {len(results)} / {len(TESTS)}", flush=True)

    summary = {
        "scope": "FRESH_PER_FILE_REGRESSION",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "project": str(ROOT),
        "log_dir": str(DEST),
        "files": len(TESTS),
        "regular_files": len(TESTS) - len(STRESS),
        "stress_files": len(STRESS),
        "tests": sum(item.get("tests") or 0 for item in results),
        "pass": sum(item.get("pass") or 0 for item in results),
        "fail": sum(item.get("fail") or 0 for item in results),
        "skip": sum(item.get("skip") or 0 for item in results),
        "failed_files": [item for item in results if is_bad(item)],
        "files_without_complete_summary": [
            item["file"] for item in results
            if any(item.get(key) is None for key in ("tests", "pass", "fail", "skip"))
        ],
        "duration_seconds": round(time.monotonic() - started, 2),
        "results": sorted(results, key=lambda item: item["file"]),
    }
    (DEST / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=True, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({key: value for key, value in summary.items() if key != "results"}, indent=2))
    return 1 if summary["failed_files"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
