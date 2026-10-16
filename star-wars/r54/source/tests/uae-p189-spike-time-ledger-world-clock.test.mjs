import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateTaskTimeLedger} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {replayJewelBankPreparation} from '../app/src/main/assets/vtt/js/adventure/jewelBankPreparations.mjs';
const copy=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P189');this.saves=0;}load(){return copy(this.state);}save(s){this.state=copy(s);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);return {g,repo};}
function fund(g){g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:600}});}
function spike(g,actionId='spike-1'){return g.prepareJewelBank({actionId,step:'spike',rng:()=>.49});}
test('P189 ten hours are committed to the canonical world task clock, not just an adventure receipt',()=>{
 const {g}=game();fund(g);const before=g.state.world.clock.elapsedMinutes??0;
 const r=spike(g);assert.equal(r.adjudication.hoursSpent,10);assert.equal(r.adjudication.timeCheckId,'task_jewel_spike_spike-1');
 assert.equal(g.state.world.clock.elapsedMinutes,before+600);
 assert.deepEqual(g.state.world.taskTimeApplications['task_jewel_spike_spike-1'],{checkId:'task_jewel_spike_spike-1',skillKey:'computadoras',minutes:600,baseMinutes:600});
 assert.equal(validateTaskTimeLedger(g.state.world).elapsedMinutes,before+600);
 assert.equal(g.state.player.character.credits,300);
});
test('P189 failed crafting still costs 300 credits and ten hours',()=>{
 const {g}=game();fund(g);const r=g.prepareJewelBank({actionId:'failed',step:'spike',rng:()=>.99});
 assert.equal(r.adjudication.success,false);assert.equal(r.preparation.spike.boost,0);
 assert.equal(g.state.player.character.credits,300);assert.equal(g.state.world.clock.elapsedMinutes,600);
});
test('P189 no double charge when the action ID is repeated; atomic state and saves unchanged',()=>{
 const {g,repo}=game();fund(g);spike(g);const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>spike(g),/registrad|fabricad/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P189 rejected crafting with insufficient credits leaves clock and world unchanged',()=>{
 const {g,repo}=game();const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>spike(g),/300 créditos/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P189 forged or missing task ledger cannot pass adventure status validation',()=>{
 const {g}=game();fund(g);spike(g);
 for(const mutate of [
  w=>delete w.taskTimeApplications['task_jewel_spike_spike-1'],
  w=>w.taskTimeApplications['task_jewel_spike_spike-1'].minutes=300,
  w=>w.publishedAdventure.operations.receipts['spike-1'].timeCheckId='task_jewel_spike_other',
  w=>w.clock.elapsedMinutes=0,
 ]){const w=copy(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/relógio|tempo|fabricação|adulterad|diverge|tarefa|Vínculo/i);}
});
test('P189 three reopenings preserve world clock, credits and spike receipt',()=>{
 const {g,repo}=game();fund(g);spike(g);const original=copy(g.state.world);
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(restored.state.world.clock.elapsedMinutes,600);assert.equal(restored.state.player.character.credits,300);assert.deepEqual(restored.state.world.taskTimeApplications,original.taskTimeApplications);assert.equal(adapter.status(restored.state.world).active,true);}
});
test('P189 legacy P188 spike receipts remain valid without retroactively charging time',()=>{
 const {g}=game();fund(g);spike(g);
 const w=copy(g.state.world),p=w.publishedAdventure;
 delete p.operations.receipts['spike-1'].timeCheckId;
 delete p.operations.bankPreparation.history[0].timeCheckId;
 delete w.taskTimeApplications['task_jewel_spike_spike-1'];w.clock.elapsedMinutes=0;
 assert.equal(adapter.status(w).active,true);
 assert.equal(replayJewelBankPreparation(p.operations.bankPreparation.history).spike.hoursSpent,10);
});
test('P189 spike crafting does not incorrectly advance abstract world ticks (separate event-turn clock)',()=>{
 const {g}=game();fund(g);const tick=g.state.world.clock.tick;spike(g);
 assert.equal(g.state.world.clock.tick,tick);assert.equal(g.state.world.clock.elapsedMinutes,600);
});
function toBank(g){for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'round-'+i});}g.advancePublishedAdventure('bank-operation');}
function bank(g,step,id,rng=()=>.99){return g.resolveJewelBankStep({actionId:id,step,target:'industrial_docks',restrainingBolt:false,rng});}
test('P189 bank retry charges thirty minutes to canonical clock after a failed first slicing',()=>{
 const {g}=game();toBank(g);
 bank(g,'acquire','capture',()=>.49);bank(g,'disable','disable',()=>.49);
 const first=bank(g,'slice','slice-first');assert.equal(first.adjudication.success,false);
 assert.equal(g.state.world.clock.elapsedMinutes??0,0);
 const second=bank(g,'slice','slice-second');assert.equal(second.adjudication.timeCheckId,'task_jewel_retry_slice-second');
 assert.equal(g.state.world.clock.elapsedMinutes,30);
 assert.equal(g.state.world.taskTimeApplications['task_jewel_retry_slice-second'].minutes,30);
 assert.equal(adapter.status(g.state.world).active,true);
});
test('P189 retry receipt with removed time ledger or forged time key is rejected',()=>{
 const {g}=game();toBank(g);bank(g,'acquire','capture',()=>.49);bank(g,'disable','disable',()=>.49);
 bank(g,'slice','slice-first');bank(g,'slice','slice-second');
 const w=copy(g.state.world);delete w.taskTimeApplications['task_jewel_retry_slice-second'];
 assert.throws(()=>adapter.status(w),/tempo|relógio|diverge|trinta/i);
 const forged=copy(g.state.world);forged.publishedAdventure.operations.receipts['slice-second'].timeCheckId='task_jewel_retry_other';
 assert.throws(()=>adapter.status(forged),/tempo|adulterado|relógio/i);
});
test('P189 bank retry idempotency protects clock and saves',()=>{
 const {g,repo}=game();toBank(g);bank(g,'acquire','capture',()=>.49);bank(g,'disable','disable',()=>.49);
 bank(g,'slice','slice-first');bank(g,'slice','slice-second');const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>bank(g,'slice','slice-second'),/registrada/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P189 failed bank retries retain thirty-minute charges after three reopenings',()=>{
 const {g,repo}=game();toBank(g);bank(g,'acquire','capture',()=>.49);bank(g,'disable','disable',()=>.49);
 bank(g,'slice','slice-first');bank(g,'slice','slice-second');bank(g,'slice','slice-third');
 assert.equal(g.state.world.clock.elapsedMinutes,60);
 for(let i=0;i<3;i++){const r=new GameController(repo);assert.equal(r.state.world.clock.elapsedMinutes,60);assert.equal(adapter.status(r.state.world).active,true);}
});
