import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage {
  constructor(){ this.m=new Map(); }
  getItem(k){ return this.m.has(k)?this.m.get(k):null; }
  setItem(k,v){ this.m.set(k,String(v)); }
  removeItem(k){ this.m.delete(k); }
}

function game(storage,key='p63'){
  return new GameController(new LocalStorageRepository(storage,key));
}
function advanceToShowdown(g){
  for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-trap','through-offices','enter-abandoned','face-kaato']) g.advancePublishedAdventure(id);
}
function setupMixed(storage,key='p63'){
  const g=game(storage,key);
  g.loadStarterCharacter('cael');
  g.setCampaignMode('MIXED');
  const group=structuredClone(g.state.group);
  group.dutyEntries=[{id:'d-p63',member:g.state.player.character.name,type:'Apoio',value:11}];
  group.obligationEntries=[{id:'o-p63',member:g.state.player.character.name,type:'Dívida',description:'Teste P63',value:12}];
  g.commit('GROUP_UPDATED',{group});
  g.initializeLivingSandbox({startingPoint:'Coruscant — Setor 943',regionLabel:'Coruscant'});
  g.startPublishedAdventureSession('under-black-sun',{rng:()=>0.5});
  return g;
}

test('P63: save/reopen preserva encontro publicado ativo e impede avanço de cena concorrente',()=>{
  const storage=new MemoryStorage();
  let g=setupMixed(storage,'p63-active');
  advanceToShowdown(g);
  g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});
  const nodeBefore=g.state.world.publishedAdventure.currentNodeId;
  assert.equal(g.state.combat.active,true);
  assert.equal(g.state.combat.publishedAdventureContext.encounterId,'kaato-showdown');

  g=game(storage,'p63-active');
  assert.equal(g.state.combat.active,true);
  assert.equal(g.state.world.publishedAdventure.systems.encounters.activeId,'kaato-showdown');
  assert.throws(()=>g.advancePublishedAdventure('kaato-captured'),/Encerre o encontro/i);
  assert.equal(g.state.world.publishedAdventure.currentNodeId,nodeBefore);
  assert.equal(g.state.combat.active,true);
});

test('P63: finishCombat valida contexto antes de salvar e não deixa estado parcial em divergência',()=>{
  const storage=new MemoryStorage();
  let g=setupMixed(storage,'p63-divergent');
  advanceToShowdown(g);
  g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});
  const repo=new LocalStorageRepository(storage,'p63-divergent');
  const broken=structuredClone(g.state);
  broken.world.publishedAdventure.currentNodeId='epilogue';
  repo.save(broken);
  g=game(storage,'p63-divergent');
  assert.throws(()=>g.finishCombat({rng:()=>0.5}),/cena da aventura mudou/i);
  const persisted=repo.load();
  assert.equal(persisted.combat.active,true);
  assert.equal(persisted.combat.publishedAdventureContext.encounterId,'kaato-showdown');
});

test('P63: combate retomado após reopen conclui uma vez, recompensa uma vez e transita ao mundo livre sem perder MIXED/Duty/Obligation',()=>{
  const storage=new MemoryStorage();
  let g=setupMixed(storage,'p63-full');
  const dutyBefore=structuredClone(g.state.group.dutyEntries);
  advanceToShowdown(g);
  g.beginPublishedAdventureEncounter(null,{rng:()=>0.5});

  g=game(storage,'p63-full');
  const enemy=g.state.combat.adversaries[0];
  enemy.wounds.current=enemy.wounds.threshold+1;
  enemy.status='incapacitated';
  g.state.combat.encounter.outcome='adversaries_defeated';
  g.finishCombat({rng:()=>0.5});

  g=game(storage,'p63-full');
  const history=g.state.world.publishedAdventure.systems.encounters.history.filter(x=>x.encounterId==='kaato-showdown');
  assert.equal(history.length,1);
  assert.equal(g.publishedAdventureStatus().node.choices.some(x=>x.id==='kaato-captured'),true);
  const creditsBefore=g.state.player.character.credits;
  const done=g.advancePublishedAdventure('kaato-captured');
  assert.equal(done.adventure.completed,true);
  assert.equal(done.rewards.credits,1000);
  assert.equal(g.state.player.character.credits,creditsBefore+1000);
  assert.equal(g.state.group.obligationEntries.find(x=>x.id==='o-p63').value,7);
  assert.deepEqual(g.state.group.dutyEntries,dutyBefore);
  const consequences=structuredClone(g.state.world.publishedAdventureConsequences['under-black-sun']);

  g=game(storage,'p63-full');
  assert.equal(g.publishedAdventureStatus().completed,true);
  assert.equal(g.state.player.character.credits,creditsBefore+1000);
  assert.equal(g.state.group.obligationEntries.find(x=>x.id==='o-p63').value,7);
  assert.deepEqual(g.state.group.dutyEntries,dutyBefore);

  const continued=g.continuePublishedAdventureToSandbox({rng:()=>0.5});
  assert.equal(continued.continued,true);
  assert.equal(continued.adventure.archived,true);
  assert.deepEqual(g.state.world.publishedAdventureConsequences['under-black-sun'],consequences);
  assert.equal(g.state.campaignMode,'MIXED');
  assert.equal(g.state.group.obligationEntries.find(x=>x.id==='o-p63').value,7);
  assert.deepEqual(g.state.group.dutyEntries,dutyBefore);

  g=game(storage,'p63-full');
  assert.equal(g.publishedAdventureStatus().archived,true);
  assert.equal(g.state.world.sandbox.initialized,true);
  assert.deepEqual(g.state.world.publishedAdventureConsequences['under-black-sun'],consequences);
  const eventsBefore=g.state.events.length;
  const second=g.continuePublishedAdventureToSandbox({rng:()=>0.5});
  assert.equal(second.continued,true);
  assert.equal(second.alreadyArchived,true);
  assert.equal(g.state.events.length,eventsBefore);
});
