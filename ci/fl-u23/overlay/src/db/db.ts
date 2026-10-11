import { openDB, unwrap, DBSchema, IDBPDatabase, IDBPTransaction } from 'idb';

import {
  CampaignState,
  Character,
  Scene,
  TimelineBranch,
  Npc,
  BestiaryEntry,
  DiceRoll,
  MapManifest,
  TokenSheet,
  AudioAsset,
  Settings,
  IndividualToken,
  TokenAssignment,
  TokenImportState,
  MapSheet,
  IndividualHex,
  MapDiscovery,
  MapImportState,
  ExperienceSessionReview,
  WorldState,
} from '../types';

export type {
  CampaignState,
  Character,
  Scene,
  TimelineBranch,
  Npc,
  BestiaryEntry,
  DiceRoll,
  MapManifest,
  TokenSheet,
  AudioAsset,
  Settings,
  IndividualToken,
  TokenAssignment,
  TokenImportState,
  MapSheet,
  IndividualHex,
  MapDiscovery,
  MapImportState,
  ExperienceSessionReview,
  WorldState,
};

export interface BraseiroDB extends DBSchema {
  app_meta: { key: string; value: { id: string; value: any; updatedAt: number } };
  campaign: { key: string; value: CampaignState };
  characters: { key: string; value: Character };
  scenes: { key: string; value: Scene; indexes: { 'by-branch': string } };
  branches: { key: string; value: TimelineBranch };
  npcs: { key: string; value: Npc };
  bestiary: { key: string; value: BestiaryEntry };
  rolls: { key: string; value: DiceRoll; indexes: { 'by-scene': string; 'by-character': string } };
  maps: { key: string; value: MapManifest };
  tokenSheets: { key: string; value: TokenSheet };
  /** Legado. Novos sons ficam exclusivamente em braseiro-audio-db. */
  audioAssets: { key: string; value: AudioAsset };
  blobs: { key: string; value: Blob };
  character_avatars: { key: string; value: any };
  settings: { key: string; value: Settings };
  token_sheets: { key: string; value: TokenSheet };
  individual_tokens: { key: string; value: IndividualToken; indexes: { 'by-sheet': string; 'by-category': string } };
  token_assignments: { key: string; value: TokenAssignment };
  token_import_state: { key: string; value: TokenImportState };
  map_sheets: { key: string; value: MapSheet };
  individual_hexes: { key: string; value: IndividualHex; indexes: { 'by-map': string } };
  map_discovery: { key: string; value: MapDiscovery };
  map_import_state: { key: string; value: MapImportState };
  map_manifests: { key: string; value: any };
  rule_documents: { key: string; value: import('../types').RuleDocument };
  rule_chunks: { key: string; value: import('../types').RuleChunk; indexes: { 'by-doc': string } };
  rule_import_state: { key: string; value: import('../types').RuleImportState };
  model_status: { key: string; value: import('../types').ModelStatus };
  character_generation_drafts: { key: string; value: import('../types').CharacterGenerationState };
  xp_sessions: { key: string; value: ExperienceSessionReview; indexes: { 'by-campaign': string } };
  world_state: { key: string; value: WorldState };
}

const DB_NAME = 'braseiro-db';
const DB_VERSION = 15;
const CRITICAL_WRITE_STORES = new Set([
  'campaign', 'characters', 'scenes', 'branches', 'npcs', 'rolls',
  'map_discovery', 'token_assignments', 'individual_hexes', 'individual_tokens',
  'rule_documents', 'rule_chunks', 'settings',
  'xp_sessions', 'world_state',
]);

let connectionPromise: Promise<IDBPDatabase<BraseiroDB>> | null = null;
let connectionGeneration = 0;
let operationTail: Promise<void> = Promise.resolve();

