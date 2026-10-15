#!/usr/bin/env python3
"""Run admission JVM tests and inventory/registry identity comparison.

This gate does not certify gameplay, source prose semantics or Android SQLite.
"""
from pathlib import Path
import importlib.util
import json
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/braseiro/dnd5e'
TEST = ROOT / 'app/src/test/java/com/braseiro/dnd5e'

def main():
    spec = importlib.util.spec_from_file_location('xmm_audit', ROOT / 'tools/audit_xmm_62_source_registry.py')
    audit = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(audit)
    audit.main()
    expected = set()
    for _, _, _, _, filename in audit.LOTS:
        expected.update(x[1:-1] for x in audit.PATTERN.findall((JAVA / filename).read_text()))
    source = (JAVA / 'XmmCombatSourceRegistry.java').read_text()
    actual = [x[1:-1] for x in audit.PATTERN.findall(source)]
    if len(actual) != 62 or len(set(actual)) != 62 or set(actual) != expected:
        raise SystemExit('XMM_HOST_REGISTRY_IDENTITIES=FAIL')
    inventory = {row['source_pointer']: row for row in json.loads(audit.INVENTORY.read_text())['entries']}
    declarations = re.findall(r'add\(entries,("(?:[^"\\]|\\.)*"),("(?:[^"\\]|\\.)*"),'
                              r'("(?:[^"\\]|\\.)*"),("\d+"),Family\.([A-Z_]+),'
                              r'("[^"]+"),("[^"]+")\);', source)
    if len(declarations) != 62:
        raise SystemExit('XMM_HOST_REGISTRY_DECLARATIONS=FAIL')
    lots = {number: (family, filename[:-5]) for number, family, _, _, filename in audit.LOTS}
    for pointer, monster, action, lot, family, executor, blocker in declarations:
        pointer, monster, action, lot, executor, blocker = map(json.loads,
            (pointer, monster, action, lot, executor, blocker))
        row = inventory[pointer]
        if ((monster, action, family.lower()) != (row['monster_id'], row['action_id'], row['family'])
                or lots[lot] != (family.lower(), executor) or not blocker):
            raise SystemExit('XMM_HOST_REGISTRY_BINDING=FAIL ' + pointer)
    print('XMM_HOST_REGISTRY_IDENTITIES=PASS count=62', flush=True)
    with tempfile.TemporaryDirectory(prefix='xmm-admission-') as folder:
        subprocess.run(['javac', '--release', '8', '-d', folder] + [str(JAVA / (x + '.java')) for x in (
            'CombatActionReceiptGuard', 'DamageMitigationRules', 'XmmDamageOrSaveBranch06',
            'XmmCombatSourceRegistry', 'XmmCombatAdmission')] +
            [str(TEST / 'XmmCombatAdmissionScenarios.java')], check=True)
        subprocess.run(['java', '-cp', folder, 'com.braseiro.dnd5e.XmmCombatAdmissionScenarios'], check=True)

if __name__ == '__main__':
    main()
