import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { MASTER_NARRATION_MAX_CHARS, validateMasterProposal } from '../app/src/main/assets/vtt/js/master/proposal.mjs';

const gemini = readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url), 'utf8');

test('P20: contrato do Mestre aceita a narração longa pedida ao Gemini', () => {
  const longNarration = Array.from({ length: 1100 }, (_, i) => `p${i}`).join(' ');
  assert.ok(longNarration.length > 5000, 'fixture deve reproduzir o conflito histórico de 5k');
  assert.ok(longNarration.length <= MASTER_NARRATION_MAX_CHARS, 'fixture deve caber no novo teto');
  const proposal = validateMasterProposal({ narration: longNarration, askPlayer: 'O que você faz?', suggestedActions: [], sourceRuleIds: [] });
  assert.equal(proposal.narration, longNarration);
});

test('P20: host nativo e Motor Barbara usam o mesmo teto de 12k', () => {
  assert.equal(MASTER_NARRATION_MAX_CHARS, 12000);
  assert.match(gemini, /até 12000 caracteres; o Motor Barbara valida o limite localmente/);
  assert.doesNotMatch(gemini, /Narração em português do Brasil, até 5000 caracteres/);
});
