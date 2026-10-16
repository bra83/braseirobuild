/**
 * P173 — Recibos de Negociação para Mercador especialista (AoR).
 *
 * Apenas GameController.resolveMerchantNegotiation emite recibos; uma proposta
 * narrativa do Mestre não pode gerar, alterar ou consumir estes registros.
 * A ação econômica exige recibo não utilizado, associado ao mesmo personagem,
 * campanha, sessão e ativo/pedido, preservado entre reaberturas.
 */
const clone=value=>structuredClone(value);
const KINDS=new Set(['buy','sell']);
const MAX_RECEIPTS=32;
export function merchantTarget(state,kind,targetId){
  if(!KINDS.has(kind)||typeof targetId!=='string'||!targetId.trim())throw new Error('Destino da Negociação inválido.');
  if(kind==='sell'){
    if(!(state.group?.ownedAssets??[]).some(a=>a.assetId===targetId))throw new Error('Ativo da Negociação não está possuído.');
  }else{
    if(state.master?.pendingAcquisition?.requestId!==targetId||state.master.pendingAcquisition.status!=='aguardando_mestre')
      throw new Error('Pedido da Negociação não está pendente.');
  }
  return targetId;
}
export function issueMerchantReceipt(state,{id,kind,targetId,check,at=new Date().toISOString()}={}){
  merchantTarget(state,kind,targetId);
  if(typeof id!=='string'||!/^merchant_[a-zA-Z0-9-]{8,}$/.test(id))throw new Error('ID de recibo inválido.');
  if(!check||typeof check.success!=='boolean'||!Number.isFinite(check.netSuccess))throw new Error('Teste de Negociação sem resultado mecânico.');
  if(!state.player?.character?.name)throw new Error('Personagem da Negociação ausente.');
  if((state.master?.merchantNegotiationReceipts??[]).some(r=>r.id===id))throw new Error('Recibo duplicado.');
  return {
    id,kind,targetId,skillKey:'negociacion',success:check.success,netSuccess:check.netSuccess,
    strainPaid:2,characterName:state.player.character.name,campaignId:state.campaignId,
    sessionId:state.sessionId,at,consumed:false,consumedAt:null
  };
}
export function merchantReceiptFor(state,{receiptId,kind,targetId,requireSuccess=false}={}){
  const receipts=state.master?.merchantNegotiationReceipts??[];
  if(!Array.isArray(receipts))throw new Error('Histórico de Negociação inválido.');
  const r=receipts.find(x=>x.id===receiptId);
  if(!r||r.consumed||r.kind!==kind||r.targetId!==targetId||r.skillKey!=='negociacion'||
      r.strainPaid!==2||r.campaignId!==state.campaignId||r.sessionId!==state.sessionId||
      r.characterName!==state.player?.character?.name||typeof r.success!=='boolean')
    throw new Error('Recibo de Negociação ausente, consumido ou incompatível.');
  if(requireSuccess&&!r.success)throw new Error('Negociação não foi bem-sucedida.');
  merchantTarget(state,kind,targetId);
  return clone(r);
}
export function validateMerchantReceiptLedger(state){
  const receipts=state.master?.merchantNegotiationReceipts??[];
  if(!Array.isArray(receipts)||receipts.length>MAX_RECEIPTS)throw new Error('Ledger de Negociação inválido.');
  const seen=new Set();
  for(const r of receipts){
    if(!r||typeof r!=='object'||typeof r.id!=='string'||seen.has(r.id)||
       !KINDS.has(r.kind)||typeof r.targetId!=='string'||!r.targetId||
       r.skillKey!=='negociacion'||typeof r.success!=='boolean'||
       !Number.isFinite(r.netSuccess)||r.strainPaid!==2||
       typeof r.characterName!=='string'||!r.characterName||
       r.campaignId!==state.campaignId||typeof r.sessionId!=='string'||
       typeof r.at!=='string'||typeof r.consumed!=='boolean'||
       (r.consumed?typeof r.consumedAt!=='string':r.consumedAt!==null))
      throw new Error('Recibo de Negociação adulterado.');
    seen.add(r.id);
    const issued=(state.events??[]).filter(e=>e.type==='MERCHANT_NEGOTIATION_RECORDED'&&e.payload?.receipt?.id===r.id);
    const event=issued[0];
    if(issued.length!==1||event.payload.receipt.kind!==r.kind||event.payload.receipt.targetId!==r.targetId||
       event.payload.receipt.success!==r.success||event.payload.receipt.netSuccess!==r.netSuccess||
       event.payload.receipt.sessionId!==r.sessionId||event.payload.receipt.characterName!==r.characterName||
       event.payload.receipt.campaignId!==r.campaignId||event.payload.receipt.strainPaid!==2||
       event.payload.receipt.at!==r.at||event.payload.receipt.consumed!==false)
      throw new Error('Recibo sem evento mecânico correspondente.');
    const spent=(state.events??[]).filter(e=>e.type==='MERCHANT_NEGOTIATION_CONSUMED'&&e.payload?.receiptId===r.id);
    if((r.consumed&&(spent.length!==1||spent[0].at!==r.consumedAt))||(!r.consumed&&spent.length!==0))
      throw new Error('Consumo de recibo inconsistente.');
  }
  return true;
}
export function appendMerchantReceipt(state,receipt){
  const current=state.master?.merchantNegotiationReceipts??[];
  if(current.some(x=>x.id===receipt.id))throw new Error('Recibo duplicado.');
  const next=[...current,clone(receipt)];
  while(next.length>MAX_RECEIPTS){
    const oldestConsumed=next.findIndex(r=>r.consumed);
    if(oldestConsumed<0)throw new Error('Limite de recibos pendentes atingido; conclua negociações antes de rolar novamente.');
    next.splice(oldestConsumed,1);
  }
  return next;
}
export function consumeMerchantReceipt(state,receiptId,at){
  const list=state.master?.merchantNegotiationReceipts??[];
  const found=list.find(r=>r.id===receiptId);
  if(!found||found.consumed)throw new Error('Recibo de Negociação já consumido.');
  return list.map(r=>r.id===receiptId?{...r,consumed:true,consumedAt:at}:r);
}
