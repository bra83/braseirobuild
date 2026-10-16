import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {adjudicateJewelOfYavinCheck} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {validateJewelStarportRoutes} from '../app/src/main/assets/vtt/js/adventure/jewelStarportRoutes.mjs';
import {jewelUnpreparedSpaceUpgrade} from '../app/src/main/assets/vtt/js/adventure/jewelStarportSecurity.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P230');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p230-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.computadoras=7;ch.characteristics.agilidad=7;ch.skills.pilotar_vehiculo_espacial=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.commit('WORLD_UPDATED',{world:adjudicateJewelOfYavinCheck(g.state.world,{checkIndex:0,result:{success:true}}).world});
 g.advancePublishedAdventure('take-jewel');g.addVehicle('yt1300',{id:'borrowed_ship',setActive:true});
 g.registerJewelCustody({actionId:'p230-custody',gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true});
 return {g,repo};
}
const access={actionId:'p230-access',shipId:'borrowed_ship',gmShipPhysicallyAccessibleConfirmed:true,gmThirdPartyShipConfirmed:true,gmTerminalAccessConfirmed:true,rng:()=>.28};
const board={actionId:'p230-board',port:'COMMERCIAL_STARPORT',gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true,accessMode:'BORROWED_SHIP',accessReceiptId:'p230-access'};
const route={actionId:'p230-route',route:'BLAST_OUT',gmAtStarportConfirmed:true,gmRoutePhysicallyAccessibleConfirmed:true,gmSecurityPositionConfirmed:true};
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
function borrow(g){assert.equal(g.attemptJewelStarportShipAccess(access).accessGranted,true);g.registerJewelBoarding(board);}
test('P230 official route requires GM physical arrival, route, security, no phantom movement',()=>{
 const {g}=fixture(),before=clone(g.state);
 for(const k of ['gmAtStarportConfirmed','gmRoutePhysicallyAccessibleConfirmed','gmSecurityPositionConfirmed'])atomic(g,()=>g.confirmJewelStarportRoute({...route,[k]:false}));
 const r=g.confirmJewelStarportRoute(route);assert.equal(r.routes.route.route,'BLAST_OUT');
 assert.equal(g.state.world.clock.elapsedMinutes,before.world.clock.elapsedMinutes);
 assert.deepEqual(g.state.world.npcs,before.world.npcs);assert.deepEqual(g.state.vehicles,before.vehicles);
 assert.equal(validateState(g.state),true);atomic(g,()=>g.confirmJewelStarportRoute({...route,actionId:'repeat'}));
});
test('P230 non-destructive maintenance/sewer/deeper routes do not award Criminal Obligation',()=>{
 for(const routeId of ['MAINTENANCE','SEWERS','DEEPER_TO_SHIP']){
  const {g}=fixture();const n=g.groupSummary().obligationTotal;
  g.confirmJewelStarportRoute({...route,route:routeId});
  atomic(g,()=>g.registerJewelStarportDestruction({actionId:'criminal',routeReceiptId:'p230-route',combatEncounterId:'holo-park-wing-guard',gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true}));
  assert.equal(g.groupSummary().obligationTotal,n);
 }
});
test('P230 destructive escape requires real published completed encounter and explicit GM proof',()=>{
 const {g}=fixture();g.confirmJewelStarportRoute(route);
 const details={actionId:'p230-destruction',routeReceiptId:'p230-route',combatEncounterId:'holo-park-wing-guard',gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true};
 atomic(g,()=>g.registerJewelStarportDestruction(details));
 const w=clone(g.state.world);(w.publishedAdventure.encounterHistory??=[]).push({encounterId:'holo-park-wing-guard',outcome:'ended',nodeId:'cloud-city-pursuit'});
 g.commit('WORLD_UPDATED',{world:w});
 atomic(g,()=>g.registerJewelStarportDestruction({...details,gmPhysicalDestructionConfirmed:false}));
 const prior=g.groupSummary().obligationTotal;const out=g.registerJewelStarportDestruction(details);
 assert.equal(out.adjudication.obligation,10);assert.equal(g.groupSummary().obligationTotal,prior+10);
 assert.equal(g.state.group.obligationEntries.filter(x=>x.id==='jewel_starport_p230-destruction').length,1);
 atomic(g,()=>g.registerJewelStarportDestruction({...details,actionId:'repeat'}));
 assert.equal(validateState(g.state),true);
});
test('P230 receipts detect falsified route, physical proof, duplicate, chronology and encounter',()=>{
 const {g}=fixture();g.confirmJewelStarportRoute(route);
 const w=clone(g.state.world);(w.publishedAdventure.encounterHistory??=[]).push({encounterId:'holo-park-wing-guard',outcome:'ended',nodeId:'cloud-city-pursuit'});g.commit('WORLD_UPDATED',{world:w});
 g.registerJewelStarportDestruction({actionId:'destroy',routeReceiptId:'p230-route',combatEncounterId:'holo-park-wing-guard',gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true});
 for(const mutate of [s=>s.world.publishedAdventure.operations.receipts['p230-route'].route='FLY_AWAY',
  s=>s.world.publishedAdventure.operations.receipts.destroy.obligation=100,
  s=>s.world.publishedAdventure.operations.receipts.destroy.gmPhysicalDestructionConfirmed=false,
  s=>s.world.publishedAdventure.operations.receipts.destroy.atMinute=999999,
  s=>s.world.publishedAdventure.encounterHistory=[],
  s=>s.world.publishedAdventure.operations.starportRoutes.route=null]){
  const bad=clone(g.state);mutate(bad);assert.throws(()=>validateState(bad));
 }
});
test('P230 3 reopens preserve Criminal Obligation, physical route and receipts',()=>{
 const {g,repo}=fixture();g.confirmJewelStarportRoute(route);
 const w=clone(g.state.world);(w.publishedAdventure.encounterHistory??=[]).push({encounterId:'holo-park-wing-guard',outcome:'ended',nodeId:'cloud-city-pursuit'});g.commit('WORLD_UPDATED',{world:w});
 g.registerJewelStarportDestruction({actionId:'destroy',routeReceiptId:'p230-route',combatEncounterId:'holo-park-wing-guard',gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true});
 for(let i=0;i<3;i++){const x=new GameController(repo);assert.equal(validateState(x.state),true);assert.equal(validateJewelStarportRoutes(x.state.world.publishedAdventure).destruction.obligation,10);assert.equal(x.state.group.obligationEntries.find(e=>e.id==='jewel_starport_destroy').value,10);}
});
test('P230 upgrade scope: borrowed ship only, Space Piloting only, before offworld escape',()=>{
 const {g}=fixture();assert.equal(jewelUnpreparedSpaceUpgrade(g.state.world,g.state.vehicles),0);
 borrow(g);assert.equal(jewelUnpreparedSpaceUpgrade(g.state.world,g.state.vehicles),1);
 assert.equal(jewelUnpreparedSpaceUpgrade(g.state.world,g.state.vehicles,'pilotar_vehiculo_planetario'),0);
 assert.equal(jewelUnpreparedSpaceUpgrade(g.state.world,g.state.vehicles,'astronavegacion'),0);
 assert.equal(jewelUnpreparedSpaceUpgrade(g.state.world,g.state.vehicles,'pilotar_vehiculo_espacial','different_ship'),0);
});
test('P230 crew Copilot real Piloting Space roll is upgraded, Planetary roll is not',()=>{
 const {g}=fixture();borrow(g);g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const space=g.useVehicleCrewAction('copilot',{skillKey:'pilotar_vehiculo_espacial',rng:()=>.28});
 assert.equal(space.check.pool.challenge,1);assert.equal(space.check.pool.difficulty,1);
 const {g:other}=fixture();borrow(other);other.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const planetary=other.useVehicleCrewAction('copilot',{skillKey:'pilotar_vehiculo_planetario',rng:()=>.28});
 assert.equal(planetary.check.pool.challenge,0);assert.equal(planetary.check.pool.difficulty,2);
});
test('P230 tractor escape and hazard real rolls upgrade in borrowed craft',()=>{
 const {g}=fixture();borrow(g);
 const hazard=g.vehiclePilotingHazardCheck({rng:()=>.28});assert.equal(hazard.officialUnpreparedUpgrade,1);
 const v=clone(g.state.vehicles);v.vehicles.borrowed_ship.tractorLocks=[{sourceId:'real-lock',rating:2}];g.commit('VEHICLES_UPDATED',{vehicles:v});
 const escape=g.attemptTractorEscape({rng:()=>.28});assert.equal(escape.difficultyUpgrades,1);
 assert.equal(escape.check.pool.challenge,1);
});
test('P230 UI/controller/bridge/bundle/Android source integration',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [path,needle] of [['r46/final-runtime.js','data-p230-route-confirm'],['r46/final-runtime.js','data-p230-destruction'],
 ['js/core/uiBridge.mjs','registerJewelStarportDestruction'],['js/core/controller.mjs','jewelUnpreparedSpaceUpgrade'],
 ['js/core/state.mjs','validateJewelStarportRoutes'],['js/adventure/jewelOfYavin.mjs','starportRoutes'],
 ['js/core/uiBridge.bundle.js','JEWEL_STARPORT_DESTRUCTION']])
 assert.ok(readFileSync(new URL(path,root),'utf8').includes(needle),path);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10230);
});
test('P230 real Wing Guard combat ending in player incapacitation does NOT trigger Criminal Obligation',()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p230-real-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=6;ch.skills.mecanica=5;ch.characteristics.voluntad=6;ch.skills.frialdad=5;g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'p230-real-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'p230-real-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 // Preexisting Arend encounter is a test prerequisite for the published Wing Guard encounter.
 const w=clone(g.state.world);(w.publishedAdventure.encounterHistory??=[]).push({encounterId:'arend-shen-four-goons',outcome:'adversaries_defeated',nodeId:w.publishedAdventure.currentNodeId});g.commit('WORLD_UPDATED',{world:w});
 g.beginJewelHoloParkWingGuard({actionId:'p230-real-wing',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>.5});
 atomic(g,()=>g.confirmJewelStarportRoute(route));
 const ended=g.finishCombat({rng:()=>.5});assert.equal(ended.publishedAdventureOutcome.encounterId,'holo-park-wing-guard');
 assert.equal(g.state.world.publishedAdventure.encounterHistory.filter(x=>x.encounterId==='holo-park-wing-guard').length,1);
 g.confirmJewelStarportRoute(route);
 atomic(g,()=>g.registerJewelStarportDestruction({actionId:'p230-real-destruction',routeReceiptId:'p230-route',combatEncounterId:'holo-park-wing-guard',gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true}));
 assert.equal(g.state.world.publishedAdventure.encounterHistory.at(-1).outcome,'player_incapacitated');assert.equal(validateState(g.state),true);
});
test('P230 Gain the Advantage uses upgraded difficulty on real Space Piloting and not Planetary',()=>{
 function setup(){const {g}=fixture();borrow(g);g.beginVehicleCombat({targetProfileKey:'tie_ln'});
  const vehicles=clone(g.state.vehicles);vehicles.vehicles.borrowed_ship.speedMax=5;vehicles.vehicles.borrowed_ship.speed=4;
  g.commit('VEHICLES_UPDATED',{vehicles});return g;}
 const space=setup().gainVehicleAdvantage({skillKey:'pilotar_vehiculo_espacial',rng:()=>.28});
 const planet=setup().gainVehicleAdvantage({skillKey:'pilotar_vehiculo_planetario',rng:()=>.28});
 assert.equal(space.check.pool.challenge,planet.check.pool.challenge+1);
 assert.equal(space.check.pool.difficulty,planet.check.pool.difficulty-1);
});
test('P230 Full Throttle real skill action upgrades borrowed ship Space Piloting only',()=>{
 function setup(){const {g}=fixture();borrow(g);const ch=clone(g.state.player.character);ch.talents=['Aceleração máxima'];g.commit('CHARACTER_UPDATED',{character:ch});g.beginVehicleCombat({targetProfileKey:'tie_ln'});return g;}
 const space=setup().useFullThrottle({skillKey:'pilotar_vehiculo_espacial',rng:()=>.28});
 const planet=setup().useFullThrottle({skillKey:'pilotar_vehiculo_planetario',rng:()=>.28});
 assert.equal(space.check.pool.challenge,1);assert.equal(planet.check.pool.challenge,0);
 assert.equal(space.difficultyUpgrades,1);assert.equal(planet.difficultyUpgrades,0);
});
