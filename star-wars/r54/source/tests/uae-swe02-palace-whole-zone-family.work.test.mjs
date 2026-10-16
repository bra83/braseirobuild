import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {SWE02_SITE_OPERATIONS,listSWE02PalaceOperations,executeSWE02PalaceOperation,validateSWE02PalaceOperations} from '../app/src/main/assets/vtt/js/adventure/swe02PalaceOperations.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const step=(w,id)=>adapter.advance(w,id).world;
const character={id:'zone-scout',name:'Escoteiro',credits:50,gear:[]};
function atReception(){
 let w=adapter.start({}).world;
 for(const id of ['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return','approach-fortified-gate'])w=step(w,id);
 w=adapter.adjudicateCheck(w,{checkIndex:0,result:{success:true,netAdvantage:0}}).world;
 return step(w,'palace-gate-debt-bluff');
}
class Repo{constructor(){this.s=createCampaign('SWE02 whole palace zone family')}load(){return structuredClone(this.s)}save(v){this.s=structuredClone(v)}}
const list=w=>listSWE02PalaceOperations(w).map(o=>o.id);
test('Family authoring: 14 added noncombat source-backed operations across 9 palace scenes',()=>{
 const added=SWE02_SITE_OPERATIONS.filter(o=>o.id!=='mos-electrogate-disable');
 assert.equal(added.length,14);
 assert.equal(new Set(added.map(o=>o.id)).size,14);
 assert.ok(added.every(o=>[42,43,44,45].includes(o.page)));
 assert.ok(added.every(o=>o.mode==='OBSERVATION'||o.mode==='CARGO'));
 assert.ok(added.every(o=>Object.hasOwn(adapter.status(atReception()).node,'choices')));
});
test('Full reconnaissance route: reception, hallway, gladiators, workshop, kitchen, bar, pad, spy, comms',()=>{
 let w=atReception(),c=structuredClone(character);
 const runs=[
  {ops:['reception-tables-cover','reception-refreshers'],go:'palace-enter-hallway'},
  {ops:['hallway-forgeries','hallway-teemo-sculpture','hallway-painting-cargo'],go:'palace-hallway-return'},
  {ops:[],go:'palace-enter-gladiators'},
  {ops:['gladiator-cells-layout'],go:'palace-gladiators-return'},
  {ops:[],go:'palace-enter-workshop'},
  {ops:['workshop-baktoid-frame'],go:'palace-workshop-return'},
  {ops:[],go:'palace-enter-kitchen'},
  {ops:['kitchen-galley'],go:'palace-kitchen-return'},
  {ops:[],go:'palace-enter-bar'},
  {ops:['bar-volatile-containers'],go:'palace-bar-return'},
  {ops:[],go:'palace-enter-pad'},
  {ops:['landing-cannon-silhouette'],go:'palace-pad-return'},
  {ops:[],go:'palace-enter-spy'},
  {ops:['spy-player-characters'],go:'palace-spy-return'},
  {ops:[],go:'palace-enter-comms'},
  {ops:['comms-network-security'],go:'palace-comms-to-reception'},
 ];
 for(const stage of runs){
  for(const id of stage.ops){
   assert.ok(list(w).includes(id),id);
   const r=executeSWE02PalaceOperation(w,c,{operationId:id});w=r.world;c=r.character;
   assert.equal(r.receipt.usable,false);
   assert.equal(validateSWE02PalaceOperations(w),true);
   assert.ok(!list(w).includes(id));
  }
  w=step(w,stage.go);
 }
 const receipts=w.publishedAdventure.palaceOperationReceipts;
 assert.equal(receipts.length,12);
 assert.equal(c.credits,50);
 assert.equal(c.gear.length,1);
 assert.equal(c.gear[0].id,'swe02_hallway_painting');
 assert.equal(c.gear[0].saleCompleted,false);
 assert.equal(c.gear[0].marketEstimateCredits,450);
 assert.equal(c.gear[0].usableInCombat,false);
 assert.ok(!w.publishedAdventure.flags.b1ReverseEngineeringEvidence);
 assert.ok(!w.publishedAdventure.flags.ryllFinancialEvidence);
 assert.ok(!w.publishedAdventure.flags.palaceBithWitnessEvidence);
 for(let i=0;i<3;i++){
  w=JSON.parse(JSON.stringify(w));c=JSON.parse(JSON.stringify(c));
  assert.equal(validateSWE02PalaceOperations(w),true);
 }
 assert.throws(()=>executeSWE02PalaceOperation(w,c,{operationId:'hallway-painting-cargo'}),/indisponível/);
});
test('Cannot loot hallway artwork from another room, mint sale cash, or impersonate proof',()=>{
 const reception=atReception();
 assert.throws(()=>executeSWE02PalaceOperation(reception,character,{operationId:'hallway-painting-cargo'}),/indisponível/);
 const hallway=step(reception,'palace-enter-hallway');
 const a=executeSWE02PalaceOperation(hallway,character,{operationId:'hallway-painting-cargo'});
 assert.equal(a.character.credits,50);
 assert.equal(a.receipt.marketEstimateCredits,450);
 const forged=structuredClone(a.world);
 forged.publishedAdventure.palaceOperationReceipts[0].saleCompleted=true;
 assert.throws(()=>validateSWE02PalaceOperations(forged),/adulterad/);
 const alsoForged=structuredClone(a.world);
 alsoForged.publishedAdventure.palaceOperationReceipts[0].marketEstimateCredits=9000;
 assert.throws(()=>validateSWE02PalaceOperations(alsoForged),/adulterad/);
});
test('Atomic controller + 3 reopenings: hallway cargo, rollback on unavailable operation',()=>{
 const repo=new Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});
 const w=step(atReception(),'palace-enter-hallway');
 g.commit('WORLD_UPDATED',{world:w});
 const before=JSON.stringify(g.state);
 assert.throws(()=>g.executeSWE02PalaceOperation({operationId:'landing-cannon-silhouette'}),/indisponível/);
 assert.equal(JSON.stringify(g.state),before);
 const receipt=g.executeSWE02PalaceOperation({operationId:'hallway-painting-cargo'});
 assert.equal(receipt.saleCompleted,false);
 assert.equal(g.state.player.character.gear.some(x=>x.id==='swe02_hallway_painting'&&x.marketEstimateCredits===450),true);
 assert.equal(g.state.player.character.credits,repo.load().player.character.credits);
 for(let i=0;i<3;i++){
  const reopen=new GameController(repo);
  assert.equal(validateSWE02PalaceOperations(reopen.state.world),true);
  assert.equal(reopen.state.player.character.gear.filter(x=>x.id==='swe02_hallway_painting').length,1);
 }
 assert.throws(()=>g.executeSWE02PalaceOperation({operationId:'hallway-painting-cargo'}),/indisponível/);
});
