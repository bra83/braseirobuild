// SWE09 The Jewel of Yavin, Wing Guard profiles, printed pp. 12–13.
// NPC maneuvers are queued explicitly by the GM, then executed on the NPC's
// actual initiative slot. Range is an explicit GM confirmation; the current
// encounter engine does not track NPC-to-NPC geometry.
import {adversaryHealthStatus} from './adversaries.mjs';
const clone=structuredClone;
const allowed=Object.freeze({covering_fire:'bespin_wing_guard_officer',tactical_direction:'bespin_wing_guard_sergeant'});
// A single free maneuver may move one band only within Engaged/Short/Medium.
// Medium <-> Long and Long <-> Extreme require two maneuvers in this ruleset,
// so this grant must NOT silently teleport an NPC across those boundaries.
const shortBands=Object.freeze(['Engajado','Curto','Médio']);
function nextShortBand(from,direction){
  if(!['approach','withdraw'].includes(direction))throw Error('Direção de deslocamento inválida.');
  const idx=shortBands.indexOf(from);
  if(idx<0)throw Error('Alcance Longo/Extremo exige duas manobras; não coberto pela manobra livre única.');
  const next=idx+(direction==='approach'?-1:1);
  if(next<0||next>=shortBands.length)throw Error('A manobra livre não pode ultrapassar um limite de alcance.');
  return shortBands[next];
}
function living(combat,id){const a=(combat.adversaries??[]).find(a=>a.id===id);return a&&!adversaryHealthStatus(a).incapacitated?a:null;}
function assertEncounter(combat){if(!combat?.active||!combat?.encounter?.slots?.length)throw Error('Manobra Wing Guard exige encontro ativo com iniciativa.');}
export function queueWingGuardTactic(combat,{actorId,kind,targetIds,confirmedRange,mode='boost',movementDirection=null,confirmedMovement=false}={}){
  assertEncounter(combat);
  if(!Object.hasOwn(allowed,kind))throw Error('Manobra Wing Guard desconhecida.');
  const actor=living(combat,actorId);
  if(!actor||actor.profileKey!==allowed[kind]||!actor.abilities?.some(a=>a.key===kind))throw Error('Ator não possui a manobra Wing Guard indicada.');
  if(actor.wingGuardManeuverRound===combat.round)throw Error('Manobra Wing Guard já realizada nesta rodada.');
  const ids=targetIds??[];
  if(!Array.isArray(ids)||ids.length<1||ids.length>(kind==='covering_fire'?2:1)||new Set(ids).size!==ids.length)
    throw Error('Alvos Wing Guard inválidos ou duplicados.');
  if(kind==='covering_fire'&&confirmedRange!=='short')throw Error('O Mestre deve confirmar alcance Curto entre os aliados.');
  if(kind==='tactical_direction'&&confirmedRange!=='medium')throw Error('O Mestre deve confirmar alcance Médio entre o sargento e o grupo.');
  if(kind==='tactical_direction'&&!['boost','free_maneuver'].includes(mode))throw Error('Modo de Tactical Direction inválido.');
  if(kind==='covering_fire'&&mode!=='boost')throw Error('Covering Fire não concede manobra livre.');
  if(kind==='tactical_direction'&&mode==='free_maneuver'&&confirmedMovement!==true)
    throw Error('O Mestre deve confirmar o trajeto livre de obstáculos e o alcance do NPC ao grupo.');
  for(const id of ids){const target=living(combat,id);if(!target||id===actorId)throw Error('Alvo aliado inexistente, incapacitado ou é o próprio ator.');
    if(kind==='tactical_direction'&&(target.profileKey!=='bespin_wing_guard_officer'||target.kind!=='minion'))throw Error('Tactical Direction exige grupo minion de oficiais Wing Guard.');
    if(kind==='tactical_direction'&&mode==='free_maneuver'){
      nextShortBand(target.range,movementDirection);
      if(target.wingFreeManeuver)throw Error('O grupo já possui manobra livre pendente.');
    }
  }
  const next=clone(combat);
  const orders=next.wingGuardOrders??[];
  if(orders.some(o=>o.actorId===actorId))throw Error('Já existe manobra Wing Guard pendente para este ator.');
  if(orders.length>=20)throw Error('Fila de manobras Wing Guard cheia.');
  const movement=kind==='tactical_direction'&&mode==='free_maneuver'
    ? {direction:movementDirection,confirmedMovement:true,expectedRange:living(combat,ids[0]).range} : null;
  next.wingGuardOrders=[...orders,{actorId,kind,targetIds:clone(ids),confirmedRange,mode,queuedRound:next.round,...(movement?{movement}: {})}];
  next.log.push({type:'WING_GUARD_MANEUVER_QUEUED',actorId,kind,targetIds:clone(ids),round:next.round});
  return next;
}
export function applyQueuedWingGuardTactic(combat,actorId){
  const order=(combat.wingGuardOrders??[]).find(o=>o.actorId===actorId);
  if(!order)return {combat:clone(combat),applied:false};
  assertEncounter(combat);
  // Revalidate all fields; no forged source, range, or ally may confer a bonus.
  const pending=clone(combat);pending.wingGuardOrders=pending.wingGuardOrders.filter(o=>o.actorId!==actorId);
  // An ally can fall between GM scheduling and the actor's initiative slot.
  // Cancel the maneuver rather than freezing combat or granting a phantom buff.
  const fallen=order.targetIds.filter(id=>{
    const target=(combat.adversaries??[]).find(a=>a.id===id);
    if(!target)throw Error('Alvo Wing Guard inexistente no recibo da manobra.');
    return adversaryHealthStatus(target).incapacitated;
  });
  if(fallen.length){pending.log.push({type:'WING_GUARD_MANEUVER_CANCELLED',actorId,round:pending.round,reason:'ally_incapacitated',targetIds:fallen});return {combat:pending,applied:false,cancelled:true};}
  // If geometry changed after scheduling, cancel rather than moving from a stale band.
  if(order.mode==='free_maneuver'&&(
    order.movement?.confirmedMovement!==true||
    pending.adversaries.find(a=>a.id===order.targetIds[0])?.range!==order.movement.expectedRange)){
    pending.log.push({type:'WING_GUARD_MANEUVER_CANCELLED',actorId,round:pending.round,reason:'range_changed'});
    return {combat:pending,applied:false,cancelled:true};
  }
  queueWingGuardTactic(pending,{actorId,kind:order.kind,targetIds:order.targetIds,
    confirmedRange:order.confirmedRange,mode:order.mode,
    movementDirection:order.movement?.direction,confirmedMovement:order.movement?.confirmedMovement});
  const next=clone(pending);
  const actor=next.adversaries.find(a=>a.id===actorId);
  actor.wingGuardManeuverRound=next.round;
  for(const id of order.targetIds){const target=next.adversaries.find(a=>a.id===id);
    if(order.kind==='covering_fire'){
      // Non-stacking +1 ranged defense through end of next round.
      target.wingCoveringFire={sourceId:actorId,expiresAfterRound:next.round+1};
    }else if(order.mode==='free_maneuver'){
      target.wingFreeManeuver={sourceId:actorId,grantedRound:next.round,
        fromRange:target.range,toRange:nextShortBand(target.range,order.movement.direction),
        direction:order.movement.direction};
    }else{
      // Boost on the minion group's next combat check (not permanent).
      target.wingTacticalBoost={sourceId:actorId,grantedRound:next.round};
    }
  }
  next.log.push({type:'WING_GUARD_MANEUVER_APPLIED',actorId,kind:order.kind,targetIds:clone(order.targetIds),mode:order.mode,round:next.round,expiresAfterRound:order.kind==='covering_fire'?next.round+1:null});
  // SWE09 printed p.13: the minion group performs the free maneuver IMMEDIATELY
  // on the sergeant's initiative, not on its own later slot.
  if(order.mode==='free_maneuver'){
    const moved=consumeWingGuardFreeManeuver(next,order.targetIds[0]);
    return {combat:moved.combat,applied:true,moved:moved.moved,kind:order.kind,targetIds:clone(order.targetIds)};
  }
  return {combat:next,applied:true,kind:order.kind,targetIds:clone(order.targetIds)};
}
export function wingGuardRangedDefense(adversary,round){
  const effect=adversary?.wingCoveringFire;
  return effect&&Number.isSafeInteger(effect.expiresAfterRound)&&round<=effect.expiresAfterRound?1:0;
}
export function consumeWingGuardTacticalBoost(adversary){
  const next=clone(adversary);
  if(!next?.wingTacticalBoost)return {adversary:next,boost:0};
  delete next.wingTacticalBoost;
  return {adversary:next,boost:1};
}

