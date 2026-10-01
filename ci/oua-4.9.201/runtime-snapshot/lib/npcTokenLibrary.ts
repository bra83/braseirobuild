import type { IndividualToken, Npc } from './db';
import { bulkSaveStoreData, getStoreData } from './db';

export type NpcTokenSource = 'bundled-npc-folder' | 'google-drive' | 'device' | 'token-sheet';

export interface NpcTokenDescriptor {
  id: string;
  tokenId: string;
  filename: string;
  path: string;
  label: string;
  index: number;
  signature: string;
  size: number;
  people: string[];
  gender: string[];
  ageGroups: string[];
  roles: string[];
  environment: string[];
  appearance: string[];
  equipment: string[];
  tags: string[];
  enabled?: boolean;
}

export interface NpcTokenManifest {
  manifestVersion: string;
  generatedAt: string;
  sourceFolder: string;
  filenamePolicy: string;
  supportedExtensions: string[];
  tokenCount: number;
  tokens: NpcTokenDescriptor[];
}

export interface NpcTokenCatalogEntry extends NpcTokenDescriptor {
  createdAt: number;
  importedAt: number;
  source: NpcTokenSource;
  driveFileId?: string;
  driveFolderId?: string;
  relativePath?: string;
  mimeType?: string;
  modifiedTime?: string;
  md5Checksum?: string;
  missingSince?: number;
}

export interface NpcTokenMatch {
  tokenId: string | null;
  score: number;
  reasons: string[];
  descriptorId?: string;
}

export interface NpcTokenDescriptorOverride {
  label?: string;
  people?: string[];
  gender?: string[];
  ageGroups?: string[];
  roles?: string[];
  environment?: string[];
  appearance?: string[];
  equipment?: string[];
  tags?: string[];
  enabled?: boolean;
}

let manifestPromise: Promise<NpcTokenManifest> | null = null;

export function normalizeNpcTokenText(value: string): string {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}

function words(value: string): string[] {
  return normalizeNpcTokenText(value).split(/\s+/).filter(Boolean);
}

