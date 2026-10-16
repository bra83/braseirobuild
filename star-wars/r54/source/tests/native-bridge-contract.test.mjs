import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = p => fs.readFileSync(p, 'utf8');
const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const gemini = read('app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java');
const images = read('app/src/main/java/com/braseiro/starwarsedge/ImageLibraryBridge.java');
const tts = read('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
const files = read('app/src/main/java/com/braseiro/starwarsedge/FileBridge.java');
const shell = read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');

function hasJavaInterfaceMethod(src, name) {
  return new RegExp(`@JavascriptInterface\\s+public\\s+[A-Za-z0-9_<>,?\\[\\] ]+\\s+${name}\\s*\\(`).test(src);
}

test('Android host registers all five native bridges under the names used by JS', () => {
  for (const name of ['BraseiroLibrary','BraseiroGemini','BraseiroImages','BraseiroTts','BraseiroFiles']) {
    assert.match(main, new RegExp(`addJavascriptInterface\\([^\\n]+,\\s*"${name}"\\)`));
  }
});

test('Library bridge methods consumed by R46 exist on the Android host', () => {
  for (const name of ['pickPdf','listDocuments','searchDocuments','setDocumentPolicy','reindexDocument','removeDocument']) {
    assert.ok(hasJavaInterfaceMethod(main, name), `LibraryBridge missing ${name}`);
    assert.match(shell, new RegExp(`\\b${name}\\b`));
  }
});

test('Gemini bridge methods consumed by engine/R46 exist on the Android host', () => {
  for (const name of ['getStatus','configureApiKey','clearApiKey','generateSceneImage','requestMasterProposal','pickSceneImage','clearSceneImage']) {
    assert.ok(hasJavaInterfaceMethod(gemini, name), `GeminiBridge missing ${name}`);
  }
  for (const name of ['getStatus','configureApiKey','clearApiKey','generateSceneImage','pickSceneImage','clearSceneImage']) {
    assert.match(shell, new RegExp(`\\b${name}\\b`));
  }
  assert.match(read('app/src/main/assets/vtt/js/core/uiBridge.mjs'), /requestMasterProposal/);
});

test('Image cache bridge methods consumed by R46 exist on the Android host', () => {
  for (const name of ['getStatus','cacheRemoteImage','clearCachedAsset']) {
    assert.ok(hasJavaInterfaceMethod(images, name), `ImageLibraryBridge missing ${name}`);
    assert.match(shell, new RegExp(`\\b${name}\\b`));
  }
});

test('TTS runtime calls only methods exposed by TtsBridge', () => {
  for (const name of ['getStatus','speak','stop']) {
    assert.ok(hasJavaInterfaceMethod(tts, name), `TtsBridge missing ${name}`);
    assert.match(runtime, new RegExp(`\\b${name}\\b`));
  }
});

test('JSON export uses the native FileBridge saveJson contract', () => {
  assert.ok(hasJavaInterfaceMethod(files, 'saveJson'));
  assert.match(shell, /BraseiroFiles/);
  assert.match(shell, /saveJson/);
});
