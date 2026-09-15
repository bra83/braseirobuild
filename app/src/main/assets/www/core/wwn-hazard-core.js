import { RNG_ALGORITHM } from './rng-core.js';

export const WWN_HAZARD_SOURCES=Object.freeze({
  falling:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.10.0 Falling and Other Hazards — Falling'}),
  suffocation:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.10.0 Falling and Other Hazards — Suffocation'}),
  poison:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.10.0 Falling and Other Hazards — Poisons'}),
});
function int(v,label,min=0){if(!Number.isInteger(v)||v<min)throw new RangeError(`${label} must be integer >= ${min}`);return v}
function face(v){return typeof v==='number'?v:v?.result}
export function fallingDiceCount({feet,controlledDescentSuccess=false,hazardousBottom=false}){
  int(feet,'feet',0);
  const effectiveFeet=controlledDescentSuccess?Math.floor(feet/2):feet;
  const base=Math.min(20,Math.floor(effectiveFeet/10));
  return Object.freeze({rule:'WWN_FALLING_DICE_COUNT_V1',sourceRefs:[WWN_HAZARD_SOURCES.falling],feet,effectiveFeet,controlledDescentSuccess,baseDice:base,hazardousBottomBonusDice:hazardousBottom?1:0,totalDice:base+(hazardousBottom?1:0)});
}
export function controlledDescentDifficulty(feet){int(feet,'feet',0);return Object.freeze({rule:'WWN_CONTROLLED_DESCENT_DIFFICULTY_V1',sourceRefs:[WWN_HAZARD_SOURCES.falling],feet,difficulty:7+Math.floor(feet/10)});}
export function resolveFallingDamage({rng,feet,controlledDescentSuccess=false,hazardousBottom=false,streamName='hazard/falling'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  const plan=fallingDiceCount({feet,controlledDescentSuccess,hazardousBottom});
  const dice=Array.from({length:plan.totalDice},()=>rng.die(6,streamName));
  return Object.freeze({...plan,rule:'WWN_FALLING_DAMAGE_V1',rngAlgorithm:rng.algorithm??RNG_ALGORITHM,dice:Object.freeze(dice),damage:dice.reduce((n,d)=>n+face(d),0)});
}
export function suffocationGraceRounds({constitutionScore=null,npc=false,notMoving=false}){
  let base;
  if(npc){base=10}else{int(constitutionScore,'constitutionScore',1);base=constitutionScore}
  return Object.freeze({rule:'WWN_SUFFOCATION_GRACE_V1',sourceRefs:[WWN_HAZARD_SOURCES.suffocation],baseRounds:base,notMoving,rounds:base*(notMoving?4:1)});
}
export function suffocationFailureDamage({levelOrHitDice}){int(levelOrHitDice,'levelOrHitDice',1);return Object.freeze({rule:'WWN_SUFFOCATION_FAILURE_DAMAGE_V1',sourceRefs:[WWN_HAZARD_SOURCES.suffocation],levelOrHitDice,damage:levelOrHitDice});}
export function typicalDungeonPoisonDamage({maximumHitPoints,physicalSaveSucceeded}){
  int(maximumHitPoints,'maximumHitPoints',1);
  const half=maximumHitPoints/2;return Object.freeze({rule:'WWN_TYPICAL_DUNGEON_POISON_V1',sourceRefs:[WWN_HAZARD_SOURCES.poison],physicalSaveSucceeded:Boolean(physicalSaveSucceeded),damage:physicalSaveSucceeded?0:(Number.isInteger(half)?half:null),exactHalfMaximumHitPoints:physicalSaveSucceeded?0:half,requiresRoundingJudgment:!physicalSaveSucceeded&&!Number.isInteger(half)});
}
