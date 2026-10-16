import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('Ala-X entra no estado persistente e combate de veículos reabre intacto',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'veh'));
  game.loadStarterCharacter('zal'); game.addVehicle('xwing_t65b'); game.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Curto'});
  game.useVehiclePilotManeuver('accelerate');
  assert.equal(game.activeVehicle().speed,1); assert.equal(game.state.vehicles.combat.maneuversUsed,1);
  const reopened=new GameController(new LocalStorageRepository(storage,'veh'));
  assert.equal(reopened.activeVehicle().shortName,'Ala-X T-65B'); assert.equal(reopened.state.vehicles.combat.active,true);
});

test('segunda manobra exclusiva exige Tensão de sistemas ou ação',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'veh2')); game.loadStarterCharacter('zal'); game.addVehicle('xwing_t65b'); game.beginVehicleCombat();
  game.useVehiclePilotManeuver('accelerate');
  assert.throws(()=>game.useVehiclePilotManeuver('accelerate'),/segunda manobra exige/i);
  game.useVehiclePilotManeuver('accelerate',{method:'strain'}); assert.equal(game.activeVehicle().systemStrain,2); assert.equal(game.state.vehicles.combat.maneuversUsed,2);
});

test('ataque veicular consome ação e aplica resultado ao alvo',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'veh3')); game.loadStarterCharacter('zal'); game.addVehicle('xwing_t65b'); game.beginVehicleCombat();
  const r=game.resolveVehicleAttack({weaponIndex:1,rng:()=>0.2}); assert.equal(game.state.vehicles.combat.actionUsed,true); assert.equal(typeof r.hit,'boolean');
  assert.throws(()=>game.resolveVehicleAttack({weaponIndex:1}),/ação deste assalto já foi usada/i);
});

test('controle de danos usa Mecânica do personagem e persiste',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'veh4')); game.loadStarterCharacter('tendaar'); game.addVehicle('xwing_t65b'); game.applyVehicleStrain(4); game.applyVehicleDamage({damage:7});
  const r=game.vehicleDamageControl({rng:()=>0.2}); assert.equal(Number.isInteger(r.difficulty),true);
  const reopened=new GameController(new LocalStorageRepository(storage,'veh4')); assert.equal(reopened.activeVehicle().id,'veh_xwing_t65b');
});
