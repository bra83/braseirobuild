import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { MASTER_NARRATION_MAX_CHARS, validateMasterProposal } from '../app/src/main/assets/vtt/js/master/proposal.mjs';

const gemini = readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url), 'utf8');

test('Gemini Interactions schema usa apenas limites estruturais suportados e não envia maxLength', () => {
  assert.doesNotMatch(gemini, /maxLength/);
  assert.match(gemini, /response_format/);
  assert.match(gemini, /mime_type", "application\/json"/);
  assert.match(gemini, /\.put\("maxItems", 6\)/);
  assert.match(gemini, /Motor Barbara valida o limite localmente/);
});

test('limites textuais removidos do schema remoto continuam obrigatórios no Motor Barbara', () => {
  assert.equal(MASTER_NARRATION_MAX_CHARS, 12000);
  assert.doesNotThrow(() => validateMasterProposal({ narration: 'x'.repeat(9000) }));
  assert.throws(() => validateMasterProposal({ narration: 'x'.repeat(12001) }), /12000 caracteres/i);
  assert.throws(() => validateMasterProposal({ askPlayer: 'x'.repeat(501) }), /500 caracteres/i);
  assert.throws(() => validateMasterProposal({ suggestedActions: ['x'.repeat(181)] }), /180 caracteres/i);
});

test('quantidade máxima de ações sugeridas continua protegida local e remotamente', () => {
  assert.throws(() => validateMasterProposal({ suggestedActions: Array.from({ length: 7 }, (_, i) => `Ação ${i}`) }), /no máximo 6 opções/i);
  const accepted = validateMasterProposal({ suggestedActions: Array.from({ length: 6 }, (_, i) => `Ação ${i}`) });
  assert.equal(accepted.suggestedActions.length, 6);
});
