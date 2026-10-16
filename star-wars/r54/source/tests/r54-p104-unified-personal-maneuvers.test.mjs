import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { applyStatusEffect } from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import { addStarterCritical } from '../app/src/main/assets/vtt/js/rules/critical.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

class MemoryRepository{constructor(state=createCampaign('P104')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(){
  const c=createBlankCharacter('P104');
  c.characteristics={fortaleza:2,agilidad:3,intelecto:3,astucia:2,voluntad:3,presencia:2};
  c.skills.armas_distancia_ligeras=2;c.skills.atletismo=1;
  c.derived={soak:2,wounds:{threshold:12,current:3},strain:{threshold:14,current:0},defense:{ranged:0,melee:0}};
  c.gear=[
    {id:'cargo_p104',name:'Carga P104',encumbrance:9,quantity:1},
    {id:'stimpack_injector',name:'Injetor de estimulantes',encumbrance:0,quantity:2},
    {id:'power_pack_or_ammo_reload',name:'Recarga adicional',encumbrance:0,quantity:2}
  ];
  c.armor=[];
  c.weapons=[
    {id:'w1',name:'Pistola P104',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Curto',encumbrance:0,qualities:[],active:true,equipped:true},
    {id:'w2',name:'Carabina P104',skill:'armas_distancia_ligeras',damage:7,crit:3,range:'Médio',encumbrance:0,qualities:['Preparação 1'],active:false,equipped:false},
    {id:'w3',name:'Lançador P104',skill:'armas_distancia_ligeras',damage:8,crit:3,range:'Médio',encumbrance:0,qualities:['Munição Limitada 1'],runtimeState:{limitedAmmoRemaining:0},active:false,equipped:false}
  ];
  c.activeWeaponId='w1';
  return c;
}
function overloaded({prone=false}={}){
  const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();
  if(prone)c=applyStatusEffect(c,{type:'prone',duration:'encounter'});
  g.setCharacter(c);g.beginCombat();assert.equal(g.encumbranceStatus().severelyEncumbered,true);return g;
}
function paidByStrain(setup,run,verify){
  const g=setup();
  assert.throws(()=>run(g,'normal'),/carga elimina a manobra gratuita/i);
  const before=g.state.player.character.derived.strain.current;
  const r=run(g,'strain');
  assert.equal(r.strainCost,2);assert.equal(r.combat.playerTurn.maneuversUsed,1);assert.equal(g.state.player.character.derived.strain.current,before+2);
  verify?.(g,r);
}

test('P104: Mirar e Cobertura usam o mesmo custo de manobra sob carga grave',()=>{
  paidByStrain(()=>overloaded(),(g,m)=>g.aim({maneuverMethod:m}),(g)=>assert.equal(g.state.combat.pendingAttackBoost,1));
  paidByStrain(()=>overloaded(),(g,m)=>g.takeCover({maneuverMethod:m}),(g)=>assert.equal(g.state.player.character.combatEffects.coverRangedDefense,1));
});

test('P104: Postura Defensiva e Levantar-se usam o mesmo custo sob carga grave',()=>{
  paidByStrain(()=>overloaded(),(g,m)=>g.takeGuardedStance({maneuverMethod:m}),(g,r)=>assert.ok(r.guardedStance));
  paidByStrain(()=>overloaded({prone:true}),(g,m)=>g.standUpFromProne({maneuverMethod:m}),(g)=>assert.equal(g.statusSummary().prone,false));
});

test('P104: sacar/trocar, Preparação e recarga usam o mesmo custo sob carga grave',()=>{
  paidByStrain(()=>overloaded(),(g,m)=>g.drawWeaponInCombat('w2',{maneuverMethod:m}),(g)=>assert.equal(g.state.player.character.activeWeaponId,'w2'));
  paidByStrain(()=>overloaded(),(g,m)=>g.prepareWeaponForAttack('w2',{maneuverMethod:m}),(g,r)=>assert.equal(r.preparedManeuvers,1));
  paidByStrain(()=>overloaded(),(g,m)=>g.reloadLimitedAmmoWeapon('w3',{maneuverMethod:m}),(g,r)=>{assert.equal(r.limitedAmmoRemaining,1);assert.equal(g.state.player.character.gear.find(x=>x.id==='power_pack_or_ammo_reload').quantity,1)});
});

test('P104: Stimpack em combate também respeita carga grave e orçamento de manobra',()=>{
  paidByStrain(()=>overloaded(),(g,m)=>g.usePlayerStimpack({maneuverMethod:m}),(g,r)=>{assert.equal(r.healed,3);assert.equal(g.state.player.character.gear.find(x=>x.id==='stimpack_injector').quantity,1)});
});

test('P104: trocar a ação paga a primeira manobra quando a carga remove a gratuita',()=>{
  const g=overloaded();const before=g.state.player.character.derived.strain.current;
  const r=g.takeCover({maneuverMethod:'action'});
  assert.equal(r.strainCost,0);assert.equal(r.combat.playerTurn.maneuversUsed,1);assert.equal(r.combat.playerTurn.actionUsed,true);assert.equal(g.state.player.character.derived.strain.current,before);
});

test('P104: Ferimento Crítico sem manobra gratuita usa o mesmo resolvedor e pode ser pago com ação',()=>{
  const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();c.gear=c.gear.filter(x=>x.id!=='cargo_p104');
  c=addStarterCritical(c,'Hamstrung',{roll:71}).character;g.setCharacter(c);g.beginCombat();
  assert.equal(g.encumbranceStatus().severelyEncumbered,false);
  assert.throws(()=>g.aim({maneuverMethod:'normal'}),/Ferimento Crítico remove a manobra gratuita/i);
  const r=g.aim({maneuverMethod:'action'});assert.equal(r.strainCost,0);assert.equal(r.combat.playerTurn.actionUsed,true);assert.equal(r.combat.playerTurn.maneuversUsed,1);
});

test('P104: controller não mantém caminhos paralelos de spendManeuver fora do resolvedor unificado',()=>{
  const controller=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/controller.mjs',import.meta.url),'utf8');
  assert.match(controller,/function spendPlayerManeuver/);
  assert.equal((controller.match(/combat_mjs_1\.spendManeuver\(/g)||[]).length,2);
});

test('P104: R46 propaga a escolha de pagamento para Mirar, Stimpack, Preparação e Recarga',()=>{
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(runtime,/R54 P104 — UNIFIED PERSONAL MANEUVER COSTS/);
  assert.match(runtime,/call\('aim',\{maneuverMethod:document\.querySelector\('\[data-p94-method\]'\)/);
  assert.match(runtime,/call\('usePlayerStimpack',\{maneuverMethod:document\.querySelector\('\[data-p94-method\]'\)/);
  assert.match(runtime,/prepareWeaponForAttack',w\.id\?\?w\.name,\{maneuverMethod:document\.querySelector\('\[data-p94-method\]'\)/);
  assert.match(runtime,/reloadLimitedAmmoWeapon',w\.id\?\?w\.name,\{maneuverMethod:document\.querySelector\('\[data-p94-method\]'\)/);
  assert.match(runtime,/Pagar 2 de Tensão/);assert.match(runtime,/Trocar a ação por manobra/);
});

test('P104: Ficar caído é uma manobra real, persistente e usa o orçamento unificado',()=>{
  paidByStrain(()=>overloaded(),(g,m)=>g.dropProne({maneuverMethod:m}),(g,r)=>{assert.equal(r.prone,true);assert.equal(g.statusSummary().prone,true)});
});

test('P104: Mirar específico aplica 2 Setbacks com uma mira e 1 com duas miras consecutivas',()=>{
  const setup=()=>{const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();c.gear=c.gear.filter(x=>x.id!=='cargo_p104');g.setCharacter(c);g.beginCombat();return g};
  let g=setup();
  g.aim({mode:'targeted',targetDetail:'blaster do alvo'});
  g.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',rng:()=>0.42});
  assert.equal(g.state.lastCheck.targetedAimSetback,2);assert.equal(g.state.lastCheck.aim.mode,'targeted');assert.equal(g.state.combat.pendingAim,undefined);
  g=setup();
  g.aim({mode:'targeted',targetDetail:'blaster do alvo'});
  g.aim({maneuverMethod:'strain',mode:'targeted',targetDetail:'blaster do alvo'});
  g.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',rng:()=>0.42});
  assert.equal(g.state.lastCheck.targetedAimSetback,1);assert.equal(g.state.lastCheck.aim.count,2);
});

test('P104: qualquer outra manobra ou ação cancela Mirar antes do próximo ataque',()=>{
  const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();c.gear=c.gear.filter(x=>x.id!=='cargo_p104');g.setCharacter(c);g.beginCombat();
  g.aim({mode:'boost'});assert.equal(g.state.combat.pendingAttackBoost,1);
  g.takeCover({maneuverMethod:'strain'});assert.equal(g.state.combat.pendingAttackBoost,0);assert.equal(g.state.combat.pendingAim,undefined);
  g.advancePlayerTurn?.({});
});

test('P104: dano que ultrapassa soak cancela Mirar',()=>{
  const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();c.gear=c.gear.filter(x=>x.id!=='cargo_p104');g.setCharacter(c);g.beginCombat();
  g.aim({mode:'boost'});assert.equal(g.state.combat.pendingAttackBoost,1);
  g.applyPlayerDamage({damage:7,pierce:0});assert.equal(g.state.combat.pendingAttackBoost,0);assert.equal(g.state.combat.pendingAim,undefined);
});

test('P104: bridge e R46 expõem Ficar Caído e os dois modos de Mirar',()=>{
  const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(bridge,/dropProne\(options = \{\}\)/);assert.match(runtime,/data-p104-drop-prone/);assert.match(runtime,/call\('dropProne'/);
  assert.match(runtime,/data-p104-aim-mode/);assert.match(runtime,/value="targeted"/);assert.match(runtime,/targetDetail:/);
});


test('P104: Médio↔Longo e Longo↔Extremo exigem duas manobras e só mudam a faixa na segunda',()=>{
  const repo=new MemoryRepository(),g=new GameController(repo);let c=hero();c.gear=c.gear.filter(x=>x.id!=='cargo_p104');g.setCharacter(c);
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer',{range:'Médio'})],rng:()=>0});const id=started.combat.adversaries[0].id;
  const first=g.changeCombatRange({targetAdversaryId:id,direction:'farther',maneuverMethod:'normal'});
  assert.equal(first.requiredManeuvers,2);assert.equal(first.movementComplete,false);assert.equal(first.maneuversRemaining,1);assert.equal(first.target.range,'Médio');
  assert.equal(g.state.combat.adversaries.find(a=>a.id===id).range,'Médio');assert.equal(g.state.combat.playerRangeMovement[id].to,'Longo');
  const reopened=new GameController(repo);assert.equal(reopened.state.combat.playerRangeMovement[id].remainingManeuvers,1);
  const second=reopened.changeCombatRange({targetAdversaryId:id,direction:'farther',maneuverMethod:'strain'});
  assert.equal(second.movementComplete,true);assert.equal(second.target.range,'Longo');assert.equal(second.maneuversRemaining,0);assert.equal(reopened.state.combat.playerRangeMovement,undefined);
});
