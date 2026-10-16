import test from 'node:test';
import assert from 'node:assert/strict';
import { startUnderBlackSun, advanceUnderBlackSun, adjudicateUnderBlackSunCheck } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

function started(){ return startUnderBlackSun({}).world; }
function advance(world,id){ return advanceUnderBlackSun(world,id).world; }

test('Triunfo na perseguição cria vantagem persistente sem spoiler interno',()=>{
  const world=started();
  const r=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:1,triumph:1,despair:0}});
  assert.equal(r.world.publishedAdventure.flags.escapeRouteSecured,true);
  assert.ok(r.adjudication.narrativeEffects.some(x=>x.kind==='scene'));
  assert.equal(r.progress.flags.escapeRouteSecured,true);
});

test('Desespero na perseguição persiste dano e custo físico',()=>{
  const world=started();
  const r=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-1,triumph:0,despair:1}});
  assert.equal(r.world.publishedAdventure.flags.speederDamaged,true);
  assert.equal(r.adjudication.playerStrainDelta,1);
  assert.ok(r.adjudication.narrativeEffects.some(x=>x.kind==='despair'));
});

test('Triunfo social no Umbra preserva discrição',()=>{
  let world=started();world=advance(world,'escape-clean');world=advance(world,'begin-investigation');world=advance(world,'go-umbra');
  const r=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:2,triumph:1,despair:0}});
  assert.equal(r.world.publishedAdventure.flags.umbraQuiet,true);
  assert.ok(r.adjudication.narrativeEffects.some(x=>x.kind==='social'));
});

test('Desespero no Spyder cria Obligation persistente sem encerrar investigação',()=>{
  let world=started();world=advance(world,'escape-clean');world=advance(world,'begin-investigation');world=advance(world,'go-spyder');
  const r=adjudicateUnderBlackSunCheck(world,{checkIndex:3,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
  assert.equal(r.world.publishedAdventure.flags.spyderDebt,true);
  assert.equal(r.world.publishedAdventure.flags.obligationPendingDelta,5);
  assert.equal(r.world.publishedAdventure.currentNodeId,'spyder');
});
