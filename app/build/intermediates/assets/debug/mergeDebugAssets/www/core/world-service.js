import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import { resolveForagingAttempt, resolveTravelDay, exploreHexDays, expeditionSupplyNeeds, TERRAIN_MPH } from './wwn-travel-core.js';
import { factionIncome, resolveFactionInitiative } from './wwn-faction-core.js';
import { advanceLivingWorldPresentation } from './living-world-core.js';


const ATLAS_TERRAIN_PRESENTATION=Object.freeze({
  PLAINS:Object.freeze({terrain:'grassland',ruleTerrain:'PLAINS',tile:'assets/wwn_asset_library_v1/hex/terrain_base_cropped/terrain_base_01_lush_grassland.png',terrainTitle:'PLANÍCIE ABERTA',terrainLabel:'PLANÍCIE',terrainDescription:'Terreno aberto de viagem relativamente rápida.'}),
  LIGHT_FOREST_OR_DESERT:Object.freeze({terrain:'light-forest',ruleTerrain:'LIGHT_FOREST_OR_DESERT',tile:'assets/wwn_asset_library_v1/hex/terrain_base_cropped/terrain_base_06_deciduous_forest.png',terrainTitle:'MATA RALA',terrainLabel:'MATA / TERRENO SECO',terrainDescription:'Vegetação ou terreno seco que reduz o ritmo de viagem.'}),
  DENSE_FOREST_OR_RUGGED_HILLS:Object.freeze({terrain:'rugged-hills',ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS',tile:'assets/wwn_asset_library_v1/hex/terrain_base_cropped/terrain_base_16_rocky_highland.png',terrainTitle:'COLINAS ACIDENTADAS',terrainLabel:'COLINAS / MATA DENSA',terrainDescription:'Terreno quebrado e de avanço cauteloso.'}),
  SWAMP_OR_MARSH:Object.freeze({terrain:'marsh',ruleTerrain:'SWAMP_OR_MARSH',tile:'assets/wwn_asset_library_v1/hex/terrain_base_cropped/terrain_base_09_reed_marsh.png',terrainTitle:'PÂNTANO',terrainLabel:'PÂNTANO / BREJO',terrainDescription:'Água rasa, lama e vegetação tornam a travessia lenta.'}),
  MOUNTAINS_OR_DIRE_WASTELANDS:Object.freeze({terrain:'mountains',ruleTerrain:'MOUNTAINS_OR_DIRE_WASTELANDS',tile:'assets/wwn_asset_library_v1/hex/terrain_base_cropped/terrain_base_16_rocky_highland.png',terrainTitle:'ESCARPAS ROCHOSAS',terrainLabel:'MONTANHAS / ERMOS',terrainDescription:'Terreno severo e exaustivo, de progresso muito lento.'}),
});
const TERRAIN_TRANSITIONS=Object.freeze({
  PLAINS:Object.freeze(['PLAINS','PLAINS','LIGHT_FOREST_OR_DESERT','DENSE_FOREST_OR_RUGGED_HILLS']),
  LIGHT_FOREST_OR_DESERT:Object.freeze(['PLAINS','LIGHT_FOREST_OR_DESERT','LIGHT_FOREST_OR_DESERT','DENSE_FOREST_OR_RUGGED_HILLS','SWAMP_OR_MARSH']),
  DENSE_FOREST_OR_RUGGED_HILLS:Object.freeze(['LIGHT_FOREST_OR_DESERT','DENSE_FOREST_OR_RUGGED_HILLS','DENSE_FOREST_OR_RUGGED_HILLS','MOUNTAINS_OR_DIRE_WASTELANDS','SWAMP_OR_MARSH']),
  SWAMP_OR_MARSH:Object.freeze(['SWAMP_OR_MARSH','SWAMP_OR_MARSH','LIGHT_FOREST_OR_DESERT','PLAINS','DENSE_FOREST_OR_RUGGED_HILLS']),
  MOUNTAINS_OR_DIRE_WASTELANDS:Object.freeze(['MOUNTAINS_OR_DIRE_WASTELANDS','MOUNTAINS_OR_DIRE_WASTELANDS','DENSE_FOREST_OR_RUGGED_HILLS','LIGHT_FOREST_OR_DESERT']),
});
function fnv1a(text){let h=0x811c9dc5;for(const c of String(text)){h^=c.codePointAt(0);h=Math.imul(h,0x01000193)>>>0;}return h>>>0;}
function axialDistance(a,b){const dq=a.q-b.q,dr=a.r-b.r;return(Math.abs(dq)+Math.abs(dr)+Math.abs(dq+dr))/2;}
function neighborRecords(state,q,r){const keys=[[1,0],[-1,0],[0,1],[0,-1],[1,-1],[-1,1]].map(([dq,dr])=>`${q+dq},${r+dr}`);return keys.map(k=>state?.worldState?.atlas?.hexes?.[k]).filter(Boolean);}
export function deriveAtlasHexForTravel(state,{campaignId,q,r}){
  const existing=state?.worldState?.atlas?.hexes?.[`${q},${r}`];if(existing?.ruleTerrain)return structuredClone(existing);
  const neighbors=neighborRecords(state,q,r).filter(x=>x?.ruleTerrain&&TERRAIN_MPH[x.ruleTerrain]!==undefined);
  const anchor=neighbors.length?neighbors.sort((a,b)=>String(a.ruleTerrain).localeCompare(String(b.ruleTerrain)))[fnv1a(`${campaignId}:${q}:${r}:anchor`)%neighbors.length]?.ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS';
  const choices=TERRAIN_TRANSITIONS[anchor]??Object.keys(ATLAS_TERRAIN_PRESENTATION);
  const chosen=choices[fnv1a(`${campaignId}:${q}:${r}:terrain`)%choices.length];
  return {...ATLAS_TERRAIN_PRESENTATION[chosen],q,r,discovered:false,entered:false,visited:false,explored:false,poi:null,poiRevealed:false,generatedBy:'ATLAS_CONTINUITY_V1'};
}

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}

function markEnteredAtlasHex(next,contract,revision){
  const p=contract?.positionAfter;if(!Number.isInteger(p?.q)||!Number.isInteger(p?.r))return;
  next.worldState=next.worldState??{};next.worldState.atlas=next.worldState.atlas??{hexes:{}};next.worldState.atlas.hexes=next.worldState.atlas.hexes??{};
  const key=`${p.q},${p.r}`,existing=next.worldState.atlas.hexes[key]??{},provided=(contract.atlasHexAfter&&typeof contract.atlasHexAfter==='object')?contract.atlasHexAfter:{};
  next.worldState.atlas.hexes[key]={...existing,...provided,q:p.q,r:p.r,ruleTerrain:provided.ruleTerrain??existing.ruleTerrain??contract.terrain,discovered:true,entered:true,visited:true,poiRevealed:true,enteredAtRevision:revision,explored:existing.explored===true||provided.explored===true};
}

function syncSinglePartyGroupPosition(next,positionAfter){
  if(!positionAfter||!next.partyState?.groups)return;const ids=Object.keys(next.partyState.groups);if(ids.length!==1)return;
  next.partyState.groups[ids[0]].position=structuredClone(positionAfter);
}
function chooseDeterministic(list,key){if(!Array.isArray(list)||list.length===0)return null;return list[fnv1a(key)%list.length]??null;}
function setDirectWorldScene(next,{eventId,revision,title,summary,kind='worldEvent',contentId='world-event'}){
  const publicScene={status:'OK',contentType:kind,contentId,title:String(title??'CENA').trim()||'CENA',summary:String(summary??'').trim(),situation:String(summary??'').trim(),leads:[]};
  next.encounterState={...(next.encounterState??{}),sceneId:`scene:${kind}:${contentId}:r${revision}`,currentScene:{contentType:kind,contentId,publicScene},scenePhase:'ACTIVE',sceneOpenedAtRevision:revision};
  next.journalState={...(next.journalState??{}),lastSceneEventId:eventId};
}

export class WorldActionService{
  constructor({persistence}){this.persistence=persistence;}

  travelDay({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId); if(!state)return Object.freeze({status:loaded.status});
    if(!contract||typeof contract!=='object')return Object.freeze({status:'MISSING_GM_TRAVEL_CONTRACT',revision:loaded.revision});
    const required=['terrain','dayEncounterDie','nightEncounterDie']; for(const k of required)if(contract[k]===undefined)return Object.freeze({status:`MISSING_GM_TRAVEL_${k.toUpperCase()}`,revision:loaded.revision});
    const rng=DeterministicRng.fromState(state.rngState);
    const supplies=state.worldState?.expedition?.supplies??null;
    const resolution=resolveTravelDay({rng,terrain:contract.terrain,hours:contract.hours??10,road:Boolean(contract.road),foulWeather:Boolean(contract.foulWeather),deepSnow:Boolean(contract.deepSnow),dayEncounterDie:contract.dayEncounterDie,nightEncounterDie:contract.nightEncounterDie,supplies,people:contract.people??1,hotDryWaterMultiplier:contract.hotDryWaterMultiplier??1,streamPrefix:`world/travel/day${(state.worldState?.elapsedDays??0)+1}`});
    const next=structuredClone(state); next.rngState=rng.exportState(); next.worldState={...(next.worldState??{}),elapsedDays:(next.worldState?.elapsedDays??0)+1,lastTravel:resolution};
    if(resolution.supplyResult){next.worldState.expedition={...(next.worldState.expedition??{}),supplies:resolution.supplyResult.after};}
    const newRevision=loaded.revision+1;
    if(contract.positionAfter!==undefined){next.worldState.position=structuredClone(contract.positionAfter);syncSinglePartyGroupPosition(next,contract.positionAfter);markEnteredAtlasHex(next,contract,newRevision);}
    next.worldState.livingWorld=advanceLivingWorldPresentation(next,{trigger:'TRAVEL_DAY',revision:newRevision,elapsedDaysDelta:0});
    next.revision=newRevision; const eventId=`mech:${campaignId}:r${newRevision}:travel-day`;
    const encounterTriggered=resolution.dayEncounter?.encounter===true||resolution.nightEncounter?.encounter===true;
    const pendingDestination=contract.pendingTravelDestination;
    const remainingAfter=Number(contract.remainingHoursAfter??0);
    if(encounterTriggered){
      if(pendingDestination&&remainingAfter>0)next.worldState.pendingTravel={q:pendingDestination.q,r:pendingDestination.r,hoursRemaining:remainingAfter,terrain:contract.terrain,interruptedAtRevision:newRevision};
      const seed=chooseDeterministic(state.contentState?.gm?.encounter_seeds??[],`${campaignId}:travel:${newRevision}`);
      setDirectWorldScene(next,{eventId,revision:newRevision,title:'ENCONTRO NO CAMINHO',summary:seed??'Algo interrompe a viagem e exige a atenção imediata do grupo.',kind:'travelEncounter',contentId:`travel-${newRevision}`});
    }else if(contract.positionAfter!==undefined){
      delete next.worldState.pendingTravel;
      const targetTitle=contract.atlasHexAfter?.placeName??contract.atlasHexAfter?.terrainTitle??contract.positionAfter?.name??'NOVO HEX';
      const terrainText=contract.atlasHexAfter?.terrainDescription??`O grupo alcança ${targetTitle}.`;
      setDirectWorldScene(next,{eventId,revision:newRevision,title:`CHEGADA • ${targetTitle}`,summary:terrainText,kind:'travelArrival',contentId:`arrival-${contract.positionAfter.q}-${contract.positionAfter.r}`});
    }
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'WORLD_TRAVEL_DAY',eventPayloadJson:canonicalJson({eventId,revision:newRevision,contract,resolution,scene:next.encounterState?.currentScene?.publicScene??null}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState.counters)});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'OK',receipt,resolution,worldState:next.worldState});
  }


  travelToAdjacentHex({campaignId,q,r,dayEncounterDie=6,nightEncounterDie=8,people=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const current=state.worldState?.position;if(!Number.isInteger(current?.q)||!Number.isInteger(current?.r))return Object.freeze({status:'CURRENT_POSITION_MISSING_NO_MUTATION',revision:loaded.revision});
    if(!Number.isInteger(q)||!Number.isInteger(r)||axialDistance(current,{q,r})!==1)return Object.freeze({status:'DESTINATION_MUST_BE_ADJACENT_NO_MUTATION',revision:loaded.revision});
    const pending=state.worldState?.pendingTravel??null;if(pending&&(pending.q!==q||pending.r!==r))return Object.freeze({status:'TRAVEL_INTERRUPTED_DESTINATION_LOCKED_NO_MUTATION',revision:loaded.revision,pendingTravel:structuredClone(pending)});
    const target=deriveAtlasHexForTravel(state,{campaignId,q,r});const terrain=target.ruleTerrain;if(TERRAIN_MPH[terrain]===undefined)return Object.freeze({status:'TARGET_TERRAIN_MISSING_NO_MUTATION',revision:loaded.revision});
    const memberCount=people??Math.max(1,Object.values(state.partyState?.groups??{}).reduce((n,g)=>n+(g.memberIds?.length??0),0));
    const hoursNeeded=6/TERRAIN_MPH[terrain];let remaining=pending?.hoursRemaining>0?Number(pending.hoursRemaining):hoursNeeded;let last=null;const legs=[];let encounterOccurred=false;
    while(remaining>0){const hours=Math.min(10,remaining);const remainingAfter=Math.max(0,remaining-hours);const isFinal=remainingAfter<=0;last=this.travelDay({campaignId,contract:{terrain,hours,dayEncounterDie,nightEncounterDie,people:memberCount,pendingTravelDestination:{q,r},remainingHoursAfter:remainingAfter,positionAfter:isFinal?{q,r,name:target.placeName??target.terrainTitle}:undefined,atlasHexAfter:isFinal?target:undefined}});if(last.status!=='OK')return last;legs.push(last.resolution);remaining=remainingAfter;encounterOccurred=last.resolution?.dayEncounter?.encounter===true||last.resolution?.nightEncounter?.encounter===true;if(encounterOccurred)break;}
    return Object.freeze({...last,status:'OK',destination:{q,r},atlasHex:target,hoursNeeded,remainingHours:remaining,legs:Object.freeze(legs),encounterOccurred,interrupted:encounterOccurred&&remaining>0});
  }

  exploreHex({campaignId,q,r,concealmentMultiplier=1,people=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const current=state.worldState?.position;if(!Number.isInteger(current?.q)||!Number.isInteger(current?.r)||current.q!==q||current.r!==r)return Object.freeze({status:'MUST_BE_IN_HEX_TO_EXPLORE_NO_MUTATION',revision:loaded.revision});
    const hex=state.worldState?.atlas?.hexes?.[`${q},${r}`];if(hex?.explored===true)return Object.freeze({status:'ALREADY_EXPLORED_NO_MUTATION',revision:loaded.revision});
    const plan=exploreHexDays({concealmentMultiplier});const memberCount=people??Math.max(1,Object.values(state.partyState?.groups??{}).reduce((n,g)=>n+(g.memberIds?.length??0),0));const needs=expeditionSupplyNeeds({people:memberCount,days:plan.days});
    const next=structuredClone(state);next.worldState=next.worldState??{};next.worldState.atlas=next.worldState.atlas??{hexes:{}};next.worldState.atlas.hexes=next.worldState.atlas.hexes??{};const existing=next.worldState.atlas.hexes[`${q},${r}`]??deriveAtlasHexForTravel(next,{campaignId,q,r});
    const supplies=next.worldState.expedition?.supplies??null;let supplyResult=null;if(supplies){const before={foodUnits:Number(supplies.foodUnits??0),waterUnits:Number(supplies.waterUnits??0)};supplyResult={needs,before,after:{foodUnits:Math.max(0,before.foodUnits-needs.foodUnits),waterUnits:Math.max(0,before.waterUnits-needs.waterUnits)},insufficientFood:before.foodUnits<needs.foodUnits,insufficientWater:before.waterUnits<needs.waterUnits};next.worldState.expedition={...(next.worldState.expedition??{}),supplies:{...supplies,...supplyResult.after}};}
    const newRevision=loaded.revision+1;
    const poiPool=state.contentState?.public?.minorPois??[];const discoveredPoi=existing.poiName?null:chooseDeterministic(poiPool,`${campaignId}:explore:${q}:${r}`);
    const discovered={poiName:existing.poiName??discoveredPoi?.name??null,poiType:existing.poiType??discoveredPoi?.type??null,poiSituation:existing.poiSituation??discoveredPoi?.situation??null};
    next.worldState.atlas.hexes[`${q},${r}`]={...existing,...discovered,q,r,placeName:existing.placeName??discovered.poiName??existing.terrainTitle,discovered:true,entered:true,visited:true,explored:true,poiRevealed:true,exploredAtRevision:newRevision};
    if(discovered.poiName){next.worldState.position={...(next.worldState.position??{}),q,r,name:discovered.poiName};syncSinglePartyGroupPosition(next,next.worldState.position);}
    next.worldState.elapsedDays=(next.worldState.elapsedDays??0)+plan.days;next.worldState.lastExploration={q,r,plan,supplyResult,discovery:discovered};next.worldState.livingWorld=advanceLivingWorldPresentation(next,{trigger:'HEX_EXPLORED',revision:newRevision,elapsedDaysDelta:plan.days});next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:hex-explored`;
    const sceneTitle=discovered.poiName??`HEX ${q},${r} EXPLORADO`;const sceneSummary=discovered.poiSituation??`A exploração de ${plan.days} dia(s) revela os detalhes perceptíveis deste hex.`;
    setDirectWorldScene(next,{eventId,revision:newRevision,title:sceneTitle,summary:sceneSummary,kind:'hexExploration',contentId:`hex-${q}-${r}`});
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'WORLD_HEX_EXPLORED',eventPayloadJson:canonicalJson({eventId,revision:newRevision,q,r,plan,supplyResult,discovery:discovered,scene:next.encounterState?.currentScene?.publicScene??null}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,plan,supplyResult,discovery:discovered,hex:structuredClone(next.worldState.atlas.hexes[`${q},${r}`])});
  }


  forage({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId); if(!state)return Object.freeze({status:loaded.status});
    if(!contract||typeof contract!=='object')return Object.freeze({status:'MISSING_GM_FORAGE_CONTRACT',revision:loaded.revision});
    for(const key of ['terrain','wisdomScore','encounterDie'])if(contract[key]===undefined)return Object.freeze({status:`MISSING_GM_FORAGE_${key.toUpperCase()}`,revision:loaded.revision});
    if(state.worldState?.pendingForage?.allocationRequired)return Object.freeze({status:'FORAGE_ALLOCATION_PENDING_NO_MUTATION',revision:loaded.revision,pending:state.worldState.pendingForage});
    const rng=DeterministicRng.fromState(state.rngState);
    const result=resolveForagingAttempt({rng,wisdomScore:contract.wisdomScore,bestSurviveLevel:contract.bestSurviveLevel??null,groupSurviveLevels:contract.groupSurviveLevels??[],terrain:contract.terrain,fullDay:Boolean(contract.fullDay),successiveDaysInSameHex:contract.successiveDaysInSameHex??0,encounterDie:contract.encounterDie,nativeWildsMinimumTwo:Boolean(contract.nativeWildsMinimumTwo),streamPrefix:`world/forage/r${loaded.revision+1}`});
    const next=structuredClone(state);next.rngState=rng.exportState();
    next.worldState={...(next.worldState??{}),pendingForage:result.allocationRequired?{unitsFound:result.unitsFound,allocationRequired:true,result}:null,lastForage:result};
    if(contract.timeAfter!==undefined)next.worldState.time=structuredClone(contract.timeAfter);
    const newRevision=loaded.revision+1;next.worldState.livingWorld=advanceLivingWorldPresentation(next,{trigger:'FORAGE',revision:newRevision,elapsedDaysDelta:contract.fullDay?1:0});next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:forage`;
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'WORLD_FORAGE_ATTEMPT',eventPayloadJson:canonicalJson({eventId,revision:newRevision,result}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState.counters)});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:result.allocationRequired?'ALLOCATION_REQUIRED':'OK',receipt,result});
  }

  allocateForage({campaignId,foodUnits=0,waterUnits=0,firewoodUnits=0}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const pending=state.worldState?.pendingForage;
    if(!pending?.allocationRequired)return Object.freeze({status:'NO_PENDING_FORAGE_NO_MUTATION',revision:loaded.revision});
    if(![foodUnits,waterUnits,firewoodUnits].every(v=>Number.isInteger(v)&&v>=0))return Object.freeze({status:'INVALID_FORAGE_ALLOCATION_NO_MUTATION',revision:loaded.revision});
    const spent=foodUnits+waterUnits+firewoodUnits;if(spent>pending.unitsFound)return Object.freeze({status:'FORAGE_ALLOCATION_EXCEEDS_FOUND_NO_MUTATION',revision:loaded.revision,available:pending.unitsFound});
    const next=structuredClone(state);const existing=next.worldState?.expedition?.supplies??{};
    next.worldState.expedition={...(next.worldState.expedition??{}),supplies:{...existing,foodUnits:Number(existing.foodUnits??0)+foodUnits,waterUnits:Number(existing.waterUnits??0)+waterUnits,firewoodUnits:Number(existing.firewoodUnits??0)+firewoodUnits}};
    next.worldState.lastForageAllocation={foodUnits,waterUnits,firewoodUnits,unallocated:pending.unitsFound-spent};next.worldState.pendingForage=null;
    const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:forage-allocation`;
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'WORLD_FORAGE_ALLOCATED',eventPayloadJson:canonicalJson({eventId,revision:newRevision,foodUnits,waterUnits,firewoodUnits,unallocated:pending.unitsFound-spent}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'OK',receipt,allocation:next.worldState.lastForageAllocation,supplies:structuredClone(next.worldState.expedition.supplies)});
  }

  startFactionTurn({campaignId,tieBreakOrder=null}){
    const {loaded,state}=loadState(this.persistence,campaignId); if(!state)return Object.freeze({status:loaded.status});
    const activeTurn=state.worldState?.factionTurn;
    if(activeTurn && activeTurn.phase && activeTurn.phase!=='TURN_COMPLETE')return Object.freeze({status:'ACTIVE_FACTION_TURN_NO_MUTATION',revision:loaded.revision,phase:activeTurn.phase});
    const factions=state.factionState??{}; const ids=Object.keys(factions);
    if(ids.length===0)return Object.freeze({status:'NO_FACTIONS_NO_MUTATION',revision:loaded.revision});
    const rng=DeterministicRng.fromState(state.rngState); const rolls=[];
    for(const id of ids){const f=factions[id]; if(!f||![f.force,f.cunning,f.wealth].every(Number.isInteger))return Object.freeze({status:`MISSING_EVIDENCE_FACTION_STATS:${id}`,revision:loaded.revision});rolls.push(resolveFactionInitiative({rng,factionId:id}));}
    const byTotal=new Map(); for(const roll of rolls){const group=byTotal.get(roll.total)??[];group.push(roll.factionId);byTotal.set(roll.total,group);}
    const tiedGroups=[...byTotal.entries()].filter(([,group])=>group.length>1).map(([total,group])=>({total,factionIds:group}));
    if(tiedGroups.length>0){
      const supplied=Array.isArray(tieBreakOrder)?tieBreakOrder:[];
      const suppliedSet=new Set(supplied);
      const allTied=tiedGroups.flatMap(g=>g.factionIds);
      if(allTied.some(id=>!suppliedSet.has(id))){
        return Object.freeze({status:'GM_TIEBREAK_REQUIRED_NO_MUTATION',revision:loaded.revision,initiative:rolls,tiedGroups});
      }
      const rank=new Map(supplied.map((id,index)=>[id,index]));
      rolls.sort((a,b)=>b.total-a.total||((rank.get(a.factionId)??Number.MAX_SAFE_INTEGER)-(rank.get(b.factionId)??Number.MAX_SAFE_INTEGER)));
    }else{
      rolls.sort((a,b)=>b.total-a.total);
    }
    const next=structuredClone(state); next.rngState=rng.exportState();
    const initiativeOrder=rolls.map(r=>r.factionId);
    const currentFactionId=initiativeOrder[0];
    const currentFaction=next.factionState[currentFactionId];
    const gained=factionIncome(currentFaction);
    currentFaction.treasure=(currentFaction.treasure??0)+gained;
    currentFaction.lastIncome=gained;
    next.worldState={...(next.worldState??{}),factionTurn:{phase:'UPKEEP_REQUIRED',initiativeOrder,initiative:rolls,currentIndex:0,currentFactionId,startedAtRevision:loaded.revision+1}};
    const newRevision=loaded.revision+1; next.revision=newRevision; const eventId=`mech:${campaignId}:r${newRevision}:faction-turn-start`;
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'FACTION_TURN_STARTED',eventPayloadJson:canonicalJson({eventId,revision:newRevision,initiative:rolls,currentFactionId,income:{[currentFactionId]:gained},nextRequiredStep:'UPKEEP'}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState.counters)});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'UPKEEP_REQUIRED',receipt,initiativeOrder,currentFactionId,income:gained,factions:structuredClone(next.factionState)});
  }
}
