import assert from 'node:assert/strict';
import fs from 'node:fs';
const mgr=fs.readFileSync('app/src/main/assets/www/core/campaign-manager-service.js','utf8');
const ui=fs.readFileSync('app/src/main/assets/www/configuracoes.js','utf8');
for(const token of ['applyStarterContentToState','makeLivingWorldInitialState','STARTER_POSITION','starterPack']) assert.ok(mgr.includes(token),`manager missing ${token}`);
for(const token of ["fetch('data/wwn.starter-region.v1.json')",'starterPack','Campanha criada com região, NPCs e mundo vivo']) assert.ok(ui.includes(token),`config missing ${token}`);
assert.ok(!ui.includes("campaignManager.create({displayName:$('newCampaignName').value,masterSeed:$('newCampaignSeed').value,activate:true})"),'empty campaign path still present');
console.log('campaign-world-bootstrap-regression PASS');
