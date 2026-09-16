import fs from 'node:fs';import assert from 'node:assert/strict';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {createPortableBackup,validatePortableBackup,restorePortableBackupToMemory} from '../app/src/main/assets/www/core/backup-service.js';
import {JournalService} from '../app/src/main/assets/www/core/journal-service.js';
import {CharacterRosterService} from '../app/src/main/assets/www/core/character-roster-service.js';
import {PartyService} from '../app/src/main/assets/www/core/party-service.js';
const j=p=>JSON.parse(fs.readFileSync(new URL(p,import.meta.url),'utf8'));
const pack=j('../app/src/main/assets/www/data/wwn.starter-region.v1.json'),character=j('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json');
for(let session=1;session<=33;session++){
 const persistence=new MemoryPersistenceAdapter();const boot=ensureStarterCampaign({persistence,character,pack});assert.ok(String(boot.status).startsWith('OK'));const id=boot.campaignId;
 const roster=new CharacterRosterService({persistence});assert.equal(roster.list({campaignId:id}).status,'OK');
 const party=new PartyService({persistence});assert.ok(['OK','PARTY_ALREADY_INITIALIZED_NO_MUTATION'].includes(party.initialize({campaignId:id}).status));
 const backup=createPortableBackup({persistence,campaignId:id});assert.equal(backup.status,'OK');assert.equal(validatePortableBackup(backup.backup).status,'OK');
 const bad=structuredClone(backup.backup);bad.events=[{eventId:'x',revision:999,eventKind:'BROKEN'}];assert.equal(validatePortableBackup(bad).status,'INVALID_BACKUP_EVENT');
 const copy=new MemoryPersistenceAdapter();const restored=restorePortableBackupToMemory({persistence:copy,backup:backup.backup,targetCampaignId:`${id}-copy-${session}`});assert.equal(restored.status,'OK');
 const journal=new JournalService({persistence});assert.equal(journal.list({campaignId:id,query:'',limit:2000}).status,'OK');
}
console.log(JSON.stringify({status:'PASS',simulatedSessions:33,areas:['starter','roster','party','backup','restore','journal']}));
