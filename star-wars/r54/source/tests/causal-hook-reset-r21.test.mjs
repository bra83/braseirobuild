import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage {
  constructor(){ this.m = new Map(); }
  getItem(k){ return this.m.has(k) ? this.m.get(k) : null; }
  setItem(k,v){ this.m.set(k,String(v)); }
  removeItem(k){ this.m.delete(k); }
}

function makeGame(key='r21-causal-hook-reset'){
  const game = new GameController(new LocalStorageRepository(new MemoryStorage(), key));
  game.loadStarterCharacter('cael');
  return game;
}

test('metadado causal não vaza para um combate posterior não causal',()=>{
  const game = makeGame();
  const loc = game.addWorldLocation({name:'Armazém'});
  const npc = game.addWorldNpc({name:'Patrulha',locationId:loc.id});
  game.scheduleWorldEvent({type:'set_npc_status',dueTick:1,payload:{npcId:npc.id,status:'hostil'}});
  game.advanceWorldTime({ticks:1});
  game.prepareMasterContext('Entro no armazém.');
  game.applyMasterProposal({sceneLocationId:loc.id,narration:'A patrulha aponta as armas.'});
  const hook = game.causalEncounterCandidates()[0];
  game.prepareMasterContext('Tento atravessar.');
  game.applyMasterProposal({narration:'A patrulha abre fogo.',encounterDirective:{sourceHookId:hook.hookId,adversaries:[{profileKey:'stormtrooper',count:1,range:'Médio'}]}});
  game.beginPendingMasterEncounter({rng:()=>0});
  assert.equal(game.state.combat.causalHookId,hook.hookId);
  game.state.combat.encounter.outcome='adversaries_defeated';
  game.finishCombat();

  assert.equal(game.state.combat.causalHookId,undefined);
  assert.equal(game.state.combat.causalHookSummary,undefined);

  game.beginPresetEncounter({rng:()=>0});
  assert.equal(game.state.combat.causalHookId,undefined);
  assert.equal(game.state.combat.causalHookSummary,undefined);
  game.state.combat.encounter.outcome='adversaries_defeated';
  const result = game.finishCombat();
  assert.equal(result.resolvedWorldHook,null);
  assert.equal(result.encounterRecord.hookId,null);
  assert.equal(game.state.world.encounterHistory.length,2);
});
