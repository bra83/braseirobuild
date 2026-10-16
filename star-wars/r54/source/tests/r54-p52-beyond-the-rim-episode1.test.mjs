import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import {
  BEYOND_THE_RIM,
  startBeyondTheRim,
  advanceBeyondTheRim,
  adjudicateBeyondTheRimCheck,
  beyondTheRimStatus,
  currentBeyondTheRimMasterRequest
} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
const step=(w,id)=>advanceBeyondTheRim(w,id).world;
const reopen=w=>JSON.parse(JSON.stringify(w));
function makeGame(key='p52'){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key));g.loadStarterCharacter('cael');return g;}
function toPrep(){let w=startBeyondTheRim(createWorld()).world;w=step(w,'meet-reom');w=step(w,'accept-job');return w;}
function toChase(){let w=toPrep();w=step(w,'investigate-rumors');w=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:1,triumph:0,despair:0}}).world;w=step(w,'return-from-rumors');w=step(w,'wait-for-droid');return step(w,'pursue-yiyar');}

test('P52 registry instala Além da Orla como EotE/MIXED sem alterar Sob um Sol Negro',()=>{
  const catalog=publishedAdventureCatalog();
  assert.equal(catalog.some(x=>x.id==='under-black-sun'),true);
  const btr=catalog.find(x=>x.id==='beyond-the-rim');
  assert.ok(btr);
  assert.deepEqual(btr.campaignModes,['EOTE','MIXED']);
  assert.equal(btr.campaignModes.includes('AOR'),false);
  assert.equal(getPublishedAdventureAdapter('beyond-the-rim').meta.originalTitle,'Beyond the Rim');
});

test('P52 início é anti-spoiler: não revela IT-3PO, Yiyar ou ISB antes da hora',()=>{
  const w=startBeyondTheRim(createWorld()).world;
  const s=beyondTheRimStatus(w);
  assert.equal(s.progress.currentNodeId,'wheel-arrival');
  assert.equal(w.npcs['btr.npc.it3po'].knownToPlayer,false);
  assert.equal(w.npcs['btr.npc.yav_yiyar'].knownToPlayer,false);
  assert.equal(w.factions['btr.fac.yiyar'].knownToPlayer,false);
  assert.equal(w.factions['btr.fac.isb'].knownToPlayer,false);
  assert.deepEqual(s.node.choices.map(x=>x.id),['meet-reom']);
  const request=currentBeyondTheRimMasterRequest(w);
  assert.equal(request.antiSpoiler,true);
  assert.equal(JSON.stringify(request).includes('Yiyar'),false);
  assert.equal(JSON.stringify(request).includes('IT-3PO'),false);
});

test('P52 preparação exige ao menos uma ação real e persiste pista de rumores após reopen',()=>{
  let w=toPrep();
  let s=beyondTheRimStatus(w);
  assert.equal(s.node.choices.some(x=>x.id==='wait-for-droid'),false);
  w=step(w,'investigate-rumors');
  w=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:2,triumph:0,despair:0}}).world;
  w=step(w,'return-from-rumors');
  w=reopen(w);
  s=beyondTheRimStatus(w);
  assert.equal(s.node.choices.some(x=>x.id==='wait-for-droid'),true);
  assert.equal(s.knownClues.some(x=>x.id==='rumor'),true);
  assert.equal(w.publishedAdventure.flags.routeIntel,true);
});

test('P52 inspeção da nave persiste detecção/remoção de rastreador e não inventa consequência imperial',()=>{
  let w=toPrep();
  w=step(w,'inspect-ship');
  w=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:0,triumph:0,despair:0}}).world;
  assert.equal(w.publishedAdventure.flags.imperialTrackerDetected,true);
  assert.equal(w.publishedAdventure.flags.imperialTrackerAttached,true);
  w=adjudicateBeyondTheRimCheck(w,{checkIndex:1,result:{success:true,netSuccess:2,netAdvantage:1,triumph:0,despair:0}}).world;
  w=reopen(w);
  assert.equal(w.publishedAdventure.flags.imperialTrackerRemoved,true);
  assert.equal(Boolean(w.publishedAdventure.flags.imperialTrackerAttached),false);
  assert.equal(beyondTheRimStatus(w).knownClues.some(x=>x.id==='tracker-removed'),true);
});

