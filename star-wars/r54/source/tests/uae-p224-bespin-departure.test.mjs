import test from 'node:test';
import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {JEWEL_PHYSICAL_ITEM_ID,validateJewelBespinDeparture} from '../app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs';
import {JEWEL_EP3_REWARDS} from '../app/src/main/assets/vtt/js/adventure/jewelEpisode3Rewards.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P224');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p224-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.astronavegacion=7;ch.characteristics.agilidad=7;ch.skills.falcatrua=7;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');
 const w=clone(g.state.world);w.locations['offworld-destination']={id:'offworld-destination',name:'Porto independente no Borda Exterior',description:'Destino físico conhecido',knownToPlayer:true,tags:[]};
 g.commit('WORLD_UPDATED',{world:w});g.addVehicle('yt1300',{id:'player_ship',setActive:true});
 return {g,repo};
}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
const custody={actionId:'p224-custody',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true};
const board={actionId:'p224-board',port:'COMMERCIAL_STARPORT',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true};
const escape={actionId:'p224-escape',destinationLocationId:'offworld-destination',gmDepartureConfirmed:true,gmNavigationConfirmed:true,routeType:'MAJOR',rng:()=>.28};
test('P224 does not grant Bespin escape XP from a story flag, ship selection or an epilogue alone',()=>{
 const {g}=fixture();assert.equal(JEWEL_EP3_REWARDS.BESPIN_ESCAPE.xp,5);
 atomic(g,()=>g.grantJewelEpisode3Reward({actionId:'xp-early',milestone:'BESPIN_ESCAPE',gmAwardConfirmed:true}));
 atomic(g,()=>g.registerJewelBoarding(board));
 atomic(g,()=>g.registerJewelCustody({...custody,gmPhysicalJewelConfirmed:false}));
});
test('P224 physical custody adds ONE real item and blocks duplicate, boarding needs security and real ship',()=>{
 const {g}=fixture();g.registerJewelCustody(custody);
 assert.equal(g.state.player.character.gear.filter(x=>x.id===JEWEL_PHYSICAL_ITEM_ID).length,1);
 atomic(g,()=>g.registerJewelCustody({...custody,actionId:'again'}));
 atomic(g,()=>g.registerJewelBoarding({...board,gmSecurityResolvedConfirmed:false}));
 g.registerJewelBoarding(board);
 assert.equal(validateJewelBespinDeparture(g.state.world.publishedAdventure).board.shipId,'player_ship');
 atomic(g,()=>g.registerJewelBoarding({...board,actionId:'again-board'}));
});
test('P224 actual successful astrogation advances persistent world clock and unlocks exactly 5 XP',()=>{
 const {g,repo}=fixture();g.registerJewelCustody(custody);g.registerJewelBoarding(board);
 const tick=g.state.world.clock.tick;
 const result=g.executeJewelBespinEscape(escape);
 assert.equal(result.escaped,true);assert.ok(g.state.world.clock.tick>tick);
 const proof=validateJewelBespinDeparture(g.state.world.publishedAdventure,g.state.world,g.state.vehicles,g.state.player.character).escape;
 assert.equal(proof.jumpProof.arrived,true);assert.equal(proof.jumpProof.vehicleId,'player_ship');
 assert.equal(proof.jumpProof.destinationLocationId,'offworld-destination');
 const before=g.state.player.character.xp.total;
 g.grantJewelEpisode3Reward({actionId:'p224-xp',milestone:'BESPIN_ESCAPE',gmAwardConfirmed:true});
 assert.equal(g.state.player.character.xp.total,before+5);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.state.player.character.xp.total,before+5);
  assert.equal(validateJewelBespinDeparture(reopened.state.world.publishedAdventure,reopened.state.world,reopened.state.vehicles,reopened.state.player.character).escape.id,'p224-escape');}
 atomic(g,()=>g.grantJewelEpisode3Reward({actionId:'p224-xp2',milestone:'BESPIN_ESCAPE',gmAwardConfirmed:true}));
 atomic(g,()=>g.executeJewelBespinEscape({...escape,actionId:'again'}));
});
test('P224 no exit without offworld destination and GM launch approval, atomic rollback',()=>{
 const {g}=fixture();g.registerJewelCustody(custody);g.registerJewelBoarding(board);
 atomic(g,()=>g.executeJewelBespinEscape({...escape,destinationLocationId:'joy.loc.escape_platform'}));
 atomic(g,()=>g.executeJewelBespinEscape({...escape,gmNavigationConfirmed:false}));
 atomic(g,()=>g.executeJewelBespinEscape({...escape,gmDepartureConfirmed:false}));
});
test('P224 rejects tampered receipts, item custody, invented jump faces, time and derived state',()=>{
 const {g}=fixture();g.registerJewelCustody(custody);g.registerJewelBoarding(board);g.executeJewelBespinEscape(escape);
 const base=g.state.world.publishedAdventure;
 for(const mutate of [p=>p.operations.receipts['p224-escape'].jumpProof.check.faces[0].index=999,
 p=>p.operations.receipts['p224-escape'].atTick=0,p=>p.operations.receipts['p224-escape'].destinationLocationId='joy.loc.escape_platform',
 p=>p.operations.bespinDeparture.escape.shipId='fake',p=>delete p.operations.receipts['p224-custody']]){
  const p=clone(base);mutate(p);assert.throws(()=>validateJewelBespinDeparture(p));
 }
 const c=clone(g.state.player.character);c.gear=[]; // once escaped, later custody changes may occur; prior escape receipt remains valid
 assert.doesNotThrow(()=>validateJewelBespinDeparture(base,g.state.world,g.state.vehicles,c));
});
test('P224 failed astrogation never creates escape proof or grants XP, but keeps the actual failed roll',()=>{
 const {g}=fixture();g.registerJewelCustody(custody);g.registerJewelBoarding(board);
 const result=g.executeJewelBespinEscape({...escape,rng:()=>0});
 assert.equal(result.escaped,false);
 assert.equal(validateJewelBespinDeparture(g.state.world.publishedAdventure).escape,null);
 assert.equal(g.state.vehicles.travel.lastHyperspaceJump.outcome.jumpSuccessful,false);
 atomic(g,()=>g.grantJewelEpisode3Reward({actionId:'xp-failed',milestone:'BESPIN_ESCAPE',gmAwardConfirmed:true}));
});
test('P224 Android UI, bridge, controller, bundle, save validator and source are wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p224-escape'],['r46/final-runtime.js','data-p224-custody'],
 ['js/core/controller.mjs','executeJewelBespinEscape'],['js/core/uiBridge.mjs','registerJewelBoarding'],
 ['js/core/uiBridge.bundle.js','jewelBespinDeparture.mjs'],['js/core/state.mjs','validateJewelBespinDeparture'],
 ['js/adventure/jewelOfYavin.mjs','bespinDeparture: clone(validateJewelBespinDeparture(p,world))']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10224);
});
test('P224 epilogue cannot claim a completed Bespin escape before the verified hyperjump',()=>{
 const {g}=fixture();
 assert.equal(g.publishedAdventureStatus('jewel-of-yavin').node.choices.some(x=>x.id==='reach-hangar'),false);
 atomic(g,()=>g.advancePublishedAdventure('reach-hangar'));
 g.registerJewelCustody(custody);g.registerJewelBoarding(board);g.executeJewelBespinEscape(escape);
 assert.equal(g.publishedAdventureStatus('jewel-of-yavin').node.choices.some(x=>x.id==='reach-hangar'),true);
 const end=g.advancePublishedAdventure('reach-hangar');assert.equal(end.adventure?.completed??g.state.world.publishedAdventure.status==='COMPLETED',true);
});
