import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { startBeyondTheRim, beyondTheRimStatus } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
test('P68: Além da Orla expõe tracker próprio sem semântica de Sob um Sol Negro',()=>{const {world}=startBeyondTheRim({});const t=beyondTheRimStatus(world).tracker;assert.equal(t.kind,'beyond-the-rim');assert.equal(t.episode,1);assert.match(t.objective,/IsoTech|acordo/);assert.deepEqual(t.stages.filter(x=>x.reached).map(x=>x.id),['wheel']);});
test('P68: tracker acompanha episódios e conclusão sem spoiler futuro',()=>{const {world}=startBeyondTheRim({});const p=world.publishedAdventure;p.currentNodeId='cholganna-search';p.visitedNodeIds.push('journey-beyond-rim');let t=beyondTheRimStatus(world).tracker;assert.equal(t.episode,2);assert.deepEqual(t.stages.filter(x=>x.reached).map(x=>x.id),['wheel','cholganna']);assert.ok(!JSON.stringify(t).includes('RAXUS PRIME') || !t.stages.find(x=>x.id==='raxus').reached);p.currentNodeId='adventure-complete';p.status='completed';t=beyondTheRimStatus(world).tracker;assert.equal(t.episode,3);assert.equal(t.stages.at(-1).reached,true);});
test('P68: runtime possui renderização dedicada 415-friendly para tracker de Além da Orla',()=>{assert.match(runtime,/tracker\.kind==='beyond-the-rim'/);assert.match(runtime,/DISPUTA YIYAR/);assert.match(runtime,/EXPOSIÇÃO IMPERIAL/);assert.match(runtime,/Progresso de Além da Orla/);});
