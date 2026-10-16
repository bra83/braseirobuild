/** P147: monotonic run IDs, cross-ledger command proof, crash-safe combat outbox. */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState, migrateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { validatePublishedActionCausality } from '../app/src/main/assets/vtt/js/adventure/publishedActionReceipts.mjs';
import { importStateJson } from '../app/src/main/assets/vtt/js/core/storage.mjs';
const clone = structuredClone;
const script = fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/published-command-client.js', import.meta.url), 'utf8');
class Repo {
  constructor(name) { this.saved = createCampaign(name); this.writes = 0; this.fail = false; }
  load() { return clone(this.saved); }
  save(value) { if (this.fail) throw Error('disk unavailable'); this.saved = clone(value); this.writes++; }
}
class Store {
  constructor() { this.values = new Map(); }
  getItem(key) { return this.values.get(key) ?? null; }
  setItem(key,value) { this.values.set(key,String(value)); }
  removeItem(key) { this.values.delete(key); }
}
function setup(name) {
  const repo = new Repo(name), store = new Store();
  let game = new GameController(repo), lostAck = false, id = 0, sandboxFinishes = 0;
  game.loadStarterCharacter('cael');
  const bridge = {
    readState() { return {ok:true,data:repo.load()}; },
    executePublishedAdventureCommand(command) {
      try {
        const result = game.executePublishedAdventureCommand(command);
        if (lostAck) { lostAck = false; return {ok:false,error:'ENGINE_EXCEPTION',message:'lost ACK'}; }
        return {ok:true,data:result};
      } catch(error) { return {ok:false,error:'PUBLISHED_ADVENTURE_FAILED',message:error.message}; }
    },
    finishCombat(options) { sandboxFinishes++; return {ok:true,data:game.finishCombat(options)}; }
  };
  function open() {
    const window = {localStorage:store,BraseiroGameBridge:bridge,
      crypto:{randomUUID:()=>`00000000-0000-4000-8000-${String(++id).padStart(12,'0')}`}};
    vm.runInNewContext(script,{window});
    return window.BraseiroPublishedActionClient;
  }
  return {repo,store,bridge,open,loseAck(){lostAck=true},reopen(){game=new GameController(repo);return open()},
    get game(){return game},get sandboxFinishes(){return sandboxFinishes}};
}
for (const {id} of publishedAdventureCatalog()) {
  test(`P147 ${id}: reinícios no mesmo milissegundo geram identidades distintas e prova causal`,()=>{
    const f=setup(`p147:serial:${id}`);
    const savedDate=globalThis.Date;
    class FrozenDate extends savedDate {
      constructor(...args) { super(...(args.length ? args : ['2026-10-11T20:00:00.000Z'])); }
      static now(){return 1791748800000;}
    }
    let a,b,c;
    try {
      globalThis.Date=FrozenDate;
      a=f.game.executePublishedAdventureCommand({actionId:`p147:${id}:start:1`,kind:'start',adventureId:id});
      b=f.game.executePublishedAdventureCommand({actionId:`p147:${id}:start:2`,kind:'start',adventureId:id});
      c=f.game.executePublishedAdventureCommand({actionId:`p147:${id}:start:3`,kind:'start',adventureId:id});
    } finally {globalThis.Date=savedDate;}
    assert.equal(a.applied,true);assert.equal(b.applied,true);assert.equal(c.applied,true);
    const world=f.repo.saved.world;
    assert.equal(world.publishedAdventureRunSerial,3);
    const receipts=world.publishedAdventureActionReceipts;
    assert.deepEqual(receipts.map(r=>r.runKey),[`run:${id}:1`,`run:${id}:2`,`run:${id}:3`]);
    assert.ok(receipts.every(r=>r.proofVersion===2));
    const proof=world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED');
    assert.equal(proof.payload.proofVersion,2);
    assert.equal(proof.payload.request,receipts[2].request);
    assert.equal(proof.payload.receiptSequence,3);
    assert.equal(proof.payload.runKey,receipts[2].runKey);
    assert.equal(validateState(f.repo.saved),true);
    const before=clone(f.repo.saved), writes=f.repo.writes;
    f.reopen();
    const replay=f.game.executePublishedAdventureCommand({actionId:`p147:${id}:start:3`,kind:'start',adventureId:id});
    assert.equal(replay.duplicate,true);assert.equal(replay.applied,false);
    assert.equal(f.repo.writes,writes);assert.deepEqual(f.repo.saved,before);
    assert.throws(()=>f.game.executePublishedAdventureCommand({actionId:`p147:${id}:start:1`,kind:'start',adventureId:id}),/outra execução/);
  });
  test(`P147 ${id}: recibo e journal adulterados não passam pela importação`,()=>{
    const f=setup(`p147:proof:${id}`);
    f.game.executePublishedAdventureCommand({actionId:`p147:${id}:proof`,kind:'start',adventureId:id});
    const base=clone(f.repo.saved);
    const variants = [
      s=>{s.world.publishedAdventureActionReceipts[0].request='{"adventureId":"ghost","args":{},"kind":"start"}';},
      s=>{s.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED').payload.request='forged';},
      s=>{s.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED').payload.receiptSequence=99;},
      s=>{delete s.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED').payload.proofVersion;},
      s=>{delete s.world.publishedAdventureActionReceipts[0].proofVersion;},
      s=>{s.world.publishedAdventureRunSerial=2;},
      s=>{s.world.publishedAdventureRunSerial=0;},
    ];
    for (const mutate of variants) {
      const corrupted=clone(base);mutate(corrupted);
      assert.throws(()=>validateState(corrupted),undefined,'corrupt state accepted');
      assert.throws(()=>migrateState(corrupted),undefined,'corrupt migration accepted');
      assert.throws(()=>importStateJson(JSON.stringify(corrupted)),undefined,'corrupt import accepted');
    }
    assert.equal(validatePublishedActionCausality(base.world),true);
  });
  test(`P147 ${id}: prova nova não exige reescrever eventos legados P146`,()=>{
    const f=setup(`p147:legacy:${id}`);
    f.game.executePublishedAdventureCommand({actionId:`p147:${id}:legacy`,kind:'start',adventureId:id});
    const state=clone(f.repo.saved),receipt=state.world.publishedAdventureActionReceipts[0];
    const event=state.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED');
    delete state.world.publishedAdventureRunSerial;
    delete receipt.proofVersion;
    receipt.runKey=JSON.stringify([state.world.publishedAdventureRuntime.adventureId,
      state.world.publishedAdventureRuntime.startedAt,state.world.publishedAdventure.startedAt]);
    delete event.payload.proofVersion;delete event.payload.request;
    delete event.payload.receiptSequence;delete event.payload.runKey;
    assert.equal(validateState(state),true);
    assert.equal(validateState(importStateJson(JSON.stringify(state))),true);
  });
}
test('P147: ACK perdido ao terminar combate publicado não pode cair no combate sandbox',()=>{
  const f=setup('p147:combat-ack'), client=f.open();
  assert.equal(client.execute('start','under-black-sun').ok,true);
  assert.equal(client.execute('encounter','under-black-sun',{encounterId:null}).ok,true);
  f.loseAck();
  const first=client.finishCombat({});
  assert.equal(first.ok,false);assert.equal(first.error,'ENGINE_EXCEPTION');
  assert.equal(f.repo.saved.combat.active,false);
  const saved=clone(f.repo.saved),writes=f.repo.writes;
  const second=client.finishCombat({});
  assert.equal(second.error,'PREVIOUS_ACTION_RECOVERED');
  assert.equal(f.sandboxFinishes,0);
  assert.equal(f.repo.writes,writes);assert.deepEqual(f.repo.saved,saved);
  assert.equal(f.store.getItem('braseiro.starwars.published.pending.v1'),null);
  assert.equal(validateState(f.repo.saved),true);
});
test('P147: ACK perdido em escolha também bloqueia finishCombat até a recuperação',()=>{
  const f=setup('p147:pending-choice'), client=f.open();
  assert.equal(client.execute('start','under-black-sun').ok,true);
  const choice=getPublishedAdventureAdapter('under-black-sun').status(f.repo.saved.world).node.choices[0].id;
  f.loseAck();assert.equal(client.execute('choice','under-black-sun',{choiceId:choice}).ok,false);
  const result=client.finishCombat({});
  assert.equal(result.error,'PREVIOUS_ACTION_RECOVERED');
  assert.equal(f.sandboxFinishes,0);
});
test('P147: falha de disco em START não incrementa serial nem cria recibo órfão',()=>{
  const f=setup('p147:rollback'), client=f.open();
  const before=clone(f.repo.saved);
  f.repo.fail=true;
  assert.equal(client.execute('start','under-black-sun').ok,false);
  assert.deepEqual(f.repo.saved,before);
  f.repo.fail=false;
  assert.equal(client.execute('start','under-black-sun').ok,true);
  assert.equal(f.repo.saved.world.publishedAdventureRunSerial,1);
  assert.equal(f.repo.saved.world.publishedAdventureActionReceipts.length,1);
});
