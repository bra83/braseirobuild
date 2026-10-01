import React, { useEffect, useMemo, useState } from 'react';
import type { Character } from '../../lib/db';
import { getStoreData } from '../../lib/db';
import type { ImmersiveAssetManifest } from '../../lib/immersiveAssets';
import { resolveImmersiveAsset } from '../../lib/immersiveAssets';
import {
  loadPartyTokenManifest,
  partyTokenVisualScale,
  resolvePartyTokenAsset,
  type PartyTokenEntry,
  type PartyTokenManifest,
} from '../../lib/partyTokenLibrary';

interface PartyClusterProps { characters: Character[]; manifest: ImmersiveAssetManifest | null; mode?: 'scene' | 'map'; }
interface PortraitEntry { id: string; name: string; culture: string; url: string | null; token: PartyTokenEntry | null; scale: number; }
interface Slot { x: number; y: number; size: number; z: number; layer: 'back'|'front'; }

const FORMATIONS: Record<number, Slot[]> = {
  1: [{ x:50,y:43,size:1.10,z:20,layer:'front' }],
  2: [{ x:39,y:39,size:1.02,z:19,layer:'front' },{ x:61,y:39,size:1.02,z:20,layer:'front' }],
  3: [{ x:50,y:25,size:.91,z:10,layer:'back' },{ x:39,y:54,size:1.02,z:20,layer:'front' },{ x:62,y:54,size:1.02,z:21,layer:'front' }],
  4: [{ x:38,y:27,size:.90,z:10,layer:'back' },{ x:63,y:27,size:.90,z:11,layer:'back' },{ x:40,y:56,size:1.02,z:20,layer:'front' },{ x:61,y:56,size:1.02,z:21,layer:'front' }],
  5: [{ x:31,y:28,size:.84,z:10,layer:'back' },{ x:50,y:22,size:.86,z:11,layer:'back' },{ x:69,y:28,size:.84,z:12,layer:'back' },{ x:40,y:57,size:.98,z:20,layer:'front' },{ x:61,y:57,size:.98,z:21,layer:'front' }],
  6: [{ x:29,y:27,size:.82,z:10,layer:'back' },{ x:50,y:21,size:.84,z:11,layer:'back' },{ x:71,y:27,size:.82,z:12,layer:'back' },{ x:32,y:57,size:.92,z:20,layer:'front' },{ x:50,y:61,size:.96,z:22,layer:'front' },{ x:69,y:57,size:.92,z:21,layer:'front' }],
};

const FALLBACK_TRAVELER = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNjAiIGhlaWdodD0iMjIwIiB2aWV3Qm94PSIwIDAgMTYwIDIyMCI+CjxlbGxpcHNlIGN4PSI4MCIgY3k9IjIwMyIgcng9IjU1IiByeT0iMTIiIGZpbGw9InJnYmEoMCwwLDAsLjM1KSIvPgo8Y2lyY2xlIGN4PSI4MCIgY3k9IjU1IiByPSIzMCIgZmlsbD0iI2Q4Yjk4YSIvPgo8cGF0aCBkPSJNDUUgNDQgODAgNSAxMTUgNDQgMTA0IDY2IDgwIDQ1IDU2IDY2WiIgZmlsbD0iIzVmNDMyNyIvPgo8cGF0aCBkPSJNMzUgMTkwUTM4IDg4IDgwIDg0UTEyMiA4OCAxMjUgMTkwWiIgZmlsbD0iIzM1NWEzYiIvPgo8cGF0aCBkPSJNNDggMTEwIDgwIDE0NSAxMTIgMTEwIiBmaWxsPSJub25lIiBzdHJva2U9IiNkM2JlODgiIHN0cm9rZS13aWR0aD0iNyIvPgo8Y2lyY2xlIGN4PSI2OSIgY3k9IjU0IiByPSIzIiBmaWxsPSIjMjUxYjEyIi8+CjxjaXJjbGUgY3g9IjkxIiBjeT0iNTQiIHI9IjMiIGZpbGw9IiMyNTFiMTIiLz4KPC9zdmc+';

