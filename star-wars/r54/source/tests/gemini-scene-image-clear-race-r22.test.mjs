import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url), 'utf8');

function method(name) {
  const start = source.indexOf(name);
  assert.ok(start >= 0, `${name} not found`);
  const next = source.indexOf('\n    private ', start + name.length);
  return source.slice(start, next < 0 ? source.length : next);
}

test('geração e importação capturam uma geração de imagem antes do trabalho assíncrono', () => {
  assert.match(source, /long imageGeneration = nextSceneImageGeneration\(\);[\s\S]*executor\.submit\(\(\) -> generateRemote\(cleanPrompt, cleanSceneKey, imageGeneration\)\)/);
  assert.match(source, /Uri uri = data\.getData\(\);\s*long imageGeneration = nextSceneImageGeneration\(\);\s*executor\.submit\(\(\) -> importSceneImage\(uri, sceneKey, imageGeneration\)\)/);
});

test('limpar imagem invalida trabalho anterior e remove temporário sob o mesmo lock', () => {
  const block = method('public String clearSceneImage()');
  assert.match(block, /synchronized \(sceneImageCommitLock\)/);
  assert.match(block, /nextSceneImageGeneration\(\)/);
  assert.match(block, /new File\(imageDir, "current\.tmp"\)/);
  assert.match(block, /notifyEvent\("image_cleared"/);
});

test('commit só persiste geração atual e ordena evento de sucesso dentro do lock', () => {
  const block = method('private boolean persistSceneImage');
  assert.match(block, /synchronized \(sceneImageCommitLock\)/);
  assert.ok((block.match(/isCurrentSceneImageGeneration\(imageGeneration\)/g) || []).length >= 2);
  const writeMeta = block.indexOf('writeBytes(imageMetaFile');
  const notify = block.indexOf('notifyEvent(successEvent');
  assert.ok(writeMeta >= 0 && notify > writeMeta, 'success event must follow metadata commit');
});

test('erros tardios de geração/importação invalidados não chegam à UI', () => {
  const generated = method('private void generateRemote');
  const imported = method('private void importSceneImage');
  assert.match(generated, /if \(isCurrentSceneImageGeneration\(imageGeneration\)\) \{\s*notifyEvent\("erro"/s);
  assert.match(imported, /if \(isCurrentSceneImageGeneration\(imageGeneration\)\) \{\s*notifyEvent\("erro"/s);
});
