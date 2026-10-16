import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {JEWEL_PHYSICAL_ITEM_ID,validateJewelBespinDeparture} from '../app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P228');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p228-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 const c=clone(g.state.player.character);c.characteristics.intelecto=7;c.skills.astronavegacion=7;g.commit('CHARACTER_UPDATED',{character:c});
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');
 const w=clone(g.state.world);w.locations['offworld-destination']={id:'offworld-destination',name:'Porto independente',description:'Destino real',knownToPlayer:true,tags:[]};g.commit('WORLD_UPDATED',{world:w});
 g.addVehicle('yt1300',{id:'player_ship',setActive:true});const companion=g.addCompanion({name:'Aliado real'});
 g.registerJewelCustody({actionId:'p228-custody',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true});
 g.transferJewelCustody({actionId:'p228-transfer',toHolderId:companion.id,gmCoLocatedConfirmed:true,gmHandOffConfirmed:true});
 return {g,repo,companion};
}
const board={actionId:'p228-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true};
const escape={actionId:'p228-escape',destinationLocationId:'offworld-destination',gmDepartureConfirmed:true,gmNavigationConfirmed:true,routeType:'MAJOR',rng:()=>.28};
function confirmed(companion){return {...board,carrierCompanionId:companion.id,gmCompanionPresentConfirmed:true,gmCompanionBoardedConfirmed:true};}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P228 actual companion carries sole Jewel aboard existing ship; PJ also confirmed boarded',()=>{
 const {g,companion}=fixture();const out=g.registerJewelBoarding(confirmed(companion));
 assert.equal(out.departure.board.holderId,companion.id);assert.equal(out.departure.board.carrierCompanionId,companion.id);
 assert.equal(g.state.player.character.gear.filter(x=>x.id===JEWEL_PHYSICAL_ITEM_ID).length,0);
 assert.equal(g.state.group.companions[0].character.gear.filter(x=>x.id===JEWEL_PHYSICAL_ITEM_ID).length,1);
 assert.equal(validateState(g.state),true);
});
test('P228 refuses unconfirmed companion presence, individual boarding, wrong carrier or phantom',()=>{
 const {g,companion}=fixture();
 for(const patch of [{gmCompanionPresentConfirmed:false},{gmCompanionBoardedConfirmed:false},{carrierCompanionId:'ghost'},
  {carrierCompanionId:'player-slot'},{gmBoardedConfirmed:false},{gmSecurityResolvedConfirmed:false}])
  atomic(g,()=>g.registerJewelBoarding({...confirmed(companion),...patch}));
});
test('P228 real hyperjump succeeds with confirmed companion aboard, no duplicate Jewel or premature XP',()=>{
 const {g,companion}=fixture();g.registerJewelBoarding(confirmed(companion));
 assert.equal(g.state.player.character.xp.total,0);
 const result=g.executeJewelBespinEscape(escape);assert.equal(result.escaped,true);
 const r=validateJewelBespinDeparture(g.state.world.publishedAdventure,g.state.world,g.state.vehicles,g.state.player.character,g.state.group);
 assert.equal(r.escape.holderId,companion.id);assert.equal(r.escape.jumpProof.vehicleId,'player_ship');
 assert.equal(g.state.player.character.xp.total,0);
 assert.equal(validateState(g.state),true);
});
test('P228 before escape, cannot delete the companion or the physical Jewel without detection',()=>{
 const {g,companion}=fixture();g.registerJewelBoarding(confirmed(companion));
 atomic(g,()=>g.removeCompanion(companion.id));
 const tampered=clone(g.state);tampered.group.companions[0].character.gear=[];assert.throws(()=>validateState(tampered));
});
test('P228 tampered receipt companion confirmation, ID or inventory is rejected',()=>{
 const {g,companion}=fixture();g.registerJewelBoarding(confirmed(companion));
 for(const mutate of [s=>s.world.publishedAdventure.operations.receipts['p228-board'].carrierCompanionId='ghost',
  s=>s.world.publishedAdventure.operations.receipts['p228-board'].gmCompanionBoardedConfirmed=false,
  s=>s.world.publishedAdventure.operations.receipts['p228-board'].gmCompanionPresentConfirmed=false,
  s=>s.group.companions[0].character.gear.push({id:JEWEL_PHYSICAL_ITEM_ID,quantity:1}),
  s=>s.world.publishedAdventure.operations.bespinDeparture.board.holderId='player-slot']){
  const bad=clone(g.state);mutate(bad);assert.throws(()=>validateState(bad));
 }
});
test('P228 three persisted reopens preserve companion board and real inventories',()=>{
 const {g,repo,companion}=fixture();g.registerJewelBoarding(confirmed(companion));
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
  assert.equal(reopened.state.world.publishedAdventure.operations.bespinDeparture.board.carrierCompanionId,companion.id);}
});
test('P228 legacy PJ-only boarding remains valid; companion flags cannot be faked',()=>{
 const {g,companion}=fixture();g.transferJewelCustody({actionId:'p228-return',toHolderId:'player-slot',gmCoLocatedConfirmed:true,gmHandOffConfirmed:true});
 atomic(g,()=>g.registerJewelBoarding({...board,gmCompanionBoardedConfirmed:true}));
 g.registerJewelBoarding(board);assert.equal(validateState(g.state),true);
 assert.equal(g.state.world.publishedAdventure.operations.bespinDeparture.board.carrierCompanionId,undefined);
});
test('P228 UI, controller, bundle, version and receipt fields are wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p228-companion-boarded'],['r46/final-runtime.js','gmCompanionPresentConfirmed'],
  ['js/core/controller.mjs','details,this.state.group'],['js/core/uiBridge.bundle.js','gmCompanionBoardedConfirmed'],
  ['js/adventure/jewelBespinDeparture.mjs','verifyBoardCarrier']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10228);
});
