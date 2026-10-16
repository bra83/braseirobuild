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
  assert.match(bundle,/"id":\s*"SW-AOR-CHAR-DUTY-002"/);
  assert.doesNotMatch(bundle,/"id":\s*"SW-AOR-(?!CHAR-DUTY-002|BASE-DUTY-CONTRIBUTION-001)/);
});

test('FINAL shell compartilhada mantém EotE padrão e só expõe origem AoR por escolha explícita',()=>{
  const shell=read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
  assert.match(shell,/profile:\s*'EOTE_CORE'/);
  assert.match(shell,/campaignMode = \['EOTE', 'AOR', 'MIXED'\]\.includes/);
  assert.match(shell,/catalog\?\.campaignMode === 'MIXED'/);
  assert.match(shell,/Edge of the Empire .* Obligation/);
  assert.match(shell,/Age of Rebellion .* Duty/);
  assert.match(shell,/Obriga.*o e Dever permanecem separados no grupo/);
});

test('FINAL: corpus mantém EotE dominante e permite somente o subconjunto AoR recuperado para Duty/Contribution',()=>{
  assert.ok(VERIFIED_RULE_CORPUS.length>0);
  const allowedAoR=new Set(['SW-AOR-CHAR-DUTY-002','SW-AOR-BASE-DUTY-CONTRIBUTION-001']);
  for(const row of VERIFIED_RULE_CORPUS){
    const allowed=row.id.startsWith('SW-EOTE-') || row.id.startsWith('SW-VTT-') || row.id.startsWith('VTT-') || allowedAoR.has(row.id);
    assert.ok(allowed,row.id);
    if(row.id.startsWith('SW-AOR-')) assert.ok(allowedAoR.has(row.id),`AoR não autorizado no corpus: ${row.id}`);
  }
  const aor=VERIFIED_RULE_CORPUS.filter(r=>r.id.startsWith('SW-AOR-'));
  assert.deepEqual(aor.map(r=>r.id).sort(),[...allowedAoR].sort());
  assert.ok(aor.every(r=>r.official===true));
  const runtime=VERIFIED_RULE_CORPUS.find(r=>r.id==='VTT-AOR-DUTY-RUNTIME-001');
  assert.equal(runtime?.official,false);
});
