import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('33 sessões simuladas reencontram o mesmo PNPC com o mesmo estado de combate',()=>{
  const storage=new MemoryStorage(); const key='r17-33-recurring-adversary';
  let game=new GameController(new LocalStorageRepository(storage,key)); game.loadStarterCharacter('cael');
  const loc=game.addWorldLocation({name:'Setor Kappa',knownToPlayer:true}); const npc=game.addWorldNpc({name:'Tenente Varo',locationId:loc.id,relation:'rival'}); const id=npc.id;
  game.prepareMasterContext('Chego ao setor.'); game.applyMasterProposal({sceneLocationId:loc.id,narration:'Setor Kappa.'});
  for(let i=1;i<=33;i++){
    game.startSession(); game.prepareMasterContext(`Reencontro ${i}`);
    game.applyMasterProposal({narration:`Varo aparece pela ${i}ª vez.`,encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:id,range:'Médio'}]}});
    game.beginPendingMasterEncounter({rng:()=>0});
    assert.equal(game.state.combat.adversaries[0].id,id);
    if(i===1) game.state.combat.adversaries[0].wounds.current=3;
    else assert.equal(game.state.combat.adversaries[0].wounds.current,3);
    game.finishCombat(); game.closeSession({summary:`Reencontro simulado ${i}.`});
    if(i%3===0) game=new GameController(new LocalStorageRepository(storage,key));
    assert.equal(game.state.world.npcs[id].id,id);
    assert.equal(game.state.world.npcs[id].combatState.wounds.current,3);
  }
  game=new GameController(new LocalStorageRepository(storage,key));
  assert.equal(game.state.sessionHistory.length,33);
  assert.equal(game.state.world.npcs[id].combatState.profileKey,'imperial_officer');
  assert.equal(game.state.world.npcs[id].combatState.wounds.current,3);
});
