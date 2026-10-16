import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { validateMasterProposal } from '../app/src/main/assets/vtt/js/master/proposal.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

function makeGame(key='r17-master-encounter'){
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),key));
  game.loadStarterCharacter('cael');
  return game;
}

test('diretriz do Mestre aceita apenas perfis, quantidades, alcances e iniciativa válidos',()=>{
  const p=validateMasterProposal({narration:'A patrulha fecha a saída.',encounterDirective:{adversaries:[{profileKey:'stormtrooper',count:3,range:'Médio'}],playerInitiativeSkill:'alerta',npcInitiativeSkill:'frialdad'}});
  assert.equal(p.encounterDirective.adversaries[0].profileKey,'stormtrooper');
  assert.throws(()=>validateMasterProposal({encounterDirective:{adversaries:[{profileKey:'inventado'}]}}),/Perfil de adversário não permitido/i);
  assert.throws(()=>validateMasterProposal({encounterDirective:{adversaries:[{profileKey:'stormtrooper',count:21}]}}),/Quantidade de adversários inválida/i);
});

test('Mestre prepara encontro e jogador ainda escolhe Reação rápida antes da iniciativa',()=>{
  const game=makeGame('r17-pending');
  game.prepareMasterContext('Sigo pelo corredor.');
  game.applyMasterProposal({narration:'Soldados aparecem no corredor.',encounterDirective:{adversaries:[{profileKey:'stormtrooper',count:2,range:'Médio'}]}});
  assert.equal(game.state.combat.active,false);
  assert.equal(game.state.master.pendingEncounter.adversaries[0].count,2);
  const started=game.beginPendingMasterEncounter({playerQuickReactionStrain:0,rng:()=>0});
  assert.equal(game.state.master.pendingEncounter,null);
  assert.equal(started.combat.active,true);
  assert.equal(started.combat.adversaries[0].profileKey,'stormtrooper');
});

test('consequência causal pode originar encontro e só é resolvida quando o combate tem desfecho',()=>{
  const game=makeGame('r17-causal-combat');
  const loc=game.addWorldLocation({name:'Armazém'}); const npc=game.addWorldNpc({name:'Patrulha',locationId:loc.id});
  game.scheduleWorldEvent({type:'set_npc_status',dueTick:1,payload:{npcId:npc.id,status:'hostil'}}); game.advanceWorldTime({ticks:1});
  game.prepareMasterContext('Entro no armazém.'); game.applyMasterProposal({sceneLocationId:loc.id,narration:'A patrulha aponta as armas.'});
  const hook=game.causalEncounterCandidates()[0];
  game.prepareMasterContext('Tento atravessar.');
  game.applyMasterProposal({narration:'A patrulha abre fogo.',encounterDirective:{sourceHookId:hook.hookId,adversaries:[{profileKey:'stormtrooper',count:1,range:'Médio'}]}});
  game.beginPendingMasterEncounter({rng:()=>0});
  assert.equal(game.state.combat.causalHookId,hook.hookId);
  assert.equal(game.state.world.hooks.find(h=>h.id===hook.hookId).status,'pending');
  game.state.combat.encounter.outcome='adversaries_defeated';
  const result=game.finishCombat();
  assert.equal(result.resolvedWorldHook.status,'resolved');
  assert.equal(game.state.world.hooks.find(h=>h.id===hook.hookId).status,'resolved');
});

test('diretriz causal inválida reverte a proposta inteira',()=>{
  const game=makeGame('r17-rollback');
  const before=JSON.stringify(game.state);
  game.prepareMasterContext('Continuo.');
  assert.throws(()=>game.applyMasterProposal({narration:'Isto não deve persistir.',encounterDirective:{sourceHookId:'whook_inexistente',adversaries:[{profileKey:'stormtrooper'}]}}),/não está mais pendente/i);
  assert.equal(JSON.stringify(game.state),before);
});
