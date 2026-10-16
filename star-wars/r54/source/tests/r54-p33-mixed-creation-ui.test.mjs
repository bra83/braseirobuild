import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const read=p=>readFileSync(new URL(`../${p}`,import.meta.url),'utf8');

test('P33 MIXED: bridge escolhe origem EotE/AoR sem converter Obligation em Duty',()=>{
 const bridge=read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
 assert.match(bridge,/creationCatalog\(origin = null\)/);
 assert.match(bridge,/mode === 'MIXED' && requestedOrigin === 'AOR'/);
 assert.match(bridge,/creationOrigin: useAor \? 'AOR' : 'EOTE'/);
 assert.match(bridge,/const created = useAor \? game\.createAorCharacter\(mapped\) : game\.createCustomCharacter\(options\)/);
});

test('P33 MIXED: shell permite escolher origem da ficha e rotula vínculo corretamente',()=>{
 const shell=read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
 assert.match(shell,/Origem da ficha/);
 assert.match(shell,/Edge of the Empire .* Obligation/);
 assert.match(shell,/Age of Rebellion .* Duty/);
 assert.match(shell,/A campanha continua MISTA/);
 assert.match(shell,/creationOrigin/);
 assert.match(shell,/Duty total/);
 assert.match(shell,/Obligation total/);
 assert.match(shell,/Contribui/);
});

test('P33 MIXED: fontes editáveis acompanham o bundle entregue',()=>{
 const app=read('shell-r46-run87/VTTApp.tsx');
 const templates=read('shell-r46-run87/Templates.tsx');
 const client=read('shell-r46-run87/gameBridgeClient.ts');
 assert.match(app,/creationOrigin: 'EOTE'/);
 assert.match(app,/loadCreationCatalogThroughBridge\(creationDraft\.creationOrigin/);
 assert.match(templates,/Origem da ficha · campanha mista/);
 assert.match(templates,/Obrigação e Dever permanecem separados no grupo/);
 assert.match(client,/creationOrigin: 'EOTE' \| 'AOR'/);
});
