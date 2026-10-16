import test from 'node:test';
import assert from 'node:assert/strict';
import { createZalStarterCharacter } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { createVehicleFromProfile, stopShort, setControlledFire, vehicleAttackPool } from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import fs from 'node:fs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

function character(...talents){ const c=createZalStarterCharacter(); c.talents=[...talents]; c.skills.artilleria=2; c.skills.pilotar_vehiculo_espacial=2; return c; }

test('Parada brusca reduz Velocidade a 0 e causa Tensão de sistemas igual à Velocidade reduzida',()=>{
  const v=createVehicleFromProfile('xwing_t65b'); v.speed=4; v.systemStrain=1;
  const r=stopShort(v);
  assert.equal(r.vehicle.speed,0);
  assert.equal(r.systemStrain,4);
  assert.equal(r.vehicle.systemStrain,5);
  assert.equal(v.speed,4,'a ficha original não é mutada');
});

test('Fogo controlado permanece bloqueado como legado Age of Rebellion no perfil EotE Core',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r15-fire'));
  game.loadStarterCharacter('zal'); game.addVehicle('xwing_t65b'); game.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Curto'});
  assert.throws(()=>game.useControlledFire(),/legado Age of Rebellion/i);
});

test('interface PT-BR mantém Parada em seco e oculta Fogo controlado legado',()=>{
  const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
  const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
  assert.match(html,/data-vehicle-maneuver="stop_short"[^>]*>Parada em seco</i);
  assert.match(html,/id="controlledFireAction"[^>]*hidden[^>]*disabled[^>]*>Fogo controlado · legado AoR</i);
  assert.match(js,/controlledFireAction'\)\.hidden=true/);
  assert.doesNotMatch(js,/game\.useControlledFire\(\)/);
});
