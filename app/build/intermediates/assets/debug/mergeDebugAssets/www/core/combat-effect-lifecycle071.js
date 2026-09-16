import { attributeModifier } from './wwn-rules-core.js';

export const WWN_COMBAT_EFFECT_071_SOURCES=Object.freeze({
  everlasting:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.6.0 Necromancer Spells — Everlasting'}),
  pavis:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.7.1 Elementalist Arts — Pavis of Elements'}),
  elementalResilience:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.7.1 Elementalist Arts — Elemental Resilience'}),
  thermalShield:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.7.1 Elementalist Arts — Thermal Shield'}),
  coldFlesh:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.14.1 Necromancer Arts — Cold Flesh'}),
  vowedDefense:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§1.5.17.1 Vowed Arts — Unarmored Defense'}),
  echo:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Resounding Temporal Echo'}),
  velocitous:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Velocitous Imbuement'}),
  wardpact:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Wardpact Invocation'}),
  guardian:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.5.0 Elementalist Spells — Elemental Guardian'}),
  clement:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Visitation of the Clement Clime'}),
  scorn:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Scorn the Fetters of Earth'}),
  aeolian:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — Sigil of Aeolian Auctoritas'}),
  tumefaction:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.4.0 High Magic Spells — The Torment of Tumefaction'}),
  likeStones:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.5.0 Elementalist Spells — Like the Stones'}),
  calcifying:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.5.0 Elementalist Spells — Calcifying Scourge'}),
  augmentVitality:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.6.0 Necromancer Spells — Augment Mortal Vitality'}),
  enfeebling:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.6.0 Necromancer Spells — Enfeebling Wave'}),
  callTomb:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§4.6.0 Necromancer Spells — Call of the Tomb'}),
});

function clone(v){return structuredClone(v)}
function norm(v){return String(v??'').trim().toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'')}
function artEntry(character,name){const target=norm(name);return (character?.arts??[]).find(a=>norm(a?.artId??a?.name??a)===target)??null;}
export function hasArt071(character,name){return Boolean(artEntry(character,name));}
function readied(character){return Array.isArray(character?.inventory)?character.inventory.filter(i=>String(i?.carryState??'').toUpperCase()==='READIED'):[];}
function hasReadiedArmor(character){return readied(character).some(i=>i?.kind==='ARMOR');}
function hasReadiedShield(character){return readied(character).some(i=>i?.kind==='SHIELD');}

export function effectiveArmorClass071({character,armorProjection,totalDefense=false,state=null,targetId=null,round=null,rangedAttack=false}={}){
  if(!character||!armorProjection||armorProjection.status!=='OK'||!Number.isInteger(armorProjection.armorClass))return Object.freeze({status:'MISSING_ARMOR_EVIDENCE'});
  let armorClass=armorProjection.armorClass;const applied=[];
  if(hasArt071(character,'Cold Flesh')&&Number.isInteger(character.level)){
    const natural=12+Math.floor(character.level/2);
    if(natural>armorClass){armorClass=natural;applied.push({effect:'COLD_FLESH_NATURAL_AC',armorClass:natural,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.coldFlesh});}
  }
  if(hasArt071(character,'Unarmored Defense')&&Number.isInteger(character.level)&&!hasReadiedArmor(character)&&!hasReadiedShield(character)){
    const dex=armorProjection.dexterityModifier??attributeModifier(character.attributes?.Dexterity);
    const candidate=13+Math.floor(character.level/2)+dex;
    if(candidate>armorClass){armorClass=candidate;applied.push({effect:'VOWED_UNARMORED_DEFENSE',armorClass:candidate,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.vowedDefense});}
  }
  if(rangedAttack===true&&state&&targetId){const ls=activeLikeStones071(state,targetId);if(ls?.element==='AIR'){armorClass+=4;applied.push({effect:'LIKE_THE_STONES_AIR_RANGED_AC',bonus:4,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.likeStones});}}
  if(totalDefense===true){armorClass+=2;applied.push({effect:'TOTAL_DEFENSE',bonus:2});}
  if(character.artState?.pavisOfElements?.active===true){
    const before=armorClass;armorClass=Math.min(18,armorClass+4);applied.push({effect:'PAVIS_OF_ELEMENTS',bonusApplied:armorClass-before,cap:18,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.pavis});
  }
  return Object.freeze({status:'OK',armorClass,baseDerivedArmorClass:armorProjection.armorClass,applied:Object.freeze(applied)});
}

function coldFleshShockCap(character,resolution){
  if(!hasArt071(character,'Cold Flesh')||!resolution?.shockEligible)return {resolution,applied:null};
  const before=Number(resolution.damage??0),pre=Math.max(0,Number(resolution.preShockDamage??0)),shock=Math.max(0,Number(resolution.shockDamage??0));
  const cappedShock=Math.min(2,shock);const after=resolution.hit===true?Math.max(pre,cappedShock):cappedShock;
  if(after>=before)return {resolution,applied:null};
  return {resolution:Object.freeze({...resolution,damageBeforeColdFlesh:before,damage:after,coldFleshShockCapped:true}),applied:{effect:'COLD_FLESH_SHOCK_CAP',before,after,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.coldFlesh}};
}

