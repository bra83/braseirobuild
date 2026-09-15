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
let winner=null;let attempts=0;
for(let attempt=1;attempt<=40&&!winner;attempt++){
  attempts=attempt;const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);character.characterId=`shove-${attempt}`;character.name='SHOVE TEST';character.attributes={...(character.attributes??{}),Strength:18};character.skills={...(character.skills??{}),Punch:1,Exert:1};
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-shove-smoke-${attempt}`});assert(String(boot.status).startsWith('OK'),`boot ${attempt} failed: ${boot.status}`);const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider();const scenes=new SceneSessionService({persistence,barbara});const combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`scene ${attempt} failed: ${opened.status}`);const started=await combat.startAtVigilia({campaignId});assert(started.status==='OK',`combat ${attempt} failed: ${started.status}`);
  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));if(projection.activeSideId==='ENEMY'){const enemyTurn=await combat.resolveEnemySide({campaignId});assert(enemyTurn.status==='OK',`enemy side ${attempt} failed: ${enemyTurn.status}`);projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  assert(projection.activeSideId==='PC',`PC side missing on ${attempt}`);const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0],enemy=projection.enemies.find(x=>!x.defeated);assert(pc&&enemy,`combatants missing ${attempt}`);
  const from=pc.position,to=enemy.position,dx=to.xFeet-from.xFeet,dy=to.yFeet-from.yFeet,d=Math.hypot(dx,dy);if(d>5){const travel=d-5,move=await combat.movePc({campaignId,pcId:pc.actorId,xFeet:from.xFeet+dx/d*travel,yFeet:from.yFeet+dy/d*travel});assert(move.status==='OK',`move ${attempt} failed: ${move.status}`);}
  const beforeState=JSON.parse(persistence.loadCampaign(campaignId).stateJson),hpBefore=beforeState.npcState[enemy.actorId].hp;
  const shove=await combat.shoveNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'UNARMED',opposedSkill:'Punch'});assert(shove.status==='OK',`shove ${attempt} failed: ${shove.status}`);const afterState=JSON.parse(persistence.loadCampaign(campaignId).stateJson),hpAfter=afterState.npcState[enemy.actorId].hp;
  assert(hpAfter===hpBefore,`shove caused damage on ${attempt}: ${hpBefore} -> ${hpAfter}`);assert(shove.attack.damage===0&&shove.attack.damageRolled===false,`shove attack rolled damage on ${attempt}`);assert(shove.targetHpBefore===shove.targetHpAfter,'service shove HP receipt must remain unchanged');assert(afterState.encounterState.combat.economy[pc.actorId].mainAvailable===false,'shove must consume Main action');assert(afterState.encounterState.lastCombatResolution?.kind==='SHOVE','shove result must persist');
  if(shove.outcome.success){assert(shove.attack.hit===true,'successful shove requires melee hit');assert(Number.isInteger(shove.attackerRoll?.total)&&Number.isInteger(shove.defenderRoll?.total),'successful shove requires opposed totals');assert(shove.attackerRoll.total>=shove.defenderRoll.total,'PC must win opposed shove, including ties');assert((afterState.encounterState.combat.proneActorIds??[]).includes(enemy.actorId),'successful shove must persist prone target');winner={attempt,campaignId,attackerTotal:shove.attackerRoll.total,defenderTotal:shove.defenderRoll.total,hpBefore,hpAfter};}
}
assert(winner,`no successful shove found in ${attempts} deterministic attempts`);
console.log(JSON.stringify({status:'PASS',attempts,winner},null,2));
console.log('WWN shove stateful smoke: OK');
