import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { sufferStrain } from '../app/src/main/assets/vtt/js/rules/health.mjs';
import { recoverAfterEncounter } from '../app/src/main/assets/vtt/js/rules/recovery.mjs';

class MemoryRepository{constructor(state=createCampaign('P109')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(talents=[]){const c=createBlankCharacter('P109');c.characteristics={fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:2};c.skills.frialdad=2;c.skills.armas_cuerpo_a_cuerpo=2;c.derived={soak:3,wounds:{threshold:30,current:0},strain:{threshold:20,current:5},defense:{ranged:0,melee:0}};c.talents=[...talents];c.gear=[{id:'heavy_battle_armor',name:'Armadura pesada de combate',quantity:1}];c.weapons=[{id:'blade',name:'Lâmina',skill:'armas_cuerpo_a_cuerpo',damage:5,crit:3,range:'Interação',active:true,equipped:true}];c.activeWeaponId='blade';return c}
function game(t=[]){const g=new GameController(new MemoryRepository());g.setCharacter(hero(t));return g}

test('P109: Determinação reduz Tensão involuntária por rank, mínimo 1',()=>{const c=hero(['Determinação','Determinação']);const r=sufferStrain(c,4);assert.equal(r.amount,2);assert.equal(r.resolve.reduced,2);assert.equal(r.character.derived.strain.current,7);const min=sufferStrain(c,1);assert.equal(min.amount,1)});

test('P109: Determinação não reduz Tensão voluntária',()=>{const c=hero(['Determinação','Determinação']);const r=sufferStrain(c,4,{voluntary:true});assert.equal(r.amount,4);assert.equal(r.resolve.reduced,0);assert.equal(r.character.derived.strain.current,9)});

test('P109: custo voluntário de Ataque Frenético ignora Determinação',()=>{const g=game(['Determinação','Determinação','Ataque frenético','Ataque frenético']);g.beginCombat();const before=g.state.player.character.derived.strain.current;g.activateFrenziedAttack({strain:2});assert.equal(g.state.player.character.derived.strain.current,before+2)});

test('P109: Armeiro soma +1 soak somente com armadura vestida',()=>{const g=game(['Armeiro']);const r=g.equipArmor('heavy_battle_armor');assert.equal(r.values.talentBonuses.soakBonus,1);assert.equal(g.state.player.character.derived.soak,6);g.unequipArmor();assert.equal(g.state.player.character.derived.soak,3)});

test('P109: Armeiro aprimorado soma +1 Defesa quando armadura tem soak 2+',()=>{const g=game(['Armeiro','Armeiro aprimorado']);const r=g.equipArmor('heavy_battle_armor');assert.equal(r.values.talentBonuses.defenseBonus,1);assert.deepEqual(g.state.player.character.derived.defense,{ranged:2,melee:2})});

test('P109: Recuperação rápida continua somando +1 Tensão recuperada por rank após encontro',()=>{const c=hero(['Recuperação rápida','Recuperação rápida']);c.derived.strain.current=10;const r=recoverAfterEncounter(c,{rng:()=>0.1});assert.equal(r.talentBonus,2);assert.ok(r.recovered>=2)});
