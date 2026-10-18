import test from 'node:test';
import assert from 'node:assert/strict';
import { NarrativeGuardService, generateStarWarsImmersiveFallback } from '../app/src/main/assets/vtt/js/master/narrativeGuard.mjs';
import { validateMasterProposal } from '../app/src/main/assets/vtt/js/master/proposal.mjs';

test('NarrativeGuardService: sanitizes AI meta speech and repetitive crutches', () => {
  const dirty = `Local atual: Coruscant subnível 1313\nStatus: Alerta\nA cena responde ao gesto do caçador de recompensas. O sistema calculou a distância. Uma sombra surge entre os contêineres e um blaster é destravado. O que você faz?`;
  const clean = NarrativeGuardService.sanitize(dirty);

  assert.ok(!clean.includes('Local atual:'));
  assert.ok(!clean.includes('Status:'));
  assert.ok(!clean.includes('A cena responde'));
  assert.ok(!clean.includes('O sistema calculou'));
  assert.ok(!clean.includes('O que você faz?'));
  assert.ok(clean.includes('Uma sombra surge entre os contêineres e um blaster é destravado.'));
});

test('NarrativeGuardService: validates narrative 5-pillar rules', () => {
  const bad = 'Como uma inteligência artificial, você sente medo da patrulha imperial. O que você faz?';
  const valBad = NarrativeGuardService.validate(bad);
  assert.strictEqual(valBad.valid, false);
  assert.ok(valBad.errors.length >= 2);

  const good = 'O vapor acre dos respiradouros envolve o cais de atracação. Theron ajusta o coldre de sua pistola blaster enquanto passos pesados de soldados imperiais ecoam pelo piso metálico.';
  const valGood = NarrativeGuardService.validate(good);
  assert.strictEqual(valGood.valid, true);
  assert.strictEqual(valGood.errors.length, 0);
});

test('NarrativeGuardService: integrated in validateMasterProposal', () => {
  const proposal = validateMasterProposal({
    narration: 'A cena responde com rapidez. Um droide de protocolo avança pela fumaça da cantina. O que você faz?'
  });
  assert.ok(!proposal.narration.includes('A cena responde'));
  assert.ok(!proposal.narration.includes('O que você faz?'));
  assert.ok(proposal.narration.includes('Um droide de protocolo avança pela fumaça da cantina.'));
});

test('generateStarWarsImmersiveFallback: produces rich sensory atmosphere and character anchoring', () => {
  const character = {
    name: 'Kaelen',
    species: 'Twi\'lek',
    armor: [{ name: 'Armadura laminada pesada' }],
    weapons: [{ name: 'Fuzil Blaster pesado' }]
  };
  const location = { name: 'Porto Espacial de Nar Shaddaa' };
  const fallback = generateStarWarsImmersiveFallback({ character, location });

  assert.ok(fallback.includes('Porto Espacial de Nar Shaddaa'));
  assert.ok(fallback.includes('Kaelen'));
  assert.ok(fallback.includes('Twi\'lek'));
  assert.ok(fallback.includes('Armadura laminada pesada'));
  assert.ok(fallback.includes('Fuzil Blaster pesado'));
  assert.ok(fallback.includes('ozônio'));
  assert.ok(!fallback.includes('O que você faz?'));
});
