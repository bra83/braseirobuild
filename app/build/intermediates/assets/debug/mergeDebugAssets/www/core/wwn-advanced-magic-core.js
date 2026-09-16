export const WWN_ADVANCED_MAGIC_SOURCES=Object.freeze({
  research:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.7.0–4.7.2 Developing New Spells'}),
  workings:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.8.0–4.8.2 Building Magical Workings'}),
  scrolls:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.9.1 Scrolls'}),
  potions:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.9.2 Potions'}),
  magicGear:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.9.3–4.9.4 Magic Weapons, Armor, and Devices'}),
  itemCreation:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.10.0 Creating Magic Items'}),
});
const f=v=>Object.freeze(v);
const int=(v,n)=>{if(!Number.isInteger(v))throw new TypeError(`${n} must be integer`);return v;};
const level=v=>{int(v,'spellLevel');if(v<1||v>5)throw new RangeError('spellLevel must be 1..5');return v;};

export const SPELL_RESEARCH=f({
  1:f({labCostSp:50000,materialsCostSp:25000,time:f({value:1,unit:'MONTH'})}),
  2:f({labCostSp:125000,materialsCostSp:50000,time:f({value:2,unit:'MONTH'})}),
  3:f({labCostSp:250000,materialsCostSp:100000,time:f({value:4,unit:'MONTH'})}),
  4:f({labCostSp:500000,materialsCostSp:200000,time:f({value:8,unit:'MONTH'})}),
  5:f({labCostSp:1000000,materialsCostSp:400000,time:f({value:2,unit:'YEAR'})}),
});
export const SPELL_FORMULA_FLAWS=f({
  1:'CASTER_ONLY',2:'WILLING_TARGETS_ONLY',3:'NO_CUMULATIVE_RESEARCH_BONUS',4:'OTHERS_ONLY',5:'UNUSUALLY_SLOW',6:'DOUBLE_SPELL_ENERGY',7:'BACKLASH_1D4_SYSTEM_STRAIN',8:'ACTUAL_LEVEL_PLUS_ONE',9:'UNRELIABLE_FIZZLE_ON_1_IN_1D6',10:'TOTAL_RESEARCH_LOSS',
});
export function spellResearchPlan({spellLevel,doubleMaterials=false,gmApproved=false,gmAssignedLevel=false}={}){
  const l=level(spellLevel),row=SPELL_RESEARCH[l];
  if(!gmApproved||!gmAssignedLevel)return f({status:'EXPLICIT_CONTEXT_CONTRACT',contextRequired:['GM_APPROVES_SPELL_CONCEPT','GM_ASSIGNS_APPROPRIATE_LEVEL'],spellLevel:l,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
  return f({status:'OK',spellLevel:l,labCostSp:row.labCostSp,materialsCostSp:row.materialsCostSp*(doubleMaterials?2:1),researchTime:doubleMaterials?f({...row.time,fraction:0.5}):row.time,oneSpellResearchAtATime:true,canAdventureDuringResearch:true,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
}
export function spellResearchCheckPlan({spellLevel,apprentices=false,specialistOwnField=false,specialResourceBonus=0}={}){
  const l=level(spellLevel);int(specialResourceBonus,'specialResourceBonus');
  return f({status:'OK',check:'Int/Magic',difficulty:10+l,bonuses:f({apprentices:apprentices?1:0,specialistOwnField:specialistOwnField?1:0,specialResourceBonus}),sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
}
export function spellResearchFailurePlan({continuationCount=0,acceptFlaw=false,flawRoll=null}={}){
  int(continuationCount,'continuationCount');if(continuationCount<0)throw new RangeError('continuationCount must be >=0');
  if(!acceptFlaw)return f({status:'RESTART_FROM_SCRATCH',newResourcesRequired:true,newFullResearchTimeRequired:true,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
  if(!Number.isInteger(flawRoll)||flawRoll<1||flawRoll>10)return f({status:'FLAW_ROLL_REQUIRED',formula:'1d10',sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
  const flaw=SPELL_FORMULA_FLAWS[flawRoll];const noBonus=flawRoll===3;const totalLoss=flawRoll===10;
  return f({status:totalLoss?'TOTAL_RESEARCH_LOSS':'CONTINUE_WITH_FLAW',flawRoll,flaw,nextResearchTimeFraction:totalLoss?1:0.5,additionalResourcesRequired:totalLoss,cumulativeCheckBonus:totalLoss?0:(noBonus?0:continuationCount+1),gmAdjustsFlaw:true,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.research]});
}

export const WORKING_DEGREES=f({TRIVIAL:f({min:1,max:4}),MINOR:f({min:4,max:8}),MAJOR:f({min:8,max:16}),GREAT:f({min:16,max:32}),SUPREME:f({min:33,max:64,demiDivineOnly:true})});
export const WORKING_AREAS=f({ROOM:1,BUILDING:4,VILLAGE:16,CITY:64,REGION:256});
export function workingEffectDifficulty({effectPoints,area='ROOM'}={}){
  if(!Array.isArray(effectPoints)||effectPoints.length<1||effectPoints.some(v=>!Number.isInteger(v)||v<1))throw new RangeError('effectPoints must be positive integers');
  const mult=WORKING_AREAS[String(area).toUpperCase()];if(!mult)throw new RangeError('invalid Working area');
  const sorted=[...effectPoints].sort((a,b)=>b-a);const base=sorted[0]+Math.ceil(sorted.slice(1).reduce((a,b)=>a+b,0)/2);return f({status:'OK',baseDifficulty:base,area:String(area).toUpperCase(),areaMultiplier:mult,finalDifficulty:base*mult,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.workings]});
}
export function workingDesignPlan({architectLevel,magicSkill,difficulty,isSpellcaster=true,gmEffectsApproved=false}={}){
  int(architectLevel,'architectLevel');int(magicSkill,'magicSkill');int(difficulty,'difficulty');if(architectLevel<1||magicSkill<0||difficulty<1)throw new RangeError('invalid Working design inputs');
  if(!gmEffectsApproved)return f({status:'EXPLICIT_CONTEXT_CONTRACT',contextRequired:['GM_DEFINES_AND_PRICES_CUSTOM_EFFECTS'],sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.workings]});
  if(!isSpellcaster||architectLevel<6)return f({status:'INELIGIBLE',reason:'SPELLCASTER_LEVEL_6_REQUIRED',sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.workings]});
  const capacity=architectLevel*magicSkill*2;const designStatus=capacity>=difficulty?'SOLO':(capacity*2>=difficulty?'ASSISTANCE_REQUIRED':'TOO_DIFFICULT');
  return f({status:'OK',capacity,difficulty,designStatus,missingAssistancePoints:designStatus==='ASSISTANCE_REQUIRED'?difficulty-capacity:0,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.workings]});
}
export function workingConstructionPlan({difficulty,doubleCostForHalfTime=false}={}){
  int(difficulty,'difficulty');if(difficulty<1)throw new RangeError('difficulty must be positive');
  const baseCostSp=1000*difficulty;const weeksBeyondMonth=Math.ceil(difficulty/5);
  return f({status:'OK',costSp:baseCostSp*(doubleCostForHalfTime?2:1),baseTime:f({months:1,weeks:weeksBeyondMonth}),timeMultiplier:doubleCostForHalfTime?0.5:1,siteMustAlreadyExist:true,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.workings]});
}

export function scrollUsePlan({spellLevel,magicSkill}={}){
  const l=level(spellLevel);int(magicSkill,'magicSkill');if(magicSkill>=l-1)return f({status:'SAFE',action:'MAIN',spellLevel:l,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.scrolls]});
  return f({status:'RISKY_CHECK_REQUIRED',action:'MAIN',checkOptions:['Int/Magic','Cha/Magic'],difficulty:8+l,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.scrolls]});
}
export function resolveRiskyScrollCheck({spellLevel,total}={}){
  const l=level(spellLevel);int(total,'total');const difficulty=8+l,margin=difficulty-total;
  return f({status:total>=difficulty?'CAST_AS_INTENDED':(margin<=2?'FIZZLES_AND_WASTED':'MISCAST_GM_TARGET_OR_EFFECT'),difficulty,total,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.scrolls]});
}
export function scrollCreationPlan({spellLevel}={}){
  const l=level(spellLevel);return f({status:'OK',costSp:1000*l,creationDifficulty:7+Math.ceil(l/2),time:f({value:1,unit:'WEEK'}),maxFrequency:f({oncePerWeeks:l}),singleUse:true,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.scrolls]});
}
export function potionIdentificationPlan({creationDifficulty}={}){
  int(creationDifficulty,'creationDifficulty');if(creationDifficulty<2)throw new RangeError('creationDifficulty must be >=2');return f({status:'OK',analysisTime:f({value:1,unit:'DAY'}),checkOptions:['Wis/Magic','Int/Magic'],difficulty:creationDifficulty,attemptsPerInvestigator:1,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.potions]});
}
export function magicalWeaponArmorBonus({kind,bonus}={}){
  const k=String(kind??'').toUpperCase();int(bonus,'bonus');if(!['WEAPON','ARMOR','SHIELD'].includes(k))throw new RangeError('kind must be WEAPON, ARMOR, or SHIELD');
  if(k==='SHIELD')return f({status:bonus===0?'OK':'INVALID',numericBonusAllowed:false,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.magicGear]});
  if(bonus<1||bonus>3)throw new RangeError('magic weapon/armor bonus must be 1..3');return f({status:'OK',kind:k,bonus,appliesTo:k==='WEAPON'?['HIT','DAMAGE','SHOCK']:['BASE_ARMOR_CLASS'],sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.magicGear]});
}

