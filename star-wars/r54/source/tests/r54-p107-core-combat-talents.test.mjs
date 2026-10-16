import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

class MemoryRepository{constructor(state=createCampaign('P107')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(talents=[]){
  const c=createBlankCharacter('P107');
  c.characteristics={fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:2};
  c.skills.pelea=2;c.skills.armas_cuerpo_a_cuerpo=2;c.skills.armas_distancia_ligeras=2;c.skills.armas_distancia_pesadas=2;c.skills.frialdad=2;
  c.derived={soak:2,wounds:{threshold:30,current:0},strain:{threshold:20,current:5},defense:{ranged:0,melee:0}};
  c.talents=[...talents];
  c.weapons=[
    {id:'p107_brawl',name:'Punhos',skill:'pelea',damage:3,crit:5,range:'Interação',active:true,equipped:true},
    {id:'p107_rifle',name:'Rifle blaster',skill:'armas_distancia_pesadas',damage:9,crit:3,range:'Extremo',active:false,equipped:false},
    {id:'p107_pistol',name:'Pistola blaster',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Médio',active:false,equipped:false}
  ];
  c.activeWeaponId='p107_brawl';
  return c;
}
function game(talents=[]){const g=new GameController(new MemoryRepository());g.setCharacter(hero(talents));return g;}

test('P107: Ataque Frenético sofre Tensão, fica pendente e aprimora somente Briga/Corpo a Corpo',()=>{
  const g=game(['Ataque frenético']);
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Interação'})],rng:()=>0});
  const id=started.combat.adversaries[0].id,before=g.state.player.character.derived.strain.current;
  const prep=g.activateFrenziedAttack({strain:1});
  assert.equal(prep.abilityUpgrades,1);assert.equal(g.state.player.character.derived.strain.current,before+1);
  const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:id,rng:()=>0.1});
  assert.equal(attack.check.frenziedAttack.abilityUpgrades,1);assert.equal(attack.check.pool.proficiency,3);assert.equal(g.state.combat.playerTurn.pendingFrenziedAttack,undefined);
});

test('P107: Força Feroz e Rajada adicionam dano real apenas nos ataques auditados',()=>{
  let g=game(['Força feroz']);
  let started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Interação'})],rng:()=>0});
  let attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:started.combat.adversaries[0].id,rng:()=>0.1});
  assert.equal(attack.hit,true);assert.equal(attack.talentDamageBonus,1);assert.equal(attack.talentDamageDetails[0].talent,'Força feroz');

  g=game(['Rajada']);
  started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Longo'})],rng:()=>0});
  attack=g.resolveWeaponAttack({weaponIndex:1,targetAdversaryId:started.combat.adversaries[0].id,rng:()=>0.1});
  assert.equal(attack.hit,true);assert.equal(attack.talentDamageBonus,1);assert.equal(attack.talentDamageDetails[0].talent,'Rajada');
});

test('P107: Golpes Letais soma +10 por rank ao crítico realmente ativado',()=>{
  const g=game(['Golpes letais','Golpes letais']);
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Curto'})],rng:()=>0});
  const target=started.combat.adversaries[0];
  g.state.combat.pendingPlayerCritical={adversaryId:target.id,weapon:'Teste',cost:0,byTriumph:false,vicious:0};
  g.state.combat.pendingPlayerSpend={adversaryId:target.id,remainingAdvantage:0,remainingTriumph:0};
  g.repository.save(g.state);
  const r=g.activatePendingPlayerCritical({rng:()=>0});
  assert.equal(r.lethalBlows.rank,2);assert.equal(r.criticalModifier,20);assert.equal(r.injury.externalModifier,20);assert.equal(r.injury.total,21);
});

test('P107: Segundo Fôlego recupera Tensão pelo rank e só funciona 1x por encontro',()=>{
  const g=game(['Segundo fôlego','Segundo fôlego']);g.beginCombat();
  const before=g.state.player.character.derived.strain.current,r=g.useSecondWind();
  assert.equal(r.recovered,2);assert.equal(g.state.player.character.derived.strain.current,before-2);assert.equal(g.talentActionStatus().secondWind.usedThisEncounter,true);
  assert.throws(()=>g.useSecondWind(),/já foi usado neste encontro/i);
});

test('P107: Saque Rápido troca arma como incidental 1x por turno sem gastar manobra',()=>{
  const g=game(['Saque rápido']);g.beginCombat();
  const r=g.drawWeaponInCombat('p107_rifle',{useQuickDraw:true});
  assert.equal(r.incidental,true);assert.equal(r.combat.playerTurn.maneuversUsed,0);assert.equal(g.state.player.character.activeWeaponId,'p107_rifle');
  assert.throws(()=>g.drawWeaponInCombat('p107_pistol',{useQuickDraw:true}),/já foi usado neste turno/i);
});

test('P107: Passo Lateral aprimora dificuldade só de ataque ranged até o próximo turno',()=>{
  const g=game(['Passo lateral']);
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});const npc=started.combat.adversaries[0];
  g.useSideStep({strain:1});
  const resolved=g._resolveNpcTurn(structuredClone(g.state.combat),structuredClone(g.state.player.character),npc.id,()=>0.1);
  assert.equal(resolved.attack.defensiveTalent,'Passo Lateral');assert.equal(resolved.attack.defensiveTalentUpgrades,1);assert.ok(resolved.attack.challenge>=1);
  g.finishPlayerInitiativeTurn({rng:()=>0.1});
  assert.equal(g.state.player.character.combatEffects?.sideStep,undefined);
});

test('P107: Postura Defensiva aprimora dificuldade só de ataque melee até o próximo turno',()=>{
  const g=game(['Postura defensiva']);
  const npc=createAdversary('stormtrooper',{range:'Interação'});npc.weapons=[npc.weapons.find(w=>w.skill==='armas_cuerpo_a_cuerpo')];
  const started=g.beginEncounter({adversaries:[npc],rng:()=>0});const id=started.combat.adversaries[0].id;
  g.useDefensiveStanceTalent({strain:1});
  const resolved=g._resolveNpcTurn(structuredClone(g.state.combat),structuredClone(g.state.player.character),id,()=>0.1);
  assert.equal(resolved.attack.defensiveTalent,'Postura Defensiva');assert.equal(resolved.attack.defensiveTalentUpgrades,1);assert.ok(resolved.attack.challenge>=1);
  g.finishPlayerInitiativeTurn({rng:()=>0.1});
  assert.equal(g.state.player.character.combatEffects?.defensiveStanceTalent,undefined);
});

test('P107: bridge expõe as novas ações de talentos de combate',()=>{
  const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  for(const marker of ['activateFrenziedAttack','useDefensiveStanceTalent','useSideStep','useSecondWind']) assert.match(bridge,new RegExp(marker));
});

test('P107: R46 oferece controles somente para talentos adquiridos e chama o bridge real',()=>{
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(runtime,/R54 P107 — EOTE CORE COMBAT TALENTS/);
  for(const marker of ["call('activateFrenziedAttack'","call('useSideStep'","call('useDefensiveStanceTalent'","call('useSecondWind'","useQuickDraw:true"]) assert.ok(runtime.includes(marker),marker);
});
