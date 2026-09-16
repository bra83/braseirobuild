import { projectPartyForPlayer } from './party-state.js';
import { projectNpcForPlayer } from './npc-registry.js';
function clone(v){return structuredClone(v)}

function findPublicContent(content,type,id){
  if(!content)return null;
  if(type==='startingHook')return (content.startingHooks??[]).find(x=>x.id===id)??null;
  if(type==='majorSite')return (content.majorSites??[]).find(x=>x.id===id)??null;
  if(type==='minorPoi')return (content.minorPois??[]).find(x=>(x.id??x.name)===id)??null;
  if(type==='settlement')return content.settlement && (content.settlement.id===id||content.settlement.name===id)?content.settlement:null;
  return null;
}
export function resolvePublicSceneContent(state,{contentType,contentId}){
  const item=findPublicContent(state?.contentState?.public,contentType,contentId);if(!item)return Object.freeze({status:'CONTENT_NOT_FOUND'});
  const title=item.title??item.name??contentId;
  const summary=item.premise??item.summary??item.situation??'';
  const situation=item.situation??null;
  const leads=clone(item.leads??item.hooks??[]);
  return Object.freeze({status:'OK',contentType,contentId,title,summary,situation,leads});
}
export function projectSceneForPlayer(state){
  const current=state?.encounterState?.currentScene??null;
  const direct=current?.publicScene&&typeof current.publicScene==='object'?Object.freeze({...clone(current.publicScene),status:'OK'}):null;
  const scene=direct??(current?resolvePublicSceneContent(state,current):Object.freeze({status:'NO_ACTIVE_SCENE'}));
  return Object.freeze({
    status:scene.status==='OK'?'OK':scene.status,
    campaignId:state?.campaignId??null,
    displayName:state?.displayName??null,
    world:Object.freeze({position:clone(state?.worldState?.position??null),time:clone(state?.worldState?.time??null),weather:clone(state?.worldState?.weather??null)}),
    scene:scene.status==='OK'?Object.freeze({...scene,imageRef:current?.imageRef??null,imageSha256:current?.imageSha256??null}):null,
    party:projectPartyForPlayer(state),
    npcs:Object.freeze(Object.fromEntries(Object.entries(state?.npcState??{}).map(([id,npc])=>[id,projectNpcForPlayer(npc)]))),
    content:clone(state?.contentState?.public??null),
  });
}
