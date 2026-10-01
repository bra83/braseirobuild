import { CampaignEvent, PlayerStats, SceneAudioPlan, SceneSourceReference, StoryChoice, TestRequirement, VoiceSegment, TtsVoiceName } from '../types';
import type { CommandAuditEntry, MasterState } from './master/types';
import { mayProvideMechanics, mayProvideNarrativeContext } from './sourceAuthority';

export interface PendingRoll {
  notation: string;
  dice?: string;
  results: number[];
  modifier: number;
  total: number;
  timestamp: number;
  type?: 'standard' | 'one-ring';
  featResult?: number;
  featResults?: number[];
  rollMode?: 'normal' | 'favoured' | 'ill-favoured';
  successResults?: number[];
  targetNumber?: number;
  skillName?: string;
  success?: boolean;
  successLevel?: 'failure' | 'success' | 'great-success' | 'extraordinary-success';
  tengwarCount?: number;
  characterId?: string;
  characterName?: string;
  magicalSuccess?: boolean;
  magicalSource?: string;
}

export interface PendingTest extends TestRequirement {
  optionId?: string;
  originalChoice: string;
  createdAt: number;
}

export interface CampaignState {
  id: string;
  name: string;
  /** Perfil genérico de sistema/cenário. Não há enum fechado por jogo. */
  systemProfileId?: string;
  settingProfileId?: string;
  rulesProfileName?: string;
  rulesEdition?: string;
  rulesSourceFolderId?: string;
  characterSheet: CharacterSheet;
  activeCharacterId?: string | null;
  partyCharacterIds?: string[];
  currentLocation: string;
  currentChapter: string;
  campaignSummary: string;
  recentEvents: string;
  clues: string;
  mysteries: string;
  quests: string;
  worldFlags: Record<string, boolean>;
  presentNpcIds: string[];
  history: SceneNode[];
  pendingDiceRoll: PendingRoll | null;
  pendingTest?: PendingTest | null;
  events?: CampaignEvent[];
  firstImageGenerated: boolean;
  schemaVersion?: number;
  createdAt: number;
  updatedAt: number;
  /** Sequência monotónica de gravações canónicas. */
  revision?: number;
  /** Identificador da última transação integral aplicada. */
  lastTransactionId?: string;
  masterState?: MasterState;
  /** Arquiteturas recentes para impedir aberturas repetidas. */
  openingArchitectureHistory?: string[];
  /** Histórico das sugestões exibidas ao jogador. */
  recentSuggestedActions?: string[];
  recentChoiceFamilies?: string[];
}

export interface CharacterSheet {
  identity: {
    name: string;
    culture: string;
    vocation: string;
    lifestyle: string;
    age: number;
    description: string;
    patron: string;
    company: string;
  };
  attributes: {
    strength: number;
    heart: number;
    wits: number;
    enduranceCurrent: number;
    enduranceMax: number;
    hopeCurrent: number;
    hopeMax: number;
    shadow: number;
    fatigue: number;
    load: number;
    wounded: boolean;
    weary: boolean;
    miserable: boolean;
    supplies: number;
    valor?: number;
    wisdom?: number;
    protection?: number;
    protectionBonus?: number;
    shadowScars?: number;
    shadowAtLimit?: boolean;
    woundSeverity?: 'moderate' | 'serious' | 'critical';
    woundDays?: number;
    temporaryWound?: boolean;
    dying?: boolean;
    dyingSinceElapsedMinute?: number;
    dyingRescueUntilElapsedMinute?: number;
    permanentInjuryMark?: string;
    unconscious?: boolean;
    dead?: boolean;
    frightenedUntilCombatEnds?: boolean;
    terribleSpellUnconscious?: boolean;
    terribleSpellUntilElapsedMinute?: number;
    poisoned?: { severity: 'moderate' | 'severe' | 'grave'; source: string; appliedDayKey: string; lastTickDayKey?: string; lastCureAttemptDayKey?: string };
    burning?: { source: string; appliedCombatRound: number; severity?: 'moderate' | 'severe' | 'grave'; fatalIncident?: boolean };
    asphyxiating?: { source: string; severity: 'moderate' | 'severe' | 'grave'; appliedCombatRound: number; fatalIncident?: boolean };
    /** Ajustes permanentes de NA por Virtudes como Proficiência. */
    tnModifiers?: Partial<Record<'strength' | 'heart' | 'wits', number>>;
  };
  skills: Array<{
    name: string;
    rank: number;
    specialization: string;
    favorite: boolean;
  }>;
  combat: {
    parry: number;
    armor: string;
    helmet: string;
    shield: string;
    /** Proficiências de combate de O Um Anel 2e. */
    proficiencies?: Record<string, number>;
    weapons: Array<{
      name: string;
      damage: number;
      injury: number;
      group: string;
      active: boolean;
      inventoryItemId?: string;
      magicalBaseItemName?: string;
      treasureIndexEntryId?: string;
      magicalQualities?: string[];
      magicalQualitiesUnlocked?: number;
      magicalRewards?: Array<{ kind:'common'|'enchanted'; id:string }>;
      magicalRewardsUnlocked?: number;
    }>;
  };
  resources: {
    coins: number;
    treasure: number;
    experience: number;
    adventurePoints: number;
    skillPoints: number;
  };
  inventory: Array<{
    id: string;
    name: string;
    quantity: number;
    weight: number;
    category: string;
    description: string;
    equipped: boolean;
    burning?: boolean;
    broken?: boolean;
    value?: number;
    damage?: number;
    injury?: number;
    load?: number;
    protection?: number;
    tags?: string[];
    magicalBaseItemName?: string;
    magicalKind?: 'magnificent-artefact' | 'wondrous-item' | 'wondrous-artefact' | 'famous-war-gear' | 'enchanted-reward';
    magicalBlessings?: string[];
    magicalOrigin?: 'Elven' | 'Dwarven' | 'Numenorean';
    magicalBanes?: string[];
    magicalQualities?: string[];
    magicalQualitiesUnlocked?: number;
    magicalRewards?: Array<{ kind:'common'|'enchanted'; id:string }>;
    magicalRewardsUnlocked?: number;
    magicalIdentified?: boolean;
    magicalCurse?: string;
    magicalCurseActive?: boolean;
    magicalCurseRevealed?: boolean;
    magicalCurseOwnerPresent?: boolean;
    magicalCurseHunterTag?: string;
    magicalCurseWeaknessFlaw?: string;
    magicalCurseShadowApplied?: number;
    magicalCurseTrigger?: string;
    magicalCurseRemoval?: string;
    treasureIndexEntryId?: string;
    destinedCharacterId?: string;
    craftsmanship?: string;
  }>;
  other: {
    virtues: string;
    rewards: string;
    flaws: string;
    notes: string;
    narrativeConditions: string;
    culturalBlessing?: string;
    shadowPath?: string;
    callingFeature?: string;
    distinctiveFeatures?: string[];
    creationTrace?: string[];
    rulesMode?: 'standard' | 'strider';
    supplementSelections?: {
      beornEnchantedSkills?: string[];
      [key: string]: unknown;
    };
    creationPreviousExperienceBudget?: number;
    heirOrigin?: {
      predecessorCharacterId: string;
      previousExperienceBudget: number;
      inheritedFavouredSkill: string;
      inheritedItemNames?: string[];
    };
    heirState?: {
      name?: string;
      previousExperiencePool: number;
      inheritedFavouredSkill?: string;
      inheritanceItemIds?: string[];
      arnorHeirloomItemId?: string;
      ready?: boolean;
      createdCharacterId?: string;
      retiredOrDead?: boolean;
    };
    characterPath?: {
      backgroundId: string;
      importantEventId: string;
      importantEventFeat: number;
      tnImprovementAttribute?: 'strength' | 'heart' | 'wits';
      addedFavouredSkills?: string[];
      removedFavouredSkills?: string[];
    };
    supplementState?: {
      houndWounded?: boolean;
      houndRecoveryPending?: boolean;
      houndRecoveryFailed?: boolean;
      houndWithHero?: boolean;
      houndWoundedSessionId?: string;
      houndRecoveryAvailableSessionId?: string;
      artificerYuleReady?: boolean;
      artificerAcquiredPhaseStartedAt?: number;
      artificerUsedYuleYear?: number;
      artificerStudyUsedPhaseStartedAt?: number;
      premonitionUsesByAdventurePhase?: Record<string, number>;
      [key: string]: unknown;
    };
    equipmentParryBonusApplied?: number;
    shadowPathStep?: number;
    retiredByShadow?: boolean;
    lastFirstAidSuccessDay?: number;
    lastFirstAidFailureDay?: number;
  };
  tokenId: string | null;
  historyLog: Array<{
    timestamp: number;
    field: string;
    oldValue: string;
    newValue: string;
    reason: string;
  }>;
}

