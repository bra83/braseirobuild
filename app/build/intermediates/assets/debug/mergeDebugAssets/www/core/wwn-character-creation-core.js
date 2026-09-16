import { DeterministicRng, RNG_ALGORITHM } from './rng-core.js';
import { attributeModifier } from './wwn-rules-core.js';
import { computeSaveTargets, parseDiceExpression } from './wwn-combat-core.js';
import { classBaseAttackBonus, classCapabilities, classHitDiceExpression, validateClassCreationContract, WWN_CLASS_SOURCES } from './wwn-class-core.js';
import { deriveAdventurerChassis, deriveFullClassChassis } from './wwn-srd-class-combination.js';
import { classArtCatalog, classProfile, levelOneSpellCatalog, WWN_SRD_CLASS_SOURCES } from './wwn-srd-class-catalog.js';
import { recomputeLevelOneTrainingEffort } from './wwn-srd-random-class.js';

export const WWN_CHARACTER_CREATION_SOURCES = Object.freeze({
  attributes: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.1 Attributes' }),
  skills: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.2.2 Gaining Skills in Character Creation' }),
  backgrounds: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.3.0–1.3.3 Backgrounds' }),
  foci: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.6.0–1.6.1 Foci' }),
  finalSteps: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.7.0–1.7.9 Final Character Creation Steps' }),
  languages: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.7.6 Choose Starting Languages' }),
  gear: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§1.7.7–1.7.8 Starting Gear / Weapon and Armor Statistics' }),
});

const ATTRIBUTES = Object.freeze(['Strength','Dexterity','Constitution','Intelligence','Wisdom','Charisma']);
const COMBAT_SKILLS = new Set(['Shoot','Stab','Punch']);

function sourceBound(value,label){
  if(!value?.sourceRef?.sourceId && !value?.sourceRef?.id) throw new Error(`${label} requires sourceRef`);
}
function requireName(value,label){if(typeof value!=='string'||!value.trim())throw new Error(`${label} required`);return value.trim();}
function clone(v){return structuredClone(v);}

export function validateCreationAttributes(attributes){
  const errors=[];
  for(const name of ATTRIBUTES){const value=attributes?.[name];if(!Number.isInteger(value)||value<3||value>18)errors.push(`${name} must be 3..18`);}
  return Object.freeze({ok:errors.length===0,errors:Object.freeze(errors),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.attributes]});
}

export function applyCreationSkillPicks(initial={}, picks=[]){
  const skills={...initial};
  for(const raw of picks){
    const skill=requireName(raw,'skill'); const current=skills[skill];
    if(current===undefined){skills[skill]=0;continue;}
    if(current===0){skills[skill]=1;continue;}
    if(current===1) return Object.freeze({ok:false,status:'SKILL_ALREADY_LEVEL_1_REPLACEMENT_REQUIRED',skill,skills:Object.freeze(skills),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.skills]});
    return Object.freeze({ok:false,status:'INVALID_CREATION_SKILL_LEVEL',skill,skills:Object.freeze(skills),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.skills]});
  }
  return Object.freeze({ok:true,status:'OK',skills:Object.freeze(skills),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.skills]});
}

