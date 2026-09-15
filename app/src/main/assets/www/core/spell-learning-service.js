import { canonicalJson } from './canonical-json.js';
import { spellById, learnableSpells } from './wwn-spell-catalog.js';
import { advancementSpellGainCount, baseSpellTraditions, spellcastingProgression } from './wwn-spell-progression.js';

function load(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function roster(state){const r={...(state.charactersState??{})};if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=structuredClone(state.characterState);return r;}
function classModeAndId(c,sourceClassId){
 const cid=String(c.classId??'').toUpperCase();
 if(cid!=='ADVENTURER')return {classId:cid,mode:'FULL'};
 const parts=(c.partialClasses??[]).map(x=>String(x).toUpperCase());const chosen=String(sourceClassId??'').toUpperCase();
 if(!chosen||!parts.includes(chosen))return null;return {classId:chosen,mode:'PARTIAL'};
}
function knownSpells(c){return [...(c.magic?.startingSpells??[]),...(c.magic?.learnedSpells??[])];}
function traditionalEducation(c){const f=(c.foci??[]).find(x=>String(x?.focusId??x?.id??x).toUpperCase().replace(/[^A-Z0-9]+/g,'_')==='TRADITIONAL_EDUCATION');return f?.tradition??f?.selectedTradition??null;}
export class SpellLearningService{
 constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
 learnAdvancementSpell({campaignId,characterId=null,sourceClassId=null,spellId}){
  const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const r=roster(state),id=characterId??state.characterState?.characterId,c=r[id];if(!c)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
  const source=classModeAndId(c,sourceClassId);if(!source)return Object.freeze({status:'INVALID_SPELLCASTING_SOURCE_NO_MUTATION',revision:loaded.revision});
  const pending=Number(c.pendingAdvancement?.newSpells??0);if(pending<1)return Object.freeze({status:'NO_PENDING_SPELL_GAIN_NO_MUTATION',revision:loaded.revision});
  const progression=spellcastingProgression({classId:source.classId,level:c.level,mode:source.mode,intelligenceModifier:0});if(progression.spellcasting===false)return Object.freeze({status:'NON_SPELLCASTING_CLASS_NO_MUTATION',revision:loaded.revision});
  const allowed=baseSpellTraditions({classId:source.classId,traditionalEducationTradition:traditionalEducation(c)});
  const spell=spellById(spellId);if(!spell)return Object.freeze({status:'UNKNOWN_SPELL_NO_MUTATION',revision:loaded.revision});
  if(!allowed.includes(spell.tradition)||spell.level>progression.maxSpellLevel)return Object.freeze({status:'SPELL_NOT_LEARNABLE_NO_MUTATION',revision:loaded.revision,allowedTraditions:allowed,maxSpellLevel:progression.maxSpellLevel});
  const known=knownSpells(c);if(known.some(x=>String(x?.spellId??x?.name??x).toUpperCase().replace(/[^A-Z0-9]+/g,'_')===spell.spellId))return Object.freeze({status:'SPELL_ALREADY_KNOWN_NO_MUTATION',revision:loaded.revision});
  const next=structuredClone(state),nr=roster(next),nc=structuredClone(c);nc.magic={...(nc.magic??{}),learnedSpells:[...(nc.magic?.learnedSpells??[]),{spellId:spell.spellId,name:spell.name,level:spell.level,tradition:spell.tradition,sourceRef:spell.sourceRefs[0]}]};nc.pendingAdvancement={...(nc.pendingAdvancement??{}),newSpells:pending-1};nr[id]=nc;next.charactersState=nr;if(next.characterState?.characterId===id)next.characterState=structuredClone(nc);
  const revision=loaded.revision+1;next.revision=revision;const eventId=`mech:${campaignId}:r${revision}:learn-spell:${id}:${spell.spellId}`;const payload={eventId,revision,kind:'CHARACTER_LEARNED_SPELL',characterId:id,sourceClassId:source.classId,mode:source.mode,spellId:spell.spellId,spellName:spell.name,spellLevel:spell.level,tradition:spell.tradition,mechanicalMutation:true,sourceRefs:spell.sourceRefs};const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:payload.kind,eventPayloadJson:canonicalJson(payload),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});return Object.freeze({status:receipt.status,receipt,event:payload,character:receipt.status==='OK'?nc:null,projectedState:receipt.status==='OK'?next:null});
 }
 listChoices({character,sourceClassId=null}){
  const source=classModeAndId(character,sourceClassId);if(!source)return Object.freeze({status:'INVALID_SPELLCASTING_SOURCE',choices:Object.freeze([])});
  const p=spellcastingProgression({classId:source.classId,level:character.level,mode:source.mode,intelligenceModifier:0});if(p.spellcasting===false)return Object.freeze({status:'NON_SPELLCASTING_CLASS',choices:Object.freeze([])});
  const traditions=baseSpellTraditions({classId:source.classId,traditionalEducationTradition:traditionalEducation(character)});
  return Object.freeze({status:'OK',sourceClassId:source.classId,mode:source.mode,progression:p,traditions,choices:learnableSpells({traditions,maxLevel:p.maxSpellLevel,known:knownSpells(character)}),advancementGainCount:advancementSpellGainCount(source)});
 }
}
