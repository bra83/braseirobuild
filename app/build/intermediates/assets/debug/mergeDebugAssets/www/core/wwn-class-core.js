export const WWN_CLASS_SOURCES=Object.freeze({
  adventurer:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.1 The Adventurer'}),
  expert:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Full Expert table; Masterful Expertise; Quick Learner'}),
  warrior:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Full Warrior table; Killing Blow; Veteran’s Luck'}),
  mage:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'Partial Mage / Arcane Tradition'}),
});
const FULL_EXPERT_BAB=[null,0,1,1,2,2,3,3,4,4,5];
const FULL_WARRIOR_BAB=[null,1,2,3,4,5,6,7,8,9,10];
const ADVENTURER_BAB=Object.freeze({
  'PARTIAL_EXPERT+PARTIAL_MAGE':[null,0,1,1,2,2,3,3,4,4,5],
  'PARTIAL_EXPERT+PARTIAL_WARRIOR':[null,1,2,2,3,4,5,5,6,6,7],
  'PARTIAL_MAGE+PARTIAL_WARRIOR':[null,1,2,2,3,4,5,5,6,6,7],
});
function level(v){if(!Number.isInteger(v)||v<1||v>10)throw new RangeError('level must be 1..10');return v;}
function comboKey(parts){return [...parts].map(v=>String(v).toUpperCase()).sort().join('+');}
export function classCapabilities(profile){
  const id=String(profile?.classId??profile??'').toUpperCase();
  if(id==='FULL_EXPERT')return Object.freeze({status:'OK',classId:id,sourceRefs:[WWN_CLASS_SOURCES.expert],quickLearner:true,masterfulExpertise:true,killingBlow:false,veteransLuck:false,arcaneTraditionRequired:false});
  if(id==='FULL_WARRIOR')return Object.freeze({status:'OK',classId:id,sourceRefs:[WWN_CLASS_SOURCES.warrior],quickLearner:false,masterfulExpertise:false,killingBlow:true,veteransLuck:true,arcaneTraditionRequired:false});
  if(id==='PARTIAL_EXPERT')return Object.freeze({status:'OK',classId:id,sourceRefs:[WWN_CLASS_SOURCES.adventurer],quickLearner:true,masterfulExpertise:false,killingBlow:false,veteransLuck:false,arcaneTraditionRequired:false});
  if(id==='PARTIAL_WARRIOR')return Object.freeze({status:'OK',classId:id,sourceRefs:[WWN_CLASS_SOURCES.adventurer],quickLearner:false,masterfulExpertise:false,killingBlow:false,veteransLuck:false,arcaneTraditionRequired:false});
  if(id==='PARTIAL_MAGE')return Object.freeze({status:'OK',classId:id,sourceRefs:[WWN_CLASS_SOURCES.adventurer,WWN_CLASS_SOURCES.mage],quickLearner:false,masterfulExpertise:false,killingBlow:false,veteransLuck:false,arcaneTraditionRequired:true});
  if(id==='FULL_MAGE'||id==='MAGE')return Object.freeze({status:'TRADITION_CONTRACT_REQUIRED',classId:id,sourceRefs:[WWN_CLASS_SOURCES.mage],arcaneTraditionRequired:true});
  return Object.freeze({status:'UNSUPPORTED_CLASS_PROFILE'});
}
export function classBaseAttackBonus({classId,level:lv,partialClasses=null}){
  level(lv);const id=String(classId??'').toUpperCase();if(id==='FULL_EXPERT')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.expert],baseAttackBonus:FULL_EXPERT_BAB[lv]});if(id==='FULL_WARRIOR')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.warrior],baseAttackBonus:FULL_WARRIOR_BAB[lv]});
  if(id==='ADVENTURER'){
    if(!Array.isArray(partialClasses)||partialClasses.length!==2)return Object.freeze({status:'TWO_PARTIAL_CLASSES_REQUIRED'});const key=comboKey(partialClasses);const table=ADVENTURER_BAB[key];if(!table){if(partialClasses.every(p=>String(p).toUpperCase()==='PARTIAL_MAGE'))return Object.freeze({status:'DUAL_PARTIAL_MAGE_TRADITIONS_REQUIRED'});return Object.freeze({status:'INVALID_ADVENTURER_COMBINATION'});}return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.adventurer],combination:key,baseAttackBonus:table[lv]});
  }
  return Object.freeze({status:'CLASS_TABLE_NOT_BOUND'});
}
export function classHitDiceExpression({classId,level:lv,partialClasses=null}){
  level(lv);const id=String(classId??'').toUpperCase();if(id==='FULL_EXPERT')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.expert],expression:`${lv}d6`});if(id==='FULL_WARRIOR')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.warrior],expression:`${lv}d6+${lv*2}`});
  if(id==='ADVENTURER'){
    if(!Array.isArray(partialClasses)||partialClasses.length!==2)return Object.freeze({status:'TWO_PARTIAL_CLASSES_REQUIRED'});const key=comboKey(partialClasses);if(key==='PARTIAL_EXPERT+PARTIAL_MAGE')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.adventurer],expression:`${lv}d6`});if(key==='PARTIAL_EXPERT+PARTIAL_WARRIOR'||key==='PARTIAL_MAGE+PARTIAL_WARRIOR')return Object.freeze({status:'OK',sourceRefs:[WWN_CLASS_SOURCES.adventurer],expression:`${lv}d6+${lv*2}`});if(partialClasses.every(p=>String(p).toUpperCase()==='PARTIAL_MAGE'))return Object.freeze({status:'DUAL_PARTIAL_MAGE_TRADITIONS_REQUIRED'});return Object.freeze({status:'INVALID_ADVENTURER_COMBINATION'});
  }return Object.freeze({status:'CLASS_TABLE_NOT_BOUND'});
}
export function validateClassCreationContract(contract){
  if(!contract||typeof contract!=='object')return Object.freeze({ok:false,status:'CLASS_CONTRACT_REQUIRED'});const id=String(contract.classId??'').toUpperCase();if(id==='FULL_EXPERT'||id==='FULL_WARRIOR')return Object.freeze({ok:true,status:'OK',profile:classCapabilities(id)});if(id==='ADVENTURER'){
    if(!Array.isArray(contract.partialClasses)||contract.partialClasses.length!==2)return Object.freeze({ok:false,status:'TWO_PARTIAL_CLASSES_REQUIRED'});const parts=contract.partialClasses.map(v=>String(v).toUpperCase());if(parts.some(v=>!['PARTIAL_EXPERT','PARTIAL_WARRIOR','PARTIAL_MAGE'].includes(v)))return Object.freeze({ok:false,status:'INVALID_PARTIAL_CLASS'});if(parts[0]===parts[1]&&parts[0]!=='PARTIAL_MAGE')return Object.freeze({ok:false,status:'DUPLICATE_PARTIAL_CLASS_NOT_ALLOWED'});if(parts.every(v=>v==='PARTIAL_MAGE')){if(!Array.isArray(contract.mageTraditions)||contract.mageTraditions.length!==2)return Object.freeze({ok:false,status:'TWO_DISTINCT_MAGE_TRADITIONS_REQUIRED'});const traditionKey=t=>String(t?.traditionId??t?.id??t?.sourceRef?.traditionId??t?.sourceRef?.id??t?.sourceRef?.locator??'').trim();const keys=contract.mageTraditions.map(traditionKey);if(keys.some(k=>!k)||keys[0]===keys[1])return Object.freeze({ok:false,status:'TWO_DISTINCT_MAGE_TRADITIONS_REQUIRED'});if(contract.mageTraditions.some(t=>!t?.sourceRef))return Object.freeze({ok:false,status:'MAGE_TRADITION_SOURCE_REQUIRED'});}else if(parts.includes('PARTIAL_MAGE')){if(!contract.mageTradition?.sourceRef)return Object.freeze({ok:false,status:'MAGE_TRADITION_SOURCE_REQUIRED'});}return Object.freeze({ok:true,status:'OK',sourceRefs:[WWN_CLASS_SOURCES.adventurer]});}
  if(id==='FULL_MAGE'||id==='MAGE'){if(!contract.mageTradition?.sourceRef)return Object.freeze({ok:false,status:'MAGE_TRADITION_SOURCE_REQUIRED'});return Object.freeze({ok:true,status:'OK',sourceRefs:[WWN_CLASS_SOURCES.mage]});}
  return Object.freeze({ok:false,status:'UNSUPPORTED_CLASS_PROFILE'});
}
