import { attributeModifier } from './wwn-rules-core.js';

export const WWN_MAGIC_SOURCES=Object.freeze({
  spellcasting:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.2 Magic / §4.2.3 Casting Prepared Spells'}),
  preparing:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.2.2 Preparing a Spell'}),
  effort:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Arts and Effort'}),
  highMage:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.10 High Mage'}),
});
function int(v,label,{min=null}={}){if(!Number.isInteger(v)||(min!==null&&v<min))throw new RangeError(`${label} invalid`);return v;}
function score(v,label){if(!Number.isInteger(v)||v<3||v>18)throw new RangeError(`${label} must be 3..18`);return v;}
function effortShape(effort){
  if(!effort||typeof effort!=='object')throw new TypeError('effort state required');const max=int(effort.max,'effort.max',{min:1});
  const c=effort.committed??{};const day=int(c.day??0,'effort day',{min:0});const scene=int(c.scene??0,'effort scene',{min:0});const indefinite=int(c.indefinite??0,'effort indefinite',{min:0});if(day+scene+indefinite>max)throw new RangeError('committed Effort exceeds max');return{max,day,scene,indefinite};
}
export function mageEffortMaximum({magicSkillLevel,intelligenceScore,charismaScore,partialMage=false}){
  int(magicSkillLevel,'Magic skill',{min:0});score(intelligenceScore,'Intelligence');score(charismaScore,'Charisma');
  const full=1+magicSkillLevel+Math.max(attributeModifier(intelligenceScore),attributeModifier(charismaScore));
  return Math.max(1,partialMage?full-1:full);
}
export function inspectEffort(effort){const s=effortShape(effort);return Object.freeze({...s,committedTotal:s.day+s.scene+s.indefinite,available:s.max-s.day-s.scene-s.indefinite});}
export function commitEffort(effort,duration){
  const s=inspectEffort(effort);const key=String(duration??'').toUpperCase();if(!['DAY','SCENE','INDEFINITE'].includes(key))return Object.freeze({status:'INVALID_EFFORT_DURATION'});if(s.available<1)return Object.freeze({status:'NO_EFFORT_AVAILABLE',before:s});
  const after={max:s.max,committed:{day:s.day,scene:s.scene,indefinite:s.indefinite}};after.committed[key.toLowerCase()] +=1;return Object.freeze({status:'OK',sourceRefs:[WWN_MAGIC_SOURCES.effort],duration:key,before:s,after:Object.freeze(after)});
}
export function recoverEffort(effort,trigger){
  const s=inspectEffort(effort);const key=String(trigger??'').toUpperCase();const after={max:s.max,committed:{day:s.day,scene:s.scene,indefinite:s.indefinite}};
  if(key==='SCENE_END')after.committed.scene=0;else if(key==='GOOD_NIGHT_REST')after.committed.day=0;else if(key==='RECLAIM_INDEFINITE')after.committed.indefinite=Math.max(0,after.committed.indefinite-1);else return Object.freeze({status:'INVALID_EFFORT_RECOVERY_TRIGGER'});
  return Object.freeze({status:'OK',sourceRefs:[WWN_MAGIC_SOURCES.effort],trigger:key,before:s,after:Object.freeze(after)});
}
export function inspectSpellCast({magicState,spellId,context={}}){
  if(!magicState||typeof magicState!=='object')return Object.freeze({status:'MAGIC_STATE_REQUIRED_NO_EFFECT'});if(!spellId)return Object.freeze({status:'SPELL_ID_REQUIRED_NO_EFFECT'});
  const prepared=Array.isArray(magicState.preparedSpellIds)?magicState.preparedSpellIds:[];if(!prepared.includes(spellId))return Object.freeze({status:'SPELL_NOT_PREPARED_NO_EFFECT'});
  const remaining=magicState.castingsRemaining;if(!Number.isInteger(remaining)||remaining<0)return Object.freeze({status:'CASTINGS_EVIDENCE_REQUIRED_NO_EFFECT'});if(remaining<1)return Object.freeze({status:'NO_CASTINGS_REMAINING_NO_EFFECT'});
  if(context.tookHpDamageThisRound===true||context.severelyJostled===true)return Object.freeze({status:'CASTING_DISRUPTED_THIS_ROUND_NO_EFFECT'});
  if(context.freeHand!==true)return Object.freeze({status:'FREE_HAND_REQUIRED_NO_EFFECT'});if(context.canSpeakClearly!==true)return Object.freeze({status:'CLEAR_VOICE_REQUIRED_NO_EFFECT'});
  if((context.wearingRestrictiveArmor===true||context.usingShield===true)&&context.armoredMagicException!==true)return Object.freeze({status:'ARMOR_OR_SHIELD_BLOCKS_CASTING_NO_EFFECT'});
  return Object.freeze({status:'OK',rule:'WWN_PREPARED_SPELL_CAST_V1',sourceRefs:[WWN_MAGIC_SOURCES.spellcasting],spellId,castingsBefore:remaining,castingsAfter:remaining-1});
}
export function prepareSpells({magicState,newPreparedSpellIds,knownSpellIds,preparedLimit,goodNightRestCompleted,hourSpent}){
  if(!magicState||typeof magicState!=='object')return Object.freeze({status:'MAGIC_STATE_REQUIRED_NO_EFFECT'});if(goodNightRestCompleted!==true||hourSpent!==true)return Object.freeze({status:'PREPARATION_TIME_OR_REST_REQUIRED_NO_EFFECT'});
  if(!Array.isArray(newPreparedSpellIds)||!Array.isArray(knownSpellIds))return Object.freeze({status:'SPELL_LIST_EVIDENCE_REQUIRED_NO_EFFECT'});int(preparedLimit,'preparedLimit',{min:0});
  if(newPreparedSpellIds.length>preparedLimit)return Object.freeze({status:'PREPARED_LIMIT_EXCEEDED_NO_EFFECT'});const known=new Set(knownSpellIds);if(newPreparedSpellIds.some(id=>!known.has(id)))return Object.freeze({status:'UNKNOWN_SPELL_CANNOT_PREPARE_NO_EFFECT'});
  return Object.freeze({status:'OK',rule:'WWN_PREPARE_SPELLS_V1',sourceRefs:[WWN_MAGIC_SOURCES.preparing],preparedSpellIds:Object.freeze([...newPreparedSpellIds])});
}
export function recoverDailyMagic(magicState){
  if(!magicState||typeof magicState!=='object')return Object.freeze({status:'MAGIC_STATE_REQUIRED_NO_EFFECT'});if(!Number.isInteger(magicState.castingsMax)||magicState.castingsMax<0)return Object.freeze({status:'CASTINGS_MAX_EVIDENCE_REQUIRED_NO_EFFECT'});
  const next=structuredClone(magicState);next.castingsRemaining=magicState.castingsMax;if(next.effort){const recovered=recoverEffort(next.effort,'GOOD_NIGHT_REST');if(recovered.status==='OK')next.effort=recovered.after;}
  return Object.freeze({status:'OK',sourceRefs:[WWN_MAGIC_SOURCES.spellcasting,WWN_MAGIC_SOURCES.effort],magicState:Object.freeze(next)});
}
