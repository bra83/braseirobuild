// Regression gates for V9 hardening (tests must fail on original V9).
// OUA_MODULE_ROOT=/path/to/extracted/V9 node --test ci/oua-4.9.212/hardening.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
const root = process.env.OUA_MODULE_ROOT;
if (!root) throw new Error('OUA_MODULE_ROOT must point to extracted and patched V9');
const moduleUrl = pathToFileURL(path.join(root, 'src/ui/bridges/Tor2eAdventureController.mjs')).href;
const { Tor2eAdventureController } = await import(moduleUrl);

function fixture() {
  return [{
    id: 'hardening-fixture', number: 1, title: 'Cenário controlado',
    synopsis: '', startingLocation: 'Bri',
    scenes: [{
      id: 'scene-1', title: 'Entrada', description: 'Teste',
      checks: [{ checkId: 'hunt-1', skill: 'Caçada', tn: 99,
        mandatory: true, failureFatigue: 2 }],
      nextSceneIds: ['scene-2'],
      combatEncounter: { name: 'Emboscada', enemies: [{
        id: 'orc-1', name: 'Orc 1', endurance: 20, enduranceCurrent: 20,
        parry: 0, targetNumber: 14, armorRating: 1, wounds: 0
      }] }
    }, { id: 'scene-2', title: 'Saída', description: 'Fim',
      checks: [], nextSceneIds: [] }]
  }];
}
function ctrl(options = {}) {
  const c = new Tor2eAdventureController(fixture(), options);
  c.startAdventure('hardening-fixture');
  return c;
}
test('OUA HARD-01: enemy ID invented by the client cannot resolve combat', () => {
  const c = ctrl();
  const before = c.exportSnapshot();
  assert.throws(() => c.performSceneCombatAttack({ targetEnemyId: 'ghost' }), /Alvo|alvo|não pertence/);
  assert.deepEqual(c.exportSnapshot(), before);
  assert.equal(c.sceneState.combatResolved, false);
});
test('OUA HARD-02: mock dice and forged combat stats rejected in production', () => {
  const authoritative = () => ({
    attacker: { cultureId: 'hobbits', strengthBonus: 0 },
    weapon: { group: 'Espadas', damage: 3, injury: 16 },
    skillRank: 2
  });
  const c = ctrl({ environment: 'production', authorizeCombatAction: authoritative });
  const before = c.exportSnapshot();
  for (const field of ['mockFeatRolls','mockSuccessRolls','mockProtectionFeatRolls','mockProtectionSuccessRolls']) {
    assert.throws(() => c.performSceneCombatAttack({
      targetEnemyId: 'orc-1', [field]: [12], commandId: 'mock-' + field
    }), /proibido|simulad/i);
  }
  assert.throws(() => c.performSceneCombatAttack({
    targetEnemyId: 'orc-1', attacker: { strengthBonus: 999 },
    weapon: { damage: 999 }, skillRank: 30, commandId: 'forged'
  }), /não autorizado/i);
  assert.deepEqual(c.exportSnapshot(), before);
  const withoutHost = ctrl({ environment: 'production' });
  assert.throws(() => withoutHost.performSceneCombatAttack({
    targetEnemyId: 'orc-1', commandId: 'missing-host'
  }), /Host/i);
});
test('OUA HARD-03: a caller cannot replace the host-signed character sheet', () => {
  const c = ctrl({
    environment: 'production', authorizedCharacterSheet: {
      skills: [{ name: 'Caçada', rank: 0 }]
    }
  });
  const snap = c.exportSnapshot();
  assert.throws(() => c.performSceneCheck('hunt-1', {
    skillRank: 30, commandId: 'rank-30'
  }), /diverge|autorizad/i);
  assert.throws(() => c.performSceneCheck('hunt-1', {
    authorizedCharacterSheet: { skills: [{ name: 'Caçada', rank: 30 }] },
    commandId: 'sheet-30'
  }), /cliente|autorizad/i);
  assert.deepEqual(c.exportSnapshot(), snap);
});
test('OUA HARD-04: a caller cannot invent a retry rule string', () => {
  const c = ctrl();
  c.performSceneCheck('hunt-1', {
    commandId: 'first-check', mockFeatRolls: [1], mockSuccessRolls: [1]
  });
  const snap = c.exportSnapshot();
  assert.throws(() => c.performSceneCheck('hunt-1', {
    commandId: 'forged-retry', allowRetry: true,
    retryRuleSource: 'any invented rule', mockFeatRolls: [12],
    mockSuccessRolls: [6]
  }), /Repetição|repeti/i);
  assert.deepEqual(c.exportSnapshot(), snap);
});
test('OUA HARD-05: commandId is in durable snapshot before returning success', () => {
  let durable = null;
  const store = { save(s) { durable = structuredClone(s); } };
  const c = ctrl({ persistenceStore: store, requireCommandId: true });
  c.performSceneCheck('hunt-1', {
    commandId: 'durable-check', mockFeatRolls: [1], mockSuccessRolls: [1]
  });
  assert.ok(durable.processedCommands.some(([id]) => id === 'durable-check'));
  const reopened = new Tor2eAdventureController(fixture(), { requireCommandId: true });
  reopened.restoreFromSnapshot(durable);
  const duplicate = reopened.performSceneCheck('hunt-1', {
    commandId: 'durable-check', mockFeatRolls: [12], mockSuccessRolls: [6]
  });
  assert.equal(duplicate.status, 'DUPLICATE_COMMAND');
  assert.deepEqual(reopened.exportSnapshot(), durable);
});
test('OUA HARD-06: rejected save rolls back cached command and gameplay state', () => {
  let reject = false;
  const store = { save() { if (reject) throw new Error('disk full'); } };
  const c = ctrl({ persistenceStore: store });
  const original = c.exportSnapshot();
  reject = true;
  assert.throws(() => c.performSceneCheck('hunt-1', {
    commandId: 'rollback-check', mockFeatRolls: [1], mockSuccessRolls: [1]
  }), /rollback|Transação/i);
  assert.deepEqual(c.exportSnapshot(), original);
});
test('OUA HARD-07: strict command ID required for durable mutations', () => {
  const c = ctrl({ requireCommandId: true });
  assert.throws(() => c.performSceneCheck('hunt-1', {
    mockFeatRolls: [1], mockSuccessRolls: [1]
  }), /commandId/);
  assert.throws(() => c.performSceneCombatAttack({ targetEnemyId: 'orc-1' }), /commandId/);
});
