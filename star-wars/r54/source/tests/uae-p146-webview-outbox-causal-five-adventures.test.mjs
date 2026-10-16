/** P146: real WebView outbox + UAE journal/receipt cross-ledger integration. */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState, migrateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { validatePublishedActionCausality } from '../app/src/main/assets/vtt/js/adventure/publishedActionReceipts.mjs';
import { importStateJson } from '../app/src/main/assets/vtt/js/core/storage.mjs';
const copy = structuredClone;
const script = fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/published-command-client.js', import.meta.url), 'utf8');
const shell = fs.readFileSync(new URL('../app/src/main/assets/vtt/r46-shell.html', import.meta.url), 'utf8');
const runtime = fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js', import.meta.url), 'utf8');
class Repo {
  constructor(name) { this.saved = createCampaign(name); this.writes = 0; this.fail = false; }
  load() { return copy(this.saved); }
  save(s) { if (this.fail) throw Error('simulated disk failure'); this.saved = copy(s); this.writes++; }
}
class Storage {
  constructor() { this.values = new Map(); this.failWrite = false; }
  getItem(k) { return this.values.get(k) ?? null; }
  setItem(k,v) { if (this.failWrite) throw Error('WebView storage full'); this.values.set(k,String(v)); }
  removeItem(k) { this.values.delete(k); }
}
function fixture(name) {
  const repo = new Repo(name);
  let game = new GameController(repo);
  game.loadStarterCharacter('cael');
  const storage = new Storage();
  let calls = 0, lostAck = false, ids = 0;
  const bridge = {
    readState() { return {ok:true,data:repo.load()}; },
    executePublishedAdventureCommand(command) {
      calls++;
      try {
        const data = game.executePublishedAdventureCommand(command);
        if (lostAck) { lostAck = false; return {ok:false,error:'ENGINE_EXCEPTION',message:'WebView destroyed after commit'}; }
        return {ok:true,data};
      } catch (error) { return {ok:false,error:'PUBLISHED_ADVENTURE_FAILED',message:error.message}; }
    },
    finishCombat(options) {
      try { return {ok:true,data:game.finishCombat(options)}; }
      catch(error) {return {ok:false,error:'COMBAT_FAILED',message:error.message};}
    }
  };
  function open() {
    const window = {localStorage:storage,BraseiroGameBridge:bridge,
      crypto:{randomUUID(){return `00000000-0000-4000-8000-${String(++ids).padStart(12,'0')}`;}}};
    vm.runInNewContext(script,{window},{filename:'published-command-client.js'});
    return window;
  }
  return {repo,storage,bridge,open,reopen(){game=new GameController(repo);return open();},
    get game(){return game},get calls(){return calls},loseAck(){lostAck=true}};
}
const pendingKey = 'braseiro.starwars.published.pending.v1';
for (const {id} of publishedAdventureCatalog()) {
  test(`P146 ${id}: escolha real, reabertura, recibo e prova causal íntegros`, () => {
    const f=fixture(`p146:${id}`), w=f.open();
    const started=w.BraseiroPublishedActionClient.execute('start',id);
    assert.equal(started.ok,true);assert.equal(started.data.applied,true);
    assert.equal(f.storage.getItem(pendingKey),null);
    const choice=getPublishedAdventureAdapter(id).status(f.repo.saved.world).node.choices[0].id;
    const moved=w.BraseiroPublishedActionClient.execute('choice',id,{choiceId:choice});
    assert.equal(moved.ok,true);assert.equal(moved.data.applied,true);
    validatePublishedActionCausality(f.repo.saved.world);validateState(f.repo.saved);
    const before=copy(f.repo.saved),writes=f.repo.writes;
    const reopened=f.reopen();
    assert.equal(reopened.__BRASEIRO_PUBLISHED_RECOVERY__,undefined);
    assert.equal(f.repo.writes,writes);assert.deepEqual(f.repo.saved,before);
    assert.equal(f.repo.saved.world.publishedAdventureActionReceipts.length,2);
  });
  test(`P146 ${id}: ACK perdido após commit não duplica escolha nem recompensa`, () => {
    const f=fixture(`p146:ack:${id}`), w=f.open();
    assert.equal(w.BraseiroPublishedActionClient.execute('start',id).ok,true);
    const choice=getPublishedAdventureAdapter(id).status(f.repo.saved.world).node.choices[0].id;
    f.loseAck();
    const failed=w.BraseiroPublishedActionClient.execute('choice',id,{choiceId:choice});
    assert.equal(failed.ok,false);assert.ok(f.storage.getItem(pendingKey));
    const checkpoint=copy(f.repo.saved),writes=f.repo.writes;
    const reloaded=f.reopen();
    assert.equal(reloaded.__BRASEIRO_PUBLISHED_RECOVERY__?.ok,true);
    assert.equal(reloaded.__BRASEIRO_PUBLISHED_RECOVERY__?.data?.duplicate,true);
    assert.equal(f.storage.getItem(pendingKey),null);
    assert.equal(f.repo.writes,writes);assert.deepEqual(f.repo.saved,checkpoint);
    validatePublishedActionCausality(f.repo.saved.world);
  });
  test(`P146 ${id}: adulteração do journal não atravessa validação/importação`, () => {
    const f=fixture(`p146:causal:${id}`),w=f.open();
    assert.equal(w.BraseiroPublishedActionClient.execute('start',id).ok,true);
    const snapshot=copy(f.repo.saved);
    const event=snapshot.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED');
    assert.ok(event);
    event.payload.kind='choice';
    assert.throws(()=>validateState(snapshot),/Prova causal/);
    assert.throws(()=>migrateState(snapshot),/Prova causal/);
    assert.throws(()=>importStateJson(JSON.stringify(snapshot)),/Prova causal/);
    assert.equal(validateState(f.repo.saved),true);
  });
}
test('P146: comando pendente de outra campanha é preservado e bloqueia mutações',()=>{
  const f=fixture('p146:cross-campaign'),w=f.open();
  assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').ok,true);
  const saved=copy(f.repo.saved);
  f.storage.setItem(pendingKey,JSON.stringify({version:1,campaignId:'camp:foreign',actionId:'pub:foreign',
    command:{kind:'start',adventureId:'under-black-sun'},fingerprint:JSON.stringify({kind:'start',adventureId:'under-black-sun'})}));
  const next=f.reopen();
  assert.equal(next.__BRASEIRO_PUBLISHED_RECOVERY__?.error,'CAMPAIGN_CHANGED');
  assert.equal(next.BraseiroPublishedActionClient.execute('start','under-black-sun').error,'CAMPAIGN_CHANGED');
  assert.deepEqual(f.repo.saved,saved);
  assert.ok(f.storage.getItem(pendingKey));
});
test('P146: falha ao gravar outbox impede qualquer efeito no mundo',()=>{
  const f=fixture('p146:storage'),w=f.open();const before=copy(f.repo.saved),writes=f.repo.writes;
  f.storage.failWrite=true;
  assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').error,'OUTBOX_FAILED');
  assert.deepEqual(f.repo.saved,before);assert.equal(f.repo.writes,writes);assert.equal(f.calls,0);
});
test('P146: falha determinística de gravação reverte estado e libera retry seguro',()=>{
  const f=fixture('p146:disk'),w=f.open();const before=copy(f.repo.saved),writes=f.repo.writes;
  f.repo.fail=true;
  const failure=w.BraseiroPublishedActionClient.execute('start','under-black-sun');
  assert.equal(failure.ok,false);assert.equal(f.storage.getItem(pendingKey),null);
  assert.deepEqual(f.repo.saved,before);assert.equal(f.repo.writes,writes);
  f.repo.fail=false;
  assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').ok,true);
});
test('P146: recusa de outro comando recupera o pendente antes de qualquer nova ação',()=>{
  const f=fixture('p146:ordering'),w=f.open();
  f.loseAck();assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').ok,false);
  const writes=f.repo.writes;
  const next=w.BraseiroPublishedActionClient.execute('choice','under-black-sun',{choiceId:'anything'});
  assert.equal(next.error,'PREVIOUS_ACTION_RECOVERED');assert.equal(f.repo.writes,writes);
  assert.equal(f.storage.getItem(pendingKey),null);
});
test('P146: combate publicado usa outbox; combate sandbox preserva rota legada',()=>{
  const f=fixture('p146:combat'),w=f.open();
  assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').ok,true);
  assert.equal(w.BraseiroPublishedActionClient.execute('encounter','under-black-sun',{encounterId:null}).ok,true);
  const result=w.BraseiroPublishedActionClient.finishCombat({});
  assert.equal(result.ok,true);assert.equal(result.data.applied,true);
  assert.equal(f.repo.saved.world.publishedAdventureActionReceipts.length,3);
  assert.equal(w.BraseiroPublishedActionClient.finishCombat({}).ok,false); // no combat remains
  validatePublishedActionCausality(f.repo.saved.world);
});
test('P146: shell loads outbox before UI and routes all five published operations',()=>{
  assert.ok(shell.indexOf('r46/published-command-client.js') < shell.indexOf('r46/final-runtime.js'));
  for(const kind of ["'start'","'choice'","'check'","'encounter'"])
    assert.ok(runtime.includes(`BraseiroPublishedActionClient.execute(${kind}`),kind);
  assert.equal((runtime.match(/BraseiroPublishedActionClient.finishCombat\(/g)||[]).length,2);
  assert.ok(!runtime.includes("bridgeResult('advancePublishedAdventure'"));
  assert.ok(!runtime.includes("bridgeResult('resolvePublishedAdventureCheck'"));
});
for (const {id} of publishedAdventureCatalog()) {
  test(`P146 ${id}: remoção de evento causal da execução atual bloqueia checkpoint`,()=>{
    const f=fixture(`p146:orphan:${id}`),w=f.open();
    assert.equal(w.BraseiroPublishedActionClient.execute('start',id).ok,true);
    const state=copy(f.repo.saved);
    assert.ok(state.world.publishedAdventureActionReceipts[0].runKey);
    state.world.publishedAdventureRuntime.canonicalEvents = state.world.publishedAdventureRuntime.canonicalEvents
      .filter(e=>e.type!=='PUBLISHED_COMMAND_COMMITTED');
    assert.throws(()=>validateState(state),/perdeu sua prova causal/);
    assert.throws(()=>importStateJson(JSON.stringify(state)),/perdeu sua prova causal/);
  });
  test(`P146 ${id}: recibo antigo não contamina nova execução da aventura`,()=>{
    const f=fixture(`p146:restart:${id}`),w=f.open();
    assert.equal(w.BraseiroPublishedActionClient.execute('start',id).ok,true);
    const old=f.repo.saved.world.publishedAdventureActionReceipts[0];
    assert.equal(w.BraseiroPublishedActionClient.execute('start',id).ok,true);
    assert.equal(f.repo.saved.world.publishedAdventureActionReceipts.length,2);
    assert.notEqual(f.repo.saved.world.publishedAdventureActionReceipts[1].runKey,old.runKey);
    assert.equal(validateState(f.repo.saved),true);
    assert.equal(validatePublishedActionCausality(f.repo.saved.world),true);
  });
}
test('P146: evento UAE de comando sem recibo correspondente é rejeitado',()=>{
  const f=fixture('p146:orphan-event'),w=f.open();
  assert.equal(w.BraseiroPublishedActionClient.execute('start','under-black-sun').ok,true);
  const forged=copy(f.repo.saved);
  const event=forged.world.publishedAdventureRuntime.canonicalEvents.find(e=>e.type==='PUBLISHED_COMMAND_COMMITTED');
  event.idempotencyKey='pub:ghost';event.payload.actionId='pub:ghost';
  assert.throws(()=>validateState(forged),/Prova causal/);
  assert.equal(validateState(f.repo.saved),true);
});
test('P146: outbox corrompido não é apagado nem executado silenciosamente',()=>{
  const f=fixture('p146:outbox-corrupt'),w=f.open();
  const before=copy(f.repo.saved),writes=f.repo.writes;
  f.storage.setItem(pendingKey,'{broken-json');
  const reopened=f.reopen();
  assert.equal(reopened.__BRASEIRO_PUBLISHED_RECOVERY__?.error,'RECOVERY_FAILED');
  assert.equal(reopened.BraseiroPublishedActionClient.execute('start','under-black-sun').error,'OUTBOX_FAILED');
  assert.equal(f.storage.getItem(pendingKey),'{broken-json');
  assert.deepEqual(f.repo.saved,before);assert.equal(f.repo.writes,writes);
});
