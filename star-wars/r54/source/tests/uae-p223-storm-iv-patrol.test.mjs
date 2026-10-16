import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {STORM_IV_IDS,BESPIN_PILOT,validateJewelStormIvPatrol} from '../app/src/main/assets/vtt/js/adventure/jewelStormIvPatrol.mjs';
import {SWE09_ADVENTURE_VEHICLE_PROFILES,vehicleStatus} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P223');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p223-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.mecanica=7;ch.characteristics.voluntad=7;ch.skills.frialdad=7;ch.skills.computadoras=7;ch.characteristics.agilidad=6;ch.skills.pilotar_vehiculo_planetario=5;g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'p223-bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'p223-bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const w=clone(g.state.world);w.publishedAdventure.encounterHistory=[{encounterId:'arend-shen-four-goons',outcome:'escaped'}];g.commit('WORLD_UPDATED',{world:w});
 g.investigateJewelArendHoldings({actionId:'p223-lead',skill:'computadoras',gmMainframeAccessConfirmed:true,rng:()=>.28});
 for(let i=0;i<4;i++){const h=['plaza-residence','figg-hall-office','port-town-building','plaza-yacht'][i];const result=g.visitJewelArendHolding({actionId:'p223-visit-'+i,holdingId:h,gmSearchConfirmed:true,rng:()=>h==='plaza-yacht'?0:0.999});if(result.investigation.foundAt)break;}
 assert.equal(g.state.world.publishedAdventure.operations.arendRevenge.foundAt,'plaza-yacht');
 g.beginJewelArendPropertyEncounter({actionId:'p223-security',holdingId:'plaza-yacht',stage:'security',range:'Médio',gmPhysicalPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmAccessConfirmed:true,rng:()=>.5});
 g.recordJewelArendPatrol({actionId:'p223-alert',type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:true,gmTransponderActivatedConfirmed:true});
 g.recordJewelArendPatrol({actionId:'p223-arrival',type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:true,gmFourDistinctVehiclesConfirmed:true});
 // Do not pretend the yacht's personal security combat has ended: this fixture explicitly closes it.
 g.finishCombat({rng:()=>.5});
 g.addVehicle('yt1300',{id:'player_ship',setActive:true});
 return {g,repo};
}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
const deploy={actionId:'p223-deploy',range:'Curto',gmPositionsConfirmed:true,gmPilotsPresentConfirmed:true};
test('P223 source-backed PDF Storm IV and Bespin pilot are exact, without invented solo minion skill',()=>{
 const p=SWE09_ADVENTURE_VEHICLE_PROFILES.storm_iv_cloud_car;
 assert.deepEqual([p.silhouette,p.speedMax,p.handling,p.armor,p.hullThreshold,p.systemStrainThreshold],[2,4,1,2,5,7]);
 assert.deepEqual([p.defense.proa,p.defense.popa,p.weapons[0].damage,p.weapons[0].crit,p.weapons[0].qualities.linked],[0,0,4,4,1]);
 assert.deepEqual([BESPIN_PILOT.brawn,BESPIN_PILOT.agility,BESPIN_PILOT.intellect,BESPIN_PILOT.cunning,BESPIN_PILOT.willpower,BESPIN_PILOT.presence,BESPIN_PILOT.soak,BESPIN_PILOT.woundThreshold],[2,3,2,2,2,2,2,12]);
 assert.equal(BESPIN_PILOT.soloSkillRank,0);
});
test('P223 cannot deploy before arrival or without a real player craft; no duplicate vehicles',()=>{
 const {g}=fixture();const w=clone(g.state.world);delete w.publishedAdventure.operations.receipts['p223-arrival'];delete w.publishedAdventure.operations.arendPatrols.venture.arrival;
 atomic(g,()=>g.deployJewelStormIvPatrol({...deploy,gmPositionsConfirmed:false}));
 g.deployJewelStormIvPatrol(deploy);
 atomic(g,()=>g.deployJewelStormIvPatrol({...deploy,actionId:'duplicate'}));
 assert.equal(STORM_IV_IDS.filter(id=>g.state.vehicles.vehicles[id]).length,4);
 assert.equal(g.state.vehicles.activeVehicleId,'player_ship');
});
test('P223 deploys four physically distinct combat vehicles, keeps exact crew and survives 3 reopenings',()=>{
 const {g,repo}=fixture();g.deployJewelStormIvPatrol(deploy);
 const state=validateJewelStormIvPatrol(g.state.world.publishedAdventure,g.state.vehicles);
 assert.deepEqual(state.deploy.vehicleIds,STORM_IV_IDS);
 for(const id of STORM_IV_IDS){const v=g.state.vehicles.vehicles[id];assert.equal(v.hullThreshold,5);assert.deepEqual(v.wingGuardPilot,BESPIN_PILOT);}
 for(let i=0;i<3;i++)assert.equal(validateJewelStormIvPatrol(new GameController(repo).state.world.publishedAdventure,repo.state.vehicles).deploy.vehicleIds.length,4);
 atomic(g,()=>g.beginJewelStormIvCombat({actionId:'x',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:false}));
});
test('P223 real vehicle combat targets exactly one of four; no premature defeat or XP',()=>{
 const {g}=fixture();g.deployJewelStormIvPatrol(deploy);
 g.beginJewelStormIvCombat({actionId:'engage-1',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:true});
 assert.equal(g.state.vehicles.combat.active,true);assert.equal(g.state.vehicles.combat.targetVehicleId,STORM_IV_IDS[0]);
 atomic(g,()=>g.beginJewelStormIvCombat({actionId:'engage-2',vehicleId:STORM_IV_IDS[1],range:'Curto',gmRangeConfirmed:true}));
 atomic(g,()=>g.resolveJewelStormIvCombat({actionId:'fake-win',outcome:'DISABLED',gmOutcomeConfirmed:true}));
 atomic(g,()=>g.resolveJewelStormIvCombat({actionId:'fake-escape',outcome:'EVADED',gmOutcomeConfirmed:true,gmDifficultyConfirmed:false,difficulty:1}));
 assert.equal(g.state.world.publishedAdventure.operations.stormIvPatrol.outcomes.length,0);
});
test('P223 real successful piloting evasion consumes one physical car, validates dice faces, no replay',()=>{
 const {g,repo}=fixture();g.deployJewelStormIvPatrol(deploy);
 g.beginJewelStormIvCombat({actionId:'engage-1',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:true});
 const result=g.resolveJewelStormIvCombat({actionId:'evade-1',outcome:'EVADED',gmOutcomeConfirmed:true,gmDifficultyConfirmed:true,difficulty:1,rng:()=>.28});
 assert.equal(result.check.success,true);assert.equal(result.adjudication.outcome,'EVADED');
 assert.equal(g.state.vehicles.combat.active,false);
 for(let i=0;i<3;i++)assert.equal(validateJewelStormIvPatrol(new GameController(repo).state.world.publishedAdventure,repo.state.vehicles).outcomes.length,1);
 atomic(g,()=>g.beginJewelStormIvCombat({actionId:'retry',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:true}));
 for(const edit of [p=>p.operations.receipts['evade-1'].check.faces[0].index=99,p=>p.operations.stormIvPatrol.outcomes[0].outcome='DISABLED']){
  const p=clone(g.state.world.publishedAdventure);edit(p);assert.throws(()=>validateJewelStormIvPatrol(p,g.state.vehicles));
 }
});
test('P223 defeat requires real combat damage log AND hull damage greater than threshold',()=>{
 const {g}=fixture();g.deployJewelStormIvPatrol(deploy);g.beginJewelStormIvCombat({actionId:'engage-1',vehicleId:STORM_IV_IDS[0],range:'Curto',gmRangeConfirmed:true});
 const v=clone(g.state.vehicles);v.vehicles[STORM_IV_IDS[0]].hullDamage=6;g.commit('VEHICLES_UPDATED',{vehicles:v});
 atomic(g,()=>g.resolveJewelStormIvCombat({actionId:'defeat',outcome:'DISABLED',gmOutcomeConfirmed:true}));
 // Simulate an already-resolved real attack log (without granting fake XP).
 v.combat.log.push({type:'ATAQUE_VEICULO',targetId:STORM_IV_IDS[0],hit:true,damage:6,damageType:'hull'});g.commit('VEHICLES_UPDATED',{vehicles:v});
 const out=g.resolveJewelStormIvCombat({actionId:'defeat',outcome:'DISABLED',gmOutcomeConfirmed:true});
 assert.equal(out.adjudication.hullDamage,6);assert.equal(vehicleStatus(g.state.vehicles.vehicles[STORM_IV_IDS[0]]).hullDisabled,true);
});
test('P223 attack on a physical Storm IV uses the existing vehicle dice/damage runtime',()=>{
 const {g}=fixture();g.deployJewelStormIvPatrol({...deploy,range:'Bocajarro'});
 g.beginJewelStormIvCombat({actionId:'combat-real',vehicleId:STORM_IV_IDS[0],range:'Bocajarro',gmRangeConfirmed:true});
 const before=g.state.vehicles.vehicles[STORM_IV_IDS[0]].hullDamage;
 const attack=g.resolveVehicleAttack({weaponIndex:0,rng:()=>.28});
 assert.equal(attack.targetVehicleId??g.state.vehicles.combat.targetVehicleId,STORM_IV_IDS[0]);
 assert.ok(Array.isArray(attack.check.faces)&&attack.check.faces.length>0);
 assert.ok(g.state.vehicles.combat.log.some(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===STORM_IV_IDS[0]));
 assert.ok(g.state.vehicles.vehicles[STORM_IV_IDS[0]].hullDamage>=before);
});
test('P223 full patrol only clears after all four distinct vehicle outcomes',()=>{
 const {g,repo}=fixture();g.deployJewelStormIvPatrol(deploy);
 atomic(g,()=>g.clearJewelStormIvPatrol({actionId:'early',gmAllFourResolvedConfirmed:true}));
 for(let i=0;i<4;i++){
  g.beginJewelStormIvCombat({actionId:'engage-'+i,vehicleId:STORM_IV_IDS[i],range:'Curto',gmRangeConfirmed:true});
  g.resolveJewelStormIvCombat({actionId:'evade-'+i,outcome:'EVADED',gmOutcomeConfirmed:true,gmDifficultyConfirmed:true,difficulty:1,rng:()=>.28});
 }
 const r=g.clearJewelStormIvPatrol({actionId:'clear',gmAllFourResolvedConfirmed:true});
 assert.equal(r.adjudication.outcomeReceiptIds.length,4);
 const xpBefore=g.state.player.character.xp.total;
 const granted=g.grantJewelEpisode3Reward({actionId:'p223-cloud-xp',milestone:'CLOUD_CAR_PATROLS',gmAwardConfirmed:true});
 assert.equal(granted.adjudication.xp,5);assert.equal(g.state.player.character.xp.total,xpBefore+5);
 atomic(g,()=>g.grantJewelEpisode3Reward({actionId:'again-xp',milestone:'CLOUD_CAR_PATROLS',gmAwardConfirmed:true}));
 for(let i=0;i<3;i++)assert.ok(validateJewelStormIvPatrol(new GameController(repo).state.world.publishedAdventure,repo.state.vehicles).cleared);
 atomic(g,()=>g.clearJewelStormIvPatrol({actionId:'again',gmAllFourResolvedConfirmed:true}));
});
test('P223 bridge, UI, state, source, version and offline bundle are wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p223-deploy'],['r46/final-runtime.js','data-p223-resolve'],
  ['js/core/controller.mjs','beginJewelStormIvCombat'],['js/core/uiBridge.mjs','resolveJewelStormIvCombat'],
  ['js/core/uiBridge.bundle.js','jewelStormIvPatrol.mjs'],['js/core/state.mjs','validateJewelStormIvPatrol'],
  ['js/adventure/jewelOfYavin.mjs','stormIvPatrol: clone(validateJewelStormIvPatrol(p))']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10223);
});
