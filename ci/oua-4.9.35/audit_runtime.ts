import { interpretOneRingObservation } from '../../TEMP_ROOT/lib/dice';
import { adjustEndurance, adjustHope, adjustShadow, adjustFatigue } from '../../TEMP_ROOT/lib/rules/hopeShadow';
import { spendFellowship } from '../../TEMP_ROOT/lib/rules/fellowship';
import { recordJourneyMove } from '../../TEMP_ROOT/lib/rules/journey';
import { moveCombatActor } from '../../TEMP_ROOT/lib/rules/combat';

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

const state:any={activeMode:'adventure',position:{currentHexId:'A',locationName:'A',routeHexIds:['A']},journey:{status:'travelling',segments:[],pendingEvent:null,marchHexesRemaining:1},company:{fellowshipCurrent:1},combat:{actors:[{actorId:'h',position:{x:.5,y:.5}}],updatedAt:0},updatedAt:0};

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
check('combat-position-clamped',p.x===0.03 && p.y===0.95,'Posicao de combate e limitada ao tabuleiro.');

console.log(JSON.stringify(out));
