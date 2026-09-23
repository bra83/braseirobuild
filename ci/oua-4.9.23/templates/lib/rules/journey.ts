import type { MasterState, JourneyRole } from '../master/types';
import { advanceWorldClock } from '../master/worldClock';

export const JOURNEY_ROLE_SKILLS: Record<JourneyRole,string> = {
  guide:'Viagem', scout:'Exploração', hunter:'Caçada', lookout:'Vigilância',
};
export const JOURNEY_ROLE_LABELS: Record<JourneyRole,string> = {
  guide:'Guia', scout:'Batedor', hunter:'Caçador', lookout:'Vigia',
};

const DIFFICULT_TERRAINS=new Set(['hills','hill','forest','dense_forest','woods','wood','marsh','swamp','waste','ruins','mountains','mountain','water']);
function terrainKey(value?:string):string{return String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim().replace(/[ -]+/g,'_');}
export function journeyTravelDaysForTerrain(terrain?:string):number{return DIFFICULT_TERRAINS.has(terrainKey(terrain))?2:1;}
export function isDifficultJourneyTerrain(terrain?:string):boolean{return journeyTravelDaysForTerrain(terrain)>1;}
export function isRoadTerrain(terrain?:string):boolean{return /road|estrada|caminho/.test(terrainKey(terrain));}

export function validateJourneyRoles(roles:Partial<Record<JourneyRole,string>>,partyIds:string[]):string[]{
  const errors:string[]=[]; const party=new Set(partyIds); const required:JourneyRole[]=['guide','scout','hunter','lookout'];
  for(const role of required){
    const id=roles[role];
    if(!id) errors.push('A Companhia precisa definir '+JOURNEY_ROLE_LABELS[role]+' ('+JOURNEY_ROLE_SKILLS[role]+').');
    else if(!party.has(id)) errors.push(JOURNEY_ROLE_LABELS[role]+': personagem fora da Companhia.');
  }
  return errors;
}

export function journeyDurationFromSegments(
  segments:Array<{terrain?:string}>,
  mounted=false,
  forcedMarch=false,
  adjustment=0,
):number{
  const hexes=segments.length;
  const difficult=segments.filter(segment=>isDifficultJourneyTerrain(segment.terrain)).length;
  let days=forcedMarch?Math.ceil(hexes/2)+difficult:hexes+difficult;
  if(mounted) days=Math.ceil(days/2);
  return Math.max(0,days+Math.trunc(Number(adjustment)||0));
}

export function startJourney(
  state:MasterState,
  destinationHexId:string|null,
  roles:Partial<Record<JourneyRole,string>>,
  routeHexIds:string[]=[],
  mounted=false,
  forcedMarch=false,
):MasterState{
  const route=routeHexIds.filter(Boolean).slice();
  const stage=route.slice(0,20);
  return {
    ...state,activeMode:'journey',company:{...state.company,journeyRoles:{...roles}},
    journey:{
      status:'planned',originHexId:state.position.currentHexId,destinationHexId:destinationHexId||route.at(-1)||null,
      startedAt:Date.now(),day:0,segments:[],pendingEvent:null,
      plannedRouteHexIds:route,stageRouteHexIds:stage,stageNumber:1,marchHexesRemaining:0,marchRollCount:0,
      awaitingEventGeneration:false,awaitingEndRoll:false,durationAdjustmentDays:0,mounted,forcedMarch,dangerEventsRemaining:0,travelFatigueByHero:{},
    },
    updatedAt:Date.now(),
  };
}

export function resolveJourneyMarch(state:MasterState,success:boolean,successIcons=0):MasterState{
  if(!['planned','travelling','paused'].includes(state.journey.status)) throw new Error('Não há Jornada ativa para uma Jogada de Marcha.');
  if(state.journey.pendingEvent||state.journey.awaitingEventGeneration) throw new Error('Resolva o Evento de Jornada antes da próxima Jogada de Marcha.');
  if(state.journey.awaitingEndRoll) throw new Error('A Companhia já alcançou o fim desta etapa.');
  const remaining=(state.journey.plannedRouteHexIds||[]).length;
  if(remaining<=0) return {...state,journey:{...state.journey,status:'paused',awaitingEndRoll:true},updatedAt:Date.now()};
  const seasonalFailure=(state.worldClock.season==='autumn'||state.worldClock.season==='winter')?1:2;
  const distance=success?3+Math.max(0,Math.trunc(Number(successIcons)||0)):seasonalFailure;
  const bounded=Math.min(distance,remaining,20);
  return {
    ...state,
    journey:{...state.journey,status:'travelling',marchHexesRemaining:bounded,marchRollCount:Math.max(0,Number(state.journey.marchRollCount)||0)+1,marchWillReachDestination:distance>=remaining},
    updatedAt:Date.now(),
  };
}

