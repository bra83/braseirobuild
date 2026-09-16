const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0–4.6.0 Spell Lists'});
const RESEARCH=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.7.0–4.7.2 Developing New Spells'});
const freeze=v=>Object.freeze(v);
const key=v=>String(v??'').trim().toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'');
function spell(name,level,tradition,extra={}){return freeze({spellId:key(name),name,level,tradition,sourceRefs:freeze([SRC]),...extra});}

export const WWN_SPELL_CATALOG=freeze([
  spell('Abdication of Temporal Presence',5,'HIGH_MAGIC'),
  spell('Adopt the Simulacular Visage',3,'HIGH_MAGIC'),
  spell('Apprehending the Arcane Form',1,'HIGH_MAGIC'),
  spell('Banishment to the Black Glass Labyrinth',5,'HIGH_MAGIC'),
  spell('Calculation of the Evoked Servitor',2,'HIGH_MAGIC'),
  spell('Calculation of the Phantasmal Eidolon',4,'HIGH_MAGIC'),
  spell('Casting Forth the Inner Eye',2,'HIGH_MAGIC'),
  spell('Cognitive Supersession of the Inferior Orders',1,'HIGH_MAGIC'),
  spell('Conjunct the Vital Viscera',3,'HIGH_MAGIC'),
  spell('Conjunction of the Inexorable Step',2,'HIGH_MAGIC'),
  spell('Contingent Excision of Arcana',4,'HIGH_MAGIC'),
  spell('The Coruscating Coffin',1,'HIGH_MAGIC'),
  spell('Damnation of the Sense',1,'HIGH_MAGIC'),
  spell('The Dazzling Prismatic Hemicycle',5,'HIGH_MAGIC'),
  spell('Decree of Ligneous Dissolution',1,'HIGH_MAGIC'),
  spell('Decree of Lithic Dissolution',2,'HIGH_MAGIC'),
  spell('Deluge of Hell',5,'HIGH_MAGIC'),
  spell('Disjunctive Temporal Reversion',4,'HIGH_MAGIC'),
  spell('The Earth as Clay',5,'HIGH_MAGIC'),
  spell('Evert the Inwardness',4,'HIGH_MAGIC'),
  spell('The Excellent Transpicuous Transformation',1,'HIGH_MAGIC'),
  spell('Exhalation of Congelating Cold',3,'HIGH_MAGIC'),
  spell('Extirpate Arcana',2,'HIGH_MAGIC'),
  spell('Foresightful Apprehension',3,'HIGH_MAGIC'),
  spell('The Glass Chimes of the Bamboo Terrace',3,'HIGH_MAGIC'),
  spell('The Grinding Geas',4,'HIGH_MAGIC'),
  spell('The Howl of Light',3,'HIGH_MAGIC'),
  spell('Imperceptible Cerebral Divulgence',1,'HIGH_MAGIC'),
  spell('Ineluctable Shackles of Volition',1,'HIGH_MAGIC'),
  spell('The Inexorable Imputation',2,'HIGH_MAGIC'),
  spell('Invocation of the Invincible Citadel',5,'HIGH_MAGIC'),
  spell('The Jade Palanquin of the Faceless God',2,'HIGH_MAGIC'),
  spell('The Long Amber Moment',1,'HIGH_MAGIC'),
  spell('Mantle of Disjecting Dissection',2,'HIGH_MAGIC'),
  spell('Obnubilation of the Will',4,'HIGH_MAGIC'),
  spell('The Ochre Sigil of Juxtaposition',4,'HIGH_MAGIC'),
  spell('Open the High Road',5,'HIGH_MAGIC'),
  spell('Phantasmal Mimesis',1,'HIGH_MAGIC'),
  spell('Prudentially Transient Abnegation of Life',2,'HIGH_MAGIC'),
  spell('Phobic Storm',3,'HIGH_MAGIC'),
  spell('Pierce the Pallid Gate',4,'HIGH_MAGIC'),
  spell('Resounding Temporal Echo',2,'HIGH_MAGIC'),
  spell('Scorn the Fetters of Earth',3,'HIGH_MAGIC'),
  spell('Sigil of Aeolian Auctoritas',4,'HIGH_MAGIC'),
  spell('The Torment of Tumefaction',3,'HIGH_MAGIC'),
  spell('Touch of Elucidating Intangibility',3,'HIGH_MAGIC'),
  spell('Vallation of Specified Exclusion',3,'HIGH_MAGIC'),
  spell('Velocitous Imbuement',1,'HIGH_MAGIC'),
  spell('The Verdant Vallation',2,'HIGH_MAGIC'),
  spell('Visitation of the Clement Clime',2,'HIGH_MAGIC'),
  spell('Wardpact Invocation',1,'HIGH_MAGIC'),
  spell('The Wind of the Final Repose',1,'HIGH_MAGIC'),
  spell('Aqueous Harmony',1,'ELEMENTALIST'),
  spell('Flame Scrying',1,'ELEMENTALIST'),
  spell('Elemental Favor',1,'ELEMENTALIST'),
  spell('Elemental Spy',1,'ELEMENTALIST'),
  spell('Boreal Wings',2,'ELEMENTALIST'),
  spell('The Burrower Below',2,'ELEMENTALIST'),
  spell('Flame Without End',2,'ELEMENTALIST'),
  spell('Pact of Stone and Sea',2,'ELEMENTALIST'),
  spell('Elemental Vallation',3,'ELEMENTALIST'),
  spell('Like the Stones',3,'ELEMENTALIST'),
  spell('Wind Walking',3,'ELEMENTALIST'),
  spell('Calcifying Scourge',4,'ELEMENTALIST'),
  spell('Elemental Guardian',4,'ELEMENTALIST'),
  spell('Fury of the Elements',5,'ELEMENTALIST'),
  spell('Tremors of the Depths',5,'ELEMENTALIST'),
  spell('Command the Dead',1,'NECROMANCER'),
  spell('Query the Skull',1,'NECROMANCER'),
  spell('Smite the Dead',1,'NECROMANCER'),
  spell('Terrible Liveliness',1,'NECROMANCER'),
  spell('Augment Mortal Vitality',2,'NECROMANCER'),
  spell('Enfeebling Wave',2,'NECROMANCER'),
  spell('Final Death',2,'NECROMANCER'),
  spell('Raise Corpse',2,'NECROMANCER'),
  spell('Compel Flesh',3,'NECROMANCER'),
  spell('Festering Curse',3,'NECROMANCER'),
  spell('Forgetting the Grave',3,'NECROMANCER'),
  spell('Merge Souls',3,'NECROMANCER'),
  spell('Boneshaper',4,'NECROMANCER'),
  spell('Raise Grave Knight',4,'NECROMANCER'),
  spell('Call of the Tomb',5,'NECROMANCER'),
  spell('Everlasting',5,'NECROMANCER')
]);

