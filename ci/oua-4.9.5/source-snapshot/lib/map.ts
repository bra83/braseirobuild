import JSZip from 'jszip';

import {
  clearStore,
  deleteStoreData,
  getStoreData,
  getStoreItem,
  saveStoreData,
} from './db';

/**
 * Geometria reconstruída diretamente sobre o PNG original de 10.630 × 7.678.
 * O identificador novo impede que centros/polígonos de versões antigas sejam
 * misturados silenciosamente com esta malha.
 */
export const ERIADOR_MAP_ID = 'terra-media-maxres-10630x7678-v2';
export const MAP_DISCOVERY_CHANGED_EVENT = 'braseiro-map-discovery-changed';
export const ERIADOR_IMAGE_WIDTH = 10_630;
export const ERIADOR_IMAGE_HEIGHT = 7_678;
export const ERIADOR_HEX_COUNT = 12_050;

export interface MapPoint {
  x: number;
  y: number;
}

export interface EriadorHex {
  id: string;
  q: number;
  r: number;
  rowIndex?: number;
  columnIndex?: number;
  centerX: number;
  centerY: number;
  normalizedCenterX: number;
  normalizedCenterY: number;
  polygon: MapPoint[];
  normalizedPolygon: MapPoint[];
  crop: { x: number; y: number; width: number; height: number };
  enabled: boolean;
  playable: boolean;
  neighbors: string[];
  terrain: string;
  region: string;
  name?: string;
  poi?: string;
  placeType?: string;
  confidence?: number;
  tags: string[];
  locationIds?: string[];
}

export interface EriadorMapManifest {
  id: string;
  manifestVersion: string;
  mapId: string;
  filename: 'map.png';
  image: {
    width: number;
    height: number;
    mimeType: 'image/png';
    file?: string;
    sha256?: string;
  };
  layout: {
    type: 'explicit';
    orientation: 'pointy-top';
    coordinateSystem: 'odd-r-offset';
    mask: 'hexagon';
    origin: MapPoint;
    horizontalStep: number;
    rowStep: number;
    oddRowOffsetX: number;
    hexWidth: number;
    hexHeight: number;
    /** Compatibilidade com a miniatura dos sete hexes. */
    vectorQ: MapPoint;
    vectorR: MapPoint;
    measuredFrom?: string;
    measurementRevision?: string;
  };
  hexCount: number;
  hexes: EriadorHex[];
  revision: number;
  createdAt: number;
  updatedAt: number;
  locationCatalog?: {
    file: string;
    count: number;
    verifiedLabelCount: number;
    derivedCount: number;
  };
  distanceScale?: Record<string, unknown>;
}

export interface TerraMediaLocation {
  id: string;
  name: string;
  nameOriginal: string;
  mapNames: string[];
  aliases: string[];
  type: string;
  categories: string[];
  regions: string[];
  state2965: string;
  description: string;
  description2: string;
  tags: string[];
  presenceOnMap?: string;
  canonicalStatus?: string;
  sources?: Record<string, unknown>;
  placement: {
    status: 'verified-label' | 'derived' | string;
    method: string;
    confidence: number;
    precision: string;
    note?: string;
  };
  geometry: { type: string; coordinates: unknown } | null;
  representativePoint: [number, number] | null;
  hexId: string | null;
  hexIds: string[];
}

interface TerraMediaLocationsDocument {
  mapId: string;
  count: number;
  verifiedLabelCount: number;
  derivedCount: number;
  unassignedCount: number;
  locations: TerraMediaLocation[];
}

export interface MapDiscovery {
  id: string;
  campaignId: string;
  mapId: string;
  currentHexId: string | null;
  discoveredHexIds: string[];
  visitedHexIds: string[];
  /** Ordered trail already travelled. Older saves are migrated automatically. */
  routeHexIds: string[];
  /** Future route explicitly planned by the player. The current hex is not repeated here. */
  plannedRouteHexIds?: string[];
  fatigue: number;
  updatedAt: number;
}

export interface MapHexCrop {
  id: string;
  mapId: string;
  hexId: string;
  geometrySignature?: string;
  blob: Blob;
  createdAt: number;
}

export interface ImportedMapPackageResult {
  manifest: EriadorMapManifest;
  imageBlob: Blob;
  imageWidth: number;
  imageHeight: number;
  importedFiles: string[];
  locationCount: number;
}

interface StoredMapDataset {
  id: string;
  mapId: string;
  filename: string;
  kind?: 'json' | 'text' | 'blob' | 'base-image';
  data?: unknown;
  text?: string;
  blob?: Blob;
  width?: number;
  height?: number;
  mimeType?: string;
  importedAt: number;
}

const BASE_IMAGE_DATASET_FILENAME = '__base_image__';
let manifestCache: EriadorMapManifest | null = null;
let runtimeManifestCache: EriadorMapManifest | null = null;
let locationsCache: TerraMediaLocation[] | null = null;
let runtimeLocationsCache: TerraMediaLocation[] | null = null;
const BUNDLED_REVISION = 1_801_012_050;
const BUNDLED_MAP_DATASETS = [
  'map_manifest_terra_media_v3.json',
  'hex_index_terra_media_v3.json',
  'terra_media_locations_integrated.json',
  'terra_media_hex_location_index.json',
  'terra_media_catalog_manifest.json',
  'terra_media_location.schema.json',
  'VALIDACAO_TERRA_MEDIA_2.0.json',
] as const;

function fileBasename(path: string): string {
  return path.replace(/\\/g, '/').split('/').filter(Boolean).at(-1) || path;
}

async function inspectImageBlob(blob: Blob): Promise<{ width: number; height: number }> {
  if ('createImageBitmap' in globalThis) {
    const bitmap = await createImageBitmap(blob);
    try {
      return { width: bitmap.width, height: bitmap.height };
    } finally {
      bitmap.close();
    }
  }
  const url = URL.createObjectURL(blob);
  try {
    const image = new Image();
    image.src = url;
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve();
      image.onerror = () => reject(new Error('A imagem-base do mapa não pôde ser aberta.'));
    });
    return { width: image.naturalWidth, height: image.naturalHeight };
  } finally {
    URL.revokeObjectURL(url);
  }
}

function terrainName(value: unknown): string {
  if (typeof value === 'string') return value;
  if (value && typeof value === 'object' && typeof (value as { primary?: unknown }).primary === 'string') {
    return (value as { primary: string }).primary;
  }
  return 'unknown';
}