export function validateBackgroundContract(background){
  if(!background||typeof background!=='object')return Object.freeze({ok:false,status:'BACKGROUND_REQUIRED'});
  try{sourceBound(background,'background');requireName(background.backgroundId??background.name,'background id');requireName(background.freeSkill,'background free skill');}
  catch(error){return Object.freeze({ok:false,status:'BACKGROUND_SOURCE_OR_FIELDS_REQUIRED',error:String(error.message)});}
  if(background.mode==='PICK'){
    if(!Array.isArray(background.learningPicks)||background.learningPicks.length!==2)return Object.freeze({ok:false,status:'BACKGROUND_PICK_REQUIRES_TWO_LEARNING_SKILLS'});
    if(background.learningPicks.some(v=>String(v).toUpperCase()==='ANY SKILL'))return Object.freeze({ok:false,status:'ANY_SKILL_NOT_ALLOWED_IN_PICK_MODE'});
  }else if(background.mode==='ROLL'){
    if(!Array.isArray(background.resolvedRollPicks)||background.resolvedRollPicks.length!==3)return Object.freeze({ok:false,status:'BACKGROUND_ROLL_REQUIRES_THREE_RESOLVED_PICKS'});
    if(background.resolvedRollPicks.some(v=>!v?.sourceRef))return Object.freeze({ok:false,status:'BACKGROUND_ROLL_PICK_SOURCE_REQUIRED'});
  }else return Object.freeze({ok:false,status:'BACKGROUND_MODE_REQUIRED'});
  return Object.freeze({ok:true,status:'OK',sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.backgrounds,background.sourceRef]});
}

function focusRequirements(classContract,classResult=null){
  if(Array.isArray(classResult?.focusRequirements)){
    const req=classResult.focusRequirements;return {any:req.filter(x=>x==='ANY').length,combat:req.filter(x=>x==='WARRIOR').length,nonCombat:req.filter(x=>x==='EXPERT').length,total:req.length};
  }
  const id=String(classContract.classId??'').toUpperCase(); let any=1,combat=0,nonCombat=0;
  if(id==='FULL_WARRIOR')combat+=1;
  if(id==='FULL_EXPERT')nonCombat+=1;
  if(id==='ADVENTURER'){
    const parts=(classContract.partialClasses??[]).map(v=>String(v).toUpperCase());
    if(parts.includes('PARTIAL_WARRIOR'))combat+=1;
    if(parts.includes('PARTIAL_EXPERT'))nonCombat+=1;
  }
  return {any,combat,nonCombat,total:any+combat+nonCombat};
}

export function validateStartingFoci({classContract,foci,classResult=null}){
  if(!Array.isArray(foci))return Object.freeze({ok:false,status:'FOCI_REQUIRED'});
  const req=focusRequirements(classContract,classResult);
  if(foci.length!==req.total)return Object.freeze({ok:false,status:'FOCUS_COUNT_MISMATCH',required:req.total,actual:foci.length});
  for(const focus of foci){try{sourceBound(focus,'focus');requireName(focus.focusId??focus.name,'focus id');}catch(error){return Object.freeze({ok:false,status:'FOCUS_SOURCE_REQUIRED',error:String(error.message)});}if(!['COMBAT','NON_COMBAT','ANY'].includes(String(focus.category??'ANY').toUpperCase()))return Object.freeze({ok:false,status:'FOCUS_CATEGORY_INVALID'});}
  const explicitCombat=foci.filter(f=>String(f.category).toUpperCase()==='COMBAT').length;
  const explicitNonCombat=foci.filter(f=>String(f.category).toUpperCase()==='NON_COMBAT').length;
  if(explicitCombat<req.combat)return Object.freeze({ok:false,status:'COMBAT_FOCUS_REQUIREMENT_NOT_MET',required:req.combat});
  if(explicitNonCombat<req.nonCombat)return Object.freeze({ok:false,status:'NON_COMBAT_FOCUS_REQUIREMENT_NOT_MET',required:req.nonCombat});
  return Object.freeze({ok:true,status:'OK',requirements:Object.freeze(req),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.foci,...foci.map(f=>f.sourceRef)]});
}

export function additionalLanguageSlots(skills){
  const value=(name)=>skills?.[name]===1?2:skills?.[name]===0?1:0;
  return Object.freeze({slots:value('Know')+value('Connect'),sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.languages]});
}

