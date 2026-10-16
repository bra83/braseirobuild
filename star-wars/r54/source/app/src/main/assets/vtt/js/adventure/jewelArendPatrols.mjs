/** SWE09 The Jewel of Yavin pp.90–91: residence Wing Guard patrols
 * every fifteen minutes outdoors; Venture alert and two flights of two cloud
 * cars arriving within ten minutes of hostilities. Time comes exclusively
 * from the persistent, task-receipt-backed world clock, never Date.now().
 */
import {diceFacesForTests} from '../rules/narrativeDice.mjs';
import {validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.90–91, Shen residence / Venture';
const PREFIX='JEWEL_AREND_PATROL_';
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const kinds=['RESIDENCE_START','RESIDENCE_CHECK','RESIDENCE_END','VENTURE_ALERT','VENTURE_ARRIVAL'];
const allowed={
 RESIDENCE_START:['gmOutdoorsConfirmed'],
 RESIDENCE_CHECK:['sequence','dueMinute','skillKey','check'],
 RESIDENCE_END:['gmLeftOutdoorsConfirmed'],
 VENTURE_ALERT:['gmCrewHostilitiesConfirmed','gmTransponderActivatedConfirmed','securityEncounterId'],
 VENTURE_ARRIVAL:['gmPhysicalArrivalConfirmed','gmFourDistinctVehiclesConfirmed','flights','vehiclesPerFlight']
};
const skills=['engano','sigilo','callejeo'];
function minutes(world){if(!world?.clock||typeof world.clock!=='object')throw Error('Relógio persistente de tarefas indisponível.');validateTaskTimeLedger(world);const n=world.clock.elapsedMinutes??0;if(!Number.isSafeInteger(n)||n<0)throw Error('Relógio persistente de tarefas inválido.');return n;}
function relevant(p){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind?.startsWith(PREFIX));}
function checkEvidence(check){
 if(!check||typeof check!=='object'||!Array.isArray(check.faces)||!check.pool||typeof check.success!=='boolean'||
  !Number.isSafeInteger(check.netSuccess)||!Number.isSafeInteger(check.netAdvantage)||
  !Number.isSafeInteger(check.triumph)||!Number.isSafeInteger(check.despair)||
  !Array.isArray(check.faces)||!check.faces.length)throw Error('Evidência dos dados da patrulha inválida.');
 const pool=check.pool;
 if(Number(pool.difficulty??0)+Number(pool.challenge??0)!==3||!Number.isSafeInteger(pool.ability)||!Number.isSafeInteger(pool.proficiency))
  throw Error('Patrulha exige teste Difícil real com três dados de Dificuldade.');
 const faces=check.faces;
 if(faces.some(x=>!['ability','proficiency','difficulty','challenge','boost','setback'].includes(x.type)||
   !Number.isSafeInteger(x.index)||x.index<0||!equal(x.face,diceFacesForTests[x.type]?.[x.index])))throw Error('Faces da patrulha inválidas.');
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback'])
  if(faces.filter(x=>x.type===type).length!==Number(pool[type]??0))throw Error('Pool e faces da patrulha divergentes.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const x of faces)for(const [k,v] of Object.entries(x.face)){
  if(!(k in raw)||!Number.isSafeInteger(v)||v<0||v>2)throw Error('Símbolo da patrulha inválido.');raw[k]+=v;
 }
 if(check.netSuccess!==raw.success+raw.triumph-raw.failure-raw.despair||
    check.netAdvantage!==raw.advantage-raw.threat||check.triumph!==raw.triumph||
    check.despair!==raw.despair||check.success!==(check.netSuccess>0))throw Error('Símbolos da patrulha não correspondem às faces.');
}
export function replayJewelArendPatrols(p){
 let residence=null,venture=null,lastMinute=-1;const ids=new Set();
 for(const [id,r] of relevant(p)){
  const type=r.kind.slice(PREFIX.length);
  if(p?.id!=='jewel-of-yavin'||!validId(id)||ids.has(id)||r.id!==id||r.version!==1||r.source!==SOURCE||
   !kinds.includes(type)||!Number.isSafeInteger(r.atMinute)||r.atMinute<0||
   Object.keys(r).some(k=>!['id','kind','version','source','atMinute',...allowed[type]].includes(k)))
   throw Error('Recibo temporal de Arend adulterado.');
  if(r.atMinute<lastMinute)throw Error('Recibos da patrulha fora da ordem do relógio.');
  lastMinute=r.atMinute;ids.add(id);
  const holding=p.operations?.arendRevenge?.foundAt;
  if(type==='RESIDENCE_START'){
   if(residence||holding!=='plaza-residence'||r.gmOutdoorsConfirmed!==true)throw Error('Exposição da residência inválida.');
   residence={startedAt:r.atMinute,endedAt:null,checks:[]};
  }else if(type==='RESIDENCE_CHECK'){
   if(!residence||residence.endedAt!==null||!skills.includes(r.skillKey)||
    r.sequence!==residence.checks.length+1||r.dueMinute!==residence.startedAt+15*r.sequence||
    r.atMinute<r.dueMinute||r.check?.difficulty!==undefined)throw Error('Teste de patrulha fora do intervalo real.');
   checkEvidence(r.check);residence.checks.push({id:r.id,sequence:r.sequence,dueMinute:r.dueMinute,atMinute:r.atMinute,
    skillKey:r.skillKey,check:clone(r.check)});
  }else if(type==='RESIDENCE_END'){
   if(!residence||residence.endedAt!==null||r.atMinute<residence.startedAt||r.gmLeftOutdoorsConfirmed!==true||
    residence.startedAt+15*(residence.checks.length+1)<=r.atMinute)throw Error('Patrulha devida não pode ser apagada saindo da rua.');
   residence.endedAt=r.atMinute;
  }else if(type==='VENTURE_ALERT'){
   if(venture||holding!=='plaza-yacht'||r.gmCrewHostilitiesConfirmed!==true||
    r.gmTransponderActivatedConfirmed!==true||r.securityEncounterId!=='arend-property-plaza-yacht-security'||
    !p.operations?.arendProperty?.stages?.some(s=>s.encounterId===r.securityEncounterId))
    throw Error('Alerta do Venture sem hostilidades reais da tripulação.');
   venture={alertAt:r.atMinute,arrival:null};
  }else if(type==='VENTURE_ARRIVAL'){
   if(!venture||venture.arrival||r.atMinute<venture.alertAt||r.atMinute>venture.alertAt+10||
    r.gmPhysicalArrivalConfirmed!==true||r.gmFourDistinctVehiclesConfirmed!==true||
    r.flights!==2||r.vehiclesPerFlight!==2)throw Error('Chegada dos cloud cars sem prazo ou presença comprovados.');
   venture.arrival={id:r.id,atMinute:r.atMinute,flights:2,vehiclesPerFlight:2};
  }
 }
 return {residence,venture};
}
export function validateJewelArendPatrols(p,world){
 const expected=replayJewelArendPatrols(p);
 if(!equal(expected,p?.operations?.arendPatrols??{residence:null,venture:null}))throw Error('Linha temporal das patrulhas diverge dos recibos.');
 if(world&&relevant(p).length){const now=minutes(world);
  for(const [,r] of relevant(p))if(r.atMinute>now)throw Error('Recibo de patrulha no futuro do relógio persistente.');
 }
 return expected;
}
export function jewelArendPatrolStatus(world){
 const p=world?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin')return null;
 const state=validateJewelArendPatrols(p,world),now=world?.clock?minutes(world):null,res=state.residence,ven=state.venture;
 const nextDue=res&&res.endedAt===null?res.startedAt+15*(res.checks.length+1):null;
 return {elapsedMinutes:now,residence:res?{...clone(res),nextDueMinute:nextDue,
  due:nextDue!==null&&now>=nextDue,minutesUntilDue:nextDue===null?null:Math.max(0,nextDue-now)}:null,
  venture:ven?{...clone(ven),deadlineMinute:ven.alertAt+10,
   minutesUntilDeadline:Math.max(0,ven.alertAt+10-now),
   status:ven.arrival?'ARRIVED':now>ven.alertAt+10?'OVERDUE_GM_CONFIRMATION':'EN_ROUTE'}:null};
}
export function recordJewelArendPatrol(world,details={}){
 const w=clone(world),p=w?.publishedAdventure,now=minutes(w);
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
  !p.operations?.arendRevenge?.foundAt)throw Error('Patrulhas exigem a busca oficial de Arend.');
 if(!validId(details.actionId)||p.operations.receipts?.[details.actionId])throw Error('Recibo de patrulha duplicado ou inválido.');
 const state=validateJewelArendPatrols(p,w),type=details.type;
 let extra={};
 if(type==='RESIDENCE_START'){
  if(p.operations.arendRevenge.foundAt!=='plaza-residence'||state.residence||details.gmOutdoorsConfirmed!==true)
   throw Error('Mestre deve confirmar que o grupo está na rua da residência.');
  extra={gmOutdoorsConfirmed:true};
 }else if(type==='RESIDENCE_END'){
  if(!state.residence||state.residence.endedAt!==null||details.gmLeftOutdoorsConfirmed!==true||
   now>=state.residence.startedAt+15*(state.residence.checks.length+1))
   throw Error('Resolva patrulhas já devidas antes de sair da rua.');
  extra={gmLeftOutdoorsConfirmed:true};
 }else if(type==='RESIDENCE_CHECK'){
  const res=state.residence,seq=(res?.checks.length??0)+1,due=res?.startedAt+15*seq;
  if(!res||res.endedAt!==null||now<due||!skills.includes(details.skillKey))throw Error('Patrulha ainda não chegou ou perícia não é oficial.');
  checkEvidence(details.check);extra={sequence:seq,dueMinute:due,skillKey:details.skillKey,check:clone(details.check)};
 }else if(type==='VENTURE_ALERT'){
  if(p.operations.arendRevenge.foundAt!=='plaza-yacht'||state.venture||
   details.gmCrewHostilitiesConfirmed!==true||details.gmTransponderActivatedConfirmed!==true||
   !p.operations.arendProperty?.stages?.some(x=>x.encounterId==='arend-property-plaza-yacht-security'))
   throw Error('Alerta do iate exige confronto real com a tripulação e transponder confirmado.');
  extra={gmCrewHostilitiesConfirmed:true,gmTransponderActivatedConfirmed:true,
   securityEncounterId:'arend-property-plaza-yacht-security'};
 }else if(type==='VENTURE_ARRIVAL'){
  if(!state.venture||state.venture.arrival||now<state.venture.alertAt||now>state.venture.alertAt+10||
   details.gmPhysicalArrivalConfirmed!==true||details.gmFourDistinctVehiclesConfirmed!==true)
   throw Error('Mestre deve confirmar chegada física de quatro veículos oficiais dentro de dez minutos.');
  extra={gmPhysicalArrivalConfirmed:true,gmFourDistinctVehiclesConfirmed:true,flights:2,vehiclesPerFlight:2};
 }else throw Error('Ação de patrulha desconhecida.');
 const r={id:details.actionId,kind:PREFIX+type,version:1,source:SOURCE,atMinute:now,...extra};
 p.operations.receipts[r.id]=clone(r);p.operations.arendPatrols=replayJewelArendPatrols(p);
 validateJewelArendPatrols(p,w);
 return {world:w,adjudication:r,status:jewelArendPatrolStatus(w)};
}
