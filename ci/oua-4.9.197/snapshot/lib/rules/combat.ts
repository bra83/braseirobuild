import type { CharacterSheet } from '../db';
import { applyFeatOneAsEye, combineRollModes, rollOneRing, secureRoll, type RollResult } from '../dice';
import { sheetFeatOneBecomesEye } from '../characterPaths';
import type { CombatActorState, CombatRollRecord, CombatStance, MasterState } from '../master/types';
import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollSources, weaponPiercingThreshold } from './equipment';
import { hasVirtue, supplementHeavyBlowStrengthBonus, supplementInspired, supplementRollMode } from './supplementMechanics';
import { applyIllLuckToResult, enchantedProtectionResultBonus, runeScoredArmourIgnoresConditions, runeScoredWeaponIgnoresConditions, straightFlightIgnoresComplications } from './magicalGearMechanics';
import { loneLandsOathHeroAttackBonus } from './loneLands';

function clamp(value:number,min:number,max:number){return Math.max(min,Math.min(max,Number(value)||0));}
function stanceOrder(stance?:CombatStance):number{return stance==='forward'?0:stance==='open'?1:stance==='defensive'?2:stance==='skirmish'?3:stance==='rearward'?4:5;}

function heroActor(characterId:string,endurance:number,enduranceMax:number):CombatActorState{
  return {actorId:characterId,actorType:'hero',stance:'open',engagedWith:[],endurance,enduranceMax,wounded:false,defeated:false,position:{x:0.35,y:0.18},stateNotes:[]};
}
export function orderedCombatActors(state:MasterState):CombatActorState[]{
  const escaped=new Set(state.combat.escapedHeroIds||[]);
  return [...state.combat.actors].filter(a=>!a.defeated&&!escaped.has(a.actorId)).sort((a,b)=>{
    if(a.actorType==='hero'&&b.actorType!=='hero') return -1;
    if(a.actorType!=='hero'&&b.actorType==='hero') return 1;
    if(a.actorType==='hero'&&b.actorType==='hero') return stanceOrder(a.stance)-stanceOrder(b.stance);
    return (a.initiative||0)-(b.initiative||0);
  });
}
export function markHeroEscaped(state:MasterState,heroId:string):MasterState{
  const hero=state.combat.actors.find(actor=>actor.actorId===heroId&&actor.actorType==='hero');
  if(!hero)throw new Error('Somente um Herói ativo pode deixar o campo de batalha.');
  const escapedHeroIds=Array.from(new Set([...(state.combat.escapedHeroIds||[]),heroId]));
  const actors=state.combat.actors.map(actor=>actor.actorId===heroId
    ?{...actor,engagedWith:[],stateNotes:[...(actor.stateNotes||[]).filter(note=>!note.startsWith('gain-ground:')),'escaped-skirmish']}
    :{...actor,engagedWith:(actor.engagedWith||[]).filter(id=>id!==heroId)});
  return {...state,combat:{...state.combat,escapedHeroIds,actors,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function markSkirmishEscapeFailed(state:MasterState,heroId:string):MasterState{
  const hero=state.combat.actors.find(actor=>actor.actorId===heroId&&actor.actorType==='hero');
  if(!hero)throw new Error('Somente um Herói ativo pode tentar fugir em Escaramuça.');
  const actors=state.combat.actors.map(actor=>actor.actorId!==heroId?actor:{...actor,stateNotes:Array.from(new Set([...(actor.stateNotes||[]).filter(note=>!note.startsWith('gain-ground:')),'temp-round:main-action-used:skirmish-escape']))});
  return {...state,combat:{...state.combat,actors,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function startCombat(state:MasterState,characters:Array<{id:string;sheet:CharacterSheet}>,actorIds:string[],environment:'open'|'underground'|'confined'='open'):MasterState{
  const existing=new Map(state.combat.actors.map(a=>[a.actorId,a]));
  const actors=actorIds.map((id,index)=>{
    const old=existing.get(id); if(old) return {...old,defeated:false,engagedWith:[],stateNotes:[]};
    const hero=characters.find(character=>character.id===id); const hp={current:Number(hero?.sheet.attributes.enduranceCurrent)||1,max:Number(hero?.sheet.attributes.enduranceMax)||1};
    const actor={...heroActor(id,hp.current,hp.max),position:{x:clamp(0.25+(index%4)*0.13,0.03,0.97),y:clamp(0.18+Math.floor(index/4)*0.09,0.05,0.45)}};
    if(hero){actor.parry=Number(hero.sheet.combat.parry)||0;actor.protection=Number(hero.sheet.attributes.protection)||0;actor.hope=Number(hero.sheet.attributes.hopeCurrent)||0;actor.shadow=Number(hero.sheet.attributes.shadow)||0;actor.fatigue=Number(hero.sheet.attributes.fatigue)||0;actor.wounded=Boolean(hero.sheet.attributes.wounded);}
    return actor;
  });
  return {...state,activeMode:'combat',combat:{status:'opening-volley',id:'combat_'+Date.now(),round:0,environment,actors,advantageDice:0,complicationDice:0,turnIndex:0,backgroundImage:state.combat.backgroundImage||null,narrativeBlocks:[],rolls:[],updatedAt:Date.now()},updatedAt:Date.now()};
}
export function rearwardAllowed(state:MasterState,actorId:string,smallFolkException=false,companionMeleeCount=0):{allowed:boolean;reason:string}{
  const escaped=new Set(state.combat.escapedHeroIds||[]);
  const heroes=state.combat.actors.filter(a=>a.actorType==='hero'&&!a.defeated&&!escaped.has(a.actorId));
  const enemies=state.combat.actors.filter(a=>a.actorType!=='hero'&&!a.defeated);
  const meleeOthers=heroes.filter(a=>a.actorId!==actorId&&a.stance!=='rearward').length+Math.max(0,Math.trunc(Number(companionMeleeCount)||0));
  if(enemies.length>heroes.length*2) return {allowed:false,reason:'Retaguarda indisponível: há mais do dobro de inimigos que Heróis.'};
  const needed=smallFolkException?1:2;
  if(meleeOthers<needed) return {allowed:false,reason:'Retaguarda exige '+needed+' companheiro(s) em posições corpo a corpo.'};
  return {allowed:true,reason:''};
}
export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false,companionMeleeCount=0):MasterState{
  if(stance==='rearward'){
    const check=rearwardAllowed(state,characterId,smallFolkException,companionMeleeCount);
    if(!check.allowed) throw new Error(check.reason);
  }
  if(stance==='skirmish'){
    const actors=state.combat.actors.map(actor=>{
      if(actor.actorId===characterId) return {...actor,stance,engagedWith:[]};
      return {...actor,engagedWith:(actor.engagedWith||[]).filter(id=>id!==characterId)};
    });
    return {...state,combat:{...state.combat,actors,updatedAt:Date.now()},updatedAt:Date.now()};
  }
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===characterId?{...actor,stance}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}
export function nextRound(state:MasterState):MasterState{
  const ordered=orderedCombatActors(state); const current=Math.max(0,Number(state.combat.turnIndex||0));
  if(!ordered.length) return state;
  const nextIndex=(current+1)%ordered.length; const wrapped=nextIndex===0;
  const round=Math.max(1,state.combat.round||1)+(wrapped?1:0);
  const actors=state.combat.actors.map(actor=>{
    const notes=(actor.stateNotes||[]).filter(note=>!note.startsWith('temp-round:')&&note!=='surprised');
    return {...actor,stateNotes:notes};
  });
  const companyInspiredThroughRound=Number(state.combat.companyInspiredThroughRound)>=round?state.combat.companyInspiredThroughRound:undefined;
  return {...state,combat:{...state.combat,status:'round',round,turnIndex:nextIndex,actors,companyInspiredThroughRound,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function isHeroInspiredInCombatRound(state:MasterState,_characterId:string):boolean{
  const round=Math.max(1,Number(state.combat.round)||1);
  return state.activeMode==='combat'&&Number(state.combat.companyInspiredThroughRound)===round;
}
export function endCombat(state:MasterState):MasterState{return {...state,activeMode:'adventure',combat:{...state.combat,status:'resolved',turnIndex:0,companyInspiredThroughRound:undefined,updatedAt:Date.now()},updatedAt:Date.now()};}
export function upsertCombatActor(state:MasterState,actor:CombatActorState):MasterState{
  const existing=state.combat.actors.find(x=>x.actorId===actor.actorId);
  const normalizedPosition=actor.position?{
    x:clamp(Math.abs(Number(actor.position.x))>1.5?Number(actor.position.x)/100:Number(actor.position.x),0.03,0.97),
    y:clamp(Math.abs(Number(actor.position.y))>1.5?Number(actor.position.y)/100:Number(actor.position.y),0.05,0.95),
  }:actor.position;
  const determinationMax=actor.determinationMax??existing?.determinationMax??(Number(actor.quickSheet?.determinationMax??actor.quickSheet?.determination)||undefined);
  const hateMax=actor.hateMax??existing?.hateMax??(Number(actor.quickSheet?.hateMax??actor.quickSheet?.hate)||undefined);
  const incoming={...actor,determinationMax,hateMax,position:normalizedPosition};
  const list=[...state.combat.actors];const index=list.findIndex(x=>x.actorId===actor.actorId);
  if(index>=0)list[index]={...list[index],...incoming,engagedWith:incoming.engagedWith||list[index].engagedWith||[],stateNotes:incoming.stateNotes||list[index].stateNotes||[]};else list.push({...incoming,engagedWith:incoming.engagedWith||[],stateNotes:incoming.stateNotes||[]});
  return {...state,combat:{...state.combat,actors:list,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function moveCombatActor(state:MasterState,actorId:string,x:number,y:number):MasterState{
  const rawX=Number(x), rawY=Number(y);
  const normalizedX=Number.isFinite(rawX)?(Math.abs(rawX)>1.5?rawX/100:rawX):0.5;
  const normalizedY=Number.isFinite(rawY)?(Math.abs(rawY)>1.5?rawY/100:rawY):0.5;
  const safeX=clamp(normalizedX,0.03,0.97);
  const safeY=clamp(normalizedY,0.05,0.95);
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===actorId?{...actor,position:{x:safeX,y:safeY}}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}
export function recordCombatNarrative(state:MasterState,text:string):MasterState{
  const clean=text.trim();if(!clean)return state;const blocks=[...(state.combat.narrativeBlocks||[]),{id:'combat_text_'+Date.now()+'_'+Math.random().toString(36).slice(2,6),round:Math.max(1,state.combat.round||1),text:clean,createdAt:Date.now()}].slice(-80);
  return {...state,combat:{...state.combat,narrativeBlocks:blocks,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function recordCombatRoll(state:MasterState,roll:CombatRollRecord):MasterState{
  const normalized={...roll,id:roll.id||'combat_roll_'+Date.now()+'_'+Math.random().toString(36).slice(2,6),createdAt:roll.createdAt||Date.now()};
  return {...state,combat:{...state.combat,rolls:[...(state.combat.rolls||[]),normalized].slice(-120),updatedAt:Date.now()},updatedAt:Date.now()};
}
export function setCombatBackground(state:MasterState,imageData:string|null):MasterState{return {...state,combat:{...state.combat,backgroundImage:imageData||null,updatedAt:Date.now()},updatedAt:Date.now()};}

export function engageActors(state:MasterState,actorId:string,targetId:string):MasterState{
  const actor=state.combat.actors.find(a=>a.actorId===actorId), target=state.combat.actors.find(a=>a.actorId===targetId);
  const escaped=new Set(state.combat.escapedHeroIds||[]);
  if(escaped.has(actorId)||escaped.has(targetId))throw new Error('Um Herói que deixou o campo de batalha não pode se engajar novamente.');
  if(!actor||!target||actor.defeated||target.defeated) throw new Error('Combatente inválido para engajamento.');
  if(actor.stance==='rearward'||actor.stance==='skirmish') throw new Error(actor.stance==='rearward'?'Um Herói na Retaguarda não pode se engajar em combate corpo a corpo.':'Um Herói em Escaramuça luta em movimento e não se engaja em combate corpo a corpo.');
  if(target.actorType==='hero'&&target.stance==='skirmish') throw new Error('Um Herói em Escaramuça não fica engajado; ataques corpo a corpo contra ele sofrem a penalidade da postura.');
  const targetHero=target.actorType==='hero'; const actorHero=actor.actorType==='hero';
  if(actorHero&&actor.engagedWith.length>=3) throw new Error('Este Herói já atingiu o limite normal de três oponentes de tamanho humano.');
  if(!actorHero&&targetHero&&target.engagedWith.length>=3) throw new Error('Este Herói já atingiu o limite normal de engajamento.');
  const actors=state.combat.actors.map(a=>a.actorId===actorId?{...a,engagedWith:Array.from(new Set([...(a.engagedWith||[]),targetId]))}:a.actorId===targetId?{...a,engagedWith:Array.from(new Set([...(a.engagedWith||[]),actorId]))}:a);
  return {...state,combat:{...state.combat,actors,updatedAt:Date.now()},updatedAt:Date.now()};
}

export function heroAttackDiceModifier(actor:CombatActorState):number{
  if(actor.stance==='forward') return 1;
  if(actor.stance==='defensive') return -Math.max(1,(actor.engagedWith||[]).length);
  if(actor.stance==='skirmish') return -1;
  return 0;
}
export function heroAttackTargetNumber(sheet:CharacterSheet,target:CombatActorState):number{
  const modifier=Number(sheet.attributes.tnModifiers?.strength||0);
  const base=sheet.other.rulesMode==='strider'?18:20;
  return clamp(base-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);
}
export function rollHeroAttack(
  sheet:CharacterSheet,
  actor:CombatActorState,
  target:CombatActorState,
  weaponIndex:number,
  hopeDice=0,
  inspired=false,
  context:{terrain?:string;isNight?:boolean;escaping?:boolean;advantageDice?:number;complicationDice?:number;ignoreComplication?:boolean;extraDicePenalty?:number;forcedMode?:'normal'|'favoured'|'ill-favoured';illFavouredSource?:boolean;combatRound?:number;openingVolley?:boolean}={},
):{roll:RollResult;weapon:NonNullable<CharacterSheet['combat']['weapons'][number]>;ranged:boolean}{
  const weapon=sheet.combat.weapons[Math.max(0,Math.min(sheet.combat.weapons.length-1,Math.trunc(weaponIndex)))]||sheet.combat.weapons[0];
  if(!weapon) throw new Error('O Herói não possui arma configurada.');
  const ranged=isRangedWeapon(weapon.name,weapon.group);
  if(actor.stance==='rearward'&&!ranged) throw new Error('Na Retaguarda só é permitido atacar com arma à distância.');
  if(actor.stance==='skirmish'&&!ranged) throw new Error('Na Postura de Escaramuça só é permitido atacar com arma à distância.');
  if(actor.stance!=='rearward'&&ranged&&actor.engagedWith.length>0) throw new Error('Um combatente engajado não pode usar livremente uma arma à distância.');
  const rank=Math.max(0,Number(sheet.combat.proficiencies?.[weapon.group]||0));
  const targetPower=Number(target.power)||Number(target.quickSheet?.power ?? target.quickSheet?.poder ?? target.quickSheet?.might)||0;
  const supplementContext={kind:'attack' as const,terrain:context.terrain,isNight:context.isNight,combatRound:context.combatRound,openingVolley:context.openingVolley,stance:actor.stance,weaponName:weapon.name,weaponGroup:weapon.group,targetPower,ranged};
  const isInspired=Boolean(inspired||automaticInspired(sheet)||supplementInspired(sheet,supplementContext));
  const inspiredDice=hopeDice>0&&isInspired?Math.max(hopeDice,2):hopeDice;
  const gainGroundNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainGroundNote?.split(':')[1])||0);
  const preparedShotNote=(actor.stateNotes||[]).find(note=>note.startsWith('prepared-shot:'));
  const preparedShot=ranged?Math.max(0,Number(preparedShotNote?.split(':')[1])||0):0;
  const stanceModifier=context.escaping&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const complications=straightFlightIgnoresComplications(sheet,weapon)||context.ignoreComplication?0:Math.max(0,Math.trunc(Number(context.complicationDice)||0));
  const combatTaskInspired=(actor.stateNotes||[]).includes('combat-task-inspired-next-attack:1')?1:0;
  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot+combatTaskInspired+loneLandsOathHeroAttackBonus(actor)+Math.max(0,Math.trunc(Number(context.advantageDice)||0))-complications-Math.max(0,Math.trunc(Number(context.extraDicePenalty)||0)));
  const naturalMode=supplementRollMode(sheet,supplementContext,'normal',{illFavoured:Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)});
  const baseMode=combineRollModes(naturalMode,context.forcedMode==='favoured',context.forcedMode==='ill-favoured');
  const mode=combineRollModes(baseMode,false,Boolean(context.illFavouredSource));
  const ignoreConditions=runeScoredWeaponIgnoresConditions(sheet,weapon);
  let roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,target),weapon.group,mode,ignoreConditions?false:Boolean(sheet.attributes.weary),ignoreConditions?false:Boolean(sheet.attributes.miserable));
  roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(sheet));
  roll=applyIllLuckToResult(sheet,roll);
  return {roll,weapon,ranged};
}
export function attackIsPiercing(sheet:CharacterSheet,weapon:{name:string;group:string;inventoryItemId?:string},roll:RollResult,usePierce=false):boolean{
  const feat=Number(roll.featResult||0);
  if(hasVirtue(sheet,'No Alvo')&&/pedra improvisada/i.test(String(weapon.name))&&feat===12) return true;
  if(feat===12) return true;
  if(feat===11) return false;
  let effective=feat;
  if(usePierce&&Number(roll.tengwarCount||0)>0&&isPierceCapable(weapon.name,weapon.group)) effective+=pierceFeatBonus(weapon.group);
  if(/m[aã]os inclementes/i.test(String(sheet.other.virtues||''))&&usePierce&&Number(roll.tengwarCount||0)>0) effective+=1;
  return effective>=weaponPiercingThreshold(sheet,weapon.name,weapon.inventoryItemId);
}
export function heroAttackDamage(sheet:CharacterSheet,weapon:{name:string;damage:number},roll:RollResult,useHeavy=false):number{
  if(!roll.success) return 0;
  let damage=Math.max(0,Number(weapon.damage)||0);
  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name)+supplementHeavyBlowStrengthBonus(sheet);
  return damage;
}
export function rollHeroProtection(sheet:CharacterSheet,injury:number,rangedPiercing=false,extraDice=0,forcedMode?:'normal'|'favoured'|'ill-favoured'):RollResult{
  const dice=Math.max(0,Number(sheet.attributes.protection)||0)+Math.trunc(Number(extraDice)||0);
  const protectionSources=protectionRollSources(sheet,rangedPiercing);
  const mode=forcedMode||combineRollModes('normal',protectionSources.favoured,protectionSources.illFavoured||Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax));
  const ignoreConditions=runeScoredArmourIgnoresConditions(sheet);
  let roll=rollOneRing(Math.max(0,dice),protectionRollBonus(sheet)+enchantedProtectionResultBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,ignoreConditions?false:Boolean(sheet.attributes.weary),ignoreConditions?false:Boolean(sheet.attributes.miserable));
  roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(sheet));
  roll=applyIllLuckToResult(sheet,roll);
  return roll;
}
export function rollActorProtection(actor:CombatActorState,injury:number,mode:'normal'|'favoured'|'ill-favoured'='normal',bonusDice=0,penaltyDice=0):RollResult{
  const protectionDice=Math.max(0,Math.max(0,Number(actor.protection)||0)+Math.max(0,Math.trunc(Number(bonusDice)||0))-Math.max(0,Math.trunc(Number(penaltyDice)||0)));
  return rollOneRing(protectionDice,0,Math.max(2,Number(injury)||2),'Proteção',mode,false,false);
}
export type WoundSeverity='moderate'|'serious'|'critical'|'second-wound';
function hasUnusedGreatDestiny(sheet:CharacterSheet):boolean{
  const owns=/grande destino|great destiny/i.test(String(sheet.other.virtues||''));
  return owns&&!Boolean(sheet.other.supplementState?.greatDestinyCompleted);
}
function markGreatDestinyPending(sheet:CharacterSheet):void{
  if(!hasUnusedGreatDestiny(sheet)) return;
  sheet.other.supplementState={...(sheet.other.supplementState||{}),greatDestinyPending:true};
}
export function applyHeroWound(input:CharacterSheet,illFavoured=false):{sheet:CharacterSheet;severity:WoundSeverity;feat:number;featResults:number[]}{
  const next=structuredClone(input);
  if(next.attributes.wounded){
    (next.other as any).lastFirstAidSuccessDay=undefined;(next.other as any).lastFirstAidFailureDay=undefined;
    next.attributes.enduranceCurrent=0;(next.attributes as any).dying=true;(next.attributes as any).unconscious=true;
    markGreatDestinyPending(next);
    return {sheet:next,severity:'second-wound',feat:0,featResults:[]};
  }
  next.attributes.wounded=true;
  (next.other as any).lastFirstAidSuccessDay=undefined;
  (next.other as any).lastFirstAidFailureDay=undefined;
  const rootsFavoured=/resistente como ra[ií]zes antigas/i.test(String(next.other.virtues||''));
  const featCount=rootsFavoured===illFavoured?1:rootsFavoured||illFavoured?2:1;
  const featResults=Array.from({length:featCount},()=>secureRoll(1,12));
  const feat=illFavoured?Math.min(...featResults):Math.max(...featResults);
  (next.attributes as any).woundFeatResults=featResults;
  if(feat===12){
    (next.attributes as any).woundSeverity='moderate';(next.attributes as any).woundDays=0;(next.attributes as any).temporaryWound=true;
    return {sheet:next,severity:'moderate',feat,featResults};
  }
  if(feat===11){
    next.attributes.enduranceCurrent=0;(next.attributes as any).dying=true;(next.attributes as any).unconscious=true;(next.attributes as any).woundSeverity='critical';(next.attributes as any).woundDays=0;
    markGreatDestinyPending(next);
    return {sheet:next,severity:'critical',feat,featResults};
  }
  (next.attributes as any).woundSeverity='serious';(next.attributes as any).woundDays=feat;
  return {sheet:next,severity:'serious',feat,featResults};
}
export function clearModerateWound(input:CharacterSheet):CharacterSheet{
  if((input.attributes as any).woundSeverity!=='moderate') return input;
  const next=structuredClone(input);next.attributes.wounded=false;(next.attributes as any).woundSeverity=undefined;(next.attributes as any).temporaryWound=false;(next.attributes as any).woundFeatResults=undefined;return next;
}
export function resolveFirstAid(input:CharacterSheet,healRoll:RollResult,currentDay:number,currentElapsedMinute=0,permanentInjuryMark=''):{sheet:CharacterSheet;success:boolean;message:string}{
  const next=structuredClone(input); const prior=Number((next.other as any).lastFirstAidSuccessDay);
  if(Number.isFinite(prior)) return {sheet:input,success:false,message:'Este Ferimento já recebeu Primeiros Socorros com sucesso.'};
  const failDay=Number((next.other as any).lastFirstAidFailureDay);
  if(!healRoll.success&&failDay===currentDay) return {sheet:input,success:false,message:'Uma tentativa falhou hoje; é preciso esperar pelo menos um dia.'};
  if(!healRoll.success){(next.other as any).lastFirstAidFailureDay=currentDay;return {sheet:next,success:false,message:'Primeiros Socorros falharam.'};}
  (next.other as any).lastFirstAidSuccessDay=currentDay;
  if((next.attributes as any).dying){
    const mark=String(permanentInjuryMark||'').trim().slice(0,160);
    if(!mark)return {sheet:input,success:false,message:'Escolha e registre uma marca permanente do Ferimento antes de salvar este Herói Morrendo.'};
    (next.attributes as any).dying=false;(next.attributes as any).dyingSinceElapsedMinute=undefined;(next.attributes as any).dyingRescueUntilElapsedMinute=Math.max(0,Number(currentElapsedMinute)||0)+60;(next.attributes as any).unconscious=true;
    (next.attributes as any).permanentInjuryMark=mark;
    next.attributes.enduranceCurrent=1;
    if(next.attributes.wounded){const daysRemoved=1+Math.max(0,Number(healRoll.tengwarCount)||0);(next.attributes as any).woundDays=Math.max(1,Number((next.attributes as any).woundDays||0)+10-daysRemoved);}
    return {sheet:next,success:true,message:'A vida foi salva; o Herói recuperará os sentidos em cerca de uma hora.'};
  }
  if((next.attributes as any).woundSeverity==='moderate'){
    return {sheet:clearModerateWound(next),success:true,message:'Primeiros Socorros encerraram o Ferimento moderado.'};
  }
  const reduction=1+Math.max(0,Number(healRoll.tengwarCount)||0);
  (next.attributes as any).woundDays=Math.max(1,Number((next.attributes as any).woundDays||1)-reduction);
  return {sheet:next,success:true,message:'Gravidade do Ferimento reduzida em '+reduction+' dia(s).'};
}
