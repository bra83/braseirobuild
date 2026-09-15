import test from 'node:test';
import assert from 'node:assert/strict';
import { DeterministicRng } from '../app/src/main/assets/www/core/rng-core.js';
import { attributeModifier, resolveSkillCheck, validateMinimalCoreCharacter } from '../app/src/main/assets/www/core/wwn-rules-core.js';

const character = {
  level: 1,
  attributes: { Strength:10, Dexterity:12, Constitution:11, Intelligence:10, Wisdom:14, Charisma:9 },
  skills: { Notice:0, Sneak:0 },
};

test('WWN attribute modifier boundaries match SRD', () => {
  assert.equal(attributeModifier(3), -2);
  for (const x of [4,5,6,7]) assert.equal(attributeModifier(x), -1);
  for (const x of [8,10,13]) assert.equal(attributeModifier(x), 0);
  for (const x of [14,16,17]) assert.equal(attributeModifier(x), 1);
  assert.equal(attributeModifier(18), 2);
});

test('Wave3 minimal character subset validates level-1 core-check fixture', () => {
  assert.deepEqual(validateMinimalCoreCharacter(character), { ok:true, errors:[] });
  assert.equal(validateMinimalCoreCharacter({ ...character, attributes:{...character.attributes, Wisdom:19} }).ok, false);
  assert.equal(validateMinimalCoreCharacter({ ...character, skills:{ Notice:2 } }).ok, false);
});

test('WWN skill check is deterministic 2d6 + skill + attribute mod vs difficulty', () => {
  const a = new DeterministicRng('rules-seed');
  const b = new DeterministicRng('rules-seed');
  const ra = resolveSkillCheck({ rng:a, skillLevel:0, attributeScore:14, difficulty:8, streamName:'notice' });
  const rb = resolveSkillCheck({ rng:b, skillLevel:0, attributeScore:14, difficulty:8, streamName:'notice' });
  assert.deepEqual(ra, rb);
  assert.equal(ra.total, ra.diceTotal + 0 + 1);
  assert.equal(ra.success, ra.total >= 8);
  assert.equal(a.exportState().counters.notice, 2);
});

test('untrained pertinent skill contributes -1', () => {
  const rng = new DeterministicRng('untrained');
  const result = resolveSkillCheck({ rng, skillLevel:null, attributeScore:10, difficulty:8, streamName:'test' });
  assert.equal(result.skillContribution, -1);
  assert.equal(result.total, result.diceTotal - 1);
});
