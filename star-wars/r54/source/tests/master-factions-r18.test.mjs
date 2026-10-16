import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='r18-factions'){ const storage=new MemoryStorage(); return {storage,game:new GameController(new LocalStorageRepository(storage,key)),key}; }

test('Mestre cria duas facções e um PNPC afiliado usando somente operações estruturadas',()=>{
  const {game:g}=game(); g.prepareMasterContext('Organize forças locais.');
  g.applyMasterProposal({worldOperations:[
    {type:'create_faction',name:'Célula Aurora',objective:'Retirar agentes',status:'oculta',resources:['Nave de transporte'],knownToPlayer:true},
    {type:'create_faction',name:'Guarnição Imperial',objective:'Descobrir a célula',status:'alerta',resources:['Patrulha'],knownToPlayer:true}
  ]});
  const aurora=Object.values(g.state.world.factions).find(f=>f.name==='Célula Aurora'); const imp=Object.values(g.state.world.factions).find(f=>f.name==='Guarnição Imperial');
  assert.ok(aurora&&imp); g.prepareMasterContext('Defina hostilidade.'); g.applyMasterProposal({worldOperations:[{type:'set_faction_relation',factionId:aurora.id,targetFactionId:imp.id,relation:'hostil'}]});
  g.prepareMasterContext('Adicione um agente.'); g.applyMasterProposal({worldOperations:[{type:'create_npc',name:'Kara',factionId:aurora.id,goal:'Buscar códigos'}]});
  assert.equal(Object.values(g.state.world.npcs).find(n=>n.name==='Kara').factionId,aurora.id); assert.equal(g.state.world.factions[aurora.id].relations[imp.id],'hostil');
});

test('operação de facção inválida faz rollback transacional da proposta inteira',()=>{
  const {game:g}=game('r18-rollback'); const f=g.addWorldFaction({name:'Aurora'}); g.prepareMasterContext('Atualize a facção.'); const before=JSON.stringify(g.state);
  assert.throws(()=>g.applyMasterProposal({worldOperations:[{type:'add_faction_resource',factionId:f.id,resource:'Códigos'},{type:'set_faction_relation',factionId:f.id,targetFactionId:'fac_missing',relation:'hostil'}]}),/Facção não encontrada/);
  assert.equal(JSON.stringify(g.state),before);
});

test('contexto do Mestre inclui facções, afiliação do PNPC e contagem digestível',()=>{
  const {game:g}=game('r18-context'); const f=g.addWorldFaction({name:'Aurora',objective:'Proteger o hangar'}); const n=g.addWorldNpc({name:'Kara',factionId:f.id});
  const c=buildMasterContext(g.state,{intentText:'Quem controla o hangar?'}); assert.equal(c.contextVersion,8); assert.equal(c.world.factions[0].id,f.id); assert.equal(c.world.npcs.find(x=>x.id===n.id).factionId,f.id); assert.equal(c.world.factions[0].objective,'Proteger o hangar');
});

test('save/reopen preserva facções, relações, recursos, fatos e afiliação de PNPC',()=>{
  const {storage,game:g,key}=game('r18-reopen'); const a=g.addWorldFaction({name:'Aurora',resources:['Códigos']}); const b=g.addWorldFaction({name:'Império'}); g.setWorldFactionRelation(a.id,b.id,'hostil'); g.addWorldFactionFact(a.id,{text:'Base secundária em segredo',knownToPlayer:false}); const n=g.addWorldNpc({name:'Kara',factionId:a.id});
  const reopened=new GameController(new LocalStorageRepository(storage,key)); assert.equal(reopened.state.world.factions[a.id].relations[b.id],'hostil'); assert.deepEqual(reopened.state.world.factions[a.id].resources,['Códigos']); assert.equal(reopened.state.world.factions[a.id].facts[0].knownToPlayer,false); assert.equal(reopened.state.world.npcs[n.id].factionId,a.id);
});
