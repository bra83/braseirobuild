import { WWN_COMBAT_SOURCES } from './wwn-combat-core.js';

export const WWN_EQUIPMENT_SOURCES = Object.freeze({
  adventuringGear: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§3.2.0 Adventuring Gear' }),
  weapons: WWN_COMBAT_SOURCES.weapons,
  armor: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§3.3.0–3.3.3 Armor / Shields / Armor Table' }),
});

const frozen = (v) => Object.freeze(v);
const mode=(id,{combatSkill,attributeOptions,damage,melee=true,shock=null,rangeFeet=null,traits=[]})=>frozen({
  modeId:id,combatSkill,attributeOptions:frozen([...attributeOptions]),damage,melee,
  shock:shock?frozen({...shock}):null,rangeFeet:rangeFeet?frozen({...rangeFeet}):null,traits:frozen([...traits]),
  sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.weapons]),
});
const weapon=(catalogId,name,{priceSilver,encumbrance,traits=[],modes,ammoType=null})=>frozen({
  catalogId,kind:'WEAPON',name,priceSilver,encumbrance,traits:frozen([...traits]),modes:frozen(modes),ammoType,
  sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.weapons]),
});
const ammo=(catalogId,name,{priceSilver,encumbrance,quantity,ammoType})=>frozen({
  catalogId,kind:'AMMO',name,priceSilver,encumbrance,quantity,ammoType,
  sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.adventuringGear]),
});

