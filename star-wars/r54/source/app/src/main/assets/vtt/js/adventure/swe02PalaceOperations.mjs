/** SWE02 palace operations: source-authored DATA, shared deterministic executor.
 * No generated narrative may grant money, gear, evidence or combat effects.
 * Inert claims are tracked but never become usable weapons without a primary
 * EotE rule profile. Official SWE02 printed pp.42, 44–45.
 */
import {equipmentCatalog} from '../rules/equipmentCatalog.mjs';
import {buildPositivePool,rollPool} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {validateCheck} from './swe02SocialP241.mjs';
const clone = structuredClone;
const ID = 'long-arm-of-the-hutt';
const source = printedPage => ({ productCode:'SWE02', printedPage, authority:'OFFICIAL_PDF_TEXT_VERIFIED' });
export const SWE02_PALACE_OPERATIONS = Object.freeze([
 {id:'workshop-toolkit',scene:'palace-workshop',label:'Recolher kit de ferramentas',page:44,mode:'GEAR',itemId:'tool_kit',quantity:1},
 {id:'workshop-lights',scene:'palace-workshop',label:'Recolher duas barras luminosas',page:44,mode:'GEAR',itemId:'glow_rod',quantity:2},
 {id:'workshop-repair',scene:'palace-workshop',label:'Separar dois kits emergenciais de reparo (ficha pendente)',page:44,mode:'SEALED',assetName:'emergency repair kits',quantity:2},
 {id:'workshop-stimpacks',scene:'palace-workshop',label:'Separar dois stimpacks (ficha pendente)',page:44,mode:'SEALED',assetName:'stimpacks',quantity:2},
 {id:'workshop-carbines',scene:'palace-workshop',label:'Reservar três carabinas blaster, sem perfil de combate EotE',page:44,mode:'SEALED',assetName:'blaster carbines',quantity:3},
 {id:'workshop-vibroaxes',scene:'palace-workshop',label:'Reservar três vibromachados, sem perfil EotE',page:44,mode:'SEALED',assetName:'vibro-axes',quantity:3},
 {id:'workshop-b1-worklog',scene:'palace-workshop',label:'Recolher diário dos três droides B1 inoperantes',page:44,mode:'EVIDENCE',flag:'b1ReverseEngineeringEvidence'},
 {id:'kitchen-ryll',scene:'palace-kitchen',label:'Abrir os dois caixotes suspeitos de ryll após teste',page:44,mode:'EVIDENCE',flag:'ryllKitchenEvidence',requiredCheck:{scene:'palace-kitchen',index:0}},
 {id:'bar-cook',scene:'palace-cantina-bar',label:'Conversar com o droide cozinheiro (não combatente)',page:44,mode:'OBSERVATION',flag:'palaceCookHarmless'},
 {id:'bar-cover',scene:'palace-cantina-bar',label:'Examinar a cobertura do balcão (+2 revés para atiradores externos)',page:44,mode:'OBSERVATION',flag:'palaceBarCoverSeen'},
 {id:'spy-bith-testimony',scene:'palace-spy-interview',label:'Confrontar o músico Bith com sua espionagem',page:45,mode:'EVIDENCE',flag:'palaceBithWitnessEvidence',requiresVisited:'palace-spy-interview'}
]);
// Additional source-authored scene operations use the SAME executor as the palace.
// Printed p.40: deactivating the unattended gate calls exactly four guards
// away from the barracks; it does not grant free palace access or victory.
export const SWE02_SITE_OPERATIONS=Object.freeze([
 {id:'mos-electrogate-disable',scene:'mos-shuuta-electrogate',label:'Desativar eletroportão: Computadores Difícil (quatro guardas são alertados)',page:40,
  mode:'CHECK',skill:'computadoras',difficulty:3,flag:'electrogateBarracksDiverted',barracksCount:4},
 // One declaration format serves every non-combat palace zone. None of these
 // observations grants armour, free damage, a successful negotiation or XP.
 {id:'gate-armored-pillars',scene:'palace-front-gate',label:'Examinar pilares e cobertura dos portões blindados',page:42,mode:'OBSERVATION',flag:'palaceGatePillarsSeen'},
 {id:'gate-two-guards',scene:'palace-front-gate',label:'Identificar os dois guardas Gamorreanos e dois capangas do portão',page:42,mode:'OBSERVATION',flag:'palaceGateGuardComplementSeen'},
 {id:'reception-tables-cover',scene:'palace-reception',label:'Reconhecer a cobertura das mesas e cadeiras da recepção',page:42,mode:'OBSERVATION',flag:'palaceReceptionCoverSeen'},
 {id:'reception-refreshers',scene:'palace-reception',label:'Reconhecer os dois lavabos da recepção',page:42,mode:'OBSERVATION',flag:'palaceReceptionRefreshersSeen'},
 {id:'hallway-forgeries',scene:'palace-grand-hallway',label:'Examinar os quadros: imitações, não obras originais',page:42,mode:'OBSERVATION',flag:'palaceHallwayForgeriesSeen'},
 {id:'hallway-teemo-sculpture',scene:'palace-grand-hallway',label:'Inspecionar a holoescultura de Teemo e o mosaico do piso',page:42,mode:'OBSERVATION',flag:'palaceHallwayArtSeen'},
 {id:'hallway-painting-cargo',scene:'palace-grand-hallway',label:'Recolher um quadro volumoso (revenda aproximada 450 créditos, não recebidos)',page:42,
  mode:'CARGO',itemId:'swe02_hallway_painting',quantity:1,marketEstimate:450,bulky:true},
 {id:'gladiator-cells-layout',scene:'palace-gladiator-cells',label:'Reconhecer quatro gladiadores e as seis celas',page:44,mode:'OBSERVATION',flag:'palaceGladiatorCellsSeen'},
 {id:'workshop-baktoid-frame',scene:'palace-workshop',label:'Inspecionar três droides B1 incompletos, sem ativá-los',page:44,mode:'OBSERVATION',flag:'palaceBaktoidFramesSeen'},
 {id:'kitchen-galley',scene:'palace-kitchen',label:'Inspecionar a cozinha e os caixotes de alimentos sem presumir ryll',page:44,mode:'OBSERVATION',flag:'palaceKitchenGalleySeen'},
 {id:'bar-volatile-containers',scene:'palace-cantina-bar',label:'Identificar recipientes voláteis — explosão exige ataque e regras de granada',page:44,mode:'OBSERVATION',flag:'palaceBarVolatileContainersSeen'},
 {id:'landing-cannon-silhouette',scene:'palace-landing-pad',label:'Identificar canhão leve estacionário, silhueta 3, dano veicular 4',page:45,mode:'OBSERVATION',flag:'palaceLandingCannonSeen'},
 {id:'comms-network-security',scene:'palace-communications',label:'Identificar rede de segurança e acesso restrito aos terminais',page:45,mode:'OBSERVATION',flag:'palaceCommsSecuritySeen'},
 {id:'spy-player-characters',scene:'palace-spy-interview',label:'Identificar possível informante Bith sem conceder prova sem entrevista',page:42,mode:'OBSERVATION',flag:'palaceBithInterviewOpportunitySeen'}
]);
const map = new Map([...SWE02_PALACE_OPERATIONS,...SWE02_SITE_OPERATIONS].map(o=>[o.id,o]));
function active(world,character){
 const p=world?.publishedAdventure;
 if(p?.id!==ID||p.status!=='ACTIVE') throw Error('SWE02: aventura inativa.');
 if(!character || !(String(character.id||character.name||'').trim()))throw Error('SWE02: personagem não identificado.');
 return p;
}
function available(p,op){
 if(p.currentNodeId!==op.scene||p.activeEncounterId)return false;
 const previous=(p.palaceOperationReceipts||[]).filter(x=>x.operationId===op.id);
 if(op.mode==='CHECK'){
  if(previous.some(x=>x.success===true)||previous.length>=5)return false;
 }else if(previous.length)return false;
 if(op.requiresVisited&&!(p.visitedNodeIds||[]).includes(op.requiresVisited))return false;
 if(op.requiredCheck && !(p.checkHistory||[]).some(x=>x.nodeId===op.requiredCheck.scene&&x.checkIndex===op.requiredCheck.index&&x.success===true&&x.source?.productCode==='SWE02'))return false;
 return true;
}
export function listSWE02PalaceOperations(world){
 const p=world?.publishedAdventure;
 if(p?.id!==ID||p.status!=='ACTIVE')return [];
 return [...SWE02_PALACE_OPERATIONS,...SWE02_SITE_OPERATIONS].filter(op=>available(p,op)).map(op=>({id:op.id,label:op.label,source:source(op.page),mode:op.mode,executable:op.mode!=='SEALED',pendingRuleProfile:op.mode==='SEALED'}));
}
export function executeSWE02PalaceOperation(world,character,{operationId,rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=active(w,c),op=map.get(operationId);
 if(!op || !available(p,op))throw Error('SWE02: operação indisponível ou não comprovada nesta cena.');
 const who=String(c.id||c.name).trim();let ownedAssetId=null,usable=false,check=null,success=null;
 if(op.mode==='CHECK'){
  const characteristic=c.characteristics?.[SKILLS[op.skill]];
  if(!Number.isInteger(characteristic)||characteristic<1||!Number.isInteger(c.skills?.[op.skill]||0))throw Error('SWE02: ficha inválida para teste da instalação.');
  check=rollPool({...buildPositivePool(characteristic,c.skills?.[op.skill]||0),difficulty:op.difficulty,challenge:0,boost:0,setback:0},rng);
  success=check.success;
  if(success)p.flags={...(p.flags||{}),[op.flag]:true};
 }
 if(op.mode==='CARGO'){
  // Official p.42 gives an AVERAGE resale value, never a present cash reward.
  // Do not provide an artificial encumbrance or a combat-ready gear profile.
  ownedAssetId=`swe02-palace:${op.id}:${who}`;
  c.gear=[...(c.gear||[]),{id:op.itemId,name:'Quadro decorativo volumoso do palácio de Teemo',
   quantity:1,ownedAssetId,sourceAdventure:source(op.page),bulky:true,encumbranceUnverified:true,
   marketEstimateCredits:op.marketEstimate,saleCompleted:false,usableInCombat:false}];
 }
 if(op.mode==='GEAR'){
  const item=equipmentCatalog().generalGear.find(x=>x.id===op.itemId&&x.source?.activeRulesAuthority===true&&x.source?.rulesProfile==='EOTE_CORE');
  if(!item)throw Error('SWE02: equipamento não possui ficha primária EotE; não entregar item genérico.');
  ownedAssetId=`swe02-palace:${op.id}:${who}`;
  c.gear=[...(c.gear||[]),{...clone(item),quantity:op.quantity,ownedAssetId,sourceAdventure:source(op.page)}];
  usable=true;
 }
 if(op.mode==='EVIDENCE'||op.mode==='OBSERVATION')p.flags={...(p.flags||{}),[op.flag]:true};
 const receipt={type:'SWE02_DECLARATIVE_PALACE_OPERATION',source:source(op.page),operationId:op.id,scene:op.scene,
  characterKey:who,mode:op.mode,quantity:op.quantity||null,assetId:ownedAssetId,
  sealedAsset:op.mode==='SEALED'?op.assetName:null,usable,
  requiredCheck:op.requiredCheck?clone(op.requiredCheck):null,...(op.mode==='CHECK'?{check,success,barracksCount:op.barracksCount}: {})};
 if(op.mode==='CARGO'){
  receipt.marketEstimateCredits=op.marketEstimate;
  receipt.bulky=true;
  receipt.saleCompleted=false;
 }
 p.palaceOperationReceipts=[...(p.palaceOperationReceipts||[]),receipt];
 return {world:w,character:c,receipt};
}
export function validateSWE02PalaceOperations(world){
 const p=world?.publishedAdventure;if(p?.id!==ID)return true;
 const rr=p.palaceOperationReceipts||[];
 if(!Array.isArray(rr)||rr.length>map.size+5)throw Error('SWE02: recibos declarativos adulterados.');
 const seen=new Set();
 for(const r of rr){
  const op=map.get(r.operationId);
  if(!op||(op.mode!=='CHECK'&&seen.has(op.id))||r.type!=='SWE02_DECLARATIVE_PALACE_OPERATION'||r.source?.productCode!=='SWE02'||
   r.source?.printedPage!==op.page||r.scene!==op.scene||r.mode!==op.mode||!r.characterKey||
   r.quantity!==(op.quantity||null)||r.assetId!==(['GEAR','CARGO'].includes(op.mode)?`swe02-palace:${op.id}:${r.characterKey}`:null)||
   r.usable!==(op.mode==='GEAR')||r.sealedAsset!==(op.mode==='SEALED'?op.assetName:null)||
   JSON.stringify(r.requiredCheck)!==JSON.stringify(op.requiredCheck||null))throw Error('SWE02: operação declarativa adulterada.');
  if(op.mode==='CARGO'&&(r.marketEstimateCredits!==op.marketEstimate||r.bulky!==true||r.saleCompleted!==false))
   throw Error('SWE02: venda ou proveniência de arte adulterada.');
  if(op.mode==='CHECK'){
   const checks=rr.filter(x=>x.operationId===op.id);
   if(checks.length>5||checks.filter(x=>x.success).length>1||r.barracksCount!==op.barracksCount||
    r.success!==r.check?.success||r.usable!==false||r.assetId!==null||r.sealedAsset!==null)
    throw Error('SWE02: teste do eletroportão adulterado.');
   validateCheck(r.check,{difficulty:op.difficulty,challenge:0,boost:0,setback:0});
  }else if(r.check!==undefined||r.success!==undefined||r.barracksCount!==undefined)throw Error('SWE02: teste inesperado em operação de palácio.');
  if(op.mode==='EVIDENCE'||op.mode==='OBSERVATION'){
   if(p.flags?.[op.flag]!==true)throw Error('SWE02: evidência ou observação sem registro íntegro.');
  }
  if(op.requiredCheck && !(p.checkHistory||[]).some(x=>x.nodeId===op.requiredCheck.scene&&x.checkIndex===op.requiredCheck.index&&x.success===true&&x.source?.productCode==='SWE02'))
   throw Error('SWE02: evidência declarativa sem teste real.');
  if(op.requiresVisited&&!(p.visitedNodeIds||[]).includes(op.requiresVisited))throw Error('SWE02: testemunha não visitada.');
  seen.add(op.id);
 }
 const gate=rr.filter(x=>x.operationId==='mos-electrogate-disable');
 if(Boolean(p.flags?.electrogateBarracksDiverted)!==gate.some(x=>x.success===true))throw Error('SWE02: desvio do quartel sem teste de eletroportão comprovado.');
 return true;
}