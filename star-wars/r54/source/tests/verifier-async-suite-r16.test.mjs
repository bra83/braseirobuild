import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const verifier=fs.readFileSync(new URL('../tools/verify-final-source.mjs',import.meta.url),'utf8');
const runner=fs.readFileSync(new URL('../tools/run-node-suite.py',import.meta.url),'utf8');

test('verificador executa a suíte completa pelo runner Python sem encadear node --test no processo Node pai',()=>{
  assert.match(verifier,/runAsync\(pythonCommand, \['tools\/run-node-suite\.py'\]/);
  assert.match(verifier,/child\.on\('close'/);
  assert.doesNotMatch(verifier,/spawnSync\(process\.execPath/);
  assert.match(verifier,/suíte Node não produziu resumo TAP completo/);
  assert.match(runner,/ThreadPoolExecutor\(max_workers=8\)/);
  assert.match(runner,/for path in STRESS:/);
  assert.match(runner,/subprocess\.run\(\['node','--test','--test-reporter=tap',rel\]/);
});
