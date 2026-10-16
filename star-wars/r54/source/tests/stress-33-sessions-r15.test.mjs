import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('33 sessões simuladas preservam campanha, histórico e PNPC após reaberturas repetidas',()=>{
  const storage=new MemoryStorage(); const key='r15-33-sessions';
  let game=new GameController(new LocalStorageRepository(storage,key));
  game.loadStarterCharacter('zal');
  const base=game.addWorldLocation({name:'Base Echo',knownToPlayer:true});
  const field=game.addWorldLocation({name:'Posto avançado',knownToPlayer:true});
  const npc=game.addWorldNpc({name:'Agente Nera',locationId:base.id,goal:'Manter contato',relation:'aliada'});
  const npcId=npc.id;

  for(let i=1;i<=33;i++){
    game.startSession();
    game.submitIntent(`Sessão ${i}: verificar situação da operação.`);
    game.scheduleWorldEvent({type:'move_npc',dueTick:game.state.world.clock.tick+1,payload:{npcId,locationId:i%2?field.id:base.id},dedupeKey:`nera-move-${i}`});
    game.scheduleWorldEvent({type:'set_npc_goal',dueTick:game.state.world.clock.tick+1,payload:{npcId,goal:`Objetivo persistente ${i}`},dedupeKey:`nera-goal-${i}`});
    const advanced=game.advanceWorldTime({ticks:1,maxEvents:10});
    assert.equal(advanced.pendingDue,0);
    game.closeSession({summary:`Sessão simulada ${i} encerrada.`});
    assert.equal(game.state.sessionHistory.length,i);
    if(i%4===0) game=new GameController(new LocalStorageRepository(storage,key));
    assert.equal(game.state.world.npcs[npcId].id,npcId);
  }

  game=new GameController(new LocalStorageRepository(storage,key));
  assert.equal(game.state.sessionHistory.length,33);
  assert.equal(game.state.session.number,33);
  assert.equal(game.state.world.npcs[npcId].goal,'Objetivo persistente 33');
  assert.equal(game.state.world.npcs[npcId].locationId,field.id);
  assert.equal(game.state.world.events.filter(e=>e.status==='pending').length,0);
  assert.equal(game.state.world.events.filter(e=>e.status==='done').length,66);
  assert.equal(game.state.world.hooks.filter(h=>h.status==='pending').length,66);
  assert.equal(new Set(game.state.world.hooks.map(h=>h.dedupeKey)).size,66);
});
