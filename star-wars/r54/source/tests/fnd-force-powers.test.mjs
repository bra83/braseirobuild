import test from 'node:test';
import assert from 'node:assert/strict';
import { FND_FORCE_POWERS } from '../app/src/main/assets/vtt/js/rules/fndForcePowers.mjs';

test('F&D Force Powers: Presença de todos os 6 poderes clássicos de Force and Destiny', () => {
  const keys = Object.keys(FND_FORCE_POWERS);
  assert.ok(keys.includes('enhance'), 'Enhance presente');
  assert.ok(keys.includes('heal_harm'), 'Heal/Harm presente');
  assert.ok(keys.includes('bind'), 'Bind presente');
  assert.ok(keys.includes('misdirect'), 'Misdirect presente');
  assert.ok(keys.includes('protect_unleash'), 'Protect/Unleash presente');
  assert.ok(keys.includes('seek'), 'Seek presente');
});

test('F&D Force Powers: Estrutura e upgrades de Enhance (Salto da Força, Aprimorar Brawn/Agility)', () => {
  const enhance = FND_FORCE_POWERS.enhance;
  assert.equal(enhance.baseCost, 10);
  assert.ok(enhance.upgrades.length >= 4);
  const jumpH = enhance.upgrades.find(u => u.id === 'control_jump_horizontal');
  const jumpV = enhance.upgrades.find(u => u.id === 'control_jump_vertical');
  const brawn = enhance.upgrades.find(u => u.id === 'control_brawn');
  assert.ok(jumpH);
  assert.ok(jumpV);
  assert.ok(brawn);
  assert.ok(jumpV.parents.includes('control_jump_horizontal'));
});

test('F&D Force Powers: Estrutura e upgrades de Heal / Harm', () => {
  const healHarm = FND_FORCE_POWERS.heal_harm;
  assert.equal(healHarm.baseCost, 10);
  const revive = healHarm.upgrades.find(u => u.id === 'control_revive');
  assert.ok(revive);
  assert.equal(revive.cost, 15);
});

test('F&D Force Powers: Estrutura e upgrades de Bind (Sufocamento da Força)', () => {
  const bind = FND_FORCE_POWERS.bind;
  assert.equal(bind.baseCost, 10);
  const suffocate = bind.upgrades.find(u => u.id === 'control_suffocate');
  assert.ok(suffocate);
  assert.equal(suffocate.type, 'control');
});

test('F&D Force Powers: Estrutura e upgrades de Protect / Unleash (Tempestade de Raios)', () => {
  const pu = FND_FORCE_POWERS.protect_unleash;
  assert.equal(pu.baseCost, 15);
  const storm = pu.upgrades.find(u => u.id === 'control_storm');
  assert.ok(storm);
  assert.equal(storm.cost, 20);
});
