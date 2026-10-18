import test from 'node:test';
import assert from 'node:assert/strict';
import {
  HOLO_BIOMES,
  calculateRangeBand,
  detectBiomeFromContext,
  createInitialHolotableState,
  moveHolotableToken,
  calculateTacticalCheckModifiers
} from '../app/src/main/assets/vtt/js/rules/tacticalHolotable.mjs';

test('Tactical Holotable: detects appropriate biome from context keywords', () => {
  assert.strictEqual(detectBiomeFromContext('Hangar Principal da Estação', 'Nave atracada na baia 4'), 'hangar_station');
  assert.strictEqual(detectBiomeFromContext('Cantina de Mos Eisley', 'Música alta e rodadas de spotchka'), 'cantina_outpost');
  assert.strictEqual(detectBiomeFromContext('Subnível 1313', 'Passarelas úmidas e esgotos industriais'), 'urban_sublevel');
  assert.strictEqual(detectBiomeFromContext('Dunas do Mar de Areia', 'Desfiladeiro rochoso e calor sufocante'), 'desert_canyon');
  assert.strictEqual(detectBiomeFromContext('Bunker da Guarnição Imperial', 'Posto de segurança e portas blindadas'), 'imperial_base');
  assert.strictEqual(detectBiomeFromContext('Florestas de Kashyyyk', 'Árvores gigantescas e ruínas no pântano'), 'jungle_wilderness');
});

test('Tactical Holotable: computes range bands from coordinates', () => {
  assert.strictEqual(calculateRangeBand(50, 55, 50, 50), 'engaged');
  assert.strictEqual(calculateRangeBand(50, 80, 50, 50), 'short');
  assert.strictEqual(calculateRangeBand(50, 100, 50, 50), 'medium');
  assert.strictEqual(calculateRangeBand(10, 10, 50, 50), 'medium');
  assert.strictEqual(calculateRangeBand(0, 0, 100, 100), 'extreme');
});

test('Tactical Holotable: moves token and detects cover from biome props', () => {
  const state = createInitialHolotableState({
    biomeKey: 'cantina_outpost',
    character: { name: 'Dash' }
  });

  assert.strictEqual(state.playerToken.name, 'Dash');
  assert.strictEqual(state.playerToken.cover, 'none');

  // Move into booth_1 (cx: 22, cy: 62, r: 8 => cover: heavy)
  const movedIntoBooth = moveHolotableToken(state, 'player_hero', 22, 62);
  assert.strictEqual(movedIntoBooth.playerToken.cover, 'heavy');

  // Move to open space
  const movedOpen = moveHolotableToken(movedIntoBooth, 'player_hero', 50, 85);
  assert.strictEqual(movedOpen.playerToken.cover, 'none');
});

test('Tactical Holotable: calculates tactical check difficulty and setback dice from cover', () => {
  const state = createInitialHolotableState({
    biomeKey: 'hangar_station',
    character: { name: 'Valen' },
    adversaries: [
      { id: 'boba', name: 'Caçador Hostil', x: 28, y: 32, cover: 'heavy' }
    ]
  });

  const mods = calculateTacticalCheckModifiers(state, 'boba');
  assert.strictEqual(mods.targetId, 'boba');
  assert.strictEqual(mods.cover, 'heavy');
  assert.strictEqual(mods.setbackDice, 2); // Heavy cover adds 2 setback dice
  assert.ok(mods.difficulty >= 1);
});
