/** SWE09 The Jewel of Yavin printed p.69, Edge of the Empire Core p.179.
 * Source-backed, physical security equipment. A restraining bolt is NOT the
 * translated 'restraining_device' controller. New slicer gear costs 500 cr.
 * Replacing the slicer's complete toolkit removes future signature penalties,
 * but does not erase the bank's historical evidence of the intrusion.
 */
import {generalGearCatalogEntry} from '../rules/equipmentCatalog.mjs';
import {replayJewelSlicingSpends} from './jewelBankSlicingSpends.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
export const BOLT_ID='restraining_bolt';
export const SLICER_GEAR_ID='slicer_gear';
export function ownedGear(character,id){
  return (character?.gear??[]).find(g=>(g?.sourceCatalogId??g?.id)===id&&Number.isSafeInteger(g?.quantity??1)&&(g?.quantity??1)>0)??null;
}
export function boltInventoryPreflight(character,{spotWelderConfirmed=false}={}){
  const item=ownedGear(character,BOLT_ID);
  if(!item)throw Error('Restraining bolt REAL não está no inventário; restraining_device não é um bolt.');
  if(generalGearCatalogEntry(BOLT_ID)?.price!==35)throw Error('Catálogo oficial do bolt não corresponde à fonte.');
  // EotE Core: attachment takes a few seconds and requires a spot-welder.
  // Its exact standalone price is not printed: accept owned tool kit (which
  // explicitly includes welders) or a GM-confirmed external welder, never
  // invent a welder purchase or consume the complete tool kit.
  const welderSource=ownedGear(character,'tool_kit')?'tool_kit':spotWelderConfirmed===true?'gm_confirmed_spot_welder':null;
  if(!welderSource)throw Error('Instalar o bolt exige soldador real: kit de ferramentas ou confirmação do Mestre.');
  return {itemId:BOLT_ID,actorId:String(character.id??character.name??'operador'),welderSource};
}
export function attachOwnedBolt(character){
  const c=structuredClone(character),item=ownedGear(c,BOLT_ID);
  if(!item)throw Error('Restraining bolt não está disponível para instalar no G-100.');
  if((item.quantity??1)===1)c.gear.splice(c.gear.indexOf(item),1);
  else item.quantity--;
  return c;
}
export function replayJewelBankToolReplacement(progress){
  const receipts=progress?.operations?.receipts??{};
  const spends=replayJewelSlicingSpends(receipts);
  let captured=false,replaced=null;
  for(const [id,r] of Object.entries(receipts)){
    if(r?.kind==='BANK_SLICE_SPEND'&&r.choice?.signatureDespair){captured=true;continue;}
    if(r?.kind!=='BANK_TOOL_REPLACE')continue;
    const gear=generalGearCatalogEntry(SLICER_GEAR_ID);
    if(replaced||!captured||!spends?.signatureCaptured||!validId(id)||r.id!==id||
      Object.keys(r).some(k=>!['id','kind','actorId','itemId','price','creditsBefore','creditsAfter','gmApproved','oldGearRemoved','sourceTable'].includes(k))||
      r.itemId!==SLICER_GEAR_ID||r.sourceTable!=='EOTE_CORE_5_8'||r.price!==gear?.price||
      r.gmApproved!==true||typeof r.actorId!=='string'||!r.actorId||r.actorId.length>120||
      !Number.isSafeInteger(r.creditsBefore)||r.creditsBefore<r.price||
      r.creditsAfter!==r.creditsBefore-r.price||typeof r.oldGearRemoved!=='boolean')
      throw Error('Recibo de substituição integral das ferramentas adulterado ou fora de ordem.');
    replaced={signatureCleared:true,replacementReceiptId:id,actorId:r.actorId,creditsSpent:r.price};
  }
  return replaced;
}
export function validateJewelBoltPurchases(progress){
  const receipts=progress?.operations?.receipts??{};
  for(const [id,r] of Object.entries(receipts)){
    if(r?.kind!=='BANK_BOLT_BUY')continue;
    if(!validId(id)||r.id!==id||
       Object.keys(r).some(k=>!['id','kind','actorId','itemId','price','creditsBefore','creditsAfter','gmApproved','sourceTable'].includes(k))||
       r.itemId!==BOLT_ID||r.price!==35||r.sourceTable!=='EOTE_CORE_5_8'||r.gmApproved!==true||
       typeof r.actorId!=='string'||!r.actorId||r.actorId.length>120||
       !Number.isSafeInteger(r.creditsBefore)||r.creditsBefore<35||r.creditsAfter!==r.creditsBefore-35)
      throw Error('Compra do restraining bolt sem recibo oficial válido.');
  }
  return true;
}
export function buyJewelRestrainingBolt(world,character,{actionId,gmApproved=false}={}){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
    throw Error('Compra do bolt exige cena bancária ativa.');
  if(!validId(actionId)||gmApproved!==true)throw Error('Compra do bolt exige aprovação do Mestre.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Compra já registrada; pagamento duplicado bloqueado.');
  const gear=generalGearCatalogEntry(BOLT_ID);
  if(gear?.price!==35||gear?.encumbrance!==0||gear?.rarity!==0)throw Error('Fonte do bolt não corresponde ao catálogo.');
  if(!Number.isSafeInteger(character?.credits)||character.credits<gear.price)
    throw Error('Créditos insuficientes para restraining bolt (35 cr).');
  const c=structuredClone(character),before=c.credits;
  c.credits-=gear.price;c.gear??=[];
  const old=ownedGear(c,BOLT_ID);
  if(old)old.quantity=(old.quantity??1)+1;
  else c.gear.push({id:BOLT_ID,sourceCatalogId:BOLT_ID,name:gear.name,quantity:1,encumbrance:0});
  const r={id:actionId,kind:'BANK_BOLT_BUY',actorId:String(c.id??c.name??'operador'),
    itemId:BOLT_ID,price:35,creditsBefore:before,creditsAfter:c.credits,gmApproved:true,sourceTable:'EOTE_CORE_5_8'};
  p.operations.receipts[actionId]=r;p.updatedAt=new Date().toISOString();
  validateJewelBoltPurchases(p);
  return {world:w,character:c,adjudication:structuredClone(r)};
}
export function validateJewelBankToolReplacement(progress){
  const expected=replayJewelBankToolReplacement(progress);
  if(!same(progress?.operations?.bankTools??null,expected))
    throw Error('Estado de substituição de ferramentas não corresponde aos recibos.');
  return expected;
}
export function replaceJewelSlicerTools(world,character,{actionId,gmApproved=false}={}){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE')throw Error('Substituição de slicing exige The Jewel of Yavin ativa.');
  if(!validId(actionId)||gmApproved!==true)throw Error('Identificador e aprovação do Mestre obrigatórios.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Substituição já registrada; não debitar duas vezes.');
  if(!replayJewelSlicingSpends(p.operations.receipts)?.signatureCaptured)
    throw Error('Não há assinatura eletrônica capturada para substituir ferramentas.');
  if(validateJewelBankToolReplacement(p))throw Error('Ferramentas já substituídas nesta operação.');
  const gear=generalGearCatalogEntry(SLICER_GEAR_ID);
  if(gear?.price!==500||gear?.encumbrance!==2||gear?.rarity!==4)
    throw Error('Equipamento de slicing sem preço/volume/raridade oficial.');
  if(!Number.isSafeInteger(character?.credits)||character.credits<gear.price)
    throw Error('Créditos insuficientes para novo equipamento de slicing (500 cr).');
  const c=structuredClone(character),before=c.credits,old=ownedGear(c,SLICER_GEAR_ID);
  if(old){if((old.quantity??1)===1)c.gear.splice(c.gear.indexOf(old),1);else old.quantity--;}
  c.credits-=gear.price;
  c.gear??=[];
  c.gear.push({id:SLICER_GEAR_ID,sourceCatalogId:SLICER_GEAR_ID,name:gear.name,
    quantity:1,encumbrance:gear.encumbrance,acquisitionReceiptId:actionId});
  const r={id:actionId,kind:'BANK_TOOL_REPLACE',actorId:String(c.id??c.name??'operador'),
    itemId:SLICER_GEAR_ID,price:gear.price,creditsBefore:before,creditsAfter:c.credits,
    gmApproved:true,oldGearRemoved:Boolean(old),sourceTable:'EOTE_CORE_5_8'};
  p.operations.receipts[actionId]=r;
  p.operations.bankTools=replayJewelBankToolReplacement(p);
  p.updatedAt=new Date().toISOString();
  validateJewelBankToolReplacement(p);
  return {world:w,character:c,adjudication:structuredClone(r),bankTools:structuredClone(p.operations.bankTools)};
}
