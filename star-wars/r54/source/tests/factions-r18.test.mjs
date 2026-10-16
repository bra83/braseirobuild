import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createWorld, createLocation, upsertLocation, createFaction, upsertFaction, setFactionObjective, setFactionStatus,
  addFactionResource, removeFactionResource, setFactionRelation, recordFactionFact, playerVisibleFaction,
  createNpc, upsertNpc, setNpcFaction, scheduleWorldEvent, advanceWorld, causalEncounterCandidates, masterWorldSummary, normalizeWorld
} from '../app/src/main/assets/vtt/js/world/world.mjs';

test('facções têm ID estável, objetivo, recursos textuais e fatos com visibilidade separada',()=>{
  let w=createWorld(); const f=createFaction({name:'Célula Aurora',objective:'Evacuar informantes',resources:['YT-1300','Códigos de acesso'],knownToPlayer:true});
  w=upsertFaction(w,f); w=recordFactionFact(w,f.id,{text:'Tem um contato em Ryloth.',knownToPlayer:true}); w=recordFactionFact(w,f.id,{text:'O líder é um agente duplo.',knownToPlayer:false});
  const pub=playerVisibleFaction(w.factions[f.id]);
  assert.equal(pub.id,f.id); assert.deepEqual(pub.resources,['YT-1300','Códigos de acesso']); assert.equal(pub.facts.length,1); assert.equal(pub.facts[0].text,'Tem um contato em Ryloth.');
});

test('relações entre facções exigem IDs existentes e não aceitam autorrelação',()=>{
  let w=createWorld(); const a=createFaction({name:'Aurora'}), b=createFaction({name:'Guarnição Imperial'}); w=upsertFaction(upsertFaction(w,a),b);
  w=setFactionRelation(w,a.id,b.id,'hostil'); assert.equal(w.factions[a.id].relations[b.id],'hostil');
  assert.throws(()=>setFactionRelation(w,a.id,'fac_inexistente','hostil'),/Facção não encontrada/);
  assert.throws(()=>setFactionRelation(w,a.id,a.id,'aliada'),/consigo mesma/);
});

test('PNPC mantém afiliação estável e afiliação inválida é rejeitada',()=>{
  let w=createWorld(); const f=createFaction({name:'Inteligência Rebelde'}); w=upsertFaction(w,f); const npc=createNpc({name:'Kara'}); w=upsertNpc(w,npc); w=setNpcFaction(w,npc.id,f.id);
  assert.equal(w.npcs[npc.id].factionId,f.id); assert.throws(()=>setNpcFaction(w,npc.id,'fac_x'),/Facção não encontrada/);
});

test('evento de facção altera o mundo e gera consequência causal no local informado',()=>{
  let w=createWorld(); const loc=createLocation({name:'Porto de Kestic',knownToPlayer:true}); w=upsertLocation(w,loc); const f=createFaction({name:'Guarnição Imperial'}); w=upsertFaction(w,f);
  w=scheduleWorldEvent(w,{type:'set_faction_objective',dueTick:2,payload:{factionId:f.id,objective:'Bloquear o porto',locationId:loc.id},dedupeKey:'imp-block-port'}).world;
  const r=advanceWorld(w,{ticks:2,maxEvents:10}); assert.equal(r.processed[0].status,'done'); assert.equal(r.world.factions[f.id].objective,'Bloquear o porto'); assert.equal(r.hooksCreated.length,1);
  const candidates=causalEncounterCandidates(r.world,loc.id); assert.equal(candidates.length,1); assert.equal(candidates[0].faction.id,f.id); assert.match(candidates[0].summary,/Guarnição Imperial mudou de objetivo/);
});

test('evento de facção com local inexistente falha isoladamente sem alterar a facção',()=>{
  let w=createWorld(); const f=createFaction({name:'Aurora',objective:'Ocultar-se'}); w=upsertFaction(w,f);
  w=scheduleWorldEvent(w,{type:'set_faction_status',dueTick:1,payload:{factionId:f.id,status:'mobilizada',locationId:'loc_inexistente'}}).world;
  const r=advanceWorld(w,{ticks:1,maxEvents:10}); assert.equal(r.processed[0].status,'failed'); assert.equal(r.world.factions[f.id].status,'active'); assert.equal(r.world.hooks.length,0);
});

test('recursos de facção não duplicam e remoção inexistente falha claramente',()=>{
  let w=createWorld(); const f=createFaction({name:'Aurora'}); w=upsertFaction(w,f); w=addFactionResource(w,f.id,'Transponder falso'); w=addFactionResource(w,f.id,'Transponder falso');
  assert.deepEqual(w.factions[f.id].resources,['Transponder falso']); w=removeFactionResource(w,f.id,'Transponder falso'); assert.deepEqual(w.factions[f.id].resources,[]);
  assert.throws(()=>removeFactionResource(w,f.id,'Transponder falso'),/não encontrado/);
});

test('resumo do Mestre contém facções completas, mas normalização remove relações e afiliações órfãs',()=>{
  const legacy={schemaVersion:3,npcs:{n:{id:'n',name:'N',factionId:'fac_missing'}},locations:{},factions:{a:{id:'a',name:'A',relations:{fac_missing:'hostil'}}},clock:{tick:0},events:[],hooks:[],history:[]};
  const w=normalizeWorld(legacy); assert.equal(w.schemaVersion,6); assert.equal(w.npcs.n.factionId,null); assert.deepEqual(w.factions.a.relations,{});
  const sum=masterWorldSummary(w); assert.equal(sum.factions.length,1); assert.equal(sum.factions[0].name,'A');
});

test('mudanças de facção persistem por sequência de eventos sem criar números de economia ou relógios próprios',()=>{
  let w=createWorld(); const f=createFaction({name:'Aurora',objective:'Observar'}); w=upsertFaction(w,f);
  w=scheduleWorldEvent(w,{type:'add_faction_resource',dueTick:1,payload:{factionId:f.id,resource:'Códigos roubados'},dedupeKey:'fac-res'}).world;
  w=scheduleWorldEvent(w,{type:'set_faction_status',dueTick:1,payload:{factionId:f.id,status:'em movimento'},dedupeKey:'fac-status'}).world;
  const r=advanceWorld(w,{ticks:1,maxEvents:10}); const f2=r.world.factions[f.id]; assert.equal(f2.status,'em movimento'); assert.deepEqual(f2.resources,['Códigos roubados']); assert.equal('power' in f2,false); assert.equal('wealth' in f2,false); assert.equal('clock' in f2,false);
});