const BY_ID=new Map(WWN_SPELL_CATALOG.map(s=>[s.spellId,s]));
const BY_NAME=new Map(WWN_SPELL_CATALOG.map(s=>[s.name,s]));
export function spellById(id){return BY_ID.get(key(id))??BY_NAME.get(String(id??''))??null;}
export function spellsByTradition(tradition){const t=key(tradition);return freeze(WWN_SPELL_CATALOG.filter(s=>s.tradition===t));}
export function learnableSpells({traditions,maxLevel,known=[]}){
  const allowed=new Set((traditions??[]).map(key));const knownIds=new Set((known??[]).map(x=>key(x?.spellId??x?.name??x)));
  return freeze(WWN_SPELL_CATALOG.filter(s=>allowed.has(s.tradition)&&s.level<=maxLevel&&!knownIds.has(s.spellId)));
}

export const WWN_SPELL_RESEARCH_TABLE=freeze({
  1:freeze({labCost:50000,materialsCost:25000,timeMonths:1}),
  2:freeze({labCost:125000,materialsCost:50000,timeMonths:2}),
  3:freeze({labCost:250000,materialsCost:100000,timeMonths:4}),
  4:freeze({labCost:500000,materialsCost:200000,timeMonths:8}),
  5:freeze({labCost:1000000,materialsCost:400000,timeMonths:24})
});
export function spellResearchPlan({spellLevel,hasApprentices=false,specialistOwnField=false,extraBonus=0,doubleMaterials=false}){
  if(!Number.isInteger(spellLevel)||spellLevel<1||spellLevel>5)throw new RangeError('spellLevel must be 1..5');
  if(!Number.isInteger(extraBonus))throw new TypeError('extraBonus must be integer');
  const row=WWN_SPELL_RESEARCH_TABLE[spellLevel];
  return freeze({rule:'WWN_SPELL_RESEARCH_V1',spellLevel,labCost:row.labCost,materialsCost:row.materialsCost*(doubleMaterials?2:1),timeMonths:row.timeMonths*(doubleMaterials?0.5:1),difficulty:10+spellLevel,check:'INT_OR_CHA/MAGIC',apprenticeBonus:hasApprentices?1:0,specialistBonus:specialistOwnField?1:0,extraBonus,totalCheckBonus:(hasApprentices?1:0)+(specialistOwnField?1:0)+extraBonus,sourceRefs:freeze([RESEARCH])});
}

