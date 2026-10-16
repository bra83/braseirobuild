/** SWE09 The Jewel of Yavin, printed p.90: Shen's four goons aboard Arrow-7.
 * AFT light repeating blaster is PERSONAL scale (not planetary). This module
 * never invents missing hull, speed, silhouette, chase ticks or vehicle damage.
 * GM confirms target is behind the craft and in the current combat range.
 */
import {adversarySkillRank,adversaryHealthStatus,aliveMinions} from '../rules/adversaries.mjs';
const clone=structuredClone, eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
export const ARROW7_OFFICIAL=Object.freeze({name:'Arrow-7 customizado',manufacturer:'Aratech Repulsor Company',type:'airspeeder civil',crewPilots:1,passengers:6,encumbrance:20,costCredits:12000,rarity:2,hardPoints:2,source:'SWE09 The Jewel of Yavin p.90',scale:'personal',fireArc:'aft'});
export const ARROW7_AFT_BLASTER=Object.freeze({name:'Blaster de repetição leve traseiro do Arrow-7',skill:'artilleria',range:'Extremo',damage:11,crit:3,pierce:1,autofire:true,autoFire:true,scale:'personal',fireArc:'aft'});
const KEYS=['id','kind','contractVersion','encounterId','round','adversaryId','targetRange','gmAftArcConfirmed','gmGunnerConfirmed','source','weapon'];
export function replayJewelArrow7Turret(progress){
 const receipts=progress?.operations?.receipts??{},history=[];
 for(const [id,r] of Object.entries(receipts)){
  if(r?.kind!=='JEWEL_ARROW7_TURRET')continue;
  if(!validId(id)||!r||Object.keys(r).some(k=>!KEYS.includes(k))||r.id!==id||r.contractVersion!==1||r.encounterId!=='arend-shen-four-goons'||!validId(r.adversaryId)||
    !Number.isSafeInteger(r.round)||r.round<1||r.round>100000||!['Curto','Médio','Longo','Extremo'].includes(r.targetRange)||r.gmAftArcConfirmed!==true||r.gmGunnerConfirmed!==true||r.source!==ARROW7_OFFICIAL.source||!eq(r.weapon,ARROW7_AFT_BLASTER))
   throw Error('Recibo do canhão Arrow-7 adulterado.');
  if(history.some(h=>h.encounterId===r.encounterId&&h.round===r.round))throw Error('Canhão Arrow-7 preparado duas vezes na mesma rodada.');
  if(history.length&&r.round<history.at(-1).round)throw Error('Ordem de rodadas do Arrow-7 adulterada.');
  history.push(clone(r));
 }
 return history.length?{contractVersion:1,history}:null;
}
export function validateJewelArrow7Turret(progress){
 const actual=progress?.operations?.arrow7Turret??null,expected=replayJewelArrow7Turret(progress);
 if(!eq(actual,expected))throw Error('Estado do Arrow-7 diverge dos recibos.');return expected;
}
export function queueJewelArrow7Turret(world,combat,details={}){
 const w=clone(world),c=clone(combat),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!c?.active||c?.publishedAdventureContext?.encounterId!=='arend-shen-four-goons')throw Error('Canhão Arrow-7 disponível apenas no encontro oficial ativo.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID de tiro do Arrow-7 duplicado ou inválido.');
 if(details.gmAftArcConfirmed!==true||details.gmGunnerConfirmed!==true)throw Error('Mestre deve confirmar artilheiro e alvo atrás do Arrow-7.');
 const a=c.adversaries?.find(x=>x.id===details.adversaryId&&x.profileKey==='shens_goons');
 if(!a||adversaryHealthStatus(a).incapacitated||aliveMinions(a)<1)throw Error('Grupo de capangas artilheiros ausente ou incapacitado.');
 if(a.range!==details.targetRange||!['Curto','Médio','Longo','Extremo'].includes(a.range))throw Error('Confirme a distância atual real sem movimentação fictícia.');
 if(adversarySkillRank(a,'artilleria')<0)throw Error('Artilheiro sem perícia válida.');
 if(c.encounter?.actedActorIds?.includes(a.id))throw Error('O artilheiro já agiu nesta rodada.');
 if(!Number.isSafeInteger(c.round)||c.round<1)throw Error('Rodada de combate inválida.');
 const previous=validateJewelArrow7Turret(p);
 if(previous?.history.some(x=>x.round===c.round))throw Error('Canhão Arrow-7 já preparado nesta rodada.');
 if(c.arrow7TurretOrder?.status==='QUEUED')throw Error('Há uma ordem Arrow-7 pendente.');
 const receipt={id:details.actionId,kind:'JEWEL_ARROW7_TURRET',contractVersion:1,encounterId:'arend-shen-four-goons',round:c.round,adversaryId:a.id,targetRange:a.range,gmAftArcConfirmed:true,gmGunnerConfirmed:true,source:ARROW7_OFFICIAL.source,weapon:clone(ARROW7_AFT_BLASTER)};
 p.operations.receipts[receipt.id]=receipt;p.operations.arrow7Turret=replayJewelArrow7Turret(p);validateJewelArrow7Turret(p);
 c.arrow7TurretOrder={receiptId:receipt.id,round:c.round,adversaryId:a.id,targetRange:a.range,status:'QUEUED'};
 c.log=[...(c.log??[]),{type:'ARROW7_TURRET_QUEUED',round:c.round,actorId:a.id,receiptId:receipt.id,source:ARROW7_OFFICIAL.source}];
 return {world:w,combat:c,adjudication:receipt};
}
export function arrow7WeaponForNpcTurn(combat,adversary){
 const o=combat?.arrow7TurretOrder;
 if(!o||o.status!=='QUEUED'||o.adversaryId!==adversary?.id)return null;
 if(o.round!==combat.round||o.targetRange!==adversary.range)throw Error('Ordem Arrow-7 vencida ou distância alterada; confirme novamente com o Mestre.');
 if(adversaryHealthStatus(adversary).incapacitated)throw Error('Artilheiro Arrow-7 incapacitado.');
 return clone(ARROW7_AFT_BLASTER);
}
export function consumeJewelArrow7Turret(combat,adversaryId){
 const c=clone(combat),o=c.arrow7TurretOrder;
 if(o?.status==='QUEUED'&&o.adversaryId===adversaryId){
  if(o.round!==c.round)throw Error('Ordem Arrow-7 fora da rodada.');
  c.arrow7TurretOrder={...o,status:'FIRED'};
  c.log=[...(c.log??[]),{type:'ARROW7_TURRET_FIRED',round:c.round,actorId:adversaryId,receiptId:o.receiptId,scale:'personal',fireArc:'aft'}];
 }
 return c;
}
