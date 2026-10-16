import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js', import.meta.url),'utf8');

test('Gemini TTS failure falls through to Android/Web instead of silencing narration',()=>{
  assert.match(runtime,/if\(r&&r\.ok\)return r;\}catch\{\}/);
  assert.doesNotMatch(runtime,/if\(r&&r\.ok\)return r;return r\|\|\{ok:false,message:'Gemini TTS indisponível\.'/);
  const gemini=runtime.indexOf("prefs.ttsProvider==='gemini'");
  const android=runtime.indexOf('const b=window.BraseiroTts',gemini);
  const web=runtime.indexOf("'speechSynthesis' in window",android);
  assert.ok(gemini>=0 && android>gemini && web>android);
});

test('Gemini TTS remains multiblock and Charon-selectable',()=>{
  const java=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url),'utf8');
  assert.match(java,/splitTtsTextStreaming\(text\)/);
  assert.match(java,/splitTtsText\(text,300\)/);
  assert.match(java,/splitTtsText\(rest\.toString\(\),850\)/);
  assert.match(runtime,/<option>Charon<\/option>/);
});
