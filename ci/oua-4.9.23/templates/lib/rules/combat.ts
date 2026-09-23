import type { CharacterSheet } from '../db';
import { rollOneRing, secureRoll, type RollResult } from '../dice';
import type { CombatActorState, CombatRollRecord, CombatStance, MasterState } from '../master/types';
import { automaticInspired, heavyBlowBonus, isPierceCapable, isRangedWeapon, pierceFeatBonus, protectionRollBonus, protectionRollMode, weaponPiercingThreshold } from './equipment';

function clamp(value:number,min:number,max:number){return Math.max(min,Math.min(max,Number(value)||0));}
function stanceOrder(stance?:CombatStance):number{return stance==='forward'?0:stance==='open'?1:stance==='defensive'?2:stance==='rearward'?3:4;}

function heroActor(characterId:string,endurance:number,enduranceMax:number):CombatActorState{
  return {actorId:characterId,actorType:'hero',stance:'open',engagedWith:[],endurance,enduranceMax,wounded:false,defeated:false,position:{x:35,y:50},stateNotes:[]};
}
export function orderedCombatActors(state:MasterState):CombatActorState[]{
  return [...state.combat.actors].filter(a=>!a.defeated).sort((a,b)=>{
    if(a.actorType==='hero'&&b.actorType!=='hero') return -1;
    if(a.actorType!=='hero'&&b.actorType==='hero') return 1;
    if(a.actorType==='hero'&&b.actorType==='hero') return stanceOrder(a.stance)-stanceOrder(b.stance);
    return (a.initiative||0)-(b.initiative||0);
  });
}
export function startCombat(state:MasterState,characters:Array<{id:string;sheet:CharacterSheet}>,actorIds:string[]):MasterState{
  const existing=new Map(state.combat.actors.map(a=>[a.actorId,a]));
  const actors=actorIds.map((id,index)=>{
    const old=existing.get(id); if(old) return {...old,defeated:false,engagedWith:[],stateNotes:[]};
    const hero=characters.find(character=>character.id===id); const hp={current:Number(hero?.sheet.attributes.enduranceCurrent)||1,max:Number(hero?.sheet.attributes.enduranceMax)||1};
    const actor={...heroActor(id,hp.current,hp.max),position:{x:25+(index%4)*13,y:62+Math.floor(index/4)*9}};
    if(hero){actor.parry=Number(hero.sheet.combat.parry)||0;actor.protection=Number(hero.sheet.attributes.protection)||0;actor.hope=Number(hero.sheet.attributes.hopeCurrent)||0;actor.shadow=Number(hero.sheet.attributes.shadow)||0;actor.fatigue=Number(hero.sheet.attributes.fatigue)||0;actor.wounded=Boolean(hero.sheet.attributes.wounded);}
    return actor;
  });
  return {...state,activeMode:'combat',combat:{status:'opening-volley',id:'combat_'+Date.now(),round:0,actors,advantageDice:0,complicationDice:0,turnIndex:0,backgroundImage:state.combat.backgroundImage||null,narrativeBlocks:[],rolls:[],updatedAt:Date.now()},updatedAt:Date.now()};
}
export function rearwardAllowed(state:MasterState,actorId:string,smallFolkException=false):{allowed:boolean;reason:string}{
  const heroes=state.combat.actors.filter(a=>a.actorType==='hero'&&!a.defeated);
  const enemies=state.combat.actors.filter(a=>a.actorType!=='hero'&&!a.defeated);
  const meleeOthers=heroes.filter(a=>a.actorId!==actorId&&a.stance!=='rearward').length;
  if(enemies.length>heroes.length*2) return {allowed:false,reason:'Retaguarda indisponível: há mais do dobro de inimigos que Heróis.'};
  const needed=smallFolkException?1:2;
  if(meleeOthers<needed) return {allowed:false,reason:'Retaguarda exige '+needed+' companheiro(s) em posições corpo a corpo.'};
  return {allowed:true,reason:''};
}
export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false):MasterState{
  if(stance==='rearward'){
    const check=rearwardAllowed(state,characterId,smallFolkException);
    if(!check.allowed) throw new Error(check.reason);
  }
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===characterId?{...actor,stance}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}
export function nextRound(state:MasterState):MasterState{
  const ordered=orderedCombatActors(state); const current=Math.max(0,Number(state.combat.turnIndex||0));
  if(!ordered.length) return state;
  const nextIndex=(current+1)%ordered.length; const wrapped=nextIndex===0;
  const round=Math.max(1,state.combat.round||1)+(wrapped?1:0);
  const actors=state.combat.actors.map(actor=>{
    const notes=(actor.stateNotes||[]).filter(note=>!note.startsWith('temp-round:'));
    return {...actor,stateNotes:notes};
  });
  return {...state,combat:{...state.combat,status:'round',round,turnIndex:nextIndex,actors,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function endCombat(state:MasterState):MasterState{return {...state,activeMode:'adventure',combat:{...state.combat,status:'resolved',turnIndex:0,updatedAt:Date.now()},updatedAt:Date.now()};}
export function upsertCombatActor(state:MasterState,actor:CombatActorState):MasterState{
  const list=[...state.combat.actors];const index=list.findIndex(x=>x.actorId===actor.actorId);
  if(index>=0)list[index]={...list[index],...actor,engagedWith:actor.engagedWith||list[index].engagedWith||[],stateNotes:actor.stateNotes||list[index].stateNotes||[]};else list.push({...actor,engagedWith:actor.engagedWith||[],stateNotes:actor.stateNotes||[]});
  return {...state,combat:{...state.combat,actors:list,updatedAt:Date.now()},updatedAt:Date.now()};
}
export function moveCombatActor(state:MasterState,actorId:string,x:number,y:number):MasterState{
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===actorId?{...actor,position:{x:Math.max(3,Math.min(97,x)),y:Math.max(5,Math.min(95,y))}}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
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
  if(!actor||!target||actor.defeated||target.defeated) throw new Error('Combatente inválido para engajamento.');
  if(actor.stance==='rearward') throw new Error('Um Herói na Retaguarda não pode se engajar em combate corpo a corpo.');
  const targetHero=target.actorType==='hero'; const actorHero=actor.actorType==='hero';
  if(actorHero&&actor.engagedWith.length>=3) throw new Error('Este Herói já atingiu o limite normal de três oponentes de tamanho humano.');
  if(!actorHero&&targetHero&&target.engagedWith.length>=3) throw new Error('Este Herói já atingiu o limite normal de engajamento.');
  const actors=state.combat.actors.map(a=>a.actorId===actorId?{...a,engagedWith:Array.from(new Set([...(a.engagedWith||[]),targetId]))}:a.actorId===targetId?{...a,engagedWith:Array.from(new Set([...(a.engagedWith||[]),actorId]))}:a);
  return {...state,combat:{...state.combat,actors,updatedAt:Date.now()},updatedAt:Date.now()};
}

export function heroAttackDiceModifier(actor:CombatActorState):number{
  if(actor.stance==='forward') return 1;
  if(actor.stance==='defensive') return -Math.max(1,(actor.engagedWith||[]).length);
  return 0;
}
export function heroAttackTargetNumber(sheet:CharacterSheet,target:CombatActorState):number{
  const modifier=Number(sheet.attributes.tnModifiers?.strength||0);
  return clamp(20-Math.max(1,Number(sheet.attributes.strength)||1)+modifier+Math.max(0,Number(target.parry)||0),2,30);
}
export function rollHeroAttack(
  sheet:CharacterSheet,
  actor:CombatActorState,
  target:CombatActorState,
  weaponIndex:number,
  hopeDice=0,
  inspired=false,
):{roll:RollResult;weapon:NonNullable<CharacterSheet['combat']['weapons'][number]>;ranged:boolean}{
  const weapon=sheet.combat.weapons[Math.max(0,Math.min(sheet.combat.weapons.length-1,Math.trunc(weaponIndex)))]||sheet.combat.weapons[0];
  if(!weapon) throw new Error('O Herói não possui arma configurada.');
  const ranged=isRangedWeapon(weapon.name,weapon.group);
  if(actor.stance==='rearward'&&!ranged) throw new Error('Na Retaguarda só é permitido atacar com arma à distância.');
  if(actor.stance!=='rearward'&&ranged&&actor.engagedWith.length>0) throw new Error('Um combatente engajado não pode usar livremente uma arma à distância.');
  const rank=Math.max(0,Number(sheet.combat.proficiencies?.[weapon.group]||0));
  const inspiredDice=inspired||automaticInspired(sheet)?Math.max(hopeDice,2):hopeDice;
  const count=Math.max(0,rank+heroAttackDiceModifier(actor)+inspiredDice);
  const mode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':'normal';
  const roll=rollOneRing(count,0,heroAttackTargetNumber(sheet,target),weapon.group,mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
  return {roll,weapon,ranged};
}
export function attackIsPiercing(sheet:CharacterSheet,weapon:{name:string;group:string},roll:RollResult,usePierce=false):boolean{
  const feat=Number(roll.featResult||0);
  if(feat===12) return true;
  if(feat===11) return false;
  let effective=feat;
  if(usePierce&&Number(roll.tengwarCount||0)>0&&isPierceCapable(weapon.name,weapon.group)) effective+=pierceFeatBonus(weapon.group);
  if(/m[aã]os inclementes/i.test(String(sheet.other.virtues||''))&&usePierce&&Number(roll.tengwarCount||0)>0) effective+=1;
  return effective>=weaponPiercingThreshold(sheet,weapon.name);
}
export function heroAttackDamage(sheet:CharacterSheet,weapon:{name:string;damage:number},roll:RollResult,useHeavy=false):number{
  if(!roll.success) return 0;
  let damage=Math.max(0,Number(weapon.damage)||0);
  if(useHeavy&&Number(roll.tengwarCount||0)>0) damage+=heavyBlowBonus(sheet,weapon.name);
  return damage;
}
export function rollHeroProtection(sheet:CharacterSheet,injury:number,rangedPiercing=false):RollResult{
  const dice=Math.max(0,Number(sheet.attributes.protection)||0);
  const mode=Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax)?'ill-favoured':protectionRollMode(sheet,rangedPiercing);
  return rollOneRing(dice,protectionRollBonus(sheet),Math.max(2,Number(injury)||2),'Proteção',mode,Boolean(sheet.attributes.weary),Boolean(sheet.attributes.miserable));
}
export function rollActorProtection(actor:CombatActorState,injury:number):RollResult{
  return rollOneRing(Math.max(0,Number(actor.protection)||0),0,Math.max(2,Number(injury)||2),'Proteção','normal',false,false);
}
export type WoundSeverity='moderate'|'serious'|'critical'|'second-wound';
export function applyHeroWound(input:CharacterSheet):{sheet:CharacterSheet;severity:WoundSeverity;feat:number}{
  const next=structuredClone(input);
  if(next.attributes.wounded){
    next.attributes.enduranceCurrent=0;(next.attributes as any).dying=true;(next.attributes as any).unconscious=true;
    return {sheet:next,severity:'second-wound',feat:0};
  }
  next.attributes.wounded=true;
  (next.other as any).lastFirstAidSuccessDay=undefined;
  (next.other as any).lastFirstAidFailureDay=undefined;
  const feat=secureRoll(1,12);
  if(feat===12){
    (next.attributes as any).woundSeverity='moderate';(next.attributes as any).woundDays=0;(next.attributes as any).temporaryWound=true;
    return {sheet:next,severity:'moderate',feat};
  }
  if(feat===11){
    next.attributes.enduranceCurrent=0;(next.attributes as any).dying=true;(next.attributes as any).unconscious=true;(next.attributes as any).woundSeverity='critical';(next.attributes as any).woundDays=0;
    return {sheet:next,severity:'critical',feat};
  }
  (next.attributes as any).woundSeverity='serious';(next.attributes as any).woundDays=feat;
  return {sheet:next,severity:'serious',feat};
}
export function clearModerateWound(input:CharacterSheet):CharacterSheet{
  if((input.attributes as any).woundSeverity!=='moderate') return input;
  const next=structuredClone(input);next.attributes.wounded=false;(next.attributes as any).woundSeverity=undefined;(next.attributes as any).temporaryWound=false;return next;
}
export function resolveFirstAid(input:CharacterSheet,healRoll:RollResult,currentDay:number):{sheet:CharacterSheet;success:boolean;message:string}{
  const next=structuredClone(input); const prior=Number((next.other as any).lastFirstAidSuccessDay);
  if(Number.isFinite(prior)) return {sheet:input,success:false,message:'Este Ferimento já recebeu Primeiros Socorros com sucesso.'};
  const failDay=Number((next.other as any).lastFirstAidFailureDay);
  if(!healRoll.success&&failDay===currentDay) return {sheet:input,success:false,message:'Uma tentativa falhou hoje; é preciso esperar pelo menos um dia.'};
  if(!healRoll.success){(next.other as any).lastFirstAidFailureDay=currentDay;return {sheet:next,success:false,message:'Primeiros Socorros falharam.'};}
  (next.other as any).lastFirstAidSuccessDay=currentDay;
  if((next.attributes as any).dying){
    (next.attributes as any).dying=false;(next.attributes as any).unconscious=true;
    next.attributes.enduranceCurrent=1;
    if(next.attributes.wounded)(next.attributes as any).woundDays=Math.max(1,Number((next.attributes as any).woundDays||0)+10);
    return {sheet:next,success:true,message:'A vida foi salva; o Herói recuperará os sentidos em cerca de uma hora.'};
  }
  const reduction=1+Math.max(0,Number(healRoll.tengwarCount)||0);
  (next.attributes as any).woundDays=Math.max(1,Number((next.attributes as any).woundDays||1)-reduction);
  return {sheet:next,success:true,message:'Gravidade do Ferimento reduzida em '+reduction+' dia(s).'};
}
