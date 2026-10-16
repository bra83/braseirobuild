import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createFaction, upsertFaction, createFactionPlan, scheduleWorldEvent, advanceWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('50 facções com planos de 3 etapas avançam por 200 eventos com orçamento e sem duplicar consequências',()=>{
  let w=createWorld(); const loc=createLocation({id:'loc_plan_stress',name:'Setor de planos'}); w=upsertLocation(w,loc);
  const plans=[];
  for(let i=0;i<50;i++){
    const f=createFaction({name:`Facção Plano ${i}`}); w=upsertFaction(w,f); const c=createFactionPlan(w,f.id,{title:`Plano ${i}`,steps:[{text:'Etapa A',locationId:loc.id},{text:'Etapa B',locationId:loc.id},{text:'Etapa C',locationId:loc.id}]}); w=c.world; plans.push([f.id,c.plan.id]);
    for(let n=0;n<4;n++) w=scheduleWorldEvent(w,{type:'advance_faction_plan',dueTick:1,payload:{factionId:f.id,planId:c.plan.id},dedupeKey:`plan:${f.id}:${n}`}).world;
  }
  let r=advanceWorld(w,{ticks:1,maxEvents:25}); assert.equal(r.budgetUsed,25); let cur=r.world;
  while(cur.events.some(e=>e.status==='pending')) cur=advanceWorld(cur,{ticks:0,maxEvents:25}).world;
  for(const [fid,pid] of plans){ const p=cur.factions[fid].plans.find(x=>x.id===pid); assert.equal(p.status,'completed'); assert.equal(p.steps.every(x=>x.status==='completed'),true); }
  assert.equal(cur.hooks.length,200); assert.equal(new Set(cur.hooks.map(h=>h.dedupeKey)).size,200);
});
