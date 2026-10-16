import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';
import { medicineDifficulty, resolveMedicineCheck, recoverAfterEncounter, restFullNight, resolveCriticalTreatment, recoverInBactaTank, recoverInOilBath } from '../app/src/main/assets/vtt/js/rules/recovery.mjs';
import { addStarterCritical } from '../app/src/main/assets/vtt/js/rules/critical.mjs';

test('Medicina escala pela condição do paciente, falta de suprimentos e autotratamento',()=>{
  const c=STARTER_PRESETS.tendaar(); c.derived.wounds.current=3; assert.equal(medicineDifficulty(c),1);
  c.derived.wounds.current=8; assert.equal(medicineDifficulty(c),2);
  c.derived.wounds.current=13; assert.equal(medicineDifficulty(c),3);
  assert.equal(medicineDifficulty(c,{medicalSupplies:false}),4);
  assert.equal(medicineDifficulty(c,{selfTreatment:true}),5);
});

test('Medicina usa sucesso para Ferimentos e Vantagem para Tensão',()=>{
  const healer=STARTER_PRESETS.tendaar(); const patient=STARTER_PRESETS.arkhan(); patient.derived.wounds.current=4; patient.derived.strain.current=3;
  const seq=[0.2,0.2,0.2,0.2,0]; let i=0; const r=resolveMedicineCheck(healer,patient,{rng:()=>seq[i++] ?? 0});
  assert.equal(r.check.success,true); assert.ok(r.healed>0); assert.ok(r.character.derived.wounds.current<4);
});

test('fim de encontro exige teste Simple de Frialdade ou Disciplina e Rapid Recovery só soma em sucesso',()=>{
  const c=STARTER_PRESETS.arkhan(); c.derived.strain.current=8; c.talents=['Recuperação rápida','Recuperação rápida'];
  const r=recoverAfterEncounter(c,{skillKey:'frialdad',rng:()=>0.2});
  assert.ok(r.basis>0); assert.equal(r.talentBonus,2); assert.equal(r.recovered,r.basis+2);
  const fail=recoverAfterEncounter(c,{skillKey:'disciplina',rng:()=>0});
  assert.equal(fail.basis,0); assert.equal(fail.talentBonus,0);
});

test('noite completa recupera 1 Ferimento, toda Tensão e restaura eficácia diária',()=>{
  const c=STARTER_PRESETS.cael(); c.derived.wounds.current=4; c.derived.strain.current=7; c.stimpackUsesToday=5; c.repairPatchUsesToday=4;
  const r=restFullNight(c); assert.equal(r.woundsRecovered,1); assert.equal(r.strainRecovered,7); assert.equal(r.character.stimpackUsesToday,0); assert.equal(r.character.repairPatchUsesToday,0);
});

test('tratamento de Ferimento Crítico usa a dificuldade de severidade e remove em sucesso',()=>{
  const healer=STARTER_PRESETS.tendaar(); let patient=STARTER_PRESETS.arkhan(); patient=addStarterCritical(patient,'crit',{roll:41}).character;
  const id=patient.criticalInjuries[0].id; const seq=[0.2,0.2,0.2,0.2,0]; let i=0;
  const r=resolveCriticalTreatment(healer,patient,{injuryId:id,rng:()=>seq[i++] ?? 0});
  assert.equal(r.difficulty,2); assert.equal(r.healedCritical,true); assert.equal(r.character.criticalInjuries.length,0);
});

test('bacta cura orgânicos e banho de óleo cura droides sem apagar críticos',()=>{
  let organic=STARTER_PRESETS.arkhan(); organic.derived.wounds.current=6; organic=addStarterCritical(organic,'crit',{roll:1}).character;
  const bacta=recoverInBactaTank(organic,4); assert.equal(bacta.woundsRecovered,2); assert.equal(bacta.character.criticalInjuries.length,1);
  let droid=STARTER_PRESETS.arkhan(); droid.creation={...(droid.creation??{}),speciesKey:'droid'}; droid.species='Droide'; droid.derived.wounds.current=6;
  assert.throws(()=>recoverInBactaTank(droid,4),/Droides não se recuperam/);
  const oil=recoverInOilBath(droid,3); assert.equal(oil.woundsRecovered,3);
});
