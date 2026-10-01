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

const PARTY_TOKEN_CROP_VERSION = 'autocrop-v2';

async function canvasBlob(canvas: HTMLCanvasElement): Promise<Blob | null> {
  return await new Promise(resolve => canvas.toBlob(resolve, 'image/webp', 0.94));
}

/** Isola o personagem principal de recortes antigos derivados de folhas com várias figuras. */
export async function autoCropPartyTokenBlob(source: Blob): Promise<Blob> {
  if (typeof document === 'undefined' || typeof createImageBitmap !== 'function') return source;
  let bitmap: ImageBitmap | null = null;
  try {
    bitmap = await createImageBitmap(source);
    const maxProbe = 260;
    const factor = Math.min(1, maxProbe / Math.max(bitmap.width, bitmap.height));
    const pw = Math.max(24, Math.round(bitmap.width * factor));
    const ph = Math.max(24, Math.round(bitmap.height * factor));
    const probe = document.createElement('canvas');
    probe.width = pw; probe.height = ph;
    const pctx = probe.getContext('2d', { willReadFrequently: true });
    if (!pctx) return source;
    pctx.clearRect(0, 0, pw, ph);
    pctx.drawImage(bitmap, 0, 0, pw, ph);
    const pixels = pctx.getImageData(0, 0, pw, ph).data;
    const mask = new Uint8Array(pw * ph);
    for (let y = 0; y < ph; y += 1) for (let x = 0; x < pw; x += 1) {
      const i = y * pw + x, p = i * 4;
      const a = pixels[p + 3], brightness = pixels[p] + pixels[p + 1] + pixels[p + 2];
      if (a > 18 && (a < 245 || brightness > 42)) mask[i] = 1;
    }
    const dilated = new Uint8Array(mask.length);
    for (let y = 0; y < ph; y += 1) for (let x = 0; x < pw; x += 1) {
      if (!mask[y * pw + x]) continue;
      for (let dy = -2; dy <= 2; dy += 1) for (let dx = -2; dx <= 2; dx += 1) {
        const nx = x + dx, ny = y + dy;
        if (nx >= 0 && nx < pw && ny >= 0 && ny < ph) dilated[ny * pw + nx] = 1;
      }
    }
    const seen = new Uint8Array(mask.length);
    type Box = { minX:number; minY:number; maxX:number; maxY:number; area:number; score:number };
    const boxes: Box[] = [];
    const qx = new Int32Array(mask.length), qy = new Int32Array(mask.length);
    for (let sy = 0; sy < ph; sy += 1) for (let sx = 0; sx < pw; sx += 1) {
      const seed = sy * pw + sx;
      if (!dilated[seed] || seen[seed]) continue;
      let head=0, tail=0, minX=sx, maxX=sx, minY=sy, maxY=sy, area=0, sumX=0, sumY=0;
      qx[tail]=sx; qy[tail]=sy; tail += 1; seen[seed]=1;
      while (head < tail) {
        const x=qx[head], y=qy[head]; head += 1; area += 1; sumX += x; sumY += y;
        if(x<minX)minX=x; if(x>maxX)maxX=x; if(y<minY)minY=y; if(y>maxY)maxY=y;
        for(let dy=-1;dy<=1;dy+=1) for(let dx=-1;dx<=1;dx+=1){
          if(!dx&&!dy)continue; const nx=x+dx, ny=y+dy;
          if(nx<0||nx>=pw||ny<0||ny>=ph)continue;
          const ni=ny*pw+nx; if(!dilated[ni]||seen[ni])continue;
          seen[ni]=1; qx[tail]=nx; qy[tail]=ny; tail += 1;
        }
      }
      if(area < Math.max(35, Math.round(pw*ph*0.003))) continue;
      const cx=sumX/area, cy=sumY/area;
      const dist=Math.hypot((cx-pw/2)/(pw/2),(cy-ph/2)/(ph/2));
      const centerBonus=(minX<=pw/2&&maxX>=pw/2)?1.65:1;
      const verticalBonus=(maxY-minY)>(maxX-minX)*0.7?1.12:1;
      boxes.push({minX,minY,maxX,maxY,area,score:area*centerBonus*verticalBonus/(1+dist*1.35)});
    }
    boxes.sort((a,b)=>b.score-a.score);
    const best=boxes[0];
    if(!best) return source;
    const scaleX=bitmap.width/pw, scaleY=bitmap.height/ph;
    let sx=best.minX*scaleX, sy=best.minY*scaleY;
    let sw=(best.maxX-best.minX+1)*scaleX, sh=(best.maxY-best.minY+1)*scaleY;
    const padX=sw*0.13, padY=sh*0.10;
    sx=Math.max(0,sx-padX); sy=Math.max(0,sy-padY);
    sw=Math.min(bitmap.width-sx,sw+padX*2); sh=Math.min(bitmap.height-sy,sh+padY*2);
    if(sw<bitmap.width*0.16||sh<bitmap.height*0.25) return source;
    const out=document.createElement('canvas');
    const outScale=Math.min(1,900/Math.max(sw,sh));
    out.width=Math.max(64,Math.round(sw*outScale)); out.height=Math.max(64,Math.round(sh*outScale));
    const ctx=out.getContext('2d');
    if(!ctx) return source;
    ctx.clearRect(0,0,out.width,out.height);
    ctx.drawImage(bitmap,sx,sy,sw,sh,0,0,out.width,out.height);
    return (await canvasBlob(out)) || source;
  } catch(error) {
    console.warn('[PARTY TOKEN] autocrop falhou; asset original preservado:', error);
    return source;
  } finally {
    try { bitmap?.close(); } catch {}
  }
}

/** Persiste apenas o asset escolhido; recortes antigos são reconstruídos automaticamente. */
export async function installPartyTokenById(tokenId: string): Promise<IndividualToken | null> {
  const manifest = await loadPartyTokenManifest();
  const entry = manifest?.tokens.find(item => item.id === tokenId);
  const url = resolvePartyTokenAsset(manifest, entry);
  if (!entry || !url) return null;
  const expectedSheetId=(manifest?.packId || 'tor2e-party-characters-v1')+':'+PARTY_TOKEN_CROP_VERSION;
  const existing = (await getStoreData<IndividualToken>('individual_tokens').catch(() => [])).find(item => item.id === tokenId);
  if (existing?.blob && existing.sheetId === expectedSheetId) return existing;
  const response = await fetch(url, { cache: 'force-cache' });
  if (!response.ok) throw new Error(`Asset de personagem indisponível: ${tokenId}`);
  const sourceBlob = await response.blob();
  const blob = await autoCropPartyTokenBlob(sourceBlob);
  const token: IndividualToken = {
    id: entry.id, sheetId: expectedSheetId, tokenIndex: Number(entry.sourceIndex || 0),
    blob, category: entry.category, createdAt: existing?.createdAt || Date.now(),
  };
  await saveStoreData('individual_tokens', token, 'corrigir recorte do personagem da Companhia');
  return token;
}
