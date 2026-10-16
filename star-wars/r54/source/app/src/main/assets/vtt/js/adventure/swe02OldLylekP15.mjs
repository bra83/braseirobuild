const SOURCE = Object.freeze({ title:'The Long Arm of the Hutt', productCode:'SWE02', printedPages:[15,16,17,18], authority:'OFFICIAL_PDF_TEXT_VERIFIED' });
const clone = value => structuredClone(value);

function requireProgress(progress) {
  if (!progress || progress.id !== 'long-arm-of-the-hutt' || progress.currentNodeId !== 'lylek-den-ambush')
    throw new Error('SWE02: a sequência do Old Lylek Den só está disponível na cena do covil.');
  return progress;
}

export function createSWE02OldLylekState() {
  return { schema:1, phase:'approach', route:null, pilotChecks:[], lookout:null, crashChecks:[], crashResolved:false,
    repairChecks:[], huntersOutcome:null, lylekTriggered:false, lylekWarning:null, lylekEncounter:null, hunterLoot:null,
    trexCarryover:{status:'pending',history:null,source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:15},reference:{title:'Edge of the Empire Adventure Book',productCode:'SWE01',printedPage:24}},
    source:clone(SOURCE) };
}

const TREX_HISTORY=Object.freeze({
  escaped:Object.freeze({present:true,preWounds:0,label:'Escapou de T’rex sem luta'}),
  defeated_unconfirmed_dead:Object.freeze({present:true,preWounds:10,label:'T’rex foi derrotado, mas sua morte não foi confirmada'}),
  confirmed_dead:Object.freeze({present:false,preWounds:0,label:'T’rex foi morto com confirmação'})
});

export function requireSWE02TrexCarryover(progress) {
  const t=progress?.oldLylek?.trexCarryover,rule=TREX_HISTORY[t?.history];
  if(t?.status!=='selected'||!rule||t.present!==rule.present||t.preWounds!==rule.preWounds)
    throw new Error('SWE02 p.15: histórico de T’rex ausente ou adulterado; registre novamente uma opção válida de Escape from Mos Shuuta.');
  return clone(t);
}

export function selectSWE02TrexCarryover(world,{history}={}) {
  const next=clone(world||{}),p=next.publishedAdventure;
  if(!p||p.id!=='long-arm-of-the-hutt'||p.currentNodeId!=='lylek-den-ambush')
    throw new Error('SWE02 p.15: histórico de T’rex só pode ser registrado na cena do Old Lylek Den.');
  const old=p.oldLylek||createSWE02OldLylekState();
  if(old.phase!=='approach'||old.trexCarryover?.status==='selected'||!Object.hasOwn(TREX_HISTORY,history))
    throw new Error('SWE02 p.15: escolha um resultado válido de Escape from Mos Shuuta antes de iniciar a emboscada.');
  const result=TREX_HISTORY[history];
  p.oldLylek={...old,trexCarryover:{status:'selected',history,present:result.present,preWounds:result.preWounds,
    label:result.label,source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:15,authority:'OFFICIAL_PDF_TEXT_VERIFIED'},
    profileSource:{title:'Edge of the Empire Adventure Book',productCode:'SWE01',printedPage:24,authority:'OFFICIAL_SOURCE_PROFILE_VERIFIED'}}};
  p.updatedAt=new Date().toISOString();
  return {world:next,carryover:clone(p.oldLylek.trexCarryover)};
}

export function chooseSWE02OldLylekRoute(progress, { slowDown } = {}) {
  const p=requireProgress(progress), old=p.oldLylek||createSWE02OldLylekState();
  if(old.phase!=='approach'||typeof slowDown!=='boolean')throw new Error('SWE02 p.15: escolha de rota indisponível ou inválida.');
  const next=clone(p);next.oldLylek={...old,route:slowDown?'slow':'fast',phase:slowDown?'lookout':'pilot-1'};
  next.oldLylek.source=clone(SOURCE);return {progress:next,phase:next.oldLylek.phase};
}