function explicitElementalResilience(character,resolution,damageContext){
  if(!hasArt071(character,'Elemental Resilience'))return{resolution,applied:null};
  const element=String(damageContext?.element??'').toUpperCase();if(!['FIRE','FROST'].includes(element))return{resolution,applied:null};
  const mode=String(damageContext?.elementalResilienceClass??'').toUpperCase();const before=Math.max(0,Number(resolution?.damage??0));let after=before;
  if(mode==='MUNDANE_COLD_OR_SUBFURNACE_HEAT')after=0;
  else if(mode==='MAGICAL_OR_EXTREME')after=Math.floor(before/2);
  else return{resolution,applied:null};
  if(after===before)return{resolution,applied:null};
  return{resolution:Object.freeze({...resolution,damageBeforeElementalResilience:before,damage:after,elementalResilienceApplied:true}),applied:{effect:'ELEMENTAL_RESILIENCE',before,after,element,mode,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.elementalResilience}};
}


function sceneTargetEffect071(state,spellId,targetId){return (state?.encounterState?.magicEffects??[]).find(e=>e?.spellId===spellId&&e?.expired!==true&&((Array.isArray(e?.targetIds)&&e.targetIds.includes(String(targetId)))||e?.targetId===String(targetId)))??null;}
export function activeLikeStones071(state,targetId){return sceneTargetEffect071(state,'LIKE_THE_STONES',targetId);}
export function activeClementClime071(state,targetId){return sceneTargetEffect071(state,'VISITATION_OF_THE_CLEMENT_CLIME',targetId);}
export function activeAugmentMortalVitality071(state,targetId){return sceneTargetEffect071(state,'AUGMENT_MORTAL_VITALITY',targetId);}
export function activeEnfeeblingWave071(state,targetId,round=null){const effects=state?.encounterState?.magicEffects??[];return effects.find(e=>e?.spellId==='ENFEEBLING_WAVE'&&e?.expired!==true&&e?.targetId===String(targetId)&&(e.duration==='SCENE'||e.remainingTargetTurns>0)&&(round===null||!Number.isInteger(e.startsRound)||round>=e.startsRound))??null;}
export function activeCallOfTheTomb071(state,targetId,round){return (state?.encounterState?.magicEffects??[]).find(e=>e?.spellId==='CALL_OF_THE_TOMB'&&e?.expired!==true&&e?.targetId===String(targetId)&&Number.isInteger(e?.startsRound)&&Number.isInteger(e?.endsAfterRound)&&round>=e.startsRound&&round<=e.endsAfterRound)??null;}
export function mainActionSuppressedByMagic071(state,targetId,round){return (state?.encounterState?.magicEffects??[]).some(e=>e?.expired!==true&&e?.targetId===String(targetId)&&e?.spellId==='SIGIL_OF_AEOLIAN_AUCTORITAS'&&Number(e?.loseMainActionRound)===Number(round));}
export function moveActionSuppressedByMagic071(state,targetId,round){return (state?.encounterState?.magicEffects??[]).some(e=>e?.expired!==true&&e?.targetId===String(targetId)&&e?.moveActionSuppressed===true&&Number.isInteger(e?.startsRound)&&Number.isInteger(e?.endsAfterRound)&&round>=e.startsRound&&round<=e.endsAfterRound);}
export function physicalSaveBonus071(state,targetId){const e=activeAugmentMortalVitality071(state,targetId);return e?Number(e.physicalSaveBonus??0):0;}
export function autoStabilizeMagic071(state,targetId){const e=activeAugmentMortalVitality071(state,targetId),ls=activeLikeStones071(state,targetId);if(e?.autoStabilizeWhenMortallyWounded===true)return Object.freeze({autoStabilized:true,source:'AUGMENT_MORTAL_VITALITY',sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.augmentVitality});if(ls?.element==='STONE'&&ls?.stone?.autoStabilizeAtZero===true)return Object.freeze({autoStabilized:true,source:'LIKE_THE_STONES_STONE',sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.likeStones});return Object.freeze({autoStabilized:false});}
export function effectiveMoveFeet071({state,targetId,round,baseMoveFeet}={}){const e=activeEnfeeblingWave071(state,targetId,round);const multiplier=e?Number(e.moveMultiplier??1):1;return Object.freeze({status:'OK',baseMoveFeet:Number(baseMoveFeet),moveFeet:Number(baseMoveFeet)*multiplier,multiplier,effect:e??null,sourceRefs:e?Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.enfeebling]):Object.freeze([])});}
export function enfeeblingRollPenalty071({state,targetId,round}={}){const e=activeEnfeeblingWave071(state,targetId,round);return Object.freeze({active:Boolean(e),attackRollsTwiceTakeWorse:Boolean(e?.attackRollsTwiceTakeWorse),damageRollsTwiceTakeWorse:Boolean(e?.damageRollsTwiceTakeWorse),effect:e??null});}
function clementMitigation071(state,targetId,resolution,damageContext){const e=activeClementClime071(state,targetId);if(!e)return{resolution,applied:null};const element=String(damageContext?.element??'').toUpperCase(),cls=String(damageContext?.clementClass??'').toUpperCase(),before=Math.max(0,Number(resolution?.damage??0));if(!['FIRE','FROST','HEAT','COLD','ACID','ELECTRICITY','ELECTRIC'].includes(element))return{resolution,applied:null};let after=before;if(cls==='MUNDANE'||cls==='NATURAL')after=0;else if(cls==='MAGICAL'){if(damageContext?.physicalSaveSucceeded===true)after=0;else after=Math.floor(before*Number(e.magicalElementalDamageMultiplier??0.5));}else return{resolution,applied:null};return{resolution:Object.freeze({...resolution,damageBeforeClementClime:before,damage:after,clementClimeApplied:true}),applied:{effect:'CLEMENT_CLIME',before,after,element,classification:cls,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.clement}};}
function likeStonesMitigation071(state,targetId,resolution,damageContext){const e=activeLikeStones071(state,targetId);if(!e)return{resolution,applied:null};const before=Math.max(0,Number(resolution?.damage??0));let after=before,mode=null;if(e.element==='STONE') {after=Math.max(0,before-3);mode='STONE_IGNORE_3';} else if(e.element==='FIRE'&&['HEAT','FIRE'].includes(String(damageContext?.element??'').toUpperCase())) {after=0;mode='FIRE_HEAT_IMMUNITY';}if(after===before)return{resolution,applied:null};return{resolution:Object.freeze({...resolution,damageBeforeLikeStones:before,damage:after,likeStonesApplied:true}),applied:{effect:'LIKE_THE_STONES',mode,before,after,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.likeStones}};}
export function applyCallOfTheTombToAttack071({state,targetId,round,resolution,weapon}={}){const e=activeCallOfTheTomb071(state,targetId,Number(round));if(!e||!resolution)return Object.freeze({status:'NO_CALL_OF_TOMB_EFFECT',resolution});const m=/^\s*(\d+)d(\d+)(?:\s*([+-])\s*(\d+))?\s*$/i.exec(String(weapon?.damage??''));if(!m)return Object.freeze({status:'DAMAGE_FORMULA_EVIDENCE_REQUIRED',resolution,effect:e});const count=Number(m[1]),sides=Number(m[2]),exprBonus=m[3]?(m[3]==='-'?-1:1)*Number(m[4]):0;let maxDamage=count*sides+exprBonus+Number(resolution.damageBonus??0);if(Number.isFinite(resolution.attributeModifier))maxDamage+=Number(resolution.attributeModifier);if(weapon?.punchSkillAddsToDamage&&weapon?.combatSkill==='Punch'&&Number.isInteger(resolution.skillLevel))maxDamage+=resolution.skillLevel;maxDamage=Math.max(0,maxDamage);if(resolution.shockEligible)maxDamage=Math.max(maxDamage,Number(resolution.shockDamage??0));const out=Object.freeze({...resolution,hitBeforeCallOfTomb:resolution.hit,hit:true,damageBeforeCallOfTomb:Number(resolution.damage??0),damage:maxDamage,preShockDamage:maxDamage,damageKind:'HIT',callOfTombAutoHit:true,callOfTombMaximizedDamage:true,callOfTombNegatesSpecialDefenses:true,callOfTombEffectId:e.effectId});return Object.freeze({status:'APPLIED',resolution:out,effect:e,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.callTomb])});}
export function applyTumefactionVigorousActionInPlace071({state,targetId,round}={}){const effects=state?.encounterState?.magicEffects??[];const i=effects.findIndex(e=>e?.spellId==='THE_TORMENT_OF_TUMEFACTION'&&e?.expired!==true&&e?.targetId===String(targetId));if(i<0)return Object.freeze({status:'NO_TUMEFACTION_EFFECT'});const e=effects[i],damageRounds=Array.isArray(e.damageRounds)?[...e.damageRounds]:[];if(damageRounds.includes(Number(round)))return Object.freeze({status:'ALREADY_DAMAGED_THIS_ROUND',damage:0,effect:e});const c=character071(state,targetId);const npc=state?.npcState?.[targetId];const target=c??npc;if(!target)return Object.freeze({status:'TARGET_NOT_FOUND'});const damage=Math.max(0,Number(e.damagePerVigorousAction??0));if(c){const n=clone(c);n.hp={...n.hp,current:Math.max(0,Number(n.hp?.current??0)-damage)};setCharacter(state,targetId,n);}else{const n=clone(npc);if(Number.isInteger(n.hp))n.hp=Math.max(0,n.hp-damage);else if(n.hp&&Number.isInteger(n.hp.current))n.hp={...n.hp,current:Math.max(0,n.hp.current-damage)};state.npcState={...(state.npcState??{}),[targetId]:n};}effects[i]={...e,damageRounds:[...damageRounds,Number(round)]};return Object.freeze({status:'APPLIED',damage,effect:effects[i],sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.tumefaction])});}
export function consumeTargetTurnEffectsInPlace071({state,targetIds=[]}={}){const effects=state?.encounterState?.magicEffects??[];const touched=[];for(let i=0;i<effects.length;i++){const e=effects[i];if(e?.expired===true||e?.spellId!=='ENFEEBLING_WAVE'||e?.duration!=='NEXT_TURN'||!targetIds.map(String).includes(String(e.targetId)))continue;const remaining=Math.max(0,Number(e.remainingTargetTurns??1)-1);effects[i]={...e,remainingTargetTurns:remaining,expired:remaining<=0,expiredAfterTargetTurn:true};touched.push({effectId:e.effectId,targetId:e.targetId,expired:remaining<=0});}return Object.freeze(touched);}