test('P52 perseguição pode recuperar IT-3PO e consolida consequência do Episódio I',()=>{
  let w=toChase();
  let r=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:2,triumph:0,despair:0}});w=r.world;
  assert.equal(r.adjudication.systems.chase.lead,2);
  assert.equal(w.publishedAdventure.flags.droidRecovered,true);
  assert.deepEqual(beyondTheRimStatus(w).node.choices.map(x=>x.id),['secure-it3po']);
  w=step(w,'secure-it3po');
  w=step(w,'depart-wheel-ahead');
  w=reopen(w);
  const s=beyondTheRimStatus(w);
  assert.equal(s.progress.currentNodeId,'episode1-boundary');
  assert.equal(s.implementation.episode1Complete,true);
  assert.equal(w.publishedAdventureConsequences['beyond-the-rim'].partyAhead,true);
  assert.equal(w.publishedAdventureConsequences['beyond-the-rim'].yiyarHasDroid,false);
  assert.equal(s.node.choices.some(x=>x.id==='plot-cholganna-route'),true);
});

test('P52 falha severa na perseguição permite fuga dos Yiyar e mantém desvantagem persistente',()=>{
  let w=toChase();
  w=adjudicateBeyondTheRimCheck(w,{checkIndex:1,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:0}}).world;
  assert.equal(w.publishedAdventure.flags.droidLost,true);
  w=step(w,'yiyar-escape');
  w=step(w,'depart-wheel-behind');
  w=reopen(w);
  assert.equal(w.publishedAdventure.flags.yiyarHasDroid,true);
  assert.equal(w.publishedAdventure.flags.partyAhead,false);
  assert.equal(w.publishedAdventureConsequences['beyond-the-rim'].yiyarHasDroid,true);
});

test('P52 controller usa o adapter genérico e mantém gate EotE/MIXED',()=>{
  const g=makeGame('p52-eote');
  const started=g.startPublishedAdventureSession('beyond-the-rim',{rng:()=>0.5});
  assert.equal(started.adventure.active,true);
  assert.equal(started.adventure.progress.currentNodeId,'wheel-arrival');
  g.advancePublishedAdventure('meet-reom',{adventureId:'beyond-the-rim'});
  assert.equal(g.publishedAdventureStatus('beyond-the-rim').progress.currentNodeId,'isotech-briefing');
  const mixed=makeGame('p52-mixed');mixed.setCampaignMode('MIXED');assert.equal(mixed.startPublishedAdventureSession('beyond-the-rim',{rng:()=>0.5}).adventure.active,true);
  const aor=makeGame('p52-aor');aor.setCampaignMode('AOR');assert.throws(()=>aor.startPublishedAdventureSession('beyond-the-rim',{rng:()=>0.5}),/EotE ou MISTA/i);
});

test('P52/P53 metadados preservam Episódio I e avançam a fronteira sem removê-lo',()=>{
  assert.equal(BEYOND_THE_RIM.implementation.episodesImplemented.includes(1),true);
  assert.equal(BEYOND_THE_RIM.implementation.episodesTotal,3);
});

test('P52 UI deixou de fixar Sob um Sol Negro e usa a aventura ativa em checks, escolhas e restart',async()=>{
  const { readFileSync }=await import('node:fs');
  const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(runtime,/publishedAdventureCatalogView/);
  assert.match(runtime,/activePublishedAdventureId/);
  assert.match(runtime,/data-start-published/);
  assert.match(runtime,/\{adventureId\}/);
  assert.match(runtime,/restartPublishedAdventure',publishedId/);
  assert.match(runtime,/Episódio I concluído e persistido/);
  assert.doesNotMatch(runtime,/startPublishedAdventureSession','under-black-sun'/);
});
