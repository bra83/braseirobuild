import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createFaction, upsertFaction, scheduleWorldEvent, advanceWorld, masterWorldSummary } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('100 facções e 200 eventos são processados por orçamento sem duplicar consequências',()=>{
  let w=createWorld(); const loc=createLocation({id:'loc_stress',name:'Setor de teste'}); w=upsertLocation(w,loc);
  const ids=[];
  for(let i=0;i<100;i++){
    const f=createFaction({name:`Facção ${i}`,objective:'Aguardar'}); w=upsertFaction(w,f); ids.push(f.id);
    w=scheduleWorldEvent(w,{type:'set_faction_status',dueTick:1,payload:{factionId:f.id,status:'mobilizada',locationId:loc.id},dedupeKey:`status:${f.id}`}).world;
    w=scheduleWorldEvent(w,{type:'add_faction_resource',dueTick:1,payload:{factionId:f.id,resource:`Recurso ${i}`,locationId:loc.id},dedupeKey:`resource:${f.id}`}).world;
  }
  let r=advanceWorld(w,{ticks:1,maxEvents:40}); assert.equal(r.budgetUsed,40); assert.equal(r.pendingDue,160);
  let cur=r.world; while(cur.events.some(e=>e.status==='pending')) cur=advanceWorld(cur,{ticks:0,maxEvents:40}).world;
  const sum=masterWorldSummary(cur); assert.equal(sum.factions.length,100); assert.equal(sum.factions.filter(f=>f.status==='mobilizada').length,100); assert.equal(sum.factions.every(f=>f.resources.length===1),true);
  assert.equal(cur.hooks.filter(h=>h.status==='pending').length,200); assert.equal(new Set(cur.hooks.map(h=>h.dedupeKey)).size,200);
});
