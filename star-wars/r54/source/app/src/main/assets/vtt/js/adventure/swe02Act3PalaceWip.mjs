// SWE02 pp.43–44: gladiator cells and throne-alarm countdown.
// No automatic gladiator loyalty or fabricated barracks headcount.
import {buildPositivePool,rollPool} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {validateCheck} from './swe02SocialP241.mjs';
import {createAdversary,adversaryCharacteristicForSkill,adversarySkillRank} from '../rules/adversaries.mjs';
import {initiativeEntry,buildInitiativeSlots} from '../rules/initiative.mjs';
const clone=structuredClone;
const SOURCE=Object.freeze({productCode:'SWE02',printedPage:43,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const PALACE='long-arm-of-the-hutt';
function active(world,node){const p=world?.publishedAdventure;
 if(p?.id!==PALACE||p.status!=='ACTIVE'||p.currentNodeId!==node)throw Error('SWE02 palácio: cena incompatível.');return p;}
function throne(combat){if(!combat?.active||combat.publishedAdventureContext?.encounterId!=='teemo-palace-guards')throw Error('SWE02 trono: combate publicado ativo obrigatório.');return combat;}
export function recruitSWE02Gladiators(world,character,{method='BRIBE',rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=active(w,'palace-gladiator-cells');
 if(p.activeEncounterId||(p.encounterHistory||[]).some(x=>x.encounterId==='laoth-palace-gladiators'))throw Error('SWE02: gladiadores já em combate ou derrotados.');
 if(p.palaceGladiatorReceipt)throw Error('SWE02: gladiadores já negociados.');
 if(!['BRIBE','COERCE'].includes(method))throw Error('SWE02: apenas suborno ou Coerção funcionam; Charme não tem efeito.');
 const key=String(c.id||c.name||'');if(!key)throw Error('SWE02: personagem não identificado.');
 let check=null,success=true,spent=0;
 if(method==='BRIBE'){
  if(!Number.isSafeInteger(c.credits)||c.credits<1000)throw Error('SWE02 p.43: suborno exige 1.000 créditos reais.');
  c.credits-=1000;spent=1000;
 }else{
  const skill='coaccion',characteristic=c.characteristics?.[SKILLS[skill]];
  if(!Number.isInteger(characteristic)||characteristic<1||!Number.isInteger(c.skills?.[skill]||0))throw Error('SWE02: ficha de Coerção inválida.');
  // Opposed Discipline of p.21 thugs: Willpower 1, no Discipline rank.
  check=rollPool({...buildPositivePool(characteristic,c.skills[skill]||0),difficulty:1,challenge:0,boost:0,setback:0},rng);
  success=check.success;
 }
 const receipt={type:'SWE02_GLADIATORS_RECRUITMENT',source:clone(SOURCE),nodeId:p.currentNodeId,
  characterKey:key,method,check,success,creditsSpent:spent,creditsAfter:c.credits,gladiators:4};
 p.palaceGladiatorReceipt=receipt;
 p.flags={...(p.flags||{}),palaceGladiatorsAllied:success};
 return {world:w,character:c,receipt:clone(receipt)};
}
export function validateSWE02Palace(world){const p=world?.publishedAdventure;
 if(p?.id!==PALACE)return true;
 const r=p.palaceGladiatorReceipt;
 if(r){if(r.type!=='SWE02_GLADIATORS_RECRUITMENT'||r.source?.productCode!=='SWE02'||r.source?.printedPage!==43||r.source?.authority!=='OFFICIAL_PDF_TEXT_VERIFIED'||r.nodeId!=='palace-gladiator-cells'||!r.characterKey||r.gladiators!==4||!['BRIBE','COERCE'].includes(r.method)||r.success!==p.flags?.palaceGladiatorsAllied||!Number.isSafeInteger(r.creditsAfter)||r.creditsAfter<0)throw Error('SWE02: recibo de gladiadores adulterado.');
  if(r.method==='BRIBE'){
   if(r.creditsSpent!==1000||r.check!==null||r.success!==true)throw Error('SWE02: suborno de gladiadores adulterado.');
  }else{
   if(r.creditsSpent!==0||r.check?.success!==r.success)throw Error('SWE02: Coerção de gladiadores adulterada.');
   validateCheck(r.check,{difficulty:1,challenge:0,boost:0,setback:0});
  }
 }else if(p.flags?.palaceGladiatorsAllied)throw Error('SWE02: gladiadores aliados sem recibo.');
 return true;
}
export function queueSWE02ThroneAlarm(combat,{confirmedBarracksCount}={}){
 const c=clone(throne(combat));
 if(!Number.isInteger(confirmedBarracksCount)||confirmedBarracksCount<1||confirmedBarracksCount>12)throw Error('SWE02: Mestre deve confirmar a quantidade real de guardas do quartel.');
 if(c.swe02ThroneAlarm||c.swe02ThroneAlarmQueued)throw Error('SWE02: alarme já solicitado.');
 if(!c.adversaries?.some(x=>x.profileKey==='teemo_the_hutt'&&x.status==='active'))throw Error('SWE02: Teemo não pode apertar o alarme incapacitado.');
 c.swe02ThroneAlarmQueued={confirmedBarracksCount};
 c.log.push({type:'SWE02_THRONE_ALARM_REQUEST',round:c.round,confirmedBarracksCount});
 return c;
}
export function triggerSWE02ThroneAlarmOnNpcTurn(combat,adversaryId){
 const c=clone(combat),q=c.swe02ThroneAlarmQueued;
 if(!q)return c;
 const teemo=c.adversaries?.find(a=>a.id===adversaryId);
 if(teemo?.profileKey!=='teemo_the_hutt'||teemo.status!=='active')return c;
 c.swe02ThroneAlarm={pressedRound:c.round,dueRound:c.round+4,confirmedBarracksCount:q.confirmedBarracksCount,arrived:false};
 delete c.swe02ThroneAlarmQueued;
 c.log.push({type:'SWE02_THRONE_ALARM_PRESSED',round:c.round,actorId:adversaryId,dueRound:c.round+4,maneuverUsed:true});
 return c;
}
export function applySWE02ThroneReinforcementsAtRoundBoundary(combat,{rng=Math.random}={}){
 const c=clone(combat),a=c.swe02ThroneAlarm;
 if(!c.active||!a||a.arrived||c.round<=a.dueRound||c.publishedAdventureContext?.encounterId!=='teemo-palace-guards')return c;
 const enemy=createAdversary('gamorrean_guard',{count:a.confirmedBarracksCount,range:'Curto',name:'Guardas Gamorreanos do quartel — reforço confirmado'});
 // Actual initiative roll, no invented slot score. New arrivals act from the next round.
 const check=rollPool({...buildPositivePool(adversaryCharacteristicForSkill(enemy,'alerta'),adversarySkillRank(enemy,'alerta')),difficulty:0,challenge:0,boost:0,setback:0},rng);
 const entry=initiativeEntry({id:enemy.id,side:'npc',check,label:enemy.name});
 c.adversaries.push(enemy);
 c.encounter.entries.push(entry);
 c.encounter.npcActorIds.push(enemy.id);
 c.encounter.slots=buildInitiativeSlots(c.encounter.entries);
 c.encounter.cursor=0;
 c.encounter.actedActorIds=[];
 c.encounter.activeActorId=null;
 c.encounter.waitingForPlayer=false;
 a.arrived=true;a.arrivalRound=c.round;a.enemyId=enemy.id;a.initiativeCheck=check;
 c.log.push({type:'SWE02_THRONE_REINFORCEMENTS_ARRIVED',round:c.round,enemyId:enemy.id,confirmedBarracksCount:a.confirmedBarracksCount,check});
 return c;
}
export function validateSWE02ThroneAlarm(combat){const a=combat?.swe02ThroneAlarm;
 if(!a)return true;
 if(!Number.isInteger(a.pressedRound)||a.pressedRound<1||a.dueRound!==a.pressedRound+4||!Number.isInteger(a.confirmedBarracksCount)||a.confirmedBarracksCount<1||a.confirmedBarracksCount>12)throw Error('SWE02: alarme do trono adulterado.');
 const pressed=(combat.log||[]).filter(x=>x.type==='SWE02_THRONE_ALARM_PRESSED');
 if(pressed.length!==1||pressed[0].round!==a.pressedRound||pressed[0].dueRound!==a.dueRound||pressed[0].maneuverUsed!==true)throw Error('SWE02: manobra do alarme sem prova.');
 if(a.arrived){const arrived=(combat.log||[]).filter(x=>x.type==='SWE02_THRONE_REINFORCEMENTS_ARRIVED');
  if(arrived.length!==1||arrived[0].enemyId!==a.enemyId||arrived[0].round!==a.arrivalRound||a.arrivalRound<=a.dueRound||!combat.adversaries?.some(x=>x.id===a.enemyId&&x.profileKey==='gamorrean_guard'))throw Error('SWE02: reforços do trono adulterados.');
  validateCheck(a.initiativeCheck,{difficulty:0,challenge:0,boost:0,setback:0});
 }
 return true;
}