export const WWN_SPELL_EFFECT_CONTRACTS=freeze({
  THE_CORUSCATING_COFFIN:freeze({damagePerCasterLevel:'1d8',save:'PHYSICAL_HALF',rangeFeetPerCasterLevel:100,oneHdNpcAlwaysSlain:true}),
  EXTIRPATE_ARCANA:freeze({radiusFeet:20,rangeFeetPerCasterLevel:100,autoNegateIfSourceLevelAtMostCaster:true,contest:'INT_OR_CHA/MAGIC',higherLevelBonus:2,dispellerWinsTies:true,standingEffectSuppression:'1d6 rounds'}),
  RESOUNDING_TEMPORAL_ECHO:freeze({targetsPerCasterLevel:1,duration:'1d4+1 rounds',extraMainActions:1,bonusActionCannotCastSpellsOrArts:true,systemStrainPerBonusRound:1}),
  VELOCITOUS_IMBUEMENT:freeze({action:'ON_TURN',targetsPerCasterLevel:1,rangeFeet:100,groundMoveMultiplier:2,avoidsFightingWithdrawal:true,duration:'SCENE',systemStrainOnUse:1}),
  WARDPACT_INVOCATION:freeze({rangeFeet:200,creatureDurationRounds:'ceil(casterLevel/2)',creatureWeaponHitRequiresAttackerPhysicalSave:true,successfulSaveNegatesHit:false,failedSaveNegatesHit:true,weaponModeHarmlessForScene:true,oncePerTargetPerScene:true}),
  THE_WIND_OF_THE_FINAL_REPOSE:freeze({rangeFeet:200,radiusFeet:20,maxAffectedHitDice:4,condition:'UNCONSCIOUS',wakeOn:['DAMAGE','MAIN_ACTION','END_SCENE'],sleepImmune:true}),
  SMITE_THE_DEAD:freeze({rangeFeetPerCasterLevel:100,radiusFeet:20,damagePerCasterLevel:'1d10',targets:'HOSTILE_UNDEAD',destroySave:'PHYSICAL',destroyIfHitDiceAtMostCasterLevel:true,mayCommitEffortForDayToAvoidSpellSlot:true}),
  RAISE_CORPSE:freeze({hitDice:1,armorClass:13,moveFeet:30,skillBonus:0,save:15,attack:'+1/1d6',instinct:0,morale:12,maxActivePerCasterLevel:1}),
  ELEMENTAL_GUARDIAN:freeze({baseHitDice:4,earthHitDice:6,armorClass:15,waterArmorClass:18,moveFeet:40,skillBonus:1,save:13,instinct:0,morale:12,attack:'+6/1d10',fireShock:'5/-',airCanFly:true,maxActive:1}),
  EVERLASTING:freeze({action:'INSTANT',radiusFeet:50,durationRounds:5,minimumHpDuringEffect:1,casterHpAfterEffect:1,benefitUsesPerDayPerCreature:1})
});
