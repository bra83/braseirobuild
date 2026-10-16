import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=p=>fs.readFileSync(new URL('../'+p,import.meta.url),'utf8');
const bridge=read('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
const chunker=read('app/src/main/java/com/braseiro/starwarsedge/TtsChunker.java');
const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const manifest=read('app/src/main/AndroidManifest.xml');

test('P76: catálogo Android lista motores/vozes e permite seleção persistente',()=>{
  assert.match(bridge,/getCatalog\(\)/);
  assert.match(bridge,/getEngines\(\)/);
  assert.match(bridge,/getVoices\(\)/);
  assert.match(bridge,/selectEngine\(String rawPackage\)/);
  assert.match(bridge,/selectVoice\(String rawVoiceName\)/);
  assert.match(bridge,/PREF_ENGINE/);
  assert.match(bridge,/PREF_VOICE/);
  assert.match(manifest,/android\.intent\.action\.TTS_SERVICE/);
  assert.match(runtime,/final-tts-engine/);
  assert.match(runtime,/final-tts-voice/);
});

test('P76: perfil neural usa 250-650 com alvo 450 e não divide cada sentença',()=>{
  assert.match(chunker,/NEURAL_TARGET\s*=\s*450/);
  assert.match(chunker,/NEURAL_MIN\s*=\s*250/);
  assert.match(chunker,/NEURAL_MAX\s*=\s*650/);
  assert.match(chunker,/char\[\]\[\] groups = new char\[\]\[\]\{\{'.', '!', '\?'\}, \{';'\}, \{','\}\}/);
  assert.ok(runtime.includes('A porta range. Há marcas no chão. Algo se move no corredor.'));
});

test('P76: Kokoro avança apenas por onDone sem flush/stop entre blocos',()=>{
  const start=bridge.indexOf('private void speakNextNeural');
  const end=bridge.indexOf('private Bundle neuralParams',start);
  const next=bridge.slice(start,end);
  assert.match(next,/TextToSpeech\.QUEUE_ADD/);
  assert.doesNotMatch(next,/QUEUE_FLUSH/);
  assert.doesNotMatch(next,/\.stop\(\)/);
  assert.match(bridge,/onDone\(String utteranceId\)/);
  assert.match(bridge,/speakNextNeural\(session, total\)/);
  assert.doesNotMatch(bridge,/postDelayed/);
  assert.doesNotMatch(bridge,/watchdog/i);
});

test('P76: contrato Sherpa/Kokoro expõe parâmetros e prewarm/streaming',()=>{
  assert.match(bridge,/putInt\("maxNumSentences", 2\)/);
  assert.match(bridge,/putFloat\("silenceScale", 0\.06f\)/);
  assert.match(bridge,/putInt\("numThreads", 4\)/);
  assert.match(bridge,/putInt\("leadingSilenceMs", 50\)/);
  assert.match(bridge,/putInt\("streamingAudioChunkMs", 125\)/);
  assert.match(bridge,/generateWithConfigAndCallback/);
  assert.match(bridge,/SynthesisCallback\.audioAvailable/);
  assert.match(bridge,/com\.k2fsa\.sherpa\.onnx\.tts\.engine/);
  assert.match(bridge,/synthesizeToFile\("Olá\."/);
  assert.match(runtime,/maybePrewarmNativeTtsForScene/);
});

test('P76: velocidade Kokoro é aplicada uma única vez no parâmetro da engine',()=>{
  const start=bridge.indexOf('private void startNeuralSequence');
  const end=bridge.indexOf('private void speakNextNeural',start);
  const neuralStart=bridge.slice(start,end);
  assert.match(neuralStart,/setSpeechRate\(1\.0f\)/);
  assert.doesNotMatch(neuralStart,/setSpeechRate\(rate\)/);
  assert.match(bridge,/putFloat\("braseiro\.kokoro\.speed", rate\)/);
});

test('P76: Samsung/Google continuam no fluxo Android padrão',()=>{
  const start=bridge.indexOf('private void queueAndroidSpeech');
  const end=bridge.indexOf('private void startNeuralSequence',start);
  const normal=bridge.slice(start,end);
  assert.match(normal,/local\.stop\(\)/);
  assert.match(normal,/setSpeechRate\(rate\)/);
  assert.match(normal,/QUEUE_FLUSH/);
  assert.match(normal,/QUEUE_ADD/);
  assert.match(bridge,/detectProfile/);
});
