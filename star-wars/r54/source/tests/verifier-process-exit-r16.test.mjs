import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const text=fs.readFileSync(new URL('../tools/verify-final-source.mjs',import.meta.url),'utf8');

test('verificador CLI encerra explicitamente após gravar relatório',()=>{
  const report=text.indexOf('log(`report=${relative(root, out)}`)');
  const exit=text.indexOf('process.exit(finalExitCode)');
  assert.ok(report>=0 && exit>report);
  assert.match(text,/const finalExitCode = process\.exitCode \|\| 0;/);
  assert.match(text,/['"]--test-force-exit['"]/);
});
