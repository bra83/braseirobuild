const SOURCE = Object.freeze({ title:'The Long Arm of the Hutt', productCode:'SWE02', printedPages:[15,16,17,18], authority:'OFFICIAL_PDF_TEXT_VERIFIED' });
const clone = value => structuredClone(value);

export const SWE02_OLD_LYLEK_MAP_ZONES=Object.freeze([
  Object.freeze({id:'heroes_approach',label:'Heroes’ Journey / approach',printedPage:16}),
  Object.freeze({id:'cave_mouth',label:'Bounty Hunters / cave mouth',printedPage:16}),
  Object.freeze({id:'hunter_speeder',label:'Bounty Hunters’ Speeder',printedPage:16}),
  Object.freeze({id:'eastern_flank',label:'Partially blocked eastern cave entrance',printedPage:17}),
  Object.freeze({id:'southern_nest',label:'Lylek Nest / southern nest',printedPage:16})
]);

export function createSWE02OldLylekMapState({pcEntries=[],hunters=[]}={}){
  if(!Array.isArray(pcEntries)||!Array.isArray(hunters)||pcEntries.some(x=>!x?.id)||hunters.some(x=>!x?.id))
    throw new Error('SWE02 p.16: o esquema do mapa exige IDs estáveis para PCs a bordo e caçadores.');
  const actors={};
  for(const pc of pcEntries){const key=`pc:${pc.id}`;if(actors[key])throw new Error('SWE02 p.16: ID de PC duplicado no mapa.');actors[key]={kind:'pc',id:pc.id,label:String(pc.character?.name||pc.name||pc.id)};}
  for(const hunter of hunters){const key=`hunter:${hunter.id}`;if(actors[key])throw new Error('SWE02 p.16: ID de caçador duplicado no mapa.');actors[key]={kind:'hunter',id:hunter.id,label:String(hunter.name||hunter.id),profileKey:hunter.profileKey||null};}
  return {schema:1,zones:clone(SWE02_OLD_LYLEK_MAP_ZONES),actors,
    actorLocations:Object.fromEntries(Object.keys(actors).map(key=>[key,actors[key].kind==='pc'?'heroes_approach':'cave_mouth'])),
    fixedFeatures:{huntersSpeeder:'hunter_speeder',lylekNest:'southern_nest'},
    geometry:{scale:null,distances:null,rangeBands:null,coverGeometry:null,lineOfSight:null,
      note:'Source-faithful named areas only; no scale, distance, range, cover geometry, or line-of-sight rules are inferred.'},
    source:{title:SOURCE.title,productCode:'SWE02',printedPage:16,relatedPrintedPage:17,authority:SOURCE.authority,visualMapReviewed:true}};
}

export function assignSWE02OldLylekMapLocation(map,{actorKey,locationId}={}){
  if(!map||map.schema!==1||!map.actors?.[actorKey]||!SWE02_OLD_LYLEK_MAP_ZONES.some(z=>z.id===locationId))
    throw new Error('SWE02 pp.16–17: escolha um PC ou caçador registrado e uma das áreas nomeadas do mapa.');
  const next=clone(map);next.actorLocations={...(next.actorLocations||{}),[actorKey]:locationId};
  next.moves=[...(next.moves||[]),{actorKey,locationId,source:{productCode:'SWE02',printedPage:16,relatedPrintedPage:17}}];
  return next;
}

function requireProgress(progress) {
  if (!progress || progress.id !== 'long-arm-of-the-hutt' || progress.currentNodeId !== 'lylek-den-ambush')
    throw new Error('SWE02: a sequência do Old Lylek Den só está disponível na cena do covil.');
  return progress;
}

export function createSWE02OldLylekState() {
  return { schema:2, phase:'approach', route:null, crewPcIds:null, pilotChecks:[], lookout:null, crashChecks:[], crashResolved:false,
    repairChecks:[],ruse:null,tactics:null,huntersOutcome:null, lylekTriggered:false, lylekWarning:null, lylekEncounter:null, hunterLoot:null,
    trexCarryover:{status:'pending',history:null,source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:15},reference:{title:'Edge of the Empire Adventure Book',productCode:'SWE01',printedPage:24}},
    source:clone(SOURCE) };
}

