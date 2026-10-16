import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
test('interface PT-BR inclui aba Base e grupo de Orla com Obligation',()=>{ for(const text of ['Base de Operações','Obligation do grupo','Recursos, Pertences e Contatos','Reserva de Destino']) assert.match(html,new RegExp(text)); assert.match(html,/data-view="baseView"/); assert.match(js,/game\.groupSummary\(\)/); assert.match(js,/game\.addGroupObligation/); assert.doesNotMatch(js,/setGroupContributionRank|addGroupDuty|removeGroupDuty/); });
test('interface expõe árvore completa de talentos com desbloqueio por conexão',()=>{ assert.match(html,/Talentos da especialização/); assert.match(js,/buyTalent/); assert.match(js,/Linha \$\{tier\}/); assert.match(html,/conexões oficiais/i); });
test('interface expõe catálogo auditado sem compra automática',()=>{ assert.match(html,/Catálogo auditado de armas e blindagens/); assert.match(html,/não desconta créditos nem adiciona item/i); assert.match(js,/proposeWeaponAcquisition/); assert.match(js,/Propor aquisição/); });
