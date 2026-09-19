import JSZip from 'jszip';
import {
  type CampaignState, type CanonicalStateSnapshot, type Character, type IndividualToken, type Manual, type ManualChunk, type Npc, type RollItem, type SoundItem, type TokenSheet,
  applyStoreChangesAtomically, createCampaignSnapshot, getStoreData, listSoundMetadata, saveEmergencyCampaignMirror,
} from './db';
import type { EriadorMapManifest, MapDiscovery } from './map';
import { resolveRulesProfile } from './autonomy/rulesProfile';
import { APP_VERSION } from './version';
import { assertCanonicalCampaignEquivalent } from './backupEquivalence';

export const BACKUP_VERSION = 7;
export const BACKUP_FORMAT = 'braseiro-rpg';

type Row = { id: string; campaignId?: string; profileId?: string; rulesProfileId?: string; systemScope?: string; [key: string]: unknown };

export interface BackupIntegrity {
  algorithm: 'SHA-256';
  checksum: string;
  recordCounts: Record<string, number>;
  campaignId: string;
  profileId: string;
}

export interface BackupData {
  format: 'braseiro-rpg' | 'braseiro-oumanel';
  backupVersion: number;
  exportedAt: number;
  appVersion: string;
  campaign: CampaignState;
  profile: { id: string; systemId: string; settingId: string; sourceFolderId?: string };
  characters: Character[];
  npcs: Npc[];
  rolls: RollItem[];
  manuals: Manual[];
  manualChunks: ManualChunk[];
  tokenSheets: TokenSheet[];
  tokensMeta: Array<Omit<IndividualToken, 'blob'>>;
  soundsMeta: SoundItem[];
  mapManifests: EriadorMapManifest[];
  mapDiscoveries: MapDiscovery[];
  commandLog: Row[];
  knowledgeLedger: Row[];
  actorMemories: Row[];
  ruleDecisions: Row[];
  soundKeywordRules: Row[];
  npcTokenCatalog: Row[];
  npcTokenImportState: Row[];
  migrationReports: Row[];
  homologationReports: Row[];
  eventJournal: Row[];
  turnTransactions: Row[];
  assetRegistry: Row[];
  healthReports: Row[];
  manualSearchIndex: Row[];
  environmentStates: Row[];
  autonomousEvents: Row[];
  environmentMemory: Row[];
  rulesFolderManifests: Row[];
  notes: string[];
  integrity?: BackupIntegrity;
}

const CAMPAIGN_ARRAYS: Array<[keyof BackupData, string]> = [
  ['commandLog','command_log'], ['knowledgeLedger','knowledge_ledger'], ['actorMemories','actor_memories'], ['ruleDecisions','rule_decisions'],
  ['migrationReports','migration_reports'], ['homologationReports','homologation_reports'], ['eventJournal','event_journal'], ['turnTransactions','turn_transactions'],
  ['healthReports','health_reports'], ['environmentStates','environment_states'], ['autonomousEvents','autonomous_events'], ['environmentMemory','environment_memory'],
];

function stripSoundBinary(sound: SoundItem): SoundItem { const { blob: _blob, ...metadata } = sound; return metadata; }
function unique<T extends { id: string }>(rows: T[]): T[] { return Array.from(new Map(rows.map(row => [row.id, row])).values()); }
function scoped(rows: Row[], campaignId: string): Row[] { return rows.filter(row => row.campaignId === campaignId || row.id.startsWith(`${campaignId}:`) || row.id.includes(`:${campaignId}:`)); }

function tokenReferences(campaign: CampaignState, characters: Character[], npcs: Npc[]): Set<string> {
  const ids = new Set<string>();
  if (campaign.characterSheet.tokenId) ids.add(campaign.characterSheet.tokenId);
  for (const character of characters) if (character.sheet.tokenId) ids.add(character.sheet.tokenId);
  for (const npc of npcs) if (npc.tokenId) ids.add(npc.tokenId);
  return ids;
}

async function digestText(value: string): Promise<string> {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest)).map(byte => byte.toString(16).padStart(2, '0')).join('');
}

function dataWithoutIntegrity(data: BackupData): Omit<BackupData, 'integrity'> { const { integrity: _integrity, ...rest } = data; return rest; }
function recordCounts(data: Omit<BackupData, 'integrity'>): Record<string, number> {
  const result: Record<string, number> = {};
  for (const [key, value] of Object.entries(data)) if (Array.isArray(value)) result[key] = value.length;
  return result;
}

