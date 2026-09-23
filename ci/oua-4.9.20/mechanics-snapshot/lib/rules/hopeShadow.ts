import type { CharacterSheet } from '../db';

export function recomputeConditions(sheet: CharacterSheet): CharacterSheet {
  const next = structuredClone(sheet);
  const burden = Math.max(0, next.attributes.load) + Math.max(0, next.attributes.fatigue);
  next.attributes.weary = next.attributes.enduranceCurrent <= burden;
  next.attributes.miserable = next.attributes.hopeCurrent <= next.attributes.shadow;
  return next;
}

export function adjustEndurance(sheet: CharacterSheet, amount: number): CharacterSheet {
  const next = structuredClone(sheet);
  next.attributes.enduranceCurrent = clamp(next.attributes.enduranceCurrent + amount, 0, next.attributes.enduranceMax);
  return recomputeConditions(next);
}

export function adjustHope(sheet: CharacterSheet, amount: number): CharacterSheet {
  const next = structuredClone(sheet);
  next.attributes.hopeCurrent = clamp(next.attributes.hopeCurrent + amount, 0, next.attributes.hopeMax);
  return recomputeConditions(next);
}

export function adjustShadow(sheet: CharacterSheet, amount: number): CharacterSheet {
  const next = structuredClone(sheet);
  next.attributes.shadow = Math.max(0, next.attributes.shadow + amount);
  return recomputeConditions(next);
}

export function adjustFatigue(sheet: CharacterSheet, amount: number): CharacterSheet {
  const next = structuredClone(sheet);
  next.attributes.fatigue = Math.max(0, next.attributes.fatigue + amount);
  return recomputeConditions(next);
}

export function spendHope(sheet: CharacterSheet, attribute: 'strength' | 'heart' | 'wits'): { sheet: CharacterSheet; bonus: number; allowed: boolean } {
  if (sheet.attributes.hopeCurrent <= 0) return { sheet, bonus: 0, allowed: false };
  const next = adjustHope(sheet, -1);
  return { sheet: next, bonus: Math.max(0, Number(sheet.attributes[attribute]) || 0), allowed: true };
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, Number(value) || 0));
}
