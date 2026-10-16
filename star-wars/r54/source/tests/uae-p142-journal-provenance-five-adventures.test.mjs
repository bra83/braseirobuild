/** P142 — real adapter + UAE security/integration certification. No new book
 * encounters or talents are invented: only executable state transitions. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy = structuredClone;

for (const { id } of publishedAdventureCatalog()) {
  const adapter = getPublishedAdventureAdapter(id);
  const module = publishedAdventureRuntimeModule(adapter.meta);
  const episode2 = Object.entries(module.nodes).find(([, node]) => node.episode === 2)?.[0];
  test(`P142 ${id}: journal vincula gatilho a evento causal real e persiste três reaberturas`, () => {
    let runtime = new AdventureRuntime(module);
    runtime.dispatchEvent('NODE_ENTER', { from: module.startNodeId, to: episode2 }, 'adapter');
    const before = runtime.getState();
    const fired = before.canonicalEvents.filter(e => e.type === 'TRIGGER_FIRED');
    assert.ok(fired.length > 0);
    for (const event of fired) {
      assert.ok(event.payload.causeEventId);
      const cause = before.canonicalEvents.find(e => e.id === event.payload.causeEventId);
      assert.ok(cause);
      assert.equal(cause.type, event.payload.eventType);
      assert.ok(before.canonicalEvents.indexOf(cause) < before.canonicalEvents.indexOf(event));
    }
    assert.equal(before.causalAuditFromSequence, 1);
    for (let reopen = 0; reopen < 3; reopen++) {
      runtime = AdventureRuntime.restore(module, JSON.stringify(runtime.getState()));
      assert.equal(validatePublishedAdventureRuntimeState(module, runtime.getState()), true);
      assert.deepEqual(runtime.getState(), before);
    }
  });
  test(`P142 ${id}: falsificação de causa, duplicação e downgrade são rejeitados`, () => {
    const runtime = new AdventureRuntime(module);
    runtime.dispatchEvent('NODE_ENTER', { to: episode2 }, 'adapter');
    const good = runtime.getState();
    const event = good.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED');
    assert.ok(event);
    const corruptions = [
      s => { s.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').payload.causeEventId = 'evt_' + id + '_999999'; },
      s => { s.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').payload.causeEventId = s.canonicalEvents.find(e => e.type === 'ADVENTURE_STARTED').id; },
      s => { delete s.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').payload.causeEventId; },
      s => { s.causalAuditFromSequence = -1; },
      s => { s.causalAuditFromSequence = s.eventSequence + 2; },
      s => { s.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').source = 'adapter'; }
    ];
    for (const mutate of corruptions) {
      const bad = copy(good); mutate(bad);
      assert.throws(() => AdventureRuntime.restore(module, bad), /causal|Journal|gatilho|Versão|origem/i);
      assert.throws(() => validateAdventureCheckpoint(module, bad));
    }
    assert.deepEqual(runtime.getState(), good);
  });
  test(`P142 ${id}: origem interna e eventos de sistema não podem ser injetados`, () => {
    const runtime = new AdventureRuntime(module);
    const before = runtime.getState();
    for (const [event, source] of [
      ['TRIGGER_FIRED','runtime'], ['CONSEQUENCE_RECORDED','adapter'],
      ['CONSEQUENCE_RESOLVED','effect'], ['OBJECTIVE_COMPLETED','runtime'],
      ['OBJECTIVE_FAILED','adapter'], ['TRIGGER_CASCADE_LIMIT','runtime'],
      ['OBJECTIVE_COMPLETED','engine'], ['CUSTOM_EVENT','engine']
    ]) {
      assert.throws(() => runtime.dispatchEvent(event, {objectiveId:'episode-1'}, source), /interno|reservada/i);
    }
    assert.throws(() => runtime.applyEffects([{type:'emit-event',eventType:'OBJECTIVE_COMPLETED',payload:{objectiveId:'episode-1'}}]), /interno/i);
    assert.throws(() => runtime.applyEffects({'emit-event':{type:'OBJECTIVE_COMPLETED',source:'engine',payload:{objectiveId:'episode-1'}}}), /reservada/i);
    assert.deepEqual(runtime.getState(), before);
  });
  test(`P142 ${id}: consequência, objetivo e relógio continuam atômicos após replay`, () => {
    let runtime = new AdventureRuntime(module);
    const clockId = Object.keys(runtime.state.clocks)[0] || 'p142-clock';
    runtime.transaction(r => {
      r.recordConsequence({id:'p142-result',title:'Resultado de teste'});
      r.resolveConsequence('p142-result');
      r.advanceClock(clockId, 1);
      r.completeObjective('episode-1');
    });
    const valid = runtime.getState();
    assert.equal(validateAdventureCheckpoint(module, valid), true);
    runtime = AdventureRuntime.restore(module, JSON.stringify(valid));
    const before = runtime.getState();
    assert.throws(() => runtime.transaction(r => {
      r.dispatchEvent('NODE_ENTER', {to:episode2}, 'adapter');
      r.advanceClock(clockId, 1);
      r.recordConsequence('p142-rollback');
      r.dispatchEvent('TRIGGER_FIRED', {triggerId:'fake'}, 'runtime');
    }), /interno/i);
    assert.deepEqual(runtime.getState(), before);
  });
  test(`P142 ${id}: conclusão de relógio exige avanço causal verificável`, () => {
    const runtime = new AdventureRuntime(module);
    const clockId = Object.keys(runtime.state.clocks)[0] || 'p142-chronometer';
    const max = runtime.state.clocks[clockId]?.max || 6;
    runtime.advanceClock(clockId, max);
    const valid = runtime.getState();
    const advance = valid.canonicalEvents.find(e => e.type === 'CLOCK_ADVANCED' && e.payload.clockId === clockId);
    const completed = valid.canonicalEvents.find(e => e.type === 'CLOCK_COMPLETED' && e.payload.clockId === clockId);
    assert.ok(advance && completed);
    assert.equal(validateAdventureCheckpoint(module, valid), true);
    for (const mutate of [
      s => {s.canonicalEvents.find(e => e.id === completed.id).payload.value = max - 1;},
      s => {s.canonicalEvents.find(e => e.id === advance.id).payload.status = 'ACTIVE';},
      s => {s.canonicalEvents.find(e => e.id === advance.id).payload.value = -1;},
      s => {s.canonicalEvents.find(e => e.id === advance.id).payload.max = 0;},
      s => {s.canonicalEvents.find(e => e.id === completed.id).payload.clockId = 'forged-clock';}
    ]) {
      const invalid = copy(valid); mutate(invalid);
      assert.throws(() => validateAdventureCheckpoint(module, invalid), /relógio|Journal/i);
    }
    assert.deepEqual(runtime.getState(), valid);
  });
  test(`P142 ${id}: adaptador oficial preserva vínculo causal durante transição real`, () => {
    const start = adapter.start({});
    const firstChoice = start.node.choices[0];
    const advanced = adapter.advance(start.world, firstChoice.id);
    const snapshot = copy(advanced.world.publishedAdventureRuntime);
    assert.equal(validatePublishedAdventureRuntimeState(module, snapshot, advanced.world.publishedAdventure), true);
    const saved = JSON.parse(JSON.stringify(snapshot));
    const restored = AdventureRuntime.restore(module, saved);
    assert.deepEqual(restored.getState(), snapshot);
    assert.equal(snapshot.causalAuditFromSequence, 1);
  });
}

test('P142: P141 sem marcador é promovido sem reescrever causalidade antiga', () => {
  const module = publishedAdventureRuntimeModule(getPublishedAdventureAdapter('under-black-sun').meta);
  const r = new AdventureRuntime(module);
  const episode2 = Object.entries(module.nodes).find(([,node]) => node.episode === 2)?.[0];
  r.dispatchEvent('NODE_ENTER', {to:episode2});
  const legacy = r.getState();
  delete legacy.causalAuditFromSequence;
  for (const event of legacy.canonicalEvents) if (event.type === 'TRIGGER_FIRED') delete event.payload.causeEventId;
  let restored = AdventureRuntime.restore(module, legacy);
  assert.equal(restored.state.causalAuditFromSequence, undefined);
  const startSequence = restored.state.eventSequence;
  restored.dispatchEvent('NODE_ENTER', {to:episode2});
  assert.equal(restored.state.causalAuditFromSequence, startSequence + 1);
  assert.equal(validateAdventureCheckpoint(module, restored.getState()), true);
});

test('P142: gatilho acionado por evento interno legítimo mantém encadeamento causal', () => {
  const module = publishedAdventureRuntimeModule(getPublishedAdventureAdapter('under-black-sun').meta);
  module.triggers.push({id:'p142-objective-event',event:'OBJECTIVE_COMPLETED',maxFires:1,effects:[{type:'set-flag',flag:'p142-objective-seen',value:true}]});
  const runtime = new AdventureRuntime(module);
  runtime.completeObjective('episode-1');
  assert.equal(runtime.state.flags['p142-objective-seen'], true);
  const event = runtime.state.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED');
  assert.equal(runtime.state.canonicalEvents.find(e => e.id === event.payload.causeEventId).type,'OBJECTIVE_COMPLETED');
  assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
});
