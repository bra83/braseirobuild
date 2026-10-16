/**
 * SWE09 The Jewel of Yavin, hiring a slicer (official chapter, p. 40).
 * Port Town Streetwise (Average); R3-8C: Intellect/Computers 3/3,
 * owner negotiates 500-1,000 credits (Hard Negotiation); Naboka: 4/5,
 * starts at 3,000 credits and never accepts below 2,000.
 * The PDF gives no price-per-success formula. A GM explicitly approves
 * the final price within the published bounds. No fabricated NPC outcome.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const int=(x,a,b)=>Number.isSafeInteger(x)&&x>=a&&x<=b;
const validId=id=>typeof id==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
export const JEWEL_HIRE_PROFILES=Object.freeze({
 r3_8c:Object.freeze({id:'r3_8c',name:'R3-8C',intellect:3,computers:3,minCredits:500,maxCredits:1000}),
 naboka:Object.freeze({id:'naboka',name:'Naboka',intellect:4,computers:5,minCredits:2000,maxCredits:3000})
});
function rollCheck(character,skill,charKey,difficulty,rng){
 const characteristicRank=character?.characteristics?.[charKey],skillRank=character?.skills?.[skill]??0;
 if(!int(characteristicRank,0,7)||!int(skillRank,0,7))throw Error('Ficha inválida para contratar slicer.');
 const pool={...buildPositivePool(characteristicRank,skillRank),difficulty,challenge:0,boost:0,setback:0};
 return {skill,charKey,characteristicRank,skillRank,pool,roll:rollPool(pool,rng)};
}
function validateRoll(e,skill,charKey,difficulty){
 if(!e||e.skill!==skill||e.charKey!==charKey||!int(e.characteristicRank,0,7)||!int(e.skillRank,0,7))throw Error('Prova da contratação adulterada.');
 const pool={...buildPositivePool(e.characteristicRank,e.skillRank),difficulty,challenge:0,boost:0,setback:0};
 if(!same(pool,e.pool)||!same(pool,e.roll?.pool)||!Array.isArray(e.roll?.faces))throw Error('Pool de contratação adulterado.');
 let i=0;const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
  for(let n=0;n<pool[type];n++){
   const face=e.roll.faces[i++],valid=diceFacesForTests[type];
   if(!face||face.type!==type||!int(face.index,0,valid.length-1)||!same(face.face,valid[face.index]))throw Error('Face de contratação adulterada.');
   for(const [k,v] of Object.entries(face.face))raw[k]+=v;
  }
 }
 if(i!==e.roll.faces.length)throw Error('Dados extras na contratação.');
 const result=resolveSymbols(raw);
 for(const key of ['success','netSuccess','netAdvantage','triumph','despair','raw'])if(!same(result[key],e.roll[key]))throw Error('Resultado de contratação adulterado.');
 return result;
}
export function replayJewelSlicerHire(records){
 if(!Array.isArray(records)||records.length>16)throw Error('Histórico de contratação inválido.');
 const ids=new Set();let hired=null;
 for(const r of records){
  if(!r||r.kind!=='BANK_SLICER_HIRE'||!validId(r.id)||ids.has(r.id)||!Object.hasOwn(JEWEL_HIRE_PROFILES,r.slicerId)||
     typeof r.actorId!=='string'||!r.actorId||r.actorId.length>120||r.gmConfirmed!==true||r.portTownContact!==true)
   throw Error('Recibo de contratação inválido.');
  ids.add(r.id);if(hired)throw Error('Um slicer já foi contratado.');
  const discovery=validateRoll(r.streetwise,'callejeo','astucia',2);
  if(r.discovered!==discovery.success)throw Error('Descoberta de slicer adulterada.');
  if(r.slicerId==='r3_8c'&&discovery.success){
   const negotiation=validateRoll(r.negotiation,'negociacion','presencia',3);
   if(r.negotiated!==negotiation.success)throw Error('Negociação do droid adulterada.');
  }else if(r.negotiation!==null||r.negotiated!==null)throw Error('Negociação sem contato válido.');
  const profile=JEWEL_HIRE_PROFILES[r.slicerId];
  if(!discovery.success){
   if(r.hired!==false||r.creditsSpent!==0||r.creditsAfter!==r.creditsBefore||r.finalPrice!==null)
    throw Error('Falha de busca não pode contratar nem cobrar créditos.');
  }else{
   if(r.hired!==true||!int(r.finalPrice,profile.minCredits,profile.maxCredits)||
      r.creditsSpent!==r.finalPrice||r.creditsAfter!==r.creditsBefore-r.finalPrice)
    throw Error('Preço ou pagamento de slicer adulterado.');
   hired={slicerId:r.slicerId,name:profile.name,intellect:profile.intellect,computers:profile.computers,
    paidCredits:r.finalPrice,hireReceiptId:r.id,actorId:`npc:${r.slicerId}`};
  }
  if(!int(r.creditsBefore,0,100000000)||!int(r.creditsAfter,0,100000000))throw Error('Saldo de contratação inválido.');
 }
 return hired;
}
export function validateJewelSlicerHire(progress){
 const receipts=Object.values(progress?.operations?.receipts||{}).filter(r=>r?.kind==='BANK_SLICER_HIRE');
 const expected=replayJewelSlicerHire(receipts);
 if(!same(progress?.operations?.hiredSlicer??null,expected))throw Error('Slicer contratado não corresponde aos recibos.');
 return true;
}
export function hireJewelSlicer(world,character,{actionId,slicerId,finalPrice,gmConfirmed=false,portTownContact=false,rng=Math.random}={}){
 const w=structuredClone(world||{}),p=w.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||!['port-town-rendezvous','market-row-chase','tourist-district-meeting','bespin-arrival','heist-homework','grand-prix-preparation','grand-prix-race','grand-prix-aftermath','gala-reconnaissance','cloud-nine-party','central-bank-slice'].includes(p.currentNodeId))
  throw Error('Contratação fora da preparação de The Jewel of Yavin.');
 if(!validId(actionId)||!Object.hasOwn(JEWEL_HIRE_PROFILES,slicerId)||gmConfirmed!==true||portTownContact!==true)
  throw Error('Slicer, ação ou contato em Port Town não confirmado.');
 p.operations??={};p.operations.receipts??={};
 if(Object.hasOwn(p.operations.receipts,actionId))throw Error('Contratação já registrada.');
 validateJewelSlicerHire(p);
 if(p.operations.hiredSlicer||p.operations.bank?.attempts>0)throw Error('Não é possível contratar outro slicer ou contratar após iniciar a invasão.');
 const credits=character?.credits;
 if(!int(credits,0,100000000))throw Error('Saldo de créditos inválido.');
 const streetwise=rollCheck(character,'callejeo','astucia',2,rng);
 const discovered=streetwise.roll.success;
 const negotiation=discovered&&slicerId==='r3_8c'?rollCheck(character,'negociacion','presencia',3,rng):null;
 // The book leaves price adjudication to the GM; never infer it from dice.
 const profile=JEWEL_HIRE_PROFILES[slicerId];
 if(discovered&&(!int(finalPrice,profile.minCredits,profile.maxCredits)||credits<finalPrice))
  throw Error('Preço aprovado fora dos limites oficiais ou créditos insuficientes.');
 const spent=discovered?finalPrice:0;
 const receipt={id:actionId,kind:'BANK_SLICER_HIRE',slicerId,actorId:String(character.id??character.name??'jogador'),
  gmConfirmed:true,portTownContact:true,streetwise,discovered,negotiation,negotiated:negotiation?.roll.success??null,
  hired:discovered,finalPrice:discovered?finalPrice:null,creditsBefore:credits,creditsAfter:credits-spent,creditsSpent:spent};
 p.operations.receipts[actionId]=receipt;
 p.operations.hiredSlicer=replayJewelSlicerHire(Object.values(p.operations.receipts).filter(r=>r?.kind==='BANK_SLICER_HIRE'));
 validateJewelSlicerHire(p);
 p.updatedAt=new Date().toISOString();
 return {world:w,character:spent?{...character,credits:credits-spent}:null,
  hiredSlicer:structuredClone(p.operations.hiredSlicer),adjudication:structuredClone(receipt)};
}