function normalizeManifest(raw: EriadorMapManifest): EriadorMapManifest {
  const layout = raw.layout as EriadorMapManifest['layout'] & Record<string, unknown>;
  const horizontalStep = Number(layout.horizontalStep || (layout.vectorQ as MapPoint | undefined)?.x || 88.26884803);
  const rowStep = Number(layout.rowStep || (layout.vectorR as MapPoint | undefined)?.y || 76.2721723);
  const oddRowOffsetX = Number(layout.oddRowOffsetX || (layout.vectorR as MapPoint | undefined)?.x || horizontalStep / 2);
  return {
    ...raw,
    id: ERIADOR_MAP_ID,
    mapId: ERIADOR_MAP_ID,
    manifestVersion: '3.0.0',
    filename: 'map.png',
    image: {
      ...raw.image,
      width: ERIADOR_IMAGE_WIDTH,
      height: ERIADOR_IMAGE_HEIGHT,
      mimeType: 'image/png',
    },
    layout: {
      ...layout,
      type: 'explicit',
      orientation: 'pointy-top',
      coordinateSystem: 'odd-r-offset',
      mask: 'hexagon',
      horizontalStep,
      rowStep,
      oddRowOffsetX,
      hexWidth: Number(layout.hexWidth || horizontalStep),
      hexHeight: Number(layout.hexHeight || rowStep * 4 / 3),
      vectorQ: { x: horizontalStep, y: 0 },
      vectorR: { x: oddRowOffsetX, y: rowStep },
    },
    hexes: (raw.hexes || []).map(hex => ({
      ...hex,
      q: Number(hex.q),
      r: Number(hex.r),
      centerX: Number(hex.centerX),
      centerY: Number(hex.centerY),
      terrain: terrainName(hex.terrain),
      confidence: typeof hex.confidence === 'number' ? hex.confidence : undefined,
      tags: Array.isArray(hex.tags) ? hex.tags : [],
      locationIds: Array.isArray(hex.locationIds) ? hex.locationIds : [],
      neighbors: Array.isArray(hex.neighbors) ? hex.neighbors : [],
      enabled: hex.enabled !== false,
      playable: hex.playable !== false && terrainName(hex.terrain) !== 'sea',
    })),
  };
}

export async function loadStoredMapImageBlob(): Promise<Blob | null> {
  const row = await getStoreItem<StoredMapDataset>('map_datasets', `${ERIADOR_MAP_ID}:${BASE_IMAGE_DATASET_FILENAME}`);
  return row?.mapId === ERIADOR_MAP_ID ? row.blob || null : null;
}

async function readBundledJson<T>(filename: string): Promise<T> {
  const response = await fetch(`./terra-media-map/${filename}`, { cache: 'no-store' });
  if (!response.ok) throw new Error(`Falha ao carregar ${filename}: HTTP ${response.status}.`);
  return await response.json() as T;
}

async function loadBundledFixedManifest(): Promise<EriadorMapManifest> {
  const authority = normalizeManifest(await readBundledJson<EriadorMapManifest>('app_map_manifest.json'));
  validateEriadorManifest(authority, ERIADOR_IMAGE_WIDTH, ERIADOR_IMAGE_HEIGHT);
  return authority;
}

/**
 * Manifesto enxuto para o boot e para o atlas imersivo. Mantém exatamente os
 * mesmos IDs, centros, vizinhos e metadados canônicos, mas não transporta no
 * JSON os 72 mil pontos de polígonos nem grava o manifesto de 9,6 MB no
 * IndexedDB durante a abertura. A geometria regular é reconstituída em memória.
 */
export async function loadRuntimeMapManifest(): Promise<EriadorMapManifest> {
  if (runtimeManifestCache) return runtimeManifestCache;
  const raw = await readBundledJson<any>('app_map_manifest_lite.json');
  const layout = raw.layout || {};
  const hexWidth = Number(layout.hexWidth || layout.horizontalStep || 88.26884803);
  const hexHeight = Number(layout.hexHeight || (Number(layout.rowStep || 76.2721723) * 4 / 3));
  const halfW = hexWidth / 2;
  const halfH = hexHeight / 2;
  const quarterH = hexHeight / 4;
  const hydrated = {
    ...raw,
    hexes: (raw.hexes || []).map((hex: any) => {
      const centerX = Number(hex.centerX);
      const centerY = Number(hex.centerY);
      const polygon = [
        { x: centerX, y: centerY - halfH },
        { x: centerX + halfW, y: centerY - quarterH },
        { x: centerX + halfW, y: centerY + quarterH },
        { x: centerX, y: centerY + halfH },
        { x: centerX - halfW, y: centerY + quarterH },
        { x: centerX - halfW, y: centerY - quarterH },
      ];
      return {
        ...hex,
        polygon,
        normalizedCenterX: centerX / ERIADOR_IMAGE_WIDTH,
        normalizedCenterY: centerY / ERIADOR_IMAGE_HEIGHT,
        normalizedPolygon: polygon.map(point => ({ x: point.x / ERIADOR_IMAGE_WIDTH, y: point.y / ERIADOR_IMAGE_HEIGHT })),
        crop: { x: centerX - halfW, y: centerY - halfH, width: hexWidth, height: hexHeight },
      };
    }),
  } as EriadorMapManifest;
  runtimeManifestCache = validateEriadorManifest(normalizeManifest(hydrated), ERIADOR_IMAGE_WIDTH, ERIADOR_IMAGE_HEIGHT);
  return runtimeManifestCache;
}

function normalizeLocation(value: TerraMediaLocation): TerraMediaLocation {
  const point = Array.isArray(value.representativePoint) && value.representativePoint.length >= 2
    ? [Number(value.representativePoint[0]), Number(value.representativePoint[1])] as [number, number]
    : null;
  return {
    ...value,
    name: String(value.name || value.nameOriginal || value.id),
    nameOriginal: String(value.nameOriginal || ''),
    mapNames: Array.isArray(value.mapNames) ? value.mapNames.map(String) : [],
    aliases: Array.isArray(value.aliases) ? value.aliases.map(String) : [],
    categories: Array.isArray(value.categories) ? value.categories.map(String) : [],
    regions: Array.isArray(value.regions) ? value.regions.map(String) : [],
    tags: Array.isArray(value.tags) ? value.tags.map(String) : [],
    state2965: String(value.state2965 || ''),
    description: String(value.description || ''),
    description2: String(value.description2 || ''),
    representativePoint: point,
    hexId: value.hexId ? String(value.hexId) : null,
    hexIds: Array.isArray(value.hexIds) ? value.hexIds.map(String) : (value.hexId ? [String(value.hexId)] : []),
    placement: {
      status: value.placement?.status || 'derived',
      method: value.placement?.method || 'unknown',
      confidence: Number(value.placement?.confidence || 0),
      precision: value.placement?.precision || 'unknown',
      note: value.placement?.note,
    },
  };
}

