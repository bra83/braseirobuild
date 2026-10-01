import JSZip from 'jszip';
import type { DriveFileInfo } from './drive';
import { downloadDriveFile, listDriveFolder } from './drive';
import { applyStoreChangesAtomically, getStoreData, type IndividualToken, type TokenSheet } from './db';
import { TOKENS_DRIVE_FOLDER_ID } from './driveFolders';
import { oneRingAutomaticTokenSheetAllowed, oneRingTokenSheetAllowed } from './tokenSheetPolicy';
export { oneRingAutomaticTokenSheetAllowed, oneRingTokenSheetAllowed } from './tokenSheetPolicy';

export interface ManifestClassification {
  category?: string;
  entityType?: string;
  cultures?: string[];
  genders?: string[];
  ageGroups?: string[];
  environment?: string[];
  roles?: string[];
  tags?: string[];
  creatureTypes?: string[];
  people?: string;
  gender?: string;
}

export interface ManifestTokenSheet extends Omit<TokenSheet, 'classification' | 'createdAt'> {
  enabled?: boolean;
  excludeFromAutomaticSelection?: boolean;
  classification?: ManifestClassification;
  layout: TokenSheet['layout'] & { mask?: string; order?: string };
  createdAt?: number;
}

export interface TokenManifest {
  manifestVersion?: string;
  sheetCount?: number;
  totalTokenCount?: number;
  sheets: ManifestTokenSheet[];
}

export interface TokenSheetImportProgress {
  stage: 'manifest' | 'downloading' | 'cropping' | 'saving' | 'done';
  message: string;
  current?: number;
  total?: number;
  filename?: string;
}

export interface TokenSheetImportResult {
  manifestVersion: string;
  sheetCount: number;
  tokenCount: number;
  blockedSheets: string[];
  sheets: ManifestTokenSheet[];
}

export function filterOneRingTokenManifest(manifest: TokenManifest): { allowed: ManifestTokenSheet[]; blocked: ManifestTokenSheet[] } {
  const sheets = Array.isArray(manifest?.sheets) ? manifest.sheets : [];
  return {
    allowed: sheets.filter(oneRingTokenSheetAllowed),
    blocked: sheets.filter(sheet => sheet.enabled !== false && !oneRingTokenSheetAllowed(sheet)),
  };
}

export function cropPositions(sheet: ManifestTokenSheet): Array<{ index: number; x: number; y: number; width: number; height: number }> {
  const layout = sheet.layout;
  if (layout.type === 'explicit' && layout.tokens?.length) return layout.tokens;
  if (layout.type !== 'grid-centers' || !layout.xCenters?.length || !layout.yCenters?.length || !layout.cropWidth || !layout.cropHeight) {
    throw new Error(`Layout inválido na folha ${sheet.id}.`);
  }
  const positions: Array<{ index: number; x: number; y: number; width: number; height: number }> = [];
  const expected = Number(sheet.tokenCount || 0);
  const cartesianCount = layout.xCenters.length * layout.yCenters.length;
  const useCartesian = cartesianCount === expected
    || (!!layout.columns && !!layout.rows && layout.columns * layout.rows === expected)
    || layout.xCenters.length !== layout.yCenters.length;
  if (useCartesian) {
    let index = 1;
    for (const y of layout.yCenters) {
      for (const x of layout.xCenters) {
        if (expected && index > expected) break;
        positions.push({ index, x: x - layout.cropWidth / 2, y: y - layout.cropHeight / 2, width: layout.cropWidth, height: layout.cropHeight });
        index += 1;
      }
    }
  } else {
    const count = Math.min(layout.xCenters.length, layout.yCenters.length, expected || Number.MAX_SAFE_INTEGER);
    for (let i = 0; i < count; i += 1) positions.push({
      index: i + 1,
      x: layout.xCenters[i] - layout.cropWidth / 2,
      y: layout.yCenters[i] - layout.cropHeight / 2,
      width: layout.cropWidth,
      height: layout.cropHeight,
    });
  }
  return positions;
}

