import { canonicalJson } from './canonical-json.js';
const SECRET_KEYS=/api[_-]?key|token|credential|password|secretKey|authorization/i;
const HIDDEN_KEYS=/gmSecrets|privateState|hiddenState|credentials|runtimeSecrets/i;
function scrub(value){
  if(Array.isArray(value))return value.map(scrub);
  if(!value||typeof value!=='object')return value;
  const out={};for(const [k,v] of Object.entries(value)){if(SECRET_KEYS.test(k)||HIDDEN_KEYS.test(k))continue;if(k==='visibility'&&String(v).toUpperCase()==='GM_ONLY')return undefined;const cleaned=scrub(v);if(cleaned!==undefined)out[k]=cleaned;}return out;
}
export function buildFilteredNarrativeContext({mechanicalEvent,state,ragSnippets=[]}){
  if(!mechanicalEvent?.eventId)throw new Error('committed mechanicalEvent required');
  const safeState=scrub({campaignId:state?.campaignId,revision:state?.revision,worldState:state?.worldState,encounterState:state?.encounterState,characterState:state?.characterState,npcState:state?.npcState});
  const safeSnippets=ragSnippets.filter(s=>s?.visibility!=='GM_ONLY').map(s=>scrub(s));
  return Object.freeze({mechanicalEvent:scrub(mechanicalEvent),state:safeState,ragSnippets:safeSnippets});
}
export class DeterministicNarrativeProvider{
  generate({mechanicalEvent,state,ragSnippets=[]}){const context=buildFilteredNarrativeContext({mechanicalEvent,state,ragSnippets});const result=mechanicalEvent.resolution?.success===true?'sucesso':mechanicalEvent.resolution?.success===false?'falha':'resolução';return Object.freeze({provider:'DETERMINISTIC_MOCK',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:`O Mestre narra a ${result} confirmada do evento ${mechanicalEvent.eventId}.`,contextDigest:canonicalJson(context).length});}
}
export class GeminiNarrativeProvider{
  constructor({transport,secretProvider,model='gemini',maxRetries=1}){if(typeof transport!=='function')throw new Error('transport required');if(typeof secretProvider!=='function')throw new Error('secretProvider required');this.transport=transport;this.secretProvider=secretProvider;this.model=model;this.maxRetries=maxRetries;}
  async generate({mechanicalEvent,state,ragSnippets=[]}){const context=buildFilteredNarrativeContext({mechanicalEvent,state,ragSnippets});const secret=await this.secretProvider();if(!secret)return Object.freeze({status:'REAL_GEMINI_CREDENTIAL_UNAVAILABLE'});const request={model:this.model,context};let lastError=null;for(let attempt=0;attempt<=this.maxRetries;attempt+=1){try{const response=await this.transport({request,secret});if(!response?.text)throw new Error('empty Gemini response');return Object.freeze({status:'OK',provider:'GEMINI',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:String(response.text),attempts:attempt+1});}catch(error){lastError=error;}}return Object.freeze({status:'NARRATIVE_PROVIDER_FAILED_NO_MECHANICAL_RETRY',parentMechanicalEventId:mechanicalEvent.eventId,error:String(lastError?.message??lastError)});}
}
