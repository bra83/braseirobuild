import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import { resolveFirstAid, resolveNaturalNightRest, resolveStabilizationCheck, stabilizedRecoveryState, hasCondition } from './wwn-health-core.js';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function conditionList(character){const raw=character.conditions??[];if(Array.isArray(raw))return raw.map(c=>typeof c==='string'?c:(c?.id??c?.type??c?.name)).filter(Boolean);return Object.entries(raw).filter(([,v])=>Boolean(v)).map(([k])=>k);}
function syncActiveCharacter(state){const id=state.characterState?.characterId;if(id){state.charactersState={...(state.charactersState??{}),[id]:structuredClone(state.characterState)};}return state;}
function setConditions(character,remove=[],add=[]){const rm=new Set(remove.map(v=>String(v).toUpperCase()));const list=conditionList(character).filter(c=>!rm.has(String(c).toUpperCase()));for(const v of add)if(!list.some(c=>String(c).toUpperCase()===String(v).toUpperCase()))list.push(v);character.conditions=list;}
function commit({persistence,loaded,next,campaignId,eventKind,suffix,payload}){const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:${suffix}`;const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind,eventPayloadJson:canonicalJson({eventId,revision:newRevision,...payload}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});return{receipt,eventId,newRevision};}

export class HealthService{
  constructor({persistence}){this.persistence=persistence;}

  nightRest({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(!state.characterState)return Object.freeze({status:'CHARACTER_REQUIRED_NO_MUTATION',revision:loaded.revision});
    if(!contract||typeof contract!=='object')return Object.freeze({status:'REST_CONDITIONS_REQUIRED_NO_MUTATION',revision:loaded.revision});
    for(const key of ['warm','fed','comfortable','uninterruptedHours'])if(contract[key]===undefined)return Object.freeze({status:`REST_${key.toUpperCase()}_REQUIRED_NO_MUTATION`,revision:loaded.revision});
    let resolution;try{resolution=resolveNaturalNightRest({character:state.characterState,...contract});}catch(error){return Object.freeze({status:'INVALID_REST_CONTRACT_NO_MUTATION',revision:loaded.revision,reason:error.message});}
    const next=structuredClone(state);next.characterState.hp.current=resolution.hpAfter;next.characterState.systemStrain={...(next.characterState.systemStrain??{}),current:resolution.systemStrainAfter,max:next.characterState.systemStrain?.max??next.characterState.attributes?.Constitution};
    next.worldState={...(next.worldState??{}),lastRest:{...resolution,contract:{warm:contract.warm,fed:contract.fed,comfortable:contract.comfortable,uninterruptedHours:contract.uninterruptedHours}}};
    syncActiveCharacter(next);const {receipt}=commit({persistence:this.persistence,loaded,next,campaignId,eventKind:'WWN_NIGHT_REST_RESOLVED',suffix:'night-rest',payload:{resolution}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:resolution.status,receipt,resolution,characterState:structuredClone(next.characterState)});
  }

  firstAid({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});if(!state.characterState)return Object.freeze({status:'CHARACTER_REQUIRED_NO_MUTATION',revision:loaded.revision});
    if(!contract||typeof contract!=='object')return Object.freeze({status:'FIRST_AID_CONTRACT_REQUIRED_NO_MUTATION',revision:loaded.revision});
    if(contract.hasHealersKit!==true)return Object.freeze({status:'HEALERS_KIT_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const rng=DeterministicRng.fromState(state.rngState);let resolution;try{resolution=resolveFirstAid({rng,target:state.characterState,healSkillLevel:contract.healSkillLevel??null,hasHealersKit:true,streamName:`health/first-aid/r${loaded.revision+1}`});}catch(error){return Object.freeze({status:'INVALID_FIRST_AID_CONTRACT_NO_MUTATION',revision:loaded.revision,reason:error.message});}
    if(resolution.status!=='OK')return Object.freeze({status:`${resolution.status}_NO_MUTATION`,revision:loaded.revision,resolution});
    const next=structuredClone(state);next.rngState=rng.exportState();next.characterState.hp.current=resolution.hpAfter;next.characterState.systemStrain.current=resolution.systemStrainAfter;
    syncActiveCharacter(next);const {receipt}=commit({persistence:this.persistence,loaded,next,campaignId,eventKind:'WWN_FIRST_AID_RESOLVED',suffix:'first-aid',payload:{resolution}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,resolution,characterState:structuredClone(next.characterState)});
  }

  stabilize({campaignId,contract}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const pc=state.characterState;if(!pc)return Object.freeze({status:'CHARACTER_REQUIRED_NO_MUTATION',revision:loaded.revision});
    if(pc.hp?.current!==0||!hasCondition(pc,'MORTALLY_INJURED'))return Object.freeze({status:'TARGET_NOT_MORTALLY_INJURED_NO_MUTATION',revision:loaded.revision});
    if(!contract||typeof contract!=='object'||contract.attributeScore===undefined||contract.roundsSinceDowned===undefined)return Object.freeze({status:'STABILIZATION_CONTRACT_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const rng=DeterministicRng.fromState(state.rngState);let resolution;try{resolution=resolveStabilizationCheck({rng,attributeScore:contract.attributeScore,healSkillLevel:contract.healSkillLevel??null,roundsSinceDowned:contract.roundsSinceDowned,hasMedicalTools:contract.hasMedicalTools!==false,aidBonus:contract.aidBonus??0,streamName:`health/stabilize/r${loaded.revision+1}`});}catch(error){return Object.freeze({status:'INVALID_STABILIZATION_CONTRACT_NO_MUTATION',revision:loaded.revision,reason:error.message});}
    const next=structuredClone(state);next.rngState=rng.exportState();next.characterState.mortalInjury={...(next.characterState.mortalInjury??{}),roundsSinceDowned:contract.roundsSinceDowned,stabilized:resolution.success,stabilizedAtRevision:resolution.success?loaded.revision+1:null};
    syncActiveCharacter(next);const {receipt}=commit({persistence:this.persistence,loaded,next,campaignId,eventKind:'WWN_STABILIZATION_ATTEMPT',suffix:'stabilize',payload:{resolution}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:resolution.success?'STABILIZED':'FAILED_STILL_MORTALLY_INJURED',receipt,resolution,characterState:structuredClone(next.characterState)});
  }

  completeStabilizedRecovery({campaignId,elapsedMinutes}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const pc=state.characterState;if(!pc)return Object.freeze({status:'CHARACTER_REQUIRED_NO_MUTATION',revision:loaded.revision});
    if(pc.mortalInjury?.stabilized!==true)return Object.freeze({status:'NOT_STABILIZED_NO_MUTATION',revision:loaded.revision});if(!Number.isInteger(elapsedMinutes)||elapsedMinutes<10)return Object.freeze({status:'TEN_MINUTES_NOT_ELAPSED_NO_MUTATION',revision:loaded.revision});
    const resolution=stabilizedRecoveryState(pc);const next=structuredClone(state);next.characterState.hp.current=1;setConditions(next.characterState,['MORTALLY_INJURED'],['FRAIL']);next.characterState.mortalInjury={...next.characterState.mortalInjury,recovered:true,recoveredAfterMinutes:elapsedMinutes};
    syncActiveCharacter(next);const {receipt}=commit({persistence:this.persistence,loaded,next,campaignId,eventKind:'WWN_STABILIZED_RECOVERY_COMPLETED',suffix:'stabilized-recovery',payload:{resolution,elapsedMinutes}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,resolution,characterState:structuredClone(next.characterState)});
  }
}
