import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { createEnvironmentRuntime,configureEnvironment,environmentSkillModifiers,applyEnvironmentTurnStart,applyNumericFall,applyForcedFall } from '../app/src/main/assets/vtt/js/rules/environment.mjs';

class Repo{constructor(s=createCampaign('P113')){this.state=structuredClone(s)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(){const c=createBlankCharacter('Explorador');c.characteristics={fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2};for(const k of Object.keys(c.skills))c.skills[k]=2;c.derived={soak:2,wounds:{threshold:12,current:0},strain:{threshold:12,current:0},defense:{ranged:0,melee:0}};return c}
function game(){const g=new GameController(new Repo());g.setCharacter(hero());return g}

test('P113: gravidade e ocultação modificam pools canônicos',()=>{let e=configureEnvironment(createEnvironmentRuntime(),{gravityMode:'heavy',gravityLevel:3,concealment:2});let m=environmentSkillModifiers(e,'atletismo','fortaleza');assert.equal(m.setback,3);m=environmentSkillModifiers(e,'armas_distancia_ligeras','agilidad');assert.equal(m.setback,2);m=environmentSkillModifiers(e,'sigilo','agilidad');assert.equal(m.boost,2);e=configureEnvironment(e,{gravityMode:'light',gravityLevel:2});m=environmentSkillModifiers(e,'coordinacion','agilidad');assert.equal(m.boost,2)});

test('P113: terreno difícil dobra manobras reais de faixa',()=>{const g=game();g.configureEnvironment({terrain:'difficult'});g.beginCombat();g.state.combat.adversaries=[createAdversary('imperial_soldier',{range:'Curto'})];g.repository.save(g.state);const a=g.changeCombatRange({targetAdversaryId:g.state.combat.adversaries[0].id,direction:'farther'});assert.equal(a.requiredManeuvers,2);assert.equal(a.movementComplete,false);const b=g.changeCombatRange({targetAdversaryId:g.state.combat.adversaries[0].id,direction:'farther',maneuverMethod:'strain'});assert.equal(b.movementComplete,true);assert.equal(b.target.range,'Médio')});

test('P113: terreno intransponível bloqueia até Atletismo/Coordenação bem-sucedido',()=>{const g=game();g.configureEnvironment({terrain:'impassable'});g.beginCombat();g.state.combat.adversaries=[createAdversary('imperial_soldier',{range:'Curto'})];g.repository.save(g.state);const id=g.state.combat.adversaries[0].id;assert.throws(()=>g.changeCombatRange({targetAdversaryId:id,direction:'farther'}),/intransponível/i);const pass=g.resolveImpassableTerrain({skillKey:'atletismo',difficulty:0,rng:()=>0.2});assert.equal(pass.success,true);assert.doesNotThrow(()=>g.changeCombatRange({targetAdversaryId:id,direction:'farther'}))});

test('P113: submersão respeita Brawn antes de iniciar sufocamento',()=>{let e=configureEnvironment(createEnvironmentRuntime(),{waterPresent:true,submerged:true});let c=hero();for(let i=0;i<3;i++){const r=applyEnvironmentTurnStart(e,c,{rng:()=>0.2});e=r.environment;c=r.character;assert.equal(r.suffocating,false)}const r=applyEnvironmentTurnStart(e,c,{rng:()=>0.2});assert.equal(r.suffocating,true);assert.equal(r.effects.some(x=>x.type==='suffocation'),true);assert.equal(r.character.derived.strain.current,3)});

test('P113: vácuo causa 3 Ferimentos por rodada e também entra em sufocamento após Brawn',()=>{let e=configureEnvironment(createEnvironmentRuntime(),{vacuum:true});let c=hero();for(let i=0;i<4;i++){const r=applyEnvironmentTurnStart(e,c,{rng:()=>0.2});e=r.environment;c=r.character}assert.equal(c.derived.wounds.current,12);assert.equal(c.derived.strain.current,3)});

test('P113: fogo/ácido/atmosfera corrosiva aplicam Ferimentos diretos por rating',()=>{const e=configureEnvironment(createEnvironmentRuntime(),{fire:2,acid:3,corrosive:1});const r=applyEnvironmentTurnStart(e,hero(),{rng:()=>0.2});assert.equal(r.character.derived.wounds.current,6);assert.deepEqual(r.effects.filter(x=>['fire','acid','corrosive'].includes(x.type)).map(x=>x.wounds),[2,3,1])});

test('P113: sufocamento gera crítico em turno subsequente quando já incapacitado por Tensão',()=>{const e=configureEnvironment(createEnvironmentRuntime(),{suffocating:true});let c=hero();c.derived.strain.current=13;const r=applyEnvironmentTurnStart(e,c,{rng:()=>0.2});assert.equal(r.effects[0].type,'suffocation');assert.ok((r.character.criticalInjuries??[]).length>=1)});

test('P113: queda curta usa soak e sucessos/vantagens para reduzir dano e Tensão',()=>{const c=hero();const r=applyNumericFall(c,{range:'Curto',netSuccess:2,netAdvantage:3,rng:()=>0.2});assert.equal(r.wounds,6);assert.equal(r.strain,7)});

test('P113: queda longa incapacita e aplica Ferimento Crítico com +50',()=>{const r=applyForcedFall(hero(),{range:'Longo',rng:()=>0.2});assert.equal(r.status.woundIncapacitated,true);assert.equal(r.critical.externalModifier,50);assert.equal(r.strain,30)});

test('P113: controller injeta ocultação em teste real e persiste ambiente',()=>{const g=game();g.configureEnvironment({concealment:2});const r=g.resolveSkillCheck({skillKey:'percepcion',difficulty:0,rng:()=>0.2});assert.equal(r.environmentModifiers.setback,2);assert.equal(r.pool.setback,2);assert.equal(g.state.environment.concealment,2)});