export interface Character {
  id: string;
  name: string;
  culture: string;
  vocation: string;
  sheet: CharacterSheet;
  createdAt?: number;
  updatedAt?: number;
}

export interface SceneNarrativeMeta {
  intentClass?: 'action-diegetic' | 'speech-diegetic' | 'test-mechanic' | 'rule-query' | 'context-query' | 'recap' | 'spatial-clarification' | 'planning';
  advancesFiction?: boolean;
  checkpointSceneId?: string | null;
  openingSignature?: string;
  qualityFlags?: string[];
  rhythmCadence?: 'frozen' | 'opening' | 'arrival' | 'conversation' | 'council' | 'exploration' | 'journey' | 'danger' | 'combat' | 'aftermath';
  rhythmFlags?: string[];
  rhythmWordCount?: number;
  rhythmParagraphCount?: number;
  /** Fatos públicos validados pelo Diretor e reutilizados como cânone perceptível. */
  visibleFacts?: string[];
}

export interface SceneNode {
  id: string;
  timestamp: number;
  playerAction: string;
  atmospheric_narration: string;
  action_result: string;
  visualPrompt: string;
  sceneVisualPrompt?: string;
  sceneVisualPromptCreatedAt?: number;
  imageData?: string;
  imageFingerprint?: string;
  imageAspect?: '9:16' | '16:9';
  imageImportedAt?: number;
  location: string;
  weather?: string;
  timeOfDay?: string;
  worldHour?: number;
  worldMinute?: number;
  choices: StoryChoice[];
  requiredTest?: TestRequirement | null;
  statUpdates: Partial<PlayerStats>;
  presentNpcIds: string[];
  audioPlan?: SceneAudioPlan;
  events?: CampaignEvent[];
  sources?: SceneSourceReference[];
  voiceScript?: VoiceSegment[];
  narrativeMeta?: SceneNarrativeMeta;
  plannedSounds?: {
    ambience?: string;
    music?: string;
    sfx?: string;
  };
}

export interface Npc {
  id: string;
  name: string;
  culture: string;
  peoples: string;
  role: string;
  animalKind?: 'bird' | 'bear' | 'other';
  faction: string;
  appearance: string;
  personality: string;
  attitude: string;
  objectives: string;
  fears: string;
  secrets: string;
  notes: string;
  trust: number;
  firstMeeting: string;
  lastMeeting: string;
  location: string;
  state: string;
  tokenId: string | null;
  portraitPrompt?: string;
  portraitPromptCreatedAt?: number;
  portraitUpdatedAt?: number;
  presentInScene: boolean;
  voiceProfile?: TtsVoiceName;
  speechStyle?: string;
  emotionalState?: string;
  nextAction?: string;
  memorySummary?: string;
  lifeStatus?: 'alive' | 'missing' | 'dead';
  wealthIndex?: number;
  wealthTier?: 'destitute' | 'modest' | 'comfortable' | 'prosperous';
  memoryEvents?: Array<{ day: number; summary: string; createdAt: number }>;
  commitments?: Array<{ id: string; title: string; dueDay?: number; status: 'open' | 'fulfilled' | 'failed'; updatedAt: number }>;
  lastWorldTickDay?: number;
  relationshipByHero?: Record<string, number>;
  tokenAssignmentMode?: 'automatic' | 'manual';
  tokenMatchScore?: number;
  tokenMatchReason?: string;
  tokenDescriptorId?: string | null;
  personalityProfileId?: string;
  personalityProfile?: Record<string, unknown>;
  autoTokenGenerated?: boolean;
  createdAt?: number;
  updatedAt?: number;
}

export interface TokenSheet {
  id: string;
  filename: string;
  tokenCount: number;
  layout: {
    type: 'grid-centers' | 'explicit';
    columns?: number;
    rows?: number;
    xCenters?: number[];
    yCenters?: number[];
    cropWidth?: number;
    cropHeight?: number;
    tokens?: Array<{ index: number; x: number; y: number; width: number; height: number }>;
  };
  classification: {
    category?: string;
    entityType?: string;
    people?: string;
    gender?: string;
    cultures?: string[];
    genders?: string[];
    ageGroups?: string[];
    environment?: string[];
    roles?: string[];
    tags?: string[];
    creatureTypes?: string[];
  };
  createdAt: number;
}

export interface IndividualToken {
  id: string;
  sheetId: string;
  tokenIndex: number;
  blob: Blob;
  category: string;
  createdAt: number;
}

export interface TokenItem {
  id: string;
  name: string;
  blob: Blob;
  mimeType: string;
  category: string;
  createdAt: number;
}

export interface Manual {
  id: string;
  name: string;
  type: 'pdf' | 'txt' | 'md';
  priority: number;
  system: string;
  size: number;
  importedAt: number;
  pageCount?: number;
  chunkCount: number;
  indexed: boolean;
  indexState?: 'pending' | 'indexing' | 'ready' | 'failed';
  profileId?: string;
  systemProfileId?: string;
  settingProfileId?: string;
  rulesSourceFolderId?: string;
  driveFileId?: string;
  driveModifiedTime?: string;
  driveMd5Checksum?: string;
  drivePath?: string;
  contentHash?: string;
  pageOffset?: number;
  lastVerifiedAt?: number;
  indexError?: string;
}

