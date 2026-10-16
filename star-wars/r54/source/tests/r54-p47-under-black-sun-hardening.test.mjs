import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import {
  startUnderBlackSun, advanceUnderBlackSun, underBlackSunStatus,
  currentUnderBlackSunChecks, currentUnderBlackSunMasterRequest,
  adjudicateUnderBlackSunCheck
} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
const step=(w,id)=>advanceUnderBlackSun(w,id).world;
function toEndgame(){let w=startUnderBlackSun(createWorld()).world;for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-return','go-zelcomm','zelcomm-clean','move-endgame','plan-direct'])w=step(w,id);return w}

test('P47: ações sugeridas ao Mestre respeitam gating e não vazam opções futuras',()=>{
  let w=startUnderBlackSun(createWorld()).world;
  w=step(w,'escape-clean'); w=step(w,'begin-investigation');
  let req=currentUnderBlackSunMasterRequest(w);
  assert.equal(req.suggestedActions.some(x=>/desfecho/i.test(x)),false);
  w=step(w,'go-spyder'); w=step(w,'spyder-return');
  req=currentUnderBlackSunMasterRequest(w);
  assert.equal(req.suggestedActions.some(x=>/desfecho/i.test(x)),false);
  w=step(w,'go-zelcomm'); w=step(w,'zelcomm-clean');
  req=currentUnderBlackSunMasterRequest(w);
  assert.equal(req.suggestedActions.some(x=>/desfecho/i.test(x)),true);
});

test('P47: rota escritórios só oferece testes coerentes e preserva índices canônicos',()=>{
  let w=toEndgame(); w=step(w,'through-offices');
  const checks=currentUnderBlackSunChecks(w);
  assert.deepEqual(checks.map(x=>x.skill),['Computadores','Percepção']);
  assert.deepEqual(checks.map(x=>x.index),[0,1]);
  assert.deepEqual(underBlackSunStatus(w).node.checks.map(x=>x.skill),['Computadores','Percepção']);
  const r=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:1}});
  assert.equal(r.adjudication.skillKey,'computadoras');
});

test('P47: rota refinaria remove teste de Computadores e mantém Frieza no índice original',()=>{
  let w=toEndgame(); w=step(w,'through-refinery');
  const checks=currentUnderBlackSunChecks(w);
  assert.deepEqual(checks.map(x=>x.skill),['Percepção','Frieza']);
  assert.deepEqual(checks.map(x=>x.index),[1,2]);
  assert.deepEqual(underBlackSunStatus(w).node.checks.map(x=>x.skill),['Percepção','Frieza']);
  const r=adjudicateUnderBlackSunCheck(w,{checkIndex:1,result:{success:true,netSuccess:1,netAdvantage:0}});
  assert.equal(r.adjudication.skillKey,'percepcion');
});
