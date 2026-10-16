/** SWE09 The Jewel of Yavin, Flight over Fight, printed pp.92–94.
 * Source-backed landscape clock tied to REAL combat rounds, not a synthetic
 * clock. No teleport, invented faction profile, or auto-awarded escape/XP.
 * Table 3-1 in the source overlaps on d100=47; require explicit GM choice.
 */
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','prototype','constructor'].includes(x);
const int=(x,a,b)=>Number.isSafeInteger(x)&&x>=a&&x<=b;
const SOURCE='SWE09 The Jewel of Yavin, Flight over Fight, pp.92–94, Table 3-1';
export const HOLO_PARK_FACTIONS=Object.freeze(['wing_guard','arend_goons','grayson_security']);
export const HOLO_PARK_RANGES=Object.freeze(['Engajado','Curto','Médio','Longo','Extremo']);
export const HOLO_PARK_LANDSCAPES=Object.freeze(['alderaan','christophsis','coruscant','kashyyyk','projectors_off']);
const KEYS=Object.freeze({
 JEWEL_HOLO_ENTER:['id','kind','version','source','gmParkArrivalConfirmed','gmPursuersConfirmed','factions','combatRound','roll100','landscape','gm47Choice'],
 JEWEL_HOLO_ROUND:['id','kind','version','source','round','gmDistanceConfirmed','range','gmPursuersNeutralizedConfirmed','roll100','landscape','gm47Choice','status'],
 JEWEL_HOLO_END:['id','kind','version','source','round','gmOutcomeConfirmed','outcome']
});
export function jewelHoloLandscapeFromD100(n,choice){
 return sceneFor(n,choice);
}
function sceneFor(n,choice){
 if(!int(n,1,100))throw Error('d100 do Holo-Park inválido.');
 if(n===47){if(!['christophsis','coruscant'].includes(choice))throw Error('Tabela 3-1 sobrepõe d100=47: Mestre deve escolher a paisagem publicada.');return choice;}
 if(choice!==null)throw Error('Escolha de desempate do Holo-Park não autorizada.');
 return n<=23?'alderaan':n<=46?'christophsis':n<=71?'coruscant':n<=95?'kashyyyk':'projectors_off';
}
function keys(r){if(!r||!KEYS[r.kind]||Object.keys(r).some(k=>!KEYS[r.kind].includes(k))||!validId(r.id)||r.version!==1||r.source!==SOURCE)throw Error('Recibo Holo-Park adulterado.');}
export function replayJewelHoloPark(progress){
 const receipts=progress?.operations?.receipts??{};let started=null,rounds=[],ended=null,landscape=null;
 for(const [id,r] of Object.entries(receipts)){
  if(!KEYS[r?.kind])continue;
  keys(r);if(id!==r.id)throw Error('ID do Holo-Park adulterado.');
  if(r.kind==='JEWEL_HOLO_ENTER'){
   if(started||rounds.length||ended||r.gmParkArrivalConfirmed!==true||r.gmPursuersConfirmed!==true||
      !int(r.combatRound,1,100000)||!Array.isArray(r.factions)||!r.factions.length||
      new Set(r.factions).size!==r.factions.length||r.factions.some(f=>!HOLO_PARK_FACTIONS.includes(f)))throw Error('Entrada do Holo-Park adulterada.');
   landscape=sceneFor(r.roll100,r.gm47Choice);if(r.landscape!==landscape)throw Error('Paisagem inicial adulterada.');started=clone(r);
  }else if(r.kind==='JEWEL_HOLO_ROUND'){
   if(!started||ended||rounds.length>=10||r.round!==started.combatRound+rounds.length||
      r.round-started.combatRound>=10||r.gmDistanceConfirmed!==true||!HOLO_PARK_RANGES.includes(r.range)||
      typeof r.gmPursuersNeutralizedConfirmed!=='boolean')throw Error('Sequência real das rodadas do Holo-Park adulterada.');
   const index=rounds.length+1;
   if(index>1&&index%2===1){landscape=sceneFor(r.roll100,r.gm47Choice);}else if(r.roll100!==null||r.gm47Choice!==null)throw Error('Paisagem trocada antes de duas rodadas.');
   if(r.landscape!==landscape)throw Error('Paisagem do Holo-Park adulterada.');
   const status=r.gmPursuersNeutralizedConfirmed||['Longo','Extremo'].includes(r.range)?'ESCAPE_PENDING_GM':index===10?'ROUND_LIMIT_PENDING_GM':'ACTIVE';
   if(r.status!==status)throw Error('Resultado de fuga do Holo-Park adulterado.');rounds.push(clone(r));
  }else{
   const last=rounds.at(-1);
   if(!last||ended||!['ESCAPE_PENDING_GM','ROUND_LIMIT_PENDING_GM'].includes(last.status)||
      r.round!==last.round||r.gmOutcomeConfirmed!==true||
      !['ESCAPED','STILL_PURSUED'].includes(r.outcome)||
      (last.status==='ESCAPE_PENDING_GM'&&r.outcome!=='ESCAPED'))throw Error('Desfecho do Holo-Park sem condições oficiais.');
   ended=clone(r);
  }
 }
 if(!started)return null;
 return {version:1,started,rounds,landscape,roundCount:rounds.length,range:rounds.at(-1)?.range??null,
  status:ended?.outcome??rounds.at(-1)?.status??'ACTIVE',endReceipt:ended};
}
export function validateJewelHoloPark(progress){const expected=replayJewelHoloPark(progress);if(!eq(expected,progress?.operations?.holoPark??null))throw Error('Estado Holo-Park diverge dos recibos.');return expected;}
function preflight(world,combat,id){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
    !['DISARMED','JUMPED','JUMPED_WITH_JEWEL','JEWEL_DROPPED','FELL','EXPLODED'].includes(p.operations?.escapeBomb?.status))
  throw Error('Holo-Park exige a fuga de Cloud City iniciada na aventura.');
 if(!combat?.active||!int(combat.round,1,100000))throw Error('Holo-Park exige rodada de combate REAL em andamento.');
 if(!validId(id)||p.operations?.receipts?.[id])throw Error('ID do Holo-Park inválido ou repetido.');
 p.operations??={};p.operations.receipts??={};validateJewelHoloPark(p);return {w,p};
}
function d100(rng){const n=Number(rng());if(!Number.isFinite(n)||n<0||n>=1)throw Error('Fonte d100 inválida.');return Math.floor(n*100)+1;}
export function enterJewelHoloPark(world,combat,details={}){
 const {w,p}=preflight(world,combat,details.actionId);
 if(p.operations.holoPark)throw Error('Holo-Park já iniciado.');
 if(p.operations.holoNarrativeClock&&!p.operations.holoNarrativeClock.ended)throw Error('Encerre primeiro o modo narrativo do Holo-Park.');
 if(details.gmParkArrivalConfirmed!==true||details.gmPursuersConfirmed!==true)throw Error('Mestre deve confirmar chegada física e facções perseguidoras.');
 const factions=details.factions;
 if(!Array.isArray(factions)||!factions.length||new Set(factions).size!==factions.length||factions.some(f=>!HOLO_PARK_FACTIONS.includes(f)))throw Error('Facções oficiais do Holo-Park não confirmadas.');
 const roll100=d100(details.rng??Math.random),gm47Choice=roll100===47?(details.gm47Choice??null):null;
 const r={id:details.actionId,kind:'JEWEL_HOLO_ENTER',version:1,source:SOURCE,gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,
  factions:[...factions],combatRound:combat.round,roll100,landscape:sceneFor(roll100,gm47Choice),gm47Choice};
 p.operations.receipts[r.id]=clone(r);p.operations.holoPark=replayJewelHoloPark(p);validateJewelHoloPark(p);
 return {world:w,adjudication:r,holoPark:clone(p.operations.holoPark)};
}
export function recordJewelHoloParkRound(world,combat,details={}){
 const {w,p}=preflight(world,combat,details.actionId),prior=p.operations.holoPark;
 if(!prior||prior.status!=='ACTIVE')throw Error('Holo-Park não iniciado ou desfecho pendente.');
 const offset=combat.holoParkRoundOffset??0;
 if(!int(offset,0,100000)||offset&& !['holo-park-arend-goons','holo-park-grayson-security'].includes(combat.publishedAdventureContext?.encounterId))
  throw Error('Relógio de combate do Holo-Park inválido.');
 const effectiveRound=combat.round+offset;
 if(effectiveRound!==prior.started.combatRound+prior.rounds.length)throw Error('Use a rodada real do combate, sem pular turnos.');
 if(details.gmDistanceConfirmed!==true||!HOLO_PARK_RANGES.includes(details.range))throw Error('Mestre deve confirmar distância após movimentação real, sem teleporte.');
 if(typeof details.gmPursuersNeutralizedConfirmed!=='boolean')throw Error('Situação dos perseguidores não confirmada.');
 const index=prior.rounds.length+1,rotate=index>1&&index%2===1;
 const roll100=rotate?d100(details.rng??Math.random):null;
 const gm47Choice=roll100===47?(details.gm47Choice??null):null;
 const landscape=rotate?sceneFor(roll100,gm47Choice):prior.landscape;
 const status=details.gmPursuersNeutralizedConfirmed||['Longo','Extremo'].includes(details.range)?'ESCAPE_PENDING_GM':index===10?'ROUND_LIMIT_PENDING_GM':'ACTIVE';
 const r={id:details.actionId,kind:'JEWEL_HOLO_ROUND',version:1,source:SOURCE,round:effectiveRound,
  gmDistanceConfirmed:true,range:details.range,gmPursuersNeutralizedConfirmed:details.gmPursuersNeutralizedConfirmed,
  roll100,landscape,gm47Choice,status};
 p.operations.receipts[r.id]=clone(r);p.operations.holoPark=replayJewelHoloPark(p);validateJewelHoloPark(p);
 return {world:w,adjudication:r,holoPark:clone(p.operations.holoPark)};
}
export function confirmJewelHoloParkOutcome(world,details={}){
 const w=clone(world),p=w?.publishedAdventure,prior=validateJewelHoloPark(p);
 if(p?.id!=='jewel-of-yavin'||!prior||!['ESCAPE_PENDING_GM','ROUND_LIMIT_PENDING_GM'].includes(prior.status)||
    details.gmOutcomeConfirmed!==true||!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('Desfecho Holo-Park não autorizado.');
 const outcome=prior.status==='ESCAPE_PENDING_GM'?'ESCAPED':details.outcome;
 if(!['ESCAPED','STILL_PURSUED'].includes(outcome)||prior.status==='ROUND_LIMIT_PENDING_GM'&&details.outcome!==outcome)
  throw Error('Mestre deve confirmar se a fuga realmente ocorreu após dez rodadas.');
 const r={id:details.actionId,kind:'JEWEL_HOLO_END',version:1,source:SOURCE,round:prior.rounds.at(-1).round,gmOutcomeConfirmed:true,outcome};
 p.operations.receipts[r.id]=clone(r);p.operations.holoPark=replayJewelHoloPark(p);validateJewelHoloPark(p);
 return {world:w,adjudication:r,holoPark:clone(p.operations.holoPark)};
}
