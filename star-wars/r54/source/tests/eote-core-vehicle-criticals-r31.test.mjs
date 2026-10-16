import test from 'node:test';
import assert from 'node:assert/strict';
import {
  VEHICLE_CRITICAL_HIT_TABLE, vehicleCriticalResult, createVehicleFromProfile, applyVehicleCritical,
  repairVehicleCritical, vehicleStatus, vehicleCriticalModifiers, unrepairedVehicleCriticalCount,
  canVoluntarilyInflictVehicleStrain, punchIt, accelerateOrBrake, expireVehicleCriticalRound,
  normalizeVehicleRuntime
} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';

test('Tabela 7-9 Core cobre 01-09 até 154+ sem lacunas',()=>{
  assert.equal(VEHICLE_CRITICAL_HIT_TABLE.length,19);
  for(let n=1;n<=200;n++) assert.ok(vehicleCriticalResult(n));
  assert.equal(vehicleCriticalResult(1).key,'mechanical_stress');
  assert.equal(vehicleCriticalResult(100).key,'shield_overload');
  assert.equal(vehicleCriticalResult(154).key,'vaporized');
});

test('Mechanical Stress causa 1 system strain, não a sequência simplificada antiga',()=>{
  const v=createVehicleFromProfile('xwing_t65b');
  const r=applyVehicleCritical(v,{roll:1});
  assert.equal(r.hit.key,'mechanical_stress');
  assert.equal(r.vehicle.systemStrain,1);
  assert.equal(r.hit.repairDifficulty,1);
});

test('cada Critical Hit não reparado soma +10 à próxima rolagem',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  let r=applyVehicleCritical(v,{roll:1}); v=r.vehicle;
  r=applyVehicleCritical(v,{roll:1});
  assert.equal(r.hit.existingCriticalModifier,10);
  assert.equal(r.hit.total,11);
  assert.equal(r.hit.key,'jostled');
});

test('Critical Hit reparado continua no histórico mas deixa de somar +10',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:1}).vehicle;
  v=repairVehicleCritical(v,0);
  assert.equal(unrepairedVehicleCriticalCount(v),0);
  const r=applyVehicleCritical(v,{roll:1});
  assert.equal(r.hit.total,1);
});

test('Jostled devolve consequências de tripulação para integração do controller',()=>{
  const r=applyVehicleCritical(createVehicleFromProfile('xwing_t65b'),{roll:10});
  assert.equal(r.hit.crewStrain,1);
  assert.equal(r.hit.crewDisorientedRounds,1);
});

test('Losing Power to Shields reduz apenas a zona afetada e reparo restaura derivação',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:19,defenseZone:'proa'}).vehicle;
  assert.equal(vehicleStatus(v).defense.proa,0);
  assert.equal(vehicleStatus(v).defense.popa,1);
  v=repairVehicleCritical(v,0);
  assert.equal(vehicleStatus(v).defense.proa,1);
});

test('Shields Failing sem Defesa usa fallback de 2 system strain',()=>{
  const v=createVehicleFromProfile('tie_ln');
  const r=applyVehicleCritical(v,{roll:55});
  assert.equal(r.hit.fallbackSystemStrain,true);
  assert.equal(r.vehicle.systemStrain,2);
});

test('falha de hiperimpulsor bloqueia salto até reparo',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:64}).vehicle;
  assert.equal(vehicleStatus(v).hyperspaceDisabled,true);
  v=repairVehicleCritical(v,0);
  assert.equal(vehicleStatus(v).hyperspaceDisabled,false);
});

test('Power Fluctuations bloqueia system strain voluntário e Punch It',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.speed=2;
  v=applyVehicleCritical(v,{roll:73}).vehicle;
  assert.equal(canVoluntarilyInflictVehicleStrain(v),false);
  assert.throws(()=>punchIt(v),/Power Fluctuations/);
});

