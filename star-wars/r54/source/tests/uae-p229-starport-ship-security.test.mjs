import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {validateJewelStarportSecurity} from '../app/src/main/assets/vtt/js/adventure/jewelStarportSecurity.mjs';
import {validateJewelBespinDeparture} from '../app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs';
const copy=structuredClone;
class Repo{constructor(){this.state=createCampaign('P229');this.saves=0;}load(){return copy(this.state);}save(s){this.state=copy(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p229-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 const ch=copy(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:ch});
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');g.addVehicle('yt1300',{id:'borrowed_ship',setActive:true});
 g.registerJewelCustody({actionId:'p229-custody',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true});
 return {g,repo};
}
const request={actionId:'p229-access',shipId:'borrowed_ship',gmShipPhysicallyAccessibleConfirmed:true,gmThirdPartyShipConfirmed:true,gmTerminalAccessConfirmed:true,rng:()=>.28};
const board={actionId:'p229-board',port:'COMMERCIAL_STARPORT',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true,accessMode:'BORROWED_SHIP',accessReceiptId:'p229-access'};
function atomic(g,fn){const old=copy(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,old);assert.equal(g.repository.saves,saves);}
test('P229 real Hard Computers 3 difficulty dice, physical third-party ship, actual roll faces',()=>{
 const {g}=fixture();const out=g.attemptJewelStarportShipAccess(request);
 assert.equal(out.accessGranted,true);assert.equal(out.adjudication.pool.difficulty,3);assert.equal(out.adjudication.pool.challenge,0);
 assert.equal(out.adjudication.roll.faces.filter(x=>x.type==='difficulty').length,3);
 assert.equal(out.adjudication.pilotingSpaceDifficultyUpgrade,1);
 assert.equal(validateJewelStarportSecurity(g.state.world.publishedAdventure).length,1);assert.equal(validateState(g.state),true);
});
test('P229 refuses phantom ship, missing confirmations, active combat, duplicate receipt',()=>{
 const {g}=fixture();for(const patch of [{shipId:'ghost'},{gmShipPhysicallyAccessibleConfirmed:false},
 {gmThirdPartyShipConfirmed:false},{gmTerminalAccessConfirmed:false},{actionId:'__proto__'}])
 atomic(g,()=>g.attemptJewelStarportShipAccess({...request,...patch}));
 g.attemptJewelStarportShipAccess(request);atomic(g,()=>g.attemptJewelStarportShipAccess({...request,actionId:'new-attempt'}));
});
test('P229 failure blocks boarding; GM-authorized real retry succeeds without faking faces',()=>{
 const {g}=fixture();const fail=g.attemptJewelStarportShipAccess({...request,rng:()=>.01});assert.equal(fail.accessGranted,false);
 atomic(g,()=>g.registerJewelBoarding(board));
 atomic(g,()=>g.attemptJewelStarportShipAccess({...request,actionId:'retry'}));
 const retry=g.attemptJewelStarportShipAccess({...request,actionId:'retry',gmRetryAuthorized:true});assert.equal(retry.accessGranted,true);
 atomic(g,()=>g.registerJewelBoarding(board));
 const out=g.registerJewelBoarding({...board,accessReceiptId:'retry'});assert.equal(out.departure.board.accessReceiptId,'retry');
});
test('P229 real access receipt is mandatory for third-party boarding, but not own ship legacy',()=>{
 const {g}=fixture();atomic(g,()=>g.registerJewelBoarding(board));
 g.attemptJewelStarportShipAccess(request);atomic(g,()=>g.registerJewelBoarding({...board,accessReceiptId:'ghost'}));
 const out=g.registerJewelBoarding(board);assert.equal(out.departure.board.accessMode,'BORROWED_SHIP');
 assert.equal(validateJewelBespinDeparture(g.state.world.publishedAdventure).board.accessReceiptId,'p229-access');
 const own=fixture().g;own.registerJewelBoarding({actionId:'own-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true});
 assert.equal(validateState(own.state),true);
});
test('P229 tampering with difficulty, face, result, proof ID, time, duplicate access fails',()=>{
 const {g}=fixture();g.attemptJewelStarportShipAccess(request);g.registerJewelBoarding(board);
 for(const mutate of [s=>s.world.publishedAdventure.operations.receipts['p229-access'].pool.difficulty=2,
  s=>s.world.publishedAdventure.operations.receipts['p229-access'].roll.faces[0].index=7,
  s=>s.world.publishedAdventure.operations.receipts['p229-access'].success=false,
  s=>s.world.publishedAdventure.operations.receipts['p229-board'].accessReceiptId='forged',
  s=>s.world.publishedAdventure.operations.starportSecurity=[],
  s=>s.world.publishedAdventure.operations.receipts['p229-access'].atMinute=99999]){
  const bad=copy(g.state);mutate(bad);assert.throws(()=>validateState(bad));
 }
});
test('P229 three reopens preserve the physical ship access and boarding proof',()=>{
 const {g,repo}=fixture();g.attemptJewelStarportShipAccess(request);g.registerJewelBoarding(board);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
  assert.equal(reopened.state.world.publishedAdventure.operations.starportSecurity[0].shipId,'borrowed_ship');
  assert.equal(reopened.state.world.publishedAdventure.operations.bespinDeparture.board.accessReceiptId,'p229-access');}
});
test('P229 published one-difficulty upgrade applies to real Space Piloting hazard check while fleeing in borrowed ship',()=>{
 const {g}=fixture();g.attemptJewelStarportShipAccess(request);g.registerJewelBoarding(board);
 const borrowed=g.vehiclePilotingHazardCheck({rng:()=>.28});
 assert.equal(borrowed.officialUnpreparedUpgrade,1);
 const own=fixture().g;own.registerJewelBoarding({actionId:'own-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true});
 const normal=own.vehiclePilotingHazardCheck({rng:()=>.28});
 assert.equal(normal.officialUnpreparedUpgrade,0);
 assert.equal(borrowed.upgrades,normal.upgrades+1);
 assert.equal(borrowed.check.pool.challenge,normal.check.pool.challenge+1);
});
test('P229 no invented XP, vehicles, pilot checks, guards or physical transit',()=>{
 const {g}=fixture();const before=copy(g.state);g.attemptJewelStarportShipAccess(request);
 assert.deepEqual(g.state.vehicles,before.vehicles);assert.equal(g.state.player.character.xp.total,before.player.character.xp.total);
 assert.deepEqual(g.state.world.publishedAdventure.encounterHistory,before.world.publishedAdventure.encounterHistory);
 assert.equal(g.state.world.clock.elapsedMinutes,before.world.clock.elapsedMinutes);
});
test('P229 UI, bridge, bundle, source, Android version and save validator connected',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p229-ship-access'],['r46/final-runtime.js','BORROWED_SHIP'],
 ['js/core/uiBridge.mjs','attemptJewelStarportShipAccess'],['js/core/uiBridge.bundle.js','JEWEL_STARPORT_SHIP_ACCESS'],
 ['js/core/state.mjs','validateJewelStarportSecurity'],['js/adventure/jewelBespinDeparture.mjs','successfulJewelStarportAccess']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10229);
});
