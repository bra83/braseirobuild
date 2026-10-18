import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EMOTIONAL_DISPOSITIONS,
  findEmotionalDisposition,
  normalizeMoralityState,
  setMoralityScore,
  addConflict,
  setSessionMoralityTrigger,
  triggerGroupMorality,
  resolveEndOfSessionMorality
} from '../app/src/main/assets/vtt/js/rules/morality.mjs';

test('F&D Morality: Tabela de Disposições Emocionais cobre 1 a 100 sem lacunas', () => {
  assert.equal(EMOTIONAL_DISPOSITIONS.length, 10);
  for (let r = 1; r <= 100; r++) {
    const disp = findEmotionalDisposition(r);
    assert.ok(disp.strength, `Disp para ${r} deve ter Força Emocional`);
    assert.ok(disp.weakness, `Disp para ${r} deve ter Fraqueza Emocional`);
    assert.ok(r >= disp.min && r <= disp.max);
  }
});

test('F&D Morality: Estado inicial padrão e normalização', () => {
  const char = { id: 'jedi-1', name: 'Kaelen' };
  const m = normalizeMoralityState(char);
  assert.equal(m.score, 50);
  assert.equal(m.conflict, 0);
  assert.equal(m.isLightParagon, false);
  assert.equal(m.isDarkSide, false);
  assert.equal(m.side, 'neutral');
});

test('F&D Morality: Acúmulo de Conflito e histórico', () => {
  let char = { id: 'jedi-1', name: 'Kaelen', morality: { score: 50, conflict: 0 } };
  char = addConflict(char, 3, 'Usou o lado sombrio para abrir a porta');
  char = addConflict(char, 2, 'Executou prisioneiro');

  const m = normalizeMoralityState(char);
  assert.equal(m.conflict, 5);
  assert.equal(m.history.length, 2);
  assert.equal(m.history[0].amount, 3);
  assert.equal(m.history[1].amount, 2);
});

test('F&D Morality: Resolução de fim de sessão (vitória da Luz, aumento de Moralidade)', () => {
  let char = { id: 'jedi-1', morality: { score: 50, conflict: 3 } };
  // Roll 8 contra 3 Conflito -> delta = +5 -> score = 55
  const res = resolveEndOfSessionMorality(char, { roll: 8 });
  assert.equal(res.delta, 5);
  assert.equal(res.newScore, 55);
  assert.equal(res.character.morality.conflict, 0);
});

test('F&D Morality: Resolução de fim de sessão (queda moral, redução de Moralidade)', () => {
  let char = { id: 'jedi-1', morality: { score: 50, conflict: 8 } };
  // Roll 3 contra 8 Conflito -> delta = -5 -> score = 45
  const res = resolveEndOfSessionMorality(char, { roll: 3 });
  assert.equal(res.delta, -5);
  assert.equal(res.newScore, 45);
  assert.equal(res.character.morality.conflict, 0);
});

test('F&D Morality: Gatilho de sessão dobra ganhos e perdas morais', () => {
  let char = { id: 'jedi-1', morality: { score: 50, conflict: 4, triggeredThisSession: true } };
  // Roll 7 contra 4 Conflito -> base 3 -> dobrado = +6 -> score = 56
  const res = resolveEndOfSessionMorality(char, { roll: 7 });
  assert.equal(res.delta, 6);
  assert.equal(res.newScore, 56);
});

test('F&D Morality: Patamares de Paragom da Luz e Usuário do Lado Sombrio', () => {
  let lightChar = setMoralityScore({ id: 'light-1' }, 75);
  let normLight = normalizeMoralityState(lightChar);
  assert.equal(normLight.isLightParagon, true);
  assert.equal(normLight.isDarkSide, false);
  assert.equal(normLight.side, 'light_paragon');
  assert.equal(normLight.strainModifier, 1);

  let masterLight = setMoralityScore({ id: 'light-2' }, 85);
  let normMaster = normalizeMoralityState(masterLight);
  assert.equal(normMaster.side, 'light_master');
  assert.equal(normMaster.strainModifier, 2);

  let darkChar = setMoralityScore({ id: 'dark-1' }, 25);
  let normDark = normalizeMoralityState(darkChar);
  assert.equal(normDark.isLightParagon, false);
  assert.equal(normDark.isDarkSide, true);
  assert.equal(normDark.side, 'dark_user');
  assert.equal(normDark.woundModifier, 1);
  assert.equal(normDark.strainModifier, -1);
});

test('F&D Morality: Persistência contra 3x ciclos de serialização JSON', () => {
  let char = { id: 'jedi-pers', morality: { score: 68, conflict: 5, triggeredThisSession: false } };
  char = addConflict(char, 2, 'Tentação sombria');
  for (let cycle = 1; cycle <= 3; cycle++) {
    const raw = JSON.stringify(char);
    char = JSON.parse(raw);
    const m = normalizeMoralityState(char);
    assert.equal(m.score, 68);
    assert.equal(m.conflict, 7);
  }
});
