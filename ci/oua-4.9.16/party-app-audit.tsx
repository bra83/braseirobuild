import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { ImmersiveJourneyView } from './components/immersive/ImmersiveJourneyView';
import { loadRuntimeMapManifest } from './lib/map';
import './index.css';

function Audit() {
  const [hexId, setHexId] = useState<string | null>(null);

  useEffect(() => {
    void loadRuntimeMapManifest().then(manifest => {
      const hex = manifest?.hexes.find((item: any) => item.id === 'BN45')
        || manifest?.hexes.find((item: any) => item.playable)
        || manifest?.hexes[0]
        || null;
      setHexId(hex?.id || null);
    });
  }, []);

  if (!hexId) {
    return <div data-audit-loading="true" style={{ color: '#fff', padding: 24 }}>Carregando mapa real...</div>;
  }

  const masterState: any = {
    position: { currentHexId: hexId, locationName: 'A Campina' },
    worldClock: { hour: 12, season: 'spring' },
    threatClocks: [],
    livingWorld: { strongholds: [] },
    scene: { tension: 0 },
  };

  const node: any = {
    id: 'audit-scene',
    location: 'A Campina',
    weather: 'Tempo aberto',
    atmospheric_narration: 'A Companhia observa a campina antes de seguir viagem.',
    action_result: '',
    choices: [],
    narrativeMeta: { generationSource: 'gemini', generationModel: 'audit-runtime' },
  };

  const auditCharacter: any = {
    id: 'audit-character',
    name: 'Auditor da Companhia',
    culture: 'Hobbit',
    sheet: { identity: { name: 'Auditor da Companhia', culture: 'Hobbit' } },
  };

  return <ImmersiveJourneyView
    campaignId="party-app-visual-audit"
    currentNode={node}
    currentImage={null}
    characters={[auditCharacter]}
    presentNpcs={[]}
    masterState={masterState}
    customAction=""
    setCustomAction={() => {}}
    onChoice={() => {}}
    onCustomActionSubmit={(event: any) => event.preventDefault()}
    onPartyMoved={async () => {}}
    onMapDiscoveryChanged={async () => {}}
    onPartyRested={async () => {}}
    onOpenDice={() => {}}
    onStartAdventure={async () => {}}
    canStartAdventure={true}
    isStoryLoading={false}
    storyError={null}
    onDismissStoryError={() => {}}
    pendingTest={null}
    isNarrationLoading={false}
    narrationError={null}
    onPlayNarration={async () => {}}
    onStopNarration={() => {}}
    onShareSceneImagePrompt={async () => {}}
    onShareNpcPortraitPrompt={async () => {}}
    onNavigate={() => {}}
  />;
}

createRoot(document.getElementById('root')!).render(<Audit />);
