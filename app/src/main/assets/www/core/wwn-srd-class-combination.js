import { WWN_FULL_CLASS_IDS, WWN_PARTIAL_CLASS_IDS, WWN_SRD_CLASS_SOURCES, classProfile, isSpellcastingClass } from './wwn-srd-class-catalog.js';

const EXPERT_BAB=Object.freeze([null,0,1,1,2,2,3,3,4,4,5]);
const PARTIAL_WARRIOR_BAB=Object.freeze([null,1,2,2,3,4,5,5,6,6,7]);
const FULL_WARRIOR_BAB=Object.freeze([null,1,2,3,4,5,6,7,8,9,10]);
const MAGE_BAB=Object.freeze([null,0,0,0,0,1,1,1,1,1,2]);
function lv(level){if(!Number.isInteger(level)||level<1||level>10)throw new RangeError('level must be 1..10');return level;}
function id(v){return String(v??'').toUpperCase();}
function pairKey(pair){return [...pair].map(id).sort().join('+');}
function sourceRefsFor(parts){return Object.freeze([WWN_SRD_CLASS_SOURCES.classFramework,WWN_SRD_CLASS_SOURCES.adventurer,...parts.map(p=>p.sourceRef)]);}

export function validateFullClass(classId){const p=classProfile(classId);if(!p)return Object.freeze({ok:false,status:'UNKNOWN_CLASS'});if(!p.fullAllowed)return Object.freeze({ok:false,status:'FULL_CLASS_NOT_ALLOWED',classId:p.classId,sourceRefs:[p.sourceRef,WWN_SRD_CLASS_SOURCES.classFramework]});return Object.freeze({ok:true,status:'OK',classId:p.classId,profile:p,sourceRefs:[p.sourceRef,WWN_SRD_CLASS_SOURCES.classFramework]});}

export function validateAdventurerPair(partialClasses){
  if(!Array.isArray(partialClasses)||partialClasses.length!==2)return Object.freeze({ok:false,status:'TWO_PARTIAL_CLASSES_REQUIRED'});
  const ids=partialClasses.map(id);if(ids[0]===ids[1])return Object.freeze({ok:false,status:'DUPLICATE_PARTIAL_CLASS_NOT_ALLOWED',partialClasses:Object.freeze(ids)});
  const parts=ids.map(classProfile);if(parts.some(p=>!p||!p.partialAllowed))return Object.freeze({ok:false,status:'INVALID_PARTIAL_CLASS',partialClasses:Object.freeze(ids)});
  const mageslayer=parts.find(p=>p.classId==='MAGESLAYER');if(mageslayer){const other=parts.find(p=>p!==mageslayer);if(other?.baseFamily==='MAGE')return Object.freeze({ok:false,status:'MAGESLAYER_MAGE_PAIR_FORBIDDEN',partialClasses:Object.freeze(ids),sourceRefs:[WWN_SRD_CLASS_SOURCES.mageslayer,WWN_SRD_CLASS_SOURCES.classFramework]});}
  const invoker=parts.find(p=>p.classId==='INVOKER');if(invoker){const other=parts.find(p=>p!==invoker);if(other&&isSpellcastingClass(other.classId))return Object.freeze({ok:false,status:'PARTIAL_INVOKER_SPELLCASTER_PAIR_FORBIDDEN',partialClasses:Object.freeze(ids),sourceRefs:[WWN_SRD_CLASS_SOURCES.invoker]});}
  return Object.freeze({ok:true,status:'OK',partialClasses:Object.freeze(ids),profiles:Object.freeze(parts),combination:pairKey(ids),sourceRefs:sourceRefsFor(parts)});
}

