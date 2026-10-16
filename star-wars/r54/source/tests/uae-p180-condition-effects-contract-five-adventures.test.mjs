/** P180: same authored condition/effect contract in preflight and runtime.
 * Five published adventures are exercised with durable receipts and 3 restores.
 * No invented official encounters or rules are introduced. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
function fixture(id){
 const m=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 m.initialObjectives['p180-verify']={title:'Verificar contrato de gatilhos',type:'SECONDARY',target:1};
 m.initialClocks['p180-clock']={max:3,value:0,status:'ACTIVE'};
 m.triggers.push({id:'p180-authorized-trigger',event:'P180_AUTHORIZED',maxFires:1,
  condition:'payload:authorized:true',
  effects:[{type:'set-flag',flag:'p180-authorized',value:true},
   {type:'advance-clock',clockId:'p180-clock',delta:1},
   {type:'complete-objective',objectiveId:'p180-verify'}],
  consequence:{id:'p180-authored-consequence',resolveWhen:'flag:p180-authorized',
   onResolveEffects:[{type:'set-flag',flag:'p180-reward',value:true}]}});
 assert.ok(validateAdventureSemantics(m).triggers>0);
 return m;
}
for (const {id} of publishedAdventureCatalog()) {
 test(`P180 ${id}: authored preflight bounds mirror runtime grammar`,()=>{
  const m=fixture(id);
  for(const bad of [false, 0, ['flag:x',null], ['flag:x',''],
   Array.from({length:65},()=> 'flag:x'),
   'any:'+Array.from({length:33},()=> 'flag:x').join('|'),
   'all:'+Array.from({length:33},()=> 'flag:x').join('|'),
   'flag:'+ 'x'.repeat(2048),
   Array.from({length:18},(_,i)=>i).reduce((s)=>[s], 'flag:x')]) {
   const corrupt=copy(m); corrupt.nodes[corrupt.startNodeId].choices ??=[];
   corrupt.nodes[corrupt.startNodeId].choices.push({id:'p180-bad',label:'bad',to:corrupt.startNodeId,condition:bad});
   assert.throws(()=>validateAdventureSemantics(corrupt),/semantic/i,JSON.stringify(bad).slice(0,70));
  }
 });
 test(`P180 ${id}: source preflight rejects reserved events in BOTH effect syntaxes`,()=>{
  const m=fixture(id);
  for(const effects of [
   [{type:'emit-event',eventType:'OBJECTIVE_COMPLETED',payload:{objectiveId:'p180-verify'}}],
   {'emit-event':{type:'PUBLISHED_COMMAND_COMMITTED',source:'controller'}},
   {'emit-event':{type:'P180_ALLOWED',source:'engine'}},
   [{type:'emit-event',eventType:'P180_ALLOWED',source:'controller'}],
   [{type:false,flag:'p180-free',value:true}],
   [{type:'add-clue',clueId:'p180-clue',title:{forged:true}}],
   [{type:'reveal-secret',secretId:'p180-secret',description:{forged:true}}],
   Array.from({length:129},()=>({type:'set-flag',flag:'p180-x',value:true}))
  ]) {
   const corrupt=copy(m); corrupt.triggers.at(-1).effects=effects;
   assert.throws(()=>validateAdventureSemantics(corrupt),/semantic/i);
  }
 });
 test(`P180 ${id}: malformed runtime effect guard never grants reward`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  const before=r.getState();
  r.applyEffects([{type:'set-flag',flag:'p180-bypass',value:true,condition:false}]);
  assert.equal(r.getState().flags['p180-bypass'],undefined);
  assert.throws(()=>r.applyEffects([{type:'set-flag',flag:'p180-first',value:true},null]),/Efeito UAE inválido/);
  assert.deepEqual(r.getState(),before,'invalid batch rolls back the first effect and journal');
  assert.throws(()=>r.applyEffects([{type:'set-flag',flag:'p180-first',value:true},{type:0,flag:'p180-invalid'}]),/Tipo de efeito UAE inválido/);
  assert.deepEqual(r.getState(),before);
  assert.throws(()=>r.applyEffects(false),/Efeitos UAE inválidos/);
  assert.deepEqual(r.getState(),before);
 });
 test(`P180 ${id}: trigger -> clock -> objective -> consequence -> 3 durable reopens`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m);
  const first=r.commitAction(`${id}:p180:authorized`,[{type:'event',eventType:'P180_AUTHORIZED',payload:{authorized:true}}]);
  assert.equal(first.applied,true);
  assert.equal(r.getState().flags['p180-authorized'],true);
  assert.equal(r.getState().clocks['p180-clock'].value,1);
  assert.equal(r.getState().objectives['p180-verify'].status,'COMPLETED');
  assert.equal(r.getState().consequences.some(c=>c.id==='p180-authored-consequence'),true);
  assert.equal(r.commitAction(`${id}:p180:resolve`,[{type:'resolve-consequence',consequenceId:'p180-authored-consequence'}]).applied,true);
  const stable=r.getState();
  assert.equal(stable.flags['p180-reward'],true);
  assert.equal(validateAdventureCheckpoint(m,stable),true);
  let serialized=r.serialize();
  for(let i=0;i<3;i++){
   const opened=AdventureRuntime.restore(m,serialized);
   assert.deepEqual(opened.getState(),stable);
   assert.equal(opened.commitAction(`${id}:p180:authorized`,[{type:'event',eventType:'P180_AUTHORIZED',payload:{authorized:true}}]).applied,false);
   assert.equal(opened.commitAction(`${id}:p180:resolve`,[{type:'resolve-consequence',consequenceId:'p180-authored-consequence'}]).applied,false);
   serialized=opened.serialize();
  }
 });
 test(`P180 ${id}: invalid mid-transaction command rolls back triggers and consequence`,()=>{
  const m=fixture(id),r=new AdventureRuntime(m),before=r.getState();
  assert.throws(()=>r.commitAction(`${id}:p180:rollback`,[
   {type:'event',eventType:'P180_AUTHORIZED',payload:{authorized:true}},
   {type:'effects',effects:[{type:'set-flag',flag:'p180-forged',value:true},false]}
  ]),/Efeito UAE inválido/);
  assert.deepEqual(r.getState(),before);
  assert.equal(r.commitAction(`${id}:p180:rollback`,[
   {type:'event',eventType:'P180_AUTHORIZED',payload:{authorized:true}}
  ]).applied,true,'failed receipt key can be safely retried');
  assert.equal(validateAdventureCheckpoint(m,r.getState()),true);
 });
 test(`P180 ${id}: invalid condition does not fire trigger or corrupt saved state`,()=>{
  const m=fixture(id);m.triggers.at(-1).condition='payload:authorized:true';
  const r=new AdventureRuntime(m),before=r.getState();
  assert.equal(r.commitAction(`${id}:p180:denied`,[{type:'event',eventType:'P180_AUTHORIZED',payload:{authorized:false}}]).applied,true);
  assert.equal(r.getState().triggersFired['p180-authorized-trigger'],undefined);
  assert.equal(r.getState().objectives['p180-verify'].status,'ACTIVE');
  assert.equal(r.getState().consequences.length,0);
  assert.equal(r.getState().clocks['p180-clock'].value,0);
  assert.equal(validateAdventureCheckpoint(m,AdventureRuntime.restore(m,r.serialize()).getState()),true);
  assert.notDeepEqual(r.getState(),before,'external event still durably journaled');
 });
}
