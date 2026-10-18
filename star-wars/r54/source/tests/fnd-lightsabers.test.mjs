import test from 'node:test';
import assert from 'node:assert/strict';
import {
  LIGHTSABER_CHASSIS_CATALOG,
  KYBER_CRYSTAL_CATALOG,
  LIGHTSABER_HILT_ATTACHMENTS,
  createLightsaber
} from '../app/src/main/assets/vtt/js/rules/lightsabers.mjs';

test('F&D Lightsabers: Catálogo básico de chassis e cristais', () => {
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.basic_lightsaber);
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.double_bladed_lightsaber);
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.shoto_lightsaber);
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.guard_shoto);
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.temple_guard_pike);
  assert.ok(LIGHTSABER_CHASSIS_CATALOG.training_lightsaber);

  assert.ok(KYBER_CRYSTAL_CATALOG.ilum_crystal);
  assert.ok(KYBER_CRYSTAL_CATALOG.dantooine_crystal);
  assert.ok(KYBER_CRYSTAL_CATALOG.lorrdian_gemstone);
  assert.ok(KYBER_CRYSTAL_CATALOG.krayt_dragon_pearl);
  assert.ok(KYBER_CRYSTAL_CATALOG.barab_ingot);
  assert.ok(KYBER_CRYSTAL_CATALOG.solari_crystal);
});

test('F&D Lightsabers: Sabre de luz básico com Cristal de Ilum padrão', () => {
  const saber = createLightsaber('basic_lightsaber');
  assert.equal(saber.damage, 6);
  assert.equal(saber.critical, 2);
  assert.ok(saber.qualities.includes('Ruptura 1'));
  assert.ok(saber.qualities.includes('Destroçar'));
  assert.equal(saber.hardPointsRemaining, 0); // 2 HP base - 2 HP cristal
});

test('F&D Lightsabers: Sabre com modificações instaladas no Cristal Kyber', () => {
  // Cristal de Ilum com +2 de Dano e -1 Crítico
  const saber = createLightsaber('basic_lightsaber', {
    crystalId: 'ilum_crystal',
    installedMods: ['damage_1', 'damage_1', 'crit_1']
  });
  assert.equal(saber.damage, 8); // 6 + 1 + 1
  assert.equal(saber.critical, 1); // 2 - 1
});

test('F&D Lightsabers: Pérola de Dragão Krayt (alto dano e crítico brutal)', () => {
  const saber = createLightsaber('basic_lightsaber', {
    crystalId: 'krayt_dragon_pearl'
  });
  assert.equal(saber.damage, 9);
  assert.equal(saber.critical, 1);
  assert.ok(saber.qualities.includes('Cruel 1'));
});

test('F&D Lightsabers: Sabre de lâmina dupla e fixação de pulso bifurcado subaquático', () => {
  const doubleSaber = createLightsaber('double_bladed_lightsaber', {
    crystalId: 'ilum_crystal',
    attachments: ['bifurcating_pulse']
  });
  assert.equal(doubleSaber.worksUnderwater, true);
  assert.ok(doubleSaber.qualities.includes('Desajeitada 3'));
  assert.ok(doubleSaber.qualities.includes('Acoplada 1'));
  assert.equal(doubleSaber.hardPointsTotal, 4);
  assert.equal(doubleSaber.hardPointsRemaining, 1); // 4 - 2 (cristal) - 1 (pulso) = 1
});

test('F&D Lightsabers: Sabre de treino não causa dano letal', () => {
  const training = createLightsaber('training_lightsaber');
  assert.equal(training.damage, 6);
  assert.equal(training.critical, null);
  assert.ok(training.qualities.includes('Dano Atordoante'));
});
