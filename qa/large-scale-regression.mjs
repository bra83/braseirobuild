import fs from 'node:fs';import assert from 'node:assert/strict';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {validateCampaignState,repairCampaignState} from '../app/src/main/assets/www/core/campaign-integrity.js';
import {createPortableBackup,validatePortableBackup} from '../app/src/main/assets/www/core/backup-service.js';
import {JournalService} from '../app/src/main/assets/www/core/journal-service.js';
import {PartyService} from '../app/src/main/assets/www/core/party-service.js';
const j=p=>JSON.parse(fs.readFileSync(new URL(p,import.meta.url),'utf8'));
const pack=j('../app/src/main/assets/www/data/wwn.starter-region.v1.json'),character=j('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json');
const persistence=new MemoryPersistenceAdapter();const boot=ensureStarterCampaign({persistence,character,pack});assert.ok(String(boot.status).startsWith('OK'));const id=boot.campaignId;
let loaded=persistence.loadCampaign(id);let state=JSON.parse(loaded.stateJson);assert.equal(validateCampaignState(state,{campaignId:id,revision:loaded.revision}).ok,true);
const backup=createPortableBackup({persistence,campaignId:id});assert.equal(backup.status,'OK');assert.equal(validatePortableBackup(backup.backup).status,'OK');
const journal=new JournalService({persistence});assert.equal(journal.list({campaignId:id}).status,'OK');
// Deliberate roster drift is detected and repaired without touching mechanics.
state.charactersState={};let check=validateCampaignState(state);assert.ok(check.warnings.includes('ACTIVE_CHARACTER_NOT_IN_ROSTER'));let fixed=repairCampaignState(state);assert.ok(fixed.repairs.includes('ACTIVE_CHARACTER_ADDED_TO_ROSTER'));assert.equal(fixed.validation.ok,true);
const party=new PartyService({persistence});let init=party.initialize({campaignId:id});assert.ok(['OK','PARTY_ALREADY_INITIALIZED_NO_MUTATION'].includes(init.status));
console.log(JSON.stringify({status:'PASS',campaignId:id,backup:true,integrity:true,journal:true,party:true}));
