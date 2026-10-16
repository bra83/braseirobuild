import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {replayJewelCustodyTransfers,validateJewelCustodyTransfers} from '../app/src/main/assets/vtt/js/adventure/jewelCustodyTransfers.mjs';
import {JEWEL_PHYSICAL_ITEM_ID} from '../app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P227');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p227-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');g.addVehicle('yt1300',{id:'player_ship',setActive:true});
 const member=g.addCompanion({name:'Aliado real'});
 g.registerJewelCustody({actionId:'p227-vault',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true});
 return {g,repo,member};
}
const confirmed={gmCoLocatedConfirmed:true,gmHandOffConfirmed:true};
function count(c){return (c?.gear??[]).filter(x=>x.id===JEWEL_PHYSICAL_ITEM_ID).length;}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P227 transfer PC → companion changes TWO real inventories exactly once',()=>{
 const {g,member}=fixture();const out=g.transferJewelCustody({actionId:'p227-1',toHolderId:member.id,...confirmed});
 assert.equal(count(g.state.player.character),0);assert.equal(count(g.state.group.companions[0].character),1);
 assert.equal(out.custody.holderId,member.id);assert.equal(out.adjudication.fromHolderId,'player-slot');
 assert.equal(replayJewelCustodyTransfers(g.state.world.publishedAdventure).transfers.length,1);
 assert.equal(validateState(g.state),true);
});
test('P227 companion → PC return restores boarding compatibility and does not award XP',()=>{
 const {g,member}=fixture();g.transferJewelCustody({actionId:'p227-1',toHolderId:member.id,...confirmed});
 atomic(g,()=>g.registerJewelBoarding({actionId:'blocked',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true}));
 g.transferJewelCustody({actionId:'p227-2',toHolderId:'player-slot',...confirmed});
 assert.equal(count(g.state.player.character),1);assert.equal(count(g.state.group.companions[0].character),0);
 g.registerJewelBoarding({actionId:'p227-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true});
 assert.equal(g.state.world.publishedAdventure.operations.bespinDeparture.board.holderId,'player-slot');
 assert.equal(g.state.player.character.xp.total,0);
});
test('P227 no duplicate, phantom recipient, unconfirmed handoff or transfer after boarding',()=>{
 const {g,member}=fixture();atomic(g,()=>g.transferJewelCustody({actionId:'bad',toHolderId:member.id,gmCoLocatedConfirmed:false,gmHandOffConfirmed:true}));
 atomic(g,()=>g.transferJewelCustody({actionId:'bad',toHolderId:'ghost',...confirmed}));
 atomic(g,()=>g.transferJewelCustody({actionId:'bad',toHolderId:'player-slot',...confirmed}));
 g.transferJewelCustody({actionId:'p227-1',toHolderId:member.id,...confirmed});
 atomic(g,()=>g.transferJewelCustody({actionId:'p227-1',toHolderId:'player-slot',...confirmed}));
 atomic(g,()=>g.removeCompanion(member.id));
});
test('P227 three persisted reopens preserve holder, receipts and unique physical item',()=>{
 const {g,repo,member}=fixture();g.transferJewelCustody({actionId:'p227-1',toHolderId:member.id,...confirmed});
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);
  assert.equal(count(reopened.state.group.companions[0].character),1);
  assert.equal(replayJewelCustodyTransfers(reopened.state.world.publishedAdventure).holderId,member.id);}
});
test('P227 replay rejects altered transfer, derived holder, missing or duplicated inventory',()=>{
 const {g,member}=fixture();g.transferJewelCustody({actionId:'p227-1',toHolderId:member.id,...confirmed});
 for(const mutate of [s=>s.world.publishedAdventure.operations.receipts['p227-1'].fromHolderId='fake',
  s=>s.world.publishedAdventure.operations.receipts['p227-1'].gmHandOffConfirmed=false,
  s=>s.world.publishedAdventure.operations.jewelCustodyTransfers.holderId='fake',
  s=>s.group.companions[0].character.gear=[],
  s=>s.player.character.gear.push({id:JEWEL_PHYSICAL_ITEM_ID,quantity:1}),
  s=>s.world.publishedAdventure.operations.receipts['p227-1'].atTick=999999]){
  const bad=clone(g.state);mutate(bad);assert.throws(()=>validateState(bad));
 }
});
test('P227 source, bridge, Android UI, bundle and version are wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p227-jewel-transfer'],['js/core/controller.mjs','transferJewelCustody(details'],
 ['js/core/uiBridge.mjs','JEWEL_CUSTODY_TRANSFER_FAILED'],['js/core/uiBridge.bundle.js','jewelCustodyTransfers.mjs'],
 ['js/core/state.mjs','next.player?.character,next.group'],['js/adventure/jewelOfYavinOperations.mjs','JEWEL_CUSTODY_TRANSFER']])
 assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10227);
});
test('P227 old P224 custody remains valid without transfers or derived ledger',()=>{
 const {g}=fixture();const p=g.state.world.publishedAdventure;
 assert.equal(p.operations.jewelCustodyTransfers,undefined);
 assert.deepEqual(replayJewelCustodyTransfers(p),{holderId:'player-slot',transfers:[]});
 assert.equal(validateJewelCustodyTransfers(p,g.state.world,g.state.player.character,g.state.group).holderId,'player-slot');
});
test('P227 companion → companion passes physical item without creating a copy',()=>{
 const {g,member}=fixture(),other=g.addCompanion({name:'Segundo aliado'});
 g.transferJewelCustody({actionId:'p227-first',toHolderId:member.id,...confirmed});
 g.transferJewelCustody({actionId:'p227-second',toHolderId:other.id,...confirmed});
 assert.equal(count(g.state.player.character),0);
 assert.equal(count(g.state.group.companions[0].character),0);
 assert.equal(count(g.state.group.companions[1].character),1);
 assert.equal(validateState(g.state),true);
 assert.equal(replayJewelCustodyTransfers(g.state.world.publishedAdventure).transfers.length,2);
});
test('P227 after boarding, a new transfer is rejected with atomic rollback',()=>{
 const {g,member}=fixture();g.registerJewelBoarding({actionId:'p227-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true});
 atomic(g,()=>g.transferJewelCustody({actionId:'p227-late',toHolderId:member.id,...confirmed}));
});
