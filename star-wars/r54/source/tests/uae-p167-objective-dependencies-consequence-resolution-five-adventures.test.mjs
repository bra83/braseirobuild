/** P167: source-authored objective dependencies and durable consequence resolution. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const clone=structuredClone;
function fixture(id){
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 module.initialObjectives['p167-evidence']={title:'Confirmar evidência',type:'SECONDARY',target:1};
 module.initialObjectives['p167-extraction']={title:'Concluir extração',type:'SECONDARY',target:2,requires:['p167-evidence']};
 module.initialClocks['p167-exposure']={value:0,max:2,status:'ACTIVE'};
 module.triggers.push({id:'p167-incident',event:'P167_INCIDENT',maxFires:1,
  effects:[{type:'set-flag',flag:'p167-incident-seen',value:true}],
  consequence:{id:'p167-recovery',description:'Resolução com dependências',resolveWhen:'flag:p167-authorized',
   onResolveEffects:[{type:'complete-objective',objectiveId:'p167-evidence'},
    {type:'progress-objective',objectiveId:'p167-extraction',delta:1},
    {type:'advance-clock',clockId:'p167-exposure',delta:1},
    {type:'set-flag',flag:'p167-resolved',value:true}]}});
 assert.equal(validateAdventureSemantics(module).triggers>0,true);
 return module;
}
for(const {id} of publishedAdventureCatalog()){
 test(`P167 ${id}: objective prerequisites fail closed with complete transaction rollback`,()=>{
  const module=fixture(id),runtime=new AdventureRuntime(module),before=runtime.getState();
  assert.throws(()=>runtime.commitAction(`${id}:premature`,[{type:'complete-objective',objectiveId:'p167-extraction'}]),/Pré-requisitos/);
  assert.deepEqual(runtime.getState(),before);
  assert.throws(()=>runtime.commitAction(`${id}:premature`,[{type:'progress-objective',objectiveId:'p167-extraction',delta:2}]),/Pré-requisitos/);
  assert.deepEqual(runtime.getState(),before);
  assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
 });
 test(`P167 ${id}: trigger -> consequence -> resolution -> objective -> clock persists across 3 reopens`,()=>{
  const module=fixture(id),runtime=new AdventureRuntime(module);
  const incident=[{type:'event',eventType:'P167_INCIDENT'}];
  assert.equal(runtime.commitAction(`${id}:incident`,incident).applied,true);
  let state=runtime.getState();
  assert.equal(state.consequences.length,1);
  assert.equal(state.consequences[0].onResolveEffects,undefined,'never persist executable script');
  assert.equal(state.flags['p167-incident-seen'],true);
  assert.throws(()=>runtime.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p167-recovery'}]),/Condição de resolução/);
  assert.deepEqual(runtime.getState(),state);
  assert.equal(runtime.commitAction(`${id}:authorize`,[{type:'effects',effects:[{type:'set-flag',flag:'p167-authorized',value:true}]}]).applied,true);
  assert.equal(runtime.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p167-recovery'}]).applied,true);
  assert.equal(runtime.commitAction(`${id}:finish`,[{type:'progress-objective',objectiveId:'p167-extraction',delta:1},{type:'advance-clock',clockId:'p167-exposure',delta:1}]).applied,true);
  state=runtime.getState();
  assert.equal(state.objectives['p167-evidence'].status,'COMPLETED');
  assert.equal(state.objectives['p167-extraction'].status,'COMPLETED');
  assert.equal(state.clocks['p167-exposure'].status,'COMPLETED');
  assert.equal(state.consequences[0].resolved,true);
  assert.equal(state.flags['p167-resolved'],true);
  assert.equal(state.canonicalEvents.filter(e=>e.type==='CONSEQUENCE_RESOLVED').length,1);
  assert.equal(validateAdventureCheckpoint(module,state),true);
  for(let n=0;n<3;n++){
   const reopened=AdventureRuntime.restore(module,runtime.serialize());
   assert.equal(reopened.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p167-recovery'}]).applied,false);
   assert.equal(reopened.commitAction(`${id}:incident`,incident).applied,false);
   assert.deepEqual(reopened.getState(),state);
  }
 });
 test(`P167 ${id}: authored resolution effect failure reverses consequence and all prior effects`,()=>{
  const module=fixture(id);
  module.triggers.at(-1).consequence.onResolveEffects.push({type:'advance-clock',clockId:'p167-exposure',delta:'NaN'});
  // Runtime rejects the invalid command atomically even if a caller bypasses
  // the source-specific preflight; published modules reject it at load time.
  assert.throws(()=>validateAdventureSemantics(module),/semantic/i);
  const runtime=new AdventureRuntime(module);
  runtime.commitAction(`${id}:incident`,[{type:'event',eventType:'P167_INCIDENT'}]);
  runtime.commitAction(`${id}:authorize`,[{type:'effects',effects:{'p167-authorized':true}}]);
  const before=runtime.getState();
  assert.throws(()=>runtime.commitAction(`${id}:resolve`,[{type:'resolve-consequence',consequenceId:'p167-recovery'}]),/Variação de relógio inválida/);
  assert.deepEqual(runtime.getState(),before);
  assert.equal(runtime.getState().consequences[0].resolved,false);
 });
 test(`P167 ${id}: imported completed objective without prerequisite is rejected`,()=>{
  const module=fixture(id),runtime=new AdventureRuntime(module),tampered=runtime.getState();
  tampered.objectives['p167-extraction'].status='COMPLETED';
  tampered.objectives['p167-extraction'].progress=2;
  tampered.completedObjectives.push('p167-extraction');
  assert.throws(()=>AdventureRuntime.restore(module,tampered),/pré-requisitos|sem evento/);
 });
 test(`P167 ${id}: forged consequence metadata cannot inject resolution effects`,()=>{
  const module=fixture(id),runtime=new AdventureRuntime(module);
  // P169 hardening: a direct call can no longer create a published
  // consequence without its source trigger, even if its script is stripped.
  const before=runtime.getState();
  assert.throws(()=>runtime.recordConsequence({id:'p167-recovery',onResolveEffects:[{type:'set-flag',flag:'p167-forged',value:true}]}),/gatilho de origem/);
  assert.deepEqual(runtime.getState(),before);
  runtime.dispatchEvent('P167_INCIDENT');
  assert.equal(runtime.getState().consequences[0].onResolveEffects,undefined);
  runtime.applyEffects({'p167-authorized':true});
  runtime.resolveConsequence('p167-recovery');
  assert.equal(runtime.getState().flags['p167-forged'],undefined);
  assert.equal(runtime.getState().flags['p167-resolved'],true);
  assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
 });
}
test('P167 preflight rejects missing, duplicate and cyclic objective dependencies',()=>{
 const module=fixture(publishedAdventureCatalog()[0].id);
 const original=clone(module.initialObjectives['p167-extraction']);
 for(const requires of [['missing'],['p167-evidence','p167-evidence'],['p167-extraction']]){
  module.initialObjectives['p167-extraction'].requires=requires;
  assert.throws(()=>validateAdventureSemantics(module),/semantic/i);
 }
 module.initialObjectives['p167-extraction']=original;
 module.initialObjectives['p167-evidence'].requires=['p167-extraction'];
 assert.throws(()=>validateAdventureSemantics(module),/circular/);
});
