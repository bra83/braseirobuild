import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createNpc, upsertNpc, scheduleWorldEvent, advanceWorld, causalEncounterCandidates, resolveWorldHook } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('evento concluído gera consequência causal persistente no local do PNPC',()=>{
  let w=createWorld();
  const a=createLocation({id:'loc_a',name:'Base'}), b=createLocation({id:'loc_b',name:'Doca'}); w=upsertLocation(upsertLocation(w,a),b);
  const npc=createNpc({name:'Aris',locationId:a.id,goal:'Vigiar'}); w=upsertNpc(w,npc);
  w=scheduleWorldEvent(w,{type:'move_npc',dueTick:1,payload:{npcId:npc.id,locationId:b.id},dedupeKey:'aris-dock'}).world;
  const r=advanceWorld(w,{ticks:1,maxEvents:10});
  assert.equal(r.hooksCreated.length,1);
  const candidates=causalEncounterCandidates(r.world,b.id);
  assert.equal(candidates.length,1);
  assert.equal(candidates[0].npc.id,npc.id);
  assert.match(candidates[0].summary,/Aris chegou a Doca/i);
});

test('consequência causal pode ser resolvida uma única vez e some dos candidatos',()=>{
  let w=createWorld(); const loc=createLocation({id:'loc_h',name:'Hangar'}); w=upsertLocation(w,loc);
  const npc=createNpc({name:'Kara',locationId:loc.id}); w=upsertNpc(w,npc);
  w=scheduleWorldEvent(w,{type:'set_npc_goal',dueTick:1,payload:{npcId:npc.id,goal:'Roubar códigos'}}).world;
  w=advanceWorld(w,{ticks:1}).world;
  const hook=causalEncounterCandidates(w,loc.id)[0];
  w=resolveWorldHook(w,hook.hookId,{resolution:'Kara confrontou o grupo.'});
  assert.equal(causalEncounterCandidates(w,loc.id).length,0);
  assert.throws(()=>resolveWorldHook(w,hook.hookId),/já foi resolvida/i);
});

test('Mestre move a cena apenas para local existente e recebe consequências causais no contexto',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'r16-causal-master'));
  const loc=game.addWorldLocation({name:'Doca 12',knownToPlayer:true});
  const npc=game.addWorldNpc({name:'Brel',locationId:loc.id,goal:'Esperar contato'});
  game.scheduleWorldEvent({type:'set_npc_status',dueTick:1,payload:{npcId:npc.id,status:'alarmado'}});
  game.advanceWorldTime({ticks:1});
  game.prepareMasterContext('Chegamos à doca.');
  game.applyMasterProposal({sceneLocationId:loc.id,narration:'A doca está tensa.'});
  const prepared=game.prepareMasterContext('Observo a doca.');
  assert.equal(prepared.context.scene.locationId,loc.id);
  assert.equal(prepared.context.causalEncounters.length,1);
  const before=JSON.stringify(game.state);
  assert.throws(()=>game.applyMasterProposal({sceneLocationId:'loc_inexistente'}),/Local da cena não encontrado/i);
  assert.equal(JSON.stringify(game.state),before);
});

test('Mestre pode narrar e resolver a consequência no mesmo fluxo transacional',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'r16-resolve-hook'));
  const loc=game.addWorldLocation({name:'Posto avançado'}); const npc=game.addWorldNpc({name:'Nera',locationId:loc.id});
  game.scheduleWorldEvent({type:'set_npc_goal',dueTick:1,payload:{npcId:npc.id,goal:'Pedir ajuda'}}); game.advanceWorldTime({ticks:1});
  game.prepareMasterContext('Entro no posto.'); game.applyMasterProposal({sceneLocationId:loc.id,narration:'Nera corre até você.'});
  const hook=game.causalEncounterCandidates()[0];
  game.prepareMasterContext('Falo com Nera.');
  game.applyMasterProposal({narration:'Nera explica o problema.',worldOperations:[{type:'resolve_hook',hookId:hook.hookId,resolution:'O pedido de ajuda foi apresentado ao jogador.'}]});
  assert.equal(game.causalEncounterCandidates().length,0);
  assert.equal(game.state.world.hooks.find(h=>h.id===hook.hookId).status,'resolved');
});
