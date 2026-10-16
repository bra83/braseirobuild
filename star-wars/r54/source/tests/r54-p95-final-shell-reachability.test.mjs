import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
const rootContract=fs.readFileSync(new URL('../gameBridgeClient.ts',import.meta.url),'utf8');
const shellContract=fs.readFileSync(new URL('../shell-r46-run87/gameBridgeClient.ts',import.meta.url),'utf8');

test('P95 R46 alcança social/oposto/competitivo/assistência sem fallback textual',()=>{
  for(const name of ['resolveSocialInfluenceCheck','resolveOpposedSkillCheck','resolveCompetitiveSkillCheck','resolveAssistedSkillCheck']){
    assert.match(runtime,new RegExp(`call\\('${name}'`));
    assert.match(bridge,new RegExp(`${name}\\(options`));
    assert.match(rootContract,new RegExp(`${name}\\?`));
    assert.match(shellContract,new RegExp(`${name}\\?`));
  }
  assert.match(runtime,/wanted=\['01','03','11'\]/);
});

test('P95 UI veicular oferece Damage Control e reparo testado, não criação manual de crítico',()=>{
  assert.match(runtime,/call\('vehicleDamageControl'/);
  assert.match(runtime,/call\('attemptRepairActiveVehicleCritical'/);
  assert.doesNotMatch(runtime,/call\('applyActiveVehicleCritical'/);
  assert.match(bridge,/attemptRepairActiveVehicleCritical\(index/);
  assert.doesNotMatch(bridge,/applyActiveVehicleCritical\(options/);
});
