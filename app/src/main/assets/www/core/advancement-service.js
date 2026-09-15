import { canonicalJson } from './canonical-json.js';
import { xpRequirement, advancedSavingThrows, resolveAdvancedMaxHp, advancementEntitlements } from './wwn-advancement-core.js';
import { deriveFullClassChassis, deriveAdventurerChassis } from './wwn-srd-class-combination.js';
import { advancementSpellGainCount, artPicksGainedAtLevel, fixedArtsGainedAtLevel } from './wwn-spell-progression.js';
import { classProfile } from './wwn-srd-class-catalog.js';

function load(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function roster(state){const r={...(state.charactersState??{})};if(state.characterState?.characterId&&!r[state.characterState.characterId])r[state.characterState.characterId]=structuredClone(state.characterState);return r;}
function attributeModifier(score){if(!Number.isInteger(score)||score<3||score>18)throw new RangeError('attribute score must be 3..18');return score===3?-2:score<=7?-1:score<=13?0:score<=17?1:2;}
function norm(v){return String(v??'').toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'');}
function hasFocus(c,id){const target=norm(id);return (c.foci??c.focuses??[]).some(f=>norm(f?.focusId??f?.id??f?.name??f)===target);}
function hasAbility(c,id){const target=norm(id);return (c.classAbilities??[]).some(a=>norm(a?.abilityId??a?.id??a?.name??a)===target);}
function classSources(c){const cid=norm(c.classId);if(cid==='ADVENTURER')return (c.partialClasses??[]).map(x=>({classId:norm(x),mode:'PARTIAL'}));return [{classId:cid,mode:'FULL'}];}
function magicGrants(c,newLevel,{spellcaster=null,highMage=false,fullInvoker=false,artPickDue=false}={}){
  const sources=classSources(c);let spells=0,arts=0;const fixedArts=[];
  for(const s of sources){
    const p=classProfile(s.classId);
    if(p?.initialTraining?.spellcasting===true)spells+=advancementSpellGainCount({classId:s.classId,mode:s.mode});
    arts+=artPicksGainedAtLevel({classId:s.classId,level:newLevel,mode:s.mode});
    fixedArts.push(...fixedArtsGainedAtLevel({classId:s.classId,level:newLevel}).map(name=>({name,classId:s.classId,sourceRef:p?.sourceRef??null})));
  }
  // Legacy explicit flags remain accepted only as a compatibility fallback for old fixtures/classes.
  if(spells===0&&spellcaster===true)spells=(highMage||fullInvoker)?2:1;
  if(arts===0&&artPickDue===true)arts=1;
  return Object.freeze({newSpells:spells,artPicks:arts,fixedArts:Object.freeze(fixedArts)});
}
function classBonusPerDie(expression,level){
  const m=String(expression??'').match(/^(\d+)d6(?:([+-])(\d+))?$/i);if(!m)return 0;
  const dice=Number(m[1]);if(dice!==level)return 0;const flat=m[2]?(m[2]==='-'?-1:1)*Number(m[3]):0;
  return Number.isInteger(flat/level)?flat/level:0;
}
function currentMaxHp(c){return Number(c.hp?.max??c.maxHp??c.hitPointsMax??c.hpMax??1);}
function constitutionModifier(c){const explicit=c.attributeModifiers?.Constitution??c.attributeModifiers?.Con;if(Number.isInteger(explicit))return explicit;const score=c.attributes?.Constitution;return Number.isInteger(score)?attributeModifier(score):0;}
function applyHpShape(c,newMax){if(c.hp&&typeof c.hp==='object'){c.hp={...c.hp,max:newMax,current:Math.min(Number(c.hp.current??newMax),newMax)};}else c.maxHp=newMax;}
function addFixedArts(c,grants){if(!grants.length)return;const known=new Set((c.arts??[]).map(a=>norm(a?.artId??a?.name??a)));const added=[];for(const g of grants){if(known.has(norm(g.name)))continue;c.arts=[...(c.arts??[]),{artId:g.name,name:g.name,classId:g.classId,sourceRef:g.sourceRef}];known.add(norm(g.name));added.push(g.name);}return added;}