export interface ManualChunk {
  id: string;
  manualId: string;
  manualName: string;
  text: string;
  /** Página impressa/exibida, quando conhecida. */
  pageNum: number;
  /** Página física dentro do ficheiro PDF. */
  physicalPageNum?: number;
  pageLabel?: string;
  index: number;
}

export interface SoundItem {
  id: string;
  name: string;
  blob?: Blob;
  nativeId?: string;
  source?: 'indexeddb' | 'native';
  mimeType: string;
  size: number;
  lastModified: number;
  signature: string;
  category: string;
  channel: 'voice' | 'ambience' | 'music' | 'sfx' | string;
  tags?: string[];
  catalogChannel?: 'VOICE' | 'SFX' | 'AMBIENCE_BASE' | 'WEATHER' | 'MUSIC' | string;
  familyKey?: string;
  environment?: string[];
  weather?: string;
  intensity?: string;
  timeOfDay?: string;
  durationMs?: number;
  loopable?: boolean;
  weight?: number;
  lastPlayedAt?: number;
  playCount?: number;
  failCount?: number;
  available?: boolean;
  createdAt: number;
}


export interface SoundKeywordRule {
  id: string;
  soundName: string;
  normalizedSoundName: string;
  keywords: string[];
  aliases: string[];
  tags: string[];
  channel?: 'ambience' | 'music' | 'sfx';
  category?: 'AMBIÊNCIA' | 'MÚSICA' | 'EFEITOS';
  createdAt: number;
  updatedAt: number;
}

export interface RollItem {
  id: string;
  dice: string;
  results: number[];
  modifier: number;
  total: number;
  timestamp: number;
  notation: string;
  type: 'standard' | 'one-ring';
  featResult?: number;
  featResults?: number[];
  rollMode?: 'normal' | 'favoured' | 'ill-favoured';
  successResults?: number[];
  targetNumber?: number;
  skillName?: string;
  success?: boolean;
  successLevel?: 'failure' | 'success' | 'great-success' | 'extraordinary-success';
  tengwarCount?: number;
}

interface SnapshotRecord {
  id: string;
  campaignId: string;
  createdAt: number;
  reason: string;
  data: CampaignState;
}

const DB_NAME = 'braseiro-db';
const DB_VERSION = 19;
const DB_OPEN_TIMEOUT_MS = 4_500;
const EMERGENCY_CAMPAIGN_KEY = 'braseiro.emergencyCampaign.v3';
const CRITICAL_STORES = new Set(['campaigns', 'characters', 'npcs', 'rolls', 'map_discovery', 'token_assignments']);
const ALL_STORES = [
  'app_meta', 'campaigns', 'characters', 'npcs', 'tokens', 'token_sheets', 'individual_tokens',
  'manuals', 'manual_chunks', 'sounds', 'sound_metadata', 'sound_blobs', 'rolls', 'map_manifests', 'map_datasets',
  'map_discovery', 'map_hex_crops', 'snapshots', 'campaign_events', 'tts_cache', 'sound_keyword_rules',
  'master_states', 'command_log', 'knowledge_ledger', 'actor_memories', 'rule_decisions', 'npc_token_catalog', 'npc_token_import_state', 'migration_reports', 'homologation_reports',
  'event_journal', 'turn_transactions', 'asset_registry', 'health_reports', 'manual_search_index', 'environment_states', 'autonomous_events', 'environment_memory',
  'transaction_outbox', 'manual_import_jobs', 'manual_import_staging', 'rules_folder_manifests', 'backup_restore_staging'
] as const;

let dbPromise: Promise<IDBDatabase> | null = null;
let writeQueue: Promise<unknown> = Promise.resolve();
const lastSnapshotAt = new Map<string, number>();

function isClosingError(error: unknown): boolean {
  const message = error instanceof Error ? `${error.name} ${error.message}` : String(error ?? '');
  return /InvalidStateError|closing|closed|inactive|not active|AbortError/i.test(message);
}

function createStores(db: IDBDatabase) {
  for (const name of ALL_STORES) {
    if (!db.objectStoreNames.contains(name)) {
      if (name === 'sound_blobs' || name === 'tts_cache') db.createObjectStore(name);
      else db.createObjectStore(name, { keyPath: 'id' });
    }
  }
}

async function openPhysicalDb(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    let settled = false;
    let blocked = false;
    const timeout = setTimeout(() => {
      if (settled) return;
      settled = true;
      dbPromise = null;
      reject(new Error(blocked
        ? 'O banco local ficou bloqueado por uma conexão anterior. Feche e abra o aplicativo novamente.'
        : 'O banco local demorou demais para responder. A abertura foi interrompida com segurança.'));
    }, DB_OPEN_TIMEOUT_MS);
    request.onupgradeneeded = () => createStores(request.result);
    request.onerror = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      dbPromise = null;
      reject(request.error || new Error('Não foi possível abrir o banco local.'));
    };
    request.onblocked = () => {
      blocked = true;
      console.warn('[BRASEIRO_DB] atualização bloqueada por outra aba/conexão.');
    };
    request.onsuccess = () => {
      const db = request.result;
      if (settled) {
        db.close();
        return;
      }
      settled = true;
      clearTimeout(timeout);
      db.onversionchange = () => {
        db.close();
        dbPromise = null;
      };
      db.onclose = () => { dbPromise = null; };
      resolve(db);
    };
  });
  return dbPromise;
}

async function withDbRetry<T>(operation: (db: IDBDatabase) => Promise<T>): Promise<T> {
  let lastError: unknown;
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      const db = await openPhysicalDb();
      return await operation(db);
    } catch (error) {
      lastError = error;
      if (!isClosingError(error) || attempt === 3) throw error;
      dbPromise = null;
      await new Promise(resolve => setTimeout(resolve, 70 * (attempt + 1)));
    }
  }
  throw lastError;
}

function requestPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('Falha na operação do banco.'));
  });
}

function transactionDone(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error || new Error('Falha na transação do banco.'));
    transaction.onabort = () => reject(transaction.error || new Error('Transação abortada.'));
  });
}

export async function openDb(): Promise<IDBDatabase> {
  return withDbRetry(async db => {
    const tx = db.transaction('app_meta', 'readonly');
    await requestPromise(tx.objectStore('app_meta').get('__health__'));
    await transactionDone(tx);
    return db;
  });
}

export async function getStoreData<T>(storeName: string): Promise<T[]> {
  return withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readonly');
    const result = await requestPromise(tx.objectStore(storeName).getAll());
    await transactionDone(tx);
    return result as T[];
  });
}

export async function getStoreItem<T>(storeName: string, id: IDBValidKey): Promise<T | undefined> {
  return withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readonly');
    const result = await requestPromise(tx.objectStore(storeName).get(id));
    await transactionDone(tx);
    return result as T | undefined;
  });
}