function validateLocationsDocument(value: unknown, manifest: EriadorMapManifest): TerraMediaLocationsDocument {
  const document = value as TerraMediaLocationsDocument;
  if (!document || document.mapId !== ERIADOR_MAP_ID || !Array.isArray(document.locations)) {
    throw new Error('O catálogo de locais não corresponde ao novo mapa da Terra-média.');
  }
  if (document.count !== document.locations.length) {
    throw new Error('A contagem declarada do catálogo de locais é inconsistente.');
  }
  const hexIds = new Set(manifest.hexes.map(hex => hex.id));
  const locationIds = new Set<string>();
  for (const raw of document.locations) {
    const location = normalizeLocation(raw);
    if (!location.id || locationIds.has(location.id)) throw new Error(`ID de local duplicado ou inválido: ${location.id || '(vazio)'}.`);
    locationIds.add(location.id);
    if (location.hexId && !hexIds.has(location.hexId)) throw new Error(`O local ${location.name} referencia o hex inexistente ${location.hexId}.`);
    if (location.hexIds.some(id => !hexIds.has(id))) throw new Error(`O local ${location.name} possui uma referência de hex inválida.`);
    if (location.representativePoint) {
      const [x, y] = location.representativePoint;
      if (x < 0 || y < 0 || x > ERIADOR_IMAGE_WIDTH || y > ERIADOR_IMAGE_HEIGHT) {
        throw new Error(`O ponto cartográfico de ${location.name} está fora da imagem.`);
      }
    }
  }
  return {
    ...document,
    locations: document.locations.map(normalizeLocation),
  };
}

