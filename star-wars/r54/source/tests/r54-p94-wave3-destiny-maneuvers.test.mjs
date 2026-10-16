import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

class MemoryRepository{constructor(state=createCampaign('P94')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function game(){const repo=new MemoryRepository(),g=new GameController(repo);g.loadStarterCharacter('cael');return {g,repo}}

test('P94: Destino luminoso gasto vira sombrio e persiste',()=>{
  const {g,repo}=game();
  g.state.group.destiny={light:2,dark:1}; repo.save(g.state);
  assert.deepEqual(g.destinySummary().pool,{light:2,dark:1});
  const after=g.spendDestiny('light');
  assert.deepEqual(after,{light:1,dark:2});
  const reopened=new GameController(repo);
  assert.deepEqual(reopened.destinySummary().pool,{light:1,dark:2});
});

test('P94: movimento entre faixas consome manobra e segunda manobra custa 2 de Tensão',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});
  const id=started.combat.adversaries[0].id;
  const strain0=g.state.player.character.derived.strain.current;
  const one=g.changeCombatRange({targetAdversaryId:id,direction:'closer',maneuverMethod:'normal'});
  assert.equal(one.to,'Curto');
  assert.equal(one.combat.playerTurn.maneuversUsed,1);
  assert.equal(one.strainCost,0);
  const two=g.changeCombatRange({targetAdversaryId:id,direction:'closer',maneuverMethod:'strain'});
  assert.equal(two.to,'Interação');
  assert.equal(two.combat.playerTurn.maneuversUsed,2);
  assert.equal(two.strainCost,2);
  assert.equal(g.state.player.character.derived.strain.current,strain0+2);
  assert.throws(()=>g.changeCombatRange({targetAdversaryId:id,direction:'farther',maneuverMethod:'strain'}),/mais de 2 manobras/i);
});

test('P94: immobilized bloqueia manobra de movimento real',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});
  const id=started.combat.adversaries[0].id;
  g.applyPlayerCondition('immobilized',1);
  assert.throws(()=>g.changeCombatRange({targetAdversaryId:id,direction:'closer'}),/immobilized/i);
});


test('P94: cobertura e sacar/trocar arma consomem manobra real',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});
  const cover=g.takeCover({maneuverMethod:'normal'});
  assert.equal(cover.coverRangedDefense,1);
  assert.equal(g.state.player.character.combatEffects.coverRangedDefense,1);
  assert.equal(g.state.combat.playerTurn.maneuversUsed,1);
  const secondWeapon=g.state.player.character.weapons[1];
  assert.ok(secondWeapon,'preset precisa possuir uma segunda arma');
  const draw=g.drawWeaponInCombat(secondWeapon.id??secondWeapon.name,{maneuverMethod:'strain'});
  assert.equal(draw.strainCost,2);
  assert.equal(g.state.combat.playerTurn.maneuversUsed,2);
  assert.ok(g.state.player.character.weapons.some(w=>w.active&&(w.id===secondWeapon.id||w.name===secondWeapon.name)));
});

test('P94: deslocar-se entre faixas abandona cobertura',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});
  const id=started.combat.adversaries[0].id;
  g.takeCover({maneuverMethod:'normal'});
  g.advancePlayerTurn({rng:()=>0.5});
  assert.equal(g.state.player.character.combatEffects.coverRangedDefense,1);
  g.changeCombatRange({targetAdversaryId:id,direction:'closer',maneuverMethod:'normal'});
  assert.equal(g.state.player.character.combatEffects?.coverRangedDefense??0,0);
});

test('P94: equipamentos expõem attachments e hard points reais sem instalar nada automaticamente',()=>{
  const {g}=game();
  const w=g.state.player.character.weapons[0];
  w.hardPoints=3;
  w.attachments=[{id:'test-attachment',name:'Attachment auditado',hardPointsRequired:2,source:{book:'Fonte local',printedPage:1}}];
  const summary=g.equipmentOwnedSummary();
  const row=summary.weapons.find(x=>x.id===w.id||x.name===w.name);
  assert.equal(row.hardPoints,3);
  assert.equal(row.hardPointsAvailable,1);
  assert.equal(row.attachments.length,1);
});

test('P94: UI final mostra Destino, condições, movimento e hard points sem consultar RAG',()=>{
  const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
  const start=runtime.indexOf('R54 P94 — P0 WAVE 3');
  assert.ok(start>0);
  const p94=runtime.slice(start);
  for(const marker of ['MANOBRAS / CONDIÇÕES','DESTINO','HARD POINTS / ATTACHMENTS','changeCombatRange','takeCover','drawWeaponInCombat','destinySummary','spendDestiny']) assert.ok(p94.includes(marker),marker);
  assert.equal(/consultRules|MASTER_RULE_QUERY|rag\//.test(p94),false);
});

test('P94: contratos TypeScript incluem as operações novas',()=>{
  for(const file of ['gameBridgeClient.ts','shell-r46-run87/gameBridgeClient.ts']){
    const text=fs.readFileSync(file,'utf8');
    for(const method of ['destinySummary','changeCombatRange','takeCover','drawWeaponInCombat']) assert.ok(text.includes(method),`${file}:${method}`);
  }
});

test('P94: cobertura adiciona Setback real a ataque NPC ranged e some ao fim do combate',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});
  const npc=started.combat.adversaries[0];
  const plain=g._resolveNpcTurn(structuredClone(g.state.combat),structuredClone(g.state.player.character),npc.id,()=>0);
  const plainSetback=Number(plain.attack.check.pool.setback??0);
  g.takeCover({maneuverMethod:'normal'});
  const covered=g._resolveNpcTurn(structuredClone(g.state.combat),structuredClone(g.state.player.character),npc.id,()=>0);
  assert.equal(Number(covered.attack.check.pool.setback??0),plainSetback+1);
  g.finishCombat({rng:()=>0});
  assert.equal(g.state.player.character.combatEffects?.coverRangedDefense??0,0);
});

test('P94: condição disoriented visível também aplica Setback real ao ataque',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Curto'})],rng:()=>0});
  const id=started.combat.adversaries[0].id;
  g.applyPlayerCondition('disoriented',1);
  const before=g.statusSummary();
  assert.equal(before.setback,1);
  const result=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:id,rng:()=>0});
  assert.ok(Number(result.check.pool.setback??0)>=1);
});
