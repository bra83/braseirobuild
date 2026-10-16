import test from 'node:test'; import assert from 'node:assert/strict'; import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs'; import {LocalStorageRepository} from '../app/src/main/assets/vtt/js/core/storage.mjs';
class M{constructor(){this.m=new Map()}getItem(k){return this.m.get(k)??null}setItem(k,v){this.m.set(k,String(v))}removeItem(k){this.m.delete(k)}}

test('33 sessões simuladas preservam histórico causal/aftermaths após reaberturas repetidas',()=>{ const storage=new M(), key='r20-33-after'; let g=new GameController(new LocalStorageRepository(storage,key)); g.loadStarterCharacter('cael'); const loc=g.addWorldLocation({name:'Setor Aurek',knownToPlayer:true}); const npc=g.addWorldNpc({name:'Rede de patrulhas',locationId:loc.id});
  for(let i=1;i<=33;i++){
    g.startSession(); g.scheduleWorldEvent({type:'set_npc_status',dueTick:g.state.world.clock.tick+1,payload:{npcId:npc.id,status:`pressão-${i}`},dedupeKey:`pressao:${i}`}); g.advanceWorldTime({ticks:1}); g.prepareMasterContext('Retorno ao setor.'); g.applyMasterProposal({sceneLocationId:loc.id}); const hook=g.causalEncounterCandidates()[0]; assert.ok(hook);
    g.prepareMasterContext('A patrulha reage.'); g.applyMasterProposal({encounterDirective:{sourceHookId:hook.hookId,adversaries:[{profileKey:'stormtrooper',count:1}]}}); g.beginPendingMasterEncounter({rng:()=>0}); g.state.combat.encounter.outcome='adversaries_defeated'; const result=g.finishCombat(); assert.ok(result.aftermath);
    if(i%3===0) g.closeWorldAftermath(result.aftermath.id,{resolution:`Repercussão ${i} estabilizada.`}); g.closeSession({summary:`Sessão causal simulada ${i}.`}); g=new GameController(new LocalStorageRepository(storage,key));
  }
  assert.equal(g.state.sessionHistory.length,33); assert.equal(g.state.world.encounterHistory.length,33); assert.equal(g.state.world.aftermaths.length,33); assert.equal(g.state.world.aftermaths.filter(a=>a.status==='closed').length,11); assert.equal(g.state.world.aftermaths.filter(a=>a.status==='active').length,22); assert.equal(new Set(g.state.world.encounterHistory.map(e=>e.hookId)).size,33); assert.equal(g.state.world.hooks.filter(h=>h.status==='pending').length,0);
});
