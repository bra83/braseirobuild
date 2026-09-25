import type { CharacterSheet } from '../db';
import type { CombatActorState, JourneyState, MasterState } from '../master/types';

function norm(value:unknown):string {
  return String(value??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
}

export const LONE_LANDS_SOURCE = 'Contos das Terras Solitárias 2e';

export const ESTELANG = {
  name:'Estelang, a Esperança Inabalável',
  baseItemName:'Espada',
  origin:'Numenorean' as const,
  banes:['Undead','Evil Men'] as const,
  enchantedQualities:['superior-grievous','very-fell'] as const,
  standardReward:'Afiada',
};

export function heroHasEstelang(sheet:CharacterSheet):boolean {
  return (sheet.inventory||[]).some(item=>
    (item.tags||[]).some(tag=>norm(tag)==='estelang')
    || norm(item.name).startsWith('estelang')
  );
}

export function weaponIsEstelang(sheet:CharacterSheet,weaponName:string):boolean {
  const wanted=norm(weaponName);
  return (sheet.inventory||[]).some(item=>
    ((item.tags||[]).some(tag=>norm(tag)==='estelang')||norm(item.name).startsWith('estelang'))
    && (norm(item.name)===wanted || norm(item.magicalBaseItemName)===wanted)
  );
}

export function actorIsUndead(actor:CombatActorState|undefined|null):boolean {
  return Boolean(actor && (actor.baneTags||[]).some(tag=>tag==='Undead'));
}

export function actorHasDeadlyAbility(actor:CombatActorState|undefined|null,name:string):boolean {
  const key=norm(name);
  return Boolean(actor && (actor.deadlyAbilities||[]).some(value=>norm(value)===key));
}

export function estelangMakesFearful(sheet:CharacterSheet,weaponName:string,target:CombatActorState):boolean {
  return weaponIsEstelang(sheet,weaponName) && actorIsUndead(target);
}

export function forodwaithJourneyDiceModifier(
  journey:JourneyState,
  skill:string,
):number {
  const state=journey.loneLandsForodwaith;
  if(!state?.active) return 0;
  let modifier=state.lossothGear?-1:-2;
  if(state.lossothGuides && ['vigilancia','exploracao','cacada','viagem'].includes(norm(skill))) modifier+=1;
  return modifier;
}

export function forodwaithRollMode(
  journey:JourneyState,
  skill:string,
  base:'favoured'|'normal'|'ill-favoured',
):'favoured'|'normal'|'ill-favoured' {
  const state=journey.loneLandsForodwaith;
  if(!state?.active||!state.stormSpirits) return base;
  if(!['vigilancia','exploracao'].includes(norm(skill))) return base;
  return 'ill-favoured';
}

export function loneLandsOathHeroAttackBonus(actor:CombatActorState):number {
  return (actor.stateNotes||[]).includes('lone-lands-oath-hero')?1:0;
}

export function loneLandsOathEnemyAttackPenalty(actor:CombatActorState):number {
  return (actor.stateNotes||[]).includes('lone-lands-oath-enemy')?-1:0;
}

export function loneLandsOathShadowBonus(state:MasterState,characterId:string):number {
  if(!state.combat.loneLandsOathActive) return 0;
  const actor=state.combat.actors.find(item=>item.actorId===characterId);
  return actor&&(actor.stateNotes||[]).includes('lone-lands-oath-hero')?1:0;
}

export function summerLossothSafeRefugeActive(state:MasterState):boolean {
  return Boolean(state.company.loneLands?.lossothSafeRefugeNextAdventure);
}

export function lossothHuntCouncilBonus(state:MasterState):number {
  return state.company.loneLands?.lossothHuntCompleted?1:0;
}
