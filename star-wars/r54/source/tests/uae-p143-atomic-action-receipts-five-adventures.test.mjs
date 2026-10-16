/** P143 — executable, idempotent, journal-audited multi-command actions. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy = structuredClone;

for (const { id } of publishedAdventureCatalog()) {
  const module = publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  const prefix = `p143:${id}:`;
  const commands = [
    { type: 'event', eventType: 'SCENE_ACTION', payload: { choice: 'investigate', phase: 1 } },
    { type: 'advance-clock', clockId: 'p143-clock', delta: 2 },
    { type: 'progress-objective', objectiveId: 'episode-1', delta: 0.25 },
    { type: 'record-consequence', consequence: { id: 'p143-consequence', title: 'Consequência persistente' } },
    { type: 'effects', effects: [{ type: 'set-flag', flag: 'p143-observed', value: true }] }
  ];
  test(`P143 ${id}: commit atômico de evento, relógio, objetivo, consequência e flag`, () => {
    let runtime = new AdventureRuntime(module);
    const start = runtime.getState();
    const result = runtime.commitAction(prefix + 'action', commands);
    assert.equal(result.applied, true);
    const state = runtime.getState();
    assert.equal(state.clocks['p143-clock'].value, 2);
    assert.equal(state.objectives['episode-1'].progress, 0.25);
    assert.equal(state.flags['p143-observed'], true);
    assert.equal(state.consequences[0].id, 'p143-consequence');
    const receipt = state.canonicalEvents.at(-1);
    assert.equal(receipt.id, result.receiptId);
    assert.equal(receipt.type, 'ADVENTURE_ACTION_COMMITTED');
    assert.equal(receipt.source, 'engine');
    assert.equal(receipt.payload.actionId, prefix + 'action');
    assert.equal(receipt.payload.firstSequence, start.eventSequence + 1);
    assert.equal(receipt.payload.lastSequence, state.eventSequence - 1);
    assert.deepEqual(receipt.payload.appliedEventIds, state.canonicalEvents.slice(1, -1).map(e => e.id));
    assert.equal(validatePublishedAdventureRuntimeState(module, state), true);
    for (let i = 0; i < 3; i++) {
      runtime = AdventureRuntime.restore(module, JSON.stringify(runtime.getState()));
      assert.deepEqual(runtime.getState(), state);
      const replay = runtime.commitAction(prefix + 'action', copy(commands));
      assert.equal(replay.applied, false);
      assert.equal(replay.receiptId, result.receiptId);
      assert.deepEqual(runtime.getState(), state);
    }
    assert.throws(() => runtime.commitAction(prefix + 'action', [{ type: 'advance-clock', clockId: 'p143-clock', delta: 5 }]), /reutilizado/);
    assert.deepEqual(runtime.getState(), state);
  });
  test(`P143 ${id}: falha no último comando reverte todos os efeitos e journal`, () => {
    const runtime = new AdventureRuntime(module);
    const before = runtime.getState();
    assert.throws(() => runtime.commitAction(prefix + 'rollback', [
      ...commands,
      { type: 'complete-objective', objectiveId: 'objective-that-does-not-exist' }
    ]), /desconhecido/);
    assert.deepEqual(runtime.getState(), before);
    assert.equal(runtime.commitAction(prefix + 'rollback', commands).applied, true);
    assert.equal(validateAdventureCheckpoint(module, runtime.getState()), true);
  });
  test(`P143 ${id}: falsificação de recibo, origem e ordem causal é rejeitada`, () => {
    const runtime = new AdventureRuntime(module);
    runtime.commitAction(prefix + 'audit', commands);
    const valid = runtime.getState();
    const mutators = [
      s => { s.canonicalEvents.at(-1).source = 'adapter'; },
      s => { s.canonicalEvents.at(-1).idempotencyKey = 'other'; },
      s => { s.canonicalEvents.at(-1).payload.firstSequence += 1; },
      s => { s.canonicalEvents.at(-1).payload.lastSequence -= 1; },
      s => { s.canonicalEvents.at(-1).payload.appliedEventIds.pop(); },
      s => { s.canonicalEvents.at(-1).payload.appliedEventIds.reverse(); },
      s => { s.canonicalEvents.at(-1).payload.actionId = 'forged'; },
      s => { s.canonicalEvents.at(-1).payload.commands = []; }
    ];
    for (const mutate of mutators) {
      const bad = copy(valid); mutate(bad);
      assert.throws(() => AdventureRuntime.restore(module, bad), /Journal|recibo|origem/i);
    }
    assert.deepEqual(runtime.getState(), valid);
  });
  test(`P143 ${id}: injeções, parâmetros inválidos e cascatas não deixam estado parcial`, () => {
    const runtime = new AdventureRuntime(module);
    const before = runtime.getState();
    const invalid = [
      [prefix + 'bad1', [{ type: 'event', eventType: 'OBJECTIVE_COMPLETED', payload: { objectiveId: 'episode-1' } }]],
      [prefix + 'bad2', [{ type: 'event', eventType: 'SCENE_ACTION', source: 'engine' }]],
      [prefix + 'bad3', [{ type: 'advance-clock', clockId: 'p143-clock', delta: NaN }]],
      [prefix + 'bad4', [{ type: 'effects', effects: [{ type: 'no-such-effect' }] }]],
      [prefix + 'bad5', [{ type: 'fail-objective', objectiveId: '__proto__' }]],
      [prefix + 'bad6', [{ type: 'resolve-consequence', consequenceId: 'missing' }]],
      [prefix + 'bad7', [{ type: 'unimplemented-command' }]],
      [prefix + 'bad8', [{ type: 'event', eventType: 'ADVENTURE_ACTION_COMMITTED' }]]
    ];
    for (const [key, cmds] of invalid) {
      assert.throws(() => runtime.commitAction(key, cmds));
      assert.deepEqual(runtime.getState(), before);
    }
    assert.throws(() => runtime.commitAction('', commands), /Chave|identificador/);
    assert.throws(() => runtime.commitAction(prefix + 'empty', []), /entre 1 e 32/);
    assert.throws(() => runtime.commitAction(prefix + 'many', Array.from({ length: 33 }, () => commands[0])), /entre 1 e 32/);
    assert.deepEqual(runtime.getState(), before);
  });
  test(`P143 ${id}: 120 ações persistentes + três reaberturas sem duplicação`, () => {
    let runtime = new AdventureRuntime(module);
    for (let turn = 1; turn <= 120; turn++) {
      const cmds = [
        { type: 'event', eventType: 'P143_TURN', payload: { turn } },
        { type: 'effects', effects: [{ type: 'adjust-counter', counter: 'p143-turns', delta: 1 }] }
      ];
      const key = `${prefix}turn-${turn}`;
      assert.equal(runtime.commitAction(key, cmds).applied, true);
      if (turn % 40 === 0) {
        for (let reopen = 0; reopen < 3; reopen++) {
          runtime = AdventureRuntime.restore(module, JSON.stringify(runtime.getState()));
          assert.equal(runtime.commitAction(key, cmds).applied, false);
        }
      }
    }
    const snapshot = runtime.getState();
    assert.equal(snapshot.counters['p143-turns'], 120);
    assert.equal(snapshot.canonicalEvents.filter(e => e.type === 'P143_TURN').length, 120);
    assert.equal(snapshot.canonicalEvents.filter(e => e.type === 'ADVENTURE_ACTION_COMMITTED').length, 120);
    assert.equal(validatePublishedAdventureRuntimeState(module, snapshot), true);
  });
  test(`P143 ${id}: escolha do grafo e gatilhos persistem no mesmo recibo`, () => {
    const runtime = new AdventureRuntime(module);
    const choice = runtime.getAvailableChoices()[0];
    assert.ok(choice, 'aventura publicada deve oferecer escolha inicial');
    const result = runtime.commitAction(prefix + 'choice', [{ type: 'transition', choiceId: choice.id }]);
    assert.equal(result.applied, true);
    assert.equal(runtime.state.currentNodeId, choice.to);
    const saved = runtime.getState();
    assert.equal(validateAdventureCheckpoint(module, saved), true);
    assert.equal(AdventureRuntime.restore(module, JSON.stringify(saved)).commitAction(prefix + 'choice', [{ type: 'transition', choiceId: choice.id }]).applied, false);
  });
}

test('P143: recibo registra também cascatas e eventos internos de objetivo', () => {
  const module = publishedAdventureRuntimeModule(getPublishedAdventureAdapter('under-black-sun').meta);
  module.triggers.push({ id: 'p143-cascade', event: 'P143_ACTION_EVENT', maxFires: 1,
    effects: [{ type: 'complete-objective', objectiveId: 'episode-1' }, { type: 'advance-clock', clockId: 'p143-cascade-clock', delta: 6 }] });
  const runtime = new AdventureRuntime(module);
  runtime.commitAction('p143:cascade', [{ type: 'event', eventType: 'P143_ACTION_EVENT' }]);
  const events = runtime.getState().canonicalEvents;
  assert.ok(events.some(e => e.type === 'TRIGGER_FIRED'));
  assert.ok(events.some(e => e.type === 'OBJECTIVE_COMPLETED'));
  assert.ok(events.some(e => e.type === 'CLOCK_COMPLETED'));
  assert.deepEqual(events.at(-1).payload.appliedEventIds, events.slice(1, -1).map(e => e.id));
  assert.equal(validateAdventureCheckpoint(module, runtime.getState()), true);
});
