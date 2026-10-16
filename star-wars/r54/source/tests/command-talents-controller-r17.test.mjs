import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

class MemoryRepository { constructor(){this.state=createCampaign('R17')} load(){return structuredClone(this.state)} save(s){this.state=structuredClone(s)} }

function gameWithTalent(talent){ const g=new GameController(new MemoryRepository()); g.loadStarterCharacter('cael'); g.state.player.character.talents.push(talent); g.repository.save(g.state); return g; }

test('Ataque rápido entra automaticamente no pool contra alvo que ainda não agiu',()=>{
  const game=gameWithTalent('Ataque rápido');
  const started=game.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  const attack=game.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.25});
  assert.equal(attack.quickStrikeBoost,1);
  assert.equal(attack.check.quickStrike.applies,true);
});

test('Ataque rápido não concede Impulso depois que o alvo já agiu',()=>{
  const game=gameWithTalent('Ataque rápido');
  const started=game.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  game.state.combat.encounter.actedActorIds=[target.id]; game.repository.save(game.state);
  const attack=game.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.25});
  assert.equal(attack.quickStrikeBoost,0);
});

test('Mando acrescenta Impulso automaticamente a teste de Liderança',()=>{
  const game=new GameController(new MemoryRepository()); game.loadStarterCharacter('arkhan');
  game.state.player.character.talents.push('Comando','Comando'); game.repository.save(game.state);
  const check=game.resolveSkillCheck({skillKey:'liderazgo',difficulty:2,rng:()=>0.25});
  assert.equal(check.talentModifiers.talentBoost,2);
  assert.equal(check.pool.boost>=2,true);
});
