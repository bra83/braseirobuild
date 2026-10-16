import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
const root=process.cwd();
const native=readFileSync(join(root,'app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java'),'utf8');
const gemini=readFileSync(join(root,'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'),'utf8');
const web=readFileSync(join(root,'app/src/main/assets/vtt/js/ui/tts.mjs'),'utf8');
test('Android TTS divide texto longo e usa QUEUE_ADD depois do primeiro bloco',()=>{
  assert.match(native,/splitForSpeech/); assert.match(native,/getMaxSpeechInputLength/);
  assert.match(native,/QUEUE_FLUSH:TextToSpeech\.QUEUE_ADD/); assert.match(native,/queued_initializing/);
});
test('Gemini TTS tem executor dedicado, primeiro bloco curto e fila de reprodução',()=>{
  assert.match(gemini,/ttsExecutor = Executors\.newSingleThreadExecutor/);
  assert.match(gemini,/splitTtsTextStreaming\(text\)/); assert.match(gemini,/splitTtsText\(text,300\)/);
  assert.match(gemini,/ttsPlaybackQueue/); assert.match(gemini,/tts_chunk_ready/); assert.match(gemini,/prepareAsync\(\)/);
});
test('fallback Web Speech narra vários blocos em sequência',()=>{
  assert.match(web,/splitNarrationBlocks/); assert.match(web,/u\.onend=\(\)=>play\(index\+1\)/);
});