// Consume the one-time grant immediately on the sergeant's NPC initiative.
// Range is measured to the PCs by the encounter engine; the GM separately
// confirmed the sergeant-to-minion range and traversable route when queued.
export function consumeWingGuardFreeManeuver(combat,targetId){
  const next=clone(combat);
  const target=next.adversaries?.find(a=>a.id===targetId);
  if(!target?.wingFreeManeuver)return {combat:next,moved:false};
  const token=target.wingFreeManeuver;
  // Remove a consumed/cancelled grant atomically, even when geometry changes.
  delete target.wingFreeManeuver;
  if(!next.active||!next.encounter?.slots?.length)throw Error('Manobra livre exige combate ativo.');
  if(target.profileKey!=='bespin_wing_guard_officer'||target.kind!=='minion')throw Error('Alvo da manobra livre falsificado.');
  const source=next.adversaries.find(a=>a.id===token.sourceId);
  const receipt=next.log.find(x=>x.type==='WING_GUARD_MANEUVER_APPLIED'&&x.actorId===token.sourceId&&
    x.kind==='tactical_direction'&&x.mode==='free_maneuver'&&x.round===token.grantedRound&&x.targetIds?.includes(targetId));
  if(!source||source.profileKey!=='bespin_wing_guard_sergeant'||!receipt||
     !Number.isSafeInteger(token.grantedRound)||token.grantedRound>next.round||
     token.toRange!==nextShortBand(token.fromRange,token.direction))
    throw Error('Recibo de manobra livre inválido.');
  if(adversaryHealthStatus(target).incapacitated||target.range!==token.fromRange){
    next.log.push({type:'WING_GUARD_FREE_MOVE_CANCELLED',targetId,round:next.round,reason:target.range!==token.fromRange?'range_changed':'incapacitated'});
    return {combat:next,moved:false,cancelled:true};
  }
  target.range=token.toRange;
  target.wingFreeManeuverUsedRound=next.round;
  next.log.push({type:'WING_GUARD_FREE_MOVE',sourceId:token.sourceId,targetId,round:next.round,fromRange:token.fromRange,toRange:token.toRange});
  return {combat:next,moved:true,fromRange:token.fromRange,toRange:token.toRange};
}
