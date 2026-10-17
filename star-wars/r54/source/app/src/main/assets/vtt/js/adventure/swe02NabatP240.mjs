// SWE02 printed pp.13–14: Nabat docking, market and militant safehouse.
// The story engine never mints currency or inventory. All mutations are
// computed here from catalog records and committed by GameController.
import {equipmentCatalog} from '../rules/equipmentCatalog.mjs';
import {healWounds,recoverStrain} from '../rules/health.mjs';
import {repairVehicleCritical,validateVehicle} from '../rules/vehicles.mjs';
const clone=v=>structuredClone(v);
export const NABAT_SOURCE=Object.freeze({productCode:'SWE02',printedPages:[13,14],authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const NABAT_P14_SOURCE=Object.freeze({productCode:'SWE02',printedPage:14,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const SCENES=new Set(['nabat-arrival','nabat-contact']);
const SAFE_DOCKING_ROUTES=Object.freeze({
 NO_INTERCEPTION:{fromNodeId:'rylos-arrival-decision',choiceId:'ship-safe-landing',flag:'shipThwheekSkipped'},
 THWHEEK_LANDED:{fromNodeId:'thwheek-engagement',choiceId:'ship-thwheek-to-nabat',flag:'shipThwheekLandedSafely'},
 CRASH_CREW_RESOLVED:{fromNodeId:'thwheek-crash',choiceId:'ship-crash-to-nabat',flag:'shipCrashCrewResolved'}
});
const hasSafeDocking=p=>Boolean(p?.flags?.nabatShipSafelyDocked&&p.nabatReceipts?.some(x=>x.type==='NABAT_SAFE_DOCKING'));
export function recordNabatSafeDocking(world,{fromNodeId,choiceId}={}){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||!p.flags?.shipBuraFound)
  throw Error('SWE02 p.13: B’ura deve estar a bordo e a nave precisa chegar a Nabat antes do atracamento.');
 if(p.currentNodeId!==fromNodeId)throw Error('SWE02 p.13: origem da chegada a Nabat inválida.');
 if(hasSafeDocking(p))throw Error('SWE02 p.13: atracamento seguro já registrado.');
 const route=Object.entries(SAFE_DOCKING_ROUTES).find(([,r])=>r.fromNodeId===fromNodeId&&r.choiceId===choiceId);
 if(!route||p.flags[route[1].flag]!==true)throw Error('SWE02 p.13: a rota de pouso não foi resolvida.');
 const [arrivalRoute]=route;
 p.nabatReceipts=[...(p.nabatReceipts||[]),{type:'NABAT_SAFE_DOCKING',nodeId:'nabat-arrival',fromNodeId,choiceId,
  arrivalRoute,shipKey:'yt1300',shipName:'Krayt Fang',buraArranged:true,
  source:{productCode:'SWE02',printedPage:13,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}}];
 p.flags={...(p.flags||{}),nabatShipSafelyDocked:true};
 return {world:w,receipt:clone(p.nabatReceipts.at(-1))};
}
function active(world,allowed=SCENES){
 const p=world?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||!allowed.has(p.currentNodeId))
  throw Error('SWE02: operação de Nabat fora da cena oficial.');
 if(SCENES.has(p.currentNodeId)&&!hasSafeDocking(p))throw Error('SWE02 p.13: B’ura ainda não providenciou o atracamento seguro em Nabat.');
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
 if(!p.flags?.nabatNynConversationClosed&&!(p.socialReceipts||[]).some(x=>['NYN_NEGOTIATION','NYN_REWARDS_DECLINED'].includes(x.type)))
  throw Error('SWE02 p.14: o aviso chega depois da conversa com Nyn.');
 if(p.flags.nabatSpaceportAlertDelivered)throw Error('SWE02: aviso já entregue.');
 p.flags={...p.flags,nabatSpaceportAlertDelivered:true};
 const r=receipt(p,'TATOOINE_VISITORS_ALERT',{visitors:['human','gand'],arrivalLeadHours:2});
 return {world:w,receipt:r};
}
export function recordNabatMorningResponse(world,character,{response}={}){
 const w=clone(world),p=active(w,new Set(['nabat-morning-departure'])),who=key(character);
 if(!who)throw Error('SWE02 p.14: identifique o PC que responde a Nyn.');
 if(!p.nabatReceipts?.some(x=>x.type==='NABAT_MORNING_BRIEFING'))throw Error('SWE02 p.14: a conversa da manhã ainda não foi apresentada.');
 if(!['same_hutt','different_problem','uncertain'].includes(response))throw Error('SWE02 p.14: resposta de história inválida.');
 if((p.nabatReceipts||[]).some(x=>x.type==='NABAT_MORNING_RESPONSE'))throw Error('SWE02 p.14: resposta à conversa da manhã já registrada.');
 const r={type:'NABAT_MORNING_RESPONSE',nodeId:p.currentNodeId,characterKey:who,response,source:NABAT_P14_SOURCE};
 p.nabatReceipts=[...(p.nabatReceipts||[]),r];p.flags={...(p.flags||{}),nabatMorningResponseRecorded:true};
 return {world:w,receipt:clone(r)};
}
export function nabatShipRepair(world,vehicles){
 const w=clone(world),v=clone(vehicles),p=active(w,new Set(['nabat-arrival']));
 if(!p.flags?.shipBuraFound)throw Error('SWE02: B’ura B’an não foi encontrado.');
 if(p.flags?.nabatHullRepaired)throw Error('SWE02: reparos da Krayt Fang já foram pagos pelos contatos.');
 if(v.combat?.active)throw Error('SWE02: não é possível reparar durante combate veicular.');
 const ship=v.vehicles?.[v.activeVehicleId];
 if(!ship||ship.key!=='yt1300'||String(ship.name||'').trim().toLowerCase()!=='krayt fang')
  throw Error('SWE02: selecione a Krayt Fang real; nenhuma nave é criada.');
 validateVehicle(ship);
 const before={hullDamage:ship.hullDamage,systemStrain:ship.systemStrain,
  criticalHits:ship.criticalHits.filter(x=>!x.repaired).map(x=>({number:x.number,key:x.key,title:x.title})),
  disabledComponents:[...(ship.disabledComponents||[])]};
 if(ship.criticalHits.some(x=>!x.repaired&&(x.key==='vaporized'||x.destroyed)))
  throw Error('SWE02: nave destruída não pode ser reparada pelos contatos.');
 for(let index=0;index<ship.criticalHits.length;index++)
  if(!ship.criticalHits[index].repaired) v.vehicles[v.activeVehicleId]=repairVehicleCritical(v.vehicles[v.activeVehicleId],index);
 const repairedShip=v.vehicles[v.activeVehicleId];
 repairedShip.hullDamage=0;repairedShip.systemStrain=0;repairedShip.disabledComponents=[];repairedShip.lastSystemStrainAt=null;
 p.flags={...p.flags,nabatHullRepaired:true};
 const r=receipt(p,'CONTACTS_PAID_SHIP_REPAIRS',{vehicleId:ship.id,repairer:'NABAT_CONTACTS',creditsSpent:0,
  hullBefore:before.hullDamage,hullAfter:repairedShip.hullDamage,
  systemStrainBefore:before.systemStrain,systemStrainAfter:repairedShip.systemStrain,
  criticalHitsRepaired:before.criticalHits,criticalHitCountAfter:repairedShip.criticalHits.filter(x=>!x.repaired).length,
  disabledComponentsBefore:before.disabledComponents,disabledComponentsAfter:[...repairedShip.disabledComponents]});
 return {world:w,vehicles:v,receipt:r};
}
export function nabatSafehouseCare(world,character,{characterKey=null}={}){
 const w=clone(world),p=active(w,new Set(['nabat-contact']));
 const who=String(characterKey??key(character)).trim();
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
 const docking=one('NABAT_SAFE_DOCKING');
 if(docking.length>1||one('SPACEPORT_WATCH_REQUESTED').length>1||one('TATOOINE_VISITORS_ALERT').length>1||one('CONTACTS_PAID_SHIP_REPAIRS').length>1)
  throw Error('SWE02: operação única de Nabat duplicada.');
 if(Boolean(f.nabatShipSafelyDocked)!==Boolean(docking.length)||
   (['nabat-arrival','nabat-contact'].includes(p.currentNodeId)&&!docking.length))
  throw Error('SWE02 p.13: cena de Nabat sem recibo de atracamento seguro.');
 if(docking.length){
  const x=docking[0],route=SAFE_DOCKING_ROUTES[x.arrivalRoute];
  if(x.nodeId!=='nabat-arrival'||x.shipKey!=='yt1300'||x.shipName!=='Krayt Fang'||x.buraArranged!==true||
    !route||x.fromNodeId!==route.fromNodeId||x.choiceId!==route.choiceId||f.shipBuraFound!==true||f[route.flag]!==true||
    x.source?.productCode!=='SWE02'||x.source?.printedPage!==13)
   throw Error('SWE02 p.13: recibo de atracamento seguro adulterado.');
 }
 if(Boolean(f.nabatSpaceportWatchRequested)!==Boolean(one('SPACEPORT_WATCH_REQUESTED').length)||
    Boolean(f.nabatSpaceportAlertDelivered)!==Boolean(one('TATOOINE_VISITORS_ALERT').length)||
    Boolean(f.nabatHullRepaired)!==Boolean(one('CONTACTS_PAID_SHIP_REPAIRS').length))
  throw Error('SWE02: sinalizador de Nabat sem recibo correspondente.');
 if(f.nabatSpaceportAlertDelivered&&!f.nabatSpaceportWatchRequested)throw Error('SWE02: alerta sem solicitação prévia.');
 const catalog=nabatMarketCatalog();
 for(const x of rr){
  const validP13=x.type==='NABAT_SAFE_DOCKING'?x.nodeId==='nabat-arrival'&&x.source?.printedPage===13:
   x.source?.printedPages?.[0]===13&&SCENES.has(x.nodeId);
  const validP14=x.source?.printedPage===14&&['nabat-morning-departure','lylek-den-ambush'].includes(x.nodeId);
  if(x.source?.productCode!=='SWE02'||(!validP13&&!validP14))
   throw Error('SWE02: recibo de Nabat sem proveniência.');
  if(x.type==='MARKET_PURCHASE'){
   const item=catalog.find(y=>y.id===x.itemId);
   if(!item||!Number.isSafeInteger(x.quantity)||x.quantity<1||x.quantity>10||x.unitPrice!==item.price||
     x.cost!==x.unitPrice*x.quantity||!Number.isSafeInteger(x.remainingCredits)||x.remainingCredits<0)
    throw Error('SWE02: preço ou compra adulterados.');
  }
  if(x.type==='CONTACTS_PAID_SHIP_REPAIRS'&&(!f.shipBuraFound||x.repairer!=='NABAT_CONTACTS'||x.creditsSpent!==0||
    x.hullBefore<0||x.hullAfter!==0||x.systemStrainBefore<0||x.systemStrainAfter!==0||
    !Array.isArray(x.criticalHitsRepaired)||x.criticalHitCountAfter!==0||
    x.criticalHitsRepaired.some(hit=>!Number.isSafeInteger(hit.number)||hit.number<1||typeof hit.key!=='string'||typeof hit.title!=='string')||
    !Array.isArray(x.disabledComponentsBefore)||x.disabledComponentsBefore.some(component=>typeof component!=='string')||
    !Array.isArray(x.disabledComponentsAfter)||x.disabledComponentsAfter.length!==0))
   throw Error('SWE02: recibo de reparos dos contatos inválido.');
  if(x.type==='SAFEHOUSE_CARE'&&(x.nodeId!=='nabat-contact'||!x.characterKey||x.woundsHealed<0||x.strainRecovered<0))
   throw Error('SWE02: cuidado médico inválido.');
  if(x.type==='TATOOINE_VISITORS_ALERT'&&(x.nodeId!=='nabat-contact'||x.arrivalLeadHours!==2))
   throw Error('SWE02: aviso do porto adulterado.');
  if(x.type==='NABAT_MORNING_BRIEFING'&&(x.nodeId!=='nabat-morning-departure'||x.serviceTunnel!==true||x.speederProgrammedForRyllMines!==true||x.buraTravelsWithParty!==true||!['HUMAN_AND_GAND_REPORTED','NO_PRIOR_VISITOR_REPORT'].includes(x.visitorReport)))
   throw Error('SWE02 p.14: briefing matinal inválido.');
  if(x.type==='NABAT_MORNING_RESPONSE'&&(x.nodeId!=='nabat-morning-departure'||!x.characterKey||!['same_hutt','different_problem','uncertain'].includes(x.response)||!one('NABAT_MORNING_BRIEFING').length))
   throw Error('SWE02 p.14: resposta matinal inválida.');
  if(x.type==='NABAT_MORNING_DEPARTURE'&&(x.nodeId!=='nabat-morning-departure'||!one('NABAT_MORNING_RESPONSE').length||x.serviceTunnel!==true||x.speederProgrammedForRyllMines!==true))
   throw Error('SWE02 p.14: saída matinal inválida.');
 }
 if(rr.filter(x=>x.type==='NABAT_MORNING_BRIEFING').length>1||rr.filter(x=>x.type==='NABAT_MORNING_RESPONSE').length>1||rr.filter(x=>x.type==='NABAT_MORNING_DEPARTURE').length>1)
  throw Error('SWE02 p.14: etapa matinal duplicada.');
 const response=one('NABAT_MORNING_RESPONSE'),departure=one('NABAT_MORNING_DEPARTURE');
 if(Boolean(f.nabatMorningResponseRecorded)!==Boolean(response.length)||Boolean(f.nabatMorningDepartureRecorded)!==Boolean(departure.length))
  throw Error('SWE02 p.14: indicador da manhã sem recibo.');
 const care=one('SAFEHOUSE_CARE');
 if(new Set(care.map(x=>x.characterKey)).size!==care.length)throw Error('SWE02: atendimento médico duplicado.');
 const briefing=one('NABAT_MORNING_BRIEFING');
 if(Boolean(f.nabatMorningBriefingReady)!==Boolean(briefing.length)||Boolean(f.nabatMorningResponseRecorded)!==Boolean(response.length)||Boolean(f.nabatMorningDepartureRecorded)!==Boolean(departure.length))
  throw Error('SWE02 p.14: etapa da manhã sem recibo correspondente.');
 if(briefing.length&&briefing[0].visitorReport!==(f.nabatSpaceportAlertDelivered?'HUMAN_AND_GAND_REPORTED':'NO_PRIOR_VISITOR_REPORT'))
  throw Error('SWE02 p.14: relato do espaçoporto não corresponde ao aviso solicitado.');
 return true;
}
