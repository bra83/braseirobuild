import { buildNarrativeCheckpoint } from '../../TEMP_ROOT/lib/narrativeEngine';
import { interpretOneRingObservation } from '../../TEMP_ROOT/lib/dice';
import { adjustEndurance, adjustHope, adjustShadow, adjustFatigue } from '../../TEMP_ROOT/lib/rules/hopeShadow';
import { spendFellowship } from '../../TEMP_ROOT/lib/rules/fellowship';
import { recordJourneyMove, resolveJourneyMarch, validateJourneyRoles, journeyTravelDaysForTerrain } from '../../TEMP_ROOT/lib/rules/journey';
import { advanceSkill, advanceCombatProficiency } from '../../TEMP_ROOT/lib/rules/progression';
import { normalizeEquipmentMechanics } from '../../TEMP_ROOT/lib/rules/equipment';
import { firmWill, resolveBoutOfMadness } from '../../TEMP_ROOT/lib/rules/shadow';
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

const roleErrors=validateJourneyRoles({guide:'h'},['h']);
check('journey-four-roles-required',roleErrors.length===3,'Jornada exige Guia, Batedor, Cacador e Vigia.');

const marched=resolveJourneyMarch(state,true,2);
check('journey-march-success-distance',marched.journey.marchHexesRemaining===1 && marched.journey.marchRollCount===1,'Marcha bem-sucedida respeita rota restante e registra a jogada.');

check('journey-difficult-terrain-time',journeyTravelDaysForTerrain('forest')===2 && journeyTravelDaysForTerrain('road')===1,'Terreno dificil custa dois dias; estrada normal custa um.');

const progressionSheet:any={...sheet,skills:[{name:'Viagem',rank:1,specialization:'',favorite:false}],resources:{coins:0,treasure:0,experience:0,adventurePoints:20,skillPoints:20},combat:{...sheet.combat,proficiencies:{Espadas:1},weapons:[]},inventory:[]};
const advSkill=advanceSkill(progressionSheet,'Viagem');
check('progression-skill-cost-deducted',advSkill.allowed===true && advSkill.cost===8 && advSkill.sheet.skills[0].rank===2 && advSkill.sheet.resources.skillPoints===12,'Elevar pericia para 2 custa 8 pontos e persiste o novo nivel.');

const advCombat=advanceCombatProficiency(progressionSheet,'Espadas');
check('progression-combat-cost-deducted',advCombat.allowed===true && advCombat.cost===8 && advCombat.sheet.combat.proficiencies.Espadas===2 && advCombat.sheet.resources.adventurePoints===12,'Elevar proficiencia para 2 custa 8 Pontos de Aventura.');

const equipmentSheet:any={...progressionSheet,attributes:{...progressionSheet.attributes,enduranceCurrent:20,fatigue:0,load:0},combat:{...progressionSheet.combat,parry:2,weapons:[]},inventory:[{id:'armor',name:'Camisa de couro',category:'Armadura',quantity:1,load:99,weight:99,equipped:true,tags:[]}]};
const eq=normalizeEquipmentMechanics(equipmentSheet);
check('equipment-derived-load-protection',eq.attributes.load===3 && eq.attributes.protection===1,'Carga e Protecao sao recalculadas pelo equipamento canonico, ignorando valor manual incorreto.');

const shadowSheet:any={...progressionSheet,attributes:{...progressionSheet.attributes,hopeCurrent:5,hopeMax:10,shadow:3,shadowScars:1},other:{...progressionSheet.other,shadowPath:'Caminho do Desespero',flaws:''}};
const firm=firmWill(shadowSheet);
check('shadow-firm-will-scar',firm.allowed===true && firm.sheet.attributes.shadow===2 && firm.sheet.attributes.shadowScars===2,'Firme Vontade converte Sombra em cicatriz permanente.');

const madnessInput:any={...shadowSheet,attributes:{...shadowSheet.attributes,shadow:10,shadowScars:1},other:{...shadowSheet.other,shadowPathStep:0}};
const madness=resolveBoutOfMadness(madnessInput);
check('shadow-bout-adds-flaw',madness.allowed===true && madness.retired===false && madness.flaw==='Perturbado' && madness.sheet.attributes.shadow===1,'Crise de Loucura avanca o Caminho da Sombra e reseta Sombra ate cicatrizes.');


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

const narrativeCampaign:any={
  id:'c2',currentLocation:'A',history:[],presentNpcIds:[],clues:'',recentEvents:'',
  masterState:{...normalized,combat:{...normalized.combat,actors:[{...normalized.combat.actors[0],position:{x:0.35,y:0.5}}]}}
};
const checkpoint=buildNarrativeCheckpoint(narrativeCampaign,[]);
check('combat-master-context-precise',checkpoint.positions[0]?.includes('x=35%')===true && checkpoint.positions[0]?.includes('y=50%')===true,'Contexto enviado ao Mestre preserva a posicao tatica em percentuais legiveis.');

console.log(JSON.stringify(out));
