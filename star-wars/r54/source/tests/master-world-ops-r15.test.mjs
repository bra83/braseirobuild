import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

function setup(key='r15-worldops'){
 const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,key));
 const loc=game.addWorldLocation({name:'Hangar 7',knownToPlayer:true});
 const npc=game.addWorldNpc({name:'Tenente Aris',locationId:loc.id,goal:'Investigar o grupo',relation:'desconfiado'});
 game.prepareMasterContext('Observo Aris no hangar.');
 return {storage,game,loc,npc,key};
}

test('proposta do Mestre pode alterar apenas o mundo por operações estruturadas validadas',()=>{
  const {game,npc}=setup();
  const p=game.applyMasterProposal({narration:'Aris recebe uma transmissão e muda de prioridade.',worldOperations:[
    {type:'set_npc_goal',npcId:npc.id,goal:'Proteger o transmissor'},
    {type:'record_npc_fact',npcId:npc.id,text:'Recebeu uma ordem cifrada do ISB.',knownToPlayer:false}
  ]});
  assert.equal(p.worldOperations.length,2);
  assert.equal(game.state.world.npcs[npc.id].goal,'Proteger o transmissor');
  assert.equal(game.state.world.npcs[npc.id].facts.at(-1).knownToPlayer,false);
});

test('operação do Mestre com PNPC/local inexistente falha transacionalmente e preserva o save',()=>{
  const {game,npc}=setup('r15-worldops-rollback');
  const before=JSON.stringify(game.state);
  assert.throws(()=>game.applyMasterProposal({worldOperations:[{type:'move_npc',npcId:npc.id,locationId:'loc_inexistente'}]}),/Local não encontrado/i);
  assert.equal(JSON.stringify(game.state),before);
});

test('Mestre pode criar PNPC/local sem fornecer ID e o motor gera identidades estáveis',()=>{
  const {game}=setup('r15-worldops-create');
  game.applyMasterProposal({worldOperations:[{type:'create_location',name:'Sala de comunicações',description:'Terminal criptografado.',knownToPlayer:false}]});
  const createdLoc=Object.values(game.state.world.locations).find(l=>l.name==='Sala de comunicações');
  assert.ok(createdLoc?.id?.startsWith('loc_'));
  game.prepareMasterContext('Um técnico chega.');
  game.applyMasterProposal({worldOperations:[{type:'create_npc',name:'Técnico Brel',goal:'Reparar o enlace',relation:'neutral'}]});
  const createdNpc=Object.values(game.state.world.npcs).find(n=>n.name==='Técnico Brel');
  assert.ok(createdNpc?.id?.startsWith('npc_'));
});

test('mundo alterado fora de cena reabre com o mesmo PNPC, ID, local, objetivo e fato',()=>{
  const {storage,game,npc,key}=setup('r15-world-reopen');
  const newLoc=game.addWorldLocation({name:'Centro de comando'});
  game.scheduleWorldEvent({type:'move_npc',dueTick:2,payload:{npcId:npc.id,locationId:newLoc.id},dedupeKey:'aris-command'});
  game.scheduleWorldEvent({type:'set_npc_goal',dueTick:2,payload:{npcId:npc.id,goal:'Alertar o comando'},dedupeKey:'aris-goal'});
  game.scheduleWorldEvent({type:'record_npc_fact',dueTick:2,payload:{npcId:npc.id,text:'Viu uma nave rebelde danificada.',knownToPlayer:false},dedupeKey:'aris-fact'});
  game.advanceWorldTime({ticks:2,maxEvents:10});
  const reopened=new GameController(new LocalStorageRepository(storage,key));
  const same=reopened.state.world.npcs[npc.id];
  assert.equal(same.id,npc.id);
  assert.equal(same.locationId,newLoc.id);
  assert.equal(same.goal,'Alertar o comando');
  assert.equal(same.facts.some(f=>f.text==='Viu uma nave rebelde danificada.'),true);
});

test('proposta rejeita campos de mundo arbitrários e lote excessivo',()=>{
  const {game,npc}=setup('r15-worldops-security');
  assert.throws(()=>game.applyMasterProposal({worldOperations:[{type:'set_npc_goal',npcId:npc.id,goal:'x',script:'rm -rf'}]}),/Campo não autorizado/i);
  assert.throws(()=>game.applyMasterProposal({worldOperations:Array.from({length:13},()=>({type:'set_npc_goal',npcId:npc.id,goal:'x'}))}),/no máximo 12/i);
});
