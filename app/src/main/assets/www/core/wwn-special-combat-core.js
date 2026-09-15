import { attributeModifier } from './wwn-rules-core.js';

export const WWN_SPECIAL_COMBAT_SOURCES = Object.freeze({
  initiative: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.2.1–2.4.2.3 Individual Initiative / Surprise / Automatic Initiative Powers' }),
  commonActions: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.4 Common Combat Actions' }),
  modifiers: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.5.2 Attack Roll Modifiers' }),
  maneuvers: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.4.7 Special Combat Maneuvers' }),
});

function int(value,label,{min=null,max=null}={}){
  if(!Number.isInteger(value)) throw new TypeError(`${label} must be an integer`);
  if(min!==null&&value<min) throw new RangeError(`${label} must be >= ${min}`);
  if(max!==null&&value>max) throw new RangeError(`${label} must be <= ${max}`);
  return value;
}
function bool(value,label){if(typeof value!=='boolean')throw new TypeError(`${label} must be boolean`);return value;}
function opposedWinner({aTotal,bTotal,aIsPc=false,bIsPc=false}){
  int(aTotal,'aTotal');int(bTotal,'bTotal');
  if(aTotal>bTotal)return 'A';if(bTotal>aTotal)return 'B';
  if(Boolean(aIsPc)!==Boolean(bIsPc))return aIsPc?'A':'B';
  return 'TIE_GM_ORDER';
}

export function resolveIndividualInitiative({rng,dexterityScore=null,dexterityModifier=null,isPc=false,combatantId='combatant',streamName='combat/individual-initiative'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('rng required');
  let modifier=dexterityModifier;
  if(modifier===null||modifier===undefined){modifier=dexterityScore===null||dexterityScore===undefined?0:attributeModifier(dexterityScore);}
  int(modifier,'dexterityModifier');
  const die=rng.die(8,streamName);
  return Object.freeze({rule:'WWN_INDIVIDUAL_INITIATIVE_V1',combatantId:String(combatantId),isPc:Boolean(isPc),die,modifier,total:die.result+modifier,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.initiative]});
}
export function orderIndividualInitiative(entries){
  if(!Array.isArray(entries)||entries.length<1)throw new TypeError('entries required');
  return Object.freeze([...entries].sort((a,b)=>b.total-a.total||(Boolean(a.isPc)!==Boolean(b.isPc)?(a.isPc?-1:1):String(a.combatantId).localeCompare(String(b.combatantId)))));
}
export function surpriseRoundPlan({surprised=false,activelyAnticipatingCombat=false}){
  bool(Boolean(surprised),'surprised');bool(Boolean(activelyAnticipatingCombat),'activelyAnticipatingCombat');
  if(activelyAnticipatingCombat)return Object.freeze({rule:'WWN_SURPRISE_V1',canBeSurprised:false,freeEnemyRound:false,gmMayApplyInitiativePenalty:true,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.initiative]});
  return Object.freeze({rule:'WWN_SURPRISE_V1',canBeSurprised:true,freeEnemyRound:Boolean(surprised),initiativeAfterFreeRound:Boolean(surprised),sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.initiative]});
}
export function automaticInitiativePriority({hasAutomaticInitiative=false}){
  return Object.freeze({rule:'WWN_AUTO_INITIATIVE_V1',automaticFirst:Boolean(hasAutomaticInitiative),tieAmongAutomaticUsers:'ROLL_INITIATIVE_NORMALLY',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.initiative]});
}