function expectedSrdSpellCount(path){
  const ids=path.kind==='FULL'?[path.classId]:path.partialClasses;const profiles=ids.map(classProfile);if(profiles.some(p=>!p))return null;
  if(path.kind==='FULL')return profiles[0].initialTraining.spellKnownFull??0;
  const casters=profiles.filter(p=>p.initialTraining.spellcasting===true);if(casters.length>=2)return 4;if(casters.length===1)return casters[0].initialTraining.spellKnownPartial??2;return 0;
}
function validateSrdTraining({path,training}){
  if(!training||training.status!=='OK'||!Array.isArray(training.classes))return Object.freeze({ok:false,status:'SRD_LEVEL1_TRAINING_REQUIRED'});
  const expectedIds=path.kind==='FULL'?[String(path.classId).toUpperCase()]:path.partialClasses.map(v=>String(v).toUpperCase());
  const gotIds=training.classes.map(c=>String(c.classId).toUpperCase());if(JSON.stringify(gotIds)!==JSON.stringify(expectedIds))return Object.freeze({ok:false,status:'SRD_TRAINING_CLASS_PATH_MISMATCH'});
  for(const entry of training.classes){const p=classProfile(entry.classId);if(!p||!entry.sourceRef)return Object.freeze({ok:false,status:'SRD_TRAINING_SOURCE_REQUIRED'});const arts=classArtCatalog(entry.classId);if((entry.arts??[]).some(a=>!arts.includes(a)))return Object.freeze({ok:false,status:'SRD_TRAINING_UNKNOWN_ART',classId:entry.classId});for(const fixed of p.initialTraining.fixedArts??[])if(!(entry.arts??[]).includes(fixed))return Object.freeze({ok:false,status:'SRD_TRAINING_FIXED_ART_MISSING',classId:entry.classId,art:fixed});}
  const expected=expectedSrdSpellCount(path);if(expected===null)return Object.freeze({ok:false,status:'SRD_TRAINING_UNKNOWN_CLASS'});const sc=training.spellcasting??{};if(sc.knownCount!==expected||!Array.isArray(sc.startingSpells)||sc.startingSpells.length!==expected)return Object.freeze({ok:false,status:'SRD_STARTING_SPELL_COUNT_MISMATCH',required:expected,actual:sc.startingSpells?.length??0});const allowed=[...new Set((sc.lists??[]).flatMap(levelOneSpellCatalog))];if(sc.startingSpells.some(x=>!allowed.includes(x)))return Object.freeze({ok:false,status:'SRD_STARTING_SPELL_UNKNOWN'});
  return Object.freeze({ok:true,status:'OK'});
}
function resolveSrdClassLevelOne(classContract){
  const path=classContract?.path;if(!path)return Object.freeze({ok:false,status:'SRD_CLASS_PATH_REQUIRED'});let chassis;
  if(path.kind==='FULL')chassis=deriveFullClassChassis({classId:path.classId,level:1});else if(path.kind==='ADVENTURER')chassis=deriveAdventurerChassis({partialClasses:path.partialClasses,level:1});else return Object.freeze({ok:false,status:'INVALID_CLASS_PATH'});
  if(chassis.status!=='OK')return Object.freeze({ok:false,status:chassis.status});const trainingCheck=validateSrdTraining({path,training:classContract.training});if(!trainingCheck.ok)return trainingCheck;
  return Object.freeze({ok:true,status:'OK',srd:true,path:Object.freeze(structuredClone(path)),training:classContract.training,hitDiceExpression:chassis.hitDiceExpression,baseAttackBonus:chassis.baseAttackBonus,focusRequirements:chassis.focusRequirements,classAbilities:chassis.classAbilities,sourceRefs:Object.freeze([...(chassis.sourceRefs??[]),...(classContract.training.sourceRefs??[]),WWN_SRD_CLASS_SOURCES.startingSpells])});
}
function resolveClassLevelOne(classContract){
  if(classContract?.ruleset==='WWN_SRD_2024')return resolveSrdClassLevelOne(classContract);
  const validity=validateClassCreationContract(classContract); if(!validity.ok)return validity;
  const id=String(classContract.classId).toUpperCase();
  if(id==='FULL_MAGE'||id==='MAGE'){
    const table=classContract.mageTradition?.fullClassTable?.[1]??classContract.mageTradition?.levelOne;
    if(!table?.sourceRef||typeof table.hitDice!=='string'||!Number.isInteger(table.baseAttackBonus))return Object.freeze({ok:false,status:'MAGE_TRADITION_LEVEL1_TABLE_REQUIRED'});
    return Object.freeze({ok:true,status:'OK',hitDiceExpression:table.hitDice,baseAttackBonus:table.baseAttackBonus,sourceRefs:[WWN_CLASS_SOURCES.mage,classContract.mageTradition.sourceRef,table.sourceRef]});
  }
  if(id==='ADVENTURER'&&(classContract.partialClasses??[]).every(v=>String(v).toUpperCase()==='PARTIAL_MAGE')){
    const table=classContract.dualMageTable?.[1]??classContract.dualMageLevelOne;
    if(!table?.sourceRef||typeof table.hitDice!=='string'||!Number.isInteger(table.baseAttackBonus))return Object.freeze({ok:false,status:'DUAL_PARTIAL_MAGE_LEVEL1_TABLE_REQUIRED'});
    return Object.freeze({ok:true,status:'OK',hitDiceExpression:table.hitDice,baseAttackBonus:table.baseAttackBonus,sourceRefs:[WWN_CLASS_SOURCES.adventurer,table.sourceRef]});
  }
  const hd=classHitDiceExpression({classId:id,level:1,partialClasses:classContract.partialClasses});
  const bab=classBaseAttackBonus({classId:id,level:1,partialClasses:classContract.partialClasses});
  if(hd.status!=='OK'||bab.status!=='OK')return Object.freeze({ok:false,status:hd.status!=='OK'?hd.status:bab.status});
  return Object.freeze({ok:true,status:'OK',hitDiceExpression:hd.expression,baseAttackBonus:bab.baseAttackBonus,sourceRefs:[...(hd.sourceRefs??[]),...(bab.sourceRefs??[])]});
}

