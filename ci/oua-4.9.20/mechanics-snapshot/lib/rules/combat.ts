import type { Character } from '../db';
import type { CombatActorState, CombatRollRecord, CombatStance, MasterState } from '../master/types';

function heroActor(hero: Character, index: number): CombatActorState {
  const attributes = hero.sheet.attributes;
  return {
    actorId: hero.id,
    actorType: 'hero',
    stance: 'open' as CombatStance,
    engagedWith: [],
    endurance: attributes.enduranceCurrent,
    enduranceMax: attributes.enduranceMax,
    wounded: attributes.wounded,
    defeated: attributes.enduranceCurrent <= 0,
    initiative: 12 - index,
    tokenId: hero.sheet.tokenId,
    position: { x: 0.18 + (index % 3) * 0.32, y: 0.76 + Math.floor(index / 3) * 0.13 },
    parry: hero.sheet.combat.parry,
    protection: Number(attributes.protection || 0),
    shadow: attributes.shadow,
    hope: attributes.hopeCurrent,
    fatigue: attributes.fatigue,
    stateNotes: [attributes.weary ? 'Cansado' : '', attributes.miserable ? 'Arrasado' : '', attributes.wounded ? 'Ferido' : ''].filter(Boolean),
    quickSheet: {
      name: hero.sheet.identity.name || hero.name,
      culture: hero.sheet.identity.culture,
      vocation: hero.sheet.identity.vocation,
      attributes: { strength: attributes.strength, heart: attributes.heart, wits: attributes.wits },
      endurance: `${attributes.enduranceCurrent}/${attributes.enduranceMax}`,
      hope: `${attributes.hopeCurrent}/${attributes.hopeMax}`,
      shadow: attributes.shadow,
      fatigue: attributes.fatigue,
      parry: hero.sheet.combat.parry,
      armor: hero.sheet.combat.armor,
      weapons: hero.sheet.combat.weapons,
      skills: hero.sheet.skills,
      virtues: hero.sheet.other.virtues,
      rewards: hero.sheet.other.rewards,
    },
  };
}

export function startCombat(state: MasterState, heroes: Character[], actorIds: string[]): MasterState {
  const allowed = new Set(actorIds);
  const previousById = new Map(state.combat.actors.map(actor => [actor.actorId, actor]));
  const heroActors = heroes.filter(hero => allowed.has(hero.id)).map(heroActor).map(actor => {
    const previous = previousById.get(actor.actorId);
    return previous?.position ? { ...actor, position: previous.position } : actor;
  });
  const previousNonHeroes = state.combat.actors.filter(actor => actor.actorType !== 'hero' && !actor.defeated);
  const actors = [...heroActors, ...previousNonHeroes].map((actor, index) => ({
    ...actor,
    initiative: actor.initiative ?? Math.max(1, 12 - index),
    position: actor.position || { x: actor.actorType === 'hero' ? 0.2 + (index % 3) * 0.18 : 0.72 + (index % 2) * 0.15, y: actor.actorType === 'hero' ? 0.75 : 0.25 + Math.floor(index / 2) * 0.16 },
  }));
  return {
    ...state,
    activeMode: 'combat',
    combat: {
      status: 'opening-volley',
      id: `combat_${Date.now()}`,
      round: 0,
      actors,
      advantageDice: 0,
      complicationDice: 0,
      turnIndex: 0,
      backgroundImage: state.combat.backgroundImage || null,
      narrativeBlocks: [],
      rolls: [],
      updatedAt: Date.now(),
    },
    updatedAt: Date.now(),
  };
}

export function setStance(state: MasterState, actorId: string, stance: CombatStance): MasterState {
  return { ...state, combat: { ...state.combat, actors: state.combat.actors.map(actor => actor.actorId === actorId ? { ...actor, stance } : actor), updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function nextRound(state: MasterState): MasterState {
  const activeActors = state.combat.actors.filter(actor => !actor.defeated);
  const nextTurn = activeActors.length ? ((state.combat.turnIndex || 0) + 1) % activeActors.length : 0;
  const wraps = activeActors.length > 0 && nextTurn === 0;
  return { ...state, combat: { ...state.combat, status: 'round', round: Math.max(1, state.combat.round + (wraps || state.combat.round === 0 ? 1 : 0)), turnIndex: nextTurn, updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function endCombat(state: MasterState): MasterState {
  return { ...state, activeMode: 'adventure', combat: { ...state.combat, status: 'resolved', updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function upsertCombatActor(state: MasterState, actor: CombatActorState): MasterState {
  const exists = state.combat.actors.some(item => item.actorId === actor.actorId);
  const actors = exists ? state.combat.actors.map(item => item.actorId === actor.actorId ? { ...item, ...actor, position: actor.position || item.position } : item) : [...state.combat.actors, actor];
  return { ...state, combat: { ...state.combat, actors, updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function moveCombatActor(state: MasterState, actorId: string, x: number, y: number): MasterState {
  const safeX = Math.max(0.03, Math.min(0.97, Number(x) || 0.5));
  const safeY = Math.max(0.05, Math.min(0.95, Number(y) || 0.5));
  return { ...state, combat: { ...state.combat, actors: state.combat.actors.map(actor => actor.actorId === actorId ? { ...actor, position: { x: safeX, y: safeY } } : actor), updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function recordCombatNarrative(state: MasterState, text: string): MasterState {
  const clean = String(text || '').trim().slice(0, 2400);
  if (!clean) return state;
  const block = { id: `combat_narrative_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, round: state.combat.round, text: clean, createdAt: Date.now() };
  return { ...state, combat: { ...state.combat, narrativeBlocks: [...(state.combat.narrativeBlocks || []), block].slice(-60), updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function recordCombatRoll(state: MasterState, roll: CombatRollRecord): MasterState {
  return { ...state, combat: { ...state.combat, rolls: [...(state.combat.rolls || []), roll].slice(-100), updatedAt: Date.now() }, updatedAt: Date.now() };
}

export function setCombatBackground(state: MasterState, imageData: string | null): MasterState {
  const safe = imageData && imageData.startsWith('data:image/') ? imageData.slice(0, 8_000_000) : null;
  return { ...state, combat: { ...state.combat, backgroundImage: safe, updatedAt: Date.now() }, updatedAt: Date.now() };
}
