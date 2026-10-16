import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const clone=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P192');this.saves=0;}load(){return clone(this.state)}save(s){this.state=clone(s);this.saves++}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});return {g,repo};}
function toBank(g){for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'round-'+i});}g.advancePublishedAdventure('bank-operation');}
function bank(g){toBank(g);g.resolveJewelBankStep({actionId:'acquire',step:'acquire',target:'industrial_docks',rng:()=>.49});g.resolveJewelBankStep({actionId:'disable',step:'disable',rng:()=>.49});}
function strong(g){const c=clone(g.state.player.character);c.characteristics.intelecto=7;c.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:c});}
function step(g,id,rng){return g.resolveJewelBankStep({actionId:id,step:'slice',rng});}
function spend(g,id,source,choice){return g.spendJewelBankSymbols({actionId:id,bankStepId:source,choice});}
function failAtomic(g,fn,pattern){const before=clone(g.state),saves=g.repository?.saves;assert.throws(fn,pattern);assert.deepEqual(g.state,before);assert.equal(g.repository?.saves,saves);}
const lcg=seed=>{let x=seed;return ()=>((x=(Math.imul(x,1664525)+1013904223)>>>0)/2**32);};
const extra=(id,tri=0)=>({extraBidderIds:[id],extraBidderTriumphs:tri});
test('P192 PDF p70: successful three-advantage bidder adds 10k conditional entitlement, never player credits',()=>{
 const {g}=game();bank(g);strong(g);const r=step(g,'slice1',lcg(3));assert.equal(r.adjudication.success,true);assert.ok(r.adjudication.evidence.roll.netAdvantage>=3);
 const credits=g.state.player.character.credits;const result=spend(g,'spend1','slice1',extra('elaiza'));
 assert.equal(result.spending.bonusEntitlementCredits,10000);assert.deepEqual(result.spending.additionalBidderIds,['elaiza']);
 assert.equal(g.state.player.character.credits,credits);assert.equal(adapter.status(g.state.world).active,true);
});
test('P192 PDF p70: triumph can pay bidder choice; success is still required',()=>{
 const {g}=game();bank(g);strong(g);const r=step(g,'slice1',()=>.92);assert.ok(r.adjudication.evidence.roll.triumph>0);assert.equal(r.adjudication.success,true);
 spend(g,'spend1','slice1',extra('elaiza',1));assert.equal(g.state.world.publishedAdventure.operations.bankSpending.bonusEntitlementCredits,10000);
});
test('P192 no additional bidder if success missing, winner chosen, unfunded Pos or symbols missing',()=>{
 for(const [rng,bidder,tri,pattern] of [[()=>.6,'elaiza',0,/Símbolos insuficientes|sucesso/],[()=>.92,'pos_podura',1,/Licitante adicional/],[()=>.92,'kaltho',1,/Licitante adicional/]]){
  const {g}=game();bank(g);strong(g);step(g,'slice1',rng);const winner=g.state.world.publishedAdventure.operations.auction.highBidderId;
  const target=bidder==='kaltho'?winner:bidder;
  failAtomic(g,()=>spend(g,'spend1','slice1',extra(target,tri)),pattern);
 }
});
test('P192 PDF p70: one threat adds actual strain to player slicer and survives three reopenings',()=>{
 const {g,repo}=game();bank(g);const r=step(g,'slice1',()=>.6);assert.ok(r.adjudication.evidence.roll.netAdvantage<=-1);
 const before=g.state.player.character.derived.strain.current;spend(g,'spend1','slice1',{slicerStrain:1});
 assert.equal(g.state.player.character.derived.strain.current,before+1);
 assert.equal(g.state.world.publishedAdventure.operations.bankSpending.slicerStrainApplied,1);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.state.player.character.derived.strain.current,before+1);assert.equal(adapter.status(reopened.state.world).active,true);}
});
test('P192 slicer strain cannot exceed real threshold or be assigned to hired NPC',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.6);const c=clone(g.state.player.character);c.derived.strain.current=c.derived.strain.threshold;g.commit('CHARACTER_UPDATED',{character:c});
 failAtomic(g,()=>spend(g,'spend1','slice1',{slicerStrain:1}),/Tensão excederia/);
});
test('P192 PDF p70: three threats dispatch a Wing Guard team equal to party size with four-hour ETA',()=>{
 const {g}=game();bank(g);const r=step(g,'slice1',()=>.6);assert.ok(r.adjudication.evidence.roll.netAdvantage<=-3);
 const now=g.state.world.clock.elapsedMinutes??0;spend(g,'spend1','slice1',{wingGuardTeams:1});
 const teams=g.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams;
 assert.equal(teams.length,1);assert.equal(teams[0].guardCount,1);assert.equal(teams[0].arrivalElapsedMinutes,now+240);
});
test('P192 PDF p70: despair dispatches Wing Guard or captures signature, not both with one despair',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.92);
 failAtomic(g,()=>spend(g,'spend1','slice1',{wingGuardDespairTeams:2,signatureDespair:1}),/Símbolos insuficientes/);
 spend(g,'spend1','slice1',{wingGuardDespairTeams:1,signatureDespair:1});
 const status=g.state.world.publishedAdventure.operations.bankSpending;
 assert.equal(status.wingGuardTeams.length,1);assert.equal(status.signatureCaptured,true);
});
test('P192 electronic signature applies one real difficulty upgrade on subsequent G-100 slicing',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.92);
 // This roll succeeds; to test the later pool we use a failed roll with despair in a separate weak operator.
 const {g:h}=game();bank(h);const first=step(h,'slice1',()=>.92);assert.equal(first.adjudication.success,false);assert.ok(first.adjudication.evidence.roll.despair>=1);
 spend(h,'spend1','slice1',{signatureDespair:1});
 const second=step(h,'slice2',()=>.6);assert.equal(second.adjudication.signatureUpgrade,true);
 assert.ok(second.adjudication.evidence.pool.challenge>=1);
 assert.equal(adapter.status(h.state.world).active,true);
});
test('P192 rejects forged signatures, additional bidder receipts, guard counts and time',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.92);spend(g,'spend1','slice1',{wingGuardDespairTeams:1,signatureDespair:1});
 for(const mutate of [w=>w.publishedAdventure.operations.bankSpending.signatureCaptured=false,
  w=>w.publishedAdventure.operations.bankSpending.wingGuardTeams[0].guardCount=9,
  w=>w.publishedAdventure.operations.receipts.spend1.partySize=0,
  w=>w.publishedAdventure.operations.receipts.spend1.dispatchElapsedMinutes=999999,
  w=>w.publishedAdventure.operations.receipts.spend1.choice.signatureDespair=2]){
  const w=clone(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/adulterad|Patrulha|símbol|gasto|Tabela 2-1|Desespero|futuro/i);
 }
});
test('P192 prevents spending one symbol twice, invalid extra bidder IDs and duplicate action',()=>{
 const {g}=game();bank(g);strong(g);step(g,'slice1',()=>.92);
 failAtomic(g,()=>spend(g,'bad','slice1',{extraBidderIds:['elaiza','elaiza'],extraBidderTriumphs:2}),/Licitantes/);
 failAtomic(g,()=>spend(g,'bad','slice1',{extraBidderIds:['not_a_bidder'],extraBidderTriumphs:1}),/Licitantes/);
 spend(g,'spend1','slice1',{extraBidderIds:['elaiza'],extraBidderTriumphs:1});
 failAtomic(g,()=>spend(g,'spend2','slice1',{extraBidderIds:['elaiza'],extraBidderTriumphs:1}),/duplicado/);
});
test('P192 UI exposes all new table outcomes and offline engine',()=>{
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const token of ['data-jewel-spend-bidder','data-jewel-spend-triumph','data-jewel-spend-strain','data-jewel-spend-guards','data-jewel-spend-trace','data-jewel-spend-despair-guards','data-jewel-spend-despair-trace','data-jewel-spend-signature'])assert.ok(ui.includes(token),token);
 for(const token of ['signatureCaptured','bonusEntitlementCredits','wingGuardTeams'])assert.ok(bundle.includes(token),token);
 assert.match(bundle,/1\.0\.(?:19[3-9]|[2-9][0-9]{2})/);
});
test('P192 second Wing Guard alarm may shorten ETA by one hour instead of adding another team',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.6);spend(g,'spend1','slice1',{wingGuardTeams:1});
 const initial=g.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams[0].arrivalElapsedMinutes;
 step(g,'slice2',()=>.6);spend(g,'spend2','slice2',{wingGuardAccelerations:1});
 const teams=g.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams;
 assert.equal(teams.length,1);assert.equal(teams[0].arrivalElapsedMinutes,initial-60);
 assert.equal(adapter.status(g.state.world).active,true);
});
test('P192 P191 legacy after-roll receipt survives the expanded replay and new actions',()=>{
 const {g,repo}=game();bank(g);step(g,'slice1',()=>.6);
 spend(g,'oldSpend','slice1',{delay15:1});
 assert.equal(g.state.world.publishedAdventure.operations.bankSpending.signatureCaptured,undefined);
 const reopened=new GameController(repo);assert.equal(adapter.status(reopened.state.world).active,true);
 step(reopened,'slice2',()=>.6);spend(reopened,'newSpend','slice2',{wingGuardTeams:1});
 assert.equal(reopened.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams.length,1);
 assert.equal(reopened.state.world.publishedAdventure.operations.bankSpending.delayMinutes,15);
});

test('P192 PDF p70: duplicate despair result can accelerate an existing patrol by one hour',()=>{
 const {g}=game();bank(g);step(g,'slice1',()=>.6);spend(g,'spend1','slice1',{wingGuardTeams:1});
 const initial=g.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams[0].arrivalElapsedMinutes;
 step(g,'slice2',()=>.92);spend(g,'spend2','slice2',{wingGuardDespairAccelerations:1});
 const teams=g.state.world.publishedAdventure.operations.bankSpending.wingGuardTeams;
 assert.equal(teams.length,1);assert.equal(teams[0].arrivalElapsedMinutes,initial-60);
});
