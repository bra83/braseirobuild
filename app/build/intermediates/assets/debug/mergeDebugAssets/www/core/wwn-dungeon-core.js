import { RNG_ALGORITHM } from './rng-core.js';

export const WWN_DUNGEON_SOURCES = Object.freeze({
  order: Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.13.1 The Order of Play'}),
  time: Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.13.2 Timekeeping in the Dungeon'}),
  movement: Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.13.3 Movement and Fleeing'}),
  encounters: Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.13.5 Wandering Encounter Checks in the Dungeon'}),
});

export const DUNGEON_TURN_MINUTES=10;
export const DUNGEON_MOVEMENT_FEET_PER_TURN=120;
export const LIGHT_DURATION_TURNS=Object.freeze({TORCH:6,FILLED_LANTERN:24});
export const DUNGEON_ENCOUNTER_INTERVAL=Object.freeze({
  ALERTED_ORGANIZED:1,
  UNALERTED_ORGANIZED:2,
  NO_ORGANIZED_ACTIVE_DEFENSE:3,
  VERY_FEW_MOBILE_INHABITANTS:4,
  ABANDONED_OR_DISUSED_NOOK:6,
  HIDDEN_UNKNOWN_TO_NATIVES:null,
});
export const DUNGEON_ACTION_TURNS=Object.freeze({
  MOVE_POINT_TO_POINT:1,
  PICK_LOCK_OR_DISARM_TRAP:1,
  FIGHT:1,
  FIRST_AID_AND_LOOT:1,
  SEARCH_ROOM:1,
  JURY_RIG_OR_WORK_DEVICE:1,
  FLEE_SUCCESS:1,
});

export function encounterIntervalForSite(defenseState){
  const key=String(defenseState??'').toUpperCase();
  if(!(key in DUNGEON_ENCOUNTER_INTERVAL))throw new RangeError('unknown dungeon defenseState');
  return DUNGEON_ENCOUNTER_INTERVAL[key];
}

export function shouldCheckDungeonEncounter({turnNumber,defenseState}){
  if(!Number.isInteger(turnNumber)||turnNumber<1)throw new RangeError('turnNumber must be >=1');
  const interval=encounterIntervalForSite(defenseState);
  return interval!==null && turnNumber%interval===0;
}

export function dungeonActionTurnCost(actionKind){
  const key=String(actionKind??'').toUpperCase();
  if(!(key in DUNGEON_ACTION_TURNS))throw new RangeError('unknown dungeon actionKind');
  return Object.freeze({sourceRefs:[WWN_DUNGEON_SOURCES.time],actionKind:key,turns:DUNGEON_ACTION_TURNS[key],minutes:DUNGEON_ACTION_TURNS[key]*DUNGEON_TURN_MINUTES});
}

export function resolveDungeonTurns({rng,currentTurn=0,turns=1,defenseState,streamPrefix='dungeon',lightSource=null}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  if(!Number.isInteger(currentTurn)||currentTurn<0)throw new RangeError('currentTurn must be >=0');
  if(!Number.isInteger(turns)||turns<1)throw new RangeError('turns must be >=1');
  encounterIntervalForSite(defenseState);
  const checks=[];
  for(let offset=1;offset<=turns;offset+=1){
    const turnNumber=currentTurn+offset;
    if(shouldCheckDungeonEncounter({turnNumber,defenseState})){
      const die=rng.die(6,`${streamPrefix}/turn${turnNumber}/wandering`);
      checks.push(Object.freeze({turnNumber,die,encounter:die.result===1,secret:true}));
    }
  }
  let light=null;
  if(lightSource){
    const type=String(lightSource.type??'').toUpperCase();
    if(!(type in LIGHT_DURATION_TURNS))throw new RangeError('unknown light source type');
    const before=Number.isInteger(lightSource.remainingTurns)?lightSource.remainingTurns:LIGHT_DURATION_TURNS[type];
    if(before<0)throw new RangeError('remainingTurns must be >=0');
    const after=Math.max(0,before-turns);
    light=Object.freeze({type,before,after,extinguished:after===0});
  }
  return Object.freeze({
    rule:'WWN_DUNGEON_TURNS_V1',rngAlgorithm:RNG_ALGORITHM,
    sourceRefs:[WWN_DUNGEON_SOURCES.order,WWN_DUNGEON_SOURCES.time,WWN_DUNGEON_SOURCES.encounters],
    currentTurn,turns,nextTurn:currentTurn+turns,elapsedMinutes:turns*DUNGEON_TURN_MINUTES,
    checks:Object.freeze(checks),encounterTriggered:checks.some(c=>c.encounter),light,
  });
}

export function dungeonMovementForTurn({pointToPoint=false}={}){
  return Object.freeze({sourceRefs:[WWN_DUNGEON_SOURCES.movement],pointToPoint,turns:1,feet:pointToPoint?null:DUNGEON_MOVEMENT_FEET_PER_TURN});
}
