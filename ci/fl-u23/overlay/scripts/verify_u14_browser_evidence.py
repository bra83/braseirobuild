#!/usr/bin/env python3
"""Fail-closed validation of actual Chromium harness output.

This script must never be treated as substitute for running the browser:
it only accepts JSON artifacts written by the actual harness during this run.
"""
import json
import sys
from pathlib import Path

# Each expected output is emitted by a separate production-UI or service harness.
# Stage 2 follow-ups and Android remain outside these gates.
GATES = {
    "official-43-browser-results.json": {
        "choicesTested": 121, "dedicatedRouteTested": 3, "stored": 124,
        "canonicalCombatRejections": 5,
    },
    "official-real-ui-results.json": {
        "initialChoicesUI": 40, "dedicatedUI": 5, "afterReload": 45,
    },
    "u07-all-official-ui-choices.json": {
        "expected": 121, "committed": 121, "rendered": 121,
        "reloadedAndRendered": 121, "doubleDispatchesChecked": 40,
    },
    "u08-official-inventory-ui.json": {
        "staleRejected": 3, "committed": 3, "reloaded": 3,
    },
    "u09-receipt-progress-ui.json": {"expected": 10, "passed": 10},
    "u10-commerce-karg-ui.json": {"passed": 2, "reloaded": 2},
    "u12-soul-after-departure.json": {
        "browserExecuted": True, "afterDepartureOpened": True, "afterReload": True,
    },
    "u20-downstream-paths.json": {"browserExecuted": True, "expected": 2, "passed": 2},
}


def verify(artifact_dir: Path) -> dict:
    findings = []
    for filename, exact_fields in GATES.items():
        path = artifact_dir / filename
        if not path.is_file():
            findings.append({"file": filename, "result": "FAIL", "error": "missing_browser_output"})
            continue
        try:
            report = json.loads(path.read_text(encoding="utf-8"))
            errors = []
            if report.get("status") != "PASS":
                errors.append("status_not_PASS")
            if report.get("failures"):
                errors.append("failures_not_empty")
            if report.get("afterReloadFailures"):
                errors.append("reload_failures_not_empty")
            for key, expected in exact_fields.items():
                actual = report.get(key)
                if type(actual) is not type(expected) or actual != expected:
                    errors.append(f"{key}: expected {expected!r}, got {actual!r}")
            if filename == "official-43-browser-results.json":
                if report.get("registered") != list(range(1, 44)):
                    errors.append("official_registry_not_1_to_43")
                if len(report.get("recorded") or []) != 124:
                    errors.append("recorded_cases_not_124")
            if filename == "u20-downstream-paths.json":
                required_paths = {"35:1:offer_gift:gm_decision", "41:1:board:follow_up"}
                proofs = report.get("paths")
                if not isinstance(proofs, list) or len(proofs) != 2 or {p.get("pathKey") for p in proofs if isinstance(p, dict)} != required_paths:
                    errors.append("missing_or_wrong_downstream_path_keys")
                else:
                    required_stages = ("reachable_UI", "preconditions", "dice_or_combat_or_adjudication",
                                       "canonical_transaction", "receipt", "reload", "campaign_isolation",
                                       "idempotency", "rollback")
                    for proof in proofs:
                        if proof.get("status") != "PASS" or not isinstance(proof.get("evidencePerStage"), dict):
                            errors.append(f"path_not_PASS:{proof.get('pathKey')}")
                        elif any(proof["evidencePerStage"].get(stage) != "PASS" for stage in required_stages):
                            errors.append(f"missing_stage:{proof.get('pathKey')}")
            findings.append({"file": filename, "result": "FAIL" if errors else "PASS", "errors": errors})
        except (OSError, ValueError, TypeError) as exc:
            findings.append({"file": filename, "result": "FAIL", "error": str(exc)})
    passed = sum(f["result"] == "PASS" for f in findings)
    return {
        "status": "PASS" if passed == len(GATES) else "FAIL",
        "passed": passed, "expected": len(GATES),
        "scope": "Chromium+IndexedDB initial choices plus 2/82 downstream paths; remaining 80 not certified; Android/APK excluded",
        "gates": findings,
    }


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: verify_u14_browser_evidence.py /path/to/e2e-results", file=sys.stderr)
        return 2
    folder = Path(sys.argv[1]).resolve()
    result = verify(folder)
    folder.mkdir(parents=True, exist_ok=True)
    (folder / "u14-browser-evidence-summary.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k != "gates"}, ensure_ascii=False))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
