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
const near=(a,b)=>Math.abs(a-b)<1e-7;
let evidence=null;

for(let attempt=1;attempt<=100&&!evidence;attempt++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);
  character.characterId=`charge-${attempt}`;character.name='CHARGE TEST';character.inventory=[];character.hp={...(character.hp??{}),current:30,max:30};character.skills={...(character.skills??{}),Punch:0};
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-charge-smoke-${attempt}`});assert(String(boot.status).startsWith('OK'),`boot failed ${attempt}: ${boot.status}`);
  const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider(),scenes=new SceneSessionService({persistence,barbara}),combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`scene failed ${attempt}: ${opened.status}`);
  const started=await combat.startAtVigilia({campaignId});assert(started.status==='OK',`start failed ${attempt}: ${started.status}`);
  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));
  if(projection.activeSideId!=='PC')continue;
  const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0],enemy=projection.enemies.find(x=>!x.defeated);if(!pc||!enemy)continue;
  const initialLoaded=persistence.loadCampaign(campaignId),initialRevision=initialLoaded.revision;
  const tooShort=await combat.chargeNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'UNARMED',xFeet:pc.position.xFeet+5,yFeet:pc.position.yFeet});
  assert(tooShort.status==='CHARGE_TOO_SHORT_NO_MUTATION',`short charge status ${attempt}: ${tooShort.status}`);assert(persistence.loadCampaign(campaignId).revision===initialRevision,'short charge mutated state');

  const dx=enemy.position.xFeet-pc.position.xFeet,dy=enemy.position.yFeet-pc.position.yFeet,d=Math.hypot(dx,dy);if(d<15)continue;
  const destination={xFeet:enemy.position.xFeet-dx/d*5,yFeet:enemy.position.yFeet-dy/d*5};
  const charge=await combat.chargeNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'UNARMED',...destination});
  assert(charge.status==='OK',`charge failed ${attempt}: ${charge.status}`);assert(charge.charge?.status==='OK','charge inspection missing');assert(charge.charge.hitModifier===2,'charge +2 hit modifier missing');assert(charge.charge.armorClassModifierUntilRoundEnd===-2,'charge -2 AC modifier missing');assert(charge.actionCost?.status==='OK','charge action cost missing');
  const after=JSON.parse(persistence.loadCampaign(campaignId).stateJson),c=after.encounterState.combat,pos=c.tactical.actors[pc.actorId];
  assert(c.economy[pc.actorId].mainAvailable===false&&c.economy[pc.actorId].moveAvailable===false,'charge must spend Main and Move');assert(near(pos.xFeet,destination.xFeet)&&near(pos.yFeet,destination.yFeet),'charge destination not persisted');assert(c.chargeArmorPenalties?.[pc.actorId]?.round===c.round&&c.chargeArmorPenalties[pc.actorId].modifier===-2,'charge AC penalty state missing');assert(after.encounterState.lastCombatResolution?.charge?.hitModifier===2,'charge last resolution missing');
  const events=persistence.listEvents(campaignId);assert(events.status==='OK','event list missing');const last=events.events.at(-1);assert(last?.eventKind==='PLAYER_CHARGE',`charge mechanical event kind wrong: ${last?.eventKind}`);
  const chargedProjection=projectCombatForPlayer(after),chargedPc=chargedProjection.pcs.find(x=>x.actorId===pc.actorId);assert(chargedPc.chargeArmorClassModifier===-2,'projection charge modifier missing');assert(chargedPc.armorClass===chargedPc.baseArmorClass-2,'projected AC must include charge penalty');
  if(c.active!==true)continue;
  const end=await combat.endPcSide({campaignId});assert(end.status==='OK',`end PC failed ${attempt}: ${end.status}`);
  const mid=JSON.parse(persistence.loadCampaign(campaignId).stateJson);if(mid.encounterState.combat.activeSideId!=='ENEMY')continue;
  const enemyTurn=await combat.resolveEnemySide({campaignId});assert(['OK','PC_SIDE_INCAPACITATED'].includes(enemyTurn.status),`enemy side failed ${attempt}: ${enemyTurn.status}`);
  const enemyAttack=(enemyTurn.actions??[]).find(a=>a?.attack)?.attack;if(enemyAttack)assert(enemyAttack.relevantAc===chargedPc.baseArmorClass-2,`enemy attack ignored charge AC penalty: ${enemyAttack.relevantAc} vs ${chargedPc.baseArmorClass-2}`);
  const nextState=JSON.parse(persistence.loadCampaign(campaignId).stateJson);if(nextState.encounterState?.combat?.active!==true)continue;
  const nextProjection=projectCombatForPlayer(nextState),nextPc=nextProjection.pcs.find(x=>x.actorId===pc.actorId);assert(nextProjection.round===chargedProjection.round+1,'round did not advance');assert(nextPc.chargeArmorClassModifier===0,'charge penalty did not expire at end of round');assert(nextPc.armorClass===nextPc.baseArmorClass,'AC did not recover after charge round');
  evidence={attempt,distanceFeet:charge.charge.distanceFeet,hitModifier:charge.charge.hitModifier,armorPenalty:-2,enemyRelevantAc:enemyAttack?.relevantAc??null,nextRound:nextProjection.round};
}
assert(evidence,'no fully certifiable charge scenario found');
console.log(JSON.stringify({status:'PASS',evidence},null,2));
console.log('WWN charge stateful smoke: OK');
