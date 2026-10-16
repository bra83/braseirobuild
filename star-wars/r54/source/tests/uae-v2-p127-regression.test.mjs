import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureModule, migrateAdventureState } from '../app/src/main/assets/vtt/js/adventure/adventureSchema.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

const fixture = () => ({
  id: 'uae-p127', title: 'Motor de integração', campaignModes: ['EOTE', 'AOR', 'MIXED'], startNodeId: 'start',
  initialObjectives: { intel: { title: 'Obter dados', type: 'PRIMARY', target: 2 }, hidden: { type: 'HIDDEN', target: 1 } },
  initialClocks: { alarm: { value: 0, max: 2, status: 'ACTIVE' } },
  nodes: {
    start: { title: 'Início', choices: [{ id: 'enter', to: 'corridor', effects: [{ type: 'advance-clock', clockId: 'alarm', delta: 1 }] }] },
    corridor: { title: 'Corredor', onEnter: [{ type: 'add-clue', clueId: 'data', title: 'Dados', summary: 'Prova' }], choices: [{ id: 'finish', to: 'end', condition: 'all:flag:access|objective:intel:COMPLETED' }] },
    end: { title: 'Fim', terminal: true, choices: [] }
  },
  triggers: [
    { id: 'on-clue', event: 'CLUE_DISCOVERED', maxFires: 1, priority: 3, effects: [{ type: 'progress-objective', objectiveId: 'intel', delta: 1 }] },
    { id: 'on-enter', event: 'NODE_ENTER', condition: 'payload:to:corridor', maxFires: 1, effects: [{ type: 'progress-objective', objectiveId: 'intel', delta: 1 }, { type: 'set-flag', flag: 'access' }] },
    { id: 'on-complete', event: 'OBJECTIVE_COMPLETED', condition: 'objective:intel:COMPLETED', maxFires: 1, consequence: { id: 'intel-acquired', title: 'Intel obtida' } }
  ]
});

test('P127 UAE: cadeia evento → trigger → objetivo → consequência em transição atômica', () => {
  const runtime = new AdventureRuntime(fixture());
  const state = runtime.transition('enter');
  assert.equal(state.currentNodeId, 'corridor');
  assert.equal(state.objectives.intel.status, 'COMPLETED');
  assert.equal(state.triggersFired['on-clue'], 1);
  assert.equal(state.triggersFired['on-enter'], 1);
  assert.equal(state.triggersFired['on-complete'], 1);
  assert.equal(state.consequences.length, 1);
  assert.equal(state.consequences[0].id, 'intel-acquired');
  assert.equal(state.clocks.alarm.value, 1);
  assert.deepEqual(runtime.getAvailableChoices().map(c => c.id), ['finish']);
  assert.equal(state.canonicalEvents.find(e => e.type === 'NODE_TRANSITION').payload.from, 'start');
  assert.equal(state.canonicalEvents.find(e => e.type === 'NODE_ENTER').payload.from, 'start');
  const ids = state.canonicalEvents.map(e => e.id);
  assert.equal(new Set(ids).size, ids.length);
  assert.ok(ids.every(id => /^evt_uae-p127_\d+$/.test(id)));
  assert.equal(runtime.transition('finish').status, 'COMPLETED');
  assert.throws(() => runtime.transition('finish'), /não está ativa/);
  assert.equal(runtime.archive().status, 'ARCHIVED');
});

test('P127 UAE: snapshot JSON restaura efeitos sem reaplicar gatilhos e sem reabrir objetivo', () => {
  const module = fixture();
  const runtime = new AdventureRuntime(module);
  runtime.transition('enter');
  const snapshot = runtime.serialize();
  const restored = AdventureRuntime.restore(module, snapshot);
  assert.deepEqual(restored.getState(), runtime.getState());
  restored.dispatchEvent('CLUE_DISCOVERED', { clueId: 'another' });
  assert.equal(restored.getState().triggersFired['on-clue'], 1);
  assert.equal(restored.getState().consequences.length, 1);
  assert.equal(restored.getState().objectives.intel.progress, 2);
  assert.equal(runtime.getState().canonicalEvents.length < restored.getState().canonicalEvents.length, true);
});

test('P127 UAE: escolha inválida e erro no efeito não deixam mutações parciais', () => {
  const module = fixture();
  module.nodes.start.choices[0].effects = [
    { type: 'set-flag', flag: 'partial' },
    { type: 'advance-clock', clockId: 'alarm', delta: 'not-a-number' }
  ];
  const runtime = new AdventureRuntime(module);
  const before = runtime.getState();
  assert.throws(() => runtime.transition('enter'), /Variação de relógio inválida/);
  assert.deepEqual(runtime.getState(), before);
  assert.throws(() => runtime.transition('invalid'), /não encontrada/);
  assert.deepEqual(runtime.getState(), before);
});