export function recordJourneyMove(state:MasterState,toHexId:string,locationName:string,distanceKm?:number,terrain?:string):MasterState{
  const fromHexId=state.position.currentHexId; const now=Date.now(); const isTravel=Boolean(fromHexId&&fromHexId!==toHexId);
  const active=state.journey.status!=='idle'&&state.journey.status!=='completed';
  if(active&&isTravel&&Number(state.journey.marchHexesRemaining||0)<=0){
    throw new Error('Faça a Jogada de Marcha do Guia antes de avançar a Jornada.');
  }
  const previousSegments=state.journey.segments||[];
  const rawSegmentDays=isTravel?journeyTravelDaysForTerrain(terrain):0;
  const segment=isTravel?{
    id:'segment_'+now+'_'+Math.random().toString(36).slice(2,6),fromHexId,toHexId,distanceKm,terrain,
    travelDays:rawSegmentDays,startedAtWorldMinute:Math.max(0,Number(state.worldClock.elapsedMinutes)||0),startedAt:now,completedAt:now,
  }:null;
  const nextSegments=segment?[...previousSegments,segment]:previousSegments;
  let effectiveTotal=active?journeyDurationFromSegments(nextSegments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),Number(state.journey.durationAdjustmentDays)||0):rawSegmentDays;
  const priorTotal=active?Math.max(0,Number(state.journey.day)||0):0;
  const deltaDays=Math.max(0,effectiveTotal-priorTotal);
  const advanced=deltaDays>0?advanceWorldClock(state.worldClock,deltaDays*1440):{clock:state.worldClock};
  if(segment) segment.completedAtWorldMinute=Math.max(0,Number(advanced.clock.elapsedMinutes)||0);

  const planned=(state.journey.plannedRouteHexIds||[]).slice();
  if(planned[0]===toHexId) planned.shift();
  const marchRemaining=active&&isTravel?Math.max(0,Number(state.journey.marchHexesRemaining||0)-1):Number(state.journey.marchHexesRemaining||0);
  const stageCompleted=active&&nextSegments.length>0&&nextSegments.length%20===0&&planned.length>0;
  const reachedDestination=active&&planned.length===0;
  const needEvent=active&&isTravel&&marchRemaining===0&&!reachedDestination&&!stageCompleted;

  return {
    ...state,worldClock:advanced.clock,
    position:{currentHexId:toHexId,locationName,routeHexIds:[...state.position.routeHexIds,toHexId].filter((id,index,all)=>index===0||id!==all[index-1]),lastMovedAt:now},
    journey:active?{
      ...state.journey,status:reachedDestination||stageCompleted?'paused':'travelling',day:effectiveTotal,segments:nextSegments,
      plannedRouteHexIds:planned,marchHexesRemaining:marchRemaining,awaitingEventGeneration:needEvent,
      awaitingEndRoll:reachedDestination,stageComplete:stageCompleted,
    }:state.journey,
    updatedAt:now,
  };
}

