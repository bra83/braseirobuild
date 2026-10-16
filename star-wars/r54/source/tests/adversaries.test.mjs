import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createAdversary, aliveMinions, adversarySkillRank, applyAdversaryDamage,
  sufferAdversaryStrain, applyAdversaryCritical, adversaryHealthStatus,
  upgradeDifficultyForAdversaryTalent
} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

test('grupo de lacaios ganha um grau de perícia por membro além do primeiro',()=>{
  const group=createAdversary('imperial_soldier',{count:4});
  assert.equal(aliveMinions(group),4);
  assert.equal(adversarySkillRank(group,'armas_distancia_pesadas'),3);
  assert.equal(adversarySkillRank(group,'alerta'),0);
});

test('Ferimentos reduzem o grupo e sua perícia automaticamente',()=>{
  let group=createAdversary('imperial_soldier',{count:4});
  const hit=applyAdversaryDamage(group,{damage:9}); // proteção 3 => 6 Ferimentos
  group=hit.adversary;
  assert.equal(hit.suffered,6);
  assert.equal(aliveMinions(group),3);
  assert.equal(adversarySkillRank(group,'armas_distancia_pesadas'),2);
});

test('lacaios e rivais convertem Tensão em Ferimentos',()=>{
  const minion=sufferAdversaryStrain(createAdversary('imperial_soldier',{count:2}),2);
  const rival=sufferAdversaryStrain(createAdversary('imperial_officer'),2);
  assert.equal(minion.convertedToWounds,true);
  assert.equal(minion.adversary.wounds.current,2);
  assert.equal(rival.convertedToWounds,true);
  assert.equal(rival.adversary.wounds.current,2);
});

test('crítico em grupo de lacaios elimina exatamente um membro ainda ativo',()=>{
  let group=createAdversary('imperial_soldier',{count:3});
  const crit=applyAdversaryCritical(group);
  group=crit.adversary;
  assert.equal(crit.applied,true);
  assert.equal(aliveMinions(group),2);
});

test('oficial imperial é rival com Adversário 1 e limiar 15',()=>{
  const officer=createAdversary('imperial_officer');
  assert.equal(officer.kind,'rival');
  assert.equal(officer.wounds.threshold,15);
  assert.equal(upgradeDifficultyForAdversaryTalent(officer),1);
  assert.equal(adversaryHealthStatus(officer).incapacitated,false);
});
