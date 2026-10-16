import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const verifier = readFileSync(new URL('../tools/verify-final-source.mjs', import.meta.url), 'utf8');

test('verificador cria validation/ antes de executar suíte ou gravar relatório', () => {
  assert.match(verifier, /mkdirSync\(validationDir, \{ recursive: true \}\)/);
  const mkdirAt = verifier.indexOf('mkdirSync(validationDir');
  const suiteAt = verifier.indexOf("runAsync(pythonCommand, ['tools/run-node-suite.py']");
  const reportAt = verifier.indexOf("LATEST_SOURCE_VALIDATION.txt");
  assert.ok(mkdirAt >= 0 && suiteAt >= 0 && mkdirAt < suiteAt && mkdirAt < reportAt);
});

test('relatório final usa o validationDir garantido', () => {
  assert.match(verifier, /join\(validationDir, 'LATEST_SOURCE_VALIDATION\.txt'\)/);
});