export function chooseSWE02OldLylekCrew(progress,{crewPcIds,rosterEntries,playerCount}={}){
  const p=requireProgress(progress),old=p.oldLylek||createSWE02OldLylekState();
  const ids=Array.isArray(crewPcIds)?crewPcIds:[],valid=new Set((rosterEntries||[]).map(x=>x?.id));
  const expected=Number(playerCount);
  if(old.phase!=='approach'||ids.length<1||ids.length>8||ids.some(x=>typeof x!=='string'||!valid.has(x))||new Set(ids).size!==ids.length||
    (Number.isInteger(expected)&&expected>0&&ids.length!==expected))
    throw new Error('SWE02 pp.15–17: registre uma vez cada PC humano a bordo; companheiros e NPCs não contam como PCs.');
  const next=clone(p);next.oldLylek={...old,schema:2,crewPcIds:[...ids],crewSource:{title:SOURCE.title,productCode:'SWE02',printedPage:15,authority:SOURCE.authority}};
  return {progress:next,crewPcIds:[...ids]};
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
  if(old.phase!=='approach'||typeof slowDown!=='boolean'||!Array.isArray(old.crewPcIds)||old.crewPcIds.length<1)throw new Error('SWE02 p.15: registre a tripulação e escolha uma rota válida.');
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
    onboardPcStrain:!check.success?{perPc:1,crewPcIds:[...(old.crewPcIds||[])],status:'PENDENTE',resolution:'Aplicação transacional ao cadastro de PCs pendente.'}:null,
    source:clone(SOURCE)};
  const checks=[...old.pilotChecks,receipt],phase=checks.length===2?'lookout':'pilot-2';
  const next=clone(p);next.oldLylek={...old,pilotChecks:checks,phase};
  return {progress:next,receipt,phase};
}

export function recordSWE02OldLylekPilotStrain(progress,{checkIndex,applications}={}){
  const p=requireProgress(progress),old=p.oldLylek,check=old?.pilotChecks?.[Number(checkIndex)];
  const ids=old?.crewPcIds??[];
  if(!check||check.success!==false||!Array.isArray(applications)||applications.length!==ids.length||
    new Set(applications.map(x=>x.pcId)).size!==ids.length||ids.some(id=>!applications.some(x=>x.pcId===id&&x.strainApplied===1)))
    throw new Error('SWE02 p.15: recibo exige 1 Tensão aplicada a cada PC a bordo após cada falha de Pilotagem.');
  const next=clone(p),checks=next.oldLylek.pilotChecks.slice();
  checks[Number(checkIndex)]={...check,onboardPcStrain:{perPc:1,crewPcIds:[...ids],status:'APPLIED',applications:clone(applications),source:{title:SOURCE.title,productCode:'SWE02',printedPage:15,authority:SOURCE.authority}}};
  next.oldLylek={...next.oldLylek,pilotChecks:checks};
  return next;
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
    next.oldLylek={...old,lookout,phase:old.route==='fast'?'crash-checks':'ruse',crashResolved:old.route!=='fast'};
  } else if(old.route==='fast')next.oldLylek={...old,lookout,phase:'crash-checks',crashResolved:false};
  else next.oldLylek={...old,lookout,phase:'ruse',crashResolved:true};
  return {progress:next,phase:next.oldLylek.phase,crash:old.route==='fast'};
}

export function recordSWE02OldLylekRuse(progress,{recognizedByHunters=false,identificationAsked=false,oskaraPresent,oskaraRecognizes=false}={}){
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='ruse'||typeof recognizedByHunters!=='boolean'||typeof identificationAsked!=='boolean'||typeof oskaraPresent!=='boolean'||
    (oskaraPresent&&typeof oskaraRecognizes!=='boolean')||(!oskaraPresent&&oskaraRecognizes!==false))
    throw new Error('SWE02 p.17: registre a resposta dos caçadores e se Oskara está presente antes do combate.');
  const trexPresent=old.trexCarryover?.present===true;
  const receipt={hail:'Local Imperial authorities; suspected ryll smuggling',uniformsWorn:false,identificationAsked,
    identifiedByHunters:identificationAsked?(trexPresent?['T’rex']:[]):[],recognizedByHunters,
    oskaraPresent,oskaraRecognizes:oskaraPresent?oskaraRecognizes:null,
    oskaraIdentifiesHumanHunters:oskaraPresent&&oskaraRecognizes,
    presumedRuseFailure:true,initiativeFollows:true,mechanicalOutcome:'No Bluff check or automatic combat advantage; source says ruse is ineffective.',
    source:{title:SOURCE.title,productCode:'SWE02',printedPage:17,authority:SOURCE.authority}};
  const next=clone(p);next.oldLylek={...old,ruse:receipt,phase:'combat-ready'};return {progress:next,receipt};
}

