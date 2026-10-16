import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import {
  UNDER_BLACK_SUN,
  startUnderBlackSun,
  advanceUnderBlackSun,
  adjudicateUnderBlackSunCheck,
  underBlackSunStatus,
  underBlackSunEncounterSpec,
} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
const step=(w,id)=>advanceUnderBlackSun(w,id).world;
function makeGame(key='p46'){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key));g.loadStarterCharacter('cael');return g}
function toEndgame(world){
  for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame']) world=step(world,id);
  return world;
}
function advanceGame(g,ids){for(const id of ids)g.advancePublishedAdventure(id)}

test('P46: grafo publicado não tem destinos quebrados e só o epílogo é terminal',()=>{
  const ids=new Set(Object.keys(UNDER_BLACK_SUN.nodes));
  for(const [nodeId,node] of Object.entries(UNDER_BLACK_SUN.nodes)){
    for(const choice of node.choices||[]) assert.ok(ids.has(choice.to),`${nodeId}/${choice.id} aponta para ${choice.to}`);
    if(nodeId==='epilogue') assert.equal((node.choices||[]).length,0);
    else assert.ok((node.choices||[]).length>0,`${nodeId} não pode ser terminal`);
  }
});

test('P46: rota NovaCORE muda a apresentação sem revelar rota não escolhida',()=>{
  let w=toEndgame(startUnderBlackSun(createWorld()).world);
  w=step(w,'plan-direct');
  w=step(w,'through-offices');
  let s=underBlackSunStatus(w);
  assert.match(s.node.title,/escritórios/i);
  assert.doesNotMatch(s.node.narration,/passarelas quentes/i);
  assert.ok(s.knownClues.some(x=>x.id==='novacore-route'&&/ESCRITÓRIOS/.test(x.title)));

  w=toEndgame(startUnderBlackSun(createWorld()).world);
  w=step(w,'plan-direct');
  w=step(w,'through-refinery');
  s=underBlackSunStatus(w);
  assert.match(s.node.title,/refinaria/i);
  assert.doesNotMatch(s.node.narration,/Terminais corporativos/i);
  assert.ok(s.knownClues.some(x=>x.id==='novacore-route'&&/REFINARIA/.test(x.title)));
});

test('P46: negociação final permite concluir sem combate somente após teste bem-sucedido',()=>{
  let w=toEndgame(startUnderBlackSun(createWorld()).world);
  for(const id of ['plan-direct','through-refinery','enter-abandoned','parley-kaato']) w=step(w,id);
  let s=underBlackSunStatus(w);
  assert.equal(s.progress.currentNodeId,'kaato-parley');
  assert.equal(s.node.choices.some(x=>x.id==='kaato-surrenders'),false);
  const r=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:1,triumph:0,despair:0}});
  w=r.world;
  s=underBlackSunStatus(w);
  assert.equal(s.node.choices.some(x=>x.id==='kaato-surrenders'),true);
  w=step(w,'kaato-surrenders');
  s=underBlackSunStatus(w);
  assert.equal(s.completed,true);
  assert.equal(s.progress.flags.kaatoOutcome,'captured');
  assert.equal(s.progress.flags.resolutionMode,'negotiation');
  assert.equal((w.publishedAdventure.systems.encounters.history||[]).some(x=>x.encounterId==='kaato-showdown'),false);
});

test('P46: combate final não permite escolher destino antes do encontro e preparação altera abertura',()=>{
  let w=toEndgame(startUnderBlackSun(createWorld()).world);
  for(const id of ['plan-trap','through-offices','enter-abandoned','face-kaato']) w=step(w,id);
  const s=underBlackSunStatus(w);
  assert.equal(s.progress.currentNodeId,'showdown');
  assert.equal(s.node.choices.length,0);
  assert.equal(s.encounter.available,true);
  assert.match(s.encounter.preparation,/Armadilha/i);
  const spec=underBlackSunEncounterSpec(w);
  assert.equal(spec.adversaries[0].range,'Curto');
  assert.equal(spec.adversaries[0].preWounds,2);
});

test('P46: playthrough controller fecha missão, persiste recompensa e continua no sandbox sem apagar MIXED/Duty',()=>{
  const g=makeGame('p46-controller');
  g.setCampaignMode('MIXED');
  const group=structuredClone(g.state.group);
  group.dutyEntries=[{id:'d-p46',member:g.state.player.character.name,type:'Apoio',value:11}];
  g.commit('GROUP_UPDATED',{group});
  const dutyBefore=structuredClone(g.state.group.dutyEntries);
  g.startPublishedAdventureSession();
  advanceGame(g,['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-trap','through-offices','enter-abandoned','face-kaato']);
  assert.equal(g.publishedAdventureStatus().node.choices.length,0);
  const started=g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});
  assert.equal(started.combat.adversaries[0].range,'Curto');
  assert.equal(started.combat.adversaries[0].wounds.current,2);
  g.state.combat.adversaries[0].wounds.current=g.state.combat.adversaries[0].wounds.threshold+1;
  g.state.combat.adversaries[0].status='incapacitated';
  g.state.combat.encounter.outcome='adversaries_defeated';
  g.finishCombat({rng:()=>0.5});
  assert.equal(g.publishedAdventureStatus().node.choices.some(x=>x.id==='kaato-captured'),true);
  const done=g.advancePublishedAdventure('kaato-captured');
  assert.equal(done.adventure.completed,true);
  assert.equal(done.rewards.credits,1000);
  assert.equal(g.state.world.publishedAdventureRewards['under-black-sun'].credits,1000);
  assert.ok(done.adventure.epilogueSummary?.lines?.length>=4);
  const consequence=structuredClone(g.state.world.publishedAdventureConsequences['under-black-sun']);
  const continued=g.continuePublishedAdventureToSandbox({rng:()=>0.5});
  assert.equal(continued.continued,true);
  assert.equal(continued.adventure.archived,true);
  assert.equal(g.state.world.sandbox.initialized,true);
  assert.deepEqual(g.state.world.publishedAdventureConsequences['under-black-sun'],consequence);
  assert.equal(g.state.campaignMode,'MIXED');
  assert.deepEqual(g.state.group.dutyEntries,dutyBefore);
});

test('P46: UI diferencia bloqueio ativo de missão concluída e oferece continuar no mundo livre',()=>{
  const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.match(runtime,/Resolva o encontro ou o teste desta cena/);
  assert.match(runtime,/CONTINUAR NO MUNDO LIVRE/);
  assert.match(runtime,/MISSÃO CONCLUÍDA/);
  assert.match(runtime,/continuePublishedAdventureToSandbox/);
  assert.match(bridge,/continuePublishedAdventureToSandbox/);
});
