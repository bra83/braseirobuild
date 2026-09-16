const S=(locator)=>Object.freeze({sourceId:'SRC-WWN-SRD-1',locator});

export const WWN_SRD_CLASS_SOURCES=Object.freeze({
  classFramework:S('§1.4.1–1.4.4 Classes / Full and Partial Classes / Adventurer / Effort'),
  adventurer:S('§1.5.1 The Adventurer'),
  accursed:S('§1.5.2 The Accursed'),
  bard:S('§1.5.3 The Bard'),
  beastmaster:S('§1.5.4 The Beastmaster'),
  bloodPriest:S('§1.5.5 The Blood Priest'),
  duelist:S('§1.5.6 The Duelist'),
  elementalist:S('§1.5.7 The Elementalist'),
  expert:S('§1.5.8 The Expert'),
  healer:S('§1.5.9 The Healer'),
  highMage:S('§1.5.10 The High Mage'),
  invoker:S('§1.5.11 The Invoker'),
  mage:S('§1.5.12 The Mage'),
  mageslayer:S('§1.5.13 The Mageslayer'),
  necromancer:S('§1.5.14 The Necromancer'),
  skinshifter:S('§1.5.15 The Skinshifter'),
  thoughtNoble:S('§1.5.16 The Thought Noble'),
  vowed:S('§1.5.17 The Vowed'),
  warrior:S('§1.5.18 The Warrior'),
  wise:S('§1.5.19 The Wise'),
  startingSpells:S('§1.7.5 Mages Choose Starting Spells'),
  dualSpellcasters:S('§4.3.0 Dual Partial Spellcasters'),
  highMagic:S('§4.4.0 High Magic Spells'),
  elementalistSpells:S('§4.5.0 Elementalist Spells'),
  necromancerSpells:S('§4.6.0 Necromancer Spells'),
});

const ART_CATALOGS=Object.freeze({
  ACCURSED:Object.freeze(['Accursed Blade','Accursed Bolt','Bewitching Distraction','Compelling Shriek','Devil’s Bargain','Dire Pact','Lying Face','Night-Black Eyes','Pacted Protection','Rob Vitality','Scourging Curse','Shadowed Steps','Snaring Speech','Sorcerous Battery','Soul Consumption','Tendrils of Night','Unseen Steps','Weight of Sin','Weeping Wounds']),
  BARD:Object.freeze(['A Thousand Tongues','Battle Cry','Cursed Tune','Deft Fingers','Entangle Incantation','Evoke Emotion','Inspire Dread','Keen Senses','Liberating Song','Rally','Soothe the Savage','Soothing Graces','Swift Misdirection']),
  BEASTMASTER:Object.freeze(['Bind Companion','Beast Ward','Eyes of the Beast','Feral Toughness','Howl of Distant Summons','Know the Weak Spot','Mind Call','Natural Weaponry','Savage Senses','Shared Vitality','Swift Healing','Tongue of the Beasts']),
  BLOOD_PRIEST:Object.freeze(['A Thousand Tongues','Armor of God','Divine Guidance','Fear No Flame','God Wills It','Merciful Healing','Sanctified Ward','Smite the Wicked','The Light of Faith','Transubstantiation','Turn False Life','Words of Mercy','Wrath of the Most High']),
  DUELIST:Object.freeze(['Favored Weapon','Blood for Blood','Burst of Speed','Code Duello','Crushing Superiority','Dauntless Step','Dodge Doom','Forced Engagement','Gentleman’s Withdrawal','Graceful Leap','Lightning Draw','Piercing Strike','Spiritual Weapon','Unbindable','Unworthy Rabble','Whirling Evasion']),
  ELEMENTALIST:Object.freeze(['Elemental Resilience','Elemental Sparks','Beckoned Deluge','Earthsight','Elemental Blast','Flamesight','Pavis of Elements','Petrifying Stare','Rune of Destruction','Steps of Air','Stunning Shock','Thermal Shield']),
  HEALER:Object.freeze(['Healing Touch','Empowered Healer','Facile Healer','Far Healer','Final Repose','Healer’s Eye','Limb Restoration','Purge Ailment','Refined Restoration','Revive the Fallen','Swift Healer','The Healer’s Knife','Tireless Vigor','Vital Furnace']),
  HIGH_MAGE:Object.freeze(['Arcane Lexicon','Counter Magic','Empowered Sorcery','Hang Sorcery','Inexorable Effect','Iron Resolution','Preparatory Countermagic','Psychic Conversion','Restrained Casting','Retain Sorcery','Sense Magic','Suppress Magic','Swift Casting','Ward Allies','Wizard’s Grandeur']),
  MAGESLAYER:Object.freeze(['Absolute Negation','Antimage','Dispel Enchantment','Disrupt Sorcery','Immaculate Body','Immaculate Mind','Know Your Prey','Magebane','Share the Pain','Spellshield','Ward Ally','Witchfinder']),
  NECROMANCER:Object.freeze(['Bonetalker','Cold Flesh','Consume Life Energy','False Death','Gravesight','Keeper of the Gate','Life Bridge','Master of Bones','Red Harvest','Unaging','Uncanny Ichor','Unliving Persistence']),
  SKINSHIFTER:Object.freeze(['Change Form','Eyes of the Hawk','Feline Leap','Feral Prowess','Intrinsic Armor','Manifest Wings','Octopus’ Embrace','Perfect Mimicry','Pliant Flesh','Savage Talons','Sculptor’s Beauty','Serpent’s Kiss','The Monkey’s Road','Warform','Wisdom of Fin and Scale']),
  THOUGHT_NOBLE:Object.freeze(['Open Mind','Block Memory','Elicitation of Truth','Facile Speech','Far Speech','Hypercognition','Iconograph','Impress Imperative','Mind Light','Mind Over Matter','Mirror Mask','Positive Association','Read Intent','Surface Apprehension','Thoughts Like Razors','Unthinkable Thought']),
  VOWED:Object.freeze(['Martial Style','Unarmed Might','Unarmored Defense','Brutal Counter','Faultless Awareness','Hurling Throw','The Inward Eye','Leap of the Heavens','Master’s Vigor','Mob Justice','Nimble Ascent','Purified Body','Revivifying Breath','Shattering Strike','Style Weaponry','Unobtrusive Step']),
  WISE:Object.freeze(['Dread Awe','Elite Ties','Erudite','Folk-Friend','Healer','Holy Sanctity','Personal Impunity','Skilled','Compel Truth','Deliver Oracle','Find Object','Read Omens','Auspicious Undertaking','Evil Eye','Ill Fate','Luck Blessing','War Curse']),
});

