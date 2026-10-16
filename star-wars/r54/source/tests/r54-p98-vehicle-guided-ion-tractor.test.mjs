import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(profile,{range='Bocajarro',key=profile,agility=5,gunnery=5}={}){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),`p98-${key}`));g.loadStarterCharacter('tendaar');const c=structuredClone(g.state.player.character);c.characteristics.agilidad=agility;c.skills.artilleria=gunnery;c.skills.pilotar_vehiculo_espacial=5;g.commit('CHARACTER_UPDATED',{character:c});g.addVehicle(profile);g.beginVehicleCombat({targetProfileKey:'tie_ln',range});return g}
function seq(values){let i=0;return()=>values[(i++)%values.length]}

test('P98 Iônica causa Tensão de sistemas sem Trauma de Casco e não oferece Critical Hit',()=>{
  const g=game('ywing_btl');const targetId=g.state.vehicles.combat.targetVehicleId;const before=structuredClone(g.state.vehicles.vehicles[targetId]);
  const r=g.resolveVehicleAttack({weaponIndex:0,rng:()=>0.99});const after=g.state.vehicles.vehicles[targetId];
  assert.equal(r.ion,true);assert.equal(r.hit,true);assert.equal(after.hullDamage,before.hullDamage);assert.ok(after.systemStrain>before.systemStrain);assert.equal(g.state.vehicles.combat.pendingTargetCritical,null);
});

test('P98 Vinculada usa o mesmo orçamento do crítico veicular',()=>{
  const g=game('xwing_t65b');const targetId=g.state.vehicles.combat.targetVehicleId;const before=g.state.vehicles.vehicles[targetId].hullDamage;
  const attack=g.resolveVehicleAttack({weaponIndex:0,rng:()=>0.5});assert.equal(attack.hit,true);assert.equal(attack.pendingSpend.remainingAdvantage,3);assert.ok(g.state.vehicles.combat.pendingTargetCritical);
  const extra=g.activatePendingVehicleQuality({qualityKey:'linked'});assert.equal(extra.remainingAdvantage,1);assert.ok(g.state.vehicles.vehicles[targetId].hullDamage>before);
  assert.throws(()=>g.activatePendingVehicleCritical({rng:()=>0}),/Resultados insuficientes/i);
});

test('P98 Guiada em ataque perdido custa 3 Vantagens e agenda nova passagem',()=>{
  const g=game('skipray_gat12h',{range:'Curto',agility:2,gunnery:1});const attack=g.resolveVehicleAttack({weaponIndex:3,rng:seq([0.9,0.8,0.01])});
  assert.equal(attack.hit,false);assert.equal(attack.check.netAdvantage,3);assert.equal(attack.pendingSpend.qualities[0].key,'guided');
  const a=g.activatePendingVehicleQuality({qualityKey:'guided'});assert.equal(a.remainingAdvantage,0);assert.equal(a.effect.scheduled,true);assert.equal(g.state.vehicles.combat.guidedProjectiles.length,1);
});

test('P98 passagem Guiada resolve no fim do round e aplica dano real',()=>{
  const g=game('skipray_gat12h',{range:'Curto',agility:2,gunnery:1,key:'guided-resolve'});g.resolveVehicleAttack({weaponIndex:3,rng:seq([0.9,0.8,0.01])});g.activatePendingVehicleQuality({qualityKey:'guided'});
  const targetId=g.state.vehicles.combat.targetVehicleId,before=g.state.vehicles.vehicles[targetId].hullDamage;
  g.nextVehicleRound({rng:()=>0.13});const after=g.state.vehicles.vehicles[targetId].hullDamage;
  assert.ok(after>before);assert.equal(g.state.vehicles.combat.guidedProjectiles.length,0);assert.ok(g.state.vehicles.combat.log.some(x=>x.type==='GUIADA_RESOLVIDA'&&x.hit===true));
});

test('P98 Raio Trator bloqueia movimento e fuga legítima remove o lock',()=>{
  const g=game('firespray');const attack=g.resolveVehicleAttack({weaponIndex:1,rng:()=>0.5});assert.equal(attack.targetEffect.type,'tractor');
  const tractoredId=g.state.vehicles.combat.targetVehicleId;g.selectVehicle(tractoredId);
  assert.throws(()=>g.useVehiclePilotManeuver('advance',{direction:'closer'}),/Raio trator ativo/i);
  const escape=g.attemptTractorEscape({rng:()=>0.5});assert.equal(escape.check.success,true);assert.equal(g.state.vehicles.vehicles[tractoredId].tractorLocks.length,0);
});

test('P98 Munição Limitada veicular e Cadência Lenta persistem por arma/round',()=>{
  const g=game('skipray_gat12h',{range:'Curto',key:'ammo-slow'});const activeId=g.state.vehicles.activeVehicleId;
  g.resolveVehicleAttack({weaponIndex:3,rng:()=>0.5});assert.equal(g.state.vehicles.vehicles[activeId].weapons[3].runtimeState.limitedAmmoRemaining,7);assert.equal(g.state.vehicles.vehicles[activeId].weapons[3].runtimeState.lastFiredRound,1);
  g.nextVehicleRound({rng:()=>0.5});assert.throws(()=>g.resolveVehicleAttack({weaponIndex:3,rng:()=>0.5}),/Cadência Lenta 1/i);
  g.nextVehicleRound({rng:()=>0.5});assert.doesNotThrow(()=>g.resolveVehicleAttack({weaponIndex:3,rng:()=>0.5}));
});

test('P98 orçamento veicular não permite Vinculada além do rating',()=>{
  const g=game('xwing_t65b',{key:'linked-rating'});g.resolveVehicleAttack({weaponIndex:0,rng:()=>0.5});
  g.state.vehicles.combat.pendingVehicleSpend.remainingAdvantage=10;
  g.activatePendingVehicleQuality({qualityKey:'linked'});g.activatePendingVehicleQuality({qualityKey:'linked'});g.activatePendingVehicleQuality({qualityKey:'linked'});
  assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'linked'}),/Vinculados disponíveis/i);
});

test('P98 bridge/R46 expõem gasto veicular, Guiada e fuga do Trator sem mutação bruta',async()=>{
  const fs=await import('node:fs/promises');const bridge=await fs.readFile(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');const runtime=await fs.readFile(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(bridge,/activatePendingVehicleQuality\(options/);assert.match(bridge,/reactivateGuidedProjectile\(projectileId/);assert.match(bridge,/attemptTractorEscape\(options/);
  assert.match(runtime,/data-p98-quality/);assert.match(runtime,/data-p98-tractor/);assert.match(runtime,/GUIADA/);assert.doesNotMatch(bridge,/clearTractorLocks\(options/);
});
