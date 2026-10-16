import test from 'node:test';
import assert from 'node:assert/strict';
import { executeMoveSceneObject, forcePowerCapabilities } from '../app/src/main/assets/vtt/js/rules/force.mjs';

test('Force Move: manipulação de objeto persistente de cena respeita silhueta e aprimoramento de arremesso', () => {
  const charWithoutMove = { force: { rating: 2, committed: 0, powers: { move: { purchased: false, upgrades: [] } } } };
  assert.throws(() => executeMoveSceneObject(charWithoutMove, { id: 'obj1', silhouette: 1 }), /não foi adquirido/i);

  const charWithBasicMove = { force: { rating: 2, committed: 0, powers: { move: { purchased: true, upgrades: [] } } } };
  // sil 0 is allowed with 0 strength upgrades
  const res0 = executeMoveSceneObject(charWithBasicMove, { id: 'crate0', silhouette: 0 }, { destinationRange: 'Médio' });
  assert.equal(res0.success, true);
  assert.equal(res0.sceneObject.range, 'Médio');
  assert.equal(res0.damage, 0);

  // sil 1 fails without strength upgrades
  assert.throws(() => executeMoveSceneObject(charWithBasicMove, { id: 'crate1', silhouette: 1 }), /excede a capacidade/i);

  // with strength upgrade and hurl
  const charWithHurl = {
    force: {
      rating: 3,
      committed: 0,
      powers: {
        move: {
          purchased: true,
          upgrades: ['strength_1', 'control_hurl']
        }
      }
    }
  };
  const resHurl = executeMoveSceneObject(charWithHurl, { id: 'boulder', silhouette: 1 }, { destinationRange: 'Curto', hurlAtTarget: 'npc_trooper' });
  assert.equal(resHurl.success, true);
  assert.equal(resHurl.damage, 10);
  assert.equal(resHurl.sceneObject.hurlTarget, 'npc_trooper');
  assert.equal(resHurl.sceneObject.impactDamage, 10);
});
