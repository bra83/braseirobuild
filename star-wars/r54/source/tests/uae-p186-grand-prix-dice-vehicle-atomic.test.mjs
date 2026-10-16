import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validatePublishedAdventureRuntimeState,publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validateJewelGrandPrixPilotEvidence} from '../app/src/main/assets/vtt/js/adventure/jewelGrandPrixDice.mjs';
const copy=structuredClone;
const adapter=getPublishedAdventureAdapter('jewel-of-yavin');
const route=['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start'];
class Repo{constructor(){this.state=createCampaign('P186');this.saves=0;}load(){return copy(this.state);}save(state){this.state=copy(state);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});for(const choice of route)g.advancePublishedAdventure(choice);g.addVehicle('x34');const v=copy(g.state.vehicles);v.vehicles[v.activeVehicleId].speed=2;g.commit('VEHICLES_UPDATED',{vehicles:v});return {g,repo};}
function validate(g){const w=g.state.world;assert.equal(adapter.status(w).active,true);assert.equal(validatePublishedAdventureRuntimeState(publishedAdventureRuntimeModule(adapter.meta),w.publishedAdventureRuntime,w.publishedAdventure),true);}
const leg=(g,i,options={})=>g.resolveJewelGrandPrixLeg({actionId:`roll-${i}`,rng:()=>.1,...options});

