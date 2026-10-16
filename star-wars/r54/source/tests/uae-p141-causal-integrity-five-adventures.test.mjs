import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { auditUaeJournalCausality } from '../app/src/main/assets/vtt/js/adventure/uaeJournalCausality.mjs';
import { validatePublishedAdventureGraph } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState, publishedAdventureUaeSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy = structuredClone;

for (const { id } of publishedAdventureCatalog()) {
  const adapter = getPublishedAdventureAdapter(id);
  const module = publishedAdventureRuntimeModule(adapter.meta);
  const graph = validatePublishedAdventureGraph(adapter.meta);
  const firstNextEpisode = Object.entries(module.nodes).find(([,node]) => node.episode === 2)?.[0];

  test(`P141 ${id}: grafo completo e rota estrutural para desfecho`, () => {
    assert.equal(graph.adventureId, id);
    assert.equal(graph.nodes, Object.keys(adapter.meta.nodes).length);
    assert.ok(graph.choices >= graph.nodes - 1);
    assert.ok(graph.endings >= 1);
    assert.deepEqual(graph.episodes, [1, 2, 3]);
  });
  test(`P141 ${id}: primeira transição oficial, JSON e reabertura preservam causa e estado`, () => {
    const started = adapter.start({});
    const first = started.node.choices[0];
    const advanced = adapter.advance(started.world, first.id);
    const before = copy(advanced.world.publishedAdventureRuntime);
    assert.equal(validatePublishedAdventureRuntimeState(module, before, advanced.world.publishedAdventure), true);
    assert.ok(auditUaeJournalCausality(module, before).events >= 3);
    const reopened = copy(advanced.world);
    reopened.publishedAdventureRuntime = JSON.parse(JSON.stringify(before));
    for (let i = 0; i < 3; i++) {
      const state = publishedAdventureUaeSnapshot(reopened, adapter);
      assert.deepEqual(state, before);
      reopened.publishedAdventureRuntime = AdventureRuntime.restore(module, JSON.stringify(state)).getState();
    }
    assert.deepEqual(advanced.world.publishedAdventureRuntime, before);
  });
  test(`P141 ${id}: gatilho real de marco gera contador e journal causal íntegros`, () => {
    const runtime = new AdventureRuntime(module);
    const eventType = 'NODE_ENTER';
    runtime.dispatchEvent(eventType, {from:module.startNodeId, to:firstNextEpisode});
    const saved = runtime.getState();
    const fired = saved.canonicalEvents.filter(e => e.type === 'TRIGGER_FIRED');
    assert.ok(fired.length >= 1, 'o gatilho de episódio foi executado');
    assert.equal(saved.objectives['episode-1'].status, 'COMPLETED');
    assert.equal(validatePublishedAdventureRuntimeState(module, saved), true);
    const invalid = copy(saved);
    invalid.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').payload.eventType = 'NON_EXISTENT_EVENT';
    assert.throws(() => AdventureRuntime.restore(module, invalid), /gatilho|causal/i);
    assert.equal(validateAdventureCheckpoint(module, saved), true);
  });
  test(`P141 ${id}: alteração de contador, objetivo e evento é recusada sem reparo`, () => {
    const runtime = new AdventureRuntime(module);
    runtime.dispatchEvent('NODE_ENTER', {to:firstNextEpisode});
    const valid = runtime.getState();
    const firedId = Object.keys(valid.triggersFired)[0];
    const variants = [
      s => {s.triggersFired[firedId] = 0;},
      s => {s.objectives['episode-1'].title = 'Objetivo falsificado';},
      s => {s.objectives['episode-1'].type = 'HIDDEN';},
      s => {s.objectives['episode-1'].target = 5;},
      s => {s.objectives['episode-1'].id = 'different';},
      s => {s.canonicalEvents.find(e => e.type === 'TRIGGER_FIRED').payload.triggerId = 'invented-trigger';},
      s => {s.canonicalEvents.find(e => e.type === 'OBJECTIVE_COMPLETED').payload.objectiveId = 'invented-objective';}
    ];
    for (const mutate of variants) {
      const bad = copy(valid); mutate(bad);
      assert.throws(() => validatePublishedAdventureRuntimeState(module, bad), /gatilho|objetivo|Journal|histórico|causal/i);
      assert.throws(() => AdventureRuntime.restore(module, bad));
    }
    assert.deepEqual(runtime.getState(), valid);
  });
  test(`P141 ${id}: consequência registrada/resolvida não aceita histórico falso`, () => {
    const runtime = new AdventureRuntime(module);
    runtime.recordConsequence({id:'p141-test-consequence',title:'Resultado auditável'});
    runtime.resolveConsequence('p141-test-consequence');
    const valid = runtime.getState();
    assert.equal(auditUaeJournalCausality(module, valid).resolutions, 1);
    assert.equal(validatePublishedAdventureRuntimeState(module, valid), true);
    for (const mutate of [
      s => {s.canonicalEvents.find(e => e.type === 'CONSEQUENCE_RECORDED').payload.consequenceId = 'unknown';},
      s => {s.canonicalEvents.find(e => e.type === 'CONSEQUENCE_RESOLVED').payload.consequenceId = 'unknown';},
      s => {s.consequences[0].resolved = false;}
    ]) {
      const invalid = copy(valid); mutate(invalid);
      assert.throws(() => validatePublishedAdventureRuntimeState(module, invalid), /consequência|Journal|resolução/i);
    }
  });
  test(`P141 ${id}: rollback transacional preserva journal, objetivo, consequência e clock`, () => {
    const runtime = new AdventureRuntime(module);
    const before = runtime.getState();
    const clockId = Object.keys(before.clocks)[0];
    assert.throws(() => runtime.transaction(r => {
      r.dispatchEvent('NODE_ENTER', {to:firstNextEpisode});
      r.recordConsequence('p141-rollback');
      r.advanceClock(clockId, 1);
      r.state.objectives['episode-1'].title = 'adulterado';
    }), /Objetivo UAE adulterado/);
    assert.deepEqual(runtime.getState(), before);
  });
}

