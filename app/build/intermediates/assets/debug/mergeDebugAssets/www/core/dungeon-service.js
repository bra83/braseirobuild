import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import { dungeonActionTurnCost, resolveDungeonTurns } from './wwn-dungeon-core.js';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function commit({persistence,loaded,state,campaignId,eventKind,eventSuffix,payload}){
  const newRevision=loaded.revision+1;state.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:${eventSuffix}`;
  const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind,eventPayloadJson:canonicalJson({eventId,revision:newRevision,...payload}),newStateJson:canonicalJson(state),rngCountersJson:canonicalJson(state.rngState?.counters??{})});
  return {receipt,eventId,newRevision};
}

export class DungeonActionService{
  constructor({persistence}){this.persistence=persistence;}

  enterSite({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!contract||typeof contract!=='object')return Object.freeze({status:'MISSING_GM_DUNGEON_CONTRACT',revision:loaded.revision});
    for(const key of ['siteId','defenseState'])if(contract[key]===undefined)return Object.freeze({status:`MISSING_GM_DUNGEON_${key.toUpperCase()}`,revision:loaded.revision});
    // Validate before mutation.
    resolveDungeonTurns({rng:{die(){return{result:2};}},currentTurn:0,turns:1,defenseState:contract.defenseState});
    const next=structuredClone(state);
    next.worldState={...(next.worldState??{}),dungeon:{active:true,siteId:String(contract.siteId),defenseState:String(contract.defenseState).toUpperCase(),turnNumber:0,elapsedMinutes:0,position:contract.position??null,lightSource:contract.lightSource??null,lastTurn:null}};
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'DUNGEON_ENTERED',eventSuffix:'dungeon-enter',payload:{siteId:next.worldState.dungeon.siteId,defenseState:next.worldState.dungeon.defenseState}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'OK',receipt,dungeon:structuredClone(next.worldState.dungeon)});
  }

  takeTurn({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const dungeon=state.worldState?.dungeon;
    if(!dungeon?.active)return Object.freeze({status:'NO_ACTIVE_DUNGEON_NO_MUTATION',revision:loaded.revision});
    if(!contract||typeof contract!=='object'||!contract.actionKind)return Object.freeze({status:'MISSING_DUNGEON_ACTION_CONTRACT_NO_MUTATION',revision:loaded.revision});
    let cost;try{cost=dungeonActionTurnCost(contract.actionKind);}catch{return Object.freeze({status:'UNKNOWN_DUNGEON_ACTION_NO_MUTATION',revision:loaded.revision});}
    const rng=DeterministicRng.fromState(state.rngState);
    const resolution=resolveDungeonTurns({rng,currentTurn:dungeon.turnNumber,turns:cost.turns,defenseState:dungeon.defenseState,streamPrefix:`dungeon/${dungeon.siteId}`,lightSource:dungeon.lightSource});
    const next=structuredClone(state);next.rngState=rng.exportState();
    next.worldState.dungeon.turnNumber=resolution.nextTurn;next.worldState.dungeon.elapsedMinutes=(dungeon.elapsedMinutes??0)+resolution.elapsedMinutes;
    next.worldState.dungeon.lastTurn={actionKind:String(contract.actionKind).toUpperCase(),resolution};
    if(resolution.light)next.worldState.dungeon.lightSource={type:resolution.light.type,remainingTurns:resolution.light.after};
    if(contract.positionAfter!==undefined)next.worldState.dungeon.position=structuredClone(contract.positionAfter);
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'DUNGEON_TURN',eventSuffix:'dungeon-turn',payload:{actionKind:String(contract.actionKind).toUpperCase(),resolution,positionAfter:contract.positionAfter??null}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:resolution.encounterTriggered?'WANDERING_ENCOUNTER_TRIGGERED':'OK',receipt,resolution,dungeon:structuredClone(next.worldState.dungeon)});
  }

  leaveSite({campaignId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!state.worldState?.dungeon?.active)return Object.freeze({status:'NO_ACTIVE_DUNGEON_NO_MUTATION',revision:loaded.revision});
    const next=structuredClone(state);next.worldState.dungeon.active=false;
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'DUNGEON_LEFT',eventSuffix:'dungeon-leave',payload:{siteId:next.worldState.dungeon.siteId,turnNumber:next.worldState.dungeon.turnNumber}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'OK',receipt,dungeon:structuredClone(next.worldState.dungeon)});
  }
}