async function collectBackupData(campaign: CampaignState): Promise<BackupData> {
  const profile = resolveRulesProfile(campaign);
  const allCharacters = await getStoreData<Character>('characters');
  const characterIds = new Set([campaign.activeCharacterId, ...(campaign.partyCharacterIds || [])].filter(Boolean) as string[]);
  const characters = allCharacters.filter(character => characterIds.has(character.id));
  const npcIds = new Set<string>([
    ...(campaign.presentNpcIds || []),
    ...(campaign.history || []).flatMap(scene => scene.presentNpcIds || []),
    ...(campaign.masterState?.npcActors || []).map(actor => actor.npcId),
  ]);
  const allNpcs = await getStoreData<Npc>('npcs');
  const npcs = allNpcs.filter(npc => npcIds.has(npc.id));
  const allManuals = await getStoreData<Manual>('manuals');
  const manuals = allManuals.filter(manual => manual.profileId === profile.id || manual.rulesSourceFolderId === profile.sourceFolderId || (!manual.profileId && manual.system === profile.displayName));
  const manualIds = new Set(manuals.map(manual => manual.id));
  const manualChunks = (await getStoreData<ManualChunk>('manual_chunks')).filter(chunk => manualIds.has(chunk.manualId));
  const manualSearchIndex = (await getStoreData<Row>('manual_search_index')).filter(row => manualIds.has(String(row.manualId || '')));
  const allTokens = await getStoreData<IndividualToken>('individual_tokens');
  const tokenIds = tokenReferences(campaign, characters, npcs);
  const tokens = allTokens.filter(token => tokenIds.has(token.id));
  const sheetIds = new Set(tokens.map(token => token.sheetId));
  const tokenSheets = (await getStoreData<TokenSheet>('token_sheets')).filter(sheet => sheetIds.has(sheet.id));
  const [rolls, sounds, mapManifests, allDiscoveries, soundKeywordRules, npcTokenCatalog, npcTokenImportState, assetRegistry, rulesFolderManifests] = await Promise.all([
    getStoreData<RollItem>('rolls'), listSoundMetadata(), getStoreData<EriadorMapManifest>('map_manifests'), getStoreData<MapDiscovery>('map_discovery'),
    getStoreData<Row>('sound_keyword_rules'), getStoreData<Row>('npc_token_catalog'), getStoreData<Row>('npc_token_import_state'), getStoreData<Row>('asset_registry'), getStoreData<Row>('rules_folder_manifests'),
  ]);
  const campaignRows: Record<string, Row[]> = {};
  for (const [backupKey, storeName] of CAMPAIGN_ARRAYS) campaignRows[String(backupKey)] = scoped(await getStoreData<Row>(storeName), campaign.id);
  const base: Omit<BackupData, 'integrity'> = {
    format: BACKUP_FORMAT, backupVersion: BACKUP_VERSION, exportedAt: Date.now(), appVersion: APP_VERSION,
    campaign: structuredClone(campaign), profile: { id: profile.id, systemId: profile.systemId, settingId: profile.settingId, sourceFolderId: profile.sourceFolderId },
    characters, npcs, rolls: rolls.slice(-400), manuals, manualChunks, tokenSheets, tokensMeta: tokens.map(({ blob: _blob, ...metadata }) => metadata),
    soundsMeta: sounds.map(stripSoundBinary), mapManifests, mapDiscoveries: allDiscoveries.filter(row => row.campaignId === campaign.id),
    commandLog: campaignRows.commandLog, knowledgeLedger: campaignRows.knowledgeLedger, actorMemories: campaignRows.actorMemories,
    ruleDecisions: campaignRows.ruleDecisions, soundKeywordRules, npcTokenCatalog, npcTokenImportState,
    migrationReports: campaignRows.migrationReports, homologationReports: campaignRows.homologationReports, eventJournal: campaignRows.eventJournal,
    turnTransactions: campaignRows.turnTransactions, assetRegistry: assetRegistry.filter(row => row.campaignId === campaign.id || row.profileId === profile.id || !row.campaignId),
    healthReports: campaignRows.healthReports, manualSearchIndex, environmentStates: campaignRows.environmentStates,
    autonomousEvents: campaignRows.autonomousEvents, environmentMemory: campaignRows.environmentMemory,
    rulesFolderManifests: rulesFolderManifests.filter(row => row.profileId === profile.id || row.id === `rules-folder:${profile.id}`),
    notes: [
      'Backup isolado por campanha e perfil de regras; outras campanhas não são apagadas na restauração.',
      'Áudios nativos grandes não são duplicados; metadados e regras de classificação são restaurados por mesclagem.',
      'Tokens com blob são incluídos somente no ZIP completo.',
    ],
  };
  const checksum = await digestText(JSON.stringify(base));
  return { ...base, integrity: { algorithm: 'SHA-256', checksum, recordCounts: recordCounts(base), campaignId: campaign.id, profileId: profile.id } };
}