export const WWN_SRD_LEVEL1_SPELLS=Object.freeze({
  HIGH_MAGIC:Object.freeze([
    'Apprehending the Arcane Form','Cognitive Supersession of the Inferior Orders','The Coruscating Coffin','Damnation of the Sense','Decree of Ligneous Dissolution','The Excellent Transpicuous Transformation','Imperceptible Cerebral Divulgence','Ineluctable Shackles of Volition','The Long Amber Moment','Phantasmal Mimesis','Velocitous Imbuement','Wardpact Invocation','The Wind of the Final Repose',
  ]),
  ELEMENTALIST:Object.freeze(['Aqueous Harmony','Flame Scrying','Elemental Favor','Elemental Spy']),
  NECROMANCER:Object.freeze(['Command the Dead','Query the Skull','Smite the Dead','Terrible Liveliness']),
});

function effort(skill,attributes,{fullAddsOne=false}={}){return Object.freeze({skill,attributes:Object.freeze([...attributes]),minimum:1,fullAddsOne});}
function initial({fixedArts=[],artPickCount=0,bonusSkill=null,bonusSkillChoice=null,spellLists=[],spellKnownFull=0,spellKnownPartial=0,spellcasting=false,notes=[]}={}){
  return Object.freeze({fixedArts:Object.freeze([...fixedArts]),artPickCount,bonusSkill,bonusSkillChoice:bonusSkillChoice?Object.freeze([...bonusSkillChoice]):null,spellLists:Object.freeze([...spellLists]),spellKnownFull,spellKnownPartial,spellcasting,notes:Object.freeze([...notes])});
}
function profile(id,label,baseFamily,{fullAllowed=false,partialAllowed=true,source,effortFormula=null,initialTraining=initial(),special={},classAbilitiesFull=[],classAbilitiesPartial=[]}={}){
  return Object.freeze({classId:id,label,baseFamily,fullAllowed,partialAllowed,sourceRef:source,effortFormula,initialTraining,special:Object.freeze({...special}),classAbilitiesFull:Object.freeze([...classAbilitiesFull]),classAbilitiesPartial:Object.freeze([...classAbilitiesPartial]),arts:ART_CATALOGS[id]??Object.freeze([])});
}

