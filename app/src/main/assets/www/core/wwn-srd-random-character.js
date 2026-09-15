import { DeterministicRng } from './rng-core.js';
import { applyCreationSkillPicks, additionalLanguageSlots, finalizeCharacterCreation, WWN_CHARACTER_CREATION_SOURCES } from './wwn-character-creation-core.js';
import { chooseRandomClassPath, inspectLevelOneTraining } from './wwn-srd-random-class.js';

const SRC=(locator)=>Object.freeze({sourceId:'SRC-WWN-SRD-1',locator});
export const WWN_RANDOM_CHARACTER_SOURCES=Object.freeze({
  attributes:SRC('§1.1.1 Generating Attributes'),
  artisan:SRC('§1.3.3 Example Backgrounds — Artisan'),
  barbarian:SRC('§1.3.3 Example Backgrounds — Barbarian'),
  alert:SRC('§1.6.1 Focus List — Alert'),
  armsmaster:SRC('§1.6.1 Focus List — Armsmaster'),
  artisanFocus:SRC('§1.6.1 Focus List — Artisan'),
  finalSkill:SRC('§1.7.4 Pick a Free Skill'),
});
const ATTRIBUTES=Object.freeze(['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma']);
const EXTRA_LANGUAGE_POOL=Object.freeze(['Antigo','Mercantil','Erudito','Costeiro']);
const BACKGROUNDS=Object.freeze([
  Object.freeze({backgroundId:'ARTISAN',freeSkill:'Craft',learning:Object.freeze(['Connect','Convince','Exert','Know','Trade']),sourceRef:WWN_RANDOM_CHARACTER_SOURCES.artisan}),
  Object.freeze({backgroundId:'BARBARIAN',freeSkill:'Survive',learning:Object.freeze(['Connect','Exert','Lead','Punch','Sneak']),sourceRef:WWN_RANDOM_CHARACTER_SOURCES.barbarian}),
]);
const FOCUS=Object.freeze({
  ANY:Object.freeze({focusId:'ALERT',name:'Alert',category:'ANY',bonusSkills:Object.freeze(['Notice']),sourceRef:WWN_RANDOM_CHARACTER_SOURCES.alert}),
  WARRIOR:Object.freeze({focusId:'ARMSMASTER',name:'Armsmaster',category:'COMBAT',bonusSkills:Object.freeze(['Stab']),sourceRef:WWN_RANDOM_CHARACTER_SOURCES.armsmaster}),
  EXPERT:Object.freeze({focusId:'ARTISAN',name:'Artisan',category:'NON_COMBAT',bonusSkills:Object.freeze(['Craft']),sourceRef:WWN_RANDOM_CHARACTER_SOURCES.artisanFocus}),
});
function pick(rng,list,stream){if(!Array.isArray(list)||list.length<1)throw new Error(`empty catalog: ${stream}`);if(list.length===1)return list[0];const r=rng.die(list.length,stream);return list[r.result-1];}
function countPicks(picks){const m=new Map();for(const p of picks)m.set(p,(m.get(p)||0)+1);return m;}
function attributes(rng){
  const out={};const rolls={};
  for(const a of ATTRIBUTES){const ds=[rng.die(6,`character/attributes/${a}/1`),rng.die(6,`character/attributes/${a}/2`),rng.die(6,`character/attributes/${a}/3`)];out[a]=ds.reduce((n,d)=>n+d.result,0);rolls[a]=ds;}
  const candidates=ATTRIBUTES.filter(a=>out[a]<14).sort((a,b)=>out[a]-out[b]||ATTRIBUTES.indexOf(a)-ATTRIBUTES.indexOf(b));
  const substituted=candidates[0]??null;if(substituted)out[substituted]=14;
  return Object.freeze({values:Object.freeze(out),rolls:Object.freeze(rolls),substitutedAttribute:substituted,sourceRef:WWN_RANDOM_CHARACTER_SOURCES.attributes});
}
function fociFor(chassis){return Object.freeze((chassis.focusRequirements??['ANY']).map(r=>structuredClone(FOCUS[r]??FOCUS.ANY)));}
function classBonusSkills(training){return training.classes.map(c=>c.bonusSkill).filter(Boolean);}
function focusBonusSkills(foci){return foci.flatMap(f=>f.bonusSkills??[]);}
function chooseBackground(rng,{reservedPicks=[]}={}){
  const reserved=countPicks(reservedPicks);const legalBackgrounds=BACKGROUNDS.filter(bg=>(reserved.get(bg.freeSkill)||0)<2);if(!legalBackgrounds.length)throw new Error('no legal source-bound background for reserved skill picks');
  const bg=pick(rng,legalBackgrounds,'character/background');
  const selected=[];const counts=countPicks([bg.freeSkill,...reservedPicks]);let pool=[...bg.learning];
  for(let i=0;i<2;i++){
    const legal=pool.filter(skill=>(counts.get(skill)||0)<2);if(!legal.length)throw new Error('no legal source-bound background learning pick');
    const skill=pick(rng,legal,`character/background/learning/${i}`);selected.push(skill);counts.set(skill,(counts.get(skill)||0)+1);pool=pool.filter(x=>x!==skill);
  }
  return Object.freeze({backgroundId:bg.backgroundId,freeSkill:bg.freeSkill,mode:'PICK',learningPicks:Object.freeze(selected),sourceRef:bg.sourceRef});
}
function languagesFor(skills,{linguaFranca='Comum',nativeLanguage='Comum'}={}){
  const slots=additionalLanguageSlots(skills).slots;
  if(slots>EXTRA_LANGUAGE_POOL.length)throw new Error('language pool too small');
  const baseline=[linguaFranca,...(nativeLanguage&&nativeLanguage!==linguaFranca?[nativeLanguage]:[])];
  return Object.freeze({linguaFranca,nativeLanguage,languages:Object.freeze([...baseline,...EXTRA_LANGUAGE_POOL.slice(0,slots)])});
}
function safePreviewSkills({background,training,foci,freeSkill}){
  const picks=[background.freeSkill,...background.learningPicks,...classBonusSkills(training),...focusBonusSkills(foci),freeSkill];
  const result=applyCreationSkillPicks({},picks);if(!result.ok)throw new Error(`random character skill materialization failed: ${result.status}`);return result.skills;
}
export function buildRandomLevelOneCharacterDraft({masterSeed='wwn-random-character',characterId='pc-random',name='Aventureiro',goal='Explorar o mundo',classPath=null,allowedClassIds=null,linguaFranca='Comum',nativeLanguage='Comum'}={}){
  const rng=new DeterministicRng(`${masterSeed}:${characterId}:draft`);const attr=attributes(rng);
  let path=classPath;if(!path){const chosen=chooseRandomClassPath({rng,allowedClassIds});if(chosen.status!=='OK')return chosen;path=chosen.path;}
  const training=inspectLevelOneTraining({path,rng,attributes:attr.values,skills:{}});if(training.status!=='OK')return training;
  const foci=fociFor(training.chassis);const freeSkill='Work';const background=chooseBackground(rng,{reservedPicks:[...classBonusSkills(training),...focusBonusSkills(foci),freeSkill]});
  const previewSkills=safePreviewSkills({background,training,foci,freeSkill});const language=languagesFor(previewSkills,{linguaFranca,nativeLanguage});
  const draft=Object.freeze({characterId,name,goal,attributes:attr.values,background,classContract:Object.freeze({ruleset:'WWN_SRD_2024',path:Object.freeze(structuredClone(path)),training}),foci,freeSkill,linguaFranca:language.linguaFranca,nativeLanguage:language.nativeLanguage,languages:language.languages,gear:Object.freeze({mode:'ROLL_SILVER'})});
  return Object.freeze({status:'OK',draft,trace:Object.freeze({attributeRolls:attr.rolls,substitutedAttribute:attr.substitutedAttribute,sourceRefs:Object.freeze([WWN_RANDOM_CHARACTER_SOURCES.attributes,background.sourceRef,WWN_CHARACTER_CREATION_SOURCES.finalSteps])})});
}
export function materializeRandomLevelOneCharacter(options={}){
  const built=buildRandomLevelOneCharacterDraft(options);if(built.status!=='OK')return built;const masterSeed=options.masterSeed??'wwn-random-character';const result=finalizeCharacterCreation({draft:built.draft,masterSeed:`${masterSeed}:${built.draft.characterId}:finalize`});if(!result.ok)return Object.freeze({status:result.status,details:result,draft:built.draft});return Object.freeze({status:'OK',character:result.character,draft:built.draft,trace:built.trace,sourceRefs:result.sourceRefs});
}