test('P186 real narrative dice are rolled with real pilot and persisted in the receipt',()=>{
 const {g}=game();const x=leg(g,1);assert.equal(x.pilotCheck.pool.difficulty,3);assert.equal(x.pilotCheck.pool.setback,0);assert.equal(x.adjudication.vehicleId,g.state.vehicles.activeVehicleId);
 assert.equal(x.adjudication.pilotEvidence.roll.faces.length,Object.values(x.pilotCheck.pool).reduce((a,b)=>a+b,0));
 assert.equal(validateJewelGrandPrixPilotEvidence(x.adjudication.pilotEvidence,1,false),true);validate(g);
});
test('P186 missed buoy actually increases the next rolled pool and survives three reopenings',()=>{
 const {g,repo}=game();const first=leg(g,1,{rng:()=>.99});assert.equal(first.adjudication.missedBuoy,true);
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(adapter.status(restored.state.world).raceNextLegRequirements.missedBuoySetbacks,1);validate(restored);}
 const second=leg(g,2);assert.equal(second.pilotCheck.pool.setback,1);assert.equal(second.adjudication.appliedSetbacks,1);
});
test('P186 tibannuck setbacks are rolled, system strain is applied to the real vehicle',()=>{
 const {g}=game();for(let i=1;i<=3;i++)leg(g,i);const old=g.activeVehicle().systemStrain;
 const r=leg(g,4,{rng:()=>.78});assert.equal(r.pilotCheck.pool.setback,3);assert.equal(r.adjudication.hazardStrain,1);
 assert.equal(g.activeVehicle().systemStrain,old+1);assert.equal(r.vehicle.systemStrain,old+1);validate(g);
});
test('P186 real challenge despair produces actual EotE vehicle critical with -20 modifier',()=>{
 const {g}=game();for(let i=1;i<=3;i++)leg(g,i);
 // 0.99 maps challenge die to Despair; -20 is applied to the actual critical table.
 const r=leg(g,4,{difficultyUpgrades:1,rng:()=>.99});assert.equal(r.pilotCheck.pool.challenge,1);
 assert.equal(r.adjudication.criticalModifier,-20);assert.equal(r.vehicleCritical.externalModifier,-20);
 assert.equal(g.activeVehicle().criticalHits.length,1);assert.equal(r.vehicleCritical.rawRoll,100);validate(g);
});
test('P186 beldon 3+ net threats creates a real vehicle critical',()=>{
 const {g}=game();for(let i=1;i<=4;i++)leg(g,i);
 const r=leg(g,5,{rng:()=>.78});assert.equal(r.pilotCheck.pool.setback,2);
 assert.ok(r.pilotCheck.roll.netAdvantage<=-3);assert.equal(r.adjudication.criticalModifier,0);
 assert.equal(r.vehicleCritical.externalModifier,0);assert.equal(g.activeVehicle().criticalHits.length,1);validate(g);
});
test('P186 duplicate and invalid requests roll back both world and vehicle without save',()=>{
 const {g,repo}=game();leg(g,1);const before=copy(g.state),count=repo.saves;
 assert.throws(()=>leg(g,1),/idempotência|ordem/);assert.deepEqual(g.state,before);assert.equal(repo.saves,count);
 assert.throws(()=>g.resolveJewelGrandPrixLeg({actionId:'bad',difficultyUpgrades:4}),/inválidas/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,count);
});
test('P186 forged die face and net successes rejected before advancing',()=>{
 const {g}=game();leg(g,1);const bad=copy(g.state.world);
 bad.publishedAdventure.operations.receipts['roll-1'].pilotEvidence.roll.faces[0].index=7;
 bad.publishedAdventure.operations.race.legs[0].pilotEvidence.roll.faces[0].index=7;
 assert.throws(()=>adapter.status(bad),/adulterad/);
 const bad2=copy(g.state.world);bad2.publishedAdventure.operations.receipts['roll-1'].pilotEvidence.roll.netSuccess=999;
 bad2.publishedAdventure.operations.race.legs[0].pilotEvidence.roll.netSuccess=999;
 assert.throws(()=>adapter.status(bad2),/adulterad/);
});
test('P186 no active vehicle cannot invent a race result or persist a receipt',()=>{
 const {g,repo}=game();const v=copy(g.state.vehicles);v.activeVehicleId=null;g.commit('VEHICLES_UPDATED',{vehicles:v});const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>leg(g,1),/veículo real/);assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P186 Android bridge and offline bundle expose real roll without changing legacy adjudication',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const fs=await import('node:fs');
 const bundle=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 assert.equal(typeof BraseiroUiBridge.prototype.resolveJewelGrandPrixLeg,'function');
 assert.equal(typeof GameController.prototype.adjudicateJewelOperation,'function');
 assert.ok(bundle.includes('resolveJewelGrandPrixLeg'));
 assert.ok(bundle.includes('jewelGrandPrixDice.mjs'));
});
test('P186 actual critical and system strain persist together after three controller reopens',()=>{
 const {g,repo}=game();for(let i=1;i<=3;i++)leg(g,i);
 const result=leg(g,4,{difficultyUpgrades:1,rng:()=>.99});
 const expected=copy(result.vehicle),receipt=copy(result.adjudication);
 for(let i=0;i<3;i++){
   const recovered=new GameController(repo);
   assert.deepEqual(recovered.activeVehicle(),expected);
   assert.deepEqual(adapter.status(recovered.state.world).progress.operations.receipts['roll-4'],receipt);
   validate(recovered);
 }
});
test('P186 forged difficulty upgrades or extra dice are rejected on replay',()=>{
 const {g}=game();leg(g,1);
 for(const change of [p=>p.difficultyUpgrades=2,p=>p.roll.faces.push(p.roll.faces[0])]){
   const forged=copy(g.state.world);change(forged.publishedAdventure.operations.receipts['roll-1'].pilotEvidence);
   change(forged.publishedAdventure.operations.race.legs[0].pilotEvidence);
   assert.throws(()=>adapter.status(forged),/adulterad/);
 }
});
test('P186 duplicate action through existing Android operation path cannot roll twice',()=>{
 const {g,repo}=game();const result=g.adjudicateJewelOperation('race_roll',{actionId:'via-bridge',rng:()=>.1});
 assert.equal(result.adjudication.kind,'GRAND_PRIX_LEG');const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>g.adjudicateJewelOperation('race_roll',{actionId:'via-bridge',rng:()=>.99}),/idempotência/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
