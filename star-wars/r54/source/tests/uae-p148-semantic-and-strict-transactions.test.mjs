/** P148 — semantic preflight + all-or-nothing UAE cascades across five published adventures. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
const copy = structuredClone;
class Repo {
  constructor(id) { this.saved=createCampaign(id); this.fail=false; this.writes=0; }
  load() { return copy(this.saved); }
  save(value) { if(this.fail) throw Error('P148 disk failure'); this.saved=copy(value); this.writes++; }
}
const catalog=publishedAdventureCatalog();
for (const {id} of catalog) {
  const source=getPublishedAdventureAdapter(id).meta;
  const module=publishedAdventureRuntimeModule(source);
  test(`P148 ${id}: preflight semântico de todas as cenas, condições, objetivos e gatilhos`, () => {
    const result=validateAdventureSemantics(module);
    assert.equal(result.adventureId,id);
    assert.equal(result.nodes,Object.keys(source.nodes).length);
    assert.ok(result.choices>=4 && result.conditions>0 && result.triggers>=5);
    assert.equal(validateAdventureCheckpoint(module,new AdventureRuntime(module).getState()),true);
  });
  test(`P148 ${id}: condição desconhecida, destino inválido e objetivo órfão rejeitados antes da execução`, () => {
    const invalidCondition=copy(source);
    invalidCondition.nodes[invalidCondition.startNodeId].choices[0].condition='unknown:p148';
    assert.throws(()=>publishedAdventureRuntimeModule(invalidCondition),/semantic|não suportado/i);
    const invalidNode=copy(source);
    invalidNode.nodes[invalidNode.startNodeId].choices[0].condition='visited:ghost-scene';
    assert.throws(()=>publishedAdventureRuntimeModule(invalidNode),/inexistente/i);
    const invalidEffect=copy(source);
    invalidEffect.nodes[invalidEffect.startNodeId].onEnter=[{type:'complete-objective',objectiveId:'ghost-objective'}];
    assert.throws(()=>publishedAdventureRuntimeModule(invalidEffect),/objetivo inexistente/i);
  });
  test(`P148 ${id}: cascata cíclica não confirma meia ação, nem recibo ou recompensa`, () => {
    const looping=copy(module);
    looping.triggers.push({id:'p148-loop',event:'P148_PING',effects:[{type:'adjust-counter',counter:'p148-reward',delta:1},{type:'emit-event',eventType:'P148_PING'}]});
    const runtime=new AdventureRuntime(looping),before=runtime.getState();
    assert.throws(()=>runtime.commitAction(`p148:${id}:loop`,[
      {type:'effects',effects:[{type:'set-flag',flag:'p148Reward',value:true}]},
      {type:'event',eventType:'P148_PING',payload:{episode:1}}
    ]),/Limite de cascata UAE/);
    assert.deepEqual(runtime.getState(),before);
    assert.equal(runtime.getState().canonicalEvents.some(e=>e.type==='ADVENTURE_ACTION_COMMITTED'),false);
    assert.equal(runtime.getState().counters['p148-reward'],undefined);
    assert.equal(runtime.getState().flags.p148Reward,undefined);
    assert.equal(validateAdventureCheckpoint(looping,runtime.getState()),true);
    // Correcting the bad authored trigger permits the same action ID to commit.
    const repaired=new AdventureRuntime(module,before);
    const result=repaired.commitAction(`p148:${id}:loop`,[
      {type:'effects',effects:[{type:'set-flag',flag:'p148Reward',value:true}]},
      {type:'event',eventType:'P148_PING',payload:{episode:1}}
    ]);
    assert.equal(result.applied,true);
    assert.equal(repaired.commitAction(`p148:${id}:loop`,[
      {type:'effects',effects:[{type:'set-flag',flag:'p148Reward',value:true}]},
      {type:'event',eventType:'P148_PING',payload:{episode:1}}
    ]).applied,false);
    assert.equal(validateAdventureCheckpoint(module,AdventureRuntime.restore(module,repaired.serialize()).getState()),true);
  });
  test(`P148 ${id}: transação manual com cascata cíclica também reverte`, () => {
    const looping=copy(module);
    looping.triggers.push({id:'p148-loop',event:'P148_PING',effects:[{type:'emit-event',eventType:'P148_PING'}]});
    const runtime=new AdventureRuntime(looping),before=runtime.getState();
    assert.throws(()=>runtime.transaction(tx=>{
      tx.applyEffects({p148Flag:true});
      tx.dispatchEvent('P148_PING');
    }),/Limite de cascata UAE/);
    assert.deepEqual(runtime.getState(),before);
  });
  test(`P148 ${id}: callback async/thenable não pode contornar rollback`, () => {
    const runtime=new AdventureRuntime(module),before=runtime.getState();
    assert.throws(()=>runtime.transaction(async tx=>{tx.applyEffects({p148Async:true});}),/assíncrona/);
    assert.deepEqual(runtime.getState(),before);
    assert.throws(()=>runtime.transaction(tx=>{tx.applyEffects({p148Thenable:true});return Promise.resolve('late');}),/assíncrona/);
    assert.deepEqual(runtime.getState(),before);
    assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
  });
  test(`P148 ${id}: comando publicado Android salva, reabre e não reaplica no mesmo ID`, () => {
    const repo=new Repo(`p148:${id}`),controller=new GameController(repo);
    controller.loadStarterCharacter('cael');
    const key=`p148:${id}:start`;
    const result=controller.executePublishedAdventureCommand({actionId:key,kind:'start',adventureId:id});
    assert.equal(result.applied,true);
    assert.equal(validateState(repo.saved),true);
    const before=copy(repo.saved),writes=repo.writes;
    const reopen=new GameController(repo);
    const duplicate=reopen.executePublishedAdventureCommand({actionId:key,kind:'start',adventureId:id});
    assert.equal(duplicate.duplicate,true);
    assert.equal(repo.writes,writes);
    assert.deepEqual(repo.saved,before);
    const node=getPublishedAdventureAdapter(id).status(repo.saved.world).node;
    const choice=node.choices[0];
    assert.ok(choice);
    const moved=reopen.executePublishedAdventureCommand({actionId:`p148:${id}:choice`,kind:'choice',adventureId:id,choiceId:choice.id});
    assert.equal(moved.applied,true);
    assert.equal(validateState(repo.saved),true);
    assert.equal(new GameController(repo).state.world.publishedAdventureRuntime.currentNodeId,repo.saved.world.publishedAdventure.currentNodeId);
  });
}
test('P148: standalone legacy dispatch preserves the explicit cascade cap audit event',()=>{
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter('under-black-sun').meta);
  module.triggers.push({id:'p148-loop',event:'P148_PING',effects:[{type:'emit-event',eventType:'P148_PING'}]});
  const runtime=new AdventureRuntime(module);
  assert.equal(runtime.dispatchEvent('P148_PING').length,10);
  assert.equal(runtime.getState().canonicalEvents.filter(e=>e.type==='TRIGGER_CASCADE_LIMIT').length,1);
});
test('P148: unknown semantic condition fails closed rather than opening a branch',()=>{
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter('under-black-sun').meta);
  const runtime=new AdventureRuntime(module);
  assert.equal(runtime.isConditionMet('P148_UNRECOGNIZED:true'),false);
});
