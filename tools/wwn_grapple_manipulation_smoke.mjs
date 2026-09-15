import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { SceneSessionService } from '../app/src/main/assets/www/core/scene-session-service.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { CombatSessionService } from '../app/src/main/assets/www/core/combat-session-service.js';
import { projectCombatForPlayer } from '../app/src/main/assets/www/core/combat-projection.js';
import { grappleMoveOutcome } from '../app/src/main/assets/www/core/wwn-special-combat-core.js';

const assert=(v,m)=>{if(!v)throw new Error(m)};
const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const base=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
const near=(a,b)=>Math.abs(a-b)<1e-7;

const tie=grappleMoveOutcome({attackerOpposedTotal:9,defenderOpposedTotal:9,attackerIsPc:true,defenderIsPc:false,mode:'MOVE'});
assert(tie.success===false&&tie.targetEscapes===true&&tie.winner==='TIE','grapple manipulation tie must free the target even when attacker is a PC');

async function heldScenario(attempt,label){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);
  character.characterId=`grapple-${label}-${attempt}`;character.name=`GRAPPLE ${label}`;
  character.attributes={...(character.attributes??{}),Strength:18};character.skills={...(character.skills??{}),Punch:0};character.inventory=[];character.hp={...(character.hp??{}),current:30,max:30};
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-grapple-${label}-${attempt}`});if(!String(boot.status).startsWith('OK'))return null;
  const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider(),scenes=new SceneSessionService({persistence,barbara}),combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});if(!['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status))return null;
  const started=await combat.startAtVigilia({campaignId});if(started.status!=='OK')return null;
  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));
  if(projection.activeSideId==='ENEMY'){const opening=await combat.resolveEnemySide({campaignId});if(!['OK','PC_SIDE_INCAPACITATED'].includes(opening.status))return null;projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  if(projection.active!==true||projection.activeSideId!=='PC')return null;
  const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0],enemy=projection.enemies.find(x=>!x.defeated);if(!pc||!enemy)return null;
  const from=pc.position,to=enemy.position,dx=to.xFeet-from.xFeet,dy=to.yFeet-from.yFeet,d=Math.hypot(dx,dy);if(d>5){const travel=d-5,move=await combat.movePc({campaignId,pcId:pc.actorId,xFeet:from.xFeet+dx/d*travel,yFeet:from.yFeet+dy/d*travel});if(move.status!=='OK')return null;}
  const grapple=await combat.grappleNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId});if(grapple.status!=='OK'||grapple.outcome.success!==true)return null;
  const ended=await combat.endPcSide({campaignId});if(ended.status!=='OK')return null;
  let state=JSON.parse(persistence.loadCampaign(campaignId).stateJson);if(state.encounterState?.combat?.active!==true)return null;
  if(state.encounterState.combat.activeSideId==='ENEMY'){const enemyTurn=await combat.resolveEnemySide({campaignId});if(!['OK','PC_VICTORY'].includes(enemyTurn.status))return null;state=JSON.parse(persistence.loadCampaign(campaignId).stateJson);}
  if(state.encounterState?.combat?.active!==true||state.encounterState.combat.activeSideId!=='PC')return null;
  const link=(state.encounterState.combat.grapples??[]).find(x=>x.assailantId===pc.actorId&&x.targetId===enemy.actorId);if(!link)return null;
  return {persistence,combat,campaignId,pcId:pc.actorId,npcId:enemy.actorId,state};
}

let moveEvidence=null;
for(let attempt=1;attempt<=120&&!moveEvidence;attempt++){
  const s=await heldScenario(attempt,'move');if(!s)continue;
  const c=s.state.encounterState.combat,from=structuredClone(c.tactical.actors[s.pcId]),targetFrom=structuredClone(c.tactical.actors[s.npcId]);
  const destination={xFeet:from.xFeet+5,yFeet:from.yFeet};
  const r=await s.combat.manipulateGrappledNpc({campaignId:s.campaignId,pcId:s.pcId,npcId:s.npcId,mode:'MOVE',...destination});assert(r.status==='OK',`move manipulation failed on ${attempt}: ${r.status}`);
  const after=JSON.parse(s.persistence.loadCampaign(s.campaignId).stateJson),ac=after.encounterState.combat;
  assert(ac.economy[s.pcId].mainAvailable===false,'grapple move must consume Main action');
  if(!r.outcome.success){assert(!(ac.grapples??[]).some(x=>x.assailantId===s.pcId&&x.targetId===s.npcId),'failed grapple move must release target');continue;}
  const pcAfter=ac.tactical.actors[s.pcId],targetAfter=ac.tactical.actors[s.npcId];
  assert(near(pcAfter.xFeet,destination.xFeet)&&near(pcAfter.yFeet,destination.yFeet),'grapple move did not move assailant to destination');
  assert(near(targetAfter.xFeet,targetFrom.xFeet+5)&&near(targetAfter.yFeet,targetFrom.yFeet),'grapple move did not carry target by same vector');
  assert((ac.grapples??[]).some(x=>x.assailantId===s.pcId&&x.targetId===s.npcId),'successful grapple move must preserve grapple');
  moveEvidence={attempt,distanceFeet:r.distanceFeet,attackerTotal:r.attackerRoll.total,defenderTotal:r.defenderRoll.total};
}
assert(moveEvidence,'no successful grapple move found');

let throwEvidence=null;
for(let attempt=1;attempt<=120&&!throwEvidence;attempt++){
  const s=await heldScenario(attempt,'throw');if(!s)continue;
  const c=s.state.encounterState.combat,targetFrom=structuredClone(c.tactical.actors[s.npcId]);
  const destination={xFeet:targetFrom.xFeet+3,yFeet:targetFrom.yFeet};
  const r=await s.combat.manipulateGrappledNpc({campaignId:s.campaignId,pcId:s.pcId,npcId:s.npcId,mode:'THROW',...destination});assert(r.status==='OK',`throw manipulation failed on ${attempt}: ${r.status}`);
  const after=JSON.parse(s.persistence.loadCampaign(s.campaignId).stateJson),ac=after.encounterState.combat;
  assert(ac.economy[s.pcId].mainAvailable===false,'grapple throw must consume Main action');
  if(!r.outcome.success){assert(!(ac.grapples??[]).some(x=>x.assailantId===s.pcId&&x.targetId===s.npcId),'failed grapple throw must release target');continue;}
  const targetAfter=ac.tactical.actors[s.npcId];
  assert(near(targetAfter.xFeet,destination.xFeet)&&near(targetAfter.yFeet,destination.yFeet),'grapple throw target destination mismatch');
  assert((ac.proneActorIds??[]).includes(s.npcId),'successful grapple throw must leave target prone');
  assert(!(ac.grapples??[]).some(x=>x.targetId===s.npcId),'successful grapple throw must end grapple');
  throwEvidence={attempt,distanceFeet:r.distanceFeet,attackerTotal:r.attackerRoll.total,defenderTotal:r.defenderRoll.total,prone:true};
}
assert(throwEvidence,'no successful grapple throw found');

console.log(JSON.stringify({status:'PASS',tie,moveEvidence,throwEvidence},null,2));
console.log('WWN grapple manipulation smoke: OK');
