export const WWN_TACTICAL_COMBAT_SOURCES = Object.freeze({
  actions: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.3–2.4.4 Combat Action Types / Common Combat Actions' }),
  movement: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.4 Run / Fighting Withdrawal' }),
  charge: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.4 Charge / Screen an Ally' }),
  range: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.5 Attack Roll Modifiers; §3.4.3 Weapon Traits' }),
});

export const NORMAL_MOVE_FEET = 30;
export const DEFAULT_MELEE_FEET = 5;
export const LONG_MELEE_FEET = 10;
export const CHARGE_MIN_FEET = 3 * 3.280839895; // rules state three meters

function finite(v,label){if(typeof v!=='number'||!Number.isFinite(v))throw new TypeError(`${label} must be finite`);return v;}
function point(p,label='point'){if(!p||typeof p!=='object')throw new TypeError(`${label} required`);return{x:finite(p.xFeet??p.x,`${label}.x`),y:finite(p.yFeet??p.y,`${label}.y`)}}
export function distanceFeet(a,b){const p=point(a,'a'),q=point(b,'b');return Math.hypot(q.x-p.x,q.y-p.y)}
export function inMelee(a,b){return distanceFeet(a,b)<=DEFAULT_MELEE_FEET+1e-9}

export function freshActionEconomy(){return Object.freeze({mainAvailable:true,moveAvailable:true,withdrewFromMelee:false,totalDefense:false});}

export function weaponRangeProfile(weapon){
  if(!weapon||typeof weapon!=='object')throw new TypeError('weapon required');
  if(weapon.melee!==false){
    const max=weapon.longMelee===true||weapon.traits?.includes?.('L')?LONG_MELEE_FEET:DEFAULT_MELEE_FEET;
    return Object.freeze({kind:'MELEE',normalFeet:max,longFeet:max});
  }
  const normal=weapon.rangeFeet?.normal??weapon.normalRangeFeet;
  const long=weapon.rangeFeet?.long??weapon.longRangeFeet;
  if(!Number.isFinite(normal)||normal<=0||!Number.isFinite(long)||long<normal)throw new RangeError('ranged weapon range evidence required');
  return Object.freeze({kind:'RANGED',normalFeet:normal,longFeet:long});
}

export function inspectAttackRange({attackerPosition,targetPosition,weapon}){
  const distance=distanceFeet(attackerPosition,targetPosition);const profile=weaponRangeProfile(weapon);
  if(profile.kind==='MELEE')return Object.freeze({status:distance<=profile.normalFeet+1e-9?'OK':'OUT_OF_RANGE',distanceFeet:distance,profile,hitModifier:0,sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.range]});
  if(distance<=profile.normalFeet+1e-9)return Object.freeze({status:'OK',distanceFeet:distance,profile,hitModifier:0,rangeBand:'NORMAL',sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.range]});
  if(distance<=profile.longFeet+1e-9)return Object.freeze({status:'OK',distanceFeet:distance,profile,hitModifier:-2,rangeBand:'LONG',sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.range]});
  return Object.freeze({status:'OUT_OF_RANGE',distanceFeet:distance,profile,hitModifier:0,sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.range]});
}

export function inspectMovement({from,to,economy,armedEnemyPositions=[],fightingWithdrawal=false,normalMoveFeet=NORMAL_MOVE_FEET}){
  const distance=distanceFeet(from,to);const e=economy??freshActionEconomy();finite(normalMoveFeet,'normalMoveFeet');
  let cost=null;
  if(distance<=normalMoveFeet+1e-9 && e.moveAvailable===true)cost='MOVE';
  else if(distance<=normalMoveFeet+1e-9 && e.mainAvailable===true)cost='MAIN_AS_SECOND_MOVE';
  else if(distance<=normalMoveFeet*2+1e-9 && e.moveAvailable===true && e.mainAvailable===true)cost='MOVE_AND_MAIN';
  else return Object.freeze({status:'INSUFFICIENT_ACTION_OR_DISTANCE',distanceFeet:distance,normalMoveFeet,sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.actions,WWN_TACTICAL_COMBAT_SOURCES.movement]});
  const leavingEnemyIds=[];
  for(const raw of armedEnemyPositions){const before=inMelee(from,raw.position);const after=inMelee(to,raw.position);if(before&&!after)leavingEnemyIds.push(raw.actorId);}
  if(leavingEnemyIds.length&&fightingWithdrawal!==true&&e.withdrewFromMelee!==true)return Object.freeze({status:'FREE_ATTACK_REQUIRED',distanceFeet:distance,cost,leavingEnemyIds:Object.freeze(leavingEnemyIds),sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.movement]});
  return Object.freeze({status:'OK',distanceFeet:distance,cost,leavingEnemyIds:Object.freeze(leavingEnemyIds),sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.actions,WWN_TACTICAL_COMBAT_SOURCES.movement]});
}

export function applyActionCost(economy,cost){const e={...(economy??freshActionEconomy())};if(cost==='MOVE')e.moveAvailable=false;else if(cost==='MAIN_AS_SECOND_MOVE')e.mainAvailable=false;else if(cost==='MOVE_AND_MAIN'){e.moveAvailable=false;e.mainAvailable=false;}else if(cost==='MAIN')e.mainAvailable=false;else throw new RangeError(`unsupported action cost ${cost}`);return Object.freeze(e);}

export function applyFightingWithdrawal(economy){const e=economy??freshActionEconomy();if(e.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT'});return Object.freeze({status:'OK',economy:Object.freeze({...e,mainAvailable:false,withdrewFromMelee:true}),sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.movement]});}

export function inspectCharge({from,to,economy,normalMoveFeet=NORMAL_MOVE_FEET}){const e=economy??freshActionEconomy();if(e.mainAvailable!==true||e.moveAvailable!==true)return Object.freeze({status:'MAIN_AND_MOVE_REQUIRED'});const distance=distanceFeet(from,to);if(distance+1e-9<CHARGE_MIN_FEET)return Object.freeze({status:'CHARGE_TOO_SHORT',distanceFeet:distance,minFeet:CHARGE_MIN_FEET});if(distance>normalMoveFeet*2+1e-9)return Object.freeze({status:'CHARGE_TOO_FAR',distanceFeet:distance,maxFeet:normalMoveFeet*2});return Object.freeze({status:'OK',distanceFeet:distance,hitModifier:2,armorClassModifierUntilRoundEnd:-2,requiresStraightClearLine:true,sourceRefs:[WWN_TACTICAL_COMBAT_SOURCES.charge]});}
