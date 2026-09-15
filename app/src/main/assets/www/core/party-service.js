import { canonicalJson } from './canonical-json.js';
import { createPartyState, splitPartyGroup, mergePartyGroups, validatePartyState, characterRosterFromState } from './party-state.js';


function comparablePosition(position){
  if(!position||typeof position!=='object')return null;
  if(Number.isInteger(position.q)&&Number.isInteger(position.r))return `hex:${position.q},${position.r}`;
  if(Number.isFinite(position.x)&&Number.isFinite(position.y))return `xy:${position.x},${position.y}`;
  return null;
}
export function partyGroupsAreColocated(partyState,groupIds){
  if(!partyState?.groups||!Array.isArray(groupIds)||groupIds.length<2)return false;
  const groups=groupIds.map(id=>partyState.groups[id]);if(groups.some(g=>!g))return false;
  const keys=groups.map(g=>comparablePosition(g.position));
  return keys.every(Boolean)&&new Set(keys).size===1;
}

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function commitParty({persistence,campaignId,loaded,state,partyState,eventKind,eventSuffix,payload}){
  const next=structuredClone(state);const newRevision=loaded.revision+1;next.revision=newRevision;next.partyState=structuredClone(partyState);
  const eventId=`mech:${campaignId}:r${newRevision}:${eventSuffix}`;
  const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind,eventPayloadJson:canonicalJson({eventId,revision:newRevision,...payload}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
  return Object.freeze({status:receipt.status,receipt,partyState:receipt.status==='OK'?structuredClone(next.partyState):null,projectedState:receipt.status==='OK'?next:null});
}
export class PartyService{
  constructor({persistence}){this.persistence=persistence;}
  initialize({campaignId,position=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(state.partyState)return Object.freeze({status:'PARTY_ALREADY_INITIALIZED_NO_MUTATION',partyState:structuredClone(state.partyState),revision:loaded.revision});
    const ids=Object.keys(characterRosterFromState(state));if(!ids.length)return Object.freeze({status:'NO_CHARACTERS_NO_MUTATION',revision:loaded.revision});
    const party=createPartyState({memberIds:ids,position:position??state.worldState?.position??null});
    return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:party,eventKind:'PARTY_INITIALIZED',eventSuffix:'party-init',payload:{memberIds:ids}});
  }
  split({campaignId,sourceGroupId,newGroups}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!state.partyState)return Object.freeze({status:'PARTY_NOT_INITIALIZED_NO_MUTATION',revision:loaded.revision});
    const result=splitPartyGroup(state.partyState,{sourceGroupId,newGroups});if(result.status!=='OK')return Object.freeze({...result,revision:loaded.revision});
    return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:result.partyState,eventKind:'PARTY_SPLIT',eventSuffix:'party-split',payload:{sourceGroupId,newGroups:newGroups.map(g=>({groupId:g.groupId,memberIds:g.memberIds,position:g.position??null})),sourceRefs:result.sourceRefs}});
  }
  merge({campaignId,groupIds,targetGroupId='party-main',label='GRUPO',position=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!state.partyState)return Object.freeze({status:'PARTY_NOT_INITIALIZED_NO_MUTATION',revision:loaded.revision});
    const result=mergePartyGroups(state.partyState,{groupIds,targetGroupId,label,position});if(result.status!=='OK')return Object.freeze({...result,revision:loaded.revision});
    return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:result.partyState,eventKind:'PARTY_MERGED',eventSuffix:'party-merge',payload:{groupIds,targetGroupId,sourceRefs:result.sourceRefs}});
  }

  mergeColocated({campaignId,groupIds,targetGroupId='party-main',label='GRUPO'}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!state.partyState)return Object.freeze({status:'PARTY_NOT_INITIALIZED_NO_MUTATION',revision:loaded.revision});
    if(!partyGroupsAreColocated(state.partyState,groupIds))return Object.freeze({status:'GROUPS_NOT_COLOCATED_NO_MUTATION',revision:loaded.revision,groupIds:[...(groupIds??[])]});
    const first=state.partyState.groups[groupIds[0]];
    const result=mergePartyGroups(state.partyState,{groupIds,targetGroupId,label,position:first.position});if(result.status!=='OK')return Object.freeze({...result,revision:loaded.revision});
    return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:result.partyState,eventKind:'PARTY_MERGED',eventSuffix:'party-merge-colocated',payload:{groupIds,targetGroupId,position:first.position,sourceRefs:result.sourceRefs}});
  }
  addMember({campaignId,characterId,groupId=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const roster=characterRosterFromState(state);if(!roster[characterId])return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    let party=state.partyState;if(!party){party=createPartyState({memberIds:Object.keys(roster),position:state.worldState?.position??null});return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:party,eventKind:'PARTY_INITIALIZED',eventSuffix:'party-init',payload:{memberIds:Object.keys(roster)}});}
    const valid=validatePartyState(party);if(!valid.ok)throw new Error(valid.errors.join('; '));
    if(Object.values(party.groups).some(g=>g.memberIds.includes(characterId)))return Object.freeze({status:'CHARACTER_ALREADY_IN_PARTY_NO_MUTATION',revision:loaded.revision});
    const target=groupId??party.activeGroupId;if(!party.groups[target])return Object.freeze({status:'GROUP_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const nextParty=structuredClone(party);nextParty.groups[target].memberIds.push(characterId);
    return commitParty({persistence:this.persistence,campaignId,loaded,state,partyState:nextParty,eventKind:'PARTY_MEMBER_ADDED',eventSuffix:'party-member-add',payload:{characterId,groupId:target}});
  }
}
