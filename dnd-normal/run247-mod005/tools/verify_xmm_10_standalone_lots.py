#!/usr/bin/env python3
"""Combined XMM independent JVM gates, lots 04–13.

Runs existing official-source-indexed verifier scripts in order. A successful
run certifies only the isolated Java executor unit tests, NOT source parity,
Android combat, state persistence, 24-hour effects or backup restore.

Usage: python3 -B dnd-normal/run247-mod005/tools/verify_xmm_10_standalone_lots.py
"""
from pathlib import Path
import json
import subprocess
import sys

TOOLS = Path(__file__).resolve().parent
GATES = (
    ("04", "verify_damage_or_save_branch04.py", 12),
    ("05", "verify_damage_or_save_branch05.py", 12),
    ("06", "verify_damage_or_save_branch06.py", 1),
    ("07", "verify_xmm_other_effects01.py", 10),
    ("08", "verify_xmm_other_effects02.py", 10),
    ("09", "verify_xmm_other_effects03.py", 9),
    ("10", "verify_xmm_attack_hit_condition01.py", 4),
    ("11", "verify_xmm_swallow_and_escape01.py", 2),
    ("12", "verify_xmm_explicit_effect_choice01.py", 1),
    ("13", "verify_xmm_monster_spell_action01.py", 1),
)
def main():
    completed = []
    failed = []
    for number, script, contracts in GATES:
        file = TOOLS / script
        if not file.is_file():
            failed.append({"lot": number, "script": script, "reason": "SCRIPT_MISSING"})
            print(f"XMM_LOT_{number}_GATE=SCRIPT_MISSING", flush=True)
            continue
        print(f"=== XMM LOTE {number}: {script} / {contracts} ações ===", flush=True)
        process = subprocess.run(
            [sys.executable, "-B", str(file)],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
        )
        print(process.stdout, end="", flush=True)
        record = {"lot": number, "script": script, "contracts": contracts,
                  "exit_code": process.returncode}
        if process.returncode == 0:
            completed.append(record)
            print(f"XMM_LOT_{number}_JVM=PASS", flush=True)
        else:
            failed.append(record)
            print(f"XMM_LOT_{number}_JVM=FAIL exit={process.returncode}", flush=True)
    manifest = {
        "scope": "independent_jvm_only",
        "source_binary_parity": "NOT_TESTED_BY_THIS_GATE",
        "live_combat_integration": "NOT_INTEGRATED",
        "sqlite_persistence": "NOT_IMPLEMENTED",
        "android_device": "NOT_TESTED",
        "lots_passed": len(completed),
        "lots_failed": len(failed),
        "actions_in_passed_lots": sum(x["contracts"] for x in completed),
        "lots": completed,
        "failures": failed,
    }
    print("XMM_STANDALONE_VERIFICATION=" + json.dumps(
        manifest, ensure_ascii=False, sort_keys=True), flush=True)
    if failed:
        raise SystemExit(1)
    print("XMM_LOTS_04_TO_13_STANDALONE_JVM=PASS", flush=True)

if __name__ == "__main__":
    main()
