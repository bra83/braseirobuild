import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = p => fs.readFileSync(new URL('../' + p, import.meta.url), 'utf8');

test('P77: TtsBridge usa somente APIs públicas disponíveis para identificar engine', () => {
  const src = read('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
  assert.doesNotMatch(src, /\.getCurrentEngine\s*\(/);
  assert.match(src, /requestedEnginePackage/);
  assert.match(src, /getDefaultEngine\s*\(/);
  assert.match(src, /activeEnginePackage\s*=\s*requestedEngine\.isEmpty\(\)\s*\?\s*systemDefaultEngine\s*:\s*requestedEngine/);
});

test('P77: Android e shell publicam a mesma versão e permanecem em 1.0.77 ou posterior', () => {
  const gradle = read('app/build.gradle.kts');
  const shell = read('app/src/main/assets/vtt/r46-shell.html');
  const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
  const code=Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]||0);
  assert.ok(code>=10077);
  const version=gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1]||'';
  const patch=Number(version.match(/^1\.0\.(\d+)$/)?.[1]||0); assert.ok(patch>=77);
  assert.ok(shell.includes(`data-version="${version}"`));
  assert.ok(runtime.includes(`R54 P${patch} · Android ${version}`), 'Diagnóstico Android precisa refletir a versão compilada.');
});
