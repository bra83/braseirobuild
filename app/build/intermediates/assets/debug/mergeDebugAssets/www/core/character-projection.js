import { attributeModifier } from './wwn-rules-core.js';
import { deriveArmorClass, deriveCombatMoveFeet } from './wwn-armor-core.js';
import { encumbranceCapacities, inventoryLoad } from './wwn-inventory-core.js';
import { classCapabilities } from './wwn-class-core.js';
import { classLabelPtBr, abilityLabelPtBr, focusLabelPtBr, skillLabelPtBr, artLabelPtBr, spellLabelPtBr } from './ptbr-labels.js';

function clone(v){return structuredClone(v)}
function idOfFocus(f){return String(typeof f==='string'?f:(f?.focusId??f?.id??f?.name??'')).trim()}
function prettyId(v){return String(v??'').replace(/^FOCUS[_:-]?/i,'').replaceAll('_',' ').replaceAll('-',' ').replace(/\s+/g,' ').trim().toUpperCase()||'—'}
function classLabel(character){const id=String(character?.classId??character?.class??'').toUpperCase();if(id!=='ADVENTURER')return classLabelPtBr(id);const parts=(character?.partialClasses??[]).map(classLabelPtBr);return parts.length?`AVENTUREIRO • ${parts.join(' + ')}`:'AVENTUREIRO'}
function abilityRows(character){
  const out=[];const caps=character?.classAbilities??{};
  if(Array.isArray(caps)){for(const value of caps){const raw=String(value);const id=raw.toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'');if(!out.some(x=>x.id===id))out.push(Object.freeze({id,label:abilityLabelPtBr(raw),source:'classAbilities'}));}return Object.freeze(out);}
  for(const key of ['masterfulExpertise','quickLearner','killingBlow','veteransLuck'])if(caps?.[key]===true)out.push(Object.freeze({id:key,label:abilityLabelPtBr(key),source:'classAbilities'}));
  if(String(character?.classId??'').toUpperCase()==='ADVENTURER'){
    for(const part of character?.partialClasses??[]){const pc=classCapabilities(String(part).toUpperCase());for(const key of ['masterfulExpertise','quickLearner','killingBlow','veteransLuck'])if(pc?.[key]===true&&!out.some(x=>x.id===key))out.push(Object.freeze({id:key,label:abilityLabelPtBr(key),source:String(part).toUpperCase()}));}
  }
  return Object.freeze(out);
}
function artRows(character){return Object.freeze((character?.arts??character?.classTraining?.classes?.flatMap(c=>(c.arts??[]).map(a=>({artId:a,name:a,classId:c.classId})))??[]).map((a,index)=>Object.freeze({id:String(a?.artId??a?.id??a?.name??`art-${index+1}`),label:artLabelPtBr(a?.name??a?.artId??a?.id??`art-${index+1}`),classId:a?.classId??null})));}
function focusRows(character){return Object.freeze((character?.foci??[]).map((f,index)=>{const id=idOfFocus(f)||`focus-${index+1}`;const raw=(typeof f==='object'&&f?.name)?f.name:id;return Object.freeze({id,label:focusLabelPtBr(raw),level:Number.isInteger(f?.level)?f.level:1,bonusSkills:Object.freeze([...(f?.bonusSkills??[])]),category:f?.category??null})}))}
function skillRows(character){
  const skills=character?.skills??{};
  return Object.freeze(Object.entries(skills).filter(([,v])=>Number.isInteger(v)).map(([name,level])=>Object.freeze({name,label:skillLabelPtBr(name),level})).sort((a,b)=>b.level-a.level||a.name.localeCompare(b.name,'en')));
}
function spellRows(character){return Object.freeze((character?.magic?.startingSpells??character?.magic?.spells??[]).map((s,index)=>Object.freeze({id:String(typeof s==='string'?s:(s?.spellId??s?.id??s?.name??`spell-${index+1}`)),label:spellLabelPtBr(typeof s==='string'?s:(s?.name??s?.spellId??s?.id??`spell-${index+1}`)),level:Number.isInteger(s?.level)?s.level:null})))}
function effortRows(character){const e=character?.effort;if(!e)return Object.freeze([]);if(Array.isArray(e))return Object.freeze(clone(e));if(typeof e!=='object')return Object.freeze([]);const rows=[];for(const [poolId,pool] of Object.entries(e)){if(pool&&typeof pool==='object')rows.push(Object.freeze({poolId,label:skillLabelPtBr(poolId),current:pool.current??pool.value??null,max:pool.max??null,committed:clone(pool.committed??null)}));}return Object.freeze(rows)}

