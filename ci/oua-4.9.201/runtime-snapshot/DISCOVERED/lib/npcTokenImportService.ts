import JSZip from 'jszip';
import type { IndividualToken, Npc, TokenSheet } from './db';
import { applyStoreChangesAtomically, getActiveCampaign, getStoreData, saveCanonicalTransaction } from './db';
import { downloadDriveFile, type DriveTreeFileInfo } from './drive';
import { TOKENS_DRIVE_FOLDER_ID } from './driveFolders';
import {
  autoAssignNpcTokens,
  describeNpcTokenFilename,
  descriptorOverrideFromTokenSheet,
  getNpcTokenCatalog,
  normalizeNpcTokenText,
  stableNpcTokenHash,
  type NpcTokenCatalogEntry,
  type NpcTokenDescriptorOverride,
} from './npcTokenLibrary';
import { importOneRingTokenLibraryFromDrive, importOneRingTokenPackage, oneRingAutomaticTokenSheetAllowed, type ManifestTokenSheet } from './tokenSheetLibrary';

const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'webp']);
const MANIFEST_NAMES = new Set(['npc_manifest.json', 'npc_token_manifest.json', 'npc_overrides.json']);
const IMPORT_STATE_ID = 'npc-drive-library';

export type NpcTokenSyncStage = 'locating-folder' | 'listing' | 'reading-manifest' | 'downloading' | 'saving' | 'assigning' | 'done';

export interface NpcTokenSyncProgress {
  stage: NpcTokenSyncStage;
  message: string;
  current?: number;
  total?: number;
  filename?: string;
}

export interface NpcTokenImportState {
  id: string;
  source: 'google-drive' | 'device';
  parentFolderId?: string;
  folderId?: string;
  folderName?: string;
  lastSyncAt: number;
  status: 'ok' | 'partial' | 'error';
  discoveredCount: number;
  activeCount: number;
  downloadedCount: number;
  unchangedCount: number;
  disabledCount: number;
  failedCount: number;
  failures: Array<{ filename: string; message: string }>;
}

export interface NpcTokenSyncSummary extends NpcTokenImportState {
  assignedNpcCount: number;
}

interface ManifestData {
  byKey: Map<string, NpcTokenDescriptorOverride>;
}

function extensionOf(name: string): string {
  return name.split('.').pop()?.toLowerCase() || '';
}

function isImageName(name: string): boolean {
  return IMAGE_EXTENSIONS.has(extensionOf(name));
}

function isManifestName(name: string): boolean {
  return MANIFEST_NAMES.has(name.toLowerCase());
}

function makeStableLocalId(prefix: string, value: string): string {
  return `${prefix}_${stableNpcTokenHash(normalizeNpcTokenText(value)).toString(16).padStart(8, '0')}`;
}

function signatureFromDrive(file: DriveTreeFileInfo): string {
  return file.md5Checksum || `${file.size || '0'}:${file.modifiedTime || ''}:${file.id}`;
}

async function sha256(blob: Blob): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', await blob.arrayBuffer());
  return Array.from(new Uint8Array(digest)).map(value => value.toString(16).padStart(2, '0')).join('');
}

async function validateImageBlob(blob: Blob, filename: string): Promise<Blob> {
  if (!blob.size) throw new Error('arquivo vazio');
  const expected = extensionOf(filename);
  const fallbackMime = expected === 'png' ? 'image/png' : expected === 'webp' ? 'image/webp' : 'image/jpeg';
  const typed = blob.type.startsWith('image/') ? blob : blob.slice(0, blob.size, fallbackMime);
  if (typeof createImageBitmap === 'function') {
    const bitmap = await createImageBitmap(typed);
    const valid = bitmap.width > 0 && bitmap.height > 0;
    bitmap.close();
    if (!valid) throw new Error('imagem sem dimensões válidas');
    return typed;
  }
  return typed;
}

function normalizeOverride(raw: any): NpcTokenDescriptorOverride {
  const list = (value: unknown) => Array.isArray(value) ? value.map(item => String(item)) : [];
  return {
    label: typeof raw?.label === 'string' ? raw.label : undefined,
    people: list(raw?.people ?? raw?.peoples ?? raw?.cultures),
    gender: list(raw?.gender ?? raw?.genders),
    ageGroups: list(raw?.ageGroups ?? raw?.ages),
    roles: list(raw?.roles ?? raw?.role),
    environment: list(raw?.environment ?? raw?.environments),
    appearance: list(raw?.appearance),
    equipment: list(raw?.equipment),
    tags: list(raw?.tags),
    enabled: raw?.enabled !== false,
  };
}