export const MAGIC_ITEM_CREATION_BANDS=f({
  SINGLE_USE:f({difficultyMin:8,difficultyMax:10,costSpMin:250,costSpMax:2500,time:f({value:1,unit:'WEEK'}),adventureComponentRequired:false}),
  LIMITED_CHARGES:f({difficultyMin:9,difficultyMax:11,costSpMin:5000,costSpMax:25000,time:f({value:1,unit:'MONTH'}),adventureComponentRequired:true}),
  LOW_POWER_PERMANENT:f({difficultyMin:10,difficultyMax:10,costSpMin:12500,costSpMax:12500,time:f({value:1,unit:'MONTH'}),adventureComponentRequired:true}),
  SIGNIFICANT_PERMANENT:f({difficultyMin:12,difficultyMax:12,costSpMin:50000,costSpMax:50000,time:f({value:3,unit:'MONTH'}),adventureComponentRequired:true}),
  POWERFUL_PERMANENT:f({difficultyMin:14,difficultyMax:14,costSpMin:250000,costSpMax:250000,time:f({value:6,unit:'MONTH'}),adventureComponentRequired:true}),
});
export function magicItemCreationPlan({band,difficulty,costSp,batchSize=1,apprentice=false,gmApproved=false,requiredAdventureComplete=false}={}){
  const b=MAGIC_ITEM_CREATION_BANDS[String(band??'').toUpperCase()];if(!b)throw new RangeError('invalid magic item creation band');int(difficulty,'difficulty');int(costSp,'costSp');int(batchSize,'batchSize');
  if(!gmApproved)return f({status:'EXPLICIT_CONTEXT_CONTRACT',contextRequired:['GM_APPROVES_ITEM_CONCEPT','GM_SELECTS_APPROPRIATE_DIFFICULTY_AND_COST_WITHIN_SRD_BAND'],sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]});
  if(difficulty<b.difficultyMin||difficulty>b.difficultyMax||costSp<b.costSpMin||costSp>b.costSpMax)return f({status:'INVALID_GM_PARAMETERS_OUTSIDE_SRD_BAND',band:b,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]});
  if(![1,2,4].includes(batchSize))throw new RangeError('batchSize must be 1, 2, or 4');if(batchSize>1&&String(band).toUpperCase()!=='SINGLE_USE')throw new RangeError('batching applies to limited-use/single-use items');
  if(b.adventureComponentRequired&&!requiredAdventureComplete)return f({status:'ADVENTURE_COMPONENT_REQUIRED_NO_MUTATION',sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]});
  const batchDifficultyModifier=batchSize===2?1:(batchSize===4?2:0);
  return f({status:'OK',band:String(band).toUpperCase(),baseDifficulty:difficulty,batchSize,batchDifficultyModifier,creationDifficulty:difficulty+batchDifficultyModifier,costSp,time:b.time,check:'Int/Magic',checkBonus:apprentice?1:0,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]});
}
export function magicItemFailurePlan({continuationCount=0,acceptFlaw=false}={}){
  int(continuationCount,'continuationCount');if(continuationCount<0)throw new RangeError('continuationCount must be >=0');
  return acceptFlaw?f({status:'CONTINUE_WITH_GM_ADJUDICATED_FLAW',nextTimeFraction:0.5,additionalCoinRequired:false,repeatAdventureRequired:false,cumulativeCheckBonus:continuationCount+1,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]}):f({status:'RESTART_FROM_BEGINNING',newCoinRequired:true,repeatAdventureRequired:false,sourceRefs:[WWN_ADVANCED_MAGIC_SOURCES.itemCreation]});
}
