/** SWE09 The Jewel of Yavin, Leave No Trace (PDF section around printed p.71).
 * Alternative to dismantling G-100: Average Charm vs Cool, plus the G-100's
 * printed Nobody's Fool 3 (three difficulty upgrades to Charm). The droid's
 * Cool rank is not printed; do not invent it. P195 legacy receipts retain their
 * original pool, while P196 new rolls enforce the three upgrades.
 */
import {rollJewelBankCheck,validateJewelBankEvidence} from './jewelBankingOperations.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
export function replayJewelLiveDroid(progress){
  const receipts=progress?.operations?.receipts??{};
  const entries=Object.entries(receipts).filter(([,r])=>r?.kind==='BANK_DROID');
  if(!entries.length)return null;
  const all=Object.values(receipts),bank=progress.operations?.bank;
  if(!bank||bank.stage!=='FINISHED'||bank.locked)throw Error('Negociação com G-100 exige slicing concluído sem bloqueio bancário.');
  const lastSlice=all.findLastIndex(r=>r?.kind==='BANK_STEP'&&r.step==='slice');
  const firstClean=all.findIndex(r=>r?.kind==='BANK_STEP'&&r.step==='clean');
  const firstEvidence=all.findIndex(r=>r?.kind==='BANK_EVIDENCE');
  const state={status:'refused',berthConfirmed:false,history:[]};
  for(const [id,r] of entries){
    const position=all.indexOf(r);
    if(!validId(id)||r.id!==id||Object.keys(r).some(k=>!['id','kind','berthConfirmed','success','evidence','droidCharmVersion'].includes(k))||
      r.berthConfirmed!==true||typeof r.success!=='boolean'||!r.evidence||
      position<=lastSlice||firstClean>=0&&position>=firstClean||firstEvidence>=0&&position>=firstEvidence||
      state.status==='persuaded')throw Error('Recibo de droid vivo inválido ou fora de ordem.');
    if(r.droidCharmVersion!==undefined&&r.droidCharmVersion!==2)throw Error('Versão da oposição do G-100 adulterada.');
    validateJewelBankEvidence(r.evidence,'persuade',bank,{droidCharmVersion:r.droidCharmVersion});
    if(r.evidence.skill!=='carisma'||r.success!==r.evidence.roll.success)throw Error('Resultado de Carisma do G-100 adulterado.');
    state.status=r.success?'persuaded':'refused';
    state.berthConfirmed=true;
    state.history.push(r);
  }
  if(state.status==='persuaded'&&(bank.evidenceCleared||firstClean>=0||firstEvidence>=0))
    throw Error('Droid vivo não pode ser desmontado ou ter a caixa-preta extraída.');
  return state;
}
export function validateJewelLiveDroid(progress){
  const expected=replayJewelLiveDroid(progress);
  if(!same(progress?.operations?.liveDroid??null,expected))throw Error('Estado do G-100 vivo diverge dos recibos.');
  return expected;
}
export function persuadeJewelLiveDroid(world,character,{actionId,berthConfirmed=false,rng=Math.random}={}){
  const w=structuredClone(world??{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
    throw Error('Negociação com o G-100 fora da cena bancária.');
  if(!validId(actionId)||berthConfirmed!==true)throw Error('Confirme um alojamento real na nave antes de convencer o droid.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Ação do G-100 duplicada.');
  validateJewelLiveDroid(p);
  const bank=p.operations.bank;
  if(!bank||bank.stage!=='FINISHED'||bank.locked||bank.evidenceCleared||p.operations.liveDroid?.status==='persuaded')
    throw Error('G-100 não está disponível para ser preservado vivo.');
  const evidence=rollJewelBankCheck(character,'persuade',bank,{skill:'carisma',droidCharmVersion:2},rng);
  const record={id:actionId,kind:'BANK_DROID',droidCharmVersion:2,berthConfirmed:true,success:evidence.roll.success,evidence};
  p.operations.receipts[actionId]=record;
  p.operations.liveDroid=replayJewelLiveDroid(p);
  p.updatedAt=new Date().toISOString();
  validateJewelLiveDroid(p);
  return {world:w,progress:structuredClone(p),adjudication:structuredClone(record),liveDroid:structuredClone(p.operations.liveDroid)};
}
