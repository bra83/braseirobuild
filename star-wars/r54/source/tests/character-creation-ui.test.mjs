import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('criador personalizado EotE aparece em PT-BR com etapas mecânicas essenciais',()=>{
  for(const text of ['Criar personagem personalizado','Espécie','Carreira','Especialização','Obligation inicial','Características','Perícias gratuitas','Motivação','Antecedentes','Criar e usar personagem']) assert.ok(html.includes(text),`faltou ${text}`);
  assert.ok(html.includes('+10 Obligation → +2.500 créditos'));
  assert.match(html,/Obligation solo é política local|política local editável/i);
  assert.match(html,/id="customMotivation"[^>]*required/);
  assert.match(html,/id="speciesVariantWrap"[^>]*hidden/);
  assert.match(html,/Fisiologia do Gand/);
});

test('interface envia os quatro benefícios de Obligation e usa o criador EotE do motor',()=>{
  assert.match(js,/OBLIGATION_TYPES_PT_BR/);
  assert.match(js,/eoteCreationPreview/);
  assert.match(js,/credits2500:\$\('obligationCredits2500'\)\.checked/);
  assert.match(js,/game\.createCustomCharacter\(creatorSelections\(\)\)/);
  assert.doesNotMatch(js,/AOR_SPECIES|AOR_CAREERS|DUTY_TYPES_PT_BR|creationPreview\(/);
});
