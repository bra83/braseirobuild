/** P145: exactly-once published commands across process death, retries and storage faults. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { validatePublishedActionReceipts } from '../app/src/main/assets/vtt/js/adventure/publishedActionReceipts.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const clone = structuredClone;
class Repo {
  constructor(id) { this.state = createCampaign(id); this.writes = 0; this.fail = false; }
  load() { return clone(this.state); }
  save(state) { if (this.fail) throw Error('P145 simulated disk error'); this.state = clone(state); this.writes++; }
}
function game(id) { const repo = new Repo(id); const controller = new GameController(repo); controller.loadStarterCharacter('cael'); return {repo, controller}; }
function receipts(g) { return g.state.world.publishedAdventureActionReceipts ?? []; }
function eventCount(g) { return g.state.world.publishedAdventureRuntime.canonicalEvents.filter(e => e.type === 'PUBLISHED_COMMAND_COMMITTED').length; }
for (const {id} of publishedAdventureCatalog()) {
  test(`P145 ${id}: início e escolha têm recibos causais e retry sem duplicação após reabrir`, () => {
    const {repo, controller:g} = game(`p145:${id}`);
    const first = g.startPublishedAdventureSession(id, { actionId: `start:${id}`, rng: () => .5 });
    assert.equal(first.applied, true); assert.equal(receipts(g).length, 1); assert.equal(eventCount(g), 1);
    const startWrites = repo.writes;
    const repeated = g.startPublishedAdventureSession(id, { actionId: `start:${id}`, rng: () => .99 });
    assert.equal(repeated.duplicate, true); assert.equal(repo.writes, startWrites);
    const adapter = getPublishedAdventureAdapter(id);
    const choice = adapter.status(g.state.world).node.choices[0].id;
    const initialNode = g.state.world.publishedAdventure.currentNodeId;
    const moved = g.advancePublishedAdventure(choice, { actionId: `choice:${id}` });
    assert.equal(moved.applied, true);
    assert.notEqual(g.state.world.publishedAdventure.currentNodeId, initialNode);
    assert.equal(receipts(g).length, 2); assert.equal(eventCount(g), 2);
    const after = clone(g.state), beforeReopen = repo.writes;
    const reopened = new GameController(repo);
    const reopenWrites = repo.writes;
    assert.equal(reopenWrites, beforeReopen); // P145 reopen is read-only
    const retry = reopened.advancePublishedAdventure(choice, { actionId: `choice:${id}` });
    assert.equal(retry.duplicate, true); assert.equal(retry.applied, false);
    assert.equal(repo.writes, reopenWrites); assert.deepEqual(reopened.state, after);
    assert.deepEqual(repo.state, after);
    assert.throws(() => reopened.advancePublishedAdventure('different-choice', { actionId: `choice:${id}` }), /reutilizado/);
    assert.equal(repo.writes, reopenWrites);
    validatePublishedActionReceipts(reopened.state.world);
    validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(adapter.meta),
      reopened.state.world.publishedAdventureRuntime, reopened.state.world.publishedAdventure);
  });
  test(`P145 ${id}: teste de perícia só adjudica uma vez, inclusive após reinício`, () => {
    const {repo, controller:g} = game(`p145:check:${id}`);
    g.startPublishedAdventureSession(id, {rng:()=>.5});
    const checkSpecs = getPublishedAdventureAdapter(id).currentChecks(g.state.world);
    if (!checkSpecs.some(spec => spec.skillKey)) {
      const before = clone(g.state), writes = repo.writes;
      assert.throws(()=>g.resolvePublishedAdventureCheck(0,{actionId:`check:${id}`,rng:()=>.5}),/adjudicação manual/);
      assert.deepEqual(g.state,before); assert.equal(repo.writes,writes);
      assert.equal(receipts(g).length,0);
      return;
    }
    const first = g.resolvePublishedAdventureCheck(0, {actionId:`check:${id}`, rng:()=>.5});
    assert.ok(first.check); assert.equal(first.applied,true); assert.equal(receipts(g).length,1);
    const state = clone(g.state), writes = repo.writes;
    const second = g.resolvePublishedAdventureCheck(0, {actionId:`check:${id}`, rng:()=>.01});
    assert.equal(second.duplicate,true); assert.deepEqual(second.check,first.check);
    assert.equal(repo.writes,writes); assert.deepEqual(g.state,state);
    const reopened = new GameController(repo);
    const writesAfterReopen = repo.writes;
    const third = reopened.resolvePublishedAdventureCheck(0, {actionId:`check:${id}`, rng:()=>.9});
    assert.equal(third.duplicate,true); assert.equal(repo.writes,writesAfterReopen);
    assert.deepEqual(third.check,first.check); assert.deepEqual(reopened.state,state);
    assert.throws(()=>reopened.resolvePublishedAdventureCheck(1,{actionId:`check:${id}`}),/reutilizado/);
  });
  test(`P145 ${id}: falha na gravação reverte recibo, UAE, cena e recompensa`, () => {
    const {repo, controller:g} = game(`p145:fail:${id}`);
    g.startPublishedAdventureSession(id, {rng:()=>.5});
    const adapter = getPublishedAdventureAdapter(id);
    const choice = adapter.status(g.state.world).node.choices[0].id;
    const before = clone(g.state), writes = repo.writes;
    repo.fail = true;
    assert.throws(()=>g.advancePublishedAdventure(choice,{actionId:`retry:${id}`}),/disk error/);
    assert.deepEqual(g.state,before); assert.deepEqual(repo.state,before); assert.equal(repo.writes,writes);
    repo.fail = false;
    const success = g.advancePublishedAdventure(choice,{actionId:`retry:${id}`});
    assert.equal(success.applied,true); assert.equal(receipts(g).length,1); assert.equal(repo.writes,writes+1);
    const snapshot = clone(g.state);
    const retry = g.advancePublishedAdventure(choice,{actionId:`retry:${id}`});
    assert.equal(retry.duplicate,true); assert.deepEqual(g.state,snapshot);
  });
}
test('P145: encontro publicado e encerramento preservam causalidade e não recuperam Tensão duas vezes', () => {
  const {repo,controller:g} = game('p145:combat');
  g.startPublishedAdventureSession('under-black-sun',{rng:()=>.5});
  const start = g.beginPublishedAdventureEncounter(null,{actionId:'combat:start',rng:()=>.5});
  assert.equal(start.applied,true); assert.equal(start.encounter.id,'black-sun-pursuers');
  const snapshot = clone(g.state), writes=repo.writes;
  const again = g.beginPublishedAdventureEncounter(null,{actionId:'combat:start',rng:()=>.01});
  assert.equal(again.duplicate,true); assert.equal(repo.writes,writes); assert.deepEqual(g.state,snapshot);
  const done = g.finishCombat({actionId:'combat:end',rng:()=>.5});
  assert.equal(done.applied,true); assert.ok(done.publishedAdventureOutcome);
  const ended = clone(g.state), doneWrites=repo.writes;
  const replay = g.finishCombat({actionId:'combat:end',rng:()=>.99});
  assert.equal(replay.duplicate,true); assert.equal(repo.writes,doneWrites);
  assert.deepEqual(replay.publishedAdventureOutcome,done.publishedAdventureOutcome);
  assert.deepEqual(g.state,ended);
  const reopened=new GameController(repo);
  assert.equal(reopened.finishCombat({actionId:'combat:end'}).duplicate,true);
  assert.deepEqual(reopened.state,ended);
  assert.equal(receipts(reopened).length,2); assert.equal(eventCount(reopened),2);
});
test('P145: corrupção do livro de recibos impede novas ações e não altera armazenamento', () => {
  const {repo,controller:g}=game('p145:tamper');
  g.startPublishedAdventureSession('under-black-sun',{actionId:'start:tamper',rng:()=>.5});
  const prior=clone(g.state); const count=repo.writes;
  g.state.world.publishedAdventureActionReceipts[0].request='{"kind":"check","adventureId":"under-black-sun","args":{}}';
  assert.throws(()=>g.resolvePublishedAdventureCheck(0,{actionId:'new:tamper',rng:()=>.5}),/Assinatura de recibo/);
  assert.equal(repo.writes,count); assert.deepEqual(repo.state,prior);
});
test('P145: idempotency key malformada ou reutilizada em outra operação é rejeitada', () => {
  const {repo,controller:g}=game('p145:key');
  g.startPublishedAdventureSession('under-black-sun',{actionId:'start:key',rng:()=>.5});
  assert.throws(()=>g.advancePublishedAdventure('anything',{actionId:'__proto__'}),/Identificador/);
  assert.throws(()=>g.resolvePublishedAdventureCheck(0,{actionId:'start:key'}),/reutilizado/);
  assert.equal(receipts(g).length,1);
  const altered=clone(g.state.world);
  altered.publishedAdventureActionReceipts.push(clone(altered.publishedAdventureActionReceipts[0]));
  assert.throws(()=>validatePublishedActionReceipts(altered),/duplicado|corrompido/);
});
test('P145: bridge Android expõe execução idempotente e reabertura não regrava storage', async () => {
  const {BraseiroUiBridge} = await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const {repo,controller:g} = game('p145:bridge');
  g.startPublishedAdventureSession('under-black-sun',{rng:()=>.5});
  const choice=getPublishedAdventureAdapter('under-black-sun').status(g.state.world).node.choices[0].id;
  const bridge=new BraseiroUiBridge({repository:repo,snapshots:{}});
  const before=repo.writes;
  const first=bridge.executePublishedAdventureCommand({kind:'choice',actionId:'bridge:choice',choiceId:choice});
  assert.equal(first.ok,true,first.message);assert.equal(first.data.applied,true);
  assert.equal(repo.writes,before+1);
  const saved=clone(repo.state),writes=repo.writes;
  const replay=bridge.executePublishedAdventureCommand({kind:'choice',actionId:'bridge:choice',choiceId:choice});
  assert.equal(replay.ok,true,replay.message);assert.equal(replay.data.duplicate,true);
  assert.equal(repo.writes,writes);assert.deepEqual(repo.state,saved);
  const conflict=bridge.executePublishedAdventureCommand({kind:'check',actionId:'bridge:choice',checkIndex:0});
  assert.equal(conflict.ok,false);assert.match(conflict.message,/reutilizado/);
  assert.equal(repo.writes,writes);
});
test('P145: importação de save adulterado é bloqueada antes da normalização do mundo', async () => {
  const {validateState,migrateState} = await import('../app/src/main/assets/vtt/js/core/state.mjs');
  const {repo,controller:g}=game('p145:import');
  g.startPublishedAdventureSession('under-black-sun',{actionId:'start:import',rng:()=>.5});
  const clean=clone(repo.state);
  assert.equal(validateState(clean),true);
  const tampered=clone(clean);
  tampered.world.publishedAdventureActionReceipts[0].sequence=999;
  assert.throws(()=>validateState(tampered),/Recibo publicado/);
  assert.throws(()=>migrateState(tampered),/Recibo publicado/);
  assert.deepEqual(repo.state,clean);
});
for (const {id} of publishedAdventureCatalog()) {
  test(`P145 ${id}: três reaberturas mantêm a prova causal sem reexecutar ação`, () => {
    const {repo,controller:g}=game(`p145:reopen:${id}`);
    g.startPublishedAdventureSession(id,{actionId:`start:reopen:${id}`,rng:()=>.5});
    const choice=getPublishedAdventureAdapter(id).status(g.state.world).node.choices[0].id;
    g.advancePublishedAdventure(choice,{actionId:`choice:reopen:${id}`});
    const checkpoint=clone(repo.state), writes=repo.writes;
    for(let attempt=0;attempt<3;attempt++) {
      const reopened=new GameController(repo);
      assert.equal(reopened.advancePublishedAdventure(choice,{actionId:`choice:reopen:${id}`}).duplicate,true);
      assert.equal(reopened.startPublishedAdventureSession(id,{actionId:`start:reopen:${id}`}).duplicate,true);
      assert.deepEqual(repo.state,checkpoint);assert.equal(repo.writes,writes);
    }
  });
}
test('P145: recibos anteriores não podem ser reaplicados após reiniciar a mesma aventura',()=>{
  const {repo,controller:g}=game('p145:restart');
  g.startPublishedAdventureSession('under-black-sun',{actionId:'start:first',rng:()=>.5});
  const choice=getPublishedAdventureAdapter('under-black-sun').status(g.state.world).node.choices[0].id;
  g.advancePublishedAdventure(choice,{actionId:'choice:first'});
  g.startPublishedAdventureSession('under-black-sun',{actionId:'start:second',rng:()=>.5});
  const after=clone(repo.state),writes=repo.writes;
  assert.throws(()=>g.advancePublishedAdventure(choice,{actionId:'choice:first'}),/outra execução|prova causal/);
  assert.equal(repo.writes,writes);assert.deepEqual(g.state,after);
});
