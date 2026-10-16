// SWE02 printed pp.13–14: Nabat docking, market and militant safehouse.
// The story engine never mints currency or inventory. All mutations are
// computed here from catalog records and committed by GameController.
import {equipmentCatalog} from '../rules/equipmentCatalog.mjs';
import {healWounds,recoverStrain} from '../rules/health.mjs';
const clone=v=>structuredClone(v);
export const NABAT_SOURCE=Object.freeze({productCode:'SWE02',printedPages:[13,14],authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const SCENES=new Set(['nabat-arrival','nabat-contact','krayt-fang-ground-inspection']);
function active(world,allowed=SCENES){
 const p=world?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||!allowed.has(p.currentNodeId))
  throw Error('SWE02: operação de Nabat fora da cena oficial.');
 return p;
}
const key=c=>String(c?.id||c?.name||'').trim();
function receipt(p,type,detail){
 p.nabatReceipts=Array.isArray(p.nabatReceipts)?p.nabatReceipts:[];
 const r={type,nodeId:p.currentNodeId,source:NABAT_SOURCE,...detail};
 p.nabatReceipts.push(r);return r;
}
export function nabatMarketCatalog(){
 const catalog=equipmentCatalog(),result=[];
 for(const [kind,items] of [['gear',catalog.generalGear],['armor',catalog.armor],['weapon',catalog.weapons]]){
  for(const item of items){
   // Cross-line AoR reference entries are not EotE Core-certified goods.
   if(item.source?.activeRulesAuthority!==true)continue;
   if(!Number.isSafeInteger(item.price)||item.price<1||item.price>500||item.restricted)continue;
   result.push({kind,id:item.id,name:item.name,price:item.price,source:clone(item.source)});
  }
 }
 return result;
}
export function nabatPurchase(world,character,{itemId,quantity=1}={}){
 const w=clone(world),c=clone(character),p=active(w);
 if(!Number.isSafeInteger(quantity)||quantity<1||quantity>10)throw Error('SWE02: quantidade inválida (1–10).');
 const listing=nabatMarketCatalog().find(x=>x.id===itemId);
 if(!listing)throw Error('SWE02: item indisponível em Nabat; máximo 500 créditos, fonte EotE Core.');
 const cost=listing.price*quantity;
 if(!Number.isSafeInteger(c.credits)||c.credits<cost)throw Error('SWE02: créditos insuficientes para compra em Nabat.');
 const full=equipmentCatalog()[listing.kind==='gear'?'generalGear':listing.kind==='armor'?'armor':'weapons'].find(x=>x.id===itemId);
 if(!full)throw Error('SWE02: catálogo incompleto.');
 const item={...clone(full),sourceCatalogId:full.id,quantity,ownedAssetId:`swe02-nabat:${(p.nabatReceipts||[]).length+1}:${itemId}`};
 c.credits-=cost;
 const bucket=listing.kind==='gear'?'gear':listing.kind==='armor'?'armor':'weapons';
 c[bucket]=[...(c[bucket]||[]),item];
 const r=receipt(p,'MARKET_PURCHASE',{itemId,quantity,unitPrice:listing.price,cost,characterKey:key(c),remainingCredits:c.credits});
 return {world:w,character:c,receipt:r,item};
}
export function requestNabatSpaceportWatch(world){
 const w=clone(world),p=active(w,new Set(['nabat-arrival','nabat-contact']));
 if(p.flags?.nabatSpaceportWatchRequested)throw Error('SWE02: vigia já solicitada.');
 p.flags={...p.flags,nabatSpaceportWatchRequested:true};
 const r=receipt(p,'SPACEPORT_WATCH_REQUESTED',{});
 return {world:w,receipt:r};
}
export function deliverNabatNightAlert(world){
 const w=clone(world),p=active(w,new Set(['nabat-contact']));
 if(!p.flags?.nabatSpaceportWatchRequested)throw Error('SWE02: nenhum pedido de aviso feito ao porto.');
 if(p.flags.nabatSpaceportAlertDelivered)throw Error('SWE02: aviso já entregue.');
 p.flags={...p.flags,nabatSpaceportAlertDelivered:true};
 const r=receipt(p,'TATOOINE_VISITORS_ALERT',{visitors:['human','gand'],arrivalLeadHours:2});
 return {world:w,receipt:r};
}
export function nabatShipRepair(world,vehicles){
 const w=clone(world),v=clone(vehicles),p=active(w,new Set(['nabat-arrival']));
 if(!p.flags?.shipBuraFound)throw Error('SWE02: B’ura B’an não foi encontrado.');
 if(p.flags?.nabatHullRepaired)throw Error('SWE02: reparo de casco já pago pelos contatos.');
 if(v.combat?.active)throw Error('SWE02: não é possível reparar durante combate veicular.');
 const ship=v.vehicles?.[v.activeVehicleId];
 if(!ship||ship.key!=='yt1300'||String(ship.name||'').trim().toLowerCase()!=='krayt fang')
  throw Error('SWE02: selecione a Krayt Fang real; nenhuma nave é criada.');
 const before=ship.hullDamage;
 if(!Number.isFinite(before)||before<0)throw Error('SWE02: estado de casco inválido.');
 ship.hullDamage=0;
 p.flags={...p.flags,nabatHullRepaired:true};
 const r=receipt(p,'CONTACTS_PAID_HULL_REPAIR',{vehicleId:ship.id,hullBefore:before,hullAfter:0,creditsSpent:0,
  criticalShipDamageUntouched:true,systemStrainUntouched:true});
 return {world:w,vehicles:v,receipt:r};
}
export function nabatSafehouseCare(world,character){
 const w=clone(world),p=active(w,new Set(['nabat-contact']));
 const who=key(character);
 if(!who)throw Error('SWE02: personagem sem identidade para cuidados.');
 if((p.nabatReceipts||[]).some(x=>x.type==='SAFEHOUSE_CARE'&&x.characterKey===who))
  throw Error('SWE02: atendimento normal já realizado para este personagem.');
 const healed=healWounds(character,Math.max(0,Number(character?.derived?.wounds?.current)||0));
 const rested=recoverStrain(healed.character,Math.max(0,Number(healed.character?.derived?.strain?.current)||0));
 const r=receipt(p,'SAFEHOUSE_CARE',{characterKey:who,woundsHealed:healed.healed,strainRecovered:rested.recovered,
  criticalInjuriesRemain:(rested.character.criticalInjuries||[]).length,criticalTreatmentPending:true});
 return {world:w,character:rested.character,receipt:r};
}
export function validateNabat(p){
 if(p?.id!=='long-arm-of-the-hutt')return true;
 const f=p.flags||{},rr=p.nabatReceipts||[];
 if(!Array.isArray(rr))throw Error('SWE02: histórico de Nabat inválido.');
 const one=t=>rr.filter(x=>x.type===t);
 if(one('SPACEPORT_WATCH_REQUESTED').length>1||one('TATOOINE_VISITORS_ALERT').length>1||one('CONTACTS_PAID_HULL_REPAIR').length>1)
  throw Error('SWE02: operação única de Nabat duplicada.');
 if(Boolean(f.nabatSpaceportWatchRequested)!==Boolean(one('SPACEPORT_WATCH_REQUESTED').length)||
    Boolean(f.nabatSpaceportAlertDelivered)!==Boolean(one('TATOOINE_VISITORS_ALERT').length)||
    Boolean(f.nabatHullRepaired)!==Boolean(one('CONTACTS_PAID_HULL_REPAIR').length))
  throw Error('SWE02: sinalizador de Nabat sem recibo correspondente.');
 if(f.nabatSpaceportAlertDelivered&&!f.nabatSpaceportWatchRequested)throw Error('SWE02: alerta sem solicitação prévia.');
 const catalog=nabatMarketCatalog();
 for(const x of rr){
  if(x.source?.productCode!=='SWE02'||x.source?.printedPages?.[0]!==13||!SCENES.has(x.nodeId))
   throw Error('SWE02: recibo de Nabat sem proveniência.');
  if(x.type==='MARKET_PURCHASE'){
   const item=catalog.find(y=>y.id===x.itemId);
   if(!item||!Number.isSafeInteger(x.quantity)||x.quantity<1||x.quantity>10||x.unitPrice!==item.price||
     x.cost!==x.unitPrice*x.quantity||!Number.isSafeInteger(x.remainingCredits)||x.remainingCredits<0)
    throw Error('SWE02: preço ou compra adulterados.');
  }
  if(x.type==='CONTACTS_PAID_HULL_REPAIR'&&(!f.shipBuraFound||x.creditsSpent!==0||x.hullAfter!==0||x.hullBefore<0))
   throw Error('SWE02: reparo sem B’ura ou sem prova de casco.');
  if(x.type==='SAFEHOUSE_CARE'&&(x.nodeId!=='nabat-contact'||!x.characterKey||x.woundsHealed<0||x.strainRecovered<0))
   throw Error('SWE02: cuidado médico inválido.');
  if(x.type==='TATOOINE_VISITORS_ALERT'&&(x.nodeId!=='nabat-contact'||x.arrivalLeadHours!==2))
   throw Error('SWE02: aviso do porto adulterado.');
 }
 const care=one('SAFEHOUSE_CARE');
 if(new Set(care.map(x=>x.characterKey)).size!==care.length)throw Error('SWE02: atendimento médico duplicado.');
 return true;
}
