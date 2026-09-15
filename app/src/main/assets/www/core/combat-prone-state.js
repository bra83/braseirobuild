import { proneCombatModifiers } from './wwn-special-combat-core.js';

function ids(combat){return Array.isArray(combat?.proneActorIds)?combat.proneActorIds:[];}
function finitePoint(p){return p&&Number.isFinite(p.xFeet)&&Number.isFinite(p.yFeet);}
function distance(a,b){return Math.hypot(b.xFeet-a.xFeet,b.yFeet-a.yFeet);}

export function isActorProne(combat,actorId){return ids(combat).includes(String(actorId));}

export function setActorProneInPlace(combat,actorId,prone=true){
  if(!combat||typeof combat!=='object')throw new TypeError('combat required');
  const id=String(actorId??'').trim();if(!id)throw new TypeError('actorId required');
  const next=new Set(ids(combat));if(prone)next.add(id);else next.delete(id);
  combat.proneActorIds=[...next].sort();
  return Object.freeze({status:'OK',actorId:id,prone:Boolean(prone),proneActorIds:Object.freeze([...combat.proneActorIds])});
}

export function proneAttackAdjustment({combat,attackerId,targetId,weapon,attackerPosition=null,targetPosition=null}){
  const attackKind=weapon?.melee===false?'RANGED':'MELEE';
  const adjacent=finitePoint(attackerPosition)&&finitePoint(targetPosition)?distance(attackerPosition,targetPosition)<=5+1e-9:false;
  return proneCombatModifiers({attackerProne:isActorProne(combat,attackerId),targetProne:isActorProne(combat,targetId),attackKind,adjacent});
}

export function effectiveProneMoveFeet({combat,actorId,baseMoveFeet}){
  if(!Number.isFinite(baseMoveFeet)||baseMoveFeet<0)throw new RangeError('baseMoveFeet must be finite and >= 0');
  const plan=proneCombatModifiers({attackerProne:isActorProne(combat,actorId),attackKind:'RANGED'});
  return Object.freeze({status:'OK',actorId:String(actorId),prone:isActorProne(combat,actorId),baseMoveFeet,movementMultiplier:plan.movementMultiplier,moveFeet:baseMoveFeet*plan.movementMultiplier,sourceRefs:plan.sourceRefs});
}
