export interface ImmersiveAssetManifest {
  schemaVersion: number;
  packId: string;
  basePath: string;
  assets: Record<string, string>;
  poi?: Record<string, string>;
  poiByHex?: Record<string, string[]>;
}

let manifestPromise: Promise<ImmersiveAssetManifest | null> | null = null;

const clean = (value: string) => String(value || '').replace(/^\/+|\/+$/g, '');
const joinAssetPath = (basePath: string, relativePath: string) => `./${[clean(basePath.replace(/^\.\//, '')), clean(relativePath)].filter(Boolean).join('/')}`;

export async function loadImmersiveAssetManifest(): Promise<ImmersiveAssetManifest | null> {
  if (manifestPromise) return manifestPromise;
  manifestPromise = fetch('./tor2e-ui/asset-manifest.json', { cache: 'no-store' })
    .then(async response => {
      if (!response.ok) return null;
      const raw = await response.json() as Partial<ImmersiveAssetManifest>;
      if (raw.schemaVersion !== 1 || typeof raw.basePath !== 'string' || !raw.assets) return null;
      return { schemaVersion: 1, packId: String(raw.packId || 'tor2e-immersive-ui'), basePath: raw.basePath, assets: { ...raw.assets }, poi: { ...(raw.poi || {}) }, poiByHex: { ...(raw.poiByHex || {}) } };
    })
    .catch(() => null);
  return manifestPromise;
}

export function resolveImmersiveAsset(manifest: ImmersiveAssetManifest | null, key: string): string | null {
  const relative = manifest?.assets?.[key];
  return manifest && relative ? joinAssetPath(manifest.basePath, relative) : null;
}

export function resolvePoiAsset(manifest: ImmersiveAssetManifest | null, poiId?: string | null): string | null {
  const relative = poiId ? manifest?.poi?.[poiId] : null;
  return manifest && relative ? joinAssetPath(manifest.basePath, relative) : null;
}

export function normalizePoiAssetKey(value?: string | null): string | null {
  const normalized = String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
  if (!normalized) return null;
  if (/ponei.*saltitante|prancing.*pony/.test(normalized)) return 'prancing_pony';
  if (/dol.*guldur/.test(normalized)) return 'dol_guldur';
  if (/valfenda|rivendell/.test(normalized)) return 'rivendell';
  if (/topo.*vento|amon.*sul|weathertop/.test(normalized)) return 'weathertop';
  if (/bree.*port|port.*bree/.test(normalized)) return 'bree_gate';
  return normalized;
}


export function resolvePoiAssetsForHex(manifest: ImmersiveAssetManifest | null, hexId?: string | null): string[] {
  if (!manifest || !hexId) return [];
  const relatives = manifest.poiByHex?.[hexId] || [];
  return Array.from(new Set(relatives.filter(Boolean))).map(relative => joinAssetPath(manifest.basePath, relative));
}