export const WWN_SRD_CLASSES=Object.freeze({
  EXPERT:profile('EXPERT','Expert','EXPERT',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.expert,initialTraining:initial(),classAbilitiesFull:['Masterful Expertise','Quick Learner'],classAbilitiesPartial:['Quick Learner']}),
  WARRIOR:profile('WARRIOR','Warrior','WARRIOR',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.warrior,initialTraining:initial(),classAbilitiesFull:["Killing Blow","Veteran’s Luck"],classAbilitiesPartial:[]}),
  HIGH_MAGE:profile('HIGH_MAGE','High Mage','MAGE',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.highMage,effortFormula:effort('Magic',['Intelligence','Charisma'],{fullAddsOne:true}),initialTraining:initial({bonusSkill:'Magic',artPickCount:2,spellLists:['HIGH_MAGIC'],spellKnownFull:4,spellKnownPartial:2,spellcasting:true}),special:{partialArtPickCount:1}}),
  NECROMANCER:profile('NECROMANCER','Necromancer','MAGE',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.necromancer,effortFormula:effort('Magic',['Intelligence','Charisma'],{fullAddsOne:true}),initialTraining:initial({bonusSkill:'Magic',artPickCount:2,spellLists:['HIGH_MAGIC','NECROMANCER'],spellKnownFull:4,spellKnownPartial:2,spellcasting:true}),special:{partialArtPickCount:1}}),
  INVOKER:profile('INVOKER','Invoker','MAGE',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.invoker,initialTraining:initial({bonusSkill:'Magic',spellLists:['HIGH_MAGIC'],spellKnownFull:4,spellKnownPartial:2,spellcasting:true,notes:['Invoker uses spell points rather than normal daily casting slots and normally gains no arts.']}),special:{forbidsPartialSpellcasterPair:true,usesSpellPoints:true}}),
  ELEMENTALIST:profile('ELEMENTALIST','Elementalist','MAGE',{fullAllowed:true,source:WWN_SRD_CLASS_SOURCES.elementalist,effortFormula:effort('Magic',['Intelligence','Charisma'],{fullAddsOne:true}),initialTraining:initial({bonusSkill:'Magic',fixedArts:['Elemental Resilience','Elemental Sparks'],artPickCount:1,spellLists:['HIGH_MAGIC','ELEMENTALIST'],spellKnownFull:4,spellKnownPartial:2,spellcasting:true})}),
  ACCURSED:profile('ACCURSED','Accursed','MAGE',{source:WWN_SRD_CLASS_SOURCES.accursed,effortFormula:effort('Magic',['Intelligence','Charisma']),initialTraining:initial({bonusSkill:'Magic',artPickCount:1,notes:['At level 1 choose exactly one of Accursed Blade or Accursed Bolt, plus one other art.']}),special:{chooseOneFixedArt:Object.freeze(['Accursed Blade','Accursed Bolt'])}}),
  BARD:profile('BARD','Bard','EXPERT',{source:WWN_SRD_CLASS_SOURCES.bard,effortFormula:effort('Perform',['Charisma']),initialTraining:initial({bonusSkill:'Perform',fixedArts:['A Thousand Tongues'],artPickCount:1}),special:{suppressesPartialExpertFocus:true,suppressesQuickLearner:true}}),
  BEASTMASTER:profile('BEASTMASTER','Beastmaster','MAGE',{source:WWN_SRD_CLASS_SOURCES.beastmaster,effortFormula:effort('Survive',['Wisdom','Charisma']),initialTraining:initial({bonusSkill:'Survive',fixedArts:['Bind Companion'],artPickCount:1})}),
  BLOOD_PRIEST:profile('BLOOD_PRIEST','Blood Priest','MAGE',{source:WWN_SRD_CLASS_SOURCES.bloodPriest,effortFormula:effort('Pray',['Wisdom','Charisma']),initialTraining:initial({bonusSkill:'Pray',artPickCount:2})}),
  DUELIST:profile('DUELIST','Duelist','MAGE',{source:WWN_SRD_CLASS_SOURCES.duelist,effortFormula:effort('Stab',['Dexterity','Intelligence']),initialTraining:initial({bonusSkill:'Stab',fixedArts:['Favored Weapon'],artPickCount:1}),special:{warriorPairHitDiceOverride:true,favoredWeaponRequired:true}}),
  HEALER:profile('HEALER','Healer','MAGE',{source:WWN_SRD_CLASS_SOURCES.healer,effortFormula:effort('Heal',['Intelligence','Charisma']),initialTraining:initial({bonusSkill:'Heal',fixedArts:['Healing Touch'],artPickCount:1})}),
  MAGESLAYER:profile('MAGESLAYER','Mageslayer','WARRIOR',{source:WWN_SRD_CLASS_SOURCES.mageslayer,effortFormula:effort('Magic',['Intelligence','Constitution']),initialTraining:initial({bonusSkill:'Magic',fixedArts:['Antimage','Magebane']}),special:{forbidsMagePair:true,suppressesPartialWarriorHpBonus:true,suppressesPartialWarriorFocus:true,martialBabWithoutHpBonus:true}}),
  SKINSHIFTER:profile('SKINSHIFTER','Skinshifter','MAGE',{source:WWN_SRD_CLASS_SOURCES.skinshifter,effortFormula:effort('Survive',['Constitution','Charisma']),initialTraining:initial({bonusSkill:'Survive',fixedArts:['Change Form'],artPickCount:1})}),
  THOUGHT_NOBLE:profile('THOUGHT_NOBLE','Thought Noble','MAGE',{source:WWN_SRD_CLASS_SOURCES.thoughtNoble,effortFormula:effort('Notice',['Intelligence','Wisdom']),initialTraining:initial({bonusSkill:'Notice',fixedArts:['Open Mind'],artPickCount:1})}),
  VOWED:profile('VOWED','Vowed','MAGE',{source:WWN_SRD_CLASS_SOURCES.vowed,effortFormula:effort('ORDER_SKILL',['BEST_ATTRIBUTE']),initialTraining:initial({bonusSkillChoice:['Exert','Know','Magic','Pray'],fixedArts:['Martial Style','Unarmed Might','Unarmored Defense'],artPickCount:1}),special:{minimumHitDieD6:true}}),
  WISE:profile('WISE','Wise','EXPERT',{source:WWN_SRD_CLASS_SOURCES.wise,initialTraining:initial({bonusSkillChoice:['Pray','Know','Magic','Survive'],notes:['Wise uses concept-based art progression and does not use Effort.']}),special:{suppressesPartialExpertFocus:true,suppressesQuickLearner:true,wiseConceptRequired:true}}),
});

