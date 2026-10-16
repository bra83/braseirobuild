/**
 * SWE09 The Jewel of Yavin, Table 2-1 (printed p.69 / PDF p.70).
 * Post-roll, source-backed consequences with deterministic receipt replay.
 * P192 adds additional-bidder entitlement (NOT paid credits), slicer strain,
 * Wing Guard dispatch/trace and electronic signature. P191 receipts remain valid.
 */
import {recordResolvedTaskTime,validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const own=(a,k)=>Object.prototype.hasOwnProperty.call(a,k);
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','prototype','constructor'].includes(id);
const positive=n=>Number.isSafeInteger(n)&&n>=0&&n<=12;
const delayId=id=>`task_jewel_slicing_delay_${id}`;
const baseKeys=['backdoorBoosts','backdoorUpgrades','delay15'];
const extraKeys=['extraBidderIds','extraBidderTriumphs','slicerStrain','wingGuardTeams','wingGuardAccelerations','wingGuardDespairTeams','wingGuardDespairAccelerations','signatureDespair'];
const bidderIds=new Set(['elaiza','pos_podura','shreya_ordassa','mil_mikkir','vorse_tabarith','zekra_fol','kaltho']);
export const initialJewelSlicingSpendState=()=>({pendingBoosts:0,pendingUpgrades:0,delayMinutes:0,spentStepIds:[]});
const extendedState=()=>({additionalBidderIds:[],bonusEntitlementCredits:0,slicerStrainApplied:0,wingGuardTeams:[],signatureCaptured:false});
export function normalizeJewelSlicingSpendChoice(choice){
  if(!choice||typeof choice!=='object'||Array.isArray(choice)||Object.keys(choice).some(k=>![...baseKeys,...extraKeys].includes(k)))
    throw Error('Escolhas da Tabela 2-1 inválidas.');
  const c={backdoorBoosts:choice.backdoorBoosts??0,backdoorUpgrades:choice.backdoorUpgrades??0,delay15:choice.delay15??0};
  const extended=extraKeys.some(k=>own(choice,k));
  if(extended){
    Object.assign(c,{extraBidderIds:choice.extraBidderIds??[],extraBidderTriumphs:choice.extraBidderTriumphs??0,
      slicerStrain:choice.slicerStrain??0,wingGuardTeams:choice.wingGuardTeams??0,
      wingGuardAccelerations:choice.wingGuardAccelerations??0,wingGuardDespairTeams:choice.wingGuardDespairTeams??0,wingGuardDespairAccelerations:choice.wingGuardDespairAccelerations??0,
      signatureDespair:choice.signatureDespair??0});
    if(!Array.isArray(c.extraBidderIds)||c.extraBidderIds.length>6||
       new Set(c.extraBidderIds).size!==c.extraBidderIds.length||
       c.extraBidderIds.some(id=>!bidderIds.has(id))||
       c.extraBidderTriumphs>c.extraBidderIds.length||c.signatureDespair>1)
      throw Error('Licitantes ou Desespero inválidos para a Tabela 2-1.');
  }
  if(Object.entries(c).some(([k,v])=>k!=='extraBidderIds'&&!positive(v)))throw Error('Custos de símbolos da Tabela 2-1 inválidos.');
  if(!Object.entries(c).some(([k,v])=>k==='extraBidderIds'?v.length:v))throw Error('Selecione uma consequência real da Tabela 2-1.');
  return c;
}
export function applyJewelBackdoors(pool,backdoor){
  if(!backdoor)return pool;
  if(!positive(backdoor.boosts)||!positive(backdoor.upgrades)||Object.keys(backdoor).some(k=>!['boosts','upgrades'].includes(k)))
    throw Error('Backdoor do banco adulterado.');
  const p={...pool};
  for(let i=0;i<backdoor.upgrades;i++){
    if(p.ability>0){p.ability--;p.proficiency++;}else p.ability++;
  }
  p.boost=(p.boost??0)+backdoor.boosts;
  if(Object.values(p).some(v=>!Number.isSafeInteger(v)||v<0||v>12))throw Error('Pool de backdoor excedeu os dados disponíveis.');
  return p;
}
export function replayJewelSlicingSpends(receipts){
  if(!receipts||typeof receipts!=='object'||Array.isArray(receipts))throw Error('Recibos bancários inválidos.');
  const state=initialJewelSlicingSpendState(),steps=new Map(),used=new Set();
  let hasContract=false,lastStep=null,elapsedSeen=0,signatureCleared=false;
  for(const [key,r] of Object.entries(receipts)){
    if(!r||typeof r!=='object'||Array.isArray(r))continue;
    if(r.kind==='BANK_STEP'){
      if(r.id!==key)throw Error('Recibo bancário fora da chave canônica.');
      if(r.step==='slice'){
        const expected=state.pendingBoosts||state.pendingUpgrades?{boosts:state.pendingBoosts,upgrades:state.pendingUpgrades}:null;
        if(!same(r.backdoor??null,expected))throw Error('Backdoor consumido não corresponde ao histórico.');
        if(Boolean(r.signatureUpgrade)!==Boolean(state.signatureCaptured&&!signatureCleared))throw Error('Assinatura eletrônica não aplicada ao slicing posterior.');
        state.pendingBoosts=0;state.pendingUpgrades=0;
        steps.set(key,r);lastStep=key;
      }else lastStep=null;
    }else if(r.kind==='BANK_TOOL_REPLACE'){
      // Receipt details are validated independently by jewelBankEquipment.
      signatureCleared=true;lastStep=null;
    }else if(r.kind==='BANK_SLICE_SPEND'){
      hasContract=true;
      if(Object.keys(r).some(k=>!['id','kind','bankStepId','choice','minutes','timeCheckId','partySize','dispatchElapsedMinutes','strainActorId','strainBefore','strainThreshold'].includes(k)))
        throw Error('Recibo de gasto bancário contém campos não autorizados.');
      if(r.id!==key||!validId(key)||!validId(r.bankStepId)||r.bankStepId!==lastStep||used.has(lastStep))
        throw Error('Gasto de símbolos fora da última rolagem ou duplicado.');
      const source=steps.get(lastStep);
      if(!source||!source.evidence?.roll)throw Error('Rolagem bancária sem prova para gastar símbolos.');
      const c=normalizeJewelSlicingSpendChoice(r.choice);
      if(!same(r.choice,c))throw Error('Custos de símbolos adulterados.');
      const roll=source.evidence.roll;
      const availableAdv=Math.max(0,roll.netAdvantage),availableThreat=Math.max(0,-roll.netAdvantage);
      const extra=c.extraBidderIds??[],tri=c.extraBidderTriumphs??0;
      const advCost=c.backdoorBoosts+2*c.backdoorUpgrades+3*(extra.length-tri);
      const threatCost=2*c.delay15+(c.slicerStrain??0)+3*((c.wingGuardTeams??0)+(c.wingGuardAccelerations??0));
      const despairCost=(c.wingGuardDespairTeams??0)+(c.wingGuardDespairAccelerations??0)+(c.signatureDespair??0);
      if(advCost>availableAdv||threatCost>availableThreat||tri>(roll.triumph??0)||despairCost>(roll.despair??0))
        throw Error('Símbolos insuficientes para as escolhas da Tabela 2-1.');
      if((c.backdoorBoosts||c.backdoorUpgrades)&&source.success)
        throw Error('Backdoor de nova tentativa não é aplicável após sucesso.');
      if(c.backdoorBoosts+c.backdoorUpgrades>12||state.pendingBoosts+c.backdoorBoosts>12||state.pendingUpgrades+c.backdoorUpgrades>12)
        throw Error('Backdoors excedem o limite de dados.');
      if((c.delay15>0)!==(r.timeCheckId===delayId(key)))throw Error('Tempo adicional sem recibo canônico.');
      if(r.minutes!==15*c.delay15)throw Error('Tempo adicional adulterado.');
      if(extra.length||(c.slicerStrain??0)||(c.wingGuardTeams??0)||(c.wingGuardAccelerations??0)||(c.wingGuardDespairTeams??0)||(c.wingGuardDespairAccelerations??0)||(c.signatureDespair??0)){
        if(!own(state,'additionalBidderIds'))Object.assign(state,extendedState());
        if(extra.length){
          if(!source.success||roll.netSuccess<=0||extra.some(id=>id==='pos_podura'||id===source.winnerId||state.additionalBidderIds.includes(id)))
            throw Error('Licitante adicional sem sucesso, repetido ou vencedor do leilão.');
          state.additionalBidderIds.push(...extra);
          state.bonusEntitlementCredits+=10000*extra.length; // Entitlement only, never character.credits.
        }
        if(c.slicerStrain){
          if(source.slicerId||r.strainActorId!==source.evidence.actorId||
             !Number.isSafeInteger(r.strainBefore)||r.strainBefore<0||
             !Number.isSafeInteger(r.strainThreshold)||r.strainThreshold<1||
             r.strainBefore+c.slicerStrain>r.strainThreshold)
            throw Error('Tensão do slicer excede o limite ou não corresponde ao operador.');
          state.slicerStrainApplied+=c.slicerStrain;
        }else if(['strainActorId','strainBefore','strainThreshold'].some(k=>own(r,k)))throw Error('Tensão do slicer sem símbolo.');
        const guards=(c.wingGuardTeams??0)+(c.wingGuardDespairTeams??0);
        if(guards||(c.wingGuardAccelerations??0)||(c.wingGuardDespairAccelerations??0)){
          if(!Number.isSafeInteger(r.partySize)||r.partySize<1||r.partySize>9||
             !Number.isSafeInteger(r.dispatchElapsedMinutes)||r.dispatchElapsedMinutes<0||r.dispatchElapsedMinutes<elapsedSeen)
            throw Error('Patrulha Wing Guard sem tamanho/horário canônico.');
          elapsedSeen=r.dispatchElapsedMinutes;
          for(let i=0;i<guards;i++)state.wingGuardTeams.push({dispatchId:key,guardCount:r.partySize,
            dispatchedAt:r.dispatchElapsedMinutes,arrivalElapsedMinutes:r.dispatchElapsedMinutes+240});
          if(c.wingGuardAccelerations||c.wingGuardDespairAccelerations){
            if(!state.wingGuardTeams.length)throw Error('Não existe patrulha para acelerar.');
            const target=state.wingGuardTeams.at(-1);
            target.arrivalElapsedMinutes=Math.max(target.dispatchedAt,target.arrivalElapsedMinutes-60*(c.wingGuardAccelerations+c.wingGuardDespairAccelerations));
          }
        }else if(own(r,'partySize')||own(r,'dispatchElapsedMinutes'))throw Error('Dados de patrulha sem símbolo.');
        if(c.signatureDespair){
          if(state.signatureCaptured)throw Error('Assinatura eletrônica já capturada.');
          state.signatureCaptured=true;
        }
      }else if(['partySize','dispatchElapsedMinutes','strainActorId','strainBefore','strainThreshold'].some(k=>own(r,k)))
        throw Error('Efeito bancário adicional sem símbolo.');
      state.pendingBoosts+=c.backdoorBoosts;
      state.pendingUpgrades+=c.backdoorUpgrades;
      state.delayMinutes+=r.minutes;
      state.spentStepIds.push(lastStep);
      used.add(lastStep);
    }
  }
  return hasContract?state:null;
}
export function validateJewelSlicingSpends(progress,world=null){
  const receipts=progress?.operations?.receipts||{};
  const expected=replayJewelSlicingSpends(receipts);
  if(!same(progress?.operations?.bankSpending??null,expected))throw Error('Estado de gastos da Tabela 2-1 não corresponde aos recibos.');
  const records=Object.values(receipts).filter(r=>r?.kind==='BANK_SLICE_SPEND');
  if(records.some(r=>r.timeCheckId)){
    if(!world)throw Error('Relógio mundial obrigatório para a Tabela 2-1.');
    validateTaskTimeLedger(world);
    for(const r of records){
      if(!r.timeCheckId)continue;
      const e=world.taskTimeApplications?.[r.timeCheckId];
      if(!e||e.checkId!==r.timeCheckId||e.skillKey!=='computadoras'||e.minutes!==r.minutes||e.baseMinutes!==r.minutes)
        throw Error('Quinze minutos de slicing sem registro no relógio.');
    }
  }
  if(world&&records.some(r=>r.dispatchElapsedMinutes!==undefined)&&
     records.some(r=>r.dispatchElapsedMinutes>(world.clock?.elapsedMinutes??0)))
    throw Error('Patrulha Wing Guard registrada no futuro.');
  return expected;
}
export function performJewelSlicingSpend(world,{actionId,bankStepId,choice}={},character=null,partySize=1){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice')
    throw Error('Gasto de símbolos fora da cena bancária.');
  if(!validId(actionId)||!validId(bankStepId))throw Error('Identificador da Tabela 2-1 inválido.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw Error('Gasto de símbolos já registrado.');
  validateJewelSlicingSpends(p,w);
  const c=normalizeJewelSlicingSpendChoice(choice);
  const source=p.operations.receipts[bankStepId];
  const strain=c.slicerStrain??0;
  let nextCharacter=null;
  if(strain){
    if(!character||!source||source.evidence?.actorId!==String(character.id??character.name??'operador')||source.slicerId)
      throw Error('Tensão exige a ficha real do slicer que rolou.');
    const {current,threshold}=character.derived?.strain??{};
    if(!Number.isSafeInteger(current)||!Number.isSafeInteger(threshold)||current<0||threshold<1||current+strain>threshold)
      throw Error('Tensão excederia o limite do slicer.');
    nextCharacter=structuredClone(character);
    nextCharacter.derived.strain.current=current+strain;
  }
  const guardCount=(c.wingGuardTeams??0)+(c.wingGuardDespairTeams??0)+(c.wingGuardAccelerations??0)+(c.wingGuardDespairAccelerations??0);
  if(guardCount&&(!Number.isSafeInteger(partySize)||partySize<1||partySize>9))throw Error('Número de personagens inválido para Wing Guard.');
  const r={id:actionId,kind:'BANK_SLICE_SPEND',bankStepId,choice:c,minutes:c.delay15*15,
    ...(c.delay15?{timeCheckId:delayId(actionId)}:{}),
    ...(strain?{strainActorId:source.evidence.actorId,strainBefore:character.derived.strain.current,
      strainThreshold:character.derived.strain.threshold}:{}),
    ...(guardCount?{partySize,dispatchElapsedMinutes:w.clock?.elapsedMinutes??0}:{})};
  p.operations.receipts[actionId]=r;
  p.operations.bankSpending=replayJewelSlicingSpends(p.operations.receipts);
  p.updatedAt=new Date().toISOString();
  const timed=c.delay15?recordResolvedTaskTime(w,{checkId:r.timeCheckId,skillKey:'computadoras',minutes:r.minutes,baseMinutes:r.minutes}):null;
  const next=timed?.world??w;
  validateJewelSlicingSpends(next.publishedAdventure,next);
  return {world:next,character:nextCharacter,progress:structuredClone(next.publishedAdventure),
    adjudication:structuredClone(r),spending:structuredClone(next.publishedAdventure.operations.bankSpending)};
}
