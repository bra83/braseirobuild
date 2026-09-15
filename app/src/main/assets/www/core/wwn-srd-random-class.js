import { WISE_LEVEL1_CONCEPTS, classArtCatalog, classProfile, levelOneSpellCatalog } from './wwn-srd-class-catalog.js';
import { deriveAdventurerChassis, deriveFullClassChassis, enumerateLegalClassPaths, validateAdventurerPair } from './wwn-srd-class-combination.js';

function requireRng(rng){if(!rng||typeof rng.die!=='function')throw new TypeError('deterministic rng with die(sides,stream) required');return rng;}
function pick(rng,list,stream){if(!Array.isArray(list)||list.length<1)throw new Error(`empty catalog: ${stream}`);const roll=rng.die(list.length,stream);const n=Number(roll?.result);if(!Number.isInteger(n)||n<1||n>list.length)throw new Error(`rng out of range: ${stream}`);return list[n-1];}
function pickDistinct(rng,list,count,stream){const pool=[...new Set(list)];const out=[];for(let i=0;i<count;i++){if(!pool.length)throw new Error(`insufficient choices: ${stream}`);const chosen=pick(rng,pool,`${stream}/${i}`);out.push(chosen);pool.splice(pool.indexOf(chosen),1);}return Object.freeze(out);}
function attrMod(score){if(!Number.isInteger(score))return 0;if(score===3)return-2;if(score<=7)return-1;if(score<=13)return 0;if(score<=17)return 1;return 2;}
function skillLevel(skills,name){const v=skills?.[name];return Number.isInteger(v)?v:0;}
function effortValue(profile,{mode='PARTIAL',attributes={},skills={},orderSkill=null}={}){
  const f=profile.effortFormula;if(!f)return null;const skill=f.skill==='ORDER_SKILL'?orderSkill:f.skill;if(!skill)return null;let best;if(f.attributes.includes('BEST_ATTRIBUTE'))best=Math.max(...['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma'].map(a=>attrMod(attributes[a])));else best=Math.max(...f.attributes.map(a=>attrMod(attributes[a])));let value=skillLevel(skills,skill)+best;if(mode==='FULL'&&f.fullAddsOne)value+=1;return Math.max(f.minimum??1,value);
}
function artPicksFor(profile,mode,rng){
  const t=profile.initialTraining;const out=[...t.fixedArts];
  if(profile.special?.chooseOneFixedArt){const chosen=pick(rng,[...profile.special.chooseOneFixedArt],`class/${profile.classId}/required-art`);out.push(chosen);const other=profile.arts.filter(a=>!profile.special.chooseOneFixedArt.includes(a));out.push(...pickDistinct(rng,other,t.artPickCount,`class/${profile.classId}/art`));return Object.freeze(out);}
  if(profile.classId==='WISE')return Object.freeze([]);
  const count=(mode==='PARTIAL'&&Number.isInteger(profile.special?.partialArtPickCount))?profile.special.partialArtPickCount:t.artPickCount;
  const candidates=profile.arts.filter(a=>!out.includes(a));out.push(...pickDistinct(rng,candidates,count,`class/${profile.classId}/art`));return Object.freeze(out);
}
function wiseTraining(rng){const concept=pick(rng,Object.values(WISE_LEVEL1_CONCEPTS),'class/WISE/concept');const art=concept.chooseOne?pick(rng,[...concept.startingArts],`class/WISE/${concept.conceptId}/art`):concept.startingArts[0];return Object.freeze({conceptId:concept.conceptId,arts:Object.freeze([art])});}
function bonusSkillFor(profile,rng){if(profile.initialTraining.bonusSkill)return profile.initialTraining.bonusSkill;if(profile.initialTraining.bonusSkillChoice)return pick(rng,[...profile.initialTraining.bonusSkillChoice],`class/${profile.classId}/bonus-skill`);return null;}
function spellAccess(profiles){return Object.freeze([...new Set(profiles.flatMap(p=>[...p.initialTraining.spellLists]))]);}
function chooseSpells(rng,profiles,count){const lists=spellAccess(profiles);const names=[...new Set(lists.flatMap(levelOneSpellCatalog))];return count>0?pickDistinct(rng,names,count,'class/starting-spells'):Object.freeze([]);}
function spellKnownCount(profiles,mode){if(mode==='FULL')return profiles[0].initialTraining.spellKnownFull??0;const spellcasters=profiles.filter(p=>p.initialTraining.spellcasting===true);if(spellcasters.length>=2)return 4;if(spellcasters.length===1)return spellcasters[0].initialTraining.spellKnownPartial??2;return 0;}

