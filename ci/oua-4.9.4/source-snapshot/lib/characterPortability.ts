import type { Character, CharacterSheet } from './db';
import { CANONICAL_SKILLS, canonicalSkillName } from './skillRegistry';

export const CHARACTER_JSON_FORMAT = 'braseiro-o-um-anel-character';
export const CHARACTER_JSON_VERSION = 1;

interface CharacterEnvelope {
  format: typeof CHARACTER_JSON_FORMAT;
  version: number;
  system: 'the-one-ring-2e';
  exportedAt: number;
  character: Character;
}

function cleanText(value: unknown, max = 4000): string {
  return String(value ?? '').replace(/\u0000/g, '').slice(0, max);
}
function finite(value: unknown, fallback = 0): number {
  const n = Number(value); return Number.isFinite(n) ? n : fallback;
}
function bounded(value: unknown, min: number, max: number, fallback = min): number {
  return Math.max(min, Math.min(max, finite(value, fallback)));
}
function newId(prefix: string) { return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`; }

export function exportCharacterJson(character: Character): string {
  const portable = structuredClone(character);
  // O retrato é um Blob do IndexedDB local. Não exportamos um ID que seria órfão no outro aparelho.
  portable.sheet.tokenId = null;
  const envelope: CharacterEnvelope = {
    format: CHARACTER_JSON_FORMAT,
    version: CHARACTER_JSON_VERSION,
    system: 'the-one-ring-2e',
    exportedAt: Date.now(),
    character: portable,
  };
  return JSON.stringify(envelope, null, 2);
}

export function characterFileName(character: Character): string {
  const base = (character.sheet.identity.name || character.name || 'heroi')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]+/g, '_').replace(/^_+|_+$/g, '').toLowerCase();
  return `o_um_anel_ficha_${base || 'heroi'}.json`;
}

export function importCharacterJson(json: string, existingNames: string[] = []): Character {
  let parsed: any;
  try { parsed = JSON.parse(json); } catch { throw new Error('O arquivo não contém JSON válido.'); }
  if (parsed?.format !== CHARACTER_JSON_FORMAT) throw new Error('Este JSON não é uma ficha portátil do Braseiro — O Um Anel.');
  if (Number(parsed.version) !== CHARACTER_JSON_VERSION) throw new Error(`Versão de ficha não suportada: ${parsed.version}.`);
  if (parsed.system !== 'the-one-ring-2e') throw new Error('A ficha pertence a outro sistema e foi recusada.');
  const source = parsed.character;
  if (!source?.sheet?.identity || !source?.sheet?.attributes) throw new Error('Ficha incompleta: identidade ou atributos ausentes.');
  const name0 = cleanText(source.sheet.identity.name || source.name, 120).trim();
  if (!name0) throw new Error('A ficha precisa ter um nome.');
  const used = new Set(existingNames.map(v => v.trim().toLocaleLowerCase('pt-BR')));
  let name = name0, suffix = 2; while (used.has(name.toLocaleLowerCase('pt-BR'))) name = `${name0} ${suffix++}`;
  const raw: any = structuredClone(source.sheet);
  const attrs = raw.attributes || {};
  const skillsByName = new Map((Array.isArray(raw.skills) ? raw.skills : []).map((s: any) => [canonicalSkillName(cleanText(s.name, 80)) || cleanText(s.name, 80), s]));
  const sheet: CharacterSheet = {
    identity: {
      name, culture: cleanText(raw.identity?.culture, 120), vocation: cleanText(raw.identity?.vocation, 120), lifestyle: cleanText(raw.identity?.lifestyle, 80),
      age: Math.round(bounded(raw.identity?.age, 0, 2000, 20)), description: cleanText(raw.identity?.description), patron: cleanText(raw.identity?.patron, 240), company: cleanText(raw.identity?.company, 240),
    },
    attributes: {
      strength: bounded(attrs.strength, 0, 20), heart: bounded(attrs.heart, 0, 20), wits: bounded(attrs.wits, 0, 20),
      enduranceCurrent: bounded(attrs.enduranceCurrent, 0, 100), enduranceMax: bounded(attrs.enduranceMax, 0, 100),
      hopeCurrent: bounded(attrs.hopeCurrent, 0, 50), hopeMax: bounded(attrs.hopeMax, 0, 50), shadow: bounded(attrs.shadow, 0, 50), fatigue: bounded(attrs.fatigue, 0, 100), load: 0,
      wounded: Boolean(attrs.wounded), weary: Boolean(attrs.weary), miserable: Boolean(attrs.miserable), supplies: bounded(attrs.supplies, 0, 100, 100),
      valor: attrs.valor == null ? undefined : bounded(attrs.valor, 0, 10), wisdom: attrs.wisdom == null ? undefined : bounded(attrs.wisdom, 0, 10), protection: attrs.protection == null ? undefined : bounded(attrs.protection, 0, 20),
      tnModifiers: attrs.tnModifiers && typeof attrs.tnModifiers === 'object' ? { strength: bounded(attrs.tnModifiers.strength,-5,5,0), heart: bounded(attrs.tnModifiers.heart,-5,5,0), wits: bounded(attrs.tnModifiers.wits,-5,5,0) } : undefined,
    },
    skills: CANONICAL_SKILLS.map(skill => {
      const s: any = skillsByName.get(skill) || { name: skill, rank: 0, favorite: false, specialization: '' };
      return { name: skill, rank: Math.round(bounded(s.rank, 0, 6)), specialization: cleanText(s.specialization, 240), favorite: Boolean(s.favorite) };
    }),
    combat: {
      parry: bounded(raw.combat?.parry, 0, 50), armor: cleanText(raw.combat?.armor || 'Nenhuma', 120), helmet: cleanText(raw.combat?.helmet || 'Nenhum', 120), shield: cleanText(raw.combat?.shield || 'Nenhum', 120),
      proficiencies: Object.fromEntries(Object.entries(raw.combat?.proficiencies || {}).map(([k,v]) => [cleanText(k, 80), Math.round(bounded(v,0,6))])),
      weapons: (Array.isArray(raw.combat?.weapons) ? raw.combat.weapons : []).slice(0, 30).map((w:any) => ({ name: cleanText(w.name,120), damage: bounded(w.damage,0,30), injury: bounded(w.injury,0,30), group: cleanText(w.group,80), active: Boolean(w.active) })),
    },
    resources: { coins: bounded(raw.resources?.coins,0,1_000_000), treasure: bounded(raw.resources?.treasure,0,1_000_000), experience: bounded(raw.resources?.experience,0,1_000_000), adventurePoints: bounded(raw.resources?.adventurePoints,0,1_000_000), skillPoints: bounded(raw.resources?.skillPoints,0,1_000_000) },
    inventory: (Array.isArray(raw.inventory) ? raw.inventory : []).slice(0, 300).map((item:any) => ({ id:newId('item'), name:cleanText(item.name||'Item',160), quantity:bounded(item.quantity,0,999,1), weight:bounded(item.weight,0,999), load:bounded(item.load ?? item.weight,0,999), category:cleanText(item.category||'Geral',100), description:cleanText(item.description), equipped:Boolean(item.equipped), value:item.value==null?undefined:bounded(item.value,0,1_000_000), damage:item.damage==null?undefined:bounded(item.damage,0,30), injury:item.injury==null?undefined:bounded(item.injury,0,30), protection:item.protection==null?undefined:bounded(item.protection,0,30), tags:Array.isArray(item.tags)?item.tags.slice(0,30).map((x:any)=>cleanText(x,100)):undefined })),
    other: {
      virtues: cleanText(raw.other?.virtues), rewards: cleanText(raw.other?.rewards), flaws: cleanText(raw.other?.flaws), notes: cleanText(raw.other?.notes,12000), narrativeConditions: cleanText(raw.other?.narrativeConditions),
      culturalBlessing: raw.other?.culturalBlessing == null ? undefined : cleanText(raw.other.culturalBlessing), shadowPath: raw.other?.shadowPath == null ? undefined : cleanText(raw.other.shadowPath), callingFeature: raw.other?.callingFeature == null ? undefined : cleanText(raw.other.callingFeature),
      distinctiveFeatures: Array.isArray(raw.other?.distinctiveFeatures) ? raw.other.distinctiveFeatures.slice(0,10).map((x:any)=>cleanText(x,100)) : undefined,
      creationTrace: Array.isArray(raw.other?.creationTrace) ? raw.other.creationTrace.slice(0,100).map((x:any)=>cleanText(x,300)) : undefined,
    },
    tokenId: null,
    historyLog: Array.isArray(raw.historyLog) ? raw.historyLog.slice(0,100).map((h:any)=>({timestamp:finite(h.timestamp,Date.now()),field:cleanText(h.field,120),oldValue:cleanText(h.oldValue,500),newValue:cleanText(h.newValue,500),reason:cleanText(h.reason,300)})) : [],
  };
  sheet.attributes.enduranceCurrent = Math.min(sheet.attributes.enduranceCurrent, sheet.attributes.enduranceMax || sheet.attributes.enduranceCurrent);
  sheet.attributes.hopeCurrent = Math.min(sheet.attributes.hopeCurrent, sheet.attributes.hopeMax || sheet.attributes.hopeCurrent);
  sheet.attributes.load = sheet.inventory.reduce((sum,item)=>sum+Number(item.load||0)*Math.max(0,Number(item.quantity||0)),0);
  const now=Date.now();
  return { id:newId('char'), name, culture:sheet.identity.culture, vocation:sheet.identity.vocation, sheet, createdAt:now, updatedAt:now };
}
