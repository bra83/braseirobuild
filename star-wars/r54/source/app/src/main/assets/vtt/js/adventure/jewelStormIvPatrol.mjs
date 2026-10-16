/** P223: SWE09 pp.90–91,95 — four separate Storm IV cloud cars, no teleportation.
 * The printed Storm IV and Bespin Defense Force Pilot stat blocks were read
 * visually on PDF page 96 (printed p.95). No invented NPC pilot rank: a lone
 * minion has zero group-only Gunnery/Piloting ranks.
 */
import {createVehicleFromProfile,vehicleStatus} from '../rules/vehicles.mjs';
import {diceFacesForTests} from '../rules/narrativeDice.mjs';
import {validateJewelArendPatrols} from './jewelArendPatrols.mjs';
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.90–91,95, Storm IV / Bespin Defense Force Pilot';
const PREFIX='JEWEL_STORM_IV_';
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
export const STORM_IV_IDS=Object.freeze([1,2,3,4].map(n=>`jewel-venture-storm-iv-${n}`));
export const BESPIN_PILOT=Object.freeze({kind:'minion',brawn:2,agility:3,intellect:2,cunning:2,willpower:2,presence:2,
 soak:2,woundThreshold:12,defense:{melee:0,ranged:0},groupOnlySkills:Object.freeze(['frialdad','artilleria','pilotar_vehiculo_planetario']),
 soloSkillRank:0,crew:['pilot','gunner'],source:'SWE09 p.95 / PDF p.96'});
const FIELDS={DEPLOY:['playerVehicleId','vehicleIds','range','gmPositionsConfirmed','gmPilotsPresentConfirmed'],
 ENGAGE:['vehicleId','range','gmRangeConfirmed'],
 OUTCOME:['vehicleId','outcome','gmOutcomeConfirmed','hullDamage','hullThreshold','attackLogCount','check','gmDifficultyConfirmed'],
 CLEARED:['outcomeReceiptIds','gmAllFourResolvedConfirmed']};