test('P141: módulo publicado sem desfecho ou com cena isolada falha antes de jogar', () => {
  const adapter = getPublishedAdventureAdapter('jewel-of-yavin');
  const disconnected = copy(adapter.meta);
  disconnected.nodes['isolated-p141'] = {title:'Isolado',episode:1,choices:[]};
  assert.throws(() => validatePublishedAdventureGraph(disconnected), /inalcançáveis/);
  const noEnding = copy(adapter.meta);
  for (const [id,node] of Object.entries(noEnding.nodes)) {
    node.terminal = false;
    if (!node.choices?.length) node.choices = [{id:'cycle',to:noEnding.startNodeId}];
  }
  assert.throws(() => validatePublishedAdventureGraph(noEnding), /sem desfecho/);
});

test('P141: contrato de visibilidade e alvo do objetivo não pode ser adulterado', () => {
  const module = {
    id:'p141-hidden',title:'Teste de sigilo',campaignModes:['EOTE'],startNodeId:'start',
    nodes:{start:{title:'Início',choices:[]}},
    initialObjectives:{secret:{title:'Segredo GM',type:'HIDDEN',target:3}},
    initialClocks:{alert:{value:0,max:6,status:'ACTIVE'}}
  };
  const runtime = new AdventureRuntime(module);
  const original = runtime.getState();
  for (const [key,value] of [['type','PRIMARY'],['title','Visível'],['target',1],['id','other']]) {
    const invalid = copy(original); invalid.objectives.secret[key] = value;
    assert.throws(() => validateAdventureCheckpoint(module, invalid), /Objetivo UAE adulterado/);
  }
  assert.equal(validateAdventureCheckpoint(module, original), true);
});
