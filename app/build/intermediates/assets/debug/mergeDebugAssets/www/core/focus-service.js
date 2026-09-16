import { canonicalJson } from './canonical-json.js';
import { focusById, validateFocusPick } from './wwn-focus-catalog.js';

function load(persistence,campaignId){
  const loaded=persistence.loadCampaign(campaignId);
  if(loaded.status!=='OK')return{loaded,state:null};
  const state=JSON.parse(loaded.stateJson);
  if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');
  return{loaded,state};
}
function roster(state){
  const r={...(state.charactersState??{})};
  if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=structuredClone(state.characterState);
  return r;
}
function normalizeFocusId(v){return String(v??'').toUpperCase().replace(/[^A-Z0-9]+/g,'_');}
function focusLevels(character){
  const levels=new Map();
  for(const raw of character.foci??character.focuses??[]){
    const id=normalizeFocusId(raw?.focusId??raw?.id??raw?.name??raw);
    if(!id)continue;
    const lv=Number.isInteger(raw?.level)?raw.level:1;
    levels.set(id,Math.max(levels.get(id)??0,lv));
  }
  return levels;
}
function materializeFoci(character,chosenId,newLevel){
  const out=[];let replaced=false;
  for(const raw of character.foci??character.focuses??[]){
    const id=normalizeFocusId(raw?.focusId??raw?.id??raw?.name??raw);
    if(id===chosenId){if(!replaced){const catalog=focusById(chosenId);out.push({focusId:chosenId,name:catalog?.name??raw?.name??chosenId,level:newLevel});replaced=true;}continue;}
    if(typeof raw==='string')out.push({focusId:id,name:focusById(id)?.name??raw,level:1});else out.push(structuredClone(raw));
  }
  if(!replaced){const catalog=focusById(chosenId);out.push({focusId:chosenId,name:catalog?.name??chosenId,level:newLevel});}
  return out;
}
const COMBAT_SKILLS=new Set(['Punch','Shoot','Stab']);
function validBonusSkillChoice(rule,skill){
  if(!rule)return skill==null;
  if(skill==null||String(skill).trim()==='')return false;
  const s=String(skill);
  if(Array.isArray(rule))return rule.includes(s);
  if(rule==='ANY_COMBAT')return COMBAT_SKILLS.has(s);
  if(rule==='ANY_EXCEPT_MAGIC_STAB_SHOOT_PUNCH')return !new Set(['Magic','Stab','Shoot','Punch']).has(s);
  if(rule==='ANY_ONE')return true;
  return true;
}
function applyThreePointFocusCredit(skills,skill){
  const next={...(skills??{})};
  const current=Number.isInteger(next[skill])?next[skill]:-1;
  let credit=3;let level=current;
  while(level<4){const target=level+1;const cost=target+1;if(cost>credit)break;credit-=cost;level=target;}
  if(level>=0)next[skill]=level;
  return {skills:next,skill,currentLevel:current,newLevel:level,remainingCredit:credit,redirectCredit:level===4?credit:0};
}

export class FocusService{
  constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
  chooseAdvancementFocus({campaignId,characterId=null,focusId,bonusSkill=null,gmApproved=false}={}){
    const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const r=roster(state);const id=characterId??state.characterState?.characterId;const c=r[id];if(!c)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const pending=c.pendingAdvancement??{};if(!Number.isInteger(pending.focusPick)||pending.focusPick<1)return Object.freeze({status:'NO_PENDING_FOCUS_PICK_NO_MUTATION',revision:loaded.revision});
    const focus=focusById(focusId);if(!focus)return Object.freeze({status:'UNKNOWN_FOCUS_NO_MUTATION',revision:loaded.revision});
    const levels=focusLevels(c);const currentLevel=levels.get(focus.id)??0;const validation=validateFocusPick({focusId:focus.id,currentLevel,character:c,gmApproved});if(!validation.ok)return Object.freeze({status:`${validation.status}_NO_MUTATION`,revision:loaded.revision,validation});
    const effect=validation.effect??{};const choiceRule=effect.bonusSkillChoice;
    const chosenSkill=effect.bonusSkill??bonusSkill??null;
    if(choiceRule&&!validBonusSkillChoice(choiceRule,bonusSkill))return Object.freeze({status:'BONUS_SKILL_CHOICE_REQUIRED_OR_INVALID_NO_MUTATION',revision:loaded.revision,focusId:focus.id,bonusSkillChoice:choiceRule});
    const actualBonusSkill=effect.bonusSkill??(choiceRule?String(bonusSkill):null);
    const next=structuredClone(state);const nextRoster=roster(next);const nc=structuredClone(c);nc.foci=materializeFoci(nc,focus.id,validation.newLevel);
    let skillCredit=null;
    if(validation.newLevel===1&&actualBonusSkill){skillCredit=applyThreePointFocusCredit(nc.skills??{},actualBonusSkill);nc.skills=skillCredit.skills;pending.focusBonusSkillCredit=skillCredit.remainingCredit;pending.focusBonusSkillRedirectCredit=skillCredit.redirectCredit;}
    nc.pendingAdvancement={...pending,focusPick:pending.focusPick-1,lastFocusPick:{focusId:focus.id,level:validation.newLevel,bonusSkill:actualBonusSkill,remainingSkillCredit:skillCredit?.remainingCredit??0}};
    nextRoster[id]=nc;next.charactersState=nextRoster;if(next.characterState?.characterId===id)next.characterState=structuredClone(nc);
    const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:focus:${id}:${focus.id}:${validation.newLevel}`;const event=Object.freeze({eventId,revision:newRevision,kind:'CHARACTER_FOCUS_ADVANCED',characterId:id,focusId:focus.id,focusLevel:validation.newLevel,bonusSkill:actualBonusSkill,skillCredit,gmApproved:Boolean(gmApproved),mechanicalMutation:true,sourceRefs:focus.sourceRefs});
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});
    return Object.freeze({status:receipt.status,receipt,event,character:receipt.status==='OK'?nc:null,projectedState:receipt.status==='OK'?next:null});
  }
}
