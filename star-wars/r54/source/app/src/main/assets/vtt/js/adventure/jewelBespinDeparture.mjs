import {successfulJewelStarportAccess,validateJewelStarportSecurity} from './jewelStarportSecurity.mjs';
import {replayJewelCustodyTransfers,validateJewelCustodyTransfers} from './jewelCustodyTransfers.mjs';
/** P224 — SWE09 The Jewel of Yavin, Flight over Fight pp.92–95.
 * Physical jewel custody, real ship boarding, real astrogation and world ticks.
 * The printed adventure does NOT prescribe a universal security/astrogation DC:
 * GM adjudicates security and the existing astrogation engine rolls the route.
 * No departure, item, flight or XP is inferred from the epilogue node alone.
 */
import {diceFacesForTests} from '../rules/narrativeDice.mjs';
import {validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
const clone=structuredClone;
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const holderId=c=>ID(c?.id)?c.id:'player-slot';
const SOURCE='SWE09 The Jewel of Yavin, pp.92–95, Flight over Fight / Episode III XP';
const PREFIX='JEWEL_BESPIN_';
export const JEWEL_PHYSICAL_ITEM_ID='swe09-jewel-of-yavin-gem';
const FIELDS={CUSTODY:['holderId','itemId','gmPhysicalJewelConfirmed','gmVaultAcquisitionConfirmed','atMinute','atTick'],
 BOARD:['holderId','itemId','shipId','port','gmBoardedConfirmed','gmSecurityResolvedConfirmed','carrierCompanionId','gmCompanionPresentConfirmed','gmCompanionBoardedConfirmed','accessMode','accessReceiptId','atMinute','atTick'],
 ESCAPE:['holderId','itemId','shipId','destinationLocationId','jumpProof','atMinute','atTick','gmDepartureConfirmed']};
const TYPES=Object.keys(FIELDS);
function clock(world){validateTaskTimeLedger(world);const m=world.clock.elapsedMinutes??0,t=world.clock.tick;
 if(!Number.isSafeInteger(m)||m<0||!Number.isSafeInteger(t)||t<0)throw Error('Relógio persistente inválido.');return {m,t};}
function physicalItem(character){return (character?.gear??[]).filter(x=>x?.id===JEWEL_PHYSICAL_ITEM_ID&&x.quantity===1);}
/** P228: a companion can physically carry the jewel on board; no passenger is inferred. */
function carrierForBoard(character,group,chain,details={}){
 const pc=holderId(character);
 if(chain.holderId===pc){
  if(details.carrierCompanionId!==undefined||details.gmCompanionPresentConfirmed!==undefined||details.gmCompanionBoardedConfirmed!==undefined)
   throw Error('Confirmações de companheiro não se aplicam à Joia com o PJ.');
  if(physicalItem(character).length!==1)return null;
  return {id:pc,companion:false};
 }
 const companion=(group?.companions??[]).find(x=>x?.id===chain.holderId);
 if(!companion?.character||!ID(companion.id)||physicalItem(companion.character).length!==1||
  details.carrierCompanionId!==companion.id||details.gmCompanionPresentConfirmed!==true||
  details.gmCompanionBoardedConfirmed!==true)throw Error('Portador companheiro deve existir, estar presente e embarcar fisicamente com a Joia.');
 return {id:companion.id,companion:true};
}
function verifyBoardCarrier(board,character,group){
 if(!board)return;
 const chainHolder=board.holderId;
 if(board.carrierCompanionId!==undefined){
  if(board.carrierCompanionId!==chainHolder||chainHolder===holderId(character)||
   board.gmCompanionPresentConfirmed!==true||board.gmCompanionBoardedConfirmed!==true||
   !(group?.companions??[]).some(x=>x?.id===chainHolder&&x.character&&physicalItem(x.character).length===1))
   throw Error('Companheiro portador não consta embarcado com a Joia física.');
 }else if(chainHolder!==holderId(character)||physicalItem(character).length!==1)
  throw Error('O PJ embarcado não possui a Joia física.');
}

function checkFaces(check){
 if(!check||!check.pool||!Array.isArray(check.faces)||!check.faces.length||check.success!==true||
 !Number.isSafeInteger(check.netSuccess)||check.netSuccess<=0||!Number.isSafeInteger(check.netAdvantage)||
 !Number.isSafeInteger(check.triumph)||!Number.isSafeInteger(check.despair))throw Error('Salto sem teste real de Astronavegação bem-sucedido.');
 const sums={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
  const n=check.pool[type]??0;
  if(!Number.isSafeInteger(n)||n<0||n>12||check.faces.filter(f=>f.type===type).length!==n)throw Error('Pool do salto adulterado.');
 }
 for(const f of check.faces){if(!Number.isSafeInteger(f.index)||f.index<0||!same(f.face,diceFacesForTests[f.type]?.[f.index]))throw Error('Faces do salto adulteradas.');
  for(const [k,v] of Object.entries(f.face)){if(!(k in sums)||!Number.isSafeInteger(v))throw Error('Símbolo do salto inválido.');sums[k]+=v;}
 }
 if(check.netSuccess!==sums.success+sums.triumph-sums.failure-sums.despair||check.netAdvantage!==sums.advantage-sums.threat||
 check.triumph!==sums.triumph||check.despair!==sums.despair)throw Error('Símbolos do salto não correspondem às faces.');
}
function isOffworld(world,id){const loc=world?.locations?.[id];return ID(id)&&loc&&
 !id.startsWith('joy.')&&!/bespin|cloud.city|cidade.das.nuvens/i.test(id+' '+loc.name);}
function jewelSource(p){return p?.flags?.jewelAcquired===true&&p.visitedNodeIds?.includes('vault-infiltration')&&
 p.checkHistory?.some(x=>x.nodeId==='vault-infiltration'&&x.success===true);}
function receipts(p){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind?.startsWith(PREFIX));}
export function replayJewelBespinDeparture(p){
 let custody=null,board=null,escape=null;let previousMinute=-1,previousTick=-1;
 for(const [id,r] of receipts(p)){
  const type=r.kind.slice(PREFIX.length);
  if(!ID(id)||r.id!==id||r.version!==1||r.source!==SOURCE||!TYPES.includes(type)||
   Object.keys(r).some(k=>!['id','kind','version','source',...FIELDS[type]].includes(k))||
   !Number.isSafeInteger(r.atMinute)||r.atMinute<previousMinute||!Number.isSafeInteger(r.atTick)||r.atTick<previousTick)
   throw Error('Recibo de fuga de Bespin adulterado ou fora de ordem.');
  previousMinute=r.atMinute;previousTick=r.atTick;
  if(type==='CUSTODY'){
   if(custody||board||escape||!jewelSource(p)||!ID(r.holderId)||r.itemId!==JEWEL_PHYSICAL_ITEM_ID||
    r.gmPhysicalJewelConfirmed!==true||r.gmVaultAcquisitionConfirmed!==true)
    throw Error('Posse da Joia não comprovada pelo cofre e pelo Mestre.');
   custody=clone(r);
  }else if(type==='BOARD'){
   if(!custody||board||escape||r.holderId!==replayJewelCustodyTransfers(p).holderId||r.itemId!==custody.itemId||
    !ID(r.shipId)||r.shipId.startsWith('jewel-venture-storm-iv-')||
    !['COMMERCIAL_STARPORT','PORT_TOWN','ARRANGED_PASSAGE'].includes(r.port)||
    r.gmBoardedConfirmed!==true||r.gmSecurityResolvedConfirmed!==true||
    (r.carrierCompanionId!==undefined&&(!ID(r.carrierCompanionId)||r.carrierCompanionId!==r.holderId||r.gmCompanionPresentConfirmed!==true||r.gmCompanionBoardedConfirmed!==true))||
    (r.carrierCompanionId===undefined&&(r.gmCompanionPresentConfirmed!==undefined||r.gmCompanionBoardedConfirmed!==undefined)))
    throw Error('Embarque sem nave física, Joia ou segurança resolvida.');
   if(r.accessMode!==undefined){
    const proof=successfulJewelStarportAccess(p,r.shipId);
    if(r.accessMode!=='BORROWED_SHIP'||!proof||r.accessReceiptId!==proof.id||proof.atMinute>r.atMinute)
      throw Error('Embarque em nave de terceiro sem Computadores Difícil realmente aprovado.');
   }else if(r.accessReceiptId!==undefined)throw Error('Recibo de acesso ao porto sem modo de embarque.');
   board=clone(r);
  }else{
   if(!board||escape||r.holderId!==board.holderId||r.itemId!==board.itemId||r.shipId!==board.shipId||
    !ID(r.destinationLocationId)||r.destinationLocationId.startsWith('joy.')||r.gmDepartureConfirmed!==true||
    r.atTick<=board.atTick||!r.jumpProof||r.jumpProof.vehicleId!==board.shipId||
    r.jumpProof.destinationLocationId!==r.destinationLocationId||r.jumpProof.arrived!==true||
    r.jumpProof.outcome?.jumpSuccessful!==true||r.jumpProof.outcome?.offCourse!==false||
    !Number.isSafeInteger(r.jumpProof.timeAdvancedTicks)||r.jumpProof.timeAdvancedTicks<1||
    r.atTick<board.atTick+r.jumpProof.timeAdvancedTicks||
    !String(r.jumpProof.originLocationId??'').startsWith('joy.'))throw Error('Fuga de Bespin sem salto hiperespacial real comprovado.');
   checkFaces(r.jumpProof.check);escape=clone(r);
  }
 }
 if((board&&!custody)||(escape&&!board))throw Error('Cadeia de custódia incompleta.');
 return {custody,board,escape};
}
export function validateJewelBespinDeparture(p,world=null,vehicles=null,character=null,group=null){
 const expected=replayJewelBespinDeparture(p);
 if(group&&character)validateJewelCustodyTransfers(p,world,character,group);
 if(expected.board&&!expected.escape&&character&&group)verifyBoardCarrier(expected.board,character,group);
 if(!same(expected,p?.operations?.bespinDeparture??{custody:null,board:null,escape:null}))throw Error('Estado da fuga diverge dos recibos.');
 if(Boolean(p?.flags?.bespinEscapeVerified)!==Boolean(expected.escape))throw Error('Flag de fuga não corresponde ao salto real.');
 if(world&&receipts(p).length){const {m,t}=clock(world);for(const [,r] of receipts(p))if(r.atMinute>m||r.atTick>t)throw Error('Fuga registrada no futuro do relógio.');
  if(expected.escape&&!isOffworld(world,expected.escape.destinationLocationId))throw Error('Destino da fuga não existe fora de Bespin.');}
 if(vehicles&&expected.board){const ship=vehicles.vehicles?.[expected.board.shipId];
  if(!ship||!ship.hyperdrive)throw Error('Nave de fuga removida ou sem hiperpropulsor.');}
 if(character&&expected.custody&&!expected.escape&&!group&&holderId(character)===replayJewelCustodyTransfers(p).holderId&&physicalItem(character).length!==1)
  throw Error('Joia física desapareceu do inventário antes da fuga.');
 return expected;
}
function base(world,combat,vehicles,character,actionId){const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
  combat?.active||p.activeEncounterId||vehicles?.combat?.active||!ID(actionId)||p.operations?.receipts?.[actionId]||
  !ID(holderId(character)))throw Error('Ação de fuga exige PJ real, perseguição ativa, ID único e ausência de combate.');
 validateJewelBespinDeparture(p,w,vehicles,character);return {w,p,stamp:clock(w)};}