export async function importTerraMediaMapPackage(file: File): Promise<ImportedMapPackageResult> {
  if (!/\.zip$/i.test(file.name) && !/zip/i.test(file.type)) {
    throw new Error('Selecione o ZIP completo do mapa da Terra-média.');
  }
  const zip = await JSZip.loadAsync(file, { checkCRC32: true, createFolders: false });
  const normalizePath = (value: string): string => value.replace(/\\/g, '/').replace(/^\/+/, '').replace(/\/+/g, '/');
  const dirname = (value: string): string => { const clean = normalizePath(value); const index = clean.lastIndexOf('/'); return index >= 0 ? clean.slice(0, index) : ''; };
  const joinPath = (...parts: string[]): string => normalizePath(parts.filter(Boolean).join('/'));
  const isIgnored = (value: string): boolean => {
    const clean = normalizePath(value);
    const base = fileBasename(clean);
    return clean.startsWith('__MACOSX/') || base === '.DS_Store' || base === 'Thumbs.db' || base.startsWith('._');
  };
  const allEntries = Object.values(zip.files as Record<string, any>)
    .filter(entry => !entry.dir && !isIgnored(entry.name))
    .map(entry => ({ path: normalizePath(entry.name), entry }));
  const byPath = new Map(allEntries.map(item => [item.path.toLocaleLowerCase('pt-BR'), item]));
  const manifestName = 'map_manifest_terra_media_v3.json';
  const manifestCandidates = allEntries.filter(item => fileBasename(item.path).toLocaleLowerCase('pt-BR') === manifestName);
  if (!manifestCandidates.length) {
    throw new Error(`Arquivo obrigatório ausente no ZIP: ${manifestName}. O importador também aceita o ZIP inteiro do projeto, desde que ele contenha a pasta terra-media-map.`);
  }

  const locateNear = (root: string, requested: string, aliases: string[] = []): { path: string; entry: any; rank: number } | null => {
    const names = [requested, ...aliases].map(name => fileBasename(name).toLocaleLowerCase('pt-BR'));
    const parent = dirname(root);
    const requestedPath = normalizePath(requested);
    const exactCandidates = [joinPath(root, requestedPath), joinPath(parent, requestedPath), joinPath(root, fileBasename(requestedPath)), joinPath(parent, fileBasename(requestedPath))];
    for (let index = 0; index < exactCandidates.length; index += 1) {
      const found = byPath.get(exactCandidates[index].toLocaleLowerCase('pt-BR'));
      if (found) return { ...found, rank: 200 - index * 10 };
    }
    let best: { path: string; entry: any; rank: number } | null = null;
    for (const item of allEntries) {
      const base = fileBasename(item.path).toLocaleLowerCase('pt-BR');
      const nameIndex = names.indexOf(base);
      if (nameIndex < 0) continue;
      const inRoot = !root || item.path.startsWith(`${root}/`);
      const inParent = !parent || item.path.startsWith(`${parent}/`);
      const common = inRoot ? 100 : inParent ? 70 : 10;
      const depthPenalty = Math.max(0, item.path.split('/').length - root.split('/').length);
      const rank = common + (names.length - nameIndex) * 4 - depthPenalty;
      if (!best || rank > best.rank) best = { ...item, rank };
    }
    return best;
  };

  let selected: { root: string; packageManifest: any; manifestEntry: any; imageEntry: any; score: number } | null = null;
  for (const candidate of manifestCandidates) {
    try {
      const packageManifest = JSON.parse(await candidate.entry.async('text')) as any;
      if (packageManifest?.mapId !== ERIADOR_MAP_ID) continue;
      const root = dirname(candidate.path);
      const appManifestFilename = String(packageManifest?.grid?.appManifest || 'app_map_manifest.json');
      const hexIndexFilename = String(packageManifest?.grid?.indexFile || 'hex_index_terra_media_v3.json');
      const locationsFilename = String(packageManifest?.dataFiles?.locations || 'terra_media_locations_integrated.json');
      const imageFilename = String(packageManifest?.image?.file || 'map_terra_media_10630x7678.png');
      const appManifest = locateNear(root, appManifestFilename);
      const hexIndex = locateNear(root, hexIndexFilename);
      const locations = locateNear(root, locationsFilename);
      const image = locateNear(root, imageFilename, ['map.png', 'map_terra_media.png']);
      const score = 100 + (appManifest ? 30 : 0) + (hexIndex ? 30 : 0) + (locations ? 30 : 0) + (image ? 60 + image.rank / 100 : 0) - root.split('/').length / 100;
      if (appManifest && hexIndex && locations && image && (!selected || score > selected.score)) {
        selected = { root, packageManifest, manifestEntry: candidate.entry, imageEntry: image.entry, score };
      }
    } catch {
      // Outro arquivo com o mesmo nome pode pertencer a uma cópia incompleta. Testamos o próximo candidato.
    }
  }
  if (!selected) {
    throw new Error('O ZIP contém o manifesto do mapa, mas nenhuma cópia completa com geometria, índice, locais e imagem 10630×7678 foi encontrada.');
  }

  const packageRoot = selected.root;
  const packageItems = allEntries.filter(item => !packageRoot || item.path.startsWith(`${packageRoot}/`) || item.path === packageRoot);
  const entries = new Map<string, any>();
  const basenameEntries = new Map<string, Array<{ relativePath: string; entry: any }>>();
  for (const item of packageItems) {
    const relativePath = packageRoot ? item.path.slice(packageRoot.length + 1) : item.path;
    if (!relativePath) continue;
    entries.set(relativePath, item.entry);
    const base = fileBasename(relativePath).toLocaleLowerCase('pt-BR');
    const group = basenameEntries.get(base) || [];
    group.push({ relativePath, entry: item.entry });
    basenameEntries.set(base, group);
  }
  const resolvePackageEntry = (name: string): any | null => {
    const normalized = normalizePath(name);
    const exact = entries.get(normalized);
    if (exact) return exact;
    const candidates = basenameEntries.get(fileBasename(normalized).toLocaleLowerCase('pt-BR')) || [];
    if (!candidates.length) return null;
    return candidates.sort((a, b) => a.relativePath.split('/').length - b.relativePath.split('/').length || a.relativePath.localeCompare(b.relativePath))[0].entry;
  };
  const readJson = async <T,>(name: string): Promise<T> => {
    const entry = resolvePackageEntry(name);
    if (!entry) throw new Error(`Arquivo obrigatório ausente no pacote selecionado: ${name}.`);
    try {
      return JSON.parse(await entry.async('text')) as T;
    } catch {
      throw new Error(`JSON inválido no pacote: ${name}.`);
    }
  };

  const packageManifest = selected.packageManifest;
  const imageFilename = String(packageManifest?.image?.file || 'map_terra_media_10630x7678.png');
  const appManifestFilename = String(packageManifest?.grid?.appManifest || 'app_map_manifest.json');
  const hexIndexFilename = String(packageManifest?.grid?.indexFile || 'hex_index_terra_media_v3.json');
  const locationsFilename = String(packageManifest?.dataFiles?.locations || 'terra_media_locations_integrated.json');

  const imageBlob = new Blob([await selected.imageEntry.async('uint8array')], { type: 'image/png' });
  const { width: imageWidth, height: imageHeight } = await inspectImageBlob(imageBlob);
  if (imageWidth !== ERIADOR_IMAGE_WIDTH || imageHeight !== ERIADOR_IMAGE_HEIGHT) {
    throw new Error(`O mapa possui ${imageWidth}×${imageHeight}; esta reconstrução exige exatamente ${ERIADOR_IMAGE_WIDTH}×${ERIADOR_IMAGE_HEIGHT}.`);
  }

  const importedManifest = normalizeManifest(await readJson<EriadorMapManifest>(appManifestFilename));
  validateEriadorManifest(importedManifest, imageWidth, imageHeight);

  const hexIndex = await readJson<any>(hexIndexFilename);
  const indexIds = Object.keys(hexIndex?.hexes || {});
  const manifestIds = new Set(importedManifest.hexes.map(hex => hex.id));
  if (Number(hexIndex?.count) !== ERIADOR_HEX_COUNT || indexIds.length !== ERIADOR_HEX_COUNT || indexIds.some(id => !manifestIds.has(id))) {
    throw new Error(`O índice não corresponde aos ${ERIADOR_HEX_COUNT.toLocaleString('pt-BR')} hexes reconstruídos.`);
  }

  const locationsDocument = validateLocationsDocument(await readJson<TerraMediaLocationsDocument>(locationsFilename), importedManifest);

  // Só os dados deste mapId são substituídos. Campanhas e outros bancos não são tocados.
  const oldRows = await getStoreData<StoredMapDataset>('map_datasets');
  for (const row of oldRows.filter(item => item.mapId === ERIADOR_MAP_ID)) {
    await deleteStoreData('map_datasets', row.id);
  }

  const importedAt = Date.now();
  for (const [relativePath, entry] of entries) {
    const filename = normalizePath(relativePath);
    if (fileBasename(filename).toLocaleLowerCase('pt-BR') === fileBasename(imageFilename).toLocaleLowerCase('pt-BR')) continue;
    const id = `${ERIADOR_MAP_ID}:${filename}`;
    if (/\.json$/i.test(filename)) {
      const data = JSON.parse(await entry.async('text')) as unknown;
      await saveStoreData<StoredMapDataset>('map_datasets', { id, mapId: ERIADOR_MAP_ID, filename, kind: 'json', data, importedAt });
    } else if (/\.(txt|md|svg)$/i.test(filename)) {
      await saveStoreData<StoredMapDataset>('map_datasets', { id, mapId: ERIADOR_MAP_ID, filename, kind: 'text', text: await entry.async('text'), importedAt });
    } else if (/\.(png|jpe?g|webp)$/i.test(filename)) {
      const bytes = await entry.async('uint8array');
      const mimeType = /\.png$/i.test(filename) ? 'image/png' : /\.webp$/i.test(filename) ? 'image/webp' : 'image/jpeg';
      await saveStoreData<StoredMapDataset>('map_datasets', { id, mapId: ERIADOR_MAP_ID, filename, kind: 'blob', blob: new Blob([bytes], { type: mimeType }), mimeType, importedAt });
    }
  }
  await saveStoreData<StoredMapDataset>('map_datasets', {
    id: `${ERIADOR_MAP_ID}:__bundled_revision__`,
    mapId: ERIADOR_MAP_ID,
    filename: '__bundled_revision__',
    kind: 'json',
    data: { revision: BUNDLED_REVISION, source: 'imported-package', packageRoot },
    importedAt,
  });
  await saveStoreData<StoredMapDataset>('map_datasets', {
    id: `${ERIADOR_MAP_ID}:${BASE_IMAGE_DATASET_FILENAME}`,
    mapId: ERIADOR_MAP_ID,
    filename: BASE_IMAGE_DATASET_FILENAME,
    kind: 'base-image',
    blob: imageBlob,
    width: imageWidth,
    height: imageHeight,
    mimeType: 'image/png',
    importedAt,
  });
  await saveStoreData('map_manifests', importedManifest);
  manifestCache = importedManifest;
  locationsCache = locationsDocument.locations;
  await clearHexCropCache();
  return {
    manifest: importedManifest,
    imageBlob,
    imageWidth,
    imageHeight,
    importedFiles: [...Array.from(entries.keys()), packageRoot ? `${dirname(packageRoot)}/map.png` : 'map.png'],
    locationCount: locationsDocument.locations.length,
  };
}

