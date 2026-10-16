import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

function run(script) {
  const r=spawnSync(process.execPath,[script],{cwd:fileURLToPath(new URL('..',import.meta.url)),encoding:'utf8'});
  assert.equal(r.status,0,`${script} falhou: ${r.stderr||r.stdout}`);
  return r.stdout;
}

test('P20: builders da raiz e tools recompilam o mesmo bundle canônico', () => {
  assert.match(run('build-ui-bridge-bundle.mjs'), /\"ok\": true/);
  const first=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url));
  assert.match(run('tools/build-ui-bridge-bundle.mjs'), /\"ok\": true/);
  const second=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url));
  assert.deepEqual(second,first);
});
