import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { VERIFIED_RULE_CORPUS } from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';

const read=(rel)=>fs.readFileSync(new URL(`../${rel}`,import.meta.url),'utf8');

test('FINAL Star Wars: motor compartilhado mantém EotE como default e só ativa AoR/Misto por escolha explícita',()=>{
  const bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
  const state=read('app/src/main/assets/vtt/js/core/state.mjs');
  assert.match(bundle,/characterCreation_mjs_1/);
  assert.match(bundle,/setCampaignMode/);
  assert.match(bundle,/initializeLivingSandbox/);
  assert.match(state,/campaignMode:\s*'EOTE'/);
  assert.match(state,/\['EOTE',\s*'AOR',\s*'MIXED'\]/);
  assert.match(bundle,/eote_test_fixture/);
  assert.doesNotMatch(bundle,/starter_pregen/);
  assert.doesNotMatch(bundle,/La Era de la Rebelión - Caja de Inicio - (?:Zal|Vendri|Tendaar|Jin-Rio|Cael|Arkhan)/);
  assert.doesNotMatch(bundle,/"id":\s*"SW-AOR-/);
});

test('FINAL EotE: shell Android compilada não contém criação Duty/AoR nem Contribution',()=>{
  const shell=read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
  assert.match(shell,/EOTE_CORE/);
  assert.match(shell,/Obligation/);
  assert.doesNotMatch(shell,/AOR_CORE|DUTY_TYPES|startingDuty|dutyReductions|Contribution Rank/);
});

test('FINAL EotE: corpus verificado só contém mecânicas EotE e políticas VTT explícitas',()=>{
  assert.ok(VERIFIED_RULE_CORPUS.length>0);
  for(const row of VERIFIED_RULE_CORPUS){
    assert.ok(row.id.startsWith('SW-EOTE-') || row.id.startsWith('SW-VTT-') || row.id.startsWith('VTT-'), row.id);
  }
  assert.equal(VERIFIED_RULE_CORPUS.some(r=>/Age of Rebellion|La Era de la Rebelión/i.test(`${r.source} ${r.edition}`)),false);
});
