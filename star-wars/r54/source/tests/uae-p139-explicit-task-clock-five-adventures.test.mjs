import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { recordResolvedTaskTime } from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';

const copy=structuredClone;
class Repo { constructor(){this.state=createCampaign('P139')}load(){return copy(this.state)}save(s){this.state=copy(s)} }
function character(){return createAorCoreCharacter({name:'Explorador P139',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});}
function game(id=null){const repo=new Repo(),g=new GameController(repo);g.setCharacter(character());if(id)g.startPublishedAdventureSession(id);return {g,repo};}
function roll(g,minutes=60){return g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,taskDurationMinutes:minutes,rng:()=>0.4});}

for(const {id} of publishedAdventureCatalog()){
 test(`P139 ${id}: tempo explícito + journal UAE idempotente após 3 reaberturas`,()=>{
  const {g,repo}=game(id);const before=copy(g.state.world);const check=roll(g,60);
  assert.equal(g.state.world.clock.elapsedMinutes,undefined,'rolar não avança relógio');
  assert.equal(g.state.world.clock.tick,before.clock.tick,'rolar não avança tick');
  const eventCount=g.state.world.publishedAdventureRuntime.canonicalEvents.length;
  let reopened=new GameController(repo);
  assert.equal(reopened.state.lastCheck.taskCheckId,check.taskCheckId,'tarefa pendente sobrevive reabertura');
  const applied=reopened.commitLastTaskTime({checkId:check.taskCheckId});
  assert.equal(applied.applied,true);assert.equal(applied.elapsedMinutes,60);
  assert.equal(reopened.state.world.clock.tick,before.clock.tick);
  assert.equal(reopened.state.world.publishedAdventureRuntime.canonicalEvents.length,eventCount+1);
  assert.equal(reopened.state.world.publishedAdventureRuntime.canonicalEvents.at(-1).type,'TASK_TIME_COMMITTED');
  assert.equal(reopened.state.world.publishedAdventureRuntime.canonicalEvents.at(-1).idempotencyKey,check.taskCheckId);
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  assert.equal(validatePublishedAdventureRuntimeState(module,reopened.state.world.publishedAdventureRuntime,reopened.state.world.publishedAdventure),true);
  for(let i=0;i<3;i++){
   const serialized=JSON.stringify(repo.state),journalCount=reopened.state.world.publishedAdventureRuntime.canonicalEvents.length;
   reopened=new GameController(repo);
   assert.equal(reopened.commitLastTaskTime({checkId:check.taskCheckId}).applied,false);
   assert.equal(JSON.stringify(repo.state),serialized,'retry não altera save');
   assert.equal(reopened.state.world.clock.elapsedMinutes,60);
   assert.equal(reopened.state.world.publishedAdventureRuntime.canonicalEvents.length,journalCount);
  }
 });
 test(`P139 ${id}: 2 tarefas sequenciais acumulam minutos sem duplicar eventos`,()=>{
  const {g,repo}=game(id);const first=roll(g,20);
  g.commitLastTaskTime({checkId:first.taskCheckId});
  const second=roll(g,45);
  assert.notEqual(first.taskCheckId,second.taskCheckId);
  g.commitLastTaskTime({checkId:second.taskCheckId});
  assert.equal(g.state.world.clock.elapsedMinutes,65);
  assert.equal(Object.keys(g.state.world.taskTimeApplications).length,2);
  assert.equal(g.state.world.publishedAdventureRuntime.canonicalEvents.filter(e=>e.type==='TASK_TIME_COMMITTED').length,2);
  assert.equal(new GameController(repo).state.world.clock.elapsedMinutes,65);
 });
 test(`P139 ${id}: id de teste antigo não pode debitar tempo ainda não confirmado`,()=>{
  const {g}=game(id);const first=roll(g,40),before=copy(g.state.world);
  roll(g,30);
  assert.throws(()=>g.commitLastTaskTime({checkId:first.taskCheckId}),/último teste/);
  assert.deepEqual(g.state.world,before);
 });
}

test('P139: testes sem duração não recebem ID de tarefa nem podem avançar minutos',()=>{
 const {g}=game();const check=g.resolveSkillCheck({skillKey:'computadoras',rng:()=>0.4});
 assert.equal(check.taskCheckId,undefined);
 assert.throws(()=>g.commitLastTaskTime({checkId:'task_nonexistent'}),/último teste/);
 assert.equal(g.state.world.clock.elapsedMinutes,undefined);
});