function createStores(db: IDBPDatabase<BraseiroDB>) {
  if (!db.objectStoreNames.contains('app_meta')) db.createObjectStore('app_meta', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('campaign')) db.createObjectStore('campaign', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('characters')) db.createObjectStore('characters', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('scenes')) {
    const store = db.createObjectStore('scenes', { keyPath: 'id' });
    store.createIndex('by-branch', 'branchId');
  }
  if (!db.objectStoreNames.contains('branches')) db.createObjectStore('branches', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('npcs')) db.createObjectStore('npcs', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('bestiary')) db.createObjectStore('bestiary', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('rolls')) {
    const store = db.createObjectStore('rolls', { keyPath: 'id' });
    store.createIndex('by-scene', 'sceneId');
    store.createIndex('by-character', 'characterId');
  }
  if (!db.objectStoreNames.contains('maps')) db.createObjectStore('maps', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('tokenSheets')) db.createObjectStore('tokenSheets', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('audioAssets')) db.createObjectStore('audioAssets', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('blobs')) db.createObjectStore('blobs');
  if (!db.objectStoreNames.contains('character_avatars')) db.createObjectStore('character_avatars', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('settings')) db.createObjectStore('settings');
  if (!db.objectStoreNames.contains('token_sheets')) db.createObjectStore('token_sheets', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('individual_tokens')) {
    const store = db.createObjectStore('individual_tokens', { keyPath: 'id' });
    store.createIndex('by-sheet', 'sheetId');
    store.createIndex('by-category', 'category');
  }
  if (!db.objectStoreNames.contains('token_assignments')) db.createObjectStore('token_assignments', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('token_import_state')) db.createObjectStore('token_import_state', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('map_sheets')) db.createObjectStore('map_sheets', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('individual_hexes')) {
    const store = db.createObjectStore('individual_hexes', { keyPath: 'id' });
    store.createIndex('by-map', 'mapId');
  }
  if (!db.objectStoreNames.contains('map_discovery')) db.createObjectStore('map_discovery', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('map_import_state')) db.createObjectStore('map_import_state', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('map_manifests')) db.createObjectStore('map_manifests', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('rule_documents')) db.createObjectStore('rule_documents', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('rule_chunks')) {
    const store = db.createObjectStore('rule_chunks', { keyPath: 'id' });
    store.createIndex('by-doc', 'documentId');
  }
  if (!db.objectStoreNames.contains('rule_import_state')) db.createObjectStore('rule_import_state', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('model_status')) db.createObjectStore('model_status', { keyPath: 'modelId' });
  if (!db.objectStoreNames.contains('character_generation_drafts')) db.createObjectStore('character_generation_drafts', { keyPath: 'id' });
  if (!db.objectStoreNames.contains('xp_sessions')) {
    const store = db.createObjectStore('xp_sessions', { keyPath: 'id' });
    store.createIndex('by-campaign', 'campaignId');
  }
  if (!db.objectStoreNames.contains('world_state')) db.createObjectStore('world_state', { keyPath: 'id' });
}

function forgetConnection(generation?: number) {
  if (generation !== undefined && generation !== connectionGeneration) return;
  connectionGeneration += 1;
  connectionPromise = null;
}

function openConnection(): Promise<IDBPDatabase<BraseiroDB>> {
  if (connectionPromise) return connectionPromise;
  const generation = connectionGeneration;
  connectionPromise = openDB<BraseiroDB>(DB_NAME, DB_VERSION, {
    upgrade(db, oldVersion, _newVersion, transaction) {
      createStores(db);
      if (oldVersion < 15) {
        const characters = unwrap(transaction.objectStore('characters')) as IDBObjectStore;
        const cursorRequest = characters.openCursor();
        cursorRequest.onsuccess = () => {
          const cursor = cursorRequest.result;
          if (!cursor) return;
          const value = cursor.value as Character;
          if (oldVersion < 13) {
            const willpower = Number(value.willpowerPoints);
            value.willpowerPoints = Number.isFinite(willpower) ? Math.max(0, Math.min(10, Math.trunc(willpower))) : 0;
          }
          for (const item of [...(value.weapons || []), ...(value.armor || []), ...(value.gear || [])]) {
            if (!['normal', 'heavy', 'light', 'small'].includes(item?.encumbranceClass)) item.encumbranceClass = 'normal';
          }
          // Mirror EncumbranceRulesEngine.itemWeight, including zero quantities
          // and source weight overrides. Runtime and migrated weight must agree.
          const weight = (items: any[]) => items.reduce((sum, item) => {
            const quantityValue = Number(item?.quantity);
            const quantity = Number.isFinite(quantityValue) ? Math.max(0, Math.trunc(quantityValue)) : 1;
            const sourceWeight = Number(item?.sourceWeightUnits);
            const units = Number.isFinite(sourceWeight) && sourceWeight >= 0
              ? sourceWeight
              : ({normal:1,heavy:2,light:0.5,small:0}[item.encumbranceClass as 'normal'|'heavy'|'light'|'small'] ?? 1);
            return sum + quantity * units;
          }, 0);
          const coins = [value.coins?.copper, value.coins?.silver, value.coins?.gold]
            .reduce((sum, amount) => {
              const value = Number(amount);
              return sum + (Number.isFinite(value) ? Math.max(0, value) : 0);
            }, 0);
          // Legacy schema upgrades must use the SAME resource-die weight as
          // EncumbranceRulesEngine.calculate: each active consumable die occupies
          // one item. Otherwise 12–14 -> 15 temporarily grants free inventory
          // capacity until an unrelated inventory transaction recalculates it.
          const resourceSlots = Object.values(value.resourceDice || {}).filter(
            die => die === 'd6' || die === 'd8' || die === 'd10' || die === 'd12',
          ).length;
          const current = weight(value.weapons || []) + weight(value.armor || []) + weight(value.gear || [])
            + (coins < 100 ? 0 : Math.ceil(coins / 100) / 2) + resourceSlots;
          value.encumbrance = { current, max: Math.max(0, Math.trunc(Number(value.attributes?.strength?.max) || 0)) * 2 };
          cursor.update(value);
          cursor.continue();
        };
      }
    },
    blocked() { forgetConnection(generation); },
    blocking() {
      // Não chamamos close() durante uso normal. A instância é apenas marcada
      // como antiga e nunca mais será entregue a uma nova operação.
      forgetConnection(generation);
    },
    terminated() { forgetConnection(generation); },
  }).then(db => {
    if (generation !== connectionGeneration) throw new DOMException('A conexão aberta tornou-se antiga antes do uso.', 'InvalidStateError');
    return db;
  }).catch(error => {
    forgetConnection(generation);
    throw error;
  });
  return connectionPromise;
}

