import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import {
  startUnderBlackSun, underBlackSunStatus, advanceUnderBlackSun,
  adjudicateUnderBlackSunCheck, underBlackSunEncounterSpec, recordUnderBlackSunEncounterOutcome
} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function makeGame(key='p36'){ const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key)); g.loadStarterCharacter('cael'); return g; }
function step(world,id){ return advanceUnderBlackSun(world,id).world; }
function routeToSpyder(world){ world=step(world,'escape-clean'); world=step(world,'begin-investigation'); return step(world,'go-spyder'); }
function routeToNovaCore(world){ world=routeToSpyder(world); world=step(world,'spyder-return'); world=step(world,'go-zelcomm'); world=step(world,'zelcomm-clean'); world=step(world,'move-endgame'); return step(world,'plan-direct'); }
function advanceGame(game,ids){ for(const id of ids) game.advancePublishedAdventure(id); }

test('P36: save P35 sem systems migra sem resetar nó, flags ou histórico',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w.publishedAdventure.currentNodeId='investigation-hub';
  w.publishedAdventure.flags={alertClock:3,momentum:2,customFlag:true};
  w.publishedAdventure.checkHistory=[{checkId:'legacy'}];
  delete w.publishedAdventure.systems;
  const s=underBlackSunStatus(w);
  assert.equal(s.progress.currentNodeId,'investigation-hub');
  assert.equal(s.progress.flags.customFlag,true);
  assert.equal(s.progress.checkHistory[0].checkId,'legacy');
  assert.ok(Array.isArray(s.progress.systems));
});

test('P36: perseguição usa consequência narrativa persistente e superfície anti-spoiler',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  const r=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:2,triumph:0,despair:0}});
  w=r.world;
  assert.equal(r.adjudication.chase.lead,2);
  assert.equal(r.adjudication.chase.status,'vantagem');
  const s=underBlackSunStatus(w);
  assert.deepEqual(s.progress.systems.map(x=>x.id),['chase']);
  assert.equal('sabacc' in s.progress.systems,false);
  assert.equal('novacore' in s.progress.systems,false);
});

test('P36: sabacc acumula rodadas e créditos de modo persistente',()=>{
  let w=routeToSpyder(startUnderBlackSun(createWorld()).world);
  let r;
  for(let i=0;i<3;i++){ r=adjudicateUnderBlackSunCheck(w,{checkIndex:3,result:{success:true,netSuccess:1,netAdvantage:2,triumph:0,despair:0}}); w=r.world; }
  assert.equal(r.adjudication.sabacc.rounds,3);
  assert.equal(r.adjudication.sabacc.creditsDelta,225);
  assert.equal(r.adjudication.sabacc.status,'won');
  assert.equal(w.publishedAdventure.flags.spyderSabaccWon,true);
});

test('P36: Zelcomm entra em lockdown e NovaCORE pode escalar risco + Tensão',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean'); w=step(w,'begin-investigation'); w=step(w,'go-zelcomm');
  let z=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
  assert.equal(z.adjudication.zelcomm.lockdown,true);
  assert.ok(z.adjudication.zelcomm.alert>=4);
  assert.equal(z.world.publishedAdventure.flags.zelcommAlarmed,true);

  w=routeToNovaCore(startUnderBlackSun(createWorld()).world);
  const n=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
  assert.equal(n.adjudication.novacore.critical,true);
  assert.ok(n.adjudication.novacore.hazard>=4);
  assert.equal(n.adjudication.playerStrainDelta,3);
  assert.equal(n.world.publishedAdventure.flags.novacoreHazardEscalated,true);
});

test('P36: perfis Black Sun e Kaa’to existem na autoridade de combate usada pelo controller',()=>{
  const blackSun=createAdversary('black_sun_enforcer',{count:3,range:'Médio'});
  assert.equal(blackSun.kind,'minion');
  assert.equal(blackSun.membersTotal,3);
  const kaato=createAdversary('kaato_leeachos',{range:'Médio'});
  assert.equal(kaato.kind,'nemesis');
  assert.equal(kaato.strain.threshold,13);
});

