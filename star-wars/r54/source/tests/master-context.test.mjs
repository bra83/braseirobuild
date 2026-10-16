import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
import { prepareLocalMasterTurn, consultRulesLocal } from '../app/src/main/assets/vtt/js/master/localMaster.mjs';
import { STARTER_PRESETS } from '../app/src/main/assets/vtt/js/rules/starterPresets.mjs';

test('contexto do Mestre inclui estado pertinente, eventos e regras recuperadas',()=>{
  const s=createCampaign('Contexto');
  s.player.character=STARTER_PRESETS.arkhan();
  s.scene.lastPlayerIntent='Eu miro e disparo no oficial';
  s.events.push({id:'x',type:'PLAYER_INTENT',at:'2026-09-05T00:00:00Z',payload:{text:s.scene.lastPlayerIntent}});
  const ctx=buildMasterContext(s,{intentText:s.scene.lastPlayerIntent,ruleQuery:'Alvo ardiloso silhueta veículo'});
  assert.equal(ctx.player.name,'Arkhan');
  assert.equal(ctx.campaign.name,'Contexto');
  assert.ok(ctx.rules.some(r=>r.id==='SW-EOTE-TALENT-VEHICLE-001'));
  assert.equal(ctx.retrieval.grounded,true);
  assert.equal(ctx.recentEvents.length,1);
});

test('preparação local não finge provedor de IA',()=>{
  const s=createCampaign('Mestre local');
  const prepared=prepareLocalMasterTurn(s,'Examino a porta antes de entrar');
  assert.equal(prepared.provider,'local-context-only');
  assert.equal(prepared.canNarrateFreely,false);
  assert.match(prepared.playerMessage,/Ação registrada/i);
});

test('consulta local devolve regra EotE auditada e falha fechado fora da autoridade disponível',()=>{
  const s=createCampaign('Consulta');
  const known=consultRulesLocal(s,'Obligation inicial quantidade personagens benefícios PE créditos');
  assert.equal(known.grounded,true);
  assert.equal(known.results[0].id,'SW-EOTE-OBLIGATION-001');
  assert.match(known.results[0].summary,/Obligation/i);
  const gap=consultRulesLocal(s,'como funciona proteção e perfuração');
  assert.equal(gap.grounded,false);
  assert.equal(gap.results.some(row=>String(row.id).startsWith('SW-AOR-')),false);
});