async function loadImage(blob: Blob): Promise<HTMLImageElement> {
  const url = URL.createObjectURL(blob);
  try {
    const image = new Image();
    image.src = url;
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve();
      image.onerror = () => reject(new Error('A imagem de tokens não pôde ser aberta.'));
    });
    return image;
  } finally {
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}

async function cropToken(image: HTMLImageElement, crop: { x: number; y: number; width: number; height: number }, mask?: string): Promise<Blob> {
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(crop.width);
  canvas.height = Math.round(crop.height);
  const context = canvas.getContext('2d');
  if (!context) throw new Error('Canvas indisponível para recortar tokens.');
  context.save();
  if (/circle|circular/i.test(mask || '')) {
    context.beginPath();
    context.arc(canvas.width / 2, canvas.height / 2, Math.min(canvas.width, canvas.height) / 2, 0, Math.PI * 2);
    context.clip();
  }
  context.drawImage(image, crop.x, crop.y, crop.width, crop.height, 0, 0, canvas.width, canvas.height);
  context.restore();
  return new Promise<Blob>((resolve, reject) => canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('Falha ao gerar recorte.')), 'image/png'));
}

async function expandSelectedFiles(selected: File[]): Promise<File[]> {
  const expanded: File[] = [];
  for (const file of selected) {
    if (/\.zip$/i.test(file.name) || /zip/i.test(file.type)) {
      const zip = await JSZip.loadAsync(file);
      for (const [path, entry] of Object.entries(zip.files)) {
        if (entry.dir) continue;
        const blob = await entry.async('blob');
        const name = path.replace(/\\/g, '/').split('/').pop() || path;
        expanded.push(new File([blob], name, { type: blob.type || '' }));
      }
    } else expanded.push(file);
  }
  return expanded;
}

function storedSheet(sheet: ManifestTokenSheet): TokenSheet & Record<string, unknown> {
  return { ...sheet, classification: sheet.classification || {}, createdAt: Date.now() } as TokenSheet & Record<string, unknown>;
}

async function replaceTokenSheetStores(sheets: ManifestTokenSheet[], tokens: IndividualToken[]): Promise<void> {
  const previousSheets = await getStoreData<TokenSheet>('token_sheets').catch(() => []);
  const previousSheetIds = new Set(previousSheets.map(sheet => sheet.id));
  const allTokens = await getStoreData<IndividualToken>('individual_tokens').catch(() => []);
  const oldLibraryTokenIds = allTokens.filter(token => previousSheetIds.has(token.sheetId)).map(token => token.id);
  await applyStoreChangesAtomically({
    token_sheets: { clear: true, put: sheets.map(storedSheet) },
    individual_tokens: { delete: oldLibraryTokenIds, put: tokens },
  });
}

async function prepareImport(manifest: TokenManifest, filesByName: Map<string, Blob>, onProgress?: (progress: TokenSheetImportProgress) => void): Promise<TokenSheetImportResult> {
  if (!Array.isArray(manifest.sheets) || !manifest.sheets.length) throw new Error('O manifesto não contém folhas de tokens.');
  const { allowed, blocked } = filterOneRingTokenManifest(manifest);
  if (!allowed.length) throw new Error('Nenhuma folha de token compatível com O Um Anel passou pelo firewall de assets.');
  const missing = allowed.map(sheet => sheet.filename).filter(filename => !filesByName.has(filename));
  if (missing.length) throw new Error(`Faltam imagens exigidas pelo manifesto: ${missing.join(', ')}`);
  const preparedTokens: IndividualToken[] = [];
  let sheetNumber = 0;
  for (const sheet of allowed) {
    sheetNumber += 1;
    onProgress?.({ stage: 'cropping', message: `Recortando ${sheet.filename} (${sheetNumber}/${allowed.length})...`, current: sheetNumber, total: allowed.length, filename: sheet.filename });
    const image = await loadImage(filesByName.get(sheet.filename)!);
    for (const crop of cropPositions(sheet)) {
      const blob = await cropToken(image, crop, sheet.layout.mask);
      preparedTokens.push({
        id: `${sheet.id}#${String(crop.index).padStart(3, '0')}`,
        sheetId: sheet.id,
        tokenIndex: crop.index,
        blob,
        category: sheet.classification?.category || 'npc',
        createdAt: Date.now(),
      });
    }
  }
  onProgress?.({ stage: 'saving', message: 'Atualizando a biblioteca sem apagar retratos de heróis ou NPCs gerados...' });
  await replaceTokenSheetStores(allowed, preparedTokens);
  const result = {
    manifestVersion: String(manifest.manifestVersion || 'unknown'),
    sheetCount: allowed.length,
    tokenCount: preparedTokens.length,
    blockedSheets: blocked.map(sheet => sheet.filename),
    sheets: allowed,
  };
  onProgress?.({ stage: 'done', message: `${result.tokenCount} tokens de ${result.sheetCount} folhas importados; ${result.blockedSheets.length} folhas externas bloqueadas.` });
  return result;
}

