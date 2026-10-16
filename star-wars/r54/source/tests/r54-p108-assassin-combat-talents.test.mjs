import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { deadlyAccuracySpec } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

class MemoryRepository{constructor(state=createCampaign('P108')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(talents=[]){
 const c=createBlankCharacter('P108');
 c.characteristics={fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:2};
 c.skills.pelea=2;c.skills.armas_cuerpo_a_cuerpo=2;c.skills.armas_distancia_ligeras=2;c.skills.armas_distancia_pesadas=2;c.skills.frialdad=2;
 c.derived={soak:2,wounds:{threshold:30,current:0},strain:{threshold:20,current:0},defense:{ranged:0,melee:0}};
 c.talents=[...talents];
 c.weapons=[
  {id:'p108_rifle',name:'Rifle blaster',skill:'armas_distancia_pesadas',damage:9,crit:3,range:'Médio',active:true,equipped:true},
  {id:'p108_blade',name:'Lâmina',skill:'armas_cuerpo_a_cuerpo',damage:5,crit:3,range:'Interação',active:false,equipped:false}
 ];c.activeWeaponId='p108_rifle';return c;
}
function game(talents=[]){const repo=new MemoryRepository();const g=new GameController(repo);g.setCharacter(hero(talents));return g;}
function encounter(g,{range='Médio',npc='imperial_officer'}={}){const a=createAdversary(npc,{range});const r=g.beginEncounter({adversaries:[a],rng:()=>0});return r.combat.adversaries[0];}

test('P108: Esquiva sofre Tensão e aprimora exatamente o próximo ataque recebido',()=>{
 const g=game(['Esquiva','Esquiva']);const npc=encounter(g,{range:'Médio'});const before=g.state.player.character.derived.strain.current;
 const prep=g.prepareDodge({strain:2});assert.equal(prep.difficultyUpgrades,2);assert.equal(g.state.player.character.derived.strain.current,before+2);
 const one=g._resolveNpcTurn(structuredClone(g.state.combat),structuredClone(g.state.player.character),npc.id,()=>0.1);
 assert.equal(one.attack.dodgeApplied,true);assert.equal(one.attack.dodgeUpgrades,2);assert.ok(one.attack.challenge>=2);assert.equal(one.character.combatEffects?.dodgeNextIncoming,undefined);
});

test('P108: Mira Precisa paga manobra/Tensão e reduz defesa do alvo no próximo ataque',()=>{
 const g=game(['Mira precisa','Mira precisa']);const npc=encounter(g,{range:'Médio'});npc.defense={ranged:2,melee:1};g.state.combat.adversaries[0]=npc;g.repository.save(g.state);
 const before=g.state.player.character.derived.strain.current;const prep=g.usePreciseAim({strain:1});assert.equal(prep.defenseReduction,1);assert.equal(g.state.player.character.derived.strain.current,before+1);
 const atk=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:npc.id,rng:()=>0.1});assert.equal(atk.check.targetDefense,1);assert.equal(atk.check.preciseAim.defenseReduction,1);assert.equal(g.state.combat.pendingPreciseAim,undefined);
});

test('P108: Tiro de Precisão estende alcance e aprimora dificuldade por banda',()=>{
 const g=game(['Tiro de precisão']);const npc=encounter(g,{range:'Longo'});g.useSniperShot({bands:1});
 const atk=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:npc.id,rng:()=>0.1});assert.equal(atk.check.sniperShot.bands,1);assert.ok(atk.challenge>=1);assert.equal(atk.targetRange,'Longo');
});

test('P108: Golpe Direcionado só gasta Destiny após acerto e soma Agilidade',()=>{
 const g=game(['Golpe direcionado']);const npc=encounter(g,{range:'Médio'});g.setGroupDestiny({light:1,dark:0});g.prepareTargetedBlow();
 const atk=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:npc.id,rng:()=>0.1});assert.equal(atk.hit,true);assert.equal(atk.destinyTalent.talent,'Golpe direcionado');assert.equal(atk.destinyTalent.amount,3);assert.deepEqual(g.state.group.destiny,{light:0,dark:1});
});

test('P108: Anatomia só gasta Destiny após acerto e soma Intelecto',()=>{
 const g=game(['Anatomia']);const npc=encounter(g,{range:'Médio'});g.setGroupDestiny({light:1,dark:0});g.prepareAnatomyLessons();
 const atk=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:npc.id,rng:()=>0.1});assert.equal(atk.hit,true);assert.equal(atk.destinyTalent.talent,'Anatomia');assert.equal(atk.destinyTalent.amount,2);assert.deepEqual(g.state.group.destiny,{light:0,dark:1});
});

test('P108: Precisão Letal usa a perícia escolhida na compra e soma ranks ao dano',()=>{
 const g=game(['Precisão letal']);g.state.player.character.talentPurchases=[{originalName:'DEADACC',choice:'armas_distancia_pesadas'}];g.repository.save(g.state);assert.deepEqual(deadlyAccuracySpec(g.state.player.character),{available:true,skillKey:'armas_distancia_pesadas',bonus:2});
 const npc=encounter(g,{range:'Médio'});const atk=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:npc.id,rng:()=>0.1});assert.equal(atk.hit,true);assert.ok(atk.talentDamageDetails.some(x=>x.talent==='Precisão letal'&&x.amount===2));
});

test('P108: bridge expõe todas as novas ações e R46 contém bloco P108',()=>{
 const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');for(const m of ['prepareDodge','usePreciseAim','useSniperShot','prepareTargetedBlow','prepareAnatomyLessons'])assert.match(bridge,new RegExp(m));
 const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');assert.match(runtime,/R54 P108/);
});
