import { getStoreData, saveStoreData, type IndividualToken } from './db';

export type PartyRace = 'hobbit' | 'dwarf' | 'elf' | 'human' | 'enemy';

export interface PartyTokenEntry {
  id: string;
  asset: string;
  race: PartyRace;
  culture: string;
  category: string;
  visualScale: number;
  anchorX: number;
  anchorY: number;
  preferredLayer: 'front' | 'back';
  sourceSheet?: string;
  sourceIndex?: number;
}

export interface PartyTokenManifest {
  schemaVersion: number;
  packId: string;
  basePath: string;
  tokenCount: number;
  tokens: PartyTokenEntry[];
  cultureAliases?: Record<string, string[]>;
}

let manifestPromise: Promise<PartyTokenManifest | null> | null = null;

const clean = (value: string) => String(value || '').replace(/^\/+|\/+$/g, '');
const normalize = (value?: string | null) => String(value || '')
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export function partyRaceFromCulture(culture?: string | null): PartyRace | null {
  const text = normalize(culture);
  if (/hobbit|condado|shire|buque|tuk/.test(text)) return 'hobbit';
  if (/anao|dwarf|erebor|montanha solitaria|khazad/.test(text)) return 'dwarf';
  if (/elf|elfo|lindon|lorien|valfenda|imladris|mirkwood/.test(text)) return 'elf';
  if (/humano|homem|bree|bri|dunedain|patrulheiro do norte|ranger|bardingo|barding|dale|rohan|rohirrim|gondor|beorn|woodmen|homens da floresta/.test(text)) return 'human';
  return null;
}

export function partyCultureKey(culture?: string | null): string | null {
  const text = normalize(culture);
  if (/bree|\bbri\b/.test(text)) return 'bree';
  if (/dunedain|patrulheiro do norte|ranger/.test(text)) return 'dunedain';
  if (/rohan|rohirrim/.test(text)) return 'rohan';
  if (/gondor/.test(text)) return 'gondor';
  if (/beorn/.test(text)) return 'beorning';
  if (/woodmen|homens da floresta/.test(text)) return 'woodmen';
  if (/hobbit|condado|shire|buque|tuk/.test(text)) return 'shire';
  if (/anao|dwarf|erebor|montanha solitaria|khazad/.test(text)) return 'erebor';
  if (/elf|elfo|lindon|lorien|valfenda|imladris|mirkwood/.test(text)) return 'woodland';
  return null;
}

export async function loadPartyTokenManifest(): Promise<PartyTokenManifest | null> {
  if (manifestPromise) return manifestPromise;
  manifestPromise = fetch('./tor2e-ui/party-token-manifest.json', { cache: 'force-cache' })
    .then(async response => {
      if (!response.ok) return null;
      const raw = await response.json() as PartyTokenManifest;
      if (raw?.schemaVersion !== 1 || !Array.isArray(raw.tokens) || !raw.basePath) return null;
      return raw;
    })
    .catch(() => null);
  return manifestPromise;
}

export function resolvePartyTokenAsset(manifest: PartyTokenManifest | null, entryOrId: PartyTokenEntry | string | null | undefined): string | null {
  if (!manifest || !entryOrId) return null;
  const entry = typeof entryOrId === 'string' ? manifest.tokens.find(item => item.id === entryOrId) : entryOrId;
  if (!entry) return null;
  return `./${[clean(manifest.basePath.replace(/^\.\//, '')), clean(entry.asset)].filter(Boolean).join('/')}`;
}

export function partyTokenMatchesCulture(entry: PartyTokenEntry, culture?: string | null): boolean {
  if (entry.race === 'enemy') return false;
  const race = partyRaceFromCulture(culture);
  if (!race) return true;
  return entry.race === race;
}

export function partyTokenVisualScale(entry: PartyTokenEntry | null | undefined, culture?: string | null): number {
  if (entry?.visualScale && Number.isFinite(entry.visualScale)) return entry.visualScale;
  switch (partyRaceFromCulture(culture)) {
    case 'hobbit': return 0.80;
    case 'dwarf': return 0.89;
    case 'elf': return 1.03;
    case 'human': return 1.0;
    default: return 0.96;
  }
}

/** Persist only the selected built-in asset. The 120-source library stays in app assets and does not bloat IndexedDB. */
export async function installPartyTokenById(tokenId: string): Promise<IndividualToken | null> {
  const existing = (await getStoreData<IndividualToken>('individual_tokens').catch(() => [])).find(item => item.id === tokenId);
  if (existing?.blob) return existing;
  const manifest = await loadPartyTokenManifest();
  const entry = manifest?.tokens.find(item => item.id === tokenId);
  const url = resolvePartyTokenAsset(manifest, entry);
  if (!entry || !url) return null;
  const response = await fetch(url, { cache: 'force-cache' });
  if (!response.ok) throw new Error(`Asset de personagem indisponível: ${tokenId}`);
  const blob = await response.blob();
  const token: IndividualToken = {
    id: entry.id,
    sheetId: manifest?.packId || 'tor2e-party-characters-v1',
    tokenIndex: Number(entry.sourceIndex || 0),
    blob,
    category: entry.category,
    createdAt: Date.now(),
  };
  await saveStoreData('individual_tokens', token, 'selecionar asset de personagem da Companhia');
  return token;
}
