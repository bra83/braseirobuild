#!/usr/bin/env python3
"""U13: execute EVERY official Chromium harness and preserve individual results.

A failure in one encounter group must not prevent diagnosing the other groups.
This runner does not claim browser coverage unless each child exits successfully.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time
from pathlib import Path

SCRIPTS = (
    'official_browser.py',
    'official_ui_browser.py',
    'official_ui_all_choices.py',
    'official_inventory_ui.py',
    'official_receipt_progress_ui.py',
    'official_u10_commerce_karg_ui.py',
    'official_u12_soul_after_departure_ui.py',
    'official_u20_downstream_paths.py',
    'official_u21_parley_rescue_ui.py',
    'official_u22_downstream_batch.py',
    'official_u23_downstream_combat.py',
    'official_u24_source_adjudications.py',
)


# U15: each production harness has one authoritative JSON proof. A script
# exiting 0 without writing its OWN current-run proof is NOT browser coverage.
# Unknown names remain supported for isolated runner contract tests.
EXPECTED_BROWSER_REPORTS = {
    'official_browser.py': 'official-43-browser-results.json',
    'official_ui_browser.py': 'official-real-ui-results.json',
    'official_ui_all_choices.py': 'u07-all-official-ui-choices.json',
    'official_inventory_ui.py': 'u08-official-inventory-ui.json',
    'official_receipt_progress_ui.py': 'u09-receipt-progress-ui.json',
    'official_u10_commerce_karg_ui.py': 'u10-commerce-karg-ui.json',
    'official_u12_soul_after_departure_ui.py': 'u12-soul-after-departure.json',
    'official_u20_downstream_paths.py': 'u20-downstream-paths.json',
    'official_u21_parley_rescue_ui.py': 'u21-parley-rescue.json',
    'official_u22_downstream_batch.py': 'u22-downstream-batch.json',
    'official_u23_downstream_combat.py': 'u23-downstream-combat.json',
    'official_u24_source_adjudications.py': 'u24-source-adjudications.json',
}


def run_suite(scripts: tuple[Path, ...], artifacts: Path, timeout_seconds: int = 360) -> dict:
    artifacts.mkdir(parents=True, exist_ok=True)
    cases = []
    for script in scripts:
        started = time.monotonic()
        name = script.stem
        record = {'script': script.name, 'status': 'FAIL', 'exit_code': None,
                  'elapsed_seconds': 0, 'stdout': f'{name}.stdout.log',
                  'stderr': f'{name}.stderr.log'}
        out = ''
        err = ''
        expected_report = EXPECTED_BROWSER_REPORTS.get(script.name)
        # Do not allow yesterday's successful JSON to satisfy today's run.
        # Only remove the known report for this harness; preserve all other logs.
        if expected_report:
            (artifacts / expected_report).unlink(missing_ok=True)
        try:
            completed = subprocess.run(
                [sys.executable, str(script)], cwd=str(script.parent),
                env={**os.environ, 'FL_E2E_ARTIFACTS': str(artifacts)},
                capture_output=True, text=True, timeout=timeout_seconds, check=False,
            )
            record['exit_code'] = completed.returncode
            record['status'] = 'PASS' if completed.returncode == 0 else 'FAIL'
            out, err = completed.stdout, completed.stderr
        except subprocess.TimeoutExpired as exc:
            record['status'] = 'TIMEOUT'
            out = exc.stdout.decode('utf-8', errors='replace') if isinstance(exc.stdout, bytes) else exc.stdout or ''
            err = exc.stderr.decode('utf-8', errors='replace') if isinstance(exc.stderr, bytes) else exc.stderr or ''
            err += f'\nTIMEOUT: {timeout_seconds}s\n'
        except OSError as exc:
            record['status'] = 'ERROR'
            err = f'{type(exc).__name__}: {exc}\n'
        if expected_report:
            proof = artifacts / expected_report
            record['report'] = expected_report
            if not proof.is_file():
                record['status'] = 'FAIL' if record['status'] == 'PASS' else record['status']
                err += '\nU15: relatório JSON do navegador não foi criado nesta execução.\n'
            else:
                try:
                    data = json.loads(proof.read_text(encoding='utf-8'))
                    if not isinstance(data, dict) or data.get('status') != 'PASS' or data.get('failures') or data.get('afterReloadFailures'):
                        record['status'] = 'FAIL' if record['status'] == 'PASS' else record['status']
                        err += '\nU15: relatório JSON do navegador não comprova PASS.\n'
                except (OSError, ValueError, TypeError) as exc:
                    record['status'] = 'FAIL' if record['status'] == 'PASS' else record['status']
                    err += f'\nU15: relatório JSON inválido: {exc}\n'
        record['elapsed_seconds'] = round(time.monotonic() - started, 3)
        (artifacts / record['stdout']).write_text(out, encoding='utf-8')
        (artifacts / record['stderr']).write_text(err, encoding='utf-8')
        cases.append(record)
        print(f"{record['status']}: {script.name} ({record['elapsed_seconds']}s)", flush=True)
    passed = sum(item['status'] == 'PASS' for item in cases)
    summary = {'status': 'PASS' if passed == len(scripts) else 'FAIL',
               'expected': len(scripts), 'passed': passed, 'failed': len(scripts) - passed,
               'cases': cases, 'browser_coverage_verified': passed == len(scripts)}
    (artifacts / 'u13-browser-suite-summary.json').write_text(
        json.dumps(summary, indent=2, ensure_ascii=False), encoding='utf-8')
    return summary


if __name__ == '__main__':
    directory = Path(__file__).resolve().parent
    artifacts = Path(os.environ.get('FL_E2E_ARTIFACTS', 'e2e-results')).resolve()
    timeout = int(os.environ.get('FL_E2E_CASE_TIMEOUT_SECONDS', '360'))
    result = run_suite(tuple(directory / name for name in SCRIPTS), artifacts, timeout)
    sys.exit(0 if result['status'] == 'PASS' else 1)
