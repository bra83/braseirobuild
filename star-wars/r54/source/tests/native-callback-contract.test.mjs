import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = p => fs.readFileSync(p, 'utf8');
const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const gemini = read('app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java');
const images = read('app/src/main/java/com/braseiro/starwarsedge/ImageLibraryBridge.java');
const shell = read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
const engine = read('app/src/main/assets/vtt/js/core/uiBridge.mjs');

test('Library Java callback name and payload envelope match the R46 consumer', () => {
  assert.match(main, /new JSONObject\(\)\.put\("type", type\)\.put\("payload", payload\)/);
  assert.match(main, /window\.braseiroLibraryNativeEvent/);
  assert.match(shell, /window\.braseiroLibraryNativeEvent\s*=\s*\(raw\)\s*=>/);
  assert.match(shell, /event\.payload\.document/);
});

test('Image cache Java callback name and data envelope match the R46 consumer', () => {
  assert.match(images, /put\("type", type\)\.put\("data", payload/);
  assert.match(images, /window\.braseiroImageLibraryNativeEvent/);
  assert.match(shell, /window\.braseiroImageLibraryNativeEvent\s*=\s*\(raw\)\s*=>/);
  assert.match(shell, /event\.data\.assetId/);
});

test('Gemini UI callback name and payload envelope match the R46 consumer', () => {
  assert.match(gemini, /put\("type", type\)\.put\("payload", payload/);
  assert.match(gemini, /window\.braseiroGeminiNativeEvent/);
  assert.match(shell, /window\.braseiroGeminiNativeEvent\s*=\s*\(raw\)\s*=>/);
  assert.match(shell, /event\.payload\.message/);
});

test('Gemini master callback name matches Motor Barbara native request hook', () => {
  assert.match(gemini, /window\.braseiroMasterNativeEvent/);
  assert.match(engine, /target\.braseiroMasterNativeEvent\s*=\s*\(raw\)\s*=>/);
  assert.match(engine, /event\?\.requestId/);
});

test('Gemini master callback preserves proposal object and model contract', () => {
  assert.match(engine, /event\?\.ok\s*===\s*true/);
  assert.match(engine, /event\.proposal\s*&&\s*typeof event\.proposal\s*===\s*'object'/);
  assert.match(engine, /proposal:\s*event\.proposal/);
  assert.match(engine, /model:\s*event\.model/);
  assert.match(gemini, /notifyMasterEvent\(/);
});
