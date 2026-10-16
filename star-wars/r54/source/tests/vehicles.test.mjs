import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createVehicleFromProfile, vehicleStatus, handlingPoolModifier, applyVehicleScaleDamage, applyVehicleSystemStrain,
  recoverVehicleSystemsAfterFullDay, accelerateOrBrake, punchIt, rangeStepsForSpeed, movePlanetaryRange,
  gunneryDifficultyBySilhouette, enfiladeDifficulty, pilotingHazardDifficulty, damageControlDifficulty,
  applyDamageControl, applyManualRepairs, applyVehicleCritical, resolveVehicleWeaponAttack
} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';

test('perfil Ala-X reproduz os valores do livro inicial',()=>{
  const v=createVehicleFromProfile('xwing_t65b');
  assert.equal(v.silhouette,3); assert.equal(v.speedMax,5); assert.equal(v.handling,1); assert.equal(v.armor,3);
  assert.equal(v.hullThreshold,10); assert.equal(v.systemStrainThreshold,10); assert.deepEqual(v.defense,{proa:1,babor:0,estribor:0,popa:1});
});

test('Blindagem reduz dano em escala de veículo e limiar só inutiliza ao superar',()=>{
  let v=createVehicleFromProfile('tie_ln');
  let r=applyVehicleScaleDamage(v,{damage:6});
  assert.equal(r.suffered,4); assert.equal(r.vehicle.hullDamage,4); assert.equal(r.status.hullDisabled,false);
  r=applyVehicleScaleDamage(r.vehicle,{damage:4});
  assert.equal(r.vehicle.hullDamage,6); assert.equal(r.status.hullDisabled,false);
  r=applyVehicleScaleDamage(r.vehicle,{damage:3});
  assert.equal(r.vehicle.hullDamage,7); assert.equal(r.status.hullDisabled,true);
});

test('Tensão de sistemas acima do limiar desliga velocidade, armas e defesa',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.speed=5;
  const r=applyVehicleSystemStrain(v,11);
  assert.equal(r.status.systemsShutdown,true); assert.equal(r.status.effectiveSpeed,0); assert.equal(r.status.weaponsOnline,false); assert.equal(r.status.defense.proa,0);
});

test('um dia completo sem nova tensão recupera 1 Tensão de sistemas',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.systemStrain=4;
  assert.equal(recoverVehicleSystemsAfterFullDay(v).vehicle.systemStrain,3);
  assert.equal(recoverVehicleSystemsAfterFullDay(v,{sufferedNewStrain:true}).vehicle.systemStrain,4);
});

test('Manejo positivo vira Impulso e negativo Contratempo',()=>{
  const x=createVehicleFromProfile('xwing_t65b'); assert.deepEqual(handlingPoolModifier(x),{boost:1,setback:0});
  const n={...x,handling:-2}; assert.deepEqual(handlingPoolModifier(n),{boost:0,setback:2});
});

test('Acelerar/Frear altera exatamente 1 e respeita limites',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v=accelerateOrBrake(v,1); assert.equal(v.speed,1); v=accelerateOrBrake(v,-1); assert.equal(v.speed,0); v=accelerateOrBrake(v,-1); assert.equal(v.speed,0);
});

test('Pisar a fundo vai à velocidade máxima e sofre tensão igual à diferença',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.speed=2; const r=punchIt(v); assert.equal(r.vehicle.speed,5); assert.equal(r.strain,3); assert.equal(r.vehicle.systemStrain,3);
});

test('Avançar usa passos de alcance conforme velocidade',()=>{
  assert.equal(rangeStepsForSpeed(0),0); assert.equal(rangeStepsForSpeed(1),1); assert.equal(rangeStepsForSpeed(3),2); assert.equal(rangeStepsForSpeed(6),3);
  assert.equal(movePlanetaryRange('Longo',{direction:'closer',speed:3}),'Curto');
  assert.equal(movePlanetaryRange('Curto',{direction:'farther',speed:1}),'Médio');
});

test('dificuldade de Artilharia usa diferença de Silhueta',()=>{
  assert.equal(gunneryDifficultyBySilhouette(3,3),2); assert.equal(gunneryDifficultyBySilhouette(2,4),1); assert.equal(gunneryDifficultyBySilhouette(5,3),3); assert.equal(gunneryDifficultyBySilhouette(6,3),4);
});

test('Enfilar usa diferença de Velocidade',()=>{
  assert.equal(enfiladeDifficulty(4,4),2); assert.equal(enfiladeDifficulty(5,4),1); assert.equal(enfiladeDifficulty(3,4),3); assert.equal(enfiladeDifficulty(2,4),4);
});

