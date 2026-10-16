// SWE02 pp.20-21 (PDF pp.19-20): non-victory New Meen evacuation and
// GM-curated allied Twi'lek support. No unsupported encounter is generated.
import {advanceLivingWorld} from '../world/sandbox.mjs';
import {aliveMinions} from '../rules/adversaries.mjs';
const clone=structuredClone;
const SOURCE=Object.freeze({productCode:'SWE02',printedPages:[20,21],authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
function active(world,nodes){const p=world?.publishedAdventure;if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||!nodes.includes(p.currentNodeId))throw Error('SWE02: cena de New Meen incompatível.');return p;}
function log(p,type,extra){p.newMeenEscortLog=Array.isArray(p.newMeenEscortLog)?p.newMeenEscortLog:[];const r={type,source:clone(SOURCE),nodeId:p.currentNodeId,...extra};p.newMeenEscortLog.push(r);return r;}
function tick(w){const t=w?.clock?.tick;if(!Number.isSafeInteger(t)||t<0)throw Error('SWE02: relógio mundial inválido.');return t;}
function normalizeLegacyLoans(m){if(!m?.loanedWeapons?.some(x=>!x.status))return;
 if((m.returned||0)!==0||m.loanedWeapons.some(x=>x.status))throw Error('SWE02: empréstimos antigos incompatíveis.');
 m.loanedWeapons.forEach((x,i)=>{x.status=i<m.losses?'LOST':'ACTIVE';});
}
export function prepareNewMeenEscort(world){
 const w=clone(world),p=active(w,['new-meen-evacuation']);if(p.newMeenEscort)throw Error('SWE02: escolta já organizada.');
 if(p.flags?.newMeenOutcome==='SECURED'||p.flags?.drombbThreatNeutralized)throw Error('SWE02: a escolta é uma alternativa à libertação, não outra vitória.');
 const escort={status:'ASSEMBLED',adults:20,childrenCount:null,childrenDescription:'um punhado de crianças',
   origin:'laoth.loc.new_meen',destination:'laoth.loc.nabat',startedAtTick:tick(w),departureTick:null,arrivalTick:null,
   gmApprovedTravelTicks:null,threatsResolved:[],significantFavor:false};
 p.newMeenEscort=escort;const receipt=log(p,'NEW_MEEN_ESCORT_ASSEMBLED',{adults:20,childrenCount:null,tick:tick(w)});
 return {world:w,receipt,escort:clone(escort)};
}
export function departNewMeenEscort(world,{travelTicks}={}){
 const w=clone(world),p=active(w,['new-meen-evacuation']),e=p.newMeenEscort;
 if(e?.status!=='ASSEMBLED')throw Error('SWE02: organize a escolta antes de partir.');
 if(!Number.isSafeInteger(travelTicks)||travelTicks<1||travelTicks>96)throw Error('SWE02: Mestre deve aprovar 1–96 ticks para esta rota (PDF não fixa duração).');
 e.status='EN_ROUTE';e.gmApprovedTravelTicks=travelTicks;e.departureTick=tick(w);
 const receipt=log(p,'NEW_MEEN_ESCORT_DEPARTED',{tick:tick(w),gmApprovedTravelTicks:travelTicks});
 return {world:w,receipt,escort:clone(e)};
}
export function arriveNewMeenEscort(world){
 const w=clone(world),p=active(w,['new-meen-evacuation']),e=p.newMeenEscort;
 if(e?.status!=='EN_ROUTE')throw Error('SWE02: a escolta não está em trânsito.');
 // A route's exact duration is not given in SWE02. The GM's chosen number
 // of sandbox ticks is applied through the EXISTING living-world executor.
 const elapsed=Math.max(0,tick(w)-e.departureTick),remaining=Math.max(0,e.gmApprovedTravelTicks-elapsed);
 const next=remaining?advanceLivingWorld(w,{ticks:remaining}).world:w;
 const np=next.publishedAdventure,ne=np.newMeenEscort;ne.status='ARRIVED';ne.arrivalTick=tick(next);ne.significantFavor=true;
 np.flags={...np.flags,newMeenEscortArrived:true};
 const receipt=log(np,'NEW_MEEN_ESCORT_ARRIVED',{tick:tick(next),adults:20,elapsedTicks:ne.arrivalTick-ne.departureTick,
  favor:'BURA_BAN_SIGNIFICANT_FAVOR_NOT_NEW_MEEN_LIBERATION'});
 return {world:next,receipt,escort:clone(ne),timeAdvancedTicks:remaining};
}
export function abandonNewMeenEscort(world){
 const w=clone(world),p=active(w,['new-meen-evacuation']),e=p.newMeenEscort;
 if(e?.status==='ARRIVED'||e?.status==='ABANDONED')throw Error('SWE02: escolta já encerrada.');
 if(!e){p.newMeenEscort={status:'ABANDONED',adults:20,childrenCount:null,origin:'laoth.loc.new_meen',destination:'laoth.loc.nabat',
  startedAtTick:tick(w),departureTick:null,arrivalTick:null,gmApprovedTravelTicks:null,significantFavor:false};}
 else{e.status='ABANDONED';e.significantFavor=false;}
 p.flags={...p.flags,newMeenEscortAbandoned:true};
 const receipt=log(p,'NEW_MEEN_ESCORT_ABANDONED',{tick:tick(w),significantFavor:false});
 return {world:w,receipt};
}
export function armNewMeenMilitia(world,character,{weaponIndex}={}){
 const w=clone(world),c=clone(character),p=active(w,['new-meen-miners','new-meen-night-raid','drombb-cantina']);
 if(!Number.isInteger(weaponIndex)||weaponIndex<0||weaponIndex>=c?.weapons?.length)throw Error('SWE02: selecione uma arma própria existente.');
 if(p.activeEncounterId)throw Error('SWE02: empréstimo deve ocorrer antes do encontro.');
 const weapon=c.weapons[weaponIndex];if(!weapon||!Number.isFinite(weapon.damage)||weapon.damage<1||
  !/blaster|pistola/i.test(String(weapon.name||''))||!['armas_distancia_ligeras','armas_distancia_pesadas'].includes(weapon.skill))
  throw Error('SWE02: milícia requer arma blaster apropriada, não uma concessão sem ficha.');
 const m=p.newMeenMilitia||{members:0,losses:0,profileKey:'laoth_drombb_thug',control:'PLAYER_APPROVED',loanedWeapons:[],roundEffects:[]};
 normalizeLegacyLoans(m);
 if(m.loanedWeapons.length>=20)throw Error('SWE02: não há mais adultos disponíveis em New Meen.');
 if(weapon.quantity>1)c.weapons[weaponIndex].quantity--;else c.weapons.splice(weaponIndex,1);
 const loanId=`new-meen-weapon-${m.loanedWeapons.length+1}`;
 m.loanedWeapons.push({loanId,weapon:clone({...weapon,quantity:1}),ownerKey:String(c.id||c.name||''),status:'ACTIVE',source:clone(SOURCE)});
 m.members++;p.newMeenMilitia=m;
 const receipt=log(p,'NEW_MEEN_MILITIA_ARMED',{loanId,ownerKey:String(c.id||c.name||''),weaponName:weapon.name,armedMembers:m.members,losses:m.losses});
 return {world:w,character:c,receipt,militia:clone(m)};
}
export function applyNewMeenMilitiaAbstractRound(world,combat,{targetGroupId}={}){
 const w=clone(world),c=clone(combat),p=active(w,['new-meen-night-raid','drombb-cantina']),m=p.newMeenMilitia;
 if(!c?.active||!p.activeEncounterId||c.publishedAdventureContext?.encounterId!==p.activeEncounterId||
   !['laoth-new-meen-night-raid','laoth-drombb-cantina'].includes(p.activeEncounterId))throw Error('SWE02: combate publicado de New Meen obrigatório.');
 if(c.encounter?.outcome)throw Error('SWE02: não há apoio após desfecho do encontro.');
 if(!m||m.members<1)throw Error('SWE02: não há milicianos armados aptos a ajudar.');
 normalizeLegacyLoans(m);
 if(m.roundEffects.some(r=>r.encounterId===p.activeEncounterId&&r.round===c.round))throw Error('SWE02: milícia já atuou nesta rodada.');
 const a=c.adversaries?.find(x=>x.id===targetGroupId);
 if(!a||a.profileKey!=='laoth_drombb_thug'||a.status!=='active'||aliveMinions(a)<1)throw Error('SWE02: alvo deve ser grupo hostil ativo de Drombb.');
 const before=a.wounds.current,armedCount=m.members;
 // SWE02 printed p.21, optional ABSTRACTED COMBAT method: one wound per
 // allied militiaman, and one ally eliminated by return fire per round.
 a.wounds.current=Math.min(a.membersTotal*a.wounds.perMember+1,before+armedCount);
 if(aliveMinions(a)===0)a.status='incapacitated';
 // An eliminated ally is not automatically dead. Its borrowed weapon stays
 // in custody and is never silently restored to the PC inventory.
 const casualty=m.loanedWeapons.find(x=>(x.status||'ACTIVE')==='ACTIVE');
 if(!casualty)throw Error('SWE02: custódia dos equipamentos da milícia inconsistente.');
 casualty.status='LOST';m.members--;m.losses++;
 const effect={encounterId:p.activeEncounterId,round:c.round,targetGroupId,armedAtStart:armedCount,
  woundsBefore:before,woundsAfter:a.wounds.current,woundsInflicted:a.wounds.current-before,alliedLosses:1,
  casualtyLoanId:casualty.loanId,method:'OFFICIAL_OPTIONAL_ABSTRACTED_COMBAT_PLAYER_APPROVED',source:clone(SOURCE)};
 m.roundEffects.push(effect);c.log=[...(c.log||[]),{type:'SWE02_TWILEK_MILITIA_ABSTRACT_ROUND',...clone(effect)}];
 const receipt=log(p,'NEW_MEEN_MILITIA_ROUND',{encounterId:p.activeEncounterId,round:c.round,
  targetGroupId,woundsInflicted:effect.woundsInflicted,alliedLosses:1});
 return {world:w,combat:c,receipt,effect};
}
// SWE02 p.21: equipment loan is a real custody transfer, not a free copy.
// A surviving ally may return the same weapon when no battle is active.
// A fallen ally's weapon requires a separate, as-yet-uncertified recovery
// procedure; no automatic loot or recovery is permitted.
export function returnNewMeenMilitiaWeapon(world,character,{loanId}={}){
 const w=clone(world),c=clone(character),p=active(w,['new-meen-miners','new-meen-night-raid','drombb-cantina']);
 const m=p.newMeenMilitia;
 if(!m||!Array.isArray(m.loanedWeapons))throw Error('SWE02: não há empréstimos da milícia.');
 if(typeof loanId!=='string'||!/^new-meen-weapon-[1-9]\d*$/.test(loanId))throw Error('SWE02: recibo de empréstimo inválido.');
 const loan=m.loanedWeapons.find(x=>x.loanId===loanId);
 if(!loan)throw Error('SWE02: arma não consta dos empréstimos.');
 if(loan.ownerKey!==String(c?.id||c?.name||''))throw Error('SWE02: apenas o proprietário pode receber sua arma.');
 // P243 legacy saves may have no per-loan status. Reconstruct casualties
 // deterministically from the number of documented abstract rounds.
 normalizeLegacyLoans(m);
 if(loan.status==='LOST')throw Error('SWE02: arma do aliado eliminado não foi recuperada.');
 if(loan.status==='RETURNED')throw Error('SWE02: arma já devolvida.');
 if(loan.status!=='ACTIVE'||m.members<1)throw Error('SWE02: custódia inválida.');
 if(!Array.isArray(c.weapons))throw Error('SWE02: inventário inválido.');
 c.weapons.push(clone(loan.weapon));
 loan.status='RETURNED';loan.returnedAtTick=tick(w);m.members--;m.returned=(m.returned||0)+1;
 const receipt=log(p,'NEW_MEEN_MILITIA_WEAPON_RETURNED',{loanId,ownerKey:loan.ownerKey,weaponName:loan.weapon.name,tick:tick(w)});
 return {world:w,character:c,receipt,loanId,returnedCount:m.returned};
}
export function validateNewMeenP243(world){
 const p=world?.publishedAdventure;if(p?.id!=='long-arm-of-the-hutt')return true;
 const logItems=p.newMeenEscortLog||[];if(!Array.isArray(logItems))throw Error('SWE02: log de escolta inválido.');
 for(const r of logItems)if(r.source?.productCode!=='SWE02'||JSON.stringify(r.source.printedPages)!=='[20,21]'||
   !['NEW_MEEN_ESCORT_ASSEMBLED','NEW_MEEN_ESCORT_DEPARTED','NEW_MEEN_ESCORT_ARRIVED','NEW_MEEN_ESCORT_ABANDONED','NEW_MEEN_MILITIA_ARMED','NEW_MEEN_MILITIA_ROUND','NEW_MEEN_MILITIA_WEAPON_RETURNED'].includes(r.type))throw Error('SWE02: recibo de escolta adulterado.');
 const e=p.newMeenEscort;
 if(e){if(!['ASSEMBLED','EN_ROUTE','ARRIVED','ABANDONED'].includes(e.status)||e.adults!==20||e.childrenCount!==null||
   e.origin!=='laoth.loc.new_meen'||e.destination!=='laoth.loc.nabat'||!Number.isSafeInteger(e.startedAtTick)||e.startedAtTick<0)
   throw Error('SWE02: estado de evacuação adulterado.');
  const types=logItems.map(r=>r.type).filter(t=>t.startsWith('NEW_MEEN_ESCORT_'));
  const ordered=types.join(',');
  const validSequences=['NEW_MEEN_ESCORT_ABANDONED','NEW_MEEN_ESCORT_ASSEMBLED',
   'NEW_MEEN_ESCORT_ASSEMBLED,NEW_MEEN_ESCORT_ABANDONED',
   'NEW_MEEN_ESCORT_ASSEMBLED,NEW_MEEN_ESCORT_DEPARTED',
   'NEW_MEEN_ESCORT_ASSEMBLED,NEW_MEEN_ESCORT_DEPARTED,NEW_MEEN_ESCORT_ABANDONED',
   'NEW_MEEN_ESCORT_ASSEMBLED,NEW_MEEN_ESCORT_DEPARTED,NEW_MEEN_ESCORT_ARRIVED'];
  if(!validSequences.includes(ordered))throw Error('SWE02: sequência de escolta adulterada.');
  if((e.status==='ARRIVED')!==types.includes('NEW_MEEN_ESCORT_ARRIVED')||
     (e.status==='ABANDONED')!==types.includes('NEW_MEEN_ESCORT_ABANDONED')||
     (e.status==='EN_ROUTE')!==types.includes('NEW_MEEN_ESCORT_DEPARTED')&&e.status!=='ARRIVED'&&e.status!=='ABANDONED')
    throw Error('SWE02: fase de escolta incompatível com recibos.');
  if(e.status==='ARRIVED'){
   if(e.significantFavor!==true||p.flags?.newMeenEscortArrived!==true||p.flags?.newMeenOutcome==='SECURED'||
    !Number.isSafeInteger(e.gmApprovedTravelTicks)||e.gmApprovedTravelTicks<1||e.gmApprovedTravelTicks>96||
    !Number.isSafeInteger(e.departureTick)||!Number.isSafeInteger(e.arrivalTick)||
    e.arrivalTick-e.departureTick<e.gmApprovedTravelTicks||tick(world)<e.arrivalTick||
    logItems.filter(r=>r.type==='NEW_MEEN_ESCORT_ARRIVED').length!==1)
    throw Error('SWE02: chegada sem viagem comprovada.');
  }else if(p.flags?.newMeenEscortArrived===true)throw Error('SWE02: favor de escolta sem chegada.');
  if(e.status==='ABANDONED'&&(e.significantFavor!==false||p.flags?.newMeenEscortAbandoned!==true))throw Error('SWE02: abandono adulterado.');
 }else if(p.flags?.newMeenEscortArrived||p.flags?.newMeenEscortAbandoned)throw Error('SWE02: bandeira de escolta sem registro.');
 const m=p.newMeenMilitia;
 if(m){if(!Number.isInteger(m.members)||!Number.isInteger(m.losses)||m.members<0||m.losses<0||
   !Array.isArray(m.loanedWeapons)||!Array.isArray(m.roundEffects)||m.members+m.losses+(m.returned||0)!==m.loanedWeapons.length||
   m.loanedWeapons.length>20||m.profileKey!=='laoth_drombb_thug'||m.control!=='PLAYER_APPROVED')throw Error('SWE02: efetivo da milícia adulterado.');
  const loanIds=new Set();
  for(const loan of m.loanedWeapons){
   if(!loan.loanId||loanIds.has(loan.loanId)||loan.source?.productCode!=='SWE02'||!loan.weapon?.name||!loan.ownerKey||
     (loan.status&&!['ACTIVE','LOST','RETURNED'].includes(loan.status)))throw Error('SWE02: arma emprestada adulterada.');
   loanIds.add(loan.loanId);
  }
  const marked=m.loanedWeapons.filter(x=>x.status);
  if(marked.length){
   if(marked.length!==m.loanedWeapons.length||marked.filter(x=>x.status==='ACTIVE').length!==m.members||
     marked.filter(x=>x.status==='LOST').length!==m.losses||
     marked.filter(x=>x.status==='RETURNED').length!==(m.returned||0))throw Error('SWE02: custódia da milícia adulterada.');
  }else if((m.returned||0)!==0)throw Error('SWE02: devolução sem custódia.');
  const armedReceipts=logItems.filter(x=>x.type==='NEW_MEEN_MILITIA_ARMED');
  if(armedReceipts.length!==m.loanedWeapons.length||new Set(armedReceipts.map(x=>x.loanId)).size!==armedReceipts.length||
     armedReceipts.some(x=>!m.loanedWeapons.some(l=>l.loanId===x.loanId&&l.weapon.name===x.weaponName&&(!x.ownerKey||l.ownerKey===x.ownerKey))))
    throw Error('SWE02: recibos de empréstimo adulterados.');
  const returnedReceipts=logItems.filter(x=>x.type==='NEW_MEEN_MILITIA_WEAPON_RETURNED');
  if(returnedReceipts.length!==(m.returned||0)||new Set(returnedReceipts.map(x=>x.loanId)).size!==returnedReceipts.length||
     returnedReceipts.some(x=>!m.loanedWeapons.some(l=>l.loanId===x.loanId&&l.status==='RETURNED'&&l.ownerKey===x.ownerKey)))
    throw Error('SWE02: recibos de devolução adulterados.');
  const seen=new Set();for(const r of m.roundEffects){const key=`${r.encounterId}:${r.round}`;if(seen.has(key)||
   !['laoth-new-meen-night-raid','laoth-drombb-cantina'].includes(r.encounterId)||!Number.isInteger(r.round)||r.round<1||
   !Number.isInteger(r.armedAtStart)||r.armedAtStart<1||r.woundsInflicted<0||r.woundsInflicted>r.armedAtStart||
   r.woundsAfter-r.woundsBefore!==r.woundsInflicted||r.alliedLosses!==1||
   r.method!=='OFFICIAL_OPTIONAL_ABSTRACTED_COMBAT_PLAYER_APPROVED')throw Error('SWE02: efeito de milícia adulterado.');seen.add(key);}
  if(m.losses!==m.roundEffects.length)throw Error('SWE02: perdas da milícia incompatíveis com rodadas.');
 }
 return true;
}
