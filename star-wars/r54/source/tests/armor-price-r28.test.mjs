import test from 'node:test';
import assert from 'node:assert/strict';
import { CORE_ARMOR_REFERENCE, equipmentCatalog } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

const byId=id=>CORE_ARMOR_REFERENCE.find(x=>x.id===id);

test('Tabla 5-2 da Caja de Inicio prova preços de quatro blindagens sobrepostas',()=>{
  assert.equal(byId('heavy_clothing').price,50);
  assert.equal(byId('padded_armor').price,500);
  assert.equal(byId('armored_clothing').price,1000);
  assert.equal(byId('heavy_battle_armor').price,5000);
});

test('preço da Caja de Inicio não substitui a fonte de Defesa/Proteção da Pantalla',()=>{
  for(const id of ['heavy_clothing','padded_armor','armored_clothing','heavy_battle_armor']){
    const item=byId(id);
    assert.equal(item.source.book,'La Era de la Rebelión - Pantalla');
    assert.equal(item.priceSource.book,'La Era de la Rebelión - Libro de reglas');
    assert.equal(item.priceSource.pdfPage,35);
    assert.equal(item.priceSource.section,'Tabla 5-2: Blindajes');
  }
});

test('blindagens sem preço comprovado nesta tabela permanecem sem preço',()=>{
  for(const id of ['laminate_armor','hostile_environment_gear','personal_deflector_shield']) assert.equal(byId(id).price,undefined);
  const c=equipmentCatalog(); c.armor.find(x=>x.id==='padded_armor').price=1;
  assert.equal(byId('padded_armor').price,500);
});
