import { RNG_ALGORITHM } from './rng-core.js';
import { attributeModifier } from './wwn-rules-core.js';

export const WWN_HEALTH_SOURCES = Object.freeze({
  mortalInjury: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.5.1 Mortal Injury and Stabilization' }),
  systemStrain: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.5.2 System Strain' }),
  naturalHealing: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.5.3 Natural Healing' }),
  firstAid: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.5.4 First Aid' }),
});

function integer(value,label,{min=null,max=null}={}){
  if(!Number.isInteger(value))throw new RangeError(`${label} must be an integer`);
  if(min!==null&&value<min)throw new RangeError(`${label} must be >= ${min}`);
  if(max!==null&&value>max)throw new RangeError(`${label} must be <= ${max}`);
  return value;
}
function hpState(character){
  const hp=character?.hp;if(!hp||!Number.isInteger(hp.current)||!Number.isInteger(hp.max)||hp.max<1||hp.current<0||hp.current>hp.max)throw new RangeError('character HP evidence required');
  return hp;
}
function constitutionScore(character){
  const score=character?.attributes?.Constitution??character?.attributes?.CON??character?.attributes?.Con;
  integer(score,'Constitution',{min:3,max:18});return score;
}
function strainState(character){
  const max=character?.systemStrain?.max??constitutionScore(character);
  const current=character?.systemStrain?.current??0;
  integer(max,'System Strain max',{min:0});integer(current,'System Strain current',{min:0,max});
  return {current,max};
}
export function hasCondition(character,condition){
  const target=String(condition).toUpperCase();
  const raw=character?.conditions??character?.statusConditions??[];
  if(Array.isArray(raw))return raw.some(c=>String(typeof c==='string'?c:(c?.id??c?.type??c?.name??'')).toUpperCase()===target);
  if(raw&&typeof raw==='object')return Object.entries(raw).some(([k,v])=>String(k).toUpperCase()===target&&Boolean(v));
  return false;
}
export function systemStrainMaximum(character){return constitutionScore(character);}