test('P139: journal de tempo exige IDs válidos e nunca converte minutos em ticks',()=>{
 const world=createWorld();world.clock.tick=7;
 const record={checkId:'task_abc123',skillKey:'computadoras',minutes:12.5,baseMinutes:25};
 const first=recordResolvedTaskTime(world,record);
 assert.equal(first.applied,true);assert.equal(first.elapsedMinutes,12.5);assert.equal(first.world.clock.tick,7);
 assert.equal(world.clock.elapsedMinutes,undefined,'função pura não altera argumento');
 assert.equal(recordResolvedTaskTime(first.world,record).applied,false);
 assert.throws(()=>recordResolvedTaskTime(first.world,{...record,minutes:13}),/divergentes/);
 assert.equal(first.world.clock.elapsedMinutes,12.5);
});

test('P139: validação rejeita duração, ledger e relógio inválidos sem mutação',()=>{
 const world=createWorld(),record={checkId:'task_abc123',skillKey:'computadoras',minutes:30,baseMinutes:60};
 for(const bad of [NaN,Infinity,-1,1e12,'30',true,{},null]){
  assert.throws(()=>recordResolvedTaskTime(world,{...record,minutes:bad}),/Duração/);
  assert.deepEqual(world,createWorld());
 }
 for(const id of ['', '__proto__','constructor','task_','task_á','other_id']){
  assert.throws(()=>recordResolvedTaskTime(world,{...record,checkId:id}),/Identificador/);
 }
 const invalid=copy(world);invalid.clock.elapsedMinutes=-10;
 assert.throws(()=>recordResolvedTaskTime(invalid,record),/Relógio/);
 const invalidLedger=copy(world);invalidLedger.taskTimeApplications=[];
 assert.throws(()=>recordResolvedTaskTime(invalidLedger,record),/Registro/);
});

test('P139: mundo e UAE permanecem íntegros se checkpoint de aventura rejeitar o débito',()=>{
 const {g,repo}=game('under-black-sun');const check=roll(g,25);
 const invalid=copy(g.state.world);invalid.publishedAdventureRuntime.eventSequence=-1;
 g.state.world=invalid;repo.state.world=copy(invalid);
 const before=copy(g.state);
 assert.throws(()=>g.commitLastTaskTime({checkId:check.taskCheckId}),/Sequência|Checkpoint|inválid/i);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);
});

test('P139: gatilhos UAE recebem TASK_TIME_COMMITTED e persistem sem reaplicar',()=>{
 const adapter=getPublishedAdventureAdapter('under-black-sun');const module=publishedAdventureRuntimeModule(adapter.meta);
 module.triggers.push({id:'task-time-trigger',event:'TASK_TIME_COMMITTED',maxFires:2,effects:[{type:'adjust-counter',counter:'minutesTracked',delta:1}]});
 let r=new AdventureRuntime(module);
 const opts={idempotencyKey:'task_abc123'};
 assert.deepEqual(r.dispatchEvent('TASK_TIME_COMMITTED',{checkId:'task_abc123',elapsedMinutes:15},'world-time',opts),['task-time-trigger']);
 assert.equal(r.state.counters.minutesTracked,1);
 r=AdventureRuntime.restore(module,r.serialize());
 assert.deepEqual(r.dispatchEvent('TASK_TIME_COMMITTED',{elapsedMinutes:15,checkId:'task_abc123'},'world-time',opts),[]);
 assert.equal(r.state.counters.minutesTracked,1);
 assert.throws(()=>r.dispatchEvent('TASK_TIME_COMMITTED',{elapsedMinutes:30,checkId:'task_abc123'},'world-time',opts),/reutilizada/);
});

test('P139: UI expõe confirmação explícita e recupera tarefa pendente após reinício',()=>{
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.match(ui,/data-p139-apply-time/);
 assert.match(ui,/call\('commitLastTaskTime',\{checkId\}\)/);
 assert.match(ui,/st\.lastCheck/);
 assert.match(ui,/taskTimeApplications/);
 assert.match(ui,/Nenhum turno avançado/);
});

test('P139: ponte Android expõe confirmação com resposta estruturada e rejeita replay divergente',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const {g,repo}=game('beyond-the-rim');
 const check=roll(g,32);
 const bridge=new BraseiroUiBridge({repository:repo});
 const first=bridge.commitLastTaskTime({checkId:check.taskCheckId});
 assert.equal(first.ok,true);assert.equal(first.data.applied,true);assert.equal(first.data.elapsedMinutes,32);
 const retry=bridge.commitLastTaskTime({checkId:check.taskCheckId});
 assert.equal(retry.ok,true);assert.equal(retry.data.applied,false);
 const invalid=bridge.commitLastTaskTime({checkId:'task_forged'});
 assert.equal(invalid.ok,false);assert.equal(invalid.error,'TASK_TIME_COMMIT_FAILED');
 assert.equal(bridge.readState().data.world.clock.elapsedMinutes,32);
});
