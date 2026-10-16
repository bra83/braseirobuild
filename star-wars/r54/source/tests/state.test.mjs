import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { applyEvent } from '../app/src/main/assets/vtt/js/core/events.mjs';
import { exportStateJson, importStateJson } from '../app/src/main/assets/vtt/js/core/storage.mjs';

test('evento com mesmo ID só é aplicado uma vez', () => {
  const s = createCampaign('Teste');
  const e = { id:'same', type:'PLAYER_INTENT', at:'2026-09-05T00:00:00Z', payload:{text:'Avançar'} };
  const once = applyEvent(s,e); const twice = applyEvent(once,e);
  assert.equal(twice.events.length,1);
});

test('export/import JSON preserva estado válido', () => {
  const s = createCampaign('Teste JSON');
  const restored = importStateJson(exportStateJson(s));
  assert.equal(restored.campaignId, s.campaignId);
  assert.equal(validateState(restored), true);
});

test('fechar/reabrir um repositório de arquivo preserva estado', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(),'braseiro-sw-'));
  const file = path.join(dir,'state.json');
  const save = s => fs.writeFileSync(file, exportStateJson(s), 'utf8');
  const load = () => importStateJson(fs.readFileSync(file,'utf8'));
  const s = createCampaign('Persistência'); save(s);
  const reopened = load();
  assert.equal(reopened.campaignId, s.campaignId);
  assert.equal(reopened.name, 'Persistência');
});

test('estado v1 é migrado sem perder campanha e recebe combate v10, veículos e sessão', () => {
  const s=createCampaign('Migração');
  const legacy=structuredClone(s); legacy.schemaVersion=1; delete legacy.combat;
  const restored=importStateJson(JSON.stringify(legacy));
  assert.equal(restored.schemaVersion,14);
  assert.equal(restored.name,'Migração');
  assert.equal(restored.combat.active,false);
  assert.equal(restored.session.status,'nao_iniciada');
});

test('estado v2 migra para v10 preservando combate e adicionando encontro', () => {
  const s=createCampaign('Migração v2');
  const legacy=structuredClone(s);
  legacy.schemaVersion=2;
  legacy.combat={active:true,round:3,turn:2,actionUsed:true,maneuversUsed:1,log:[{type:'LEGADO'}]};
  const restored=importStateJson(JSON.stringify(legacy));
  assert.equal(restored.schemaVersion,14);
  assert.equal(restored.name,'Migração v2');
  assert.equal(restored.combat.active,true);
  assert.equal(restored.combat.round,3);
  assert.deepEqual(restored.combat.adversaries,[]);
  assert.equal(restored.combat.encounter,null);
});


test('estado v3 migra para v10 adicionando contexto do Mestre sem perder encontro',()=>{
  const s=createCampaign('Migração v3');
  const legacy=structuredClone(s); legacy.schemaVersion=3; delete legacy.master;
  const restored=importStateJson(JSON.stringify(legacy));
  assert.equal(restored.schemaVersion,14);
  assert.deepEqual(restored.master.lastRuleResults,[]);
  assert.equal(restored.master.corpusVersion,1);
});
