import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
test('Cena mostra somente planos explicitamente públicos e não lê objetivo/recursos privados',async()=>{
  const js=await readFile(new URL('js/ui/app.mjs',root),'utf8'); const fn=js.slice(js.indexOf('function renderKnownFactions'),js.indexOf('function renderSession'));
  assert.match(fn,/faction\.plans/); assert.match(fn,/filter\(p=>p\.knownToPlayer\)/); assert.match(fn,/etapa atual/); assert.doesNotMatch(fn,/\.objective/); assert.doesNotMatch(fn,/\.resources/); assert.doesNotMatch(fn,/\.relations/);
});
