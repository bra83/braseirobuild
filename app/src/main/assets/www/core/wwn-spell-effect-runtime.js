import { WWN_SPELL_CATALOG, spellById } from './wwn-spell-catalog.js';

const SRC=(locator)=>Object.freeze({sourceId:'SRC-WWN-SRD-1',locator});
export const WWN_SPELL_EFFECT_SOURCES=Object.freeze({
  coffin:SRC('§4.4.0 The Coruscating Coffin'),
  wind:SRC('§4.4.0 The Wind of the Final Repose'),
  echo:SRC('§4.4.0 Resounding Temporal Echo'),
  velocitous:SRC('§4.4.0 Velocitous Imbuement'),
  wardpact:SRC('§4.4.0 Wardpact Invocation'),
  smite:SRC('§4.6.0 Smite the Dead'),
  corpse:SRC('§4.6.0 Raise Corpse'),
  guardian:SRC('§4.5.0 Elemental Guardian'),
  everlasting:SRC('§4.6.0 Everlasting'),
  clement:SRC('§4.4.0 Visitation of the Clement Clime'),
  scorn:SRC('§4.4.0 Scorn the Fetters of Earth'),
  aeolian:SRC('§4.4.0 Sigil of Aeolian Auctoritas'),
  tumefaction:SRC('§4.4.0 The Torment of Tumefaction'),
  phobic:SRC('§4.4.0 Phobic Storm'),
  likeStones:SRC('§4.5.0 Like the Stones'),
  calcifying:SRC('§4.5.0 Calcifying Scourge'),
  augmentVitality:SRC('§4.6.0 Augment Mortal Vitality'),
  enfeebling:SRC('§4.6.0 Enfeebling Wave'),
  callTomb:SRC('§4.6.0 Call of the Tomb'),
});
const freeze=v=>Object.freeze(v);
const key=v=>String(v??'').trim().toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'');

