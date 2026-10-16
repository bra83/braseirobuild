/** P227 — Physical chain-of-custody for SWE09's unique Jewel of Yavin.
 * This is an inventory-integrity mechanism, not a newly invented printed reward.
 * Only the active player character and real companions with persistent sheets
 * may hold the item. P228 allows a real companion carrier to board with GM proof.
 */
import {validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
const ITEM='swe09-jewel-of-yavin-gem';
const KIND='JEWEL_CUSTODY_TRANSFER';
const SOURCE='SWE09 The Jewel of Yavin pp.92–95 — physical item custody';
const clone=structuredClone;
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=v=>typeof v==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(v)&&!['__proto__','constructor','prototype'].includes(v);
const pcId=character=>validId(character?.id)?character.id:'player-slot';
const count=character=>(character?.gear??[]).filter(x=>x?.id===ITEM&&x.quantity===1).length;
function entries(p){return Object.entries(p?.operations?.receipts??{});}
function stamp(world){validateTaskTimeLedger(world);const minute=world.clock.elapsedMinutes??0,tick=world.clock.tick;
 if(!Number.isSafeInteger(minute)||minute<0||!Number.isSafeInteger(tick)||tick<0)throw Error('Relógio inválido para custódia.');return {minute,tick};}
export function replayJewelCustodyTransfers(p){
 const all=entries(p),original=all.find(([,r])=>r?.kind==='JEWEL_BESPIN_CUSTODY')?.[1];
 let holderId=original?.holderId??null,transfers=[],previousMinute=original?.atMinute??-1,previousTick=original?.atTick??-1;
 let boarded=false,acquired=false;
 for(const [id,r] of all){
  if(r?.kind==='JEWEL_BESPIN_CUSTODY')acquired=true;
  if(r?.kind==='JEWEL_BESPIN_BOARD'||r?.kind==='JEWEL_BESPIN_ESCAPE')boarded=true;
  if(r?.kind!==KIND)continue;
  if(!original||!acquired||boarded||!validId(id)||r.id!==id||r.kind!==KIND||r.version!==1||r.source!==SOURCE||
   Object.keys(r).some(k=>!['id','kind','version','source','itemId','fromHolderId','toHolderId','atMinute','atTick','gmCoLocatedConfirmed','gmHandOffConfirmed'].includes(k))||
   r.itemId!==ITEM||!validId(r.fromHolderId)||!validId(r.toHolderId)||r.fromHolderId===r.toHolderId||
   r.fromHolderId!==holderId||r.gmCoLocatedConfirmed!==true||r.gmHandOffConfirmed!==true||
   !Number.isSafeInteger(r.atMinute)||r.atMinute<previousMinute||!Number.isSafeInteger(r.atTick)||r.atTick<previousTick)
   throw Error('Cadeia de custódia da Joia adulterada, duplicada ou fora de ordem.');
  holderId=r.toHolderId;previousMinute=r.atMinute;previousTick=r.atTick;transfers.push(clone(r));
 }
 return {holderId,transfers};
}
export function validateJewelCustodyTransfers(p,world=null,character=null,group=null){
 const chain=replayJewelCustodyTransfers(p);
 if(p?.operations?.jewelCustodyTransfers!==undefined&&!same(p.operations.jewelCustodyTransfers,chain))
  throw Error('Estado derivado de custódia não corresponde aos recibos.');
 if(world){const now=stamp(world);for(const r of chain.transfers)
  if(r.atMinute>now.minute||r.atTick>now.tick)throw Error('Transferência da Joia registrada no futuro.');}
 if(character&&group&&chain.holderId){
  const roster=[{id:pcId(character),character},...(group.companions??[]).map(c=>({id:c.id,character:c.character}))];
  if(new Set(roster.map(x=>x.id)).size!==roster.length)throw Error('Identificadores de portadores ambíguos.');
  if(!roster.some(x=>x.id===chain.holderId))throw Error('Portador físico da Joia não existe no grupo.');
  let total=0;
  for(const member of roster){const n=count(member.character);total+=n;
   if(n!==(member.id===chain.holderId?1:0))throw Error('Inventário da Joia diverge da cadeia de custódia.');
   if((member.character?.gear??[]).some(x=>x?.id===ITEM&&x.quantity!==1))throw Error('Quantidade da Joia física inválida.');}
  if(total!==1)throw Error('A Joia deve existir uma única vez.');
 }
 return chain;
}
export function transferJewelCustody(world,combat,vehicles,character,group,details={}){
 const w=clone(world),p=w.publishedAdventure,c=clone(character),g=clone(group);
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
  combat?.active||p.activeEncounterId||vehicles?.combat?.active||!validId(details.actionId)||
  p.operations?.receipts?.[details.actionId]||details.gmCoLocatedConfirmed!==true||details.gmHandOffConfirmed!==true)
  throw Error('Transferência exige cena ativa, ID único e entrega física confirmada pelo Mestre.');
 const chain=validateJewelCustodyTransfers(p,w,c,g),original=entries(p).find(([,r])=>r?.kind==='JEWEL_BESPIN_CUSTODY');
 if(!original||entries(p).some(([,r])=>['JEWEL_BESPIN_BOARD','JEWEL_BESPIN_ESCAPE'].includes(r?.kind)))
  throw Error('A Joia precisa ser adquirida e ainda não embarcada.');
 const roster=[{id:pcId(c),character:c},...(g.companions??[]).map(x=>({id:x.id,character:x.character}))];
 const from=roster.find(x=>x.id===chain.holderId),to=roster.find(x=>x.id===details.toHolderId);
 if(!from||!to||from===to||count(from.character)!==1||count(to.character)!==0)
  throw Error('Transferência exige portador atual real e destinatário sem a Joia.');
 const [item]=from.character.gear.filter(x=>x.id===ITEM);
 from.character.gear=from.character.gear.filter(x=>x.id!==ITEM);
 to.character.gear??=[];to.character.gear.push(clone(item));
 const now=stamp(w);
 const receipt={id:details.actionId,kind:KIND,version:1,source:SOURCE,itemId:ITEM,
  fromHolderId:from.id,toHolderId:to.id,atMinute:now.minute,atTick:now.tick,
  gmCoLocatedConfirmed:true,gmHandOffConfirmed:true};
 p.operations.receipts[receipt.id]=clone(receipt);
 p.operations.jewelCustodyTransfers=replayJewelCustodyTransfers(p);
 validateJewelCustodyTransfers(p,w,c,g);
 return {world:w,character:c,group:g,adjudication:receipt,custody:clone(p.operations.jewelCustodyTransfers)};
}
