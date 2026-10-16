import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_GENERAL_GEAR_REFERENCE, CORE_WEAPON_CATALOG, CORE_ARMOR_REFERENCE } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';
import { opticalSetbackRelief } from '../app/src/main/assets/vtt/js/rules/generalGearEffects.mjs';
import { STARTER_VEHICLE_PROFILES } from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { ADVERSARY_PROFILES, AD_HOC_NPC_SOURCE } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { VERIFIED_RULE_CORPUS } from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';

const assertCrossLine=(source,label)=>{
  assert.equal(source?.authority,'CROSS_LINE_REFERENCE',label);
  assert.equal(source?.rulesProfile,'AOR_REFERENCE_ONLY',label);
  assert.equal(source?.activeRulesAuthority,false,label);
};

test('conteúdo AoR reutilizado é marcado como referência cruzada, nunca autoridade EotE',()=>{
  assertCrossLine(STARTER_GENERAL_GEAR_REFERENCE[0].source,'general gear');
  assertCrossLine(CORE_WEAPON_CATALOG[0].source,'weapon');
  assertCrossLine(CORE_ARMOR_REFERENCE[0].source,'armor');
  assertCrossLine(opticalSetbackRelief('electrobinoculars',{setback:1,longRangeSetback:1}).source,'gear effect');
  assertCrossLine(STARTER_VEHICLE_PROFILES.xwing_t65b.source,'vehicle');
  assertCrossLine(ADVERSARY_PROFILES.stormtrooper.source,'adversary');
  assertCrossLine(AD_HOC_NPC_SOURCE,'ad-hoc adversary');
});

test('referências cruzadas AoR continuam bloqueadas; somente Duty/Contribution auditados entram como autoridade explícita',()=>{
  const allowed=new Set(['SW-AOR-CHAR-DUTY-002','SW-AOR-BASE-DUTY-CONTRIBUTION-001']);
  const aor=VERIFIED_RULE_CORPUS.filter(row=>String(row.id).startsWith('SW-AOR-'));
  assert.deepEqual(aor.map(row=>row.id).sort(),[...allowed].sort());
  assert.ok(aor.every(row=>row.official===true));
  for(const row of VERIFIED_RULE_CORPUS){
    if(/Age of Rebellion|La Era de la Rebelión/i.test(`${row.source??''} ${row.edition??''}`) && String(row.id).startsWith('SW-AOR-')) assert.ok(allowed.has(row.id),row.id);
  }
});
