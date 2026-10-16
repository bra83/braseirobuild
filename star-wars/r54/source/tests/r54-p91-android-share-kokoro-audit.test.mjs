import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = p => fs.readFileSync(new URL('../'+p, import.meta.url), 'utf8');
const manifest = read('app/src/main/AndroidManifest.xml');
const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const gemini = read('app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java');
const npc = read('app/src/main/java/com/braseiro/starwarsedge/NpcTokenBridge.java');
const share = read('app/src/main/java/com/braseiro/starwarsedge/ExternalImageShareState.java');
const tts = read('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');

const wrapper = read('gradle/wrapper/gradle-wrapper.properties');

test('P91 audit: ILUSTRAÇÃO chama método que existe no bridge Android e registra destino persistente',()=>{
  assert.match(runtime,/openGeminiScenePrompt\(prompt,sceneKey\)/);
  assert.match(gemini,/public String openGeminiScenePrompt\(String prompt, String sceneKey\)/);
  assert.match(gemini,/ExternalImageShareState\.setScene\(activity, cleanSceneKey\)/);
  assert.match(gemini,/boolean importSharedSceneImage\(Uri uri, String sceneKey\)/);
});

test('P91 audit: Star Wars é destino image share e recebe retorno em Activity singleTop',()=>{
  assert.match(manifest,/android:launchMode="singleTop"/);
  assert.match(manifest,/android\.intent\.action\.SEND/);
  assert.match(manifest,/android:mimeType="image\/\*"/);
  assert.match(main,/protected void onNewIntent\(Intent intent\)/);
  assert.match(main,/handleIncomingImageShare\(intent\)/);
  assert.match(main,/ExternalImageShareState\.get\(this\)/);
});

test('P91 audit: retorno compartilhado é roteado para cena ou NPC e destino sobrevive à recriação',()=>{
  assert.match(main,/geminiBridge\.importSharedSceneImage\(uri, pending\.targetId\)/);
  assert.match(main,/npcTokenBridge\.importSharedTokenImage\(uri, pending\.targetId\)/);
  assert.match(share,/getSharedPreferences\(PREFS, Context\.MODE_PRIVATE\)/);
  assert.match(share,/KIND_SCENE/);
  assert.match(share,/KIND_NPC/);
  assert.match(npc,/ExternalImageShareState\.setNpc\(activity, cleanNpcId\)/);
  assert.match(npc,/ExecutorService tokenExecutor/);
});

test('P91 audit: TTS migra pacote legado/DB Next-gen para Sherpa instalado sem fabricar vozes',()=>{
  assert.match(tts,/LEGACY_BRASEIRO_ENGINE = "com\.braseiro\.kokoro\.ptbr"/);
  assert.match(tts,/SHERPA_ENGINE = "com\.k2fsa\.sherpa\.onnx\.tts\.engine"/);
  assert.match(tts,/SHERPA_ENGINE_DEBUG = "com\.k2fsa\.sherpa\.onnx\.tts\.engine\.debug"/);
  assert.match(tts,/migrateLegacyEnginePreference\(savedEngine\)/);
  assert.match(tts,/lower\.contains\("db next-gen"\)/);
  assert.match(tts,/engine\.getVoices\(\)/);
  assert.doesNotMatch(tts,/pf_dora|pf_dora2|pm_santa|fabricatedVoice/i);
});

test('P91 audit: Sherpa aparece como Braseiro Kokoro PT-BR neural e mantém fluxo neural encadeado',()=>{
  assert.match(tts,/BRASEIRO_KOKORO_LABEL = "Braseiro Kokoro PT-BR · neural"/);
  assert.match(tts,/maxNumSentences", 2/);
  assert.match(tts,/silenceScale", 0\.06f/);
  assert.match(tts,/numThreads", 4/);
  assert.match(tts,/generateWithConfigAndCallback/);
  assert.match(tts,/speakNextNeural\(session, total\)/);
});


test('P91 audit: wrapper Android não depende do host GitHub para obter Gradle',()=>{
  assert.match(wrapper,/downloads\.gradle\.org\/distributions\/gradle-8\.13-bin\.zip/);
  assert.doesNotMatch(wrapper,/github\.com/);
});