export const WWN_FULL_CLASS_IDS=Object.freeze(Object.values(WWN_SRD_CLASSES).filter(c=>c.fullAllowed).map(c=>c.classId));
export const WWN_PARTIAL_CLASS_IDS=Object.freeze(Object.values(WWN_SRD_CLASSES).filter(c=>c.partialAllowed).map(c=>c.classId));
export const WWN_PARTIAL_ONLY_CLASS_IDS=Object.freeze(Object.values(WWN_SRD_CLASSES).filter(c=>!c.fullAllowed&&c.partialAllowed).map(c=>c.classId));

export const WISE_LEVEL1_CONCEPTS=Object.freeze({
  MUNDANE_PRIEST:Object.freeze({conceptId:'MUNDANE_PRIEST',label:'Mundane Priest',startingArts:Object.freeze(['Holy Sanctity']),sourceRef:WWN_SRD_CLASS_SOURCES.wise}),
  WITCH:Object.freeze({conceptId:'WITCH',label:'Witch',startingArts:Object.freeze(['Dread Awe']),sourceRef:WWN_SRD_CLASS_SOURCES.wise}),
  SEER:Object.freeze({conceptId:'SEER',label:'Seer',startingArts:Object.freeze(['Erudite','Folk-Friend']),chooseOne:true,sourceRef:WWN_SRD_CLASS_SOURCES.wise}),
  ESOTERIC:Object.freeze({conceptId:'ESOTERIC',label:'Esoteric',startingArts:Object.freeze([...ART_CATALOGS.WISE]),chooseOne:true,sourceRef:WWN_SRD_CLASS_SOURCES.wise}),
});

export function classProfile(classId){return WWN_SRD_CLASSES[String(classId??'').toUpperCase()]??null;}
export function classArtCatalog(classId){return classProfile(classId)?.arts??Object.freeze([]);}
export function levelOneSpellCatalog(listId){return WWN_SRD_LEVEL1_SPELLS[String(listId??'').toUpperCase()]??Object.freeze([]);}
export function isSpellcastingClass(classId){return classProfile(classId)?.initialTraining?.spellcasting===true;}
