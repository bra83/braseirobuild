export interface TokenSheetPolicyInput {
  id?: string;
  filename: string;
  enabled?: boolean;
  excludeFromAutomaticSelection?: boolean;
  classification?: {
    category?: string;
    entityType?: string;
    cultures?: string[];
    tags?: string[];
  };
}

const BLOCKED_CULTURES = new Set(['drow', 'gnome', 'tiefling', 'dragonborn', 'halforc', 'half_orc']);
const BLOCKED_TAGS = new Set(['custom_fantasy', 'drow', 'dark_elf', 'gnome', 'tiefling']);
const BLOCKED_ENTITY_TYPES = new Set(['beholder']);
const normalize = (value: unknown) => String(value || '')
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');

/** Fail-closed para folhas explicitamente classificadas como fantasia externa ao cenário de O Um Anel. */
export function oneRingTokenSheetAllowed(sheet: TokenSheetPolicyInput): boolean {
  if (sheet.enabled === false) return false;
  const classification = sheet.classification || {};
  if (normalize(classification.category) === 'custom') return false;
  if (BLOCKED_ENTITY_TYPES.has(normalize(classification.entityType))) return false;
  const cultures = (classification.cultures || []).map(normalize);
  const tags = (classification.tags || []).map(normalize);
  if (cultures.some(value => BLOCKED_CULTURES.has(value))) return false;
  if (tags.some(value => BLOCKED_TAGS.has(value))) return false;
  const filename = normalize(sheet.filename);
  if (/(^|_)(drow|gnom|tiefling|beholder|bruenor)(_|$)/.test(filename)) return false;
  return true;
}

export function oneRingAutomaticTokenSheetAllowed(sheet: TokenSheetPolicyInput): boolean {
  return oneRingTokenSheetAllowed(sheet) && sheet.excludeFromAutomaticSelection !== true;
}