async function putRaw<T extends { id: string }>(storeName: string, item: T): Promise<void> {
  await withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).put(item);
    await transactionDone(tx);
  });
}

async function trimSnapshots(campaignId: string): Promise<void> {
  const snapshots = (await getStoreData<SnapshotRecord>('snapshots'))
    .filter(item => item.campaignId === campaignId)
    .sort((a, b) => b.createdAt - a.createdAt);
  for (const stale of snapshots.slice(16)) await deleteStoreData('snapshots', stale.id, false);
}

function cloneSceneWithoutImage(scene: SceneNode): SceneNode {
  const { imageData: _discardedImage, ...lightweight } = scene;
  return structuredClone(lightweight) as SceneNode;
}

function cloneCampaignWithoutImages(campaign: CampaignState, historyLimit?: number): CampaignState {
  const { history, ...campaignWithoutHistory } = campaign;
  const selectedHistory = historyLimit == null ? history : history.slice(-historyLimit);
  return {
    ...structuredClone(campaignWithoutHistory),
    history: selectedHistory.map(cloneSceneWithoutImage),
  };
}

export async function createCampaignSnapshot(campaign: CampaignState, reason = 'autosave'): Promise<void> {
  // Snapshots protect mechanics and story without duplicating every base64 image several times.
  const clean = cloneCampaignWithoutImages(campaign);
  await putRaw('snapshots', {
    id: `${campaign.id}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    campaignId: campaign.id,
    createdAt: Date.now(),
    reason,
    data: clean,
  });
  await trimSnapshots(campaign.id);
}

export async function recoverLatestCampaignSnapshot(campaignId: string): Promise<CampaignState | null> {
  const snapshots = (await getStoreData<SnapshotRecord>('snapshots'))
    .filter(item => item.campaignId === campaignId)
    .sort((a, b) => b.createdAt - a.createdAt);
  return snapshots[0]?.data ? structuredClone(snapshots[0].data) : null;
}

function campaignForEmergencyMirror(campaign: CampaignState): CampaignState {
  return cloneCampaignWithoutImages(campaign, 160);
}

export function saveEmergencyCampaignMirror(campaign: CampaignState): void {
  try {
    localStorage.setItem(EMERGENCY_CAMPAIGN_KEY, JSON.stringify(campaignForEmergencyMirror(campaign)));
  } catch (error) {
    console.warn('[BRASEIRO_DB] espelho de emergência não pôde ser atualizado:', error);
  }
}

export function readEmergencyCampaignMirror(): CampaignState | null {
  try {
    const raw = localStorage.getItem(EMERGENCY_CAMPAIGN_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CampaignState;
    if (!parsed?.id || !parsed.characterSheet || !Array.isArray(parsed.history)) return null;
    return parsed;
  } catch (error) {
    console.warn('[BRASEIRO_DB] espelho de emergência inválido:', error);
    return null;
  }
}

export async function saveStoreData<T extends { id: string }>(storeName: string, item: T, reason = 'save'): Promise<void> {
  if (!item?.id) throw new Error(`Item de ${storeName} sem id.`);
  const execute = async () => {
    if (storeName === 'campaigns') {
      const incoming = item as unknown as CampaignState;
      const previous = await getStoreItem<CampaignState>('campaigns', item.id).catch(() => undefined);
      const destructiveWriteAllowed = /restaur|nova crônica|nova cronica|reiniciar|importar backup/i.test(reason);
      if (previous?.history?.length && incoming.history?.length < previous.history.length && !destructiveWriteAllowed) {
        throw new Error(`Proteção do banco: uma gravação tentou reduzir o histórico de ${previous.history.length} para ${incoming.history?.length || 0} cenas.`);
      }
      if (previous?.characterSheet?.identity?.name && !incoming.characterSheet?.identity?.name) {
        throw new Error('Proteção do banco: uma ficha vazia tentou substituir a ficha existente.');
      }
      const now = Date.now();
      const previousSnapshot = lastSnapshotAt.get(item.id) || 0;
      const important = /novo turno|restaur|nova crônica|teste solicitado|rolagem|imagem de cena/i.test(reason);
      const interval = important ? 8_000 : 60_000;
      if ((previous?.history?.length || previous?.characterSheet) && now - previousSnapshot >= interval) {
        try {
          await createCampaignSnapshot(previous, reason);
          lastSnapshotAt.set(item.id, now);
        } catch (error) {
          // Snapshot is an extra safety net; its failure must never block the primary save.
          console.warn('[BRASEIRO_DB] snapshot falhou, mas a gravação principal continuará:', error);
        }
      }
    }
    await putRaw(storeName, item);
    if (storeName === 'campaigns') {
      await setAppMeta('activeCampaignId', item.id);
      saveEmergencyCampaignMirror(item as unknown as CampaignState);
    }
    window.dispatchEvent(new CustomEvent('braseiro-db-write', { detail: { storeName, id: item.id } }));
  };
  if (CRITICAL_STORES.has(storeName)) {
    const queued = writeQueue.then(execute, execute);
    writeQueue = queued.catch(() => undefined);
    await queued;
  } else {
    await execute();
  }
}

export async function bulkSaveStoreData<T extends { id?: string }>(storeName: string, items: T[]): Promise<void> {
  if (!items.length) return;
  await withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    for (const item of items) store.put(item);
    await transactionDone(tx);
  });
}

export async function deleteStoreData(storeName: string, id: IDBValidKey, queued = true): Promise<void> {
  const execute = () => withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).delete(id);
    await transactionDone(tx);
  });
  if (queued && CRITICAL_STORES.has(storeName)) {
    const promise = writeQueue.then(execute, execute);
    writeQueue = promise.catch(() => undefined);
    await promise;
  } else await execute();
}

export async function clearStore(storeName: string): Promise<void> {
  await withDbRetry(async db => {
    const tx = db.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).clear();
    await transactionDone(tx);
  });
}

export async function replaceStoresAtomically(data: Record<string, Array<{ id?: string }>>): Promise<void> {
  const storeNames = Object.keys(data);
  await withDbRetry(async db => {
    const tx = db.transaction(storeNames, 'readwrite');
    for (const storeName of storeNames) {
      const store = tx.objectStore(storeName);
      store.clear();
      for (const item of data[storeName]) store.put(item);
    }
    await transactionDone(tx);
  });
}

export interface AtomicStoreChange {
  put?: any[];
  delete?: IDBValidKey[];
  clear?: boolean;
}

/**
 * Aplica mudanças relacionadas em uma única transação IndexedDB sem limpar stores grandes.
 * É usada por importadores para que catálogo, blobs e estado de sincronização nunca fiquem pela metade.
 */
export async function applyStoreChangesAtomically(changes: Record<string, AtomicStoreChange>): Promise<void> {
  const storeNames = Object.keys(changes);
  if (!storeNames.length) return;
  await withDbRetry(async db => {
    const tx = db.transaction(storeNames, 'readwrite');
    for (const [storeName, change] of Object.entries(changes)) {
      const store = tx.objectStore(storeName);
      if (change.clear) store.clear();
      for (const id of change.delete || []) store.delete(id);
      for (const item of change.put || []) store.put(item);
    }
    await transactionDone(tx);
  });
}

export async function setAppMeta(key: string, value: unknown): Promise<void> {
  await putRaw('app_meta', { id: key, value, updatedAt: Date.now() });
}

export async function getAppMeta<T>(key: string): Promise<T | undefined> {
  const row = await getStoreItem<{ id: string; value: T }>('app_meta', key);
  return row?.value;
}



export interface CanonicalTransactionInput {
  campaign: CampaignState;
  reason: string;
  transactionId?: string;
  characters?: Character[];
  deleteCharacterIds?: string[];
  npcs?: Npc[];
  deleteNpcIds?: string[];
  audits?: CommandAuditEntry[];
  mapDiscovery?: any;
  environmentState?: any;
  environmentMemory?: any[];
  autonomousEvents?: any[];
  journalEntries?: any[];
  campaignEvents?: any[];
  turn?: {
    id: string;
    idempotencyKey: string;
    sceneId?: string;
    resultDigest?: string;
    payload?: Record<string, unknown>;
  };
}

interface TransactionOutboxRecord {
  id: string;
  campaignId: string;
  status: 'pending' | 'failed';
  createdAt: number;
  updatedAt: number;
  attempts: number;
  input: CanonicalTransactionInput;
  error?: string;
}

function cleanCampaignCheckpoint(campaign: CampaignState): CampaignState {
  return cloneCampaignWithoutImages(campaign);
}

export interface CanonicalStateSnapshot {
  campaign: CampaignState;
  characters: Character[];
  npcs: Npc[];
  mapDiscovery?: any;
  environmentState?: any;
  environmentMemory: any[];
  autonomousEvents: any[];
  capturedAt: number;
}

function mergeById<T extends { id: string }>(existing: T[], incoming: T[] = [], deleted: string[] = []): T[] {
  const rows = new Map(existing.map(item => [item.id, structuredClone(item)]));
  for (const id of deleted) rows.delete(id);
  for (const item of incoming) rows.set(item.id, structuredClone(item));
  return Array.from(rows.values());
}

/**
 * Captura o estado canónico correlacionado com a campanha. O checkpoint não
 * contém blobs pesados, mas é suficiente para reconstruir campanha, elenco,
 * posição e memória ambiental após uma interrupção ou corrupção parcial.
 */
async function buildCanonicalStateSnapshot(campaign: CampaignState, input: CanonicalTransactionInput, capturedAt: number): Promise<CanonicalStateSnapshot> {
  const [storedCharacters, storedNpcs, mapRows, environmentRows, memoryRows, autonomousRows] = await Promise.all([
    getStoreData<Character>('characters').catch(() => []),
    getStoreData<Npc>('npcs').catch(() => []),
    getStoreData<any>('map_discovery').catch(() => []),
    getStoreData<any>('environment_states').catch(() => []),
    getStoreData<any>('environment_memory').catch(() => []),
    getStoreData<any>('autonomous_events').catch(() => []),
  ]);
  const mergedCharacters = mergeById(storedCharacters, input.characters || [], input.deleteCharacterIds || []);
  const relevantCharacterIds = new Set([
    ...(campaign.partyCharacterIds || []),
    campaign.activeCharacterId || '',
    ...(input.characters || []).map(item => item.id),
  ].filter(Boolean));
  const characters = mergedCharacters.filter(item => relevantCharacterIds.has(item.id));

  const mergedNpcs = mergeById(storedNpcs, input.npcs || [], input.deleteNpcIds || []);
  const relevantNpcIds = new Set([
    ...(campaign.presentNpcIds || []),
    ...(campaign.masterState?.npcActors || []).map(item => item.npcId),
    ...(input.npcs || []).map(item => item.id),
  ].filter(Boolean));
  const npcs = mergedNpcs.filter(item => relevantNpcIds.has(item.id));

  const mapDiscovery = input.mapDiscovery || mapRows
    .filter(row => row.campaignId === campaign.id)
    .sort((a, b) => Number(b.updatedAt || 0) - Number(a.updatedAt || 0))[0];
  const environmentState = input.environmentState || environmentRows
    .filter(row => row.campaignId === campaign.id)
    .sort((a, b) => Number(b.generatedAt || b.updatedAt || 0) - Number(a.generatedAt || a.updatedAt || 0))[0];
  const environmentMemory = mergeById(
    memoryRows.filter(row => row.campaignId === campaign.id),
    input.environmentMemory || [],
  );
  const autonomousEvents = mergeById(
    autonomousRows.filter(row => row.campaignId === campaign.id),
    input.autonomousEvents || [],
  );
  return {
    campaign: cleanCampaignCheckpoint(campaign),
    characters,
    npcs,
    mapDiscovery: mapDiscovery ? structuredClone(mapDiscovery) : undefined,
    environmentState: environmentState ? structuredClone(environmentState) : undefined,
    environmentMemory,
    autonomousEvents,
    capturedAt,
  };
}

async function pruneCampaignCheckpoints(campaignId: string): Promise<void> {
  const rows = (await getStoreData<any>('event_journal').catch(() => []))
    .filter(row => row.campaignId === campaignId && row.eventType === 'CAMPAIGN_CHECKPOINT')
    .sort((a, b) => Number(b.createdAt || 0) - Number(a.createdAt || 0));
  for (const stale of rows.slice(30)) await deleteStoreData('event_journal', stale.id, false).catch(() => undefined);
}

/**
 * Grava campanha, atores, mapa, ambiente, diário e estado do turno numa única
 * transação IndexedDB. A outbox é persistida antes do commit e removida dentro
 * da mesma transação, permitindo recuperação idempotente após morte do processo.
 */
export async function saveCanonicalTransaction(input: CanonicalTransactionInput): Promise<CampaignState> {
  if (!input.campaign?.id) throw new Error('Campanha inválida na transação canónica.');
  const transactionId = input.transactionId || `canonical:${input.campaign.id}:${Date.now()}:${Math.random().toString(36).slice(2, 8)}`;
  const execute = async (): Promise<CampaignState> => {
    const previous = await getStoreItem<CampaignState>('campaigns', input.campaign.id).catch(() => undefined);
    if (input.turn) {
      const duplicate = (await getStoreData<any>('turn_transactions').catch(() => []))
        .find(row => row.campaignId === input.campaign.id && row.idempotencyKey === input.turn!.idempotencyKey && row.status === 'committed');
      if (duplicate && previous) return previous;
    }
    if (previous?.history?.length && input.campaign.history.length < previous.history.length && !/restaur|migra|recuper/i.test(input.reason)) {
      throw new Error('Proteção do banco: a transação tentou reduzir o histórico da campanha.');
    }
    if (previous?.characterSheet?.identity?.name && !input.campaign.characterSheet?.identity?.name) {
      throw new Error('Proteção do banco: uma ficha vazia tentou substituir a ficha existente.');
    }
    if (previous) {
      // Mantém uma cópia síncrona exclusivamente da última revisão confirmada.
      // Uma proposta ainda não commitada jamais pode virar fonte de recuperação.
      saveEmergencyCampaignMirror(previous);
      try { await createCampaignSnapshot(previous, `antes de ${input.reason}`); }
      catch (error) { console.warn('[BRASEIRO_DB] snapshot preventivo:', error); }
    }
    const { campaign: _campaignWithImages, ...outboxRest } = input;
    const outboxInput: CanonicalTransactionInput = {
      ...structuredClone(outboxRest),
      campaign: cloneCampaignWithoutImages(input.campaign),
    };
    const outbox: TransactionOutboxRecord = {
      id: transactionId,
      campaignId: input.campaign.id,
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      attempts: 0,
      input: { ...outboxInput, transactionId },
    };
    await putRaw('transaction_outbox', outbox);

    const now = Date.now();
    const campaign: CampaignState = {
      ...input.campaign,
      history: input.campaign.history.map(scene => ({ ...scene })),
      schemaVersion: Math.max(350, Number(input.campaign.schemaVersion || 0)),
      revision: Math.max(Number(previous?.revision || 0) + 1, Number(input.campaign.revision || 0) + 1),
      lastTransactionId: transactionId,
      updatedAt: Math.max(now, Number(input.campaign.updatedAt || 0)),
    };
    const canonicalState = await buildCanonicalStateSnapshot(campaign, input, now);
    const checkpoint = {
      id: `checkpoint:${campaign.id}:${campaign.revision}`,
      campaignId: campaign.id,
      eventType: 'CAMPAIGN_CHECKPOINT',
      entityType: 'canonical-state',
      entityId: campaign.id,
      payload: { revision: campaign.revision, transactionId, campaign: canonicalState.campaign, state: canonicalState },
      idempotencyKey: transactionId,
      createdAt: now,
      applied: true,
    };
    const stores = new Set<string>(['campaigns','app_meta','event_journal','transaction_outbox']);
    if (input.characters?.length || input.deleteCharacterIds?.length) stores.add('characters');
    if (input.npcs?.length || input.deleteNpcIds?.length) stores.add('npcs');
    if (campaign.masterState) { stores.add('master_states'); stores.add('knowledge_ledger'); stores.add('actor_memories'); }
    if (input.audits?.length) { stores.add('command_log'); stores.add('rule_decisions'); }
    if (input.mapDiscovery) stores.add('map_discovery');
    if (input.environmentState) stores.add('environment_states');
    if (input.environmentMemory?.length) stores.add('environment_memory');
    if (input.autonomousEvents?.length) stores.add('autonomous_events');
    if (input.campaignEvents?.length) stores.add('campaign_events');
    if (input.turn) stores.add('turn_transactions');

    try {
      await withDbRetry(async db => {
        const tx = db.transaction(Array.from(stores), 'readwrite');
        tx.objectStore('campaigns').put(campaign);
        tx.objectStore('app_meta').put({ id: 'activeCampaignId', value: campaign.id, updatedAt: now });
        tx.objectStore('event_journal').put(checkpoint);
        for (const entry of input.journalEntries || []) tx.objectStore('event_journal').put(entry);
        for (const event of input.campaignEvents || []) {
          tx.objectStore('campaign_events').put(event);
          tx.objectStore('event_journal').put({
            id: `campaign-event:${campaign.id}:${event.id}`, campaignId: campaign.id, turnId: input.turn?.id,
            eventType: String(event.type || 'event').toUpperCase(), entityType: event.type === 'location_changed' ? 'map' : event.type === 'npc_encountered' ? 'npc' : 'campaign',
            entityId: event.type === 'npc_encountered' ? String(event.data?.npcId || '') : campaign.id,
            payload: { sceneId: event.sceneId || null, ...(event.data || {}) }, createdAt: event.timestamp || now, applied: true,
            idempotencyKey: input.turn?.idempotencyKey,
          });
        }
        for (const characterId of input.deleteCharacterIds || []) tx.objectStore('characters').delete(characterId);
        for (const character of input.characters || []) tx.objectStore('characters').put(character);
        for (const npcId of input.deleteNpcIds || []) tx.objectStore('npcs').delete(npcId);
        for (const npc of input.npcs || []) tx.objectStore('npcs').put(npc);
        if (input.mapDiscovery) tx.objectStore('map_discovery').put(input.mapDiscovery);
        if (input.environmentState) tx.objectStore('environment_states').put(input.environmentState);
        for (const row of input.environmentMemory || []) tx.objectStore('environment_memory').put(row);
        for (const row of input.autonomousEvents || []) tx.objectStore('autonomous_events').put(row);
        if (campaign.masterState) {
          tx.objectStore('master_states').put({ id: campaign.id, campaignId: campaign.id, ...campaign.masterState });
          for (const entry of campaign.masterState.knowledge || []) tx.objectStore('knowledge_ledger').put({ ...entry, id: `${campaign.id}:${entry.id}`, entryId: entry.id, campaignId: campaign.id });
          for (const actor of campaign.masterState.npcActors || []) tx.objectStore('actor_memories').put({ ...actor, id: `${campaign.id}:${actor.npcId}`, campaignId: campaign.id });
        }
        for (const audit of input.audits || []) {
          tx.objectStore('command_log').put(audit);
          if (audit.command && 'source' in audit.command && audit.command.source) {
            tx.objectStore('rule_decisions').put({ id: `${campaign.id}:${audit.id}`, campaignId: campaign.id, commandId: audit.id, source: audit.command.source, command: audit.command, timestamp: audit.timestamp });
          }
        }
        if (input.turn) {
          tx.objectStore('turn_transactions').put({
            id: input.turn.id,
            idempotencyKey: input.turn.idempotencyKey,
            campaignId: campaign.id,
            action: '',
            status: 'committed',
            committedSceneId: input.turn.sceneId,
            resultDigest: input.turn.resultDigest,
            createdAt: now,
            updatedAt: now,
          });
          tx.objectStore('event_journal').put({
            id: `${input.turn.id}:committed`, campaignId: campaign.id, turnId: input.turn.id,
            eventType: 'TURN_COMMITTED', entityType: 'campaign', entityId: campaign.id,
            payload: { sceneId: input.turn.sceneId || null, resultDigest: input.turn.resultDigest || '', ...(input.turn.payload || {}) },
            idempotencyKey: input.turn.idempotencyKey, createdAt: now, applied: true,
          });
        }
        tx.objectStore('transaction_outbox').delete(transactionId);
        await transactionDone(tx);
      });
    } catch (error) {
      await putRaw('transaction_outbox', { ...outbox, status: 'failed', attempts: outbox.attempts + 1, updatedAt: Date.now(), error: error instanceof Error ? error.message : String(error) }).catch(() => undefined);
      throw error;
    }
    saveEmergencyCampaignMirror(campaign);
    try {
      window.BraseiroAndroid?.saveRecoveryBackup?.(campaign.id, JSON.stringify(canonicalState));
    } catch (error) {
      console.warn('[BRASEIRO_DB] backup redundante Android não pôde ser atualizado:', error);
    }
    window.dispatchEvent(new CustomEvent('braseiro-db-write', { detail: { storeName: 'campaigns', id: campaign.id, transactionId } }));
    void pruneCampaignCheckpoints(campaign.id);
    return campaign;
  };
  const queued = writeQueue.then(execute, execute);
  writeQueue = queued.catch(() => undefined);
  return queued;
}

export async function recoverPendingCanonicalTransactions(): Promise<number> {
  const rows = (await getStoreData<TransactionOutboxRecord>('transaction_outbox').catch(() => []))
    .sort((a, b) => a.createdAt - b.createdAt);
  let recovered = 0;
  for (const row of rows) {
    try {
      await saveCanonicalTransaction({ ...row.input, transactionId: row.id });
      recovered += 1;
    } catch (error) {
      console.warn('[BRASEIRO_DB] transação pendente não pôde ser recuperada:', row.id, error);
    }
  }
  return recovered;
}

export async function saveMasterTransaction(
  campaign: CampaignState,
  characters: Character[],
  audits: CommandAuditEntry[],
  reason = 'transação do Mestre',
  npcs: Npc[] = [],
  mapDiscovery?: { id: string; campaignId: string; mapId: string; currentHexId?: string | null },
): Promise<CampaignState> {
  return saveCanonicalTransaction({ campaign, characters, audits, reason, npcs, mapDiscovery });
}

export async function getActiveCampaign(): Promise<CampaignState | null> {
  const mirror = readEmergencyCampaignMirror();
  let campaigns: CampaignState[] = [];
  try { campaigns = await getStoreData<CampaignState>('campaigns'); }
  catch (error) {
    console.error('[BRASEIRO_DB] leitura principal falhou; usando espelho de emergência:', error);
    return mirror;
  }
  if (!campaigns.length) {
    if (mirror) {
      await putRaw('campaigns', mirror).catch(error => console.warn('[BRASEIRO_DB] restauração do espelho falhou:', error));
      return mirror;
    }
    return null;
  }
  const activeId = await getAppMeta<string>('activeCampaignId').catch(() => undefined);
  const active = campaigns.find(item => item.id === activeId)
    || campaigns.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))[0]
    || null;
  const mirrorIsNewer = Boolean(active && mirror?.id === active.id && (
    Number(mirror.revision || 0) > Number(active.revision || 0)
    || (Number(mirror.revision || 0) === Number(active.revision || 0) && Number(mirror.updatedAt || 0) > Number(active.updatedAt || 0))
    || (Number(mirror.revision || 0) === Number(active.revision || 0) && mirror.history.length > active.history.length)
    || (Number(mirror.revision || 0) === Number(active.revision || 0) && (mirror.events?.length || 0) > (active.events?.length || 0))
    || (Boolean(mirror.lastTransactionId) && !active.lastTransactionId)
  ));
  if (active && mirror && mirrorIsNewer) {
    console.warn('[BRASEIRO_DB] banco principal estava atrás do espelho; recuperando a revisão canônica mais recente.');
    await putRaw('campaigns', mirror).catch(() => undefined);
    return mirror;
  }
  return active;
}

export const getCharacters = async (): Promise<Character[]> => getStoreData<Character>('characters');
export const saveCharacter = async (character: Character): Promise<void> => saveStoreData('characters', character);
export const deleteCharacter = async (id: string): Promise<void> => deleteStoreData('characters', id);

function normalizeSearch(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

const SEARCH_STOP_WORDS = new Set([
  'para','com','uma','uns','umas','que','por','como','seu','sua','seus','suas','dos','das','de','do','da','em','no','na','nos','nas','ao','aos','pela','pelo','pelos','pelas','este','esta','isso','isto','ele','ela','eles','elas','mais','menos','muito','muita','entre','sobre','sem','quando','onde','qual','quais','agora','depois','antes','ainda','jogador','personagem','acao','ação','cena','atual'
]);

export async function searchChunks(keywords: string[], systemScopes: string[] = [], authorityMode: 'mechanics' | 'narrative' = 'mechanics'): Promise<ManualChunk[]> {
  const allChunks = await getStoreData<ManualChunk>('manual_chunks');
  if (!allChunks.length) return [];
  const manuals = await getStoreData<Manual>('manuals');
  const normalizedScopes = Array.from(new Set(systemScopes.map(normalizeSearch).map(value => value.trim()).filter(Boolean)));
  const mechanicalTorScope = normalizedScopes.some(scope => /one-ring|um-anel|the-one-ring|tor2e/.test(scope));
  const allowedManualIds = normalizedScopes.length
    ? new Set(manuals.filter(manual => {
        if (!manual.indexed || (manual.indexState !== undefined && manual.indexState !== 'ready') || Number(manual.chunkCount || 0) <= 0) return false;
        if (mechanicalTorScope && !(authorityMode === 'mechanics' ? mayProvideMechanics(manual) : mayProvideNarrativeContext(manual))) return false;
        const candidates = [manual.system, manual.profileId, manual.systemProfileId, manual.settingProfileId]
          .map(value => normalizeSearch(value || '').trim()).filter(Boolean);
        if (!candidates.length) return false;
        return normalizedScopes.some(scope => candidates.some(candidate => scope === candidate || scope.includes(candidate) || candidate.includes(scope)));
      }).map(manual => manual.id))
    : new Set(manuals.filter(manual => manual.indexed && (manual.indexState === undefined || manual.indexState === 'ready') && Number(manual.chunkCount || 0) > 0).map(manual => manual.id));
  // Quando um perfil foi informado, nunca caia silenciosamente em manuais de
  // outro sistema. Ausência de fonte correta resulta em ausência de regra.
  const chunks = allChunks.filter(chunk => allowedManualIds.has(chunk.manualId));
  if (!chunks.length) return [];
  const normalizedQueries = Array.from(new Set(keywords.map(normalizeSearch).map(value => value.trim()).filter(value => value.length > 2)));
  const terms = Array.from(new Set(normalizedQueries
    .flatMap(value => value.split(/[^a-z0-9]+/i))
    .filter(value => value.length > 2 && !SEARCH_STOP_WORDS.has(value))))
    .slice(0, 90);
  if (!terms.length && !normalizedQueries.length) return [];

  const priorityMap = new Map(manuals.map(item => [item.id, item.priority || 4]));
  type SearchIndexRecord = {
    id: string; manualId: string; manualName: string; pageNum: number; chunkIndex: number;
    tokenCount: number; terms: Record<string, number>; updatedAt: number;
  };
  const indexRows: SearchIndexRecord[] = await getStoreData<SearchIndexRecord>('manual_search_index').catch(() => [] as SearchIndexRecord[]);
  const chunkById = new Map(chunks.map(chunk => [chunk.id, chunk]));

  let ranked: Array<{ chunk: ManualChunk; score: number }> = [];
  if (indexRows.length >= Math.max(1, Math.floor(chunks.length * 0.7))) {
    const documentCount = indexRows.length;
    const avgLength = indexRows.reduce<number>((sum, row) => sum + Math.max(1, row.tokenCount || 1), 0) / documentCount;
    const documentFrequency = new Map<string, number>();
    for (const term of terms) documentFrequency.set(term, indexRows.reduce<number>((count, row) => count + (row.terms?.[term] ? 1 : 0), 0));
    const k1 = 1.35;
    const b = 0.72;
    ranked = indexRows.flatMap(row => {
      const chunk = chunkById.get(row.id);
      if (!chunk) return [];
      let score = 0;
      for (const term of terms) {
        const tf = row.terms?.[term] || 0;
        if (!tf) continue;
        const df = documentFrequency.get(term) || 0;
        const idf = Math.log(1 + (documentCount - df + 0.5) / (df + 0.5));
        const norm = tf + k1 * (1 - b + b * Math.max(1, row.tokenCount) / Math.max(1, avgLength));
        score += idf * (tf * (k1 + 1)) / norm;
      }
      const normalizedText = normalizeSearch(chunk.text);
      for (const query of normalizedQueries) {
        if (query.includes(' ') && normalizedText.includes(query)) score += 5 + Math.min(8, query.length / 10);
      }
      const priority = priorityMap.get(chunk.manualId) || 4;
      score *= 1 + Math.max(0, 5 - priority) * 0.16;
      return score > 0 ? [{ chunk, score }] : [];
    }).sort((a, b) => b.score - a.score);
  }

  // Fallback compatível com bancos anteriores: a primeira busca ainda funciona
  // enquanto o supervisor reconstrói o índice persistente em segundo plano.
  if (!ranked.length) {
    ranked = chunks.map(chunk => {
      const text = normalizeSearch(chunk.text);
      let score = 0;
      for (const query of normalizedQueries) {
        if (query.includes(' ') && text.includes(query)) score += 55 + Math.min(40, query.length / 2);
      }
      for (const term of terms) {
        const occurrences = text.split(term).length - 1;
        if (occurrences) score += Math.min(occurrences, 8) * 6;
        const regex = new RegExp(`\\b${escapeRegExp(term)}\\b`, 'g');
        score += (text.match(regex) || []).length * 10;
        if (/jornada|movimento|combate|conselho|descanso|teste|rolagem|fadiga|carga|bloqueio|parada|postura|numero|alvo|esperanca|sombra/.test(term)) score += occurrences * 10;
      }
      const priority = priorityMap.get(chunk.manualId) || 4;
      score *= priority === 1 ? 1.8 : priority === 2 ? 1.5 : priority === 3 ? 1.25 : 1;
      return { chunk, score };
    }).filter(item => item.score > 0).sort((a, b) => b.score - a.score);
  }

  const selected: ManualChunk[] = [];
  const pageKeys = new Set<string>();
  const manualCounts = new Map<string, number>();
  for (const item of ranked) {
    const pageKey = `${item.chunk.manualId}:${item.chunk.pageNum}`;
    const manualCount = manualCounts.get(item.chunk.manualId) || 0;
    if ((pageKeys.has(pageKey) || manualCount >= 4) && selected.length >= 5) continue;
    selected.push(item.chunk);
    pageKeys.add(pageKey);
    manualCounts.set(item.chunk.manualId, manualCount + 1);
    if (selected.length >= 12) break;
  }
  return selected;
}

export async function saveSoundBlob(id: string, blob: Blob): Promise<void> {
  await withDbRetry(async db => {
    const tx = db.transaction('sound_blobs', 'readwrite');
    tx.objectStore('sound_blobs').put(blob, id);
    await transactionDone(tx);
  });
}

export async function getSoundBlob(id: string): Promise<Blob | undefined> {
  return withDbRetry(async db => {
    const tx = db.transaction('sound_blobs', 'readonly');
    const result = await requestPromise(tx.objectStore('sound_blobs').get(id));
    await transactionDone(tx);
    return result as Blob | undefined;
  });
}

export async function listSoundMetadata(): Promise<SoundItem[]> {
  const modern = await getStoreData<SoundItem>('sound_metadata');
  if (modern.length) return modern;
  const legacy = await getStoreData<SoundItem>('sounds');
  if (!legacy.length) return [];
  for (const item of legacy) {
    if (item.blob) await saveSoundBlob(item.id, item.blob);
    const { blob: _blob, ...metadata } = item;
    await putRaw('sound_metadata', { ...metadata, source: 'indexeddb' });
  }
  return getStoreData<SoundItem>('sound_metadata');
}

export async function saveSoundItem(item: SoundItem, blob?: Blob): Promise<void> {
  const actualBlob = blob || item.blob;
  if (actualBlob) await saveSoundBlob(item.id, actualBlob);
  const { blob: _blob, ...metadata } = item;
  await saveStoreData('sound_metadata', { ...metadata, source: item.source || 'indexeddb' });
}

export async function deleteSoundItem(id: string): Promise<void> {
  await deleteStoreData('sound_metadata', id, false);
  await withDbRetry(async db => {
    const tx = db.transaction('sound_blobs', 'readwrite');
    tx.objectStore('sound_blobs').delete(id);
    await transactionDone(tx);
  });
}


interface TtsCacheRecord { audio: string; createdAt: number; lastUsedAt: number }

export async function getTtsCache(key: string): Promise<string | null> {
  return withDbRetry(async db => {
    const tx = db.transaction('tts_cache', 'readonly');
    const row = await requestPromise(tx.objectStore('tts_cache').get(key)) as TtsCacheRecord | undefined;
    await transactionDone(tx);
    return row?.audio || null;
  }).catch(() => null);
}

export async function setTtsCache(key: string, audio: string): Promise<void> {
  await withDbRetry(async db => {
    const tx = db.transaction('tts_cache', 'readwrite');
    tx.objectStore('tts_cache').put({ audio, createdAt: Date.now(), lastUsedAt: Date.now() } satisfies TtsCacheRecord, key);
    await transactionDone(tx);
  });
  // Keep cache bounded; old records are dispensable and never affect campaign data.
  await withDbRetry(async db => {
    const tx = db.transaction('tts_cache', 'readwrite');
    const store = tx.objectStore('tts_cache');
    const keys = await requestPromise(store.getAllKeys());
    if (keys.length > 72) {
      for (const keyToDelete of keys.slice(0, keys.length - 72)) store.delete(keyToDelete);
    }
    await transactionDone(tx);
  }).catch(() => undefined);
}
