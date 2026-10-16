import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {STARPORT_CLOUD_IDS,validateJewelStarportCloudCars} from '../app/src/main/assets/vtt/js/adventure/jewelStarportCloudCars.mjs';
import {STORM_IV_IDS,BESPIN_PILOT} from '../app/src/main/assets/vtt/js/adventure/jewelStormIvPatrol.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P231');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p231-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 const c=clone(g.state.player.character);c.characteristics.agilidad=7;c.skills.pilotar_vehiculo_planetario=7;g.commit('CHARACTER_UPDATED',{character:c});
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');g.addVehicle('yt1300',{id:'p231-player-ship',setActive:true});
 g.registerJewelCustody({actionId:'p231-custody',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true});
 return {g,repo};
}
const board={actionId:'p231-board',port:'PORT_TOWN',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true};
const arrival={range:'Curto',gmAirborneConfirmed:true,gmSquadronArrivedConfirmed:true,gmPositionsConfirmed:true,gmDistinctPilotsConfirmed:true};
const atomic=(g,fn)=>{const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);};
test('P231 requires real boarding and takeoff, refuses phantom arrival',()=>{
 const {g}=fixture();atomic(g,()=>g.arriveJewelStarportSquadron({actionId:'early',...arrival}));
 g.registerJewelBoarding(board);atomic(g,()=>g.arriveJewelStarportSquadron({actionId:'false-airborne',...arrival,gmAirborneConfirmed:false}));
 assert.equal(validateJewelStarportCloudCars(g.state.world.publishedAdventure),null);
});
test('P231 first squadron creates four separate official Storm IV and does not clone Venture patrol',()=>{
 const {g,repo}=fixture();g.registerJewelBoarding(board);
 const out=g.arriveJewelStarportSquadron({actionId:'arrival-1',...arrival});assert.equal(out.adjudication.wave,1);
 assert.equal(STARPORT_CLOUD_IDS.filter(id=>g.state.vehicles.vehicles[id]).length,4);
 assert.ok(STARPORT_CLOUD_IDS.every(id=>!STORM_IV_IDS.includes(id)));
 for(const id of STARPORT_CLOUD_IDS.slice(0,4)){const v=g.state.vehicles.vehicles[id];assert.equal(v.hullThreshold,5);assert.equal(v.speedMax,4);assert.deepEqual(v.wingGuardPilot,BESPIN_PILOT);}
 for(let i=0;i<3;i++)assert.equal(validateJewelStarportCloudCars(new GameController(repo).state.world.publishedAdventure,repo.state.world,repo.state.vehicles).waves.length,1);
 atomic(g,()=>g.arriveJewelStarportSquadron({actionId:'early-2',...arrival}));
});
test('P231 cannot fabricate combat outcome or overwrite pilot skills',()=>{
 const {g}=fixture();g.registerJewelBoarding(board);g.arriveJewelStarportSquadron({actionId:'arrival-1',...arrival});
 atomic(g,()=>g.beginJewelStarportCloudCombat({actionId:'bad',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:true}));
 g.beginJewelStarportCloudCombat({actionId:'engage-1',vehicleId:STARPORT_CLOUD_IDS[0],range:'Curto',gmRangeConfirmed:true});
 atomic(g,()=>g.resolveJewelStarportCloudCombat({actionId:'fake-win',outcome:'DISABLED',gmOutcomeConfirmed:true}));
 atomic(g,()=>g.resolveJewelStarportCloudCombat({actionId:'fake-evade',outcome:'EVADED',gmOutcomeConfirmed:true,gmDifficultyConfirmed:false,difficulty:1}));
 atomic(g,()=>g.resolveJewelStarportCloudIncomingAttack({npcGunnery:8}));
});
test('P231 12 distinct cars in three physical waves of four with 12 actual piloting dice rolls',()=>{
 const {g,repo}=fixture();g.registerJewelBoarding(board);const initialXp=g.state.player.character.xp.total;
 for(let wave=1;wave<=3;wave++){
  g.arriveJewelStarportSquadron({actionId:'arrival-'+wave,...arrival});
  for(let i=0;i<4;i++){
   const id=STARPORT_CLOUD_IDS[(wave-1)*4+i];
   g.beginJewelStarportCloudCombat({actionId:`engage-${wave}-${i}`,vehicleId:id,range:'Curto',gmRangeConfirmed:true});
   const r=g.resolveJewelStarportCloudCombat({actionId:`evade-${wave}-${i}`,outcome:'EVADED',gmOutcomeConfirmed:true,
     gmDifficultyConfirmed:true,difficulty:1,rng:()=>.28});
   assert.equal(r.check.success,true);assert.ok(r.check.faces.length>0);assert.equal(r.adjudication.vehicleId,id);
  }
  atomic(g,()=>g.arriveJewelStarportSquadron({actionId:'not-cleared-'+wave,...arrival}));
  g.clearJewelStarportSquadron({actionId:'clear-'+wave,gmFourResolvedConfirmed:true});
 }
 const p=g.state.world.publishedAdventure;const s=validateJewelStarportCloudCars(p,g.state.world,g.state.vehicles);
 assert.equal(s.waves.length,3);assert.equal(s.allTwelveResolved,true);
 assert.equal(new Set(s.waves.flatMap(w=>w.arrival.vehicleIds)).size,12);
 assert.equal(g.state.player.character.xp.total,initialXp);
 assert.equal(validateState(g.state),true);
 for(let i=0;i<3;i++)assert.equal(validateJewelStarportCloudCars(new GameController(repo).state.world.publishedAdventure,repo.state.world,repo.state.vehicles).allTwelveResolved,true);
 atomic(g,()=>g.arriveJewelStarportSquadron({actionId:'fourth',...arrival}));
});
test('P231 receipt tampering and vehicle tampering are rejected on load',()=>{
 const {g}=fixture();g.registerJewelBoarding(board);g.arriveJewelStarportSquadron({actionId:'arrival-1',...arrival});
 const bad=clone(g.state);bad.world.publishedAdventure.operations.receipts['arrival-1'].vehicleIds[0]='jewel-venture-storm-iv-1';
 assert.throws(()=>validateState(bad));
 const bad2=clone(g.state);bad2.vehicles.vehicles[STARPORT_CLOUD_IDS[0]].hullThreshold=99;
 assert.throws(()=>validateState(bad2));
 const bad3=clone(g.state);bad3.world.publishedAdventure.operations.receipts['arrival-1'].atMinute=999999;
 assert.throws(()=>validateState(bad3));
});
test('P231 player attack and official lone minion incoming attack use real vehicle combat',()=>{
 const {g}=fixture();g.registerJewelBoarding(board);
 g.arriveJewelStarportSquadron({actionId:'arrival-1',...arrival,range:'Bocajarro'});
 g.beginJewelStarportCloudCombat({actionId:'engage-real',vehicleId:STARPORT_CLOUD_IDS[0],range:'Bocajarro',gmRangeConfirmed:true});
 const a=g.resolveVehicleAttack({weaponIndex:0,rng:()=>.28});
 assert.ok(Array.isArray(a.check.faces)&&a.check.faces.length>0);
 assert.ok(g.state.vehicles.combat.log.some(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===STARPORT_CLOUD_IDS[0]));
 const b=g.resolveJewelStarportCloudIncomingAttack({rng:()=>.28});
 assert.ok(b.attack||b.blocked);
 assert.ok(g.state.vehicles.combat.log.some(x=>x.type==='ATAQUE_NPC_VEICULO'||x.type==='AOR_BRILLIANT_EVASION_BLOCK'||x.attackerId===STARPORT_CLOUD_IDS[0]));
 assert.equal(validateState(g.state),true);
});
test('P231 Android bridge, controller, UI and bundle wired, without Java/Kokoro change',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p231-arrival'],['r46/final-runtime.js','data-p231-resolve'],
  ['js/core/controller.mjs','arriveJewelStarportSquadron'],['js/core/uiBridge.mjs','clearJewelStarportSquadron'],
  ['js/core/uiBridge.bundle.js','jewelStarportCloudCars.mjs'],['js/core/state.mjs','validateJewelStarportCloudCars'],
  ['js/adventure/jewelOfYavin.mjs','starportCloudCars: clone']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.match(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8'),/versionCode = (?:1023[1-9]|102[4-9]\d|10[3-9]\d\d|1[1-9]\d{3,})/);
});