test('Engine Damaged reduz velocidade máxima efetiva sem corromper valor-base',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.speed=5;
  v=applyVehicleCritical(v,{roll:91}).vehicle;
  assert.equal(v.speedMax,5);
  assert.equal(vehicleStatus(v).effectiveSpeedMax,4);
  assert.equal(vehicleStatus(v).effectiveSpeed,4);
});

test('Engines Down impede manobras mas preserva curso/velocidade atual',()=>{
  let v=createVehicleFromProfile('xwing_t65b'); v.speed=3;
  v=applyVehicleCritical(v,{roll:100,modifier:9}).vehicle;
  const s=vehicleStatus(v);
  assert.equal(s.effectiveSpeedMax,0);
  assert.equal(s.effectiveSpeed,3);
  assert.equal(s.noManeuvers,true);
  assert.throws(()=>accelerateOrBrake(v,1),/Critical Hit/);
});

test('Destabilized reduz ambos thresholds efetivos à metade',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:100,modifier:34}).vehicle;
  const s=vehicleStatus(v);
  assert.equal(s.effectiveHullThreshold,5);
  assert.equal(s.effectiveSystemStrainThreshold,5);
});

test('Major Hull Breach em atmosfera vira Destabilized',()=>{
  const r=applyVehicleCritical(createVehicleFromProfile('xwing_t65b'),{roll:100,modifier:27,inAtmosphere:true});
  assert.equal(r.hit.key,'destabilized');
  assert.equal(vehicleStatus(r.vehicle).effectiveHullThreshold,5);
});

test('Component Hit expira mecanicamente mas segue contando +10 até reparo',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:46,component:'hyperdrive',currentRound:2}).vehicle;
  assert.ok(vehicleCriticalModifiers(v,{currentRound:3}).temporaryDisabledComponents.includes('hyperdrive'));
  v=expireVehicleCriticalRound(v,4);
  assert.equal(vehicleCriticalModifiers(v,{currentRound:4}).temporaryDisabledComponents.includes('hyperdrive'),false);
  assert.equal(unrepairedVehicleCriticalCount(v),1);
  const next=applyVehicleCritical(v,{roll:1,currentRound:4});
  assert.equal(next.hit.total,11);
});

test('Shield Overload só pode ser reparado após o encontro',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:100}).vehicle;
  assert.equal(v.systemStrain,2);
  assert.throws(()=>repairVehicleCritical(v,0,{encounterEnded:false}),/fim do encontro/);
  assert.doesNotThrow(()=>repairVehicleCritical(v,0,{encounterEnded:true}));
});

test('Breaking Up destrói ao atravessar o fim do assalto seguinte',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:100,modifier:45,currentRound:3}).vehicle;
  assert.equal(vehicleStatus(v,{currentRound:4}).destroyed,false);
  v=expireVehicleCriticalRound(v,5);
  assert.equal(vehicleStatus(v,{currentRound:5}).destroyed,true);
});

test('Vaporized destrói imediatamente e não admite reparo',()=>{
  let v=createVehicleFromProfile('xwing_t65b');
  v=applyVehicleCritical(v,{roll:100,modifier:54}).vehicle;
  assert.equal(vehicleStatus(v).destroyed,true);
  assert.throws(()=>repairVehicleCritical(v,0),/não pode ser reparada/);
});

test('normalizador preserva saves antigos sem criticalState novo',()=>{
  const v=createVehicleFromProfile('xwing_t65b');
  v.criticalHits=[{number:1,effect:'legado',repaired:false,component:null}];
  delete v.roundEffects;
  const runtime=normalizeVehicleRuntime({activeVehicleId:v.id,vehicles:{[v.id]:v}});
  assert.ok(runtime.vehicles[v.id]);
  assert.equal(runtime.vehicles[v.id].criticalHits[0].effect,'legado');
  assert.equal(runtime.vehicles[v.id].roundEffects.evasive,false);
});
