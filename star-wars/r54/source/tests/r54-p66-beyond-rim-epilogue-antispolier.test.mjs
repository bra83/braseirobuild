import test from 'node:test';
import assert from 'node:assert/strict';
import { startBeyondTheRim, beyondTheRimStatus } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

test('P66: epílogo permanece oculto enquanto Além da Orla está ativa',()=>{
  const {world}=startBeyondTheRim({});
  world.publishedAdventure.systems.imperialExposure=6;
  world.publishedAdventure.flags.yiyarEscalated=true;
  world.publishedAdventure.flags.harsolTrust=3;
  assert.deepEqual(beyondTheRimStatus(world).epilogue,[]);
});

test('P66: conclusão revela apenas consequências conquistadas em PT-BR',()=>{
  const {world}=startBeyondTheRim({});
  const p=world.publishedAdventure;
  p.status='completed'; p.currentNodeId='adventure-complete';
  p.systems.imperialExposure=4; p.flags.yiyarEscalated=true; p.flags.harsolTrust=2; p.flags.imperialTrackerDetected=true;
  const c=beyondTheRimStatus(world).epilogue;
  assert.deepEqual(c.map(x=>x.id),['expedition-aftermath','survivors','yiyar-rivalry','imperial-trail']);
  assert.ok(c.every(x=>!JSON.stringify(x).match(/spoiler|future node|hidden/i)));
});

test('P66: epílogo não inventa rivalidade ou perseguição imperial não conquistadas',()=>{
  const {world}=startBeyondTheRim({});
  const p=world.publishedAdventure;
  p.status='completed'; p.currentNodeId='adventure-complete'; p.systems.imperialExposure=1;
  const ids=beyondTheRimStatus(world).epilogue.map(x=>x.id);
  assert.deepEqual(ids,['expedition-aftermath']);
});
