import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
const css=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.css',import.meta.url),'utf8');
test('UI publicada oferece ROLAR por check',()=>{assert.match(runtime,/data-published-check/);assert.match(runtime,/BraseiroPublishedActionClient\.execute\('check'/);assert.match(runtime,/ROLANDO/)});
test('resultado narrativo mostra sucesso, vantagem, momentum e alerta',()=>{for(const token of ['SUCESSO','Vantagem','Momentum','Alerta'])assert.match(runtime,new RegExp(token))});
test('check resolvido tem estado visual',()=>{assert.match(runtime,/is-resolved/);assert.match(css,/final-published-checks li\.is-resolved/)});
