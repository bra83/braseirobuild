import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createNpc, upsertNpc, moveNpc, recordNpcFact, playerVisibleNpc } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('NPC mantém identidade estável ao mudar de local',()=>{
  let w=createWorld(); const npc=createNpc({name:'Kara',locationId:'hangar'}); w=upsertNpc(w,npc); w=moveNpc(w,npc.id,'cantina'); assert.equal(w.npcs[npc.id].id,npc.id); assert.equal(w.npcs[npc.id].locationId,'cantina');
});

test('fatos ocultos do NPC não atravessam a fronteira do jogador',()=>{
  let w=createWorld(); const npc=createNpc({name:'Kara'}); w=upsertNpc(w,npc); w=recordNpcFact(w,npc.id,{text:'É informante imperial',knownToPlayer:false}); w=recordNpcFact(w,npc.id,{text:'Conhece o hangar',knownToPlayer:true}); const visible=playerVisibleNpc(w.npcs[npc.id]); assert.deepEqual(visible.facts.map(x=>x.text),['Conhece o hangar']);
});
