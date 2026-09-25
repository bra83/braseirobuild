import { interpretOneRingObservation } from '../../TEMP_ROOT/lib/dice';
import { adjustEndurance, adjustHope, adjustShadow, adjustFatigue } from '../../TEMP_ROOT/lib/rules/hopeShadow';
import { spendFellowship } from '../../TEMP_ROOT/lib/rules/fellowship';
import { recordJourneyMove } from '../../TEMP_ROOT/lib/rules/journey';
import { moveCombatActor, startCombat, upsertCombatActor } from '../../TEMP_ROOT/lib/rules/combat';
import { normalizeMasterState } from '../../TEMP_ROOT/lib/master/state';

const out:any[]=[];
function check(id:string, ok:boolean, detail:string){ out.push({id,status:ok?'PASSOU':'FALHOU',detail}); if(!ok) process.exitCode=1; }

let r=interpretOneRingObservation([12],[],20,'normal',false,0,false);
check('dice-gandalf-auto-success',r.success===true && r.featResult===12,'Runa de Gandalf vence TN alto sem dado de sucesso.');

r=interpretOneRingObservation([11],[6,6,6],5,'normal',false,0,true);
check('dice-eye-miserable-auto-fail',r.success===false,'Arrasado + Olho de Sauron falha mesmo com soma alta.');

r=interpretOneRingObservation([5],[1,2,3,4],14,'normal',true,0,false);
check('dice-weary-zeroes-1-3',r.total===9,'Cansado ignora 1-3 nos dados de sucesso.');

const fav=interpretOneRingObservation([11,8],[],14,'favoured',false,0,false);
const ill=interpretOneRingObservation([11,8],[],14,'ill-favoured',false,0,false);
check('dice-favoured-illfavoured-selection',fav.featResult===8 && ill.featResult===11,'Favorecido pega melhor; desfavorecido pega pior, com Olho ranqueado abaixo.');

const sheet:any={attributes:{strength:5,heart:4,wits:3,enduranceCurrent:10,enduranceMax:20,hopeCurrent:5,hopeMax:10,shadow:2,fatigue:3,load:7,weary:false,miserable:false,tnModifiers:{}},skills:[],combat:{},identity:{},other:{}};

let s=adjustEndurance(sheet,-1);
check('condition-weary-recomputed',s.attributes.enduranceCurrent===9 && s.attributes.weary===true,'Endurance mudou e Cansado foi recalculado contra carga+fadiga.');

s=adjustShadow(adjustHope(sheet,-4),3);
check('condition-miserable-recomputed',s.attributes.hopeCurrent===1 && s.attributes.shadow===5 && s.attributes.miserable===true,'Esperanca/Sombra recalculam Arrasado.');

s=adjustFatigue(sheet,-99);
check('fatigue-never-negative',s.attributes.fatigue===0,'Fadiga nao fica negativa.');

const state:any={activeMode:'adventure',worldClock:{year:2965,season:'spring',day:1,hour:8,minute:0,elapsedMinutes:480},position:{currentHexId:'A',locationName:'A',routeHexIds:['A']},journey:{status:'travelling',segments:[],pendingEvent:null,marchHexesRemaining:1,plannedRouteHexIds:['B']},company:{fellowshipCurrent:1},combat:{actors:[{actorId:'h',position:{x:.5,y:.5}}],updatedAt:0},updatedAt:0};

const spent=spendFellowship(state,2);
check('fellowship-insufficient-no-mutation',spent.spent===false && spent.state===state,'Sociedade insuficiente nao altera estado.');


let blocked=false;
try { recordJourneyMove({...state,journey:{...state.journey,marchHexesRemaining:0}},'B','B',10,'road'); } catch { blocked=true; }
check('journey-march-gate-blocks-free-move',blocked,'Jornada ativa sem Marcha disponível rejeita avanço de hex.');

const moved=recordJourneyMove(state,'B','B',10,'road');
const moved2=recordJourneyMove(moved,'B','B',0,'road');
check('journey-route-no-consecutive-duplicate',moved2.position.routeHexIds.join(',')==='A,B','Movimento persiste rota sem duplicar hex consecutivo.');

const movedCombat=moveCombatActor(state,'h',-4,9);
const p=movedCombat.combat.actors[0].position;
check('combat-position-clamped',p.x===0.03 && p.y===0.09,'Posicao normalizada permanece dentro do tabuleiro.');

const legacyMoved=moveCombatActor(state,'h',35,50);
const lp=legacyMoved.combat.actors[0].position;
check('combat-legacy-percent-normalized',lp.x===0.35 && lp.y===0.5,'Coordenadas legadas em percentual sao convertidas para 0..1.');

const started=startCombat({...state,combat:{...state.combat,actors:[]}},[{id:'h',sheet} as any],['h']);
const sp=started.combat.actors[0].position!;
check('combat-start-position-normalized',sp.x>=0.03 && sp.x<=0.97 && sp.y>=0.05 && sp.y<=0.95,'Inicio de combate cria tokens em coordenadas 0..1.');

const upserted=upsertCombatActor(state,{...state.combat.actors[0],position:{x:72,y:24}} as any);
const up=upserted.combat.actors[0].position!;
check('combat-upsert-legacy-normalized',up.x===0.72 && up.y===0.24,'Upsert converte posicoes percentuais antigas.');

const campaign:any={
  id:'c',currentLocation:'A',campaignSummary:'',partyCharacterIds:['h'],activeCharacterId:'h',
  masterState:{...state,combat:{...state.combat,actors:[{...state.combat.actors[0],position:{x:35,y:50}}]}}
};
const normalized=normalizeMasterState(campaign);
const np=normalized.combat.actors[0].position!;
check('combat-save-migration',np.x===0.35 && np.y===0.5,'Save legado com posicao 0..100 migra para 0..1 ao reabrir.');

console.log(JSON.stringify(out));
