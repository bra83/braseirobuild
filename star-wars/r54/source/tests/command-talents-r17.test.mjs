import test from 'node:test';
import assert from 'node:assert/strict';
import { talentSkillPoolModifiers, quickStrikeBoost } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

function character(...talents){ return {talents}; }

test('Comando adiciona 1 Impulso por grau somente a Liderança',()=>{
  const c=character('Comando','Comando');
  const leadership=talentSkillPoolModifiers(c,'liderazgo',{boost:1,setback:0});
  assert.equal(leadership.boost,3);
  assert.equal(leadership.talentBoost,2);
  assert.equal(leadership.details.find(d=>d.originalName==='COM')?.amount,2);
  const cool=talentSkillPoolModifiers(c,'frialdad',{boost:1,setback:0});
  assert.equal(cool.boost,1);
});

test('Ataque rápido adiciona Impulso igual ao grau contra alvo que ainda não agiu',()=>{
  const c=character('Ataque rápido','Ataque rápido');
  const pending=quickStrikeBoost(c,{targetActorId:'adv_1',actedActorIds:['adv_2']});
  assert.deepEqual({boost:pending.boost,applies:pending.applies},{boost:2,applies:true});
  const acted=quickStrikeBoost(c,{targetActorId:'adv_1',actedActorIds:['adv_1']});
  assert.deepEqual({boost:acted.boost,applies:acted.applies},{boost:0,applies:false});
});

test('Ataque rápido não se aplica sem alvo de iniciativa conhecido',()=>{
  const c=character('Ataque rápido');
  assert.equal(quickStrikeBoost(c,{targetActorId:null,actedActorIds:[]}).boost,0);
});
