/** SWE09 The Jewel of Yavin, printed pp.90–91, "Sweet Revenge".
 * Arend's holdings are discoverable on an Average Computers/Streetwise check.
 * His location is NOT exposed by the mainframe: the GM may use the published
 * escalating d100 search (25/50/75/automatic) at four distinct holdings.
 * This does not teleport Arend, defeat him, or award unearned credits.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const integer=(v,a,b)=>Number.isSafeInteger(v)&&v>=a&&v<=b;
const validId=v=>typeof v==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(v)&&!['__proto__','constructor','prototype'].includes(v);
export const AREND_HOLDINGS=Object.freeze([
 Object.freeze({id:'plaza-residence',label:'Residência particular — Distrito da Praça',printedPage:90}),
 Object.freeze({id:'figg-hall-office',label:'Escritório próximo ao Figg Hall',printedPage:91}),
 Object.freeze({id:'port-town-building',label:'Imóvel comercial — Port Town',printedPage:91}),
 Object.freeze({id:'plaza-yacht',label:'Iate privado atracado no nível da Praça',printedPage:91})
]);
const SOURCE='SWE09 The Jewel of Yavin, Sweet Revenge, pp.90–91';
const FACE_TYPES=['ability','proficiency','difficulty','challenge','boost','setback'];
const LEAD_KEYS=['id','kind','contractVersion','source','actorId','skill','characteristic','characteristicValue','rank','pool','roll','gmMainframeAccessConfirmed','gmRetryAuthorized','discovered'];
const VISIT_KEYS=['id','kind','contractVersion','source','holdingId','searchIndex','d100','threshold','found','gmSearchConfirmed'];
function checkLead(r){
 if(!validId(r.id)||r.kind!=='JEWEL_AREND_LEADS'||r.contractVersion!==1||r.source!==SOURCE||!validId(r.actorId)||
  !['computadoras','callejeo'].includes(r.skill)||r.characteristic!==(r.skill==='computadoras'?'intelecto':'astucia')||
  !integer(r.characteristicValue,0,7)||!integer(r.rank,0,7)||r.gmMainframeAccessConfirmed!==true||typeof r.gmRetryAuthorized!=='boolean'||
  Object.keys(r).some(k=>!LEAD_KEYS.includes(k)))throw Error('Recibo de pesquisa de Arend adulterado.');
 const pool={...buildPositivePool(r.characteristicValue,r.rank),difficulty:2,challenge:0,boost:0,setback:0};
 if(!eq(pool,r.pool)||!eq(pool,r.roll?.pool)||!Array.isArray(r.roll?.faces))throw Error('Pool de pesquisa de Arend adulterado.');
 let i=0;const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const type of FACE_TYPES)for(let j=0;j<pool[type];j++){
  const f=r.roll.faces[i++],faces=diceFacesForTests[type];
  if(!f||f.type!==type||!integer(f.index,0,faces.length-1)||!eq(f.face,faces[f.index]))throw Error('Faces da pesquisa de Arend adulteradas.');
  for(const [key,n] of Object.entries(f.face))raw[key]+=n;
 }
 if(i!==r.roll.faces.length)throw Error('Dados extras na pesquisa de Arend.');
 const expected=resolveSymbols(raw);
 for(const k of ['success','netSuccess','netAdvantage','triumph','despair','raw'])if(!eq(expected[k],r.roll[k]))throw Error('Símbolos da pesquisa de Arend adulterados.');
 if(r.discovered!==expected.success)throw Error('Resultado da pesquisa de Arend adulterado.');
}
function checkVisit(r,index,seen){
 if(!validId(r.id)||r.kind!=='JEWEL_AREND_VISIT'||r.contractVersion!==1||r.source!==SOURCE||
  !AREND_HOLDINGS.some(x=>x.id===r.holdingId)||seen.has(r.holdingId)||r.searchIndex!==index||r.gmSearchConfirmed!==true||
  Object.keys(r).some(k=>!VISIT_KEYS.includes(k)))throw Error('Recibo de visita a Arend adulterado.');
 const threshold=index*25;
 if(r.threshold!==threshold||index<1||index>4||
  (index===4?(r.d100!==null||r.found!==true):(!integer(r.d100,1,100)||r.found!==(r.d100<=threshold))))
  throw Error('Rolagem d100 ou chance oficial de Arend adulterada.');
}
export function replayJewelArendRevenge(progress){
 const receipts=progress?.operations?.receipts??{};
 const checks=[],visits=[],seen=new Set();let leadsKnown=false,foundAt=null;
 for(const [id,r] of Object.entries(receipts)){
  if(!['JEWEL_AREND_LEADS','JEWEL_AREND_VISIT'].includes(r?.kind))continue;
  if(id!==r.id)throw Error('ID de pesquisa de Arend adulterado.');
  if(r.kind==='JEWEL_AREND_LEADS'){
   if(leadsKnown||visits.length||checks.length&&r.gmRetryAuthorized!==true)throw Error('Repetição indevida da pesquisa de Arend.');
   checkLead(r);checks.push(clone(r));leadsKnown=r.discovered;
  }else{
   if(!leadsKnown||foundAt||visits.length>=4)throw Error('Visita de Arend sem pista ou após localização.');
   checkVisit(r,visits.length+1,seen);seen.add(r.holdingId);visits.push(clone(r));
   if(r.found)foundAt=r.holdingId;
  }
 }
 return checks.length||visits.length?{contractVersion:1,checks,leadsKnown,visits,foundAt,remainingHoldings:AREND_HOLDINGS.filter(x=>!seen.has(x.id)).map(x=>x.id)}:null;
}
export function validateJewelArendRevenge(progress){
 const expected=replayJewelArendRevenge(progress),actual=progress?.operations?.arendRevenge??null;
 if(!eq(expected,actual))throw Error('Estado de pesquisa de Arend diverge dos recibos.');
 return expected;
}
function preflight(world,combat,actionId){
 const p=world?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!p.operations?.escapeBomb)
  throw Error('A pesquisa de Arend exige a traição revelada em SWE09.');
 if(combat?.active||p.activeEncounterId)throw Error('Encerre o combate antes de investigar Arend.');
 if(!p.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome))
  throw Error('Resolva primeiro o encontro com os quatro capangas de Arend.');
 if(!validId(actionId)||p.operations?.receipts?.[actionId])throw Error('ID de pesquisa de Arend inválido ou duplicado.');
 validateJewelArendRevenge(p);return p;
}
export function investigateJewelArendHoldings(world,combat,character,details={}){
 const w=clone(world),p=preflight(w,combat,details.actionId),prior=validateJewelArendRevenge(p);
 if(prior?.leadsKnown||prior?.visits.length)throw Error('Os quatro endereços já foram descobertos.');
 if(prior?.checks.length&&details.gmRetryAuthorized!==true)throw Error('Nova tentativa exige autorização expressa do Mestre.');
 if(details.gmMainframeAccessConfirmed!==true)throw Error('Mestre deve confirmar acesso ao computador da rede de Cloud City.');
 const skill=details.skill,characteristic=skill==='computadoras'?'intelecto':skill==='callejeo'?'astucia':null;
 const stat=character?.characteristics?.[characteristic],rank=character?.skills?.[skill]??0,actorId=String(character?.id??character?.name??'');
 if(!characteristic||!validId(actorId)||!integer(stat,0,7)||!integer(rank,0,7))throw Error('Ficha real de Computadores ou Conhecimento das Ruas inválida.');
 const pool={...buildPositivePool(stat,rank),difficulty:2,challenge:0,boost:0,setback:0};
 const roll=rollPool(pool,details.rng??Math.random);
 const r={id:details.actionId,kind:'JEWEL_AREND_LEADS',contractVersion:1,source:SOURCE,actorId,skill,characteristic,characteristicValue:stat,rank,pool,roll,
  gmMainframeAccessConfirmed:true,gmRetryAuthorized:details.gmRetryAuthorized===true,discovered:roll.success};
 p.operations.receipts[r.id]=clone(r);p.operations.arendRevenge=replayJewelArendRevenge(p);validateJewelArendRevenge(p);
 return {world:w,adjudication:r,investigation:clone(p.operations.arendRevenge)};
}
export function visitJewelArendHolding(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details.actionId),prior=validateJewelArendRevenge(p);
 if(!prior?.leadsKnown||prior.foundAt)throw Error('Descubra os endereços antes de procurar Arend, sem repetir um encontro resolvido.');
 if(!prior.remainingHoldings.includes(details.holdingId)||details.gmSearchConfirmed!==true)throw Error('Local ainda não disponível ou visita não confirmada pelo Mestre.');
 const index=prior.visits.length+1,threshold=index*25;
 // The fourth distinct holding contains Arend automatically, per SWE09.
 const d100=index===4?null:Math.floor((details.rng??Math.random)()*100)+1;
 if(index<4&&!integer(d100,1,100))throw Error('Rolagem d100 inválida.');
 const r={id:details.actionId,kind:'JEWEL_AREND_VISIT',contractVersion:1,source:SOURCE,holdingId:details.holdingId,
  searchIndex:index,d100,threshold,found:index===4||d100<=threshold,gmSearchConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.arendRevenge=replayJewelArendRevenge(p);validateJewelArendRevenge(p);
 return {world:w,adjudication:r,investigation:clone(p.operations.arendRevenge)};
}