export function activeEverlastingEffect071(state,targetId,round){
  const effects=state?.encounterState?.magicEffects??[];return effects.find(e=>e?.spellId==='EVERLASTING'&&e?.expired!==true&&Array.isArray(e.allyIds)&&e.allyIds.includes(targetId)&&Number.isInteger(e.startsRound)&&Number.isInteger(e.endsAfterRound)&&round>=e.startsRound&&round<=e.endsAfterRound)??null;
}

export function applyPcDamageMitigations071({state,combat,targetId,resolution,damageContext=null}={}){
  const character=state?.charactersState?.[targetId]??(state?.characterState?.characterId===targetId?state.characterState:null);if(!character||!resolution)return Object.freeze({status:'MISSING_TARGET_OR_RESOLUTION',resolution});
  let current=resolution;const applied=[];
  const cold=coldFleshShockCap(character,current);current=cold.resolution;if(cold.applied)applied.push(cold.applied);
  const elemental=explicitElementalResilience(character,current,damageContext);current=elemental.resolution;if(elemental.applied)applied.push(elemental.applied);
  const clement=clementMitigation071(state,targetId,current,damageContext);current=clement.resolution;if(clement.applied)applied.push(clement.applied);
  const stones=likeStonesMitigation071(state,targetId,current,damageContext);current=stones.resolution;if(stones.applied)applied.push(stones.applied);
  const everlasting=activeEverlastingEffect071(state,targetId,Number(combat?.round));
  if(everlasting&&Number.isInteger(character.hp?.current)&&character.hp.current>0&&Number(current.damage??0)>0){
    const maxDamage=Math.max(0,character.hp.current-Number(everlasting.minimumHp??1));
    if(current.damage>maxDamage){const before=current.damage;current=Object.freeze({...current,damageBeforeEverlasting:before,damage:maxDamage,everlastingFloorApplied:true,everlastingEffectId:everlasting.effectId});applied.push({effect:'EVERLASTING_HP_FLOOR',before,after:maxDamage,effectId:everlasting.effectId,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.everlasting});}
  }
  return Object.freeze({status:'OK',resolution:current,applied:Object.freeze(applied)});
}

