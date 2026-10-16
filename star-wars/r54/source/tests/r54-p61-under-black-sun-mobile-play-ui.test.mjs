import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { startUnderBlackSun, advanceUnderBlackSun, underBlackSunStatus } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
const css=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.css',import.meta.url),'utf8');
const step=(w,id)=>advanceUnderBlackSun(w,id).world;

test('P61: sessão renderiza tracker público com objetivo, investigação e pressão',()=>{
  assert.match(runtime,/const tracker=status\?\.tracker\|\|null/);
  assert.match(runtime,/OBJETIVO ATUAL/);
  assert.match(runtime,/INVESTIGAÇÃO/);
  assert.match(runtime,/ALERTA/);
  assert.match(runtime,/MOMENTUM/);
  assert.match(runtime,/final-published-tracker/);
});

test('P61: superfície de sessão usa tracker anti-spoiler em vez de flags internas',()=>{
  const start=runtime.indexOf("const tracker=status?.tracker||null");
  const end=runtime.indexOf('const epilogue=',start);
  const block=runtime.slice(start,end);
  assert.ok(start>=0 && end>start);
  assert.doesNotMatch(block,/status\?\.progress\?\.flags|status\.progress\.flags|factoryRoute|trapPrepared|resolutionMode|obligationPendingDelta/);
});

test('P61: ações essenciais aparecem antes dos detalhes secundários',()=>{
  const start=runtime.indexOf('panel.innerHTML=`<div class="final-published-scene-head"');
  const end=runtime.indexOf('panel.querySelectorAll(\'[data-published-check]\')',start);
  const block=runtime.slice(start,end);
  assert.ok(block.indexOf('TESTES DISPONÍVEIS') < block.indexOf('supportingDetails'));
  assert.ok(block.indexOf('O QUE FAZER') < block.indexOf('supportingDetails'));
  assert.match(runtime,/INFORMAÇÕES DA CENA/);
  assert.ok(block.indexOf('O QUE FAZER') < block.indexOf('${supportingDetails}'));
});

test('P61: layout 415x915 mantém alvos touch grandes e tracker compacto',()=>{
  assert.match(css,/@media\(max-width:415px\)/);
  assert.match(css,/\.final-published-choices button\{min-height:46px\}/);
  assert.match(css,/\.final-published-checks li>button\{min-width:70px;min-height:42px\}/);
  assert.match(css,/\.final-published-milestones\{[^}]*overflow-x:auto/s);
});

test('P61: tracker pós-investigação continua sem revelar rota futura',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  for(const id of ['escape-clean','begin-investigation','go-umbra','umbra-talk','umbra-social-clean','go-spyder','spyder-return']) w=step(w,id);
  const t=underBlackSunStatus(w).tracker;
  assert.equal(t.investigation.ready,true);
  assert.equal(t.investigation.found,2);
  assert.doesNotMatch(JSON.stringify(t),/NovaCORE|fábrica|esconderijo|rendição|armadilha/i);
});
