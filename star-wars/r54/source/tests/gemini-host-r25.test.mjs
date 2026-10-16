import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const bridge = fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url), 'utf8');
const activity = fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java', import.meta.url), 'utf8');

test('host injeta BraseiroGemini sem substituir BraseiroLibrary', () => {
  assert.match(activity, /addJavascriptInterface\(new LibraryBridge\(\), "BraseiroLibrary"\)/);
  assert.match(activity, /addJavascriptInterface\(geminiBridge\.javascriptInterface\(\), "BraseiroGemini"\)/);
});

test('chave Gemini fica fora do estado e protegida por AndroidKeyStore/no-backup', () => {
  assert.match(bridge, /getNoBackupFilesDir\(\)/);
  assert.match(bridge, /AndroidKeyStore/);
  assert.match(bridge, /AES\/GCM\/NoPadding/);
  assert.doesNotMatch(bridge, /put\("apiKey"/);
  assert.match(bridge, /KEY_FALLBACK_ENCRYPTED/);
  assert.match(bridge, /KEY_FALLBACK_ALIAS/);
  assert.match(bridge, /storeEncryptedFallback\(clean\)/);
  assert.doesNotMatch(bridge, /putString\(KEY_FALLBACK,\s*clean\)/);
});

test('host usa endpoint/modelo oficial atual e nunca envia chave no corpo JSON', () => {
  assert.match(bridge, /gemini-3\.1-flash-image/);
  assert.match(bridge, /generativelanguage\.googleapis\.com\/v1beta\/interactions/);
  assert.match(bridge, /x-goog-api-key/);
  assert.doesNotMatch(bridge, /\.put\("api[_-]?key"/i);
});

test('geração é explícita, assíncrona e limitada a prompt/sceneKey', () => {
  assert.match(bridge, /generateSceneImage\(String prompt, String sceneKey\)/);
  assert.match(bridge, /executor\.submit\(\(\) -> generateRemote/);
  assert.match(bridge, /cleanPrompt\.length\(\) > 5000/);
  assert.match(bridge, /scene_\[0-9a-f\]\{8\}/);
});

test('imagem gerada usa 16:9 e 1K', () => {
  assert.match(bridge, /aspect_ratio/);
  assert.match(bridge, /16:9/);
  assert.match(bridge, /image_size/);
  assert.match(bridge, /1K/);
});

test('host aceita fallback de importação manual com cópia privada', () => {
  assert.match(bridge, /ACTION_OPEN_DOCUMENT/);
  assert.match(bridge, /setType\("image\/\*"\)/);
  assert.match(bridge, /persistSceneImage\(bytes, mime, sceneKey, "import", imageGeneration, "imported"\)/);
});

test('imagem só é servida pela URL local interceptada', () => {
  assert.match(bridge, /braseiro\.local/);
  assert.match(activity, /shouldInterceptRequest/);
  assert.match(activity, /openSceneImageResponse/);
});

test('eventos nativos não incluem chave nem prompt', () => {
  const eventLines = bridge.split('\n').filter(line => line.includes('notifyEvent(')).join('\n');
  assert.doesNotMatch(eventLines, /apiKey/);
  assert.doesNotMatch(eventLines, /prompt/);
});
