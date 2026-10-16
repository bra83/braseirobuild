// SWE02 Act 3, printed pp.34, 44–46. No narrator-created victories.
// This executor covers two distinct nonviolent endings and the real
// strongbox after an independently proven combat victory. It is NOT an
// assertion that the entire published adventure has been certified.
import {rollPool,buildPositivePool} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {attackDifficultyForWeapon} from '../rules/attackDifficulty.mjs';
import {spendAction} from '../rules/combat.mjs';
import {validateCheck} from './swe02SocialP241.mjs';
const clone=structuredClone;
const ID='long-arm-of-the-hutt';
const SRC=Object.freeze({productCode:'SWE02',printedPages:[34,44,45,46],authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const CHANDELIER_SOURCE=Object.freeze({productCode:'SWE02',printedPage:46,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const EVIDENCE=['B1_DROIDS','RYLL_WITHHELD','JABBA_SPY'];
function progress(world){const p=world?.publishedAdventure;
 if(p?.id!==ID||p.status!=='ACTIVE')throw Error('SWE02 Ato 3: aventura ativa obrigatória.');return p;}
function hasCheck(p,node,index){return (p.checkHistory||[]).some(x=>x.nodeId===node&&Number(x.checkIndex)===index&&x.success===true&&x.source?.productCode==='SWE02');}
export function act3VerifiedEvidence(p){
 if(!p||p.id!==ID)return [];
 const v=p.visitedNodeIds||[];
 const sourceAction=id=>(p.palaceOperationReceipts||[]).some(r=>r.operationId===id&&r.source?.productCode==='SWE02');
 const b1=(v.includes('palace-workshop-evidence')&&p.flags?.b1ReverseEngineeringEvidence===true)||hasCheck(p,'palace-communications',3)||(p.flags?.b1ReverseEngineeringEvidence===true&&sourceAction('workshop-b1-worklog'));
 const ryll=(v.includes('palace-kitchen-evidence')&&hasCheck(p,'palace-kitchen',0))||hasCheck(p,'palace-communications',2)||(p.flags?.ryllKitchenEvidence===true&&hasCheck(p,'palace-kitchen',0)&&sourceAction('kitchen-ryll'));
 const spy=hasCheck(p,'palace-communications',4)||(p.flags?.palaceBithWitnessEvidence===true&&v.includes('palace-spy-interview')&&sourceAction('spy-bith-testimony'));
 return [b1&&'B1_DROIDS',ryll&&'RYLL_WITHHELD',spy&&'JABBA_SPY'].filter(Boolean);
}
function inPalace(p){return p.currentNodeId.startsWith('palace-')||['teemo-showdown','tatooine-infiltration'].includes(p.currentNodeId);}
function provenTeemoDeath(p){return p.flags?.teemoDefeated===true&&(p.encounterHistory||[]).some(r=>
 r.encounterId==='teemo-palace-guards'&&r.outcome==='adversaries_defeated'&&r.combatProof?.teemoIncapacitated===true&&r.combatProof?.eliminated===true&&
 Array.isArray(r.combatProof.enemyIds)&&new Set(r.combatProof.enemyIds).size===r.combatProof.groupCount&&r.combatProof.enemyIds.length===r.combatProof.groupCount);}
function addReceipt(p,type,fields){p.act3ResolutionReceipts=[...(p.act3ResolutionReceipts||[]),{type,source:clone(SRC),...fields}];return clone(p.act3ResolutionReceipts.at(-1));}
export function prepareSWE02AnattaDossier(world){
 const w=clone(world),p=progress(w);
 if(!inPalace(p))throw Error('SWE02: as provas concretas precisam ser obtidas no palácio.');
 if(p.activeEncounterId)throw Error('SWE02: não preparar dossiê durante combate.');
 if(p.act3Dossier||p.act3Resolution)throw Error('SWE02: dossiê já registrado ou desfecho resolvido.');
 const evidence=act3VerifiedEvidence(p);
 if(evidence.length<2)throw Error('SWE02 p.34: Anatta exige pelo menos duas provas concretas independentes.');
 p.act3Dossier={evidence,recipient:'ANATTA',verifiedAtNode:p.currentNodeId};
 const receipt=addReceipt(p,'ANATTA_DOSSIER_PREPARED',{evidence,verifiedAtNode:p.currentNodeId});
 return {world:w,receipt,evidence};
}
export function resolveSWE02TeemoWithoutCombat(world,{method}={}){
 const w=clone(world),p=progress(w);
 if(!['BLACKMAIL','JABBA'].includes(method))throw Error('SWE02: método de desfecho inválido.');
 if(p.activeEncounterId)throw Error('SWE02: combate ativo impede acordo pacífico.');
 if(!p.act3Dossier||p.act3Dossier.evidence.length<2||p.act3Resolution||p.flags?.teemoDefeated)
  throw Error('SWE02: faltam provas independentes ou Teemo já foi resolvido.');
 if(method==='BLACKMAIL'&&p.currentNodeId!=='teemo-showdown')throw Error('SWE02: chantagem exige confronto diante de Teemo.');
 if(method==='JABBA'&&!inPalace(p))throw Error('SWE02: comunicação com Anatta requer provas recolhidas no palácio.');
 const evidence=act3VerifiedEvidence(p);
 if(p.act3Dossier.evidence.some(x=>!evidence.includes(x))||evidence.length<2)throw Error('SWE02: dossiê não corresponde a provas reais.');
 p.act3Resolution={method,teemoStatus:method==='BLACKMAIL'?'ALIVE_DEAL':'REMOVED_BY_JABBA',
  bountyThreatEnded:true,teemoKilled:false,jabbaInformed:method==='JABBA',evidence:clone(p.act3Dossier.evidence)};
 p.flags={...(p.flags||{}),teemoPeacefullyNeutralized:true,jabbaInformed:method==='JABBA'};
 const receipt=addReceipt(p,'TEEMO_PEACEFUL_RESOLUTION',clone(p.act3Resolution));
 return {world:w,receipt,resolution:clone(p.act3Resolution)};
}
export function claimSWE02TeemoStrongbox(world,character){
 const w=clone(world),c=clone(character),p=progress(w);
 if(p.currentNodeId!=='teemo-showdown')throw Error('SWE02: cofre do trono só pode ser recolhido na sala após a derrota.');
 if(p.activeEncounterId||!provenTeemoDeath(p))throw Error('SWE02: cofre exige derrota de Teemo comprovada por combate encerrado.');
 if(p.act3Strongbox||p.act3Resolution)throw Error('SWE02: cofre já recolhido ou desfecho incompatível.');
 if(!Number.isSafeInteger(c.credits)||c.credits<0||c.credits>Number.MAX_SAFE_INTEGER-10000)throw Error('SWE02: carteira inválida.');
 c.credits+=10000;
 p.act3Strongbox={credits:10000,recipientKey:String(c.id||c.name||''),creditsAfter:c.credits};
 const receipt=addReceipt(p,'TEEMO_STRONGBOX_CLAIMED',clone(p.act3Strongbox));
 return {world:w,character:c,receipt};
}
// SWE02 p.46: the chain is a blaster target. The real attack roll upgrades
// its ranged difficulty twice. On a hit Teemo dies, and his guards flee;
// no 'victory' flag is minted before the combat's own closure receipt.
export function shootSWE02Chandelier(world,combat,character,{weaponIndex,rng=Math.random}={}){
 const w=clone(world),c=clone(combat),p=progress(w),actor=clone(character);
 if(p.currentNodeId!=='teemo-showdown'||p.activeEncounterId!=='teemo-palace-guards'||
    !c?.active||c.publishedAdventureContext?.encounterId!=='teemo-palace-guards')
  throw Error('SWE02: lustre só pode ser atingido no combate real do trono.');
 if(!hasCheck(p,'teemo-showdown',0))throw Error('SWE02 p.46: identifique a corrente com Percepção Média primeiro.');
 if(!c.encounter?.waitingForPlayer||c.encounter.activeActorId!=='player'||c.playerTurn?.actionUsed)
  throw Error('SWE02: aguarde a ação real do jogador.');
 if(p.act3ChandelierReceipts?.some(x=>x.success))throw Error('SWE02: lustre já derrubado.');
 if(!Number.isInteger(weaponIndex)||weaponIndex<0||weaponIndex>=actor.weapons?.length)throw Error('SWE02: arma inválida.');
 const weapon=actor.weapons[weaponIndex];
 if(!/blaster|bláster/i.test(weapon.name||'')||!['armas_distancia_ligeras','armas_distancia_pesadas'].includes(weapon.skill))
  throw Error('SWE02 p.46: a corrente exige uma arma blaster.');
 const teemo=c.adversaries?.find(a=>a.profileKey==='teemo_the_hutt');
 if(!teemo||teemo.status!=='active')throw Error('SWE02: Teemo não está presente e ativo.');
 const base=attackDifficultyForWeapon(weapon,teemo.range||'Médio');
 if(base<2)throw Error('SWE02: dificuldade da corrente requer duas melhorias; confira a distância.');
 const characteristic=actor.characteristics?.[SKILLS[weapon.skill]];
 if(!Number.isInteger(characteristic)||characteristic<1)throw Error('SWE02: atributo do atirador inválido.');
 const pool={...buildPositivePool(characteristic,actor.skills?.[weapon.skill]||0),difficulty:base-2,challenge:2,boost:0,setback:0};
 const check=rollPool(pool,rng);
 const after=spendAction(c,'SWE02 p.46: tiro na corrente do lustre');
 const affected=[];
 if(check.success){
  teemo.status='incapacitated';teemo.wounds.current=teemo.wounds.threshold+1;
  for(const a of after.adversaries){if(a.profileKey==='teemo_the_hutt'){
    a.status='incapacitated';a.wounds.current=a.wounds.threshold+1;
  }else if(a.status==='active'){a.status='retreated';affected.push(a.id);}}
  after.encounter.outcome='adversaries_defeated';after.encounter.waitingForPlayer=false;
 }
 const receipt={type:'TEEMO_CHANDELIER_SHOT',source:clone(CHANDELIER_SOURCE),encounterId:'teemo-palace-guards',
  nodeId:p.currentNodeId,round:after.round,weaponName:weapon.name,weaponSkill:weapon.skill,
  characterKey:String(actor.id||actor.name||''),range:teemo.range||'Médio',baseDifficulty:base,
  pool,check,success:check.success,retreatedEnemyIds:affected};
 p.act3ChandelierReceipts=[...(p.act3ChandelierReceipts||[]),receipt];
 after.log=[...(after.log||[]),{type:'SWE02_TEEMO_CHANDELIER',round:after.round,success:check.success,
   teemoIncapacitated:check.success,retreatedEnemyIds:affected}];
 return {world:w,combat:after,receipt};
}
export function swe02Act3Consequences(p){
 if(!p||p.id!==ID)return null;
 const r=p.act3Resolution;
 const dead=provenTeemoDeath(p);
 const neutralized=dead||Boolean(r);
 return {neutralized,method:dead?'COMBAT':r?.method||null,
  teemoDead:dead,teemoAliveDeal:r?.method==='BLACKMAIL',teemoRemovedByJabba:r?.method==='JABBA',
  bountyThreatEnded:neutralized,jabbaRetaliationRisk:dead&&!p.flags?.jabbaInformed,
  twilekRelations:p.flags?.newMeenOutcome==='LOST'&&!neutralized?'HOSTILE_RISK':'UNRESOLVED_BY_SOURCE_OR_FRIENDLY',
  rewardCreditsClaimed:p.act3Strongbox?.credits||0};
}
export function validateSWE02Act3(world){
 const p=world?.publishedAdventure;if(p?.id!==ID)return true;
 const shots=p.act3ChandelierReceipts||[];
 if(!Array.isArray(shots)||shots.filter(x=>x.success).length>1)throw Error('SWE02 Ato 3: tiros do lustre duplicados.');
 for(const shot of shots){
  if(shot.type!=='TEEMO_CHANDELIER_SHOT'||shot.source?.productCode!=='SWE02'||shot.source?.printedPage!==46||shot.encounterId!=='teemo-palace-guards'||shot.nodeId!=='teemo-showdown'||
    !Number.isInteger(shot.round)||shot.round<1||!shot.characterKey||!['armas_distancia_ligeras','armas_distancia_pesadas'].includes(shot.weaponSkill)||!(/blaster|bláster/i.test(shot.weaponName||''))||
    !Number.isInteger(shot.baseDifficulty)||shot.baseDifficulty<2||shot.pool?.challenge!==2||shot.pool?.difficulty!==shot.baseDifficulty-2||shot.success!==shot.check?.success||
    !Array.isArray(shot.retreatedEnemyIds)||(!shot.success&&shot.retreatedEnemyIds.length))throw Error('SWE02 Ato 3: tiro do lustre adulterado.');
  validateCheck(shot.check,{difficulty:shot.baseDifficulty-2,challenge:2,boost:0,setback:0});
  if(!hasCheck(p,'teemo-showdown',0))throw Error('SWE02 Ato 3: lustre sem Percepção real.');
 }
 const receipts=p.act3ResolutionReceipts||[];
 if(!Array.isArray(receipts)||receipts.length>3)throw Error('SWE02 Ato 3: recibos adulterados.');
 const types=receipts.map(x=>x.type);
 if(new Set(types).size!==types.length||receipts.some(x=>x.source?.productCode!=='SWE02'||JSON.stringify(x.source.printedPages)!=='[34,44,45,46]'||x.source.authority!=='OFFICIAL_PDF_TEXT_VERIFIED'))throw Error('SWE02 Ato 3: fonte ou recibos adulterados.');
 const evidence=act3VerifiedEvidence(p),d=p.act3Dossier,r=p.act3Resolution,b=p.act3Strongbox;
 if(Boolean(d)!==types.includes('ANATTA_DOSSIER_PREPARED')||Boolean(r)!==types.includes('TEEMO_PEACEFUL_RESOLUTION')||Boolean(b)!==types.includes('TEEMO_STRONGBOX_CLAIMED'))throw Error('SWE02 Ato 3: estado sem recibo.');
 if(d){const receipt=receipts.find(x=>x.type==='ANATTA_DOSSIER_PREPARED');
  if(d.recipient!=='ANATTA'||!Array.isArray(d.evidence)||d.evidence.length<2||new Set(d.evidence).size!==d.evidence.length||d.evidence.some(x=>!EVIDENCE.includes(x)||!evidence.includes(x))||JSON.stringify(receipt.evidence)!==JSON.stringify(d.evidence)||receipt.verifiedAtNode!==d.verifiedAtNode)throw Error('SWE02 Ato 3: dossiê sem provas reais.');}
 if(r){const receipt=receipts.find(x=>x.type==='TEEMO_PEACEFUL_RESOLUTION');
  if(!d||!['BLACKMAIL','JABBA'].includes(r.method)||r.teemoKilled!==false||r.bountyThreatEnded!==true||r.jabbaInformed!==(r.method==='JABBA')||r.teemoStatus!==(r.method==='JABBA'?'REMOVED_BY_JABBA':'ALIVE_DEAL')||JSON.stringify(r.evidence)!==JSON.stringify(d.evidence)||p.flags?.teemoPeacefullyNeutralized!==true||p.flags?.jabbaInformed!==(r.method==='JABBA')||p.flags?.teemoDefeated===true||JSON.stringify({...receipt,source:undefined,type:undefined})!==JSON.stringify({...r,source:undefined,type:undefined}))throw Error('SWE02 Ato 3: desfecho pacífico adulterado.');}
 else if(p.flags?.teemoPeacefullyNeutralized||p.flags?.jabbaInformed)throw Error('SWE02 Ato 3: neutralização sem provas.');
 if(b){const receipt=receipts.find(x=>x.type==='TEEMO_STRONGBOX_CLAIMED');
  if(r||!provenTeemoDeath(p)||b.credits!==10000||!b.recipientKey||!Number.isSafeInteger(b.creditsAfter)||receipt.credits!==10000||receipt.recipientKey!==b.recipientKey||receipt.creditsAfter!==b.creditsAfter)throw Error('SWE02 Ato 3: cofre sem vitória ou valor adulterado.');}
 return true;
}
