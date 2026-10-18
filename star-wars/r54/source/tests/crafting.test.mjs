import test from 'node:test';
import assert from 'node:assert/strict';
import {
  WEAPON_CRAFTING_TEMPLATES,
  ARMOR_CRAFTING_TEMPLATES,
  DROID_CRAFTING_TEMPLATES,
  craftItem
} from '../app/src/main/assets/vtt/js/rules/crafting.mjs';

test('Crafting: Templates básicos existem e são imutáveis', () => {
  assert.ok(WEAPON_CRAFTING_TEMPLATES.blaster_pistol);
  assert.ok(WEAPON_CRAFTING_TEMPLATES.blaster_rifle);
  assert.ok(ARMOR_CRAFTING_TEMPLATES.light_armor);
  assert.ok(ARMOR_CRAFTING_TEMPLATES.heavy_armor);
  assert.ok(DROID_CRAFTING_TEMPLATES.combat_droid);
  assert.ok(DROID_CRAFTING_TEMPLATES.astromech_droid);
});

test('Crafting: Criação de Pistola Blaster com Sucesso e Vantagem (+1 Dano)', () => {
  const char = { name: 'Engenheiro Kel', credits: 1000, weapons: [] };
  const res = craftItem(char, {
    templateType: 'weapon',
    templateId: 'blaster_pistol',
    rollResult: { successes: 2, advantage: 3, triumph: 0, threat: 0, despair: 0 }
  });

  assert.equal(res.success, true);
  assert.equal(res.creditsSpent, 150);
  assert.equal(res.character.credits, 850);
  assert.equal(res.item.damage, 7); // Base 6 + 1 (por 3 vantagens)
  assert.equal(res.character.weapons.length, 1);
});

test('Crafting: Criação de Armadura Pesada com Triunfo concede Defensiva 1', () => {
  const char = { name: 'Ferreiro Mandalorian', credits: 2000, armor: [] };
  const res = craftItem(char, {
    templateType: 'armor',
    templateId: 'heavy_armor',
    rollResult: { successes: 3, advantage: 1, triumph: 1, threat: 0, despair: 0 }
  });

  assert.equal(res.success, true);
  assert.ok(res.item.qualities.includes('Defensiva 1'));
  assert.equal(res.character.armor.length, 1);
});

test('Crafting: Falha no teste de Mecânica consome 50% dos créditos em sucata', () => {
  const char = { name: 'Aprendiz', credits: 500, weapons: [] };
  const res = craftItem(char, {
    templateType: 'weapon',
    templateId: 'blaster_rifle', // Custo 400
    rollResult: { successes: 0, advantage: 1, triumph: 0, threat: 2, despair: 0 }
  });

  assert.equal(res.success, false);
  assert.equal(res.salvagedCreditsLost, 200); // 50% de 400
  assert.equal(res.character.credits, 300);
  assert.equal(res.character.weapons.length, 0);
});

test('Crafting: Construção de Droide de Combate com sucesso', () => {
  const char = { name: 'Técnico Droid', credits: 5000, gear: [] };
  const res = craftItem(char, {
    templateType: 'droid',
    templateId: 'combat_droid',
    rollResult: { successes: 2, advantage: 2, triumph: 0, threat: 0, despair: 0 }
  });

  assert.equal(res.success, true);
  assert.equal(res.character.credits, 2500);
  assert.equal(res.item.category, 'droid');
  assert.equal(res.character.gear.length, 1);
});
