/** P168 — five published adventure adapters, real UAE semantic/persistence gates. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
function fixture(id){
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 module.initialObjectives['p168-prep']={title:'Preparação',type:'SECONDARY',target:1};
 module.initialObjectives['p168-rescue']={title:'Resgate',type:'SECONDARY',target:1,requires:['p168-prep']};
 module.initialClocks['p168-alert']={value:0,max:2,status:'ACTIVE'};
 module.triggers.push({id:'p168-alert-trigger',event:'P168_ALARM',maxFires:1,
  effects:[{type:'advance-clock',clockId:'p168-alert',delta:1}],
  consequence:{id:'p168-result',resolveWhen:'flag:p168-authorized',onResolveEffects:[
   {type:'complete-objective',objectiveId:'p168-prep'},
   {type:'complete-objective',objectiveId:'p168-rescue'},
   {type:'advance-clock',clockId:'p168-alert',delta:1},
   {type:'set-flag',flag:'p168-finished',value:true}]}});
 validateAdventureSemantics(module);return module;
}
for(const {id} of publishedAdventureCatalog()){
 test(`P168 ${id}: imported objective completed without prerequisite rejected even when journal predates causal audit`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m),snapshot=r.getState();
  snapshot.objectives['p168-rescue'].status='COMPLETED';snapshot.objectives['p168-rescue'].progress=1;
  snapshot.completedObjectives.push('p168-rescue');
  // Checkpoint gate rejects the impossible graph before journal audits can
  // accept a pre-P141 incomplete history.
  assert.throws(()=>validateAdventureCheckpoint(m,snapshot),/pré-requisitos/);
  assert.throws(()=>AdventureRuntime.restore(m,snapshot),/pré-requisitos/);
 });
 test(`P168 ${id}: published resolution rejects injected effects and rolls back all state`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  r.commitAction(`${id}:alarm`,[{type:'event',eventType:'P168_ALARM'}]);
  r.commitAction(`${id}:authorize`,[{type:'effects',effects:{'p168-authorized':true}}]);
  const before=r.getState();
  assert.throws(()=>r.resolveConsequence('p168-result',[{type:'set-flag',flag:'p168-injected',value:true}]),/não aceita efeitos/);
  assert.deepEqual(r.getState(),before);
  assert.equal(r.getState().flags['p168-injected'],undefined);
 });
 test(`P168 ${id}: full trigger -> consequence -> objectives -> clock -> triple restore and replay`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  const action=[{type:'event',eventType:'P168_ALARM'}];
  assert.equal(r.commitAction(`${id}:alarm`,action).applied,true);
  assert.equal(r.commitAction(`${id}:authorize`,[{type:'effects',effects:{'p168-authorized':true}}]).applied,true);
  assert.equal(r.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p168-result'}]).applied,true);
  const state=r.getState();
  assert.equal(state.flags['p168-finished'],true);
  assert.equal(state.objectives['p168-rescue'].status,'COMPLETED');
  assert.equal(state.clocks['p168-alert'].status,'COMPLETED');
  assert.equal(validateAdventureCheckpoint(m,state),true);
  let serialized=r.serialize();
  for(let i=0;i<3;i++){
   const restored=AdventureRuntime.restore(m,serialized);
   assert.equal(restored.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p168-result'}]).applied,false);
   assert.equal(restored.commitAction(`${id}:alarm`,action).applied,false);
   assert.deepEqual(restored.getState(),state);
   serialized=restored.serialize();
  }
 });
 test(`P168 ${id}: source collision with same consequence ID but different effects rejected at preflight`,()=>{
  const m=fixture(id);
  m.triggers.push({id:'p168-conflict',event:'P168_OTHER',consequence:{id:'p168-result',resolveWhen:'flag:p168-authorized',onResolveEffects:[{type:'set-flag',flag:'p168-forged',value:true}]}});
  assert.throws(()=>validateAdventureSemantics(m),/scripts conflitantes/);
 });
}
