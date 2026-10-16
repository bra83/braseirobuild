/** P231 — SWE09 The Jewel of Yavin p.94: at least twelve Wing Guard Storm IV
 * cloud cars pursue after takeoff, four at a time. This implements the first
 * twelve, in three independently GM-confirmed squadrons; it does NOT infer
 * aircraft arrival, NPC actions, movement, defeat, or XP.
 */
import {createVehicleFromProfile,vehicleStatus} from '../rules/vehicles.mjs';
import {BESPIN_PILOT,validateStormIvEvasionCheck,STORM_IV_IDS} from './jewelStormIvPatrol.mjs';
import {replayJewelBespinDeparture} from './jewelBespinDeparture.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SRC='SWE09 The Jewel of Yavin, p.94, Flight over Fight / Escape to the Clouds';
const PRE='JEWEL_STARPORT_CLOUD_';
const idOK=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const RANGES=['Bocajarro','Curto','Médio','Longo','Extremo'];
export const STARPORT_CLOUD_IDS=Object.freeze(Array.from({length:12},(_,i)=>`jewel-starport-storm-iv-${i+1}`));
const waveIds=n=>STARPORT_CLOUD_IDS.slice((n-1)*4,n*4);
const FIELDS={ARRIVAL:['wave','vehicleIds','playerVehicleId','range','atMinute','gmAirborneConfirmed','gmSquadronArrivedConfirmed','gmPositionsConfirmed','gmDistinctPilotsConfirmed'],
 ENGAGE:['wave','vehicleId','range','atMinute','gmRangeConfirmed'],
 OUTCOME:['wave','vehicleId','outcome','atMinute','gmOutcomeConfirmed','hullDamage','hullThreshold','attackLogCount','check','gmDifficultyConfirmed'],
 CLEARED:['wave','outcomeReceiptIds','atMinute','gmFourResolvedConfirmed']};
