import fs from 'node:fs';import assert from 'node:assert/strict';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {createPortableBackup,validatePortableBackup,restorePortableBackupToMemory} from '../app/src/main/assets/www/core/backup-service.js';
import {validateCampaignState,repairCampaignState} from '../app/src/main/assets/www/core/campaign-integrity.js';
import {CharacterRosterService} from '../app/src/main/assets/www/core/character-roster-service.js';
import {PartyService} from '../app/src/main/assets/www/core/party-service.js';
import {JournalService} from '../app/src/main/assets/www/core/journal-service.js';
import {exportPortableCharacterJson,importPortableCharacterJson} from '../app/src/main/assets/www/core/character-portable.js';
const j=p=>JSON.parse(fs.readFileSync(new URL(p,import.meta.url),'utf8'));
const pack=j('../app/src/main/assets/www/data/wwn.starter-region.v1.json'),character=j('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json');
for(let session=1;session<=33;session++){
 const persistence=new MemoryPersistenceAdapter();const boot=ensureStarterCampaign({persistence,character,pack});assert.ok(String(boot.status).startsWith('OK'));const id=boot.campaignId;
 let loaded=persistence.loadCampaign(id);let state=JSON.parse(loaded.stateJson);assert.equal(validateCampaignState(state,{campaignId:id,revision:loaded.revision}).ok,true);
 const roster=new CharacterRosterService({persistence});const listed=roster.list({campaignId:id});assert.equal(listed.status,'OK');const active=listed.activeCharacterId;assert.ok(active);
 const exported=roster.export({campaignId:id,characterId:active});assert.equal(exported.status,'OK');assert.equal(importPortableCharacterJson(exported.json).status,'OK');
 const badCharacter=JSON.parse(exported.json);delete badCharacter.character.characterId;assert.equal(importPortableCharacterJson(JSON.stringify(badCharacter)).status,'INVALID_CHARACTER');
 const party=new PartyService({persistence});assert.ok(['OK','PARTY_ALREADY_INITIALIZED_NO_MUTATION'].includes(party.initialize({campaignId:id}).status));
 const backup=createPortableBackup({persistence,campaignId:id});assert.equal(validatePortableBackup(backup.backup).status,'OK');
 const duplicateSnapshot=structuredClone(backup.backup);duplicateSnapshot.snapshots=[...(duplicateSnapshot.snapshots??[]),...(duplicateSnapshot.snapshots??[]).slice(0,1)];if(duplicateSnapshot.snapshots.length>1)assert.equal(validatePortableBackup(duplicateSnapshot).status,'DUPLICATE_BACKUP_SNAPSHOT');
 const copy=new MemoryPersistenceAdapter();assert.equal(restorePortableBackupToMemory({persistence:copy,backup:backup.backup,targetCampaignId:`${id}-deep-${session}`}).status,'OK');
 assert.equal(new JournalService({persistence}).timeline({campaignId:id,limit:500}).status,'OK');
 loaded=persistence.loadCampaign(id);state=JSON.parse(loaded.stateJson);const drift=structuredClone(state);if(drift.characterState?.characterId){drift.charactersState[drift.characterState.characterId]={...drift.charactersState[drift.characterState.characterId],name:'DRIFT'};const repaired=repairCampaignState(drift);assert.ok(repaired.repairs.includes('ACTIVE_CHARACTER_ROSTER_SYNCED'));assert.equal(repaired.validation.ok,true);}
}
console.log(JSON.stringify({status:'PASS',simulatedSessions:33,depth:'portable-character+backup+integrity+party+journal'}));