export function shortHexLabel(hex: Pick<EriadorHex, 'id'>): string {
  return hex.id;
}

export interface HexNarrativeLocation {
  name: string;
  regionName: string;
  terrainName: string;
  source: 'verified-override' | 'exact-location' | 'manifest' | 'regional-anchor' | 'terrain' | 'unresolved';
  confidence: number;
  exactLocationIds: string[];
  regionalAnchorId?: string;
  regionalAnchorDistance?: number;
}

const VERIFIED_HEX_NARRATIVE_OVERRIDES: Record<string, Pick<HexNarrativeLocation, 'name' | 'regionName' | 'terrainName' | 'confidence'>> = {
  // O rótulo impresso “A Campina” atravessa BN45. “Rohan” está ancorado em
  // BI55 e não pode substituir o nome local desta área do mapa.
  BN45: { name: 'A Campina', regionName: 'A Campina', terrainName: 'campinas e pradarias', confidence: 0.99 },
};

function normalizedCartographicText(value: unknown): string {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function isRegionalLocation(location: TerraMediaLocation): boolean {
  const text = normalizedCartographicText([location.type, ...location.categories].join(' '));
  return /\b(regiao|reino|terra|region|realm)\b/.test(text);
}

function exactLocationPriority(location: TerraMediaLocation): number {
  const text = normalizedCartographicText([location.type, ...location.categories, ...location.tags].join(' '));
  const precisionBonus = location.placement.precision === 'printed-symbol-center' ? 25 : location.placement.status === 'verified-label' ? 12 : 0;
  const kind = /cidade|vila|aldeia|povoado|porto|city|town|village|port/.test(text) ? 100
    : /fortaleza|castelo|forte|cidadela|fortress|castle|keep/.test(text) ? 92
      : /refugio|salao|estalagem|taverna|inn|hall/.test(text) ? 86
        : /ruina|monumento|marco|ponte|vau|ruin|monument|bridge/.test(text) ? 78
          : isRegionalLocation(location) ? 70
            : /floresta|bosque|montanha|colina|pantano|forest|mountain|marsh/.test(text) ? 62
              : /rio|lago|mar|baia|river|lake|sea|bay/.test(text) ? 42 : 50;
  return kind + precisionBonus + Math.round(Number(location.placement.confidence || 0) * 10);
}

function hexDistancesFrom(hex: EriadorHex, byId: Map<string, EriadorHex>, maxRing = 12): Map<string, number> {
  const distances = new Map<string, number>([[hex.id, 0]]);
  const queue = [hex.id];
  let cursor = 0;
  while (cursor < queue.length) {
    const id = queue[cursor++];
    const distance = distances.get(id)!;
    if (distance >= maxRing) continue;
    for (const neighborId of byId.get(id)?.neighbors || []) {
      if (distances.has(neighborId)) continue;
      distances.set(neighborId, distance + 1);
      queue.push(neighborId);
    }
  }
  return distances;
}

function readableTerrainForLocation(hex: EriadorHex, placeName: string): string {
  const explicit = String(hex.terrain || '').trim();
  if (explicit && normalizedCartographicText(explicit) !== 'unknown') return explicit;
  const text = normalizedCartographicText(`${placeName} ${hex.region || ''} ${hex.name || ''} ${hex.poi || ''}`);
  if (/campina|pampa|folde|rohan|planicie|pradaria/.test(text)) return 'campos e pradarias';
  if (/floresta|bosque|forest/.test(text)) return 'floresta';
  if (/montanha|colina|mountain|hill/.test(text)) return 'montanhas e colinas';
  if (/pantano|charneca|brejo|marsh|swamp/.test(text)) return 'pântanos e charnecas';
  if (/rio|anduin|limlume|river/.test(text)) return 'margens fluviais';
  return 'terreno ainda não catalogado';
}

export function resolveHexNarrativeLocationFromIndex(
  hex: EriadorHex,
  byId: Map<string, EriadorHex>,
  locations: TerraMediaLocation[],
): HexNarrativeLocation {
  const exactLocations = locations.filter(location => location.hexIds.includes(hex.id));
  const exactIds = exactLocations.map(location => location.id);
  const override = VERIFIED_HEX_NARRATIVE_OVERRIDES[hex.id];
  if (override) return { ...override, source: 'verified-override', exactLocationIds: exactIds };

  const exactRegion = exactLocations
    .filter(isRegionalLocation)
    .sort((a, b) => exactLocationPriority(b) - exactLocationPriority(a))[0];
  const exactLandmark = [...exactLocations].sort((a, b) => exactLocationPriority(b) - exactLocationPriority(a))[0];
  const explicitRegion = String(hex.region || '').trim();
  const explicitName = String(hex.name || hex.poi || '').trim();

  let regionName = explicitRegion || exactRegion?.name || exactLandmark?.regions?.[0] || '';
  let regionalAnchor: { location: TerraMediaLocation; distance: number } | undefined;
  if (!regionName) {
    const distances = hexDistancesFrom(hex, byId, 12);
    regionalAnchor = locations
      .filter(isRegionalLocation)
      .map(location => ({
        location,
        distance: Math.min(...location.hexIds.map(id => distances.get(id) ?? Number.POSITIVE_INFINITY)),
      }))
      .filter(item => Number.isFinite(item.distance) && item.distance <= 12)
      .sort((a, b) => a.distance - b.distance
        || Number(b.location.placement.confidence || 0) - Number(a.location.placement.confidence || 0)
        || a.location.name.length - b.location.name.length)[0];
    // Um rótulo regional próximo identifica a área pelo próprio nome impresso.
    // As regiões-pai do catálogo (por exemplo, Rohan em uma entrada de rio) não
    // são promovidas a posição atual.
    regionName = regionalAnchor?.location.name || '';
  }

  const name = exactLandmark?.name || explicitName || regionName;
  if (name) {
    const source: HexNarrativeLocation['source'] = exactLandmark ? 'exact-location' : explicitName || explicitRegion ? 'manifest' : 'regional-anchor';
    const confidence = exactLandmark
      ? Number(exactLandmark.placement.confidence || 0.8)
      : regionalAnchor ? Math.max(0.45, Number(regionalAnchor.location.placement.confidence || 0.7) - regionalAnchor.distance * 0.035) : 0.85;
    return {
      name,
      regionName: regionName || name,
      terrainName: readableTerrainForLocation(hex, regionName || name),
      source,
      confidence,
      exactLocationIds: exactIds,
      regionalAnchorId: regionalAnchor?.location.id,
      regionalAnchorDistance: regionalAnchor?.distance,
    };
  }

  const terrain = readableTerrainForLocation(hex, '');
  if (terrain !== 'terreno ainda não catalogado') {
    return { name: terrain, regionName: 'região ainda não catalogada', terrainName: terrain, source: 'terrain', confidence: 0.55, exactLocationIds: exactIds };
  }
  return {
    name: 'região ainda não catalogada',
    regionName: 'região ainda não catalogada',
    terrainName: terrain,
    source: 'unresolved',
    confidence: 0,
    exactLocationIds: exactIds,
  };
}

export function resolveHexNarrativeLocation(
  manifest: Pick<EriadorMapManifest, 'hexes'>,
  locations: TerraMediaLocation[],
  hexId: string,
): HexNarrativeLocation {
  const byId = new Map(manifest.hexes.map(hex => [hex.id, hex]));
  const hex = byId.get(hexId);
  if (!hex) return {
    name: 'região ainda não catalogada', regionName: 'região ainda não catalogada',
    terrainName: 'terreno ainda não catalogado', source: 'unresolved', confidence: 0, exactLocationIds: [],
  };
  return resolveHexNarrativeLocationFromIndex(hex, byId, locations);
}

export function validateEriadorManifest(value: unknown, imageWidth?: number, imageHeight?: number): EriadorMapManifest {
  const manifest = normalizeManifest(value as EriadorMapManifest);
  if (!manifest || manifest.id !== ERIADOR_MAP_ID || manifest.mapId !== ERIADOR_MAP_ID || manifest.filename !== 'map.png') {
    throw new Error('Este manifesto não pertence à geometria reconstruída da Terra-média.');
  }
  if (manifest.layout?.orientation !== 'pointy-top' || manifest.layout?.coordinateSystem !== 'odd-r-offset') {
    throw new Error('O manifesto deve usar grade pointy-top no sistema odd-r-offset medido no mapa original.');
  }
  if (!Array.isArray(manifest.hexes) || manifest.hexes.length !== ERIADOR_HEX_COUNT || manifest.hexCount !== ERIADOR_HEX_COUNT) {
    throw new Error(`A malha reconstruída deve conter exatamente ${ERIADOR_HEX_COUNT.toLocaleString('pt-BR')} hexes.`);
  }
  if (manifest.image.width !== ERIADOR_IMAGE_WIDTH || manifest.image.height !== ERIADOR_IMAGE_HEIGHT) {
    throw new Error(`O manifesto deve usar a imagem ${ERIADOR_IMAGE_WIDTH}×${ERIADOR_IMAGE_HEIGHT}.`);
  }
  if (imageWidth && imageHeight && (manifest.image.width !== imageWidth || manifest.image.height !== imageHeight)) {
    throw new Error(`O manifesto espera ${manifest.image.width}×${manifest.image.height}, mas o PNG possui ${imageWidth}×${imageHeight}.`);
  }

  const ids = new Set<string>();
  const coords = new Set<string>();
  for (const hex of manifest.hexes) {
    if (!hex.id || ids.has(hex.id)) throw new Error(`ID de hex duplicado ou inválido: ${hex.id || '(vazio)'}.`);
    const coord = `${hex.r}:${hex.q}`;
    if (coords.has(coord)) throw new Error(`Coordenada de hex duplicada: ${coord}.`);
    if (!Array.isArray(hex.polygon) || hex.polygon.length !== 6) throw new Error(`Polígono inválido em ${hex.id}.`);
    if (!Number.isFinite(hex.centerX) || !Number.isFinite(hex.centerY)) throw new Error(`Centro inválido em ${hex.id}.`);
    ids.add(hex.id);
    coords.add(coord);
  }
  for (const hex of manifest.hexes) {
    for (const neighborId of hex.neighbors) {
      if (!ids.has(neighborId)) throw new Error(`Vizinho inexistente ${neighborId} em ${hex.id}.`);
    }
  }
  const byId = new Map(manifest.hexes.map(hex => [hex.id, hex]));
  for (const hex of manifest.hexes) {
    for (const neighborId of hex.neighbors) {
      if (!byId.get(neighborId)?.neighbors.includes(hex.id)) {
        throw new Error(`Vizinhança assimétrica entre ${hex.id} e ${neighborId}.`);
      }
    }
  }
  return manifest;
}

export function pointInPolygon(point: MapPoint, polygon: MapPoint[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i];
    const b = polygon[j];
    const crosses = ((a.y > point.y) !== (b.y > point.y))
      && point.x < ((b.x - a.x) * (point.y - a.y)) / ((b.y - a.y) || Number.EPSILON) + a.x;
    if (crosses) inside = !inside;
  }
  return inside;
}

