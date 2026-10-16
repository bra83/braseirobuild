import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('P11: tela de voz não contradiz integração TTS real',()=>{
 const src=fs.readFileSync(new URL('../shell-r46-run87/Templates.tsx',import.meta.url),'utf8');
 assert.match(src,/TTS integrado:/);
 assert.match(src,/múltiplos blocos/);
 assert.doesNotMatch(src,/TTS estão indisponíveis até uma integração real ser confirmada/);
});
