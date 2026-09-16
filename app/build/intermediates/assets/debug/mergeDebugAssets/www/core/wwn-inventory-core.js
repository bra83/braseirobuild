import { attributeModifier } from './wwn-rules-core.js';

export const WWN_INVENTORY_SOURCES = Object.freeze({
  encumbrance: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.9.0 Encumbrance' }),
  bundledGear: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.9.1 Bundled Gear' }),
  gearTable: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§3.2.0 Adventuring Gear' }),
  crafting: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.8.1 Crafting Gear / §2.8.4 Example Modifications' }),
});

export function encumbranceCapacities(strengthScore) {
  attributeModifier(strengthScore);
  return Object.freeze({ stowed: strengthScore, readied: Math.floor(strengthScore / 2) });
}

export function assessEncumbrance({ strengthScore, readiedPoints = 0, stowedPoints = 0 }) {
  if (![readiedPoints, stowedPoints].every(Number.isInteger) || readiedPoints < 0 || stowedPoints < 0) throw new RangeError('encumbrance points must be non-negative integers');
  const cap = encumbranceCapacities(strengthScore);
  const overReady = readiedPoints - cap.readied;
  const overStowed = stowedPoints - cap.stowed;
  let status = 'NORMAL'; let moveFeet = 30;
  if (overReady > 4 || overStowed > 8) { status = 'IMPRACTICAL'; moveFeet = 10; }
  else if (overReady > 2 || overStowed > 4) { status = 'OVERLOAD_2'; moveFeet = 10; }
  else if (overReady > 0 || overStowed > 0) { status = 'OVERLOAD_1'; moveFeet = 20; }
  return Object.freeze({ sourceRefs: [WWN_INVENTORY_SOURCES.encumbrance], capacities: cap, readiedPoints, stowedPoints, overReady: Math.max(0, overReady), overStowed: Math.max(0, overStowed), status, moveFeet });
}

export function bundledGearEncumbrance({ itemCount, eligible }) {
  if (!Number.isInteger(itemCount) || itemCount < 0) throw new RangeError('itemCount must be a non-negative integer');
  if (!eligible) return Object.freeze({ bundled: false, encumbrance: itemCount, sourceRefs: [WWN_INVENTORY_SOURCES.bundledGear] });
  return Object.freeze({ bundled: true, encumbrance: Math.ceil(itemCount / 3), sourceRefs: [WWN_INVENTORY_SOURCES.bundledGear] });
}

function activeMod(item,modId,character){
  return (item?.mods??[]).find(m=>m?.status==='ACTIVE'&&String(m.modId).toUpperCase()===modId&&(m.specificUserId==null||String(m.specificUserId)===String(character?.characterId)))??null;
}

export function effectiveItemEncumbrance(character,item){
  if(!item||typeof item!=='object')throw new TypeError('inventory item required');
  const base=item.encumbrance??0;
  if(!Number.isInteger(base)||base<0)throw new RangeError('inventory item encumbrance must be a non-negative integer');
  const carryState=String(item.carryState??'STOWED').toUpperCase();
  if(carryState==='WORN'&&item.weightRule==='NO_ENCUMBRANCE_WHILE_WORN')return Object.freeze({base,effective:0,reason:'WORN_GEAR_TABLE_EXCEPTION',sourceRefs:[WWN_INVENTORY_SOURCES.gearTable]});
  let reduction=0;
  if(item?.crafting?.quality==='MASTERCRAFTED'&&String(item.kind??'').toUpperCase()==='ARMOR')reduction+=Number(item.crafting.mastercraftedArmorEncumbranceReduction??1);
  if(activeMod(item,'TAILORED_HARNESS',character))reduction+=1;
  const effective=reduction>0?Math.max(1,base-reduction):base;
  return Object.freeze({base,effective,reduction,reason:reduction?'CRAFTING_OR_MOD_REDUCTION':'BASE',sourceRefs:reduction?[WWN_INVENTORY_SOURCES.crafting]:[WWN_INVENTORY_SOURCES.encumbrance]});
}

export function inspectInventoryEvidence(character) {
  if (!character || typeof character !== 'object') return Object.freeze({ status: 'MISSING_CHARACTER' });
  if (!Array.isArray(character.inventory)) return Object.freeze({ status: 'MISSING_EVIDENCE', reason: 'character.inventory absent' });
  return Object.freeze({ status: 'OK', itemCount: character.inventory.length, items: structuredClone(character.inventory) });
}