test('P127 UAE: desconhecido não libera escolhas e lógica all/any não permite bypass', () => {
  const runtime = new AdventureRuntime(fixture());
  assert.equal(runtime.isConditionMet('nonexistent:feature'), false);
  assert.equal(runtime.isConditionMet('all:flag:missing|!flag:missing'), false);
  assert.equal(runtime.isConditionMet('any:flag:missing|!flag:missing'), true);
  assert.equal(runtime.isConditionMet(['!flag:missing', 'flag:missing']), false);
});

test('P127 UAE: relógio concluído dispara uma única vez e resolve consequência', () => {
  const module = fixture();
  module.triggers.push({ id: 'clock', event: 'CLOCK_COMPLETED', maxFires: 1, condition: 'payload:clockId:alarm', consequence: {id:'alert-raised',onResolveEffects:{alertHandled:true}} });
  const runtime = new AdventureRuntime(module);
  runtime.advanceClock('alarm', 2);
  runtime.advanceClock('alarm', 1);
  assert.equal(runtime.getState().triggersFired.clock, 1);
  assert.equal(runtime.getState().consequences.filter(x => x.id === 'alert-raised').length, 1);
  runtime.resolveConsequence('alert-raised');
  assert.equal(runtime.getState().consequences[0].resolved, true);
  assert.equal(runtime.getState().flags.alertHandled, true);
});

test('P127 UAE: ciclo de gatilhos termina com limite auditável sem travamento', () => {
  const module = fixture();
  module.triggers = [{ id: 'loop', event: 'PING', effects: [{ type: 'emit-event', eventType: 'PING' }] }];
  const runtime = new AdventureRuntime(module);
  const fired = runtime.dispatchEvent('PING');
  assert.equal(fired.length, 10);
  assert.equal(runtime.getState().triggersFired.loop, 10);
  assert.equal(runtime.getState().canonicalEvents.filter(e => e.type === 'TRIGGER_CASCADE_LIMIT').length, 1);
});

test('P127 UAE: validação rejeita links quebrados, IDs duplicados e gatilhos malformados', () => {
  const a = fixture();
  a.nodes.start.choices.push({ id: 'enter', to: 'end' });
  assert.throws(() => validateAdventureModule(a), /duplicada/);
  const b = fixture();
  b.nodes.start.choices[0].to = 'missing';
  assert.throws(() => validateAdventureModule(b), /inexistente/);
  const c = fixture();
  c.triggers.push({ id: 'on-clue', event: 'PING' });
  assert.throws(() => validateAdventureModule(c), /duplicado/);
});

test('P127 UAE: migração é idempotente, impede cruzamento de aventura e snapshot futuro', () => {
  const module = fixture();
  const initial = new AdventureRuntime(module).getState();
  const old = { ...initial, schemaVersion: 1, objectives: {}, clocks: {}, eventSequence: undefined };
  const once = migrateAdventureState(old, module);
  const twice = migrateAdventureState(once, module);
  assert.deepEqual(once, twice);
  assert.ok(once.objectives.intel);
  assert.ok(once.clocks.alarm);
  assert.throws(() => migrateAdventureState({ ...old, adventureId: 'other' }, module), /outra aventura/);
  assert.throws(() => migrateAdventureState({ ...old, schemaVersion: 999 }, module), /versão futura/);
  assert.throws(() => migrateAdventureState({ ...old, currentNodeId: 'deleted' }, module), /inexistente/);
});

test('P127 UAE: as cinco aventuras publicadas preservam grafo íntegro, adaptadores e restauração', () => {
  const catalog = publishedAdventureCatalog();
  assert.equal(catalog.length, 5);
  for (const { id } of catalog) {
    const adapter = getPublishedAdventureAdapter(id);
    assert.equal(validateAdventureModule(adapter.meta), true, id);
    const runtime = new AdventureRuntime(adapter.meta);
    assert.equal(AdventureRuntime.restore(adapter.meta, runtime.serialize()).getState().currentNodeId, adapter.meta.startNodeId);
    assert.equal(typeof adapter.start, 'function');
    assert.equal(typeof adapter.advance, 'function');
    assert.equal(typeof adapter.recordEncounterOutcome, 'function');
    assert.equal(typeof adapter.markRewardsApplied, 'function');
    assert.equal(typeof adapter.archive, 'function');
  }
});
