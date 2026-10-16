import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
test('Cena mostra Forças em jogo em PT-BR sem expor objetivo ou recursos privados',async()=>{
  const html=await readFile(new URL('index.html',root),'utf8'); const js=await readFile(new URL('js/ui/app.mjs',root),'utf8');
  assert.match(html,/Forças em jogo/); assert.match(html,/fatos públicos/); assert.match(js,/filter\(f=>Boolean\(f\.knownToPlayer\)\)/); assert.match(js,/filter\(f=>f\.knownToPlayer\)/);
  const fn=js.slice(js.indexOf('function renderKnownFactions'),js.indexOf('function renderSession'));
  assert.doesNotMatch(fn,/\.objective/); assert.doesNotMatch(fn,/\.resources/); assert.doesNotMatch(fn,/\.relations/);
});
