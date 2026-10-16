/** SWE09 The Jewel of Yavin, pp.90–91: a failed Hard check while exposed
 * near Shen's residence can bring pursuers. This module deploys ONLY a
 * physically confirmed Wing Guard group. Other pursuer factions, if any,
 * remain for explicit GM adjudication; there is no phantom reinforcement.
 */
import {validateJewelArendPatrols} from './jewelArendPatrols.mjs';
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.90–91, Shen residence pursuit';
const KIND='JEWEL_RESIDENCE_WING_RESPONSE';
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','prototype','constructor'].includes(x);
const EQ=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const RANGES=['Engajado','Curto','Médio','Longo','Extremo'];
const KEYS=['id','kind','version','source','checkId','sequence','encounterId','profileKey','count','range','atMinute','gmPhysicalArrivalConfirmed','gmDistinctOfficersConfirmed','gmWingGuardChosenConfirmed'];
export const residenceWingEncounterId=sequence=>`jewel-residence-wing-${sequence}`;
function checkFor(p,id){return validateJewelArendPatrols(p).residence?.checks.find(x=>x.id===id);}
function records(p){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind===KIND);}
export function replayJewelResidenceWingResponse(p){
 const out=[];const seen=new Set();
 for(const [id,r] of records(p)){
  const check=checkFor(p,r.checkId);
  if(p?.id!=='jewel-of-yavin'||!ID(id)||r.id!==id||r.kind!==KIND||r.version!==1||r.source!==SOURCE||
    Object.keys(r).some(k=>!KEYS.includes(k))||!check||check.check.success||seen.has(r.checkId)||
    r.sequence!==check.sequence||r.encounterId!==residenceWingEncounterId(check.sequence)||
    r.profileKey!=='bespin_wing_guard_officer'||!Number.isSafeInteger(r.count)||r.count<1||r.count>8||
    !RANGES.includes(r.range)||!Number.isSafeInteger(r.atMinute)||r.atMinute<check.atMinute||
    r.gmPhysicalArrivalConfirmed!==true||r.gmDistinctOfficersConfirmed!==true||r.gmWingGuardChosenConfirmed!==true)
   throw Error('Recibo de resposta da Guarda Alada inválido ou sem falha real.');
  if(out.length&&r.atMinute<out.at(-1).atMinute)throw Error('Respostas da patrulha fora de ordem temporal.');
  seen.add(r.checkId);out.push(clone(r));
 }
 for(const r of out){
  const resolved=(p.encounterHistory??[]).filter(x=>x.encounterId===r.encounterId);
  if(resolved.length>1||resolved.some(x=>!['adversaries_defeated','escaped','negotiated','ended','player_incapacitated'].includes(x.outcome)))
   throw Error('Histórico da patrulha incompatível com a resposta registrada.');
 }
 return out;
}
export function validateJewelResidenceWingResponse(p,world){
 const expected=replayJewelResidenceWingResponse(p);
 if(!EQ(expected,p?.operations?.residenceWingResponse??[]))throw Error('Estado da Guarda Alada não corresponde aos recibos.');
 if(world?.clock&&expected.some(x=>x.atMinute>(world.clock.elapsedMinutes??0)))throw Error('Resposta da patrulha registrada no futuro.');
 return expected;
}
export function pendingJewelResidenceWingChecks(p){
 const state=validateJewelArendPatrols(p).residence;
 const done=new Set(validateJewelResidenceWingResponse(p).map(x=>x.checkId));
 return (state?.checks??[]).filter(x=>!x.check.success&&!done.has(x.id));
}
export function prepareJewelResidenceWingResponse(world,combat,details={}){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
   p.operations?.arendRevenge?.foundAt!=='plaza-residence')throw Error('Resposta só na residência de Arend localizada oficialmente.');
 if(combat?.active||p.activeEncounterId)throw Error('Resolva o combate anterior antes de registrar outra patrulha.');
 if(!ID(details.actionId)||p.operations.receipts?.[details.actionId])throw Error('Identificador de patrulha repetido ou inválido.');
 const pending=pendingJewelResidenceWingChecks(p),check=pending[0];
 if(!check||check.id!==details.checkId)throw Error('Escolha a primeira falha de patrulha ainda pendente.');
 if(details.gmPhysicalArrivalConfirmed!==true||details.gmDistinctOfficersConfirmed!==true||details.gmWingGuardChosenConfirmed!==true)
  throw Error('Mestre precisa confirmar chegada física, indivíduos distintos e Guarda Alada como facção perseguidora.');
 if(!Number.isSafeInteger(details.count)||details.count<1||details.count>8||!RANGES.includes(details.range))
  throw Error('Mestre deve confirmar quantidade (1–8) e distância física dos guardas.');
 const now=w.clock?.elapsedMinutes;
 if(!Number.isSafeInteger(now)||now<check.atMinute)throw Error('Relógio real incompatível com a falha de patrulha.');
 const r={id:details.actionId,kind:KIND,version:1,source:SOURCE,checkId:check.id,sequence:check.sequence,
  encounterId:residenceWingEncounterId(check.sequence),profileKey:'bespin_wing_guard_officer',count:details.count,
  range:details.range,atMinute:now,gmPhysicalArrivalConfirmed:true,gmDistinctOfficersConfirmed:true,
  gmWingGuardChosenConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.residenceWingResponse=replayJewelResidenceWingResponse(p);
 validateJewelResidenceWingResponse(p,w);
 return {world:w,adjudication:r,encounterId:r.encounterId};
}
export function jewelResidenceWingEncounter(p,id){
 const r=validateJewelResidenceWingResponse(p).find(x=>x.encounterId===id);
 if(!r||p.encounterHistory?.some(x=>x.encounterId===id&&x.outcome))return null;
 return {id:r.encounterId,label:`Guarda Alada — patrulha física da residência (intervalo ${r.sequence})`,
  available:true,status:p.activeEncounterId===id?'active':'ready',
  playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
  adversaries:[{profileKey:'bespin_wing_guard_officer',count:r.count,range:r.range,
   name:`Oficiais da Guarda Alada — patrulha ${r.sequence}`}]};
}