function rollStartingHp({rng,expression,constitutionScore}){
  const spec=parseDiceExpression(expression);if(spec.count!==1)throw new Error('level-one hit dice expression must roll exactly one die');
  const die=rng.die(spec.sides,'character-creation/hp'); const con=attributeModifier(constitutionScore); const hp=Math.max(1,die.result+spec.bonus+con);
  return Object.freeze({hp,die,expression,constitutionModifier:con,sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.finalSteps]});
}

function resolveGear({gear,rng}){
  if(!gear||typeof gear!=='object')return Object.freeze({ok:false,status:'STARTING_GEAR_CHOICE_REQUIRED'});
  if(gear.mode==='ROLL_SILVER'){
    const dice=[rng.die(6,'character-creation/starting-silver'),rng.die(6,'character-creation/starting-silver'),rng.die(6,'character-creation/starting-silver')];
    const silver=dice.reduce((a,d)=>a+d.result,0)*10;
    return Object.freeze({ok:true,status:'OK',inventory:Object.freeze([]),silver,dice,sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.gear]});
  }
  if(gear.mode==='GM_PACKAGE'){
    if(!gear.package?.sourceRef||!Array.isArray(gear.package.items))return Object.freeze({ok:false,status:'SOURCE_BOUND_GM_PACKAGE_REQUIRED'});
    if(gear.package.items.some(i=>!i?.sourceRef))return Object.freeze({ok:false,status:'GEAR_ITEM_SOURCE_REQUIRED'});
    return Object.freeze({ok:true,status:'OK',inventory:Object.freeze(clone(gear.package.items)),silver:Number.isInteger(gear.package.silver)?gear.package.silver:0,sourceRefs:[WWN_CHARACTER_CREATION_SOURCES.gear,gear.package.sourceRef,...gear.package.items.map(i=>i.sourceRef)]});
  }
  return Object.freeze({ok:false,status:'INVALID_STARTING_GEAR_MODE'});
}

