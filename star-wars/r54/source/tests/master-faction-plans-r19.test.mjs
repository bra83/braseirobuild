import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function make(key){ return new GameController(new LocalStorageRepository(new MemoryStorage(),key)); }

test('Mestre cria e avança plano de facção por operações estruturadas',()=>{
  const g=make('r19-plan-master'); const loc=g.addWorldLocation({name:'Doca'}); const f=g.addWorldFaction({name:'Aurora'}); g.prepareMasterContext('Organize a extração.');
  g.applyMasterProposal({worldOperations:[{type:'create_faction_plan',factionId:f.id,title:'Extrair agente',objective:'Retirar Kara',steps:[{text:'Chegar à doca',locationId:loc.id,knownToPlayer:true},{text:'Partir',knownToPlayer:false}],knownToPlayer:true}]});
  const plan=g.state.world.factions[f.id].plans[0]; assert.equal(plan.status,'planned');
  g.prepareMasterContext('Inicie o plano.'); g.applyMasterProposal({worldOperations:[{type:'advance_faction_plan',factionId:f.id,planId:plan.id}]});
  assert.equal(g.state.world.factions[f.id].plans[0].status,'active'); assert.equal(g.state.world.factions[f.id].plans[0].steps[0].status,'active');
});

test('operação inválida em plano reverte a proposta inteira',()=>{
  const g=make('r19-plan-rollback'); const f=g.addWorldFaction({name:'Aurora'}); const before=JSON.stringify(g.state); g.prepareMasterContext('Crie plano.');
  assert.throws(()=>g.applyMasterProposal({narration:'Não deve persistir',worldOperations:[{type:'create_faction_plan',factionId:f.id,title:'Erro',steps:[{text:'Ir',locationId:'loc_missing'}]}]}),/Local não encontrado/);
  assert.equal(JSON.stringify(g.state),before);
});

test('contexto do Mestre inclui planos completos e versão 6',()=>{
  const g=make('r19-plan-context'); const f=g.addWorldFaction({name:'Aurora'}); g.addWorldFactionPlan(f.id,{title:'Plano secreto',steps:['Observar','Agir']}); const c=buildMasterContext(g.state,{intentText:'O que a facção planeja?'});
  assert.equal(c.contextVersion,8); assert.equal(c.world.factions[0].plans[0].title,'Plano secreto'); assert.equal(c.world.factions[0].plans[0].steps.length,2);
});
