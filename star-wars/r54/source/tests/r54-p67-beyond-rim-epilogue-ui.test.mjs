import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { startBeyondTheRim, beyondTheRimStatus } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');

test('P67: painel visual consome epílogo público sem acessar flags internas',()=>{
  assert.match(runtime,/Array\.isArray\(status\?\.epilogue\)/);
  assert.match(runtime,/MISSÃO CONCLUÍDA · CONSEQUÊNCIAS/);
  const epilogueBlock=runtime.slice(runtime.indexOf('const epilogueLines='),runtime.indexOf('const encounter=status'));
  assert.doesNotMatch(epilogueBlock,/progress\?\.flags|publishedAdventure\.flags|yiyarEscalated|imperialExposure|harsolTrust/);
});

test('P67: aventura ativa não fornece linhas de epílogo para a UI',()=>{
  const {world}=startBeyondTheRim({});
  world.publishedAdventure.flags.yiyarEscalated=true;
  world.publishedAdventure.systems.imperialExposure=6;
  assert.deepEqual(beyondTheRimStatus(world).epilogue,[]);
});

test('P67: UI recebe somente resumos públicos em PT-BR depois da conclusão',()=>{
  const {world}=startBeyondTheRim({});
  const p=world.publishedAdventure;
  p.status='completed'; p.currentNodeId='adventure-complete';
  p.flags.yiyarEscalated=true; p.flags.harsolTrust=2; p.flags.imperialTrackerDetected=true; p.systems.imperialExposure=4;
  const epilogue=beyondTheRimStatus(world).epilogue;
  assert.equal(epilogue.length,4);
  assert.ok(epilogue.every(x=>typeof x.summary==='string'&&x.summary.length>20));
  assert.ok(epilogue.every(x=>!('flags' in x)&&!('systems' in x)));
});
