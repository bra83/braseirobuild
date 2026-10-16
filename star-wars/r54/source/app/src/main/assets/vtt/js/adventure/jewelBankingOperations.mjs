/**
 * SWE09 The Jewel of Yavin, Banking on a Break (pp. 68–72).
 * Stateful remote G-100 droid acquisition, incapacitation, bank intrusion
 * and evidence disposal. No fictitious credits or reward is paid here.
 * A receipt records each real narrative-dice roll; replay rejects modified
 * faces, changed outcomes, missing stages and forged successful transfers.
 */
import {buildPositivePool, rollPool, resolveSymbols, diceFacesForTests} from '../rules/narrativeDice.mjs';
import {validateJewelBankPreparation} from './jewelBankPreparations.mjs';
import {validateJewelSlicerHire,JEWEL_HIRE_PROFILES} from './jewelHiredSlicers.mjs';
import {recordResolvedTaskTime,validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
import {applyJewelBackdoors,replayJewelSlicingSpends,validateJewelSlicingSpends} from './jewelBankSlicingSpends.mjs';
import {validateJewelEvidence} from './jewelBankEvidence.mjs';
import {validateJewelLiveDroid} from './jewelBankLiveDroid.mjs';
import {validateJewelBankSettlement} from './jewelBankSettlement.mjs';
import {boltInventoryPreflight,attachOwnedBolt,validateJewelBankToolReplacement,validateJewelBoltPurchases} from './jewelBankEquipment.mjs';

const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const integer=(v,lo,hi)=>Number.isSafeInteger(v)&&v>=lo&&v<=hi;
const own=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
const retryTimeId=id=>`task_jewel_retry_${id}`;
const cleanupTimeId=id=>`task_jewel_cleanup_${id}`;
const TARGETS=Object.freeze({industrial_docks:1,port_town:2,tourist_district:3});
const SKILLS=Object.freeze({acquire:['sigilo','actividad_criminal'],disable:['computadoras'],subdue:['atletismo','pelea'],slice:['computadoras'],clean:['mecanica'],fence:['callejeo'],persuade:['carisma']});
const CHARACTERISTICS=Object.freeze({sigilo:'agilidad',actividad_criminal:'astucia',computadoras:'intelecto',mecanica:'intelecto',callejeo:'astucia',carisma:'presencia',atletismo:'fortaleza',pelea:'fortaleza'});
export const JEWEL_BANK_STEPS=Object.freeze(['acquire','disable','slice','clean']);
export const initialJewelBankState=()=>({stage:'SEARCH',target:null,restrainingBolt:false,alarm:false,attempts:0,minutesSpent:0,
  diverted:false,emptyWinner:false,locked:false,evidenceCleared:false,history:[]});

function difficultyFor(step,state,details={}){
  if(step==='acquire'){
    if(!own(TARGETS,details.target))throw new Error('Posto do G-100 não existe.');
    // Unrestrained droid may raise the alarm: one difficulty upgrade.
    return {difficulty:TARGETS[details.target]-(details.restrainingBolt?0:1),
      challenge:details.restrainingBolt?0:1};
  }
  if(step==='subdue')return {difficulty:3,challenge:0}; // SWE09: Hard Athletics/Brawl.
  if(step==='disable'){
    // Hard Computers versus the droid's Discipline; bolt downgrades twice.
    const upgrades=state.restrainingBolt?0:2;
    return {difficulty:3-upgrades,challenge:upgrades};
  }
  if(step==='slice'){
    // Lobot's published subroutine: Intellect 5, Computers 2.
    // Each prior failed attempt upgrades difficulty once; alarm upgrades once.
    const upgrades=2+state.attempts+Number(state.alarm);
    let difficulty=3,challenge=2;
    for(let i=0;i<state.attempts+Number(state.alarm);i++){
      if(difficulty>0){difficulty--;challenge++;}
      else difficulty++;
    }
    if(details.preparation?.lobot==='neutralized'){
      difficulty=4;challenge=0;
      for(let i=0;i<state.attempts+Number(state.alarm);i++){
        if(difficulty>0){difficulty--;challenge++;}else difficulty++;
      }
    }else if(details.preparation?.lobot==='distracted'&&challenge>0){challenge--;difficulty++;}
    if(details.signatureUpgrade){
      // Captured electronic signature: one upgrade to future Cloud City slicing.
      if(difficulty>0){difficulty--;challenge++;}else difficulty++;
    }
    return {difficulty,challenge};
  }
  if(step==='clean')return {difficulty:4,challenge:0};
  if(step==='persuade')return details.droidCharmVersion===2
    ? {difficulty:1,challenge:2} // SWE09 G-100 Nobody's Fool 3: upgrade Average Charm three times.
    : {difficulty:2,challenge:0}; // P195 receipts remain replayable without retroactive upgrades.
  if(step==='fence')return {difficulty:3,challenge:0};
  throw new Error('Etapa bancária desconhecida.');
}
function assertStage(step,state,details={}){
  if(state.locked&&step!=='clean')throw new Error('G-100 bloqueado após quatro tentativas.');
  if(step==='acquire'&&state.stage!=='SEARCH')throw new Error('Droid já capturado ou etapa incorreta.');
  if(step==='subdue'&&state.stage!=='SECURED')throw new Error('Capture o G-100 antes de tentar contê-lo fisicamente.');
  if(step==='disable'&&state.stage!=='SECURED')throw new Error('Capture o droid antes de incapacitá-lo.');
  if(step==='slice'&&state.stage!=='READY')throw new Error('Incapacite o droid antes de acessar o banco.');
  if(step==='persuade'&&(state.stage!=='FINISHED'||state.evidenceCleared))throw Error('Droid vivo só pode ser convencido após o slicing e antes da desmontagem.');
  if(step==='clean'&&state.stage!=='FINISHED')throw new Error('Finalize a tentativa de transferência antes de eliminar provas.');
  if(step==='slice'&&state.attempts>=4)throw new Error('Máximo de quatro tentativas excedido.');
  if(step==='clean'&&state.evidenceCleared)throw new Error('Caixa-preta já extraída.');
  if(step==='fence'&&(!state.evidenceCleared||state.stage!=='FINISHED'))throw new Error('Extraia a caixa-preta antes de vender peças.');
  if(step==='acquire'&&(!own(TARGETS,details.target)||typeof details.restrainingBolt!=='boolean'))throw new Error('Alvo ou contenção do droid inválidos.');
}
function checkSkill(step,skill){
  if(typeof skill!=='string'||!SKILLS[step]?.includes(skill))throw new Error('Perícia bancária inválida para esta etapa.');
}
export function rollJewelBankCheck(character,step,state,details={},rng=Math.random){
  assertStage(step,state,details);
  const skill=details.skill||(step==='acquire'?'sigilo':step==='clean'?'mecanica':'computadoras');
  checkSkill(step,skill);
  const charKey=CHARACTERISTICS[skill],characteristicRank=character?.characteristics?.[charKey],skillRank=character?.skills?.[skill];
  if(!integer(characteristicRank,0,7)||!integer(skillRank,0,7))throw new Error('Ficha de operador bancário inválida.');
  const negative=difficultyFor(step,state,details);
  const prep=step==='slice'?details.preparation:null;
  const pool=applyJewelBackdoors({...buildPositivePool(characteristicRank,skillRank),...negative,
    boost:(step==='disable'&&details.muscleAssist?.success?2:0)+(prep?.spike?.success&&state.attempts===0?prep.spike.boost:0),
    setback:prep?({none:3,basic:1,full:0}[prep.sample]??3)+Number(prep.beaAlerted===true):0},details.backdoor);
  const roll=rollPool(pool,rng);
  return {actorId:String(character.id??character.name??'operador'),step,skill,charKey,characteristicRank,skillRank,pool,roll};
}
export function validateJewelBankEvidence(evidence,step,state,details={}){
  if(!evidence||typeof evidence!=='object'||Array.isArray(evidence)||evidence.step!==step||
    typeof evidence.actorId!=='string'||!evidence.actorId||evidence.actorId.length>120)throw new Error('Prova bancária inválida.');
  checkSkill(step,evidence.skill);
  const {characteristicRank,skillRank}=evidence;
  if(evidence.charKey!==CHARACTERISTICS[evidence.skill]||!integer(characteristicRank,0,7)||!integer(skillRank,0,7))throw new Error('Atributos do operador adulterados.');
  const prep=step==='slice'?details.preparation:null;
  const expected=applyJewelBackdoors({...buildPositivePool(characteristicRank,skillRank),...difficultyFor(step,state,details),
    boost:(step==='disable'&&details.muscleAssist?.success?2:0)+(prep?.spike?.success&&state.attempts===0?prep.spike.boost:0),
    setback:prep?({none:3,basic:1,full:0}[prep.sample]??3)+Number(prep.beaAlerted===true):0},details.backdoor);
  if(!same(evidence.pool,expected)||!same(evidence.roll?.pool,expected)||!Array.isArray(evidence.roll?.faces))throw new Error('Pool bancário adulterado.');
  const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
  let i=0;
  for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
    for(let n=0;n<expected[type];n++){
      const face=evidence.roll.faces[i++],valid=diceFacesForTests[type];
      if(!face||face.type!==type||!integer(face.index,0,valid.length-1)||!same(face.face,valid[face.index]))throw new Error('Face bancária adulterada.');
      for(const [symbol,count] of Object.entries(face.face))raw[symbol]+=count;
    }
  }
  if(i!==evidence.roll.faces.length)throw new Error('Dados bancários adicionais detectados.');
  const resolved=resolveSymbols(raw);
  for(const key of ['success','netSuccess','netAdvantage','triumph','despair','raw'])
    if(!same(evidence.roll[key],resolved[key]))throw new Error('Símbolos bancários adulterados.');
  return true;
}
function applyBankRecord(state,record){
  const step=record.step;
  assertStage(step,state,record);
  if(record.muscleAssist!==undefined){
    if(step!=='disable'||!record.muscleAssist||typeof record.muscleAssist.success!=='boolean'||!record.muscleAssist.evidence)
      throw Error('Assistência física do G-100 fora da incapacitação.');
    validateJewelBankEvidence(record.muscleAssist.evidence,'subdue',state);
    if(!['atletismo','pelea'].includes(record.muscleAssist.evidence.skill)||
       record.muscleAssist.success!==record.muscleAssist.evidence.roll.success)
      throw Error('Assistência física adulterada.');
  }
  validateJewelBankEvidence(record.evidence,step,state,record);
  if(record.slicerId!==undefined){
    if(step!=='slice'||!Object.hasOwn(JEWEL_HIRE_PROFILES,record.slicerId))throw Error('Slicer NPC não autorizado para esta etapa.');
    const npc=JEWEL_HIRE_PROFILES[record.slicerId];
    if(record.evidence.actorId!==`npc:${npc.id}`||record.evidence.characteristicRank!==npc.intellect||record.evidence.skillRank!==npc.computers)
      throw Error('Atributos ou identidade do slicer NPC adulterados.');
  }
  const success=record.evidence.roll.success;
  if(step==='clean'&&record.cleanupVersion!==undefined&&record.cleanupVersion!==2)
    throw Error('Versão da desmontagem do G-100 adulterada.');
  if(step==='clean'&&record.cleanupVersion===2&&record.timeCheckId!==cleanupTimeId(record.id))
    throw Error('Desmontagem P194 sem tempo confirmado.');
  if(step==='clean'&&record.timeCheckId!==undefined&&record.timeCheckId!==cleanupTimeId(record.id))
    throw Error('Tempo de desmontagem do G-100 adulterado.');
  if(step==='slice'&&record.timeCheckId!==undefined&&
     (state.attempts<1||record.timeCheckId!==retryTimeId(record.id)))
    throw Error('Vínculo de tempo da nova tentativa bancária adulterado.');
  if(record.boltInventoryVersion!==undefined){
    if(step!=='acquire'||record.boltInventoryVersion!==2||record.restrainingBolt!==true||
       record.boltItemId!=='restraining_bolt'||record.boltActorId!==record.evidence.actorId||
       !['tool_kit','gm_confirmed_spot_welder'].includes(record.boltWelderSource))
      throw Error('Recibo de bolt físico adulterado.');
  }else if(record.boltItemId!==undefined||record.boltActorId!==undefined||record.boltWelderSource!==undefined)
    throw Error('Bolt sem recibo de inventário.');
  if(record.success!==success)throw new Error('Resultado bancário não corresponde aos dados.');
  if(step==='acquire'){
    if(success){state.stage='SECURED';state.target=record.target;state.restrainingBolt=record.restrainingBolt;
      if(record.boltInventoryVersion===2)state.boltAttachment={itemId:record.boltItemId,actorId:record.boltActorId,actionId:record.id,welderSource:record.boltWelderSource};
    }
  }else if(step==='disable'){
    if(success)state.stage='READY';
    else state.alarm=true;
  }else if(step==='slice'){
    // SWE09 p.69: Naboka will not risk another attempt after his first failed slice.
    // Legacy P207 receipts are replayed as written; all new P208 receipts carry
    // an explicit policy marker so removing it from a later receipt is detected.
    if(record.slicerPolicyVersion===2 && record.slicerId==='naboka' &&
       state.history.some(r=>r.step==='slice'&&r.slicerId==='naboka'&&!r.success))
      throw Error('Naboka recusa novas tentativas após a primeira falha. Use o personagem jogador.');
    if(typeof record.winnerId!=='string'||!record.winnerId)throw new Error('Licitante vencedor obrigatório.');
    if(state.winnerId&&state.winnerId!==record.winnerId)throw new Error('Licitante vencedor alterado durante a operação.');
    state.winnerId=record.winnerId;
    state.attempts++;
    if(state.attempts>1)state.minutesSpent+=30;
    if(success){
      state.stage='FINISHED';
      state.emptyWinner=record.winnerId==='pos_podura';
      state.diverted=!state.emptyWinner;
    }else if(state.attempts===4){state.stage='FINISHED';state.locked=true;}
  }else if(step==='clean'){
    if(success)state.evidenceCleared=true;
    // Failed cleanup may be retried, but no invented security outcome.
  }
  state.history.push(record);
}
export function replayJewelBankState(records){
  if(!Array.isArray(records)||records.length>32)throw new Error('Histórico bancário inválido.');
  const state=initialJewelBankState(),ids=new Set();
  for(const record of records){
    if(!record||typeof record!=='object'||Array.isArray(record)||record.kind!=='BANK_STEP'||
      typeof record.id!=='string'||!record.id||ids.has(record.id)||!JEWEL_BANK_STEPS.includes(record.step))
      throw new Error('Recibo bancário inválido ou duplicado.');
    ids.add(record.id);
    applyBankRecord(state,record);
  }
  return state;
}
export function validateJewelBankState(progress,world=null){
  validateJewelBankPreparation(progress,world);
  validateJewelSlicerHire(progress);
  validateJewelSlicingSpends(progress,world);
  const bank=progress?.operations?.bank;
  const receipts=progress?.operations?.receipts||{};
  const records=Object.values(receipts).filter(r=>r?.kind==='BANK_STEP');
  validateJewelBoltPurchases(progress);
  validateJewelBankToolReplacement(progress);
  if(progress.bankEquipmentContractVersion!==undefined&&progress.bankEquipmentContractVersion!==1)
    throw Error('Contrato de equipamentos bancários adulterado.');
  if(!bank){
    if(progress.operations?.liveDroid!=null||Object.values(receipts).some(r=>r?.kind==='BANK_DROID'))
      throw Error('G-100 vivo não pode ter recibo sem operação bancária.');
    validateJewelBankSettlement(progress);
    if(records.length|| (progress.bankContractVersion===1 &&
      (progress.flags?.bankDiversionSecured===true||progress.flags?.bankEmptyWinner===true)))
      throw new Error('Transferência bancária sem recibo do G-100.');
    return true;
  }
  if(progress.bankContractVersion!==1)throw new Error('Contrato bancário ausente para nova operação.');
  const slices=records.filter(r=>r.step==='slice');
  const policy=progress.bankSlicerPolicy??null;
  if(policy!==null){
    if(policy.version!==2||typeof policy.fromActionId!=='string')throw Error('Política do slicer adulterada.');
    const first=slices.findIndex(r=>r.id===policy.fromActionId);
    if(first<0||slices.slice(0,first).some(r=>r.slicerPolicyVersion!==undefined)||
       slices.slice(first).some(r=>r.slicerPolicyVersion!==2))throw Error('Recibos da política de Naboka adulterados.');
  }else if(slices.some(r=>r.slicerPolicyVersion!==undefined))throw Error('Política de Naboka sem âncora de recibos.');
  if(slices.length){
    const auction=progress.operations.auction;
    if(!auction?.completed||!auction.highBidderId||
       slices.some(r=>r.winnerId!==auction.highBidderId))
      throw new Error('Recibos bancários não correspondem ao leilão concluído.');
  }
  const prep=progress?.operations?.bankPreparation;
  if(prep&&records.some(r=>r.step==='slice'&&!same(r.preparation,prep)))
    throw new Error('Preparação do slicing não corresponde aos recibos.');
  const timed=records.filter(r=>r.timeCheckId!==undefined&&(r.step==='slice'||r.step==='clean'));
  if(timed.length){
    if(!world)throw Error('Relógio mundial necessário para verificar as novas tentativas bancárias.');
    validateTaskTimeLedger(world);
    for(const r of timed){
      const entry=world.taskTimeApplications?.[r.timeCheckId];
      const expectedSkill=r.step==='clean'?'mecanica':'computadoras';
      const expectedMinutes=r.step==='clean'?120:30;
      if(!entry||entry.checkId!==r.timeCheckId||entry.skillKey!==expectedSkill||
        entry.minutes!==expectedMinutes||entry.baseMinutes!==expectedMinutes)
        throw Error('Tempo bancário não corresponde ao relógio mundial.');
    }
  }
  if(world){
    const expectedCleanup=new Set(records.filter(r=>r.step==='clean'&&r.timeCheckId).map(r=>r.timeCheckId));
    for(const key of Object.keys(world.taskTimeApplications??{}))
      if(key.startsWith('task_jewel_cleanup_')&&!expectedCleanup.has(key))
        throw Error('Tempo de desmontagem órfão no relógio mundial.');
  }
  for(const r of slices){
    if(r.slicerId!==undefined&&(r.slicerId!==progress.operations?.hiredSlicer?.slicerId||
       !progress.operations?.receipts?.[progress.operations.hiredSlicer.hireReceiptId]))
      throw Error('Slicer NPC não contratado para o slicing.');
  }
  const expected=replayJewelBankState(records);
  if(!same(bank,expected))throw new Error('Estado bancário não corresponde ao histórico dos dados.');
  if(progress.flags?.bankDiversionSecured===true&&!bank.diverted)throw new Error('Desvio bancário sem transferência bem-sucedida.');
  if(progress.flags?.bankEmptyWinner===true&&!bank.emptyWinner)throw new Error('Saldo bancário inexistente adulterado.');
  validateJewelEvidence(progress);
  if(progress.bankEquipmentContractVersion===1&&records.some(r=>r.step==='acquire'&&r.restrainingBolt===true&&r.boltInventoryVersion!==2))
    throw Error('Nova captura com bolt sem prova física do inventário.');
  validateJewelLiveDroid(progress);
  validateJewelBankSettlement(progress);
  return true;
}
export function performJewelBankStep(world,nodes,character,{actionId,step,target=null,restrainingBolt=false,skill=null,muscleAssistSkill=null,useHiredSlicer=false,spotWelderConfirmed=false,rng=Math.random}={}){
  const w=structuredClone(world||{}),p=w.publishedAdventure;
  if(!p||p.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='central-bank-slice'||!own(nodes,p.currentNodeId))
    throw new Error('Operação bancária fora da cena.');
  if(typeof actionId!=='string'||!/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(actionId)||
     ['__proto__','constructor','prototype'].includes(actionId))throw new Error('Identificador bancário inválido.');
  p.operations??={};p.operations.receipts??={};
  if(own(p.operations.receipts,actionId))throw new Error('Ação bancária já registrada.');
  validateJewelBankState(p,w);
  if(!JEWEL_BANK_STEPS.includes(step))throw new Error('Etapa bancária desconhecida.');
  if(step==='clean'&&p.operations.liveDroid?.status==='persuaded')
    throw Error('O G-100 foi preservado vivo; desmontagem proibida.');
  if(step==='slice'&&(!p.operations.auction?.completed||!p.operations.auction.highBidderId))
    throw new Error('Leilão precisa ter um vencedor confirmado para transferir fundos.');
  const bank=p.operations.bank||initialJewelBankState();
  const bolt=step==='acquire'&&restrainingBolt?boltInventoryPreflight(character,{spotWelderConfirmed}):null;
  const spent=replayJewelSlicingSpends(p.operations.receipts);
  const backdoor=step==='slice'&&(spent?.pendingBoosts||spent?.pendingUpgrades)?{boosts:spent.pendingBoosts,upgrades:spent.pendingUpgrades}:null;
  const details={...(step==='acquire'?{target,restrainingBolt}:{}),
    ...(backdoor?{backdoor}:{}),
    ...(step==='slice'&&spent?.signatureCaptured&&!p.operations.bankTools?.signatureCleared?{signatureUpgrade:true}:{}),
    ...(step==='slice'?{winnerId:p.operations.auction.highBidderId,
      ...(p.operations.bankPreparation?{preparation:structuredClone(p.operations.bankPreparation)}:{})}:{})};
  if(typeof useHiredSlicer!=='boolean'||(useHiredSlicer&&step!=='slice'))throw Error('Slicer NPC somente no slicing.');
  const hire=useHiredSlicer?p.operations.hiredSlicer:null;
  if(useHiredSlicer&&!hire)throw Error('Contrate um slicer antes de usá-lo.');
  if(step==='slice'&&hire?.slicerId==='naboka'&&bank.history.some(r=>r.step==='slice'&&r.slicerId==='naboka'&&!r.success))
    throw Error('Naboka recusou nova tentativa após a primeira falha. O jogador pode tentar pessoalmente.');
  const operator=hire?{id:`npc:${hire.slicerId}`,characteristics:{intelecto:hire.intellect},skills:{computadoras:hire.computers}}:character;
  if(muscleAssistSkill!==null&&(step!=='disable'||!['atletismo','pelea'].includes(muscleAssistSkill)))
    throw Error('Assistência física somente em incapacitação com Atletismo ou Briga.');
  if(muscleAssistSkill!==null){
    const physical=rollJewelBankCheck(character,'subdue',bank,{skill:muscleAssistSkill},rng);
    details.muscleAssist={success:physical.roll.success,evidence:physical};
  }
  const evidence=rollJewelBankCheck(operator,step,bank,{...details,...(skill?{skill}:{})},rng);
  const record={id:actionId,kind:'BANK_STEP',step,...details,
    ...(step==='slice'?{slicerPolicyVersion:2}:{}),
    ...(bolt?{boltInventoryVersion:2,boltItemId:bolt.itemId,boltActorId:bolt.actorId,boltWelderSource:bolt.welderSource}:{}),
    ...(hire?{slicerId:hire.slicerId}:{}),success:evidence.roll.success,evidence,
    ...(step==='slice'&&bank.attempts>=1?{timeCheckId:retryTimeId(actionId)}:{}),
    ...(step==='clean'?{cleanupVersion:2,timeCheckId:cleanupTimeId(actionId)}:{})};
  const updated=structuredClone(bank);
  applyBankRecord(updated,record);
  p.operations.bank=updated;
  p.operations.receipts[actionId]=record;
  if(step==='slice'&&!p.bankSlicerPolicy)p.bankSlicerPolicy={version:2,fromActionId:actionId};
  if(p.operations.bankSpending)p.operations.bankSpending=replayJewelSlicingSpends(p.operations.receipts);
  if(updated.diverted)p.flags.bankDiversionSecured=true;
  if(updated.emptyWinner)p.flags.bankEmptyWinner=true;
  p.updatedAt=new Date().toISOString();
  const timed=record.timeCheckId?recordResolvedTaskTime(w,{
    checkId:record.timeCheckId,skillKey:step==='clean'?'mecanica':'computadoras',
    minutes:step==='clean'?120:30,baseMinutes:step==='clean'?120:30
  }):null;
  const nextWorld=timed?.world??w;
  validateJewelBankState(nextWorld.publishedAdventure,nextWorld);
  const nextCharacter=bolt&&record.success?attachOwnedBolt(character):null;
  return {world:nextWorld,character:nextCharacter,progress:structuredClone(nextWorld.publishedAdventure),adjudication:structuredClone(record),bank:structuredClone(updated)};
}

/** SWE09 p.69: computed from actual failed BANK_STEP receipts; no invented NPC turn. */
export function jewelSlicerAvailability(progress){
 const hire=progress?.operations?.hiredSlicer??null;
 const history=progress?.operations?.bank?.history??[];
 const refused=hire?.slicerId==='naboka'&&history.some(r=>r.step==='slice'&&r.slicerId==='naboka'&&r.success===false);
 return {hiredSlicerId:hire?.slicerId??null,refused:!!refused,canUseHiredSlicer:!!hire&&!refused,playerMayRetry:!!refused&&progress?.operations?.bank?.stage==='READY',source:'SWE09 The Jewel of Yavin, p.69'};
}
