import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const read = p => fs.readFileSync(p, 'utf8');
const readme = read('README.md');
const first = read('LEIA_PRIMEIRO.md');
const validation = read('FINAL_1.0.0_VALIDATION.md');

test('entry documentation identifies EOTE_CORE + Obligation and current recovery path', () => {
  for (const text of [readme, first, validation]) {
    assert.match(text, /EOTE_CORE/);
    assert.match(text, /Obligation/i);
  }
  assert.match(first, /RETOMADA_ATUAL\.md/);
  assert.doesNotMatch(first, /473\/473/);
  assert.doesNotMatch(first, /quatro cenários/);
});

test('validation documentation reports the current full suite and six stress suites', () => {
  assert.match(readme, /783\/783/);
  assert.match(first, /783\/783/);
  assert.match(validation, /783\/783/);
  assert.match(readme, /6\/6 stress/i);
  assert.match(first, /6\/6 stress/i);
  assert.match(validation, /6\/6 stress/i);
  assert.match(validation, /272 entradas pessoais/i);
  assert.match(validation, /25 veículos/i);
});
