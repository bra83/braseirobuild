import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const read=(p)=>readFileSync(new URL(`../${p}`,import.meta.url),'utf8');

test('R33 UI: primeiro uso oferece EotE, AoR ou Misto e apenas um ponto inicial',()=>{
 const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
 assert.match(runtime,/MUNDO LIVRE/);
 assert.match(runtime,/value=\"EOTE\"/); assert.match(runtime,/value=\"AOR\"/); assert.match(runtime,/value=\"MIXED\"/);
 assert.match(runtime,/sandbox-start/);
 assert.match(runtime,/setCampaignMode/);
 assert.match(runtime,/initializeLivingSandbox/);
 assert.match(runtime,/nenhuma aventura é obrigatória|sem missão obrigatória|sem roteiro obrigatório|sem trilho obrigatório/i);
});

test('R33 UI: criação AoR reutiliza a shell aprovada e troca rótulos Obligation→Duty',()=>{
 const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
 const bridge=read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
 assert.match(runtime,/relabelCreationForCampaign/);
 assert.match(runtime,/Obligation/); assert.match(runtime,/Duty/);
 assert.match(bridge,/activeProfile:\s*useAor\s*\?\s*'AOR_CORE'/);
 assert.match(bridge,/createAorCharacter/);
});

test('R33 UI: sandbox vivo é uma extensão funcional da R46, não um redesenho',()=>{
 const html=read('app/src/main/assets/vtt/r46-shell.html');
 const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
 assert.match(html,/r46\/r46-shell\.bundle\.js/);
 assert.match(html,/r46\/final-runtime\.js/);
 assert.match(runtime,/6 facções/); assert.match(runtime,/36 NPCs/); assert.match(runtime,/12 locais/);
 assert.match(runtime,/clocks|relógios/i); assert.match(runtime,/mercados/i); assert.match(runtime,/oportunidades/i);
});
