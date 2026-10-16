import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { naturalRerollOption } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

class Repo{constructor(s=createCampaign('P111')){this.state=structuredClone(s)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(t=[]){const c=createBlankCharacter('P111');c.characteristics={fortaleza:3,agilidad:3,intelecto:3,astucia:3,voluntad:3,presencia:3};for(const k of Object.keys(c.skills))c.skills[k]=2;c.derived={soak:3,wounds:{threshold:20,current:0},strain:{threshold:30,current:0},defense:{ranged:0,melee:0}};c.talents=[...t];return c}
function game(t=[]){const g=new GameController(new Repo());g.setCharacter(hero(t));g.startSession({rng:()=>0.99});return g}

const cases=[
 ['Brigão nato','pelea','NATBRAW'],['Encantador nato','carisma','NATCHARM'],['Médico nato','medicina','NATDOC'],['Impositor nato','coaccion','NATENF'],['Caçador nato','percepcion','NATHUN'],['Atirador nato','armas_distancia_pesadas','NATMAR'],['Negociador nato','negociacion','NATNEG'],['Sobrevivente nato','supervivencia','NATOUT'],['Piloto nato','artilleria','NATPIL'],['Programador nato','computadoras','NATPRO'],['Trapaceiro nato','sigilo','NATROG'],['Acadêmico nato','educacion','NATSCH'],['Funileiro nato','mecanica','NATTIN']
];

test('P111: catálogo Nato reconhece as 13 famílias EotE Core nas perícias corretas',()=>{for(const [name,skill,id] of cases){const spec=naturalRerollOption(hero([name]),skill);assert.equal(spec.available,true,name);assert.equal(spec.originalName,id,name)}});

test('P111: rerrolagem Nato usa o mesmo pool e preserva o primeiro resultado como evidência',()=>{const g=game(['Médico nato']);let n=0;const rng=()=>{n++;return n<7?0.99:0.01};const r=g.resolveSkillCheck({skillKey:'medicina',difficulty:2,useNaturalReroll:true,rng});assert.equal(r.naturalReroll.used,true);assert.equal(r.naturalReroll.label,'Médico nato');assert.deepEqual(r.naturalReroll.originalRoll.pool,r.pool);assert.notDeepEqual(r.naturalReroll.originalRoll.faces,r.faces)});

test('P111: talento Nato é limitado a uma vez por sessão',()=>{const g=game(['Programador nato']);g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,useNaturalReroll:true,rng:()=>0.2});assert.throws(()=>g.resolveSkillCheck({skillKey:'astronavegacion',difficulty:2,useNaturalReroll:true,rng:()=>0.2}),/já foi usado nesta sessão/i)});

test('P111: nova sessão limpa o uso Nato',()=>{const g=game(['Trapaceiro nato']);g.resolveSkillCheck({skillKey:'sigilo',difficulty:2,useNaturalReroll:true,rng:()=>0.2});g.closeSession();g.startSession({rng:()=>0.99});assert.doesNotThrow(()=>g.resolveSkillCheck({skillKey:'actividad_criminal',difficulty:2,useNaturalReroll:true,rng:()=>0.2}))});

test('P111: rerrolagem Nato é rejeitada quando a perícia não pertence ao talento adquirido',()=>{const g=game(['Médico nato']);assert.throws(()=>g.resolveSkillCheck({skillKey:'sigilo',difficulty:2,useNaturalReroll:true,rng:()=>0.2}),/Nenhum talento Nato/i)});
