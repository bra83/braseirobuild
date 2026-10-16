import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(path, 'utf8');
const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const npcBridge = read('app/src/main/java/com/braseiro/starwarsedge/NpcTokenBridge.java');
const diceUi = read('app/src/main/assets/vtt/r54-device-p6.js');

test('R80/R82: ilustração da cena abre Gemini externo; retorno é compartilhado ao app', () => {
  assert.match(runtime, /openGeminiScenePrompt/);
  assert.match(runtime, /window\.__r54SceneImagePendingKey=sceneKey/);
  assert.match(runtime, /compartilhe com o app Star Wars/i);
  assert.doesNotMatch(runtime, /safeParse\(g\.generateSceneImage\(prompt,sceneKey\)/);
});

test('R80: host Android expõe bridge persistente de tokens de NPC', () => {
  assert.match(main, /new NpcTokenBridge\(this, webView\)/);
  assert.match(main, /addJavascriptInterface\(npcTokenBridge\.javascriptInterface\(\), "BraseiroNpcTokens"\)/);
  assert.match(main, /npcTokenBridge\.handleActivityResult/);
  assert.match(main, /npcTokenBridge\.openTokenResponse/);
  assert.match(npcBridge, /openGeminiTokenPrompt\(/);
  assert.match(npcBridge, /pickTokenImage\(/);
  assert.match(npcBridge, /getTokenStatus\(/);
  assert.match(npcBridge, /window\.braseiroNpcTokenNativeEvent/);
});

test('R80: dados narrativos mostram símbolos por face e animação mais curta', () => {
  assert.match(diceUi, /function faceSymbols\(/);
  assert.match(diceUi, /add\('success','S',face\.success\)/);
  assert.match(diceUi, /add\('triumph','TRI',face\.triumph\)/);
  assert.match(diceUi, /\.sym\.success/);
  assert.match(diceUi, /\.sym\.triumph/);
  assert.match(diceUi, /elapsed<1480/);
  assert.doesNotMatch(diceUi, /Number\(face\.index\|\|0\)\+1/);
});
