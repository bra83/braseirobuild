import type { CharacterSheet } from '../db';
import type { TestRequirement } from '../../types';
import { attributeForSkill, targetNumberForSkill } from '../characterRules';
import { canonicalSkillName } from '../skillRegistry';

export function targetNumberFromAttribute(attribute: number): number {
  return Math.max(10, Math.min(20, 20 - Math.max(0, Number(attribute) || 0)));
}

export function findSkill(sheet: CharacterSheet, skillName: string) {
  const canonical = canonicalSkillName(skillName);
  const normalized = normalize(skillName);
  if (!normalized) return undefined;
  return sheet.skills.find(skill => canonical && canonicalSkillName(skill.name) === canonical)
    || sheet.skills.find(skill => normalize(skill.name) === normalized)
    || sheet.skills.find(skill => normalize(skill.name).includes(normalized) || normalized.includes(normalize(skill.name)));
}

export function skillAttribute(_sheet: CharacterSheet, skillName: string): 'strength' | 'heart' | 'wits' {
  return attributeForSkill(skillName);
}

export function createSkillTest(sheet: CharacterSheet, skillName: string, reason: string, characterId?: string): TestRequirement & { characterId?: string } {
  const skill = findSkill(sheet, skillName);
  const attribute = skillAttribute(sheet, skillName);
  return {
    id: `test_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    skill: skill?.name || skillName || 'Perícia apropriada',
    targetNumber: targetNumberForSkill(sheet, skillName),
    rollMode: skill?.favorite ? 'favoured' : 'normal',
    reason: String(reason || `Teste de ${skill?.name || skillName || 'perícia apropriada'}`),
    characterId,
  };
}

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

/**
 * O Um Anel pede dados quando há incerteza relevante e consequência em jogo.
 * Gestos sociais rotineiros não viram teste apenas porque uma perícia existe.
 */
export function shouldSuppressRoutineTest(action: string, test?: TestRequirement | null): boolean {
  if (!test) return false;
  const actionText = normalize(action);
  const stakesText = normalize(`${test.reason || ''} ${test.consequences || ''}`);
  const explicitRisk = /relut|hostil|desconfi|engan|mentir|intimid|ameac|segredo|ocult|informacao protegida|informação protegida|perigo|ferimento|sombra|fadiga|persegu|escapar|furtiv|roubar|arrombar|escalar|saltar|combate|ataque|resistir|fracasso/.test(stakesText);
  if (explicitRisk) return false;

  const routineSocial = /^(cumpriment|saud|dizer ola|dizer olá|apresentar[- ]?se|agradec|desped|pedir aposento|pedir um quarto|pedir quarto|pedir comida|pedir bebida|fazer um pedido|sentar[- ]?se|entrar|aproximar[- ]?se cordialmente)/.test(actionText)
    || /cumpriment.*(estalajadeiro|npc|homem|mulher|viajante)/.test(actionText);
  return routineSocial;
}
