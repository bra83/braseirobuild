/** P234 — Canonical five-adventure boundary. This is an allowlist, not an editorial certification. */
export const FIVE_PUBLISHED_ADVENTURE_IDS = Object.freeze([
  'under-black-sun',
  'beyond-the-rim',
  'long-arm-of-the-hutt',
  'jewel-of-yavin',
  'mask-of-the-pirate-queen'
]);

const ALLOWED = new Set(FIVE_PUBLISHED_ADVENTURE_IDS);
const ALIASES = Object.freeze({
  'under-black-sun': ['under a black sun', 'under black sun', 'sob um sol negro', 'swep01'],
  'beyond-the-rim': ['beyond the rim', 'alem da orla', 'swe05'],
  'long-arm-of-the-hutt': ['the long arm of the hutt', 'long arm of the hutt', 'o longo braco do hutt', 'swe02'],
  'jewel-of-yavin': ['the jewel of yavin', 'jewel of yavin', 'a joia de yavin', 'swe09'],
  'mask-of-the-pirate-queen': ['mask of the pirate queen', 'a mascara da rainha pirata', 'swe13']
});

function normalize(value) {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}
function containsPhrase(haystack, phrase) {
  return ` ${haystack} `.includes(` ${phrase} `);
}

export function assertFivePublishedAdventures(ids) {
  if (!Array.isArray(ids) || ids.length !== ALLOWED.size || new Set(ids).size !== ALLOWED.size || ids.some(id => !ALLOWED.has(id))) {
    throw new Error('Escopo de aventuras inválido: apenas os cinco módulos canônicos são permitidos.');
  }
  return true;
}

export function isCanonicalAdventureId(id) { return ALLOWED.has(String(id ?? '')); }

export function canonicalAdventureForSource(row = {}) {
  const text = normalize([row.fileName, row.title, row.source, row.id].filter(Boolean).join(' '));
  for (const id of FIVE_PUBLISHED_ADVENTURE_IDS) {
    if (ALIASES[id].some(alias => containsPhrase(text, normalize(alias)))) return id;
  }
  return null;
}

/** Fail closed for imported documents classified as AVENTURA.
 * Core rules, errata and supplements remain available to the RAG.
 * An unrecognized adventure is not silently treated as canonical.
 */
export function allowedHybridLibrarySource(row = {}) {
  if (row.enabled === false) return false;
  return String(row.authorityType ?? '').trim().toUpperCase() !== 'AVENTURA' || canonicalAdventureForSource(row) !== null;
}

/** Code resolves mechanics. RAG supplies cited reference. Gemini supplies proposals/narration.
 * No route by itself applies state changes. Absence of the code executor blocks resolution.
 */
export function hybridDecision({purpose, adventureId = null, executor = null, sources = [], providerAvailable = false} = {}) {
  if (adventureId != null && !isCanonicalAdventureId(adventureId)) return Object.freeze({route:'BLOCKED',reason:'Aventura fora dos cinco módulos autorizados.',canMutateState:false});
  if (purpose === 'mechanics') {
    return Object.freeze(typeof executor === 'function'
      ? {route:'CODE',reason:'Executor determinístico explicitamente fornecido; aprovação e validação de estado permanecem obrigatórias.',canMutateState:false}
      : {route:'BLOCKED',reason:'Sem executor determinístico; RAG e Gemini não podem substituir a regra.',canMutateState:false});
  }
  if (purpose === 'reference') {
    const grounded = sources.some(s => s && ['TESTADO','IMPLEMENTADO','FONTE_IMPORTADA'].includes(s.status) &&
      typeof s.summary === 'string' && s.summary.trim() && s.summary !== 'A_AUDITAR' &&
      (s.sourceKind !== 'library_pdf' || (allowedHybridLibrarySource(s) && (s.page != null || s.documentId != null))));
    return Object.freeze(grounded
      ? {route:'RAG',reason:'Consulta apenas a referências identificadas; resultado não executa mecânica.',canMutateState:false}
      : {route:'GM_REVIEW',reason:'Sem referência suficiente para afirmar regra oficial; pedir fonte ou julgamento explícito.',canMutateState:false});
  }
  if (purpose === 'narrative') {
    return Object.freeze(providerAvailable
      ? {route:'GEMINI_PROPOSAL',reason:'Narração/proposta sujeita ao validador local; IA sem autoridade mecânica.',canMutateState:false}
      : {route:'LOCAL_FALLBACK',reason:'Sem provedor: preservar estado e contexto, sem inventar prosa ou efeitos.',canMutateState:false});
  }
  return Object.freeze({route:'BLOCKED',reason:'Finalidade de ação desconhecida.',canMutateState:false});
}
