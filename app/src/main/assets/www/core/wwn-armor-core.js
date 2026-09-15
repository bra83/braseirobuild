import { attributeModifier } from './wwn-rules-core.js';
import { inventoryLoad } from './wwn-inventory-core.js';

export const WWN_ARMOR_SOURCES=Object.freeze({
  armor:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.3.0 Armor'}),
  categories:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.3.1 Light, Medium, and Heavy Armor'}),
  shields:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.3.2 Shields'}),
});

function readiedInventory(character){return Array.isArray(character?.inventory)?character.inventory.filter(i=>String(i?.carryState??'').toUpperCase()==='READIED'):null;}

export function deriveArmorClass(character){
  if(!character||typeof character!=='object')return Object.freeze({status:'MISSING_CHARACTER'});
  const dex=character.attributes?.Dexterity;
  if(!Number.isInteger(dex))return Object.freeze({status:'MISSING_EVIDENCE_DEXTERITY'});
  const dexMod=attributeModifier(dex);const readied=readiedInventory(character);
  if(readied===null){if(Number.isInteger(character.armorClass))return Object.freeze({status:'OK',armorClass:character.armorClass,baseArmorClass:null,dexterityModifier:dexMod,legacyExplicit:true,sourceRefs:[WWN_ARMOR_SOURCES.armor]});return Object.freeze({status:'MISSING_EVIDENCE_INVENTORY_AND_ARMOR_CLASS'});}
  const armors=readied.filter(i=>i?.kind==='ARMOR'&&Number.isInteger(i.armorClassBase));const shields=readied.filter(i=>i?.kind==='SHIELD'&&Number.isInteger(i.shieldBaseArmorClass));
  const armor=armors.sort((a,b)=>b.armorClassBase-a.armorClassBase||String(a.itemId??a.catalogId).localeCompare(String(b.itemId??b.catalogId)))[0]??null;
  const shield=shields.sort((a,b)=>b.shieldBaseArmorClass-a.shieldBaseArmorClass||String(a.itemId??a.catalogId).localeCompare(String(b.itemId??b.catalogId)))[0]??null;
  let base=armor?.armorClassBase??10;let shieldBonus=0;
  if(shield){if(base>=shield.shieldBaseArmorClass)shieldBonus=1;else base=shield.shieldBaseArmorClass;}
  return Object.freeze({status:'OK',armorClass:base+shieldBonus+dexMod,baseArmorClass:base,shieldBonus,dexterityModifier:dexMod,armorItemId:armor?.itemId??null,shieldItemId:shield?.itemId??null,hasShield:Boolean(shield),sourceRefs:[WWN_ARMOR_SOURCES.armor,WWN_ARMOR_SOURCES.shields]});
}

export function armorSkillPenalty(character,skillName){
  const readied=readiedInventory(character);if(readied===null)return Object.freeze({status:'MISSING_EVIDENCE_INVENTORY'});
  const armor=readied.filter(i=>i?.kind==='ARMOR'&&['MEDIUM','HEAVY'].includes(i.armorCategory)).sort((a,b)=>(b.armorClassBase??0)-(a.armorClassBase??0))[0]??null;
  if(!armor)return Object.freeze({status:'OK',penalty:0,armorItemId:null,sourceRefs:[WWN_ARMOR_SOURCES.categories]});
  const skill=String(skillName??'');let penalty=0;if(armor.armorCategory==='MEDIUM'&&skill==='Sneak')penalty=-(armor.encumbrance??0);if(armor.armorCategory==='HEAVY'&&['Sneak','Exert'].includes(skill))penalty=-(armor.encumbrance??0);
  return Object.freeze({status:'OK',penalty,armorItemId:armor.itemId??null,armorCategory:armor.armorCategory,sourceRefs:[WWN_ARMOR_SOURCES.categories]});
}

export function deriveCombatMoveFeet(character){
  if(!character||typeof character!=='object')return Object.freeze({status:'MISSING_CHARACTER'});const load=inventoryLoad(character);if(load.status==='OK')return Object.freeze({status:'OK',moveFeet:load.assessment.moveFeet,load,sourceRefs:load.sourceRefs});
  // Existing canonical fixtures may have no item list but do have no evidence of overload; do not infer a reduced move.
  // Callers can only use 30 as legacy explicit/default if the inventory is truly absent and no contradictory move is stored.
  if(load.status==='MISSING_EVIDENCE'&&Number.isFinite(character.moveFeet))return Object.freeze({status:'OK',moveFeet:character.moveFeet,legacyExplicit:true,sourceRefs:[]});
  if(load.status==='MISSING_EVIDENCE')return Object.freeze({status:'OK',moveFeet:30,legacyDefault:true,sourceRefs:[]});
  return Object.freeze({status:load.status,reason:load.reason??null});
}
