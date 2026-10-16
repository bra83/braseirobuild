import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {replayJewelSlicerHire} from '../app/src/main/assets/vtt/js/adventure/jewelHiredSlicers.mjs';
const clone=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P190');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:6000}});return {g,repo};}
const hire=(g,id='r3_8c',price=700,extra={})=>g.hireJewelBankSlicer({actionId:'hire-'+id,slicerId:id,finalPrice:price,gmConfirmed:true,portTownContact:true,rng:()=>.49,...extra});
function toBank(g){for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'round-'+i});}g.advancePublishedAdventure('bank-operation');}
function step(g,step,id,extra={}){return g.resolveJewelBankStep({actionId:id,step,target:'industrial_docks',restrainingBolt:false,rng:()=>.49,...extra});}
test('P190 R3-8C Streetwise/Negotiation dice and 500-1000 GM price are real and durable',()=>{
 const {g}=game();const r=hire(g);assert.equal(r.hiredSlicer.slicerId,'r3_8c');assert.equal(r.hiredSlicer.intellect,3);assert.equal(r.hiredSlicer.computers,3);
 assert.equal(r.adjudication.streetwise.pool.difficulty,2);assert.equal(r.adjudication.negotiation.pool.difficulty,3);
 assert.equal(g.state.player.character.credits,5300);assert.equal(adapter.status(g.state.world).active,true);
});
test('P190 Naboka has source-backed Intellect 4/Computers 5 and minimum 2000',()=>{
 const {g}=game();const r=hire(g,'naboka',2500);assert.equal(r.hiredSlicer.intellect,4);assert.equal(r.hiredSlicer.computers,5);
 assert.equal(r.adjudication.negotiation,null);assert.equal(g.state.player.character.credits,3500);
});
test('P190 GM price outside official NPC bounds is rejected atomically',()=>{
 for(const [id,price] of [['r3_8c',499],['r3_8c',1001],['naboka',1999],['naboka',3001]]){
  const {g,repo}=game(),before=clone(g.state),saves=repo.saves;
  assert.throws(()=>hire(g,id,price),/Preço aprovado/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
 }
});
test('P190 failed Streetwise search does not pay or create hired slicer',()=>{
 const {g}=game();const r=hire(g,'r3_8c',700,{rng:()=>.99});assert.equal(r.adjudication.discovered,false);
 assert.equal(r.hiredSlicer,null);assert.equal(g.state.player.character.credits,6000);
 assert.equal(adapter.status(g.state.world).active,true);
});
test('P190 no GM confirmation or Port Town contact: atomic rejection',()=>{
 const {g,repo}=game(),before=clone(g.state),saves=repo.saves;
 assert.throws(()=>hire(g,'r3_8c',700,{gmConfirmed:false}),/contato|confirma/);
 assert.throws(()=>hire(g,'r3_8c',700,{portTownContact:false}),/contato|confirma/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P190 insufficient funds prevents a hire without committing dice/credits',()=>{
 const {g,repo}=game();g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:500}});
 const before=clone(g.state),saves=repo.saves;
 assert.throws(()=>hire(g,'naboka',2500),/insuficientes/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P190 duplicate hire or reused action ID cannot double debit',()=>{
 const {g,repo}=game();hire(g);const before=clone(g.state),saves=repo.saves;
 assert.throws(()=>hire(g),/registrada/);assert.throws(()=>hire(g,'naboka',2500,{actionId:'second'}),/outro slicer/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P190 hired slicer actually rolls bank Computers 3/3 and retains NPC identity in receipt',()=>{
 const {g}=game();hire(g);toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');
 const r=step(g,'slice','slice',{useHiredSlicer:true});assert.equal(r.adjudication.slicerId,'r3_8c');
 assert.equal(r.adjudication.evidence.actorId,'npc:r3_8c');assert.equal(r.adjudication.evidence.characteristicRank,3);
 assert.equal(r.adjudication.evidence.skillRank,3);assert.equal(adapter.status(g.state.world).active,true);
});
test('P190 Naboka uses 4/5 instead of the player stats for slicing',()=>{
 const {g}=game();hire(g,'naboka',2500);toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');
 const r=step(g,'slice','slice',{useHiredSlicer:true});assert.equal(r.adjudication.evidence.characteristicRank,4);
 assert.equal(r.adjudication.evidence.skillRank,5);assert.equal(r.adjudication.slicerId,'naboka');
});
test('P190 NPC slicing without prior hiring is rejected without mutation',()=>{
 const {g,repo}=game();toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');const before=clone(g.state),saves=repo.saves;
 assert.throws(()=>step(g,'slice','slice',{useHiredSlicer:true}),/Contrate/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P190 forged hire price, NPC stats, rolls, identity or missing hire receipt fail status',()=>{
 const {g}=game();hire(g);toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');step(g,'slice','slice',{useHiredSlicer:true});
 for(const mutate of [
  w=>w.publishedAdventure.operations.receipts['hire-r3_8c'].finalPrice=400,
  w=>w.publishedAdventure.operations.hiredSlicer.computers=7,
  w=>w.publishedAdventure.operations.receipts['hire-r3_8c'].streetwise.roll.faces[0].index=999,
  w=>w.publishedAdventure.operations.receipts.slice.slicerId='naboka',
  w=>w.publishedAdventure.operations.receipts.slice.evidence.skillRank=7,
  w=>delete w.publishedAdventure.operations.receipts['hire-r3_8c']
 ]){const w=clone(g.state.world);mutate(w);assert.throws(()=>adapter.status(w),/adulterad|slicer|contrat|recibo|face|preço|Preço|Atributos|Slicer/i);}
});
test('P190 three reopens preserve hire payment, source stats and bank receipt',()=>{
 const {g,repo}=game();hire(g);toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');step(g,'slice','slice',{useHiredSlicer:true});
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(restored.state.player.character.credits,5300);
  assert.equal(restored.state.world.publishedAdventure.operations.hiredSlicer.slicerId,'r3_8c');
  assert.equal(restored.state.world.publishedAdventure.operations.bank.history.find(x=>x.step==='slice').slicerId,'r3_8c');
  assert.equal(adapter.status(restored.state.world).active,true);}
});
test('P190 P189 legacy saves without hired slicers remain valid',()=>{
 const {g}=game();toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');step(g,'slice','slice');
 assert.equal(g.state.world.publishedAdventure.operations.hiredSlicer,undefined);
 assert.equal(adapter.status(g.state.world).active,true);
 assert.equal(replayJewelSlicerHire([]),null);
});
test('P190 no hiring after slicing started and NPC never applies to G-100 capture',()=>{
 const {g}=game();toBank(g);step(g,'acquire','acquire');step(g,'disable','disable');step(g,'slice','slice',{rng:()=>.99});
 assert.throws(()=>hire(g),/Contratação fora|após iniciar|não é possível/i);
 const {g:second}=game();hire(second);toBank(second);
 assert.throws(()=>step(second,'acquire','capture',{useHiredSlicer:true}),/somente no slicing/);
});
test('P190 Android WebView bridge and PT-BR panel expose hiring and explicit NPC slicing choice',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const {readFileSync}=await import('node:fs');
 const panel=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.equal(typeof BraseiroUiBridge.prototype.hireJewelBankSlicer,'function');
 assert.match(panel,/data-jewel-hire/);assert.match(panel,/data-jewel-use-slicer/);
 assert.match(panel,/Contato em Port Town confirmado/);assert.match(panel,/bridgeResult\('hireJewelBankSlicer'/);
 assert.match(bundle,/jewelHiredSlicers\.mjs/);assert.match(bundle,/hireJewelBankSlicer/);
});
test('P190 receipt cannot pretend an R3 negotiation took place after failed Streetwise discovery',()=>{
 const {g}=game();hire(g,'r3_8c',700,{rng:()=>.99});const w=clone(g.state.world);
 const r=w.publishedAdventure.operations.receipts['hire-r3_8c'];r.negotiated=true;
 assert.throws(()=>adapter.status(w),/Negociação sem contato válido/);
});