// Source-bound catalog from the WWN SRD weapon/adventuring gear tables.
// Throwable weapons expose both melee and thrown modes; a thrown mode uses Shoot by default,
// while the service may explicitly select Stab/Exert when a player chooses that legal option.
export const WWN_EQUIPMENT_CATALOG = frozen({
  AXE_HAND:weapon('AXE_HAND','Machado de mão',{priceSilver:10,encumbrance:1,traits:['T'],modes:[
    mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d6',shock:{damage:1,ac:15},traits:['T']}),
    mode('THROWN',{combatSkill:'Shoot',attributeOptions:['Strength','Dexterity'],damage:'1d6',melee:false,rangeFeet:{normal:10,long:30},traits:['T']}),
  ]}),
  AXE_WAR:weapon('AXE_WAR','Machado de guerra',{priceSilver:50,encumbrance:2,traits:['2H'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d10',shock:{damage:3,ac:15},traits:['2H']})]}),
  BLACKJACK:weapon('BLACKJACK','Cassetete',{priceSilver:1,encumbrance:1,traits:['S','LL'],modes:[mode('MELEE',{combatSkill:'Punch',attributeOptions:['Strength','Dexterity'],damage:'1d4',shock:null,traits:['S','LL']})]}),
  BOW_LARGE:weapon('BOW_LARGE','Arco grande',{priceSilver:20,encumbrance:2,traits:['2H','R','PM'],ammoType:'ARROW',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d8',melee:false,rangeFeet:{normal:100,long:600},traits:['2H','R','PM']})]}),
  BOW_SMALL:weapon('BOW_SMALL','Arco pequeno',{priceSilver:20,encumbrance:1,traits:['2H','R','PM'],ammoType:'ARROW',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d6',melee:false,rangeFeet:{normal:50,long:300},traits:['2H','R','PM']})]}),
  CLAW_BLADES:weapon('CLAW_BLADES','Lâminas de garra',{priceSilver:10,encumbrance:1,traits:['S'],modes:[mode('MELEE',{combatSkill:'Punch',attributeOptions:['Strength','Dexterity'],damage:'1d6',shock:{damage:2,ac:13},traits:['S']})]}),
  CLUB:weapon('CLUB','Clava',{priceSilver:0,encumbrance:1,traits:['T','LL'],modes:[
    mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d4',shock:null,traits:['T','LL']}),
    mode('THROWN',{combatSkill:'Shoot',attributeOptions:['Strength','Dexterity'],damage:'1d4',melee:false,rangeFeet:{normal:10,long:30},traits:['T','LL']}),
  ]}),
  CLUB_GREAT:weapon('CLUB_GREAT','Clava grande',{priceSilver:1,encumbrance:2,traits:['2H'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d10',shock:{damage:2,ac:15},traits:['2H']})]}),
  CROSSBOW:weapon('CROSSBOW','Besta',{priceSilver:10,encumbrance:1,traits:['2H','SR','PM'],ammoType:'QUARREL',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d10',melee:false,rangeFeet:{normal:100,long:300},traits:['2H','SR','PM']})]}),
  DAGGER:weapon('DAGGER','Adaga',{priceSilver:3,encumbrance:1,traits:['S','T','PM'],modes:[
    mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d4',shock:{damage:1,ac:15},traits:['S','T','PM']}),
    mode('THROWN',{combatSkill:'Shoot',attributeOptions:['Strength','Dexterity'],damage:'1d4',melee:false,rangeFeet:{normal:30,long:60},traits:['S','T','PM']}),
  ]}),
  HALBERD:weapon('HALBERD','Alabarda',{priceSilver:50,encumbrance:2,traits:['2H','L'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d10',shock:{damage:2,ac:15},traits:['2H','L']})]}),
  HAMMER_GREAT:weapon('HAMMER_GREAT','Martelo grande',{priceSilver:50,encumbrance:2,traits:['2H'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d10',shock:{damage:2,ac:18},traits:['2H']})]}),
  HAMMER_WAR:weapon('HAMMER_WAR','Martelo de guerra',{priceSilver:30,encumbrance:1,traits:[],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d8',shock:{damage:1,ac:18}})]}),
  HURLANT_GREAT:weapon('HURLANT_GREAT','Hurlant grande',{priceSilver:10000,encumbrance:15,traits:['FX','SS','AP'],ammoType:'HURLANT_BOLT',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'3d10',melee:false,rangeFeet:{normal:600,long:2400},traits:['FX','SS','AP']})]}),
  HURLANT_HAND:weapon('HURLANT_HAND','Hurlant de mão',{priceSilver:1000,encumbrance:1,traits:['SS','AP'],ammoType:'HURLANT_BOLT',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d12',melee:false,rangeFeet:{normal:30,long:60},traits:['SS','AP']})]}),
  HURLANT_LONG:weapon('HURLANT_LONG','Hurlant longo',{priceSilver:4000,encumbrance:2,traits:['2H','SS','AP','PM'],ammoType:'HURLANT_BOLT',modes:[mode('RANGED',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'2d8',melee:false,rangeFeet:{normal:200,long:600},traits:['2H','SS','AP','PM']})]}),
  MACE:weapon('MACE','Maça',{priceSilver:15,encumbrance:1,traits:['LL'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d6',shock:{damage:1,ac:18},traits:['LL']})]}),
  PIKE:weapon('PIKE','Pique',{priceSilver:10,encumbrance:2,traits:['2H','L'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d8',shock:{damage:1,ac:18},traits:['2H','L']})]}),
  SPEAR_HEAVY:weapon('SPEAR_HEAVY','Lança pesada',{priceSilver:10,encumbrance:2,traits:['2H'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d10',shock:{damage:2,ac:15},traits:['2H']})]}),
  SPEAR_LIGHT:weapon('SPEAR_LIGHT','Lança leve',{priceSilver:5,encumbrance:1,traits:['T'],modes:[
    mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d6',shock:{damage:2,ac:13},traits:['T']}),
    mode('THROWN',{combatSkill:'Shoot',attributeOptions:['Strength','Dexterity'],damage:'1d6',melee:false,rangeFeet:{normal:30,long:60},traits:['T']}),
  ]}),
  THROWING_BLADE:weapon('THROWING_BLADE','Lâmina de arremesso',{priceSilver:3,encumbrance:1,traits:['S','T','N'],modes:[mode('THROWN',{combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d4',melee:false,rangeFeet:{normal:30,long:60},traits:['S','T','N']})]}),
  STAFF:weapon('STAFF','Cajado',{priceSilver:1,encumbrance:1,traits:['2H','LL'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d6',shock:{damage:1,ac:13},traits:['2H','LL']})]}),
  STILETTO:weapon('STILETTO','Estilete',{priceSilver:10,encumbrance:1,traits:['S','PM'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Dexterity'],damage:'1d4',shock:{damage:1,ac:18},traits:['S','PM']})]}),
  SWORD_GREAT:weapon('SWORD_GREAT','Espada grande',{priceSilver:250,encumbrance:2,traits:['2H'],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength'],damage:'1d12',shock:{damage:2,ac:15},traits:['2H']})]}),
  SWORD_LONG:weapon('SWORD_LONG','Espada longa',{priceSilver:100,encumbrance:1,traits:[],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d8',shock:{damage:2,ac:13}})]}),
  SWORD_SHORT:weapon('SWORD_SHORT','Espada curta',{priceSilver:10,encumbrance:1,traits:[],modes:[mode('MELEE',{combatSkill:'Stab',attributeOptions:['Strength','Dexterity'],damage:'1d6',shock:{damage:2,ac:15}})]}),

  WAR_SHIRT:frozen({catalogId:'WAR_SHIRT',kind:'ARMOR',name:'Camisa de guerra',priceSilver:5,encumbrance:0,armorClassBase:11,armorCategory:'LIGHT',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  BUFF_COAT:frozen({catalogId:'BUFF_COAT',kind:'ARMOR',name:'Casaco reforçado',priceSilver:50,encumbrance:0,armorClassBase:12,armorCategory:'LIGHT',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  LINOTHORAX:frozen({catalogId:'LINOTHORAX',kind:'ARMOR',name:'Linotórax',priceSilver:20,encumbrance:1,armorClassBase:13,armorCategory:'LIGHT',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  WAR_ROBE:frozen({catalogId:'WAR_ROBE',kind:'ARMOR',name:'Manto de guerra',priceSilver:50,encumbrance:3,armorClassBase:14,armorCategory:'LIGHT',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  PIECED_ARMOR:frozen({catalogId:'PIECED_ARMOR',kind:'ARMOR',name:'Armadura composta',priceSilver:100,encumbrance:2,armorClassBase:14,armorCategory:'LIGHT',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  MAIL_SHIRT:frozen({catalogId:'MAIL_SHIRT',kind:'ARMOR',name:'Cota de malha curta',priceSilver:250,encumbrance:1,armorClassBase:14,armorCategory:'MEDIUM',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  CUIRASS_GREAVES:frozen({catalogId:'CUIRASS_GREAVES',kind:'ARMOR',name:'Couraça e grevas',priceSilver:250,encumbrance:2,armorClassBase:15,armorCategory:'MEDIUM',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  SCALED_ARMOR:frozen({catalogId:'SCALED_ARMOR',kind:'ARMOR',name:'Armadura de escamas',priceSilver:500,encumbrance:3,armorClassBase:16,armorCategory:'MEDIUM',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  MAIL_HAUBERK:frozen({catalogId:'MAIL_HAUBERK',kind:'ARMOR',name:'Hauberk de malha',priceSilver:750,encumbrance:2,armorClassBase:16,armorCategory:'HEAVY',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  PLATE_ARMOR:frozen({catalogId:'PLATE_ARMOR',kind:'ARMOR',name:'Armadura de placas',priceSilver:1000,encumbrance:2,armorClassBase:17,armorCategory:'HEAVY',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  GREAT_ARMOR:frozen({catalogId:'GREAT_ARMOR',kind:'ARMOR',name:'Armadura grande',priceSilver:2000,encumbrance:3,armorClassBase:19,armorCategory:'HEAVY',sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  GRAND_PLATE:frozen({catalogId:'GRAND_PLATE',kind:'ARMOR',name:'Grande placa',priceSilver:2000,encumbrance:3,armorClassBase:16,armorCategory:'HEAVY',specialRules:['GRAND_PLATE_WEAPON_IMMUNITY'],sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  SHIELD_SMALL:frozen({catalogId:'SHIELD_SMALL',kind:'SHIELD',name:'Escudo pequeno',priceSilver:20,encumbrance:1,shieldBaseArmorClass:13,sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  SHIELD_LARGE:frozen({catalogId:'SHIELD_LARGE',kind:'SHIELD',name:'Escudo grande',priceSilver:10,encumbrance:1,shieldBaseArmorClass:14,sourceRefs:frozen([WWN_EQUIPMENT_SOURCES.armor])}),
  ARROWS_20:ammo('ARROWS_20','Flechas, 20',{priceSilver:2,encumbrance:1,quantity:20,ammoType:'ARROW'}),
  HURLANT_BOLTS_20:ammo('HURLANT_BOLTS_20','Virotes de hurlant, 20',{priceSilver:20,encumbrance:1,quantity:20,ammoType:'HURLANT_BOLT'}),
});

export function equipmentCatalogItem(catalogId){return WWN_EQUIPMENT_CATALOG[String(catalogId??'').toUpperCase()]??null;}

export function bestWeaponAttribute(character,attributeOptions){
  if(!character||!Array.isArray(attributeOptions)||attributeOptions.length<1)return null;
  const score=(k)=>Number(character.attributes?.[k]??-Infinity);
  return [...attributeOptions].sort((a,b)=>score(b)-score(a)||String(a).localeCompare(String(b)))[0]??null;
}

export function materializeWeaponContract(character,item,{modeId=null,combatSkill=null}={}){
  if(!item||item.kind!=='WEAPON')return Object.freeze({status:'NOT_A_WEAPON'});
  const modes=item.weaponModes??item.modes;
  if(!Array.isArray(modes)||modes.length<1)return Object.freeze({status:'MISSING_WEAPON_MODE'});
  const chosen=modes.find(m=>m.modeId===(modeId??item.activeModeId))??modes[0];
  let skill=combatSkill??chosen.combatSkill;
  if(chosen.modeId==='THROWN'&&combatSkill!=null&&!['Shoot','Stab','Exert'].includes(combatSkill))return Object.freeze({status:'INVALID_THROWN_COMBAT_SKILL'});
  // Thrown weapons may use Shoot, Stab, or Exert when explicitly selected by the player.
  const attribute=bestWeaponAttribute(character,chosen.attributeOptions);
  if(!attribute)return Object.freeze({status:'MISSING_WEAPON_ATTRIBUTE'});
  return Object.freeze({status:'OK',weapon:Object.freeze({
    weaponId:item.itemId??item.catalogId,name:item.name,combatSkill:skill,attribute,damage:chosen.damage,melee:chosen.melee,
    shock:chosen.shock?Object.freeze({...chosen.shock}):null,rangeFeet:chosen.rangeFeet?Object.freeze({...chosen.rangeFeet}):null,
    traits:Object.freeze([...(chosen.traits??item.traits??[])]),longMelee:(chosen.traits??[]).includes('L'),lessLethal:(chosen.traits??[]).includes('LL'),
    sourceRefs:Object.freeze([...(chosen.sourceRefs??item.sourceRefs??[WWN_EQUIPMENT_SOURCES.weapons])]),
  }),modeId:chosen.modeId});
}
