import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('harness web: todo id literal acessado por $(id) existe no HTML',()=>{
  const htmlIds=new Set([...html.matchAll(/\bid=["']([^"']+)/g)].map(m=>m[1]));
  const refs=new Set([...js.matchAll(/\$\('([^']+)'\)/g)].map(m=>m[1]));
  const missing=[...refs].filter(id=>!htmlIds.has(id)).sort();
  assert.deepEqual(missing,[]);
});
