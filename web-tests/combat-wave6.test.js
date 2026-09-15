import test from 'node:test';
import assert from 'node:assert/strict';
import { DeterministicRng } from '../app/src/main/assets/www/core/rng-core.js';
import { computeSaveTargets, orderInitiativeSides, resolveAttack, resolveSavingThrow, resolveSideInitiative } from '../app/src/main/assets/www/core/wwn-combat-core.js';

function fixedRng(results){let i=0;return{die(sides,stream='fixed'){const result=results[i++];if(!Number.isInteger(result)||result<1||result>sides)throw new Error(`bad fixed result ${result} for d${sides}`);return{algorithm:'fixed-test',stream,counterBefore:i-1,rawHex:'0',sides,result};}};}

const iria={level:1,attributes:{Strength:7,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9},skills:{Survive:1,Notice:1,Sneak:0,Exert:0}};

test('Wave6 canonical IRIA save targets are Physical15 Evasion15 Mental14 Luck15',()=>{
  assert.deepEqual(computeSaveTargets(iria),{Physical:15,Evasion:15,Mental:14,Luck:15});
});

test('saving throws honor natural 1 auto-fail and natural 20 auto-success',()=>{
  const fail=resolveSavingThrow({rng:fixedRng([1]),saveType:'PHYSICAL',target:2,modifier:99});
  assert.equal(fail.success,false); assert.equal(fail.naturalOverride,'AUTO_FAIL');
  const pass=resolveSavingThrow({rng:fixedRng([20]),saveType:'MENTAL',target:99,modifier:-99});
  assert.equal(pass.success,true); assert.equal(pass.naturalOverride,'AUTO_SUCCESS');
});

test('side initiative is d8 + Dexterity modifier and PC side wins tie',()=>{
  const pc=resolveSideInitiative({rng:fixedRng([4]),bestDexterityScore:12});
  const npc=resolveSideInitiative({rng:fixedRng([4]),dexterityModifier:0});
  assert.equal(pc.total,4); assert.equal(npc.total,4);
  const order=orderInitiativeSides([{sideId:'enemy',isPcSide:false,total:npc.total},{sideId:'pc',isPcSide:true,total:pc.total}]);
  assert.equal(order[0].sideId,'pc');
});

test('trained melee hit uses d20 + BAB + attribute + combat skill and rolls damage',()=>{
  const attacker={baseAttackBonus:1,attributes:{Strength:14,Dexterity:10,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},skills:{Stab:1}};
  const weapon={combatSkill:'Stab',attribute:'Strength',damage:'1d8',melee:true,shock:{damage:2,ac:15}};
  const target={meleeAC:13,rangedAC:13,shockImmune:false};
  const r=resolveAttack({rng:fixedRng([10,5]),attacker,weapon,target});
  assert.equal(r.hitTotal,13); assert.equal(r.hit,true); assert.equal(r.damage,6); // 5 + Str mod +1
});

test('untrained attack applies -2 and Shock can damage on a miss',()=>{
  const attacker={baseAttackBonus:0,attributes:{Strength:14,Dexterity:10,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},skills:{}};
  const weapon={combatSkill:'Stab',attribute:'Strength',damage:'1d8',melee:true,shock:{damage:2,ac:15}};
  const target={meleeAC:15,rangedAC:15,shockImmune:false};
  const r=resolveAttack({rng:fixedRng([10]),attacker,weapon,target});
  assert.equal(r.skillContribution,-2); assert.equal(r.hit,false); assert.equal(r.damageKind,'SHOCK'); assert.equal(r.damage,3);
});

test('hit damage can never be below applicable Shock',()=>{
  const attacker={baseAttackBonus:10,attributes:{Strength:10,Dexterity:10,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},skills:{Stab:1}};
  const weapon={combatSkill:'Stab',attribute:'Strength',damage:'1d4',melee:true,shock:{damage:4,ac:15}};
  const target={meleeAC:10,rangedAC:10,shockImmune:false};
  const r=resolveAttack({rng:fixedRng([2,1]),attacker,weapon,target});
  assert.equal(r.hit,true); assert.equal(r.damage,4);
});

test('deterministic initiative repeats from identical seed/state',()=>{
  const a=new DeterministicRng('combat-seed'); const b=new DeterministicRng('combat-seed');
  assert.deepEqual(resolveSideInitiative({rng:a,bestDexterityScore:12}),resolveSideInitiative({rng:b,bestDexterityScore:12}));
});
