/** P204 — source-backed SWE09 bomb and EotE falling consequences on PRESENT companions.
 * An explicit GM location confirmation is required for each target and each event.
 * Reuses the P203 damage/soak/critical engine and its full narrative-dice replay.
 * Legacy player impact receipts remain unchanged.
 */
import {performJewelEscapeImpact,replayJewelEscapeImpact,validateJewelEscapeImpact} from './jewelEscapeImpact.mjs';
import {validateJewelEscapeBomb} from './jewelEscapeBomb.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const KEYS=['id','kind','companionId','actorId','mode','position','gmPresenceConfirmed','impactReceipt'];
function synthetic(progress,companionId,history=[]){
 const p=clone(progress);
 const original=p.operations?.receipts??{};
 const receipts={};
 for(const [id,r] of Object.entries(original))if(r?.kind==='JEWEL_ESCAPE_BOMB')receipts[id]=r;
 for(const r of history)receipts[r.id]=clone(r);
 p.operations={...p.operations,receipts,escapeImpact:history.length?null:null};
 if(history.length)p.operations.escapeImpact=replayJewelEscapeImpact(p);
 return p;
}
export function replayJewelCompanionImpact(progress){
 const receipts=progress?.operations?.receipts??{};
 const list=Object.entries(receipts).filter(([,r])=>r?.kind==='JEWEL_COMPANION_IMPACT');
 if(!list.length)return null;
 validateJewelEscapeBomb(progress);
 const byCompanion={};
 for(const [id,r] of list){
  if(!r||Object.keys(r).some(k=>!KEYS.includes(k))||id!==r.id||!validId(id)||!validId(r.companionId)||!validId(r.actorId)||
    r.gmPresenceConfirmed!==true||!['blast','fall'].includes(r.mode)||r.position!==(r.mode==='blast'?'inside-speeder':'falling')||
    r.impactReceipt?.id!==id||r.impactReceipt?.kind!=='JEWEL_ESCAPE_IMPACT'||r.impactReceipt?.actorId!==r.actorId||r.impactReceipt?.mode!==r.mode)
   throw Error('Recibo de impacto do companheiro adulterado ou sem posição confirmada.');
  const previous=byCompanion[r.companionId]??{actorId:r.actorId,blastApplied:false,fallApplied:false,history:[]};
  if(previous.actorId!==r.actorId||previous[r.mode==='blast'?'blastApplied':'fallApplied']||
    (previous.history.length&&!eq(previous.history.at(-1).after,r.impactReceipt.before)))
   throw Error('Impactos de companheiro duplicados ou ficha anterior divergente.');
  const history=[...previous.history,r.impactReceipt];
  const tmp=synthetic(progress,r.companionId,history);
  const state=validateJewelEscapeImpact(tmp);
  byCompanion[r.companionId]={actorId:r.actorId,blastApplied:state.blastApplied,fallApplied:state.fallApplied,history};
 }
 return {impactVersion:3,byCompanion};
}
export function validateJewelCompanionImpact(progress){
 const actual=progress?.operations?.escapeCompanionImpact??null,expected=replayJewelCompanionImpact(progress);
 if(!eq(actual,expected))throw Error('Impactos dos companheiros divergem dos recibos.');
 return expected;
}
export function performJewelCompanionImpact(world,member,details={}){
 const w=clone(world??{}),p=w.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit')throw Error('Impacto fora da fuga oficial.');
 if(!validId(details.actionId)||!validId(details.companionId)||!validId(member?.id)||member.id!==details.companionId||
   details.gmPresenceConfirmed!==true||details.position!==(details.mode==='blast'?'inside-speeder':'falling'))
  throw Error('Selecione companheiro real e confirme sua posição na explosão ou queda.');
 if(p.operations?.receipts?.[details.actionId])throw Error('ID de impacto duplicado.');
 const before=clone(member.character),actorId=String(before?.id??before?.name??'');
 if(!validId(actorId))throw Error('Companheiro sem ficha válida.');
 const previous=validateJewelCompanionImpact(p);
 const record=previous?.byCompanion?.[member.id];
 if(record&&record.actorId!==actorId)throw Error('Ficha de companheiro substituída durante a fuga.');
 if(record?.history.length&&!eq(record.history.at(-1).after,before))throw Error('Ficha atual não corresponde ao último impacto persistido.');
 const shadow=clone(w);shadow.publishedAdventure=synthetic(p,member.id,record?.history??[]);
 const result=performJewelEscapeImpact(shadow,before,details);
 const impactReceipt=clone(result.adjudication);
 const r={id:details.actionId,kind:'JEWEL_COMPANION_IMPACT',companionId:member.id,actorId,mode:details.mode,
  position:details.position,gmPresenceConfirmed:true,impactReceipt};
 p.operations.receipts[r.id]=r;
 p.operations.escapeCompanionImpact=replayJewelCompanionImpact(p);
 validateJewelCompanionImpact(p);
 return {world:w,character:result.character,adjudication:r,companionImpact:clone(p.operations.escapeCompanionImpact)};
}
