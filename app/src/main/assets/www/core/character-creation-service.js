import { canonicalJson } from './canonical-json.js';
import { finalizeCharacterCreation } from './wwn-character-creation-core.js';
import { materializeRandomLevelOneCharacter } from './wwn-srd-random-character.js';
import { validateAttributeGeneration } from './wwn-attribute-generation-core.js';
import { createPartyState } from './party-state.js';

function clone(v){return structuredClone(v)}
function rosterOf(state){const r={...(state.charactersState??{})};if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=clone(state.characterState);return r;}
function loadFresh(persistence,campaignId){
  const loaded=persistence.loadCampaign(campaignId,{fresh:true});
  if(loaded.status!=='OK')return{loaded,state:null};
  const state=JSON.parse(loaded.stateJson);
  if(Number(state.revision)!==Number(loaded.revision))throw new Error('State/load revision mismatch');
  return{loaded,state};
}
function partyHasCharacter(state,characterId){
  return Object.values(state?.partyState?.groups??{}).some(g=>(g?.memberIds??[]).includes(characterId));
}
function addToParty(next,characterId){
  if(!next.partyState){next.partyState=createPartyState({memberIds:Object.keys(next.charactersState),position:next.worldState?.position??null});return;}
  const groups=next.partyState.groups??{};
  if(Object.values(groups).some(g=>(g.memberIds??[]).includes(characterId)))return;
  const target=next.partyState.activeGroupId;
  if(!groups[target])throw new Error('active party group missing');
  groups[target].memberIds.push(characterId);
}
function buildCreatedState(state,character,setActive){
  const next=clone(state);
  next.charactersState=rosterOf(next);
  next.charactersState[character.characterId]=clone(character);
  if(setActive||!next.characterState)next.characterState=clone(character);
  addToParty(next,character.characterId);
  return next;
}
function verifyCreated({persistence,campaignId,characterId,mustBeActive}){
  const verified=loadFresh(persistence,campaignId);
  if(!verified.state)return Object.freeze({status:'PERSISTENCE_VERIFICATION_FAILED',reason:`reload:${verified.loaded.status}`});
  const roster=rosterOf(verified.state);
  const character=roster[characterId]??null;
  if(!character)return Object.freeze({status:'PERSISTENCE_VERIFICATION_FAILED',reason:'character-missing-after-commit',revision:verified.loaded.revision});
  if(!partyHasCharacter(verified.state,characterId))return Object.freeze({status:'PERSISTENCE_VERIFICATION_FAILED',reason:'party-membership-missing-after-commit',revision:verified.loaded.revision});
  if(mustBeActive&&verified.state.characterState?.characterId!==characterId)return Object.freeze({status:'PERSISTENCE_VERIFICATION_FAILED',reason:'active-character-mismatch-after-commit',revision:verified.loaded.revision});
  return Object.freeze({status:'OK',loaded:verified.loaded,state:verified.state,character});
}
function commitCreated({persistence,campaignId,loaded,state,character,eventKind,eventSuffix,setActive=false,sourceRefs=[]}){
  let currentLoaded=loaded;
  let currentState=state;
  const characterId=character.characterId;

  for(let attempt=0;attempt<2;attempt++){
    const existing=rosterOf(currentState)[characterId];
    if(existing){
      const alreadyValid=partyHasCharacter(currentState,characterId)&&(!setActive||currentState.characterState?.characterId===characterId);
      if(alreadyValid)return Object.freeze({status:'OK',receipt:null,character:Object.freeze(clone(existing)),projectedState:Object.freeze(clone(currentState)),verified:true,idempotent:true});
      return Object.freeze({status:'CHARACTER_ID_EXISTS_NO_MUTATION',revision:currentLoaded.revision});
    }

    const next=buildCreatedState(currentState,character,setActive);
    const mustBeActive=next.characterState?.characterId===characterId;
    const newRevision=currentLoaded.revision+1;
    next.revision=newRevision;
    const eventId=`mech:${campaignId}:r${newRevision}:${eventSuffix}`;
    const event={eventId,revision:newRevision,kind:eventKind,sourceRefs,characterId,setActive:Boolean(setActive),mechanicalMutation:true};
    const receipt=persistence.commit({campaignId,expectedRevision:currentLoaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});

    if(receipt.status==='REVISION_CONFLICT'&&attempt===0){
      const reloaded=loadFresh(persistence,campaignId);
      if(!reloaded.state)return Object.freeze({status:reloaded.loaded.status,receipt});
      currentLoaded=reloaded.loaded;
      currentState=reloaded.state;
      continue;
    }
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt,character:null,projectedState:null});

    const verified=verifyCreated({persistence,campaignId,characterId,mustBeActive});
    if(verified.status!=='OK')return Object.freeze({...verified,receipt,character:null,projectedState:null});
    return Object.freeze({status:'OK',receipt,character:Object.freeze(clone(verified.character)),projectedState:Object.freeze(clone(verified.state)),verified:true});
  }
  return Object.freeze({status:'REVISION_CONFLICT'});
}
function validateManualDraft(draft,revision){if(draft?.classContract?.ruleset==='WWN_SRD_2024'){const attributes=validateAttributeGeneration({attributes:draft.attributes,generation:draft.attributeGeneration});if(!attributes.ok)return Object.freeze({status:attributes.status,details:attributes,revision});}return null;}
function materializeManual({draft,masterSeed,revision}){const invalid=validateManualDraft(draft,revision);if(invalid)return invalid;const result=finalizeCharacterCreation({draft,masterSeed});if(!result.ok)return Object.freeze({status:result.status,details:result,revision});return Object.freeze({status:'OK',result});}