function finish(w,p,r){p.operations.receipts[r.id]=clone(r);p.operations.bespinDeparture=replayJewelBespinDeparture(p);
 if(r.kind===PREFIX+'ESCAPE')p.flags={...(p.flags??{}),bespinEscapeVerified:true};
 validateJewelBespinDeparture(p,w);return {world:w,adjudication:clone(r),departure:clone(p.operations.bespinDeparture)};}
export function registerJewelCustody(world,combat,vehicles,character,details={}){
 const {w,p,stamp}=base(world,combat,vehicles,character,details.actionId);
 if(replayJewelBespinDeparture(p).custody||!jewelSource(p)||details.gmPhysicalJewelConfirmed!==true||
 details.gmVaultAcquisitionConfirmed!==true||physicalItem(character).length)throw Error('Joia deve ser obtida no cofre e confirmada fisicamente uma única vez.');
 const updated=clone(character);updated.gear??=[];
 updated.gear.push({id:JEWEL_PHYSICAL_ITEM_ID,name:'Joia de Yavin',quantity:1,kind:'story',source:'SWE09 — cofre central'});
 const r={id:details.actionId,kind:PREFIX+'CUSTODY',version:1,source:SOURCE,holderId:holderId(character),itemId:JEWEL_PHYSICAL_ITEM_ID,
 gmPhysicalJewelConfirmed:true,gmVaultAcquisitionConfirmed:true,atMinute:stamp.m,atTick:stamp.t};
 const out=finish(w,p,r);return {...out,character:updated};
}
export function registerJewelBoarding(world,combat,vehicles,character,details={},group=null){
 const {w,p,stamp}=base(world,combat,vehicles,character,details.actionId),state=replayJewelBespinDeparture(p);
 const ship=vehicles.vehicles?.[vehicles.activeVehicleId];
 if(group)validateJewelCustodyTransfers(p,w,character,group);
 const carrier=state.custody&&!state.board?carrierForBoard(character,group,replayJewelCustodyTransfers(p),details):null;
 const borrowed=details.accessMode==='BORROWED_SHIP';
 const access=borrowed?successfulJewelStarportAccess(p,ship?.id):null;
 if(details.accessMode!==undefined&&!borrowed)throw Error('Modo de acesso à nave não reconhecido.');
 if(borrowed&&(!access||details.accessReceiptId!==access.id))throw Error('Nave de terceiro exige Computadores Difícil aprovado e recibo real.');
 if(!state.custody||state.board||!carrier||
 !ship||ship.id!==vehicles.activeVehicleId||!ship.hyperdrive||ship.disabledComponents?.includes('hyperdrive')||
 details.gmBoardedConfirmed!==true||details.gmSecurityResolvedConfirmed!==true||
 !['COMMERCIAL_STARPORT','PORT_TOWN','ARRANGED_PASSAGE'].includes(details.port))
 throw Error('Embarque exige Joia no inventário, nave hiperespacial real, presença e segurança resolvida pelo Mestre.');
 const r={id:details.actionId,kind:PREFIX+'BOARD',version:1,source:SOURCE,holderId:carrier.id,itemId:JEWEL_PHYSICAL_ITEM_ID,
 shipId:ship.id,port:details.port,gmBoardedConfirmed:true,gmSecurityResolvedConfirmed:true,
 ...(borrowed?{accessMode:'BORROWED_SHIP',accessReceiptId:access.id}:{}),
 ...(carrier.companion?{carrierCompanionId:carrier.id,gmCompanionPresentConfirmed:true,gmCompanionBoardedConfirmed:true}:{}),
 atMinute:stamp.m,atTick:stamp.t};
 const out=finish(w,p,r);if(group)validateJewelBespinDeparture(p,w,vehicles,character,group);return out;
}
export function prepareJewelEscape(world,combat,vehicles,character,details={},group=null){
 const {w,p}=base(world,combat,vehicles,character,details.actionId),state=replayJewelBespinDeparture(p);
 if(!state.board||state.escape||
 vehicles.activeVehicleId!==state.board.shipId||!isOffworld(w,details.destinationLocationId)||
 details.gmDepartureConfirmed!==true||details.gmNavigationConfirmed!==true)
 throw Error('Fuga exige embarque comprovado, nave ativa, Joia física e destino real fora de Bespin.');
 verifyBoardCarrier(state.board,character,group);
 return {board:state.board,destinationLocationId:details.destinationLocationId};
}
export function finalizeJewelEscape(world,combat,vehicles,character,details,jump,group=null){
 const {w,p,stamp}=base(world,combat,vehicles,character,details.actionId);
 const state=replayJewelBespinDeparture(p);
 if(!state.board||state.escape||
 !isOffworld(w,details.destinationLocationId)||vehicles.activeVehicleId!==state.board.shipId||
 details.gmDepartureConfirmed!==true||details.gmNavigationConfirmed!==true||
 jump?.vehicleId!==state.board.shipId||jump?.destinationLocationId!==details.destinationLocationId||
 jump?.arrived!==true||jump?.outcome?.jumpSuccessful!==true||jump?.outcome?.offCourse!==false||
 !Number.isSafeInteger(jump.timeAdvancedTicks)||jump.timeAdvancedTicks<1||stamp.t<state.board.atTick+jump.timeAdvancedTicks)
 throw Error('Salto real ainda não concluiu a fuga de Bespin.');
 verifyBoardCarrier(state.board,character,group);checkFaces(jump.check);
 const r={id:details.actionId,kind:PREFIX+'ESCAPE',version:1,source:SOURCE,holderId:state.board.holderId,itemId:JEWEL_PHYSICAL_ITEM_ID,
 shipId:state.board.shipId,destinationLocationId:details.destinationLocationId,jumpProof:clone(jump),atMinute:stamp.m,atTick:stamp.t,gmDepartureConfirmed:true};
 return finish(w,p,r);
}
