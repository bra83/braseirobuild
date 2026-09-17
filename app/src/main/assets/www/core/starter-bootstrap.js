import { makeEmptyCampaignState, serializeCampaignState } from './campaign-state.js';
import { createPartyState } from './party-state.js';
import { applyStarterContentToState } from './starter-content-service.js';
import { STARTER_CAMPAIGN_ID } from './runtime-campaign.js';
import { prepareCampaignOpening } from './campaign-opening.js';
import { canonicalJson } from './canonical-json.js';

export const STARTER_DISPLAY_NAME='SOMBRAS DE GELO E FERRO';
export const STARTER_MASTER_SEED='braseiro-wwn-sombras-gelo-ferro-v1';
export const STARTER_POSITION=Object.freeze({q:17,r:22,name:'RUÍNAS DA VIGÍLIA'});

function clone(v){return structuredClone(v)}
function canonicalClassId(value){
  const raw=String(value??'').trim();
  if(!raw)return null;
  const id=raw.toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_+|_+$/g,'');
  if(id==='FULL_EXPERT'||id==='PARTIAL_EXPERT')return 'EXPERT';
  if(id==='FULL_WARRIOR'||id==='PARTIAL_WARRIOR')return 'WARRIOR';
  return id;
}
function normalizeCharacter(raw){
  if(!raw||typeof raw!=='object')throw new TypeError('canonical starter character required');
  const c=clone(raw);delete c.fixtureKind;delete c.authority;delete c.evidencePolicy;
  c.characterId=String(c.characterId??c.id??'iria');
  c.classId=canonicalClassId(c.classId??c.class);
  if(Array.isArray(c.partialClasses))c.partialClasses=c.partialClasses.map(canonicalClassId).filter(Boolean);
  if(!c.name)throw new Error('starter character name required');
  if(!c.classId)throw new Error('starter character classId required');
  return c;
}

export function makeStarterCampaignState({character=null,pack,campaignId=STARTER_CAMPAIGN_ID,displayName=STARTER_DISPLAY_NAME,masterSeed=STARTER_MASTER_SEED}={}){
  let state=makeEmptyCampaignState({campaignId,displayName,masterSeed});
  state.worldState={...state.worldState,position:clone(STARTER_POSITION),elapsedDays:0};
  if(character){
    const c=normalizeCharacter(character);
    state.characterState=c;state.charactersState={[c.characterId]:clone(c)};
    state.partyState=createPartyState({memberIds:[c.characterId],position:state.worldState.position});
  }
  state=structuredClone(applyStarterContentToState(state,pack));
  return state;
}

export function ensureStarterCampaign({persistence,character,pack,campaignId=STARTER_CAMPAIGN_ID,startWithCharacter=false}={}){
  if(!persistence)throw new TypeError('persistence required');
  const existing=persistence.loadCampaign(campaignId);
  if(existing.status==='OK')return Object.freeze({status:'OK_EXISTING',campaignId,revision:existing.revision,state:JSON.parse(existing.stateJson)});
  if(existing.status!=='NOT_FOUND')return Object.freeze({status:existing.status,campaignId});
  let state=makeStarterCampaignState({character:startWithCharacter?character:null,pack,campaignId});
  const receipt=persistence.createCampaign({campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:`snapshot:${campaignId}:r0`});
  if(receipt.status!=='OK')return Object.freeze({status:receipt.status,campaignId,receipt,state});
  const opening=prepareCampaignOpening(state,{campaignId,nextRevision:receipt.newRevision+1});
  let openingReceipt=null,openingStatus=opening.status;
  if(opening.status==='OK'){
    openingReceipt=persistence.commit({campaignId,expectedRevision:receipt.newRevision,eventId:opening.eventId,eventKind:'SCENE_OPENED',eventPayloadJson:canonicalJson(opening.event),newStateJson:serializeCampaignState(opening.state),rngCountersJson:canonicalJson(opening.state.rngState?.counters??{})});
    openingStatus=openingReceipt.status;if(openingReceipt.status==='OK')state=opening.state;
  }
  return Object.freeze({status:'OK_CREATED',campaignId,receipt,openingStatus,openingReceipt,revision:openingReceipt?.status==='OK'?openingReceipt.newRevision:receipt.newRevision,state});
}