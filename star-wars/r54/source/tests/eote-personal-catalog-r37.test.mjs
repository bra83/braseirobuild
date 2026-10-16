import test from 'node:test';
import assert from 'node:assert/strict';
import { CORE_WEAPON_CATALOG, EOTE_CORE_ARMOR_CATALOG, EOTE_CORE_GENERAL_GEAR, generalGearCatalogEntry } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

test('R37: catálogo pessoal inclui os 2 itens oficiais SWE09/EotE (62 itens) com Appraiser\'s Eye fundamentado em Fly Casual',()=>{
  assert.equal(CORE_WEAPON_CATALOG.length,31);
  assert.equal(EOTE_CORE_ARMOR_CATALOG.length,7);
  assert.equal(EOTE_CORE_GENERAL_GEAR.length,24);
  assert.equal(CORE_WEAPON_CATALOG.length + EOTE_CORE_ARMOR_CATALOG.length + EOTE_CORE_GENERAL_GEAR.length,62);
  const item=generalGearCatalogEntry('appraisers_eye');
  assert.equal(item.price,700); assert.equal(item.rarity,7); assert.equal(item.encumbrance,0);
  assert.equal(item.source.book,'Fly Casual'); assert.equal(item.source.section,'Table 2-4: Gear');
  assert.equal(item.source.authority,'PRIMARY_RULE_SOURCE'); assert.equal(item.source.rulesProfile,'EOTE_CORE');
});
