#!/usr/bin/env python3
"""Verify exact source identity, compiled contracts and executable family scenarios.

Source bytes must be the independent pinned bestiary, never compiled app data.
No edits to the shared source comparator; no network or Android dependencies.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent.parent
FIXTURE = ROOT / 'tools/damage_or_save_branch01/contracts.json'
PACKAGE = 'com.braseiro.dnd5e.XmmDamageOrSaveBranch01Scenarios'
BLOB = '9a4137b508ef37562ed948894a70b1594b57e85c'


def verify(source_path, output=None):
    raw = Path(source_path).read_bytes()
    blob = hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()
    if blob != BLOB:
        raise ValueError('Independent pinned source blob mismatch: ' + blob)
    source = json.loads(raw)
    fixture = json.loads(FIXTURE.read_text())
    rows = []
    for entry in fixture['entries']:
        ptr = entry['source_pointer'].split('/')
        monster = source['monster'][int(ptr[2])]
        action = monster[ptr[3]][int(ptr[4])]
        assert monster['name'] == entry['monster_id']
        assert action['name'] == entry['action_id']
        # Includes the entire action, especially Animal Spirit's sibling list.
        digest = hashlib.sha256(json.dumps(action, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
        assert digest == entry['source_action_sha256'], entry['source_pointer']
        rows.append(dict(source_pointer=entry['source_pointer'], monster=monster['name'],
                         action=action['name'], source_action_sha256=digest,
                         baseline='MANUAL_UNSUPPORTED: unmodeled source effect guard present',
                         result='CORE_IMPLEMENTED_INTEGRATION_BLOCKED', combat_applied=False))
    compiled = ROOT / 'app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java'
    compiled_rows = []
    for line in compiled.read_text().splitlines():
        try:
            compiled_rows.append(json.loads(json.loads(line.strip().rstrip(','))))
        except (ValueError, TypeError):
            pass
    for entry in fixture['entries']:
        mon = next(x for x in compiled_rows if x['id'] == entry['monster_id'])
        actions = mon['actions'] + mon['special_actions']
        action = next(a for a in actions if a['name'] == entry['action_id'])
        assert action.get('manual_unmodeled_source_effect_required') is True
        for key, value in entry['mechanical_contract'].items():
            assert action[key] == value, (entry['source_pointer'], key)
    with tempfile.TemporaryDirectory(prefix='damage-branch01-') as classes:
        main = ROOT / 'app/src/main/java/com/braseiro/dnd5e/XmmDamageOrSaveBranch01.java'
        scenarios = ROOT / 'app/src/test/java/com/braseiro/dnd5e/XmmDamageOrSaveBranch01Scenarios.java'
        subprocess.run(['javac', '--release', '8', '-d', classes, str(main), str(scenarios)], check=True)
        observed = subprocess.check_output(['java', '-cp', classes, PACKAGE, '--contracts'], text=True).splitlines()
        assert observed == [e['runtime_contract'] for e in fixture['entries']], 'runtime/source mismatch'
        tests = subprocess.check_output(['java', '-cp', classes, PACKAGE], text=True).strip()
    report = dict(source_commit=fixture['source_commit'], source_blob=blob,
                  core_implemented=12, combat_integrated=0, integration_blocked=12,
                  core_remaining=0, tests=tests, entries=rows)
    if output:
        Path(output).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--json-out', type=Path)
    args = parser.parse_args()
    print(json.dumps(verify(args.source, args.json_out), ensure_ascii=False, indent=2))
