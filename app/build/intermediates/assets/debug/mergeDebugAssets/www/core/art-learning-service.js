import { canonicalJson } from './canonical-json.js';
import { classArtCatalog, classProfile } from './wwn-srd-class-catalog.js';

const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5 Class Arts'});
function load(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function roster(state){const r={...(state.charactersState??{})};if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=structuredClone(state.characterState);return r;}
function sourceClass(c,sourceClassId){const cid=String(c.classId??'').toUpperCase();if(cid!=='ADVENTURER')return cid;const parts=(c.partialClasses??[]).map(x=>String(x).toUpperCase()),chosen=String(sourceClassId??'').toUpperCase();return chosen&&parts.includes(chosen)?chosen:null;}
function artName(x){return String(x?.artId??x?.name??x);}
function known(c){return new Set((c.arts??[]).map(artName));}
export function validateArtChoice({character,sourceClassId=null,artName:requested}){
 const cid=sourceClass(character,sourceClassId);if(!cid)return Object.freeze({ok:false,status:'INVALID_ART_SOURCE_NO_MUTATION'});
 const profile=classProfile(cid);if(!profile)return Object.freeze({ok:false,status:'UNKNOWN_ART_CLASS_NO_MUTATION'});
 const name=String(requested??'');const catalog=classArtCatalog(cid);if(!catalog.includes(name))return Object.freeze({ok:false,status:'UNKNOWN_CLASS_ART_NO_MUTATION',classId:cid});
 if(known(character).has(name))return Object.freeze({ok:false,status:'ART_ALREADY_KNOWN_NO_MUTATION',classId:cid});
 if((profile.initialTraining?.fixedArts??[]).includes(name))return Object.freeze({ok:false,status:'FIXED_ART_NOT_SELECTABLE_NO_MUTATION',classId:cid});
 if(cid==='MAGESLAYER')return Object.freeze({ok:false,status:'MAGESLAYER_ARTS_USE_FIXED_PROGRESSION_NO_MUTATION',classId:cid});
 if(cid==='HEALER'&&['Limb Restoration','Revive the Fallen'].includes(name)&&Number(character.level??1)<8)return Object.freeze({ok:false,status:'ART_LEVEL_REQUIREMENT_NO_MUTATION',classId:cid,minimumLevel:8});
 return Object.freeze({ok:true,status:'OK',classId:cid,name,profile,sourceRefs:Object.freeze([profile.sourceRef??SRC])});
}
export class ArtLearningService{
 constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
 listChoices({character,sourceClassId=null}){
  const cid=sourceClass(character,sourceClassId);if(!cid)return Object.freeze({status:'INVALID_ART_SOURCE',choices:Object.freeze([])});
  const p=classProfile(cid);if(!p)return Object.freeze({status:'UNKNOWN_ART_CLASS',choices:Object.freeze([])});
  const choices=classArtCatalog(cid).filter(name=>validateArtChoice({character,sourceClassId:cid,artName:name}).ok);
  return Object.freeze({status:'OK',classId:cid,choices:Object.freeze(choices)});
 }
 learnPendingArt({campaignId,characterId=null,sourceClassId=null,artName:requested}){
  const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const r=roster(state),id=characterId??state.characterState?.characterId,c=r[id];if(!c)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
  const pending=Number(c.pendingAdvancement?.artPicks??0);if(pending<1)return Object.freeze({status:'NO_PENDING_ART_PICK_NO_MUTATION',revision:loaded.revision});
  const check=validateArtChoice({character:c,sourceClassId,artName:requested});if(!check.ok)return Object.freeze({...check,revision:loaded.revision});
  const next=structuredClone(state),nr=roster(next),nc=structuredClone(c);nc.arts=[...(nc.arts??[]),{artId:check.name,name:check.name,classId:check.classId,sourceRef:check.profile.sourceRef??SRC}];nc.pendingAdvancement={...(nc.pendingAdvancement??{}),artPicks:pending-1};nr[id]=nc;next.charactersState=nr;if(next.characterState?.characterId===id)next.characterState=structuredClone(nc);
  const revision=loaded.revision+1;next.revision=revision;const eventId=`mech:${campaignId}:r${revision}:learn-art:${id}:${check.classId}:${check.name.replace(/[^A-Za-z0-9]+/g,'_')}`;const payload={eventId,revision,kind:'CHARACTER_LEARNED_ART',characterId:id,classId:check.classId,artName:check.name,mechanicalMutation:true,sourceRefs:check.sourceRefs};const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:payload.kind,eventPayloadJson:canonicalJson(payload),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});return Object.freeze({status:receipt.status,receipt,event:payload,character:receipt.status==='OK'?nc:null,projectedState:receipt.status==='OK'?next:null});
 }
}
