import test from 'node:test';
import assert from 'node:assert/strict';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { skillRankCost, spendXpOnSkill, isCareerSkill, skillRankCostForCharacter, spendXpOnSkillForCharacter, grantExperience } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';

test('perícia profissional custa novo grau multiplicado por 5',()=>{
  assert.equal(skillRankCost(0,true),5);
  assert.equal(skillRankCost(1,true),10);
  assert.equal(skillRankCost(4,true),25);
});

test('perícia não profissional acrescenta 5 PE por grau adquirido',()=>{
  assert.equal(skillRankCost(0,false),10);
  assert.equal(skillRankCost(1,false),15);
  assert.equal(skillRankCost(4,false),30);
});

test('progressão é sequencial, respeita grau máximo 5 e desconta PE uma vez',()=>{
  const c=createBlankCharacter('Teste'); c.xp.available=25;
  const r=spendXpOnSkill(c,'pilotar',{isCareer:true});
  assert.equal(r.cost,5); assert.equal(r.character.skills.pilotar,1); assert.equal(r.character.xp.available,20);
  assert.equal(c.skills.pilotar,0); assert.equal(c.xp.available,25);
  c.skills.pilotar=5; assert.throws(()=>spendXpOnSkill(c,'pilotar',{isCareer:true}),/grau atual/);
});


test('personagem inicial usa automaticamente suas perícias profissionais auditadas',()=>{
  const zal=STARTER_PRESETS.zal();
  zal.xp.available=20;
  assert.equal(isCareerSkill(zal,'pilotar'),true);
  assert.equal(isCareerSkill(zal,'engano'),false);
  assert.equal(skillRankCostForCharacter(zal,'pilotar'),15);
  assert.equal(skillRankCostForCharacter(zal,'engano'),15);
  const trained=spendXpOnSkillForCharacter(zal,'pilotar');
  assert.equal(trained.newRank,3);
  assert.equal(trained.cost,15);
});

test('concessão de PE aumenta total e saldo sem alterar a ficha original',()=>{
  const c=createBlankCharacter('PE');
  const result=grantExperience(c,10,{reason:'Fim da aventura'});
  assert.equal(result.character.xp.total,10);
  assert.equal(result.character.xp.available,10);
  assert.equal(c.xp.total,0);
});