export function finalizeCharacterCreation({draft,masterSeed='character-creation'}){
  if(!draft||typeof draft!=='object')return Object.freeze({ok:false,status:'DRAFT_REQUIRED'});
  const attr=validateCreationAttributes(draft.attributes);if(!attr.ok)return Object.freeze({ok:false,status:'INVALID_ATTRIBUTES',errors:attr.errors});
  const bg=validateBackgroundContract(draft.background);if(!bg.ok)return bg;
  const classResult=resolveClassLevelOne(draft.classContract??{});if(!classResult.ok)return classResult;
  const foci=validateStartingFoci({classContract:draft.classContract??{},foci:draft.foci,classResult});if(!foci.ok)return foci;
  const rng=new DeterministicRng(masterSeed);
  let skillPickNames=[draft.background.freeSkill];
  if(draft.background.mode==='PICK')skillPickNames.push(...draft.background.learningPicks);
  else for(const pick of draft.background.resolvedRollPicks){if(pick.kind==='SKILL')skillPickNames.push(pick.skill);}
  if(classResult.srd)for(const c of classResult.training.classes){if(c.bonusSkill)skillPickNames.push(c.bonusSkill);}
  for(const focus of draft.foci){for(const skill of focus.bonusSkills??[])skillPickNames.push(skill);}
  if(draft.freeSkill)skillPickNames.push(draft.freeSkill); else return Object.freeze({ok:false,status:'FREE_SKILL_REQUIRED'});
  const skillResult=applyCreationSkillPicks({},skillPickNames);if(!skillResult.ok)return skillResult;
  const languageSlots=additionalLanguageSlots(skillResult.skills).slots;
  const languages=Array.isArray(draft.languages)?draft.languages.map(v=>String(v).trim()).filter(Boolean):[];
  if(typeof draft.linguaFranca!=='string'||!draft.linguaFranca.trim())return Object.freeze({ok:false,status:'LINGUA_FRANCA_REQUIRED'});
  const baselineLanguages=new Set([draft.linguaFranca.trim(),...(draft.nativeLanguage&&draft.nativeLanguage!==draft.linguaFranca?[draft.nativeLanguage.trim()]:[])]);
  const extras=languages.filter(v=>!baselineLanguages.has(v));
  if(extras.length!==languageSlots)return Object.freeze({ok:false,status:'LANGUAGE_SLOT_MISMATCH',required:languageSlots,actual:extras.length});
  const gear=resolveGear({gear:draft.gear,rng});if(!gear.ok)return gear;
  const hp=rollStartingHp({rng,expression:classResult.hitDiceExpression,constitutionScore:draft.attributes.Constitution});
  const saves=computeSaveTargets({level:1,attributes:draft.attributes});
  let classId,partialClasses,capabilities,magic=null,arts=[],effort=null,classTraining=null;
  if(classResult.srd){
    classId=classResult.path.kind==='FULL'?String(classResult.path.classId).toUpperCase():'ADVENTURER';partialClasses=classResult.path.kind==='ADVENTURER'?[...classResult.path.partialClasses]:[];capabilities=Object.freeze([...(classResult.classAbilities??[])]);
    const adjusted=recomputeLevelOneTrainingEffort({training:classResult.training,attributes:draft.attributes,skills:skillResult.skills});if(adjusted.status!=='OK')return Object.freeze({ok:false,status:adjusted.status});classTraining=adjusted;
    arts=adjusted.classes.flatMap(c=>(c.arts??[]).map(a=>Object.freeze({artId:a,name:a,classId:c.classId,sourceRef:c.sourceRef})));
    const spellNames=adjusted.spellcasting?.startingSpells??[];const startingSpells=spellNames.map(name=>Object.freeze({spellId:name,name,sourceRef:WWN_SRD_CLASS_SOURCES.startingSpells}));
    const profiles=(classResult.path.kind==='FULL'?[classResult.path.classId]:classResult.path.partialClasses).map(classProfile);const usesSpellPoints=profiles.some(p=>p?.special?.usesSpellPoints===true);
    if(startingSpells.length)magic=Object.freeze({startingSpells:Object.freeze(startingSpells),spellLists:Object.freeze([...(adjusted.spellcasting?.lists??[])]),castingMode:usesSpellPoints?'SPELL_POINTS':'SPELLCASTING'});
    const pools={};for(const c of adjusted.classes)if(Number.isInteger(c.effortMax))pools[c.classId]=Object.freeze({current:c.effortMax,max:c.effortMax,committed:Object.freeze({})});effort=Object.keys(pools).length?Object.freeze(pools):null;
  }else{
    classId=String(draft.classContract.classId).toUpperCase();partialClasses=clone(draft.classContract.partialClasses??[]);capabilities=classId==='ADVENTURER'?Object.freeze({status:'ADVENTURER',partialClasses:clone(draft.classContract.partialClasses)}):classCapabilities(classId);
    const isMage=classId==='FULL_MAGE'||classId==='MAGE'||(draft.classContract.partialClasses??[]).some(v=>String(v).toUpperCase()==='PARTIAL_MAGE');let startingSpellCount=0;
    if(isMage){const spellcasting=draft.classContract.mageTradition?.spellcasting!==false || (draft.classContract.mageTraditions??[]).some(t=>t.spellcasting!==false);if(spellcasting){startingSpellCount=(draft.classContract.partialClasses??[]).filter(v=>String(v).toUpperCase()==='PARTIAL_MAGE').length===2?4:(classId==='FULL_MAGE'||classId==='MAGE'?4:2);}if(startingSpellCount>0){if(!Array.isArray(draft.startingSpells)||draft.startingSpells.length!==startingSpellCount||draft.startingSpells.some(s=>!s?.sourceRef))return Object.freeze({ok:false,status:'SOURCE_BOUND_STARTING_SPELLS_REQUIRED',required:startingSpellCount});}magic=Object.freeze({tradition:clone(draft.classContract.mageTradition??null),traditions:clone(draft.classContract.mageTraditions??[]),startingSpells:clone(draft.startingSpells??[])});}
  }
  const character=Object.freeze({
    characterId:requireName(draft.characterId??'pc-1','characterId'),name:requireName(draft.name,'name'),goal:requireName(draft.goal,'goal'),
    classId,partialClasses:Object.freeze(partialClasses),classPath:classResult.srd?Object.freeze(clone(classResult.path)):null,level:1,xp:0,attributes:clone(draft.attributes),skills:clone(skillResult.skills),
    baseAttackBonus:classResult.baseAttackBonus,hp:Object.freeze({current:hp.hp,max:hp.hp}),systemStrain:Object.freeze({current:0,max:draft.attributes.Constitution}),
    savingThrows:saves,foci:clone(draft.foci),classAbilities:capabilities,arts:Object.freeze(arts),inventory:clone(gear.inventory),silver:gear.silver,languages:[...baselineLanguages,...extras],magic,effort,classTraining,
    creationProvenance:Object.freeze({rule:classResult.srd?'WWN_CHARACTER_CREATION_V2_SRD':'WWN_CHARACTER_CREATION_V1',rngAlgorithm:RNG_ALGORITHM,sourceRefs:[...attr.sourceRefs,...bg.sourceRefs,...foci.sourceRefs,...classResult.sourceRefs,...gear.sourceRefs,WWN_CHARACTER_CREATION_SOURCES.finalSteps],hpRoll:hp,rngState:rng.exportState()}),
  });
  return Object.freeze({ok:true,status:'OK',character,sourceRefs:character.creationProvenance.sourceRefs});
}

