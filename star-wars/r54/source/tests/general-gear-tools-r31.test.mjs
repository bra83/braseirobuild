import test from 'node:test';
import assert from 'node:assert/strict';
import { mechanicsToolSetbackRelief, emergencyRepairPatchProfile, ammoReloadProfile } from '../app/src/main/assets/vtt/js/rules/generalGearEffects.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('kit de ferramentas remove somente a parcela explicitamente atribuída à falta de ferramentas',()=>{
  const r=mechanicsToolSetbackRelief('tool_kit',{setback:3,missingToolsSetback:2});
  assert.equal(r.removedSetback,2); assert.equal(r.remainingSetback,1); assert.equal(r.requiredForDroidRepair,true);
  assert.throws(()=>mechanicsToolSetbackRelief('tool_kit',{setback:1,missingToolsSetback:2}),/não pode exceder/);
});

test('GameController aplica kit de ferramentas apenas a Mecânica',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r31-tools'));
  game.loadStarterCharacter('tendaar');
  game.state.player.character.gear.push({id:'tool_kit',name:'Kit de ferramentas',quantity:1}); game.repository.save(game.state);
  const r=game.resolveSkillCheck({skillKey:'mecanica',difficulty:2,setback:3,gearAssist:{itemId:'tool_kit',missingToolsSetback:2},rng:()=>0});
  assert.equal(r.gearModifier.removedSetback,2); assert.equal(r.pool.setback,1);
  assert.throws(()=>game.resolveSkillCheck({skillKey:'percepcion',difficulty:2,setback:1,gearAssist:{itemId:'tool_kit',missingToolsSetback:1},rng:()=>0}),/só pode aliviar/);
});

test('remendo de reparo de emergência fica registrado como ação para droide, equivalente ao injetor, sem fingir consumo',()=>{
  const p=emergencyRepairPatchProfile();
  assert.equal(p.target,'droid'); assert.equal(p.cost,'action'); assert.equal(p.sameAs,'stimpack'); assert.equal(p.automated,false);
});

test('recarga adicional fica registrada como manobra após Desespero de munição, sem inventar rastreamento de munição',()=>{
  const p=ammoReloadProfile();
  assert.equal(p.trigger,'despair_on_ranged_attack_ammo_depleted'); assert.equal(p.cost,'maneuver'); assert.equal(p.effect,'reload_and_continue'); assert.equal(p.automated,false);
});