export function isClosingConnectionError(error: unknown): boolean {
  const name = error instanceof DOMException ? error.name : (error as any)?.name || '';
  const message = error instanceof Error ? error.message : String(error || '');
  return [
    'InvalidStateError', 'TransactionInactiveError', 'AbortError', 'UnknownError',
  ].includes(name) || /connection is closing|database connection is closing|database is closed|invalidstateerror|transaction.*closing|transaction is inactive|not active|connection.*closed/i.test(message);
}

export function invalidateDbConnection(): void {
  forgetConnection();
}

async function probe(db: IDBPDatabase<BraseiroDB>): Promise<void> {
  const tx = db.transaction('app_meta', 'readonly');
  await tx.store.get('__health_probe__');
  await tx.done;
}

async function healthyConnection(): Promise<IDBPDatabase<BraseiroDB>> {
  let lastError: unknown;
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      const db = await openConnection();
      await probe(db);
      return db;
    } catch (error) {
      lastError = error;
      forgetConnection();
      if (!isClosingConnectionError(error) && attempt === 0) throw error;
      await new Promise(resolve => setTimeout(resolve, 70 * (attempt + 1)));
    }
  }
  throw lastError;
}

async function executeWithRetry<T>(operation: (db: IDBPDatabase<BraseiroDB>) => Promise<T>): Promise<T> {
  let lastError: unknown;
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      const db = await healthyConnection();
      return await operation(db);
    } catch (error) {
      lastError = error;
      if (!isClosingConnectionError(error) || attempt === 3) throw error;
      forgetConnection();
      await new Promise(resolve => setTimeout(resolve, 90 * (attempt + 1)));
    }
  }
  throw lastError;
}

/**
 * Uma única fila central impede gravações concorrentes e também evita que uma
 * leitura observe uma substituição de estado pela metade. A fila sempre segue
 * adiante mesmo quando uma operação falha.
 */
function enqueue<T>(work: () => Promise<T>): Promise<T> {
  const result = operationTail.then(work, work);
  operationTail = result.then(() => undefined, () => undefined);
  return result;
}

export async function withDbRetry<T>(operation: (db: IDBPDatabase<BraseiroDB>) => Promise<T>): Promise<T> {
  const writes = new Set<string>();
  const result = await enqueue(() => executeWithRetry(async raw => {
    const tracked = new Proxy(raw as any, {
      get(target, property) {
        const name = String(property);
        const value = Reflect.get(target, property, target);
        if (typeof value !== 'function') return value;
        if (WRITE_METHODS.has(name)) {
          return async (...args: any[]) => {
            const output = await value.apply(target, args);
            writes.add(String(args[0]));
            return output;
          };
        }
        return value.bind(target);
      },
    }) as IDBPDatabase<BraseiroDB>;
    return operation(tracked);
  }));
  writes.forEach(notifyCriticalWrite);
  return result;
}

