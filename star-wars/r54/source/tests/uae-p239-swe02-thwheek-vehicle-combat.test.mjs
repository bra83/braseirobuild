import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {createVehicleFromProfile,vehicleStatus} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {THWHEEK_ID} from '../app/src/main/assets/vtt/js/adventure/swe02ThwheekP239.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
class Repo{constructor(){this.state=createCampaign('P239');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const make=({count=1,ship=true,slow=false,nonOptimal=false}={})=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:count,rng:()=>.9});
 if(ship)g.addVehicle('yt1300',{name:'Krayt Fang'});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 if(slow){g.resolvePublishedAdventureCheck(3,{rng:()=>.2});g.resolvePublishedAdventureCheck(5,{rng:()=>.2});}
 g.resolvePublishedAdventureCheck(0,{rng:()=>nonOptimal?.99:.2});
 g.advancePublishedAdventure('ship-find-bura');g.advancePublishedAdventure('ship-depart-verified');
 return {g,repo};
};
const begin=g=>{g.advancePublishedAdventure('ship-follow-interception');g.advancePublishedAdventure('ship-start-interception')};
const shoot=g=>{g.useVehiclePilotManeuver('accelerate');g.useVehiclePilotManeuver('advance',{method:'strain',direction:'closer'});return g.resolveVehicleAttack({rng:()=>.1})};
const reopen=(repo,n=3)=>{for(let i=0;i<n;i++){const h=new GameController(repo);a.status(h.state.world);assert.equal(h.state.vehicles.vehicles[THWHEEK_ID]?.hullThreshold,8);}};
test('P239 SWE02 p.12: official Dunelizard profile and linked laser cannons',()=>{
 const v=createVehicleFromProfile('swe02_thwheek_dunelizard');assert.equal(v.silhouette,3);assert.equal(v.speedMax,4);
 assert.equal(v.handling,1);assert.equal(v.armor,3);assert.equal(v.hullThreshold,8);assert.equal(v.systemStrainThreshold,7);
 assert.equal(v.weapons[0].damage,6);assert.equal(v.weapons[0].qualities.linked,1);assert.equal(v.source.printedPage,12);
});
test('P239 optimal navigation + slowed transponder: direct Nabat, no fighter',()=>{
 const {g}=make({slow:true,nonOptimal:false});const p=g.state.world.publishedAdventure;
 assert.equal(p.currentNodeId,'rylos-arrival-decision');assert.equal(p.flags.shipThwheekInterceptionRequired,false);
 assert.throws(()=>g.advancePublishedAdventure('ship-follow-interception'),/inválida/);
 g.advancePublishedAdventure('ship-safe-landing');assert.equal(g.state.world.publishedAdventure.currentNodeId,'nabat-arrival');
});
test('P239 failed route forces interception even after slowed tracker',()=>{
 const {g}=make({slow:true,nonOptimal:true});assert.equal(g.state.world.publishedAdventure.flags.shipThwheekInterceptionRequired,true);
 begin(g);assert.equal(g.state.vehicles.combat.targetVehicleId,THWHEEK_ID);
});
test('P239 p.12: cannot start Thwheek without actual Krayt Fang',()=>{
 const {g,repo}=make({ship:false});g.advancePublishedAdventure('ship-follow-interception');const before=repo.saves;
 assert.throws(()=>g.advancePublishedAdventure('ship-start-interception'),/Krayt Fang/);
 assert.equal(repo.saves,before);assert.equal(g.state.world.publishedAdventure.currentNodeId,'thwheek-orbit');
});
test('P239 real vehicle engine: Short-range fighter and player must maneuver into Bocajarro',()=>{
 const {g}=make();begin(g);assert.equal(g.state.vehicles.combat.range,'Curto');
 assert.throws(()=>g.resolveVehicleAttack({rng:()=>.1}),/fora do alcance/);
 assert.throws(()=>g.endVehicleCombat(),/resolva a interceptação/);
 assert.throws(()=>g.applyVehicleDamage({damage:100,target:'target'}),/somente por rolagem/);
 assert.throws(()=>g.beginVehicleCombat({targetProfileKey:'tie_ln'}),/SWE02/);
 const attack=shoot(g);assert.equal(attack.hit,true);assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].hullDamage,5);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.evasive,true);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,false);
});
test('P239 real hit + five hull trauma: pursuit remains open, withdrawal permits Nabat',()=>{
 const {g,repo}=make();begin(g);shoot(g);
 const purs=g.resolveSWE02ThwheekCombat({pursue:true});assert.equal(purs.outcome,'PURSUIT_CONTINUES');
 assert.equal(g.state.vehicles.combat.active,true);assert.equal(g.state.world.publishedAdventure.flags.shipThwheekResolved,undefined);
 assert.throws(()=>g.resolveIncomingVehicleAttack({rng:()=>.1}),/fugindo/);
 const ended=g.resolveSWE02ThwheekCombat();assert.equal(ended.outcome,'THWHEEK_WITHDREW');
 assert.equal(g.state.vehicles.combat.active,false);assert.equal(g.state.world.publishedAdventure.shipThwheekReceipt.outgoingHits,1);
 g.advancePublishedAdventure('ship-thwheek-to-nabat');assert.equal(g.state.world.publishedAdventure.currentNodeId,'nabat-arrival');reopen(repo);
});
test('P239 p.12: no victory without actual hits and no duplicate start/resolve',()=>{
 const {g,repo}=make();begin(g);const before=repo.saves;
 assert.throws(()=>g.resolveSWE02ThwheekCombat(),/comprovad/);
 assert.equal(repo.saves,before);assert.throws(()=>g.beginSWE02ThwheekCombat(),/não confirmada|já iniciada/);
 assert.throws(()=>g.advancePublishedAdventure('ship-thwheek-to-nabat'),/inválida/);
});
test('P239 p.12: incoming attack uses fixed Agility 3, Gunnery 0 even with forged GM overrides',()=>{
 const {g,repo}=make();begin(g);const r=g.resolveIncomingVehicleAttack({npcAgility:6,npcGunnery:5,rng:()=>.1});
 assert.equal(r.attack.check.pool.ability+r.attack.check.pool.proficiency,3);
 assert.equal(g.state.vehicles.combat.lastIncomingAttack.damage,4);
 assert.throws(()=>g.resolveIncomingVehicleAttack({rng:()=>.1}),/já atacou/);reopen(repo);
});
test('P239 p.12: target stay-on-target switches to evasive after first actual hit, persists next round',()=>{
 const {g}=make();begin(g);assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,true);
 shoot(g);g.nextVehicleRound();assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.evasive,true);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,false);
});
test('P239 p.12: shot-down Krayt Fang needs real Pilot and crew Resilience checks',()=>{
 const {g,repo}=make();begin(g);
 for(let i=0;i<6;i++){g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;g.nextVehicleRound();}
 assert.equal(g.activeVehicle().hullDamage,24);const r=g.resolveSWE02ThwheekCombat();assert.equal(r.outcome,'KRAYT_FANG_SHOT_DOWN');
 assert.throws(()=>g.advancePublishedAdventure('ship-thwheek-to-nabat'),/inválida/);
 g.advancePublishedAdventure('ship-thwheek-crash');assert.throws(()=>g.advancePublishedAdventure('ship-crash-to-nabat'),/inválida/);
 const pilot=g.resolveSWE02CrashPilot({rng:()=>.1});assert.equal(pilot.softLanding,false);
 const crew=g.resolveSWE02CrashResilience({rng:()=>.1});assert.equal(crew.completed,true);
 assert.throws(()=>g.resolveSWE02CrashResilience({rng:()=>.1}),/falta Pilotagem|indisponível/);
 g.advancePublishedAdventure('ship-crash-to-nabat');assert.equal(g.state.world.publishedAdventure.currentNodeId,'nabat-arrival');reopen(repo);
});
test('P239 p.12: three-person crew cannot be falsely certified by one PC',()=>{
 const {g}=make({count:3});begin(g);
 for(let i=0;i<6;i++){g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;g.nextVehicleRound();}
 g.resolveSWE02ThwheekCombat();g.advancePublishedAdventure('ship-thwheek-crash');g.resolveSWE02CrashPilot({rng:()=>.1});
 const one=g.resolveSWE02CrashResilience({rng:()=>.1});assert.equal(one.checked,1);assert.equal(one.expected,3);
 assert.equal(one.completed,false);assert.throws(()=>g.advancePublishedAdventure('ship-crash-to-nabat'),/inválida/);
});
test('P239 fail-closed: forged outcome or crew completion cannot survive adapter status',()=>{
 const {g}=make();begin(g);shoot(g);g.resolveSWE02ThwheekCombat();
 const w=structuredClone(g.state.world);w.publishedAdventure.flags.shipThwheekLandedSafely=false;
 assert.throws(()=>a.status(w),/pouso seguro/);
 const x=structuredClone(g.state.world);x.publishedAdventure.shipThwheekReceipt.outgoingHits=0;
 assert.throws(()=>a.status(x),/sem prova/);
 const y=structuredClone(g.state.world);y.publishedAdventure.flags.shipCrashCrewResolved=true;
 assert.throws(()=>a.status(y),/tripulação/);
});
test('P239 bridge exposes authoritative vehicle result and crash methods',()=>{
 const b=new BraseiroUiBridge({repository:new Repo()});
 assert.equal(typeof b.resolveSWE02ThwheekCombat,'function');
 assert.equal(typeof b.activateSWE02ThwheekLinked,'function');
 assert.equal(typeof b.resolveSWE02CrashPilot,'function');
 assert.equal(typeof b.resolveSWE02CrashResilience,'function');
});
