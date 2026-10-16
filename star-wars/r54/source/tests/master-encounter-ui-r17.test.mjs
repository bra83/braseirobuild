import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
test('interface PT-BR mostra encontro preparado sem esconder escolha de Reação rápida',()=>{
  assert.match(html,/Encontro preparado pelo Mestre/);
  assert.match(html,/Entrar no encontro/);
  assert.match(ui,/Você ainda escolhe se usa Reação rápida antes da iniciativa/);
  assert.match(ui,/beginPendingMasterEncounter/);
  assert.match(ui,/Ataque rápido \+\$\{attack\.quickStrikeBoost\} Impulso/);
});
