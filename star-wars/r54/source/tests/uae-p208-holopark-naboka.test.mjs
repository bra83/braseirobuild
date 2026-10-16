import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {enterJewelHoloPark,recordJewelHoloParkRound,confirmJewelHoloParkOutcome,validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
const clone=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P208');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:6000}});return {g,repo};}
function bankGame(){const {g,repo}=game();g.hireJewelBankSlicer({actionId:'hire-naboka',slicerId:'naboka',finalPrice:2500,gmConfirmed:true,portTownContact:true,rng:()=>.49});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});}
 g.advancePublishedAdventure('bank-operation');
 for(const step of ['acquire','disable'])g.resolveJewelBankStep({actionId:step,step,target:'industrial_docks',restrainingBolt:false,rng:()=>.49});return {g,repo};}
const slice=(g,id,useHiredSlicer,rng)=>g.resolveJewelBankStep({actionId:id,step:'slice',useHiredSlicer,rng});
function holoFixture(){const {g,repo}=game();for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const world=clone(g.state.world);world.publishedAdventure.operations.escapeBomb={status:'DISARMED'};const combat={active:true,round:1,encounter:{actedActorIds:[]}};return {g,repo,world,combat};}
const enter={actionId:'holo-enter',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,factions:['wing_guard','arend_goons'],rng:()=>0};
const tick=(round,range='Médio',rng=()=>.4)=>({actionId:'holo-'+round,range,gmDistanceConfirmed:true,gmPursuersNeutralizedConfirmed:false,rng});
function atomic(g,fn,pattern){const s=clone(g.state),n=g.repository.saves;assert.throws(fn,pattern);assert.deepEqual(g.state,s);assert.equal(g.repository.saves,n);}
test('P208 Naboka refuses any additional hired NPC slice after a first real failure; player can retry',()=>{
 const {g}=bankGame();const first=slice(g,'slice1',true,()=>.99);assert.equal(first.adjudication.success,false);
 const availability=adapter.status(g.state.world).slicerAvailability;assert.equal(availability.refused,true);assert.equal(availability.canUseHiredSlicer,false);assert.equal(availability.playerMayRetry,true);
 atomic(g,()=>slice(g,'slice2',true,()=>.49),/Naboka recusou/);
 const second=slice(g,'slice2',false,()=>.49);assert.equal(second.adjudication.slicerId,undefined);assert.equal(second.bank.attempts,2);
 assert.equal(adapter.status(g.state.world).bankStatus.stage,'FINISHED');
});
test('P208 slicer policy receipt and anchor cannot be forged independently',()=>{
 const {g}=bankGame();slice(g,'slice1',true,()=>.99);
 for(const mutation of [p=>delete p.bankSlicerPolicy,p=>delete p.operations.receipts.slice1.slicerPolicyVersion,p=>p.bankSlicerPolicy.fromActionId='fake']){
  const w=clone(g.state.world);mutation(w.publishedAdventure);assert.throws(()=>adapter.status(w),/política|Política|recibos/i);
 }
});
test('P208 old P207 receipt format without slicer policy still replays',()=>{
 const {g}=bankGame();slice(g,'slice1',true,()=>.99);const w=clone(g.state.world),p=w.publishedAdventure;
 delete p.bankSlicerPolicy;delete p.operations.receipts.slice1.slicerPolicyVersion;delete p.operations.bank.history.find(x=>x.id==='slice1').slicerPolicyVersion;
 assert.equal(adapter.status(w).bankStatus.attempts,1);
});
test('P208 Naboka refusal persists across three real repository reopenings',()=>{
 const {g,repo}=bankGame();slice(g,'slice1',true,()=>.99);for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(adapter.status(reopened.state.world).slicerAvailability.refused,true);atomic(reopened,()=>slice(reopened,'slice2',true,()=>.49),/Naboka recusou/);}
});
test('P208 Holo-Park cannot begin without real combat, physical arrival and confirmed factions',()=>{
 const {world,combat}=holoFixture();assert.throws(()=>enterJewelHoloPark(world,{active:false,round:1},enter),/rodada de combate/);
 assert.throws(()=>enterJewelHoloPark(world,combat,{...enter,gmParkArrivalConfirmed:false}),/chegada física/);
 assert.throws(()=>enterJewelHoloPark(world,combat,{...enter,factions:['imaginary']}),/Facções/);
 const first=enterJewelHoloPark(world,combat,enter);assert.equal(first.holoPark.landscape,'alderaan');assert.equal(first.holoPark.roundCount,0);
});
test('P208 Table 3-1 d100 official ranges and printed overlap at 47 requires GM',()=>{
 for(const [n,name] of [[1,'alderaan'],[23,'alderaan'],[24,'christophsis'],[46,'christophsis'],[48,'coruscant'],[71,'coruscant'],[72,'kashyyyk'],[95,'kashyyyk'],[96,'projectors_off'],[100,'projectors_off']]){
  const {world,combat}=holoFixture();const r=enterJewelHoloPark(world,combat,{...enter,rng:()=>((n-.5)/100)});assert.equal(r.holoPark.landscape,name);
 }
 const {world,combat}=holoFixture();assert.throws(()=>enterJewelHoloPark(world,combat,{...enter,rng:()=>.465}),/sobrepõe/);
 const choice=enterJewelHoloPark(world,combat,{...enter,rng:()=>.465,gm47Choice:'coruscant'});assert.equal(choice.holoPark.landscape,'coruscant');
});
test('P208 Holo-Park changes scene only every two ACTUAL combat rounds',()=>{
 let {world,combat}=holoFixture();world=enterJewelHoloPark(world,combat,enter).world;
 for(let round=1;round<=4;round++){
  combat.round=round;const r=recordJewelHoloParkRound(world,combat,tick(round));world=r.world;
  assert.equal(r.adjudication.roll100,round===3?41:null);
  assert.equal(r.holoPark.landscape,round<3?'alderaan':'christophsis');
  assert.equal(r.holoPark.roundCount,round);
 }
 assert.throws(()=>recordJewelHoloParkRound(world,{...combat,round:4},tick(5)),/rodada real/);
});
test('P208 Long range or dispersed pursuers requires a separate GM outcome receipt',()=>{
 let {world,combat}=holoFixture();world=enterJewelHoloPark(world,combat,enter).world;
 const r=recordJewelHoloParkRound(world,combat,tick(1,'Longo'));world=r.world;
 assert.equal(r.holoPark.status,'ESCAPE_PENDING_GM');assert.throws(()=>recordJewelHoloParkRound(world,{...combat,round:2},tick(2)),/desfecho pendente/);
 assert.throws(()=>confirmJewelHoloParkOutcome(world,{actionId:'end',gmOutcomeConfirmed:false}),/não autorizado/);
 const end=confirmJewelHoloParkOutcome(world,{actionId:'end',gmOutcomeConfirmed:true});assert.equal(end.holoPark.status,'ESCAPED');
 assert.throws(()=>confirmJewelHoloParkOutcome(end.world,{actionId:'end2',gmOutcomeConfirmed:true}),/não autorizado/);
});
test('P208 tenth round ends chase but NEVER fabricates escape or XP',()=>{
 let {world,combat}=holoFixture();world=enterJewelHoloPark(world,combat,enter).world;
 for(let i=1;i<=10;i++){combat.round=i;world=recordJewelHoloParkRound(world,combat,tick(i)).world;}
 assert.equal(world.publishedAdventure.operations.holoPark.status,'ROUND_LIMIT_PENDING_GM');
 assert.throws(()=>confirmJewelHoloParkOutcome(world,{actionId:'end',gmOutcomeConfirmed:true}),/Mestre deve confirmar/);
 const end=confirmJewelHoloParkOutcome(world,{actionId:'end',gmOutcomeConfirmed:true,outcome:'STILL_PURSUED'});
 assert.equal(end.holoPark.status,'STILL_PURSUED');assert.equal(end.world.publishedAdventure.flags?.escapedBespin,undefined);
});
test('P208 Holo-Park replay rejects forged landscapes, rounds, range, GM confirmations and endings',()=>{
 let {world,combat}=holoFixture();world=enterJewelHoloPark(world,combat,enter).world;world=recordJewelHoloParkRound(world,combat,tick(1)).world;
 for(const fn of [p=>p.operations.receipts['holo-enter'].roll100=90,p=>p.operations.receipts['holo-1'].round=7,p=>p.operations.receipts['holo-1'].range='Longo',p=>p.operations.receipts['holo-1'].gmDistanceConfirmed=false,p=>p.operations.holoPark.landscape='coruscant']){
  const forged=clone(world);fn(forged.publishedAdventure);assert.throws(()=>validateJewelHoloPark(forged.publishedAdventure),/adulterad|diverge|rodadas/);
 }
});
test('P208 Holo-Park controller atomic saves, bridge, PT-BR controls, and three reopenings',()=>{
 const {g,repo,world,combat}=holoFixture();g.commit('WORLD_UPDATED',{world});g.commit('COMBAT_UPDATED',{combat});
 const first=g.enterJewelHoloPark(enter);assert.equal(first.holoPark.landscape,'alderaan');
 const round=g.recordJewelHoloParkRound(tick(1));assert.equal(round.holoPark.roundCount,1);
 atomic(g,()=>g.recordJewelHoloParkRound(tick(1)),/repetido/);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateJewelHoloPark(reopened.state.world.publishedAdventure).roundCount,1);}
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,token] of [['r46/final-runtime.js','data-holo-park-round'],['r46/final-runtime.js','Naboka recusou'],['js/core/uiBridge.mjs','JEWEL_HOLO_END_FAILED'],['js/core/uiBridge.bundle.js','jewelHoloPark.mjs'],['js/adventure/jewelOfYavin.mjs','validateJewelHoloPark']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(token),file);
});