export function resolveStabilizationCheck({rng,attributeScore,healSkillLevel=null,roundsSinceDowned=0,hasMedicalTools=true,aidBonus=0,streamName='health/stabilize'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  integer(attributeScore,'attributeScore',{min:3,max:18});
  if(!(healSkillLevel===null||healSkillLevel===undefined||(Number.isInteger(healSkillLevel)&&healSkillLevel>=0)))throw new RangeError('healSkillLevel must be null/untrained or integer >= 0');
  integer(roundsSinceDowned,'roundsSinceDowned',{min:0,max:5});
  integer(aidBonus,'aidBonus',{min:0,max:1});
  const difficulty=8+roundsSinceDowned+(hasMedicalTools?0:2);
  const skillContribution=healSkillLevel??-1;
  const attributeMod=attributeModifier(attributeScore);
  const d1=rng.die(6,streamName);const d2=rng.die(6,streamName);
  const total=d1.result+d2.result+skillContribution+attributeMod+aidBonus;
  return Object.freeze({
    rule:'WWN_STABILIZATION_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_HEALTH_SOURCES.mortalInjury],
    dice:Object.freeze([d1,d2]),attributeScore,attributeModifier:attributeMod,healSkillLevel:healSkillLevel??null,skillContribution,aidBonus,
    roundsSinceDowned,hasMedicalTools,difficulty,total,success:total>=difficulty,
  });
}

export function resolveFirstAid({rng,target,healSkillLevel=null,hasHealersKit=false,streamName='health/first-aid'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  const hp=hpState(target);const strain=strainState(target);
  if(!hasHealersKit)return Object.freeze({rule:'WWN_FIRST_AID_V1',sourceRefs:[WWN_HEALTH_SOURCES.firstAid],status:'HEALERS_KIT_REQUIRED_NO_EFFECT'});
  if(hp.current>=hp.max)return Object.freeze({rule:'WWN_FIRST_AID_V1',sourceRefs:[WWN_HEALTH_SOURCES.firstAid],status:'TARGET_NOT_WOUNDED_NO_EFFECT'});
  if(strain.current>=strain.max)return Object.freeze({rule:'WWN_FIRST_AID_V1',sourceRefs:[WWN_HEALTH_SOURCES.firstAid,WWN_HEALTH_SOURCES.systemStrain],status:'SYSTEM_STRAIN_MAX_NO_BENEFIT',systemStrain:strain});
  if(!(healSkillLevel===null||healSkillLevel===undefined||(Number.isInteger(healSkillLevel)&&healSkillLevel>=0)))throw new RangeError('healSkillLevel must be null/untrained or integer >= 0');
  const die=rng.die(6,streamName);const bonus=healSkillLevel??-1;const rolled=Math.max(0,die.result+bonus);const healed=Math.min(hp.max-hp.current,rolled);
  return Object.freeze({
    rule:'WWN_FIRST_AID_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[WWN_HEALTH_SOURCES.firstAid,WWN_HEALTH_SOURCES.systemStrain],status:'OK',
    die,healSkillLevel:healSkillLevel??null,bonus,rolled,healed,hpBefore:hp.current,hpAfter:hp.current+healed,
    systemStrainBefore:strain.current,systemStrainAfter:strain.current+1,systemStrainMax:strain.max,frailRemains:hasCondition(target,'FRAIL'),
  });
}

export function resolveNaturalNightRest({character,warm,fed,comfortable,uninterruptedHours}){
  const hp=hpState(character);const strain=strainState(character);integer(uninterruptedHours,'uninterruptedHours',{min:0});
  const adequate=warm===true&&fed===true&&comfortable===true&&uninterruptedHours>=8;
  if(!adequate)return Object.freeze({rule:'WWN_NATURAL_REST_V1',sourceRefs:[WWN_HEALTH_SOURCES.naturalHealing,WWN_HEALTH_SOURCES.systemStrain],status:'PRIVATION_OR_INTERRUPTION_NO_RECOVERY',hpBefore:hp.current,hpAfter:hp.current,systemStrainBefore:strain.current,systemStrainAfter:strain.current});
  if(hasCondition(character,'FRAIL'))return Object.freeze({rule:'WWN_NATURAL_REST_V1',sourceRefs:[WWN_HEALTH_SOURCES.naturalHealing,WWN_HEALTH_SOURCES.systemStrain],status:'FRAIL_NO_NATURAL_RECOVERY',hpBefore:hp.current,hpAfter:hp.current,systemStrainBefore:strain.current,systemStrainAfter:strain.current});
  integer(character.level,'character level',{min:1});
  return Object.freeze({rule:'WWN_NATURAL_REST_V1',sourceRefs:[WWN_HEALTH_SOURCES.naturalHealing,WWN_HEALTH_SOURCES.systemStrain],status:'OK',hpBefore:hp.current,hpAfter:Math.min(hp.max,hp.current+character.level),systemStrainBefore:strain.current,systemStrainAfter:Math.max(0,strain.current-1)});
}

export function stabilizedRecoveryState(character){
  const hp=hpState(character);
  return Object.freeze({rule:'WWN_STABILIZED_RECOVERY_V1',sourceRefs:[WWN_HEALTH_SOURCES.mortalInjury],status:'OK',minutes:10,hpBefore:hp.current,hpAfter:1,removeCondition:'MORTALLY_INJURED',addCondition:'FRAIL'});
}

export function nonLethalRecoveryState(character){
  const hp=hpState(character);
  return Object.freeze({rule:'WWN_NONLETHAL_RECOVERY_V1',sourceRefs:[WWN_HEALTH_SOURCES.mortalInjury],status:'OK',minutes:10,hpBefore:hp.current,hpAfter:1,frailApplied:false});
}