export async function importOneRingTokenPackage(selected: File[], onProgress?: (progress: TokenSheetImportProgress) => void): Promise<TokenSheetImportResult> {
  onProgress?.({ stage: 'manifest', message: 'Lendo manifesto e pacote de tokens...' });
  const files = await expandSelectedFiles(selected);
  const manifestFile = files.find(file => file.name === 'tokens_manifest.json');
  if (!manifestFile) throw new Error('tokens_manifest.json não encontrado.');
  const manifest = JSON.parse(await manifestFile.text()) as TokenManifest;
  const byName = new Map<string, Blob>(files.map(file => [file.name, file]));
  return prepareImport(manifest, byName, onProgress);
}

export async function importOneRingTokenLibraryFromDrive(onProgress?: (progress: TokenSheetImportProgress) => void): Promise<TokenSheetImportResult> {
  onProgress?.({ stage: 'manifest', message: 'Lendo o manifesto de tokens no Google Drive...' });
  const metadata = await listDriveFolder(TOKENS_DRIVE_FOLDER_ID);
  const manifestInfo = metadata.find(file => file.name === 'tokens_manifest.json');
  if (!manifestInfo) throw new Error('tokens_manifest.json não encontrado na pasta de tokens configurada.');
  const manifestBlob = await downloadDriveFile(manifestInfo.id, manifestInfo.mimeType);
  const manifest = JSON.parse(await manifestBlob.text()) as TokenManifest;
  const { allowed } = filterOneRingTokenManifest(manifest);
  const byName = new Map<string, Blob>();
  let current = 0;
  for (const sheet of allowed) {
    const info = metadata.find(file => file.name === sheet.filename);
    if (!info) throw new Error(`A imagem ${sheet.filename} não foi encontrada no Google Drive.`);
    current += 1;
    onProgress?.({ stage: 'downloading', message: `Baixando ${sheet.filename}...`, current, total: allowed.length, filename: sheet.filename });
    byName.set(sheet.filename, await downloadDriveFile(info.id, info.mimeType));
  }
  return prepareImport(manifest, byName, onProgress);
}

export async function clearTokenSheetLibrary(): Promise<number> {
  const sheets = await getStoreData<TokenSheet>('token_sheets').catch(() => []);
  const sheetIds = new Set(sheets.map(sheet => sheet.id));
  const tokens = await getStoreData<IndividualToken>('individual_tokens').catch(() => []);
  const deleteIds = tokens.filter(token => sheetIds.has(token.sheetId)).map(token => token.id);
  await applyStoreChangesAtomically({ token_sheets: { clear: true }, individual_tokens: { delete: deleteIds } });
  return deleteIds.length;
}

export function driveInfoByFilename(files: DriveFileInfo[]): Map<string, DriveFileInfo> {
  return new Map(files.map(file => [file.name, file]));
}
