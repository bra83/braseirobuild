import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';

class Repo{constructor(s=createCampaign('P110')){this.state=structuredClone(s)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(t=[]){const c=createBlankCharacter('P110');c.characteristics={fortaleza:3,agilidad:3,intelecto:3,astucia:3,voluntad:3,presencia:3};for(const k of ['sigilo','actividad_criminal','medicina','computadoras','coaccion'])c.skills[k]=2;c.derived={soak:3,wounds:{threshold:20,current:0},strain:{threshold:30,current:0},defense:{ranged:0,melee:0}};c.talents=[...t];return c}
function game(t=[]){const g=new GameController(new Repo());g.setCharacter(hero(t));return g}

test('P110: Mestre das Sombras sofre 2 Tensão e reduz dificuldade de Sigilo',()=>{const g=game(['Mestre das sombras']);const r=g.resolveSkillCheck({skillKey:'sigilo',difficulty:3,useMasterOfShadows:true,rng:()=>0.1});assert.equal(r.pool.difficulty,2);assert.equal(r.activeSkillTalents[0].label,'Mestre das sombras');assert.equal(g.state.player.character.derived.strain.current,2)});

test('P110: Mestre das Sombras rejeita perícia fora de Sigilo/Atividade Criminal',()=>{const g=game(['Mestre das sombras']);assert.throws(()=>g.resolveSkillCheck({skillKey:'computadoras',difficulty:3,useMasterOfShadows:true,rng:()=>0.1}),/não se aplica/i)});

test('P110: Médico Mestre reduz Medicina a no mínimo Fácil',()=>{const g=game(['Médico mestre']);const r=g.resolveSkillCheck({skillKey:'medicina',difficulty:1,useMasterDoctor:true,rng:()=>0.1});assert.equal(r.pool.difficulty,1);assert.equal(g.state.player.character.derived.strain.current,2)});

test('P110: Slicer Mestre reduz dificuldade de Computadores',()=>{const g=game(['Slicer mestre']);const r=g.resolveSkillCheck({skillKey:'computadoras',difficulty:4,useMasterSlicer:true,rng:()=>0.1});assert.equal(r.pool.difficulty,3);assert.equal(r.activeSkillTalents[0].label,'Slicer mestre')});

test('P110: Intimidador sofre Tensão e faz downgrade real da dificuldade de Coerção',()=>{const g=game(['Intimidador','Intimidador']);const r=g.resolveSkillCheck({skillKey:'coaccion',difficulty:1,challenge:2,intimidatingStrain:2,rng:()=>0.1});assert.equal(r.pool.challenge,0);assert.equal(r.pool.difficulty,3);assert.equal(r.intimidating.downgrades,2);assert.equal(g.state.player.character.derived.strain.current,2)});

test('P110: Determinação não reduz custo voluntário dos talentos mestre',()=>{const g=game(['Determinação','Determinação','Médico mestre']);g.resolveSkillCheck({skillKey:'medicina',difficulty:2,useMasterDoctor:true,rng:()=>0.1});assert.equal(g.state.player.character.derived.strain.current,2)});
