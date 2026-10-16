import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { startUnderBlackSun, advanceUnderBlackSun, underBlackSunStatus } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

const step=(w,id)=>advanceUnderBlackSun(w,id).world;

test('P80: projeção pública não expõe flags arbitrárias/secretas mesmo se save antigo estiver contaminado',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w.publishedAdventure.flags.secretFutureBoss='NÃO PODE VAZAR';
  w.publishedAdventure.flags.spoilerEscapeRoute='NÃO PODE VAZAR';
  w.publishedAdventure.flags.internalDebugSeed=123;
  const s=underBlackSunStatus(w);
  assert.equal(s.progress.flags.secretFutureBoss,undefined);
  assert.equal(s.progress.flags.spoilerEscapeRoute,undefined);
  assert.equal(s.progress.flags.internalDebugSeed,undefined);
  assert.equal(s.progress.flags.alertClock,0);
});

test('P80: start/advance retornam snapshot público e não o progresso mestre bruto',()=>{
  let r=startUnderBlackSun(createWorld());
  r.world.publishedAdventure.flags.secretFutureBoss='oculto';
  r=advanceUnderBlackSun(r.world,'escape-clean');
  assert.equal(r.progress.flags.secretFutureBoss,undefined);
  assert.equal(r.progress.flags.chaseOutcome,'clean');
  assert.equal(r.node.id,'chop-talk');
});

test('P80: rotas futuras permanecem invisíveis até serem descobertas',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean');w=step(w,'begin-investigation');
  let s=underBlackSunStatus(w);
  assert.equal(s.knownClues.some(c=>/NOVACORE|943|ESCONDERIJO/i.test(c.title+' '+c.summary)),false);
  assert.equal(JSON.stringify(s).includes('novacore-plant'),false);
  assert.equal(JSON.stringify(s).includes('kaato-hideout'),false);
});

test('P80: save/reopen mantém estado e a projeção anti-spoiler',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  for(const id of ['escape-fight','begin-investigation','go-spyder','spyder-warn-black-sun'])w=step(w,id);
  w.publishedAdventure.flags.secretGMNote='não mostrar';
  w=JSON.parse(JSON.stringify(w));
  const s=underBlackSunStatus(w);
  assert.equal(s.progress.currentNodeId,'investigation-hub');
  assert.equal(s.progress.flags.blackSunAgainstKaato,true);
  assert.equal(s.progress.flags.policeAttention,true);
  assert.equal(s.progress.flags.secretGMNote,undefined);
});