export const PartyCluster: React.FC<PartyClusterProps> = ({ characters, manifest, mode = 'scene' }) => {
  const [portraits, setPortraits] = useState<PortraitEntry[]>([]);
  const [partyManifest, setPartyManifest] = useState<PartyTokenManifest | null>(null);
  const party = useMemo(() => characters.slice(0, 6), [characters]);

  useEffect(() => { void loadPartyTokenManifest().then(setPartyManifest); }, []);

  useEffect(() => {
    let cancelled = false; const urls: string[] = [];
    void Promise.all([
      getStoreData<any>('individual_tokens').catch(() => []),
      loadPartyTokenManifest().catch(() => null),
    ]).then(([stored, builtIns]) => {
      const next = party.map(character => {
        const tokenId = character.sheet?.tokenId;
        const storedToken = stored.find((item: any) => item?.id === tokenId && item?.blob);
        const builtIn = tokenId ? builtIns?.tokens.find(item => item.id === tokenId) || null : null;
        let url: string | null = null;
        // Built-in TOR2e party assets are authoritative for built-in token ids.
        // A stale IndexedDB blob must never replace a curated asset with an unrelated image.
        if (builtIn) url = resolvePartyTokenAsset(builtIns, builtIn);
        else if (storedToken?.blob) { url = URL.createObjectURL(storedToken.blob); urls.push(url); }
        return {
          id: character.id,
          name: character.sheet?.identity?.name || character.name || 'Herói',
          culture: character.sheet?.identity?.culture || character.culture || '',
          url,
          token: builtIn,
          scale: partyTokenVisualScale(builtIn, character.sheet?.identity?.culture || character.culture),
        };
      });
      // Altos atrás, baixos à frente: produz o agrupamento compacto do atlas sem exigir imagem pré-combinada.
      next.sort((a,b) => b.scale - a.scale);
      if (!cancelled) setPortraits(next);
    }).catch(() => { if (!cancelled) setPortraits([]); });
    return () => { cancelled = true; urls.forEach(URL.revokeObjectURL); };
  }, [party]);

  const base = resolveImmersiveAsset(manifest, 'party.base');
  const shadow = resolveImmersiveAsset(manifest, 'party.shadow');
  const visiblePortraits: PortraitEntry[] = portraits.length ? portraits : [{
    id: 'company-fallback',
    name: characters[0]?.sheet?.identity?.name || characters[0]?.name || 'Companhia',
    culture: characters[0]?.sheet?.identity?.culture || characters[0]?.culture || '',
    url: FALLBACK_TRAVELER,
    token: null,
    scale: 1,
  }];
  const formation = FORMATIONS[Math.max(1, Math.min(6, visiblePortraits.length))] || FORMATIONS[1];
  const mapMode = mode === 'map';
  return <div
    aria-label="Companhia no hex atual"
    data-party-cluster="true"
    data-party-world-child="true"
    style={mapMode ? {
      pointerEvents: 'none', position: 'relative',
      zIndex: 35, height: 92, width: 112, overflow: 'visible',
    } : {
      pointerEvents: 'none', position: 'absolute', left: '50%', top: '42%', transform: 'translate(-50%, -50%)',
      zIndex: 35, height: 122, width: 148, maxWidth: '38vw', maxHeight: '18vh', overflow: 'visible',
    }}
  >
    <div data-party-soft-shadow="true" aria-hidden="true" style={{ position: 'absolute', left: '12%', right: '12%', bottom: 5, height: '31%', zIndex: 0, borderRadius: '50%', background: 'radial-gradient(ellipse at center, rgba(0,0,0,.58) 0%, rgba(0,0,0,.34) 47%, rgba(0,0,0,0) 76%)', filter: 'blur(2px)', transform: 'scaleX(1.06)' }} />
    {base ? <img data-party-base="true" src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '1%', zIndex: 1, height: '74%', width: '98%', objectFit: 'contain', objectPosition: 'center center', overflow: 'visible', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/> : <div data-party-base="true" aria-hidden="true" style={{ position: 'absolute', left: '13%', right: '13%', bottom: 8, height: '45%', zIndex: 1, borderRadius: '50%', border: '3px solid #9a6b34', background: 'radial-gradient(ellipse at 50% 38%, #7f542c 0%, #513319 48%, #2b1a0d 78%, #150d08 100%)', boxShadow: 'inset 0 0 0 2px rgba(232,195,130,.3), 0 4px 5px rgba(0,0,0,.65)' }} />}
    <div style={{ position: 'absolute', inset: 0, zIndex: 2, overflow: 'visible' }}>
      {visiblePortraits.map((entry,index) => {
        const p = formation[index] || formation[formation.length-1];
        const effectiveScale = Math.min(1.08, p.size * entry.scale);
        return <img
          key={entry.id}
          src={entry.url || FALLBACK_TRAVELER}
          alt={entry.name}
          draggable={false}
          style={{
            position: 'absolute', width: mapMode ? 44 : 58, height: mapMode ? 62 : 80, maxWidth: mapMode ? undefined : '16vw', maxHeight: mapMode ? undefined : '12vh', objectFit: 'contain', objectPosition: 'center bottom',
            left: `${p.x}%`, top: `${p.y}%`, transform: `translate(-50%, -50%) scale(${effectiveScale})`, transformOrigin: '50% 82%', zIndex: p.z,
            filter: 'drop-shadow(0 3px 2px rgba(0,0,0,.78))',
          }}
        />;
      })}
    </div>
  </div>;
};
