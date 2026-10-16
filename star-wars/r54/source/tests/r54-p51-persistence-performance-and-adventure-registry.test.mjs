import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign, migrateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import {
  startUnderBlackSun,
  advanceUnderBlackSun,
  adjudicateUnderBlackSunCheck,
  underBlackSunStatus,
  currentUnderBlackSunMasterRequest
} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import {
  publishedAdventureCatalog,
  getPublishedAdventureAdapter,
  publishedAdventureAdapterSnapshot
} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

class MemoryStorage {
  constructor(){ this.m=new Map(); }
  getItem(k){ return this.m.get(k) ?? null; }
  setItem(k,v){ this.m.set(k,String(v)); }
  removeItem(k){ this.m.delete(k); }
}
const step=(w,id)=>advanceUnderBlackSun(w,id).world;
function toParley(){
  let w=startUnderBlackSun(createWorld()).world;
  for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-direct','through-refinery','enter-abandoned','parley-kaato']) w=step(w,id);
  return w;
}
function toNovacorePlant(){
  let w=startUnderBlackSun(createWorld()).world;
  for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-direct','through-refinery']) w=step(w,id);
  return w;
}
const reopenWorld=w=>JSON.parse(JSON.stringify(w));

test('P51 WORLD_UPDATED mantém estado completo mas histórico de eventos guarda somente resumo compacto e idempotente',()=>{
  const storage=new MemoryStorage();
  let g=new GameController(new LocalStorageRepository(storage,'p51-events'));
  g.loadStarterCharacter('cael');
  const location=g.addWorldLocation({name:'P51 Dock',knownToPlayer:true});
  assert.equal(g.state.world.locations[location.id].name,'P51 Dock');
  let evt=g.state.events.at(-1);
  assert.equal(evt.type,'WORLD_UPDATED');
  assert.equal(evt.payload.world.locations,undefined);
  assert.equal(evt.payload.world.counts.locations,1);
  const firstCounts=structuredClone(evt.payload.world.counts);
  g=new GameController(new LocalStorageRepository(storage,'p51-events'));
  evt=g.state.events.findLast(e=>e.type==='WORLD_UPDATED');
  assert.deepEqual(evt.payload.world.counts,firstCounts);
  assert.equal(evt.payload.world.locations,undefined);
  assert.equal(g.state.world.locations[location.id].name,'P51 Dock');
});

test('P51 migrateState compacta snapshot WORLD_UPDATED legado sem alterar o mundo atual',()=>{
  const state=createCampaign('legacy-event');
  state.world.locations.legacy={id:'legacy',name:'Legacy',description:'',knownToPlayer:true,tags:[]};
  state.events.push({id:'legacy-world-event',type:'WORLD_UPDATED',at:new Date().toISOString(),payload:{world:structuredClone(state.world)}});
  state.appliedActionIds.push('legacy-world-event');
  const migrated=migrateState(state);
  assert.equal(migrated.world.locations.legacy.name,'Legacy');
  assert.equal(migrated.events[0].payload.world.locations,undefined);
  assert.equal(migrated.events[0].payload.world.counts.locations,1);
});

test('P51 Desespero em negociação final fracassada encerra a via diplomática, persiste e não oferece rendição',()=>{
  let w=toParley();
  const adjudicated=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
  w=reopenWorld(adjudicated.world);
  let s=underBlackSunStatus(w);
  assert.equal(s.progress.flags.parleyBroken,true);
  assert.equal(s.progress.flags.kaatoAlerted,true);
  assert.equal(s.node.checks.length,0);
  assert.equal(s.node.choices.some(c=>c.id==='kaato-surrenders'),false);
  assert.equal(s.node.choices.some(c=>c.id==='parley-breaks'),true);
  assert.match(s.checkFeedback.at(-1).summary,/negociação se rompeu/i);
  const request=currentUnderBlackSunMasterRequest(w);
  assert.equal(request.checks.length,0);
  assert.equal(request.suggestedActions.some(x=>/rendição/i.test(x)),false);
  w=step(w,'parley-breaks');
  s=underBlackSunStatus(w);
  assert.equal(s.progress.currentNodeId,'showdown');
  assert.equal(s.progress.flags.parleyBroken,true);
});

test('P51 Threat + Despair em NovaCORE produz risco crítico persistente e consequência mecânica explícita',()=>{
  let w=toNovacorePlant();
  const available=underBlackSunStatus(w).node.checks;
  const friezaIndex=available.findIndex(c=>c.skill==='Frieza');
  assert.ok(friezaIndex>=0);
  const r=adjudicateUnderBlackSunCheck(w,{checkIndex:2,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
  assert.equal(r.adjudication.novacore.critical,true);
  assert.equal(r.adjudication.novacore.hazard,4);
  assert.equal(r.adjudication.playerStrainDelta,3);
  w=reopenWorld(r.world);
  const s=underBlackSunStatus(w);
  const system=s.progress.systems.find(x=>x.id==='novacore');
  assert.equal(system.status,'critical');
  assert.equal(system.value,4);
  assert.match(s.checkFeedback.at(-1).summary,/nível crítico/i);
});

test('P51 registry mantém Sob um Sol Negro funcional e expõe contrato genérico para próximas aventuras',()=>{
  const catalog=publishedAdventureCatalog();
  assert.equal(catalog.some(x=>x.id==='under-black-sun'&&x.campaignModes.includes('EOTE')&&x.campaignModes.includes('MIXED')),true);
  assert.equal(catalog.find(x=>x.id==='under-black-sun').campaignModes.includes('AOR'),false);
  const adapter=getPublishedAdventureAdapter('under-black-sun');
  assert.equal(typeof adapter.start,'function');
  assert.equal(typeof adapter.advance,'function');
  assert.equal(typeof adapter.recordEncounterOutcome,'function');
  const snapshot=publishedAdventureAdapterSnapshot('under-black-sun');
  assert.deepEqual(snapshot.capabilities,{checks:true,encounters:true,rewards:true,archive:true});
  assert.throws(()=>getPublishedAdventureAdapter('not-installed'),/desconhecida/i);
});
