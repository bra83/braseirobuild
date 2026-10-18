import test from 'node:test';
import assert from 'node:assert/strict';
import {
  SUPPLEMENTAL_FORCE_POWERS,
  ALL_FORCE_POWERS
} from '../app/src/main/assets/vtt/js/rules/fndExpandedForcePowers.mjs';

test('Supplemental Force Powers: All 8 canonical sourcebook powers exist and are immutable', () => {
  const expectedPowers = [
    'battle_meditation',
    'foresee',
    'farsight',
    'alter',
    'conjure',
    'suppress',
    'manipulate',
    'imbue'
  ];

  for (const key of expectedPowers) {
    const power = SUPPLEMENTAL_FORCE_POWERS[key];
    assert.ok(power, `Poder ${key} deve existir`);
    assert.ok(power.baseCost >= 10);
    assert.ok(Array.isArray(power.upgrades) && power.upgrades.length >= 3);
    assert.ok(power.source.book, `Fonte de ${key} deve ser documentada`);
  }
});

test('ALL_FORCE_POWERS aggregates both F&D Core and Supplemental powers', () => {
  const keys = Object.keys(ALL_FORCE_POWERS);
  // F&D Core (6): enhance, heal_harm, bind, misdirect, protect_unleash, seek
  // Supplemental (8): battle_meditation, foresee, farsight, alter, conjure, suppress, manipulate, imbue
  assert.equal(keys.length, 14);
  assert.ok(ALL_FORCE_POWERS.enhance);
  assert.ok(ALL_FORCE_POWERS.battle_meditation);
  assert.ok(ALL_FORCE_POWERS.manipulate);
  assert.ok(ALL_FORCE_POWERS.imbue);
});

test('Upgrade tree integrity for expanded powers', () => {
  for (const [key, power] of Object.entries(SUPPLEMENTAL_FORCE_POWERS)) {
    const upgradeIds = new Set(power.upgrades.map(u => u.id));
    for (const u of power.upgrades) {
      assert.ok(u.cost > 0, `Custo do upgrade ${u.id} em ${key} deve ser positivo`);
      for (const parent of u.parents) {
        assert.ok(upgradeIds.has(parent), `Pai ${parent} do upgrade ${u.id} deve existir em ${key}`);
      }
    }
  }
});