export function findHexAtPoint(manifest: EriadorMapManifest, point: MapPoint): EriadorHex | null {
  // A malha é regular; restringir primeiro pela caixa evita testar 12 mil polígonos em cada toque.
  const approximateRow = Math.round((point.y - manifest.layout.origin.y) / manifest.layout.rowStep);
  for (let r = approximateRow - 1; r <= approximateRow + 1; r += 1) {
    if (r < 0) continue;
    const rowOffset = r % 2 ? manifest.layout.oddRowOffsetX : 0;
    const approximateColumn = Math.round((point.x - manifest.layout.origin.x - rowOffset) / manifest.layout.horizontalStep);
    for (let c = approximateColumn - 1; c <= approximateColumn + 1; c += 1) {
      const hex = manifest.hexes.find(item => item.r === r && item.q === c);
      if (hex?.enabled && pointInPolygon(point, hex.polygon)) return hex;
    }
  }
  return null;
}

export function areManifestNeighbors(_manifest: EriadorMapManifest, a: EriadorHex, b: EriadorHex): boolean {
  if (a.id === b.id) return false;
  return Boolean(a.neighbors?.includes(b.id) && b.neighbors?.includes(a.id));
}

function mergeEditableMetadata(authority: EriadorMapManifest, saved?: EriadorMapManifest | null): EriadorMapManifest {
  if (!saved || saved.mapId !== authority.mapId) return authority;
  const savedById = new Map(saved.hexes.map(hex => [hex.id, hex]));
  const now = Date.now();
  return normalizeManifest({
    ...authority,
    hexes: authority.hexes.map(hex => {
      const old = savedById.get(hex.id);
      if (!old) return hex;
      // Centros, polígonos, recortes, IDs e vizinhos nunca vêm do save antigo.
      return {
        ...hex,
        enabled: old.enabled !== false,
        playable: old.playable !== false,
        terrain: terrainName(old.terrain || hex.terrain),
        region: old.region || hex.region,
        name: old.name || hex.name,
        poi: old.poi || hex.poi,
        placeType: old.placeType || hex.placeType,
        confidence: typeof old.confidence === 'number' ? old.confidence : hex.confidence,
        tags: Array.isArray(old.tags) ? old.tags : hex.tags,
      };
    }),
    revision: Math.max(Number(authority.revision || 0), Number(saved.revision || 0)) + 1,
    createdAt: saved.createdAt || authority.createdAt || now,
    updatedAt: now,
  });
}

