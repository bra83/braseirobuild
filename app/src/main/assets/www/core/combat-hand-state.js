function upper(v){return String(v??'').toUpperCase()}
function itemKind(item){return upper(item?.kind??item?.itemKind??item?.category)}
function readied(item){return upper(item?.carryState)==='READIED'}
function explicitHands(item){const n=item?.handsRequired??item?.handUse??item?.hands;if(Number.isInteger(n)&&n>=0&&n<=2)return n;return null}

export function inspectCharacterHands(character){
  if(!character||typeof character!=='object')return Object.freeze({status:'CHARACTER_REQUIRED'});const inventory=Array.isArray(character.inventory)?character.inventory:[];let occupied=0;const unknown=[];const contributors=[];
  for(const item of inventory){if(!readied(item))continue;const explicit=explicitHands(item);if(explicit!==null){occupied+=explicit;contributors.push(Object.freeze({itemId:item.itemId??item.id??null,hands:explicit,reason:'EXPLICIT'}));continue;}const kind=itemKind(item);if(kind==='ARMOR'||kind==='AMMO'||kind==='AMMUNITION')continue;if(kind==='SHIELD'){occupied+=1;contributors.push(Object.freeze({itemId:item.itemId??item.id??null,hands:1,reason:'SHIELD'}));continue;}if(kind==='WEAPON'){const traits=(item.traits??item.weapon?.traits??[]).map(upper);const hands=traits.includes('2H')?2:1;occupied+=hands;contributors.push(Object.freeze({itemId:item.itemId??item.id??null,hands,reason:traits.includes('2H')?'TWO_HANDED_WEAPON':'WEAPON'}));continue;}unknown.push(item.itemId??item.id??item.name??'READIED_ITEM');}
  if(unknown.length)return Object.freeze({status:'MISSING_HAND_OCCUPANCY_EVIDENCE',unknown:Object.freeze(unknown),occupiedKnown:Math.min(2,occupied),contributors:Object.freeze(contributors)});return Object.freeze({status:'OK',occupiedHands:Math.min(2,occupied),freeHands:Math.max(0,2-occupied),bothHandsFree:occupied===0,contributors:Object.freeze(contributors)});
}