function effortPool(character,classId){return character?.effort?.[classId]??null;}
function artClass(character,name){return String(artEntry(character,name)?.classId??'').toUpperCase()||'ELEMENTALIST';}
function availableEffort(character,classId){const p=effortPool(character,classId);if(!p||!Number.isInteger(p.max))return false;const c=p.committed??{};return Number(c.day??0)+Number(c.scene??0)+Number(c.indefinite??0)<p.max;}
function setCharacter(state,id,character){state.charactersState={...(state.charactersState??{}),[id]:clone(character)};if(state.characterState?.characterId===id)state.characterState=clone(character);}

export function inspectThermalShieldReaction071({state,targetId,resolution,damageContext,visibleCasterIds=[]}={}){
  const element=String(damageContext?.element??'').toUpperCase();if(!['FIRE','FROST'].includes(element)||Number(resolution?.damage??0)<=0)return Object.freeze({status:'NOT_APPLICABLE',candidates:Object.freeze([])});
  const candidates=[];for(const id of visibleCasterIds??[]){const c=state?.charactersState?.[id]??(state?.characterState?.characterId===id?state.characterState:null);if(!c||Number(c.hp?.current??0)<=0||!hasArt071(c,'Thermal Shield'))continue;const classId=artClass(c,'Thermal Shield');if(availableEffort(c,classId))candidates.push({casterId:id,classId});}
  return Object.freeze({status:candidates.length?'DECISION_REQUIRED':'NO_ELIGIBLE_CASTER',targetId,element,candidates:Object.freeze(candidates),sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.thermalShield])});
}

