import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createNpc, upsertNpc, recordNpcFact, scheduleWorldEvent, advanceWorld, masterWorldSummary, normalizeWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('mundo v2 preserva objetivo, relação, fatos privados e locais',()=>{
  let w=createWorld(); const loc=createLocation({id:'loc_hangar',name:'Hangar',knownToPlayer:true}); w=upsertLocation(w,loc);
  const npc=createNpc({name:'Kara',locationId:loc.id,goal:'Conseguir códigos',relation:'aliada'}); w=upsertNpc(w,npc);
  w=recordNpcFact(w,npc.id,{text:'Trabalha para Inteligência',knownToPlayer:false});
  const sum=masterWorldSummary(w); const k=sum.npcs.find(n=>n.id===npc.id);
  assert.equal(k.goal,'Conseguir códigos'); assert.equal(k.relation,'aliada'); assert.equal(k.facts[0].knownToPlayer,false); assert.equal(sum.locations[0].name,'Hangar');
});

test('agenda do mundo executa eventos vencidos por orçamento e sem duplicação',()=>{
  let w=createWorld(); const npc=createNpc({name:'Kara',locationId:'A'}); w=upsertNpc(w,npc);
  let a=scheduleWorldEvent(w,{type:'move_npc',dueTick:2,payload:{npcId:npc.id,locationId:'B'},dedupeKey:'kara_move_b'}); w=a.world;
  const dup=scheduleWorldEvent(w,{type:'move_npc',dueTick:2,payload:{npcId:npc.id,locationId:'C'},dedupeKey:'kara_move_b'}); assert.equal(dup.created,false); w=dup.world;
  const r1=advanceWorld(w,{ticks:1,maxEvents:50}); assert.equal(r1.processed.length,0); assert.equal(r1.world.npcs[npc.id].locationId,'A');
  const r2=advanceWorld(r1.world,{ticks:1,maxEvents:50}); assert.equal(r2.processed.length,1); assert.equal(r2.world.npcs[npc.id].locationId,'B'); assert.equal(r2.world.events[0].status,'done');
});

test('evento inválido falha isoladamente e não impede os demais',()=>{
  let w=createWorld(); const npc=createNpc({name:'Kara',locationId:'A'}); w=upsertNpc(w,npc);
  w=scheduleWorldEvent(w,{type:'move_npc',dueTick:1,payload:{npcId:'inexistente',locationId:'X'}}).world;
  w=scheduleWorldEvent(w,{type:'set_npc_goal',dueTick:1,payload:{npcId:npc.id,goal:'Voltar à base'}}).world;
  const r=advanceWorld(w,{ticks:1,maxEvents:10});
  assert.equal(r.processed.length,2); assert.equal(r.processed.filter(x=>x.status==='failed').length,1); assert.equal(r.world.npcs[npc.id].goal,'Voltar à base');
});

test('orçamento limita processamento de centenas de eventos sem perder pendências',()=>{
  let w=createWorld();
  for(let i=0;i<300;i++){ const npc=createNpc({name:`NPC ${i}`,locationId:'A'}); w=upsertNpc(w,npc); w=scheduleWorldEvent(w,{type:'move_npc',dueTick:1,payload:{npcId:npc.id,locationId:'B'},dedupeKey:`move_${npc.id}`}).world; }
  const first=advanceWorld(w,{ticks:1,maxEvents:50}); assert.equal(first.processed.length,50); assert.equal(first.pendingDue,250);
  let cur=first.world; for(let i=0;i<5;i++) cur=advanceWorld(cur,{ticks:0,maxEvents:50}).world;
  const summary=masterWorldSummary(cur); assert.equal(summary.npcs.length,300); assert.equal(summary.npcs.filter(n=>n.locationId==='B').length,300); assert.equal(cur.events.filter(e=>e.status==='pending').length,0);
});

test('normalização migra aliases antigos relationship/objective/knownFacts',()=>{
  const legacy={schemaVersion:1,npcs:{n1:{id:'n1',name:'Velho',relationship:'hostil',objective:'Escapar',knownFacts:['Viu o herói']}},locations:{},clock:{tick:3},events:[]};
  const w=normalizeWorld(legacy); assert.equal(w.schemaVersion,6); assert.equal(w.npcs.n1.relation,'hostil'); assert.equal(w.npcs.n1.goal,'Escapar'); assert.equal(w.npcs.n1.facts[0].knownToPlayer,true);
});
