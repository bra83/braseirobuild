/** P153 — event-scoped conditional effects, durable objectives, clocks and consequences. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

function fixture(id) {
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  module.initialClocks.p153Clock={value:0,max:2,status:'ACTIVE'};
  module.triggers.push({
    id:'p153-parent',event:'P153_SIGNAL',condition:'payload:route:sector:7',
    effects:[
      {type:'set-flag',flag:'p153Qualified',value:true,condition:'payload:route:sector:7'},
      {type:'adjust-counter',counter:'p153Counter',delta:3,condition:'payload:choice:yes'},
      {type:'advance-clock',clockId:'p153Clock',delta:1,condition:'payload:choice:yes'},
      {type:'progress-objective',objectiveId:'adventure-finale',delta:1,condition:'payload:choice:yes'},
      {type:'emit-event',eventType:'P153_CHILD',payload:{phase:'child'},condition:'payload:choice:yes'}
    ],consequence:{id:'p153Consequent',summary:'A durable test consequence'}
  });
  module.triggers.push({id:'p153-child',event:'P153_CHILD',effects:[
    {type:'set-flag',flag:'p153Child',condition:'payload:phase:child'},
    {type:'set-flag',flag:'p153LeakedParent',condition:'payload:route:sector:7'}
  ]});
  assert.ok(validateAdventureSemantics(module).triggers>=2);
  return module;
}
const commands=[{type:'event',eventType:'P153_SIGNAL',payload:{route:'sector:7',choice:'yes'}}];
for(const {id} of publishedAdventureCatalog()) {
  test(`P153 ${id}: trigger payload drives conditional effects, objectives, clocks and consequences atomically`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module);
    const result=runtime.commitAction(`p153:${id}:signal`,commands);
    assert.equal(result.applied,true);
    const state=runtime.getState();
    assert.equal(state.flags.p153Qualified,true);
    assert.equal(state.flags.p153Child,true);
    assert.equal(state.flags.p153LeakedParent,undefined,'child must not inherit parent payload');
    assert.equal(state.counters.p153Counter,3);
    assert.equal(state.clocks.p153Clock.value,1);
    assert.equal(state.objectives['adventure-finale'].status,'COMPLETED');
    assert.equal(state.consequences.filter(x=>x.id==='p153Consequent').length,1);
    assert.equal(validateAdventureCheckpoint(module,state),true);
    const snapshot=runtime.serialize();
    for(let reopen=0;reopen<3;reopen++){
      const restored=AdventureRuntime.restore(module,snapshot);
      assert.equal(restored.commitAction(`p153:${id}:signal`,commands).applied,false);
      assert.deepEqual(restored.getState(),state);
      assert.equal(validateAdventureCheckpoint(module,restored.getState()),true);
    }
  });
  test(`P153 ${id}: unmatched and direct effects cannot reuse stale event context`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module);
    runtime.dispatchEvent('P153_SIGNAL',{route:'sector:7',choice:'no'},'p153',{idempotencyKey:`p153:${id}:no`});
    let state=runtime.getState();
    assert.equal(state.flags.p153Qualified,true);
    assert.equal(state.counters.p153Counter,undefined);
    assert.equal(state.clocks.p153Clock.value,0);
    assert.equal(state.objectives['adventure-finale'].status,'ACTIVE');
    runtime.applyEffects([{type:'set-flag',flag:'p153Stale',condition:'payload:route:sector:7'}]);
    assert.equal(runtime.getState().flags.p153Stale,undefined);
    runtime.dispatchEvent('P153_SIGNAL',{route:'sector:8',choice:'yes'},'p153');
    state=runtime.getState();
    assert.equal(state.counters.p153Counter,undefined);
    assert.equal(state.triggersFired['p153-parent'],1);
    assert.equal(validateAdventureCheckpoint(module,state),true);
  });
  test(`P153 ${id}: malformed event and failed transaction roll back all ledgers`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module),before=runtime.getState();
    assert.throws(()=>runtime.commitAction(`p153:${id}:broken`,[
      ...commands,{type:'advance-clock',clockId:'p153Clock',delta:Infinity}
    ]),/não persistível|inválid|fora dos limites/);
    assert.deepEqual(runtime.getState(),before);
    assert.throws(()=>runtime.applyEffects([{type:'set-flag',flag:'x'}],[]),/Contexto de evento UAE inválido/);
    assert.deepEqual(runtime.getState(),before);
    assert.equal(runtime.commitAction(`p153:${id}:broken`,commands).applied,true);
    assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
  });
  test(`P153 ${id}: child events cannot counterfeit parent event context`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module);
    runtime.dispatchEvent('P153_CHILD',{phase:'other',route:'sector:8'},'p153');
    assert.equal(runtime.getState().flags.p153Child,undefined);
    assert.equal(runtime.getState().flags.p153LeakedParent,undefined);
    runtime.dispatchEvent('P153_CHILD',{phase:'child'},'p153');
    assert.equal(runtime.getState().flags.p153Child,true);
    assert.equal(runtime.getState().flags.p153LeakedParent,undefined);
    assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
  });
}
for(const {id} of publishedAdventureCatalog()) {
  test(`P153 ${id}: fresh checkpoint rejects unjournaled completed objective`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module),tampered=runtime.getState();
    tampered.objectives['adventure-finale'].status='COMPLETED';
    tampered.objectives['adventure-finale'].progress=1;
    tampered.completedObjectives.push('adventure-finale');
    assert.throws(()=>AdventureRuntime.restore(module,tampered),/sem evento/);
  });
  test(`P153 ${id}: fresh checkpoint rejects unjournaled consequence resolution`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module);
    runtime.recordConsequence({id:'p153Guarded'});
    const tampered=runtime.getState();
    tampered.consequences[0].resolved=true;
    tampered.consequences[0].resolvedAt=new Date().toISOString();
    assert.throws(()=>AdventureRuntime.restore(module,tampered),/sem evento/);
  });
  test(`P153 ${id}: audited consequence resolution and failed objectives survive reopens`,()=>{
    const module=fixture(id),runtime=new AdventureRuntime(module);
    runtime.transaction(tx=>{
      tx.recordConsequence('p153Guarded');
      tx.resolveConsequence('p153Guarded');
      tx.failObjective('adventure-finale');
    });
    const state=runtime.getState();
    assert.equal(state.consequences[0].resolved,true);
    assert.equal(state.objectives['adventure-finale'].status,'FAILED');
    assert.equal(validateAdventureCheckpoint(module,state),true);
    assert.deepEqual(AdventureRuntime.restore(module,runtime.serialize()).getState(),state);
  });
}
for(const {id} of publishedAdventureCatalog()) {
  test(`P153 ${id}: authored consequences with reserved persistence fields fail preflight`,()=>{
    const module=fixture(id);
    const authored=module.triggers.find(x=>x.id==='p153-parent');
    for(const bad of [{id:'bad',resolved:true},{id:'bad',recordedAt:'2026-01-01'},
      {id:'__proto__'},{id:'bad',summary:{nested:'not allowed'}}]){
      authored.consequence=bad;
      assert.throws(()=>validateAdventureSemantics(module),/semantic/i);
    }
  });
  test(`P153 ${id}: valid authored consequence passes semantic preflight and survives JSON`,()=>{
    const module=fixture(id);
    const authored=module.triggers.find(x=>x.id==='p153-parent');
    authored.consequence={id:'p153Consequent',title:'Consequence',severity:2};
    assert.ok(validateAdventureSemantics(module).triggers>=2);
    const runtime=new AdventureRuntime(module);
    runtime.dispatchEvent('P153_SIGNAL',{route:'sector:7',choice:'yes'},'p153');
    const state=AdventureRuntime.restore(module,runtime.serialize()).getState();
    assert.equal(state.consequences[0].severity,2);
    assert.equal(validateAdventureCheckpoint(module,state),true);
  });
}
