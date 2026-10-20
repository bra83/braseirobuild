import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const bridge=readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java',import.meta.url),'utf8');
const tts=readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java',import.meta.url),'utf8');
test('Gemini 3.8 uses structured Interactions with hidden voice metadata',()=>{
  assert.match(bridge,/model\.startsWith\("gemini-3\.8-"\)/);
  assert.match(bridge,/"speech_metadata"/);
  assert.match(bridge,/"user_input"/);
  assert.match(bridge,/ttsVoiceStyle/);
  assert.match(bridge,/configureTtsVoiceStyle/);
  assert.doesNotMatch(bridge,/Read in a deep voice:/i);
});
test('voice transcript remains separate from directions and refuses truncation',()=>{
  assert.match(bridge,/put\("text",text\)/);
  assert.match(bridge,/if \(text\.length\(\) > 12000\) return fail/);
  assert.match(bridge,/!Character\.isWhitespace\(remaining\.charAt\(cut\)\)/);
  assert.match(bridge,/ttsSessionId\+\+/);
});
test('installed Kokoro and Android TTS remain supported',()=>{
  assert.match(tts,/com\.k2fsa\.sherpa\.onnx\.tts\.engine/);
  assert.match(tts,/onDone/);
  assert.match(tts,/selectEngine/);
});
