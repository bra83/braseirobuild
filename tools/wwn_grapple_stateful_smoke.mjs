import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { SceneSessionService } from '../app/src/main/assets/www/core/scene-session-service.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { CombatSessionService } from '../app/src/main/assets/www/core/combat-session-service.js';
import { projectCombatForPlayer } from '../app/src/main/assets/www/core/combat-projection.js';

const assert=(v,m)=>{if(!v)throw new Error(m)};
const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const base=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
let startEvidence=null,restrictionEvidence=null,escapeEvidence=null,damageEvidence=null;

for(let attempt=1;attempt<=100&&!(startEvidence&&restrictionEvidence&&escapeEvidence&&damageEvidence);attempt++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);
  character.characterId=`grapple-${attempt}`;character.name='GRAPPLE TEST';character.attributes={...(character.attributes??{}),Strength:18};character.skills={...(character.skills??{}),Punch:2};character.inventory=[];character.hp={...(character.hp??{}),current:30,max:30};
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-grapple-smoke-${attempt}`});assert(String(boot.status).startsWith('OK'),`boot ${attempt} failed: ${boot.status}`);
  const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider(),scenes=new SceneSessionService({persistence,barbara}),combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`scene ${attempt} failed: ${opened.status}`);
  const started=await combat.startAtVigilia({campaignId});assert(started.status==='OK',`combat ${attempt} failed: ${started.status}`);
  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));const enemyFirst=projection.activeSideId==='ENEMY';
  if(enemyFirst){const enemyTurn=await combat.resolveEnemySide({campaignId});assert(['OK','PC_SIDE_INCAPACITATED'].includes(enemyTurn.status),`opening enemy side ${attempt} failed: ${enemyTurn.status}`);projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  if(projection.active!==true||projection.activeSideId!=='PC')continue;
  const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0],enemy=projection.enemies.find(x=>!x.defeated);if(!pc||!enemy)continue;
  assert(pc.hands===undefined,'grapple projection should not exist before grapple patch application');
}

console.log('PREPATCH_SENTINEL');
