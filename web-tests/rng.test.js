import test from 'node:test';
import assert from 'node:assert/strict';
import { DeterministicRng, RNG_ALGORITHM } from '../app/src/main/assets/www/core/rng-core.js';

test('same seed + stream yields same deterministic vector', () => {
  const a = new DeterministicRng('braseiro-test');
  const b = new DeterministicRng('braseiro-test');
  const va = Array.from({ length: 8 }, () => a.die(20, 'dice'));
  const vb = Array.from({ length: 8 }, () => b.die(20, 'dice'));
  assert.deepEqual(va, vb);
  assert.equal(va[0].algorithm, RNG_ALGORITHM);
});


test('golden RNG vector is locked for replay compatibility', () => {
  const rng = new DeterministicRng('braseiro-test');
  const vector = Array.from({ length: 8 }, () => rng.die(20, 'dice')).map(x => [x.rawHex, x.result]);
  assert.deepEqual(vector, [
    ['30a630eaa1462bec', 17],
    ['f783fe8dad686751', 10],
    ['1cf354bc6f62407e', 3],
    ['b5fa8781dcfcaa62', 11],
    ['027da2d3a9f342ad', 2],
    ['3bdbf50a233119d3', 4],
    ['b129ad480f5470fc', 17],
    ['0c37d88b30dec4c5', 6],
  ]);
});

test('stream interleaving does not change a stream sequence', () => {
  const baseline = new DeterministicRng('seed');
  const expected = [baseline.die(6, 'dice'), baseline.die(6, 'dice'), baseline.die(6, 'dice')];
  const interleaved = new DeterministicRng('seed');
  const actual = [interleaved.die(6, 'dice')];
  interleaved.die(100, 'world');
  actual.push(interleaved.die(6, 'dice'));
  interleaved.die(100, 'encounter');
  actual.push(interleaved.die(6, 'dice'));
  assert.deepEqual(actual.map(x => x.rawHex), expected.map(x => x.rawHex));
});

test('export/import resumes exact stream counters', () => {
  const rng = new DeterministicRng('resume-seed');
  rng.die(8, 'dice');
  rng.die(8, 'dice');
  const state = rng.exportState();
  const nextA = rng.die(8, 'dice');
  const resumed = DeterministicRng.fromState(state);
  const nextB = resumed.die(8, 'dice');
  assert.deepEqual(nextB, nextA);
});
