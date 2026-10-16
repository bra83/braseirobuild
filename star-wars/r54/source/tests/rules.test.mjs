import test from 'node:test';
import assert from 'node:assert/strict';
import { buildPositivePool, resolveSymbols, rollPool } from '../app/src/main/assets/vtt/js/rules/narrativeDice.mjs';

test('reserva positiva usa o maior valor como total e o menor como upgrades', () => {
  assert.deepEqual(buildPositivePool(4, 2), { ability: 2, proficiency: 2 });
  assert.deepEqual(buildPositivePool(3, 5), { ability: 2, proficiency: 3 });
});

test('êxito/falha e vantagem/ameaça se cancelam em eixos independentes', () => {
  const r = resolveSymbols({ success: 3, failure: 1, advantage: 1, threat: 2 });
  assert.equal(r.success, true);
  assert.equal(r.netSuccess, 2);
  assert.equal(r.netAdvantage, -1);
});

test('triunfo conta como êxito e desespero conta como falha sem apagar o símbolo especial', () => {
  const r = resolveSymbols({ triumph: 1, despair: 1, advantage: 2 });
  assert.equal(r.netSuccess, 0);
  assert.equal(r.success, false);
  assert.equal(r.triumph, 1);
  assert.equal(r.despair, 1);
});

test('rolagem aceita RNG injetado para testes determinísticos', () => {
  const seq = [0.26, 0.99]; let i=0;
  const r = rollPool({ ability:1, difficulty:1 }, () => seq[i++]);
  assert.equal(r.faces.length, 2);
  assert.equal(typeof r.success, 'boolean');
});