test('P36: controller inicia encontro publicado e grava desfecho persistente',()=>{
  const g=makeGame('p36-black-sun');
  g.startPublishedAdventureSession();
  const spec=underBlackSunEncounterSpec(g.state.world);
  assert.equal(spec.id,'black-sun-pursuers');
  const started=g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});
  assert.equal(started.combat.active,true);
  assert.equal(started.combat.adversaries[0].profileKey,'black_sun_enforcer');
  assert.equal(g.state.combat.publishedAdventureContext.encounterId,'black-sun-pursuers');
  g.state.combat.encounter.outcome='adversaries_defeated';
  const done=g.finishCombat({rng:()=>0.5});
  assert.equal(done.publishedAdventureOutcome.encounterId,'black-sun-pursuers');
  assert.equal(g.publishedAdventureStatus().encounter.status,'resolved');
  assert.equal(g.state.world.publishedAdventure.flags.blackSunPursuitFought,true);
});

test('P36: Kaa’to usa NPC do mundo, persiste incapacitação e não reaparece íntegro',()=>{
  const g=makeGame('p36-kaato');
  g.startPublishedAdventureSession();
  advanceGame(g,['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-direct','through-offices','enter-abandoned','face-kaato']);
  assert.equal(g.publishedAdventureStatus().progress.currentNodeId,'showdown');
  const started=g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});
  const a=started.combat.adversaries[0];
  assert.equal(a.worldNpcId,'uabs.npc.kaato');
  assert.equal(a.id,'uabs.npc.kaato');
  g.state.combat.adversaries[0].wounds.current=g.state.combat.adversaries[0].wounds.threshold+1;
  g.state.combat.adversaries[0].status='incapacitated';
  g.state.combat.encounter.outcome='adversaries_defeated';
  g.finishCombat({rng:()=>0.5});
  assert.equal(g.state.world.npcs['uabs.npc.kaato'].status,'incapacitado');
  assert.equal(g.state.world.publishedAdventure.flags.kaatoNeutralized,true);
  assert.equal(g.publishedAdventureStatus().encounter.status,'resolved');
});

test('P36: consequência de sabacc atinge créditos reais do personagem pelo controller',()=>{
  const g=makeGame('p36-sabacc-credits');
  g.startPublishedAdventureSession();
  advanceGame(g,['escape-clean','begin-investigation','go-spyder']);
  const before=Number(g.state.player.character.credits)||0;
  const r=g.resolvePublishedAdventureCheck(3,{rng:()=>0.5});
  const delta=Number(r.characterEffects.creditsDelta)||0;
  assert.notEqual(delta,0);
  assert.equal(Number(g.state.player.character.credits),Math.max(0,before+delta));
});

test('P36: mantém gate EotE/MIXED e não converte Duty/Obligation entre linhas',()=>{
  const aor=makeGame('p36-aor');
  aor.setCampaignMode('AOR');
  assert.throws(()=>aor.startPublishedAdventureSession(),/EotE ou MISTA/i);
  const mixed=makeGame('p36-mixed');
  mixed.setCampaignMode('MIXED');
  assert.equal(mixed.startPublishedAdventureSession().adventure.active,true);
  assert.equal(mixed.state.campaignMode,'MIXED');
});

