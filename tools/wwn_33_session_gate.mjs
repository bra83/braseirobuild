import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { SceneSessionService } from '../app/src/main/assets/www/core/scene-session-service.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { PlayerMechanicsFacade } from '../app/src/main/assets/www/core/player-mechanics-facade.js';
import { EquipmentService } from '../app/src/main/assets/www/core/equipment-service.js';
import { MundaneEquipmentService } from '../app/src/main/assets/www/core/mundane-equipment-service.js';
import { CombatSessionService } from '../app/src/main/assets/www/core/combat-session-service.js';

const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const baseCharacter=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
const assert=(value,message)=>{if(!value)throw new Error(message)};
const skillCases=[['Notice','Wisdom'],['Survive','Wisdom'],['Sneak','Dexterity'],['Exert','Constitution']];
const saveCases=['PHYSICAL','EVASION','MENTAL','LUCK'];
const purchaseCases=['CANDLE','TORCH','ROPE_50','WATERSKIN_GALLON','RATIONS_WEEK'];
const reports=[];

for(let sessionNo=1;sessionNo<=33;sessionNo++){
  const persistence=new MemoryPersistenceAdapter();
  const character=structuredClone(baseCharacter);
  character.characterId=`iria-s${sessionNo}`;character.name=`IRIA ${sessionNo}`;delete character.silver;delete character.copper;character.inventory=null;
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-33-session-${sessionNo}`});
  assert(String(boot.status).startsWith('OK'),`S${sessionNo} boot failed: ${boot.status}`);
  const campaignId=boot.campaignId;
  const barbara=new DeterministicNarrativeProvider();
  const scenes=new SceneSessionService({persistence,barbara});
  const mechanics=new PlayerMechanicsFacade({persistence});
  const equipment=new EquipmentService({persistence});
  const mundane=new MundaneEquipmentService({persistence});
  const combat=new CombatSessionService({persistence,barbara});

  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});
  assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`S${sessionNo} scene failed: ${opened.status}`);

  let expectedRevision=persistence.loadCampaign(campaignId).revision;
  for(let i=0;i<4;i++){
    const [skill,attribute]=skillCases[(sessionNo+i)%skillCases.length];
    const result=mechanics.skillCheck({campaignId,skill,attribute,difficulty:6+((sessionNo+i)%5)});
    assert(result.status==='OK',`S${sessionNo} skill ${skill} failed: ${result.status}`);
    expectedRevision+=1;assert(result.summary.revision===expectedRevision,`S${sessionNo} skill revision mismatch`);
  }
  for(let i=0;i<2;i++){
    const saveType=saveCases[(sessionNo+i)%saveCases.length];
    const result=mechanics.rollSave({campaignId,saveType});
    assert(result.status==='OK',`S${sessionNo} save ${saveType} failed: ${result.status}`);
    expectedRevision+=1;assert(result.summary.revision===expectedRevision,`S${sessionNo} save revision mismatch`);
  }

  const init=equipment.initializeStartingGear({campaignId});
  assert(init.status==='OK',`S${sessionNo} starting gear failed: ${init.status}`);expectedRevision+=1;
  const purchaseId=purchaseCases[sessionNo%purchaseCases.length];
  const bought=mundane.purchase({campaignId,catalogId:purchaseId,quantity:1,carryState:'STOWED'});
  assert(bought.status==='OK',`S${sessionNo} purchase ${purchaseId} failed: ${bought.status}`);expectedRevision+=1;
  if(bought.acquired?.[0]?.itemId){
    const ready=mundane.setCarryState({campaignId,itemId:bought.acquired[0].itemId,to:'READIED'});
    assert(ready.status==='OK',`S${sessionNo} ready item failed: ${ready.status}`);expectedRevision+=1;
  }

  const start=await combat.startAtVigilia({campaignId});
  assert(['OK','COMBAT_ALREADY_ACTIVE_NO_MUTATION'].includes(start.status),`S${sessionNo} combat start failed: ${start.status}`);
  if(start.status==='OK')expectedRevision+=1;

  const beforeReload=persistence.loadCampaign(campaignId);
  assert(beforeReload.status==='OK',`S${sessionNo} pre-reload missing`);
  assert(beforeReload.revision===expectedRevision,`S${sessionNo} expected revision ${expectedRevision}, got ${beforeReload.revision}`);
  const serialized=beforeReload.stateJson;
  const roundTrip=JSON.stringify(JSON.parse(serialized));
  assert(JSON.parse(roundTrip).revision===expectedRevision,`S${sessionNo} JSON round-trip revision mismatch`);
  const events=persistence.listEvents(campaignId);
  assert(events.status==='OK',`S${sessionNo} event listing failed`);
  assert((events.events??[]).length>=expectedRevision,`S${sessionNo} event count lower than revision`);
  const summary=mechanics.summary({campaignId});
  assert(summary.status==='OK'&&summary.revision===expectedRevision,`S${sessionNo} final UI summary mismatch`);
  assert(summary.combat.active===true,`S${sessionNo} combat not active after start`);
  reports.push({sessionNo,revision:expectedRevision,eventCount:events.events.length,skillChecks:4,saves:2,purchase:purchaseId,combatActive:summary.combat.active,character:summary.character.name});
}

const totalEvents=reports.reduce((n,r)=>n+r.eventCount,0);
const minRevision=Math.min(...reports.map(r=>r.revision));
const maxRevision=Math.max(...reports.map(r=>r.revision));
console.log(JSON.stringify({status:'PASS',sessions:reports.length,totalEvents,minRevision,maxRevision,reports},null,2));
assert(reports.length===33,'33 sessions were not completed');
assert(reports.every(r=>r.combatActive),'not every session reached live combat');
console.log(`WWN 33-session integrated gate: PASS • ${reports.length} sessions • ${totalEvents} persisted events`);
