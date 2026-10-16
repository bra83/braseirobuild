import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { activeWeaponQualities } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { attackDifficultyForWeapon } from '../app/src/main/assets/vtt/js/rules/attackDifficulty.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='p97'){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key));g.loadStarterCharacter('tendaar');return g}
function encounter(g,profile='imperial_officer'){return g.beginEncounter({adversaries:[createAdversary(profile)],rng:()=>0}).combat.adversaries[0]}
function pending(g,target,quality,{adv=4,rank=1,damage=6,pierce=0,damageType='wounds',hit=true,cost=null,netSuccess=1}={}){
  const resolvedCost=cost??(quality==='sunder'?1:(quality==='blast'&&!hit?3:2));
  g.state.combat.pendingPlayerSpend={attackId:'atk_p97',adversaryId:target.id,weaponIndex:0,weapon:'P97',remainingAdvantage:adv,remainingTriumph:0,qualities:[{key:quality,label:quality,rank,cost:resolvedCost,repeatable:['sunder','autofire','linked'].includes(quality),missBlast:quality==='blast'&&!hit}],activations:{},attackSnapshot:{hit,damage,pierce,damageType,vicious:0,baseDamage:damage,netSuccess}};
}

test('P97 identifica qualidades ativas source-bound sem confundir passivas',()=>{
  const q=activeWeaponQualities({qualities:['Desorientação 3','Atordoadora 2','Incendiária 4','Hendedura','Automática']},{hit:true,useAutoFire:true});
  assert.deepEqual(q.map(x=>x.key),['autofire','disorient','stun','burn','sunder']);
  assert.equal(q.find(x=>x.key==='sunder').cost,1);
});

test('P97 orçamento de Vantagens é único: qualidade gasta impede crítico com as mesmas Vantagens',()=>{
  const g=game('p97-budget');const target=encounter(g);
  pending(g,target,'disorient',{adv:2,rank:2});
  g.state.combat.pendingPlayerCritical={adversaryId:target.id,weapon:'P97',cost:2,byTriumph:false,vicious:0};
  const r=g.activatePendingWeaponQuality({qualityKey:'disorient'});assert.equal(r.remainingAdvantage,0);
  assert.throws(()=>g.activatePendingPlayerCritical({rng:()=>0}),/Resultados insuficientes/i);
});

test('P97 Desorientação aplica Setback real ao próximo ataque do adversário e expira por turnos',()=>{
  const g=game('p97-disorient');const target=encounter(g);
  pending(g,target,'disorient',{adv:2,rank:2});g.activatePendingWeaponQuality({qualityKey:'disorient'});
  const before=g.state.combat.adversaries.find(a=>a.id===target.id);assert.equal(before.statusEffects[0].type,'disoriented');assert.equal(before.statusEffects[0].roundsRemaining,2);
  const resolved=g._resolveNpcTurn(g.state.combat,g.state.player.character,target.id,()=>0.2);
  assert.ok(resolved.attack.check.pool.setback>=1);
  const after=resolved.combat.adversaries.find(a=>a.id===target.id);assert.equal(after.statusEffects[0].roundsRemaining,1);
});

test('P97 Atordoadora causa Tensão direta e rival converte em Ferimentos',()=>{
  const g=game('p97-stun');const target=encounter(g,'imperial_officer');
  pending(g,target,'stun',{adv:2,rank:3});const r=g.activatePendingWeaponQuality({qualityKey:'stun'});
  assert.equal(r.effect.strain,3);assert.equal(r.effect.convertedToWounds,true);
  assert.equal(g.state.combat.adversaries.find(a=>a.id===target.id).wounds.current,3);
});

test('P97 Incendiária persiste e aplica dano base no início dos turnos do alvo',()=>{
  const g=game('p97-burn');const target=encounter(g);
  pending(g,target,'burn',{adv:2,rank:2,damage:8});g.activatePendingWeaponQuality({qualityKey:'burn'});
  let current=g.state.combat.adversaries.find(a=>a.id===target.id);assert.equal(current.ongoingEffects[0].roundsRemaining,2);
  const before=current.wounds.current;const resolved=g._resolveNpcTurn(g.state.combat,g.state.player.character,target.id,()=>0.2);
  current=resolved.combat.adversaries.find(a=>a.id===target.id);assert.ok(current.wounds.current>before);assert.equal(current.ongoingEffects[0].roundsRemaining,1);
});

test('P97 Hendedura/Sunder gasta 1 Vantagem por etapa e dano Moderate aumenta dificuldade da arma NPC',()=>{
  const g=game('p97-sunder');const target=encounter(g);
  pending(g,target,'sunder',{adv:2});g.activatePendingWeaponQuality({qualityKey:'sunder',targetWeaponIndex:0});g.activatePendingWeaponQuality({qualityKey:'sunder',targetWeaponIndex:0});
  const damaged=g.state.combat.adversaries.find(a=>a.id===target.id);assert.equal(damaged.weapons[0].damageState,'moderate');assert.equal(g.state.combat.pendingPlayerSpend.remainingAdvantage,0);
  const base=attackDifficultyForWeapon(damaged.weapons[0],damaged.range);const resolved=g._resolveNpcTurn(g.state.combat,g.state.player.character,target.id,()=>0.2);
  assert.equal(resolved.attack.weaponDamageState,'moderate');assert.equal(resolved.attack.difficulty,base+1);
});

test('P97 Automática só aplica acerto adicional depois de gastar 2 Vantagens',()=>{
  const g=game('p97-auto');const target=encounter(g);const before=target.wounds.current;
  pending(g,target,'autofire',{adv:2,damage:8});const r=g.activatePendingWeaponQuality({qualityKey:'autofire'});
  assert.equal(r.remainingAdvantage,0);assert.ok(g.state.combat.adversaries.find(a=>a.id===target.id).wounds.current>before);
});


