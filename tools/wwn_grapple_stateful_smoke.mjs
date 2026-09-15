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

function recordDamage(turnResult,attempt,phase){
  const rows=turnResult?.grappleEndRound??[];
  const hit=rows.find(x=>x?.status==='DAMAGE_APPLIED');
  if(hit&&!damageEvidence){
    assert(hit.hpAfter<hit.hpBefore,`automatic grapple damage did not reduce HP on ${attempt}`);
    assert(hit.damage?.damageKind==='AUTOMATIC_HIT',`automatic grapple damage kind missing on ${attempt}`);
    assert(Number.isInteger(hit.damage?.damage)&&hit.damage.damage>0,`automatic grapple damage invalid on ${attempt}`);
    damageEvidence={attempt,phase,hpBefore:hit.hpBefore,hpAfter:hit.hpAfter,damage:hit.damage.damage};
  }
}

for(let attempt=1;attempt<=100&&!(startEvidence&&restrictionEvidence&&escapeEvidence&&damageEvidence);attempt++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);
  character.characterId=`grapple-${attempt}`;character.name='GRAPPLE TEST';
  character.attributes={...(character.attributes??{}),Strength:18};
  character.skills={...(character.skills??{}),Punch:2};
  character.inventory=[];
  character.hp={...(character.hp??{}),current:30,max:30};

  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-grapple-smoke-${attempt}`});
  assert(String(boot.status).startsWith('OK'),`boot ${attempt} failed: ${boot.status}`);
  const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider();
  const scenes=new SceneSessionService({persistence,barbara});
  const combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});
  assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`scene ${attempt} failed: ${opened.status}`);
  const started=await combat.startAtVigilia({campaignId});
  assert(started.status==='OK',`combat ${attempt} failed: ${started.status}`);

  let projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));
  const enemyFirst=projection.activeSideId==='ENEMY';
  if(enemyFirst){
    const openingEnemy=await combat.resolveEnemySide({campaignId});
    assert(['OK','PC_SIDE_INCAPACITATED'].includes(openingEnemy.status),`opening enemy side ${attempt} failed: ${openingEnemy.status}`);
    projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));
  }
  if(projection.active!==true||projection.activeSideId!=='PC')continue;

  const pc=projection.pcs.find(x=>x.actorId===character.characterId)??projection.pcs[0];
  const enemy=projection.enemies.find(x=>!x.defeated);
  if(!pc||!enemy)continue;
  assert(pc.hands?.status==='OK'&&pc.hands.bothHandsFree===true,`hands-free projection missing on ${attempt}`);

  const from=pc.position,to=enemy.position,dx=to.xFeet-from.xFeet,dy=to.yFeet-from.yFeet,d=Math.hypot(dx,dy);
  if(d>5){
    const travel=d-5;
    const move=await combat.movePc({campaignId,pcId:pc.actorId,xFeet:from.xFeet+dx/d*travel,yFeet:from.yFeet+dy/d*travel});
    assert(move.status==='OK',`move ${attempt} failed: ${move.status}`);
  }

  const before=JSON.parse(persistence.loadCampaign(campaignId).stateJson),hpBefore=before.npcState[enemy.actorId].hp;
  const grapple=await combat.grappleNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId});
  assert(grapple.status==='OK',`grapple ${attempt} failed: ${grapple.status}`);
  const after=JSON.parse(persistence.loadCampaign(campaignId).stateJson),hpAfter=after.npcState[enemy.actorId].hp;
  assert(hpAfter===hpBefore,`grapple start caused damage on ${attempt}: ${hpBefore} -> ${hpAfter}`);
  assert(grapple.attack.damage===0&&grapple.attack.damageRolled===false,`grapple opening attack rolled damage on ${attempt}`);
  assert(after.encounterState.combat.economy[pc.actorId].mainAvailable===false,`grapple must consume Main action on ${attempt}`);
  assert(after.encounterState.lastCombatResolution?.kind==='GRAPPLE_START',`grapple result did not persist on ${attempt}`);
  if(!grapple.outcome.success)continue;

  const link=(after.encounterState.combat.grapples??[]).find(x=>x.assailantId===pc.actorId&&x.targetId===enemy.actorId);
  assert(link,`successful grapple link missing on ${attempt}`);
  startEvidence??={attempt,enemyFirst,attackerTotal:grapple.attackerRoll?.total,defenderTotal:grapple.defenderRoll?.total,hpBefore,hpAfter};

  const projected=projectCombatForPlayer(after),grapplingPc=projected.pcs.find(x=>x.actorId===pc.actorId),grappledEnemy=projected.enemies.find(x=>x.actorId===enemy.actorId);
  assert(grapplingPc?.grapple?.locked===true&&grappledEnemy?.grapple?.locked===true,`grapple projection missing on ${attempt}`);
  const blockedMove=await combat.movePc({campaignId,pcId:pc.actorId,xFeet:pc.position.xFeet+1,yFeet:pc.position.yFeet});
  assert(blockedMove.status==='GRAPPLED_CANNOT_MOVE_NO_MUTATION',`grapple movement restriction failed on ${attempt}: ${blockedMove.status}`);
  const blockedWeapon=await combat.attackNpc({campaignId,pcId:pc.actorId,npcId:enemy.actorId,weaponId:'NOT_UNARMED'});
  assert(blockedWeapon.status==='GRAPPLE_UNARMED_ONLY_NO_MUTATION',`grapple weapon restriction failed on ${attempt}: ${blockedWeapon.status}`);
  restrictionEvidence??={attempt,moveStatus:blockedMove.status,weaponStatus:blockedWeapon.status};

  const pcEnd=await combat.endPcSide({campaignId});
  assert(pcEnd.status==='OK',`end PC side ${attempt} failed: ${pcEnd.status}`);
  recordDamage(pcEnd,attempt,enemyFirst?'PC_LAST_SIDE':'PC_FIRST_SIDE');

  let stateAfterPc=JSON.parse(persistence.loadCampaign(campaignId).stateJson);
  if(stateAfterPc.encounterState?.combat?.active===true&&stateAfterPc.encounterState.combat.activeSideId==='ENEMY'){
    const enemyTurn=await combat.resolveEnemySide({campaignId});
    assert(['OK','PC_VICTORY'].includes(enemyTurn.status),`grappled enemy side ${attempt} failed: ${enemyTurn.status}`);
    const escapeAction=(enemyTurn.actions??[]).find(a=>a?.grappleEscape)?.grappleEscape;
    if(escapeAction&&!escapeEvidence){
      assert(escapeAction.kind==='GRAPPLE_ESCAPE'&&escapeAction.actionType==='MAIN',`enemy grapple escape did not consume Main on ${attempt}`);
      assert(Number.isInteger(escapeAction.victimRoll?.total),`enemy escape roll missing on ${attempt}`);
      assert(Array.isArray(escapeAction.assailantRolls)&&escapeAction.assailantRolls.length>=1,`assailant opposed roll missing on ${attempt}`);
      escapeEvidence={attempt,escaped:escapeAction.escape?.escaped===true,victimTotal:escapeAction.victimRoll.total,assailantTotals:escapeAction.assailantRolls.map(x=>x.roll.total)};
    }
    recordDamage(enemyTurn,attempt,'ENEMY_LAST_SIDE');
  }
}

assert(startEvidence,'no successful grapple start found in deterministic attempts');
assert(restrictionEvidence,'grapple movement/weapon restrictions were not certified');
assert(escapeEvidence,'enemy Main Action grapple escape was not observed');
assert(damageEvidence,'automatic end-of-round grapple damage was not observed');
console.log(JSON.stringify({status:'PASS',startEvidence,restrictionEvidence,escapeEvidence,damageEvidence},null,2));
console.log('WWN grapple stateful smoke: OK');