export function inventoryLoad(character) {
  const evidence=inspectInventoryEvidence(character);
  if(evidence.status!=='OK')return Object.freeze({status:evidence.status,reason:evidence.reason??null});
  let readiedPoints=0,stowedPoints=0,wornPoints=0;
  for(const item of character.inventory){
    const enc=effectiveItemEncumbrance(character,item).effective;
    const state=String(item.carryState??'STOWED').toUpperCase();
    if(state==='READIED')readiedPoints+=enc;
    else if(state==='STOWED')stowedPoints+=enc;
    else if(state==='WORN'){
      if(item.weightRule!=='NO_ENCUMBRANCE_WHILE_WORN')throw new RangeError('only gear marked as no-encumbrance-while-worn may use WORN carryState');
      wornPoints+=enc;
    } else throw new RangeError('carryState must be READIED, STOWED, or WORN');
  }
  return Object.freeze({status:'OK',readiedPoints,stowedPoints,wornPoints,assessment:assessEncumbrance({strengthScore:character.attributes?.Strength,readiedPoints,stowedPoints}),sourceRefs:[WWN_INVENTORY_SOURCES.encumbrance,WWN_INVENTORY_SOURCES.gearTable,WWN_INVENTORY_SOURCES.crafting]});
}

export function moveInventoryItem(character,{itemId,to}) {
  const evidence=inspectInventoryEvidence(character);if(evidence.status!=='OK')return Object.freeze({status:evidence.status,reason:evidence.reason??null});
  const target=String(to??'').toUpperCase();if(!['READIED','STOWED','WORN'].includes(target))throw new RangeError('to must be READIED, STOWED, or WORN');
  const index=character.inventory.findIndex(i=>(i.itemId??i.id)===itemId);if(index<0)return Object.freeze({status:'ITEM_NOT_FOUND'});
  if(target==='WORN'&&character.inventory[index].weightRule!=='NO_ENCUMBRANCE_WHILE_WORN')return Object.freeze({status:'ITEM_CANNOT_BE_WORN_FOR_ZERO_ENCUMBRANCE'});
  const next=structuredClone(character);next.inventory[index].carryState=target;
  return Object.freeze({status:'OK',character:next,load:inventoryLoad(next),actionCost:'MAIN_ACTION',sourceRefs:[WWN_INVENTORY_SOURCES.encumbrance,WWN_INVENTORY_SOURCES.gearTable]});
}

export function breakBundledInventoryItem(character,{itemId}){
  const evidence=inspectInventoryEvidence(character);if(evidence.status!=='OK')return Object.freeze({status:evidence.status,reason:evidence.reason??null});
  const index=character.inventory.findIndex(i=>(i.itemId??i.id)===itemId);if(index<0)return Object.freeze({status:'ITEM_NOT_FOUND'});
  const source=character.inventory[index];
  if(source.weightRule!=='BUNDLED_THREE'||source.quantity!==3)return Object.freeze({status:'ITEM_IS_NOT_A_THREE_UNIT_BUNDLE'});
  const next=structuredClone(character);const bundle=next.inventory[index];const unitName=String(bundle.name??'Bundled item').replace(/,?\s*3 bundled$/i,'');
  const units=Array.from({length:3},(_,n)=>({itemId:`${bundle.itemId}:unit:${n+1}`,catalogId:bundle.catalogId,name:unitName,kind:bundle.kind,priceCopper:Number.isInteger(bundle.priceCopper)?Math.floor(bundle.priceCopper/3):null,priceSilver:Number.isFinite(bundle.priceSilver)?bundle.priceSilver/3:null,encumbrance:1,carryState:'STOWED',weightRule:'BUNDLE_THREE_FOR_ONE_ENCUMBRANCE',sourceRefs:structuredClone(bundle.sourceRefs??[])}));
  next.inventory.splice(index,1,...units);
  return Object.freeze({status:'OK',character:next,createdItemIds:Object.freeze(units.map(x=>x.itemId)),load:inventoryLoad(next),actionCost:'MAIN_ACTION',sourceRefs:[WWN_INVENTORY_SOURCES.bundledGear,WWN_INVENTORY_SOURCES.gearTable]});
}
