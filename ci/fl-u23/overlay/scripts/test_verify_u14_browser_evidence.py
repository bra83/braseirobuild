#!/usr/bin/env python3
"""U14 contract tests with artificial fixtures; NEVER counts as browser E2E."""
import importlib.util
import json
import tempfile
from pathlib import Path

path = Path(__file__).with_name('verify_u14_browser_evidence.py')
spec = importlib.util.spec_from_file_location('verify_u14', path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
checks = 0
with tempfile.TemporaryDirectory() as t:
    root = Path(t)
    r = module.verify(root)
    assert r['status'] == 'FAIL' and r['passed'] == 0
    checks += 1
    for file_name, fields in module.GATES.items():
        obj = {'status': 'PASS', 'failures': [], **fields}
        if file_name == 'official-43-browser-results.json':
            obj.update(registered=list(range(1, 44)), recorded=[{'id': i} for i in range(124)], afterReloadFailures=[])
        if file_name == 'u20-downstream-paths.json':
            obj['paths'] = [{'pathKey': key, 'status': 'PASS', 'evidencePerStage': {stage: 'PASS' for stage in
                ('reachable_UI', 'preconditions', 'dice_or_combat_or_adjudication', 'canonical_transaction',
                 'receipt', 'reload', 'campaign_isolation', 'idempotency', 'rollback')}} for key in
                ('35:1:offer_gift:gm_decision', '41:1:board:follow_up')]
        (root / file_name).write_text(json.dumps(obj), encoding='utf8')
    assert module.verify(root)['status'] == 'PASS'
    checks += 1
    registry = root / 'official-43-browser-results.json'
    obj = json.loads(registry.read_text()); obj['registered'] = list(range(1,43))
    registry.write_text(json.dumps(obj)); assert module.verify(root)['status'] == 'FAIL'
    checks += 1
    obj['registered'] = list(range(1,44))
    authority = obj.pop('canonicalCombatRejections', None)
    registry.write_text(json.dumps(obj)); assert module.verify(root)['status'] == 'FAIL'
    checks += 1
    obj['canonicalCombatRejections'] = authority
    registry.write_text(json.dumps(obj)); assert module.verify(root)['status'] == 'PASS'
    checks += 1
    obj['recorded'] = []
    registry.write_text(json.dumps(obj)); assert module.verify(root)['status'] == 'FAIL'
    checks += 1
    obj['recorded'] = [{'id': i} for i in range(124)]; registry.write_text(json.dumps(obj))
    ui = root / 'u07-all-official-ui-choices.json'
    x = json.loads(ui.read_text()); x['committed'] = 120; ui.write_text(json.dumps(x))
    assert module.verify(root)['status'] == 'FAIL'
    checks += 1
    x['committed'] = 121; ui.write_text(json.dumps(x)); assert module.verify(root)['status'] == 'PASS'
    checks += 1
    x['doubleDispatchesChecked'] = True; ui.write_text(json.dumps(x)); assert module.verify(root)['status'] == 'FAIL'
    checks += 1
    x['doubleDispatchesChecked'] = 40; ui.write_text(json.dumps(x))
    progress = root / 'u09-receipt-progress-ui.json'
    y = json.loads(progress.read_text()); y['status'] = 'BLOCKED'; progress.write_text(json.dumps(y))
    assert module.verify(root)['status'] == 'FAIL'; checks += 1
    y['status'] = 'PASS'; progress.write_text(json.dumps(y)); assert module.verify(root)['status'] == 'PASS'; checks += 1
    # A report with only the earlier nine cases is no longer complete.
    y['passed'] = 9; y['expected'] = 9; progress.write_text(json.dumps(y))
    assert module.verify(root)['status'] == 'FAIL'; checks += 1
    y['passed'] = 10; y['expected'] = 10; progress.write_text(json.dumps(y))
    assert module.verify(root)['status'] == 'PASS'; checks += 1
    downstream = root / 'u20-downstream-paths.json'
    z = json.loads(downstream.read_text())
    z['paths'][0]['evidencePerStage']['rollback'] = 'NOT_RUN'
    downstream.write_text(json.dumps(z)); assert module.verify(root)['status'] == 'FAIL'; checks += 1
    z['paths'][0]['evidencePerStage']['rollback'] = 'PASS'
    downstream.write_text(json.dumps(z)); assert module.verify(root)['status'] == 'PASS'; checks += 1
    z['paths'][1]['pathKey'] = '35:1:offer_gift:gm_decision'
    downstream.write_text(json.dumps(z)); assert module.verify(root)['status'] == 'FAIL'; checks += 1
    z['paths'][1]['pathKey'] = '41:1:board:follow_up'
    downstream.write_text(json.dumps(z)); assert module.verify(root)['status'] == 'PASS'; checks += 1
    progress.unlink(); assert module.verify(root)['status'] == 'FAIL'; checks += 1
print(f'U14_EVIDENCE_VALIDATOR_CONTRACT PASS {checks}/{checks} (artificial JSON fixtures, not Chromium)')
