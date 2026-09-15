import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { SceneSessionService } from '../app/src/main/assets/www/core/scene-session-service.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { CombatSessionService } from '../app/src/main/assets/www/core/combat-session-service.js';
import { projectCombatForPlayer } from '../app/src/main/assets/www/core/combat-projection.js';

const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const base=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
const assert=(v,m)=>{if(!v)throw new Error(m)};
const reports=[];

for(let sessionNo=1;sessionNo<=33;sessionNo++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);
  character.characterId=`play-s${sessionNo}`;character.name=`PLAY SESSION ${sessionNo}`;
  character.hp={...(character.hp??{}),current:30,max:30};
  character.attributes={...(character.attributes??{}),Strength:18,Dexterity:18};
  character.skills={...(character.skills??{}),Punch:1};character.baseAttackBonus=Math.max(3,Number(character.baseAttackBonus??0));character.inventory=[];
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-full-play-${sessionNo}`});
  assert(String(boot.status).startsWith('OK'),`S${sessionNo} boot: ${boot.status}`);const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider(),scenes=new SceneSessionService({persistence,barbara}),combat=new CombatSessionService({persistence,barbara});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});assert(['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status),`S${sessionNo} scene: ${opened.status}`);
  const started=await combat.startAtVigilia({campaignId});assert(started.status==='OK',`S${sessionNo} combat start: ${started.status}`);
  let p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));assert(p.status==='OK'&&p.active,`S${sessionNo} projection after start`);let enemyActions=0,pcActions=0;
  if(p.activeSideId==='ENEMY'){
    const e=await combat.resolveEnemySide({campaignId});assert(['OK','PC_SIDE_INCAPACITATED'].includes(e.status),`S${sessionNo} opening enemy: ${e.status}`);enemyActions+=(e.actions??[]).length;p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));
  }
  assert(p.active&&p.activeSideId==='PC',`S${sessionNo} PC side not reached`);const pc=p.pcs.find(x=>x.actorId===character.characterId)??p.pcs[0],enemy=p.enemies.find(x=>!x.defeated);assert(pc&&enemy&&pc.position&&enemy.position,`S${sessionNo} combatants missing`);
  const dx=enemy.position.xFeet-pc.position.xFeet,dy=enemy.position.yFeet-pc.position.yFeet,d=Math.hypot(dx,dy);if(d>5+1e-9){const travel=d-5,dest={xFeet:pc.position.xFeet+dx/d*travel,yFeet:pc.position.yFeet+dy/d*travel};const move=await combat.movePc({campaignId,pcId:pc.actorId,...dest});assert(move.status==='OK',`S${sessionNo} PC move: ${move.status}`);pcActions++;}
  p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));const liveEnemy=p.enemies.find(x=>!x.defeated);if(liveEnemy&&p.active&&p.activeSideId==='PC'){
    const attack=await combat.attackNpc({campaignId,pcId:pc.actorId,npcId:liveEnemy.actorId,weaponId:'UNARMED'});assert(attack.status==='OK',`S${sessionNo} PC attack: ${attack.status}`);pcActions++;assert(Number.isInteger(attack.resolution?.damage),`S${sessionNo} attack resolution missing`);
  }
  p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));if(p.active&&p.activeSideId==='PC'){const end=await combat.endPcSide({campaignId});assert(end.status==='OK',`S${sessionNo} end PC: ${end.status}`);pcActions++;p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  if(p.active&&p.activeSideId==='ENEMY'){const e=await combat.resolveEnemySide({campaignId});assert(['OK','PC_SIDE_INCAPACITATED'].includes(e.status),`S${sessionNo} enemy reply: ${e.status}`);enemyActions+=(e.actions??[]).length;p=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));}
  const loaded=persistence.loadCampaign(campaignId);assert(loaded.status==='OK',`S${sessionNo} final state missing`);const state=JSON.parse(loaded.stateJson);assert(state.revision===loaded.revision,`S${sessionNo} revision mismatch`);const events=persistence.listEvents(campaignId);assert(events.status==='OK'&&events.events.length>=loaded.revision,`S${sessionNo} events/revision mismatch`);assert(pcActions>=2,`S${sessionNo} insufficient player actions`);assert(enemyActions>=1||state.encounterState?.combat?.outcome==='PC_VICTORY',`S${sessionNo} no enemy turn evidence`);
  reports.push({sessionNo,revision:loaded.revision,eventCount:events.events.length,pcActions,enemyActions,round:state.encounterState?.combat?.round??null,active:state.encounterState?.combat?.active===true,outcome:state.encounterState?.combat?.outcome??null});
}
assert(reports.length===33,'33 play sessions not completed');assert(reports.every(r=>r.pcActions>=2),'a play session lacked real PC actions');const totalEvents=reports.reduce((n,r)=>n+r.eventCount,0),totalPcActions=reports.reduce((n,r)=>n+r.pcActions,0),totalEnemyActions=reports.reduce((n,r)=>n+r.enemyActions,0);console.log(JSON.stringify({status:'PASS',sessions:reports.length,totalEvents,totalPcActions,totalEnemyActions,reports},null,2));console.log(`WWN 33 full play sessions: PASS • ${reports.length} sessions • ${totalPcActions} PC actions • ${totalEnemyActions} enemy actions • ${totalEvents} persisted events`);
