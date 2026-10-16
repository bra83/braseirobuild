import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { setForceRating, purchaseForcePower, purchaseForceUpgrade, normalizeForceState } from '../app/src/main/assets/vtt/js/rules/force.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

class MemoryRepository{constructor(state=createCampaign('P105')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function baseHero(){
  let c=createBlankCharacter('P105');
  c.characteristics={fortaleza:2,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:2};
  c.skills.armas_distancia_ligeras=2;c.skills.disciplina=2;
  c.derived={soak:2,wounds:{threshold:12,current:0},strain:{threshold:14,current:0},defense:{ranged:0,melee:0}};
  c.weapons=[{id:'w1',name:'Pistola P105',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Médio',qualities:[],active:true,equipped:true}];
  c.activeWeaponId='w1';c.xp={available:1000,total:1000,spent:0};
  c=setForceRating(c,2);return c;
}
function withSense(mode='defense'){
  let c=purchaseForcePower(baseHero(),'sense').character;
  c=purchaseForceUpgrade(c,'sense','control_defense').character;
  if(mode==='offense'){
    c=purchaseForceUpgrade(c,'sense','duration').character;
    c=purchaseForceUpgrade(c,'sense','strength').character;
    c=purchaseForceUpgrade(c,'sense','control_offense').character;
  }
  return c;
}
function withInfluence(){return purchaseForcePower(baseHero(),'influence').character;}

test('P105: ativar Sentir contínuo consome a ação e compromete 1 dado da Força',()=>{
  const g=new GameController(new MemoryRepository());g.setCharacter(withSense('defense'));g.beginCombat();
  const r=g.activateSenseOngoing({mode:'defense'});
  assert.equal(r.combat.playerTurn.actionUsed,true);
  assert.equal(normalizeForceState(g.state.player.character).committed,1);
  assert.ok(normalizeForceState(g.state.player.character).ongoing.some(x=>x.id==='sense-defense'));
  assert.throws(()=>g.activateSenseOngoing({mode:'defense'}),/ação deste turno já foi usada|já está ativo/i);
});

test('P105: Sentir defensivo aprimora a dificuldade do primeiro ataque NPC do assalto',()=>{
  const g=new GameController(new MemoryRepository());g.setCharacter(withSense('defense'));g.beginCombat();g.activateSenseOngoing({mode:'defense'});g.advancePlayerTurn();
  const adv=createAdversary('son_barab',{range:'Médio'});g.state.combat.adversaries=[adv];
  const r=g._resolveNpcTurn(g.state.combat,g.state.player.character,adv.id,()=>0.42);
  assert.equal(r.attack.senseDefense.applied,true);assert.equal(r.attack.senseDefense.upgrades,1);assert.equal(r.attack.challenge,1);
  assert.equal(normalizeForceState(r.character).ongoing.find(x=>x.id==='sense-defense').triggersUsed,1);
});

test('P105: Sentir ofensivo aprimora a habilidade do próximo ataque do jogador',()=>{
  const g=new GameController(new MemoryRepository());g.setCharacter(withSense('offense'));g.beginCombat();g.activateSenseOngoing({mode:'offense'});g.advancePlayerTurn();
  const r=g.resolveWeaponAttack({weaponIndex:0,targetRange:'Médio',rng:()=>0.42});
  assert.equal(r.hit!==undefined,true);assert.equal(g.state.lastCheck.senseOffense.applied,true);assert.equal(g.state.lastCheck.senseOffense.upgrades,2);
});

test('P105: Influência básica exige poder comprado, consome ação e aplica Tensão real a alvo engajado',()=>{
  const g=new GameController(new MemoryRepository());g.setCharacter(withInfluence());g.beginCombat();
  const adv=createAdversary('son_barab',{range:'Interação'});g.state.combat.adversaries=[adv];
  const before=adv.strain.current;
  const r=g.activateInfluenceBasic({targetAdversaryIds:[adv.id],rng:()=>0.99});
  assert.equal(r.success,true);assert.equal(r.forcePointCost,1);assert.equal(r.combat.playerTurn.actionUsed,true);assert.equal(r.affected[0].convertedToWounds,false);
  assert.equal(g.state.combat.adversaries[0].strain.current,before+1);
});

test('P105: Influência falha sem pontos suficientes mas ainda consome a ação',()=>{
  const g=new GameController(new MemoryRepository());g.setCharacter(withInfluence());g.beginCombat();
  const adv=createAdversary('son_barab',{range:'Interação'});g.state.combat.adversaries=[adv];
  const before=adv.strain.current;
  const r=g.activateInfluenceBasic({targetAdversaryIds:[adv.id],activations:2,rng:()=>0});
  assert.equal(r.success,false);assert.equal(r.points,0);assert.equal(g.state.combat.playerTurn.actionUsed,true);assert.equal(g.state.combat.adversaries[0].strain.current,before);
});

test('P105: bridge expõe ativação executável de Sentir e Influência sem RAG',()=>{
  const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.match(bridge,/activateSenseOngoing\(options = \{\}\)/);assert.match(bridge,/endSenseOngoing\(options = \{\}\)/);assert.match(bridge,/activateInfluenceBasic\(options = \{\}\)/);
});

test('P105: R46 expõe somente Sentir adquirido e Influência contra alvo persistente',()=>{
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(runtime,/R54 P105 — EOTE CORE FORCE ACTIONS/);
  assert.match(runtime,/call\('activateSenseOngoing'/);
  assert.match(runtime,/call\('activateInfluenceBasic'/);
  assert.match(runtime,/Resultados sombrios não são usados automaticamente/);
});
