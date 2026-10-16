import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { applyStatusEffect, statusModifiers } from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import { activeWeaponQualities } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='p99'){const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key));g.loadStarterCharacter('tendaar');return g}
function start(g,target=createAdversary('imperial_officer')){g.beginEncounter({adversaries:[target],rng:()=>0});return g.state.combat.adversaries[0]}
function pending(g,target,adv=4){g.state.combat.pendingPlayerSpend={attackId:'atk_p99',adversaryId:target.id,weaponIndex:0,weapon:'P99',remainingAdvantage:adv,remainingTriumph:0,qualities:[{key:'knockdown',label:'Derrubar',rank:1,cost:2,repeatable:false}],activations:{},attackSnapshot:{hit:true,damage:6,pierce:0,damageType:'wounds',baseDamage:6,netSuccess:1}}}
function equip(g,{skill='armas_distancia_ligeras',range='Curto'}={}){const c=structuredClone(g.state.player.character);c.characteristics.agilidad=4;c.characteristics.fortaleza=4;c.skills[skill]=4;c.weapons=[{id:'p99',name:'Arma P99',skill,damage:6,crit:3,range,qualities:[],active:true,equipped:true}];c.activeWeaponId='p99';g.commit('CHARACTER_UPDATED',{character:c});}

test('P99 Derrubar aparece como qualidade ativa por 2 Vantagens base',()=>{const q=activeWeaponQualities({qualities:['Derrubar']},{hit:true});const k=q.find(x=>x.key==='knockdown');assert.ok(k);assert.equal(k.cost,2);});

test('P99 Derrubar aplica prone persistente e consome orçamento único',()=>{const g=game('p99-knock');const t=start(g);pending(g,t,2);const r=g.activatePendingWeaponQuality({qualityKey:'knockdown'});assert.equal(r.remainingAdvantage,0);const a=g.state.combat.adversaries.find(x=>x.id===t.id);assert.equal(statusModifiers(a).prone,true);assert.equal(a.statusEffects.find(x=>x.type==='prone').duration,'encounter');});

test('P99 Derrubar cobra +1 Vantagem por Silhueta do alvo acima do atacante',()=>{const g=game('p99-size');const raw=createAdversary('imperial_officer');raw.silhouette=3;const t=start(g,raw);pending(g,t,3);assert.throws(()=>g.activatePendingWeaponQuality({qualityKey:'knockdown'}),/Vantagens insuficientes/i);g.state.combat.pendingPlayerSpend.remainingAdvantage=4;const r=g.activatePendingWeaponQuality({qualityKey:'knockdown'});assert.equal(r.effect.attackerSilhouette,1);assert.equal(r.effect.targetSilhouette,3);assert.equal(r.effect.cost,4);});

test('P99 alvo prone concede Boost a ataque melee e Setback a ataque ranged',()=>{const ranged=game('p99-ranged');equip(ranged,{skill:'armas_distancia_ligeras',range:'Curto'});let t=start(ranged);t.range='Curto';t=applyStatusEffect(t,{type:'prone',duration:'encounter'});ranged.state.combat.adversaries[0]=t;const ar=ranged.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:t.id,rng:()=>0.3});assert.ok(ar.check.pool.setback>=1);
 const melee=game('p99-melee');equip(melee,{skill:'armas_cuerpo_a_cuerpo',range:'Interação'});let m=start(melee);m.range='Interação';m=applyStatusEffect(m,{type:'prone',duration:'encounter'});melee.state.combat.adversaries[0]=m;const am=melee.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:m.id,rng:()=>0.3});assert.ok(am.check.pool.boost>=1);});

test('P99 jogador caído precisa gastar manobra real para levantar',()=>{const g=game('p99-stand');start(g);let c=applyStatusEffect(g.state.player.character,{type:'prone',duration:'encounter'});g.commit('CHARACTER_UPDATED',{character:c});assert.equal(g.statusSummary().prone,true);const r=g.standUpFromProne({maneuverMethod:'normal'});assert.equal(statusModifiers(r.character).prone,false);assert.equal(r.combat.playerTurn.maneuversUsed,1);assert.throws(()=>g.standUpFromProne(),/não está caído/i);});

test('P99 NPC prone levanta no próprio turno antes de agir',()=>{const g=game('p99-npcstand');let t=start(g);t=applyStatusEffect(t,{type:'prone',duration:'encounter'});g.state.combat.adversaries[0]=t;const r=g._resolveNpcTurn(g.state.combat,g.state.player.character,t.id,()=>0.2);const after=r.combat.adversaries.find(x=>x.id===t.id);assert.equal(statusModifiers(after).prone,false);assert.match(r.summary.text,/levantou-se/i);});

test('P99 bridge/contratos/R46 expõem Levantar-se sem mutação bruta de prone',async()=>{const fs=await import('node:fs/promises');const bridge=await fs.readFile(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');const runtime=await fs.readFile(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');const c1=await fs.readFile(new URL('../gameBridgeClient.ts',import.meta.url),'utf8');assert.match(bridge,/standUpFromProne\(options/);assert.match(runtime,/data-p99-stand/);assert.match(c1,/standUpFromProne\?:/);assert.doesNotMatch(runtime,/applyStatus\(['"]prone/);});
