import test from 'node:test';
import assert from 'node:assert/strict';
import {
  purchaseForcePower,
  purchaseForceUpgrade,
  eligibleForceUpgrades,
  normalizeForceState
} from '../app/src/main/assets/vtt/js/rules/force.mjs';

function makeForceChar() {
  return {
    id: 'char-jedi-01',
    name: 'Jedi Cal',
    force: { rating: 2, committed: 0, darkSideUser: false, ongoing: [], powers: {} },
    xp: { available: 100, total: 200 }
  };
}

test('Force Powers: Can purchase and upgrade F&D Core powers (Enhance, Heal/Harm, Bind, etc.)', () => {
  const char = makeForceChar();

  // Buy Enhance (10 XP)
  const buyRes = purchaseForcePower(char, 'enhance');
  assert.equal(buyRes.cost, 10);
  assert.equal(buyRes.character.xp.available, 90);

  // Eligible upgrades for Enhance
  const upgrades = eligibleForceUpgrades(buyRes.character, 'enhance');
  assert.ok(upgrades.length > 0);
  const firstUpgrade = upgrades.find(u => u.available);
  assert.ok(firstUpgrade);

  // Buy upgrade
  const upRes = purchaseForceUpgrade(buyRes.character, 'enhance', firstUpgrade.id);
  assert.equal(upRes.character.xp.available, 90 - firstUpgrade.cost);
});

test('Force Powers: Can purchase and upgrade Supplemental powers (Battle Meditation, Manipulate, etc.)', () => {
  const char = makeForceChar();

  // Buy Battle Meditation (15 XP)
  const buyRes = purchaseForcePower(char, 'battle_meditation');
  assert.equal(buyRes.cost, 15);
  assert.equal(buyRes.character.xp.available, 85);

  // Upgrades
  const upgrades = eligibleForceUpgrades(buyRes.character, 'battle_meditation');
  assert.ok(upgrades.length >= 4);
  const rootUpgrade = upgrades.find(u => u.parents.length === 0);
  assert.ok(rootUpgrade);

  // Buy root upgrade (range_1)
  const upRes = purchaseForceUpgrade(buyRes.character, 'battle_meditation', rootUpgrade.id);
  assert.equal(upRes.character.xp.available, 85 - rootUpgrade.cost);
});
