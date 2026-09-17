import { canonicalJson } from './canonical-json.js';

function clone(v){return structuredClone(v)}
function load(persistence,campaignId){
  const loaded=persistence?.loadCampaign?.(campaignId);
  if(!loaded||loaded.status!=='OK')return{loaded:loaded??{status:'CAMPAIGN_UNAVAILABLE'},state:null};
  try{return{loaded,state:JSON.parse(loaded.stateJson)}}catch{return{loaded:{...loaded,status:'CORRUPT_STATE'},state:null}}
}
function normalizeImage(result){
  const url=String(result?.url??'').trim();
  if(!url)return null;
  return{url,sha256:String(result?.sha256??'').trim()||null,width:Number.isFinite(Number(result?.width))?Number(result.width):null,height:Number.isFinite(Number(result?.height))?Number(result.height):null};
}
function commitState({persistence,campaignId,loaded,next,eventKind,eventPayload}){
  const newRevision=loaded.revision+1;next.revision=newRevision;
  const eventId=`scene-image:${campaignId}:r${newRevision}:${eventKind.toLowerCase()}`;
  const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind,eventPayloadJson:canonicalJson({eventId,revision:newRevision,...eventPayload,mechanicalMutation:false}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
  return Object.freeze({status:receipt.status,receipt,eventId,revision:receipt.status==='OK'?receipt.newRevision:loaded.revision});
}

export function pendingSceneImageFromState(state){
  const p=state?.encounterState?.pendingSceneImage;
  if(!p?.url)return null;
  return clone(p);
}

export function storePendingSceneImageForCampaign({persistence,campaignId,result}={}){
  const image=normalizeImage(result);if(!image)return Object.freeze({status:'IMAGE_URL_REQUIRED_NO_MUTATION'});
  const {loaded,state}=load(persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
  const next=clone(state);next.encounterState={...(next.encounterState??{}),pendingSceneImage:{...image,campaignId,receivedAtRevision:loaded.revision+1}};
  return commitState({persistence,campaignId,loaded,next,eventKind:'SCENE_IMAGE_PENDING',eventPayload:{image}});
}

export function attachSceneImageToCampaign({persistence,campaignId,result,generated=false,interactionId=null,model=null}={}){
  const image=normalizeImage(result);if(!image)return Object.freeze({status:'IMAGE_URL_REQUIRED_NO_MUTATION'});
  const {loaded,state}=load(persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
  const current=state.encounterState?.currentScene;if(!current)return Object.freeze({status:'ACTIVE_SCENE_REQUIRED_NO_MUTATION'});
  const next=clone(state);next.encounterState={...(next.encounterState??{}),currentScene:{...current,imageRef:image.url,imageSha256:image.sha256}};
  delete next.encounterState.pendingSceneImage;
  if(generated){const prior=next.aiState?.sceneVisualSession??{};next.aiState={...(next.aiState??{}),sceneVisualSession:{previousInteractionId:interactionId??prior.previousInteractionId??null,model:model??prior.model??null,sequence:(prior.sequence??0)+1,lastSceneRevision:loaded.revision+1,lastImageSha256:image.sha256??null}}}
  return commitState({persistence,campaignId,loaded,next,eventKind:generated?'SCENE_IMAGE_GENERATED':'SCENE_IMAGE_ATTACHED',eventPayload:{image,generated,interactionId:interactionId??null,model:model??null}});
}

export function removeSceneImageFromCampaign({persistence,campaignId}={}){
  const {loaded,state}=load(persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
  const current=state.encounterState?.currentScene;const pending=state.encounterState?.pendingSceneImage;
  if(!current?.imageRef&&!current?.imageUrl&&!pending?.url)return Object.freeze({status:'NO_SCENE_IMAGE_NO_MUTATION'});
  const next=clone(state);
  if(next.encounterState?.currentScene){delete next.encounterState.currentScene.imageRef;delete next.encounterState.currentScene.imageUrl;delete next.encounterState.currentScene.imageSha256}
  if(next.encounterState)delete next.encounterState.pendingSceneImage;
  return commitState({persistence,campaignId,loaded,next,eventKind:'SCENE_IMAGE_REMOVED',eventPayload:{removed:true}});
}