export async function importBundledTerraMediaDatasets(): Promise<void> {
  const revisionId = `${ERIADOR_MAP_ID}:__bundled_revision__`;
  // Nunca leia a store inteira aqui. Ela pode conter o PNG 10.630 x 7.678 do
  // usuário; getAll() materializava esse Blob de dezenas de MB durante o boot.
  const revisionRow = await getStoreItem<StoredMapDataset>('map_datasets', revisionId);
  const revision = revisionRow?.data as { revision?: number } | undefined;
  if (revision?.revision === BUNDLED_REVISION) return;
  for (const filename of BUNDLED_MAP_DATASETS) {
    try {
      const data = await readBundledJson<unknown>(filename);
      await saveStoreData<StoredMapDataset>('map_datasets', {
        id: `${ERIADOR_MAP_ID}:${filename}`,
        mapId: ERIADOR_MAP_ID,
        filename,
        kind: 'json',
        data,
        importedAt: Date.now(),
      });
    } catch (error) {
      console.warn(`BRASEIRO_MAPA: dataset integrado indisponível: ${filename}`, error);
    }
  }
  await saveStoreData<StoredMapDataset>('map_datasets', {
    id: revisionId,
    mapId: ERIADOR_MAP_ID,
    filename: '__bundled_revision__',
    kind: 'json',
    data: { revision: BUNDLED_REVISION },
    importedAt: Date.now(),
  });
}

export async function loadMapManifest(): Promise<EriadorMapManifest | null> {
  if (manifestCache) return manifestCache;
  const records = await getStoreData<EriadorMapManifest>('map_manifests');
  const saved = records.find(record => record.id === ERIADOR_MAP_ID) || null;
  try {
    const authority = await loadBundledFixedManifest();
    const fixed = mergeEditableMetadata(authority, saved);
    // A imagem importada já é validada no momento da importação. Decodificá-la
    // novamente aqui consumia mais de 300 MB em RGBA e podia matar/congelar o
    // renderer da WebView antes do primeiro quadro.
    await saveStoreData('map_manifests', fixed);
    await importBundledTerraMediaDatasets();
    manifestCache = fixed;
    return fixed;
  } catch (error) {
    console.error('BRASEIRO_MAPA: falha ao carregar a geometria reconstruída', error);
    if (saved) {
      try {
        manifestCache = validateEriadorManifest(saved, ERIADOR_IMAGE_WIDTH, ERIADOR_IMAGE_HEIGHT);
        return manifestCache;
      } catch {
        console.warn('BRASEIRO_MAPA: manifesto salvo incompatível ignorado.');
      }
    }
    return null;
  }
}

export async function loadTerraMediaLocations(): Promise<TerraMediaLocation[]> {
  if (locationsCache) return locationsCache;
  const manifest = await loadMapManifest();
  if (!manifest) return [];
  const storedRow = await getStoreItem<StoredMapDataset>('map_datasets', `${ERIADOR_MAP_ID}:terra_media_locations_integrated.json`);
  const stored = storedRow?.mapId === ERIADOR_MAP_ID ? storedRow.data : undefined;
  try {
    const raw = stored || await readBundledJson<TerraMediaLocationsDocument>('terra_media_locations_integrated.json');
    locationsCache = validateLocationsDocument(raw, manifest).locations;
    return locationsCache;
  } catch (error) {
    console.error('BRASEIRO_MAPA: catálogo de locais inválido', error);
    return [];
  }
}

/** Catálogo de locais do boot/imersivo sem depender do espelho pesado do mapa em IndexedDB. */
export async function loadRuntimeTerraMediaLocations(): Promise<TerraMediaLocation[]> {
  if (runtimeLocationsCache) return runtimeLocationsCache;
  try {
    const manifest = await loadRuntimeMapManifest();
    const raw = await readBundledJson<TerraMediaLocationsDocument>('terra_media_locations_integrated.json');
    runtimeLocationsCache = validateLocationsDocument(raw, manifest).locations;
    return runtimeLocationsCache;
  } catch (error) {
    console.error('BRASEIRO_MAPA: catálogo runtime de locais inválido', error);
    return [];
  }
}

export async function loadTerraMediaRuntimeContext(hexId: string): Promise<{
  hexId: string;
  locations: TerraMediaLocation[];
  exactLocations: TerraMediaLocation[];
  derivedLocations: TerraMediaLocation[];
}> {
  const locations = (await loadRuntimeTerraMediaLocations()).filter(location => location.hexIds.includes(hexId));
  return {
    hexId,
    locations,
    exactLocations: locations.filter(location => location.placement.status === 'verified-label'),
    derivedLocations: locations.filter(location => location.placement.status !== 'verified-label'),
  };
}

export async function saveMapManifest(manifest: EriadorMapManifest): Promise<void> {
  const authority = await loadBundledFixedManifest();
  const fixed = mergeEditableMetadata(authority, manifest);
  validateEriadorManifest(fixed, ERIADOR_IMAGE_WIDTH, ERIADOR_IMAGE_HEIGHT);
  await saveStoreData('map_manifests', fixed);
  manifestCache = fixed;
}

export function emptyDiscovery(campaignId: string): MapDiscovery {
  return {
    id: `${campaignId}:${ERIADOR_MAP_ID}`,
    campaignId,
    mapId: ERIADOR_MAP_ID,
    currentHexId: null,
    discoveredHexIds: [],
    visitedHexIds: [],
    routeHexIds: [],
    plannedRouteHexIds: [],
    fatigue: 0,
    updatedAt: Date.now(),
  };
}

