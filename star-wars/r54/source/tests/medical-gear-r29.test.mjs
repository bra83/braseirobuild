import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { generalGearCatalogEntry } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';
import { medicineEquipmentProfile, resolveMedicineCheck } from '../app/src/main/assets/vtt/js/rules/recovery.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('Medpac e Medpac de emergência carregam apenas os efeitos de Medicina comprovados',()=>{
  const medpac=generalGearCatalogEntry('medpac');
  const emergency=generalGearCatalogEntry('emergency_medpac');
  assert.deepEqual(medpac.medicine,{medicalSupplies:true,boost:1});
  assert.deepEqual(emergency.medicine,{medicalSupplies:true,boost:0});
  assert.equal(medpac.effectSource.book,'Edge of the Empire Core Rulebook');
  assert.equal(medpac.effectSource.printedPage,112);
});

test('perfil médico recusa item sem efeito de Medicina auditado',()=>{
  assert.throws(()=>medicineEquipmentProfile('comlink'),/não possui efeito de Medicina auditado/);
  assert.throws(()=>medicineEquipmentProfile('item_inexistente'),/Equipamento médico desconhecido/);
});

test('Medpac remove penalização de falta de suprimentos e adiciona 1 dado de Benefício',()=>{
  const c=STARTER_PRESETS.tendaar();
  c.derived.wounds.current=13;
  const r=resolveMedicineCheck(c,c,{medicalSupplies:false,medicalItemId:'medpac',boost:2,rng:()=>0});
  assert.equal(r.difficulty,5);
  assert.equal(r.medicalSupplies,true);
  assert.equal(r.medicalEquipment.itemId,'medpac');
  assert.equal(r.check.pool.boost,3);
});

test('Medpac de emergência remove penalização sem acrescentar dado de Benefício',()=>{
  const c=STARTER_PRESETS.tendaar();
  c.derived.wounds.current=13;
  const r=resolveMedicineCheck(c,c,{medicalSupplies:false,medicalItemId:'emergency_medpac',boost:2,rng:()=>0});
  assert.equal(r.difficulty,5);
  assert.equal(r.medicalSupplies,true);
  assert.equal(r.medicalEquipment.itemId,'emergency_medpac');
  assert.equal(r.check.pool.boost,2);
});

test('sem item médico, ausência de suprimentos mantém a penalização existente',()=>{
  const c=STARTER_PRESETS.tendaar();
  c.derived.wounds.current=13;
  const r=resolveMedicineCheck(c,c,{medicalSupplies:false,rng:()=>0});
  assert.equal(r.difficulty,6);
  assert.equal(r.medicalEquipment,null);
});

test('GameController aceita item médico explícito sem alterar a API legada',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'r29-medical'));
  game.loadStarterCharacter('tendaar');
  game.state.player.character.derived.wounds.current=3;
  const r=game.useMedicine({medicalSupplies:false,medicalItemId:'medpac',rng:()=>0});
  assert.equal(r.medicalEquipment.itemId,'medpac');
  assert.equal(r.check.pool.boost,1);
  assert.equal(r.difficulty,3);
});