export class AdvancementService{
  constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}
  advanceLevel({campaignId,characterId=null,pace='FAST',hitDieRolls,quickLearner=null,spellcaster=null,highMage=false,fullInvoker=false,artPickDue=false}){
    const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const r=roster(state);const id=characterId??state.characterState?.characterId;const c=r[id];if(!c)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const oldLevel=Number(c.level??1);const newLevel=oldLevel+1;if(newLevel>10)return Object.freeze({status:'MAX_LEVEL_REACHED_NO_MUTATION',revision:loaded.revision});const xp=Number(c.xp??c.experiencePoints??0);const required=xpRequirement({level:newLevel,pace});if(!Number.isInteger(xp)||xp<required.xp)return Object.freeze({status:'INSUFFICIENT_XP_NO_MUTATION',revision:loaded.revision,requiredXp:required.xp,currentXp:xp,newLevel});
    const classChassis=norm(c.classId)==='ADVENTURER'?deriveAdventurerChassis({partialClasses:c.partialClasses??[],level:newLevel}):deriveFullClassChassis({classId:c.classId,level:newLevel});
    if(classChassis.status!=='OK')return Object.freeze({status:'INVALID_CLASS_PATH_NO_MUTATION',revision:loaded.revision,classStatus:classChassis.status});
    const conMod=constitutionModifier(c),dieHard=hasFocus(c,'DIE_HARD'),perDie=classBonusPerDie(classChassis.hitDiceExpression,newLevel);
    const hp=resolveAdvancedMaxHp({newLevel,currentMaxHp:currentMaxHp(c),hitDieRolls,constitutionModifier:conMod,dieHard,classBonusPerDie:perDie});
    const saves=advancedSavingThrows(c.savingThrows??c.saves??{});const ql=quickLearner==null?hasAbility(c,'QUICK_LEARNER'):Boolean(quickLearner);
    const generic=advancementEntitlements({newLevel,quickLearner:ql,spellcaster:false,highMage:false,fullInvoker:false,artPickDue:false});
    const magic=magicGrants(c,newLevel,{spellcaster,highMage,fullInvoker,artPickDue});
    const entitlements=Object.freeze({...generic,magic:Object.freeze({rule:'WWN_MAGIC_ADVANCEMENT_V2_CLASS_DERIVED',spellcaster:magic.newSpells>0,newSpells:magic.newSpells,artPicks:magic.artPicks,fixedArts:magic.fixedArts,artScheduleRequiresClassTable:false,sourceRefs:generic.magic.sourceRefs}),attackBonusRequiresClassTable:false});
    const next=structuredClone(state);const nextRoster=roster(next);const nc=structuredClone(c);nc.level=newLevel;applyHpShape(nc,hp.newMaxHp);nc.baseAttackBonus=classChassis.baseAttackBonus;nc.savingThrows={...saves.scores};nc.unspentSkillPoints=Number(nc.unspentSkillPoints??0)+entitlements.skillPoints;
    const fixedArtsAdded=addFixedArts(nc,magic.fixedArts)??[];
    nc.pendingAdvancement={...(nc.pendingAdvancement??{}),level:newLevel,focusPick:entitlements.focus.eligible?1:0,focusBonusSkillCredit:0,newSpells:magic.newSpells,artPicks:magic.artPicks,attackBonusRequiresClassTable:false,classChassisSourceRefs:classChassis.sourceRefs};
    nextRoster[id]=nc;next.charactersState=nextRoster;if(next.characterState?.characterId===id)next.characterState=structuredClone(nc);
    const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:level-up:${id}:${newLevel}`;const event=Object.freeze({eventId,revision:newRevision,kind:'CHARACTER_LEVEL_ADVANCED',characterId:id,oldLevel,newLevel,xp,pace:required.pace,hp,classHitDiceExpression:classChassis.hitDiceExpression,classBonusPerDie:perDie,fixedArtsAdded:Object.freeze(fixedArtsAdded),entitlements,mechanicalMutation:true,sourceRefs:entitlements.sourceRefs});const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:event.kind,eventPayloadJson:canonicalJson(event),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});return Object.freeze({status:receipt.status,receipt,event,character:receipt.status==='OK'?nc:null,projectedState:receipt.status==='OK'?next:null});
  }
}
