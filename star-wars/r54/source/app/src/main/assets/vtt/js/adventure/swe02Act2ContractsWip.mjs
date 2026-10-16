// SWE02 official printed pp.33 and 37. These are deterministic transactions;
// neither narrative text nor an unverified encounter flag can mint a passage.
import {buildPositivePool,rollPool} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {validateCheck} from './swe02SocialP241.mjs';
import {spendAction} from '../rules/combat.mjs';
import {applyAdversaryDamage} from '../rules/adversaries.mjs';
const clone=structuredClone;
const ID='long-arm-of-the-hutt';
const src=page=>({productCode:'SWE02',printedPage:page,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
function progress(world,node){const p=world?.publishedAdventure;if(p?.id!==ID||p.status!=='ACTIVE'||p.currentNodeId!==node)throw Error('SWE02: ação fora da cena publicada.');return p;}
function who(character){const key=String(character?.id||character?.name||'').trim();if(!key)throw Error('SWE02: personagem sem identidade.');return key;}
function priceWallet(character,amount){if(!Number.isSafeInteger(character?.credits)||character.credits<amount)throw Error('SWE02: créditos insuficientes para a transação.');}
function validateSWE02Check(check,expected){validateCheck(check,expected);}

// Four normal questions; a fifth is a final warning. Trade interest prevents
// Piddock from terminating the conversation. No answer is manufactured.
export function askSWE02Piddock(world,{topic='GERAL',expressTradeInterest=false}={}){
 const w=clone(world),p=progress(w,'piddock-party');
 if(p.activeEncounterId)throw Error('SWE02: não conversar durante combate.');
 if(typeof topic!=='string'||!topic.trim()||topic.length>100)throw Error('SWE02: pergunta inválida.');
 const questions=p.piddockQuestionReceipts||[];
 if(p.piddockConversationClosed)throw Error('SWE02 p.33: Piddock encerrou a conversa.');
 if(questions.length>=5&&!p.piddockTradeInterest)throw Error('SWE02 p.33: Piddock não responde mais sem interesse comercial.');
 if(expressTradeInterest&&!p.piddockTradeInterest){p.piddockTradeInterest=true;p.piddockTradeReceipt={type:'PIDDOCK_TRADE_INTEREST',source:src(33),nodeId:'piddock-party',afterOrdinal:questions.length};}
 const ordinal=questions.length+1,warning=ordinal===4&&!p.piddockTradeInterest;
 const closed=ordinal>=5&&!p.piddockTradeInterest;
 const receipt={type:'PIDDOCK_QUESTION',source:src(33),nodeId:'piddock-party',topic:topic.trim(),ordinal,
  tradeInterest:Boolean(p.piddockTradeInterest),warning,conversationClosed:closed};
 p.piddockQuestionReceipts=[...questions,receipt];p.piddockConversationClosed=closed;
 return {world:w,receipt};
}
export function showSWE02PiddockTradeInterest(world){const w=clone(world),p=progress(w,'piddock-party');
 if(p.piddockConversationClosed)throw Error('SWE02: a conversa já terminou.');
 if(p.piddockTradeInterest)throw Error('SWE02: interesse comercial já registrado.');
 p.piddockTradeInterest=true;p.piddockTradeReceipt={type:'PIDDOCK_TRADE_INTEREST',source:src(33),nodeId:'piddock-party',afterOrdinal:(p.piddockQuestionReceipts||[]).length};
 return {world:w,receipt:clone(p.piddockTradeReceipt)};
}
export function hireSWE02Vio(world,character,{partySize,offer='WEX',rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=progress(w,'piddock-party');
 if(p.activeEncounterId||p.vioHireReceipt)throw Error('SWE02: contratação dos Vio indisponível ou duplicada.');
 if(!['WEX','ORPA','NEGOTIATE'].includes(offer))throw Error('SWE02: oferta dos Vio inválida.');
 const size=Number(partySize),confirmed=Number(p.flags?.confirmedPartySize);
 if(!Number.isInteger(size)||size<1||size>8||(Number.isInteger(confirmed)&&confirmed>0&&confirmed!==size))throw Error('SWE02: número real de passageiros precisa ser confirmado.');
 const key=who(c);let check=null,perPerson=offer==='ORPA'?1000:500;
 if(offer==='NEGOTIATE'){
  const characteristic=c.characteristics?.[SKILLS.negociacion];
  if(!Number.isInteger(characteristic)||characteristic<1)throw Error('SWE02: ficha inválida para Negociação.');
  const pool={...buildPositivePool(characteristic,c.skills?.negociacion||0),difficulty:2,challenge:0,boost:0,setback:0};
  check=rollPool(pool,rng);if(check.success)perPerson=250;
 }
 const total=perPerson*size;priceWallet(c,total);c.credits-=total;
 const receipt={type:'VIO_PASSAGE_HIRED',source:src(33),nodeId:'piddock-party',characterKey:key,
  offer,partySize:size,perPerson,creditsSpent:total,creditsRemaining:c.credits,check,
  route:'DIRECT_TO_LUCKY_GUESS',shipOwnership:'ORPA_AND_WEX_VIO',skipDimmockAndGantry:true};
 p.vioHireReceipt=receipt;p.flags={...(p.flags||{}),vioHired:true};
 return {world:w,character:c,receipt};
}

// SWE02 printed p.33: a spoken arms deal is not a payment. Only the
// freighter-side agent receives credits and transfers real inventory.
// The three combat profiles use the published p.33 "otherwise normal"
// clause; pistol/rifle are also explicitly statted on p.37.
export const PIDDOCK_WEAPONS=Object.freeze([
 {id:'geo-heavy-pistol',name:'Pistola blaster pesada geonosiana',price:1100,skill:'armas_distancia_ligeras',damage:7,crit:2,range:'Médio',stunSetting:true,profilePages:[33,37]},
 {id:'geo-carbine',name:'Carabina blaster geonosiana',price:1300,skill:'armas_distancia_pesadas',damage:9,crit:2,range:'Médio',stunSetting:true,profilePages:[33]},
 {id:'geo-rifle',name:'Fuzil blaster geonosiano',price:1500,skill:'armas_distancia_pesadas',damage:9,crit:2,range:'Longo',stunSetting:true,profilePages:[33,37]}
]);
const PIDDOCK_BY_ID=new Map(PIDDOCK_WEAPONS.map(x=>[x.id,x]));
function dealValid(r){const item=PIDDOCK_BY_ID.get(r?.weaponId);
 if(!item||r.type!=='PIDDOCK_VERBAL_ARMS_AGREEMENT'||r.source?.productCode!=='SWE02'||r.source?.printedPage!==33||
  r.nodeId!=='piddock-party'||!r.characterKey||!Number.isInteger(r.quantity)||r.quantity<1||r.quantity>8||
  r.unitPrice!==item.price||r.discountPercent!==(r.haggleCheck?.success?10:0)||
  r.creditsDue!==item.price*r.quantity*(r.discountPercent===10?9:10)/10||
  r.paidAtParty!==false||r.weaponsDeliveredAtParty!==false)throw Error('SWE02: contrato Piddock adulterado.');
 if(r.haggleCheck)validateSWE02Check(r.haggleCheck,{difficulty:1,challenge:1,boost:0,setback:0});
 return item;
}
export function agreeSWE02PiddockArms(world,character,{weaponId,quantity=1,haggle=false,rng=Math.random}={}){
 const w=clone(world),p=progress(w,'piddock-party'),key=who(character),item=PIDDOCK_BY_ID.get(weaponId);
 if(p.activeEncounterId||!p.piddockTradeInterest||p.piddockConversationClosed)throw Error('SWE02: Piddock exige conversa comercial ativa.');
 if(!item||!Number.isInteger(quantity)||quantity<1||quantity>8||typeof haggle!=='boolean')throw Error('SWE02: arma ou quantidade inválida.');
 if((p.piddockArmsAgreements||[]).some(x=>x.weaponId===weaponId))throw Error('SWE02: acordo dessa arma já registrado.');
 let check=null;
 if(haggle){const skill='negociacion',characteristic=character?.characteristics?.[SKILLS[skill]];
  if(!Number.isInteger(characteristic)||characteristic<1||!Number.isInteger(character.skills?.[skill]||0))throw Error('SWE02: ficha inválida para Negociação.');
  check=rollPool({...buildPositivePool(characteristic,character.skills?.[skill]||0),difficulty:1,challenge:1,boost:0,setback:0},rng);
 }
 const discount=check?.success?10:0;
 const receipt={type:'PIDDOCK_VERBAL_ARMS_AGREEMENT',source:src(33),nodeId:'piddock-party',characterKey:key,
  weaponId,quantity,unitPrice:item.price,discountPercent:discount,creditsDue:item.price*quantity*(discount?9:10)/10,
  haggleCheck:check,paidAtParty:false,weaponsDeliveredAtParty:false};
 p.piddockArmsAgreements=[...(p.piddockArmsAgreements||[]),receipt];
 p.flags={...(p.flags||{}),piddockTradeAgreed:true};
 return {world:w,receipt};
}
export function deliverSWE02PiddockArms(world,character,{weaponId}={}){
 const w=clone(world),c=clone(character),p=progress(w,'piddock-freighter-handoff'),key=who(c);
 if(p.activeEncounterId)throw Error('SWE02: encontro ativo impede entrega.');
 const agreement=(p.piddockArmsAgreements||[]).find(x=>x.weaponId===weaponId);
 if(!agreement||agreement.characterKey!==key)throw Error('SWE02: contrato próprio inexistente.');
 if((p.piddockArmsDeliveries||[]).some(x=>x.weaponId===weaponId))throw Error('SWE02: entrega duplicada.');
 const item=dealValid(agreement);priceWallet(c,agreement.creditsDue);
 if(!Array.isArray(c.weapons))c.weapons=[];
 const newWeapons=Array.from({length:agreement.quantity},(_,index)=>({
  id:`swe02-piddock:${weaponId}:${key}:${index+1}`,name:item.name,skill:item.skill,damage:item.damage,
  crit:item.crit,range:item.range,stunSetting:item.stunSetting,
  source:{productCode:'SWE02',printedPages:item.profilePages,authority:'OFFICIAL_ADVENTURE_PROFILE'},
  purchasedFrom:'Duke Piddock',ownedAssetId:`swe02-piddock:${weaponId}:${key}:${index+1}`
 }));
 c.credits-=agreement.creditsDue;c.weapons.push(...newWeapons);
 const receipt={type:'PIDDOCK_FREIGHTER_ARMS_DELIVERY',source:src(33),nodeId:'piddock-freighter-handoff',
  weaponId,characterKey:key,quantity:agreement.quantity,creditsSpent:agreement.creditsDue,
  creditsRemaining:c.credits,ownedAssetIds:newWeapons.map(x=>x.ownedAssetId),agent:'PIDDOCK_AGENT',shipOwnershipUnchanged:true};
 p.piddockArmsDeliveries=[...(p.piddockArmsDeliveries||[]),receipt];
 return {world:w,character:c,receipt};
}
export function piddockArmsCatalog(world){const p=world?.publishedAdventure;
 if(p?.id!==ID||p.status!=='ACTIVE'||!['piddock-party','piddock-freighter-handoff'].includes(p.currentNodeId))return [];
 return PIDDOCK_WEAPONS.map(item=>({...item,agreed:Boolean((p.piddockArmsAgreements||[]).find(x=>x.weaponId===item.id)),
  delivered:Boolean((p.piddockArmsDeliveries||[]).find(x=>x.weaponId===item.id))}));
}

// Vrixx'tt cannot be charmed or coerced. His price buys one week, and the
// higher price buys a real sniper shot against a remaining Geonosian.
export function bribeSWE02Vrixxtt(world,combat,character,{credits=500,targetId=null,rng=Math.random}={}){
 const w=clone(world),c=clone(combat),buyer=clone(character),p=progress(w,'gantry-aa7');
 if(p.activeEncounterId!=='laoth-gantry-aa7'||!c?.active||c.publishedAdventureContext?.encounterId!=='laoth-gantry-aa7')throw Error('SWE02: Vrixx’tt só negocia no combate AA7.');
 if(!c.encounter?.waitingForPlayer||c.encounter.activeActorId!=='player'||c.playerTurn?.actionUsed)throw Error('SWE02: suborno exige ação de iniciativa do jogador.');
 if(p.vrixxttBribeReceipt)throw Error('SWE02: Vrixx’tt já recebeu suborno.');
 if(![500,1000].includes(credits))throw Error('SWE02 p.37: preço mínimo de 500 ou 1.000 créditos.');
 const key=who(buyer);priceWallet(buyer,credits);
 const gand=c.adversaries?.find(a=>a.profileKey==='laoth_vrixxtt_findsman'&&a.status==='active');
 if(!gand)throw Error('SWE02: Vrixx’tt não está ativo para aceitar dinheiro.');
 const remaining=c.adversaries.filter(a=>a.profileKey==='laoth_geonosian_soldier'&&a.status==='active');
 if(credits===1000&&remaining.length&&!remaining.some(a=>a.id===targetId))throw Error('SWE02: escolha um soldado real para o tiro do Gand.');
 let after=spendAction(c,'SWE02 p.37: pagar Vrixx’tt');buyer.credits-=credits;
 let shot=null;
 if(credits===1000&&remaining.length){
  const target=after.adversaries.find(a=>a.id===targetId);
  // Official Gand: Agility 4, Ranged Heavy 1, rifle damage 9. Long range 3;
  // cargo cover adds one Setback, per printed p.37.
  const pool={...buildPositivePool(4,1),difficulty:3,challenge:0,boost:0,setback:1};
  const check=rollPool(pool,rng);
  const rawDamage=check.success?9+check.netSuccess:0;
  const applied=check.success?applyAdversaryDamage(target,{damage:rawDamage}):null;
  if(applied)after.adversaries[after.adversaries.findIndex(a=>a.id===targetId)]=applied.adversary;
  shot={targetId,check,pool,rawDamage,suffered:applied?.suffered||0,targetStatus:applied?.adversary.status||target.status};
 }
 const index=after.adversaries.findIndex(a=>a.id===gand.id);after.adversaries[index].status='retreated';
 const receipt={type:'VRIXXTT_BRIBE',source:src(37),nodeId:'gantry-aa7',encounterId:'laoth-gantry-aa7',
  characterKey:key,creditsSpent:credits,creditsRemaining:buyer.credits,round:after.round,
  vrixxttId:gand.id,headStartDays:7,sniperSupport:credits===1000,shot};
 p.vrixxttBribeReceipt=receipt;
 after.log=[...(after.log||[]),{type:'SWE02_VRIXXTT_BRIBED',round:after.round,creditsSpent:credits,vrixxttId:gand.id,shot:clone(shot)}];
 return {world:w,combat:after,character:buyer,receipt};
}
export function validateSWE02Act2Contracts(world){
 const p=world?.publishedAdventure;if(p?.id!==ID)return true;
 const questions=p.piddockQuestionReceipts||[];
 if(!Array.isArray(questions)||questions.length>100)throw Error('SWE02: perguntas Piddock adulteradas.');
 for(let i=0;i<questions.length;i++){
  const r=questions[i];if(r.type!=='PIDDOCK_QUESTION'||r.source?.productCode!=='SWE02'||r.source?.printedPage!==33||r.nodeId!=='piddock-party'||
   !r.topic||r.topic.length>100||r.ordinal!==i+1||r.warning!==(i===3&&!r.tradeInterest)||
   r.conversationClosed!==(i>=4&&!r.tradeInterest)||Boolean(r.tradeInterest)!==Boolean(p.piddockTradeReceipt&&p.piddockTradeReceipt.afterOrdinal<=i))
   throw Error('SWE02: recibo Piddock adulterado.');
  if(i>4&&!r.tradeInterest)throw Error('SWE02: perguntas após encerramento de Piddock.');
 }
 if(Boolean(p.piddockConversationClosed)!==Boolean(questions.at(-1)?.conversationClosed))throw Error('SWE02: fechamento Piddock adulterado.');
 if(Boolean(p.piddockTradeInterest)!==Boolean(p.piddockTradeReceipt||questions.some(x=>x.tradeInterest)))throw Error('SWE02: interesse comercial sem recibo.');
 if(p.piddockTradeReceipt&&(p.piddockTradeReceipt.type!=='PIDDOCK_TRADE_INTEREST'||p.piddockTradeReceipt.source?.printedPage!==33||!Number.isInteger(p.piddockTradeReceipt.afterOrdinal)||p.piddockTradeReceipt.afterOrdinal<0||p.piddockTradeReceipt.afterOrdinal>questions.length))throw Error('SWE02: comércio Piddock adulterado.');
 const agreements=p.piddockArmsAgreements||[],deliveries=p.piddockArmsDeliveries||[];
 if(!Array.isArray(agreements)||agreements.length>3||new Set(agreements.map(x=>x.weaponId)).size!==agreements.length||
  !Array.isArray(deliveries)||deliveries.length>agreements.length||new Set(deliveries.map(x=>x.weaponId)).size!==deliveries.length)
  throw Error('SWE02: contratos/entregas Piddock duplicados.');
 if(Boolean(p.flags?.piddockTradeAgreed)!==Boolean(agreements.length)||agreements.length&&!p.piddockTradeReceipt)throw Error('SWE02: armas negociadas sem interesse comercial comprovado.');
 for(const r of agreements)dealValid(r);
 for(const d of deliveries){const a=agreements.find(x=>x.weaponId===d.weaponId);
  if(!a||d.type!=='PIDDOCK_FREIGHTER_ARMS_DELIVERY'||d.source?.productCode!=='SWE02'||d.source?.printedPage!==33||
   d.nodeId!=='piddock-freighter-handoff'||d.characterKey!==a.characterKey||d.quantity!==a.quantity||
   d.creditsSpent!==a.creditsDue||!Number.isSafeInteger(d.creditsRemaining)||d.creditsRemaining<0||
   d.agent!=='PIDDOCK_AGENT'||d.shipOwnershipUnchanged!==true||!Array.isArray(d.ownedAssetIds)||
   d.ownedAssetIds.length!==a.quantity||d.ownedAssetIds.some((id,i)=>id!==`swe02-piddock:${d.weaponId}:${d.characterKey}:${i+1}`))
   throw Error('SWE02: entrega Piddock adulterada.');
 }
 const v=p.vioHireReceipt;
 if(Boolean(p.flags?.vioHired)!==Boolean(v))throw Error('SWE02: contratação dos Vio sem recibo.');
 if(v){
  if(v.type!=='VIO_PASSAGE_HIRED'||v.source?.printedPage!==33||v.source?.productCode!=='SWE02'||v.nodeId!=='piddock-party'||!v.characterKey||
    !['WEX','ORPA','NEGOTIATE'].includes(v.offer)||!Number.isInteger(v.partySize)||v.partySize<1||v.partySize>8||
    v.perPerson!==(v.offer==='ORPA'?1000:v.offer==='NEGOTIATE'&&v.check?.success?250:500)||v.creditsSpent!==v.partySize*v.perPerson||
    !Number.isSafeInteger(v.creditsRemaining)||v.creditsRemaining<0||v.route!=='DIRECT_TO_LUCKY_GUESS'||v.shipOwnership!=='ORPA_AND_WEX_VIO'||v.skipDimmockAndGantry!==true)
    throw Error('SWE02: pagamento Vio adulterado.');
  if(v.offer==='NEGOTIATE')validateSWE02Check(v.check,{difficulty:2,challenge:0,boost:0,setback:0});
  else if(v.check!==null)throw Error('SWE02: teste Vio não solicitado.');
 }
 const r=p.vrixxttBribeReceipt;
 if(r){
  if(r.type!=='VRIXXTT_BRIBE'||r.source?.productCode!=='SWE02'||r.source?.printedPage!==37||r.nodeId!=='gantry-aa7'||r.encounterId!=='laoth-gantry-aa7'||
   !r.characterKey||![500,1000].includes(r.creditsSpent)||!Number.isSafeInteger(r.creditsRemaining)||r.creditsRemaining<0||
   !Number.isInteger(r.round)||r.round<1||!r.vrixxttId||r.headStartDays!==7||r.sniperSupport!==(r.creditsSpent===1000))
   throw Error('SWE02: suborno Vrixx’tt adulterado.');
  if(r.shot){if(r.creditsSpent!==1000||!r.shot.targetId||r.shot.pool?.difficulty!==3||r.shot.pool?.setback!==1||r.shot.pool?.ability!==3||r.shot.pool?.proficiency!==1||
     r.shot.rawDamage!==(r.shot.check.success?9+r.shot.check.netSuccess:0)||!Number.isInteger(r.shot.suffered)||r.shot.suffered<0||r.shot.suffered>r.shot.rawDamage)
     throw Error('SWE02: tiro Vrixx’tt adulterado.');
   validateSWE02Check(r.shot.check,{ability:3,proficiency:1,difficulty:3,challenge:0,boost:0,setback:1});}
  if((p.encounterHistory||[]).some(x=>x.encounterId==='laoth-gantry-aa7'&&x.outcome==='adversaries_defeated')){
   const record=p.encounterHistory.find(x=>x.encounterId==='laoth-gantry-aa7'&&x.outcome==='adversaries_defeated');
   if(!record.combatProof?.bribedVrixxttId||record.combatProof.bribedVrixxttId!==r.vrixxttId)throw Error('SWE02: vitória AA7 sem suborno refletido no combate.');
  }
 }
 return true;
}