const CLOSED=Object.freeze({
  THE_CORUSCATING_COFFIN:freeze({resolverId:'SPELL_CORUSCATING_COFFIN_V1',mode:'CLOSED_WITH_SAVE_AND_DAMAGE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.coffin])}),
  THE_WIND_OF_THE_FINAL_REPOSE:freeze({resolverId:'SPELL_WIND_FINAL_REPOSE_V1',mode:'CLOSED_CONDITION',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.wind])}),
  RESOUNDING_TEMPORAL_ECHO:freeze({resolverId:'SPELL_RESOUNDING_TEMPORAL_ECHO_V1',mode:'CLOSED_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.echo])}),
  VELOCITOUS_IMBUEMENT:freeze({resolverId:'SPELL_VELOCITOUS_IMBUEMENT_V1',mode:'CLOSED_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.velocitous])}),
  WARDPACT_INVOCATION:freeze({resolverId:'SPELL_WARDPACT_INVOCATION_V1',mode:'CLOSED_STATUS_WITH_MODE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.wardpact])}),
  SMITE_THE_DEAD:freeze({resolverId:'SPELL_SMITE_THE_DEAD_V1',mode:'CLOSED_DAMAGE_WITH_SAVE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.smite])}),
  RAISE_CORPSE:freeze({resolverId:'SPELL_RAISE_CORPSE_V1',mode:'CLOSED_ENTITY_TEMPLATE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.corpse])}),
  ELEMENTAL_GUARDIAN:freeze({resolverId:'SPELL_ELEMENTAL_GUARDIAN_V1',mode:'CLOSED_ENTITY_TEMPLATE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.guardian])}),
  EVERLASTING:freeze({resolverId:'SPELL_EVERLASTING_V1',mode:'CLOSED_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.everlasting])}),
  VISITATION_OF_THE_CLEMENT_CLIME:freeze({resolverId:'SPELL_CLEMENT_CLIME_V1',mode:'CLOSED_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.clement])}),
  SCORN_THE_FETTERS_OF_EARTH:freeze({resolverId:'SPELL_SCORN_FETTERS_V1',mode:'CLOSED_STATUS_WITH_MODE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.scorn])}),
  SIGIL_OF_AEOLIAN_AUCTORITAS:freeze({resolverId:'SPELL_AEOLIAN_AUCTORITAS_V1',mode:'CLOSED_AREA_SAVE_DAMAGE_OR_WEATHER',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.aeolian])}),
  THE_TORMENT_OF_TUMEFACTION:freeze({resolverId:'SPELL_TUMEFACTION_V1',mode:'CLOSED_CURSE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.tumefaction])}),
  PHOBIC_STORM:freeze({resolverId:'SPELL_PHOBIC_STORM_V1',mode:'CLOSED_MORALE_AREA',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.phobic])}),
  LIKE_THE_STONES:freeze({resolverId:'SPELL_LIKE_THE_STONES_V1',mode:'CLOSED_STATUS_WITH_MODE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.likeStones])}),
  CALCIFYING_SCOURGE:freeze({resolverId:'SPELL_CALCIFYING_SCOURGE_V1',mode:'CLOSED_SAVE_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.calcifying])}),
  AUGMENT_MORTAL_VITALITY:freeze({resolverId:'SPELL_AUGMENT_MORTAL_VITALITY_V1',mode:'CLOSED_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.augmentVitality])}),
  ENFEEBLING_WAVE:freeze({resolverId:'SPELL_ENFEEBLING_WAVE_V1',mode:'CLOSED_AREA_SAVE_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.enfeebling])}),
  CALL_OF_THE_TOMB:freeze({resolverId:'SPELL_CALL_OF_THE_TOMB_V1',mode:'CLOSED_AREA_SAVE_STATUS',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.callTomb])}),
});

export const WWN_SPELL_EFFECT_CONTRACT_CATALOG=freeze(WWN_SPELL_CATALOG.map(spell=>freeze({
  spellId:spell.spellId,name:spell.name,level:spell.level,tradition:spell.tradition,
  classification:CLOSED[spell.spellId]?.mode??'GM_ADJUDICATION_REQUIRED',
  resolverId:CLOSED[spell.spellId]?.resolverId??null,
  sourceRefs:freeze([...(spell.sourceRefs??[]),...(CLOSED[spell.spellId]?.sourceRefs??[])]),
})));
const BY_ID=new Map(WWN_SPELL_EFFECT_CONTRACT_CATALOG.map(x=>[x.spellId,x]));
export function spellEffectContract(spellId){const s=spellById(spellId);return s?BY_ID.get(s.spellId)??null:null;}
export function spellEffectCoverageAudit(){const closed=WWN_SPELL_EFFECT_CONTRACT_CATALOG.filter(x=>x.resolverId).length;return freeze({status:WWN_SPELL_EFFECT_CONTRACT_CATALOG.length===WWN_SPELL_CATALOG.length?'OK':'INCOMPLETE',spellCount:WWN_SPELL_CATALOG.length,contractCount:WWN_SPELL_EFFECT_CONTRACT_CATALOG.length,closedResolverCount:closed,adjudicationCount:WWN_SPELL_CATALOG.length-closed});}

function settledFaces(meta,sides,count){
  const rolls=meta?.rolls;if(!Array.isArray(rolls)||rolls.length!==count||rolls.some(v=>!Number.isInteger(v)||v<1||v>sides))throw new RangeError(`physical spell effect requires exactly ${count} settled d${sides} faces`);
  if(meta?.forcedSettle===true)throw new Error('forced physical settle is not authoritative');return rolls;
}
function halve(total,roundingPolicy){if(total%2===0)return total/2;const p=String(roundingPolicy??'').toUpperCase();if(p==='DOWN')return Math.floor(total/2);if(p==='UP')return Math.ceil(total/2);return null;}
export function resolveCoruscatingCoffin({casterLevel,targetHitDice=null,targetIsNpc=false,physicalSaveSucceeded=false,physicalDice,halfDamageRounding=null}){
  if(!Number.isInteger(casterLevel)||casterLevel<1)throw new RangeError('casterLevel must be >= 1');
  const rolls=settledFaces(physicalDice,8,casterLevel),rawDamage=rolls.reduce((a,b)=>a+b,0),half=physicalSaveSucceeded?halve(rawDamage,halfDamageRounding):rawDamage;
  if(physicalSaveSucceeded&&half===null)return freeze({status:'HALF_DAMAGE_ROUNDING_CONTRACT_REQUIRED_NO_EFFECT',rawDamage,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.coffin])});
  const inevitableSlay=targetIsNpc===true&&targetHitDice===1;
  return freeze({status:'OK',rule:'WWN_CORUSCATING_COFFIN_V1',resolverId:CLOSED.THE_CORUSCATING_COFFIN.resolverId,casterLevel,rolls:freeze([...rolls]),rawDamage,physicalSaveSucceeded:Boolean(physicalSaveSucceeded),damage:half,inevitableSlay,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.coffin])});
}
export function resolveWindFinalReposeTarget({hitDice,living=true,sleeps=true}){
  if(!Number.isInteger(hitDice)||hitDice<1)throw new RangeError('hitDice must be >= 1');const affected=living===true&&sleeps===true&&hitDice<=4;
  return freeze({status:'OK',rule:'WWN_WIND_FINAL_REPOSE_TARGET_V1',affected,condition:affected?'UNCONSCIOUS':null,wakesOn:affected?freeze(['DAMAGE','MAIN_ACTION','END_SCENE']):freeze([]),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.wind])});
}
export function resolveRaiseCorpseTemplate({casterLevel}){if(!Number.isInteger(casterLevel)||casterLevel<1)throw new RangeError('casterLevel must be >= 1');return freeze({status:'OK',rule:'WWN_RAISE_CORPSE_TEMPLATE_V1',maxActive:casterLevel,template:freeze({hitDice:1,armorClass:13,moveFeet:30,skillBonus:0,save:15,attackBonus:1,damage:'1d6',instinct:0,morale:12}),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.corpse])});}
export function resolveElementalGuardianTemplate({element}){const e=key(element);if(!['EARTH','WATER','FIRE','AIR'].includes(e))throw new RangeError('element must be EARTH, WATER, FIRE, or AIR');return freeze({status:'OK',rule:'WWN_ELEMENTAL_GUARDIAN_TEMPLATE_V1',element:e,maxActive:1,template:freeze({hitDice:e==='EARTH'?6:4,armorClass:e==='WATER'?18:15,moveFeet:40,skillBonus:1,save:13,instinct:0,morale:12,attackBonus:6,damage:'1d10',shock:e==='FIRE'?'5/-':null,canFly:e==='AIR'}),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.guardian])});}
export function resolveEverlastingStatus(){return freeze({status:'OK',rule:'WWN_EVERLASTING_STATUS_V1',radiusFeet:50,durationRounds:5,minimumHp:1,casterHpAtEnd:1,usesPerCreaturePerDay:1,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.everlasting])});}


function requireCasterLevel(casterLevel){if(!Number.isInteger(casterLevel)||casterLevel<1)throw new RangeError('casterLevel must be >= 1');return casterLevel;}
function uniqueIds(ids){if(!Array.isArray(ids)||ids.some(x=>!String(x??'').trim()))throw new TypeError('explicit target ids required');const out=[...new Set(ids.map(x=>String(x)))];if(out.length!==ids.length)throw new RangeError('duplicate target ids are not allowed');return out;}
export function resolveResoundingTemporalEcho({casterLevel,targetIds,physicalDice}){
  requireCasterLevel(casterLevel);const ids=uniqueIds(targetIds);if(ids.length<1||ids.length>casterLevel)throw new RangeError('Resounding Temporal Echo targets exceed caster level or are empty');
  const [durationRoll]=settledFaces(physicalDice,4,1);const durationRounds=durationRoll+1;
  return freeze({status:'OK',rule:'WWN_RESOUNDING_TEMPORAL_ECHO_V1',resolverId:CLOSED.RESOUNDING_TEMPORAL_ECHO.resolverId,targetIds:freeze(ids),maxTargets:casterLevel,durationRoll,durationRounds,extraMainActionsPerTurn:1,bonusActionCannotCastSpellsOrArts:true,systemStrainPerRoundBonusActionUsed:1,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.echo])});
}
export function resolveVelocitousImbuement({casterLevel,targetIds}){
  requireCasterLevel(casterLevel);const ids=uniqueIds(targetIds);if(ids.length<1||ids.length>casterLevel)throw new RangeError('Velocitous Imbuement targets exceed caster level or are empty');
  return freeze({status:'OK',rule:'WWN_VELOCITOUS_IMBUEMENT_V1',resolverId:CLOSED.VELOCITOUS_IMBUEMENT.resolverId,targetIds:freeze(ids),maxTargets:casterLevel,rangeFeet:100,groundMoveMultiplier:2,canTraverseWallsAndCeilings:true,avoidsFightingWithdrawal:true,canPassAroundArmedFoes:true,duration:'SCENE',systemStrainWhenAdvantageUsed:1,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.velocitous])});
}
export function resolveWardpactInvocation({casterLevel,mode,targetId=null,weaponId=null,targetIsCaster=false,naturalBodyWeapon=false}){
  requireCasterLevel(casterLevel);const m=key(mode);if(!['CREATURE','WEAPON'].includes(m))throw new RangeError('Wardpact mode must be CREATURE or WEAPON');
  if(m==='CREATURE'){if(!String(targetId??'').trim())throw new TypeError('Wardpact creature target required');if(targetIsCaster===true)throw new RangeError('Wardpact creature mode cannot target the caster');const durationRounds=Math.ceil(casterLevel/2);return freeze({status:'OK',rule:'WWN_WARDPACT_INVOCATION_V1',resolverId:CLOSED.WARDPACT_INVOCATION.resolverId,mode:m,targetId:String(targetId),rangeFeet:200,durationRounds,weaponHitRequiresAttackerPhysicalSave:true,failedSaveNegatesHit:true,oncePerTargetPerScene:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.wardpact])});}
  if(!String(weaponId??'').trim())throw new TypeError('Wardpact weapon target required');if(naturalBodyWeapon===true)throw new RangeError('Wardpact cannot affect natural body weapons');return freeze({status:'OK',rule:'WWN_WARDPACT_INVOCATION_V1',resolverId:CLOSED.WARDPACT_INVOCATION.resolverId,mode:m,weaponId:String(weaponId),rangeFeet:200,harmlessForScene:true,oncePerTargetPerScene:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.wardpact])});
}
export function resolveSmiteTheDead({casterLevel,targets,physicalDice}){
  requireCasterLevel(casterLevel);if(!Array.isArray(targets))throw new TypeError('explicit undead targets required');const rolls=settledFaces(physicalDice,10,casterLevel),damage=rolls.reduce((a,b)=>a+b,0);const seen=new Set();
  const resolutions=targets.map(t=>{const targetId=String(t?.targetId??'');if(!targetId||seen.has(targetId))throw new RangeError('unique targetId required');seen.add(targetId);if(t?.hostileUndead!==true)throw new RangeError('Smite the Dead applies only to explicit hostile undead');if(!Number.isInteger(t.hitDice)||t.hitDice<1)throw new RangeError('target hitDice required');if(typeof t.physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');const destroyEligible=t.hitDice<=casterLevel;return freeze({targetId,hitDice:t.hitDice,physicalSaveSucceeded:t.physicalSaveSucceeded,damage,destroyEligible,destroyedOutright:destroyEligible&&!t.physicalSaveSucceeded});});
  return freeze({status:'OK',rule:'WWN_SMITE_THE_DEAD_V1',resolverId:CLOSED.SMITE_THE_DEAD.resolverId,casterLevel,rolls:freeze([...rolls]),damage,resolutions:freeze(resolutions),mayCommitEffortForDayToAvoidSpellSlot:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.smite])});
}
export function resolveElementalGuardianSummon({element,physicalDice}){
  const base=resolveElementalGuardianTemplate({element}),t=base.template,rolls=settledFaces(physicalDice,8,t.hitDice),hp=rolls.reduce((a,b)=>a+b,0);
  return freeze({...base,rule:'WWN_ELEMENTAL_GUARDIAN_SUMMON_V1',rolls:freeze([...rolls]),hitPoints:hp,template:freeze({...t,hp:freeze({current:hp,max:hp})})});
}

export function resolveVisitationClementClime({casterLevel,allyIds=[]}){
  requireCasterLevel(casterLevel);const ids=uniqueIds(allyIds);if(ids.length>3*casterLevel)throw new RangeError('Clement Clime visible allies exceed three per caster level');
  return freeze({status:'OK',rule:'WWN_CLEMENT_CLIME_V1',resolverId:CLOSED.VISITATION_OF_THE_CLEMENT_CLIME.resolverId,allyIds:freeze(ids),maxVisibleAllies:3*casterLevel,durationHours:casterLevel,mundaneImmunities:freeze(['EXTREME_HEAT','EXTREME_COLD','NATURAL_ACID','NATURAL_ELECTRICITY']),magicalElementalDamageMultiplier:0.5,physicalSaveNegatesMagicalElementalDamage:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.clement])});
}
export function resolveScornFettersOfEarth({casterLevel,mode='ALLIES',allyIds=[],flyingTargets=[],magicSkill=0}){
  requireCasterLevel(casterLevel);const m=key(mode);if(!Number.isInteger(magicSkill)||magicSkill<0)throw new RangeError('magicSkill must be integer >= 0');
  if(m==='ALLIES'){const ids=uniqueIds(allyIds);if(ids.length>casterLevel)throw new RangeError('Scorn allies exceed one per caster level');return freeze({status:'OK',rule:'WWN_SCORN_FETTERS_V1',resolverId:CLOSED.SCORN_THE_FETTERS_OF_EARTH.resolverId,mode:m,allyIds:freeze(ids),duration:'SCENE',casterAlsoAffected:true,canMoveInThreeDimensions:true,remainsSuspendedAfterMovement:true,gentleDescentAtEnd:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.scorn])});}
  if(m!=='GROUND_FLYERS')throw new RangeError('Scorn mode must be ALLIES or GROUND_FLYERS');if(!Array.isArray(flyingTargets)||flyingTargets.length>casterLevel)throw new RangeError('Scorn flying targets exceed one per caster level');const seen=new Set();const resolutions=flyingTargets.map(t=>{const id=String(t?.targetId??'');if(!id||seen.has(id))throw new RangeError('unique flying targetId required');seen.add(id);if(typeof t?.physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');return freeze({targetId:id,physicalSavePenalty:-magicSkill,physicalSaveSucceeded:t.physicalSaveSucceeded,grounded:!t.physicalSaveSucceeded,fallEquivalentDistanceMultiplier:t.physicalSaveSucceeded?0:0.5,groundedFor:'SCENE'});});return freeze({status:'OK',rule:'WWN_SCORN_FETTERS_V1',resolverId:CLOSED.SCORN_THE_FETTERS_OF_EARTH.resolverId,mode:m,resolutions:freeze(resolutions),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.scorn])});
}
export function resolveLikeTheStones({element}){const e=key(element);if(!['STONE','WATER','AIR','FIRE'].includes(e))throw new RangeError('Like the Stones element must be STONE, WATER, AIR, or FIRE');return freeze({status:'OK',rule:'WWN_LIKE_THE_STONES_V1',resolverId:CLOSED.LIKE_THE_STONES.resolverId,element:e,duration:'SCENE',needsNoBreath:true,immuneToNewPoisonsAndDiseases:true,stone:e==='STONE'?freeze({autoStabilizeAtZero:true,ignoreDamagePerInstance:3}):null,water:e==='WATER'?freeze({canPassMouseSizedAperture:true}):null,air:e==='AIR'?freeze({canFlyAtUsualMove:true,rangedArmorClassBonus:4}):null,fire:e==='FIRE'?freeze({startTurnMeleeDamage:'1d6',immuneToHeatDamage:true}):null,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.likeStones])});}
export function resolveCalcifyingScourge({targetKind='CREATURE',physicalSaveSucceeded,physicalDice=null}){const kind=key(targetKind);if(!['CREATURE','OBJECT'].includes(kind))throw new RangeError('targetKind must be CREATURE or OBJECT');if(typeof physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');if(!physicalSaveSucceeded)return freeze({status:'OK',rule:'WWN_CALCIFYING_SCOURGE_V1',resolverId:CLOSED.CALCIFYING_SCOURGE.resolverId,targetKind:kind,physicalSaveSucceeded:false,petrified:true,duration:'UNTIL_DISPELLED_OR_CASTER_RELEASE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.calcifying])});if(kind==='OBJECT')return freeze({status:'OK',rule:'WWN_CALCIFYING_SCOURGE_V1',resolverId:CLOSED.CALCIFYING_SCOURGE.resolverId,targetKind:kind,physicalSaveSucceeded:true,petrified:false,moveActionLostRounds:0,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.calcifying])});const [durationRoll]=settledFaces(physicalDice,6,1);return freeze({status:'OK',rule:'WWN_CALCIFYING_SCOURGE_V1',resolverId:CLOSED.CALCIFYING_SCOURGE.resolverId,targetKind:kind,physicalSaveSucceeded:true,petrified:false,durationRoll,moveActionLostRounds:durationRoll,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.calcifying])});}
export function resolveAugmentMortalVitality({magicSkill}){if(!Number.isInteger(magicSkill)||magicSkill<0)throw new RangeError('magicSkill must be integer >= 0');return freeze({status:'OK',rule:'WWN_AUGMENT_MORTAL_VITALITY_V1',resolverId:CLOSED.AUGMENT_MORTAL_VITALITY.resolverId,duration:'SCENE',physicalSaveBonus:magicSkill,autoStabilizeWhenMortallyWounded:true,instantHealOneNonMortalInjuryOnce:true,systemStrainGained:1,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.augmentVitality])});}
export function resolveEnfeeblingWaveTarget({living=true,physicalSaveSucceeded}){if(typeof physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');if(living!==true)return freeze({status:'OK',rule:'WWN_ENFEEBLING_WAVE_TARGET_V1',resolverId:CLOSED.ENFEEBLING_WAVE.resolverId,affected:false,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.enfeebling])});return freeze({status:'OK',rule:'WWN_ENFEEBLING_WAVE_TARGET_V1',resolverId:CLOSED.ENFEEBLING_WAVE.resolverId,affected:true,physicalSaveSucceeded,duration:physicalSaveSucceeded?'NEXT_TURN':'SCENE',moveMultiplier:0.5,attackRollsTwiceTakeWorse:true,damageRollsTwiceTakeWorse:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.enfeebling])});}
export function resolveAeolianAuctoritas({casterLevel,mode='GUST',targets=[],physicalDiceByTarget={}}){requireCasterLevel(casterLevel);const m=key(mode);if(m==='WEATHER')return freeze({status:'OK',rule:'WWN_AEOLIAN_AUCTORITAS_V1',resolverId:CLOSED.SIGIL_OF_AEOLIAN_AUCTORITAS.resolverId,mode:m,radiusMiles:0.25,durationHours:casterLevel,seasonallyPossibleClimateOnly:true,extremeWeatherForbidden:true,sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.aeolian])});if(m!=='GUST')throw new RangeError('Aeolian mode must be GUST or WEATHER');if(!Array.isArray(targets))throw new TypeError('explicit gust targets required');const seen=new Set();const resolutions=targets.map(t=>{const id=String(t?.targetId??'');if(!id||seen.has(id))throw new RangeError('unique targetId required');seen.add(id);if(t?.manSizeOrSmaller!==true)return freeze({targetId:id,affected:false,reason:'TOO_LARGE'});if(typeof t?.physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');if(t.physicalSaveSucceeded)return freeze({targetId:id,affected:false,physicalSaveSucceeded:true});const [damage]=settledFaces(physicalDiceByTarget?.[id],6,1);return freeze({targetId:id,affected:true,physicalSaveSucceeded:false,pushFeet:30,loseNextRoundMainAction:true,damage});});return freeze({status:'OK',rule:'WWN_AEOLIAN_AUCTORITAS_V1',resolverId:CLOSED.SIGIL_OF_AEOLIAN_AUCTORITAS.resolverId,mode:m,lineFeet:100,widthFeet:30,resolutions:freeze(resolutions),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.aeolian])});}
export function resolveTumefaction({casterLevel,targetHitDice,living=true}){requireCasterLevel(casterLevel);if(!Number.isInteger(targetHitDice)||targetHitDice<1)throw new RangeError('targetHitDice must be >= 1');return freeze({status:'OK',rule:'WWN_TUMEFACTION_V1',resolverId:CLOSED.THE_TORMENT_OF_TUMEFACTION.resolverId,affected:living===true,damagePerVigorousAction:living===true?2*casterLevel:0,damageMaxTimesPerRound:living===true?1:0,movementExempt:true,escapeAction:'MAIN',escapeSave:'PHYSICAL',failedSaveDuration:targetHitDice<casterLevel?'INDEFINITE':'SCENE',sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.tumefaction])});}
export function resolvePhobicStormTargets({casterLevel,targets}){requireCasterLevel(casterLevel);if(!Array.isArray(targets))throw new TypeError('explicit enemy targets required');const seen=new Set();const resolutions=targets.map(t=>{const id=String(t?.targetId??'');if(!id||seen.has(id))throw new RangeError('unique targetId required');seen.add(id);const morale=Number(t?.morale);if(t?.fearless===true||morale===12)return freeze({targetId:id,affected:false,immune:true,reason:t?.fearless===true?'FEARLESS':'MORALE_12'});if(!Number.isInteger(morale)||morale<2||morale>12)throw new RangeError('target morale must be 2..12');if(typeof t?.moraleCheckSucceeded!=='boolean')throw new TypeError('explicit Morale check result required');return freeze({targetId:id,affected:true,immune:false,moralePenalty:-1,moraleCheckSucceeded:t.moraleCheckSucceeded,flees:!t.moraleCheckSucceeded,damage:t.moraleCheckSucceeded?casterLevel:0,zeroHpInsteadBecomesOneAndFlees:t.moraleCheckSucceeded});});return freeze({status:'OK',rule:'WWN_PHOBIC_STORM_V1',resolverId:CLOSED.PHOBIC_STORM.resolverId,radiusFeet:40,resolutions:freeze(resolutions),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.phobic])});}
export function resolveCallOfTheTombTargets({casterLevel,targets,currentRound=1}){requireCasterLevel(casterLevel);if(!Number.isInteger(currentRound)||currentRound<1)throw new RangeError('currentRound must be >=1');if(!Array.isArray(targets))throw new TypeError('explicit enemy targets required');const seen=new Set();const resolutions=targets.map(t=>{const id=String(t?.targetId??'');if(!id||seen.has(id))throw new RangeError('unique targetId required');seen.add(id);if(typeof t?.physicalSaveSucceeded!=='boolean')throw new TypeError('explicit Physical save result required');const durationRounds=t.physicalSaveSucceeded?1:casterLevel;return freeze({targetId:id,physicalSaveSucceeded:t.physicalSaveSucceeded,durationRounds,startsRound:currentRound,endsAfterRound:currentRound+durationRounds-1,attacksAutomaticallyHit:true,maximizeAllDamageDice:true,negateSpecialDefenses:true});});return freeze({status:'OK',rule:'WWN_CALL_OF_THE_TOMB_V1',resolverId:CLOSED.CALL_OF_THE_TOMB.resolverId,radiusFeet:40,resolutions:freeze(resolutions),sourceRefs:freeze([WWN_SPELL_EFFECT_SOURCES.callTomb])});}

