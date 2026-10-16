import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import {
  UNDER_BLACK_SUN,
  startUnderBlackSun,
  underBlackSunStatus,
  advanceUnderBlackSun,
  markUnderBlackSunRewardsApplied,
  currentUnderBlackSunMasterRequest,
  recordUnderBlackSunEncounterOutcome
} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

function step(world, choiceId){ return advanceUnderBlackSun(world, choiceId).world; }

test('P33: catálogo preserva EotE/MIXED e não oferece aventura EotE pura em AoR',()=>{
  assert.equal(UNDER_BLACK_SUN.id,'under-black-sun');
  assert.deepEqual(UNDER_BLACK_SUN.campaignModes,['EOTE','MIXED']);
  assert.equal(UNDER_BLACK_SUN.rulesProfile,'EOTE_CORE');
});

test('P33: início é PT-BR, persistente e não revela nós futuros ao jogador',()=>{
  const started=startUnderBlackSun(createWorld());
  const status=underBlackSunStatus(started.world);
  assert.equal(status.active,true);
  assert.equal(status.progress.currentNodeId,'hot-pursuit');
  assert.match(status.node.title,/Perseguição/i);
  assert.ok(status.node.choices.length>=3);
  assert.equal('nodes' in status,false);
  assert.equal('to' in status.node.choices[0],false);
  assert.equal('effects' in status.node.choices[0],false);
  const req=currentUnderBlackSunMasterRequest(started.world);
  assert.equal(req.antiSpoiler,true);
  assert.equal(req.nodeId,'hot-pursuit');
  assert.ok(req.suggestedActions.every(x=>typeof x==='string' && x.length>0));
});

test('P33: Episódio II aceita investigação em qualquer ordem e só libera desfecho após 2 linhas',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean');
  w=step(w,'begin-investigation');
  let s=underBlackSunStatus(w);
  assert.equal(s.progress.investigationCount,0);
  assert.equal(s.node.choices.some(x=>x.id==='move-endgame'),false);

  w=step(w,'go-spyder');
  w=step(w,'spyder-return');
  s=underBlackSunStatus(w);
  assert.equal(s.progress.investigationCount,1);
  assert.equal(s.node.choices.some(x=>x.id==='go-spyder'),false);
  assert.equal(s.node.choices.some(x=>x.id==='move-endgame'),false);

  w=step(w,'go-zelcomm');
  w=step(w,'zelcomm-clean');
  s=underBlackSunStatus(w);
  assert.equal(s.progress.investigationCount,2);
  assert.equal(s.node.choices.some(x=>x.id==='move-endgame'),true);
  assert.equal(s.node.choices.some(x=>x.id==='go-umbra'),true);
});

test('P33: flags de consequências persistem até o Episódio III e epílogo',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  for(const choice of [
    'escape-fight','begin-investigation','go-umbra','follow-speng','speng-complete',
    'go-spyder','spyder-warn-black-sun','move-endgame','plan-shadow','through-offices',
    'enter-abandoned','face-kaato','kaato-vanishes'
  ]) {
    if(choice==='kaato-vanishes') w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:'ended',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:'active'}]}).world;
    w=step(w,choice);
  }
  const s=underBlackSunStatus(w);
  assert.equal(s.completed,true);
  assert.equal(s.progress.flags.policeAttention,true);
  assert.equal(s.progress.flags.kaatoAlerted,true);
  assert.equal(s.progress.flags.blackSunAgainstKaato,true);
  assert.equal(s.progress.flags.kaatoRecurring,true);
  assert.equal(s.progress.currentNodeId,'epilogue');
});

test('P33: recompensa fica registrada e não reseta ao reiniciar a aventura na mesma campanha',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=markUnderBlackSunRewardsApplied(w);
  assert.equal(w.publishedAdventureRewards['under-black-sun'].applied,true);
  const restarted=startUnderBlackSun(w).world;
  const s=underBlackSunStatus(restarted);
  assert.equal(s.progress.rewardsApplied,true);
});

test('P33: UI oferece aventura publicada separada do mundo livre e bridge expõe contrato',()=>{
  const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const controller=readFileSync(new URL('../app/src/main/assets/vtt/js/core/controller.mjs',import.meta.url),'utf8');
  assert.match(runtime,/publishedAdventureCatalogView/);
  assert.match(runtime,/data-start-published/);
  assert.match(runtime,/MUNDO LIVRE/);
  assert.match(runtime,/enhancePublishedAdventurePanel/);
  assert.match(bridge,/startPublishedAdventureSession/);
  assert.match(bridge,/advancePublishedAdventure/);
  assert.match(controller,/campaignModes\.includes\(mode\)/);
  assert.match(controller,/publishedAdventureRewards|rewardsApplied/);
});

test('P34: checks da aventura expõem pool mecânico sem revelar destinos', async()=>{
  const mod=await import('../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs');
  const started=mod.startUnderBlackSun(createWorld());
  const checks=mod.currentUnderBlackSunChecks(started.world);
  assert.equal(checks[0].skillKey,'pilotar_vehiculo_planetario');
  assert.deepEqual(checks[0].pool,{difficulty:2,challenge:0,setback:0});
  assert.equal('to' in checks[0],false);
});

test('P34: Advantage/Threat/Triumph/Despair alimentam consequência persistente e relógio de alerta', async()=>{
  const mod=await import('../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs');
  let w=mod.startUnderBlackSun(createWorld()).world;
  let a=mod.adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:false,netSuccess:0,netAdvantage:-2,triumph:0,despair:1}});
  w=a.world;
  assert.equal(a.adjudication.alertClock,4);
  assert.equal(a.progress.flags.kaatoAlerted,true);
  a=mod.adjudicateUnderBlackSunCheck(w,{checkIndex:1,result:{success:true,netSuccess:1,netAdvantage:2,triumph:1,despair:0}});
  assert.equal(a.adjudication.momentum,4);
  assert.equal(a.progress.checkHistory.length,2);
});

test('P34: controller e bridge ligam teste publicado ao resolvedor narrativo real',()=>{
  const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const controller=readFileSync(new URL('../app/src/main/assets/vtt/js/core/controller.mjs',import.meta.url),'utf8');
  assert.match(controller,/resolvePublishedAdventureCheck/);
  assert.match(controller,/resolveSkillCheck\(\{ skillKey: spec\.skillKey/);
  assert.match(controller,/publishedAdventureRegistry_mjs_1/);
  assert.match(controller,/adapter\.adjudicateCheck/);
  assert.match(bridge,/resolvePublishedAdventureCheck/);
});
