import { makeEmptyCampaignState, serializeCampaignState } from './campaign-state.js';
import { createPartyState } from './party-state.js';
import { applyStarterContentToState } from './starter-content-service.js';
import { STARTER_CAMPAIGN_ID } from './runtime-campaign.js';

export const STARTER_DISPLAY_NAME='SOMBRAS DE GELO E FERRO';
export const STARTER_MASTER_SEED='braseiro-wwn-sombras-gelo-ferro-v1';
export const STARTER_POSITION=Object.freeze({q:17,r:22,name:'RUÍNAS DA VIGÍLIA'});

function clone(v){return structuredClone(v)}
function normalizeCharacter(raw){
  if(!raw||typeof raw!=='object')throw new TypeError('canonical starter character required');
  const c=clone(raw);delete c.fixtureKind;delete c.authority;delete c.evidencePolicy;
  c.characterId=String(c.characterId??c.id??'iria');
  c.classId=c.classId??c.class??null;
  if(!c.name)throw new Error('starter character name required');
  return c;
}

export function makeStarterCampaignState({character,pack,campaignId=STARTER_CAMPAIGN_ID,displayName=STARTER_DISPLAY_NAME,masterSeed=STARTER_MASTER_SEED}={}){
  const c=normalizeCharacter(character);let state=makeEmptyCampaignState({campaignId,displayName,masterSeed});
  state.characterState=c;state.charactersState={[c.characterId]:clone(c)};
  state.worldState={...state.worldState,position:clone(STARTER_POSITION),elapsedDays:0};
  state.partyState=createPartyState({memberIds:[c.characterId],position:state.worldState.position});
  state=structuredClone(applyStarterContentToState(state,pack));
  return state;
}

export function ensureStarterCampaign({persistence,character,pack,campaignId=STARTER_CAMPAIGN_ID}={}){
  if(!persistence)throw new TypeError('persistence required');
  const existing=persistence.loadCampaign(campaignId);
  if(existing.status==='OK')return Object.freeze({status:'OK_EXISTING',campaignId,revision:existing.revision,state:JSON.parse(existing.stateJson)});
  if(existing.status!=='NOT_FOUND')return Object.freeze({status:existing.status,campaignId});
  const state=makeStarterCampaignState({character,pack,campaignId});
  const receipt=persistence.createCampaign({campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:`snapshot:${campaignId}:r0`});
  return Object.freeze({status:receipt.status==='OK'?'OK_CREATED':receipt.status,campaignId,receipt,state});
}
