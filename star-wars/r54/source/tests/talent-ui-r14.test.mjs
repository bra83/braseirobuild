import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const app=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('interface PT-BR expõe Reação rápida antes da iniciativa',()=>{
  assert.match(html,/id="quickReactionStrain"/);
  assert.match(html,/Reação rápida antes da iniciativa/);
  assert.match(app,/playerQuickReactionStrain/);
  assert.match(app,/Reação rápida:/);
});

test('interface de Nave expõe Aceleração máxima como ação e versão aprimorada como manobra',()=>{
  assert.match(html,/id="fullThrottleAction"[^>]*>Aceleração máxima · ação</);
  assert.match(html,/id="fullThrottleManeuver"[^>]*>Aceleração máxima aprimorada · manobra</);
  assert.match(app,/useFullThrottle\(\{asManeuver:false\}/);
  assert.match(app,/useFullThrottle\(\{asManeuver:true/);
});
