import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

function game(key){ const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key)); g.loadStarterCharacter('cael'); return g; }

test('PNPC persistente usa o mesmo ID no combate e conserva dano entre reencontros',()=>{
  const g=game('r17-persist-combat');
  const loc=g.addWorldLocation({name:'Corredor'}); const npc=g.addWorldNpc({name:'Tenente Varo',locationId:loc.id});
  g.prepareMasterContext('Vejo Varo.');
  g.applyMasterProposal({sceneLocationId:loc.id,narration:'Varo bloqueia a passagem.',encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:npc.id,range:'Médio'}]}});
  g.beginPendingMasterEncounter({rng:()=>0});
  assert.equal(g.state.combat.adversaries[0].id,npc.id);
  g.state.combat.adversaries[0].wounds.current=5;
  g.finishCombat();
  assert.equal(g.state.world.npcs[npc.id].combatState.wounds.current,5);
  assert.equal(g.state.world.npcs[npc.id].combatState.profileKey,'imperial_officer');

  g.prepareMasterContext('Varo retorna.');
  g.applyMasterProposal({narration:'Varo retorna ferido.',encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:npc.id,range:'Médio'}]}});
  g.beginPendingMasterEncounter({rng:()=>0});
  assert.equal(g.state.combat.adversaries[0].id,npc.id);
  assert.equal(g.state.combat.adversaries[0].wounds.current,5);
});

test('PNPC persistente não pode trocar silenciosamente de perfil nem reaparecer incapacitado',()=>{
  const g=game('r17-profile-lock'); const loc=g.addWorldLocation({name:'Doca'}); const npc=g.addWorldNpc({name:'Agente',locationId:loc.id});
  g.prepareMasterContext('Agente aparece.'); g.applyMasterProposal({sceneLocationId:loc.id,narration:'Agente ameaça.',encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:npc.id}]}}); g.beginPendingMasterEncounter({rng:()=>0});
  g.state.combat.adversaries[0].status='incapacitated'; g.state.combat.adversaries[0].wounds.current=99; g.state.combat.encounter.outcome='adversaries_defeated'; g.finishCombat();
  const before=JSON.stringify(g.state); g.prepareMasterContext('Ele volta?');
  assert.throws(()=>g.applyMasterProposal({narration:'Não deveria voltar.',encounterDirective:{adversaries:[{profileKey:'stormtrooper',worldNpcId:npc.id}]}}),/perfil de combate|incapacitado/i);
  assert.equal(JSON.stringify(g.state),before);
});
