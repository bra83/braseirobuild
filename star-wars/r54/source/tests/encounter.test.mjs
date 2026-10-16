import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { currentSlot } from '../app/src/main/assets/vtt/js/rules/encounter.mjs';

class MemoryRepository {
  constructor(state=createCampaign('Teste')){ this.state=structuredClone(state); }
  load(){ return structuredClone(this.state); }
  save(s){ this.state=structuredClone(s); }
}

test('encontro converte iniciativa em posições e para na posição do jogador',()=>{
  const game=new GameController(new MemoryRepository());
  game.loadStarterCharacter('cael');
  const result=game.beginEncounter({
    adversaries:[createAdversary('imperial_soldier',{count:3}),createAdversary('imperial_officer')],
    rng:()=>0
  });
  assert.equal(result.combat.encounter.slots.length,3);
  assert.equal(currentSlot(result.combat).side,'player');
  assert.equal(result.combat.encounter.waitingForPlayer,true);
});

test('ao encerrar posição PJ, motor executa posições PNPC e volta ao PJ no novo assalto',()=>{
  const game=new GameController(new MemoryRepository());
  game.loadStarterCharacter('cael');
  game.beginEncounter({
    adversaries:[createAdversary('imperial_soldier',{count:3}),createAdversary('imperial_officer')],
    rng:()=>0
  });
  const result=game.finishPlayerInitiativeTurn({rng:()=>0});
  assert.equal(result.npcTurns.length,2);
  assert.equal(result.combat.round,2);
  assert.equal(currentSlot(result.combat).side,'player');
  assert.equal(result.combat.encounter.waitingForPlayer,true);
});

test('ataque do jogador pode atingir adversário do encontro e Adversário 1 melhora a dificuldade',()=>{
  const game=new GameController(new MemoryRepository());
  game.loadStarterCharacter('cael');
  const started=game.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  const attack=game.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.15});
  assert.equal(attack.challenge,1);
  assert.equal(attack.difficulty,1);
  assert.equal(attack.targetAdversaryId,target.id);
});
