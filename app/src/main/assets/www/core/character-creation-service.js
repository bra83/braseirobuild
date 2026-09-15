import { canonicalJson } from './canonical-json.js';
import { finalizeCharacterCreation } from './wwn-character-creation-core.js';
import { materializeRandomLevelOneCharacter } from './wwn-srd-random-character.js';
import { validateAttributeGeneration } from './wwn-attribute-generation-core.js';
import { createPartyState } from './party-state.js';

function clone(v){return structuredClone(v)}
function rosterOf(state){const r={...(state.charactersState??{})};if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=clone(state.characterState);return r;}
function addToParty(next,characterId){
  if(!next.partyState){next.partyState=createPartyState({memberIds:Object.keys(next.charactersState),position:next.worldState?.position??null});return;}
  const groups=next.partyState.groups??{};if(Object.values(groups).some(g=>(g.memberIds??[]).includes(characterId)))return;const target=next.partyState.activeGroupId;if(!groups[target])throw new Error('active party group missing');groups[target].memberIds.push(characterId);
}
function commitCreated({persistence,campaignId,loaded,state,character,eventKind,eventSuffix,setActive=false,sourceRefs=[]}){
  const next=clone(state);next.charactersState=rosterOf(next);next.charactersState[character.characterId]=clone(character);if(setActive||!next.characterState)next.characterState=clone(character);addToParty(next,character.characterId);
  const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:${eventSuffix}`;const event={eventId,revision:newRevision,kind:eventKind,sourceRefs,characterId:character.characterId,setActive:Boolean(setActive),mechanicalMutation:true};
  const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
  return Object.freeze({status:receipt.status,receipt,character,projectedState:receipt.status==='OK'?next:null});
}

export class CharacterCreationService{
  constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
  finalize({campaignId,draft,masterSeed}){
    const loaded=this.persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return Object.freeze({status:loaded.status});
    const state=JSON.parse(loaded.stateJson);if(state.characterState)return Object.freeze({status:'CHARACTER_ALREADY_FINALIZED_NO_MUTATION',revision:loaded.revision});
    if(draft?.classContract?.ruleset==='WWN_SRD_2024'){
      const attributes=validateAttributeGeneration({attributes:draft.attributes,generation:draft.attributeGeneration});
      if(!attributes.ok)return Object.freeze({status:attributes.status,details:attributes,revision:loaded.revision});
    }
    const result=finalizeCharacterCreation({draft,masterSeed:masterSeed??state.rngState?.masterSeed??campaignId});if(!result.ok)return Object.freeze({status:result.status,details:result,revision:loaded.revision});
    return commitCreated({persistence:this.persistence,campaignId,loaded,state,character:result.character,eventKind:'CHARACTER_FINALIZED',eventSuffix:'character-finalized',setActive:true,sourceRefs:result.sourceRefs});
  }
  generateRandom({campaignId,characterId,name='Aventureiro',goal='Explorar o mundo',classPath=null,allowedClassIds=null,setActive=false,linguaFranca='Comum',nativeLanguage='Comum'}={}){
    const loaded=this.persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return Object.freeze({status:loaded.status});const state=JSON.parse(loaded.stateJson);const id=String(characterId??'').trim();if(!id)return Object.freeze({status:'CHARACTER_ID_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const roster=rosterOf(state);if(roster[id])return Object.freeze({status:'CHARACTER_ID_EXISTS_NO_MUTATION',revision:loaded.revision});
    const result=materializeRandomLevelOneCharacter({masterSeed:state.rngState?.masterSeed??campaignId,characterId:id,name,goal,classPath,allowedClassIds,linguaFranca,nativeLanguage});if(result.status!=='OK')return Object.freeze({status:result.status,details:result,revision:loaded.revision});
    return commitCreated({persistence:this.persistence,campaignId,loaded,state,character:result.character,eventKind:'RANDOM_CHARACTER_CREATED',eventSuffix:`random-character:${id}`,setActive,sourceRefs:result.sourceRefs});
  }
}
