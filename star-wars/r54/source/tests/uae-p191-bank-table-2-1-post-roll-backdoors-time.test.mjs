import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {replayJewelSlicingSpends} from '../app/src/main/assets/vtt/js/adventure/jewelBankSlicingSpends.mjs';
import {readFileSync} from 'node:fs';
const clone=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P191');this.saves=0;}load(){return clone(this.state)}save(s){this.state=clone(s);this.saves++}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});return {g,repo};}
function toBank(g){for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'round-'+i});}g.advancePublishedAdventure('bank-operation');}
function bank(g){toBank(g);g.resolveJewelBankStep({actionId:'acquire',step:'acquire',target:'industrial_docks',rng:()=>.49});g.resolveJewelBankStep({actionId:'disable',step:'disable',rng:()=>.49});}
function step(g,id,rng){return g.resolveJewelBankStep({actionId:id,step:'slice',rng:()=>rng});}
function spend(g,id,bankStepId,choice){return g.spendJewelBankSymbols({actionId:id,bankStepId,choice});}
function strong(g){const c=clone(g.state.player.character);c.characteristics.intelecto=7;c.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:c});}
function invalid(g,details,pattern){const before=clone(g.state),saves=g.repository?.saves;assert.throws(()=>g.spendJewelBankSymbols(details),pattern);assert.deepEqual(g.state,before);if(saves!==undefined)assert.equal(g.repository.saves,saves);}
test('P191 PDF p70: one advantage creates one real boost for next bank slicing roll',()=>{
 const {g}=game();bank(g);strong(g);const first=step(g,'slice1',.89);assert.equal(first.adjudication.success,false);assert.ok(first.adjudication.evidence.roll.netAdvantage>=1);
 spend(g,'spend1','slice1',{backdoorBoosts:1});const next=step(g,'slice2',.89);
 assert.equal(next.adjudication.backdoor.boosts,1);assert.equal(next.adjudication.evidence.pool.boost,1);
 assert.equal(g.state.world.publishedAdventure.operations.bankSpending.pendingBoosts,0);assert.equal(adapter.status(g.state.world).active,true);
});
test('P191 PDF p70: two advantages upgrade positive dice in the next check',()=>{
 const {g}=game();bank(g);strong(g);const first=step(g,'slice1',.89);assert.ok(first.adjudication.evidence.roll.netAdvantage>=2);
 spend(g,'spend1','slice1',{backdoorUpgrades:1});const next=step(g,'slice2',.89);
 assert.equal(next.adjudication.backdoor.upgrades,1);assert.equal(next.adjudication.evidence.pool.proficiency,7);
 assert.equal(next.adjudication.evidence.pool.ability,1);assert.equal(adapter.status(g.state.world).active,true);
});
test('P191 PDF p70: two threats cost 15 actual elapsed minutes and durable ledger',()=>{
 const {g,repo}=game();bank(g);const first=step(g,'slice1',.6);assert.ok(first.adjudication.evidence.roll.netAdvantage<=-2);
 const before=g.state.world.clock.elapsedMinutes??0;const r=spend(g,'spend1','slice1',{delay15:1});
 assert.equal(r.adjudication.minutes,15);assert.equal(g.state.world.clock.elapsedMinutes,before+15);
 assert.equal(g.state.world.taskTimeApplications['task_jewel_slicing_delay_spend1'].minutes,15);
 assert.equal(adapter.status(g.state.world).active,true);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.state.world.clock.elapsedMinutes,before+15);assert.equal(adapter.status(reopened.state.world).active,true);}
});
test('P191 cannot spend more advantages than rolled or threats on advantage result',()=>{
 const {g}=game();bank(g);strong(g);step(g,'slice1',.89);
 invalid(g,{actionId:'bad',bankStepId:'slice1',choice:{backdoorBoosts:12}},/Símbolos insuficientes/);
 invalid(g,{actionId:'bad',bankStepId:'slice1',choice:{delay15:1}},/Símbolos insuficientes/);
});
test('P191 spending must follow a real latest bank slice and cannot be replayed',()=>{
 const {g}=game();bank(g);strong(g);step(g,'slice1',.89);
 invalid(g,{actionId:'bad',bankStepId:'forged',choice:{backdoorBoosts:1}},/fora da última/);
 spend(g,'spend1','slice1',{backdoorBoosts:1});
 invalid(g,{actionId:'bad2',bankStepId:'slice1',choice:{backdoorBoosts:1}},/duplicado/);
 invalid(g,{actionId:'spend1',bankStepId:'slice1',choice:{backdoorBoosts:1}},/já registrado/);
});
test('P191 tampering with spending choices, roll faces or pending state fails status',()=>{
 const {g}=game();bank(g);strong(g);step(g,'slice1',.89);spend(g,'spend1','slice1',{backdoorBoosts:1});
 for(const mutate of [
  w=>w.publishedAdventure.operations.receipts.spend1.choice.backdoorBoosts=10,
  w=>w.publishedAdventure.operations.receipts.slice1.evidence.roll.faces[0].index=999,
  w=>w.publishedAdventure.operations.bankSpending.pendingBoosts=9,
  w=>w.publishedAdventure.operations.receipts.spend1.bankStepId='acquire',
  w=>w.publishedAdventure.operations.receipts.spend1.minutes=15,
  w=>delete w.publishedAdventure.operations.receipts.spend1
 ]){const w=clone(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/adulterad|símbol|recibo|gasto|Backdoor|tempo|face|histórico|Tabela 2-1/i);}
});
test('P191 tampering with the 15-minute task ledger fails status',()=>{
 const {g}=game();bank(g);step(g,'slice1',.6);spend(g,'spend1','slice1',{delay15:1});
 for(const mutate of [w=>w.taskTimeApplications.task_jewel_slicing_delay_spend1.minutes=30,w=>w.clock.elapsedMinutes+=15]){
  const w=clone(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/Relógio|registro|tarefa|minutos|slicing/i);
 }
});
test('P191 no automatic credit payout, no fake bank transfer, no second spend',()=>{
 const {g}=game();bank(g);strong(g);const credits=g.state.player.character.credits;step(g,'slice1',.89);spend(g,'spend1','slice1',{backdoorBoosts:1});
 assert.equal(g.state.player.character.credits,credits);assert.equal(g.state.world.publishedAdventure.flags.bankDiversionSecured,undefined);
 assert.equal(replayJewelSlicingSpends(g.state.world.publishedAdventure.operations.receipts).pendingBoosts,1);
});
test('P191 P190 legacy receipts without BANK_SLICE_SPEND stay valid',()=>{
 const {g}=game();bank(g);step(g,'slice1',.6);assert.equal(g.state.world.publishedAdventure.operations.bankSpending,undefined);
 assert.equal(replayJewelSlicingSpends(g.state.world.publishedAdventure.operations.receipts),null);assert.equal(adapter.status(g.state.world).active,true);
});
test('P191 UI bridge exposes after-roll choices and displays only supported table outcomes',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.equal(typeof BraseiroUiBridge.prototype.spendJewelBankSymbols,'function');
 assert.match(ui,/data-jewel-spend-boost/);assert.match(ui,/data-jewel-spend-upgrade/);assert.match(ui,/data-jewel-spend-delay/);
 assert.match(ui,/bridgeResult\('spendJewelBankSymbols'/);
});
test('P191 combines a boost and an upgrade from the same post-roll choice without overspending',()=>{
 const {g}=game();bank(g);strong(g);const first=step(g,'slice1',.89);assert.ok(first.adjudication.evidence.roll.netAdvantage>=3);
 spend(g,'spend1','slice1',{backdoorBoosts:1,backdoorUpgrades:1});const next=step(g,'slice2',.89);
 assert.equal(next.adjudication.backdoor.boosts,1);assert.equal(next.adjudication.backdoor.upgrades,1);
 assert.equal(next.adjudication.evidence.pool.boost,1);assert.equal(next.adjudication.evidence.pool.ability,1);
 assert.equal(adapter.status(g.state.world).active,true);
});
test('P191 unknown spend fields, fractional counts and empty choices reject atomically',()=>{
 const {g}=game();bank(g);strong(g);step(g,'slice1',.89);
 for(const choice of [{backdoorBoosts:1,extraCredits:10000},{backdoorBoosts:1.5},{},{backdoorUpgrades:-1},{delay15:'1'}])
  invalid(g,{actionId:'bad',bankStepId:'slice1',choice},/inválid|Selecione|Custos/i);
});
test('P191 unknown receipt fields and forged extra time receipt are rejected on readback',()=>{
 const {g}=game();bank(g);step(g,'slice1',.6);spend(g,'spend1','slice1',{delay15:1});
 for(const mutate of [w=>w.publishedAdventure.operations.receipts.spend1.extraPayout=10000,
  w=>w.publishedAdventure.operations.receipts.spend1.timeCheckId='task_other',
  w=>w.publishedAdventure.operations.receipts.spend1.choice.delay15=0]){
  const w=clone(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/recibo|tempo|gasto|adulterad|custos|Selecione/i);
 }
});
test('P191 UI offline bundle includes the new Table 2-1 engine and the bridge method',()=>{
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.match(bundle,/jewelBankSlicingSpends\.mjs/);assert.match(bundle,/spendJewelBankSymbols/);
 assert.match(bundle,/BANK_SLICE_SPEND/);assert.match(bundle,/1\.0\.(?:19[1-9]|[2-9][0-9]{2})/);
});
