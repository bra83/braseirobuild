import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const app=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('Cena expõe fluxo de sessão e teste sugerido do Mestre em PT-BR',()=>{
  for (const label of ['Iniciar sessão','Encerrar sessão','Teste sugerido pelo Mestre','Resolver este teste']) assert.ok(html.includes(label));
  assert.match(app,/SESSION_PHASE_LABELS_PT_BR/);
  assert.match(app,/resolveSuggestedMasterCheck/);
});

test('Personagem expõe progressão por PE com custo automático de perícia profissional',()=>{
  for (const label of ['Progressão por PE','Comprar próximo grau','perícia profissional']) assert.ok(html.includes(label) || app.includes(label));
  assert.match(app,/skillRankCostForCharacter/);
});