function adventurerBabTable(parts){
  if(parts.some(p=>p.classId==='WARRIOR'))return PARTIAL_WARRIOR_BAB;
  if(parts.some(p=>p.classId==='MAGESLAYER'))return PARTIAL_WARRIOR_BAB;
  if(parts.some(p=>p.baseFamily==='EXPERT'))return EXPERT_BAB;
  return MAGE_BAB;
}
function adventurerHitDice(parts,level){
  if(parts.some(p=>p.classId==='WARRIOR')){
    if(parts.some(p=>p.classId==='DUELIST'))return `${level}d6`;
    return `${level}d6+${level*2}`;
  }
  if(parts.some(p=>p.classId==='MAGESLAYER'))return `${level}d6`;
  if(parts.some(p=>p.baseFamily==='EXPERT'))return `${level}d6`;
  if(parts.some(p=>p.special?.minimumHitDieD6===true))return `${level}d6`;
  return `${level}d6-${level}`;
}
function focusRequirementsForPair(parts){
  const req=['ANY'];
  if(parts.some(p=>p.classId==='WARRIOR'))req.unshift('WARRIOR');
  if(parts.some(p=>p.classId==='EXPERT'))req.unshift('EXPERT');
  return Object.freeze(req);
}

export function deriveFullClassChassis({classId,level=1}){
  lv(level);const valid=validateFullClass(classId);if(!valid.ok)return valid;const p=valid.profile;
  let expression,bab;
  if(p.classId==='WARRIOR'){expression=`${level}d6+${level*2}`;bab=FULL_WARRIOR_BAB[level];}
  else if(p.classId==='EXPERT'){expression=`${level}d6`;bab=EXPERT_BAB[level];}
  else{expression=`${level}d6-${level}`;bab=MAGE_BAB[level];}
  const focusRequirements=p.classId==='WARRIOR'?Object.freeze(['WARRIOR','ANY']):p.classId==='EXPERT'?Object.freeze(['EXPERT','ANY']):Object.freeze(['ANY']);
  return Object.freeze({status:'OK',classId:p.classId,level,hitDiceExpression:expression,baseAttackBonus:bab,focusRequirements,classAbilities:p.classAbilitiesFull,sourceRefs:Object.freeze([p.sourceRef,WWN_SRD_CLASS_SOURCES.classFramework])});
}

export function deriveAdventurerChassis({partialClasses,level=1}){
  lv(level);const valid=validateAdventurerPair(partialClasses);if(!valid.ok)return Object.freeze({status:valid.status,ok:false,...valid});const parts=valid.profiles;
  return Object.freeze({status:'OK',ok:true,classId:'ADVENTURER',partialClasses:valid.partialClasses,combination:valid.combination,level,hitDiceExpression:adventurerHitDice(parts,level),baseAttackBonus:adventurerBabTable(parts)[level],focusRequirements:focusRequirementsForPair(parts),classAbilities:Object.freeze(parts.flatMap(p=>p.classAbilitiesPartial)),sourceRefs:valid.sourceRefs});
}

export function enumerateLegalClassPaths(){
  const paths=[];for(const classId of WWN_FULL_CLASS_IDS)paths.push(Object.freeze({kind:'FULL',classId}));
  for(let i=0;i<WWN_PARTIAL_CLASS_IDS.length;i++)for(let j=i+1;j<WWN_PARTIAL_CLASS_IDS.length;j++){const pair=[WWN_PARTIAL_CLASS_IDS[i],WWN_PARTIAL_CLASS_IDS[j]];if(validateAdventurerPair(pair).ok)paths.push(Object.freeze({kind:'ADVENTURER',partialClasses:Object.freeze(pair)}));}
  return Object.freeze(paths);
}

export function classCoverageAudit(){
  const paths=enumerateLegalClassPaths();const covered=new Set();for(const p of paths){if(p.kind==='FULL')covered.add(p.classId);else p.partialClasses.forEach(x=>covered.add(x));}
  const missing=WWN_PARTIAL_CLASS_IDS.filter(x=>!covered.has(x));const missingFull=WWN_FULL_CLASS_IDS.filter(x=>!paths.some(p=>p.kind==='FULL'&&p.classId===x));
  return Object.freeze({status:missing.length||missingFull.length?'INCOMPLETE':'OK',fullClassCount:WWN_FULL_CLASS_IDS.length,partialClassCount:WWN_PARTIAL_CLASS_IDS.length,legalPathCount:paths.length,missing:Object.freeze(missing),missingFull:Object.freeze(missingFull)});
}