export function snapAttackPlan({mainActionAvailable=true}){
  if(!mainActionAvailable)return Object.freeze({rule:'WWN_SNAP_ATTACK_V1',legal:false,reason:'MAIN_ACTION_ALREADY_SPENT',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
  return Object.freeze({rule:'WWN_SNAP_ATTACK_V1',legal:true,actionType:'INSTANT',consumesMainAction:true,hitModifier:-4,allowedAttackKinds:Object.freeze(['MELEE','RANGED']),npcRestriction:'ONLY_WELL_TRAINED_DISCIPLINED_NPCS',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
}
export function swarmAttackPlan({otherAssailants=0}){
  int(otherAssailants,'otherAssailants',{min:0,max:3});
  return Object.freeze({rule:'WWN_SWARM_ATTACK_V1',actionType:'MAIN',otherAssailants,hitBonus:otherAssailants*2,damageBonus:otherAssailants,hitBonusCap:6,damageBonusCap:3,bonusAddsToShock:false,shockAlwaysApplicable:true,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
}
export function screenCapacity({highestCombatSkill}){
  int(highestCombatSkill,'highestCombatSkill',{min:0});
  return Object.freeze({rule:'WWN_SCREEN_ALLY_V1',canScreen:highestCombatSkill>=1,maxAttackersPerRound:highestCombatSkill>=1?highestCombatSkill:0,actionType:'MOVE',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
}
export function resolveScreenContest({attackerTotal,attackerIsPc=false,defenders=[]}){
  int(attackerTotal,'attackerTotal');if(!Array.isArray(defenders))throw new TypeError('defenders must be an array');
  const successful=[];
  for(const d of defenders){const total=int(d.total,'defender.total');const winner=opposedWinner({aTotal:attackerTotal,bTotal:total,aIsPc:Boolean(attackerIsPc),bIsPc:Boolean(d.isPc)});if(winner==='B')successful.push({defenderId:String(d.defenderId),total,isPc:Boolean(d.isPc)});}
  successful.sort((a,b)=>a.total-b.total||a.defenderId.localeCompare(b.defenderId));
  return Object.freeze({rule:'WWN_SCREEN_CONTEST_V1',attackTargets:successful.length?'DEFENDER':'WARD',defenderId:successful[0]?.defenderId??null,successfulDefenders:Object.freeze(successful),sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
}
export function proneCombatModifiers({attackerProne=false,targetProne=false,attackKind='RANGED',adjacent=false}){
  const kind=String(attackKind).toUpperCase();if(!['MELEE','RANGED'].includes(kind))throw new RangeError('attackKind must be MELEE or RANGED');
  let hitModifier=0;
  if(attackerProne&&kind==='MELEE')hitModifier-=4;
  if(targetProne&&kind==='RANGED')hitModifier-=2;
  if(targetProne&&kind==='MELEE'&&adjacent)hitModifier+=2;
  return Object.freeze({rule:'WWN_PRONE_COMBAT_V1',hitModifier,movementMultiplier:attackerProne?0.5:1,goProneAction:'ON_TURN',standUpAction:'MOVE',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions,WWN_SPECIAL_COMBAT_SOURCES.modifiers]});
}
export function attackSituationModifier({targetHalfCover=false,targetAlmostCompleteCover=false,targetUnseenKnownPosition=false,targetUnseenUnknownPosition=false}){
  if(targetUnseenUnknownPosition)return Object.freeze({rule:'WWN_ATTACK_SITUATION_MODIFIER_V1',attackPossible:false,hitModifier:null,reason:'UNKNOWN_EXACT_POSITION',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.modifiers]});
  let mod=0;if(targetAlmostCompleteCover)mod-=4;else if(targetHalfCover)mod-=2;if(targetUnseenKnownPosition)mod-=4;
  return Object.freeze({rule:'WWN_ATTACK_SITUATION_MODIFIER_V1',attackPossible:true,hitModifier:mod,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.modifiers]});
}
export function holdActionPlan({moveActionAvailable=true}){
  return Object.freeze({rule:'WWN_HOLD_ACTION_V1',legal:Boolean(moveActionAvailable),actionCost:'MOVE',triggerRestOfTurnAs:'INSTANT',expires:'END_OF_ROUND',responseResolvesBeforeTriggeringAction:true,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.commonActions]});
}

export function resolvePcManeuverSkillRoll({rng,attributeScore,skillLevel=null,modifier=0,streamName='combat/maneuver/pc'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('rng required');int(attributeScore,'attributeScore',{min:3,max:18});if(!(skillLevel===null||skillLevel===undefined||(Number.isInteger(skillLevel)&&skillLevel>=0)))throw new RangeError('skillLevel must be null/untrained or integer >= 0');int(modifier,'modifier');
  const d1=rng.die(6,streamName),d2=rng.die(6,streamName),attributeMod=attributeModifier(attributeScore),skillContribution=skillLevel??-1,total=d1.result+d2.result+attributeMod+skillContribution+modifier;
  return Object.freeze({rule:'WWN_MANEUVER_PC_OPPOSED_ROLL_V1',dice:Object.freeze([d1,d2]),attributeScore,attributeModifier:attributeMod,skillLevel:skillLevel??null,skillContribution,modifier,total,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function resolveNpcManeuverSkillRoll({rng,skillBonus,modifier=0,streamName='combat/maneuver/npc'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('rng required');int(skillBonus,'skillBonus');int(modifier,'modifier');const d1=rng.die(6,streamName),d2=rng.die(6,streamName),total=d1.result+d2.result+skillBonus+modifier;
  return Object.freeze({rule:'WWN_MANEUVER_NPC_OPPOSED_ROLL_V1',dice:Object.freeze([d1,d2]),skillBonus,modifier,total,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}

export function shoveOutcome({attackHit,attackerOpposedTotal=null,defenderOpposedTotal=null,attackerIsPc=false,defenderIsPc=false}){
  if(!attackHit)return Object.freeze({rule:'WWN_SHOVE_V1',success:false,reason:'MELEE_ATTACK_MISSED',damage:0,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
  const winner=opposedWinner({aTotal:int(attackerOpposedTotal,'attackerOpposedTotal'),bTotal:int(defenderOpposedTotal,'defenderOpposedTotal'),aIsPc:Boolean(attackerIsPc),bIsPc:Boolean(defenderIsPc)});
  return Object.freeze({rule:'WWN_SHOVE_V1',success:winner==='A',winner,damage:0,onSuccess:Object.freeze(['PUSH_UP_TO_3_METERS','KNOCK_PRONE']),sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function grappleStartOutcome({unarmedAttackHit,bothHandsFree,attackerOpposedTotal=null,defenderOpposedTotal=null,attackerIsPc=false,defenderIsPc=false}){
  if(!unarmedAttackHit||!bothHandsFree)return Object.freeze({rule:'WWN_GRAPPLE_START_V1',success:false,reason:!unarmedAttackHit?'UNARMED_ATTACK_MISSED':'BOTH_HANDS_REQUIRED',damage:0,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
  const winner=opposedWinner({aTotal:int(attackerOpposedTotal,'attackerOpposedTotal'),bTotal:int(defenderOpposedTotal,'defenderOpposedTotal'),aIsPc:Boolean(attackerIsPc),bIsPc:Boolean(defenderIsPc)});
  return Object.freeze({rule:'WWN_GRAPPLE_START_V1',success:winner==='A',winner,damage:0,restrictions:winner==='A'?Object.freeze(['NO_LOCATION_MOVEMENT','UNARMED_ATTACKS_ONLY']):Object.freeze([]),automaticEndRoundUnarmedDamage:winner==='A',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function grappleEscapeOutcome({victimOpposedTotal,assailantOpposedTotals,victimIsPc=false,assailantsArePc=[]}){
  int(victimOpposedTotal,'victimOpposedTotal');if(!Array.isArray(assailantOpposedTotals)||assailantOpposedTotals.length<1)throw new TypeError('assailantOpposedTotals required');
  const beatsAll=assailantOpposedTotals.every((total,i)=>opposedWinner({aTotal:victimOpposedTotal,bTotal:int(total,'assailant total'),aIsPc:Boolean(victimIsPc),bIsPc:Boolean(assailantsArePc[i])})==='A');
  return Object.freeze({rule:'WWN_GRAPPLE_ESCAPE_V1',escaped:beatsAll,actionCost:'MAIN',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function grappleMoveOutcome({attackerOpposedTotal,defenderOpposedTotal,attackerIsPc=false,defenderIsPc=false,mode='MOVE'}){
  const winner=opposedWinner({aTotal:int(attackerOpposedTotal,'attackerOpposedTotal'),bTotal:int(defenderOpposedTotal,'defenderOpposedTotal'),aIsPc:Boolean(attackerIsPc),bIsPc:Boolean(defenderIsPc)});
  const success=winner==='A';const normalized=String(mode).toUpperCase();if(!['MOVE','THROW'].includes(normalized))throw new RangeError('mode must be MOVE or THROW');
  return Object.freeze({rule:'WWN_GRAPPLE_MOVE_V1',success,targetEscapes:!success,actionCost:'MAIN',moveTargetFeet:success&&normalized==='MOVE'?10:0,throwTargetFeet:success&&normalized==='THROW'?5:0,targetProne:success&&normalized==='THROW',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function sizePenaltyForShoveOrGrapple({substantiallyLargerHumanoid=false,barelyPlausibleOrQuadruped=false}){
  return Object.freeze({rule:'WWN_GRAPPLE_SIZE_MODIFIER_V1',skillCheckModifier:barelyPlausibleOrQuadruped?-4:substantiallyLargerHumanoid?-2:0,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function dualWieldPlan({primarySkillLevel,secondarySkillLevel,targetInRangeOfBoth=true}){
  int(primarySkillLevel,'primarySkillLevel',{min:0});int(secondarySkillLevel,'secondarySkillLevel',{min:0});
  const legal=primarySkillLevel>=1&&secondarySkillLevel>=1;
  return Object.freeze({rule:'WWN_DUAL_WIELD_V1',legal,hitModifier:legal?-1:null,hitDamageBonus:legal&&targetInRangeOfBoth?2:0,shockBonus:0,chooseOneWeaponForAttack:true,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function rangedExecutionDifficulty({rangeBand}){
  const band=String(rangeBand).toUpperCase();const map={WITHIN_2_METERS:6,NORMAL:8,LONG:10};if(!(band in map))throw new RangeError('rangeBand must be WITHIN_2_METERS, NORMAL, or LONG');
  return Object.freeze({rule:'WWN_EXECUTION_RANGED_V1',aimMinutes:1,difficulty:map[band],armorClassIgnoredOnSuccessfulSkillCheck:true,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function executionPhysicalSavePlan({combatSkillLevel}){
  int(combatSkillLevel,'combatSkillLevel',{min:0});return Object.freeze({rule:'WWN_EXECUTION_SAVE_V1',saveType:'PHYSICAL',saveModifier:-combatSkillLevel,onFailure:'ZERO_HP_MORTALLY_WOUNDED_OR_UNCONSCIOUS_IF_NONLETHAL',onSuccess:'MAXIMUM_DAMAGE',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
export function resolveExecutionAfterSave({saveSucceeded,nonLethal=false,maximumDamage}){
  int(maximumDamage,'maximumDamage',{min:0});
  if(saveSucceeded)return Object.freeze({rule:'WWN_EXECUTION_OUTCOME_V1',setHitPointsToZero:false,damage:maximumDamage,mortallyWounded:false,unconscious:false,sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
  return Object.freeze({rule:'WWN_EXECUTION_OUTCOME_V1',setHitPointsToZero:true,damage:null,mortallyWounded:!nonLethal,unconscious:Boolean(nonLethal),sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}
