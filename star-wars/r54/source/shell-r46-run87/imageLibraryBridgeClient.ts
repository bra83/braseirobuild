import type { BridgeImageAsset } from './gameBridgeClient';

export type ImageLibraryNativeAvailability = 'available' | 'unavailable' | 'invalid';
export type ImageCacheStatus = {
  assetId: string; cached: boolean; runtimeUrl?: string; sourceUrl?: string; mimeType?: string; bytes?: number; sha256?: string; cachedAt?: string;
};
export type ImageLibraryNativeEvent = { type: string; data: Record<string, unknown> };

type NativeRawResult = { ok?: unknown; error?: unknown; message?: unknown; [key:string]:unknown };
interface BraseiroImagesContract {
  getStatus: (assetId: string) => string;
  cacheRemoteImage: (assetId: string, remoteUrl: string) => string;
  clearCachedAsset: (assetId: string) => string;
}

declare global {
  interface Window {
    BraseiroImages?: BraseiroImagesContract;
    braseiroImageLibraryNativeEvent?: (raw: string) => void;
  }
}

function bridge(): unknown { return typeof window === 'undefined' ? undefined : window.BraseiroImages; }
function isBridge(value: unknown): value is BraseiroImagesContract {
  return !!value && typeof value === 'object' && typeof (value as Partial<BraseiroImagesContract>).getStatus === 'function' && typeof (value as Partial<BraseiroImagesContract>).cacheRemoteImage === 'function' && typeof (value as Partial<BraseiroImagesContract>).clearCachedAsset === 'function';
}
function parse(raw: unknown): NativeRawResult | null {
  if (typeof raw !== 'string' || !raw.trim()) return null;
  try { const value=JSON.parse(raw); return value && typeof value==='object' && !Array.isArray(value) ? value as NativeRawResult : null; } catch { return null; }
}
function normalizeId(value: unknown){ const id=String(value??'').trim(); return /^[A-Za-z0-9._:-]{1,180}$/.test(id) ? id : ''; }
function allowedRemoteUrl(value: unknown){
  try { const url=new URL(String(value??'')); return url.protocol==='https:' && url.hostname==='kainrath.wordpress.com' && url.pathname.startsWith('/wp-content/uploads/') ? url.toString() : ''; } catch { return ''; }
}

export function getImageLibraryNativeAvailability(): ImageLibraryNativeAvailability {
  const value=bridge(); if (value===undefined) return 'unavailable'; return isBridge(value) ? 'available' : 'invalid';
}
export function getImageLibraryNativeCapabilities(){ const ok=isBridge(bridge()); return {status:ok,cache:ok,clear:ok}; }

export function getImageCacheStatus(assetId: string): {ok:true;data:ImageCacheStatus}|{ok:false;message:string} {
  const id=normalizeId(assetId); if (!id) return {ok:false,message:'assetId de imagem inválido.'};
  const value=bridge(); if (!isBridge(value)) return {ok:false,message:'Cache nativo de imagens indisponível neste ambiente.'};
  const raw=parse(value.getStatus(id));
  if (!raw || raw.ok!==true || raw.assetId!==id || typeof raw.cached!=='boolean') return {ok:false,message:'Resposta inválida do cache nativo de imagens.'};
  const data:ImageCacheStatus={assetId:id,cached:raw.cached};
  if (typeof raw.runtimeUrl==='string' && raw.runtimeUrl.startsWith('https://braseiro.local/library-image/')) data.runtimeUrl=raw.runtimeUrl;
  if (typeof raw.sourceUrl==='string') data.sourceUrl=raw.sourceUrl;
  if (typeof raw.mimeType==='string') data.mimeType=raw.mimeType;
  if (typeof raw.bytes==='number' && Number.isFinite(raw.bytes) && raw.bytes>=0) data.bytes=raw.bytes;
  if (typeof raw.sha256==='string') data.sha256=raw.sha256;
  if (typeof raw.cachedAt==='string') data.cachedAt=raw.cachedAt;
  return {ok:true,data};
}

export function cacheExternalImage(asset: BridgeImageAsset): {ok:true;status:string}|{ok:false;message:string} {
  const id=normalizeId(asset?.assetId); if (!id) return {ok:false,message:'assetId de imagem inválido.'};
  if (asset.sourceType!=='external_reference' || asset.remoteReady!==true) return {ok:false,message:'Este asset não possui arquivo externo aprovado para cache.'};
  const remote=allowedRemoteUrl(asset.remoteUrl); if (!remote) return {ok:false,message:'A URL externa deste asset não está na lista permitida.'};
  const value=bridge(); if (!isBridge(value)) return {ok:false,message:'Cache nativo de imagens indisponível neste ambiente.'};
  const raw=parse(value.cacheRemoteImage(id,remote));
  if (!raw || raw.ok!==true || typeof raw.status!=='string') return {ok:false,message:typeof raw?.message==='string'?raw.message:'Não foi possível iniciar o cache da imagem.'};
  return {ok:true,status:raw.status};
}

export function clearCachedImage(assetId: string): {ok:true}|{ok:false;message:string} {
  const id=normalizeId(assetId); if (!id) return {ok:false,message:'assetId de imagem inválido.'};
  const value=bridge(); if (!isBridge(value)) return {ok:false,message:'Cache nativo de imagens indisponível neste ambiente.'};
  const raw=parse(value.clearCachedAsset(id));
  return raw?.ok===true ? {ok:true} : {ok:false,message:typeof raw?.message==='string'?raw.message:'Não foi possível remover a imagem do cache.'};
}

export function parseImageLibraryNativeEvent(raw: string): ImageLibraryNativeEvent | null {
  try {
    const decoded=JSON.parse(raw); if (!decoded || typeof decoded!=='object' || Array.isArray(decoded) || typeof decoded.type!=='string') return null;
    const data=decoded.data && typeof decoded.data==='object' && !Array.isArray(decoded.data) ? decoded.data as Record<string,unknown> : {};
    return {type:decoded.type,data};
  } catch { return null; }
}

export function packagedImageUrl(asset: BridgeImageAsset): string | null {
  if (!asset.localPath) return null;
  const clean=asset.localPath.replace(/^\/+/, '');
  return `file:///android_asset/vtt/${clean}`;
}

export function browserExternalFallbackUrl(asset: BridgeImageAsset): string | null {
  return asset.sourceType==='external_reference' && asset.remoteReady===true ? allowedRemoteUrl(asset.remoteUrl) || null : null;
}