export async function loadMapDiscovery(campaignId: string): Promise<MapDiscovery> {
  const records = await getStoreData<MapDiscovery>('map_discovery');
  const found = records.find(record => record.id === `${campaignId}:${ERIADOR_MAP_ID}`);
  if (!found) return emptyDiscovery(campaignId);
  const visited = Array.isArray(found.visitedHexIds) ? found.visitedHexIds : [];
  const route = Array.isArray(found.routeHexIds) && found.routeHexIds.length ? found.routeHexIds : visited;
  const discovered = Array.isArray(found.discoveredHexIds) ? found.discoveredHexIds : [];
  const plannedRouteHexIds = Array.isArray(found.plannedRouteHexIds) ? found.plannedRouteHexIds.filter(Boolean) : [];
  const currentHexId = found.currentHexId || route.at(-1) || visited.at(-1) || discovered.at(-1) || null;
  return {
    ...emptyDiscovery(campaignId),
    ...found,
    mapId: ERIADOR_MAP_ID,
    currentHexId,
    discoveredHexIds: discovered,
    visitedHexIds: visited,
    routeHexIds: route,
    plannedRouteHexIds,
  };
}

export async function saveMapDiscovery(discovery: MapDiscovery): Promise<void> {
  const updated = { ...discovery, mapId: ERIADOR_MAP_ID, updatedAt: Date.now() };
  await saveStoreData('map_discovery', updated);
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: updated.campaignId, mapId: updated.mapId, currentHexId: updated.currentHexId },
    }));
  }
}


/** Finds a playable shortest path. The returned array includes start and destination. */
export function findShortestPlayablePath(
  manifest: EriadorMapManifest,
  startHexId: string,
  destinationHexId: string,
  maxVisited = 20_000,
): string[] {
  if (startHexId === destinationHexId) return [startHexId];
  const byId = new Map(manifest.hexes.map(hex => [hex.id, hex]));
  const start = byId.get(startHexId);
  const destination = byId.get(destinationHexId);
  if (!start || !destination || !destination.enabled || !destination.playable) return [];
  const queue = [startHexId];
  const previous = new Map<string, string | null>([[startHexId, null]]);
  let cursor = 0;
  while (cursor < queue.length && previous.size <= maxVisited) {
    const id = queue[cursor++];
    const hex = byId.get(id);
    if (!hex) continue;
    for (const neighborId of hex.neighbors || []) {
      if (previous.has(neighborId)) continue;
      const neighbor = byId.get(neighborId);
      if (!neighbor || !neighbor.enabled || !neighbor.playable) continue;
      if (neighbor.terrain === 'water' || neighbor.terrain === 'impassable') continue;
      previous.set(neighborId, id);
      if (neighborId === destinationHexId) {
        const path: string[] = [destinationHexId];
        let step: string | null = id;
        while (step) { path.push(step); step = previous.get(step) || null; }
        return path.reverse();
      }
      queue.push(neighborId);
    }
  }
  return [];
}

export async function createHexCrop(image: HTMLImageElement, hex: EriadorHex): Promise<Blob> {
  const geometrySignature = [
    image.naturalWidth, image.naturalHeight,
    hex.crop.x, hex.crop.y, hex.crop.width, hex.crop.height,
    ...hex.polygon.flatMap(point => [point.x, point.y]),
  ].map(value => Number(value).toFixed(2)).join(':');
  const cached = (await getStoreData<MapHexCrop>('map_hex_crops')).find(item => item.hexId === hex.id && item.mapId === ERIADOR_MAP_ID);
  if (cached?.blob && cached.geometrySignature === geometrySignature) return cached.blob;
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.ceil(hex.crop.width));
  canvas.height = Math.max(1, Math.ceil(hex.crop.height));
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas indisponível para recortar o hex.');
  ctx.beginPath();
  hex.polygon.forEach((point, index) => {
    const x = point.x - hex.crop.x;
    const y = point.y - hex.crop.y;
    if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(image, hex.crop.x, hex.crop.y, hex.crop.width, hex.crop.height, 0, 0, canvas.width, canvas.height);
  const blob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(result => result ? resolve(result) : reject(new Error('Falha ao gerar o recorte.')), 'image/png');
  });
  await saveStoreData<MapHexCrop>('map_hex_crops', {
    id: `${ERIADOR_MAP_ID}:${hex.id}`,
    mapId: ERIADOR_MAP_ID,
    hexId: hex.id,
    geometrySignature,
    blob,
    createdAt: Date.now(),
  });
  return blob;
}

export async function createHexAnalysisCrop(image: HTMLImageElement, hex: EriadorHex, expansion = 2.6): Promise<Blob> {
  const sourceWidth = Math.min(image.naturalWidth, Math.max(hex.crop.width, hex.crop.width * expansion));
  const sourceHeight = Math.min(image.naturalHeight, Math.max(hex.crop.height, hex.crop.height * expansion));
  const sourceX = Math.max(0, Math.min(image.naturalWidth - sourceWidth, hex.centerX - sourceWidth / 2));
  const sourceY = Math.max(0, Math.min(image.naturalHeight - sourceHeight, hex.centerY - sourceHeight / 2));
  const canvas = document.createElement('canvas');
  const targetMax = 768;
  const scaleFactor = Math.min(targetMax / sourceWidth, targetMax / sourceHeight, 2);
  canvas.width = Math.max(1, Math.round(sourceWidth * scaleFactor));
  canvas.height = Math.max(1, Math.round(sourceHeight * scaleFactor));
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas indisponível para preparar a análise do terreno.');
  ctx.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, canvas.width, canvas.height);
  ctx.save();
  ctx.strokeStyle = 'rgba(255, 196, 64, 0.95)';
  ctx.lineWidth = Math.max(2, 3 * scaleFactor);
  ctx.setLineDash([8 * scaleFactor, 5 * scaleFactor]);
  ctx.beginPath();
  hex.polygon.forEach((point, index) => {
    const x = (point.x - sourceX) * scaleFactor;
    const y = (point.y - sourceY) * scaleFactor;
    if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.closePath();
  ctx.stroke();
  ctx.restore();
  return await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(result => result ? resolve(result) : reject(new Error('Falha ao gerar o recorte contextual.')), 'image/png', 0.95);
  });
}

export async function clearHexCropCache(): Promise<void> {
  await clearStore('map_hex_crops');
}

export async function removeHexCrop(hexId: string): Promise<void> {
  await deleteStoreData('map_hex_crops', `${ERIADOR_MAP_ID}:${hexId}`);
}
