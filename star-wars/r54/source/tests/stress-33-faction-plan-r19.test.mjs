import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('33 sessões simuladas preservam plano de facção, afiliação do PNPC e avanço explícito após reaberturas',()=>{
  const storage=new MemoryStorage(); const key='r19-33-faction-plan'; let g=new GameController(new LocalStorageRepository(storage,key)); g.loadStarterCharacter('zal');
  const loc=g.addWorldLocation({name:'Setor Lambda',knownToPlayer:true}); const f=g.addWorldFaction({name:'Célula Aurora',knownToPlayer:true}); const npc=g.addWorldNpc({name:'Agente Nera',locationId:loc.id,factionId:f.id});
  const steps=Array.from({length:20},(_,i)=>({text:`Etapa ${i+1}`,locationId:loc.id,knownToPlayer:i<2})); const plan=g.addWorldFactionPlan(f.id,{title:'Operação Horizonte',steps,knownToPlayer:true});
  let advances=0;
  for(let i=1;i<=33;i++){
    g.startSession();
    if(i<=21){ g.scheduleWorldEvent({type:'advance_faction_plan',dueTick:g.state.world.clock.tick+1,payload:{factionId:f.id,planId:plan.id},dedupeKey:`horizonte:${i}`}); g.advanceWorldTime({ticks:1,maxEvents:10}); advances++; }
    g.closeSession({summary:`Sessão de plano ${i}.`});
    if(i%4===0) g=new GameController(new LocalStorageRepository(storage,key));
    assert.equal(g.state.world.npcs[npc.id].factionId,f.id); assert.equal(g.state.world.factions[f.id].plans[0].id,plan.id);
  }
  g=new GameController(new LocalStorageRepository(storage,key)); const final=g.state.world.factions[f.id].plans[0];
  assert.equal(g.state.sessionHistory.length,33); assert.equal(final.status,'completed'); assert.equal(final.steps.every(s=>s.status==='completed'),true); assert.equal(advances,21);
  assert.equal(g.state.world.hooks.filter(h=>h.planId===plan.id).length,21); assert.equal(new Set(g.state.world.hooks.filter(h=>h.planId===plan.id).map(h=>h.dedupeKey)).size,21);
});
