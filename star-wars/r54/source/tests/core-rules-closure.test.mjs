import test from 'node:test';
import assert from 'node:assert/strict';
import { weaponHasQuality, weaponQualityRank, weaponAttackModifiers, resolveWeaponHit } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { calculateArmorValues } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';
import { applyAdversaryDamage } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { applyPlayerCondition, playerConditionModifiers, extinguishFireCheck, escapeEnsnareCheck } from '../app/src/main/assets/vtt/js/rules/conditions.mjs';
import { createVehicleFromProfile, applyJamming, vehicleTemporaryEffects } from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';

test('EQUIP.QUALITIES: Cortosis torna armadura imune a Pierce e arma imune a Sunder', () => {
  const adversary = {
    key: 'test_adversary',
    name: 'Guarda com Cortosis',
    kind: 'rival',
    soak: 5,
    wounds: { current: 0, threshold: 15 },
    cortosis: true
  };
  const result = applyAdversaryDamage(adversary, { damage: 8, pierce: 3 });
  assert.equal(result.effectiveSoak, 5, 'Cortosis preserva o soak total contra Pierce');
  assert.equal(result.suffered, 3, 'Dano sofrido é 8 - 5 = 3');
});

test('EQUIP.QUALITIES: Superior adiciona +1 dano base e 1 Advantage na arma', () => {
  const weapon = {
    id: 'superior_blaster',
    name: 'Pistola Superior',
    damage: 6,
    crit: 3,
    qualities: ['Superior']
  };
  const check = { success: true, netSuccess: 1, netAdvantage: 0 };
  const hit = resolveWeaponHit(weapon, check);
  assert.equal(hit.damage, 8, 'Dano é 6 (base) + 1 (Superior) + 1 (netSuccess) = 8');
  assert.equal(hit.superior, true);
});

test('EQUIP.QUALITIES: Superior na armadura concede +1 soak e -1 encumbrance', () => {
  const armor = {
    id: 'superior_laminate',
    name: 'Armadura Laminada Superior',
    soak: 2,
    defense: 1,
    encumbrance: 4,
    qualities: ['Superior']
  };
  const values = calculateArmorValues(armor, { worn: true });
  assert.equal(values.soak, 3, 'Soak aumentado em +1');
  assert.equal(values.encumbrance, 3, 'Encumbrance reduzido em 1');
});

test('EQUIP.QUALITIES: Inferior na arma reduz dano base e adiciona Threat', () => {
  const weapon = {
    id: 'inferior_blaster',
    name: 'Pistola Inferior',
    damage: 6,
    crit: 3,
    qualities: ['Inferior']
  };
  const check = { success: true, netSuccess: 1, netAdvantage: 1 };
  const hit = resolveWeaponHit(weapon, check);
  assert.equal(hit.damage, 6, 'Dano base 6 - 1 + 1 netSuccess = 6');
  assert.equal(hit.inferior, true);
});

test('EQUIP.QUALITIES: Inferior na armadura aumenta encumbrance e reduz defense/soak', () => {
  const armor = {
    id: 'inferior_armor',
    name: 'Armadura Inferior',
    soak: 2,
    defense: 1,
    encumbrance: 3,
    qualities: ['Inferior']
  };
  const values = calculateArmorValues(armor, { worn: true });
  assert.equal(values.defense.ranged, 0, 'Defense reduzida em 1');
  assert.equal(values.encumbrance, 4, 'Encumbrance aumentado em 1');
});

test('EQUIP.QUALITIES: Burn e Ensnare possuem verificações determinísticas de remoção', () => {
  const hardFire = extinguishFireCheck({ surface: 'hard' });
  assert.equal(hardFire.difficulty, 2, 'Apagar fogo em superfície dura é Médio (2)');
  const waterFire = extinguishFireCheck({ hasWater: true });
  assert.equal(waterFire.autoExtinguish, true, 'Água apaga fogo comum imediatamente');
  const ensnare = escapeEnsnareCheck();
  assert.equal(ensnare.difficulty, 3, 'Escapar de Ensnare é Difícil (3)');
  assert.equal(ensnare.skill, 'atletismo');
});

test('VEHICLE.ACTIONS: Jamming bloqueia comunicações e sensores temporariamente', () => {
  const vehicle = createVehicleFromProfile('x34');
  const jammed = applyJamming(vehicle, { targetVehicleId: 'enemy_1', currentRound: 1, durationRounds: 2 });
  const effRound1 = vehicleTemporaryEffects(jammed, { currentRound: 1 });
  assert.ok(effRound1.jamming, 'Jamming ativo no assalto 1');
  assert.equal(effRound1.jamming.targetVehicleId, 'enemy_1');
  const effRound3 = vehicleTemporaryEffects(jammed, { currentRound: 3 });
  assert.equal(effRound3.jamming, null, 'Jamming expira após duração configurada');
});
