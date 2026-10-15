"""Exact batch/compiled comparison; never substitutes compiled data for pinned source."""
import argparse
import hashlib
import json
from pathlib import Path
from verify_xmm_503_source import java_rows

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/editorial-evidence/xmm-damage-or-save-branch-02'
SOURCE_BLOB = '9a4137b508ef37562ed948894a70b1594b57e85c'

def compare(job, compiled):
    rows = {r['id']: r for r in compiled}
    results = []
    for entry in job['entries']:
        # Match exact monster AND exact category/slot. Never select by prose similarity.
        parts = entry['source_pointer'].split('/')
        category, slot = parts[3], int(parts[4])
        monster = rows[entry['monster_id']]
        actions = monster['actions'] if category == 'action' else [
            a for a in monster.get('special_actions', []) if a.get('category') == 'legendary_action']
        action = actions[slot]
        mismatches = [k for k, v in entry['mechanical_contract'].items() if action.get(k) != v]
        if action['name'] != entry['action_id']:
            mismatches.append('action_id')
        results.append({
            'source_pointer': entry['source_pointer'],
            'monster': entry['monster_id'], 'action': entry['action_id'],
            'compiled_contract_mismatches': mismatches,
            'existing_compiled_action': action,
            'status': 'BLOCKED_SOURCE_AND_INTEGRATION',
            'completed': False,
            'reason': 'Pinned source unavailable: full branches, riders, durations, target '
                      'eligibility and action cost cannot be certified. Protected UI/SQLite '
                      'adapter not changed; independent generic core is not combat execution.'})
    return results

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out', type=Path)
    args = p.parse_args()
    job = json.loads((EVIDENCE / 'job.json').read_text())
    java = ROOT / 'app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java'
    results = compare(job, java_rows(java.read_text()))
    report = {'job_id': job['job_id'], 'source_commit': job['source_commit'],
              'expected_source_blob': SOURCE_BLOB, 'pinned_source_verified': False,
              'compiled_file_sha256': hashlib.sha256(java.read_bytes()).hexdigest(),
              'implemented_complete_actions': 0, 'remaining_actions': len(results),
              'integration_dependent_actions': len(results),
              'independent_core': 'XmmDamageOrSaveBranch02 (unregistered)',
              'results': results}
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.out:
        args.out.write_text(text)
    else:
        print(text, end='')
    return int(any(r['compiled_contract_mismatches'] for r in results))

if __name__ == '__main__':
    raise SystemExit(main())
