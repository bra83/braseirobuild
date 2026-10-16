import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
const bridge=fs.readFileSync('app/src/main/assets/vtt/js/core/uiBridge.mjs','utf8');
const controller=fs.readFileSync('app/src/main/assets/vtt/js/core/controller.mjs','utf8');

test('P92: shell final expõe painéis P0 nas rotas de combate, veículo, viagem, personagem, progressão e itens',()=>{
  for(const token of ["r==='02'","r==='03'","r==='04'","r==='06'","r==='07'","r==='08'"]) assert.ok(runtime.includes(token),token);
  for(const marker of ['COMBATE PESSOAL','COMBATE DE NAVE / VEÍCULO','ASTRONAVEGAÇÃO / HIPERESPAÇO','FORÇA','PROGRESSÃO / CONTRIBUIÇÃO','EQUIPAMENTO ATIVO']) assert.ok(runtime.includes(marker),marker);
});

test('P92: UI P0 chama apenas bridge mecânico, sem consultRules/RAG',()=>{
  const start=runtime.indexOf('R54 P92 — P0 MECHANICAL REACHABILITY');
  assert.ok(start>0);
  const p92=runtime.slice(start);
  assert.equal(/consultRules|rag\/|MASTER_RULE_QUERY/.test(p92),false);
  for(const method of ['resolveWeaponAttack','equipWeapon','equipArmor','resolveVehicleAttack','resolveHyperspaceJump','buyForcePower','resolveCampaignMilestone']) assert.ok(p92.includes(method),method);
});

test('P92: bridge final alcança as mutações P0 do GameController',()=>{
  for(const method of ['equipmentOwnedSummary','equipWeapon','equipArmor','combatRuntimeStatus','resolveWeaponAttack','vehicleRuntimeStatus','resolveVehicleAttack','astrogationPlan','resolveHyperspaceJump','groupSummary','resolveCampaignMilestone']) assert.ok(bridge.includes(`${method}(`),method);
  for(const method of ['equipmentOwnedSummary','equipWeapon','equipArmor','resolveWeaponAttack','resolveVehicleAttack','astrogationPlan','resolveHyperspaceJump','resolveContributionMilestone']) assert.ok(controller.includes(`${method}(`),method);
});
