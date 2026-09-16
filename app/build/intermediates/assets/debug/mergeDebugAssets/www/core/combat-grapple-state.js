function clone(v){return structuredClone(v)}
function links(combat){return Array.isArray(combat?.grapples)?combat.grapples:[]}
function id(v,label){const s=String(v??'').trim();if(!s)throw new TypeError(`${label} required`);return s}

export function grappleLinks(combat){return Object.freeze(links(combat).map(clone))}
export function grappleTargetOf(combat,assailantId){const a=String(assailantId??'');return links(combat).find(x=>x.assailantId===a)?.targetId??null}
export function grappleAssailantsOf(combat,targetId){const t=String(targetId??'');return Object.freeze(links(combat).filter(x=>x.targetId===t).map(x=>x.assailantId))}
export function actorGrappleState(combat,actorId){const a=String(actorId??'');const asAssailant=links(combat).filter(x=>x.assailantId===a),asTarget=links(combat).filter(x=>x.targetId===a);return Object.freeze({actorId:a,locked:asAssailant.length>0||asTarget.length>0,unarmedOnly:asAssailant.length>0||asTarget.length>0,asAssailant:Object.freeze(asAssailant.map(clone)),asTarget:Object.freeze(asTarget.map(clone))})}
export function isActorGrappleLocked(combat,actorId){return actorGrappleState(combat,actorId).locked}

export function startGrappleInPlace(combat,{assailantId,targetId,round}){
  if(!combat||typeof combat!=='object')throw new TypeError('combat required');const a=id(assailantId,'assailantId'),t=id(targetId,'targetId');if(a===t)return Object.freeze({status:'SELF_GRAPPLE_FORBIDDEN'});if(!Number.isInteger(round)||round<1)throw new RangeError('round required');
  const current=links(combat);const held=current.find(x=>x.assailantId===a);if(held)return Object.freeze({status:held.targetId===t?'GRAPPLE_ALREADY_ACTIVE':'ASSAILANT_ALREADY_GRAPPLING',targetId:held.targetId});
  const next=[...current,{assailantId:a,targetId:t,startedRound:round}];combat.grapples=next;return Object.freeze({status:'OK',link:Object.freeze(clone(next[next.length-1])),grapples:Object.freeze(next.map(clone))});
}
export function endGrappleInPlace(combat,{assailantId,targetId}){
  if(!combat||typeof combat!=='object')throw new TypeError('combat required');const a=id(assailantId,'assailantId'),t=id(targetId,'targetId');const current=links(combat),next=current.filter(x=>!(x.assailantId===a&&x.targetId===t));combat.grapples=next;return Object.freeze({status:next.length===current.length?'GRAPPLE_NOT_FOUND':'OK',assailantId:a,targetId:t,grapples:Object.freeze(next.map(clone))});
}
export function endAllGrapplesForActorInPlace(combat,actorId){if(!combat||typeof combat!=='object')throw new TypeError('combat required');const a=id(actorId,'actorId'),current=links(combat),ended=current.filter(x=>x.assailantId===a||x.targetId===a),next=current.filter(x=>x.assailantId!==a&&x.targetId!==a);combat.grapples=next;return Object.freeze({status:'OK',actorId:a,ended:Object.freeze(ended.map(clone)),grapples:Object.freeze(next.map(clone))})}