export function inspectLevelOneTraining({path,rng,attributes={},skills={}}){
  requireRng(rng);let profiles,chassis,mode;
  if(path?.kind==='FULL'){const p=classProfile(path.classId);if(!p?.fullAllowed)return Object.freeze({status:'INVALID_FULL_CLASS'});profiles=[p];mode='FULL';chassis=deriveFullClassChassis({classId:p.classId,level:1});}
  else if(path?.kind==='ADVENTURER'){const valid=validateAdventurerPair(path.partialClasses);if(!valid.ok)return Object.freeze({status:valid.status});profiles=[...valid.profiles];mode='PARTIAL';chassis=deriveAdventurerChassis({partialClasses:path.partialClasses,level:1});}
  else return Object.freeze({status:'INVALID_CLASS_PATH'});
  const perClass=[];for(const p of profiles){const wise=p.classId==='WISE'?wiseTraining(rng):null;const bonusSkill=bonusSkillFor(p,rng);const mergedSkills={...skills};if(bonusSkill&&mergedSkills[bonusSkill]===undefined)mergedSkills[bonusSkill]=0;const orderSkill=p.classId==='VOWED'?bonusSkill:null;perClass.push(Object.freeze({classId:p.classId,bonusSkill,arts:wise?.arts??artPicksFor(p,mode,rng),wiseConceptId:wise?.conceptId??null,effortMax:effortValue(p,{mode,attributes,skills:mergedSkills,orderSkill}),sourceRef:p.sourceRef}));}
  const spellCount=spellKnownCount(profiles,mode);const startingSpells=chooseSpells(rng,profiles,spellCount);
  return Object.freeze({status:'OK',path:Object.freeze(structuredClone(path)),chassis,classes:Object.freeze(perClass),spellcasting:Object.freeze({knownCount:spellCount,lists:spellAccess(profiles),startingSpells}),sourceRefs:Object.freeze([...new Set(profiles.map(p=>p.sourceRef))])});
}


export function recomputeLevelOneTrainingEffort({training,attributes={},skills={}}={}){
  if(!training||training.status!=='OK'||!Array.isArray(training.classes))return Object.freeze({status:'INVALID_TRAINING'});
  const mode=training.path?.kind==='FULL'?'FULL':'PARTIAL';
  const classes=training.classes.map(entry=>{
    const profile=classProfile(entry.classId);if(!profile)return Object.freeze({...entry,effortMax:null});
    const orderSkill=profile.classId==='VOWED'?entry.bonusSkill:null;
    return Object.freeze({...structuredClone(entry),effortMax:effortValue(profile,{mode,attributes,skills,orderSkill})});
  });
  return Object.freeze({...structuredClone(training),classes:Object.freeze(classes)});
}

export function chooseRandomClassPath({rng,allowedClassIds=null}={}){
  requireRng(rng);let paths=[...enumerateLegalClassPaths()];if(Array.isArray(allowedClassIds)&&allowedClassIds.length){const allowed=new Set(allowedClassIds.map(v=>String(v).toUpperCase()));paths=paths.filter(p=>p.kind==='FULL'?allowed.has(p.classId):p.partialClasses.every(x=>allowed.has(x)));}
  if(!paths.length)return Object.freeze({status:'NO_LEGAL_CLASS_PATH'});const path=pick(rng,paths,'class/path');return Object.freeze({status:'OK',path});
}

export function generateRandomLevelOneClassContract({rng,attributes={},skills={},allowedClassIds=null}={}){
  const chosen=chooseRandomClassPath({rng,allowedClassIds});if(chosen.status!=='OK')return chosen;const training=inspectLevelOneTraining({path:chosen.path,rng,attributes,skills});if(training.status!=='OK')return training;return Object.freeze({status:'OK',classPath:chosen.path,chassis:training.chassis,training});
}
