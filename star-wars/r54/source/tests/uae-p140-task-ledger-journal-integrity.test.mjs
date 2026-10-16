import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { recordResolvedTaskTime, validateTaskTimeLedger } from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import { recordPublishedAdventureTaskTime } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
const clone = structuredClone;
class Repo { constructor() { this.state=createCampaign('P140'); } load() { return clone(this.state); } save(s) { this.state=clone(s); } }
function makeGame(id) {
 const repo=new Repo(), g=new GameController(repo);
 g.setCharacter(createAorCoreCharacter({name:'Auditor P140',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'}));
 if(id)g.startPublishedAdventureSession(id);
 return {g,repo};
}
function check(g, minutes=20) { return g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,taskDurationMinutes:minutes,rng:()=>0.4}); }
function committed(id) { const {g,repo}=makeGame(id); const a=check(g,20);g.commitLastTaskTime({checkId:a.taskCheckId}); const b=check(g,30);g.commitLastTaskTime({checkId:b.taskCheckId});return {g,repo,keys:[a.taskCheckId,b.taskCheckId]}; }

for(const {id} of publishedAdventureCatalog()) {
 test(`P140 ${id}: recibos, journal, soma e três restaurações coerentes`,()=>{
  const {g,repo,keys}=committed(id);let current=g;
  for(let n=0;n<3;n++){
   const audit=validateTaskTimeLedger(current.state.world);
   assert.deepEqual(audit,{elapsedMinutes:50,confirmedTasks:2});
   const events=current.state.world.publishedAdventureRuntime.canonicalEvents.filter(e=>e.type==='TASK_TIME_COMMITTED');
   assert.deepEqual(events.map(e=>e.idempotencyKey),keys);
   const snapshot=JSON.stringify(repo.state);
   for(const key of keys)assert.equal(current.commitLastTaskTime({checkId:key}).applied,false);
   assert.equal(JSON.stringify(repo.state),snapshot,'retry must not persist');
   current=new GameController(repo);
  }
 });
 test(`P140 ${id}: adulteração do relógio bloqueia débito e replay sem mutação`,()=>{
  const {g,repo,keys}=committed(id);
  g.state.world.clock.elapsedMinutes+=7;repo.state=clone(g.state);
  const before=clone(g.state);
  assert.throws(()=>g.commitLastTaskTime({checkId:keys[0]}),/Relógio|histórico/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);
 });
 test(`P140 ${id}: adulteração de recibo bloqueia replay e novos commits`,()=>{
  const {g,repo,keys}=committed(id);
  g.state.world.taskTimeApplications[keys[0]].minutes=15;repo.state=clone(g.state);
  const before=clone(g.state);
  assert.throws(()=>g.commitLastTaskTime({checkId:keys[0]}),/Relógio|histórico|Registro/);
  const third=check(g,15);const pending=clone(g.state);
  assert.throws(()=>g.commitLastTaskTime({checkId:third.taskCheckId}),/Relógio|histórico|Registro/);
  assert.deepEqual(g.state,pending);assert.deepEqual(repo.state,pending);
  assert.notDeepEqual(before,pending);
 });
 test(`P140 ${id}: adulteração de journal UAE rejeitada sem reparo silencioso`,()=>{
  const {g,repo,keys}=committed(id);
  const e=g.state.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.idempotencyKey===keys[0]);
  e.payload.elapsedMinutes=21;repo.state=clone(g.state);const before=clone(g.state);
  assert.throws(()=>g.commitLastTaskTime({checkId:keys[0]}),/Journal UAE de tarefas/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);
 });
 test(`P140 ${id}: evento sem recibo é rejeitado na restauração auditada`,()=>{
  const {g,keys}=committed(id);
  delete g.state.world.taskTimeApplications[keys[0]];
  g.state.world.clock.elapsedMinutes=30;
  assert.throws(()=>validateTaskTimeLedger(g.state.world),/Journal UAE de tarefas/);
 });
 test(`P140 ${id}: evento não pode ser inserido sem recibo confirmado`,()=>{
  const {g}=makeGame(id);const before=clone(g.state.world);
  assert.throws(()=>recordPublishedAdventureTaskTime(g.state.world,{checkId:'task_fake',skillKey:'computadoras',minutes:30,baseMinutes:30}),/recibo confirmado/);
  assert.deepEqual(g.state.world,before);
 });
 test(`P140 ${id}: rollback UAE mantém recibos intactos quando evento falha`,()=>{
  const {g,repo}=makeGame(id), a=check(g,45);
  g.state.world.publishedAdventureRuntime.eventSequence=-1;
  repo.state=clone(g.state);const before=clone(g.state);
  assert.throws(()=>g.commitLastTaskTime({checkId:a.taskCheckId}),/Sequência|Checkpoint|inválid/i);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);
 });
}

