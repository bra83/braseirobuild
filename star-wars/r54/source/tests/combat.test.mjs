import test from 'node:test';
import assert from 'node:assert/strict';
import { createCombatState, startCombat, nextPlayerTurn, spendAction, spendManeuver } from '../app/src/main/assets/vtt/js/rules/combat.mjs';

test('turno permite 1 ação e 1 manobra normal',()=>{
  let c=startCombat(createCombatState());
  c=spendAction(c,'Atacar');
  const m=spendManeuver(c,{label:'Mover'});
  assert.equal(m.combat.playerTurn.actionUsed,true);
  assert.equal(m.combat.playerTurn.maneuversUsed,1);
  assert.equal(m.strainCost,0);
});

test('segunda manobra exige Tensão ou troca da ação e nunca passa de 2',()=>{
  let c=startCombat(createCombatState());
  c=spendManeuver(c).combat;
  assert.throws(()=>spendManeuver(c),/segunda manobra/i);
  const extra=spendManeuver(c,{method:'strain'});
  assert.equal(extra.strainCost,2);
  assert.equal(extra.combat.playerTurn.maneuversUsed,2);
  assert.throws(()=>spendManeuver(extra.combat,{method:'strain'}),/mais de 2 manobras/i);
});

test('ação pode ser trocada pela segunda manobra e o turno seguinte reinicia recursos',()=>{
  let c=startCombat(createCombatState());
  c=spendManeuver(c).combat;
  c=spendManeuver(c,{method:'action'}).combat;
  assert.equal(c.playerTurn.actionUsed,true);
  const next=nextPlayerTurn(c);
  assert.equal(next.round,2);
  assert.equal(next.playerTurn.actionUsed,false);
  assert.equal(next.playerTurn.maneuversUsed,0);
});