export function applyThermalShieldReactionInPlace071({state,targetId,resolution,damageContext,visibleCasterIds=[],decision=null}={}){
  const inspection=inspectThermalShieldReaction071({state,targetId,resolution,damageContext,visibleCasterIds});if(inspection.status!=='DECISION_REQUIRED')return Object.freeze({status:inspection.status,resolution,inspection});
  const action=String(decision?.action??'').toUpperCase();if(!action)return Object.freeze({status:'THERMAL_SHIELD_DECISION_REQUIRED',resolution,inspection});
  if(action==='DECLINE')return Object.freeze({status:'DECLINED',resolution,inspection});
  if(action!=='USE')return Object.freeze({status:'INVALID_THERMAL_SHIELD_DECISION',resolution,inspection});
  const casterId=String(decision?.casterId??'');const candidate=inspection.candidates.find(c=>c.casterId===casterId);if(!candidate)return Object.freeze({status:'THERMAL_SHIELD_CASTER_NOT_ELIGIBLE',resolution,inspection});
  const original=state.charactersState?.[casterId]??(state.characterState?.characterId===casterId?state.characterState:null);const caster=clone(original),pool=clone(effortPool(caster,candidate.classId));const committed={day:Number(pool.committed?.day??0),scene:Number(pool.committed?.scene??0),indefinite:Number(pool.committed?.indefinite??0)};committed.scene+=1;caster.effort={...(caster.effort??{}),[candidate.classId]:{...pool,committed,current:pool.max-(committed.day+committed.scene+committed.indefinite)}};setCharacter(state,casterId,caster);
  return Object.freeze({status:'USED',resolution:Object.freeze({...resolution,damageBeforeThermalShield:resolution.damage,damage:0,thermalShieldNegated:true,thermalShieldCasterId:casterId}),inspection,casterId,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.thermalShield])});
}

export function expireRoundEffectsInPlace071({state,completedRound}={}){
  if(!state?.encounterState||!Number.isInteger(completedRound))return Object.freeze([]);const effects=state.encounterState.magicEffects;if(!Array.isArray(effects))return Object.freeze([]);const expired=[];
  for(let i=0;i<effects.length;i++){const e=effects[i];if(e?.expired===true||!Number.isInteger(e?.endsAfterRound)||e.endsAfterRound>completedRound)continue;const nextEffect={...e,expired:true,expiredAfterRound:completedRound};effects[i]=nextEffect;if(e.spellId==='EVERLASTING'){const casterId=e.casterId;const caster=state.charactersState?.[casterId]??(state.characterState?.characterId===casterId?state.characterState:null);let casterHpAfter=null;if(caster&&Number.isInteger(caster.hp?.current)&&caster.hp.current>0){const nc=clone(caster);nc.hp={...nc.hp,current:1};setCharacter(state,casterId,nc);casterHpAfter=1;}expired.push({effectId:e.effectId,spellId:'EVERLASTING',casterId,casterHpAfter,sourceRef:WWN_COMBAT_EFFECT_071_SOURCES.everlasting});}else if(['RESOUNDING_TEMPORAL_ECHO','WARDPACT_INVOCATION'].includes(e.spellId))expired.push({effectId:e.effectId,spellId:e.spellId,sourceRef:e.spellId==='RESOUNDING_TEMPORAL_ECHO'?WWN_COMBAT_EFFECT_071_SOURCES.echo:WWN_COMBAT_EFFECT_071_SOURCES.wardpact});}
  return Object.freeze(expired);
}


