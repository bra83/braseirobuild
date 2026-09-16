function clone(v){return structuredClone(v)}
function norm(v){return String(v??'').normalize('NFKD').replace(/[\u0300-\u036f]/g,'').trim().toUpperCase()}
function currentSite(state){const name=norm(state?.worldState?.position?.name);if(!name)return null;return (state?.contentState?.public?.majorSites??[]).find(site=>norm(site?.name)===name)??null}

export function prepareCampaignOpening(state,{campaignId=state?.campaignId,nextRevision=(state?.revision??0)+1}={}){
  if(!state||typeof state!=='object')return Object.freeze({status:'STATE_REQUIRED'});
  const position=state.worldState?.position;if(!Number.isInteger(position?.q)||!Number.isInteger(position?.r))return Object.freeze({status:'OPENING_POSITION_REQUIRED'});
  const site=currentSite(state);const region=state.contentState?.public?.region??null;
  const placeName=String(position.name??site?.name??`Q${position.q} • R${position.r}`).trim();
  const summary=String(site?.summary??region?.tone??`A campanha começa em ${placeName}.`).trim();
  const situation=String(site?.situation??'').trim()||null;
  const contentId=`opening-${position.q}-${position.r}`;
  const publicScene=Object.freeze({status:'OK',contentType:'campaignOpening',contentId,title:`ABERTURA • ${placeName}`,summary,situation,leads:Object.freeze([]),narrativeScale:'MAJOR_OPENING'});
  const eventId=`scene:${campaignId}:r${nextRevision}:campaign-opening`;
  const next=clone(state);next.revision=nextRevision;
  next.encounterState={...(next.encounterState??{}),sceneId:`scene:campaignOpening:${contentId}:r${nextRevision}`,currentScene:{contentType:'campaignOpening',contentId,publicScene:clone(publicScene)},scenePhase:'ACTIVE',sceneOpenedAtRevision:nextRevision};
  next.journalState={...(next.journalState??{}),lastMechanicalEventId:eventId,lastSceneEventId:eventId};
  next.worldState=next.worldState??{};next.worldState.atlas=next.worldState.atlas??{hexes:{}};next.worldState.atlas.hexes=next.worldState.atlas.hexes??{};
  const key=`${position.q},${position.r}`,prior=next.worldState.atlas.hexes[key]??{};
  next.worldState.atlas.hexes[key]={...prior,q:position.q,r:position.r,discovered:true,entered:true,visited:true,poiRevealed:true,placeName,poiName:site?.name??prior.poiName??placeName,poiType:site?.kind??prior.poiType??null,enteredAtRevision:prior.enteredAtRevision??nextRevision};
  const openingContext={campaign:state.displayName??null,region:region?{name:region.name??null,tone:region.tone??null,scope:region.scope??null}:null,position:clone(position),currentSite:site?{name:site.name,kind:site.kind,summary:site.summary,situation:site.situation}:null};
  const event=Object.freeze({eventId,revision:nextRevision,kind:'SCENE_OPENED',contentType:'campaignOpening',contentId,scene:publicScene,openingContext,mechanicalMutation:false,timeAdvanced:false,positionChanged:false,narrativeScale:'MAJOR_OPENING'});
  return Object.freeze({status:'OK',state:next,event,scene:publicScene,eventId});
}