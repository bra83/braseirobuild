import test from 'node:test';
import assert from 'node:assert/strict';
import { CORE_WEAPON_CATALOG, CORE_ARMOR_REFERENCE, equipmentCatalog, weaponCatalogEntry } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

test('Pantalla fornece catálogo auditado de 31 armas e 7 blindagens de referência',()=>{
  assert.equal(CORE_WEAPON_CATALOG.length,31); assert.equal(CORE_ARMOR_REFERENCE.length,7);
  assert.equal(new Set(CORE_WEAPON_CATALOG.map(x=>x.id)).size,31); assert.equal(new Set(CORE_ARMOR_REFERENCE.map(x=>x.id)).size,7);
});

test('preços, raridade e restrição de exemplos centrais permanecem estáveis',()=>{
  const pistol=weaponCatalogEntry('blaster_pistol'); assert.deepEqual({price:pistol.price,rarity:pistol.rarity,damage:pistol.damage,crit:pistol.crit,range:pistol.range,restricted:pistol.restricted},{price:400,rarity:4,damage:6,crit:3,range:'Médio',restricted:false});
  const heavy=weaponCatalogEntry('heavy_repeating_blaster'); assert.equal(heavy.price,6000); assert.equal(heavy.rarity,8); assert.equal(heavy.restricted,true); assert.equal(heavy.damage,15);
  const detonator=weaponCatalogEntry('thermal_detonator'); assert.equal(detonator.price,2000); assert.equal(detonator.rarity,8); assert.equal(detonator.restricted,true);
});

test('armas corpo a corpo preservam dano por Fortaleza quando a tabela usa +N',()=>{
  assert.equal(weaponCatalogEntry('combat_knife').damageBonus,1); assert.equal(weaponCatalogEntry('vibroaxe').damageBonus,3); assert.equal(weaponCatalogEntry('lightsaber').damage,10);
});

test('catálogo retorna cópia e não permite mutar a autoridade em memória',()=>{
  const c=equipmentCatalog(); c.weapons[0].name='alterado'; assert.notEqual(CORE_WEAPON_CATALOG[0].name,'alterado');
});
