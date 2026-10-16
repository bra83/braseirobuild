// SWE02 printed pp.20–23. Death is NOT inferred from incapacitation.
// The GM must explicitly adjudicate fatalities, bounded by actual minion
// losses in a live published combat; the dice executor alone determines Coerce.
import {aliveMinions} from '../rules/adversaries.mjs';
import {spendAction} from '../rules/combat.mjs';
import {skillRoll,validateCheck} from './swe02SocialP241.mjs';
const clone=structuredClone;
const SRC={productCode:'SWE02',printedPage:23,authority:'OFFICIAL_PDF_TEXT_VERIFIED'};
const SCENES={'laoth-new-meen-night-raid':'new-meen-night-raid','laoth-drombb-cantina':'drombb-cantina'};
function context(world,combat){
 const p=world?.publishedAdventure,id=combat?.publishedAdventureContext?.encounterId;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||!combat?.active||!SCENES[id]||p.activeEncounterId!==id||p.currentNodeId!==SCENES[id])throw Error('New Meen: combate publicado real obrigatório.');
 return {p,id};
}
function casualties(a){if(a?.profileKey!=='laoth_drombb_thug'||a.kind!=='minion')throw Error('New Meen: apenas capangas podem contar como baixas.');
 const total=a.membersTotal,per=a.wounds?.perMember,wounds=a.wounds?.current;
 if(!Number.isInteger(total)||total<1||!Number.isInteger(per)||per<1||!Number.isFinite(wounds)||wounds<0)throw Error('New Meen: ficha de capangas inválida.');
 return total-aliveMinions(a);
}
export function adjudicateNewMeenDeaths(world,combat,{groupId,confirmedDeaths}={}){
 const w=clone(world),c=clone(combat),{p,id}=context(w,c);
 if(typeof groupId!=='string'||!groupId.trim()||!Number.isInteger(confirmedDeaths)||confirmedDeaths<1)throw Error('New Meen: confirme grupo e número positivo de mortes.');
 const a=c.adversaries.find(x=>x.id===groupId);if(!a)throw Error('New Meen: grupo não pertence ao combate ativo.');
 const lost=casualties(a),rr=p.newMeenFatalityReceipts||[];
 const prior=rr.filter(x=>x.encounterId===id&&x.groupId===groupId).reduce((sum,x)=>sum+x.confirmedDeaths,0);
 if(prior+confirmedDeaths>lost)throw Error('New Meen: mortes confirmadas excedem baixas comprovadas por dano.');
 const receipt={type:'NEW_MEEN_FATALITY_ADJUDICATION',source:clone(SRC),encounterId:id,nodeId:p.currentNodeId,
  groupId,confirmedDeaths,confirmedTotal:prior+confirmedDeaths,verifiedCasualties:lost,membersTotal:a.membersTotal,
  perMember:a.wounds.perMember,wounds:a.wounds.current,combatRound:c.round,adjudication:'GM_CONFIRMED_FATAL_NOT_MERELY_INCAPACITATED'};
 p.newMeenFatalityReceipts=[...rr,receipt];
 c.log=[...(c.log||[]),{type:'SWE02_NEW_MEEN_FATALITIES_CONFIRMED',round:c.round,groupId,confirmedDeaths,verifiedCasualties:lost}];
 return {world:w,combat:c,receipt};
}
export function verifiedNewMeenDeaths(p,{includeActive=false}={}){
 const receipts=p?.newMeenFatalityReceipts||[];
 const history=p?.encounterHistory||[];
 return receipts.filter(r=>r.encounterId==='laoth-drombb-cantina'&&includeActive||
   r.encounterId==='laoth-new-meen-night-raid'&&history.some(h=>h.encounterId===r.encounterId&&h.combatProof?.groupSnapshots?.some(s=>s.id===r.groupId&&s.wounds>=r.wounds&&s.membersTotal===r.membersTotal&&s.perMember===r.perMember)))
  .reduce((n,r)=>n+r.confirmedDeaths,0);
}
export function coerceNewMeenThugs(world,combat,character,{rng=Math.random}={}){
 const w=clone(world),c=clone(combat),{p,id}=context(w,c);
 if(id!=='laoth-drombb-cantina')throw Error('New Meen: Coerção só na cantina.');
 if(c.encounter?.outcome||!c.encounter?.waitingForPlayer||c.encounter?.activeActorId!=='player')throw Error('New Meen: aguarde a ação real do jogador.');
 if(c.playerTurn?.actionUsed)throw Error('New Meen: ação do turno já consumida.');
 const deaths=verifiedNewMeenDeaths(p,{includeActive:true});if(deaths<1)throw Error('New Meen: sem mortes confirmadas por combate, Coerção não permitida.');
 const difficulty=deaths>=9?1:deaths>=5?2:3;
 const check=skillRoll(character,'coaccion',difficulty,rng);
 const after=spendAction(c,'SWE02: Coerção dos capangas de Drombb');
 const surrenderedGroupIds=[];
 if(check.success){for(const a of after.adversaries){if(a.profileKey==='laoth_drombb_thug'&&a.status==='active'&&aliveMinions(a)>0){a.status='surrendered';surrenderedGroupIds.push(a.id);}}}
 const receipt={type:'NEW_MEEN_COERCION',source:clone(SRC),encounterId:id,nodeId:p.currentNodeId,round:after.round,
  characterKey:String(character?.id||character?.name||''),confirmedDeaths:deaths,difficulty,skillKey:'coaccion',check,
  success:check.success,surrenderedGroupIds};
 p.newMeenCoercionReceipts=[...(p.newMeenCoercionReceipts||[]),receipt];
 after.log=[...(after.log||[]),{type:'SWE02_NEW_MEEN_COERCION',round:after.round,success:check.success,difficulty,confirmedDeaths:deaths,surrenderedGroupIds}];
 return {world:w,combat:after,receipt};
}
export function validateNewMeenP242(p){if(p?.id!=='long-arm-of-the-hutt')return true;
 const deaths=p.newMeenFatalityReceipts||[],coercions=p.newMeenCoercionReceipts||[];
 if(!Array.isArray(deaths)||!Array.isArray(coercions))throw Error('New Meen: recibos inválidos.');
 const groups=new Map(),history=p.encounterHistory||[];
 for(const r of deaths){if(r.type!=='NEW_MEEN_FATALITY_ADJUDICATION'||r.source?.productCode!=='SWE02'||r.source.printedPage!==23||
  SCENES[r.encounterId]!==r.nodeId||!r.groupId||!Number.isInteger(r.confirmedDeaths)||r.confirmedDeaths<1||
  !Number.isInteger(r.verifiedCasualties)||r.verifiedCasualties<r.confirmedDeaths||
  !Number.isInteger(r.membersTotal)||r.membersTotal<1||!Number.isInteger(r.perMember)||r.perMember<1||
  !Number.isFinite(r.wounds)||r.wounds<0||!Number.isInteger(r.combatRound)||r.combatRound<1||
  r.adjudication!=='GM_CONFIRMED_FATAL_NOT_MERELY_INCAPACITATED')throw Error('New Meen: adjudicação de mortes adulterada.');
  const computed=Math.min(r.membersTotal,r.wounds===0?0:Math.floor((r.wounds-1)/r.perMember));
  if(r.verifiedCasualties!==computed)throw Error('New Meen: baixas incompatíveis com ferimentos.');
  const key=r.encounterId+':'+r.groupId,prior=groups.get(key)||0;groups.set(key,prior+r.confirmedDeaths);
  if(r.confirmedTotal!==prior+r.confirmedDeaths||r.confirmedTotal>r.verifiedCasualties)throw Error('New Meen: mortes duplicadas/excessivas.');
  const finished=history.find(h=>h.encounterId===r.encounterId&&h.combatProof?.groupSnapshots);
  if(finished){const s=finished.combatProof.groupSnapshots.find(x=>x.id===r.groupId);
   if(!s||s.profileKey!=='laoth_drombb_thug'||s.membersTotal!==r.membersTotal||s.perMember!==r.perMember||s.wounds<r.wounds)throw Error('New Meen: mortes sem prova final de combate.');}
 }
 for(const r of coercions){if(r.type!=='NEW_MEEN_COERCION'||r.source?.productCode!=='SWE02'||r.source.printedPage!==23||r.encounterId!=='laoth-drombb-cantina'||r.nodeId!=='drombb-cantina'||
  r.skillKey!=='coaccion'||!Number.isInteger(r.confirmedDeaths)||r.confirmedDeaths<1||r.difficulty!==(r.confirmedDeaths>=9?1:r.confirmedDeaths>=5?2:3)||
  !Array.isArray(r.surrenderedGroupIds)||new Set(r.surrenderedGroupIds).size!==r.surrenderedGroupIds.length||r.success!==r.check?.success||!r.characterKey)throw Error('New Meen: coerção adulterada.');
  validateCheck(r.check,{difficulty:r.difficulty,challenge:0,boost:0,setback:0});
  if(!r.success&&r.surrenderedGroupIds.length)throw Error('New Meen: rendição sem sucesso.');
  if(r.confirmedDeaths>deaths.reduce((n,x)=>n+x.confirmedDeaths,0))throw Error('New Meen: mortes inventadas para Coerção.');
 }
 return true;
}

