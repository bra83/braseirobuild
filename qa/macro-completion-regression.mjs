import assert from 'node:assert/strict';
import fs from 'node:fs';
const html=fs.readFileSync('app/src/main/assets/www/gestao.html','utf8');
const js=fs.readFileSync('app/src/main/assets/www/gestao.js','utf8');
for(const id of ['advanceLevel','learnFocus','learnSpell','learnArt','focusPick','spellPick','artPick']) assert.ok(html.includes(`id="${id}"`),id);
for(const svc of ['AdvancementService','FocusService','SpellLearningService','ArtLearningService']) assert.ok(js.includes(svc),svc);
for(const call of ['advanceLevel({campaignId','chooseAdvancementFocus({campaignId','learnAdvancementSpell({campaignId','learnPendingArt({campaignId']) assert.ok(js.includes(call),call);
console.log('MACRO_COMPLETION_REGRESSION PASS');
