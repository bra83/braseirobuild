/** SWE09 The Jewel of Yavin pp.90–91, Sweet Revenge: every actual change
 * of location during the search calls for a Hard Deception/Stealth/Streetwise
 * check. A failure draws one pursuer faction, two Threat draw two, and a
 * Despair draws all three. The check does NOT materialize anyone: the GM
 * confirms each faction's physical arrival, personnel and range separately.
 * Residence 15-minute Wing Guard patrols are a DIFFERENT published trigger.
 */
import {validateJewelArendRevenge} from './jewelArendRevenge.mjs';
import {diceFacesForTests,buildPositivePool} from '../rules/narrativeDice.mjs';
const copy=structuredClone,EQ=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SOURCE='SWE09 The Jewel of Yavin, pp.90–91, Sweet Revenge, location-to-location pursuit';
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const RANGES=['Engajado','Curto','Médio','Longo','Extremo'];
const SKILLS=Object.freeze({engano:'astucia',sigilo:'agilidad',callejeo:'astucia'});
export const PURSUER_FACTIONS=Object.freeze({
 wing_guard:Object.freeze({profileKey:'bespin_wing_guard_officer',maxCount:8,label:'Guarda Alada de Bespin'}),
 grayson_security:Object.freeze({profileKey:'grayson_private_security_guard',maxCount:6,label:'Segurança particular de Grayson'}),
 arend_goons:Object.freeze({profileKey:'shens_goons',maxCount:4,label:'Capangas de Arend Shen'})
});
const CK='JEWEL_PURSUIT_TRAVEL_CHECK',ARR='JEWEL_PURSUIT_GROUP_ARRIVAL';
const CKEYS=['id','kind','version','source','visitId','holdingId','searchIndex','atMinute','actorId','skillKey','characteristic','characteristicValue','rank','pool','faces','netSuccess','netAdvantage','triumph','despair','success','slots','gmNewLocationConfirmed'];
const AKEYS=['id','kind','version','source','checkId','visitId','slot','encounterId','faction','profileKey','count','range','atMinute','gmPhysicalArrivalConfirmed','gmDistinctPersonnelConfirmed','gmFactionChosenConfirmed'];
const validNumber=(n,min,max)=>Number.isSafeInteger(n)&&n>=min&&n<=max;
const receiptEntries=p=>Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind===CK||r?.kind===ARR);
export const jewelPursuitEncounterId=(searchIndex,slot)=>`jewel-pursuit-travel-${searchIndex}-${slot}`;
function proof(r){
 const pos=buildPositivePool(r.characteristicValue,r.rank);
 if(!validNumber(r.characteristicValue,0,7)||!validNumber(r.rank,0,7)||!EQ(pos,{ability:r.pool?.ability,proficiency:r.pool?.proficiency})||
  !validNumber(r.pool?.difficulty,0,5)||!validNumber(r.pool?.challenge,0,5)||r.pool.difficulty+r.pool.challenge!==3||
  !validNumber(r.pool?.boost,0,8)||!validNumber(r.pool?.setback,0,8)||!Array.isArray(r.faces))throw Error('Pool real da perseguição adulterado.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let i=0;
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
  for(let j=0;j<r.pool[type];j++){
   const face=r.faces[i++],faces=diceFacesForTests[type];
   if(!face||face.type!==type||!validNumber(face.index,0,faces.length-1)||!EQ(face.face,faces[face.index]))throw Error('Faces reais da perseguição adulteradas.');
   for(const [k,n] of Object.entries(face.face))raw[k]+=n;
  }
 }
 if(i!==r.faces.length)throw Error('Dados adicionais falsificados na perseguição.');
 const success=raw.success+raw.triumph-raw.failure-raw.despair;
 const advantage=raw.advantage-raw.threat;
 const slots=success>0?0:raw.despair>0?3:advantage<=-2?2:1;
 if(r.netSuccess!==success||r.netAdvantage!==advantage||r.triumph!==raw.triumph||r.despair!==raw.despair||
  r.success!==(success>0)||r.slots!==slots)throw Error('Símbolos/quantidade de grupos divergentes dos dados.');
}
export function replayJewelPursuitTravel(p){
 const visits=validateJewelArendRevenge(p)?.visits??[];
 const checks=[],arrivals=[],seenVisits=new Set(),byCheck=new Map(),factions=new Map();let lastMinute=-1;
 for(const [id,r] of receiptEntries(p)){
  if(p?.id!=='jewel-of-yavin'||!ID(id)||r.id!==id||r.version!==1||r.source!==SOURCE||
   !validNumber(r.atMinute,0,Number.MAX_SAFE_INTEGER)||r.atMinute<lastMinute)throw Error('Recibo de perseguição inválido ou fora da cronologia.');
  lastMinute=r.atMinute;
  if(r.kind===CK){
   const visit=visits.find(x=>x.id===r.visitId);
   if(Object.keys(r).some(k=>!CKEYS.includes(k))||!visit||visit.holdingId!==r.holdingId||visit.searchIndex!==r.searchIndex||
    seenVisits.has(r.visitId)||!ID(r.actorId)||!Object.hasOwn(SKILLS,r.skillKey)||r.characteristic!==SKILLS[r.skillKey]||
    r.gmNewLocationConfirmed!==true)throw Error('Teste de deslocamento sem visita física oficial.');
   proof(r);seenVisits.add(r.visitId);byCheck.set(r.id,r);checks.push(copy(r));factions.set(r.id,new Set());
  }else{
   const check=byCheck.get(r.checkId),spec=PURSUER_FACTIONS[r.faction];
   const already=arrivals.filter(x=>x.checkId===r.checkId);
   if(Object.keys(r).some(k=>!AKEYS.includes(k))||!check||!spec||check.slots===0||
    r.visitId!==check.visitId||r.slot!==already.length+1||r.slot>check.slots||
    r.encounterId!==jewelPursuitEncounterId(check.searchIndex,r.slot)||r.profileKey!==spec.profileKey||
    !validNumber(r.count,1,spec.maxCount)||!RANGES.includes(r.range)||r.atMinute<check.atMinute||
    factions.get(r.checkId).has(r.faction)||r.gmPhysicalArrivalConfirmed!==true||
    r.gmDistinctPersonnelConfirmed!==true||r.gmFactionChosenConfirmed!==true)
    throw Error('Facção perseguidora sem falha real, presença ou indivíduos distintos.');
   factions.get(r.checkId).add(r.faction);arrivals.push(copy(r));
  }
 }
 for(const r of arrivals){
  const history=(p.encounterHistory??[]).filter(x=>x.encounterId===r.encounterId);
  if(history.length>1||history.some(x=>!['adversaries_defeated','escaped','negotiated','ended','player_incapacitated'].includes(x.outcome)))
   throw Error('Histórico de combate da perseguição inconsistente.');
 }
 return {checks,arrivals};
}
export function validateJewelPursuitTravel(p,world){
 const expected=replayJewelPursuitTravel(p);
 if(!EQ(expected,p?.operations?.pursuitTravel??{checks:[],arrivals:[]}))throw Error('Estado da perseguição difere dos recibos.');
 if(world?.clock&&expected.checks.concat(expected.arrivals).some(x=>x.atMinute>(world.clock.elapsedMinutes??0)))
  throw Error('Perseguição registrada no futuro do relógio real.');
 return expected;
}
export function jewelPursuitTravelStatus(world){
 const p=world?.publishedAdventure;if(p?.id!=='jewel-of-yavin')return null;
 const state=validateJewelPursuitTravel(p,world),visits=validateJewelArendRevenge(p)?.visits??[];
 const lastVisit=visits.at(-1)||null,check=state.checks.find(x=>x.visitId===lastVisit?.id)||null;
 const arrivals=check?state.arrivals.filter(x=>x.checkId===check.id):[];
 return {...state,lastVisit:copy(lastVisit),lastCheck:copy(check),nextSlot:check&&!check.success&&arrivals.length<check.slots?arrivals.length+1:null};
}
function preflight(w,combat,actionId){
 const p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!p.operations?.arendRevenge?.leadsKnown)
  throw Error('Deslocamento perseguidor só na busca publicada por Arend.');
 if(combat?.active||p.activeEncounterId)throw Error('Resolva o combate anterior.');
 if(!ID(actionId)||p.operations?.receipts?.[actionId])throw Error('ID de ação inválido ou duplicado.');
 validateJewelPursuitTravel(p,w);return p;
}
export function recordJewelPursuitTravelCheck(world,combat,character,details={}){
 const w=copy(world),p=preflight(w,combat,details.actionId),status=jewelPursuitTravelStatus(w),visit=status.lastVisit;
 if(!visit||status.lastCheck||visit.id!==details.visitId||details.gmNewLocationConfirmed!==true)
  throw Error('Apenas uma rolagem por deslocamento/visita física confirmada.');
 if(!Object.hasOwn(SKILLS,details.skillKey))throw Error('Perícia de evasão não publicada.');
 const characteristic=SKILLS[details.skillKey],stat=character?.characteristics?.[characteristic],rank=character?.skills?.[details.skillKey]??0;
 const actorId=String(character?.id??character?.name??'');
 if(!ID(actorId)||!validNumber(stat,0,7)||!validNumber(rank,0,7))throw Error('Ficha real do PJ inválida.');
 const check=details.check;
 if(!check||!check.pool||!Array.isArray(check.faces))throw Error('Rolagem real ausente.');
 const atMinute=w.clock?.elapsedMinutes??0;
 if(!validNumber(atMinute,0,Number.MAX_SAFE_INTEGER))throw Error('Relógio persistente ausente.');
 const slots=check.success?0:check.despair>0?3:check.netAdvantage<=-2?2:1;
 const r={id:details.actionId,kind:CK,version:1,source:SOURCE,visitId:visit.id,holdingId:visit.holdingId,
  searchIndex:visit.searchIndex,atMinute,actorId,skillKey:details.skillKey,characteristic,
  characteristicValue:stat,rank,pool:copy(check.pool),faces:copy(check.faces),
  netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair,
  success:check.success,slots,gmNewLocationConfirmed:true};
 proof(r);p.operations.receipts[r.id]=copy(r);p.operations.pursuitTravel=replayJewelPursuitTravel(p);
 validateJewelPursuitTravel(p,w);
 return {world:w,adjudication:r,status:jewelPursuitTravelStatus(w)};
}
export function prepareJewelPursuitGroup(world,combat,details={}){
 const w=copy(world),p=preflight(w,combat,details.actionId),status=jewelPursuitTravelStatus(w),check=status.lastCheck;
 if(!check||check.success||status.nextSlot===null||details.checkId!==check.id||details.slot!==status.nextSlot)
  throw Error('Não existe grupo perseguindo neste deslocamento.');
 const previous=status.arrivals.at(-1);
 if(previous&&!p.encounterHistory?.some(x=>x.encounterId===previous.encounterId&&x.outcome))
  throw Error('Resolva o encontro anterior antes da próxima facção.');
 const spec=PURSUER_FACTIONS[details.faction];
 if(!spec||status.arrivals.some(x=>x.checkId===check.id&&x.faction===details.faction)||
  !validNumber(details.count,1,spec.maxCount)||!RANGES.includes(details.range)||
  details.gmPhysicalArrivalConfirmed!==true||details.gmDistinctPersonnelConfirmed!==true||details.gmFactionChosenConfirmed!==true)
  throw Error('Mestre deve confirmar facção, quantidade, distância e indivíduos fisicamente distintos.');
 const now=w.clock?.elapsedMinutes??0;
 if(!validNumber(now,check.atMinute,Number.MAX_SAFE_INTEGER))throw Error('Relógio real não permite chegada.');
 const r={id:details.actionId,kind:ARR,version:1,source:SOURCE,checkId:check.id,visitId:check.visitId,
  slot:details.slot,encounterId:jewelPursuitEncounterId(check.searchIndex,details.slot),faction:details.faction,
  profileKey:spec.profileKey,count:details.count,range:details.range,atMinute:now,
  gmPhysicalArrivalConfirmed:true,gmDistinctPersonnelConfirmed:true,gmFactionChosenConfirmed:true};
 p.operations.receipts[r.id]=copy(r);p.operations.pursuitTravel=replayJewelPursuitTravel(p);
 validateJewelPursuitTravel(p,w);
 return {world:w,adjudication:r,encounterId:r.encounterId};
}
export function jewelPursuitGroupEncounter(p,id){
 const r=validateJewelPursuitTravel(p).arrivals.find(x=>x.encounterId===id);
 if(!r||p.encounterHistory?.some(x=>x.encounterId===id&&x.outcome))return null;
 return {id:r.encounterId,label:`${PURSUER_FACTIONS[r.faction].label} — deslocamento ${r.visitId}, grupo ${r.slot}`,
  available:true,status:p.activeEncounterId===id?'active':'ready',playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
  adversaries:[{profileKey:r.profileKey,count:r.count,range:r.range,name:PURSUER_FACTIONS[r.faction].label}]};
}