export function recordSWE02OldLylekPilot(progress, check) {
  const p=requireProgress(progress), old=p.oldLylek;
  if(!old||!['pilot-1','pilot-2'].includes(old.phase)||old.route!=='fast'||typeof check?.success!=='boolean')
    throw new Error('SWE02 p.15: teste de Pilotagem fora da sequência.');
  const index=old.pilotChecks.length;
  if(index>1||old.phase!==`pilot-${index+1}`)throw new Error('SWE02 p.15: os dois testes devem ser sucessivos.');
  const receipt={index:index+1,skillKey:'pilotar_veiculo_planetario',difficulty:2,success:check.success,
    netSuccess:Number(check.netSuccess)||0,netAdvantage:Number(check.netAdvantage)||0,collision:!check.success,
    onboardPcStrain:!check.success?{perPc:1,status:'PENDENTE',resolution:'Manual: aplicar 1 Tensão a cada PJ a bordo; o cadastro local não confirma a tripulação inteira.'}:null,
    source:clone(SOURCE)};
  const checks=[...old.pilotChecks,receipt],phase=checks.length===2?'lookout':'pilot-2';
  const next=clone(p);next.oldLylek={...old,pilotChecks:checks,phase};
  return {progress:next,receipt,phase};
}

export function recordSWE02OldLylekLookout(progress, check) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='lookout'||typeof check?.success!=='boolean'||old.lookout)
    throw new Error('SWE02 p.17: Percepção indisponível ou já registrada.');
  const receipt={skillKey:'percepcion',difficulty:3,success:check.success,netSuccess:Number(check.netSuccess)||0,
    netAdvantage:Number(check.netAdvantage)||0,spottedGand:check.success,source:clone(SOURCE)};
  const next=clone(p);next.oldLylek={...old,lookout:receipt,phase:'bluff'};
  return {progress:next,receipt,phase:'bluff'};
}

export function proceedSWE02OldLylekBluff(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||!['lookout','bluff'].includes(old.phase))throw new Error('SWE02 p.17: aproximação indisponível.');
  const next=clone(p);
  const lookout=old.lookout||{attempted:false,spottedGand:false,source:clone(SOURCE)};
  if(old.phase==='lookout'){
    next.oldLylek={...old,lookout,phase:old.route==='fast'?'crash-checks':'combat-ready',crashResolved:old.route!=='fast'};
  } else if(old.route==='fast')next.oldLylek={...old,lookout,phase:'crash-checks',crashResolved:false};
  else next.oldLylek={...old,lookout,phase:'combat-ready',crashResolved:true};
  return {progress:next,phase:next.oldLylek.phase,crash:old.route==='fast'};
}

export function approachSWE02OldLylekOnFoot(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||!['lookout','bluff'].includes(old.phase))
    throw new Error('SWE02 p.17: abordagem a pé indisponível.');
  const next=clone(p),lookout=old.lookout||{attempted:false,spottedGand:false,source:clone(SOURCE)};
  next.oldLylek={...old,lookout,phase:'combat-ready',approachOnFoot:true,crashResolved:true};
  return {progress:next,phase:'combat-ready'};
}

export function recordSWE02OldLylekCrashCheck(progress,{characterName,skillKey,check}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='crash-checks'||!['aguante','coordenacion'].includes(skillKey)||!String(characterName||'').trim()||typeof check?.success!=='boolean')
    throw new Error('SWE02 p.17: escolha Aguante ou Coordenação e um personagem da tripulação.');
  if(old.crashChecks.some(x=>x.characterName===characterName))throw new Error('SWE02 p.17: este personagem já fez o teste do choque.');
  const receipt={characterName:String(characterName),skillKey,difficulty:2,success:check.success,damage:check.success?0:6,
    netSuccess:Number(check.netSuccess)||0,netAdvantage:Number(check.netAdvantage)||0,source:clone(SOURCE)};
  const next=clone(p);next.oldLylek={...old,crashChecks:[...old.crashChecks,receipt]};
  return {progress:next,receipt};
}

