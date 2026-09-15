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
let winner=null;
for(let attempt=1;attempt<=60&&!winner;attempt++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);character.characterId=`shove-push-${attempt}`;character.name='SHOVE PUSH TEST';character.attributes={...(character.attributes??{}),Strength:18};character.skills={...(character.skills??{}),Punch:2,Exert:2};
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-shove-push-${attempt}`});assert(String(boot.status).startsWith('OK'),`boot ${attempt} failed: ${boot.status}`);const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider(),scenes=new SceneSessionService({persistence,barbara}),combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`scene ${attempt} failed: ${opened.status}`);const started=await combat.startAtVigilia({campaignId});assert(started.status==='OK',`combat ${attempt} failed: ${started.status}`);
  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));if(projection.activeSideId==='ENEMY'){const enemyTurn=await combat.resolveEnemySide({campaignId});assert(enemyTurn.status==='OK',`enemy side ${attempt} failed: ${enemyTurn.status}`);projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  assert(projection.activeSideId==='PC',`PC side missing ${attempt}`);const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0],enemy=projection.enemies.find(x=>!x.defeated);assert(pc&&enemy,`combatants missing ${attempt}`);
  const from=pc.position,to=enemy.position,dx=to.xFeet-from.xFeet,dy=to.yFeet-from.yFeet,d=Math.hypot(dx,dy);if(d>5){const travel=d-5,move=await combat.movePc({campaignId,pcId:pc.actorId,xFeet:from.xFeet+dx/d*travel,yFeet:from.yFeet+dy/d*travel});assert(move.status==='OK',`move ${attempt} failed: ${move.status}`);}
  const ready=JSON.parse(persistence.loadCampaign(campaignId).stateJson),rev=ready.revision,pcPos=ready.encounterState.combat.tactical.actors[pc.actorId],enemyPos=structuredClone(ready.encounterState.combat.tactical.actors[enemy.actorId]),hpBefore=ready.npcState[enemy.actorId].hp;
  const tooFar=await combat.shoveNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'UNARMED',opposedSkill:'Punch',effect:'PUSH',pushFeet:12});assert(tooFar.status==='SHOVE_PUSH_DISTANCE_INVALID_NO_MUTATION',`oversize push status ${attempt}: ${tooFar.status}`);assert(persistence.loadCampaign(campaignId).revision===rev,'invalid push mutated state');
  const pushFeet=7,attempted=await combat.shoveNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'UNARMED',opposedSkill:'Punch',effect:'PUSH',pushFeet});assert(attempted.status==='OK',`push ${attempt} failed: ${attempted.status}`);const after=JSON.parse(persistence.loadCampaign(campaignId).stateJson),hpAfter=after.npcState[enemy.actorId].hp;
  assert(attempted.effect==='PUSH','push effect missing');assert(attempted.attack.damage===0&&attempted.attack.damageRolled===false,'push opening attack rolled damage');assert(hpAfter===hpBefore,'push changed target HP');assert(after.encounterState.combat.economy[pc.actorId].mainAvailable===false,'push must spend Main');assert(after.encounterState.lastCombatResolution?.effect==='PUSH','push result not persisted');
  if(!attempted.outcome.success)continue;
  const baseDistance=Math.hypot(enemyPos.xFeet-pcPos.xFeet,enemyPos.yFeet-pcPos.yFeet),ux=(enemyPos.xFeet-pcPos.xFeet)/baseDistance,uy=(enemyPos.yFeet-pcPos.yFeet)/baseDistance,expected={xFeet:enemyPos.xFeet+ux*pushFeet,yFeet:enemyPos.yFeet+uy*pushFeet},actual=after.encounterState.combat.tactical.actors[enemy.actorId];
  assert(attempted.pushDestination&&near(attempted.pushDestination.xFeet,expected.xFeet)&&near(attempted.pushDestination.yFeet,expected.yFeet),'service push destination wrong');assert(near(actual.xFeet,expected.xFeet)&&near(actual.yFeet,expected.yFeet),'persisted pushed position wrong');assert(!(after.encounterState.combat.proneActorIds??[]).includes(enemy.actorId),'PUSH option must not automatically knock target prone');winner={attempt,pushFeet,attackerTotal:attempted.attackerRoll.total,defenderTotal:attempted.defenderRoll.total,hpBefore,hpAfter,expected,actual};
}
assert(winner,'no successful shove-push found');
console.log(JSON.stringify({status:'PASS',winner},null,2));
console.log('WWN shove push stateful smoke: OK');
