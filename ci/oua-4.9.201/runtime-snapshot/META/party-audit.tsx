import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { EriadorMap } from './components/EriadorMap';
import { loadRuntimeMapManifest } from './lib/map';
import { saveStoreData } from './lib/db';
import './index.css';

const TOKEN_ID = 'party-audit-token';
const hero:any = {
  id: 'audit-hero',
  name: 'Trotter',
  culture: 'Bree',
  sheet: {
    identity: { name: 'Trotter', culture: 'Bree', vocation: 'Warden' },
    tokenId: TOKEN_ID
  },
  createdAt: Date.now(),
  updatedAt: Date.now()
};

function Audit() {
  const [hexId, setHexId] = useState<string | null>(null);
  useEffect(() => {
    void (async () => {
      const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="220" viewBox="0 0 160 220"><ellipse cx="80" cy="203" rx="55" ry="12" fill="rgba(0,0,0,.35)"/><circle cx="80" cy="55" r="30" fill="#d8b98a"/><path d="M45 44 Q80 5 115 44 L104 66 Q80 45 56 66 Z" fill="#5f4327"/><path d="M35 190 Q38 88 80 84 Q122 88 125 190 Z" fill="#355a3b"/><path d="M48 110 L80 145 L112 110" fill="none" stroke="#d3be88" stroke-width="7"/><circle cx="69" cy="54" r="3" fill="#251b12"/><circle cx="91" cy="54" r="3" fill="#251b12"/></svg>';
      await saveStoreData('individual_tokens', { id: TOKEN_ID, name: 'Trotter', blob: new Blob([svg], {type:'image/svg+xml'}), createdAt: Date.now(), updatedAt: Date.now() } as any, 'party visual audit');
      const manifest = await loadRuntimeMapManifest();
      const hex = manifest?.hexes.find((item:any) => item.playable) || manifest?.hexes[0] || null;
      setHexId(hex?.id || null);
    })();
  }, []);
  if (!hexId) return <div style={{color:'#fff',padding:24}}>Loading real map...</div>;
  return <div style={{position:'fixed',inset:0,width:'100vw',height:'100vh',overflow:'hidden'}}>
    <EriadorMap
      currentLocation="Party visual audit"
      campaignId="party-visual-audit"
      currentHexId={hexId}
      onPartyMoved={async()=>{}}
      onDiscoveryChanged={async()=>{}}
      strongholds={[]}
      immersive
      partyMarkerMode="none"
      followParty={false}
      partyCharacters={[hero]}
    />
  </div>;
}
createRoot(document.getElementById('root')!).render(<Audit/>);