export function chooseSWE02OldLylekTactics(progress,{approach,frontalOutcome='not_applicable',lylekBranch}={}){
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='combat-ready'||!old.ruse||!['frontal','east_cave_flank'].includes(approach)||
    !['disturb_on_advantage','stay_passive'].includes(lylekBranch)||
    (approach==='frontal'&&!['fighting_retreat','fight_to_last'].includes(frontalOutcome))||
    (approach==='east_cave_flank'&&frontalOutcome!=='not_applicable'))
    throw new Error('SWE02 pp.16–18: registre aproximação, resposta frontal dos caçadores e decisão do ramo do lylek.');
  const receipt={approach,frontalOutcome,lylekBranch,cover:{rockPiles:true,cave:true,rangedSetback:2},map:{heroesJourney:'northern approach',hunterSpeeder:'cave mouth',cave:'north',eastFlank:'partially blocked eastern entrance',lylekNest:'southern nest'},
    source:{title:SOURCE.title,productCode:'SWE02',printedPage:16,relatedPrintedPages:[17,18],authority:SOURCE.authority}};
  const next=clone(p);next.oldLylek={...old,tactics:receipt};return {progress:next,receipt};
}

export function approachSWE02OldLylekOnFoot(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||!['lookout','bluff'].includes(old.phase))
    throw new Error('SWE02 p.17: abordagem a pé indisponível.');
  const next=clone(p),lookout=old.lookout||{attempted:false,spottedGand:false,source:clone(SOURCE)};
  next.oldLylek={...old,lookout,phase:'ruse',approachOnFoot:true,crashResolved:true};
  return {progress:next,phase:'combat-ready'};
}

export function recordSWE02OldLylekCrashCheck(progress,{characterId,characterName,skillKey,check}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='crash-checks'||!Array.isArray(old.crewPcIds)||!old.crewPcIds.includes(characterId)||!['aguante','coordinacion'].includes(skillKey)||!String(characterName||'').trim()||typeof check?.success!=='boolean')
    throw new Error('SWE02 p.17: escolha Aguante ou Coordenação e um personagem da tripulação.');
  if(old.crashChecks.some(x=>x.characterId===characterId))throw new Error('SWE02 p.17: este personagem já fez o teste do choque.');
  const receipt={characterId,characterName:String(characterName),skillKey,difficulty:2,success:check.success,damage:check.success?0:6,
    netSuccess:Number(check.netSuccess)||0,netAdvantage:Number(check.netAdvantage)||0,source:clone(SOURCE)};
  const next=clone(p);next.oldLylek={...old,crashChecks:[...old.crashChecks,receipt]};
  return {progress:next,receipt};
}

export function confirmSWE02OldLylekCrashCrew(progress,{expectedPcCount}={}) {
  const p=requireProgress(progress),old=p.oldLylek,n=Number(expectedPcCount);
  if(!old||old.phase!=='crash-checks'||!Number.isInteger(n)||n<1||n>8||old.crashChecks.length!==n||
    new Set(old.crashChecks.map(x=>x.characterId)).size!==n||!old.crewPcIds||old.crewPcIds.length!==n||
    old.crewPcIds.some(id=>!old.crashChecks.some(x=>x.characterId===id)))
    throw new Error('SWE02 p.17: cada PC precisa de seu teste de Aguante ou Coordenação antes do reparo.');
  const next=clone(p);next.oldLylek={...old,phase:'repair',crashResolved:true,expectedPcCount:n};
  return {progress:next,phase:'repair'};
}

