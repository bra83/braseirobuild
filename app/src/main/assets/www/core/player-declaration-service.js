import { canonicalJson } from './canonical-json.js';
import { projectSceneForPlayer } from './scene-projection.js';

function loadState(persistence,campaignId){
  const loaded=persistence.loadCampaign(campaignId);
  if(loaded.status!=='OK')return {loaded,state:null};
  const state=JSON.parse(loaded.stateJson);
  if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');
  return {loaded,state};
}

export class PlayerDeclarationService{
  constructor({persistence,barbara}){
    if(!persistence)throw new TypeError('persistence required');
    if(!barbara||typeof barbara.generate!=='function')throw new TypeError('barbara provider required');
    this.persistence=persistence;this.barbara=barbara;
  }
  async declare({campaignId,text}){
    const declaration=String(text??'').trim();
    if(!declaration)return Object.freeze({status:'DECLARATION_REQUIRED_NO_MUTATION'});
    if(declaration.length>1600)return Object.freeze({status:'DECLARATION_TOO_LONG_NO_MUTATION'});
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const projection=projectSceneForPlayer(state);let scene=projection.scene;let synthesizedScene=false;
    if(!scene||scene.status!=='OK'){const p=state.worldState?.position??{};const name=String(p.name??(Number.isInteger(p.q)&&Number.isInteger(p.r)?`HEX Q${p.q} • R${p.r}`:'POSIÇÃO ATUAL')).trim()||'POSIÇÃO ATUAL';scene=Object.freeze({status:'OK',contentType:'sandboxPosition',contentId:Number.isInteger(p.q)&&Number.isInteger(p.r)?`position-${p.q}-${p.r}`:'position-current',title:name,summary:`Você está em ${name}.`,situation:`Você está em ${name}.`,leads:Object.freeze([])});synthesizedScene=true;}
    const next=structuredClone(state);const newRevision=loaded.revision+1;next.revision=newRevision;if(synthesizedScene){next.encounterState={...(next.encounterState??{}),sceneId:`scene:sandbox:${scene.contentId}:r${newRevision}`,currentScene:{contentType:scene.contentType,contentId:scene.contentId,publicScene:structuredClone(scene)},scenePhase:'ACTIVE',sceneOpenedAtRevision:newRevision};}
    const eventId=`scene:${campaignId}:r${newRevision}:player-declaration`;
    next.journalState={...(next.journalState??{}),lastSceneEventId:eventId};
    next.encounterState={...(next.encounterState??{}),lastPlayerDeclaration:{text:declaration,revision:newRevision,eventId}};
    const event=Object.freeze({eventId,revision:newRevision,kind:'PLAYER_DECLARATION',declaration,scene,mechanicalMutation:false,timeAdvanced:false,positionChanged:false});
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    try{
      const narrative=this.barbara.generateAsync?await this.barbara.generateAsync({mechanicalEvent:event,state:next}):await Promise.resolve(this.barbara.generate({mechanicalEvent:event,state:next}));
      if(!String(narrative?.text??'').trim())return Object.freeze({status:'DECLARATION_COMMITTED_NARRATIVE_PENDING',receipt,narrative});
      const narrativeReceipt=this.persistence.appendNarrative({campaignId,eventId:`narrative:${eventId}`,mechanicalEventId:eventId,mechanicalRevision:newRevision,eventPayloadJson:canonicalJson(narrative)});
      return Object.freeze({status:'OK',receipt,narrativeReceipt,narrative,projection:projectSceneForPlayer(next)});
    }catch(error){return Object.freeze({status:'DECLARATION_COMMITTED_NARRATIVE_PENDING',receipt,narrativeError:String(error?.message??error),projection:projectSceneForPlayer(next)});}
  }
}
