import type { CharacterSheet } from './db';
import { attributeForCanonicalSkill, canonicalSkillName, type SkillAttribute } from './skillRegistry';

function normalize(value: unknown): string { return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim(); }
export { type SkillAttribute };
export function attributeForSkill(skillName: string): SkillAttribute { return attributeForCanonicalSkill(skillName); }
export function targetNumberForSkill(sheet: CharacterSheet, skillName: string): number {
  const key = attributeForSkill(skillName); const attribute = Math.max(1, Number(sheet.attributes[key]) || 0); const modifier = Number(sheet.attributes.tnModifiers?.[key] || 0); return Math.max(8, Math.min(19, 20 - attribute + modifier));
}
export function findSkill(sheet: CharacterSheet, skillName: string) {
  const wanted = canonicalSkillName(skillName);
  return sheet.skills.find(skill => canonicalSkillName(skill.name) === wanted) || sheet.skills.find(skill => normalize(skill.name) === normalize(skillName)) || null;
}
