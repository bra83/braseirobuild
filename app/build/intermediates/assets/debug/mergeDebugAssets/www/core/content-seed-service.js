import { canonicalJson } from './canonical-json.js';
import { applyStarterContentToState } from './starter-content-service.js';
export class ContentSeedService{
  constructor({persistence}){this.persistence=persistence;}
  seed({campaignId,pack}){
    const loaded=this.persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return Object.freeze({status:loaded.status});
    const state=JSON.parse(loaded.stateJson);if(state.contentState?.public?.packId===pack?.pack_id)return Object.freeze({status:'CONTENT_ALREADY_SEEDED_NO_MUTATION',revision:loaded.revision});
    const next=structuredClone(applyStarterContentToState(state,pack));const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:content-seed`;
    const event={eventId,revision:newRevision,kind:'CONTENT_SEEDED',packId:pack.pack_id,mechanicalMutation:false};
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
    return Object.freeze({status:receipt.status,receipt,contentState:receipt.status==='OK'?structuredClone(next.contentState):null,projectedState:receipt.status==='OK'?next:null});
  }
}