export function stableNpcTokenHash(value: string): number {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function unique(values: string[]): string[] {
  return Array.from(new Set(values.filter(Boolean)));
}

function titleFromFilename(filename: string): string {
  const stem = filename.replace(/^.*[\\/]/, '').replace(/\.[^.]+$/, '');
  return words(stem).map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}

const PEOPLE: Record<string, string> = {
  anao: 'anao', ana: 'anao', anoes: 'anao', anas: 'anao',
  elfo: 'elfo', elfa: 'elfo', elfos: 'elfo', elfas: 'elfo',
  hobbit: 'hobbit', hobbits: 'hobbit',
  humano: 'humano', humana: 'humano', homem: 'humano', mulher: 'humano',
  dunedain: 'dunedain', dunadan: 'dunedain', beornida: 'beornida',
  orc: 'orc', orcs: 'orc', goblin: 'goblin', troll: 'troll',
};
const GENDER: Record<string, string> = {
  homem: 'masculino', masculino: 'masculino', menino: 'masculino', velho: 'masculino', rapaz: 'masculino',
  mulher: 'feminino', feminina: 'feminino', menina: 'feminino', velha: 'feminino', moca: 'feminino',
  elfo: 'masculino', elfa: 'feminino', anao: 'masculino', ana: 'feminino',
};
const AGE: Record<string, string> = {
  crianca: 'crianca', menino: 'crianca', menina: 'crianca', garoto: 'crianca', garota: 'crianca',
  jovem: 'jovem', adolescente: 'jovem', rapaz: 'jovem', moca: 'jovem', adulto: 'adulto', adulta: 'adulto',
  velho: 'velho', velha: 'velho', idoso: 'velho', idosa: 'velho', anciao: 'velho', ancia: 'velho',
};
const ROLE: Record<string, string> = {
  andarilho: 'andarilho', andarilha: 'andarilho', viajante: 'andarilho', peregrino: 'andarilho', peregrina: 'andarilho', errante: 'andarilho',
  barman: 'barman', taverneiro: 'barman', taverneira: 'barman', taberneiro: 'barman', taberneira: 'barman',
  estalajadeiro: 'estalajadeiro', estalajadeira: 'estalajadeiro',
  mendigo: 'mendigo', mendiga: 'mendigo', pedinte: 'mendigo',
  mercador: 'mercador', mercadora: 'mercador', comerciante: 'mercador', vendedor: 'mercador', vendedora: 'mercador',
  ferreiro: 'ferreiro', ferreira: 'ferreiro', guarda: 'guarda', sentinela: 'guarda', vigia: 'guarda',
  soldado: 'soldado', soldada: 'soldado', guerreiro: 'guerreiro', guerreira: 'guerreiro',
  arqueiro: 'arqueiro', arqueira: 'arqueiro', arco: 'arqueiro', flecha: 'arqueiro', flechas: 'arqueiro', cacador: 'cacador', cacadora: 'cacador', rastreador: 'cacador', rastreadora: 'cacador',
  curandeiro: 'curandeiro', curandeira: 'curandeiro', herbalista: 'curandeiro', medico: 'curandeiro', medica: 'curandeiro',
  campones: 'campones', camponesa: 'campones', fazendeiro: 'campones', fazendeira: 'campones', lavrador: 'campones', lavradora: 'campones',
  nobre: 'nobre', senhor: 'nobre', senhora: 'nobre', lorde: 'nobre', dama: 'nobre', emissario: 'emissario', emissaria: 'emissario',
  espiao: 'espiao', espia: 'espiao', ladrao: 'ladrao', ladra: 'ladrao', capitao: 'capitao', capita: 'capitao',
  barqueiro: 'barqueiro', barqueira: 'barqueiro', pescador: 'pescador', pescadora: 'pescador',
  sacerdote: 'sacerdote', sacerdotisa: 'sacerdote', sabio: 'sabio', sabia: 'sabio', crianca: 'crianca', menino: 'crianca', menina: 'crianca',
};
const ENVIRONMENT: Record<string, string> = {
  estalagem: 'estalagem', taverna: 'estalagem', pousada: 'estalagem', salao: 'estalagem',
  floresta: 'floresta', bosque: 'floresta', mata: 'floresta', estrada: 'estrada', caminho: 'estrada', trilha: 'estrada',
  cidade: 'cidade', vila: 'vila', povoado: 'vila', aldeia: 'vila', porto: 'porto', rio: 'rio', montanha: 'montanha',
  caverna: 'caverna', ruina: 'ruina', castelo: 'castelo', fazenda: 'fazenda', mercado: 'mercado', pantano: 'pantano', neve: 'neve',
};
const EQUIPMENT: Record<string, string> = {
  arco: 'arco', flecha: 'flecha', flechas: 'flecha', espada: 'espada', lanca: 'lanca', machado: 'machado', escudo: 'escudo',
  cajado: 'cajado', faca: 'faca', adaga: 'adaga', martelo: 'martelo', avental: 'avental', capuz: 'capuz', capa: 'capa',
  armadura: 'armadura', elmo: 'elmo', cachimbo: 'cachimbo', livro: 'livro', machadinha: 'machado', arcoflecha: 'arco',
};
const APPEARANCE = new Set([
  'barba','barbudo','barbuda','loiro','loira','ruivo','ruiva','moreno','morena','careca','cicatriz','magro','magra','gordo','gorda',
  'alto','alta','baixo','baixa','palido','palida','vestido','preto','preta','branco','branca','cinza','verde','azul','vermelho','vermelha',
  'dourado','dourada','prateado','prateada','trancas','tranca','cabelos','cabelo','olhos','tatuagem','sujo','suja','elegante','encapuzado','encapuzada',
]);

/** Converte o nome sugestivo do arquivo em metadados usados pelo seletor local. */
export function describeNpcTokenFilename(filename: string, override: NpcTokenDescriptorOverride = {}) {
  const tokens = words(filename.replace(/^.*[\\/]/, '').replace(/\.[^.]+$/, ''));
  return {
    label: override.label || titleFromFilename(filename),
    people: unique([...tokens.map(token => PEOPLE[token]), ...(override.people || [])]),
    gender: unique([...tokens.map(token => GENDER[token]), ...(override.gender || [])]),
    ageGroups: unique([...tokens.map(token => AGE[token]), ...(override.ageGroups || [])]),
    roles: unique([...tokens.map(token => ROLE[token]), ...(override.roles || [])]),
    environment: unique([...tokens.map(token => ENVIRONMENT[token]), ...(override.environment || [])]),
    appearance: unique([...tokens.filter(token => APPEARANCE.has(token)), ...(override.appearance || [])]),
    equipment: unique([...tokens.map(token => EQUIPMENT[token]), ...(override.equipment || [])]),
    tags: unique([...tokens, ...(override.tags || []).map(normalizeNpcTokenText)]),
    enabled: override.enabled !== false,
  };
}

const MANIFEST_TRAIT_MAP: Record<string, string> = {
  human: 'humano', dwarf: 'anao', elf: 'elfo', hobbit: 'hobbit', beorning: 'beornida',
  barding: 'bardingo', men_of_wilderland: 'homem_floresta', evil_men: 'humano',
  male: 'masculino', female: 'feminino', mixed: 'misto', adult: 'adulto', elder: 'velho', child: 'crianca', young: 'jovem',
  musician: 'musico', bard: 'musico', performer: 'musico', tavern_worker: 'estalajadeiro', inn_worker: 'estalajadeiro',
  innkeeper: 'estalajadeiro', bartender: 'barman', patron: 'viajante', traveller: 'andarilho', warrior: 'guerreiro',
  raider: 'guerreiro', chieftain: 'capitao', hunter: 'cacador', guardian: 'guarda', civilian: 'civil',
  mysterious_stranger: 'andarilho', beast: 'fera', monster: 'monstro', goblin: 'goblin', orc: 'orc', undead: 'morto_vivo',
  tavern: 'estalagem', inn: 'estalagem', forest: 'floresta', dark_forest: 'floresta', wilderness: 'ermos',
  underground: 'caverna', cave: 'caverna', elven_realm: 'floresta', village: 'vila', town: 'cidade', city: 'cidade',
};

function manifestTraits(values: unknown): string[] {
  const input = Array.isArray(values) ? values : values ? [values] : [];
  return unique(input.map(value => normalizeNpcTokenText(String(value))).filter(Boolean).map(value => MANIFEST_TRAIT_MAP[value] || value));
}

/** Converte metadados do tokens_manifest compartilhado para o vocabulário canônico de matching de O Um Anel. */
export function descriptorOverrideFromTokenSheet(sheet: { filename?: string; classification?: Record<string, unknown> }): NpcTokenDescriptorOverride {
  const classification = sheet.classification || {};
  return {
    label: titleFromFilename(String(sheet.filename || 'token')),
    people: manifestTraits(classification.cultures ?? classification.people),
    gender: manifestTraits(classification.genders ?? classification.gender),
    ageGroups: manifestTraits(classification.ageGroups),
    roles: manifestTraits(classification.roles),
    environment: manifestTraits(classification.environment),
    appearance: [],
    equipment: [],
    tags: unique([
      ...manifestTraits(classification.tags),
      ...manifestTraits(classification.creatureTypes),
      ...manifestTraits(classification.entityType),
    ]),
    enabled: true,
  };
}

const TRAIT_ALIASES: Record<string, string[]> = {
  humano: ['humano','humana','homem','mulher','homens','mulheres'], anao: ['anao','ana','anoes','anas'], elfo: ['elfo','elfa','elfos','elfas'],
  hobbit: ['hobbit','hobbits'], dunedain: ['dunedain','dunadan','guardiao do norte','guardia do norte'],
  beornida: ['beornida','beorning','beornings'], bardingo: ['bardingo','bardinga','barding','bardeses','bardes'],
  homem_floresta: ['homem da floresta','homens da floresta','woodman','woodmen','men of wilderland','terras ermas'],
  masculino: ['homem','masculino','menino','rapaz','velho','anciao'], feminino: ['mulher','feminino','menina','moca','velha','ancia'],
  crianca: ['crianca','menino','menina','garoto','garota'], jovem: ['jovem','rapaz','moca','adolescente'], velho: ['velho','velha','idoso','idosa','anciao','ancia'],
  barman: ['barman','taverneiro','taverneira','taberneiro','taberneira'], estalajadeiro: ['estalajadeiro','estalajadeira','dono da estalagem','dona da estalagem'],
  andarilho: ['andarilho','andarilha','viajante','peregrino','peregrina','errante'], mendigo: ['mendigo','mendiga','pedinte'],
  mercador: ['mercador','mercadora','comerciante','vendedor','vendedora'], guarda: ['guarda','sentinela','vigia'], soldado: ['soldado','soldada','guerreiro','guerreira'],
  arqueiro: ['arqueiro','arqueira','arco','flecha'], cacador: ['cacador','cacadora','rastreador','rastreadora'], ferreiro: ['ferreiro','ferreira','forjador','forjadora'],
  curandeiro: ['curandeiro','curandeira','herbalista','medico','medica'], campones: ['campones','camponesa','fazendeiro','fazendeira','lavrador','lavradora'],
  nobre: ['nobre','senhor','senhora','lorde','dama'], estalagem: ['estalagem','taverna','pousada','salao'], floresta: ['floresta','bosque','mata'],
  estrada: ['estrada','caminho','trilha'], cidade: ['cidade','vila','povoado','aldeia'],
};

function npcText(npc: Partial<Npc>): string {
  return normalizeNpcTokenText([
    npc.name, npc.culture, npc.peoples, npc.role, npc.faction, npc.appearance, npc.personality,
    npc.attitude, npc.objectives, npc.fears, npc.location, npc.state, npc.notes,
  ].filter(Boolean).join(' '));
}

function inferredTraits(npc: Partial<Npc>): Set<string> {
  const text = npcText(npc);
  const result = new Set(words(text));
  for (const [canonical, aliases] of Object.entries(TRAIT_ALIASES)) {
    if (aliases.some(alias => text.includes(normalizeNpcTokenText(alias)))) result.add(canonical);
  }
  return result;
}

function intersects(values: string[], traits: Set<string>): string[] {
  return unique(values.map(normalizeNpcTokenText).filter(value => traits.has(value) || words(value).some(word => traits.has(word))));
}

export async function loadNpcTokenManifest(force = false): Promise<NpcTokenManifest> {
  if (force) manifestPromise = null;
  if (!manifestPromise) {
    manifestPromise = fetch('./npc/npc_token_manifest.json', { cache: force ? 'reload' : 'no-cache' })
      .then(async response => {
        if (!response.ok) throw new Error(`Manifesto de tokens de NPC indisponível (${response.status}).`);
        const manifest = await response.json() as NpcTokenManifest;
        return { ...manifest, tokens: Array.isArray(manifest.tokens) ? manifest.tokens : [], tokenCount: Number(manifest.tokenCount) || 0 };
      })
      .catch(error => {
        console.warn('[NPC TOKENS] manifesto incorporado não carregado:', error);
        return {
          manifestVersion: '1.0.0', generatedAt: '', sourceFolder: 'npc', filenamePolicy: 'descritivo_separado_por_sublinhados',
          supportedExtensions: ['.png','.jpg','.jpeg','.webp'], tokenCount: 0, tokens: [],
        };
      });
  }
  return manifestPromise;
}

/** Importa apenas os tokens que vieram incorporados ao APK. O Drive é tratado pelo serviço próprio. */
export async function syncBundledNpcTokens(force = false): Promise<NpcTokenCatalogEntry[]> {
  const manifest = await loadNpcTokenManifest(force);
  if (!manifest.tokens.length) return [];
  const existing = await getStoreData<NpcTokenCatalogEntry>('npc_token_catalog').catch(() => []);
  const existingById = new Map(existing.map(item => [item.id, item] as const));
  const now = Date.now();
  const catalog: NpcTokenCatalogEntry[] = existing.filter(item => item.source !== 'bundled-npc-folder');
  const individual: IndividualToken[] = [];
  for (const descriptor of manifest.tokens) {
    const current = existingById.get(descriptor.id);
    catalog.push({ ...descriptor, enabled: descriptor.enabled !== false, createdAt: current?.createdAt || now, importedAt: now, source: 'bundled-npc-folder' });
    if (!force && current?.signature === descriptor.signature) continue;
    try {
      const response = await fetch(descriptor.path, { cache: force ? 'reload' : 'force-cache' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const blob = await response.blob();
      individual.push({ id: descriptor.tokenId, sheetId: 'npc-bundled', tokenIndex: descriptor.index, blob, category: 'npc', createdAt: current?.createdAt || now });
    } catch (error) {
      console.warn(`[NPC TOKENS] falha ao importar ${descriptor.filename}:`, error);
    }
  }
  await bulkSaveStoreData('npc_token_catalog', catalog);
  if (individual.length) await bulkSaveStoreData('individual_tokens', individual);
  return catalog.filter(item => item.enabled !== false);
}

export async function getNpcTokenCatalog(options: { includeDisabled?: boolean } = {}): Promise<NpcTokenCatalogEntry[]> {
  const stored = await getStoreData<NpcTokenCatalogEntry>('npc_token_catalog').catch(() => []);
  const catalog = stored.length ? stored : await syncBundledNpcTokens(false);
  return options.includeDisabled ? catalog : catalog.filter(item => item.enabled !== false);
}

export function matchNpcToken(
  npc: Partial<Npc> & { id?: string },
  catalog: NpcTokenCatalogEntry[],
  usedTokenIds: Set<string> = new Set(),
): NpcTokenMatch {
  const available = catalog.filter(item => item.enabled !== false);
  if (!available.length) return { tokenId: null, score: 0, reasons: [] };
  const traits = inferredTraits(npc);
  const text = npcText(npc);
  const scored = available.map(descriptor => {
    let score = 0;
    const reasons: string[] = [];
    const scoreGroup = (label: string, values: string[], weight: number) => {
      const hits = intersects(values || [], traits);
      if (hits.length) {
        score += weight + Math.min(weight, Math.max(0, hits.length - 1) * Math.ceil(weight / 3));
        reasons.push(`${label}: ${hits.join(', ')}`);
      }
    };
    scoreGroup('povo', descriptor.people, 24); scoreGroup('gênero', descriptor.gender, 18); scoreGroup('idade', descriptor.ageGroups, 16);
    scoreGroup('papel', descriptor.roles, 28); scoreGroup('ambiente', descriptor.environment, 10); scoreGroup('aparência', descriptor.appearance, 7);
    scoreGroup('equipamento', descriptor.equipment, 8);
    const tagHits = (descriptor.tags || []).map(normalizeNpcTokenText).filter(tag => tag.length > 2 && text.includes(tag));
    if (tagHits.length) { score += Math.min(20, tagHits.length * 3); reasons.push(`detalhes: ${unique(tagHits).slice(0, 5).join(', ')}`); }
    if (usedTokenIds.has(descriptor.tokenId)) score -= 22;
    return { descriptor, score, reasons };
  });
  const bestScore = Math.max(...scored.map(item => item.score));
  const candidates = scored.filter(item => item.score === bestScore).sort((a, b) => a.descriptor.id.localeCompare(b.descriptor.id));
  const chosen = candidates[stableNpcTokenHash(String(npc.id || npc.name || 'npc')) % candidates.length];
  if (!chosen || chosen.score < 1) return { tokenId: null, score: chosen?.score || 0, reasons: [] };
  return { tokenId: chosen.descriptor.tokenId, descriptorId: chosen.descriptor.id, score: chosen.score, reasons: chosen.reasons };
}

export function autoAssignNpcToken(npc: Npc, catalog: NpcTokenCatalogEntry[], allNpcs: Npc[], force = false): Npc {
  if (npc.tokenId && !force) return npc;
  if (npc.tokenAssignmentMode === 'manual' && !force) return npc;
  const used = new Set(allNpcs.filter(item => item.id !== npc.id && item.tokenId).map(item => String(item.tokenId)));
  const match = matchNpcToken(npc, catalog, used);
  if (!match.tokenId) return npc;
  return {
    ...npc,
    tokenId: match.tokenId,
    tokenAssignmentMode: 'automatic',
    tokenMatchScore: match.score,
    tokenMatchReason: match.reasons.join(' · '),
    tokenDescriptorId: match.descriptorId || null,
    updatedAt: Date.now(),
  };
}

export function autoAssignNpcTokens(npcs: Npc[], catalog: NpcTokenCatalogEntry[], force = false): Npc[] {
  let output = structuredClone(npcs);
  for (let index = 0; index < output.length; index += 1) output[index] = autoAssignNpcToken(output[index], catalog, output, force);
  return output;
}
