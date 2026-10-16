import test from 'node:test';
import assert from 'node:assert/strict';
import { createAdHocRival, ALLY_NPC_CONTROL_MODES } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

test('fallback de PNPC usa exatamente Características 2, Ferimentos 8 e grau 1 nas perícias escolhidas',()=>{
  const npc=createAdHocRival({name:'Técnico local',trainedSkills:['computadoras','mecanica']});
  assert.deepEqual(Object.values(npc.characteristics),[2,2,2,2,2,2]);
  assert.equal(npc.wounds.threshold,8);
  assert.deepEqual(npc.skills,{computadoras:1,mecanica:1});
  assert.equal(npc.kind,'rival');
});

test('fallback rejeita perícia inventada',()=>{
  assert.throws(()=>createAdHocRival({trainedSkills:['hackear_a_forca']}),/não mapeada/i);
});

test('modos de controle de aliados preservam as três opções documentadas',()=>{
  assert.deepEqual(Object.keys(ALLY_NPC_CONTROL_MODES),['narrative','abstract_combat','delegated_combat']);
});
