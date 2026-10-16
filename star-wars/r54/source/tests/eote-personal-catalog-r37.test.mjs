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

test('EotE Core printed-page citations resolve each ship-transfer gear profile to its actual printed entry',()=>{
  for(const [id,page] of [['electrobinoculars',175],['multi_scanner',175],['hand_scanner',176],['medpac',177],['breath_mask',179],['rations',179],['glow_rod',181],['tool_kit',181]]){
    const item=generalGearCatalogEntry(id);
    assert.equal(item.source.book,'Edge of the Empire Core Rulebook',id);
    assert.equal(item.source.printedPage,page,id);
    assert.equal(item.source.authority,'PRIMARY_RULE_SOURCE',id);
    assert.equal(item.source.rulesProfile,'EOTE_CORE',id);
  }
  assert.equal(generalGearCatalogEntry('medpac').effectSource.printedPage,112);
  assert.equal(generalGearCatalogEntry('medpac').effectSource.section,'Medicine');
});
