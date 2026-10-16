import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../shell-r46-run87/Templates.tsx', import.meta.url), 'utf8');
const bundleRaw = fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/r46-shell.bundle.js', import.meta.url), 'utf8');
const bundle = bundleRaw.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(Number.parseInt(hex, 16)));
const stale = 'OCR, IA/Gemini e TTS continuam sem homologação neste ambiente';
const current = 'Integrações de código Barbara/Gemini/TTS e fallbacks estão ativas e cobertas por testes.';
const physical = 'áudio/latência Gemini/Charon, OCR e validação dos bridges nativos';

test('P18: fonte e bundle não apresentam Gemini/TTS implementados como indisponíveis', () => {
  for (const text of [src, bundle]) {
    assert.equal(text.includes(stale), false);
    assert.equal(text.includes(current), true);
    assert.equal(text.includes(physical), true);
  }
});