function magicEffects071(state){return Array.isArray(state?.encounterState?.magicEffects)?state.encounterState.magicEffects:[];}
function character071(state,id){return state?.charactersState?.[id]??(state?.characterState?.characterId===id?state.characterState:null);}
function strain071(character){return character?.systemStrain&&Number.isInteger(character.systemStrain.current)&&Number.isInteger(character.systemStrain.max)?character.systemStrain:null;}
function activeRoundTargetEffect071(state,spellId,targetId,round){return magicEffects071(state).find(e=>e?.spellId===spellId&&e?.expired!==true&&Array.isArray(e.targetIds)&&e.targetIds.includes(targetId)&&Number.isInteger(e.startsRound)&&Number.isInteger(e.endsAfterRound)&&round>=e.startsRound&&round<=e.endsAfterRound)??null;}
export function activeResoundingTemporalEcho071(state,targetId,round){return activeRoundTargetEffect071(state,'RESOUNDING_TEMPORAL_ECHO',targetId,round);}
export function activeVelocitousImbuement071(state,targetId){return magicEffects071(state).find(e=>e?.spellId==='VELOCITOUS_IMBUEMENT'&&e?.expired!==true&&Array.isArray(e.targetIds)&&e.targetIds.includes(targetId))??null;}
export function activeWardpactCreature071(state,targetId,round){return magicEffects071(state).find(e=>e?.spellId==='WARDPACT_INVOCATION'&&e?.mode==='CREATURE'&&e?.expired!==true&&e?.targetId===targetId&&Number.isInteger(e.startsRound)&&Number.isInteger(e.endsAfterRound)&&round>=e.startsRound&&round<=e.endsAfterRound)??null;}
export function activeWardpactWeapon071(state,ownerId,weaponId){return magicEffects071(state).find(e=>e?.spellId==='WARDPACT_INVOCATION'&&e?.mode==='WEAPON'&&e?.expired!==true&&e?.weaponOwnerId===String(ownerId)&&e?.weaponId===String(weaponId))??null;}
function usageKey(round,targetId){return `${round}:${targetId}`;}
function magicUsage071(combat){combat.magicUsage071={...(combat.magicUsage071??{}),echoBonusMainUsed:{...(combat.magicUsage071?.echoBonusMainUsed??{})}};return combat.magicUsage071;}
export function inspectEchoBonusMain071({state,combat,targetId}={}){const round=Number(combat?.round);const effect=activeResoundingTemporalEcho071(state,targetId,round);if(!effect)return Object.freeze({status:'NO_ECHO_EFFECT'});const usage=combat?.magicUsage071?.echoBonusMainUsed?.[usageKey(round,targetId)]===true;if(usage)return Object.freeze({status:'ECHO_BONUS_MAIN_ALREADY_USED',effect});const c=character071(state,targetId),ss=strain071(c);if(!ss)return Object.freeze({status:'SYSTEM_STRAIN_EVIDENCE_REQUIRED',effect});if(ss.current>=ss.max)return Object.freeze({status:'SYSTEM_STRAIN_MAXIMUM',effect,strain:ss});return Object.freeze({status:'AVAILABLE',effect,strain:ss});}
export function actionEconomyWithEcho071({state,combat,targetId,economy}={}){const base={...(economy??{})};const mainSuppressed=mainActionSuppressedByMagic071(state,targetId,Number(combat?.round));const moveSuppressed=moveActionSuppressedByMagic071(state,targetId,Number(combat?.round));if(mainSuppressed)base.mainAvailable=false;if(moveSuppressed)base.moveAvailable=false;const i=inspectEchoBonusMain071({state,combat,targetId});const echoVirtual=base.mainAvailable!==true&&i.status==='AVAILABLE';if(echoVirtual)base.mainAvailable=true;return Object.freeze({...base,echoBonusMainVirtual:echoVirtual,mainSuppressedByMagic:mainSuppressed,moveSuppressedByMagic:moveSuppressed});}
export function applyActionCostWithEchoInPlace071({state,combat,targetId,cost}={}){const original={...(combat?.economy?.[targetId]??{})};const round=Number(combat?.round),mainSuppressed=mainActionSuppressedByMagic071(state,targetId,round),moveSuppressed=moveActionSuppressedByMagic071(state,targetId,round);const needsMain=['MAIN','MAIN_AS_SECOND_MOVE','MOVE_AND_MAIN'].includes(cost),needsMove=['MOVE','MOVE_AND_MAIN'].includes(cost);let usedEcho=false;const next={...original};if(needsMove){if(moveSuppressed||next.moveAvailable!==true)return Object.freeze({status:'MOVE_ACTION_UNAVAILABLE'});next.moveAvailable=false;}if(needsMain){if(!mainSuppressed&&next.mainAvailable===true)next.mainAvailable=false;else{const i=inspectEchoBonusMain071({state,combat,targetId});if(i.status!=='AVAILABLE')return Object.freeze({status:mainSuppressed?'MAIN_ACTION_SUPPRESSED':i.status,inspection:i});const c=clone(character071(state,targetId)),ss=strain071(c);c.systemStrain={...c.systemStrain,current:ss.current+1};setCharacter(state,targetId,c);const usage=magicUsage071(combat);usage.echoBonusMainUsed[usageKey(round,targetId)]=true;usedEcho=true;}}combat.economy[targetId]=Object.freeze(next);return Object.freeze({status:'OK',economy:combat.economy[targetId],usedEcho,systemStrainAdded:usedEcho?1:0,mainSuppressedByMagic:mainSuppressed,moveSuppressedByMagic:moveSuppressed,sourceRefs:usedEcho?Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.echo]):Object.freeze([])});}
export function inspectVelocitousMovement071({state,targetId,normalMoveFeet,distanceFeet,leavingMelee=false,specialTraversal=false,advantageNeededOverride=null}={}){const effect=activeVelocitousImbuement071(state,targetId);if(!effect)return Object.freeze({status:'NO_VELOCITOUS_EFFECT',normalMoveFeet,maxMoveFeet:normalMoveFeet,advantageNeeded:false});const c=character071(state,targetId),ss=strain071(c);if(!ss)return Object.freeze({status:'SYSTEM_STRAIN_EVIDENCE_REQUIRED',effect});const maxMoveFeet=Number(normalMoveFeet)*Number(effect.groundMoveMultiplier??2);const inferred=Number(distanceFeet)>Number(normalMoveFeet)+1e-9||leavingMelee===true||specialTraversal===true;const advantageNeeded=typeof advantageNeededOverride==='boolean'?advantageNeededOverride:inferred;if(advantageNeeded&&ss.current>=ss.max)return Object.freeze({status:'SYSTEM_STRAIN_MAXIMUM',effect,normalMoveFeet,maxMoveFeet,advantageNeeded,strain:ss});return Object.freeze({status:'AVAILABLE',effect,normalMoveFeet,maxMoveFeet,advantageNeeded,strain:ss});}
export function applyVelocitousUseInPlace071({state,targetId,inspection}={}){if(inspection?.status!=='AVAILABLE'||inspection.advantageNeeded!==true)return Object.freeze({status:'NO_STRAIN_REQUIRED'});const c=clone(character071(state,targetId)),ss=strain071(c);if(!ss||ss.current>=ss.max)return Object.freeze({status:'SYSTEM_STRAIN_MAXIMUM'});c.systemStrain={...c.systemStrain,current:ss.current+1};setCharacter(state,targetId,c);return Object.freeze({status:'USED',systemStrainBefore:ss.current,systemStrainAfter:ss.current+1,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.velocitous])});}
export function applyWardpactHit071({state,combat,targetId,attackerId,resolution,physicalSaveSucceeded}={}){if(!resolution?.hit)return Object.freeze({status:'NOT_APPLICABLE',resolution});const effect=activeWardpactCreature071(state,targetId,Number(combat?.round));if(!effect)return Object.freeze({status:'NO_WARDPACT_EFFECT',resolution});if(typeof physicalSaveSucceeded!=='boolean')return Object.freeze({status:'WARDPACT_PHYSICAL_SAVE_REQUIRED',resolution,effect});if(physicalSaveSucceeded)return Object.freeze({status:'SAVE_SUCCEEDED',resolution:Object.freeze({...resolution,wardpactPhysicalSaveSucceeded:true,wardpactEffectId:effect.effectId}),effect});return Object.freeze({status:'HIT_NEGATED',resolution:Object.freeze({...resolution,hitBeforeWardpact:true,hit:false,damageBeforeWardpact:Number(resolution.damage??0),damage:0,damageKind:'NONE',wardpactPhysicalSaveSucceeded:false,wardpactNegatedHit:true,wardpactEffectId:effect.effectId,wardpactAttackerId:attackerId}),effect,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.wardpact])});}
export function applyWardpactWeaponHarmless071({state,ownerId,weaponId,resolution}={}){const effect=activeWardpactWeapon071(state,ownerId,weaponId);if(!effect)return Object.freeze({status:'NO_WARDPACT_WEAPON_EFFECT',resolution});return Object.freeze({status:'WEAPON_HARMLESS',resolution:Object.freeze({...resolution,damageBeforeWardpactWeapon:Number(resolution.damage??0),damage:0,damageKind:'NONE',wardpactWeaponHarmless:true,wardpactEffectId:effect.effectId}),effect,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.wardpact])});}
export function markElementalGuardianDestroyedInPlace071({state,guardianNpcId,round=null}={}){const effects=magicEffects071(state);const i=effects.findIndex(e=>e?.spellId==='ELEMENTAL_GUARDIAN'&&e?.guardianNpcId===guardianNpcId&&e?.destroyed!==true);if(i<0)return Object.freeze({status:'NO_GUARDIAN_EFFECT'});effects[i]={...effects[i],destroyed:true,destroyedAtRound:round};return Object.freeze({status:'MARKED_DESTROYED',effect:effects[i],sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.guardian])});}


