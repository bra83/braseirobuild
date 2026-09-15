const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.8.4 Example Modifications'});
const freeze=v=>Object.freeze(v);
function mod(modId,name,minimumCraftSkill,costSilver,salvageUnits,targets,effect){return freeze({modId,name,minimumCraftSkill,costSilver,salvageUnits,targets:freeze([...targets]),effect:freeze(effect),sourceRefs:freeze([SRC])});}
export const WWN_MOD_CATALOG=freeze([
 mod('ARROW_STORM','Arrow Storm',2,5000,1,['PROJECTILE_WEAPON'],{selfGeneratingAmmo:true,reloadSpeedChanged:false}),
 mod('ASSASSINS_TRINKET',"Assassin's Trinket",2,1000,1,['ONE_HANDED_WEAPON'],{jewelryForm:true,toggleAction:'ON_TURN'}),
 mod('AUGMENTED_GEAR','Augmented Gear',1,5000,1,['TOOL','MEDICAL_KIT','EQUIPMENT'],{skillBonus:1,chosenPurposeRequired:true,bonusKind:'SKILL'}),
 mod('AUTOMATIC_RELOAD','Automatic Reload',2,10000,2,['HURLANT'],{oncePerSceneReloadAction:'ON_TURN',ammoRequired:true}),
 mod('CUSTOMIZED','Customized',1,1000,0,['WEAPON','ARMOR'],{specificUserRequired:true,weaponHitBonus:1,armorClassBonus:1,excludes:['SHIELD']}),
 mod('FLYING_RAZOR','Flying Razor',1,5000,1,['THROWN_WEAPON'],{returnsAfterAttack:true}),
 mod('HARMONIZED_AEGIS','Harmonized Aegis',3,10000,1,['ARMOR'],{coordinationMinutes:10,immuneToCoordinatedCasterHarmfulSpellsUntil:'END_OF_DAY'}),
 mod('LONG_ARM','Long Arm',2,5000,1,['RANGED_WEAPON','THROWN_WEAPON'],{normalRangeMultiplier:2,maximumRangeMultiplier:2}),
 mod('MANIFOLD_MAIL','Manifold Mail',2,5000,1,['ARMOR'],{appearancePresets:true,toggleAction:'ON_TURN',statisticsUnchanged:true}),
 mod('OMENED_AIM','Omened Aim',2,4000,0,['RANGED_WEAPON','THROWN_WEAPON'],{hitBonus:1,bonusKind:'HIT'}),
 mod('PRESERVING_GRACE','Preserving Grace',1,5000,1,['CLOTHING','ARMOR'],{autoStabilizeUsesPerWeek:1}),
 mod('RAZOR_EDGE','Razor Edge',2,5000,1,['WEAPON'],{damageBonus:2,shockBonus:2,bonusKind:'DAMAGE'}),
 mod('TAILORED_HARNESS','Tailored Harness',2,5000,0,['ARMOR'],{specificUserRequired:true,encumbranceReduction:1,skillPenaltyUnchanged:true,armoredMagicUnchanged:true}),
 mod('THIRSTING_BLADE','Thirsting Blade',3,10000,2,['WEAPON'],{hitBonus:1,bonusKind:'HIT'}),
]);
export const WWN_MOD_BY_ID=freeze(Object.fromEntries(WWN_MOD_CATALOG.map(x=>[x.modId,x])));
export function wwnMod(modId){return WWN_MOD_BY_ID[String(modId??'').toUpperCase()]??null;}
