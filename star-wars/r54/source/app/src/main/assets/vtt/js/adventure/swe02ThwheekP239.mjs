// SWE02, printed p.12 (PDF p.12): Thwheek's real G1-M4-C interception.
// The authoritative damage/roll engine remains rules/vehicles.mjs; this
// module only enforces the adventure's causality and records its outcome.
import {createVehicleFromProfile,vehicleStatus} from '../rules/vehicles.mjs';
const clone=v=>structuredClone(v);
export const THWHEEK_ID='swe02-thwheek-dunelizard';
export const THWHEEK_CONTEXT='swe02-thwheek-p239';
export const THWHEEK_SOURCE=Object.freeze({productCode:'SWE02',printedPage:12,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
function progress(world){
 const p=world?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE')throw Error('SWE02: aventura não está ativa.');
 return p;
}
function context(vehicles){
 const c=vehicles?.combat;
 if(c?.publishedVehicleContext?.id!==THWHEEK_CONTEXT||c.targetVehicleId!==THWHEEK_ID)
  throw Error('SWE02: combate de Thwheek não pertence a esta aventura.');
 return c;
}
export function beginThwheekInterception(world,vehicles,{initiative}={}){
 const w=clone(world),v=clone(vehicles),p=progress(w);
 if(p.currentNodeId!=='thwheek-orbit'||p.flags?.shipThwheekInterceptionRequired!==true||p.flags?.shipThwheekCombatStarted)
  throw Error('SWE02: interceptação não confirmada ou já iniciada.');
 if(v.combat?.active)throw Error('SWE02: outro combate veicular ainda ativo.');
 if(!initiative||initiative.source?.printedPage!==12||!Array.isArray(initiative.slots))
  throw Error('SWE02 p.12: iniciativa de Cool obrigatória antes do combate.');
 const ship=v.vehicles?.[v.activeVehicleId];
 if(!ship||ship.key!=='yt1300'||String(ship.name||'').trim().toLowerCase()!=='krayt fang')
  throw Error('SWE02: selecione a Krayt Fang YT-1300 real; o motor não cria uma nave fictícia.');
 if(vehicleStatus(ship).destroyed||vehicleStatus(ship).hullDisabled)throw Error('SWE02: nave indisponível para interceptação.');
 if(v.vehicles?.[THWHEEK_ID])throw Error('SWE02: Dunelizard já existe; não duplicar combate.');
 const enemy=createVehicleFromProfile('swe02_thwheek_dunelizard',{id:THWHEEK_ID});
 enemy.speed=4;enemy.roundEffects.stayOnTarget=true;
 v.vehicles[enemy.id]=enemy;
 v.combat={...v.combat,active:true,round:1,targetVehicleId:enemy.id,range:'Curto',actionUsed:false,maneuversUsed:0,
   swe02Initiative:clone(initiative),
   npcAttackResolvedRound:null,lastAttack:null,lastIncomingAttack:null,pendingTargetCritical:null,pendingIncomingCritical:null,
   pendingVehicleSpend:null,publishedVehicleContext:{id:THWHEEK_CONTEXT,adventureId:p.id,source:THWHEEK_SOURCE,
    playerVehicleId:ship.id,playerInitialHull:ship.hullDamage,enemyInitialHull:enemy.hullDamage,firstHitRecorded:false},
   log:[...(v.combat?.log||[]),{type:'SWE02_THWHEEK_START',round:1,playerVehicleId:ship.id,targetId:enemy.id,printedPage:12}]};
 p.flags={...p.flags,shipThwheekCombatStarted:true,shipThwheekProfileProvisional:true};
 p.shipThwheekStart={source:THWHEEK_SOURCE,playerVehicleId:ship.id,enemyVehicleId:enemy.id,
  playerInitialHull:ship.hullDamage,enemyInitialHull:enemy.hullDamage};
 return {world:w,vehicles:v};
}
export function thwheekTactics(vehicles){
 const v=clone(vehicles),c=context(v),enemy=v.vehicles[THWHEEK_ID];
 if(!c.active||!enemy)throw Error('SWE02: combate veicular encerrado.');
 const hit=Boolean(c.publishedVehicleContext.firstHitRecorded)||
   (c.log||[]).some(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===THWHEEK_ID&&x.hit===true);
 enemy.roundEffects={...enemy.roundEffects,stayOnTarget:!hit,evasive:hit};
 c.publishedVehicleContext.firstHitRecorded=hit;
 return v;
}
export function resolveThwheekInterception(world,vehicles,{pursue=false}={}){
 const w=clone(world),v=clone(vehicles),p=progress(w),c=context(v);
 if(!c.active||p.currentNodeId!=='thwheek-engagement'||!p.flags?.shipThwheekCombatStarted||p.flags?.shipThwheekResolved)
  throw Error('SWE02: confronto não está aberto.');
 const enemy=v.vehicles?.[THWHEEK_ID],ship=v.vehicles?.[c.publishedVehicleContext.playerVehicleId];
 if(!enemy||!ship||v.activeVehicleId!==ship.id)throw Error('SWE02: identidade da nave alterada durante o combate.');
 const hits=(c.log||[]).filter(x=>['ATAQUE_VEICULO','ATAQUE_VEICULAR_INIMIGO'].includes(x.type)&&x.hit===true&&Number(x.damage)>0);
 const outgoing=hits.filter(x=>x.targetId===THWHEEK_ID),incoming=hits.filter(x=>x.targetId===ship.id);
 const playerDisabled=vehicleStatus(ship).hullDisabled,enemyDamage=enemy.hullDamage-c.publishedVehicleContext.enemyInitialHull;
 let outcome;
 if(playerDisabled){if(!incoming.length)throw Error('SWE02: queda sem ataque inimigo comprovado.');outcome='KRAYT_FANG_SHOT_DOWN';}
 else if(enemyDamage>enemy.hullThreshold){if(!outgoing.length)throw Error('SWE02: queda de Thwheek sem disparo comprovado.');outcome='THWHEEK_SHOT_DOWN';}
 else if(enemyDamage>=5){if(!outgoing.length)throw Error('SWE02: retirada de Thwheek sem disparo comprovado.');
   if(pursue){c.publishedVehicleContext.pursuitConfirmed=true;return {world:w,vehicles:v,outcome:'PURSUIT_CONTINUES'};}
   outcome='THWHEEK_WITHDREW';
 }else throw Error('SWE02: nem a fuga de Thwheek nem a queda da Krayt Fang foram comprovadas.');
 p.flags={...p.flags,shipThwheekResolved:true,shipThwheekOutcome:outcome,
   shipCrashCrewPending:outcome==='KRAYT_FANG_SHOT_DOWN',shipThwheekSurvives:true};
 p.shipThwheekReceipt={source:THWHEEK_SOURCE,outcome,playerVehicleId:ship.id,enemyVehicleId:enemy.id,
   playerInitialHull:c.publishedVehicleContext.playerInitialHull,playerFinalHull:ship.hullDamage,
   enemyInitialHull:c.publishedVehicleContext.enemyInitialHull,enemyFinalHull:enemy.hullDamage,
   outgoingHits:outgoing.length,incomingHits:incoming.length,round:c.round};
 c.log=[...(c.log||[]),{type:'SWE02_THWHEEK_RESOLVED',outcome,round:c.round}];
 c.active=false;c.round=0;c.actionUsed=false;c.maneuversUsed=0;
 c.publishedVehicleContext={...c.publishedVehicleContext,completed:true,outcome};
 return {world:w,vehicles:v,outcome};
}
export function validateThwheek(p){
 if(p?.id!=='long-arm-of-the-hutt')return true;
 const f=p.flags||{},start=p.shipThwheekStart,receipt=p.shipThwheekReceipt;
 if(f.shipThwheekCombatStarted===true&&(!start||start.source?.printedPage!==12||start.enemyVehicleId!==THWHEEK_ID))
  throw Error('SWE02: início do combate espacial sem recibo oficial.');
 if(f.shipThwheekResolved===true){
  if(!start||!receipt||receipt.source?.printedPage!==12||receipt.enemyVehicleId!==THWHEEK_ID||
    receipt.playerVehicleId!==start.playerVehicleId||receipt.playerInitialHull!==start.playerInitialHull||
    !['THWHEEK_WITHDREW','THWHEEK_SHOT_DOWN','KRAYT_FANG_SHOT_DOWN'].includes(receipt.outcome)||
    receipt.outcome!==f.shipThwheekOutcome||
    (receipt.outcome==='KRAYT_FANG_SHOT_DOWN' ? receipt.incomingHits<1 : receipt.outgoingHits<1))
   throw Error('SWE02: desfecho espacial sem prova de combate.');
 }
 if(f.shipThwheekResolved===true){
  const landed=receipt.outcome!=='KRAYT_FANG_SHOT_DOWN';
  if(f.shipThwheekLandedSafely!==landed)throw Error('SWE02: pouso seguro sem resultado do duelo.');
  if(landed&&receipt.enemyFinalHull-receipt.enemyInitialHull<5)throw Error('SWE02: retirada sem cinco traumas de casco.');
  if(!landed&&receipt.playerFinalHull<=receipt.playerInitialHull)throw Error('SWE02: queda sem dano no casco.');
 }
 if(f.shipCrashCrewResolved===true){
  const crew=p.shipCrashCrewChecks||[],pilot=p.shipCrashPilotCheck,expected=p.shipCrashExpectedCrewCount;
  if(receipt?.outcome!=='KRAYT_FANG_SHOT_DOWN'||!pilot||pilot.source?.printedPage!==12||
     !Number.isInteger(expected)||expected<1||crew.length!==expected||
     new Set(crew.map(x=>x.characterName)).size!==expected||crew.some(x=>x.sourcePage!==12)||
     f.shipCrashCrewPending===true)
   throw Error('SWE02: tripulação declarada segura sem rolagens oficiais completas.');
 }
 if(f.shipThwheekOutcome&&!f.shipThwheekResolved)throw Error('SWE02: resultado espacial forjado.');
 if(f.shipCrashCrewPending===true&&f.shipThwheekOutcome!=='KRAYT_FANG_SHOT_DOWN')throw Error('SWE02: acidente sem derrota espacial.');
 return true;
}
