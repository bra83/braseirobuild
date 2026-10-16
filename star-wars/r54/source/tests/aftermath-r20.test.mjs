import test from 'node:test';
import assert from 'node:assert/strict';
import {createWorld,createLocation,upsertLocation,createFaction,upsertFaction,createFactionPlan,scheduleWorldEvent,advanceWorld,recordEncounterOutcome,closeWorldAftermath,activeWorldAftermaths,normalizeWorld} from '../app/src/main/assets/vtt/js/world/world.mjs';

test('encontro causal cria histórico persistente e consequência duradoura ligada a facção/plano/local',()=>{
  let w=createWorld(); const loc=createLocation({name:'Doca',knownToPlayer:true}); w=upsertLocation(w,loc); const fac=createFaction({name:'Império',knownToPlayer:true}); w=upsertFaction(w,fac); const plan=createFactionPlan(w,fac.id,{title:'Tomar a doca',steps:[{text:'Ocupar',locationId:loc.id}],knownToPlayer:true}); w=plan.world;
  const scheduled=scheduleWorldEvent(w,{type:'advance_faction_plan',dueTick:1,payload:{factionId:fac.id,planId:plan.plan.id}}); w=advanceWorld(scheduled.world,{ticks:1}).world; const hook=w.hooks[0];
  const out=recordEncounterOutcome(w,{hookId:hook.id,outcome:'adversaries_defeated',summary:'Patrulha derrotada.',rounds:3,adversaries:[{id:'a1',name:'Patrulha',factionId:fac.id,status:'incapacitated',wounds:9,criticalCount:1}]});
  assert.equal(out.created,true); assert.equal(out.encounter.planId,plan.plan.id); assert.equal(out.encounter.locationId,loc.id); assert.equal(out.aftermath.factionId,fac.id); assert.equal(out.aftermath.status,'active'); assert.equal(out.world.encounterHistory.length,1); assert.equal(out.world.aftermaths.length,1);
});

test('mesmo hook não duplica histórico/aftermath e consequência pode ser encerrada sem apagar histórico',()=>{
  let w=createWorld(); const loc=createLocation({name:'Hangar'}); w=upsertLocation(w,loc); const fac=createFaction({name:'Guarnição'}); w=upsertFaction(w,fac); const plan=createFactionPlan(w,fac.id,{title:'Ocupar',steps:[{text:'Entrar',locationId:loc.id}]}); w=plan.world; w=scheduleWorldEvent(w,{type:'advance_faction_plan',dueTick:1,payload:{factionId:fac.id,planId:plan.plan.id}}).world; w=advanceWorld(w,{ticks:1}).world; const hook=w.hooks[0];
  const first=recordEncounterOutcome(w,{hookId:hook.id,outcome:'adversaries_defeated'}); const second=recordEncounterOutcome(first.world,{hookId:hook.id,outcome:'adversaries_defeated'}); assert.equal(second.created,false); assert.equal(second.world.encounterHistory.length,1); assert.equal(second.world.aftermaths.length,1);
  const id=first.aftermath.id; const closed=closeWorldAftermath(second.world,id,{resolution:'A doca foi evacuada.'}); assert.equal(closed.aftermaths[0].status,'closed'); assert.equal(closed.encounterHistory.length,1); assert.equal(activeWorldAftermaths(closed).length,0); assert.throws(()=>closeWorldAftermath(closed,id),/já foi encerrada/i);
});

test('migração de mundo anterior recebe coleções de histórico sem inventar registros',()=>{
  const w=normalizeWorld({schemaVersion:5,npcs:{},locations:{},factions:{},clock:{tick:9},events:[],hooks:[],history:[]}); assert.equal(w.schemaVersion,6); assert.deepEqual(w.aftermaths,[]); assert.deepEqual(w.encounterHistory,[]);
});