export class CharacterCreationService{
  constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
  finalize({campaignId,draft,masterSeed}){
    const {loaded,state}=loadFresh(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(state.characterState)return Object.freeze({status:'CHARACTER_ALREADY_FINALIZED_NO_MUTATION',revision:loaded.revision});
    const made=materializeManual({draft,masterSeed:masterSeed??state.rngState?.masterSeed??campaignId,revision:loaded.revision});if(made.status!=='OK')return made;
    return commitCreated({persistence:this.persistence,campaignId,loaded,state,character:made.result.character,eventKind:'CHARACTER_FINALIZED',eventSuffix:'character-finalized',setActive:true,sourceRefs:made.result.sourceRefs});
  }
  finalizeForRoster({campaignId,draft,masterSeed,setActive=true}={}){
    const {loaded,state}=loadFresh(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const id=String(draft?.characterId??'').trim();if(!id)return Object.freeze({status:'CHARACTER_ID_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const roster=rosterOf(state);if(roster[id])return Object.freeze({status:'CHARACTER_ID_EXISTS_NO_MUTATION',revision:loaded.revision});
    const made=materializeManual({draft,masterSeed:masterSeed??`${state.rngState?.masterSeed??campaignId}:${id}:manual`,revision:loaded.revision});if(made.status!=='OK')return made;
    return commitCreated({persistence:this.persistence,campaignId,loaded,state,character:made.result.character,eventKind:'MANUAL_CHARACTER_CREATED',eventSuffix:`manual-character:${id}`,setActive,sourceRefs:made.result.sourceRefs});
  }
  generateRandom({campaignId,characterId,name='Aventureiro',goal='Explorar o mundo',classPath=null,allowedClassIds=null,setActive=false,linguaFranca='Comum',nativeLanguage='Comum'}={}){
    const {loaded,state}=loadFresh(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const id=String(characterId??'').trim();if(!id)return Object.freeze({status:'CHARACTER_ID_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const roster=rosterOf(state);if(roster[id])return Object.freeze({status:'CHARACTER_ID_EXISTS_NO_MUTATION',revision:loaded.revision});
    const result=materializeRandomLevelOneCharacter({masterSeed:state.rngState?.masterSeed??campaignId,characterId:id,name,goal,classPath,allowedClassIds,linguaFranca,nativeLanguage});if(result.status!=='OK')return Object.freeze({status:result.status,details:result,revision:loaded.revision});
    return commitCreated({persistence:this.persistence,campaignId,loaded,state,character:result.character,eventKind:'RANDOM_CHARACTER_CREATED',eventSuffix:`random-character:${id}`,setActive,sourceRefs:result.sourceRefs});
  }
}
