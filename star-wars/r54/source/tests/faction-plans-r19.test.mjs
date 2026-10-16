import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createFaction, upsertFaction, createFactionPlan, setFactionPlanStatus, advanceFactionPlan, playerVisibleFaction, scheduleWorldEvent, advanceWorld, causalEncounterCandidates } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('plano de facção usa etapas textuais explícitas e não avança sozinho',()=>{
  let w=createWorld(); const loc=createLocation({name:'Doca 9',knownToPlayer:true}); w=upsertLocation(w,loc); const f=createFaction({name:'Célula Aurora',knownToPlayer:true}); w=upsertFaction(w,f);
  const created=createFactionPlan(w,f.id,{title:'Retirar informante',objective:'Extrair Kara',knownToPlayer:true,steps:[{text:'Chegar à doca',locationId:loc.id,knownToPlayer:true},{text:'Abrir rota de fuga',knownToPlayer:false}]});
  w=created.world; let p=w.factions[f.id].plans[0]; assert.equal(p.status,'planned'); assert.deepEqual(p.steps.map(s=>s.status),['planned','planned']);
  w=setFactionPlanStatus(w,f.id,p.id,'active'); p=w.factions[f.id].plans[0]; assert.equal(p.steps[0].status,'active'); assert.equal(p.steps[1].status,'planned');
  const visible=playerVisibleFaction(w.factions[f.id]); assert.equal(visible.plans.length,1); assert.equal(visible.plans[0].steps.length,1); assert.equal(visible.plans[0].steps[0].text,'Chegar à doca');
});

test('avanço explícito conclui uma etapa por vez e encerra o plano no final',()=>{
  let w=createWorld(); const f=createFaction({name:'Guarnição'}); w=upsertFaction(w,f); let r=createFactionPlan(w,f.id,{title:'Fechar o setor',steps:['Bloquear saídas','Revistar hangares']}); w=r.world; const id=r.plan.id;
  r=advanceFactionPlan(w,f.id,id); w=r.world; assert.equal(r.plan.status,'active'); assert.equal(r.nextStep.text,'Bloquear saídas'); assert.equal(r.completedStep,null);
  r=advanceFactionPlan(w,f.id,id); w=r.world; assert.equal(r.completedStep.text,'Bloquear saídas'); assert.equal(r.nextStep.text,'Revistar hangares');
  r=advanceFactionPlan(w,f.id,id); w=r.world; assert.equal(r.completedStep.text,'Revistar hangares'); assert.equal(r.nextStep,null); assert.equal(r.plan.status,'completed');
  assert.throws(()=>advanceFactionPlan(w,f.id,id),/encerrado/i);
});

test('evento de avanço de plano gera consequência causal deduplicada no local da etapa ativa',()=>{
  let w=createWorld(); const loc=createLocation({name:'Hangar Azul'}); w=upsertLocation(w,loc); const f=createFaction({name:'Império'}); w=upsertFaction(w,f); let c=createFactionPlan(w,f.id,{title:'Tomar o hangar',steps:[{text:'Isolar acessos',locationId:loc.id}]}); w=c.world;
  w=scheduleWorldEvent(w,{type:'advance_faction_plan',dueTick:1,payload:{factionId:f.id,planId:c.plan.id},dedupeKey:'plan:imp:hangar'}).world;
  const out=advanceWorld(w,{ticks:1,maxEvents:10}); assert.equal(out.processed[0].status,'done'); assert.equal(out.hooksCreated.length,1); assert.equal(out.hooksCreated[0].locationId,loc.id); assert.equal(out.hooksCreated[0].planId,c.plan.id);
  assert.equal(causalEncounterCandidates(out.world,loc.id).length,1);
  const again=advanceWorld(out.world,{ticks:0,maxEvents:10}); assert.equal(again.hooksCreated.length,0);
});

test('plano rejeita etapa ligada a local inexistente',()=>{
  let w=createWorld(); const f=createFaction({name:'Aurora'}); w=upsertFaction(w,f);
  assert.throws(()=>createFactionPlan(w,f.id,{title:'Plano inválido',steps:[{text:'Ir',locationId:'loc_missing'}]}),/Local não encontrado/);
});

test('plano ativo não volta silenciosamente a planejado e aborto encerra etapas restantes',()=>{
  let w=createWorld(); const f=createFaction({name:'Aurora'}); w=upsertFaction(w,f); let c=createFactionPlan(w,f.id,{title:'Operação',steps:['A','B']}); w=setFactionPlanStatus(c.world,f.id,c.plan.id,'active');
  assert.throws(()=>setFactionPlanStatus(w,f.id,c.plan.id,'planned'),/não pode voltar/i);
  w=setFactionPlanStatus(w,f.id,c.plan.id,'aborted'); const p=w.factions[f.id].plans[0]; assert.equal(p.status,'aborted'); assert.equal(p.steps.every(s=>s.status==='skipped'),true);
});
