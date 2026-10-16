import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot,
  validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

const clone = structuredClone;
const adapter = getPublishedAdventureAdapter('under-black-sun');
const source = () => publishedAdventureRuntimeModule(adapter.meta);
const badTrigger = event => {
  const module = source();
  module.triggers.push({id:`p133-fail-${event}`,event,maxFires:1,
    effects:[{type:'adjust-counter',counter:'broken',delta:'invalid'}]});
  return new AdventureRuntime(module);
};

for (const [event,mutate] of [
  ['OBJECTIVE_PROGRESS',r=>r.updateObjectiveProgress('episode-1',0.25)],
  ['OBJECTIVE_COMPLETED',r=>r.completeObjective('episode-1')],
  ['OBJECTIVE_FAILED',r=>r.failObjective('episode-1')],
  ['CONSEQUENCE_RECORDED',r=>r.recordConsequence('alert')],
  ['CLUE_DISCOVERED',r=>r.addClue('a','Pista','Resumo')],
  ['SECRET_REVEALED',r=>r.revealSecret('s','Segredo')]
]) {
  test(`P133 UAE ${event}: falha de gatilho desfaz estado, journal e sequência`,()=>{
    const runtime=badTrigger(event),before=runtime.getState();
    assert.throws(()=>mutate(runtime),/Variação de contador inválida/);
    assert.deepEqual(runtime.getState(),before);
  });
}

test('P133 UAE: transação composta desfaz relógio, flag, objetivo e journal se etapa final falhar',()=>{
  const runtime=new AdventureRuntime(source());
  const before=runtime.getState();
  assert.throws(()=>runtime.transaction(r=>{
    r.applyEffects([{type:'set-flag',flag:'new-clue',value:true}]);
    r.advanceClock('alertClock',1);
    r.updateObjectiveProgress('episode-1',0.5);
    r.recordConsequence('alert');
    r.dispatchEvent('NEW_SCENE',{episode:1});
    r.applyEffects([{type:'adjust-counter',counter:'heat',delta:'invalid'}]);
  }),/Variação de contador inválida/);
  assert.deepEqual(runtime.getState(),before);
});

test('P133 UAE: validação de chave protege contra prototype pollution e rollback',()=>{
  const runtime=new AdventureRuntime(source()),before=runtime.getState();
  for(const operation of [
    ()=>runtime.applyEffects([{type:'set-flag',flag:'__proto__',value:{polluted:true}}]),
    ()=>runtime.applyEffects([{type:'set-counter',counter:'constructor',value:10}]),
    ()=>runtime.advanceClock('__proto__',1),
    ()=>runtime.applyEffects(JSON.parse('{"__proto__":true}'))
  ]) {
    assert.throws(operation,/inválid[ao]/);
    assert.deepEqual(runtime.getState(),before);
  }
  assert.equal({}.polluted,undefined);
});

test('P133 UAE: overflow numérico de contadores não gera estado impossível',()=>{
  const runtime=new AdventureRuntime(source()),before=runtime.getState();
  assert.throws(()=>runtime.applyEffects([
    {type:'set-counter',counter:'heat',value:1e308},
    {type:'adjust-counter',counter:'heat',delta:1e308}
  ]),/fora dos limites numéricos/);
  assert.deepEqual(runtime.getState(),before);
  assert.throws(()=>runtime.applyEffects({ 'set-counter':{counter:'heat',value:''}}),/Valor de contador inválido/);
  assert.deepEqual(runtime.getState(),before);
});

test('P133 UAE: sequência esgotada não duplica eventos nem perde atomicidade',()=>{
  const runtime=new AdventureRuntime(source());
  runtime.state.eventSequence=Number.MAX_SAFE_INTEGER;
  const before=runtime.getState();
  assert.throws(()=>runtime.recordConsequence('x'),/Sequência de eventos esgotada/);
  assert.deepEqual(runtime.getState(),before);
});

for(const {id} of publishedAdventureCatalog()) {
  test(`P133 ${id}: cinco aventuras preservam consequências, relógios e objetivos no JSON`,()=>{
    const a=getPublishedAdventureAdapter(id);
    const world=a.start({}).world;
    const module=publishedAdventureRuntimeModule(a.meta);
    const runtime=AdventureRuntime.restore(module,world.publishedAdventureRuntime);
    const clockId=Object.keys(runtime.state.clocks)[0];
    runtime.transaction(r=>{
      r.recordConsequence('p133-suspicion');
      r.resolveConsequence('p133-suspicion');
      r.updateObjectiveProgress('episode-1',0.5);
      if(clockId) r.advanceClock(clockId,1);
      r.dispatchEvent('P133_INTEGRATION_CHECK',{adventureId:id});
    });
    const saved=clone(world);
    saved.publishedAdventureRuntime=JSON.parse(runtime.serialize());
    assert.equal(validatePublishedAdventureRuntimeState(module,saved.publishedAdventureRuntime,saved.publishedAdventure),true);
    const restored=publishedAdventureUaeSnapshot(JSON.parse(JSON.stringify(saved)),a);
    assert.deepEqual(restored,saved.publishedAdventureRuntime);
    assert.equal(restored.consequences.find(c=>c.id==='p133-suspicion').resolved,true);
    assert.equal(restored.objectives['episode-1'].progress,0.5);
    assert.ok(restored.canonicalEvents.some(e=>e.type==='P133_INTEGRATION_CHECK'));
  });
}
