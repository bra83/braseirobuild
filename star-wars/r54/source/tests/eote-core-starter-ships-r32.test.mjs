import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EOTE_CORE_STARTER_VEHICLE_KEYS, STARTER_VEHICLE_PROFILES, eoteCoreStarterVehicleProfiles,
  createVehicleFromProfile, resolveVehicleWeaponAttack, vehicleStatus,
  tractorLockDifficulty, tractorEscapeCheck
} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';

const ace={characteristics:{agilidad:5},skills:{artilleria:5,pilotar_vehiculo_espacial:5}};

test('starter ships EotE Core ativos são Wayfarer, YT-1300 e Firespray',()=>{
  assert.deepEqual(EOTE_CORE_STARTER_VEHICLE_KEYS,['wayfarer','yt1300','firespray']);
  const profiles=eoteCoreStarterVehicleProfiles();
  assert.deepEqual(Object.keys(profiles),['wayfarer','yt1300','firespray']);
  for(const p of Object.values(profiles)) assert.equal(p.source.activeRulesAuthority,true);
});

test('Wayfarer reproduz perfil Core final + errata de Sensor Range Short',()=>{
  const v=createVehicleFromProfile('wayfarer');
  assert.deepEqual({sil:v.silhouette,speed:v.speedMax,handling:v.handling,armor:v.armor,hull:v.hullThreshold,strain:v.systemStrainThreshold},{sil:5,speed:3,handling:-2,armor:4,hull:32,strain:20});
  assert.deepEqual(v.defense,{proa:1,babor:1,estribor:1,popa:2});
  assert.equal(v.sensorRange,'Curto'); assert.equal(v.encumbranceCapacity,850); assert.equal(v.hardPoints,5); assert.equal(v.price,120000); assert.equal(v.rarity,5);
});

test('YT-1300 reproduz perfil Core final',()=>{
  const v=createVehicleFromProfile('yt1300');
  assert.deepEqual({sil:v.silhouette,speed:v.speedMax,handling:v.handling,armor:v.armor,hull:v.hullThreshold,strain:v.systemStrainThreshold},{sil:4,speed:3,handling:-1,armor:3,hull:22,strain:15});
  assert.deepEqual(v.defense,{proa:1,babor:0,estribor:0,popa:1});
  assert.equal(v.hyperdriveClass,2); assert.equal(v.backupHyperdriveClass,12); assert.equal(v.encumbranceCapacity,165); assert.equal(v.hardPoints,6); assert.equal(v.weapons.length,2);
});

test('Firespray reproduz perfil Core final',()=>{
  const v=createVehicleFromProfile('firespray');
  assert.deepEqual({sil:v.silhouette,speed:v.speedMax,handling:v.handling,armor:v.armor,hull:v.hullThreshold,strain:v.systemStrainThreshold},{sil:4,speed:4,handling:0,armor:4,hull:15,strain:12});
  assert.deepEqual(v.defense,{proa:1,babor:0,estribor:0,popa:1});
  assert.equal(v.hyperdriveClass,3); assert.equal(v.backupHyperdriveClass,15); assert.equal(v.encumbranceCapacity,40); assert.equal(v.hardPoints,4); assert.equal(v.weapons[1].qualities.tractor,2);
});

test('Tractor 2 acerta sem causar Hull Trauma e trava movimento com dificuldade 2',()=>{
  const attacker=createVehicleFromProfile('firespray');
  const target=createVehicleFromProfile('yt1300');
  const r=resolveVehicleWeaponAttack({character:ace,attacker,target,weaponIndex:1,targetRange:'Bocajarro',rng:()=>0.1});
  assert.equal(r.hit,true); assert.equal(r.nonDamage,true); assert.equal(r.damage,0); assert.equal(r.targetDamage,null);
  assert.equal(r.targetEffect.type,'tractor'); assert.equal(r.targetEffect.rating,2);
  assert.equal(r.targetEffect.vehicle.hullDamage,0); assert.equal(tractorLockDifficulty(r.targetEffect.vehicle),2);
  assert.equal(vehicleStatus(r.targetEffect.vehicle).tractorLocked,true);
});

test('teste de Pilotagem contra Tractor usa rating e libera a nave em sucesso',()=>{
  let v=createVehicleFromProfile('yt1300');
  // produz lock pelo próprio ataque para validar a cadeia completa
  const attacker=createVehicleFromProfile('firespray');
  const hit=resolveVehicleWeaponAttack({character:ace,attacker,target:v,weaponIndex:1,targetRange:'Bocajarro',rng:()=>0.1});
  v=hit.targetEffect.vehicle;
  const escaped=tractorEscapeCheck(v,ace,{rng:()=>0.1});
  assert.equal(escaped.difficulty,2); assert.equal(escaped.check.success,true); assert.equal(tractorLockDifficulty(escaped.vehicle),0);
});

test('X-Wing legado continua referência AoR; TIE/ln e Lambda foram promovidos pelo próprio Core EotE no R35',()=>{
  const x=STARTER_VEHICLE_PROFILES.xwing_t65b;
  assert.equal(x.source.authority,'CROSS_LINE_REFERENCE');
  assert.equal(x.source.activeRulesAuthority,false);
  for(const key of ['tie_ln','lambda_t4a']){
    const p=STARTER_VEHICLE_PROFILES[key];
    assert.equal(p.source.authority,'PRIMARY_EOTE_CORE');
    assert.equal(p.source.activeRulesAuthority,true);
  }
});
