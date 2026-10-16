/** SWE09 p.90 and EotE Core, The Chase (vehicle chapter): competitive Piloting
 * checks at the beginning of each round. Airspeeder pursuit uses planetary
 * range; a win shifts one band, plus relative speed difference if VERIFIED.
 * No speed, armor, hull, pilot skill, or movement duration is inferred from
 * the incomplete published Arrow-7 vehicle profile.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
const clone=structuredClone;
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','prototype','constructor'].includes(x);
const int=(x,min,max)=>Number.isSafeInteger(x)&&x>=min&&x<=max;
export const ARROW7_CHASE_RANGES=Object.freeze(['Bocajarro','Curto','Médio','Longo','Extremo']);
export const ARROW7_CHASE_SOURCE=Object.freeze({adventure:'SWE09 The Jewel of Yavin, p.90',chase:'Edge of the Empire Core Rulebook, The Chase (vehicle chapter)',scale:'planetary'});
const KEYS=new Set(['id','kind','contractVersion','source','encounterId','round','playerRole','playerPilot','arrow7Pilot','terrainDifficulty','terrainSetback','gmTerrainConfirmed','gmBothVehiclesConfirmed','gmStartingRangeConfirmed','initialRange','previousRange','playerSpeed','arrow7Speed','gmSpeedsVerified','checks','winner','bandShift','newRange','status']);
const FACE_KEYS=['ability','proficiency','difficulty','challenge','boost','setback'];
function pilot(x){return x&&validId(x.id)&&int(x.agility,0,7)&&int(x.rank,0,7)&&x.skill==='pilotar_vehiculo_planetario'&&typeof x.provenance==='string'&&x.provenance.length>0&&x.provenance.length<=120;}
function checkEvidence(e,p,difficulty,setback){
 const expected={...buildPositivePool(p.agility,p.rank),difficulty,challenge:0,boost:0,setback};
 if(!e||!same(e.pool,expected)||!e.roll||!same(e.roll.pool,expected)||!Array.isArray(e.roll.faces))throw Error('Pool de perseguição adulterado.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let i=0;
 for(const type of FACE_KEYS)for(let j=0;j<expected[type];j++){
  const f=e.roll.faces[i++],faces=diceFacesForTests[type];
  if(!f||f.type!==type||!int(f.index,0,faces.length-1)||!same(f.face,faces[f.index]))throw Error('Face da perseguição adulterada.');
  for(const [k,n] of Object.entries(f.face))raw[k]+=n;
 }
 if(i!==e.roll.faces.length)throw Error('Número de dados da perseguição adulterado.');
 const expectedSymbols=resolveSymbols(raw);
 for(const k of ['success','netSuccess','netAdvantage','triumph','despair','raw'])if(!same(e.roll[k],expectedSymbols[k]))throw Error('Símbolos da perseguição adulterados.');
 return expectedSymbols;
}
function result(previous,r){
 if(!r||r.kind!=='JEWEL_ARROW7_CHASE'||r.contractVersion!==1||!validId(r.id)||r.encounterId!=='arend-shen-four-goons'||!same(r.source,ARROW7_CHASE_SOURCE)||
 !int(r.round,1,100000)||!['evader','pursuer'].includes(r.playerRole)||!pilot(r.playerPilot)||!pilot(r.arrow7Pilot)||
 r.arrow7Pilot.provenance!=='GM_CONFIRMED_NPC_PILOT_SHEET'||r.gmTerrainConfirmed!==true||r.gmBothVehiclesConfirmed!==true||
 !int(r.terrainDifficulty,0,5)||!int(r.terrainSetback,0,6)||r.gmStartingRangeConfirmed!==true||
 !['Bocajarro','Curto','Médio','Longo','Extremo'].includes(r.initialRange)||!['Bocajarro','Curto','Médio','Longo','Extremo'].includes(r.previousRange)||
 !r.checks||!r.checks.player||!r.checks.arrow7||!Object.keys(r).every(k=>KEYS.has(k)))throw Error('Recibo da perseguição adulterado.');
 const verified=r.gmSpeedsVerified===true;
 if(![true,false].includes(r.gmSpeedsVerified)||verified!== (r.playerSpeed!==null&&r.arrow7Speed!==null)||
 (verified&&(!int(r.playerSpeed,0,6)||!int(r.arrow7Speed,0,6)))||(!verified&&(r.playerSpeed!==null||r.arrow7Speed!==null)))throw Error('Velocidade da perseguição não comprovada.');
 const p=checkEvidence(r.checks.player,r.playerPilot,r.terrainDifficulty,r.terrainSetback);
 const a=checkEvidence(r.checks.arrow7,r.arrow7Pilot,r.terrainDifficulty,r.terrainSetback);
 // Source compares total net successes. Equal scores do NOT move the vehicles.
 const ps=Math.max(0,p.netSuccess),as=Math.max(0,a.netSuccess);
 const winner=ps===as?'tie':ps>as?'player':'arrow7';
 const pursuerWins=(winner==='player'&&r.playerRole==='pursuer')||(winner==='arrow7'&&r.playerRole==='evader');
 const winnerSpeed=winner==='player'?r.playerSpeed:r.arrow7Speed;
 const loserSpeed=winner==='player'?r.arrow7Speed:r.playerSpeed;
 const shift=winner==='tie'?0:1+(verified?Math.max(0,winnerSpeed-loserSpeed):0);
 const start=ARROW7_CHASE_RANGES.indexOf(r.previousRange);
 const dest=start+(winner==='tie'?0:pursuerWins?-shift:shift);
 const newRange=ARROW7_CHASE_RANGES[Math.max(0,Math.min(4,dest))];
 const status=dest<=0&&winner!=='tie'?'CONTACT_PENDING_GM':dest>4?'ESCAPE_PENDING_GM':'ACTIVE';
 if(r.winner!==winner||r.bandShift!==shift||r.newRange!==newRange||r.status!==status)throw Error('Resultado da perseguição adulterado.');
 if(previous){if(previous.status!=='ACTIVE'||r.round<=previous.round||r.previousRange!==previous.newRange||r.initialRange!==previous.initialRange||r.playerRole!==previous.playerRole||r.playerPilot.id!==previous.playerPilot.id||r.arrow7Pilot.id!==previous.arrow7Pilot.id)throw Error('Sequência da perseguição adulterada.');}
 else if(r.previousRange!==r.initialRange)throw Error('Distância inicial da perseguição adulterada.');
 return r;
}
export function replayJewelArrow7Chase(progress){
 const receipts=progress?.operations?.receipts??{},history=[];let endReceipt=null;
 for(const [id,r] of Object.entries(receipts)){
  if(r?.kind==='JEWEL_ARROW7_CHASE'){
   if(endReceipt||!validId(id)||r.id!==id)throw Error('ID ou ordem de perseguição adulterados.');
   result(history.at(-1)??null,r);history.push(clone(r));
  }else if(r?.kind==='JEWEL_ARROW7_CHASE_END'){
   const last=history.at(-1),expected=last?.status==='CONTACT_PENDING_GM'?'CONTACT':last?.status==='ESCAPE_PENDING_GM'?'ESCAPED':null;
   if(endReceipt||!last||!expected||!validId(id)||r.id!==id||r.gmConfirmed!==true||r.contractVersion!==1||
    r.outcome!==expected||r.chaseRound!==last.round||r.range!==last.newRange||!same(r.source,ARROW7_CHASE_SOURCE)||
    Object.keys(r).some(k=>!['id','kind','contractVersion','source','chaseRound','range','outcome','gmConfirmed'].includes(k)))
    throw Error('Desfecho da perseguição adulterado.');
   endReceipt=clone(r);
  }
 }
 return history.length?{contractVersion:1,history,round:history.at(-1).round,initialRange:history[0].initialRange,range:history.at(-1).newRange,
  status:endReceipt?.outcome??history.at(-1).status,playerRole:history[0].playerRole,...(endReceipt?{endReceipt}:{})}:null;
}
export function validateJewelArrow7Chase(progress){
 const actual=progress?.operations?.arrow7Chase??null,expected=replayJewelArrow7Chase(progress);
 if(!same(actual,expected))throw Error('Estado da perseguição diverge dos recibos.');return expected;
}
export function resolveJewelArrow7Chase(world,combat,playerCharacter,details={}){
 const w=clone(world),c=clone(combat),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!c?.active||c.publishedAdventureContext?.encounterId!=='arend-shen-four-goons')throw Error('Perseguição somente no encontro oficial dos quatro capangas.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID da perseguição duplicado ou inválido.');
 if(!int(c.round,1,100000)||c.encounter?.actedActorIds?.length)throw Error('Teste competitivo deve ocorrer no início da rodada, antes de qualquer turno.');
 const prior=validateJewelArrow7Chase(p);
 if(prior&&prior.round>=c.round)throw Error('Perseguição já resolvida nesta rodada.');
 if(prior&&prior.status!=='ACTIVE')throw Error('Resultado da perseguição aguarda decisão do Mestre.');
 if(details.gmBothVehiclesConfirmed!==true||details.gmTerrainConfirmed!==true||details.gmStartingRangeConfirmed!==true)throw Error('Mestre deve confirmar dois veículos, terreno e distância planetária real.');
 const playerId=String(playerCharacter?.id??playerCharacter?.name??'');
 if(!validId(playerId))throw Error('Piloto do jogador sem ficha real.');
 const playerPilot={id:playerId,agility:playerCharacter?.characteristics?.agilidad,rank:playerCharacter?.skills?.pilotar_vehiculo_planetario,skill:'pilotar_vehiculo_planetario',provenance:'CURRENT_PLAYER_CHARACTER'};
 const npc=details.arrow7Pilot??{};
 const arrow7Pilot={id:npc.id,agility:npc.agility,rank:npc.rank,skill:'pilotar_vehiculo_planetario',provenance:'GM_CONFIRMED_NPC_PILOT_SHEET'};
 if(!pilot(playerPilot)||!pilot(arrow7Pilot)||details.gmNpcPilotSheetConfirmed!==true)throw Error('Ficha real do piloto do jogador e ficha confirmada do piloto NPC são obrigatórias; não inventar perícia.');
 const role=prior?.playerRole??details.playerRole;
 if(!['evader','pursuer'].includes(role)||prior&&role!==details.playerRole)throw Error('Papel da perseguição inválido.');
 const initial=prior?.initialRange??details.initialRange;
 if(!ARROW7_CHASE_RANGES.includes(initial)||prior&&details.initialRange!==initial)throw Error('Distância inicial planetária inválida.');
 const terrainDifficulty=details.terrainDifficulty,terrainSetback=details.terrainSetback;
 if(!int(terrainDifficulty,0,5)||!int(terrainSetback,0,6))throw Error('Dificuldade de terreno deve ser confirmada pelo Mestre.');
 const speedsVerified=details.gmSpeedsVerified===true;
 const playerSpeed=speedsVerified?details.playerSpeed:null,arrow7Speed=speedsVerified?details.arrow7Speed:null;
 if(speedsVerified&&(!int(playerSpeed,0,6)||!int(arrow7Speed,0,6)))throw Error('Velocidades atuais devem ser verificadas, não inventadas.');
 const pool=who=>({...buildPositivePool(who.agility,who.rank),difficulty:terrainDifficulty,challenge:0,boost:0,setback:terrainSetback});
 const rng=details.rng??Math.random;
 const checks={player:{pool:pool(playerPilot)},arrow7:{pool:pool(arrow7Pilot)}};
 checks.player.roll=rollPool(checks.player.pool,rng);checks.arrow7.roll=rollPool(checks.arrow7.pool,rng);
 const r={id:details.actionId,kind:'JEWEL_ARROW7_CHASE',contractVersion:1,source:clone(ARROW7_CHASE_SOURCE),encounterId:'arend-shen-four-goons',round:c.round,playerRole:role,playerPilot,arrow7Pilot,terrainDifficulty,terrainSetback,gmTerrainConfirmed:true,gmBothVehiclesConfirmed:true,gmStartingRangeConfirmed:true,initialRange:initial,previousRange:prior?.range??initial,playerSpeed,arrow7Speed,gmSpeedsVerified:speedsVerified,checks,winner:null,bandShift:0,newRange:null,status:null};
 // Derive result from actual faces, never from a caller-supplied success.
 const player=checks.player.roll.netSuccess,arrow=checks.arrow7.roll.netSuccess;
 r.winner=Math.max(0,player)===Math.max(0,arrow)?'tie':player>arrow?'player':'arrow7';
 const chasing=(r.winner==='player'&&role==='pursuer')||(r.winner==='arrow7'&&role==='evader');
 r.bandShift=r.winner==='tie'?0:1+(speedsVerified?Math.max(0,(r.winner==='player'?playerSpeed:arrow7Speed)-(r.winner==='player'?arrow7Speed:playerSpeed)):0);
 const dest=ARROW7_CHASE_RANGES.indexOf(r.previousRange)+(r.winner==='tie'?0:chasing?-r.bandShift:r.bandShift);
 r.newRange=ARROW7_CHASE_RANGES[Math.max(0,Math.min(4,dest))];
 r.status=dest<=0&&r.winner!=='tie'?'CONTACT_PENDING_GM':dest>4?'ESCAPE_PENDING_GM':'ACTIVE';
 result(prior?.history.at(-1)??null,r);
 p.operations.receipts[r.id]=clone(r);p.operations.arrow7Chase=replayJewelArrow7Chase(p);validateJewelArrow7Chase(p);
 c.arrow7Chase={round:c.round,range:r.newRange,status:r.status,receiptId:r.id,scale:'planetary'};
 c.log=[...(c.log??[]),{type:'ARROW7_CHASE_COMPETITIVE',round:c.round,receiptId:r.id,range:r.newRange,winner:r.winner,status:r.status}];
 return {world:w,combat:c,adjudication:r,chase:clone(p.operations.arrow7Chase)};
}

/** The printed chase rules leave the final contact/escape adjudication to GM. */
export function confirmJewelArrow7ChaseOutcome(world,combat,details={}){
 const w=clone(world),c=clone(combat),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!c?.active||
 c.publishedAdventureContext?.encounterId!=='arend-shen-four-goons')throw Error('Desfecho fora da perseguição oficial ativa.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID de desfecho duplicado ou inválido.');
 const state=validateJewelArrow7Chase(p),outcome=state?.status==='CONTACT_PENDING_GM'?'CONTACT':state?.status==='ESCAPE_PENDING_GM'?'ESCAPED':null;
 if(!outcome||details.gmConfirmed!==true||details.outcome!==outcome)throw Error('Desfecho requer contato/fuga real e confirmação expressa do Mestre.');
 const r={id:details.actionId,kind:'JEWEL_ARROW7_CHASE_END',contractVersion:1,source:clone(ARROW7_CHASE_SOURCE),chaseRound:state.round,range:state.range,outcome,gmConfirmed:true};
 p.operations.receipts[r.id]=clone(r);p.operations.arrow7Chase=replayJewelArrow7Chase(p);validateJewelArrow7Chase(p);
 c.arrow7Chase={round:state.round,range:state.range,status:outcome,receiptId:r.id,scale:'planetary'};
 c.log=[...(c.log??[]),{type:'ARROW7_CHASE_GM_OUTCOME',round:c.round,receiptId:r.id,outcome,range:state.range}];
 return {world:w,combat:c,adjudication:r,chase:clone(p.operations.arrow7Chase)};
}
