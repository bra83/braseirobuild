const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.7 Elementalist; §1.5.10 High Mage; §1.5.11 Invoker; §1.5.14 Necromancer'});
const freeze=v=>Object.freeze(v);
const NORMAL_FULL=freeze([null,[1,1,3],[1,1,3],[2,2,4],[2,2,5],[3,3,6],[3,3,7],[4,4,8],[4,4,9],[5,5,10],[5,6,12]]);
const NORMAL_PARTIAL=freeze([null,[1,1,2],[1,1,3],[1,1,3],[1,2,4],[2,2,5],[2,3,6],[2,3,7],[2,3,7],[3,4,8],[3,4,9]]);
const INV_FULL=freeze([null,[1,1,2],[1,3,3],[2,4,4],[2,5,4],[3,6,5],[3,7,6],[4,8,7],[4,9,7],[5,10,8],[5,11,9]]);
const INV_PARTIAL=freeze([null,[1,1,1],[1,1,3],[1,2,3],[1,2,4],[2,3,4],[2,3,5],[2,4,5],[2,4,6],[3,5,6],[3,6,7]]);
const DUAL_PARTIAL=freeze([null,[1,1,3],[1,1,4],[1,2,5],[2,2,6],[2,2,8],[2,3,9],[3,3,10],[3,4,12],[3,4,13],[4,5,15]]);
function lv(v){if(!Number.isInteger(v)||v<1||v>10)throw new RangeError('level must be 1..10');return v;}
function mode(v){const m=String(v??'FULL').toUpperCase();if(!['FULL','PARTIAL'].includes(m))throw new RangeError('mode must be FULL or PARTIAL');return m;}
function id(v){return String(v??'').toUpperCase();}
export function spellcastingProgression({classId,level,mode:castingMode='FULL',intelligenceModifier=0}){
  const c=id(classId),l=lv(level),m=mode(castingMode);
  if(!Number.isInteger(intelligenceModifier))throw new TypeError('intelligenceModifier must be integer');
  if(c==='INVOKER'){const [maxSpellLevel,pointsBase,preparedBase]=(m==='FULL'?INV_FULL:INV_PARTIAL)[l];return freeze({rule:'WWN_INVOKER_PROGRESSION_V1',classId:c,level:l,mode:m,maxSpellLevel,spellPoints:Math.max(0,pointsBase+intelligenceModifier),spellsPrepared:Math.max(0,preparedBase+intelligenceModifier),spellsCast:null,sourceRefs:freeze([SRC])});}
  if(!['HIGH_MAGE','ELEMENTALIST','NECROMANCER'].includes(c))return freeze({rule:'WWN_SPELLCASTING_PROGRESSION_V1',classId:c,level:l,mode:m,spellcasting:false,sourceRefs:freeze([SRC])});
  const [maxSpellLevel,spellsCast,spellsPrepared]=(m==='FULL'?NORMAL_FULL:NORMAL_PARTIAL)[l];
  return freeze({rule:'WWN_SPELLCASTING_PROGRESSION_V1',classId:c,level:l,mode:m,spellcasting:true,maxSpellLevel,spellsCast,spellsPrepared,spellPoints:null,sourceRefs:freeze([SRC])});
}
export function advancementSpellGainCount({classId,mode:castingMode='FULL'}){
 const c=id(classId),m=mode(castingMode); if(c==='HIGH_MAGE'||c==='INVOKER')return m==='FULL'?2:1;if(c==='ELEMENTALIST'||c==='NECROMANCER')return 1;return 0;
}
export function baseSpellTraditions({classId,traditionalEducationTradition=null}){
 const c=id(classId);let out=[];
 if(c==='HIGH_MAGE')out=['HIGH_MAGIC'];
 else if(c==='ELEMENTALIST')out=['HIGH_MAGIC','ELEMENTALIST'];
 else if(c==='NECROMANCER')out=['HIGH_MAGIC','NECROMANCER'];
 else if(c==='INVOKER'){out=['HIGH_MAGIC'];if(traditionalEducationTradition)out.push(id(traditionalEducationTradition));}
 return freeze([...new Set(out)]);
}
export function artPicksGainedAtLevel({classId,level,mode:castingMode='FULL'}){
 const c=id(classId),l=lv(level),m=mode(castingMode);
 if(c==='HIGH_MAGE'||c==='NECROMANCER'){
   if(m==='FULL')return l===1?2:[2,4,6,8,10].includes(l)?1:0;
   return [1,2,3,6,9].includes(l)?1:0;
 }
 if(c==='ELEMENTALIST'){
   if(l===1)return 1; // fixed Resilience + Sparks are not picks
   if(m==='FULL')return [2,4,6,8,10].includes(l)?1:0;
   return [3,6,9].includes(l)?1:0;
 }
 if(c==='HEALER')return [1,2,4,5,6,8,10].includes(l)?1:0;
 return 0;
}
export function fixedArtsAtLevelOne({classId}){
 const c=id(classId);if(c==='ELEMENTALIST')return freeze(['Elemental Resilience','Elemental Sparks']);if(c==='HEALER')return freeze(['Healing Touch']);if(c==='MAGESLAYER')return freeze(['Antimage','Magebane']);return freeze([]);
}
export function fixedArtsGainedAtLevel({classId,level}){
 const c=id(classId),l=lv(level);if(c!=='MAGESLAYER')return freeze([]);
 const table={2:['Witchfinder','Spellshield'],3:['Disrupt Sorcery'],4:['Know Your Prey'],5:['Share the Pain'],6:['Dispel Enchantment'],7:['Ward Ally'],8:['Immaculate Body'],9:['Immaculate Mind'],10:['Absolute Negation']};
 return freeze([...(table[l]??[])]);
}

export function dualPartialSpellcastingProgression({level}){
  const l=lv(level);const [maxSpellLevel,spellsCast,spellsPrepared]=DUAL_PARTIAL[l];
  return freeze({rule:'WWN_DUAL_PARTIAL_SPELLCASTING_PROGRESSION_V1',level:l,mode:'DUAL_PARTIAL',spellcasting:true,maxSpellLevel,spellsCast,spellsPrepared,spellPoints:null,sourceRefs:freeze([{sourceId:'SRC-WWN-SRD-1',locator:'§4.3.0 Dual Partial Spellcasters'}])});
}