async function validateBackup(input: unknown): Promise<BackupData> {
  if (!input || typeof input !== 'object') throw new Error('O arquivo não contém um backup válido.');
  const data = input as BackupData;
  if (![BACKUP_FORMAT, 'braseiro-oumanel'].includes(String(data.format))) throw new Error('Formato de backup desconhecido.');
  if (!Array.from({ length: BACKUP_VERSION - 1 }, (_, index) => index + 2).includes(Number(data.backupVersion))) throw new Error(`Versão de backup incompatível: ${String(data.backupVersion)}.`);
  if (!data.campaign?.id || !data.campaign.characterSheet || !Array.isArray(data.campaign.history)) throw new Error('A campanha do backup está incompleta. Nenhum dado atual foi alterado.');
  const arrays = ['characters','npcs','manuals','manualChunks','mapDiscoveries','tokensMeta'] as const;
  for (const key of arrays) if (!Array.isArray((data as any)[key])) throw new Error(`Backup inválido: ${key} não é uma lista.`);
  const duplicateCheck = (name: string, rows: Array<{ id?: string }>) => {
    const ids = rows.map(row => row.id).filter(Boolean) as string[];
    if (new Set(ids).size !== ids.length) throw new Error(`Backup inválido: IDs duplicados em ${name}.`);
  };
  duplicateCheck('characters', data.characters); duplicateCheck('npcs', data.npcs); duplicateCheck('manuals', data.manuals); duplicateCheck('manualChunks', data.manualChunks);
  const manualIds = new Set(data.manuals.map(row => row.id));
  if (data.manualChunks.some(row => !manualIds.has(row.manualId))) throw new Error('Backup inválido: há trechos sem manual correspondente.');
  const characterIds = new Set(data.characters.map(row => row.id));
  const requiredCharacterIds = new Set([data.campaign.activeCharacterId, ...(data.campaign.partyCharacterIds || [])].filter(Boolean) as string[]);
  for (const id of requiredCharacterIds) if (!characterIds.has(id)) throw new Error(`Backup inválido: o herói ${id} é referenciado pela campanha, mas não está no backup.`);
  const npcIds = new Set(data.npcs.map(row => row.id));
  const requiredNpcIds = new Set<string>([
    ...(data.campaign.presentNpcIds || []),
    ...(data.campaign.history || []).flatMap(scene => scene.presentNpcIds || []),
    ...(data.campaign.masterState?.npcActors || []).map(actor => actor.npcId),
  ]);
  for (const id of requiredNpcIds) if (!npcIds.has(id)) throw new Error(`Backup inválido: o NPC ${id} é referenciado pela campanha, mas não está no backup.`);
  const tokenIds = new Set([...(data.tokensMeta || []).map(row => row.id), ...(data.npcTokenCatalog || []).map(row => row.id)]);
  for (const id of tokenReferences(data.campaign, data.characters, data.npcs)) if (!tokenIds.has(id)) throw new Error(`Backup inválido: o token ${id} é referenciado, mas não possui metadados.`);
  if (data.profile?.id && data.integrity?.profileId && data.profile.id !== data.integrity.profileId) throw new Error('Backup inválido: o perfil não confere com o manifesto de integridade.');
  if (Number(data.backupVersion) >= 7) {
    const resolvedProfile = resolveRulesProfile(data.campaign);
    if (!data.profile?.id || resolvedProfile.id !== data.profile.id) throw new Error('Backup inválido: o perfil declarado não corresponde ao sistema/cenário da campanha.');
  }
  if (data.integrity?.campaignId && data.integrity.campaignId !== data.campaign.id) throw new Error('Backup inválido: o ID da campanha não confere com o manifesto de integridade.');
  if (data.mapDiscoveries.some(row => row.campaignId !== data.campaign.id)) throw new Error('Backup inválido: contém mapa de outra campanha.');
  for (const [key] of CAMPAIGN_ARRAYS) {
    for (const row of ((data as any)[key] || []) as Row[]) if (row.campaignId && row.campaignId !== data.campaign.id) throw new Error(`Backup inválido: ${String(key)} contém outra campanha.`);
  }
  if (data.integrity?.recordCounts) {
    const actualCounts = recordCounts(dataWithoutIntegrity(data));
    for (const [key, expected] of Object.entries(data.integrity.recordCounts)) {
      if (Number(actualCounts[key] || 0) !== Number(expected)) throw new Error(`Backup inválido: contagem de ${key} não confere.`);
    }
  }
  if (data.integrity?.checksum) {
    const calculated = await digestText(JSON.stringify(dataWithoutIntegrity(data)));
    if (calculated !== data.integrity.checksum) throw new Error('Checksum do backup não confere. Nenhum dado atual foi alterado.');
  } else if (Number(data.backupVersion) >= 7) throw new Error('Backup v7 sem checksum de integridade.');
  return data;
}