// Closure 071 finalization: terminal spatial/action contracts for the last source-bound effects.
function activeScorn071(state,targetId){
  const effects=state?.encounterState?.magicEffects??[];
  return effects.find(e=>e?.spellId==='SCORN_THE_FETTERS_OF_EARTH'&&e?.expired!==true&&Array.isArray(e?.targetIds)&&e.targetIds.includes(String(targetId))&&e?.canMoveInThreeDimensions===true)??null;
}
function activeScornGrounding071(state,targetId){
  const effects=state?.encounterState?.magicEffects??[];
  return effects.find(e=>e?.spellId==='SCORN_THE_FETTERS_OF_EARTH'&&e?.expired!==true&&e?.targetId===String(targetId)&&e?.grounded===true)??null;
}
export function magicSpatialMovementContract071({state,targetId,mode='GROUND',apertureMouseSized=false,explicitThreeDimensionalGeometry=false}={}){
  const requested=String(mode??'GROUND').trim().toUpperCase();
  const scorn=activeScorn071(state,targetId),grounded=activeScornGrounding071(state,targetId),stones=activeLikeStones071(state,targetId);
  if(requested==='FLY'||requested==='THREE_DIMENSIONAL'){
    if(grounded)return Object.freeze({status:'GROUNDED_FOR_SCENE',allowed:false,grounded:true,effect:grounded,sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.scorn])});
    const canFly=Boolean(scorn?.canMoveInThreeDimensions||stones?.element==='AIR'&&stones?.air?.canFlyAtUsualMove===true);
    if(!canFly)return Object.freeze({status:'NO_THREE_DIMENSIONAL_MOVEMENT_EFFECT',allowed:false});
    if(explicitThreeDimensionalGeometry!==true)return Object.freeze({status:'EXPLICIT_3D_GEOMETRY_REQUIRED',allowed:false,capability:true,sourceRefs:Object.freeze([scorn?WWN_COMBAT_EFFECT_071_SOURCES.scorn:WWN_COMBAT_EFFECT_071_SOURCES.likeStones])});
    return Object.freeze({status:'ALLOWED',allowed:true,threeDimensional:true,remainsSuspended:Boolean(scorn?.remainsSuspendedAfterMovement),sourceRefs:Object.freeze([scorn?WWN_COMBAT_EFFECT_071_SOURCES.scorn:WWN_COMBAT_EFFECT_071_SOURCES.likeStones])});
  }
  if(requested==='MOUSE_APERTURE'||requested==='APERTURE'){
    const allowed=stones?.element==='WATER'&&stones?.water?.canPassMouseSizedAperture===true&&apertureMouseSized===true;
    return Object.freeze({status:allowed?'ALLOWED':'APERTURE_NOT_ALLOWED',allowed,waterForm:stones?.element==='WATER',sourceRefs:allowed?Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.likeStones]):Object.freeze([])});
  }
  return Object.freeze({status:'ALLOWED',allowed:true,threeDimensional:false});
}
function settledD6_071(meta){const rolls=meta?.rolls;if(meta?.forcedSettle===true)throw new Error('forced physical settle is not authoritative');if(!Array.isArray(rolls)||rolls.length!==1||!Number.isInteger(rolls[0])||rolls[0]<1||rolls[0]>6)throw new RangeError('one settled physical d6 required');return rolls[0];}
function hpShape071(target){if(Number.isInteger(target?.hp))return{kind:'INT',current:target.hp,max:Number.isInteger(target.maxHp)?target.maxHp:target.hp};if(Number.isInteger(target?.hp?.current))return{kind:'OBJ',current:target.hp.current,max:Number.isInteger(target.hp.max)?target.hp.max:target.hp.current};if(Number.isInteger(target?.currentHp))return{kind:'CURRENT',current:target.currentHp,max:Number.isInteger(target.maxHp)?target.maxHp:target.currentHp};return null;}
function setTargetHp071(state,targetId,after){const pc=character071(state,targetId);if(pc){const n=clone(pc);n.hp={...n.hp,current:after};setCharacter(state,targetId,n);return n;}const npc=state?.npcState?.[targetId];if(!npc)return null;const n=clone(npc);if(Number.isInteger(n.hp))n.hp=after;else if(n.hp&&Number.isInteger(n.hp.current))n.hp={...n.hp,current:after};else n.currentHp=after;state.npcState={...(state.npcState??{}),[targetId]:n};return n;}
export function applyLikeStonesFireAuraInPlace071({state,casterId,round,targets=[],coverageConfirmed=false,rollPolicy=null,sharedPhysicalDice=null}={}){
  const effect=activeLikeStones071(state,casterId);if(effect?.element!=='FIRE'||effect?.fire?.startTurnMeleeDamage!=='1d6')return Object.freeze({status:'NO_LIKE_STONES_FIRE_AURA'});
  if(!Number.isInteger(round)||round<1)return Object.freeze({status:'ROUND_REQUIRED'});
  if(coverageConfirmed!==true)return Object.freeze({status:'EXPLICIT_MELEE_COVERAGE_REQUIRED'});
  const policy=String(rollPolicy??'').toUpperCase();if(!['SHARED','PER_TARGET'].includes(policy))return Object.freeze({status:'EXPLICIT_DAMAGE_ROLL_POLICY_REQUIRED'});
  const effects=state?.encounterState?.magicEffects??[],idx=effects.findIndex(e=>e?.effectId===effect.effectId);if(idx<0)return Object.freeze({status:'FIRE_AURA_EFFECT_NOT_FOUND'});
  const usedRounds=Array.isArray(effect.fireAuraRounds)?effect.fireAuraRounds:[];if(usedRounds.includes(round))return Object.freeze({status:'FIRE_AURA_ALREADY_RESOLVED_THIS_ROUND'});
  const targetList=Array.isArray(targets)?targets:[];if(targetList.some(t=>!String(t?.targetId??'').trim()||String(t.targetId)===String(casterId)))return Object.freeze({status:'VALID_ADJACENT_TARGETS_REQUIRED'});
  let shared=null;try{if(policy==='SHARED')shared=settledD6_071(sharedPhysicalDice);}catch(err){return Object.freeze({status:'PHYSICAL_D6_REQUIRED',message:err.message});}
  const results=[];
  for(const t of targetList){const targetId=String(t.targetId),obj=character071(state,targetId)??state?.npcState?.[targetId],hp=hpShape071(obj);if(!obj||!hp)return Object.freeze({status:'TARGET_HP_EVIDENCE_REQUIRED',targetId});let damage;try{damage=policy==='SHARED'?shared:settledD6_071(t.physicalDice);}catch(err){return Object.freeze({status:'PHYSICAL_D6_REQUIRED',targetId,message:err.message});}const after=Math.max(0,hp.current-damage);setTargetHp071(state,targetId,after);results.push(Object.freeze({targetId,damage,hpBefore:hp.current,hpAfter:after}));}
  effects[idx]={...effect,fireAuraRounds:[...usedRounds,round]};
  return Object.freeze({status:'APPLIED',round,rollPolicy:policy,results:Object.freeze(results),sourceRefs:Object.freeze([WWN_COMBAT_EFFECT_071_SOURCES.likeStones])});
}