test('P97 Explosão em acerto exige alvos Engajados explícitos e aplica dano só aos selecionados',()=>{
  const g=game('p97-blast-hit');
  const primary=createAdversary('imperial_officer',{name:'Primário'}),secondary=createAdversary('imperial_officer',{name:'Secundário'}),far=createAdversary('imperial_officer',{name:'Distante'});
  g.beginEncounter({adversaries:[primary,secondary,far],rng:()=>0});
  pending(g,primary,'blast',{adv:2,rank:5,hit:true,netSuccess:2});
  assert.throws(()=>g.activatePendingWeaponQuality({qualityKey:'blast'}),/Engajado/i);
  const beforeSecondary=g.state.combat.adversaries.find(a=>a.id===secondary.id).wounds.current;
  const beforeFar=g.state.combat.adversaries.find(a=>a.id===far.id).wounds.current;
  const r=g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[secondary.id]});
  assert.equal(r.remainingAdvantage,0);assert.equal(r.effect.damage,7);assert.equal(r.effect.targets.length,1);assert.equal(r.effect.targets[0].id,secondary.id);
  assert.ok(g.state.combat.adversaries.find(a=>a.id===secondary.id).wounds.current>beforeSecondary);
  assert.equal(g.state.combat.adversaries.find(a=>a.id===far.id).wounds.current,beforeFar);
});

test('P97 Explosão em ataque que errou custa 3 Vantagens e atinge o alvo original',()=>{
  const g=game('p97-blast-miss');const target=encounter(g);const before=target.wounds.current;
  pending(g,target,'blast',{adv:3,rank:6,hit:false,netSuccess:0});
  const r=g.activatePendingWeaponQuality({qualityKey:'blast'});
  assert.equal(r.remainingAdvantage,0);assert.equal(r.effect.missBlast,true);assert.equal(r.effect.damage,6);assert.equal(r.effect.targets[0].id,target.id);
  assert.ok(g.state.combat.adversaries.find(a=>a.id===target.id).wounds.current>before);
});


test('P97 Concussiva deixa o alvo Atordoado e ele perde a próxima ação',()=>{
  const g=game('p97-concussive');const target=encounter(g);
  pending(g,target,'concussive',{adv:2,rank:1});g.activatePendingWeaponQuality({qualityKey:'concussive'});
  const before=g.state.combat.adversaries.find(a=>a.id===target.id);assert.equal(before.statusEffects[0].type,'staggered');
  const resolved=g._resolveNpcTurn(g.state.combat,g.state.player.character,target.id,()=>0.2);
  assert.match(resolved.summary.text,/perde a ação/i);
});

test('P97 Enredante aplica immobilized pelo rating sem inventar dano',()=>{
  const g=game('p97-ensnare');const target=encounter(g);const wounds=target.wounds.current;
  pending(g,target,'ensnare',{adv:2,rank:3});const r=g.activatePendingWeaponQuality({qualityKey:'ensnare'});
  const after=g.state.combat.adversaries.find(a=>a.id===target.id);const imm=after.statusEffects.find(x=>x.type==='immobilized');
  assert.ok(imm);assert.equal(imm.roundsRemaining,3);assert.equal(after.wounds.current,wounds);assert.equal(r.remainingAdvantage,0);
});

test('P97 Vinculada respeita rating e orçamento compartilhado',()=>{
  const g=game('p97-linked');const target=encounter(g);const before=target.wounds.current;
  pending(g,target,'linked',{adv:4,rank:2,damage:8});
  g.activatePendingWeaponQuality({qualityKey:'linked'});g.activatePendingWeaponQuality({qualityKey:'linked'});
  assert.equal(g.state.combat.pendingPlayerSpend.remainingAdvantage,0);assert.equal(g.state.combat.pendingPlayerSpend.activations.linked,2);
  assert.ok(g.state.combat.adversaries.find(a=>a.id===target.id).wounds.current>before);
  assert.throws(()=>g.activatePendingWeaponQuality({qualityKey:'linked'}),/Vinculados disponíveis/i);
});

test('P97 Modo atordoante é incidental persistente, muda dano para Tensão e limita alcance a Curto',()=>{
  const g=game('p97-stun-setting');
  const c=structuredClone(g.state.player.character);c.characteristics.agilidad=4;c.skills.armas_distancia_ligeras=4;c.weapons=[{id:'stun_pistol',name:'Pistola P97',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Médio',qualities:['Modo atordoante'],active:true,equipped:true}];c.activeWeaponId='stun_pistol';g.commit('CHARACTER_UPDATED',{character:c});
  const target=encounter(g);g.state.combat.adversaries[0].range='Médio';
  const toggled=g.toggleWeaponStunSetting('stun_pistol');assert.equal(toggled.stunSettingActive,true);
  assert.throws(()=>g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.2}),/limita o alcance a Curto/i);
  g.state.combat.adversaries[0].range='Curto';
  const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.2});assert.equal(attack.hit,true);assert.equal(attack.damageType,'strain');assert.equal(attack.stunSettingActive,true);
});

test('P97 bridge e R46 expõem gasto explícito de qualidades e Modo atordoante',async()=>{
  const fs=await import('node:fs/promises');
  const bridge=await fs.readFile(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const runtime=await fs.readFile(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(bridge,/activatePendingWeaponQuality\(options/);assert.match(bridge,/toggleWeaponStunSetting\(weaponId/);
  assert.match(runtime,/data-p97-quality/);assert.match(runtime,/Vantagens restantes/);assert.match(runtime,/data-p97-stun-setting/);assert.match(runtime,/data-p97-blast-target/);
});