test('P140: save legado sem ledger continua aceito e primeiro débito é persistido',()=>{
 const world=createWorld();assert.deepEqual(validateTaskTimeLedger(world),{elapsedMinutes:0,confirmedTasks:0});
 const a=recordResolvedTaskTime(world,{checkId:'task_legacy1',skillKey:'computadoras',minutes:3.25,baseMinutes:6.5});
 assert.equal(a.world.clock.elapsedMinutes,3.25);assert.equal(validateTaskTimeLedger(a.world).confirmedTasks,1);
 assert.equal(world.clock.elapsedMinutes,undefined);
});
test('P140: tempo confirmado antes de começar aventura não exige journal retroativo',()=>{
 const {g}=makeGame();const c=check(g,25);g.commitLastTaskTime({checkId:c.taskCheckId});
 g.startPublishedAdventureSession('under-black-sun');
 assert.equal(validateTaskTimeLedger(g.state.world).elapsedMinutes,25);
 const next=check(g,10);g.commitLastTaskTime({checkId:next.taskCheckId});
 assert.equal(validateTaskTimeLedger(g.state.world).confirmedTasks,2);
 assert.equal(g.state.world.publishedAdventureRuntime.canonicalEvents.filter(e=>e.type==='TASK_TIME_COMMITTED').length,1);
});
test('P140: relógio fracionário é auditado sem perder precisão de save JSON',()=>{
 let w=createWorld();for(let i=0;i<15;i++)w=recordResolvedTaskTime(w,{checkId:`task_fraction_${i}`,skillKey:'computadoras',minutes:0.1,baseMinutes:0.2}).world;
 assert.equal(validateTaskTimeLedger(JSON.parse(JSON.stringify(w))).confirmedTasks,15);
});
test('P140: ledger rejeita registros órfãos, IDs perigosos e dados não numéricos',()=>{
 const base=createWorld();
 const changes=[
  w=>{w.taskTimeApplications={task_foo:{checkId:'task_bar',skillKey:'computadoras',minutes:2,baseMinutes:3}};w.clock.elapsedMinutes=2},
  w=>{w.taskTimeApplications={task_foo:{checkId:'task_foo',skillKey:'computadoras',minutes:'2',baseMinutes:3}};w.clock.elapsedMinutes=2},
  w=>{w.taskTimeApplications={task_foo:{checkId:'task_foo',skillKey:'computadoras',minutes:4,baseMinutes:3}};w.clock.elapsedMinutes=4},
  w=>{w.taskTimeApplications={task_foo:{checkId:'task_foo',skillKey:'computadoras',minutes:2,baseMinutes:3,secret:true}};w.clock.elapsedMinutes=2},
  w=>{w.clock.elapsedMinutes=5},
  w=>{w.clock.elapsedMinutes=Infinity},
  w=>{w.taskTimeApplications=[]}
 ];
 for(const mutate of changes){const w=clone(base);mutate(w);assert.throws(()=>validateTaskTimeLedger(w),/Registro|Relógio|histórico/);}
});

test('P140: importação JSON rejeita relógio adulterado antes da migração',async()=>{
 const {importStateJson}=await import('../app/src/main/assets/vtt/js/core/storage.mjs');
 const {g}=committed('under-black-sun');const broken=clone(g.state);
 broken.world.clock.elapsedMinutes+=9;
 assert.throws(()=>importStateJson(JSON.stringify(broken)),/Relógio|histórico/);
 assert.equal(importStateJson(JSON.stringify(g.state)).world.clock.elapsedMinutes,50);
});
test('P140: repositório rejeita gravação e restauração de ledger adulterado',async()=>{
 const {LocalStorageRepository}=await import('../app/src/main/assets/vtt/js/core/storage.mjs');
 const store=new Map(),storage={getItem:k=>store.get(k)??null,setItem:(k,v)=>store.set(k,v),removeItem:k=>store.delete(k)};
 const repo=new LocalStorageRepository(storage,'p140',null);
 const {g}=committed('jewel-of-yavin');repo.save(g.state);
 assert.equal(repo.load().world.clock.elapsedMinutes,50);
 const invalid=clone(g.state);invalid.world.clock.elapsedMinutes=49;
 assert.throws(()=>repo.save(invalid),/Relógio|histórico/);
 assert.equal(repo.load().world.clock.elapsedMinutes,50,'falha não substitui save válido');
 store.set('p140',JSON.stringify(invalid));
 assert.throws(()=>repo.load(),/Relógio|histórico/);
});
test('P140: journal UAE adulterado é rejeitado ao validar estado completo',async()=>{
 const {validateState}=await import('../app/src/main/assets/vtt/js/core/state.mjs');
 const {g,keys}=committed('long-arm-of-the-hutt');const state=clone(g.state);
 assert.equal(validateState(state),true);
 const event=state.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.idempotencyKey===keys[1]);
 event.payload.baseMinutes=999;
 assert.throws(()=>validateState(state),/Journal UAE de tarefas/);
});
