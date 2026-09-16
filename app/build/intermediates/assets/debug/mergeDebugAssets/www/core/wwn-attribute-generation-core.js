const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.1.1 Generating Attributes'});
const ATTRIBUTES=Object.freeze(['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma']);
const ARRAY=Object.freeze([14,12,11,10,9,7]);
const freeze=v=>Object.freeze(v);
function sameAttributes(a,b){return ATTRIBUTES.every(k=>a?.[k]===b?.[k]);}
function validFinal(attributes){return attributes&&ATTRIBUTES.every(k=>Number.isInteger(attributes[k])&&attributes[k]>=3&&attributes[k]<=18);}

export function validateAttributeGeneration({attributes,generation}={}){
  if(!validFinal(attributes))return freeze({ok:false,status:'INVALID_FINAL_ATTRIBUTES',sourceRefs:[SRC]});
  if(!generation||typeof generation!=='object')return freeze({ok:false,status:'ATTRIBUTE_GENERATION_CONTRACT_REQUIRED',sourceRefs:[SRC]});
  const mode=String(generation.mode??'').toUpperCase();
  if(mode==='STANDARD_ARRAY'){
    const values=ATTRIBUTES.map(k=>attributes[k]).sort((a,b)=>b-a);
    const expected=[...ARRAY].sort((a,b)=>b-a);
    if(JSON.stringify(values)!==JSON.stringify(expected))return freeze({ok:false,status:'STANDARD_ARRAY_VALUES_MISMATCH',expected:ARRAY,sourceRefs:[SRC]});
    if(generation.substitutedAttribute)return freeze({ok:false,status:'STANDARD_ARRAY_CANNOT_USE_14_SUBSTITUTION',sourceRefs:[SRC]});
    return freeze({ok:true,status:'OK',mode,sourceRefs:[SRC]});
  }
  if(mode==='ROLL_3D6_ORDER'){
    const rolls=generation.rolls;if(!rolls||typeof rolls!=='object')return freeze({ok:false,status:'ATTRIBUTE_ROLLS_REQUIRED',sourceRefs:[SRC]});
    const rolled={};
    for(const a of ATTRIBUTES){const dice=rolls[a];if(!Array.isArray(dice)||dice.length!==3||dice.some(v=>!Number.isInteger(v)||v<1||v>6))return freeze({ok:false,status:'ATTRIBUTE_REQUIRES_THREE_SETTLED_D6',attribute:a,sourceRefs:[SRC]});rolled[a]=dice[0]+dice[1]+dice[2];}
    const substituted=generation.substitutedAttribute??null;
    if(substituted!==null&&!ATTRIBUTES.includes(substituted))return freeze({ok:false,status:'INVALID_SUBSTITUTED_ATTRIBUTE',sourceRefs:[SRC]});
    const expected={...rolled};if(substituted!==null)expected[substituted]=14;
    if(!sameAttributes(attributes,expected))return freeze({ok:false,status:'ATTRIBUTES_DO_NOT_MATCH_ORDERED_ROLLS_AND_SUBSTITUTION',rolled:freeze(rolled),substitutedAttribute:substituted,expected:freeze(expected),sourceRefs:[SRC]});
    return freeze({ok:true,status:'OK',mode,rolled:freeze(rolled),substitutedAttribute:substituted,sourceRefs:[SRC]});
  }
  return freeze({ok:false,status:'ATTRIBUTE_GENERATION_MODE_REQUIRED',allowed:freeze(['ROLL_3D6_ORDER','STANDARD_ARRAY']),sourceRefs:[SRC]});
}

export const WWN_STANDARD_ATTRIBUTE_ARRAY=ARRAY;