function parseManifestObject(raw: any): ManifestData {
  const byKey = new Map<string, NpcTokenDescriptorOverride>();
  const add = (key: string, value: any) => {
    const normalized = key.replace(/\\/g, '/').replace(/^\.\//, '').trim();
    if (!normalized) return;
    const override = normalizeOverride(value);
    byKey.set(normalized, override);
    byKey.set(normalized.toLocaleLowerCase('pt-BR'), override);
    const base = normalized.split('/').pop() || normalized;
    if (!byKey.has(base)) byKey.set(base, override);
    if (!byKey.has(base.toLocaleLowerCase('pt-BR'))) byKey.set(base.toLocaleLowerCase('pt-BR'), override);
  };

  if (Array.isArray(raw?.tokens)) {
    for (const item of raw.tokens) {
      const key = String(item?.relativePath || item?.path || item?.filename || '');
      add(key, item);
    }
  } else if (raw && typeof raw === 'object') {
    for (const [key, value] of Object.entries(raw)) {
      if (key === 'manifestVersion' || key === 'generatedAt' || key === 'tokenCount') continue;
      if (value && typeof value === 'object' && !Array.isArray(value)) add(key, value);
    }
  }
  return { byKey };
}

function mergeManifestData(parts: ManifestData[]): ManifestData {
  const byKey = new Map<string, NpcTokenDescriptorOverride>();
  for (const part of parts) for (const [key, value] of part.byKey) byKey.set(key, value);
  return { byKey };
}

function overrideFor(manifest: ManifestData, relativePath: string, filename: string): NpcTokenDescriptorOverride {
  return manifest.byKey.get(relativePath)
    || manifest.byKey.get(relativePath.toLocaleLowerCase('pt-BR'))
    || manifest.byKey.get(filename)
    || manifest.byKey.get(filename.toLocaleLowerCase('pt-BR'))
    || {};
}

async function readDriveManifests(files: DriveTreeFileInfo[], onProgress?: (progress: NpcTokenSyncProgress) => void): Promise<ManifestData> {
  const manifests = files.filter(file => isManifestName(file.name)).sort((a, b) => a.relativePath.localeCompare(b.relativePath, 'pt-BR'));
  const parts: ManifestData[] = [];
  for (let index = 0; index < manifests.length; index += 1) {
    const file = manifests[index];
    onProgress?.({ stage: 'reading-manifest', message: `Lendo ${file.relativePath}...`, current: index + 1, total: manifests.length, filename: file.relativePath });
    try {
      const blob = await downloadDriveFile(file.id, file.mimeType);
      parts.push(parseManifestObject(JSON.parse(await blob.text())));
    } catch (error) {
      throw new Error(`Manifesto inválido em ${file.relativePath}: ${error instanceof Error ? error.message : String(error)}`);
    }
  }
  return mergeManifestData(parts);
}

function catalogEntryFromDrive(
  file: DriveTreeFileInfo,
  index: number,
  folderId: string,
  current: NpcTokenCatalogEntry | undefined,
  manifest: ManifestData,
  now: number,
): NpcTokenCatalogEntry {
  const traits = describeNpcTokenFilename(file.name, overrideFor(manifest, file.relativePath, file.name));
  const tokenId = current?.tokenId || `npc-drive:${file.id}`;
  return {
    id: `npc_drive_${file.id}`,
    tokenId,
    filename: file.name,
    path: file.relativePath,
    label: traits.label,
    index,
    signature: signatureFromDrive(file),
    size: Number(file.size || 0),
    people: traits.people,
    gender: traits.gender,
    ageGroups: traits.ageGroups,
    roles: traits.roles,
    environment: traits.environment,
    appearance: traits.appearance,
    equipment: traits.equipment,
    tags: traits.tags,
    enabled: traits.enabled,
    createdAt: current?.createdAt || now,
    importedAt: now,
    source: 'google-drive',
    driveFileId: file.id,
    driveFolderId: folderId,
    relativePath: file.relativePath,
    mimeType: file.mimeType,
    modifiedTime: file.modifiedTime,
    md5Checksum: file.md5Checksum,
  };
}

async function assignUnassignedNpcTokens(catalog: NpcTokenCatalogEntry[]): Promise<number> {
  const npcs = await getStoreData<Npc>('npcs').catch(() => []);
  if (!npcs.length) return 0;
  const assigned = autoAssignNpcTokens(npcs, catalog, false);
  const count = assigned.reduce((total, npc, index) => total + (npc.tokenId !== npcs[index].tokenId ? 1 : 0), 0);
  if (count) {
    const campaign = await getActiveCampaign();
    if (campaign) await saveCanonicalTransaction({ campaign: { ...campaign, updatedAt: Date.now() }, npcs: assigned, reason: 'atribuir tokens de NPC após sincronização' });
  }
  return count;
}

/**
 * Reconstrói o catálogo de retratos a partir dos tokens individuais já recortados pelo manifesto.
 * Folhas marcadas como custom/excluídas nunca entram na seleção automática de O Um Anel.
 */
export async function rebuildNpcTokenCatalogFromSheetLibrary(assign = true): Promise<{ catalogCount: number; assignedNpcCount: number }> {
  const sheets = await getStoreData<(TokenSheet & ManifestTokenSheet)>('token_sheets').catch(() => []);
  const tokens = await getStoreData<IndividualToken>('individual_tokens').catch(() => []);
  const automaticSheets = new Map(sheets.filter(oneRingAutomaticTokenSheetAllowed).map(sheet => [sheet.id, sheet] as const));
  const existing = await getStoreData<NpcTokenCatalogEntry>('npc_token_catalog').catch(() => []);
  const preserved = existing.filter(item => item.source !== 'token-sheet');
  const now = Date.now();
  const catalog: NpcTokenCatalogEntry[] = [];
  for (const token of tokens) {
    const sheet = automaticSheets.get(token.sheetId);
    if (!sheet) continue;
    const traits = describeNpcTokenFilename(sheet.filename, descriptorOverrideFromTokenSheet(sheet));
    const suffix = String(token.tokenIndex).padStart(3, '0');
    catalog.push({
      id: `npc_sheet_${sheet.id}_${suffix}`,
      tokenId: token.id,
      filename: `${sheet.filename}#${suffix}`,
      path: `${sheet.filename}#${suffix}`,
      label: `${traits.label} ${suffix}`,
      index: token.tokenIndex,
      signature: `${sheet.id}:${token.tokenIndex}:${token.blob.size}`,
      size: token.blob.size,
      people: traits.people,
      gender: traits.gender,
      ageGroups: traits.ageGroups,
      roles: traits.roles,
      environment: traits.environment,
      appearance: traits.appearance,
      equipment: traits.equipment,
      tags: traits.tags,
      enabled: true,
      createdAt: now,
      importedAt: now,
      source: 'token-sheet',
      driveFolderId: TOKENS_DRIVE_FOLDER_ID,
      relativePath: `${sheet.filename}#${suffix}`,
      mimeType: token.blob.type || 'image/png',
    });
  }
  await applyStoreChangesAtomically({ npc_token_catalog: { clear: true, put: [...preserved, ...catalog] } });
  const assignedNpcCount = assign ? await assignUnassignedNpcTokens([...preserved, ...catalog].filter(item => item.enabled !== false)) : 0;
  return { catalogCount: catalog.length, assignedNpcCount };
}

/**
 * Sincroniza o catálogo oficial descrito por tokens_manifest.json. O manifesto é a autoridade de geometria:
 * folhas são recortadas em tokens individuais antes de entrarem no catálogo de NPCs.
 */
export async function syncNpcTokensFromConfiguredDrive(
  onProgress?: (progress: NpcTokenSyncProgress) => void,
): Promise<NpcTokenSyncSummary> {
  const startedAt = Date.now();
  onProgress?.({ stage: 'locating-folder', message: 'Localizando o manifesto oficial da biblioteca de tokens...' });
  const imported = await importOneRingTokenLibraryFromDrive(progress => {
    const stage: NpcTokenSyncStage = progress.stage === 'manifest' ? 'reading-manifest'
      : progress.stage === 'saving' ? 'saving'
      : progress.stage === 'done' ? 'saving'
      : 'downloading';
    onProgress?.({ stage, message: progress.message, current: progress.current, total: progress.total, filename: progress.filename });
  });
  onProgress?.({ stage: 'assigning', message: 'Construindo catálogo individual e atribuindo retratos compatíveis...' });
  const rebuilt = await rebuildNpcTokenCatalogFromSheetLibrary(true);
  const state: NpcTokenImportState = {
    id: IMPORT_STATE_ID,
    source: 'google-drive',
    parentFolderId: TOKENS_DRIVE_FOLDER_ID,
    folderId: TOKENS_DRIVE_FOLDER_ID,
    folderName: 'BRASEIRO_TOKENS (manifesto filtrado para O Um Anel)',
    lastSyncAt: Date.now(),
    status: 'ok',
    discoveredCount: imported.tokenCount,
    activeCount: rebuilt.catalogCount,
    downloadedCount: imported.tokenCount,
    unchangedCount: 0,
    disabledCount: imported.blockedSheets.length,
    failedCount: 0,
    failures: [],
  };
  await applyStoreChangesAtomically({ npc_token_import_state: { put: [state] } });
  onProgress?.({ stage: 'done', message: `${rebuilt.catalogCount} retratos individuais ativos; ${imported.blockedSheets.length} folhas externas bloqueadas; ${rebuilt.assignedNpcCount} NPCs receberam retrato.` });
  return { ...state, assignedNpcCount: rebuilt.assignedNpcCount, lastSyncAt: Math.max(startedAt, state.lastSyncAt) };
}

async function expandSelectedFiles(selected: File[]): Promise<Array<{ file: File; relativePath: string }>> {
  const expanded: Array<{ file: File; relativePath: string }> = [];
  for (const file of selected) {
    if (/\.zip$/i.test(file.name) || /zip/i.test(file.type)) {
      const zip = await JSZip.loadAsync(file);
      for (const [relativePath, entry] of Object.entries(zip.files) as Array<[string, any]>) {
        if (entry.dir) continue;
        const blob = await entry.async('blob');
        const name = relativePath.replace(/\\/g, '/').split('/').pop() || relativePath;
        expanded.push({ file: new File([blob], name, { type: blob.type }), relativePath: relativePath.replace(/\\/g, '/') });
      }
    } else expanded.push({ file, relativePath: file.webkitRelativePath || file.name });
  }
  return expanded;
}

/** Importação alternativa por ZIP ou seleção múltipla, sem recompilar o APK. */
export async function importNpcTokensFromDevice(
  selected: File[],
  onProgress?: (progress: NpcTokenSyncProgress) => void,
): Promise<NpcTokenSyncSummary> {
  const packageHasTokenManifest = selected.some(file => file.name === 'tokens_manifest.json' || /\.zip$/i.test(file.name) || /zip/i.test(file.type));
  if (packageHasTokenManifest) {
    try {
      const imported = await importOneRingTokenPackage(selected, progress => onProgress?.({
        stage: progress.stage === 'manifest' ? 'reading-manifest' : progress.stage === 'saving' ? 'saving' : progress.stage === 'done' ? 'saving' : 'downloading',
        message: progress.message, current: progress.current, total: progress.total, filename: progress.filename,
      }));
      const rebuilt = await rebuildNpcTokenCatalogFromSheetLibrary(true);
      const state: NpcTokenImportState = {
        id: 'npc-device-sheet-library', source: 'device', lastSyncAt: Date.now(), status: 'ok',
        discoveredCount: imported.tokenCount, activeCount: rebuilt.catalogCount, downloadedCount: imported.tokenCount,
        unchangedCount: 0, disabledCount: imported.blockedSheets.length, failedCount: 0, failures: [],
      };
      await applyStoreChangesAtomically({ npc_token_import_state: { put: [state] } });
      onProgress?.({ stage: 'done', message: `${rebuilt.catalogCount} retratos do pacote disponíveis; ${rebuilt.assignedNpcCount} NPCs receberam retrato.` });
      return { ...state, assignedNpcCount: rebuilt.assignedNpcCount };
    } catch (error) {
      // ZIPs sem tokens_manifest seguem pelo importador legado de imagens individuais.
      if (selected.some(file => file.name === 'tokens_manifest.json')) throw error;
    }
  }
  const expanded = await expandSelectedFiles(selected);
  const manifests = expanded.filter(item => isManifestName(item.file.name));
  const manifestParts: ManifestData[] = [];
  for (const item of manifests) {
    try { manifestParts.push(parseManifestObject(JSON.parse(await item.file.text()))); }
    catch (error) { throw new Error(`Manifesto inválido em ${item.relativePath}: ${error instanceof Error ? error.message : String(error)}`); }
  }
  const manifest = mergeManifestData(manifestParts);
  const images = expanded.filter(item => isImageName(item.file.name)).sort((a, b) => a.relativePath.localeCompare(b.relativePath, 'pt-BR'));
  if (!images.length) throw new Error('Nenhuma imagem PNG, JPG, JPEG ou WEBP foi selecionada.');

  const existing = await getStoreData<NpcTokenCatalogEntry>('npc_token_catalog').catch(() => []);
  const currentByPath = new Map(existing.filter(item => item.source === 'device').map(item => [item.relativePath || item.filename, item] as const));
  const now = Date.now();
  const entries: NpcTokenCatalogEntry[] = [];
  const blobs: IndividualToken[] = [];
  const failures: Array<{ filename: string; message: string }> = [];
  let unchangedCount = 0;

  for (let index = 0; index < images.length; index += 1) {
    const item = images[index];
    onProgress?.({ stage: 'downloading', message: `Validando ${item.relativePath}...`, current: index + 1, total: images.length, filename: item.relativePath });
    try {
      const blob = await validateImageBlob(item.file, item.file.name);
      const signature = await sha256(blob);
      const current = currentByPath.get(item.relativePath) || currentByPath.get(item.file.name);
      const traits = describeNpcTokenFilename(item.file.name, overrideFor(manifest, item.relativePath, item.file.name));
      const baseId = current?.id || makeStableLocalId('npc_device', item.relativePath);
      const tokenId = current?.tokenId || `npc-device:${baseId}`;
      const entry: NpcTokenCatalogEntry = {
        id: baseId, tokenId, filename: item.file.name, path: item.relativePath, label: traits.label, index, signature, size: blob.size,
        people: traits.people, gender: traits.gender, ageGroups: traits.ageGroups, roles: traits.roles, environment: traits.environment,
        appearance: traits.appearance, equipment: traits.equipment, tags: traits.tags, enabled: traits.enabled,
        createdAt: current?.createdAt || now, importedAt: now, source: 'device', relativePath: item.relativePath, mimeType: blob.type,
      };
      entries.push(entry);
      if (current?.signature === signature) unchangedCount += 1;
      else blobs.push({ id: tokenId, sheetId: 'npc-device', tokenIndex: index, blob, category: 'npc', createdAt: current?.createdAt || now });
    } catch (error) {
      failures.push({ filename: item.relativePath, message: error instanceof Error ? error.message : String(error) });
    }
  }
  const state: NpcTokenImportState = {
    id: 'npc-device-library', source: 'device', lastSyncAt: now,
    status: failures.length ? (entries.length ? 'partial' : 'error') : 'ok',
    discoveredCount: images.length, activeCount: entries.filter(item => item.enabled !== false).length,
    downloadedCount: blobs.length, unchangedCount, disabledCount: entries.filter(item => item.enabled === false).length,
    failedCount: failures.length, failures,
  };
  await applyStoreChangesAtomically({ npc_token_catalog: { put: entries }, individual_tokens: { put: blobs }, npc_token_import_state: { put: [state] } });
  const assignedNpcCount = await assignUnassignedNpcTokens(await getNpcTokenCatalog());
  onProgress?.({ stage: 'done', message: `${state.activeCount} tokens importados do aparelho; ${assignedNpcCount} NPCs receberam retrato.` });
  return { ...state, assignedNpcCount };
}

export async function getNpcTokenImportStates(): Promise<NpcTokenImportState[]> {
  return getStoreData<NpcTokenImportState>('npc_token_import_state').catch(() => []);
}

/** Limpa somente a biblioteca de retratos de NPC; não toca nos tokens de heróis, mapa ou campanha. */
export async function clearNpcTokenLibrary(): Promise<void> {
  const catalog = await getStoreData<NpcTokenCatalogEntry>('npc_token_catalog').catch(() => []);
  const npcTokenIds = catalog.filter(item => item.source !== 'token-sheet').map(item => item.tokenId);
  await applyStoreChangesAtomically({
    npc_token_catalog: { clear: true },
    npc_token_import_state: { clear: true },
    individual_tokens: { delete: npcTokenIds },
  });
}
