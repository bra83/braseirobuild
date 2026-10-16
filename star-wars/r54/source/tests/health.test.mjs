import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { applyDamage, getHealthStatus, sufferStrain, useStimpack } from '../app/src/main/assets/vtt/js/rules/health.mjs';

test('Proteção reduz dano e Perfuração reduz a Proteção efetiva',()=>{
  const c=STARTER_PRESETS.cael();
  const r=applyDamage(c,{damage:10,pierce:2});
  assert.equal(r.effectiveSoak,3); assert.equal(r.suffered,7); assert.equal(r.character.derived.wounds.current,7);
});

test('ultrapassar o limiar de Ferimentos causa inconsciência e registra crítico',()=>{
  let c=STARTER_PRESETS.arkhan(); c.derived.wounds.current=10;
  const r=applyDamage(c,{damage:5});
  assert.equal(r.status.unconscious,true); assert.equal(r.criticalAdded,true); assert.equal(r.character.criticalInjuries.length,1);
});

test('ultrapassar o limiar de Tensão causa inconsciência',()=>{
  let c=STARTER_PRESETS['jin-rio'](); c.derived.strain.current=11;
  const r=sufferStrain(c,2); assert.equal(getHealthStatus(r.character).strainIncapacitated,true);
});

test('primeiro stimpack do dia cura 5 Ferimentos e é consumido',()=>{
  let c=STARTER_PRESETS.arkhan(); c.derived.wounds.current=8;
  const item=c.gear.find(g=>/injetor/i.test(g.name)); item.quantity=6;
  const r=useStimpack(c);
  assert.equal(r.healingValue,5); assert.equal(r.healed,5); assert.equal(r.character.derived.wounds.current,3); assert.equal(r.remaining,5); assert.equal(r.usesToday,1);
});

test('stimpacks seguintes curam 4, 3, 2, 1 e o sexto não cura no mesmo dia',()=>{
  let c=STARTER_PRESETS.arkhan(); c.derived.wounds.current=20;
  const item=c.gear.find(g=>/injetor/i.test(g.name)); item.quantity=8;
  const values=[];
  for(let i=0;i<6;i++){ const r=useStimpack(c); values.push(r.healingValue); c=r.character; c.derived.wounds.current=20; }
  assert.deepEqual(values,[5,4,3,2,1,0]);
});