export function recordSWE02OldLylekRepair(progress,check,{reductionMinutes=null}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||!['repair','repair-failed'].includes(old.phase)||typeof check?.success!=='boolean')throw new Error('SWE02 p.17: reparo não disponível.');
  const advantage=Math.max(0,Number(check.netAdvantage)||0)>0;
  if(reductionMinutes!==null&&(!advantage||!Number.isInteger(Number(reductionMinutes))||Number(reductionMinutes)<1||Number(reductionMinutes)>29))
    throw new Error('SWE02 p.17: redução escolhida pelo Mestre exige Vantagem e deve deixar ao menos um minuto de reparo.');
  const reduction=advantage&&reductionMinutes!==null?Number(reductionMinutes):null;
  const receipt={skillKey:'mecanica',difficulty:1,success:check.success,netSuccess:Number(check.netSuccess)||0,
    netAdvantage:Number(check.netAdvantage)||0,timeMinutes:reduction===null?30:30-reduction,
    advantageReduction:advantage?{status:reduction===null?'PENDING_GM_CHOICE':'APPLIED',sourceAllowsReduction:true,minutes:reduction,amountUnspecifiedByBook:true}:null,
    source:clone(SOURCE)};
  const next=clone(p),checks=[...old.repairChecks,receipt];
  next.oldLylek={...old,repairChecks:checks,phase:check.success?'ruse':'repair-failed',repairSuccess:check.success};
  return {progress:next,receipt,phase:next.oldLylek.phase};
}

export function markSWE02OldLylekCombatStarted(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='combat-ready'||!old.ruse||!old.tactics||old.huntersOutcome)throw new Error('SWE02 pp.16–18: ruse, mapa e decisão tática/lylek do Mestre precisam estar registrados antes dos caçadores.');
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
  const advantageTrigger=!old.lylekTriggered&&old.tactics?.lylekBranch==='disturb_on_advantage'&&(combatLog||[]).some(x=>Number(x?.check?.netAdvantage)>0||Number(x?.netAdvantage)>0);
  const huntersOutcome=outcome==='adversaries_defeated'&&defeated?'defeated':outcome==='player_incapacitated'?'party-incapacitated':'retreated';
  const next=clone(p);next.oldLylek={...old,phase:'post-hunters',huntersOutcome,lylekTriggered:old.lylekTriggered||advantageTrigger,
    hunterProof:{enemyIds:adversaries.map(a=>a.id),outcome,defeated,source:clone(SOURCE)}};
  if(advantageTrigger){const victim=adversaries.find(a=>!['incapacitated','defeated','retreated'].includes(a.status));if(victim)next.oldLylek.lylekKillReceipt={hunterId:victim.id,profileKey:victim.profileKey,disposition:'Killed by the lylek; prey taken to nest',trigger:'Any Advantage during subsequent hunter fight',source:{title:SOURCE.title,productCode:'SWE02',printedPage:18,authority:SOURCE.authority}};}
  next.flags={...(next.flags||{}),lylekHuntersResolved:true};
  if(huntersOutcome==='defeated')next.oldLylek.hunterLoot={status:'available',items:[{name:'stimpack',quantity:2},{name:'medpac',quantity:1},{name:'emergency repair kit',quantity:1}],source:{title:SOURCE.title,productCode:'SWE02',printedPage:18,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}};
  return {progress:next,huntersOutcome,lylekTriggered:next.oldLylek.lylekTriggered};
}

export function triggerSWE02OldLylekAdvantage(progress,{hunterId,triggerId}={}){
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='hunters-combat'||old.tactics?.lylekBranch!=='disturb_on_advantage'||old.lylekTriggered||typeof hunterId!=='string'||!hunterId)
    throw new Error('SWE02 p.18: vantagem do combate só pode acionar uma vez quando o Mestre escolheu perturbar o ninho.');
  const receipt={hunterId,triggerId:String(triggerId||''),disposition:'killed by lylek; prey taken to nest',event:'The lylek bursts from the southern nest, kills one hunter and retreats with its prey.',source:{title:SOURCE.title,productCode:'SWE02',printedPage:18,authority:SOURCE.authority}};
  const next=clone(p);next.oldLylek={...old,lylekTriggered:true,lylekKillReceipt:receipt};return {progress:next,receipt};
}

