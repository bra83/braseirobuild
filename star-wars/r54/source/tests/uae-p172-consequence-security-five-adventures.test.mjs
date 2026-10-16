/** P172 — integrated source-authenticated consequence resolution and fail-closed predicates. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

function fixture(id) {
 const m=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 m.initialObjectives['p172-intel']={title:'Coletar inteligência',type:'SECONDARY',target:1};
 m.initialObjectives['p172-exfil']={title:'Retirada',type:'PRIMARY',target:1,requires:['p172-intel']};
 m.initialClocks['p172-alert']={value:0,max:2,status:'ACTIVE'};
 m.triggers.push({id:'p172-authored-string',event:'P172_INTEL',maxFires:1,
  effects:[{type:'complete-objective',objectiveId:'p172-intel'},
   {type:'advance-clock',clockId:'p172-alert',delta:1}],
  consequence:'p172-source-string'});
 m.triggers.push({id:'p172-authored-object',event:'P172_EXFIL',maxFires:1,
  condition:'objective:p172-intel:COMPLETED',
  consequence:{id:'p172-source-object',resolveWhen:'flag:p172-clear',
   onResolveEffects:[{type:'complete-objective',objectiveId:'p172-exfil'},
    {type:'advance-clock',clockId:'p172-alert',delta:1},
    {type:'set-flag',flag:'p172-finished',value:true}]}});
 validateAdventureSemantics(m);
 return m;
}
for(const {id} of publishedAdventureCatalog()) {
 test(`P172 ${id}: authored string consequence rejects injected rewards without mutation`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  r.commitAction(`${id}:p172:intel`,[{type:'event',eventType:'P172_INTEL'}]);
  const before=r.getState();
  for(const payload of [
   {'p172-free-credits':999999},
   [{type:'complete-objective',objectiveId:'p172-exfil'}],
   [{type:'set-flag',flag:'p172-free-credits',value:true}]
  ]) {
   assert.throws(()=>r.resolveConsequence('p172-source-string',payload),/não aceita efeitos/);
   assert.deepEqual(r.getState(),before);
  }
  assert.equal(r.getState().objectives['p172-exfil'].status,'ACTIVE');
  assert.equal(r.getState().flags['p172-free-credits'],undefined);
 });
 test(`P172 ${id}: trigger -> string/object consequences -> prerequisites -> clocks -> three restores`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  assert.equal(r.commitAction(`${id}:p172:intel`,[{type:'event',eventType:'P172_INTEL'}]).applied,true);
  assert.equal(r.commitAction(`${id}:p172:exfil`,[{type:'event',eventType:'P172_EXFIL'}]).applied,true);
  assert.throws(()=>r.resolveConsequence('p172-source-object'),/Condição de resolução/);
  assert.equal(r.commitAction(`${id}:p172:clear`,[{type:'effects',effects:{'p172-clear':true}}]).applied,true);
  assert.equal(r.commitAction(`${id}:p172:resolve-string`,[{type:'resolve-consequence',consequenceId:'p172-source-string'}]).applied,true);
  assert.equal(r.commitAction(`${id}:p172:resolve-object`,[{type:'resolve-consequence',consequenceId:'p172-source-object'}]).applied,true);
  const stable=r.getState();
  assert.equal(stable.objectives['p172-intel'].status,'COMPLETED');
  assert.equal(stable.objectives['p172-exfil'].status,'COMPLETED');
  assert.equal(stable.clocks['p172-alert'].status,'COMPLETED');
  assert.equal(stable.flags['p172-finished'],true);
  assert.equal(validateAdventureCheckpoint(m,stable),true);
  let json=r.serialize();
  for(let n=0;n<3;n++){
   const opened=AdventureRuntime.restore(m,json);
   assert.deepEqual(opened.getState(),stable);
   assert.equal(opened.commitAction(`${id}:p172:resolve-object`,[{type:'resolve-consequence',consequenceId:'p172-source-object'}]).applied,false);
   json=opened.serialize();
  }
 });
 test(`P172 ${id}: rollback if a resolution action injects a script after legitimate events`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  r.commitAction(`${id}:p172:intel`,[{type:'event',eventType:'P172_INTEL'}]);
  const before=r.getState();
  assert.throws(()=>r.commitAction(`${id}:p172:attack`,[
   {type:'event',eventType:'P172_EXFIL'},
   {type:'effects',effects:{'p172-clear':true}},
   {type:'resolve-consequence',consequenceId:'p172-source-string'},
   {type:'complete-objective',objectiveId:'p172-nonexistent'}
  ]),/desconhecido/);
  assert.deepEqual(r.getState(),before);
  assert.equal(r.commitAction(`${id}:p172:attack`,[
   {type:'event',eventType:'P172_EXFIL'},
   {type:'effects',effects:{'p172-clear':true}},
   {type:'resolve-consequence',consequenceId:'p172-source-string'}
  ]).applied,true);
  assert.equal(validateAdventureCheckpoint(m,r.getState()),true);
 });
 test(`P172 ${id}: invalid compound conditions, unknown operators and nonexistent scenes fail closed`,()=>{
  const r=new AdventureRuntime(fixture(id));
  for(const predicate of ['all:', 'any:', 'all:flag:p172-ready|',
   'any:|flag:p172-ready', [], ['flag:p172-ready',''],
   'unvisited:__not_an_authored_scene__', 'clock:p172-alert:!=:0',
   'counter:p172-missing:??:0'])
   assert.equal(r.isConditionMet(predicate),false,`predicate ${JSON.stringify(predicate)}`);
  assert.equal(r.isConditionMet('clock:p172-alert:=:0'),true);
 });
 test(`P172 ${id}: source preflight blocks forged consequence metadata and reserved event emission`,()=>{
  const m=fixture(id);
  for(const field of ['authored','triggerId','causeEventId','resolved','recordedAt','resolvedAt']) {
   const forged=structuredClone(m);
   forged.triggers.push({id:`p172-forged-${field}`,event:'P172_FORGED',
    consequence:{id:`p172-consequence-${field}`,[field]:'forged'}});
   assert.throws(()=>validateAdventureSemantics(forged),/campo interno/);
  }
  const forged=structuredClone(m);
  forged.triggers.push({id:'p172-forged-event',event:'P172_FORGED',
   effects:[{type:'emit-event',eventType:'ADVENTURE_ACTION_COMMITTED'}]});
  assert.throws(()=>validateAdventureSemantics(forged),/evento reservado/);
 });
}
