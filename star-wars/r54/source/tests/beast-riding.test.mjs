import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CANONICAL_MOUNTS,
  tameBeast,
  mountBeast,
  dismountBeast,
  executeMountedCharge,
  checkBeastPanic
} from '../app/src/main/assets/vtt/js/rules/beastRiding.mjs';

function makeChar() {
  return {
    id: 'char-rider-01',
    name: 'Cavaleiro Boba',
    mounts: []
  };
}

test('Beast Riding: Canonical mount catalog integrity', () => {
  const keys = Object.keys(CANONICAL_MOUNTS);
  assert.ok(keys.includes('dewback'));
  assert.ok(keys.includes('tauntaun'));
  assert.ok(keys.includes('bantha'));
  assert.ok(keys.includes('varactyl'));
  assert.ok(keys.includes('blurrg'));

  const dewback = CANONICAL_MOUNTS.dewback;
  assert.equal(dewback.silhouette, 3);
  assert.equal(dewback.woundThreshold, 22);
  assert.equal(dewback.soak, 5);
});

test('Beast Riding: Taming success with bond and failure with retaliation', () => {
  const char = makeChar();

  // Success with triumph -> tamed with bond
  const successResult = tameBeast(char, 'dewback', { successes: 2, triumphs: 1 });
  assert.equal(successResult.tamed, true);
  assert.equal(successResult.hasBond, true);
  assert.equal(successResult.character.mounts.length, 1);
  assert.equal(successResult.mount.templateId, 'dewback');

  // Failure with despair -> retaliation damage
  const failResult = tameBeast(char, 'bantha', { successes: 0, despairs: 1 });
  assert.equal(failResult.tamed, false);
  assert.equal(failResult.retaliation, true);
  assert.equal(failResult.damageSuffered, 10);
});

test('Beast Riding: Mount, Dismount, and Mounted Charge damage bonus', () => {
  const char = makeChar();
  const tameRes = tameBeast(char, 'dewback', { successes: 2 });
  const mountInstance = tameRes.mount;

  // Mount
  const mounted = mountBeast(mountInstance);
  assert.equal(mounted.mounted, true);

  // Mounted charge adds beast silhouette (3) to weapon damage (6) -> 9
  const charge = executeMountedCharge(mounted, 6);
  assert.equal(charge.chargeBonus, 3);
  assert.equal(charge.totalDamage, 9);

  // Dismount
  const dismounted = dismountBeast(mounted);
  assert.equal(dismounted.mounted, false);
});

test('Beast Riding: Panic check when damage exceeds half wound threshold', () => {
  const char = makeChar();
  const tameRes = tameBeast(char, 'tauntaun', { successes: 2 }); // woundThreshold = 14
  let mountInstance = mountBeast(tameRes.mount);

  // Take 4 damage (less than half 7) -> no panic check needed
  const lightDamage = checkBeastPanic(mountInstance, 4);
  assert.equal(lightDamage.panicked, false);

  // Take 5 more damage (total 9 >= 7) with failed check -> panic and thrown rider
  const severeDamage = checkBeastPanic(lightDamage.mount, 5, { successes: 0 });
  assert.equal(severeDamage.panicked, true);
  assert.equal(severeDamage.thrownRider, true);
  assert.equal(severeDamage.strainSufferedByRider, 3);
  assert.equal(severeDamage.mount.mounted, false);
});
