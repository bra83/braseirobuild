/**
 * SWE09 The Jewel of Yavin, printed pp. 68–70, 73 and 90.
 * Reconciles the bank's successful transfer into a separate IN-WORLD account
 * ledger. This is NOT character credits, a cash withdrawal, or a promise that
 * Arend will pay the party. Amounts come only from the completed auction and
 * successful Table 2-1 additional-bidder receipts.
 */
import {replayJewelSlicingSpends} from './jewelBankSlicingSpends.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const validAccount=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const allowed=new Set(['id','kind','source','winnerId','winningSliceReceiptId','auctionReceiptId','destinationAccount','gmConfirmed','winningBidCredits','additionalBidderIds','additionalBidderCredits','totalCredits','status','ledger']);
const money=n=>Number.isSafeInteger(n)&&n>=0&&n<=1_000_000_000;
function facts(progress){
  const o=progress?.operations??{},receipts=o.receipts??{},bank=o.bank,auction=o.auction;
  if(!bank||bank.stage!=='FINISHED'||!auction?.completed||!validId(auction.highBidderId)||!money(auction.highBidCredits))
    throw Error('Leilão e operação bancária devem estar concluídos antes da liquidação.');
  const slices=Object.values(receipts).filter(r=>r?.kind==='BANK_STEP'&&r.step==='slice');
  const finalSlice=slices.at(-1);
  if(!finalSlice||finalSlice.winnerId!==auction.highBidderId||!validId(finalSlice.id))throw Error('Liquidação sem recibo de slicing do vencedor.');
  const successful=finalSlice.success===true;
  if(successful!==(!bank.locked)||Boolean(bank.emptyWinner)!==(successful&&auction.highBidderId==='pos_podura')||
     Boolean(bank.diverted)!==(successful&&auction.highBidderId!=='pos_podura'))throw Error('Estado da transferência bancária inconsistente.');
  const auctionRound=Object.values(receipts).filter(r=>r?.kind==='AUCTION_ROUND').at(-1);
  if(!auctionRound||!validId(auctionRound.id)||auctionRound.completed!==true||auctionRound.highBidCredits!==auction.highBidCredits||auctionRound.highBidderId!==auction.highBidderId)
    throw Error('Lance vencedor sem recibo do leilão.');
  const spends=replayJewelSlicingSpends(receipts);
  const extra=spends?.additionalBidderIds??[];
  if(!Array.isArray(extra)||extra.some(id=>!validId(id)||id===auction.highBidderId||id==='pos_podura')||new Set(extra).size!==extra.length)
    throw Error('Licitantes adicionais inválidos para liquidação.');
  const bonus=successful?extra.length*10000:0;
  if(spends?.bonusEntitlementCredits!==undefined&&spends.bonusEntitlementCredits!==extra.length*10000)
    throw Error('Créditos adicionais não correspondem aos símbolos consumidos.');
  const winner=successful&&!bank.emptyWinner?auction.highBidCredits:0;
  const total=winner+bonus;
  if(!money(total))throw Error('Total bancário fora do limite.');
  return {winnerId:auction.highBidderId,winningSliceReceiptId:finalSlice.id,auctionReceiptId:auctionRound.id,
    winningBidCredits:winner,additionalBidderIds:[...extra],additionalBidderCredits:bonus,totalCredits:total,
    status:!successful?'FAILED':total===0?'NO_FUNDS':'POSTED'};
}
function expectedReceipt(progress,{actionId,destinationAccount,gmConfirmed}){
  if(!validId(actionId)||!validAccount(destinationAccount)||gmConfirmed!==true)
    throw Error('Liquidação exige ID, conta de destino válida e confirmação do Mestre.');
  const f=facts(progress);
  const ledger=f.status==='POSTED'?[...(f.winningBidCredits?[{source:'winning_bid',bidderId:f.winnerId,credits:f.winningBidCredits}]:[]),
    ...f.additionalBidderIds.map(id=>({source:'additional_bidder',bidderId:id,credits:10000}))]:[];
  return {id:actionId,kind:'BANK_SETTLEMENT',source:'SWE09_BANKING_ON_A_BREAK',...f,destinationAccount,gmConfirmed:true,ledger};
}
export function validateJewelBankSettlement(progress){
  const o=progress?.operations??{},receipts=o.receipts??{};
  const settlements=Object.entries(receipts).filter(([,r])=>r?.kind==='BANK_SETTLEMENT');
  if(settlements.length>1)throw Error('Liquidação bancária duplicada.');
  if(!settlements.length){if(o.bankSettlement!==undefined)throw Error('Liquidação sem recibo.');return true;}
  const [id,r]=settlements[0];
  if(!r||Object.keys(r).some(k=>!allowed.has(k))||r.id!==id||!validId(id))throw Error('Recibo de liquidação adulterado.');
  const expected=expectedReceipt(progress,{actionId:id,destinationAccount:r.destinationAccount,gmConfirmed:r.gmConfirmed});
  if(!same(r,expected)||!same(o.bankSettlement,expected))throw Error('Liquidação ou livro-razão bancário adulterado.');
  const entries=Object.entries(receipts),settlementIndex=entries.findIndex(([key])=>key===id);
  if(entries.slice(settlementIndex+1).some(([,x])=>x?.kind==='BANK_STEP'&&x.step==='slice'||x?.kind==='BANK_SLICE_SPEND'||x?.kind==='AUCTION_ROUND'))
    throw Error('Liquidação antecede alteração posterior da transferência.');
  return true;
}
export function settleJewelBank(world,{actionId,destinationAccount='arend_shen',gmConfirmed=false}={}){
  const w=structuredClone(world??{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
    throw Error('Liquidação disponível somente na cena bancária ativa.');
  p.operations??={};p.operations.receipts??={};
  if(!validId(actionId)||own(p.operations.receipts,actionId))throw Error('Liquidação repetida ou identificador inválido.');
  validateJewelBankSettlement(p);
  if(p.operations.bankSettlement)throw Error('A transferência já foi liquidada.');
  const r=expectedReceipt(p,{actionId,destinationAccount,gmConfirmed});
  p.operations.receipts[actionId]=r;
  p.operations.bankSettlement=structuredClone(r);
  p.updatedAt=new Date().toISOString();
  validateJewelBankSettlement(p);
  return {world:w,progress:structuredClone(p),settlement:structuredClone(r),adjudication:structuredClone(r)};
}
