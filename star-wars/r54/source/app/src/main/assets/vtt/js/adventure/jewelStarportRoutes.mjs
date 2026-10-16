/** SWE09 The Jewel of Yavin p.94, After the Battle.
 * Real route choices and conditional +10 Criminal Obligation for actual
 * destruction of part of the crowded commercial starport. No phantom
 * guards, simulated damage, forced escape or automatic bounty.
 */
const clone=structuredClone,same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SOURCE='SWE09 The Jewel of Yavin, p.94, Flight over Fight, After the Battle';
const ROUTES=['MAINTENANCE','SEWERS','DEEPER_TO_SHIP','BLAST_OUT'];
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const K1=['id','kind','version','source','route','atMinute','gmAtStarportConfirmed','gmRoutePhysicallyAccessibleConfirmed','gmSecurityPositionConfirmed'];
const K2=['id','kind','version','source','routeReceiptId','combatEncounterId','obligationEntryId','characterId','obligation','atMinute','gmPhysicalDestructionConfirmed','gmDestructionDuringCombatConfirmed'];
function entries(p){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>['JEWEL_STARPORT_ROUTE','JEWEL_STARPORT_DESTRUCTION'].includes(r?.kind));}
export function replayJewelStarportRoutes(p){
 let route=null,destruction=null,last=-1;
 for(const [id,r] of entries(p)){
  if(p?.id!=='jewel-of-yavin'||!ID(id)||r.id!==id||r.version!==1||r.source!==SOURCE||
     !Number.isSafeInteger(r.atMinute)||r.atMinute<last)throw Error('Recibo de rota do porto inválido ou fora de ordem.');
  last=r.atMinute;
  if(r.kind==='JEWEL_STARPORT_ROUTE'){
   if(route||destruction||Object.keys(r).some(k=>!K1.includes(k))||!ROUTES.includes(r.route)||
      r.gmAtStarportConfirmed!==true||r.gmRoutePhysicallyAccessibleConfirmed!==true||r.gmSecurityPositionConfirmed!==true)
    throw Error('Rota física do porto sem confirmação ou duplicada.');
   route=clone(r);
  }else{
   if(!route||destruction||route.route!=='BLAST_OUT'||r.routeReceiptId!==route.id||
      Object.keys(r).some(k=>!K2.includes(k))||!ID(r.combatEncounterId)||!ID(r.obligationEntryId)||
      !ID(r.characterId)||r.obligation!==10||r.gmPhysicalDestructionConfirmed!==true||
      r.gmDestructionDuringCombatConfirmed!==true||
      !p.encounterHistory?.some(e=>e.encounterId===r.combatEncounterId&&e.outcome&&e.outcome!=='player_incapacitated'))
    throw Error('Destruição do porto exige combate publicado realmente encerrado e dano confirmado pelo Mestre.');
   destruction=clone(r);
  }
 }
 return {route,destruction};
}
export function validateJewelStarportRoutes(p,world=null){
 const actual=replayJewelStarportRoutes(p);
 if(!same(actual,p?.operations?.starportRoutes??{route:null,destruction:null}))throw Error('Rotas do porto divergem dos recibos.');
 if(world?.clock&&entries(p).some(([,r])=>r.atMinute>world.clock.elapsedMinutes))throw Error('Rota do porto registrada no futuro.');
 return actual;
}
function base(world,combat,details){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
    combat?.active||p.activeEncounterId||!ID(details.actionId)||p.operations?.receipts?.[details.actionId])
  throw Error('Rota do porto exige fuga ativa, ID único e combate encerrado.');
 const minute=w.clock?.elapsedMinutes??0;
 if(!Number.isSafeInteger(minute)||minute<0)throw Error('Relógio persistido do porto inválido.');
 validateJewelStarportRoutes(p,w);return {w,p,minute};
}
export function confirmJewelStarportRoute(world,combat,details={}){
 const {w,p,minute}=base(world,combat,details);
 if(validateJewelStarportRoutes(p).route||!ROUTES.includes(details.route)||
    details.gmAtStarportConfirmed!==true||details.gmRoutePhysicallyAccessibleConfirmed!==true||
    details.gmSecurityPositionConfirmed!==true)throw Error('Mestre deve confirmar localização, rota física e posição da segurança.');
 const r={id:details.actionId,kind:'JEWEL_STARPORT_ROUTE',version:1,source:SOURCE,route:details.route,atMinute:minute,
  gmAtStarportConfirmed:true,gmRoutePhysicallyAccessibleConfirmed:true,gmSecurityPositionConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.starportRoutes=replayJewelStarportRoutes(p);
 validateJewelStarportRoutes(p,w);return {world:w,adjudication:r,routes:clone(p.operations.starportRoutes)};
}
export function registerJewelStarportDestruction(world,group,combat,character,details={}){
 const {w,p,minute}=base(world,combat,details),current=validateJewelStarportRoutes(p);
 if(!current.route||current.route.route!=='BLAST_OUT'||current.destruction||
    details.routeReceiptId!==current.route.id||!ID(details.combatEncounterId)||
    !p.encounterHistory?.some(e=>e.encounterId===details.combatEncounterId&&e.outcome&&e.outcome!=='player_incapacitated')||
    details.gmPhysicalDestructionConfirmed!==true||details.gmDestructionDuringCombatConfirmed!==true)
  throw Error('Apenas destruição física comprovada após combate real autoriza Obrigação Criminal.');
 const characterId=character?.id??character?.name;
 if(!ID(characterId)||!Array.isArray(group?.obligationEntries))throw Error('PJ real e livro de Obrigação exigidos.');
 const g=clone(group),entryId='jewel_starport_'+details.actionId;
 if(g.obligationEntries.some(x=>x.id===entryId))throw Error('Obrigação já lançada.');
 // Legacy character obligations remain represented when group entries are
 // first initialized, avoiding silent loss of the pre-existing total.
 if(!g.obligationEntries.length&&Array.isArray(character.obligations))
  g.obligationEntries=character.obligations.map((e,i)=>({id:`jewel_starport_legacy_${details.actionId}_${i}`,
   member:String(e.member??character.name??characterId),type:String(e.type??'Obrigação'),
   description:String(e.description??''),value:Number(e.value??e.startingValue??0)}));
 if(g.obligationEntries.some(x=>!Number.isSafeInteger(x.value)||x.value<0||x.value>1000))
  throw Error('Livro legado de Obrigação inválido.');
 g.obligationEntries.push({id:entryId,member:String(character.name??characterId),type:'Criminal',value:10,
  description:'Destruição física do porto comercial de Cloud City — SWE09 p.94'});
 const r={id:details.actionId,kind:'JEWEL_STARPORT_DESTRUCTION',version:1,source:SOURCE,routeReceiptId:current.route.id,
  combatEncounterId:details.combatEncounterId,obligationEntryId:entryId,characterId,obligation:10,atMinute:minute,
  gmPhysicalDestructionConfirmed:true,gmDestructionDuringCombatConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.starportRoutes=replayJewelStarportRoutes(p);
 validateJewelStarportRoutes(p,w);return {world:w,group:g,adjudication:r,routes:clone(p.operations.starportRoutes)};
}
