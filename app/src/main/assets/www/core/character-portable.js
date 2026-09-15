import { canonicalJson } from './canonical-json.js';

export const PORTABLE_CHARACTER_VERSION=1;
const ATTRS=['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma'];
const SECRET_KEY=/api[_-]?key|access[_-]?token|refresh[_-]?token|credential|password|authorization/i;
function clone(v){return structuredClone(v)}
function scrub(v){if(Array.isArray(v))return v.map(scrub);if(!v||typeof v!=='object')return v;const o={};for(const [k,val] of Object.entries(v)){if(!SECRET_KEY.test(k))o[k]=scrub(val)}return o;}

export function validatePortableCharacterRecord(record){
  const errors=[];
  if(!record||typeof record!=='object')return Object.freeze({ok:false,errors:Object.freeze(['record required'])});
  if(record.format!=='BRASEIRO_WWN_CHARACTER'||record.version!==PORTABLE_CHARACTER_VERSION)errors.push('unsupported character format/version');
  const c=record.character;
  if(!c||typeof c!=='object')errors.push('character required');
  if(!c?.name||typeof c.name!=='string')errors.push('name required');
  if(!Number.isInteger(c?.level)||c.level<1||c.level>10)errors.push('level must be 1..10');
  for(const a of ATTRS){const v=c?.attributes?.[a];if(!Number.isInteger(v)||v<3||v>18)errors.push(`${a} must be 3..18`);}
  if(!c?.hp||!Number.isInteger(c.hp.current)||!Number.isInteger(c.hp.max)||c.hp.max<1||c.hp.current<0||c.hp.current>c.hp.max)errors.push('valid hp current/max required');
  const ss=c?.systemStrain;const ssMax=ss?.max??c?.attributes?.Constitution;const ssCur=ss?.current??0;
  if(!Number.isInteger(ssMax)||ssMax<0||!Number.isInteger(ssCur)||ssCur<0||ssCur>ssMax)errors.push('valid System Strain current/max required');
  return Object.freeze({ok:errors.length===0,errors:Object.freeze(errors)});
}

export function createPortableCharacter(character){
  const record={format:'BRASEIRO_WWN_CHARACTER',version:PORTABLE_CHARACTER_VERSION,system:'WWN',character:scrub(clone(character))};
  const valid=validatePortableCharacterRecord(record);if(!valid.ok)throw new Error(valid.errors.join('; '));
  return Object.freeze(record);
}
export function exportPortableCharacterJson(character){return canonicalJson(createPortableCharacter(character));}
export function importPortableCharacterJson(text){
  let record;try{record=JSON.parse(String(text));}catch{return Object.freeze({status:'INVALID_JSON'});}
  const valid=validatePortableCharacterRecord(record);if(!valid.ok)return Object.freeze({status:'INVALID_CHARACTER',errors:valid.errors});
  if(SECRET_KEY.test(canonicalJson(record)))return Object.freeze({status:'CREDENTIAL_LEAK_DETECTED'});
  return Object.freeze({status:'OK',character:Object.freeze(clone(record.character)),record:Object.freeze(clone(record))});
}