test('P36: runtime e bridge expõem sistemas/encontro sem revelar especificação futura',()=>{
  const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.match(runtime,/final-published-systems/);
  assert.match(runtime,/data-published-encounter/);
  assert.match(runtime,/BraseiroPublishedActionClient\.execute\('encounter'/);
  assert.match(bridge,/beginPublishedAdventureEncounter/);
  assert.match(bridge,/PUBLISHED_ADVENTURE_ENCOUNTER_FAILED/);
});

test('P37: epílogo persiste destino de Kaa’to e consequências de facções sem spoiler antecipado',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean'); w=step(w,'begin-investigation'); w=step(w,'go-spyder'); w=step(w,'spyder-warn-black-sun'); w=step(w,'go-zelcomm'); w=step(w,'zelcomm-clean'); w=step(w,'move-endgame'); w=step(w,'plan-direct'); w=step(w,'through-offices'); w=step(w,'enter-abandoned'); w=step(w,'face-kaato');
  assert.equal(w.publishedAdventureConsequences,undefined);
  w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:'ended',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:'active'}]}).world;
  w=step(w,'kaato-vanishes');
  assert.equal(w.npcs['uabs.npc.kaato'].status,'desaparecido');
  assert.equal(w.publishedAdventureConsequences['under-black-sun'].kaatoRecurring,true);
  assert.ok(w.factions['uabs.fac.black_sun'].facts.some(x=>/Verannis/.test(x)));
  assert.equal(underBlackSunStatus(w).progress.consequencesApplied,true);
});

test('P37: roubo Zelcomm adiciona Obligation antes da recompensa e mantém Duty separado',()=>{
  const g=makeGame('p37-obligation'); g.setCampaignMode('MIXED');
  const group=structuredClone(g.state.group); group.obligationEntries=[{id:'o1',member:g.state.player.character.name,type:'Dívida',value:10}]; group.dutyEntries=[{id:'d1',member:g.state.player.character.name,type:'Apoio',value:7}]; g.commit('GROUP_UPDATED',{group});
  g.startPublishedAdventureSession(); advanceGame(g,['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-weapons','move-endgame','plan-direct','through-offices','enter-abandoned','face-kaato']);
  g.commit('WORLD_UPDATED',{world:recordUnderBlackSunEncounterOutcome(g.state.world,{encounterId:'kaato-showdown',outcome:'adversaries_defeated',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:'incapacitated'}]}).world});
  const done=g.advancePublishedAdventure('kaato-captured');
  assert.equal(done.rewards.obligationAdded,5); assert.equal(done.rewards.obligationReduced,5);
  assert.equal(g.state.group.obligationEntries[0].value,10);
  assert.equal(g.state.group.dutyEntries[0].value,7);
});

test('P39: epílogo integra consequências ao Mundo Vivo somente após o desfecho',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean'); w=step(w,'begin-investigation'); w=step(w,'go-spyder'); w=step(w,'spyder-warn-black-sun'); w=step(w,'go-zelcomm'); w=step(w,'zelcomm-weapons'); w=step(w,'move-endgame'); w=step(w,'plan-direct'); w=step(w,'through-offices'); w=step(w,'enter-abandoned'); w=step(w,'face-kaato');
  assert.equal(w.publishedAdventureWorldPlans,undefined);
  assert.equal(w.livingDepth,undefined);
  w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:'ended',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:'active'}]}).world;
  w=step(w,'kaato-vanishes');
  assert.equal(w.livingDepth.npcs['uabs.npc.kaato'].lifeState,'missing');
  assert.ok(w.livingDepth.npcs['uabs.npc.kaato'].memory.some(m=>/NovaCORE/.test(m.summary)));
  assert.ok(w.livingDepth.factions['uabs.fac.pykes'].relations['player-group']>0);
  assert.ok(w.livingDepth.factions['uabs.fac.zelcomm'].relations['player-group']<0);
  assert.ok(w.publishedAdventureWorldPlans['under-black-sun'].some(p=>p.id==='kaato-return'));
  assert.ok(w.publishedAdventureWorldPlans['under-black-sun'].some(p=>p.id==='zelcomm-retaliation'));
  assert.equal(w.publishedAdventureConsequences['under-black-sun'].livingWorldApplied,true);
});

test('P39: Kaa’to morto sincroniza estado profundo e não cria plano de retorno',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-direct','through-offices','enter-abandoned','face-kaato']) w=step(w,id);
  w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:'ended',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:'incapacitated'}]}).world;
  w=step(w,'kaato-killed');
  assert.equal(w.livingDepth.npcs['uabs.npc.kaato'].lifeState,'dead');
  assert.equal(w.publishedAdventureWorldPlans['under-black-sun'].some(p=>p.id==='kaato-return'),false);
});