export function confirmSWE02OldLylekCrashCrew(progress,{expectedPcCount}={}) {
  const p=requireProgress(progress),old=p.oldLylek,n=Number(expectedPcCount);
  if(!old||old.phase!=='crash-checks'||!Number.isInteger(n)||n<1||n>8||old.crashChecks.length!==n)
    throw new Error('SWE02 p.17: cada PC precisa de seu teste de Aguante ou Coordenação antes do reparo.');
  const next=clone(p);next.oldLylek={...old,phase:'repair',crashResolved:true,expectedPcCount:n};
  return {progress:next,phase:'repair'};
}

export function recordSWE02OldLylekRepair(progress,check) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||!['repair','repair-failed'].includes(old.phase)||typeof check?.success!=='boolean')throw new Error('SWE02 p.17: reparo não disponível.');
  const receipt={skillKey:'mecanica',difficulty:1,success:check.success,netSuccess:Number(check.netSuccess)||0,
    netAdvantage:Number(check.netAdvantage)||0,timeMinutes:30,source:clone(SOURCE)};
  const next=clone(p),checks=[...old.repairChecks,receipt];
  next.oldLylek={...old,repairChecks:checks,phase:check.success?'combat-ready':'repair-failed',repairSuccess:check.success};
  return {progress:next,receipt,phase:next.oldLylek.phase};
}

export function markSWE02OldLylekCombatStarted(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='combat-ready'||old.huntersOutcome)throw new Error('SWE02 pp.15–17: hunters só começam após resolver rota e aproximação.');
  const next=clone(p);next.oldLylek={...old,phase:'hunters-combat'};return next;
}

export function recordSWE02OldLylekHunterOutcome(progress,{outcome,adversaries=[],combatLog=[]}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='hunters-combat'||old.huntersOutcome)throw new Error('SWE02 pp.15–18: combate dos caçadores não está ativo.');
  if(!['adversaries_defeated','player_incapacitated','ended','escaped'].includes(outcome))throw new Error('SWE02 p.18: resultado de combate inválido.');
  const expectedCount=Number(p.flags?.confirmedPartySize), expected=Number.isInteger(expectedCount)&&expectedCount>0?expectedCount:null;
  const history=old.trexCarryover?.history,carryover=requireSWE02TrexCarryover(p),hasTrex=TREX_HISTORY[history]?.present===true;
  const expectedTrex=hasTrex&&expected>1?1:0;
  const trexCount=adversaries.filter(a=>a.profileKey==='trex').length;
  const gandCount=adversaries.filter(a=>a.profileKey==='laoth_gand_bounty_hunter').length;
  if(!expected||adversaries.length!==expected||adversaries.some(a=>a.profileKey!=='laoth_gand_bounty_hunter'&&!['laoth_bounty_hunter','trex'].includes(a.profileKey))||
     gandCount!==1||trexCount!==expectedTrex||
     adversaries.some(a=>!a.id)||new Set(adversaries.map(a=>a.id)).size!==adversaries.length)
    throw new Error('SWE02 p.15: resultado exige os caçadores reais em quantidade igual aos PCs.');
  const defeated=adversaries.every(a=>['incapacitated','defeated'].includes(a.status));
  if(outcome==='adversaries_defeated'&&!defeated)throw new Error('SWE02 p.15: vitória exige os caçadores incapacitados.');
  const advantageTrigger=(combatLog||[]).some(x=>Number(x?.check?.netAdvantage)>0||Number(x?.netAdvantage)>0);
  const huntersOutcome=outcome==='adversaries_defeated'&&defeated?'defeated':outcome==='player_incapacitated'?'party-incapacitated':'retreated';
  const next=clone(p);next.oldLylek={...old,phase:'post-hunters',huntersOutcome,lylekTriggered:old.lylekTriggered||advantageTrigger,
    hunterProof:{enemyIds:adversaries.map(a=>a.id),outcome,defeated,source:clone(SOURCE)}};
  next.flags={...(next.flags||{}),lylekHuntersResolved:true};
  if(huntersOutcome==='defeated')next.oldLylek.hunterLoot={status:'available',items:[{name:'stimpack',quantity:2},{name:'medpac',quantity:1},{name:'emergency repair kit',quantity:1}],source:{title:SOURCE.title,productCode:'SWE02',printedPage:18,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}};
  return {progress:next,huntersOutcome,lylekTriggered:next.oldLylek.lylekTriggered};
}

