import { attributeModifier } from './wwn-rules-core.js';

export const WWN_INVENTORY_SOURCES = Object.freeze({
  encumbrance: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.9.0 Encumbrance' }),
  bundledGear: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§2.9.1 Bundled Gear' }),
});

export function encumbranceCapacities(strengthScore) {
  attributeModifier(strengthScore); // range validation is identical to an attribute score
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

export function inspectInventoryEvidence(character) {
  if (!character || typeof character !== 'object') return Object.freeze({ status: 'MISSING_CHARACTER' });
  if (!Array.isArray(character.inventory)) return Object.freeze({ status: 'MISSING_EVIDENCE', reason: 'character.inventory absent' });
  return Object.freeze({ status: 'OK', itemCount: character.inventory.length, items: structuredClone(character.inventory) });
}

export function inventoryLoad(character) {
  const evidence=inspectInventoryEvidence(character);
  if(evidence.status!=='OK')return Object.freeze({status:evidence.status,reason:evidence.reason??null});
  let readiedPoints=0,stowedPoints=0;
  for(const item of character.inventory){
    const enc=item.encumbrance??0;
    if(!Number.isInteger(enc)||enc<0)throw new RangeError('inventory item encumbrance must be a non-negative integer');
    const state=String(item.carryState??'STOWED').toUpperCase();
    if(state==='READIED')readiedPoints+=enc;else if(state==='STOWED')stowedPoints+=enc;else throw new RangeError('carryState must be READIED or STOWED');
  }
  return Object.freeze({status:'OK',readiedPoints,stowedPoints,assessment:assessEncumbrance({strengthScore:character.attributes?.Strength,readiedPoints,stowedPoints}),sourceRefs:[WWN_INVENTORY_SOURCES.encumbrance]});
}

export function moveInventoryItem(character,{itemId,to}) {
  const evidence=inspectInventoryEvidence(character);if(evidence.status!=='OK')return Object.freeze({status:evidence.status,reason:evidence.reason??null});
  const target=String(to??'').toUpperCase();if(!['READIED','STOWED'].includes(target))throw new RangeError('to must be READIED or STOWED');
  const index=character.inventory.findIndex(i=>(i.itemId??i.id)===itemId);if(index<0)return Object.freeze({status:'ITEM_NOT_FOUND'});
  const next=structuredClone(character);next.inventory[index].carryState=target;
  return Object.freeze({status:'OK',character:next,load:inventoryLoad(next),actionCost:'MAIN_ACTION',sourceRefs:[WWN_INVENTORY_SOURCES.encumbrance]});
}