function featRank(value:number):number{return value===12?12:value===11?0:value;}
function chooseFeat(results:number[],mode:'favoured'|'normal'|'ill-favoured'):number{
  if(!results.length) return 1;
  if(mode==='normal') return results[0];
  return results.reduce((chosen,value)=>mode==='favoured'
    ?(featRank(value)>featRank(chosen)?value:chosen)
    :(featRank(value)<featRank(chosen)?value:chosen));
}
export type JourneyRegionKind='border'|'wild'|'dark';
export function journeyEventRollMode(region:JourneyRegionKind):'favoured'|'normal'|'ill-favoured'{
  return region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
}
function eventFromFeat(feat:number):{type:string;fatigue:number;description:string}{
  if(feat===11) return {type:'terrible-misfortune',fatigue:3,description:'Infortúnio Terrível'};
  if(feat===1) return {type:'despair',fatigue:2,description:'Desespero'};
  if(feat===2||feat===3) return {type:'ill-choices',fatigue:2,description:'Escolhas Ruins'};
  if(feat>=4&&feat<=7) return {type:'mishap',fatigue:2,description:'Azar'};
  if(feat===8||feat===9) return {type:'shortcut',fatigue:1,description:'Atalho'};
  if(feat===10) return {type:'chance-meeting',fatigue:1,description:'Encontro Fortuito'};
  return {type:'joyful-sight',fatigue:0,description:'Visão Alegre'};
}
export function generateJourneyEvent(state:MasterState,region:JourneyRegionKind,targetDie:number,featResults:number[],terrain?:string):MasterState{
  if(!state.journey.awaitingEventGeneration&&Number(state.journey.dangerEventsRemaining||0)<=0) throw new Error('Nenhum Evento de Jornada está aguardando geração.');
  const targetRoll=Math.max(1,Math.min(6,Math.trunc(targetDie)));
  const targetRole:JourneyRole=targetRoll<=2?'scout':targetRoll<=4?'lookout':'hunter';
  const mode=journeyEventRollMode(region);
  const feat=chooseFeat(featResults,mode);
  const event=eventFromFeat(feat);
  return {
    ...state,
    journey:{
      ...state.journey,awaitingEventGeneration:false,
      pendingEvent:{
        id:'journey_event_'+Date.now(),targetRole,skill:JOURNEY_ROLE_SKILLS[targetRole],fatigueOnFailure:event.fatigue,
        description:event.description,eventType:event.type,featResult:feat,region,terrain:terrain||'',rollMode:mode,
        successDiceModifier:isRoadTerrain(terrain)?1:isDifficultJourneyTerrain(terrain)?-1:0,
      },
    },
    updatedAt:Date.now(),
  };
}
export interface JourneyEventEffects {
  allFatigue:number;targetFatigue:number;allShadow:number;targetShadow:number;allHope:number;woundTarget:boolean;dayAdjustment:number;
}
export function resolveJourneyEvent(state:MasterState,success:boolean,successIcons=0):{state:MasterState;effects:JourneyEventEffects}{
  const pending=state.journey.pendingEvent;
  if(!pending) throw new Error('Nenhum Evento de Jornada pendente.');
  const type=String(pending.eventType||''); const effects:JourneyEventEffects={allFatigue:Math.max(0,Number(pending.fatigueOnFailure)||0),targetFatigue:0,allShadow:0,targetShadow:0,allHope:0,woundTarget:false,dayAdjustment:0};
  if(type==='terrible-misfortune'&&!success) effects.woundTarget=true;
  if(type==='despair'&&!success) effects.allShadow=1;
  if(type==='ill-choices'&&!success) effects.targetShadow=1;
  if(type==='mishap'&&!success){effects.dayAdjustment=1;effects.targetFatigue=1;}
  if(type==='shortcut'&&success) effects.dayAdjustment=-1;
  if(type==='chance-meeting'&&success) effects.allFatigue=0;
  if(type==='joyful-sight'&&success) effects.allHope=1;
  const adjustment=(Number(state.journey.durationAdjustmentDays)||0)+effects.dayAdjustment;
  const effective=journeyDurationFromSegments(state.journey.segments,Boolean(state.journey.mounted),Boolean(state.journey.forcedMarch),adjustment);
  const danger=Math.max(0,Number(state.journey.dangerEventsRemaining||0)-(Number(state.journey.dangerEventsRemaining||0)>0?1:0));
  const shifted=effects.dayAdjustment!==0?advanceWorldClock(state.worldClock,effects.dayAdjustment*1440).clock:state.worldClock;
  return {
    state:{...state,worldClock:shifted,journey:{...state.journey,pendingEvent:null,durationAdjustmentDays:adjustment,day:effective,dangerEventsRemaining:danger,awaitingEventGeneration:danger>0},updatedAt:Date.now()},
    effects,
  };
}
export function setDangerousArea(state:MasterState,danger:number):MasterState{
  const value=Math.max(0,Math.trunc(Number(danger)||0));
  return {...state,journey:{...state.journey,dangerEventsRemaining:value,awaitingEventGeneration:value>0,marchHexesRemaining:0},updatedAt:Date.now()};
}
export function continueJourneyStage(state:MasterState):MasterState{
  if(!state.journey.stageComplete) return state;
  return {...state,journey:{...state.journey,status:'planned',stageComplete:false,stageNumber:Math.max(1,Number(state.journey.stageNumber)||1)+1,stageRouteHexIds:(state.journey.plannedRouteHexIds||[]).slice(0,20),marchHexesRemaining:0},updatedAt:Date.now()};
}
export function completeJourney(state:MasterState):MasterState{
  return {...state,activeMode:'adventure',journey:{...state.journey,status:'completed',destinationHexId:state.position.currentHexId,pendingEvent:null,awaitingEndRoll:false,marchHexesRemaining:0},updatedAt:Date.now()};
}
