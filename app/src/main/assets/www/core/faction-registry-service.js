import {canonicalJson} from './canonical-json.js';
import {factionMaximumHp} from './wwn-faction-core.js';
function clone(v){return structuredClone(v)}
function load(p,c){const r=p.loadCampaign(c);if(r.status!=='OK')return{r,state:null};return{r,state:JSON.parse(r.stateJson)}}
function cleanText(v){const s=String(v??'').trim();return s||null}
function rating(v){const n=Number(v);return Number.isInteger(n)&&n>=1&&n<=8?n:null}
function commit(p,r,s,c,kind,suffix,payload){const revision=r.revision+1;s.revision=revision;const eventId=`mech:${c}:r${revision}:faction-registry:${suffix}`;return p.commit({campaignId:c,expectedRevision:r.revision,eventId,eventKind:kind,eventPayloadJson:canonicalJson({eventId,revision,...payload}),newStateJson:canonicalJson(s),rngCountersJson:canonicalJson(s.rngState?.counters??{})})}
export class FactionRegistryService{
 constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence}
 list({campaignId}){const {r,state}=load(this.persistence,campaignId);return Object.freeze({status:state?'OK':r.status,factions:state?clone(state.factionState??{}):{}})}
 upsert({campaignId,factionId,name,force,cunning,wealth,treasure=0,magic='NONE',goal=null,home=null,tag=null}){
  const id=cleanText(factionId),nm=cleanText(name),f=rating(force),c=rating(cunning),w=rating(wealth),tr=Number(treasure);const mg=String(magic??'NONE').toUpperCase();
  if(!id||!nm)return Object.freeze({status:'FACTION_ID_AND_NAME_REQUIRED_NO_MUTATION'});if(!f||!c||!w)return Object.freeze({status:'FACTION_ATTRIBUTES_1_TO_8_REQUIRED_NO_MUTATION'});if(!Number.isInteger(tr)||tr<0)return Object.freeze({status:'FACTION_TREASURE_INVALID_NO_MUTATION'});if(!['NONE','LOW','MEDIUM','HIGH'].includes(mg))return Object.freeze({status:'FACTION_MAGIC_INVALID_NO_MUTATION'});
  const {r,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:r.status});if(state.worldState?.factionTurn?.phase&&state.worldState.factionTurn.phase!=='TURN_COMPLETE')return Object.freeze({status:'ACTIVE_FACTION_TURN_NO_MUTATION'});
  const previous=state.factionState?.[id]??null,maxHp=factionMaximumHp({force:f,cunning:c,wealth:w});const next=clone(state);next.factionState={...(next.factionState??{})};next.factionState[id]={...(previous??{}),factionId:id,id,name:nm,force:f,cunning:c,wealth:w,maxHp,hp:previous?Math.min(maxHp,Math.max(0,Number(previous.hp??maxHp))):maxHp,treasure:tr,magic:mg,goal:cleanText(goal),home:cleanText(home),tag:cleanText(tag),assets:clone(previous?.assets??{}),bases:clone(previous?.bases??{}),authority:'GM_EXPLICIT_WWN_FACTION'};
  const receipt=commit(this.persistence,r,next,campaignId,previous?'FACTION_UPDATED':'FACTION_CREATED',id,{factionId:id,name:nm,force:f,cunning:c,wealth:w,maxHp,treasure:tr,magic:mg,goal:cleanText(goal),home:cleanText(home),tag:cleanText(tag),authority:'GM_EXPLICIT_WWN_FACTION'});return Object.freeze({status:receipt.status,receipt,faction:receipt.status==='OK'?clone(next.factionState[id]):null});
 }
 remove({campaignId,factionId}){const id=cleanText(factionId);if(!id)return Object.freeze({status:'FACTION_ID_REQUIRED_NO_MUTATION'});const {r,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:r.status});if(!state.factionState?.[id])return Object.freeze({status:'FACTION_NOT_FOUND_NO_MUTATION'});if(state.worldState?.factionTurn?.phase&&state.worldState.factionTurn.phase!=='TURN_COMPLETE')return Object.freeze({status:'ACTIVE_FACTION_TURN_NO_MUTATION'});const next=clone(state);delete next.factionState[id];const receipt=commit(this.persistence,r,next,campaignId,'FACTION_REMOVED',`${id}:remove`,{factionId:id});return Object.freeze({status:receipt.status,receipt});}
}
