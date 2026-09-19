import firebaseConfig from '../firebase-applet-config.json';
import { getAccessToken, googleSignIn, invalidateAccessToken } from './auth';

export interface DriveFileInfo {
  id: string;
  name: string;
  mimeType: string;
  size?: string;
  modifiedTime?: string;
  md5Checksum?: string;
  parents?: string[];
}

export interface DriveTreeFileInfo extends DriveFileInfo {
  parentFolderId: string;
  relativePath: string;
  depth: number;
}

const DRIVE_API = 'https://www.googleapis.com/drive/v3';

async function requestDrive(url: string, token: string | null): Promise<Response> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 20_000);
  try {
    return await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      signal: controller.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('O Google Drive não respondeu em 20 segundos. Tente novamente pela Biblioteca de Manuais.');
    }
    throw error;
  } finally {
    window.clearTimeout(timeout);
  }
}

async function tokenOrAuthorize(): Promise<string> {
  const cached = await getAccessToken();
  if (cached) return cached;
  const result = await googleSignIn();
  return result.accessToken;
}

/**
 * Lista uma pasta compartilhada primeiro sem login, como no projeto funcional.
 * Quando a pasta é privada, pede a autorização nativa e repete a operação.
 */
export async function listDriveFolder(folderId: string): Promise<DriveFileInfo[]> {
  const execute = async (token: string | null) => {
    const params = new URLSearchParams({
      q: `'${folderId}' in parents and trashed = false`,
      fields: 'nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum,parents)',
      pageSize: '1000',
      orderBy: 'name',
    });
    if (!token && firebaseConfig.apiKey) params.set('key', firebaseConfig.apiKey);

    const all: DriveFileInfo[] = [];
    let pageToken = '';
    do {
      if (pageToken) params.set('pageToken', pageToken);
      const response = await requestDrive(`${DRIVE_API}/files?${params}`, token);
      if (!response.ok) return { response, files: all, next: '' };
      const data = await response.json();
      all.push(...(data.files || []));
      pageToken = data.nextPageToken || '';
    } while (pageToken);
    return { response: new Response(null, { status: 200 }), files: all, next: '' };
  };

  let token = await getAccessToken();
  let result = await execute(token);
  if (result.response.ok) return result.files;

  // Um token vencido ou sem a permissão correta não pode impedir a tentativa pública.
  if (token && (result.response.status === 401 || result.response.status === 403)) {
    invalidateAccessToken();
    token = null;
    result = await execute(null);
    if (result.response.ok) return result.files;
  }

  // Pasta pública: a tentativa sem login usa a mesma chave do projeto funcional.
  // Pasta privada: abrir autorização nativa e repetir.
  token = await tokenOrAuthorize();
  result = await execute(token);
  if (!result.response.ok) {
    const detail = await result.response.text().catch(() => '');
    if (result.response.status === 401) invalidateAccessToken();
    throw new Error(`Google Drive recusou a listagem (HTTP ${result.response.status}). ${detail}`.trim());
  }
  return result.files;
}

export async function downloadDriveFile(fileId: string, mimeType?: string): Promise<Blob> {
  const isGoogleDoc = mimeType === 'application/vnd.google-apps.document';
  const buildUrl = (token: string | null) => {
    const params = new URLSearchParams();
    if (!token && firebaseConfig.apiKey) params.set('key', firebaseConfig.apiKey);
    if (isGoogleDoc) {
      params.set('mimeType', 'text/plain');
      return `${DRIVE_API}/files/${fileId}/export?${params}`;
    }
    params.set('alt', 'media');
    return `${DRIVE_API}/files/${fileId}?${params}`;
  };

  let token = await getAccessToken();
  let response = await requestDrive(buildUrl(token), token);
  if (response.ok) return response.blob();

  if (token && (response.status === 401 || response.status === 403)) {
    invalidateAccessToken();
    token = null;
    response = await requestDrive(buildUrl(null), null);
    if (response.ok) return response.blob();
  }

  token = await tokenOrAuthorize();
  response = await requestDrive(buildUrl(token), token);
  if (!response.ok) {
    const detail = await response.text().catch(() => '');
    if (response.status === 401) invalidateAccessToken();
    throw new Error(`Google Drive recusou o download (HTTP ${response.status}). ${detail}`.trim());
  }
  return response.blob();
}


/** Localiza uma subpasta direta pelo nome. Falha em caso de duplicidade para não ler a pasta errada. */
export async function findDriveChildFolder(parentFolderId: string, folderName: string): Promise<DriveFileInfo> {
  const wanted = folderName.trim().toLocaleLowerCase('pt-BR');
  const children = await listDriveFolder(parentFolderId);
  const matches = children.filter(file =>
    file.mimeType === 'application/vnd.google-apps.folder'
    && file.name.trim().toLocaleLowerCase('pt-BR') === wanted
  );
  if (!matches.length) {
    throw new Error(`A subpasta "${folderName}" não foi encontrada dentro da pasta de tokens configurada.`);
  }
  if (matches.length > 1) {
    throw new Error(`Existem ${matches.length} subpastas chamadas "${folderName}". Mantenha apenas uma para evitar uma sincronização ambígua.`);
  }
  return matches[0];
}

/**
 * Percorre uma árvore do Drive com profundidade limitada. Arquivos retornam o caminho
 * relativo à pasta raiz, permitindo organizar a biblioteca em subpastas sem perder os nomes.
 */
export async function listDriveFolderRecursive(
  rootFolderId: string,
  options: {
    maxDepth?: number;
    includeFolders?: boolean;
    shouldTraverseFolder?: (name: string, relativePath: string, depth: number) => boolean;
  } = {},
): Promise<DriveTreeFileInfo[]> {
  const maxDepth = Math.max(0, Math.min(12, options.maxDepth ?? 6));
  const output: DriveTreeFileInfo[] = [];
  const queue: Array<{ id: string; path: string; depth: number }> = [{ id: rootFolderId, path: '', depth: 0 }];
  const visited = new Set<string>();

  while (queue.length) {
    const current = queue.shift()!;
    if (visited.has(current.id)) continue;
    visited.add(current.id);
    const children = await listDriveFolder(current.id);
    for (const child of children) {
      const relativePath = current.path ? `${current.path}/${child.name}` : child.name;
      const record: DriveTreeFileInfo = {
        ...child,
        parentFolderId: current.id,
        relativePath,
        depth: current.depth,
      };
      const isFolder = child.mimeType === 'application/vnd.google-apps.folder';
      if (!isFolder || options.includeFolders) output.push(record);
      if (isFolder && current.depth < maxDepth && (options.shouldTraverseFolder?.(child.name, relativePath, current.depth + 1) ?? true)) {
        queue.push({ id: child.id, path: relativePath, depth: current.depth + 1 });
      }
    }
  }
  return output;
}
