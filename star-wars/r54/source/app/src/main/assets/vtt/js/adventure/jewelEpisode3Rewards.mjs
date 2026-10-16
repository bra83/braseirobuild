/** SWE09 The Jewel of Yavin, printed p.95, Episode III XP Rewards.
 * Only the milestones backed by persisted encounter/scene receipts are payable.
 * A receipt is bound to the real PC and applied to the real progression ledger.
 * Other official rewards (cloud cars and Bespin escape) remain locked until
 * their corresponding vehicle/travel outcomes exist in the runtime.
 */
import {grantExperience} from '../rules/progression.mjs';
import {validateJewelShenAftermath} from './jewelShenAftermath.mjs';
import {validateJewelHoloPark} from './jewelHoloPark.mjs';
import {validateJewelArendProperty} from './jewelArendProperty.mjs';
import {validateJewelStormIvPatrol} from './jewelStormIvPatrol.mjs';
import {validateJewelBespinDeparture} from './jewelBespinDeparture.mjs';
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, p.95, Episode III XP Rewards';
const KIND='JEWEL_EP3_XP';
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export const JEWEL_EP3_REWARDS=Object.freeze({
 HOLO_PARK_ESCAPE:Object.freeze({xp:10,label:'Escapar dos perseguidores no Holo-Park'}),
 ARIS_ALLIANCE:Object.freeze({xp:5,label:'Firmar aliança com Aris Shen'}),
 AREND_DEFEATED:Object.freeze({xp:15,label:'Localizar e derrotar Arend Shen'}),
 CLOUD_CAR_PATROLS:Object.freeze({xp:5,label:'Derrotar ou evadir patrulhas de cloud cars'}),
 BESPIN_ESCAPE:Object.freeze({xp:5,label:'Escapar de Bespin com a Joia de Yavin'})
});
const FIELDS=['id','kind','version','source','milestone','xp','characterId','proofReceiptId','beforeTotal','beforeAvailable','afterTotal','afterAvailable','gmAwardConfirmed'];
function proofFor(p,milestone){
 if(milestone==='HOLO_PARK_ESCAPE'){
  const park=validateJewelHoloPark(p);
  if(park?.status!=='ESCAPED'||park.endReceipt?.outcome!=='ESCAPED')throw Error('A fuga real do Holo-Park ainda não foi confirmada.');
  return park.endReceipt.id;
 }
 if(milestone==='ARIS_ALLIANCE'){
  const state=validateJewelShenAftermath(p);
  if(!state?.aris?.allied)throw Error('Aliança real com Aris ainda não foi registrada.');
  return state.aris.id;
 }
 if(milestone==='CLOUD_CAR_PATROLS'){
  const patrol=validateJewelStormIvPatrol(p);
  if(!patrol?.cleared||patrol.outcomes.length!==4)throw Error('Quatro cloud cars ainda não foram derrotados ou evadidos em combate real.');
  return patrol.cleared.id;
 }
 if(milestone==='BESPIN_ESCAPE'){
  const departure=validateJewelBespinDeparture(p);
  if(!departure?.escape)throw Error('Fuga real de Bespin com a Joia ainda não comprovada.');
  return departure.escape.id;
 }
 if(milestone==='AREND_DEFEATED'){
  const state=validateJewelShenAftermath(p);
  if(!['DEAD','INCARCERATED'].includes(state?.arend?.status))throw Error('Arend ainda não foi derrotado ou capturado.');
  const property=validateJewelArendProperty(p);
  const actual=property?.stages?.find(x=>x.stage==='arend');
  if(!actual||!p.encounterHistory?.some(x=>x.encounterId===actual.encounterId&&x.outcome==='adversaries_defeated'))
   throw Error('O combate real contra Arend precisa terminar com derrota registrada antes dos 15 PE.');
  return state.arend.id;
 }
 throw Error('Marco de experiência não homologado para concessão.');
}
export function replayJewelEpisode3Rewards(p){
 const awards=[];const seen=new Set();
 for(const [id,r] of Object.entries(p?.operations?.receipts??{})){
  if(r?.kind!==KIND)continue;
  if(p?.id!=='jewel-of-yavin'||!ID(id)||r.id!==id||r.kind!==KIND||r.version!==1||r.source!==SOURCE||
    Object.keys(r).some(k=>!FIELDS.includes(k))||!JEWEL_EP3_REWARDS[r.milestone]||r.xp!==JEWEL_EP3_REWARDS[r.milestone].xp||
    !ID(r.characterId)||r.gmAwardConfirmed!==true||r.proofReceiptId!==proofFor(p,r.milestone)||
    ![r.beforeTotal,r.beforeAvailable,r.afterTotal,r.afterAvailable].every(x=>Number.isSafeInteger(x)&&x>=0)||
    r.afterTotal!==r.beforeTotal+r.xp||r.afterAvailable!==r.beforeAvailable+r.xp||
    seen.has(r.characterId+'|'+r.milestone))throw Error('Recibo de experiência SWE09 adulterado ou duplicado.');
  seen.add(r.characterId+'|'+r.milestone);awards.push(clone(r));
 }
 return awards;
}
export function validateJewelEpisode3Rewards(p){
 const expected=replayJewelEpisode3Rewards(p);
 if(!equal(expected,p?.operations?.episode3Rewards??[]))throw Error('Experiência SWE09 diverge dos recibos.');
 return expected;
}
export function grantJewelEpisode3Reward(world,combat,character,details={}){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||!['ACTIVE','COMPLETED'].includes(p.status)||combat?.active||p.activeEncounterId)
  throw Error('Experiência exige a aventura ativa/concluída e nenhum combate pendente.');
 const characterId=ID(character?.id)?character.id:'player-slot';
 if(!ID(details.actionId)||p.operations?.receipts?.[details.actionId]||!ID(characterId)||
    details.gmAwardConfirmed!==true)throw Error('Confirmação do Mestre, personagem e ID único são obrigatórios.');
 const milestone=details.milestone,rule=JEWEL_EP3_REWARDS[milestone];
 if(!rule)throw Error('Recompensa sem gatilho comprovado ou ainda não implementada.');
 if(validateJewelEpisode3Rewards(p).some(r=>r.milestone===milestone&&r.characterId===characterId))
  throw Error('Este marco já concedeu PE ao personagem; não repetir.');
 const proofReceiptId=proofFor(p,milestone);
 const before=character.xp;
 if(!before||!Number.isSafeInteger(before.total)||!Number.isSafeInteger(before.available)||before.total<0||before.available<0)
  throw Error('Ficha real sem controle de PE válido.');
 const progressed=grantExperience(character,rule.xp,{reason:rule.label+' (SWE09 p.95)'}).character;
 const r={id:details.actionId,kind:KIND,version:1,source:SOURCE,milestone,xp:rule.xp,characterId,
  proofReceiptId,beforeTotal:before.total,beforeAvailable:before.available,
  afterTotal:progressed.xp.total,afterAvailable:progressed.xp.available,gmAwardConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.episode3Rewards=replayJewelEpisode3Rewards(p);
 validateJewelEpisode3Rewards(p);
 return {world:w,character:progressed,adjudication:r,awards:clone(p.operations.episode3Rewards)};
}
