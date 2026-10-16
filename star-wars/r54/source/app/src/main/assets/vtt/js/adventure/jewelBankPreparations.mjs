/**
 * SWE09, The Jewel of Yavin pp.39–41, 68–72.
 * GM-confirmed source preparation and real Computers roll for a single-use
 * computer spike. Receipts are replayed, not trusted as mutable summaries.
 * No fictitious GM success, inventory or financial settlement.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
import {recordResolvedTaskTime,validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
const integer=(v,min,max)=>Number.isSafeInteger(v)&&v>=min&&v<=max;
const spikeTimeId=id=>`task_jewel_spike_${id}`;
const methods=Object.freeze({
  arend_access_card:'full',
  government_infiltration:'full',
  public_office_distraction:'basic'
});
export const initialJewelBankPreparation=()=>({sample:'none',sampleMethod:null,lobot:'active',beaAlerted:false,
  spike:null,history:[]});
function checkRoll(evidence){
  if(!evidence||!integer(evidence.characteristicRank,0,7)||!integer(evidence.skillRank,0,7))
    throw Error('Prova de fabricação do spike inválida.');
  const pool={...buildPositivePool(evidence.characteristicRank,evidence.skillRank),difficulty:3,challenge:0,boost:0,setback:0};
  if(!same(evidence.pool,pool)||!same(evidence.roll?.pool,pool))throw Error('Pool de fabricação do spike adulterado.');
  let index=0;
  const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
  for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
    for(let i=0;i<pool[type];i++){
      const face=evidence.roll.faces?.[index++],valid=diceFacesForTests[type];
      if(!face||face.type!==type||!integer(face.index,0,valid.length-1)||!same(face.face,valid[face.index]))
        throw Error('Face do spike adulterada.');
      for(const [symbol,count] of Object.entries(face.face))raw[symbol]+=count;
    }
  }
  if(index!==evidence.roll.faces?.length)throw Error('Dados extras na fabricação do spike.');
  const result=resolveSymbols(raw);
  for(const k of ['success','netSuccess','netAdvantage','triumph','despair','raw'])
    if(!same(result[k],evidence.roll[k]))throw Error('Resultado do spike adulterado.');
  return result;
}
export function replayJewelBankPreparation(records){
  if(!Array.isArray(records)||records.length>12)throw Error('Histórico de preparação bancária inválido.');
  const p=initialJewelBankPreparation(),ids=new Set(),types=new Set();
  for(const r of records){
    if(!r||r.kind!=='BANK_PREP'||!validId(r.id)||ids.has(r.id))
      throw Error('Recibo de preparação inválido ou duplicado.');
    ids.add(r.id);
    if(r.step==='spike'&&types.has('spike'))throw Error('Spike já fabricado.');
    types.add(r.step);
    if(r.step==='sample'){
      if(r.gmConfirmed!==true||!own(methods,r.method)||r.sample!==methods[r.method])
        throw Error('Amostra de criptografia sem confirmação ou origem válida.');
      if(p.sample==='full'||(p.sample==='basic'&&r.sample!=='full'))
        throw Error('Amostra não pode regredir ou repetir sem ganho.');
      p.sample=r.sample;p.sampleMethod=r.method;
    }else if(r.step==='lobot'){
      if(r.gmConfirmed!==true||!['active','distracted','neutralized'].includes(r.mode)||
        typeof r.beaAlerted!=='boolean')
        throw Error('Contramedida de Lobot sem confirmação válida.');
      const ranks={active:0,distracted:1,neutralized:2};
      if(ranks[r.mode]<ranks[p.lobot]||(p.beaAlerted&&!r.beaAlerted)||
         (r.mode===p.lobot&&r.beaAlerted===p.beaAlerted))
        throw Error('Situação de Lobot não pode regredir nem repetir sem mudança.');
      p.lobot=r.mode;p.beaAlerted=r.beaAlerted;
    }else if(r.step==='spike'){
      if(!integer(r.creditsSpent,300,300)||!integer(r.hoursSpent,10,10)||
        typeof r.actorId!=='string'||!r.actorId||r.actorId.length>120||
        !integer(r.creditsBefore,300,100000000)||r.creditsAfter!==r.creditsBefore-300)
        throw Error('Custo do spike adulterado.');
      // P189 receipts commit the full ten hours to the canonical task-time ledger.
      // P188 receipts had no such link and remain valid without retroactive charges.
      if(r.timeCheckId!==undefined&&r.timeCheckId!==spikeTimeId(r.id))
        throw Error('Vínculo do relógio de fabricação adulterado.');
      const roll=checkRoll(r.evidence);
      if(r.success!==roll.success||r.boost!== (roll.success?Math.max(0,roll.netAdvantage):0))
        throw Error('Bônus do spike não corresponde aos dados.');
      p.spike={actorId:r.actorId,boost:r.boost,success:r.success,creditsSpent:300,hoursSpent:10};
    }else throw Error('Preparação bancária desconhecida.');
    p.history.push(r);
  }
  return p;
}
export function validateJewelBankPreparation(progress,world=null){
  const p=progress?.operations?.bankPreparation;
  const receipts=Object.values(progress?.operations?.receipts||{}).filter(r=>r?.kind==='BANK_PREP');
  if(!p){if(receipts.length)throw Error('Recibo bancário sem preparação.');return true;}
  if(!same(p,replayJewelBankPreparation(receipts)))throw Error('Preparação bancária adulterada.');
  const timed=receipts.filter(r=>r.step==='spike'&&r.timeCheckId!==undefined);
  if(timed.length){
    if(!world)throw Error('Relógio mundial necessário para validar a fabricação do spike.');
    validateTaskTimeLedger(world);
    for(const r of timed){
      const entry=world.taskTimeApplications?.[r.timeCheckId];
      if(!entry||entry.checkId!==r.timeCheckId||entry.skillKey!=='computadoras'||
        entry.minutes!==600||entry.baseMinutes!==600)
        throw Error('Fabricação do spike sem dez horas no relógio mundial.');
    }
  }
  return true;
}
export function performJewelBankPreparation(world,character,{actionId,step,method,mode,beaAlerted=false,gmConfirmed=false,rng=Math.random}={}){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||
    !['grand-prix-race','cloud-nine-party','central-bank-slice'].includes(p.currentNodeId))
    throw Error('Preparação fora da janela da aventura.');
  if(!validId(actionId)||!['sample','lobot','spike'].includes(step))
    throw Error('Ação de preparação inválida.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Preparação já registrada.');
  validateJewelBankPreparation(p,w);
  if(p.operations.bank?.attempts>0||p.operations.bank?.diverted)
    throw Error('Não é permitido preparar retroativamente após o slicing.');
  const current=p.operations.bankPreparation||initialJewelBankPreparation();
  if(step==='spike'&&current.spike)throw Error('Spike já fabricado.');
  const record={id:actionId,kind:'BANK_PREP',step};
  let updatedCharacter=null;
  if(step==='sample'){
    if(gmConfirmed!==true||!own(methods,method))throw Error('Amostra requer origem e aprovação do Mestre.');
    Object.assign(record,{method,sample:methods[method],gmConfirmed:true});
  }else if(step==='lobot'){
    if(gmConfirmed!==true||!['active','distracted','neutralized'].includes(mode)||
      typeof beaAlerted!=='boolean')throw Error('Contramedida de Lobot requer aprovação do Mestre.');
    Object.assign(record,{mode,beaAlerted,gmConfirmed:true});
  }else{
    const credits=character?.credits,rank=character?.characteristics?.intelecto,skill=character?.skills?.computadoras;
    if(!integer(credits,300,100000000))throw Error('São necessários 300 créditos para os componentes do spike.');
    if(!integer(rank,0,7)||!integer(skill,0,7))throw Error('Ficha de slicer inválida.');
    const pool={...buildPositivePool(rank,skill),difficulty:3,challenge:0,boost:0,setback:0};
    const roll=rollPool(pool,rng),boost=roll.success?Math.max(0,roll.netAdvantage):0;
    Object.assign(record,{actorId:String(character.id??character.name??'operador'),
      creditsBefore:credits,creditsAfter:credits-300,creditsSpent:300,hoursSpent:10,
      timeCheckId:spikeTimeId(actionId),
      evidence:{characteristicRank:rank,skillRank:skill,pool,roll},success:roll.success,boost});
    updatedCharacter={...character,credits:credits-300};
  }
  const next=replayJewelBankPreparation([...current.history,record]);
  p.operations.bankPreparation=next;p.operations.receipts[actionId]=record;
  p.updatedAt=new Date().toISOString();
  const timed=step==='spike'?recordResolvedTaskTime(w,{
    checkId:record.timeCheckId,skillKey:'computadoras',minutes:600,baseMinutes:600
  }):null;
  const nextWorld=timed?.world??w;
  validateJewelBankPreparation(nextWorld.publishedAdventure,nextWorld);
  return {world:nextWorld,character:updatedCharacter,preparation:structuredClone(next),adjudication:structuredClone(record)};
}
