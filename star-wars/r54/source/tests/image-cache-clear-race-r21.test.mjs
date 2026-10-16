import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const java = fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/ImageLibraryBridge.java', import.meta.url), 'utf8');

function method(name) {
  const start = java.indexOf(name);
  assert.ok(start >= 0, `${name} not found`);
  const next = java.indexOf('\n    @JavascriptInterface', start + 1);
  const privateNext = java.indexOf('\n    private ', start + 1);
  const candidates = [next, privateNext].filter(v => v > start);
  const end = candidates.length ? Math.min(...candidates) : java.length;
  return java.slice(start, end);
}

test('download de cache captura uma geração antes de entrar no executor', () => {
  const block = method('public String cacheRemoteImage');
  assert.match(block, /synchronized \(cacheCommitLock\) \{\s*generation = nextCacheGeneration\(cleanId\)/s);
  assert.match(block, /executor\.submit\(\(\) -> fetchAndCache\(cleanId, remote, generation\)\)/);
});

test('clear invalida a geração e remove imagem metadata e temporário sob o mesmo lock de commit', () => {
  const block = method('public String clearCachedAsset');
  assert.match(block, /synchronized \(cacheCommitLock\)/);
  assert.match(block, /nextCacheGeneration\(cleanId\)/);
  assert.match(block, /File temp = tempFile\(cleanId\)/);
  assert.match(block, /!temp\.exists\(\) \|\| temp\.delete\(\)/);
});

test('fetch só confirma arquivos se sua geração continuar atual dentro do lock de commit', () => {
  const block = method('private void fetchAndCache');
  assert.match(block, /synchronized \(cacheCommitLock\) \{\s*if \(!isCurrentCacheGeneration\(assetId, generation\)\)/s);
  const check = block.indexOf('isCurrentCacheGeneration(assetId, generation)');
  const rename = block.indexOf('temp.renameTo(target)');
  const metadata = block.indexOf('writeJson(metadataFile(assetId), meta)');
  const notify = block.indexOf('notifyEvent("cached", meta)');
  const lock = block.indexOf('synchronized (cacheCommitLock)');
  const lockEnd = block.indexOf('}\n        } catch', lock);
  assert.ok(check >= 0 && rename > check && metadata > rename && notify > metadata);
  assert.ok(lock >= 0 && notify > lock && (lockEnd < 0 || notify < lockEnd));
});

test('fetch invalidado não publica erro tardio depois de clear ou requisição mais nova', () => {
  const block = method('private void fetchAndCache');
  assert.match(block, /catch \(Exception e\)[\s\S]*if \(isCurrentCacheGeneration\(assetId, generation\)\)[\s\S]*notifyEvent\("error"/);
});