export async function generateQuickBackup(campaign: CampaignState): Promise<string> { return JSON.stringify(await collectBackupData(campaign), null, 2); }

export async function generateCompleteBackup(campaign: CampaignState): Promise<Blob> {
  const data = await collectBackupData(campaign);
  const zip = new JSZip();
  zip.file('backup.json', JSON.stringify(data, null, 2));
  const tokenFolder = zip.folder('tokens');
  const wanted = new Set(data.tokensMeta.map(row => row.id));
  for (const token of (await getStoreData<IndividualToken>('individual_tokens')).filter(row => wanted.has(row.id))) tokenFolder?.file(`${token.id}.png`, token.blob);
  return zip.generateAsync({ type: 'blob', compression: 'DEFLATE', compressionOptions: { level: 5 }, streamFiles: true });
}

async function idsForCampaign(storeName: string, campaignId: string): Promise<string[]> {
  return scoped(await getStoreData<Row>(storeName).catch(() => []), campaignId).map(row => row.id);
}

async function applyBackup(data: BackupData, tokenBlobs?: IndividualToken[]): Promise<CampaignState> {
  const valid = await validateBackup(data);
  const campaigns = await getStoreData<CampaignState>('campaigns').catch(() => []);
  const current = campaigns.find(row => row.id === valid.campaign.id);
  if (current) await createCampaignSnapshot(current, 'antes de restaurar backup 3.5');
  const stagingId = `restore:${valid.campaign.id}:${Date.now()}`;
  const transactionId = `backup-restore:${valid.campaign.id}:${Date.now()}`;
  const locallyAvailableTokens = tokenBlobs
    ? new Set(tokenBlobs.map(row => row.id))
    : new Set((await getStoreData<IndividualToken>('individual_tokens').catch(() => [])).map(row => row.id));
  const sanitizeSheet = (sheet: Character['sheet']): Character['sheet'] => ({
    ...structuredClone(sheet),
    tokenId: sheet.tokenId && locallyAvailableTokens.has(sheet.tokenId) ? sheet.tokenId : null,
  });
  const restoredCharacters = valid.characters.map(character => ({ ...structuredClone(character), sheet: sanitizeSheet(character.sheet) }));
  const restoredNpcs = valid.npcs.map(npc => ({ ...structuredClone(npc), tokenId: npc.tokenId && locallyAvailableTokens.has(npc.tokenId) ? npc.tokenId : null }));
  const restoredCampaignBase = structuredClone(valid.campaign);
  const activeRestoredCharacter = restoredCharacters.find(row => row.id === restoredCampaignBase.activeCharacterId);
  restoredCampaignBase.characterSheet = activeRestoredCharacter?.sheet || sanitizeSheet(restoredCampaignBase.characterSheet);
  const persisted: CampaignState = {
    ...restoredCampaignBase,
    schemaVersion: Math.max(350, Number(valid.campaign.schemaVersion || 0)),
    revision: Math.max(Number(current?.revision || 0) + 1, Number(valid.campaign.revision || 0) + 1, 1),
    lastTransactionId: transactionId,
    updatedAt: Date.now(),
  };
  // Gate de restauração: antes de tocar no banco, comprove que apenas campos
  // voláteis de revisão/migração mudaram. Isso torna o round-trip um invariante
  // executável da própria implementação, não apenas um teste externo.
  assertCanonicalCampaignEquivalent(restoredCampaignBase, persisted);
  const currentCharacterIds = new Set([current?.activeCharacterId, ...(current?.partyCharacterIds || [])].filter(Boolean) as string[]);
  const currentNpcIds = new Set<string>([
    ...(current?.presentNpcIds || []),
    ...(current?.history || []).flatMap(scene => scene.presentNpcIds || []),
    ...(current?.masterState?.npcActors || []).map(actor => actor.npcId),
  ]);
  const allCurrentCharacters = await getStoreData<Character>('characters').catch(() => []);
  const allCurrentNpcs = await getStoreData<Npc>('npcs').catch(() => []);
  const currentReferencedTokens = current
    ? tokenReferences(current, allCurrentCharacters.filter(row => currentCharacterIds.has(row.id)), allCurrentNpcs.filter(row => currentNpcIds.has(row.id)))
    : new Set<string>();

  const [existingManuals, existingChunks, existingSearchRows, existingAssets, existingRuleManifests] = await Promise.all([
    getStoreData<Manual>('manuals').catch(() => []),
    getStoreData<ManualChunk>('manual_chunks').catch(() => []),
    getStoreData<Row>('manual_search_index').catch(() => []),
    getStoreData<Row>('asset_registry').catch(() => []),
    getStoreData<Row>('rules_folder_manifests').catch(() => []),
  ]);
  const replacedManualIds = new Set(existingManuals
    .filter(row => row.profileId === valid.profile.id || (valid.profile.sourceFolderId && row.rulesSourceFolderId === valid.profile.sourceFolderId))
    .map(row => row.id));
  const deleteChunkIds = existingChunks.filter(row => replacedManualIds.has(row.manualId)).map(row => row.id);
  const deleteSearchIds = existingSearchRows.filter(row => replacedManualIds.has(String(row.manualId || ''))).map(row => row.id);
  const deleteAssetIds = existingAssets
    .filter(row => row.campaignId === valid.campaign.id || row.profileId === valid.profile.id)
    .map(row => row.id);
  const deleteRuleManifestIds = existingRuleManifests
    .filter(row => row.profileId === valid.profile.id || row.id === `rules-folder:${valid.profile.id}`)
    .map(row => row.id);

  const canonicalState: CanonicalStateSnapshot = {
    campaign: { ...persisted, history: persisted.history.map(scene => ({ ...scene, imageData: undefined })) },
    characters: restoredCharacters,
    npcs: restoredNpcs,
    mapDiscovery: valid.mapDiscoveries.sort((a, b) => Number(b.updatedAt || 0) - Number(a.updatedAt || 0))[0],
    environmentState: (valid.environmentStates || []).sort((a, b) => Number(b.generatedAt || b.updatedAt || 0) - Number(a.generatedAt || a.updatedAt || 0))[0],
    environmentMemory: valid.environmentMemory || [],
    autonomousEvents: valid.autonomousEvents || [],
    capturedAt: Date.now(),
  };
  const checkpoint: Row = {
    id: `checkpoint:${persisted.id}:${persisted.revision}`,
    campaignId: persisted.id,
    eventType: 'CAMPAIGN_CHECKPOINT',
    entityType: 'canonical-state',
    payload: { revision: persisted.revision, transactionId, campaign: canonicalState.campaign, state: canonicalState },
    createdAt: Date.now(),
    applied: true,
  };
  await applyStoreChangesAtomically({ backup_restore_staging: { put: [{ id: stagingId, campaignId: valid.campaign.id, status: 'validated', createdAt: Date.now(), integrity: valid.integrity }] } });
  try {
    const changes: Record<string, { put?: any[]; delete?: IDBValidKey[] }> = {
      campaigns: { put: [persisted] },
      app_meta: { put: [{ id: 'activeCampaignId', value: persisted.id, updatedAt: Date.now() }] },
      characters: { put: restoredCharacters, delete: Array.from(currentCharacterIds) },
      npcs: { put: restoredNpcs, delete: Array.from(currentNpcIds) },
      rolls: { put: valid.rolls }, manuals: { put: valid.manuals, delete: Array.from(replacedManualIds) }, manual_chunks: { put: valid.manualChunks, delete: deleteChunkIds },
      token_sheets: { put: valid.tokenSheets }, sound_metadata: { put: valid.soundsMeta }, map_manifests: { put: valid.mapManifests }, map_discovery: { put: valid.mapDiscoveries, delete: await idsForCampaign('map_discovery', valid.campaign.id) },
      command_log: { put: valid.commandLog || [], delete: await idsForCampaign('command_log', valid.campaign.id) },
      knowledge_ledger: { put: valid.knowledgeLedger || [], delete: await idsForCampaign('knowledge_ledger', valid.campaign.id) },
      actor_memories: { put: valid.actorMemories || [], delete: await idsForCampaign('actor_memories', valid.campaign.id) },
      rule_decisions: { put: valid.ruleDecisions || [], delete: await idsForCampaign('rule_decisions', valid.campaign.id) },
      sound_keyword_rules: { put: valid.soundKeywordRules || [] }, npc_token_catalog: { put: valid.npcTokenCatalog || [] }, npc_token_import_state: { put: valid.npcTokenImportState || [] },
      migration_reports: { put: valid.migrationReports || [], delete: await idsForCampaign('migration_reports', valid.campaign.id) },
      homologation_reports: { put: valid.homologationReports || [], delete: await idsForCampaign('homologation_reports', valid.campaign.id) },
      event_journal: { put: [...(valid.eventJournal || []), checkpoint], delete: await idsForCampaign('event_journal', valid.campaign.id) },
      turn_transactions: { put: valid.turnTransactions || [], delete: await idsForCampaign('turn_transactions', valid.campaign.id) },
      asset_registry: { put: valid.assetRegistry || [], delete: deleteAssetIds }, health_reports: { put: valid.healthReports || [], delete: await idsForCampaign('health_reports', valid.campaign.id) },
      manual_search_index: { put: valid.manualSearchIndex || [], delete: deleteSearchIds }, environment_states: { put: valid.environmentStates || [], delete: await idsForCampaign('environment_states', valid.campaign.id) },
      autonomous_events: { put: valid.autonomousEvents || [], delete: await idsForCampaign('autonomous_events', valid.campaign.id) },
      environment_memory: { put: valid.environmentMemory || [], delete: await idsForCampaign('environment_memory', valid.campaign.id) },
      rules_folder_manifests: { put: valid.rulesFolderManifests || [], delete: deleteRuleManifestIds },
      backup_restore_staging: { delete: [stagingId] },
    };
    if (tokenBlobs) changes.individual_tokens = {
      put: tokenBlobs,
      delete: Array.from(new Set([...currentReferencedTokens, ...valid.tokensMeta.map(row => row.id)])),
    };
    await applyStoreChangesAtomically(changes);
    saveEmergencyCampaignMirror(persisted);
    return persisted;
  } catch (error) {
    await applyStoreChangesAtomically({ backup_restore_staging: { put: [{ id: stagingId, campaignId: valid.campaign.id, status: 'failed', createdAt: Date.now(), updatedAt: Date.now(), integrity: valid.integrity, error: error instanceof Error ? error.message : String(error) }] } }).catch(() => undefined);
    throw error;
  }
}

export async function restoreQuickBackup(jsonText: string): Promise<CampaignState> {
  let parsed: unknown;
  try { parsed = JSON.parse(jsonText); } catch { throw new Error('O JSON do backup está corrompido. Nenhum dado atual foi alterado.'); }
  return applyBackup(await validateBackup(parsed));
}

export async function restoreCompleteBackup(zipBlob: Blob): Promise<CampaignState> {
  let zip: JSZip;
  try { zip = await JSZip.loadAsync(zipBlob); } catch { throw new Error('O arquivo ZIP está corrompido. Nenhum dado atual foi alterado.'); }
  const indexFile = zip.file('backup.json');
  if (!indexFile) throw new Error('backup.json não foi encontrado dentro do ZIP.');
  const data = await validateBackup(JSON.parse(await indexFile.async('string')));
  const tokens: IndividualToken[] = [];
  for (const metadata of data.tokensMeta || []) {
    const file = zip.file(`tokens/${metadata.id}.png`);
    if (!file) throw new Error(`Token ${metadata.id} está ausente no backup completo.`);
    tokens.push({ ...metadata, blob: await file.async('blob') });
  }
  return applyBackup(data, tokens);
}
