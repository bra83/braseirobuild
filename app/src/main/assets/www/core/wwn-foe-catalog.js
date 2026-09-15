const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§5.1.0 Monster and NPC Statistics'});
const freeze=(v)=>Object.freeze(v);
function p(id,name,category,hd,ac,attack,damage,shock,move,morale,instinct,skill,save,extra={}){return freeze({id,name,category,hitDice:hd,armorClass:ac,attackBonus:attack,damage,shock,moveFeet:move,morale,instinct,skillBonus:skill,saveTarget:save,sourceRefs:freeze([SRC]),...extra});}
export const WWN_FOE_CATALOG=freeze([
 p('PEACEFUL_HUMAN','Peaceful Human','NORMAL_HUMAN',1,10,0,'WPN','WPN',30,7,5,1,15),
 p('THUG_OR_MILITIA','Thug or Militia','NORMAL_HUMAN',1,13,1,'WPN','WPN',30,8,4,1,15,{armored:true}),
 p('BARBARIAN_FIGHTER','Barbarian Fighter','NORMAL_HUMAN',1,13,2,'WPN+1','WPN+1',30,8,5,1,15,{armored:true}),
 p('VETERAN_SOLDIER','Veteran Soldier','NORMAL_HUMAN',1,13,2,'WPN+1','WPN+1',30,8,3,1,15,{armored:true}),
 p('SKILLED_VETERAN','Skilled Veteran','NORMAL_HUMAN',2,15,3,'WPN+1','WPN+1',30,9,2,1,14,{armored:true}),
 p('ELITES_OR_SPECIAL_GUARDS','Elites or Special Guards','NORMAL_HUMAN',3,18,4,'WPN+2','WPN+2',30,10,2,2,14,{armored:true}),
 p('KNIGHT_OR_MINOR_HERO','Knight or Minor Hero','NORMAL_HUMAN',4,18,6,'WPN+2','WPN+2',30,10,1,2,13,{armored:true}),
 p('WARRIOR_BARON','Warrior Baron','NORMAL_HUMAN',6,18,8,'WPN+3','WPN+3',30,9,1,2,12,{armored:true}),
 p('BARBARIAN_WARLORD','Barbarian Warlord','NORMAL_HUMAN',8,16,10,'WPN+4','WPN+4/-',30,10,3,2,11,{armored:true,attacksPerAction:2}),
 p('MIGHTY_GENERAL','Mighty General','NORMAL_HUMAN',8,18,10,'WPN+4','WPN+4/-',30,10,1,3,11,{armored:true}),
 p('MAJOR_HERO','Major Hero','NORMAL_HUMAN',10,18,12,'WPN+5','WPN+5/-',30,10,2,3,10,{armored:true,attacksPerAction:2}),
 p('GREAT_WARRIOR_KING','Great Warrior King','NORMAL_HUMAN',12,18,14,'WPN+5','WPN+5/-',30,10,1,3,9,{armored:true,attacksPerAction:2}),
 p('PETTY_MAGE','Petty Mage','SPELLCASTER',2,10,1,'WPN','WPN',30,8,4,1,14,{spellcaster:true}),
 p('TRIBAL_SHAMAN','Tribal Shaman','SPELLCASTER',4,10,3,'WPN+1','WPN+1',30,9,4,1,13,{spellcaster:true}),
 p('SKILLED_SORCERER','Skilled Sorcerer','SPELLCASTER',5,10,1,'WPN','WPN',30,9,4,2,13,{spellcaster:true}),
 p('MASTER_WIZARD','Master Wizard','SPELLCASTER',8,13,1,'WPN','WPN',30,9,3,2,11,{spellcaster:true}),
 p('FAMOUS_ARCH_MAGE','Famous Arch-Mage','SPELLCASTER',10,13,2,'WPN','WPN',30,9,2,3,10,{spellcaster:true}),
 p('SMALL_PACK_PREDATOR','Small Pack Predator','NORMAL_ANIMAL',1,12,2,'1D4','1/13',40,7,6,1,15),
 p('LARGE_SOLITARY_PREDATOR','Large Solitary Predator','NORMAL_ANIMAL',5,13,6,'1D8','2/13',30,8,6,1,13),
 p('APEX_PREDATOR','Apex Predator','NORMAL_ANIMAL',6,13,6,'1D8','2/13',40,8,6,2,12,{attacksPerAction:2}),
 p('HERD_BEAST','Herd Beast','NORMAL_ANIMAL',2,11,2,'1D4',null,40,7,6,1,14),
 p('VICIOUS_LARGE_HERBIVORE','Vicious Large Herbivore','NORMAL_ANIMAL',4,13,5,'1D10','1/13',40,9,6,1,13),
 p('ELEPHANTINE_GRAZER','Elephantine Grazer','NORMAL_ANIMAL',6,13,5,'2D8',null,40,7,6,1,12),
 p('AUTOMATON_HUMANLIKE','Automaton, Humanlike','UNNATURAL_ENTITY',2,13,2,'WPN','WPN',30,12,3,1,14),
 p('AUTOMATON_LABORER','Automaton, Laborer','UNNATURAL_ENTITY',2,15,2,'1D6','1/13',30,12,3,1,14),
 p('AUTOMATON_MILITARY','Automaton, Military','UNNATURAL_ENTITY',4,18,5,'1D10+2','4/15',30,12,3,1,13),
 p('AUTOMATON_WARBOT','Automaton, Warbot','UNNATURAL_ENTITY',10,20,12,'1D12+5','7/-',40,12,2,2,10,{attacksPerAction:3}),
 p('SLIME_OR_OOZE','Slime or ooze','UNNATURAL_ENTITY',6,10,6,'1D8','1/-',20,12,5,1,12,{attacksPerAction:2}),
 p('PREDATOR_SMALL_VICIOUS','Predator, Small Vicious','UNNATURAL_ENTITY',1,14,1,'1D4','1/13',30,7,5,1,15),
 p('PREDATOR_LARGE_VICIOUS','Predator, Large Vicious','UNNATURAL_ENTITY',6,13,7,'2D6','2/15',40,9,5,2,13,{attacksPerAction:2}),
 p('PREDATOR_HULKING','Predator, Hulking','UNNATURAL_ENTITY',10,15,12,'2D6+3','6/15',30,10,4,1,10,{attacksPerAction:2}),
 p('PREDATOR_HELLBEAST','Predator, Hellbeast','UNNATURAL_ENTITY',10,18,12,'1D10+5','6/-',60,11,4,3,10,{attacksPerAction:4}),
 p('UNNATURAL_SWARM','Unnatural Swarm','UNNATURAL_ENTITY',4,10,6,'1D6','1/-',30,10,5,1,13,{attacksPerAction:3}),
 p('TERRIBLE_WARBEAST','Terrible Warbeast','UNNATURAL_ENTITY',8,15,10,'2D6+4','7/15',40,9,4,2,11,{attacksPerAction:2}),
 p('LEGENDARY_GOD_TITAN','Legendary God-Titan','UNNATURAL_ENTITY',20,22,20,'2D10+5','10/-',40,10,3,3,2,{attacksPerAction:3}),
]);
export const WWN_FOE_BY_ID=freeze(Object.fromEntries(WWN_FOE_CATALOG.map(x=>[x.id,x])));
export function foeProfile(id){return WWN_FOE_BY_ID[String(id??'').toUpperCase().replace(/[^A-Z0-9]+/g,'_')]??null;}
export function foeHitPointPlan({profileId,hitDieRolls}){const profile=foeProfile(profileId);if(!profile)throw new RangeError('unknown foe profile');if(!Array.isArray(hitDieRolls)||hitDieRolls.length!==profile.hitDice||hitDieRolls.some(v=>!Number.isInteger(v)||v<1||v>8))throw new RangeError('hitDieRolls must provide exactly one d8 result per HD');return freeze({rule:'WWN_FOE_HP_V1',profileId:profile.id,hitDice:profile.hitDice,hitPoints:hitDieRolls.reduce((a,b)=>a+b,0),rolls:freeze([...hitDieRolls]),sourceRefs:profile.sourceRefs});}
export function spellcasterFoeMagicPlan(profileId){const p=foeProfile(profileId);if(!p||!p.spellcaster)return freeze({status:'NOT_SPELLCASTER'});return freeze({status:'OK',profileId:p.id,effectiveMageLevel:p.hitDice,effort:p.skillBonus+2,traditionAndPowersRequireGmSelection:true,sourceRefs:p.sourceRefs});}
