/** SWE09 The Jewel of Yavin, "Leave No Trace", printed p.71 / PDF around p.72.
 * After the G-100 is dismantled, its black box must be stolen or destroyed;
 * its remaining parts may be dumped at Cloud City's edge or split among fences
 * with a Hard Streetwise check. No credits, items, or automatic travel invented.
 * P193 saves with no BANK_EVIDENCE records retain their old status.
 */
import {rollJewelBankCheck,validateJewelBankEvidence} from './jewelBankingOperations.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
export const initialJewelEvidenceState=()=>({blackBox:null,parts:null,blackBoxRisk:false,history:[]});
export function replayJewelEvidence(progress){
  const receipts=progress?.operations?.receipts??{};
  const entries=Object.entries(receipts).filter(([,r])=>r?.kind==='BANK_EVIDENCE');
  if(!entries.length)return null;
  if(entries.length>32)throw Error('Excesso de decisões de evidências bancárias.');
  const bank=progress.operations?.bank;
  const all=Object.values(receipts);
  const lastCleanIndex=all.findLastIndex(r=>r?.kind==='BANK_STEP'&&r.step==='clean'&&r.success===true);
  if(lastCleanIndex<0||entries.some(([,r])=>all.indexOf(r)<=lastCleanIndex))
    throw Error('Provas registradas antes da desmontagem do G-100.');
  if(!bank?.evidenceCleared||!bank.history?.some(r=>r.step==='clean'&&r.success))
    throw Error('Caixa-preta não foi extraída do G-100.');
  const state=initialJewelEvidenceState();
  for(const [key,r] of entries){
    if(!validId(key)||r.id!==key||!['destroy_box','take_box','dump_parts','fence_parts'].includes(r.action)||
      Object.keys(r).some(k=>!['id','kind','action','reachedCityEdge','evidence','success'].includes(k)))
      throw Error('Recibo de evidência bancária adulterado.');
    if(r.action==='destroy_box'||r.action==='take_box'){
      if(state.blackBox!==null||own(r,'evidence')||own(r,'success')||own(r,'reachedCityEdge'))
        throw Error('Caixa-preta decidida duas vezes ou com resultado forjado.');
      state.blackBox=r.action==='destroy_box'?'destroyed':'taken';
      state.blackBoxRisk=state.blackBox==='taken';
    }else{
      if(state.blackBox===null||state.parts!==null)throw Error('Destino das peças fora de ordem ou duplicado.');
      if(r.action==='dump_parts'){
        if(r.reachedCityEdge!==true||own(r,'evidence')||own(r,'success'))
          throw Error('Descarte na borda de Cloud City sem confirmação do Mestre.');
        state.parts='dumped';
      }else{
        if(own(r,'reachedCityEdge')||!r.evidence||typeof r.success!=='boolean')
          throw Error('Venda das peças sem teste real de Manha.');
        validateJewelBankEvidence(r.evidence,'fence',bank);
        if(r.success!==r.evidence.roll.success)throw Error('Resultado da venda não corresponde aos dados.');
        if(r.success)state.parts='fenced';
      }
    }
    state.history.push(r);
  }
  return state;
}
export function validateJewelEvidence(progress){
  const expected=replayJewelEvidence(progress);
  if(!same(progress?.operations?.bankEvidence??null,expected))throw Error('Estado de evidências não corresponde aos recibos.');
  return expected;
}
export function performJewelEvidenceAction(world,character,{actionId,action,reachedCityEdge=false,rng=Math.random}={}){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
    throw Error('Decisão sobre provas fora da operação bancária.');
  if(!validId(actionId)||!['destroy_box','take_box','dump_parts','fence_parts'].includes(action))
    throw Error('Decisão sobre provas inválida.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Decisão sobre provas já registrada.');
  validateJewelEvidence(p);
  const previous=p.operations.bankEvidence??initialJewelEvidenceState();
  if(['destroy_box','take_box'].includes(action)&&previous.blackBox!==null)throw Error('Caixa-preta já decidida.');
  if(['dump_parts','fence_parts'].includes(action)&&(previous.blackBox===null||previous.parts!==null))
    throw Error('Extraia/decida a caixa-preta antes de descartar as peças.');
  const record={id:actionId,kind:'BANK_EVIDENCE',action};
  if(action==='dump_parts'){
    if(reachedCityEdge!==true)throw Error('Confirme chegada real à borda de Cloud City; nenhuma viagem é inventada.');
    record.reachedCityEdge=true;
  }
  if(action==='fence_parts'){
    const evidence=rollJewelBankCheck(character,'fence',p.operations.bank,{skill:'callejeo'},rng);
    record.evidence=evidence;record.success=evidence.roll.success;
  }
  p.operations.receipts[actionId]=record;
  p.operations.bankEvidence=replayJewelEvidence(p);
  p.updatedAt=new Date().toISOString();
  validateJewelEvidence(p);
  return {world:w,progress:structuredClone(p),adjudication:structuredClone(record),bankEvidence:structuredClone(p.operations.bankEvidence)};
}
