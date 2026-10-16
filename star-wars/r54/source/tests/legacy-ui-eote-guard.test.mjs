import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('harness web legado permanece EOTE_CORE e não reativa criação/grupo AoR',()=>{
  assert.match(js,/eoteCharacterCreation\.mjs/);
  assert.match(html,/Obligation inicial/);
  assert.match(html,/Obligation do grupo/);
  assert.doesNotMatch(html,/Duty inicial|Dever e Faixa de Contribuição|Tipo de Duty|Caixa de Início|Personagem inicial verificado/);
  assert.doesNotMatch(js,/characterCreation\.mjs|AOR_SPECIES|AOR_CAREERS|DUTY_TYPES_PT_BR|customDuty|groupContribution|addGroupDuty\(|renderRebelGroup/);
});
