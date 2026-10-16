import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {createVehicleFromProfile,vehicleStatus} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {THWHEEK_ID} from '../app/src/main/assets/vtt/js/adventure/swe02ThwheekP239.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
class Repo{constructor(){this.state=createCampaign('P239');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const make=({count=1,ship=true,slow=false,nonOptimal=false,sameName=false}={})=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:count,rng:()=>.9});
 const firstName=g.state.player.character.name;
 for(let i=1;i<count;i++){const character=structuredClone(g.state.player.character);character.name=sameName?firstName:`${character.name} ${i+1}`;g.registerPartyCharacter({character});}
 if(ship)g.addVehicle('yt1300',{name:'Krayt Fang'});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});g.advancePublishedAdventure('inspect-krayt-fang');
 if(slow){g.resolvePublishedAdventureCheck(3,{rng:()=>.2});g.resolvePublishedAdventureCheck(5,{rng:()=>.2});}
 g.resolvePublishedAdventureCheck(0,{rng:()=>nonOptimal?.99:.2});
 g.advancePublishedAdventure('ship-find-bura');g.advancePublishedAdventure('ship-depart-verified');
 return {g,repo};
};
const begin=(g,options={})=>{g.advancePublishedAdventure('ship-follow-interception');g.advancePublishedAdventure('ship-start-interception',{rng:()=>.1,...options})};
const passToNpc=g=>{while(g.state.vehicles.combat.swe02Initiative.slots[g.state.vehicles.combat.swe02Initiative.index].side!=='npc')g.passSWE02VehicleTurn();};
const closeRound=g=>{const round=g.state.vehicles.combat.round;let guard=0;while(g.state.vehicles.combat.round===round&&guard++<10)g.passSWE02VehicleTurn();assert.notEqual(g.state.vehicles.combat.round,round);};
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
test('P239 SWE02 p.11 six-hour transit advances one persisted living-world tick exactly once',()=>{
 const {g,repo}=make();const p=g.state.world.publishedAdventure;
 assert.equal(p.shipSequence.transit.elapsedHours,6);assert.equal(p.shipSequence.transit.uneventful,true);
 assert.equal(p.shipSequence.transit.durationStatus,'A_LITTLE_MORE_THAN_SIX_HOURS');
 assert.equal(g.state.world.clock.tick,1);assert.equal(g.state.world.livingDepth.lastTick,1);
 const saves=repo.saves;assert.throws(()=>g.advancePublishedAdventure('ship-depart-verified'),/inválida/);
 assert.equal(repo.saves,saves);assert.equal(repo.state.world.clock.tick,1);
 const reopened=new GameController(repo);assert.equal(reopened.state.world.clock.tick,1);
 assert.equal(reopened.state.world.publishedAdventure.shipSequence.transit.elapsedHours,6);
});
test('P239 SWE02 p.8 failed route records unquantified extra transit time for GM adjudication',()=>{
 const {g,repo}=make({nonOptimal:true}),transit=g.state.world.publishedAdventure.shipSequence.transit;
 assert.equal(transit.elapsedHoursIsMinimum,true);
 assert.equal(transit.durationStatus,'LONGER_THAN_OPTIMAL_UNQUANTIFIED');
 assert.equal(transit.extraDurationStatus,'GM_TIME_ADJUDICATION_REQUIRED');
 assert.equal(g.state.world.clock.tick,1);
 const applied=g.adjudicateSWE02ShipTransitExtraDuration({ticks:2});
 assert.equal(applied.transit.extraDurationStatus,'GM_ADJUDICATED');assert.equal(applied.transit.extraDurationTicks,2);
 assert.equal(applied.clockTick,3);assert.equal(repo.state.world.livingDepth.lastTick,3);
 assert.throws(()=>g.adjudicateSWE02ShipTransitExtraDuration({ticks:2}),/não há atraso/);
 const reopened=new GameController(repo);assert.equal(reopened.state.world.clock.tick,3);
 assert.equal(reopened.state.world.publishedAdventure.shipSequence.transit.extraDurationTicks,2);
 assert.throws(()=>reopened.adjudicateSWE02ShipTransitExtraDuration({ticks:1}),/não há atraso/);
});
test('P239 SWE02 normal route rejects extra transit time adjudication',()=>{
 const {g}=make();assert.equal(g.state.world.clock.tick,1);
 assert.throws(()=>g.adjudicateSWE02ShipTransitExtraDuration({ticks:1}),/não há atraso/);
 assert.equal(g.state.world.clock.tick,1);
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
 assert.equal(g.state.vehicles.combat.swe02Initiative.slots.length,2);assert.equal(g.state.vehicles.combat.swe02Initiative.slots[0].side,'player');
 assert.throws(()=>g.resolveIncomingVehicleAttack({rng:()=>.1}),/iniciativa/);
 assert.throws(()=>g.resolveVehicleAttack({rng:()=>.1}),/fora do alcance/);
 assert.throws(()=>g.endVehicleCombat(),/resolva a interceptação/);
 assert.throws(()=>g.applyVehicleDamage({damage:100,target:'target'}),/somente por rolagem/);
 assert.throws(()=>g.beginVehicleCombat({targetProfileKey:'tie_ln'}),/SWE02/);
 const attack=shoot(g);assert.equal(attack.hit,true);assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].hullDamage,5);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.evasive,true);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,false);
});
test('P239 p.12: each PC Cool slot selects its owner and receipts its vehicle actions',()=>{
 const {g,repo}=make({count:2});begin(g);const order=g.state.vehicles.combat.swe02Initiative.slots;
 assert.deepEqual(order.map(x=>x.side),['player','player','npc']);
 const first=order[0].actorId,second=order[1].actorId;assert.equal(g.state.player.roster.activeCharacterId,first);
 g.useVehiclePilotManeuver('accelerate');g.passSWE02VehicleTurn();
 assert.equal(g.state.player.roster.activeCharacterId,second);assert.equal(g.state.vehicles.combat.actionUsed,false);
 g.useVehiclePilotManeuver('brake');
 assert.deepEqual(g.state.vehicles.combat.swe02Initiative.actionReceipts.map(x=>x.actorId),[first,second]);
 reopen(repo);
});
test('P239 p.12: PCs who did not see Thwheek emerge roll Vigilance; the others roll Cool',()=>{
 const {g,repo}=make({count:2});const ids=g.state.player.roster.entries.map(x=>x.id),unaware=[ids[1]];
 begin(g,{unawareCharacterIds:unaware});const initiative=g.state.vehicles.combat.swe02Initiative;
 assert.deepEqual(initiative.unawareCharacterIds,unaware);
 assert.equal(initiative.slots.find(x=>x.actorId===ids[0]).skill,'cool');
 assert.equal(initiative.slots.find(x=>x.actorId===ids[1]).skill,'vigilance');
 assert.equal(initiative.slots.find(x=>x.actorId==='swe02-thwheek').skill,'cool');
 assert.doesNotThrow(()=>migrateState(repo.state));
 const altered=structuredClone(repo.state);
 altered.vehicles.combat.swe02Initiative.unawareCharacterIds=[];
 assert.throws(()=>migrateState(altered),/perícia de iniciativa diverge/);
});
test('P239 p.12: unaware PC selection rejects unknown or duplicated roster IDs',()=>{
 const {g}=make();const id=g.state.player.roster.entries[0].id;g.advancePublishedAdventure('ship-follow-interception');
 assert.throws(()=>g.advancePublishedAdventure('ship-start-interception',{unawareCharacterIds:[id,id]}),/não correspondem/);
 assert.throws(()=>g.advancePublishedAdventure('ship-start-interception',{unawareCharacterIds:['forged-pc']}),/não correspondem/);
});
test('P239 p.12: initiative blocks bypassing Thwheek and manual round advancement',()=>{
 const {g}=make();begin(g);assert.throws(()=>g.nextVehicleRound(),/iniciativa/);
 g.passSWE02VehicleTurn();assert.throws(()=>g.passSWE02VehicleTurn(),/Thwheek precisa resolver/);
});
test('P239 legacy active Thwheek saves without initiative remain loadable and use their old round flow',()=>{
 const {g}=make();begin(g);const legacy=structuredClone(g.state);delete legacy.vehicles.combat.swe02Initiative;
 assert.equal(migrateState(legacy).vehicles.combat.active,true);
 const repo=new Repo();repo.state=legacy;const old=new GameController(repo);
 old.useVehiclePilotManeuver('accelerate');old.nextVehicleRound();
 assert.equal(old.state.vehicles.combat.round,2);
});
test('P239 real hit + five hull trauma: pursuit remains open, withdrawal permits Nabat',()=>{
 const {g,repo}=make();begin(g);shoot(g);
 const purs=g.resolveSWE02ThwheekCombat({pursue:true});assert.equal(purs.outcome,'PURSUIT_CONTINUES');
 assert.equal(g.state.vehicles.combat.active,true);assert.equal(g.state.world.publishedAdventure.flags.shipThwheekResolved,undefined);
 assert.throws(()=>g.resolveIncomingVehicleAttack({rng:()=>.1}),/fugindo|iniciativa/);
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
 const {g,repo}=make();begin(g);passToNpc(g);const r=g.resolveIncomingVehicleAttack({npcAgility:6,npcGunnery:5,rng:()=>.1});
 assert.equal(r.attack.check.pool.ability+r.attack.check.pool.proficiency,3);
 assert.equal(g.state.vehicles.combat.lastIncomingAttack.damage,4);
 assert.throws(()=>g.resolveIncomingVehicleAttack({rng:()=>.1}),/já atacou/);reopen(repo);
});
test('P239 p.12: target stay-on-target switches to evasive after first actual hit, persists next round',()=>{
 const {g}=make();begin(g);assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,true);
 shoot(g);passToNpc(g);closeRound(g);assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.evasive,true);
 assert.equal(g.state.vehicles.vehicles[THWHEEK_ID].roundEffects.stayOnTarget,false);
});
test('P239 p.12: shot-down Krayt Fang needs real Pilot and crew Resilience checks',()=>{
 const {g,repo}=make();begin(g);
 for(let i=0;i<6;i++){passToNpc(g);g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;closeRound(g);}
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
 for(let i=0;i<6;i++){passToNpc(g);g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;closeRound(g);}
 g.resolveSWE02ThwheekCombat();g.advancePublishedAdventure('ship-thwheek-crash');g.resolveSWE02CrashPilot({rng:()=>.1});
 const one=g.resolveSWE02CrashResilience({rng:()=>.1});assert.equal(one.checked,1);assert.equal(one.expected,3);
 assert.equal(one.completed,false);assert.throws(()=>g.advancePublishedAdventure('ship-crash-to-nabat'),/inválida/);
});
test('P239 p.12: crash Resilience is recorded once per stable PC ID, including duplicate names',()=>{
 const {g,repo}=make({count:2,sameName:true});begin(g);
 for(let i=0;i<6;i++){passToNpc(g);g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;closeRound(g);}
 g.resolveSWE02ThwheekCombat();g.advancePublishedAdventure('ship-thwheek-crash');g.resolveSWE02CrashPilot({rng:()=>.1});
 const expected=[...g.state.world.publishedAdventure.shipCrashExpectedCrewIds];
 for(const id of expected){g.selectPartyCharacter(id);g.resolveSWE02CrashResilience({rng:()=>.1});}
 const checks=g.state.world.publishedAdventure.shipCrashCrewChecks;
 assert.deepEqual(checks.map(x=>x.characterId).sort(),expected.sort());assert.equal(new Set(checks.map(x=>x.characterName)).size,1);
 assert.equal(g.state.world.publishedAdventure.flags.shipCrashCrewResolved,true);reopen(repo);
 const altered=structuredClone(repo.state);altered.world.publishedAdventure.shipCrashExpectedCrewIds[1]='other-pc';
 assert.throws(()=>migrateState(altered),/roster atual diverge/);
});
test('P239 p.12: successful Pilot soft landing upgrades one crew Resilience die',()=>{
 const {g}=make();begin(g);
 for(let i=0;i<6;i++){passToNpc(g);g.resolveIncomingVehicleAttack({rng:()=>.1});if(vehicleStatus(g.activeVehicle()).hullDisabled)break;closeRound(g);}
 g.resolveSWE02ThwheekCombat();g.advancePublishedAdventure('ship-thwheek-crash');
 const pilotFaces=[.2,.2,.3,0,0,0];let pilotIndex=0;
 const pilot=g.resolveSWE02CrashPilot({rng:()=>pilotFaces[pilotIndex++]??0});assert.equal(pilot.softLanding,true);
 const crewFaces=[.2,.2,.2,0,0,0];let crewIndex=0;
 const crew=g.resolveSWE02CrashResilience({rng:()=>crewFaces[crewIndex++]??0});
 assert.deepEqual({ability:crew.check.pool.ability,proficiency:crew.check.pool.proficiency},{ability:1,proficiency:2});
 assert.equal(crew.check.success,true);
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
 assert.equal(typeof b.passSWE02VehicleTurn,'function');
});
