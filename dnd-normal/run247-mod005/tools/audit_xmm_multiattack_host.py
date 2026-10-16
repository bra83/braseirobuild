#!/usr/bin/env python3
"""Audit the original 70 PR40 pointers without claiming gameplay coverage."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/braseiro/dnd5e'
INVENTORY = ROOT / 'docs/editorial-evidence/xmm-condition-choice-preview-local-20261015/manual-effect-inventory-236.json'


def audit():
    plans = {}
    for match in re.finditer(r'^\s*("(?:[^"\\]|\\.)*"),?$',
                             (JAVA / 'Xmm2025MultiattackPlanIndex.java').read_text(), re.M):
        row = json.loads(json.loads(match[1]))
        if row['id'] in plans:
            raise ValueError('duplicate_multiattack_plan')
        plans[row['id']] = row
    entries = [row for row in json.loads(INVENTORY.read_text())['entries']
               if row['family'] == 'multiattack_sequence']
    if len(entries) != 70 or len({r['source_pointer'] for r in entries}) != 70:
        raise ValueError('original_scope_pointer_count_mismatch')
    rows = []
    for entry in entries:
        plan = plans[entry['monster_id']]
        if entry['source_pointer'] not in (plan['ptr'], plan['ptr'] + '/0'):
            raise ValueError('multiattack_source_pointer_mismatch')
        variants = []
        for number, groups in enumerate(plan['v']):
            slot = 0
            blocked = ''
            for group in groups:
                if group['k'] != 'attack' and not blocked:
                    blocked = f"multiattack_component_executor_not_installed_{group['k']}_{slot}"
                slot += group['c']
            if not blocked and not 2 <= slot <= 8:
                blocked = 'invalid_multiattack_count'
            variants.append({'variant_index': number, 'slots': slot,
                             'admission': blocked or 'REQUIRES_LIVE_HOST_VALIDATION'})
        rows.append({'monster': entry['monster_id'], 'source_pointer': entry['source_pointer'],
                     'plan_pointer': plan['ptr'],
                     'status': 'REQUIRES_EXPLICIT_SELECTION_AND_LIVE_HOST_VALIDATION' if variants
                     else 'complex_multiattack_requires_manual_source_resolution',
                     'variants': variants})
    return {'scope': 'original_pr40_70_multiattack_pointers', 'pointers': len(rows),
            'structured_candidates': sum(bool(r['variants']) for r in rows),
            'blocked_missing_source_plan': sum(not r['variants'] for r in rows),
            'runtime_tested': False, 'full_combat_automation_certified': False,
            'rows': rows}


if __name__ == '__main__':
    print(json.dumps(audit(), ensure_ascii=False, indent=2))