export function inspectSWE02OldLylekSpeeder(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='post-hunters'||old.huntersOutcome!=='defeated'||old.hunterLoot?.status!=='available')
    throw new Error('SWE02 p.18: inspecione o speeder após derrotar os caçadores.');
  const next=clone(p);next.oldLylek={...old,hunterLoot:{...old.hunterLoot,status:'found',inspected:true}};return next;
}

export function warnSWE02OldLylek(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='post-hunters'||!old.lylekTriggered||old.lylekWarning)throw new Error('SWE02 p.18: B’ura só intervém após a perturbação do ninho e insistência dos PCs.');
  const next=clone(p);next.oldLylek={...old,lylekWarning:{warned:true,source:clone(SOURCE)},phase:'lylek-warned'};return next;
}

export function persistSWE02OldLylek(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='lylek-warned'||!old.lylekTriggered)throw new Error('SWE02 p.18: o lylek permanece passivo enquanto não for provocado.');
  const profileSource={title:'Edge of the Empire Beginner Game',productCode:'SWE01',printedPage:47,authority:'OFFICIAL_SOURCE_PROFILE_VERIFIED',verification:'VISUAL_PAGE_REVIEW',driveFileId:'1UtxFwtuN6pfc2kzHpA02Ooa2-jyiKjNk'};
  const next=clone(p);next.oldLylek={...old,phase:'lylek-ready',lylekEncounter:{profileKey:'swe02_captive_rancor',source:{...clone(SOURCE),printedPage:18,referenceProduct:'Edge of the Empire Beginner Game',referencePrintedPage:47,profileSourceVerified:true,profileSource,status:'VERIFIED'}}};return next;
}

export function markSWE02OldLylekAttackStarted(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='lylek-ready'||old.lylekEncounter?.profileKey!=='swe02_captive_rancor'||old.lylekEncounter?.source?.profileSourceVerified!==true)
    throw new Error('SWE02 p.18: o perfil de referência verificado e a decisão de persistir são necessários.');
  const next=clone(p);next.oldLylek={...old,phase:'lylek-attack-started',lylekEncounter:{...old.lylekEncounter,started:true}};return next;
}

export function resolveSWE02OldLylek(progress,{outcome,combatAdversary}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='lylek-attack-started'||!['player_incapacitated','adversaries_defeated','ended','escaped'].includes(outcome))throw new Error('SWE02 p.18: desfecho do ataque ao lylek indisponível.');
  // The published text says the beast beats PCs into submission, then returns to feed.
  // It does not specify a mechanical winner/defeat threshold; retain the GM adjudication.
  const next=clone(p);next.oldLylek={...old,phase:'post-lylek',lylekResolution:{outcome,gmConfirmedSubmission:outcome==='player_incapacitated',beastProfileVerified:true,profileKey:old.lylekEncounter?.profileKey,source:{...clone(SOURCE),printedPage:18,profile:{title:'Edge of the Empire Beginner Game',productCode:'SWE01',printedPage:47,authority:'OFFICIAL_SOURCE_PROFILE_VERIFIED'}}}};return next;
}

export function canSWE02OldLylekContinue(progress) {
  const p=progress,old=p?.oldLylek;
  return Boolean(old&&['post-hunters','post-lylek','lylek-warned','lylek-ready'].includes(old.phase)&&old.huntersOutcome);
}