const types=Object.keys(FIELDS);
const records=p=>Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind?.startsWith(PRE));
const minute=w=>{const m=w?.clock?.elapsedMinutes??0;if(!Number.isSafeInteger(m)||m<0)throw Error('Relógio persistido inválido.');return m;};
function base(world,vehicles,details){const p=world?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
  !idOK(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('Ação de perseguição fora da cena ou recibo duplicado.');
 const boarding=replayJewelBespinDeparture(p).board;
 if(!boarding||replayJewelBespinDeparture(p).escape||!vehicles?.vehicles?.[boarding.shipId]||
   vehicles.activeVehicleId!==boarding.shipId)throw Error('Perseguição aérea exige embarque comprovado, nave ativa e ausência de salto final.');
 return {p,boarding};
}
export function replayJewelStarportCloudCars(p){
 const entries=records(p);if(!entries.length)return null;
 if(p?.id!=='jewel-of-yavin')throw Error('Recibos de perseguição sem aventura.');
 const board=replayJewelBespinDeparture(p).board;
 if(!board)throw Error('Perseguição sem embarque real.');
 const waves=[],seen=new Set();let engaged=null,prevMinute=board.atMinute;
 for(const [id,r] of entries){
  const type=r.kind.slice(PRE.length);
  if(!idOK(id)||id!==r.id||seen.has(id)||!types.includes(type)||r.version!==1||r.source!==SRC||
    Object.keys(r).some(k=>!['id','kind','version','source',...FIELDS[type]].includes(k))||
    !Number.isSafeInteger(r.atMinute)||r.atMinute<prevMinute)throw Error('Recibo da perseguição aérea inválido ou fora de ordem.');
  seen.add(id);prevMinute=r.atMinute;
  if(!Number.isSafeInteger(r.wave)||r.wave<1||r.wave>3)throw Error('Esquadrilha fora da fonte oficial.');
  if(type==='ARRIVAL'){
   if(engaged||r.wave!==waves.length+1||r.wave>1&&!waves.at(-1)?.cleared||
     !eq(r.vehicleIds,waveIds(r.wave))||r.playerVehicleId!==board.shipId||!RANGES.includes(r.range)||
     r.gmAirborneConfirmed!==true||r.gmSquadronArrivedConfirmed!==true||
     r.gmPositionsConfirmed!==true||r.gmDistinctPilotsConfirmed!==true)throw Error('Esquadrilha sem chegada física e tripulação confirmadas.');
   waves.push({arrival:clone(r),outcomes:[],cleared:null});
  }else{
   const w=waves.at(-1);
   if(!w||r.wave!==waves.length||w.cleared)throw Error('Evento aéreo sem esquadrilha ativa.');
   if(type==='ENGAGE'){
    if(engaged||!w.arrival.vehicleIds.includes(r.vehicleId)||w.outcomes.some(o=>o.vehicleId===r.vehicleId)||
      !RANGES.includes(r.range)||r.gmRangeConfirmed!==true)throw Error('Alvo aéreo não está fisicamente disponível.');
    engaged=clone(r);
   }else if(type==='OUTCOME'){
    if(!engaged||engaged.vehicleId!==r.vehicleId||r.gmOutcomeConfirmed!==true||
      !['DISABLED','EVADED'].includes(r.outcome))throw Error('Desfecho aéreo sem combate individual.');
    if(r.outcome==='DISABLED'){
     if(r.check!==null||r.gmDifficultyConfirmed!==false||!Number.isSafeInteger(r.hullDamage)||
      !Number.isSafeInteger(r.hullThreshold)||r.hullDamage<=r.hullThreshold||
      !Number.isSafeInteger(r.attackLogCount)||r.attackLogCount<1)throw Error('Destruição aérea sem dano veicular real.');
    }else{
     if(r.hullDamage!==null||r.hullThreshold!==null||r.attackLogCount!==null||r.gmDifficultyConfirmed!==true)
      throw Error('Evasão sem dificuldade confirmada.');
     validateStormIvEvasionCheck(r.check);
    }
    w.outcomes.push(clone(r));engaged=null;
   }else{
    if(engaged||w.outcomes.length!==4||r.gmFourResolvedConfirmed!==true||
       !eq(r.outcomeReceiptIds,w.outcomes.map(o=>o.id)))throw Error('Esquadrilha não foi resolvida integralmente.');
    w.cleared=clone(r);
   }
  }
 }
 if(waves.length>3)throw Error('Número de esquadrilhas inválido.');
 return {waves,engaged,allTwelveResolved:waves.length===3&&waves.every(w=>Boolean(w.cleared))};
}
export function validateJewelStarportCloudCars(p,world=null,vehicles=null){
 const replay=replayJewelStarportCloudCars(p);
 if(!eq(replay,p?.operations?.starportCloudCars??null))throw Error('Estado de perseguição aérea diverge dos recibos.');
 if(!replay)return null;
 if(world){const now=minute(world);if(records(p).some(([,r])=>r.atMinute>now))throw Error('Recibo aéreo no futuro.');}
 if(vehicles){
  for(const w of replay.waves){for(const id of w.arrival.vehicleIds){const v=vehicles.vehicles?.[id];
   if(STORM_IV_IDS.includes(id)||!v||v.key!=='storm_iv_cloud_car'||v.silhouette!==2||v.speedMax!==4||
     v.handling!==1||v.armor!==2||v.hullThreshold!==5||v.systemStrainThreshold!==7||
     v.starportWingArrivalId!==w.arrival.id||!eq(v.wingGuardPilot,BESPIN_PILOT))
     throw Error('Storm IV do porto alterado ou confundido com os carros do Venture.');
  }
  for(const o of w.outcomes){if(o.outcome==='DISABLED'){
   const v=vehicles.vehicles[o.vehicleId];const logs=(vehicles.combat?.log??[]).filter(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===o.vehicleId&&x.hit===true&&x.damage>0&&x.damageType==='hull');
   if(!vehicleStatus(v).hullDisabled||v.hullDamage<o.hullDamage||logs.length<o.attackLogCount)throw Error('Dano do carro não confere com os ataques.');
  }} }
  if(replay.engaged&&(!vehicles.combat?.active||vehicles.combat.targetVehicleId!==replay.engaged.vehicleId))
   throw Error('Combate veicular não corresponde ao encontro aéreo.');
 }
 return replay;
}
function update(w,vs){const p=w.publishedAdventure;p.operations.starportCloudCars=replayJewelStarportCloudCars(p);validateJewelStarportCloudCars(p,w,vs);}
export function arriveJewelStarportSquadron(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),{p,boarding}=base(w,vs,details),s=validateJewelStarportCloudCars(p,w,vs);
 const wave=(s?.waves.length??0)+1;
 if(wave>3||s?.engaged||s?.waves.at(-1)?.cleared===null||vs.combat?.active||
   details.gmAirborneConfirmed!==true||details.gmSquadronArrivedConfirmed!==true||
   details.gmPositionsConfirmed!==true||details.gmDistinctPilotsConfirmed!==true||!RANGES.includes(details.range)||
   waveIds(wave).some(id=>vs.vehicles[id]))throw Error('Confirme decolagem e chegada física da próxima esquadrilha de quatro.');
 const r={id:details.actionId,kind:PRE+'ARRIVAL',version:1,source:SRC,wave,vehicleIds:waveIds(wave),
  playerVehicleId:boarding.shipId,range:details.range,atMinute:minute(w),gmAirborneConfirmed:true,
  gmSquadronArrivedConfirmed:true,gmPositionsConfirmed:true,gmDistinctPilotsConfirmed:true};
 for(const [i,id] of r.vehicleIds.entries()){
  const v=createVehicleFromProfile('storm_iv_cloud_car',{id,name:`Storm IV · porto · esquadrilha ${wave}, carro ${i+1}`});
  v.wingGuardPilot=clone(BESPIN_PILOT);v.starportWingArrivalId=r.id;v.speed=4;vs.vehicles[id]=v;
 }
 p.operations.receipts[r.id]=clone(r);update(w,vs);return {world:w,vehicles:vs,adjudication:r};
}
export function beginJewelStarportCloudCombat(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),{p}=base(w,vs,details),s=validateJewelStarportCloudCars(p,w,vs),active=s?.waves.at(-1);
 if(!active||active.cleared||s.engaged||vs.combat?.active||!active.arrival.vehicleIds.includes(details.vehicleId)||
  active.outcomes.some(o=>o.vehicleId===details.vehicleId)||!RANGES.includes(details.range)||details.gmRangeConfirmed!==true)
  throw Error('Selecione um Storm IV presente e confirme seu alcance real.');
 const r={id:details.actionId,kind:PRE+'ENGAGE',version:1,source:SRC,wave:active.arrival.wave,
  vehicleId:details.vehicleId,range:details.range,atMinute:minute(w),gmRangeConfirmed:true};
 vs.combat={...vs.combat,active:true,round:1,targetVehicleId:r.vehicleId,range:r.range,actionUsed:false,
  maneuversUsed:0,lastAttack:null,pendingTargetCritical:null,pendingIncomingCritical:null,pendingVehicleSpend:null,
  npcAttackResolvedRound:null,lastIncomingAttack:null,hullDamageControlAttempts:{},
  log:[...(vs.combat?.log??[]),{type:'JEWEL_STARPORT_CLOUD_START',vehicleId:r.vehicleId,round:1}]};
 p.operations.receipts[r.id]=clone(r);update(w,vs);return {world:w,vehicles:vs,adjudication:r};
}
export function resolveJewelStarportCloudCombat(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),{p}=base(w,vs,details),s=validateJewelStarportCloudCars(p,w,vs),engaged=s?.engaged;
 if(!engaged||!vs.combat?.active||vs.combat.targetVehicleId!==engaged.vehicleId||details.gmOutcomeConfirmed!==true)
  throw Error('Combate aéreo individual ainda não foi confirmado.');
 const v=vs.vehicles[engaged.vehicleId];let check=null,hullDamage=null,hullThreshold=null,attackLogCount=null,gmDifficultyConfirmed=false;
 if(details.outcome==='DISABLED'){
  const logs=(vs.combat.log??[]).filter(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===engaged.vehicleId&&x.hit===true&&x.damage>0&&x.damageType==='hull');
  if(!vehicleStatus(v).hullDisabled||!logs.length)throw Error('Carro precisa ser inutilizado por ataques veiculares reais.');
  hullDamage=v.hullDamage;hullThreshold=v.hullThreshold;attackLogCount=logs.length;
 }else if(details.outcome==='EVADED'){
  if(details.gmDifficultyConfirmed!==true)throw Error('Dificuldade da evasão exige confirmação do Mestre.');
  validateStormIvEvasionCheck(details.check);check=clone(details.check);gmDifficultyConfirmed=true;
 }else throw Error('Desfecho aéreo não suportado.');
 const r={id:details.actionId,kind:PRE+'OUTCOME',version:1,source:SRC,wave:engaged.wave,vehicleId:engaged.vehicleId,
  outcome:details.outcome,atMinute:minute(w),gmOutcomeConfirmed:true,hullDamage,hullThreshold,attackLogCount,check,gmDifficultyConfirmed};
 vs.combat={...vs.combat,active:false,round:0,targetVehicleId:null,actionUsed:false,maneuversUsed:0,
  pendingTargetCritical:null,pendingIncomingCritical:null,pendingVehicleSpend:null,npcAttackResolvedRound:null,
  log:[...(vs.combat.log??[]),{type:'JEWEL_STARPORT_CLOUD_OUTCOME',vehicleId:r.vehicleId,outcome:r.outcome}]};
 p.operations.receipts[r.id]=clone(r);update(w,vs);return {world:w,vehicles:vs,adjudication:r};
}
export function clearJewelStarportSquadron(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),{p}=base(w,vs,details),s=validateJewelStarportCloudCars(p,w,vs),wave=s?.waves.at(-1);
 if(!wave||wave.cleared||s.engaged||vs.combat?.active||wave.outcomes.length!==4||details.gmFourResolvedConfirmed!==true)
  throw Error('Quatro carros devem ter desfechos individuais reais antes de liberar a próxima esquadrilha.');
 const r={id:details.actionId,kind:PRE+'CLEARED',version:1,source:SRC,wave:wave.arrival.wave,
  outcomeReceiptIds:wave.outcomes.map(o=>o.id),atMinute:minute(w),gmFourResolvedConfirmed:true};
 p.operations.receipts[r.id]=clone(r);update(w,vs);return {world:w,adjudication:r};
}