const TYPES=Object.keys(FIELDS);
function receipts(p){return Object.entries(p?.operations?.receipts??{}).filter(([,v])=>v?.kind?.startsWith(PREFIX));}
export function validateStormIvEvasionCheck(check){
 if(!check||typeof check!=='object'||!check.pool||!Array.isArray(check.faces)||!check.faces.length||
   !Number.isSafeInteger(check.netSuccess)||!Number.isSafeInteger(check.netAdvantage)||
   !Number.isSafeInteger(check.triumph)||!Number.isSafeInteger(check.despair)||check.success!==true)
   throw Error('Evasão exige rolagem real de Pilotagem bem-sucedida.');
 const pool=check.pool;
 for(const key of ['ability','proficiency','difficulty','challenge','boost','setback']){
  if(!Number.isSafeInteger(pool[key])||pool[key]<0||pool[key]>10||check.faces.filter(x=>x.type===key).length!==pool[key])
   throw Error('Pool da evasão diverge das faces.');
 }
 if(pool.difficulty+pool.challenge<1||pool.difficulty+pool.challenge>5)throw Error('Dificuldade da evasão não foi adjudicada.');
 const sums={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const x of check.faces){
  if(!Number.isSafeInteger(x.index)||x.index<0||!equal(x.face,diceFacesForTests[x.type]?.[x.index]))throw Error('Faces de evasão adulteradas.');
  for(const [key,v] of Object.entries(x.face)){if(!(key in sums)||!Number.isSafeInteger(v))throw Error('Símbolo de evasão inválido.');sums[key]+=v;}
 }
 if(check.netSuccess!==sums.success+sums.triumph-sums.failure-sums.despair||
    check.netAdvantage!==sums.advantage-sums.threat||check.triumph!==sums.triumph||check.despair!==sums.despair||check.netSuccess<=0)
  throw Error('Resultados da evasão não conferem com os dados.');
}
function arrival(p){const state=validateJewelArendPatrols(p);return state?.venture?.arrival??null;}
function base(p){if(p?.id!=='jewel-of-yavin'||!arrival(p))throw Error('Quatro cloud cars exigem chegada física oficial já confirmada.');}
export function replayJewelStormIvPatrol(p){
 const list=receipts(p);if(!list.length)return null;
 base(p);let deploy=null,engaged=null,cleared=null;const outcomes=[],seenIds=new Set();
 for(const [id,v] of list){const type=v.kind.slice(PREFIX.length);
  if(!ID(id)||id!==v.id||seenIds.has(id)||!TYPES.includes(type)||v.version!==1||v.source!==SOURCE||
   Object.keys(v).some(k=>!['id','kind','version','source',...FIELDS[type]].includes(k)))throw Error('Recibo Storm IV inválido ou duplicado.');
  seenIds.add(id);
  if(type==='DEPLOY'){
   if(deploy||v.gmPositionsConfirmed!==true||v.gmPilotsPresentConfirmed!==true||!ID(v.playerVehicleId)||
    STORM_IV_IDS.includes(v.playerVehicleId)||!equal(v.vehicleIds,STORM_IV_IDS)||
    !['Bocajarro','Curto','Médio','Longo','Extremo'].includes(v.range))throw Error('Implantação Storm IV adulterada.');
   deploy=clone(v);
  }else if(type==='ENGAGE'){
   if(!deploy||cleared||engaged||!STORM_IV_IDS.includes(v.vehicleId)||
    outcomes.some(o=>o.vehicleId===v.vehicleId)||v.gmRangeConfirmed!==true||
    !['Bocajarro','Curto','Médio','Longo','Extremo'].includes(v.range))throw Error('Encontro Storm IV inválido ou repetido.');
   engaged=clone(v);
  }else if(type==='OUTCOME'){
   if(!engaged||v.vehicleId!==engaged.vehicleId||v.gmOutcomeConfirmed!==true||
    !['DISABLED','EVADED'].includes(v.outcome)||outcomes.some(o=>o.vehicleId===v.vehicleId))
    throw Error('Desfecho Storm IV sem encontro físico.');
   if(v.outcome==='DISABLED'){
    if(v.check!==null||v.gmDifficultyConfirmed!==false||!Number.isSafeInteger(v.hullDamage)||
     !Number.isSafeInteger(v.hullThreshold)||v.hullDamage<=v.hullThreshold||
     !Number.isSafeInteger(v.attackLogCount)||v.attackLogCount<1)throw Error('Cloud car não foi realmente inutilizado.');
   }else{
    if(v.hullDamage!==null||v.hullThreshold!==null||v.attackLogCount!==null||v.gmDifficultyConfirmed!==true)
     throw Error('Evasão Storm IV sem adjudicação do Mestre.');
    validateStormIvEvasionCheck(v.check);
   }
   outcomes.push(clone(v));engaged=null;
  }else{
   if(!deploy||engaged||cleared||outcomes.length!==4||v.gmAllFourResolvedConfirmed!==true||
     !equal(v.outcomeReceiptIds,outcomes.map(o=>o.id)))throw Error('Patrulha Storm IV não foi resolvida por completo.');
   cleared=clone(v);
  }
 }
 if(!deploy)throw Error('Resultados Storm IV sem implantação.');
 return {deploy,engaged,outcomes,cleared};
}
export function validateJewelStormIvPatrol(p,vehicles=null){
 const expected=replayJewelStormIvPatrol(p);
 if(!equal(expected,p?.operations?.stormIvPatrol??null))throw Error('Estado da patrulha Storm IV diverge dos recibos.');
 if(expected&&vehicles){
  const list=expected.deploy.vehicleIds;
  for(const id of list){const v=vehicles.vehicles?.[id];
   if(!v||v.key!=='storm_iv_cloud_car'||v.silhouette!==2||v.speedMax!==4||v.handling!==1||
     v.armor!==2||v.hullThreshold!==5||v.systemStrainThreshold!==7||
     !equal(v.wingGuardPilot,BESPIN_PILOT)||v.wingGuardPatrolId!==expected.deploy.id)
    throw Error('Ficha oficial Storm IV ou piloto alterados no save.');
  }
  for(const out of expected.outcomes){
   const v=vehicles.vehicles[out.vehicleId];
   if(out.outcome==='DISABLED'){
    const logs=(vehicles.combat?.log??[]).filter(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===out.vehicleId&&x.hit===true&&x.damage>0&&x.damageType==='hull');
    if(!vehicleStatus(v).hullDisabled||v.hullDamage<out.hullDamage||logs.length<out.attackLogCount)
     throw Error('Inutilização Storm IV não corresponde ao combate real.');
   }
  }
  if(expected.engaged&&(vehicles.combat?.targetVehicleId!==expected.engaged.vehicleId||!vehicles.combat?.active))
   throw Error('Combate Storm IV ativo não corresponde ao recibo.');
 }
 return expected;
}
export function deployJewelStormIvPatrol(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),p=w.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit')throw Error('Fora da perseguição de Cloud City.');
 base(p);
 if(replayJewelStormIvPatrol(p)||vs.combat?.active||!ID(details.actionId)||p.operations.receipts?.[details.actionId]||
  details.gmPositionsConfirmed!==true||details.gmPilotsPresentConfirmed!==true||
  !['Bocajarro','Curto','Médio','Longo','Extremo'].includes(details.range)||
  !vs.activeVehicleId||!vs.vehicles[vs.activeVehicleId]||STORM_IV_IDS.includes(vs.activeVehicleId)||
  STORM_IV_IDS.some(id=>vs.vehicles[id]))throw Error('Confirme nave do jogador, posições físicas, pilotos e quatro veículos distintos.');
 const r={id:details.actionId,kind:PREFIX+'DEPLOY',version:1,source:SOURCE,
  playerVehicleId:vs.activeVehicleId,vehicleIds:[...STORM_IV_IDS],range:details.range,
  gmPositionsConfirmed:true,gmPilotsPresentConfirmed:true};
 for(const [i,id] of STORM_IV_IDS.entries()){
  const v=createVehicleFromProfile('storm_iv_cloud_car',{id,name:`Storm IV — voo ${Math.floor(i/2)+1}, carro ${(i%2)+1}`});
  v.wingGuardPilot=clone(BESPIN_PILOT);v.wingGuardPatrolId=r.id;v.speed=4;
  vs.vehicles[id]=v;
 }
 p.operations.receipts[r.id]=clone(r);p.operations.stormIvPatrol=replayJewelStormIvPatrol(p);
 validateJewelStormIvPatrol(p,vs);
 return {world:w,vehicles:vs,adjudication:r};
}
export function beginJewelStormIvCombat(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),p=w.publishedAdventure;
 const state=validateJewelStormIvPatrol(p,vs);
 if(!state||state.engaged||state.cleared||vs.combat?.active||!STORM_IV_IDS.includes(details.vehicleId)||
  state.outcomes.some(x=>x.vehicleId===details.vehicleId)||!ID(details.actionId)||p.operations.receipts?.[details.actionId]||
  details.gmRangeConfirmed!==true||!['Bocajarro','Curto','Médio','Longo','Extremo'].includes(details.range)||
  vs.activeVehicleId!==state.deploy.playerVehicleId)throw Error('Combate Storm IV exige alvo físico distinto e alcance confirmado.');
 const r={id:details.actionId,kind:PREFIX+'ENGAGE',version:1,source:SOURCE,vehicleId:details.vehicleId,
  range:details.range,gmRangeConfirmed:true};
 vs.combat={...vs.combat,active:true,round:1,targetVehicleId:details.vehicleId,range:details.range,
  actionUsed:false,maneuversUsed:0,lastAttack:null,pendingTargetCritical:null,pendingIncomingCritical:null,
  pendingVehicleSpend:null,npcAttackResolvedRound:null,lastIncomingAttack:null,hullDamageControlAttempts:{},
  log:[...(vs.combat.log??[]),{type:'JEWEL_STORM_IV_START',vehicleId:details.vehicleId,round:1}]};
 p.operations.receipts[r.id]=clone(r);p.operations.stormIvPatrol=replayJewelStormIvPatrol(p);
 validateJewelStormIvPatrol(p,vs);return {world:w,vehicles:vs,adjudication:r};
}
export function resolveJewelStormIvCombat(world,vehicles,details={}){
 const w=clone(world),vs=clone(vehicles),p=w.publishedAdventure,state=validateJewelStormIvPatrol(p,vs);
 const vehicleId=state?.engaged?.vehicleId,v=vs.vehicles?.[vehicleId];
 if(!v||!vs.combat.active||vs.combat.targetVehicleId!==vehicleId||!ID(details.actionId)||p.operations.receipts?.[details.actionId]||
  details.gmOutcomeConfirmed!==true)throw Error('Encontro Storm IV real ainda não foi iniciado.');
 let check=null,hullDamage=null,hullThreshold=null,attackLogCount=null,gmDifficultyConfirmed=false;
 if(details.outcome==='DISABLED'){
  const logs=(vs.combat.log??[]).filter(x=>x.type==='ATAQUE_VEICULO'&&x.targetId===vehicleId&&x.hit===true&&x.damage>0&&x.damageType==='hull');
  if(!vehicleStatus(v).hullDisabled||!logs.length)throw Error('Cloud car precisa sofrer dano veicular real até ser inutilizado.');
  hullDamage=v.hullDamage;hullThreshold=v.hullThreshold;attackLogCount=logs.length;
 }else if(details.outcome==='EVADED'){
  if(details.gmDifficultyConfirmed!==true)throw Error('Mestre precisa adjudicar a dificuldade da evasão.');
  validateStormIvEvasionCheck(details.check);check=clone(details.check);gmDifficultyConfirmed=true;
 }else throw Error('Desfecho Storm IV não é oficial.');
 const r={id:details.actionId,kind:PREFIX+'OUTCOME',version:1,source:SOURCE,vehicleId,outcome:details.outcome,
  gmOutcomeConfirmed:true,hullDamage,hullThreshold,attackLogCount,check,gmDifficultyConfirmed};
 vs.combat={...vs.combat,active:false,round:0,targetVehicleId:null,actionUsed:false,maneuversUsed:0,
  pendingTargetCritical:null,pendingIncomingCritical:null,pendingVehicleSpend:null,npcAttackResolvedRound:null,
  log:[...(vs.combat.log??[]),{type:'JEWEL_STORM_IV_OUTCOME',vehicleId,outcome:details.outcome}]};
 p.operations.receipts[r.id]=clone(r);p.operations.stormIvPatrol=replayJewelStormIvPatrol(p);
 validateJewelStormIvPatrol(p,vs);return {world:w,vehicles:vs,adjudication:r};
}
export function clearJewelStormIvPatrol(world,vehicles,details={}){
 const w=clone(world),p=w.publishedAdventure,state=validateJewelStormIvPatrol(p,vehicles);
 if(!state||state.engaged||state.cleared||state.outcomes.length!==4||!ID(details.actionId)||
  p.operations.receipts?.[details.actionId]||details.gmAllFourResolvedConfirmed!==true)
  throw Error('Quatro carros precisam ser derrotados ou evadidos com provas individuais.');
 const r={id:details.actionId,kind:PREFIX+'CLEARED',version:1,source:SOURCE,
  outcomeReceiptIds:state.outcomes.map(x=>x.id),gmAllFourResolvedConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.stormIvPatrol=replayJewelStormIvPatrol(p);
 validateJewelStormIvPatrol(p,vehicles);return {world:w,adjudication:r};
}
