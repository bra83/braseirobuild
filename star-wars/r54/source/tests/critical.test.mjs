import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { addStarterCritical, criticalInjuryTable, criticalCheckModifiers, expirePlayerTurnCriticalEffects, expireEncounterCriticalEffects, isDeadFromCritical } from '../app/src/main/assets/vtt/js/rules/critical.mjs';

test('tabela Core de Ferimentos Críticos cobre 01-05 até 151+ em 29 faixas',()=>{
  const table=criticalInjuryTable();
  assert.equal(table.length,29);
  assert.equal(table[0].min,1); assert.equal(table[0].max,5); assert.equal(table[0].code,'minor_nick');
  assert.equal(table.at(-1).min,151); assert.equal(table.at(-1).code,'dead');
});

test('cada Ferimento Crítico anterior adiciona +10 à nova rolagem',()=>{
  let c=STARTER_PRESETS.arkhan();
  let r=addStarterCritical(c,'primeiro',{roll:1});
  assert.equal(r.total,1); assert.equal(r.injury.code,'minor_nick');
  r=addStarterCritical(r.character,'segundo',{roll:1});
  assert.equal(r.total,11); assert.equal(r.injury.existingModifier,10); assert.equal(r.injury.code,'sudden_jolt');
});

test('Off-Balance afeta um único teste e Compromised dura até o fim do encontro',()=>{
  let c=STARTER_PRESETS.arkhan();
  c=addStarterCritical(c,'off',{roll:21}).character;
  let m=criticalCheckModifiers(c,{skillKey:'frialdad',characteristicKey:'presencia',consume:true});
  assert.equal(m.setback,1);
  m=criticalCheckModifiers(m.character,{skillKey:'frialdad',characteristicKey:'presencia',consume:true});
  assert.equal(m.setback,0);
  c=addStarterCritical(m.character,'comp',{roll:76}).character; // +10 existente => 86 Compromised
  m=criticalCheckModifiers(c,{skillKey:'frialdad',characteristicKey:'presencia'});
  assert.equal(m.difficultyIncrease,1);
  c=expireEncounterCriticalEffects(c);
  assert.equal(criticalCheckModifiers(c,{skillKey:'frialdad',characteristicKey:'presencia'}).difficultyIncrease,0);
});

test('Stunned expira ao final do próximo turno e bloqueia apenas ações via modifier',()=>{
  let c=STARTER_PRESETS.arkhan();
  c=addStarterCritical(c,'stun',{roll:31}).character;
  assert.equal(criticalCheckModifiers(c).cannotAct,true);
  c=expirePlayerTurnCriticalEffects(c);
  assert.equal(criticalCheckModifiers(c).cannotAct,false);
});

test('Horrific Injury reduz característica apenas enquanto o crítico existir; Gruesome reduz permanentemente',()=>{
  let c=STARTER_PRESETS.arkhan();
  c=addStarterCritical(c,'prévio',{roll:1}).character;
  c=addStarterCritical(c,'horrific',{roll:96,rng:()=>0}).character; // +10 =>106
  assert.equal(c.criticalInjuries[1].characteristicAffected,'fortaleza');
  assert.equal(criticalCheckModifiers(c,{characteristicKey:'fortaleza'}).characteristicPenalty,1);
  const before=c.characteristics.fortaleza;
  c=addStarterCritical(c,'gruesome',{roll:100,modifier:6,rng:()=>0}).character; // +20 +6 =>126
  assert.equal(c.characteristics.fortaleza,Math.max(1,before-1));
});

test('resultado 151+ registra Dead',()=>{
  let c=STARTER_PRESETS.arkhan();
  const r=addStarterCritical(c,'fatal',{roll:100,modifier:51});
  assert.equal(r.injury.code,'dead'); assert.equal(isDeadFromCritical(r.character),true);
});