// SWE02 p.23: Drombb's cantina safe contains exactly 2,000 credits.
// A defeated Drombb carries the key; alternatively Hard Skulduggery.
export function openDrombbSafe(world,character,{method='key',rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=w?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||p.currentNodeId!=='drombb-cantina'||!p.flags?.drombbThreatNeutralized)
  throw Error('SWE02: cofre disponível somente após neutralizar Drombb na cantina.');
 const combat=p.encounterHistory?.find(x=>x.encounterId==='laoth-drombb-cantina'&&x.outcome==='adversaries_defeated');
 if(!combat?.combatProof?.eliminated||!combat.combatProof.groupSnapshots?.some(x=>x.profileKey==='laoth_angu_drombb'&&['incapacitated','defeated'].includes(x.status)))
  throw Error('SWE02: sem prova da derrota de Drombb e acesso à chave.');
 if((p.newMeenSafeReceipts||[]).length)throw Error('SWE02: cofre já investigado; recompensa não pode ser repetida.');
 if(!['key','skulduggery'].includes(method))throw Error('SWE02: método do cofre inválido.');
 const check=method==='skulduggery'?skillRoll(c,'actividad_criminal',3,rng):null;
 const success=method==='key'||check.success;
 if(!Number.isSafeInteger(c.credits)||c.credits<0||c.credits>Number.MAX_SAFE_INTEGER-2000)throw Error('SWE02: carteira inválida.');
 if(success)c.credits+=2000;
 const receipt={type:'NEW_MEEN_SAFE',source:clone(SRC),nodeId:p.currentNodeId,method,check,success,
  characterKey:String(c.id||c.name||''),creditsGranted:success?2000:0,creditsRemaining:c.credits,
  combatEncounterId:combat.encounterId};
 p.newMeenSafeReceipts=[receipt];return {world:w,character:c,receipt};
}
export function validateNewMeenSafe(p){if(p?.id!=='long-arm-of-the-hutt')return true;
 const rr=p.newMeenSafeReceipts||[];if(!Array.isArray(rr)||rr.length>1)throw Error('SWE02: cofre duplicado.');
 for(const r of rr){if(r.type!=='NEW_MEEN_SAFE'||r.source?.productCode!=='SWE02'||r.source.printedPage!==23||r.nodeId!=='drombb-cantina'||
   !['key','skulduggery'].includes(r.method)||!r.characterKey||r.combatEncounterId!=='laoth-drombb-cantina'||
   r.success!==(r.method==='key'||r.check?.success)||r.creditsGranted!==(r.success?2000:0)||
   !Number.isSafeInteger(r.creditsRemaining)||r.creditsRemaining<0)throw Error('SWE02: recibo do cofre adulterado.');
  if(r.method==='skulduggery')validateCheck(r.check,{difficulty:3,challenge:0,boost:0,setback:0});
  else if(r.check!==null)throw Error('SWE02: chave não exige rolagem.');
  if(!p.encounterHistory?.some(x=>x.encounterId==='laoth-drombb-cantina'&&x.outcome==='adversaries_defeated'&&x.combatProof?.eliminated))throw Error('SWE02: cofre sem vitória real.');
 }
 return true;
}
