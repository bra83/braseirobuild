import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
test('combate só mostra nome da facção do adversário quando ela é conhecida pelo jogador',async()=>{
  const js=await readFile(new URL('js/ui/app.mjs',root),'utf8'); const fn=js.slice(js.indexOf('function renderEncounter'),js.indexOf('function renderCombat'));
  assert.match(fn,/adv\.factionId/); assert.match(fn,/faction\?\.knownToPlayer/); assert.match(fn,/faction\.name/);
});