function notifyCriticalWrite(storeName: string) {
  if (typeof window === 'undefined' || !CRITICAL_WRITE_STORES.has(storeName)) return;
  window.dispatchEvent(new CustomEvent('braseiro-critical-write', { detail: { store: storeName } }));
}

const READ_METHODS = new Set(['get', 'getKey', 'getAll', 'getAllKeys', 'getAllFromIndex', 'getKeyFromIndex', 'count', 'countFromIndex']);
const WRITE_METHODS = new Set(['put', 'add', 'delete', 'clear']);

/**
 * Fachada compatível com o código legado. Nenhuma tela recebe IDBDatabase real:
 * cada chamada valida a conexão central no instante da operação e repete a
 * operação automaticamente se o WebView informar que a conexão está fechando.
 */
export async function getDb(): Promise<IDBPDatabase<BraseiroDB>> {
  await withDbRetry(async () => undefined);
  return new Proxy({} as IDBPDatabase<BraseiroDB>, {
    get(_target, property) {
      const name = String(property);
      if (READ_METHODS.has(name)) {
        return (...args: any[]) => withDbRetry(db => (db as any)[name](...args));
      }
      if (WRITE_METHODS.has(name)) {
        return (...args: any[]) => withDbRetry(db => (db as any)[name](...args));
      }
      if (name === 'close') return () => undefined;
      if (name === 'name') return DB_NAME;
      if (name === 'version') return DB_VERSION;
      if (name === 'objectStoreNames') return { contains: () => true };
      return undefined;
    },
  });
}

export async function withTransaction<T>(
  stores: keyof BraseiroDB | Array<keyof BraseiroDB>,
  mode: 'readonly' | 'readwrite',
  operation: (transaction: IDBPTransaction<BraseiroDB, any, any>) => Promise<T>,
): Promise<T> {
  const storeList = (Array.isArray(stores) ? stores : [stores]).map(String);
  const result = await enqueue(() => executeWithRetry(async db => {
    const tx = db.transaction(stores as any, mode as any) as IDBPTransaction<BraseiroDB, any, any>;
    try {
      const value = await operation(tx);
      await tx.done;
      return value;
    } catch (error) {
      // A failed callback must not leave a write transaction alive or commit
      // writes made before the exception. Abort and drain it before rethrowing.
      try { tx.abort(); } catch { /* the transaction may already be inactive */ }
      try { await tx.done; } catch { /* preserve the original operation error */ }
      throw error;
    }
  }));
  if (mode === 'readwrite') storeList.forEach(notifyCriticalWrite);
  return result;
}

export async function initDB() {
  await withDbRetry(async () => undefined);
  return getDb();
}

export async function ensureLibraryDatabaseReady() {
  const ready = await withDbRetry(async db => (
    db.objectStoreNames.contains('rule_documents')
    && db.objectStoreNames.contains('rule_chunks')
    && db.objectStoreNames.contains('rule_import_state')
  ));
  if (!ready) throw new Error('O banco local não concluiu a migração da Biblioteca. Reabra o aplicativo antes de tentar novamente.');
  return getDb();
}

export async function saveStoreData<T>(storeName: keyof BraseiroDB, data: T): Promise<void> {
  await withDbRetry(db => db.put(storeName as any, data as any).then(() => undefined));
}

export async function getStoreData<T>(storeName: keyof BraseiroDB): Promise<T[]> {
  return await withDbRetry(db => db.getAll(storeName as any)) as unknown as T[];
}

export type ConsistentStoreRecord = { key: IDBValidKey; value: any };

/**
 * Captura várias stores dentro da mesma transação readonly. Como a captura
 * inteira ocupa um único lugar na fila central, nenhuma gravação pode se
 * intercalar entre campanha, personagens e cenas do mesmo snapshot.
 */
export async function readStoresConsistently(
  stores: ReadonlyArray<keyof BraseiroDB>,
): Promise<Record<string, ConsistentStoreRecord[]>> {
  return enqueue(() => executeWithRetry(async db => {
    const tx = db.transaction([...stores] as any, 'readonly') as IDBPTransaction<BraseiroDB, any, 'readonly'>;
    const output: Record<string, ConsistentStoreRecord[]> = {};
    for (const storeName of stores) {
      const records: ConsistentStoreRecord[] = [];
      let cursor = await tx.objectStore(storeName as any).openCursor();
      while (cursor) {
        records.push({ key: cursor.primaryKey, value: cursor.value });
        cursor = await cursor.continue();
      }
      output[String(storeName)] = records;
    }
    await tx.done;
    return output;
  }));
}
