import { RNG_ALGORITHM } from './rng-core.js';
import { attributeModifier } from './wwn-rules-core.js';

export const WWN_ENCOUNTER_SOURCES=Object.freeze({
  reaction:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Reaction Rolls and Parleying / SRD §5.2'}),
  opposed:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Opposed Skill Checks / SRD §2.3.4'}),
  escape:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Morale Checks and Prudent Retreats — Fleeing and Escape / SRD §5.3.2'}),
  morale:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§5.3.1 Making a Morale Check'}),
  instinct:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§5.4.1 Making an Instinct Check'}),
});

function faceValue(v){return typeof v==='number'?v:v?.result}
function freezeDice(faces){return Object.freeze(faces.map((v,index)=>Object.freeze({result:faceValue(v),sides:6,index,source:'PHYSICAL_3D_TOP_FACE'})))}
function physicalFaces(meta,count){
  const faces=meta?.rolls;
  if(!Array.isArray(faces)||faces.length!==count||faces.some(v=>!Number.isInteger(v)||v<1||v>6))throw new RangeError(`physical encounter resolution requires exactly ${count} settled d6 faces`);
  if(meta?.forcedSettle===true)throw new Error('forced physical settle is not authoritative');
  return faces;
}
function physicalProof(meta){return Object.freeze({seed:String(meta?.seed??''),physicsMs:Number(meta?.physicsMs??0),visualQuality:String(meta?.visualQuality??''),forcedSettle:false})}
function reactionDisposition(total){
  if(total<=2)return Object.freeze({id:'AGGRESSIVELY_HOSTILE',label:'AGRESSIVAMENTE HOSTIL'});
  if(total<=5)return Object.freeze({id:'UNFRIENDLY_HOSTILE',label:'HOSTIL E DESCONFIADO'});
  if(total<=8)return Object.freeze({id:'EXPECTED',label:'REAÇÃO ESPERADA'});
  if(total<=11)return Object.freeze({id:'FRIENDLY_BENIGN',label:'AMISTOSO E BENIGNO'});
  return Object.freeze({id:'MAXIMALLY_HELPFUL',label:'TÃO PRESTATIVO QUANTO POSSÍVEL'});
}
function finishReaction({faces,charismaScore,physicalMeta=null}){
  if(!Number.isInteger(charismaScore)||charismaScore<3||charismaScore>18)throw new RangeError('Charisma score must be 3..18');
  const values=faces.map(faceValue);const charismaModifier=attributeModifier(charismaScore),diceTotal=values[0]+values[1],total=diceTotal+charismaModifier,disposition=reactionDisposition(total);
  return Object.freeze({rule:'WWN_REACTION_ROLL_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.reaction],rngAlgorithm:physicalMeta?'PHYSICAL_3D_V1':RNG_ALGORITHM,diceAuthority:physicalMeta?'PHYSICAL_TOP_FACE_AFTER_SETTLE':'DETERMINISTIC_RNG',dice:physicalMeta?freezeDice(faces):Object.freeze(faces),diceTotal,charismaScore,charismaModifier,total,disposition,success:total>=6,physicalDice:physicalMeta?physicalProof(physicalMeta):null});
}
export function resolveReactionRoll({rng,charismaScore,streamName='encounter/reaction'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  const dice=[rng.die(6,streamName),rng.die(6,streamName)];
  return finishReaction({faces:dice,charismaScore});
}
export function resolveReactionRollFromPhysicalDice({physicalDice,charismaScore}){
  const faces=physicalFaces(physicalDice,2);return finishReaction({faces,charismaScore,physicalMeta:physicalDice});
}
function finishOpposed({actorFaces,defenderFaces,actorSkillLevel,actorAttributeScore,defenderSkillBonus=0,actorModifier=0,physicalMeta=null,rule='WWN_OPPOSED_SKILL_CHECK_V1',sourceRefs=[WWN_ENCOUNTER_SOURCES.opposed]}){
  if(!(actorSkillLevel===null||actorSkillLevel===undefined||(Number.isInteger(actorSkillLevel)&&actorSkillLevel>=0)))throw new RangeError('actorSkillLevel must be null/untrained or integer >=0');
  if(!Number.isInteger(defenderSkillBonus))throw new RangeError('defenderSkillBonus must be integer');
  if(!Number.isInteger(actorModifier))throw new RangeError('actorModifier must be integer');
  const actorAttributeModifier=attributeModifier(actorAttributeScore),actorSkillContribution=actorSkillLevel??-1;
  const actorValues=actorFaces.map(faceValue),defenderValues=defenderFaces.map(faceValue);const actorDiceTotal=actorValues.reduce((a,b)=>a+b,0),defenderDiceTotal=defenderValues.reduce((a,b)=>a+b,0);
  const actorTotal=actorDiceTotal+actorAttributeModifier+actorSkillContribution+actorModifier,defenderTotal=defenderDiceTotal+defenderSkillBonus;
  const success=actorTotal>=defenderTotal; // WWN opposed checks: PC wins ties.
  const allFaces=[...actorFaces,...defenderFaces];
  return Object.freeze({rule,sourceRefs,rngAlgorithm:physicalMeta?'PHYSICAL_3D_V1':RNG_ALGORITHM,diceAuthority:physicalMeta?'PHYSICAL_TOP_FACE_AFTER_SETTLE':'DETERMINISTIC_RNG',dice:physicalMeta?freezeDice(allFaces):allFaces,actorDice:Object.freeze(actorFaces),defenderDice:Object.freeze(defenderFaces),actorDiceTotal,defenderDiceTotal,actorSkillLevel:actorSkillLevel??null,actorSkillContribution,actorAttributeScore,actorAttributeModifier,actorModifier,defenderSkillBonus,actorTotal,defenderTotal,total:actorTotal,opposedTotal:defenderTotal,success,tie:actorTotal===defenderTotal,physicalDice:physicalMeta?physicalProof(physicalMeta):null});
}
export function resolveOpposedSkillCheck({rng,actorSkillLevel,actorAttributeScore,defenderSkillBonus=0,actorModifier=0,streamName='encounter/opposed'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  const actor=[rng.die(6,`${streamName}/pc`),rng.die(6,`${streamName}/pc`)];const defender=[rng.die(6,`${streamName}/npc`),rng.die(6,`${streamName}/npc`)];
  return finishOpposed({actorFaces:actor,defenderFaces:defender,actorSkillLevel,actorAttributeScore,defenderSkillBonus,actorModifier});
}
export function resolveOpposedSkillCheckFromPhysicalDice({physicalDice,actorSkillLevel,actorAttributeScore,defenderSkillBonus=0,actorModifier=0}){
  const faces=physicalFaces(physicalDice,4);return finishOpposed({actorFaces:faces.slice(0,2),defenderFaces:faces.slice(2),actorSkillLevel,actorAttributeScore,defenderSkillBonus,actorModifier,physicalMeta:physicalDice});
}
export function resolvePursuitEscape(args){return Object.freeze({...resolveOpposedSkillCheck(args),rule:'WWN_FLEEING_ESCAPE_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.escape,WWN_ENCOUNTER_SOURCES.opposed]})}
export function resolvePursuitEscapeFromPhysicalDice(args){return Object.freeze({...resolveOpposedSkillCheckFromPhysicalDice(args),rule:'WWN_FLEEING_ESCAPE_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.escape,WWN_ENCOUNTER_SOURCES.opposed]})}
export function resolveSafeWithdrawalNoRoll({reason='FOES_UNAWARE'}={}){return Object.freeze({rule:'WWN_SAFE_WITHDRAWAL_NO_ROLL_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.escape],rngAlgorithm:'NO_ROLL_REQUIRED',diceAuthority:'NO_ROLL_REQUIRED',dice:Object.freeze([]),total:null,success:true,reason});}


function requireNpcScore(value,label,min,max){
  if(!Number.isInteger(value)||value<min||value>max)throw new RangeError(`${label} must be integer ${min}..${max}`);
  return value;
}

export function resolveMoraleCheck({rng,moraleScore,streamName='encounter/morale'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  requireNpcScore(moraleScore,'moraleScore',2,12);
  const dice=[rng.die(6,streamName),rng.die(6,streamName)];
  const total=dice.reduce((n,d)=>n+faceValue(d),0);
  const losesHeart=total>moraleScore;
  return Object.freeze({rule:'WWN_MORALE_CHECK_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.morale],rngAlgorithm:rng.algorithm??RNG_ALGORITHM,dice:Object.freeze(dice),diceTotal:total,moraleScore,success:!losesHeart,losesHeart,outcome:losesHeart?'SEEK_ESCAPE_OR_STOP_BATTLE':'HOLDS_NERVE'});
}

export function resolveInstinctCheck({rng,instinctScore,streamName='encounter/instinct'}){
  if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng required');
  requireNpcScore(instinctScore,'instinctScore',0,10);
  if(instinctScore===0)return Object.freeze({rule:'WWN_INSTINCT_CHECK_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.instinct],rngAlgorithm:'NO_ROLL_REQUIRED',dice:Object.freeze([]),dieTotal:null,instinctScore,success:true,actsInstinctively:false,immune:true,outcome:'TACTICAL_CONTROL'});
  const die=rng.die(10,streamName);const value=faceValue(die);const actsInstinctively=value<=instinctScore;
  return Object.freeze({rule:'WWN_INSTINCT_CHECK_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.instinct],rngAlgorithm:rng.algorithm??RNG_ALGORITHM,dice:Object.freeze([die]),dieTotal:value,instinctScore,success:!actsInstinctively,actsInstinctively,immune:false,outcome:actsInstinctively?'ONE_ROUND_SUBOPTIMAL_INSTINCTIVE_ACTION':'TACTICAL_CONTROL'});
}

export function instinctFailureRate(instinctScore){
  requireNpcScore(instinctScore,'instinctScore',0,10);
  return Object.freeze({rule:'WWN_INSTINCT_LARGE_GROUP_RATE_V1',sourceRefs:[WWN_ENCOUNTER_SOURCES.instinct],instinctScore,failedFraction:instinctScore/10,failedPercent:instinctScore*10});
}