export function projectCharacterForSheet(character){
  if(!character||typeof character!=='object')return Object.freeze({status:'NO_CHARACTER'});
  const attributes={};for(const key of ['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma']){const score=character.attributes?.[key];attributes[key]=Object.freeze({score:Number.isInteger(score)?score:null,modifier:Number.isInteger(score)?attributeModifier(score):null});}
  const armor=deriveArmorClass(character);const move=deriveCombatMoveFeet(character);const load=Array.isArray(character.inventory)?inventoryLoad(character):null;const strength=character.attributes?.Strength;const capacities=Number.isInteger(strength)?encumbranceCapacities(strength):null;
  const inventory=Array.isArray(character.inventory)?character.inventory.map(i=>Object.freeze({itemId:i.itemId??i.id??null,name:i.name??i.catalogId??i.itemId??'ITEM',kind:i.kind??'ITEM',carryState:String(i.carryState??'STOWED').toUpperCase(),encumbrance:i.encumbrance??0,loaded:i.loaded??null,quantity:i.quantity??null})):null;
  return Object.freeze({
    status:'OK',characterId:String(character.characterId??character.id??''),name:character.name??'—',goal:character.goal??null,level:character.level??null,xp:character.xp??0,
    classId:String(character.classId??character.class??''),classLabel:classLabel(character),partialClasses:Object.freeze([...(character.partialClasses??[])]),
    attributes:Object.freeze(attributes),skills:skillRows(character),savingThrows:Object.freeze(clone(character.savingThrows??character.saves??{})),
    hp:Object.freeze(clone(character.hp??{current:null,max:null})),systemStrain:Object.freeze(clone(character.systemStrain??{current:null,max:null})),
    baseAttackBonus:Number.isInteger(character.baseAttackBonus)?character.baseAttackBonus:null,
    armorClass:armor.status==='OK'?armor.armorClass:null,armorProjection:armor,moveFeet:move.status==='OK'?move.moveFeet:null,
    classAbilities:abilityRows(character),foci:focusRows(character),arts:artRows(character),inventory:inventory?Object.freeze(inventory):null,
    load:load?.status==='OK'?Object.freeze({readiedPoints:load.readiedPoints,stowedPoints:load.stowedPoints,readiedCapacity:load.assessment.capacities.readied,stowedCapacity:load.assessment.capacities.stowed,status:load.assessment.status}):Object.freeze({readiedPoints:null,stowedPoints:null,readiedCapacity:capacities?.readied??null,stowedCapacity:capacities?.stowed??null,status:load?.status??'MISSING_EVIDENCE'}),
    silver:Number.isInteger(character.silver)?character.silver:null,languages:Object.freeze([...(character.languages??[])]),magic:character.magic?Object.freeze(clone(character.magic)):null,spells:spellRows(character),effort:effortRows(character),conditions:Object.freeze(clone(character.conditions??[])),visualProfile:character.visualProfile?Object.freeze(clone(character.visualProfile)):null,
  });
}


export function projectCharacterRosterForSheet(state){
  const roster={...(state?.charactersState??{})};const active=state?.characterState;if(active?.characterId&&!roster[active.characterId])roster[active.characterId]=active;return Object.freeze({status:Object.keys(roster).length?'OK':'NO_CHARACTERS',activeCharacterId:active?.characterId??null,characters:Object.freeze(Object.values(roster).map(projectCharacterForSheet))});
}
