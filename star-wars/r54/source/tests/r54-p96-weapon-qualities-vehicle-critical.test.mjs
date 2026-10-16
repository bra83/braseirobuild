import test from 'node:test';
import assert from 'node:assert/strict';
import { weaponUsageModifiers, weaponDefenseModifiers } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='p96'){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key));g.loadStarterCharacter('tendaar');return g}
function setWeapon(g,weapon){const c=structuredClone(g.state.player.character);c.weapons=[{id:'w_p96',name:'Arma P96',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Médio',qualities:[],...weapon,active:true,equipped:true}];c.activeWeaponId='w_p96';g.commit('CHARACTER_UPDATED',{character:c});return c.weapons[0]}

test('P96 qualidades passivas: Imprecisa, Aparatosa, Desajeitada, Defensiva e Deflexão produzem modificadores source-bound',()=>{
  const weapon={qualities:['Imprecisa 2','Aparatosa 4','Desajeitada 5','Defensiva 1','Deflexão 2']};
  const char={characteristics:{fortaleza:2,agilidad:3}};
  const m=weaponUsageModifiers(weapon,char);
  assert.equal(m.setback,2);
  assert.equal(m.cumbersomeDifficulty,2);
  assert.equal(m.unwieldyDifficulty,2);
  assert.equal(m.difficultyIncrease,4);
  assert.deepEqual(weaponDefenseModifiers(weapon),{melee:1,ranged:2});
});

test('P96 ataque real aplica Imprecisa/Aparatosa/Desajeitada ao pool, não apenas metadados',()=>{
  const g=game('p96-passive-pool');
  const c=structuredClone(g.state.player.character);c.characteristics.fortaleza=2;c.characteristics.agilidad=3;c.skills.armas_distancia_ligeras=3;g.commit('CHARACTER_UPDATED',{character:c});
  setWeapon(g,{qualities:['Imprecisa 2','Aparatosa 4','Desajeitada 5']});
  const r=g.resolveWeaponAttack({difficulty:0,rng:()=>0.2});
  assert.equal(r.check.pool.setback,2);
  assert.equal(r.check.pool.difficulty,4);
});

test('P96 Preparação exige manobras reais, persiste e é consumida no ataque',()=>{
  const storage=new MemoryStorage();let g=new GameController(new LocalStorageRepository(storage,'p96-prepare'));g.loadStarterCharacter('tendaar');setWeapon(g,{qualities:['Preparação 1']});g.beginCombat();
  assert.throws(()=>g.resolveWeaponAttack({difficulty:0,rng:()=>0.2}),/exige 1 manobra\(s\) de Preparação/i);
  const prep=g.prepareWeaponForAttack('w_p96');assert.equal(prep.preparedManeuvers,1);
  g=new GameController(new LocalStorageRepository(storage,'p96-prepare'));assert.equal(g.state.player.character.weapons[0].runtimeState.preparedManeuvers,1);
  const r=g.resolveWeaponAttack({difficulty:0,rng:()=>0.2});assert.equal(r.weapon.runtimeState.preparedManeuvers,0);assert.equal(g.state.player.character.weapons[0].runtimeState.preparedManeuvers,0);
});

test('P96 Munição Limitada conta disparos e recarga consome item realmente possuído',()=>{
  const g=game('p96-ammo');setWeapon(g,{qualities:['Munição limitada 1']});
  g.resolveWeaponAttack({difficulty:0,rng:()=>0.2});
  assert.equal(g.state.player.character.weapons[0].runtimeState.limitedAmmoRemaining,0);
  assert.throws(()=>g.resolveWeaponAttack({difficulty:0,rng:()=>0.2}),/sem munição limitada/i);
  const c=structuredClone(g.state.player.character);c.gear=[...(c.gear??[]),{id:'power_pack_or_ammo_reload',name:'Recarga adicional',quantity:1}];g.commit('CHARACTER_UPDATED',{character:c});
  const reload=g.reloadLimitedAmmoWeapon('w_p96');assert.equal(reload.limitedAmmoRemaining,1);assert.equal(g.state.player.character.gear.some(x=>x.id==='power_pack_or_ammo_reload'),false);
  assert.doesNotThrow(()=>g.resolveWeaponAttack({difficulty:0,rng:()=>0.2}));
});

test('P96 Cadência Lenta impede disparo pelos assaltos exigidos',()=>{
  const g=game('p96-slow');setWeapon(g,{qualities:['Cadência lenta 1']});g.beginCombat();
  g.resolveWeaponAttack({difficulty:0,rng:()=>0.2});
  g.advancePlayerTurn();
  assert.throws(()=>g.resolveWeaponAttack({difficulty:0,rng:()=>0.2}),/Cadência Lenta 1/i);
  g.advancePlayerTurn();
  assert.doesNotThrow(()=>g.resolveWeaponAttack({difficulty:0,rng:()=>0.2}));
});

test('P96 ataque veicular real cria Critical Hit pendente e ativação grava crítico no alvo',()=>{
  const storage=new MemoryStorage();let g=new GameController(new LocalStorageRepository(storage,'p96-vcrit'));g.loadStarterCharacter('tendaar');
  const c=structuredClone(g.state.player.character);c.characteristics.agilidad=5;c.skills.artilleria=5;g.commit('CHARACTER_UPDATED',{character:c});
  g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Curto'});
  const attack=g.resolveVehicleAttack({weaponIndex:1,rng:()=>0.99});
  assert.equal(attack.criticalAvailable,true);assert.ok(g.state.vehicles.combat.pendingTargetCritical);
  const targetId=g.state.vehicles.combat.targetVehicleId;
  const critical=g.activatePendingVehicleCritical({rng:()=>0});assert.equal(critical.activated,true);assert.equal(g.state.vehicles.vehicles[targetId].criticalHits.length,1);assert.equal(g.state.vehicles.combat.pendingTargetCritical,null);
  g=new GameController(new LocalStorageRepository(storage,'p96-vcrit'));assert.equal(g.state.vehicles.vehicles[targetId].criticalHits.length,1);
});

test('P96 Critical Hit que exige componente preserva o mesmo d100 até a escolha',()=>{
  const g=game('p96-vcrit-component');const c=structuredClone(g.state.player.character);c.characteristics.agilidad=5;c.skills.artilleria=5;g.commit('CHARACTER_UPDATED',{character:c});g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Curto'});g.resolveVehicleAttack({weaponIndex:1,rng:()=>0.99});
  const first=g.activatePendingVehicleCritical({rng:()=>0.5});assert.equal(first.activated,false);assert.equal(first.componentRequired,true);assert.equal(first.preview.rawRoll,51);assert.equal(g.state.vehicles.combat.pendingTargetCritical.rawRoll,51);
  const second=g.activatePendingVehicleCritical({component:'engines',rng:()=>0});assert.equal(second.activated,true);assert.equal(second.hit.rawRoll,51);assert.equal(second.hit.component,'engines');assert.equal(second.hit.key,'component_hit');
});

test('P96 bridge expõe apenas ativação de crítico pendente do ataque, não criação arbitrária',async()=>{
  const fs=await import('node:fs/promises');const bridge=await fs.readFile(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.match(bridge,/activatePendingVehicleCritical\(options/);
  assert.doesNotMatch(bridge,/applyActiveVehicleCritical\(options/);
  assert.match(bridge,/prepareWeaponForAttack\(weaponId/);
  assert.match(bridge,/reloadLimitedAmmoWeapon\(weaponId/);
});
