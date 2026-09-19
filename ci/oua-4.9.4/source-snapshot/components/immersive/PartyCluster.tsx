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

interface PartyClusterProps { characters: Character[]; manifest: ImmersiveAssetManifest | null; }
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

export const PartyCluster: React.FC<PartyClusterProps> = ({ characters, manifest }) => {
  const [portraits, setPortraits] = useState<PortraitEntry[]>([]);
  const [partyManifest, setPartyManifest] = useState<PartyTokenManifest | null>(null);
  const party = useMemo(() => characters.slice(0, 6), [characters]);

  useEffect(() => { void loadPartyTokenManifest().then(setPartyManifest); }, []);

  useEffect(() => {
    let cancelled = false; const urls: string[] = [];
    void Promise.all([
      getStoreData<any>('individual_tokens').catch(() => []),
      loadPartyTokenManifest(),
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
      }).filter(entry => entry.url);
      // Altos atrás, baixos à frente: produz o agrupamento compacto do atlas sem exigir imagem pré-combinada.
      next.sort((a,b) => b.scale - a.scale);
      if (!cancelled) setPortraits(next);
    }).catch(() => { if (!cancelled) setPortraits([]); });
    return () => { cancelled = true; urls.forEach(URL.revokeObjectURL); };
  }, [party]);

  const base = resolveImmersiveAsset(manifest, 'party.base');
  const shadow = resolveImmersiveAsset(manifest, 'party.shadow');
  const formation = FORMATIONS[Math.max(1, Math.min(6, portraits.length))] || FORMATIONS[1];

  if (!portraits.length) return null;
  return <div
    aria-label="Companhia no hex atual"
    style={{
      pointerEvents: 'none', position: 'absolute', left: '50%', top: '42%', transform: 'translate(-50%, -50%)',
      zIndex: 35, height: 122, width: 148, maxWidth: '38vw', maxHeight: '18vh', overflow: 'visible',
    }}
  >
    {shadow && <img src={shadow} alt="" draggable={false} style={{ position: 'absolute', bottom: 1, left: '4%', height: '68%', width: '92%', objectFit: 'contain', opacity: .45, mixBlendMode: 'multiply' }}/>}
    {base && <img src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '4%', zIndex: 1, height: '70%', width: '92%', objectFit: 'contain', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/>}
    <div style={{ position: 'absolute', inset: 0, zIndex: 2, overflow: 'visible' }}>
      {portraits.map((entry,index) => {
        const p = formation[index] || formation[formation.length-1];
        const effectiveScale = Math.min(1.08, p.size * entry.scale);
        return <img
          key={entry.id}
          src={entry.url || undefined}
          alt={entry.name}
          draggable={false}
          style={{
            position: 'absolute', width: 58, height: 80, maxWidth: '16vw', maxHeight: '12vh', objectFit: 'contain', objectPosition: 'center bottom',
            left: `${p.x}%`, top: `${p.y}%`, transform: `translate(-50%, -50%) scale(${effectiveScale})`, transformOrigin: '50% 82%', zIndex: p.z,
            filter: 'drop-shadow(0 3px 2px rgba(0,0,0,.78))',
          }}
        />;
      })}
    </div>
  </div>;
};
