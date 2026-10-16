import { buildPositivePool, rollPool } from './narrativeDice.mjs';
import { SKILLS } from './character.mjs';
import { sufferStrain } from './health.mjs';
import { applyStatusEffect, removeStatusEffect, hasFearImmunity, hasFearChecked } from './statusEffects.mjs';
import { upgradeNegativePool } from './destiny.mjs';
import { confidenceFearDifficultyReduction } from './talents.mjs';
const clone=v=>structuredClone(v);
export const FEAR_DIFFICULTIES=Object.freeze({minimal:1,moderate:2,very:3,mortal:4,utter:5});
export function fearDifficulty(level,{upgrades=0}={}){const difficulty=typeof level==='number'?Number(level):FEAR_DIFFICULTIES[level];if(!Number.isInteger(difficulty)||difficulty<0||difficulty>5)throw new Error('Nível de medo inválido.');return upgradeNegativePool({difficulty,challenge:0},Math.max(0,Number(upgrades)||0));}
export function fearConsequences(check={}){
  const success=Boolean(check.success);const failures=Math.max(0,-Number(check.netSuccess||0));const threats=Math.max(0,-Number(check.netAdvantage||0));const advantages=Math.max(0,Number(check.netAdvantage||0));const despair=Math.max(0,Number(check.despair||0));const triumph=Math.max(0,Number(check.triumph||0));
  return {setbackPerAction:success?0:1,strain:threats>=3?0:failures,difficultyIncrease:despair,staggered:threats>=3,firstCheckBoost:success&&advantages>0?1:0,triumph,failures,threats,advantages};
}
export function rollFearCheck(character,{skillKey='disciplina',difficulty=2,upgrades=0,boost=0,setback=0,rng=Math.random}={}){
  if(!['disciplina','frialdad'].includes(skillKey))throw new Error('Teste de Fear usa Disciplina ou Frieza.');
  const charKey=SKILLS[skillKey];const positive=buildPositivePool(Number(character?.characteristics?.[charKey]??0),Number(character?.skills?.[skillKey]??0));
  const raw=typeof difficulty==='number'?difficulty:FEAR_DIFFICULTIES[difficulty];
  if(!Number.isInteger(raw)||raw<0||raw>5)throw new Error('Nível de medo inválido.');
  const reduction=confidenceFearDifficultyReduction(character,skillKey);
  const neg=fearDifficulty(Math.max(1,raw-reduction),{upgrades});
  return {...rollPool({...positive,...neg,boost:Number(boost)||0,setback:Number(setback)||0},rng),fearTalentReduction:Math.min(reduction,Math.max(0,raw-1))};
}
export function resolveFear(character,{level='moderate',difficulty=null,upgrades=0,prepared=false,skillKey=null,boost=0,setback=0,sourceId='fear',forceRepeat=false,threatMode='strain',triumphMode='cancel',rng=Math.random}={}){
  if(!character)throw new Error('Nenhum personagem carregado.');if(!forceRepeat&&hasFearChecked(character,sourceId))return {character:clone(character),skipped:true,reason:'already_checked',sourceId};if(!forceRepeat&&hasFearImmunity(character,sourceId))return {character:clone(character),skipped:true,immune:true,sourceId};
  const skill=skillKey??(prepared?'frialdad':'disciplina');if(!['disciplina','frialdad'].includes(skill))throw new Error('Teste de medo usa Disciplina ou Frialdade.');
  const originalDifficulty=difficulty??(typeof level==='number'?level:FEAR_DIFFICULTIES[level]);
  const reduction=confidenceFearDifficultyReduction(character,skill);
  const neg=fearDifficulty(Math.max(1,originalDifficulty-reduction),{upgrades});
  const check=rollFearCheck(character,{skillKey:skill,difficulty:originalDifficulty,upgrades,boost,setback,rng});
  let next=clone(character);next=applyStatusEffect(next,{type:'fear_checked',source:'fear',sourceId,duration:'encounter'});const failures=Math.max(0,-Number(check.netSuccess||0));const threats=Math.max(0,-Number(check.netAdvantage||0));const advantages=Math.max(0,Number(check.netAdvantage||0));
  if(!check.success) next=applyStatusEffect(next,{type:'fear_setback',source:'fear',sourceId,duration:'encounter',effect:{setback:1}});
  if(threats>=3&&threatMode==='stagger') next=applyStatusEffect(next,{type:'staggered',source:'fear',sourceId,duration:'next_turn',rounds:1}); else if(threats>0) next=sufferStrain(next,threats).character;
  if(Number(check.despair||0)>0) next=applyStatusEffect(next,{type:'fear_difficulty',source:'fear',sourceId,duration:'encounter',effect:{difficultyIncrease:Number(check.despair)}});
  if(advantages>0) next=applyStatusEffect(next,{type:'fear_boost',source:'fear',sourceId,duration:'next_check',effect:{boost:1}});
  if(Number(check.triumph||0)>0){if(triumphMode==='immunity')next=applyStatusEffect(next,{type:'fear_immunity',source:'fear',sourceId,duration:'encounter'});else if(triumphMode==='cancel'){next=removeStatusEffect(next,{type:'fear_setback'});next=removeStatusEffect(next,{type:'fear_difficulty'});next=removeStatusEffect(next,{type:'staggered'});}}
  return {character:next,check,skillKey:skill,difficulty:neg.difficulty,challenge:neg.challenge,fearTalentReduction:check.fearTalentReduction,failures,threats,advantages,sourceId};
}
