import { canonicalJson } from './canonical-json.js';
import { resolvePublicSceneContent, projectSceneForPlayer } from './scene-projection.js';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function latestNarrativeFor(events,parentEventId){for(let i=(events?.length??0)-1;i>=0;i--){const e=events[i];if(e.eventKind!=='NARRATIVE')continue;try{const p=JSON.parse(e.payloadJson);if(p.parentMechanicalEventId===parentEventId)return p;}catch{}}return null;}

export class SceneSessionService{
  constructor({persistence,barbara}){if(!persistence)throw new TypeError('persistence required');if(!barbara||typeof barbara.generate!=='function')throw new TypeError('barbara provider required');this.persistence=persistence;this.barbara=barbara;}
  async openContentScene({campaignId,contentType,contentId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const publicScene=resolvePublicSceneContent(state,{contentType,contentId});if(publicScene.status!=='OK')return Object.freeze({status:'CONTENT_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const existing=state.encounterState?.currentScene;
    if(existing?.contentType===contentType&&existing?.contentId===contentId)return Object.freeze({status:'SCENE_ALREADY_OPEN_NO_MUTATION',revision:loaded.revision,scene:publicScene,projection:projectSceneForPlayer(state)});
    const newRevision=loaded.revision+1;const eventId=`scene:${campaignId}:r${newRevision}:${contentType}:${contentId}`;
    const next=structuredClone(state);next.revision=newRevision;
    next.encounterState={...(next.encounterState??{}),sceneId:`scene:${contentType}:${contentId}:r${newRevision}`,currentScene:{contentType,contentId},scenePhase:'ACTIVE',sceneOpenedAtRevision:newRevision};
    next.journalState={...(next.journalState??{}),lastSceneEventId:eventId};
    const sceneEvent=Object.freeze({eventId,revision:newRevision,kind:'SCENE_OPENED',contentType,contentId,scene:publicScene,mechanicalMutation:false,timeAdvanced:false,positionChanged:false});
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:sceneEvent.kind,eventPayloadJson:canonicalJson(sceneEvent),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    try{
      const narrative=this.barbara.generateAsync?await this.barbara.generateAsync({mechanicalEvent:sceneEvent,state:next}):await this.barbara.generate({mechanicalEvent:sceneEvent,state:next});
      const narrativeReceipt=this.persistence.appendNarrative({campaignId,eventId:`narrative:${eventId}`,mechanicalEventId:eventId,mechanicalRevision:newRevision,eventPayloadJson:canonicalJson(narrative)});
      return Object.freeze({status:'OK',receipt,narrativeReceipt,scene:publicScene,narrative,projection:projectSceneForPlayer(next)});
    }catch(error){return Object.freeze({status:'SCENE_COMMITTED_NARRATIVE_PENDING',receipt,scene:publicScene,projection:projectSceneForPlayer(next),narrativeError:String(error?.message??error)});}
  }
  load({campaignId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const parent=state.journalState?.lastSceneEventId??null;let narrative=null;
    if(parent&&typeof this.persistence.latestNarrative==='function'){const r=this.persistence.latestNarrative(campaignId,parent);if(r?.status==='OK'&&r.event?.payloadJson){try{narrative=JSON.parse(r.event.payloadJson)}catch{}}}
    else if(parent){const events=this.persistence.listEvents(campaignId)?.events??[];narrative=latestNarrativeFor(events,parent);}
    return Object.freeze({status:'OK',revision:loaded.revision,projection:projectSceneForPlayer(state),narrative});
  }
}