export function resolveSWE02OldLylekHuntersRetreat(progress,{enemyIds=[]}={}){
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='hunters-combat'||old.tactics?.approach!=='frontal'||old.tactics?.frontalOutcome!=='fighting_retreat'||!Array.isArray(enemyIds)||enemyIds.length<1||new Set(enemyIds).size!==enemyIds.length)
    throw new Error('SWE02 p.17: a retirada de combate exige abordagem frontal e escolha de retirada registrada.');
  const next=clone(p);next.oldLylek={...old,hunterRetreatReceipt:{status:'RESOLVED',route:'dotted map path',enemyIds:[...enemyIds],source:{title:SOURCE.title,productCode:'SWE02',printedPage:16,relatedPrintedPage:17,authority:SOURCE.authority}}};
  return {progress:next,receipt:clone(next.oldLylek.hunterRetreatReceipt)};
}

export function inspectSWE02OldLylekSpeeder(progress) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='post-hunters'||old.huntersOutcome!=='defeated'||old.hunterLoot?.status!=='available')
    throw new Error('SWE02 p.18: inspecione o speeder após derrotar os caçadores.');
  const next=clone(p);next.oldLylek={...old,hunterLoot:{...old.hunterLoot,status:'found',inspected:true}};return next;
}

export function recordSWE02OldLylekLootGrant(progress,{inventoryIds}={}){
  const p=requireProgress(progress),old=p.oldLylek,ids=['swe02-old-lylek-stimpack-1','swe02-old-lylek-stimpack-2','swe02-old-lylek-medpac','swe02-old-lylek-emergency-repair-kit'];
  if(old?.hunterLoot?.status!=='found'||old.hunterLoot.inventoryReceipt||!Array.isArray(inventoryIds)||
    inventoryIds.length!==ids.length||new Set(inventoryIds).size!==ids.length||ids.some(id=>!inventoryIds.includes(id)))
    throw new Error('SWE02 p.18: loot grant requires the four exact inventory entries and cannot be duplicated.');
  const next=clone(p);next.oldLylek={...old,hunterLoot:{...old.hunterLoot,inventoryReceipt:{status:'GRANTED',inventoryIds:ids,owner:'party.belongings',source:{title:SOURCE.title,productCode:'SWE02',printedPage:18,authority:SOURCE.authority}}}};
  return next;
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

export function resolveSWE02OldLylek(progress,{outcome,combatAdversary,gmConfirmedSubmission=false}={}) {
  const p=requireProgress(progress),old=p.oldLylek;
  if(!old||old.phase!=='lylek-attack-started'||!['player_incapacitated','adversaries_defeated','ended','escaped'].includes(outcome))throw new Error('SWE02 p.18: desfecho do ataque ao lylek indisponível.');
  if(outcome==='player_incapacitated'&&gmConfirmedSubmission!==true)
    throw new Error('SWE02 p.18: confirme explicitamente a submissão decidida pelo Mestre; o texto não define um limite numérico.');
  // The published text says the beast beats PCs into submission, then returns to feed.
  // It does not specify a mechanical winner/defeat threshold; retain the GM adjudication.
  const next=clone(p);next.oldLylek={...old,phase:'post-lylek',lylekResolution:{outcome,gmConfirmedSubmission:outcome==='player_incapacitated',numericSubmissionThreshold:null,beastProfileVerified:true,profileKey:old.lylekEncounter?.profileKey,source:{...clone(SOURCE),printedPage:18,profile:{title:'Edge of the Empire Beginner Game',productCode:'SWE01',printedPage:47,authority:'OFFICIAL_SOURCE_PROFILE_VERIFIED'}}}};return next;
}

export function canSWE02OldLylekContinue(progress) {
  const p=progress,old=p?.oldLylek;
  return Boolean(old&&['post-hunters','post-lylek','lylek-warned','lylek-ready'].includes(old.phase)&&old.huntersOutcome);
}