test('perigo de pilotagem usa maior entre Velocidade e metade da Silhueta, com upgrades pelo menor',()=>{
  const v=createVehicleFromProfile('lambda_t4a'); v.speed=3; assert.deepEqual(pilotingHazardDifficulty(v),{difficulty:3,upgrades:2});
});

test('controle de danos calcula dificuldade e repara um tipo por vez',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.hullDamage=6; v.systemStrain=4;
  assert.equal(damageControlDifficulty(v,{mode:'system'}),1); assert.equal(damageControlDifficulty(v,{mode:'hull'}),2);
  const sys=applyDamageControl(v,{successes:2,mode:'system'}); assert.equal(sys.vehicle.systemStrain,2); assert.equal(sys.vehicle.hullDamage,6);
  const hull=applyDamageControl(v,{successes:2,mode:'hull'}); assert.equal(hull.vehicle.hullDamage,4); assert.equal(hull.vehicle.systemStrain,4);
  const manual=applyManualRepairs(v,{successes:3}); assert.equal(manual.hullRecovered,2);
});

test('Critical Hits de veículo usam a Table 7-9 EotE Core com d100 e +10 cumulativo',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  let r=applyVehicleCritical(v,{roll:1}); v=r.vehicle; assert.equal(v.systemStrain,1); assert.equal(r.hit.key,'mechanical_stress');
  r=applyVehicleCritical(v,{roll:1}); v=r.vehicle; assert.equal(r.hit.total,11); assert.equal(r.hit.key,'jostled');
  r=applyVehicleCritical(v,{roll:71}); v=r.vehicle; assert.equal(r.hit.total,91); assert.equal(r.hit.key,'engine_damaged');
  assert.equal(vehicleStatus(v).effectiveSpeedMax,4);
});

test('ataque de arma de nave usa Artilharia, Silhueta, Defesa e Blindagem',()=>{
  const attacker=createVehicleFromProfile('xwing_t65b'); const target=createVehicleFromProfile('tie_ln'); const char=STARTER_PRESETS['zal']();
  const r=resolveVehicleWeaponAttack({character:char,attacker,target,weaponIndex:0,rng:()=>0.2});
  assert.equal(r.difficulty,2); assert.equal(r.targetDefense,0); assert.equal(typeof r.hit,'boolean');
  if(r.hit) assert.equal(r.targetDamage.effectiveArmor,2);
});

test('Manobras evasivas aumentam a Dificuldade e Seguir o objetivo melhora Capacidade, sem dados extras indevidos',()=>{
  const char=STARTER_PRESETS['zal']();
  let attacker=createVehicleFromProfile('xwing_t65b'); let target=createVehicleFromProfile('tie_ln');
  attacker.speed=3; target.speed=3;
  attacker.roundEffects.stayOnTarget=true;
  target.roundEffects.evasive=true;
  const r=resolveVehicleWeaponAttack({character:char,attacker,target,weaponIndex:1,targetRange:'Curto',rng:()=>0.2});
  assert.equal(r.positiveUpgrades,1);
  assert.equal(r.difficultyUpgrades,1);
  assert.equal(r.challenge,1);
  assert.equal(r.difficulty,1);
  assert.equal(r.check.pool.boost,0);
  assert.equal(r.check.pool.setback,0);
});

test('arma de veículo não pode atingir além do alcance indicado no perfil',()=>{
  const char=STARTER_PRESETS['zal']();
  const attacker=createVehicleFromProfile('xwing_t65b'); const target=createVehicleFromProfile('tie_ln');
  assert.throws(()=>resolveVehicleWeaponAttack({character:char,attacker,target,weaponIndex:0,targetRange:'Curto'}),/fora do alcance/i);
  assert.doesNotThrow(()=>resolveVehicleWeaponAttack({character:char,attacker,target,weaponIndex:1,targetRange:'Curto',rng:()=>0.2}));
});

test('Ajuste fino e Reparos sólidos ampliam somente o tipo correspondente',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.hullDamage=6; v.systemStrain=6;
  const operator=STARTER_PRESETS.tendaar(); operator.talents=['Ajuste fino','Reparos sólidos','Reparos sólidos'];
  const sys=applyDamageControl(v,{successes:2,operator,mode:'system'});
  assert.equal(sys.vehicle.systemStrain,3); assert.equal(sys.vehicle.hullDamage,6);
  assert.deepEqual(sys.talentBonuses,{systemStrain:1,hull:0});
  const hull=applyDamageControl(v,{successes:2,operator,mode:'hull'});
  assert.equal(hull.vehicle.systemStrain,6); assert.equal(hull.vehicle.hullDamage,2);
  assert.deepEqual(hull.talentBonuses,{systemStrain:0,hull:2});
});
