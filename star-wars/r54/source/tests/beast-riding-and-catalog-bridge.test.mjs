import test from 'node:test';
import assert from 'node:assert/strict';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

function createMockRepository() {
  let saved = null;
  return {
    load() { return saved; },
    save(state) { saved = structuredClone(state); return saved; }
  };
}

test('Controller & Bridge: Beast Riding, mounts catalog and taming', () => {
  const repo = createMockRepository();
  const bridge = new BraseiroUiBridge({ repository: repo });
  bridge.newCampaign('Campanha Feras');

  // Catalogs
  const mountsRes = bridge.mountsCatalog();
  assert.equal(mountsRes.ok, true);
  assert.ok(mountsRes.data.dewback);
  assert.ok(mountsRes.data.tauntaun);

  const careerSpecsRes = bridge.careerExpansionCatalog();
  assert.equal(careerSpecsRes.ok, true);
  assert.ok(careerSpecsRes.data.bounty_hunter.martial_artist);
  assert.ok(careerSpecsRes.data.smuggler.gunslinger);

  // Character creation
  const charRes = bridge.createFndCharacter({ name: 'Rider Luke', careerKey: 'seeker', specializationKey: 'hunter' });
  assert.equal(charRes.ok, true);

  // Tame beast
  const tameRes = bridge.tameBeast('dewback', { successes: 2, triumphs: 1 });
  assert.equal(tameRes.ok, true);
  assert.equal(tameRes.data.tamed, true);
  assert.equal(tameRes.data.hasBond, true);
  const mountId = tameRes.data.mount.id;

  // Mount beast
  const mountAct = bridge.mountBeast(mountId);
  assert.equal(mountAct.ok, true);
  assert.equal(mountAct.data.mounted, true);

  // Mounted charge
  const chargeRes = bridge.executeMountedCharge(mountId, 6);
  assert.equal(chargeRes.ok, true);
  assert.equal(chargeRes.data.totalDamage, 9); // 6 + 3 silhouette

  // Beast panic check
  const panicRes = bridge.checkBeastPanic(mountId, 15, { successes: 0 });
  assert.equal(panicRes.ok, true);
  assert.equal(panicRes.data.panicked, true);
  assert.equal(panicRes.data.thrownRider, true);

  // Dismount
  const dismountRes = bridge.dismountBeast(mountId);
  // Already thrown off by panic: throws already dismounted
  assert.equal(dismountRes.ok, false);
});
