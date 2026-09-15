import test from 'node:test';
import assert from 'node:assert/strict';
import { levelForXp, fullExpertBaseAttackBonus, fullExpertLevelBenefits, skillStepCost, resolveFullExpertHpIncrease } from '../app/src/main/assets/www/core/wwn-advancement-core.js';

function fixedRng(results){let i=0;return{die(sides,stream='fixed'){const result=results[i++];if(!Number.isInteger(result)||result<1||result>sides)throw new Error('bad fixed die');return{algorithm:'fixed-test',stream,counterBefore:i-1,rawHex:'0',sides,result};}};}

test('fast and slow XP tables reach exact canonical levels',()=>{
  assert.equal(levelForXp({xp:3,pace:'FAST'}).level,2);
  assert.equal(levelForXp({xp:5,pace:'FAST'}).level,2);
  assert.equal(levelForXp({xp:6,pace:'FAST'}).level,3);
  assert.equal(levelForXp({xp:6,pace:'SLOW'}).level,2);
  assert.equal(levelForXp({xp:139,pace:'SLOW'}).level,10);
});

test('Full Expert attack bonus follows class table and Quick Learner grants four skill points',()=>{
  assert.deepEqual([1,2,3,4,5,6,7,8,9,10].map(fullExpertBaseAttackBonus),[0,1,1,2,2,3,3,4,4,5]);
  const l2=fullExpertLevelBenefits(2); assert.equal(l2.skillPointsGained,4);assert.equal(l2.focusPickRequired,true);
  const l3=fullExpertLevelBenefits(3); assert.equal(l3.skillPointsGained,4);assert.equal(l3.focusPickRequired,false);
});

test('skill step costs and minimum levels match advancement table',()=>{
  assert.deepEqual([0,1,2,3,4].map(n=>{const x=skillStepCost(n);return[x.skillPointCost,x.minimumCharacterLevel];}),[[1,1],[2,1],[3,3],[4,6],[5,9]]);
});

test('level-up HP rerolls all hit dice and uses +1 fallback when candidate is not better',()=>{
  const better=resolveFullExpertHpIncrease({rng:fixedRng([6,6]),newLevel:2,constitutionScore:11,currentMaxHp:5});
  assert.equal(better.candidateTotal,12);assert.equal(better.newMaxHp,12);assert.equal(better.fallbackPlusOne,false);
  const fallback=resolveFullExpertHpIncrease({rng:fixedRng([1,1]),newLevel:2,constitutionScore:11,currentMaxHp:5});
  assert.equal(fallback.candidateTotal,2);assert.equal(fallback.newMaxHp,6);assert.equal(fallback.fallbackPlusOne,true);
});
