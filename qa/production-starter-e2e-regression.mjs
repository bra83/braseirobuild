import assert from 'node:assert/strict';
import fs from 'node:fs';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {PlayerMechanicsFacade} from '../app/src/main/assets/www/core/player-mechanics-facade.js';
import {AdvancementService} from '../app/src/main/assets/www/core/advancement-service.js';

const character=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-character.v1.json',import.meta.url),'utf8'));
const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const persistence=new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character,pack,campaignId:'production-starter-e2e'});
assert.ok(String(boot.status).startsWith('OK'));
const facade=new PlayerMechanicsFacade({persistence});
let summary=facade.summary({campaignId:boot.campaignId});
assert.equal(summary.status,'OK');assert.equal(summary.character.classId,'EXPERT');assert.equal(summary.character.name,'IRIA');
const skill=facade.skillCheck({campaignId:boot.campaignId,skill:'Survive',attribute:'Wisdom',difficulty:8});assert.equal(skill.status,'OK');
const foe=facade.encounterOptions()[0];assert.ok(foe?.profileId);const added=facade.addEncounterFoes({campaignId:boot.campaignId,profileId:foe.profileId,count:1});assert.equal(added.status,'OK');
const combat=facade.startCombat({campaignId:boot.campaignId});assert.equal(combat.status,'OK');assert.equal(combat.summary.combat.active,true);
// End the synthetic combat before testing level advancement.
let loaded=persistence.loadCampaign(boot.campaignId);let state=JSON.parse(loaded.stateJson);state.encounterState={...(state.encounterState??{}),combat:null};state.characterState.xp=100000;state.charactersState[state.characterState.characterId]=structuredClone(state.characterState);state.revision=loaded.revision+1;let receipt=persistence.commit({campaignId:boot.campaignId,expectedRevision:loaded.revision,eventId:'production-starter-e2e-setup',eventKind:'TEST_SETUP',eventPayloadJson:'{}',newStateJson:JSON.stringify(state),rngCountersJson:JSON.stringify(state.rngState?.counters??{})});assert.equal(receipt.status,'OK');
const advanced=new AdvancementService({persistence}).advanceLevel({campaignId:boot.campaignId,hitDieRolls:[4,4]});assert.equal(advanced.status,'OK');assert.equal(advanced.character.level,2);assert.equal(advanced.character.classId,'EXPERT');
summary=facade.summary({campaignId:boot.campaignId});assert.equal(summary.status,'OK');assert.equal(summary.character.level,2);
console.log('PRODUCTION_STARTER_E2E PASS bootstrap + skill + encounter + combat + advancement');
