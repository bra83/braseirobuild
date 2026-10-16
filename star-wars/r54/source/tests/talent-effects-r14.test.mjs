import test from 'node:test';
import assert from 'node:assert/strict';
import { createZalStarterCharacter } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { quickReactionOption, talentPilotedVehicleDefenseBonus, talentPilotedVehicleSilhouette, fullThrottleTalentSpec } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { preparePlayerInitiative } from '../app/src/main/assets/vtt/js/rules/initiative.mjs';
import { createVehicleFromProfile, vehicleStatus, attemptFullThrottle, expireFullThrottle, vehicleAttackPool } from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

function withTalents(...names){ const c=createZalStarterCharacter(); c.talents=[...names]; c.skills.pilotar_vehiculo_espacial=2; return c; }

test('Reação rápida limita gasto ao grau e converte Tensão em Impulso de iniciativa',()=>{
  const c=withTalents('Reação rápida','Reação rápida');
  assert.deepEqual(quickReactionOption(c,2),{rank:2,spend:2,boost:2});
  assert.throws(()=>quickReactionOption(c,3),/no máximo 2/i);
  const prepared=preparePlayerInitiative(c,{quickReactionStrain:2});
  assert.equal(prepared.character.derived.strain.current,2);
  assert.equal(prepared.boost,2);
});

test('Condução defensiva soma Defesa em todas as zonas sem alterar a ficha base da nave',()=>{
  const c=withTalents('Condução defensiva','Condução defensiva');
  const v=createVehicleFromProfile('xwing_t65b');
  assert.equal(talentPilotedVehicleDefenseBonus(c),2);
  assert.deepEqual(vehicleStatus(v,{operator:c}).defense,{proa:3,babor:2,estribor:2,popa:3});
  assert.deepEqual(v.defense,{proa:1,babor:0,estribor:0,popa:1});
});

test('Alvo ardiloso reduz em 1 a Silhueta defensiva do veículo pilotado',()=>{
  const c=withTalents('Alvo ardiloso');
  assert.equal(talentPilotedVehicleSilhouette(c,3),2);
  assert.equal(talentPilotedVehicleSilhouette(withTalents(),3),3);
});

test('Aceleração máxima usa dificuldade 3, duração por Astúcia e Suprema aumenta +2',()=>{
  const c=withTalents('Aceleração máxima','Aceleração máxima suprema');
  const v=createVehicleFromProfile('xwing_t65b');
  const spec=fullThrottleTalentSpec(c);
  assert.equal(spec.bonus,2);
  const r=attemptFullThrottle(v,c,{currentRound:2,rng:()=>0.2});
  assert.equal(r.difficulty,3);
  assert.equal(r.check.success,true);
  assert.equal(r.vehicle.speedMax,7);
  assert.equal(r.durationRounds,c.characteristics.astucia);
  assert.equal(r.vehicle.roundEffects.fullThrottle.expiresAfterRound,4);
  assert.equal(expireFullThrottle(r.vehicle,4).expired,false);
  const expired=expireFullThrottle(r.vehicle,5);
  assert.equal(expired.expired,true);
  assert.equal(expired.vehicle.speedMax,5);
});

test('Aceleração máxima aprimorada pode ser tentada como manobra, custa 1 Tensão e usa dificuldade 2',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'r14-ft'));
  game.loadStarterCharacter('zal');
  game.state.player.character.talents=['Aceleração máxima','Aceleração máxima aprimorada'];
  game.state.player.character.skills.pilotar_vehiculo_espacial=2;
  game.addVehicle('xwing_t65b'); game.beginVehicleCombat();
  const before=game.state.player.character.derived.strain.current;
  const r=game.useFullThrottle({asManeuver:true,rng:()=>0.2});
  assert.equal(r.difficulty,2);
  assert.equal(game.state.player.character.derived.strain.current,before+1);
  assert.equal(game.state.vehicles.combat.maneuversUsed,1);
  assert.equal(game.activeVehicle().speedMax,6);
});

test('ataque contra piloto com Alvo ardiloso e Condução defensiva usa Silhueta/Defesa efetivas',()=>{
  const attacker=createVehicleFromProfile('tie_ln');
  const target=createVehicleFromProfile('xwing_t65b');
  const targetOperator=withTalents('Alvo ardiloso','Condução defensiva');
  const attackerCharacter=withTalents(); attackerCharacter.skills.artilleria=2;
  const r=vehicleAttackPool({character:attackerCharacter,attacker,target,targetOperator,facing:'proa',weaponIndex:0,targetRange:'Bocajarro',rng:()=>0.2});
  assert.equal(r.targetSilhouette,2);
  assert.equal(r.targetDefense,2);
});

test('Cartógrafo galáctico reduz à metade o tempo habitual da Astronavegação',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'r14-cartographer'));
  game.loadStarterCharacter('zal');
  game.state.player.character.talents=['Cartógrafo galáctico'];
  game.addVehicle('xwing_t65b');
  const r=game.plotHyperspaceCourse({rng:()=>0.2});
  assert.equal(r.timeMultiplier,0.5);
});
