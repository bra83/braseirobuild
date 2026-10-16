import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
const css=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.css',import.meta.url),'utf8');
test('P56: painel publicado renderiza timeline compacta a partir do sceneJournal público',()=>{assert.match(runtime,/status\?\.sceneJournal/);assert.match(runtime,/CAMINHO PERCORRIDO/);assert.match(runtime,/final-published-timeline-item/)});
test('P56: timeline mostra somente choiceLabel e destino já projetados, sem effects internos',()=>{const start=runtime.indexOf('const sceneJournal=');const end=runtime.indexOf('const epilogue=',start);const block=runtime.slice(start,end);assert.match(block,/choiceLabel/);assert.match(block,/toNodeId/);assert.doesNotMatch(block,/\.effects|kaatoOutcome|NovaCORE/)});
test('P56: timeline mobile tem truncamento e tipografia compacta',()=>{assert.match(css,/\.final-published-timeline/);assert.match(css,/text-overflow:ellipsis/);assert.match(css,/@media\(max-width:390px\).*final-published-timeline/s)});

import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { startUnderBlackSun, advanceUnderBlackSun, underBlackSunStatus } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
test('P56: journal fornece títulos somente das cenas visitadas para a timeline',()=>{let w=startUnderBlackSun(createWorld()).world;w=advanceUnderBlackSun(w,'escape-clean').world;const j=underBlackSunStatus(w).sceneJournal;assert.equal(j[0].fromTitle,'Perseguição em alta velocidade');assert.equal(j[0].toTitle,'Conversa na oficina de Chopper');assert.doesNotMatch(JSON.stringify(j),/NovaCORE|Kaa.to/)});
